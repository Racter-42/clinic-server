package com.xiaoyu.clinic.service;

import com.alibaba.fastjson2.JSON;
import com.xiaoyu.clinic.mapper.SourceMapper;
import com.xiaoyu.clinic.pojo.Source;
import com.xiaoyu.clinic.utils.RedisLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service                                        // 交给 Spring 管理
public class SourceService {

    @Autowired
    private SourceMapper sourceMapper;          // 号源 Mapper（查数据库）

    @Autowired
    private StringRedisTemplate redisTemplate;  // Redis 工具

    @Autowired
    private RedisLock redisLock;                // 分布式锁（防缓存击穿）

    // ========== 未来 7 天号源 + 缓存三防（与医生列表对称）==========
    public List<Source> listFuture7Days() {
        String key = "source:list:future7days";              // 号源缓存的 key

        // ① 防穿透：先查缓存
        List<Source> cachedList = readCache(key);
        if (cachedList != null) {
            return cachedList;
        }

        // ② 防击穿：分布式锁，同一时间只放一个线程去查库
        String lockKey = "lock:" + key;
        String owner = redisLock.tryLock(lockKey, 10);   // 10 秒自动过期，防持锁方卡死后锁永不释放
        if (owner != null) {
            try {
                // 双重检查：等锁的线程可能已经把缓存写好了
                cachedList = readCache(key);
                if (cachedList != null) {
                    return cachedList;
                }
                // 真正查库 + 写缓存
                return queryAndCache(key);
            } finally {
                // 只删自己加的锁：万一临界区跑超了 10 秒、锁已被别人拿走，不能误删
                redisLock.unlock(lockKey, owner);
            }
        }

        // 没抢到锁：等别人查完。和医生列表一样的道理——锁只有 10 秒寿命，
        // 万一手气差一直轮不到，不能无限递归把自己栈挤爆，最多等 3 轮就查库兜底
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            cachedList = readCache(key);
            if (cachedList != null) {
                return cachedList;
            }
        }
        // 兜底：数据库永远是对的，缓存只是加速，查一次库不会错
        return sourceMapper.listFuture7Days();
    }

    /** 从 Redis 读缓存并转成对象；没缓存返回 null（EMPTY 占位转成空列表返回） */
    private List<Source> readCache(String key) {
        String cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            return null;
        }
        if ("EMPTY".equals(cached)) {
            return Collections.emptyList();
        }
        return JSON.parseArray(cached, Source.class);
    }

    /** 查数据库并把结果写进缓存（防穿透占位 + 防雪崩随机过期） */
    private List<Source> queryAndCache(String key) {
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
    }
}
