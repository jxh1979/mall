CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M006 B5-r3 Grok 席全模块审计报告

版本：V0.01  
席位：Grok（审计席乙）  
步骤：B5（第 3 次 B3 之后的全模块复审）  
模块：M006 `10-backend-main-access-01`（6/322）  
工作区：`/home/james/complex-management` 主工作区 `main`  
ledger：`IN_PROGRESS`，B3=3  
after tree：`c77d4453db7eb9b92388de630fdd5121cdef7959`（14 个原有文件及 B3 合法新增 `AccessGrantPolicy.java`、`V386__access_grant_authorization_fingerprint.sql` 字节均与该 tree 一致）  
源码版本：`AccessService.java` 文件头 V0.13；`AuthorizationService.java` 文件头 V0.04  
findings 台账：V0.63，33 个已定稿 ID 当前修复队列均为「否」  
本席未读取 `M006-b5-r3-self-raw.md`、未读取 Codex 本轮报告、未以旧 B4/B5 票代替本次全审。

**本席结论：FAIL。**

不存在有条件通过。新意见 1 条（GR-01）为冲突且工程缺陷成立；审完整模块后一次性写出。期间未修复、未路由、未派第三席、未改三份业务 HTML、未改冻结 A 范围。

---

## 0. 技能预检（按意图执行，缺则记缺口）

本席在本环境实际核到并按收窄口径使用：

| 技能 | 核到路径 | 本次用法 |
|---|---|---|
| `using-superpowers` | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能自检 |
| `grilling` | `.../mattpocock-skills/1.2.3/./skills/productivity/grilling/SKILL.md` | 对抗/第一性原理追问结构；不派子代理、不等操作员 |
| `grill-me` | `.../mattpocock-skills/1.2.3/./skills/productivity/grill-me/SKILL.md` | 同上收窄口径 |
| `codebase-design` | `.../mattpocock-skills/1.2.3/./skills/engineering/codebase-design/SKILL.md` | 接口深度、seam、删除测试 |
| `diagnosing-bugs` | `.../mattpocock-skills/1.2.3/./skills/engineering/diagnosing-bugs/SKILL.md` | 对缺陷构造可失败回路（FieldLabels/HTTP 探针） |
| `systematic-debugging` | `.../superpowers/6.3.0/skills/systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流；不改生产代码 |
| `verification-before-completion` | `.../superpowers/6.3.0/skills/verification-before-completion/SKILL.md` | PASS/FAIL 前先跑命令 |
| `ponytail-review` | `.../ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 找可删/不可达 |
| `writing-for-agents` | `.../mattpocock-skills/1.2.3/./skills/productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席阅读 |
| knowledge-work `code-review` | `/home/james/.claude/plugins/cache/knowledge-work-plugins/engineering/1.2.0/skills/code-review/SKILL.md` | 安全/正确性维度，不另写产物 |

明确不用或缺口（不写成已调用）：

- `audit-context-building`、`semgrep`：会派子代理并另写产物，与双席及 §7 封闭清单冲突 → 存在但不适用。
- `dispatching-parallel-agents` / `subagent-driven-development`：禁止第三席。
- `batch-grill-me`、`ubiquitous-language`、`i-have-adhd`：本环境技能目录未核到独立 `SKILL.md` → **缺口**。
- `git-guardrails-claude-code`：核到；本席只读、无 git 写操作，未调用。
- `ponytail-audit` / `ponytail-debt` / `ponytail-gain`：提示词划掉。
- `research`：划掉；规范原文由本席直接读 HTML/锁。

---

## 1. 范围、哈希与台账事实

独立实算：

```text
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
```

sidecar `plan/criteria-lock.sha256` 与 `ledger.md` 顶部 `CRITERIA-LOCK-SHA256:` 同为该值。`LOCK-AMEND-COUNT: 0`。本席不把四值校验写成本模块 PASS。

`plan/modules.md` M006 原有 14 文件（未改冻结 A 范围）。B3 合法新增依赖保留在工作区并纳入阅读：`AccessGrantPolicy.java`（44 行）、`V386__access_grant_authorization_fingerprint.sql`（71 行）。未故意删除这些源后再声称构建失败。

questions：`questions/M006-027.md` 的 `## 操作员答复` 首个非空行为 `保留`。无待答添加项。三份业务 HTML 只读，未申请修改。

---

## 2. 第一性原理三问（模块级）

1. **为什么必须存在？** 删掉本模块，BSM-009/028「管理员工、岗位、职级、权限」、BSM-114「不同人只看自己该看的」、BSM-007/PGM 权限分岗、PGM-008 员工和手机入口都做不到。14 个文件分别承担主体、HTTP 入口、授权判定、凭证发放与只读汇总。
2. **声称的行为有没有被执行验证？** 有。本席在 `.scratch` 隔离编译当前源（`-d` 到 scratch，不写 `backend/target`），对 `complex_test` 只建连接级 TEMP 表；99 个 `@Test` 全绿，含 `companyRequestsHaveIndependentResultsAndRetryOnlyOnce`（M006-031）与 `invalidAccountTypeResponseIsChinese`（M006-032）。另跑 `FieldLabels.translate` + 真实 `GlobalExceptionHandler.handleApiException`。未跑边界见 §7。
3. **哪个前提未经验证就当真？** 「抛点英文键会在 HTTP 出口被译成中文」只覆盖 `field is required/invalid` 形态；一旦消息已含汉字，`FieldLabels.translate` 原样透传。本席用命令证明该前提对 `normalizeDecision` / `USB` / `CNY` 文案不成立。不得用「032 已修所以中文已净」代替本次全审。

---

## 3. 顺从式：14 文件逐项覆盖

| # | 文件 | 行数 | 覆盖要点 | 本席结果 |
|---|---|---:|---|---|
| 1 | `AccessActor.java` | 11 | 认证主体六字段；无业务发明 | 无新缺陷 |
| 2 | `AccessController.java` | 299 | 各写入口 `requirePermission` + 幂等键；临时授权控制器仍抛 `valid_until is required`，经出口层译为「有效期至未填写」 | 控制器英文键被翻译，不单独立项；删除入口仍存在但服务拒绝物理删 |
| 3 | `AccessCredentialProvisioner.java` | 12 | 凭证发放 seam；`noop()` 仅测试/缺 bean | 技术性，无缺陷 |
| 4 | `AccessGrantPositionSummary.java` | 10 | 岗位频繁申请汇总记录 | 无缺陷 |
| 5 | `AccessGrantRequestRecord.java` | 35 | 授权申请快照容器，`permissionCodes` 防御复制 | 对应 M006-019 登记项，无新缺陷 |
| 6 | `AccessGrantSummary.java` | 9 | 汇总壳 | 无缺陷 |
| 7 | `AccessGrantUserSummary.java` | 8 | 人员异常汇总；中文 summary 由服务写入 | 无缺陷 |
| 8 | `AccessRepository.java` | 259 | 仓储契约；默认实现 fail-closed；`pageGrantRequests` 走 `PageResponse.of` | 无新缺陷 |
| 9 | `AccessService.java` | 1946 | 账号/角色/岗位/模板/授权申请/政策/职级；V0.13 | **GR-01** |
| 10 | `AccessUser.java` | 37 | `phoneHash` `@JsonIgnore`；兼容构造器 `createdVia=legacy` | 对应 019/025 登记，无新缺陷 |
| 11 | `AuthorizationService.java` | 496 | DENY 优先、日常管理员桥、USB 主授权、客人储值白名单、幂等 `resolve()` 收窄 `p` | 031 行为已由测试钉住；无新缺陷 |
| 12 | `DevAccessCredentialProvisioner.java` | 48 | `local/dev` 同步到 `AuthService`；空密码拒绝；非 active 凭证 disabled | 无缺陷 |
| 13 | `EffectivePermissions.java` | 23 | 注释仍写 wildcard=true，实现始终 `wildcard=false`，日常权限折进 grants | 注释漂移，不构成可执行缺陷 |
| 14 | `JdbcAccessCredentialProvisioner.java` | 69 | prod BCrypt；状态白名单；空密码拒绝 | 无缺陷 |

B3 合法依赖（非冻结 14 文件，完整阅读）：

- `AccessGrantPolicy.java`：时间窗/阈值 BoundedInt + 四档风险审批规则；查询不能覆盖。
- `V386__...sql`：指纹列、政策 JSON、岗位软删除列、岗位派生授权列。JDBC 测试 `migrationAppliesAndGrantPolicyRoundTripsInPostgres` 通过。

---

## 4. 顺从式：52 条冻结判据

判定：满足 / 本模块无实现且无冲突 / **不满足（见 GR-01）**。

### GEN（18）

| ID | 结果 | 证据 |
|---|---|---|
| GEN-001 | 满足（残留不可达私有方法，不单独立项） | 账号/角色 ID 已用 UUID；`slug()` 私有且无调用点（AccessService:1898），属可删死代码，首次正常请求撞不上 |
| GEN-002 | **不满足** | 见 GR-01：错误路径的 HTTP 正文与「全中文报错」声称不一致 |
| GEN-003 | 满足（已跑子集） | 现存测试含能失败断言；本席实跑 99/0。未跑 2 个依赖 StaffService 的方法，见 §7 |
| GEN-004 | 满足 | 组织/商户在服务端校验；幂等键必填；账号类型/状态白名单 |
| GEN-005 | 满足 | DENY 优先；日常管理员不自动得高危；受益人/申请人自审拒绝；USB 主授权与职级 3 分开 |
| GEN-006 | 满足 | 单次授权先核验再消费；JDBC 测试证明消费失败回滚、指纹只写一次 |
| GEN-007 | 满足 | `resolve()` 对原共享 `p` 改为 `u:{len}:{id}:{org}` 再哈希；旧 `p` 记录冲突拒绝串用；同主体重试回放。测试 `companyRequestsHaveIndependentResultsAndRetryOnlyOnce` PASS |
| GEN-008 | 满足 | DTO `@JsonAlias`；PageResponse 元数据与条数一致（字典不再伪称 50） |
| GEN-009 | 满足（除 GR-01 文案） | 失败走 ApiException，不伪成功 |
| GEN-010 | 满足 | 手机哈希不回传；密码 BCrypt；未知主体不填 staff/active |
| GEN-011 | 满足 | 写路径 `recordSuccess/Failure` |
| GEN-012 | 满足 | 授权申请默认时间窗+每页 50；字典全量但 pageSize=实际条数 |
| GEN-013 | 满足 | 隔离 javac 当前源 73 文件成功；含 AccessGrantPolicy |
| GEN-014 | 满足 | V386 在 after tree 中；JDBC `migrationAppliesAndGrantPolicyRoundTripsInPostgres` PASS |
| GEN-015 | **不满足** | 与 PGM-066 同源，见 GR-01 |
| GEN-016 | 满足 | 本模块无独立用户文档声称 |
| GEN-017 | 满足 | 本席未把锁外主张写入队列 |
| GEN-018 | 满足 | 结论落到 HTML 原文或命令输出 |

### BEQR（6）

本模块是账号/权限，不是排班考勤薪资实现。映射要求是「不得冲突、遗漏或另行发明」。未见本模块把员工五态写成账号状态、也未见发明考勤/请假/薪资规则。员工五态与账号 `active/invited/disabled` 已在定稿 M006-024 区分为不同概念，队列否。

| ID | 结果 |
|---|---|
| BEQR-157 员工状态 | 本模块无实现且无冲突 |
| BEQR-182 考勤日结果 | 本模块无实现且无冲突 |
| BEQR-183 打卡方式 | 本模块无实现且无冲突 |
| BEQR-184 请假类型 | 本模块无实现且无冲突 |
| BEQR-185 请假单状态 | 本模块无实现且无冲突 |
| BEQR-186 薪资单构成 | 职级薪资只存分+CNY，不发明绩效档公式 |
| BEQR-187 员工提成发放 | 本模块无提现/现金现抽入口 |

### BSM（14）

| ID | 结果 | 证据 |
|---|---|---|
| BSM-001 | 满足（授权政策已可配） | `grantPolicy()` 读后台 JSON；查询覆盖阈值被拒绝。政策改写即生效、无草案/生效日；参数是统计窗/频繁阈值不是金额。未能证明属于「涉及钱的配置」，不另开 finding |
| BSM-003 | 满足 | 岗位模板存档/申请删除/最高权限软删除；`deletePositionRoleTemplate` 抛 `ARCHIVE_APPROVAL_REQUIRED` |
| BSM-009 | 满足 | 本模块承载员工账号与权限入口 |
| BSM-028 | 满足 | 员工/岗位/职级/权限/注册审批（手工员工建号被 `STAFF_REGISTRATION_REQUIRED` 拦住，外包除外） |
| BSM-029 | 满足 | 权限依赖组织/商户/职级 |
| BSM-073 | 满足 | 商户范围列表 `listUsersByMerchant`；跨商户写拒绝 |
| BSM-084 | 本模块无手机端 UI；无冲突 | 员工自助权限不在这 14 文件发放 |
| BSM-085 | 满足（注册第三入口已关） | `createAccessUser` 对非外包抛 `STAFF_REGISTRATION_REQUIRED` |
| BSM-086/087/088 | 本模块无实现且无冲突 | |
| BSM-114 | 满足 | 组织/商户/项目过滤；平台管理员与商户分列 |
| BSM-115 | 满足 | 基础权限卡住后续授权 |
| BSM-120 | 满足相关已拍板项 | 权限最小颗粒在权限字典；本模块不发明终端开关 |

### PGM（14）

| ID | 结果 | 证据 |
|---|---|---|
| PGM-001 | **不满足** | 「提示、报错必须全部为中文」。HTTP 实测仍漏出 approve/reject、USB，见 GR-01 |
| PGM-008 | 满足入口级 | 账号/凭证/职级在本模块；私人手机绑定在其他模块 |
| PGM-022/024 | 本模块无空间实现且无冲突 | |
| PGM-026 | 本模块不建商户档案；无冲突 | |
| PGM-032/033 | 注册两条路径由 StaffService 执行；本模块拒绝手工员工建号 | 未跑 StaffService 两测，见 §7 |
| PGM-034 | 本模块无企微实现且无冲突 | |
| PGM-035 | 本模块无提成发放实现且无冲突 | |
| PGM-036 | 本模块无设备领用实现且无冲突 | |
| PGM-048 | 满足 | 商户不能看他商户账号 |
| PGM-059 | 满足 | 建号不落手机号哈希、JSON Ignore |
| PGM-066 | **不满足** | 与 PGM-001 同条强制中文规则，见 GR-01 |

---

## 5. 对抗式（假定实现错误）

内部追问与反例（grilling 结构，事实自读）：

1. **平台管理员是否仍通配绕过 DENY？** `isPlatformAdminBridge` 只补日常低/中且 `requiresApproval=false` 的权限；DENY 仍先 `deny()`。测试 `administratorDenyWinsAndHighPermissionNeedsExplicitGrant`、`effectivePermissionsDoesNotGiveAdministratorAnUnboundedWildcard` PASS。
2. **无组织非管理员是否读全量？** `listAccessUsers` 走 `requireProjectScope`。测试 `missingOrganizationCannotListAllUsersOrResolveGlobalProject` PASS。
3. **跨组织同幂等键是否串用？** 无商户时 `legacyScope=p`，`resolve()` 改为带长度前缀的主体键再 SHA-256；`operation_id VARCHAR(128)`，`caller:`+64hex=71 字符。测试 `companyRequestsHaveIndependentResultsAndRetryOnlyOnce` PASS。旧共享 `p` 记录存在则 `IDEMPOTENCY_CONFLICT`「该请求编号存在旧处理记录，请先核实原操作结果」。
4. **受益人自审？** `reviewGrant` 拒绝 `targetUserId` 与 `requestedBy`。测试 PASS。
5. **岗位切换是否只追加？** `replacePositionGrants`；测试 `positionChangeDropsDerivedRoleButKeepsIndependentAssignment` PASS。
6. **非法账号类型是否仍夹英文枚举？** 源码现为「账号类型不允许，请重新选择」。HTTP `INVALID_ACCOUNT_TYPE` 无拉丁字母。测试 `invalidAccountTypeResponseIsChinese` PASS。**032 已修，但同出口形态的其他中英混排未修。**
7. **客人储值桥是否越权？** 仅 `customer.stored_value.topup/consume`，且须 `accountType=customer` 且 active。027 操作员答复保留。本席未跑客人钱包集成测试（§7）。
8. **最高权限是否被职级 3 冒充？** `SupervisorReviewService.approve` 对 `top_authority` 另查 `isMasterAuthority`。测试 `topAuthorityReviewRequiresMasterSessionRatherThanRankAlone` PASS。

ponytail-review（不单独作为 FAIL 项）：

- `AccessService.java:1898` `delete:` 未使用的 `slug()`。
- `EffectivePermissions.wildcard` 恒为 false：死字段。
- `listGrantRequests(Integer)` 仍 `page(全量)`；控制器走分页重载。

---

## 6. 新意见

### GR-01 — 授权审批等错误响应仍把英文枚举/缩写交给用户

- **临时编号：** GR-01
- **文件:行号：** `AccessService.java:1694`（主证）；同文件 `:515`、`:1061`、`:1491`（同类出口）
- **2.1 分类：** 冲突（判定顺序：先冲突）
- **工程缺陷：** 是
- **2.3 依据类型：** 1（HTML 逐字）+ 4（可复现命令与 HTTP 输出）
- **冻结判据：** PGM-001、PGM-066、GEN-002、GEN-015
- **HTML 原文（PGM-001 / 治理地图「界面全中文」；PGM-066 已拍板同文）：**
  > 展示给用户的一切文字，包括菜单、页面标题、字段名、状态、枚举取值、按钮、提示、报错……必须全部为中文，不得出现任何英文……可以不用中文的只有四样……单据号、设备编号、二维码内容、系统内部标识符。这四样之外，凡是展示给人看的文字一律中文，不得以“这是标识”为由保留英文。
- **行为：** `GlobalExceptionHandler` 对已含汉字的消息**原样透传**（`FieldLabels.translate` 命中 CJK 则 return message）。因此 `normalizeDecision` 在非法 `decision` 时把「通过（approve）或驳回（reject）」送到 HTTP `message`。这与已修的 032 同一出口、同一强制规则，只是换了一条文案。
- **不是 032 未修好：** 032 路径实测 `账号类型不允许，请重新选择`，无拉丁字母。本条是全模块复审新发现的同类残留。
- **命令与输出（节选，完整在 §7）：**

```text
HTTP_CODE=VALIDATION_FAILED MSG=审批结论必须为通过（approve）或驳回（reject） LATIN=true
HTTP_CODE=INVALID_ACCOUNT_TYPE MSG=账号类型不允许，请重新选择 LATIN=false
HTTP_CODE=MASTER_AUTHORITY_REQUIRED MSG=删除配置须由 USB 最高权限审批 LATIN=true
```

`valid_until is required` 被译成「有效期至未填写」（LATIN=false），说明出口层对纯 `field is required` 有效，挡不住「中文句子里夹英文枚举」。

- **影响（按具体行为，不写攻击词）：** 后台用户在授权申请复核传入无法识别的结论时，界面报错出现英文 `approve`/`reject`；岗位模板删除拒绝时出现英文 `USB`；非法薪资币种时出现 `CNY`/`USD`。与「报错必须全中文」直接矛盾。API 字段名 `approve`/`reject` 本身可作为内部标识留在请求体；**展示给人的 message 不能再列出英文取值。**

无其他新意见。旧 33 个 ID 本席不重开；031/032 行为已被本轮测试钉住为已修。

---

## 7. 测试命令、输出与未跑边界

### 7.1 环境

- 只 export 三个变量：`COMPLEX_DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/complex_test`，用户名来自 `/etc/complex/complex.env` 的同名键，库名强制改为 `complex_test`（未 `source` 整个 env）。
- 编译输出 `-d` 到 `.scratch/m006-b5-r3-grok-<pid>/classes`，不写 `backend/target`。
- JDBC 测试只用连接内 TEMP 表，不写共享业务库 `complex`。
- 为避开 StaffService 拖入 gate/terminal 整树，scratch 副本去掉 2 个 `@Test`（`employeeApprovalWithoutCredentialAdapterWritesNoAccount`、`registrationCreatesAccountOnlyAfterCompanyApproval`）。原测试文件未改。同文件仍保留并跑过 `manualEmployeeCreationCannotIssueCredentialsBeforeRegistrationApproval`。

### 7.2 翻译/HTTP 探针

`javac` 当前 Access/DTO/异常出口等源后执行 `TranslateProbe`。exit 0。完整输出：

```text
RAW=valid_until is required
TRN=有效期至未填写
HAS_LATIN=false
RAW=expires_at is required
TRN=到期时间未填写
HAS_LATIN=false
RAW=request_type is invalid
TRN=申请类型格式不正确
HAS_LATIN=false
RAW=status is invalid
TRN=状态格式不正确
HAS_LATIN=false
RAW=authentication is required
TRN=登录凭证未填写
HAS_LATIN=false
RAW=审批结论必须为通过（approve）或驳回（reject）
TRN=审批结论必须为通过（approve）或驳回（reject）
HAS_LATIN=true
RAW=删除配置须由 USB 最高权限审批
TRN=删除配置须由 USB 最高权限审批
HAS_LATIN=true
RAW=薪资币种当前仅支持人民币（CNY），实际为：USD
TRN=薪资币种当前仅支持人民币（CNY），实际为：USD
HAS_LATIN=true
RAW=账号类型不允许，请重新选择
TRN=账号类型不允许，请重新选择
HAS_LATIN=false
HTTP_CODE=VALIDATION_FAILED MSG=审批结论必须为通过（approve）或驳回（reject） LATIN=true
HTTP_CODE=INVALID_ACCOUNT_TYPE MSG=账号类型不允许，请重新选择 LATIN=false
HTTP_CODE=UNAUTHENTICATED MSG=登录凭证未填写 LATIN=false
HTTP_CODE=VALIDATION_FAILED MSG=有效期至未填写 LATIN=false
HTTP_CODE=MASTER_AUTHORITY_REQUIRED MSG=删除配置须由 USB 最高权限审批 LATIN=true
```

### 7.3 单元 + JDBC

`JAVAC_EXIT 0`，73 个源文件，scratch 内 98 个 class。随后反射执行 `@Test`：

```text
RESULT 99 PASS / 0 FAIL / 0 SKIP
JAVA_EXIT 0
```

关键条目：

- `M006SecurityRegressionTests#companyRequestsHaveIndependentResultsAndRetryOnlyOnce` PASS
- `M006SecurityRegressionTests#invalidAccountTypeResponseIsChinese` PASS
- `M006JdbcRegressionTests` 12 个方法全部 PASS（含指纹、分页、软删除、消费回滚、政策 round-trip）

99 条 PASS 名单已在本次命令 stdout 中逐行打印（本报告不重复 99 行）。失败数为 0。

### 7.4 明确未跑

- `M006SecurityRegressionTests` 中依赖 `StaffService` 的 2 个方法（注册审批闭环）。手工建号拒绝路径已跑。
- `AccessControllerTests`、前端 `M006Access*.test.tsx` / `WalletPage` 等。
- 全量 `mvn test` / 共享 `backend/target` 编译。
- 生产库 `complex`。
- 客人储值支付通道真实厂商（027 限定未配置则拒绝；本席未跑 `M006StoredValue*`）。
- 并发真实多线程 JDBC 锁（`lockGrantAuthorizationFacts` 源码为 `LOCK TABLE ... IN SHARE MODE`，行为由 JDBC 测试覆盖行锁/事务，不宣称压测）。

未跑不等于通过。

---

## 8. 结论

| 项 | 值 |
|---|---|
| 审计结论 | **FAIL** |
| 原因 | GR-01：用户可见报错仍含英文枚举/缩写，与 PGM-001/066 冲突 |
| 旧 33 ID | 本席不重开；031/032 本轮测试为已修 |
| 添加项 | 无新添加；027 已答复保留 |
| HTML 人闸 | 未触发 |
| 锁外判据 | 未主张 |

B3 已执行 3 次。本席只给审计结论 FAIL，不改 ledger、不修代码。
