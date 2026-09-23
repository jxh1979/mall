# XM-M038-A 全链下钻两份测试按已定稿页面行为同步（合成模块，D-031）
版本：V0.01

2026-09-23 收口（操作员 2026-09-23「解决这些」；ledger D-031 按 D-027 先例单独立项）。来源：modules/M038-access-web-seg1.md 登记的 XM-M038-A（origin/main 前端全量单测基线失败）。范围 2 个测试文件：apps/web/src/admin/pages/TraceLinkDrilldown.test.tsx（属 M081）、apps/web/src/admin/pages/TraceLinkP2c.test.tsx（属 M082）；判据取锁内 M081/M082 映射（同一组 21 项），只读引用。B1 双 FAIL（Grok GR-01…06、GR-07 只登记／席甲 CX-01…06）→ B2-r1 双 PASS 定稿 XM-M038-A-001…006 → B3-r1（origin/main 一次性 worktree：BASE 11 测 5 红 → GREEN 8 测全绿；006 变异转红；前端全量 978 测全绿）→ B4-r1 双 PASS → B5-r1 双 PASS：Grok V0.01 SHA256 828f2fd2a0be247fd78cf736461bea89927d2f018edb550c484835fe8c9b3eb2；席甲 V0.01 SHA256 382fd06e687b3efcb22a2a31a68ab2c4180e30f6497e729c157a30b4287ee56a；四值全等 b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458。B3 累计 1。Grok 票均为 grok-4.6/xhigh（操作员改用 grok-4.7 的指示到达时本模块 B5 已在运行）。

根因：5 条红全是测试过期，页面未回归。3 条随 M023 检查点 b6d8e203（M023-005 把商户门户三页的下钻改指商户可达页或降为纯文本；处置写了「测试同步」，但这两份测试属 M081/M082、不在 M023 写入集合）；2 条随 M012 发布 0b272eec（治理拓扑与组织商户页改分页取数，测试桩只桩 get）。两处检查点只跑本模块测试，verify:predeploy 不跑前端单测，故未拦下。

修复摘要：001、003、004 删除三条过期用例（现行行为已由 MerchantZoneSettlementsPage.test.tsx、MerchantZoneContractsPage.test.tsx、MerchantScopePage.test.tsx 守住，变异证据 /home/james/audit-tmp/xm038a_mut2.log），并删去随之闲置的两行导入。002、005 两份 makeApi 的 getPaged 按同一路由表返回分页形状。006「无 navigateTo 时降级纯文本」的否定断言改查任何 trace-link- 前缀元素（修前在「无 navigateTo 仍出按钮」变异下不失败）。未改页面、TraceLink、AdminApp、路由、客户端与后端。

跨模块观察（cross_module_observations）：
- GR-07（→M285）：「我的账单」ADM-MER-003（商户专页）来源合同下钻指 ADM-CONT-001（需 contract.draft.manage），商户账号点了静默不跳，与 M023-005 同根；TraceLinkP2c.test.tsx「我的账单来源合同」用例锁住该目标，M285 修页面时须同批改该用例，否则重演本项。
- OBS（→M003/M005）：`verify:predeploy`（package.json）与 `.github/workflows/predeploy-gates.yml` 不跑前端/后端单测，是本项与 XM-M036-A 基线红积累的根因（两席定为只登记，不在本模块加门禁）。
- 其余登记：两份测试各复制一份 makeApi/baseProps；测试不经 AdminApp.navigateTo，测不到无权限时的静默拦截。
