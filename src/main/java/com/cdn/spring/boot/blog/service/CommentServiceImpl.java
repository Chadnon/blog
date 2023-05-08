package com.cdn.spring.boot.blog.service;

import com.cdn.spring.boot.blog.domain.Comment;
import com.cdn.spring.boot.blog.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 评论服务实现
 */
@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Override
    public Comment getCommentById(Long id) {
        return commentRepository.getOne(id);
    }

    @Override
    public void removeComment(Long id) {
        commentRepository.deleteById(id);
    }
}
