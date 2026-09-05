# 退租结算落财务台账 — 历史建设进度台账（分支 feat/termination-settlement）

版本：V0.01

## 目标
合同退租变更（move_out/early_termination）终审通过 → 自动生成退租结算并落应收应付台账：
①未清应收清算（只读汇总不重挂）②押金处置（净额>0 挂应付、≤0 只列示）③提前退租违约金挂应收 ④结算快照留痕 ⑤幂等防重 ⑥前端展示+source_type筛选 ⑦契约门禁 ⑧测试。

## 方案取舍（零迁移）
- 触发点：`ContractService.approveChange` 的 COMPLETED 分支，changeType∈{move_out,early_termination} → 调 `settleTerminationIfNeeded`。
- 结算快照落点：**复用既有 `contract_settlements` 表**（settlement_items JSONB），零迁移。逐项 arApItemId 回写留痕。
- 落台账：新 `TerminationSettlementPort`（finance 实现，@Lazy 反向注入 ContractService，抄 turnoverRentPort 范式）→ persistDerivedArApItem 挂 payable(押金退还)/receivable(违约金)。
- 幂等：结算单 id = "cst_"+deterministicSuffix(changeId)；已存在即跳过；端口幂等键取结算单 id（派生台账项 id 确定，ON CONFLICT DO NOTHING）。
- source_type 新增 `termination_penalty` + `deposit_refund` 进 FinanceService.SOURCE_TYPES + 中文标签（DB 层 ar_ap_items.source_type 无 CHECK，服务层枚举即可，抄 V110/V299 同款，**零迁移**）。
- 无新端点（结算是 approveChange 副作用；前端读既有 listSettlements）；无新迁移（V303 未用）。

## 已改/新增文件
- 新 `backend/.../contract/TerminationSettlementMath.java` — 纯算法（抵扣/净额/违约金/清单）。
- 新 `backend/.../finance/TerminationSettlementPort.java` — 端口接口。
- 新 `backend/.../contract/TerminationSettlementMathTests.java` — 纯单测 7。
- 改 `backend/.../finance/FinanceService.java` — implements 加端口 + SOURCE_TYPES+标签 + 2 端口方法。
- 改 `backend/.../contract/ContractService.java` — 加 terminationSettlementPort 字段 + approveChange 钩子 + settleTerminationIfNeeded/annotateSettlementItem。
- 改 `backend/.../contract/ContractServiceTests.java` — +7 service 测试 + Recording 端口 + unpaidAr 字段。
- 改 `backend/.../contract/ContractJdbcIntegrationTests.java` — +结算快照往返（write-only，不跑库）。
- 改前端 `apps/web/src/admin/pages/ContractWorkbenchPage.tsx` — 结算明细渲染清单/押金抵扣/净额/违约金。
- 改前端 `apps/web/src/admin/pages/FinanceArApLedgerPage.tsx` — 来源类型标签+详情。
- 改 `apps/web/src/admin/types.ts` — FinanceLedgerRow.sourceType + SettlementRow.settlementItems。
- 改后端 `JdbcFinanceRepository.java` listArApItemsPaged 两重载 keyword 加 source_type ILIKE（退租相关可筛）。

## 门禁（无迁移/无端点）
operation-matrix→closure→security-baseline→latest.json→清 surefire→entry 基线；verify:predeploy。
无迁移故跳 domain-ownership-scope/data-isolation-spec。

## 当时状态
以下为该分支当时的建设记录，测试数字不代表当前工作区的测试结果。
完成。后端(单测32绿)+前端(vitest15绿+typecheck绿)+verify:predeploy全链绿。已 commit+push。
