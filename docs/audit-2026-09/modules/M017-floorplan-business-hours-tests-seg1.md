# M017 空间点位、楼层平面图与营业时段／后台验证／第1段
版本：V0.01

状态：PASS。范围：plan/modules.md V0.04 的 5 文件（FloorPlanControllerTests、FloorPlanImportParserTests、FloorPlanJdbcIntegrationTests、FloorPlanServiceTests、VenueBusinessHoursServiceTests；计划冻结 1352 行，当前含 M015/M016/M017 B3 追加）；判据 90 项（BEQR 22、BSM 14、GEN 18、PGM 36）。B3 整批修复累计 1 次。修复队列10 个 ID 全否（8 项已修：001–008；009/010 观察）；添加项 0；questions 0；无 HTML 修改申请。

- B1：Grok `modules/M017-b1-grok-raw.md` V0.01；席甲 `modules/M017-b1-self-raw.md` V0.01（均 FAIL）。
- B2：候选与双 PASS 定稿见 findings V1.100（Grok raw 追加段、席甲 raw 追加段）。
- B3-r1：席甲只落 5 个测试文件（实现零改动）：解析器 10 个错误码负例与中文文案、发布不覆盖既有图形/失败任务/候选归属/上限/详情审计、诚实分页与 Controller 桩、逐端点 403 与缺幂等键、corridor 负例与 candidates CHECK、真实 SQL（projectExists、@Nested 真实 spaces 查询）、营业时段真实审计与断言加严；主工作区 5 类全绿（TEST-*.xml 计数）；一次性副本 6 个实现变异体 6/6 被对应新例击红。
- B4-r1：`modules/M017-b4-r1-grok-raw.md` / `modules/M017-b4-r1-self-raw.md` 双 PASS（内容与 §5 闸 3 归因）。
- B5-r1：`modules/M017-b5-r1-grok-raw.md`（33467 bytes SHA 95db6f0d412b283157a8f7a920d0be1d82ba106a6c41366e031e54b1924719a4）/ `modules/M017-b5-r1-self-raw.md`（15875 bytes SHA 2b41ee248de935779ee3bb55406c05f2e025fb063ee29183406b86afd6ddd361）双 PASS；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。
- 台账：ledger V3.49 先记 PASS，再写本文件。
- 跨模块观察（cross_module_observations）：见 findings V1.100 定稿节「否」项 009（requireText 英文键＝FieldLabels 设计口径）、010（导入上限用例登记、审计 *_failed 事件登记、M015-010/011 XM 不在测试面加例）。
