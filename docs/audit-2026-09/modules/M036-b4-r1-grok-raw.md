CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M036 B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 独立只读审计席乙（grok-4.6 / xhigh）
计划：160 审计 V0.17 B4 与 §5 闸 3
本文件路径：`docs/audit-2026-09/modules/M036-b4-r1-grok-raw.md`
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

模块：M036 / 共 471，B3 第 1 次之后的 B4。计划名：功能权限点、职级三十档、权限模板与最终权限计算／后台验证／第2段。
修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r1 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。席甲为作者自审票；本席 FAIL 具否决权，本轮不行使。§5 闸 3 越界判定同样须双 PASS。

未读取：任何 `M036-b4-r1-self-raw.md`（落盘时该路径已存在，本席按独立性禁令整文件不打开）。已读：`160-audit-prompt.md` V0.17 B4 与 §5 闸 3／§6、`findings.md` V3.12 顶部「当前 M036 B2-r1 双PASS定稿」、本席旧 `M036-b1-grok-raw.md` V0.01/V0.02（B1+B2，允许）、冻结 tree diff 与 D-029 发布变体、`JdbcAccessRepository.replacePositionGrants`／`findPermissionEffects`（origin/main 与两 tree 同文岗位 JOIN；主工作区 TEMPORARY_GRANT SQL 含未发布 `grant_group_id`，不在本 diff 内）、`AuthorizationService.isActive`、after 树三测试类、B3 冻结 `m036_b3r1_run.log`。ledger 只读取模块状态与 B3 次数；其中 after tree 之后写入的席甲 B4 封存行不构成本席证据。本票以两 tree 全文 diff、定稿最小处置、本席 B2-r1 边界、既有 RED/GREEN/变异日志＋本席独立重跑为准。

本轮实际审查口径：两个可复核 tree 之间的无过滤 binary 全文 diff；M036-001／002 是否按 findings V3.12 定稿最小处置与本席 B2-r1 边界实现；是否最懒可行（ponytail ultra）；是否引入新缺陷或新业务发明；每一变更行能否归因到归属「是」的 ID（仅 001、002）；新测试断言能否失败；可执行位与文件模式是否未变。只用既有冻结判据。未新造判据/角色，未询问操作员。对抗式按 grilling 追问结构自构反例，未派子代理。

---

## 0. 技能预检与节点应用（本环境实核）

本席按 `160-audit-prompt.md` V0.17 §6 在 **本机路径** 核验，不抄 Codex 预检表。grilling 只借追问树自问自答（事实自己读；技能本体「问人 / 派子代理查事实」作废）。Arc `audit` 本体要派评审子代理并写 `docs/arc/audits/`，与本轮「禁子代理、唯一可写 grok-raw」冲突，**不执行**。`requesting-code-review` 本体的 `HEAD~1` / 同会话子代理复审 / 无 ID 即刻修，一律不用——本席自身即 B4 外审。`receiving-code-review` 本节点不适用（本席产出意见，不收意见去修）。`test-driven-development` / mattpocock `tdd`：本轮只核 B3 已落的红绿／变异证据并本席重跑 GREEN，不新写测试。`resolving-merge-conflicts`：工作区无进行中 merge/rebase，节点不适用。`writing-for-agents`：本 raw 按完成准则写给下游合取席读。`git-guardrails-claude-code`：存在；本席未 push/reset/clean。`i-have-adhd`：存在；与「raw 第一行必须是独占锁行」冲突时，160 提示词优先。`brainstorming` / `writing-plans`：阶段 A 技能，B4 不适用。`ponytail`：B3 默认强度，B4 只审不修。`long-running-background-tasks`：本席 Maven 跑完即收，无后台作业遗留。`graft`：本环境存在，已用于定位 `replacePositionGrants`／`findPermissionEffects`／`isActive`。

| 技能 | 本环境路径 | 本节点用法 | 缺口 |
|---|---|---|---|
| `using-superpowers` | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工前技能清单；用户指令优先于技能本体 | 无 |
| `using-arc` | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 路由识别；**不执行** Arc 全量审计工作流 | 本体要派评审子代理，记缺口 |
| Arc `audit` | howells `skills/audit/SKILL.md` | 同上 | **不用**（第三席+§7 外产物） |
| `graft` | `~/.claude/skills/graft/SKILL.md` | `graft grep` 定位 `replacePositionGrants`／`findPermissionEffects`／`isActive` | 无 |
| `grilling` / `grill-me` | mattpocock `skills/productivity/grilling/SKILL.md`；`grill-me` 仅转发 | 对抗式自问自答；作废等用户/派子代理 | 无 |
| `batch-grill-me` | — | — | **本机技能树不存在**，记缺口，不冒充已调用 |
| `ubiquitous-language` | — | — | **不存在**，记缺口 |
| `codebase-design` | mattpocock `engineering/codebase-design/SKILL.md` | 内存仓 adapter 对齐 Jdbc 岗位 JOIN／撤旧应用 seam；本 diff 只改测试 adapter 与断言，不改被测模块 | 不派 DESIGN-IT-TWICE 子代理 |
| `diagnosing-bugs` | 同包 `engineering/diagnosing-bugs/SKILL.md` | 红环=只读命令（三工具 sha256、`cmp` 冻结 diff、读 RED/变异违例行、本席重跑 GREEN） | 三个变异未本席重放（会改被测代码）；以既有 `m036_b3r1_run.log` 行号与 after 树断言对齐 |
| `systematic-debugging` | superpowers | 只借读错误→追数据流（定稿声称→内存仓／Jdbc SQL→测试闸）；作废插桩 | 无 |
| `verification-before-completion` | superpowers | PASS 前本席实算哈希/`cmp`/D-029 正文比对/两处 Maven；不以「应能过」代替 | 变异 3 红以既有日志为证 |
| `requesting-code-review` | superpowers | B4=本席即外审；基线=两 tree id 而非 `HEAD~1`；不派子代理 | 本体子代理路径作废 |
| `ponytail-review` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只找可删的越界添加 | 不落地 |
| `tdd` / `test-driven-development` | 存在 | 核 001／002 RED（三变异）→ GREEN（发布口径 54+22+13）；不改被测即绿是定稿要求 | 无 |
| `long-running-background-tasks` | grok bundled | Maven 同步跑完 | 节点不适用遗留 |
| `git-guardrails-claude-code` | mattpocock misc | 只读；未装 hook | **存在但未安装** |
| Trail of Bits `audit-context-building` / `differential-review` | 存在 | 本体强制子代理 | **不用** |
| `resolving-merge-conflicts` | 存在 | 工作区无 merge/rebase | 节点不适用 |
| `writing-for-agents` | 存在 | 本 raw 给下游合取席 | 无 |
| `ponytail` | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail/SKILL.md` | B4 只审：测试夹具最小对齐 + 一条新测，ultra 已满足 | 不修 |
| `i-have-adhd` | `~/.claude/plugins/cache/i-have-adhd/i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 结论先行、编号；与独占锁行冲突时 160 优先 | 无 |

明确不用（提示词已划掉，本席亦不调用）：`dispatching-parallel-agents`、`subagent-driven-development`、`executing-plans`、`using-git-worktrees`（B3/B4/B5 主工作区就地；本席为发布口径独立 GREEN 建的 origin/main 一次性 worktree 已 `git worktree remove --force` 删除）、`research`、`code-review`（mattpocock）、`domain-modeling`、`ponytail-audit`。未使用 MCP。未建脚本/持久日志/临时审计产物（`/tmp/m036_b4_r1_verify.diff` 在 `cmp` 后已删；一次性 worktree `/tmp/m036_b4_r1_pubwt` 已删除）。

graft 本席调用节省合计约 166663 tokens（`replacePositionGrants` / `findPermissionEffects` / `isActive`）。

---

## 1. 输入与独立锁实算

本席哈希命令（独立实算，未抄 sidecar / ledger / 派发期望值）：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
python3 -c "import hashlib; print(hashlib.sha256(open('docs/audit-2026-09/plan/criteria-lock.md','rb').read()).hexdigest())"
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具同值，与本文件第一行独占锁行一致。文件 889020 字节。sidecar 一行同值（对照，非抄写来源）。ledger 顶部基准同值（对照）。criteria-lock 版本仍 V0.11。LOCK-AMEND-COUNT 仍为 1。

| 项 | 值 |
|---|---|
| 模块 | M036 后台验证／第2段（计划 3 文件；本轮 B3 写入集合＝三测试类，不改被测、不加迁移） |
| 阶段 | B4-r1 只读审计 B3 第1次整批修复的「修复内容」本身 |
| 本轮修复队列 | findings.md V3.12：M036-001 细化／是／是；002 技术性／是／是；修复队列「否」0 |
| B3 | 已执行 1 次；模块仍 IN_PROGRESS |
| before（对象类型 tree） | `b637f432404d6f402eede21dc4af52a6265a87fa` |
| after（对象类型 tree） | `0b38ca49734ed5f50c724214285b7119ecf0c809` |
| `git cat-file -t` | 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary b637f432404d6f402eede21dc4af52a6265a87fa 0b38ca49734ed5f50c724214285b7119ecf0c809`（无路径过滤） |
| 实际差异 | 11784 bytes；SHA256 `1e3ca09807990735c264be0b4c7ee5da9c41671ee4bb9da4a984637504de6427`；3 files changed, 79 insertions(+), 10 deletions(-)；numstat 34/8 + 21/1 + 24/1；UTF-8 |
| 冻结 diff | `/home/james/audit-tmp/m036_b3r1.diff` 11784 bytes，同 SHA；本席 `git diff --binary` 写入 `/tmp/m036_b4_r1_verify.diff` 后 `cmp` **IDENTICAL**，随后删除 |
| 正本 | `/home/james/complex-management` main；hostname=`y9000p-ai` |
| findings.md | V3.12；本席 `sha256sum` = `8a99367d45f1251a0b6e2fd7a864df5e3a745bf4e3a65ca5ba0597433daf676e` |
| 160-audit-prompt.md V0.17 | 本席 `sha256sum` = `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`（路径 `.scratch/module-recheck-20260908/160-audit-prompt.md`） |
| 本席 B1+B2 raw | `M036-b1-grok-raw.md` 56186 bytes，SHA256 `0c4227794df08be9c1cc3d6a67847f2361af6e98d862a9776818d30e1566c043`（与定稿段所引一致；本席独立实算） |
| 三 HTML（对照，不在 diff） | BEQR `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`；BSM `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`；PGM `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |
| B3 运行日志 | `/home/james/audit-tmp/m036_b3r1_run.log` 5812 bytes，SHA256 `9bef514546a69231283c658357d69e20d00bb739f12479338a0e7ba0c02bfc54` |
| D-029 发布变体 | `/home/james/audit-tmp/m036_AuthorizationServiceTests.publish.java` SHA256 `3b3b6c2b2b6c042d92bb0c8dd2721d80def1ace85417197fd907f5d570f6671a`；其与 origin/main 的差异 `/home/james/audit-tmp/m036_b3r1_publish_authz.diff` 1632 bytes SHA256 `2383da84389745bc582e0b755a18b2bcf77f2a70ce348489faa6421bcd44830d` |

工作区三文件 blob 与 after 树全等（`hash-object` = `git ls-tree after`）：AccessServiceTests `ebdf8cae…`、AuthorizationServiceTests `f3e040d0…`、M006JdbcRegressionTests `ece6ff7e…`。可执行位未变：diff 头均为 `100644`→`100644`；工作区 `stat` 664（rw-rw-r--）。无 merge/rebase。三文件 `git status` 为已修改未提交，符合 B3 未提交落盘。

被测生产文件两 tree 同 blob（不在 diff 内）：`AccessService.java` `de7e6c4f…`、`JdbcAccessRepository.java` `837b04aa…`、`AuthorizationService.java` `4bfae6d2…`。无迁移路径进入 `--name-only`。

---

## 2. diff 逐块归因表（§5 闸 3）

闸 3：每一行必须归因到 findings.md V3.12 已定稿且归属「是」的 ID。本批仅 001、002。归因不到即越界 → 整轮 FAIL。

`git diff --name-only --binary <before> <after>` 恰好 3 路径，无第四文件。

| 块 | 路径 | 模式 | ± | 归属 | 理由 |
|---|---|---|---|---|---|
| A1 | `AccessServiceTests.java` 文件头版本 | 100644 保持 | +1 / −1 | **M036-001** | 该文件全部实质改动属 001；版本 V0.08→V0.09（加一） |
| A2 | 同文件 `savingAPositionMapping…` 角色夹具 | 100644 保持 | +7 / −4 | **M036-001** | 旧角色／新角色改用互不相同的码（`ops.production_readiness.view`／`governance.supervisor_review.view`）；模板仍 `ops.production_readiness.check` |
| A3 | 同测试 定岗后有效集 | 100644 保持 | +2 / −0 | **M036-001** | `effectivePermissionsForUser` 断言含旧角色码与模板码 |
| A4 | 同测试 改映射后有效集与模板应用状态 | 100644 保持 | +5 / −0 | **M036-001** | 含新角色码＋模板码、不含旧角色码；应用状态 `revoked` 然后 `active`（撤上一次岗位派生再记新应用） |
| A5 | 同测试 停用后 | 100644 保持 | +5 / −0 | **M036-001** | 三者都不含；应用状态 `containsOnly("revoked")` |
| A6 | 同文件内存仓 `replacePositionGrants` | 100644 保持 | +7 / −1 | **M036-001** | 新增 `positionApplicationIds`；先 `revokeTemplateApplications` 上次岗位应用 id，再记新角色与新应用 id |
| A7 | 同文件内存仓 `applyTemplate` | 100644 保持 | +1 / −1 | **M036-001** | 应用 id 由 `"app_"+userId` 改为 `"app_"+userId+"_"+(size+1)`，同一用户两次套模板不碰撞 |
| A8 | 同文件内存仓 `findPermissionEffects` | 100644 保持 | +6 / −1 | **M036-001** | 把 `positionRoles` 非空启用角色并入 ROLE 来源授予 |
| B1 | `AuthorizationServiceTests.java` 文件头版本 | 100644 保持 | +1 / −1 | **M036-002** | 主工作区未发布版 V0.03→V0.04（加一）；发布变体另为 V0.01→V0.02，见 §3.2 |
| B2 | 同文件新测试 `grantWhoseValidityHasNotStartedDoesNotAuthorize` | 100644 保持 | +20 / −0 | **M036-002** | 插在 `effectivePermissionsExcludesExpiredGrant` 之后、私有助手之前；`validFrom=2099-01-01`（未来），未复用 `temporaryGrant` 助手 |
| C1 | `M006JdbcRegressionTests.java` 文件头版本 | 100644 保持 | +1 / −1 | **M036-001** | V0.13→V0.14（加一） |
| C2 | 同文件新测试 `positionDerivedRoleFeedsPermissionEffectsUntilThePositionGrantsAreCleared` | 100644 保持 | +23 / −0 | **M036-001** | 只建 TEMP 表／补 TEMP 列；`findPermissionEffects` 返回岗位 ROLE 授予；`replacePositionGrants(user,null,null,[],…)` 后为空 |

行数核对：Access 34/8 + Authz 21/1 + M006 24/1 = 79 insertions、10 deletions。与 `--stat`／`--numstat` 一致。无剩余行。

**未出现、故不构成越界的路径（本席主动核「有没有多出来」）**：`AccessService.java`、`JdbcAccessRepository.java`、`AuthorizationService.java`、任何 `db/migration/V*.sql`、三份需求 HTML、lock／sidecar／ledger／findings、前端、OpenAPI、`AccessJdbcIntegrationTests`、其它测试类。缺席不是越界；定稿禁止改被测与加迁移。

**归因票：PASS。** 79/79 插入行、10/10 删除行均可归到 001／002；无无 ID 行。

---

## 3. 内容审计

### 3.1 M036-001（细化／是／是）

定稿最小处置（findings V3.12 + 本席 B2-r1 边界）：① 内存仓按 Jdbc 语义对齐——`applyTemplate` 应用 id 不重复；`replacePositionGrants` 先撤上一次岗位派生的模板应用再记新角色与新应用 id；`findPermissionEffects` 把岗位派生角色并为 ROLE 来源授予。② `savingAPositionMapping…` 改用互不相同的码，经 `effectivePermissionsForUser` 断言定岗后／改映射后／停用后的有效集与模板应用状态。③ `M006JdbcRegressionTests` 补一条只建临时表的真库测试，守住岗位 JOIN 与置空收回。不改被测、不加迁移。

**① 内存仓**

- `applyTemplate`：`"app_" + userId + "_" + (templateApplications.size() + 1)`。同用户两次套用得到 `_1`／`_2`，不再共用 `app_<userId>`。B2 允许「UUID 或等价唯一键」；`size()+1` 在本夹具只追加不删除的 ArrayList 上等价唯一。本文件无其它断言依赖 `app_` 前缀。
- `replacePositionGrants`：先 `revokeTemplateApplications(userId, positionApplicationIds 上次列表)`，再 `positionRoles.put`、`positionApplicationIds.put(copyOf(applicationIds))`。与 origin/main `JdbcAccessRepository.replacePositionGrants`（先把 `position_application_ids` 指向的 active 应用置 revoked，再 UPDATE `assigned_role_id`／`position_application_ids`）同向。独立模板应用不在该列表，不受影响。
- `findPermissionEffects`：`LinkedHashSet` 并入 `positionRoles.get(userId)` 非空值，沿用既有 `role==null || !"active"` 跳过与 `Source.ROLE` 构造。与 origin/main 岗位查询 `user_position_assignments ⋈ roles ⋈ role_permissions`、`'ROLE' AS source` 同向。

**② 断言三态**

旧角色 `ops.production_readiness.view`、新角色 `governance.supervisor_review.view`、模板 `ops.production_readiness.check`，三码互异；`setUp` 里这三码本就 `addPermission`，本 diff 未改 setUp。定岗后 `contains` 旧角色码＋模板码；改映射后 `contains` 新角色码＋模板码且 `doesNotContain` 旧角色码，应用状态 `containsExactly("revoked","active")`（插入序＝先旧后新）；停用后三者 `doesNotContain`，应用 `containsOnly("revoked")`。指针断言（`positionRoles`／薪资 500000）保留。

**③ 真库测试**

`openTemporaryTables` 默认 TEMP `permission_templates` 只有 `id/single_use`。新测为 `findPermissionEffects` 实际 JOIN 自建 TEMP：`user_roles`／`roles`／`role_permissions`／`user_position_assignments`／`permission_template_items`／`user_permission_overrides`（`LIKE public … INCLUDING ALL`），并 ALTER 已有 TEMP 的 `permission_templates`／`permission_template_applications`／`temporary_permission_grants` 补列。INSERT 只打 TEMP 名；类注释写明只用 `complex_test` 连接内临时表。`containsExactly(tuple("position.only.code", ROLE, GRANT))` 后 `replacePositionGrants("user", null, null, List.of(), …)`，再 `isEmpty()`。origin/main 该 SQL 用 `id::text` 作临时授权 `source_id`（无 `grant_group_id`），与 TEMP 列相容。

**未越界**：无被测 Java、无迁移、无 AccessJdbcIntegrationTests、无新权限码／状态机。

### 3.2 M036-002（技术性／是／是）

定稿：AuthorizationServiceTests 补一条——生效时间在 2099 年的授予被拒且不进有效集；插在 `effectivePermissionsExcludesExpiredGrant` 之后、私有助手之前；不得复用 2099 为截止时间的 `temporaryGrant` 助手；不改被测；两版打同一处补丁，发布＝origin/main＋补丁（D-029）。

主工作区 after 树：新测试 `grantWhoseValidityHasNotStartedDoesNotAuthorize` 紧接 `effectivePermissionsExcludesExpiredGrant` 的闭合 `}`，下一符号为 `private AccessRepository.PermissionEffect effect(`。`PermissionEffect` 参数序为 `validFrom` 然后 `validUntil`：`2099-01-01T00:00:00Z`／`2099-01-02T00:00:00Z`。`temporaryGrant(...)` 助手把 `validFrom` 写死 `2020-01-01`、参数当 `validUntil`——本测试未调用它。`assertPermissionDenied` → `requirePermission` 期望 `PERMISSION_DENIED`；再 `effectivePermissions().grants().doesNotContain("ops.production_readiness.manage")`。`setUp` 已 `addKnownPermission` 该码。`isActive`（两 tree 同文 L524）：`checkedAt.isBefore(validFrom)` → false。不改 `AuthorizationService`／Jdbc SQL。

**D-029**：插入测试正文 915 字节，主工作区版与发布变体 **逐字相同**。只差版本号行：主工作区 V0.03→V0.04（本 tree diff）；发布变体 V0.01→V0.02。origin/main 文件无该测试。将冻结 `m036_b3r1_publish_authz.diff` 去掉路径后缀 `(origin/main)`／`(发布变体)` 后 `git apply`，得到文件 SHA256 `3b3b6c2b…`，与 `m036_AuthorizationServiceTests.publish.java` **IDENTICAL**。AccessServiceTests／M006JdbcRegressionTests 修后主工作区 SHA256（`be3e9be4…`／`b16e34e2…`）与发布口径拷贝逐字节相同（before 树这两文件 blob 已等于 origin/main）。

### 3.3 新断言能否失败

冻结日志 `/home/james/audit-tmp/m036_b3r1_run.log`（发布口径，origin/main 一次性 scratch，HEAD `3ca1b507`）：

| 阶段 | 结果 |
|---|---|
| BASE 未打补丁 | AccessServiceTests 54、AuthorizationServiceTests 21、M006JdbcRegressionTests 12，全绿 |
| 补丁＋`assign_passes_no_app_ids`（定岗把空列表传给 `replacePositionGrants`） | AccessServiceTests **1 failure**＝`savingAPositionMapping…`（期望 `[revoked, active]` 实得 `[active, active]`）；另两类 22／13 绿 |
| 补丁＋`no_position_role_join`（岗位 JOIN 恒空） | M006JdbcRegressionTests **1 failure**＝新真库测试（期望 ROLE 授予，实得 `[]`）；另两类 54／22 绿 |
| 补丁＋`ignore_valid_from`（`isActive` 去掉生效时间判断） | AuthorizationServiceTests **1 failure**＝新授权测试（`Expecting code to raise a throwable`）；另两类 54／13 绿 |
| 补丁、无变异 GREEN | 54＋22＋13 全绿 |

本席独立重跑：

1. 主工作区（in-memory，无 DB）：`AccessServiceTests#savingAPositionMappingRecomputesAssignedUsersAndDeactivatingRevokesDerivedGrants` Tests run 1 Failures 0 Errors 0；`AuthorizationServiceTests#grantWhoseValidityHasNotStartedDoesNotAuthorize` Tests run 1 Failures 0 Errors 0。`MVN_EXIT=0`。
2. 发布口径：`git worktree add --detach /tmp/m036_b4_r1_pubwt origin/main`（`3ca1b507`）；拷入未发布 V427 与三份发布变体测试文件；只导出三个 `COMPLEX_DATABASE_*`（从 `/etc/complex/complex.env` 解析，**未 source**），URL 库名改 `complex_test`；`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`；`mvn -Dtest=AccessServiceTests,AuthorizationServiceTests,M006JdbcRegressionTests test` → `MVN_EXIT_PUBWT=0`：AccessServiceTests **54/0/0**、AuthorizationServiceTests **22/0/0**、M006JdbcRegressionTests **13/0/0**。随后 `git worktree remove --force`，目录已不存在。主工作区三文件 blob 未变。

主工作区整类对照（非发布口径，不否决本票）：冻结日志末段 AccessServiceTests 6 ERROR＝W-M036-3 直接受益人；M006JdbcRegressionTests 2 ERROR＝W-M036-2 `grant_group_id`（新真库测试同因，因主工作区 `JdbcAccessRepository` 未发布 SQL 读该列，origin/main 用 `id::text`）；AuthorizationServiceTests 25 全绿（24 未发布＋1 新测）。属 D-029 已登记，不入本模块队列。

### 3.4 业务发明／越界／模式

- 无新权限码、无新状态机、无新审批规则。注释引用治理地图原文是 002 的冻结上位，不是新需求。
- 无「顺手优化／换框架／补抽象」。`LinkedHashSet` 并集、`positionApplicationIds`、TEMP 补列均为定稿 ①③ 的最小缝。
- ponytail-review：`size()+1` 短于 UUID；`Map.of` 两对角色；真库测试只 INSERT 三条。**Lean already.**
- 可执行位与文件模式未变（100644／664）。
- 三个文件版本各加一。

内容票：**PASS。**

---

## 4. 对抗式自问（grilling 结构，自答）

1. **`applyTemplate` 不用 UUID 是否偏离 B2「与 Jdbc `pta_`+UUID 同形」？** 否。B2 写「UUID（或等价唯一键）即可」。夹具只追加，`size()+1` 对同用户二次套用唯一；若仍用 `app_<userId>`，第二次 apply 与第一次同 id，撤旧会把刚插入的新应用一并 revoked，定岗后有效集必假红——这正是 B2 补这一刀的原因。
2. **改映射处 `containsExactly("revoked","active")` 是否超出定稿「停用后旧应用为 revoked」？** 否。它锁的是 ① 的「先撤上一次岗位应用再记新 id」，变异 `assign_passes_no_app_ids` 正红在这一行。停用处另有 `containsOnly("revoked")` 对应定稿 ② 原文。
3. **真库测试 `user_id='user'` 会不会打到 public 行？** 否。`CREATE TEMP TABLE … LIKE public` 后未限定名解析到 `pg_temp`；六张 JOIN 表均 TEMP；类约定不写 public。本席发布口径 GREEN 13 证明 INSERT／SELECT／UPDATE 走 TEMP。`replacePositionGrants` 要求 `changed==1`，TEMP 里先 INSERT 了一行岗位。
4. **新授权测试若把 2099 写在 `validUntil` 上会不会假绿？** 会，且会被本席否决。实装是 `validFrom=2099-01-01`。`temporaryGrant` 助手 2099＝截止、`validFrom=2020`，未调用。变异去掉 `validFrom` 判断后面 `requirePermission` 不再抛，证明闸打在生效时间而非到期。
5. **主工作区 AuthorizationServiceTests V0.03→V0.04 会不会把未发布 MASTER／商户范围反转带进发布？** 否。本 tree 是主工作区 before→after，before 已是 V0.03。发布变体＝origin/main V0.01＋同一测试正文，版本 V0.02。插入测试逐字相同。B2 禁止随本模块带出 V0.03 的 MASTER／商户范围反转——发布文件不是 V0.03 底。
6. **主工作区 M006 新测 ERROR 是否说明 ①③ 没修好？** 否。ERROR 文本是 TEMPORARY_GRANT SQL 的 `grant_group_id`（W-M036-2）。发布口径 origin/main Jdbc 无该列，本席 GREEN 13。属 D-029，不入队。

---

## 5. 本席未做 / 缺口

- 未读 `M036-b4-r1-self-raw.md`。
- 未 fetch／commit／push／merge／deploy。
- 未改业务／HTML／锁／findings／ledger。
- 未对数据库发写语句（跑既有测试类除外；M006 只建 TEMP）。
- 未派 MCP／子代理／第三席。
- 未新建持久脚本或审计产物。
- 三个变异未本席重放 worktree（既有日志违例行已与 after 树断言对齐）。
- `batch-grill-me`／`ubiquitous-language` 本机不存在。
- Arc `audit`／Trail of Bits 差分审／requesting-code-review 子代理路径故意不用。

---

## 6. 票面

| 维度 | 票 |
|---|---|
| 内容（是否按定稿最小处置与 B2 边界落地；断言能失败；无业务发明；模式未变） | **PASS** |
| 归因（§5 闸 3：逐行归到 001／002） | **PASS** |
| 合取 | 内容 FAIL 或归因 FAIL 即 FAIL；两者均 PASS |
| **本席 B4-r1 总结论** | **PASS** |

不是模块 PASS，不是 B5，不是授权收口。双席合取后才进 B5。本席 FAIL 否决权本轮不行使。

环境：`y9000p-ai`；正本 `/home/james/complex-management`；日期 2026-09-21。

---

## 7. 本轮 diff 全文（冻结文件，与两 tree 实算 IDENTICAL）

```
diff --git a/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java b/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
index da9feeb5..ebdf8cae 100644
--- a/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.08
+// 文件版本：V0.09
 
 import com.complex.platform.support.InMemoryAuditEventRepository;
 import com.complex.platform.support.InMemoryIdempotencyRepository;
@@ -1572,11 +1572,14 @@ class AccessServiceTests {
         accessRepository.users.put("usr_recompute", new AccessUser(
                 "usr_recompute", "recompute", "staff", "seed-org", null, "active",
                 Instant.parse("2026-06-14T00:00:00Z")));
-        for (String roleId : List.of("role_recompute_old", "role_recompute_new")) {
-            accessRepository.roles.put(roleId, new RoleRecord(
-                    roleId, roleId, "Ops checker", "project", "active",
+        // M036-001：旧角色、新角色、模板各带一个互不相同的码，有效权限集才分得清派生来自哪里。
+        for (Map.Entry<String, String> role : Map.of(
+                "role_recompute_old", "ops.production_readiness.view",
+                "role_recompute_new", "governance.supervisor_review.view").entrySet()) {
+            accessRepository.roles.put(role.getKey(), new RoleRecord(
+                    role.getKey(), role.getKey(), "Ops checker", "project", "active",
                     Instant.parse("2026-06-14T00:00:00Z"),
-                    List.of("ops.production_readiness.check"),
+                    List.of(role.getValue()),
                     null));
         }
         accessRepository.saveTemplate(new PermissionTemplate(
@@ -1598,12 +1601,19 @@ class AccessServiceTests {
                 "assign-recompute", null, "usr_recompute", "ops_recompute", "lvl_m1_lower",
                 500000L, "CNY", null));
         assertThat(accessRepository.positionRoles.get("usr_recompute")).isEqualTo("role_recompute_old");
+        assertThat(authorizationService.effectivePermissionsForUser("usr_recompute").grants())
+                .contains("ops.production_readiness.view", "ops.production_readiness.check");
 
         service.updatePositionRoleTemplate(mapping.id(), new PositionRoleTemplateRequest(
                 "map-recompute-change", null, "ops_recompute", "lvl_m1_lower",
                 "role_recompute_new", "tpl_recompute", "active"));
 
         assertThat(accessRepository.positionRoles.get("usr_recompute")).isEqualTo("role_recompute_new");
+        assertThat(authorizationService.effectivePermissionsForUser("usr_recompute").grants())
+                .contains("governance.supervisor_review.view", "ops.production_readiness.check")
+                .doesNotContain("ops.production_readiness.view");
+        assertThat(accessRepository.templateApplications).filteredOn(a -> "usr_recompute".equals(a.targetId()))
+                .extracting(PermissionTemplateApplication::status).containsExactly("revoked", "active");
         assertThat(accessRepository.findStaffLevelAssignment("usr_recompute")).get()
                 .extracting(StaffLevelAssignment::baseSalaryAmountCent)
                 .isEqualTo(500000L);
@@ -1614,6 +1624,11 @@ class AccessServiceTests {
 
         assertThat(accessRepository.positionRoles).containsKey("usr_recompute");
         assertThat(accessRepository.positionRoles.get("usr_recompute")).isNull();
+        assertThat(authorizationService.effectivePermissionsForUser("usr_recompute").grants())
+                .doesNotContain("ops.production_readiness.view", "governance.supervisor_review.view",
+                        "ops.production_readiness.check");
+        assertThat(accessRepository.templateApplications).filteredOn(a -> "usr_recompute".equals(a.targetId()))
+                .extracting(PermissionTemplateApplication::status).containsOnly("revoked");
     }
 
     // M031-003（GEN-011）：岗位模板生命周期与授权政策两个写口失败也留痕。
@@ -1684,8 +1699,14 @@ class AccessServiceTests {
             if (fingerprints.putIfAbsent(id, fingerprint) != null) throw new IllegalStateException("fingerprint already set");
         }
         @Override public Optional<String> findGrantFingerprint(String id) { return Optional.ofNullable(fingerprints.get(id)); }
+        final Map<String, List<String>> positionApplicationIds = new LinkedHashMap<>();
+        // M036-001：与 JdbcAccessRepository 同语义——先撤上一次岗位派生的模板应用，再记新角色与新应用 id；独立模板应用不受影响。
         @Override public void replacePositionGrants(String userId, String roleId, String mappingId,
-                List<String> applicationIds, String actorId, Instant at) { positionRoles.put(userId, roleId); }
+                List<String> applicationIds, String actorId, Instant at) {
+            revokeTemplateApplications(userId, positionApplicationIds.getOrDefault(userId, List.of()), actorId, at);
+            positionRoles.put(userId, roleId);
+            positionApplicationIds.put(userId, List.copyOf(applicationIds));
+        }
         final Map<String, PermissionTemplate> templates = new LinkedHashMap<>();
         final Map<String, List<PermissionTemplateItem>> items = new LinkedHashMap<>();
         final Map<String, TemporaryGrant> temporaryGrants = new LinkedHashMap<>();
@@ -1925,7 +1946,7 @@ class AccessServiceTests {
         ) {
             List<PermissionTemplateApplication> applications = targetUserIds.stream()
                     .map(userId -> new PermissionTemplateApplication(
-                            "app_" + userId,
+                            "app_" + userId + "_" + (templateApplications.size() + 1),
                             templateId,
                             "user",
                             userId,
@@ -2016,7 +2037,12 @@ class AccessServiceTests {
                     effects.add(override);
                 }
             }
-            for (String roleId : userRoles.getOrDefault(userId, Set.of())) {
+            // M036-001：与 JdbcAccessRepository 同语义——岗位派生角色与直接装配的角色一样以 ROLE 来源并入授予。
+            Set<String> roleIds = new LinkedHashSet<>(userRoles.getOrDefault(userId, Set.of()));
+            if (positionRoles.get(userId) != null) {
+                roleIds.add(positionRoles.get(userId));
+            }
+            for (String roleId : roleIds) {
                 RoleRecord role = roles.get(roleId);
                 if (role == null || !"active".equals(role.status())) {
                     continue;
diff --git a/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java b/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java
index 714c10a2..f3e040d0 100644
--- a/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.03
+// 文件版本：V0.04
 
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
@@ -363,6 +363,26 @@ class AuthorizationServiceTests {
         assertThat(service.effectivePermissions().grants()).doesNotContain("ops.production_readiness.view");
     }
 
+    // M036-002（治理地图「临时授权可设有效期或单次使用」）：生效时间未到的授予不放行，也不进有效权限集。
+    @Test
+    void grantWhoseValidityHasNotStartedDoesNotAuthorize() {
+        authenticate("usr_staff", "staff", "staff");
+        repository.addEffect(new AccessRepository.PermissionEffect(
+                "usr_staff",
+                "ops.production_readiness.manage",
+                AccessRepository.Effect.GRANT,
+                AccessRepository.Source.TEMPORARY_GRANT,
+                "temp_not_started",
+                Instant.parse("2099-01-01T00:00:00Z"),
+                Instant.parse("2099-01-02T00:00:00Z"),
+                false,
+                false
+        ));
+
+        assertPermissionDenied("ops.production_readiness.manage");
+        assertThat(service.effectivePermissions().grants()).doesNotContain("ops.production_readiness.manage");
+    }
+
     private AccessRepository.PermissionEffect effect(
             String userId,
             String permissionCode,
diff --git a/backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java b/backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java
index 37c1310c..ece6ff7e 100644
--- a/backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java
+++ b/backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.13
+// 文件版本：V0.14
 import static org.assertj.core.api.Assertions.*;
 import static org.junit.jupiter.api.Assumptions.assumeTrue;
 
@@ -248,6 +248,29 @@ class M006JdbcRegressionTests {
         assertThatThrownBy(() -> repository.deletePositionRoleTemplate("mapping")).isInstanceOf(IllegalStateException.class);
     }
 
+    // M036-001：岗位派生角色（user_position_assignments.assigned_role_id）的权限码以 ROLE 来源并入有效权限；
+    // 岗位派生置空后不再返回。
+    @Test void positionDerivedRoleFeedsPermissionEffectsUntilThePositionGrantsAreCleared() {
+        for (String table : java.util.List.of("user_roles", "roles", "role_permissions", "user_position_assignments",
+                "permission_template_items", "user_permission_overrides")) {
+            jdbc.execute("CREATE TEMP TABLE " + table + " (LIKE public." + table + " INCLUDING ALL)");
+        }
+        jdbc.execute("ALTER TABLE pg_temp.permission_templates ADD COLUMN status varchar, ADD COLUMN valid_from timestamptz, ADD COLUMN valid_to timestamptz");
+        jdbc.execute("ALTER TABLE pg_temp.permission_template_applications ADD COLUMN target_type varchar, ADD COLUMN revoked_by varchar, ADD COLUMN revoked_at timestamptz");
+        jdbc.execute("ALTER TABLE pg_temp.temporary_permission_grants ADD COLUMN subject_type varchar");
+        jdbc.update("INSERT INTO roles (id, role_name, data_scope, status, created_at) VALUES ('position-role','岗位角色','project','active',now())");
+        jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES ('position-role','position.only.code')");
+        jdbc.update("INSERT INTO user_position_assignments (user_id,position_code,staff_level_id,organization_id,assigned_by,assigned_role_id) VALUES ('user','position','level','org','operator','position-role')");
+
+        assertThat(repository.findPermissionEffects("user", Instant.now()))
+                .extracting(AccessRepository.PermissionEffect::permissionCode, AccessRepository.PermissionEffect::source,
+                        AccessRepository.PermissionEffect::effect)
+                .containsExactly(tuple("position.only.code", AccessRepository.Source.ROLE, AccessRepository.Effect.GRANT));
+
+        repository.replacePositionGrants("user", null, null, java.util.List.of(), "operator", Instant.now());
+        assertThat(repository.findPermissionEffects("user", Instant.now())).isEmpty();
+    }
+
     private UUID temporaryGrant() {
         UUID id = UUID.randomUUID();
         jdbc.update("INSERT INTO temporary_permission_grants (id, subject_id, permission_code, single_use) VALUES (?, 'user', 'permission', true)", id);
```
