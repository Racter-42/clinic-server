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
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `doctor` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(50) NOT NULL COMMENT '姓名',
  `title` varchar(20) DEFAULT NULL COMMENT '职称',
  `license_no` varchar(30) DEFAULT NULL COMMENT '执业证号',
  `dept_id` int DEFAULT NULL COMMENT '科室ID',
  `status` tinyint DEFAULT '0' COMMENT '0在岗/1停诊/2离职',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_license` (`license_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='医生表';

-- ---------------------------------------------------------------------
-- 3. 排班表
--    uk_doctor_shift 唯一索引：同一医生同一天同一班次只能排一次
--    （D11 排班冲突的"插入防重"防线）
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
-- 5. 预约记录表（知识点 4B 新增）
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
  UNIQUE KEY `uk_source_id` (`source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约记录表';
