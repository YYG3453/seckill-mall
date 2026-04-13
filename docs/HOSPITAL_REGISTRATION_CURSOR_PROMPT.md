# 医院挂号系统 — Cursor / AI 开发 Prompt 模板

> 将下方 **「可直接复制的 Prompt」** 整段粘贴到 Cursor Chat / Composer 或同类 AI 工具中，按需替换 `{占位符}`。  
> 与 **`HOSPITAL_REGISTRATION_SYSTEM_SPEC.md`** 配合使用效果更佳。

---

## 可直接复制的 Prompt（中文版）

```text
你是一名资深全栈工程师，正在从零实现一套「医院挂号系统」教学/生产级演示项目。请严格按以下约束设计与编码。

【总目标】
多角色系统：患者前台（登录注册、科室、医生、号源、下单、我的挂号）；医生工作台（今日患者、接诊、病历）；管理后台（科室/医生/排班、挂号审核与统计）。代码需便于学习：每个类、重要方法、复杂分支、SQL、MQ 消费逻辑都要有中文注释，说明「做什么、为什么、注意点」。Java 代码风格遵循《阿里巴巴 Java 开发手册》（命名、常量、异常、日志、集合、安全、事务边界等）。

【后端 — Spring Cloud Alibaba 微服务】
- Spring Cloud + Spring Cloud Alibaba
- Nacos：服务注册发现 + 配置中心（数据源、Redis、MQ、JWT 密钥等禁止硬编码进仓库，用配置中心 + 本地 profile 覆盖）
- Spring Cloud Gateway：统一入口、路由、JWT 校验 Filter、与 Sentinel 集成
- OpenFeign：服务调用 + Sentinel 熔断降级
- MyBatis-Plus + MySQL 8.0
- Redis：热点缓存（科室/医生/号源查询）；分布式锁实现号源扣减防超卖（SET NX EX + 安全释放）
- RocketMQ 5.x：挂号成功等事件异步处理（统计、通知占位、审计）；消费幂等
- Sentinel：网关与服务侧限流、熔断、降级，返回统一 Result
- JWT：登录签发；Gateway 解析后传递用户上下文
- Lombok、Hutool、Jakarta Validation（Controller 入参 @Valid）

建议服务拆分（可合并但中间件必须真实使用）：gateway、auth、user、dept-doctor、schedule、registration、medical-record、admin；公共模块 common-core（统一 Result、异常、错误码）、common-feign 等。

【前端 — Vue3 + Element Plus】
- Vue 3 + Vite + Element Plus + Axios + Vue Router + Pinia
- ECharts：管理端挂号统计（趋势、科室占比等）
- 目录分 patient / doctor / admin 模块；路由守卫按角色控制
- 请求拦截器带 Bearer Token，响应统一处理业务 code

【中间件 — 必须使用】
Nacos、Redis、MySQL 8.0、RocketMQ 5.x、Sentinel。

【开发工具约定】
IDEA、Navicat、ApiFox、Git；接口用 ApiFox/OpenAPI 维护。

【核心业务 — 挂号下单】
高并发下：先查号源（可缓存），下单时 Redis 分布式锁保证同一号源不重复卖出；本地事务写订单与扣减号源；成功后发 MQ；失败与超时要有明确错误码与前端提示。所有对外 API 统一响应结构（含 traceId）。

【输出要求】
1. 先给出仓库/模块目录树与端口规划表，再分文件输出关键代码。
2. Java：Controller / Service / Mapper 分层清晰；禁止大段逻辑堆在 Controller。
3. 每段代码带学习向中文注释（类注释、方法 JavaDoc、关键步骤行内注释）。
4. 给出 MySQL 核心表 DDL 片段与 Redis Key、MQ Topic 命名规范。
5. 说明本地启动顺序：MySQL → Redis → RocketMQ → Nacos → 各微服务 → Gateway → 前端。

当前任务：{在此填写：例如「生成父 POM 与 gateway 模块骨架」或「实现 registration-service 下单接口」}
```

---

## 英文版 Prompt（可选）

```text
You are a senior full-stack engineer. Build a hospital appointment (registration) system using Spring Cloud Alibaba microservices and Vue 3 + Element Plus.

Backend: Nacos (discovery + config), Gateway, Feign + Sentinel, MyBatis-Plus, MySQL 8, Redis (cache + distributed locks for slot booking), RocketMQ 5.x (async events), JWT auth, Lombok, Hutool, Jakarta Validation.

Frontend: Vue 3, Vite, Element Plus, Axios, Router, Pinia, ECharts for admin statistics.

Mandatory middleware: Nacos, Redis, MySQL, RocketMQ, Sentinel.

Comment every non-trivial class/method/block in Chinese for learning; follow Alibaba Java coding guidelines. Use unified API Result and traceId.

Task: {fill in your task}
```

---

## 使用提示

1. **第一次对话**：Prompt 末尾任务写「生成架构说明 + 父工程与各子模块空壳 + docker-compose 草稿」。  
2. **迭代**：每次只选一个服务或一个前端模块，避免单次输出过大。  
3. **规范检查**：要求 AI 「对照 HOSPITAL_REGISTRATION_SYSTEM_SPEC.md 检查是否遗漏中间件与模块」。  

---

*与 `HOSPITAL_REGISTRATION_SYSTEM_SPEC.md` 同步维护。*
