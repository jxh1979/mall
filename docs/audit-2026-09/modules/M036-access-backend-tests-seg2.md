# M036 功能权限点、职级三十档、权限模板与最终权限计算／后台验证／第2段
版本：V0.01

2026-09-21 首次审计收口（操作员 2026-09-20 指令：严格按 V0.17，逐模块检查点）。3 个测试文件（AccessServiceTests.java、AuthorizationServiceTests.java、M006JdbcRegressionTests.java）、52 判据；B1 双 FAIL（Grok GR-01／席甲 CX-M036-001）→ B2-r1 双 PASS 定稿 M036-001（细化）、002（技术性）→ B3-r1（席甲：发布口径 BASE 54＋21＋12 → 三个变异各自 RED → GREEN 54＋22＋13）→ B4-r1 双 PASS → B5-r1 双 PASS：Grok V0.01 SHA256 748e3539d8509ac4b46c3650fbb931338389876be04a78bf17251e95c3d312d7；席甲 V0.01 SHA256 d0993e8016972b84ebcc409960cdb2af6bd9df5c7b4f113d651cdc980dc9d8b0；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。B3 累计 1。

修复摘要：001 岗位映射重算/停用收回的测试此前只断言内存仓的角色指针；内存仓不把岗位派生角色并入有效集、不撤旧岗位应用，与 JdbcAccessRepository 语义分叉，而真库层的岗位角色查询也没有任何测试（变异后全量 0 红）。现内存仓按 Jdbc 语义对齐，测试经有效权限集与模板应用状态断言，M006JdbcRegressionTests 补一条临时表真库测试守住该查询与置空收回。002 AuthorizationService 里「生效时间未到不算生效」是唯一的闸（SQL 不按生效时间过滤）且无测试；现补一条。被测代码未改。

D-029：AuthorizationServiceTests.java 主工作区版含未发布工作流改动，发布的是「origin/main 版＋同一补丁」。

跨模块观察（cross_module_observations）：
- W-M036-1…3（只在主工作区未发布版成立，不入队、不发布）：AuthorizationServiceTests V0.03/V0.04 锁未发布行为且依赖 origin/main 没有的常量；未发布的 Jdbc 查询用 grant_group_id，M006JdbcRegressionTests 手写临时表无该列（2 条报错）；未发布的 SupervisorReviewService 要求登记直接受益人，AccessServiceTests 6 条报错。相应工作流发布时须同批改这些测试与调用点。
- XM-M036-A（→各自模块）：origin/main 3ca1b507 全量后端测试 3471 个里有 20 个红，均与本模块无关（FinanceControllerTests 12、ComplianceControllerTests 3、FacilityEquipmentArchiveDeleteJdbcIntegrationTests 3、AmusementServiceTests 1、CustomerGuardedActionAndRejectionAuditTests 1）；后端测试不在 CI 门禁里。
- 其余登记：单元素循环与恒假条件；`markSingleUsePermissionConsumed` 内存实现恒 true；`assumeTrue` 缺环境整类跳过；手写临时表只含被测 SQL 用到的列；`copyPermissionTemplateKeepsSnapshot…` 源模板前后权限项相同；三个幸存但属第二道闸的变异。
