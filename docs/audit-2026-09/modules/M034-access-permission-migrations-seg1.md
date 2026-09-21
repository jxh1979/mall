# M034 功能权限点、职级三十档、权限模板与最终权限计算／数据库迁移／第1段
版本：V0.01

2026-09-21 首次审计收口（操作员 2026-09-20 指令：严格按 V0.17，逐模块检查点）。13 个已应用的权限迁移（V003、V092、V158、V169、V180、V204、V261、V270、V321、V322、V387、V393、V394）、18 判据；B1 Grok FAIL（GR-01）／席甲 PASS 经核源撤回 → B2-r1 双 PASS 定稿 M034-001（冲突）→ B3-r1（席甲：主工作区对 complex_test，RED 26 测 1 失败 → 新增 V429 后 GREEN 26/26，Flyway 428→429）→ B4-r1 双 PASS → B5-r1 双 PASS：Grok V0.01 SHA256 5b0759830843c8c65e4c86f2e225bd48419d2912a029375a171a9f1e8ad27f72；席甲 V0.01 SHA256 7bd7d110125dd9da2b536019319d0f099b185fad13a4542b668ea039f4d1f78b；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。B3 累计 1。

修复摘要：001 枚举速查「核销记录状态」原文「系统不提供人工放行：核销要么成功、要么拒绝或失败，任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过」；V270 却种入 `verification.manual_override`（人工放行票务核销）并授给平台管理员，项目权限矩阵文档自 2026-08-06 起已标「停用，待代码轮下线」而字典一直 active。已应用迁移不回改，新增前向迁移 V429：该码 `retired`，清掉它在 role_permissions、permission_template_items、user_permission_overrides 的行，active 的临时授权置 revoked。权限判定先看字典是否 active，故该码对任何主体（含最高权限）一律拒绝，入口即便还在也放不行。

跨模块观察（cross_module_observations）：
- XM-M034-A（→票务后台实现/接口定义所在模块）：`POST /api/v1/ticketing/verifications/manual-override`（TicketingController L498-502）、`TicketingService.manualOverrideVerification`、契约与 `TicketingControllerTests` 相关用例应随「人工放行已废止」下线；`docs/business-decision-log-v0.1.md:417` 的 2026-07-07 增项已被 2026-08-06 R10 口径取代。
- XM-M034-B（→游乐迁移/后台模块）：`amusement.verification.override`（强制改判游乐核验结果，V014 种入）与同一句「系统不提供人工放行」同根，权限矩阵文档 L78/L269 标「停用，待代码轮下线」而库内仍 active；`/api/v1/amusement/manual-overrides` 端点与 `AmusementService` 的强制改判仍在。须由该模块以前向迁移停用并下线。
- 其余登记：`role_platform_admin` 库内持全部 active 码、运行时按 M006 裁决过滤；high 且免审批 10 条（V394 注释写 11）；V261「累计漏 25 条」不可复现；「新权限迁移须同步授平台管理员」无门禁、现状缺 0；外部审计与外包角色 role_permissions 为空是 V181 终态；`permissions.status` 无 CHECK；`roles.description` 仍有英文（非上屏面）。
