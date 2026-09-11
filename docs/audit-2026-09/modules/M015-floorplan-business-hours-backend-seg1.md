# M015 空间点位、楼层平面图与营业时段／后台实现／第1段
版本：V0.01

状态：PASS。范围：plan/modules.md V0.04 的 23 文件（floorplan 包 17 + governance 包 ProjectRecord/SpaceRecord/SpaceTypeRecord/VenueBusinessHoursRecord/Repository/Service；计划冻结 1999 行）；判据 90 项（BEQR 22、BSM 14、GEN 18、PGM 36；席甲 B1 曾写 89，B2 勘误）。B3 整批修复累计 2 次。修复队列14 个 ID 全否（9 项已修；010/011 XM 同批、012 全局遗漏、013 XM→M016/M018、014 观察）；添加项 0；questions 0；无 HTML 修改申请。

- B1：Grok `modules/M015-b1-grok-raw.md` V0.01；席甲 `modules/M015-b1-self-raw.md` V0.01（均 FAIL）。
- B2：候选与双 PASS 定稿见 findings V1.94（Grok raw 追加段、席甲 raw 追加段）。
- B3-r1：席甲在主工作区 TDD 修复（RED 11 例失败 → GREEN 5 个测试类 54 例全绿）。
- B4-r1：`modules/M015-b4-r1-grok-raw.md` / `modules/M015-b4-r1-self-raw.md`——范围归因双 PASS；内容 Grok FAIL（001 派生主键超 VARCHAR(64)、003 生产路径未达项目 scope），席甲核源后撤票（self-raw V0.02）。
- B3-r2：只再修 001（派生主键＝前缀 + SHA-256 前 32 位）/003（currentMerchantIdQuietly + 测试桩生产语义）：RED 4 例 → GREEN 5 个测试类 55 例全绿。
- B4-r2：`modules/M015-b4-r2-grok-raw.md` / `modules/M015-b4-r2-self-raw.md` 双 PASS（内容与 §5 闸 3 归因）。
- B5-r1：`modules/M015-b5-r1-grok-raw.md`（41949 bytes SHA 73f8f552a491c2dd1de844adb054bdfebd85a2001c7fb3b00cefe3469a7321ac）/ `modules/M015-b5-r1-self-raw.md`（31876 bytes SHA 2184901a923e29ad41acac364b228fca5e9c3318b027cce3e48c2b774824a666）双 PASS；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。
- 台账：ledger V3.37 先记 PASS，再写本文件。
- 跨模块观察（cross_module_observations）：见 findings V1.94 定稿节「否」项（候选区域类型摊位/公共区与 mall_floor_plans.project_id NOT NULL → M016 迁移；spaces 用途/负责人 → 全局遗漏）。
