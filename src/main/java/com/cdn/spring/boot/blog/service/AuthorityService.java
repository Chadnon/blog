package com.cdn.spring.boot.blog.service;

import com.cdn.spring.boot.blog.domain.Authority;

/**
 * Authority服务接口
 */
public interface AuthorityService {
    /**
     * 根据id获取 Authority
     * @param id
     * @return
     */
    Authority getAuthorityById(Long id);
}
