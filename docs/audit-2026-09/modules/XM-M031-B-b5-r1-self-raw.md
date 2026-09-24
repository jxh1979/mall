CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# XM-M031-B B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Opus 5.5（作者自审票；B1 沿用登记计数与 B2-r1 回归宿主选错两处已更正）。2026-09-24T06:11:46Z
- 锁 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（V0.14；实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。主工作区范围文件现状＝origin/main 4a066def＋冻结 diff /home/james/audit-tmp/xm031b_b3r1.diff。
- 范围：开工段 6 个文件现状＋B3 改到的 TraceabilityJdbcIntegrationTests；判据 M092、M095 映射并集 46 项。B3 累计 1。
- 顺序：本席取证先跑完，随后落本报告；落盘前未读 Grok 正文。未改任何已跟踪文件；一次性 worktree 与一次性空库已删。

## 1. 文件覆盖

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/java/com/complex/platform/evidence/AuditEventService.java | 136 | 5304 | 580300fdce8703509ef944fc6731cedd3f9d484d63397d94af39bddcadafe13a | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/evidence/AuditEventRepository.java | 18 | 694 | addf2088dfe30ac292f9d7df49267d217c67fefaeec54686b30024fb4022ee57 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/evidence/JdbcAuditEventRepository.java | 111 | 6329 | 76d25db10b61c9270b5d2ca7f7c968fcd8d29f84f851fa1c7ff4665d596d28cb | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/evidence/AuditedOperationExecutor.java | 188 | 7319 | 211ce3bf58d3290ef8a969277e67d3fbacb1d0509dcdaac00189d6f17d9ad210 | 改动处及上下文全读 |
| backend/src/main/java/com/complex/platform/evidence/AuditEvent.java | 53 | 1727 | 3d533833303997d9deafa7702ebde874dfc4b7c5ce32690672df66b4c9180685 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/evidence/AuditEventServiceTests.java | 58 | 2297 | 07c4de0155031517573d3bdca213a748bc06e873ec2d17d8dcc085c73c73f387 | 改动处及上下文全读 |
| backend/src/test/java/com/complex/platform/evidence/TraceabilityJdbcIntegrationTests.java | 202 | 11196 | cbe3d5ef5409dce871ba87466e43674959294a43537218558a4a647b6c7418d5 | 改动处及上下文全读 |

## 2. 可执行证据

后端全量（/home/james/audit-tmp/xm031b_b5_run.log）：

```
START=2026-09-24T06:09:49Z HEAD=e456ba96 origin/main=4a066def
scratch at 4a066def
冻结 diff 已应用
match AuditEventService.java
match TraceabilityJdbcIntegrationTests.java
scratch db xm031bb5_test created
MVN_EXIT=0
suites=263 tests=3539 red=0
全部红（预期 0）：
相对基线 origin/main 4a066def（0）转绿：0
scratch db dropped
scratch dirty after cleanup: 0
scratch removed
three HTML + lock sha:
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458 
END=2026-09-24T06:10:41Z
```

修复后重跑 B1 两个探针（/home/james/audit-tmp/xm031b_probe_post.log；修复前见 xm031b_probe.log）：

```
START=2026-09-24T06:10:49Z scratch at 4a066def DB=xm031bp_test PATCH=/home/james/audit-tmp/xm031b_patch.py
patch applied:  2 files changed, 75 insertions(+)
scratch db xm031bp_test created
MVN_EXIT=0
XM031BPROBE aborted_tx original=DataIntegrityViolationException recordFailure=ok propagated=DataIntegrityViolationException failure_rows=1
XM031BPROBE plain_failure_rows=1 transactional_failure_rows=1
scratch db dropped
scratch dirty after cleanup: 0
scratch removed
END=2026-09-24T06:11:08Z
```

## 3. 三线审计

- 第一性原理：要做到失败与拒绝事后查得到，且记失败本身不得顶掉原错误。实际：带事务写方法失败事件由 0 行变 1 行；事务被 SQL 错误中止时 recordFailure 由抛 UncategorizedSQLException 变为正常写入、原 DataIntegrityViolationException 原样抛出；成功事件仍随业务回滚。未验证前提：无。
- 顺从式：46 项逐项见 §4。
- 对抗式（grilling）：问：注解会不会因类内自调用失效？答：类内无对 recordFailure 的自调用，生产调用都来自别的 bean。问：测试里 119 处直接 new 的服务会不会行为变化？答：不经代理，注解不生效，行为同前，内存单测全绿。问：独立事务会不会被外层锁住？答：audit_events 无外键、主键随机；类级事务里 TRUNCATE audit_events 的两条用例只直接调仓储，不经 recordFailure；全量 263 套无挂起。问：失败行提交后会不会碰到别的测试的精确计数？答：GREEN 全量 0 红。问：成功事件有没有被一并独立？答：没有，回归③修复前后都 0 行。问：有无新引入问题？答：未见。
- 无新缺陷、无添加项、无 HTML 问题。

## 4. 46 判据逐项

| 判据 | 出处 | 适用｜证据｜结论 |
|---|---|---|
| BEQR-063 高危动作（需审批/双证据） | business-enum-quick-reference-v0.1.html:111 | 适用｜高危动作失败尝试留痕｜通过 |
| BEQR-138 风险四级 | business-enum-quick-reference-v0.1.html:217 | 无本面｜未触及｜通过 |
| BEQR-139 事件状态 | business-enum-quick-reference-v0.1.html:218 | 无本面｜未触及｜通过 |
| BEQR-140 主管复核强制项 | business-enum-quick-reference-v0.1.html:219 | 无本面｜本合成模块范围（7 个文件现状）不触及「主管复核强制项」所述对象｜通过 |
| BEQR-141 经营异常处置状态 | business-enum-quick-reference-v0.1.html:220 | 无本面｜本合成模块范围（7 个文件现状）不触及「经营异常处置状态」所述对象｜通过 |
| BEQR-142 异常状态的统一出口 | business-enum-quick-reference-v0.1.html:221 | 无本面｜本合成模块范围（7 个文件现状）不触及「异常状态的统一出口」所述对象｜通过 |
| BSM-001 整体说明：这套系统到底管什么 | business-system-map-for-operations-v0.1.html:505 | 无本面｜本合成模块范围（7 个文件现状）不触及「整体说明：这套系统到底管什么」所述对象｜通过 |
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | business-system-map-for-operations-v0.1.html:563 | 无本面｜未触及｜通过 |
| BSM-022 留痕与异常 | business-system-map-for-operations-v0.1.html:666 | 适用｜留痕与异常：失败动作留痕留得住｜通过 |
| BSM-028 主要功能 | business-system-map-for-operations-v0.1.html:698 | 无本面｜本合成模块范围（7 个文件现状）不触及「主要功能」所述对象｜通过 |
| BSM-093 留痕与异常：重要动作留得住，出了问题追得回 | business-system-map-for-operations-v0.1.html:1574 | 适用｜第 1574 行「重要动作留得住」｜通过 |
| BSM-094 必须关注的动作 | business-system-map-for-operations-v0.1.html:1578 | 适用｜第 1583 行「核销成功、失败、拒绝、撤销」：带事务路径的失败与拒绝留痕留得住｜通过 |
| BSM-095 怎样减少人工填写 | business-system-map-for-operations-v0.1.html:1596 | 无本面｜本合成模块范围（7 个文件现状）不触及「怎样减少人工填写」所述对象｜通过 |
| BSM-096 处理结果 | business-system-map-for-operations-v0.1.html:1607 | 无本面｜本合成模块范围（7 个文件现状）不触及「处理结果」所述对象｜通过 |
| BSM-097 风险等级与风控规则 | business-system-map-for-operations-v0.1.html:1619 | 无本面｜本合成模块范围（7 个文件现状）不触及「风险等级与风控规则」所述对象｜通过 |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | business-system-map-for-operations-v0.1.html:1827 | 适用｜第 1845 行「一律拒绝并留痕」：带事务路径的拒绝留痕留得住｜通过 |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | business-system-map-for-operations-v0.1.html:1897 | 无本面｜本合成模块范围（7 个文件现状）不触及「相互关系：为什么这些子系统必须连在一起」所述对象｜通过 |
| BSM-120 已确认规则和实施前输入项 | business-system-map-for-operations-v0.1.html:1957 | 适用｜第 1995 行同句留痕要求｜通过 |
| GEN-001 必要性与最小性 | 通用 | 适用｜生产只加一处传播注解；回归只加三条｜通过 |
| GEN-002 可执行正确性 | 通用 | 适用｜origin/main＋冻结 diff 后端全量 263 套 3539 测 0 红；修复后探针：带事务写方法失败事件 1 行、事务中止时 recordFailure=ok 且失败事件 1 行、原异常原样（xm031b_probe_post.log）｜通过 |
| GEN-003 测试有效性 | 通用 | 适用｜两条回归在旧实现下红（xm031b_b3_red.msgs），一条守住成功路径不被加宽｜通过 |
| GEN-004 信任边界校验 | 通用 | 无本面｜不涉信任边界输入｜通过 |
| GEN-005 授权与最小权限 | 通用 | 无本面｜不改授权判定｜通过 |
| GEN-006 数据完整性 | 通用 | 适用｜成功事件仍随业务回滚（回归③）；失败事件独立存续｜通过 |
| GEN-007 并发与幂等 | 通用 | 适用｜audit_events 无外键、主键随机，独立事务不等外层锁；全量无挂起｜通过 |
| GEN-008 接口与模式一致性 | 通用 | 适用｜构造签名未变，119 处 new 构造不受影响｜通过 |
| GEN-009 错误处理与恢复 | 通用 | 适用｜事务中止后原异常不再被顶掉（回归②）｜通过 |
| GEN-010 安全与敏感信息 | 通用 | 无本面｜不涉敏感信息｜通过 |
| GEN-011 可观测与审计证据 | 通用 | 适用｜带事务写方法的失败与拒绝事件留得住（回归①）｜通过 |
| GEN-012 资源与性能边界 | 通用 | 适用｜每次失败多借一连接片刻，已登记观察｜通过 |
| GEN-013 构建配置与依赖 | 通用 | 无本面｜无新依赖｜通过 |
| GEN-014 数据库迁移正确性 | 通用 | 无本面｜无迁移｜通过 |
| GEN-015 界面可用性与可访问性 | 通用 | 无本面｜不涉界面｜通过 |
| GEN-016 文档可验证性 | 通用 | 适用｜recordFailure 注释与行为一致（独立事务、成功事件随调用方事务）｜通过 |
| GEN-017 业务来源分类 | 通用 | 适用｜001 细化，B2 双席定稿｜通过 |
| GEN-018 第一性原理证据 | 通用 | 适用｜结论落到 HTML 原句、源码行、探针与全量输出｜通过 |
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | platform-governance-map-for-operations-v0.1.html:256 | 适用｜见 PGM-061/062｜通过 |
| PGM-014 留痕复核 | platform-governance-map-for-operations-v0.1.html:393 | 适用｜带事务写动作的失败与拒绝可追溯｜通过 |
| PGM-023 主要功能 | platform-governance-map-for-operations-v0.1.html:467 | 无本面｜本合成模块范围（7 个文件现状）不触及「主要功能」所述对象｜通过 |
| PGM-026 主要功能 | platform-governance-map-for-operations-v0.1.html:492 | 无本面｜本合成模块范围（7 个文件现状）不触及「主要功能」所述对象｜通过 |
| PGM-037 主要功能 | platform-governance-map-for-operations-v0.1.html:615 | 无本面｜本合成模块范围（7 个文件现状）不触及「主要功能」所述对象｜通过 |
| PGM-061 留痕和复核：重要动作不能只靠口头说明 | platform-governance-map-for-operations-v0.1.html:892 | 适用｜第 893 行「结果是什么，都要能查到」：失败结果查得到｜通过 |
| PGM-062 必须留痕的动作 | platform-governance-map-for-operations-v0.1.html:896 | 适用｜第 899、902 行权限变更与被拒绝访问的失败留痕留得住｜通过 |
| PGM-063 常见复核 | platform-governance-map-for-operations-v0.1.html:911 | 适用｜复核有据｜通过 |
| PGM-064 正式挂起订单审计与结案 | platform-governance-map-for-operations-v0.1.html:923 | 无本面｜本合成模块范围（7 个文件现状）不触及「正式挂起订单审计与结案」所述对象｜通过 |
| PGM-066 已拍板规则和待排期问题 | platform-governance-map-for-operations-v0.1.html:951 | 适用｜第 960 行「所有有过程的业务动作都必须留痕」：失败路径留痕留得住｜通过 |

共 46 项；逐项给出适用与结论的 33 项，其余 13 项无本面。

## 5. 已定稿 001 现态

已修（B3-r1），B4-r1 双 PASS；修复队列「是」＝0。

## 6. 本次 Findings

无新增。登记不变：IdempotencyService 注释与带事务调用方（→M084）、独立事务借第二连接的池耗尽、终端字面量 web、errorCode 为空、读路径坏 JSON。

## 7. 结论

席甲 B5-r1 票：**PASS**。
