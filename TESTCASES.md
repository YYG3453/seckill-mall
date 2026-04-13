# 核心功能测试用例（TESTCASES.md）

## 1. 正常秒杀流程（前端操作）

1. 打开 <http://localhost:8080>，使用 `user1 / 123456` 登录。
2. 进入关联秒杀的商品详情（演示数据中为 **商品 ID 3**，名称含「秒杀款」）。
3. 确认秒杀倒计时处于「距结束」且按钮为「立即抢购」后，点击抢购。
4. 在「订单」列表中应出现 **待支付** 订单；点击 **模拟支付**，状态变为 **已支付**。

## 2. 超卖测试（并发）

使用 Apache Bench（示例）对秒杀接口加压前，需先登录获取 Cookie，再调用获取 path 接口拿到动态 path（以下为思路示例，实际需替换 `JSESSIONID`、`path`、`itemId`）：

```bash
# 登录（保存 Cookie 到文件）
curl -c cookies.txt -X POST "http://localhost:8081/api/user/login" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"user1\",\"password\":\"123456\"}"

# 获取秒杀 path（演示场次 itemId=1）
curl -b cookies.txt "http://localhost:8081/api/seckill/path/1"

# 使用返回的 path 并发（将 PATH 替换为上一步返回的 path）
ab -n 50 -c 50 -p nul -T "application/x-www-form-urlencoded" ^
  -C "JSESSIONID=xxxx" "http://localhost:8081/api/seckill/PATH/do?itemId=1"
```

**验收**：`seckill_event.seckill_stock` 不为负数；成功下单数量不超过初始秒杀库存；Redis `seckill:stock:1` 与数据库最终一致。

## 3. 限流测试

同一用户 1 分钟内对 `/api/seckill/{path}/do` 请求超过 5 次（或单 IP 超过 10 次）时，应返回业务提示 **「请求过于频繁」**（需每次先获取有效 path，或使用脚本固定 Cookie 重复 POST）。

## 4. 路径隐藏测试

- 直接访问不存在的路径，例如：  
  `POST http://localhost:8081/api/seckill/fakepath/do?itemId=1`  
  在未持有合法 `seckill:path:user:item` 时应失败（业务错误提示路径无效或类似信息）。
- 先 `GET /api/seckill/path/{itemId}` 再使用返回的 path 调用 `POST /api/seckill/{path}/do?itemId=` 应可正常进入秒杀逻辑（在时间与库存允许的前提下）。

## 5. AI 推荐测试

1. 使用 `user1` 登录后浏览若干商品（产生 `view` 日志）。
2. 调用：  
   `GET http://localhost:8081/api/ai/recommend`（需携带登录 Cookie，或通过前端首页「为你推荐」观察）。  
3. 返回列表应与浏览/购买过的商品在分类、标签、价格档位上存在一定相似性（简化 Jaccard 方案）。

## 6. 秒杀提醒测试

1. 登录后，在秒杀商品详情页点击 **提醒我**（写入 `seckill_reminder`）。
2. 将某场次 `start_time` 调整为「当前时间后 2～5 分钟」（可通过后台编辑或改库），等待定时任务（每 2 分钟扫描一次）。
3. 前端每 30 秒轮询 `GET /api/notifications/unread`，应出现「即将开始」类通知；消息入口可点开查看。

## 7. 购物车与订单

1. 登录后将商品加入购物车，勾选后 **结算**，生成待支付订单。
2. 等待超过 30 分钟未支付（或改库将 `create_time` 提前），运行后端定时任务周期后订单应变更为 **已取消**，且普通商品 `product.stock` 回补。

---

更多接口可在浏览器开发者工具 Network 中结合 Cookie 查看请求与响应（统一结构 `code / msg / data`）。
