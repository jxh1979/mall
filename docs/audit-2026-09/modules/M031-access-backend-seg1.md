# M031 功能权限点、职级三十档、权限模板与最终权限计算／后台实现／第1段
版本：V0.01

2026-09-21 首次审计收口（操作员 2026-09-20 指令：严格按 V0.17，逐模块检查点）。3 个文件（AccessController.java、AccessRepository.java、AccessService.java）、52 判据；B1 双 FAIL（Grok GR-01、GR-03／席甲 CX-M031-001、002）→ B2-r1 双 PASS 定稿 M031-001（细化）、002（细化）、003（技术性），GR-01 与席甲 OBS-9 登记为跨模块项 → B3-r1（席甲：一次性 worktree 里 BASE 全绿 → RED → GREEN 13＋54＋39；变异核对 002 的测试能红）→ B4-r1 双 PASS → B5-r1 双 PASS：Grok V0.01 SHA256 3a01f0a960da0933777fb992102591aaf15a45f1ce6fa5e5ff4aa823bdf9b431；席甲 V0.01 SHA256 c95e5605c13bced405d56aad40196013f4eedb8c05fe1b57528c8a2d8fefb899；四值全等 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。B3 累计 1。

修复摘要：001 岗位映射存档后恢复即回到可定岗的启用态（先清生命周期标记、再用仓储 upsert 写回 active）；同范围已有启用中的替代映射时 409「同岗位同职级已有启用中的岗位模板，须先停用或存档后再恢复」且零写。002 为「保存岗位映射后重算在岗人员派生权／停用即收回」补测试（实现无缺陷）。003 `changePositionTemplateLifecycle` 与 `updateGrantPolicy` 失败也留痕。同批带入 2026-09-11 两个未合并本地提交（OPEN PR #447）与一处已暂存改动的内容，以及配套测试改动（AccessServiceTests.java、AccessControllerTests.java；M006SecurityRegressionTests.java 用「origin/main 版＋本模块 3 行补丁」的发布变体，主工作区该文件另含别的模块未发布改动）。

跨模块观察（cross_module_observations）：
- XM-M031-A（→M032，关联未发布提交 52074492）：主工作区 JdbcAccessRepository 把「organizationId 为 null＝平台管理员全量」改成了 PROJECT_SCOPE_REQUIRED，平台管理员列权限模板/角色/岗位映射会 403；origin/main 不复现；与 AccessRepository 接口契约相违，应在 M032 处置。
- XM-M031-B（→M092）：带 `@Transactional` 的写方法在 catch 里记的失败事件随事务回滚丢失（真库复现：不带事务 1 行、带事务 0 行）；AccessService 有 11 个这样的注解，全后端同型服务文件 25 个；单点处置在 AuditEventService。003 补的两处失败留痕真库生效依赖本项。
- XM-M031-C（→M392）：控制器 6 个映射不在 openapi-p0.yaml。
- 其余登记：接口保存岗位映射 `organizationId` 恒为 null；`grantSummary`/`effectivePermissionsForUser` 无查看事件；`listUserIdsAssignedToPosition` N+1；5 处经办人写成字面量 `"access-api"`；`slug()` 无引用、`wildcard()` 短路死分支；保存映射会把已获批高危重新挂起（字面合 HTML，若操作员认为应免属业务裁决）；停用→存档→恢复后在岗人员派生不自动恢复（偏保守）；管理员直发日常临时授权不经主管链（HTML 未禁止）；`M006JdbcRegressionTests` 临时表缺 `grant_group_id`（M037）。
