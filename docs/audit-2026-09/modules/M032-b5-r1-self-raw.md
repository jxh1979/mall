CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M032 B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；本轮改动为席甲 B3 所写，且席甲 B1 漏了 GR-03，以 Grok 票为准）。2026-09-21T06:33:39Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`，origin/main `2c178f7a`。
- 模块 M032 25 文件（§1 全读），52 判据；B3 累计 1。发布口径见 ledger D-029：origin/main 版＋本模块补丁；主工作区 JdbcAccessRepository.java、AuthorizationService.java 的未发布工作流改动不在发布口径内，W-1…W-4 已登记。
- 顺序：本席取证脚本先跑完，随后落本报告；落盘前未读 Grok 正文（modules/M032-b5-r1-grok-raw.md 存在=False）。只读承诺：未改任何已跟踪文件；数据库只对 complex_test SELECT（测试类自身写入在事务内回滚）。
- 本轮补的方法（B1 漏了 GR-03）：凡「整份 JSON 进出」的值对象，先 SELECT 库内那一行的实际形状，再对记录类字段。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| JdbcAccessRepository.java | 1485 | 76013 | 7639b374fd956a5d02a24bca0a7d6165873f2df9fe52a06fd421e7088ef995ba | 全读 |
| AccessActor.java | 11 | 223 | 5b0115b479d4a2f9ddec2f7bc3837ffbcb3ca18dfd30b98ada61250545eddf3a | 全读 |
| AccessCredentialProvisioner.java | 12 | 335 | c87b5ff721de2308b4a0ab0b9395a9dca19f8551d157f798acece0ec6688ab5b | 全读 |
| JdbcAccessCredentialProvisioner.java | 69 | 3103 | 06a4e3f9f09c2902eabb9d53161330b05b1fdfe3c7f2ae5821c129ed121a082e | 全读 |
| AccessGrantPolicy.java | 55 | 2915 | 81873e72e8332532af9469de5e2182f2988ca2f5ab5fdf31550470930796e8e2 | 全读 |
| AccessGrantPositionSummary.java | 10 | 232 | 08fa864f1b0cada3309ea24503d159df5b1075c77e49a04ad75aa61ca1c156df | 全读 |
| AccessGrantRequestRecord.java | 35 | 934 | b056364a8d6526123b1137c0ec7016240ca42083e9ca1050f905a4e55166ee89 | 全读 |
| AccessGrantSummary.java | 9 | 219 | 7648579e55d34871a4deea19ccde7eef74f592613d3cdf6015d7ca7dce7e1d96 | 全读 |
| AccessGrantUserSummary.java | 8 | 155 | 80fb7b5d47b0afdec3cda37e2c954950242603ed23e2d847f136b3b794636a7e | 全读 |
| AccessUser.java | 37 | 1042 | 717ebbd9d9ce56217a6045f20b42f47ec4aa76fdd2a14ac97c6b49e94387ceb1 | 全读 |
| AuthorizationService.java | 537 | 25639 | 990bce522fa847c5a26ddc23fd623477617d12db244b2fc09b87824cd758c062 | 全读 |
| EffectivePermissions.java | 23 | 960 | 55bef627be66a02a45ddc4968a15c60b1e9d3357de7e3eaeb8a819f085e0247e | 全读 |
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

## 2. 可执行证据原文（/home/james/audit-tmp/m032_b5_run.log）

```
START=2026-09-21T06:33:20Z HEAD=e456ba96 origin/main=2c178f7a DB=complex_test files=25
== 0. files (lines/bytes/sha256 | vs origin/main)
JdbcAccessRepository.java 1485 76013 7639b374fd956a5d02a24bca0a7d6165873f2df9fe52a06fd421e7088ef995ba | [ 1 file changed, 21 insertions(+), 18 deletions(-)]
AccessActor.java 11 223 5b0115b479d4a2f9ddec2f7bc3837ffbcb3ca18dfd30b98ada61250545eddf3a | []
AccessCredentialProvisioner.java 12 335 c87b5ff721de2308b4a0ab0b9395a9dca19f8551d157f798acece0ec6688ab5b | []
JdbcAccessCredentialProvisioner.java 69 3103 06a4e3f9f09c2902eabb9d53161330b05b1fdfe3c7f2ae5821c129ed121a082e | []
AccessGrantPolicy.java 55 2915 81873e72e8332532af9469de5e2182f2988ca2f5ab5fdf31550470930796e8e2 | [ 1 file changed, 14 insertions(+), 3 deletions(-)]
AccessGrantPositionSummary.java 10 232 08fa864f1b0cada3309ea24503d159df5b1075c77e49a04ad75aa61ca1c156df | []
AccessGrantRequestRecord.java 35 934 b056364a8d6526123b1137c0ec7016240ca42083e9ca1050f905a4e55166ee89 | []
AccessGrantSummary.java 9 219 7648579e55d34871a4deea19ccde7eef74f592613d3cdf6015d7ca7dce7e1d96 | []
AccessGrantUserSummary.java 8 155 80fb7b5d47b0afdec3cda37e2c954950242603ed23e2d847f136b3b794636a7e | []
AccessUser.java 37 1042 717ebbd9d9ce56217a6045f20b42f47ec4aa76fdd2a14ac97c6b49e94387ceb1 | []
AuthorizationService.java 537 25639 990bce522fa847c5a26ddc23fd623477617d12db244b2fc09b87824cd758c062 | [ 1 file changed, 14 insertions(+), 23 deletions(-)]
EffectivePermissions.java 23 960 55bef627be66a02a45ddc4968a15c60b1e9d3357de7e3eaeb8a819f085e0247e | [ 1 file changed, 2 insertions(+), 2 deletions(-)]
PermissionDefinition.java 11 247 1d0ee72f430acf5a5dd8ff7f46af8abdcd630e84e9af83740ffe88c37aa792b4 | []
PermissionTemplate.java 37 1494 84749f393404f07ba97d8cde6686496532d6f9834b86c86273ab51cc167504ee | []
PermissionTemplateApplication.java 20 450 660d7a9be8d5fbc06c4872278b1ce7d5d5bf8d856eea130714f1c590ae254a97 | []
PermissionTemplateApplyResult.java 8 170 80134f3a3e8157cbf3c8af227a1f4bea9c4aa6c8986466fc31f70dd9d8a2d076 | []
PermissionTemplateItem.java 10 232 6f87855d883c18299f42e3703309c51cb77a1669587b6cc784a2853056c61b4f | []
PositionRoleTemplate.java 34 1284 1d09de44f1d488d0dbef17a4c62dd42cb411e61f82ec1ec61ac63c8238442b2b | []
RoleRecord.java 17 400 f0d867f56c06bbd8cfb886b6a713cbc7a85fb55e62109b28c6004e1f7ceae77b | []
RoleAssignmentRecord.java 7 125 2034337e12c8d9b38bb93205058b5844047148c5965932a259e9e17375f1ad0e | []
StaffLevelRecord.java 19 609 ed83f2f33532f072b88c2a271b6f39d7fdd20fbfe1ce0078634aaef66775c474 | []
StaffLevelAssignment.java 20 585 602c779aa27cc96d3be7794df7f772a353a8eca8b4a9d37445e66dfbb6b0af29 | []
UserPositionAssignment.java 14 302 015ec50b24fe7458a52131f7fe2e7446aab165000f5087d3f57cafc115ca3ff1 | []
UserPositionAssignmentResult.java 10 269 437d1c416d5027911eff9d34c421d93202dbe523f9658653406acdec9524775c | []
package-info.java 1 37 bf3835ba131ca4ad8c2e1ab5311cab18d2f9fe5755ecee9a311b47095af9efe8 | []
== 1. 改动处原文
package com.complex.platform.access;

// 文件版本：V0.02
import com.complex.platform.common.api.ApiException;
import java.util.Map;
import java.util.Set;

/**
 * M006-016：沿用主管复核策略表保存授权政策；查询不能覆盖政策值。
 * M032-001：库内该行自 V389 起带审批通用时限 {@code deadlineWorkingDays}；记录类须认得它（否则读不出整份政策），
 * 未跑 V389 的库没有该键故可为空；为空时不输出，保存走合并写，库内原值保留。
 */
public record AccessGrantPolicy(BoundedInt windowDays, BoundedInt positionThreshold,
                                BoundedInt userThreshold, Map<String, ReviewRule> riskReviews,
                                @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                                BoundedInt deadlineWorkingDays) {
    public AccessGrantPolicy {
        if (windowDays == null || positionThreshold == null || userThreshold == null
                || riskReviews == null || !riskReviews.keySet().equals(Set.of("low", "medium", "high", "critical"))
                || riskReviews.values().stream().anyMatch(java.util.Objects::isNull)) {
            throw ApiException.badRequest("INVALID_GRANT_POLICY", "须配置时间窗、频繁阈值及全部既有风险等级的审批规则");
        }
        riskReviews = Map.copyOf(riskReviews);
    }

    public AccessGrantPolicy(BoundedInt windowDays, BoundedInt positionThreshold,
                             BoundedInt userThreshold, Map<String, ReviewRule> riskReviews) {
        this(windowDays, positionThreshold, userThreshold, riskReviews, null);
    }

779:                UPDATE supervisor_review_policies SET parameters = parameters || CAST(:parameters AS jsonb), updated_at = now()
13: * <p>{@code wildcard} 现恒为 {@code false}：平台管理员不再是通配，其日常功能码已直接并入 {@code grants}
20:        boolean wildcard,
new EffectivePermissions( 构造点: backend/src/main/java/com/complex/platform/access/AuthorizationService.java:206:        return new EffectivePermissions(userId, List.copyOf(effective), denies, 
== 2. 发布口径实跑
migrations copied: V427__m009_project_profiles_and_associations.sql
 M backend/src/main/java/com/complex/platform/access/AccessGrantPolicy.java
 M backend/src/main/java/com/complex/platform/access/EffectivePermissions.java
 M backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
 M backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java
 M backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
b5 MVN_EXIT=0
   AccessControllerTests tests=13 errors=0 skipped=0 failures=0
   AccessJdbcIntegrationTests tests=25 errors=0 skipped=0 failures=0
   AccessServiceTests tests=54 errors=0 skipped=0 failures=0
   AuthorizationServiceTests tests=21 errors=0 skipped=0 failures=0
   M006JdbcRegressionTests tests=12 errors=0 skipped=0 failures=0
   M006SecurityRegressionTests tests=39 errors=0 skipped=0 failures=0
scratch removed
== 3. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T06:33:36Z
```

## 3. 三线审计

- 第一性原理线：用途/实际行为同 B1。修后：库内现行授权政策行能读出；保存不抹审批时限键；类注释不再声称不存在的 wildcard 行为。
- 顺从式线：52 判据逐项见 §4。
- 对抗式线（grilling）：未跑 V389 的库（无该键）→ 第 5 组件得 null、紧凑构造器不拒、序列化不输出该键，行为与修前一致；客户端显式带 `deadlineWorkingDays`→ 走 `BoundedInt` 校验后合并写入；客户端不带→ 库内原值保留；`riskReviews` 是顶层键，合并写按整键替换，而记录类要求四个风险等级齐全，不会留下半套；想通过本接口删掉某个键→ 做不到，也没有这个业务；`updateGrantPolicy` 的返回体是请求里的政策（客户端没带时限则返回体里也没有），再 GET 即见库内全量——返回体与库内差一个可选键，不立项；对外 JSON 多一个可选字段，前端不认识的字段会被忽略；4 参构造器仍可用（M006JdbcRegressionTests 12 绿）。无新缺陷、无添加项、无 HTML 问题。

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
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | 适用｜「不同人只看自己该看的」：各带参列表与分页均带项目或商户谓词；发布口径（origin/main 版）与接口契约一致｜通过（主工作区未发布版另有 W-1…W-4，已按 D-029 登记到 2026-09-13 未发布工作流，不在本模块发布口径内） |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | 无本面｜本模块是权限仓储实现、最终权限计算服务与记录类，不承载、不展示、不声称「相互关系：为什么这些子系统必须连在一起」的对象、状态或界面文字 |
| BSM-120 已确认规则和实施前输入项 | 无本面｜本模块是权限仓储实现、最终权限计算服务与记录类，不承载、不展示、不声称「已确认规则和实施前输入项」的对象、状态或界面文字 |
| GEN-001 必要性与最小性 | 适用｜25 文件；记录类无多余成员；`EffectivePermissions.wildcard` 恒为 false 已在类注释如实写明（002）｜通过 |
| GEN-002 可执行正确性 | 适用｜发布口径一次性 worktree 实跑六类测试全绿（§2 探针 2：13/25/54/21/12/39）；库内现行授权政策行可读、保存不抹审批时限键（001）｜通过（主工作区未发布版另有 W-1…W-4，已按 D-029 登记到 2026-09-13 未发布工作流，不在本模块发布口径内） |
| GEN-003 测试有效性 | 适用｜新增真库回归 `storedGrantPolicyParsesAndSavingKeepsDeadlineWorkingDays` 在修前红（Unrecognized field）、修后绿；内存政策 JSON 已与库内实态一致，修前 11 条用例随之倒下、修后全绿｜通过 |
| GEN-004 信任边界校验 | 适用｜`AccessGrantPolicy` 构造即校验上下限与风险等级闭集；`BoundedInt.checked` 校验查询时间窗；`findTemporaryGrant` 对非 UUID 的 id 会在 `UUID.fromString` 抛 IllegalArgumentException（OBS-2，id 正常来自列表，不立项）｜通过 |
| GEN-005 授权与最小权限 | 适用｜显式拒绝优先（L77-79）；平台管理员默认角色里的高危码不当逐项授权（`isExplicitOrDailyGrant`）；最高权限独立识别；`holdsPermissionViaRole` 查询异常保守返回 false；主工作区版的 scope 方法不再因平台管理员身份放开（注释引七类红线第四条）｜通过 |
| GEN-006 数据完整性 | 适用｜时间窗判定与到期清理同 B1；授权政策保存改为 jsonb 合并写，客户端没带的键保持库内原值｜通过 |
| GEN-007 并发与幂等 | 适用｜`lockGrantAuthorizationFacts` 以 SHARE MODE 锁 8 张授权定义表（源码注明上限与升级路径）；`findPositionRoleTemplateById`/`findGrantRequestById` FOR UPDATE、`findPositionRoleTemplate` FOR SHARE；`saveGrantRequest` 非 pending 只许从 pending 改一次；单次消耗 UPDATE 带 `consumed = FALSE`｜通过 |
| GEN-008 接口与模式一致性 | 适用｜记录类形状与库内政策 JSON 一致（5 个键，第 5 个可空）；发布口径 Jdbc 与 `AccessRepository` 接口契约一致｜通过（主工作区未发布版另有 W-1…W-4，已按 D-029 登记到 2026-09-13 未发布工作流，不在本模块发布口径内） |
| GEN-009 错误处理与恢复 | 适用｜影响行数不为 1 即抛中文 IllegalStateException；`holdsPermissionViaRole`/`levelRankOf`/`*Quietly` 的降级均为保守方向；主工作区版 `currentProjectScopeQuietly` 名为 Quietly 却会抛（OBS-1）｜通过 |
| GEN-010 安全与敏感信息 | 适用｜口令不落日志、不回显；bcrypt 强度 10、SecureRandom；凭证状态只有 active/disabled 两态映射｜通过 |
| GEN-011 可观测与审计证据 | 适用｜到期与单次消耗留痕见 PGM-001；失败留痕在服务层（M031），事务内丢失已登记 XM-M031-B（→M092）｜通过 |
| GEN-012 资源与性能边界 | 适用｜角色列表批量取权限码（L318-337 消除 1+N）；`findRoleById` 单条仍逐条取码（合理）；`findPermissionEffects` 每次读先做本人到期清理（源码注明 ponytail 上限）｜通过 |
| GEN-013 构建配置与依赖 | 适用｜只依赖 Spring JDBC、Spring Security Crypto、Jackson 注解、JDK｜通过 |
| GEN-014 数据库迁移正确性 | 无本面｜本模块无迁移；SQL 引用的 19 张表与写路径 198 列在 complex_test 全部存在（探针 3；`requested_group` 是 CTE 名不是表） |
| GEN-015 界面可用性与可访问性 | 适用｜面向操作者的异常文案均为中文；`PERMISSION_DENIED` 文案带权限码（机器标识）｜通过 |
| GEN-016 文档可验证性 | 适用｜`EffectivePermissions` 类注释已改为现状（wildcard 恒 false、日常码并入 grants、denies 优先）；`AccessGrantPolicy` 新注释与实现一致；其余注释同 B1｜通过 |
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

## 5. 已定稿 001、002 现态

全部已修（B3-r1），B4-r1 双 PASS；修复队列「是」= 0。W-1…W-4 登记未变。

## 6. 本次 Findings

无新增。

## 7. 观察（登记）

- 同 B2-r1 定稿的登记项；另记：`updateGrantPolicy` 返回体不含客户端没带的可选键（库内仍有）。

## 8. 结论

席甲 B5-r1 票：**PASS**（发布口径）。25 文件全读、52 判据逐项、取证脚本实跑复核；未改任何已跟踪文件；落盘前未读 Grok 本次正文。
