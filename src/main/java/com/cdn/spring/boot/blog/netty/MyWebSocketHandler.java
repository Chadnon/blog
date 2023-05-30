package com.cdn.spring.boot.blog.netty;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cdn.spring.boot.blog.domain.ChatMessage;
import com.cdn.spring.boot.blog.domain.view.SocketMessage;
import com.cdn.spring.boot.blog.listen.MySessionListener;
import com.cdn.spring.boot.blog.repository.ChatMessageRepository;
import com.cdn.spring.boot.blog.util.DateUtils;
import com.cdn.spring.boot.blog.util.SpringUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息处理
 **/
public class MyWebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private final Logger logger = LoggerFactory.getLogger(MyWebSocketHandler.class);
    /**
     * 存储已经登录用户的channel对象
     */
    public static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 存储用户id和用户的channelId绑定
     */
    public static ConcurrentHashMap<Long, ChannelId> userMap = new ConcurrentHashMap<>();

    ChatMessageRepository chatMessageRepository = SpringUtil.getBean(ChatMessageRepository.class);


    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        logger.info("与客户端建立连接，通道开启！");
        //添加到channelGroup通道组
        channelGroup.add(ctx.channel());
        ctx.channel().id();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("与客户端断开连接，通道关闭！");
        //添加到channelGroup 通道组
        channelGroup.remove(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //首次连接是FullHttpRequest，把用户id和对应的channel对象存储起来
        if (null != msg && msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            String uri = request.uri();
            HashMap param = getUrlParams(uri);
            Long userId = Long.parseLong(param.get("userId").toString());
            userMap.put(userId, ctx.channel().id());
            logger.info("登录的用户id是：{}", userId);

            //如果url包含参数，需要处理
            if (uri.contains("?")) {
                String newUri = uri.substring(0, uri.indexOf("?"));
                request.setUri(newUri);
            }

        } else if (msg instanceof TextWebSocketFrame) {
            long lastTime = System.currentTimeMillis();
            //正常的TEXT消息类型
            TextWebSocketFrame frame = (TextWebSocketFrame) msg;
            logger.info("客户端收到服务器数据：{}", frame.text());
            SocketMessage socketMessage = JSON.parseObject(frame.text(), SocketMessage.class);
            //更新时间
            socketMessage.setLastDay(DateUtils.getDayNow(lastTime));

            chatMessageRepository.save(new ChatMessage(socketMessage.getChatMainId(), socketMessage.getMessage()));
            //如果对方也在线,则推送消息
            ChannelId channelId = userMap.get(socketMessage.getFriendId());

            if (channelId != null) {
                Channel ct = channelGroup.find(channelId);
                if (ct != null) {
                    ct.writeAndFlush(new TextWebSocketFrame(JSONObject.toJSONString(socketMessage)));
                }
            }

        }
        super.channelRead(ctx, msg);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) throws Exception {

    }

    private HashMap getUrlParams(String url) {
        if (!url.contains("=")) {
            return null;
        }
        HashMap map = new HashMap();
        String param = url.substring(url.indexOf("?") + 1);
        String arr[] = param.split("&");
        for (String str : arr) {
            String s[] = str.split("=");
            map.put(s[0], s[1]);
        }
        return map;
    }

}

/**
 * 这段代码实现了一个WebSocket服务器的处理器，继承了Netty的SimpleChannelInboundHandler<TextWebSocketFrame>，
 * 用于处理来自客户端的WebSocket Frame消息。
 *
 * 该处理器包含了以下几个重要的功能：
 *
 * 1.记录已连接的客户端Channel，并存储用户id和ChannelId的映射关系；
 *
 * 2.处理客户端的登录请求，将用户id和对应的ChannelId存储起来；
 *
 * 3.处理客户端发送的文本消息，将其解析为SocketMessage对象，将消息存储到数据库，并且转发给指定的客户端（如果对方在线的话）。
 *
 * 具体来说，该处理器中的方法逻辑如下：
 *
 * 1.channelActive(ChannelHandlerContext ctx)方法用于处理通道连接的建立，当有新的客户端连接到服务器时，将其Channel
 * 对象添加到channelGroup中，并记录连接日志。
 *
 * 2.channelInactive(ChannelHandlerContext ctx)方法用于处理通道连接的断开，当有客户端从服务器断开连接时，将其Channel
 * 对象从channelGroup中移除，并记录断开连接的日志。
 *
 * 3.channelRead(ChannelHandlerContext ctx, Object msg)方法用于处理来自客户端的消息，包含两种情况：
 *
 *     第一次连接请求，此时msg为FullHttpRequest类型的请求，该方法从请求中解析出用户id并将其和ChannelId进行绑定，以便后续的消息转发。
 *     文本消息请求，此时msg为TextWebSocketFrame类型的请求，该方法将消息解析为SocketMessage对象，并且将其存储到数据库中。
 *     如果对方在线，则会将消息转发给对应的客户端。
 *
 * 4.channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame)方法为空实现，没有实际作用。
 *
 * 5.getUrlParams(String url)方法用于从WebSocket的连接URL中解析出用户id等参数，以便进行用户信息的处理。
 *
 * 除此之外，该处理器中还包含了一个全局的channelGroup变量和一个userMap变量，分别用于记录已连接的客户端Channel和用户id和ChannelId的映射关系。
 * 该处理器还通过SpringUtil.getBean(ChatMessageRepository.class)获取了一个ChatMessageRepository对象，用于存储消息到数据库中
 */