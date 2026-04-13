# Frontend-Backend Call Flow

## How To Read
- `View/Component -> api/*.js -> Controller#method -> Service`
- `store/*.js` is usually called by `router` or `views`, then calls `api/*.js`.

## Global Entry Flow
- `router/index.js` guard -> `store/user.loadMe()` -> `api/user.fetchMe()` -> `UserController#me` -> `UserService#profile`
- `MainLayout.vue` notifications poll -> `api/notification.fetchUnreadNotifications()` -> `NotificationController#unread` -> `NotificationService#unread`

## Mall Flows
- `Home.vue`
  - product list -> `api/product.fetchProducts` -> `ProductController#page` -> `ProductService#page`
  - categories -> `api/product.fetchCategories` -> `CategoryController#list`
  - recommend -> `api/ai.fetchRecommend` -> `AIController#recommend` -> `RecommendService#recommend`
  - seckill demo event -> `api/seckill.fetchPublicEvent` -> `SeckillController#publicEvent`
- `Search.vue`
  - search/filter -> `api/product.fetchProducts` -> `ProductController#page`
  - categories -> `api/product.fetchCategories` -> `CategoryController#list`
- `ProductDetail.vue`
  - detail -> `api/product.fetchProduct` -> `ProductController#detail` -> `ProductService#detail`
  - add cart -> `api/cart.addCart` -> `CartController#add` -> `CartService#add`
  - seckill flow -> `api/seckill.*` -> `SeckillController` -> `SeckillService/SeckillTxService`
- `Cart.vue`
  - cart read/update -> `api/cart.*` -> `CartController` -> `CartService`
  - checkout -> `api/order.createOrderFromCart` -> `OrderController#fromCart` -> `OrderService#createFromCart`
- `OrderList.vue`
  - list/pay/detail -> `api/order.*` -> `OrderController` -> `OrderService`
- `Profile.vue`
  - me/update/avatar -> `api/user.*` -> `UserController` -> `UserService`/`FileStorageService`
- `Login.vue`
  - login -> `store/user.login` -> `api/user.login` -> `UserController#login` -> `UserService#login`
  - then cart refresh -> `store/cart.refresh` -> `api/cart.fetchCart` -> `CartController#list`
- `Register.vue`
  - register -> `api/user.register` -> `UserController#register` -> `UserService#register`
- `AIChatBot.vue`
  - chat -> `api/ai.chatAi` -> `AIController#chat` -> `AiChatService` -> `OpenAiCompatibleChatClient` (if enabled)

## Admin Flows
- `AdminProducts.vue` -> `api/admin.*product*` -> `AdminController` -> `ProductMapper/ProductService/FileStorageService`
- `AdminUsers.vue` -> `api/admin.adminUsers/adminUserStatus` -> `AdminController#users/#userStatus`
- `AdminOrders.vue` -> `api/admin.adminOrders/adminShip` -> `AdminController#orders/#ship` -> `OrderService#ship`
- `SeckillEventManage.vue` -> `api/admin.adminSeckill*` -> `AdminController#seckill*` -> `SeckillService` (redis sync)
- `AdminDashboard.vue`
  - monitor -> `api/admin.adminMonitorStats` -> `AdminController#monitorStats` -> `MonitorService`
  - statistics -> `api/admin.adminStatisticsDashboard` -> `AdminController#statisticsDashboard` -> `AdminStatisticsService`

## Time Sync
- `SeckillCountdown.vue` -> `api/seckill.fetchServerNow` -> `CommonController#now`

