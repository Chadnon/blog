package com.cdn.spring.boot.blog.service;

import com.cdn.spring.boot.blog.domain.Vote;
import com.cdn.spring.boot.blog.repository.VoteRepository;
import org.aspectj.lang.annotation.AfterThrowing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Vote服务接口实现.
 */
@Service
public class VoteServiceImpl implements VoteService {

    @Autowired
    private VoteRepository voteRepository;
    @Override
    public Vote getVoteById(Long id) {
        return voteRepository.getOne(id);
    }

    @Override
    public void removeVote(Long id) {
        voteRepository.deleteById(id);
    }
}
