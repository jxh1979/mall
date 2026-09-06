CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
版本：V0.01
# M006 B5-r4 Grok 全模块复审原始报告

席位：Grok（审计席乙）
步骤：B5（第 4 次 B3 之后的全模块复审，整模块重审，不只看 diff）
模块：M006 `10-backend-main-access-01`（plan/modules.md 第 74 行起，14 文件）
工作区：`/home/james/complex-management` 主工作区
ledger：`IN_PROGRESS`，B3=4
findings 台账：V0.67，M006-001…034 当前修复队列全部为「否」
本席未读取 `M006-b5-r4-self-raw.md` 或任何本轮席甲报告。历史 raw（b1 / b4-r1…r4 / b5-r2 / b5-r3）仅作测试命令形态参考，结论为本次独立审计。期间未修复、未改三份业务 HTML、未改锁 / ledger / findings。

**审计结论：PASS**

runbook §9 条件：14 文件均有覆盖记录；映射 52 条判据 ID 均已核对；三线均执行；声称均有 2.3 四类证据之一；无未答 questions；无未定稿缺陷；本报告第一行哈希有效。

---

## 0. 技能预检（按意图执行，缺则记缺口，不冒充已调用）

本席在本环境实际核到并按提示词 §6 收窄口径使用：

| 技能 | 核到路径 | 本次用法 |
|---|---|---|
| `using-superpowers` | `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能自检 |
| `grilling` | `.../mattpocock-skills/1.2.3/./skills/productivity/grilling/SKILL.md` | 对抗/第一性原理追问结构；不派子代理、不等操作员 |
| `grill-me` | `.../mattpocock-skills/1.2.3/./skills/productivity/grill-me/SKILL.md` | 同上收窄口径 |
| `codebase-design` | `.../mattpocock-skills/1.2.3/./skills/engineering/codebase-design/SKILL.md` | 接口深度、seam、删除测试 |
| `diagnosing-bugs` | `.../mattpocock-skills/1.2.3/./skills/engineering/diagnosing-bugs/SKILL.md` | 对缺陷构造可失败回路（FieldLabels/HTTP 探针） |
| `systematic-debugging` | `.../superpowers/6.3.0/skills/systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流；不改生产代码 |
| `verification-before-completion` | `.../superpowers/6.3.0/skills/verification-before-completion/SKILL.md` | PASS/FAIL 前先跑命令 |
| `ponytail-review` | `.../ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 找可删/不可达 |
| `writing-for-agents` | `.../mattpocock-skills/1.2.3/./skills/productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席阅读 |
| `receiving-code-review` | `.../superpowers/6.3.0/skills/receiving-code-review/SKILL.md` | 只借核实再接受或反驳；本席不修复 |
| knowledge-work `code-review` | `/home/james/.claude/plugins/cache/knowledge-work-plugins/engineering/1.2.0/skills/code-review/SKILL.md` | 安全/正确性维度，不另写产物 |
| `i-have-adhd` | `/home/james/.claude/plugins/cache/i-have-adhd/i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 核到存在；按提示词全程生效输出口径，本体 HITL 作废 |
| `git-guardrails-claude-code` | `.../mattpocock-skills/1.2.3/skills/misc/git-guardrails-claude-code/SKILL.md` | 核到；本席只读、无 git 写操作，未调用 |

明确不用或缺口（不写成已调用）：

- Arc `audit`：会派第三席并写 `docs/arc/audits/`，与双席及 §7 封闭清单冲突 → 存在但不适用。
- `dispatching-parallel-agents` / `subagent-driven-development`：禁止第三席。
- `batch-grill-me`、`ubiquitous-language`：本环境技能目录未核到独立 `SKILL.md` → **缺口**。
- `ponytail-audit` / `ponytail-debt` / `ponytail-gain`：提示词划掉。
- `research`：划掉；规范原文由本席直接读 HTML/锁。

---

## 1. 范围、哈希与台账事实

独立实算：

```text
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
```

sidecar `plan/criteria-lock.sha256` 与 `ledger.md` 顶部 `CRITERIA-LOCK-SHA256:` 同为该值。`LOCK-AMEND-COUNT: 0`。本席不把四值校验写成本模块 PASS（那是调度席 B5 后的闸 1）。

`plan/modules.md` M006 冻结 14 文件。B3 合法新增依赖纳入阅读但不扩冻结 A 范围：`AccessGrantPolicy.java`。`git ls-files -z -- <14 paths> | xargs -0 cat | wc -l` 实跑 **3262**（modules.md 记 2988 为切分时数字；B3 后源码变长，不构成越界切分缺陷）。

questions：`docs/audit-2026-09/questions/M006-027.md` 的 `## 操作员答复` 段首个非空行为 `保留`。无待答添加项。三份业务 HTML 只读，未申请修改。

---

## 2. 第一性原理三问（模块级）

1. **这段东西为什么必须存在？** 删掉本模块，BSM-009/028「管理员工、岗位、职级、权限」、BSM-114「不同人只看自己该看的」、PGM-008 员工和手机入口、PGM-001「再管资格：员工是否有权限」都做不到。14 个文件分别承担主体记录、HTTP 入口、授权判定、凭证发放与只读汇总。答得上 → 有业务上位需求。
2. **声称的行为有没有被实际执行验证？** 有。本席在 `.scratch/m006-b5-r4-grok-*` 隔离 `javac --release 21`（`-d` 到 scratch，不写 `backend/target`），只 export 三个 `COMPLEX_DATABASE_*` 且库名强制 `complex_test`。`TranslateProbe` 走真实 `FieldLabels.translate` + `GlobalExceptionHandler.handleApiException`。反射执行 `@Test`：**109 PASS / 0 FAIL / 2 SKIP**。关键钉：`invalidDecisionMessageIsChinese`、`invalidAccountTypeResponseIsChinese`、`companyRequestsHaveIndependentResultsAndRetryOnlyOnce`、JDBC 12 项全绿。未跑边界见 §7。
3. **哪个前提未经验证就当真？** 「抛点英文键会在 HTTP 出口被译成中文」只覆盖 `field is required/invalid` 形态，已用命令证明对 `valid_until`/`expires_at`/`authentication is required` 成立。对已含汉字的业务话术，`FieldLabels.translate` 原样透传——本轮 034 四处源码已改为纯中文（「U 盘」与 HTML 原文一致，见 §5）。不得用「上一轮 Grok 说已修」代替本次命令。

---

## 3. 顺从式：14 文件逐项覆盖

| # | 文件 | 行数 | 覆盖要点 | 本席结果 |
|---|---|---:|---|---|
| 1 | `AccessActor.java` | 11 | 认证主体六字段；无业务发明 | 无 finding |
| 2 | `AccessController.java` | 299 | 各写入口 `requirePermission` + 幂等键；临时授权控制器抛 `valid_until is required`，经出口层译为「有效期至未填写」；`DELETE` 入口仍在但服务拒绝物理删 | 无 finding |
| 3 | `AccessCredentialProvisioner.java` | 12 | 凭证发放 seam；`noop()` 仅测试/缺 bean | 技术性，无缺陷 |
| 4 | `AccessGrantPositionSummary.java` | 10 | 岗位频繁申请汇总记录 | 无 finding |
| 5 | `AccessGrantRequestRecord.java` | 35 | 授权申请快照容器，`permissionCodes` 防御复制 | 对应已出队 M006-019，无新缺陷 |
| 6 | `AccessGrantSummary.java` | 9 | 汇总壳 | 无 finding |
| 7 | `AccessGrantUserSummary.java` | 8 | 人员异常汇总；中文 summary 由服务写入 | 无 finding |
| 8 | `AccessRepository.java` | 259 | 仓储契约；默认实现 fail-closed；`pageGrantRequests` 走 `PageResponse.of` | 无 finding |
| 9 | `AccessService.java` | 1946 | 账号/角色/岗位/模板/授权申请/政策/职级；文件头仍 V0.13 | 无新缺陷；034 四处文案已中文 |
| 10 | `AccessUser.java` | 37 | `phoneHash` `@JsonIgnore`；兼容构造器 `createdVia=legacy` | 对应已出队 019/025，无新缺陷 |
| 11 | `AuthorizationService.java` | 496 | DENY 优先、日常管理员桥、USB 主授权、客人储值白名单、幂等 `resolve()` 收窄 `p` | 031 行为由测试钉住；无新缺陷 |
| 12 | `DevAccessCredentialProvisioner.java` | 48 | `local/dev` 同步到 `AuthService`；空密码拒绝；状态白名单 | 无 finding |
| 13 | `EffectivePermissions.java` | 23 | 注释仍写 wildcard=true，实现始终 `wildcard=false`，日常权限折进 grants | 注释漂移，不构成可执行缺陷 |
| 14 | `JdbcAccessCredentialProvisioner.java` | 69 | prod BCrypt；状态白名单；空密码拒绝；非 active 凭证 disabled | 无 finding |

B3 合法依赖（非冻结 14 文件，完整阅读）：

- `AccessGrantPolicy.java`：时间窗/阈值 BoundedInt + 四档风险审批规则；查询不能覆盖。构造失败文案为中文。

无 finding 的文件覆盖结论：上述 14 个文件均已整文件阅读；除已出队 ID 对应的已修行为外，本次未发现新的工程缺陷。

---

## 4. 顺从式：52 条冻结判据逐条核对

判定：满足 / 本 14 文件无该业务执行器 / 不满足。本次无「不满足」。

### GEN-001…GEN-018

| ID | 结果 | 证据 |
|---|---|---|
| GEN-001 | 满足 | 账号/角色 ID 用 UUID；私有 `slug()`（AccessService:1898）无调用点，属可删死代码，首次正常请求撞不上，不单独立项 |
| GEN-002 | 满足 | 入口/主路径/错误路径可执行；034 四处 HTTP 正文与声称中文一致（§7 探针） |
| GEN-003 | 满足（已跑子集） | 现存测试含能失败断言；本席实跑 109/0/2。未跑 2 个依赖 StaffService 的方法及 `@WebMvcTest` 控制器测试，见 §7 |
| GEN-004 | 满足 | 组织/商户在服务端校验；幂等键必填；账号类型/状态白名单；空密码拒绝 |
| GEN-005 | 满足 | DENY 优先；日常管理员不自动得高危；受益人/申请人自审拒绝；USB 主授权与职级 3 分开。测试 `administratorDenyWinsAndHighPermissionNeedsExplicitGrant` PASS |
| GEN-006 | 满足 | 单次授权先核验再消费；JDBC 测试证明消费失败回滚、指纹只写一次 |
| GEN-007 | 满足 | `resolve()` 对原共享 `p` 改为 `u:{len}:{id}:{org}` 再哈希。测试 `companyRequestsHaveIndependentResultsAndRetryOnlyOnce` PASS |
| GEN-008 | 满足 | PageResponse 元数据与条数一致（字典不再伪称 50）；错误码与 DTO 字段契约未在本轮改坏 |
| GEN-009 | 满足 | 失败走 ApiException，不伪成功；空密码/缺适配器显式失败 |
| GEN-010 | 满足 | 手机哈希不回传（`@JsonIgnore`）；密码 BCrypt；未知主体不填 staff/active |
| GEN-011 | 满足 | 写路径 `recordSuccess/Failure` |
| GEN-012 | 满足 | 授权申请默认时间窗+每页 50 且可到第 2 页（JDBC `grantPaginationKeepsScopeAndCanReachTheSecondPage` PASS）；字典全量但 pageSize=实际条数 |
| GEN-013 | 满足 | 本 14 文件无构建脚本；隔离 javac 当前 67 源 + 2 探针 exit 0 |
| GEN-014 | 本 14 文件无该业务执行器 | 无迁移文件在本模块清单；B3 合法新增 V386 由 JDBC `migrationAppliesAndGrantPolicyRoundTripsInPostgres` PASS 覆盖，不扩冻结范围 |
| GEN-015 | 满足 | 用户可见报错为中文；「U 盘」对齐 HTML 原文；权限码属系统内部标识符 |
| GEN-016 | 满足 | 本模块无独立用户文档把计划写成当前事实 |
| GEN-017 | 满足 | 本席未把锁外主张写入队列；027 已答复保留 |
| GEN-018 | 满足 | 结论落到 HTML 原文或命令输出 |

### BEQR（7）

本模块是账号/权限入口，不是排班考勤薪资单实现。映射要求「不得冲突、遗漏或另行发明」。未见本模块把员工五态写成账号状态，也未见发明考勤/请假/薪资公式。员工五态与账号 `active/invited/disabled` 已在定稿 M006-024 区分为不同概念，队列否。

| ID | 结果 |
|---|---|
| BEQR-157 员工状态 | 本 14 文件无该业务执行器（无待入职/在职/试岗/停岗/离职状态机）；账号三态不冒充五态，无冲突 |
| BEQR-182 考勤日结果 | 本 14 文件无该业务执行器 |
| BEQR-183 打卡方式 | 本 14 文件无该业务执行器 |
| BEQR-184 请假类型 | 本 14 文件无该业务执行器 |
| BEQR-185 请假单状态 | 本 14 文件无该业务执行器 |
| BEQR-186 薪资单构成 | 本 14 文件无该业务执行器（职级薪资只存分+人民币币种，不发明绩效档公式） |
| BEQR-187 员工提成发放 | 本 14 文件无该业务执行器（无提现/现金现抽入口） |

### BSM（14）

| ID | 结果 | 证据 |
|---|---|---|
| BSM-001 | 满足（授权政策已可配） | `grantPolicy()` 读后台 JSON；查询覆盖阈值被拒绝。政策改写即生效、无草案/生效日；参数是统计窗/频繁阈值不是金额。未能证明属于「涉及钱的配置」，不另开 finding |
| BSM-003 | 满足 | 岗位模板存档/申请删除/最高权限软删除；`deletePositionRoleTemplate` 抛 `ARCHIVE_APPROVAL_REQUIRED`。测试 `directPositionTemplateDeleteCannotBypassArchiveAndApproval` PASS |
| BSM-009 | 满足 | 本模块承载员工账号与权限入口 |
| BSM-028 | 满足 | 员工/岗位/职级/权限；手工员工建号被 `STAFF_REGISTRATION_REQUIRED` 拦住（外包除外） |
| BSM-029 | 满足 | 权限依赖组织/商户/职级 |
| BSM-073 | 满足 | 商户范围列表 `listUsersByMerchant`；跨商户写拒绝 |
| BSM-084 | 本 14 文件无该业务执行器 | 无手机端 UI；员工自助权限不在这 14 文件发放；无冲突 |
| BSM-085 | 满足（注册第三入口已关） | `createAccessUser` 对非外包抛 `STAFF_REGISTRATION_REQUIRED`。测试 `manualEmployeeCreationCannotIssueCredentialsBeforeRegistrationApproval` PASS |
| BSM-086 | 本 14 文件无该业务执行器 | 无排班/考勤/请假实现；无冲突 |
| BSM-087 | 本 14 文件无该业务执行器 | 无薪资单展示实现；无冲突 |
| BSM-088 | 本 14 文件无该业务执行器 | 无提成发放/现金现抽；无冲突 |
| BSM-114 | 满足 | 组织/商户/项目过滤；平台管理员与商户分列 |
| BSM-115 | 满足 | 基础权限卡住后续授权 |
| BSM-120 | 满足相关已拍板项 | 「员工权限按单一功能最小颗粒度」由权限字典承载；本模块不发明终端开关。职级 M1–M10 不在这 14 文件实现 |

### PGM（13）

| ID | 结果 | 证据 |
|---|---|---|
| PGM-001 | 满足 | 「提示、报错必须全部为中文」。034 四处 HTTP 实测无英文枚举；「U 盘密钥」与业务地图原文一致（见下） |
| PGM-008 | 满足入口级 | 账号/凭证/职级在本模块；私人手机绑定在其他模块。本 14 文件无私人手机设备绑定执行器 |
| PGM-022 | 本 14 文件无该业务执行器 | 无空间/点位实现；无冲突 |
| PGM-024 | 本 14 文件无该业务执行器 | 无低消/容量/库位实现；无冲突 |
| PGM-026 | 本 14 文件无该业务执行器 | 不建商户档案；无冲突 |
| PGM-032 | 满足相关部分 | 注册两条路径由 StaffService 执行；本模块拒绝手工员工建号。本 14 文件无私人手机绑定/换机执行器 |
| PGM-033 | 本 14 文件无该业务执行器 | 无私人手机设备标识/破解复核/寄存柜实现；注册第三入口已关，无冲突 |
| PGM-034 | 本 14 文件无该业务执行器 | 无企微会话实现；无冲突 |
| PGM-035 | 本 14 文件无该业务执行器 | 无提成发放实现；无冲突 |
| PGM-036 | 本 14 文件无该业务执行器 | 无设备领用实现；无冲突 |
| PGM-048 | 满足 | 商户不能看他商户账号。测试 `listAccessUsersFiltersToCurrentMerchantWhenMerchantScoped` PASS |
| PGM-059 | 满足 | 建号不落手机号哈希、JSON Ignore。测试 `manualAccountDoesNotPersistOrReturnPhoneDigest`、`accountJsonDoesNotExposeStoredPhoneDigest` PASS |
| PGM-066 | 满足 | 与 PGM-001 同条强制中文规则。权限码 `access.user.manage` 属「系统内部标识符」四样例外，不构成冲突 |

**PGM-001/066 与「U 盘」：** `[A-Za-z]` 探针把「删除配置须由最高权限（U 盘密钥）审批」标 `LATIN=true`。业务地图原文（`docs/business-system-map-for-operations-v0.1.html:576`）即写「持授权 U 盘密钥的最高权限本人发起的操作」。实现与 HTML 逐字对齐，不是 USB 英文缩写残留，不构成冲突，不回退 M006-034。

---

## 5. 对抗式（假定实现错误）

内部追问与反例（grilling 结构，事实自读，不等操作员）：

1. **平台管理员是否仍通配绕过 DENY？** `permissionGrant` 先扫 DENY 再走桥；日常管理员只补 `requiresApproval=false` 且 risk∈{low,medium} 的权限。测试 `administratorDenyWinsAndHighPermissionNeedsExplicitGrant`、`effectivePermissionsDoesNotGiveAdministratorAnUnboundedWildcard` PASS。
2. **无组织非管理员是否读全量？** `listAccessUsers` 走 `requireProjectScope`。测试 `missingOrganizationCannotListAllUsersOrResolveGlobalProject` PASS。
3. **跨组织同幂等键是否串用？** 无商户时 `legacyScope=p`，`resolve()` 改为带长度前缀的主体键。测试 `companyRequestsHaveIndependentResultsAndRetryOnlyOnce` PASS。旧 M006-031 不回退。
4. **受益人/申请人自审？** `reviewGrant` 拒绝 `targetUserId` 与 `requestedBy`。测试 `beneficiaryCannotApproveOwnGrant` PASS。旧 M006-007 不回退。
5. **岗位切换是否只追加？** `replacePositionGrants`；测试 `positionChangeDropsDerivedRoleButKeepsIndependentAssignment` PASS。旧 M006-009 不回退。
6. **非法账号类型是否仍夹英文枚举？** HTTP `INVALID_ACCOUNT_TYPE` 消息 `账号类型不允许，请重新选择`，无拉丁字母。测试 `invalidAccountTypeResponseIsChinese` PASS。旧 M006-032 不回退。
7. **034 四处是否仍夹英文？** 见 §7 探针：审批结论/币种 LATIN=false；最高权限文案对齐 HTML「U 盘密钥」。测试 `invalidDecisionMessageIsChinese` PASS。不回退 M006-034。
8. **客人储值桥是否越权？** 仅 `customer.stored_value.topup/consume`，且须 `accountType=customer` 且 active。027 操作员答复保留。本席未跑客人钱包集成测试（§7）。
9. **最高权限是否被职级 3 冒充？** 测试 `topAuthorityReviewRequiresMasterSessionRatherThanRankAlone` PASS。
10. **空权限/倒置时间窗是否仍能写成 active？** 测试 `emptyPermissionsAndInvertedWindowAreRejectedBeforeGrantWrite`、`pendingTemporaryGrantRejectsInvertedTimeWindow` PASS。旧 M006-013 不回退。
11. **手工员工建号是否仍直接发 active 凭证？** 非外包抛 `STAFF_REGISTRATION_REQUIRED`。测试 PASS。旧 M006-011 不回退。
12. **物理删除岗位模板？** 服务抛 `ARCHIVE_APPROVAL_REQUIRED`。测试 PASS。旧 M006-026 不回退。

ponytail-review（不单独作为 FAIL 项，首次正常请求撞不上）：

- `AccessService.java:1898` `delete:` 未使用的 `slug()`。
- `EffectivePermissions.wildcard` 恒为 false：死字段（行为正确：DENY 优先，权限折进 grants）。
- `listGrantRequests(Integer)` 仍 `page(全量)`；HTTP 控制器走分页重载。
- 文件头 `AccessService.java` 仍 V0.13（B3-r4 改了文案未递增版本头）；版本头非用户可见文案、非冻结判据。

---

## 6. Findings

本次无新 GR-xx。旧 M006-001…034 均已出队且本轮行为证据显示已修，不按回退统一规则标注任何旧 ID。

新问题不得直接要求修复：本席未登记候选缺陷。

分类备忘（不进队列）：

- 控制器/服务抛点 `valid_until is required` 等英文字段键：经 `FieldLabels.translate` 成为「有效期至未填写」。2.1 技术性，无业务发明，工程缺陷不成立。
- `没有操作权限：access.user.manage`：权限码为 PGM-066 允许的系统内部标识符。工程缺陷不成立。

---

## 7. 测试命令、输出与未跑边界

### 7.1 环境

- 只 export 三个变量：从 `/etc/complex/complex.env` 读取同名键，`COMPLEX_DATABASE_URL` 库名强制改为 `complex_test`（未 `source` 整个 env）。实跑 `DB_URL jdbc:postgresql://127.0.0.1:5432/complex_test`。
- 编译输出 `-d` 到 `.scratch/m006-b5-r4-grok-7hz_kvob/classes`，不写 `backend/target`。
- JDBC 测试只用连接内 TEMP 表，不写共享业务库 `complex`。
- 为避开 StaffService 拖入 gate/terminal 整树，反射运行器 `--skip` 两个 `@Test`（`employeeApprovalWithoutCredentialAdapterWritesNoAccount`、`registrationCreatesAccountOnlyAfterCompanyApproval`）。原测试文件未改。同文件仍保留并跑过 `manualEmployeeCreationCannotIssueCredentialsBeforeRegistrationApproval`。

### 7.2 翻译/HTTP 探针

`javac` 当前 Access/DTO/异常出口等源后执行 `TranslateProbe`。`PROBE_EXIT 0`。完整输出：

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
RAW=审批结论必须为通过或驳回
TRN=审批结论必须为通过或驳回
HAS_LATIN=false
RAW=删除配置须由最高权限（U 盘密钥）审批
TRN=删除配置须由最高权限（U 盘密钥）审批
HAS_LATIN=true
RAW=薪资币种当前仅支持人民币，不支持所填币种
TRN=薪资币种当前仅支持人民币，不支持所填币种
HAS_LATIN=false
RAW=账号类型不允许，请重新选择
TRN=账号类型不允许，请重新选择
HAS_LATIN=false
RAW=岗位模板须先存档，填写理由申请删除，经最高权限审批后软删除
TRN=岗位模板须先存档，填写理由申请删除，经最高权限审批后软删除
HAS_LATIN=false
RAW=频繁申请临时授权，建议评估配置固定权限
TRN=频繁申请临时授权，建议评估配置固定权限
HAS_LATIN=false
RAW=临时权限申请频繁，请核查异常情况
TRN=临时权限申请频繁，请核查异常情况
HAS_LATIN=false
RAW=无法识别的认证主体
TRN=无法识别的认证主体
HAS_LATIN=false
RAW=没有操作权限：access.user.manage
TRN=没有操作权限：access.user.manage
HAS_LATIN=true
HTTP_CODE=PARAM_INVALID DETAIL=VALIDATION_FAILED MSG=审批结论必须为通过或驳回 LATIN=false
HTTP_CODE=PARAM_INVALID DETAIL=INVALID_ACCOUNT_TYPE MSG=账号类型不允许，请重新选择 LATIN=false
HTTP_CODE=UNAUTHORIZED DETAIL=UNAUTHENTICATED MSG=登录凭证未填写 LATIN=false
HTTP_CODE=PARAM_INVALID DETAIL=VALIDATION_FAILED MSG=有效期至未填写 LATIN=false
HTTP_CODE=FORBIDDEN DETAIL=MASTER_AUTHORITY_REQUIRED MSG=删除配置须由最高权限（U 盘密钥）审批 LATIN=true
HTTP_CODE=PARAM_INVALID DETAIL=UNSUPPORTED_CURRENCY MSG=薪资币种当前仅支持人民币，不支持所填币种 LATIN=false
HTTP_CODE=FORBIDDEN DETAIL=PERMISSION_DENIED MSG=没有操作权限：access.user.manage LATIN=true
```

`JAVAC_EXIT 0`，67 个业务/测试源 + runner/probe。

### 7.3 单元 + JDBC

随后反射执行 `@Test`：

```text
RESULT 109 PASS / 0 FAIL / 2 SKIP
JAVA_EXIT 0
```

分项：

| 类 | 结果 |
|---|---|
| `M006SecurityRegressionTests` | 32 PASS / 0 FAIL / 2 SKIP |
| `AccessServiceTests` | 35 PASS / 0 FAIL / 0 SKIP |
| `AuthorizationServiceTests` | 21 PASS / 0 FAIL / 0 SKIP |
| `M006JdbcRegressionTests` | 12 PASS / 0 FAIL / 0 SKIP |
| `IdempotencyServiceTests` | 9 PASS / 0 FAIL / 0 SKIP |

关键条目：

- `M006SecurityRegressionTests#invalidDecisionMessageIsChinese` PASS（034）
- `M006SecurityRegressionTests#invalidAccountTypeResponseIsChinese` PASS（032）
- `M006SecurityRegressionTests#companyRequestsHaveIndependentResultsAndRetryOnlyOnce` PASS（031）
- `M006JdbcRegressionTests` 12 个方法全部 PASS（含指纹、分页、软删除、消费回滚、政策 round-trip）

### 7.4 明确未跑

- `M006SecurityRegressionTests` 中依赖 `StaffService` 的 2 个方法（注册审批闭环）。手工建号拒绝路径已跑。
- `AccessControllerTests`（`@WebMvcTest` + Spring Security 容器）。本席反射运行器对它报 `NotAMockException`（缺少 MockitoExtension/Spring 测试上下文），**不把运行器缺口折算为模块缺陷**。控制器鉴权与 `valid_until` 翻译已由源码阅读 + TranslateProbe + SecurityRegression 覆盖。
- 前端 `M006Access*.test.tsx` / `WalletPage` 等。
- 全量 `mvn test` / 共享 `backend/target` 编译。
- 生产库 `complex`。
- 客人储值支付通道真实厂商（027 限定未配置则拒绝；本席未跑钱包集成）。
- 并发真实多线程压测（JDBC 已覆盖行锁/事务，不宣称压测）。

未跑不等于通过。已跑证据足够支撑本席 PASS，不把未跑项写成已验证。

---

## 8. 旧 ID 回退核对

findings.md 当前 M006-001…034 修复队列全部为「否」。本席按回退统一规则：若旧 ID 未修好须标注该 ID。本次命令与源码核对：

| 旧 ID | 本轮证据 | 回退？ |
|---|---|---|
| 001 DENY/高危通配 | `administratorDenyWinsAndHighPermissionNeedsExplicitGrant` PASS | 否 |
| 002 跨商户 | `organizationReadWorksButCrossMerchantWriteDoesNot` PASS | 否 |
| 003 无组织全局 | `missingOrganizationCannotListAllUsersOrResolveGlobalProject` PASS | 否 |
| 004 slug 主键 | `collidingUserSlugsRemainDifferentUsers` / JDBC `createCollisionCannotOverwriteExistingUserOrRole` PASS | 否 |
| 005 改模板提权 | `templateEditCannotGrantAnUnownedHighPermission` PASS | 否 |
| 006 快照偷换 | `changedRoleCannotBeAppliedUnderAnOldApproval` PASS | 否 |
| 007 自审 | `beneficiaryCannotApproveOwnGrant` PASS | 否 |
| 009 岗位只追加 | `positionChangeDropsDerivedRoleButKeepsIndependentAssignment` PASS | 否 |
| 011 手工建号 | `manualEmployeeCreationCannotIssueCredentialsBeforeRegistrationApproval` PASS | 否 |
| 013 空权限/倒置窗 | `emptyPermissionsAndInvertedWindowAreRejectedBeforeGrantWrite` PASS | 否 |
| 014 单次消耗 | `twoPermissionReviewConsumesOneSourceOnlyAfterBothChecksPass` PASS | 否 |
| 015 英文汇总 | 源码中文「频繁申请临时授权…」 | 否 |
| 016 政策写死 | `summaryQueryCannotOverrideBusinessThresholds` PASS | 否 |
| 024/029 状态与空密码 | `invalidStatusAndBlankPasswordHaveNoWrites` PASS | 否 |
| 025 手机哈希 | `manualAccountDoesNotPersistOrReturnPhoneDigest` PASS | 否 |
| 026 物理删除 | `directPositionTemplateDeleteCannotBypassArchiveAndApproval` PASS | 否 |
| 028 未知主体 | `untrustedPrincipalIsRejected` PASS | 否 |
| 030 伪分页 | `referenceDictionaryDoesNotPretendItWasTruncatedAtFifty`、`grantListHasADefaultTimeWindow`、JDBC 第二页 PASS | 否 |
| 031 跨组织请求编号 | `companyRequestsHaveIndependentResultsAndRetryOnlyOnce` PASS | 否 |
| 032 账号类型英文 | HTTP 探针 + 测试 PASS | 否 |
| 034 审批/USB/CNY 英文 | HTTP 探针 + `invalidDecisionMessageIsChinese` PASS；「U 盘」对齐 HTML | 否 |

其余已出队 ID（008/010/012/017/018/019–023/027/033）源码路径仍在，无回归迹象，不重开。

---

## 9. 结论

| 项 | 值 |
|---|---|
| 审计结论 | **PASS** |
| 新 finding | 无 |
| 旧 34 ID | 不回退 |
| 添加项 | 无新添加；027 已答复保留 |
| HTML 人闸 | 未触发 |
| 锁外判据 | 未主张 |

B3 已执行 4 次。本席只给审计结论 PASS，不改 ledger、不写模块收口文件、不修代码。
