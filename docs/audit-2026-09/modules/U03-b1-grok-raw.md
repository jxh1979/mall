CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U03 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 只读全量三线（未发布材料第 3 段；不做 B3/B4/B5；处置写给发布者，本轮不修源）
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 段：U03 — 未发布材料第 3 段「员工网页和组织网页」
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 末尾附录；`plan/runbook.md` V0.02 §11；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 审计对象（不得冒称 main 或已发布修复）：
  - 未发布提交：`03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb`（本仓 `git cat-file -t` **无此 commit 对象**）
  - 未发布树：`e28dad059b2c7e08bcc95a8db18552d3f76edf4b`（`git cat-file -t` = **tree**，可解析）
  - 发布基线：`242f487233d028173cb9198eecd079f5d877405c`（commit，可解析）
  - 只读输入根：`/home/james/complex-management/.scratch/module-recheck-20260908/unpublished-03acdb98`
  - 清单：`.scratch/module-recheck-20260908/unpublished-03acdb98-input.json` V0.01，76 文件
- 本段：8 文件，物理行 1844，标记 NORMAL；main 主归属 M012、M057、M081、M290、M293；新增文件无 main 归属者适用 GEN-001–018
- 冻结判据：上述模块映射行并集 21 项（与锁行并集相同；COUNT=21 unique=21 MISSING=[]）：GEN-001…GEN-018、BSM-001、BSM-003、PGM-001
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 且发布前须修：GR-U03-001、GR-U03-002、GR-U03-003、GR-U03-004、GR-U03-005、GR-U03-006、GR-U03-007、GR-U03-008、GR-U03-009、GR-U03-010、GR-U03-011、GR-U03-012、GR-U03-013、GR-U03-014
- 添加项（B1 只登记，不代用户回答 2.2；B2 须逐条发问）：GR-U03-002、GR-U03-003
- 只登记、建议队列「否」：观察见 §8
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做写操作
- 本报告不表示全仓、main 模块或发布门禁通过。M001–M010 检查点已合并；M011–M012 检查点已合入当前 HEAD（见 §1）。原 322/471 模块结论不继承到本未发布段。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md` | 内部 sharpen（本体只转发 grilling） |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（overlay vitest / tsc / git diff）再下结论；不改生产代码 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流（本段 8 文件→U04 types / U05 Staff* / U06 Platform*→main 对照→HTML） |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；未跑的后端集成不写通过 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本段验证均秒级，未起 monitor |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：本机技能树未找到
- `ubiquitous-language`：本机技能树未找到
- Arc `audit` 会派评审子代理：与双席/§7 冲突，**不调用**
- `using-superpowers` 要求技能优先于探索：与「禁止子代理 / 本提示指定 controller」冲突处，按用户指令收窄

明确不用：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、MCP、第三主体。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 同值
- `criteria-lock.md` 版本 V0.11；判据总数 401
- 映射行：
  - `M012 →` L6391 闭集含 BSM-001/BSM-003/GEN-001–018/PGM-001
  - `M057 →` L6436 同上
  - `M081 →` L6460 同上
  - `M290 →` L6669 同上
  - `M293 →` L6672 同上
  - 并集 COUNT=21 unique=21 MISSING=[]，与开工提示闭集相同
- `plan/modules.md` 附录 U03 八路径与用户清单逐字相同

HEAD（独立实算，**与开工提示中的 bd445728 不一致**）：

- `git rev-parse HEAD` = `git rev-parse origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`
- `git log -1 --oneline`：`445a49f4 Merge pull request #429 from jxh1979/audit/20260909/M011-M012-checkpoint`
- 提示所称 `bd445728a6ec7bb36a9e7ff19fe56927410ac61e` 存在且为祖先（`Merge pull request #428` M001–M010 检查点）；`merge-base --is-ancestor` = YES
- 本席对照 main 已修项时以 **当前 HEAD=445a49f4** 为准（含 M011–M012 已合入修复）
- 本席未设 `GIT_INDEX_FILE`，未动主索引

ledger 只读核验（不改）：V2.49，463871 bytes，SHA256 `2778c55906551f97e3c5e79b59d69a913505753e4efa509414c126d0a7a9c488`（与开工提示一致）。findings V1.47，554183 bytes，SHA256 `207b968f74fcfaaa917ded2e748e5b0dfe1cc5a1ef7a08aad6cca4214d4d6105`。台账中与本段同名文件直接相关的已定稿项：M009-003（合作方式六类）、M012-006/007/008（创建预填/电话正则/存档写路径）。本席独立对照见 §6，不继承其 PASS。

其它只读哈希：

| 文件 | 字节 | SHA256 |
|---|---:|---|
| `plan/runbook.md` V0.02 | 10409 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `plan/modules.md` V0.04 | 421826 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |
| `160-audit-prompt.md` V0.17 | 65856 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `unpublished-03acdb98-input.json` V0.01 | 17547 | `10eb8240ee08ee2fae053235250f19335cacc2ff8d9a2ed5edbe69be366293e9` |
| `docs/business-enum-quick-reference-v0.1.html` | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 304482 | `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 103015 | `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c` |

三 HTML 只读、未改。锁内本段用到的锚点行对本席读取的现行 HTML **全部命中**（见 §1.1）。三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。BEQR「合作模式」「员工状态」不在本段冻结闭集，仅作数据流对照，不作为 finding 支撑 ID。

### 1.1 HTML 锚点实核（锁出处行 → 现行正本）

| 判据 | 锁出处 | 现行该行含 |
|---|---|---|
| BSM-001 | bsm:505 | `整体说明：这套系统到底管什么`（同段含「合作模式有哪几种」及策略可配原则） |
| BSM-003 | bsm:563 | `系统通用能力：数据可改可删、操作有引导、权限分岗`（锁摘录仅为 h2+导语；存档删除细则属未映射 BSM-004，不拿未映射子条当本段冲突 ID） |
| PGM-001 | pgm:256 | `整体定位：平台治理层不是一个业务部门，而是所有业务的底座`（同段含「见名可点 / 不允许孤立数据」） |

对照用（非本段冻结 ID，不支撑 finding）：BEQR L231 合作模式六类；BEQR L240 员工状态五值；BSM L701/L1433/L1441–1458 商户合作方式、员工注册两条路径、排班考勤请假薪资；PGM L495/L578 合作方式与自营标注。

## 2. 8 文件覆盖表（全文已读；行=`\n` 计数 = splitlines）

副本根：`unpublished-03acdb98/`。清单八条目 sha256/行/字节与独立 `sha256sum`/`wc` **8/8 match**。副本 `git hash-object` 与树 `e28dad05:<path>` blob **8/8 相同**。

| # | 路径 | 行 | 字节 | SHA256 | 全文 |
|---|---|---:|---:|---|---|
| 1 | `apps/web/src/admin/pages/OrganizationMerchantPage.tsx` | 480 | 29799 | `54e39b868275943cd76987e8a6d93de88f13b3c93695593a861a0893d9b96503` | 已读 L1–L480 |
| 2 | `apps/web/src/admin/pages/StaffPayrollLeavePage.tsx` | 331 | 21079 | `e0b6c7fc87b505f9835877224582a251330911064d145b7567adbb591e3d95ea` | 已读 L1–L331 |
| 3 | `apps/web/src/admin/pages/StaffProfilesPage.test.tsx` | 112 | 5780 | `f4e3a1fb971deece7874b14e168a26b0e3c5fd64994e123017d3b132ac50614e` | 已读 L1–L112 |
| 4 | `apps/web/src/admin/pages/StaffProfilesPage.tsx` | 212 | 12098 | `4eab40e08aab79009a2a49f4392b13ca15acadccc2b4c3a2132006be619494d6` | 已读 L1–L212 |
| 5 | `apps/web/src/admin/pages/StaffShiftAttendancePage.batchshift.test.tsx` | 134 | 7110 | `32abe8911e674954a0edb810283196e10175339023c4868de396618e7607d0db` | 已读 L1–L134 |
| 6 | `apps/web/src/admin/pages/StaffShiftAttendancePage.guidedcreate.test.tsx` | 107 | 5525 | `f2b007727b00d34b11260df996976a3e5cd5cc16a7b00734434dfa575f8fece2` | 已读 L1–L107 |
| 7 | `apps/web/src/admin/pages/StaffShiftAttendancePage.settings.test.tsx` | 61 | 3752 | `2068734c7a8760f49d16e69d8d7d466d4ebcb6eca743a96e9e68d623107e331a` | 已读 L1–L61（main 无此文件） |
| 8 | `apps/web/src/admin/pages/StaffShiftAttendancePage.tsx` | 407 | 26866 | `1629b00be3595d1373a8a25dc4e2d6ed504347b0c4c194b7e229e8d12dd6bed2` | 已读 L1–L407 |

合计 1844 行 / 112009 字节。

### 2.1 相对发布基线的逐文件 diff stat（真实改动）

命令：`git diff --numstat/--stat 242f487233d028173cb9198eecd079f5d877405c e28dad059b2c7e08bcc95a8db18552d3f76edf4b -- <path>`

| 路径 | + | − | shortstat |
|---|---:|---:|---|
| OrganizationMerchantPage.tsx | 12 | 0 | 12 insertions（文件头版本注释 + 自营抽屉字段 + 自营标记动作） |
| StaffPayrollLeavePage.tsx | 39 | 1 | 40 行 hunk；扣薪文案改为工时比例；新增普通扣除上限审批 UI |
| StaffProfilesPage.test.tsx | 35 | 0 | 新增员工状态/存档/删除申请 3 例 |
| StaffProfilesPage.tsx | 72 | 12 | 84 行 hunk；注册审批/存档删除/五态/手机号 |
| StaffShiftAttendancePage.batchshift.test.tsx | 4 | 3 | 审批单断言 |
| StaffShiftAttendancePage.guidedcreate.test.tsx | 3 | 2 | 审批单断言 |
| StaffShiftAttendancePage.settings.test.tsx | 61 | 0 | **新文件** |
| StaffShiftAttendancePage.tsx | 59 | 16 | 75 行 hunk；排班审批/可见天数/修改历史 |
| **本段合计** | **285** | **34** | 8 files changed |
| **全 76 文件** | **4745** | **1084** | 与提示一致 |

blob：

| 路径 | 基线 git blob | 未发布树/副本 blob | 当前 HEAD blob | BASE vs MAIN |
|---|---|---|---|---|
| OrganizationMerchantPage.tsx | `432714076c2a782773f3f656384a93210954a53e` | `7e951704d02d79c999ae3d688bdfd1902590c7c7` | `7d5b3ee9c5ee5f590c30401e0b02c1722efe5d94` | **不等**（main 自基线 +71/−47） |
| StaffPayrollLeavePage.tsx | `0ba58e7be09d2f54c43b32d26458d9c021f5a24d` | `093a0ae6c7288b24102bd6454ade9fb492127da8` | 同基线 | 相等 |
| StaffProfilesPage.test.tsx | `77e0954a0e3235c3d32a8629f6f996824d0a5389` | `66072ae98dcb6fc67d2659c711929b7608259c68` | 同基线 | 相等 |
| StaffProfilesPage.tsx | `d8dedc7648424fd5fbd983015644f21cc34c3c10` | `8a433cd2b0a5129fddd2be3f5e866c8962846191` | 同基线 | 相等 |
| StaffShiftAttendancePage.batchshift.test.tsx | `691029a54d6a2a7372e0e3628fdc34c8128c6339` | `01838a6261cc4de43dbf57f9e61cdfc9e1888c4d` | 同基线 | 相等 |
| StaffShiftAttendancePage.guidedcreate.test.tsx | `ab5aa0af468c232cf760712bdf381418689cc546` | `c212cc4b69333820ef2ad5c4af64a9147934d088` | 同基线 | 相等 |
| StaffShiftAttendancePage.settings.test.tsx | 基线/HEAD **无此路径** | `802f0de0f57af64da7428ceec13312e3e25bb79a` | MAIN_MISSING | 新增 |
| StaffShiftAttendancePage.tsx | `4954b599a30abdc63ede898e9a078e0d55447f14` | `ee78d947825b3680d40b542182fd78d184441bf2` | 同基线 | 相等 |

`git diff --shortstat` 基线..HEAD 对本段 8 路径：**仅** OrganizationMerchantPage.tsx 1 file +71/−47。其余 6 个已存在文件 main 自基线未改；settings.test.tsx 为未发布新增。

未发布相对基线 OrganizationMerchantPage 的实质新增（12 行，不含纯版本注释则 11 行业务）：

- 抽屉字段「自营商户」读 `row.selfOperated`
- 行内动作「标记为自营 / 取消自营标记」POST `/api/v1/platform/merchants/{id}/self-operated`

**该文件其余 468 行仍是基线实现**，因此带着基线的三值合作方式、验收预填、11 位手机号正则、存档无 try/catch 一起发布。

## 3. 数据流（本段 → 未发布其它段 → main 对照 → SQL/HTML）

```
OrganizationMerchantPage (U03)
  ├─ types.MerchantAdmissionRow.selfOperated          ← U04 apps/web/src/admin/types.ts:1121
  ├─ AdminApi.toSnakeCaseKey                          ← main adminClient.ts（本段未改；settings 测试覆盖）
  ├─ POST /api/v1/platform/merchants                  ← 未发布 PlatformService.saveMerchant
  ├─ POST /api/v1/platform/merchants/{id}/self-operated
  │     PlatformController.setMerchantSelfOperated    ← 未发布新增；main grep 0 命中
  │     PlatformService.setMerchantSelfOperated
  │     merchants.is_self_operated                    ← 未发布 V408
  └─ 合作形式三码 fixed_rent_plus_commission / revenue_share / fixed_rent
        与 HTML 六类及 main M009-003 白名单冲突

StaffProfilesPage (U03)
  ├─ GET /api/v1/staff/profiles?include_archived=true
  ├─ POST .../archive|unarchive|delete-request|delete-approve|employment-status|sales-position
  ├─ POST /api/v1/staff/registrations/{id}/review
  └─ 未发布 StaffController / StaffService
        删除：master 在 requestDeleteProfile 内直接 approveDeleteProfile
        五态白名单 pending_entry/employed/trial/suspended/departed
        maskedPersonalInfo 掩手机号

StaffPayrollLeavePage (U03)
  ├─ GET/POST /api/v1/staff/settings/payroll-penalty-cap  ← 未发布；bps，默认 2000=20%
  ├─ GET /api/v1/staff/payroll?month=
  ├─ POST /payroll/base|/adjustments|/close
  └─ GET/POST /api/v1/staff/leaves

StaffShiftAttendancePage (U03)
  ├─ GET/POST /api/v1/staff/settings/shift-visible-days   ← 未发布；即时保存
  ├─ POST /api/v1/staff/shifts 与 /shifts/batch          ← StaffApprovalService.submitShifts
  ├─ GET /api/v1/staff/shifts/{id}/history
  └─ listShiftsPaged 读 staff_shift_schedules（已发布行；草稿在 revision 表）
```

main 对照（只读，非本段修复对象）：

- `OrganizationMerchantPage.tsx` V0.02：COOPERATION_FORMS 六类中文 option；表单空初始；电话非空校验；存档/删除 try/catch + 测试 15/15 绿
- `apps/web/src/admin/types.ts` **无** `selfOperated`
- `backend/.../PlatformService.java` MERCHANT_COOPERATION_FORMS = `{self_operated,lease,joint_operation,managed_operation,service_provider,other}`；**无** `setMerchantSelfOperated`
- 人事三页 main blob = 基线 blob，M009–M012 未改它们；发布未发布版不会覆盖人事页已修项（那些修复不在这三文件），但会把基线+未发布增量装上当前 main

跨段依赖：只发布本段 8 文件、不带 U04 `types.ts` → tsc 4 红（GR-U03-006）；不带未发布 Platform/Staff 后端 → 自营 POST 对当前 main 无路由、合作三码被 main 白名单拒绝（GR-U03-001）。

## 4. 三线审计

### 4.1 第一性原理（每文件三问）

#### OrganizationMerchantPage.tsx（L1–L480）

1. **为何必须存在**：BSM-001「先管清楚基础盘子：哪个综合体、谁在经营」；PGM-001「先管归属：哪个商户」。组织/商户/项目档案页有上位。自营标记有上位「在商户档案上标注为自营」（BSM 员工注册条 / PGM-578），属该句的下位实现。
2. **声称是否执行**：声称维护基础档案、业态六值、引导式新建、存档删除。实际：业态六值下拉在；**合作形式仍是收费模板三码**；创建静默提交假主体/假证据/预填「二期测试商户」；电话正则 11 位大陆号；存档/删除无 try/catch。自营 POST 依赖未发布后端。
3. **未验证前提**：发布到当前 main 时，默认 `cooperationForm="fixed_rent_plus_commission"` 会被 M009-003 白名单拒绝；`selfOperated` 字段在 main 类型中不存在。首次把该页合进当前 main 就会撞上（GR-U03-001/002/006）。

#### StaffPayrollLeavePage.tsx（L1–L331）

1. **为何必须存在**：BSM-001 管工作人员；HTML 薪资四块+请假审批是人事经营动作。有上位。扣除上限 UI 是 BSM-001「各类扣除合计不得超过…20%…人力、财务、法务三审并由顶层平台最终确认」的下位。
2. **声称是否执行**：扣薪文案已改为「事假按请假占实际排班的工时比例」，与 HTML 请假扣薪条一致（相对基线的日工资÷21.75 是纠偏）。上限提交走审批、测试断言「不把提交当成生效」。月结/奖惩按钮无 try/catch，失败成未处理拒绝。
3. **未验证前提**：顶层平台确认是否由 `SupervisorReviewService.evaluate(parallelLines)` 补全，本段前端只采集三审人。未跑后端审批链。收窄为观察。

#### StaffProfilesPage.tsx（L1–L212）+ `.test.tsx`（L1–L112）

1. **为何必须存在**：销售岗是提成费率档的档案标识（HTML 销售提成条）；员工状态五值与 BEQR-240 同文；存档删除是 BSM-003 标题级「数据可改可删」。有上位。
2. **声称是否执行**：五态+事由、存档须事由、删除申请不等于批准，测试 3 例覆盖。`isMaster` 客户端隐藏批准钮；未发布 `StaffService.requestDeleteProfile` 对 master 会直接批准——UI 文案「直接删除」与后端一致，不是只靠界面。列表始终 `include_archived=true` 再客户端过滤。
3. **未验证前提**：注册批准/驳回、master 删除路径本段测试未跑。人员/商户 ID 无 TraceLink。

#### StaffShiftAttendancePage.tsx（L1–L407）+ 三测试

1. **为何必须存在**：主管排班、可见天数、严格考勤是 HTML 排班考勤条的后台面。有上位。批量按周是「主管排班」的下位效率实现。
2. **声称是否执行**：单班/批量改为提交审批（测试改断言 review_id）；可见天数首次须上下限（settings 测试绿）；修改历史走 `/history`。描述写「修改排班必须填写原因」，确认步 **无** note 校验，打开编辑时 `setNote("")`。单班点位 `split(",")`，批量用 `splitList`（中英文逗号）。
3. **未验证前提**：`today()`/`currentMonth()` 用 `toISOString().slice`（UTC）。中国站点本地 08:00 前第一次打开会落到昨日/上月。HTML 考勤「以当地时间为准」在未映射子条；本段用 GEN-002。

#### StaffShiftAttendancePage.settings.test.tsx（新文件）

1. **为何必须存在**：给可见天数与扣除上限提供能失败的断言。有测试上位（GEN-003）。
2. **声称是否执行**：隔离 vitest 2/2 绿；AdminApi 蛇形转换与页面 camelCase 一致。
3. **未验证前提**：不覆盖「已有上下限的二次修改」、不覆盖顶层确认、不覆盖自营按钮。

### 4.2 顺从式（21 条逐项）

| ID | 本段适用面 | 结论 |
|---|---|---|
| GEN-001 | 八文件均有档案/人事上位 | 页有必要。合作三码不是最小实现（应用 HTML 六类）。自营标记有上位。通过必要性；三码走冲突 001 |
| GEN-002 | 创建/存档/排班/月结主路径 | **FAIL** 001（三码 vs main 白名单）、002（假主体）、004（存档失败不可见）、006（tsc/404）、008（UTC 日）、009（修改无事由校验）、010（中文逗号点位）、014（月结/奖惩无 catch） |
| GEN-003 | 本段 4 个测试文件 | 自测 16/16 绿。**FAIL** 007：自营按钮/修改排班/master 删除/注册审批无用例；main 商户页 15 例对未发布页 6 红 |
| GEN-004 | 创建/配置/电话/上限输入 | 电话正则是未授权的格式闭集（003）。上限/天数有上下限校验。假主体 id 当输入 → 002 |
| GEN-005 | 按钮 PermissionButton / RowActions | 商户动作挂 `merchant.profile.manage`；员工删除后端强制 master。请假审批钮无 permissions 字段，但列表仅 `staff.shift.manage` 才加载——观察。通过主路径 |
| GEN-006 | 存档先于删除；排班草稿与发布表分离 | 前端删除入口仅 `archivedAt`；后端 `STAFF_PROFILE_NOT_ARCHIVED`。排班 list 读已发布表。通过 |
| GEN-007 | 幂等键 `Date.now()` | 单击首次不撞；连点同毫秒属铁律 6 收窄。观察 |
| GEN-008 | 合作方式/自营字段/前后端 | **FAIL** 001（三码 vs 六类 vs main 白名单）、006（selfOperated 类型与路由） |
| GEN-009 | 写路径错误可见 | **FAIL** 004（商户存档/删除）、014（薪资月结/奖惩） |
| GEN-010 | 员工手机号、初始密码、设备指纹 | 列表手机号走后端 mask。档案仍全量拉已存档 → **FAIL** 013。初始密码 type=password 属注册必要，观察。考勤抽屉展示 deviceFingerprint 属排班主管溯源，观察 |
| GEN-011 | AuditTrailHint + 后端 audit | 自营/销售岗/上限/可见天数后端有 recordSuccess。页面提示存在。通过 |
| GEN-012 | 列表默认全量、无分页 UI | 后端 profiles/shifts 有 page 默认 500。人事页一次拉全。与现网其它后台页同构，观察不升 |
| GEN-013 | 本段无包配置 | **无实现面**于构建脚本。发布面 tsc 依赖 U04 → 并入 006 |
| GEN-014 | 本段无 SQL | **无实现面**。跨段 V408 `is_self_operated` 不在本 8 文件 |
| GEN-015 | 展示中文 | **FAIL** 001（表内 `lease` 原文）、011（`员工 user_id` / `space_id`） |
| GEN-016 | 文件头版本、接口路径 | 路径字符串与未发布 Controller 映射一致。通过（不把注释当 HTML） |
| GEN-017 | 分类纪律 | 三码=冲突；预填/电话正则=添加；自营标记=细化；存档 catch=技术性。未把 HTML 沉默当删除 |
| GEN-018 | 本报告依据 | 类型 1 HTML 逐字 + 类型 4 命令。未用席甲结论当依据（ledger 仅作已登记边界） |
| BSM-001 | 基础盘子、合作模式清单、策略可配 | **FAIL** 001：合作形式三码增删「有哪几种」。上限/可见天数属策略参数下位，细化通过 |
| BSM-003 | 标题级可改可删、有引导、分岗 | 新建商户/薪资基数/排班有 GuidedFlowModal。存档失败不可见并入 004。锁摘录不含 BSM-004 细则，不拿「最高权限删除」子条当本段冲突 ID（员工删除后端已接 master，观察） |
| PGM-001 | 归属/资格/责任；见名可点 | 商户页有抽屉+TraceLink（项目→商户）。人事三页人员/商户 ID 不可点 → **FAIL** 012 |

### 4.3 对抗式（grilling 自问自答，不派子代理）

Frontier 已结算，不向操作员发问：

1. **发布这八文件会不会覆盖 main 上 M009–M012 已修项？** 会。OrganizationMerchantPage 是唯一 BASE≠MAIN 的本段文件。main 自基线 +71/−47（六类合作方式、清空预填、电话非空、存档 catch）。未发布相对基线只 +12 行自营，**其余仍是基线**。直推/覆盖该路径会把 M009-003、M012-006/007/008 已修 UI 打回。人事七文件 main=基线，覆盖的是「基线+未发布增量」，不破坏其它模块已修文件，但会把未发布人事行为打到当前 main 后端（部分路由 main 没有）。
2. **第一次用当前 main 后端打开未发布「新建商户」并提交，合作形式是什么？** 默认 `fixed_rent_plus_commission`。main `PlatformService` 白名单六类会 `MERCHANT_COOPERATION_FORM_INVALID`。首次正常会。
3. **第一次点「标记为自营」打到当前 main？** `POST /api/v1/platform/merchants/{id}/self-operated`。main Java 无 `setMerchantSelfOperated`，无该路由 → 404。首次正常会。tsc 在缺 U04 时 4 红，首次构建会。
4. **main 的 OrganizationMerchantPage.test 对未发布页第一次跑几红？** 本席隔离 overlay：**6 failed / 9 passed / 15**。对照：同一测试在当前 main 源上 **15/15 绿**。席甲 ledger 写「1 失败/11 通过」与本席实算不符；本席以本席命令为准。
5. **第一次存档：空理由拦得住吗？失败看得到吗？** 空理由仍 prompt 拦截。但自营按钮使行内从单按钮变成「操作 ▾」下拉，`merchant-archive-m-1` 默认不在文档里，main M012-008 用例 1012ms 找不到元素。接口失败时未发布页无 catch → 未处理拒绝。首次按 main 测试合同会。
6. **修改已有排班不填原因第一次提交？** UI 放行，未发布 `StaffService.draftShift` `requireReason(note)` → 400，弹窗 catch 显错。引导步未拦。首次会。
7. **中国时区 8 月 1 日 07:30 打开薪酬页月份是什么？** `currentMonth()` = `toISOString().slice(0,7)` → UTC 7 月。第一次正常会（每月 1 日 08:00 前）。
8. **把中文逗号点位粘进单班表单？** `points.split(",")` 得到一整段；批量 `splitList` 能拆。首次按排班员从名单粘贴的注释口径会。
9. **自营布尔和合作方式「自营」是不是两个旋钮？** 未发布里合作方式还不是六类，布尔单独实现「标注为自营」。若发布者先合 main 六类（含 `self_operated`）再叠布尔，就会出现两个独立「自营」。要两三步才成立 → 观察，不升独立缺陷（铁律 6）。
10. **HTML 沉默的「顶层平台确认」表单字段？** 文案已写「再由顶层平台最终确认」；前端只采集三审。不能因 HTML 沉默判冲突。观察。

## 5. 隔离验证命令与输出（只读 /tmp，不改副本/main）

环境：`/tmp/u03-b1-tree`（`tsconfig.base.json` + `package.json` + `apps/web` rsync，排除 node_modules；`ln -s` 仓库 `node_modules` 与 `apps/web/node_modules`；overlay 本段 8 文件）。未 `npm install`，未写永久测试，未碰 `complex_test*`。

CMD-U03-SELF：`cd /tmp/u03-b1-tree && npm run test:unit -- src/admin/pages/StaffProfilesPage.test.tsx src/admin/pages/StaffShiftAttendancePage.batchshift.test.tsx src/admin/pages/StaffShiftAttendancePage.guidedcreate.test.tsx src/admin/pages/StaffShiftAttendancePage.settings.test.tsx`

OUT-U03-SELF：exit 0；`Test Files  4 passed (4)`；`Tests  16 passed (16)`（7+2+3+4）。

CMD-U03-MAIN-TEST：同树跑 `src/admin/pages/OrganizationMerchantPage.test.tsx`（main 测试 × 未发布页）

OUT-U03-MAIN-TEST：exit 1；`Tests  6 failed | 9 passed (15)`。失败摘要：

| 用例 | 实算 |
|---|---|
| 三表真实字段 | expected 租赁，received 行内 `lease` |
| 合作形式下拉 | expected 租赁/服务商接入，received `固定租金+抽成收入分成固定租金` |
| 引导创建流 | 电话预填 `13800000000`，expected `''` |
| 商户抽屉 | expected 租赁，received `合作形式lease` + `自营商户否（商户代提交，经域公司审批）` |
| 存档空理由/POST | `Unable to find ... merchant-archive-m-1`（操作 ▾） |
| 存档失败可见 | 同上，找不到 archive testid |

CMD-U03-CTRL：当前 main 工作区跑同一 OrganizationMerchantPage.test.tsx → `Tests  15 passed (15)`。

CMD-U03-PAYROLL：overlay 上 `StaffPayrollLeavePage.robustness.test.tsx` → `Tests  1 passed (1)`。

CMD-U03-TRACE：overlay 上 `TraceLinkP2c.test.tsx` → `Tests  5 passed (5)`。

CMD-U03-TSC：`cd /tmp/u03-b1-tree/apps/web && npx tsc -b --noEmit`（仅 overlay 8 文件，types 仍为 main）

OUT-U03-TSC：exit 1；四处 `TS2339: Property 'selfOperated' does not exist on type 'MerchantAdmissionRow'`（L268/L320/L325/L326）。

CMD-U03-TSC-U04：再 overlay 未发布 `apps/web/src/admin/types.ts` 后同样 tsc → exit 0。随后已把 types 拷回 main 版，树内不留 U04。

未跑：后端集成、Flyway、浏览器 E2E、对 `complex_test*` 的任何写。不把未跑项写通过。

## 6. 与 main 已修项分叉对照（对抗式固定问题）

| 已定稿 ID | main 已修（HEAD 445a49f4） | 未发布副本同路径 | 发布覆盖后果 |
|---|---|---|---|
| M009-003 | COOPERATION_FORMS 六类中文；后端白名单六码 | 仍 `["fixed_rent_plus_commission","revenue_share","fixed_rent"]` + `formatBusinessStatus` | 覆盖六类 UI；创建默认三码被当前后端拒绝 |
| M012-006 | 表单空初始；创建请求不传假主体/假证据/面积租金/验收备注 | 预填「二期测试商户」「运营联系人」「13800000000」；请求含 org-1 / contract-subject-1 / att-merchant-profile-create / 1F-A / 128.5 / status active | 把操作员已答「删除」的验收默认值带回 |
| M012-007 | 电话非空校验，无 11 位正则 | `/^1[3-9]\d{9}$/` | 把操作员已答「删除」的格式发明带回 |
| M012-008 | 存档/删除 try/catch + 测试 | 无 catch；自营钮把 archive testid 收进下拉 | 覆盖写路径可见性与 15 例合同 |

处置（给发布者，本轮不修）：**以当前 main OrganizationMerchantPage 为底做三方合并**，只把「自营标注」字段/动作（及配套测试）迁到六类/空预填/非空电话/写路径 catch 之上；禁止用未发布整文件覆盖。人事七文件无 main 已修可覆盖，但须与未发布后端/U04 同批，否则自营 404、types 不编译。

## 7. Findings（临时 ID；正式 ID 仅 B2 分配）

### GR-U03-001 — 合作形式三码对抗 HTML 六类，并覆盖 M009-003

- **文件:行号**：`OrganizationMerchantPage.tsx:33,73,262,350,465`
- **原文**：`const COOPERATION_FORMS = ["fixed_rent_plus_commission", "revenue_share", "fixed_rent"];`；默认 `useState("fixed_rent_plus_commission")`；下拉 `formatBusinessStatus(form)`；表/抽屉同样 `formatBusinessStatus(row.cooperationForm)`。
- **业务影响**：把收费模板名当作合作方式，增删「有哪几种」；表内 `lease` 以英文原文出现；对当前 main 提交即 `MERCHANT_COOPERATION_FORM_INVALID`。
- **可复现**：CMD-U03-MAIN-TEST；下拉 received `固定租金+抽成收入分成固定租金`；行内 received `lease`。main 对照 15/15 绿。
- **冻结判据**：BSM-001（「合作模式有哪几种…增加或删减一类必须先经批准写入三份 HTML」）；GEN-008；GEN-015；GEN-002
- **2.3 依据类型**：1 HTML 原文 + 4 可复现命令
- **四分类**：**冲突**（与 BSM-001 明文六类清单矛盾；三码来自「默认模板」不是合作方式）
- **工程缺陷**：是。建议登记：是。发布前须修：是
- **最小处置**：弃三码，采用 main 已修六类中文 option 与可空未选项；创建默认空，不传非法码。自营**标注**走独立布尔或与 `self_operated` 合作方式对齐，不得用收费模板顶替。

### GR-U03-002 — 创建预填验收默认值与假主体/假证据（覆盖 M012-006）

- **文件:行号**：`OrganizationMerchantPage.tsx:68–73,159–178`
- **原文**：`useState("二期测试商户")` / `"运营联系人"` / `"13800000000"`；请求 `businessEntityId: entities[0]?.id ?? "org-1"`、`contractEntityId: "contract-subject-1"`、`settlementEntityId: "settlement-subject-1"`、`evidenceAttachmentIds: ["att-merchant-profile-create"]`、`operatorNote: "完整后台组织与商户档案验收创建"`、写死面积/租金/备用电话/`status: "active"`。页内边界文案仍称「不在本页替代四类主体绑定」。
- **业务影响**：验收数据进入生产创建路径；假主体无 FK 可悬空；与页内声明相反。操作员对 M012-006 已答「删除」。
- **可复现**：CMD-U03-MAIN-TEST 创建流：电话 value=`13800000000` expected `''`。源码 L159–178。
- **冻结判据**：GEN-017；GEN-004；GEN-011；BSM-001
- **2.3 依据类型**：4 可复现命令（源码+测试）
- **四分类**：**添加**（三份 HTML 无这些默认值/假主体/假证据；不隶属已有条目的下位细节）
- **无业务发明？** 不适用（本条是添加）。技术性通道不走。
- **工程缺陷**：是。建议登记：是。发布前须修：是（B2 须 2.2 询问；与已答「删除」的 M012-006 同内容，发布者应按删除合入 main 已修）
- **最小处置**：以 main 空预填、只提交用户填写字段为底；不传假 id/假证据/验收备注。

### GR-U03-003 — 前端 11 位大陆手机号正则（覆盖 M012-007）

- **文件:行号**：`OrganizationMerchantPage.tsx:25,153–156,453`
- **原文**：`contactPhoneInvalid: "联系电话必须是 11 位中国大陆手机号。"`；`if (!/^1[3-9]\d{9}$/.test(contactPhone.trim()))`；引导步同正则。
- **业务影响**：HTML 无电话格式闭集；座机/海外/分机无法建档。操作员对 M012-007 已答「删除」。
- **可复现**：源码 L153–156；main 测试已改为非空校验，对未发布页在创建流第一步即因预填电话偏离。
- **冻结判据**：GEN-017；GEN-004
- **2.3 依据类型**：4 可复现命令
- **四分类**：**添加**（HTML 沉默≠冲突；构成产品规则发明）
- **工程缺陷**：是。建议登记：是。发布前须修：是（B2 再问或按已答删除对齐 main）
- **最小处置**：改为非空校验，与当前 main 及后台 trimToNull 对齐。

### GR-U03-004 — 商户存档/删除写路径失败不可见，并覆盖 M012-008

- **文件:行号**：`OrganizationMerchantPage.tsx:201–238`
- **原文**：`archiveMerchant` / `requestDeleteMerchant` / `approveDeleteMerchant` 无 try/catch；对比 main `reportWriteFailure`。
- **业务影响**：接口失败成未处理 Promise 拒绝，页面无 `setGlobalError`，操作员以为成功。
- **可复现**：CMD-U03-MAIN-TEST「写路径失败可见」红（叠加 005 找不到按钮）；源码无 catch。无业务发明：纯错误处理缺口。
- **冻结判据**：GEN-009；GEN-002；BSM-003（标题级「数据可改可删」须可执行）
- **2.3 依据类型**：4 可复现命令
- **四分类**：**技术性**（无业务发明：不新增归档规则，只补失败可见）
- **无业务发明理由**：catch+setGlobalError 是运行时安全失败，不改变存档/删除业务口径。
- **工程缺陷**：是。建议登记：是。发布前须修：是
- **最小处置**：按 main M012-008 加 catch 与可失败测试；合并时保留 main 测试。

### GR-U03-005 — 自营动作把存档钮收进下拉，破坏已修测试合同

- **文件:行号**：`OrganizationMerchantPage.tsx:306–331`（`merchantActions` 在非存档行 push 自营，使 actions≥2 → `RowActions` 变 Dropdown）
- **原文**：`if (!row.archivedAt && row.deleteStatus !== "pending") actions.push({ key: "self-operated", ...})`
- **业务影响**：`data-testid="merchant-archive-m-1"` 默认不在 DOM；main 存档用例 1012ms `Unable to find`。不是自营业务本身的错，是落地方式破坏已有可失败断言。
- **可复现**：CMD-U03-MAIN-TEST 两例 archive testid。
- **冻结判据**：GEN-003；GEN-002
- **2.3 依据类型**：4 可复现命令
- **四分类**：**技术性**（无业务发明：下拉是 UI 编排，不新增「自营」定义——自营定义见 007）
- **无业务发明理由**：RowActions 收纳规则是既有组件行为，不是新的商户分类。
- **工程缺陷**：是。建议登记：是。发布前须修：是
- **最小处置**：发布后保持 archive testid 可直接查询，或同步改测试为先展开「操作」；不得未改测试就覆盖 main。

### GR-U03-006 — `selfOperated` 在当前 main 无类型、无后端路由

- **文件:行号**：`OrganizationMerchantPage.tsx:268,320–326`；数据流 `unpublished .../types.ts:1121`、`PlatformController.java:86–91`；main `apps/web/src/admin/types.ts` MerchantAdmissionRow 无该字段；main `backend` grep `setMerchantSelfOperated` = 0
- **原文**：`row.selfOperated`；`api.post("/api/v1/platform/merchants/" + id + "/self-operated", { selfOperated: !row.selfOperated, reason })`
- **业务影响**：只发本段 8 文件则 tsc 红、运行期点按钮 404。
- **可复现**：CMD-U03-TSC 4× TS2339；CMD-U03-TSC-U04 同批 types 后 tsc 0；main grep 0。
- **冻结判据**：GEN-008；GEN-002；GEN-013
- **2.3 依据类型**：4 可复现命令
- **四分类**：**技术性**（无业务发明：缺的是类型/路由同批，不是新的自营规则）
- **无业务发明理由**：字段已在未发布 U04/U05 定义；本条只登记跨段发布缺口。
- **工程缺陷**：是。建议登记：是。发布前须修：是
- **最小处置**：与 U04 types、未发布 PlatformController/Service、V408 同批；或从本页删除该动作直到后端在 main。

### GR-U03-007 — 自营标注无本段测试（细化项的测试缺口）

- **文件:行号**：`OrganizationMerchantPage.tsx:268,320–329`
- **原文**：抽屉「是（员工本人注册，经所属公司审批）/否（商户代提交，经域公司审批）」；prompt 事由后 POST。
- **业务影响**：HTML「在商户档案上标注为自营的商户视同公司自有，其员工按公司自有员工路径办理」有上位，属细化。本段 4 个测试文件 0 例点击 `merchant-self-operated-*`、0 例断言 POST body。
- **可复现**：`rg self-operated /tmp/u03-b1-tree/apps/web/src/admin/pages/*.test.tsx` 无命中；CMD-U03-SELF 16 例均不覆盖该按钮。
- **冻结判据**：GEN-003
- **2.3 依据类型**：4 可复现命令
- **四分类**：**细化**（上位：BSM 员工注册条 / PGM-578「商户档案上标注为自营」；本段冻结 ID 用 GEN-003 管测试，不把未映射 BSM 员工条写成冲突 ID）
- **工程缺陷**：是。建议登记：是。发布前须修：是
- **最小处置**：补能失败用例：空事由不提交；填事由 POST `{selfOperated, reason}`；不把提交当即改合作方式六类。

### GR-U03-008 — 日期/月份默认用 UTC 日历日

- **文件:行号**：`StaffShiftAttendancePage.tsx:41–43,50`；`StaffPayrollLeavePage.tsx:31–33,48`
- **原文**：`return new Date().toISOString().slice(0, 10);` / `.slice(0, 7)`
- **业务影响**：UTC+8 每日 00:00–08:00 排班日期、每月 1 日 00:00–08:00 薪酬月份落到前一自然日/月。考勤「当地时间」在未映射子条；可执行错误仍成立。
- **可复现**：`TZ=Asia/Shanghai node -e 'const d=new Date("2026-08-01T00:30:00+08:00"); console.log(d.toISOString().slice(0,10), d.toISOString().slice(0,7))'` → `2026-07-31` / `2026-07`。
- **冻结判据**：GEN-002
- **2.3 依据类型**：4 可复现命令
- **四分类**：**技术性**（无业务发明：日历日取值错误，不新增时区规则）
- **无业务发明理由**：用本地日历日替代 UTC slice 是实现正确性，不发明「营业日」。
- **工程缺陷**：是。建议登记：是。发布前须修：是
- **最小处置**：按本地（或项目当地）日历格式化；与未发布 `LocalZone` 后端对齐。

### GR-U03-009 — 申请修改排班不校验事由

- **文件:行号**：`StaffShiftAttendancePage.tsx:260–264,322–329,137–153`
- **原文**：打开编辑 `setNote("")`；步 2 description「修改排班必须填写原因」；`validate` 仅在步 1 校验员工/时间；`saveShift` 无 `editingShift && !note.trim()` 分支。未发布 `draftShift` 对已有班 `requireReason(request.note())`。
- **业务影响**：引导声称必填，确认步可提交空事由，到后端才 400。BSM-003 标题「操作有引导」——当步应拦住。
- **可复现**：源码对照；本段无修改排班测试。
- **冻结判据**：GEN-002；BSM-003
- **2.3 依据类型**：4 可复现命令（源码）
- **四分类**：**技术性**（无业务发明：HTML 已要求修改填原因，缺口是引导未执行声称）
- **无业务发明理由**：不新增「必须填原因」规则，只让 UI 与已有后端/文案一致。
- **工程缺陷**：是。建议登记：是。发布前须修：是
- **最小处置**：编辑态步 2 validate 要求非空事由；补一条能失败测试。

### GR-U03-010 — 单班点位只按英文逗号拆，与批量/注释不一致

- **文件:行号**：`StaffShiftAttendancePage.tsx:27–30,151` vs `179,186`
- **原文**：`splitList` 认 `[,，\s]+`；`saveShift` 用 `points.split(",")`。
- **业务影响**：注释写「从名单里粘一串进来也认」；单班中文逗号变成一个非法 space_id。
- **可复现**：源码；批量测试用英文逗号故 16/16 仍绿。
- **冻结判据**：GEN-008；GEN-002
- **2.3 依据类型**：4 可复现命令（源码）
- **四分类**：**技术性**（无业务发明：同一输入的拆分函数不统一）
- **无业务发明理由**：不新增点位语法，只复用已有 `splitList`。
- **工程缺陷**：是。建议登记：是。发布前须修：是
- **最小处置**：`saveShift` 改 `splitList(points)`；补中文逗号用例。

### GR-U03-011 — 展示文案夹杂 `user_id` / `space_id`

- **文件:行号**：`StaffShiftAttendancePage.tsx:316,327,363,389`；`StaffPayrollLeavePage.tsx:253,266`
- **原文**：可见 label「员工 user_id」「点位(space_id 逗号分隔)」。
- **业务影响**：违反 GEN-015 与项目「展示文字全中文」（系统内部标识符可出现在单据号等四处，不作为表单标签）。
- **可复现**：打开新增排班/薪资基数弹窗即见。
- **冻结判据**：GEN-015
- **2.3 依据类型**：4 可复现命令（源码/DOM）
- **四分类**：**技术性**（无业务发明：文案语言，不新增字段）
- **无业务发明理由**：改成「员工账号编号」「点位编号」不改变请求体。
- **工程缺陷**：是。建议登记：是。发布前须修：是
- **最小处置**：标签全中文；testid/请求字段保持英文码。

### GR-U03-012 — 人事三页人员/商户标识不可点击溯源

- **文件:行号**：`StaffProfilesPage.tsx:165–168`（姓名/手机/merchantId/userId 纯文本）；`StaffPayrollLeavePage.tsx:282`；`StaffShiftAttendancePage.tsx:254,287`
- **原文**：`<td>{row.merchantId ?? "平台"}</td>` 等，无 `TraceLink` / 无整行抽屉（档案页）。
- **业务影响**：PGM-001「页面上每个名称、编号、金额、状态、人员，都能点开看它的明细和来源单据」「不允许出现无法点击溯源的孤立数据」。商户页已有抽屉范式，人事页未接。
- **可复现**：源码；档案页无 RowDetailDrawer。
- **冻结判据**：PGM-001
- **2.3 依据类型**：1 HTML 原文
- **四分类**：**细化**（见名可点是 PGM-001 已有原则的下位实现缺口，不是新原则）
- **工程缺陷**：是。建议登记：是。发布前须修：是
- **最小处置**：商户 ID 用 TraceLink 到 ADM-GOV-002；人员/薪资/排班行接已有抽屉或 TraceLink；客人查询通道不适用于员工档案（员工不是客人）。

### GR-U03-013 — 员工档案始终请求 include_archived=true

- **文件:行号**：`StaffProfilesPage.tsx:45–52,139`
- **原文**：`api.get("/api/v1/staff/profiles?include_archived=true")`；checkbox 只过滤 `visibleRows`。
- **业务影响**：未勾选「显示已存档」仍把已存档员工（掩码手机号仍是 PII 残留）下载到浏览器。后端默认 false。
- **可复现**：源码 L51 vs L139。
- **冻结判据**：GEN-010；GEN-005（最小拉取）
- **2.3 依据类型**：4 可复现命令（源码）
- **四分类**：**技术性**（无业务发明：不改变「可显示已存档」规则，只把过滤放到请求参数）
- **无业务发明理由**：勾选后再带 `include_archived=true` 与后端参数同名，不新增归档语义。
- **工程缺陷**：是。建议登记：是。发布前须修：是
- **最小处置**：默认 false；勾选后再 reload。测试「显示已存档后可以申请删除」改为先断言未请求 archived、勾选后请求。

### GR-U03-014 — 奖惩录入与月结无错误处理、奖惩成功不刷新列表

- **文件:行号**：`StaffPayrollLeavePage.tsx:119–136`
- **原文**：`saveAdjustment` / `closeMonth` 无 try/catch；`saveAdjustment` 不 `reload()`。
- **业务影响**：失败未处理拒绝；成功列表仍旧，操作员以为没入账。基线已有，未发布未修；全量审文件仍成立。首次点「执行月结」遇 403/500 即发生。
- **可复现**：源码；本段无月结/奖惩测试。
- **冻结判据**：GEN-009；GEN-002
- **2.3 依据类型**：4 可复现命令（源码）
- **四分类**：**技术性**（无业务发明）
- **无业务发明理由**：catch+reload 不改变月结/奖惩口径。
- **工程缺陷**：是。建议登记：是。发布前须修：是
- **最小处置**：与薪资基数保存同样 try/catch；成功 reload。

## 8. 观察（无新根因，完整登记，建议队列否）

1. **OBS-01**：开工提示 HEAD=bd445728；实算 origin/main=445a49f4（#429 M011–M012 检查点，bd445728 之祖先）。对照已修项用 445a49f4。
2. **OBS-02**：commit 对象 `03acdb98` 本仓不存在；tree `e28dad05` 可解析。与 U01/U02 相同事实。
3. **OBS-03**：本段自测 16/16 绿，不能替代 001–014。席甲 ledger 写商户页「1 失败/11 通过」；本席 overlay 为 6 失败/9 通过（15 例）。互盲，不以席甲数字为准。
4. **OBS-04**：扣除上限文案含顶层确认，表单只采三审人。HTML 要求顶层最终确认。可能由审批链补全（U05/U06）。不因沉默升冲突。
5. **OBS-05**：排班 KPI 注释承认平台口径与项目过滤列表不同。分析端点框架级边界，首次打开显示「—」或平台数，不单独开缺陷。
6. **OBS-06**：`usr_master_authority` 客户端判断与未发布 `isMasterAuthority` 同约定；删除不是只靠藏按钮。
7. **OBS-07**：若先合 main 六类（含合作方式 `self_operated`）再叠 `is_self_operated` 布尔，会出现两个「自营」旋钮。要两步才成立，铁律 6 不升。发布说明里应二选一或强制同步。
8. **OBS-08**：打卡方式展示「NFC」英文。HTML 亦写 NFC。不升。
9. **OBS-09**：幂等键 `Date.now()` 连点同毫秒。铁律 6 收窄。
10. **OBS-10**：GEN-014 本段无迁移，无实现面。V408 在未发布其它段。
11. **OBS-11**：员工五态与 BEQR-240 同文（待入职/在职/试岗/停岗/离职）。BEQR-240 非本段冻结 ID，作对照：非发明。
12. **OBS-12**：请假扣薪文案相对基线改为工时比例，与 HTML 请假扣薪条同向，属纠偏。
13. **OBS-13**：排班改为提交上一级审批，与 HTML「排班表最终发布须经该主管的上一级审批」同向。
14. **OBS-14**：TraceLinkP2c 5/5 在未发布商户页上仍绿（下钻 focus 未坏）。
15. **OBS-15**：`METHOD_LABELS.manual="历史手动记录"` 不是新的打卡入口，本页无补打卡表单。
16. **OBS-16**：ponytail：`isMaster` 写死用户名可改为调后端已有 `isMasterAuthority` 语义的会话字段；不落地。
17. **OBS-17**：HTML 正本未发现需人闸修改的原文错误。
18. **OBS-18**：未发布 `StaffNumericSetting` 默认可见天数 7、上限 bps 2000，与 HTML 出厂 7 天 / 20% 同向。

## 9. 添加项清单（供主调度按 2.2 处理，不代用户回答）

| 临时 ID | 一句话 | HTML 能否指向上位 | 建议问法（B2 写 questions，B1 不发问） |
|---|---|---|---|
| GR-U03-002 | 创建预填验收商户名/联系人/电话及假主体/假证据/面积租金 | 否 | 删除还是保留？M012-006 已答删除 |
| GR-U03-003 | 联系电话 11 位大陆正则 | 否 | 删除还是保留为产品规则？M012-007 已答删除 |

其余 finding 不是添加。自营标注（007）已指向 HTML「商户档案上标注为自营」，不进 2.2。

## 10. 结论

**FAIL**

不满足单席 PASS：存在未定稿工程缺陷（001–014）；添加项未答（002、003）；发布会覆盖 main 已修 M009-003/M012-006/007/008。无条件通过。无未闭合待问却 PASS。

发布者最小路径（本轮不修）：以 HEAD `445a49f4` 的 OrganizationMerchantPage 为底三方合并自营标注；人事七文件可快进但须同批 U04/U05/U06/V408；修 008–014 的可执行缺口；B2 对 002/003 按 2.2 处理（内容与已答删除的 M012 项重合）。

完成判据（本 raw）：八文件覆盖表齐全；21 条判据均有顺从式结论；三线已执行；每条 finding 含临时 ID/行号/原文/影响/命令/冻结 ID/依据类型/四分类/处置；第一行为独立实算锁哈希且正文无第二处独占同名行。

## U03 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 49874 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，独立）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02 §7/§11；criteria-lock V0.11；modules.md V0.04 附录 U03
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`（PR #429 已含 M009-003 与 M012-006/007/008）。人事六文件 `HEAD` blob = 基线 `242f4872`；`OrganizationMerchantPage.tsx` 三方 blob 不等（基线 `43271407` / 未发布 `7e951704` / HEAD `7d5b3ee9`）。
- 审计对象仍是只读输入副本 `.scratch/module-recheck-20260908/unpublished-03acdb98`（树 `e28dad059b2c7e08bcc95a8db18552d3f76edf4b`，基线 `242f487233d028173cb9198eecd079f5d877405c`），不是 main。三份 HTML 取 main 正本。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B2 核实结论。
- 本席 B2-r1：**PASS**（001–015 逐 ID 全 PASS；添加 2 沿用 M012-006/007 已答「删除」，不为本段再问 2.2；发布前须修「是」=001–009，「否」=010–015）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。映射行 M012/M057/M081/M290/M293 闭集相同，COUNT=21 unique=21 MISSING=[]（GEN-001–018、BSM-001、BSM-003、PGM-001）。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要核对只读（副本 8/8 哈希、blob、`git diff -U0`、HTML 锚点、`/tmp/u03_vitest_head445a.log`）；未重跑 vitest、未装依赖、未碰 `complex_test*`。

此票只判断 findings.md V1.48 顶部「当前 U03 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、「发布前须修」是/否与最小处置。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。材料未发布，定稿后状态应记 REVIEWED-FAIL（有已定稿缺陷，处置写给发布者），不做 B3。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/U03-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 49874 | `28dcd8efd729c7839e4cfd33e6c551df1645ffbcb08a3643d43727c6f8ebb1b5` |
| `docs/audit-2026-09/modules/U03-b1-self-raw.md` | V0.01 | 35075 | `ba66667d5b5783337fd5cd10c0c6bb22e64d3c70d7687a6acfc9a7a3f2655650` |
| `docs/audit-2026-09/findings.md` | V1.48 | 562893 | `0edcf3e313ad7f3fcb5295dd7c7ae1d77e097f0a35245f69db96f8e1dae50c7d` |
| `docs/audit-2026-09/ledger.md`（只读） | V2.50 | 465618 | `4830472ac7e7bdae85535b1be97c870d5c97decde47ab45708b2e864068b4bea` |

候选形态：V1.48 顶部 U03-001…015；questions 0；无 HTML 修改申请。U03 ledger 行 IN_PROGRESS。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

HTML 正本哈希与 B1 一致：BEQR `4396bb71…1d66`、BSM `a7f7b7a3…66fa`、PGM `c4238afd…e71c`。副本八文件 sha256/行/字节与树 `e28dad05:<path>` blob 8/8 全等（本轮复算）。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers | 先核技能；用户/提示词覆盖技能本体 |
| receiving-code-review | 先核 HTML/diff/blob/grep/已答 2.2 再接受或反驳候选；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：003/004 是否必须再问、002 是否越界发明旋钮关系、001 是否最小、006/007 是否发明业务、008–014 分界、015 是否写错 §8 |
| verification-before-completion | 声称前先跑 sha256/git rev-parse/diff/grep/HTML sed；本票不声称新的 vitest 绿 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流（8 文件→U04 types / U08 Platform* / U10 V408→main 对照→HTML） |
| ponytail-review | 只评最小删除/收窄，不落地。003/004 不重开弹窗；002 不强制产品二选一；001 不另造幂等方案 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到**；未冒充调用 |
| Arc audit | 会派子代理；与双席冲突；**未用** |

### 完整差集（GR-001…014、§8 OBS-01…18、§9 添加 GR-002/003；席甲 CX-001/002、OBS-1…7）

漏并：无。两席 B1 全部临时 ID 仍落在 001–015。席甲把 CX-002 整文件分叉捆成一条「技术性」，候选按本席拆成 002 冲突 / 003 添加 / 004 添加 / 005 技术性，不丢原意。席甲自称无添加项，候选取本席 GR-002/003 的添加分类，不照抄席甲。席甲 OBS-4 升入 009 是因为本席 GR-009 已是缺陷，不是把观察当缺陷发明。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/须修 | 本席票 |
|---|---|---|---|
| U03-001 | CX-U03-001 + GR-U03-007（测试缺口） | 技术性/是/是 | **PASS（技术性/是/是）** |
| U03-002 | GR-U03-001 + CX-U03-002（分叉面） | 冲突（main 已修 M009-003）/是/是 | **PASS（冲突 main 已修/是/是）** |
| U03-003 | GR-U03-002 + CX-U03-002 | 添加（操作员已答删除＝M012-006）/是/是 | **PASS（添加已答删除/是/是；不再问）** |
| U03-004 | GR-U03-003 + CX-U03-002 | 添加（操作员已答删除＝M012-007）/是/是 | **PASS（添加已答删除/是/是；不再问）** |
| U03-005 | GR-U03-004 + CX-U03-002 | 技术性（main 已修 M012-008）/是/是 | **PASS（技术性 main 已修/是/是）** |
| U03-006 | GR-U03-005 | 技术性/是/是 | **PASS（技术性/是/是）** |
| U03-007 | GR-U03-006 | 技术性（跨段同批）/是/是 | **PASS（技术性/是/是）** |
| U03-008 | GR-U03-013 | 技术性/是/是 | **PASS（技术性/是/是；新增代码）** |
| U03-009 | GR-U03-009 + 席甲 OBS-4 | 技术性/是/是 | **PASS（技术性/是/是；新增代码）** |
| U03-010 | GR-U03-008 | 技术性（main 既有）/是/否（归 M290/M293） | **PASS（技术性 main 既有/是/否）** |
| U03-011 | GR-U03-010 | 技术性（main 既有）/是/否（归 M290） | **PASS（技术性 main 既有/是/否）** |
| U03-012 | GR-U03-011 | 技术性（main 既有）/是/否（归 M290/M293） | **PASS（技术性 main 既有/是/否）** |
| U03-013 | GR-U03-012 | 细化（main 既有）/是/否（归 M057/M290/M293） | **PASS（细化 main 既有/是/否）** |
| U03-014 | GR-U03-014 | 技术性（main 既有）/是/否（归 M293） | **PASS（技术性 main 既有/是/否）** |
| U03-015 | 双方观察归并 | 登记/否/否 | **PASS（登记/否/否）** |

冲突入队 1（002）。添加入队 2（003、004，已答删除）。技术性入队 6（001、005、006、007、008、009）。main 既有 5（010–014）。登记观察 1（015）。questions 0。

发布前须修「是」=001–009（9）；「否」=010–015（6）。与候选汇总一致。

测试计数勘误（本批锁定）：席甲 B1 §2/§4 所记 main 测试×副本页「1 失败/11 通过」是 bd445728 测试版本。本席 B1 overlay 为 6 失败/9 通过（15）。席甲 B2 用 `git show HEAD:…OrganizationMerchantPage.test.tsx`（445a49f4）在 `/tmp/u-overlay` 重跑，日志 `/tmp/u03_vitest_head445a.log` 原文 `Tests  6 failed | 9 passed (15)`，失败六条＝真实字段渲染、六类合作方式中文、创建流、抽屉合作形式中文、存档理由拦截、写路径失败可见。本批以 **6/15** 为准。

### 重点 (a) 添加项 003/004 不再问 2.2 — **PASS（合规，不必再问）**

本席 B1 把 GR-002/003 标添加，并写「B2 须逐条发问」与「B2 再问或按已答删除对齐 main」两句。本轮独立核：

- 未发布 `OrganizationMerchantPage.tsx:68–73/:159–178` 仍预填「二期测试商户/运营联系人/13800000000」、请求夹带 `org-1` / `contract-subject-1` / `att-merchant-profile-create` / 面积租金常量 / `status: "active"`；`:25/:153–156/:453` 仍是 `/^1[3-9]\d{9}$/`。
- 同文件同代码在 M012 已按 2.2 发问：`questions/M012-006.md`、`questions/M012-007.md` 操作员 2026-09-09 弹窗答复首行均为精确「删除」，PR #429 已在 HEAD 执行（空预填、电话非空、不传假主体）。
- 副本早于该修复（未发布 blob `7e951704` ≠ HEAD `7d5b3ee9`）。HTML 仍无这些默认值/格式闭集 → 分类保持**添加**，不是改判成技术性。

2.2 问的是产品规则发明，不是审计段编号。同一发明已答「删除」并落地，为本段再弹一次是重复发问，不是冻结流程要求。候选登记「添加（操作员已答删除）」、发布前须修**是**、处置「以 main 为底合并，不再问 2.2」：分类诚实、须修挡住整文件覆盖回退、处置不发明主体选择器（避开本席在 M012-006 否决过的「三选一必填」）。票 **PASS**。禁止：另写 questions/U03-003.md、把添加改成技术性从而抹掉「已答删除」痕迹、用未发布整文件盖 HEAD。

### 重点 (b) U03-002 三码与分叉面 — **PASS（成立，不越界）**

GR-001 原意：三码收费模板对抗 HTML 六类（BSM L701 / PGM L495 明文「自营、租赁、联营、代运营、服务商接入、其他」），表内 `lease` 原文，对当前 main 白名单 `MERCHANT_COOPERATION_FORM_INVALID`。四分类**冲突**，须修是。CX-002 把整文件分叉捆成技术性，覆盖过宽；候选把冲突本体留给 002、把预填/正则/catch 拆到 003–005，是拆捆不是改写。

「冲突（main 已修 M009-003）」≠ U01 的「冲突（main 既有）」：前者是 main 已修、未发布仍旧、发布会回退，故须修**是**；后者是 main 同位置已有、材料未加重，故须修否。分界成立。

处置「以 main 为底，弃三码」= GR-001 最小句，不发明第七类。后半「自营标注为独立布尔并与合作方式『自营』关系在发布说明中二选一或强制同步（OBS）」是本席 OBS-07 原文，标了 OBS，不升独立缺陷、不把二选一写成 B3 必改产品闭集。铁律 6 仍成立。不越界。票 **PASS**。

### 重点 (c) U03-001 缺幂等键 + 无测试 — **PASS（并列最小）**

副本 `PlatformController.java:86–91` `requireIdempotencyKey`；DTO `MerchantSelfOperatedRequest(idempotencyKey, selfOperated, reason)`。页面 `:325` POST 体只有 `{selfOperated, reason}`。同页创建/存档已用 `merchant-…-${Date.now()}`。本段 4 个测试文件 0 例点 `merchant-self-operated-*`。首次点「标记为自营」打未发布后端必 400；打当前 main 则 404（007）。

GR-007 的「细化」指自营**标注能力**有 HTML 上位（PGM L578 / BSM L1433），不是把缺测试当成产品规则。合并后分类取**技术性**覆盖缺键（GEN-007/008）+ 测试缺口（GEN-003），不把自营标注改判为添加或冲突。处置=加同页 Date.now() 幂等键 + 空事由不提交 / 填事由 POST 体可失败用例，是同页已有模式，不另造幂等服务。票 **PASS**。

### 重点 (d) U03-006/007 — **PASS（技术性、须修是、最懒、不发明业务）**

006：非存档行 push 自营后 `actions≥2`，`RowActions` 变下拉，`merchant-archive-m-1` 不在文档。445a49f4 测试 15 例中存档空理由与写路径失败可见两条因此 1011ms 找不到按钮（日志原文）。无业务发明：下拉是既有组件收纳，不是新的商户分类。处置「保持 archive testid 可直接查询，或同步改测试先展开操作」是二选一最小，禁止未改测试就覆盖。票 **PASS**。

007：页面读写 `selfOperated`；HEAD `types.ts` 0 命中；HEAD Java 0 处 `setMerchantSelfOperated`；U04 `types.ts:1121` 有字段；U08 有控制器/服务/DTO/`JdbcPlatformRepository` 列；U10 `V408__staff_registration_company_routes.sql` 原文 `ALTER TABLE merchants ADD COLUMN is_self_operated BOOLEAN NOT NULL DEFAULT FALSE`。只发本段 8 文件则 tsc 4× TS2339、运行期 404。缺的是同批，不是新的自营规则。处置「与 U04/U08/U10 同一 PR，否则先移除该动作」是 GR-006 原文。票 **PASS**。

### 重点 (e) 008/009 新增 vs 010–014 main 既有 — **PASS（分界正确，含 GR-008）**

沿用 U01 定稿口径：main HEAD 同位置已有、材料 diff 未触及缺陷本体 → 技术性/细化（main 既有）、发布前须修否、归主模块；材料不得加重。本席按「缺陷本体是否出现在 `git diff -U0 242f4872 e28dad05` 的 +/- 中」独立核：

| 项 | 命令/事实 | main HEAD | 未发布 | diff 是否触及缺陷本体 | 加重？ |
|---|---|---|---|---|---|
| 008 `include_archived=true` | 基线 GET `/staff/profiles`（无 query）；副本 `:51` `?include_archived=true` | 无该 query | 有 | **+ 行引入** | 是（新代码）→ 须修是 |
| 009 修改排班无事由校验 | 基线/HEAD 无 `editingShift`；副本 `:260–264` `setNote("")`、`:313` validate 只拦员工/起止、`:324` 声称必须填原因 | 无该路径 | 新增「申请修改」 | **新增代码** | 是 → 须修是 |
| 010 UTC `today`/`currentMonth` | 三方函数体逐字相同 `toISOString().slice(0,10/7)`；人事页 HEAD blob＝基线 | 有 | 同文（行号平移） | payroll diff 0 处 `toISOString`；shift U0 仅新增 `inputTime`（datetime-local 纠偏），**未改 `today()` 本体** | 否 → 须修否，归 M290/M293 |
| 011 `points.split(",")` vs `splitList` | 基线 `:144` 已是 `split(",")`；副本 `:151` 同 | 有 | 同 | `points.split` 不在 +/- | 否 → 须修否，归 M290 |
| 012 `user_id`/`space_id` 标签 | 基线/HEAD 同文；副本标签同，仅给编辑态加 `disabled` | 有 | 同文 | +/- 只加 disabled，未新造英文标签；薪酬页 `user_id` 不在 payroll diff | 否 → 须修否，归 M290/M293 |
| 013 见名可点 | 基线档案 `merchantId` 已是纯文本、无 TraceLink；HEAD blob＝基线 | 有 | 新增列（手机号等）仍纯文本 | 未引入 TraceLink，也未把原可点改不可点 | 否（未加重根缺陷）→ 须修否，归 M057/M290/M293 |
| 014 奖惩/月结无 catch | 基线 `saveAdjustment`/`closeMonth` 无 try/catch，奖惩不 reload；副本 `:119–136` 同构 | 有 | 同 | payroll U0 不触及这两函数 | 否 → 须修否，归 M293 |

GR-008：本席 B1 按全量审文件写了须修是，未在 finding 正文写「pre-existing」四字；函数体三方全等、diff 未改 UTC 实现，B2 适用 U01 口径改为须修否。候选据此归 main，正确，不是漏修。008/009 是本材料新增，须修是。分界成立。票 **PASS**。

### 重点 (f) U03-015 观察 — **PASS（未写错本席 §8）**

015 列席甲 OBS-1（`isMaster` 字面）、OBS-2（英文结果键归 M293）、OBS-3（legend）、OBS-5/6/7，并以「Grok OBS-01…18」收本席观察，点名 OBS-03 两席计数（勘误 445a49f4 测试 6/15）与 OBS-07 两个自营旋钮，以及 OBS-11/12/13/18 与 HTML 同向。席甲 OBS-4 已并入 009，不在 015 重复。未把本席 001–014 任一 finding 降成观察，也未把 §8 升格入队。OBS-03 写的是本席原 6/15，不是席甲 B1 的 1/11。票 **PASS**。

定稿时 015 不必把 §8 逐条抄全；不得写成「Grok 无观察」或把两个自营旋钮升成独立须修 ID。

### 001 独立裁定（技术性／须修是）

见 (c)。缺键使细化能力不可用；测试缺口并列。最小＝同页 Date.now() + 可失败用例。票 **PASS**。

### 002 独立裁定（冲突 main 已修／须修是）

见 (b)。首次用当前 main 后端提交默认 `fixed_rent_plus_commission` 即拒。以 HEAD 页为底弃三码。票 **PASS**。

### 003 独立裁定（添加已答删除／须修是／不再问）

见 (a)。票 **PASS**。

### 004 独立裁定（添加已答删除／须修是／不再问）

见 (a)。HTML 无电话格式闭集。票 **PASS**。

### 005 独立裁定（技术性 main 已修／须修是）

副本 `:201–238` `archiveMerchant`/`requestDeleteMerchant`/`approveDeleteMerchant` 无 try/catch（席甲 B1 GEN-009「新增写路径均 catch」对存档旧路径不成立；自营新路径 `:324–328` 有 catch，不在本条）。HEAD 已有 `reportWriteFailure`。无业务发明。票 **PASS**。

### 006 独立裁定（技术性／须修是）

见 (d)。票 **PASS**。

### 007 独立裁定（技术性／须修是）

见 (d)。U08＝第 8 段 PlatformController/Service/DTO/Jdbc/MerchantRecord；U10 V408 含 `is_self_operated` 列。票 **PASS**。

### 008 独立裁定（技术性／须修是）

见 (e)。未勾选仍把已存档档案（含脱敏手机）拉到浏览器。默认 false、勾选再 reload。票 **PASS**。

### 009 独立裁定（技术性／须修是）

见 (e)。文案声称必填，确认步可交空事由，到后端 `requireReason` 才 400。补步 2 validate + 一条可失败用例。票 **PASS**。

### 010 独立裁定（技术性 main 既有／须修否）

见 (e)。UTC+8 00:00–08:00 落前日/月在 main 已存在。归 M290/M293。票 **PASS**。

### 011 独立裁定（技术性 main 既有／须修否）

见 (e)。票 **PASS**。

### 012 独立裁定（技术性 main 既有／须修否）

见 (e)。薪酬页另有 `:260` `{ label: "员工 user_id" }`，同一条，不另开 ID。票 **PASS**。

### 013 独立裁定（细化 main 既有／须修否）

PGM L276「不允许出现无法点击溯源的孤立数据」。根缺陷在基线人事页。本材料新增列仍纯文本，未加重到须把本段当发布门。归 M057/M290/M293。票 **PASS**。

### 014 独立裁定（技术性 main 既有／须修否）

见 (e)。票 **PASS**。

### 015 独立裁定（登记／否）

见 (f)。票 **PASS**。

### 分类 / 队列 / 给发布者的最小边界

本批可定稿（须席甲本批亦 PASS）。发布前须修是=001–009。否=010–015。添加 2 已答删除，不进 BLOCKED-ASK，不新开 questions。

发布者最小集（不在本轮改源）：

- 以 HEAD `445a49f4` 的 `OrganizationMerchantPage.tsx` / `.test.tsx` 为底三方合并，只迁自营标注 12 行（抽屉字段 + 行动作）；禁止用未发布整文件覆盖（否则回退 M009-003 与 M012-006/007/008）。
- 001：自营 POST 加同页 `idempotencyKey`（`merchant-self-operated-…-${Date.now()}`），补空事由不提交与 POST 体可失败用例。
- 002：弃三码，沿用 main 六类中文 option 与可空未选项。两个「自营」旋钮写进发布说明（OBS-07），不在本 ID 发明同步规则。
- 003/004：沿用 main 空预填与电话非空；不传假主体/假证据；不再问 2.2。
- 005：沿用 main `reportWriteFailure` 与 15 例合同。
- 006：archive testid 可直接查询，或测试先展开「操作」。
- 007：U04 types + U08 控制器/服务/Jdbc + U10 V408 同 PR；否则先从本页拿掉该动作。
- 008：档案列表默认不带 `include_archived`，勾选再 reload。
- 009：编辑态步 2 非空事由 + 一条可失败用例。
- 010–014 不在本材料关门，记入 M057/M290/M293。
- 人事七文件可相对基线快进，但须与 U05/U06 员工后端同批（新排班审批/上限/可见天数路由）；U08 只绑商户自营，不必写成人事七文件的同批条件。
- 禁止：整树覆盖 main；为本段再弹 M012-006/007 同问；把 010 UTC 改回本段发布门；把 OBS-07 升成独立须修 ID。

非阻断勘误（不定稿作废，不必 r2）：

1. 001 处置可补一句「POST 不断言把合作方式改成六类之一」（GR-007 原句，防与 OBS-07 混，不另开 ID）。
2. 002 处置后半保持 OBS，不要读成冲突本体强制产品二选一。
3. 012 薪酬页 `:260` 与 `:253/:266` 同条。
4. 候选「共同处置」把 U08 写进人事七文件同批，范围略宽；U08 只服务 002/006/007 的自营标注。
5. 本席 B1 GR-008 未写「pre-existing」字样；B2 按函数体三方全等归 main 既有，与 U01 口径一致。

### 结论

全批差集无漏并。003/004 沿用已答「删除」合规，不必再问。002 冲突成立、不越界。001 缺键+测试并列最小。006/007 技术性最懒。008/009 新增须修、010–014 确为 main 既有（含 UTC）。015 未写错 §8，OBS-03 以 6/15 为准、OBS-07 两个旋钮在册。逐 ID 001–015 全 **PASS**。本席 B2-r1 **PASS**。未经双 PASS 不得把候选当定稿，更不称 U03 或 main 模块 PASS。步骤仍 B2；U03 IN_PROGRESS；材料未发布，不做 B3。
