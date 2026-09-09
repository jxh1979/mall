CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M012 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 修复前整模块全量审计
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 模块：M012 — 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／网页与验证／第1段
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 FILES 6 文件，计划冻结有效内容行 1485；当前 `git ls-files -z -- <6 paths> | xargs -0 cat | wc -l` = **1501**（含已合入 main 的 M009-003 伴生改动，不以 1485 冒称当前行数）
- 冻结判据：21 条（`M012 → [BSM-001, BSM-003, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-001]`，与锁映射行逐字相同；COUNT=21 unique=21 MISSING=[]）
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 修复队列的临时编号：GR-M012-001、GR-M012-002、GR-M012-003、GR-M012-004、GR-M012-005、GR-M012-008
- 添加项（B1 只登记，不代用户回答 2.2；B2 须逐条发问）：GR-M012-006、GR-M012-007
- 只登记、建议队列「否」：无（观察见 §8，不升 finding）
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员
- 本报告不表示全仓或发布门禁通过。M001–M011 已闭环，不重开。原 322 模块编号及旧结论不代表当前 471 模块完成。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令与 controller 优先于技能本体 |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md` | 内部 sharpen（本体只转发 grilling） |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 删除测试：绑定页若删掉错位字段，复杂度回到后台记录本身；不派 DESIGN-IT-TWICE 子代理 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（unwrap 模拟 + vitest 绿在错误夹具上）再下结论；不改生产代码 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流（页→`/api/v1/platform/*`→DTO→测试） |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；测试 28/28 绿 ≠ 行为对 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git、未直推、未安装 hook（安装会写仓库外配置，B1 禁止） |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 测试 1.8s 内结束，未起 monitor |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：本机技能树未找到
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，V0.17 明确不用）
- Arc `audit` 技能会派评审子代理并另写 `docs/arc/audits/`：与双席/§7 冲突，**不调用**
- superpowers `using-superpowers` 要求技能优先于探索：与「禁止子代理 / 本提示指定 controller」冲突处，按用户指令收窄

明确不用（controller 划掉）：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、code-review（mattpocock）、ponytail-audit/debt/gain。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 同值
- `criteria-lock.md` 版本 V0.11
- 映射行 `M012 → [BSM-001, BSM-003, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-001]` 与开工口径 21 项逐字相同
- `plan/modules.md` M012 FILES 6 条与用户清单逐字相同
- 6 文件 `git ls-files -z -- <6 paths> | xargs -0 cat | wc -l` → **1501**（计划冻结 1485 为切分时数字；差额来自已合入 origin/main 的 M009-003 伴生，不构成越界切分缺陷）
- 6 文件字节/SHA256 与用户固定清单逐文件 `match=True`（见 §2）

HEAD：`main` = `origin/main` = `bd445728a6ec7bb36a9e7ff19fe56927410ac61e`（`rev-parse HEAD` / `origin/main` / `merge-base` 三值相等）。本席未设 `GIT_INDEX_FILE`，未动主索引。工作区另有 M011 已双 PASS 未提交测试及审计产物（`git status --porcelain` 不含本模块 6 文件）；本席只读消费方、未改那些文件。

ledger 只读核验（不改）：V2.35，439601 bytes，SHA256 `0dd92182a80764de41eb03f5ca418659e909077c61d156ace48ee1d7d3a6e3a9`；M012 行 IN_PROGRESS、B3=0。findings V1.37，514937 bytes，SHA256 `fbae23dba8cd40647dcf52074f69f5507bb149ecc922738c0cae16bdc787137e`，仅作已登记边界背景（M009-003 / 012 / 036、M010-002 / 003），不代替本模块审计。

controller `160-audit-prompt.md` V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`；runbook V0.02 SHA256 `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`；modules.md V0.04 SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa`。

三份 HTML 只读、未改。锁内 BSM-001 来源行对本席读取的现行 HTML **命中**（`business-system-map-for-operations-v0.1.html:505`）；BSM-003 命中 `:563`；PGM-001 命中 `platform-governance-map-for-operations-v0.1.html:256`。

三 HTML 当前字节/SHA256（只读核验，本席未改）：

| 文件 | 字节 | SHA256 |
|---|---:|---|
| docs/business-enum-quick-reference-v0.1.html | 133961 | 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 |
| docs/business-system-map-for-operations-v0.1.html | 304482 | a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa |
| docs/platform-governance-map-for-operations-v0.1.html | 103015 | c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c |

三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。BEQR-148/154/155、PGM-018/019 等未映射本模块的锚点，只作 GEN-008 对照的 HTML 原文，不当作本模块冻结判据。

## 2. 6 文件覆盖表（全文已读；行=splitlines=`\n` 计数）

| # | 路径 | 行 | 字节 | SHA256 | 全文 |
|---|---|---:|---:|---|---|
| 1 | apps/web/src/admin/pages/GovernanceTopologyPage.tsx | 231 | 14547 | 31e530e213b51d4f0366fa1121c896f1d2c35ddd5a33c4ce476990e5aba7ae3c | 已读 L1–L231 |
| 2 | apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx | 188 | 9604 | 7426c19a4600d678684c06926ad3932b2e491b7b713144cc8831490dc98dc73d | 已读 L1–L188（9 @it） |
| 3 | apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 480 | 29371 | 76571cb692a4bc75ccd26f46be02e8c07c4c72806c9339feb55f91fb7144abe4 | 已读 L1–L480（未截断） |
| 4 | apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx | 261 | 14985 | 99a3b8201d9d7a401a4013a27a5e76a325919a759c93b1ea34b0b04ac3e77f8e | 已读 L1–L261（12 @it） |
| 5 | apps/web/src/admin/pages/SubjectBindingsPage.tsx | 220 | 12689 | 7c83e10ef83ead253e1c2daf3eb5b4e7c311f2d38478bd247e6162820ec0a2df | 已读 L1–L220 |
| 6 | apps/web/src/admin/pages/SubjectBindingsPage.test.tsx | 121 | 5916 | c6e9d77a20b3ff430938e6e89df545211bab16f8d8d0b8efb56443a7a0e4e428 | 已读 L1–L121（7 @it） |

合计 1501 行 / 87112 字节。用户固定清单逐文件 match。`parentEntityId` 在本 6 文件 0 命中（与 M009-012 转入口径一致，不重开）。

## 3. 数据流（页面 → `/api/v1/platform/*` → 测试）

后台只读核对（非本模块文件，不作修复对象）：`PlatformController.java`、`PlatformService.java`、`BusinessEntityBindingRecord.java`、`MerchantRecord.java`、`ProjectRecord.java`、`SpaceRecord.java`、`StatusChangeRequest.java`、`MerchantProfileRequest.java`、OpenAPI `StatusChangeRequest`（required=`idempotency_key`/`action`/`reason`）。

| 页面 | 方法 | 路径 | 后台权限 | 页内权限门 | 请求/响应要点 |
|---|---|---|---|---|---|
| 拓扑 | GET | `/api/v1/platform/business-entities` | `entity.binding.view` + 平台作用域 | 无该权限则**不请求**、主体列空 | `BusinessEntityRecord`：entityName/entityRole/unifiedSocialCreditCode/status |
| 拓扑 | GET | `/api/v1/platform/merchants` | `merchant.profile.view` | 页路由要求此码 | `MerchantRecord` 含 cooperationForm 六类码、status 五态（归档走 archivedAt） |
| 拓扑 | GET | `/api/v1/platform/projects` | `governance.project.view` | 页路由要求 | `ProjectRecord`：name/code/merchantId/venueType/status |
| 拓扑 | GET | `/api/v1/platform/spaces` | `governance.space.view` | 页路由要求 | `SpaceRecord`：**有 projectId，无 venueId**；spaceType∈字典 8 码；status∈{active,unavailable} |
| 组织商户 | GET | 同上 entities/merchants/projects | 同左 | merchants 带 `includeArchived`/`merchantType` | 创建 POST `/merchants`：`MerchantProfileRequest` 校验业态六值、合作方式六类（空=未定）、主体状态白名单 |
| 组织商户 | POST | `/merchants/{id}/archive\|unarchive\|delete-request\|delete-approve` | manage / approve | 行内 `RowActions` | 存档 reason 必填；取消存档可空；审批 decision=approved\|rejected |
| 绑定 | GET | `/api/v1/platform/business-entity-bindings` | `entity.binding.view` | 页路由要求 | 记录字段=`merchantId, operationEntityId, contractEntityId, settlementEntityId, status, …` **不是** businessObjectId/operatorSubject |
| 绑定 | POST | `/business-entity-bindings/{id}/change` | `entity.binding.manage` | `HighRiskConfirm` | **必填 `action`∈{active,inactive,suspended}** + reason；本页请求体无 action |

路由（只读）：`adminRoutes.ts` ADM-GOV-001 要求 `governance.project.view`+`governance.space.view`+`merchant.profile.view`（**不含** `entity.binding.view`）；ADM-GOV-002 要求 `merchant.profile.view`+`governance.project.view`；ADM-GOV-003 要求 `entity.binding.view`。

`AdminApi.get` 将信封 `data.items` 拍平并把 snake_case 转 camelCase，**不改字段名语义**。

## 4. 三线审计

### 4.1 第一性原理（每文件三问）

#### GovernanceTopologyPage.tsx

1. **为何必须存在**：删掉则 BSM-001「哪个综合体、谁在经营、在哪经营」与 PGM-001「先管归属：哪个综合体、哪个楼层、哪个区域、哪个点位、哪个商户」在后台无入口。有上位需求。
2. **声称是否执行**：自称只展示关系、读四接口。KPI/表/抽屉/缺权限不请求主体，测试覆盖了 KPI 与抽屉。但「已挂接」计数读 `space.venueId`（后台无此字段）；项目业态 `display(venueType)` 英文直出。声称「治理关系入口」在真实响应下主体列可空、已挂接恒 0。
3. **未验证前提**：测试夹具把 `venue_id` 从未出现的空间当成已挂接；把 `entity_role:"operation"` 当成仍可返回。

#### GovernanceTopologyPage.test.tsx

1. **为何必须存在**：GEN-003 要求关键分支可失败断言。KPI 聚焦与缺权限不白屏有上位。
2. **声称是否执行**：9 例 vitest 均绿（CMD-VITEST）。中文断言只覆盖 `active/suspended→有效/已封禁`，不断言 venueType/spaceType 中文，反而用 `hall`/`operation`/`bar` 夹具。
3. **未验证前提**：夹具形状 = 页面期望形状，不是后台记录形状。绿测不证明真实 GET `/spaces` 的已挂接数字。

#### OrganizationMerchantPage.tsx（L1–L480 全文）

1. **为何必须存在**：BSM-001 基础盘子；BSM-003 通用「可改可删、操作有引导」；页面实现新建商户引导流与存档/删除。有上位。
2. **声称是否执行**：证据字符串列出 GET 三接口与 POST `/merchants`。引导流校验名称/业态/手机号有测试。边界文案写「不在本页替代四类主体绑定」，但 `createMerchant` 静默 POST `businessEntityId/contractEntityId/settlementEntityId` 及租金面积。合作方式下拉六类与后台 `MERCHANT_COOPERATION_FORMS` 同词表（M009-003 已改，属实）。
3. **未验证前提**：默认「二期测试商户」「13800000000」「lease」「org-1」「contract-subject-1」「12.3」被当成可提交业务值；无 `entity.binding.view` 时 `entities[0]` 为空仍提交 `org-1`。

#### OrganizationMerchantPage.test.tsx

1. **为何必须存在**：锁创建校验与 KPI 聚焦。
2. **声称是否执行**：12 例绿。创建流断言 POST 发出且不含 `fixed_rent`；列表/抽屉却断言「固定租金」「运营主体」和原文 `bar`。
3. **未验证前提**：夹具 `cooperation_form:"fixed_rent"`、`entity_role:"operation"` 与现行后台闭集相反（M010 明确留给本模块）。

#### SubjectBindingsPage.tsx

1. **为何必须存在**：PGM-001「归属、权限、合同和责任」；BSM-001「谁在经营、签了什么合同」。四类主体绑定是底座。
2. **声称是否执行**：读 `/business-entity-bindings`、变更 POST `.../change`。表格读 `businessObjectId/operatorSubject/contractSubject/...`；真实 unwrap 后这些键为 `undefined`，单元格为「-」。变更体无 `action`，与 OpenAPI required 及 `PlatformService.changeBusinessEntityBinding` 白名单直接冲突。
3. **未验证前提**：测试夹具用同一套臆造键，故绿。首次正常打开真实接口必出空列；首次点「提交绑定变更」必缺 action。

#### SubjectBindingsPage.test.tsx

1. **为何必须存在**：GEN-003。KPI 聚焦有覆盖。
2. **声称是否执行**：7 例绿，全部不发 POST、不读 `merchantId`。
3. **未验证前提**：`business_object_id` 等不是 Jackson 会给出的键。

### 4.2 顺从式（21 条逐项）

| ID | 本模块适用面 | 结论 |
|---|---|---|
| BSM-001 | 三页是「基础盘子」入口：主体/商户/项目/空间/绑定 | **FAIL 证据**：绑定列读错字段，谁在经营/合同/结算在本页不可见；创建商户写入伪造主体 ID。HTML 原句「哪个综合体、谁在经营、在哪经营、签了什么合同、谁能操作、谁负责。」（BSM `:509-510`） |
| BSM-003 | 锁摘录仅为 h2+导语「不属于某一个子系统的通用规则」；子条存档/引导/分岗属 BSM-004/005/007，**未映射本模块** | 页有引导新建与存档删除，作为通用规则的承载面存在。子条细节不升本模块冲突。存档关键分支无测试 → 记 GEN-003 |
| GEN-001 | 三页均有 HTML 上位；KPI 聚焦为交互收窄不是无需求抽象 | 通过（KPI 同构不单独开 finding） |
| GEN-002 | 绑定变更声称可提交；拓扑声称已挂接计数 | **FAIL**：GR-001/002/003 |
| GEN-003 | 28 例均执行且有断言 | **FAIL**：绿在错误夹具上；不锁真实字段/闭集/变更 action；存档删除无测试。GR-005/008 |
| GEN-004 | 创建/变更输入 | **FAIL**：隐藏字段未校验即提交；绑定缺 action。GR-002/006 |
| GEN-005 | 按钮权限与缺 `entity.binding.view` 不请求 | 读路径降级设计成立（拓扑测试覆盖）。写路径：无主体查看权仍可 POST `org-1`（ADM-GOV-002 路由不含该权）→ 并入 GR-006 |
| GEN-006 | 无本模块 SQL；一次 POST 一单 | 无实现面（库约束）。观察：merchants 三主体列无 FK（V005），伪造 ID 可落库 |
| GEN-007 | 幂等键含 `Date.now()` | 观察：单击首次成功不撞；双击/重试换键。铁律 6 收窄不修 |
| GEN-008 | 页/后台/OpenAPI/夹具枚举与字段 | **FAIL**：GR-001/002/004/005 |
| GEN-009 | 创建有 try/catch；存档/删除/审批无 | **FAIL 轻**：GR-008 后半 |
| GEN-010 | 无密钥、无注入点；电话为表单 | 通过（展示层 React 文本节点） |
| GEN-011 | `AuditTrailHint`；变更带 evidenceAttachmentIds | 假 ID `evidence-full-admin` 并入 GR-006（添加）。留痕事件名本身中文页外 |
| GEN-012 | 列表默认 size=500，页未自扩 | 通过 |
| GEN-013 | 6 文件无构建配置 | **无实现面**：原因=本模块无 package/CI/tsconfig |
| GEN-014 | 6 文件无迁移 | **无实现面**：原因=本模块无 SQL/Flyway |
| GEN-015 | 键盘行点击、抽屉 Esc、中文文案 | 部分 FAIL：英文枚举/表头见 GR-004；其余抽屉/KPI 中文横幅成立 |
| GEN-016 | 页内「接口证据」字符串 | 所列路径与 `api.get/post` 一致。通过（自称可复现，不把 OpenAPI 当需求源） |
| GEN-017 | 分类纪律 | 本席未把 HTML 沉默当冲突；添加项完整列出。夹具默认值走添加不是技术性 |
| GEN-018 | 本报告依据 | 类型 1 HTML 逐字 + 类型 4 命令（vitest/unwrap/sha256）。未用历史席位意见当依据 |
| PGM-001 | 底座+全中文+可点击 | **FAIL**：英文业态/空间类型/「ID」表头（GR-004）；绑定对象不可见（GR-001）。可点击：商户 TraceLink 存在；绑定列当前无有效对象可点 |

### 4.3 对抗式（grilling 自问自答，不派子代理）

Frontier 已结算，不向操作员发问：

1. **真实 GET 绑定的行，页面读到的键是什么？** 自己跑 unwrap：`merchantId/operationEntityId/contractEntityId/settlementEntityId`。页面读 `businessObjectId/operatorSubject/...` → 全 `undefined`。首次正常打开即发生。
2. **点「提交绑定变更」的 JSON 有没有 OpenAPI 必填 `action`？** 没有。`PlatformService` `requireText(actual.action(),"action")` + 白名单 active/inactive/suspended。首次确认提交即 400。
3. **空间「已挂接」在真实 SpaceRecord 上是否非 0？** `venueId` 不存在，`projectId` 存在。`attachedSpaces` 恒 0。首次打开即发生。
4. **项目业态/空间类型用户看见什么？** `display("bar")`=`bar`；`formatBusinessStatus("shop")`=`shop`（statusLabels 无 8 类空间码）。字典 `V184` 有中文 `type_name`，页不调 `/space-types`。首次打开即发生。
5. **测试是否会因真实闭集而红？** 不会：它们自己造 `operation`/`fixed_rent`/`hall`/`terminated`。GEN-003 失败。
6. **无 entity.binding.view 的商户管理员第一次点创建会提交什么主体？** `entities[0]?.id ?? "org-1"` → `"org-1"`。该页路由本来就不含该权限。首次创建即发生。
7. **HTML 有没有规定手机号必须 11 位大陆号？** 三份无。不能判冲突；登记添加。
8. **绑定页没有「创建绑定」是不是冲突？** HTML 未规定必须在本页创建；后台 POST 创建在。铁律 6 / HTML 沉默 → 观察，不升冲突。
9. **极端：并发双开绑定、page>500 截断、unavailable 空间英文。** 双开非首次必撞；500 是后台默认；unavailable 仅当存在该状态行才英文（与 shop 英文同类，并入 GR-004）。不另造理论闸。

ponytail-review（只列不改）：

- `OrganizationMerchantPage.tsx:L79-88,L170-189`: delete: 验收夹具默认值写入生产创建。空表单 + 用户填。
- `SubjectBindingsPage.tsx:L48,L132-136`: delete: 验收原因与假证据。用户填原因 + 必填 action。
- `SubjectBindingRow` 臆造七字段：shrink 到后台四 ID。types.ts 不在本模块，B3 可在页内读真实键。
- KPI URL 三页复制：yagni 不成立（全站样板，有测试）。不删。
- `net: 生产默认值与错位字段可删，约数十行；不把 KPI 样板算膨胀。`

## 5. Findings（临时 ID，正式 ID 仅 B2 分配）

### GR-M012-001

- **标题**：四类主体绑定页读取臆造字段，真实后台记录下经营对象/三主体全为「-」，唯一绑定 KPI 恒 0
- **文件:行号**：`SubjectBindingsPage.tsx:59-67,202-207`（读 `businessObjectId/operatorSubject/contractSubject/contractSubjectId/settlementSubject/settlementSubjectId/auditEvidenceRef`）；`SubjectBindingsPage.tsx:106`（`uniqueObjects` 基于 `businessObjectId`）；`SubjectBindingsPage.test.tsx:9-12`（夹具同构臆造）；对照只读 `BusinessEntityBindingRecord.java:6-18`、`PlatformService.java:569-575`
- **完整原文（页）**：`{ label: "业务对象", value: display(row.businessObjectId) }` 等；表体 `<td>{display(row.businessObjectId)}</td><td>{display(row.operatorSubject)}</td>…`
- **业务影响**：打开「四类主体绑定」看不到谁在经营、合同主体、结算主体；BSM-001/PGM-001 归属入口空转。KPI「唯一绑定对象」对真实数据为 0。
- **可复现命令/最小反例**（类型 4）：

```text
# 模拟 AdminApi unwrap（snake→camel，拍平 items）后页面读键：
PAGE_READS
  businessObjectId=None
  operatorSubject=None
  contractSubject=None
  contractSubjectId=None
  settlementSubject=None
  settlementSubjectId=None
  auditEvidenceRef=None
  merchantId='m-1'
  operationEntityId='ent-op'
  contractEntityId='ent-ct'
  settlementEntityId='ent-st'
```

- **冻结判据**：GEN-008、GEN-002、GEN-003、BSM-001、PGM-001
- **2.3 依据类型**：4（unwrap 命令）+ 1（BSM-001「谁在经营、签了什么合同」；PGM-001「先管归属」）
- **四分类**：**技术性**
- **无业务发明理由**：后台已有四类主体绑定记录与三主体 ID 字段；缺陷是前端键名与 Jackson/camelCase 投影不一致，没有引入 HTML 没有的业务实体或状态。
- **工程缺陷**：是
- **建议入队**：是
- **最小处置**：本页改读 `merchantId/operationEntityId/contractEntityId/settlementEntityId`（及 status/changeReason）；测试夹具改为真实 snake_case。`apps/web/src/admin/types.ts` 的 `SubjectBindingRow` 不在本模块 6 文件，B3 可在页内局部类型，不强制改 types.ts。

### GR-M012-002

- **标题**：绑定变更请求缺少 OpenAPI/后台必填 `action`，首次确认提交无法变更三态
- **文件:行号**：`SubjectBindingsPage.tsx:124-138` 请求体仅 `reason/idempotencyKey/evidenceAttachmentIds`；对照只读 `StatusChangeRequest.java:5-10`、`PlatformService.java:602-604`、`openapi-p0.yaml:27693-27710` required=`idempotency_key,action,reason`，action∈启用/停用/暂停
- **完整原文**：`const body = { reason, idempotencyKey: \`full-admin-subject-binding-change-${selected.id}-${Date.now()}\`, evidenceAttachmentIds: ["evidence-full-admin"] };`
- **业务影响**：用户按页面「提交绑定变更」操作，后台 `requireText(action)` 400。绑定三态（active/inactive/suspended）无法从本页切换。
- **可复现命令/最小反例**（类型 4）：`CHANGE_SNAKE {'reason': '完整后台验收变更', 'idempotency_key': '…', 'evidence_attachment_ids': ['evidence-full-admin']} HAS_ACTION False`。测试 7 例均不调用 POST（CMD-VITEST）。
- **冻结判据**：GEN-002、GEN-008、GEN-004、GEN-003
- **2.3 依据类型**：4 + 3（OpenAPI 组件原文 required action）+ 实现事实（后台白名单，不作新需求源）
- **四分类**：**技术性**
- **无业务发明理由**：三态编码已在后台/OpenAPI；页面漏传必填字段，不是新业务规则。
- **工程缺陷**：是
- **建议入队**：是
- **最小处置**：表单增加「启用/停用/暂停」中文选项，POST `action`；测试断言 body 含 action 且非法值可失败。假证据 ID 与默认原因见 GR-006（添加，不并入本条修复默认）。

### GR-M012-003

- **标题**：拓扑页「已挂接」用臆造 `venueId`，真实空间 `projectId` 下计数恒 0
- **文件:行号**：`GovernanceTopologyPage.tsx:97-99,218`；类型注释只读 `types.ts:1109-1110`「旧臆造字段」；`SpaceRecord.java:8-21` 无 venueId、有 projectId
- **完整原文**：`const attachedSpaces = spaces.filter((space) => space.venueId && projectIds.has(space.venueId)).length;` KPI small `{attachedSpaces} 个已挂接`
- **业务影响**：空间已挂接项目时仍显示「0 个已挂接」，归属关系计数撒谎。
- **可复现命令/最小反例**（类型 4）：unwrap `{project_id:"p-1"}` → `SPACE_venueId None projectId p-1`。夹具未给 venue_id，测试不断言该 KPI。
- **冻结判据**：GEN-008、GEN-002、BSM-001、PGM-001
- **2.3 依据类型**：4
- **四分类**：**技术性**
- **无业务发明理由**：挂接关系后台用 `spaces.project_id`；前端读错键。
- **工程缺陷**：是
- **建议入队**：是
- **最小处置**：改为 `space.projectId && projectIds.has(space.projectId)`；测试用真实 `project_id` 断言非 0。

### GR-M012-004

- **标题**：项目业态、空间类型对用户直出英文码；绑定表头含英文「ID」
- **文件:行号**：
  - `GovernanceTopologyPage.tsx:125,168` `display(project?.venueType)`
  - `GovernanceTopologyPage.tsx:139,187` `formatBusinessStatus(space.spaceType)`（`shop/building/zone/…` 不在 statusLabels，原样英文；字典 8 类中文在 V184 `type_name`）
  - `OrganizationMerchantPage.tsx:293,363` 项目业态同样 `display(row.venueType)`
  - `OrganizationMerchantPage.test.tsx:138` **断言** `toContain("bar")`
  - `SubjectBindingsPage.tsx:196` 表头「合同主体 ID」「结算主体 ID」（抽屉已用「编号」）
- **HTML 原文**（类型 1，PGM-001 `:5034`）：「展示给用户的一切文字，包括菜单、页面标题、字段名、状态、**枚举取值**、按钮、提示、报错……**必须全部为中文，不得出现任何英文**（如 active/draft 英文状态……）。可以不用中文的只有四样……单据号、设备编号、二维码内容、系统内部标识符。」表头「ID」是给人看的字段名，不是四样例外。
- **业务影响**：首次打开拓扑/组织页，业态列可见 `bar`，空间类型可见 `hall`/`shop`；绑定表头可见 `ID`。违反全中文铁律。
- **可复现命令/最小反例**（类型 4）：`LABEL bar -> 酒吧`（若调用 formatBusinessStatus 本可中文，页却用 display）；`LABEL shop -> <passthrough shop>`；CMD-VITEST 组织页用例明确 expect `bar`。
- **冻结判据**：PGM-001、GEN-015、GEN-008
- **2.3 依据类型**：1 + 4
- **四分类**：**冲突**
- **工程缺陷**：是
- **建议入队**：是
- **最小处置**：项目 `venueType` 走 `formatBusinessStatus`（bar→酒吧 已在表内）；空间类型读 `/api/v1/platform/space-types` 的中文 `typeName` 或页内 8 类中文表（与 V184 一致）；表头改为「合同主体编号」「结算主体编号」。statusLabels.ts 不在本模块，不强制改它。测试改为断言「酒吧」「商铺/铺位」等中文、`not.toContain("bar")`。

### GR-M012-005

- **标题**：本模块 vitest 夹具仍使用已删除的 `entity_role:"operation"`、收费模板码 `cooperation_form:"fixed_rent"`、非字典 `space_type:"hall"`、非绑定三态 `terminated`，并断言「运营主体」「固定租金」
- **文件:行号**：
  - `GovernanceTopologyPage.test.tsx:9` `entity_role: "operation"`
  - `GovernanceTopologyPage.test.tsx:14-15` `venue_type: "bar"`、`space_type: "hall"`（字典无 hall）
  - `OrganizationMerchantPage.test.tsx:12,15,124,132,227,237`
  - `SubjectBindingsPage.test.tsx:10-11` `status: "terminated"`（变更白名单为 active/inactive/suspended）
- **转入口径**：M010 第二批 B2 明示前端夹具 `entity_role:"operation"` 不并入 M010-012、留给所属模块——即本 6 文件。M009-003：页内六类 option 已改；本测试列表/抽屉仍按旧收费模板码取数。
- **后台闭集（只读对照，非本模块需求源）**：`BUSINESS_ENTITY_ROLES={platform,project_company,management_company}`；`MERCHANT_COOPERATION_FORMS` 六类；空间字典 8 码；绑定变更三态。
- **HTML 对照（GEN-008 用，不引用未映射 ID 当本模块判据）**：合作模式「自营、租赁、联营、代运营、服务商接入、其他」（BEQR `:231`）；「固定租金」是「其他」合同套用的收费模板，不是合作方式。主体角色三值见 PGM `:417-420` 顶层平台/城市项目公司/域主管公司。
- **业务影响**：28/28 绿不能在真实闭集上失败；回归无法锁 M009/M010 已收口的词表。
- **可复现命令**：CMD-VITEST 28 passed；`LABEL operation -> 运营主体`；`LABEL fixed_rent -> 固定租金`。
- **冻结判据**：GEN-003、GEN-008、GEN-017
- **2.3 依据类型**：4 + 1
- **四分类**：**细化**（闭集已在 HTML/后台写明，测试未跟到现行词表；不是 HTML 无相关条目的添加）
- **工程缺陷**：是
- **建议入队**：是
- **最小处置**：夹具改为 `platform|project_company|management_company`、`lease|self_operated|…`、`shop|building|…`、绑定 `active|inactive|suspended`；断言中文「项目公司/租赁/商铺」等；删除对「运营主体」「固定租金」作为本页合法展示的断言。创建流里「下拉不含 fixed_rent」的负例可保留。

### GR-M012-006（添加）

- **标题**：生产创建/变更请求写入「完整后台验收」夹具默认值（含用户看不见的金额与主体 ID）
- **文件:行号**：
  - `OrganizationMerchantPage.tsx:79-88` `useState("二期测试商户")`、`"运营联系人"`、`"13800000000"`、`"lease"`
  - `OrganizationMerchantPage.tsx:170-189` 另静默：`businessEntityId: entities[0]?.id ?? "org-1"`、`contractEntityId: "contract-subject-1"`、`settlementEntityId: "settlement-subject-1"`、`mallBlockCode: "1F-A"`、`areaSquareMeter: 128.5`、`leasedAreaSquareMeter: 120`、`secondaryContactName: "备用联系人"`、`secondaryContactPhone: "13900000000"`、`rentPerSquareMeter: 12.3`、`status: "active"`、`operatorNote: "完整后台组织与商户档案验收创建"`、`evidenceAttachmentIds: ["att-merchant-profile-create"]`
  - `SubjectBindingsPage.tsx:48` `useState("完整后台验收变更")`
  - `SubjectBindingsPage.tsx:135` `evidenceAttachmentIds: ["evidence-full-admin"]`
- **页面自身边界文案**（`OrganizationMerchantPage.tsx:21`）：「组织与商户档案只维护基础档案，不在本页替代四类主体绑定」——与静默 POST 三主体 ID 相反（作 GEN-002 事实，分类仍走添加：这些是业务默认值）。
- **业务影响**：首次点「＋ 新建商户」，表单已填测试商户/电话；确认页看不到的租金 12.3 与伪造合同/结算主体仍会写入（merchants 三列无 FK，可落库）。首次打开绑定页，变更原因已是验收句，提交带不存在的附件 ID。
- **HTML**：三份 HTML **无**「创建商户时默认二期测试商户/12.3 元租金/org-1」或「变更原因默认完整后台验收」。最近对照：BSM-001 `:509-510` 要记清楚谁在经营、签了什么合同——不是写死假 ID。
- **冻结判据**：GEN-017、GEN-004、GEN-011、BSM-001
- **2.3 依据类型**：1（HTML 无该默认）+ 4（源码即提交字面量；创建测试只 assert 名称进 POST，不锁隐藏字段）
- **四分类**：**添加**（业务默认值、金额、主体 ID，属 controller 2.1 业务发明，不得伪装技术性）
- **工程缺陷**：是（行为有害），**建议入队：否（待 B2 按 2.2 询问；不得在 B1 代答）**
- **最小处置**：B2 用下列模板逐条问（本席不写 `questions/` 文件）。若答「删除」：表单清空、隐藏字段改为用户选真实主体或根本不传（与边界文案一致）；绑定原因必填空初始值、证据改为真实附件或后台允许的空列表。

【添加项草稿 A — 供主调度 2.2】

HTML 原文（出处：三份 HTML 中无任何相关条目；最接近 BSM-001 `/docs/business-system-map-for-operations-v0.1.html:509-510`）：
> 哪个综合体、谁在经营、在哪经营、签了什么合同、谁能操作、谁负责。

添加了什么：
OrganizationMerchantPage 创建请求预填/隐写测试商户名、电话、合作方式 lease、面积、租金 12.3、假主体 ID org-1/contract-subject-1/settlement-subject-1、status=active、验收备注与假附件；SubjectBindingsPage 预填「完整后台验收变更」并 POST `evidence-full-admin`。

添加后的代价：
验收数据可进生产库；无 entity.binding.view 的账号首次创建即写 org-1；审计证据不可追溯；与页内「不替代四类主体绑定」文案相反。

删除的代价：
创建/变更须用户填写或选择真实主体与原因；现有引导流测试需改断言（不再依赖预填电话 `13800000000` 才能走到校验下一步——当前测试会改电话，删除预填不破坏名称/业态校验）。

### GR-M012-007（添加）

- **标题**：联系电话前端写死「11 位中国大陆手机号」正则
- **文件:行号**：`OrganizationMerchantPage.tsx:25,164-166,453` `TEXT.contactPhoneInvalid`；`/^1[3-9]\d{9}$/`
- **HTML**：三份无商户联系电话格式闭集。最接近：通用「操作有引导」BSM-003 导语，未规定号段。
- **业务影响**：座机/海外号/分机无法通过本页创建（后台 `saveMerchant` 对 phone 仅 trimToNull，无此正则）。
- **冻结判据**：GEN-017、GEN-004
- **2.3 依据类型**：1（无相关条目）
- **四分类**：**添加**（校验规则=业务发明）
- **工程缺陷**：是否成立取决于 2.2（保留=产品规则；删除=去掉或改为非空即可）
- **建议入队**：否（待 2.2）
- **最小处置**：B2 询问。本席不代答。

【添加项草稿 B — 供主调度 2.2】

HTML 原文（出处：三份 HTML 中无任何相关条目；最接近 BSM-003 `:563-564`「整套系统都遵守的通用规则」无电话格式）：
> 下面这几条不属于某一个子系统，而是整套系统都遵守的通用规则。

添加了什么：
OrganizationMerchantPage 引导流/提交以 `/^1[3-9]\d{9}$/` 拒绝非 11 位大陆手机号。

添加后的代价：
合法非手机号联系方式无法建档；与后台宽松校验不一致（GEN-008）。

删除的代价：
失去明显填错（12345）的即时拦截；测试「命中手机号校验」需改成非空或其它已批准规则。

### GR-M012-008

- **标题**：存档/取消存档/申请删除/批准驳回无测试；写路径无 try/catch，失败可成未处理拒绝
- **文件:行号**：`OrganizationMerchantPage.tsx:212-249` `archiveMerchant`/`requestDeleteMerchant`/`approveDeleteMerchant` 无 catch；测试文件 261 行无 archive/unarchive/delete 用例（12 例均为 KPI/抽屉/引导创建）
- **业务影响**：BSM-003 标题含「数据可改可删」，本页是商户存档删除的唯一前台。关键分支无任何可失败断言。网络/400 时创建有弹窗错误，存档没有。
- **可复现命令**：CMD-VITEST 12 例名称不含 archive/delete；`rg archive|unarchive|delete-request OrganizationMerchantPage.test.tsx` 无业务断言（本席以全文阅读确认，不另造测试）。
- **冻结判据**：GEN-003、GEN-009、BSM-003
- **2.3 依据类型**：4
- **四分类**：**技术性**
- **无业务发明理由**：存档删除已是后台与页内按钮的既有行为；补的是测试与错误可见性，不新增业务状态。
- **工程缺陷**：是
- **建议入队**：是
- **最小处置**：各一条可失败测试（存档带 reason 的 POST 路径；空理由拦截；删除申请；批准/驳回权限码）；写路径 catch 后 `setGlobalError`，与创建流一致。不扩 M009 后台。

## 6. 观察（无新根因 finding，完整登记）

| ID | 内容 | 收窄 |
|---|---|---|
| OBS-01 | 项目表商户列展示 `merchantId` 而非商户名；有 TraceLink | 系统内部标识符属 PGM-001 四样例外；不升冲突 |
| OBS-02 | 绑定即使改键后仍只显示主体 ID、无名称 | 后台记录无名称字段；不升「必须联表」为新需求 |
| OBS-03 | 本页无「创建绑定」入口；后台 POST `/business-entity-bindings` 存在 | HTML 未规定必须在本页创建；沉默不升冲突 |
| OBS-04 | `formatBusinessStatus("active")`=`有效`、`"suspended"`=`已封禁`，与商户「正常/暂停」、空间「可用」用词不同 | 映射在 `statusLabels.ts`（M009-003 明示不在本模块）；本页跟随共享表。不把跨模块词表当本模块冲突 |
| OBS-05 | 合作方式六类页内 `COOPERATION_FORMS` 已与后台同词；`lease` 等不在 statusLabels，靠页内 label | M009-003 已完成的部分属实；残留问题在测试夹具（GR-005） |
| OBS-06 | 引导流测试注释仍写「业态五值」，代码已含 media/传媒；测试未断言传媒 | 注释陈旧；不单独开缺陷，并入 GR-005 夹具清理时可带 |
| OBS-07 | 幂等键 `Date.now()`，双击可建两单 | 铁律 6：首次正常单击不撞 |
| OBS-08 | 列表默认 500 条，页不传 page | 后台 LIST_MAX；非本页发明 |
| OBS-09 | `parentEntityId` 本 6 文件 0 命中 | 与 M009-012 一致 |
| OBS-10 | TechDetails 展示 `/api/v1/platform/...` 路径 | 系统内部标识符；折叠技术区 |
| OBS-11 | HighRiskConfirm / 存档 prompt / 抽屉 / KPI 横幅中文 | 通过面 |
| OBS-12 | 取消存档不填理由 | 与后台「取消存档可空」及 HTML「点取消存档即可」一致 |
| OBS-13 | ADM-GOV-001/002 路由不含 `entity.binding.view`，主体列降级为空是有意防白屏 | 读路径通过（有测试）；写路径 org-1 归 GR-006 |
| OBS-14 | `empty={false}` 自绘空态 | 技术安排，无业务发明 |
| OBS-15 | 本模块 vitest 28/28 通过 | GEN-003：通过本身不替代行为证据 |
| OBS-16 | HTML 三文件未发现需人闸的原文错误 | 不上报改 HTML |
| OBS-17 | OpenAPI 绑定 change 的 path 描述含 “undefined 参数” 字样 | 属 OpenAPI 文件（M013 域），不并入本 6 文件缺陷 |
| OBS-18 | 空间状态 `unavailable` 若出现亦英文 | 与 GR-004 同类；种子若全 active 则少见，处置同空间类型中文表 |

## 7. 命令证据

### CMD-LOCK / 文件哈希 / HEAD

见 §1–§2。`git diff --cached --stat` 空。本席未 `git add`。

### CMD-VITEST

```text
cd /home/james/complex-management && npm run --workspace apps/web test:unit -- \
  src/admin/pages/GovernanceTopologyPage.test.tsx \
  src/admin/pages/OrganizationMerchantPage.test.tsx \
  src/admin/pages/SubjectBindingsPage.test.tsx
```

- exit=0
- Test Files 3 passed (3)
- Tests 28 passed (28)（拓扑 9 + 组织 12 + 绑定 7）
- Duration 1.57s（墙钟约 1.8s）
- jsdom：`Window.scrollTo` / `getComputedStyle(pseudo)` Not implemented（不影响红绿）
- **不把 28 绿写成行为通过。**

未跑：会清共享库的脚本、后端 IT、全仓 vitest、npm install、浏览器 E2E。后端接口只读核对源码/OpenAPI，未对 `complex_test` 发写。

### CMD-UNWRAP（只读 python，未写盘）

见 GR-001/002/003 反例。statusLabels：`bar→酒吧`（页未用）、`shop→passthrough`、`operation→运营主体`、`fixed_rent→固定租金`、`lease→passthrough`。

## 8. 结论

**FAIL**。

理由（任一即不得 PASS）：存在未定稿工程缺陷 GR-001–005、GR-008；存在未答添加项 GR-006、GR-007。无有条件通过。

建议 B2 合取时：冲突/细化/技术性可自动入队（001–005、008）；006/007 必须 2.2 逐条问，未答则 BLOCKED-ASK。本席不修复、不写 questions 文件、不改 HTML/锁/台账。

覆盖声明：6/6 文件全文三线；21/21 判据已核对；无实现面的 GEN-013/014 已写原因。

## M012 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 40713 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，独立）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M012 FILES 6 / 判据 21：BSM-001、BSM-003、GEN-001–018、PGM-001）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = merge-base = `bd445728a6ec7bb36a9e7ff19fe56927410ac61e`。工作区含 M011 已闭环 B3 修复（未提交）；本模块 6 文件 `git status --porcelain` 空，指纹与 B1 覆盖表逐文件全等。B3=0。无源修复。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账。
- 本席 B2-r1：**FAIL**（否决点仅 006 最小处置发明业务；001–005、007–009 逐 ID 票 PASS，其定稿措辞见各节，不得因本批 FAIL 丢弃）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 与 ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。未重跑 vitest（6 文件指纹与 B1 表全等，B2 只核差集与处置）。

此票只判断 findings.md V1.39 顶部「当前 M012 2.2答复已到，B2-r1候选定队列」及其下「当前 M012 B2-r1 整批候选」相对本次两份完整 B1 raw、已答 questions 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M012-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 40713 | `93333eb5c29a14de1ba7c770051014b5659550e58c4024aa4e2e1641d649aacf` |
| `docs/audit-2026-09/modules/M012-b1-self-raw.md` | V0.01 | 21559 | `249952156dcb1a85a8f77437f1c3e72aa6b01e104da6619535df49a377a487ea` |
| `docs/audit-2026-09/findings.md` | V1.39 | 525715 | `a0abab33e9b8790c48f9393fbba434334adadfbb57953b69aae71ffc36ec3934` |
| `docs/audit-2026-09/questions/M012-006.md` | V0.02 | 2250 | `10afba22473850cfa245b2fc4e5d40436cf1ce83ae91e2ab5fae08c09def109a` |
| `docs/audit-2026-09/questions/M012-007.md` | V0.02 | 1324 | `fe3a18a768464ac000734771f5e57f76cca5c01ee89cae3bcd3a0b9d7a348f75` |

ledger 只读：V2.37，441442 bytes，SHA256 `0c2d320ab3cccbf4aeb12e4829fb3e65286c82afd89850599de5452ea6f4542a`；M012 IN_PROGRESS、B3=0。questions「## 操作员答复」首个非空行均精确为「删除」。映射行 `M012 → [BSM-001, BSM-003, GEN-001 … GEN-018, PGM-001]` COUNT=21 unique=21 MISSING=[]。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

候选形态：V1.39 顶部为 2.2 答复后的**定队列段**（006/007 队列改是、处置改写）；其下「整批候选」001–009 原表保留（006/007 单元格仍写待答）。本席以顶部定队列为 006/007 的当前有效候选处置，原表其余 ID 七列仍有效。不把原表残留「待2.2」当作本批队列。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers | 先核技能；用户/提示词覆盖技能本体 |
| receiving-code-review | 先核源/命令再接受或反驳；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：006 是否误把 requireBusinessEntity 安到 saveMerchant、004 空间类型中文最懒来源、005 是否漏退役码、008 下钻是否首次必撞 |
| verification-before-completion | 声称前先跑只读哈希/行号核对；本票不声称 28 用例本轮新绿 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码 |
| codebase-design | 删除测试：绑定页深度落在后台记录四 ID，不落 types.ts 臆造七字段 |
| ponytail-review | 只评最小删除/收窄，不落地。006 三主体选择器 = 发明；004 另拉 /space-types = 非最懒；008 下钻 = 铁律 6 不修 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到**；未冒充调用 |
| Arc audit | 会派子代理并另写 `docs/arc/audits/`，与双席冲突；**未用** |

### 完整差集（GR-001…008、OBS-01…18、席甲 CX-01…06）

漏并：无。两席 B1 全部临时 ID 仍落在 001–009。001/003 仍按本席 GR 收入，不是席甲发明。席甲 B1 宣称「无 2.2 添加项」、把 CX-01 标技术性：分类以本席添加为准（已走 2.2，操作员已答删除）。席甲把 `PlatformService.java:560-561` `requireBusinessEntity` 误标为 createMerchant 用——核源该两行在 `createBusinessEntityBinding`，不在 `saveMerchant`；此误标进入候选 006 事实栏与顶部定队列，是本批否决的根。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M012-001 | GR-M012-001（席甲 B1 未发现，候选已核实） | 技术性/是/是 | **PASS（技术性/是/是）** |
| M012-002 | GR-M012-002 + 席甲 CX-M012-02（缺 action 半边） | 技术性/是/是 | **PASS（技术性/是/是；与 006 证据/原因分界见下）** |
| M012-003 | GR-M012-003（席甲 B1 未发现，候选已核实） | 技术性/是/是 | **PASS（技术性/是/是）** |
| M012-004 | GR-M012-004 + 席甲 CX-M012-05 | 冲突/是/是 | **PASS（冲突/是/是；空间类型中文=页内 8 码表）** |
| M012-005 | GR-M012-005 + 席甲 CX-M012-04/06 | 细化/是/是 | **PASS（细化/是/是；同文件退役码未漏）** |
| M012-006 | GR-M012-006 + 席甲 CX-M012-01 技术侧 + CX-02 证据侧 | 添加/是（答删除）/是 | **FAIL（队列是成立；顶部处置发明业务，否决定稿）** |
| M012-007 | GR-M012-007 | 添加/是（答删除）/是 | **PASS（添加/是/是；非空=本题推荐替换）** |
| M012-008 | GR-M012-008 + 席甲 CX-M012-03 | 技术性/是/是 | **PASS（技术性/是/是；下钻必修超出铁律 6，定稿拿掉）** |
| M012-009 | GR OBS-01…18 + 席甲注记 | 技术性/正向 /否/否 | **PASS（否/否；OBS-06/18 已并入 005/004，不得写成无缺陷）** |

冲突 1（004）。细化入队 1（005）。技术性入队 4（001、002、003、008）。添加入队 2（006、007；006 处置须 r2）。否 1（009）。

### 001 独立裁定（技术性／是；页内改键，types.ts 不改）

核源：`SubjectBindingsPage.tsx:59-67,106,196-209` 读 `businessObjectId/operatorSubject/contractSubject/contractSubjectId/settlementSubject/settlementSubjectId/auditEvidenceRef`；`uniqueObjects` 基于 `businessObjectId`；合同主体 KPI 基于 `row.contractSubject`；选中展示 `selected?.businessObjectId`。`BusinessEntityBindingRecord` 字段为 `merchantId/operationEntityId/contractEntityId/settlementEntityId/status/effectiveFrom/effectiveTo/changeReason`。`types.ts:1148-1158` 七臆造字段为 optional，且**无** merchantId 等真实键。夹具 `SubjectBindingsPage.test.tsx:9-12` 同构臆造。AdminApi unwrap 只做 snake→camel，不改语义。

四分类技术性成立。无业务发明：后台已有四 ID；缺陷是键名错位。types.ts 不在本 6 文件，页内局部类型是最懒，改共享类型会波及其他页引用。KPI 按 merchantId 去重锁的是「唯一绑定对象」在真实响应下非 0，是本条最小可见修复，不是新口径。

最小处置（保持候选，补一句）：页内改读真实键；唯一绑定对象按 merchantId 去重；合同主体 KPI 改读 `contractEntityId`、选中展示改读 `merchantId`；抽屉/表体随改；夹具改真实 snake_case（`merchant_id` 等）；**不改** `types.ts`。禁止联表取主体名（OBS-02，后台记录无名称）。表头「ID」→「编号」归 004，001 只换所读字段。

票：**PASS**。

### 002 独立裁定（技术性／是；三态下拉+POST action）

核源：`SubjectBindingsPage.tsx:132-136` 请求体仅 reason/idempotencyKey/假证据，无 action。`StatusChangeRequest` 与 `changeBusinessEntityBinding:602-604` `requireText(action)` 且限 `active/inactive/suspended`。OpenAPI required 含 action。页面无三态控件。测试 7 例不发 POST。变更 API **不接受**新的三主体 ID，只改 status/reason（`:619-630`）；激活时才 `syncMerchantEntities`。

四分类技术性成立。无业务发明：三态已在后台/OpenAPI。最懒：表单加「启用/停用/暂停」中文下拉（值即三态），POST 带 action；测试断言 body 含 action∈三态。缺 action 前端拦截即可。不必绕过下拉打后台 400（闸在 M009）。不要在本页加「改绑三主体」字段（change 接口无此槽，属发明，且与 OBS-03 不在本页创建绑定一致）。

与 006 合并落地边界：002 不得把 `evidenceAttachmentIds:["evidence-full-admin"]` 或默认原因「完整后台验收变更」锁进新测试。原因空初始且必填、假证据不传，归 006。合并后 body = `{action, reason, idempotencyKey}`，证据省略或空列表（OpenAPI required 无证据；`changeBusinessEntityBinding` 不 `requireText` 证据）。002 与 006 可同批改同一文件，测试只留一条提交断言，归因 002（action）+006（原因/证据），禁止各写一条重复 POST。

票：**PASS**。

### 003 独立裁定（技术性／是）

核源：`GovernanceTopologyPage.tsx:97-99` `space.venueId`；`SpaceRecord.java:8-21` 有 `projectId` 无 `venueId`；`types.ts:1109-1110` 标旧臆造。拓扑夹具已给 `project_id`、未给 `venue_id`，测试不断言该 KPI → 恒 0 被绿测放过。最小处置：`space.projectId && projectIds.has(space.projectId)`；测试用真实 `project_id` 断言非 0。不发明「挂接」新定义。

票：**PASS**。

### 004 独立裁定（冲突／是；空间类型中文最懒=页内 8 码表）

核源：项目业态四处 `display(venueType)`（组织页 `:293,:363`，拓扑 `:125,:168`）；`formatBusinessStatus("bar")`=`酒吧` 已在 `statusLabels.ts:197`，页却不用。空间类型 `formatBusinessStatus(space.spaceType)` 对 V184 八码回退原码（statusLabels 无 shop/building/zone/floor/public_area/parking/booth）。表头「合同主体 ID」「结算主体 ID」（`:196`）；抽屉已用「编号」。测试 `toContain("bar")`。HTML PGM-001：展示文字含枚举取值必须中文，表头字段名不在四样例外。

四分类冲突成立。statusLabels.ts 不在本模块，**不改**。

空间类型中文最懒来源：**页内 8 码中文表，与 V184 种子逐字一致**，禁止本模块改 statusLabels，也不拉 `/space-types`。理由：COOPERATION_FORMS 已是本域页内表先例；另一次 GET 增加加载/失败面，非最懒；字典虽可后台增改，本批只锁现行种子 8 码（building 楼栋、floor 楼层、zone 区块/区域、shop 商铺/铺位、public_area 公共区域、parking 车位/车库、booth 摊位/展位、other 其他）；新增类型仍原码直到共享表所在模块处理。ADM-GOV-001 虽有 `governance.space.view`，有权限不等于本页必须多打一接口。

OBS-18 `unavailable` 与本条同类：statusLabels 无该键；后台 `requireSpaceStatus` 文案已是「可用/不可用」。并入 004 页内表（`active`/`unavailable`→可用/不可用），不另开 ID。项目 `venueType` 走 `formatBusinessStatus`（bar→酒吧）。表头改「合同主体编号」「结算主体编号」。测试改中文、`not.toContain("bar")`。

票：**PASS**。

### 005 独立裁定（细化／是；同文件退役码未漏）

本 6 文件只读扫描存活退役/非法码：

| 字面量 | 位置 | 归本 ID？ |
|---|---|---|
| `entity_role:"operation"` | 组织测试 `:12`、拓扑测试 `:9` | 是 → `platform`/`project_company`/`management_company` |
| `cooperation_form:"fixed_rent"` | 组织测试 `:15` 及「固定租金」「运营主体」断言 | 是 → 六类之一（`lease` 等） |
| `space_type:"hall"` | 拓扑测试 `:15` | 是 → V184 八码之一（`shop` 等） |
| `status:"terminated"` | 绑定测试 `:11` | 是 → 绑定三态之一 |
| 注释「业态五值」 | 组织测试 `:158` | 是（CX-06）→ 六值，可断言传媒 |
| `not.toContain("fixed_rent"|"revenue_share")` | 组织测试创建流 | **负例，保留** |
| `venue_type:"bar"` | 两测试 | 否：bar 仍是合法业态码，英文直出归 004 |
| `business_object_id` 等臆造键 | 绑定夹具 | 否：归 001 |
| `spv` / `fixed_rent_plus_commission` / 商户 `draft` / `venue_id` | 本 6 文件无存活夹具 | 无 |

页内 `COOPERATION_FORMS` 六类已与后台同词（OBS-05），不属本条。最小处置维持候选：夹具改现行闭集，断言中文「项目公司/租赁/商铺」等，删除「运营主体」「固定租金」作为合法展示的断言，创建流负例保留，注释改六值。

票：**PASS**。未漏同文件其它退役码。

### 006 独立裁定（添加／队列是；顶部处置发明业务 → 本 ID 票 FAIL）

2.2 事实：`questions/M012-006.md`「## 操作员答复」首行精确「删除」。按 V0.17 §2.2，删除 → 入修复队列。**队列「是」成立，本席不否决入队。**

否决的是顶部定队列的**最小处置**：

> 经营/合同/结算主体由用户从 `/business-entities` 列表选择（三选一必填，缺 `entity.binding.view` 时列表为空即无法创建并提示）

核源（只读，不采信席甲 B1 行号归并）：

- `requireBusinessEntity` 定义在 `PlatformService.java:708-713`，**唯一业务调用**在 `createBusinessEntityBinding:559-561`（经营/合同/结算三 ID 必须存在）。`changeBusinessEntityBinding` 不改三 ID。
- `createMerchant` → `saveMerchant:735-788`：三主体列为 `existing == null ? trimToNull(request.businessEntityId/contractEntityId/settlementEntityId()) : existing.…`。**无**存在性校验、无必填。`operatorNote`/`evidenceAttachmentIds` 不写入 MerchantRecord。
- 注释 `:756` 与测试 `createBusinessEntityBindingSyncsMerchantEntityColumns`（`:271-283`）：「三个主体列以生效绑定为准」；激活绑定才 `syncMerchantEntities`。更新路径明确不覆盖三列（`:144-146` 改档后 `contractEntityId` 仍 null）。
- 页内边界文案 `OrganizationMerchantPage.tsx:21`：「不在本页替代四类主体绑定」。
- 路由 ADM-GOV-002 **不含** `entity.binding.view`（`adminRoutes.ts:17`）；缺该权时本页有意不请求主体列表（`:129-133`），读路径降级是防白屏（OBS-13），不是创建闸。
- merchants 三主体列无 FK（本席 B1 GEN-006 观察）：假 ID 可落库。席甲 B1「创建必 404 BUSINESS_ENTITY_NOT_FOUND」把绑定创建的闸安到商户创建上，**事实错误**；候选原表 006 事实栏与顶部定队列沿用该误标。

铁律 6 / GEN-017：2.2「删除」=删掉添加的验收默认值与假主体/假证据，不是用新的必填选择器+新权限耦合去替换。把「选真实主体**或根本不传**」（本席 B1 原文，且候选原表 006 仍写「或不传」）收成「三选一必填 + 缺 view 无法创建」，是：

1. 发明「建档必须先有三主体」的流程（HTML 无；后台创建允许三列 null）。
2. 发明「ADM-GOV-002 创建商户需要 entity.binding.view」的权限（路由没有；本页缺权不请求是读路径设计）。
3. 在本页写三主体列却不创建绑定记录，与 M009-008「生效绑定是唯一来源」分叉；随后绑定激活会覆盖，档案页写入是死字段。
4. 比「不传」多一整块选择器 UI，不是最懒。

questions 006「删除的代价」本身是「选择…**或**按四类主体绑定流程另行绑定」。操作员答删除，并未裁定必须走选择器。顶部定队列丢掉「或不传」，改写了本席 GR-006 原意。

B2-r2 正确口径（本批不得按顶部 006 定稿）：

- 队列仍是。分类仍添加。
- 表单清空商户名/联系人/电话/合作方式预填（合作方式空＝后台未定，下拉可空 option，不发明新码）。
- 创建请求**不传** `businessEntityId/contractEntityId/settlementEntityId`（及区块/面积/备用联系人/租金/status/operatorNote/假证据）。status 不传则 `requireSubjectStatus` 缺省 active，不是本页发明。
- **不**新增三主体选择器，**不**把 `entity.binding.view` 变成创建前置。缺该权时创建仍可进行（三列 null），与现路由一致。
- 三主体由 `createBusinessEntityBinding` + `syncMerchantEntities` 承接。本页无创建绑定入口维持 OBS-03（HTML 未规定必须在本页创建，沉默不升冲突，B3 禁止顺手加创建绑定）。
- 绑定页：原因空初始且必填；不提交假证据（省略或 `[]`）。不要做附件上传器。
- 测试：创建 POST 断言不含假 ID/假证据/验收备注/写死面积租金；绑定提交断言不含 `evidence-full-admin`。名称/业态校验保留。

票：**FAIL**。否决本批定稿。

### 007 独立裁定（添加／是；非空=本题推荐替换）

2.2：`questions/M012-007.md` 答复首行「删除」。添加物是 `/^1[3-9]\d{9}$/`。本题「删除的代价」写明「改为非空校验」；操作员答的是该问。本席 B1 原文「去掉或改为非空即可」。

后台 `saveMerchant:767` 对电话 `trimToNull`，**不**非空、无号段。候选「与后台一致」不成立。最懒本可去掉前端格式校验、电话可空。但把「连非空也不要」当成第二问，是本席替操作员收窄已答问题，铁律 5/2.2 禁止。按已答问题：删正则，改为非空；文案/测试随改；后台不变。

定稿删掉「与后台一致」。写明：前端非空（引导流即时拦空串），后台仍可空。

票：**PASS**。

### 008 独立裁定（技术性／是；下钻超出铁律 6）

核源：`archiveMerchant/requestDeleteMerchant/approveDeleteMerchant`（`:212-249`）无 try/catch；创建流有。组织测试 12 例无存档/删除。绑定测试对 POST `/change` 零覆盖（CX-03 前半）。拓扑 `focusObjectId` 下钻（`:100-110`）无测试（CX-03 后半）；`renderPage` 不传 focus，无 focus 时 effect 直接 return。

铁律 6（V0.17：「第一次正常跑就会撞上吗？」）：

- 存档/删除：本页唯一前台入口，首次点按钮即发 POST；网络/400 无 catch → 未处理拒绝。关键分支无断言。入队成立。最懒：**存档一条**（空理由拦截可同条先拦；有理由则断言 POST 路径/body 含 reason）+ **删除申请或审批一条** + 写路径 `catch` 后 `setGlobalError`（与创建流一致）。不要存档/取消/申请/批准/驳回五条 HTTP 各写一条。取消存档可空是 OBS-12，不测。
- 绑定提交：002 已要求 body 含 action 的一条断言；008「随 002」只是归因，**禁止**再写第二条 POST。
- 下钻：首次打开本页无 `focusObjectId`，行为就是不动。要「从别的页带 focus 进来」才走到滚动。不是首次必撞。列为 008 必修超出最小，**拿掉**，归 009 登记。B3 禁止为下钻加用例。

无业务发明：补的是既有按钮的测试与错误可见性。

票：**PASS**（下钻不入 008 必修）。

### 009 独立裁定（观察／否）

候选未把本席 OBS 改写成缺陷入队，也未漏 OBS 编号。须纠正两处措辞，不改队列：

- OBS-06（注释五值）本席 B1 已并入 GR-005 → 005，不得在 009 写成「无缺陷」。
- OBS-18（`unavailable` 英文）本席 B1 已并入 GR-004 → 004，同上。
- OBS-13 写路径 `org-1` 并入 006（按本票改为不传，不是改成选择器）。读路径缺权不请求维持观察。

其余 OBS-01/02/03/04/05/07/08/09/10/11/12/14/15/16/17：系统内部标识符、不联表取名、无创建绑定入口、statusLabels 跨模块用词、幂等键 Date.now()、列表 500、TechDetails 路径、取消存档可空、OpenAPI「undefined」属 M013——维持否。席甲无独立编号 OBS 表；「席甲 OBS/注记」所指 parentEntityId 0 命中即本席 OBS-09 / M009-012，未写错。

票：**PASS（否）**。

### 分类 / 队列 / 允许的最小修复边界

本批不得定稿。B2-r2 若只改 006 处置为「不传三主体、不新选择器、不把 entity.binding.view 当创建闸」，并写入 008 下钻降 009、004 页内 8 码表、007 删「与后台一致」，则队列是=001–008；否=009。一次 B3-r1 只改本模块 6 文件，不改 types.ts、statusLabels.ts、M009 后台、三 HTML、锁、迁移、权限种子、路由权限码。可执行行为先写归属该 ID 的最小失败测试（GEN-003）：

- 001：夹具改真实 snake_case 后现页必红（全「-」/KPI 0）；再改页内读键见绿。types.ts 不动。
- 002：一条提交断言 body.action∈三态；缺 action 拦截。假证据/默认原因归 006，不在本条锁死。
- 003：真实 `project_id` 断言已挂接非 0。
- 004：业态/空间类型/表头中文；`not.toContain("bar")`；页内 8 码表 + unavailable→不可用；不改 statusLabels、不拉 /space-types。
- 005：夹具闭集 + 中文断言；负例保留。
- 006（r2 后）：不传三主体及假字段；原因空初始必填；创建仍可不持 entity.binding.view。
- 007：正则→非空；后台不改。
- 008：存档一条 + 删除一条 + catch。绑定 POST 不另写。下钻不写。

禁止：商户创建页三主体必选；缺 `entity.binding.view` 禁止创建；本页加创建绑定；改 types.ts/statusLabels.ts；为每个存档/删除 verb 各写一条；为下钻写用例；把 009 观察升格入队。

改动文件自身版本递增（三页面现 V0.01 或无版本头的加 V0.01）。

### 结论

全批差集无漏并。001–005、007–009 逐 ID 票 PASS。006 入队正确，但顶部定队列把「不传三主体 / 或不传」收成必选选择器 + 新权限闸，误用 `requireBusinessEntity`（该闸在绑定创建，不在 `saveMerchant`），改写本席 GR-006 原意并发明业务，本席 **FAIL**。须 B2-r2 按本票改写 006 处置后再票；未经双 PASS 不进 B3，更不称模块 PASS。步骤仍 B2，M012 IN_PROGRESS，B3=0。

## M012 B2-r2 整批归并复核（Grok 乙席）

- 文件自身版本：V0.03（本段追加；上方 V0.01 B1 40713 字节 + V0.02 B2-r1 追加，全文 63960 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，独立）。步骤：B2-r2 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M012 FILES 6 / 判据 21：BSM-001、BSM-003、GEN-001–018、PGM-001）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = merge-base = `bd445728a6ec7bb36a9e7ff19fe56927410ac61e`。工作区含 M011 已闭环未提交修复；本模块 6 文件 `git status --porcelain` 空，指纹与 B1 覆盖表逐文件全等。ledger V2.38 IN_PROGRESS；B3=0；无源修复。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲 B2-r1 PASS，只核 r2 候选是否落实本席 B2-r1。
- 本席 B2-r2：**PASS**（001–009 逐 ID 均 PASS；r1 唯一否决点 006 处置已闭合）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容与 ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。未重跑 vitest（6 文件指纹与 B1 表全等，B2 只核差集与 r1 意见落实）。

此票只判断 findings.md V1.40 顶部「当前 M012 B2-r1 合取FAIL…B2-r2 修订候选」相对本次两份完整 B1 raw（含各自 B2-r1 追加段与席甲勘误）、已答 questions 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法，并核对该候选是否准确落实本席 B2-r1（V0.02）否决与收窄意见、有无新的改写或遗漏。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL**、V0.02 B2-r1 **FAIL** 原文。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M012-b1-grok-raw.md` 前缀（本席 B1+B2-r1，本段追加前） | V0.02 | 63960 | `875e3e8bd54f6c74710952f167b7ce4465d7ecebfe6f0603f0765636ed7a5e2e` |
| 其中 B1 前缀 | V0.01 | 40713 | `93333eb5c29a14de1ba7c770051014b5659550e58c4024aa4e2e1641d649aacf` |
| `docs/audit-2026-09/modules/M012-b1-self-raw.md`（席甲 B1+B2-r1 勘误） | V0.02 | 27474 | `7033fc88d7048e36ec4a3703afa6253133745d03ed111cc3a1cf4c4be413fcb8` |
| 其中席甲 B1 前缀 | V0.01 | 21559 | `249952156dcb1a85a8f77437f1c3e72aa6b01e104da6619535df49a377a487ea` |
| `docs/audit-2026-09/findings.md` | V1.40 | 529212 | `34c8270395110560181415c1b9f38ba7e6adfe3bfc0f42677303f0d9ee909779` |
| `docs/audit-2026-09/questions/M012-006.md` | V0.03 | 3134 | `53d069e2a1a37109c125170db3ff199790bd1bc060c3338f4bd4eb5bf26acde5` |
| `docs/audit-2026-09/questions/M012-007.md` | V0.02 | 1324 | `fe3a18a768464ac000734771f5e57f76cca5c01ee89cae3bcd3a0b9d7a348f75` |

ledger 只读：V2.38，442831 bytes，SHA256 `371445d6cf055d1f426fb8fa2716dc7e7750f6957cbacf0be0806ed1c1b8d00f`；M012 IN_PROGRESS、B3=0。questions「## 操作员答复」首个非空行均精确为「删除」（006 勘误段不改首行）。映射行 `M012 → [BSM-001, BSM-003, GEN-001 … GEN-018, PGM-001]` COUNT=21 unique=21 MISSING=[]。本文件独占锁哈希字段行仍仅文首一处，本段未新增。6 文件指纹与 B1 覆盖表逐文件全等。

候选形态：V1.40 顶部为 r2 **条目覆盖段**（非整表重写）；其下 2.2 答复段与「当前 M012 B2-r1 整批候选」001–009 原表保留。未改条目（001/002/003/005）沿用 r1 表七列；修订条目以顶部五条为准：006/007/008/009 旧处置单元格作废；004 原表七列仍在，顶部补充收窄空间类型中文来源并并入空间状态。本席按此读取，不把 2.2 段「三选一必填 + entity.binding.view」或原表 006「创建必 404」/007「与后台一致」/008「下钻一条」当作本批有效处置。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers | 先核技能；用户/提示词覆盖技能本体 |
| receiving-code-review | 先核源/命令再接受或反驳；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：006 是否仍发明三主体必选、004 是否仍开 /space-types、008 是否仍写下钻、007 是否仍写「与后台一致」、有无新改写 |
| verification-before-completion | 声称前先跑只读哈希/行号核对；本票不声称 28 用例本轮新绿 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码 |
| codebase-design | 删除测试：商户创建深度落在档案字段，不落三主体选择器；绑定页深度落在后台四 ID |
| ponytail-review | 只评最小删除/收窄，不落地。三主体选择器 / 创建闸 / 下钻用例 / 另拉 /space-types = yagni |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到**；未冒充调用 |
| Arc audit | 会派子代理并另写 `docs/arc/audits/`，与双席冲突；**未用** |

### r1 意见 → r2 候选对照（本席 B2-r1 原文，不采信席甲转述）

本席 B2-r1：**FAIL**。唯一否决点 006：队列「是」成立，但 V1.39 顶部定队列把「不传三主体 / 或不传」收成「三选一必填 + 缺 entity.binding.view 无法创建」，误把 `requireBusinessEntity`（`createBusinessEntityBinding:559-561`）安到 `saveMerchant`。另提出 004 空间类型=页内 8 码表且 OBS-18 并入、007 删「与后台一致」、008 下钻拿掉归 009、009 措辞。001/002/003/005 票 PASS，口径不变。

| 本席 r1 要求 | r2 候选（V1.40 顶部） | 落实？ |
|---|---|---|
| 006 队列仍是、分类仍添加；表单清空预填；创建**不传**三主体及区块/面积/备用联系人/租金/status/operatorNote/假证据；不新选择器；不把 entity.binding.view 当创建闸；三主体由绑定创建 + syncMerchantEntities 承接；绑定原因空初始必填、假证据省略；不做附件上传器；事实栏 404 作废改悬空落库 | 「不传」三主体及隐藏字段；「不新增三主体选择器、不把 entity.binding.view 当创建前置」；承接句与页内边界/M009-008 一致；原因空初始必填、假证据省略；不做附件上传器；「创建必 404」作废改「假主体 id 静默落库为悬空引用（三列无 FK）」 | 是 |
| 004：页内 8 码中文表与 V184 逐字一致；不改 statusLabels、不拉 /space-types；OBS-18 active/unavailable→可用/不可用并入 | 「空间类型走页内 8 码中文表（楼栋/楼层/区块/区域/商铺/铺位/公共区域/车位/车库/摊位/展位/其他）」；空间状态页内映射「可用/不可用」（OBS-18 并入）；不改 statusLabels.ts、不拉 /space-types | 是 |
| 007：删「与后台一致」；前端非空（引导流即时拦空串），后台仍可空、不改 | 同句定稿 | 是 |
| 008：存档一条（空理由拦截与有理由 POST 同条）+ 删除申请或审批一条 + catch；绑定提交随 002 只一条；下钻拿掉归 009，B3 禁止为下钻加用例 | 同句收窄 | 是 |
| 009：OBS-06 并入 005、OBS-18 并入 004、OBS-13 写路径 org-1 并入 006（改为不传）；其余观察维持否；下钻无测试登记于此 | 同句措辞 | 是 |
| 001/002/003/005 口径不变 | 「其余001/002/003/005口径不变」；r1 表四行七列未改写 | 是 |

**r1 裁定未被误读。** 无把「不传三主体」读回选择器、无把 `requireBusinessEntity` 再安到商户创建、无把「页内 8 码表」读成仍拉 /space-types、无把「下钻拿掉」读成仍必修、无把「前端非空 / 后台可空」读成改后台。无新永久 ID、无锁外判据、无 HTML 修改申请、添加项 006/007 已答删除、不进 BLOCKED-ASK。

席甲 B2-r1 票 PASS 且 006 勘误已写「不传」、但当时 findings V1.39 **顶部**仍是选择器——本席否决的是台账顶部，不是席甲勘误正文。席甲 r1 的 008 仍写「下钻一条」，本席不以该票为准。r2 顶部已按本席 r1 改写，不构成照抄席甲。

### 完整差集（GR-001…008、OBS-01…18、席甲 CX-01…06）

漏并：无。两席 B1 全部临时 ID 仍落在 001–009。001/003 仍按本席 GR 收入，不是席甲发明。席甲 B1 宣称「无 2.2 添加项」、把 CX-01 标技术性：分类以本席添加为准（已走 2.2，操作员已答删除）。

| 正式ID | 本次来源（不改写原意） | r2 分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M012-001 | GR-M012-001（席甲 B1 未发现，候选已核实） | 技术性/是/是 | **PASS（技术性/是/是）** |
| M012-002 | GR-M012-002 + 席甲 CX-M012-02（缺 action 半边） | 技术性/是/是 | **PASS（技术性/是/是；与 006 证据/原因分界见下）** |
| M012-003 | GR-M012-003（席甲 B1 未发现，候选已核实） | 技术性/是/是 | **PASS（技术性/是/是）** |
| M012-004 | GR-M012-004 + 席甲 CX-M012-05 | 冲突/是/是（空间类型中文来源已收窄） | **PASS（冲突/是/是；页内 8 码表 + 空间状态）** |
| M012-005 | GR-M012-005 + 席甲 CX-M012-04/06 | 细化/是/是 | **PASS（细化/是/是）** |
| M012-006 | GR-M012-006 + 席甲 CX-M012-01 技术侧 + CX-02 证据侧 | 添加/是（答删除）/是（处置已改为不传） | **PASS（添加/是/是；r1 否决点闭合）** |
| M012-007 | GR-M012-007 | 添加/是（答删除）/是（措辞已删「与后台一致」） | **PASS（添加/是/是）** |
| M012-008 | GR-M012-008 + 席甲 CX-M012-03 | 技术性/是/是（下钻已拿掉） | **PASS（技术性/是/是；下钻不入必修）** |
| M012-009 | GR OBS-01…18 + 席甲注记 | 技术性/正向 /否/否（措辞已纠正） | **PASS（否/否）** |

冲突 1（004）。细化入队 1（005）。技术性入队 4（001、002、003、008）。添加入队 2（006、007）。否 1（009）。

### 001 独立裁定（技术性／是；口径与 r1 同，未改）

r1 表单元格未改。核源仍成立：`SubjectBindingsPage.tsx:59-67,106,196-209` 读臆造七字段；`BusinessEntityBindingRecord` 为 merchantId/operationEntityId/contractEntityId/settlementEntityId/status/effectiveFrom/effectiveTo/changeReason；`types.ts:1148-1158` 七臆造 optional 且无真实键；夹具同构臆造。最小处置仍：页内改读真实键；唯一绑定对象按 merchantId 去重；合同主体 KPI 改读 `contractEntityId`、选中展示改读 `merchantId`；抽屉/表体随改；夹具改真实 snake_case；**不改** `types.ts`。禁止联表取主体名。表头「ID」→「编号」归 004。票：**PASS**。

### 002 独立裁定（技术性／是；口径与 r1 同，未改）

r1 表未改：请求体无 action；后台 `requireText(action)` 限三态；页面无三态控件。最小处置仍：表单加「启用/停用/暂停」中文下拉，POST 带 action；测试一条断言 body.action∈三态。不要在本页加「改绑三主体」字段。与 006 合并落地：002 不得把假证据或默认原因锁进新测试；合并后 body = `{action, reason, idempotencyKey}`，证据省略；测试只留一条提交断言，归因 002（action）+006（原因/证据）。票：**PASS**。

### 003 独立裁定（技术性／是；口径与 r1 同，未改）

r1 表未改：`space.venueId` 不存在，改 `space.projectId && projectIds.has(space.projectId)`；测试用真实 `project_id` 断言已挂接非 0。票：**PASS**。

### 004 修订核实（冲突／是；空间类型中文来源已收窄，OBS-18 并入）

本席 r1 已 PASS 冲突入队，但把「或读 /space-types」裁掉：最懒=页内 8 码表，与 V184 种子逐字一致；OBS-18 并入。

r2 顶部补充：页内 8 码中文表（楼栋/楼层/区块/区域/商铺/铺位/公共区域/车位/车库/摊位/展位/其他）= 本席 r1 八码映射（building 楼栋、floor 楼层、zone 区块/区域、shop 商铺/铺位、public_area 公共区域、parking 车位/车库、booth 摊位/展位、other 其他）；空间状态 active/unavailable 页内映射「可用/不可用」——与 `requireSpaceStatus` 文案同词，非本页发明；不改 statusLabels.ts、不拉 /space-types。

项目业态走 `formatBusinessStatus`（bar→酒吧 已在表内）、表头改「合同主体编号」「结算主体编号」、测试改中文且 `not.toContain("bar")`：原表未改，沿用。

原表 004 处置仍写「或读 `/space-types` 的 typeName」。004 不在「旧单元格作废」名单，但是顶部补充后一句「不拉 /space-types」是对本席 r1 的收窄落地，**本批有效口径以顶部为准**。B3 若按原表去 GET `/space-types`，即违反本票。不因此 FAIL。

票：**PASS**。

### 005 独立裁定（细化／是；口径与 r1 同，未改）

r1 表未改。本 6 文件存活退役码仍只那些：`entity_role:"operation"`、`cooperation_form:"fixed_rent"`、`space_type:"hall"`、绑定 `status:"terminated"`、注释「业态五值」。创建流 `not.toContain("fixed_rent"|"revenue_share")` 负例保留。`venue_type:"bar"` 英文直出归 004。票：**PASS**。

### 006 修订核实（添加／是；r1 唯一否决点已闭合）

2.2：`questions/M012-006.md` 答复首行仍精确「删除」。队列「是」成立，本席 r1 未否决入队。

本席 r1 FAIL 理由：V1.39 顶部「三选一必填 + 缺 entity.binding.view 无法创建」发明「建档必须先有三主体」和「ADM-GOV-002 创建需要 entity.binding.view」，并误用绑定创建的 `requireBusinessEntity`。

本批类型 4 核源（只读，不采信转述）：

- `requireBusinessEntity` 定义 `:708-713`，业务调用仍在 `createBusinessEntityBinding:559-561`。`saveMerchant:763-765` 对三主体仅 `trimToNull`，可空、不校验存在。
- `requireSubjectStatus:666-668` `trimToDefault(raw, "active")`；`requireCooperationForm:655-662` 允许空（空＝尚未确定）。
- `syncMerchantEntities:716-718` 注释：生效绑定是三列唯一来源。
- 页内边界 `OrganizationMerchantPage.tsx:21`：「不在本页替代四类主体绑定」。
- 路由 ADM-GOV-002 requiredPermissions 仅 `merchant.profile.view` + `governance.project.view`，**不含** `entity.binding.view`（该码在 ADM-GOV-003）。
- questions 006 勘误段已把「创建必然 404」改为悬空落库；答复首行未改。

r2 顶部处置：不预填；创建**不传**三主体及隐藏字段；不新选择器；不把 entity.binding.view 当创建前置；假证据省略；不做附件上传器；事实栏 404 作废。与本席 r1「B2-r2 正确口径」逐句对齐。

铁律 6 / GEN-017：2.2「删除」=删掉验收默认值与假主体/假证据，不是用新选择器替换。r2 走删除，不发明流程。

定稿补两句（不改队列、不构成新改写）：(1) 缺 `entity.binding.view` 时创建仍可进行（三列 null），与现路由一致；(2) 创建 POST 体**不含**三主体键本身（不只是不含假 id 值）；名称/业态校验保留。本页无创建绑定入口维持 OBS-03，B3 禁止顺手加。

票：**PASS**。r1 否决点闭合。

### 007 修订核实（添加／是；「与后台一致」已删）

2.2：`questions/M012-007.md` 答复首行「删除」。添加物是 `/^1[3-9]\d{9}$/`。本题「删除的代价」写明改为非空；操作员答的是该问。

核源：`saveMerchant:767` 对电话 `trimToNull`，不非空、无号段。r2 删「与后台一致」，定稿「前端改非空校验（引导流即时拦空串），后台仍可空、不改」——与本席 r1 同句。原表 007 旧处置作废。票：**PASS**。

### 008 修订核实（技术性／是；下钻已拿掉）

本席 r1：存档一条 + 删除申请或审批一条 + catch；绑定 POST 不另写；下钻超出铁律 6（首次打开无 focus），拿掉归 009。

r2 顶部同句。原表「下钻一条」「删除申请/审批各一条」旧处置作废。核源未变：archive/unarchive/delete-request/delete-approve `:212-249` 无 try/catch；组织测试 12 例无存档/删除；绑定测试对 POST `/change` 零覆盖；拓扑 `focusObjectId` 无 focus 时 effect 直接 return。票：**PASS**。

### 009 修订核实（观察／否；措辞已纠正）

本席 r1 不改队列，只纠正：OBS-06 已并入 005、OBS-18 已并入 004、OBS-13 写路径 org-1 并入 006（不传，不是选择器）；下钻无测试登记于此。r2 顶部同句。其余 OBS-01/02/03/04/05/07/08/09/10/11/12/14/15/16/17 维持否。无一条观察被改写入队。票：**PASS（否）**。

### 残留措辞（不构成新改写，不因此 FAIL）

- 2.2 答复段仍写 006「三选一必填…缺 entity.binding.view…无法创建」与 007「与后台一致」：这是 r1 历史记录，**不是**本批有效口径。
- 原表 006 事实栏仍写「创建必 404」与「选择真实…或不传」；007 仍写「与后台一致」；008 仍写「下钻一条」。用户指定这四行旧处置作废，以顶部 r2 为准。
- 原表 004 仍写「或读 `/space-types`」。顶部补充「不拉 /space-types」覆盖该分支；B3 只走页内 8 码表。
- 顶部 FAIL 叙事为说明 r1 否决原因而引用「三选一必填」，落在历史句，不在 006 新处置句。

B3 若按 2.2 段或原表 006 去加选择器/权限闸，或按原表 004 去拉 `/space-types`，或按原表 008 写下钻用例，即违反本席本票。

### 分类 / 队列 / 允许的最小修复边界

本批可定稿（仍须席甲本批 B2-r2 PASS 合取）。队列是=001–008；否=009。一次 B3-r1 只改本模块 6 文件，不改 types.ts、statusLabels.ts、M009 后台、三 HTML、锁、迁移、权限种子、路由权限码。可执行行为先写归属该 ID 的最小失败测试（GEN-003）：

- 001：夹具改真实 snake_case 后现页必红（全「-」/KPI 0）；再改页内读键见绿。types.ts 不动。
- 002：一条提交断言 body.action∈三态；缺 action 拦截。假证据/默认原因归 006，不在本条锁死。
- 003：真实 `project_id` 断言已挂接非 0。
- 004：业态/空间类型/表头中文；`not.toContain("bar")`；页内 8 码表（上列八码中文）+ unavailable→不可用；不改 statusLabels、不拉 /space-types。
- 005：夹具闭集 + 中文断言；负例保留。
- 006：不传三主体及假字段（POST 体不含三主体键）；原因空初始必填；创建仍可不持 entity.binding.view；名称/业态校验保留。绑定提交断言随 002 只一条，同时不含 `evidence-full-admin`。
- 007：正则→非空；后台不改。
- 008：存档一条（空理由拦截与有理由 POST 同条）+ 删除申请或审批一条 + catch。绑定 POST 不另写。下钻不写。

禁止：商户创建页三主体必选；缺 `entity.binding.view` 禁止创建；本页加创建绑定；改 types.ts/statusLabels.ts；为每个存档/删除 verb 各写一条；为下钻写用例；把 009 观察升格入队；按原表 004 拉 `/space-types`；按 2.2 段或原表 006 加选择器。

改动文件自身版本递增（`OrganizationMerchantPage.tsx`/`.test.tsx` 现 V0.01→V0.02，其余四文件无版本头的加 V0.01）。

### 本席实跑（B2-r2，只读；无写模式）

- 锁文件 hashlib / sha256sum：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（与 sidecar 内容、ledger 顶行、本文件既有文首独占行同值）
- 输入七文件字节与 SHA：上表全等
- 6 文件指纹：与 B1 表全等（拓扑 231/14547/`31e530e2…bae3c` 等六行）
- `HEAD`/`origin/main`/merge-base 三值 `bd445728a6ec7bb36a9e7ff19fe56927410ac61e`
- 点读：`requireBusinessEntity` `:559-561` 仍在绑定创建；`saveMerchant:763-765` 仍 `trimToNull`；`requireSubjectStatus` 缺省 active；`requireCooperationForm` 允许空；ADM-GOV-002 无 `entity.binding.view`；页内边界文案仍在；questions 006/007 答复首行「删除」
- 未跑：主仓测试、psql 写、部署、修复
- 未改 findings/ledger/锁/源/HTML/主索引

### 结论

候选按本席 B2-r1 意见修订：006 处置改为不传三主体、不加选择器、不把 entity.binding.view 当创建闸，事实栏 404 作废改悬空落库；004 补页内 8 码表与空间状态、不拉 /space-types；007 删「与后台一致」；008 去下钻；009 措辞纠正 OBS 并入。001/002/003/005 口径未改。全量差集无漏并、无新 ID、无新判据。r1 任一裁定均未被误读。r1 表/2.2 段残留单元格不改变已裁定的队列/分类。

本席队列：**001、002、003、004、005、006、007、008 是（8 项）；009 否（1 项）**。

本席 B2-r2：**PASS**

未经双 PASS 不进 B3，更不称模块 PASS。步骤仍 B2，M012 `IN_PROGRESS`，B3=0。不覆盖上方 B1 FAIL 与 B2-r1 FAIL 原文。
