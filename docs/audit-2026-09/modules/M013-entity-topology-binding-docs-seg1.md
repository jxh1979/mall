# M013 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第1段
版本：V0.02（2026-09-19T21:53:15Z；V0.01 为 2026-09-11 B5-r1 收口记录，其内容以下按时序并入）

状态：PASS（B5-r5 双 PASS，2026-09-19T21:53:15Z）。范围：plan/modules.md V0.04 的 4 文件（docs/business-entity-binding-development-design-v0.1.md、docs/group-governance-rebuild-prd-v1.0.md、docs/platform-governance-acceptance-pack-v0.2.md、docs/platform-governance-prd-v0.1.md；计划冻结 1517 行，现 1533 行）；判据 84 项（BEQR 21、BSM 12、GEN 18、PGM 33）。B3 整批修复累计 5 次（3 次额度 + 操作员 2026-09-19 两次批准追加）。M013-001–024：21 项已修复（001–008、011–016、018、019–024），009 跨模块观察（OpenAPI `BusinessEntityRequest.entity_role` 旧六值，归 M392），010 登记观察（含 B5-r3 O-B5-01/O-B5-03 续记），017 跨模块缺口（集团总览项目级结果过滤，归 M348、依赖 M032）。HTML 修改申请 1 项（治理地图 L741「考勤打卡」→「查看考勤」）经操作员逐句批准，已于 PR #433 落地为治理地图 V0.12；此后无 HTML 申请。

- B1：Grok `modules/M013-b1-grok-raw.md` V0.01（FAIL，GR-001…005）；席甲 `modules/M013-b1-self-raw.md` V0.01（FAIL，CX-001…007）。
- B2-r1：候选 findings V1.84 → 双 PASS 定稿 V1.85。B3-r1：修 4 份文档（绑定设计 V0.09、集团 PRD V1.01、验收包 V0.04、PRD V0.04）。B4-r1 / B5-r1 双 PASS（`modules/M013-b5-r1-grok-raw.md` 44522 bytes SHA a14fde45…；`modules/M013-b5-r1-self-raw.md` 17768 bytes SHA 79f602ed…）。ledger V3.21 记 PASS。
- 2026-09-14 Codex 预检发现实现说明滞后，恢复 B1 完整双席：B1 双 FAIL → B2-r2 定稿 011–016 → B3-r2 → B4-r2 合取 FAIL（版本行）→ B2-r3 定稿 +018 → B3-r3 → B4-r3 双 PASS → B5-r3。
- B5-r3（2026-09-19，席甲由 Claude 接替 Codex；Grok 因 Codex 派发未取得退出码判交付失败、原命令重派 1 次）：两席均 FAIL（席甲 CX-B5-01/02/03；Grok GR-B5R3-01/02）→ BLOCKED-3ROUNDS → 操作员批准追加一次修复 → B2-r4 定稿 019–023（019 冲突：删四文档中与 PGM L658/L669 相反的「仅职级变化/责任范围变化」排除；020 集团 PRD request_type 六类；021 验收包 G7 删「所有对象继续携带 project_id」；022 绑定设计补 V427 项目关联与关联端点；023 绑定设计披露 OpenAPI status 四值裂口）→ B3-r4（四文档 22 处，绑定 V0.12 / 集团 V1.03 / 验收包 V0.06 / PRD V0.06）→ B4-r4 双 PASS（`modules/M013-b4-r4-self-raw.md` 4d7a6f5b…；`modules/M013-b4-r4-grok-raw.md` f5ca9bab…）。
- B5-r4：席甲 FAIL（CX-B5R4-01：023 拟改写入的括注「归档走存档端点」对主体档案不成立——PlatformController 无 business-entities 存档端点，提示语为商户口径共用）/ Grok PASS → 合取 FAIL → BLOCKED-4ROUNDS → 操作员再次批准追加一次修复 → B2-r5 定稿 024（细化，方案 A 删括注）→ B3-r5（绑定设计 3 处，V0.13）→ B4-r5 双 PASS（`modules/M013-b4-r5-self-raw.md` 9ca77c99…；`modules/M013-b4-r5-grok-raw.md` 见 ledger V4.25）。
- B5-r5：`modules/M013-b5-r5-grok-raw.md`（29508 bytes SHA 366f6d3459faa0548ef2ff68f937b967c6d3ceadb87f20f298ebbe6c17c5fed3）/ `modules/M013-b5-r5-self-raw.md`（12931 bytes SHA 2f34738470eccc0ffbeede61ccd8268382525306a6c857878e9c0f75bcee0c7d）双 PASS；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。
- 台账：ledger V4.27 先记 PASS，再写本文件；findings 最终 V2.38 口径（唯一当前表「当前 M013 B4-r5双PASS后队列：001–024全部否」）。
- 跨模块观察（cross_module_observations）：
  - M013-009 → M392（OpenAPI 契约闭合第 5 段）：`docs/openapi/openapi-p0.yaml` `BusinessEntityRequest.entity_role` enum 应收为 platform / project_company / management_company。
  - M013-023 → M392：同 schema `BusinessEntityRequest.status` enum（draft / active / suspended / archived）与服务 `SUBJECT_STATUSES` 五值不一致。
  - M013-017 → M348（依赖 M032）：`GET /api/v1/analytics/group-overview` 项目级调用者的汇总结果未按本项目过滤。
  - M013-019 同根句 → M014 `docs/platform-governance-product-spec-v0.2.md:1979`、M421 `docs/business-html-source-of-truth-acceptance-spec-v0.1.md:67`（RULE-GOV-ROLE-CHANGE-01）、M439 `docs/product-requirements-v0.1.md:601`、`:650`：仍含与 PGM L658/L669 相反的「仅职级变化」排除。
  - O-B5R4-01 → PlatformService 所属后台模块：`requireSubjectStatus` 提示语「归档请使用存档操作」被创建主体与商户档案更新共用，对主体档案调用方误导（主体无存档端点）。
