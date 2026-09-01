# 医院门诊医生排班与号源平台

面向小型连锁门诊诊所的医生排班与号源管理平台，覆盖「排班 → 号源生成 → 挂号预约 → 诊疗记录留痕」主链路。
个人独立开发项目，从需求分析、库表设计、接口开发到文档与 Git 维护全部由本人完成。

## 业务痛点与对应解法

| 业务痛点 | 解法 | 落地位置 |
| --- | --- | --- |
| 医生停诊不能物理删除（医疗合规要求诊疗记录留痕） | `status` 软删除：0 在岗 / 1 停诊 / 2 离职，删除改更新，查询过滤 | `doctor` 表 + `DoctorService` |
| 执业证号重复会引发医疗事故 | `uk_license` 唯一索引 + 全局异常转 4001 | `schema.sql` |
| 同一医生同时段重复排班 | 联合唯一索引 `uk_doctor_shift (doctor_id, shift_date, shift_type)` 物理拦截 | `schedule` 表 |
| 号源被重复预约（超卖） | `UPDATE source SET status=1 WHERE id=? AND status=0` 判断影响行数（1 抢到 / 0 被抢） | `SourceMapper#reserve` |
| 同一号源生成多条预约记录 | `uk_source_id` 唯一索引兜底，乐观锁失效时的最后防线 | `reserve_record` 表 |
| 号源靠人工录入效率低 | `@Scheduled` 定时扫描未来 7 天排班自动生成号源 | `SourceTask` |
| 高频读接口响应慢 | Redis 缓存科室医生列表 + 未来 7 天号源，含穿透 / 击穿 / 雪崩三防 | `DoctorService` / `SourceService` |
| 用户重复点击导致重复挂号 | 一次性防重令牌：Redis 生成 token，提交时删除校验 | `ReserveController` / `ReserveService` |

## 技术栈

- 后端：Spring Boot 4.1.1 + MyBatis 4.1.0 + JDK 17
- 存储：MySQL 8（InnoDB / utf8mb4）+ Redis（StringRedisTemplate）
- 安全：JWT（jjwt 0.12.5）+ 拦截器白名单
- 工程化：Spring AOP 审计日志 + `@RestControllerAdvice` 全局异常处理 + Knife4j 5.4.0 接口文档
- 调度：Spring `@Scheduled` + Cron
- 第三方：Deepseek API（RestTemplate 调用 + 失败降级）
- 构建：Maven（单模块）

## 功能模块

### 医生与科室
- `POST /doctor/add`、`PUT /doctor/update`、`DELETE /doctor/delete/{id}`
- `GET /doctor/list` —— 带 Redis 三防缓存 + 延迟双删
- `GET /doctor/countByDept` —— 科室在岗医生数统计

### 排班
- `POST /schedule/add` —— 唯一索引拦截重复排班
- `PUT /schedule/update`、`GET /schedule/list`、`GET /schedule/queryByDept`

### 号源与挂号
- `GET /source/list` —— 未来 7 天号源，带 Redis 三防缓存
- `GET /reserve/token` —— 获取一次性防重令牌
- `POST /reserve` —— 挂号：防重令牌校验 + 号源防超卖 + 写预约记录，整段包 `@Transactional`
- `SourceTask` —— 定时任务扫描未来 7 天排班自动生成号源，重复号源由唯一索引拦截

### 基础能力
- `POST /login` —— JWT 登录鉴权，拦截器未登录返回 401，白名单放行 `/login`、`/uploads/**`、`/doc.html`、`/swagger-ui/**` 等
- `POST /api/recommend` —— 智能科室推荐，Deepseek API 调用失败自动降级为「请前往导诊台」
- `POST /upload` —— 图片上传，类型白名单 + UUID 重命名 + 路径配置化
- `AuditLogAspect` —— AOP 操作审计，记录「谁在什么时间操作了什么」，基于 SLF4J，业务零侵入
- `GlobalExceptionHandler` —— 统一异常处理，区分业务异常 / 参数校验 / 唯一键冲突 / 系统异常

## 缓存设计（三防 + 一致性）

- 防穿透：数据库查不到也写入 `EMPTY` 空占位，2 分钟短过期
- 防击穿：`setIfAbsent` 互斥锁，只放 1 个线程回源，双重检查 + `finally` 释放锁
- 防雪崩：过期时间加随机值（`30 + ThreadLocalRandom.nextInt(3)` 分钟），避免大批 key 同时失效
- 一致性：更新操作采用延迟双删（先删缓存 → 更新数据库 → 延迟 500ms 再删一次）

## 数据库设计

5 张表，按依赖顺序：科室 → 医生 → 排班 → 号源 → 预约记录。建表脚本见 `src/main/resources/sql/schema.sql`，全部字段带中文注释。

```
clinic_dept     科室表
doctor          医生表    uk_license (license_no)
schedule        排班表    uk_doctor_shift (doctor_id, shift_date, shift_type)
source          号源表    uk_doctor_date_slot (doctor_id, shift_date, time_slot)
reserve_record  预约记录表 uk_source_id (source_id) + idx_patient_phone (patient_phone)
```

`idx_patient_phone` 为按手机号查预约记录场景所加：优化前 `EXPLAIN` 显示 `type=ALL` 扫描 5001 行，加索引后 `type=ref` 只扫描 1 行。

## 目录结构

```
src/main/java/com/xiaoyu/clinic
├── controller      # 接口层
├── service         # 业务逻辑层（事务在此）
├── mapper          # MyBatis 映射层（注解方式）
├── pojo            # 实体 / DTO / 统一响应 Result
├── config          # 拦截器、静态资源映射等配置
├── interceptor     # JWT 登录拦截器
├── aspect          # AOP 操作审计切面
├── exception       # 自定义业务异常 + 全局异常处理器
├── task            # 号源生成定时任务
├── utils           # JWT 工具类
└── ClinicServerApplication
```

## 本地运行

1. 创建数据库：`CREATE DATABASE clinic DEFAULT CHARACTER SET utf8mb4;`
2. 执行建表脚本 `src/main/resources/sql/schema.sql`
3. 复制 `src/main/resources/application.properties.example` 为 `application.properties`，填入自己的数据库账号密码、Redis 地址、JWT 密钥与 Deepseek API Key
4. 启动 `ClinicServerApplication`
5. 打开 `http://localhost:8080/doc.html` 查看 Knife4j 接口文档

> `application.properties` 已在 `.gitignore` 中忽略，仓库只提交占位符模板，避免个人密钥与密码入库。

## AI 辅助开发说明

开发过程中使用 AI 工具辅助，主要用于：样板代码生成（CRUD 与 SQL）、报错解释、模拟面试官提问。
表结构设计、业务逻辑、bug 调试与每行代码的人工审查均由本人完成，可脱稿讲解全部实现细节。
