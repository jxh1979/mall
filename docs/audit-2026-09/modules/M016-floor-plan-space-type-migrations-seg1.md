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
