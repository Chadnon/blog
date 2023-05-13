package com.cdn.spring.boot.blog.service;

import com.cdn.spring.boot.blog.domain.Follow;
import com.cdn.spring.boot.blog.repository.FollowReposity;
import com.cdn.spring.boot.blog.util.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * 关注服务接口实现
 */
@Service
public class FollowServiceImpl implements FollowService{

    @Autowired
    private FollowReposity followReposity;


    @Override
    public Follow saveFollow(Follow follow) {
        Follow principal = followReposity.findFollowByFansIdAndFollowId(follow.getFansId(), follow.getFollowId());
        if(principal != null){
            principal.setStatus(Constant.FOLLOW_STATUS_YES);
        }else{
            principal = follow;
        }
        return followReposity.save(principal);
    }

    @Override
    public void removeFollow(Long fans_id, Long follow_id) {
        Follow principal = followReposity.findFollowByFansIdAndFollowId(fans_id, follow_id);
        if(principal != null){
            principal.setStatus(Constant.FOLLOW_STATUS_NO);
        }
        followReposity.save(principal);
    }

    @Override
    public Page<Follow> listFollowsByFansIdLike(Integer status, Long fansId, Pageable pageable) {
        return followReposity.findAllByStatusAndFansId(status, fansId, pageable);
    }

    @Override
    public Page<Follow> listFollowsByFollowerId(Integer status, Long followerId, Pageable pageable) {
        return followReposity.findAllByStatusAndFollowId(status, followerId, pageable);
    }

    @Override
    public Follow isFollow(Long fans_id, Long follow_id) {
        return followReposity.findFollowByFansIdAndFollowId(fans_id,follow_id);
    }
}
