CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M015 B5-r1 整模块全量三线审计（Grok 席乙）

- 文件版本：V0.01（指定路径原先不存在，本次新建完整报告）
- 席位：固定 Grok 外审席乙（grok-4.6；不派子代理、不用 MCP、不问操作员、不读本轮 self-raw）
- 步骤：B5-r1（B3 两次整批修复 r1+r2 后的整模块全量三线审计；不是只看 diff；不可用 B4 双 PASS、B3 自报红→绿或 B1 历史票替代）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M015 同一 23 文件（floorplan 包 17 + governance 包 ProjectRecord/SpaceRecord/SpaceTypeRecord/VenueBusinessHoursRecord/Repository/Service）；`runbook.md` V0.02；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 模块：M015 — 空间点位、楼层平面图与营业时段／后台实现／第1段
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算 `sha256sum` / `python3 hashlib` / `openssl dgst -sha256` 三工具同值（889020 字节 / 6850 行）；sidecar 全文一行同值。独占锁行仅本文第一行，正文不再出现同名独占字段。
- 映射：`M015 → [BEQR-063, BEQR-074, BEQR-143, BEQR-144, BEQR-145, BEQR-146, BEQR-147, BEQR-148, BEQR-149, BEQR-150, BEQR-151, BEQR-152, BEQR-153, BEQR-154, BEQR-155, BEQR-156, BEQR-157, BEQR-158, BEQR-159, BEQR-166, BEQR-170, BEQR-189, BSM-001, BSM-003, BSM-004, BSM-005, BSM-006, BSM-007, BSM-009, BSM-028, BSM-029, BSM-072, BSM-074, BSM-114, BSM-115, BSM-120, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-001, PGM-002, PGM-003, PGM-004, PGM-005, PGM-006, PGM-007, PGM-008, PGM-009, PGM-010, PGM-011, PGM-012, PGM-013, PGM-014, PGM-015, PGM-016, PGM-017, PGM-018, PGM-019, PGM-020, PGM-021, PGM-022, PGM-023, PGM-026, PGM-027, PGM-031, PGM-035, PGM-040, PGM-041, PGM-042, PGM-043, PGM-044, PGM-048, PGM-060, PGM-065, PGM-066]`，COUNT=90 unique=90 MISSING=[] EXTRA=[]（BEQR 22 / BSM 14 / GEN 18 / PGM 36）；与锁映射行 L6394 逐字相同；与本席 B1 所列逐字相同；定义与 HTML 出处均从锁自读，不新造判据、不 LOCK-AMEND
- 正本检出：`HEAD=origin/main=a041458d06051fed3811d562764cd5900bba6f72`（PR #435 已合并）。B3-r2 after tree 对象 `e7d8355fc16574f780cf51b134f06c72550cb666` 可解析为 tree；本模块 23 文件当前工作区 blob 与该 after **MISMATCH_COUNT=0**。HEAD blob 仍为修复前（B3 未提交）
- 本席约束：只读业务代码与计划；除本 raw 外不写文件；不修源/三 HTML/锁/findings/ledger；不 commit/push/PR/merge/部署；不装依赖；不写永久测试；主仓 cwd 不写模式（验证在 `/tmp/m015-b5-r1-verify`，结束已 `rm -rf`；凭据脚本 `/tmp/m015-b5-r1-creds.sh` 已删）
- 未读取：`M015-b5-r1-self-raw`（当前亦不存在）。B4-r1 / B4-r2 双席 raw 与 findings V1.94 只作已定稿边界，不作本 B5 通过依据
- 已定稿背景（只作边界，不代替本轮全量重审）：findings V1.94 — 本批定稿 ID 已修（M015-001…009 已修并经 B4-r1（002/004–009）与 B4-r2（001/003）双 PASS；010–014 否（010/011 XM 同批、012 全局遗漏、013 XM→M016/M018、014 观察））。B2 定稿禁止项（不得在 B5 升格为缺陷）：改迁移/OpenAPI/三份 HTML/权限种子/其它模块源码；新增字段/接口/业务规则；把跨模块登记项（候选区域类型摊位/公共区、spaces 用途/负责人）当本模块可修缺陷

本次审计结论：**PASS**。无新增未定稿工程缺陷、无添加项、无待问 2.2、无锁外判据、无 HTML 修改申请。001–009 在当前 23 文件上独立复验成立；010–014 维持队列「否」，本席不升格。模块生命周期仍 `IN_PROGRESS`，本票不等于双席 B5 或模块 PASS。席甲=Claude Fable 5.1（操作员 2026-09-09 接替指令），其票是作者自审票；本席 FAIL 具否决权——本席不行使否决。

---

## 0. 技能自核（V0.17 §6，按 B5 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令与 controller 优先于技能本体。本步禁止子代理，与技能「先派子代理」冲突处按用户指令收窄 |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `productivity/grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（锁哈希/HTML 行命中/derivedId 长度/FieldLabels 算法/mvn -Dtest）再下结论；B5 不修复 |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；未运行测试不写通过 |
| systematic-debugging | superpowers 同名 | 只借读错误→复现→追数据流（HTML 原句→Controller/Service/Repository/Parser→V005/V023/V034/V226/V201→AuthorizationService/IdempotencyService/测试/OpenAPI/前端只读）；B5 不修复 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地；不把 B2 禁止项写成缺陷 |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本步验证均秒～十几秒级，未起 monitor |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；Arc `audit` 会派评审子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用** |
| domain.md | `docs/agents/domain.md` | 词表/三 HTML 最终解释权；未改 CONTEXT.md |

B5 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）、using-git-worktrees、executing-plans、dispatching-parallel-agents、subagent-driven-development。

缺口（不冒称调用）：

- `batch-grill-me`：本机 mattpocock 技能树未找到独立技能（有 `grilling` / `grill-me` / `grill-with-docs`）
- `ubiquitous-language`：本机技能树未找到（`domain-modeling` 存在，V0.17 明确不用）
- Arc `audit`：与双席/§7 冲突，**不调用**
- Trail of Bits `audit-context-building`：会派 function-analyzer 子代理，**不调用**

明确不用（controller 划掉）：MCP、第三主体、修源/HTML/锁/台账。

---

## 1. 锁、范围与源指纹

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。独立实算值见本文第一行独占锁行。

| 项 | 值 |
|---|---|
| 锁文件 | `docs/audit-2026-09/plan/criteria-lock.md` V0.11 |
| 字节 | 889020 |
| 行（`wc -l` / `b'\n'` 计数） | 6850 |
| python3 hashlib | 与 sha256sum 同值 |
| openssl dgst -sha256 | 同值 |
| sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` | 全文一行同值 |
| 锁 blob | `8db3fc02ce8a6dc998863a9784e7e88af511b960` |
| findings | V1.94；SHA256 `21ae323afe35d15eb7f40d2150972a230a09e57e3be722a7acd8fe9f573deda4`；775822 字节 |
| ledger | V3.36；SHA256 `c5bbdbf914a71e59043d809839d65a120781bfca78eebe59c52a4100372df4c6`；597663 字节；M015 `IN_PROGRESS`，B3=2，B5-r1 进行中 |
| 映射行抽出 | 90 条、无重复、MISSING=[] EXTRA=[]；BEQR 22 / BSM 14 / GEN 18 / PGM 36 |
| 锁内映射 HTML 来源行 vs 现行三 HTML | 72/72 hit=True miss=0（脚本按锁「来源：`path:line`」对照现行 HTML 同行非空；GEN 18 条无 HTML 行） |
| 计划 FILES | 23 条与 modules.md V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 1999（切分时 HEAD）；当前工作区 `\n` 计数 **2109**（B3-r1+r2 后的现态，不改计划凑数） |
| questions | 无 `questions/M015*`；无未答问询 |
| 160 提示词 | `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17；SHA256 `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| modules.md / runbook.md | SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` / `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |

三 HTML 工作区 SHA（本席独立，未改）：

| 文件 | 现行 | 行 | 字节 | SHA256 |
|---|---|---:|---:|---|
| `docs/business-enum-quick-reference-v0.1.html` | V0.07 | 308 | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | V0.14 | 2032 | 304482 | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | V0.12 | 979 | 103015 | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |

三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。

`git status --porcelain`（本席只读核验，未再写）：本模块 6 个源文件 + 3 个测试类相对 HEAD 已改（B3 未提交）；findings/ledger 已改；本模块 B1/B4 双席 raw 未跟踪。本 23 文件全部 `endswith \n`。

---

## 2. 23 文件当前行数/字节/SHA 与全量三线覆盖表

行=`b'\n'` 计数。工作区 `git hash-object` = after tree `e7d8355fc16574f780cf51b134f06c72550cb666` 对应 blob。全文已读，未只读标题/搜索/截断。

| # | 路径 | 行 | 字节 | SHA256 | after blob | 覆盖 |
|---|---|---:|---:|---|---|---|
| 1 | `backend/src/main/java/com/complex/platform/floorplan/FloorPlanController.java` | 95 | 4323 | `33e14ba8868dd24c5235ba55658e51a38c7ff52572d3cb59fd10c07849941bef` | `723f582bf253a1e34a172a568bd26bce5804df1f` | 已读 L1–L95 |
| 2 | `backend/src/main/java/com/complex/platform/floorplan/FloorPlanRecord.java` | 21 | 503 | `9db3b6126ab0ca3e624e857684f68e680d7c99f9fe3e0ef6cf8a31080d373bce` | `ca563abcf21c7d7300f7dd28b085a213526141c9` | 已读 L1–L21 |
| 3 | `backend/src/main/java/com/complex/platform/floorplan/FloorPlanRepository.java` | 51 | 2148 | `9c5e50bff229838722ac00cc9096a8a188b111731be90da291a25c1f43c986a7` | `97d918beb8753be0c77e1246045d3645e1d61f57` | 已读 L1–L51 |
| 4 | `backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java` | 677 | 31050 | `d912fb381895537f930728f17f6e06df45f7ccd715331572ccd8debbe9834005` | `75663eebb0b0bf40ef89967877b832c961eb21f2` | 已读 L1–L677 |
| 5 | `backend/src/main/java/com/complex/platform/floorplan/JdbcFloorPlanRepository.java` | 454 | 21976 | `bb3fed48fb09410e7532c6930e7d3b2a3c83080490782cd2d04736b03cf502f3` | `8f27454787c3371f84ca944400f9ec6e2498a0e4` | 已读 L1–L454 |
| 6 | `backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportCandidateRecord.java` | 23 | 596 | `7bf4056cf8b920718b377272334b32f7d64c76f329078042f9e77bdedb065a33` | `34ac9ae7fa86690250c658991b84a2f7f3bc4230` | 已读 L1–L23 |
| 7 | `backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportDetailsResponse.java` | 14 | 458 | `5b0011b08d106da9a797ecd6d9915d665460c8823c03c50140b68354a11ae8d5` | `41c964f2510373e1bda8c777bb684f7b6466c598` | 已读 L1–L14 |
| 8 | `backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportErrorRecord.java` | 14 | 295 | `6cfdb0787b4f3b6a4b785106e9f89f26b366d5b25a0fe326f9c9f6032b6b4e92` | `c1e6075cc525afca265344bafe2e81a7f0242ad1` | 已读 L1–L14 |
| 9 | `backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParseResult.java` | 35 | 912 | `3315458ff08fb345a499a6fba09a296a671e92a6b07a717f8634ce0faab1666b` | `99bdc2681f0cb3e2dad8f0e3d499c07b3ddbc5dd` | 已读 L1–L35 |
| 10 | `backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportParser.java` | 303 | 12350 | `9c8fb4eef4fbe0a4b3dfbe1aaa74392f756e6b290a3e7aebb7d05fc3596e2ccb` | `53a26bc86c6293bd83e9c178b1b2c3aabd16def2` | 已读 L1–L303 |
| 11 | `backend/src/main/java/com/complex/platform/floorplan/FloorPlanImportTaskRecord.java` | 19 | 440 | `164d55ea7a81e8099c1ad8f44ef07bd3b3e474dfe7fb9f9d3f69893ac423a270` | `cf8f4cc98eeb17a2ff985c22660e7440dc0e766d` | 已读 L1–L19 |
| 12 | `backend/src/main/java/com/complex/platform/floorplan/FloorPlanPoint.java` | 7 | 112 | `cc32d4e81cc6e58813c676f5ebfeeff86e2e48d4ea360ad2183843ffd661b6f5` | `ff96bb78472413f4724228d77e4265af9fc0d088` | 已读 L1–L7 |
| 13 | `backend/src/main/java/com/complex/platform/floorplan/FloorPlanShapeRecord.java` | 22 | 546 | `4cfece0c8c5a9f13f2171b0d667eb29c109fe6cc3c9bfdf58590a575607e168f` | `670d0446650cf26f5a5a1f7123be152906154f75` | 已读 L1–L22 |
| 14 | `backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanCreateRequest.java` | 16 | 383 | `0bc12e7e268a7d8904645c560027f48262a330412cada0f44b0d7da47bef8625` | `ec6337e2003de5c9ffa18aa2191a5c96490b8a65` | 已读 L1–L16 |
| 15 | `backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportCreateRequest.java` | 10 | 240 | `7ecd9fca8fab9864815285d809a18375c6532c5036990599d11b12f3a99598a8` | `4e3b4f3b22a43cbf61a5408a17d0091279072faf` | 已读 L1–L10 |
| 16 | `backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanImportPublishRequest.java` | 13 | 348 | `ae890f5f0877b2b3d120d626434b63a0158cffa87f75afa6fb503ef8f2359859` | `4a8fd59b92df04e035b743a07d44a3900dced802` | 已读 L1–L13 |
| 17 | `backend/src/main/java/com/complex/platform/floorplan/dto/FloorPlanShapeSaveRequest.java` | 18 | 438 | `b890d08f0bfb9e5e1675dc4d2f84a5b21e5f59427b39d59053653e545e07e20d` | `1695827d7cf5293c3015c9e1e9a45973632b464e` | 已读 L1–L18 |
| 18 | `backend/src/main/java/com/complex/platform/governance/ProjectRecord.java` | 17 | 342 | `be4cac0713c505a6e2734af8a0fee0392008b1ae6762e776a8047d8bb5ce95c5` | `355c9db8f09e522b890330101b4ab4427133de0f` | 已读 L1–L17 |
| 19 | `backend/src/main/java/com/complex/platform/governance/SpaceRecord.java` | 22 | 515 | `f13f27eaf2dbdb754f18d4f841f1ebfd515d973e5863624583deab8adac7eac1` | `593348d18a1b762172b3fb4267e2ec471dffa1fa` | 已读 L1–L22 |
| 20 | `backend/src/main/java/com/complex/platform/governance/SpaceTypeRecord.java` | 15 | 386 | `388191c191993bac3ae452374d001a2236f5245a41257b6b9e2c6e7f649b7bd1` | `ac8d00fab69cf56024a5f8b1f86b1c862da55e91` | 已读 L1–L15 |
| 21 | `backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRecord.java` | 29 | 1244 | `ec04b55b8b3c747b9623450bd7460e00b6a585b2d2d0f8485f80cfc59def6957` | `0e075a32afd92284ad98d0ff0258bc65aba0c4c1` | 已读 L1–L29 |
| 22 | `backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursRepository.java` | 119 | 6399 | `f4557789c8eaa6cd0f5a8be883c2a32568264324fb2191848df33623ec02a6ba` | `30946ec517ca7a25d1b14ca1f7d91cf6ef349601` | 已读 L1–L119 |
| 23 | `backend/src/main/java/com/complex/platform/governance/VenueBusinessHoursService.java` | 115 | 5939 | `866fbe936d58375b88cb58cbd845c23c710f869c951e043a9d196c5512920f12` | `2192b92b9ecd7fed3e918a226b6c9277e1a398a4` | 已读 L1–L115 |

合计 **2109** 行。未改文件相对 HEAD 的 17 个小文件 blob 与 HEAD 全等；相对 HEAD 已改的 6 个本模块源文件（Repository / Service / Jdbc / Parser / VenueHours Repository / VenueHours Service）均等于 after tree。

只读依赖（不审这些文件本身，但必须追到事实）：

- 迁移：`V005`（projects/spaces，M010）、`V023`（mall_floor_plans / floor_plan_shapes，id VARCHAR(64)，SHAPE CHECK 六值）、`V034`（导入三表，id VARCHAR(64)）、`V226`（venue_business_hours is_draft/effective_from）、`V201`（营业日切日，**未映射本模块**）
- `IdempotencyService.runAndComplete`：生产注入 `TransactionTemplate` 后 action+complete 原子（L160–172）
- `AuthorizationService.currentMerchantIdQuietly`（L440–455，永不抛）/ `currentMerchantScope`（L422–431，无商户则抛）/ `currentProjectScopeQuietly`（L510–516，非平台管理员走 `requireProjectScope`，无 org 会抛）/ `requirePlatformScope`
- `AuditEventService`：平面图/导入/发布/营业时间均 recordSuccess/Failure
- `PlatformService`/`JdbcPlatformRepository`：spaces CRUD 与空间类型活校验在治理其它段，本模块只有记录类
- `contract/JdbcContractRepository` L459：铺位六态着色数据源（BEQR-170 实现面不在本 23 文件）
- 前端 `FloorPlanImportPage.tsx`：HighRiskConfirm 弹窗（L228–231）；发布体仅 idempotency_key+candidate_ids
- 测试（M017，只跑不审文件）：`FloorPlanControllerTests` 12、`FloorPlanImportParserTests` 4、`FloorPlanJdbcIntegrationTests` 3、`FloorPlanServiceTests` 27、`VenueBusinessHoursServiceTests` 9
- OpenAPI `FloorPlanImportPublishRequest`（`openapi-p0.yaml` L32976–32999）：required 仅 key+candidate_ids，`additionalProperties:false`，candidate_ids maxItems 200；描述「发布人工确认后」但 schema 无确认字段
- 出口翻译：`FieldLabels.translate` KEY_PREFIXED（`image_width 必须大于 0` → `图片宽度必须大于 0`）；本模块 005 已绕开 candidate_ids/plan_name 键碰撞

---

## 3. 三线审计（逐文件；第一性原理 / 顺从式 / 对抗式）

### 3.1 FloorPlanController.java（L1–L95）

- **第一性原理**：删除则 PGM-023 平面图导入/发布无 HTTP 入口。8 个端点：list/create 平面图、list/save 图形、list/create 导入、candidates、publish。写路径均 `requirePermission` + `requireIdempotencyKey`。
- **顺从式**：权限码 `floor_plan.view` / `manage` / `import.view` / `import.manage` 与 V023/V034 种子、OpenAPI `x-permission` 对齐。发布走独立 POST，符合「导入不直接写入正式区域」。
- **对抗式**：无 `@GuardedAction`（记观察）。publish 请求体无确认字段，curl 可绕过前端 `HighRiskConfirm`——与定稿 M015-011（XM，队列否）同根因，不升格。无项目 scope 参数，隔离在 Service。

### 3.2 FloorPlanRecord.java（L1–L21）

- **第一性原理**：mall_floor_plans 行映射。`merchantId` `@JsonIgnore` 防出网泄漏商户内键。
- **顺从式**：字段与 V023+V052 列一致。status 闭集在 Service/CHECK：active/inactive/archived。
- **对抗式**：`projectId` 创建路径现已必填（002）。无负责人/租赁状态（本记录不是铺位）。

### 3.3 FloorPlanRepository.java（L1–L51）

- **第一性原理**：仓储缝。`findAllFloorPlansByProject` 隔离闸口保留。
- **顺从式**：CRUD/导入任务/候选/错误接口齐全。新增 `projectExists`（002）。
- **对抗式**：Service **现已调用** `findAllFloorPlansByProject`（003）。默认空实现若被误用返回空列表——当前唯一实现是 Jdbc 覆盖。

### 3.4 FloorPlanService.java（L1–L677）——行为中枢

- **第一性原理**：删掉则导入解析、发布、幂等、审计、商户/项目过滤全部消失。
- **顺从式**：
  - 写路径走 `IdempotencyService.execute`（生产事务模板包裹多写）。
  - 导入上限 1_000_000 字符 / 2000 候选 / 1000 顶点；发布 `candidate_ids` ≤200（008）。
  - 列表诚实不分页：`new PageResponse<>(records, 1, Math.max(records.size(), 1), records.size())`（007）。
  - 派生主键 `derivedId(prefix, seed)=prefix+SHA-256[:32]` 恒 36 字符（001 r2）；候选/错误 `fpic_`/`fpie_`+slug(taskId_n) 随 36 字符任务 id ≤46，落入 VARCHAR(64)。
  - 列表/创建/归属：`currentMerchantIdQuietly` 永不抛，项目员工走到 `currentProjectScopeQuietly`（003）；`project_id` 必填且 `projectExists`（002）；项目员工创建必须等于 scope。
  - 中文整句：候选项列表不能为空 / 平面图名称不能为空 / 多边形至少需要 3 个顶点 / 导入内容超过 100 万字符上限；本模块 FIELD_LABELS 覆盖 status/shape_type/ref_type/source_format（005/006）。
  - 发布只接受 parsed+pending；跨图 shapeId 拒绝；审计成功/失败齐全。
- **对抗式**（grilling 自问，见 §7）：二次确认仍只在前端（011 不升格）；任务一次发布即 `published`，现测 `publishImportTaskRejectsAlreadyPublishedTask` 锁死该终态（与 008 的 200 上限并存记观察，不发明分批发）；商户账号创建不校验 project_id=其项目（003 商户优先口径，记观察）；`requirePositive("image_width")` 抛点含英文字段名，但 `FieldLabels.KEY_PREFIXED` 出网译成「图片宽度必须大于 0」（不升缺陷）；本文件已无 `currentMerchantScope(`。

### 3.5 JdbcFloorPlanRepository.java（L1–L454）

- **第一性原理**：SQL 适配器。删掉则无法持久化。
- **顺从式**：ON CONFLICT(id) 更新业务列；jsonb 读写失败抛 IllegalStateException（AUD-020）。`findAllFloorPlansByProject` / `projectExists` SQL 谓词下推。命名参数，无拼接。
- **对抗式**：`saveFloorPlan` ON CONFLICT **不更新 merchant_id**（014 观察）。`saveShape` ON CONFLICT 可改 `floor_plan_id`（Service 已拦跨图）。无事务注解——依赖调用方 IdempotencyService 事务模板。

### 3.6–3.9 / 3.11–3.13 / 3.14–3.17 记录与 DTO

| 文件 | 第一性原理 | 顺从 | 对抗 |
|---|---|---|---|
| ImportCandidateRecord | 候选行 | 与 V034 列一致 | 无租赁态 |
| ImportDetailsResponse | 复核聚合 | task+candidates+errors | 错误文案现为中文（004） |
| ImportErrorRecord | 解析错误行 | 与 V034 一致 | field_name 仍为机读键（points/viewBox），error_message 中文 |
| ImportParseResult | Parser 输出 | CandidateDraft/ErrorDraft | 默认 label `shape-N`（观察） |
| ImportTaskRecord | 导入任务 | 含 source_payload | payload 可含 SVG 全文 |
| FloorPlanPoint | 归一化顶点 | x/y double | 无 |
| FloorPlanShapeRecord | 正式区域 | 与 V023 CHECK 六类 shape / 六类 ref | 无铺位六态（BEQR-170 无实现面） |
| FloorPlanCreateRequest | 创建入参 | 含 projectId/幂等键 | operatorNote 不落库（014） |
| FloorPlanImportCreateRequest | 导入入参 | format/name/payload | 无 |
| FloorPlanImportPublishRequest | 发布入参 | 仅 key+note+ids | **无 confirmed 字段**（011 XM）；note 不进审计 details（014 同根） |
| FloorPlanShapeSaveRequest | 存图入参 | 含 shapeId | operatorNote 不落库 |

### 3.10 FloorPlanImportParser.java（L1–L303）

- **第一性原理**：SVG/standard_json → 候选+错误。正则抽 polygon/polyline，不用 XML 解析器（避免 XXE，正面）。
- **顺从式**：viewBox 正宽高；折线须闭合；点归一到 [0,1]；JSON 要 shapes 数组。error_message 已中文（004）：「导入内容不能为空」「SVG 必须包含宽高为正数的 viewBox」「多边形至少需要 3 个顶点」等。
- **对抗式**：不校验 SHAPE_TYPES（013 XM / OBS）。默认 shop/custom。不解析 `<path>`。`readViewBox` 用整份 payload 扫属性。`shape-N` 默认英文标签；「折线（polyline）」/「svg / standard_json」按铁律 7 系统内部标识符，不升缺陷。

### 3.18 ProjectRecord.java

- **第一性原理**：projects 行 DTO。PGM-020「记录项目状态、负责人、地址和经营范围」。
- **顺从式**：与 V005 projects 列一致（id/merchantId/venueType/name/code/floor/capacity/status/时间）。
- **对抗式**：缺负责人/地址/经营范围。**已登记 M009-013/018，不重开、不加迁移。**

### 3.19 SpaceRecord.java

- **第一性原理**：spaces 行。PGM-023「记录空间面积、用途、状态、负责人和所属商户」。
- **顺从式**：area/spaceType/status/merchantId/projectId/polygon 有。
- **对抗式**：无负责人、无独立「用途」列（012 全局遗漏，不升格；写路径在 PlatformService，非本模块）。

### 3.20 SpaceTypeRecord.java

- **第一性原理**：V184 字典一行。删除则后台可维护类型 DTO 消失。
- **顺从式**：typeCode/typeName/sortOrder/status。
- **对抗式**：本模块无 Service 写路径（无实现面，CRUD 在治理其它段）。活校验属 M013-004/M014-002。

### 3.21–3.23 VenueBusinessHours*

- **第一性原理**：V226 酒吧场次开歇配置面。删除则运营只能改库，入场资格/当天无限按场次核销会停。**这不是 BEQR-187 营业日 10:00 切日**（营业日在 V201，未映射本模块）。
- **顺从式**：day_of_week 0–6；跨零点 close<open 放行；开歇同刻拒绝；disabled 行保留。审计 viewed/updated。唯一键 (venue_id, day_of_week) upsert。场地须存在（`findVenueProjectId`，009）；列表 JOIN spaces 按 project_id；保存跨项目 `CROSS_PROJECT_FORBIDDEN`。中文：「场地不能为空」「开始/结束营业时间必须填写，格式 HH:mm」。
- **对抗式**：类注释仍写死「保存即生效、不做草稿/发布」，`is_draft` 复用为停用——与 BSM-001 冲突，但是定稿 010 XM 队列否，不升格。控制器（模块外）`requirePlatformScope` 拦商户。`list()` 直接调 `currentProjectScopeQuietly`：生产非平台管理员无 org 会抛（AuthorizationService 实现与「永不抛」注释不一致，禁改它模块，记观察）。不走 IdempotencyService（014）。`effective_from` 可空。

---

## 4. 90 条冻结判据核对

依据类型：1 HTML 原文 / 2 可执行产物 / 3 外部规范 / 4 可复现命令。无实现面必须写原因。不得把 010–014 升格为本模块未修缺陷。

| ID | 本模块适用面 | 结论 |
|---|---|---|
| BEQR-063 | 酒吧反结/沽清等；平面图发布不在该闭集 | **无实现面**。发布高危走 PGM-023 |
| BEQR-074 | 隐私桌台 | **无实现面** |
| BEQR-143…BEQR-151 / BEQR-153…BEQR-159 | 四类主体/状态/十一端/权限计算等 | **无实现面**（DTO 不实现这些状态机）。ProjectRecord 缺列不重开 |
| BEQR-152 | 所有数据默认按项目隔离 | **本模块读/写路径已按项目隔离**（003/009）。商户账号走商户闸优先（003 定稿口径） |
| BEQR-166 | 合同审批链 | **无实现面** |
| BEQR-170 | 铺位六态须在平面图着色 | **无实现面**：ShapeRecord 无租赁态；实现在 `JdbcContractRepository`（只读依赖） |
| BEQR-189 | 管理费 | **无实现面** |
| BSM-001 | 时刻类策略；涉及钱须草案转正不即改即生效 | 现码仍保存即生效。**维持 010 否（XM）**，不升格 |
| BSM-003 | 通用能力 | 权限分岗有码；引导/删除在其它模块 |
| BSM-004 | 配置存档删除审批 | 仅有 archived 状态成员，无删除/审批 API。**无实现面** |
| BSM-005 | 分步引导新建 | 后端无引导状态；前端 M018。**无实现面** |
| BSM-006 | 动态二维码 | **无实现面** |
| BSM-007 | 高危不自动给平台管理员 | 种子在 V034（M016）把 import.manage 授给 role_platform_admin。本模块只消费。观察，不升格 |
| BSM-009 | 基础管理含空间 | 平面图/空间 DTO 承载部分 |
| BSM-028 | 建立楼层/区域/点位 | 部分：平面图+SpaceRecord；创建必填 project_id（002） |
| BSM-029 | 和谁有关 | **无实现面**（无票务/合同接线代码） |
| BSM-072 / BSM-074 | 商户范围内使用 | 商户 scope 过滤有（`currentMerchantIdQuietly` + `findAllFloorPlansByMerchant`） |
| BSM-114 | 入口隔离 | 商户过滤有；项目员工按项目（003） |
| BSM-115 / BSM-120 | 子系统连接/已确认规则 | **无实现面** |
| GEN-001 | 必要性 | 包有上位 HTML；`findAllFloorPlansByProject` 现已调用，默认空实现仍保留给闸门 |
| GEN-002 | 可执行正确性 | 隔离副本 55/55 绿；001–009 行为与测试对齐。测试绿不替代 010–014 的 HTML 缺口（那些已定稿否） |
| GEN-003 | 测试有效性 | 现测覆盖 001–009 回归；未覆盖 011 确认字段/010 草案/201 候选续发。通过本身不替代行为证据 |
| GEN-004 | 信任边界 | project_id 存在性（002）；venue 存在性（009）；发布 200 上限（008） |
| GEN-005 | 授权最小权限 | 权限码存在且 controller 调用；项目隔离 003/009。二次确认仍前端（011 否） |
| GEN-006 | 数据完整性 | 生产 IdempotencyService 事务模板覆盖导入/发布多写。venue upsert 单句原子 |
| GEN-007 | 幂等 | 平面图写路径有 execute；venue 靠唯一键 last-write-wins（014） |
| GEN-008 | 接口一致 | 005/006 中文整句已绕开 FieldLabels 碰撞；发布 200 与 OpenAPI maxItems 对齐（008）。导入 2000 vs 发布 200 + 一次终态见观察，不新开缺陷 |
| GEN-009 | 错误恢复 | 非法 shape_type 在落候选时校验（013/OBS）。发布已 published 再发被拒（现测锁死） |
| GEN-010 | 安全 | Parser 非 XML；payload 上限有。SQL 命名参数 |
| GEN-011 | 审计 | 平面图/导入/发布/营业时间均 recordSuccess/Failure。operatorNote 不落平面图表/不进发布审计 details（014） |
| GEN-012 | 资源边界 | 导入上限有；列表诚实不分页（007）；发布 200（008） |
| GEN-013 | 构建依赖 | 本模块无新依赖声明 |
| GEN-014 | 迁移 | 迁移文件属 M016；本席只追数据流。V023 CHECK 与 Java 闭集一致。补列不在本模块加 |
| GEN-015 | 展示中文 | Parser/Service 用户句已中文（004/005/006）。`image_width 必须大于 0` 经 FieldLabels 出网「图片宽度必须大于 0」。polyline/svg 为系统内部标识符 |
| GEN-016 | 文档可验证 | OpenAPI 描述「人工确认」但 schema 无确认字段，与实现一致地缺确认（011） |
| GEN-017 | 四分类 | 本席已依序分类；无添加项 |
| GEN-018 | 证据 | 本报告命令与源码行号 |
| PGM-001 | 治理底座/中文 | 与 GEN-015 同结论 |
| PGM-002…PGM-004, PGM-006…PGM-016 | 其它治理入口 | **无实现面**（本 23 文件不实现那些模块） |
| PGM-005 | 空间和点位入口 | 本模块正是该入口后台 |
| PGM-017 | 基础对象带综合体项目归属 | 平面图创建必填已存在的 project_id（002） |
| PGM-018…PGM-021 | 综合体项目档案 | ProjectRecord 缺列不重开 |
| PGM-022 | 空间发生在哪 | 平面图归属项目（002）；空间负责人 012 否 |
| PGM-023 | 平面图导入；发布高危须二次确认；不可静默覆盖 | 导入/发布路径存在。追加写入不覆盖旧行。二次确认 011 否。负责人 012 否。摊位/公共区 013 否 |
| PGM-026 / PGM-027 | 商户/铺位着色 | BEQR-170 无实现面 |
| PGM-031 / PGM-035 | 经销/提成 | **无实现面** |
| PGM-040 | 单一功能授权 | 权限码颗粒度够 |
| PGM-041 | 数据按项目隔离；高危 | 项目隔离 003/009；二次确认 011 否 |
| PGM-042…PGM-044 | 职级/外包/桌台权限 | **无实现面** |
| PGM-048 | 商户侧授权范围 | 商户过滤有 |
| PGM-060 / PGM-065 / PGM-066 | 会员/连接/已拍板 | **无实现面** |

---

## 5. 已定稿 ID 独立复验（001–009 须在当前树上仍成立；010–014 不升格）

### 5.1 队列「是」已修 → 本席复验仍成立

| ID | 复验事实（当前树） | 证据 |
|---|---|---|
| **001** | `derivedId("fps_"/"fpi_", seed)` = 前缀 + SHA-256 前 32 位，恒 36 字符；图形/任务/发布图形三处。跨图同键两例仍在。现网键 `fpi-1757570000000` 与 UUID 键 python 实算均 ≤64（任务 36、候选 43、图形 36） | `FloorPlanService.java:268,296,393,650-654`；`FloorPlanServiceTests.derivedIdsFitVarchar64Columns` / `saveShapeWithSameKeyAcrossFloorPlansDoesNotOverwrite`；V023/V034 `id VARCHAR(64)` |
| **002** | `project_id` 必填；`repository.projectExists` 不存在 → `PROJECT_NOT_FOUND`「综合体项目不存在或已被删除」 | `FloorPlanService.java:506-510`；`JdbcFloorPlanRepository.java:64-69`；测试 `createFloorPlanRequiresProjectId` / `createFloorPlanRejectsUnknownProject` |
| **003** | 五处 `currentMerchantIdQuietly`（列表/创建/校验项目/ensure 商户闸/ensure 项目闸）。项目员工 `listFloorPlansFiltersToProjectScope`、跨项目 `CROSS_PROJECT_FORBIDDEN`、创建他项目拒绝。本模块源 **零** `currentMerchantScope(` | `FloorPlanService.java:72,230,511,602,612`；测试三例 + `serviceWithProjectScope` 桩（生产语义：quietly 空、project 有值） |
| **004** | Parser 13 处 error_message 中文 | `FloorPlanImportParser.java:37,45,57,68,75,80,90,107,122,128,241-244`；`FloorPlanImportParserTests` 4 绿（断言码，文案在源） |
| **005** | 本模块中文整句，不经 FieldLabels 碰撞键 | `候选项列表不能为空` L374/585；`requireField(...,"平面图名称")` L236/519；`FIELD_LABELS` L48-49 |
| **006** | 去掉括号英文字段名 | `多边形至少需要 3 个顶点` L538；`导入内容超过 100 万字符上限` L556；VenueHours `场地不能为空` L60；`开始营业时间必须填写，格式 HH:mm` L99 |
| **007** | 三处诚实不分页 | L88/104/150 `Math.max(records.size(), 1)`；测试 `listShapesReportsHonestPageSizeAndTotal` pageSize==total==2 |
| **008** | `candidateIds.size() > 200` → VALIDATION_FAILED | L46 `MAX_PUBLISH_CANDIDATES=200`；L587-588；OpenAPI L32995 maxItems 200 |
| **009** | venue 必须是 spaces.id；列表/保存按项目 | `VenueBusinessHoursRepository.findVenueProjectId` / `listByProject`；Service L63-68, L44-47；测试 `rejectsUnknownVenue` / `rejectsVenueOutsideProjectScope` / `listFiltersByProjectScope` |

### 5.2 队列「否」——现码仍可见缺口，本席按 B2 禁止项不升格

| ID | 现码仍在 | 为何不升格 |
|---|---|---|
| **010** | `VenueBusinessHoursService.java:24-26`「保存即生效」；`is_draft`←disabled | 冲突 XM 同批（转正端点 M009 / V226 M247 / 前端 M018 / 核销侧）；B2 禁止当本模块可修 |
| **011** | `FloorPlanImportPublishRequest` 无确认字段；Controller L85-94 无闸 | XM：OpenAPI additionalProperties:false 须加字段（M392）+ 前端 M018 同批；单独加字段会让现网前端 400 |
| **012** | `SpaceRecord` 无用途/负责人列 | 全局遗漏；补列迁移不在本模块（与 M009-013/018 同口径）；B2 禁止把 spaces 用途/负责人当本模块可修 |
| **013** | `SHAPE_TYPES` 六值无摊位/公共区 | XM→M016 CHECK + 前端 M018 已含 booth/public_area；B2 禁止把候选区域类型当本模块可修 |
| **014** | 观察桶：Idempotency 营业时间、operatorNote、判据 90 勘误等 | 观察，队列否 |

---

## 6. 本次 findings / 观察

### 6.1 未定稿工程缺陷

无。

### 6.2 添加项（供 2.2）

无。

### 6.3 观察（不入队、不升格；临时编号仅本席 B5 登记）

- **GR-M015-B5-OBS-01**（同根因 008 已修）：`MAX_IMPORT_CANDIDATES=2000` 与 `MAX_PUBLISH_CANDIDATES=200` 并存，且 `publishImportTaskOnce` 一次成功即把任务写成 `published`（`FloorPlanServiceTests.publishImportTaskRejectsAlreadyPublishedTask` 锁死续发）。201+ 候选项不能一次发完也不能续发。008 最小处置仅对齐 OpenAPI maxItems，B4 已 PASS；本席不发明分批发工作流、不把已定稿 008 改回「是」。
- **GR-M015-B5-OBS-02**（同根因 003 商户优先）：商户账号 `currentMerchantIdQuietly` 有值时创建不校验 `project_id` 等于其项目，只校验项目存在。可把平面图挂到任意已有项目，项目员工列表会看见。003 定稿口径即「商户 scope 优先」；不新增「商户必须绑项目」规则。
- **GR-M015-B5-OBS-03**（它模块）：`AuthorizationService.currentProjectScopeQuietly` 注释写永不抛、无 org 的全局平台员工应 empty，实现却 `requireProjectScope` 无 org 即抛。VenueHours `list()` 与 FloorPlan 非商户分支会受影响。B2 禁止改其它模块源码。
- **GR-M015-B5-OBS-04**（同根因 014）：create/shape `operatorNote` 不落平面图表；发布 DTO 的 `operatorNote` 不写入任务、也不进 `recordSuccess` details（OpenAPI 写「进入审计留痕」）。
- **GR-M015-B5-OBS-05**：Parser 默认 label `shape-N`；error 中 polyline/svg/standard_json 为系统内部标识符。
- **GR-M015-B5-OBS-06**：`requirePositive(..., "image_width")` 抛点含英文字段名；`FieldLabels.KEY_PREFIXED` 出网「图片宽度必须大于 0」（python 复算 KEY_PREFIXED match=True）。不构成 GEN-015 出网英文。
- **GR-M015-B5-OBS-07**：`saveFloorPlan` ON CONFLICT 不更新 merchant_id；cancelled/rejected 状态成员无 API；无 `@GuardedAction`；Parser 不校验 SHAPE 闭集（013）；不解析 path。
- **GR-M015-B5-OBS-08**：ProjectRecord 缺列已闭环 M009-013/018。BEQR-187 营业日 ≠ V226 场次开歇。V034 把 high+requires_approval 的 import.manage 授给 role_platform_admin（种子 M016）。

### 6.4 HTML 修改申请

无。

---

## 7. 对抗式追问树（grilling 自问，不派子代理）

1. 发布能否不经二次确认？能。011 否，不升格。
2. 发布会不会静默 UPDATE 已有 shape id？导入派生新 hash id，追加不覆盖。覆盖条款未破。
3. 项目员工是否只看本项目平面图？是（003 复验）。
4. 项目员工调 `currentMerchantScope` 是否还能列出？本模块已不调会抛的方法。
5. 派生 id 对现网 `fpi-<13位>` 与 UUID 键是否仍超 64？否，恒 36 / 候选 43。
6. 营业时间是否即改即生效？是。010 否，不升格。
7. 解析错误运营能否读懂？中文（004）。
8. 空 candidate_ids 提示什么？「候选项列表不能为空」（005）。
9. 空间能否记负责人？不能。012 否。
10. 201 个候选能否发完？一次 200 上限 + 任务终态 published。OBS-01，不发明分批发。
11. 并发双 POST 导入？IdempotencyService+生产事务模板。不另开。
12. 铺位六态着色？本 23 文件无字段。无实现面（合同仓）。

ponytail-review：`findAllFloorPlansByProject` 默认空实现是隔离闸保留，注释禁止删除。`derivedId` 三处共用，非一层一实现。Lean already. net: 0 强制可删。不把禁止项写成缺陷。

---

## 8. 命令与真实输出 / 退出码

### 8.1 锁与检出

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md
python3 hashlib 同值；openssl dgst -sha256 同值
sidecar 全文一行同值
git rev-parse HEAD
# a041458d06051fed3811d562764cd5900bba6f72
git rev-parse origin/main
# a041458d06051fed3811d562764cd5900bba6f72
git cat-file -t e7d8355fc16574f780cf51b134f06c72550cb666
# tree
# 23 文件 work blob vs after：MISMATCH_COUNT 0
```

### 8.2 测试（隔离副本；只导出三个 COMPLEX_DATABASE_*；URL 强制 complex_test；未 source /etc/complex/complex.env）

第一次（失败，如实记）：`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` 无 javac。`mvn -o ... test` **exit 1**。

```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.15.0:compile
Fatal error compiling: error: release version 21 not supported
```

第二次（Temurin JDK 21；`/tmp/m015-b5-r1-verify/backend`；主仓未写 target）：

```
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
export COMPLEX_DATABASE_URL='jdbc:postgresql://127.0.0.1:5432/complex_test'
# USERNAME/PASSWORD 从 /etc/complex/complex.env 单行读取写入一次性脚本后 source，结束删除
mvn -o -Dtest=FloorPlanControllerTests,FloorPlanImportParserTests,FloorPlanJdbcIntegrationTests,FloorPlanServiceTests,VenueBusinessHoursServiceTests -Dsurefire.failIfNoSpecifiedTests=false test
```

关键输出：

```
Database: jdbc:postgresql://127.0.0.1:5432/complex_test (PostgreSQL 16.14)
Successfully validated 390 migrations
Current version of schema "public": 422
Schema "public" is up to date. No migration necessary.

Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- FloorPlanImportParserTests
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- FloorPlanJdbcIntegrationTests
Tests run: 27, Failures: 0, Errors: 0, Skipped: 0 -- FloorPlanServiceTests
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0 -- FloorPlanControllerTests
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 -- VenueBusinessHoursServiceTests

Results:
Tests run: 55, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
MVN_EXIT=0
```

未对 `complex_test` 做手工写（JDBC 测试 `@Transactional` 回滚）。未把未运行的测试写通过。第一次失败不折算为业务 finding。

### 8.3 derivedId / FieldLabels 复算（type 4）

```
task             len=36 fpi_06cd4dea7ac6a1fb4787ad5cdaccdc4b
candidate        len=43 fpic_fpi_06cd4dea7ac6a1fb4787ad5cdaccdc4b_1
shape            len=36 fps_...
KEY_PREFIXED 'image_width 必须大于 0' => '图片宽度必须大于 0'
本模块源 currentMerchantScope( 命中数 = 0
HTML 来源行 72/72 hit
```

### 8.4 清理

`rm -rf /tmp/m015-b5-r1-verify /tmp/m015-b5-r1-creds.sh /tmp/m015-b5-r1-db.env`。主仓无本席写入（本 raw 除外）。

---

## 9. 技能实用 / 缺口

实用：grilling 追问树定位 001 长度、003 抛点、008/一次终态交互；verification-before-completion 阻止把第一次 JDK 失败写成通过；diagnosing-bugs 用隔离副本 + complex_test 实跑 55 例；ponytail-review 未把隔离闸默认空实现标成可删。

缺口见 §0。未冒称调用 batch-grill-me / Arc audit / 子代理。

---

## 10. 本席 B5 结论

**PASS**。

23 文件全覆盖；90 判据逐项核对；三线均执行；声称均有四类证据之一。无未答 questions。无未定稿缺陷。无添加项。无 HTML 修改申请。Grok 哈希行有效。001–009 独立复验仍成立。010–014 维持否，不升格。本票不等于双席合取或模块 PASS。

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M015 B5-r1 再执行 整模块全量三线审计（Grok 席乙 V0.02）

- 文件版本：V0.02（同名 raw 追加；旧 41949 字节 V0.01 前缀完整保留，本段自本行上一空行后的独占锁行起）
- 席位：固定 Grok 外审席乙（grok-4.6 / reasoning xhigh；不派子代理、不用 MCP、不问操作员）
- 步骤：B5-r1 再执行（操作员 2026-09-20 裁定：历史双 PASS + 席甲 2026-09-19 单席补充复查不算完成，须席乙完整 B5 全量重审；B3 累计仍为 2，文件名 r1 不改）
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M015 23 文件；`runbook.md` V0.02；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 模块：M015 — 空间点位、楼层平面图与营业时段／后台实现／第1段
- 正本：`/home/james/complex-management` main 主工作区
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算 `sha256sum` / `python3 hashlib` / `openssl dgst -sha256` 三工具同值，未抄 sidecar / ledger / 派发期望 / 旧 raw
- 独立性：未读 `M015-b5-r1-self-raw.md`（含其任何新追加段）；旧 B1/B4/B5 grok 与 findings 只作已定稿边界，不复用历史票代替本轮取证
- 禁止动作：未修业务/HTML/锁/findings/ledger；未 fetch/commit/push/merge/deploy；未建脚本、日志或临时产物；验证命令 stdout 仅由本席工具缓冲

本次审计结论：**FAIL**。

新工程缺陷 2 条（临时编号 GR-M015-B5r1-01 / GR-M015-B5r1-02），均无已定稿可沿用且仍在修复队列的 ID（001–014 当前全部为「否」），按回退统一规则整批须先回 B2。001–009 已修点在当前源上仍成立，不重复立项。010–014 维持「否」，不升格。无添加项、无 HTML 自身问题申请、无锁外判据。本票不等于双席合取。

---

## 0. 技能自核（V0.17 §6，按 B5 收窄；环境实用强制技能如实记录）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能；用户指令优先。B5 禁止子代理/问人/修复 |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；Arc `audit` 会派评审子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用** |
| graft | `~/.claude/skills/graft/SKILL.md` | 已读。本任务已给出 23 个精确路径，按「已能点名则直接读源」收窄，未再跑 `graft ask/grep`（全文读源是 B5 硬要求，不能用节点摘要替代） |
| grilling | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证：越权、幂等、项目 scope、派生主键长度、发布上限、营业时段场地校验、错误文案中文 |
| grill-me | 同套件 `productivity/grill-me/SKILL.md`（转发 grilling） | 内部 sharpen |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红环（锁哈希、源行、AuthorizationService 真实现 vs 测试桩、surefire XML）再下结论；B5 不修复、不写新测试 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 接口契约：`currentProjectScopeQuietly` 名称 vs `requireProjectScope` 实现 |
| verification-before-completion | superpowers 同名 | 先跑指定 mvn 再写可执行声称 |
| systematic-debugging | superpowers 同名 | 只借读错误→复现→追数据流；B5 不修 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 可删面：`FloorPlanRepository.findAllFloorPlansByProject` 默认 `List.of()` 是隔离闸门禁锚点，不标删除；`orElseGet(findAllFloorPlans)` 在生产上是死通道，归缺陷而非「可删复杂度」 |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 已读；本次 mvn 约 6s 完成，未起 monitor |
| writing-for-agents | mattpocock 同名 | 本 raw 给下游合取席读 |

B5 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail 修复强度（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts、using-git-worktrees、executing-plans、dispatching-parallel-agents、subagent-driven-development、research、domain-modeling。

缺口（不冒称调用）：

- `batch-grill-me`：mattpocock 技能树无独立技能（有 grilling / grill-me）
- `ubiquitous-language`：未找到
- Arc `audit` / Trail of Bits `audit-context-building`：会派子代理，**不调用**

---

## 1. 锁、范围与源指纹

本席哈希命令：`sha256sum /home/james/complex-management/docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`（独立实算）。`python3 hashlib` 与 `openssl dgst -sha256` 同值。独占锁行仅本段第一行。

| 项 | 值 |
|---|---|
| 锁文件 | `docs/audit-2026-09/plan/criteria-lock.md` V0.11 |
| 字节 / 行 | 889020 / 6850 |
| sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` | 全文一行同值（核后一致，非抄写来源） |
| 映射行 `criteria-lock.md:6394` | 90 条、无重复、MISSING=[] EXTRA=[]；BEQR 22 / BSM 14 / GEN 18 / PGM 36 |
| 计划 FILES | 23 条与 modules.md V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 1999；当前工作区 `wc -l` **2118**（以实算为准，不改计划凑数） |
| 工作区 HEAD | `e456ba96c84185c8d39f18f548011daac48a6466`（`origin/main`=`84e3441fd0f2c4724621ff34be6d244e40f2ca7c`，主工作区另有未推提交/暂存，审计对象为工作区现状） |
| questions | 无 `questions/M015*` |
| 160 提示词 | `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 |

三 HTML 工作区 SHA（本席独立 `sha256sum`，只读未改）：

| 文件 | SHA256 |
|---|---|
| `docs/business-enum-quick-reference-v0.1.html` | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |

`plan/modules.md` SHA256 `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa`；`plan/runbook.md` SHA256 `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f`。

### 1.1 23 文件覆盖表（全文已读；SHA 为工作区 blob）

| 行(wc -l) | SHA256 | 路径 |
|---:|---|---|
| 95 | `33e14ba8868dd24c5235ba55658e51a38c7ff52572d3cb59fd10c07849941bef` | `backend/src/main/java/com/complex/platform/floorplan/FloorPlanController.java` |
| 21 | `9db3b6126ab0ca3e624e857684f68e680d7c99f9fe3e0ef6cf8a31080d373bce` | `.../FloorPlanRecord.java` |
| 51 | `9c5e50bff229838722ac00cc9096a8a188b111731be90da291a25c1f43c986a7` | `.../FloorPlanRepository.java` |
| 678 | `21e7ea67b986b73d827e20a5a24f55c96ad8340659e117bdb22e8714522a193c` | `.../FloorPlanService.java`（版本头 V0.03） |
| 454 | `bb3fed48fb09410e7532c6930e7d3b2a3c83080490782cd2d04736b03cf502f3` | `.../JdbcFloorPlanRepository.java` |
| 23 | `7bf4056cf8b920718b377272334b32f7d64c76f329078042f9e77bdedb065a33` | `.../FloorPlanImportCandidateRecord.java` |
| 14 | `5b0011b08d106da9a797ecd6d9915d665460c8823c03c50140b68354a11ae8d5` | `.../FloorPlanImportDetailsResponse.java` |
| 14 | `6cfdb0787b4f3b6a4b785106e9f89f26b366d5b25a0fe326f9c9f6032b6b4e92` | `.../FloorPlanImportErrorRecord.java` |
| 35 | `3315458ff08fb345a499a6fba09a296a671e92a6b07a717f8634ce0faab1666b` | `.../FloorPlanImportParseResult.java` |
| 303 | `9c8fb4eef4fbe0a4b3dfbe1aaa74392f756e6b290a3e7aebb7d05fc3596e2ccb` | `.../FloorPlanImportParser.java` |
| 19 | `164d55ea7a81e8099c1ad8f44ef07bd3b3e474dfe7fb9f9d3f69893ac423a270` | `.../FloorPlanImportTaskRecord.java` |
| 7 | `cc32d4e81cc6e58813c676f5ebfeeff86e2e48d4ea360ad2183843ffd661b6f5` | `.../FloorPlanPoint.java` |
| 22 | `4cfece0c8c5a9f13f2171b0d667eb29c109fe6cc3c9bfdf58590a575607e168f` | `.../FloorPlanShapeRecord.java` |
| 16 | `0bc12e7e268a7d8904645c560027f48262a330412cada0f44b0d7da47bef8625` | `.../dto/FloorPlanCreateRequest.java` |
| 10 | `7ecd9fca8fab9864815285d809a18375c6532c5036990599d11b12f3a99598a8` | `.../dto/FloorPlanImportCreateRequest.java` |
| 13 | `ae890f5f0877b2b3d120d626434b63a0158cffa87f75afa6fb503ef8f2359859` | `.../dto/FloorPlanImportPublishRequest.java` |
| 18 | `b890d08f0bfb9e5e1675dc4d2f84a5b21e5f59427b39d59053653e545e07e20d` | `.../dto/FloorPlanShapeSaveRequest.java` |
| 25 | `3d18793172d90f57cd5296ae76f2d15983a61a60a5ff93b24dd8bd1697284487` | `.../governance/ProjectRecord.java`（工作区相对 HEAD 已暂存 M009 字段，见观察） |
| 22 | `f13f27eaf2dbdb754f18d4f841f1ebfd515d973e5863624583deab8adac7eac1` | `.../governance/SpaceRecord.java` |
| 15 | `388191c191993bac3ae452374d001a2236f5245a41257b6b9e2c6e7f649b7bd1` | `.../governance/SpaceTypeRecord.java` |
| 29 | `ec04b55b8b3c747b9623450bd7460e00b6a585b2d2d0f8485f80cfc59def6957` | `.../governance/VenueBusinessHoursRecord.java` |
| 119 | `f4557789c8eaa6cd0f5a8be883c2a32568264324fb2191848df33623ec02a6ba` | `.../governance/VenueBusinessHoursRepository.java` |
| 115 | `866fbe936d58375b88cb58cbd845c23c710f869c951e043a9d196c5512920f12` | `.../governance/VenueBusinessHoursService.java` |
| **2118** | | **23 文件合计** |

---

## 2. 可执行证据（类型 2 / 4）

命令（只从 `/etc/complex/complex.env` 逐行读取三个键，URL 库名 `/complex`→`/complex_test`；未 source 该文件；未连生产库 `complex`；未 DDL/写生产）：

```
mvn -o -q --file backend/pom.xml -Dtest=FloorPlanControllerTests,FloorPlanImportParserTests,FloorPlanJdbcIntegrationTests,FloorPlanServiceTests,VenueBusinessHoursServiceTests -Dsurefire.failIfNoSpecifiedTests=false test
```

- 进程环境仅保留 PATH/HOME/USER/JAVA 等必要项 + 三个 `COMPLEX_DATABASE_*`；`COMPLEX_DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/complex_test`
- 未导出 `SPRING_PROFILES_ACTIVE`（按派发）；Spring 回落到默认 profile `local`
- Flyway：`Successfully validated 395 migrations`；`Current version of schema "public": 427`；`No migration necessary`
- **MVN_EXIT=0**
- surefire XML（`backend/target/surefire-reports/TEST-*.xml`，含 `@Nested`）：

| XML | tests | failures | errors | skipped |
|---|---:|---:|---:|---:|
| FloorPlanControllerTests | 14 | 0 | 0 | 0 |
| FloorPlanImportParserTests | 14 | 0 | 0 | 0 |
| FloorPlanJdbcIntegrationTests | 7 | 0 | 0 | 0 |
| FloorPlanServiceTests | 36 | 0 | 0 | 0 |
| VenueBusinessHoursServiceTests | 12 | 0 | 0 | 0 |
| **合计** | **83** | **0** | **0** | **0** |

GEN-003：测试通过本身不替代行为证据。本席不以 83 绿宣称 003 的「平台管理员全量」已在生产路径成立——见 GR-M015-B5r1-01（测试桩把 `currentProjectScopeQuietly` 改写成返回 `Optional.empty()`，与 `AuthorizationService` 真实现相反）。

派生主键长度（类型 4，python hashlib 复算，与 `FloorPlanService.derivedId` 同算法）：`fps_`/`fpi_` = 前缀+32 hex = **36**；候选/错误 `fpic_`/`fpie_` + 任务 id + `_1` = **43**；`fp_`+UUID = **39**；`vbh_`+32 hex = **36**。均 ≤ VARCHAR(64)。M015-001 现态成立。

---

## 3. 第一性原理三问（按文件组）

### 3.1 FloorPlanController

1. **为何必须存在**：删掉则无 `/api/v1/floor-plans` 入口，PGM-023「商场平面图网页导入…人工复核后发布」与 BSM-028「建立综合体项目、楼层、区域、点位」的后台写/读做不到。
2. **声称是否执行**：8 个映射均 `requirePermission` + 写路径 `requireIdempotencyKey`。本席跑 `FloorPlanControllerTests` 14/14，日志可见无权限 `PERMISSION_DENIED`、缺幂等键 `VALIDATION_FAILED raw_message=idempotency_key is required`。
3. **未核实前提**：权限码是否在种子里授给目标岗位——种子不在本模块，本席不发明授权矩阵；入口本身有服务端闸。

### 3.2 FloorPlanService + DTOs + 记录类 + Parser + Jdbc 仓库

1. **为何必须存在**：平面图 CRUD、多边形、导入解析/候选/发布、项目存在性与 scope 过滤是 PGM-022/023 与 BSM-028 的下位实现。
2. **声称是否执行**：创建校验 `projectExists`、列表商户优先/项目其次、派生 id、发布上限 200、导入节点上限、中文校验句、SHAPE_TYPES 八值，均在源中；测试 36+14+7 绿覆盖其中主路径。生产幂等写由 `IdempotencyService.runAndComplete` 在注入 `PlatformTransactionManager` 时包事务（`IdempotencyService.java:167-172`）。
3. **未核实前提（本轮被证伪）**：`listFloorPlans` / `checkProjectOwnership` / `validateCreateRequest` / `VenueBusinessHoursService.list|save` 把 `currentProjectScopeQuietly()` 当作「无项目则 empty、平台管理员全量」。真实现是 `Optional.of(requireProjectScope(actor))`，无 `organizationId` **抛** `PROJECT_SCOPE_REQUIRED`（`AuthorizationService.java:289-294, 504-507`）。`orElseGet(repository::findAllFloorPlans)` 与 `listAll()` 在生产不可达。见 GR-M015-B5r1-01。

### 3.3 VenueBusinessHours*

1. **为何必须存在**：服务注释与 BSM-001/场次划界：入场资格/当天无限权益靠营业时段；无配置入口则只能改库。
2. **声称是否执行**：星期 0–6、HH:mm、开歇同刻拒绝、场地必须是 `spaces.id`、项目 scope 过滤；`VenueBusinessHoursServiceTests` XML 12/12 绿。Controller（非本模块）另有 `requirePlatformScope`。
3. **未核实前提**：同 3.2，平台管理员无 org 时 `list()`/`save()` 在服务层抛 `PROJECT_SCOPE_REQUIRED`，与测试桩 `Optional.ofNullable(null)` 不符。

### 3.4 ProjectRecord / SpaceRecord / SpaceTypeRecord

1. **为何必须存在**：综合体项目、空间、空间类型是平面图 `project_id` 与营业时段 `venue_id=spaces.id` 的归属载体（PGM-005/017/022，BSM-028）。
2. **声称是否执行**：本模块内仅为记录类型，无行为；`floorplan` 包零引用 `ProjectRecord` 字段。
3. **未核实前提**：工作区 `ProjectRecord` 相对 HEAD 暂存增加 `ownerPerson`/`address`/`businessScope` 及兼容构造（`git diff --cached`），属 **M009 在途**，本模块只登记跨模块观察，不入本模块队列。

---

## 4. 顺从式：90 项逐条（定义来自锁，不新增判据）

适用口径：本模块文件对锚点有实现面则核一致性；无实现面则核无冲突/无发明。映射不等于要求本模块实现沽清、合同审批或管理费。

### 4.1 BEQR（22）

| ID | 适用 | 证据（类型 1=锁内 HTML 原文） | 结论 |
|---|---|---|---|
| BEQR-063 高危动作 | 本模块无反结/沽清/免单实现面；发布二次确认已定稿 M015-011（否/XM） | 锁 L786；PGM-023 L472 发布高危走 011 | 无新冲突 |
| BEQR-074 隐私桌台 | 无桌台权限实现 | 锁 L907 | 无本面、无发明 |
| BEQR-143 四类主体 | ProjectRecord/SpaceRecord 不发明四类主体枚举 | 锁 L1666 | 无发明 |
| BEQR-144 经营主体关系 | 无行政隶属链硬编码 | 锁 L1677 | 无冲突 |
| BEQR-145 城市项目公司 | 无把项目公司写成管酒吧 | 锁 L1688 | 无冲突 |
| BEQR-146 运营公司职责 | 无本面 | 锁 L1699 | 无本面 |
| BEQR-147 主要收入归属 | 无票款/租金记账 | 锁 L1710 | 无本面 |
| BEQR-148 合作模式 | 无合作模式枚举发明 | 锁 L1721 | 无本面 |
| BEQR-149 职级 | 无职级 | 锁 L1732 | 无本面 |
| BEQR-150 终端十一端 | 本模块为 Web API 后台，不发明终端清单 | 锁 L1743 | 无本面 |
| BEQR-151 最终权限计算 | 入口 `requirePermission`；计算在 access 模块 | 锁 L1754 | 无冲突 |
| BEQR-152 项目与敏感数据权限 | **适用核心**。列表/读写按商户或项目过滤；**平台之上跨项目汇总通道在生产不可达** | 锁 L1765；`FloorPlanService:72-79,601-610`；`VenueBusinessHoursService:42-69`；见 01 | **本项支撑 FAIL** |
| BEQR-153 终端开关 | 无开关实现 | 锁 L1776 | 无本面 |
| BEQR-154 综合体项目状态 | 不发明项目状态机；`projectExists` 只查存在 | 锁 L1787；`JdbcFloorPlanRepository:65-69` | 无发明 |
| BEQR-155 商户状态 | 无本面 | 锁 L1798 | 无本面 |
| BEQR-156 合同状态 | 无本面 | 锁 L1809 | 无本面 |
| BEQR-157 员工状态 | 无本面 | 锁 L1820 | 无本面 |
| BEQR-158 设备状态 | 无本面 | 锁 L1831 | 无本面 |
| BEQR-159 权限状态 | 无本面 | 锁 L1842 | 无本面 |
| BEQR-166 合同审批链 | 无合同审批 | 锁 L1919 | 无本面 |
| BEQR-170 铺位租赁状态 | 平面图 shape 不是租赁六态；六态在 contract 仓（014 观察） | 锁 L1963；SHAPE_TYPES 八值无租赁态 | 无冲突 |
| BEQR-189 管理费方式 | 无本面 | 锁 L2172 | 无本面 |

### 4.2 BSM（14）

| ID | 适用 | 证据 | 结论 |
|---|---|---|---|
| BSM-001 整体说明/可配原则 | 营业时段「保存即生效」与「涉及钱的配置须草案化」之张力已定稿 M015-010（否/XM），不升格 | 锁 L2271 含「不即改即生效」；`VenueBusinessHoursService:24-26` | 维持 010 |
| BSM-003 通用能力 | 引导/分岗在前端与权限模块；本模块提供后端闸 | 锁 L2349 | 无冲突 |
| BSM-004 配置可存档删除 | 营业时段停用=is_draft 复用，不物理删（010 语义） | 锁；`VenueBusinessHoursRecord:15-16` | 随 010 |
| BSM-005 分步引导新建 | 后端无引导 UI；不发明步骤 | 锁 | 无本面 |
| BSM-006 动态二维码 | 无本面 | 锁 L2395 | 无本面 |
| BSM-007 平台管理员日常权限 | **适用**。平面图查看属日常后台能力；无 org 的平台管理员会被项目 scope 抛点挡住 | 锁 L2407；与 01 同根 | **本项支撑 FAIL** |
| BSM-009 基础管理 | 空间/项目/平面图属基础管理盘子 | 锁 L2439 | 有上位 |
| BSM-028 主要功能 | 建立楼层/区域/点位；导入发布为下位 | 锁 L2730 HTML「建立综合体项目、楼层、区域、点位」 | 有上位；发布状态机见 02 |
| BSM-029 和谁有关 | 连接基础管理 | 锁 | 无冲突 |
| BSM-072 商户完整使用自己的系统 | 商户 scope 列表 `findAllFloorPlansByMerchant`；不能看他商户 | `FloorPlanService:75-76,612-617` | 商户面成立 |
| BSM-074 和谁有关 | 商户不能看平台全局和其他商户 | 同上 | 无冲突 |
| BSM-114 各类入口 | API 按权限码分 view/manage/import | Controller:33-93 | 无冲突 |
| BSM-115 相互关系 | 平面图连空间/项目 | create 校验 projectExists | 无冲突 |
| BSM-120 已确认规则 | 「未来集团视图、做好项目归属」 | 锁 L4909；project_id 必填（002） | 项目归属成立；跨项目汇总见 01 |

### 4.3 GEN（18）

| ID | 适用 | 证据 | 结论 |
|---|---|---|---|
| GEN-001 | 23 文件均有可验证用途；`findAllFloorPlansByProject` 默认空实现是隔离闸锚点 | 仓库接口注释 L10-16 | 不删 |
| GEN-002 | 主路径测试绿；**管理员全量路径生产不可执行** | mvn 83 绿 vs AuthorizationService 抛点 | **01 缺陷** |
| GEN-003 | 测试桩伪造 quietly 永不抛 | `FloorPlanServiceTests:445-461` 注释写「生产语义…永不抛」；真代码抛 | **01 缺陷** |
| GEN-004 | 创建/导入/发布/营业时段均有校验 | Service validate* | 成立（除 01） |
| GEN-005 | 入口权限 + 对象归属；管理员被误伤、跨项目汇总做不到 | Controller + Service check* | **01 缺陷** |
| GEN-006 | 派生 id 分平面图域；生产导入写在幂等事务内 | derivedId L653-655；IdempotencyService L167-172 | 001 成立 |
| GEN-007 | 幂等键 + 事务；发布非幂等重入靠任务状态 | publish 仅 `parsed` 可发 | 发布一次终态；子集后剩余见 02 |
| GEN-008 | 八值白名单与 V423/M016 同批；DTO 无分页参数与诚实 PageResponse 一致 | SHAPE_TYPES L35；PageResponse L79/95/141 | 成立 |
| GEN-009 | 部分发布把任务标 published、其余候选仍 pending | `publishImportTaskOnce:365-445` | **02 缺陷** |
| GEN-010 | 无注入/路径穿越；JSON 坏数据走 500 | Jdbc L430-444 | 成立 |
| GEN-011 | 写路径 auditEventService.recordSuccess/Failure | Service 多处 | 成立 |
| GEN-012 | 导入 2000 节点/1000 顶点；发布 200 | L44-47,565-590 | 成立 |
| GEN-013 | 本模块无独立构建入口 | — | 无本面 |
| GEN-014 | 无迁移文件（M016） | — | 无本面 |
| GEN-015 | 004–006 中文句仍在；`Strings.requireText` 英键经 FieldLabels 出网 | Parser 中文；Service FIELD_LABELS | 成立 |
| GEN-016 | 本模块无文档完成声明 | — | 无本面 |
| GEN-017 | 本轮分类见 §6 | — | 已走四分类 |
| GEN-018 | 本报告证据为 HTML/命令/源行 | — | 本席遵守 |

### 4.4 PGM（36）

| ID | 适用 | 证据 | 结论 |
|---|---|---|---|
| PGM-001 治理层底座 | 空间/项目/时段属底座 | 锁 | 有上位 |
| PGM-002 治理顺序 | 无本面流程引擎 | 锁 | 无本面 |
| PGM-003 模块入口 | 无导航 | 锁 | 无本面 |
| PGM-004 综合体和集团 | project_id 归属 | 002 已修 | 成立 |
| PGM-005 空间和点位 | SpaceRecord 载体；补列 012 否 | 锁；SpaceRecord 无用途/负责人 | 维持 012 |
| PGM-006–016 | 商户/合同/员工/权限/终端/设备/商户侧/开放接口/留痕/楼宇/应急 | 本模块无对应实现或仅记录类 | 无本面/无发明 |
| PGM-017 综合体和集团 | 创建须已存在项目 | `validateCreateRequest:507-518` | 002 成立 |
| PGM-018–021 | 多对多/职责/主要功能/业务意义 | 无发明隶属链 | 无冲突 |
| PGM-022 空间和点位 | 上位；导入在 023 | 锁 L 空间节 | 有上位 |
| PGM-023 主要功能 | HTML：解析候选（铺位、摊位、公共区）→ 人工复核 → 发布；二次确认；不可静默覆盖 | 现行 HTML L472；八值含 shop/booth/public_area；发布追加新 shape 不改旧行；确认字段 011 否/XM | **发布子集状态机 02**；011 不升格 |
| PGM-026–027 | 他节主要功能/边界 | 无本面 | 无本面 |
| PGM-031 / 035 | 经销/提成 | 无本面 | 无本面 |
| PGM-040 单一功能授权 | 独立 permission 码 | Controller | 成立 |
| PGM-041 权限规则 | 「所有业务数据默认按项目隔离…只有项目之上的获授权岗位才可以跨项目汇总」 | 锁 L5796-5800；01 使「之上岗位汇总」在本模块不可达 | **本项支撑 FAIL** |
| PGM-042–044 / 048 / 060 | 职级/外包/桌台/商户侧/会员 | 无本面 | 无本面 |
| PGM-065 连接关系 | 平面图连空间 | 有 | 无冲突 |
| PGM-066 已拍板 | 不发明待排期项为已实现 | — | 无假完成 |

90/90 均有适用记录。顺从线本身在 01/02 上失败，故整席 FAIL。

---

## 5. 对抗式（grilling 结构，自问自答，不等操作员）

❓ 越权：项目员工能否读他项平面图？  
➡️ `ensureFloorPlanExists`→`checkProjectOwnership`：scope 存在且不等则 `CROSS_PROJECT_FORBIDDEN`（L601-609）。商户走 `checkMerchantOwnership`。**平台管理员无 org：quietly 抛 `PROJECT_SCOPE_REQUIRED`，连本项也读不到**（01）。

❓ 幂等：跨图同客户端键会不会改挂图形？  
➡️ `derivedId("fps_", floorPlanId + "_" + key)` 恒 36 字符（L268-269,653-655）。001 现态成立。

❓ 项目 scope：无商户有项目的员工？  
➡️ `currentMerchantIdQuietly` 空后走项目过滤（L75-79,512-518）。项目员工桩与生产：quietly 返回 of(projectId) 当 actor 有 organizationId。此支成立。

❓ 派生主键长度？  
➡️ 见 §2，36/43/39 ≤ 64。

❓ 发布上限？  
➡️ `candidateIds.size() > 200` → 中文 VALIDATION_FAILED（L588-589）。008 现态成立。

❓ 营业时段场地校验？  
➡️ 非 `spaces.id` → `VENUE_NOT_FOUND`；他项 → `CROSS_PROJECT_FORBIDDEN`（L63-68）。009 现态成立。无 org 管理员仍被 quietly 抛点挡住（01）。

❓ 错误文案中文？  
➡️ Parser 13 处中文；本模块 `FIELD_LABELS`+`requireField("平面图名称")`；`Strings.requireText` 英键经 FieldLabels。004–006 现态成立。

❓ 人工复核只发布部分候选？  
➡️ 第一次按 PGM-023「复核后发布」只勾选部分 `candidate_ids`：任务变 `published`，未选仍 `pending`，再次发布被「只有解析完成的导入任务才能发布」拒绝（02）。

❓ 解析器不校验闭集、非法 shape_type？  
➡️ 生产 `IdempotencyService` 事务包裹 `createImportTaskOnce`，非法类型 `normalizeAllowed` 抛错后回滚。013 原文「事务回滚，无半写」在生产仍成立。不新立项。

---

## 6. 已定稿 M015-001…014 复验（不重复立项）

| ID | 队列 | 本轮现态 |
|---|---|---|
| 001 派生主键超 64 | 否（已修） | derivedId 36 字符；候选 43。成立 |
| 002 project_id 必填且存在 | 否（已修） | `requireText`+`projectExists`→`PROJECT_NOT_FOUND`。成立 |
| 003 项目 scope | 否（已修） | **项目员工支成立；「平台管理员全量」生产未落地** → 新缺陷 01，不重开 003 号 |
| 004 Parser 中文 | 否（已修） | 源中文仍在。成立 |
| 005 本模块中文整句 | 否（已修） | FIELD_LABELS / 平面图名称 / 候选项列表。成立 |
| 006 去掉夹带英文键 | 否（已修） | 所列句子已纯中文。成立 |
| 007 诚实不分页 | 否（已修） | 三处 `pageSize=max(size,1), total=size`。成立 |
| 008 发布 ≤200 | 否（已修） | L588-589。成立 |
| 009 场地存在+项目过滤 | 否（已修） | findVenueProjectId + listByProject。成立 |
| 010 保存即生效 vs 草案化 | 否/XM | 实现仍保存即生效。不升格 |
| 011 发布无服务端确认字段 | 否/XM | DTO 仍无确认字段。不升格 |
| 012 spaces 用途/负责人 | 否/全局遗漏 | SpaceRecord 仍无此二列。不升格 |
| 013 摊位/公共区 | 否/XM→M016 | SHAPE_TYPES 八值含 booth/public_area。不升格 |
| 014 观察 | 否 | 营业时段不走 IdempotencyService、靠 UNIQUE(venue_id,day_of_week)；saveFloorPlan 不更新 merchant_id。不升格 |

---

## 7. 本轮问题（收齐后统一判定）

### GR-M015-B5r1-01 — 平台管理员全量通道是死代码；测试桩伪造 `currentProjectScopeQuietly`

- **文件:行 / 原句**
  - `FloorPlanService.java:72-79`：`merchantScope.isPresent() ? findAllFloorPlansByMerchant : currentProjectScopeQuietly().map(findAllFloorPlansByProject).orElseGet(findAllFloorPlans)`
  - `FloorPlanService.java:512-518`、`601-610`：创建与 `checkProjectOwnership` 同样调用 quietly
  - `VenueBusinessHoursService.java:42-47,65-68`：列表/保存同样调用
  - `AuthorizationService.java:289-294`：`requireProjectScope` — 「projectId == null \|\| isBlank → 抛 PROJECT_SCOPE_REQUIRED」
  - `AuthorizationService.java:504-507`：`currentProjectScopeQuietly` **原句** `return Optional.of(requireProjectScope(actor));`（名称「Quietly」与实现相反，且**不**对 `isPlatformAdminBridge` 放行）
  - `FloorPlanServiceTests.java:445-461`：注释「平台管理员桥（生产语义：…项目 scope 皆空、永不抛）」+ 桩返回 `Optional.empty()`
  - `VenueBusinessHoursServiceTests.java:89-94`：`Optional.ofNullable(projectScope)` 使 null 变 empty
- **正常触发（第一次就会撞）**：`accountType=platform_admin` 且 `organizationId` 空（`AccessRepository` 注释「Null organizationId = platform admin」）的主体，已通过 `floor_plan.view` / `governance.space.view` 后调用 `listFloorPlans` 或 `VenueBusinessHoursService.list` → 生产抛 `PROJECT_SCOPE_REQUIRED`，永远走不到 `findAllFloorPlans`/`listAll`。BSM-007「平台管理员拥有全部日常功能权限」、BEQR-152/PGM-041「只有项目之上的获授权岗位可以跨项目汇总」、B2-r2 非阻断「不把无组织全局员从现网全量改成 403」均被现实现违反。
- **四分类**：冲突？否（HTML 要求之上岗位可汇总，实现不是「多给了规则」而是没做到）。细化？**是**（BEQR-152 / PGM-041 / BSM-007 的下位实现错误）。不到技术性/添加。
- **已有判据**：BEQR-152、PGM-041、BSM-007、GEN-002、GEN-003、GEN-005。不新造判据。
- **证据类型**：1（HTML/锁原文）+ 4（源行并列；本席未跑 M006 测试类，`M006SecurityRegressionTests:146-150` 只作只读旁证：缺组织时 quietly 抛，不计入本模块 surefire）。
- **工程缺陷**：是。不复用 M015-003 号（003 已否=已修项目员工支；本条是 003 声明的「平台管理员全量」未在真 AuthorizationService 契约下落地）。
- **修复队列**：无 ID 且 003 已不在队列 → 回退须整批先 B2。

### GR-M015-B5r1-02 — 部分发布后任务直接 published，剩余 pending 无法再发

- **文件:行 / 原句**
  - `FloorPlanService.java:365-367`：`if (!"parsed".equals(task.status())) throw … "只有解析完成的导入任务才能发布"`
  - `FloorPlanService.java:369-389`：按请求 `candidateIds` 子集取 pending 候选
  - `FloorPlanService.java:422-435`：整任务 `status` 写成 `"published"`，**不**把未选候选改为 rejected，也**不**把任务留在 parsed
- **正常触发**：PGM-023「由人工复核候选与错误后发布」——操作员去掉错误候选、只提交部分 id（DTO 正是 `List<String> candidateIds`）。第一次成功后任务已 published；剩余 pending 再发布即 400。HTML 要求的复核子集发布在第二次无法完成。
- **四分类**：冲突？否（HTML 未写「一次必须发完全部」）。细化？**是**（PGM-023 发布写入正式区域的下位状态机）。不到技术性/添加。
- **已有判据**：PGM-023、GEN-009。
- **证据类型**：1 + 4（源行；指定 mvn 未覆盖该负例，故不以测试绿否定）。
- **工程缺陷**：是。无已定稿 ID。

### 观察（不入缺陷、不升 014）

- `ProjectRecord` 暂存 M009 字段 `ownerPerson`/`address`/`businessScope`：floorplan 包零引用；属 M009，不入本模块队列。
- `PlatformController` `PageResponse.of(list(),1,500)` 截断营业时段列表：控制器不在 M015 FILES。
- `operatorNote` 发布请求不落库：014 已观察。
- `saveFloorPlan` ON CONFLICT 不更新 `merchant_id`：014 已观察。
- 商户账号创建时跳过 project_id==scope（L512 `if merchant empty`）：商户是否持 `floor_plan.manage` 未在本模块证明，不为假设授权加需求。

无添加项。三 HTML 本身未审出须铁律 4 上报的损坏。

---

## 8. 三线收口

| 线 | 执行 | 结果 |
|---|---|---|
| 第一性原理 | 23 文件三问 + 90 定义全文（锁）+ 三 HTML 相关锚（BEQR-152、BSM-007/028、PGM-022/023/041、平面图 L472） | 01 证伪「quietly=empty=全量」 |
| 顺从式 | 90/90 有适用记录 | 01/02 未满足已映射判据 |
| 对抗式 | grilling 七问 + 部分发布 + 非法类型事务 | 01/02 |

---

## 9. 本席 B5 再执行结论

**FAIL**。

23 文件全覆盖；90 判据逐项核对；三线均执行；声称有四类证据。无未答 questions。无 HTML 修改申请。无锁外判据。哈希行有效。

未定稿工程缺陷 2 条（GR-M015-B5r1-01、GR-M015-B5r1-02），均无「有 ID 且仍在修复队列」可沿用，按 V0.17 回退统一规则整批先回 B2，禁止直接 B3。001–009 已修点不回潮为新 ID。010–014 维持否。本票具否决权，不等于双席合取完成。
