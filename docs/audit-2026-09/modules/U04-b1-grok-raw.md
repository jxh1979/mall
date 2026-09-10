CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U04 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 只读全量三线（未发布材料第 4 段；不做 B3/B4/B5；处置写给发布者，本轮不修源）
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 段：U04 — 未发布材料第 4 段「员工网页和组织网页」
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 末尾附录；`plan/runbook.md` V0.02 §11；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 审计对象（不得冒称 main 或已发布修复）：
  - 未发布提交：`03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb`（本仓 `git cat-file -t` **无此 commit 对象**）
  - 未发布树：`e28dad059b2c7e08bcc95a8db18552d3f76edf4b`（`git cat-file -t` = **tree**，可解析）
  - 发布基线：`242f487233d028173cb9198eecd079f5d877405c`（commit，可解析）
  - 只读输入根：`/home/james/complex-management/.scratch/module-recheck-20260908/unpublished-03acdb98`
  - 清单：`.scratch/module-recheck-20260908/unpublished-03acdb98-input.json` V0.01，76 文件
- 本段：1 文件，物理行 1301，标记 NORMAL；main 主归属 M365
- 冻结判据：M365 映射行并集 21 项（与锁行并集相同；COUNT=21 unique=21 MISSING=[]）：GEN-001…GEN-018、BSM-001、BSM-003、PGM-001
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 且发布前须修：GR-U04-001、GR-U04-002
- 添加项（B1 只登记，不代用户回答 2.2；B2 须逐条发问）：GR-U04-001
- 只登记、建议队列「否」：观察见 §8
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做写操作
- 本报告不表示全仓、main 模块或发布门禁通过。M001–M010 检查点已合并；M011–M012 检查点已合入当前 HEAD（见 §1）。原 322/471 模块结论不继承到本未发布段。不得把本副本冒称 main 源码或已发布修复。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（`/tmp/u04-b1-mini` tsc 探针 + `/tmp/u04-b1` overlay tsc）再下结论；不改生产代码 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流（本段 types.ts → U03 页 / U05 StaffRegistration / U08 Platform* / U10 V408 → main 对照 → HTML） |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；未跑的后端集成 / 共享库测试不写通过 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本段验证均秒～二十秒级，未起 monitor |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：套件文档写明已并入 `grilling`，无独立技能可装
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
  - `M365 →` L6744 闭集含 BSM-001/BSM-003/GEN-001–018/PGM-001
  - 并集 COUNT=21 unique=21 MISSING=[] EXTRA=[]，与开工提示闭集相同
- `plan/modules.md` 附录 U04 单路径 `apps/web/src/admin/types.ts` 1301 行、main 归属 M365，与用户清单逐字相同

HEAD（独立实算，**与开工提示中的 bd445728 不一致**）：

- `git rev-parse HEAD` = `git rev-parse origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`
- `git log -1 --oneline`：`445a49f4 Merge pull request #429 from jxh1979/audit/20260909/M011-M012-checkpoint`
- 提示所称 `bd445728a6ec7bb36a9e7ff19fe56927410ac61e` 存在且为祖先（`merge-base --is-ancestor` = YES；该 commit 为 M001–M010 检查点 PR #428）
- 本席对照 main 已修项时以 **当前 HEAD=445a49f4** 为准（含 M011–M012 已合入修复）
- 本席未设 `GIT_INDEX_FILE`，未动主索引

ledger 只读核验（不改）：V2.52，468023 bytes，SHA256 `bd82e9fdc517c3d1af45dde89a17b9f18692a583c0c8f9a83357202edafa74df`（与开工提示一致）。findings V1.49，565641 bytes，SHA256 `2ca8751dd5731c9406c0f58b23965d2c254fdeffde31b07327978b5220d6797c`（与开工提示一致）。台账中与本段同名文件直接相关的已定稿项：

- M009-003：合作方式六类技术码 `self_operated/lease/joint_operation/managed_operation/service_provider/other`（改的是 `PlatformService` + main `OrganizationMerchantPage.tsx`，**未改** `types.ts`）
- M012-001：`SubjectBindingsPage` 改读真实键；明确 `types.ts` 不在 M012 六文件，臆造 optional 仍留在本文件
- M012-003：空间 `venueId` 为 types.ts 已标旧臆造字段
- U03-002：未发布商户页三码合作形式 vs main 六类（冲突，main 已修）
- U03-007：`selfOperated` 须与 U04 types / U08 控制器 / U10 V408 同批（本段是该同批的类型半边）

本席独立对照见 §6，不继承其 PASS。

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

三 HTML 只读、未改。锁内本段用到的锚点行对本席读取的现行 HTML **全部命中**（见 §1.1）。三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。BEQR「合作模式」、BSM L701/L1433、PGM L495 不在本段冻结闭集时仅作数据流对照，不作为 finding 支撑 ID；本段冻结业务 ID 仅 BSM-001 / BSM-003 / PGM-001。BSM-001 原文含「合作模式有哪几种」属口径本身不做后台可配，可支撑本段对「第二自营轴」的分类。

清单本段条目与独立实算 1/1 相同：`path=apps/web/src/admin/types.ts` sha256 `0b82f6c2ca2422fa6ed1cc28ed54824d73539a24eb9ca001793d7b6a2c49540c` lines=1301 bytes=34127。副本 `git hash-object` = 树 `e28dad05:apps/web/src/admin/types.ts` blob = `5bcc6876879e1f3fda9c0a9b35033732696f1281`。全 76 文件相对基线 numstat 合计 **+4745 / −1084**（独立 `git diff --numstat` 实算，与开工提示一致）。

### 1.1 HTML 锚点实核（锁出处行 → 现行正本）

| 判据 | 锁出处 | 现行该行含 |
|---|---|---|
| BSM-001 | bsm:505 | `整体说明：这套系统到底管什么`（同段含「合作模式有哪几种」及策略可配原则；「有哪几种」清单不做后台可配） |
| BSM-003 | bsm:563 | `系统通用能力：数据可改可删、操作有引导、权限分岗`（锁摘录仅为 h2+导语；存档删除细则属未映射 BSM-004，不拿未映射子条当本段冲突 ID） |
| PGM-001 | pgm:256 | `整体定位：平台治理层不是一个业务部门，而是所有业务的底座`（同段含「见名可点 / 不允许孤立数据」） |

对照用（非本段冻结 ID，不支撑 finding）：BEQR L231 合作模式六类；BSM L701 六类合作方式；BSM L1433 商户档案标注自营→员工走公司自有路径；PGM L495 区分六类合作方式。

## 2. 1 文件覆盖表（全文已读；行=`\n` 计数 = splitlines）

副本根：`unpublished-03acdb98/`。清单本段条目 sha256/行/字节与独立 `sha256sum`/`wc` **1/1 match**。副本 `git hash-object` 与树 `e28dad05:<path>` blob **1/1 相同**。文件以 `\n` 结尾。

| # | 路径 | 行 | 字节 | SHA256 | 全文 |
|---|---|---:|---:|---|---|
| 1 | `apps/web/src/admin/types.ts` | 1301 | 34127 | `0b82f6c2ca2422fa6ed1cc28ed54824d73539a24eb9ca001793d7b6a2c49540c` | 已读 L1–L1301 |

合计 1301 行 / 34127 字节。导出 `export type` **92** 个（L2 `AdminUser` … L1292 `VoucherOverviewRow`）。本段无测试文件、无迁移、无 OpenAPI、无 SQL。

结构覆盖（不能只看 +2 行差异；按块读完）：

| 行区间 | 块 | 覆盖 |
|---|---|---|
| L1 | 版本注释 `// 文件版本：V0.01`（相对基线新增） | 已读 |
| L2–L86 | 壳：`AdminUser` / 权限态 / 导航域 / 账号形态 / 路由 / API 客户端 / 页 props | 已读 |
| L88–L161 | 看板指标 + 账号/权限模板行 | 已读 |
| L165–L436 | 票务 / 酒吧 / 存酒 / 合同 / 主管审核策略 | 已读 |
| L439–L677 | 游乐 / 库存 / 财务 / 对账 / 工单 / 集成 | 已读 |
| L679–L918 | 风险 / 审计双表 / 指标 / 商户费用 | 已读 |
| L921–L1068 | 分红 / 商户专区结算合同账单 | 已读 |
| L1071–L1171 | 治理主体/项目/空间/商户准入/主体绑定（**本段唯一业务新增在 L1121**） | 已读 |
| L1174–L1301 | 清理作业 / 操作留痕 / KPI 明细 / 券台账 | 已读 |

员工 DTO：本文件 **无** `StaffProfile*` / `StaffShift*` / `StaffPayroll*` 导出。未发布员工页（U03）在页面内联 `StaffProfileRow`（缺 `companyId`/`registrationSource`，对照 U05 `StaffProfileRecord`）。本段职责是后台 DTO 枢纽；员工形状不在本文件，记 OBS-05，不把 HTML 沉默升冲突。

### 2.1 相对发布基线的逐文件 diff stat（真实改动）

命令：`git diff --stat 242f487233d028173cb9198eecd079f5d877405c e28dad059b2c7e08bcc95a8db18552d3f76edf4b -- apps/web/src/admin/types.ts`

```
 apps/web/src/admin/types.ts | 2 ++
 1 file changed, 2 insertions(+)
```

numstat：`2	0	apps/web/src/admin/types.ts`

blob：

| 参照 | blob |
|---|---|
| 发布基线 `242f4872:apps/web/src/admin/types.ts` | `8d26e82118d6f9e607fe85e18ec638a81adc5dd4` |
| 未发布树 `e28dad05:…` = 副本 hash-object | `5bcc6876879e1f3fda9c0a9b35033732696f1281` |
| HEAD `445a49f4:…` | `8d26e82118d6f9e607fe85e18ec638a81adc5dd4`（**与基线相同**） |
| `bd445728:…` | `8d26e82118d6f9e607fe85e18ec638a81adc5dd4`（同） |

`git diff 242f4872 HEAD -- apps/web/src/admin/types.ts`：**空**（main 自基线未改此文件）。

统一 diff（基线 → 未发布树）全文：

```
diff --git a/apps/web/src/admin/types.ts b/apps/web/src/admin/types.ts
index 8d26e821..5bcc6876 100644
--- a/apps/web/src/admin/types.ts
+++ b/apps/web/src/admin/types.ts
@@ -1,3 +1,4 @@
+// 文件版本：V0.01
 export type AdminUser = {
   id: string;
   username: string;
@@ -1117,6 +1118,7 @@ export type GovernanceSpaceRow = {
 
 export type MerchantAdmissionRow = {
   id: string;
+  selfOperated?: boolean;
   merchantName?: string;
   merchantType?: string;
   businessEntityId?: string;
```

即：+版本行；+`MerchantAdmissionRow.selfOperated?: boolean`（L1121）。其余 1299 行与 main/基线逐字节相同。

全 76 文件相对基线：`files 76 +4745 -1084`。

## 3. 第一性原理线

### 3.1 本文件为什么必须存在

`types.ts` 是管理后台 DTO 枢纽（M365：`AdminApp` / `adminRoutes` / 各页 `AdminPageProps`）。删掉它，后台 TypeScript 无共享行类型，BSM-001「工作人员 / 谁能操作」、PGM-001「先管归属/再管资格」在网页层失去编译期契约。有业务上位，不是死代码。

### 3.2 本段相对基线的 +2 行为什么必须存在

1. `// 文件版本：V0.01`：无 HTML 上位。技术性版本头，无业务发明。删掉不影响任何 HTML 条款。
2. `selfOperated?: boolean`：
   - 指向 BSM L1433「在商户档案上标注为自营的商户视同公司自有，其员工按公司自有员工路径办理」——**字段存在**可作该句的下位实现（细化）。
   - HTML 同时把「自营」放进封闭六类合作方式（BSM L701、BSM-001「合作模式有哪几种」不做可配、PGM L495）。本类型把 `selfOperated?: boolean` 与 `cooperationForm?: string` **并列且无关系约束**，允许 `cooperationForm:"lease"` 且 `selfOperated:true`，也允许 `cooperationForm:"self_operated"` 且 `selfOperated:false`。
   - 未发布后端员工注册读的是布尔，不是合作方式（U05 `StaffRegistrationService.java:66,91`）。未发布写路径 `PlatformService.setMerchantSelfOperated`（U08 L178–190）只 `withSelfOperated`，**不改** `cooperationForm`。更新商户时 L655 从旧行拷回布尔，与新合作方式解耦。
   - 因此「独立布尔可与六类合作方式分叉」**不是** L1433 的下位细节（下位细节不会引入与「合作方式=自营」相反的第二真值），构成 DTO 业务发明 → **添加**（GR-U04-001）。不代用户回答 2.2。

### 3.3 声称的行为有没有被实际执行验证

本文件是纯类型，无运行时。验证用隔离 `tsc`（类型 4），不装依赖、不写永久测试、不碰 `complex_test*`：

| 探针 | 输入 | exit | 关键输出 |
|---|---|---:|---|
| MINI-UNPUB | `/tmp/u04-b1-mini`：未发布 `types.ts` + 分叉字面量 | **0** | 分叉合法；`cooperationForm:"fixed_rent_plus_commission"` 亦合法（string 未收窄） |
| MINI-MAIN | 同探针换 main `types.ts` | **2** | `probe.ts(5,3)` 与 `(10,3)` `TS2353: 'selfOperated' does not exist in type 'MerchantAdmissionRow'` |
| TEST-A | `/tmp/u04-b1` overlay：未发布 types + **main 全页** + 分叉探针 | **0** | `error TS` = 0 |
| TEST-B | main types + 未发布 `OrganizationMerchantPage.tsx` | **2** | **4× TS2339** L268/L320/L325/L326 `Property 'selfOperated' does not exist` |
| TEST-C | 未发布 types + 未发布该页 | **0** | `error TS` = 0 |
| TEST-D | main types + main 页 | **0** | `error TS` = 0 |

未跑：后端集成、vitest、会清库的脚本。不把未运行测试写通过。U03 测试文件对 `selfOperated` **0 命中**（`rg` 无匹配）。

### 3.4 未验证就当真的前提

1. 「发布本文件会覆盖 main 已修的 types.ts」——**假**。HEAD blob = 基线 blob，main 未改此文件（§6）。
2. 「`selfOperated` JSON 键等于库列 `is_self_operated`」——**假**。Jackson `SNAKE_CASE` 序列化 Java 字段 `selfOperated` → `self_operated`；`adminClient.toCamelCaseKey("self_operated")` → `selfOperated`。列名 `is_self_operated` 若误进 JSON 会变成 `isSelfOperated`，对不上类型。首次正常路径走 Java 字段名，不发生；记 OBS-07，不升 finding。
3. 「可选 `?` 等于后端总会给布尔」——未验证运行时 GET。Java 是 primitive `boolean`，V408 `NOT NULL DEFAULT FALSE`。类型可选是渐进宽松。OBS-06。
4. 「单独把本文件快进合进 main 即完成自营标注」——**假**。TEST-A 只证明 tsc 绿；main 无列、无 `POST /merchants/{id}/self-operated`（HEAD `rg is_self_operated` 于 `backend/**/db/migration` 与 `apps/web/src/admin` 均为 0）。U03 页若随后合入会 404。GR-U04-002。

## 4. 顺从式线（21 项冻结判据逐项）

每个 ID 都过本文件；无 finding ≠ 未审。

| ID | 适用面 | 证据 | 本段结论 |
|---|---|---|---|
| GEN-001 | 必要性 | 文件是后台 DTO 枢纽；+`selfOperated` 承载 L1433 标注，但做成与六类并列的第二轴（001）；版本头无需求承载（OBS-09） | **FAIL** 001 |
| GEN-002 | 可执行正确性 | 纯类型；隔离 tsc A/C/D=0，B=4×TS2339；无运行时入口 | **FAIL** 002（缺同批则 U03 页不能构建） |
| GEN-003 | 测试有效性 | 本段无测试文件；U03 `OrganizationMerchantPage.test.tsx` 0 命中 `selfOperated`；不把「tsc 绿」当行为证据 | 无实现面（测试在消费者段）；OBS-08 |
| GEN-004 | 信任边界 | 类型不校验输入；`cooperationForm?: string` 不收窄六类；`selfOperated?` 不校验与合作方式一致 | 校验在 U08 服务；本文件无实现面 |
| GEN-005 | 授权 | 类型不授权；页 props 声明 `effectivePermissions`；写自营的权限码在 U08 `merchant.profile.manage` | 无实现面 |
| GEN-006 | 数据完整性 | 类型允许 `lease`+`selfOperated:true` 与 `self_operated`+`false` 同时成立（MINI-UNPUB exit 0）；U08 writer 不维持不变量 | **FAIL** 001 |
| GEN-007 | 并发幂等 | 无运行时。OpenAPI 该 POST `x-idempotency-required: true`；`MerchantSelfOperatedRequest` 含 `idempotencyKey`；本文件未导出该请求类型 | 无实现面；OBS-11 |
| GEN-008 | 跨层一致 | JSON `self_operated` ↔ 类型 `selfOperated` ↔ Java `selfOperated` 一致；库列 `is_self_operated` 仅 JDBC。`cooperationForm` 仍是开放 string，与 main 白名单六码不一致（main 既有）。新布尔与六类无类型关系。GET OpenAPI 商户 schema 未列 `self_operated`（未发布 yaml 仅 POST 体有） | **FAIL** 001/002 |
| GEN-009 | 错误处理 | 无运行时 | 无实现面 |
| GEN-010 | 安全与 PII | `AccessUserRow.phone/phoneHash`、`MerchantAdmissionRow.contactPhone` 等为类型声明；脱敏在 U08 `MerchantRecord.withMaskedPhones`。无秘密、无注入面 | 无本段引入泄露；OBS-10 |
| GEN-011 | 可观测 | 无日志。U08 成功事件 `merchant.self_operated_changed` 不在本文件 | 无实现面 |
| GEN-012 | 资源边界 | 无循环/查询 | 无实现面 |
| GEN-013 | 构建配置 | 本文件非构建脚本。合入 U03 页而不合本文件 → tsc 红（TEST-B） | **FAIL** 002 |
| GEN-014 | 迁移 | 本文件无 SQL。对应列在 U10 `V408__staff_registration_company_routes.sql` L2 `ALTER TABLE merchants ADD COLUMN is_self_operated BOOLEAN NOT NULL DEFAULT FALSE`；main **无** V408 | 无实现面；同批见 002 |
| GEN-015 | 界面与中文 | 本文件无展示文案；导航 key 是内部标识。展示「自营商户」在 U03 页 | 无实现面 |
| GEN-016 | 文档可验证 | 版本头 V0.01 与文件一致；不得把本副本写成 main 已发布 | 本报告未冒称；OBS-09 |
| GEN-017 | 业务来源分类 | 按冲突→细化→技术性→添加。001=添加（独立自营布尔）；002=技术性（同批构建）。HTML 沉默不升冲突 | 本席已分类 |
| GEN-018 | 第一性证据 | 结论落到 HTML 逐字原文 + tsc 输出，不靠席位意见 | 见 §3/§9 |
| BSM-001 | 系统管什么 / 合作模式有哪几种 | 六类合作方式是口径清单、不可后台增删一类。本文件第二自营轴等于在 DTO 增一类「员工路径自营」开关 | **FAIL** 001 |
| BSM-003 | 通用能力导语 | 本文件含商户 `archivedAt/deleteStatus`（基线既有，非本段新增）。新增布尔与存档删除无直接关系。不拿未映射 BSM-004 当冲突 ID | 无本段新增冲突 |
| PGM-001 | 治理底座 / 全链可点 | 本文件提供 `navigateTo`/`focusObjectId` 及行 id。自营布尔若与合作方式分叉，归属/资格两条链各说各话 | **FAIL** 001（资格轴分裂） |

## 5. 对抗式线

grilling 追问树（事实自取，不问操作员）：

1. **发布本文件会不会覆盖 main 已修项？** 不会。HEAD/基线 blob 均为 `8d26e821`。M009-003 / M012-006/007/008 改的是 `OrganizationMerchantPage` 等，不在本文件。对抗式固定题的答案： **本段文件对 main 已修项是快进 +2 行，不是覆盖冲突。** 整段仍因 001/002 FAIL。
2. **类型能否表达「租赁商户 + 自营标注」？** 能。MINI-UNPUB exit 0。`StaffRegistrationService` 只看布尔 → 首次在租赁商户上点 U03「标记为自营」就会改员工路径、不改合作方式。属首次正常使用，非极端巧合。
3. **只发 U03 不发本文件？** TEST-B 4× TS2339。首次 `tsc` 即红。
4. **只发本文件不发 U08/U10？** TEST-A tsc 绿，字段运行期恒 `undefined`（JSON 无键）；随后 U03 页 POST main 无路由 → 404。首次点按钮即红。
5. **`is_self_operated` 会不会被 camel 错位？** 仅当 JSON 用列名。首次正常 Jackson 用字段名 `self_operated`。不升 finding（controller：要两三件不巧才成立 → 不修）。
6. **员工页是否依赖本文件的员工类型？** 否。内联类型。本文件不承担员工行契约（OBS-05）。
7. **main 页读 `selfOperated`？** HEAD `apps/web/src/admin` **0 命中**。快进合入后 main 页行为不变。
8. ** ponytail：** 相对基线的 +2 行几乎无可删。版本头可删（`yagni`，1 行）。`selfOperated` 若只是 `cooperationForm==="self_operated"` 的投影则属第二轴，应删字段改派生——这是 001 的删除分支，不是本轮落地。`net: -1`（仅版本头）若保留独立布尔；若删除布尔则 `net: -2` 并改 U03/U08/U10。

越权/并发/金额：本文件无运行时。权限在 U08。幂等键在客户端 `normalizeRequestBody` 自动填（U03 页可不显式传）。不把客户端自动填幂等写成 types.ts 的行为证据。

## 6. 与 main 分叉对照（对抗式固定题）

| 项 | 基线 242f4872 | 未发布 e28dad05 / 副本 | main HEAD 445a49f4 |
|---|---|---|---|
| types.ts blob | 8d26e821 | 5bcc6876 | 8d26e821 |
| 版本头 | 无 | `// 文件版本：V0.01` | 无 |
| `MerchantAdmissionRow.selfOperated` | 无 | `selfOperated?: boolean` L1121 | 无 |
| `cooperationForm` 类型 | `string?` | `string?`（未收窄） | `string?` |
| 合作方式六类白名单 | 当时服务层无 | 未发布页仍用三码（U03，非本文件） | M009-003：`PlatformService.MERCHANT_COOPERATION_FORMS` 六码；main 页 `COOPERATION_FORMS` 中文六类 |
| `POST …/self-operated` | 无 | U08 `PlatformController.java:86–92` | 无 |
| `merchants.is_self_operated` | 无 | U10 V408 L2 | 无 |
| 员工注册是否读该布尔 | 无 | U05 `StaffRegistrationService.java:66,91` | 无此字段 |

**发布本段文件到当前 main：** `git diff e28dad05 HEAD -- apps/web/src/admin/types.ts` 显示 main 相对未发布是 **删 2 行**。合入方向是把这 2 行加到 main。**不会**覆盖 M009–M012 在本文件上的修复——因为那些修复从未改过本文件。

**会与 main 已修项打架的不是本文件内容，而是本文件所开启的合同：**

- 未发布 U03 页仍写三码 `fixed_rent_plus_commission/revenue_share/fixed_rent`（已定稿 U03-002），本类型 `cooperationForm?: string` **照单全收**（MINI 探针第三例 exit 0），起不到 main 白名单的编译闸。
- 本类型新增的 `selfOperated` 让 U03 页能通过 tsc（TEST-C=0），从而把「独立自营按钮」送到运行期；运行期打 main 无路由（002），打未发布后端则与六类分叉（001）。

不得把「本文件快进安全」写成整段 PASS。

## 7. Findings

临时 ID 自 GR-U04-001 起。正式 ID 仅 B2 分配。

### GR-U04-001 — 独立于六类合作方式的 `selfOperated?: boolean`（添加）

- **文件:行号：** `apps/web/src/admin/types.ts:1121`（类型所在）；并列字段 `cooperationForm?: string` 在 `:1136`。数据流对照（只读，非本段修复面）：未发布 `apps/web/src/admin/pages/OrganizationMerchantPage.tsx:268,:320-326`；`backend/.../PlatformService.java:178-190,:655`；`backend/.../StaffRegistrationService.java:66,91`；`backend/.../MerchantRecord.java:36`；`V408__staff_registration_company_routes.sql:2`。
- **完整原文：**
  ```
  export type MerchantAdmissionRow = {
    id: string;
    selfOperated?: boolean;
    merchantName?: string;
    ...
    cooperationForm?: string;
  ```
- **业务影响：** 后台所有读 `MerchantAdmissionRow` 的页（未发布商户页抽屉「自营商户」是/否、以及 `BarVenueSelect` / `FinancePropertyFeePage` / `GovernanceTopologyPage` / `TerminalDevicesPage` 等）在类型上把「员工路径是否自营」做成可与「合作方式」相反的第二开关。租赁商户可被标自营而合作方式仍是租赁；合作方式已是 `self_operated` 的商户也可 `selfOperated:false`。员工两条注册路径（BSM L1433）将跟布尔走、不跟六类走。资格/归属在治理底座上分裂（PGM-001）。
- **可复现命令 / 最小反例：**
  ```
  # /tmp/u04-b1-mini：未发布 types.ts + 下列字面量
  export const splitBrain: MerchantAdmissionRow = { id: "mch_probe", cooperationForm: "lease", selfOperated: true };
  export const bothSelf: MerchantAdmissionRow = { id: "mch_probe2", cooperationForm: "self_operated", selfOperated: false };
  $ROOT/node_modules/typescript/bin/tsc --noEmit -p /tmp/u04-b1-mini/tsconfig.json
  # MINI-UNPUB-EXIT=0（类型接受分叉）
  # 换 main types.ts → MINI-MAIN-EXIT=2，TS2353 selfOperated does not exist
  ```
  后端对照（只读）：`setMerchantSelfOperated` 只 `withSelfOperated`；`StaffRegistrationService` 用 `merchant.selfOperated()`，不用 `cooperationForm`。
- **冻结判据：** BSM-001、GEN-001、GEN-006、GEN-008、GEN-017、PGM-001
- **2.3 依据类型：** 1（HTML 逐字）+ 4（tsc 输出）
- **四分类：** **添加**（判定顺序：非冲突——HTML 未写「标注不得独立于合作方式」，沉默不升冲突；非细化——可与「合作方式=自营」相反的第二真值不是 L1433 的下位细节；非技术性——DTO 布尔是业务开关，2.1 规定类型/DTO 不豁免业务发明）。
- **无业务发明理由：** 不适用（本条是添加，有业务发明）。
- **添加项 2.2 材料（B1 只列，不发问、不代答）：**
  - HTML 原文（出处：`docs/business-system-map-for-operations-v0.1.html` L701 / L1433；同旨 `docs/platform-governance-map-for-operations-v0.1.html` L495；BSM-001 段内「合作模式有哪几种」）：
    > 建立商户档案，区分自营、租赁、联营、代运营、服务商接入、其他这六类合作方式。
    > 在商户档案上标注为自营的商户视同公司自有，其员工按公司自有员工路径办理。
  - 添加了什么：`MerchantAdmissionRow.selfOperated?: boolean`，与 `cooperationForm?: string` 并列、类型上可任意组合。
  - 添加后的代价：维护两套「是否自营」；员工路径与结算/合同合作方式可分叉；U03/U05/U08/U10 全部绑在布尔上；测试与 OpenAPI 要跟第二轴。
  - 删除的代价：删字段则 U03 页 4× TS2339（TEST-B）、U05 注册分流与 U08 端点/U10 列失去前端契约；若改为派生自 `cooperationForm==="self_operated"`，须改 U05/U08 读路径并去掉独立 POST 或把它改成改合作方式。
- **工程缺陷成立：** 是
- **建议登记：** 是（发布前须经 B2 2.2；答复「删除」则去掉独立布尔改派生；答复「保留」则须钉死与六类的同步或明确允许分叉的业务规则——后者仍要写进 HTML 才不是发明，本席不代答）
- **最小处置（写给发布者，本轮不修）：** 不要把本字段当作无成本快进。B2 问清后：要么删除 `selfOperated`、员工路径改读合作方式「自营」；要么保留布尔并在 U08 writer 与类型层强制与 `cooperationForm==="self_operated"` 同步（或经 HTML 批准成为第二轴）。不得在未答 2.2 时合入。

### GR-U04-002 — `selfOperated` 类型与 U03 页 / U08 路由 / U10 列必须同批（技术性）

- **文件:行号：** `apps/web/src/admin/types.ts:1121`；对照未发布 `OrganizationMerchantPage.tsx:268,:320-326`；`PlatformController.java:86-92`；`V408__staff_registration_company_routes.sql:2`。
- **完整原文：** 同 001 的 `selfOperated?: boolean;`。U03 页原文：`row.selfOperated ? "是（员工本人注册，经所属公司审批）" : "否（…）"`；`api.post("/api/v1/platform/merchants/" + id + "/self-operated", { selfOperated: !row.selfOperated, reason })`。
- **业务影响：** 无新业务规则。缺本文件，U03 页不能通过当前 main 的 tsc；有本文件无 U08/U10，按钮第一次点击打当前 main 即 404。类型半边不能单独称为「自营标注已交付」。
- **可复现命令：**
  ```
  TSC=$ROOT/node_modules/typescript/bin/tsc   # Version 5.9.3
  # TEST-B：main types + 未发布 OrganizationMerchantPage.tsx
  # TEST-B-EXIT=2
  # 4× TS2339 L268/L320/L325/L326 Property 'selfOperated' does not exist on type 'MerchantAdmissionRow'
  # TEST-A：未发布 types + main 全页 → EXIT=0
  # TEST-C：两边都未发布 → EXIT=0
  # HEAD: rg is_self_operated backend/src/main/resources/db/migration → 无；ls *408* → NO V408 on main
  # HEAD: rg selfOperated apps/web/src/admin → 无
  ```
- **冻结判据：** GEN-002、GEN-008、GEN-013
- **2.3 依据类型：** 4（tsc / rg / ls 输出）
- **四分类：** **技术性**
- **无业务发明理由：** 同批约束只保证类型、路由、列一起能构建和被调用，不新增「自营」口径、不增枚举、不改员工路径规则。规则发明在 001。
- **工程缺陷成立：** 是
- **建议登记：** 是
- **最小处置（写给发布者，本轮不修）：** 与 U03 商户页（及其测试）、U08 `MerchantRecord`/`PlatformService.setMerchantSelfOperated`/`PlatformController`/`MerchantSelfOperatedRequest`/`JdbcPlatformRepository`、U10 V408 **同一 PR**。禁止只合本文件当作自营交付；只合本文件虽 tsc 绿（TEST-A）但是死字段。若 001 答「删除」，本条随布尔删除而消失。

## 8. 观察（无新增根因；建议登记否）

| ID | 观察 | 归 |
|---|---|---|
| OBS-01 | 本文件相对 main **快进 +2 行**，不覆盖 M009-003 / M012-006/007/008（那些不在 types.ts）。不得把「无覆盖」写成整段 PASS | 发布说明 |
| OBS-02 | `cooperationForm?: string` 未收窄为六类联合类型；MINI 探针接受 `fixed_rent_plus_commission`。main 服务层已白名单六码。本段 diff 未改此字段 | M365 / M009 既有 |
| OBS-03 | `displayName`/`companyName`（L1141–1142）、`GovernanceSpaceRow.venueId`（L1111 已标旧臆造）、`SubjectBindingRow` 七个臆造 optional（L1150–1161）。L1071–1073 自承旧臆造。M012 已改页面、明确不改 types.ts | M365 / M012 既有 |
| OBS-04 | `AuditEventRow` L695–726 驼峰/蛇形双写 | M365 既有 |
| OBS-05 | 无 Staff* 导出；U03 `StaffProfilesPage.tsx:12-24` 内联行类型缺 `companyId`/`registrationSource`（U05 V408 已加）。HTML 未要求必须集中到 types.ts | U03/U05；不升冲突 |
| OBS-06 | 未发布 OpenAPI GET 商户 schema 未列 `self_operated`；POST 体有且 required。实际 GET JSON 以 Java 字段为准。类型 optional vs Java primitive boolean vs V408 NOT NULL | 后续 OpenAPI 段；本文件对齐 Java |
| OBS-07 | 库列 `is_self_operated` vs JSON `self_operated`。误用列名才会 camel 成 `isSelfOperated`。首次正常路径不发生 | 极端，不修 |
| OBS-08 | 本段无测试；U03 测试 0 命中新字段。GEN-003 消费者段已在 U03-007 登记 | U03 |
| OBS-09 | `// 文件版本：V0.01` 技术性、无业务发明、无 HTML 上位 | 可删可留 |
| OBS-10 | 电话/phoneHash/证件等仅为类型；脱敏在后端。本段未新增 PII 字段（`selfOperated` 非 PII） | 无缺陷 |
| OBS-11 | 未导出 `MerchantSelfOperatedRequest`；本文件惯例以 Row 为主、请求体多为 `Record<string, unknown>` | 惯例，非发明 |
| OBS-12 | 开工提示 HEAD=bd445728；实算 HEAD=origin/main=`445a49f4`（#429 M011–M012）。bd445728 为祖先。对照以 445a49f4 为准 | 记录 |
| OBS-13 | 未发布提交对象 `03acdb98` 本仓不可解析；树 `e28dad05` 可解析。审计输入是副本+树，不是该 commit | 记录 |
| OBS-14 | BSM-003 子条存档删除（BSM-004）未映射本段；`MerchantAdmissionRow.archivedAt/deleteStatus` 为基线既有，不拿未映射子条打冲突 | 分类纪律 |
| OBS-15 | main `OrganizationMerchantPage.tsx` 已用六类中文 + `self_operated` 作为**合作方式码**，与本类型新增**布尔**同名根 `self_operated`/`selfOperated`，阅读易混。命名碰撞本身不构成第三轴，但放大 001 | 随 001 |

## 9. 命令证据（只读）

环境：未 `source` env 文件；未 export 任何 DB 变量；未写 `complex_test*`。`tsc` 用仓库已有 `node_modules/typescript/bin/tsc`（5.9.3），未 `npm install`。

```
git rev-parse HEAD
# 445a49f409f0cfbe6dd616ae079be1299824c48f
git rev-parse origin/main
# 445a49f409f0cfbe6dd616ae079be1299824c48f
git merge-base --is-ancestor bd445728a6ec7bb36a9e7ff19fe56927410ac61e HEAD ; echo $?
# 0
git cat-file -t e28dad059b2c7e08bcc95a8db18552d3f76edf4b
# tree
git cat-file -t 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb
# fatal: git cat-file: could not get object info
git cat-file -t 242f487233d028173cb9198eecd079f5d877405c
# commit

sha256sum docs/audit-2026-09/plan/criteria-lock.md
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
sha256sum docs/audit-2026-09/ledger.md docs/audit-2026-09/findings.md
# bd82e9fdc517c3d1af45dde89a17b9f18692a583c0c8f9a83357202edafa74df  ledger.md
# 2ca8751dd5731c9406c0f58b23965d2c254fdeffde31b07327978b5220d6797c  findings.md

sha256sum .scratch/module-recheck-20260908/unpublished-03acdb98/apps/web/src/admin/types.ts
# 0b82f6c2ca2422fa6ed1cc28ed54824d73539a24eb9ca001793d7b6a2c49540c
wc -l -c …/types.ts
# 1301 34127
git hash-object …/types.ts
# 5bcc6876879e1f3fda9c0a9b35033732696f1281
git rev-parse e28dad05:apps/web/src/admin/types.ts
# 5bcc6876879e1f3fda9c0a9b35033732696f1281
git rev-parse HEAD:apps/web/src/admin/types.ts
# 8d26e82118d6f9e607fe85e18ec638a81adc5dd4
git diff --numstat 242f4872 e28dad05 -- apps/web/src/admin/types.ts
# 2       0       apps/web/src/admin/types.ts
git diff --numstat 242f4872 e28dad05 | awk '{a+=$1;b+=$2;n++} END{print n, "+",a, "-",b}'
# 76 + 4745 - 1084

# MINI-UNPUB-EXIT=0
# MINI-MAIN-EXIT=2  TS2353 ×2 selfOperated does not exist
# TEST-A-EXIT=0  errorTS=0   (unpublished types + main pages)
# TEST-B-EXIT=2  errorTS=4   TS2339 ×4 OrganizationMerchantPage :268/:320/:325/:326
# TEST-C-EXIT=0  errorTS=0
# TEST-D-EXIT=0  errorTS=0

python3 toCamel: self_operated -> selfOperated ; is_self_operated -> isSelfOperated
```

TEST-A/B/C/D 在 `/tmp/u04-b1`：rsync 了 `apps/web`（排除 node_modules/dist/test-results），`ln -s` 仓库 `node_modules` 到 overlay 根，用已有 tsc，**未改**副本、main、HTML、锁、台账。MINI 在 `/tmp/u04-b1-mini`，仅 `types.ts`+`probe.ts`。

未运行：vitest、maven、flyway、任何连库脚本。不把它们写通过。

## 10. 结论

**FAIL**

没有有条件通过。添加项 GR-U04-001 未闭合（B1 不得代答 2.2）。技术性 GR-U04-002 发布前须与 U03/U08/U10 同批。本文件相对 main 虽是快进、不覆盖 M009–M012 在 types.ts 上的修复，但不能单独称为可发布。

B2 输入：本 raw；席甲 self-raw 由调度另收。本席未读 self-raw。正式 ID 仅 B2 分配。

## U04 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 36941 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，独立）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02 §7/§11；criteria-lock V0.11；modules.md V0.04 附录 U04（未发布材料第 4 段，1 文件 `apps/web/src/admin/types.ts` 1301 行，main 归属 M365）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`。`types.ts` HEAD blob = 基线 blob = `8d26e82118d6f9e607fe85e18ec638a81adc5dd4`（main 自基线未改此文件）。
- 审计对象仍是只读输入副本 `.scratch/module-recheck-20260908/unpublished-03acdb98`（树 `e28dad059b2c7e08bcc95a8db18552d3f76edf4b`，基线 `242f487233d028173cb9198eecd079f5d877405c`），不是 main。三份 HTML 取 main 正本。未发布提交对象 `03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb` 本仓 `git cat-file -t` 仍不可解析。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B1 PASS，不照抄席甲 B2 核实结论。
- 本席 B2-r1：**PASS**（001–003 逐 ID 全 PASS；添加 1 已答「删除」，不为本发明再问 2.2；发布前须修「是」=001/002，「否」=003）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。映射行 M365 闭集相同，COUNT=21 unique=21 MISSING=[]（GEN-001–018、BSM-001、BSM-003、PGM-001）。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要核对只读（副本 1/1 哈希、blob、`git diff --numstat`、HTML 锚点、`questions/U04-001.md` 答复首行、U03 定稿节原文）；未重跑 tsc、未装依赖、未碰 `complex_test*`。

此票只判断 findings.md V1.50 顶部「当前 U04 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、「发布前须修」是/否与最小处置。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。材料未发布，定稿后状态应记 REVIEWED-FAIL（有已定稿缺陷，处置写给发布者），不做 B3。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/U04-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 36941 | `aed41cddef4d1116672f535e77abfd9d74399426fd67c9139776fa5a970eb2b5` |
| `docs/audit-2026-09/modules/U04-b1-self-raw.md` | V0.01 | 13393 | `72baf5349285aef599d65307b71e7639f523a154f8fce79ddb6a7525b5eadf48` |
| `docs/audit-2026-09/findings.md` | V1.50 | 570389 | `608c4f97959154ce630005e8583ec8e256032654f8ce4c6aa5879ba73f1655b8` |
| `docs/audit-2026-09/ledger.md`（只读） | V2.53 | 470219 | `da2fa877c7d9e1150c0edeec84fefc1f9a6f8e9044d3150eeee088fac307a7c5` |
| `docs/audit-2026-09/questions/U04-001.md` | V0.01 | 2282 | `5a9e9dab904fe9b82ccb2e414ae07515ea7192c7ed404685c31828a126c2b3d1` |

候选形态：V1.50 顶部 U04-001…003；questions 1（U04-001，答复首行精确「删除」）；无 HTML 修改申请。U04 ledger 行 IN_PROGRESS。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

HTML 正本哈希与 B1 一致：BEQR `4396bb71…1d66`、BSM `a7f7b7a3…66fa`、PGM `c4238afd…e71c`。副本本段 1/1 sha256/行/字节与树 `e28dad05:apps/web/src/admin/types.ts` blob `5bcc6876879e1f3fda9c0a9b35033732696f1281` 全等（本轮复算）。`git diff --numstat 242f4872 e28dad05 -- apps/web/src/admin/types.ts` = `2 0`（+版本行 +`selfOperated?: boolean`）。HEAD `apps/web/src/admin/types.ts` 对 `selfOperated` 0 命中。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers | 先核技能；用户/提示词覆盖技能本体 |
| receiving-code-review | 先核 HTML/diff/blob/grep/已答 2.2 再接受或反驳候选；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：001 分类是否仍是添加、处置「派生」是否发明新规则、002 是否仍须独立入队、003 是否写错 §8、跨段是否改写 U03 定稿 |
| verification-before-completion | 声称前先跑 sha256/git rev-parse/diff/grep/HTML sed/questions 首行；本票不声称新的 tsc 绿 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流（types.ts → U03 页 / U05 StaffRegistration / U08 Platform* / U10 V408 → main 对照 → HTML） |
| ponytail-review | 只评最小删除/收窄，不落地。001 删第二轴不另造投影层；002 不并入 001；不重开弹窗 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到**；未冒充调用 |
| Arc audit | 会派子代理；与双席冲突；**未用** |

### 完整差集（GR-U04-001/002、§8 OBS-01…15；席甲无 finding、OBS-1/2）

漏并：无。两席 B1 全部临时 ID 仍落在 001–003。席甲 B1 自称无 finding、票 PASS；候选取本席 GR-001 的添加分类并记席甲漏判勘误，不照抄席甲 PASS。席甲 OBS-1/2 并入 003，不升缺陷。本席 OBS-01…15 全部落入 003（点名 02/04/05/06/07/11/12/13/15），未把 001/002 降成观察。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/须修 | 本席票 |
|---|---|---|---|
| U04-001 | GR-U04-001（席甲 B1 漏判，勘误） | 添加（操作员已答删除）/是/是 | **PASS（添加已答删除/是/是；不再问）** |
| U04-002 | GR-U04-002 | 技术性/是/是（随 001 删除消失；若违背 001 则四件同一 PR） | **PASS（技术性/是/是；须独立入队）** |
| U04-003 | 双方观察归并 | 登记/否/否 | **PASS（登记/否/否）** |

添加入队 1（001，已答删除）。技术性入队 1（002）。登记观察 1（003）。questions 1 已答。无 HTML 修改申请。

发布前须修「是」=001、002（2）；「否」=003（1）。与候选汇总一致。

独立复核席甲 B1 漏判（不读其 B2 结论当证据）：self-raw §3 写「类型层只需一个可选布尔字段」指向 PGM L578 / BSM L1433；§5 GEN-001「通过」同锚；GEN-017「通过，本段无业务分类争议」。未按冲突→细化→技术性→添加对照 BSM L701 / PGM L495 / L498 六类已含「自营」。本席本轮重读 HTML：BSM L701「区分自营、租赁、联营、代运营、服务商接入、其他这六类合作方式」；PGM L495「区分自营、租赁、联营、代运营、服务商接入、其他等合作方式」；L498 再列六类；BSM L1433 / PGM L578「在商户档案上标注为自营的商户视同公司自有，其员工按公司自有员工路径办理」。副本 `types.ts:1121` `selfOperated?: boolean` 与 `:1136` `cooperationForm?: string` 并列无约束。四分类：**非冲突**（HTML 未写「标注不得独立于合作方式」，沉默不升冲突）；**非细化**（可与「合作方式=自营」相反的第二真值不是 L1433 的下位细节）；**非技术性**（DTO 布尔是员工路径开关，2.1 规定类型/DTO 不豁免业务发明）；**添加**。席甲 B1 PASS 低估成立。B2 接受添加、按 2.2 发问，不是把漏判改成冲突。

### 重点 (a) U04-001 添加已答删除 — **PASS（分类/须修/处置成立，不越界）**

`questions/U04-001.md`「## 操作员答复」首个非空行精确「删除」（本席实读）。2.2：答删除 → 入发布前须修。分类保持**添加**（不因已答删除改判冲突或技术性）。工程缺陷是。须修是。不再问。

处置是否把「派生」写成新规则——独立裁定**否**：

- 本席 B1 GR-001 删除分支原文：「去掉独立布尔、员工路径改读合作方式『自营』」；2.2 材料「若改为派生自 `cooperationForm==="self_operated"`，须改 U05/U08 读路径并去掉独立 POST」。建议登记：「答复『删除』则去掉独立布尔改派生」。
- 问句删除代价已写「员工注册路径改按合作方式＝『自营』（main 六类码 self_operated）派生，只改第 5 段读路径一处」。操作员答的是该问，不是空白「删除」后再由 B2 发明读法。
- HTML 剩余口径：六类合作方式已含「自营」（L701/L495/L498）；L1433 的「标注为自营」在删掉第二轴后，商户档案上只剩这一格。main 已有码 `self_operated`（`PlatformService` 白名单；HEAD 商户页 `{ code: "self_operated", label: "自营" }`）。U05 改读 `cooperationForm == "self_operated"` 是删布尔后的读路径，不是新的投影产品句、不是第七类、不是类型层新闭集。
- 随之去掉 U03「标记为自营」按钮、U08 端点/DTO/仓储插拔、U10 V408 该列：问句删除代价已列，属同一发明的机械后果，不是另立流程。

禁止读成：另写「合作方式投影到员工路径」新产品规则；U08 writer 强制同步两轴；为本段把 `cooperationForm?: string` 收窄为六类联合类型（那是 OBS-02 / M365 既有，须修否）。候选「自营标注＝合作方式『自营』」只表示第二轴删除后 L1433 落在既有六类码上。票 **PASS**。

### 重点 (b) U04-002 随 001 消失但仍须独立入队 — **PASS（成立）**

GR-002 原意：无新业务规则；缺本文件则 U03 页 TEST-B 4× TS2339；有本文件无 U08/U10 则按钮打 main 404；类型半边不能单独称为自营交付。最小处置：「与 U03/U08/U10 同一 PR」；「若 001 答删除，本条随布尔删除而消失」。

候选分类技术性、缺陷是、须修是：与原意一致。处置「随 001 删除一并消失；若发布者违背 001 保留布尔则四件同一 PR」= 删除分支 + 保留分支安全带，不是改写。

**仍须独立入队，不并入 001。** 001 是添加（业务发明 + 2.2）；002 是技术性（GEN-002/008/013 同批构建）。并入 001 会在「违背 001 仍发布尔」时丢掉同批约束。四件＝类型/页面/路由/列（U04/U03/U08/U10），与本席 B1 002 清单一致；U05 读路径属 001，不塞进 002。席甲 overlay tsc 退出 0 只证明四件同在时可构建（本席 TEST-C），不能否定 TEST-B/TEST-A。票 **PASS**。

### 重点 (c) U04-003 观察 — **PASS（未写错本席 §8）**

003 列席甲 OBS-1（旧臆造字段 main 既有，归 M365，与 M012 同根）、OBS-2（Staff 行类型内联于页面），并以「Grok OBS-01…15」收本席观察，点名 OBS-02（`cooperationForm` 未收窄）、OBS-04（AuditEventRow 双写）、OBS-05（与席甲 OBS-2 同）、OBS-06/07/11/15 随 001 删除失效、OBS-12（开工提示 HEAD 写 bd445728、实为 445a49f4）、OBS-13（提交对象 03acdb98 本仓不可解析、以树 e28dad05 为准）。

本席本轮复算 OBS-12/13：`HEAD`=`origin/main`=`445a49f4`；`git cat-file -t 03acdb98` 仍 fatal；树 `e28dad05` = tree。OBS-06（GET schema / optional vs primitive vs NOT NULL）、OBS-07（列名 `is_self_operated` camel 错位）、OBS-11（未导出 `MerchantSelfOperatedRequest`）、OBS-15（布尔与六类码同名根）在 001 删除路径上失去对象，标失效成立，不是把它们升成须修 ID。未把 001/002 降成观察，也未把 §8 其余项（OBS-01 快进、OBS-03 臆造、OBS-08 无测试、OBS-09 版本头、OBS-10/14）写成缺陷。定稿时 003 不必把 §8 逐条抄全。票 **PASS**。

### 重点 (d) 跨段效应 — **PASS（合规，未改 U03 已定稿文本）**

本席实读 findings V1.50 中「当前 U03 B2-r1 双PASS定稿」节仍含：U03-001 缺幂等键、U03-006 存档按钮下拉、U03-007 与 U04/U08/U10 同批、发布者最小集「只迁自营标注（含幂等键与用例）」。U04 候选写「跨段效应（登记，不改已定稿文本）」——单元格未改，声明属实。

收敛为「从商户页去掉自营动作」不发明业务：U03-007 本就有「否则先移除该动作」；U03-006 的下拉由多出来的自营动作引起，去掉即恢复 archive testid；U03-001 是给该 POST 补键，POST 按 001 删除后不必再补键。这是已答删除的机械后果，不是重开 U03 B2、不是改 REVIEWED-FAIL 单元格。

U05/U08/U10「引用 questions/U04-001.md，不再弹窗」：2.2 问的是产品发明，不是审计段号。同一独立布尔已答删除。U03-003/004 沿用 M012-006/007 已答删除的先例适用。范围只及本自营布尔及其插拔/读路径；U05/U08/U10 若另有添加仍须 2.2。票 **PASS**。

### 001 独立裁定（添加已答删除／须修是／不再问）

见 (a)。冻结判据 BSM-001、GEN-001、GEN-006、GEN-008、GEN-017、PGM-001 与本席 B1 一致，未把 L701/L495 锁外锚点写成 finding 支撑 ID（它们只作 2.2 原文与数据流对照）。票 **PASS**。

### 002 独立裁定（技术性／须修是／独立入队）

见 (b)。票 **PASS**。

### 003 独立裁定（登记／否）

见 (c)。票 **PASS**。

### 分类 / 队列 / 给发布者的最小边界

本批可定稿（须席甲本批亦 PASS）。发布前须修是=001、002。否=003。添加 1 已答删除，不进 BLOCKED-ASK，不新开 questions。

发布者最小集（不在本轮改源）：

- 删除 `MerchantAdmissionRow.selfOperated`；不要把该字段当无成本快进合入。
- L1433「标注为自营」落在既有六类合作方式「自营」（码 `self_operated`）。U05 `StaffRegistrationService` 改读 `cooperationForm`，只改读路径一处。
- 去掉 U03 页「标记为自营 / 取消自营标记」动作（含 `POST …/self-operated` 客户端）；U03-001 不再补幂等键，U03-006 随动作消失恢复 archive testid。
- 去掉 U08 `setMerchantSelfOperated` / `MerchantSelfOperatedRequest` / `MerchantRecord.selfOperated` / Jdbc `is_self_operated` 插拔与对应 OpenAPI POST。
- U10 V408 去掉 `is_self_operated` 列（编号与 main V410 关系仍归 M010-009，不在本段发明版本号）。
- 002：上列删除使本条消失。若违背 001 仍发布尔，则类型 + 商户页 + 路由 + 列同一 PR，禁止只合 `types.ts`。
- 本文件其余 1299 行与 main/基线逐字节相同；版本头可留可删（OBS-09）。
- 后续段引用 `questions/U04-001.md`，不再就该布尔发 2.2。
- 禁止：整树覆盖 main；把「派生」写成新的投影/同步产品规则；为本段再弹同一问；把 OBS-02 六类未收窄升成本段须修；改 U03 已定稿单元格。

非阻断勘误（不定稿作废，不必 r2）：

1. 001 处置「自营标注＝合作方式『自营』／派生」钉死读法：删除第二轴后的读路径，不是新规则。禁止另写同步函数或第七类。
2. U03 定稿节仍写「只迁自营标注（含幂等键）」——单元格保持；定稿 U04 后席甲可在 U03 定稿节加一行指向 U04-001（不改 001–015 单元格），避免只读 U03 的发布者仍去迁自营按钮。
3. 003 席甲 OBS-1 的 `BusinessEntityRow` 臆造字段本体是 L1084–1086（`name/code/orgType`）；候选 L1071–1073 是同块注释。同条，不另开 ID。
4. 「不再弹窗」只覆盖本自营布尔发明；U05/U08/U10 其它添加仍走 2.2。
5. 002 四件不含 U05，与本席 B1 一致。

### 结论

全批差集无漏并。001 添加已答删除成立，处置不是新的派生规则。002 须独立入队，随 001 删除消失，违背则四件同一 PR。003 未写错 §8，OBS-06/07/11/15 随 001 失效、OBS-12/13 勘误属实。跨段登记合规，未改 U03 已定稿文本。逐 ID 001–003 全 **PASS**。本席 B2-r1 **PASS**。未经双 PASS 不得把候选当定稿，更不称 U04 或 main 模块 PASS。步骤仍 B2；U04 IN_PROGRESS；材料未发布，不做 B3。
