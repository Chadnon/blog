package com.cdn.spring.boot.blog.repository;

import com.cdn.spring.boot.blog.domain.Authority;
import com.cdn.spring.boot.blog.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

/**Vote 仓库
 */
public interface VoteRepository extends JpaRepository<Vote, Long> {
}
