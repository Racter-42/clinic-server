package com.xiaoyu.clinic.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的分布式锁。
 *
 * 加锁：SET key value NX EX 秒 —— 一条命令同时完成「不存在才写」和「设过期时间」，天然原子
 * 解锁：Lua 脚本比对持有者标识，只删自己加的锁
 *
 * 用到的场景：缓存击穿防护（同一时间只放一个线程去查数据库）
 */
@Component
public class RedisLock {

    // 解锁脚本：先比对值，相等才删。
    // 写成一个脚本交给 Redis 执行，是为了让「比对」和「删除」之间插不进别的命令——
    // 如果分成 GET 再 DEL 两步，两步之间锁可能刚好过期并被别人拿到，就会误删别人的锁
    // KEYS[1] = 锁的 key，ARGV[1] = 加锁时写进去的持有者标识
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "  return redis.call('del', KEYS[1]) " +
                    "else " +
                    "  return 0 " +
                    "end";

    @Autowired
    private StringRedisTemplate redisTemplate;

    // 脚本对象建一次就能反复用，不用每次解锁都新建
    private final DefaultRedisScript<Long> unlockScript =
            new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);

    /**
     * 尝试加锁，拿不到就立刻返回，不阻塞（阻塞等待由调用方自己控制）
     *
     * @param lockKey       锁的 key
     * @param expireSeconds 锁的自动过期时间（秒）。必须给过期时间，
     *                      否则持锁方进程崩了、没走到解锁那行，这把锁就永远留着，谁都拿不到
     * @return 加锁成功返回持有者标识（解锁时要原样传回来）；失败返回 null
     */
    public String tryLock(String lockKey, long expireSeconds) {
        // 持有者标识：每次加锁都生成一个新的，用来区分「这把锁是谁的」
        String owner = UUID.randomUUID().toString();
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, owner, expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success) ? owner : null;
    }

    /**
     * 解锁：只有锁还是自己的才删。
     *
     * 为什么不能直接 delete：如果临界区执行时间超过了锁的过期时间，
     * 锁已经自动释放、并被另一个线程重新拿到了，这时直接删就把别人的锁删了，
     * 后面再来的人又能拿到锁 —— 锁就形同虚设了。
     *
     * @return 释放成功 true；锁已不属于自己（过期后被别人拿走）false
     */
    public boolean unlock(String lockKey, String owner) {
        if (owner == null) {
            return false;                                  // 本来就没拿到锁，不用释放
        }
        Long deleted = redisTemplate.execute(
                unlockScript, Collections.singletonList(lockKey), owner);
        return deleted != null && deleted > 0;             // 脚本返回 1=删掉了，0=不是自己的锁
    }
}
