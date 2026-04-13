# 医院挂号系统 — 项目规格说明书（Spring Cloud Alibaba + Vue3）

> 本文档用于：**立项对齐、架构设计、接口约定、中间件使用说明、开发与 Code Review 基准**。  
> 代码实现阶段要求：**逐段注释说明含义与用途**（类、方法、关键分支、复杂 SQL、消息消费逻辑等），并遵循 **《阿里巴巴 Java 开发手册》** 风格（命名、格式、异常、日志、集合、并发、安全等）。

---

## 一、项目概述

### 1.1 建设目标

构建一套 **多角色、可水平扩展** 的互联网医院挂号系统：患者在线选科室/医生、查号源、下单挂号；医生查看当日患者、接诊并填写病历；管理员维护基础数据、排班与审核，并提供挂号统计可视化。

### 1.2 架构形态

- **后端**：Spring Cloud Alibaba **微服务** + **统一网关** + **Nacos** 注册与配置 + **OpenFeign** 服务间调用。  
- **前端**：Vue 3 + Element Plus + Vite，按 **用户前台 / 医生工作台 / 管理后台** 分模块与路由。  
- **中间件（必选）**：Nacos、Redis、MySQL 8.0、RocketMQ 5.x、Sentinel。

---

## 二、技术栈清单

### 2.1 后端（Spring Cloud Alibaba）

| 序号 | 技术 | 用途说明 |
|------|------|----------|
| 1 | Spring Cloud | 微服务基础设施：服务发现、配置、负载均衡、熔断等 |
| 2 | Spring Cloud Alibaba | 与 Nacos、Sentinel、RocketMQ 等阿里生态集成 |
| 3 | **Nacos** | **服务注册与发现**；**配置中心**（多环境、共享配置、动态刷新） |
| 4 | Spring Cloud Gateway | 统一入口：路由、JWT 鉴权、限流与 Sentinel 集成 |
| 5 | OpenFeign | 服务间 HTTP 调用，配合 Sentinel 做熔断 |
| 6 | **MyBatis-Plus** | ORM，减少样板 SQL，分页与条件构造 |
| 7 | **MySQL 8.0** | 主业务库：用户、科室、医生、排班、挂号、病历等 |
| 8 | **Redis** | 缓存（科室/医生/号源视图）、会话辅助、**分布式锁**（扣减号源） |
| 9 | **RocketMQ 5.x** | 异步解耦：挂号成功通知、统计聚合、审计流水、削峰 |
| 10 | **Sentinel** | **限流、熔断、降级**（网关 + 资源维度） |
| 11 | **JWT** | 无状态登录认证（Access Token；Refresh 策略可选） |
| 12 | Lombok | 减少 getter/setter/toString 等样板代码 |
| 13 | Hutool | 日期、加密、ID、集合等工具方法（避免重复造轮子） |
| 14 | Jakarta Validation | 入参校验（`@Valid`、`@NotNull` 等），统一校验异常处理 |

### 2.2 前端（Vue3 + Element Plus）

| 序号 | 技术 | 用途说明 |
|------|------|----------|
| 1 | Vue 3 | 组合式 API，组件化界面 |
| 2 | Element Plus | 表格、表单、对话框、布局等后台/中台 UI |
| 3 | Vite | 开发与构建，快速 HMR |
| 4 | Axios | HTTP 调用，拦截器统一 Token 与错误码 |
| 5 | Vue Router | 路由与权限守卫（角色：患者/医生/管理员） |
| 6 | Pinia | 全局状态：用户信息、字典、侧边栏等 |
| 7 | ECharts | **挂号统计**：按日/科室/医生维度的图表 |

### 2.3 中间件（必须用到）

1. **Nacos** — 注册中心 + 配置中心  
2. **Redis** — 缓存 + 分布式锁  
3. **MySQL 8.0** — 持久化  
4. **RocketMQ 5.x** — 消息队列  
5. **Sentinel** — 限流 / 熔断 / 降级  

### 2.4 开发工具

| 工具 | 用途 |
|------|------|
| IntelliJ IDEA | 后端与多模块工程开发 |
| Navicat | MySQL 建模、脚本执行、数据巡检 |
| ApiFox | 接口文档、Mock、联调、自动化测试 |
| Git | 版本控制与分支策略（建议 main/develop/feature） |

---

## 三、微服务划分建议

> 可按团队规模合并服务；以下为 **标准教学/中大型** 推荐拆分，便于演示 Nacos、Feign、MQ、分布式锁。

| 服务名 | 职责 | 典型技术点 |
|--------|------|------------|
| `gateway-service` | 路由、JWT 校验、Sentinel 网关流控 | Gateway Filter、Sentinel |
| `auth-service` | 登录、注册、Token 签发、刷新（可选） | JWT、BCrypt、Validation |
| `user-service` | 患者档案、基础用户信息扩展 | MyBatis-Plus、Redis 缓存 |
| `dept-doctor-service` | 科室、医生档案、医生简介 | 缓存热点科室/医生列表 |
| `schedule-service` | 排班、号源生成与查询 | Redis 缓存号源、与挂号服务协作 |
| `registration-service` | 挂号下单、订单状态、取消规则 | **Redis 分布式锁**扣号、**RocketMQ** 投递事件 |
| `medical-record-service` | 接诊、病历 CRUD（医生侧） | 与挂号订单关联 |
| `admin-service` | 后台审核、统计聚合查询、导出 | 读多写少可缓存 + MQ 异步统计 |
| `common-*` | `common-core`（异常、Result、常量）、`common-redis`、`common-mq` 等 | 复用与规范 |

**说明**：若初期简化，可将 `user-service` 与 `auth-service` 合并，但文档仍要求 **Nacos、Redis、MQ、Sentinel、JWT** 在架构中真实落地。

---

## 四、业务模块与前端页面映射

### 4.1 用户前台（患者）

| 功能 | 说明 | 后端依赖 |
|------|------|----------|
| 登录 / 注册 | 手机号或用户名 + 密码；JWT 返回 | auth-service |
| 科室浏览 | 树或列表展示科室 | dept-doctor-service |
| 医生列表 | 按科室筛选、分页 | dept-doctor-service |
| 号源查询 | 按医生 + 日期展示可预约时段 | schedule-service |
| 挂号下单 | 选号源下单、幂等、防超卖 | registration-service + Redis 锁 + MQ |
| 挂号记录 | 我的订单、状态、取消 | registration-service |

### 4.2 医生工作台

| 功能 | 说明 |
|------|------|
| 今日患者 | 当日已挂号列表（关联排班） |
| 接诊 | 标记已接诊 / 叫号顺序（可选） |
| 病历填写 | 主诉、现病史、诊断、医嘱等 |

### 4.3 管理后台

| 功能 | 说明 |
|------|------|
| 科室管理 | 科室增删改查、排序、启用禁用 |
| 医生管理 | 关联科室、职称、简介、账号绑定（可选） |
| 排班管理 | 生成号源、停诊、放号量 |
| 挂号审核 | 退费/异常单审核（按业务需要） |
| 统计 | ECharts：挂号量趋势、科室占比、医生排行 |

---

## 五、核心业务流程（文字版）

### 5.1 挂号下单（关键路径）

1. 患者查询号源（读多，**Redis 缓存**号源视图，短 TTL + 取消时失效）。  
2. 提交挂号请求 → `registration-service`：  
   - 使用 **Redis 分布式锁**（key 含 `scheduleId` + `slot` 或号源 ID）保证 **同一号源仅一人成功**。  
   - 扣减号源与写订单在同一 **本地事务**；成功后 **发送 RocketMQ 消息**（如 `registration-success`）。  
3. 消费者：更新统计、发送站内信/短信占位、写审计日志（可异步）。  
4. **Sentinel**：对「下单接口」配置 QPS / 线程数限流，防止冲库；Feign 调用下游失败时 **熔断降级**（返回友好提示）。  

### 5.2 JWT 认证

1. 登录成功后签发 JWT（含 `userId`、`roles`、`exp`）。  
2. **Gateway** 全局过滤器解析 JWT，校验签名与过期，写入下游请求头（如 `X-User-Id`）或 Spring Security 上下文（按选型）。  
3. 前端 Axios 拦截器附带 `Authorization: Bearer <token>`。  

### 5.3 Nacos 配置

- 各服务 `bootstrap.yml` 指向 Nacos；**数据源、Redis、MQ、JWT 密钥**等放 **配置中心**，禁止硬编码进仓库（可用占位 + 本地 `application-local.yml` 覆盖）。  

---

## 六、数据库设计要点（MySQL 8.0）

> 以下为 **逻辑表** 建议，实际字段以 ER 图为准。

- `sys_user`：用户、密码哈希、角色、手机号。  
- `dept`：科室。  
- `doctor`：医生，关联 `dept_id`。  
- `schedule`：排班（医生、日期、时段类型）。  
- `schedule_slot`：号源（余量、状态）。  
- `registration_order`：挂号订单（幂等键 `idempotent_key`）。  
- `medical_record`：病历，关联订单与医生。  

**规范**：主键建议雪花或自增；重要表有 `create_time` / `update_time`；软删除用 `deleted`；金额/次数用合适精度。

---

## 七、Redis 使用约定

| 场景 | Key 示例 | 说明 |
|------|----------|------|
| 科室列表缓存 | `dept:list:v1` | 带版本或 TTL，后台变更时删除 |
| 号源余量 | `slot:stock:{slotId}` | 与 DB 一致性策略：先锁再扣，或 Lua 脚本（按实现选型） |
| 分布式锁 | `lock:reg:{slotId}` | `SET NX EX`，finally 释放需校验 value 防误删 |
| 限流辅助（可选） | `ratelimit:user:{userId}` | 配合网关 Sentinel 双层防护 |

---

## 八、RocketMQ 主题建议（5.x）

| Topic | 生产者 | 消费者 | 说明 |
|-------|--------|--------|------|
| `registration-success` | registration-service | admin/stat | 异步统计、大屏数据 |
| `registration-cancel` | registration-service | 通知模块 | 释放号源后的后续动作 |
| `audit-log` | 各服务（AOP） | 日志服务或落库 | 操作审计（可选） |

**要求**：消息体 JSON 带 `traceId`；消费幂等（业务唯一键去重）。

---

## 九、Sentinel 规则建议

- **网关**：按 API 分组限流（登录、下单、查询号源）。  
- **服务**：Feign 调用 `schedule-service`、`registration-service` 配置慢调用比例熔断。  
- **降级**：返回统一 `Result` 结构，提示「系统繁忙，请稍后再试」。  

---

## 十、接口与文档

- 使用 **ApiFox** 维护 OpenAPI：路径前缀建议 `/api/v1/...`。  
- 统一响应：`code`、`message`、`data`、`timestamp`、`traceId`。  
- 错误码分段：业务 1xxxx、系统 5xxxx、鉴权 401xx。  

---

## 十一、前端工程结构建议

```
src/
  api/           # 按模块拆分请求
  views/
    patient/     # 用户前台
    doctor/      # 医生工作台
    admin/       # 管理后台
  router/        # 路由 + 权限 meta
  stores/        # Pinia
  components/    # 通用组件
```

- 路由懒加载；表格页统一封装分页与查询表单。  
- ECharts 按需引入，统计页独立 chunk。  

---

## 十二、阿里巴巴 Java 开发规范（落地要求）

实现代码时至少遵守以下习惯（与手册章节对应，评审可对照）：

1. **命名**：类名 UpperCamelCase，方法/变量 lowerCamelCase，常量全大写下划线。  
2. **魔法值**：禁止魔法字符串/数字，使用枚举或常量类。  
3. **异常**：业务异常与系统异常分离，不要吞异常；日志带上下文。  
4. **日志**：使用 SLF4J，占位符 `{}`，敏感信息脱敏。  
5. **集合**：指定初始容量；并发场景用并发集合或同步策略。  
6. **注释**：类、接口、public 方法写清 **用途、参数、返回值、异常**；复杂逻辑分段注释「为什么这样写」。  
7. **安全**：SQL 防注入（MyBatis `#{}`）；接口鉴权与数据权限校验（医生只能看自己的患者等）。  
8. **事务**：事务范围尽量小；远程调用一般在事务外或最终一致性（MQ）。  

---

## 十三、交付物清单（建议）

- [ ] 各服务源码与父 POM  
- [ ] `docker-compose` 或 K8s 部署说明（含 Nacos、Redis、MQ、MySQL）  
- [ ] Navicat 可执行的 `schema.sql` / `data.sql`  
- [ ] ApiFox 导出的 OpenAPI  
- [ ] 前端 `.env.example`  
- [ ] `README.md`：启动顺序、端口表、默认账号  

---

## 十四、关联文件

- AI 辅助实现时可配合使用：同目录下的 **`HOSPITAL_REGISTRATION_CURSOR_PROMPT.md`**（完整 Prompt 模板）。

---

*文档版本：v1.0 | 与业务需求变更时请递增版本并记录修订说明。*
