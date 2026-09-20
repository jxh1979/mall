# M023 商户档案、商户类型与商户侧门户／网页与验证／第1段
版本：V0.01

2026-09-20 首次审计收口（操作员 2026-09-20 指令：严格按 V0.17，逐模块检查点）。6 个前端文件、21 判据；B1 双 FAIL（Grok GR-01…06、席甲 CX-M023-001…005）→ B2-r1 双 PASS 定稿 M023-001…006（006 冲突，其余细化）→ B3-r1（席甲，TDD：RED 18 例失败 → GREEN 31 例 + tsc 通过）→ B4-r1 双 PASS → B5-r1 Grok FAIL（GR-01：合同字典键 draft 对不上后端 drafting；席甲 PASS 撤回）→ B2-r2 双 PASS 定稿 M023-007 → B3-r2（2 文件，TDD）→ B4-r2 双 PASS → B5-r2 双 PASS：Grok V0.01 SHA256 79de241f9c148c4569289323b1c69e29ddd3ef8b0a67888b39f9d54488fd49e4；席甲 V0.01 SHA256 168011600f4983dec64232fef87fb55d59ef3b071c53668836ee0c896905bcc7；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。B3 累计 2。

修复摘要：001 商户授权工作台按后端真实字段展示（金额读 totalAmountCent 分转元，删后端不存在的列与 KPI，夹具改真实形状）；002 删除不调后端却宣称留痕的「确认授权范围」，事件名/权限码改真实值；003 三页状态全中文（账单页内字典、结算补「已作废」、未知码中文占位）；004「未结清」排除已结清与已作废；005 下钻只保留商户可达目标（对账页合同 → 我的合同），其余纯文本；006 对账页展示结算明细数字并恢复留言（既有 note-only 端点、权限门控；文案写明留言不改状态、不暂停付款、退租金额异议的正式处理走退租审批——同时满足业务地图 L1322 与枚举 L252）；007「我的合同」起草态按后端真码 drafting 显示「草拟」。同批带入 2026-09-11 PR #444 三个本地提交的内容。

跨模块观察（cross_module_observations）：后端合同第 9 态 `amended`（V314，被变更子合同接替的原合同）在前端无中文名、三份 HTML 未命名（枚举 L239 只列八态）——本页显示「未知状态」，命名属业务用词未自拟，归合同状态机模块（M254/M041），已报操作员；「我的对账」无月度结算与分成明细（后端/产品一期范围，已报操作员）；`tests/e2e/full-admin-remaining.spec.ts:896-898` 与 `docs/full-admin-ui-acceptance-matrix-v0.1.md` UI-ADM-MER-002 仍期待「确认授权范围」伪动作（随 002 过时，属 e2e/文档模块）；ADM-MER-002 在 governance 组且无 accountForms；`MerchantBillingService.listBills` 的 findAllBills 分支与 M020-001 同一错误前提；`types.ts` 的 MerchantScopeBillRow/WorkOrderRow 含后端不存在的字段（本模块已不再引用）。
