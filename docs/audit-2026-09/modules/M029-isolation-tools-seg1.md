# M029 租户范围与数据隔离（技术不变量）／检查及生成工具／第1段
版本：V0.01

2026-09-21 首次审计收口（操作员 2026-09-20 指令：严格按 V0.17，逐模块检查点）。4 个工具文件、18 判据；B1 Grok FAIL（GR-01、GR-02）／席甲 PASS 经复核撤回 → B2-r1 双 PASS 定稿 M029-001、002（均细化）→ B3-r1（席甲：原工具 RED 3 处 → 修后 GREEN；重生成后两个 --check、隔离机审、测试库门禁与全链 verify:predeploy 全绿；均在基于 origin/main 的一次性 worktree 里做）→ B4-r1 双 PASS → B5-r1 双 PASS：Grok V0.01 SHA256 0972ddbb63d1c76977d44a64546216a6d1adf9d13d78cba60374d2a8ac546c8c；席甲 V0.01 SHA256 fee1c69c38e146bf4d860b4182ae175c3763cae4e7aa6b8dc594fffe1e52e26c；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。B3 累计 1。

修复摘要：001 规格生成器不再发射规格 §1 类型闭集之外的 `inherited-or-location-scope`（自动区块只收自带 scope 列的表，恒为 direct，与 CSV 同口径）；CSV 生成器补 `business_process_step_records` 的 inherited 规则（经 process_instance_id 继承）。机械后果：规格自动区块 2 个单元格、CSV 1 行随批重生成（属 M028 文件，只变生成器拥有的区域）。002 隔离机审在抽表名前中和 `IS [NOT] DISTINCT FROM` 运算符，不再把其后的别名当成未登记表。

跨模块观察（cross_module_observations）：`security-scans/tenant-isolation-baseline.json` 的 `F2:UNREGISTERED_TABLE:d` 键修后变为陈旧项，另有陈旧键 access_grant_requests（M030）；CSV 生成器写死 status=covered 致 uncovered 检查不可达；测试库门禁根目录缺失时静默 PASS；隔离机审扫描面外有 17 个含内联 SQL 的 Java 文件（头注 B4 已登记）；`verify:test-db-isolation` 不在 delivery-gates（package.json）；四个工具无自带单测。
