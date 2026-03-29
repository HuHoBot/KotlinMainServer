# HuHoBot-Websocket-Kotlin v0.0.3

refactor: 重构代码以使用Kotlin数据类和协程
- 重构多个类为数据类以简化代码结构，使用协程替代线程处理异步任务，优化事件处理逻辑和网络通信，提升代码可读性和性能。
- 移除冗余的init块和手动属性赋值，改为使用主构造函数。将线程操作替换为协程以提高并发性能。引入EventContext简化事件处理参数传递。优化异常处理和日志记录。
- 重构ClientManager和BanManager以提升线程安全性和性能。简化WebsocketServer的消息处理流程，移除冗余代码。使用apply和let等Kotlin特性使代码更简洁。
- 这些改动提高了代码的可维护性，减少了潜在的内存泄漏风险，并使异步操作更高效可靠。

