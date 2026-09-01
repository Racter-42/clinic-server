package com.xiaoyu.clinic.service;

import com.xiaoyu.clinic.exception.BusinessException;
import com.xiaoyu.clinic.mapper.ReserveRecordMapper;
import com.xiaoyu.clinic.mapper.SourceMapper;
import com.xiaoyu.clinic.pojo.ReserveRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class ReserveService {

    @Autowired
    private StringRedisTemplate redisTemplate;  // Redis 工具（处理防重 token）

    @Autowired
    private SourceMapper sourceMapper;          // 号源 Mapper（扣减号源）

    @Autowired
    private ReserveRecordMapper reserveRecordMapper;  // 预约记录 Mapper（写预约记录）

    /**
     * 挂号：防重复提交 + 号源防超卖 + 写预约记录
     *
     *  为什么返回 void？
     *   因为失败全部靠"抛异常"表达（由 GlobalExceptionHandler 转成 JSON），
     *   能走到最后一行就说明一定成功了，不需要返回值。
     *
     *  @Transactional 为什么在这？
     *   方法里有两条写数据库的 SQL（扣减号源 + 写预约记录），
     *   必须保证"要么都成功，要么都撤销"。
     */
    @Transactional                              //  事务注解（从 Controller 搬到这里）
    public void reserve(Integer sourceId, String token,
                        String patientName, String patientPhone) {

        // ===== 第一道关：防重复提交（Redis SETNX 令牌）=====
        // 删 token：第一次删成功返回 true；第二次删，key 已不存在，返回 false
        Boolean del = redisTemplate.delete("reserve:token:" + token);

        if (!Boolean.TRUE.equals(del)) {
            //  抛异常，不是 return！
            // 抛 RuntimeException 的子类 → Spring 事务回滚 → 异常冒泡到 GlobalExceptionHandler
            // → 自动转成 {"code":4004,"message":"请勿重复提交"} 返回给前端
            throw new BusinessException(4004, "请勿重复提交");
        }

        // ===== 第二道关：号源扣减（乐观锁 WHERE status = 0）=====
        // 返回影响行数：1 = 抢到了（status 从 0 改成 1）；0 = 没抢到（号源不存在或已被约走）
        int rows = sourceMapper.reserve(sourceId);

        if (rows == 0) {
            //  同样抛异常：事务回滚 + 转成 {"code":4003,...}
            throw new BusinessException(4003, "该号源已被预约，请刷新重试");
        }

        // ===== 第三道关：写预约记录（新表 + 唯一索引兜底）=====
        ReserveRecord record = new ReserveRecord();   // new 一个空对象
        record.setSourceId(sourceId);                 // 存号源 ID
        record.setPatientName(patientName);           // 存患者姓名
        record.setPatientPhone(patientPhone);         // 存患者手机号
        record.setStatus(1);                          // 1 = 已预约

        reserveRecordMapper.insert(record);           // 插入数据库
        // 如果 source_id 重复 → 数据库抛异常
        // → MyBatis 翻译成 DuplicateKeyException
        // → GlobalExceptionHandler 转成 4001
        // → 事务回滚（号源 status 恢复 0）

        //  能走到这里 = 两步都成功 = 事务提交
        // 方法正常返回（没有抛异常）→ Spring 提交事务 → 号源 status=1 和预约记录同时生效
    }
}