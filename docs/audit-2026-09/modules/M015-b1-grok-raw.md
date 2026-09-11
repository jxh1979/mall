CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M015 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 修复前整模块全量审计
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 模块：M015 — 空间点位、楼层平面图与营业时段／后台实现／第1段
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 FILES 23 文件，计划冻结有效内容行 1999；当前 `git ls-files -z -- <23 paths> | xargs -0 cat | wc -l` 等价实算 **1999**（splitlines，与计划冻结一致）
- 冻结判据：锁映射行 `M015 → [...]` 实算 **90** 个唯一 ID（BEQR 22、BSM 14、GEN 18、PGM 36）；开工提示与 ledger 写「89 / PGM 35」是少计了 1 条 PGM。本席按锁映射 90 项全核，与用户清单逐字相同（MISSING=[] EXTRA=[] COUNT=90 unique=90）
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 修复队列的临时编号：GR-M015-001 … GR-M015-012
- 添加项（B1 只登记，不代用户回答 2.2；本批无）：无
- 只登记、建议队列「否」：观察见 §8
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做手工写操作（JDBC 集成测试由 Spring `@Transactional` 回滚）
- 本报告不表示全仓或发布门禁通过。M001–M014 及 U01–U18 已闭环，不重开。原 322 模块编号及旧结论不代表当前 471 模块完成。findings V1.90 仅作已登记边界背景。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令与 controller 优先于技能本体 |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；Arc `audit` 会派评审子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用** |
| grilling | `.../productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md` | 内部 sharpen |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 删除测试：删掉 floorplan 包则 PGM-023 平面图导入/发布做不到；删掉 VenueBusinessHours* 则场次开歇配置面消失。不派 DESIGN-IT-TWICE 子代理 |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（sha256/HTML 行命中/FieldLabels 键碰撞/mvn -Dtest）再下结论；不改生产代码 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流（HTML 原句→Controller/Service/Repository/Parser→V005/V023/V034/V226→测试/OpenAPI/前端只读） |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；测试 43 PASS ≠ 与 HTML 一致 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git、未直推、未安装 hook |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本步验证均秒级，未起 monitor |
| domain.md | `docs/agents/domain.md` | 词表/三 HTML 最终解释权；未改 CONTEXT.md |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail 落地（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：本机 mattpocock 技能树未找到独立技能
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，V0.17 明确不用）
- Arc `audit` 技能会派评审子代理并另写 `docs/arc/audits/`：与双席/§7 冲突，**不调用**
- superpowers `using-superpowers` 要求技能优先于探索：与「禁止子代理 / 本提示指定 controller」冲突处，按用户指令收窄
- Trail of Bits `audit-context-building` 会派 function-analyzer 子代理：与禁止子代理冲突，**不调用**

明确不用（controller 划掉）：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、code-review（mattpocock）、ponytail-audit/debt/gain、MCP、第三主体。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 同值
- `criteria-lock.md` 版本 V0.11；判据总数 401
- 映射行 `M015 → [...]`（L6394）与开工清单 90 ID 逐字相同
- `plan/modules.md` M015 FILES 23 条与用户清单逐字相同
- 23 文件字节/SHA256/行数与用户固定清单 **match=True**（见 §2）；合计 1999 行

HEAD：`main` = `origin/main` = `a041458d06051fed3811d562764cd5900bba6f72`（`rev-parse HEAD` / `origin/main` 两值相等）。本席未设 `GIT_INDEX_FILE`，未动主索引。

工作区：`git status --porcelain` = ` M docs/audit-2026-09/ledger.md`（M015 PENDING→IN_PROGRESS 的台账写入）。开工提示称「工作区干净」与此不完全一致；本 23 文件未改。本席只读，未再写 ledger。

ledger 只读核验（不改）：V3.29，588636 bytes，SHA256 `9da7e745ba1c6ccb1a4021b527d8768abe98b3789e0849f9bd4f47b410e49870`；M015 行 IN_PROGRESS、B3=0。findings V1.90，754909 bytes，SHA256 `48369705ee11705e0ae705ea005612c4bd2a28b63eac46874905f308e4d4114f`，仅作已登记边界背景（M009-013/018 projects 补列＝全局遗漏不在模块内加迁移；M013-004/M014-002 空间类型活校验＝V184 八码；M014-001 综合体项目是空间树归属根不是空间类型；业务地图 V0.14 / 治理地图 V0.12），不代替本模块审计。

controller `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21`；runbook V0.02 SHA256 `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`；modules.md V0.04 SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa`。

三份 HTML 只读、未改。锁内本模块关键 HTML 锚点对本席读取的现行 HTML **命中**（BEQR-063 `:111`、BEQR-170 `:259`、BSM-001 `:505`、BSM-028 `:698`、PGM-001 `:256`、PGM-005 `:347`、PGM-022 `:463`、PGM-023 `:467` / 平面图导入句 `:472` 均 hit=True）。BEQR-187「运营营业日」锁定义在 `:187`，**未映射本模块**，只作对照，不支撑 finding。

三 HTML 当前字节/SHA256（只读核验，本席未改）：

| 文件 | 字节 | SHA256 |
|---|---:|---|
| docs/business-enum-quick-reference-v0.1.html | 133961 | 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 |
| docs/business-system-map-for-operations-v0.1.html | 304482 | eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 |
| docs/platform-governance-map-for-operations-v0.1.html | 103015 | a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 |

三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。

## 2. 全文件分段覆盖表（全文已读；行=splitlines=`\n` 计数）

全部 23 文件 `endswith \\n` = True（last_byte=10）。用户固定清单逐文件 match。

| # | 路径 | 行 | 字节 | SHA256 | 全文 |
|---|---|---:|---:|---|---|
| 1 | backend/src/main/java/com/complex/platform/floorplan/FloorPlanController.java | 95 | 4323 | 33e14ba8868dd24c5235ba55658e51a38c7ff52572d3cb59fd10c07849941bef | 已读 L1–L95 |
| 2 | backend/src/main/java/com/complex/platform/floorplan/FloorPlanRecord.java | 21 | 503 | 9db3b6126ab0ca3e624e857684f68e680d7c99f9fe3e0ef6cf8a31080d373bce | 已读 L1–L21 |
| 3 | backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java | 47 | 1987 | af5b478aa351e2e77c60da6f481b938b7df5108f0311c3be15f714ce7551a186 | 已读 L1–L47 |
| 4 | backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java | 627 | 27664 | cd51ee50a1a0c8f54ea0f11b87779fccb033aa347f2493997b9af5c4c60a3e22 | 已读 L1–L627 |
| 5 | backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java | 446 | 21684 | f002ed04ccfc7b3b58f67f8cfd8e7abc5d8c87699dc17bebfd73a5933718f54a | 已读 L1–L446 |
| 6 | backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportCandidateRecord.java | 23 | 596 | 7bf4056cf8b920718b377272334b32f7d64c76f329078042f9e77bdedb065a33 | 已读 L1–L23 |
| 7 | backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportDetailsResponse.java | 14 | 458 | 5b0011b08d106da9a797ecd6d9915d665460c8823c03c50140b68354a11ae8d5 | 已读 L1–L14 |
| 8 | backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportErrorRecord.java | 14 | 295 | 6cfdb0787b4f3b6a4b785106e9f89f26b366d5b25a0fe326f9c9f6032b6b4e92 | 已读 L1–L14 |
| 9 | backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParseResult.java | 35 | 912 | 3315458ff08fb345a499a6fba09a296a671e92a6b07a717f8634ce0faab1666b | 已读 L1–L35 |
| 10 | backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java | 302 | 12287 | 96741bca52bb7f7bf739523331ea6a141554e3d73d1c7f89fd2a8fff95ef5796 | 已读 L1–L302 |
| 11 | backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportTaskRecord.java | 19 | 440 | 164d55ea7a81e8099c1ad8f44ef07bd3b3e474dfe7fb9f9d3f69893ac423a270 | 已读 L1–L19 |
| 12 | backend/src/main/java/com/complex/platform/floorplan/FloorPlanPoint.java | 7 | 112 | cc32d4e81cc6e58813c676f5ebfeeff86e2e48d4ea360ad2183843ffd661b6f5 | 已读 L1–L7 |
| 13 | backend/src/main/java/com/complex/platform/floorplan/FloorPlanShapeRecord.java | 22 | 546 | 4cfece0c8c5a9f13f2171b0d667eb29c109fe6cc3c9bfdf58590a575607e168f | 已读 L1–L22 |
| 14 | backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanCreateRequest.java | 16 | 383 | 0bc12e7e268a7d8904645c560027f48262a330412cada0f44b0d7da47bef8625 | 已读 L1–L16 |
| 15 | backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportCreateRequest.java | 10 | 240 | 7ecd9fca8fab9864815285d809a18375c6532c5036990599d11b12f3a99598a8 | 已读 L1–L10 |
| 16 | backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportPublishRequest.java | 13 | 348 | ae890f5f0877b2b3d120d626434b63a0158cffa87f75afa6fb503ef8f2359859 | 已读 L1–L13 |
| 17 | backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanShapeSaveRequest.java | 18 | 438 | b890d08f0bfb9e5e1675dc4d2f84a5b21e5f59427b39d59053653e545e07e20d | 已读 L1–L18 |
| 18 | backend/src/main/java/com/complex/platform/governance/ProjectRecord.java | 17 | 342 | be4cac0713c505a6e2734af8a0fee0392008b1ae6762e776a8047d8bb5ce95c5 | 已读 L1–L17 |
| 19 | backend/src/main/java/com/complex/platform/governance/SpaceRecord.java | 22 | 515 | f13f27eaf2dbdb754f18d4f841f1ebfd515d973e5863624583deab8adac7eac1 | 已读 L1–L22 |
| 20 | backend/src/main/java/com/complex/platform/governance/SpaceTypeRecord.java | 15 | 386 | 388191c191993bac3ae452374d001a2236f5245a41257b6b9e2c6e7f649b7bd1 | 已读 L1–L15 |
| 21 | backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRecord.java | 29 | 1244 | ec04b55b8b3c747b9623450bd7460e00b6a585b2d2d0f8485f80cfc59def6957 | 已读 L1–L29 |
| 22 | backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java | 90 | 4643 | a3ec4ab2ae792171ed1c16b3a155ce96f38ec827125ac7d823873cb75219aa3e | 已读 L1–L90 |
| 23 | backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java | 97 | 4756 | 9d42049d73771cd918dd5890249c6a73b43ff45313964d889bf9fcd65e2766af | 已读 L1–L97 |

合计 1999 行。未只读标题/搜索/截断。

数据流只读依赖（不审这些文件本身，但必须追到事实）：

- 迁移：`V005__p0_platform_master_data.sql`（projects/spaces）、`V023__p1_floor_plan_polygons.sql`、`V034__p2_floor_plan_import_tasks.sql`、`V052__floorplan_merchant_isolation.sql`、`V226__entitlement_session_kind_batch3.sql`（venue_business_hours）、`V251__permission_names_i18n_zh.sql`
- OpenAPI：`docs/openapi/openapi-p0.yaml` `/api/v1/floor-plans*`、`FloorPlanImportPublishRequest`
- 测试（M017，只跑不审文件）：`FloorPlanControllerTests` 12、`FloorPlanServiceTests` 18、`FloorPlanImportParserTests` 4、`FloorPlanJdbcIntegrationTests` 3、`VenueBusinessHoursServiceTests` 6
- 前端（M018，只读依赖不审）：`FloorPlanImportPage.tsx`（HighRiskConfirm）、`VenueBusinessHoursPage.tsx`（保存即刻生效）
- 出口翻译：`FieldLabels.java`（非本模块文件，但是本模块抛点的出网层）
- 控制器接线：`PlatformController` venue-business-hours（非本模块文件）

## 3. 三线审计（逐文件）

### 3.1 FloorPlanController.java（L1–L95）

- **第一性原理**：删除则 PGM-023 平面图导入/发布无 HTTP 入口。8 个端点：list/create 平面图、list/save 图形、list/create 导入、candidates、publish。写路径均 `requirePermission` + `requireIdempotencyKey`。
- **顺从式**：权限码 `floor_plan.view` / `manage` / `import.view` / `import.manage` 与 V023/V034 种子、OpenAPI `x-permission` 对齐。发布走独立 POST，符合「导入不直接写入正式区域」。
- **对抗式**：无 `@GuardedAction`（终端开关切面不拦本控制器；同包治理 PlatformController 同样无，记观察）。publish 请求体无确认字段，curl 可绕过前端 `HighRiskConfirm`（GR-001）。无项目 scope 参数。

### 3.2 FloorPlanRecord.java（L1–L21）

- **第一性原理**：mall_floor_plans 行映射。`merchantId` `@JsonIgnore` 防出网泄漏商户内键。
- **顺从式**：字段与 V023+V052 列一致（含 merchant_id）。status 闭集在 Service/CHECK：active/inactive/archived。
- **对抗式**：无负责人/租赁状态（本记录不是铺位）。`projectId` 可空 → GR-010。

### 3.3 FloorPlanRepository.java（L1–L47）

- **第一性原理**：仓储缝。`findAllFloorPlansByProject` 注释写明隔离闸口，默认空实现。
- **顺从式**：CRUD/导入任务/候选/错误接口齐全。
- **对抗式**：Service **从不调用** `findAllFloorPlansByProject`（GR-002）。默认空实现若被误用会返回空列表而非全量——当前唯一实现是 Jdbc 覆盖。

### 3.4 FloorPlanService.java（L1–L627）

- **第一性原理**：本模块行为中枢。删掉则导入解析、发布、幂等、审计、商户过滤全部消失。
- **顺从式**：写路径走 `IdempotencyService.execute`（生产注入 TransactionTemplate 后 action 多写+complete 原子，见 IdempotencyService L160–172）。导入上限 1_000_000 字符 / 2000 候选 / 1000 顶点。发布只接受 `parsed`+`pending`。形状跨平面图 shapeId 拒绝。审计成功/失败事件齐全。
- **对抗式**：见 GR-001/002/006/008/009/010/012。`listFloorPlans` 仅 `currentMerchantScope`，空则 `findAllFloorPlans()`，**不用** `currentProjectScopeQuietly`。`createFloorPlanOnce` 用 UUID，projectId 来自请求体未校验。`normalizeAllowed` 抛 `field is invalid`（shape_type/source_format 无 FieldLabels → 出网兜底「提交内容有误」）。`candidate_ids is required` 出网译成「招商对象未填写」。publish 循环写入形状，不检查既有布局、不要求确认；派生 id `fps_+slug(task+candidate)` 不覆盖旧行（「不可静默覆盖」在追加语义上成立，但二次确认不成立）。`PageResponse(records, 1, 50, n)` 伪分页。`operatorNote` 在 create/shape 请求中存在但不落平面图表。

### 3.5 JdbcFloorPlanRepository.java（L1–L446）

- **第一性原理**：SQL 适配器。删掉则无法持久化。
- **顺从式**：ON CONFLICT(id) 更新业务列；jsonb 读写失败抛 IllegalStateException（注释 AUD-020，避免被映射成 400）。`findAllFloorPlansByProject` SQL 谓词下推。
- **对抗式**：`saveFloorPlan` ON CONFLICT **不更新 merchant_id**（UUID 新建路径首轮不撞；记观察）。`saveShape` ON CONFLICT 可改 `floor_plan_id`（Service 层已拦跨图 shapeId）。无事务注解——依赖调用方 IdempotencyService 事务模板。

### 3.6–3.9 / 3.11–3.13 记录与 DTO 小文件

| 文件 | 第一性原理 | 顺从 | 对抗 |
|---|---|---|---|
| ImportCandidateRecord | 候选行 | 与 V034 列一致；polygon 拷贝 | 无租赁态 |
| ImportDetailsResponse | 复核聚合 | task+candidates+errors | 把英文 error_message 原样出网（GR-005） |
| ImportErrorRecord | 解析错误行 | 与 V034 一致 | error_message 无语言约束 |
| ImportParseResult | Parser 输出 | CandidateDraft/ErrorDraft | 默认 label `shape-N` 英文 |
| ImportTaskRecord | 导入任务 | 含 source_payload | payload 可含 SVG 全文 |
| FloorPlanPoint | 归一化顶点 | x/y double | 无 |
| FloorPlanShapeRecord | 正式区域 | 与 V023 CHECK 六类 shape / 六类 ref | 无铺位六态（BEQR-170 无实现面） |
| FloorPlanCreateRequest | 创建入参 | 含幂等键 | plan_name 键碰撞（GR-006）；无确认 |
| FloorPlanImportCreateRequest | 导入入参 | format/name/payload | 无 |
| FloorPlanImportPublishRequest | 发布入参 | 仅 key+note+ids | **无 confirmed 字段**（GR-001）；OpenAPI maxItems 200 本 DTO 不表达 |
| FloorPlanShapeSaveRequest | 存图入参 | 含 shapeId | operatorNote 不落库 |

### 3.10 FloorPlanImportParser.java（L1–L302）

- **第一性原理**：SVG/standard_json → 候选+错误。正则抽 polygon/polyline，不用 XML 解析器（避免 XXE，正面）。
- **顺从式**：viewBox 正宽高；折线须闭合；点归一到 [0,1]；JSON 要 shapes 数组。
- **对抗式**：全部 `errorMessage` 英文（GR-005）。不校验 SHAPE_TYPES——非法 `data-shape-type` 要到 Service `normalizeAllowed` 才炸，且任务可能已按 parsed 计数。默认 shop/custom。不解析 `<path>`。`readViewBox` 用整份 payload 扫属性，可能误读内嵌元素 viewBox。首轮正常 SVG 带根 viewBox 不炸。

### 3.18 ProjectRecord.java

- **第一性原理**：projects 行 DTO。PGM-020「记录项目状态、负责人、地址和经营范围」。
- **顺从式**：与 V005 projects 列一致（id/merchantId/venueType/name/code/floor/capacity/status/时间）。
- **对抗式**：缺负责人/地址/经营范围。**已登记 M009-013/018：补列属新建能力，不在本模块加迁移。本席不重开、不入新队列。** 见观察 OBS-01。

### 3.19 SpaceRecord.java

- **第一性原理**：spaces 行。PGM-023「记录空间面积、用途、状态、负责人和所属商户」。
- **顺从式**：area/spaceType/status/merchantId/projectId/polygon 有。
- **对抗式**：无负责人字段（GR-007）。空间类型活校验在 V184/其它模块（M013-004/M014-002 转入，不在本记录内加字典迁移）。

### 3.20 SpaceTypeRecord.java

- **第一性原理**：V184 字典一行。删除则后台可维护类型 DTO 消失。
- **顺从式**：typeCode/typeName/sortOrder/status。注释写明 createSpace 硬校验数据源。
- **对抗式**：本模块无 Service 写路径（无实现面，CRUD 在治理其它段）。

### 3.21–3.23 VenueBusinessHours*

- **第一性原理**：V226 酒吧场次开歇配置面。删除则运营只能改库，入场资格/当天无限按场次核销会停。**这不是 BEQR-187 营业日 10:00 切日**（营业日在 V201，未映射本模块）。
- **顺从式**：day_of_week 0–6；跨零点 close<open 放行；开歇同刻拒绝；disabled 行保留。审计 viewed/updated。唯一键 (venue_id, day_of_week) upsert。
- **对抗式**：类注释与 L21–24 写死「保存即生效、不做草稿/发布」；`is_draft` 复用为停用（GR-003，与 BSM-001 涉及钱配置须草案/转正/指定生效日矛盾，亦与 V226 注释「草稿态与生效锚点」矛盾）。`listAll`/`upsert` 无 project/merchant 谓词（GR-004）。`venue_id` 只非空不校验空间存在（GR-011）。不调用 IdempotencyService（控制器在模块外要求 key；前端页不发 key——M018 观察）。英文 `venue_id is required` 经 FieldLabels 译成「场地未填写」（此条出网中文正确）。

## 4. 90 条冻结判据核对

依据类型：1 HTML 原文 / 2 可执行产物 / 3 外部规范 / 4 可复现命令。无实现面必须写原因，不能把 HTML 沉默当冲突。

| ID | 本模块适用面 | 结论 |
|---|---|---|
| BEQR-063 | 酒吧反结/沽清等；平面图发布不在该闭集 | **无实现面**。发布高危走 PGM-023 |
| BEQR-074 | 隐私桌台 | **无实现面** |
| BEQR-143…BEQR-159 | 四类主体/状态/十一端/权限计算等 | **无实现面**（DTO 不实现这些状态机）。ProjectRecord 缺列不重开（OBS-01） |
| BEQR-166 | 合同审批链 | **无实现面** |
| BEQR-170 | 铺位六态须在平面图着色（PGM-026 句） | **无实现面**：ShapeRecord 无租赁态；联动属租赁/前端。不把沉默当冲突 |
| BEQR-189 | 管理费 | **无实现面** |
| BSM-001 | 时刻类策略；涉及钱须草案转正不即改即生效 | **冲突** GR-003 |
| BSM-003 | 通用能力 | 权限分岗有码；引导/删除在其它模块 |
| BSM-004 | 配置存档删除审批 | 仅有 archived 状态成员，无删除/审批 API。**无实现面**（不发明删除） |
| BSM-005 | 分步引导新建 | 后端无引导状态；前端 M018。**无实现面** |
| BSM-006 | 动态二维码 | **无实现面** |
| BSM-007 | 高危不自动给平台管理员 | 种子在 V034（M016）把 import.manage 授给 role_platform_admin。本模块只消费。OBS-08 XM |
| BSM-009 | 基础管理含空间 | 平面图/空间 DTO 承载部分 |
| BSM-028 | 建立楼层/区域/点位 | 部分：平面图+SpaceRecord；负责人缺 GR-007 |
| BSM-029 | 和谁有关 | **无实现面**（无票务/合同接线代码） |
| BSM-072 / BSM-074 | 商户范围内使用 | 商户 scope 过滤有；项目隔离无 GR-002 |
| BSM-114 | 入口隔离 | 商户过滤有；项目员工可看全量平面图 GR-002 |
| BSM-115 / BSM-120 | 子系统连接/已确认规则 | **无实现面**（无连接代码） |
| GEN-001 | 必要性 | 包有上位 HTML；findAllFloorPlansByProject 为隔离闸保留，不删 |
| GEN-002 | 可执行正确性 | 测试 43 PASS；行为与 HTML 仍有缺口，不凭测试通过放行 |
| GEN-003 | 测试有效性 | 现测未覆盖二次确认/项目隔离/中文解析错误/venue 归属。OBS-02 |
| GEN-004 | 信任边界 | GR-010/011/009 |
| GEN-005 | 授权最小权限 | GR-001/002/004。权限码存在且 controller 调用 |
| GEN-006 | 数据完整性 | 生产 IdempotencyService 事务模板覆盖导入/发布多写。venue upsert 单句原子。不另开 finding |
| GEN-007 | 幂等 | 平面图写路径有 execute；venue 靠唯一键 last-write-wins。OBS-03 |
| GEN-008 | 接口一致 | GR-006/009；前端 geojson/draft/booth 属 M018 OBS-04 |
| GEN-009 | 错误恢复 | 非法 shape_type 在落任务后才校验（解析器不校验闭集）。OBS-05 |
| GEN-010 | 安全 | Parser 非 XML；payload 上限有。无注入点在本模块 SQL（命名参数） |
| GEN-011 | 审计 | 平面图/导入/发布/营业时间均 recordSuccess/Failure。create/shape 的 operatorNote 不落库 OBS-06 |
| GEN-012 | 资源边界 | 导入上限有；列表伪分页 GR-008；publish ids 无上限 GR-009 |
| GEN-013 | 构建依赖 | 本模块无新依赖声明 |
| GEN-014 | 迁移 | 迁移文件属 M016；本席只追数据流。V023 CHECK 与 Java 闭集一致 |
| GEN-015 | 展示中文 | GR-005/006/012 |
| GEN-016 | 文档可验证 | OpenAPI 描述「人工确认」但 schema 无确认字段，与实现一致地**缺确认** |
| GEN-017 | 四分类 | 本席已依序分类 |
| GEN-018 | 证据 | 本报告 |
| PGM-001 | 治理底座/中文 | GR-005/006 |
| PGM-002…PGM-004, PGM-006…PGM-016 | 其它治理入口 | **无实现面**（本 23 文件不实现那些模块） |
| PGM-005 | 空间和点位入口 | 本模块正是该入口后台 |
| PGM-017…PGM-021 | 综合体项目档案 | ProjectRecord 缺列不重开 OBS-01 |
| PGM-022 | 空间发生在哪 | GR-007/010 |
| PGM-023 | 平面图导入；发布高危须二次确认；不可静默覆盖 | GR-001；追加写入不覆盖旧行（覆盖条款未破）。负责人 GR-007 |
| PGM-026 / PGM-027 | 商户/铺位着色 | BEQR-170 无实现面 |
| PGM-031 / PGM-035 | 经销/提成 | **无实现面** |
| PGM-040 / PGM-041 | 单一功能授权；数据按项目隔离；高危 | 权限码颗粒度够；项目隔离 GR-002；二次确认 GR-001 |
| PGM-042…PGM-044 | 职级/外包/桌台权限 | **无实现面** |
| PGM-048 | 商户侧授权范围 | 商户过滤有 |
| PGM-060 / PGM-065 / PGM-066 | 会员/连接/已拍板 | **无实现面** |

## 5. Findings（临时 ID；正式 ID 仅 B2 分配）

### GR-M015-001 — 发布无服务端二次确认

- **文件:行号**：`FloorPlanImportPublishRequest.java:5-12`；`FloorPlanService.java:349-435, 558-566`；`FloorPlanController.java:85-94`
- **完整原文（HTML）**：治理地图 PGM-023 L472：「商场平面图网页导入：……发布为高危动作，须二次确认——发布会写入正式平面图区域，不可静默覆盖既有布局证据。」
- **事实**：DTO/OpenAPI required 仅 `idempotency_key`+`candidate_ids`。Service 有幂等与 pending 校验，无 `confirmed`/二次确认令牌。前端 `HighRiskConfirm` 为 `window.confirm`（M018 只读），同一 POST 可被 curl 直接调用。
- **业务影响**：高危发布可被脚本/漏点 UI 一次打进正式平面图；「须二次确认」只停在界面。
- **可复现**：`FloorPlanControllerTests.publishImportUsesImportManagePermissionAndReturnsTask` 对无确认体返回 200。命令：对已 parsed 任务 POST `/api/v1/floor-plans/{id}/imports/{importId}/publish` 仅带幂等键与 candidate_ids 即发布。
- **冻结判据**：PGM-023、GEN-005；依据类型 **1+4**
- **四分类**：**细化**（HTML 已有「须二次确认」，实现缺服务端闸）
- **工程缺陷**：是。建议入队：是
- **最小处置**：发布请求强制业务确认字段（或等价服务端二次确认），缺省拒绝；不要只靠前端弹窗
- **无业务发明**：否（本条非技术性）

### GR-M015-002 — 平面图读/写未按项目隔离

- **文件:行号**：`FloorPlanService.java:65-80, 219-236`；`JdbcFloorPlanRepository.java:31-61`；`FloorPlanRepository.java:16-18`
- **完整原文**：BEQR-152「所有数据默认按项目隔离，普通岗位不得跨项目查看」；PGM-041 同句。OpenAPI `x-data-scope: project-space`。
- **事实**：`listFloorPlans` 只分商户 scope / 全量。`currentMerchantScope` 为空（项目员工、平台员）走 `findAllFloorPlans()`。`findAllFloorPlansByProject` 已实现且注释称隔离闸，**Service 零调用**。`createFloorPlan` 的 `projectId` 来自请求体。
- **业务影响**：项目岗位可列举他项目平面图；可把图建到他项目 id 下。
- **可复现**：对照 `AuthorizationService.currentProjectScopeQuietly`（L510–516：非平台管理员返回项目 ID）与 Service L65-69。现测 `listFloorPlansFiltersToMerchantScope` 只覆盖商户，`resultAdmin` 期望两商户全可见，**没有项目 scope 用例**。
- **冻结判据**：BEQR-152、PGM-041、GEN-005、GEN-008；依据类型 **1+4**
- **四分类**：**细化**
- **工程缺陷**：是。建议入队：是
- **最小处置**：列表/读取按 `currentProjectScopeQuietly` 调 `findAllFloorPlansByProject`；创建时项目 id 取 token 或校验归属，禁止客户端任意填写他项目

### GR-M015-003 — 场地营业时间「保存即生效」且把 is_draft 当作停用

- **文件:行号**：`VenueBusinessHoursService.java:21-24, 44-76`；`VenueBusinessHoursRepository.java:48-61`；`VenueBusinessHoursRecord.java:12-16`
- **完整原文**：BSM-001「所有影响钱和业务判断的策略参数——……时刻……」「所有涉及钱的配置一律版本化：……先存为草案、经高敏权限“转正”审批通过后按指定日期生效，不即改即生效」「无法判断是否涉及钱的，一律按涉及钱处理」。本表划场次，过场作废入场资格（服务自身注释 L17-19）。
- **事实**：注释写死不做草稿/发布；`upsert` 直接改 open/close/`is_draft`。V226 注释：「is_draft/effective_from 留草稿态与生效锚点（运营编辑先存草稿再发布）」。Service 把 `is_draft` 映射为 disabled=停用。
- **业务影响**：改开歇立刻影响当晚核销；无法按生效日转正；草稿列被占用后无法表达「未发布草案」。
- **可复现**：`VenueBusinessHoursServiceTests.savesNormalBusinessWindow` 一次 save 即落库。前端文案「保存即刻生效，不设草稿」（只读依赖）。
- **冻结判据**：BSM-001；依据类型 **1+2**
- **四分类**：**冲突**（与「不即改即生效 / 草案转正」明文相反）
- **工程缺陷**：是。建议入队：是
- **最小处置**：删除即改即生效口径；`is_draft` 回到草案/发布；生效日必填；停用若仍需要，另列，不占用草稿列
- **无业务发明**：冲突优先于技术性

### GR-M015-004 — 营业时间全量读写无项目/商户谓词、无场地归属校验

- **文件:行号**：`VenueBusinessHoursService.java:36-76`；`VenueBusinessHoursRepository.java:29-46`
- **完整原文**：同 BEQR-152 / PGM-041 项目隔离。
- **事实**：`listAll` `ORDER BY venue_id, day_of_week` 无 WHERE。`upsert` 接受任意 venue_id。模块外 `PlatformController` 只 `requirePlatformScope()`——无 merchantId 的项目员工会放行（`requirePlatformScope` L497-502 只拦商户账号）。
- **业务影响**：项目员工可读写全部场地营业时间。
- **可复现**：读 Service.list/save 与 Repository SQL；对照 AuthorizationService.requirePlatformScope。
- **冻结判据**：BEQR-152、PGM-041、GEN-005；依据类型 **1+4**
- **四分类**：**细化**
- **工程缺陷**：是。建议入队：是
- **最小处置**：按项目（及场地所属空间/商户）过滤；无归属拒绝

### GR-M015-005 — 导入解析错误英文入库并出网

- **文件:行号**：`FloorPlanImportParser.java:36,44,56,67,74,79,89,106,121,127,239-244`
- **完整原文**：铁律/PGM-001/GEN-015 展示文字中文。Parser 错误进入 `floor_plan_import_errors.error_message`，经 `FloorPlanImportDetailsResponse` 原样返回，**不经** `GlobalExceptionHandler`/`FieldLabels`。
- **事实**：如 `"source_payload is required"`、`"svg viewBox with positive width and height is required"`、`"polygon requires at least 3 points"`。Jdbc 测试把英文当合法落库（M017 只读）。
- **业务影响**：复核页「说明」列对运营展示英文。
- **可复现**：`FloorPlanImportParserTests.rejectsUnclosedSvgPolylineAsImportError`；`parser.parse("svg","")` 得英文。本席已跑该类 4 PASS。
- **冻结判据**：GEN-015、PGM-001；依据类型 **1+2**
- **四分类**：**细化**
- **工程缺陷**：是。建议入队：是
- **最小处置**：error_message 改为中文业务句；error_code 可保留机读码

### GR-M015-006 — 校验键与 FieldLabels 碰撞，出网错译

- **文件:行号**：`FloorPlanService.java:365,498,564`；`FloorPlanCreateRequest.java:9`
- **完整原文**：GEN-015/PGM-001。出网层 `FieldLabels.translate`：`candidate_ids`→「招商对象」，`plan_name`→「方案名称」（FieldLabels.java:279,413，非本模块文件，但是本模块抛点的出口）。
- **事实**：空 candidate_ids → 用户看到「招商对象未填写」。缺 plan_name →「方案名称未填写」。`shape_type is invalid` 无标签 → 兜底「提交内容有误」。逻辑按 FieldLabelsTests 算法复算：`candidate_ids is required` => `招商对象未填写`。
- **业务影响**：发布/创建失败提示指到错误业务对象。
- **可复现**：CMD 见 §9 键碰撞复算；对照 FieldLabelsTests.translateRequiredAndInvalidToChinese。
- **冻结判据**：GEN-015、GEN-008、PGM-001；依据类型 **1+4**
- **四分类**：**细化**
- **工程缺陷**：是。建议入队：是
- **最小处置**：本模块抛中文整句（同文件已有「只有解析完成的导入任务才能发布」），或换不碰撞的字段键并在 FieldLabels 补平面图义（后一动作跨模块，B2 归并）

### GR-M015-007 — SpaceRecord 无「负责人」

- **文件:行号**：`SpaceRecord.java:8-21`；对照 V005 spaces L55-68（迁移属 M016）
- **完整原文**：PGM-023「记录空间面积、用途、状态、负责人和所属商户。」
- **事实**：记录有 area/spaceType/status/merchantId，无负责人。与 M009-013 **不是同一条**（那条是 projects 负责人/地址/经营范围）。
- **业务影响**：空间对象无法记录责任人，班次/设备「责任区域」缺锚点。
- **可复现**：读 SpaceRecord 与 V005 CREATE TABLE spaces。
- **冻结判据**：PGM-023、PGM-022；依据类型 **1+4**
- **四分类**：**细化**
- **工程缺陷**：是。建议入队：是（写路径/迁移 XM 到空间 CRUD/M016，本模块至少记录类与读模型应对齐；B2 定是否拆 XM）
- **最小处置**：补负责人字段并打通校验；禁止在本模块发明新空间类型枚举（活校验仍是 V184）

### GR-M015-008 — 列表伪分页、一次返回全表

- **文件:行号**：`FloorPlanService.java:65-80, 82-96, 128-141`
- **事实**：`new PageResponse<>(records, 1, 50, records.size())` 而 `records` 是 findAll* 全量。
- **业务影响**：平面图/图形/导入任务多时一次加载无界。
- **可复现**：读 L79；ControllerTests 只断言 total，不断言截断。
- **冻结判据**：GEN-012；依据类型 **4**
- **四分类**：**技术性**（分页是实现机制）
- **无业务发明理由**：未引入 HTML 没有的业务实体/状态/权限；只是 PageResponse 契约与实现不符
- **工程缺陷**：是。建议入队：是
- **最小处置**：真实 LIMIT/OFFSET 或明确不分页并去掉虚假 size=50

### GR-M015-009 — 发布 candidate_ids 无数量上限

- **文件:行号**：`FloorPlanService.java:359-366, 558-566`；对照 OpenAPI `FloorPlanImportPublishRequest.candidate_ids` maxItems 200
- **事实**：Java 只拒绝空列表；导入解析有 2000 上限，发布可一次写入 2000 个形状。
- **业务影响**：一次发布放大写放大；与契约 200 不一致。
- **可复现**：读 validateImportPublishRequest；OpenAPI yaml L32993-32995。
- **冻结判据**：GEN-004、GEN-012、GEN-008；依据类型 **4**
- **四分类**：**技术性**
- **无业务发明理由**：上限是资源闸，不是新业务规则
- **工程缺陷**：是。建议入队：是
- **最小处置**：服务端强制与契约一致的上限

### GR-M015-010 — 创建平面图 project_id 可空且不校验

- **文件:行号**：`FloorPlanService.java:222-225, 492-504`；`FloorPlanCreateRequest.java:6`；V023 `project_id VARCHAR(64)` 可空
- **完整原文**：BSM-028「建立综合体项目、楼层、区域、点位」；PGM-022 空间归属项目。
- **事实**：`trimToNull(request.projectId())`，validateCreateRequest 不要求 project_id，不查 projects 表。
- **业务影响**：无归属根的平面图，隔离谓词无法挂项目。
- **可复现**：createRequest 测试带 prj_1 但不校验存在；validateCreateRequest 无 project 检查。
- **冻结判据**：PGM-022、BSM-028、GEN-004；依据类型 **1+4**
- **四分类**：**细化**
- **工程缺陷**：是。建议入队：是
- **最小处置**：必填且必须是调用方可见的已有项目

### GR-M015-011 — venue_id 不校验场地存在

- **文件:行号**：`VenueBusinessHoursService.java:48-66`
- **事实**：非空白即 upsert。可写入不存在的 venue_id。
- **业务影响**：幽灵场地配置；核销侧按 id 划场次会配到空对象。
- **可复现**：`VenueBusinessHoursServiceTests.savesNormalBusinessWindow` 使用 `"sp_bar_main"` 无存在性断言。
- **冻结判据**：GEN-004；依据类型 **4**
- **四分类**：**细化**（场次必须挂真实空间/酒吧）
- **工程缺陷**：是。建议入队：是
- **最小处置**：保存前校验 venue 为现存空间（或酒吧点位），否则中文 404

### GR-M015-012 — 已中文的校验句夹带英文字段名

- **文件:行号**：`FloorPlanService.java:517, 535`
- **事实**：`FieldLabels.translate` 对已含汉字的句子原样透传，故「多边形顶点（polygon_points）至少需要 3 个点」「导入内容（source_payload）超过 1000000 字符上限」整句出网含英文。
- **业务影响**：后台/设备端展示英文标识符，违反「展示文字全中文」（单据号等四类例外不含字段名）。
- **可复现**：键碰撞脚本对夹汉句子保持原文。
- **冻结判据**：GEN-015、PGM-001；依据类型 **1+4**
- **四分类**：**细化**
- **工程缺陷**：是。建议入队：是
- **最小处置**：去掉括号内英文字段名，或改纯中文

## 6. 添加项

无。`disabled`/停用视为「该日不营业」的细化，不单列 2.2。`risk_zone`/`route` 等 shape_type 视为点位细化，不升添加。

## 7. 对抗式追问树（grilling 自问，不派子代理）

1. 发布能否不经二次确认？能。GR-001。
2. 发布会不会静默 UPDATE 已有 shape id？导入派生新 id，追加不覆盖。覆盖条款未破。
3. 项目员工是否只看本项目平面图？否。GR-002。
4. 营业时间是否即改即生效？是。GR-003。
5. 解析错误运营能否读懂？英文。GR-005。
6. 空 candidate_ids 提示什么？「招商对象未填写」。GR-006。
7. 空间能否记负责人？不能。GR-007。
8. 并发双 POST 导入？IdempotencyService+生产事务模板覆盖。不另开。
9. slug 碰撞（`abc!` 与 `abc@`）？须刻意选键，非首轮正常。OBS-07。
10. 铺位六态着色？本 23 文件无字段。无实现面。

## 8. 观察（不升 finding / 不入队）

- **OBS-01**：ProjectRecord 仍无负责人/地址/经营范围。对齐已闭环 M009-013/018，不重开、不加迁移。
- **OBS-02**：M017 测试 43 PASS 未覆盖 GR-001/002/004/005；Jdbc 测试把英文 error_message 当合法。GEN-003：测试通过不替代行为证据。
- **OBS-03**：VenueBusinessHours 不走 IdempotencyService，靠 UNIQUE(venue_id,day_of_week) 覆盖。
- **OBS-04**：前端默认 `sourceFormat=geojson`、创建 `status=draft`、SHAPE 含 booth/public_area，后端闭集为 svg/standard_json 与 active/inactive/archived 与 shop/area/…。属 M018 契约，本席不审前端。
- **OBS-05**：Parser 不校验 shape/ref 闭集；非法类型在 save 候选时才失败。
- **OBS-06**：create/shape 的 operatorNote 不落 mall_floor_plans / floor_plan_shapes。
- **OBS-07**：slug 对非 `[a-z0-9_-]` 压成 `_`，极端键碰撞才发生，按铁律 6 不修。
- **OBS-08**：V034 把 high+requires_approval 的 `floor_plan.import.manage` 授给 `role_platform_admin`。种子在 M016。
- **OBS-09**：BEQR-187 营业日切日 ≠ V226 场次开歇。本模块实现后者。V201 不在本 FILES。
- **OBS-10**：M014-001 项目非空间类型：本模块 SpaceTypeRecord 与 ProjectRecord 分离，未把项目写入空间类型闭集。
- **OBS-11**：ponytail：`findAllFloorPlansByProject` 默认空实现是隔离闸保留，注释禁止删除。Lean 以外无强制可删。
- **OBS-12**：saveFloorPlan ON CONFLICT 不更新 merchant_id；UUID 新建首轮不触发。
- **OBS-13**：工作区 `ledger.md` 已改 IN_PROGRESS，与「干净」不完全一致；23 业务文件干净。

## 9. 命令证据

```
# HEAD
git rev-parse HEAD
# a041458d06051fed3811d562764cd5900bba6f72
git rev-parse origin/main
# a041458d06051fed3811d562764cd5900bba6f72
git status --porcelain
#  M docs/audit-2026-09/ledger.md

# 锁
sha256sum docs/audit-2026-09/plan/criteria-lock.md
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# 23 文件 SHA/行：python 实算 MISMATCH=0 TOTAL_LINES=1999

# 测试（只 export 三个变量；URL 强制 complex_test，未 source 整个 env）
export COMPLEX_DATABASE_URL='jdbc:postgresql://127.0.0.1:5432/complex_test'
# USER/PASSWORD 从 /etc/complex/complex.env 单行 grep，未 source
cd backend && mvn -o -q -Dtest=com.complex.platform.floorplan.FloorPlanServiceTests,com.complex.platform.floorplan.FloorPlanImportParserTests,com.complex.platform.floorplan.FloorPlanControllerTests,com.complex.platform.governance.VenueBusinessHoursServiceTests test
# UNIT_EXIT=0
# FloorPlanServiceTests 18 / ImportParserTests 4 / ControllerTests 12 / VenueBusinessHoursServiceTests 6
mvn -o -q -Dtest=com.complex.platform.floorplan.FloorPlanJdbcIntegrationTests,com.complex.platform.common.api.FieldLabelsTests test
# JDBC_FIELDLABELS_EXIT=0
# FloorPlanJdbcIntegrationTests 3（Flyway 390 迁移，schema public:422，No migration necessary，连到 complex_test）
# FieldLabelsTests 7
# 合计本席实跑 43+7，failures=0 errors=0
```

Flyway 日志：`Database: jdbc:postgresql://127.0.0.1:5432/complex_test`。未对库做手工写。

键碰撞复算（按 FieldLabels.translate 算法，非改码）：

```
candidate_ids is required => 招商对象未填写
plan_name is required => 方案名称未填写
shape_type is invalid => 提交内容有误，请检查后重新提交
多边形顶点（polygon_points）至少需要 3 个点 => （原样，含英文）
```

HTML 命中：PGM-023 L472 平面图导入句 hit=True；BEQR-170 L259 hit=True；BSM-001 L505 hit=True。

## 10. 结论

**FAIL**。

12 条工程缺陷建议入队（GR-M015-001…012）。添加项 0。无未答 questions。无 HTML 人闸。23 文件与 90 判据已全覆盖。测试 43 项现测绿不能覆盖上述缺陷。本席 FAIL 具否决权，不得以自审 PASS 覆盖。

## M015 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 43052 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，独立）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M015 FILES 23 / 锁映射 90 / 计划冻结 1999 行）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `a041458d06051fed3811d562764cd5900bba6f72`（PR #435 M014 闭环检查点已合并）。本模块 23 文件指纹与 B1 覆盖表全等。B3=0。无源修复。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B1 分类。
- 本席 B2-r1：**FAIL**（007 最小处置 `PageResponse.of(records,1,500)` 会截断 `listShapes`，既非本席 GR-008「真实 LIMIT/OFFSET」也非「明确不分页」。其余 001–006、008–014 逐 ID PASS。添加 0、不发 2.2）
- 正文实算锁：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。映射行 `M015 → [...]`（`criteria-lock.md` L6394）COUNT=90 unique=90（BEQR 22、BSM 14、GEN 18、PGM 36）；席甲 B1 写 89/PGM 35 为少计，候选已勘误，本席 B1 实算 90。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要复现只读。

此票只判断 findings.md V1.91 顶部「当前 M015 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M015-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 43052 | `649f4b190d5d598b99e76d91b91a62aee7e861e22444d0b4e3ec2b433c41ca98` |
| `docs/audit-2026-09/modules/M015-b1-self-raw.md` | V0.01 | 31125 | `0f51db18a789833b1c056e7f0fbcb94016169cf65372ec2dc1078d21c9417f34` |
| `docs/audit-2026-09/findings.md` | V1.91 | 766855 | `03bbb2096a56f46e1bb6dfdf1f338d5ebe01601545175c6b65cb50db21808088` |

ledger 只读：V3.30，589704 bytes，SHA256 `3cdc2944dbd9372f38b5b94ab5fe5ee8965cb365251bc7667e6697e747cb4e7d`；M015 IN_PROGRESS、B3=0。23 文件 `git ls-files` 合计 **1999** 行，与计划冻结一致。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

三 HTML 只读核验（本席未改；与 B1 同）：

| 文件 | 现行 | SHA256 |
|---|---|---|
| 业务地图 | V0.14 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| 治理地图 | V0.12 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |
| 枚举速查 | V0.07 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |

工作区只读：`git status --porcelain` = findings.md / ledger.md 已改（席甲落候选）、本两份 raw 未跟踪。本席只追加本指定路径。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核源/HTML/命令再接受或反驳；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：001 技术性还是发布覆盖冲突、003 是否 GR-002 原意、005/006 不改 FieldLabels 是否够、007 of(1,500) 会否截断、009 venue＝spaces.id、010 单独改服务会否断现网、011 单独加字段会否 400、012/013 否是否 M009-013 口径、014 是否写错观察 |
| verification-before-completion | 声称前先跑只读哈希/行号/源码核对 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码 |
| codebase-design | 删除测试：本模块深度落在平面图导入/发布与场次开歇配置面，不落营业日切日或铺位六态 |
| ponytail-review | 只评最小删除/收窄，不落地。001 主键加域比改 upsert 语义更懒；007 诚实不分页比硬编码 500 更懒；010 本批不改服务比单改草案更懒 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到**；未冒充调用 |

### 完整差集（GR-001…012、OBS-01…13、席甲 CX-001…005、OBS-1…6）

漏并：无。两席 B1 全部临时 ID 仍落在 001–014。本席 GR-001…012 原意：007 的候选最小处置偏离「真实分页或明确不分页」两支（见下），分类/队列未改写。席甲独有 CX-001 本席 B1 未登记，本票独立核源后归 001，不是照抄席甲分类。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M015-001 | CX-M015-001（本席 B1 未发现） | 技术性/是/是 | **PASS（技术性/是/是；主键加入平面图域最懒）** |
| M015-002 | GR-M015-010 + CX-M015-002 | 细化/是/是 | **PASS（细化/是/是；与 003 合取）** |
| M015-003 | GR-M015-002 | 细化/是/是 | **PASS（细化/是/是；与 GR-002 原意一致）** |
| M015-004 | GR-M015-005 + CX-M015-003（解析器） | 细化/是/是 | **PASS（细化/是/是）** |
| M015-005 | GR-M015-006 | 细化/是/是 | **PASS（细化/是/是；本模块抛中文整句足够）** |
| M015-006 | GR-M015-012 | 细化/是/是 | **PASS（细化/是/是；不改 FieldLabels 足够）** |
| M015-007 | GR-M015-008 | 技术性/是/是 | **FAIL（技术性/是/是成立；of(records,1,500) 截断 listShapes，不是最懒可行）** |
| M015-008 | GR-M015-009 | 技术性/是/是 | **PASS（技术性/是/是；与契约 200 一致）** |
| M015-009 | GR-M015-004 + GR-M015-011 | 细化/是/是 | **PASS（细化/是/是；venue＝spaces.id 核源成立）** |
| M015-010 | GR-M015-003 | 冲突/是/否（XM） | **PASS（冲突/是/否；单独改服务会断现网。非阻断：XM 模块号见下）** |
| M015-011 | GR-M015-001 | 细化/是/否（XM） | **PASS（细化/是/否；OpenAPI additionalProperties:false，单独加必填字段现网 400）** |
| M015-012 | GR-M015-007 + CX-M015-005 | 细化（遗漏，全局）/是/否 | **PASS（否合规；与 M009-013/018 补列口径同构）** |
| M015-013 | CX-M015-004 + OBS-04/05 | 细化（遗漏，跨模块）/是/否 | **PASS（否合规；CHECK 不在本模块 23 文件）** |
| M015-014 | 双方观察 + 判据 89→90 勘误 | 技术性/正向 /否/否 | **PASS（否；未把应升项留观察）** |

冲突 1（010，否/XM）。细化入队 6（002/003/004/005/006/009）。技术性入队 3（001/007/008，其中 007 最小处置否决）。细化否 3（011/012/013）。观察否 1（014）。添加 0。

### 001 独立裁定（技术性／是；席甲独有，核源成立）

本席 B1 未登记（当时只核了发布路径「派生新 id、追加不覆盖」，未核 saveShape/导入任务把客户端键当全局主键）。

核源（只读）：

- `FloorPlanService:259` 无 `shapeId` 时 `"fps_"+slug(idempotencyKey)`；`:287` `"fpi_"+slug(idempotencyKey)`。
- 幂等操作键已按图分域：`:114` `"saveFloorPlanShape:"+id`、`:168` `"createFloorPlanImport:"+id`。
- `JdbcFloorPlanRepository:131-140` 图形 `ON CONFLICT (id)` **改写** `floor_plan_id`；`:156-165` 任务 upsert **不改写** `floor_plan_id`，但改 payload/状态/计数。
- 候选/错误 id 随 `taskId`：`:307` `fpic_+slug(taskId+"_"+n)`。任务被跨图撞主键后，A 图任务行仍挂 A，内容被 B 覆写。
- 现测 `createFloorPlanWithSameKeyAcrossMerchantsDoesNotOverwrite` 只覆盖平面图创建（UUID）；`saveShapeRejectsShapeIdBelongingToDifferentFloorPlan` 只拦**显式** shapeId，不拦同键派生。
- 前端导入键是 `fpi-${Date.now()}`，不消掉 API 层「两位操作员用 shape-1」这条首轮路径。

四分类：**技术性**。ID 组成与 upsert 列是实现机制。PGM-023 L472「不可静默覆盖既有布局证据」说的是**发布写入正式区域**，不是本条主键域；本席 B1 已记发布追加不覆盖旧行。支撑本条的是 GEN-006/007/002。候选写 PGM-022 该句——该句在 PGM-023 子锚，不在 PGM-022 原文。非阻断：r2 判据列改 PGM-023 或删该句，留下 GEN-006/007。不升冲突。

无业务发明：不增减空间类型、不改发布规则。

最小处置「主键加入平面图域」**最懒**：一行改 slug 输入，候选/错误随任务 id，与已按图分域的操作键对齐，并保留确定性 id 作为幂等缓存失效后的第二层。比「冲突时拒写」更干净（任务路径仍会覆写 payload）。UUID（与 createFloorPlan 同）也一行，但丢掉第二层幂等；不否决候选支。不改 upsert 去改写 `floor_plan_id` 代替改主键——任务路径仍坏。

票：**PASS**。

### 002 独立裁定（细化／是）

核源：`:224` `trimToNull(request.projectId())`；`validateCreateRequest:492-504` 不要 project_id、不查 `projects`。PGM-017/022、BSM-028、M014-001：项目是归属根。`FloorPlanRepository` 无 `projectExists`（B3 加一条 SELECT）。列级 NOT NULL 在 V023＝M016，XM 登记正确。

与 003 合取「须在调用方项目 scope 内」＝本席 GR-010「必须是调用方可见的已有项目」。无业务发明。

票：**PASS**。

### 003 独立裁定（细化／是；与 GR-002 原意一致）

核源：`listFloorPlans:65-69` 只 `currentMerchantScope`，空则 `findAllFloorPlans()`；`findAllFloorPlansByProject` SQL 已实现、Service 零调用；`ensureFloorPlanExists:568-572` 只 `checkMerchantOwnership`。`AuthorizationService.currentProjectScopeQuietly:510-515` 已有项目通道，ServiceOrder/Finance 等已用。BEQR-152 L235「所有数据默认按项目隔离」。

候选：商户 scope 优先 → 项目 scope → 平台管理员全量；`ensureFloorPlanExists` 加项目归属（`CROSS_PROJECT_FORBIDDEN`）；创建时项目 scope 存在则 `project_id` 必须等于 scope。这是 GR-002「列表/读取按 quietly 调 ByProject；创建取 token 或校验归属」的落地，走「校验归属」支，不发明新权限码。商户优先避免项目员工通道误伤商户过滤。无业务发明。

非阻断：`currentProjectScopeQuietly` 注释写永不抛，实现会对无 `organizationId` 走 `requireProjectScope` 抛错（M006 回归已断言）；B3 须商户 scope 优先、平台管理员桥接 empty，与现有调用方同一用法，不要把无组织全局员从「现网全量」改成 403。

票：**PASS**。

### 004 独立裁定（细化／是）

核源：Parser 英文 `error_message` 落 `floor_plan_import_errors`，详情原样出网，不经 FieldLabels。PGM-001/GEN-015。error_code 保留。夹具随改。与 GR-005 一致。

票：**PASS**。

### 005 / 006 独立裁定（细化／是；本模块抛中文整句足够）

核源：`FieldLabels:279` `candidate_ids`→「招商对象」、`:413` `plan_name`→「方案名称」；`:365/:564` `"candidate_ids is required"` 出网「招商对象未填写」；`:498` `requireText(...,"plan_name")`→「方案名称未填写」；`:601` `field+" is invalid"` 无标签→兜底「提交内容有误」。`:517/:535` 已含汉字，`translate` 原样透传，括号英文出网。`VenueBusinessHoursService:50` `"venue_id is required"`（标签「场地」已能译成「场地未填写」）；`:81/:86` `open_time`/`close_time` 打头夹英文键。

GR-006 原最小处置第一支：本模块抛中文整句（同文件已有「只有解析完成的导入任务才能发布」），或换键并改 FieldLabels（跨模块，B2 归并）。候选取第一支、**不改 FieldLabels**（非本模块）＝原意且更懒。中文整句走 `translate` 的「已含汉字原样透传」，碰撞消失。006 去掉括号英文／营业时间纯中文＝GR-012，并收 CX-003 服务侧英文残留。无业务发明（校验条件不变）。

票：005 **PASS**；006 **PASS**。

### 007 独立裁定（技术性／是；最小处置截断 → 本 ID FAIL）

核源：`FloorPlanService:79/95/141` `new PageResponse<>(records,1,50,records.size())`，`records` 为 `findAll*` 全量。Controller 三端点均无 page/size 参数。`PageResponse.of(all,page,size)`（`:16-21`）对全量做内存切片，`total` 为真实大小；注释写明「生产 Jdbc 各自走 SQL COUNT+LIMIT/OFFSET，不经此方法」。`PlatformController:176` 场地营业时间用 `of(...,1,500)` 的前提是仓储注释「量级＝场地数×7，很小」。本模块 `MAX_IMPORT_CANDIDATES=2000`，`listShapes` 可达 2000 行；`of(records,1,500)` 在 `n>500` 时只返回前 500，且没有第 2 页参数，多出的图形对客户端不可达。现网构造函数虽谎报 pageSize=50，但 **items 仍是全量**；改成 of(1,500) 是丢数据，不是修谎报。

四分类技术性成立。无业务发明。队列「是」成立。

GR-008 原最小处置只有两支：「真实 LIMIT/OFFSET」或「明确不分页并去掉虚假 size=50」。候选第三支硬编码 500、不新增分页参数，对 `listFloorPlans`/`listImportTasks` 碰巧够用，对 `listShapes` **不可行**。最懒可行＝三处诚实不分页，例如 `new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size())` 或 `PageResponse.of(records, 1, Math.max(records.size(), 1))`，仍不新增分页参数。不要抄营业时间的 500。

票：**FAIL**。r2 须改 007 最小处置为诚实不分页（或真分页且 Controller 增加 page/size——后者不是更懒）。

### 008 独立裁定（技术性／是）

核源：`validateImportPublishRequest:558-566` 只拒空列表；OpenAPI `candidate_ids` maxItems 200。服务端 `size()>200` → VALIDATION_FAILED 中文。上限是资源闸。无业务发明。

票：**PASS**。

### 009 独立裁定（细化／是；与 GR-004/011 原意一致）

核源：`VenueBusinessHoursService:48-66` 非空即 upsert，无存在性、无项目谓词；`listAll` 无 WHERE。`PlatformController:172-185` 只 `requirePlatformScope()`（拦商户，放行项目员工）。前端 `VenueBusinessHoursPage:60-62` 场地下拉来自 `/api/v1/platform/spaces`，选项 `space.id`；测试夹具 `"sp_bar_main"`。venue＝`spaces.id` 成立。

候选：`findVenueProjectId`（`SELECT project_id FROM spaces WHERE id=:id`）→ 不存在 `VENUE_NOT_FOUND` 中文 404；列表 JOIN spaces 按项目 scope；保存不在 scope 内 `CROSS_PROJECT_FORBIDDEN`；服务注入 `AuthorizationService`。这是 GR-011「保存前校验现存空间」+ GR-004「按项目过滤、无归属拒绝」的合取，用已有 quietly 通道，不发明「场地」新实体。构造变更落在本模块 Service/测试（M017 测试类随 B3，候选已写）。无业务发明。

票：**PASS**。

### 010 独立裁定（冲突／是／否 XM；不改写冲突原意）

核源：

- 服务注释与实现「保存即生效、不做草稿/发布」；`is_draft` 复用为停用（`:21-24/:62-68`）。
- V226 注释：`is_draft/effective_from` 留草稿态与生效锚点；表 `UNIQUE(venue_id, day_of_week)`，草案与生效**不能两行并存**。
- BSM-001 L521：涉及钱的配置先草案、高敏转正、按指定日期生效，不即改即生效；无法判断是否涉及钱一律按涉及钱；时刻属策略参数。本表划场次，过场作废入场资格。
- 核销 `JdbcTicketingRepository.findBusinessHours:1168-1175` 只 `is_draft = FALSE`，**不读** `effective_from`。
- 转正 HTTP 在 `PlatformController`（M009 FILES，非本模块）；前端 `VenueBusinessHoursPage`（M018）文案/行为保存即刻生效；V226 文件在 **M247**（不是 M016）。
- FinanceService 草案+起草人+差人转正是结算配置中心范式，V226 **无起草人列**。本席 GR-003 最小处置是「is_draft 回到草案/发布；生效日必填；停用另列」，**没有**要求起草人列。

四分类：**冲突**（与 GR-003 相同）。工程缺陷是。无业务发明。

队列：本席 B1 写「建议入队：是」指缺陷要修，不是授权本模块 B3 单独改服务。B2 归属：本模块 23 文件不够一次不破坏现网的闭合。若只把 save 改成 `is_draft=TRUE`：唯一键覆盖生效行，核销侧当停用，现网配置面即断，且无转正入口，草案永不能生效。故 **冲突/否（XM 同批）**，不是改写成「冲突不成立」。

最懒且不破坏现网：本模块本批**不改** `VenueBusinessHoursService` 行为。XM 同批（不是本 B3）：本模块服务去掉「保存即生效」、`is_draft` 还回草案（停用另列）、生效日必填；**M247** 处理 V226 唯一键/版本化（草案与生效可并存）；**M009** `PlatformController` 转正端点；**M018** 前端；ticketing 读 `is_draft=FALSE` 且尊重 `effective_from`。不要为「差人」新加起草人列（BSM-001 要高敏转正，可用既有权限，不必抄 Finance 列）。不要套 M009-013「补列＝新建能力」口径——本条是行为冲突，只是闭合面跨模块。

非阻断（不否决）：候选写「V226 起草人列（M016）」模块号错误、列也不是本席原最小处置；r2 事实栏改 M247/M009，删起草人列。

票：**PASS（否/XM）**。

### 011 独立裁定（细化／是／否 XM）

核源：PGM-023 L472「发布为高危动作，须二次确认」。DTO/OpenAPI required 仅 `idempotency_key`+`candidate_ids`；schema `additionalProperties: false`（openapi-p0.yaml `:32976-32999`，M392）。前端 `FloorPlanImportPage:109-110` 发布体只有这两键；`:228-235` `HighRiskConfirm` 弹窗后才 POST。权限 `floor_plan.import.manage` high/requires_approval 不能替代请求体确认。curl 可绕过弹窗——这是 GR-001 原意，不是席甲 OBS-1「前端已有弹窗即足够」。

队列否：后端单独加必填确认字段 → 现网前端 400，且须改 additionalProperties:false 的 schema。本模块 DTO 在 FILES 内，但闭合要 M392+M018 同批。与 010 相同：原「建议入队：是」保留为缺陷登记，本模块 B3 不单独落地。最懒：XM 同批加确认字段（或缺省拒绝的等价服务端闸），前端发布体带上；本模块不先加必填项。

票：**PASS（否/XM）**。

### 012 独立裁定（全局遗漏／否；与 M009-013/018 同口径）

核源：V005 `spaces` 与 `SpaceRecord` 有面积/状态/所属商户，无「用途」「负责人」。PGM-023「记录空间面积、用途、状态、负责人和所属商户。」本席 GR-007 只点名负责人，并写「B2 定是否拆 XM」；席甲 CX-005 补用途，合入 012 不改写原意。补列是新建能力，本模块不加迁移。记录类随迁移同批，避免有字段无列。

M009-013 定稿：projects 补列＝全局遗漏，不在持有记录类的模块加迁移。spaces 同构。非阻断：候选写「迁移归 M016」——V005 在 **M010**，M016 四文件无 spaces 补列；新 V 号也不在本模块加。队列否不依赖这个模块号。

票：**PASS（否）**。

### 013 独立裁定（跨模块遗漏／否）

核源：`SHAPE_TYPES` 六值无 booth/public_area；V023:33 / V034 CHECK 同六值（M016 FILES）。PGM-023 L472 候选区域「铺位、摊位、公共区」。解析器不校验闭集，非白名单到 `normalizeAllowed` 整单 VALIDATION_FAILED。前端 M018 `SHAPE_TYPE_LABELS` 已含 booth/public_area，默认 `geojson`/`status=draft` 与后端闭集不一致——本席 OBS-04/05，升入本 ID 否队，不是把观察改写成「是」。

本模块改 Java 白名单而不放宽 CHECK 会在落库炸。不在本模块改已应用迁移。否（XM：M016 侧放宽 CHECK 的**新**迁移 + 本模块白名单同批；前端闭集 M018）。`risk_zone`/`route` 仍作点位细化、不升添加。

票：**PASS（否）**。

### 014 独立裁定（观察／否；未写错应升项）

候选未把本席应入队项留在观察：GR-001…012 均在 001–013；席甲 CX-001…005 均在 001/002/004/012/013。OBS-04/05 升入 013 否队，来源格写 OBS-01…03/06…13 排除 04/05，正确。席甲 OBS-1（前端弹窗）已归 011，不是「前端已足够」降级。判据 89→90 勘误与本席 B1 §1 一致。

事实栏覆盖：ProjectRecord 不重开、IdempotencyService 事务包裹（席甲 OBS-2）、is_draft 见 010、operatorNote、slug 铁律 6、V034 高危码授平台管理员（M016）、营业日≠场次开歇、ByProject 保留、saveFloorPlan 不改 merchant_id、铺位租赁在 contract。

非阻断：来源含 OBS-03，事实栏未点名「VenueBusinessHours 不走 IdempotencyService、靠 UNIQUE last-write-wins」。这不是应升项，r2 可补半句。席甲 OBS-4 的 `SpaceRecord.polygonPoints` 为 `List<Map>` 仍留否。

票：**PASS（否）**。

### 分类 / 队列 / 允许的最小修复边界

本批**不得定稿**（本席 FAIL）。r2 须改 007 最小处置后再送 B2。下列边界供 r2 使用；未经双 PASS 禁止 B3。一次 B3 将只改本模块 23 文件及其测试类（M017 五类可随「是」项），不改三 HTML、OpenAPI、迁移、FieldLabels、PlatformController、前端、ticketing、锁、台账。

- 001：图形/导入任务主键 `"fps_"+slug(floorPlanId+"_"+key)`、`"fpi_"+slug(floorPlanId+"_"+key)`（候选/错误随任务）；TDD 跨图同键不覆盖两例先红。不改发布追加语义。
- 002：创建 `project_id` 必填且 `projectExists`；缺失/不存在中文错误；NOT NULL 不在本模块。
- 003：列表商户 scope 优先，其次 `currentProjectScopeQuietly`→`findAllFloorPlansByProject`，平台管理员全量；`ensureFloorPlanExists` 项目归属；创建时项目 scope 存在则 project_id 等于 scope。
- 004：Parser 13 处 error_message 改中文，error_code 不变。
- 005/006：本模块抛中文整句，不改 FieldLabels。
- 007（须改候选）：三处诚实不分页（pageSize=max(size,1)，total=size）；禁止硬编码 500 切片；不新增分页参数。
- 008：`candidateIds.size()>200` 中文 VALIDATION_FAILED。
- 009：spaces 存在性 + 项目 scope 过滤；注入 AuthorizationService。
- 010–014：本模块 B3 不修。

禁止：单独把营业时间 save 改成草案；单独给发布 DTO 加必填确认字段；本模块加 spaces/V226 迁移；改 FieldLabels 去碰 candidate_ids/plan_name 全局义；把摊位/公共区只改 Java 不改 CHECK。

改动文件自身版本递增。

### 结论

全批差集无漏并。001 席甲独有项核源成立，技术性「是」，主键加平面图域最懒。003/009 与 GR-002/004/011 原意一致。005/006 抛中文整句、不改 FieldLabels 足够。007 `of(records,1,500)` 截断 listShapes → **FAIL**。010 冲突成立、队列否（XM）以免断现网，起草人列/M016 为非阻断。011 否（XM）以免现网发布 400。012/013 否与 M009-013/018 口径合规。014 未写错应升观察。逐 ID：007 FAIL，其余 PASS。本席 B2-r1 **FAIL**。未经双 PASS 不得把候选当定稿，更不称模块 PASS。步骤仍 B2；M015 IN_PROGRESS；B3=0。

## M015 B2-r2 整批归并复核（Grok 乙席）

- 文件自身版本：V0.03（本段追加；上方 V0.01 B1 43052 字节 + V0.02 B2-r1 追加，全文 **67177** 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，独立）。步骤：B2-r2 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3。
- 控制：160-audit-prompt.md V0.17；runbook V0.02；criteria-lock V0.11；modules.md V0.04（471 模块，M015 FILES 23 / 锁映射 90 / 计划冻结 1999 行）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `a041458d06051fed3811d562764cd5900bba6f72`（PR #435 已合并）。本模块 23 文件指纹与 B1 覆盖表逐文件全等。ledger V3.31 IN_PROGRESS；B3=0。无源修复。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲 B2-r1 PASS（其 007 经 `PageResponse.of:16-21` 切片复核后撤回），只核 r2 候选是否落实本席 B2-r1。
- 本席 B2-r2：**PASS**（001–014 逐 ID 均 PASS；r1 唯一否决点 007 已闭合；001/010/012/014 事实栏与 003 B3 边界非阻断均已并入）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。映射行 `M015 → [...]`（`criteria-lock.md` L6394）COUNT=90 unique=90（BEQR 22、BSM 14、GEN 18、PGM 36）。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要复现只读（`PageResponse.of` 切片在解释器内模拟，未改源、未跑主仓测试）。

此票只判断 findings.md 顶部 V1.92「当前 M015 B2-r2 修订候选」相对本次两份完整 B1 raw（含各自 B2-r1 追加段）的全批差集、GEN-017 四分类、工程缺陷、修复队列与最小修法，并核对该候选是否准确落实本席 B2-r1（V0.02）否决与非阻断意见、有无新的改写或遗漏。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL**、V0.02 B2-r1 **FAIL** 原文。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/M015-b1-grok-raw.md` 前缀（本席 B1+B2-r1，本段追加前） | V0.02 | 67177 | `418ba0b106e22b8708c514dd5b135532839f19a72b4d64a1476120e4a0655a73` |
| 其中 B1 前缀 | V0.01 | 43052 | `649f4b190d5d598b99e76d91b91a62aee7e861e22444d0b4e3ec2b433c41ca98` |
| `docs/audit-2026-09/modules/M015-b1-self-raw.md`（席甲 B1+B2-r1） | V0.02 | 36670 | `f84775f2ea3b0d86df292a68abc53e2c244f2435ccaf2f150bd2d3c3b45a68d5` |
| 其中席甲 B1 前缀 | V0.01 | 31125 | `0f51db18a789833b1c056e7f0fbcb94016169cf65372ec2dc1078d21c9417f34` |
| `docs/audit-2026-09/findings.md` | V1.92 | 771076 | `dcfec423ffb55e3027d5cdd2110343170f472ba47d190c458c62a57d677baded` |

ledger 只读：V3.31，591036 bytes，SHA256 `2825591ce8bcc829cd5ae1189997e74783f79b7d0e8497bef7a89ca956a6ce9f`；M015 IN_PROGRESS、B3=0。23 文件 `git ls-files` 合计 **1999** 行，与计划冻结及 B1 覆盖表逐文件字节/SHA 全等（`mismatch=0`）。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

三 HTML 只读核验（本席未改；与 B1 同）：

| 文件 | 现行 | SHA256 |
|---|---|---|
| 业务地图 | V0.14 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| 治理地图 | V0.12 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |
| 枚举速查 | V0.07 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |

工作区只读：`git status --porcelain` = ` M findings.md` / ` M ledger.md`（席甲落 r2 候选）、本两份 raw 未跟踪。本席只追加本指定路径。

候选形态：V1.92 顶部为 r2 **条目覆盖段**（非整表重写）；其下「当前 M015 B2-r1 整批候选」001–014 原表保留。未改条目（002/004/005/006/008/009/011/013 及 003 的分类/队列/处置）沿用 r1 表七列；修订条目（007 最小处置；001/010/012/014 事实栏；003 非阻断并入 B3 边界）以顶部为准覆盖对应单元格。本席按此读取，不把 r1 表残留「`PageResponse.of(records,1,500)` / PGM-022 引句 / V226 起草人列（M016）/ 迁移归 M016」当作本批有效处置。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers / using-arc | 先核路由；用户指定 B2 归并覆盖 Arc audit；Arc audit 会派子代理并另写 `docs/arc/audits/`，**未用** |
| receiving-code-review | 先核源/HTML/命令再接受或反驳；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：007 构造器是否仍切片、TDD 空列表是否自打、001 是否仍引 PGM-022、010 是否仍写起草人/M016、012 是否仍写 M016、014 是否仍漏 OBS-03、003 是否仍把无组织员打成 403 |
| verification-before-completion | 声称前先跑只读哈希/行号/源码核对；本票不声称 43 用例本轮新绿 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流；不改生产代码 |
| codebase-design | 删除测试：007 深度落在「契约 pageSize 与 items 一致」，不落真 SQL 分页 |
| ponytail-review | 只评最小删除/收窄，不落地。007 诚实不分页 = yagni（相对真 LIMIT/OFFSET 与硬编码 500） |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| long-running-background-tasks | 本步秒级，未起 monitor |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到**；未冒充调用 |

### r1 意见 → r2 候选对照（本席 B2-r1 原文，不采信席甲转述）

本席 B2-r1：**FAIL**。唯一否决点 007 最小处置 `PageResponse.of(records,1,500)` 截断 `listShapes`。另提出非阻断：001 判据列改 PGM-023 或删该句、留下 GEN-006/007；010 事实栏改 M247/M009、删起草人列；012 V005 在 M010、新 V 号亦不在本模块；014 补 OBS-03 半句；003 B3 须商户 scope 优先、平台管理员桥接 empty，勿把无组织全局员从现网全量改成 403。其余 001–006、008–014 分类/缺陷/队列票 PASS。

| 本席 r1 要求 | r2 候选（V1.92 顶部） | 落实？ |
|---|---|---|
| 007：三处诚实不分页 `new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size())` 或 `of(..., max(size,1))`；禁止硬编码 500 切片；不新增分页参数 | 「三处改 `new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size())`（pageSize=total=真实条数、items 全量；空列表 pageSize=1 与 `of()` safeSize 同约定），不新增分页参数、不硬编码 500；TDD 先加 `listShapes` `pageSize()==total()==items.size()` 一例红」 | 是（取本席第一支构造器；TDD 为例红，属 GEN-003，不改写两支原意） |
| 001：判据列改 PGM-023 **或** 删 PGM-022 引句，留下 GEN-006/007（支撑实为 GEN-006/007/002） | 「判据列改为 GEN-006/GEN-007/GEN-002/BSM-001（删 PGM-022 引句）；最小处置主键加平面图域不变」 | 是（删句支；BSM-001 本就在 r1 表，r1 已 PASS，非新加） |
| 010：事实栏改 M247/M009，删起草人列；队列仍否/XM 同批；本批不改服务草案行为 | V226 在 M247、转正端点 `PlatformController` 在 M009；XM 同批含本模块服务 + M247 唯一键/版本化 + M009 转正 + M018 前端 + ticketing 尊重 `effective_from`；不新加起草人列 | 是 |
| 012：V005 在 M010；新 V 号亦不在本模块加；队列否不变 | 「V005 `spaces` 在 M010（不是 M016）；补列的新 V 号亦不在本模块加」；队列否 | 是 |
| 014：事实栏补「VenueBusinessHours 不走 IdempotencyService、靠 UNIQUE last-write-wins」 | 「`VenueBusinessHoursService.save` 不走 IdempotencyService，靠 V226 `UNIQUE(venue_id, day_of_week)` last-write-wins（Grok OBS-03）」 | 是 |
| 003：商户 scope 优先、平台管理员桥接 empty；只在商户空时查项目；不把无组织全局员从全量改 403 | 「商户 scope 优先、平台管理员桥接为空……B3 只在商户 scope 为空时才查项目 scope（列表、`ensureFloorPlanExists`、创建校验三处同一写法），不把无组织全局员从『现网全量』改成 403」 | 是 |
| 002/004/005/006/008/009/011/013 口径不变 | 「其余 ID 分类/队列/处置不变」；r1 表对应行七列未改写 | 是 |

**r1 裁定未被误读。** 无把「诚实不分页」读成仍抄 500、无把「删 PGM-022 引句」读成继续用该句撑主键域、无把 010 再写成起草人列/M016、无把 012 补列塞进本模块、无把 003 写成无组织员 403。无新永久 ID、无锁外判据、无 HTML 修改申请、无添加项、不进 BLOCKED-ASK。

### 完整差集（GR-001…012、OBS-01…13、席甲 CX-001…005、OBS-1…6）

漏并：无。两席 B1 全部临时 ID 仍落在 001–014。r2 未增删 ID。

| 正式ID | 本次来源（不改写原意） | r2 分类/缺陷/队列 | 本席票 |
|---|---|---|---|
| M015-001 | CX-M015-001（本席 B1 未发现） | 技术性/是/是（判据列已删 PGM-022） | **PASS（技术性/是/是）** |
| M015-002 | GR-M015-010 + CX-M015-002 | 细化/是/是 | **PASS（r2未变）** |
| M015-003 | GR-M015-002 | 细化/是/是（B3 边界已并入） | **PASS（细化/是/是；B3 边界闭合）** |
| M015-004 | GR-M015-005 + CX-M015-003 | 细化/是/是 | **PASS（r2未变）** |
| M015-005 | GR-M015-006 | 细化/是/是 | **PASS（r2未变）** |
| M015-006 | GR-M015-012 | 细化/是/是 | **PASS（r2未变）** |
| M015-007 | GR-M015-008 | 技术性/是/是（处置改为诚实不分页） | **PASS（技术性/是/是；r1 否决点闭合）** |
| M015-008 | GR-M015-009 | 技术性/是/是 | **PASS（r2未变）** |
| M015-009 | GR-M015-004 + GR-M015-011 | 细化/是/是 | **PASS（r2未变）** |
| M015-010 | GR-M015-003 | 冲突/是/否（XM；事实栏已改 M247/M009、删起草人） | **PASS（冲突/是/否 XM；r2未改队列）** |
| M015-011 | GR-M015-001 | 细化/是/否（XM） | **PASS（r2未变）** |
| M015-012 | GR-M015-007 + CX-M015-005 | 细化（遗漏，全局）/是/否（V005 改 M010） | **PASS（否；事实栏闭合）** |
| M015-013 | CX-M015-004 + OBS-04/05 | 细化（遗漏，跨模块）/是/否 | **PASS（r2未变）** |
| M015-014 | 双方观察 + 判据 89→90 勘误 | 技术性/正向 /否/否（补 OBS-03） | **PASS（否；事实栏闭合）** |

冲突 1（010，否/XM）。细化入队 6（002/003/004/005/006/009）。技术性入队 3（001/007/008）。细化否 3（011/012/013）。观察否 1（014）。添加 0。

### 007 修订核实（r1 唯一否决点；已闭合）

本席 GR-M015-008 原最小处置只有两支：「真实 LIMIT/OFFSET」或「明确不分页并去掉虚假 size=50」。r1 候选第三支 `of(records,1,500)` 既非两支之一。

本批类型 4 核源（只读）：

- `FloorPlanService:79/95/141` 仍 `new PageResponse<>(records, 1, 50, records.size())`，`records` 为 `findAll*` 全量；现网 **items 全量、pageSize 谎报 50**。
- `PageResponse.of`（`:16-21`）对全量 `subList` 切片，`total=all.size()`；`size<1` 时 `safeSize=500`。解释器按同一算术模拟：`all=2000, of(1,500)` → items=500、pageSize=500、total=2000，多出的 1500 行客户端不可达。
- `MAX_IMPORT_CANDIDATES=2000`（`:42`）；发布后 `listShapes` 可达 2000 行。`FloorPlanController` 三列表 GET 均无 `page`/`size` 参数，无第 2 页入口。
- 对照 `PlatformController:176` 营业时间 `of(...,1,500)` 的前提是仓储注释「量级＝场地数×7，很小」，不能抄到图形列表。
- 现测：`FloorPlanControllerTests:163-164` stub `PageResponse<>(List.of(shape()), 1, 50, 1)` 只断言权限/首条 id；`FloorPlanServiceTests` 无 `pageSize==total==items.size()` 断言。现网 2 条时 `50==2==2` 为假，一例即可红。

r2 处置 = 本席第一支构造器原文：`new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size())`。记录构造器不切片，items 仍是传入全量；`n≥1` 时 pageSize=total=n，去掉虚假 50；空列表 pageSize=1、total=0，与 `of()` `safeSize` 下限同约定。不新增分页参数、不硬编码 500。此即 GR-008「明确不分页并去掉虚假 size=50」一支，且比真 LIMIT/OFFSET 更懒（三处一行 + 服务侧一例红，不改 Controller 签名、不改 SQL）。

TDD「`pageSize()==total()==items.size()` 一例红」锁的是谎报 50，须打 **非空** `listShapes` 服务返回（空列表按约定 pageSize=1≠total=0，不是本例断言对象）；不要只改 ControllerTests 的 stub。属 GEN-003，不构成新改写。

票：**PASS**。r1 否决点闭合。

### 001 修订核实（判据列勘误；分类/队列/处置 r2未变）

锁：PGM-022 来源治理地图 `:463`（h2「空间和点位」），原文无「不可静默覆盖既有布局证据」。该句在 PGM-023 子锚，治理地图 `:472`（本席读现行 HTML hit=True）。支撑主键域的是 GEN-006/007/002。r2 删 PGM-022 引句，留下 GEN-006/007/002/BSM-001。BSM-001 已在 r1 表且本席 r1 已 PASS，不是本轮新加。最小处置仍「主键加入平面图域」，TDD 跨图同键两例。票：**PASS**。

### 003 修订核实（B3 边界并入；分类/队列/处置 r2未变）

核源：`listFloorPlans:65-69` 只 `currentMerchantScope`，空则 `findAllFloorPlans()`；`findAllFloorPlansByProject` 仅仓储有、Service **零调用**；`ensureFloorPlanExists:568-572` 只 `checkMerchantOwnership`。`currentProjectScopeQuietly:510-515`：平台管理员桥 → `Optional.empty()`；否则 `Optional.of(requireProjectScope(actor))`，无 `organizationId` 抛 `PROJECT_SCOPE_REQUIRED`（与注释「永不抛」不完全一致，M006 已回归）。`currentMerchantScope` 对非管理员无商户已抛 `MERCHANT_SCOPE_REQUIRED`；现网「空则全量」实际只落在平台管理员桥。

r2 B3：商户 scope 优先；只在商户空时查项目；平台管理员桥接为空 → 仍全量；三处同一写法。这是 GR-002「商户优先避免误伤」+「不要把无组织全局员从现网全量改成 403」的落地，不发明权限码。票：**PASS**。

### 010 修订核实（事实栏勘误；冲突/是/否 XM r2未变）

核源：`V226__entitlement_session_kind_batch3.sql` 在 **M247** FILES（modules.md L3935），不在 M016（M016 仅 V023/V034/V184/V203）。表列止于 `is_draft`/`effective_from` 等，**无起草人列**。`PlatformController` 在 **M009** FILES（L137），`:180-185` 转正入口所在控制器；`:184` 有 `requireIdempotencyKey` 但不经本模块 Service 的 `IdempotencyService`。GR-003 原处置是 `is_draft` 还回草案/发布、生效日必填、停用另列，没有起草人列。

r2 队列仍否/XM 同批（本模块服务 + M247 版本化使草案与生效并存 + M009 转正 + M018 前端 + ticketing 读 `is_draft=FALSE` 且尊重 `effective_from`），本批不改 `VenueBusinessHoursService` 草案行为。与本席原意一致，不是改写成「冲突不成立」。票：**PASS（否/XM）**。

### 012 修订核实（事实栏勘误；否 r2未变）

核源：`V005__p0_platform_master_data.sql` 在 **M010** FILES（L163）。`spaces` 列：id/project_id/merchant_id/parent_space_id/space_name/space_type/floor_no/area_square_meter/polygon_points/status/时间戳，无「用途」「负责人」。`SpaceRecord` 同构。M016 四文件无 spaces 补列。补列属新建能力，新 V 号亦不在本模块加。与 M009-013/018 同口径。票：**PASS（否）**。

### 014 修订核实（事实栏补 OBS-03；否 r2未变）

核源：`VenueBusinessHoursService` / `Repository` **零** `IdempotencyService` 引用；`save` 每次新 `vbh_`+UUID 后 `ON CONFLICT (venue_id, day_of_week) DO UPDATE`（`:48-61`），即 UNIQUE last-write-wins。`PlatformController:184` 只校验请求带幂等键，不把 save 送进 `IdempotencyService.execute`——与 OBS-03 原句一致，不是应升项。r2 补入事实栏。席甲 OBS-4 `polygonPoints` 仍留否。票：**PASS（否）**。

### 002 / 004 / 005 / 006 / 008 / 009 / 011 / 013（r2未变）

无新事实。复述 r1 票：002 细化/是（project_id 必填+存在性；NOT NULL 不在本模块）；004 Parser 13 处中文；005/006 本模块抛中文整句、不改 FieldLabels；008 `candidateIds.size()>200`；009 spaces 存在性+项目谓词、注入 AuthorizationService；011 否/XM（单独加确认字段现网 400）；013 否/XM（不在本模块改 CHECK）。票：八条均 **PASS（r2未变）**。

### 残留措辞（不构成新改写，不因此 FAIL）

r1 表 007 处置仍写 `PageResponse.of(records,1,500)`；001 判据列仍含 PGM-022 引句；010 仍写「V226 起草人列（M016）」；012 仍写「迁移归 M016」。这是未改条目沿用原表的历史单元格，**不是**本批有效口径。定稿后须以 V1.92 顶部 r2 覆盖段为准；B3 若按 r1 表 007 调用 `of(...,1,500)`，即违反本席本票。

007 TDD 一例须非空 `listShapes` 服务返回（现网谎报 50）；空列表 pageSize=1≠total=0 是 `of()` 约定，不是该断言对象。不要只改 ControllerTests stub。

### 分类 / 队列 / 允许的最小修复边界

本批可定稿（仍须席甲本批 B2-r2 PASS 合取）。队列是=001、002、003、004、005、006、007、008、009；否=010、011、012、013、014。一次 B3-r1 只改本模块 23 文件及其测试类（M017 五类可随「是」项），不改三 HTML、OpenAPI、迁移、FieldLabels、PlatformController、前端、ticketing、锁、台账。可执行行为先写归属该 ID 的最小失败测试（GEN-003）：

- 001：图形/导入任务主键 `"fps_"+slug(floorPlanId+"_"+key)`、`"fpi_"+slug(floorPlanId+"_"+key)`（候选/错误随任务）；TDD 跨图同键不覆盖两例先红。不改发布追加语义。
- 002：创建 `project_id` 必填且 `projectExists`；缺失/不存在中文错误；NOT NULL 不在本模块。
- 003：列表商户 scope 优先；仅当商户 scope 为空时查 `currentProjectScopeQuietly`→有则 `findAllFloorPlansByProject`、平台管理员桥 empty 则全量；`ensureFloorPlanExists`/创建校验同一写法。不把平台管理员从现网全量改成 403。
- 004：Parser 13 处 error_message 改中文，error_code 不变。
- 005/006：本模块抛中文整句，不改 FieldLabels。
- 007：三处 `new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size())`；禁止 `of(...,1,500)` 与硬编码 500；不新增分页参数；TDD 非空 `listShapes` 一例 `pageSize()==total()==items.size()` 先红。
- 008：`candidateIds.size()>200` 中文 VALIDATION_FAILED。
- 009：spaces 存在性 + 项目 scope 过滤；注入 AuthorizationService。
- 010–014：本模块 B3 不修。

禁止：按 r1 表 007 切片 500；单独把营业时间 save 改成草案；单独给发布 DTO 加必填确认字段；本模块加 spaces/V226 迁移；改 FieldLabels 去碰 candidate_ids/plan_name 全局义；把摊位/公共区只改 Java 不改 CHECK；为 010 新加起草人列。

改动文件自身版本递增。

### 本席实跑（B2-r2，只读；无写模式）

- 锁文件 hashlib / sha256sum：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（与 sidecar、ledger 顶行、本文件既有文首独占行同值）
- 输入三文件字节与 SHA：上表全等（与用户给定清单全等）
- 23 文件指纹：与 B1 表全等，合计 1999 行
- HEAD/`origin/main`：`a041458d06051fed3811d562764cd5900bba6f72`
- 点读：`PageResponse.of:16-21` subList；`FloorPlanService:42/79/95/141`；Controller 三 GET 无 page/size；`currentProjectScopeQuietly:510-515` / `requireProjectScope:288-294`；`ensureFloorPlanExists:568-572`；Service 零调用 `findAllFloorPlansByProject`；V226 在 M247、无起草人列、`UNIQUE(venue_id, day_of_week)`；V005 在 M010、spaces 无用途/负责人；`VenueBusinessHoursService` 零 IdempotencyService；PGM-022 `:463` vs PGM-023 `:472` 引句；治理地图 L472 hit=True
- 解释器模拟：`of(2000,1,500)` → items=500；构造器 `max(n,1)` 对 n=2000 全量、n=0 时 pageSize=1/total=0
- 未跑：主仓测试、psql 写、部署、修复
- 未改 findings/ledger/锁/源/HTML/主索引

### 结论

候选按本席 B2-r1 意见修订：007 改为三处诚实不分页构造器 + `listShapes` 一例红，即 GR-008「明确不分页并去掉虚假 size=50」一支，且最懒可行；001 删 PGM-022 引句；010 改 M247/M009、删起草人列；012 改 V005 在 M010；014 补 UNIQUE last-write-wins；003 B3 边界商户优先、管理员桥 empty。002/004/005/006/008/009/011/013 口径未改。全量差集无漏并、无新 ID、无新判据、无本批添加项。r1 任一裁定均未被误读。r1 表残留单元格不改变已裁定的队列/分类。

本席队列：**001、002、003、004、005、006、007、008、009 是（9 项）；010、011、012、013、014 否（5 项，010/011 XM）**。

本席 B2-r2：**PASS**

未经双 PASS 不进 B3，更不称模块 PASS。步骤仍 B2，M015 `IN_PROGRESS`，B3=0。不覆盖上方 B1 FAIL 与 B2-r1 FAIL 原文。
