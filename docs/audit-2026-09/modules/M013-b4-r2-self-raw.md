CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M013 B4-r2 Codex 修复内容与归因审计

版本：V0.01

## 结论
修复内容：FAIL。逐行归因：PASS。模块仍 IN_PROGRESS，B3累计2/3；尚未读取本次Grok raw/CLI正文。16处业务说明修改正确完成，但本轮新升版造成绑定设计L1与L3当前版本矛盾，见CX-B4-01。Grok未交付前不统一路由，不修任何文件，不把6项转否。

## 范围和完整性
before 75941c7d111b1a88313417197a581a48d917915c
after ad9c27c34fcf3032cf42e330821e14f26d5e510e
完整无过滤命令：git diff --binary 75941c7d111b1a88313417197a581a48d917915c ad9c27c34fcf3032cf42e330821e14f26d5e510e
33677字节；SHA256 6b70b54e324d51022c3fbc988d7172548f1114907fb710111f9b4214631c2055；4文件16hunk，共+24/-20。前后由不存在的GIT_INDEX_FILE临时索引git add -A / git write-tree取得，tracked/staged/untracked全覆盖。真实索引SHA 1657ef91568fc6c00e7fd66f58f4c00461260651323d2ecbf616238602b973a2未变。两个快照之间仅4文件写入，无台账或报告写入。本次独立复审直接读取实际完整diff及源字节；12项HTML/直接依赖SHA复核与B1全文读取相同。
- docs/business-entity-binding-development-design-v0.1.md：153行，SHA256 04e4111bc47ad2c41d4f1188996f7e56b8a2dd875b1b0128b4468d7efcf8e674
- docs/group-governance-rebuild-prd-v1.0.md：115行，SHA256 3f11679ea2a13ee3aa0a0299cb611e43168190453390c40c8018c7e78b821ee5
- docs/platform-governance-acceptance-pack-v0.2.md：290行，SHA256 6fb5d6022519244bee5704014474daafab7a30521e7a7f020c215818d0749603
- docs/platform-governance-prd-v0.1.md：970行，SHA256 c32e883636b97cb136a4478d9dd51c6d2e1656b437e4bd98ced094ee0fe32333

## 第一性原理与内容审查
用途：四份文档承接主体绑定/集团/授权/验收的既有HTML要求；修改只恢复现行事实，不新增业务规则。实际验证：文档类用下方可复现静态命令输出；没有重跑Java、HTTP、浏览器或数据库，引用现有测试仅作断言语义证据。未验证前提：原B2把文件版本理解为只有标题，漏掉绑定设计L3明确“版本/最后更新/现行”元数据，现由正常读页即复现推翻；不能以双B2同意为事实替代。

### M013-011：内容核对通过
冻结ID：BEQR-143、PGM-026、GEN-002、GEN-008、GEN-016、GEN-018。
实际证据：PlatformService:756–866：创建先锁商户，requireUnfinishedBindingSlot、requireUnchangedFinancialSubjects；未来 effective_from 写 inactive 且不投影商户三主体，已到日期写 active 才同步。945–960：已有非空三主体不得经此接口替换；ACTIVE_BINDING_EXISTS 包括 active 或 inactive+effective_to为空。change 828–837：BINDING_PERIOD_ENDED / BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW / BINDING_NOT_EFFECTIVE。JdbcPlatformRepository:585–601 对应SQL；PlatformServiceTests:860–942现有反例（本次只读，未冒称重跑）。
与当前B3对应：docs/business-entity-binding-development-design-v0.1.md:80；docs/business-entity-binding-development-design-v0.1.md:90；docs/business-entity-binding-development-design-v0.1.md:99；docs/business-entity-binding-development-design-v0.1.md:117；docs/business-entity-binding-development-design-v0.1.md:118。修改后的断言与上述当前分支相符；未补后台缺口、未改HTML。该内容通过不覆盖文件版本元数据新缺陷。

### M013-012：内容核对通过
冻结ID：BEQR-152、PGM-041、GEN-005、GEN-008、GEN-016、GEN-018。
实际证据：AnalyticsController:119–125仅 requirePermission、requirePlatformScope，再 groupOverview(groupBy)。AuthorizationService:482–498只查平台管理员桥、外包、merchantId，不检查project/organization；AnalyticsService:774–830读取全部project快照/平台回退，未接调用者project。无商户归属的项目员工获analytics.group.view后，当前链没有本项目过滤。AnalyticsServiceTests:286–300明确无actor输入时返回project_1及project_2。
与当前B3对应：docs/group-governance-rebuild-prd-v1.0.md:14；docs/group-governance-rebuild-prd-v1.0.md:95；docs/group-governance-rebuild-prd-v1.0.md:110；docs/platform-governance-acceptance-pack-v0.2.md:181；docs/platform-governance-prd-v0.1.md:657。修改后的断言与上述当前分支相符；未补后台缺口、未改HTML。

### M013-013：内容核对通过
冻结ID：PGM-041、GEN-002、GEN-008、GEN-016。
实际证据：AccessService:879–901明确dailyTemporary=approvalRequired.isEmpty() && temporary_permission；仅非日常临时且无审批必需码才拒绝，日常临时使用permission_grant_l1。AccessServiceTests:1430–1453已有dailyTemporaryPermissionApplicationEntersSupervisorChain，断言pending与一级链。PGM-041：员工可申请临时权限，按层级对应主管审批。
与当前B3对应：docs/group-governance-rebuild-prd-v1.0.md:71。修改后的断言与上述当前分支相符；未补后台缺口、未改HTML。

### M013-014：内容核对通过
冻结ID：BSM-007、PGM-041、GEN-005、GEN-008、GEN-016。
实际证据：BSM-007平台管理员仅自动拥有日常功能；高危等逐项授予。AuthorizationService:178–206实际返回wildcard=false，daily集合扣denies；AccessService:2060–2076使用requireActorCanGrantImmediate，808–812只对立即生效普通码检查持权，待批高危不要求配置人先持有。assignUserPositionDirect:1464–1527混合模板保存普通部分，高危交holdHighRiskPositionPermissions，而非整单拒绝。
与当前B3对应：docs/group-governance-rebuild-prd-v1.0.md:34；docs/group-governance-rebuild-prd-v1.0.md:38。修改后的断言与上述当前分支相符；未补后台缺口、未改HTML。

### M013-015：内容核对通过
冻结ID：BSM-007、GEN-008、GEN-016。
实际证据：本文:7称迁移/代码/接口为当前实现证据。V170:100确为历史high；V394:13明确UPDATE apply风险档为low，:14授予staff_self；当前所有迁移中此码无后续反改。原初值可保留作历史，不能未标注地当现行值。
与当前B3对应：docs/group-governance-rebuild-prd-v1.0.md:90。修改后的断言与上述当前分支相符；未补后台缺口、未改HTML。

### M013-016：内容核对通过
冻结ID：PGM-041、GEN-002、GEN-008、GEN-016。
实际证据：AccessController:158–165仍接收这些参数但透传；AccessService:1044–1055任一threshold非null或window非后台值即QUERY_POLICY_OVERRIDE_FORBIDDEN；实际阈值/窗口从grantPolicy读取。正常按文档示例填写数字即被拒绝，不是极端情形。
与当前B3对应：docs/group-governance-rebuild-prd-v1.0.md:77；docs/group-governance-rebuild-prd-v1.0.md:83。修改后的断言与上述当前分支相符；未补后台缺口、未改HTML。

## 对抗式完整审查
1. 未来日期是否现在同步结算主体：现文明确inactive/不提前同步，不等自动到日启用；期间结束/生效中停用/已有财税替换限制准确。未完成槽位的服务校验与DB仅active唯一索引区分，没有杜撰DB约束。
2. 通过requirePlatformScope是否即可跨项目：现文否定该推断，保留实际后台缺口017→M348、依赖M032；V394审批属性闭合不再冒充结果隔离闭合。
3. 日常临时申请或混合岗位是否全部被拒：现文分清每日临时仍主管链、普通模板立即和高危挂起；不会把直接角色授权、直接模板应用变成免审。
4. URL照抄是否拒绝：不再给summary阈值参数；兼容window仅等于后台值，阈值即传即拒绝有直接Service证据。
5. 版本与完成声明是否可追溯：历史日志保持历史，不因日期过期改写旧记录；但L3明确现行元数据不是历史，标题更新后两处当前版本不同，必须作为新工程缺陷统一B2。

## 本次全部发现
### CX-B4-01：绑定设计的当前版本和更新时间未随本轮更新
位置：docs/business-entity-binding-development-design-v0.1.md:1,3。
原句（完整）：
> # 四类主体唯一绑定开发设计 V0.10
> > 版本：V0.09 ｜ 最后更新：2026-09-11 ｜ 状态：现行开发设计
正常触发：打开同一文件页首，两处都描述当前版本却分别为V0.10/V0.09；本次确实2026-09-14当地日修改而“最后更新”仍旧。这是本轮标题hunk和相邻元数据不一致，GEN-016明确覆盖版本与状态，GEN-018要求命令可复现。
分类依序：不与HTML业务原文冲突；版本元数据不属于业务细化；无实体/枚举/权限/默认值/流程新发明，归技术性，有工程缺陷。没有添加人闸、无需新增判据。第一性依据类型4：下列版本比对命令输出same=false，和已取得after tree本轮差异；用户版本铁律是独立明示约束。
最小拟修：下一次B3把本文件标题当前V0.10→V0.11，并把完整元数据改为“> 版本：V0.11 ｜ 最后更新：2026-09-14 ｜ 状态：现行开发设计”。版本不可倒退/抹去V0.10；不改业务段落、不修改历史日志和HTML。影响仅使文档身份/更新时间一致。
该缺陷目前无正式ID，须等两席B4收齐后回B2分配；B4失败不清既有011–016是。归因均成立，无越界故不按§5闸3回滚；不能悄悄修版本。

## 全部16 hunk逐块归因
| 文件 | hunk/位置 | 归因ID | 理由 |
|---|---|---|---|
| binding | 1 L1 | M013-011 | 该文件5处改动的强制升版，B2明列；漏元数据属内容缺陷不是无归因 |
| binding | 2 L77 | M013-011 | 创建/状态/未完成槽位现状 |
| binding | 3 L87 | M013-011 | 新建流程现状 |
| binding | 4 L96 | M013-011 | 状态接口守卫与未完成审批 |
| binding | 5 L114 | M013-011 | 两行API现状 |
| group | 1 L1 | M013-012–016 | 同批该文件升版 |
| group | 2 L11 | M013-012 | 总览隔离错误结论 |
| group | 3 L31 | M013-014 | 2处wildcard与混合岗位错误 |
| group | 4 L68 | M013-013、015、016、012 | L71=013；77/83=016；90=015；95=012，其他仅上下文 |
| group | 5 L107 | M013-012–016 | L110=012；追加现有日志与空行承载本批012–016 |
| acceptance | 1 L1 | M013-012 | G7修正的升版 |
| acceptance | 2 L178 | M013-012 | G7分清审批属性/范围隔离 |
| acceptance | 3 L197 | M013-012 | 既有日志表记录本次修正 |
| PRD | 1 L1 | M013-012 | 已确认15修正的升版 |
| PRD | 2 L654 | M013-012 | 限定闭合对象 |
| PRD | 3 L967 | M013-012 | 既有日志表记录本次修正 |
全部新增/删除行均归B2 V2.25六个YES。没有借017改后台或修改旧001–010。

## 实际检查命令与输出
通过python3 -（SSH stdin）读取四源，逐条assert定稿new在实际文件；hashlib.sha256对12个直接依赖/HTML与B1指纹比对；re提取L1的V数字与L3“> 版本：”，打印same；git diff --check两树；解析完整diff所有@@。
```text
DEPENDENCY_HASH_CHECK 12 PASS
CURRENT_VERSION_COMPARE {"title": "V0.10", "metadata": "V0.09", "same": false}
EXACT_REPLACEMENTS 16 PASS
DIFF_CHECK_EXIT 0  
HUNK docs/business-entity-binding-development-design-v0.1.md 1 @@ -1,4 +1,4 @@
HUNK docs/business-entity-binding-development-design-v0.1.md 2 @@ -77,7 +77,7 @@
HUNK docs/business-entity-binding-development-design-v0.1.md 3 @@ -87,7 +87,7 @@
HUNK docs/business-entity-binding-development-design-v0.1.md 4 @@ -96,7 +96,7 @@
HUNK docs/business-entity-binding-development-design-v0.1.md 5 @@ -114,8 +114,8 @@
HUNK docs/group-governance-rebuild-prd-v1.0.md 1 @@ -1,4 +1,4 @@
HUNK docs/group-governance-rebuild-prd-v1.0.md 2 @@ -11,7 +11,7 @@
HUNK docs/group-governance-rebuild-prd-v1.0.md 3 @@ -31,11 +31,11 @@
HUNK docs/group-governance-rebuild-prd-v1.0.md 4 @@ -68,31 +68,31 @@
HUNK docs/group-governance-rebuild-prd-v1.0.md 5 @@ -107,7 +107,9 @@
HUNK docs/platform-governance-acceptance-pack-v0.2.md 1 @@ -1,4 +1,4 @@
HUNK docs/platform-governance-acceptance-pack-v0.2.md 2 @@ -178,7 +178,7 @@
HUNK docs/platform-governance-acceptance-pack-v0.2.md 3 @@ -197,6 +197,7 @@
HUNK docs/platform-governance-prd-v0.1.md 1 @@ -1,4 +1,4 @@
HUNK docs/platform-governance-prd-v0.1.md 2 @@ -654,7 +654,7 @@
HUNK docs/platform-governance-prd-v0.1.md 3 @@ -967,3 +967,4 @@ P0 阶段的重点是把“谁、在哪、凭什么、谁能操作、谁负责
ROW | M013 `综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第1段` | IN_PROGRESS | 2 | 2 | [历史B5-r1双PASS](modules/M013-entity-topology-binding-docs-seg1.md) | 0 | 2026-09-14 B3-r2完成；B4-r2双席审计中；完整before/after见顶部；6项仍是、017跨模块否 |
CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
LOCK-AMEND-COUNT: 1
LEDGER V4.03 743312 edd4dc35d1f507996a660707f69bf32ec4d85c1e7265c9c1d26613b445f27930
```
B3前后四值（actual/sidecar/ledger/本次前B1 Grok最后独占锁行）：b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a / b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a / b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a / b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。修改后相同，LOCK-AMEND-COUNT1。
技能：既有已读requesting-code-review仅派Grok；systematic-debugging/diagnosing-bugs以原句→源码/版本probe核真；grilling自问自答不用第三席；ponytail-review最小文档修正；verification-before-completion在本次检查不全绿时明确FAIL；writing-for-agents按固定raw。文档豁免执行行为TDD，不造测试。

## 完整binary diff（原样）
```diff
diff --git a/docs/business-entity-binding-development-design-v0.1.md b/docs/business-entity-binding-development-design-v0.1.md
index ad294981..c55e3120 100644
--- a/docs/business-entity-binding-development-design-v0.1.md
+++ b/docs/business-entity-binding-development-design-v0.1.md
@@ -1,4 +1,4 @@
-# 四类主体唯一绑定开发设计 V0.09
+# 四类主体唯一绑定开发设计 V0.10
 
 > 版本：V0.09 ｜ 最后更新：2026-09-11 ｜ 状态：现行开发设计
 > 业务最终解释权：`business-system-map-for-operations-v0.1.html`、`platform-governance-map-for-operations-v0.1.html`、`business-enum-quick-reference-v0.1.html`。代码、迁移和接口只说明当前实现，不得覆盖三份 HTML。
@@ -77,7 +77,7 @@
 
 唯一约束 `uq_business_entity_bindings_active_merchant` 保证每个商户至多一条 `active` 记录。
 
-当前没有 `project_id`、`binding_no`、`version`、`created_by`、`reviewed_by`、`reviewed_at`，也没有 `business_entity_binding_usages` 表。当前创建接口直接写 `active`，已有 active 时拒绝；状态变更接口只改启用、停用或暂停，尚未实现 HTML 要求的主体权责配置审批、新旧关系切换和下游统一引用闭环。
+当前没有 `project_id`、`binding_no`、`version`、`created_by`、`reviewed_by`、`reviewed_at`，也没有 `business_entity_binding_usages` 表。当前创建按 `effective_from` 相对 UTC 当天判断：未来日期写 `inactive`，不提前同步商户三主体；已到日期写 `active` 并同步。已有生效或未结束待生效绑定时拒绝重复；已有商户三主体不得经此接口替换。状态变更还受生效期间及财税变更守卫限制（见 §5）。完整主体权责配置审批、新旧关系切换和下游统一引用闭环仍未实现。
 
 ## 4. 目标流程与当前差距
 
@@ -87,7 +87,7 @@
 2. 转商户时，经营对象、运营主体、合同主体、结算主体默认可指向同一公司。
 3. 经适用审批生效后，下游合同、结算、权限、看板和第三方接入引用同一绑定。
 
-当前差距：`POST /api/v1/platform/business-entity-bindings` 直接创建 active 记录，没有草稿、审批和复核状态。
+当前差距：`POST /api/v1/platform/business-entity-bindings` 按生效日期创建 `active` 或待生效 `inactive` 记录，只有生效时才同步商户三主体；待生效记录不是审批草稿，尚无完整草稿、审批和复核流程。
 
 ### 4.2 主体或权责调整
 
@@ -96,7 +96,7 @@
 3. 审批后只影响生效时间之后的新业务；历史合同、订单、票据、结算和快照保持原值。
 4. 下游新业务切到新绑定，历史引用继续可追溯。
 
-当前差距：`POST /api/v1/platform/business-entity-bindings/{id}/change` 只改变现有记录状态，不能提交新的三主体组合，也没有上述审批链和历史版本切换。
+当前差距：`POST /api/v1/platform/business-entity-bindings/{id}/change` 只接受状态动作并校验生效期间；生效中绑定不得直接停用或暂停，已结束期间不得重开，未到生效日不得启用或结束。该接口不能提交新的三主体组合，也没有上述审批链和历史版本切换；到达生效日期后可调用启用，不表示系统会自动启用。
 
 ### 4.3 准入与引用
 
@@ -114,8 +114,8 @@
 | `/api/v1/platform/business-entities` | GET | 真分页查询主体档案 |
 | `/api/v1/platform/business-entities` | POST | 创建主体档案 |
 | `/api/v1/platform/business-entity-bindings` | GET | 真分页查询四类主体绑定 |
-| `/api/v1/platform/business-entity-bindings` | POST | 直接创建 active 绑定；已有 active 时返回 `ACTIVE_BINDING_EXISTS` |
-| `/api/v1/platform/business-entity-bindings/{id}/change` | POST | 将状态改为 active、inactive 或 suspended，并填写原因 |
+| `/api/v1/platform/business-entity-bindings` | POST | 未来 `effective_from` 写 `inactive` 且不提前同步商户三主体；已到日期写 `active` 并同步。已有 `active` 或 `inactive` 且 `effective_to` 为空的绑定返回 `ACTIVE_BINDING_EXISTS`；替换商户既有三主体返回 `BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW` |
+| `/api/v1/platform/business-entity-bindings/{id}/change` | POST | 接受 active、inactive、suspended 并必填原因；已结束期间返回 `BINDING_PERIOD_ENDED`，生效中绑定停用/暂停返回 `BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW`，未到生效日返回 `BINDING_NOT_EFFECTIVE`。启用还检查商户、三主体、未完成绑定槽位及既有财税主体不被替换 |
 
 当前不存在 `/active?businessObjectId=` 和 `/{id}/usages` 两条接口，也不存在主体完整 CRUD。文档不得调用不存在的端点。
 
diff --git a/docs/group-governance-rebuild-prd-v1.0.md b/docs/group-governance-rebuild-prd-v1.0.md
index e1dfa2e9..78485824 100644
--- a/docs/group-governance-rebuild-prd-v1.0.md
+++ b/docs/group-governance-rebuild-prd-v1.0.md
@@ -1,4 +1,4 @@
-# 集团治理重建 PRD（V1.01·补断点）
+# 集团治理重建 PRD（V1.02·补断点）
 
 > **锚点口径（2026-08-05 R7·GAP-36 锚点重建）**：本文引用三份业务 HTML（BSM＝`business-system-map-for-operations-v0.1.html`／PGM＝`platform-governance-map-for-operations-v0.1.html`／ENUM＝`business-enum-quick-reference-v0.1.html`）时，一律采用**「条目名主锚点 + 行号辅助」**；标注「实测 L…」的行号为 **2026-08-05** 按现行 HTML `sed -n 'Np'` 逐条实测值，**以条目名为主锚点**，行号漂移不影响条目效力。变更日志章及既有「锚点复核」块内的历史行号为当时记录，一律不改写。
 
@@ -11,7 +11,7 @@
 - 不重写 RBAC：继续复用 `permissions`、`roles`、`role_permissions`、`user_roles`、`permission_templates`、`permission_template_items`、`permission_template_applications`、`temporary_permission_grants`。
 - 不重写主管复核：高危授权审批复用 `SupervisorReviewService` 与 `supervisor_review_decisions`。敏感动作可要求上一级、上两级或最高权限，这是敏感度要求，不是发起人可选档位，也不等于指定角色档或合同单级。发起单据时只需指定最终审批人，系统按组织层级自动补全。当前实现代码值 `one_level_up` / `two_levels_up` / `top_authority` 仅为实现证据，不取得业务效力。
 - 本轮新增的是治理闭环，不是新权限模型：平台管理员配置岗位模板，HR/平台按用户职位定岗，系统自动授予角色并套模板；高危权限授出前可被审批闸拦住。
-- 集团下钻与项目公司级隔离按角色分级共存，不冲突：**平台/集团级角色**（`requirePlatformScope()` 放行者）可跨项目公司汇总下钻；**项目公司管理员**只看本项目公司；**商户用户** scope 一律 fail-closed 403。`GET /api/v1/analytics/group-overview` 除校验 `analytics.group.view` 外，还调用 `requirePlatformScope()`，商户 scope 调用 fail-closed（返回 403）。即「集团下钻」只对平台/集团级角色开放，对项目公司/商户仍按其 scope 收敛。
+- 集团下钻的业务边界：只有项目之上的获授权岗位可跨项目汇总，项目公司管理员限本项目，商户用户不得查看集团总览。**当前实现待对齐**：`GET /api/v1/analytics/group-overview` 校验 `analytics.group.view` 和 `requirePlatformScope()`；后者拦截商户及外包范围，但未按项目级调用者过滤汇总。不得把通过该守卫等同于已认定的平台/集团级角色；项目范围隔离尚未闭环，见 §4。
 - 不做：多套审批流引擎、按业务线重造授权模型、把职位等同于权限。职位只驱动默认角色/模板，最终有效权限仍由 RBAC + 临时授权 + 显式拒绝计算。
 
 ## 1. 业务对象与状态枚举（当前实现证据；锚点为枚举 HTML §九平台治理，不是 ENUM §十地产运营）
@@ -31,11 +31,11 @@
 **业务规则**（GOV「角色装配（已确认）」两支与五类高危以 HTML 为准。本节表名、唯一约束、接口取值与回退值为当前实现证据，三份业务 HTML 零命中，不取得效力。）
 - 平台管理员配置 `position_role_templates`：`position_code + staff_level_id` 绑定一个 `role_id` 与一个 `permission_template`。这对应业务口径“职位/层级对应哪个模板”。
 - 【实现证据/不取得效力】活跃模板唯一约束：`COALESCE(organization_id,'') + position_code + staff_level_id` 在 `status='active'` 下唯一——三份业务 HTML 零命中，不得立法。
-- 创建/更新职位模板必须是平台管理员，并且复用 `requireActorCanGrant`：模板里角色权限和权限模板条目都不能超出操作者自身有效权限，平台管理员 wildcard 例外。
+- 创建/更新职位模板必须是平台管理员，服务使用 `requireActorCanGrantImmediate` 检查模板中会立即生效的普通权限不得超出操作者自身有效权限；须审批的高危权限不要求配置者预先持有，实际授出仍须审批。平台管理员没有 wildcard 全权限例外，显式拒绝继续有效。
 - 给用户定岗 `assignUserPosition` 时，系统按目标用户组织、职位、职级查模板，写三类结果：`staff_level_assignments`、`user_position_assignments`、`user_roles`，并套用 `permission_templates`。
 - 跨组织闸保持原 RBAC 规则：非平台管理员不得给其他组织用户定岗；角色或模板若带组织归属，也必须与目标用户组织一致。
 - `access.role.assign` 独立为高危权限，V169 设为 `risk_level=high`、`requires_approval=true`，避免把“配角色”混在普通 `access.role.manage` 里。
-- 直接分配/取消角色同样复用 `requireActorCanGrant` 和跨组织闸；角色装配只要包含五类强制高危权限或其他 `requires_approval=true` 权限，就必须被审批闸拦截并走 §3 申请，不受可关闭总开关影响。
+- 直接分配/取消角色继续使用 `requireActorCanGrant` 和跨组织闸；直接授出须审批的高危权限仍被拦截并走 §3 申请。同范围定岗按普通权限与待批高危权限分别处理：普通部分随保存生效，高危部分挂起进入审批链，通过后才释放，不因模板含高危权限而整单拒绝；岗位规则不推广为其他授权入口的免审例外。
 
 **关键端点（实际 Controller）**
 - `GET /api/v1/access/position-role-templates`
@@ -68,31 +68,31 @@
   - **第 5 类 税费规则与税费调整（2026-08-04 对齐平台治理地图/枚举速查表）**：所有税费规则和税费调整均须经过**财务审核、税务审核、法务审核**，再由**顶层平台公司的老板最终确认后生效**；**任一环节驳回均不得生效**；**提交人不得审批自己提交的调整**；全部审批意见和时间必须留痕。**税率、税目不属于普通后台配置项**，不得按普通配置项直接改动。
     - **实现待对齐（2026-08-04 登记；2026-09-11 M013 核对更新）**：当前实现已有薪资个税档权限码 `staff.tax_bracket.manage`（V217，`risk_level=high`、`requires_approval=true`；属薪资个税档，不是本条第 5 类「税费规则与税费调整」）与 `SupervisorReviewService` 的 `finance.tax_rule.adjust` 复核动作（策略 tax_rule_adjustment_l3，不是 `permissions` 行），但本条 HTML「税费规则与税费调整」在 `permissions` 侧尚无 `finance.tax_*` 权限码落点，**记为实现待对齐（范围收窄为该权限码）**，按事实源优先级**不反向改写 PRD**，留待实现批次补齐。
 - 申请类型当前服务支持三类：`user_role`、`user_position`、`temporary_permission`。DB CHECK 预留 `role_permission`，但 `AccessService` 当前不支持该分支。
-- 申请时服务先解析目标授权会授出的权限码，只允许包含 `requires_approval=true` 的授权申请；若没有审批必需权限，返回 `GRANT_APPROVAL_NOT_REQUIRED`，应走直接授权。
+- 申请时服务先解析目标权限码。角色/岗位授权申请若不含 `requires_approval=true` 的权限，返回 `GRANT_APPROVAL_NOT_REQUIRED`，应走直接授权；临时权限申请无论日常或高危均进入主管审批链，其中仅含日常权限的申请使用当前一级主管策略，不直接授出。
 - 审批链不由发起人选择档位，也不按风险值自动映射成可选档或指定角色。发起单据时只需指定最终审批人，系统按组织层级自动补全。当前实现把 `critical`/`high`/其他 写成 `permission_grant_l3`/`l2`/`l1` 与 `top_authority`/`two_levels_up`/`one_level_up`，属实现证据，不是业务可选档。
 - 授权申请人与审批人必须职责分离；同一人审批自己的授权申请时一律拒绝并留痕。
 - 【实现证据/不取得效力】流程：申请创建 `access_grant_requests(status=pending)` 并调用 `SupervisorReviewService.blockForAdmission` 生成 `review_id`；审批端调用 SupervisorReview `approve` 后，批准则应用角色/定岗/临时权限并写 `applied`，驳回写 `rejected`——三份业务 HTML 零命中上述表名、状态流程与服务名，不得立法。
 - 临时权限必须有 `expires_at`；可带 `valid_from`、`single_use`。V170 扩展 `temporary_permission_grants` 的 `status`、`valid_from`、撤销、单次消耗字段；有效权限查询按有效期和消耗状态过滤。
 - 角色装配按治理 HTML「角色装配（已确认）」两支执行。同商户同项目：保存岗位、职责或同一商户、同一项目范围内的所属组织变更时，系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效，同一商户、同一项目范围内的普通模板权限随保存立即生效。保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批，审批通过后方可生效；属于既有高危权限的，不得因处于岗位模板内而免除授出前审批；超出新模板范围的增权保持未生效，审批通过后方可生效。在上述变更触发重算时，需保留的员工单独授权必须重新校验新范围，超出新范围则暂停并重新审批。本规则不把仅职级变化、责任范围变化或任意手工模板应用列为触发条件（DEC-R8-91）。
-- 智能总结：`grantSummary` 统计窗口内临时权限申请。某职位/层级/权限组合达到阈值时返回“建议转固定权限”；某用户频繁申请时返回异常总结供复核。
+- 智能总结：`grantSummary` 统计窗口内临时权限申请。某职位/层级/权限组合达到阈值时返回“建议转固定权限”；某用户频繁申请时返回异常总结供复核。统计窗口和频繁阈值从后台授权政策读取，查询不得覆盖；兼容的 `window_days` 仅在等于后台值时接受，两个阈值参数只要传入即返回 `QUERY_POLICY_OVERRIDE_FORBIDDEN`。
 
 **关键端点（实际 Controller）**
 - `POST /api/v1/access/grant-requests`
 - `GET /api/v1/access/grant-requests?window_days=...`
 - `POST /api/v1/access/grant-requests/{id}/review`
-- `GET /api/v1/access/grant-requests/summary?window_days=...&position_threshold=...&user_threshold=...`
+- `GET /api/v1/access/grant-requests/summary`（使用后台授权政策，不传阈值参数）
 - `POST /api/v1/access/temporary-grants`
 - `POST /api/v1/access/temporary-grants/{id}/revoke`
 
 **迁移 / 表 / 权限**
 - 迁移：`V170__access_grant_requests.sql`
 - 表：`access_grant_requests`、`temporary_permission_grants`；批量更新 `permissions.requires_approval`
-- 权限：`access.grant_request.view`（medium）、`access.grant_request.apply`（high）、`access.grant_request.review`（critical、requires approval）、既有 `governance.supervisor_review.approve`
+- 权限：`access.grant_request.view`（medium）、`access.grant_request.apply`（low；V170 原 high，V394 已调整）、`access.grant_request.review`（critical、requires approval）、既有 `governance.supervisor_review.approve`
 - UI：`ADM-ACC-003` 访问治理工作台
 
 ## 4. 集团下钻对标
 **业务规则**（可见范围与平台/集团级角色以 GOV 为准。本节 `groupBy`、缺主体归并与快照回退值为当前实现证据，三份业务 HTML 零命中，不取得效力。）
-- 集团视图能看什么由职位模板最终授出的权限决定；只有拥有 `analytics.group.view` **且通过 `requirePlatformScope()`（平台/集团级角色）**的职位/角色/临时授权可见集团总览。项目公司管理员即便持有该权限，也因 scope 收敛到本项目、无法跨项目公司下钻；商户用户 fail-closed 403。
+- 集团视图的业务可见范围仍按治理 HTML 的项目隔离与岗位授权执行。**当前实现**只校验 `analytics.group.view` 和 `requirePlatformScope()`，再按 `groupBy` 汇总全部项目快照；无商户归属的项目级调用者通过守卫后，结果没有按其项目收敛。该范围缺口待后台实现模块处理，不能据本 PRD 判为已经隔离。
 - 【实现证据/不取得效力】端点 `GET /api/v1/analytics/group-overview` 接受 `groupBy=project|entity`。`project` 按综合体项目聚合；`entity` 通过 `projects` 关联 `merchants.business_entity_id` 派生，缺主体归 `entity_unassigned`——三份业务 HTML 零命中 `groupBy` 与 `entity_unassigned`，不得立法。
 - Controller 同时校验 `analytics.group.view` 和 `requirePlatformScope()`；即使某商户用户误拿到权限，也会因 merchant scope 被拒绝。
 - 指标目录来自真实 ETL 快照：`revenue.net`、`revenue.ticket`、`revenue.bar`、`traffic.amusement`、`verify.done`、`risk.pending_review`；商户数由当日 merchant ETL 快照去重统计。
@@ -107,7 +107,9 @@
 **迁移 / 表 / 权限**
 - 迁移：权限来自 `V115__analytics_group_overview_permission.sql`；职位授权闭环来自 V169；高危授权审批来自 V170
 - 表：`analytics_business_metric_snapshots`、`projects`、`merchants.business_entity_id`
-- 权限：`analytics.group.view`（`domain=analytics`、`risk_level=high`、`requires_approval=true`，V394 已置）。**实现待对齐（2026-08-04 F1 登记）已闭合（2026-09-11 M013 核对）**：跨项目汇总下钻属 §3 强制闸门第 2 类「提权或跨商户跨项目（综合体）」，按该强制闸门 `requires_approval` **应为 `true`**；V115 初始种子为 `false`，V394 已更新为 `true`，与 §3「不得把 HTML 明确的强制项降级为无需审批」一致；§4 的 `requirePlatformScope()` 只收敛可见范围、不替代授权审批。
+- 权限：`analytics.group.view`（`domain=analytics`、`risk_level=high`、`requires_approval=true`，V394 已置）。**审批属性缺口（2026-08-04 F1 登记）已闭合（2026-09-11 M013 核对）**：跨项目汇总下钻属 §3 强制闸门第 2 类「提权或跨商户跨项目（综合体）」，按该强制闸门 `requires_approval` **应为 `true`**；V115 初始种子为 `false`，V394 已更新为 `true`，与 §3「不得把 HTML 明确的强制项降级为无需审批」一致；该结论只涉及审批属性；`requirePlatformScope()` 不替代授权审批，也尚未完成本节所述项目级结果过滤。
 - UI：`ADM-ANA-002` 集团总览；可通过 `ADM-ACC-003` 的职位模板授出或收回 `analytics.group.view`
 
 > 变更记录（2026-09-11，V1.01，160 全量审计 M013-002/005/006）：§1 `risk_level` CHECK 与 §4 `analytics.group.view` 审批属性按 V394 更新为已闭合；§2 关键端点补 lifecycle 并标注 DELETE 恒拒绝；§3 税费权限码登记句按 V217 / SupervisorReview 现状改写。
+
+> 变更记录（2026-09-14，V1.02，M013-012–016）：集团项目隔离现状与审批属性闭合分开；岗位模板普通权限与待批高危、日常临时审批、申请权限风险档、总结查询参数按现有实现订正。
diff --git a/docs/platform-governance-acceptance-pack-v0.2.md b/docs/platform-governance-acceptance-pack-v0.2.md
index dbdfc245..c5b1f63c 100644
--- a/docs/platform-governance-acceptance-pack-v0.2.md
+++ b/docs/platform-governance-acceptance-pack-v0.2.md
@@ -1,4 +1,4 @@
-# 平台治理验收包 V0.04
+# 平台治理验收包 V0.05
 
 > 合并说明：本文按 2026-06-10 全量文档收口与合并归档审计执行，用于替代下列分散源文件。源文件已移入 `_archive/20260610-retention-audit/merged-layer-spec-sources/`，后续开发、审计和交接以本文为入口。
 
@@ -178,7 +178,7 @@
 | G4 | 空间/铺位建模 | 部分 | 空间对象须按 GOV 既有口径收敛 | P0 按楼层、区域、点位、商铺、卡座、吧台位、游乐入口、库房和设备点位建模；铺位按 HTML 已定义口径管理，不另立区块层级 |
 | G5 | 合同审批边界 | 已收敛 | 原"P0 仅单级确认、多级审批 P1"与 GOV 多级模型冲突 | 合同新建、到期终止、变更三类各走各自的审批流程模板、模板分别可配（业务参数，按综合体可配，须同时配置上下限），各类阶段数与审批岗位可以不同；每类均按统一审批模型执行：草稿提交时指定这条主线的最终审批人（默认总经理），系统按组织层级逐级自动补全；业务、财务、法务为平行的相关部门线，各指定该部门最终审批人并补全至该部门预先设定的最低起点层级；主线与各条平行线全部通过合同才生效，任一节点驳回则整单退回起草。三类均不得录入即生效：新建未审批不生效、到期不得无审批自动终止、变更未审批不生效。产品细化见 PRD §12 已确认第 18 条 |
 | G6 | 高级审批级别阶梯 | 已补 | 原把"指定角色"与 level_rank 计算写成已建模缺口 | 收口为 GOV 三档：上一级、上两级、最高权限。最高权限是持有授权 U 盘密钥的那一个人，不按职级档位解析；不设指定角色档 |
-| G7 | 集团视图与跨项目汇总 | 已承接；实现缺口已闭合 | 已由 `group-governance-rebuild-prd-v1.0.md` 与 V115/V169/V170 承接，所有对象继续携带 `project_id` | `analytics.group.view` 审批属性已由 V394 置 `requires_approval=true`（集团治理 PRD §4 同步），不反向改写业务口径 |
+| G7 | 集团视图与跨项目汇总 | 已承接；审批属性已闭合，项目范围隔离待对齐 | 已由 `group-governance-rebuild-prd-v1.0.md` 与 V115/V169/V170 承接基本聚合能力，所有对象继续携带 `project_id` | `analytics.group.view` 审批属性已由 V394 置 `requires_approval=true`；项目级调用者的汇总结果尚未按本项目过滤（集团治理 PRD §4），不得把审批属性闭合当作隔离已完成 |
 
 > 待批准事项：员工数据处理与监控告知的同意凭据尚未写入三份 HTML，不作为本验收包必过项，也不得升为必填字段或注册流程，不另造替换 consent 字段。
 
@@ -197,6 +197,7 @@
 | 日期 | 类型 | 位置 | 内容 | 原因/依据 |
 |---|---|---|---|---|
 | 2026-09-11 | 改 | §10A G7 / §10 | 版本升 V0.04；集团视图审批属性缺口按 V394 改为已闭合 | 160 全量审计 M013-002 |
+| 2026-09-14 | 改 | §10A G7 | 版本升 V0.05；区分集团审批属性已闭合与项目范围隔离待对齐 | M013-012；集团治理 PRD §4 与当前集团总览调用链 |
 | 2026-09-01 | 增/改 | §2 / §2A / §10A G7 | 版本升 V0.03；补金额精度、合同本地 AI、合同当地时间和外部经销商全链验收；集团视图统一为已承接并保留实现缺口 | M01 全量复审 ADV-01/02/04/05/08；三份业务 HTML 现文 |
 | 2026-06-07 | 改 | §6 终端开关页验收 | 终端枚举改为 10 端，对齐字段字典 §3.1 | D5、govfam-3；本轮已再对齐十一端 |
 | 2026-06-07 | 改 | §7 终端开关状态 | 去掉无支撑的"定时启用/定时停用"独立态，改为派生过渡态 | govfam-4 |
diff --git a/docs/platform-governance-prd-v0.1.md b/docs/platform-governance-prd-v0.1.md
index 29d77349..ead26b29 100644
--- a/docs/platform-governance-prd-v0.1.md
+++ b/docs/platform-governance-prd-v0.1.md
@@ -1,4 +1,4 @@
-# 平台治理层产品需求 PRD V0.04
+# 平台治理层产品需求 PRD V0.05
 
 > 权限边界：本 PRD 只提供产品细化；三份业务 HTML 拥有最终业务解释权。其他文档、配置、代码和数据结构均不得覆盖 HTML；HTML 未记载的新规则须经用户批准并写入 HTML 后方可生效。**三份 HTML 之间若出现冲突，按确认时间线判定：凡经用户确认过的内容，确认时间越晚的优先级越高，后确认的覆盖先确认的（以决策台账记录的裁决时间为准）；无法确定确认时间先后的，仍须逐项重新裁决**（2026-08-05 R7 补，依据 PGM「解释权声明」条 / DIFF-A-051）。
 
@@ -654,7 +654,7 @@
 > **（2026-07-20 安全修复 AUD-011，补记于第 14 条）权限模板应用不是高危授权审批的例外**：`applyPermissionTemplate` 现与角色/岗位/临时授权走同一政策引擎 `requireGrantApprovalIfNeeded`——模板含任一 `requires_approval` 权限时，经模板入口批量应用一律 fail-closed 拒绝（`GRANT_REQUIRES_APPROVAL` 403），不得绕过职责分离/主管复核。高风险授权须改走 `applyGrant → reviewGrant` 主管复核流（该流按权限快照建待审请求、批准后再原子应用）；模板入口对高风险直接拒绝、无待审窗口，故审批期间模板内容变化亦无从偷换。
 14A. 资金/核销/退款/调账/分红发放、提权或跨商户跨综合体、敏感数据、高危业务、**税费规则与税费调整**（共“五类”）——上述权限授出前必须先审批才生效；配置只允许新增高危项或提高审批等级，不得关闭这些强制闸门。授权申请人与审批人必须职责分离，申请人不能审批自己的授权申请。**持股分红的方案配置、算单、审批和发放属于资金类高危动作，由最高权限审批，提交人不得审批自己的单据。税费规则与税费调整的审批链固定为：财务审核 → 税务审核 → 法务审核 → 顶层平台公司老板最终确认后生效**；任一环节驳回均不得生效，提交人不得审批自己提交的调整，全部审批意见和时间必须留痕；税率、税目不属于普通后台配置项。（依据 PGM「高危授权审批（已确认）」条、ENUM「税费配置」条；与 §7.8.1 及 `group-governance-rebuild-prd-v1.0.md` §3 口径一致）
 14B. 保存岗位、职责或同一商户、同一项目范围内的所属组织变更后必须立即按变更后的岗位权限模板重算有效权限并使旧岗位或旧范围派生权限失效；同一商户、同一项目范围内的普通模板权限随保存立即生效，超出新模板范围的增权保持未生效直至审批通过。保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批，审批通过后方可生效；属于既有高危权限的，不得因处于岗位模板内而免除授出前审批。本条不把仅职级变化或任意手工模板应用列为触发条件（DEC-R8-91）。
-15. 所有对象必须带综合体项目（`project_id`）归属，跨项目汇总一律按该归属聚合——这是本 PRD 的强制要求。集团视图与跨项目汇总下钻**已由 `group-governance-rebuild-prd-v1.0.md` 承接落地**（V115 集团总览权限、V169 职位角色装配、V170 高危授权审批）（GAP-20 `analytics.group.view` `requires_approval` 已由 V394 置 `true`，闭环），**不再是本 PRD 的未来待办**。（2026-08-04 F-FIN 时态协调：**本 PRD 早期表述**“未来需要集团视图和跨项目汇总视图，但 P0 先保证……”与本节后续细化第 12 条的已落地状态时态冲突，以本条为准。2026-08-07 引文订正：该句**不是 HTML 原文**——PGM「未来需要集团视图和跨项目汇总视图」条现文为“未来需要集团视图和跨项目汇总视图，保证所有对象带综合体项目归属”，无“但 P0 先”字样）
+15. 所有对象必须带综合体项目（`project_id`）归属，跨项目汇总一律按该归属聚合——这是本 PRD 的强制要求。集团视图与跨项目汇总下钻**已由 `group-governance-rebuild-prd-v1.0.md` 承接落地**（V115 集团总览权限、V169 职位角色装配、V170 高危授权审批）（GAP-20 `analytics.group.view` `requires_approval` 已由 V394 置 `true`，闭环），**基本聚合能力不再是本 PRD 的未来待办**；上述闭环仅指审批属性，项目级调用者的汇总结果尚未按本项目过滤，仍须按集团治理 PRD §4 对齐项目隔离。（2026-08-04 F-FIN 时态协调：**本 PRD 早期表述**“未来需要集团视图和跨项目汇总视图，但 P0 先保证……”与本节后续细化第 12 条的已落地状态时态冲突，以本条为准。2026-08-07 引文订正：该句**不是 HTML 原文**——PGM「未来需要集团视图和跨项目汇总视图」条现文为“未来需要集团视图和跨项目汇总视图，保证所有对象带综合体项目归属”，无“但 P0 先”字样）
 16. 所有端都必须有开关接口，支持按全局、综合体、商户、业务系统、模块、版本/设备、灰度控制，高层级关闭压倒低层级打开；关闭后该端或模块不可用，打开后仍受权限、数据范围和设备状态控制；与钱有关的规则（分账比例、结算口径、综合服务费、租金与抽成规则）一律不得使用灰度、不得按商户或设备部分开放，只能按综合体整体统一切换，切换日期由后台配置（业务参数，按综合体统一可配，须同时配置上下限），切换前后各自适用完整的一套规则；灰度只用于不影响金额计算的功能开放。同一套规则之下的参数取值按三份业务 HTML 为该参数写明的粒度配置，不属于灰度或部分开放。开关缺配置行一律默认关闭（opt-in），不得由代码或部署脚本视同打开；开关服务、支付通路、核销与入场等营业必需的能力不得在营业时段进行任何可能导致中断的变更、升级或维护，所有此类操作只能安排在非营业时段并事先报备；开关拉取失败按兜底关闭处理、写操作一律拒绝，只有留痕不受兜底关闭影响。终端类型为受控十一类（客人手机应用、客人小程序、Web 前端（业务人员，客人无网页端）、工作人员平板、员工工作手机、员工私人手机、商户网页门户、商户手机应用、触摸屏一体机、移动管理端、独立经销商 Web 门户），产品细化见 `platform-governance-product-spec-v0.2.md`「字段字典」部分 §3.1「终端开关字段」，最终业务边界仍以三份业务 HTML 为准。
 17. 所有有过程的业务动作必须留痕，订单获取、销售归属、卡座服务、异常第一处理人，以及酒水和可追责物品从库房到客人的交接过程必须能追溯责任人、设备、地点、时间和结果。已拍板例外：安检环节不产生按人记录，留痕与溯源以设备侧记录为终点。
 18. 合同生命周期统一为「信息录入/发起 → 可配置审批流 → 审批通过后生效」：新建合同审批通过后才生效；到期或终止审批通过后才终止；变更（续租、退租、面积变更、租金调整、经营范围变更、提前终止、经营范围授权、位置变更）审批通过后才变更或终止落地。审批流模板可配，一律按全平台统一的审批模型执行：指定主线最终审批人（默认总经理）＋系统逐级自动补全，业务/财务/法务为平行的相关部门线、各指定该部门最终审批人并补全至该部门预设的最低起点层级，主线与各平行线都通过才生效；任一节点驳回则整单退回起草；超时未处理自动提醒并升级到上一级、不得默认通过；全程保留原因、版本快照与审计。（2026-08-04 用户裁决改写）
@@ -967,3 +967,4 @@ P0 阶段的重点是把“谁、在哪、凭什么、谁能操作、谁负责
 | 2026-08-29 | 改 | §7.3 / §12#10 / §12#5 / §15.2 / §15.4 | 业态去掉是否核心/第三方接入/统计分类/状态；废止多类对象统一待复核；告知/同意不得升为必填或注册流程，不另造 consent_* | M01 ADV-017 / ADV-003 |
 | 2026-08-29 | 改 | §6.3 / §7.2 / §7.3 | 业态收回 ENUM 六值+单一归属，废止维护/绑定开关/复合业态；废止空间「是否可留痕」开关 | M01 R-007 / R-008 |
 | 2026-09-11 | 改 | §5 / §6.2.1 / §6.9 / §12#8 / §12#15 / 变更日志 | 版本升 V0.04；私人手机低风险功能列表「考勤打卡」改「查看考勤」（治理地图 V0.12 L741、业务地图 V0.14 L930/L1865/L1445）；§6.2.1 改为字典硬校验口径并补 `parking`；自营标注改为合作方式 `self_operated` 既有字段；准入闸口现状改为员工域已接线、四域未接；GAP-20 按 V394 闭合 | 160 全量审计 M013-002/003/004/007/008；PGM L741、BSM L930/L1865/L1445 现文 |
+| 2026-09-14 | 改 | §12 已确认第15条 | 版本升 V0.05；集团基本聚合与审批属性已具备，项目级结果过滤仍待对齐 | M013-012；PGM 权限规则及集团治理 PRD §4 |
```
