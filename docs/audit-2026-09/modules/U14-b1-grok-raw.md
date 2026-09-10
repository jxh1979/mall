CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U14 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 只读全量三线（未发布材料第 14 段；不做 B3/B4/B5；处置写给发布者，本轮不修源）
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 段：U14 — 未发布材料第 14 段「完整接口定义」
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 末尾附录；`plan/runbook.md` V0.02 §11；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 审计对象（不得冒称 main 或已发布修复）：
  - 未发布提交：`03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb`（本仓 `git cat-file -t` **无此 commit 对象**）
  - 未发布树：`e28dad059b2c7e08bcc95a8db18552d3f76edf4b`（`git cat-file -t` = **tree**，可解析）
  - 发布基线：`242f487233d028173cb9198eecd079f5d877405c`（commit，可解析）
  - 只读输入根：`/home/james/complex-management/.scratch/module-recheck-20260908/unpublished-03acdb98`
  - 清单：`.scratch/module-recheck-20260908/unpublished-03acdb98-input.json` V0.01，76 文件
- 本段：1 文件，物理行 51250，标记 **OVERSIZE-SINGLE-FILE**；main 主归属：**M391**（`docs/openapi/openapi-p0.json`）
- 冻结判据：M391 映射行 L6770 并集 **18** 项 = GEN-001–GEN-018（相对开工提示 MISSING=[] EXTRA=[]）；定义区从锁全文读过
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 且发布前须修：GR-U14-001、GR-U14-002、GR-U14-003、GR-U14-004、GR-U14-005、GR-U14-006、GR-U14-007、GR-U14-008
- 添加项（B1 只登记，不代用户回答 2.2）：**1 条 = GR-U14-002**，即 `POST /api/v1/platform/merchants/{merchant_id}/self-operated`（`setMerchantSelfOperated`）独立自营布尔写口。操作员 2026-09-09 已对 `questions/U04-001.md` 答「删除」；本段是该添加的契约面，**不新开 2.2、不再问**。无其它新添加。
- 同根已定稿、不新开 2.2：U04-001 / U05-001 / U05-003 / U05-005 / U05-006 / U05-008 / U05-011 / U07-004 / U08-001 / U08-003 / U13-001
- 只登记、建议队列「否」：观察见 §8
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做写操作
- 本报告不表示全仓、main 模块或发布门禁通过。M001–M012 检查点已合入当前 HEAD（PR #428/#429）。原 322/471 模块结论不继承到本未发布段。不得把本副本冒称 main 源码或已发布修复。
- 本席独立实跑（隔离 `/tmp`，不改副本/main）：未发布 json+yaml 同树 `verify-openapi-contract.mjs` **PASS**（730 ops）；main json+yaml **PASS**（720 ops）；只把本段 JSON 覆到 main YAML：`verify-openapi-contract.mjs` **FAIL**（39 处 json↔yaml 深不等）；未发布 JSON + main 矩阵：`generate-openapi-operation-matrix.mjs --check` **FAIL**（矩阵与 OpenAPI 不同步）。副本自洽 **不能** 消解 HTML 冲突或对 main 的契约分叉。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`（SHA256 `fa5c1e5ee76b1c8f1ae56101f52c9e239de75d5c578adc61227b92d10b7e52ef`） | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md`（SHA256 `b9339b09ee3980808d8c5a35c7251b891b8b1e0036ec4ca37812b976ebddf6b6`） | 隔离 `/tmp` 叠 JSON/YAML 复现 39 处漂移与矩阵红 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md`（SHA256 `808fc5717aa88ad65efff312b11c186294d3e6ee301afb584e2f86599b137787`） | 只借读错误→复现→追数据流（本段 JSON → U05/U07/U08/U15 副本 → main 对照 → HTML） |
| verification-before-completion | superpowers 同名（SHA256 `2befe7fc55bcadaa3d97dd9e8efeb633d2561c0ebe74c5a8b17c4d9e7e4520b3`） | 先跑命令再声称；副本 verify 绿不能写成 HTML 通过 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md`（SHA256 `40df33b58fc6ef889b93585733feb9566b76e9586efa7f376785c1e995197ac0`） | 只列可删复杂度（独立自营写口、死字段 `device_fingerprint`） |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md`（SHA256 `a842323e664e5af104eac5c97ad22fda929ebeb62d81c501161ac1f6f482db58`） | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md`（SHA256 `6f9ae7c1c295d7e67eda1e3a616c337c057f45f1a5bf2955efb07d539a655b79`） | 本段验证均秒级，未起 monitor |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：套件文档写明已并入 `grilling`，无独立技能可装
- `ubiquitous-language`：本机技能树未找到
- Arc `audit` 会派评审子代理：与双席/§7 冲突，**不调用**
- `using-superpowers` 要求技能优先于探索：与「禁止子代理 / 本提示指定 controller」冲突处，按用户指令收窄

明确不用：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、MCP、第三主体。

内部 grilling 树（自答，未问操作员）：

1. 这 1 个 51250 行文件必须存在吗？必须有**等价的 P0 正式契约**（paths + components + x-permission/x-auth-context/x-terminal）。删掉它，M391 的编码前置契约、springdoc 比对和 `verify:openapi` 没有本段这一层。文件作为契约是细化，不是添加。未发布增量（+10 operation / 改 8 个既有 staff 口）才是本段要审的业务面。
2. 整段按现文件叠进当前 main、首轮会不会过门禁？**只叠 JSON 不会。** main YAML 仍是基线，`verify-openapi-contract.mjs` 报 39 处深不等（CMD-JSONONLY EXIT 1）；main 矩阵 `--check` 红（CMD-MATRIX EXIT 1）。json+yaml 同批在 `/tmp` 可绿，只证明未发布树自洽。
3. 本文件会不会覆盖 main 已修的 M009–M012？**对本路径不会回退。** `git rev-parse` 显示 main 该 blob == 基线 blob `bfa444d6…`，`git log 242f4872..HEAD -- docs/openapi/openapi-p0.json` 空。对抗式固定问题的真实形态是：整文件覆盖会把已定稿删除的自营写口和公开无设备注册契约**写进**当前 main，而不是回退 Java 侧 M009 修复。
4. 公开 `POST /staff/self-registrations` 无设备字段、`security: []`，首轮正常客户端按契约编码就会发出无绑定的明文手机号。不是两三件不巧。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 内容同值
- `criteria-lock.md` 版本 V0.11；判据总数 401
- M391 映射行 L6770：`[GEN-001 … GEN-018]` 共 18 ID，与开工提示逐项相同；定义区 §general L12–L100 均从锁全文读过
- HTML 出处：本段映射无 BEQR/BSM/PGM ID；分类仍按 GEN-017 对照三份 HTML 正本原文（见各 finding 引用行）。不把 HTML 锚点 ID 写入本段冻结判据格。

HEAD（独立实算）：

- `git rev-parse HEAD` = `git rev-parse origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`
- `git log -1 --oneline`：`445a49f4 Merge pull request #429 from jxh1979/audit/20260909/M011-M012-checkpoint`
- 本席对照 main 已修项时以 **当前 HEAD=445a49f4** 为准（含 M001–M012）
- 本席未设 `GIT_INDEX_FILE`，未动主索引

ledger 只读核验（不改）：V2.83，517173 bytes，SHA256 `26bf86fcf47afee8e8ff3deada7de6a7e9cde6ac11c89f614c796b40d0015211`（与开工提示一致）。findings V1.69，660692 bytes，SHA256 `d7772ad98cf91d7353372e4fef586954eb3416f9888312e935c08aa59e517e2d`（与开工提示一致）。台账中与本段直接相关的已定稿项（背景，不继承 PASS）：

- U00-000：未发布树相对 242f4872 全 76 文件与 main 分叉
- U04-001：独立自营布尔 / `setMerchantSelfOperated` 已答删除
- U05-003：自助注册须带设备公钥/指纹
- U05-005：可见天数缺行不得空上下限
- U05-006：柜机双身份
- U05-011：employment-status 与 shift-visible-days 写口缺幂等键
- U08-003：OpenAPI `setMerchantSelfOperated` 随 001 删除
- U13-001：安全基线钉未发布 OpenAPI SHA `f8de4f8c…`、730 ops / public 7（main 720/5）

### 1.1 输入副本与树对象

| 项 | 值 |
|---|---|
| 副本路径 | `.scratch/module-recheck-20260908/unpublished-03acdb98/docs/openapi/openapi-p0.json` |
| 副本 SHA256 | `f8de4f8c2957953911030512a3502b6a5d8bdc4ffd57dc6d3c4049a0df788384` |
| 清单条目 | 同 SHA、51250 行、1696081 bytes；本席 `sha256sum`/`wc` 与清单一致 |
| `git hash-object` 副本 | `0cd43e73050c947a13e72a0be1f0ca92ecba2978` = `e28dad05:docs/openapi/openapi-p0.json` |
| 基线 blob | `bfa444d6f1f1f6601a037b83a08c39585aacfbd6` |
| main HEAD blob | `bfa444d6f1f1f6601a037b83a08c39585aacfbd6`（**等于基线**） |
| main 文件 SHA256 | `308dd6313c63db6938b5a0243d8369e43e03684fd8f55abbcfded4f8f0ce30bb`（50519 行 / 1673360 bytes） |
| 未发布 yaml（U15，只读对照） | SHA256 `93434ad64c0c6cb18d98f9bc76399e80130d57ab66d477e13faffce53624cc9d`；与本段 JSON 在副本树内深等价（CMD-COPY PASS） |

76 文件清单本席核对本条 1/1 哈希。席甲声称 76/76 为背景；本席只对 U14 这一文件独立实算。

### 1.2 相对发布基线 diff stat（本席实跑）

```
git diff --numstat 242f487233d028173cb9198eecd079f5d877405c e28dad059b2c7e08bcc95a8db18552d3f76edf4b -- docs/openapi/openapi-p0.json
764	33	docs/openapi/openapi-p0.json
git diff --shortstat 242f4872 e28dad05
76 files changed, 4745 insertions(+), 1084 deletions(-)
git diff --stat HEAD e28dad05 -- docs/openapi/openapi-p0.json
docs/openapi/openapi-p0.json | 797 +++++++++++++++++++++++++++++++++++++++++--
1 file changed, 764 insertions(+), 33 deletions(-)
git log --oneline 242f4872..445a49f4 -- docs/openapi/openapi-p0.json
(空)
```

即：本文件相对基线 = 相对当前 main；main 自基线**未改**该路径。全 76 文件合计 +4745/−1084 与开工提示一致。

## 2. OVERSIZE 全文件覆盖表

本席 JSON.parse 整文件一次成功（51250 行 / 1696081 bytes）。不得把「只看 diff」称为全量。覆盖方法：

| 结构段 | 行号 | 覆盖动作 |
|---|---|---|
| `openapi` / `info` / `servers` / `security` / `tags` | 1–81 | 全文阅读。`info.version` 基线/main `0.1.0` → 未发布 `V0.14`；title/description/servers/security/tags 与 main 深等（tags 20 个） |
| `paths` 604 条 | 82–37048 | 全量解析 730 个 HTTP operation；与 main 720 个逐 operation 深比较：712 深等、8 改写、10 新增、0 删除。8+10 个 operation 按行通读（见 §2.1） |
| 既有 paths 前缀（至 staff/registrations 前） | 82–12053 | 与 main 深等；抽读首条 `GET /api/open/d/{endpoint_key}/events/{event_id}` L83– 确认 `operationId`/`security` 结构 |
| 改写：registrations / review / device-credentials | 12054–12270+ | 全文 |
| 中间未改 paths | 12300–29599 | 与 main 深等（JSON 对象相等，非抽样宣称） |
| 改写：shifts / batch / mine / punch / month-view | 29602–30700 一带 | 全文（punch L29894–29979、shifts L29602–） |
| 中间未改 paths | 30701–36324 | 与 main 深等 |
| 新增 8 路径 / 10 operation | 36325–37048 | 全文 |
| `components` | 37049–51214 | 键集合与 main 相同（securitySchemes 4 / parameters 25 / requestBodies 216 / responses 4 / schemas 229）；内容深比较：仅 `requestBodies.StaffRegistrationRequest` 与 `schemas.StaffRegistrationRequest` 改变。securitySchemes 全文（L37050–37071） |
| `x-contract-rules` / `x-contract-hardening` / `x-p0-example-coverage` | 51215–51250 | 全文，与 main 深等 |

`$ref` 全树由 `verify-openapi-contract.mjs` 解析（副本 json+yaml PASS，无空键、无悬挂 ref、operationId 唯一）。

### 2.1 相对基线/main 的 operation 差集

新增 10（8 路径；shift-visible-days 与 payroll-penalty-cap 各 GET+POST）：

| method | path | operationId | 行 |
|---|---|---|---|
| GET | `/api/v1/staff/registration-companies` | listStaffRegistrationCompanies | 36325 |
| POST | `/api/v1/staff/self-registrations` | submitStaffSelfRegistration | 36369 |
| POST | `/api/v1/staff/profiles/{id}/employment-status` | setStaffEmploymentStatus | 36461 |
| GET | `/api/v1/staff/settings/shift-visible-days` | getStaffShiftVisibility | 36549 |
| POST | `/api/v1/staff/settings/shift-visible-days` | setStaffShiftVisibility | 36592 |
| GET | `/api/v1/staff/settings/payroll-penalty-cap` | getStaffPenaltyCap | 36671 |
| POST | `/api/v1/staff/settings/payroll-penalty-cap` | requestStaffPenaltyCap | 36714 |
| GET | `/api/v1/staff/shifts/{id}/history` | listStaffShiftHistory | 36828 |
| POST | `/api/v1/platform/merchants/{merchant_id}/self-operated` | setMerchantSelfOperated | 36884 |
| POST | `/api/v1/staff/locker-events` | recordStaffLockerScan | 36971 |

改写 8（路径已存在于 main）：

| method | path | operationId | 行 |
|---|---|---|---|
| POST | `/api/v1/staff/registrations` | submitStaffRegistration | 12054 |
| POST | `/api/v1/staff/registrations/{id}/review` | reviewStaffRegistration | 12099 |
| POST | `/api/v1/staff/device-credentials` | bindStaffDeviceCredential | 12240 |
| POST | `/api/v1/staff/shifts` | upsertStaffShift | 29602 |
| POST | `/api/v1/staff/shifts/batch` | batchUpsertStaffShifts | 29748 |
| GET | `/api/v1/staff/shifts/mine` | listMyStaffShifts | 29863 |
| POST | `/api/v1/staff/attendance/punch` | punchStaffAttendance | 29894 |
| GET | `/api/v1/staff/attendance/month-view` | myStaffAttendanceMonthView | 30680 |

schema/requestBody 改写：`StaffRegistrationRequest`（schema L46389；requestBody example L38570–38587）。

public 口：main 5 个（login / customer phone-login / customer logout / ticket-products / health）→ 未发布 **7** 个（加 registration-companies、self-registrations）。与 U13-001 钉的 730/public 7 一致。

### 2.2 跨段数据流（只读，不审他段为 PASS）

```
openapi-p0.json (U14)
  ├─ POST /staff/self-registrations, GET /staff/registration-companies
  │     → 副本 StaffController.java:46-56 + StaffRegistrationService
  │     → 副本 SecurityConfig.java:118-122 permitAll
  │     → 副本 dto/StaffSelfRegistrationRequest.java（六字段，无设备公钥/指纹）
  │     → main SecurityConfig 无这两条（公开面是本材料引入）
  │     → HTML PGM L578-579 / BSM L935 / BSM L1433
  ├─ POST /platform/merchants/{id}/self-operated
  │     → 副本 PlatformController.java:86-92 + MerchantSelfOperatedRequest
  │     → U04-001 已答删除；main 无此端点
  ├─ POST /staff/locker-events
  │     → 副本 StaffLockerService.java:40-54（requirePermission staff.profile.self + kiosk verified）
  │     → U10 V409 staff_locker_records；HTML PGM L582 / BSM L938
  ├─ POST employment-status / POST shift-visible-days
  │     → 副本 StaffController.java:95-98 / 203-206 无 requireIdempotencyKey（U05-011）
  ├─ GET shift-visible-days 文案「上下限为空」
  │     → 副本 JdbcStaffRepository 缺行 (7,null,null)；U10 V402 未种子该键
  ├─ punch method=charging_disconnect、终端仅 staff_work_phone
  │     → 副本 StaffService.punch + U10 V405 CHECK；HTML BEQR L280
  └─ 同树 yaml (U15) 与 json 深等价；main yaml 仍基线
        → 只合 U14 不合 U15 则 verify:openapi 红
        → 合 JSON 不合 U17 矩阵则 docs:operation-matrix --check 红
        → 安全基线三件钉本 SHA（U13-001）
```

SQL/迁移：本段无 SQL。U10 V400–V409 只读对照（V408 `is_self_operated` 已答删除；V405 打卡 method；V409 柜机表）。main 最高迁移 V410。本席未对 `complex_test*` 做写操作。

## 3. 第一性原理

1. **为什么必须存在。** P0 编码前置契约：730 个（main 720）HTTP 操作的 path/method/operationId、权限、鉴权上下文、终端、幂等、审计事件名、字段级 requestBody。删除后 `verify:openapi`、springdoc 比对、客户端生成没有正式源。HTML 做不到「十一端按契约访问」的机审层。必要性成立。
2. **声称的行为是否执行。** 本段是文档/契约，不是 Java。可执行声称 = 仓库门禁把 json 与 yaml 深比较、校验 x-* 扩展、解析 $ref。副本树 json+yaml：**执行且绿**。与 HTML、与 main 已定稿删除项、与未发布 Java 的一致性：**不绿**（见 findings）。不能把 CMD-COPY PASS 写成业务通过。
3. **未经验证前提。** （a）发布者会同时改 U15 YAML / U16 附录 / U17 矩阵 / U13 安全基线；（b）操作员删除独立自营后本契约会摘条；（c）公开注册会补设备证明。这三件在本材料里都还没发生。首轮按现文件发布就会撞门禁或把已删 API 写进 main。

## 4. 顺从式（GEN-001–018 逐项）

本段映射只有 GEN。每项都核过；「通过」仅指该判据在本文件上的适用结论，不升格为段 PASS。

| ID | 适用 | 结论 | 证据 |
|---|---|---|---|
| GEN-001 | 是 | **FAIL**（002） | 独立自营写口无 HTML 上位（六类合作方式已含「自营」）；`device_fingerprint` 声明「不采用」仍留字段（观察，不单独立项） |
| GEN-002 | 是 | **FAIL**（001/004/007） | 只叠 JSON 门禁红；GET 可见天数把空上下限写成合同；example `merchant_id: null` 对 required |
| GEN-003 | 是 | 无测试面 | 本段无测试文件。`verify-openapi-p0-examples` 只检查 `x-p0-example-coverage` 既有 8 个 request bodies（CMD-EX PASS），**不覆盖** 10 个新 operation。不把该绿写成新接口已测 |
| GEN-004 | 是 | **FAIL**（003/005） | 公开自助注册无设备公钥/指纹；柜机口同时要求 staff-token 与 kiosk 验签 |
| GEN-005 | 是 | **FAIL**（003） | `x-auth-context: public` + `security: []` 把员工注册与公司列表放在默认拒绝之外 |
| GEN-006 | 无实现面 | 豁免（原因：本段无事务/约束 DDL；柜机「取空后可供他人」是描述，完整性在 U05/U10） | |
| GEN-007 | 是 | **FAIL**（006） | 两写口 `x-idempotency-required: false`，与同控制器 23 处写口及 U05-011 同根 |
| GEN-008 | 是 | **FAIL**（001–007） | json/yaml/矩阵/DTO/Java 注释/HTML 五态与公开面多处不一致 |
| GEN-009 | 是 | **FAIL**（008） | 8 个写口 `x-audit-event-failure: "none"`（main 720 口该字段从不为 none；打卡从 `punch_failed` 改为 none） |
| GEN-010 | 是 | **FAIL**（003） | 公开 POST 收 `phone`/`id_doc` 原值，无设备绑定、无认证 |
| GEN-011 | 是 | **FAIL**（005/008） | 柜机 success 写成 `staff.locker.store / staff.locker.retrieve`（斜杠+空格，不是单一事件名）；failure 字面量 none |
| GEN-012 | 无实现面 | 豁免（原因：契约无循环/连接/批大小实现） | |
| GEN-013 | 是 | **FAIL**（001） | 只合本段，`verify:openapi` / `docs:operation-matrix --check` 依赖未声明的 U15/U17 同批 |
| GEN-014 | 无实现面 | 豁免（原因：本段无迁移；V 号冲突在 U10-001） | |
| GEN-015 | 部分 | 观察 | 本段新增/改写 summary、description 为中文。712 个未改 operation 的英文 summary 与 main 字节级相同，归 M391 既有，不构成本段新根因。铁律 7 四类例外覆盖 operationId/字段名 |
| GEN-016 | 是 | **FAIL**（001/004） | GET 可见天数把「未配置上下限为空」写成当前合同；example 与 schema 互斥；info.version `V0.14` 只在未发布树 |
| GEN-017 | 是 | **FAIL**（002/003/004） | 002=添加（已答删除）；003/004=冲突；005–008=技术性且逐条写了无业务发明理由。未把 HTML 沉默判冲突 |
| GEN-018 | 是 | 本席遵守 | 每条 finding 落到 HTML 原文或可复现命令；不把席位意见当依据 |

## 5. 对抗式

固定问题「发布会覆盖/冲突 main 已修项」：

- 本文件 main blob == 基线 blob，M009–M012 **没有**改 `docs/openapi/openapi-p0.json`。整文件覆盖**不会**回退那些模块在 Java/SQL 上的已修项。
- 会把未发布 +764/−33 **安装**到当前 main：10 个新 operation（含已答删除的自营写口、公开无设备注册）、8 个 staff 口改写（phone_hash→phone 明文、打卡 method、排班待审）。
- 只覆盖 JSON 保留 main YAML：39 处深不等，门禁红（CMD-JSONONLY）。
- 覆盖 JSON 保留 main 矩阵：`--check` 红（CMD-MATRIX）。
- 与 U13 已定稿安全基线：未发布钉 730/public 7 / SHA `f8de4f8c…`；main 720/5。只合本段或只合 U13 都会让 `--strict` 红。

其它反例：

- 匿名客户端按契约 POST `/staff/self-registrations`，body 仅 `idempotency_key`+`company_id`+`name`（phone 非必填），`X-Terminal-Type: app`：契约 200 成功路径，无设备字段可填。HTML 要求安装时绑定设备标识。
- `cooperationForm=lease` 的商户仍可通过 `setMerchantSelfOperated{self_operated:true}` 把员工赶到本人注册路径（U04-001 已证的双开关）；本契约是该开关的 HTTP 面。
- 打卡失败：契约 `x-audit-event-failure: none`，审计实现若按契约将不记失败事件。
- example `merchant_id: null` 对 required `merchant_id`：生成器/测试夹具会发出 schema 拒绝的 payload。

理论极端（收窄不修）：OpenAPI 被换成另一份运行时 springdoc 输出且无人跑 `verify:openapi`——要两件不巧，不修。JWT Bearer 顶层 security 被 public 口 `security: []` 覆盖是**显式设计**，不是极端。

## 6. 与 main 分叉对照（只读）

| 项 | 未发布副本 | 当前 main 445a49f4 |
|---|---|---|
| blob | `0cd43e73…` | `bfa444d6…` = 基线 |
| ops / paths | 730 / 604 | 720 / 596 |
| public | 7 | 5 |
| info.version | V0.14 | 0.1.0 |
| StaffRegistrationRequest | `phone`/`id_doc` 原值；required 含 `merchant_id` | `phone_hash`/`id_doc_hash`；required 无 merchant_id |
| example 注册 | `phone: "13800138000"`, `merchant_id: null` | `phone_hash: "sha256:phone_hash_placeholder"` |
| punch terminals | 仅 `staff_work_phone` | `staff_private_phone`+`web` |
| punch method | nfc/qr/charging_disconnect | nfc/qr/manual |
| punch failure 事件 | `none` | `staff.attendance.punch_failed` |
| 自营写口 | 有 | 无 |
| 自助注册/公司列表 | 有，public | 无 |
| yaml | 与 json 深等（U15） | 与 json 深等（基线） |
| `x-audit-event-failure==none` | 12 个 operation（8 个写口） | **0** |

main 该文件无 M009–M012 修补。发布本段等于在当前 main 上应用 +764/−33，不是三方合并冲突，是**单向安装未发布契约**。

## 7. Findings

### GR-U14-001

- **临时 ID**：GR-U14-001
- **完整原文**：只把本段 `openapi-p0.json` 叠到当前 main（YAML、operation 矩阵、闭合附录、安全基线仍是 HEAD）时，`tools/verify-openapi-contract.mjs` 因 json↔yaml 深比较失败（声称 39 处，日志列出前 10 处含 registrations summary/description/终端数组、shifts 审计事件、`shift_id` 缺 yaml）；`tools/generate-openapi-operation-matrix.mjs --check` 对 main 矩阵文件报 out of sync。未发布 json+yaml 同树则 verify-openapi PASS。本文件 main 自基线未改，整文件覆盖不会回退 M009–M012 对该路径的修复（无），但会把 10 个新 operation 与 8 个改写安装进 HEAD。
- **文件:行号**：`docs/openapi/openapi-p0.json:1-51250`（整文件为发布物）；差集见 §2.1
- **业务影响**：发布门禁红；或强合后客户端/矩阵/安全基线与契约三套数。U13-001 已钉本 SHA 与 730/7，须同批重生。
- **可复现命令/最小反例**：CMD-JSONONLY EXIT 1；CMD-MATRIX EXIT 1；CMD-COPY EXIT 0。见 §9。
- **冻结判据 ID**：GEN-002、GEN-008、GEN-013、GEN-016
- **2.3 依据类型**：可复现命令输出
- **四分类**：技术性
- **无业务发明理由**：json/yaml/矩阵同步是既有仓库门禁，不新增业务规则。
- **工程缺陷及是否建议登记**：成立；建议 B2 队列「是」
- **最小处置（写给发布者）**：以 main `445a49f4` 为底，本段与 U15 YAML、U16 附录、U17 矩阵同一 PR 重放；先按 002–008 改契约再生成 yaml/矩阵/附录；U13 安全基线三件在 OpenAPI 定稿后 `docs:security-baseline` 重生。禁止整树覆盖。迁移重编号归 U10（≥V411），本段无 SQL。

### GR-U14-002

- **临时 ID**：GR-U14-002
- **完整原文**：新增 `POST /api/v1/platform/merchants/{merchant_id}/self-operated`（operationId `setMerchantSelfOperated`），requestBody 必填布尔 `self_operated` + 缘由 + 幂等键；description「自营商户员工采用公司员工本人注册路径」；tags 为 `staff`（路径却在 `/api/v1/platform/merchants`）。这是独立于六类合作方式的第二轴写口。操作员已对 U04-001 答删除，并写明 OpenAPI POST 随之去掉。
- **文件:行号**：`docs/openapi/openapi-p0.json:36884-36970`（schema 布尔 L36939-36940；required L36948-36951）
- **业务影响**：契约一旦发布，客户端会继续打已删后端；租赁商户可被标成「自营」而走公司员工注册（与合同/结算用的 `cooperation_form` 分叉）。
- **可复现命令/最小反例**：`python3 -c` 解析未发布 JSON，path 存在且 main 同 path 不存在（CMD-PARSE）。`questions/U04-001.md` 操作员答复「删除」。
- **冻结判据 ID**：GEN-001、GEN-008、GEN-017
- **2.3 依据类型**：HTML 原文（BSM L701「区分自营、租赁、联营、代运营、服务商接入、其他这六类合作方式」；BSM L1433 / PGM L578「在商户档案上标注为自营的商户视同公司自有」——落在六类码，不是新布尔列/新 POST）
- **四分类**：添加（操作员已答删除；引用 `questions/U04-001.md`，不新开 2.2）
- **工程缺陷及是否建议登记**：成立；建议 B2 队列「是」
- **最小处置**：发布前删除该 path/operation 及一切 `self_operated` 布尔字段；员工分流改读合作方式码 `self_operated`（U05-001）。随 001 同 PR。

### GR-U14-003

- **临时 ID**：GR-U14-003
- **完整原文**：`GET /api/v1/staff/registration-companies` 与 `POST /api/v1/staff/self-registrations` 均 `x-permission: public`、`x-auth-context: public`、`security: []`、`x-terminal-allowed` 含 `app` 与 `staff_private_phone`。自助注册 requestBody 字段仅 idempotency_key/company_id/merchant_id/name/phone/id_doc，**无设备公钥/指纹**；required 甚至不含 phone。这把「安装时绑定该手机专属设备标识」从契约里拿掉，并允许客人端码 `app` 打员工注册。副本 `SecurityConfig.java:121-122` permitAll 与 `StaffSelfRegistrationRequest` 六字段与此同构（U05-003/U07-004/U01-001 已定稿）。
- **文件:行号**：`docs/openapi/openapi-p0.json:36325-36459`（public/security L36333-36336、L36366；终端 L36336-36338、L36380-36382；schema L36417-36453）
- **业务影响**：任意客户端可按契约枚举有效公司并提交姓名/手机/证件待审申请，无设备绑定、无认证。PII 在公开写口上以原值传输。
- **可复现命令/最小反例**：契约本身即最小反例（无设备字段可填）。HTML PGM L579「私人手机 App 安装时生成并绑定一个该手机专属的设备标识」；BSM L935 同旨；BEQR L233 终端十一端将「客人手机应用」与「员工私人手机」分开。
- **冻结判据 ID**：GEN-004、GEN-005、GEN-008、GEN-010
- **2.3 依据类型**：HTML 原文
- **四分类**：冲突（同根 U05-003 / U07-004 / U01-001 / U13-004）
- **工程缺陷及是否建议登记**：成立；建议 B2 队列「是」
- **最小处置**：自助注册改为私人手机通道；请求增加设备公钥/指纹，无指纹 4xx；`x-terminal-allowed` 去掉 `app`；permitAll 仅在该契约落地后保留并加频控（U07-004）。公司列表若仍需匿名发现，须另证来源校验，不能作为无绑定注册的前置。不新开 2.2。

### GR-U14-004

- **临时 ID**：GR-U14-004
- **完整原文**：`GET /api/v1/staff/settings/shift-visible-days` description「按当前经营主体读取；出厂包含今天在内七天，**未配置的上下限为空**。」把 U05/U10 已定稿的缺行 `(7,null,null)` 写成正式合同。HTML 要求出厂默认 7 天且须同时配置上下限。同文件 POST 已要求 min/max/reason（L36598、L36659-36663），GET 与 POST 口径自相矛盾。
- **文件:行号**：`docs/openapi/openapi-p0.json:36554-36556`
- **业务影响**：客户端会把空上下限当合法出厂完成；与 BSM「未填写上下限的参数不得上线」冲突。
- **可复现命令/最小反例**：description 原文。HTML BSM L1444「出厂默认 7 天，须同时配置上下限」。
- **冻结判据 ID**：GEN-002、GEN-008、GEN-016
- **2.3 依据类型**：HTML 原文
- **四分类**：冲突（同根 U05-005 / U10-003）
- **工程缺陷及是否建议登记**：成立；建议 B2 队列「是」
- **最小处置**：随 U05-005/U10-003 改 description：读到出厂行（值 7 + 发布者写入的上下限）或缺行 fail-closed。禁止把空上下限写成合同。席位不代填上下限数字。

### GR-U14-005

- **临时 ID**：GR-U14-005
- **完整原文**：`POST /api/v1/staff/locker-events` description「员工取认证身份，柜机取设备身份」；`x-auth-context: staff-token` 且 `x-terminal-allowed: ["kiosk"]`、`x-permission: staff.profile.self`。这是双身份调用形态。`x-audit-event-success` 值为 `staff.locker.store / staff.locker.retrieve`（含空格与斜杠），不是单一事件名；副本 `StaffLockerService.java:81` 实际记 `staff.locker.{action}`。HTML 只要求柜子自动记录是谁/哪一格/时间，不要求调用方同时持有员工会话和柜机验签。
- **文件:行号**：`docs/openapi/openapi-p0.json:36971-36987`（description L36978；auth L36981；terminal L36982-36984；success L36986）
- **业务影响**：无同时持有两套身份的单端调用方（U11-006 已证测试靠注入）；契约把该形态冻结。审计采集若按字面量匹配会找不到事件。
- **可复现命令/最小反例**：字段原文。HTML PGM L582 / BSM L938「由柜子自动记录，不设手工登记」。
- **冻结判据 ID**：GEN-002、GEN-004、GEN-008、GEN-011
- **2.3 依据类型**：HTML 原文
- **四分类**：技术性（同根 U05-006 / U13-006）
- **无业务发明理由**：鉴权上下文与审计事件名是实现可观测性/调用形态，不发明「要不要寄存柜」这条业务。
- **工程缺陷及是否建议登记**：成立；建议 B2 队列「是」
- **最小处置**：随 U05-006 改为单端：柜机以已登记 kiosk 签名上报，员工身份由扫码载荷承载；`x-audit-event-success` 改为单一名或按 action 分两个 operation。删除并列双身份表述。

### GR-U14-006

- **临时 ID**：GR-U14-006
- **完整原文**：`POST /api/v1/staff/profiles/{id}/employment-status` 与 `POST /api/v1/staff/settings/shift-visible-days` 均 `x-idempotency-required: false`（L36477、L36607）。同文件其它员工写口为 true。副本 `StaffController.java:95-98`、`:203-206` 无 `requireIdempotencyKey`，U05-011 已定稿须补。契约把缺幂等写成正式。
- **文件:行号**：`docs/openapi/openapi-p0.json:36477`、`36607`
- **业务影响**：任职状态与可见天数写口重试/双击可重复生效；与同控制器 23 处写口不一致。
- **可复现命令/最小反例**：字段原文。对照 `POST /staff/registrations` L12070 `x-idempotency-required: true`。
- **冻结判据 ID**：GEN-007、GEN-008
- **2.3 依据类型**：可执行产物（契约字段 + 副本控制器缺校验，U05-011）
- **四分类**：技术性（同根 U05-011）
- **无业务发明理由**：幂等键是重试协议，不发明任职状态或可见天数业务。
- **工程缺陷及是否建议登记**：成立；建议 B2 队列「是」
- **最小处置**：两口改为 `x-idempotency-required: true`，与 U05-011 同 PR 在控制器校验并按操作命名空间去重。

### GR-U14-007

- **临时 ID**：GR-U14-007
- **完整原文**：`components.schemas.StaffRegistrationRequest` required 含 `merchant_id`（L46393-46398），property 描述「代员工提交的商户编号，必填」。同名 requestBody example 仍给 `"merchant_id": null`（L38580），并改用 `"phone": "13800138000"`。副本 `StaffRegistrationRequest.java:7` 注释仍写「merchant_id 为空表示公司自有员工」。schema、example、Java 注释三套互斥。
- **文件:行号**：schema L46389-46438；example L38570-38587；对照副本 `backend/src/main/java/com/complex/platform/access/staff/dto/StaffRegistrationRequest.java:7`
- **业务影响**：按 example 编码的客户端会被 schema 拒绝；按 Java 注释传空 merchant_id 会被 OpenAPI required 判非法。商户代提交路径本身（必填 merchant_id）是 HTML 细化，缺陷是契约自洽。
- **可复现命令/最小反例**：CMD-PARSE 打印 schema.required 与 example.merchant_id。
- **冻结判据 ID**：GEN-002、GEN-008
- **2.3 依据类型**：可执行产物
- **四分类**：技术性
- **无业务发明理由**：example 与 required 对齐是契约正确性，不发明商户代提交。
- **工程缺陷及是否建议登记**：成立；建议 B2 队列「是」
- **最小处置**：example 改为非空 merchant_id（测试占位符即可）；Java 注释随「公司员工走 self-registrations」删掉「空=公司自有」。phone 示例勿用可被误认为真实号的值（可用明显占位）。

### GR-U14-008

- **临时 ID**：GR-U14-008
- **完整原文**：本材料把字面量 `"none"` 写入 `x-audit-event-failure`（main 720 个 operation 该字段从不为 none，success 也不为 none）。写口 8 个：`upsertStaffShift`/`batchUpsertStaffShifts`（从 `staff.shift.save_failed` 改来）、`punchStaffAttendance`（从 `staff.attendance.punch_failed` 改来）、`submitStaffSelfRegistration`、`setStaffEmploymentStatus`、`setStaffShiftVisibility`、`requestStaffPenaltyCap`、`recordStaffLockerScan`。`verify-openapi-contract.mjs` 只检查字段非空，把 `"none"` 当合法事件名，门禁不红。
- **文件:行号**：L29618、L29760 一带 batch、L29909 punch、L36386 self-reg、L36479 employment-status、L36609 visibility POST、L36731 penalty-cap POST、L36987 locker
- **业务影响**：按契约实现的失败审计将寻找名为 none 的事件或不记失败；打卡失败留痕被显式拿掉。
- **可复现命令/最小反例**：CMD-PARSE 统计 unpublished 12 / main 0。main punch 对照 `git show HEAD:docs/openapi/openapi-p0.json` 同 operation 的 failure 为 `staff.attendance.punch_failed`。
- **冻结判据 ID**：GEN-009、GEN-011
- **2.3 依据类型**：可复现命令输出
- **四分类**：技术性
- **无业务发明理由**：审计事件名是可观测性约定，不发明打卡/排班业务。
- **工程缺陷及是否建议登记**：成立；建议 B2 队列「是」
- **最小处置**：写口恢复或新给 `*_failed` 事件名（打卡至少恢复 `staff.attendance.punch_failed`）；GET 若确无审计可在门禁允许的前提下留空策略由发布者统一，不得用字面量 none 混充。柜机失败名随 005 一并改。

## 8. 观察（无新根因，完整登记，建议队列「否」）

1. 712 个未改 operation 与 main 深等；其中英文 summary 是 M391 既有，本段未加重。
2. `AccessUserRequest.properties.phone` L44281 仍写「仅保存不可逆 phone_hash」——main 既有，本材料只把员工注册从 hash 改为原值加密，未改账号建号 schema。归 M391/账号模块，不新开。
3. `GET payroll-penalty-cap` L36678「提成不入分母，事假另算」与 HTML 总薪资定义（不含提成）及「请假扣薪不计入合计上限」同向；**未**把底薪+奖金称作总薪资。分母缺补贴/绩效是 U05-004 实现面，本段描述未重复该错误句。不升冲突。
4. `employment_status` 五码 `pending_entry/employed/trial/suspended/departed` 与 BEQR L240 五态一一对应；英文码属系统内部标识符（铁律 7）。不升冲突。中文展示归 UI（U03）。
5. punch 去掉 `manual`、终端收窄为工作手机、加入 `charging_disconnect`，与 BEQR L280 同向。这是本材料改对的部分。
6. `device_fingerprint` 保留但声明不采用（L29955-29958）：死字段，GEN-001 气味；随打卡改写可删。不单独立项。
7. `x-p0-example-coverage` 与 main 相同，策略写明「full operation examples are P1」；新高风险写口无 example，examples 门禁仍绿。登记。
8. 公开公司列表在 U05-012 已作观察；本契约把它锁成 public。并入 003 处置，不第二开添加。
9. `setMerchantSelfOperated` tags=`staff` 随 002 删除消失。
10. 本段无 SQL、无测试文件、无 UI。未跑 Spring / 未写库。CMD-COPY 绿只证明未发布 json↔yaml。
11. 提交 `03acdb98` 本仓无 commit 对象；审查输入是树 `e28dad05` 的只读副本。
12. `info.description` 在 JSON 里仍说「本文采用 JSON-compatible YAML」——main 既有套话。
13. public 口 responses 模板含 401/403/404/409，与 `security: []` 并列，属样板复制，不单独立项。
14. 排班改写把成功审计改到 `governance.supervisor_review.evaluated`：与 U09 主管审批链同向，属细化；缺陷只在 failure=none（008）。
15. 自助注册 required 不含 phone/id_doc：HTML「提交资料」未强制字段清单。不把沉默判冲突。若 003 补设备字段，资料项随实现。

## 9. 命令证据

工作目录除非注明，均为只读。隔离验证在 `/tmp/u14-run-*`（软链仓库 `node_modules`，不装依赖）。

**CMD-LOCK** `sha256sum docs/audit-2026-09/plan/criteria-lock.md`  
OUT：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` EXIT 0

**CMD-HEAD** `git rev-parse HEAD origin/main`  
OUT：二者 `445a49f409f0cfbe6dd616ae079be1299824c48f` EXIT 0

**CMD-OBJ** `git cat-file -t` e28dad05 / 03acdb98 / 242f4872  
OUT：`tree` / fatal could not get object info / `commit`

**CMD-BLOB** `git rev-parse 242f4872:docs/openapi/openapi-p0.json` 与 `HEAD:…` 与 `e28dad05:…`  
OUT：`bfa444d6…` / `bfa444d6…` / `0cd43e73…`

**CMD-HASH** 副本 `sha256sum` + `wc -l -c`  
OUT：`f8de4f8c…`  51250 1696081；与清单一致

**CMD-DIFF** `git diff --numstat 242f4872 e28dad05 -- docs/openapi/openapi-p0.json`  
OUT：`764	33	docs/openapi/openapi-p0.json`  
全 76：`76 files changed, 4745 insertions(+), 1084 deletions(-)`  
`git log --oneline 242f4872..HEAD -- docs/openapi/openapi-p0.json`：空

**CMD-PARSE** Python 深比较未发布 vs main JSON  
OUT：ops 730 vs 720；paths 604 vs 596；added 10 removed 0 changed-shared 8；public 7 vs 5；schema 仅 StaffRegistrationRequest 变；requestBodies 仅同名变；failure=none unpublished 12（写口 8）main 0；phone_hash 次数 1 vs 5

**CMD-COPY** `/tmp/u14-run-copy`（未发布 json+yaml + 软链 node_modules）`node tools/verify-openapi-contract.mjs`  
OUT：`OpenAPI contract verification passed.` EXIT 0

**CMD-JSONONLY** `/tmp/u14-run-jsononly`（未发布 json + **main yaml**）同脚本  
OUT：EXIT 1；`openapi-p0.json vs openapi-p0.yaml: 39 deep-equality diff(s)`；前 10 条见 §7.001

**CMD-MAIN** 仓库 cwd `node tools/verify-openapi-contract.mjs`  
OUT：`OpenAPI contract verification passed.` EXIT 0

**CMD-EX** `/tmp/u14-run-copy` `node tools/verify-openapi-p0-examples.mjs`  
OUT：`OpenAPI P0 example verification passed: 8 request bodies and 3 responses checked.` EXIT 0

**CMD-MATRIX** `/tmp/u14-run-matrix`（未发布 json+yaml + **main** `p0-openapi-operation-acceptance-matrix-v0.1.md`）`node tools/generate-openapi-operation-matrix.mjs --check`  
OUT：EXIT 1；`docs/p0-openapi-operation-acceptance-matrix-v0.1.md is out of sync with docs/openapi/openapi-p0.json`

**CMD-LEDGER** `sha256sum docs/audit-2026-09/ledger.md findings.md`  
OUT：`26bf86fc…` / `d7772ad9…` 与开工提示一致

首次用 NODE_PATH 而未软链 `node_modules` 时 ESM 报 `Cannot find package 'yaml'`（EXIT 1）——属隔离环境装配，不是契约 finding；改软链后 CMD-COPY/CMD-MAIN 绿。不把该失败折算业务缺陷。

未跑：Spring 单测、flyway、会对 `complex_test*` 写库的脚本、`verify:security-baseline --strict`（U13 已跑，本段不重复写库/不改基线文件）。

## 10. 添加项完整列表（供主调度按 2.2 处理）

| 临时 ID | 添加了什么 | HTML 能否指向上位 | 本席分类 | 2.2 |
|---|---|---|---|---|
| GR-U14-002 | `POST …/self-operated` 独立布尔写口 | 六类合作方式已含「自营」；不能指向「第二轴布尔」 | 添加 | **已答删除** `questions/U04-001.md`，不新开、不再问 |

无其它添加。公开自助注册能指向 PGM L578 两条路径，是冲突（缺设备证明）不是添加。任职五态、打卡三法、可见天数、处罚 20%、柜机扫码均能指向 HTML 上位，属细化或冲突/技术性，不是添加。

## 11. 结论

**FAIL**

发布前须修：GR-U14-001 … GR-U14-008（8）。添加项 1 条已答删除，不弹窗。观察 §8 共 15 条，建议队列「否」。无 HTML 修改申请。无未闭合待问。不存在有条件通过。

本段适用 18 条 GEN 均已逐项核对；1 文件 OVERSIZE 全结构覆盖；三线均执行。处置写给发布者：以 main `445a49f4` 为底，U14+U15+U16+U17 同 PR，先按 002–008 改契约再生成 yaml/矩阵/附录/安全基线；自营写口删除；公开注册补设备证明；禁止整树覆盖。

## U14 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 43111 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02 §7/§11；criteria-lock V0.11；modules.md V0.04 附录「未发布材料第 14 段：完整接口定义」（1 文件 51250 行 OVERSIZE-SINGLE-FILE，main 主归属 M391；适用判据＝映射行并集 18 项 GEN-001–GEN-018）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`。
- 审计对象仍是只读输入副本 `.scratch/module-recheck-20260908/unpublished-03acdb98`（树 `e28dad059b2c7e08bcc95a8db18552d3f76edf4b`，基线 `242f487233d028173cb9198eecd079f5d877405c`），不是 main。三份 HTML 取 main 正本。未发布提交对象 `03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb` 本仓 `git cat-file -t` 仍不可解析。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B2 核实结论；原文与哈希本席本节点独立实算。
- 本席 B2-r1：**PASS**（001–009 逐 ID 全 PASS；添加 1 已答删除、不发新 2.2；发布前须修「是」=001–008，「否」=009）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。判据总数 401；M391 映射行 L6770 共 18 ID＝GEN-001–GEN-018，MISSING=[] EXTRA=[]。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要核对只读（输入哈希、本段 blob 与树 `e28dad05` 全等、main/基线 blob `bfa444d6…`、未发布 OpenAPI SHA `f8de4f8c…`、main OpenAPI SHA `308dd631…`、副本行 `:36325-36459`/`:36554-36556`/`:36598`/`:36659-36663`/`:36678`/`:36884-36970`/`:36971-36987`/`:36477`/`:36607`/`:38580`/`:46393-46398`/`:29955-29958`、HTML PGM L578/L579/L582、BSM L701/L935/L938/L1433/L1444、BSM L521、BEQR L233/L240/L280、`questions/U04-001.md` 答复首行「删除」、已定稿 U01-001/U01-007/U02-002/U05-003/U05-005/U05-006/U05-011/U06-005/U07-004/U10-003/U13-001/U13-004/U13-006、package.json 既有 `verify:openapi`/`docs:operation-matrix`/`verify:operation-matrix`/`docs:openapi-closure`/`docs:security-baseline`）；未重跑 overlay/生成器、未装依赖、未碰 `complex_test*`。追加稿仅经 `/tmp/u14-b2-append.md` 中转，落盘后删除。本票不把任何 U-FIX worktree 当作本段已发布修复。

此票只判断 findings.md V1.70 顶部「当前 U14 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、「发布前须修」是/否与最小处置。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。材料未发布，定稿后状态应记 REVIEWED-FAIL（有已定稿缺陷，处置写给发布者），不做 B3。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/U14-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 43111 | `5e716790f4b04710ccee15614e6f3520df2c4d6a0ec1d03ccd47bf7bdbebb73b` |
| `docs/audit-2026-09/modules/U14-b1-self-raw.md` | V0.01 | 17652 | `cb5482ce21b51247ee5fa014112b7b6b6a5c691ad0aec580aa58ecaabf88f69a` |
| `docs/audit-2026-09/findings.md` | V1.70 | 669270 | `847bd32f56c8f3aa82056dda8fd4a78eaa84a4abac3f96097a723c5b835e088e` |
| `docs/audit-2026-09/ledger.md`（只读） | V2.84 | 518879 | `dc3e8d59adf41cf33116b7efeb574f389abf83dab0fba28f07dd1bf56dd5a97b` |
| `docs/audit-2026-09/questions/U04-001.md`（只读，沿用） | V0.01 | 2282 | `5a9e9dab904fe9b82ccb2e414ae07515ea7192c7ed404685c31828a126c2b3d1`；答复首行精确「删除」 |

候选形态：V1.70 顶部 U14-001…009；questions 0（引用 U04-001）；添加 1 已答删除；无 HTML 修改申请。U14 ledger 行 IN_PROGRESS。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

HTML 正本哈希：BEQR `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`、BSM `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa`、PGM `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c`。副本本段 1/1 sha256/行/字节与树 `e28dad05` blob 全等（本轮复算：副本 SHA `f8de4f8c2957953911030512a3502b6a5d8bdc4ffd57dc6d3c4049a0df788384`，`git hash-object` = `0cd43e73050c947a13e72a0be1f0ca92ecba2978` = `e28dad05:docs/openapi/openapi-p0.json`）。基线/main blob 同为 `bfa444d6f1f1f6601a037b83a08c39585aacfbd6`。`git diff --numstat 242f4872 e28dad05 -- docs/openapi/openapi-p0.json` = `764	33`。全树 `git diff --shortstat 242f4872 e28dad05` = 76 files, +4745/−1084。

### 技能（V0.17 §6 收窄，本节点）

| 技能 | 本节点用法 |
|---|---|
| using-arc / using-superpowers | 先核路由；用户指定 B2 归并覆盖 Arc audit；未改走 audit 工作流 |
| receiving-code-review | 只取其「先核原文再接受或反驳、禁止表演性同意、B2 不修复」；本体若派同会话子代理则成第四席，**未调用** |
| grilling | 只借追问结构自查：(a) 四分类与 GR-001…008、(b) 001 叠法是否一条且只引既有门禁、(c) 003 处置与 U05-003/U07-004、(d) 009 是否写错 §8.3/6/15 |
| verification-before-completion | 声称前先跑 sha256/git rev-parse/git cat-file/hash-object/原文行号/HTML 锚点/package.json 脚本名；本票不声称本轮新跑 overlay 或契约门禁 |
| systematic-debugging / diagnosing-bugs | 只借读错→对照数据流（本段 JSON → U15 yaml/U16 附录/U17 矩阵/U13 基线 → main 720 → HTML）；本轮不建 `/tmp` overlay |
| ponytail-review | 只评最小删除/收窄，不落地。001 只重生既有生成物；002 删已答删除的写口；003–008 只改与 HTML/既有范式相反的契约字段 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |
| Arc audit | 会派子代理；与双席冲突；**未用** |

内部 grilling（自答，未问操作员）：

1. 003/004 改记冲突、002 添加已答删除、其余技术性，是否把业务发明伪装回去或改写 GR 四分类？否。见 (a)。
2. json↔yaml 39 处深比较红与矩阵 `--check` 红能否并成一条叠法项，处置是否只引用既有门禁与生成器？能。见 (b)。
3. 003 保留 public、补设备证明、去掉 app、加频控，是否与 U05-003/U07-004 定稿一致？「另证来源校验」是否发明？一致，只引用。见 (c)。
4. 009 把第 3/6/15 条写成观察，是否把应升队项降级或写错 §8？否。见 (d)。

### 完整差集（GR-001…008、§8 观察 1–15、席甲 CX-001…004、OBS-1…4）

漏并：无。两席 B1 全部临时 ID / 观察仍落在 001–009。未把本席 finding 降观察。席甲独有项无超出 CX-001…004 与 OBS-1…4。席甲 B1 漏判四处（叠法门禁红、可见天数空上下限合同、柜机双身份契约、注册 schema/example/Java 注释三套互斥）且把注册口只写成终端自报——候选已勘误接受本席八项须修，并按 GEN-017 把 003/004 记冲突、002 记添加已答删除，本席独立复核后维持。

| 临时 ID | 落入正式 ID | 是否改写本席原意 |
|---|---|---|
| GR-U14-001 | U14-001 | 否：技术性叠法；json↔yaml 深比较红 + 矩阵 `--check` 红；先改 002–008 再生成；安全基线随后重生；禁整树 |
| GR-U14-002 | U14-002 | 否：添加；已答删除；删 path/布尔；分流读六类码 |
| GR-U14-003 | U14-003 | 否：冲突；公开注册无设备证明 + 终端含 app；同根 U05-003/U07-004/U01-001 |
| GR-U14-004 | U14-004 | 否：冲突；GET 可见天数把空上下限写成合同；同根 U05-005 |
| GR-U14-005 | U14-005 | 否：技术性；柜机双身份 + 成功事件双值字面量；同根 U05-006 |
| GR-U14-006 | U14-006 | 否：技术性；两写口幂等 false；同根 U05-011 |
| GR-U14-007 | U14-007 | 否：技术性；schema required 含 merchant_id 而 example 给 null |
| GR-U14-008 | U14-008 | 否：技术性；8 个写口 failure 字面量 none |
| §8 1–15 | U14-009 | 「含」为闭集转述；第 3/6/15 条未把观察升冲突，也未把 finding 降观察（见 (d)） |
| 席甲 CX-U14-001 | U14-008 | 子集（8 写口 none）；并入 GR-008 不改技术性 |
| 席甲 CX-U14-002 | U14-003 | 子集（终端含 app）；并入 GR-003，分类维持冲突 |
| 席甲 CX-U14-003 | U14-002 | 同根添加已答删除 |
| 席甲 CX-U14-004 | U14-006 | 同根技术性幂等 |
| 席甲 OBS-1…3 | U14-009 | 维持观察（main 既有范式 / 版本格式 / 管理口私人手机） |
| 席甲 OBS-4 | U14-009 | 商户代提交路径与 HTML 两条路径同向，维持观察；其中「打卡 device_fingerprint 可不填与 U07 一致」不得覆盖本席 §8.6 死字段（见 (d) 与文末第 6 条） |

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/须修 | 本席票 |
|---|---|---|---|
| U14-001 | GR-U14-001（席甲漏） | 技术性（发布叠法，同根 U13-001/U10-001）/是/是 | **PASS（技术性叠法/是/是；只引用既有门禁与生成器）** |
| U14-002 | GR-U14-002 + 席甲 CX-U14-003 | 添加（操作员已答删除）/是/是 | **PASS（添加已答删除/是/是；不新开 2.2）** |
| U14-003 | GR-U14-003 + 席甲 CX-U14-002 | 冲突（同根 U05-003/U07-004/U01-001/U02-002/U13-004）/是/是 | **PASS（冲突/是/是；与 GR-003 四分类一致）** |
| U14-004 | GR-U14-004（席甲漏） | 冲突（同根 U05-005/U10-003/U06-005）/是/是 | **PASS（冲突/是/是；与 GR-004 四分类一致）** |
| U14-005 | GR-U14-005（席甲漏） | 技术性（同根 U05-006/U13-006）/是/是 | **PASS（技术性/是/是）** |
| U14-006 | GR-U14-006 + 席甲 CX-U14-004 | 技术性（同根 U05-011）/是/是 | **PASS（技术性/是/是）** |
| U14-007 | GR-U14-007（席甲漏） | 技术性/是/是 | **PASS（技术性/是/是）** |
| U14-008 | GR-U14-008 + 席甲 CX-U14-001 | 技术性/是/是 | **PASS（技术性/是/是）** |
| U14-009 | 双方观察归并 | 登记/否/否 | **PASS（登记/否/否；第 3/6/15 条未写错原意）** |

技术性入队 5（001 叠法、005 柜机契约、006 幂等、007 schema/example、008 失败事件）。添加入队 1（002，已答删除）。冲突入队 2（003 公开注册无设备证明、004 可见天数空上下限）。登记观察 1（009）。questions 0。无 HTML 修改申请。

发布前须修「是」=001–008（8）；「否」=009（1）。与候选汇总一致。席甲 B1 票 FAIL 但只立四项且漏 001/004/005/007，属低估；候选接受本席八项须修并勘误，本席独立复核后维持。

### 重点 (a) 003/004＝冲突，002＝添加已答删除，001/005/006/007/008＝技术性 — **PASS（与 GR-001…008 四分类一致；GEN-017 顺序与同根 U05/U07/U01 项一致）**

独立核，不采信席甲「文档随代码定稿改」或候选「按 GEN-017 改记冲突」六个字为己证。

GEN-017 原文：每项内容按 **冲突→细化→技术性→添加** 顺序分类；不得把业务发明伪装成技术实现。

本席 B1 四分类 vs 候选：

| GR | 本席四分类 | 候选 | 同根已定稿 |
|---|---|---|---|
| 001 | 技术性 | 技术性 | U13-001 / U10-001 技术性叠法 |
| 002 | 添加（已答删除） | 添加（已答删除） | U04-001 / U08-001 / U10-002 / U11-002 |
| 003 | 冲突 | 冲突 | U05-003 / U07-004 / U01-001 / U13-004 |
| 004 | 冲突 | 冲突 | U05-005 / U10-003 / U06-005 |
| 005 | 技术性 | 技术性 | U05-006 / U13-006 |
| 006 | 技术性 | 技术性 | U05-011 |
| 007 | 技术性 | 技术性 | 本段契约自洽；商户代提交本身是 HTML 细化 |
| 008 | 技术性 | 技术性 | 审计事件名；不发明打卡/排班业务 |

本轮原文（副本，本席抽出，不采信席甲引文为己证）：

- `:36333-36338`/`:36377-36382`/`security: []` 把公司列表与自助注册放在 public；requestBody 仅六字段、required 无 phone、**无设备公钥/指纹**。HTML PGM L579 / BSM L935「私人手机 App **安装时**生成并绑定该手机专属的设备标识」。公开无指纹与绑定顺序相反 → **冲突优先**。公开两路径能指向 PGM L578，不是添加。同根 U05-003。席甲 CX-002 只写终端含 `app`，覆盖不足但分类已是冲突。候选补设备字段 + 去掉 `app`，与 GR-003 一致。
- `:36554-36556`「出厂包含今天在内七天，**未配置的上下限为空**」。HTML BSM L1444「须同时配置上下限」；L521「未填写上下限的参数不得上线」。同文件 POST `:36598`/`:36659-36663` 已要求 min/max/reason。GET 把缺行 `(7,null,null)` 写成合同 → **冲突优先**。同根 U05-005。席甲漏判；候选立 004 成立。
- `:36884-36970` `setMerchantSelfOperated` 独立布尔写口。HTML BSM L701 六类已含「自营」；BSM L1433 / PGM L578 标注落在六类码。操作员 `questions/U04-001.md` 答复首行「删除」→ **添加**，引用已答，不新开 2.2。

001/005/006/007/008 保持技术性：json/yaml/矩阵同步是闸口一致性；柜机双身份不改「扫码留痕、不手工登记」的上位需求；幂等键是重试协议；example 与 required 对齐是契约正确性；failure 字面量 none 是可观测性约定。无业务发明。

候选 003 同根多写 U02-002/U13-004，004 同根多写 U06-005/U10-003：均是已定稿同缺陷的其它面，不改四分类，不发明新根。票 **PASS**。

### 重点 (b) U14-001 把 json/yaml 深比较红与矩阵 `--check` 红并为一条叠法项 — **PASS（成立；处置只引用既有门禁与生成器）**

独立核：

1. 本文件 main blob＝基线 blob `bfa444d6…`。只把未发布 JSON 叠到当前 main（YAML 仍 HEAD）时，B1 CMD-JSONONLY：`verify-openapi-contract.mjs` EXIT 1，声称 39 处深不等。未发布 json+yaml 同树 CMD-COPY EXIT 0。本轮不重跑，源码行与 B1 命令记录仍成立。
2. 只合 JSON 保留 main 矩阵：B1 CMD-MATRIX `generate-openapi-operation-matrix.mjs --check` EXIT 1（out of sync）。失败形态是生成物未跟契约，不是新业务。
3. U13-001 已钉未发布 OpenAPI SHA `f8de4f8c…`、730 ops / public 7；main 720/5。OpenAPI 不定稿就重生基线会钉错 SHA。GR-001 已写「U13 安全基线三件在 OpenAPI 定稿后 `docs:security-baseline` 重生」。并入 001 的**后续动作**，不另开 finding，也不把 U13-001 的重生从须修拿掉。

两条失败形态不同（yaml 深不等、矩阵不同步），**同一发布动作**：以 main `445a49f4` 为底，先按 002–008 改本段契约，再与 U15 yaml、U16 附录、U17 矩阵同 PR 对齐；基线三件随后重生。禁止整树覆盖。并成一条成立，不是把 003/004 的 HTML 冲突捆进「技术性」。

本轮核 `package.json` 已有，无新闸：

- `verify:openapi` = `node tools/verify-openapi-contract.mjs`
- `docs:operation-matrix` / `verify:operation-matrix`（`--check`）
- `docs:openapi-closure` / `verify:openapi-closure`
- `docs:security-baseline`

候选处置「先按 002–008 改契约再生成 yaml/矩阵/附录、U13 安全基线三件随后 `docs:security-baseline` 重生」只引用上述脚本与 U15/U16/U17 同树源。yaml 在仓库里是与 json 深比较的双写源，不是第三条业务规则；「生成 yaml」读作与 U15 同批改到深等。无新业务实体。票 **PASS**。

### 重点 (c) U14-003 处置保留 public、设备公钥/指纹必填、去掉 app、加频控 — **PASS（与 U05-003/U07-004 定稿一致；「另证来源校验」只引用不发明）**

独立核已定稿原文，不采信候选转述为己证。

- U05-003 定稿处置：自助注册请求增加私人手机设备公钥/指纹并在落档或批准时绑定，不要求先有 staffProfileId；**公开端口加频控/来源校验**。
- U07-004 定稿处置：随 U05-003（请求带设备公钥/指纹）；**公开端口加频控/来源校验**；**permitAll 仅在该契约落地后保留**。
- U01-001 定稿处置：自助注册归员工私人手机通道；终端身份由绑定/登记设备类型决定，**不自报 app**。

候选 003：请求增加设备公钥/指纹必填（无指纹 4xx）；`x-terminal-allowed` 去掉 `app` 只留私人手机码；public 保留但加频控（U07-004）。与上列三句合取一致：public/permitAll 不是「无设备证明仍放行」，而是设备证明落到请求体之后仍可匿名发现+提交、另加频控。不得读成只改通道、不补指纹（与 U13-004 定稿同一并行根）。

「公司列表匿名发现须另证来源校验」：GR-003 原文是「公司列表**若仍需**匿名发现，须另证来源校验，**不能作为无绑定注册的前置**」。U07-004/U05-003 已写「来源校验」。候选压缩掉「若仍需」与「不能作为前置」，但仍是引用已定稿的来源校验，不是新的发现业务、不新开 2.2。定稿宜把条件句补回，见文末第 2 条。分类/须修不变。票 **PASS**。

### 重点 (d) U14-009 观察是否写错本席 §8（第 3/6/15 条） — **PASS（未写错；无应升队被降观察项）**

独立核本席 §8 原文。

**第 3 条。** 本席原文：「`GET payroll-penalty-cap` L36678「提成不入分母，事假另算」与 HTML 总薪资定义（不含提成）及「请假扣薪不计入合计上限」同向；**未**把底薪+奖金称作总薪资。分母缺补贴/绩效是 U05-004 实现面，本段描述未重复该错误句。不升冲突。」本轮核 `:36678` 原文确为「普通处罚合计封顶，提成不入分母，事假另算」，无「固定工资与奖金」冒充总薪资。候选「3 上限描述未重复错误句」是该句后半的压缩。「未重复」只覆盖本段**未**把底薪+奖金写成总薪资，**不**覆盖 U05-004 实现面（已在 U05 须修）。未把上限描述升冲突，也未宣称 U05-004 已消解。正确。

**第 6 条。** 本席原文：「`device_fingerprint` 保留但声明不采用（L29955-29958）：死字段，GEN-001 气味；随打卡改写可删。不单独立项。」本轮核 `:29955-29958`「可不填；实际设备编号来自已验证设备身份，不采用此字段」。候选「6 `device_fingerprint` 死字段随打卡改写可删」未写成须修，也未升冲突。席甲 OBS-4「可不填与 U07 一致」列在席甲观察侧：U07 定稿是设备签名钥源，不是打卡 body 这一死字段。009 同时保留本席第 6 条，未用席甲句覆盖。正确。

**第 15 条。** 本席原文：「自助注册 required 不含 phone/id_doc：HTML「提交资料」未强制字段清单。不把沉默判冲突。若 003 补设备字段，资料项随实现。」候选「15 自助注册 required 不含 phone/id_doc 为 HTML 沉默」未升冲突。设备字段的须修在 003，不在 009。正确。

**应升队而被降观察？无。** 本席 GR-001…008 全部入 001–008 且须修是。席甲漏判的 001/004/005/007 已入队。§8.8 公开公司列表并入 003，不第二开添加。票 **PASS**。

### 001 独立裁定（技术性叠法／须修是）

见 (b)。工程缺陷是。发布前须修是。最小处置不发明业务，只引用既有 `verify:openapi` / `docs:operation-matrix` / `docs:openapi-closure` / `docs:security-baseline`、U13-001 重生顺序、禁止整树覆盖。票 **PASS**。

### 002 独立裁定（添加已答删除／须修是；同根 U04-001）

见 (a)。`:36884-36970` 独立布尔写口。`questions/U04-001.md` 答复首行「删除」，并写明 OpenAPI POST 随之去掉。删除该 path/operation 与 `self_operated` 布尔；员工分流读六类码（U05-001）；730→729 后生成物重生。不新开 2.2。票 **PASS**。

### 003 独立裁定（冲突／须修是；同根 U05-003/U07-004/U01-001）

见 (a)(c)。公开自助注册无设备公钥/指纹，终端含 `app`。随 U05-003 补设备证明（无指纹 4xx）；随 U01-001 去掉 `app`、归私人手机通道；随 U07-004 在该契约落地后保留 public 并加频控/来源校验。公司列表匿名发现不作为无绑定注册的前置。不新开 2.2。票 **PASS**。

### 004 独立裁定（冲突／须修是；同根 U05-005）

见 (a)。GET description 把空上下限写成出厂完成。随 U05-005/U10-003 改 description：读到出厂行（值 7 + 发布者写入的上下限）或缺行 fail-closed。禁止把空上下限写成合同。席位不代填上下限数字。票 **PASS**。

### 005 独立裁定（技术性／须修是；同根 U05-006）

`:36978`「员工取认证身份，柜机取设备身份」+ `x-auth-context: staff-token` + `x-terminal-allowed: [kiosk]` + `x-permission: staff.profile.self`；`:36986` 成功事件「staff.locker.store / staff.locker.retrieve」。HTML PGM L582 / BSM L938 柜子自动记录、不设手工登记。属把错误调用形态写进契约，不新造寄存业务。随 U05-006 改单端：柜机以已登记 kiosk **设备签名**上报，员工身份由扫码载荷承载；成功事件改单一名或按 action 分两个 operation。候选「设备令牌」应读成 U05-006 的「设备签名」，见文末第 1 条。票 **PASS**。

### 006 独立裁定（技术性／须修是；同根 U05-011）

`:36477`、`:36607` 两写口 `x-idempotency-required: false`。任职状态请求体现无 `idempotency_key`。与同文件其它员工写口及 U05-011 同根。改 true，请求体加幂等键，控制器按操作命名空间去重。幂等是重试协议，不发明任职/可见天数业务。票 **PASS**。

### 007 独立裁定（技术性／须修是）

schema required 含 `merchant_id`（`:46393-46398`），example `:38580` 给 `null`，副本 Java `:7` 注释「空表示公司自有员工」。商户代提交必填 merchant_id 是 HTML 两条路径的细化；缺陷是三套互斥。example 改非空占位；Java 注释随「公司员工走 self-registrations」删除「空=公司自有」。示例手机号用明显占位。票 **PASS**。

### 008 独立裁定（技术性／须修是）

8 个写口 `x-audit-event-failure: "none"`（含打卡/排班从基线既有 `*_failed` 退化）。main 720 口该字段从不为 none。verifier 只查非空，门禁不红。写口恢复或新给 `*_failed` 事件名（打卡至少恢复 `staff.attendance.punch_failed`）；不得用字面量 none；柜机失败名随 005。第 5/6 段 `recordFailure` 是实现面，随契约事件名同 PR，不在本段发明新事件业务。票 **PASS**。

### 009 独立裁定（登记／须修否）

见 (d)。双方观察归并，工程缺陷否，发布前须修否。OBS-1 设置口 `x-data-scope: project` 按 U01「main 既有」口径须修否、归 M391。`info.version` V0.14、712 个未改 operation 英文 summary、AccessUserRequest phone_hash 同口径。票 **PASS**。

### 最小修正意见（非阻断，不改票）

1. U14-005 处置「kiosk 设备令牌」定稿写成「kiosk **设备签名**」，与 U05-006 / GR-005 / U13-006 一致，不得读成另发设备令牌。分类/须修不变。
2. U14-003「公司列表匿名发现须另证来源校验」定稿补回 GR-003 条件句：「若仍需匿名发现，须另证来源校验，不能作为无绑定注册的前置」。来源校验已在 U07-004，不发明。
3. U14-003 同根 U01-001 是私人手机通道/不自报 app；设备公钥/指纹是 U05-003 / U01-007 / U02-002。处置已两句都有，定稿不得读成只改通道。
4. U14-003 字段名 `device_public_key`/`device_fingerprint_hash` 随 U05-003 语义，不把属性名钉成新业务字段。
5. U14-001「生成 yaml」=与 U15 同批改到 `verify:openapi` 深等；附录用既有 `docs:openapi-closure`。单元格可补脚本名，不改票。
6. U14-009 席甲 OBS-4「打卡 device_fingerprint 可不填与 U07 一致」不得覆盖 §8.6 死字段。U07 是验签钥源；该字段声明不采用，随打卡改写可删，不单独立项。
7. U14-009 第 3 条「未重复错误句」只覆盖本段未把底薪+奖金称作总薪资；U05-004 实现面仍须修。
8. U14-001 处置格补「与 U05+U06+U07+U10+U13+U15+U16+U17 同一 PR」（候选汇总已有 U14+U15+U16+U17，单元格未写跨段实现面）。不改票。
9. U14-008 本段最小是改契约事件名；`recordFailure` 归第 5/6 段同 PR，不在本段发明事件清单。

全批差集无漏并。四分类与 GR-001…008 一致；席甲漏判四处候选已勘误。001 叠法成立且只引用既有门禁与生成器。003 处置与 U05-003/U07-004 定稿一致，来源校验只引用。009 未写错第 3/6/15 条原意，无应升队被降观察项。逐 ID 001–009 全 **PASS**。本席 B2-r1 **PASS**。未经双 PASS 不得把候选当定稿，更不称 U14 或 main 模块 PASS。步骤仍 B2；U14 IN_PROGRESS；材料未发布，不做 B3。

本批最终：**PASS**
