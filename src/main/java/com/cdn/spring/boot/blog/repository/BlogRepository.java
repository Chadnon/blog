package com.cdn.spring.boot.blog.repository;

import com.cdn.spring.boot.blog.domain.Blog;
import com.cdn.spring.boot.blog.domain.Catalog;
import com.cdn.spring.boot.blog.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Blog 仓库
 */
public interface BlogRepository extends JpaRepository<Blog, Long> {

    /**
     * 根据用户名分页查询用户列表
     * @param user
     * @param title
     * @param pageable
     * @return
     */
    Page<Blog> findByUserAndTitleLike(User user, String title, Pageable pageable);

    /**
     * 根据用户名分页查询用户列表(时间逆序)
     * @param user
     * @param title
     * @param pageable
     * @return
     */
    Page<Blog> findByTitleLikeAndUserOrTagsLikeAndUserOrderByCreateTimeDesc(String title,User user,String tags,User user2,Pageable pageable);

    /**
     * 根据分类查询博客
     * @param catalog
     * @param pageable
     * @return
     */
    Page<Blog> findByCatalog(Catalog catalog, Pageable pageable);
}
