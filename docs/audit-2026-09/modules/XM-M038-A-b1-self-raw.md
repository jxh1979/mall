CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M038-A B1 修复前全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Opus 5.5（作者自审票，按 §4「合取不变、推翻权不对等」处理；同一席位自本会话起由 Opus 5.5 接续）。2026-09-23T21:00:32Z。
- 锁 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（现行 V0.14；实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。
- 合成模块 XM-M038-A（ledger D-031；来源 modules/M038-access-web-seg1.md 登记的 XM-M038-A；目标模块 M081/M082）。范围 2 个测试文件全文；判据＝锁内 M081/M082 映射（同一组 21 项），只读引用。
- 只读上下文：两文件渲染的 8 个页面、components/TraceLink.tsx、AdminApp.tsx `navigateTo`（L545 起）、adminRoutes.ts、findings.md 已定稿 M023-005、M012-013。
- 顺序：Grok 已先派（modules/XM-M038-A-b1-grok-raw.md），本报告落盘前未读其正文。只读承诺：未改任何已跟踪文件；探针与变异都在 origin/main 一次性 worktree `/home/james/scratch-xm038a` 里做，做完 `git checkout -- apps` 还原（dirty=0）。

## 1. 文件覆盖

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| apps/web/src/admin/pages/TraceLinkDrilldown.test.tsx | 131 | 6460 | 7425b66a9c6f9127451e5b85405e129c82524df184d89280a3d08d0decc55264 | 全读，6 条用例 |
| apps/web/src/admin/pages/TraceLinkP2c.test.tsx | 107 | 4797 | 6ecec6c0a1bd0f4f5deadaf37e04e5c95fb7278c39d02da1580a303f4eb4fcd8 | 全读，5 条用例 |

两文件主工作区与 origin/main bfb17f8b 逐字节相同。

## 2. 可执行证据原文

```
## 前端全量（origin/main bfb17f8b，xm038a_full.log）
 FAIL  src/admin/pages/TraceLinkDrilldown.test.tsx > 全链下钻 P1-A：源页引用字段成可点链接 > 我的对账合同列点击 → navigateTo(ADM-CONT-001, contractId)
 FAIL  src/admin/pages/TraceLinkDrilldown.test.tsx > 全链下钻 P1-A：目标页按 ?focus= 定位 > 治理拓扑带 focusObjectId(merchantId) → 高亮该商户行
 FAIL  src/admin/pages/TraceLinkP2c.test.tsx > 全链下钻 P2-C：商户门户源页引用可点 > 商户授权工作台：账单商户列点击 → navigateTo(ADM-GOV-002, merchantId)
 FAIL  src/admin/pages/TraceLinkP2c.test.tsx > 全链下钻 P2-C：商户门户源页引用可点 > 我的合同：合同号→ADM-CONT-001、空间→ADM-GOV-007 均可点
 FAIL  src/admin/pages/TraceLinkP2c.test.tsx > 全链下钻 P2-C：组织与商户档案按 ?focus= 定位 > 带 focusObjectId(merchantId) → 高亮命中的商户档案行，未命中行不高亮
 Test Files  2 failed | 204 passed (206)
      Tests  5 failed | 976 passed (981)

## 探针：只把两份测试 makeApi 的 getPaged 改为按同一路由表返回分页形状（xm038a_probe2.log，一次性 worktree，已还原）
     ✓ 合同工作台商户列点击 → navigateTo(ADM-GOV-002, merchantId) 218ms
     × 我的对账合同列点击 → navigateTo(ADM-CONT-001, contractId) 1011ms
     ✓ 无 navigateTo 时降级纯文本（不可点） 7ms
     ✓ 合同工作台带 focusObjectId → 高亮该合同行 43ms
     ✓ 审计事件页带 focusObjectId(类型:id) → 切操作日志视图并按业务类型/对象服务端反查 5ms
     ✓ 治理拓扑带 focusObjectId(merchantId) → 高亮该商户行 6ms
     ✓ 我的账单来源合同(sourceType=contract)点击 → navigateTo(ADM-CONT-001, sourceId) 31ms
     ✓ 我的账单非合同来源(sourceType=meter) → 不渲染合同链接 10ms
     × 我的合同：合同号→ADM-CONT-001、空间→ADM-GOV-007 均可点 1011ms
     × 商户授权工作台：账单商户列点击 → navigateTo(ADM-GOV-002, merchantId) 1010ms
     ✓ 带 focusObjectId(merchantId) → 高亮命中的商户档案行，未命中行不高亮 173ms
⎯⎯⎯⎯⎯⎯⎯ Failed Tests 3 ⎯⎯⎯⎯⎯⎯⎯
      Tests  3 failed | 8 passed (11)

## 变异：TraceLink 无 navigateTo 也渲染按钮，只跑「无 navigateTo 时降级纯文本」（xm038a_mut1.log，已还原）
 ✓ src/admin/pages/TraceLinkDrilldown.test.tsx (6 tests | 5 skipped) 31ms
      Tests  1 passed | 5 skipped (6)
```

## 3. 三线审计

### 3.1 第一性原理

- 这两份测试守的是 PGM-001/BSM-001 落到后台的「数据可溯源、全链可点击」：源页的对象引用渲染成可点下钻、目标页按 focusObjectId 定位高亮。
- 不可再拆的事实：①商户门户三页的下钻目标由已定稿 M023-005 改过——「我的对账」合同改指商户可达的 ADM-MER-006（`MerchantZoneSettlementsPage.tsx:191/221`），「我的合同」合同号/空间与「商户授权工作台」商户号无商户可达目标，降为纯文本（`MerchantZoneContractsPage.tsx:136-137`、`MerchantScopePage.tsx:175` 的 `display(row.merchantId)`）；理由是 `AdminApp.navigateTo` 对无权限目标静默 return，商户账号点了没反应。②治理拓扑页与组织商户页由 M012 发布 0b272eec 起改为 `api.getPaged` 分页取数（`GovernanceTopologyPage.tsx:74-93`、`OrganizationMerchantPage.tsx:140-153`）。③两份测试的 `makeApi` 只按路由表桩 `get`，`getPaged` 恒返回空（Drilldown L20、P2c L19）。
- 结论：5 条红全部是测试过期，不是页面回归。探针证实：只把 `makeApi` 的 `getPaged` 改为按同一路由表返回分页形状，两条定位用例即转绿（§2）；其余 3 条断言的是 M023-005 已删除/改指的目标。

### 3.2 顺从式

21 项逐项见 §4。

### 3.3 对抗式（grilling）

- Q 通过的 6 条都能失败吗？A 「合同工作台商户列」「合同工作台 focus」「审计事件页反查」「我的账单来源合同」「我的账单非合同来源」断言的 testid 与页面现行 pageId 一致，改坏页面即红。唯独 Drilldown L66-73「无 navigateTo 时降级纯文本」查的是 `trace-link-ADM-CONT-001-c-77`，而该页现在的链接是 `trace-link-ADM-MER-006-c-77`——否定断言落空。变异实测：TraceLink 改成无 navigateTo 也渲染按钮，该用例照样通过（§2）。→ CX-06。
- Q 定位用例红，会不会是页面丢了定位？A 探针补分页桩后两条都绿，高亮逻辑在（拓扑 `data-focus-node`、组织页 `organization-merchant-row-*` + `admin-row-selected`）。
- Q 「商户授权工作台」除了链接，还有别的原因红吗？A 有：页面按 `user.merchantId` 过滤账单（`MerchantScopePage.tsx:110/124`），测试用户无 merchantId → `scopeMissing`，表为空。修测试时须给用户商户归属，否则断言「纯文本」也会因无行而空转。
- Q 「我的账单来源合同 → ADM-CONT-001」通过，是否就对？A 不对，但缺陷在页面：「我的账单」是 ADM-MER-003（`adminRoutes.ts:89`，`accountForms: ["merchant"]`），合同工作台 ADM-CONT-001 需 `contract.draft.manage`，商户账号点了静默不跳——与 M023-005 同根。页面属 M099（`MerchantZoneBillsPage.tsx:125/151`），本合成模块不改页面，登记 OBS-1。
- Q 为什么 5 条红能在 origin/main 上留两三天？A `verify:predeploy`（package.json L96，M003）与 predeploy-gates.yml（M005）不跑前端/后端单测；各检查点只跑本模块测试。登记 OBS-2。
- Q 修法会不会顺手扩到页面或 TraceLink？A 不会：最小处置只改两份测试的断言与桩，页面、TraceLink、AdminApp、路由均属别的模块。

## 4. 21 判据逐项

| 判据 | 适用｜证据｜结论 |
|---|---|
| BSM-001 整体说明 | 适用｜测试断言的下钻目标须与已定稿页面行为一致；Drilldown L55-64、P2c L64-76/L78-90 断言 M023-005 已改指/删除的目标｜不通过（CX-01、CX-03、CX-04） |
| BSM-003 通用能力 | 适用｜两份测试不涉及增删改与引导流程｜通过 |
| GEN-001 必要性与最小性 | 适用｜11 条用例各有用途；两文件各复制一份 makeApi/baseProps（只登记，OBS-3）｜通过 |
| GEN-002 可执行正确性 | 适用｜origin/main 上 5 条失败（§2）｜不通过（CX-01…05） |
| GEN-003 测试有效性 | 适用｜5 条红；「无 navigateTo 时降级纯文本」变异下不失败（§2）｜不通过（CX-01…06） |
| GEN-004 信任边界校验 | 无本面｜测试文件无外部输入处理｜通过 |
| GEN-005 授权与最小权限 | 适用｜「商户授权工作台」页按 user.merchantId 过滤，测试用户无商户归属（CX-04 修时补）；其余无本面｜不通过（CX-04） |
| GEN-006 数据完整性 | 无本面｜无写路径｜通过 |
| GEN-007 并发与幂等 | 无本面｜无写路径｜通过 |
| GEN-008 接口与模式一致性 | 适用｜测试桩只桩 get，两页已改 getPaged（Drilldown L20、P2c L19）｜不通过（CX-02、CX-05） |
| GEN-009 错误处理与恢复 | 无本面｜测试不涉错误路径｜通过 |
| GEN-010 安全与敏感信息 | 适用｜桩数据无敏感信息｜通过 |
| GEN-011 可观测与审计证据 | 适用｜审计事件页反查用例断言 business_type/business_id 与返回事件名，能失败｜通过 |
| GEN-012 资源与性能边界 | 无本面｜通过 |
| GEN-013 构建配置与依赖 | 适用｜测试无新依赖；CI 不跑单测属 M003/M005（OBS-2）｜通过（本范围） |
| GEN-014 数据库迁移正确性 | 无本面｜通过 |
| GEN-015 界面可用性与可访问性 | 适用｜测试把「商户点了没反应」的链接当作应有行为（CX-01/03/04 同根）｜不通过（CX-01、CX-03、CX-04） |
| GEN-016 文档可验证性 | 适用｜用例名现在时声称「→ navigateTo(ADM-CONT-001…)」「均可点」「账单商户列点击」与页面不符｜不通过（CX-01、CX-03、CX-04，改断言时同步改名） |
| GEN-017 业务来源分类 | 适用｜见 §5，均按冲突→细化→技术性→添加顺序｜通过 |
| GEN-018 第一性原理证据 | 适用｜结论均落到源码行与可复现命令输出（§2）｜通过 |
| PGM-001 整体定位 | 适用｜全链可点击的受众可达口径由 M023-005 定稿，测试须与之一致｜不通过（CX-01、CX-03、CX-04） |

## 5. 本次 Findings（临时编号）

| 编号 | 位置与原句 | 正常触发 | 分类 | 判据 | 证据类型 | 最小处置 |
|---|---|---|---|---|---|---|
| CX-01 | TraceLinkDrilldown.test.tsx:55-64「我的对账合同列点击 → navigateTo(ADM-CONT-001, contractId)」，断言 `trace-link-ADM-CONT-001-c-77` 与 `("ADM-CONT-001","c-77")` | 在 origin/main 跑该文件即红：页面链接为 ADM-MER-006 | 细化（上位 PGM-001；目标口径已由 M023-005 定稿；不改业务） | BSM-001、PGM-001、GEN-002、GEN-003、GEN-015、GEN-016 | 类型 4（命令输出）＋源码行 | 断言与用例名改为 ADM-MER-006 |
| CX-02 | TraceLinkDrilldown.test.tsx:20 `getPaged` 恒空；L113-130「治理拓扑带 focusObjectId → 高亮该商户行」 | 同上即红：`focus node not rendered yet` | 技术性（测试桩与页面分页取数约定；无业务发明） | GEN-008、GEN-002、GEN-003 | 类型 4（探针转绿） | makeApi 的 getPaged 按同一路由表返回 `{items,total,page,size}` |
| CX-03 | TraceLinkP2c.test.tsx:64-76「我的合同：合同号→ADM-CONT-001、空间→ADM-GOV-007 均可点」 | 同上即红：两处 testid 不存在 | 细化（同 CX-01） | BSM-001、PGM-001、GEN-002、GEN-003、GEN-015、GEN-016 | 类型 4＋源码行 | 改为断言合同号与空间编号以纯文本显示、无 `trace-link-` 按钮、navigateTo 未被调用；改用例名 |
| CX-04 | TraceLinkP2c.test.tsx:78-90「商户授权工作台：账单商户列点击 → navigateTo(ADM-GOV-002, merchantId)」 | 同上即红：页面按 user.merchantId 过滤，且商户号已降纯文本 | 细化（同 CX-01） | BSM-001、PGM-001、GEN-002、GEN-003、GEN-005、GEN-015、GEN-016 | 类型 4＋源码行 | 测试用户补 `merchantId: "m-5"`；断言账单行显示商户号纯文本、无 `trace-link-ADM-GOV-002-m-5`；改用例名 |
| CX-05 | TraceLinkP2c.test.tsx:19 `getPaged` 恒空；L93-106「带 focusObjectId(merchantId) → 高亮命中的商户档案行」 | 同上即红：`organization-merchant-row-m-2` 找不到 | 技术性（同 CX-02） | GEN-008、GEN-002、GEN-003 | 类型 4（探针转绿） | 同 CX-02 |
| CX-06 | TraceLinkDrilldown.test.tsx:66-73「无 navigateTo 时降级纯文本（不可点）」查 `trace-link-ADM-CONT-001-c-77` | 页面现为 ADM-MER-006，否定断言落空；变异（TraceLink 无 navigateTo 也渲染按钮）下仍通过 | 技术性（测试有效性） | GEN-003 | 类型 4（变异） | 否定断言改查 `trace-link-ADM-MER-006-c-77` |

写入集合（拟）：只这两份测试文件。不改页面、TraceLink、AdminApp、adminRoutes、公共客户端与后端。

## 6. 只登记（不入本合成模块队列）

- OBS-1（→M099）：`MerchantZoneBillsPage.tsx:125/151`（ADM-MER-003，商户专页）来源合同链接指 ADM-CONT-001（需 `contract.draft.manage`），商户账号点了静默不跳，与 M023-005 同根；TraceLinkP2c.test.tsx:39-50 锁住该目标。M099 修页面时须同批改本文件该用例（写入集合须含 TraceLinkP2c.test.tsx），否则重演本项。
- OBS-2（→M003/M005）：`verify:predeploy`（package.json L96）与 `.github/workflows/predeploy-gates.yml` 不跑前端/后端单测，是本项与 XM-M036-A 基线红积累的根因。
- OBS-3：两份测试各复制一份 makeApi/baseProps，未抽公共夹具（不影响正确性）。

## 7. 结论

席甲 B1 票：**FAIL**（CX-01…06）。两文件全读、21 判据逐项、三线完成；探针与变异均在一次性 worktree 里做并已还原；落盘前未读 Grok 本次正文。


## XM-M038-A B2-r1 整批候选复核（席甲票，V0.02）

- 时间：2026-09-23T21:17:37Z。输入：两席 B1 完整 raw（席甲 V0.01＝上方本报告、Grok modules/XM-M038-A-b1-grok-raw.md V0.01）与候选 findings.md V3.91 顶部「当前 XM-M038-A B2-r1 整批候选」段（全文件 SHA256 84226279284993dca234031b07778883b4c7443738d6d4892af9eb37d0400e7d）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：CX-0n＝GR-0n（n=1…6）逐条同一事实，归并为 001…006；GR-07＝本席 OBS-1，归属更正为 M285（本席 B1 误写 M099）；两席其余观察登记；无漏并、无改写。
- 自审纠正：001/003/004 本席 B1 拟改写断言，读 Grok B1 后补跑变异（xm038a_mut2.log）证实三处现行行为已由 M023 本页测试守住，改取删除过期用例，比改写更小且不留重复断言。
- 分类：001/003/004 细化（PGM-001，M023-005 定稿）；002/005 技术性（测试桩与分页接口约定，无 HTML 句子可细化，无业务发明）；006 技术性（测试有效性）。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r1 票：**PASS**。
