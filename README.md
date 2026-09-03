# 医院门诊医生排班与号源平台

面向小型连锁门诊诊所的**医生排班与号源管理**后端服务，覆盖「排班 → 号源生成 → 挂号预约 → 诊疗留痕」完整业务链路。
个人独立开发项目，需求分析、库表设计、接口开发、并发安全设计与文档维护均由本人完成。

> **一句话概括技术含量**：这是一个把「**高并发下不超卖、不重复提交、缓存不脏读**」三个真实生产问题完整落地的项目，而不只是增删改查。

---

## 亮点速览

| 维度 | 成果 | 落地位置 |
| --- | --- | --- |
| 接口性能 | 高频读接口响应时间 **126ms → 6ms**（本地实测，10 次取平均） | `DoctorService` / `SourceService` |
| 并发安全 | 号源防超卖三道防线：数据库 CAS + 唯一索引 + Redis 防重令牌，实测并发下零超卖 | `ReserveService` |
| 缓存可靠性 | 缓存三防（穿透 / 击穿 / 雪崩）+ 延迟双删保证一致性 | `DoctorService` / `SourceService` |
| 查询优化 | 手机号查询加索引后 `EXPLAIN` 由 `type=ALL` 扫 5001 行 → `type=ref` 扫 1 行 | `reserve_record.idx_patient_phone` |
| 第三方容错 | Deepseek 智能导诊：超时控制 + 失败重试 + 降级兜底，AI 挂了不影响挂号主流程 | `DeepseekService` |

---

## 业务痛点与对应解法

项目的每张表、每个索引、每段加锁代码，都对应一个真实的业务问题：

| 业务痛点 | 解法 | 落地位置 |
| --- | --- | --- |
| 医生停诊不能物理删除（医疗合规要求诊疗记录留痕） | `status` 软删除：0 在岗 / 1 停诊 / 2 离职，删除改更新，查询过滤 | `doctor` 表 + `DoctorService` |
| 执业证号重复会引发医疗事故 | `uk_license` 唯一索引 + 全局异常转 4001 | `schema.sql` |
| 同一医生同时段重复排班 | 联合唯一索引 `uk_doctor_shift (doctor_id, shift_date, shift_type)` 物理拦截 | `schedule` 表 |
| **号源被重复预约（超卖）** | `UPDATE source SET status=1 WHERE id=? AND status=0` 判断影响行数（1 抢到 / 0 被抢） | `SourceMapper#reserve` |
| 同一号源生成多条预约记录 | `uk_source_id` 唯一索引兜底，乐观锁失效时的最后防线 | `reserve_record` 表 |
| 号源靠人工录入效率低 | `@Scheduled` 定时扫描未来 7 天排班自动生成号源 | `SourceTask` |
| 高频读接口响应慢 | Redis 缓存科室医生列表 + 未来 7 天号源，含穿透 / 击穿 / 雪崩三防 | `DoctorService` / `SourceService` |
| 用户重复点击导致重复挂号 | 一次性防重令牌：Redis 生成 token，提交时原子删除校验 | `ReserveController` / `ReserveService` |
| 「谁改了什么」事后无法追溯 | AOP 环绕切面记录操作人 / IP / 参数 / 耗时，落 `audit_log` 表 | `AuditLogAspect` |

---

## 技术栈

| 分类 | 选型 | 说明 |
| --- | --- | --- |
| 核心框架 | Spring Boot 4.1.1 | JDK 17 |
| 持久层 | MyBatis 4.1.0 | 注解方式，开启驼峰映射 |
| 数据库 | MySQL 8 | InnoDB / utf8mb4，6 张表 |
| 缓存 | Redis | `StringRedisTemplate`（避免 JDK 序列化乱码） |
| 认证 | JWT（jjwt 0.12.5） | 拦截器 + 白名单放行 |
| 切面 | Spring AOP | 操作审计日志 |
| 校验 | Jakarta Validation | `@Valid` / `@Validated` |
| 接口文档 | Knife4j 5.4.0 | `/doc.html` |
| 调度 | Spring `@Scheduled` | Cron 表达式 |
| JSON | fastjson2 2.0.53 | 缓存序列化 |
| 第三方 | Deepseek API | RestTemplate 调用 + 降级 |
| 构建 | Maven | 单模块 |

---

## 接口清单

> 除白名单接口外，全部需要登录：请求头携带 `token: <JWT>`，未登录返回 401。

| 方法 | 路径 | 说明 | 需登录 |
| --- | --- | --- | --- |
| POST | `/login` | 登录，返回 JWT | ❌ |
| GET | `/doctor/list` | 医生列表（**Redis 三防缓存 + 延迟双删**） | ✅ |
| POST | `/doctor/add` | 新增医生（`@Valid` 校验 + 执业证号唯一） | ✅ |
| PUT | `/doctor/update` | 更新医生（**延迟双删**保证缓存一致） | ✅ |
| DELETE | `/doctor/delete/{id}` | 停诊（软删除） | ✅ |
| GET | `/doctor/countByDept` | 各科室在岗医生数统计 | ✅ |
| POST | `/schedule/add` | 新增排班（唯一索引拦截冲突） | ✅ |
| PUT | `/schedule/update` | 更新排班 | ✅ |
| GET | `/schedule/list` | 排班列表 | ✅ |
| GET | `/schedule/queryByDept` | 按科室查排班 | ✅ |
| GET | `/source/list` | 未来 7 天号源（**Redis 三防缓存**） | ✅ |
| GET | `/reserve/token` | 获取一次性防重令牌（10 分钟有效） | ✅ |
| POST | `/reserve` | 挂号：参数校验 → 防重令牌 → **防超卖** → 写预约记录 | ✅ |
| POST | `/api/recommend` | 智能科室推荐（Deepseek + 失败降级） | ✅ |
| POST | `/upload` | 图片上传（类型白名单 + UUID 重命名） | ✅ |

白名单（无需登录）：`/login`、`/uploads/**`、`/doc.html`、`/swagger-ui/**`、`/v3/api-docs/**`、`/webjars/**`、`/knife4j/**`

---

## 并发与数据一致性设计（本项目核心）

### 1. 号源防超卖：三道防线

```
第一道：Redis 防重令牌（SETNX）  → 拦住"同一个人重复点击"
第二道：数据库 CAS 乐观锁        → 拦住"多个人抢同一个号"
第三道：uk_source_id 唯一索引    → 兜底，防止前两道都失效
```

**第二道的核心是一条带条件的 UPDATE**（不用 `SELECT` 后判断，那样有并发窗口）：

```sql
UPDATE source SET status = 1 WHERE id = ? AND status = 0
```

判断返回的影响行数：**1 = 抢到号，0 = 已被约走**。MySQL 的行锁保证这条语句的原子性，并发请求下只会有 1 个成功。

**为什么事务下沉到 Service**：扣减号源 + 写预约记录是两条写 SQL，必须同生共死。放在 Controller 会导致异常时号源已扣、记录没写。

**为什么用事务提交回调删缓存**（`TransactionSynchronization.afterCommit`）：`@Transactional` 的提交时机在方法返回时。若在方法内直接删缓存，事务尚未提交，其他请求读到的仍是旧值并写回缓存——删了等于白删。

### 2. 防重复提交：一次性令牌

```
1. GET /reserve/token  → 服务端生成 UUID，SETNX 存 Redis（10 分钟过期）
2. POST /reserve?token=xxx  → 服务端 DELETE 该 key
3. DELETE 返回 true = 首次提交放行；返回 false = 重复提交拒绝
```

Redis 的 `DEL` 是单线程原子操作，天然适合做「只能成功一次」的判定。

### 3. 缓存三防 + 一致性

| 问题 | 场景 | 解法 |
| --- | --- | --- |
| 穿透 | 查不存在的数据，每次都打数据库 | 空结果也写 `EMPTY` 占位，**2 分钟短过期** |
| 击穿 | 单个热点 key 过期瞬间大量请求涌入 | `setIfAbsent` 互斥锁，只放 1 个线程回源 + 双重检查 + `finally` 释放锁 |
| 雪崩 | 大批 key 同时过期 | 过期时间加随机值 `30 + ThreadLocalRandom.nextInt(3)` 分钟 |
| 一致性 | 更新数据后缓存是旧值 | 延迟双删：先删缓存 → 更新数据库 → 延迟 500ms 再删一次 |

> 实现细节：没抢到锁的线程不无限递归（防栈溢出），改为**最多轮询 3 轮、每轮 50ms**，超时则直接查库兜底——缓存只是加速，数据库才是权威。

### 4. 排班防并发覆盖：version 乐观锁

排班的「防重复」和「防覆盖」是两个独立的并发问题，用了两把不同的锁：

| 并发问题 | 场景 | 解法 |
| --- | --- | --- |
| 重复插入 | 同一医生同一时段被添加两次 | 联合唯一索引 `uk_doctor_shift` 物理拦截 |
| 并发覆盖 | 两个管理员同时修改同一条排班，后提交的把先提交的改动**无声覆盖** | `version` 乐观锁 |

乐观锁核心 SQL（`ScheduleMapper#update`）：

```sql
UPDATE schedule
SET shift_type = #{shiftType}, version = version + 1
WHERE id = #{id} AND version = #{version}
```

- 修改前先读出 `version`，提交时原样带回，数据库层校验版本一致性
- 影响行数 = 1 更新成功；= 0 说明读取之后已被他人修改（version 对不上），`ScheduleService` 抛出「数据已被其他人修改，请刷新后重试」
- 与号源防超卖的 CAS 是同一个思想：**把并发判断下沉到 UPDATE 的 WHERE 条件里，而不是先 SELECT 再判断**（先查后改存在并发窗口）

---

## 参数校验设计

所有写接口均做入参校验，非法参数在进入 Service 前被拦截：

| 校验位置 | 写法 | 抛出的异常 | 兜底处理 |
| --- | --- | --- | --- |
| `@RequestBody` 对象（如新增医生） | 参数前加 `@Valid` | `MethodArgumentNotValidException` | 全局处理器取第一条字段提示，返回 4000 |
| `@RequestParam` 平铺参数（如挂号） | **类上加 `@Validated`**，参数加 `@NotBlank` / `@Pattern` | `ConstraintViolationException` | 同上，返回 4000 |

> 坑点记录：`@Valid` 只管 `@RequestBody` 对象，管不了平铺的 `@RequestParam`；平铺参数必须在**类上**加 `@Validated`（Spring 提供）才生效。

业务规则校验：
- 患者手机号 `^1[3-9]\d{9}$` —— 挂号关键联系方式，格式错误会导致回访失败
- 执业证号 `^\d{15}$` —— 医疗规范 15 位数字，唯一性另由 `uk_license` 唯一索引保证

全局异常处理器按类型分级返回，**绝不把堆栈暴露给前端**：

| 异常类型 | 错误码 | 场景 |
| --- | --- | --- |
| `BusinessException` | 业务自定义（4003/4004…） | 号源已被预约、重复提交 |
| `DuplicateKeyException` | 4001 | 唯一索引冲突（执业证号重复 / 排班冲突） |
| `MaxUploadSizeExceededException` | 4002 | 上传文件超限 |
| `MethodArgumentNotValidException` / `ConstraintViolationException` | 4000 | 参数校验失败 |
| `Exception`（兜底） | 500 | 系统异常，真实堆栈只进日志 |

---

## 数据库设计

**6 张表**，按依赖顺序：科室 → 医生 → 排班 → 号源 → 预约记录 → 操作审计。建表脚本见 `src/main/resources/sql/schema.sql`，全部字段带中文注释。

```
clinic_dept     科室表
doctor          医生表        uk_license (license_no)
schedule        排班表        uk_doctor_shift (doctor_id, shift_date, shift_type)
source          号源表        uk_doctor_date_slot (doctor_id, shift_date, time_slot)
reserve_record  预约记录表    uk_source_id (source_id) + idx_patient_phone (patient_phone)
audit_log       操作审计表    idx_user_id / idx_operation / idx_create_time
```

### 索引优化实测

`idx_patient_phone` 为按手机号查预约记录场景所加（导诊台高频操作）。5000 行数据实测：

| | 优化前 | 优化后 |
| --- | --- | --- |
| `EXPLAIN type` | `ALL`（全表扫描） | `ref`（索引命中） |
| 扫描行数 | 5001 | 1 |

### 设计取舍说明

- **重复排班 / 重复号源用唯一索引而非应用层判断**：应用层判断存在并发窗口（两个请求同时查到「不存在」然后都插入），唯一索引是数据库层面的物理拦截，绝对可靠。
- **防重复与防覆盖是两个问题，用两把不同的锁**：重复插入由联合唯一索引 `uk_doctor_shift` 物理拦截（更简单、更可靠）；并发覆盖由 `version` 乐观锁解决——`UPDATE ... WHERE id=? AND version=?`，影响行数为 0 说明数据已被他人修改，抛业务异常让用户刷新重试。详见上方「并发与数据一致性设计」第 4 节。

---

## 第三方调用容错

Deepseek 智能导诊是外部依赖，设计上保证「**AI 挂了不影响挂号主流程**」：

| 风险 | 应对 |
| --- | --- |
| 对方服务器卡死，请求一直挂着占满 Tomcat 线程池 | `SimpleClientHttpRequestFactory` 设连接超时 3s / 读取超时 5s |
| 网络偶发抖动 | `ResourceAccessException` 捕获后 sleep 300ms 重试 1 次 |
| Key 无效 / 被限流（重试无用） | 不重试，直接降级 |
| 响应体为空 / AI 返回空串 | 统一降级 |
| **最终兜底** | 返回固定文案「系统繁忙，请前往导诊台咨询」 |

---

## 目录结构

```
src/main/java/com/xiaoyu/clinic
├── controller      # 接口层：参数接收与校验，不含业务逻辑
├── service         # 业务逻辑层：事务边界、缓存三防、第三方调用
├── mapper          # MyBatis 数据访问层（注解方式）
├── pojo            # 实体 / DTO / 统一响应 Result
├── config          # 拦截器注册、静态资源映射
├── interceptor     # JWT 登录拦截器
├── aspect          # AOP 操作审计切面
├── exception       # 自定义业务异常 + 全局异常处理器
├── task            # 号源生成定时任务
├── utils           # JWT 工具类
└── ClinicServerApplication
```

分层约定：**Controller 只做参数校验与转发，业务逻辑与事务一律下沉 Service**（事务放 Controller 会出现「异常已抛出但部分 SQL 已执行」的风险）。

---

## 本地运行

### 环境要求

JDK 17+、MySQL 8、Redis、Maven

### 步骤

1. 创建数据库
   ```sql
   CREATE DATABASE clinic DEFAULT CHARACTER SET utf8mb4;
   ```

2. 执行建表脚本
   ```bash
   mysql -uroot -p clinic < src/main/resources/sql/schema.sql
   ```

3. 准备配置文件
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```
   填入自己的：数据库账号密码、Redis 地址、JWT 密钥、Deepseek API Key（**没有 Key 也能跑，导诊接口会自动降级**）

4. 启动应用：`ClinicServerApplication`

5. 验证：打开 `http://localhost:8080/doc.html` 查看 Knife4j 接口文档，可直接在页面上调试

### 快速验证主流程

```bash
# 1. 登录拿 token
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 2. 带 token 查医生列表（第一次走数据库，第二次走 Redis 缓存）
curl http://localhost:8080/doctor/list -H "token: <上一步返回的JWT>"

# 3. 挂号：先拿防重令牌，再提交
curl http://localhost:8080/reserve/token -H "token: <JWT>"
curl -X POST "http://localhost:8080/reserve?token=<上一步的token>&sourceId=1&patientName=张三&patientPhone=13800138000" \
  -H "token: <JWT>"
# 同一个 token 再提交一次 → 返回「请勿重复提交」
```

> **关于密钥安全**：`application.properties` 已在 `.gitignore` 中忽略，仓库只提交占位符模板 `application.properties.example`，避免个人密钥与数据库密码入库。

---

## 开发中踩过的坑（记录备查）

| 坑 | 现象 | 解决 |
| --- | --- | --- |
| 事务内删缓存导致脏读 | 预约成功后列表仍显示「可约」 | 改用 `afterCommit` 回调，事务提交后再删 |
| 上传超限返回 Tomcat HTML 错误页 | 前端拿不到 JSON | 配置 `spring.servlet.multipart.resolve-lazily=true`，让异常在 Controller 层抛出 |
| 静态资源映射路径缺尾斜杠 | 图片访问 404 | location 结尾补 `/`，并加 `file:` 前缀 |
| 平铺参数 `@Valid` 不生效 | 校验注解写了但没拦截 | 改为类上加 `@Validated` |
| 缓存互斥锁递归重试 | 高并发下有栈溢出风险 | 改为有限轮询 3 轮 + 查库兜底 |

---

## AI 辅助开发说明

开发过程中使用 AI 工具辅助，主要用于：样板代码生成（CRUD 与 SQL）、报错解释、模拟面试官提问。
**表结构设计、业务逻辑、并发安全方案、bug 调试与每行代码的人工审查均由本人完成**，可讲解其具体实现细节与取舍原因。

---

## 后续规划

- [ ] 号源按医生维度分页查询，支持按日期 / 科室筛选
- [ ] 预约记录取消与号源回滚（当前仅支持预约，取消流程待补）
- [ ] 审计日志接入消息队列异步落库，进一步降低主流程耗时
- [ ] 登录鉴权接入 Spring Security，支持角色区分（管理员 / 导诊台）
