CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M038-A B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Opus 5.5（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-23T21:42:39Z
- 锁 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（现行 V0.14；实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区两文件即发布版；origin/main bfb17f8b。
- 合成模块 XM-M038-A 两个测试文件（§1 全读），锁内 M081/M082 映射 21 判据逐项；B3 累计 1；findings V3.93：001…006 全否（已修）。
- 顺序：本席取证脚本先跑完，随后落本报告；落盘前未读 Grok 正文（modules/XM-M038-A-b5-r1-grok-raw.md）。只读承诺：未改任何已跟踪文件；一次性 worktree 已删除。

## 1. 文件覆盖

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| apps/web/src/admin/pages/TraceLinkDrilldown.test.tsx | 126 | 6417 | 6e56106913f9b78308b6e9a8c8bb015fc8b287fecae2157d109e10966a8041ae | 全读，5 条用例 |
| apps/web/src/admin/pages/TraceLinkP2c.test.tsx | 84 | 3724 | 41b7e16467a0c97503e250df19186903420019909c404759be7de107da16f235 | 全读，3 条用例 |

逐条用例（改后）：

| 文件:行 | 用例 | 与页面现行已定稿行为一致 | 能否失败 |
|---|---|---|---|
| Drilldown:45-58 | 合同工作台商户列 → ADM-GOV-002 | 是（ContractWorkbenchPage 为后台页，目标可达） | 是（改 pageId 或删链接即红） |
| Drilldown:60-68 | 无 navigateTo 时降级纯文本 | 是 | 是（B3 变异：无 navigateTo 也出按钮即红） |
| Drilldown:72-88 | 合同工作台 focus 高亮且非焦点行不高亮 | 是 | 是 |
| Drilldown:90-106 | 审计事件页反查 business_type/business_id | 是 | 是 |
| Drilldown:108-125 | 治理拓扑 focus 高亮（分页桩） | 是（M012-013 分页） | 是（不渲染节点即超时、无高亮类即红） |
| P2c:42-53 | 我的账单来源合同 → ADM-CONT-001 | 页面现状如此；该目标对商户受众不可达，已登记 GR-07→M285（B2 双席定为只登记） | 是 |
| P2c:55-65 | 我的账单非合同来源不渲染合同链接 | 是 | 是（去掉 sourceType 判断即渲染该 testid） |
| P2c:70-83 | 组织商户档案 focus 高亮、非焦点不高亮（分页桩） | 是 | 是 |

## 2. 可执行证据原文（/home/james/audit-tmp/xm038a_b5_run.log）

```
START=2026-09-23T21:41:13Z HEAD=e456ba96 origin/main=bfb17f8b
TraceLinkDrilldown.test.tsx 126 6417 6e56106913f9b78308b6e9a8c8bb015fc8b287fecae2157d109e10966a8041ae
TraceLinkP2c.test.tsx 84 3724 41b7e16467a0c97503e250df19186903420019909c404759be7de107da16f235
scratch at bfb17f8b
冻结 diff 已应用
match TraceLinkDrilldown.test.tsx
match TraceLinkP2c.test.tsx
VITEST_EXIT(module+m023)=0
 ✓ src/admin/pages/MerchantZoneContractsPage.test.tsx (8 tests) 114ms
 ✓ src/admin/pages/MerchantScopePage.test.tsx (14 tests) 163ms
 ✓ src/admin/pages/MerchantZoneSettlementsPage.test.tsx (10 tests) 178ms
 ✓ src/admin/pages/TraceLinkP2c.test.tsx (3 tests) 199ms
 ✓ src/admin/pages/TraceLinkDrilldown.test.tsx (5 tests) 281ms
 Test Files  5 passed (5)
      Tests  40 passed (40)
VITEST_EXIT(full)=0
 Test Files  206 passed (206)
      Tests  978 passed (978)
TSC_EXIT=0
scratch dirty after cleanup: 0
scratch removed
three HTML + lock sha:
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458 
END=2026-09-23T21:41:40Z
```

## 3. 三线审计

- 第一性原理：两文件守「全链可点击」的源页链接与目标页定位；删除的三条所守行为现由 M023 本页测试守住（本轮连同两文件一起实跑 40 测全绿；B2 变异证据 xm038a_mut2.log）；分页桩形状 `{items,total,page,size}` 与页面 `useServerPagedRows` 所读字段一致。
- 顺从式：21 判据逐项见 §4。
- 对抗式（grilling）：Q 删了三条，TraceLink 文件还剩什么意义？A 仍有合同工作台源页链接、无 navigateTo 降级、三条目标页定位、审计反查、账单来源合同两条，覆盖 P1-A/P2-C 的其余面。Q 分页桩 `size: 500` 会不会掩盖分页缺陷？A 两页调用方就用 500 作页大小，桩按调用方返回一页，不伪造多页；分页本身由 M012 本页测试守。Q P2c 第 66 行多一空行？A 删用例后的排版残留，不影响行为，不立项。Q 有无新引入问题？A 无：只改两测试文件、全量 978 绿、类型检查通过。无新缺陷、无添加项、无 HTML 问题。

## 4. 21 判据逐项

| 判据 | 适用｜证据｜结论 |
|---|---|
| BSM-001 整体说明 | 适用｜留下的断言与已定稿页面行为一致（§1 表）；我的账单目标问题已登记 M285｜通过 |
| BSM-003 通用能力 | 适用｜不涉增删改与引导｜通过 |
| GEN-001 必要性与最小性 | 适用｜删除与 M023 本页测试重复的三条；夹具重复只登记｜通过 |
| GEN-002 可执行正确性 | 适用｜两文件 8 测、全量 978 测全绿（§2）｜通过 |
| GEN-003 测试有效性 | 适用｜每条用例均能失败（§1 表；006 有变异证据）｜通过 |
| GEN-004 信任边界校验 | 无本面｜通过 |
| GEN-005 授权与最小权限 | 适用｜不再有无商户归属夹具去断言商户页的用例；商户受众可达由 M023 本页测试守｜通过 |
| GEN-006 数据完整性 | 无本面｜通过 |
| GEN-007 并发与幂等 | 无本面｜通过 |
| GEN-008 接口与模式一致性 | 适用｜桩的 get/getPaged 与页面取数方式一致（002/005）｜通过 |
| GEN-009 错误处理与恢复 | 无本面｜通过 |
| GEN-010 安全与敏感信息 | 适用｜桩数据无敏感信息｜通过 |
| GEN-011 可观测与审计证据 | 适用｜审计反查用例保留且能失败｜通过 |
| GEN-012 资源与性能边界 | 无本面｜通过 |
| GEN-013 构建配置与依赖 | 适用｜无新依赖；类型检查通过；预部署不跑单测已登记 M003/M005｜通过 |
| GEN-014 数据库迁移正确性 | 无本面｜通过 |
| GEN-015 界面可用性与可访问性 | 适用｜本范围不再断言商户点了没反应的链接（我的账单一条已登记 M285）｜通过 |
| GEN-016 文档可验证性 | 适用｜用例名与断言一致；两处注释说明改动缘由且与现状相符｜通过 |
| GEN-017 业务来源分类 | 适用｜001/003/004 细化、002/005/006 技术性，B2 双席定稿｜通过 |
| GEN-018 第一性原理证据 | 适用｜结论落到源码行与命令输出｜通过 |
| PGM-001 整体定位 | 适用｜全链可点击的受众可达口径与 M023-005 一致｜通过 |

## 5. 已定稿 001…006 现态

全部已修（B3-r1），B4-r1 双 PASS；修复队列「是」＝0。

## 6. 本次 Findings

无新增。只登记项不变：GR-07（→M285）、预部署不跑单测（→M003/M005）、夹具重复、测试不经 AdminApp.navigateTo。

## 7. 结论

席甲 B5-r1 票：**PASS**。两文件全读、21 判据逐项、取证脚本实跑复核；未改任何已跟踪文件；落盘前未读 Grok 本次正文。
