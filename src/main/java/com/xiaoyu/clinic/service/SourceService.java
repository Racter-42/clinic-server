package com.xiaoyu.clinic.service;

import com.alibaba.fastjson2.JSON;
import com.xiaoyu.clinic.mapper.SourceMapper;
import com.xiaoyu.clinic.pojo.Source;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service                                        // ⭐ 交给 Spring 管理
public class SourceService {

    @Autowired
    private SourceMapper sourceMapper;          // 号源 Mapper（查数据库）

    @Autowired
    private StringRedisTemplate redisTemplate;  // Redis 工具

    // ========== 未来 7 天号源 + 缓存三防（与医生列表对称）==========
    public List<Source> listFuture7Days() {
        String key = "source:list:future7days";              // ⭐ 号源缓存的 key

        // ① 防穿透：先查缓存
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            if ("EMPTY".equals(cached)) {
                return Collections.emptyList();
            }
            return JSON.parseArray(cached, Source.class);
        }

        // ② 防击穿：互斥锁
        String lockKey = "lock:" + key;
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(locked)) {
            try {
                // 双重检查
                cached = redisTemplate.opsForValue().get(key);
                if (cached != null) {
                    return "EMPTY".equals(cached)
                            ? Collections.emptyList()
                            : JSON.parseArray(cached, Source.class);
                }
                // 查数据库
                List<Source> list = sourceMapper.listFuture7Days();
                if (list.isEmpty()) {
                    // 防穿透：空结果也占位，2 分钟短过期
                    redisTemplate.opsForValue().set(key, "EMPTY", 2, TimeUnit.MINUTES);
                } else {
                    // 防雪崩：随机过期时间（30~32 分钟）
                    int ttl = 30 + ThreadLocalRandom.current().nextInt(3);
                    redisTemplate.opsForValue().set(key, JSON.toJSONString(list), ttl, TimeUnit.MINUTES);
                }
                return list;
            } finally {
                redisTemplate.delete(lockKey);
            }
        } else {
            // 没抢到锁：等 50ms 重试
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return listFuture7Days();
        }
    }
}