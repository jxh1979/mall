CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M032 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-21T05:48:10Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`，origin/main 2c178f7a。
- 模块 M032「功能权限点、职级三十档、权限模板与最终权限计算／后台实现／第2段」25 文件 2488 行，52 判据。B3 累计 0。
- 三态：25 个文件里 23 个与 origin/main 逐字节相同；JdbcAccessRepository.java（未发布提交 52074492、49aaaa94）与 AuthorizationService.java（未发布提交 e9b88faf 等加已暂存改动，V0.08→V0.13）不同。
- D-029 环境决定（技术性，席甲自定）：本模块 25 个文件里 JdbcAccessRepository.java 与 AuthorizationService.java 带着 2026-09-13 起未发布的跨模块工作流改动（提交 52074492、e9b88faf、49aaaa94 加已暂存改动；主工作区另有 84 个已暂存文件、49 个未推提交与之并存），其中 AuthorizationService.java:141 依赖同样未发布的 `SessionIssuer.MASTER_ACCOUNT_TYPE`，「origin/main＋本模块 25 个现行文件」编译失败——这两个文件的主工作区版不能随本模块单独发布。发布口径：本模块检查点只带「两版都成立、能在 origin/main 上独立验证」的修复与台账/raw；只在未发布版成立的项登记到该未发布工作流，由操作员决定该工作流何时整体立项（先例 D-027 合成模块）。两席照常全文审主工作区现状，结论注明在哪一版成立。
- 顺序：Grok 已先派（modules/M032-b1-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何源码/测试/文档；数据库只对 complex_test SELECT（测试类自身写入除外）；一次性 worktree 用后已还原移除。

## 1. 文件覆盖表

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| JdbcAccessRepository.java | 1485 | 75999 | ecc78c0b65ecf3cf4154947c6160622b39eb881404bbb3501788839a74c9d86b | 全读 |
| AccessActor.java | 11 | 223 | 5b0115b479d4a2f9ddec2f7bc3837ffbcb3ca18dfd30b98ada61250545eddf3a | 全读 |
| AccessCredentialProvisioner.java | 12 | 335 | c87b5ff721de2308b4a0ab0b9395a9dca19f8551d157f798acece0ec6688ab5b | 全读 |
| JdbcAccessCredentialProvisioner.java | 69 | 3103 | 06a4e3f9f09c2902eabb9d53161330b05b1fdfe3c7f2ae5821c129ed121a082e | 全读 |
| AccessGrantPolicy.java | 44 | 2184 | 6f0de0f7052efa45430347547746379da6ec376235db46fe9e19f5017e087fb7 | 全读 |
| AccessGrantPositionSummary.java | 10 | 232 | 08fa864f1b0cada3309ea24503d159df5b1075c77e49a04ad75aa61ca1c156df | 全读 |
| AccessGrantRequestRecord.java | 35 | 934 | b056364a8d6526123b1137c0ec7016240ca42083e9ca1050f905a4e55166ee89 | 全读 |
| AccessGrantSummary.java | 9 | 219 | 7648579e55d34871a4deea19ccde7eef74f592613d3cdf6015d7ca7dce7e1d96 | 全读 |
| AccessGrantUserSummary.java | 8 | 155 | 80fb7b5d47b0afdec3cda37e2c954950242603ed23e2d847f136b3b794636a7e | 全读 |
| AccessUser.java | 37 | 1042 | 717ebbd9d9ce56217a6045f20b42f47ec4aa76fdd2a14ac97c6b49e94387ceb1 | 全读 |
| AuthorizationService.java | 537 | 25639 | 990bce522fa847c5a26ddc23fd623477617d12db244b2fc09b87824cd758c062 | 全读 |
| EffectivePermissions.java | 23 | 882 | 68ff6d1da086bfb7e1aa0b33aed0de3962a515737a91096fb791cfc9faced44e | 全读 |
| PermissionDefinition.java | 11 | 247 | 1d0ee72f430acf5a5dd8ff7f46af8abdcd630e84e9af83740ffe88c37aa792b4 | 全读 |
| PermissionTemplate.java | 37 | 1494 | 84749f393404f07ba97d8cde6686496532d6f9834b86c86273ab51cc167504ee | 全读 |
| PermissionTemplateApplication.java | 20 | 450 | 660d7a9be8d5fbc06c4872278b1ce7d5d5bf8d856eea130714f1c590ae254a97 | 全读 |
| PermissionTemplateApplyResult.java | 8 | 170 | 80134f3a3e8157cbf3c8af227a1f4bea9c4aa6c8986466fc31f70dd9d8a2d076 | 全读 |
| PermissionTemplateItem.java | 10 | 232 | 6f87855d883c18299f42e3703309c51cb77a1669587b6cc784a2853056c61b4f | 全读 |
| PositionRoleTemplate.java | 34 | 1284 | 1d09de44f1d488d0dbef17a4c62dd42cb411e61f82ec1ec61ac63c8238442b2b | 全读 |
| RoleRecord.java | 17 | 400 | f0d867f56c06bbd8cfb886b6a713cbc7a85fb55e62109b28c6004e1f7ceae77b | 全读 |
| RoleAssignmentRecord.java | 7 | 125 | 2034337e12c8d9b38bb93205058b5844047148c5965932a259e9e17375f1ad0e | 全读 |
| StaffLevelRecord.java | 19 | 609 | ed83f2f33532f072b88c2a271b6f39d7fdd20fbfe1ce0078634aaef66775c474 | 全读 |
| StaffLevelAssignment.java | 20 | 585 | 602c779aa27cc96d3be7794df7f772a353a8eca8b4a9d37445e66dfbb6b0af29 | 全读 |
| UserPositionAssignment.java | 14 | 302 | 015ec50b24fe7458a52131f7fe2e7446aab165000f5087d3f57cafc115ca3ff1 | 全读 |
| UserPositionAssignmentResult.java | 10 | 269 | 437d1c416d5027911eff9d34c421d93202dbe523f9658653406acdec9524775c | 全读 |
| package-info.java | 1 | 37 | bf3835ba131ca4ad8c2e1ab5311cab18d2f9fe5755ecee9a311b47095af9efe8 | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m032_b1_run.log）

```
START=2026-09-21T05:42:04Z HEAD=e456ba96 origin/main=2c178f7a DB=complex_test files=25
== 0. files (lines/bytes/sha256 | status | vs origin/main)
JdbcAccessRepository.java 1485 75999 ecc78c0b65ecf3cf4154947c6160622b39eb881404bbb3501788839a74c9d86b | [] | [ 1 file changed, 19 insertions(+), 16 deletions(-)]
AccessActor.java 11 223 5b0115b479d4a2f9ddec2f7bc3837ffbcb3ca18dfd30b98ada61250545eddf3a | [] | []
AccessCredentialProvisioner.java 12 335 c87b5ff721de2308b4a0ab0b9395a9dca19f8551d157f798acece0ec6688ab5b | [] | []
JdbcAccessCredentialProvisioner.java 69 3103 06a4e3f9f09c2902eabb9d53161330b05b1fdfe3c7f2ae5821c129ed121a082e | [] | []
AccessGrantPolicy.java 44 2184 6f0de0f7052efa45430347547746379da6ec376235db46fe9e19f5017e087fb7 | [] | []
AccessGrantPositionSummary.java 10 232 08fa864f1b0cada3309ea24503d159df5b1075c77e49a04ad75aa61ca1c156df | [] | []
AccessGrantRequestRecord.java 35 934 b056364a8d6526123b1137c0ec7016240ca42083e9ca1050f905a4e55166ee89 | [] | []
AccessGrantSummary.java 9 219 7648579e55d34871a4deea19ccde7eef74f592613d3cdf6015d7ca7dce7e1d96 | [] | []
AccessGrantUserSummary.java 8 155 80fb7b5d47b0afdec3cda37e2c954950242603ed23e2d847f136b3b794636a7e | [] | []
AccessUser.java 37 1042 717ebbd9d9ce56217a6045f20b42f47ec4aa76fdd2a14ac97c6b49e94387ceb1 | [] | []
AuthorizationService.java 537 25639 990bce522fa847c5a26ddc23fd623477617d12db244b2fc09b87824cd758c062 | [M ] | [ 1 file changed, 14 insertions(+), 23 deletions(-)]
EffectivePermissions.java 23 882 68ff6d1da086bfb7e1aa0b33aed0de3962a515737a91096fb791cfc9faced44e | [] | []
PermissionDefinition.java 11 247 1d0ee72f430acf5a5dd8ff7f46af8abdcd630e84e9af83740ffe88c37aa792b4 | [] | []
PermissionTemplate.java 37 1494 84749f393404f07ba97d8cde6686496532d6f9834b86c86273ab51cc167504ee | [] | []
PermissionTemplateApplication.java 20 450 660d7a9be8d5fbc06c4872278b1ce7d5d5bf8d856eea130714f1c590ae254a97 | [] | []
PermissionTemplateApplyResult.java 8 170 80134f3a3e8157cbf3c8af227a1f4bea9c4aa6c8986466fc31f70dd9d8a2d076 | [] | []
PermissionTemplateItem.java 10 232 6f87855d883c18299f42e3703309c51cb77a1669587b6cc784a2853056c61b4f | [] | []
PositionRoleTemplate.java 34 1284 1d09de44f1d488d0dbef17a4c62dd42cb411e61f82ec1ec61ac63c8238442b2b | [] | []
RoleRecord.java 17 400 f0d867f56c06bbd8cfb886b6a713cbc7a85fb55e62109b28c6004e1f7ceae77b | [] | []
RoleAssignmentRecord.java 7 125 2034337e12c8d9b38bb93205058b5844047148c5965932a259e9e17375f1ad0e | [] | []
StaffLevelRecord.java 19 609 ed83f2f33532f072b88c2a271b6f39d7fdd20fbfe1ce0078634aaef66775c474 | [] | []
StaffLevelAssignment.java 20 585 602c779aa27cc96d3be7794df7f772a353a8eca8b4a9d37445e66dfbb6b0af29 | [] | []
UserPositionAssignment.java 14 302 015ec50b24fe7458a52131f7fe2e7446aab165000f5087d3f57cafc115ca3ff1 | [] | []
UserPositionAssignmentResult.java 10 269 437d1c416d5027911eff9d34c421d93202dbe523f9658653406acdec9524775c | [] | []
package-info.java 1 37 bf3835ba131ca4ad8c2e1ab5311cab18d2f9fe5755ecee9a311b47095af9efe8 | [] | []
49aaaa94 fix(access): 单次临时授权按整组一次消耗
e9b88faf fix(access): 非密钥主体审批层级不超过上两级
52074492 fix(access): 平台管理员不再整类关掉商户项目隔离
== 1. 基线一：主工作区
main MVN_EXIT=1
   AccessControllerTests tests=13 errors=0 skipped=0 failures=0
   AccessJdbcIntegrationTests tests=24 errors=0 skipped=0 failures=0
   AccessServiceTests tests=54 errors=6 skipped=0 failures=0
       error assignUserPositionLandsNormalPartAndHoldsHighRiskPartUntilChainApproves | 退款、免单、授权与资金事项必须登记直接受益人
       error dailyTemporaryPermissionApplicationEntersSupervisorChain | 退款、免单、授权与资金事项必须登记直接受益人
       error approvedTemporaryGrantExpiresByEffectivePermissionRead | 退款、免单、授权与资金事项必须登记直接受益人
       error auditorOutsideTemplatePermissionNeedsApprovedTemporaryGrantBeforeUse | 退款、免单、授权与资金事项必须登记直接受益人
       error approvedUserRoleGrantRequestUsesSupervisorReviewAndLandsAssignment | 退款、免单、授权与资金事项必须登记直接受益人
       error removingTheHighRiskHoldDenyRequiresApproval | 退款、免单、授权与资金事项必须登记直接受益人
   AuthorizationServiceTests tests=24 errors=0 skipped=0 failures=0
   M006JdbcRegressionTests tests=12 errors=1 skipped=0 failures=0
       error temporaryGrantLogsItsActualSourceAndConsumesOnlyOnce | PreparedStatementCallback; bad SQL grammar [UPDATE temporary_permission_grants&#10;   SET consumed = TRUE,&#10;       consumed_at = ?&#10; WHERE grant
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=0
== 2. 基线二：一次性 worktree＝origin/main＋本模块 25 个现行文件（测试文件取 origin/main 版；为过 Flyway 校验拷入主工作区未发布的迁移）
migrations copied: V427__m009_project_profiles_and_associations.sql
 M backend/src/main/java/com/complex/platform/access/AuthorizationService.java
 M backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
scratch MVN_EXIT=1
[ERROR] COMPILATION ERROR : 
[ERROR] /home/james/scratch-m032b1/backend/src/main/java/com/complex/platform/access/AuthorizationService.java:[141,33] cannot find symbol
[ERROR] /home/james/scratch-m032b1/backend/src/main/java/com/complex/platform/access/AuthorizationService.java:[141,33] cannot find symbol
scratch removed
== 3. JdbcAccessRepository 写路径列名对 complex_test
UNKNOWN TABLE JdbcAccessRepository.java requested_group
write-path columns checked = 198 missing = 0 ; tables referenced = 19
tables: access_grant_requests access_users auth_credentials permission_template_applications permission_template_items permission_template_usage_logs permission_templates permissions position_role_templates role_permissions roles staff_level_assignments staff_levels staff_profiles supervisor_review_policies temporary_permission_grants user_permission_overrides user_position_assignments user_roles
== 4. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T05:42:18Z
```

补充只读核对：
- `temporary_permission_grants.grant_group_id` 为 character varying、`id` 为 uuid（complex_test information_schema）；该列自已发布的 V004 迁移起就有，未发布改动里 `WHERE grant_group_id = :source_id` 类型无误。M006JdbcRegressionTests 那条报错的根因是 `column "grant_group_id" does not exist`——测试自建的临时表没有该列。
- `git show origin/main:…/auth/SessionIssuer.java | grep -c MASTER_ACCOUNT_TYPE` = 0；主工作区 `SessionIssuer.java:14` 有。
- `grep -rn "new EffectivePermissions(" backend/src/main/java` 只有 `AuthorizationService.java:206` 一处，第四参恒 `false`；origin/main 同。前端 185 个文件引用 `wildcard` 字段。
- origin/main 版 `currentSupervisorLevel` 为 `(rank + 2) / 3` 不封顶；消费方 SupervisorReviewService 在 origin/main 另有 `isMasterAuthority` 闸（L308/L338、L762/L790）。主工作区版封顶为 2。

## 3. 三线审计

- 第一性原理线。用途：权限仓储的 Jdbc 实现、最终权限计算与各 scope 判定、口令入库、授权政策值对象、21 个记录类。实际行为见 §4 逐项。未经验证的前提：①主工作区版能单独发布——不能（基线二编译失败，CX-M032-004）；②主工作区版的「空范围即拒绝」与调用方一致——不一致（CX-M032-002）；③未发布的 grant_group_id 消耗改动有测试守着——守它的真库测试夹具落后、现为红（CX-M032-003）；④类注释对 wildcard 的现在时声称成立——不成立（CX-M032-001，两版都如此）。
- 顺从式线：52 判据逐项见 §4；25 文件全读。
- 对抗式线（grilling）：同一笔单次授权两个并发请求→UPDATE 带 `consumed = FALSE`，只有一个成功，另一个 `deny`；看一眼菜单会不会烧掉单次授权→不会，`effectivePermissions` 不消耗；显式拒绝与最高权限谁先→先判拒绝（L77-79 在 L80 之前）；平台管理员默认角色里的高危码→不算逐项授权；到期清理重复读→只命中 active 行，幂等；岗位模板物理删除→恒抛；授权申请二次决定→`WHERE status = 'pending'` 影响 0 行即抛；非 UUID 的临时授权 id→`UUID.fromString` 抛非中文异常（OBS-2）；主工作区版下无项目归属的主体调任何用 `currentProjectScopeQuietly` 的读口→抛 PROJECT_SCOPE_REQUIRED（方法名仍叫 Quietly，OBS-1）。

## 4. 52 判据逐项

| 判据 | 适用｜证据｜结论 |
|---|---|
| BEQR-157 员工状态 | 无本面｜原文是员工档案状态；本模块只读写登录账号状态（`JdbcAccessCredentialProvisioner` 只认 active/invited/disabled）与账号记录，不另造员工状态值 |
| BEQR-182 考勤日结果 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 25 个文件不读写、不展示、不声称该对象（职级薪资字段只是定岗记录的一部分，不是薪资单） |
| BEQR-183 打卡方式 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 25 个文件不读写、不展示、不声称该对象（职级薪资字段只是定岗记录的一部分，不是薪资单） |
| BEQR-184 请假类型 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 25 个文件不读写、不展示、不声称该对象（职级薪资字段只是定岗记录的一部分，不是薪资单） |
| BEQR-185 请假单状态 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 25 个文件不读写、不展示、不声称该对象（职级薪资字段只是定岗记录的一部分，不是薪资单） |
| BEQR-186 薪资单构成 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 25 个文件不读写、不展示、不声称该对象（职级薪资字段只是定岗记录的一部分，不是薪资单） |
| BEQR-187 员工提成发放 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 25 个文件不读写、不展示、不声称该对象（职级薪资字段只是定岗记录的一部分，不是薪资单） |
| BSM-001 整体说明：这套系统到底管什么 | 无本面｜本模块是权限仓储实现、最终权限计算服务与记录类，不承载、不展示、不声称「整体说明：这套系统到底管什么」的对象、状态或界面文字 |
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | 适用｜「权限分岗」的计算链在 `AuthorizationService`：显式拒绝优先→最高权限/平台管理员日常码/客人自助白名单→显式或日常授予；单次授权只在真正执行时消耗（L36-61、L64-94、L150-207）｜通过 |
| BSM-009 基础管理 | 无本面｜本模块是权限仓储实现、最终权限计算服务与记录类，不承载、不展示、不声称「基础管理」的对象、状态或界面文字 |
| BSM-028 主要功能 | 无本面｜本模块是权限仓储实现、最终权限计算服务与记录类，不承载、不展示、不声称「主要功能」的对象、状态或界面文字 |
| BSM-029 和谁有关 | 无本面｜本模块是权限仓储实现、最终权限计算服务与记录类，不承载、不展示、不声称「和谁有关」的对象、状态或界面文字 |
| BSM-073 主要功能 | 无本面｜本模块是权限仓储实现、最终权限计算服务与记录类，不承载、不展示、不声称「主要功能」的对象、状态或界面文字 |
| BSM-084 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 25 个文件不读写、不展示、不声称该对象（职级薪资字段只是定岗记录的一部分，不是薪资单） |
| BSM-085 日常工作 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 25 个文件不读写、不展示、不声称该对象（职级薪资字段只是定岗记录的一部分，不是薪资单） |
| BSM-086 排班、考勤与请假 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 25 个文件不读写、不展示、不声称该对象（职级薪资字段只是定岗记录的一部分，不是薪资单） |
| BSM-087 我的薪资 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 25 个文件不读写、不展示、不声称该对象（职级薪资字段只是定岗记录的一部分，不是薪资单） |
| BSM-088 员工提成、活动执行与人员服务边界 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 25 个文件不读写、不展示、不声称该对象（职级薪资字段只是定岗记录的一部分，不是薪资单） |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | 适用｜「不同人只看自己该看的」：`listUsers(org)`/`listUsersByMerchant`/`listRoles(org)`/`listTemplates(org)`/`listPositionRoleTemplates(org)`/`pageGrantRequests` 均带项目或商户谓词；主工作区版对空范围改为拒绝，与已发布的接口契约和调用点不一致（CX-M032-002，只在未发布版成立）｜通过（origin/main 版）；主工作区版见 CX-M032-002 |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | 无本面｜本模块是权限仓储实现、最终权限计算服务与记录类，不承载、不展示、不声称「相互关系：为什么这些子系统必须连在一起」的对象、状态或界面文字 |
| BSM-120 已确认规则和实施前输入项 | 无本面｜本模块是权限仓储实现、最终权限计算服务与记录类，不承载、不展示、不声称「已确认规则和实施前输入项」的对象、状态或界面文字 |
| GEN-001 必要性与最小性 | 适用｜25 文件 2488 行；记录类无多余成员；`EffectivePermissions.wildcard` 字段恒为 false（CX-M032-001）｜通过（除 CX-M032-001） |
| GEN-002 可执行正确性 | 适用｜基线一（主工作区）：AccessJdbcIntegrationTests 24、AuthorizationServiceTests 24、M006SecurityRegressionTests 39、AccessControllerTests 13 全绿；AccessServiceTests 6 个报错来自别的模块未发布改动（M031 已查清）；M006JdbcRegressionTests 1 条报错来自本模块未发布改动（CX-M032-003）。基线二（origin/main＋本模块 25 个现行文件）编译失败（CX-M032-004）｜不通过（CX-M032-003/004，均只在未发布版成立） |
| GEN-003 测试有效性 | 适用｜AuthorizationServiceTests 24 与 AccessJdbcIntegrationTests 24 真库执行权限计算与 SQL；M006JdbcRegressionTests 的临时表夹具落后于未发布的 grant_group_id 改动（CX-M032-003）｜通过（origin/main 版） |
| GEN-004 信任边界校验 | 适用｜`AccessGrantPolicy` 构造即校验上下限与风险等级闭集；`BoundedInt.checked` 校验查询时间窗；`findTemporaryGrant` 对非 UUID 的 id 会在 `UUID.fromString` 抛 IllegalArgumentException（OBS-2，id 正常来自列表，不立项）｜通过 |
| GEN-005 授权与最小权限 | 适用｜显式拒绝优先（L77-79）；平台管理员默认角色里的高危码不当逐项授权（`isExplicitOrDailyGrant`）；最高权限独立识别；`holdsPermissionViaRole` 查询异常保守返回 false；主工作区版的 scope 方法不再因平台管理员身份放开（注释引七类红线第四条）｜通过 |
| GEN-006 数据完整性 | 适用｜时间窗判定 `isActive`：未到生效时间或已过失效时间均无效，`validUntil` 为空视为长期；到期清理用同一 `checkedAt`｜通过 |
| GEN-007 并发与幂等 | 适用｜`lockGrantAuthorizationFacts` 以 SHARE MODE 锁 8 张授权定义表（源码注明上限与升级路径）；`findPositionRoleTemplateById`/`findGrantRequestById` FOR UPDATE、`findPositionRoleTemplate` FOR SHARE；`saveGrantRequest` 非 pending 只许从 pending 改一次；单次消耗 UPDATE 带 `consumed = FALSE`｜通过 |
| GEN-008 接口与模式一致性 | 适用｜origin/main 版 Jdbc 与 `AccessRepository` 接口契约「Null = platform admin (all)」一致；主工作区版三个带参列表对 null 改为拒绝，与接口注释和 AccessService 调用点相违（CX-M032-002）｜通过（origin/main 版）；主工作区版不通过 |
| GEN-009 错误处理与恢复 | 适用｜影响行数不为 1 即抛中文 IllegalStateException；`holdsPermissionViaRole`/`levelRankOf`/`*Quietly` 的降级均为保守方向；主工作区版 `currentProjectScopeQuietly` 名为 Quietly 却会抛（OBS-1）｜通过 |
| GEN-010 安全与敏感信息 | 适用｜口令不落日志、不回显；bcrypt 强度 10、SecureRandom；凭证状态只有 active/disabled 两态映射｜通过 |
| GEN-011 可观测与审计证据 | 适用｜到期与单次消耗留痕见 PGM-001；失败留痕在服务层（M031），事务内丢失已登记 XM-M031-B（→M092）｜通过 |
| GEN-012 资源与性能边界 | 适用｜角色列表批量取权限码（L318-337 消除 1+N）；`findRoleById` 单条仍逐条取码（合理）；`findPermissionEffects` 每次读先做本人到期清理（源码注明 ponytail 上限）｜通过 |
| GEN-013 构建配置与依赖 | 适用｜只依赖 Spring JDBC、Spring Security Crypto、Jackson 注解、JDK｜通过 |
| GEN-014 数据库迁移正确性 | 无本面｜本模块无迁移；SQL 引用的 19 张表与写路径 198 列在 complex_test 全部存在（探针 3；`requested_group` 是 CTE 名不是表） |
| GEN-015 界面可用性与可访问性 | 适用｜面向操作者的异常文案均为中文；`PERMISSION_DENIED` 文案带权限码（机器标识）｜通过 |
| GEN-016 文档可验证性 | 适用｜`EffectivePermissions.java` 类注释现在时声称「wildcard=true 仅当 actor 命中平台管理员桥」，全后端唯一构造点 `AuthorizationService.java:206` 恒传 false（origin/main 同）——声称不成立（CX-M032-001）；其余注释逐条对照实现一致｜不通过（CX-M032-001） |
| GEN-017 业务来源分类 | 适用｜未见另造业务规则；主工作区版对「红线④是否约束平台管理员」的取向属 2026-09-13 未发布工作流的既定方向，不在本模块重判｜通过 |
| GEN-018 第一性原理证据 | 适用｜结论落到源码原文、两条基线测试输出、complex_test 只读查询与 HTML 原句｜通过 |
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | 适用｜到期、单次消耗均写 `permission_template_usage_logs`（`expireLapsedEffects` L910-941、`markSingleUsePermissionConsumed` L1010-1062），UPDATE 带 `consumed = FALSE`/`status = 'active'` 原子闸，重复读取不重复写｜通过 |
| PGM-008 员工和手机 | 无本面｜本模块是权限仓储实现、最终权限计算服务与记录类，不承载、不展示、不声称「员工和手机」的对象、状态或界面文字 |
| PGM-022 空间和点位：所有业务都要知道“发生在哪” | 无本面｜本模块是权限仓储实现、最终权限计算服务与记录类，不承载、不展示、不声称「空间和点位：所有业务都要知道“发生在哪”」的对象、状态或界面文字 |
| PGM-024 影响业务 | 无本面｜本模块是权限仓储实现、最终权限计算服务与记录类，不承载、不展示、不声称「影响业务」的对象、状态或界面文字 |
| PGM-026 主要功能 | 无本面｜本模块是权限仓储实现、最终权限计算服务与记录类，不承载、不展示、不声称「主要功能」的对象、状态或界面文字 |
| PGM-032 员工和手机：人、私人手机、工作手机要能对应起来 | 无本面｜本模块是权限仓储实现、最终权限计算服务与记录类，不承载、不展示、不声称「员工和手机：人、私人手机、工作手机要能对应起来」的对象、状态或界面文字 |
| PGM-033 主要功能 | 适用｜定岗落库的四张表（staff_level_assignments、user_position_assignments、permission_template_applications、派生替换）写路径列名对 complex_test 全部存在（探针 3：198 列 0 缺）；`replacePositionGrants` 先撤旧派生应用再写新派生｜通过 |
| PGM-034 工作手机和企业微信 | 无本面｜本模块是权限仓储实现、最终权限计算服务与记录类，不承载、不展示、不声称「工作手机和企业微信」的对象、状态或界面文字 |
| PGM-035 提成发放和人员服务治理 | 无本面｜原文属考勤、请假、薪资单或提成发放；本模块 25 个文件不读写、不展示、不声称该对象（职级薪资字段只是定岗记录的一部分，不是薪资单） |
| PGM-036 设备管理：谁领了什么设备，设备能不能用 | 无本面｜本模块是权限仓储实现、最终权限计算服务与记录类，不承载、不展示、不声称「设备管理：谁领了什么设备，设备能不能用」的对象、状态或界面文字 |
| PGM-048 商户侧：在授权业务范围内全开放 | 适用｜商户侧：`listUsersByMerchant` 按 merchant_id；`pageGrantRequests` 商户谓词；`AuthorizationService.requirePlatformScope`/`requirePlatformComplianceScope` 对商户主体与外包 fail-closed｜通过 |
| PGM-059 客人与员工的个人信息保护 | 适用｜口令只以 bcrypt(10) 哈希入库（`JdbcAccessCredentialProvisioner`）；`AccessUser.phoneHash` 带 `@JsonIgnore`、`RoleRecord.organizationId` 带 `@JsonIgnore`｜通过 |
| PGM-066 已拍板规则和待排期问题 | 适用｜岗位模板禁止物理删除（Jdbc `deletePositionRoleTemplate` 恒抛）；占用计数含「未记映射 id 的旧定岗行」回退匹配（L505-516）；授权申请指纹只在 pending 且为空时写一次（L728-736）｜通过 |

## 5. 本次 Findings

### CX-M032-001（技术性；两版都成立，可随本模块发布）`EffectivePermissions` 类注释对 wildcard 的现在时声称不成立

- 位置：`EffectivePermissions.java` 类注释末段「`wildcard=true` 仅当 actor 命中平台管理员桥（status=active 且 accountType=platform_admin），但前端/后端仍须让 `denies` 优先于 wildcard。」
- 证据：全后端唯一构造点 `AuthorizationService.java:206` `new EffectivePermissions(userId, List.copyOf(effective), denies, false, checkedAt)`，第四参恒 false；平台管理员的日常码是并入 `grants` 返回的（L200-204）。origin/main 同。前端 185 个文件读该字段，已闭环的 `AccessService.requireActorCanGrant` 里 `if (self.wildcard()) return;` 因此是死分支（M031 已登记）。
- 正常触发：平台管理员调 `/api/v1/access/effective-permissions`，返回 `wildcard=false`，与类注释相反；按注释写前端或排障的人会被误导。
- 依据：GEN-016（注释与文档性声称须可复现）。
- 最小拟处置：只改该类注释，写成现状——`wildcard` 现恒为 false，平台管理员的日常功能码已并入 `grants`，`denies` 仍优先；不改字段、不改计算。

### CX-M032-002（细化；只在主工作区未发布版成立）空范围即拒绝的三个带参列表与已发布的接口契约、调用点不一致

- 位置：`JdbcAccessRepository.java:74-75`、`:308-309`、`:417-418`（`requireProjectScopeId`，L1473-1477）；对照 `AccessRepository.java:19-28`「Null = platform admin (all templates)」与 `AccessService.java` 三个列表方法对平台管理员传 null。
- 证据：Grok M031 B1 GR-01、M031 B2 两席核实；origin/main 版 Jdbc 对 null 回退全量，不复现。即 XM-M031-A。
- 正常触发（主工作区版）：平台管理员打开权限模板、角色或岗位映射列表即 403。
- 最小拟处置：不在本模块单点改。按未发布工作流的方向（空范围拒绝），应改的是已闭环 M031 的调用点与接口注释；按接口契约方向，应回退本模块这三处。两个方向都牵动该工作流的取向，登记为该工作流的发布前置条件。

### CX-M032-003（技术性；只在主工作区未发布版成立）单次临时授权改按整组消耗后，守它的真库测试夹具未跟上

- 位置：`JdbcAccessRepository.java:1037-1046`（`WHERE grant_group_id = :source_id`）、`:887-895`（`grant_group_id::text AS source_id`）；夹具在 `M006JdbcRegressionTests.java` 的临时表定义（M037 文件）。
- 证据：基线一 `M006JdbcRegressionTests tests=12 errors=1`，根因 `column "grant_group_id" does not exist`。
- 最小拟处置：夹具临时表补 `grant_group_id` 列并在插入时赋值；随该未发布工作流一起处置。

### CX-M032-004（技术性；环境事实）本模块主工作区版不能单独发布

- 证据：基线二 `COMPILATION ERROR … AuthorizationService.java:[141,33] cannot find symbol`（`SessionIssuer.MASTER_ACCOUNT_TYPE` 只在主工作区）。
- 处置：见 §0 D-029。

## 6. 观察（登记，不立项）

- OBS-1（主工作区版）：`currentProjectScopeQuietly()` 现在会抛 PROJECT_SCOPE_REQUIRED，方法名与返回 Optional 的形态仍是「不抛」时代的；方法注释已写明「没有项目归属则拒绝」。调用方里 `.orElse(...)`/`.orElseGet(全量)` 分支因此成为死分支（含已闭环 M031 的 `canViewGrantRequest`）。
- OBS-2：`findTemporaryGrant` 对非 UUID 的 id 由 `UUID.fromString` 抛 IllegalArgumentException（非中文、非 ApiException）；id 正常来自列表，不为此立项。
- OBS-3：origin/main 版 `currentSupervisorLevel` 不封顶，M3 以上职级会得到 ≥3；origin/main 的消费方另有最高权限闸。主工作区版封顶为 2（未发布）。
- OBS-4：生命周期保存 SQL 与 `withLifecycle` 恒写 inactive；M031-001 已在调用侧用「先生命周期保存、再 upsert」解决，本模块不动。
- OBS-5：`listTemplates(org)` 不含平台模板、`listRoles(org)` 含全局角色——与「非平台管理员不得套用平台模板」的既有规则一致。
- OBS-6：`lockGrantAuthorizationFacts` 以表级 SHARE 锁暂停授权定义写入、`findPermissionEffects` 读时顺带清理到期行——源码均有 ponytail 注释写明上限与升级路径。
- OBS-7：`createTemporaryGrant` 逐码逐条 INSERT（非批量），一笔授权的码数很小，不立项。

## 7. 结论

席甲 B1 票：**FAIL**（CX-M032-001 两版都成立；CX-M032-002、003、004 只在主工作区未发布版成立；OBS-1…7 登记）。25 文件 2488 行全读、52 判据逐项、三线完成；未改任何文件；落盘前未读 Grok 本次正文。


## M032 B2-r1 整批候选复核（席甲票，V0.02）

- 时间：2026-09-21T06:05:30Z。输入：modules/M032-b1-grok-raw.md V0.01（46223 bytes，SHA256 1ac439d8812b0550e153d7e3c4ca71d2715023795d88962388bfc773beefb165）、modules/M032-b1-self-raw.md V0.01（上方本报告）；候选 findings.md 顶部「当前 M032 B2-r1 整批候选」段（全文件 SHA256 c8a6005ab395930b5e84f8fdb4c213caebcb4f5ea6d2c3243877ce6196f71ee9）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：GR-03＋真库探针→001；CX-M032-001＋O-3→002；GR-01＝CX-M032-002→W-1；GR-02＝OBS-1→W-2；CX-M032-003＝O-1→W-3；CX-M032-004＝O-12→W-4；O-4/O-5 并入同一未发布工作流；两席其余观察全部登记；无漏并、无改写。
- 自审纠正：GR-03 席甲漏——B1 读了 `AccessGrantPolicy` 的四字段与 `saveGrantPolicy` 的整份覆盖，但没有去库里看这一行政策 JSON 的实际形状。教训同 [权限字典先查库内实态]：凡代码里有「整份 JSON 进出」的值对象，先 SELECT 库内那一行再下结论。核源时进一步发现读路径在真库上直接失败，已用探针坐实。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。
