package com.cdn.spring.boot.blog.listen;

import com.cdn.spring.boot.blog.domain.User;
import com.cdn.spring.boot.blog.netty.MyWebSocketHandler;
import io.netty.channel.ChannelId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.HashMap;

/**
 * session超时,移除 websocket对应的channel
 **/
public class MySessionListener implements HttpSessionListener {


    private final Logger logger = LoggerFactory.getLogger(MySessionListener.class);

    @Override
    public void sessionCreated(HttpSessionEvent httpSessionEvent) {
        logger.info("sessionCreated sessionId={}", httpSessionEvent.getSession().getId());
        MySessionContext.AddSession(httpSessionEvent.getSession());
    }
    @Override
    public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
        HttpSession session = httpSessionEvent.getSession();
        User user = null;
        if (SecurityContextHolder.getContext().getAuthentication() !=null && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                &&  !SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString().equals("anonymousUser")) {
            user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        }
        //销毁时重websocket channel中移除
        if(user!=null) {
          ChannelId channelId= MyWebSocketHandler.userMap.get(user.getId());
          if(channelId!=null){
              //移除了私聊的channel对象, 群聊的还未移除
              MyWebSocketHandler.userMap.remove(user.getId());
              MyWebSocketHandler.channelGroup.remove(channelId);
              logger.info("session timeout,remove channel, username={}",user.getUsername());
          }

        }
        MySessionContext.DelSession(session);
        logger.info("session destroyed  .... ");
    }


    public static class MySessionContext {

        private static HashMap mymap = new HashMap();

        public static synchronized void AddSession(HttpSession session) {
            if (session != null) {
                mymap.put(session.getId(), session);
            }
        }

        public static synchronized void DelSession(HttpSession session) {
            if (session != null) {
                mymap.remove(session.getId());
            }
        }

        public static synchronized HttpSession getSession(String session_id) {
            if (session_id == null){
                return null;
            }
            return (HttpSession) mymap.get(session_id);
        }
    }
}
