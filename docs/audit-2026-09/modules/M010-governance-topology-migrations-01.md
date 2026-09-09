# M010 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／数据库迁移／第1段
版本：V0.01

状态：PASS。范围：plan/modules.md V0.04 的 4 文件（V005 / V048 / V318 / V354，历史迁移正文未改）+ 本模块修复产物 V410__m010_business_entity_topology_and_role_isolation.sql；判据 GEN-001–018 共 18 项。B3 整批修复累计 2 次。M010-001–004 与 012 已修复；005–011、013、014 仅登记；当前 14 项修复队列全否。

B5-r2 按 Grok 真实退出→席甲（Claude Fable 5.1，作者自审票）主仓实跑并独立落盘→读取 Grok 顺序完成，两席全模块三线 PASS：
- [席甲完整报告](M010-b5-r2-self-raw.md)：V0.01，20044 bytes，SHA256 846ec98dc791f420982127215bfa21416f5234b8af73ed4b03467f6d0e88b9ac。
- [Grok完整报告](M010-b5-r2-grok-raw.md)：V0.01，32729 bytes，SHA256 25524c741f56e867dde01a6274f86194f1345ee0587b4c33a6b1a3098e672912。
- B5 后 actual/sidecar/ledger/精确本次 Grok raw 四值全等：b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。已先写 ledger V2.27 为 PASS，再创建本文件；LOCK-AMEND-COUNT 仍 1。

修复摘要（新迁移 V410，操作员裁定自 V410 起编号）：删除 V318 引入、与 HTML「多对多协作、不是固定隶属链」相反的 business_entities.parent_entity_id 列与索引（001，删列经操作员明确授权；六行主体骨架的稳定 id/名称/角色保留）；entity_role 白名单收为 HTML 三角色 platform / project_company / management_company，存量 operation/spv 行坍缩为 management_company（002/003，操作员按 2.2 答「删除」，questions/M010-002.md、M010-003.md）；roles 由全局 UNIQUE(role_name) 改为「全局名唯一 + 组织内名唯一」表达式唯一索引，使 V048 的组织隔离可用（004）。M011 PlatformJdbcIntegrationTests 三条最小 JDBC 断言红→绿；结算两测试、M011 六处夹具与 analytics 一处夹具（012）作最小伴生调整。

环境：原 complex_test（含未发布 V400–V409）改名为 complex_test_pre_v410_20260909 原样保留；新 complex_test 由 Flyway 从 V001 迁到 V410，MigrationVersions 断言 410=410（M009-017 的环境红随之消失）。登记项：005（V005 英文权限名已被 V251 覆盖）、006（历史迁移注释不实，不可改）、007（本库不加硬 FK）、008（business_entities.status 无 CHECK、V354 的 inactive 与 M009 状态集分叉）、009（版本号/测试库已处置）、010（下游测试已随批处理）、011（其余观察）、013（roles 唯一索引语义说明）、014（结算 S1 文案残留）。正本三 HTML/锁/权限种子/历史迁移未修改，未提交/合并/推送/部署。
