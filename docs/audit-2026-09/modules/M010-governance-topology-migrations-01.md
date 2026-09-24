# M010 主体拓扑与角色隔离迁移复查
版本：V0.04

2026-09-22 Codex补充复查：PASS。Grok NOT-CALLED，按操作员“无问题只自己审计”执行；不冒称当前新双PASS。历史双席结论完整保留。

4份冻结迁移203行+纠正迁移V410 26行/18GEN完整复查。当前complex_test只读查询exit0：父链列/索引已无，角色CHECK三值，组织内角色名唯一，8个权限名称中文；5份迁移文件实算checksum均与应用记录一致。未发现新问题，14项既有队列保持全否。

本轮未重跑迁移、未写数据库、未修改代码或HTML；复用同会话M009已执行24项JDBC通过证据，未冒称本轮新执行。B3累计2、本轮修复0。现锁b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458，LOCK-AMEND-COUNT1；旧Grok报告属于历史，不作为本轮第四值。

完整报告：M010-b5-r2-self-raw.md当前V0.03，SHA256 992ddb28d54a9f4c70d10734544ea607ef54f7aff11562fbf3d13c02b22ba994。未提交、推送或部署。

## 历史V0.02全文（保留，非本次事实）

# M010 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／数据库迁移／第1段
版本：V0.02

2026-09-14 Codex补充复查：PASS；Grok NOT-CALLED。本次按用户“无问题只需自己审计；发现问题再走原完整流程”执行，未冒充新双PASS。历史Claude Fable 5.1/Grok B5-r2双PASS保留。B3累计2，本次修复0。

已全文复查4份冻结迁移（203行）及直接纠正迁移V410，完成18个GEN判据、第一性原理/顺从式/对抗式三线。当前complex_test只读核验exit0：旧固定父链列/索引均0，主体角色只允许3类，组织内角色名唯一，旧英文权限名已由V251替换；相关迁移005/048/251/318/354/410/427均成功。复用本会话已执行的23条PlatformJdbcIntegrationTests通过结果，未重复跑、不称本次新执行。无新问题；历史14项仍全否（5已修、9登记）。

[本次Codex完整报告](M010-b5-r2-self-raw.md)：V0.02，32232bytes，SHA256 fb375eff1cbcd320557d9caa30e420ee93a4d44e3a187a140c233e225b58955a；原V0.01的20044byte报告完整保留。旧Grok报告V0.01未写入，SHA25625524c741f56e867dde01a6274f86194f1345ee0587b4c33a6b1a3098e672912保持。锁实算/sidecar/ledger=b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a；LOCK-AMEND-COUNT=1；旧Grok锁相同但不充当本次调用。

HEAD e456ba96c84185c8d39f18f548011daac48a6466，main。三HTML/源码/迁移均未改，未提交推送合并部署。先核验并写ledger V3.74保留PASS，再更新本文件。

---
以下为历史V0.01原文（当时库V410、当时席甲Claude，非本次事实）：

# M010 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／数据库迁移／第1段
版本：V0.01

状态：PASS。范围：plan/modules.md V0.04 的 4 文件（V005 / V048 / V318 / V354，历史迁移正文未改）+ 本模块修复产物 V410__m010_business_entity_topology_and_role_isolation.sql；判据 GEN-001–018 共 18 项。B3 整批修复累计 2 次。M010-001–004 与 012 已修复；005–011、013、014 仅登记；当前 14 项修复队列全否。

B5-r2 按 Grok 真实退出→席甲（Claude Fable 5.1，作者自审票）主仓实跑并独立落盘→读取 Grok 顺序完成，两席全模块三线 PASS：
- [席甲完整报告](M010-b5-r2-self-raw.md)：V0.01，20044 bytes，SHA256 846ec98dc791f420982127215bfa21416f5234b8af73ed4b03467f6d0e88b9ac。
- [Grok完整报告](M010-b5-r2-grok-raw.md)：V0.01，32729 bytes，SHA256 25524c741f56e867dde01a6274f86194f1345ee0587b4c33a6b1a3098e672912。
- B5 后 actual/sidecar/ledger/精确本次 Grok raw 四值全等：b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。已先写 ledger V2.27 为 PASS，再创建本文件；LOCK-AMEND-COUNT 仍 1。

修复摘要（新迁移 V410，操作员裁定自 V410 起编号）：删除 V318 引入、与 HTML「多对多协作、不是固定隶属链」相反的 business_entities.parent_entity_id 列与索引（001，删列经操作员明确授权；六行主体骨架的稳定 id/名称/角色保留）；entity_role 白名单收为 HTML 三角色 platform / project_company / management_company，存量 operation/spv 行坍缩为 management_company（002/003，操作员按 2.2 答「删除」，questions/M010-002.md、M010-003.md）；roles 由全局 UNIQUE(role_name) 改为「全局名唯一 + 组织内名唯一」表达式唯一索引，使 V048 的组织隔离可用（004）。M011 PlatformJdbcIntegrationTests 三条最小 JDBC 断言红→绿；结算两测试、M011 六处夹具与 analytics 一处夹具（012）作最小伴生调整。

环境：原 complex_test（含未发布 V400–V409）改名为 complex_test_pre_v410_20260909 原样保留；新 complex_test 由 Flyway 从 V001 迁到 V410，MigrationVersions 断言 410=410（M009-017 的环境红随之消失）。登记项：005（V005 英文权限名已被 V251 覆盖）、006（历史迁移注释不实，不可改）、007（本库不加硬 FK）、008（business_entities.status 无 CHECK、V354 的 inactive 与 M009 状态集分叉）、009（版本号/测试库已处置）、010（下游测试已随批处理）、011（其余观察）、013（roles 唯一索引语义说明）、014（结算 S1 文案残留）。正本三 HTML/锁/权限种子/历史迁移未修改，未提交/合并/推送/部署。


## 2026-09-22 当前锁完整双席重审
按操作员最新指令从M010完整B1重开。B1：Codex V0.04/Grok V0.03 PASS；B2：Codex V0.05并V0.06措辞收束、Grok V0.04 PASS；findings V3.52当前14项队列全否、9观察完整保留，013三边界补齐。跳过新增B3/B4；历史B3累计2不清零。B5-r2：Codex V0.04/Grok V0.02 PASS，Grok真实exit0，四值全等。ledger已先置PASS，本节不是单席复查。
范围：四冻结SQL203行+既有修复V410全文26行，18GEN。实际只读schema/Flyway/checksum全一致；未新跑JDBC，不宣称全项目全绿。无本次修复、无HTML改动。Grok B1临时建库违反派发限制已如实登记；库已清理，Codex额外SELECT确认无残留；B2/B5禁止建删库且未再执行。旧报告字节保留。
锁SHA：b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458；LOCK-AMEND-COUNT：1。
B5 self SHA：03477f08fd20a6743c145272e7947d0430016dd52c54f7b77be943760630ebc0。
B5 Grok SHA：f617c513ad4654f4fa10d9507e52e1da5f911c36ab0a4e68be6774dbf044751b。
