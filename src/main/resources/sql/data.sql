-- =====================================================================
-- clinic 门诊排班与号源平台 —— 本地测试数据脚本
-- =====================================================================
-- 用法：
--   命令行（方式一）：mysql -uroot -p clinic < data.sql
--   命令行（方式二）：mysql -uroot -p -e "source D:/.../sql/data.sql"
--   图形化工具：Navicat / IDEA Database 打开本文件，全选执行
--
-- 与 schema.sql 的分工：
--   schema.sql 只建表结构，不含数据；本文件负责填数据
--   执行顺序：先 schema.sql，再 data.sql
--
-- 幂等说明：本脚本可以反复执行
--   科室/医生用 INSERT IGNORE（主键或唯一索引冲突自动跳过）
--   排班/号源同样靠唯一索引兜底，不会产生重复
--   预约记录只对 status=1 且尚无记录的号源补插
--
-- 数据规模说明：
--   第 1-6 步是基础数据，只有未来 7 天的号源，几百行；
--   第 7 步补历史数据，把 reserve_record 撑到 5000+ 行。
--   表小的时候 MySQL 会直接走全表扫描，schema.sql 里 idx_patient_phone
--   这条索引优化（type=ALL 扫全表 → type=ref 命中单行）在小表上看不出差别，
--   所以第 7 步默认执行；只想灌基础数据的话，把第 7 步整段注释掉。
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 科室：3 个补到 12 个，覆盖常见门诊科室
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `clinic_dept` (`id`, `name`) VALUES
  (4,  '妇产科'),
  (5,  '骨科'),
  (6,  '眼科'),
  (7,  '耳鼻喉科'),
  (8,  '口腔科'),
  (9,  '皮肤科'),
  (10, '神经内科'),
  (11, '心内科'),
  (12, '急诊科');

-- ---------------------------------------------------------------------
-- 2. 医生：11 个补到 32 个，每个科室都有医生
--    职称按真实医院的层级分布（主任 > 副主任 > 主治 > 住院）
--    license_no 沿用 Z2023xxx 编号规则，靠唯一索引保证不重复
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `doctor` (`id`, `name`, `title`, `license_no`, `dept_id`, `status`) VALUES
  (12, '苏晴',   '主治医师',   'Z2023012', 1,  0),
  (13, '何秀兰', '主任医师',   'Z2023013', 4,  0),
  (14, '徐梦洁', '主治医师',   'Z2023014', 4,  0),
  (15, '马建军', '主任医师',   'Z2023015', 5,  0),
  (16, '高伟',   '副主任医师', 'Z2023016', 5,  0),
  (17, '谢明珠', '副主任医师', 'Z2023017', 6,  0),
  (18, '韩雪',   '主治医师',   'Z2023018', 6,  0),
  (19, '罗大伟', '主治医师',   'Z2023019', 7,  0),
  (20, '崔晓',   '住院医师',   'Z2023020', 7,  0),
  (21, '蒋文博', '副主任医师', 'Z2023021', 8,  0),
  (22, '秦岚',   '主治医师',   'Z2023022', 8,  0),
  (23, '曹敏',   '主治医师',   'Z2023023', 9,  0),
  (24, '邓超',   '住院医师',   'Z2023024', 9,  0),
  (25, '田志国', '主任医师',   'Z2023025', 10, 0),
  (26, '汪蕾',   '副主任医师', 'Z2023026', 10, 0),
  (27, '卢建华', '主任医师',   'Z2023027', 11, 0),
  (28, '段丽',   '主治医师',   'Z2023028', 11, 0),
  (29, '顾晨阳', '住院医师',   'Z2023029', 11, 0),
  (30, '邵峰',   '副主任医师', 'Z2023030', 12, 0),
  (31, '万小雨', '主治医师',   'Z2023031', 12, 0),
  (32, '蔡明轩', '住院医师',   'Z2023032', 12, 0);

-- ---------------------------------------------------------------------
-- 3. 排班：生成今天起未来 14 天的排班
--    排班窗口比号源宽（号源只有 7 天，见第 4 步），这不是笔误：
--      排班是管理员提前定好的，可以一直排到两周后；
--      号源是定时任务每天扫 ScheduleMapper.findFuture7Days()
--      的 7 天窗口滚动发布出来的 —— 两者本来就是两个窗口
--    如果排班也只排 7 天，第二天定时任务扫 [明天, 明天+7) 时
--    最后一天查不到排班，号源就会凭空缺一天
--    只给在岗医生（status=0）排；离职/停诊的医生不产生新排班
--    排班密度用取模模拟现实：不是每个医生每天都出诊，
--    否则 32 个医生 × 14 天全出诊会有 400+ 条排班，反而不像真实门诊
--    MOD 里的乘数（7 / 3）用来打散规律，避免同一医生总是同一天出诊
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `schedule` (`doctor_id`, `shift_date`, `shift_type`)
WITH RECURSIVE `days` (`dt`) AS (
    SELECT CURDATE()
    UNION ALL
    SELECT `dt` + INTERVAL 1 DAY FROM `days` WHERE `dt` < CURDATE() + INTERVAL 13 DAY
)
SELECT
    d.`id`                                                   AS `doctor_id`,
    dy.`dt`                                                  AS `shift_date`,
    -- 班次分布：上午约 60%、下午约 30%、晚班约 10%（贴近门诊实际）
    CASE
        WHEN MOD(d.`id` * 3 + DAYOFYEAR(dy.`dt`), 10) < 6 THEN 1
        WHEN MOD(d.`id` * 3 + DAYOFYEAR(dy.`dt`), 10) < 9 THEN 2
        ELSE 3
    END                                                      AS `shift_type`
FROM `doctor` d
CROSS JOIN `days` dy
WHERE d.`status` = 0
  -- 出诊频率：约 2/3 的日子出诊
  AND MOD(d.`id` * 7 + DAYOFYEAR(dy.`dt`), 3) <> 0;

-- ---------------------------------------------------------------------
-- 4. 号源：把排班"翻译"成号源
--    时段切分规则与 SourceTask.getTimeSlots() 保持一致：
--      上午(1) → 08:00-09:00 / 09:00-10:00 / 10:00-11:00   3 个
--      下午(2) → 14:00-15:00 / 15:00-16:00 / 16:00-17:00   3 个
--      晚班(3) → 18:00-19:00 / 19:00-20:00                 2 个
--    只处理 [今天, 今天+6] 这 7 天的排班，与定时任务的扫描窗口一致；历史号源不动
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `source` (`doctor_id`, `shift_date`, `time_slot`)
SELECT s.`doctor_id`, s.`shift_date`, m.`slot`
FROM `schedule` s
JOIN (
    SELECT 1 AS `shift_type`, '08:00-09:00' AS `slot` UNION ALL
    SELECT 1, '09:00-10:00' UNION ALL
    SELECT 1, '10:00-11:00' UNION ALL
    SELECT 2, '14:00-15:00' UNION ALL
    SELECT 2, '15:00-16:00' UNION ALL
    SELECT 2, '16:00-17:00' UNION ALL
    SELECT 3, '18:00-19:00' UNION ALL
    SELECT 3, '19:00-20:00'
) m ON m.`shift_type` = s.`shift_type`
-- 必须卡死 [今天, 今天+6天] 这个闭区间（共 7 天，与 findFuture7Days 等价）
-- 只写 >= CURDATE() 会把历史遗留的脏排班（例如 2099-01-01）也翻出来生成号源
WHERE s.`shift_date` BETWEEN CURDATE() AND CURDATE() + INTERVAL 6 DAY;

-- ---------------------------------------------------------------------
-- 5. 标记已约号源：把约 40% 的可约号源置为已约(1)
--    这样前端列表能同时看到"可约"和"已约"两种状态，
--    也方便验证号源扣减（status 0→1）这条链路
--    注意：只动未来的可约号源，历史数据保持原样
-- ---------------------------------------------------------------------
UPDATE `source`
SET `status` = 1
WHERE `shift_date` BETWEEN CURDATE() AND CURDATE() + INTERVAL 6 DAY
  AND `status` = 0
  AND MOD(`id` * 13, 10) < 4;

-- ---------------------------------------------------------------------
-- 6. 预约记录：给已约号源补上对应的患者信息
--    必须和 source.status 一致 —— 号源是"已约"却没有预约记录，
--    会让人觉得数据是手改出来的
--    患者姓名/手机号用取模拼出来，保证不重复、格式合法（11 位）
--    create_time 落在最近 72 小时内，符合"提前一两天约号"的习惯
-- ---------------------------------------------------------------------
INSERT INTO `reserve_record` (`source_id`, `patient_name`, `patient_phone`, `status`, `create_time`)
SELECT
    s.`id`                                                          AS `source_id`,
    CONCAT(
        ELT(MOD(s.`id`, 12) + 1, '张','王','李','赵','刘','陈','杨','黄','周','吴','徐','孙'),
        ELT(MOD(s.`id` * 7, 20) + 1, '伟','芳','娜','敏','静','丽','强','磊','军','洋',
                                      '勇','艳','杰','娟','涛','明','霞','鹏','婷','斌'),
        -- 约三成的人名是三个字，看起来更自然
        IF(MOD(s.`id`, 3) = 0,
           ELT(MOD(s.`id` * 11, 8) + 1, '华','宁','佳','宇','琳','峰','悦','轩'),
           '')
    )                                                               AS `patient_name`,
    CONCAT(
        '1',
        ELT(MOD(s.`id`, 6) + 1, '38','39','58','59','86','88'),
        LPAD(MOD(s.`id` * 7919, 100000000), 8, '0')
    )                                                               AS `patient_phone`,
    1                                                               AS `status`,
    NOW() - INTERVAL MOD(s.`id`, 72) HOUR                            AS `create_time`
FROM `source` s
WHERE s.`status` = 1
  AND s.`shift_date` BETWEEN CURDATE() AND CURDATE() + INTERVAL 6 DAY
  AND NOT EXISTS (SELECT 1 FROM `reserve_record` r WHERE r.`source_id` = s.`id`);

-- ---------------------------------------------------------------------
-- 7. 压测数据扩充：补历史排班 / 号源 / 预约，把表撑到 5000+ 行
--    做这个是为了让 schema.sql 里 idx_patient_phone 的优化能被验证：
--    "导诊台按手机号查患者约过哪些号"这条查询，在没有索引时是全表扫描，
--    有了索引只扫 1 行。但表只有几百行时优化器可能直接全表扫，
--    两者差距看不出来，必须先把数据量堆上去。
--
--    为什么补的是"历史"而不是"未来"：
--      未来号源受 7 天发布窗口约束（SourceTask 每天扫 findFuture7Days），
--      硬造未来数据会和定时任务的实际行为打架；
--      历史数据没有这个约束，而且"患者查自己约过的号"本来查的就是历史，
--      正好是这条索引的真实使用场景。
--
--    天数说明：120 天是让 reserve_record 落到 5000+ 行所需的量，
--              调大调小只影响数据量，不影响业务逻辑
--    耗时说明：数千行写入，本地约 1-2 秒
--    幂等说明：本段同样可以反复执行，不会产生重复记录
-- ---------------------------------------------------------------------

-- 7.1 历史排班：过去 120 天，出诊密度与未来排班共用一套取模规则
INSERT IGNORE INTO `schedule` (`doctor_id`, `shift_date`, `shift_type`)
WITH RECURSIVE `hist_days` (`dt`) AS (
    SELECT CURDATE() - INTERVAL 120 DAY
    UNION ALL
    SELECT `dt` + INTERVAL 1 DAY FROM `hist_days` WHERE `dt` < CURDATE() - INTERVAL 1 DAY
)
SELECT
    d.`id`                                                   AS `doctor_id`,
    dy.`dt`                                                  AS `shift_date`,
    CASE
        WHEN MOD(d.`id` * 3 + DAYOFYEAR(dy.`dt`), 10) < 6 THEN 1
        WHEN MOD(d.`id` * 3 + DAYOFYEAR(dy.`dt`), 10) < 9 THEN 2
        ELSE 3
    END                                                      AS `shift_type`
FROM `doctor` d
CROSS JOIN `hist_days` dy
WHERE d.`status` = 0
  -- 出诊频率同样是约 2/3 的日子
  AND MOD(d.`id` * 7 + DAYOFYEAR(dy.`dt`), 3) <> 0;

-- 7.2 历史号源：时段切分规则与第 4 步一致，窗口换成过去 120 天
INSERT IGNORE INTO `source` (`doctor_id`, `shift_date`, `time_slot`)
SELECT s.`doctor_id`, s.`shift_date`, m.`slot`
FROM `schedule` s
JOIN (
    SELECT 1 AS `shift_type`, '08:00-09:00' AS `slot` UNION ALL
    SELECT 1, '09:00-10:00' UNION ALL
    SELECT 1, '10:00-11:00' UNION ALL
    SELECT 2, '14:00-15:00' UNION ALL
    SELECT 2, '15:00-16:00' UNION ALL
    SELECT 2, '16:00-17:00' UNION ALL
    SELECT 3, '18:00-19:00' UNION ALL
    SELECT 3, '19:00-20:00'
) m ON m.`shift_type` = s.`shift_type`
WHERE s.`shift_date` BETWEEN CURDATE() - INTERVAL 120 DAY AND CURDATE() - INTERVAL 1 DAY;

-- 7.3 标记已约：历史号源里约七成已经约出去了
--     真实门诊的号源大部分会被约走，全留可约反而不像跑过的系统
UPDATE `source`
SET `status` = 1
WHERE `shift_date` BETWEEN CURDATE() - INTERVAL 120 DAY AND CURDATE() - INTERVAL 1 DAY
  AND `status` = 0
  AND MOD(`id` * 13, 10) < 7;

-- 7.4 历史预约记录：给已约的历史号源补患者信息
--     create_time 落在就诊日前 1-3 天，符合提前预约的习惯
INSERT INTO `reserve_record` (`source_id`, `patient_name`, `patient_phone`, `status`, `create_time`)
SELECT
    s.`id`                                                          AS `source_id`,
    CONCAT(
        ELT(MOD(s.`id`, 12) + 1, '张','王','李','赵','刘','陈','杨','黄','周','吴','徐','孙'),
        ELT(MOD(s.`id` * 7, 20) + 1, '伟','芳','娜','敏','静','丽','强','磊','军','洋',
                                      '勇','艳','杰','娟','涛','明','霞','鹏','婷','斌'),
        IF(MOD(s.`id`, 3) = 0,
           ELT(MOD(s.`id` * 11, 8) + 1, '华','宁','佳','宇','琳','峰','悦','轩'),
           '')
    )                                                               AS `patient_name`,
    CONCAT(
        '1',
        ELT(MOD(s.`id`, 6) + 1, '38','39','58','59','86','88'),
        LPAD(MOD(s.`id` * 7919, 100000000), 8, '0')
    )                                                               AS `patient_phone`,
    1                                                               AS `status`,
    TIMESTAMP(s.`shift_date`) - INTERVAL (MOD(s.`id`, 3) + 1) DAY    AS `create_time`
FROM `source` s
WHERE s.`status` = 1
  AND s.`shift_date` BETWEEN CURDATE() - INTERVAL 120 DAY AND CURDATE() - INTERVAL 1 DAY
  AND NOT EXISTS (SELECT 1 FROM `reserve_record` r WHERE r.`source_id` = s.`id`);

-- ---------------------------------------------------------------------
-- 8. 执行结果自检
-- ---------------------------------------------------------------------
SELECT '科室(clinic_dept)'      AS `表`, COUNT(*) AS `条数` FROM `clinic_dept`
UNION ALL SELECT '医生(doctor)',        COUNT(*) FROM `doctor`
UNION ALL SELECT '排班(schedule)',      COUNT(*) FROM `schedule`
UNION ALL SELECT '号源(source)',        COUNT(*) FROM `source`
UNION ALL SELECT '预约(reserve_record)', COUNT(*) FROM `reserve_record`;

SELECT '未来 7 天号源分布' AS `说明`;
SELECT `shift_date`, COUNT(*) AS `号源数`,
       SUM(`status` = 1) AS `已约`, SUM(`status` = 0) AS `可约`
FROM `source`
WHERE `shift_date` BETWEEN CURDATE() AND CURDATE() + INTERVAL 6 DAY
GROUP BY `shift_date` ORDER BY `shift_date`;
