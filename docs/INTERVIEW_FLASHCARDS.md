# 面试速记卡（610 条 · 每答 2～3 句口语版）

> 用法：遮住 **A** 先自答，再对照。和 `INTERVIEW_QA.md` 互补，本文件偏「短、密、好背」。  
> 编号连续到 **610**，可按天拆 50～80 条滚动复习；**311～450** 为新增基础加强段。

---

## Java 基础（001～040）

**001. Q: `==` 和 `equals` 区别？**  
**A:** `==` 比的是引用或基本类型值。`equals` 比的是对象语义相等，String 常用重写。业务判等一般用 `equals` 或工具类。

**002. Q: `String` 为什么不可变？**  
**A:** 内部 `char[]/byte[]` 不暴露可变接口，利于常量池复用和安全。频繁拼接用 `StringBuilder` 省对象。

**003. Q: `final` 修饰类/方法/变量各啥意思？**  
**A:** 类不可继承，方法不可重写，变量引用不可换。注意 `final` 对象内容仍可能变，除非字段也不可变。

**004. Q: 接口和抽象类怎么选？**  
**A:** 多重能力用接口，模板复用带状态用抽象类。Java8+ 接口可有默认方法，边界更模糊，看团队规范。

**005. Q: 重载和重写区别？**  
**A:** 重载同方法名不同参数，编译期绑定。重写子类改父类方法，运行期多态。秒杀里子类少，但框架里很常见。

**006. Q: `static` 能访问实例成员吗？**  
**A:** 不能直接访问，要先有对象引用。`static` 属于类，生命周期跟类走。

**007. Q: `try-with-resources` 干啥的？**  
**A:** 自动关闭实现了 `AutoCloseable` 的资源，避免忘关流。比手写 `finally` 更干净。

**008. Q: 异常 `checked` vs `unchecked`？**  
**A:** checked 必须处理或声明，`IOException` 那种。`RuntimeException` 可不强制。业务里自定义一般继承 `RuntimeException`。

**009. Q: `hashCode` 和 `equals` 要一起重写吗？**  
**A:** 用作 `HashMap` key 时必须一致契约：`equals` 相等则 `hashCode` 相等。只重写一个会出诡异 bug。

**010. Q: `ArrayList` 扩容机制大概怎样？**  
**A:** 数组满了按策略扩容（常见 1.5 倍），拷贝数组。初始化若能估容量就 `new ArrayList(cap)` 少扩容。

**011. Q: `HashMap` 1.8 链表过长会怎样？**  
**A:** 会树化（红黑树）降查找复杂度。负载因子默认 0.75 平衡空间与时间。

**012. Q: `ConcurrentHashMap` 为什么线程安全？**  
**A:** 分段/CAS+ synchronized 等实现并发读写。秒杀里若共享计数可用，但要懂内存可见性。

**013. Q: `volatile` 能保证原子性吗？**  
**A:** 不能保证 `i++` 这种复合操作原子。它主要保可见性+禁止部分重排，常作状态标志位。

**014. Q: `synchronized` 锁的是什么？**  
**A:** 实例方法锁 `this`，静态方法锁 `Class` 对象，代码块可指定对象。本项目订单号生成用它保互斥。

**015. Q: `ReentrantLock` 比 `synchronized` 多啥能力？**  
**A:** 可尝试锁、可中断、可公平锁、条件变量。简单场景 `synchronized` 够用，复杂协调用 `Lock`。

**016. Q: 线程池核心参数有哪些？**  
**A:** 核心线程数、最大线程、队列、拒绝策略、线程工厂。队列满了触发拒绝策略，别用无限队列掩盖问题。

**017. Q: 为什么不用 `Executors` 默认工厂乱建池？**  
**A:** 有些默认配置队列过大或线程无界，容易把内存打爆。生产一般自定义 `ThreadPoolExecutor`。

**018. Q: `ThreadLocal` 使用注意啥？**  
**A:** 线程隔离存上下文，用完要 `remove`，否则线程池复用可能内存泄漏或脏数据。

**019. Q: 深拷贝和浅拷贝？**  
**A:** 浅拷贝共享内部引用对象，深拷贝递归复制。DTO 层拷贝要注意别共享可变集合引用。

**020. Q: Java 8 `Stream` 适合干啥？**  
**A:** 链式处理集合，可读性好。注意别在流里做副作用 IO，大集合要考虑性能。

**021. Q: `Optional` 用来干啥？**  
**A:** 显式表达可能为空，减少 NPE。别滥用 `get()`，用 `orElse`/`ifPresent`。

**022. Q: 序列化 `serialVersionUID` 干啥？**  
**A:** 版本兼容校验，反序列化类结构变了可能不匹配。`SessionUser` 这种进 Session 的类要稳定。

**023. Q: 反射优缺点？**  
**A:** 灵活但破坏封装、性能差、类型不安全。框架里常用，业务代码少用手搓反射。

**024. Q: 注解本质是什么？**  
**A:** 元数据，编译期或运行期由框架处理。项目里 `@RateLimit` 给 AOP 用。

**025. Q: `equals` 里用 `instanceof` 还是 `getClass`？**  
**A:** 需要子类对称相等用 `instanceof` 模式，严格类型用 `getClass`。按 `equals` 契约选。

**026. Q: `BigDecimal` 为啥算钱？**  
**A:** 避免 `double` 二进制浮点误差。秒杀价、订单金额都用它更稳。

**027. Q: `LocalDateTime` 和 `Instant` 区别？**  
**A:** 前者无时区语义，后者是时间线瞬时点。存库常配合时区配置统一。

**028. Q: 什么是装箱拆箱？**  
**A:** 基本类型和包装类自动转换。注意 `==` 比较 Integer 缓存坑。

**029. Q: `enum` 单例为啥安全？**  
**A:** 类加载初始化一次，防反射多实例。比懒汉双重检查更省心。

**030. Q: `clone()` 为啥不常用？**  
**A:** 默认浅拷贝，契约弱。更常用拷贝构造或 Bean 拷贝工具。

**031. Q: 什么是 SPI？**  
**A:** Service Provider Interface，JDK/框架插件机制。面试常问 JDBC Driver 加载。

**032. Q: `ClassLoader` 双亲委派是啥？**  
**A:** 先让父加载器加载，避免核心类被覆盖。破坏委派要非常谨慎。

**033. Q: `StringBuilder` 线程安全吗？**  
**A:** 不安全，但单线程更快。多线程拼接用 `StringBuffer` 或并发工具。

**034. Q: `fail-fast` 迭代器啥意思？**  
**A:** 遍历中结构被改会抛异常。并发修改要换并发集合或加锁。

**035. Q: `Comparable` 和 `Comparator`？**  
**A:** 前者对象自身排序，后者外部比较器。排序需求多变时用 `Comparator`。

**036. Q: Java 内存模型 JMM 讲一句？**  
**A:** 规定多线程下主内存/working memory 的读写规则，`happens-before` 是核心。

**037. Q: `happens-before` 举两个？**  
**A:** `volatile` 写先于读可见；`synchronized` 解锁先于后续加锁同监视器的可见性。

**038. Q: 伪共享是啥？**  
**A:** 多核缓存行竞争导致性能掉。高并发计数器设计会考虑填充，业务项目少见。

**039. Q: `finalize` 为啥不推荐？**  
**A:** 不确定时机、影响 GC、可能复活对象。用 `try-with-resources` 或 cleaner 模式。

**040. Q: 记录类 `record` 了解吗？**  
**A:** Java16+ 不可变数据载体，自动生成构造器、`equals` 等。新项目可替代部分 DTO 样板。

---

## Java 并发与 JVM（041～075）

**041. Q: CAS 是啥？**  
**A:** Compare-And-Swap，无锁原子更新。`AtomicLong` 底层靠 CAS，ABA 问题可用版本号解决。

**042. Q: ABA 问题啥场景？**  
**A:** 值从 A 变 B 又变 A，CAS 以为没变。栈/链表操作要小心，可用 `AtomicStampedReference`。

**043. Q: 自旋锁适合啥？**  
**A:** 锁持有时间极短、线程竞争不激烈。过长自旋浪费 CPU。

**044. Q: 偏向锁/轻量锁/重量锁听过吗？**  
**A:** JVM 锁升级路径，从无竞争到竞争加剧逐步变重。面试背概念，线上看 JFR 更实在。

**045. Q: `CountDownLatch` 干啥？**  
**A:** 等一组任务完成再继续。适合启动检查、并行分段汇总。

**046. Q: `CyclicBarrier` 和 `CountDownLatch` 区别？**  
**A:** Barrier 可循环重用，线程互相等待齐步走。Latch 一次性计数。

**047. Q: `Semaphore` 使用场景？**  
**A:** 限流资源池，比如最多 N 个并发进临界区。和项目里 Redis 限流是不同层面。

**048. Q: `CompletableFuture` 好处？**  
**A:** 组合异步任务链，回调清晰。注意线程池隔离，别用默认 `ForkJoinPool` 扛 IO。

**049. Q: 线程池任务拒绝策略有哪些？**  
**A:** 抛异常、调用者跑、丢弃、丢弃最老。项目 `CallerRunsPolicy` 背压一种思路。

**050. Q: 什么是活锁、死锁？**  
**A:** 死锁互相等资源；活锁一直在让步却进不去。排查死锁用线程 dump。

**051. Q: JVM 堆栈区别？**  
**A:** 堆放对象，栈放栈帧局部变量。OOM 分堆溢出和栈溢出不同。

**052. Q: GC Root 有哪些？**  
**A:** 栈引用、静态字段、JNI、CL 等。可达性分析从 GC Root 出发。

**053. Q: Minor GC vs Full GC？**  
**A:** 新生代回收 vs 整堆或更多区域回收。Full GC STW 长要警惕。

**054. Q: CMS 和 G1 区别一句？**  
**A:** CMS 老年代并发收集器已过时；G1 分区可预测停顿，Java9+ 默认趋势。

**055. Q: 强软弱虚引用？**  
**A:** 强引用不回收；软内存紧回收；弱下次 GC 回收；虚用于跟踪回收。弱引用常见于缓存。

**056. Q: `OOM` 常见类型？**  
**A:** Java heap、Metaspace、Direct buffer、unable create native thread。定位看 dump 和日志。

**057. Q: `jstack` 干啥？**  
**A:** 打线程栈，查死锁、卡死、热点栈帧。

**058. Q: `jmap` 干啥？**  
**A:** 生成堆 dump 或看堆摘要。大堆 dump 注意磁盘和时间。

**059. Q: JIT 是啥？**  
**A:** 热点代码编译成本地机器码提速。别过早微优化，先测再优化。

**060. Q: 逃逸分析有啥用？**  
**A:** JVM 可能栈上分配、锁消除。理论题多，业务以压测为准。

**061. Q: `synchronized` 可重入吗？**  
**A:** 可重入，同线程多次获取同监视器不会死锁。

**062. Q: `volatile` 能替代锁吗？**  
**A:** 不能解决复合操作原子性。只适合一写多读可见性场景。

**063. Q: 线程间通信方式？**  
**A:** `wait/notify`、`Lock` 条件、`BlockingQueue`、`CompletableFuture`。队列削峰常用。

**064. Q: 什么是守护线程？**  
**A:** 不影响 JVM 退出，比如 GC 线程。业务线程一般非守护。

**065. Q: `interrupt` 怎么用？**  
**A:** 协作式中断标志，阻塞 IO/锁要配合可中断 API。别乱吞中断。

**066. Q: `Thread.sleep` 释放锁吗？**  
**A:** 不释放 `synchronized` 锁，只暂停当前线程。

**067. Q: `yield` 干啥？**  
**A:** 提示调度器让出 CPU，不保证。少用，靠队列和池更稳。

**068. Q: 并发集合 `CopyOnWriteArrayList` 适合啥？**  
**A:** 读多写少，写时复制数组。写频繁别用，成本高。

**069. Q: `BlockingQueue` 为啥削峰？**  
**A:** 生产者快消费者慢时排队缓冲，配合线程池消费。秒杀可异步化思路之一。

**070. Q: 乐观锁 vs 悲观锁？**  
**A:** 乐观靠版本号/CAS 重试，悲观直接锁住。DB 条件更新类似乐观思路。

**071. Q: 什么是内存屏障？**  
**A:** CPU/编译器重排限制，`volatile` 和锁隐含屏障。背概念即可。

**072. Q: `happens-before` 和可见性关系？**  
**A:** 若 A happens-before B，则 A 的写入对 B 可见。并发正确性核心。

**073. Q: 线程池为啥不建议 `Executors.newCachedThreadPool`？**  
**A:** 可能创建过多线程把机器打挂。生产自定义参数。

**074. Q: `Future.get` 注意啥？**  
**A:** 会阻塞，要设超时。异常会被包装成 `ExecutionException`。

**075. Q: 如何定位 CPU 飙高？**  
**A:** `top` 找进程线程，`jstack` 看栈，结合 profiler。面试说思路即可。

---

## Spring / Spring Boot（076～120）

**076. Q: IOC 是什么？**  
**A:** 控制反转，对象创建交给容器管，类只声明依赖。Spring 通过注入组装。

**077. Q: DI 有哪些方式？**  
**A:** 构造器注入推荐，字段/Setter 也可。构造器注入依赖清晰、易测。

**078. Q: `@Component` `@Service` `@Repository` 区别？**  
**A:** 都是组件扫描，`@Service/@Repository` 语义分层，异常转换历史上 `@Repository` 有特殊支持。

**079. Q: `@Autowired` 和 `@Resource`？**  
**A:** 前者 Spring 按类型优先，后者 JSR250 按名字。团队统一一种避免混。

**080. Q: 循环依赖 Spring 咋处理？**  
**A:** 单例 setter 注入可用三级缓存，构造器循环依赖会挂。最好从设计上断开环。

**081. Q: Bean 作用域有哪些？**  
**A:** singleton、prototype、request、session 等。默认单例，注意有状态 Bean 别乱单例。

**082. Q: `@Configuration` 和 `@Bean`？**  
**A:** 配置类里定义工厂方法创建 Bean，适合第三方对象装配。

**083. Q: `@Value` 和 `@ConfigurationProperties`？**  
**A:** 零散配置用 `@Value`，一组前缀配置用 properties 类更清晰，类型安全。

**084. Q: Spring Boot 自动配置原理一句？**  
**A:** `META-INF/spring.factories`/`AutoConfiguration` 条件装配 `@ConditionalOn*`。

**085. Q: `starter` 是什么？**  
**A:** 依赖打包，把常用库和自动配置拉进来，开箱即用。

**086. Q: Actuator 了解吗？**  
**A:** 提供健康检查、指标端点，生产监控基础。演示项目可后续加。

**087. Q: `@Transactional` 默认传播？**  
**A:** `REQUIRED`，加入当前事务或新建。读代码要关注传播和隔离级别。

**088. Q: 事务失效常见原因？**  
**A:** 自调用、非 public、异常被吞、数据库引擎不支持事务。排查很常考。

**089. Q: `@Transactional` rollbackFor？**  
**A:** 默认只对 RuntimeException 回滚，checked 异常要指定 `rollbackFor`。

**090. Q: Spring MVC 处理流程？**  
**A:** DispatcherServlet -> HandlerMapping -> Adapter -> Controller -> ViewResolver/消息转换。

**091. Q: `@RequestParam` `@PathVariable` `@RequestBody`？**  
**A:** 表单参数、路径变量、JSON body。项目购物车用 `x-www-form-urlencoded` 配 `@RequestParam`。

**092. Q: 全局异常处理怎么写？**  
**A:** `@RestControllerAdvice` + `@ExceptionHandler` 统一返回结构。

**093. Q: `RestTemplate` vs `WebClient`？**  
**A:** 同步阻塞 vs 响应式。项目 AI 调用用 `RestTemplate` 足够直观。

**094. Q: Spring Validation 怎么用？**  
**A:** DTO 上加注解，`@Valid` 触发，`MethodArgumentNotValidException` 统一处理。

**095. Q: 拦截器和 Filter 顺序？**  
**A:** Filter 在 Servlet 层更外，Interceptor 在 Spring MVC 内。认证日志分层不同。

**096. Q: CORS 是什么？**  
**A:** 浏览器跨域安全策略，服务端要返回允许源和方法。项目 dev 配 `allowCredentials`。

**097. Q: `multipart` 上传限制在哪配？**  
**A:** `spring.servlet.multipart.max-file-size`。还要和业务校验一致。

**098. Q: `@Scheduled` 注意啥？**  
**A:** 默认单线程串行，长任务阻塞其他任务。可配线程池或拆任务。

**099. Q: Spring 里如何读配置？**  
**A:** `@Value`、`@ConfigurationProperties`、`Environment`。敏感信息别写死仓库。

**100. Q: Bean 生命周期大概？**  
**A:** 实例化、属性注入、Aware、初始化前后、销毁。理解即可，背核心节点。

**101. Q: `@Primary` `@Qualifier` 干啥？**  
**A:** 多实现注入时消歧义。一个默认一个按名注入。

**102. Q: Spring 事件机制？**  
**A:** `ApplicationEventPublisher` 发布事件，监听异步解耦。小项目少用，大系统常见。

**103. Q: `@Async` 要什么？**  
**A:** `@EnableAsync` + 线程池 Bean。注意异常处理和返回值 `Future`。

**104. Q: Spring 为啥不推荐字段注入？**  
**A:** 不利于测试、隐藏依赖、可能 NPE。构造器注入更明确。

**105. Q: `ApplicationContext` vs `BeanFactory`？**  
**A:** Context 功能更全，事件、国际化、AOP 集成。实际都用 Context。

**106. Q: `@Scope("request")` 适用？**  
**A:** Web 请求级 Bean，注意别在单例里注入 request Bean 出错。

**107. Q: Spring Boot 如何改端口？**  
**A:** `server.port`，或用命令行参数覆盖。项目后端 8081。

**108. Q: 如何做配置多环境？**  
**A:** `application-dev.yml` + `spring.profiles.active`。演示项目可简化。

**109. Q: `@ConditionalOnProperty` 干啥？**  
**A:** 配置开关控制 Bean 是否加载，适合做 feature toggle。

**110. Q: Spring 事务和 Redis 事务一样吗？**  
**A:** 不一样。Redis 事务不是传统 ACID，Lua 才原子。别混概念。

**111. Q: `@ControllerAdvice` 能返回啥？**  
**A:** 可返回 `ResponseEntity` 或统一 body，看项目约定。

**112. Q: JSON 日期格式谁控制？**  
**A:** Jackson 全局配置或字段注解。项目 `write-dates-as-timestamps: false`。

**113. Q: Spring 里如何拿到当前请求？**  
**A:** `RequestContextHolder`。AOP/工具类里常用，注意异步线程丢失上下文。

**114. Q: `HandlerInterceptor#preHandle` 返回 false 会怎样？**  
**A:** 中断链路，不进入 Controller。登录拦截就这么干。

**115. Q: Spring MVC 如何做文件下载？**  
**A:** `ResponseEntity<Resource>` 或流写出，设置 `Content-Disposition`。

**116. Q: `@ResponseStatus` 干啥？**  
**A:** 标记异常映射 HTTP 状态码，配合异常处理。

**117. Q: Spring Boot 如何打包运行？**  
**A:** `mvn package` 出 jar，`java -jar`。Docker 化另说。

**118. Q: DevTools 了解吗？**  
**A:** 开发热重启，生产别引入。面试提一句即可。

**119. Q: Spring 为啥流行？**  
**A:** 生态全、约定大于配置、社区大。企业级标配。

**120. Q: 你项目为啥用 2.7 而不是 3.x？**  
**A:** 稳定兼容、资料多；升级 3 要 Jakarta 包名等迁移成本。面试诚实说版本选型。

---

## Spring AOP / 安全 / Session（121～145）

**121. Q: AOP 核心概念？**  
**A:** 切面、连接点、切点、通知、织入。业务横切关注点抽出来。

**122. Q: 五种通知类型？**  
**A:** before/after/afterReturning/afterThrowing/around。around 最强也最危险。

**123. Q: JDK 动态代理 vs CGLIB？**  
**A:** JDK 基于接口，CGLIB 基于子类。Spring 默认有接口走 JDK。

**124. Q: AOP 自调用会失效吗？**  
**A:** 同类内部调用绕不过代理，常见失效点。用注入自己或拆 Bean。

**125. Q: `@Aspect` 如何切注解？**  
**A:** `@Before("@annotation(xxx)")` 绑定方法注解参数。

**126. Q: BCrypt 特点？**  
**A:** 自适应成本哈希，抗彩虹表。项目密码存储用它。

**127. Q: 为啥密码不能明文？**  
**A:** 泄露即完蛋。哈希+盐是底线，配合 HTTPS 传输。

**128. Q: Session 固定攻击听过吗？**  
**A:** 登录后应轮换 session id。框架一般可配置，演示项目可提改进点。

**129. Q: SameSite cookie 干啥？**  
**A:** 降低 CSRF 风险。项目配 `lax` 兼顾部分跨站场景。

**130. Q: CSRF 在前后端分离怎么防？**  
**A:** 常用 Token 或双重 cookie，或纯 API + 无 cookie 方案改 JWT。看架构。

**131. Q: XSS 如何防？**  
**A:** 输入转义、CSP、HttpOnly cookie。前端 `v-html` 慎用。

**132. Q: 文件上传如何防路径穿越？**  
**A:** normalize 路径、校验前缀、白名单扩展名。项目 `FileStorageService` 有校验思路。

**133. Q: 如何做接口鉴权？**  
**A:** Session、JWT、网关鉴权。项目 Session + 拦截器。

**134. Q: RBAC 是什么？**  
**A:** 角色访问控制，用户-角色-权限。项目简化为 `admin/user`。

**135. Q: 如何做审计日志？**  
**A:** 记录谁何时做了什么关键操作。后台操作可扩展审计表。

**136. Q: Spring Security 和本项目关系？**  
**A:** 项目只用 `spring-security-crypto` 做密码哈希，未用完整 Security 过滤器链。

**137. Q: 如果上 Spring Security 你会怎么做？**  
**A:** 用 SecurityFilterChain 配表单/会话或 JWT，逐步替换手写拦截器。

**138. Q: 会话集群为啥要 Redis？**  
**A:** 多台机器共享 session，负载均衡不丢登录态。

**139. Q: Session timeout 配置意义？**  
**A:** 平衡安全与体验。项目 30 分钟示例。

**140. Q: 登出为啥 invalidate？**  
**A:** 服务端会话失效，防止会话复用。

**141. Q: 如何做幂等登录？**  
**A:** 登录本身重复无害；关键写操作要幂等 token。

**142. Q: 如何做接口防重放？**  
**A:** nonce+timestamp+签名 或短期 token。秒杀可扩展。

**143. Q: 限流和熔断区别？**  
**A:** 限流保护自身；熔断快速失败保护依赖。高可用体系里常一起。

**144. Q: 如何做灰度发布？**  
**A:** 网关流量标签、配置中心开关、数据库兼容双写。大话题分阶段答。

**145. Q: 如何做密钥管理？**  
**A:** 环境变量、Vault、KMS。别把 key 提交 git。

---

## MyBatis / MyBatis-Plus（146～165）

**146. Q: `#{}` 和 `${}`？**  
**A:** `#` 预编译防注入，`$` 字符串拼接危险。动态表名才谨慎用 `$`。

**147. Q: MyBatis 一级二级缓存？**  
**A:** 一级 SqlSession 默认，二级 mapper 级默认关。高并发常关或用 Redis。

**148. Q: MP 分页怎么做？**  
**A:** `Page` + 分页插件拦截器。项目 `MybatisPlusConfig` 注册。

**149. Q: `LambdaQueryWrapper` 好处？**  
**A:** 类型安全字段引用，重构友好。注意 SQL 组合优先级。

**150. Q: `nested` 解决啥问题？**  
**A:** 给 OR 条件加括号，避免破坏 AND 语义。搜索 bug 典型。

**151. Q: `@Update` 注解 SQL 用在哪？**  
**A:** 原子扣库存 `stock >= qty` 条件更新，防负数卖出。

**152. Q: Mapper 接口为何不用实现类？**  
**A:** MyBatis 动态代理生成实现，XML 或注解绑定 SQL。

**153. Q: XML vs 注解 SQL？**  
**A:** 复杂动态 SQL XML 更清晰；简单 CRUD 注解快。

**154. Q: 什么是乐观锁插件？**  
**A:** MP 提供 `@Version` 字段自动更新。项目秒杀用 SQL 条件也算乐观思路。

**155. Q: 逻辑删除了解吗？**  
**A:** 标记删除不物理删。电商订单一般慎重，需合规与归档策略。

**156. Q: 字段映射下划线转驼峰？**  
**A:** `map-underscore-to-camel-case: true` 减少手工 alias。

**157. Q: N+1 查询问题？**  
**A:** 循环里查库。用 join 或批量 in 解决。

**158. Q: 如何打印 SQL？**  
**A:** 日志实现或 p6spy。开发开生产关或采样。

**159. Q: 批量插入怎么优化？**  
**A:** `rewriteBatchedStatements=true` + 批量 API。大数据量分页批处理。

**160. Q: MP 自动填充字段？**  
**A:** `MetaObjectHandler` 填 `createTime`。项目部分手写。

**161. Q: 为什么 `order` 表要反引号？**  
**A:** `order` 是 SQL 关键字，反引号避免解析冲突。

**162. Q: 唯一索引冲突怎么处理？**  
**A:** 捕获 `DuplicateKeyException` 转业务提示“已存在”。

**163. Q: 慢 SQL 优化第一步？**  
**A:** `EXPLAIN` 看是否走索引、是否全表扫描。

**164. Q: 覆盖索引是什么？**  
**A:** 索引列覆盖查询字段，减少回表。

**165. Q: 事务隔离级别默认？**  
**A:** MySQL InnoDB 默认 RR。秒杀高并发可能要读已提交+业务锁，看场景。

---

## MySQL 深入（166～200）

**166. Q: InnoDB vs MyISAM？**  
**A:** InnoDB 支持事务行锁，生产默认。MyISAM 少用了。

**167. Q: 聚簇索引是什么？**  
**A:** 叶子存整行数据，主键索引即聚簇。二级索引叶子存主键。

**168. Q: 最左前缀原则？**  
**A:** 联合索引从左列开始匹配，跳过左列可能不走索引。

**169. Q: 索引失效场景列几个？**  
**A:** 对列函数、隐式类型转换、前导模糊 `%xx`、OR 条件不当等。

**170. Q: 什么是回表？**  
**A:** 二级索引查到主键再回聚簇取完整行。

**171. Q: 什么是间隙锁？**  
**A:** RR 下范围锁防幻读，可能死锁。秒杀热点要警惕。

**172. Q: MVCC 一句？**  
**A:** 多版本并发控制，快照读减少锁冲突。

**173. Q: redo log vs binlog？**  
**A:** redo 崩溃恢复物理日志；binlog 主从复制逻辑日志。两阶段提交关联。

**174. Q: 什么是幻读？**  
**A:** 同一事务两次范围读行数不一致。RR+间隙锁缓解。

**175. Q: `COUNT(*)` 性能？**  
**A:** InnoDB 无精确免费计数，大表用覆盖索引或冗余计数表。

**176. Q: 分库分表何时考虑？**  
**A:** 单表行数/索引/写入瓶颈。演示项目单库即可。

**177. Q: 读写分离怎么做？**  
**A:** 主从复制+路由数据源。注意延迟读问题。

**178. Q: 主键为什么推荐自增整型？**  
**A:** 聚簇索引顺序插入页分裂少。UUID 随机插入碎片多。

**179. Q: 外键要不要？**  
**A:** 互联网常不用外键约束，改由应用保证，方便分库和批量变更。

**180. Q: 软删除对索引影响？**  
**A:** 条件常带 `deleted=0`，索引设计要包含它。

**181. Q: 订单状态用字符串还是枚举表？**  
**A:** 演示用字符串简单；生产可用字典表+约束。

**182. Q: 金额用 decimal 还是分整型？**  
**A:** decimal 直观；分整型避免小数。各有利弊。

**183. Q: 如何做分页深翻页优化？**  
**A:** `seek` 分页（上次 max id）代替大 offset。

**184. Q: 什么是数据库连接池？**  
**A:** 复用连接减开销。Spring Boot 默认 HikariCP。

**185. Q: 事务传播 `REQUIRES_NEW` 干啥？**  
**A:** 新开事务，日志审计常用，失败不影响外层。

**186. Q: `SELECT FOR UPDATE`？**  
**A:** 悲观锁读，防并发修改。秒杀热点慎用。

**187. Q: 如何避免大事务？**  
**A:** 拆步骤、异步化、减少锁持有时间。

**188. Q: 什么是死锁？如何解？**  
**A:** 互相等待；MySQL 会回滚代价小事务。业务上固定加锁顺序。

**189. Q: `EXPLAIN` 关键字段？**  
**A:** type、key、rows、Extra。看是否 Using filesort/temporary。

**190. Q: 为什么要避免 `SELECT *`？**  
**A:** 增加 IO、覆盖索引失效、网络浪费。

**191. Q: 字符集为啥 utf8mb4？**  
**A:** 支持 emoji 和完整 Unicode，避免 utf8 阉割版坑。

**192. Q: 如何做数据归档？**  
**A:** 历史订单冷表/对象存储，热库瘦身。

**193. Q: binlog 格式 statement/row？**  
**A:** row 更准，主从一致性好。运维常考。

**194. Q: 自增 ID 用完怎么办？**  
**A:** bigint 基本用不完；真要换分布式 ID。

**195. Q: 分布式 ID 方案？**  
**A:** 雪花、号段、Redis INCR。项目雪花简化版订单号。

**196. Q: 如何保证订单号唯一？**  
**A:** DB 唯一索引+生成器保证极低碰撞。失败重试。

**197. Q: 如何做乐观锁版本号？**  
**A:** `update ... where id=? and version=?`，影响行数 0 则重试。

**198. Q: 什么是读写分离延迟？**  
**A:** 从库同步慢读到旧数据。关键读走主或等待同步。

**199. Q: `JOIN` 太多表问题？**  
**A:** 性能差难优化，适当冗余或分步查。

**200. Q: 表字段 NULL 影响？**  
**A:** 索引、比较、默认值要小心。核心字段尽量 NOT NULL + 默认。

---

## Redis / 缓存 / Lua（201～245）

**201. Q: Redis 为啥快？**  
**A:** 内存 + 单线程事件循环 + IO 多路复用。适合热点读写。

**202. Q: 常见数据结构？**  
**A:** String Hash List Set ZSet Stream 等。项目用 String 库存、Hash 购物车。

**203. Q: 缓存穿透？**  
**A:** 查不存在 key 打穿 DB。用布隆过滤器或空值短缓存。

**204. Q: 缓存击穿？**  
**A:** 热点 key 过期瞬间并发打 DB。互斥锁或逻辑过期。

**205. Q: 缓存雪崩？**  
**A:** 大量 key 同时过期。随机 TTL + 集群 + 限流。

**206. Q: 热 key 问题？**  
**A:** 单 key QPS 过高打爆单分片。本地缓存+拆分 key+读写分离。

**207. Q: Redis 持久化 RDB vs AOF？**  
**A:** RDB 快照快可能丢多；AOF 更持久体积大。生产常混合。

**208. Q: Redis 事务能保证原子吗？**  
**A:** 不是严格事务，`MULTI/EXEC` 批量，中间失败不好回滚。复杂用 Lua。

**209. Q: Lua 在 Redis 里原子吗？**  
**A:** 是，脚本执行期间不穿插其他命令。秒杀核心原因。

**210. Q: `SET NX EX` 干啥？**  
**A:** 分布式锁/占位常用，注意过期续期与误删（token）。

**211. Q: Redisson 锁了解吗？**  
**A:** 封装可重入锁、看门狗续期。比自研稳，但要引入依赖。

**212. Q: Redis 和 DB 一致性策略？**  
**A:** 旁路缓存、延迟双删、订阅 binlog。秒杀用补偿更实际。

**213. Q: 过期策略？**  
**A:** 定期删除+惰性删除。理解即可。

**214. Q: 内存淘汰策略？**  
**A:** `volatile-lru` 等。缓存要配 `maxmemory` 防 OOM。

**215. Q: `Bitmap` 使用场景？**  
**A:** 签到、去重标记，省空间。

**216. Q: `HyperLogLog` 干啥？**  
**A:** 基数估算，省内存，近似统计 UV。

**217. Q: Redis Stream 和 List 区别？**  
**A:** Stream 更像消息队列，支持消费组。秒杀异步通知可扩展。

**218. Q: 如何做限流计数？**  
**A:** `INCR` + `EXPIRE` 固定窗口，或滑动窗口用 zset。

**219. Q: Redis 单线程还叫高并发？**  
**A:** 内存操作快，瓶颈常在网络 IO；6.0 多 IO 线程。

**220. Q: 大 key 问题？**  
**A:** 阻塞、迁移慢。要拆分或压缩。

**221. Q: 管道 pipeline？**  
**A:** 批量减少 RTT。注意原子性边界。

**222. Q: Redis Cluster 槽点？**  
**A:** 16384 槽分片，多 key 操作要同槽 hashtag。

**223. Q: 缓存更新先删还是先更 DB？**  
**A:** 常见 Cache Aside：先更 DB 再删缓存，或延迟双删。看一致性要求。

**224. Q: 为什么购物车放 Redis？**  
**A:** 读写快、结构 Hash 合适、会话型数据。落库也可看业务。

**225. Q: Session 放 Redis 啥结构？**  
**A:** Spring Session 序列化存 Redis，多实例共享。

**226. Q: Redis 连接池为啥重要？**  
**A:** 频繁建连贵。Lettuce 池配置在 yml。

**227. Q: 缓存与 DB 同时写失败咋办？**  
**A:** 重试、补偿任务、对账。秒杀里补偿回滚库存。

**228. Q: `DECR` 到负数咋办？**  
**A:** 项目 Lua 里检测回滚 `INCR`，防超卖。

**229. Q: Redis 脚本会阻塞吗？**  
**A:** 单线程执行脚本期间其他命令等待，脚本要短。

**230. Q: 如何做分布式限流网关层？**  
**A:** Nginx+lua、Spring Cloud Gateway+Redis。项目应用层限流。

**231. Q: 缓存一致性最强方案？**  
**A:** 没有银弹，看业务容忍度。金融级常同步链路+对账。

**232. Q: Redis 事务 `WATCH`？**  
**A:** 乐观锁监听 key，事务提交前变化则失败。

**233. Q: `UNLINK` vs `DEL`？**  
**A:** unlink 异步删大 key，减少阻塞。

**234. Q: Redis 6 ACL？**  
**A:** 细粒度权限控制，生产安全。

**235. Q: 如何做 Redis 备份恢复演练？**  
**A:** 定期 RDB/AOF 恢复演练，验证 RTO/RPO。

**236. Q: 秒杀库存为啥不放纯 DB？**  
**A:** DB 扛不住瞬时并发，Redis 先挡洪峰。

**237. Q: 热点商品库存拆分？**  
**A:** 把库存拆多个子 key 分摊，或分段预减。高级优化。

**238. Q: Redis 与本地缓存组合？**  
**A:** Caffeine 本地 + Redis 远程，注意失效一致。

**239. Q: 缓存空对象 TTL 多长？**  
**A:** 短一点防穿透，别太长占内存。

**240. Q: 如何用 Redis 实现排行榜？**  
**A:** ZSet score 排序。推荐相似度也可用 ZSet。

**241. Q: `SET` 参数 `NX` `XX`？**  
**A:** NX 不存在才设，XX 存在才设。占位常用 NX。

**242. Q: Redis 序列化注意啥？**  
**A:** 统一格式，避免乱码。项目 `RedisTemplate` Jackson 配类型信息要懂风险。

**243. Q: 秒杀 Lua 返回 0 有哪些情况？**  
**A:** 无 key、库存不足、已抢过、并发边界回滚等。

**244. Q: 如何验证 Lua 逻辑正确？**  
**A:** 单元模拟并发、redis-benchmark、集成测试+监控对账。

**245. Q: Redis 监控指标？**  
**A:** QPS、内存、慢日志、连接数、复制延迟。面试列几个即可。

---

## 网络 / HTTP / 浏览器（246～265）

**246. Q: HTTP 无状态啥意思？**  
**A:** 服务端不天然记用户，靠 Cookie/Session/Token 维持状态。

**247. Q: Cookie SameSite 三个值？**  
**A:** Strict/Lax/None。跨站携带策略不同。

**248. Q: HTTPS 握手大概？**  
**A:** TLS 协商加密套件，证书校验，对称密钥会话加密。

**249. Q: GET POST 区别面试怎么说？**  
**A:** 语义幂等、缓存、body 长度限制，实际看服务端实现别教条。

**250. Q: 状态码 301 302？**  
**A:** 永久/临时重定向。SEO 和缓存行为不同。

**251. Q: 401 vs 403？**  
**A:** 未认证 vs 已认证无权限。项目拦截器区分。

**252. Q: OPTIONS 预检为啥？**  
**A:** CORS 复杂跨域前浏览器先发预检问服务器允许不。

**253. Q: `Content-Type` 常见？**  
**A:** `application/json`、`x-www-form-urlencoded`、`multipart/form-data`。

**254. Q: 什么是 REST？**  
**A:** 资源+动词 HTTP 方法的风格，不是严格标准。

**255. Q: 幂等 HTTP 方法？**  
**A:** GET/PUT/DELETE 设计上幂等，POST 默认不幂等。

**256. Q: WebSocket 和 HTTP？**  
**A:** WS 全双工长连接，适合推送。项目用轮询+定时任务演示。

**257. Q: DNS 解析过程一句？**  
**A:** 递归查询拿到 IP。CDN 依赖 DNS。

**258. Q: 什么是 CDN？**  
**A:** 边缘节点缓存静态资源加速。图片静态资源可接 CDN。

**259. Q: TCP vs UDP？**  
**A:** TCP 可靠有序，UDP 快不保证。视频实时可能 UDP。

**260. Q: 什么是长连接？**  
**A:** HTTP keep-alive 复用 TCP 连接减握手开销。

**261. Q: 什么是反向代理？**  
**A:** Nginx 对外统一入口，转发内网服务。生产标配。

**262. Q: 负载均衡算法？**  
**A:** 轮询、加权、最少连接、一致性哈希。会话粘滞要小心。

**263. Q: 什么是 API 网关？**  
**A:** 统一鉴权、限流、路由、观测。微服务前置。

**264. Q: 如何做接口版本管理？**  
**A:** URL `/v1` 或 Header 版本号。演进兼容策略要想好。

**265. Q: 什么是 HSTS？**  
**A:** 强制 HTTPS，防降级攻击。运维安全配置。

---

## Vue3 / 前端工程（266～295）

**266. Q: Vue3 Composition API 好处？**  
**A:** 逻辑复用更清晰，类型友好。`setup` 里组织代码。

**267. Q: `ref` vs `reactive`？**  
**A:** `ref` 适合基本类型/单值，`reactive` 适合对象。解构注意丢响应式。

**268. Q: `computed` 干啥？**  
**A:** 派生状态缓存，依赖变才重算。别在 computed 里做副作用。

**269. Q: `watch` vs `watchEffect`？**  
**A:** watch 明确监听源；watchEffect 自动收集依赖。

**270. Q: 组件通信方式？**  
**A:** props/emit、provide/inject、pinia、事件总线少用。

**271. Q: Pinia 和 Vuex？**  
**A:** Pinia 更轻无 mutation 样板，TS 友好。新项目首选。

**272. Q: Vue Router 两种模式？**  
**A:** history 和 hash。项目 history 需服务端 fallback。

**273. Q: 路由懒加载好处？**  
**A:** 减首包体积，加快首屏。`import()` 动态导入。

**274. Q: `keep-alive` 干啥？**  
**A:** 缓存组件实例，切页保状态。列表页常用。

**275. Q: `nextTick` 干啥？**  
**A:** DOM 更新后回调，操作更新后 DOM 用。

**276. Q: Vite 为什么快？**  
**A:** 开发态原生 ESM，按需编译。冷启动快。

**277. Q: ESModule 和 CommonJS？**  
**A:** 前端现代打包用 ESM；Node 历史 CJS 多。

**278. Q: Axios 拦截器典型用途？**  
**A:** 统一 token、统一错误、统一日志。项目处理 code 和 401。

**279. Q: 前端如何做防抖节流？  
**A:** 搜索输入防抖，滚动节流。项目可有工具函数。

**280. Q: Element Plus 表单校验？**  
**A:** rules + model，提交前 validate。减少低级错误。

**281. Q: ECharts 性能注意？**  
**A:** 数据量大要 sampling、dataZoom，销毁实例防泄漏。

**282. Q: 如何做前端错误监控？  
**A:** Sentry、自采集上报。小项目 console + 后端日志。

**283. Q: CSP 是啥？  
**A:** 内容安全策略限制脚本来源，防 XSS。

**284. Q: 什么是 Tree Shaking？  
**A:** 打包去掉未用代码，ESM 静态分析友好。

**285. Q: package lock 要不要提交？  
**A:** 应用项目一般要，锁依赖版本，CI 可复现。

**286. Q: npm 和 pnpm 区别一句？  
**A:** pnpm 硬链接省磁盘。团队统一工具链。

**287. Q: 如何做前端权限路由？  
**A:** meta + beforeEach 动态路由表。项目简化为 admin 拦截。

**288. Q: `v-if` vs `v-show`？  
**A:** if 真移除 DOM，show 只是 display。切换频繁用 show。

**289. Q: key 在列表为啥重要？  
**A:** 帮助 diff 识别节点，避免状态错乱。

**290. Q: scoped CSS 原理？  
**A:** 加唯一属性选择器隔离样式。深度选择器 `:deep`。

**291. Q: 如何做国际化 i18n？  
**A:** vue-i18n，抽文案。演示项目可不做。

**292. Q: PWA 了解吗？  
**A:** 离线缓存+安装体验。电商可选。

**293. Q: SSR 适用场景？  
**A:** SEO 要求高的页面。后台管理一般 CSR 足够。

**294. Q: 前端如何做接口 mock？  
**A:** vite-plugin-mock 或 MSW。联调前提速。

**295. Q: 如何做组件单元测试？  
**A:** Vitest + Vue Test Utils。项目可补关键逻辑测试。

---

## 项目结合题 / 场景题（296～310）

**296. Q: 用一句话描述秒杀系统目标？**  
**A:** 高并发下正确扣库存、每人限购、失败可解释、可监控可对账。

**297. Q: 动态 path 解决了什么攻击面？**  
**A:** 固定 URL 被脚本批量 POST，path 短期有效提高成本。

**298. Q: 为什么要双维度限流？  
**A:** 防单用户狂点也防单 IP 刷接口，覆盖面更大。

**299. Q: 订单超时取消为啥要区分秒杀行？  
**A:** 秒杀库存回滚涉及 Redis+DB 与普通商品不同，必须分支处理。

**300. Q: 推荐系统为啥离线算相似度？  
**A:** 在线全表两两算太重，预计算放 ZSet，在线聚合邻居。

**301. Q: AI 注入实时数据会不会泄露隐私？  
**A:** 普通用户只注入本人数据；管理员给汇总；避免把其他用户明细塞 prompt。

**302. Q: 全局异常返回 200+code 合理吗？  
**A:** 看团队约定，前端好处理；也可用 HTTP 状态码表达错误层级。

**303. Q: 管理后台为何单独路由前缀？  
**A:** 权限隔离清晰，拦截器一条规则覆盖 `/api/admin`。

**304. Q: 商品搜索为何同时支持 q 和 keyword？  
**A:** 兼容不同前端参数命名，减少联调摩擦。

**305. Q: 购物车用 Hash JSON 优缺点？  
**A:** 灵活但序列化开销；极端可考虑 Hash field 结构化。

**306. Q: 订单号为何不用 UUID？  
**A:** 可读性差、索引随机插入性能一般；趋势递增更友好。

**307. Q: 演示支付如何解释？  
**A:** 教学环境模拟状态流转，真实支付对接三方与签名验签。

**308. Q: 如果上 Kubernetes 你会拆哪些服务？  
**A:** 网关、商城 API、后台 API、Redis、MySQL 外置，文件存储改对象存储。

**309. Q: 如何做压测报告最小闭环？  
**A:** 场景、并发、指标（TPS/P99/错误率）、资源曲线、结论与瓶颈。

**310. Q: 你认为自己项目最大亮点？  
**A:** 把秒杀一致性、权限、观测、文档化串成闭环，能讲清楚也能演进。

---

## Java 基础加强（311～450）

**311. Q: Java 八种基本类型？**  
**A:** byte、short、int、long、float、double、char、boolean。默认值要背熟，尤其 boolean 是 false、引用默认 null。

**312. Q: 包装类型解决啥问题？**  
**A:** 集合与泛型要对象；提供 parse、比较等 API。注意 `null` 拆箱直接 NPE。

**313. Q: `Integer` 缓存区间？**  
**A:** 默认 -128～127，`valueOf` 走缓存。两个 128 用 `==` 可能 false，业务用 `equals`。

**314. Q: 啥时会自动装箱/拆箱？**  
**A:** 赋值、算术、方法传参、集合读写都可能触发。混用 `int`/`Integer` 小心隐式与 NPE。

**315. Q: `char` 和汉字啥关系？**  
**A:** `char` 是 UTF-16 码元，增补平面要代理对。业务别乱拆字，用 `String`。

**316. Q: `String` 不可变指什么？**  
**A:** 内部序列对外不可改，变量重赋值是换引用。安全、哈希稳定、常量池复用都靠它。

**317. Q: `intern()` 干啥？**  
**A:** 把字符串放进常量池复用，省内存也可能撑大池子。高并发滥用要谨慎。

**318. Q: `new String("ab")` 几个对象？**  
**A:** 堆上至少一个新 `String`；池里 `"ab"` 可能已存在。说清楚堆与池分工即可。

**319. Q: 循环里字符串拼接用啥？**  
**A:** 用 `StringBuilder`，别一直 `+` 产生大量中间对象。

**320. Q: `StringBuffer` 和 `StringBuilder`？**  
**A:** `StringBuffer` 带锁线程安全更慢；单线程优先 `StringBuilder`。

**321. Q: 数组的 `length`？**  
**A:** 是 final 字段不是方法；多维数组是数组的数组。

**322. Q: 可变参数 `...` 底层？**  
**A:** 编译成数组，注意重载解析和空参歧义。

**323. Q: 枚举能有构造器吗？**  
**A:** 可以，隐式 `private`。每个枚举常量是静态终态实例。

**324. Q: 枚举单例为啥面试常夸？**  
**A:** 实例个数固定、构造私有，语言层就限制滥用，比懒汉双重检查省心。

**325. Q: `switch` 何时支持 `String`？**  
**A:** Java 7+，编译器转哈希分支。`switch` 的 `null` 会 NPE，先判空。

**326. Q: `switch` 穿透啥意思？**  
**A:** 不写 `break` 会继续执行下一分支。`switch` 表达式用 `yield` 减少穿透坑。

**327. Q: 增强 for 能删集合吗？**  
**A:** 一般不行，抛 `ConcurrentModificationException`。用 `Iterator.remove` 或安全集合策略。

**328. Q: 生产默认开 `assert` 吗？**  
**A:** 不开 `-ea`，别用断言做业务分支。业务用显式 `if` 或异常。

**329. Q: `instanceof` 模式匹配？**  
**A:** Java 16+ 判断同时绑定变量，少一次强转，代码更干净。

**330. Q: `record` 适合啥？**  
**A:** 不可变数据载体，自动生成 `equals`/`hashCode`/`toString`，写 DTO 省事。

**331. Q: `sealed` 类干啥？**  
**A:** 限制谁能继承/实现，封闭体系，配合模式匹配更清晰。

**332. Q: `var` 能用在哪里？**  
**A:** 仅局部且必须有初始化。团队要统一可读性规范。

**333. Q: 文本块 `\"\"\"`？**  
**A:** 多行字符串少转义，注意缩进对齐规则。

**334. Q: `switch` 表达式里 `yield`？**  
**A:** 从分支返回值，和语句版 `switch` 区分，减少副作用穿透。

**335. Q: 内部类分几种？**  
**A:** 成员、静态成员、局部、匿名。捕获局部变量要 effectively final。

**336. Q: 静态内部类能访问外部实例吗？**  
**A:** 不能隐式持有，需要就自己传外部引用。

**337. Q: `this()` 和 `super()`？**  
**A:** 都要在第一行，只能二选一；不写则隐式 `super()`。

**338. Q: 子类继承父类构造器吗？**  
**A:** 不继承，但子类构造必须先链到父类构造。

**339. Q: 多态下方法与字段看啥类型？**  
**A:** 实例方法看运行时类型；字段没有多态，看编译期声明类型。

**340. Q: 父类 `private` 子类同名算重写吗？**  
**A:** 不算，子类是新方法，`@Override` 会报错。

**341. Q: `equals`/`hashCode` 契约？**  
**A:** 相等必须同哈希；只改一个 `HashMap` 会诡异失灵。

**342. Q: `wait`/`notify` 注意？**  
**A:** 必须在同步块里且持有同一 monitor，否则 `IllegalMonitorStateException`。

**343. Q: `clone()` 深还是浅？**  
**A:** 默认浅拷贝，引用字段共享。深拷贝自己递归或拷贝构造。

**344. Q: `Arrays.copyOf` 深拷贝吗？**  
**A:** 只拷贝数组壳一层，元素引用照旧。

**345. Q: `Comparable` 和 `Comparator`？**  
**A:** 自然顺序写类里；比较器外部灵活，可多策略排序。

**346. Q: `List.of`/`Map.of` 能改吗？**  
**A:** 不可变，`add`/`put` 抛异常；`Map.of` 还不许 null 键值。

**347. Q: `Optional` 咋用才不坑？**  
**A:** 少裸 `get`，用 `orElse`/`orElseGet`/`orElseThrow`；别滥用当字段类型。

**348. Q: Stream 为啥说惰性？**  
**A:** 中间操作不立刻算，遇到 terminal 才管道执行；Stream 别重复使用。

**349. Q: `flatMap` 解决啥？**  
**A:** 一层里每个元素再展开成流，拍平 `List<List<>>` 一类结构。

**350. Q: 泛型擦除是啥？**  
**A:** 运行时丢类型参数，拿不了 `List<String>` 里的 String。反射写法受限。

**351. Q: PECS 口诀？**  
**A:** Producer extends、Consumer super；只读 `? extends`，只写 `? super`。

**352. Q: 为啥不能 `new T()`？**  
**A:** 擦除后没有真实类型信息构造对象，用工厂或 `Class` 反射。

**353. Q: `Class.forName` 会初始化类吗？**  
**A:** 一般会触发初始化；`loadClass` 不一定，JDBC 老代码常见对比题。

**354. Q: 反射性能？**  
**A:** 比直接调用慢，有访问检查；热点路径少反射或生成字节码。

**355. Q: JDK 动态代理限制？**  
**A:** 基于接口；Spring 无接口时常用 CGLIB 子类代理。

**356. Q: 注解 `Retention` 三档？**  
**A:** SOURCE、CLASS、RUNTIME；Spring 配置类注解多在 RUNTIME 才能反射读到。

**357. Q: 为啥用 `java.time`？**  
**A:** 不可变、线程安全、API 清晰，替代 `Date`/`Calendar` 坑。

**358. Q: `BigDecimal` 比较？**  
**A:** 用 `compareTo`，`equals` 还看标度容易踩坑。

**359. Q: `try-with-resources`？**  
**A:** `AutoCloseable` 自动关闭，异常可有 suppressed 链。

**360. Q: `finally` 里 `return` 有啥问题？**  
**A:** 会覆盖 `try` 的返回或吞异常，排查地狱，面试直接说避免。

**361. Q: `Error` 要 catch 吗？**  
**A:** 一般不，多是 JVM 级严重问题。业务层捕获 `Exception` 分支即可。

**362. Q: `StackOverflowError` 常见原因？**  
**A:** 递归太深或栈帧过大。改算法或调栈参数。

**363. Q: 常见 OOM 类型？**  
**A:** 堆、Metaspace、直接内存、栈都可能。结合 dump 和监控定位。

**364. Q: `Serializable` 干啥？**  
**A:** 标记可序列化；`serialVersionUID` 不一致反序列化会挂，改类要评估兼容。

**365. Q: `transient`？**  
**A:** 默认序列化跳过该字段，反序列化给默认值，敏感信息常用。

**366. Q: 双亲委派模型？**  
**A:** 先让父加载器加载，保护核心类库不被覆盖。SPI/Tomcat 会打破场景要会举。

**367. Q: 类加载大致阶段？**  
**A:** 加载、验证、准备、解析、初始化；静态块在初始化跑。

**368. Q: 访问修饰符可见性？**  
**A:** private、包可见、protected、public 画表背，子类与包是考点。

**369. Q: Java 是值传递吗？**  
**A:** 是值传递：传引用的拷贝。方法内换引用不影响外面变量。

**370. Q: `main` 方法签名？**  
**A:** `public static void main(String[] args)`，JVM 固定入口。

**371. Q: 代码块执行顺序面试题？**  
**A:** 父静→子静→父实例/构造→子实例/构造，输出题按这个推。

**372. Q: 接口默认方法冲突？**  
**A:** 多接口同签名默认方法，实现类要显式选或重写。

**373. Q: `volatile` 能保证 `i++` 吗？**  
**A:** 不能，不原子。用 `AtomicInteger` 或同步。

**374. Q: 四种引用？**  
**A:** 强、软、弱、虚；弱常用于 `WeakHashMap`、虚用于跟踪回收。

**375. Q: `ThreadLocal` 线程池注意？**  
**A:** 用完 `remove`，否则复用线程串数据或泄漏。

**376. Q: `Arrays.asList` 能增删吗？**  
**A:** 固定大小底层数组，`add`  Unsupported，要可变就包一层 `ArrayList`。

**377. Q: `subList` 注意？**  
**A:** 是视图，动原列表结构后子列表可能失效。

**378. Q: `HashMap` 的 key 可变对象？**  
**A:** 改参与 hash 的字段会找不到 entry，key 用不可变更稳。

**379. Q: `LinkedHashMap` 用途？**  
**A:** 保插入或访问顺序，可做简单 LRU。

**380. Q: `ConcurrentModificationException` 啥时候？**  
**A:** 迭代中结构被改，或 foreach 里调集合 `remove`。用迭代器删或并发集合。

**381. Q: `Queue` 的 `offer`/`add`？**  
**A:** 有界队列 `offer` 失败返回 false，`add` 抛异常。

**382. Q: 为啥推荐 `Deque` 代替 `Stack`？**  
**A:** `Stack` 老类设计别扭，`Deque` 接口更清晰。

**383. Q: `EnumMap` 为啥快？**  
**A:** 数组下标用 ordinal，无哈希冲突，键必须是同一枚举。

**384. Q: `IdentityHashMap`？**  
**A:** 用 `==` 比引用，不是 `equals`，特殊场景才用。

**385. Q: `Objects.equals` 好处？**  
**A:** 两个 null 也安全，少写样板判空。

**386. Q: 装箱性能问题？**  
**A:** 频繁装箱制造垃圾，循环里尽量用基本类型。

**387. Q: `module-info` 了解吗？**  
**A:** JPMS 声明导出与依赖，`exports`/`opens` 控制可见与反射，JDK9+。

**388. Q: 不可变对象好处？**  
**A:** 线程安全、可共享、哈希稳定；`String`、多数 `java.time` 如此。

**389. Q: 防御性拷贝啥时候？**  
**A:** getter 返回可变集合/数组时 copy 一份，防外部改坏内部状态。

**390. Q: POJO/DTO 边界乱会怎样？**  
**A:** 层间耦合、改一处炸一片，团队要约定对象类型边界。

**391. Q: JIT 大概干啥？**  
**A:** 热点字节码编译成本地码，提高峰值性能，配合内联逃逸分析。

**392. Q: 桥方法 bridge 为啥存在？**  
**A:** 泛型擦除后子类重写签名要对齐，编译器合成桥接保多态。

**393. Q: `invokedynamic` 一句？**  
**A:** 支持 lambda、方法句柄等运行时绑定，了解即可加分。

**394. Q: `>>` 和 `>>>`？**  
**A:** 算术右移保留符号；逻辑右移高位补 0。

**395. Q: 整数溢出？**  
**A:** 默认环绕不抛异常，要严格用 `Math.addExact` 等。

**396. Q: `NaN` 比较？**  
**A:** `NaN == NaN` 为 false，用 `Float.isNaN`。

**397. Q: `Reader`/`Writer` vs 字节流？**  
**A:** 字符流管编解码，文本用字符流，二进制用字节流。

**398. Q: `Charset` 为啥显式指定？**  
**A:** 平台默认编码坑跨环境，文件与网络常锁 UTF-8。

**399. Q: `Files.lines` 大文件？**  
**A:** 流式读；`readAllLines` 一次进内存可能爆。

**400. Q: `Buffer.flip`？**  
**A:** 写转读，调 position/limit，NIO 常规套路。

**401. Q: `equals` 对称性？**  
**A:** `a.equals(b)` 与 `b.equals(a)` 要一致；继承时用 `instanceof` 还是 `getClass` 要想清。

**402. Q: Lombok `@Data`/`@EqualsAndHashCode`？**  
**A:** 注意包含字段范围，关联实体防循环引用。

**403. Q: `String` 的 `hashCode`？**  
**A:** 多项式累加可缓存，相等字符串哈希必等。

**404. Q: `StrictMath` vs `Math`？**  
**A:** 严格 IEEE；`Math` 可能更快平台相关，一般写 `Math`。

**405. Q: `enum` 能实现接口吗？**  
**A:** 能，常用来做策略枚举，分常量实现。

**406. Q: `switch` 对 `enum` 好处？**  
**A:** 编译器检查穷尽，改枚举能编译期发现遗漏。

**407. Q: `Properties` 老在哪？**  
**A:** 继承 `Hashtable`，键值 String；新项目多用 yaml/properties 库。

**408. Q: `System.arraycopy`？**  
**A:** 原生批量拷数组，重叠区间要注意方向。

**409. Q: `toArray(new T[0])` idiom？**  
**A:** 让 JVM 分配合适大小，现代写法常推荐。

**410. Q: `Character.isDigit` 认 ASCII 吗？**  
**A:** 按 Unicode 类别，校验业务要看需求别想当然。

**411. Q: `TreeMap` 复杂度？**  
**A:** 红黑树，查找插入删除 O(log n)，要排序或区间遍历时用。

**412. Q: `BitSet` 干啥？**  
**A:** 紧凑位向量，大量布尔标记省内存。

**413. Q: `WeakHashMap` 何时丢 entry？**  
**A:** key 只被弱引用时 GC 可回收，适合做缓存键生命周期跟对象走。

**414. Q: `PriorityQueue` 注意啥？**  
**A:** 堆实现，非线程安全；迭代顺序不等于堆序，要按优先级取用 poll。

**415. Q: `ConcurrentHashMap` 复合操作安全吗？**  
**A:** 单次读写常安全，`putIfAbsent` 等单 op 原子，但 `if (!contains) put` 仍非原子。

**416. Q: `Collections.synchronizedList` 仍要小心？**  
**A:** 单方法同步，复合操作要外部同步或换并发集合。

**417. Q: `Comparator.comparing` 好处？**  
**A:** 链式比较、可读好，少写样板比较器。

**418. Q: `Spliterator` 一句？**  
**A:** 可分迭代器，支撑并行流拆分数据源。

**419. Q: `parallelStream` 注意？**  
**A:** 公共 ForkJoinPool，任务要无共享可变状态；IO 密集别瞎并行。

**420. Q: `Collectors.toMap` 键冲突？**  
**A:** 重复 key 默认抛异常，要提供 merge 函数。

**421. Q: 抽象类能有构造器吗？**  
**A:** 能有，给子类 `super` 用，但不能 `new` 抽象类本身。

**422. Q: 接口里字段默认啥修饰？**  
**A:** `public static final`，就是常量。

**423. Q: 静态方法里能用 `this` 吗？**  
**A:** 不能，没有实例上下文。

**424. Q: `equals` 还要满足啥？**  
**A:** 自反、对称、传递、一致；与 `hashCode` 配套。

**425. Q: `HashMap` 链表过长？**  
**A:** JDK8+ 超阈值转红黑树，降最坏查找，前提 key 可比。

**426. Q: `ArrayList` 扩容？**  
**A:** 数组满则扩容（常见 1.5 倍）并拷贝，能估容量就带初始 size。

**427. Q: `HashMap` 负载因子意义？**  
**A:** 控制扩容阈值，权衡空间与时间，默认 0.75 经验值。

**428. Q: `AutoCloseable` 和 `Closeable`？**  
**A:** 前者更宽可抛任何异常；后者 IO 专用。try-with-resources 用前者。

**429. Q: `SecureRandom` 和 `Random`？**  
**A:** 安全随机更不可预测，加密场景别用普通 `Random`。

**430. Q: `UUID.randomUUID` 干啥？**  
**A:** 生成随机 UUID，分布式 id 之一，可读性差。

**431. Q: Javadoc 里能写 `*/` 吗？**  
**A:** 不能，会截断注释导致编译炸，换表述或拆开写。

**432. Q: `readObject` 自定义序列化？**  
**A:** 可控字段读写顺序与安全校验，防反序列化Gadget链要警惕。

**433. Q: `Cleaner` 比 `finalize`？**  
**A:** 更可控的清理注册，别依赖 GC 时机；资源还是 try-with-resources 最稳。

**434. Q: Java「半编译」啥意思？**  
**A:** 源码到字节码，运行时解释+JIT，别死磕纯编译/纯解释标签。

**435. Q: 虚方法分派？**  
**A:** 运行时按实际类型选重写方法，多态核心。

**436. Q: `Base64` 用途？**  
**A:** 二进制转可打印 ASCII，传输嵌入小数据，不是加密。

**437. Q: `URL` 和 `URI`？**  
**A:** URI 标识，URL 定位资源，关系常一起考。

**438. Q: `ProcessBuilder` 一句？**  
**A:** 创建子进程，注意命令注入要白名单参数。

**439. Q: 注解 `@Inherited`？**  
**A:** 子类继承父类上的该注解；默认不继承。

**440. Q: `Scanner` 读大文件？**  
**A:** 有解析开销，大文本流式用 `BufferedReader` 更合适。

**441. Q: `split` 正则注意？**  
**A:** 特殊字符要转义；频繁分割先编译 `Pattern`。

**442. Q: `String` 常量折叠？**  
**A:** 编译期能算的字面量拼接直接合成一个常量。

**443. Q: `Comparable` 用减法比较整数？**  
**A:** 可能溢出，用 `Integer.compare` 更安全。

**444. Q: 内部类访问外部 private？**  
**A:** 编译器生成桥接访问方法，语法上能访问。

**445. Q: 函数式接口几个抽象方法？**  
**A:** 一个抽象方法（默认/静态不算），`@FunctionalInterface` 编译检查。

**446. Q: lambda 捕获变量要求？**  
**A:** final 或 effectively final，不能改被捕获变量。

**447. Q: 方法引用四类？**  
**A:** 静态、实例、特定对象、构造器，配合函数式接口。

**448. Q: `Stream` 短路操作？**  
**A:** `findFirst`、`anyMatch` 等可能不遍历全流。

**449. Q: `Collectors.joining`？**  
**A:** 拼字符串带分隔符/前后缀，比手动 `+` 清爽。

**450. Q: 为啥 keySet 视图改会反映到 Map？**  
**A:** 是底层 map 的视图，不是独立拷贝，迭代时结构变要小心。

---

## Java 并发与 JVM 基础加强（451～510）

**451. Q: 进程和线程？**  
**A:** 进程资源隔离，线程共享地址空间更轻，Java 线程映射 OS 线程。

**452. Q: 并发 vs 并行？**  
**A:** 并发是交替推进，并行是同时跑，多核才有真并行。

**453. Q: 线程六种状态？**  
**A:** NEW、RUNNABLE、BLOCKED、WAITING、TIMED_WAITING、TERMINATED。

**454. Q: `sleep` 放锁吗？**  
**A:** 不放；`wait` 在同步块里会释放 monitor。

**455. Q: `join` 干啥？**  
**A:** 当前线程等目标线程结束，可带超时。

**456. Q: `interrupt` 怎么用？**  
**A:** 协作式中断标志，阻塞 IO/锁不一定立刻响应，要处理 `InterruptedException`。

**457. Q: 为啥别用 `stop`？**  
**A:** 强制停线程破坏不变量，用中断+退出条件。

**458. Q: `synchronized` 锁啥？**  
**A:** 对象头 monitor；静态方法锁 Class 对象。

**459. Q: 可重入？**  
**A:** 同线程可再次获取同锁，`synchronized`/`ReentrantLock` 都可重入。

**460. Q: `volatile` 保证啥？**  
**A:** 可见性与一定有序性，不保证 `i++` 原子。

**461. Q: CAS？**  
**A:** 比较并交换，CPU 原子指令，乐观锁基础。

**462. Q: ABA？**  
**A:** 值改回原被误认为没变，用版本号/`AtomicStampedReference`。

**463. Q: `LongAdder` vs `AtomicLong`？**  
**A:** 高竞争计数 `LongAdder` 分段合并更稳。

**464. Q: `ReentrantLock` 比 `synchronized`？**  
**A:** 可中断、超时、公平锁、`tryLock`，复杂场景更灵活。

**465. Q: `ReadWriteLock`？**  
**A:** 读共享写独占，读多写少提升吞吐，注意写饥饿。

**466. Q: `CountDownLatch`/`CyclicBarrier`？**  
**A:** 前者一次倒计时，后者可重用栅栏同步。

**467. Q: `Semaphore`？**  
**A:** 许可数限流，控制并发度。

**468. Q: `CompletableFuture`？**  
**A:** 组合异步流水线，别乱用默认线程池把池打满。

**469. Q: 线程池七个参数？**  
**A:** 核心、最大、存活、队列、线程工厂、拒绝策略、单位。

**470. Q: 拒绝策略 `CallerRuns`？**  
**A:** 调用线程自己跑任务，形成背压，防无限堆任务。

**471. Q: 为啥慎 `Executors` 默认工厂？**  
**A:** 隐藏队列边界，无界队列可能 OOM，显式 `ThreadPoolExecutor` 更透明。

**472. Q: `ThreadLocal` 原理一句？**  
**A:** 每线程一份 map 存变量副本，记得回收防泄漏。

**473. Q: happens-before？**  
**A:** JMM 规定的可见性顺序，不是物理时间先后。

**474. Q: 双重检查锁单例 `volatile`？**  
**A:** 防止构造重排序导致看到半初始化对象。

**475. Q: 伪共享？**  
**A:** 多核改同一缓存行不同变量互相失效，对齐/填充可缓解。

**476. Q: `CopyOnWriteArrayList`？**  
**A:** 写时复制，读多写少，写会复制数组成本高。

**477. Q: `BlockingQueue` 场景？**  
**A:** 生产者消费者、线程池任务队列，有界无界要选对。

**478. Q: `submit` 异常去哪了？**  
**A:** 包在 `Future.get`，别丢 Future；或用 handler。

**479. Q: 线程池优雅关闭？**  
**A:** `shutdown`→`awaitTermination`，不行再 `shutdownNow` 并处理中断。

**480. Q: 死锁四条件？**  
**A:** 互斥、占有等待、不可抢占、循环等待，破一环。

**481. Q: 堆分代目的？**  
**A:** 短命对象多，Young GC 频繁但快，老年代长寿对象。

**482. Q: Minor GC / Full GC？**  
**A:** Young 区回收 vs 整堆或老年代回收，停顿和成本差很大。

**483. Q: STW？**  
**A:** Stop-The-World，GC 暂停应用线程，低延迟要控停顿。

**484. Q: G1 特点？**  
**A:** Region 化、可设停顿目标、Mixed GC，JDK9+ 常用默认。

**485. Q: CMS 问题？**  
**A:** 碎片、`concurrent mode failure` 退化，已逐步淡出。

**486. Q: ZGC/Shenandoah 一句？**  
**A:** 超低停顿趋势，大堆可关注。

**487. Q: Metaspace？**  
**A:** 类元数据在本地内存，类加载多要防 OOM。

**488. Q: 直接内存？**  
**A:** `allocateDirect` 不走堆，回收依赖 Cleaner，不当也 OOM。

**489. Q: GC Roots？**  
**A:** 栈、静态、JNI、类加载器等可达性起点。

**490. Q: OOM 排查第一步？**  
**A:** 拿 heap dump，MAT 看 Dominator Tree 找泄漏链。

**491. Q: CPU 飙高？**  
**A:** 找热点线程，映射栈到代码，profiler 验证。

**492. Q: JIT 内联？**  
**A:** 小热点方法内联减开销，虚方法难内联。

**493. Q: 逃逸分析？**  
**A:** 对象不逃出方法线程可栈上分配或锁消除。

**494. Q: Safepoint？**  
**A:** 线程安全点停下做 GC 等，`-XX:+PrintGC` 调优会碰到概念。

**495. Q: `ForkJoinPool`？**  
**A:** 工作窃取，分治并行，`parallelStream` 默认公共池。

**496. Q: 守护线程？**  
**A:** 不阻止 JVM 退出，`setDaemon` 要在 `start` 前。

**497. Q: `volatile` 数组元素？**  
**A:** 只保证数组引用可见，元素用 `Atomic*Array` 或同步。

**498. Q: 活锁？**  
**A:** 线程都在退让重试却无进展，比死锁难肉眼看出。

**499. Q: `StampedLock`？**  
**A:** 乐观读+升级，难用，读极多可考虑。

**500. Q: `Phaser` 一句？**  
**A:** 可动态注册参与者的同步栅栏，比分阶段 CyclicBarrier 灵活。

**501. Q: `Exchanger`？**  
**A:** 两线程在同步点交换数据，小众场景。

**502. Q: `Thread.yield`？**  
**A:** 提示调度器，是否让出不可依赖，少用。

**503. Q: 线程工厂干啥？**  
**A:** 自定义线程名、优先级、未捕获异常处理，排障友好。

**504. Q: 上下文切换成本？**  
**A:** 保存恢复寄存器与栈，过多线程反而慢。

**505. Q: `synchronized` 锁升级了解吗？**  
**A:** 偏向→轻量→重量，JDK 实现演进，面试提一嘴无锁到重量级。

**506. Q: `wait` 为啥抛中断异常？**  
**A:** 可被中断唤醒，清中断标志前要决定策略。

**507. Q: `notify` vs `notifyAll`？**  
**A:** 唤醒一个或全部等待线程，避免漏唤醒用 `notifyAll` 更稳。

**508. Q: `Thread.sleep(0)`？**  
**A:** 可能让出 CPU，行为依赖平台，别当同步原语。

**509. Q: `Executors.newCachedThreadPool` 风险？**  
**A:** 无限创建线程可能把机器打爆，高并发要限流。

**510. Q: JVM 调优先说啥？**  
**A:** 定目标（吞吐/延迟），再选收集器、堆大小，压测+指标闭环。

---

## 计算机 / 网络 / 数据库基础加强（511～560）

**511. Q: TCP 和 UDP？**  
**A:** TCP 可靠有序有连接；UDP 无连接尽力交付，实时可丢用 UDP。

**512. Q: 三次握手？**  
**A:** 同步序列号、确认双向可达，防历史连接。

**513. Q: 四次挥手 TIME_WAIT？**  
**A:** 等迟滞包消失、全双工关闭，短连接多要关注端口与调优。

**514. Q: SYN 洪泛？**  
**A:** 伪造源 IP 打半连接，SYN cookie/限速缓解。

**515. Q: HTTP 无状态？**  
**A:** 协议本身不记会话，用 Cookie/Session/JWT 补状态。

**516. Q: HTTP/2？**  
**A:** 多路复用单连接，二进制分帧，缓解 HTTP1 队头阻塞（TCP 层仍可能有）。

**517. Q: HTTP/3？**  
**A:** 基于 QUIC/UDP，改善握手与丢包，趋势了解。

**518. Q: HTTPS 握手？**  
**A:** 证书链校验+密钥协商，对称加密传数据。

**519. Q: DNS 大致流程？**  
**A:** 递归解析器迭代问根/顶级/权威，TTL 影响缓存。

**520. Q: CDN？**  
**A:** 边缘缓存静态资源就近访问，动态回源策略要配好。

**521. Q: 四层七层负载？**  
**A:** 基于 IP:端口 vs HTTP 内容路由，各用不同组件。

**522. Q: NAT？**  
**A:** 私网地址映射公网，省 IPv4。

**523. Q: WebSocket？**  
**A:** 握手后全双工长连接，适合推送。

**524. Q: CSRF？**  
**A:** 浏览器带 Cookie 被动发起恶意请求，Token/SameSite 防御。

**525. Q: XSS 三类？**  
**A:** 反射、存储、DOM；转义+CSP+HttpOnly 组合拳。

**526. Q: SQL 注入？**  
**A:** 拼接 SQL 执行用户输入，预编译参数化根治。

**527. Q: 事务 ACID？**  
**A:** 原子一致隔离持久，一致性常靠业务约束+隔离级别。

**528. Q: 隔离级别现象？**  
**A:** 脏读、不可重复读、幻读，从读未提交到串行化逐级收紧。

**529. Q: MVCC？**  
**A:** 多版本快照读，减读写阻塞，写仍要锁。

**530. Q: redo/undo/binlog？**  
**A:** redo 崩溃恢复页；undo 回滚一致性读；binlog 主从复制逻辑日志。

**531. Q: 最左前缀？**  
**A:** 联合索引从左匹配，跳过左列可能用不上索引。

**532. Q: 覆盖索引？**  
**A:** 索引含查询字段不回表，`Using index`。

**533. Q: 回表？**  
**A:** 二级索引找主键再查聚簇索引取行。

**534. Q: 深分页优化？**  
**A:** 延迟关联、seek（上次 max id）、搜索 scroll，少用大 offset。

**535. Q: 死锁 InnoDB？**  
**A:** 检测回滚代价小事务；应用重试；SQL 顺序一致。

**536. Q: 间隙锁？**  
**A:** 锁索引间隙防幻读，RR 下要注意与死锁。

**537. Q: 乐观锁悲观锁？**  
**A:** 版本号/CAS vs `select for update`，按冲突率选。

**538. Q: 缓存与 DB 一致性？**  
**A:** 旁路缓存、延迟双删、canal/binlog；强一致难，接受短暂窗口。

**539. Q: CAP？**  
**A:** 分区容错现实存在，多数在 C 与 A 间权衡，别背死公式。

**540. Q: BASE？**  
**A:** 基本可用、软状态、最终一致，分布式常见叙事。

**541. Q: 幂等设计？**  
**A:** 业务唯一键、去重表、Token，状态机合法迁移。

**542. Q: JOIN 算法？**  
**A:** NLJ、Hash Join、Sort Merge，优化器按数据量与索引选。

**543. Q: 自增主键优缺点？**  
**A:** 顺序插入友好；分布式热点可用雪花等替代。

**544. Q: 慢查询起手式？**  
**A:** `EXPLAIN`、看 type、rows、是否回表、索引选择性。

**545. Q: 索引下推？**  
**A:** 引擎层先过滤索引条件减少回表，MySQL 5.6+。

**546. Q: 分库分表难点？**  
**A:** 分布式 ID、跨片 join/聚合、扩容迁移、分布式事务取舍。

**547. Q: Linux 管道？**  
**A:** 进程间单向字节流，`|` 组合命令，简单 IPC。

**548. Q: 虚拟内存？**  
**A:** 进程隔离+超额提交，缺页中断换页。

**549. Q: inode？**  
**A:** 存元数据，目录项映射文件名到 inode，硬链接同 inode。

**550. Q: 软链硬链？**  
**A:** 软链可跨文件系统指路径；硬链同分区多名字。

**551. Q: 文件权限 rwx？**  
**A:** 用户/组/其他三段，数字模式常考。

**552. Q: 进程通信？**  
**A:** 管道、消息队列、共享内存、套接字，共享内存最快但难搞同步。

**553. Q: TIME_WAIT 过多？**  
**A:** 短连接高并发占端口，连接池、复用、内核参数配合治理。

**554. Q: ARP？**  
**A:** 局域网 IP 解析 MAC。

**555. Q: MTU 分片？**  
**A:** 超 MTU IP 分片，一片丢全丢，尽量路径 MTU 发现。

**556. Q: 中间人攻击？**  
**A:** 伪造证书或劫持链路，HTTPS+证书校验+HSTS 缓解。

**557. Q: OAuth2 四个角色？**  
**A:** 资源所有者、客户端、授权服务器、资源服务器。

**558. Q: JWT 注意？**  
**A:** 无状态难吊销，载荷别塞秘密；短 token+refresh 或黑名单。

**559. Q: BCrypt？**  
**A:** 自适应成本慢哈希存口令，别明文别 MD5。

**560. Q: REST GET 幂等？**  
**A:** 语义上重复 GET 无副作用，实现别写错。

---

## 前端与工程基础加强（561～590）

**561. Q: 301 和 302？**  
**A:** 301 永久搬域名 SEO 权重迁移；302 临时，缓存行为不同。

**562. Q: 304？**  
**A:** 协商缓存未改，省带宽，靠 ETag/Last-Modified。

**563. Q: CORS 预检？**  
**A:** 复杂请求先 OPTIONS，服务端返回 Allow-* 头。

**564. Q: JSON 与 JS 对象？**  
**A:** JSON 键必须双引号，更严格；`JSON.parse` 校验格式。

**565. Q: Promise 状态？**  
**A:** pending→fulfilled/rejected，不可逆；链式返回新 Promise。

**566. Q: 宏任务微任务？**  
**A:** 同步完先清微任务再下一个宏任务，`Promise.then` 是微任务。

**567. Q: 闭包用途？**  
**A:** 私有化状态、工厂函数；注意引用的内存释放。

**568. Q: `==` vs `===`？**  
**A:** `===` 无隐式转换，业务判断默认用它。

**569. Q: `slice`/`splice`？**  
**A:** slice 不修改原数组；splice 原地改，别混名。

**570. Q: 深拷贝？**  
**A:** `structuredClone`、lodash；`JSON` 丢函数/日期类型。

**571. Q: localStorage vs sessionStorage？**  
**A:** 同源持久 vs 标签页会话，别存敏感明文。

**572. Q: Cookie 安全属性？**  
**A:** HttpOnly 防 XSS 偷、Secure 要求 HTTPS、SameSite 降 CSRF。

**573. Q: 防抖节流？**  
**A:** 防抖合并末次触发；节流固定间隔，搜索 vs 滚动典型场景。

**574. Q: PUT PATCH？**  
**A:** PUT 常整体替换；PATCH 部分更新，看团队约定。

**575. Q: npm devDependencies？**  
**A:** 仅构建工具放 dev，库发布别误把运行时依赖放错栏。

**576. Q: `^` `~` 版本？**  
**A:** 兼容次版本 vs 补丁级；lockfile 锁真实版本。

**577. Q: Source Map 生产？**  
**A:** 可不对外暴露，监控平台再上传 mapping。

**578. Q: Vue `ref`/`reactive`？**  
**A:** ref 包基本类型用 `.value`；reactive 代理对象，解构丢响应用 `toRefs`。

**579. Q: `computed`/`watch`？**  
**A:** computed 缓存派生；watch 做副作用，别在 computed 里改状态。

**580. Q: `v-once`？**  
**A:** 静态子树只渲染一次，大数据列表片段可用。

**581. Q: 虚拟列表？**  
**A:** 只挂载可视区 DOM，长列表降压力。

**582. Q: SSR hydration？**  
**A:** 客户端接管服务端 HTML，不匹配会警告或闪烁。

**583. Q: TypeScript `unknown`？**  
**A:** 比 `any` 安全，先收窄再使用。

**584. Q: ESModule vs CJS？**  
**A:** ESM 静态分析利于 Tree Shaking；Node 历史 CJS 多。

**585. Q: 事件捕获/冒泡？**  
**A:** 先捕获到目标再冒泡，`stopPropagation` 截断。

**586. Q: `fetch` 注意？**  
**A:** 4xx/5xx 默认不抛，要手动 `response.ok` 检查。

**587. Q: Web Worker？**  
**A:** 后台线程跑重计算，避免卡主线程，通信用 postMessage。

**588. Q: CSP 一句？**  
**A:** 内容安全策略限制脚本来源，降 XSS 面。

**589. Q: Tree Shaking？**  
**A:** 打包删未用代码，ESM 静态结构友好。

**590. Q: `.env` 注意？**  
**A:** 密钥不进仓库，生产用密钥管理，示例用 `.env.example`。

---

## 框架基础查漏补缺（591～610）

**591. Q: Servlet 生命周期？**  
**A:** init/service/destroy，一实例多线程请求，Spring MVC 底下还是 Servlet。

**592. Q: Filter vs Interceptor？**  
**A:** Filter 在 Servlet 容器链；Interceptor 在 DispatcherServlet 里，层次不同。

**593. Q: `DispatcherServlet`？**  
**A:** 前端控制器统一分发 HandlerAdapter，Spring MVC 心脏。

**594. Q: Bean 生命周期大意？**  
**A:** 实例化→注入→Aware→BeanPostProcessor→init→使用→销毁回调。

**595. Q: 构造器注入为啥推荐？**  
**A:** 依赖齐全才能创建，利于不可变与单测，`@RequiredArgsConstructor` 常用。

**596. Q: `@Transactional` 失效常见？**  
**A:** 自调用绕代理、非 public、异常被吞、错传播、多线程。

**597. Q: Spring AOP 代理？**  
**A:** 有接口 JDK 动态代理，无接口 CGLIB，理解代理边界。

**598. Q: `@RestController`？**  
**A:** `@Controller`+`@ResponseBody`，返回体走消息转换器变 JSON。

**599. Q: `@RequestBody`？**  
**A:** HTTP 体反序列化到对象，常配合 JSON。

**600. Q: MyBatis `#{}` `${}`？**  
**A:** `#` 预编译防注入；`$` 字符串替换只用于白名单动态列名表名。

**601. Q: MyBatis 二级缓存？**  
**A:** namespace 级，分布式下易脏读，生产常关或配合一致性方案。

**602. Q: Spring Boot 自动配置？**  
**A:** `spring.factories`/`AutoConfiguration.imports`+条件注解装配 starter。

**603. Q: Actuator 生产？**  
**A:** 关敏感端点或加认证，别暴露 env/heapdump。

**604. Q: Redis 单线程？**  
**A:** 命令执行单线程模型，慢命令会拖全局，6+ 有 IO 线程辅助。

**605. Q: RDB vs AOF？**  
**A:** 快照快可能丢多；AOF 耐久体积大，混合持久化折中。

**606. Q: 缓存穿透击穿雪崩？**  
**A:** 不存在查询、热点过期、集体过期；布隆、互斥、随机 TTL、限流组合拳。

**607. Q: Lua 在 Redis？**  
**A:** 脚本原子执行，适合库存扣减逻辑打包。

**608. Q: 会话 Spring Session？**  
**A:** 把 HttpSession 外置 Redis 等，集群共享登录态。

**609. Q: `@Valid`/`@Validated`？**  
**A:** JSR-303 校验入参，配合全局异常返回可读错误。

**610. Q: 接口版本兼容一句？**  
**A:** URL 头/参数带 v1，弃用期双读，文档写清楚 breaking change。

---

## 附：背诵节奏建议（与本文件配套）

- 全文件 **610** 条：每天背 **50～80** 条，遮住 A 自测。  
- 错题打星，第二天先复习星标。  
- 搭配 `INTERVIEW_MEMORIZATION_PLAN.md` 的控场句，避免发散。
