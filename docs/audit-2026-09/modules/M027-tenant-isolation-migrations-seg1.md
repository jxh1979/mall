# M027 租户范围与数据隔离（技术不变量）／数据库迁移／第1段
版本：V0.01

2026-09-21 首次审计收口（操作员 2026-09-20 指令：严格按 V0.17，逐模块检查点）。13 个已应用迁移（V044、V045、V046、V047、V049、V051、V052、V065、V174、V214、V237、V243、V244）、18 判据（GEN-001…018）；B1 双 PASS（0 finding）→ B2-r1 双 PASS 定稿（修复队列为空）→ 按 V0.17 跳过 B3/B4 → B5-r1 双 PASS：Grok V0.01 SHA256 88d453afde6afc7e7f221b358db54c90a9d75143987fdba6aa8b6a724b340914；席甲 V0.01 SHA256 9c7fe67281178efaa8517aad5c9cb0020f23376c53aff00f14091075b6c4465a；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。B3 累计 0；未改任何业务文件。

核实摘要：13 个版本在 complex_test 全部 success；所建隔离列、部分索引、3 个约束、2 个表达式唯一索引现态齐全；唯一键列均 NOT NULL；隔离列由服务端身份写入（抽核财务、证据、库存三域）；现行 upsert 冲突目标与 V174 表达式索引逐字匹配；V214 回填口径与现行仓储写入口径一致；读路径对项目级调用方 fail-closed（project_id 为 NULL 的历史行只在平台口径可见）。

跨模块观察（cross_module_observations）：`chk_stock_adjustment_type`（V046）以 NOT VALID 建立后从未 VALIDATE，对新写入已生效；补 VALIDATE 的前向迁移在生产存量含闭集外旧值时会使部署失败，属发布决策，已报操作员。merchant_id 宽度三档（64/80/100）；staff_base_salaries.project_id 无索引；`upsertBaseSalary` 冲突时不更新 project_id（access/staff 模块）；部分调用方对 `currentMerchantScope()` 仍写 `.orElse(null)`（各调用方模块）；隔离靠列与应用层谓词、无行级安全策略；V243 回填对生产存量的实际命中无法在只读约束下验证。
