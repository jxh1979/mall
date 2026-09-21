# M030 租户范围与数据隔离（技术不变量）／质量检查记录／第1段
版本：V0.01

2026-09-21 首次审计收口（操作员 2026-09-20 指令：严格按 V0.17，逐模块检查点）。1 个文件 security-scans/tenant-isolation-baseline.json、18 判据；B1 双 FAIL（Grok GR-01 与席甲 CX-M030-001 为同一条）→ B2-r1 双 PASS 定稿 M030-001（细化）→ B3-r1（席甲：删两条陈旧键；一次性 worktree 里修前陈旧提示 2 条 → 修后 0 条，默认路径、verify:security-baseline 与全链 predeploy 全绿）→ B4-r1 双 PASS → B5-r1 双 PASS：Grok V0.01 SHA256 95c42b9fa75eb6e76143489f52f17b3e6ac7ef7ad787097c427bf2cac473656c；席甲 V0.01 SHA256 045400716537b0917ea90688446073e6bc1427e481af85c0f36030e088b54d5f；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。B3 累计 1。

修复摘要：001 从隔离机审棘轮基线里删去两条扫描器已不再复现的键——`F1:UNSCOPED_DIRECT_READ:access_grant_requests`（仓储已按项目过滤）与 `F2:UNREGISTERED_TABLE:d`（M029-002 起扫描器不再把 `IS NOT DISTINCT FROM` 后的别名当表）；清单现 2 条＝扫描器现行 finding 2 条，这两个已收紧的点重新受棘轮保护。未跑 --write-baseline；同批带入 2026-09-11 OPEN PR #446 两个本地提交的内容（四条 text 与 note 的中性化改写）。

跨模块观察（cross_module_observations）：`tools/generate-formal-security-baseline.mjs:304-310` 与其生成的 `security-scans/findings-register-v1.md:27` 仍写「access_grant_requests 保留 baseline…收紧至 1 条…可移除最后 1 条」，与基线现状不符；`verify-tenant-isolation.mjs --write-baseline` 内置的旧 note 要求的 SEC-*-TENANT-ISOLATION-* 一一对应在现行台账里不存在；两条活 F1（integration_channel_store_map、revenue_stream_attribution）为台账跟踪中的已知缺口。
