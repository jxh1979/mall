CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M033 B5-r1 席乙（Grok）整模块三线全量确认审

版本：V0.01  
席位：审计席乙 / grok-4.6 xhigh  
步骤：B5-r1（B2 定稿修复队列为空，按 V0.17 跳过 B3/B4；B3 累计 0；文件名按派发指令用 r1，不另造 r0）  
正本：`/home/james/complex-management` `main` 主工作区  
提示词：`160-audit-prompt.md` V0.17；`plan/modules.md` V0.04；`plan/criteria-lock.md` V0.11；`plan/runbook.md` V0.02  
findings 输入：V3.04（本席只读，不改）  
独立性：未读 `modules/M033-b5-r1-self-raw.md`。可读旧 B1/B2：`M033-b1-grok-raw.md`（全文 SHA256 `10ca20e8b86b07fa64b6b7ed40aa0066215e7f7f03446143d6f44d548c014ca4`）、`M033-b1-self-raw.md`（全文 SHA256 `5f01f3dd7d617f634b65b2f7dbc752aab2c396d9d43fece548e638cd774f8d80`）。读了 `ledger.md` 当前段（其中已有席甲 B5 封存字样与 XM-M033-D 字样）——该字样**不当成本席 finding**；account_type 契约差由本席独立对照 OpenAPI/DTO/Service 复核，结论见观察，不升队。未用 MCP、子代理、第三席。未问人。未改业务/HTML/锁/findings/ledger。未 fetch/commit/push/merge/deploy。未建脚本/日志/临时产物。未对任何库执行写 SQL。

本席结论：**PASS**

---

## 0. 环境实用强制技能（本席自核，铁律 3）

本节点按 V0.17 §6 + runbook：B5 只读全量确认，禁止修复。

| 技能 | 本席路径 | 用法 |
|---|---|---|
| `using-superpowers` | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能自检 |
| `using-arc` | `/home/james/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 路由：本任务是 160 审计席乙，不走 Arc implement/audit 工作流 |
| `graft` | `/home/james/.claude/skills/graft/SKILL.md` | `graft grep` 追 DTO 消费边 |
| `grilling` / `grill-me` | mattpocock `skills/productivity/grilling`、`grill-me` | 对抗式自问自答；本体「等用户 / 派子代理」作废 |
| `systematic-debugging` | superpowers | 只借读错误→复现→追数据流；不改码 |
| `verification-before-completion` | superpowers | PASS 前跑 Jackson 探针与既有测试并读输出 |
| `codebase-design` | mattpocock | DTO 是 HTTP 缝上的浅适配器，删除测试见 §4 |
| `diagnosing-bugs` | mattpocock | 以 jshell + 既有单测为反馈环；本体「向用户展示假说」作废 |
| `ponytail-review` | ponytail 4.9.0 | 只列可删项，不落地 |
| `writing-for-agents` | mattpocock | 本 raw 给下游席/调度读；不写模块收口文件 |
| `tdd` / `test-driven-development` | 存在 | **本节点不适用**（B5 禁止修复，无 ID 可归因的新测试） |
| `requesting-code-review` / `receiving-code-review` | 存在 | **本节点不适用**（B4 / Codex 收票） |
| `brainstorming` / `writing-plans` | 存在 | **本节点不适用**（阶段 A） |
| `ponytail` | 存在 | **本节点不适用**（B3 修复强度） |
| `resolving-merge-conflicts` | 存在 | 无合并冲突，未调用 |
| `git-guardrails-claude-code` | 存在（可选） | 本体要问安装范围，铁律 5 禁止；记存在、不调用 |
| `batch-grill-me` / `ubiquitous-language` | **缺口** | 本环境无对应 SKILL.md，不冒充已调用 |
| 划掉技能（worktree / 子代理 / research / code-review 本体等） | —— | 按 §6 不用 |

graft 本轮两次：`AccessUserRequest|AccessGrantApplyRequest|…` ≈76368 tok；其余 8 个 Request ≈379126 tok。

---

## 1. 锁哈希与范围事实（类型 4）

独立实算（不得抄 sidecar/ledger/派发期望值）：

```
sha256sum /home/james/complex-management/docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
```

sidecar `plan/criteria-lock.sha256` 同行；ledger 顶部 `CRITERIA-LOCK-SHA256:` 同行。本席只报实算值。

`plan/modules.md` V0.04 M033：13 文件、有效内容 229 行、标记 NORMAL。本席复算：

```
git ls-files -z -- <13 paths> | xargs -0 cat | wc -l
229
```

`origin/main` = `124902b7366060925cfa49a24d7dfe4afaa3079b`（`124902b7 Merge pull request #473`）。十三文件 `git diff --exit-code 124902b7 -- <path>` 全部 IDENTICAL。工作区对 HEAD 无 DTO diff。自 B1 以来本模块写入集合仍为空。

| # | 路径 | wc -l | sha256 |
|---|---|---:|---|
| 1 | `backend/src/main/java/com/complex/platform/access/dto/AccessGrantApplyRequest.java` | 48 | `26907d4bdd81d538b6a2a08619919b1ab37ced7de85120b7015ffae531d0bea8` |
| 2 | `…/AccessGrantReviewRequest.java` | 12 | `a12c80e8d3cd44db2ea5e0827d88666467307e3facf5e455cb85990e671d3d19` |
| 3 | `…/AccessUserRequest.java` | 36 | `51ffd29cb122c336d1a964f29d7572569ff09257d4ecf19c7b8a546f00cf0fb3` |
| 4 | `…/PermissionTemplateApplyRequest.java` | 13 | `da109c37fefc054678c70ab15e7c739c86f280a9e814d16ed47012b4407d3090` |
| 5 | `…/PermissionTemplateRequest.java` | 19 | `ce2fcc2dfe284bd2f847d1fcd85bbff1b910db71862f266630bdd639284a70ca` |
| 6 | `…/PositionRoleTemplateRequest.java` | 12 | `e154bff2c91cf9a9a7a5751dee44decd7904e6638d89dc67bcf36363fe088999` |
| 7 | `…/RoleAssignmentRequest.java` | 9 | `00e229f0511e24f168068827b6a39348b8d5a325ae9c1731e6d9a455cfe02ea9` |
| 8 | `…/RoleRequest.java` | 14 | `bdd4a6297423cda981f62693a5a6879c6f08e50948d30933a8fc949bc460027b` |
| 9 | `…/StaffLevelAssignmentRequest.java` | 12 | `c569cd7da131ee7c7850ff3e8c368a3b80ed5505f49f50280b8c43a9a25fc031` |
| 10 | `…/StatusChangeRequest.java` | 11 | `6d4ac51fe553cb890897d6d609fb5b5eff5cb311dd6852978751457a4bcd3dd6` |
| 11 | `…/TemporaryGrantRequest.java` | 17 | `6ea363657bdb78e4f427d6b03592e9c4a00e7d930e908b79ad6d8276d47b2f49` |
| 12 | `…/UserPermissionOverrideRequest.java` | 13 | `a871b6b46c7889e1cc020b8d37865dc5a92d7835f3a3a39281d287f11065bab0` |
| 13 | `…/UserPositionAssignmentRequest.java` | 13 | `ae0f2cc0e0575b5196489347fe1428e21eea53e4675c22a111107a41d994f0d1` |

全局 Jackson：`backend/src/main/resources/application.yml:16` `property-naming-strategy: SNAKE_CASE`。仓内生产代码零处打开 `FAIL_ON_UNKNOWN_PROPERTIES`；Spring Boot 构建器默认 false。`git diff --stat origin/main -- AccessService.java` 空（消费面与 origin/main 一致）。

---

## 2. 第一性原理线（2.3 三问）

### 2.1 这段东西为什么必须存在？删掉则 HTML 哪一条做不到？

十三份都是写请求记录，作为 `AccessController` `@RequestBody` 的 HTTP 缝。删掉它们，下列 HTML 在本后台入口上没有入参承载：

- BSM-028「管理员工、岗位、班次、职级、权限」；「发起单据时只需指定该事项的最终审批人」→ `AccessGrantApplyRequest.finalApproverId`
- BSM-120「职级只给薪资大范围，具体薪资仍需单独填写」→ `StaffLevelAssignmentRequest` / `UserPositionAssignmentRequest` / 授权申请上的 `baseSalaryAmountCent`
- PGM-066「员工权限按单一功能最小颗粒度配置，支持角色和员工级完全自定义」→ `RoleRequest.permissionCodes`、`UserPermissionOverrideRequest`
- PGM-033 注册两条路径之外，后台仍要能建外包供应商号 → `AccessUserRequest`（服务层只放行 `outsourced_vendor`，属已闭环 M031）
- PGM-001「再管资格：员工是否有权限」→ 角色/模板/临时授权/覆盖 一组请求体

答得上 = 有业务上位。个别组件（`phone`、部分 `evidenceAttachmentIds`、`mode` 契约词）答不上「必须存在才能让 HTML 成立」——走 2.1 分类后已在 B2 定为细化+登记，见 §6，**无新证据不改判**。

### 2.2 声称的行为有没有被实际执行验证？

DTO 本身不可单独起进程。类型 2 对纯记录不强制；本席用类型 4：

**A. Jackson 2.21.5 + SNAKE_CASE + JavaTimeModule + FAIL_ON_UNKNOWN=false**（对齐 Boot；classpath=`backend/target/classes` + jackson-databind/core 2.21.5 + annotations 2.21 + jsr310 2.21.4）：

```
PHONE=null
USER=u
ACCT=outsourced_vendor
EV=[ev1]
CONSENT=c1
TOSTRING_HAS_PASS=true
CAMEL_IDEM=null
MODE=replace
TPL=tpl
APPLY_EV=[ev9]
FINAL=usr_boss
GRANT_EV=[evg]
STAFF_TYPE=staff
ROLE_EV=[evr]
ROLE_NAME=巡检员
EFFECT=deny
ACTION=wrong
TG_EV=[evt]
TG_SINGLE=true
```

FAIL_ON_UNKNOWN=true 的对照：camelCase `idempotencyKey` → `UnrecognizedPropertyException`，已知属性列表含 `"phone"`（`@JsonIgnore` 仍登记属性名）。

**B. 既有测试（纯单测，未 source `/etc/complex/complex.env`，未碰库）：**

```
mvn -Dtest=AccessServiceTests#createAccessUserWithAllowedAccountTypeSucceeds+createAccessUserWithPlatformAdminAccountTypeThrowsValidationError+applyPermissionTemplateAppliesToManyUsersAndWritesAuditEvent+createAccessUserStoresManualEvidenceAndProvisionsCredential,M006SecurityRegressionTests#manualAccountDoesNotPersistOrReturnPhoneDigest test
AccessServiceTests: Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
M006SecurityRegressionTests: Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

`manualAccountDoesNotPersistOrReturnPhoneDigest` 原句断言：`created.phoneHash()` 为空（构造器仍传入 `"13800000000"`）。`createAccessUserWithAllowedAccountTypeSucceeds` 只循环 `outsourced_vendor`。`applyPermissionTemplateAppliesToManyUsers…` 传 `mode="merge"`，断言套用成功且 `singleUse` 来自模板而非契约三枚举。

**C. 消费（类型 4，`rg` + 源码）：**

- `AccessUserRequest.phone()`：`AccessService` **零调用**。`@JsonIgnore` 在 bytecode `phone()` 的 `RuntimeVisibleAnnotations`。
- `createAccessUser` 读 username/password/accountType/organizationId/merchantId/status/consentReference/evidenceAttachmentIds/operatorNote/idempotencyKey（`AccessService.java:1178-1228`）。非 `outsourced_vendor` → `STAFF_REGISTRATION_REQUIRED`（L1203-1206）。
- `applyPermissionTemplate` 只把 `mode` 与 `"single_use"` 比较，其余当追加（L241 原句：`boolean singleUse = template.singleUse() || "single_use".equalsIgnoreCase(trimToDefault(request.mode(), "append"));`）。不读 `request.evidenceAttachmentIds()`、不读体里 `templateId`（用路径 id）。
- `applyGrant` 读 `finalApproverId`（L928），**不读** `evidenceAttachmentIds`。
- `reviewGrant` **读** `evidenceAttachmentIds`（L989）送进主管链。
- `createRole` **不读** `evidenceAttachmentIds`（L319-327 只组 RoleRecord）。
- `templateFromRequest` **不读** `evidenceAttachmentIds` / 体 `sourceTemplateId`；内部 mode 写死 `"merge"`（L2290，属 M031）。
- `changePositionTemplateLifecycle` 在 `approve_delete`/`reject_delete` **读** `StatusChangeRequest.evidenceAttachmentIds`（L548）。
- 管理端 `PermissionTemplatesPage.tsx:328` 原句：`mode: singleUse ? "single_use" : "append"`。

### 2.3 哪个前提未经验证就当真？

已用命令拆掉、不得再当隐含前提：

1. 「OpenAPI 写了 `phone` 就会哈希入库」——JSON 丢弃 + Service 不读。仍是 M033-001 事实，处置仍不在本 13 文件。
2. 「契约 `mode=replace|merge|preview` 会改变套用语义」——jshell `MODE=replace`，Service 只认 `single_use`。仍是 M033-002。
3. 「写请求信封上的 `evidence_attachment_ids` 都会进审批/审计」——仅建用户、授权复核、岗位模板删除审批三条路径读取。仍是 M033-003。
4. 「队列为空 = 模块已全对」——本席仍整模块重跑三线；空队列只跳过 B3/B4，不是收口。
5. 「`@Valid` 会校验记录组件」——十三记录无 Jakarta 约束，`@Valid` 空转；首次缺字段走 Service `requireText`（O-10，不立项）。
6. 「ledger 里席甲 B5 的 XM-M033-D 已是本席结论」——不是。本席独立对照后把它留在观察（见 §7），不升队。

---

## 3. 顺从式：52 项逐项

映射原文：`criteria-lock.md:6412` 一行 52 个 ID。每条定义已读（general L12–100；BEQR/BSM/PGM 各 `###` 块含 HTML 原文）。「适用」= 本 13 文件有入参/枚举/命名缝需要核该锚点；「无本面」= 锚点正文不在这些记录类上落地，本模块不发明该规则即可。

| ID | 本面 | 理由与证据 |
|---|---|---|
| BEQR-157 | 无本面 | HTML「员工状态：待入职、在职、试岗、停岗、离职」。本面 `AccessUserRequest.status` 是账号生命周期 `active/invited/disabled`（OpenAPI L31948-31954 与 `requireAccountStatus` L1260-1265 一致），不是员工档案状态。未把员工五态写进这些 DTO。 |
| BEQR-182 | 无本面 | 考勤日结果。十三文件无考勤字段。 |
| BEQR-183 | 无本面 | 打卡方式。无本面。 |
| BEQR-184 | 无本面 | 请假类型。无本面。 |
| BEQR-185 | 无本面 | 请假单状态。无本面。 |
| BEQR-186 | 部分适用 | HTML 是月结薪资单构成。本面只有岗位/职级/授权申请上的 `baseSalaryAmountCent`（具体薪资，整数分），对应 BSM-120「职级只给薪资大范围，具体薪资仍需单独填写」，不是薪资单项目发明。未把绩效档/扣款上限做成 DTO 枚举。 |
| BEQR-187 | 无本面 | 提成发放（实时计算进月结、禁止现金现抽）。DTO 无提成字段。 |
| BSM-001 | 适用 | 「谁能操作」需要授权入参缝。记录类提供该缝，无把票务/酒吧流程写进 DTO。 |
| BSM-003 | 适用 | 「权限分岗」。角色/岗位模板/职级/覆盖请求是下位实现。 |
| BSM-009 | 适用 | 「管理…员工、设备、权限」卡片。本面是权限/员工账号写入口，不含设备。 |
| BSM-028 | 适用 | 「管理员工、岗位、班次、职级、权限」；「发起单据只需指定最终审批人」→ `finalApproverId`（Service L928 有读；契约 schema L36584-36668 无该键，见观察）。班次不在本面。 |
| BSM-029 | 适用 | 「用户权限要知道员工注册信息、职级和审批状态」。`AccessUserRequest`/`StaffLevelAssignmentRequest`/`AccessGrantReviewRequest` 承接。 |
| BSM-073 | 部分适用 | 商户专区自助五项无本面。`accountType` 含 `merchant`/`merchant_admin` 字符串，校验在 M031。 |
| BSM-084 | 无本面 | 员工私人手机 App 自助。本面是后台写请求。 |
| BSM-085 | 部分适用 | 「新员工注册分两条路径」在 PGM-033/M031；本面后台建号只剩外包供应商。DTO 不发明第三条注册路径。 |
| BSM-086 | 无本面 | 排班/考勤/请假。无本面。 |
| BSM-087 | 无本面 | 「我的薪资」展示。无工资条 DTO。 |
| BSM-088 | 无本面 | 提成/营销打卡边界。无本面。 |
| BSM-114 | 适用 | 管理后台入口「配置、查询、复核」。这些 Request 即后台写入口的体。 |
| BSM-115 | 适用 | 「基础管理连所有业务…员工…由谁负责」。授权/岗位/职级请求把人绑到组织。 |
| BSM-120 | 适用 | 「职级可按 M1-M10…职级只给薪资大范围，具体薪资仍需单独填写」。`staffLevelId` + `baseSalaryAmountCent` 分离，符合原文，未把职级当成薪资。 |
| GEN-001 | 适用 | 记录类有承载；冗余组件见已登记 001/003。删除测试：删 DTO 会把 Jackson 映射散到 Controller。浅适配器，值得保留。 |
| GEN-002 | 适用 | 入口可反序列化（jshell）；主路径/错误路径由 Service 执行，测试 5/5 绿。 |
| GEN-003 | 适用 | 所跑测试有能失败断言（`phoneHash()` 为空、`INVALID_ACCOUNT_TYPE`、套用人数）。测试通过不替代 Jackson 探针。 |
| GEN-004 | 适用 | 边界校验在 Service（`requireText`/`ALLOWED_ACCOUNT_TYPES`/`requireAccountStatus`）。DTO 无 BV = O-10，首次缺字段非静默。 |
| GEN-005 | 适用 | Controller 各端点 `requirePermission`；体字段不能改路径上的 userId（路径覆盖，O-4）。`effect`/`decision` 无 DTO 枚举，由 Service 归一。 |
| GEN-006 | 无本面 | 记录类不写库。持久化在 M031/仓库。 |
| GEN-007 | 适用 | 全组（除纯 StatusChange 的部分调用）有 `idempotencyKey`；幂等哈希整对象（含口令预像，O-6）。 |
| GEN-008 | 适用 | 本模块主伤面。001/002/003 仍成立；契约差有效修复在 M392。DTO 与 Boot SNAKE_CASE 一致（仅 `AccessUserRequest` 另有同名 `@JsonAlias`）。 |
| GEN-009 | 适用 | 契约字段静默丢（phone/错误 mode/未消费 evidence）= 伪成功，已登记。本 13 文件内改组件不能让首次正常使用变对。 |
| GEN-010 | 适用 | `phone` 不进站（与 PGM-059 采集收窄一致，M006-025）。`password` 必须进站；`toString` 含明文但 Controller/Service 无把 Request 打日志的点（O-6）。 |
| GEN-011 | 适用 | 复核/建用户/删岗位模板读 evidence；申请/建角色/建模板/套用/直发临时授权不读（003）。 |
| GEN-012 | 适用 | DTO 无 maxItems；OpenAPI 有。首次超长由更下层或框架处理，不立项。 |
| GEN-013 | 无本面 | 无构建/依赖文件。 |
| GEN-014 | 无本面 | 无 SQL。未跑 psql。 |
| GEN-015 | 无本面 | 无界面。 |
| GEN-016 | 无本面 | 本模块无 md。DTO 注释（`AccessGrantApplyRequest` L6、`UserPermissionOverrideRequest` L5）与 Service 实读一致，不是现在时假完成。 |
| GEN-017 | 适用 | 本席分类顺序：冲突→细化→技术性→添加。001/002/003 仍为细化。无新添加项。 |
| GEN-018 | 适用 | 本报告结论落到 HTML 原文、可复现命令、bytecode/测试输出。 |
| PGM-001 | 适用 | 治理底座「再管资格」。写请求是资格变更入口。 |
| PGM-008 | 适用 | 「员工和手机」卡片：注册/岗位状态。本面是后台账号/授权，不含设备绑定字段。 |
| PGM-022 | 无本面 | 空间点位。无本面。 |
| PGM-024 | 无本面 | 低消/容量/库位。无本面。 |
| PGM-026 | 无本面 | 商户档案四类主体绑定。`merchantId` 只是账号归属，不发明绑定规则。 |
| PGM-032 | 适用 | 「系统要知道员工是谁、审核是否通过」。`AccessUserRequest` 是后台建号体，与 App 注册分流在 M031。 |
| PGM-033 | 适用 | HTML 注册两条路径（公司自有 App / 商户代提）。后台 DTO 仍暴露 `accountType` 字符串；Service 对非 `outsourced_vendor` 抛 `STAFF_REGISTRATION_REQUIRED`。OpenAPI 枚举是 staff/sales/supervisor/merchant_admin/merchant，**不含** `outsourced_vendor`（`openapi-p0.yaml:31931-31939`）。本席 B1 已在本 ID 记下「暴露 staff/sales 等枚举」；无文件变化，**不升队**（§7）。 |
| PGM-034 | 无本面 | 工作手机/企微。无本面。 |
| PGM-035 | 无本面 | 提成发放治理。无本面。 |
| PGM-036 | 无本面 | 设备管理。无本面。 |
| PGM-048 | 部分适用 | 商户侧授权范围内全开放。本面可带 `merchant` 账号类型字符串；开放范围不在 DTO。 |
| PGM-059 | 适用 | 「手机号加密存放、界面脱敏」。本后台建号体 `@JsonIgnore String phone`（L14）+ 不落库，与 M006-025 收窄采集一致；契约仍写 phone_hash（001→M392）。 |
| PGM-066 | 适用 | 「权限最小颗粒」→ `permissionCodes`；「有过程必须留痕」「人工只补原因和证据」是操作指引、不作合规判定依据（原文 L6359）。evidence 丢弃见 003。 |

映射未含 PGM-040（权限和职级专章）。本席仍用该 HTML 作类型 1 上位需求读过入口卡片 PGM-009，**不 LOCK-AMEND**（O-9）。

---

## 4. 对抗式（grilling 自问自答）

❓ Q1 按契约传 `phone`，首次正常使用是 200 还是 400？  
➡️ Jackson Boot 口径 `PHONE=null`，HTTP 路径不会因该字段 400。Service 不读。与 B2 定稿 001 相同。删组件后未知字段仍被忽略，可观察结果不变。

❓ Q2 按契约传 `mode=preview`，会不会只预览不落库？  
➡️ jshell 绑上 `replace`（同属契约三词）。`AccessService.java:241` 只认 `single_use`。管理端发 `single_use`/`append`。本 DTO 是无约束 String。改 13 文件不能让 preview 变成预览。

❓ Q3 申请/建角色/建模板/套用/直发临时授权夹 `evidence_attachment_ids`，审批链看得到吗？  
➡️ 绑定成功（`GRANT_EV`/`ROLE_EV`/`APPLY_EV`/`TG_EV`）。`createRole`/`applyGrant`/`applyPermissionTemplate`/`templateFromRequest`/`createTemporaryGrant` 不读该组件。`TemporaryGrantRequest` 九参仍被 M031 `AccessService.java:1809` 构造。删组件必须改已闭环文件或零可观察差别。

❓ Q4 `account_type=outsourced_vendor` 契约禁止、实现唯一成功，要不要对本 DTO 立项？  
➡️ OpenAPI L31933-31938 枚举无 `outsourced_vendor`；jshell `ACCT=outsourced_vendor` 绑定成功（DTO 无枚举）。Service L1203 只放行它。这是 GEN-008 契约/实现差，有效修复在 M392（补枚举或改描述）与已闭环 M031（白名单）。给 DTO 加 OpenAPI 那五词会阻断唯一成功路径；只加 `outsourced_vendor` 仍对不齐契约。**无本模块可观察改法。B1 PGM-033 已记暴露 staff/sales 枚举，无新证据不升队。**

❓ Q5 `final_approver_id` 有 DTO 无契约，是不是本模块多造字段？  
➡️ HTML BSM-028 明文「只需指定最终审批人」。Service L928 有读。缺键在契约。DTO 方向正确（O-1 / XM-M033-C）。

❓ Q6 camelCase 体字段会不会让幂等键丢失造成重复写？  
➡️ Boot SNAKE_CASE 下 `CAMEL_IDEM=null`。管理端 `normalizeRequestBody` 转 snake。首次正常不走 camel。O-3。

❓ Q7 `toString` 含口令是 GEN-010 泄漏吗？  
➡️ `TOSTRING_HAS_PASS=true`。Controller/Service 无 `log(request)`。幂等哈希需要整对象预像。排除口令会让「只改口令的重放」撞键。O-6。

❓ Q8 无 BV 是否信任边界失败？  
➡️ 缺 `username` 走 `requireText`，中文错误，非静默。O-10。

❓ Q9 这些记录是不是过浅、该删？  
➡️ codebase-design 删除测试：删掉后 Jackson 别名/忽略/组件顺序会散到每个 Controller 方法。浅是缝的形状，不是 yagni。ponytail-review 可删项仍是 B1 那几处，B2 已否决本模块删除。

❓ Q10 队列为空能否把 B5 当形式过场？  
➡️ 不能。本席全文重读 13 文件、52 定义、HTML 原文、消费与探针后，没有新的本模块工程缺陷。

---

## 5. 文件覆盖（无 finding ≠ 未审）

| # | 文件 | 覆盖动作 | 与已定稿/观察的关系 |
|---|---|---|---|
| 1 | AccessGrantApplyRequest.java | 全文；jshell FINAL/GRANT_EV；applyGrant L900-928 | 003 不读 evidence；O-1 finalApproverId；O-5 兼容构造；O-11 expiresAt |
| 2 | AccessGrantReviewRequest.java | 全文；reviewGrant L989 实读 evidence | 复核路径读附件，与 003 五件对照 |
| 3 | AccessUserRequest.java | 全文；javap JsonIgnore；jshell PHONE/ACCT；测试 4+1 | 001 phone；O-3 JsonAlias；O-6 toString；PGM-033 枚举观察 |
| 4 | PermissionTemplateApplyRequest.java | 全文；jshell MODE；Service L241；前端 L328 | 002 mode；003 evidence；O-13 体 templateId |
| 5 | PermissionTemplateRequest.java | 全文；templateFromRequest L2279-2301 | 003 evidence；sourceTemplateId 体不读 |
| 6 | PositionRoleTemplateRequest.java | 全文；create/updatePositionRoleTemplate | 无未消费信封字段 |
| 7 | RoleAssignmentRequest.java | 全文；assign/unassign | 无 evidence 组件 |
| 8 | RoleRequest.java | 全文；createRole L319-327 | 003 evidence 不读 |
| 9 | StaffLevelAssignmentRequest.java | 全文；StaffService L233 再构造 | O-4 userId 路径覆盖 |
| 10 | StatusChangeRequest.java | 全文；生命周期 L548 读 evidence；revoke | O-12 revoke 不读 action |
| 11 | TemporaryGrantRequest.java | 全文；jshell TG_*；M031 L1809 九参 | 003 |
| 12 | UserPermissionOverrideRequest.java | 全文；jshell EFFECT；注释 grant\|deny | O-2 无 OpenAPI schema（`rg UserPermissionOverrideRequest docs/openapi/openapi-p0.yaml` 零命中） |
| 13 | UserPositionAssignmentRequest.java | 全文；定岗/内部再构造 | O-4 |

---

## 6. 已定稿三条：复证、不改判（禁止复用为新票）

B2-r1 双 PASS 定稿（findings V3.04）：M033-001/002/003 均细化、工程缺陷「是」、修复队列「否」。本席 B5 **独立复跑**后事实仍在，**无新证据，不改分类、不改队列、不新开 GR。**

### 复证 M033-001（原 GR-01，不复用票号）

- 文件/行/原句：`AccessUserRequest.java:14` `@com.fasterxml.jackson.annotation.JsonIgnore String phone,`
- 正常触发：按契约 `openapi-p0.yaml:31927-31930`「手机号；后端不落明文，仅保存不可逆 phone_hash」发 JSON `"phone":"13800000000"`。
- 四分类：冲突？HTML 未要求此后台建号接口必须收号（PGM-059 是存放/脱敏，注册在 PGM-033）。不冲突。能指向上位 PGM-059/建号接口 → **细化**。非技术性、非添加。
- 已有判据：GEN-008、PGM-059。
- 证据类型：4（jshell `PHONE=null`；javap `JsonIgnore`；`M006SecurityRegressionTests.java:221-223` `created.phoneHash()` 为空；`AccessService` 零 `request.phone()`）。
- 本模块改法：删组件则拆 M006-025 回归抓手，HTTP 仍忽略未知字段。不触碰 M031/M392 则首次行为不变。维持队列「否」，XM-M033-A。

### 复证 M033-002（原 GR-02）

- 文件/行/原句：`PermissionTemplateApplyRequest.java:11` `String mode`
- 正常触发：按契约 L29439-29445 枚举 `replace/merge/preview` 发 `mode=replace`。
- 四分类：HTML 无这三种套用语义 → 非冲突。上位是「可套用模板」（BSM-028/PGM-066 权限配置）→ **细化**。
- 已有判据：GEN-008。
- 证据类型：4（jshell `MODE=replace`；Service L241 只认 `single_use`；前端 L328 `single_use`/`append`；测试传 `"merge"` 仍按模板 singleUse 成功套用）。
- 本模块 DTO 是无约束 String，改注释无行为差。维持队列「否」，XM-M033-B。

### 复证 M033-003（原 GR-03）

- 文件/行/原句：五件的 `List<String> evidenceAttachmentIds`：`AccessGrantApplyRequest.java:10`、`RoleRequest.java:8`、`PermissionTemplateRequest.java:9`、`PermissionTemplateApplyRequest.java:8`、`TemporaryGrantRequest.java:9`。
- 正常触发：按契约在这些写请求夹 `evidence_attachment_ids`。
- 四分类：HTML「所有有过程的业务动作都必须留痕」点名了权限变更/临时授权（本席 B2 已订正「未点名」过窄）；「人工只补原因和证据」是操作指引。DTO 有字段但本模块不接链 ≠ 与明文相反的实现（冲突要「明文表述相矛盾」）。指向上位留痕 → **细化**。让这些动作真的存证是新能力，属添加，不在本 B5 发明需求。
- 已有判据：GEN-008、GEN-011、PGM-066。
- 证据类型：4（jshell 五件都能绑上 EV；`createRole`/`applyGrant`/`applyPermissionTemplate`/`templateFromRequest` 不读；`TemporaryGrantRequest` 被 M031 L1809 按现有参数构造）。
- 维持队列「否」，XM-M033-C。

---

## 7. 观察（不立项 / 不升队）

无新证据，沿用 B1/B2 已同意的观察，不改判：

| 编号 | 事实 | 处置 |
|---|---|---|
| O-1 | `finalApproverId` 服务有读；OpenAPI `AccessGrantApplyRequest` L36584-36668 无 `final_approver_id`（L26347 是另一条平行审批线 schema，不是本 DTO） | XM-M033-C → M392 |
| O-2 | `UserPermissionOverrideRequest` 及两路径不在 OpenAPI（rg 零命中） | 同 XM-M031-C / XM-M033-C |
| O-3 | 仅 AccessUserRequest 有与 SNAKE_CASE 同名的 `@JsonAlias`；Boot 下 camelCase 静默丢 | 不立项 |
| O-4 | 职级/定岗体 `userId` 被路径覆盖 | OpenAPI 已写不在体；安全方向对 |
| O-5 | 两紧凑构造器仍被测试调用 | 非死代码 |
| O-6 | 记录 `toString` 含口令预像；无日志点 | 不立项 |
| O-9 | PGM-040 未映射 M033 | 不 LOCK-AMEND |
| O-10 | 无 BV，`@Valid` 空转 | Service 中文校验 |
| O-11 | `expiresAt` vs `validUntil` 分属不同 schema | 各自契约一致 |
| O-12 | `/temporary-grants/{id}/revoke` 不读 `action` | URL 已表达动作 |
| O-13 | 套用体 `templateId` 与路径重复，体不读 | 首次两处同一 ID |
| O-14 | `templateFromRequest` 内部 mode `"merge"` | M031，非本记录字段 |
| O-ACCT | OpenAPI `account_type` 枚举无 `outsourced_vendor`，却列出 staff/sales 等；DTO 无枚举故两者都能反序列化；Service 只让 `outsourced_vendor` 成功 | 本席 B1 PGM-033 已覆盖「暴露 staff/sales 等枚举」。有效修复在 M392/M031。本 13 文件加枚举会阻断唯一成功路径或仍对不齐契约。**不升队、不新开 GR。** |

D-029 / 未发布工作流：13 文件与 `124902b7` 逐字节相同，不适用。`UserScopeChangeRequest.java` 同目录但不在本 FILES。

ponytail-review：`AccessUserRequest.java:14 delete:` JsonIgnore 后的 phone（B2 已否决删除）；`AccessUserRequest.java:9-18 shrink:` 同名 JsonAlias；五处未消费 evidence（B2 已否决本模块删除）。其余：Lean already。`net: 0`（本模块允许删除的行在定稿后为 0）。

---

## 8. 新 findings

**无。** 审完 13/13、52/52、三线之后，没有「本模块 13 文件内可改、且改完后首次正常使用可观察行为变对、且不触碰已闭环模块」的新工程缺陷。不为理论场景加需求。不把队列为空当成免审。

添加项：0。questions：0。HTML 铁律 4：三份 HTML 只读，未申请改。

---

## 9. 步骤结论

**PASS**

同时满足 runbook §9：13/13 覆盖记录；52/52 有适用或无本面及理由；三线均执行；声称有类型 1 或 4 证据；无未答 questions；无未定稿的本模块新缺陷；已登记 001/002/003 事实仍在且不改判；本席独立实算锁行有效。

本票是 B5 步骤票，不是代写 ledger `PASS`、不是写 `M033-*.md` 收口文件。合取与四值校验由席甲在双方 raw 封存后做。B3 累计仍为 0。
