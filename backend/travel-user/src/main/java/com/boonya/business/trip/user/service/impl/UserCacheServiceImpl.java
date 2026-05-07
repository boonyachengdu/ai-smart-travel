package com.boonya.business.trip.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boonya.business.trip.common.entity.User;
import com.boonya.business.trip.user.mapper.UserMapper;
import com.boonya.business.trip.user.service.UserCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserCacheServiceImpl implements UserCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private UserMapper userMapper;

    private static final String USER_KEY_PREFIX = "cache:user:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    @Override
    public void cacheUsername(Long userId, String username) {
        if (userId == null || username == null) {
            return;
        }
        String key = USER_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, username, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    @Override
    public void cacheUsernames(Collection<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(User::getId, userIds);
        List<User> users = userMapper.selectList(wrapper);

        for (User user : users) {
            cacheUsername(user.getId(), user.getUsername());
        }
    }

    @Override
    public void removeUserCache(Long userId) {
        if (userId == null) {
            return;
        }
        String key = USER_KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }
}
