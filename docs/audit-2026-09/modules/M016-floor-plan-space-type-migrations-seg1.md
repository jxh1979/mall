# M016 空间点位、楼层平面图与营业时段／数据库迁移／第1段
版本：V0.03

2026-09-20 B5-r1 再执行双 PASS（操作员裁定补派 Grok 全量重审；同一 B3 次数 1 下两席向同名 b5 raw 追加完整报告）：Grok V0.02 追加段 SHA256（全文）41a2f0fcf1c15ce94b673038b4285617af01ee7d9d95d0ef21c9a3e7eb5381d8；席甲 V0.03 追加段 SHA256（全文）3d166ef798bd28dd2a1d28c73be1b79f02509bdeab3305109fa1ffaf06149907；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。历史 finding 全否不变；无新问题/人闸。

---
以下历史 V0.02 原文原样保留：

# M016 空间点位、楼层平面图与营业时段／数据库迁移／第1段
版本：V0.02

2026-09-20 席甲补充复查PASS；Grok NOT-CALLED。本次按操作员「从 M009 重新独立审计，有问题按 V0.17 完整双席流程」执行，无问题只记录，不是新双 PASS；历史 Claude Fable 5.1/Grok B5-r1 双 PASS 保留。B3 累计 1，本次修复 0。

当前 4 冻结迁移 + V423 全文与 18 判据三线核对完成，五文件 SHA 与 B5-r1 逐一相等（零改写）。同表后续迁移 M018 V424–V426（字典 status 闭集扩为四值、历史 disabled→archived）与 M009 在途 V427 只读追流：V425 取代 V423 两值 CHECK 属 M018 授权变更，非本模块缺陷；V184/V423「active/disabled」注释陈旧登记为观察。活库约束实查 001/002 原样；6 类 106 例（XML 口径）主工作区 `complex_test`（Flyway 427）实跑零失败/错误/跳过。

[本次完整报告](M016-b5-r1-self-raw.md) V0.02：50012 bytes，SHA256 c702e7e91420f405f5e913ee784610004f118810cf99fb60cb269d8c5df624be；旧 16411 byte 前缀完整保留。旧 Grok V0.01 SHA256 b5add5923387475c2fc89971bd69b347f70286eddc6379e896cb81d0ed9279cd 保持。历史 4 项全否（3 已修、004 观察）不改；没有新问题/人闸。

锁实算/sidecar/ledger＝b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a，LOCK-AMEND-COUNT 1；HEAD e456ba96。三 HTML/源码/迁移/测试/锁未改、未发布；先写 ledger V4.55 保留 PASS，后写本文件。

---
以下历史 V0.01 原文原样保留：

# M016 空间点位、楼层平面图与营业时段／数据库迁移／第1段
版本：V0.01

状态：PASS。范围：plan/modules.md V0.04 的 4 文件（V023__p1_floor_plan_polygons.sql、V034__p2_floor_plan_import_tasks.sql、V184__space_type_dictionary.sql、V203__spaces_status_check.sql；计划冻结 164 行）；判据 18 项（GEN-001…018）。B3 整批修复累计 1 次。修复队列4 个 ID 全否（3 项已修：001–003；004 观察）；添加项 0；questions 0；无 HTML 修改申请。

- B1：Grok `modules/M016-b1-grok-raw.md` V0.01；席甲 `modules/M016-b1-self-raw.md` V0.01（均 FAIL：GR-M016-001…003 / CX-M016-001…003 同集）。
- B2：候选与双 PASS 定稿见 findings V1.97（Grok raw 追加段、席甲 raw 追加段）。
- B3-r1：席甲在主工作区 TDD 修复：新增 `V423__m016_floor_plan_and_space_type_checks.sql`（已应用迁移零改写；一次性 m016_scratch 库干跑 RED 4 → GREEN 50/50，主工作区 RED 4 例红 → GREEN 6 类 75 例全绿，complex_test 升到 V423）；同批 M015 `FloorPlanService.SHAPE_TYPES`、M017 三个测试类先红用例、门禁产物再生成。
- B4-r1：`modules/M016-b4-r1-grok-raw.md` / `modules/M016-b4-r1-self-raw.md` 双 PASS（内容与 §5 闸 3 归因）。
- B5-r1：`modules/M016-b5-r1-grok-raw.md`（37973 bytes SHA b5add5923387475c2fc89971bd69b347f70286eddc6379e896cb81d0ed9279cd）/ `modules/M016-b5-r1-self-raw.md`（16411 bytes SHA 6ef6ad5d81260d5c083b1b5cc7f7b69670b6f9529d7a568af16df6a435cb34d7）双 PASS；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。
- 台账：ledger V3.43 先记 PASS，再写本文件。
- 跨模块观察（cross_module_observations）：见 findings V1.97 定稿节「否」项 004（前端 corridor 标签 / sourceFormat 默认 geojson → M018；spaces 用途/负责人补列 → 全局遗漏 M015-012；V052 merchant_id 可空无 FK → 非本模块）。
