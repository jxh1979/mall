# M013 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第1段
版本：V0.01

状态：PASS。范围：plan/modules.md V0.04 的 4 文件（docs/business-entity-binding-development-design-v0.1.md、docs/group-governance-rebuild-prd-v1.0.md、docs/platform-governance-acceptance-pack-v0.2.md、docs/platform-governance-prd-v0.1.md；计划冻结 1517 行）；判据 84 项（BEQR 21、BSM 12、GEN 18、PGM 33）。B3 整批修复累计 1 次。M013-001–008 已修复（细化 5、技术性 2、冲突 1）；M013-009 为跨模块观察（OpenAPI `BusinessEntityRequest.entity_role` 旧六值，归 M392）；M013-010 登记观察。HTML 修改申请 1 项（治理地图 L741「考勤打卡」→「查看考勤」）经操作员逐句批准，已于 PR #433 落地为治理地图 V0.12。

- B1：Grok `modules/M013-b1-grok-raw.md` V0.01（FAIL，GR-001…005）；席甲 `modules/M013-b1-self-raw.md` V0.01（FAIL，CX-001…007）。
- B2-r1：候选 findings V1.84 → 双 PASS 定稿 V1.85（Grok raw V0.02 追加段、席甲 raw V0.02 追加段）。
- B3-r1：席甲在主工作区修 4 份文档（绑定设计 V0.09、集团 PRD V1.01、验收包 V0.04、PRD V0.04）。
- B4-r1：`modules/M013-b4-r1-grok-raw.md` / `modules/M013-b4-r1-self-raw.md` 双 PASS（内容与 §5 闸 3 归因）。
- B5-r1：`modules/M013-b5-r1-grok-raw.md`（44522 bytes SHA a14fde458071763182a64d9463991f8b249fc4fd0da17dc738ecd7786dc89094）/ `modules/M013-b5-r1-self-raw.md`（17768 bytes SHA 79f602ed4eb5a2a045aeca9686143b935df5eb4147543ddff1bb3380dcbffac9）双 PASS；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。
- 台账：ledger V3.21 先记 PASS，再写本文件；findings 最终 V1.86 口径。
- 跨模块观察（cross_module_observations）：M013-009 → M392（OpenAPI 契约闭合第 5 段）：`docs/openapi/openapi-p0.yaml:29205-29215` `entity_role` enum 应收为 platform / project_company / management_company。
