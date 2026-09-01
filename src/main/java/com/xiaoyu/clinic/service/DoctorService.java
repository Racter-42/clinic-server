package com.xiaoyu.clinic.service;

import com.alibaba.fastjson2.JSON;                          // fastjson2：对象 ↔ JSON 互转
import com.xiaoyu.clinic.mapper.DoctorMapper;
import com.xiaoyu.clinic.pojo.Doctor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;                              //  空列表
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;            //  随机数（防雪崩）
import java.util.concurrent.TimeUnit;

@Service
public class DoctorService {

    @Autowired
    private DoctorMapper mapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // ========== 带缓存 + 三防的医生列表查询==========
    public List<Doctor> listAll() {
        String key = "doctor:list";                        // 缓存的 key

        // ① 防穿透（第一道）：先查缓存
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {                              // 缓存里有东西
            if ("EMPTY".equals(cached)) {                  // 是"空对象占位"？
                return Collections.emptyList();            // 返回空列表（不是 null）
            }
            return JSON.parseArray(cached, Doctor.class);  // 是真数据 → 反序列化返回
        }

        // ② 防击穿（第二道）：互斥锁 SETNX，只让 1 个线程查数据库
        String lockKey = "lock:" + key;                    // 锁的 key，约定 "lock:" 前缀
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);   // 抢锁（不存在才成功）
        if (Boolean.TRUE.equals(locked)) {                 // 抢到锁了

            try {
                // 双重检查：等锁的线程可能已经把缓存写好了，别再查一次库
                cached = redisTemplate.opsForValue().get(key);
                if (cached != null) {
                    return "EMPTY".equals(cached)
                            ? Collections.emptyList()      // 空占位 → 空列表
                            : JSON.parseArray(cached, Doctor.class);
                }

                // 真正查数据库（只有抢到锁的这 1 个线程会执行）
                List<Doctor> list = mapper.listAll();

                if (list.isEmpty()) {
                    // 防穿透：查不到也写个"EMPTY"占位，2 分钟短过期（万一以后真有数据能尽快读到）
                    redisTemplate.opsForValue().set(key, "EMPTY", 2, TimeUnit.MINUTES);
                } else {
                    // 防雪崩：过期时间加随机值（30~32 分钟），避免大批 key 同时过期
                    int ttl = 30 + ThreadLocalRandom.current().nextInt(3);
                    redisTemplate.opsForValue().set(key, JSON.toJSONString(list), ttl, TimeUnit.MINUTES);
                }
                return list;

            } finally {
                redisTemplate.delete(lockKey);             // 释放锁（必须 finally，出异常也释放）
            }

        } else {
            // 没抢到锁：说明别的线程正在查库，等 50ms 再重试（此时缓存多半已写好）
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return listAll();                              // 递归重试
        }
    }

    public void insert(Doctor d){ mapper.insert(d);}
    // ========== 更新医生 + 延迟双删（保证缓存一致性）==========
    public void update(Doctor d) {
        // 1. 先删缓存（⚠️ 项目缓存的是列表，所以删 doctor:list）
        redisTemplate.delete("doctor:list");
        // 2. 更新数据库
        mapper.update(d);
        // 3. 延迟 500ms 再删一次（防中间有请求读到旧数据写回缓存）
        new Thread(() -> {
            try {
                Thread.sleep(500);                    // 等 500ms
            } catch (Exception e) {
                // sleep 被打断：空 catch 兜底，不往外抛
            }
            redisTemplate.delete("doctor:list");      // 第二次删缓存
        }).start();                                    // 异步执行，不阻塞当前请求
    }

    public void delete(Integer id){ mapper.delete(id);}

    // ========== 科室在岗医生数统计 ==========
    public List<Map<String, Object>> countByDept() {
        return mapper.countByDept();
    }
}