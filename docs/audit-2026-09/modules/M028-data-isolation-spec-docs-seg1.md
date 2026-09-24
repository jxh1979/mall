# M028 租户范围与数据隔离（技术不变量）／文档与接口定义／第1段
版本：V0.01

2026-09-21 首次审计收口（操作员 2026-09-20 指令：严格按 V0.17，逐模块检查点）。2 个文件（数据隔离安全规格 md、域归属范围 CSV）、33 判据；开工前按 D-028 把两个生成物的主工作区副本对齐到 origin/main 已发布版本；B1 双 FAIL（Grok GR-01…11、席甲 CX-M028-001…005）→ B2-r1 双 PASS 定稿 M028-001…010（010 冲突，其余细化；GR-06 只登记）→ B3-r1（席甲：修前检查 16 处 → 修后 0 处；自动区块与 CSV 不动；带改后文件在基于 origin/main 的一次性 worktree 里两个生成器 --check 与条目锚点门禁均通过）→ B4-r1 双 PASS → B5-r1 双 PASS：Grok V0.01 SHA256 c8f1e03dab2b5d59bd32a78d71271cb89de9d6d853a368e37ee9412ea045f86c；席甲 V0.01 SHA256 1ef3b6eddd10b6f8741aa0fc4678bb8c3c1ed9b2d451a9909358f58ef1b81338；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。B3 累计 1。

修复摘要（规格文档 V0.02→V0.03，只动手写段）：001 §2 参考实现改为现行两维范围下推写法；002 两处行号引用改为不漂移的指称；003 对 V046/V049/V051 的表数与列宽改成迁移原值；004 存酒手机号 at-rest 加密缺口（ISO-GAP-06）改为已关闭并写明承接；005 §6A.1 读谓词拆为业务数据表（参数级、旧行 NULL 不可见）与配置默认槽（列级）两类，去掉「一律」与「语义等价」；006 §8 改述 CSV／§3A／§3 三者关系与新增表两步；007 不存在的证据 ID 改指现行代码注释；008 §9 两份清单标为 V140 初版、注明 cam 两表已随 V274 删除、现行以库内触发器为准；009 两处「治理 PRD §4」补文件名与节号；010（冲突）审计人员可见范围按治理地图「内部稽核可查看本综合体全部数据」改写。

跨模块观察（cross_module_observations）：生成器对 `business_process_step_records`、`venue_business_hours`、`venue_sessions` 的分类在 §3A 与 CSV 间不一致，§3A 出现 §1 未定义的类型值 `inherited-or-location-scope`（M029）；`platform-governance-prd-v0.1.md` §4.1 标题仍写「代码待落地」；文中路径两种基准并存（docs 相对与仓库根相对）；§5 存量回填与 V046 `chk_stock_adjustment_type` VALIDATE 仍待执行；主工作区 `currentMerchantScope()` 未发布暂存改动与 origin/main 不同、未发布 V427 使主工作区生成器检查必红（D-028 联动项）。
