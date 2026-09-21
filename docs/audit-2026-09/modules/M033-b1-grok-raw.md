CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M033 B1 席乙独立审计报告（Grok）

- 席位：Grok 4.6 / reasoning xhigh（席乙，只读外审）
- 步骤：B1 修复前全量审计（本模块首次开工，PENDING→IN_PROGRESS；本席不改 ledger/findings/计划）
- 控制合同：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17（实算 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04；runbook V0.02；锁 `criteria-lock.md` V0.11（LOCK-AMEND-COUNT 1）
- 模块：M033「功能权限点、职级三十档、权限模板与最终权限计算／后台实现／第3段」；标记 NORMAL；计划 FILES 13 / 有效行 229（本席 `git ls-files -z -- <13 paths> | xargs -0 cat | wc -l` = **229**）
- 互盲：未读 `modules/M033-b1-self-raw.md`，未用 MCP / 子代理 / 第三席
- 本席结论：**FAIL**（三线跑完、13 文件全文覆盖、52 判据逐项记录；工程缺陷临时 GR 3 条，待 B2 定稿）

---

## 0. 环境实用强制技能（本席自核，铁律 3）

本机路径核验，不抄 Codex 预检表。

| 技能 | 本机路径 | 本轮用法 |
|---|---|---|
| using-superpowers | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能路由。用户指令优先：禁子代理/MCP，故不走 Arc 全量评审席 |
| graft | `~/.claude/skills/graft/SKILL.md` | 定位 13 个记录类的生产/测试调用边；消费方只读追证 |
| grilling | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | **只借追问结构自问自答**；本体「等用户回答」「dispatch sub-agent」作废 |
| grill-me | 同上目录 `grill-me/SKILL.md`（`disable-model-invocation: true`） | 转发 grilling；未冒充已另开会话 |
| systematic-debugging | `.../superpowers/6.3.0/skills/systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流；不改代码、不加探针 |
| diagnosing-bugs | `.../mattpocock-skills/1.2.3/skills/engineering/diagnosing-bugs/SKILL.md` | 只借「先有可红命令」：本轮可红命令是 jshell Jackson 反序列化，不是改码循环 |
| verification-before-completion | `.../superpowers/6.3.0/skills/verification-before-completion/SKILL.md` | 声称前落命令输出（sha256sum / cmp / wc / jshell） |
| codebase-design | `.../mattpocock-skills/1.2.3/skills/engineering/codebase-design/SKILL.md` | 只借 seam/interface 词汇：这 13 个 record 是 HTTP→Service 的浅接口 |
| ponytail-review | `.../ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只找可删：JsonIgnore 后的死 phone、与 SNAKE_CASE 同名的 JsonAlias、收下不读的 evidence |
| i-have-adhd | `.../i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 输出形态；CLI 按用户「不回带正文」收窄，结论只在本 raw |
| writing-for-agents | 存在 | B1 不写模块收口文件；按意图跳过 |
| git-guardrails-claude-code | 存在 | 本体是装 Claude Code hook；本席按意图不装、不 fetch/commit/push |
| using-arc / audit | Arc 全量审计要派子代理 | **按意图执行**：不派；本轮走 160 V0.17 双席合同 |
| audit-context-building | 要派 function-analyzer 子代理 | **按意图执行**：不派 |
| brainstorming / writing-plans | 阶段 A | B1 按意图跳过本体 |
| tdd / test-driven-development / ponytail | B3 | B1 禁止修复，不调用本体 |
| requesting/receiving-code-review | B4 / 席甲 | B1 不请审、不修 |
| using-git-worktrees / research / dispatching-parallel-agents | 提示词划掉 | 不用 |

**缺口（不得冒充已调用）**

- `batch-grill-me`：本机 mattpocock 技能树未找到独立 SKILL.md
- `ubiquitous-language`：未找到
- `grill-with-docs`：存在但不在 §6 必核表，未当 grilling 替代

---

## 1. 环境事实（本席实算）

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
cat docs/audit-2026-09/plan/criteria-lock.sha256
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
git rev-parse HEAD            # e456ba96c84185c8d39f18f548011daac48a6466
git rev-parse origin/main     # 124902b7366060925cfa49a24d7dfe4afaa3079b
```

- 主工作区 `main` 相对 `origin/main` ahead 49 / behind 44，含未推提交与暂存；**本模块 13 个文件 `cmp` 与 `origin/main` 逐字节相同**（D-029 未发布工作流不适用）。
- 同目录另有 `UserScopeChangeRequest.java`，**不在** `plan/modules.md` M033 FILES，本席不审。
- 消费方只读追证：`AccessController` / `AccessService`（M031 已闭环）、`StaffService.assignStaffLevel` 构造本模块 `StaffLevelAssignmentRequest`；契约 `docs/openapi/openapi-p0.yaml`（M392）只读。
- ledger：XM-M031-A/B/C、W-1…W-4、D-029 已登记相邻事实。无新证据不重复立项。XM-M031-C = 控制器 6 个映射不在 OpenAPI（`/grant-policy` GET/POST、`/position-role-templates/{id}/lifecycle`、`/users/{id}/scope`、`/users/{id}/permission-overrides` 及 `/remove`）。
- 全局 Jackson：`backend/src/main/resources/application.yml:16` `property-naming-strategy: SNAKE_CASE`。未配置 `fail-on-unknown-properties`；Spring Boot 构建器默认 false。本席 jshell 按「SNAKE_CASE + FAIL_ON_UNKNOWN=false」复现 Boot 口径。
- 前端 `apps/web/src/admin/client/adminClient.ts:90-91` 发请求前把键转 snake，并补 `idempotencyKey`。
- 未改源码/测试/计划/findings/ledger；未对任何库发写 SQL；未建脚本/日志/临时产物。

### 1.1 十三文件实算

| 文件 | 行 | SHA256 | vs origin/main |
|---|---:|---|---|
| AccessGrantApplyRequest.java | 48 | 26907d4bdd81d538b6a2a08619919b1ab37ced7de85120b7015ffae531d0bea8 | IDENTICAL |
| AccessGrantReviewRequest.java | 12 | a12c80e8d3cd44db2ea5e0827d88666467307e3facf5e455cb85990e671d3d19 | IDENTICAL |
| AccessUserRequest.java | 36 | 51ffd29cb122c336d1a964f29d7572569ff09257d4ecf19c7b8a546f00cf0fb3 | IDENTICAL |
| PermissionTemplateApplyRequest.java | 13 | da109c37fefc054678c70ab15e7c739c86f280a9e814d16ed47012b4407d3090 | IDENTICAL |
| PermissionTemplateRequest.java | 19 | ce2fcc2dfe284bd2f847d1fcd85bbff1b910db71862f266630bdd639284a70ca | IDENTICAL |
| PositionRoleTemplateRequest.java | 12 | e154bff2c91cf9a9a7a5751dee44decd7904e6638d89dc67bcf36363fe088999 | IDENTICAL |
| RoleAssignmentRequest.java | 9 | 00e229f0511e24f168068827b6a39348b8d5a325ae9c1731e6d9a455cfe02ea9 | IDENTICAL |
| RoleRequest.java | 14 | bdd4a6297423cda981f62693a5a6879c6f08e50948d30933a8fc949bc460027b | IDENTICAL |
| StaffLevelAssignmentRequest.java | 12 | c569cd7da131ee7c7850ff3e8c368a3b80ed5505f49f50280b8c43a9a25fc031 | IDENTICAL |
| StatusChangeRequest.java | 11 | 6d4ac51fe553cb890897d6d609fb5b5eff5cb311dd6852978751457a4bcd3dd6 | IDENTICAL |
| TemporaryGrantRequest.java | 17 | 6ea363657bdb78e4f427d6b03592e9c4a00e7d930e908b79ad6d8276d47b2f49 | IDENTICAL |
| UserPermissionOverrideRequest.java | 13 | a871b6b46c7889e1cc020b8d37865dc5a92d7835f3a3a39281d287f11065bab0 | IDENTICAL |
| UserPositionAssignmentRequest.java | 13 | ae0f2cc0e0575b5196489347fe1428e21eea53e4675c22a111107a41d994f0d1 | IDENTICAL |
| **合计** | **229** | | |

全文阅读，不抽样。无 Bean Validation 注解；控制器 `@Valid` 对本组记录不产生字段约束，必填在 `AccessService`。

---

## 2. 第一性原理线（2.3 三问，逐文件）

业务上位需求以三 HTML 原文为准，不以 OpenAPI/注释为业务依据。这 13 个 record 是管理后台权限写口的请求体（浅模块：几乎没有实现，行为在 M031 Service）。

### 2.1 为什么必须存在（删掉则 HTML 哪条做不到）

HTML 原文（`docs/platform-governance-map-for-operations-v0.1.html:643-669`，判据定义区 PGM-040 有摘录，**PGM-040 未映射到 M033**，此处只作类型 1 上位需求，不扩锁）：

> 权限按单一功能最小颗粒度配置。支持角色级权限模板。支持员工级完全自定义配置。最终权限 = 角色权限 + 基础模板 + 有效临时模板 + 员工单独授予 − 显式拒绝；显式拒绝优先级最高。临时授权可设有效期或单次使用。平台管理员配置各岗位权限模板，并定「职位/层级对应哪个模板」；HR 部门定每个人的层级/职位。职级可按 M1-M10 配置。每个职级可分上、中、下。职级只绑定薪资大范围，具体薪资单独填写。

HTML 原文（`docs/business-system-map-for-operations-v0.1.html:698-749` BSM-028）：

> 管理员工、岗位、班次、职级、权限、工作设备和员工注册审批。发起单据时只需指定该事项的最终审批人，系统按组织层级自动补全……

| 文件 | 用途 | 删掉则做不到的 HTML |
|---|---|---|
| RoleRequest | 建角色+权限码+数据范围 | 角色级权限模板 / 单一功能权限点 |
| PermissionTemplateRequest | 建/改/复制基础或临时模板 | 基础模板 + 有效临时模板 |
| PermissionTemplateApplyRequest | 套用模板到多人 | 角色/模板批量配置 |
| PositionRoleTemplateRequest | 岗位×职级 → 角色+模板 | 「职位/层级对应哪个模板」 |
| RoleAssignmentRequest | 直接装配/取消用户角色 | 角色权限进入最终权限公式 |
| UserPositionAssignmentRequest | 定岗（路径覆盖 userId） | 保存岗位后按模板重算 |
| StaffLevelAssignmentRequest | 定职级+具体薪资 | 职级只绑薪资大范围，具体薪资单独填写 |
| TemporaryGrantRequest | 直发临时授权 | 临时授权有效期或单次 |
| AccessGrantApplyRequest | 高危/临时申请进主管链 | 敏感动作须审批；指定最终审批人 |
| AccessGrantReviewRequest | 批准/驳回申请 | 审批链推进 |
| UserPermissionOverrideRequest | grant / deny 员工级覆盖 | 员工单独授予 − 显式拒绝 |
| AccessUserRequest | 后台建权限用户 | 员工要有账号才能挂权限；与「两条注册路径」并存的管理端入口 |
| StatusChangeRequest | 岗位模板生命周期 / 临时授权收回 | 配置可存档删除（BSM-004 在映射外，BSM-003/028 权限分岗下的下线） |

答得上 ≠ 每个字段都有 HTML 锚点。字段级走 2.1 四分类。

### 2.2 声称的行为有没有被实际执行验证

**类型 4（jshell，Jackson 2.21.5，SNAKE_CASE + JavaTimeModule + FAIL_ON_UNKNOWN=false，classpath=`backend/target/classes`）：**

```
PHONE=null
USER=u1
PASS=secret-pass
TYPE=staff
ORG=org1
SERIAL={"idempotency_key":"k1",...,"password":"secret-pass",...,"consent_reference":null}
  （无 phone 键）
CAMEL_IDEM=null  CAMEL_TYPE=null  CAMEL_ORG=null
EV=[ev1]  FINAL=usr_boss  EXP=2026-12-31T00:00:00Z
MODE=replace  TPL=tplX
ROLE_EV=[ev9]  ROLE_SCOPE=project
```

另一次默认 FAIL_ON_UNKNOWN=true 的 SNAKE_CASE 探针：camelCase `idempotencyKey` → `UnrecognizedPropertyException`；已知属性列表含 `"phone"`（JsonIgnore 仍登记属性名，值丢弃）。

**类型 4 源码追踪（graft callers + 全文读 AccessService）：** 见 §4 消费矩阵。现有测试以 `new Record(...)` 走 Java 构造器，**不能**证明 JSON/`@JsonIgnore`（`createAccessUserStoresManualEvidenceAndProvisionsCredential` 用 Java 传入 phone 仍断言 `phoneHash()==null`，证明的是 Service 不读 phone，不是 Jackson）。

未在本席跑 Maven：主工作区 `AccessService`/`AccessServiceTests` 含未发布工作流（D-028/D-029），失败不得折算成本模块 finding。本模块 13 文件与 origin/main 相同，Jackson 行为以 jshell 对已编译 class 为准。

### 2.3 未经验证就当真的前提

1. 「`@Valid` 会按 OpenAPI required 拒请求」——这 13 个 record 无约束注解，前提不成立；真正校验在 Service。
2. 「OpenAPI `phone` 会写成 `phone_hash`」——DTO `@JsonIgnore` + Service 不调用 `request.phone()`，前提不成立（GR-01）。
3. 「OpenAPI `mode=replace|merge|preview` 会改变套用语义」——Service 只认 `single_use`，默认当 `append`（GR-02）。
4. 「请求体里的 `evidence_attachment_ids` 都会进审批/审计」——只在建用户、授权复核、岗位模板删除审批三条路径被读（GR-03）。
5. 「JsonAlias 让 AccessUserRequest 同时吃 camelCase」——别名是 snake 名，与 SNAKE_CASE 主名相同；camelCase 在 Boot 口径下静默丢（O-3）。

---

## 3. 顺从式：52 判据逐项

映射原文：`criteria-lock.md` L6412。每条先读定义全文（本席已读定义区对应节）及 HTML 原文。

| ID | 适用? | 理由（落到本 13 文件） |
|---|---|---|
| BEQR-157 | 无本面 | 锚点「员工状态＝待入职、在职、试岗、停岗、离职」。`AccessUserRequest.status` 经 `requireAccountStatus` 只认 `active/invited/disabled`（账号可登录态）。本模块无员工档案状态字段。 |
| BEQR-182 | 无本面 | 考勤日结果。无打卡/日结果请求体。 |
| BEQR-183 | 无本面 | 打卡方式。无。 |
| BEQR-184 | 无本面 | 请假类型。无。 |
| BEQR-185 | 无本面 | 请假单状态。无。 |
| BEQR-186 | 部分适用 | 薪资单构成公式不在本面。`StaffLevelAssignmentRequest`/`UserPositionAssignmentRequest`/`AccessGrantApplyRequest` 的 `baseSalaryAmountCent`/`salaryCurrency`/`salaryDetail` 承载「具体薪资单独填写」（HTML 职级规则）。币种消费方只认 CNY。 |
| BEQR-187 | 无本面 | 提成发放。无发放请求体。 |
| BSM-001 | 适用（背景） | 「谁能操作」是基础盘子。本组是权限写入口体，不实现整段经营闭环。 |
| BSM-003 | 适用 | 「权限分岗」。角色/模板/岗位/覆盖/临时授权请求体即分岗配置入口。 |
| BSM-009 | 适用 | 基础管理卡片含员工、权限。 |
| BSM-028 | 适用 | 「管理员工、岗位、职级、权限」；「发起单据只需指定最终审批人」对应 `AccessGrantApplyRequest.finalApproverId`（服务有读，契约缺键见 O-1）。 |
| BSM-029 | 适用 | 「用户权限要知道……职级和审批状态」→ 职级/岗位/授权申请体。设备/私人手机无本面。 |
| BSM-073 | 部分适用 | 商户专区员工功能大部分无本面。`AccessUserRequest.accountType` 含 `merchant`/`merchant_admin`。 |
| BSM-084 | 无本面 | 员工手机端自助。无自助请求体。 |
| BSM-085 | 部分适用 | 「新员工注册两条路径」对照 `AccessUserRequest` 是第三条（后台建号）。绑定/工单/代订无本面。 |
| BSM-086 | 无本面 | 排班考勤请假。 |
| BSM-087 | 部分适用 | 「具体薪资」字段在职级/定岗请求；工资条展示无本面。 |
| BSM-088 | 无本面 | 提成活动边界。 |
| BSM-114 | 适用 | 管理后台写入口的请求体。 |
| BSM-115 | 适用（背景） | 基础管理连权限。无票码/供应链字段。 |
| BSM-120 | 适用 | 「职级可按 M1-M10 上中下；具体薪资仍需单独填写」「权限按单一功能最小颗粒」。本 DTO 用 `staffLevelId`/`permissionCodes` 承载，不写死 M1–M10 字面（字面在职级主数据，非本段）。 |
| GEN-001 | 适用 | 死字段/冗余别名/未消费信封见 GR 与 O。 |
| GEN-002 | 适用 | JSON 绑定与 Service 语义须一致；jshell 已跑。 |
| GEN-003 | 适用 | 既有测试走 Java 构造器，未覆盖 `@JsonIgnore`；不把「测试绿」当 JSON 证据。 |
| GEN-004 | 适用 | 无 BV 注解；边界校验在 Service。首次缺必填走 Service 中文错误，非静默。 |
| GEN-005 | 适用 | 记录类自身不授权；门禁在 Controller（M031）。DTO 无伪造权限码的本地放行。 |
| GEN-006 | 适用 | 金额字段 `Long` 可空；负值由 Service 拒。本面无事务。 |
| GEN-007 | 适用 | 全组有 `idempotencyKey`；幂等哈希整对象（含 password 预像，见 O-6）。 |
| GEN-008 | 适用 | **本轮主伤面**：命名策略、OpenAPI 枚举、DTO 字段、Service 实读必须一致。GR-01/02/03。 |
| GEN-009 | 适用 | 契约字段静默丢弃＝伪成功。GR-01/02/03。 |
| GEN-010 | 适用 | phone JsonIgnore；password 进序列化串；审计 metadata 只记 `credential_provisioned` 布尔（`AccessService.java:1268-1276`）。 |
| GEN-011 | 适用 | evidence 收下不读则留痕缺口。GR-03。 |
| GEN-012 | 适用 | 无本面循环/连接；OpenAPI 对 list 有 maxItems，DTO 无对应约束（观察，首次超长由更下层或框架处理，不立项）。 |
| GEN-013 | 无本面 | 无构建脚本。 |
| GEN-014 | 无本面 | 无迁移。 |
| GEN-015 | 无本面 | 无 UI。 |
| GEN-016 | 适用 | 注释声称须可复现：`AccessGrantApplyRequest` L6、`UserPermissionOverrideRequest` L4 与 Service 一致；`AccessUserRequest` L3「文件版本 V0.01」无可执行行为。 |
| GEN-017 | 适用 | 本报告四分类。 |
| GEN-018 | 适用 | 结论落到 HTML / jshell / 源码行。 |
| PGM-001 | 适用（背景） | 治理层「再管资格：员工是否有权限」。 |
| PGM-008 | 部分适用 | 卡片「员工注册、岗位状态」。建号字段适用；手机/企微无本面。 |
| PGM-022 | 无本面 | 空间点位。 |
| PGM-024 | 无本面 | 低消/容量/库位。 |
| PGM-026 | 部分适用 | `organizationId`/`merchantId` 把账号绑到主体。招商线索无本面。 |
| PGM-032 | 部分适用 | 「员工不是只建账号」。本面是建账号请求；审核/私人手机/工作手机无本面。 |
| PGM-033 | 适用 | 注册两条路径 vs 后台 `AccessUserRequest`。Service 对非 `outsourced_vendor` 抛 `STAFF_REGISTRATION_REQUIRED` 属 M031 已闭环，本面仍暴露 OpenAPI 的 staff/sales 等枚举。 |
| PGM-034 | 无本面 | 工作手机/企微。 |
| PGM-035 | 无本面 | 提成发放。 |
| PGM-036 | 无本面 | 设备领用。 |
| PGM-048 | 部分适用 | 商户侧员工账号类型；不实现商户专区页面。 |
| PGM-059 | 适用 | 「手机号加密存放、界面脱敏」。本面 phone 被 JsonIgnore 且不落库；password 序列化见 O-6。 |
| PGM-066 | 适用 | 「权限最小颗粒」「有过程必须留痕」。permissionCodes 适用；evidence 丢弃见 GR-03。 |

**映射缺口观察（不 LOCK-AMEND）**：权限/职级正文锚点是 PGM-040 及子 h3，**未**出现在 M033 映射列表（与 M031/M032 同一组 52 项）。本席仍用该 HTML 原文作类型 1 上位需求，不把「映射未列」写成代码缺陷。

---

## 4. 消费矩阵（后端读不读）

Controller 一律 `@RequestBody` 收下整记录。下表「HTTP 体字段 → Service 是否读取」。路径参数覆盖的单独注明。

| 记录 | 字段 | Service 实读? | 证据 |
|---|---|---|---|
| AccessUserRequest | username, password, accountType, organizationId, merchantId, status, consentReference, evidenceAttachmentIds, operatorNote, idempotencyKey | 是 | `AccessService.java:1178-1228` |
| AccessUserRequest | **phone** | **否**（JSON 也进不来） | L14 `@JsonIgnore`；jshell `PHONE=null`；`createAccessUser` 无 `request.phone()` |
| PermissionTemplateRequest | templateName, templateType, permissionCodes, dataScope, validFrom, validUntil, singleUse, operatorNote, idempotencyKey | 是 | `templateFromRequest` L2279-2301 |
| PermissionTemplateRequest | **evidenceAttachmentIds** | **否** | `templateFromRequest` 未引用 |
| PermissionTemplateApplyRequest | targetUserIds, idempotencyKey, mode（仅 `single_use`） | 部分 | L214-241 |
| PermissionTemplateApplyRequest | **templateId** | **否**（用路径 `id`） | L220 `findTemplateById(id)` |
| PermissionTemplateApplyRequest | **evidenceAttachmentIds, operatorNote** | **否** | apply 方法未引用 |
| RoleRequest | roleName, description, permissionCodes, dataScope, idempotencyKey | 是 | L316-328 |
| RoleRequest | **evidenceAttachmentIds, operatorNote** | **否** | createRole 未引用 operatorNote/evidence |
| RoleAssignmentRequest | 全部 | 是 | assign/unassign；内部 `applyApprovedGrant` 再构造 |
| PositionRoleTemplateRequest | 全部（status 默认 active） | 是 | `savePositionRoleTemplate` L2092-2112 |
| StaffLevelAssignmentRequest | 全部；HTTP 体 `userId` 被路径覆盖 | 是（覆盖后） | Controller L269-276；OpenAPI 写明 user_id 不在体 |
| UserPositionAssignmentRequest | 同上 | 是（覆盖后） | Controller L288-296 |
| TemporaryGrantRequest | targetUserId, permissionCodes, validFrom, validUntil, singleUse, reason, idempotencyKey | 是 | `createTemporaryGrantDirect` L1551-1581 |
| TemporaryGrantRequest | **evidenceAttachmentIds, operatorNote** | **否** | Direct 只用 `reason` |
| AccessGrantApplyRequest | requestType 及按类型的 role/position/salary/permission/validFrom/expiresAt/singleUse/reason/finalApproverId/idempotencyKey/operatorNote | 是 | `applyGrant` L900-928 + build* |
| AccessGrantApplyRequest | **evidenceAttachmentIds** | **否** | apply/build 未引用；jshell 仍能绑 `EV=[ev1]` |
| AccessGrantReviewRequest | 全部 | 是 | L979-992 传入 SupervisorReviewApprovalRequest |
| UserPermissionOverrideRequest | 全部 | 是 | L1422-1450 |
| StatusChangeRequest | 生命周期：action/reason/idempotency/evidence（仅 approve/reject_delete） | 部分 | L503-548 |
| StatusChangeRequest | 收回临时授权：idempotency/reason | 部分 | L860-879 **不读 action/evidence**（URL 已是 revoke） |

向后兼容构造器：

- `AccessGrantApplyRequest` 16 参（无 finalApproverId）——测试仍调用，例如 `AccessServiceTests.java:1025-1028`。
- `AccessUserRequest` 11 参（无 consentReference）——`M006SecurityRegressionTests.java:221-222, 627-628`。

不是死代码。

幂等：`IdempotencyService.hashJson` → `objectMapper.writeValueAsString(request)` 再 SHA-256。jshell `SERIAL` 含 `"password":"secret-pass"`，不含 phone。审计 `accessUserAuditMetadata` 只放 `credential_provisioned` 布尔，不含口令/手机号。

---

## 5. 对抗式（grilling 自问自答）

❓ **Q1 — phone JsonIgnore 是安全加固还是契约破洞？**  
➡️ 两者同时成立。测试 `manualAccountDoesNotPersistOrReturnPhoneDigest`（`M006SecurityRegressionTests.java:219-223`）写明「无消费用途的重复手机号不能继续被 SHA256 摘要存储」。HTML 员工手机在注册/档案面（PGM-033/PGM-059），不是这个后台建号体必须收号。但 OpenAPI `AccessUserRequest.phone` 仍写「仅保存不可逆 phone_hash」。首次按契约带 `phone`：HTTP 200 路径上号被丢掉（jshell `PHONE=null`，SERIAL 无 phone）。这是静默丢字段，不是 400。**立项 GR-01。** 不据此发明「后台建号必须存手机号」。

❓ **Q2 — 实现 replace/merge/preview 算不算添加？**  
➡️ 算。HTML 只说模板可配、可套用，没有这三种模式。OpenAPI 枚举是实现发明。首次按契约 `mode=replace`：jshell 绑上 `replace`，`AccessService.java:241` 只把 `single_use` 当一次性，其余当默认 append。最小处置是 fail-closed 或把契约收到实际词，**不是**在本模块实现三套套用语义。**立项 GR-02。**

❓ **Q3 — evidence 丢弃是不是「可选字段所以无所谓」？**  
➡️ 可选不等于可以假装收下。HTML「人工只补原因和证据」（PGM-066/BSM-120 留痕条）。OpenAPI 在 Role/Template/GrantApply/TemporaryGrant 体里都列了 `evidence_attachment_ids`。复核口和建用户口真的读。申请/建角色/建模板/套用/直发临时授权不读。首次按契约夹附件：绑定成功（jshell `EV=[ev1]`、`ROLE_EV=[ev9]`），库和审批链没有这批 ID。**立项 GR-03。** 当前网页授权申请表单不传 evidence（只在复核传），不否定契约首次路径。

❓ **Q4 — 这些是否其实是 M031/M392 的事？**  
➡️ 读不读在 Service（M031）；契约在 OpenAPI（M392）；**注解和字段声明在本模块文件**。提示词明确要审「JSON 命名与契约」「接口收下却丢弃」。JsonIgnore 只能改本文件。不把 XM-M031-C（缺 6 个路径）重开。`final_approver_id` 是 DTO 有、契约无，DTO 方向正确，记 O-1 转 M392，不对本文件开删除。

❓ **Q5 — 未发布工作流会不会让本结论只在脏树上成立？**  
➡️ `cmp` 13 文件与 origin/main 相同。D-029 不适用。jshell 用的是当前 `target/classes` 的这些 record。

❓ **Q6 — camelCase 静默丢要不要立项？**  
➡️ 管理端 `normalizeRequestBody` 转 snake，首次正常走 snake。camelCase 丢是 Boot `fail-unknown=false` 的全局口径，不是这 13 个独有。AccessUserRequest 的 JsonAlias 也没补 camel 名。**不立项，O-3。**

❓ **Q7 — password 进幂等 JSON 预像？**  
➡️ 是。库只存 SHA-256。审计无明文。要排除口令会导致「只改口令的重放」撞幂等。**不立项，O-6。** 与 phone 不对称是对的：口令必须进站。

❓ **Q8 — 路径覆盖 userId 是丢弃吗？**  
➡️ OpenAPI 写明 user_id 由路径提供。首次按契约不带体字段。体里多带的 user_id 被 Controller 覆盖。安全方向（不能靠体改别人）。**不立项，O-4。**

❓ **Q9 — StatusChangeRequest.action 在 /revoke 不读？**  
➡️ URL 已经是收回。OpenAPI 共用 StatusChangeRequest 强制 action。传错 action 仍收回。要两三件事同时发生（共用 schema + 调用方以为 action 分流）。**不立项，O-12。**

❓ **Q10 — 无 BV 注解？**  
➡️ 首次缺 username 等走 Service `requireText`，有中文错误，不是静默。**不立项，O-10。**

---

## 6. 临时 findings

四分类顺序均为：冲突 → 细化 → 技术性 → 添加。冻结 ID 只引用锁内条目。

### GR-01 — AccessUserRequest.phone：契约可写，JSON 丢弃，服务不读

- **文件:行**：`backend/src/main/java/com/complex/platform/access/dto/AccessUserRequest.java:14`（记录组件 L8-19；紧凑构造仍接收 phone：L21-35）
- **原文**：
  ```
          @JsonAlias("account_type") String accountType,
          @JsonAlias("organization_id") String organizationId,
          @JsonAlias("merchant_id") String merchantId,
          String status,
          @JsonAlias("consent_reference") String consentReference
  ```
  上一组件为：`@com.fasterxml.jackson.annotation.JsonIgnore String phone,`
- **更长上下文**：同文件无其它 Jackson 忽略。全局 SNAKE_CASE。OpenAPI `docs/openapi/openapi-p0.yaml:31927-31930`：`phone`「后端不落明文，仅保存不可逆 phone_hash。非强制实名场景可留空。」`AccessService.createAccessUser` L1170-1226 组装 `AccessUser` 时不调用 `request.phone()`。
- **正常触发 / 影响**：管理端或按 OpenAPI 首次 `POST /api/v1/access/users`，JSON 含 `"phone":"13800138000"`。jshell（SNAKE_CASE+Boot 口径）`PHONE=null`，`SERIAL` 无 phone 键、有 `"password":"secret-pass"`。请求不当 400。既有测试用 Java 构造传入 phone 仍 `phoneHash()==null`（`AccessServiceTests.java:324-338`，`M006SecurityRegressionTests.java:220-223`）。调用方以为号已哈希入库，实际空。
- **最小拟处置**：与 M006-025/HTML 注册路径对齐：**本记录删除 `phone` 组件**（紧凑构造一并收），契约侧去掉 `phone`（M392，本模块不改 OpenAPI）。不要在本模块接「后台建号存手机号」——那是添加，且与现测试意图相反。
- **四分类**：非冲突（HTML 未要求此接口收号）。属 **细化**（员工账号/个人信息保护的下位实现）。不是纯构建技术性（DTO 字段有业务名）。不是添加。
- **工程缺陷是否成立**：**是**（GEN-008 / GEN-009 / GEN-010 / PGM-059）
- **冻结 ID**：GEN-008，GEN-009，GEN-010，PGM-059
- **依据类型**：4（jshell 输出）+ 1（PGM-059 HTML「手机号……加密存放」）+ 源码行（实现证据）

### GR-02 — PermissionTemplateApplyRequest.mode：契约三枚举，服务只认 single_use

- **文件:行**：`PermissionTemplateApplyRequest.java:11` `String mode`
- **原文**：
  ```
  public record PermissionTemplateApplyRequest(
          String idempotencyKey,
          String operatorNote,
          List<String> evidenceAttachmentIds,
          String templateId,
          List<String> targetUserIds,
          String mode
  )
  ```
- **更长上下文**：OpenAPI `openapi-p0.yaml:29439-29445` `mode` enum `replace | merge | preview`。消费：`AccessService.java:241` `boolean singleUse = template.singleUse() || "single_use".equalsIgnoreCase(trimToDefault(request.mode(), "append"));` 随后 `repository.applyTemplate(...)`。HTML 无 replace/merge/preview 词。
- **正常触发 / 影响**：首次按契约 `POST .../permission-templates/{id}/apply` 且 `"mode":"replace"`（或 merge/preview）。jshell `MODE=replace`。服务不走替换/合并/预览，只当非 `single_use` 的默认 append。调用方以为覆盖式套用，实际追加。
- **最小拟处置**：本记录不实现三套业务语义。**(1)** 消费方对非 `single_use`/`append`/空 直接 400；或 **(2)** 契约枚举收到实际词（M392）。本模块最小是让字段注释/取值与实读一致，避免无约束 String 承接未实现枚举。
- **四分类**：非冲突。**细化**（套用模板的下位参数）。实现三模式才是添加，本条不建议添加。
- **工程缺陷是否成立**：**是**（GEN-008 / GEN-009）
- **冻结 ID**：GEN-008，GEN-009，BSM-003
- **依据类型**：4（jshell `MODE=replace`）+ 源码 L241 + OpenAPI 作实现证据（非业务依据）

### GR-03 — 多条写请求的 evidenceAttachmentIds（及部分 operatorNote）收下不读

- **文件:行**
  - `AccessGrantApplyRequest.java:10` `List<String> evidenceAttachmentIds`
  - `RoleRequest.java:8` 同名
  - `PermissionTemplateRequest.java:9` 同名
  - `PermissionTemplateApplyRequest.java:8` 同名
  - `TemporaryGrantRequest.java:9` 同名（`operatorNote` L7 同样不读）
- **原文（申请体，代表）**：
  ```
  public record AccessGrantApplyRequest(
          String idempotencyKey,
          String operatorNote,
          List<String> evidenceAttachmentIds,
          String requestType,
          ...
  ```
- **更长上下文**：`applyGrant`/`buildUserRoleGrantRequest`/`buildUserPositionGrantRequest`/`buildTemporaryPermissionGrantRequest` 均不读 evidence。`createRole`、`templateFromRequest`、`applyPermissionTemplate`、`createTemporaryGrantDirect` 同。对照：`createAccessUser` L1224、`reviewGrant` L989、生命周期 `approve_delete` L548 **有读**。jshell：申请 JSON 的 `evidence_attachment_ids:["ev1"]` → `EV=[ev1]`；角色 JSON → `ROLE_EV=[ev9]`。
- **正常触发 / 影响**：首次按 OpenAPI 在建角色/建模板/套用/高危申请/直发临时授权时带证据 ID。HTTP 绑定成功，审批链与模板/角色行不出现这些 ID。HTML「所有有过程的业务动作都必须留痕……人工只补原因和证据」（PGM-066 / BSM-120）。
- **最小拟处置**：优先 **删除这些记录上未消费的 evidence 组件**（及 TemporaryGrant 未读的 operatorNote），契约同步（M392）。不要在本模块发明「申请单存证」表结构。若 B2 认定申请必须带证，那是 M031 接 `review` 同款传入，超出本 13 文件最小删除。
- **四分类**：非与 HTML 明文相反的实现（HTML 要求留痕，代码是缺接）。**细化**（留痕下位）。不是添加。
- **工程缺陷是否成立**：**是**（GEN-008 / GEN-011 / PGM-066）
- **冻结 ID**：GEN-008，GEN-011，PGM-066
- **依据类型**：1（PGM-066 HTML 留痕原文）+ 4（jshell 能绑定）+ 源码未引用

---

## 7. 观察（不立项 / 不重复 XM）

| ID | 内容 | 为何不立项 |
|---|---|---|
| O-1 | `AccessGrantApplyRequest.finalApproverId` 服务有读（`applyGrant` L928），jshell `FINAL=usr_boss`；OpenAPI `AccessGrantApplyRequest`（约 L36584-36668）无 `final_approver_id`。HTML「只需指定最终审批人」。当前网页申请表单也不传。 | DTO 方向正确。缺键在契约模块。**转 M392**，不删本字段。非 XM-M031-C（那是缺路径）。 |
| O-2 | `UserPermissionOverrideRequest` 及 overrides 两路径不在 OpenAPI | **XM-M031-C** 已登记 |
| O-3 | 仅 `AccessUserRequest` 有 `@JsonAlias("idempotency_key")` 等 snake 别名；与 SNAKE_CASE 主名相同。camelCase 在 Boot 口径静默丢（`CAMEL_IDEM=null`）。前端已转 snake。 | 首次正常不走 camel。别名可删（ponytail），无首次伤害。 |
| O-4 | 职级/定岗体含 `userId`，Controller 用路径覆盖 | OpenAPI 已写「不在请求体」。 |
| O-5 | 两紧凑构造器仍被测试调用 | 非死代码。 |
| O-6 | 幂等 `writeValueAsString` 含明文口令预像；审计无明文 | 哈希入库；排除口令会让改口令重放撞键。 |
| O-7 | D-029 / W-1…W-4 / XM-M031-A | 13 文件与 origin/main 相同，不适用。 |
| O-8 | `UserScopeChangeRequest.java` 同目录 | 非本模块 FILES。 |
| O-9 | PGM-040 未映射到 M033 | 计划问题，本席不 LOCK-AMEND。 |
| O-10 | 无 BV，`@Valid` 空转 | Service 有中文校验，非静默。 |
| O-11 | `expiresAt` vs `validUntil` | 各自契约一致，不是同一 schema。 |
| O-12 | `/temporary-grants/{id}/revoke` 不读 `action` | URL 已表达动作。 |
| O-13 | `PermissionTemplateApplyRequest.templateId` 与路径重复，体不读 | 首次正常两处填同一 ID，无伤害。 |
| O-14 | `templateFromRequest` 把模板内部 mode 写死 `"merge"`（L2290） | 在 AccessService（M031），不是本记录字段。 |

ponytail-review：`AccessUserRequest.java:14 delete: JsonIgnore 后的 phone 组件`；`AccessUserRequest.java:9-18 shrink: 与 SNAKE_CASE 同名的 JsonAlias`；GR-03 五处 evidence `delete: 无消费方的信封字段`。`net: 约 -20 行（若 B2 采纳删除）`。

---

## 8. 文件覆盖清单（无 finding ≠ 未审）

| # | 文件 | 覆盖动作 | 本文件临时 GR |
|---|---|---|---|
| 1 | AccessGrantApplyRequest.java | 全文；申请路径；jshell；16 参构造器调用点 | GR-03；O-1/O-5/O-11 |
| 2 | AccessGrantReviewRequest.java | 全文；reviewGrant 全字段有读 | 无 |
| 3 | AccessUserRequest.java | 全文；jshell；createAccessUser | GR-01；O-3/O-5/O-6 |
| 4 | PermissionTemplateApplyRequest.java | 全文；applyPermissionTemplate | GR-02，GR-03；O-13 |
| 5 | PermissionTemplateRequest.java | 全文；templateFromRequest | GR-03 |
| 6 | PositionRoleTemplateRequest.java | 全文；savePositionRoleTemplate 全读 | 无 |
| 7 | RoleAssignmentRequest.java | 全文；assign/unassign/内部再构造 | 无 |
| 8 | RoleRequest.java | 全文；createRole | GR-03 |
| 9 | StaffLevelAssignmentRequest.java | 全文；StaffService L233 再构造；路径覆盖 | O-4 |
| 10 | StatusChangeRequest.java | 全文；lifecycle vs revoke | O-12 |
| 11 | TemporaryGrantRequest.java | 全文；createTemporaryGrantDirect | GR-03 |
| 12 | UserPermissionOverrideRequest.java | 全文；effect grant\|deny 与注释一致 | O-2 |
| 13 | UserPositionAssignmentRequest.java | 全文；定岗/内部再构造 | O-4 |

---

## 9. 步骤结论

**FAIL**

同时满足：13/13 全文覆盖；52/52 判据有适用或无本面及理由；三线均执行；声称有类型 1 或 4 证据；无 questions 文件（B1 不问添加）。不满足「无未定稿缺陷」：GR-01/02/03 工程缺陷成立，须 B2 归并定稿。Grok FAIL 否决单票通过。

本席不修复、不改计划/findings/ledger、不进入 B2 以外的步骤。

## M033 B2-r1 整批候选复核（席乙票，V0.02）

- 时间：2026-09-21T07:31:20Z。席位：乙 / Grok 4.6 xhigh / 只读外审，正本 `/home/james/complex-management` main 主工作区。
- 输入：`modules/M033-b1-grok-raw.md` V0.01（35607 bytes，SHA256 `e9097cb55ae9e1f8e2221aed1cd3376f6a043b870a853c0e6566f2ffa941a771`，本席 B1：FAIL，GR-01、GR-02、GR-03，观察 O-1…O-14）；`modules/M033-b1-self-raw.md` V0.01 封存前缀（24360 bytes，SHA256 `b4fe55ed697658aefdcb38d11127a8d8e5beb442b35dc58ad9ddb5d60b119982`，席甲 B1：PASS，OBS-1…7）。席甲 raw 现文件更长（25755 bytes，全文 SHA `5f01f3dd7d617f634b65b2f7dbc752aab2c396d9d43fece548e638cd774f8d80`），本票只读该 24360 字节前缀，未通读、未采信其后追加的 B2 结论段。
- 候选：`docs/audit-2026-09/findings.md` V3.03 顶部「当前 M033 B2-r1 整批候选」段（全文件 SHA256 `d6b3b353e90dbf779907d94cfdd6695008a0166ad8b3e70d6abaea61e3dc94be`）。B1 后四值仍全等 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（本席本步只读复核，不写新的独占 CRITERIA-LOCK-SHA256 行）。
- 范围：只审本批归并 / 分类 / 队列 / 最小修复边界，不重新审模块。未改源码 / 文档 / HTML / lock / findings / ledger；未 fetch / commit / push / merge / deploy；未写新产物；未对任何数据库执行写语句；未用 MCP 或子代理。
- 本席 B1 拟处置（删除 DTO 组件）与席甲候选（13 个文件不改、登记转 XM）只在处置上分歧。本票按「改了本模块 13 个文件之后，首次正常使用下可观察行为会不会变对、且不触碰已闭环模块」逐条独立核席甲技术理由。
- 本席 B2-r1 票：**PASS**（整批候选同意）。

### 完整差集

| 正式ID / 登记号 | 本次来源（不改写原意） | 候选分类 / 缺陷 / 队列 | 本席票 |
|---|---|---|---|
| M033-001 | 本席 GR-01 ＝ 席甲 OBS-3 | 细化 / 是 / 否（XM→M392） | **同意不入本模块队列**；撤回本席 B1「本记录删除 phone 组件」的最小拟处置 |
| M033-002 | 本席 GR-02（席甲 B1 漏契约枚举对照） | 细化 / 是 / 否（XM→M392） | **同意不入本模块队列**；撤回本席 B1「本模块改取值/注释与实读对齐」作为可入队修复 |
| M033-003 | 本席 GR-03 ＝ 席甲 OBS-1 | 细化 / 是 / 否（登记） | **同意不入本模块队列**；撤回本席 B1「删除五处未消费 evidence 组件」的最小拟处置。对席甲「HTML 未点名授权类动作」作非阻断事实订正，不升队 |
| XM-M033-A | 001 契约侧 | →M392：`AccessUserRequest.phone` 描述与实现不符 | **同意** |
| XM-M033-B | 002 契约侧 | →M392：套用 `mode` 枚举与后端、管理端实际取值不符 | **同意** |
| XM-M033-C | O-1 ＋ O-2 ＋ 003 契约侧 | →M392：缺 `final_approver_id`；`UserPermissionOverrideRequest` 无 schema（同 XM-M031-C）；对不落证据的端点仍宣称 `evidence_attachment_ids`。003 兼 →M031（若要把请求体证据接到审批链） | **同意登记**；不升本模块队列 |
| （无新 ID） | 两席其余观察 | 只登记不入队 | **同意；无应升队项** |

冲突入队 0。细化入队 0（001/002/003 均登记、队列「否」）。技术性入队 0。添加 0。questions 0。无 HTML 修改申请。修复队列「是」= 0。本模块写入集合为空。席甲 B1 PASS 撤回后合取 FAIL，与候选一致。按 V0.17：空队列跳过 B3/B4，直接进 B5；B5 双 PASS 仍是收口条件。

**整批候选：同意。**

---

### 一、差集核实（逐项独立）

**一、001（GR-01 / phone）。票：同意席甲不删组件、不入本模块队列；撤回本席 B1 删除拟处置。**

独立核源（不重审模块）：

1. `AccessUserRequest.java:14` `@JsonIgnore String phone`；L21-35 十参便捷构造仍接收 `phone` 并转发到规范构造。`AccessService.createAccessUser` L1170-1238 **零处** `request.phone()`；`new AccessUser(...)` L1213-1226 亦无手机号参。
2. 已闭环 `M006-025`（findings 台账：冲突；工程缺陷是；队列否；标题「手工建号把手机号存为无密钥SHA-256并经AccessUser返回」）。拟修原文：优先收窄「没有真实消费用途的重复手机号采集/摘要暴露」。`M006SecurityRegressionTests.java:219-223` 注释写明「M006-025」；`new AccessUserRequest("phone", null, List.of(), "vendor", "password", "13800000000", ...)` 后 `assertThat(created.phoneHash()).isNull()`。该十参把手机号放在 password 之后，删记录组件则该调用点不能按现签名编译。该测试文件不在本模块 13 FILES 内。
3. HTTP 未知字段：`application.yml:15-16` 仅 `spring.jackson.property-naming-strategy: SNAKE_CASE`，无 `fail-on-unknown` / `FAIL_ON_UNKNOWN_PROPERTIES`。`backend/src/main` 与 `apps` 生产代码 **零处** 打开该闸；仅两份测试对 `new ObjectMapper()` **关掉**它。仓内无 `Jackson2ObjectMapperBuilder` 定制。Spring Boot 构建器默认 `FAIL_ON_UNKNOWN_PROPERTIES=false`（与本席 B1 jshell Boot 口径一致）。删掉组件后，按契约仍发 `"phone"` → 仍是未知字段被忽略，HTTP 仍 200、仍不落库。
4. 说谎句在契约 `openapi-p0.yaml:31927-31930`：「后端不落明文，仅保存不可逆 phone_hash」。该文件属 M392（`plan/modules.md` 路径表）。本模块 13 文件不含 OpenAPI。
5. 本模块内改法对照「首次正常使用可观察行为变对」：删组件（即使另留丢弃重载以保住测试编译）→ 调用方按契约带 `phone` 的 HTTP 路径可观察结果不变。去掉 `@JsonIgnore` 让字段进站 → Service 仍不读，且与 M006-025 收窄采集的方向相反。在本 DTO 把手机号接进哈希/加密 → 必须改已闭环 `AccessService`/`AccessUser`，并推翻 M006-025。**没有不触碰已闭环模块、且能让「按契约传手机号」从伪成功变成正确行为的本模块改法。** 正确处置是契约去掉 `phone` 或改写为「本接口不接收手机号」。

**明确同意**候选：001 细化 / 缺陷是 / 队列否；XM-M033-A → M392。撤回本席 B1「本记录删除 phone 组件」。

**二、002（GR-02 / mode）。票：同意不入本模块队列；对「400 必须改 AccessService」作非阻断订正，结论不变。**

独立核源：

1. DTO `PermissionTemplateApplyRequest.java:11` `String mode`，无约束、无注释。
2. 消费 `AccessService.java:241`：`boolean singleUse = template.singleUse() || "single_use".equalsIgnoreCase(trimToDefault(request.mode(), "append"));` 随后 `repository.applyTemplate(...)`。只认 `single_use`，其余（含空、`append`、`replace`、`merge`、`preview`）当追加。
3. 现行管理端 `apps/web/src/admin/pages/PermissionTemplatesPage.tsx:328`：`mode: singleUse ? "single_use" : "append"`。首次正常管理端路径不发契约三枚举。
4. 契约 `openapi-p0.yaml:29439-29445` enum `replace | merge | preview`。三方里只有契约取值集与实现/管理端不一致。该文件属 M392。
5. 对不认识的 mode 返回 400：控制器 `AccessController.java:67-69` 已有 `@RequestBody @Valid`。DTO 加 Bean Validation 约束、不改 AccessService 源码，也能让 `@Valid` 对 `replace` 走 400。席甲「400 要改已闭环 M031 的 AccessService」作为**唯一**路径不成立。
6. 但该 400 **不是**「首次正常使用行为变对」：按契约发 `replace`/`merge`/`preview` 会从「200 + 静默追加」变成「400」，并不是替换/合并/预览被执行；管理端 `single_use`/`append` 路径可观察结果不变。在 DTO 实现三套套用语义是添加，且必须改 `AccessService.applyPermissionTemplate`（已闭环 M031）。只加字段注释：无可观察行为变化，也不满足 GEN-016（此处无现在时假注释）。**本模块 13 文件内没有能让套用语义按契约变对、又不触碰 M031/M392 的改法。**

**明确同意**候选：002 细化 / 缺陷是 / 队列否；XM-M033-B → M392（枚举收到实际词 `single_use` / `append`）。撤回本席 B1 把「本模块改取值/注释」当作可入队最小修复。非阻断：400 不是必须改 AccessService，但 400 也不能让本条入本模块队列。

**三、003（GR-03 / evidenceAttachmentIds）。票：同意不删五组件、不入本模块队列；对「HTML 未点名」作非阻断事实订正。**

独立核源：

1. 五处声明与本席 B1 一致：`AccessGrantApplyRequest.java:10`、`RoleRequest.java:8`、`PermissionTemplateRequest.java:9`、`PermissionTemplateApplyRequest.java:8`、`TemporaryGrantRequest.java:9`。Service 对这五条写路径不读该访问器（对照有读：`StatusChangeRequest` L548、`AccessGrantReviewRequest` L989、`AccessUserRequest` L1224）。
2. 信封计数（本席实算，非抄候选）：`backend/src/main/java` 下 `*Request.java` 共 383 个，其中声明 `evidenceAttachmentIds` **135** 个；`openapi-p0.yaml` `components.schemas` 共 229 个，属性含 `evidence_attachment_ids:` 的 **96** 个。与候选逐字相符。只从这 5 个 DTO 删除，本模块写口将与全项目信封及契约 96 schema 不一致；契约仍宣称可传。
3. 删与不删对 HTTP 调用方：同上，Boot 口径忽略未知字段。按契约夹 `evidence_attachment_ids` 仍 200、仍不落这批 ID。
4. `AccessService.java:1809-1819`（已闭环 M031）`new TemporaryGrantRequest(idempotencyKey, request.reason(), List.of(), request.targetUserId(), request.permissionCodes(), request.validFrom(), request.expiresAt(), request.singleUse(), request.reason())` —— 九参，第三参即 `evidenceAttachmentIds`。主代码仅此一处 `new TemporaryGrantRequest(`。删该组件必须改 M031 源文件。V0.17 闸 3「diff 逐行归因……不设例外清单」：已闭环模块没有「构造器参数个数微调」豁免（候选写「10.4」；本席在 V0.17 / runbook 未找到字面「§10.4 微小改动豁免」，按闸 3 无例外清单 + 本模块写入集合不含 `AccessService.java` 核实质，不另加判据）。主代码无 `new RoleRequest(` / `new PermissionTemplateRequest(` / `new PermissionTemplateApplyRequest(` / `new AccessGrantApplyRequest(`；即便只删其余四组件，HTTP 可观察结果仍不变，且破坏信封一致。
5. HTML：席甲「『人工只补原因和证据』未点名授权类动作」**过窄**。治理地图 `platform-governance-map-for-operations-v0.1.html:656`「权限的授予、收回、到期、单次消耗、失效全部留痕」；同文件 `:899` 必须留痕动作清单含「权限变更、临时授权和敏感权限使用」。PGM-066 锁原文（`:962` / 锁 L6359）同时写明：该「人工只补原因和证据」句**是操作指引，不作为合规判定依据**；留痕是否完备以「所有有过程的业务动作都必须留痕」为准。订正止于此：HTML **点名了**授权类留痕。这不把 003 送进本模块队列——删 DTO 组件不能让附件进审批链或事件；把请求体证据 ID 接到已闭环 `AccessService` 的申请/建角色/建模板/套用/直发临时授权路径，超出本 13 文件，且与「复核口已经读证据」不是同一条接线。候选把「真的存证」标成添加项，作为 M033 本批 2.2 题目过宽（本批无添加、不发问）；是否在 M031 用既有审计事件已满足「授予留痕」、抑或还要把信封 ID 入库，留给 XM-M033-C → M031，不在本模块 B3。

**明确同意**候选：003 细化 / 缺陷是 / 队列否；登记并转 M392（契约不应对不落证据的端点宣称该字段）与 M031（接线若要做）。撤回本席 B1「优先删除这些记录上未消费的 evidence 组件」。TemporaryGrant 未读的 `operatorNote` 仍只登记（信封前三件套，1809 已用 `reason` 填入），不另开 ID。

**四、空队列与跳过 B3/B4。票：同意。**

三条均「有工程缺陷」但「本模块修复队列否」。V0.17 写死「冲突 / 有工程缺陷的 → 是」指**能在本模块写入集合落地的**缺陷；跨模块契约/已闭环服务的有效修复不进本模块 B3（否则 B3 只能改出无可观察差别的 DTO，或越界改 M031/M392）。先例：M006-025 自身即「缺陷是 / 队列否」。本模块写入集合为空 → 跳过 B3、B4（B3 次数不加），直接 B5；B5 双 PASS 才收口。同意。本票不是模块 PASS，不是授权跳过 B5。

**五、其余观察。票：全部只登记；无漏并须升队项。**

候选已覆盖：`PermissionTemplateApplyRequest.templateId`、`PermissionTemplateRequest.sourceTemplateId` 后端不读（席甲 OBS-2；本席 B1 有 O-13，漏 `sourceTemplateId`，候选补登，同意）；两向后兼容构造器（O-5）；toString 含口令、控制器/服务无日志点、幂等只存含随机键的请求 JSON 的 SHA-256（O-6；密码进预像仍不立项）；无 BV、`@Valid` 空转（O-10）；仅 `AccessUserRequest` 与 SNAKE_CASE 同名的 `@JsonAlias`（O-3）；职级/定岗体 `userId` 被路径覆盖（O-4）；`/temporary-grants/{id}/revoke` 不读 `action`（O-12）；`expiresAt` 与 `validUntil` 分属不同 schema（O-11）；映射未含 PGM-040（O-9，不 LOCK-AMEND）。O-1 / O-2 进 XM-M033-C。O-7（D-029 不适用：13 文件与 origin/main 相同）与 O-8（`UserScopeChangeRequest` 非本 FILES）不进本批台账，正确。本席 O-14（`templateFromRequest` 内部 mode 写死 `"merge"`，AccessService L2290）属已闭环 M031，不是本记录字段，不升本模块队列、不另开 XM。无应升队项。

**六、本批无添加项、无 HTML 申请、无 questions。票：同意。**

`docs/audit-2026-09/questions/` 无 `M033*`。001/002/003 不把「实现三模式 / 后台建号存手机号 / 申请单存证表」当成本批添加去 2.2。不 BLOCKED-ASK。不申请改三 HTML。

---

### 二、分类理由

四分类顺序：冲突 → 细化 → 技术性 → 添加。

- **001 细化。** 能指到 PGM-059「手机号……加密存放」的下位实现（本接口不收号、不落摘要）。不是与 HTML 不能同时成立的冲突句（HTML 未要求此后台建号体必须收号；收号并无密钥 SHA-256 反而是已删的 M006-025 冲突）。不是添加。同意采本席 B1 分类。缺陷成立：GEN-008（契约描述与实现不符）；PGM-059 作上位。GEN-009 静默丢字段的伤害在契约被 M392 改掉之前仍在，但不把「删 DTO 组件」写成能消除该伤害的本模块修复。
- **002 细化。** 套用模板的下位参数。HTML 无 replace/merge/preview 词；实现那三套语义才是添加。同意。缺陷成立：GEN-008。有效修复在 M392。
- **003 细化。** 写请求信封 / 留痕下位，不是与 HTML 明文相反的实现。同意不升冲突、不升添加（本批不问「是否必须把这五条写口的附件入库」）。缺陷成立：GEN-008 / GEN-011（契约宣称可传、实现不读）。有效修复不在本 13 文件。

工程缺陷 001/002/003 均为「是」。队列三「否」零「是」。

---

### 三、最小修复边界

本模块写入集合＝**空**。一次 B3 禁止改这 13 个记录类、禁止改 `AccessService.java` / `AccessController.java` / `M006SecurityRegressionTests.java` / `openapi-p0.yaml` / 三 HTML / 锁。XM-M033-A/B/C 不在本模块 B3 执行。

| ID | 允许（本模块 B3） | 禁止 |
|---|---|---|
| 001 | 无 | 删 `phone` 组件；去掉 `@JsonIgnore`；在本 DTO/Service 接回哈希或加密存储；改 M006 回归测试签名冒充本模块修复 |
| 002 | 无 | 在 DTO 实现 replace/merge/preview；加 BV 让契约枚举 400 却声称行为已变对；改 AccessService 对未知 mode 抛 400 并算本模块修复 |
| 003 | 无 | 只删这 5 个 `evidenceAttachmentIds`；改 L1809 九参构造；在本模块发明申请单存证表 |

---

### 四、添加／HTML／questions

无添加。无 HTML 修改申请。无 questions。不 BLOCKED-ASK。

---

### 分类／队列／允许的最小修复边界（收口）

本席 B2-r1 **PASS**。整批候选同意。定稿仍须席甲本批 B2 PASS 合取。未经双 PASS 禁止把空队列当模块收口；下一步是 B5（跳过 B3/B4）。

- 001：细化／是／否。同意不删 phone。JsonIgnore＋十参构造是 M006-025 的回归抓手；删与不删 HTTP 无可观察差别；说谎的是契约 L31927-31930（M392）。
- 002：细化／是／否。同意不改 DTO。管理端 L328 发 `single_use`/`append`，Service L241 只认 `single_use`，只有契约三枚举不对。400 不是必须改 AccessService，但也不能让套用语义变对。
- 003：细化／是／否。同意不删五组件。信封 135/96 计数核实；L1809 九参核实；无已闭环模块构造器微调豁免。HTML 其实点名了授权类留痕（L656、L899），「未点名」不成立，但不构成入本模块队列的理由。
- XM-M033-A/B/C 同意登记。观察全部只登记，无升队。无添加、无 HTML 申请、无 questions。

### 结论

全批差集无原意改写。GR-01→001、GR-02→002、GR-03→003，分类采本席细化，缺陷「是」，队列「否」——因为本模块 13 文件内不存在「改完之后首次正常使用可观察行为变对、且不触碰已闭环模块」的最小改法。本席 B1 三条删除/就地对齐拟处置撤回，改为同意候选整批。空队列跳过 B3/B4，B5 仍须双 PASS。本席 B2-r1 **PASS**。本票不是模块 PASS。步骤仍 B2；M033 IN_PROGRESS；B3=0。
