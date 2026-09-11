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
