# 启动说明（STARTUP.md）

## 环境要求

- JDK 17
- MySQL 8.0
- Redis 6.x（或兼容的 Redis 服务）
- Node.js 16+

## 数据库初始化

1. 在 MySQL 中执行 `db/schema.sql` 创建库表。
2. 执行 `db/data.sql` 导入演示数据（含管理员 `admin / admin123`、用户 `user1 / 123456`、秒杀场次等）。

## Redis 配置

编辑 `backend/src/main/resources/application.yml` 中的：

- `spring.redis.host`
- `spring.redis.port`
- `spring.redis.password`（无密码则留空）

## 后端启动

```bash
cd backend
mvn clean spring-boot:run
```

默认端口 **8081**，API 前缀 **`/api`**。

## 前端启动

```bash
cd frontend
npm install
npm run dev
```

默认端口 **8080**。Vite 已将 `/api` 代理到 `http://localhost:8081`。

## 跨域与会话

- 后端已配置 CORS，允许来源 `http://localhost:8080`，并开启 `allowCredentials`。
- 认证使用 **Spring Session + Redis**，浏览器通过 **Cookie（JSESSIONID）** 维持登录；前端 Axios 已设置 `withCredentials: true`。

## 默认访问

- 前端：<http://localhost:8080>
- 后端健康检查：任意 `/api/common/now` 应返回 JSON。

## 常见问题

- **登录后仍提示未登录**：确认前端走 Vite 代理（不要直接把 Axios `baseURL` 指到 8081 且未带 Cookie）。
- **秒杀库存为 0**：首次启动需连上 Redis；后台会 `@PostConstruct` 预热未结束场次到 Redis。若 Redis 清空，请重启后端或重新保存场次以同步。
