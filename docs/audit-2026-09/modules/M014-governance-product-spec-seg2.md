# M014 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第2段
版本：V0.03

状态：PASS。2026-09-23T09:33:49.464436+00:00，B5-r8第二次 Codex/Grok V0.02双PASS；Grok真实exit0。

范围：docs/platform-governance-product-spec-v0.2.md V0.13，3521行/236455字节，SHA256 020cceb047d5511a65dd8c08d2f06425d52727ee86cca6e769b8f96eeaf84017；84条冻结判据。
B3累计8。001–038队列全否：34已修、019/020观察、027/028无工程缺陷；无添加项、待答问题或HTML修改申请。本轮仅029日志§7→§8、038十条→十一条及共同标题版本递增。
B4-r8双PASS后，B5第一次Grok就025详情“异常说明”提出FAIL；B2独立核证为事项展示而非异常班次必填，双方确认缺陷否、原文保留。未修复，按同r8再次完整B5，双方PASS。保留两次原票，不把旧FAIL改写成PASS。
证据：M014-b4-r8-self-raw.md / M014-b4-r8-grok-raw.md；M014-b1-self-raw.md末V0.13 / M014-b1-grok-raw.md末V0.10；M014-b5-r8-self-raw.md末V0.02 / M014-b5-r8-grok-raw.md末V0.02。
B5后锁四值实算/sidecar/ledger/Grok末段均为 b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458，LOCK-AMEND-COUNT=1。ledger先记PASS后写本收口。
三份业务HTML、程序、SQL与测试未改。文档审计不等于所有产品功能已实现，跨模块观察继续保留。
发布分支：feature/audit-m014-20260923。发布仅携带本模块规格及其审计证据、全局台账检查点；实际提交/推送/合并以该分支PR为准，不混入8个工具配置文件。

## 历史收口记录（原文保留）
# M014 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第2段
历史版本：V0.02（2026-09-20T03:47:35Z；V0.01 为 2026-09-11 B5-r1 收口记录，以下按时序并入）

状态：PASS（B5-r4 双 PASS，2026-09-20T03:47:35Z）。范围：plan/modules.md V0.04 的 1 文件（docs/platform-governance-product-spec-v0.2.md；计划冻结 3494 行，现 3505 行，OVERSIZE-SINGLE-FILE，按段全读）；判据 84 项（BEQR 21、BSM 12、GEN 18、PGM 33）。B3 整批修复累计 4 次（3 次额度 + 操作员 2026-09-19 获批追加 1 次）。修复队列 25 个 ID 全否（23 已修：001–018 r1、021–023 r2、024 r3、025 r4；019 XM→M261；020 登记观察）；添加项 0；questions 0；无 HTML 修改申请。

- 2026-09-11 首轮：B1 双 FAIL → B2-r2 定稿 18 项 → B3-r1（V0.05→V0.06）→ B4-r1 双 PASS → B5-r1 双 PASS（`modules/M014-b5-r1-grok-raw.md` 47722 bytes SHA 7e399d8d…；`modules/M014-b5-r1-self-raw.md` 16155 bytes SHA bb535ff7…）；ledger V3.28 记 PASS。
- 2026-09-19 重开（操作员「从 M009 重新独立审计，有问题按 V0.17 完整双席流程」；席甲 Claude 预检发现与 M013 收敛口径同根的问题）：B1 两席 FAIL（席甲 CX-M014-B1-01/02；Grok GR-M014-B1-01/02/03）→ B2 定稿 021（冲突：F9 强制补充规则与异常分支的职级排除/职级挡位）、022（细化：§18/§21「所有对象继续携带 project_id」假完成）、023（细化：业态列表「编号」列回灌已废止字段）→ B3-r2（单文件 11 处，V0.06→V0.07）→ B4-r2 双 PASS → B5-r2 合取 FAIL（席甲 PASS `modules/M014-b5-r2-self-raw.md` 11970 bytes SHA 681c122bf8f67e7c2ced620611fc3fd9032242e03584995f21d458d43788105d；Grok FAIL `modules/M014-b5-r2-grok-raw.md` 33526 bytes SHA 0213bb82f1e26aefbda852fd3bf84d043dc0b452476ebc9727bee9a39420fa2a：GR-M014-B5r2-01，席甲核实接受）→ B2-r3 定稿 024（细化：岗位/班次字段与 F7 异常分支回灌已废止的「启用职级/启用岗位/岗位当前不可分配员工」门控）→ B3-r3（单文件 7 处，V0.07→V0.08）→ B4-r3 双 PASS → B5-r3 两席 FAIL（席甲 `modules/M014-b5-r3-self-raw.md` 18292 bytes SHA e04ab20c925b5e05e2d4ed63781798b6f9bfa5acf531bf56f19649ea147b9146 CX-M014-B5r3-01；Grok `modules/M014-b5-r3-grok-raw.md` 33768 bytes SHA 682e21dbd16f4353451536b65ed8db79963ccc4a461fe7bd13981a66ac991082 GR-M014-B5r3-01，同根）→ BLOCKED-3ROUNDS → 操作员批准追加一次修复 → B2-r4 定稿 025（细化：班次「异常」状态门控回灌已废止规则：:484/:496/:1078/:2565/:2573）→ B3-r4（单文件 10 处，V0.08→V0.09）→ B4-r4 双 PASS → B5-r4 双 PASS（`modules/M014-b5-r4-grok-raw.md` 38834 bytes SHA 305e00b7cb4daa343e8d76e9709ae202be21a3c1fbf66d82598bbbe84c50fa4b；`modules/M014-b5-r4-self-raw.md` 14208 bytes SHA bbf1995a0f365e47502ee330d68ee721f832978a1d3398035cf8057db4d45bee）；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。
- 台账：ledger V4.54 先记 PASS，再写本文件；findings 最终 V2.49 口径（唯一当前表「当前 M014 B4-r4双PASS后队列：001–025全部否」）。
- 跨模块观察（cross_module_observations）：
  - M014-019 → M261：`docs/merchant-space-contract-model-v0.1.md:64-66` 仍把 V318 `parent_entity_id` 与旧白名单写成当前代码结构（V410 已删父链、三角色）。
  - M014-020 → governance/review 与 access 实现模块：§24.3 审批人解析仍含职级算术与 `specific_role`（M087 起审批人身份由组织层级节点决定，rank 作档位下限）；`POST /api/v1/access/permission-templates/{id}/copy` 仍在而规格已废止复制模板；BEQR-148「其他」收费模板未进字段表；`dealer_portal` 第十一端代码值未实现（V111 十码）。
  - M014-021 同根句 → M421 `docs/business-html-source-of-truth-acceptance-spec-v0.1.md:67`（RULE-GOV-ROLE-CHANGE-01）、M439 `docs/product-requirements-v0.1.md:601`、`:650`。
  - 席甲方法教训（已存记忆）：B5 投票前须对全文【已废止/不得回灌】条目做同根活句扫描（024、025 均为此型）。

