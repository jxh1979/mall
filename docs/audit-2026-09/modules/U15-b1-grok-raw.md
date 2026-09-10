CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U15 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 只读全量三线（未发布材料第 15 段；不做 B3/B4/B5；处置写给发布者，本轮不修源）
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 段：U15 — 未发布材料第 15 段「完整接口定义」
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 末尾附录；`plan/runbook.md` V0.02 §11；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 审计对象（不得冒称 main 或已发布修复）：
  - 未发布提交：`03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb`（本仓 `git cat-file -t` **无此 commit 对象**）
  - 未发布树：`e28dad059b2c7e08bcc95a8db18552d3f76edf4b`（`git cat-file -t` = **tree**，可解析）
  - 发布基线：`242f487233d028173cb9198eecd079f5d877405c`（commit，可解析）
  - 只读输入根：`/home/james/complex-management/.scratch/module-recheck-20260908/unpublished-03acdb98`
  - 清单：`.scratch/module-recheck-20260908/unpublished-03acdb98-input.json` V0.01，76 文件
- 本段：1 文件，物理行 37017，标记 **OVERSIZE-SINGLE-FILE**；main 主归属：**M392**（`docs/openapi/openapi-p0.yaml`）
- 冻结判据：M392 映射行 L6771 并集 **18** 项 = GEN-001–GEN-018（相对开工提示 MISSING=[] EXTRA=[]）；定义区从锁全文读过
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 且发布前须修：GR-U15-001、GR-U15-002、GR-U15-003、GR-U15-004、GR-U15-005、GR-U15-006、GR-U15-007、GR-U15-008
- 添加项（B1 只登记，不代用户回答 2.2）：**1 条 = GR-U15-002**，即 YAML 契约面的 `POST /api/v1/platform/merchants/{merchant_id}/self-operated`（`setMerchantSelfOperated`）独立自营布尔写口。操作员 2026-09-09 已对 `questions/U04-001.md` 答「删除」；U14-002 已定稿同根。本段是该添加的 YAML 序列化面，**不新开 2.2、不再问**。无其它新添加。
- 同根已定稿、不新开 2.2：U04-001 / U05-001 / U05-003 / U05-005 / U05-006 / U05-011 / U07-004 / U08-001 / U08-003 / U10-002 / U13-001 / U14-001…U14-008
- 只登记、建议队列「否」：观察见 §8
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做写操作
- 本报告不表示全仓、main 模块或发布门禁通过。M001–M012 检查点已合入当前 HEAD（PR #428/#429）。原 322/471 模块结论不继承到本未发布段。不得把本副本冒称 main 源码或已发布修复。不得把 U14 JSON 的 B2 定稿写成「本 YAML 已审过、可跳过」。
- 本席独立实跑（隔离 `/tmp/u15-b1-grok`，不改副本/main）：未发布 json+yaml 同树 `verify-openapi-contract.mjs` **PASS**（730 ops，paths/components 深等 0）；main json+yaml **PASS**（720 ops）；只把本段 YAML 覆到 main JSON：`verify-openapi-contract.mjs` **FAIL**（39 处 json↔yaml 深不等）；只把未发布 JSON 覆到 main YAML：同样 **FAIL** 39 处；未发布 JSON + main 矩阵：`generate-openapi-operation-matrix.mjs --check` **FAIL**；**只覆 YAML、JSON/矩阵仍 HEAD：矩阵 `--check` PASS（720，矩阵脚本只读 JSON，抓不到 YAML 漂移）**。副本自洽 **不能** 消解 HTML 冲突或对 main 的契约分叉。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`（SHA256 `fa5c1e5ee76b1c8f1ae56101f52c9e239de75d5c578adc61227b92d10b7e52ef`） | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md`（`disable-model-invocation: true`，本体转发 grilling） | 内部 sharpen |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md`（SHA256 `b9339b09ee3980808d8c5a35c7251b891b8b1e0036ec4ca37812b976ebddf6b6`） | 隔离 `/tmp` 叠 YAML/JSON 复现 39 处漂移、矩阵 YAML 盲区 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md`（SHA256 `808fc5717aa88ad65efff312b11c186294d3e6ee301afb584e2f86599b137787`） | 只借读错误→复现→追数据流（本段 YAML → U14 JSON / U05/U07/U08 副本 → main 对照 → HTML） |
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

1. 这 1 个 37017 行 YAML 必须存在吗？必须有**等价的 P0 正式契约 YAML 面**（paths + components + x-permission/x-auth-context/x-terminal）。仓库门禁 `verify-openapi-contract.mjs` 把 json↔yaml 深比较写成硬条件。删掉它，M392 没有本段这一层，只留 JSON 则 verify 红。文件作为契约是细化，不是添加。未发布增量（+10 operation / 改 8 个既有 staff 口）才是本段要审的业务面。
2. 整段按现文件叠进当前 main、首轮会不会过门禁？**只叠 YAML 不会。** main JSON 仍是基线，`verify-openapi-contract.mjs` 报 39 处深不等（CMD-YAMLONLY EXIT 1）。矩阵脚本**只读 JSON**，只叠 YAML 时 `--check` 仍绿（CMD-MATRIX-YAMLONLY EXIT 0，720 ops）——这是本段相对 JSON 段的固定对抗面：YAML 漂移可被矩阵漏掉，只能靠 verify-openapi。json+yaml 同批在 `/tmp` 可绿，只证明未发布树自洽。
3. 本文件会不会覆盖 main 已修的 M009–M012？**对本路径不会回退。** `git rev-parse` 显示 main 该 blob == 基线 blob `161f4c1a…`，`git log 242f4872..HEAD -- docs/openapi/openapi-p0.yaml` 空。对抗式固定问题的真实形态是：整文件覆盖会把已定稿删除的自营写口和公开无设备注册契约**写进**当前 main，而不是回退 Java 侧 M009 修复。
4. 公开 `POST /staff/self-registrations` 无设备字段、`security: []`，首轮正常客户端按契约编码就会发出无绑定的明文手机号。不是两三件不巧。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 sha256 为 `e27adda4…`，内容行即锁哈希）
- `criteria-lock.md` 版本 V0.11；判据总数 401
- M392 映射行 L6771：`[GEN-001 … GEN-018]` 共 18 ID，与开工提示逐项相同；定义区 §general L12–L100 均从锁全文读过
- HTML 出处：本段映射无 BEQR/BSM/PGM ID；分类仍按 GEN-017 对照三份 HTML 正本原文（见各 finding 引用行）。不把 HTML 锚点 ID 写入本段冻结判据格。

HEAD（独立实算）：

- `git rev-parse HEAD` = `git rev-parse origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`
- `git log -1 --oneline`：`445a49f4 Merge pull request #429 from jxh1979/audit/20260909/M011-M012-checkpoint`
- 本席对照 main 已修项时以 **当前 HEAD=445a49f4** 为准（含 M001–M012）
- 本席未设 `GIT_INDEX_FILE`，未动主索引

ledger 只读核验（不改）：V2.86，521634 bytes，SHA256 `4f5c9a5167cfdc95aebd00710501da0ae1e40f3da08e64a08b20246fa1af3287`（与开工提示一致）。findings V1.71，671691 bytes，SHA256 `4400493e8e612263df453f3d88e4c1a766f388b3bcae13ec385a4e97aede7a2e`（与开工提示一致）。台账中与本段直接相关的已定稿项（背景，不继承 PASS）：

- U00-000：未发布树相对 242f4872 全 76 文件与 main 分叉
- U04-001：独立自营布尔 / `setMerchantSelfOperated` 已答删除
- U05-003：自助注册须带设备公钥/指纹
- U05-005：可见天数缺行不得空上下限
- U05-006：柜机双身份
- U05-011：employment-status 与 shift-visible-days 写口缺幂等键
- U08-003：OpenAPI `setMerchantSelfOperated` 随 001 删除
- U13-001：安全基线钉未发布 **JSON** SHA `f8de4f8c…`、730 ops / public 7（main 720/5）；**未钉本 YAML SHA `93434ad6…`**
- U14-001…008：同契约 JSON 面已 B2 定稿 REVIEWED-FAIL；本段是 YAML 序列化，须独立落临时 ID，不把 U14 正式 ID 写进本段 finding 格冒充已审

### 1.1 输入副本与树对象

| 项 | 值 |
|---|---|
| 副本路径 | `.scratch/module-recheck-20260908/unpublished-03acdb98/docs/openapi/openapi-p0.yaml` |
| 副本 SHA256 | `93434ad64c0c6cb18d98f9bc76399e80130d57ab66d477e13faffce53624cc9d` |
| 清单条目 | 同 SHA、37017 行、1315326 bytes；本席 `sha256sum`/`wc`/`stat` 与清单一致 |
| `git hash-object` 副本 | `1ab4fd6009fdcaeddfb016d339678d860b470ca6` = `e28dad05:docs/openapi/openapi-p0.yaml` |
| 基线 blob | `161f4c1aa1609460a3fd80e1be7efeeeaebc175a` |
| main HEAD blob | `161f4c1aa1609460a3fd80e1be7efeeeaebc175a`（**等于基线**） |
| main 文件 SHA256 | `ae9f6f0c6eeaee2e9e8d4a841dc50d9caa5315e9ef1d811d895b033f013edf10`（36485 行 / 1298424 bytes） |
| 未发布 json（U14，只读对照） | SHA256 `f8de4f8c2957953911030512a3502b6a5d8bdc4ffd57dc6d3c4049a0df788384`；与本段 YAML 在副本树内 paths+components+info/tags/x-* **深等价 0 diff**（CMD-PARSE / CMD-COPY PASS） |

76 文件清单本席核对本条 1/1 哈希。席甲声称 76/76 为背景；本席只对 U15 这一文件独立实算。

### 1.2 相对发布基线 diff stat（本席实跑）

```
git diff --numstat 242f487233d028173cb9198eecd079f5d877405c e28dad059b2c7e08bcc95a8db18552d3f76edf4b -- docs/openapi/openapi-p0.yaml
712	180	docs/openapi/openapi-p0.yaml
git diff --stat 242f4872 e28dad05 -- docs/openapi/openapi-p0.yaml
 docs/openapi/openapi-p0.yaml | 892 ++++++++++++++++++++++++++++++++++---------
 1 file changed, 712 insertions(+), 180 deletions(-)
git diff --shortstat 242f4872 e28dad05
76 files changed, 4745 insertions(+), 1084 deletions(-)
git diff --stat HEAD e28dad05 -- docs/openapi/openapi-p0.yaml
 docs/openapi/openapi-p0.yaml | 892 +++++++++----------------------------------
 1 file changed, 180 insertions(+), 712 deletions(-)
  （方向：未发布树 → HEAD，即 HEAD 比未发布少 712 行、多 180 行）
git log --oneline 242f4872..445a49f4 -- docs/openapi/openapi-p0.yaml
(空)
```

即：本文件相对基线 = 相对当前 main；main 自基线**未改**该路径。全 76 文件合计 +4745/−1084 与开工提示一致。

## 2. OVERSIZE 全文件覆盖表

不得把「只看 diff」称为全量。覆盖方法：仓库 `yaml` 包（与 `verify-openapi-contract.mjs` 同一解析器，`maxAliasCount: -1`）整文件一次解析成功；再按结构段通读新增/改写原文，未改段用与 main 的 operation 深等证明（712 个）。

| 结构段 | 行号 | 覆盖动作 |
|---|---|---|
| `openapi` / `info` / `servers` / `security` / `tags` | 1–32 | 全文阅读。`openapi: 3.1.0` 与 main 同；`info.version` 基线/main `0.1.0` → 未发布 `V0.14`；title「综合体管理系统 P0 API 契约」；description 写「JSON-compatible YAML」；servers/security/tags 20 个与 unpublished JSON 深等 |
| `paths` 604 条 | 33–26269 | 全量解析 730 个 HTTP operation；与 unpublished JSON 730 个 paths+components **0 深 diff**（键序不计）；与 main 720 个逐 operation 深比较：712 深等、8 改写、10 新增、0 删除 |
| **YAML 键序**（相对 JSON） | 全 paths | YAML 把 8 条新 path **前置**（L34 起）；JSON 把同 8 条 **追加在末尾**。604 条 path 键序全错位，语义仍深等。verify 不计键序 |
| 新增 8 路径 / 10 operation | 34–558 | **全文通读**（history / shift-visible-days GET+POST / payroll-penalty-cap GET+POST / self-registrations / registration-companies / employment-status / locker-events / self-operated） |
| 既有 paths 前缀（void-reason-codes 起至 registrations 前） | 559–10087 | 与 main 深等；抽读 L559–570 `GET /api/v1/bar/void-reason-codes` 确认 operationId/中文 summary 结构 |
| 改写：registrations / review / device-credentials | 10088–10253 | 全文 |
| 中间未改 paths | 10254–22620 | 与 main 深等；抽读 L15000–15040 `POST /api/v1/inventory/saved-bottles/{id}/dispute` |
| 改写：shifts / batch / mine / punch | 22621–22904 | 全文（含 punch method enum / device_fingerprint 死字段） |
| 中间未改 | 22905–23414 | 与 main 深等（`/attendance/mine` 等） |
| 改写：month-view | 23415–23447 | 全文 |
| 其余未改 paths 至 components 前 | 23448–26269 | 与 main 深等 |
| `components.securitySchemes` | 26271–26287 | 全文。4 套：BearerAuth / DeviceCredential / OpenAppSignature / WebhookSignature，与 main 键集合相同 |
| `components.parameters` | 26288–26487 | 25 个，与 main 键数相同 |
| `components.requestBodies` | 26488–27981 | 216 个；内容深比较仅 `StaffRegistrationRequest` example 改变。该 example **全文** L27308–27320 |
| `components.responses` | 27982–28031 | 全文：Success / Page / Error / WebhookAccepted |
| `components.schemas` | 28032–36988 | 229 个，与 main 键数相同；仅 `StaffRegistrationRequest` 改变。该 schema **全文** L33334–33375 |
| `x-contract-rules` / `x-contract-hardening` / `x-p0-example-coverage` | 36989–37017 | 全文，与 unpublished JSON 及 main **深等**（策略仍写 full examples 是 P1；8 个既有 request_bodies，不含本段 10 个新 operation） |

`$ref` 全树由 `verify-openapi-contract.mjs` 解析（副本 json+yaml PASS：无空键、无悬挂 ref、operationId 唯一）。路径键无重复（604 unique）。YAML 1.1 布尔词 `yes/no/on/off` 未引号出现次数 = 0。`<<:` merge key = 0。

YAML 锚点（本文件特有序列化，JSON 无此面）：`&a1` 定义 2 次（L826 `responses: &a1`、L7759 `"403": &a1`），`*a1` 使用 25 次。`git diff 242f4872 e28dad05 -- 本文件` **零命中** `&a1`/`*a1`（基线既有）。官方 `yaml` 包默认即可解析（CMD-PARSE YAML-DEFAULT-OK）。PyYAML `safe_load` 因重复锚点 ComposerError（基线同样红，见 §8）。不把基线既有解析器差写成新根因。

### 2.1 相对基线/main 的 operation 差集

新增 10（8 路径；shift-visible-days 与 payroll-penalty-cap 各 GET+POST）：

| method | path | operationId | YAML 行 |
|---|---|---|---|
| GET | `/api/v1/staff/shifts/{id}/history` | listStaffShiftHistory | 34 |
| GET | `/api/v1/staff/settings/shift-visible-days` | getStaffShiftVisibility | 74 |
| POST | `/api/v1/staff/settings/shift-visible-days` | setStaffShiftVisibility | 106 |
| GET | `/api/v1/staff/settings/payroll-penalty-cap` | getStaffPenaltyCap | 163 |
| POST | `/api/v1/staff/settings/payroll-penalty-cap` | requestStaffPenaltyCap | 195 |
| POST | `/api/v1/staff/self-registrations` | submitStaffSelfRegistration | 279 |
| GET | `/api/v1/staff/registration-companies` | listStaffRegistrationCompanies | 347 |
| POST | `/api/v1/staff/profiles/{id}/employment-status` | setStaffEmploymentStatus | 379 |
| POST | `/api/v1/staff/locker-events` | recordStaffLockerScan | 442 |
| POST | `/api/v1/platform/merchants/{merchant_id}/self-operated` | setMerchantSelfOperated | 497 |

改写 8（路径已存在于 main）：

| method | path | operationId | YAML 行 |
|---|---|---|---|
| POST | `/api/v1/staff/registrations` | submitStaffRegistration | 10088 |
| POST | `/api/v1/staff/registrations/{id}/review` | reviewStaffRegistration | 10121 |
| POST | `/api/v1/staff/device-credentials` | bindStaffDeviceCredential | 10221 |
| POST | `/api/v1/staff/shifts` | upsertStaffShift | 22621 |
| POST | `/api/v1/staff/shifts/batch` | batchUpsertStaffShifts | 22729 |
| GET | `/api/v1/staff/shifts/mine` | listMyStaffShifts | 22817 |
| POST | `/api/v1/staff/attendance/punch` | punchStaffAttendance | 22840 |
| GET | `/api/v1/staff/attendance/month-view` | myStaffAttendanceMonthView | 23415 |

schema/requestBody 改写：`StaffRegistrationRequest`（schema L33334–33375；requestBody example L27308–27320）。

public 口：main 5 个（login / customer phone-login / customer logout / ticket-products / health）→ 未发布 **7** 个（加 registration-companies、self-registrations）。与 U13-001 钉的 730/public 7 一致。

### 2.2 跨段数据流（只读，不审他段为 PASS）

```
openapi-p0.yaml (U15)
  ≡ openapi-p0.json (U14)  paths+components+info/x-* 深等 0
  ├─ POST /staff/self-registrations, GET /staff/registration-companies
  │     → 副本 StaffController.java:46-56 + StaffRegistrationService
  │     → 副本 SecurityConfig.java:118-122 permitAll（L121-122 两条）
  │     → 副本 dto/StaffSelfRegistrationRequest.java（六字段，无设备公钥/指纹）
  │     → main SecurityConfig 无这两条（公开面是本材料引入）
  │     → HTML PGM L578-579 / BSM L935 / BSM L1433
  ├─ POST /platform/merchants/{id}/self-operated
  │     → 副本 PlatformController.java:86-92 + MerchantSelfOperatedRequest
  │     → U04-001 已答删除；main 无此端点
  ├─ POST /staff/locker-events
  │     → 副本 StaffLockerService.java:40-54（requirePermission staff.profile.self + kiosk verified）
  │     → 副本 StaffController.java:40-44 有 requireIdempotencyKey
  │     → U10 V409 staff_locker_records；HTML PGM L582 / BSM L938
  ├─ POST employment-status / POST shift-visible-days
  │     → 副本 StaffController.java:95-98 / 203-206 无 requireIdempotencyKey（U05-011）
  ├─ GET shift-visible-days 文案「上下限为空」
  │     → 副本 JdbcStaffRepository.java:61-67 缺行 orElse(defaultValue,null,null)；U10 V402 未种子该键
  ├─ punch method=charging_disconnect、终端仅 staff_work_phone
  │     → 副本 StaffService.punch + U10 V405 CHECK；HTML BEQR L280
  └─ 同树 json (U14) 与 yaml 深等价；main yaml 仍基线
        → 只合 U15 不合 U14 则 verify:openapi 红（39），但矩阵仍绿（只读 json）
        → 合 JSON 不合 U17 矩阵则 docs:operation-matrix --check 红
        → 安全基线三件钉 JSON SHA（U13-001），不钉本 YAML SHA
```

SQL/迁移：本段无 SQL。U10 V400–V409 只读对照（V408 `is_self_operated` 已答删除；V405 打卡 method；V409 柜机表；V402 只种子 `payroll_penalty_cap_bps`，不种子 `shift_visible_days`）。main 最高迁移 V410。本席未对 `complex_test*` 做写操作。

## 3. 第一性原理

1. **为什么必须存在。** P0 编码前置契约的 YAML 面：730 个（main 720）HTTP 操作的 path/method/operationId、权限、鉴权上下文、终端、幂等、审计事件名、字段级 requestBody。删除后 `verify:openapi` 的 json↔yaml 深比较没有对等物。HTML 做不到「十一端按契约访问」的机审层。必要性成立。YAML 不是第二份业务发明，是 JSON 的门禁对等序列化。
2. **声称的行为是否执行。** 本段是文档/契约，不是 Java。可执行声称 = 仓库门禁把 json 与 yaml 深比较、校验 x-* 扩展、解析 $ref。副本树 json+yaml：**执行且绿**。与 HTML、与 main 已定稿删除项、与未发布 Java 的一致性：**不绿**（见 findings）。不能把 CMD-COPY PASS 写成业务通过。
3. **未经验证前提。** （a）发布者会同时改 U14 JSON / U16 附录 / U17 矩阵 / U13 安全基线；（b）操作员删除独立自营后本契约会摘条；（c）公开注册会补设备证明；（d）有人以为「只合 YAML、矩阵绿」就算过。这四件在本材料里都还没发生。首轮按现文件发布就会撞门禁，或把已删 API 写进 main，或 YAML 漂移被矩阵漏掉。

## 4. 顺从式（GEN-001–018 逐项）

本段映射只有 GEN。每项都核过；「通过」仅指该判据在本文件上的适用结论，不升格为段 PASS。

| ID | 适用 | 结论 | 证据 |
|---|---|---|---|
| GEN-001 | 是 | **FAIL**（002） | 独立自营写口无 HTML 上位（六类合作方式已含「自营」）；`device_fingerprint` 声明「不采用」仍留字段（观察，不单独立项） |
| GEN-002 | 是 | **FAIL**（001/004/007） | 只叠 YAML 门禁红（39）；矩阵对 YAML 盲；GET 可见天数把空上下限写成合同；example `merchant_id: null` 对 required |
| GEN-003 | 是 | 无测试面 | 本段无测试文件。`verify-openapi-p0-examples` 只检查 `x-p0-example-coverage` 既有 8 个 request bodies（CMD-EX PASS），**不覆盖** 10 个新 operation。不把该绿写成新接口已测 |
| GEN-004 | 是 | **FAIL**（003/005） | 公开自助注册无设备公钥/指纹；柜机口同时要求 staff-token 与 kiosk 验签 |
| GEN-005 | 是 | **FAIL**（003） | `x-auth-context: public` + `security: []` 把员工注册与公司列表放在默认拒绝之外 |
| GEN-006 | 无实现面 | 豁免（原因：本段无事务/约束 DDL；柜机「取空后可供他人」是描述，完整性在 U05/U10） | |
| GEN-007 | 是 | **FAIL**（006） | 两写口 `x-idempotency-required: false`，与同控制器其它写口及 U05-011 同根 |
| GEN-008 | 是 | **FAIL**（001–007） | yaml/json/矩阵/DTO/Java 注释/HTML 五态与公开面多处不一致；YAML 键序与 JSON 不同但不破深等 |
| GEN-009 | 是 | **FAIL**（008） | 8 个写口 `x-audit-event-failure: "none"`（main 720 口该字段从不为 none；打卡从 `punch_failed` 改为 none） |
| GEN-010 | 是 | **FAIL**（003） | 公开 POST 收 `phone`/`id_doc` 原值，无设备绑定、无认证 |
| GEN-011 | 是 | **FAIL**（005/008） | 柜机 success 写成 `staff.locker.store / staff.locker.retrieve`（斜杠+空格，不是单一事件名）；failure 字面量 none |
| GEN-012 | 无实现面 | 豁免（原因：契约无循环/连接/批大小实现） | |
| GEN-013 | 是 | **FAIL**（001） | 只合本段，`verify:openapi` 依赖未声明的 U14 同批；矩阵脚本不读 YAML，只合本段矩阵仍绿 |
| GEN-014 | 无实现面 | 豁免（原因：本段无迁移；V 号冲突在 U10-001） | |
| GEN-015 | 部分 | 观察 | 本段新增/改写 summary、description 为中文。712 个未改 operation 的英文 summary 与 main 字节级相同，归 M392 既有，不构成本段新根因。铁律 7 四类例外覆盖 operationId/字段名 |
| GEN-016 | 是 | **FAIL**（001/004） | GET 可见天数把「未配置上下限为空」写成当前合同；example 与 schema 互斥；info.version `V0.14` 只在未发布树；verify 深比较**不覆盖 info** |
| GEN-017 | 是 | **FAIL**（002/003/004） | 002=添加（已答删除）；003/004=冲突；005–008=技术性且逐条写了无业务发明理由。未把 HTML 沉默判冲突 |
| GEN-018 | 是 | 本席遵守 | 每条 finding 落到 HTML 原文或可复现命令；不把席位意见当依据 |

## 5. 对抗式

固定问题「发布会覆盖/冲突 main 已修项」：

- 本文件 main blob == 基线 blob，M009–M012 **没有**改 `docs/openapi/openapi-p0.yaml`。整文件覆盖**不会**回退那些模块在 Java/SQL 上的已修项。
- 会把未发布 +712/−180 **安装**到当前 main：10 个新 operation（含已答删除的自营写口、公开无设备注册）、8 个 staff 口改写（phone_hash→phone 明文、打卡 method、排班待审）。
- 只覆盖 YAML 保留 main JSON：39 处深不等，门禁红（CMD-YAMLONLY）；**矩阵仍绿**（CMD-MATRIX-YAMLONLY EXIT 0）。这是 YAML 段特有的漏检窗。
- 覆盖 JSON 保留 main YAML：39 处反向深不等（CMD-JSONONLY）；矩阵红（CMD-MATRIX）。
- 与 U13 已定稿安全基线：未发布钉 JSON 730/public 7 / SHA `f8de4f8c…`；main 720/5。YAML SHA `93434ad6…` 不在基线三件里。只合本段或只合 U13 都会让 `--strict` 或 verify:openapi 红。

其它反例：

- 匿名客户端按契约 POST `/staff/self-registrations`，body 仅 `idempotency_key`+`company_id`+`name`（phone 非必填），`X-Terminal-Type: app`：契约 200 成功路径，无设备字段可填。HTML 要求安装时绑定设备标识。
- `cooperationForm=lease` 的商户仍可通过 `setMerchantSelfOperated{self_operated:true}` 把员工赶到本人注册路径（U04-001 已证的双开关）；本契约是该开关的 HTTP 面。
- 打卡失败：契约 `x-audit-event-failure: none`，审计实现若按契约将不记失败事件。
- example `merchant_id: null` 对 required `merchant_id`：生成器/测试夹具会发出 schema 拒绝的 payload。
- 只合 YAML：发布者看到矩阵绿、误以为契约已同步，JSON 客户端仍是旧 720 口、无自助注册。

理论极端（收窄不修）：OpenAPI 被换成另一份运行时 springdoc 输出且无人跑 `verify:openapi`——要两件不巧，不修。JWT Bearer 顶层 security 被 public 口 `security: []` 覆盖是**显式设计**，不是极端。PyYAML 因重复锚点 `&a1` 加载失败：官方门禁用 node `yaml` 包且基线同样红，不修（§8）。

## 6. 与 main 分叉对照（只读）

| 项 | 未发布副本 YAML | 当前 main 445a49f4 YAML |
|---|---|---|
| blob | `1ab4fd60…` | `161f4c1a…` = 基线 |
| SHA256 | `93434ad6…` | `ae9f6f0c…` |
| 行/字节 | 37017 / 1315326 | 36485 / 1298424 |
| ops / paths | 730 / 604 | 720 / 596 |
| public | 7 | 5 |
| info.version | V0.14 | 0.1.0 |
| StaffRegistrationRequest | `phone`/`id_doc` 原值；required 含 `merchant_id` | `phone_hash`/`id_doc_hash`；required 无 merchant_id |
| example 注册 | `phone: "13800138000"`, `merchant_id: null` | `phone_hash: "sha256:phone_hash_placeholder"` |
| punch terminals | 仅 `staff_work_phone` | `staff_private_phone`+`web` |
| punch method | nfc/qr/charging_disconnect | nfc/qr/manual |
| punch failure 事件 | `none` | `staff.attendance.punch_failed` |
| 自营写口 | 有（L497） | 无 |
| 自助注册/公司列表 | 有，public | 无 |
| 与同树 JSON | paths/components/info/x-* 深等 0 | 与 main JSON 深等（基线） |
| `x-audit-event-failure==none` | 12 个 operation（8 个写口） | **0** |
| path 键序 | 新 path 前置 | 与 JSON 同序（无这 8 条） |

main 该文件无 M009–M012 修补。发布本段等于在当前 main 上应用 +712/−180，不是三方合并冲突，是**单向安装未发布契约**。U08 四个 Java 文件 main 已修、禁止整文件覆盖——本 YAML 路径本身未被 M009 改过，但安装后会把 U08/U04 已删的 HTTP 面重新写进 main。

## 7. Findings

### GR-U15-001

- **临时 ID**：GR-U15-001
- **完整原文**：只把本段 `openapi-p0.yaml` 叠到当前 main（JSON、operation 矩阵、闭合附录、安全基线仍是 HEAD）时，`tools/verify-openapi-contract.mjs` 因 json↔yaml 深比较失败（声称 39 处；本席 CMD-YAMLONLY 列出全部 39 条，含 8 条新 path「missing in json」、registrations summary/description/终端数组、shifts 审计事件、`shift_id` 缺 json、StaffRegistrationRequest phone vs phone_hash）。`tools/generate-openapi-operation-matrix.mjs --check` **在此叠法下仍绿**（720 ops，脚本只读 JSON，CMD-MATRIX-YAMLONLY EXIT 0）。反向只叠 JSON 保留 main YAML：verify 同样 39 处红（CMD-JSONONLY），且矩阵红（CMD-MATRIX EXIT 1）。未发布 json+yaml 同树则 verify-openapi PASS（CMD-COPY EXIT 0）。本文件 main 自基线未改，整文件覆盖不会回退 M009–M012 对该路径的修复（无），但会把 10 个新 operation 与 8 个改写安装进 HEAD。
- **文件:行号**：`docs/openapi/openapi-p0.yaml:1-37017`（整文件为发布物）；差集见 §2.1；39 处清单见 §9 CMD-YAMLONLY
- **业务影响**：发布门禁红；或只合 YAML 时矩阵仍绿、JSON 客户端停在 720 口。U13-001 已钉 JSON SHA 与 730/7，须同批重生。YAML SHA `93434ad6…` 现不在安全基线三件内。
- **可复现命令/最小反例**：CMD-YAMLONLY EXIT 1；CMD-JSONONLY EXIT 1；CMD-COPY EXIT 0；CMD-MATRIX EXIT 1；CMD-MATRIX-YAMLONLY EXIT 0。见 §9。
- **冻结判据 ID**：GEN-002、GEN-008、GEN-013、GEN-016
- **2.3 依据类型**：可复现命令输出
- **四分类**：技术性
- **无业务发明理由**：json/yaml/矩阵同步是既有仓库门禁，不新增业务规则。矩阵不读 YAML 是既有生成器实现，不是新业务。
- **工程缺陷及是否建议登记**：成立；建议 B2 队列「是」（同根 U14-001）
- **最小处置（写给发布者）**：以 main `445a49f4` 为底，本段与 U14 JSON、U16 附录、U17 矩阵同一 PR 重放；先按 002–008 改契约再**从定稿 JSON 生成** yaml/矩阵/附录，禁止手改 YAML 独走；U13 安全基线三件在 OpenAPI 定稿后 `docs:security-baseline` 重生（建议同时钉 YAML SHA，或明确「verify:openapi 已覆盖 yaml」）。禁止整树覆盖。迁移重编号归 U10（≥V411），本段无 SQL。

### GR-U15-002

- **临时 ID**：GR-U15-002
- **完整原文**：新增 `POST /api/v1/platform/merchants/{merchant_id}/self-operated`（operationId `setMerchantSelfOperated`），requestBody 必填布尔 `self_operated` + 缘由 + 幂等键；description「自营商户员工采用公司员工本人注册路径」；tags 为 `staff`（路径却在 `/api/v1/platform/merchants`）。这是独立于六类合作方式的第二轴写口。操作员已对 U04-001 答删除，并写明 OpenAPI POST 随之去掉。
- **文件:行号**：`docs/openapi/openapi-p0.yaml:497-558`（schema 布尔 L541–542；required L547–550）
- **业务影响**：契约一旦发布，客户端会继续打已删后端；租赁商户可被标成「自营」而走公司员工注册（与合同/结算用的 `cooperation_form` 分叉）。
- **可复现命令/最小反例**：CMD-PARSE 解析未发布 YAML，path 存在且 main 同 path 不存在。`questions/U04-001.md` 操作员答复「删除」。
- **冻结判据 ID**：GEN-001、GEN-008、GEN-017
- **2.3 依据类型**：HTML 原文（BSM L701「区分自营、租赁、联营、代运营、服务商接入、其他这六类合作方式」；PGM L495/L498 同旨六类；BSM L1433 / PGM L578「在商户档案上标注为自营的商户视同公司自有」——落在六类码，不是新布尔列/新 POST）
- **四分类**：添加（操作员已答删除；引用 `questions/U04-001.md`，不新开 2.2）
- **工程缺陷及是否建议登记**：成立；建议 B2 队列「是」（同根 U14-002 / U04-001 / U08-001）
- **最小处置**：发布前删除该 path/operation 及一切 `self_operated` 布尔字段；员工分流改读合作方式码 `self_operated`（U05-001）。随 001 同 PR。

### GR-U15-003

- **临时 ID**：GR-U15-003
- **完整原文**：`GET /api/v1/staff/registration-companies` 与 `POST /api/v1/staff/self-registrations` 均 `x-permission: public`、`x-auth-context: public`、`security: []`、`x-terminal-allowed` 含 `app` 与 `staff_private_phone`。自助注册 requestBody 字段仅 idempotency_key/company_id/merchant_id/name/phone/id_doc，**无设备公钥/指纹**；required 甚至不含 phone。这把「安装时绑定该手机专属设备标识」从契约里拿掉，并允许客人端码 `app` 打员工注册。副本 `SecurityConfig.java:121-122` permitAll 与 `StaffSelfRegistrationRequest` 六字段与此同构（U05-003/U07-004/U01-001/U14-003 已定稿）。
- **文件:行号**：`docs/openapi/openapi-p0.yaml:279-378`（public/security L286–288、L346；终端 L289–291、L357–359；schema L317–345）
- **业务影响**：任意客户端可按契约枚举有效公司并提交姓名/手机/证件待审申请，无设备绑定、无认证。PII 在公开写口上以原值传输。
- **可复现命令/最小反例**：契约本身即最小反例（无设备字段可填）。HTML PGM L579「私人手机 App 安装时生成并绑定一个该手机专属的设备标识」；BSM L935 同旨；BEQR L233 终端十一端将「客人手机应用」与「员工私人手机」分开。
- **冻结判据 ID**：GEN-004、GEN-005、GEN-008、GEN-010
- **2.3 依据类型**：HTML 原文
- **四分类**：冲突（同根 U05-003 / U07-004 / U01-001 / U13-004 / U14-003）
- **工程缺陷及是否建议登记**：成立；建议 B2 队列「是」
- **最小处置**：自助注册改为私人手机通道；请求增加设备公钥/指纹，无指纹 4xx；`x-terminal-allowed` 去掉 `app`；permitAll 仅在该契约落地后保留并加频控（U07-004）。公司列表若仍需匿名发现，须另证来源校验，不能作为无绑定注册的前置。不新开 2.2。

### GR-U15-004

- **临时 ID**：GR-U15-004
- **完整原文**：`GET /api/v1/staff/settings/shift-visible-days` description「按当前经营主体读取；出厂包含今天在内七天，**未配置的上下限为空**。」把 U05/U10 已定稿的缺行 `(7,null,null)` 写成正式合同。HTML 要求出厂默认 7 天且须同时配置上下限。同文件 POST 已要求 min/max/reason（L111、L158–162），GET 与 POST 口径自相矛盾。V402 只种子处罚上限，不种子可见天数。
- **文件:行号**：`docs/openapi/openapi-p0.yaml:80`
- **业务影响**：客户端会把空上下限当合法出厂完成；与 BSM「未填写上下限的参数不得上线」冲突。
- **可复现命令/最小反例**：description 原文。HTML BSM L1444「出厂默认 7 天，须同时配置上下限」。副本 `JdbcStaffRepository.java:67` `orElse(new StaffNumericSetting(defaultValue, null, null))`。
- **冻结判据 ID**：GEN-002、GEN-008、GEN-016
- **2.3 依据类型**：HTML 原文
- **四分类**：冲突（同根 U05-005 / U10-003 / U14-004）
- **工程缺陷及是否建议登记**：成立；建议 B2 队列「是」
- **最小处置**：随 U05-005/U10-003 改 description：读到出厂行（值 7 + 发布者写入的上下限）或缺行 fail-closed。禁止把空上下限写成合同。席位不代填上下限数字。

### GR-U15-005

- **临时 ID**：GR-U15-005
- **完整原文**：`POST /api/v1/staff/locker-events` description「员工取认证身份，柜机取设备身份」；`x-auth-context: staff-token` 且 `x-terminal-allowed: ["kiosk"]`、`x-permission: staff.profile.self`。这是双身份调用形态。`x-audit-event-success` 值为 `staff.locker.store / staff.locker.retrieve`（含空格与斜杠），不是单一事件名；副本 `StaffLockerService.java:81` 实际记 `staff.locker.{action}`。HTML 只要求柜子自动记录是谁/哪一格/时间，不要求调用方同时持有员工会话和柜机验签。
- **文件:行号**：`docs/openapi/openapi-p0.yaml:442-456`（description L448；auth L451；terminal L452–453；success L455）
- **业务影响**：无同时持有两套身份的单端调用方（U11-006 已证测试靠注入）；契约把该形态冻结。审计采集若按字面量匹配会找不到事件。
- **可复现命令/最小反例**：字段原文。HTML PGM L582 / BSM L938「由柜子自动记录，不设手工登记」。
- **冻结判据 ID**：GEN-002、GEN-004、GEN-008、GEN-011
- **2.3 依据类型**：HTML 原文
- **四分类**：技术性（同根 U05-006 / U13-006 / U14-005）
- **无业务发明理由**：鉴权上下文与审计事件名是实现可观测性/调用形态，不发明「要不要寄存柜」这条业务。
- **工程缺陷及是否建议登记**：成立；建议 B2 队列「是」
- **最小处置**：随 U05-006 改为单端：柜机以已登记 kiosk 签名上报，员工身份由扫码载荷承载；`x-audit-event-success` 改为单一名或按 action 分两个 operation。删除并列双身份表述。

### GR-U15-006

- **临时 ID**：GR-U15-006
- **完整原文**：`POST /api/v1/staff/profiles/{id}/employment-status` 与 `POST /api/v1/staff/settings/shift-visible-days` 均 `x-idempotency-required: false`（L393、L119）。同文件其它员工写口为 true（self-registrations L292、penalty-cap POST L208、locker L454、shifts L22633）。副本 `StaffController.java:95-98`、`:203-206` 无 `requireIdempotencyKey`，U05-011 已定稿须补。契约把缺幂等写成正式。两口 requestBody 亦无 `idempotency_key` 字段。
- **文件:行号**：`docs/openapi/openapi-p0.yaml:393`、`119`
- **业务影响**：任职状态与可见天数写口重试/双击可重复生效；与同控制器其它写口不一致。
- **可复现命令/最小反例**：字段原文。对照 `POST /staff/registrations` L10102 `x-idempotency-required: true`。
- **冻结判据 ID**：GEN-007、GEN-008
- **2.3 依据类型**：可执行产物（契约字段 + 副本控制器缺校验，U05-011）
- **四分类**：技术性（同根 U05-011 / U14-006）
- **无业务发明理由**：幂等键是重试协议，不发明任职状态或可见天数业务。
- **工程缺陷及是否建议登记**：成立；建议 B2 队列「是」
- **最小处置**：两口改为 `x-idempotency-required: true` 并在 schema 增加 `idempotency_key`，与 U05-011 同 PR 在控制器校验并按操作命名空间去重。

### GR-U15-007

- **临时 ID**：GR-U15-007
- **完整原文**：`components.schemas.StaffRegistrationRequest` required 含 `merchant_id`（L33338–33342），property 描述「代员工提交的商户编号，必填」。同名 requestBody example 仍给 `merchant_id: null`（L27317），并改用 `phone: "13800138000"`。副本 `StaffRegistrationRequest.java:7` 注释仍写「merchant_id 为空表示公司自有员工」。schema、example、Java 注释三套互斥。
- **文件:行号**：schema L33334–33375；example L27308–27320；对照副本 `backend/src/main/java/com/complex/platform/access/staff/dto/StaffRegistrationRequest.java:7`
- **业务影响**：按 example 编码的客户端会被 schema 拒绝；按 Java 注释传空 merchant_id 会被 OpenAPI required 判非法。商户代提交路径本身（必填 merchant_id）是 HTML 细化，缺陷是契约自洽。
- **可复现命令/最小反例**：CMD-PARSE 打印 schema.required=`['idempotency_key','project_id','name','merchant_id']` 与 example.merchant_id=`None`。
- **冻结判据 ID**：GEN-002、GEN-008
- **2.3 依据类型**：可执行产物
- **四分类**：技术性（同根 U14-007）
- **无业务发明理由**：example 与 required 对齐是契约正确性，不发明商户代提交。
- **工程缺陷及是否建议登记**：成立；建议 B2 队列「是」
- **最小处置**：example 改为非空 merchant_id（测试占位符即可）；Java 注释随「公司员工走 self-registrations」删掉「空=公司自有」。phone 示例勿用可被误认为真实号的值（可用明显占位）。

### GR-U15-008

- **临时 ID**：GR-U15-008
- **完整原文**：本材料把字面量 `"none"` 写入 `x-audit-event-failure`（main 720 个 operation 该字段从不为 none，success 也不为 none）。写口 8 个：`upsertStaffShift`/`batchUpsertStaffShifts`（从 `staff.shift.save_failed` 改来）、`punchStaffAttendance`（从 `staff.attendance.punch_failed` 改来）、`submitStaffSelfRegistration`、`setStaffEmploymentStatus`、`setStaffShiftVisibility`、`requestStaffPenaltyCap`、`recordStaffLockerScan`。`verify-openapi-contract.mjs` 只检查字段非空，把 `"none"` 当合法事件名，门禁不红。
- **文件:行号**：L22635 shifts、L22743 batch、L22853 punch、L294 self-reg、L395 employment-status、L121 visibility POST、L210 penalty-cap POST、L456 locker
- **业务影响**：按契约实现的失败审计将寻找名为 none 的事件或不记失败；打卡失败留痕被显式拿掉。
- **可复现命令/最小反例**：CMD-PARSE 统计 unpublished YAML 12 / main 0。main punch 对照 `git show HEAD:docs/openapi/openapi-p0.yaml` 同 operation 的 failure 为 `staff.attendance.punch_failed`。
- **冻结判据 ID**：GEN-009、GEN-011
- **2.3 依据类型**：可复现命令输出
- **四分类**：技术性（同根 U14-008）
- **无业务发明理由**：审计事件名是可观测性约定，不发明打卡/排班业务。
- **工程缺陷及是否建议登记**：成立；建议 B2 队列「是」
- **最小处置**：写口恢复或新给 `*_failed` 事件名（打卡至少恢复 `staff.attendance.punch_failed`）；GET 若确无审计可在门禁允许的前提下留空策略由发布者统一，不得用字面量 none 混充。柜机失败名随 005 一并改。

## 8. 观察（无新根因，完整登记，建议队列「否」）

1. 712 个未改 operation 与 main 深等；其中英文 summary 是 M392 既有，本段未加重。
2. YAML path 键序与同树 JSON 全错位（新 path YAML 前置、JSON 追加）；verify 不计键序，深等 0。重生 YAML 时按 JSON 键序生成即可，不单独立项。
3. 重复锚点 `&a1` 定义 2 次（L826、L7759）+ `*a1` 25 次：`git diff` 本文件零命中，基线/main 同样 2/25。官方 node `yaml` 包解析绿；PyYAML `safe_load` ComposerError（基线同样红）。官方门禁路径不经 PyYAML。收窄不修。
4. `generate-openapi-operation-matrix.mjs` 只读 JSON：只叠 YAML 矩阵仍绿。并入 001 处置（同 PR 从 JSON 生成），不第二开。
5. U13 安全基线钉 JSON SHA 不钉 YAML SHA。verify:openapi 已覆盖 yaml↔json。并入 001/U13-001 重生，不第二开。
6. `verify-openapi-contract.mjs` 深比较只走 paths+components，**不比 info.version**（V0.14 vs 0.1.0）。同树 json/yaml info 已对齐。登记。
7. `GET payroll-penalty-cap` L169「提成不入分母，事假另算」与 HTML 总薪资定义（不含提成）及「请假扣薪不计入合计上限」同向；**未**把底薪+奖金称作总薪资。分母缺补贴/绩效是 U05-004 实现面，本段描述未重复该错误句。不升冲突。
8. `employment_status` 五码 `pending_entry/employed/trial/suspended/departed` 与 BEQR L240 五态一一对应；英文码属系统内部标识符（铁律 7）。不升冲突。中文展示归 UI（U03）。
9. punch 去掉 `manual`、终端收窄为工作手机、加入 `charging_disconnect`，与 BEQR L280 同向。这是本材料改对的部分。
10. `device_fingerprint` 保留但声明不采用（L22891–22893）：死字段，GEN-001 气味；随打卡改写可删。不单独立项。
11. `x-p0-example-coverage` 与 main 相同，策略写明「full operation examples are P1」；新高风险写口无 example，examples 门禁仍绿（CMD-EX PASS，8+3）。登记。
12. 公开公司列表在 U05-012 已作观察；本契约把它锁成 public。并入 003 处置，不第二开添加。
13. `setMerchantSelfOperated` tags=`staff` 随 002 删除消失。
14. 本段无 SQL、无测试文件、无 UI。未跑 Spring / 未写库。CMD-COPY 绿只证明未发布 json↔yaml。
15. 提交 `03acdb98` 本仓无 commit 对象；审查输入是树 `e28dad05` 的只读副本。
16. `info.description` 仍说「本文采用 JSON-compatible YAML」——main 既有套话。
17. public 口 responses 模板含 401/403/404/409，与 `security: []` 并列，属样板复制，不单独立项。
18. `x-contract-rules.terminalAllowedRule` 写「D5 的 10 个终端码」；BEQR L233 是十一端。该句与 main 深等，本段 diff 未触及。归 M392 既有，不新开。
19. GET 四处 `x-audit-event-success/failure: none`（history L49–50、vis GET L89–90、penalty GET L178–179、companies L361–362）随 008 由发布者统一 GET 策略，不另开。
20. 席甲开工笔记写「YAML 不逐行通读」。本席未采信该覆盖法：新增/改写原文通读 + 712 未改 operation 与 main 深等 + 门禁全树 $ref。

## 9. 命令证据

隔离根：`/tmp/u15-b1-grok`（软链仓库 `node_modules`，不装依赖、不写副本/main）。

### CMD-LOCK

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
```

sidecar 内容同行。ledger 521634 / `4f5c9a51…`；findings 671691 / `4400493e…`。

### CMD-BLOBS / CMD-HASH

```
git rev-parse HEAD == origin/main == 445a49f409f0cfbe6dd616ae079be1299824c48f
git cat-file -t e28dad059b2c7e08bcc95a8db18552d3f76edf4b  → tree
git cat-file -t 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb → fatal: could not get object info
git rev-parse 242f4872:docs/openapi/openapi-p0.yaml → 161f4c1aa1609460a3fd80e1be7efeeeaebc175a
git rev-parse HEAD:docs/openapi/openapi-p0.yaml     → 161f4c1aa1609460a3fd80e1be7efeeeaebc175a
git rev-parse e28dad05:docs/openapi/openapi-p0.yaml → 1ab4fd6009fdcaeddfb016d339678d860b470ca6
git hash-object <副本 yaml>                         → 1ab4fd6009fdcaeddfb016d339678d860b470ca6
sha256sum 副本 yaml → 93434ad64c0c6cb18d98f9bc76399e80130d57ab66d477e13faffce53624cc9d
wc -l/-c 副本 → 37017 1315326
```

### CMD-DIFFSTAT

见 §1.2。全 76 文件 `76 files changed, 4745 insertions(+), 1084 deletions(-)`。

### CMD-COPY（未发布 json+yaml）EXIT 0

```
OpenAPI contract verification passed.
```

### CMD-MAIN EXIT 0

```
OpenAPI contract verification passed.
```

### CMD-YAMLONLY（main json + 未发布 yaml）EXIT 1

```
OpenAPI contract verification failed:
- openapi-p0.json vs openapi-p0.yaml: 39 deep-equality diff(s) in paths/components; first 10:
-   paths./api/v1/staff/registrations.post.summary: "员工自助注册" (json) vs "商户代员工提交注册" (yaml)
-   paths./api/v1/staff/registrations.post.description: "员工私人手机 App 提交注册（个人信息只传不可逆哈希 phone_hash/id_doc_hash，PIPL）。…" (json) vs "项目和商户编号必填；…" (yaml)
-   paths./api/v1/staff/registrations.post.x-terminal-allowed: array length 2 (json) vs 4 (yaml)
…（门禁只打印前 10；本席独立 walk 全 39 条如下）
01 registrations.post.summary
02 registrations.post.description
03 registrations.post.x-terminal-allowed 长度 2 vs 4
04 registrations/{id}/review.post.description
05 device-credentials.post.description
06 shifts.post.description
07 shifts.post.x-audit-event-success staff.shift.saved vs governance.supervisor_review.evaluated
08 shifts.post.x-audit-event-failure staff.shift.save_failed vs none
09 shifts.post … note.description
10 shifts.post … shift_id missing in json
11 shifts/batch.post.description
12 shifts/batch.post.x-audit-event-success
13 shifts/batch.post.x-audit-event-failure
14 shifts/mine.get.description
15 punch.post.description
16 punch.post.x-terminal-allowed 长度 2 vs 1
17 punch.post.x-audit-event-failure punch_failed vs none
18 punch.post requestBody.required 长度 2 vs 3
19 punch.post method.description
20 punch.post method.enum[2] manual vs charging_disconnect
21 punch.post device_fingerprint.description
22 month-view.get.description
23 paths./api/v1/staff/shifts/{id}/history missing in json
24 paths./api/v1/staff/settings/shift-visible-days missing in json
25 paths./api/v1/staff/settings/payroll-penalty-cap missing in json
26 paths./api/v1/staff/self-registrations missing in json
27 paths./api/v1/staff/registration-companies missing in json
28 paths./api/v1/staff/profiles/{id}/employment-status missing in json
29 paths./api/v1/staff/locker-events missing in json
30 paths./api/v1/platform/merchants/{merchant_id}/self-operated missing in json
31 requestBodies.StaffRegistrationRequest.example.phone_hash missing in yaml
32 requestBodies.StaffRegistrationRequest.example.phone missing in json
33 schemas.StaffRegistrationRequest.description
34 schemas.StaffRegistrationRequest.required 长度 3 vs 4
35 schemas.StaffRegistrationRequest.properties.merchant_id.description
36 schemas…phone_hash missing in yaml
37 schemas…id_doc_hash missing in yaml
38 schemas…phone missing in json
39 schemas…id_doc missing in json
```

### CMD-JSONONLY（未发布 json + main yaml）EXIT 1

对称 39 处（json/yaml 对调）。门禁同样只列前 10。

### CMD-EX-COPY EXIT 0

```
OpenAPI P0 example verification passed: 8 request bodies and 3 responses checked.
```

不覆盖 10 个新 operation。

### CMD-MATRIX（未发布 json + main 矩阵）EXIT 1

```
docs/p0-openapi-operation-acceptance-matrix-v0.1.md is out of sync with docs/openapi/openapi-p0.json; run npm run docs:operation-matrix
```

### CMD-MATRIX-YAMLONLY（main json + 未发布 yaml + main 矩阵）EXIT 0

```
docs/p0-openapi-operation-acceptance-matrix-v0.1.md is in sync with docs/openapi/openapi-p0.json; 720 operations checked.
```

矩阵不读 YAML。这是 001 的 YAML 特有漏检窗。

### CMD-PARSE（node yaml 包，与门禁同解析器）

```
yOps=730 jOps=730 myOps=720 mjOps=720
yPaths=604 myPaths=596
added=10 removed=0 changed=8 same=712
yjPathDiffs=0 yjCompDiffs=0 yjInfoDiffs=0
pathOrderMismatch=604
yInfoVer=V0.14 mInfoVer=0.1.0
publicY=7 publicM=5
noneFailY=12 (write 8) noneFailM=0
schema.required=['idempotency_key','project_id','name','merchant_id']
example.merchant_id=null example.phone='13800138000'
self required=['idempotency_key','company_id','name'] 无设备字段
empIdemp=false visPostIdemp=false
boolishCount=0 merge=0
YAML-DEFAULT-OK；YAML-UNLIMITED-OK ops 604
```

### CMD-PYYAML（非门禁，对照）

```
unpublished: ComposerError found duplicate anchor 'a1'; first L826, second L7759
main/baseline: 同错（first ~L301, second ~L7234）
```

不折算为业务 finding（§8.3）。

未运行：Spring 测试、Flyway、会清理共享库的脚本、对 `complex_test*` 的任何写。未声称这些通过。

## 10. 结论

**FAIL**

本段 1 文件全结构覆盖；冻结 18 项 GEN 逐项核对；三线均执行；8 条工程缺陷成立（001 叠法/YAML 矩阵盲区、002 添加已答删除、003/004 冲突、005–008 技术性）；添加项 1 条已有操作员答复，不新开 2.2；无未答 questions；无「有条件通过」。发布前须与 U14 JSON、U16 附录、U17 矩阵、U13 安全基线同 PR，以 main `445a49f4` 为底，先按 002–008 改契约再生成 yaml，禁止整树覆盖。

## U15 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 51956 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02 §7/§11；criteria-lock V0.11；modules.md V0.04 附录「未发布材料第 15 段：完整接口定义」（1 文件 37017 行 OVERSIZE-SINGLE-FILE，main 主归属 M392；适用判据＝映射行并集 18 项 GEN-001–GEN-018）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`。
- 审计对象仍是只读输入副本 `.scratch/module-recheck-20260908/unpublished-03acdb98`（树 `e28dad059b2c7e08bcc95a8db18552d3f76edf4b`，基线 `242f487233d028173cb9198eecd079f5d877405c`），不是 main。三份 HTML 取 main 正本。未发布提交对象 `03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb` 本仓 `git cat-file -t` 仍不可解析。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B2 核实结论；原文与哈希本席本节点独立实算。
- 本席 B2-r1：**PASS**（001–009 逐 ID 全 PASS；添加 1 已答删除、不发新 2.2；发布前须修「是」=001–008，「否」=009）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。判据总数 401；M392 映射行 L6771 共 18 ID＝GEN-001–GEN-018，MISSING=[] EXTRA=[]。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要核对只读（输入哈希、本段 blob 与树 `e28dad05` 全等、main/基线 blob `161f4c1a…`、未发布 YAML SHA `93434ad6…`、main YAML SHA `ae9f6f0c…`、副本行 `:80`/`:119`/`:121`/`:210`/`:279-378`/`:294`/`:393`/`:395`/`:442-456`/`:497-558`/`:826`/`:7759`/`:27317`/`:33338-33342`/`:22635`/`:22743`/`:22853`/`:22891-22893`/`:36994`、HTML PGM L578/L579/L582、BSM L701/L935/L938/L1444、BSM L521、BEQR L233/L240、`questions/U04-001.md` 答复首行「删除」、已定稿 U14-001…008 / U04-001 / U05-003/005/006/011 / U01-001 / U02-002 / U07-004 / U10-003 / U13-001、package.json 既有 `verify:openapi`/`docs:openapi-examples`/`docs:operation-matrix`/`verify:operation-matrix`/`docs:openapi-closure`/`docs:security-baseline`、`tools/apply-openapi-p0-examples.mjs` 从 json 对象 `YAML.stringify` 同写两份、`generate-openapi-operation-matrix.mjs` 只读 json）；未重跑 overlay/生成器、未装依赖、未碰 `complex_test*`。追加稿仅经 `/tmp/u15-b2-append.md` 中转，落盘后删除。本票不把任何 U-FIX worktree 当作本段已发布修复。

此票只判断 findings.md V1.72 顶部「当前 U15 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、「发布前须修」是/否与最小处置。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。材料未发布，定稿后状态应记 REVIEWED-FAIL（有已定稿缺陷，处置写给发布者），不做 B3。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/U15-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 51956 | `86dfb24bb9ffa73df1e3ff8c30ddd9187f7cdb5e85d24a743e6d3f1316f82292` |
| `docs/audit-2026-09/modules/U15-b1-self-raw.md` | V0.01 | 11125 | `ddf6b4710acc5b1886f634c6ae604193195e372f9c46d1c93295e86cf4ecf55f` |
| `docs/audit-2026-09/findings.md` | V1.72 | 677642 | `99471869691b84d9234ffba276ffb294464d7084cb8d224ed9a1e098ca78f722` |
| `docs/audit-2026-09/ledger.md`（只读） | V2.87 | 523294 | `d7af1142bf33b46dd251ba02e3e01348dd0f5bb6e44eca58c7049e2908d22bb8` |
| `docs/audit-2026-09/questions/U04-001.md`（只读，沿用） | V0.01 | 2282 | `5a9e9dab904fe9b82ccb2e414ae07515ea7192c7ed404685c31828a126c2b3d1`；答复首行精确「删除」 |

候选形态：V1.72 顶部 U15-001…009；questions 0（引用 U04-001）；添加 1 已答删除；无 HTML 修改申请。U15 ledger 行 IN_PROGRESS。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

HTML 正本哈希：BEQR `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`、BSM `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa`、PGM `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c`。副本本段 1/1 sha256/行/字节与树 `e28dad05` blob 全等（本轮复算：副本 SHA `93434ad64c0c6cb18d98f9bc76399e80130d57ab66d477e13faffce53624cc9d`，`git hash-object` = `1ab4fd6009fdcaeddfb016d339678d860b470ca6` = `e28dad05:docs/openapi/openapi-p0.yaml`）。基线/main blob 同为 `161f4c1aa1609460a3fd80e1be7efeeeaebc175a`。`git diff --numstat 242f4872 e28dad05 -- docs/openapi/openapi-p0.yaml` = `712	180`。全树 `git diff --shortstat 242f4872 e28dad05` = 76 files, +4745/−1084。

### 技能（V0.17 §6 收窄，本节点）

| 技能 | 本节点用法 |
|---|---|
| using-arc / using-superpowers | 先核路由；用户指定 B2 归并覆盖 Arc audit；未改走 audit 工作流 |
| receiving-code-review | 只取其「先核原文再接受或反驳、禁止表演性同意、B2 不修复」；本体若派同会话子代理则成第四席，**未调用** |
| grilling | 只借追问结构自查：(a) 逐条同根登记是否与 GR-001…008 一致、是否重复入队、(b) 001 矩阵盲区并项与既有生成器、(c) 002–008 四分类是否与 U14/GR 逐条一致、(d) 009 是否写错 §8.2/3/18/20 |
| verification-before-completion | 声称前先跑 sha256/git rev-parse/git cat-file/hash-object/原文行号/HTML 锚点/package.json 脚本名与生成器读写面；本票不声称本轮新跑 overlay 或契约门禁 |
| systematic-debugging / diagnosing-bugs | 只借读错→对照数据流（本段 YAML → U14 JSON / U16 附录 / U17 矩阵 / U13 基线 → main 720 → HTML）；本轮不建 `/tmp` overlay |
| ponytail-review | 只评最小删除/收窄，不落地。001 只从定稿 json 用既有脚本同写 yaml；002–008 不在 yaml 上另做一遍业务修补 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |
| Arc audit | 会派子代理；与双席冲突；**未用** |

内部 grilling（自答，未问操作员）：

1. 本段与 U14 为同一契约两种序列化，候选逐条登记 U15-001…008 而不是一条「同根」，是否改写 GR-001…008 或造成 8 次独立修 yaml？否。见 (a)。
2. 矩阵只读 json、只叠 yaml 时 `--check` 仍绿，并入 001 叠法项、处置「禁止手改 yaml 独走」是否只引用既有生成器？是。见 (b)。
3. 002–008 四分类是否与 U14-002…008 及 GR-U15-002…008 逐条一致？是。见 (c)。
4. 009 是否写错 §8 第 2/3/18/20 条，或把应升队项降观察？否。见 (d)。

### 完整差集（GR-001…008、§8 观察 1–20、席甲 CX-001、OBS-1…2）

漏并：无。两席 B1 全部临时 ID / 观察仍落在 001–009。未把本席 finding 降观察。席甲独有项无超出 CX-U15-001 与 OBS-1…2。席甲 B1 以一条同根登记覆盖第 14 段四项，漏判 yaml 矩阵盲区（只叠 yaml 矩阵仍绿）以及 004/005/007；候选已按 U14 逐条拆分并接受本席八项须修，本席独立复核后维持。

| 临时 ID | 落入正式 ID | 是否改写本席原意 |
|---|---|---|
| GR-U15-001 | U15-001 | 否：技术性叠法；verify 39 处红 + **矩阵只读 json 的 yaml 漏检窗**；先改定稿 json 再生成 yaml；禁手改 yaml 独走；禁整树 |
| GR-U15-002 | U15-002 | 否：添加；已答删除；删 path/布尔；yaml 由 json 重生 |
| GR-U15-003 | U15-003 | 否：冲突；公开注册无设备证明 + 终端含 app；同根 U14-003/U05-003 |
| GR-U15-004 | U15-004 | 否：冲突；GET 可见天数把空上下限写成合同；同根 U14-004/U05-005 |
| GR-U15-005 | U15-005 | 否：技术性；柜机双身份 + 成功事件双值字面量；同根 U14-005/U05-006 |
| GR-U15-006 | U15-006 | 否：技术性；两写口幂等 false；同根 U14-006/U05-011 |
| GR-U15-007 | U15-007 | 否：技术性；schema required 含 merchant_id 而 example 给 null |
| GR-U15-008 | U15-008 | 否：技术性；8 个写口 failure 字面量 none |
| §8 1–20 | U15-009 | 「含」为闭集转述；第 2/3/18/20 条未把观察升冲突，也未把 finding 降观察（见 (d)；第 1 条括注漏写，非阻断） |
| 席甲 CX-U15-001 叠法句 | U15-001 | 子集（「随第 14 段修 JSON 后同步 YAML」）；并入 GR-001，补上席甲未跑的 yaml 矩阵盲区 |
| 席甲 CX-U15-001 自营写口 | U15-002 | 同根添加已答删除 |
| 席甲 CX-U15-001 注册/app | U15-003 | 子集（终端含 app）；并入 GR-003，分类维持冲突 |
| 席甲 CX-U15-001 幂等 false | U15-006 | 同根技术性幂等（候选行号改用 `:119`/`:393`，不采席甲 `:103`/`:377`） |
| 席甲 CX-U15-001 写口 none | U15-008 | 子集；并入 GR-008，候选行号改用 failure 字段行 |
| 席甲 OBS-1 | U15-009 | 引号/缩进重序列化语义不变；重复锚点 `&a1` 基线既有归 M392 |
| 席甲 OBS-2 | U15-009 | 同一契约、第 14 段为主——只作观察记录，**不得**读成取消 U15-001…008（见 (a) 与文末第 2 条） |

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/须修 | 本席票 |
|---|---|---|---|
| U15-001 | GR-U15-001 + 席甲 CX-U15-001（叠法句） | 技术性（发布叠法，同根 U14-001/U13-001）/是/是 | **PASS（技术性叠法/是/是；矩阵盲区并项成立；只引用既有生成器）** |
| U15-002 | GR-U15-002 + 席甲 CX-U15-001 | 添加（操作员已答删除）/是/是 | **PASS（添加已答删除/是/是；不新开 2.2）** |
| U15-003 | GR-U15-003 + 席甲 CX-U15-001 | 冲突（同根 U14-003/U05-003/U07-004/U01-001/U02-002）/是/是 | **PASS（冲突/是/是；与 GR-003 四分类一致）** |
| U15-004 | GR-U15-004（席甲漏） | 冲突（同根 U14-004/U05-005/U10-003）/是/是 | **PASS（冲突/是/是；与 GR-004 四分类一致）** |
| U15-005 | GR-U15-005（席甲漏） | 技术性（同根 U14-005/U05-006）/是/是 | **PASS（技术性/是/是）** |
| U15-006 | GR-U15-006 + 席甲 CX-U15-001 | 技术性（同根 U14-006/U05-011）/是/是 | **PASS（技术性/是/是）** |
| U15-007 | GR-U15-007（席甲漏） | 技术性（同根 U14-007）/是/是 | **PASS（技术性/是/是）** |
| U15-008 | GR-U15-008 + 席甲 CX-U15-001 | 技术性（同根 U14-008）/是/是 | **PASS（技术性/是/是）** |
| U15-009 | 双方观察归并 | 登记/否/否 | **PASS（登记/否/否；第 2/3/18/20 条未写错原意）** |

技术性入队 5（001 叠法/矩阵盲区、005 柜机契约、006 幂等、007 schema/example、008 失败事件）。添加入队 1（002，已答删除）。冲突入队 2（003 公开注册无设备证明、004 可见天数空上下限）。登记观察 1（009）。questions 0。无 HTML 修改申请。

发布前须修「是」=001–008（8）；「否」=009（1）。与候选汇总一致。席甲 B1 票 FAIL 但只立一条同根且漏 001 矩阵盲区/004/005/007，属低估；候选接受本席八项须修并勘误，本席独立复核后维持。

### 重点 (a) 逐条同根登记 U15-001…008，不是一条「同根」— **PASS（与 GR-001…008 一致；不是 8 次独立修 yaml）**

独立核，不采信席甲 OBS-2「避免重复入队」六个字为己证。

本席 B1 原文要求「须独立落临时 ID，不把 U14 正式 ID 写进本段 finding 格冒充已审」。GR-U15-001…008 各有独立四分类、独立 yaml 行号、独立工程缺陷，且均建议队列「是」、同根 U14-00N。把八条压成 CX-U15-001 一条，会丢掉：

- yaml 特有矩阵盲区（GR-001 / 本段对抗面，JSON 段没有「只叠 yaml 矩阵仍绿」）
- GET 可见天数空上下限合同（GR-004）
- locker 双身份与双值成功事件（GR-005）
- StaffRegistrationRequest schema/example 互斥（GR-007）

候选按 U14-001…008 逐条登记 U15-001…008，与 GR 一一对应。这是**同一缺陷在 yaml 序列化面上的同根登记**，不是第二套业务修补队列。

是否重复入队：否。八条处置都指向「json 定稿后由生成器同写 yaml」，yaml 面不另手改 002–008 的业务字段。B4 仍需要 U15-00N 绑定 yaml diff 行（生成后的 yaml 行仍须能归因）。若只留一条「同根 U14」，发布物 yaml 上的 004/005/007 行将无本段 ID 可绑。统一生成路径 ≠ 合并 ID。

席甲 CX-U15-001 只覆盖第 14 段 CX-U14-001…004（写口 none / 注册含 app / 自营写口 / 幂等 false），对应本批 008/003/002/006。候选拆分并补 001/004/005/007，与 GR 一致。OBS-2 留在 009 作观察即可，不得覆盖 001–008。票 **PASS**。

### 重点 (b) U15-001 把 yaml 矩阵盲区并入叠法项 — **PASS（成立；「禁止手改 yaml 独走」只引用既有生成器）**

独立核：

1. 本文件 main blob＝基线 blob `161f4c1a…`。B1 CMD-YAMLONLY：只把未发布 yaml 覆到 main json，`verify-openapi-contract.mjs` EXIT 1，39 处深不等（含 8 条新 path missing in json）。CMD-COPY（未发布 json+yaml 同树）EXIT 0。本轮不重跑，源码行与 B1 命令记录仍成立。
2. `tools/generate-openapi-operation-matrix.mjs` 源码第 3 行 `const jsonPath = "docs/openapi/openapi-p0.json"`，**只读 json**。B1 CMD-MATRIX-YAMLONLY EXIT 0（720 ops，声称 in sync）。这是 yaml 段相对 json 段的固定漏检窗：只合 yaml 时矩阵仍绿、JSON 客户端停在 720 口。GR-001 已把该窗写进 finding 正文，§8.4 写「并入 001，不第二开」。并入成立，不是把 003/004 的 HTML 冲突捆进「技术性」。
3. 反向只合 json 保留 main yaml：verify 同样 39 处红且矩阵红——那是 U14-001 的叠法面。本段 001 补的是 **yaml 独走时矩阵不红** 这一面。两条失败形态、**同一发布动作**：先按 U14-002…008 改定稿 json，再生成 yaml/矩阵/附录；U13 安全基线三件随后重生。禁止整树覆盖。

本轮核 `package.json` 已有，无新闸：

- `verify:openapi` = `node tools/verify-openapi-contract.mjs`（json↔yaml 深比较）
- `docs:openapi-examples` = `node tools/apply-openapi-p0-examples.mjs`：读 json，对对象 `YAML.stringify` 后 **同写 json 与 yaml**（L155–156）；`StaffRegistrationRequest` 不在其 example 表内，不会覆盖 U14-007 的 example 修正
- `docs:operation-matrix` / `verify:operation-matrix`（`--check`，只读 json——这正是盲区来源）
- `docs:openapi-closure` / `verify:openapi-closure`
- `docs:security-baseline`

候选「禁止手改 yaml 独走」+ 点名 `apply-openapi-p0-examples.mjs` 同写两份：该脚本是仓库里从 json 对象写出 yaml 的既有入口，不是新生成器、不发明第三条业务规则。矩阵/附录/基线随后重生只引用上列脚本。无新业务实体。票 **PASS**。

### 重点 (c) U15-002…008 分类与 U14-002…008 逐条一致 — **PASS（与 GR-U15-002…008 四分类一致）**

独立核，不采信候选「按 U14 逐条」六个字为己证。GEN-017 顺序：冲突 → 细化 → 技术性 → 添加。

| ID | GR-U15 四分类 | 候选 U15 | 定稿 U14 | 同根 |
|---|---|---|---|---|
| 002 | 添加（已答删除） | 添加（已答删除） | 添加（已答删除） | U04-001 |
| 003 | 冲突 | 冲突 | 冲突 | U05-003 / U07-004 / U01-001 |
| 004 | 冲突 | 冲突 | 冲突 | U05-005 / U10-003 |
| 005 | 技术性 | 技术性 | 技术性 | U05-006 |
| 006 | 技术性 | 技术性 | 技术性 | U05-011 |
| 007 | 技术性 | 技术性 | 技术性 | 契约自洽 |
| 008 | 技术性 | 技术性 | 技术性 | 审计事件名 |

本轮原文（副本 yaml，本席抽出，不采信席甲引文为己证）：

- `:497-558` `setMerchantSelfOperated` 独立布尔写口。HTML BSM L701 六类已含「自营」；PGM L578 标注落在六类码。`questions/U04-001.md` 答复首行「删除」→ **添加**，引用已答，不新开 2.2。与 U14-002 同分类。
- `:279-378` 两口 `x-auth-context: public` + `security: []`，终端含 `app`；requestBody 六字段、**无设备公钥/指纹**，required 甚至不含 phone。HTML PGM L579 / BSM L935「安装时生成并绑定设备标识」；BEQR L233 客人手机与员工私人手机分端。公开无指纹与绑定顺序相反 → **冲突优先**。与 U14-003 同分类。
- `:80`「未配置的上下限为空」。HTML BSM L1444「须同时配置上下限」；L521「未填写上下限的参数不得上线」。同文件 POST `:111` 已要求上下限和缘由。GET 把缺行写成合同 → **冲突优先**。与 U14-004 同分类。席甲漏判；候选立 004 成立。
- `:442-456`「员工取认证身份，柜机取设备身份」+ `staff-token` + `kiosk` + `staff.profile.self`；`:455` 成功事件双值字面量。调用形态/事件名，不发明寄存业务 → **技术性**。与 U14-005 同分类。
- `:119`、`:393` 两写口 `x-idempotency-required: false` → **技术性**。与 U14-006 同分类。
- schema `:33338-33342` required 含 `merchant_id`，example `:27317` 给 `null` → **技术性**。与 U14-007 同分类。
- 8 个写口 `:121/:210/:294/:395/:456/:22635/:22743/:22853` failure 字面量 none；main 0 → **技术性**。与 U14-008 同分类。

001/005/006/007/008 保持技术性：json/yaml/矩阵同步是闸口一致性；柜机双身份不改「扫码留痕、不手工登记」的上位需求；幂等键是重试协议；example 与 required 对齐是契约正确性；failure 字面量 none 是可观测性约定。无业务发明。

U15-003 同根多写 U02-002、未写 GR-003 已列的 U13-004：均是已定稿同缺陷的其它面，不改四分类。定稿宜补 U13-004，见文末第 3 条。票 **PASS**。

### 重点 (d) U15-009 观察是否写错本席 §8（第 2/3/18/20 条）— **PASS（未写错；无应升队被降观察项）**

独立核本席 §8 原文。用户点名四条：

**第 2 条。** 本席原文：「YAML path 键序与同树 JSON 全错位（新 path YAML 前置、JSON 追加）；verify 不计键序，深等 0。重生 YAML 时按 JSON 键序生成即可，不单独立项。」候选「2 yaml 与 json path 键序错位」是该句压缩。未升冲突、未写成须修。键序由 001 的生成器按 json 写出即消失。正确。

**第 3 条。** 本席原文：`&a1` 定义 2 次（L826、L7759）+ `*a1` 25 次，`git diff` 本文件零命中，基线/main 同样 2/25；官方 node `yaml` 包绿，PyYAML 基线同样红；官方门禁不经 PyYAML；收窄不修。本轮核副本 L826 / L7759 确为 `&a1`。候选「3 重复锚点基线既有」与席甲 OBS-1「归 M392」同向。未升 finding。正确。

**第 18 条。** 本席原文：`terminalAllowedRule` 写「D5 的 10 个终端码」；BEQR L233 是十一端；该句与 main 深等，本段 diff 未触及；归 M392 既有，不新开。本轮核副本 `:36994` 与 main `:36462` 同句。沿用 U01「main 既有」口径：本材料未加重 → 须修否、归 M392。候选「18 `terminalAllowedRule`「10 个终端码」main 既有归 M392」未写错。正确。

**第 20 条。** 本席原文：席甲开工笔记「YAML 不逐行通读」本席未采信；覆盖法＝新增/改写原文通读 + 712 未改 operation 与 main 深等 + 门禁全树 `$ref`。候选「20 席甲开工笔记「不逐行通读」Grok 未采信」未改成采信席甲覆盖法，也未把 OVERSIZE 抽样升成缺陷。正确。

**应升队而被降观察？无。** GR-001…008 全部入 001–008 且须修是。席甲漏判的 001 矩阵盲区/004/005/007 已入队。§8.4 矩阵盲区并入 001，§8.5 基线不钉 yaml SHA 并入 001/U13-001，§8.12 公开公司列表并入 003，§8.13 tags 随 002，§8.19 GET 四处 none 随 008。第 7 条上限描述未重复错误句仍留观察（与 U14-009 第 3 条同向，不宣称 U05-004 已消解）。

括注写「§8 1–20」却从第 2 条起列、漏第 1 条「712 个未改 operation 与 main 深等、英文 summary 归 M392」：闭集转述不完整，不改变第 2/3/18/20 条原意，也不把 712 深等升缺陷。定稿补回，见文末第 1 条。票 **PASS**。

### 001 独立裁定（技术性叠法／须修是）

见 (a)(b)。工程缺陷是。发布前须修是。最小处置不发明业务：以 main `445a49f4` 为底，与 U14 同一 PR，先按 U14-002…008 改定稿 json，再用既有 `docs:openapi-examples`（`apply-openapi-p0-examples.mjs`）从 json 同写 yaml；矩阵/附录用既有 `docs:operation-matrix` / `docs:openapi-closure`；U13 安全基线三件随后 `docs:security-baseline`。禁止手改 yaml 独走。禁止整树覆盖。票 **PASS**。

### 002 独立裁定（添加已答删除／须修是；同根 U14-002 / U04-001）

见 (c)。`:497-558` 独立布尔写口。答复首行「删除」，并写明 OpenAPI POST 随之去掉。删除该 path/operation 与 `self_operated` 布尔；yaml 由 json 重生。不新开 2.2。票 **PASS**。

### 003 独立裁定（冲突／须修是；同根 U14-003）

见 (c)。公开自助注册无设备公钥/指纹，终端含 `app`。处置「随 U14-003 由 json 重生」继承 U14-003 定稿：设备公钥/指纹必填（无指纹 4xx）、去掉 `app`、public 保留但加频控、公司列表若仍需匿名发现须另证来源校验且不能作为无绑定注册的前置。不在 yaml 上发明另一套注册规则。不新开 2.2。票 **PASS**。

### 004 独立裁定（冲突／须修是；同根 U14-004）

见 (c)。`:80` 把空上下限写成出厂完成。随 U14-004：读到出厂行（值 7 + 发布者写入的上下限）或缺行 fail-closed。席位不代填上下限数字。yaml 由 json 重生。票 **PASS**。

### 005 独立裁定（技术性／须修是；同根 U14-005）

`:448` 双身份 + `:455` 双值成功事件。随 U14-005 / U05-006 改单端：柜机以已登记 kiosk **设备签名**上报，员工身份由扫码载荷承载；成功事件改单一名或按 action 分两个 operation。候选写「随 U14-005」，U14-005 曾用「设备令牌」——本段读成「设备签名」，见文末第 5 条。票 **PASS**。

### 006 独立裁定（技术性／须修是；同根 U14-006）

`:119`、`:393` 两写口幂等 false。随 U14-006 改 true 并加 `idempotency_key`。幂等是重试协议。yaml 由 json 重生。票 **PASS**。

### 007 独立裁定（技术性／须修是；同根 U14-007）

schema required 含 `merchant_id`，example `:27317` 给 `null`。example 改非空占位；Java 注释在实现面随 U14-007。yaml 由 json 重生。票 **PASS**。

### 008 独立裁定（技术性／须修是；同根 U14-008）

8 个写口 failure 字面量 none（含打卡/排班从基线 `*_failed` 退化）。随 U14-008 恢复/新给 `*_failed`；不得用字面量 none；柜机失败名随 005。GET 四处 none 由发布者统一 GET 策略（§8.19），不另开 ID。yaml 由 json 重生。票 **PASS**。

### 009 独立裁定（登记／须修否）

见 (d)。双方观察归并，工程缺陷否，发布前须修否。重复锚点、键序、`terminalAllowedRule` 十码、席甲覆盖法不采信，均维持观察。`info.version` V0.14、712 个未改 operation 英文 summary 按 U01「main 既有」口径须修否、归 M392。票 **PASS**。

### 最小修正意见（非阻断，不改票）

1. U15-009 括注写「§8 1–20」却从第 2 条起列：定稿补回第 1 条「712 个未改 operation 与 main 深等，英文 summary 归 M392 既有、本段未加重」。分类/须修不变。
2. U15-009 席甲 OBS-2「以第 14 段 ID 为主」不得读成取消 U15-001…008。那是席甲 B1 一条同根的原建议；本批已拆分为八条同根登记，处置统一走 json→生成器。
3. U15-003 同根格补 U13-004（GR-003 有；U14-003 定稿有）。不改冲突分类。
4. U15-001 单元格补「以 main 445a49f4 为底」「与 U14+U16+U17 同一 PR」「脚本名 `docs:openapi-examples` / `docs:operation-matrix` / `docs:openapi-closure` / `docs:security-baseline`」。`apply-openapi-p0-examples.mjs` 是既有 json→yaml 同写入口，不是新闸。
5. U15-005「随 U14-005」读成 U05-006 / GR-005 的「kiosk **设备签名**」，不得读成另发设备令牌。
6. U15-003 压缩句「设备公钥/指纹必填、去 app、频控」定稿不得丢掉「私人手机通道 / 无指纹 4xx / 公司列表若仍需匿名发现须另证来源校验、不能作为无绑定注册的前置」。随 U14-003，不发明。
7. U15-001 可补 GR-001 的建议句：基线三件随后重生时同时钉 yaml SHA，或写明 `verify:openapi` 已覆盖 yaml。不另开 ID。
8. U15-004 同根格可补 U06-005（U14-004 定稿有）。不改冲突分类。
9. U15-008 本段最小是契约事件名随 json 重生；`recordFailure` 归第 5/6 段同 PR，不在 yaml 发明事件清单。
10. U15-009 第 14 条压缩「无 SQL/测试/commit 对象」可展开「无 UI；CMD-COPY 绿只证明未发布 json↔yaml」。不改登记。

全批差集无漏并。四分类与 GR-001…008 一致；席甲一条同根登记漏判 004/005/007 与 yaml 矩阵盲区，候选已勘误。001 叠法成立且只引用既有生成器；矩阵盲区并入 001 与 §8.4 一致。002–008 与 U14-002…008 逐条同分类。009 未写错第 2/3/18/20 条原意，无应升队被降观察项。逐 ID 001–009 全 **PASS**。本席 B2-r1 **PASS**。未经双 PASS 不得把候选当定稿，更不称 U15 或 main 模块 PASS。步骤仍 B2；U15 IN_PROGRESS；材料未发布，不做 B3。

本批最终：**PASS**
