# M026 租户范围与数据隔离（技术不变量）／后台实现／第1段
版本：V0.01

2026-09-20 首次审计收口（操作员 2026-09-20 指令：严格按 V0.17，逐模块检查点）。1 个文件 backend/src/main/java/com/complex/platform/common/jdbc/JdbcTimestamps.java、21 判据；B1 Grok FAIL（GR-01）／席甲 PASS 经复核撤回 → B2-r1 双 PASS 定稿 M026-001（技术性）→ B3-r1（席甲：修前检查 RED → 修后 GREEN + 单文件 javac；方法体指纹不变）→ B4-r1 双 PASS → B5-r1 双 PASS：Grok V0.01 SHA256 9388291edbbf143dcf55e6388cc9cdfcdeec3fec2e73009a6a09a9debb383316；席甲 V0.01 SHA256 ba50fc9cd4391a834f665c1e3dfe3b18bb8a61d307a698d03a0a724246c66d33；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。B3 累计 1。

修复摘要（文件加版本 V0.01）：001 类注释去掉「已把各 JDBC 仓储与参数源里逐字复制的转换全部收口到本类」的完成口径（仓内尚有 9 处同构私有副本不经本类），改为只陈述本类是什么、由 import 本类者使用。两个方法体未动。

跨模块观察（cross_module_observations）：同构私有副本仍在别的模块——`JdbcMerchantServiceRepository:316`、`JdbcProductionReadinessRepository:197`（`getTimestamp().toInstant()` 写法）、`JdbcInventoryRepository:2177`、`JdbcTicketingRepository:1720`、`JdbcAmusementRowMapper:333` 等，另有 `TicketFacePhotoRepository`、`VenueBusinessHoursRepository` 的内联 `atOffset(ZoneOffset.UTC)`；`JdbcIdempotencyRepository` 写用本类、读走 `getTimestamp().toInstant()`；pgjdbc 42.7.11 对 timestamp without time zone 的两种读法在非 UTC JVM 下相差时区偏移（库内仅 flyway_schema_history.installed_on，无调用方）。
