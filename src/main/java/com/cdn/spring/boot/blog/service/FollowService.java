package com.cdn.spring.boot.blog.service;


import com.cdn.spring.boot.blog.domain.Blog;
import com.cdn.spring.boot.blog.domain.Follow;
import com.cdn.spring.boot.blog.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 关注服务接口
 * Created by DoinNang on 2020/6/6 15:14
 */
public interface FollowService {

    /**
     * 保存Follow
     * @param follow
     * @return
     */
    Follow saveFollow(Follow follow);

    /**
     * 删除Follow
     * @param fans_id
     *  @param follow_id
     * @return
     */
    void removeFollow(Long fans_id, Long follow_id);

    /**
     * 获取关注列表
     * @param
     * @return
     */
    Page<Follow> listFollowsByFansIdLike(Integer status, Long fansId, Pageable pageable);

    /**
     * 获取粉丝列表
     * @param followerId
     * @return
     */
    Page<Follow> listFollowsByFollowerId(Integer status, Long followerId,Pageable pageable);

    /**
     * 判断一个人是不是另一个人的粉丝
     * @param fans_id
     * @param follow_id
     * @return
     */
    Follow isFollow(Long fans_id, Long follow_id);
}
