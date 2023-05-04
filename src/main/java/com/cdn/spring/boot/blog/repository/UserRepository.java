package com.cdn.spring.boot.blog.repository;

import com.cdn.spring.boot.blog.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;


public interface UserRepository extends JpaRepository<User,Long> {
    /**
     * 根据用户名分页查询用户列表
     * @param name
     * @param pageable
     * @return
     */
    Page<User> findByNameLike(String name, Pageable pageable);

    User findByUsername(String username);

    /**
     * 根据名称列表查询
     * @param usernames
     * @return
     */
    List<User> findByUsernameIn(Collection<String> usernames);

    /**
     * 根据主键列表查询
     * @param ids
     * @return
     */
    List<User> findByIdIn(Collection<Long> ids);

}
