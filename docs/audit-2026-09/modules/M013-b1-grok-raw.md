CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M013 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 修复前整模块全量审计
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 模块：M013 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第1段
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 FILES 4 文件，计划冻结有效内容行 1517；当前 `git ls-files -z -- <4 paths> | xargs -0 cat | wc -l` = **1517**（与计划冻结一致）
- 冻结判据：84 条（`M013 → [BEQR-063, BEQR-074, BEQR-143, BEQR-144, BEQR-145, BEQR-146, BEQR-147, BEQR-148, BEQR-149, BEQR-150, BEQR-151, BEQR-152, BEQR-153, BEQR-154, BEQR-155, BEQR-156, BEQR-157, BEQR-158, BEQR-159, BEQR-166, BEQR-189, BSM-004, BSM-005, BSM-006, BSM-007, BSM-009, BSM-028, BSM-029, BSM-072, BSM-074, BSM-114, BSM-115, BSM-120, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-002, PGM-003, PGM-004, PGM-005, PGM-006, PGM-007, PGM-008, PGM-009, PGM-010, PGM-011, PGM-012, PGM-013, PGM-014, PGM-015, PGM-016, PGM-017, PGM-018, PGM-019, PGM-020, PGM-021, PGM-026, PGM-027, PGM-031, PGM-035, PGM-040, PGM-041, PGM-042, PGM-043, PGM-044, PGM-048, PGM-060, PGM-065, PGM-066]`，与锁映射行逐字相同；COUNT=84 unique=84 MISSING=[] EXTRA=[]）
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 修复队列的临时编号：GR-M013-001、GR-M013-002、GR-M013-003、GR-M013-004、GR-M013-005
- 添加项（B1 只登记，不代用户回答 2.2；本批无）：无
- 只登记、建议队列「否」：观察见 §8（不升 finding）
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做写操作
- 本报告不表示全仓或发布门禁通过。M001–M012 及 U01–U18 已闭环，不重开。原 322 模块编号及旧结论不代表当前 471 模块完成。findings V1.83 仅作已登记边界背景。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令与 controller 优先于技能本体 |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；Arc `audit` 会派评审子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用** |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 删除测试：绑设计若删掉已删除列的「当前字段」表，复杂度回到 HTML 多对多与四类绑定本身；不派 DESIGN-IT-TWICE 子代理 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（sha256/列存在性/OpenAPI enum vs `BUSINESS_ENTITY_ROLES`/DELETE 恒冲突）再下结论；不改生产代码 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流（文档陈述→HTML 原句→V410/`PlatformService`/OpenAPI） |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；`verify:openapi` exit 0 ≠ 文档与 Java 枚举一致 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git、未直推、未安装 hook（安装会写仓库外配置，B1 禁止） |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本步验证均秒级，未起 monitor |
| domain.md | `docs/agents/domain.md` | 词表/三 HTML 最终解释权；未改 CONTEXT.md |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：本机 mattpocock 技能树未找到独立技能
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，V0.17 明确不用）
- Arc `audit` 技能会派评审子代理并另写 `docs/arc/audits/`：与双席/§7 冲突，**不调用**
- superpowers `using-superpowers` 要求技能优先于探索：与「禁止子代理 / 本提示指定 controller」冲突处，按用户指令收窄

明确不用（controller 划掉）：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、code-review（mattpocock）、ponytail-audit/debt/gain、MCP、第三主体。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 同值
- `criteria-lock.md` 版本 V0.11；判据总数 401
- 映射行 `M013 → [...]` 与开工口径 84 项逐字相同
- `plan/modules.md` M013 FILES 4 条与用户清单逐字相同
- 4 文件 `git ls-files -z -- <4 paths> | xargs -0 cat | wc -l` → **1517**
- 4 文件字节/SHA256 与用户固定清单逐文件 `match=True`（见 §2）

HEAD：`main` = `origin/main` = `ec735cac3806283a6c6ea7d452f3e7c9a180fa09`（`rev-parse HEAD` / `origin/main` / `merge-base` 三值相等）。本席未设 `GIT_INDEX_FILE`，未动主索引。

工作区：`git status --porcelain` = ` M docs/audit-2026-09/ledger.md`（M013 PENDING→IN_PROGRESS 的台账写入）。开工提示称「工作区干净」与此不完全一致；本 4 文件未改。本席只读，未再写 ledger。

ledger 只读核验（不改）：V3.15，573356 bytes，SHA256 `0b693142b5c15bd48370cf66021feae4c936406f3ad6d8c45acd4876161a7fb0`；M013 行 IN_PROGRESS、B3=0。findings V1.83，720813 bytes，SHA256 `c1471adf07f252f3523c631eee05d02ebd4e0ffbc162f66075af27ac10426554`，仅作已登记边界背景（M009 三角色/V410 删父链/六类合作方式/空间状态闭集；M010-002/003 entity_role 三值；U04-001 自营独立布尔=删除；业务地图 V0.14 考勤打卡以 L1445 为准），不代替本模块审计。

controller `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`；runbook V0.02 SHA256 `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`；modules.md V0.04 SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa`。

三份 HTML 只读、未改。锁内本模块 66 条 HTML 锚点来源行对本席读取的现行 HTML **全部命中**（抽样+脚本：BEQR-143 `:226`、BEQR-148 `:231`、BEQR-155 `:238`、BSM-004 `:566`、BSM-028 `:698`、PGM-018 `:415`、考勤 L1445 均 hit=True；66/66 miss=0）。

三 HTML 当前字节/SHA256（只读核验，本席未改）：

| 文件 | 字节 | SHA256 |
|---|---:|---|
| docs/business-enum-quick-reference-v0.1.html | 133961 | 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 |
| docs/business-system-map-for-operations-v0.1.html | （现行） | eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 |
| docs/platform-governance-map-for-operations-v0.1.html | 103015 | c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c |

业务地图哈希相对 M012 时代已变（PR #432 V0.14）；锁行号对本模块锚点仍命中。三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。BSM-001/PGM-001 未映射本模块，只作对照。

## 2. 4 文件覆盖表（全文已读；行=splitlines=`\n` 计数）

| # | 路径 | 行 | 字节 | SHA256 | 全文 |
|---|---|---:|---:|---|---|
| 1 | docs/business-entity-binding-development-design-v0.1.md | 154 | 12560 | fd715410a760c94d7cfabb976b4bab29c80e4e535401dafa75954f86e84ed9b4 | 已读 L1–L154（标题 V0.08） |
| 2 | docs/group-governance-rebuild-prd-v1.0.md | 110 | 18123 | f933b0fa6e0d9927c6b6498cfc3a69f61d7e5c8bbd54a0f869b4c6c32ee07666 | 已读 L1–L110 |
| 3 | docs/platform-governance-acceptance-pack-v0.2.md | 288 | 35023 | 6b7214c1e181f2467c178d19341cc75eabf8d7a7b335cdb32403f9eea0abb64a | 已读 L1–L288（含 §11 变更日志与 2026-08-28 补录，未截断） |
| 4 | docs/platform-governance-prd-v0.1.md | 965 | 144100 | e5bd6e18b2173b37919b358554be6c11ba13eaad56bb36321fbb774291785990 | 已读 L1–L965（标题 V0.03；未只读标题/搜索） |

合计 1517 行 / 209806 字节。用户固定清单逐文件 match。

## 3. 数据流（文档陈述 → HTML 原句 → main 实现事实）

本模块为文档与接口定义类。实现对照只读，不作修复对象。

| 文档声称 | HTML 原句（锁） | main 实现事实 |
|---|---|---|
| 四类主体=经营对象/运营/合同/结算，同一经营对象唯一绑定 | BEQR-143 `:226`；PGM-026 `:497` | `BusinessEntityBindingRecord`：`merchantId/operationEntityId/contractEntityId/settlementEntityId`；`uq_business_entity_bindings_active_merchant` 仍在 V010；创建写 `active`，已有 active → `ACTIVE_BINDING_EXISTS` |
| 物理项目与运营公司多对多，不是固定隶属链 | BEQR-144 `:227`；PGM-018 `:415` | V410:9 `DROP COLUMN IF EXISTS parent_entity_id`；`BusinessEntityRecord` 无 parent 字段；Jdbc INSERT 无该列 |
| 主体角色 | PGM-018/019 顶层平台 / 城市项目公司 / 域主管公司 | `PlatformService.BUSINESS_ENTITY_ROLES` = `platform/project_company/management_company`；V410 CHECK 同三值。**OpenAPI `BusinessEntityRequest.entity_role` enum 仍为 `operation/contract/settlement/invoice/payment/other`** |
| 合作方式六类 | BEQR-148 `:231`；BSM-028 | `MERCHANT_COOPERATION_FORMS` = `self_operated/lease/joint_operation/managed_operation/service_provider/other`。生产代码 **0** 处 `is_self_operated`/`selfOperated`（U04-001 删除已落地） |
| 商户/运营主体六态+退出缓冲 | BEQR-155 `:238` | `SUBJECT_STATUSES` 五码 + 归档走 `archived_at`；`PlatformService` 注释写明退出缓冲不在存档路径触发 |
| 绑定变更 | PGM-026 权责配置审批 | `POST .../business-entity-bindings/{id}/change` 只接受 `action∈{active,inactive,suspended}` + reason；不能提交新三主体组合（设计 L100 作差距登记，现仍真） |
| 主体 API | （实现证据） | GET/POST `/api/v1/platform/business-entities`、GET/POST `/business-entity-bindings`、POST `.../{id}/change` 均在 `PlatformController` 与 OpenAPI。无 `/active?businessObjectId=`、无 `/{id}/usages`（设计 L121 诚实） |
| 集团下钻 | PGM-017/066 集团视图；PGM-041 跨项目须先审批 | `GET /api/v1/analytics/group-overview` + `requirePlatformScope()` 存在；`analytics.group.view` 由 V115 种 `requires_approval=FALSE`，**V394 已改为 TRUE**，之后无回写 |
| 职位模板删除 | BSM-004 先存档再审批软删 | `DELETE /position-role-templates/{id}` **恒抛** `ARCHIVE_APPROVAL_REQUIRED`；真实路径 `POST .../lifecycle`（archive/request_delete/approve_delete） |
| 考勤打卡 | BSM L1445 | PRD §12B.2 三种领取方式+取最早，与 L1445 逐字同旨 |
| 空间类型 | PGM「空间和点位」L469 九类业务名词 | PRD §6.2.1 声称 OpenAPI `SpaceRequest.space_type` 为当前实现枚举；OpenAPI 含 `parking`；`createSpace` 实际校验 `space_type_dictionary` active 行；V184 种子为 building/floor/zone/shop/public_area/parking/booth/other |

`npm run verify:openapi` → **exit 0**（契约自洽）。该门禁**不**证明 OpenAPI enum 与 `PlatformService` 白名单一致。

## 4. 三线审计

### 4.1 第一性原理（每文件三问）

#### docs/business-entity-binding-development-design-v0.1.md（L1–L154）

1. **为何必须存在**：删掉则 BEQR-143/PGM-026 四类主体唯一绑定、BEQR-144/PGM-018 多对多协作、BSM-004 存档删除在开发设计层无对当前接口/表的诚实差距清单。有上位。
2. **声称是否执行**：自称「代码、迁移和接口只说明当前实现」。§5 所列五条 API 路径在 `PlatformController`/`openapi-p0.yaml` 均存在；唯一约束名、`ACTIVE_BINDING_EXISTS`、变更三态与 `PlatformService` 一致；「不存在 `/active` 与 usages」为真。但 §3.1 把 **V318 `parent_entity_id` 与 V354 含 HTML 没有的角色值写成「当前」**——V410 已删列并收三角色，声称的「当前」未执行。
3. **未验证前提**：把 V354 当活库形状；未把 V410、OpenAPI `entity_role` 六旧值、`BUSINESS_ENTITY_ROLES` 三值放进同一张「当前」表。

#### docs/group-governance-rebuild-prd-v1.0.md（L1–L110）

1. **为何必须存在**：PGM-017/066 集团视图、PGM-041 角色装配与五类高危、BEQR-151 最终权限。有上位。文内多次标明实现证据不取得效力，方向正确。
2. **声称是否执行**：V115/V169/V170 文件存在；`GET /api/v1/analytics/group-overview`、`requirePlatformScope()`、职位模板 CRUD、grant-requests、temporary-grants、staff-levels、role-assignments、`PUT /users/{id}/position` 均在 Controller。L109「`analytics.group.view` 当前 `requires_approval=false`」**已被 V394 证伪**。L43 所列 `DELETE .../position-role-templates/{id}` 作为「实际 Controller」关键端点，**首次正常调用恒 409 `ARCHIVE_APPROVAL_REQUIRED`**，真实闭环是未列入清单的 `/lifecycle`。
3. **未验证前提**：把 2026-08-04 的「实现缺口」登记当 2026-09-10 的当前事实；「实际 Controller」清单未随存档删除范式刷新。

#### docs/platform-governance-acceptance-pack-v0.2.md（L1–L288）

1. **为何必须存在**：产品设计验收清单，承接 PGM 模块入口与状态闭集，避免编码前文档互打。有上位。L18 自承不是接口/数据库验收。
2. **声称是否执行**：§7 状态表与 BEQR-154/155/156/157/158/159 及「试运营系统不管」一致；十一端与 BEQR-150 一致；七类红线与 PRD/PGM 同旨；G7 称集团视图已由集团治理 PRD + V115/V169/V170 承接——路径存在。G7「`analytics.group.view` 审批属性实现缺口」同步过期（同 GR-M013-003）。补录块对已废止闭集有【已废止/不得回灌】标记，未把历史冲突升回现行。
3. **未验证前提**：G7 依赖集团 PRD 的过期「当前实现」句；L204 变更日志仍把 D8 写成改成设备指纹（正文已降为未立法证据）——历史日志，不升缺陷。

#### docs/platform-governance-prd-v0.1.md（L1–L965，未截断）

1. **为何必须存在**：平台治理层产品需求正本之一，承接 PGM 治理顺序、四类绑定、多对多、权限、终端、留痕、考勤。删掉则 P0 产品边界无上层文件。有上位。
2. **声称是否执行**：组织关系 L7/L191 明确不建固定四层隶属链，与 BEQR-144/PGM-018 同旨。§12B.2 考勤与 BSM L1445 同旨（三种领取、取最早、不设宽限、当地时间）。合作模式六类与 BEQR-148 同旨。§6.9「在商户档案上标注为自营」有 HTML 上位；同句「**「自营」标注字段属代码轮新增待办**」把已落地的合作方式 `self_operated` 说成尚待新增字段，与 U04-001 删除独立布尔及 main 实现相反。§6.2.1 声称 OpenAPI space_type 是当前实现枚举，与字典硬校验及 OpenAPI 自身含 `parking` 的表不一致。
3. **未验证前提**：把 2026-08-07/08 的「代码轮新增待办」当仍待做；把 OpenAPI 空间枚举当活校验闭集。

### 4.2 顺从式（84 条逐项）

适用面=本 4 文件是否与该锚点原文一致、可执行声称是否指向存在对象。无本文件陈述面则记「无实现面」及原因，不把 HTML 沉默当冲突。

| ID | 本模块适用面 | 结论 |
|---|---|---|
| BEQR-063 | 高危动作（酒吧沽清等） | 集团 PRD §3 / 治理 PRD 14A 将高危业务列入五类强制审批，未发明相反沽清规则。酒吧双证据细节无本文件展开。**无相反。通过** |
| BEQR-074 | 隐私桌台 | 4 文件 0 命中「隐私桌台」。**无实现面**（原因=本段不写酒吧桌台权限点）。不升冲突 |
| BEQR-143 | 四类主体 | 绑设计 §1/§3.2、PRD §7.4/§12#19 与原文一致。通过（当前接口差距有登记） |
| BEQR-144 | 多对多非隶属链 | 绑设计 L22/L56、PRD L7/L191 业务口径正确。**FAIL**：仍把父链列写成当前实现（GR-001） |
| BEQR-145 | 城市项目公司 | PRD §5 城市项目公司排除清单照抄 PGM。通过 |
| BEQR-146 | 运营公司职责 | PRD 娱乐平台公司段照抄 PGM。通过 |
| BEQR-147 | 主要收入归属 | 本 4 文件不立法收入拆分公式；PRD 12C 股权/牌照分记与 PGM 同旨。无相反。通过 |
| BEQR-148 | 合作模式六类 | PRD L208/L292/L668 六类+「其他」须指定收费模板。**FAIL**：§6.9 另造「自营标注字段待办」（GR-002） |
| BEQR-149 | 职级 M1–M10 上中下 | PRD §6.10/§7.8/§12#6。通过 |
| BEQR-150 | 终端十一端 | PRD L38、验收包 §6 十一端（含独立经销商门户）。通过 |
| BEQR-151 | 最终权限计算 | PRD L399/L650、集团 PRD、验收包 §4 公式一致。通过 |
| BEQR-152 | 项目与敏感数据 | PRD §6.10/§12#20、验收包。通过 |
| BEQR-153 | 终端开关优先级 | PRD L655、验收包终端行。通过 |
| BEQR-154 | 综合体项目六态 | PRD §7.1/§10、验收包 §7。通过 |
| BEQR-155 | 商户六态+缓冲+试运营不管 | PRD §6.4/§7.4、验收包 §7、绑设计 L58。业务口径通过。设计「词表未实现」对五态+archived_at 已过宽（观察） |
| BEQR-156 | 合同八态+当地时间 | PRD §6.5/§7.5、验收包 §2A/§7。通过 |
| BEQR-157 | 员工五态 | PRD §7.6/§10、验收包 §7。通过 |
| BEQR-158 | 设备七态 | PRD §7.7/§10、验收包 §7。通过 |
| BEQR-159 | 权限五态 | PRD §7.8/§10、验收包 §7。通过 |
| BEQR-166 | 合同审批链 | PRD §6.5.1、验收包 G5。通过 |
| BEQR-189 | 管理费方式 | 4 文件不定义服务点单管理费闭集。**无实现面**。不升冲突 |
| BSM-004 | 存档/审批软删 | 绑设计 §2.5/§7、PRD §15C 与原文同旨。**FAIL 轻**：集团 PRD 把恒失败的 DELETE 写成实际删除入口（GR-005） |
| BSM-005 | 新建分步引导 | 验收包/PRD 不反对。无本文件实现。无实现面（产品清单级） |
| BSM-006 | 动态二维码 | 无本文件展开。无实现面 |
| BSM-007 | 平台管理员 vs 最高权限 | PRD §7.8.1 与原文同旨。通过 |
| BSM-009 | 基础管理入口 | 验收包/PRD 范围覆盖综合体/空间/商户/合同/员工/设备/权限。通过 |
| BSM-028 | 基础管理主要功能 | 绑设计+PRD 承接六类合作、四类绑定、多对多。通过（父链当前事实见 GR-001） |
| BSM-029 | 和谁有关 | PRD §11 连接表。通过 |
| BSM-072 | 商户管理/业态六值 | PRD L4/L32/L266 六值+单一归属。通过 |
| BSM-074 | 商户和谁有关 | 无相反。通过 |
| BSM-114 | 各类入口 | 验收包页面表、PRD §9。通过 |
| BSM-115 | 相互关系 | PRD §11。通过 |
| BSM-120 | 已确认规则 | PRD §12 已确认条与 PGM-066 对齐处成立。通过（本文件不负责 BSM-120 全表每一拍板的编码） |
| GEN-001 | 必要性 | 4 文件均有 HTML 上位。通过 |
| GEN-002 | 可执行正确性 | **FAIL**：GR-001/003/004/005（声称的当前接口/枚举/审批属性与 main 不一致） |
| GEN-003 | 测试有效性 | **无实现面**（原因=本 4 文件无测试代码）。不把未运行测试写通过 |
| GEN-004 | 信任边界 | 文档不实现校验器；绑设计记录创建直接 active 为差距。无本文件可执行输入面 |
| GEN-005 | 授权 | 绑设计 §6 权限码与 Controller 一致；集团 PRD `requirePlatformScope` 与代码一致。通过（文档层） |
| GEN-006 | 数据完整性 | 绑设计记录唯一约束存在（V010）。通过（引用真）。无本文件 SQL |
| GEN-007 | 并发幂等 | 文档不实现幂等键。无实现面 |
| GEN-008 | 接口与模式一致性 | **FAIL**：GR-001/002/004（父链列、entity_role 三套词表、自营待办字段、space_type） |
| GEN-009 | 错误处理 | 文档层把差距标为差距处诚实。DELETE 写成可用入口则伪成功（并入 GR-005） |
| GEN-010 | 安全 | 未引入密钥明文。通过 |
| GEN-011 | 可观测 | 文档要求留痕；不伪造已写审计事件。通过 |
| GEN-012 | 资源边界 | 无本文件循环/连接。无实现面 |
| GEN-013 | 构建配置 | **无实现面**（原因=无 package/CI/tsconfig） |
| GEN-014 | 迁移正确性 | **无实现面**（原因=本模块无 SQL 文件）。把历史迁移写成当前事实走 GEN-016（GR-001） |
| GEN-015 | 中文可用性 | 正文中文；代码标识符/单据号例外。通过（文档层） |
| GEN-016 | 文档可验证性 | **FAIL**：GR-001/002/003/004/005 |
| GEN-017 | 分类纪律 | 本席未把 HTML 沉默当冲突；未把业务发明标技术性。通过 |
| GEN-018 | 证据 | 类型 1 HTML 逐字 + 类型 4 命令。通过 |
| PGM-002 | 治理顺序五步 | PRD §2.2。通过 |
| PGM-003 | 模块入口 | 验收包/PRD 覆盖卡片主题。通过 |
| PGM-004 | 综合体和集团卡片 | 集团 PRD + PRD §8.2/§12#15。通过（实现缺口句见 GR-003） |
| PGM-005 | 空间和点位卡片 | PRD §6.2/§7.2 九类业务名词与 HTML L469 同。space_type 实现对照失败并入 GR-004 |
| PGM-006 | 商户和主体卡片 | 绑设计+PRD。通过 |
| PGM-007 | 合同和准入 | PRD §6.5。通过 |
| PGM-008 | 员工和手机 | PRD §6.9。通过（自营待办见 GR-002） |
| PGM-009 | 权限和职级卡片 | PRD §6.10。通过 |
| PGM-010 | 终端开关卡片 | PRD L38/L655、验收包。通过 |
| PGM-011 | 设备管理 | PRD §6.8/§7.7。通过 |
| PGM-012 | 商户侧 | 验收包边界+PRD。通过 |
| PGM-013 | 开放接口 | 验收包非范围/PRD 边界。无相反 |
| PGM-014 | 留痕复核 | PRD §6.11/§7.9。通过 |
| PGM-015 | 楼宇运营 | PRD 12A.4 保安团队。通过 |
| PGM-016 | 应急与隐私 | PRD §15B。通过 |
| PGM-017 | 综合体和集团 h2 | PRD L55/`project_id` 强制、集团 PRD。通过 |
| PGM-018 | 多对多非隶属 | 业务口径通过。**FAIL 当前实现段** GR-001 |
| PGM-019 | 各主体职责 | PRD 职责表。entity_role 活闭集未写入绑设计当前表（GR-001） |
| PGM-020 | 综合体主要功能 | PRD §7.1。通过 |
| PGM-021 | 业务意义 | PRD 定位段。通过 |
| PGM-026 | 商户主要功能/四类绑定/权责 | 绑设计+PRD 与 HTML `:497` 同旨；权责审批标为差距（诚实）。通过（父链当前事实除外） |
| PGM-027 | 业务边界 | 验收包/PRD 商户不能看全局。通过 |
| PGM-031 | 外部经销商 | PRD §6.5.4、验收包 §2A。通过 |
| PGM-035 | 提成发放 | 验收包 L152、PRD 12B.5。通过 |
| PGM-040 | 权限 h2 | PRD 权限章。通过 |
| PGM-041 | 权限规则/角色装配/五类高危 | 集团 PRD §2–3、PRD 14A。**FAIL**：把已修的 `analytics.group.view` 审批属性仍写成缺口（GR-003） |
| PGM-042 | 职级规则 | PRD。通过 |
| PGM-043 | 外包与审计/导出脱敏 | PRD §4/§15A。通过 |
| PGM-044 | 桌台挂单经销权限 | PRD 无隐私桌台展开（BEQR-074 无实现面）；经销职责分离在 §6.5.4。无相反 |
| PGM-048 | 商户侧全开放 | 验收包边界。通过 |
| PGM-060 | 会员身份营销生物 | 本文件不立法会员积分。无实现面 |
| PGM-065 | 连接关系 | PRD §11。通过 |
| PGM-066 | 已拍板 | 集团视图/全中文/留痕等 PRD §12 已确认承接。通过 |

### 4.3 对抗式（grilling 自问自答，不派子代理）

Frontier 已结算，不向操作员发问：

1. **活库还有 `parent_entity_id` 吗？** V410:9 DROP；Jdbc INSERT 无该列；`BusinessEntityRecord` 无该字段；OpenAPI 无 `parent_entity`。绑设计 L39/L46 仍列为当前字段。首次按该设计写 INSERT 必红。
2. **`entity_role` 活闭集是什么？** 服务与 V410 CHECK = 三值。OpenAPI = `operation/contract/settlement/invoice/payment/other`。绑设计说 V354 仍含 HTML 没有的值。三套词表。按 OpenAPI 生成的 POST `/business-entities` 带 `operation` → `PlatformService.requireBusinessEntityRole` 中文 400。首次正常集成即发生。
3. **「自营标注字段」还要不要新建？** U04-001 操作员答「删除」。main 无 `selfOperated` 列/接口。合作方式码 `self_operated` 已在 `MERCHANT_COOPERATION_FORMS`。PRD L361 待办会重建第二轴。
4. **`analytics.group.view` 现在要不要审批？** V394 `requires_approval=TRUE`；V394 之后无回写。集团 PRD L109 / 验收包 G7 仍写 false 缺口。
5. **点集团 PRD 列出的 DELETE 模板接口会怎样？** `AccessService.deletePositionRoleTemplate` 无条件 `throw ARCHIVE_APPROVAL_REQUIRED`。首次调用即冲突。真实路径 `/lifecycle` 未列入「实际 Controller」。
6. **PRD 空间枚举表能否指导 createSpace？** 服务硬校验字典 active `type_code`，不是 OpenAPI 闭集；表还漏 OpenAPI 已有的 `parking`。按该表编码会漏车位、并误把 OpenAPI 当活校验。
7. **绑设计 §5 API 是否指向不存在端点？** 五条路径均存在。此点通过。
8. **HTML 有没有规定绑定记录状态必须是 active/inactive/suspended？** 三份无此英文三态闭集。设计标为当前服务能力+差距，不升冲突。
9. **考勤是否与 V0.14 L1445 相反？** PRD §12B.2 与 L1445 同旨。通过。
10. **极端：并发双开绑定、无 project_id 的主体、税费权限检索句。** 双开非本文件可执行面；`business_entities` 无 `project_id` 绑设计已登记差距（诚实）；税费「全库零命中」检索句已被 `finance.tax_rule.adjust` 与 `staff.tax_bracket.manage` 证伪，但是否等于 HTML「税费规则」权限码不在本模块实现面——观察，不另造理论闸。

ponytail-review（只列不改）：

- `business-entity-binding-development-design-v0.1.md:L39,L46,L56,L154`: delete: 把已删除父链列与 V354 五值 CHECK 写成当前。改为 V410 后形状（无 parent；三角色）+ 标明 OpenAPI 漂移。
- `platform-governance-prd-v0.1.md:L361,L962`: delete: 「自营标注字段属代码轮新增待办」。改为合作方式=`self_operated` 即自营标注。
- `group-governance-rebuild-prd-v1.0.md:L109` 与验收包 G7: delete: 「当前 requires_approval=false」缺口句。改为 V394 已 TRUE，或删缺口。
- `group-governance-rebuild-prd-v1.0.md:L43`: delete: 把恒失败的 DELETE 当关键端点。改为 `/lifecycle`。
- `platform-governance-prd-v0.1.md:L245-259`: shrink: 「OpenAPI 即当前实现枚举」表。改为字典硬校验，OpenAPI 只作契约快照。
- `net: 删过期「当前」句约数十行；不把 KPI/权限公式的正当重复算膨胀。`

## 5. Findings（临时 ID，正式 ID 仅 B2 分配）

### GR-M013-001

- **文件:行号**：`docs/business-entity-binding-development-design-v0.1.md:39,46,56,154`（对照 `docs/openapi/openapi-p0.yaml:29205-29215`；`backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql:9,18-19`；`PlatformService.java:61,698-702`；`BusinessEntityRecord.java:6-16`；`JdbcPlatformRepository.java:47-53`）
- **完整原文**：
  - L39：「当前迁移：V005 基表 + V318 `parent_entity_id` + V354 `archive_reason` 与 `entity_role` CHECK。」
  - L46：`parent_entity_id` | `VARCHAR(64)`，松引用 | 当前代码中的父级关系
  - L56：「V318/V354 把 `parent_entity_id` 用作固定层级，且 V354 的角色白名单仍含 HTML 没有的代码值」
  - L154：「当前实现：V002、V005、V010、V318、V354」
- **业务影响**：开发设计自称说明「当前实现」。按该表建父链列或按 OpenAPI 六旧值 POST 主体，与 HTML 多对多（BEQR-144/PGM-018）及已合入的 V410/三角色校验直接撞车；文档把已删除实现写成活合同。
- **可复现命令/最小反例**：
  ```
  rg -n "parent_entity_id" backend/src/main/java/com/complex/platform/governance/*.java
  # 主代码 0 字段；仅历史迁移 V318/V354 与 V410 DROP
  sed -n '9,19p' backend/src/main/resources/db/migration/V410__m010_business_entity_topology_and_role_isolation.sql
  sed -n '29205,29215p' docs/openapi/openapi-p0.yaml
  # enum: operation, contract, settlement, invoice, payment, other
  ```
  最小反例：按 OpenAPI 生成 `entity_role=operation` 的 `POST /api/v1/platform/business-entities` → `requireBusinessEntityRole` 400「主体角色必须为 顶层平台/城市项目公司/域主管公司 之一」。
- **冻结判据**：GEN-016、GEN-008、GEN-002、BEQR-144、PGM-018、PGM-019
- **2.3 依据类型**：4（命令+源码/迁移/OpenAPI 原文）+ 1（HTML 多对多原句）
- **四分类**：细化（上位=BEQR-144/PGM-018「不是固定隶属链」及 PGM-018/019 三角色；文档未发明新隶属业务，而是把已删除实现当当前事实）
- **工程缺陷**：成立
- **建议入队**：是
- **无业务发明理由**：不适用（非技术性）
- **最小处置**：刷新 §3.1/§8「当前」快照：删除 `parent_entity_id` 当前字段行；`entity_role` 写成 V410 三值；列出 OpenAPI 旧六值与服务三值差异为契约缺口（或声明 OpenAPI 未对齐、不得当活闭集）；当前实现迁移补 V410。不改三 HTML。

### GR-M013-002

- **文件:行号**：`docs/platform-governance-prd-v0.1.md:361,370,962`
- **完整原文**：L361「**在商户档案上标注为自营的商户视同公司自有**，其员工按公司自有员工路径办理（……**商户档案的「自营」标注字段属代码轮新增待办**）。」L962 变更日志同句。
- **业务影响**：HTML「标注为自营」与六类合作方式之「自营」是同一口径（U04-001 已答删除独立布尔）。PRD 把「标注」说成尚待新增字段，会再引入可与 `cooperation_form=lease` 分叉的第二轴，员工注册路径与合同/结算所用合作方式再次分叉。
- **可复现命令/最小反例**：
  ```
  rg -n "selfOperated|is_self_operated|self-operated" backend/src/main --glob '*.java' --glob '*.sql'
  # 生产代码 0 命中（测试方法名除外）
  sed -n '49,50p' backend/src/main/java/com/complex/platform/governance/PlatformService.java
  # MERCHANT_COOPERATION_FORMS 含 self_operated
  ```
  HTML：`business-system-map-for-operations-v0.1.html:1433`「在商户档案上标注为自营的商户视同公司自有」；BEQR-148 合作模式闭集含自营。
- **冻结判据**：GEN-016、GEN-008、BEQR-148、PGM-026（合作方式六类）、BSM「新员工注册分两条路径」（锁映射经 BEQR-148/PGM-008 相关）
- **2.3 依据类型**：1 + 4
- **四分类**：细化（上位=HTML「标注为自营」+ 合作方式「自营」；待办把已有六类码说成新字段，属过期实现计划，不是新业务概念待 2.2）
- **工程缺陷**：成立
- **建议入队**：是
- **最小处置**：删除「代码轮新增待办」。写明自营标注=合作方式 `self_operated`（已有字段），不得另建独立布尔/接口。

### GR-M013-003

- **文件:行号**：`docs/group-governance-rebuild-prd-v1.0.md:109`；`docs/platform-governance-acceptance-pack-v0.2.md:182`（G7）
- **完整原文**：
  - 集团 PRD L109：「**实现待对齐（2026-08-04 F1 登记）**：跨项目汇总下钻属 §3 强制闸门第 2 类……按该强制闸门 `requires_approval` **应为 `true`**；当前实现为 `false`，……**属实现缺口**。」
  - 验收包 G7：「`analytics.group.view` 审批属性实现缺口按集团治理 PRD 登记，不反向改写业务口径」
- **业务影响**：把已闭合的实现缺口继续写成现行缺口，后续批次可能重复「补 TRUE」或错误地按 false 做授权设计。PGM-041 明文跨项目须先审批。
- **可复现命令/最小反例**：
  ```
  sed -n '16,18p' backend/src/main/resources/db/migration/V394__permission_risk_tiers_and_sensitive_codes_require_approval.sql
  rg -n "analytics.group.view" backend/src/main/resources/db/migration/V4*.sql
  # V394 之后无回写 FALSE
  ```
- **冻结判据**：GEN-016、GEN-002、PGM-041
- **2.3 依据类型**：4 + 1
- **四分类**：细化（上位=PGM-041「提权或跨商户跨项目」授出前须审批；文档未发明新权限，只是过期实现登记）
- **工程缺陷**：成立
- **建议入队**：是
- **最小处置**：两处改为「V394 已将 `analytics.group.view.requires_approval` 置 TRUE」，删除「当前为 false / 实现缺口」句；业务口径（应为 true）保留。

### GR-M013-004

- **文件:行号**：`docs/platform-governance-prd-v0.1.md:245-259`
- **完整原文**：L247「OpenAPI `SpaceRequest.space_type` 是当前实现枚举」；表列 mall/floor/zone/shop/booth/bar_table/facility/other，无 `parking`。
- **业务影响**：按该表当活闭集实现 `createSpace` 会（1）漏 OpenAPI 已有的 `parking`；（2）忽略服务层真正校验的 `space_type_dictionary`。空间对象与主体/商户经营范围绑定的入口会按错词表。
- **可复现命令/最小反例**：
  ```
  sed -n '28991,29003p' docs/openapi/openapi-p0.yaml   # 含 parking
  sed -n '474,475p' backend/src/main/java/com/complex/platform/governance/PlatformService.java
  # 硬校验字典 active type_code
  sed -n '20,32p' backend/src/main/resources/db/migration/V184__space_type_dictionary.sql
  ```
- **冻结判据**：GEN-016、GEN-008、GEN-002、PGM-005
- **2.3 依据类型**：4
- **四分类**：细化（上位=PGM「空间和点位」业务名词；实现枚举对照写错，不是新空间层级立法。HTML L469 九类业务名词 PRD 已列，本 finding 只打「OpenAPI=当前实现」假完成）
- **工程缺陷**：成立
- **建议入队**：是
- **最小处置**：删除或改写「OpenAPI 是当前实现枚举」；改为「活校验=空间类型字典；OpenAPI/本表仅为契约快照，以字典为准」。业务名词列可留。不在本模块改 OpenAPI/V184（非本 4 文件）。

### GR-M013-005

- **文件:行号**：`docs/group-governance-rebuild-prd-v1.0.md:43`
- **完整原文**：「**关键端点（实际 Controller）**」列表含 `DELETE /api/v1/access/position-role-templates/{id}`，无 `POST /api/v1/access/position-role-templates/{id}/lifecycle`。
- **业务影响**：按「实际 Controller」集成删除模板，首次正常调用即 409，不会走到 BSM-004 的存档→申请删除→最高权限审批。真实闭环被文档藏起来。
- **可复现命令/最小反例**：
  ```
  sed -n '204,208p' backend/src/main/java/com/complex/platform/access/AccessController.java
  sed -n '479,481p' backend/src/main/java/com/complex/platform/access/AccessService.java
  # throw ARCHIVE_APPROVAL_REQUIRED
  sed -n '192,202p' backend/src/main/java/com/complex/platform/access/AccessController.java
  ```
  最小反例：`DELETE /api/v1/access/position-role-templates/{id}` → 冲突「岗位模板须先存档，填写理由申请删除，经最高权限审批后软删除」。
- **冻结判据**：GEN-002、GEN-016、BSM-004
- **2.3 依据类型**：4 + 1
- **四分类**：细化（上位=BSM-004 先存档再审批软删；文档把已废直删入口写成实际可用）
- **工程缺陷**：成立
- **建议入队**：是
- **最小处置**：关键端点表删除或标注 DELETE 恒拒绝；补 `POST .../lifecycle` 及 action=archive|restore|request_delete|approve_delete|reject_delete。

## 6. 命令证据

```
CMD-HEAD: git rev-parse HEAD && git rev-parse origin/main && git merge-base HEAD origin/main
OUT: ec735cac3806283a6c6ea7d452f3e7c9a180fa09 ×3

CMD-LOCK: sha256sum docs/audit-2026-09/plan/criteria-lock.md
OUT: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

CMD-FILES: 四文件 python hashlib + splitlines
OUT: 与用户固定 154/12560/fd715410…、110/18123/f933b0fa…、288/35023/6b7214c1…、965/144100/e5bd6e18… 全 match
OUT: git ls-files -z -- <4> | xargs -0 cat | wc -l → 1517

CMD-STATUS: git status --porcelain
OUT: " M docs/audit-2026-09/ledger.md"

CMD-OPENAPI: npm run verify:openapi
OUT: exit 0 / "OpenAPI contract verification passed."
（不证明 entity_role 与 Java 白名单一致）

CMD-V410: sed -n '9,19p' .../V410__m010_business_entity_topology_and_role_isolation.sql
OUT: DROP parent_entity_id; CHECK (platform, project_company, management_company)

CMD-ROLE: PlatformService.java:61 BUSINESS_ENTITY_ROLES 三值
CMD-OPENAPI-ENUM: openapi-p0.yaml:29208-29215 operation/contract/settlement/invoice/payment/other
CMD-COOP: MERCHANT_COOPERATION_FORMS 含 self_operated；生产代码无 is_self_operated
CMD-V394: requires_approval=TRUE for analytics.group.view；V4* 无回写
CMD-DELETE: AccessService.deletePositionRoleTemplate 恒 throw ARCHIVE_APPROVAL_REQUIRED
CMD-HTML-LOCK: 66 条 HTML 锚点来源行 miss=0；BSM L1445 考勤 hit=True
CMD-LEDGER: V3.15 573356bytes 0b693142…；findings V1.83 720813bytes c1471adf…
```

未运行会清理共享库的脚本。未跑后端测试套件（本模块无测试文件；不把未运行测试写通过）。

## 7. 无实现面一览

| 判据 | 原因 |
|---|---|
| GEN-003 | 4 文件无测试 |
| GEN-012 | 4 文件无循环/连接/批处理实现 |
| GEN-013 | 4 文件无构建/CI/依赖清单 |
| GEN-014 | 4 文件无 SQL/Flyway 正文（引用迁移的「当前」真假走 GEN-016） |
| BEQR-074 | 不写隐私桌台权限点 |
| BEQR-189 | 不写服务点单管理费闭集 |
| BSM-005/006 | 无本文件可执行引导流/动态码实现 |
| PGM-060 | 不写会员积分/营销规则 |

## 8. 观察（不升 finding；无新增根因则不并入 §5）

1. 文件名版本与正文版本不一致：绑设计文件名 v0.1 / 正文 V0.08；验收包文件名 v0.2 / 正文 V0.03；治理 PRD 文件名 v0.1 / 正文 V0.03。本仓库惯例，不单独开缺陷。
2. 开工提示「工作区干净」与 `git status` 中 ledger 已改不一致；本 4 文件干净。
3. 绑设计 L58「当前 status 未实现完整词表」对五态+`archived_at` 过宽；退出缓冲期仍可能未在治理服务落地（`PlatformService` 注释明示不在存档路径触发）——缓冲期缺口若要修属于实现模块，本文件已标差距，不升本模块冲突。
4. 集团 PRD L68「全库无 finance.tax / tax_rule 命中」检索句已过时（现有 `finance.tax_rule.adjust` 动作与 `staff.tax_bracket.manage`）；是否等于 HTML「税费规则与税费调整」权限码不在本 4 文件实现面。
5. 集团 PRD L27「permissions.risk_level 现有表无 CHECK」已被 V394 `chk_permissions_risk_level` 证伪；与 GR-003 同批过期实现证据，不另开根因。
6. PRD L38 终端枚举「以字段字典为准」与 L655「最终业务边界仍以三份 HTML 为准」并存；现行十一端列表与 BEQR-150 一致，不升冲突。
7. PRD 变更日志仍保留 2026-06-07「P0 仅单级确认」历史行，后文 2026-07-28/08-04 已改写；属日志，不升缺陷。
8. 验收包补录与变更日志仍出现设备指纹/NFC 历史用语，现行条目标【已废止】或「不得升为必过」。
9. 绑设计 §4–§7 对创建直接 active、无审批链、无 usages、无主体完整 CRUD 的差距登记与现网仍一致，属诚实差距，不是缺陷。
10. `verify:openapi` 绿只证明契约文件自洽。
11. 理论极端（双开绑定、page>500）按铁律 6 收窄：非首次正常必撞，不新开闸。
12. 三 HTML 本身无本席建议人闸修改项。

## 9. 审计结论

**FAIL**。

4 文件全文三线已跑，84 条已逐项核对，命令已实跑。存在 5 条工程缺陷（建议入队是），无添加项、无未答 questions、无有条件通过。Grok 哈希行有效。任一缺口即不得 PASS。

完成本步判据：指定路径非空且含本次完整报告；第一行为独立实算锁 SHA；未修源/HTML/锁/台账；未读 self-raw。

## M013 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 41273 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，独立）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M013 FILES 4 / 判据 84）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `5692a89502833a423d465b3e2cb8834c9ad91b49`（PR #433 已合并：治理地图 V0.12）。本模块 4 文件指纹与 B1 覆盖表逐文件全等。B3=0。无源修复。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B1 分类。
- 本席 B2-r1：**PASS**（001–010 逐 ID 全 PASS；添加 0、不发 2.2；修复队列「是」=001–008，「否」=009 XM→M392、010）
- 正文实算锁：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 与 ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要复现只读。

此票只判断 findings.md V1.84 顶部「当前 M013 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M013-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 41273 | `f6e9f5adf5c5ff8a47552ee7958cda5cff697cad53a2db5825c580e9c21db61e` |
| `docs/audit-2026-09/modules/M013-b1-self-raw.md` | V0.01 | 27544 | `4be118bafd89ce483dc21ba233af01a515f2a248e1d2f4f0b8e7447b22b3285b` |
| `docs/audit-2026-09/findings.md` | V1.84 | 730800 | `fd94dbd724bb8a1ab8a34314940da9516a3ba6ffb2ffecd9500b6bb9b288c193` |

ledger 只读：V3.17，575679 bytes，SHA256 `fdb2ca098697c03c2f270749c3f3c25e80177353fdb4714bf7dd4e25dac1a759`；M013 IN_PROGRESS、B3=0。映射行 `M013 → [BEQR-063 … PGM-066]` COUNT=84 unique=84 MISSING=[] EXTRA=[]。4 文件 `git ls-files -z -- <4 paths> | xargs -0 cat | wc -l` = **1517**。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

三 HTML 只读核验（本席未改）：

| 文件 | 现行 | SHA256 |
|---|---|---|
| 业务地图 | V0.14 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66`（与 B1 同） |
| 治理地图 | V0.12 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1`（相对 B1 的 V0.11/`c4238afd…` 已变，PR #433） |
| 枚举速查 | V0.07 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`（与 B1 同） |

本模块 4 文件 SHA 与 B1 覆盖表全等：绑设计 `fd715410…ed9b4`、集团 PRD `f933b0fa…7666`、验收包 `6b7214c1…b64a`、治理 PRD `e5bd6e18…5990`。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核源/HTML/命令再接受或反驳；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：001 拆 009 是否丢 OpenAPI、002 是否漏同批句、006 检索句应否升队、008 现行 HTML 是否真冲突 |
| verification-before-completion | 声称前先跑只读哈希/行号/源码核对 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码 |
| codebase-design | 删除测试：绑设计深度落在 HTML 多对多与四类绑定，不落已删父链列 |
| ponytail-review | 只评最小删除/收窄，不落地。004 整表删除=非最懒；001 改 OpenAPI=非本模块；006 不立法权限码名 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到**；未冒充调用 |

### 完整差集（GR-001…005、OBS-01…12、席甲 CX-001…007、OBS-1…4）

漏并：无。两席 B1 全部临时 ID 仍落在 001–010。本席 GR-001…005 原意未改写。席甲独有 CX-003/005/006 本席 B1 未升或未发现，本票独立核源后分别归 006/007/008，不是照抄席甲分类。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M013-001 | GR-M013-001 + CX-M013-001（契约侧拆出见 009） | 细化/是/是 | **PASS（细化/是/是；最懒=刷新快照，不改 OpenAPI）** |
| M013-002 | GR-M013-003 + CX-M013-002 + OBS-05 | 细化/是/是 | **PASS（细化/是/是；五处现行陈述收全，未漏同批活句）** |
| M013-003 | GR-M013-002 + CX-M013-004 | 细化/是/是 | **PASS（细化/是/是；与 U04-001、BSM L1433 一致）** |
| M013-004 | GR-M013-004 + CX-M013-007 | 细化/是/是 | **PASS（细化/是/是；最懒=改导语+补 parking+注差异；不应删整表）** |
| M013-005 | GR-M013-005（席甲 B1 未发现，候选已核实） | 细化/是/是 | **PASS（细化/是/是；标恒拒绝+补 lifecycle 是最懒）** |
| M013-006 | CX-M013-003 + OBS-04（本席 B1 观察，本票独立升队） | 技术性/是/是 | **PASS（技术性/是/是；改一句检索句成立，不维持否）** |
| M013-007 | CX-M013-005（本席 B1 未登记） | 技术性/是/是 | **PASS（技术性/是/是；「孤立」已证伪）** |
| M013-008 | CX-M013-006（本席 B1 只核了 §12B.2） | 冲突/是/是 | **PASS（冲突/是/是；现行 HTML 逐字冲突成立）** |
| M013-009 | GR-M013-001 契约侧拆出（非观察） | 技术性（跨模块）/是/否（XM→M392） | **PASS（否合规；未写错 B1 观察）** |
| M013-010 | OBS-01…03/06…12 + 席甲 OBS-1…4 | 技术性/正向 /否/否 | **PASS（否；未把应升项留在观察）** |

冲突 1（008）。细化入队 5（001–005）。技术性入队 2（006、007）。技术性跨模块否 1（009）。观察否 1（010）。添加 0。

### 001 独立裁定（细化／是；最懒=V410 快照；契约侧拆 009 合规）

核源（只读）：绑设计 L39 仍写「当前迁移：V005 + V318 `parent_entity_id` + V354」；L46 字段表仍有 `parent_entity_id`「当前代码中的父级关系」；L56 把 V318/V354 父链与 V354 白名单写成现行差距；L154「当前实现」止于 V354。V410:9 `DROP COLUMN IF EXISTS parent_entity_id`，V410:18-19 CHECK 三值 `platform/project_company/management_company`；`PlatformService.BUSINESS_ENTITY_ROLES` 同三值；governance 包 Java `parent_entity` **0** 命中；OpenAPI `BusinessEntityRequest.entity_role` `:29208-29215` 仍 `operation/contract/settlement/invoice/payment/other`。

四分类：**细化**（与 GR-001 原意相同，不改用席甲「技术性」）。上位=BEQR-144/PGM-018「不是固定隶属链」及 PGM-018/019 三角色；同文件业务口径已禁止按父链推导隶属，缺陷是把已删除实现写成当前快照（GEN-016/GEN-008/GEN-002），不是新隶属立法，也不是与 HTML 业务句对打的冲突。

无业务发明：三角色与删父链已在 V410/M010 落地。最懒：§3.1/§8 按 V410 刷新、字段表删父链行、`entity_role` 写三值、差距段登记 OpenAPI 旧六值为契约缺口且不得当活闭集。禁止本模块改 OpenAPI/迁移/三 HTML。候选「V005 + V354 archive_reason/角色 CHECK + V410 删父链并收窄」可接受为历史叠层（V354 加列、V410 换 CHECK）；B3 不得把 V354 五值 CHECK 再写成活约束。

契约缺口拆 009「否（XM→M392）」**合规**：GR-001 原文即「列出 OpenAPI 旧六值为契约缺口（或声明未对齐、不得当活闭集）」且「不改三 HTML」；`docs/openapi/openapi-p0.yaml` 是 M392 唯一 FILES。001 仍在本模块文档登记缺口，009 只阻止本模块 B3 改 yaml。不是把观察升成跨模块项。

票：**PASS**。

### 002 独立裁定（细化／是；五处未漏同批活句）

核源：集团 PRD L109「当前实现为 `false`…属实现缺口」；L26「`risk_level` 现有表无 CHECK」（本席 OBS-05，B1 误记 L27；L27 是临时授权 status 另一表，**不是** V394 同批）；验收包 L181 G7「实现有已登记缺口」与 L193「审批属性缺口继续如实登记」；治理 PRD L654「GAP-20：`requires_approval=false` 待对齐，未完全闭环」。V394:16-18 已 `UPDATE … analytics.group.view … requires_approval = TRUE`，V394:15 `chk_permissions_risk_level`；其后 V4* 无回写 FALSE。

同批过期实现证据句盘点（4 文件全文检索 `analytics.group.view` / `GAP-20` / `requires_approval=false` / `现有表无 CHECK` / `实现缺口`）：

| 位置 | 是否现行「当前事实」句 | 归本 ID？ |
|---|---|---|
| 集团 PRD L109 | 是 | 是（GR-003 原核） |
| 集团 PRD L26 | 是（risk_level CHECK，V394 同迁移） | 是（OBS-05 并入，未另开根因） |
| 验收包 L181 / L193 | 是 | 是（席甲补点，本席 B1 只写了 G7） |
| 治理 PRD L654 | 是 | 是（席甲补点） |
| 验收包 L199 变更日志「保留实现缺口」 | 否（2026-09-01 历史行） | 否，留 010 |
| 集团 PRD L5 总则「若不满足则记待对齐」 | 否（条件句，非点名 false） | 否 |
| 集团 PRD L27 临时授权 status 无 CHECK | 否（非 V394、非 group.view） | 否，不顺手扩圈 |

未漏同批**现行**证据句。最小处置「五处改为 V394 已闭合、业务口径应为 true 保留」与 GR-003「两处改为 V394 已 TRUE，删除当前为 false / 实现缺口」同旨，只是收全了席甲多出来的验收包修订说明与 PRD GAP-20 及本席 OBS-05。不发明新审批规则。

票：**PASS**。

### 003 独立裁定（细化／是；与 U04-001、BSM L1433 一致）

核源：治理 PRD L361/L962「商户档案的「自营」标注字段属代码轮新增待办」。`questions/U04-001.md` 操作员答复首行精确「删除」独立布尔。BSM L1433 原文：「在商户档案上标注为自营的商户视同公司自有，其员工按公司自有员工路径办理。」`MERCHANT_COOPERATION_FORMS` 含 `self_operated`；生产 `src/main` `selfOperated`/`is_self_operated` **0** 命中。

四分类细化成立（GR-002 原意）：上位=HTML「标注为自营」+ BEQR-148 六类之自营；待办把已有合作方式码说成新字段，属过期实现计划，不是 2.2 添加。与 U04-001「删除的代价：员工注册路径改按合作方式＝自营派生」逐字同向。最小处置「自营标注＝合作方式 `self_operated`（既有字段），不新增独立字段/接口」无业务发明。席甲标技术性不采用。

票：**PASS**。

### 004 独立裁定（细化／是；最懒不是删整表）

核源：PRD L247「OpenAPI `SpaceRequest.space_type` 是当前实现枚举」；表 L251-259 八值 `mall/floor/zone/shop/booth/bar_table/facility/other`，无 `parking`。OpenAPI `:28994-29003` 九值（多 `parking`）。`PlatformService:474-478` `createSpace` 硬校验 `space_type_dictionary` active `type_code`。V184 种子：`building/floor/zone/shop/public_area/parking/booth/other`（与 OpenAPI 词表不同：无 mall/bar_table/facility，有 building/public_area）。

GR-004 原文：「删除**或改写**『OpenAPI 是当前实现枚举』；改为活校验=字典；OpenAPI/本表仅为契约快照；**业务名词列可留**；不在本模块改 OpenAPI/V184。」席甲 CX-007 只补 parking。候选合取：导语改字典硬校验 + 表补 parking + 注明字典码差异。这是最懒可行：保留 HTML 九类业务名词对照，去掉「OpenAPI=活闭集」假完成。

**不应删除整张 OpenAPI 对照表。** 整表删除会丢掉 PGM-005 业务名词细化，比改导语更重，且改写了 GR-004「业务名词列可留」。B3 约束（非阻断、不否决）：导语改「契约快照」后，表内 `mall/bar_table/facility` **不得再标成 `spaces.space_type` 活码**；须写明 V184 八码与 OpenAPI 九码差异。禁止本模块改 yaml/V184。

票：**PASS**。

### 005 独立裁定（细化／是；标恒拒绝+补 lifecycle 是最懒）

核源：集团 PRD L44「关键端点（实际 Controller）」列 `DELETE /api/v1/access/position-role-templates/{id}`，无 lifecycle。`AccessController:204-208` DELETE → `AccessService:479-481` 无条件 `throw ARCHIVE_APPROVAL_REQUIRED`。真实路径 `AccessController:192` `POST …/{id}/lifecycle`；`changePositionTemplateLifecycle` action 闭集 `archive/restore/request_delete/approve_delete/reject_delete`。

GR-005 原文允许「删除**或**标注 DELETE 恒拒绝」并补 lifecycle。标注比删行更懒：该 DELETE 端点仍存在，首次正常调用即 409，藏起来会让集成者找不到冲突来源。补一行 lifecycle 才能把 BSM-004 存档→申请删除→最高权限审批写进「实际 Controller」。不发明新删除流程。

票：**PASS**。

### 006 独立裁定（技术性／是；不维持 B1「否」）

本席 B1 OBS-04：集团 PRD L68 检索句已被 `finance.tax_rule.adjust` 与 `staff.tax_bracket.manage` 证伪，但「是否等于 HTML 税费规则权限码」不在本 4 文件实现面，故不升 finding。B2 只问：改一句检索句能否成立为技术性「是」。

独立核源：L68 原文「当前实现**无税费类 `requires_approval=true` 权限码**（全库 `*.sql`/`*.java` 检索无 `finance.tax` / `tax_rule` 命中）」。`SupervisorReviewService:49,65` 动作 `finance.tax_rule.adjust` 与策略 `tax_rule_adjustment_l3`（平行线 finance/tax/legal + 主线 top_authority）已在库。`permissions` 生产 SQL/Java **无** `finance.tax_*` 权限码行。V217 `staff.tax_bracket.manage`（`requires_approval=TRUE`，中文名「维护薪资税档」）是薪资个税档，**不是** HTML 第 5 类「税费规则与税费调整」（税率/税目、财务+税务+法务+老板）。

裁定：**升队，技术性／是。** 理由（独立，非照抄席甲）：

1. 与 GR-003 同类：本模块 PRD 把可复现的「当前实现」写成假句（GEN-016）。检索括号「全库零命中」已被 `finance.tax_rule.adjust` 证伪；首次把该 PRD 当现行缺口清单读，会以为仓库里没有 tax_rule 字符串。
2. 最小处置只改这一句登记，不在本模块补权限码、不立法新税种、不关闭 HTML 第 5 类。无业务发明。
3. 铁律 6：这是文档模块的首次正常阅读必撞的假完成句，不是理论闸。
4. B1 观察的「HTML 第 5 类落点仍缺」仍然成立，升队改的是假证据句，不是把第 5 类标闭合。

不维持「否」。

非阻断勘误（不否决）：候选事实栏「检索句与『无税费类 requires_approval=true 权限码』句**均已假**」过宽。假的是检索括号；「无第 5 类权限码落点」仍真。V217 不得写成第 5 类已闭合；`finance.tax_rule.adjust` 是复核动作码，不是 `permissions` 行。候选最小处置已写「finance.tax_* 权限码仍缺、待对齐范围收窄为权限码落点」，B3 按此写现状即可，禁止发明具体权限码清单或把个税档算进第 5 类。

票：**PASS**。

### 007 独立裁定（技术性／是）

本席 B1 未登记。核源：治理 PRD L639「当前 `SupervisorReviewService.evaluate` 为孤立评估接口，业务域动作前尚未调用」。`StaffApprovalService.java:44` 排班发布、`:61` 处罚上限均 `reviews.evaluate`；`SupervisorReviewController:47` 为 HTTP 入口。main/java 其余业务域无 `reviews.evaluate`（票务/酒吧/游乐/结算未接线）。

「孤立评估接口」已假；同句所列四域目标未变。四分类技术性成立：改现状句，不改 HTML 准入闸口规则，不把员工域接线推广成四域已闭合。最小处置「员工域两处已接线；票务/酒吧/游乐/结算动作前仍未调用（目标不变）」无业务发明。本席 B1 漏检不阻本票接受。

票：**PASS**。

### 008 独立裁定（冲突／是；现行 HTML 逐字核对）

本席 B1 对抗问 9 只核了 PRD §12B.2 与 BSM L1445 同旨（三种领取、取最早），判通过；**未核** §5/§6.9 私人手机六项列表。B2 按现行 HTML 逐字：

- 治理地图 **V0.12** L741：「员工自有手机只开放低风险功能（公告、我的工单、帮客人订位、**查看考勤**、请假、查自己薪资等）。」（「考勤打卡」在该文件 **0** 命中）
- 业务地图 **V0.14** L930 / L1865：私人手机可离场事项为「**查看考勤**」；L1445 仍为「**考勤打卡从领取工作手机开始算**」（全文「考勤打卡」仅余该处）
- 治理 PRD L211、L374：私人手机六项仍写「**考勤打卡**」，并声称引自 PGM「同一个员工 App…」条
- 契约 `punchStaffAttendance` `x-terminal-allowed` **仅** `staff_work_phone`

冲突成立：PRD 把打卡放进私人手机低风险列表，与现行 PGM L741、BSM L930/L1865 及 L1445 工作手机打卡口径直接对打。GEN-017 第一格即冲突，不能降细化。工程缺陷是。最小处置「两处考勤打卡→查看考勤，其余五项不动」=操作员已批的 HTML 改法，无业务发明。不改 §12B.2（与 L1445 仍同旨，本席 B1 该项观察保持）。不改三 HTML（已是 V0.12/V0.14 现文）。

票：**PASS**。

### 009 独立裁定（技术性跨模块／否；未写错 B1 观察）

009 **不是**观察项，是 GR-001 证据里的 OpenAPI 半边拆出。本席 OBS-10 原文只是「`verify:openapi` 绿只证明契约文件自洽」，仍留在 010，没有被写成 009。OpenAPI 六旧值与服务三值不一致是 GR-001 的可复现反例（按契约 POST `entity_role=operation` 首次即 400），缺陷成立；本模块 4 文件不含 yaml，队列「否」+ XM→M392（该模块 FILES 唯一=`docs/openapi/openapi-p0.yaml`，标题即「OpenAPI 契约闭合…第5段」）合规。本模块 B3 只在 001 差距段登记「见 009」，不改 yaml。

票：**PASS（否）**。

### 010 独立裁定（观察／否；未写错 B1 观察）

候选未把本席应入队项留在观察：OBS-04 已升 006、OBS-05 已并 002。未把 GR-001…005 降成观察。010 收纳的诚实登记与惯例项与 OBS-01/03/06/08/09/10 同旨；席甲 OBS-1 文件名版本、OBS-2 主体档案存档未落地、OBS-4 §1 表名零命中措辞合规——未写成缺陷。OBS-02（开工提示 vs ledger 脏）属过程，可丢。OBS-07 PRD 变更日志「P0 仅单级确认」历史行未点名，与验收包历史用语同类，维持否即可，不升队。OBS-11/12 过程/负向观察可丢。无应升被降。

票：**PASS（否）**。

### 分类 / 队列 / 允许的最小修复边界

本批可定稿（须席甲本批 B2 亦 PASS）。队列是=001–008；否=009、010。一次 B3-r1 只改本模块 4 文件，不改三 HTML、OpenAPI、迁移、Java、锁、台账。文档类按类型 4 最小命令举证，不强制造测试。

- 001：绑设计 §3.1/§8 刷新 V410；删 `parent_entity_id` 行；`entity_role` 三值；差距段写 OpenAPI 旧六值不得当活闭集（见 009）；不改 yaml。
- 002：五处现行句改 V394 已闭合；应为 true 的业务口径保留；L199 历史日志不改或只加「当时」也不强制。
- 003：L361/L962 待办改为合作方式 `self_operated`；变更日志一行订正；不新增字段/接口。
- 004：§6.2.1 导语改字典硬校验；表留业务名词、补 parking、注明与 V184 差异；**不删整表**；不改 yaml/V184。
- 005：DELETE 标恒拒绝；补 `POST …/lifecycle` 一行及五 action。
- 006：只改 L68 检索/现状句；第 5 类落点仍待对齐；不写权限码清单；V217 若提及须标「薪资个税档，非第 5 类」。
- 007：L639 现状句改员工域两处已接线、四域仍未调用。
- 008：L211/L374「考勤打卡」→「查看考勤」；§12B.2 不动。
- 009/010：本模块 B3 不修。

禁止：回灌父链；新建自营布尔；把 OpenAPI 当 createSpace 活闭集；把 DELETE 当可用删除；把 V217 或 `finance.tax_rule.adjust` 写成 HTML 第 5 类已闭合；私人手机列表保留打卡；本模块改 yaml/HTML。

改动文件自身版本递增。

### 结论

全批差集无漏并。001 最懒快照+009 否拆出合规。002 五处收全同批活句，未漏。003 与 U04-001 / BSM L1433 一致。004 不应删整表。005 标恒拒绝+lifecycle 最懒。006 本席独立把 OBS-04 升为技术性「是」（不维持否）；007/008 席甲独有项核源成立。009/010 未写错 B1 观察。逐 ID 001–010 全 **PASS**。本席 B2-r1 **PASS**。未经双 PASS 不得把候选当定稿，更不称模块 PASS。步骤仍 B2；M013 IN_PROGRESS；B3=0。

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M013 B1 Grok 独立审计原文（重开追加）

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；本席 FAIL 具否决权）
- 阶段：B1 整模块全量审计（Codex 预检发现问题后按 V0.17 完整双席重开；本次尚未 B3）
- 报告自身版本：V0.03（指定路径前缀 62958 bytes / 旧 V0.02 完整保留，本次追加；只采信本段）
- 模块：M013 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／文档与接口定义／第1段
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 FILES 4 文件，计划冻结有效内容行 1517；当前 `git ls-files -z -- <4 paths> | xargs -0 cat | wc -l` = **1524**（与用户口径一致；冻结 1517 不改）
- 冻结判据：84 条（`M013 → [BEQR-063, BEQR-074, BEQR-143, BEQR-144, BEQR-145, BEQR-146, BEQR-147, BEQR-148, BEQR-149, BEQR-150, BEQR-151, BEQR-152, BEQR-153, BEQR-154, BEQR-155, BEQR-156, BEQR-157, BEQR-158, BEQR-159, BEQR-166, BEQR-189, BSM-004, BSM-005, BSM-006, BSM-007, BSM-009, BSM-028, BSM-029, BSM-072, BSM-074, BSM-114, BSM-115, BSM-120, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-002, PGM-003, PGM-004, PGM-005, PGM-006, PGM-007, PGM-008, PGM-009, PGM-010, PGM-011, PGM-012, PGM-013, PGM-014, PGM-015, PGM-016, PGM-017, PGM-018, PGM-019, PGM-020, PGM-021, PGM-026, PGM-027, PGM-031, PGM-035, PGM-040, PGM-041, PGM-042, PGM-043, PGM-044, PGM-048, PGM-060, PGM-065, PGM-066]`，与锁映射行逐字相同；COUNT=84 unique=84 BEQR21/BSM12/GEN18/PGM33 MISSING=[] EXTRA=[]）
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 修复队列的临时编号：GR-M013-011
- 添加项（B1 只登记，不代用户回答 2.2；本批无）：无
- 只登记、建议队列「否」：观察见 §8（含 009 跨模块 OpenAPI 仍真、不重编号；010 既有边界不伪称新需求）
- 未修源码、未改三 HTML、未改计划/findings/ledger/锁、未 fetch/commit/push/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本次 `M013-b1-self-raw.md` 新追加、未问操作员、未对任一 `complex_test*` 库做写操作、未建脚本/临时报告
- 历史：原 Claude Fable5.1 / Grok B5-r1 双 PASS 与 B3 累计 1 保留、未清零；旧 001–008 已修转否，当前十项否。本次独立核未发现 001–008 回归；009/010 不重开为新需求
- 本报告不表示全仓或发布门禁通过

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令与 controller 优先于技能本体 |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；Arc `audit` 会派评审子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用** |
| grilling | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证；本体「等用户回答」和「dispatch sub-agent」作废 |
| grill-me | 同套件 `grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 删除测试：绑设计「当前创建/变更」段若只删假句不补现行写路径，调用方会按 HTML 审批链去调一个已改写的接口；不派 DESIGN-IT-TWICE 子代理 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 红环=文档「直接写 active」句 vs 工作树 `effective ? "active" : "inactive"`；不改生产代码、不跑会写库的测试 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流（文档「当前」→工作树 `PlatformService`→既有单测方法名） |
| verification-before-completion | superpowers 同名 | 先跑 sha256/wc/sed/rg 再声称；未跑 Java 测试（文档审计不强制、且会写库） |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度：假的「当前」句应改写而非加闸；不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git、未直推、未安装 hook |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本步验证均秒级，未起 monitor |
| domain.md | `docs/agents/domain.md` | 词表/三 HTML 最终解释权；未改 CONTEXT.md |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：本机 mattpocock 技能树未找到独立技能（仅有 grilling / grill-me）
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，V0.17 明确不用）
- Arc `audit`：与双席/§7 冲突，**不调用**
- diagnosing-bugs 要求「把假说列表出示给用户」：与「不等操作员」冲突，自问自答
- grilling 要求 wait-for-user 与 dispatch sub-agent：作废
- i-have-adhd：本席无独立 SKILL.md 可核，不冒称已调用；输出按 controller 收窄为无寒暄

明确不用（controller 划掉）：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、code-review（mattpocock）、ponytail-audit/debt/gain、MCP、第三主体。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本段第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 同值
- ledger 顶部同值；`LOCK-AMEND-COUNT: 1`
- `criteria-lock.md` 版本 V0.11
- 映射行 `M013 → [...]` 与开工口径 84 项逐字相同
- `plan/modules.md` M013 FILES 4 条与用户清单逐字相同
- 4 文件 `git ls-files -z -- <4 paths> | xargs -0 cat | wc -l` → **1524**
- `wc -l` 分文件：153 / 113 / 289 / 969 = 1524

controller `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`；runbook V0.02 SHA256 `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`；modules.md V0.04 SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa`。

HEAD：`rev-parse HEAD` = `e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main` = `495285da120e6c8c0cc85659c1a1501baf2b294d`（两值不等）。本席未设 `GIT_INDEX_FILE`，未动主索引。用户指令：现行代码读主工作区工作状态（含已批准未提交修复），不是只读 HEAD blob。

工作区：`git status --porcelain` 含大量 M009 等已批准未提交修复（`PlatformService.java` 等）；**本模块 4 文件 porcelain 为空**（未改）。本席只读，未写 ledger/findings。

三份 HTML 只读、未改。本模块关键锚点对本席读取的现行 HTML 命中（见下表）。三 HTML 本身未发现需铁律 4 人闸修改的原文错误。此前「考勤打卡→查看考勤」是用户已批准历史落地（治理地图 V0.12 L741），不是本次 HTML 新授权。

三 HTML 当前字节/SHA256（只读核验，本席未改）：

| 文件 | SHA256 |
|---|---|
| docs/business-enum-quick-reference-v0.1.html | 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 |
| docs/business-system-map-for-operations-v0.1.html | eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 |
| docs/platform-governance-map-for-operations-v0.1.html | a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 |

HTML 锚点抽核（`splitlines()[n-1]` 含锁词）：

| 锁来源 | 命中 |
|---|---|
| BEQR-143 `:226` 四类主体 | True |
| BEQR-144 `:227` 经营主体关系 | True |
| BEQR-148 `:231` 合作模式 | True |
| BEQR-155 `:238` 商户/运营主体状态 | True |
| BSM-004 `:566` 配置数据可以存档 | True |
| BSM-028 `:698` 主要功能 | True |
| BSM L1445 考勤打卡从领取工作手机开始算 | True |
| BSM L1865 查看考勤 | True |
| PGM-018 `:415` 多对多协作 | True |
| PGM-026 `:492` 主要功能 | True |
| PGM L741 查看考勤 | True；该文件「考勤打卡」命中 0 |

## 2. 4 文件覆盖表（全文已读；行=`wc -l`）

| # | 路径 | 行 | SHA256 | 全文 |
|---|---|---:|---|---|
| 1 | docs/business-entity-binding-development-design-v0.1.md | 153 | f78f63b3bffe8bbd842f4c4866010abaed652f333d7cdd31fbbb2cc1a510fb7f | 已读 L1–L153（标题 V0.09） |
| 2 | docs/group-governance-rebuild-prd-v1.0.md | 113 | e60983ccb2884319e36b4059203096582adc0c76c15b239dcdfbc3abfd37b36b | 已读 L1–L113（标题 V1.01） |
| 3 | docs/platform-governance-acceptance-pack-v0.2.md | 289 | ca94985de3792578c3436914133c17a99812f5fb5b32acd2ac45a422139def56 | 已读 L1–L289（含 §11 与 2026-08-28 补录，未截断） |
| 4 | docs/platform-governance-prd-v0.1.md | 969 | aa73e6c76e8389757a635806c893b9a9e8e8539166fd51a8719d3e0bcb1f1413 | 已读 L1–L969（标题 V0.04；未只读标题/搜索） |

合计 1524 行。实现对照只读工作树，不作修复对象。

## 3. 数据流（文档陈述 → HTML 原句 → 工作树实现事实）

| 文档声称 | HTML 原句（锁） | 工作树实现事实 |
|---|---|---|
| 四类主体=经营对象/运营/合同/结算，同一经营对象唯一绑定 | BEQR-143 `:226`；PGM-026 `:492` | `BusinessEntityBindingRecord` 四列仍在；`uq_business_entity_bindings_active_merchant` 仍在 V010（仅 `status='active'`）。服务层另有 `hasUnfinishedBusinessEntityBinding`：active **或** inactive 且 `effective_to IS NULL` |
| 物理项目与运营公司多对多，不是固定隶属链 | BEQR-144 `:227`；PGM-018 `:415` | V410:9 `DROP COLUMN IF EXISTS parent_entity_id`；`chk_be_entity_role` 三值 platform/project_company/management_company；`BUSINESS_ENTITY_ROLES` 同三值。绑设计 §3.1 已按 V410 写。OpenAPI `entity_role` 仍旧六值（M013-009，不重编号） |
| 合作方式六类 | BEQR-148 `:231` | `MERCHANT_COOPERATION_FORMS` 含 `self_operated`；PRD §6.9 已写「自营＝self_operated」（001–008 未回归） |
| 商户/运营主体六态+退出缓冲 | BEQR-155 `:238` | `SUBJECT_STATUSES` 五码 + 归档走存档；绑设计 L58 仍登记词表/缓冲未闭环（诚实差距，不升） |
| 绑定创建「直接写 active」 | PGM-026 权责配置；调整只对生效后业务有效 | **现网已变**：`createBusinessEntityBinding` L778–797 `effectiveFrom.isAfter(today)` → 写 `inactive` 且不 `syncMerchantEntities`；未到期槽位拒绝码仍为 `ACTIVE_BINDING_EXISTS` 但文案为「生效或待生效」。见 GR-M013-011 |
| 绑定变更「只改启用/停用/暂停」 | 同 PGM-026 | **现网已变**：active→非 active 恒 `BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW`；已结束 `BINDING_PERIOD_ENDED`；未到生效日 `BINDING_NOT_EFFECTIVE`；替换已有三主体同码。change 仍不能提交新三主体组合。见 GR-M013-011 |
| 主体 API 五条 | （实现证据） | GET/POST entities、GET/POST bindings、POST change 仍在 `PlatformController`。工作树另有 `POST /business-entities/{entity_id}/projects`（V427 `business_entity_projects`）。绑设计 §5 未列该端点；`business_entities` 表仍无 `project_id` 列（列陈述仍真）。见 OBS |
| 集团下钻 | PGM-017/066；PGM-041 跨项目须先审批 | `GET /api/v1/analytics/group-overview` + `requirePlatformScope()`；V394 已置 `analytics.group.view` `requires_approval=true`。集团 PRD L110 / 验收包 G7 / 治理 PRD §12#15 已写闭合（002 未回归） |
| 职位模板删除 | BSM-004 | `AccessService.deletePositionRoleTemplate:481-482` 仍无条件抛 `ARCHIVE_APPROVAL_REQUIRED`；集团 PRD L44–45 已标恒 409 + lifecycle（005 未回归） |
| 考勤 | BSM L1445 打卡；PGM L741 查看考勤 | PRD §12B.2 与 L1445 同旨；§5/§6.9 私人手机六项为「查看考勤」（008 未回归） |
| 空间类型 | PGM「空间和点位」 | PRD §6.2.1 活校验＝V184 字典；表含 parking 并注明 mall/bar_table/facility 非活码（004 未回归） |
| 税费第 5 类 | PGM-041 | 集团 PRD L69 已收窄为 `finance.tax_*` 权限码待对齐；V217 标薪资个税档非第 5 类（006 未回归） |
| evaluate 接线 | 治理顺序 PGM-002 | PRD §12#8：员工域两处已接线、四域未接；`StaffApprovalService:44,61` 仍 `reviews.evaluate`（007 未回归） |

## 4. 三线审计

### 4.1 第一性原理（每文件三问）

#### docs/business-entity-binding-development-design-v0.1.md（L1–L153）

1. **为何必须存在**：删掉则 BEQR-143/PGM-026 四类主体唯一绑定、BEQR-144/PGM-018 多对多、BSM-004 存档删除在开发设计层无对当前接口/表的诚实差距清单。有上位。
2. **声称是否执行**：自称「代码、迁移和接口只说明当前实现」。§3.1 V410 三角色/删父链与工作树一致；§5 五条路径仍存在；无 `/active` 与 usages 为真。**§3.2 L80 / §4.1 L90 / §5 L117 把创建写成「直接写 active、已有 active 时拒绝」——工作树首次用未来 `effectiveFrom` 创建即写 `inactive` 且不投影商户三主体；已有待生效 inactive 再创建仍 409 `ACTIVE_BINDING_EXISTS`。L80/L99/L118 把变更写成「只改启用/停用/暂停」——对一条已 active 绑定 POST `action=inactive` 首次即 409 `BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW`，并不改状态。**
3. **未验证前提**：把 B3-r1 当时的「创建直接 active / change 只改三态」当 2026-09-14 工作树事实；未把 M009 未提交的 `createBusinessEntityBinding`/`changeBusinessEntityBinding` 写路径放进同一张「当前」表。

#### docs/group-governance-rebuild-prd-v1.0.md（L1–L113）

1. **为何必须存在**：PGM-017/066 集团视图、PGM-041 角色装配与五类高危、BEQR-151 最终权限。有上位。文内多次标明实现证据不取得效力。
2. **声称是否执行**：V115/V169/V170 文件存在；关键端点含 DELETE 恒 409 + lifecycle；`analytics.group.view` 已写 V394 TRUE；税费句已收窄。与工作树 AccessService/V394/SupervisorReview 一致。
3. **未验证前提**：本文件「当前实现」句经抽核未再把已闭合项写成缺口。

#### docs/platform-governance-acceptance-pack-v0.2.md（L1–L289）

1. **为何必须存在**：产品设计验收清单，承接 PGM 模块入口与状态闭集。有上位。L18 自承不是接口/数据库验收。
2. **声称是否执行**：§7 状态表与 BEQR-154/155/156/157/158/159 及「试运营系统不管」一致；十一端与 BEQR-150 一致；G7 已写 V394 闭合。补录块对已废止闭集有【已废止/不得回灌】。
3. **未验证前提**：L200/L206 变更日志历史「保留实现缺口 / P2」是当时记录，正文 G7 已覆盖；不升缺陷。

#### docs/platform-governance-prd-v0.1.md（L1–L969，未截断）

1. **为何必须存在**：平台治理层产品需求正本之一。删掉则 P0 产品边界无上层文件。有上位。
2. **声称是否执行**：组织关系不建固定四层隶属链，与 BEQR-144/PGM-018 同旨。§12B.2 考勤与 BSM L1445 同旨。合作模式六类与 BEQR-148 同旨。§6.9 自营＝`self_operated`。§6.2.1 字典硬校验。私人手机「查看考勤」。GAP-20 已闭合。§12#8 evaluate 员工域已接线。
3. **未验证前提**：产品需求「当前实现」句经抽核未把已修项写回待办。本文件不是绑定写路径的「当前 API」表，不承担 GR-M013-011 的假完成句。

### 4.2 顺从式（84 条逐项）

适用面=本 4 文件是否与该锚点原文一致、可执行声称是否指向存在对象。无本文件陈述面则记「无实现面」及原因，不把 HTML 沉默当冲突。工作树代码只作文档可执行声称的证据。

| ID | 本模块适用面 | 结论 |
|---|---|---|
| BEQR-063 | 高危动作（酒吧沽清等） | 集团 PRD §3 / 治理 PRD 14A 将高危业务列入五类强制审批，未发明相反沽清规则。酒吧双证据细节无本文件展开。**无相反。通过** |
| BEQR-074 | 隐私桌台 | 4 文件 0 命中「隐私桌台」。**无实现面**（本段不写酒吧桌台权限点）。不升冲突 |
| BEQR-143 | 四类主体 | 绑设计 §1/§3.2、PRD §7.4/§12#19 与原文一致。通过（当前写路径差距见 011，不改 HTML 唯一绑定口径） |
| BEQR-144 | 多对多非隶属链 | 绑设计 L22/L56、PRD L7/L191 业务口径正确；V410 已落地且文档已刷新。通过 |
| BEQR-145 | 城市项目公司 | PRD §5 城市项目公司排除清单与原文同旨。通过 |
| BEQR-146 | 运营公司职责 | PRD 娱乐平台公司段照抄 PGM。通过 |
| BEQR-147 | 主要收入归属 | 4 文件不展开票款名目表；无相反规则。**无实现面/无相反** |
| BEQR-148 | 合作模式六类 | PRD §6.5.1/§6.9 六类+「其他」须指定收费模板；自营＝`self_operated`。通过 |
| BEQR-149 | 职级 M1–M10 | PRD §6.10/§12#6 同旨。通过 |
| BEQR-150 | 终端十一端 | 验收包 §6/PRD §2.1 十一端闭集同旨。通过 |
| BEQR-151 | 最终权限计算 | 集团 PRD / 治理 PRD §6.10 公式同原文。通过 |
| BEQR-152 | 项目与敏感数据 | PRD §2A/§7.8 隔离与敏感数据清单同旨。通过 |
| BEQR-153 | 终端开关优先级 | 验收包 §4 终端开关行 / PRD §12#16 同旨。通过 |
| BEQR-154 | 综合体项目六态 | 验收包 §7 / PRD §10 草稿…归档。通过 |
| BEQR-155 | 商户六态+缓冲 | 验收包 §7 / PRD §7.4 / 绑设计 L58 同旨；实现词表差距诚实登记。通过 |
| BEQR-156 | 合同状态+当地时间 | 验收包 §7 / PRD §6.5 同旨。通过 |
| BEQR-157 | 员工五态 | 验收包 §7 / PRD §10。通过 |
| BEQR-158 | 设备七态 | 验收包 §7 / PRD §10。通过 |
| BEQR-159 | 权限五态 | 验收包 §7 / PRD §10。通过 |
| BEQR-166 | 合同审批链 | 验收包 G5 / PRD §6.5.1 主线+平行线。通过 |
| BEQR-189 | 管理费方式 | 4 文件不立法服务点单管理费区间。**无实现面** |
| BSM-004 | 存档删除 | 绑设计 §2.5 / 集团 PRD lifecycle / PRD §15C 同旨。通过 |
| BSM-005 | 分步引导 | 4 文件不写新建弹窗交互。**无实现面** |
| BSM-006 | 动态二维码 | 4 文件 0 命中刷新间隔。**无实现面** |
| BSM-007 | 平台管理员 vs 最高权限 | PRD §7.8.1 同旨。通过 |
| BSM-009 | 基础管理入口 | 验收包/PRD 覆盖项目/空间/商户/合同/员工/设备/权限。通过 |
| BSM-028 | 基础管理主要功能 | 四文件承接多对多、六类合作、七类红线、城市项目公司排除。通过 |
| BSM-029 | 和谁有关 | PRD §11 与后续系统关系同旨。通过 |
| BSM-072 | 商户专区 | PRD/验收包商户侧边界；不展开专区五项实现。无相反。通过 |
| BSM-074 | 商户和谁有关 | 同 BSM-072。通过 |
| BSM-114 | 各类入口 | PRD 私人手机六项＝查看考勤，与 L741/L1865 同旨；打卡仍在工作手机 §12B.2。通过 |
| BSM-115 | 子系统必须连在一起 | PRD §11 / PGM-065 承接。通过 |
| BSM-120 | 已确认规则 | 4 文件不把已拍板项改成相反。通过 |
| GEN-001 | 必要性 | 4 文件均有 HTML 上位。通过 |
| GEN-002 | 可执行正确性 | **FAIL**：绑设计把 POST create/change 写成可执行当前行为，工作树首次正常照做即偏离（GR-011） |
| GEN-003 | 测试有效性 | 文档模块无测试面；不强制造测。不适用本文件。通过（豁免） |
| GEN-004 | 信任边界 | 无本文件实现面。通过 |
| GEN-005 | 授权 | 绑设计 §6 权限码与集团 PRD 高危闸同旨。通过 |
| GEN-006 | 数据完整性 | 绑设计登记唯一约束仍在 V010。通过 |
| GEN-007 | 并发幂等 | 未为极端并发补需求。通过 |
| GEN-008 | 接口一致性 | OpenAPI entity_role 旧六值仍真，已由 009 XM 登记，本模块差距段已写「不得当活闭集」。不重开。通过（本模块文档侧） |
| GEN-009 | 错误处理 | 绑设计仍写 `ACTIVE_BINDING_EXISTS` 码名，码仍存在但触发面已扩到待生效；属 011 事实过期的一部分 |
| GEN-010 | 安全 | 无本文件引入的泄露/注入。通过 |
| GEN-011 | 可观测 | 无相反。通过 |
| GEN-012 | 资源边界 | 无本文件无界放大。通过 |
| GEN-013 | 构建配置 | 无。通过 |
| GEN-014 | 迁移 | 绑设计引用 V002/V005/V010/V354/V410 文件均存在；V427 未写入该「当前迁移」句（OBS，列级无 project_id 仍真） |
| GEN-015 | 界面 | 文档无 UI 实现。通过 |
| GEN-016 | 文档可验证性 | **FAIL**：L80/L90/L99/L117/L118 把旧写路径写成当前事实（GR-011） |
| GEN-017 | 四分类 | 本席按冲突→细化→技术性→添加走完；011 落细化。通过（审计分类） |
| GEN-018 | 四类证据 | 011 依据=工作树源码+既有测试方法原文（类型 4）；HTML 原文（类型 1）。本条对模块文档：假「当前」句把历史当事实，**FAIL** 并入 011 |
| PGM-002 | 治理顺序五步 | PRD §2.2 同序。通过 |
| PGM-003 | 模块入口 | 验收包 §4 / PRD §9。通过 |
| PGM-004 | 综合体和集团 | 集团 PRD + PRD §8.2。通过 |
| PGM-005 | 空间和点位 | PRD §6.2/§6.2.1。通过 |
| PGM-006 | 商户和主体 | 绑设计+PRD §6.4/§7.4。通过 |
| PGM-007 | 合同和准入 | PRD §6.5。通过 |
| PGM-008 | 员工和手机 | PRD §6.9「查看考勤」。通过 |
| PGM-009 | 权限和职级 | PRD §6.10。通过 |
| PGM-010 | 终端开关 | PRD §12#16 / 验收包。通过 |
| PGM-011 | 设备管理 | PRD §6.8/§7.7。通过 |
| PGM-012 | 商户侧 | 验收包边界 / PRD。通过 |
| PGM-013 | 开放接口 | 4 文件只声明不发明对方规则。通过 |
| PGM-014 | 留痕复核 | PRD §6.11/§7.9。通过 |
| PGM-015 | 楼宇运营 | 验收包范围含物业/招商，不展开实现。无相反 |
| PGM-016 | 应急与隐私 | PRD §12A/§15。通过 |
| PGM-017 | 当前一个项目未来集团 | 绑设计 L23 / 集团 PRD §4 / PRD §12#15。通过 |
| PGM-018 | 多对多+三角色 | 绑设计 §3.1 已 V410。通过 |
| PGM-019 | 各主体职责 | PRD 照抄娱乐平台公司等。通过 |
| PGM-020 | 集团主要功能 | 集团 PRD §4 下钻。通过 |
| PGM-021 | 业务意义 | 无相反。通过 |
| PGM-026 | 四类绑定+权责配置 | 业务口径在绑设计 §1–§2 / PRD §12#19 与 HTML 同旨。**当前实现句**把「尚未实现审批链」说成「change 仍能改三态」已假——分类细化缺陷见 011，不把 HTML 权责规则判冲突 |
| PGM-027 | 商户边界 | PRD/验收包同旨。通过 |
| PGM-031 | 外部经销商 | 验收包 §2A / PRD §6.5.4。通过 |
| PGM-035 | 提成发放 | PRD 帮客人订位/四条钱。通过 |
| PGM-040 | 单一功能授权 | PRD §12#10。通过 |
| PGM-041 | 权限规则+五类高危 | 集团 PRD §3 / PRD 14A；V394 已闭合跨项目审批属性。通过 |
| PGM-042 | 职级规则 | PRD §12#6。通过 |
| PGM-043 | 外包与审计 | PRD §4。通过 |
| PGM-044 | 桌台/经销权限 | 4 文件不展开桌台权限点。**无实现面** |
| PGM-048 | 商户侧全开放 | 同 PGM-027。通过 |
| PGM-060 | 会员营销生物信息 | PRD §2A.6 后台配置不能改 HTML 口径。通过 |
| PGM-065 | 连接关系 | PRD §11。通过 |
| PGM-066 | 已拍板集团视图 | PRD §12#15 / 验收包 G7 已承接。通过 |

顺从式汇总：84/84 均有适用记录。失败判据：GEN-002、GEN-016、GEN-018（并入同一工程缺陷 GR-011）。其余通过或无实现面。

### 4.3 对抗式（grilling 结构自问自答）

grilling 本体 wait-for-user / sub-agent 作废。本席自问自答；事实已用类型 4 命令核过。

❓ **Q1** — **「当前创建直接写 active」是否仍是工作树事实？**
工作树 `PlatformService.createBusinessEntityBinding`：`effectiveFrom` 缺省=今天；`effectiveFrom.isAfter(today)` → status=`inactive` 且跳过 `syncMerchantEntities`。既有测试方法名 `futureBindingDoesNotChangeCurrentMerchantSubjects` 断言 status=`inactive` 且商户三主体仍 null。
➡️ 否。文档 L80/L90/L117 已假。

❓ **Q2** — **「已有 active 时拒绝」是否覆盖首次正常再创建？**
`requireUnfinishedBindingSlot` → `hasUnfinishedBusinessEntityBinding`：`status='active' OR (status='inactive' AND effective_to IS NULL)`。未来生效的 inactive 也会 409，码名仍 `ACTIVE_BINDING_EXISTS`，文案已改为「生效或待生效」。
➡️ 文档少写待生效槽位。首次：先 POST 未来日期，再 POST 第二条即 409，即使库里 0 条 active。

❓ **Q3** — **「change 只改三态」对已生效绑定是否成立？**
`changeBusinessEntityBinding`：若 existing.status==active 且 action≠active → `BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW`，不写库。测试 `activeBindingCannotBeDisabledToReplaceFinancialSubjects` 对 inactive/suspended 均 throw。
➡️ 否。照文档对 active 绑定 POST inactive，首次即 409，状态不变。

❓ **Q4** — **这是 HTML 冲突还是文档相对实现过期？**
HTML PGM-026 要求财税主体变更走权责配置审批、只影响未来业务。代码拒绝无审核的停用/换主体，更靠近 HTML。文档业务口径仍写「须审批、尚未实现完整链」——后半「尚未完整链」仍真；假的是「当前接口会把状态改成 inactive/suspended」。
➡️ 四分类第一格冲突不成立（未与 HTML 明文对打）。细化：上位=PGM-026/BEQR-143。工程缺陷=是（GEN-016）。

❓ **Q5** — **会不会把 009/010 伪称新需求？**
OpenAPI entity_role 旧六值仍在 `openapi-p0.yaml:29205-29215`，绑设计 L55 已登记 M013-009。不新开。010 历史观察「创建直接 active 与现网一致」是 B5 时的诚实性判断，现已过期——过期的是观察所依据的实现，不是把 010 改写成新 ID。新证据走 011。
➡️ 不重编号。

❓ **Q6** — **V427 项目关联是否单独立项？**
`business_entities` 表仍无 `project_id` 列（L56 列陈述仍真）。工作树有 `business_entity_projects` + `POST /business-entities/{id}/projects`。照文档 POST 创建主体（不带 project_id）仍可走 `creationProject(..., true)` 用当前项目 scope，**不是**「文档正常照做必失败」。缺列关联表属诚实不全，不是假完成句。
➡️ 观察，不升 finding。不把后台项目关联改进纳入本模块修复队列。

❓ **Q7** — **HTML 自身有没有要交 Codex 的原文错误？**
PGM L741 / BSM L1865「查看考勤」、BSM L1445「考勤打卡从领取工作手机开始算」内部一致；PRD 已跟。本轮未发现需铁律 4 改 HTML 的原文错误。
➡️ 不上报 HTML 修改。

❓ **Q8** — **极端并发/未提交竞态是否立项？**
`lockMerchant` 已在 create/change。为竞态补需求违反铁律 6。
➡️ 不立项。

对抗式收口：唯一工程缺陷 GR-M013-011。无添加项。无 HTML 人闸。

## 5. 临时 finding

### GR-M013-011

- **文件:行号**：`docs/business-entity-binding-development-design-v0.1.md:80`、`:90`、`:99`、`:117`、`:118`
- **原文及更长上下文**：
  - L78–L80：唯一约束段之后：「当前没有 `project_id`、`binding_no`、`version`、`created_by`、`reviewed_by`、`reviewed_at`，也没有 `business_entity_binding_usages` 表。**当前创建接口直接写 `active`，已有 active 时拒绝；状态变更接口只改启用、停用或暂停**，尚未实现 HTML 要求的主体权责配置审批、新旧关系切换和下游统一引用闭环。」
  - L88–L90：§4.1 首次绑定目标流程 1–3 之后：「**当前差距：`POST /api/v1/platform/business-entity-bindings` 直接创建 active 记录**，没有草稿、审批和复核状态。」
  - L99：§4.2 之后：「当前差距：`POST /api/v1/platform/business-entity-bindings/{id}/change` **只改变现有记录状态**，不能提交新的三主体组合，也没有上述审批链和历史版本切换。」
  - L117–L118：§5 当前 API 表：「POST | **直接创建 active 绑定；已有 active 时返回 `ACTIVE_BINDING_EXISTS`**」；「POST change | **将状态改为 active、inactive 或 suspended，并填写原因**」
- **正常触发/影响**：文档正常照做。
  1. `POST /api/v1/platform/business-entity-bindings` 且 `effectiveFrom` = 今天+10 日：文档承诺 status=`active` 且（按旧路径）投影商户主体；工作树写 `inactive`、不 `syncMerchantEntities`（`PlatformService.java:778-797`；测试 `futureBindingDoesNotChangeCurrentMerchantSubjects`）。
  2. 上述待生效记录仍在时再 POST 一条：文档承诺「仅已有 active 才拒绝」；工作树 409 `ACTIVE_BINDING_EXISTS`「该商户已存在生效或待生效的主体绑定」（`:957-959`；`JdbcPlatformRepository.hasUnfinishedBusinessEntityBinding:592-601`）。
  3. 对一条 `status=active` 且 `effective_to IS NULL` 的绑定 `POST .../change` `action=inactive`：文档承诺改为 inactive；工作树 409 `BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW`「停用生效中的主体绑定须经过财税权责变更审核」，行不变（`:831-834`；测试 `activeBindingCannotBeDisabledToReplaceFinancialSubjects`）。
- **最小拟处置**：只改本文件「当前」句与 §5 表，版本 V0.09→V0.10。写明：创建按 `effectiveFrom` 相对 UTC 当天决定 `active`（并 sync）或 `inactive`（不提前 sync）；未完成槽位=active 或（inactive 且 effective_to 空）→ `ACTIVE_BINDING_EXISTS`；change 仍只接受三态枚举，但对 active 停用/暂停、未到生效日启用或结束、已结束期间重开、替换已有财务三主体分别 409（码名按源码）；「不能提交新三主体组合、没有完整权责审批链/草稿」仍真。不改 Java/迁移/OpenAPI/三 HTML。不把拒绝当成已实现的审批流。
- **四分类**：冲突？否（未与 HTML 明文对打；HTML 仍要求权责审批，文档业务段仍写须审批）。细化？是（上位 PGM-026 `:492` 四类唯一绑定+公司主体与权责配置；BEQR-143 `:226`）。技术性？不进入（已能指向上位）。添加？否。
- **工程缺陷是否成立**：是（GEN-016/GEN-002/GEN-018；并 GEN-009 触发面过期）
- **冻结 ID**：GEN-016、GEN-002、GEN-018、PGM-026、BEQR-143
- **2.3 依据**：类型 4（工作树 `PlatformService.java:756-867,945-960` 与 `PlatformServiceTests.java:752-766,861-873,929-942` 原文；`JdbcPlatformRepository.java:592-601`；V010 unique index 仍仅 active）+ 类型 1（PGM-026 / BEQR-143 HTML 原文，用于上位与「非冲突」判定）。未跑 Java 测试（文档审计不强制；不写库）。

「尚未实现完整审批链」本身仍真，不单独立项。本项只打假的「当前接口行为」。

## 6. 与历史 001–010 的差集（防重复）

| 历史 ID | 本轮核 | 处置 |
|---|---|---|
| 001 V410 父链/三角色 | 绑设计 L39/L45/L55 已写 V410；工作树 V410 文件存在、`BUSINESS_ENTITY_ROLES` 三值 | 未回归。不重开 |
| 002 V394 group.view | 集团 PRD L110、验收包 G7、PRD §12#15 已闭合；V394 SQL 仍含 `analytics.group.view` | 未回归 |
| 003 自营待办 | PRD §6.9 已 `self_operated`；变更日志 L965 订正句仍在 | 未回归 |
| 004 空间字典 | PRD §6.2.1 字典八码+parking+非活码说明 | 未回归 |
| 005 DELETE/lifecycle | 集团 PRD L44–45；AccessService:481-482 仍恒 409 | 未回归 |
| 006 税费检索句 | 集团 PRD L69 已收窄 | 未回归 |
| 007 evaluate | PRD §12#8 员工域已接线；StaffApprovalService:44,61 | 未回归 |
| 008 查看考勤 | PRD L211/L377；PGM L741 | 未回归。HTML 改法不作为本次新授权 |
| 009 OpenAPI entity_role | yaml:29208-29214 仍六旧值 | **仍真**。本模块不重编号、不改 yaml、不进本模块队列 |
| 010 观察包 | 其中「创建直接 active 与现网一致」所依据的实现已变 | **不把 010 改写成新需求**。新证据=011 |

## 7. 观察（建议队列「否」）

- **OBS-01**：V427 `business_entity_projects` + `POST /api/v1/platform/business-entities/{entity_id}/projects` 已在工作树；绑设计 L56「当前表没有 project_id」对 `business_entities` 列仍真；§5 未列关联端点。照文档创建主体不带 project_id 仍可走当前项目 scope，非首次必失败。不升。不把 M009 项目关联改进纳入本模块队列。
- **OBS-02**：OpenAPI `BusinessEntityRequest.entity_role` 旧六值＝009，已在绑设计 L55 登记。verify:openapi 本轮未跑（非本模块文件）。
- **OBS-03**：V010 唯一索引仍只约束 `status='active'`；待生效槽位是服务层 EXISTS，不是新 DB 唯一约束。不发明「待生效也必须有部分唯一索引」需求。
- **OBS-04**：`HEAD`≠`origin/main`；审计输入=工作树（含未提交 `PlatformService.java`）。4 文档 porcelain 干净。
- **OBS-05**：文件名 `platform-governance-prd-v0.1.md` vs 正文 V0.04 等为仓库惯例（010 已收）。
- **OBS-06**：绑设计 L75「服务只接受 active/inactive/suspended」作为 action 闭集仍真；假的是「接受后会改成该状态」。已并 011，不另开。
- **OBS-07**：验收包补录/变更日志历史「P2」「实现缺口」已标当时；正文 G7 已闭合。不升。
- **OBS-08**：集团 PRD §1 表名零命中不取得效力措辞仍合规。
- **OBS-09**：绑设计 L58 status 词表/缓冲未闭环方向仍真（`SUBJECT_STATUSES` 无归档码，归档走 `archived_at`）。
- **OBS-10**：无 `/active`、无 usages、无主体完整 CRUD、change 不能提交新三主体——这些「不存在」声称仍真。
- **OBS-11**：本席未跑 `npm run verify:openapi` / Java 测试；文档可执行声称用源码与测试方法原文（类型 4）证明。不把未跑测试折成 PASS。
- **OBS-12**：三 HTML 本轮无自身原文错误需人闸。

## 8. PASS/FAIL

单席 PASS 必须：4 文件覆盖、84 判据核对、三线、四类证据、无未答 questions、无未定稿缺陷、哈希行有效。

本席：**FAIL**。

- 4 文件全文覆盖：是
- 84 判据：是（失败项并入 GR-M013-011）
- 三线：是
- 未定稿缺陷：GR-M013-011（细化，工程缺陷是）
- 添加项 / questions：无
- HTML 修改申请：无
- 哈希行：本段首行独立实算，与 sidecar/ledger 顶部同值

B1 结束。不修、不定新队列、不等操作员。下游 B2 归并后再定正式 ID。

## M013 B2-r2 整批归并复核（Grok 乙席）

- 文件自身版本：V0.04（本段追加；上方前缀 98356 bytes / SHA256 d638a7134bdb7396534deb764548c3796c5c301761b42af97bbc36c0c323312a 的 B1 V0.03 及更早前缀原样保留，不得覆盖；本段不写独占锁哈希字段行，最新完整 B1 锁段保持在 V0.03 段首）
- 席位：Grok 独立复核席乙（固定 grok-4.6 xhigh；FAIL 具否决权）。步骤：B2-r2 整批候选复核，不是 B1 重审、不是 B3、不是修复、不是模块 PASS。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`；runbook V0.02；criteria-lock V0.11；modules.md V0.04（M013 FILES 4 / 判据 84）
- 正本：`/home/james/complex-management` main 工作树。`HEAD`=`e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main`=`495285da120e6c8c0cc85659c1a1501baf2b294d`（两值不等，与 B1 V0.03 相同；本席读工作树，未设 GIT_INDEX_FILE，未动主索引）。本模块 4 文件 porcelain 空。
- 只读：未改 findings/ledger/三 HTML/锁/sidecar/任何业务文件；未 fetch/commit/push/deploy；未写脚本/日志/临时报告；未派子代理、未调 MCP。唯一写入=本指定路径追加。
- 本席 B2-r2：**PASS**（逐 ID 全 PASS；添加 0、不发 2.2；修复队列「是」=011–016，「否」=001–010 保持、017 XM→M348）。此票只通过候选归并，不是模块 PASS。须席甲本批 B2 亦 PASS 才能定稿。不覆盖上方 B1 V0.03 **FAIL** 原文。

正文实算锁（本段不写独占标签行）：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar 文件内容同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `modules/M013-b1-grok-raw.md` 前缀（本席 B1 最新独占锁段，本段追加前） | V0.03 | 98356 | `d638a7134bdb7396534deb764548c3796c5c301761b42af97bbc36c0c323312a` |
| `modules/M013-b1-self-raw.md` B1 独占锁段 | V0.03 | 60622 | `d2222ac3a96165f0f17438191c2758de779e9ff1eeacc5f6f6968da63f70023f` |
| `docs/audit-2026-09/findings.md` 全文件（顶端「当前 M013 B2-r2 整批候选 V2.24」） | V2.24 | 911157 | `608151f69052d71804ddbbe058eb286d111b1225a526fe1fdf6b9343be086c6e` |

只采各 raw 最后独占锁行起的完整本次 B1。Grok 真实 exit0（调度层）。本席完整阅读席甲 B1 V0.03（含 CX-M013-N01…N06 / O01…O06 / 84 矩阵）后再核候选；不把本席 B1「集团 PRD 抽核通过」当作对 N02–N06 的否决。席甲 self-raw 在 B1 段后另有其 B2 V0.04 自审票，本席独立核候选与源码，不表演性同意。

4 文件指纹与 B1 V0.03 覆盖表全等（未改）：

| 文件 | SHA256 |
|---|---|
| docs/business-entity-binding-development-design-v0.1.md | f78f63b3bffe8bbd842f4c4866010abaed652f333d7cdd31fbbb2cc1a510fb7f |
| docs/group-governance-rebuild-prd-v1.0.md | e60983ccb2884319e36b4059203096582adc0c76c15b239dcdfbc3abfd37b36b |
| docs/platform-governance-acceptance-pack-v0.2.md | ca94985de3792578c3436914133c17a99812f5fb5b32acd2ac45a422139def56 |
| docs/platform-governance-prd-v0.1.md | aa73e6c76e8389757a635806c893b9a9e8e8539166fd51a8719d3e0bcb1f1413 |

三 HTML 只读、未改：枚举 `4396bb71…e1d66`；业务地图 `eebc5b41…68d66`；治理地图 `a7fe8c01…50e0c1`。本轮无铁律 4 申请。已批「查看考勤」不重问、不反改。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定本机 CLI B2、禁止 MCP/子代理；Arc audit 会另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核源/HTML/命令再接受或反驳；禁止表演性同意；B2 不修复。尤其不得只复核本席已发现的 011 |
| grilling | 只借追问结构自查：012 守卫是否读 organizationId、013 dailyTemporary 是否真走 l1、014 Immediate 是否推广到直接授角色、016 Controller 收参是否等于覆盖、011 是否把拒绝写成审批已落地 |
| verification-before-completion | 声称前先跑 sha256/行号/源码读取 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码、不跑会写库的测试 |
| codebase-design | 删除测试：假「当前」句应改写而非加闸；不发明待生效 DB 唯一索引或自动启用调度 |
| ponytail-review | 只评最小删除/收窄，不落地。012 不在本模块补项目过滤；017 不修后台 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review / 子代理 / MCP | **未用**（用户指令：直接本机 CLI） |
| tdd / ponytail | B2 禁止修复 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |

### 完整差集（必须覆盖两席全部临时号，不得只核 011）

漏并：无。正式编号新分配 011–017，不重号旧 009/010，不改写 Grok/Codex 原临时号。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M013-001–008 | 历史已修（两席 B1 均核未回归） | 否 | **PASS（保持否）** |
| M013-009 | Grok OBS-02；Codex O02/O04 部分 | 否（XM→M392） | **PASS（保持否；OpenAPI 六旧值仍真，本模块不改 yaml）** |
| M013-010 | Grok OBS-01/04/05/07/08/09/10/11/12；Codex O01/O03/O04/O06 | 否（观察续记） | **PASS（保持否；含已批查看考勤差摘录，不重问）** |
| M013-011 | CX-M013-N01 + GR-M013-011 | 细化/是/是 | **PASS** |
| M013-012 | CX-M013-N02（Grok B1 未发现） | 细化/是/是 | **PASS（独立核源后接受，不维持 B1 抽核绿）** |
| M013-013 | CX-M013-N03（Grok B1 未发现） | 细化/是/是 | **PASS（独立核源后接受）** |
| M013-014 | CX-M013-N04（Grok B1 未发现） | 冲突/是/是 | **PASS（独立核源后接受；wildcard 冲突先成立）** |
| M013-015 | CX-M013-N05（Grok B1 未发现） | 细化/是/是 | **PASS（独立核源后接受）** |
| M013-016 | CX-M013-N06（Grok B1 未发现） | 细化/是/是 | **PASS（独立核源后接受）** |
| M013-017 | CX-M013-N02 关联 / Codex O05 | 细化（跨模块）/是/否（XM→M348；守卫依赖 M032） | **PASS（本模块队列否；本轮不得修后台）** |

观察映射（候选末尾，全部覆盖，无丢弃）：
- Grok OBS-01→010；OBS-02→009；OBS-03/06→011；OBS-04/05/07/08/09/10/11/12→010。
- Codex O01→010（BSM-114/120 已批查看考勤 vs 锁内旧摘录，不改锁/HTML）；O02/O04→010/009；O03（PRD606 业态引用）→010 非阻断，不新造开关；O05→017；O06→010。
- 本席 B1 仅 011 入队；集团 PRD「当前实现句抽核未再把已闭合项写成缺口」只覆盖旧 002 审批属性，**不是**对 N02–N06 具体反例的否决。

旧 001–008 未回归（V410 父链/角色、V394 group 审批属性、self_operated、空间字典、DELETE/lifecycle、税费落点说明、evaluate 员工接线、私人查看考勤）。012 项目 scope 缺口与旧 002「审批属性」不是同一事实，不能覆盖旧 002，也不能用 V394 证明隔离已完成。

### 独立取证（类型 2 源码 + 类型 4 只读命令；未跑 Java/HTTP/写库）

本席直接读取工作树，不沿用席甲摘要当依据。

**011 / GR-011 + N01（绑定当前写路径）**
- `PlatformService.createBusinessEntityBinding:756-801`：锁商户；`requireUnfinishedBindingSlot`；`requireUnchangedFinancialSubjects`；`LocalDate.now(ZoneOffset.UTC)`；`effectiveFrom.isAfter(today)` → status=`inactive` 且不 `syncMerchantEntities`；已到日 `active` 才同步。
- `changeBusinessEntityBinding:803-867`：action 闭集 active/inactive/suspended 且必填原因；`effectiveTo!=null` → `BINDING_PERIOD_ENDED`；active→非 active → `BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW`；`effectiveFrom.isAfter(today)` → `BINDING_NOT_EFFECTIVE`；action=active 另检商户/三主体/未完成槽/财税三主体不被替换。change 仍不能提交新三主体（`StatusChangeRequest` 无三列）。
- `requireUnfinishedBindingSlot:957-960` 码名仍 `ACTIVE_BINDING_EXISTS`，文案「生效或待生效」；`JdbcPlatformRepository.hasUnfinishedBusinessEntityBinding:592-601` SQL：`status='active' OR (status='inactive' AND effective_to IS NULL)`。V010 唯一索引仍只约束 active（OBS-03，不发明新唯一索引）。
- 既有测试方法原文：`futureBindingDoesNotChangeCurrentMerchantSubjects:861-873`；`anotherFutureBindingCannotReserveTheSameMerchant:876-887`；`futureBindingCannotActivateOrEndBeforeItsStart:890-903`；`dueBindingCanActivateButAnEndedHistoricalPeriodCannotReopen:906-926`；`bindingCannotReplaceExistingFinancialSubjectsEvenForMasterAuthority:929-943`；`activeBindingCannotBeDisabledToReplaceFinancialSubjects:752`。本席只读方法体，未冒称重跑。
- 无调度自动启用。拒绝码不是审批流。绑设计 L80/L90/L99/L117/L118 五处「当前」句均已假。

**012 / N02（Grok B1 漏；本席独立接受）**
- `AnalyticsController.getGroupOverview:119-125`：仅 `requirePermission("analytics.group.view")` + `requirePlatformScope()`，再 `service.groupOverview(groupBy)`。无 project/organization 参数，无 `currentProjectScopeQuietly`。
- 对照同文件 `getMetricDetail:381-408`：注释写明「requirePlatformScope 拦商户级/外包供应商（放行项目级）；项目级作用域收紧在下方由 currentProjectScopeQuietly 派生」——**该收紧未接到 group-overview**。
- `AuthorizationService.requirePlatformScope:482-498`：platform_admin 桥放行；外包拒绝；`actor.merchantId()` 非空拒绝；**不读 `organizationId` / 不调 `requireProjectScope`**。`AccessActor` 字段仅 id/username/accountType/organizationId/merchantId/status，无 projectId。`requireProjectScope:289-294` 才把 `organizationId` 当项目。
- `AnalyticsService.groupOverview:763-771` / `buildGroupOverview:774-830`：只按 `groupBy` 读全部 project 快照或平台回退，无调用者 scope。
- `AnalyticsServiceTests.groupOverviewReturnsMultipleRowsWhenEtlEmitsMultipleProjects:286-300`：无 actor，返回 `project_1` 与 `project_2`。
- V394:16-18 只把 `analytics.group.view` 置 `requires_approval=TRUE`，不补隔离。旧 002 闭合的是审批属性。
- 本席 B1 把 L110/G7/§12#15「已闭合」当成隔离已落地，属漏检。N02 反例成立，接受入队。

**013 / N03**
- `AccessService.applyGrant:879-901`：`dailyTemporary = approvalRequired.isEmpty() && REQUEST_TEMPORARY_PERMISSION`；仅 `approvalRequired.isEmpty() && !dailyTemporary` 才 `GRANT_APPROVAL_NOT_REQUIRED`；日常临时 `GrantReviewPolicy("permission_grant_l1","one_level_up",1)`。
- `AccessServiceTests.dailyTemporaryPermissionApplicationEntersSupervisorChain:1430-1453`：日常码 `ops.production_readiness.view` 申请 pending 且 `permission_grant_l1`。
- 上位 PGM「员工可申请临时权限，按层级对应主管审批」（治理地图实测命中）。文档 L71 把「无审批必需码一律拒绝」写成全部申请类型，已假。

**014 / N04**
- HTML BSM-007（业务地图「平台管理员拥有全部日常功能权限」）：日常自动同步；资金/敏感/删除/最终审批不因平台管理员身份自动获得，必须逐项授予。
- `computeEffectivePermissions:178-206`：**返回** `new EffectivePermissions(..., false, checkedAt)`，wildcard 标志恒 false。内部 wildcard 分支也只加 daily（MASTER 除外）。
- `requireActorCanGrant:789-801`：`if (self.wildcard()) return;` 因返回值恒 false 而成死路径，平台管理员没有「可授自己未持有码」的例外。
- `savePositionRoleTemplate:2060-2076`：`requireActorCanGrantImmediate`（808-812：从持权检查中滤掉 `requires_approval` 码）。
- `assignUserPositionDirect:1464-1527` + `holdHighRiskPositionPermissions:729-766`：普通随保存；高危 DENY 挂起 + `position_high_risk_release` 审批，非整单拒绝。
- 直接角色：`assignUserRoleDirect:1354-1358` 仍 `requireActorCanGrant` + `requireGrantApprovalIfNeeded`（高危 403 `GRANT_REQUIRES_APPROVAL`，须走 §3 申请）。岗位特例不得推广。
- 四分类：第一格冲突成立（wildcard 例外 vs BSM-007）；其余实现说明按同一缺陷收窄为细化现状。候选顺序正确。

**015 / N05**
- V170:100 `access.grant_request.apply` 种子 `high`。
- V394:13 `UPDATE … risk_level='low' WHERE permission_code='access.grant_request.apply'`；:14 授予 `role_staff_self`。
- 后续迁移仅 V251 改中文名，无风险档反改。文档 L90 未标注地写 high = 把历史当现行。申请=low 不等于授出免审。

**016 / N06**
- `AccessController.grantSummary:158-165` 仍接收 `window_days` / `position_threshold` / `user_threshold` 并透传。
- `AccessService.grantSummary:1044-1052`：`positionThreshold!=null || userThreshold!=null` **只要传入即** `QUERY_POLICY_OVERRIDE_FORBIDDEN`（即使数值等于政策）；`windowDays!=null && != policy.windowDays()` 才拒；实际天数/阈值从 `grantPolicy()` 读。
- `M006SecurityRegressionTests.summaryQueryCannotOverrideBusinessThresholds:517-521`：`grantSummary(null, Integer.MAX_VALUE, null)` 断言抛错。正常按文档示例填数字即拒，非极端。Controller 收参 ≠ 接受覆盖。

**017**
- 后台缺口与 012 同链。`plan/modules.md` M348 FILES 含 `AnalyticsController.java` / `AnalyticsService.java`；M032 FILES 含 `AuthorizationService.java`。本席未全审这两模块，不宣布其 PASS、不分配其未来 ID。本模块只修 012 文档。队列否。本轮不得修后台。

### 16 处原句→拟改（逐处唯一匹配，准确且最小）

候选原句在现文件对应行均唯一命中（python count=1）。拟改对照源码：

| # | 位置 | 原句要点 | 拟改要点 | 准确/最小 |
|---|---|---|---|---|
| 1 | 绑设计:80 | 创建直接写 active；change 只改三态 | UTC 当天判断未来 inactive 不同步 / 已到 active 才同步；未结束槽拒绝；三主体不得经此替换；期间+财税守卫；完整审批闭环仍未实现 | 是/是 |
| 2 | 绑设计:90 | POST 直接创建 active，无草稿审批 | 按生效日写 active 或待生效 inactive；待生效不是审批草稿；尚无完整草稿/审批/复核 | 是/是 |
| 3 | 绑设计:99 | change 只改状态 | 只接受状态动作并校验期间；生效中不得停用暂停、已结束不得重开、未到日不得启用或结束；不能交新三主体；无审批链；到日可调用启用≠自动启用 | 是/是（合入本席 L99 + 槽位/期间） |
| 4 | 绑设计:117 | 直接创建 active；仅已有 active 才 ACTIVE_BINDING_EXISTS | 未来 inactive；已到 active 并同步；active 或 inactive+effective_to 空 → 该码；替换三主体 → BINDING_FINANCIAL_CHANGE_REQUIRES_REVIEW | 是/是 |
| 5 | 绑设计:118 | 将状态改为三态并填原因 | 接受三态+必填原因；三段拒绝码；启用另检商户/三主体/槽位/财税 | 是/是 |
| 6 | 集团PRD:14 | requirePlatformScope 放行者=平台/集团级角色且项目管理员已按本项目收敛 | 业务边界保留 HTML 分级；当前实现未按项目过滤；不得把通过守卫等同已认定集团角色 | 是/是（「项目之上的获授权岗位」已见于本文 L66，非新角色） |
| 7 | 集团PRD:95 | 项目公司管理员因 scope 无法跨项目 | 当前只校验权限+守卫再按 groupBy 汇总全部项目；无商户归属的项目级调用者结果未按本项目收敛 | 是/是 |
| 8 | 集团PRD:110 | 实现待对齐已闭合（V394）且 requirePlatformScope 只收敛可见范围 | 闭合对象限定为审批属性；守卫不替代审批，也尚未完成项目级结果过滤 | 是/是（不重开 002） |
| 9 | 验收包:181 G7 | 实现缺口已闭合 | 审批属性已闭合，项目范围隔离待对齐；不得把审批属性当隔离完成 | 是/是 |
| 10 | 主PRD:657 §12#15 | 承接落地、不再是未来待办 | 基本聚合能力不再是未来待办；闭环仅审批属性；项目级过滤仍须按集团 PRD §4 对齐 | 是/是（保留 F-FIN 历史注） |
| 11 | 集团PRD:71 | 无审批必需码一律 GRANT_APPROVAL_NOT_REQUIRED | 角色/岗位无必需码才拒；临时无论日常/高危进主管链，日常用一级策略，不直接授出 | 是/是 |
| 12 | 集团PRD:34 | requireActorCanGrant + 平台管理员 wildcard 例外 | Immediate 只检立即生效普通码；高危不要求配置者预持；无 wildcard 全权限例外；显式拒绝有效 | 是/是 |
| 13 | 集团PRD:38 | 含高危就必须整单走 §3 | 直接分配/取消仍 requireActorCanGrant+跨组织闸+高危拦截；同范围定岗普通立即/高危挂起；不推广为其他入口免审 | 是/是 |
| 14 | 集团PRD:90 | apply（high） | apply（low；V170 原 high，V394 已调整） | 是/是 |
| 15 | 集团PRD:77 | 智能总结达阈值建议转固定 | 补：窗口和频繁阈值从后台政策读，查询不得覆盖；window_days 仅等于后台值可接受；两阈值只要传入即 QUERY_POLICY_OVERRIDE_FORBIDDEN | 是/是 |
| 16 | 集团PRD:83 | summary?window_days=&position_threshold=&user_threshold= | `GET …/summary`（使用后台政策，不传阈值参数） | 是/是 |

拟改未把拒绝写成审批已落地；未发明自动启用、待生效 DB 唯一索引、新权限码、新角色、业态开关、HTML 改法。无添加项。普通非 HTML 文档修复已有操作员授权，本票不另开 2.2。

### 四分类顺序（GEN-017）

均依 冲突 → 细化 → 技术性 → 添加，能指向上位则不进技术性：

- 011：与 HTML 权责审批口径同向（代码拒绝无审核停用/换主体更靠近 PGM-026/BEQR-143）；缺陷是「当前接口行为」过期。细化。
- 012：HTML/PRD 业务规则仍要项目隔离；缺陷是把未实现写成已收敛。细化，不是把 HTML 隔离删掉。
- 013：上位 PGM-041 临时权限主管审批；缺陷是排除条件写错。细化。
- 014：wildcard 例外与 BSM-007 明文冲突，第一格即冲突（删除该例外）；Immediate/定岗分叉是同一节现状收窄，不另开添加。
- 015：风险档是已有权限条目的实现证据刷新。细化。
- 016：总结阈值属 PGM-041「频繁申请自动总结」的实现说明。细化。
- 017：跨模块实现缺口，细化，队列否。

无添加、无新闸、无 questions。缺失按 V0.17：不自造门闸（不补自动启用、不补待生效唯一索引、不在本模块实现项目过滤）。

### 分类 / 队列 / 允许的最小修复边界（仅当双 PASS）

本批可定稿（须席甲本批 B2 亦 PASS）。队列是=011–016；否=001–010、017。一次 B3 只改本模块 4 文件 16 处现状文字，并分别递增标题版本：绑定 V0.09→V0.10，集团 V1.01→V1.02，验收包 V0.04→V0.05，主 PRD V0.04→V0.05。各文件现有变更记录处追加最小一行（绑定无专用日志则不新建章节，仅改标题），分别归 011、012、013–016；不改旧历史日志。不改三 HTML、OpenAPI、迁移、Java、锁、台账。文档类按类型 4 最小命令举证，不强制造测试。

禁止：把 011 的拒绝码写成财税审批已落地或到日自动启用；为待生效槽位加 DB 唯一索引；用 012 重开旧 002 或把 V394 当隔离完成；在本模块修 Analytics/Authorization；把 014 岗位挂起推广为直接授角色免审；把 015 apply=low 写成授出免审；放宽 016 查询覆盖；重号 009/010；把已批查看考勤再申请 HTML 修改；017 当本轮后台修复。

非阻断（不否决、不改票）：集团 PRD L66 已有「项目之上的获授权岗位」用语，012 拟改 L14 复用该词，B3 不得另造角色名/权限码。V251 中文名仍为「申请高危授权」不在本批 16 处内，不顺手扩圈。

### 结论

全批差集无漏并。本席 B1 只出 011，N02–N06 五条带直接源码反例，本票逐条独立读取后全部接受，不维持 B1 集团章节抽核绿。011 合入 L99 与待生效槽位/期间守卫，审批缺口继续诚实。012–016 原句/拟改 16 处准确最小。017 只登记 M348，M032 为守卫依赖。001–010 不抹除。添加 0。HTML 改法 0。

逐 ID 001–017 全 **PASS**。本席 B2-r2 **PASS**。未经双 PASS 不得把候选当定稿，更不称模块 PASS。步骤仍 B2；M013 IN_PROGRESS；B3 累计 1，本次尚未执行。

## M013 B2-r3 整批归并复核（Grok 乙席）

- 文件自身版本：V0.05（本段追加；上方前缀 119130 bytes / SHA256 `9dc13b4307adeaf59eeca40fda7b11a85828d514d9bf32e85c740b297fa04f93` 的 B2-r2 V0.04 及更早 B1 前缀原样保留，不得覆盖；本段不写独占锁哈希字段行，最新完整 B1 锁段保持在既有独占行）
- 席位：Grok 独立复核席乙（固定 grok-4.6 xhigh；FAIL 具否决权）。步骤：B2-r3 整批候选复核，不是 B1 重审、不是 B3、不是修复、不是模块 PASS、不是 B4 改票原文。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（M013 FILES 4 / 判据 84）
- 正本：`/home/james/complex-management` 分支 `main`。`HEAD`=`e456ba96c84185c8d39f18f548011daac48a6466`；`origin/main`=`495285da120e6c8c0cc85659c1a1501baf2b294d`（两值不等；本席读工作树，未设 GIT_INDEX_FILE，未动主索引）。本模块 4 业务文件相对 HEAD 为 B3-r2 未提交改动；本席只读、不修。
- 只读：未改 findings/ledger/三 HTML/锁/sidecar/任何业务文件；未 fetch/commit/push/merge/deploy；未写脚本/日志/临时报告；未派子代理、未调 MCP、未提问、未建其他文件。唯一写入=本指定路径追加。
- 本席 B2-r3：**PASS**（逐 ID 001–018 全 PASS；添加 0、不发 2.2；修复队列「是」=011–016 保持 + 新 018，「否」=001–010 保持、017 XM→M348）。此票只通过本批候选归并，不是模块 PASS。须席甲本批 B2 亦 PASS 才能定稿。不覆盖上方 B1 FAIL / B2-r2 PASS / B4-r2 原文。B3 累计 2/3；双 B2 PASS 才第 3 次，不修业务句。

正文实算锁（本段不写独占标签行）：`sha256sum` / `hashlib` / `openssl dgst -sha256` 三工具同值 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar 正文一行同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。criteria-lock 仍 V0.11。不 LOCK-AMEND。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `modules/M013-b1-grok-raw.md` 前缀（本段追加前） | V0.04 止 | 119130 | `9dc13b4307adeaf59eeca40fda7b11a85828d514d9bf32e85c740b297fa04f93` |
| `modules/M013-b4-r2-self-raw.md` 完整本次 | V0.01 | 47331 | `c7d26b9aeca2ce9abd64b523162b9e48083f9583465118f89befd395ae3fa15a` |
| `modules/M013-b4-r2-grok-raw.md` 完整本次 | V0.01 | 56850 | `72aefb6fd603c46fb66003bbf56f6555115367f5056b19d4156a3902887ea45d` |
| `docs/audit-2026-09/findings.md` 全文件（顶端「当前 M013 B4-r2回退 B2-r3整批候选」） | V2.26 | 915102 | `39fd20cc554ccf352febd2b063f978b13f786aee2de020ca62e662138db02c44` |

双方已封存，本席互读完整 B4-r2 两票（含完整 33677 byte / 16 hunk binary diff，双方原文一致）。席甲：修复内容 FAIL / 归因 PASS，发现 CX-B4-01。本席 B4-r2 原文：修复内容 PASS / 归因 PASS，§5/§9 把绑定 L3 列为观察、未分配 ID。合取 FAIL。无越界，不回滚。B4 未双 PASS，011–016 不得提前转否。

B3-r2 before/after（双方相同，本席 B4 已实算，本轮不重跑 diff 当新证据）：tree `75941c7d111b1a88313417197a581a48d917915c` → `ad9c27c34fcf3032cf42e330821e14f26d5e510e`；diff SHA256 `6b70b54e324d51022c3fbc988d7172548f1114907fb710111f9b4214631c2055`。工作树四文件仍等于 after：

| 文件 | 行 | SHA256 |
|---|---:|---|
| docs/business-entity-binding-development-design-v0.1.md | 153 | `04e4111bc47ad2c41d4f1188996f7e56b8a2dd875b1b0128b4468d7efcf8e674` |
| docs/group-governance-rebuild-prd-v1.0.md | 115 | `3f11679ea2a13ee3aa0a0299cb611e43168190453390c40c8018c7e78b821ee5` |
| docs/platform-governance-acceptance-pack-v0.2.md | 290 | `6fb5d6022519244bee5704014474daafab7a30521e7a7f020c215818d0749603` |
| docs/platform-governance-prd-v0.1.md | 970 | `c32e883636b97cb136a4478d9dd51c6d2e1656b437e4bd98ced094ee0fe32333` |

questions/M013*：无。添加项 0。HTML 申请 0。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定本机 CLI B2、禁止 MCP/子代理；Arc audit 会另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核 GEN-016 原文 + 绑定 L1/L3 字节再接受或反驳 CX-B4-01；禁止表演性同意；不以本席 B4 观察或 B2-r2 PASS 代替证据。B2 不修复 |
| grilling | 只借追问结构自查：L3 是否历史日志、打开文件是否即复现、能否把文档判据缩成 API、B2「仅改标题」是否豁免、只把 L3 改成 V0.10 是否违反 +.01 |
| verification-before-completion | 声称前先跑 sha256 / 行字节 / `CURRENT_VERSION_COMPARE` / 16 句 `in` 命中 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码 |
| codebase-design / ponytail-review | 删除测试：假当前版本行应改写而非加闸/加变更日志章节；不发明第三份版本字段 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review / 子代理 / MCP | **未用** |
| tdd / ponytail | B2 禁止修复 |
| graft | 本节点是文档元数据与已封存 raw，不查源码符号；**未调用** |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |

### 完整差集（必须覆盖既有 ID、新 ID、两席差集）

漏并：无。新正式号只分配 **M013-018**（来源 CX-B4-01；本席 B4 临时观察无独立 GR 号）。不重号 009/010/011。不改写任一方 B4 原票。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M013-001–008 | 历史已修（B1/B2-r2 核未回归；本 diff 未回写） | 否 | **PASS（保持否）** |
| M013-009 | OpenAPI entity_role 六旧值 | 否（XM→M392） | **PASS（保持否；本模块不改 yaml）** |
| M013-010 | 观察包 | 否 | **PASS（保持否）** |
| M013-011 | 绑定 5 处现状 + 当时标题 V0.10 | 细化/是/是 | **PASS（保持是；B4 未双 PASS 不清队。5 处拟改句现文件各唯一命中，下轮 B3 只核保持，不再改同句、不再为 011 升另外三文件）** |
| M013-012 | 集团隔离 vs 审批属性 | 细化/是/是 | **PASS（保持是；三文件拟改句仍在）** |
| M013-013 | 日常临时仍审批 | 细化/是/是 | **PASS（保持是）** |
| M013-014 | wildcard 例外删除 | 冲突/是/是 | **PASS（保持是）** |
| M013-015 | apply 风险档 V394 low | 细化/是/是 | **PASS（保持是）** |
| M013-016 | summary 禁阈值覆盖 | 细化/是/是 | **PASS（保持是）** |
| M013-017 | 集团总览项目过滤后台缺口 | 细化（跨模块）/是/否（XM→M348；AuthorizationService 依赖 M032） | **PASS（保持否；`plan/modules.md` M348 FILES 含 AnalyticsController.java；M032 为功能权限/最终权限计算后台段。本轮不得称已修）** |
| M013-018 | CX-B4-01；本席 B4 §5/§9 同处观察（当时判观察，本票改判） | 技术性/是/是（候选） | **PASS（独立核后接受入队；见下节。不以省轮次或 B2-r2 PASS 代替）** |

观察映射：本席 B4「绑定 H1 已升 V0.10、L3 仍 V0.09/2026-09-11」= 席甲 CX-B4-01，**不是**第二项。B4 归因双方均 PASS、无越界行，不回滚 011–016 业务句。

16 处业务拟改在 after/工作树仍各 `count_files=1`（本席 python `in` 全 True）。双方 B4 均认为这 16 处内容纠正够；本席 B4 原文内容 PASS 仍成立，但整批 B4 合取 FAIL，故 011–016 **仍是**。

### 独立核 M013-018（本席 B4 观察 → 本票改判；先假定对 → 构造反例）

冻结原文 GEN-016：「文档中的路径、命令、链接、**版本、状态**和完成声明须可从当前纳入范围复现，不得把计划或旧证据写成当前事实。」GEN-018：结论须落到 HTML 原文、可执行产物、外部规范原文或**可复现命令输出**之一；惯例/历史结论/席位意见本身不是依据。GEN-017 顺序：冲突 → 细化 → 技术性 → 添加。

工作树现文（`docs/business-entity-binding-development-design-v0.1.md`，153 行，SHA256 `04e4111b…e674`）L1/L3 完整原句：

```markdown
# 四类主体唯一绑定开发设计 V0.10

> 版本：V0.09 ｜ 最后更新：2026-09-11 ｜ 状态：现行开发设计
```

可复现命令输出（类型 4，本席当场）：

```
CURRENT_VERSION_COMPARE {'title': 'V0.10', 'metadata': 'V0.09', 'date': '2026-09-11', 'status': '现行开发设计', 'same': False}
L1 exact: '# 四类主体唯一绑定开发设计 V0.10'
L3 exact: '> 版本：V0.09 ｜ 最后更新：2026-09-11 ｜ 状态：现行开发设计'
MUTUAL_EXCLUSIVE_CURRENT_VERSIONS True
```

L1 `V0.10` 命中 1 次；L3 整行命中 1 次。打开同一文件页首即同时看到两个「当前版本」。L3 含「版本：」「状态：现行开发设计」，不是变更日志、不是历史摘录。本文件无「变更记录」「变更日志」章节。

**本席 B4-r2 观察理由逐条核，全部不成立：**

1. 「第一次按 §5 API 接入不依赖该 banner / 铁律 6 非第一次正常跑必撞」——把 GEN-016 的文档判据缩成「API 是否失败」。GEN-016 明文列版本、状态；设计文档第一次正常使用是打开文件。类型 4 打开即 `same=false`，无偶发条件、不需 Java/HTTP。
2. 「GEN-016 权威版本以本轮授权递增的标题为准」——锁内无此层级。L3 自己声明「版本」且「现行」。两处当前版本互斥即旧证据写成当前事实。
3. 「B2 对绑定写明无专用日志则不新建章节，仅改标题」——那是 B2-r2 执行边界对同步元数据的遗漏，是**本轮待复核前提**，不是 HTML 业务来源，也不是 GEN-016 豁免。不得用 B2-r2 双 PASS 证明 L3 可以停在 V0.09。
4. 「不把更新 banner 立成锁外新需求」——版本/状态已在冻结 GEN-016，属 M013 映射 84 条之内。不是新判据、不加闸。
5. L3 不是历史日志。另外三文件页首无「版本：V…｜状态：现行」第二当前版本行（集团仅标题 V1.02，验收包 V0.05，主 PRD V0.05），故本项只打绑定设计两行，不扩 ID。

**四分类：** 不与三 HTML 业务原文冲突（元数据不是权责/隔离规则）。版本行不属于已有主体/枚举/权限/默认值/流程的业务细化。无新实体/权限/状态机/收费发明。能指向上位的业务格进不去 → **技术性**。工程缺陷 **是**（GEN-016 版本/状态不可复现；GEN-018 类型 4）。队列 **是**。

**反例（本席主动找，不成立则维持观察）：**

- 若 L3 是「当时记录、一律不改写」的历史块：现文写「现行开发设计」，且文件无变更日志章。反例失败。
- 若只改 L3 为 V0.10、标题不动：这仍是一次内容改动。用户铁律「有内容改动必须版本 +.01」；V0.10 已由 B3-r2 写入，不能回退或假装没发生。故两行一并到 **V0.11**，不是把 L3 补成 V0.10。
- 若日期用 UTC 2026-09-15：候选与派发明确「操作员当地日 2026-09-14」。本机 `TZ=PDT date`=`2026-09-14`。采用 2026-09-14，不另造日期。
- 若给绑定新建变更日志章：超出最小两行，B2-r2 已禁止无中生有章节。018 不新建日志。

**最小拟改（完整，仅此两行，均归 018）：**

原文：
```markdown
# 四类主体唯一绑定开发设计 V0.10

> 版本：V0.09 ｜ 最后更新：2026-09-11 ｜ 状态：现行开发设计
```

拟改：
```markdown
# 四类主体唯一绑定开发设计 V0.11

> 版本：V0.11 ｜ 最后更新：2026-09-14 ｜ 状态：现行开发设计
```

影响：只让本文件当前版本和更新时间一致。011 五处业务说明、接口行为、审批规则、HTML、代码、另外三文件、旧历史日志均不改变。无业务发明，免人闸。普通非 HTML 修复已有操作员授权，本票不另开 2.2。

### 下一批 B3 边界（仅当本批双 PASS）

- B3 累计 2/3；本次若定稿则第 3 次。不修业务。
- 011–016 原 16 处已精确落地：下一次 B3 **先核 16 处已修文本保持**，不得为重复执行再改同句，不得再升集团/验收包/主 PRD 标题。
- 唯一拟实际 diff：绑定设计 L1+L3 两行（+2/−2），两行均归 **M013-018**。绑定 011 正文保留。
- 001–010、017 不动。017 后台项目过滤仍归 M348（依赖 M032），不得称已修。
- 不改三 HTML、OpenAPI、迁移、Java、锁、台账。不新建绑定变更日志章。文档类按类型 4 最小命令举证，不强制造测试。
- 禁止：把 018 做成回退 V0.10、只改一行、把 L3 当历史删除、把版本不一致写成新业务规则、借 018 改 16 处业务句、把 011–016 提前转否、把 017 拉进本 diff。

### grilling（自问，不等人）

1. 是否用 B2-r2 PASS 证明 L3 可旧？否。那是遗漏前提。
2. 是否用本席 B4 PASS 否决 CX-B4-01？否。B4 合取 FAIL；本票按 GEN-016 原文改判观察。
3. 是否新增判据？否。GEN-016 已含版本/状态。
4. 011–016 能否因 16 处内容够而转否？否。B4 未双 PASS。
5. 018 能否并进 011？否。011 是接口现状句；018 是文档身份元数据。不重号。
6. 添加/HTML/questions？0 / 0 / 0。

### 结论

差集仅 CX-B4-01 vs 本席 B4 观察一项，现归 **M013-018**。分类技术性、工程缺陷是、队列是、最小两行 V0.11 / 2026-09-14，本席独立核后接受。011–016 仍是；001–010、017 仍否。16 处业务句保持、下轮不重复改。添加 0。HTML 改法 0。无新判据/门闸/产物。

逐 ID 001–018 全 **PASS**。本席 B2-r3 **PASS**。未经双 PASS 不得把候选当定稿，更不称模块 PASS。步骤仍 B2；M013 IN_PROGRESS；B3 累计 2/3，本次尚未第 3 次。

## M013 获批追加一次修复 B2-r4 整批归并复核（Grok 乙席）

- 文件自身版本：V0.06（本段追加；上方前缀 133121 bytes / SHA256 `485f35257824197ecc98e1fe476c226e291251acca94433b06b0633ab84ea071` 的 B2-r3 V0.05 及更早 B1/B2 前缀原样保留，不得覆盖；本段不写独占锁哈希字段行，最新完整 B1 锁段保持在既有独占行）
- 席位：Grok 独立复核席乙（固定 grok-4.6 xhigh；FAIL 具否决权）。步骤：获批追加一次修复后的 B2 整批候选复核，不是 B5 重审、不是 B3、不是修复、不是模块 PASS。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（M013 FILES 4 / 判据 84）
- 正本：`/home/james/complex-management` 分支 `main`。`HEAD`=`e456ba96c84185c8d39f18f548011daac48a6466`。本席读工作树（含已暂存未提交），未设 GIT_INDEX_FILE，未动主索引。本席只读、不修。
- 只读：未改 findings/ledger/三 HTML/锁/sidecar/任何业务文件；未 fetch/commit/push/merge/deploy；未写脚本/日志/临时报告；未派子代理、未调 MCP、未提问、未建其他文件。未读本次 `M013-b1-self-raw.md` 新增 B2 结论（V0.06）。唯一写入=本指定路径追加。
- 重派说明：首次按候选 V2.30 派发，因漏并席甲观察，席甲在本席运行约 65 秒时中止，未产出报告，目标 raw 仍 133121 bytes。本次按 V2.31 重派。
- 操作员 2026-09-19 在 M013 BLOCKED-3ROUNDS 后选择「批准追加一次修复」：先 B2 定稿，再 B3（第 4 次）→B4-r4→B5-r4。B3 历史累计 3 不清零。批准追加修复不是本席 PASS 理由。
- 席甲自 2026-09-19 起由 Claude 接替 Codex。本席互读的是 Claude 席甲 B5-r3 V0.01，不是 Codex 未取得退出码的旧票。
- 本席 B2-r4：**PASS**（逐 ID 001–023 全 PASS；添加 0、不发 2.2；修复队列「是」=新 019–023，「否」=001–018 保持、010 续记仍否）。此票只通过本批候选归并，不是模块 PASS。须席甲本批 B2 亦 PASS 才能定稿。不覆盖上方 B1 FAIL / B2-r1..r3 / B5-r3 原文。

正文实算锁（本段不写独占标签行）：`sha256sum` / `hashlib` / `openssl dgst -sha256` 三工具同值 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar 正文一行同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。criteria-lock 仍 V0.11。不 LOCK-AMEND。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `modules/M013-b1-grok-raw.md` 前缀（本段追加前） | V0.05 止 | 133121 | `485f35257824197ecc98e1fe476c226e291251acca94433b06b0633ab84ea071` |
| `modules/M013-b5-r3-self-raw.md` 完整本次 | V0.01 | 23489 | `bd610a579af0d1d91dd1dd52fe68b7c0f7e4a5b90a20b17932dce814e2f9b916` |
| `modules/M013-b5-r3-grok-raw.md` 自第二个 `CRITERIA-LOCK-SHA256` 行（L359）起的本次完整报告 | V0.02 | 全文件 59882 | `e4748561d14d8b6508dd8329a8a51a68601be6fac3b5e2a8a7a410efd355b808`（前缀 V0.01 为 Codex 派发未取得退出码的旧票，不作输入） |
| `docs/audit-2026-09/findings.md` 全文件（顶端「当前 M013 获批追加一次修复：B2整批候选（尚未定稿）」V2.31） | V2.31 | 940753 | `70cfc3ba2c6016763f924f3a9dcc3ebac043200e2f2bc1038ef8d192a15b32c7` |

双方 B5-r3 已封存，均 FAIL。席甲（Claude）新发现 CX-B5-01 冲突、CX-B5-02/03 细化，观察 O-B5-01/02/03。本席 B5-r3 V0.02 新发现 GR-B5R3-01/02；PGM-042 行判否、Q4 判「文已分清」、G7 判与集团隔离口径一致。本票只审本批归并/分类/队列/最小修复边界，不重审别的模块。questions/M013*：无。添加项 0。HTML 申请 0。

工作树四文件（与 B5-r3 所报哈希一致，本席重算）：

| 文件 | 行 | bytes | SHA256 |
|---|---:|---:|---|
| docs/business-entity-binding-development-design-v0.1.md | 153 | 14100 | `21c704562c25af0987d781a91d8269a64caae1fd48dd5461b3561077d9018d65` |
| docs/group-governance-rebuild-prd-v1.0.md | 115 | 19925 | `3f11679ea2a13ee3aa0a0299cb611e43168190453390c40c8018c7e78b821ee5` |
| docs/platform-governance-acceptance-pack-v0.2.md | 290 | 35548 | `6fb5d6022519244bee5704014474daafab7a30521e7a7f020c215818d0749603` |
| docs/platform-governance-prd-v0.1.md | 970 | 145990 | `c32e883636b97cb136a4478d9dd51c6d2e1656b437e4bd98ced094ee0fe32333` |

### 技能（V0.17 §6 收窄，本节点实核）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 用户指定本机 CLI B2、禁止 MCP/子代理；Arc audit 会另写 `docs/arc/audits/`，**未用**。本步未另开两技能全文 |
| receiving-code-review | 按意图：先核 HTML/DEC/源码/迁移再接受或反驳；禁止因操作员批准或席甲已 PASS 而表演性同意。B2 不修复 |
| grilling | 按意图自问：PGM-042 否能否挡住 019、Q4「文已分清」能否挡住 020、G7 一致能否挡住 021、010 观察包能否吞掉 020/022、删「责任范围」是否发明触发项 |
| verification-before-completion | 声称前先跑 sha256 / 行字节 / 22 处 `count==1` / 四文件同根活句 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现；不改生产代码。未连 `complex_test` 实查 pg_constraint（以 V392 文本为 CHECK 源） |
| codebase-design / ponytail-review | 删除测试：假完成声明应删不扩写；不发明第三套项目归属规则 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review / 子代理 / MCP | **未用** |
| tdd / ponytail | B2 禁止修复 |
| graft | 本节点是归并复核，路径与行号已由派发指定；**未调用** graft 检索 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未另读独立技能全文**；未冒充调用 |

### 完整差集（必须覆盖既有 ID、新 ID、两席差集）

漏并：无。V2.30 漏并的 O-B5-01、O-B5-03 与本席 B5-r3「历史已批口径」已由 V2.31 补为 M013-010 续记行。新正式号只分配 **M013-019…023**。不重号 009/010。不改写任一方 B5 原票。不把 001–018 提前改定性。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M013-001–008 | 历史已修（B4-r3 双 PASS 后转否；本批未回写） | 否 | **PASS（保持否）** |
| M013-009 | OpenAPI entity_role 六旧值 | 否（XM→M392） | **PASS（保持否；本模块不改 yaml）** |
| M013-010 | 观察包 + 本批续记 | 否 | **PASS（保持否；续记只登记，见下）** |
| M013-011–016 | 历史已修（B3-r2/r3 落地；B4 未再回写） | 否 | **PASS（保持否）** |
| M013-017 | 集团总览项目过滤后台缺口 | 否（XM→M348） | **PASS（保持否；不重开）** |
| M013-018 | 绑定标题/元数据版本行 | 否 | **PASS（保持否；L1/L3 现已同为 V0.11）** |
| M013-019 | CX-B5-01；本席 B5-r3 PGM-042 行判否，本票独立改判 | 冲突/是/是 | **PASS（独立核后接受入队；反驳本席 B5-r3 PGM-042 否）** |
| M013-020 | CX-B5-02；本席 B5-r3 Q4「文已分清」，本票独立改判 | 细化/是/是 | **PASS（独立核后接受入队；Q4 只分清第四类，挡不住 CHECK 六值）** |
| M013-021 | CX-B5-03；本席 B5-r3 G7 判一致，本票独立改判该分句 | 细化/是/是 | **PASS（独立核后接受入队；G7 隔离口径仍对，「继续携带」分句为假完成）** |
| M013-022 | GR-B5R3-01 并席甲 O-B5-02 | 细化/是/是 | **PASS（独立核后接受；O-B5-02 升缺陷，不并回 010）** |
| M013-023 | GR-B5R3-02 | 细化/是/是 | **PASS（独立核后接受；OpenAPI 本身仍 XM→M392，同 009）** |

两席 B5-r3 全部条目去向：CX-B5-01→019；CX-B5-02→020；CX-B5-03→021；GR-B5R3-01+O-B5-02→022；GR-B5R3-02→023；O-B5-01、O-B5-03、本席历史已批口径→010 续记。无第三项、无丢条。

### 独立核 M013-019（CX-B5-01；反驳本席 B5-r3 PGM-042 否）

冻结原文：PGM-041 角色装配含「HR 部门定每个人的层级/职位，定完系统自动适配对应角色和权限模板」（HTML L658 / lock 摘录同句）。PGM-042 职级规则四条，末条「职级影响默认权限模板和审批层级，但不能替代功能权限点」（HTML L669）。GEN-017 先冲突后细化。GEN-016 完成声明须可复现。

五处现行排除句（本席打开现文，不是复述旧票）：

- 集团 PRD L76、治理 PRD L407：「本规则不把仅职级变化、责任范围变化或任意手工模板应用列为触发条件（DEC-R8-91）。」各文件恰好 1 次。
- 治理 PRD L523 表内「仅职级变化不作为本规则触发」；L544「本规则不扩展到仅职级变化，也不扩展到任意手工模板应用」；L656「本条不把仅职级变化或任意手工模板应用列为触发条件（DEC-R8-91）。」

DEC-R8-91 原文（`docs/prd-audit/business-decision-ledger.md` L1936）：「岗位、职责、组织或商户变化保存后立即按模板重算权限」；结尾「本裁决不扩展到未点名的“职级”或任意手工模板应用」。**没有「责任范围」**。「不扩展到未点名的职级」是该 DEC 的射程限定（不把免审立即重算裁决扩到职级），不是「职级变化不得重算」的新规则。HTML L658 第一句已经要求定层级后自动适配，不依赖该 DEC 去发明职级触发项。

现行代码（类型 4，读源码，未冒称实跑）：`AccessService.java` L1305–1313 注释 M006-G-02，已定岗员工改 `staffLevelId` 走 `assignUserPositionDirect`，按新职级岗位映射重算派生角色/模板。与排除句相反，与 HTML「定完自动适配」「职级影响默认权限模板」同向。

本席 B5-r3 PGM-042 行「职级只绑薪资大范围 / 否」**不成立为对本项的否决**：PGM-042 四条都在 lock 摘录里，第三条才是薪资大范围；第四条 L669 正是排除句所抵触的。本席当时未把五处排除句对照 L658/L669。本票改判。

R-002：席甲引本地裁决记录，160 无副本；本席未打开该路径，**不把它当依据**。HTML + DEC 原文 + AccessService 已够。

**四分类：** 与 PGM L658/L669 相反 → **冲突**（先于细化）。不是把职级写进正向触发清单（HTML 已有「定完自动适配」）；只删相反排除。工程缺陷 **是**。队列 **是**。

**责任范围：** DEC 与 HTML 正向触发清单均无该词。删掉「不把责任范围变化列为触发」只是去掉 DEC 未授权的额外排除，正向清单仍是岗位/职责/组织/商户/跨项目，**不是发明「责任范围变化必重算」**。

**历史不改：** 集团 PRD L5 是 2026-07-27 修订说明④（当时补了职级重算）；治理 PRD L966 是 2026-08-10 变更日志（当时按 DEC 写下排除）。二者是当时记录，不是现行规则行。不改正确。

**跨模块只登记：** 本席打开 `docs/platform-governance-product-spec-v0.2.md:1979` 同根排除句；`docs/business-html-source-of-truth-acceptance-spec-v0.1.md:67` RULE-GOV-ROLE-CHANGE-01「仅职级变化不触发本规则」；`docs/product-requirements-v0.1.md:601`、`:650` 活句、`:688` 为 2026-08-10 变更日志历史。均不在 M013 四文件。只登记 cross_module_observations、本模块不改，正确。

**最小边界同意：** 只删五处排除中的「仅职级变化」「责任范围变化」，保留「任意手工模板应用」（DEC 原文与第 6 条免审射程均有）。集团 V1.02→V1.03 与 020 合并一次；PRD V0.05→V0.06。不改 HTML、代码、决策台账、锁。四文件内「仅职级变化」活句仅集团 L76 + 治理 PRD L407/523/544/656，全在候选；绑定设计与验收包零命中。

### 独立核 M013-020（CX-B5-02；反驳「Q4 文已分清」挡入队）

集团 PRD L23 表头「取值（当前实现 CHECK / 服务校验）」写四值并称「DB CHECK 预留 4 类」；L70「申请类型当前服务支持三类……DB CHECK 预留 `role_permission`」。同模块另外三文件 `request_type` 零命中。

V170 L67–68 CHECK 确为四值。V392（HEAD 提交 `5b640680`，已发布）把 `chk_access_grant_requests_type` 扩为六值：原四值 + `user_scope_change` + `position_high_risk_release`。其后无迁移再改该约束。

`AccessService.java`：L56–57 定义两类；L1580–1585 `buildGrantRequest` 申请入口只收前三类；L1760–1787 `applyApprovedGrant` 处理 `REQUEST_USER_SCOPE_CHANGE` 与 `REQUEST_POSITION_HIGH_RISK_RELEASE`。本席 B5-r3 写「服务 L2046 仅三类」行号已漂移，事实是入口三类、审批应用含五类（另加 role_permission 仍不开放）。

本席 B5-r3 Q4「第四类 `role_permission`？DB CHECK 预留，服务仅三类。文已分清。」——对 **第四类 vs 申请入口** 仍真，L70 拟改也保留该分清。挡不住的是表头「DB CHECK 预留 **4 类**」在 V392 之后为假当前事实（GEN-016）。公开/内部之分不能把错误的 CHECK 个数写成现状。

**不并 010：** Codex B1 O06 把「四类 CHECK vs 公开三类」当诚实观察并入 010（技术性/正向，否）。010 从未把「CHECK 现为四」核成可复现当前事实，更未覆盖 V392 六值。另立 020 正确。

**四分类：** HTML 零命中这些类型名（集团 PRD 自己也标零命中不取得效力）→ 非冲突。是实现枚举/入口的当前事实订正 → **细化**。工程缺陷 **是**。队列 **是**。最小：L23 改六值并注明入口只收前三类、两类服务内生成、`role_permission` 未开放；L70 补一句。与 019 同文件一次版本。无业务发明。

### 独立核 M013-021（CX-B5-03；G7 隔离口径仍对，「继续携带」分句独立为假）

验收包 L181 G7 说明：「已由 `group-governance-rebuild-prd-v1.0.md` 与 V115/V169/V170 承接基本聚合能力，所有对象继续携带 `project_id`」。四文件内「所有对象继续携带」仅此 1 处。治理 PRD L657「所有对象必须带综合体项目（`project_id`）归属」是 **要求句**，不是完成声称，候选正确不改。

列级实查（读迁移，未连库）：`V005` `business_entities` 无 `project_id`；后续 V318/V354/V410 不加该列。`V002`/`V010` `business_entity_bindings` 无 `project_id`。`access_users`/`roles`/`permission_templates` 建表无该列。V427（工作树已暂存、**不在 HEAD**）建 `business_entity_projects` 多对多，仍不给主体表加列。绑定设计 L56、L80 明写没有，与列级事实一致。

PGM L412–413：「当前先按一个综合体落地，但所有基础对象都要带“综合体项目归属”。」这是要求。G7 写成「继续携带」= 把要求写成已完成事实（GEN-016）。

本席 B5-r3 G7 判「审批属性已闭合、项目范围隔离待对齐」与集团 PRD §4 一致——那是 M013-012 已修口径，**仍然对**。漏的是说明栏「继续携带」分句。本票只对该分句改判，不推翻隔离口径。

席甲曾给删句或改写成「须带（当前尚无该列）」二选一。候选选删除、不另写新要求句：验收包不是立法处，要求已在 PGM 与治理 PRD L657。**同意删除**，改写会扩写。

**四分类：** 未把 HTML 要求改成相反规则 → 非冲突。现状/完成声称过时 → **细化**。工程缺陷 **是**。队列 **是**。验收包 V0.05→V0.06 加一行变更记录。

### 独立核 M013-022（GR-B5R3-01 并 O-B5-02）

绑定设计 L39 迁移只到 V410；L56 把「当前表没有 `project_id`」当模型缺口；§5 API 表无 `POST /api/v1/platform/business-entities/{entity_id}/projects`；§8 依据无 V427。

工作树（类型 4）：`V427__m009_project_profiles_and_associations.sql` `git ls-files` 有、`HEAD` 无（`status` 为 `A` 已暂存）。`PlatformController` L27 `@RequestMapping("/api/v1/platform")`，L97–104 `POST /business-entities/{entity_id}/projects`。`PlatformService` L91–92 列表 `listBusinessEntitiesPaged(currentProject())`；L121 `creationProject`、L133 `associateBusinessEntityProject`（创建必写关联）；L363–381 关联方法 `requirePlatformScope` + `isMasterAuthority`；L419–429 最高权限可指定已存在项目。`JdbcPlatformRepository` L1233–1264 INSERT `business_entity_projects`，列表 `EXISTS` 过滤。

列级「主体表无 `project_id`」仍真。假的是把列级缺失写成「现行没有项目归属落点」，且漏已存在入口。第一次按 §3.1/§5 对接就会与现行入口对不上（GEN-002/016/014、PGM-017）。

席甲 O-B5-02 维持观察，理由是列级陈述仍真。列级真不能豁免「当前 API / 当前迁移」漏写已落地入口。本席 B5 已立 GR-B5R3-01。升缺陷正确。

**不并 010：** 010 登记的是「表没有 project_id」方向诚实，未要求补 V427/关联端点。另立 022。

**四分类：** 业务规则仍要项目归属，未与 HTML 对打 → 非冲突。实现细节/当前模型 → **细化**。工程缺陷 **是**。队列 **是**。拟改补 V427、保留「主体表本身无列」、§5 补关联端点、§8 补 V427，与 023 一次版本 V0.11→V0.12。不改代码、不发明缓冲期或新权责规则。§7 验收「保留综合体项目归属」是要求句，候选不改，正确。

拟改「持授权 U 盘密钥的最高权限」与本文他处及 HTML 用词一致；服务消息是「持授权密钥的最高权限」。不是新业务规则。

### 独立核 M013-023（GR-B5R3-02）

绑定 L49 status 行只写「尚无 DB CHECK」（DB 侧为真，V005 有列无 chk_be_status）。L55 只披露 OpenAPI `entity_role` 旧六值（009）。

OpenAPI `BusinessEntityRequest.status`（`openapi-p0.yaml` L29228–29236）枚举 draft/active/suspended/archived。`PlatformService` L54–56 `SUBJECT_STATUSES` = pending_admission/active/suspended/terminated/blacklisted；L893–900 `requireSubjectStatus`，缺省 active，其余 400 并提示归档走存档。按契约传 draft/archived 第一次即 400。

L57–58「未实现 HTML 完整词表和缓冲期」是另一差距（BEQR-155 六态 vs 服务五码+存档轴），候选不改该句，正确。023 只补契约四值 vs 服务五值。

OpenAPI 文件本身改动归 M392，同 009，本模块不改 yaml。正确。

**四分类：** 文档未把 draft 写成 HTML 商户状态 → 非冲突。状态词表的契约/服务对照 → **细化**（与 Grok B5 一致；009 是改 yaml 的跨模块技术性/否，023 是本模块设计披露/是）。工程缺陷 **是**。队列 **是**。

### 独立核 M013-010 续记与「另立新 ID」

O-B5-01：治理 PRD L650「职级只影响默认权限模板和审批层级**建议**」较 PGM L669 多「建议」。同文件 L648 与验收包 G6：升级放行层级按组织汇报链解析、不折算职级档位。HTML 未写明职级 **如何** 影响审批层级；未见可复现的相反实现。多一个「建议」不是闭集对打，不立案。只登记正确。

O-B5-03：验收包 §11 行序 09-11、09-14 在 09-01 之前，非时间序。不影响内容可验证性。只登记正确。候选 021 新行接在 09-14 之后，不重排历史行。正确。

本席 B5-r3 历史已批口径（BSM-114 锁旧摘录与 BSM-120 页脚、私人手机查看考勤、017→M348、009→M392、聚合≠隔离等）独立复核后保留，不重开。续记用「等」收口，不把已批口径写成新缺陷。正确。

**另立 019–023 而不并入 010：** 010 是技术性/正向观察汇总、队列否，只登记「差距方向正确的诚实登记」。020 是 CHECK 个数为假；022 是漏写已落地关联/入口。并进 010 会把缺陷藏进观察包，无法进修复队列。正确。

### 22 处原句、同根、发明、版本

本席 python `str.count`：候选 22 处原句在 **各自当前文件** 均为 1 次（其中 8/15/19/20 为行内唯一子串，替换后保留行尾原文：§8 仍接三个类名；G7 仍保留「已由…承接」；L544 仍接「现有手工授权…不因本节放宽」）。不是整行唯一但满足 check 模式「恰好 1 次」。

四文件同根活句：仅职级排除五处全在 019；责任范围活句仅集团 L76 与治理 PRD L407（L5 历史不改）；「继续携带」仅验收包 L181；「DB CHECK 预留 4 类」仅集团 L23；「尚无 DB CHECK」仅绑定 L49。无遗漏。V427 / `business_entity_projects` 现文四文件零命中，正是 022 要补的。

拟改只做必要改动：不把职级写入正向触发清单；不把 021 改成新要求句；不改 OpenAPI/代码/HTML；不新建绑定变更日志章（沿用标题+元数据行，与 018 先例一致；022/023 的版本行在项 1–2）。各文件内容改动一次版本 +.01，同文件多项合并一次。无添加项，无 HTML 申请，无业务发明。

### 下一批 B3 边界（仅当本批双 PASS）

- B3 历史累计 3/3 不清零；本次若定稿则记第 4 次，取得 after 树才记 4。
- 只改本模块 4 文件，按候选 22 处逐字 apply。先核 22 处原句仍各 1 次再改。
- 版本：绑定 V0.11→V0.12；集团 V1.02→V1.03；验收包 V0.05→V0.06；主 PRD V0.05→V0.06。变更记录只追加候选给出的新行，不改旧历史日志（含集团 L5、PRD L966、验收包 09-01 及更早行）。
- 001–018 不动。009 仍 XM→M392；017 仍 XM→M348。
- 不改三 HTML、OpenAPI、迁移、Java、锁、台账。文档类按类型 4 最小命令举证，不强制造测试。
- 禁止：把 职级 写进正向触发清单当新规则；把 021 改写成「须带 project_id」新要求；把 V427 写成「项目归属已完成」；在本模块改 OpenAPI；把 020/022 塞回 010；借本批改 001–018 已修句。

### grilling（自问，不等人）

1. 是否因操作员批准追加修复而被迫 PASS？否。逐项对照 HTML/DEC/源码。
2. 是否用本席 B5-r3 PGM-042 否否决 019？否。该否只核了薪资大范围，未核 L669 与五处排除。
3. 是否用 Q4「文已分清」否决 020？否。分清的是第四类，不是 CHECK 个数。
4. 是否用 G7 隔离口径否决 021？否。隔离口径仍对，「继续携带」分句独立为假。
5. 020/022 能否并进 010？否。010 观察包不进队列。
6. 删「责任范围」是否发明触发项？否。正向清单未加该词。
7. 022 是否把未进 HEAD 的 V427 写成已发布历史？否。审计对象是工作树；拟改写「当前实现」且保留「主体表无列」。
8. 添加/HTML/questions？0 / 0 / 0。
9. 是否读了本次 self B2 V0.06？否。不拿席甲 B2 当依据。

### 结论

差集五新 ID + 010 续记，无漏并。019 冲突入队（反驳本席 B5 PGM-042 否）；020/021/022/023 细化入队；010 续记仍否；001–018 仍否。分类、队列、最小边界均接受。添加 0。HTML 改法 0。无新判据/门闸/产物。

逐 ID 001–023 全 **PASS**。本席 B2-r4 **PASS**。未经双 PASS 不得把候选当定稿，更不称模块 PASS。步骤仍 B2；M013 IN_PROGRESS；B3 累计 3，获批追加的第 4 次尚未执行。

## M013 第二次获批追加修复 B2-r5 整批归并复核（Grok 乙席）

- 文件自身版本：V0.07（本段追加；上方前缀 156407 bytes / SHA256 `560eed6a1f09c5a710df695d3ee5d08d2a6bf5b588437874c2809eef7fa09bd8` 的 B2-r4 V0.06 及更早 B1/B2 前缀原样保留，不得覆盖；本段不写独占锁哈希字段行，最新完整 B1 锁段保持在既有独占行）
- 席位：Grok 独立复核席乙（固定 grok-4.6 xhigh；FAIL 具否决权）。步骤：第二次获批追加修复后的 B2 整批候选复核，不是 B5 重审、不是 B3、不是修复、不是模块 PASS。
- 控制：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（M013 FILES 4 / 判据 84）
- 正本：`/home/james/complex-management` 分支 `main`。`HEAD`=`e456ba96c84185c8d39f18f548011daac48a6466`。本席读工作树，未设 GIT_INDEX_FILE，未动主索引。本席只读、不修。
- 只读：未改 findings/ledger/三 HTML/锁/sidecar/任何业务文件；未 fetch/commit/push/merge/deploy；未写脚本/日志/临时报告；未派子代理、未调 MCP、未提问、未建其他文件。未读本次 `M013-b1-self-raw.md` 新增 B2 结论（V0.07）。唯一写入=本指定路径追加。
- 操作员 2026-09-19 在 M013 BLOCKED-4ROUNDS 后再次明确选择「批准再追加一次修复」：先 B2 定稿，再 B3（第 5 次）→B4-r5→B5-r5。B3 历史累计 4 不清零，第 5 次尚未执行。批准追加修复不是本席 PASS 理由。
- 本席 B5-r4 票为 PASS、无新发现，**不得**用来维护「括注成立」的旧结论；本票按源码与现文独立改判该括注。
- 本席 B2-r5：**PASS**（逐 ID 001–024 全 PASS；添加 0、不发 2.2；修复队列「是」=新 024，「否」=001–023 保持）。此票只通过本批候选归并，不是模块 PASS。须席甲本批 B2 亦 PASS 才能定稿。不覆盖上方 B1 FAIL / B2-r1..r4 / B5-r4 原文。

正文实算锁（本段不写独占标签行）：`sha256sum` / `hashlib` / `openssl dgst -sha256` 三工具同值 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；未扩判据，不 LOCK-AMEND。criteria-lock 仍 V0.11。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `modules/M013-b1-grok-raw.md` 前缀（本段追加前） | V0.06 止 | 156407 | `560eed6a1f09c5a710df695d3ee5d08d2a6bf5b588437874c2809eef7fa09bd8` |
| `modules/M013-b5-r4-self-raw.md` 完整本次 | V0.01 | 17629 | `af260f344faee42bc7e3e8a705f6b51af2fc22f1454b7c9dc446e7c3a4388b77`（席甲 FAIL，CX-B5R4-01） |
| `modules/M013-b5-r4-grok-raw.md` 完整本次 | V0.01 | 25264 | `ec4a7b838abe2939ce52b396d2fdb21eeeadf23f5440dded3447182a019a8407`（本席当时 PASS、无新发现；本票不沿用该无发现） |
| `docs/audit-2026-09/findings.md` 全文件（顶端「当前 M013 第二次获批追加修复：B2整批候选（尚未定稿）」V2.35） | V2.35 | 951931 | `d19f19aa9bc0700587daeb4e69a5a262477a3e88962d0692e0318ee96ead2a73` |

双方 B5-r4 已封存，合取 FAIL。席甲新发现 CX-B5R4-01。本席 B5-r4 无新发现。本票只审本批归并/分类/队列/最小修复边界，不重审别的模块，不重开 001–023 已落地项（除 ID 判定所问的 023 是否重开）。questions/M013*：无。添加项 0。HTML 申请 0。

工作树绑定设计（本批唯一拟改文件；本席重算）：

| 文件 | 行 | SHA256 |
|---|---:|---|
| `docs/business-entity-binding-development-design-v0.1.md` | 154 | `9cdec650f49335a42a8769aaea0fbfa0881f15b77994171b0d381538edf001b8` |

### 技能（V0.17 §6 收窄，本节点实核）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 用户指定本机 CLI B2、禁止 MCP/子代理；Arc audit 会另写 `docs/arc/audits/`，**未用**。本步未另开两技能全文 |
| receiving-code-review | 按意图：先核 Controller 映射 / Service 共用校验 / Jdbc 存档写法 / 同文 L57·L108·L121 再接受或反驳；禁止因操作员批准或本席 B5-r4 PASS 而表演性同意。B2 不修复 |
| grilling | 按意图自问：括注对主体档案是否成立、能否用服务提示语代替存档入口、023 原缺陷是否已落地、删括注是否扩写、001–023 应否重开 |
| verification-before-completion | 声称前先跑 sha256 / 指定 `grep -n Mapping` / `requireSubjectStatus` 调用点 / Jdbc `business_entities` 写语句 / 3 处 `count==1` |
| systematic-debugging / diagnosing-bugs | 只借读错→复现；不改生产代码。未连库、未跑 HTTP |
| codebase-design / ponytail-review | 删除测试：不可复现的括注应删不扩写；不发明主体存档端点或 `archived` 可设码 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review / 子代理 / MCP | **未用** |
| tdd / ponytail | B2 禁止修复 |
| graft | 已读技能全文；派发已指定 `grep -n Mapping` 与行号，本节点**未调用** graft 检索 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未另读独立技能全文**；未冒充调用 |

### 完整差集（必须覆盖既有 ID、新 ID、两席差集）

漏并：无。新正式号只分配 **M013-024**。不重开 023。不改写任一方 B5-r4 原票。不把 001–023 提前改定性。席甲 CX-B5R4-01 全部并入 024。本席 B5-r4「无新发现」不产生第二项。O-B5R4-01 只登记跨模块观察，不立案、不入本批队列。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M013-001–008 | 历史已修（本批未回写） | 否 | **PASS（保持否）** |
| M013-009 | OpenAPI entity_role 六旧值 | 否（XM→M392） | **PASS（保持否；本模块不改 yaml）** |
| M013-010 | 观察包 | 否 | **PASS（保持否）** |
| M013-011–016 | 历史已修 | 否 | **PASS（保持否）** |
| M013-017 | 集团总览项目过滤后台缺口 | 否（XM→M348） | **PASS（保持否；不重开）** |
| M013-018 | 绑定标题/元数据版本行 | 否 | **PASS（保持否）** |
| M013-019–022 | B4-r4 双 PASS 已转否 | 否 | **PASS（保持否；本批未回写）** |
| M013-023 | OpenAPI status 四值裂口披露；B4-r4 双 PASS 转否 | 否 | **PASS（保持否；不重开。原缺陷已落地，见下）** |
| M013-024 | CX-B5R4-01（席甲 B5-r4）；本席 B5-r4 无此项 | 细化/是/是（候选） | **PASS（独立核后接受入队；反驳本席 B5-r4「无新发现」对该括注的漏审）** |

两席 B5-r4 全部条目去向：CX-B5R4-01→024；本席 B5-r4 无新发现→无第二项；O-B5R4-01→跨模块观察（收口时登记，本批不修代码措辞）。无第三项、无丢条。

### 一、独立核 CX-B5R4-01：括注对主体档案是否成立

派发要求用 `grep -n Mapping` 列出 `PlatformController.java` 全部映射。本席实跑，与 business-entities / 存档相关行如下（省略 import 与类级 `@RequestMapping("/api/v1/platform")` 在 L27）：

- L42 `@GetMapping("/business-entities")`
- L52 `@PostMapping("/business-entities")`
- L59 `@GetMapping("/merchants")` … L71 POST merchants、L78 PUT merchants、L88 PUT merchants/project
- L97 `@PostMapping("/business-entities/{entity_id}/projects")`
- L108 `@PostMapping("/merchants/{merchant_id}/archive")`
- L117 `@PostMapping("/merchants/{merchant_id}/unarchive")`
- L190 `@PostMapping("/space-types/{id}/archive")`
- L199 `@PostMapping("/space-types/{id}/unarchive")`

`business-entities` 映射**仅** L42 GET、L52 POST、L97 POST projects 三处。无 `PUT`/`DELETE`、无 `.../archive`、无 `.../unarchive`。存档端点只有商户 L108/L117 与空间类型 L190/L199。席甲实查与本席本次 grep 一致。

`PlatformService.java`：

- L53 注释原文：「M009-004：BEQR-155 商户/运营主体可设状态（待准入/正常/暂停/终止/黑名单）；「归档」只经存档端点（archived_at 轴），不作可设代码。」——编号与「存档端点 / archived_at 轴」是商户口径（M009-004）。L54–56 `SUBJECT_STATUSES` 五码不含 `archived`。
- L893–900 `requireSubjectStatus` 提示「状态必须为 待准入/正常/暂停/终止/黑名单 之一；归档请使用存档操作」。
- 该函数仅两处调用：L119 `createBusinessEntity`（主体创建）、L1001 商户档案更新。提示语被主体创建与商户更新**共用**；「存档操作」对商户成立（Controller L108/L117 + `archiveMerchant` L199），对主体档案没有对应入口。
- 商户存档方法存在：`archiveMerchant` L199、`unarchiveMerchant` L223。`archiveBusinessEntity` 在 `com.complex.platform.governance` 零命中。

`JdbcPlatformRepository.java`：`business_entities` 写路径为 L47–63 `INSERT ... ON CONFLICT DO UPDATE`（列：id / entity_name / entity_role / 证照与账户 / status / created_at / updated_at），无 `archive_reason`、无 `archived_at`、无 `archiveBusinessEntity`、无 `UPDATE business_entities SET ... archive...`。列表/计数均为 SELECT。**无主体存档写法。**

同文对照（打开现文，不是复述席甲）：

- L49 当前字段表：`…/ `blacklisted`（归档走存档端点）`
- L57：HTML 六态含归档；「当前 `status` 未实现该完整词表和缓冲期闭环」——已登记词表差距
- L108：`business_entities` 有 `archive_reason`，「完整存档/删除流程未在当前接口落地」
- L121：「也不存在主体完整 CRUD。文档不得调用不存在的端点。」

L49 括注与 L108/L121 相反。接入方按 §3.1 当前字段表去找主体存档接口会 404。括注**对主体档案不成立**。

**本席 B5-r4 §5 当时核了什么：** 对照表把「主体 status 五码+归档走存档」写到 `PlatformService:54–56、:893–899`。当时核的是五码白名单与校验函数提示语/注释，**没有** `grep` Controller 的 business-entities 映射，**没有**核主体存档端点是否存在，**没有**核 Jdbc 是否有 `business_entities` 存档写法。把商户口径的「归档走存档端点」直接当成主体档案的当前 API 事实。该漏审成立。本票改判：工程缺陷在绑定设计 L49 括注，不在五码白名单本身。

BEQR-155 服务五码陈述仍与 `SUBJECT_STATUSES` 一致；HTML 六态差距已由 L57 承载。本项不是推翻五码，只否定括注。

### 二、ID 判定：立 024、不重开 023

023 原缺陷（findings 已定稿拟改，B3-r4 已写入）：绑定 L49 当时只写「尚无 DB CHECK」，未披露 OpenAPI `BusinessEntityRequest.status` 四值 draft/active/suspended/archived 与服务五值不一致。现文 L56 仍有「同一 schema 的 `status` 枚举仍是 draft / active / suspended / archived，与服务可设五值不一致，同属契约缺口（M013-023，归 OpenAPI 契约模块处理）」。该披露在当前文件恰好仍在，无回归。023 队列保持否正确。

括注「（归档走存档端点）」是 B3-r4 按 023 拟改**一并写入**的附加短句，不是 023 原缺陷本身。重开 023 会把「已落地的契约披露」与「新引入的假当前入口」绑成同一 ID，无法在 B4-r5 把删括注归因清楚。按「无 ID 者分配新 ID」立 **M013-024** 并注明来源于 023 拟改引入，正确。

### 三、四分类、工程缺陷、队列

GEN-017 顺序：冲突 → 细化 → 技术性 → 添加。

- **非冲突：** HTML BSM-004 / 绑定 §2.5 / §7「存档删除」要求配置类主体档案可存档。L49 括注并未把「不得存档」写成规则，也未与 HTML 六态对打；L57 仍承认 HTML 含归档。同文自相矛盾走 GEN-001，不是与 HTML 相反。
- **细化：** 「当前字段」表把不存在的主体存档端点写成现行入口，属实现事实误述（GEN-002、GEN-016）。
- **非技术性：** 不是观察级措辞。按该句对接会调用不存在的接口。
- **非添加：** 不发明存档业务、不申请 HTML、不把 `archived` 写成可设码、不新写要求句。

工程缺陷 **是**。修复队列 **是**。判据引用 GEN-002、GEN-016、GEN-001 成立；PGM-006（商户和主体当前事实）作同向支撑，不新增锁外判据。O-B5R4-01（校验函数提示语对主体调用方误导）是代码措辞，归 PlatformService 所属后台模块，本模块文档不据此立法——只登记、本批不修，正确。

### 四、最小边界：方案 A，不选 B

方案 A：删括注「（归档走存档端点）」，句止于 `` `blacklisted` ``。五码仍在；L57 词表/缓冲期差距与 L108 存档流程未落地仍在，不重复。同 021 先例（删不可复现声明、不扩写）。

方案 B：改写为「（`archived` 不由该字段写入；主体存档流程未在当前接口落地，见 §4.3）」。事实也对，但在已有 L57/L108 之外再写一遍，扩写。

**最小且不扩写 = 方案 A。** 候选「本批逐句原句与拟改」已按 A 给出 B3 执行文本，同意按此逐字，不改走 B。

版本：标题与 L3 元数据 V0.12→V0.13（内容改动一次 +.01）。L3 最后更新仍为 2026-09-19（与现文同日，不改日期）。绑定设计通篇无「变更记录/变更日志」段（本席 `变更记录|变更日志` 零命中），不新造章节——与 018 先例一致。不改代码、HTML、OpenAPI。同意。

### 五、3 处原句、001–023、业务发明

本席 python `str.count`，候选 3 处原句在**当前绑定设计**均为恰好 1 次：

1. `# 四类主体唯一绑定开发设计 V0.12`
2. `> 版本：V0.12 ｜ 最后更新：2026-09-19 ｜ 状态：现行开发设计`
3. `| `status` | `VARCHAR(32)` | 当前代码状态，尚无 DB CHECK；服务只接受 `pending_admission` / `active` / `suspended` / `terminated` / `blacklisted`（归档走存档端点） |`

「（归档走存档端点）」全文件 1 次。`V0.12` 仅出现在上述两处版本行（共 2 次）。拟改后 L56 的 023 披露句不被触碰。

001–023：候选表只有 M013-024 一行拟「是」；下方 B4-r4 队列段 001–023 仍全部「否」，本批声明「001–023保持否」「001–023 不动」。本席同意保持否，不借本批改已修句。

业务发明：无。添加项 0。HTML 申请 0。questions 0。不把主体存档写成已落地，不把 `archived` 写入可设五码，不改 OpenAPI。

### 下一批 B3 边界（仅当本批双 PASS）

- B3 历史累计 4 不清零；本次若定稿则记第 5 次，取得 after 树才记 5。
- 只改绑定设计 1 文件、候选 3 处逐字 apply。先核 3 处原句仍各 1 次再改。
- 版本 V0.12→V0.13；L3 日期 2026-09-19 不动；不新造变更日志段。
- 001–023 不动。009 仍 XM→M392；017 仍 XM→M348；023 披露句保留。
- 不改三 HTML、OpenAPI、迁移、Java、锁、台账。文档类按类型 4 最小命令举证，不强制造测试。
- 禁止：改走方案 B 扩写；重开 023；把 O-B5R4-01 当成本模块修代码；借本批改 001–023；把删括注写成「主体存档已实现」或「HTML 不再要求存档」。

### grilling（自问，不等人）

1. 是否因操作员批准再追加一次修复而被迫 PASS？否。括注对主体档案的成立性以本次 grep/源码为准。
2. 是否因本席 B5-r4 PASS 而维持「无新发现」？否。当时未核主体存档端点，本票改判。
3. 能否用 `requireSubjectStatus` 提示语证明主体存档入口存在？否。提示语被商户更新共用，入口只在商户/空间类型。
4. 能否因 L108 已登记差距而把 L49 降为观察、队列否？否。当前字段表是接入方第一事实源，假入口是缺陷。
5. 应否重开 023？否。原缺陷（四值裂口披露）已在 L56，括注是拟改引入的新误述。
6. 方案 B 是否更「完整」所以该选？否。完整事实已在 L57/L108，B 扩写。
7. 添加/HTML/questions？0 / 0 / 0。
8. 是否读了本次 self B2 V0.07？否。不拿席甲 B2 当依据。

### 结论

差集一新 ID 024，无漏并。024 细化入队；001–023 仍否。分类、队列、最小边界（方案 A、V0.12→V0.13、L3 同日、不新造日志）均接受。添加 0。HTML 改法 0。无新判据/门闸/产物。

同意候选整批。逐 ID 001–024 全 **PASS**。本席 B2-r5 **PASS**。未经双 PASS 不得把候选当定稿，更不称模块 PASS。步骤仍 B2；M013 IN_PROGRESS；B3 累计 4，第二次获批追加的第 5 次尚未执行。

