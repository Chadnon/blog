package com.cdn.spring.boot.blog.repository;

import com.cdn.spring.boot.blog.domain.Authority;
import org.springframework.data.jpa.repository.JpaRepository;

/**Authority仓库
 */
public interface AuthorityRepository extends JpaRepository<Authority, Long> {
}
