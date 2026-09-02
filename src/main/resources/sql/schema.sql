-- =====================================================================
-- clinic 医院门诊医生排班与号源平台 —— 数据库初始化脚本
-- =====================================================================
-- 用法：
--   方式一（命令行）：mysql -uroot -p < schema.sql
--   方式二（Navicat/IDEA Database）：直接打开本文件执行
--
-- 注意：
--   1. 请先创建数据库：CREATE DATABASE clinic DEFAULT CHARACTER SET utf8mb4;
--   2. 表按依赖顺序创建：科室 → 医生 → 排班 → 号源 → 预约记录
--   3. 本文件不包含任何测试数据，clone 后请先执行本脚本再启动项目
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 科室表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `clinic_dept` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) NOT NULL COMMENT '科室名',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科室表';

-- ---------------------------------------------------------------------
-- 2. 医生表
--    注意：license_no（执业证号）有唯一索引，重复插入会抛
--    DuplicateKeyException → 全局异常处理转成 4001
--    avatar：医生头像访问 URL（/uploads/xxx.jpg），医生档案页直接拿它当图片地址
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `doctor` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) NOT NULL COMMENT '姓名',
  `title` varchar(20) DEFAULT NULL COMMENT '职称',
  `license_no` varchar(30) DEFAULT NULL COMMENT '执业证号',
  `dept_id` int DEFAULT NULL COMMENT '科室ID',
  `status` tinyint DEFAULT '0' COMMENT '0在岗/1停诊/2离职',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像访问URL（/uploads/xxx.jpg）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_license` (`license_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生表';

-- ---------------------------------------------------------------------
-- 3. 排班表
--    uk_doctor_shift 唯一索引：同一医生同一天同一班次只能排一次
--    （排班冲突的"插入防重"防线）
--    version 乐观锁版本号：预留（当前防重复走唯一索引）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `schedule` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `doctor_id` int NOT NULL COMMENT '医生ID',
  `shift_date` date NOT NULL COMMENT '排班日期',
  `shift_type` tinyint NOT NULL COMMENT '班次：1上午/2下午/晚班',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doctor_shift` (`doctor_id`,`shift_date`,`shift_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班表';

-- ---------------------------------------------------------------------
-- 4. 号源表
--    uk_doctor_date_slot 唯一索引：同一医生同一天同一时段只生成一个号源
--    status 0可约/1已约/2已停 —— 乐观锁防超卖的核心字段：
--    扣减号源 = UPDATE source SET status=1 WHERE id=? AND status=0
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `source` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `doctor_id` int NOT NULL COMMENT '医生ID',
  `shift_date` date NOT NULL COMMENT '排班日期',
  `time_slot` varchar(20) NOT NULL COMMENT '时段（如 09:00-12:00）',
  `status` tinyint DEFAULT '0' COMMENT '0可约/1已约/2已停',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doctor_date_slot` (`doctor_id`,`shift_date`,`time_slot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='号源表';

-- ---------------------------------------------------------------------
-- 5. 预约记录表
--    uk_source_id 唯一索引：一个号源最多一条预约记录 ——
--    乐观锁失效时的最后一道兜底防线
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `reserve_record` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `source_id` int NOT NULL COMMENT '号源ID（对应 source.id）',
  `patient_name` varchar(50) NOT NULL COMMENT '患者姓名',
  `patient_phone` varchar(20) NOT NULL COMMENT '患者手机号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1已预约 2已取消',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '预约时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_source_id` (`source_id`),
  -- 按手机号查这位患者约过哪些号（导诊台会用到）
  -- 没这个索引就是全表扫描：5000 行数据实测 type=ALL 扫 5001 行，加上之后 type=ref 只扫 1 行
  KEY `idx_patient_phone` (`patient_phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约记录表';

-- ---------------------------------------------------------------------
-- 6. 操作审计日志表
--    之前 AOP 只把操作打到日志文件里，日志文件一滚动、服务一重启，
--    旧的审计记录就翻不回来了；医疗合规要求"谁在什么时候改了什么"随时能查
--    success 记操作成败；error_msg 记失败原因（不记堆栈，太长且没必要）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` varchar(50) DEFAULT NULL COMMENT '操作人账号（登录接口的 userId，没登录是 anonymous）',
  `operation` varchar(100) NOT NULL COMMENT '操作的接口：Controller.方法名',
  `http_method` varchar(10) DEFAULT NULL COMMENT '请求方式 GET/POST/PUT/DELETE',
  `uri` varchar(255) DEFAULT NULL COMMENT '请求路径',
  `ip` varchar(50) DEFAULT NULL COMMENT '来访 IP',
  `params` text COMMENT '请求参数（JSON 串，密码已打码，超长截断）',
  `success` tinyint NOT NULL DEFAULT '1' COMMENT '1成功 0失败',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败原因（成功为 null）',
  `cost_ms` int DEFAULT NULL COMMENT '接口耗时（毫秒）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_operation` (`operation`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志表';
