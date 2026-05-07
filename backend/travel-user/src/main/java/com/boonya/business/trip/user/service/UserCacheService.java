package com.boonya.business.trip.user.service;

import java.util.Collection;

public interface UserCacheService {

    /**
     * 缓存用户名
     */
    void cacheUsername(Long userId, String username);

    /**
     * 批量缓存用户名
     */
    void cacheUsernames(Collection<Long> userIds);

    /**
     * 删除用户缓存
     */
    void removeUserCache(Long userId);
}