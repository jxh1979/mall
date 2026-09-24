# XM-M031-B 失败留痕随调用方事务回滚丢失（合成模块，D-031）
版本：V0.01

2026-09-24T06:28:51Z 收口（操作员 2026-09-23「解决这些」；ledger D-031 按 D-027 先例单独立项）。来源：M031 收口登记转 M092 的跨模块项。范围：M092 审计写入链 AuditEventService、AuditEventRepository、JdbcAuditEventRepository、AuditedOperationExecutor、AuditEvent 与 M095 AuditEventServiceTests；B2-r2 定稿把回归放进 M095 的 TraceabilityJdbcIntegrationTests。判据取锁内 M092、M095 映射并集 46 项，只读引用。

流程：B1 双 FAIL（Grok GR-01／席甲 CX-01、CX-02）→ B2-r1 双 PASS 定稿 XM-M031-B-001（采 Grok 的方法级注解写法与 4a066def 重数）→ B2-r2 双 PASS（回归宿主由 EvidenceEventJdbcIntegrationTests 改为 TraceabilityJdbcIntegrationTests：前者 @BeforeEach 清 evidence_events，在不带事务的方法里会真提交）→ B3-r1（2 文件 75 行新增，冻结 /home/james/audit-tmp/xm031b_b3r1.diff SHA256 3967a92b09840dda73033e5bd01d1ddd4880608b9466b790d044653d73955a6b；RED 两条新回归红 → GREEN 3539 测 0 红）→ B4-r1 双 PASS（2 块越界 0）→ B5-r1 双 PASS：Grok V0.01 SHA256 02203e9a7cced7b6b3652d423e3b97af33b752af6c0a5a794b10b7abf267e74b；席甲 V0.01 SHA256 4a85fc93a3cb97565618b4cc533aaa085efd220ce7a5786c7e37876027d08b46；四值全等 b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458。B3 累计 1。Grok 票均为 grok-4.7/xhigh。

根因与修复：AuditEventService 无事务注解，recordFailure 经同一 JDBC 模板参与调用方事务。①带 @Transactional 的写方法失败时，catch 里记的失败事件随业务回滚一并消失（探针：不带事务 1 行、带事务 0 行）；②调用方事务已因 SQL 错误中止时，同一事务内写失败事件本身抛「current transaction is aborted」，生产 catch「recordFailure(...); throw exception;」的原异常被顶掉。修复：只给 recordFailure 加 @Transactional(propagation = REQUIRES_NEW)，失败事件在独立事务里提交；成功事件仍随业务事务回滚。影响面（4a066def）：46 个调用方文件、155 处调用，其中 25 个文件带 @Transactional、31 处调用落在带注解的方法里，一处修复全部覆盖。依据：治理地图第 893、899、902、960 行，BSM 第 1583、1599、1845、1995 行。

跨模块观察（cross_module_observations）：
- 观察（→M084）：IdempotencyService 注释「reserve 仍在外先提交……失败留痕在外照常落」只相对其模板自建事务成立；调用方带 @Transactional 时占位与 markRetryableFailure 随外层回滚，重试可再占位，不改。
- 观察：独立事务每次失败多借一个连接片刻，连接池默认 10，只在约池大小的请求同时卡在 recordFailure 时才会等连接。
- 不立案：AuditEventService 终端字面量恒为 web；errorCode 为空时 Map.of 抛空指针（正常工厂均传非空码）；读路径坏 JSON 收成空 map。
- 测试宿主经验：方法级 NOT_SUPPORTED 会让类里 @BeforeEach 的清表等夹具写操作真提交，选回归宿主前须先读夹具。
