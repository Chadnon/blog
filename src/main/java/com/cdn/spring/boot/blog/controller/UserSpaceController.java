package com.cdn.spring.boot.blog.controller;

import com.cdn.spring.boot.blog.domain.*;
import com.cdn.spring.boot.blog.domain.es.EsBlog;
import com.cdn.spring.boot.blog.service.*;
import com.cdn.spring.boot.blog.util.Constant;
import com.cdn.spring.boot.blog.util.ConstraintViolationExceptionHandler;
import com.cdn.spring.boot.blog.util.IPUtil;
import com.cdn.spring.boot.blog.vo.Response;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolationException;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 用户主页空间控制器
 */
@Controller
@RequestMapping("/u")
public class UserSpaceController {

    @Qualifier("userServiceImpl")
    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserService userService;

    @Autowired
    private BlogService blogService;

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private FollowService followService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${file.server.url}")
    private String fileServerUrl;

    @Value("${netty.ws}")
    private String ws;

    /**
     * 用户的主页
     *
     * @param username
     * @param model
     * @return
     */
    @GetMapping("/{username}")
    public String userSpace(@PathVariable("username") String username, Model model) {
        User user = (User) userDetailsService.loadUserByUsername(username);
        model.addAttribute("user", user);
        model.addAttribute("userId", user.getId());
        return "redirect:/u/" + username + "/blogs";
    }

    /**
     * 获取个人页面设置页面
     *
     * @param username
     * @param model
     * @return
     */
    @GetMapping("/{username}/profile")
    @PreAuthorize("authentication.name.equals(#username)")
    public ModelAndView profile(@PathVariable("username") String username, Model model) {
        User user = (User) userDetailsService.loadUserByUsername(username);
        model.addAttribute("user", user);
        model.addAttribute("fileServerUrl", fileServerUrl);//文件服务器地址
        model.addAttribute("userId", user.getId());
        return new ModelAndView("/userspace/profile", "userModel", model);
    }

    /**
     * 保存个人设置
     *
     * @param user
     * @return
     */
    @PostMapping("/{username}/profile")
    @PreAuthorize("authentication.name.equals(#username)")
    public String saveProfile(@PathVariable("username") String username, User user) {
        User originalUser = userService.getUserById(user.getId()); //根据id获取用户
        originalUser.setEmail(user.getEmail());
        originalUser.setName(user.getName());

        // 判断密码是否做了变更
        String rawPassword = originalUser.getPassword();
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodePasswd = encoder.encode(user.getPassword());
        boolean isMatch = encoder.matches(rawPassword, encodePasswd);
        if (!isMatch) {
            originalUser.setEncodePassword(user.getPassword());
        }

        userService.saveUser(originalUser);
        return "redirect:/u/" + username + "/profile";
    }

    /**
     * 获取编辑头像的界面
     *
     * @param username
     * @param model
     * @return
     */
    @GetMapping("/{username}/avatar")
    @PreAuthorize("authentication.name.equals(#username)")
    public ModelAndView avatar(@PathVariable("username") String username, Model model) {
        User user = (User) userDetailsService.loadUserByUsername(username);
        model.addAttribute("user", user);
        return new ModelAndView("/userspace/avatar", "userModel", model);
    }


    /**
     * 保存头像
     *
     * @param username
     * @param user
     * @return
     */
    @PostMapping("/{username}/avatar")
    @PreAuthorize("authentication.name.equals(#username)")
    public ResponseEntity<Response> saveAvatar(@PathVariable("username") String username, @RequestBody User user) {
        String avatarUrl = user.getAvatar();

        User originalUser = userService.getUserById(user.getId());
        originalUser.setAvatar(avatarUrl);
        userService.saveUser(originalUser);

        return ResponseEntity.ok().body(new Response(true, "处理成功", avatarUrl));
    }

    /*
        * 获取用户的博客列表
     */
    @GetMapping("/{username}/blogs")
    public String listBlogsByOrder(@PathVariable("username") String username,
                                   @RequestParam(value = "order", required = false, defaultValue = "new") String order,
                                   @RequestParam(value = "catalog", required = false) Long category,
                                   @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                                   @RequestParam(value = "async", required = false) boolean async,
                                   @RequestParam(value = "pageIndex", required = false, defaultValue = "0") int pageIndex,
                                   @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize,
                                   Model model) {

        User user = (User) userDetailsService.loadUserByUsername(username); //博主
        model.addAttribute("user", user);

        boolean isBlogOwner = false;
        User principal = null; //当前访客
        // 判断操作的是不是自己主页
        if (SecurityContextHolder.getContext().getAuthentication() != null && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                && !SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString().equals("anonymousUser")) {
            principal = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal != null && username.equals(principal.getUsername())) {
                isBlogOwner = true;
            }
        }
        model.addAttribute("isBlogOwner", isBlogOwner);

        //如果不是自己主页，判断自己是否已经关注对方
        boolean isFollow = false;
        if (principal != null) {
            //防止当前用户是空，会报错
            principal = userService.getUserById(principal.getId()); //更新用户状态
            Follow follow = followService.isFollow(principal.getId(), user.getId());
            //判断当前用户是不是博主粉丝
            isFollow = (follow != null && follow.getStatus().equals(Constant.FOLLOW_STATUS_YES));
        }
        model.addAttribute("isFollow", isFollow);
        model.addAttribute("reader", principal.getUsername());
        Page<Blog> page = null;
        if (category != null) { //分类查询
            Catalog catalog = catalogService.getCatalogById(category);
            Pageable pageable = PageRequest.of(pageIndex, pageSize);
            page = blogService.listBlogsByCatalog(catalog, pageable);
            order = "";
        }

        if (order.equals("hot")) { // 最热查询
            Sort sort = new Sort(Sort.Direction.DESC, "readSize", "commentSize", "likeSize");
            Pageable pageable = PageRequest.of(pageIndex, pageSize, sort);
            page = blogService.listBlogsByTitleLikeAndSort(user, keyword, pageable);
        }
        if (order.equals("new")) { // 最新查询
            Pageable pageable = PageRequest.of(pageIndex, pageSize);
            page = blogService.listBlogsByTitleLike(user, keyword, pageable);
        }


        List<Blog> list = page.getContent();    // 当前所在页面数据列表

        model.addAttribute("catalogId", category);
        model.addAttribute("keyword", keyword);
        model.addAttribute("order", order);
        model.addAttribute("page", page);
        model.addAttribute("blogList", list);
        model.addAttribute("userId", user.getId());
        return (async == true ? "/userspace/u :: #mainContainerRepleace" : "/userspace/u");
    }

    /**
     * 获取博客展示界面
     *
     * @param id
     * @param model
     * @return
     */
    @GetMapping("/{username}/blogs/{id}")
    public String getBlogById(@PathVariable("username") String username, @PathVariable("id") Long id, Model model, HttpServletRequest request) {
        User principal = null;
        Blog blog = blogService.getBlogById(id);

        // 每次读取，同一个ip地址一天只能增加一次访问量
        //获取IP地址
        String ipAddress = IPUtil.getIpAddr(request);
        System.out.println(ipAddress);
        if (!stringRedisTemplate.hasKey("blogs:" + id)) {  //第一次设置键值的时候先设置过期时间为今天最后一秒
            stringRedisTemplate.opsForSet().add("blogs:" + id, "");
            //先获取今天最后一秒的时间值
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
            String[] data = df.format(new Date()).split(" ")[0].split("-");
            Calendar calendar = Calendar.getInstance();
            calendar.clear();
            calendar.set(Integer.valueOf(data[0]), Integer.valueOf(data[1]) - 1, Integer.valueOf(data[2]), 23, 59, 59);
            long millis = calendar.getTimeInMillis();
            stringRedisTemplate.expire("blogs:" + id, millis - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }
        Long isiIpExist = stringRedisTemplate.opsForSet().add("blogs:" + id, ipAddress);
        if (isiIpExist == 1) {  //返回说明ip增加成功,即这个ip是今天首次访问
            blogService.readingIncrease(id);
        }

        boolean isBlogOwner = false;

        // 判断操作用户是否是博客的所有者
        if (SecurityContextHolder.getContext().getAuthentication() != null && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                && !SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString().equals("anonymousUser")) {
            principal = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal != null && username.equals(principal.getUsername())) {
                isBlogOwner = true;
            }
        }

        // 判断操作用户的点赞情况
        List<Vote> votes = blog.getVotes();
        Vote currentVote = null; // 当前用户的点赞情况

        if (principal != null) {
            for (Vote vote : votes) {
                vote.getUser().getUsername().equals(principal.getUsername());
                currentVote = vote;
                break;
            }
        }

        boolean isFollow = false;
        String currentUsername = null;
        if (principal != null) {
            //防止当前用户是空，会报错
            principal = userService.getUserById(principal.getId()); //更新用户状态
            Follow follow = followService.isFollow(principal.getId(), blog.getUser().getId());
            //判断当前用户是不是博主粉丝
            isFollow = (follow != null && follow.getStatus().equals(1));
            currentUsername = principal.getUsername();
        }

        model.addAttribute("username", currentUsername);
        model.addAttribute("isBlogOwner", isBlogOwner);
        model.addAttribute("isFollow", isFollow);
        model.addAttribute("blogModel", blog);
        model.addAttribute("currentVote", currentVote);
        model.addAttribute("userId", principal.getId());
        return "/userspace/blog";
    }


    /**
     * 删除博客
     *
     * @param username
     * @param id
     * @return
     */
    @DeleteMapping("/{username}/blogs/{id}")
    @PreAuthorize("authentication.name.equals(#username)")
    public ResponseEntity<Response> deleteBlog(@PathVariable("username") String username, @PathVariable("id") Long id) {

        try {
            blogService.removeBlog(id);
        } catch (Exception e) {
            return ResponseEntity.ok().body(new Response(false, e.getMessage()));
        }

        String redirectUrl = "/u/" + username + "/blogs";
        return ResponseEntity.ok().body(new Response(true, "处理成功", redirectUrl));
    }

    /**
     * 获取新增博客的界面
     *
     * @param model
     * @return
     */
    @GetMapping("/{username}/blogs/edit")
    public ModelAndView createBlog(@PathVariable("username") String username, Model model) {
        User user = (User) userDetailsService.loadUserByUsername(username);
        List<Catalog> catalogs = catalogService.listCatalogs(user);

        model.addAttribute("blog", new Blog(null, null, null));
        model.addAttribute("catalogs", catalogs);
        model.addAttribute("fileServer", fileServerUrl);
        model.addAttribute("userId", user.getId());
        return new ModelAndView("/userspace/blogedit", "blogModel", model);
    }

    /**
     * 获取编辑博客的界面
     *
     * @param model
     * @return
     */
    @GetMapping("/{username}/blogs/edit/{id}")
    public ModelAndView editBlog(@PathVariable("username") String username, @PathVariable("id") Long id, Model model) {
        User user = (User) userDetailsService.loadUserByUsername(username);
        List<Catalog> catalogs = catalogService.listCatalogs(user);
        model.addAttribute("userId", user.getId());
        model.addAttribute("catalogs", catalogs);
        model.addAttribute("blog", blogService.getBlogById(id));
        model.addAttribute("fileServer", fileServerUrl);
        return new ModelAndView("/userspace/blogedit", "blogModel", model);
    }

    /**
     * 保存博客
     *
     * @param username
     * @param blog
     * @return
     */
    @PostMapping("/{username}/blogs/edit")
    @PreAuthorize("authentication.name.equals(#username)")
    public ResponseEntity<Response> saveBlog(@PathVariable("username") String username, @RequestBody Blog blog) {
        // 对 Catalog 进行空处理
        if (blog.getCatalog() == null || blog.getCatalog().getId() == null) {
            return ResponseEntity.ok().body(new Response(false, "未选择分类"));
        }

        try {
            if (blog.getId() == 0) {  //新增
                blog.setId(null);
                User user = (User) userDetailsService.loadUserByUsername(username);
                blog.setUser(user);
                blogService.saveBlog(blog);
            } else {    //更新
                Blog originalBlog = blogService.getBlogById(blog.getId());
                originalBlog.setContent(blog.getContent());
                originalBlog.setTitle(blog.getTitle());
                originalBlog.setSummary(blog.getSummary());
                originalBlog.setCatalog(blog.getCatalog());
                originalBlog.setTags(blog.getTags());
                blogService.saveBlog(originalBlog);
            }
        } catch (ConstraintViolationException e) {
            return ResponseEntity.ok().body(new Response(false, ConstraintViolationExceptionHandler.getMessage(e)));
        } catch (Exception e) {
            return ResponseEntity.ok().body(new Response(false, e.getMessage()));
        }

        String redirectUrl = "/u/" + username + "/blogs/" + blog.getId();
        return ResponseEntity.ok().body(new Response(true, "处理成功", redirectUrl));
    }

    /**
     * 获取关注/粉丝列表页面
     *
     * @param username
     * @param model
     * @return
     */
    @GetMapping("/{username}/{followOrFans}")
    @PreAuthorize("authentication.name.equals(#username)")
    public ModelAndView followList(@PathVariable("username") String username, @PathVariable("followOrFans") String followOrFans, Model model,
                                   @RequestParam(value = "pageIndex", required = false, defaultValue = "0") int pageIndex,
                                   @RequestParam(value = "pageSize", required = false, defaultValue = "10") int pageSize) {
        User user = (User) userDetailsService.loadUserByUsername(username);
        Page<Follow> page = null;
        List<Follow> followsList;
        List<User> resultList = null;
        try {
            Pageable pageable = PageRequest.of(pageIndex, pageSize);
            List<Long> ids = null;
            if (followOrFans.equals("follow")) {  //获取关注列表
                page = followService.listFollowsByFansIdLike(Constant.FOLLOW_STATUS_YES, user.getId(), pageable);
                followsList = page.getContent();    // 当前所在页面数据列表
                ids = new ArrayList<>();
                for (Follow follow : followsList) {
                    ids.add(follow.getFollowId());
                }
            } else if (followOrFans.equals("fans")) { //获取粉丝列表
                page = followService.listFollowsByFollowerId(Constant.FOLLOW_STATUS_YES, user.getId(), pageable);
                followsList = page.getContent();    // 当前所在页面数据列表
                ids = new ArrayList<>();
                for (Follow follow : followsList) {
                    ids.add(follow.getFansId());
                }
            }

            resultList = userService.listUsersByIds(ids);
        } catch (Exception e) {
            e.printStackTrace();
        }
        model.addAttribute("userId", user.getId());
        model.addAttribute("user", user);
        model.addAttribute("followList", resultList);
        model.addAttribute("page", page);
        model.addAttribute("fileServerUrl", fileServerUrl);//文件服务器地址
        return new ModelAndView("/userspace/" + followOrFans, "userModel", model);
    }

    /**
     * 关注博主
     *
     * @param bloggerId
     * @return
     */
    @PostMapping("/{username}/follow")
    @PreAuthorize("authentication.name.equals(#username)")
    public ResponseEntity<Response> addFollow(@PathVariable("username") String username, Long bloggerId) {
        if (username == null) {
            return ResponseEntity.ok().body(new Response(true, "请登录后再关注！！", null));
        }
        User user = (User) userDetailsService.loadUserByUsername(username);
        User blogger = userService.getUserById(bloggerId);
        try {
            Follow follow = new Follow(Constant.FOLLOW_STATUS_YES, user.getId(), bloggerId);
            followService.saveFollow(follow);
        } catch (Exception e) {
            return ResponseEntity.ok().body(new Response(false, e.getMessage()));
        }
        String redirectUrl = "/u/" + blogger.getUsername() + "/blogs" + bloggerId;
        return ResponseEntity.ok().body(new Response(true, "关注成功", redirectUrl));
    }

    /**
     * 取消关注
     *
     * @param username
     * @param bloggerId
     * @return
     */
    @DeleteMapping("/{username}/follow")
    @PreAuthorize("authentication.name.equals(#username)")
    public ResponseEntity<Response> deleteFollow(@PathVariable("username") String username, Long bloggerId) {
        User user = (User) userDetailsService.loadUserByUsername(username);
        User blogger = userService.getUserById(bloggerId);
        try {
            followService.removeFollow(user.getId(), bloggerId);
        } catch (Exception e) {
            return ResponseEntity.ok().body(new Response(false, e.getMessage()));
        }

        String redirectUrl = "/u/" + blogger.getUsername() + "/blogs" + bloggerId;
        return ResponseEntity.ok().body(new Response(true, "取消关注成功", redirectUrl));
    }

    /**
     * 获取聊天页面
     *
     * @param username
     * @param model
     * @return
     */
    @GetMapping("/{username}/chat")
    @PreAuthorize("authentication.name.equals(#username)")
    public String chat(@PathVariable("username") String username, Model model) {
        User user = (User) userDetailsService.loadUserByUsername(username);
        model.addAttribute("user", user);
        model.addAttribute("userId", user.getId());
        //注入websocket端口
        model.addAttribute("ws",ws);
        chatService.findChatViewByUserId(user.getId(),model);
        return "/userspace/chat";
    }

    /**
     * 通过指定用户id获取聊天页面
     * @param username
     * @param model
     * @return
     */
    @GetMapping("/{username}/chat/{friendId}")
    @PreAuthorize("authentication.name.equals(#username)")
    public String chatByFriendId(@PathVariable("username") String username, @PathVariable("friendId") Long friendId, Model model) {
        User user = (User) userDetailsService.loadUserByUsername(username);
        model.addAttribute("user", user);
        model.addAttribute("friendId", friendId);
        //注入websocket端口
        model.addAttribute("ws",ws);
        model.addAttribute("userId", user.getId());
        chatService.findChatViewByUserId(user.getId(),model);
        boolean existChatMain = chatService.isExistChatMain(user.getId(), friendId);
        if(!existChatMain){
            chatService.creatChatMain(user.getId(), friendId);
        }
        model.addAttribute("exist",existChatMain);
        return "/userspace/chat";
    }

    /**
     * 清除未读消息标记
     * @param username
     * @return
     */
    @DeleteMapping("/{username}/chat/{userId}/{friendId}")
    @PreAuthorize("authentication.name.equals(#username)")
    public ResponseEntity<Response> deleteUnreadMark(@PathVariable("username") String username,
                                                     @PathVariable("userId") Long userId,
                                                     @PathVariable("friendId") Long friendId) {
        try {
            chatService.deleteUnreadMark(userId, friendId);
        } catch (Exception e) {
            return ResponseEntity.ok().body(new Response(false, e.getMessage()));
        }

        String redirectUrl = "/u/" + username + "/chat";
        return ResponseEntity.ok().body(new Response(true, "处理成功", redirectUrl));
    }

}
