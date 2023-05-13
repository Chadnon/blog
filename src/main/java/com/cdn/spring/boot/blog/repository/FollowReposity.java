package com.cdn.spring.boot.blog.repository;

import com.cdn.spring.boot.blog.domain.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 关注列表仓库
 */
public interface FollowReposity extends JpaRepository<Follow, Long> {

    /**
     * 查询关注列表
     * @param fans_id
     * @param pageable
     * @return
     */
    Page<Follow> findAllByStatusAndFansId(Integer status, Long fans_id, Pageable pageable);

    /**
     * 查询粉丝列表
     * @param follow_id
     * @param pageable
     * @return
     */
    Page<Follow> findAllByStatusAndFollowId(Integer status, Long follow_id, Pageable pageable);

    /**
     * 判断一个人是不是另一个人的粉丝
     * @param fans_id
     * @param follow_id
     * @return
     */
    Follow findFollowByFansIdAndFollowId (Long fans_id, Long follow_id);
}
