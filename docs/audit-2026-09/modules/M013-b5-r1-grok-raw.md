CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M013 B5-r1 整模块全量三线审计（Grok 席乙）

- 文件版本：V0.01（指定路径原先不存在，本次新建完整报告）
- 席位：固定 Grok 外审席乙（grok-4.6；不派子代理、不用 MCP、不问操作员、不读本轮 self-raw）
- 步骤：B5-r1（B3 第 1 次整批修复后的整模块全量三线审计；不是只看 diff；不可用 B4-r1 双 PASS、B3 自报红→绿或 B1 历史票替代）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M013 同一 4 文件；`runbook.md` V0.02；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 模块：M013 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第1段
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算 `sha256sum` / `python3 hashlib` / `openssl dgst -sha256` 三工具同值（889020 字节）；sidecar 全文一行同值。独占锁行仅本文第一行，正文不再出现同名独占字段。
- 映射：`M013 → [BEQR-063, BEQR-074, BEQR-143, BEQR-144, BEQR-145, BEQR-146, BEQR-147, BEQR-148, BEQR-149, BEQR-150, BEQR-151, BEQR-152, BEQR-153, BEQR-154, BEQR-155, BEQR-156, BEQR-157, BEQR-158, BEQR-159, BEQR-166, BEQR-189, BSM-004, BSM-005, BSM-006, BSM-007, BSM-009, BSM-028, BSM-029, BSM-072, BSM-074, BSM-114, BSM-115, BSM-120, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-002, PGM-003, PGM-004, PGM-005, PGM-006, PGM-007, PGM-008, PGM-009, PGM-010, PGM-011, PGM-012, PGM-013, PGM-014, PGM-015, PGM-016, PGM-017, PGM-018, PGM-019, PGM-020, PGM-021, PGM-026, PGM-027, PGM-031, PGM-035, PGM-040, PGM-041, PGM-042, PGM-043, PGM-044, PGM-048, PGM-060, PGM-065, PGM-066]`，COUNT=84 unique=84 MISSING=[] EXTRA=[]；与锁映射行逐字相同；定义与 HTML 出处均从锁自读，不新造判据、不 LOCK-AMEND
- 正本检出：`HEAD=origin/main=5692a89502833a423d465b3e2cb8834c9ad91b49`（PR #433 治理地图 V0.12 已合并）。B3-r1 after tree 对象 `dcfc3a8cc70a23e43ef3b47c668411f2c05af1b4` 可解析为 tree；本模块 4 文件当前工作区 blob 与该 after 逐文件全等。当前整工作区临时索引 `GIT_INDEX_FILE=mktemp git add -A && git write-tree`=`2ff9d98857d33531e8954f56d7ba1cfc38fed333`（findings V1.86 / ledger 顶段 / M013 B1·B4 raw 已写入，不等于 B3 after，属台账前进，本席未改源）
- 本席约束：只读业务代码与计划；除本 raw 外不写文件；不修源/三 HTML/锁/findings/ledger；不 commit/push/PR/merge/部署；不装依赖；不写永久测试；主仓 cwd 不写模式（验证在 `/tmp/m013-b5-r1-verify` 与 `/tmp/m013-b5-r1-criteria.txt`，结束已 `rm -rf`）
- 未读取：`M013-b5-r1-self-raw`（当前亦不存在）。B4-r1 双席 raw 与 findings V1.86 只作已定稿边界，不作本 B5 通过依据
- 已定稿背景（只作边界，不代替本轮全量重审）：findings V1.86 — 001（绑定设计按 V410 刷新、登记 OpenAPI 契约缺口）、002（analytics.group.view / risk_level CHECK 按 V394 已闭合）、003（自营标注＝合作方式 self_operated）、004（§6.2.1 字典硬校验 + parking）、005（端点表 DELETE 恒拒绝 + lifecycle）、006（税费登记句现状）、007（准入闸口现状员工域已接线）、008（「考勤打卡」→「查看考勤」两处）已修转「否」；009（OpenAPI entity_role 旧六值）为 XM 归 M392、010 仅登记。B2-r1 定稿禁止项（不得在 B5 升格为缺陷）：改 OpenAPI/V184/迁移/源码、新增字段或接口、删除 §6.2.1 对照表、改三份 HTML。当前修复队列「是」=0。

本次审计结论：**PASS**。无新增未定稿工程缺陷、无添加项、无待问 2.2、无锁外判据、无 HTML 修改申请。001–008 在当前四文件上独立复验成立；009 维持 XM→M392；010 维持仅登记。模块生命周期仍 `IN_PROGRESS`，本票不等于双席 B5 或模块 PASS。席甲=Claude Fable 5.1（操作员 2026-09-09 接替指令），其票是作者自审票；本席 FAIL 具否决权——本席不行使否决。

---

## 0. 技能自核（V0.17 §6，按 B5 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令与 controller 优先于技能本体。本步禁止子代理，与技能「先派子代理」冲突处按用户指令收窄 |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；未运行测试不写通过 |
| systematic-debugging | superpowers 同名 | 只借读错误→复现→追数据流（文档陈述→HTML 原句→V410/V394/`PlatformService`/OpenAPI）；B5 不修复 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地；不把 B2 禁止项写成缺陷 |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本步验证均秒级，未起 monitor |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；Arc `audit` 会派评审子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用** |

B5 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）、using-git-worktrees、executing-plans、dispatching-parallel-agents、subagent-driven-development。

缺口（不冒称调用）：

- `batch-grill-me`：本机 mattpocock 技能树未找到独立技能
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，V0.17 明确不用）
- Arc `audit`：与双席/§7 冲突，**不调用**

明确不用（controller 划掉）：MCP、第三主体、修源/HTML/锁/台账。

---

## 1. 锁、范围与源指纹

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。独立实算值见本文第一行独占锁行。

| 项 | 值 |
|---|---|
| 锁文件 | `docs/audit-2026-09/plan/criteria-lock.md` V0.11 |
| 字节 | 889020 |
| python3 hashlib | 与 sha256sum 同值 |
| openssl dgst -sha256 | 同值 |
| sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` | 全文一行同值 |
| 锁 blob | `8db3fc02ce8a6dc998863a9784e7e88af511b960` |
| findings | V1.86；SHA256 `2008c37d623535d144fa080b93435716895af052479ee5e2afa3dec1d77969c2`；734491 字节 |
| ledger | SHA256 `4887ca9525882603846136c03edbacbd00e437130206818cc9f84c34d2a7955a`；578531 字节；M013 `IN_PROGRESS`，B3=1，B5-r1 进行中 |
| 映射行抽出 | 84 条、无重复、MISSING=[] EXTRA=[]；BEQR 21 / BSM 12 / GEN 18 / PGM 33 |
| 锁内映射 HTML 来源行 vs 现行三 HTML | 66/66 hit=True miss=0（脚本按锁「来源：`path:line`」对照现行 HTML 同行；抽样 BEQR-143 `:226`、BEQR-148 `:231`、BEQR-155 `:238`、BSM-004 `:566`、BSM-028 `:698`、PGM-018 `:415`、PGM L741 查看考勤、BSM L1445 考勤打卡、BSM L1433 自营标注均命中） |
| 计划 FILES | 4 条与 modules.md V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 1517（切分时 HEAD）；当前工作区 `\n` 计数 **1524**（B3-r1 +31/−24 后的现态，不改计划凑数） |
| questions | 无 `questions/M013*`；无未答问询 |
| 160 提示词 | `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| modules.md / runbook.md | SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` / `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |

映射闭集（锁末映射行原文抽出，84 unique）见上文。GEN 无 HTML 行。

三 HTML 工作区 SHA（本席独立，未改）：

| 文件 | 现行 | 行 | 字节 | SHA256 |
|---|---|---:|---:|---|
| `docs/business-enum-quick-reference-v0.1.html` | V0.07 | 308 | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | V0.14 | 2032 | 304482 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | V0.12 | 979 | 103015 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |

证据类型按提示词 2.3：1=三 HTML 原文；2=可执行产物行为；3=外部规范原文；4=可复现命令输出。本模块为文档，以类型 1+4 为主；无本模块测试文件，不把未运行测试写通过。

`git status --porcelain` 对三 HTML / OpenAPI / 锁 / 源码 / 迁移均为空。工作区脏路径仅 findings、ledger、本模块 4 文件（B3 未提交）及 M013 B1/B4 raw（untracked）。

### 1.1 4 文件当前行数 / 字节 / SHA256（工作区实算，行=`\n` 计数 = splitlines）

| # | 路径 | 行 | 字节 | SHA256 | git blob（work=after） | HEAD blob | 全文 |
|---|---|---:|---:|---|---|---|---|
| 1 | `docs/business-entity-binding-development-design-v0.1.md` | 153 | 13095 | `f78f63b3bffe8bbd842f4c4866010abaed652f333d7cdd31fbbb2cc1a510fb7f` | `ad294981d1fca52b35f7986adb1505aafcecd333` | `c91bdf9ee390df79b934354eacabc4e6eaf3d5da` | 已读 L1–L153（标题 V0.09） |
| 2 | `docs/group-governance-rebuild-prd-v1.0.md` | 113 | 19023 | `e60983ccb2884319e36b4059203096582adc0c76c15b239dcdfbc3abfd37b36b` | `e1dfa2e9434ac96a40249eba901997bb59da627c` | `49be576047a67fb89e2ee87865748a7c5f55ae50` | 已读 L1–L113（标题 V1.01） |
| 3 | `docs/platform-governance-acceptance-pack-v0.2.md` | 289 | 35245 | `ca94985de3792578c3436914133c17a99812f5fb5b32acd2ac45a422139def56` | `dbdfc24501028d0e1cbbf130d33c461c467b8bfc` | `7185a6fc511d84085fb8900a4784831cdce25e8d` | 已读 L1–L289（标题 V0.04；含 §11 变更日志与 2026-08-28 补录，未截断） |
| 4 | `docs/platform-governance-prd-v0.1.md` | 969 | 145630 | `aa73e6c76e8389757a635806c893b9a9e8e8539166fd51a8719d3e0bcb1f1413` | `29d77349ae3eb2d49e950be7f4d17c274e771da4` | `3f3c1d73f6dbdf47800f229b2b1147f3c0e028db` | 已读 L1–L969（标题 V0.04；未只读标题/搜索） |

合计行 1524、字节 212993。四文件均整文件读完，未按标题抽样。四文件 blob 与 B3-r1 after tree `dcfc3a8c…` 全等；HEAD blob 仍为 before（B3 未提交）。`git cat-file -t dcfc3a8c…` = `tree`。

必要只读依赖（验证本模块声称的当前实现，不扩审模块；本席未改）：

| 路径 | 用途 | 与 HEAD |
|---|---|---|
| `V005__p0_platform_master_data.sql` | `business_entities` 基表（无 parent、无 archive_reason、status 无 CHECK） | 只读 |
| `V318__business_entity_parent_layer.sql` | 历史父链列（V410 已删） | 只读 |
| `V354__business_entity_master_data.sql` | `archive_reason` 列 + 历史五值 CHECK（V410 已换） | 只读 |
| `V410__m010_business_entity_topology_and_role_isolation.sql` | DROP `parent_entity_id`；CHECK 三值 | 只读 |
| `V184__space_type_dictionary.sql` | 字典八码 | 只读 |
| `V115` / `V169` / `V170` / `V217` / `V394` | 集团权限种子、职位装配、授权申请、薪资税档、risk_level CHECK + group.view TRUE | 只读 |
| `PlatformService.java` | `BUSINESS_ENTITY_ROLES` 三值、`MERCHANT_COOPERATION_FORMS` 六类、`SUBJECT_STATUSES` 五码、`createSpace` 字典硬校验、绑定三态 | 只读 |
| `BusinessEntityRecord.java` | 无 parent / 无 archiveReason 字段 | 只读 |
| `JdbcPlatformRepository.java` | INSERT 无 parent 列 | 只读 |
| `PlatformController.java` | `/api/v1/platform` 主体与绑定五条 API | 只读 |
| `AccessController.java:32,192,204` | `/api/v1/access`；lifecycle；DELETE | 只读 |
| `AccessService.java:479-532` | DELETE 恒抛；lifecycle 五 action | 只读 |
| `StaffApprovalService.java:44,61` | 两处 `reviews.evaluate` | 只读 |
| `SupervisorReviewService.java:49,65` | `finance.tax_rule.adjust` / `tax_rule_adjustment_l3` | 只读 |
| `AnalyticsController.java:33,119-124` | `GET /api/v1/analytics/group-overview` + `requirePlatformScope` | 只读 |
| `docs/openapi/openapi-p0.yaml` | `entity_role` 旧六值；`space_type` 九值含 parking；`punchStaffAttendance` 仅 `staff_work_phone` | 未改 |

---

## 2. 数据流（文档陈述 → HTML 原句 → main 实现事实）

本模块为文档与接口定义类。实现对照只读，不作修复对象。

| 文档声称（当前 after） | HTML 原句（锁/现文） | main 实现事实 |
|---|---|---|
| 四类主体=经营对象/运营/合同/结算，同一经营对象唯一绑定 | BEQR-143 `:226`；PGM-026 `:492` | `BusinessEntityBindingRecord` 四列；`uq_business_entity_bindings_active_merchant` 在 V010；已有 active → `ACTIVE_BINDING_EXISTS` |
| 物理项目与运营公司多对多，不是固定隶属链 | BEQR-144 `:227`；PGM-018 `:415` | V410:9 `DROP COLUMN IF EXISTS parent_entity_id`；`BusinessEntityRecord` 无 parent；Jdbc INSERT 无该列；governance 包 Java `parent_entity` 0 命中 |
| 主体角色三角色 | PGM-018/019 顶层平台 / 城市项目公司 / 域主管公司 | `BUSINESS_ENTITY_ROLES` = 三值；V410 CHECK 同三值。OpenAPI `BusinessEntityRequest.entity_role` 仍旧六值（009，本模块只登记） |
| 合作方式六类；自营标注＝`self_operated` | BEQR-148 `:231`；BSM L1433 / PGM L578 | `MERCHANT_COOPERATION_FORMS` 含 `self_operated`；`StaffRegistrationService` 按该码分流；生产代码 0 处 `is_self_operated`/`selfOperated` 字段 |
| 商户/运营主体六态+退出缓冲；试运营不管 | BEQR-155 `:238` | `SUBJECT_STATUSES` 五码 + 归档走 `archived_at`；绑设计已标词表/缓冲差距 |
| 绑定变更只改三态 | （实现证据；HTML 无此英文闭集） | `changeBusinessEntityBinding` action∈{active,inactive,suspended}；不能提交新三主体组合（设计已标差距） |
| 主体 API 五条；无 `/active`、无 usages | （实现证据） | `PlatformController` `@RequestMapping("/api/v1/platform")` 五条均在；`/active` 与 `/usages` 0 命中 |
| 集团下钻须审批 + 平台 scope | PGM-041 跨项目须先审批；PGM-017/066 | `GET /group-overview` 同时 `analytics.group.view` + `requirePlatformScope()`；V394 已 `requires_approval=TRUE`；其后无回写 FALSE |
| 职位模板删除走存档审批 | BSM-004 `:566` | DELETE `:204` → `deletePositionRoleTemplate:479-481` 无条件 `ARCHIVE_APPROVAL_REQUIRED`；真实路径 `:192` lifecycle，action∈{archive,restore,request_delete,approve_delete,reject_delete} |
| 活空间类型＝字典 | PGM-005 九类业务名词 | `createSpace` 硬校验 `listActiveSpaceTypeCodes()`；V184 八码 building/floor/zone/shop/public_area/parking/booth/other；OpenAPI 九值含 parking 及 mall/bar_table/facility（契约快照，非活码） |
| 私人手机低风险含「查看考勤」；打卡在工作手机 | PGM V0.12 L741；BSM L930/L1865/L1445 | 契约 `punchStaffAttendance` `x-terminal-allowed` 仅 `staff_work_phone` |
| 税费第 5 类权限码仍缺；个税档≠第 5 类 | PGM-041 五类高危 | `staff.tax_bracket.manage` 在 V217（high、requires_approval=TRUE，薪资个税档）；`finance.tax_rule.adjust` 是 SupervisorReview 动作+`tax_rule_adjustment_l3`，不是 `permissions` 行；`finance.tax_*` 权限码 0 行 |
| 准入闸口员工域已接、四域未接 | PGM 合同准入 / PRD §12#8 目标 | `reviews.evaluate` 仅 `StaffApprovalService:44,61` 两处；票务/酒吧/游乐/结算 0 调用 |

---

## 3. 三线审计

### 3.1 第一性原理（每文件三问）

#### docs/business-entity-binding-development-design-v0.1.md（L1–L153，V0.09）

1. **为何必须存在**：删掉则 BEQR-143/PGM-026 四类主体唯一绑定、BEQR-144/PGM-018 多对多协作、BSM-004 存档删除在开发设计层无对当前接口/表的诚实差距清单。有上位。
2. **声称是否执行**：自称「代码、迁移和接口只说明当前实现」。§5 五条 API 在 `PlatformController`/`openapi-p0.yaml` 均存在；唯一约束名、`ACTIVE_BINDING_EXISTS`、变更三态与 `PlatformService` 一致；「不存在 `/active` 与 usages」为真。§3.1「当前迁移」现写 V005+V354+V410 删父链并收三角色；字段表无 `parent_entity_id`；`entity_role` 写 V410 三值且点名 `BUSINESS_ENTITY_ROLES`；差距段把 OpenAPI 旧六值登记为契约缺口（见 009）且「不得当作活闭集」；§8 当前实现已含 V410。与 V410:9 DROP、V410 CHECK 三值、Java 0 parent 字段一致。
3. **未验证前提**：无。V354 CHECK 在「当前迁移」叠层中作为历史加列/换 CHECK 的前序（B2 已接受该叠层写法）；活约束以字段表「V410：三值」为准，差距段用「曾含」过去时。`archive_reason` 列为 V354 实列，Java record 未映射——同文件 §4.3 已标完整存档流程未落地，不另开缺陷。

#### docs/group-governance-rebuild-prd-v1.0.md（L1–L113，V1.01）

1. **为何必须存在**：PGM-017/066 集团视图、PGM-041 角色装配与五类高危、BEQR-151 最终权限。有上位。文内多次标明实现证据不取得效力，方向正确。
2. **声称是否执行**：V115/V169/V170 文件存在；`GET /api/v1/analytics/group-overview`、`requirePlatformScope()`、职位模板 CRUD、grant-requests、temporary-grants、staff-levels、role-assignments、`PUT /users/{id}/position` 均在 Controller。§1 `risk_level` 写 V394 已加 CHECK（low/medium/high/critical）——V394:15 实有。§4 `analytics.group.view` 写 `requires_approval=true`，V394 已置，V115 之后无回写 FALSE。§2 DELETE 标「恒返回 409 `ARCHIVE_APPROVAL_REQUIRED`，不是删除入口」并补 lifecycle 五行 action——与 `AccessService:479-532` 一致。§3 税费句写 V217 个税档≠第 5 类、`finance.tax_rule.adjust` 是复核动作不是 permissions 行、`finance.tax_*` 仍缺——与检索一致。
3. **未验证前提**：无把已闭合缺口再写成现行 false。L27 临时授权 status「现有表无 CHECK」仍真（无 `chk_*temporary*`）。L5 总则「若不满足则记待对齐」为条件句，非点名 false。

#### docs/platform-governance-acceptance-pack-v0.2.md（L1–L289，V0.04）

1. **为何必须存在**：产品设计验收清单，承接 PGM 模块入口与状态闭集，避免编码前文档互打。有上位。L18 自承不是接口/数据库验收。
2. **声称是否执行**：§7 状态表与 BEQR-154/155/156/157/158/159 及「试运营系统不管」一致；十一端与 BEQR-150 一致；七类红线与 PRD/PGM 同旨。G7 现为「实现缺口已闭合」+ V394 `requires_approval=true`；§10 修订说明同步。补录块对已废止闭集有【已废止/不得回灌】标记，未把历史冲突升回现行。
3. **未验证前提**：L200 变更日志 2026-09-01 行仍写「保留实现缺口」——历史行，正文 G7/修订说明已改闭合，不升缺陷（同 010）。

#### docs/platform-governance-prd-v0.1.md（L1–L969，V0.04，未截断）

1. **为何必须存在**：平台治理层产品需求正本之一，承接 PGM 治理顺序、四类绑定、多对多、权限、终端、留痕、考勤。删掉则 P0 产品边界无上层文件。有上位。
2. **声称是否执行**：组织关系 L7/L191 明确不建固定四层隶属链，与 BEQR-144/PGM-018 同旨。§12B.2 考勤与 BSM L1445 同旨（三种领取、取最早、不设宽限、当地时间）。合作模式六类与 BEQR-148 同旨。§5/§6.9 私人手机六项为「查看考勤」，与 PGM L741、BSM L930/L1865 同旨；§12B.2 标题仍「考勤打卡」（B2 禁止改）。§6.9 自营标注＝`self_operated` 既有字段。§6.2.1 活校验＝字典；表头「OpenAPI 契约快照，非活码」；补 parking；脚注写明 mall/bar_table/facility 非活码、building/public_area 不在 OpenAPI。§12#8 现状为员工域两处已接、四域未接。§12#15 GAP-20 写 V394 已 `true` 闭环。
3. **未验证前提**：变更日志 L965 保留「代码轮新增待办」历史句并当场「2026-09-11 订正作废」——日志，不升缺陷。L38「枚举以字段字典为准」与 L655「最终业务边界仍以三份 HTML 为准」并存，现行十一端与 BEQR-150 一致（010）。Keystore 在现行条目标「未立法 / 不得升为已确认规则」，修订说明头仍述 D8 收口史——不升冲突。

### 3.2 顺从式（84 条逐项）

适用面=本 4 文件是否与该锚点原文一致、可执行声称是否指向存在对象。无本文件陈述面则记「无实现面」及原因，不把 HTML 沉默当冲突。B2 禁止项不升格。

| ID | 本模块适用面 | 结论 |
|---|---|---|
| BEQR-063 | 高危动作（酒吧沽清等） | 集团 PRD §3 / 治理 PRD 14A 将高危业务列入五类强制审批，未发明相反沽清规则。酒吧双证据细节无本文件展开。**无相反。通过** |
| BEQR-074 | 隐私桌台 | 4 文件 0 命中「隐私桌台」。**无实现面**（原因=本段不写酒吧桌台权限点）。不升冲突 |
| BEQR-143 | 四类主体 | 绑设计 §1/§3.2、PRD §7.4/§12#19 与原文一致。通过（当前接口差距有登记） |
| BEQR-144 | 多对多非隶属链 | 绑设计 L22、PRD L7/L191 业务口径正确；§3.1 当前快照已按 V410 删父链。**通过**（001 闭合） |
| BEQR-145 | 城市项目公司 | PRD §5 城市项目公司排除清单照抄 PGM。通过 |
| BEQR-146 | 运营公司职责 | PRD 娱乐平台公司段照抄 PGM。通过 |
| BEQR-147 | 主要收入归属 | 本 4 文件不立法收入拆分公式；PRD 12C 股权/牌照分记与 PGM 同旨。无相反。通过 |
| BEQR-148 | 合作模式六类 | PRD L208/L292/L668 六类+「其他」须指定收费模板；§6.9 自营＝`self_operated` 既有字段。**通过**（003 闭合） |
| BEQR-149 | 职级 M1–M10 上中下 | PRD §6.10/§7.8/§12#6。通过 |
| BEQR-150 | 终端十一端 | PRD L38、验收包 §6 十一端（含独立经销商门户）。通过 |
| BEQR-151 | 最终权限计算 | PRD L399/L650、集团 PRD、验收包 §4 公式一致。通过 |
| BEQR-152 | 项目与敏感数据 | PRD §6.10/§12#20、验收包。通过 |
| BEQR-153 | 终端开关优先级 | PRD L655、验收包终端行。通过 |
| BEQR-154 | 综合体项目六态 | PRD §7.1/§10、验收包 §7。通过 |
| BEQR-155 | 商户六态+缓冲+试运营不管 | PRD §6.4/§7.4、验收包 §7、绑设计 L57。业务口径通过。设计「词表未实现」对五态+archived_at 已过宽（观察，同 010） |
| BEQR-156 | 合同八态+当地时间 | PRD §6.5/§7.5、验收包 §2A/§7。通过 |
| BEQR-157 | 员工五态 | PRD §7.6/§10、验收包 §7。通过 |
| BEQR-158 | 设备七态 | PRD §7.7/§10、验收包 §7。通过 |
| BEQR-159 | 权限五态 | PRD §7.8/§10、验收包 §7。通过 |
| BEQR-166 | 合同审批链 | PRD §6.5.1、验收包 G5。通过 |
| BEQR-189 | 管理费方式 | 4 文件不定义服务点单管理费闭集。**无实现面**。不升冲突 |
| BSM-004 | 存档/审批软删 | 绑设计 §2.5/§7、PRD §15C 与原文同旨。集团 PRD 端点表 DELETE 恒拒绝+lifecycle。**通过**（005 闭合） |
| BSM-005 | 新建分步引导 | 验收包/PRD 不反对。无本文件实现。无实现面（产品清单级） |
| BSM-006 | 动态二维码 | 无本文件展开。无实现面 |
| BSM-007 | 平台管理员 vs 最高权限 | PRD §7.8.1 与原文同旨。通过 |
| BSM-009 | 基础管理入口 | 验收包/PRD 范围覆盖综合体/空间/商户/合同/员工/设备/权限。通过 |
| BSM-028 | 基础管理主要功能 | 绑设计+PRD 承接六类合作、四类绑定、多对多。通过 |
| BSM-029 | 和谁有关 | PRD §11 连接表。通过 |
| BSM-072 | 商户管理/业态六值 | PRD L4/L32/L266 六值+单一归属。通过 |
| BSM-074 | 商户和谁有关 | 无相反。通过 |
| BSM-114 | 各类入口 | 验收包页面表、PRD §9。通过 |
| BSM-115 | 相互关系 | PRD §11。通过 |
| BSM-120 | 已确认规则 | PRD §12 已确认条与 PGM-066 对齐处成立。通过（本文件不负责 BSM-120 全表每一拍板的编码） |
| GEN-001 | 必要性 | 4 文件均有 HTML 上位。通过 |
| GEN-002 | 可执行正确性 | 001–008 所涉「当前」声称现与 main 一致。OpenAPI 旧六值已标明非活闭集（009 不在本模块修）。**通过** |
| GEN-003 | 测试有效性 | **无实现面**（原因=本 4 文件无测试代码）。不把未运行测试写通过 |
| GEN-004 | 信任边界 | 文档不实现校验器；绑设计记录创建直接 active 为差距。无本文件可执行输入面 |
| GEN-005 | 授权 | 绑设计 §6 权限码与 Controller 一致；集团 PRD `requirePlatformScope` 与代码一致。通过（文档层） |
| GEN-006 | 数据完整性 | 绑设计记录唯一约束存在（V010）。通过（引用真）。无本文件 SQL |
| GEN-007 | 并发幂等 | 文档不实现幂等键。无实现面 |
| GEN-008 | 接口与模式一致性 | 父链/自营/space_type 活闭集已改。OpenAPI entity_role 漂移已登记不得当活闭集。**通过**（009 不升本模块缺陷） |
| GEN-009 | 错误处理 | 文档层把差距标为差距处诚实。DELETE 现标恒拒绝，不再伪成功。通过 |
| GEN-010 | 安全 | 未引入密钥明文。通过 |
| GEN-011 | 可观测 | 文档要求留痕；不伪造已写审计事件。通过 |
| GEN-012 | 资源边界 | 无本文件循环/连接。无实现面 |
| GEN-013 | 构建配置 | **无实现面**（原因=无 package/CI/tsconfig） |
| GEN-014 | 迁移正确性 | **无实现面**（原因=本模块无 SQL 文件）。引用迁移的「当前」真假走 GEN-016，现已与 V410/V394 对齐 |
| GEN-015 | 中文可用性 | 正文中文；代码标识符/单据号例外。通过（文档层） |
| GEN-016 | 文档可验证性 | 001–008 过期「当前」句已改。残留历史日志/文件名版本/诚实差距见观察，不构成未定稿缺陷。**通过** |
| GEN-017 | 分类纪律 | 本席未把 HTML 沉默当冲突；未把业务发明标技术性；未把 B2 禁止项升格。通过 |
| GEN-018 | 证据 | 类型 1 HTML 逐字 + 类型 4 命令。通过 |
| PGM-002 | 治理顺序五步 | PRD §2.2。通过 |
| PGM-003 | 模块入口 | 验收包/PRD 覆盖卡片主题。通过 |
| PGM-004 | 综合体和集团卡片 | 集团 PRD + PRD §8.2/§12#15。通过（002 闭合） |
| PGM-005 | 空间和点位卡片 | PRD §6.2/§7.2 九类业务名词与 HTML 同。space_type 改为字典硬校验+契约快照。**通过**（004 闭合） |
| PGM-006 | 商户和主体卡片 | 绑设计+PRD。通过 |
| PGM-007 | 合同和准入 | PRD §6.5。通过 |
| PGM-008 | 员工和手机 | PRD §6.9；私人手机列表「查看考勤」。**通过**（008 闭合） |
| PGM-009 | 权限和职级卡片 | PRD §6.10。通过 |
| PGM-010 | 终端开关卡片 | PRD L38/L655、验收包。通过 |
| PGM-011 | 设备管理 | PRD §6.8/§7.7。通过 |
| PGM-012 | 商户侧 | 验收包边界+PRD。通过 |
| PGM-013 | 开放接口 | 验收包非范围/PRD 边界。无相反 |
| PGM-014 | 留痕复核 | PRD §6.11/§7.9。通过 |
| PGM-015 | 楼宇运营 | PRD 12A.4 保安团队。通过 |
| PGM-016 | 应急与隐私 | PRD §15B。通过 |
| PGM-017 | 综合体和集团 h2 | PRD L55/`project_id` 强制、集团 PRD。通过 |
| PGM-018 | 多对多非隶属 | 业务口径通过；当前实现段已按 V410。**通过**（001 闭合） |
| PGM-019 | 各主体职责 | PRD 职责表；entity_role 活闭集写入绑设计当前表为 V410 三值。通过 |
| PGM-020 | 综合体主要功能 | PRD §7.1。通过 |
| PGM-021 | 业务意义 | PRD 定位段。通过 |
| PGM-026 | 商户主要功能/四类绑定/权责 | 绑设计+PRD 与 HTML `:492` 同旨；权责审批标为差距（诚实）。通过 |
| PGM-027 | 业务边界 | 验收包/PRD 商户不能看全局。通过 |
| PGM-031 | 外部经销商 | PRD §6.5.4、验收包 §2A。通过 |
| PGM-035 | 提成发放 | 验收包 L152、PRD 12B.5。通过 |
| PGM-040 | 权限 h2 | PRD 权限章。通过 |
| PGM-041 | 权限规则/角色装配/五类高危 | 集团 PRD §2–3、PRD 14A。group.view 审批属性已写 V394 闭合。**通过**（002 闭合） |
| PGM-042 | 职级规则 | PRD。通过 |
| PGM-043 | 外包与审计/导出脱敏 | PRD §4/§15A。通过 |
| PGM-044 | 桌台挂单经销权限 | PRD 无隐私桌台展开（BEQR-074 无实现面）；经销职责分离在 §6.5.4。无相反 |
| PGM-048 | 商户侧全开放 | 验收包边界。通过 |
| PGM-060 | 会员身份营销生物 | 本文件不立法会员积分。无实现面 |
| PGM-065 | 连接关系 | PRD §11。通过 |
| PGM-066 | 已拍板 | 集团视图/全中文/留痕等 PRD §12 已确认承接。通过 |

### 3.3 对抗式（grilling 自问自答，不派子代理）

Frontier 已结算，不向操作员发问。每问先假定文档仍错，用命令打假。

1. **活库还有 `parent_entity_id` 当当前字段吗？** V410:9 DROP；Jdbc INSERT 无该列；`BusinessEntityRecord` 无该字段；governance Java `parent_entity` 0 命中。绑设计字段表已无该行；差距段用「曾」。**闭合（001）**。
2. **`entity_role` 活闭集被写成 V354 五值或 OpenAPI 六值了吗？** 服务与 V410 CHECK = 三值。字段表写 V410 三值。OpenAPI 仍 `operation/contract/settlement/invoice/payment/other`，差距段标明契约缺口、不得当活闭集（009）。按 OpenAPI 生成 `entity_role=operation` 的 POST 仍 400——这是 M392 的活缺陷，本模块已登记且 B2 禁止改 yaml。**不升格**。
3. **「自营标注字段」还要新建吗？** U04-001 删除。main 无独立布尔。§6.9 现写合作方式 `self_operated`。`StaffRegistrationService` 已按该码分流。**闭合（003）**。变更日志 L965 历史「待办」已订正作废，不升。
4. **`analytics.group.view` 现在还写成 false 缺口吗？** 集团 PRD L110、验收包 G7/修订说明、PRD §12#15 均写 V394 已 TRUE。V394:16-18 实更；其后 V4* 无回写 FALSE。risk_level CHECK 已写 V394。**闭合（002）**。
5. **点集团 PRD 列出的 DELETE 会不会被当成可用删除入口？** 现标「恒返回 409 `ARCHIVE_APPROVAL_REQUIRED`，不是删除入口」并列出 lifecycle。`AccessService:479-481` 无条件 throw；lifecycle `:192` 五 action 均在 switch。**闭合（005）**。
6. **PRD 空间表还把 OpenAPI 当 createSpace 活闭集吗？** 导语改字典硬校验；表头「非活码」；parking 已补；脚注 mall/bar_table/facility 非字典活码。`createSpace:474-478` 确校验字典。V184 八码与脚注逐字同。表未删（B2 禁止删整表）。**闭合（004）**。按 mall POST 仍会被字典拒绝——文档已声明非活码，不构成假完成。
7. **绑设计 §5 API 是否指向不存在端点？** 五条路径均在 `PlatformController` `@RequestMapping("/api/v1/platform")`。`/active`、`/usages` 0 命中。此点通过。
8. **HTML 有没有规定绑定记录状态必须是 active/inactive/suspended？** 三份无此英文三态闭集。设计标为当前服务能力+差距。不升冲突。
9. **私人手机列表是否还把打卡放进去？** §5 L211、§6.9 L377 为「查看考勤」。全文「考勤打卡」仅余 §12B.2 标题（应对 L1445）与变更日志引用 BSM 条名。PGM L741 / BSM L930/L1865 为查看考勤。契约打卡仅 `staff_work_phone`。**闭合（008）**。
10. **税费检索句是否还写全库零命中 / 把 V217 算进第 5 类？** 集团 PRD L69 明确个税档≠第 5 类、动作码不是 permissions 行、`finance.tax_*` 仍缺。`rg finance.tax` 生产 Java 仅 `TAX_RULE_ADJUST_ACTION`。**闭合（006）**。
11. **evaluate 是否还写成孤立接口？** PRD §12#8 写员工域两处已调用、四域尚未。`rg reviews.evaluate` 仅 `StaffApprovalService:44,61`。**闭合（007）**。
12. **极端：并发双开绑定、无 project_id 的主体、把 mall 当活码编码、改 OpenAPI。** 双开非本文件可执行面；`business_entities` 无 `project_id` 绑设计已登记差距（诚实）；mall 已标非活码；改 OpenAPI/V184/源码为 B2 禁止项，**不升格**。
13. **三 HTML 自身是否需人闸修改？** 本模块 66 锚点全部命中。PGM V0.12 仅版本行+L741（操作员已批）。本席不申请改 HTML。

ponytail-review（只列不改）：

- 绑设计 L39「当前迁移」叠层含 V354 entity_role CHECK：shrink 可改成「V354 加 archive_reason；V410 换三角色 CHECK」。活表已正确，**不升缺陷**。
- PRD 变更日志 L965 历史「待办」句：delete 可整行改写，但已有订正括号，最懒是保留。
- §6.2.1 对照表：B2 禁止删除。Lean already on the live-vs-snapshot split.
- `net: 不把 KPI/权限公式的正当重复算膨胀；不建议本模块改 yaml。`

---

## 4. 已定稿 001–008 在当前树上的独立闭合复验

| ID | 当前文件关键句 | 命令事实 | 闭合 |
|---|---|---|---|
| 001 | 绑设计 L39/L45/L55/L153：V410 删父链、三角色、OpenAPI 旧六值不得当活闭集 | V410 DROP+CHECK 三值；Record/Jdbc 无 parent；OpenAPI `:29208-29215` 仍旧六值（登记 009） | 是 |
| 002 | 集团 PRD L26/L110、验收包 L181/L193、PRD L657：V394 已 TRUE / 已加 CHECK | V394:15 CHECK；V394:16-18 UPDATE group.view TRUE；其后无 FALSE 回写 | 是 |
| 003 | PRD L364 `self_operated` 既有字段；L965 订正作废待办 | `MERCHANT_COOPERATION_FORMS` 含该码；生产 0 独立布尔；注册服务按该码分流 | 是 |
| 004 | PRD L247 字典硬校验；L259 parking；L262 差异脚注；表未删 | V184 八码；OpenAPI 九值含 parking；createSpace 校验字典 | 是 |
| 005 | 集团 PRD L44–45 DELETE 恒 409 + lifecycle 五 action | Controller `:192`/`:204`；Service `:479-532` switch 五支 | 是 |
| 006 | 集团 PRD L69 个税档≠第 5 类、动作码非 permissions、finance.tax_* 仍缺 | V217 种 `staff.tax_bracket.manage`；SupervisorReview `:49,65`；permissions 无 finance.tax_* | 是 |
| 007 | PRD L642 员工域两处已接、四域未接 | `reviews.evaluate` 仅 StaffApprovalService 两处 | 是 |
| 008 | PRD L211/L377 查看考勤；§12B.2 未改 | PGM L741 / BSM L930/L1865 查看考勤；L1445 仍考勤打卡；punch 仅工作手机 | 是 |

009：OpenAPI 旧六值仍在，绑设计已登记且标明不得当活闭集。队列「否」XM→M392。本席不升格、不改 yaml。

010：文件名版本惯例、诚实差距、历史日志、字段字典 vs HTML 并存——仍观察。

---

## 5. Findings / 观察（本次临时编号）

**未定稿工程缺陷：无。** 添加项：无。HTML 修改申请：无。锁外判据：无。

观察（不升缺陷；与已定稿同根因已标明）：

### GR-M013-B5-01（同根 M013-010）

文件名版本与正文版本不一致：绑设计文件名 v0.1 / 正文 V0.09；集团 PRD 文件名 v1.0 / 正文 V1.01；验收包文件名 v0.2 / 正文 V0.04；治理 PRD 文件名 v0.1 / 正文 V0.04。仓库惯例，不单独开缺陷。

### GR-M013-B5-02（同根 M013-010）

变更日志历史行：验收包 L200「保留实现缺口」（2026-09-01）；PRD L965「代码轮新增待办」+当场订正作废。正文现行句已闭合，不升。

### GR-M013-B5-03（同根 M013-010）

绑设计 L57「当前 status 未实现完整词表」对五态+`archived_at` 偏宽；退出缓冲仍可能未在治理服务落地。差距方向正确，属诚实登记。

### GR-M013-B5-04（同根 M013-009）

OpenAPI `BusinessEntityRequest.entity_role` 仍旧六值。本模块已登记不得当活闭集。B2 禁止本模块改 yaml。不升格。

### GR-M013-B5-05（登记，非 002）

集团 PRD L27 临时授权 `status`「现有表无 CHECK」仍真（无 chk 约束）。非 V394 同批，不扩圈。

### GR-M013-B5-06（并入绑设计已有存档差距，不另开）

`business_entities.archive_reason` 为 V354 实列；`BusinessEntityRecord` 无该字段；Jdbc 对主体 INSERT 未写该列（商户存档走 merchants）。§4.3 已标完整存档/删除流程未在当前接口落地。

### GR-M013-B5-07（同根 M013-010）

PRD 修订说明仍述 Keystore 身份方案史；现行条目标「未立法 / 不得升为已确认规则」并【已废止/不得回灌】设备指纹作凭证定义。不升冲突。

### GR-M013-B5-08（同根 M013-010）

PRD L38 终端枚举「以字段字典为准」与 L655「最终业务边界仍以三份 HTML 为准」并存；现行十一端与 BEQR-150 一致。

### GR-M013-B5-09（过程）

当前整工作区 tree `2ff9d988…` ≠ B3 after `dcfc3a8c…`，差在 findings/ledger/本模块 raw。四文件 blob 与 after 全等。本席只审四文件当前内容。

### GR-M013-B5-10（实现文案，非本模块；B2 禁止改源）

`createSpace` 中文 400 写「铺位类型必须是铺位类型字典中的启用值」。文档未引用该句。不升本模块缺陷、不改源。

无应升被降项。B1 五条缺陷与席甲后补 006/007/008 在当前树均已闭合。

---

## 6. 命令证据

本模块无测试文件。未跑后端/前端测试套件。未 `source` 环境文件。未对 `complex_test*` 做写操作。未运行 `npm run verify:openapi`（契约自洽≠与 Java 白名单一致；本席直接读 yaml 枚举）。

```
CMD-LOCK: sha256sum docs/audit-2026-09/plan/criteria-lock.md
OUT: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md
EXIT: 0
CMD-LOCK-PY: python3 hashlib
OUT: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
EXIT: 0
CMD-LOCK-SSL: openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
OUT: SHA2-256(...)= b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
EXIT: 0
LOCK 字节: 889020

CMD-HEAD: git rev-parse HEAD && git rev-parse origin/main
OUT: 5692a89502833a423d465b3e2cb8834c9ad91b49 ×2
EXIT: 0

CMD-AFTER-TYPE: git cat-file -t dcfc3a8cc70a23e43ef3b47c668411f2c05af1b4
OUT: tree
EXIT: 0

CMD-BLOBS: git hash-object <4 files> vs git rev-parse dcfc3a8c:<path>
OUT: 四文件 work=after 全等
  binding ad294981d1fca52b35f7986adb1505aafcecd333
  group   e1dfa2e9434ac96a40249eba901997bb59da627c
  pack    dbdfc24501028d0e1cbbf130d33c461c467b8bfc
  prd     29d77349ae3eb2d49e950be7f4d17c274e771da4
EXIT: 0

CMD-TREE-NOW: GIT_INDEX_FILE=$TMP/index git add -A && git write-tree
OUT: 2ff9d98857d33531e8954f56d7ba1cfc38fed333
EXIT: 0
（≠ after；含 findings/ledger/raw 前进。临时索引已删，未动主索引。）

CMD-MAP: python 抽出 M013 → [...] 
OUT: COUNT 84 unique 84 DUPS [] BEQR 21 BSM 12 GEN 18 PGM 33
EXIT: 0

CMD-HTML-LOCK: 66 锚点来源行 vs 现行 HTML
OUT: hit 66 miss 0
EXIT: 0

CMD-V410: sed -n '9,19p' .../V410__m010_business_entity_topology_and_role_isolation.sql
OUT: DROP COLUMN IF EXISTS parent_entity_id;
     DROP CONSTRAINT IF EXISTS chk_be_entity_role;
     ADD CONSTRAINT ... CHECK (entity_role IN ('platform', 'project_company', 'management_company'));
EXIT: 0

CMD-ROLES: PlatformService.java:61
OUT: BUSINESS_ENTITY_ROLES = Set.of("platform", "project_company", "management_company");
CMD-COOP: :49-50 MERCHANT_COOPERATION_FORMS 含 self_operated
CMD-SUBJ: :53-55 SUBJECT_STATUSES 五码（无 archived 作为可设码）

CMD-RECORD: BusinessEntityRecord 无 parent / 无 archiveReason
CMD-PARENT-JAVA: rg parent_entity backend/.../governance → 空
CMD-JDBC: INSERT business_entities 列无 parent_entity_id

CMD-OPENAPI-ROLE: docs/openapi/openapi-p0.yaml:29208-29215
OUT: enum operation / contract / settlement / invoice / payment / other

CMD-OPENAPI-SPACE: :28994-29003
OUT: mall floor zone shop booth bar_table facility parking other

CMD-V184: INSERT 八码 building floor zone shop public_area parking booth other
EXIT: 0

CMD-CREATE-SPACE: PlatformService.java ~474-478
OUT: if (!repository.listActiveSpaceTypeCodes().contains(spaceType)) badRequest 铺位类型必须是铺位类型字典中的启用值

CMD-V394: :15 chk_permissions_risk_level (low/medium/high/critical)
          :16-18 UPDATE requires_approval=TRUE WHERE permission_code IN ('analytics.group.view', ...)
CMD-V4-WRITEBACK: rg analytics.group.view 于 V4* → 仅 V115 种子 FALSE 与 V394 TRUE 与 V180/V251 非回写
EXIT: 0

CMD-DELETE: AccessController.java:204 DeleteMapping；AccessService.java:479-481
OUT: throw ApiException.conflict("ARCHIVE_APPROVAL_REQUIRED", "岗位模板须先存档…")
CMD-LIFECYCLE: AccessController.java:192 PostMapping .../lifecycle
OUT: switch archive / restore / request_delete / approve_delete / reject_delete
CMD-PREFIX: AccessController @RequestMapping("/api/v1/access")

CMD-EVAL: rg reviews.evaluate backend/src/main/java
OUT: 仅 StaffApprovalService.java:44 与 :61
EXIT: 0

CMD-TAX: rg finance.tax 生产 sql/java（排除 Test）
OUT: 仅 SupervisorReviewService.java:49 TAX_RULE_ADJUST_ACTION = "finance.tax_rule.adjust"
     :65 tax_rule_adjustment_l3 + finance/tax/legal 平行线
CMD-V217: V217__payroll_tax_brackets.sql:45 staff.tax_bracket.manage high TRUE

CMD-GROUP: AnalyticsController.java:119 GET /group-overview
           :122 requirePermission("analytics.group.view"); :124 requirePlatformScope();
CMD-GROUPBY: AnalyticsService.java:900-906 project|entity 否则 VALIDATION_FAILED

CMD-PUNCH: openapi-p0.yaml:21400-21406 punchStaffAttendance x-terminal-allowed: staff_work_phone

CMD-SELFOP: rg selfOperated|is_self_operated 生产 java/sql（排除 Test）→ 0 字段；self_operated 在合作方式闭集与 StaffRegistrationService 分流

CMD-UQ: V010 uq_business_entity_bindings_active_merchant 存在
CMD-ACTIVE: PlatformService ACTIVE_BINDING_EXISTS 存在
CMD-CHANGE: action 仅 active/inactive/suspended

CMD-HTML-NOW:
  PGM L741 查看考勤（「考勤打卡」该文件 0 命中于低风险列表）
  BSM L930/L1865 查看考勤；L1445 考勤打卡从领取工作手机开始算
  BSM L1433 / PGM L578 标注为自营视同公司自有

CMD-ATTEND-PRD: rg 考勤打卡 四文件
OUT: 仅 PRD:725 §12B.2 标题；:960/:969 变更日志引用
CMD-VIEW-PRD: L211 L377 查看考勤

CMD-QUESTIONS: ls docs/audit-2026-09/questions/M013*
OUT: No such file or directory
EXIT: 2（无未答问询；该失败不是业务 finding）

CMD-DIRTY-HTML-OPENAPI-LOCK: git status --porcelain -- 三 HTML openapi 锁
OUT: 空
EXIT: 0
```

命令失败如实：`questions/M013*` glob 无文件（exit 2），表示无本模块问询文件，不是验证中断。临时索引 write-tree 成功后已 `rm -rf` 该 mktemp 目录。

---

## 7. 无实现面一览

| 判据 | 原因 |
|---|---|
| GEN-003 | 4 文件无测试 |
| GEN-007 | 4 文件无幂等键/并发实现 |
| GEN-012 | 4 文件无循环/连接/批处理实现 |
| GEN-013 | 4 文件无构建/CI/依赖清单 |
| GEN-014 | 4 文件无 SQL/Flyway 正文（引用迁移的「当前」真假走 GEN-016） |
| BEQR-074 | 不写隐私桌台权限点 |
| BEQR-189 | 不写服务点单管理费闭集 |
| BSM-005/006 | 无本文件可执行引导流/动态码实现 |
| PGM-060 | 不写会员积分/营销规则 |

---

## 8. 审计结论

**PASS**。

4 文件全文三线已跑，84 条已逐项核对，命令已实跑。001–008 在当前 after 树上独立复验闭合。009 维持跨模块否。010 维持观察。无新增未定稿工程缺陷，无添加项，无未答 questions，无 HTML 修改申请，无锁外判据。Grok 哈希行有效。未把 B2 禁止项升格。不存在有条件通过。

本票不等于双席 B5 或模块 PASS。未经席甲本步 PASS 且四值全等，不得改 ledger 为 PASS。

完成本步判据：指定路径非空且含本次完整报告；第一行为独立实算锁 SHA；正文无第二处同名独占锁行；未修源/HTML/锁/台账；未读 self-raw。
