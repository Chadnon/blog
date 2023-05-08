package com.cdn.spring.boot.blog.repository;

import com.cdn.spring.boot.blog.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 评论仓库
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

}