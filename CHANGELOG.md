# HuHoBot-Websocket-Kotlin v0.0.2

feat(server): 实现服务器连接频率限制与封禁机制- 新增 BanManager.queryBanReason 方法用于查询封禁原因
- 在 ClientManager 中添加连接尝试计数器和时间窗口限制
- 实现 recordConnectionAttempt 方法检测频繁连接并自动封禁
- 修改握手处理逻辑，增加对频繁连接的封禁判断
- 更新日志记录格式，移除多余的 [Websocket] 前缀- 优化 Server_handle_ShakeHand 中的握手包发送逻辑
- 在 helpCommand 和 queryCommand 中添加封禁查询功能- 移除 ShadowJar 的最小化配置以避免依赖冲突
- 为 BotClient 添加独立的日志记录器- 调整常量命名规范并增强连接超时检测逻辑

