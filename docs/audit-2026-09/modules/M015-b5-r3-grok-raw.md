CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M015 B5-r3 整模块全量三线审计（Grok 席乙）

- 文件版本：V0.01（指定路径原先不存在，本次新建完整报告）
- 席位：固定 Grok 独立只读审计席乙（grok-4.6 / xhigh；不派子代理、不用 MCP、不问操作员、不读本次 `M015-b5-r3-self-raw.md`）
- 步骤：B5-r3（B3-r3 第 3 次上限修复、B4-r3 内容/归因双 PASS 之后的**整模块**全量三线重审；不是只看 diff；不可复用 B2/B4 票替代本轮取证）
- 正本：`/home/james/complex-management` main 主工作区
- 计划：`docs/audit-2026-09/plan/modules.md` V0.04 M015 同一 23 文件（floorplan 包 17 + governance 包 ProjectRecord / SpaceRecord / SpaceTypeRecord / VenueBusinessHoursRecord / VenueBusinessHoursRepository / VenueBusinessHoursService）
- 控制提示词：`.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17 B1/B5 及三线；`runbook.md` V0.02
- 判据锁：`docs/audit-2026-09/plan/criteria-lock.md` V0.11；本席独立实算（见本文第一行独占锁行）；不抄 sidecar / ledger / 派发期望值
- 映射：锁 L6394 `M015 → […]` 抽出 90 项、无重复、MISSING=[] EXTRA=[]（BEQR 22 / BSM 14 / GEN 18 / PGM 36）；定义区全文自锁读取，相关三 HTML 按锁「来源：path:line」对照现行文件，72/72 hit、miss=0；不新增判据、不 LOCK-AMEND
- 现态声称：FloorPlanService V0.04、VenueBusinessHoursService V0.02；after tree `3036d0c089905520aaa86fddb7a75eb75409b019`（`git cat-file -t` = tree）；本模块 23 文件工作区相对该 after **`git diff --name-only` 空**
- HEAD：`e456ba96c84185c8d39f18f548011daac48a6466`（B3 未提交；现态在工作区）
- 背景（只作边界，不代替本轮全量重审）：操作员 2026-09-20 裁定补派的 B5-r1 再执行本席 FAIL（GR-M015-B5r1-01/02）已定稿为 M015-015/016，B3-r3 修复、B4-r3 双 PASS。findings 现在 V2.52：001–016 全部否（001–009、015、016 已修；010/011 XM 同批、012 全局遗漏、013 XM、014 观察）。不把全部否当功能全实现；允许真实新证据或回归。本轮若仍 FAIL 即交操作员 `BLOCKED-3ROUNDS`，本席不擅加第 4 次、不修代码。
- 未读取：任何本次 `M015-b5-r3-self-raw.md`。可读旧 B1/B2/B4/B5（含 B5-r1 V0.02 FAIL 段、B4-r3、B1 B2-r3 追加段）仅作已定稿边界与历史分类，**本轮 23 文件 + 90 定义 + 三 HTML 全文重审**。
- 约束：只写本 raw；不修业务/HTML/锁/findings/ledger；不 fetch/commit/push/merge/deploy；不建脚本/日志/临时产物；只允许两席。

本次审计结论：**PASS**。无新增未定稿工程缺陷、无添加项、无待问 2.2、无锁外判据、无 HTML 修改申请。001–009 与 015/016 在当前 23 文件上独立复验现态成立；010–014 维持队列「否」，本席不升格。ProjectRecord 含 M009 在途暂存改动只登记跨模块观察。模块生命周期仍由席甲写 ledger；本票不等于双席 B5 或模块收口 PASS。

---

## 0. 技能自核（V0.17 §6，按环境实用强制技能如实记录；B5 收窄）

本席实际读取并按收窄口径使用（路径均 `test -f` 为 PRESENT）：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| using-superpowers | `~/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 先核技能再取证；用户指令优先于技能本体。禁止子代理 |
| using-arc | `~/.claude/plugins/marketplaces/howells/plugins/arc/skills/using-arc/SKILL.md` | 已读；`references/platform-tools.md` 本机不存在（记缺口）；Arc `audit` 会派子代理并另写 `docs/arc/audits/`，与双席/§7 冲突，**不调用** |
| graft | `~/.claude/skills/graft/SKILL.md` | `graft grep currentProjectScopeQuietly --in backend/.../access/` 定位授权真实现后再读源；不把 graft 当证据替代源读 |
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证；不等操作员、不派子代理 |
| grill-me | 同套件 `productivity/grill-me/SKILL.md`（本体转发 grilling，`disable-model-invocation: true`） | 内部 sharpen；按意图执行 |
| codebase-design | `.../engineering/codebase-design/SKILL.md` | 深度模块/缝：FloorPlanService 把授权桥藏在 `projectScopeOrAll()`，Controller 只 `requirePermission` |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（锁哈希三工具、HTML 行命中、mvn -Dtest、AuthorizationService:289-294/504-507）再下结论；B5 不修复 |
| tdd | `.../engineering/tdd/SKILL.md` | B5 不写测试；只核已有例是否能失败（015/016 生产语义桩） |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 给下游席甲合取读；完成标准=整模块审完、90 项逐条、三线落盘、结论二元 |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；未运行测试不写通过 |
| systematic-debugging | superpowers 同名 | 只借读错误→复现→追数据流；B5 不修、不加诊断桩 |
| ponytail / ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/` | B5 不落地修复；review 只列可删复杂度。本模块无新的「第一次正常跑就会撞上」的可删冲突 |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本步验证秒～十几秒级，未起 monitor |
| resolving-merge-conflicts | 本机 PRESENT | 无合并，不调用 |

B5 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、using-git-worktrees、executing-plans、dispatching-parallel-agents、subagent-driven-development。

缺口（不冒称调用）：

- `batch-grill-me`：`~/.agents/skills/batch-grill-me/SKILL.md` MISSING；mattpocock 树有 grilling / grill-me，无独立 batch-grill-me
- `ubiquitous-language`：本机技能树未找到
- Arc `references/platform-tools.md`：路径不存在
- Trail of Bits `audit-context-building`：会派 function-analyzer 子代理，**不调用**

明确不用：MCP、第三主体、修源/HTML/锁/台账。

graft 本轮 1 次：`graft grep currentProjectScopeQuietly` 报 tokens saved ≈ 60,213。

---

## 1. 锁、范围与源指纹

本席哈希命令（独立实算，未抄基准）：

```
sha256sum /home/james/complex-management/docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
python3 hashlib.sha256(Path(...).read_bytes()).hexdigest()
openssl dgst -sha256 docs/audit-2026-09/plan/criteria-lock.md
```

三工具同值。独占锁行仅本文第一行。

| 项 | 值 |
|---|---|
| 锁文件 | `docs/audit-2026-09/plan/criteria-lock.md` V0.11 |
| 字节 | 889020 |
| 行 `wc -l` | 6850 |
| python3 hashlib | 与 sha256sum 同值 |
| openssl dgst -sha256 | 同值 |
| sidecar `plan/criteria-lock.sha256` | 全文一行同值（事后对照，非抄入第一行） |
| ledger 顶部 `CRITERIA-LOCK-SHA256:` | 同值（事后对照） |
| findings | V2.52；SHA256 `f2ccaf6ced3d3f1a9a63802257cf89781333a35d71e51595b4bff9d3769ad4e4` |
| ledger | SHA256 `072253759d6298efc6c255ec88cea0ec8f4f20f44a4d16a063afa4c5b4e89aa6` |
| 映射抽出 | 90 条、unique 90、MISSING=[] |
| 锁内映射 HTML 来源行 vs 现行三 HTML | 72/72 hit=True miss=0（GEN 18 无 HTML 行） |
| 计划 FILES | 23 条与 modules.md V0.04 FILES-BEGIN/END 逐字相同 |
| 计划冻结有效内容行 | 1999；当前工作区 `\n` 计数 **2139**（B3 累计后现态，不改计划凑数） |
| questions | `questions/M015*` 计数 0 |
| after tree | `3036d0c089905520aaa86fddb7a75eb75409b019`；23 文件 `git diff --name-only` 空 |
| HEAD | `e456ba96c84185c8d39f18f548011daac48a6466` |

三 HTML 工作区 SHA（本席独立，未改）：

| 文件 | SHA256 |
|---|---|
| `docs/business-enum-quick-reference-v0.1.html` | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | `eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66` |
| `docs/platform-governance-map-for-operations-v0.1.html` | `a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1` |

### 1.1 23 文件覆盖表（全文阅读；行数/SHA 实算）

| # | 行 | SHA256 | 路径 | 版本头 |
|---|---:|---|---|---|
| 1 | 95 | `33e14ba8868dd24c5235ba55658e51a38c7ff52572d3cb59fd10c07849941bef` | `.../floorplan/FloorPlanController.java` | 无 |
| 2 | 21 | `9db3b6126ab0ca3e624e857684f68e680d7c99f9fe3e0ef6cf8a31080d373bce` | `.../floorplan/FloorPlanRecord.java` | 无 |
| 3 | 51 | `9c5e50bff229838722ac00cc9096a8a188b111731be90da291a25c1f43c986a7` | `.../floorplan/FloorPlanRepository.java` | V0.01 |
| 4 | 689 | `5cad799d091fd43b58d99ff05d12e10afbc16e63fb7e59857dcd94c042ccdd80` | `.../floorplan/FloorPlanService.java` | V0.04 |
| 5 | 454 | `bb3fed48fb09410e7532c6930e7d3b2a3c83080490782cd2d04736b03cf502f3` | `.../floorplan/JdbcFloorPlanRepository.java` | V0.01 |
| 6 | 23 | `7bf4056cf8b920718b377272334b32f7d64c76f329078042f9e77bdedb065a33` | `.../floorplan/FloorPlanImportCandidateRecord.java` | 无 |
| 7 | 14 | `5b0011b08d106da9a797ecd6d9915d665460c8823c03c50140b68354a11ae8d5` | `.../floorplan/FloorPlanImportDetailsResponse.java` | 无 |
| 8 | 14 | `6cfdb0787b4f3b6a4b785106e9f89f26b366d5b25a0fe326f9c9f6032b6b4e92` | `.../floorplan/FloorPlanImportErrorRecord.java` | 无 |
| 9 | 35 | `3315458ff08fb345a499a6fba09a296a671e92a6b07a717f8634ce0faab1666b` | `.../floorplan/FloorPlanImportParseResult.java` | 无 |
| 10 | 303 | `9c8fb4eef4fbe0a4b3dfbe1aaa74392f756e6b290a3e7aebb7d05fc3596e2ccb` | `.../floorplan/FloorPlanImportParser.java` | V0.01 |
| 11 | 19 | `164d55ea7a81e8099c1ad8f44ef07bd3b3e474dfe7fb9f9d3f69893ac423a270` | `.../floorplan/FloorPlanImportTaskRecord.java` | 无 |
| 12 | 7 | `cc32d4e81cc6e58813c676f5ebfeeff86e2e48d4ea360ad2183843ffd661b6f5` | `.../floorplan/FloorPlanPoint.java` | 无 |
| 13 | 22 | `4cfece0c8c5a9f13f2171b0d667eb29c109fe6cc3c9bfdf58590a575607e168f` | `.../floorplan/FloorPlanShapeRecord.java` | 无 |
| 14 | 16 | `0bc12e7e268a7d8904645c560027f48262a330412cada0f44b0d7da47bef8625` | `.../floorplan/dto/FloorPlanCreateRequest.java` | 无 |
| 15 | 10 | `7ecd9fca8fab9864815285d809a18375c6532c5036990599d11b12f3a99598a8` | `.../floorplan/dto/FloorPlanImportCreateRequest.java` | 无 |
| 16 | 13 | `ae890f5f0877b2b3d120d626434b63a0158cffa87f75afa6fb503ef8f2359859` | `.../floorplan/dto/FloorPlanImportPublishRequest.java` | 无 |
| 17 | 18 | `b890d08f0bfb9e5e1675dc4d2f84a5b21e5f59427b39d59053653e545e07e20d` | `.../floorplan/dto/FloorPlanShapeSaveRequest.java` | 无 |
| 18 | 25 | `3d18793172d90f57cd5296ae76f2d15983a61a60a5ff93b24dd8bd1697284487` | `.../governance/ProjectRecord.java` | V0.01 |
| 19 | 22 | `f13f27eaf2dbdb754f18d4f841f1ebfd515d973e5863624583deab8adac7eac1` | `.../governance/SpaceRecord.java` | 无 |
| 20 | 15 | `388191c191993bac3ae452374d001a2236f5245a41257b6b9e2c6e7f649b7bd1` | `.../governance/SpaceTypeRecord.java` | 无 |
| 21 | 29 | `ec04b55b8b3c747b9623450bd7460e00b6a585b2d2d0f8485f80cfc59def6957` | `.../governance/VenueBusinessHoursRecord.java` | 无 |
| 22 | 119 | `f4557789c8eaa6cd0f5a8be883c2a32568264324fb2191848df33623ec02a6ba` | `.../governance/VenueBusinessHoursRepository.java` | V0.01 |
| 23 | 125 | `149c1eec1650ada8cf9a447bf3fd7117683e71c1a882dea84680d2134082fd5e` | `.../governance/VenueBusinessHoursService.java` | V0.02 |
| Σ | **2139** | | | |

23/23 全文覆盖。无 OVERSIZE-SINGLE-FILE。无抽样宣称整文件 PASS。

---

## 2. 可执行证据（类型 2 / 4）

### 2.1 命令（未 `source` `/etc/complex/complex.env`）

逐行读取该 env 的 `COMPLEX_DATABASE_URL` / `COMPLEX_DATABASE_USERNAME` / `COMPLEX_DATABASE_PASSWORD` 三键后 `export`；URL 将库名改为 `complex_test`。`env | awk -F= '/^COMPLEX_DATABASE_/'` 仅这三键。未导出其它 COMPLEX_*。未连生产库、未写 DDL。

本席转换时曾用 `${url%/complex}` 剥后缀；若原 URL 以 `/complex` 结尾会暂时变成无库名 JDBC。**Flyway 实日志**（类型 2）：

```
org.flywaydb.core.FlywayExecutor : Database: jdbc:postgresql://127.0.0.1:5432/complex_test (PostgreSQL 16.14)
o.f.core.internal.command.DbValidate : Successfully validated 395 migrations
o.f.core.internal.command.DbMigrate : Current version of schema "public": 427
o.f.core.internal.command.DbMigrate : Schema "public" is up to date. No migration necessary.
```

证明本次 JDBC 目标是 `complex_test`，不是生产库名 `complex`。

然后：

```
mvn -o -q --file backend/pom.xml -Dtest=FloorPlanControllerTests,FloorPlanImportParserTests,FloorPlanJdbcIntegrationTests,FloorPlanServiceTests,VenueBusinessHoursServiceTests -Dsurefire.failIfNoSpecifiedTests=false test
```

**exit code = 0**（命令末 `echo MVN_EXIT=$?` 打印 `MVN_EXIT=0`）。

### 2.2 Surefire XML 计数（含 @Nested；不以 .txt 摘要替代）

路径：`backend/target/surefire-reports/TEST-*.xml`（本次运行 2026-09-19T22:25 时间戳）。

| 类 | tests | failures | errors | skipped | nested/total testcase |
|---|---:|---:|---:|---:|---|
| FloorPlanControllerTests | 14 | 0 | 0 | 0 | 0/14 |
| FloorPlanImportParserTests | 14 | 0 | 0 | 0 | 0/14 |
| FloorPlanJdbcIntegrationTests | 7 | 0 | 0 | 0 | 0/7 |
| FloorPlanServiceTests | 39 | 0 | 0 | 0 | 0/39 |
| VenueBusinessHoursServiceTests | 14 | 0 | 0 | 0 | **1/14**（`$RealRepositoryCases.findVenueProjectIdAndListByProjectHitRealSpacesTable`） |
| **合计** | **88** | **0** | **0** | **0** | |

对照：`VenueBusinessHoursServiceTests.txt` 写 `Tests run: 0`——JUnit 把 @Nested 记在子 classname 上，txt 摘要对顶层记 0。**以 XML 为准**。

测试通过本身不替代行为证据（GEN-003）：下面三线仍按源码 + HTML 原文 + 授权真实现核对。015/016 相关例名：`platformAdminWithoutOrganizationSeesAllFloorPlans`、`staffWithoutProjectScopeIsRejected`、`platformAdminWithoutOrganizationListsAllVenues`、`publishRemainingPendingCandidatesAfterPartialPublish`、`importTaskWithOnlyErrorsIsFailedAndCannotBePublished`。

---

## 3. 第一性原理三问（逐文件簇）

依据类型：1=HTML 原文，2=可执行产物，3=外部规范原文，4=可复现命令。不接受「历史审过 / 测试绿所以对 / 席位意见」。

### 3.1 FloorPlanController（L1–95）

1. **为什么必须存在？** PGM-023 L472「商场平面图网页导入…发布到正式平面图」+ BSM-028「建立综合体项目、楼层、区域、点位」需要 HTTP 入口。删掉则后台无法调列表/创建/图形/导入。上位：PGM-023 / BSM-009。
2. **声称是否执行？** 8 个端点均 `authorizationService.requirePermission(...)` 后转 Service；写路径 `requireIdempotencyKey`。ControllerTests 14 例 XML 全绿（类型 2）：无权限 → PERMISSION_DENIED；缺幂等键 → VALIDATION_FAILED。
3. **未验证前提？** 权限点是否在角色模板授出属 M031，本模块只校验调用了 requirePermission。不把「前端藏按钮」当授权。

### 3.2 FloorPlanService V0.04（L1–689）— 本模块行为核心

1. **为什么必须存在？** 把平面图 CRUD、导入解析落库、分批发布、项目/商户隔离做成可调用行为。删掉则 PGM-023 导入/发布、BEQR-152 项目隔离在本包无实现。
2. **声称是否执行？** 见 §5 grilling与 §6 015/016 复验；ServiceTests 39 例 + Jdbc 7 例 XML 全绿。
3. **未验证前提？** （a）生产路径 `IdempotencyService.runAndComplete` 在注入 `transactionTemplate` 时包裹 action（`IdempotencyService.java:167-172`，非本模块文件，类型 4 读源）；单测路径 template 可为 null，属测试缝。（b）`currentProjectScopeQuietly` **不会**因平台管理员返回 empty——这是 M006 红线④，本模块用 `isPlatformAdmin` 桥，见 §6。

### 3.3 JdbcFloorPlanRepository + FloorPlanRepository（L1–454 / L1–51）

1. **为什么必须存在？** 持久化 mall_floor_plans / floor_plan_shapes / import_* ；`findAllFloorPlansByProject` 是 BEQR-152 行级通道。接口 default 空实现带注释：零调用会被 tenant-isolation 门禁当作无隔离能力（类型 4 读注释 L14–16）。Jdbc 覆盖了 SQL 谓词。
2. **声称是否执行？** JdbcIntegrationTests 7 例连 `complex_test` 跑查询/约束。`projectExists` = `SELECT COUNT(1) FROM projects`。
3. **未验证前提？** 迁移 CHECK/NOT NULL 在 M016（V423 等同批）；本文件不发明列。`saveFloorPlan` ON CONFLICT **不更新** `merchant_id`（L92–102）——014 已观察，首次创建写入后粘滞，第一次正常创建会写 merchant_id，不构成半写。

### 3.4 FloorPlanImportParser + ParseResult/Candidate/Error/Task/Details/Point/Shape + 四个 DTO

1. **为什么必须存在？** PGM-023「系统解析出候选区域（铺位、摊位、公共区），由人工复核候选与错误后发布」。Parser 把 svg/standard_json 变成候选+错误；DTO 是 HTTP 体。
2. **声称是否执行？** ParserTests 14 例；错误文案中文（M015-004）：如 `多边形至少需要 3 个顶点`、`SVG 必须包含宽高为正数的 viewBox`。SHAPE 默认 `shop`（铺位）；booth 由 data-shape-type / JSON 传入，服务层 `SHAPE_TYPES` 八值含 booth/public_area（M015-013 同批，现态 L35）。
3. **未验证前提？** 解析器本身不校验 SHAPE 闭集，非法类型在 `createImportTaskOnce` `normalizeAllowed` 整单失败。生产经幂等事务则回滚；不把「解析器宽容」升格为新需求。二次确认字段仍不在 Publish DTO（010/011 已 XM 否）。

### 3.5 VenueBusinessHoursService V0.02 + Record + Repository

1. **为什么必须存在？** 服务注释 L20–22：入场资格按场次核销与当天无限权益靠 `venue_business_hours` 划场次；无配置则核销拒。上位 BSM-001 策略参数可配（时刻）+ 基础管理场地。删掉则运营只能改库。
2. **声称是否执行？** list/save 走 `projectScopeOrAll()`；场地须 `spaces.id`（`findVenueProjectId`）；开歇同刻拒；跨零点放行。VenueHours 测试 XML 14 例全绿。
3. **未验证前提？** 「保存即生效、is_draft=停用」与 BSM-001 钱类配置须草案/转正 **冲突**——已定稿 M015-010 XM 否，本席不升格、不单模块改语义。save 不走 IdempotencyService，靠 V226 `UNIQUE(venue_id, day_of_week)` last-write-wins（014）。Controller 的 `requirePlatformScope` 在 M009 `PlatformController.java:233-245`（非本 23 文件）；服务层自身对商户无二次闸，生产入口在控制器。第一次正常走 Controller 会被 `requirePlatformScope` 拒商户（类型 4 读 M009 源，登记观察不发明本模块新闸）。

### 3.6 ProjectRecord / SpaceRecord / SpaceTypeRecord

1. **为什么必须存在？** PGM-017「所有基础对象都要带综合体项目归属」；PGM-023「记录空间面积、用途、状态、负责人和所属商户」；空间类型字典是 createSpace 硬校验数据源（SpaceTypeRecord 注释）。本模块只有记录类，CRUD 在其它模块。
2. **声称是否执行？** 记录类无行为。SpaceRecord 仍无「用途」「负责人」字段——M015-012 全局遗漏已否。ProjectRecord 现含 `ownerPerson/address/businessScope` + 旧 10 参兼容构造（相对 HEAD 有暂存 diff）——**M009 在途**，floorplan 包 0 引用，本模块不裁决。
3. **未验证前提？** 不把记录类缺列当本模块可修缺陷（012 口径）。不把 M009 字段当本轮 B5 回归。

---

## 4. 顺从式：90 项逐条（适用 / 证据）

判定：适用=本 23 文件实现或必须不与该锚点冲突；不适用=本文件无该域行为，核对无发明/无冲突即可。证据类型 1/2/4。不新增判据。

### 4.1 BEQR（22）

| ID | 适用 | 证据 |
|---|---|---|
| BEQR-063 高危动作（酒吧反结/沽清等） | 不适用 | 本包无酒吧账单。平面图发布高危二次确认属 PGM-023，后端确认字段 XM=M015-011 否。无冲突。类型 1 |
| BEQR-074 隐私桌台 | 不适用 | 无桌台权限码。无冲突。类型 1 |
| BEQR-143 四类主体 | 不适用 | 无经营/合同/结算主体绑定实现。ProjectRecord 非四类主体。类型 1 |
| BEQR-144 经营主体关系 | 不适用 | 无运营公司多对多实现。类型 1 |
| BEQR-145 城市项目公司 | 部分/记录 | ProjectRecord 表示综合体项目档案形状；无「不管理酒吧…」行为。无冲突。类型 1 |
| BEQR-146 运营公司职责 | 不适用 | 无域公司职责代码。类型 1 |
| BEQR-147 主要收入归属 | 不适用 | 无票款/租金记账。类型 1 |
| BEQR-148 合作模式 | 不适用 | 无合同合作模式枚举写入。类型 1 |
| BEQR-149 职级 | 不适用 | 无 M1–M10。类型 1 |
| BEQR-150 终端十一端 | 不适用 | 无终端开关。类型 1 |
| BEQR-151 最终权限计算 | 部分 | Controller/营业时段入口只 `requirePermission`；最终权限算法在 AuthorizationService（非本模块）。本模块不另发明权限公式。类型 4 |
| **BEQR-152 项目与敏感数据权限** | **适用** | HTML：「所有数据默认按项目隔离，普通岗位不得跨项目查看；只有项目之上的获授权岗位可以跨项目汇总。」实现：`projectScopeOrAll()`（Service L616-621 / Hours L57-62）平台管理员 empty=全量，其余 `currentProjectScopeQuietly()`；商户列表优先 `currentMerchantIdQuietly`。测试：项目员工只见本项目、跨项目 403、无归属员工 403、平台管理员全量。类型 1+2。见 §5/§6 |
| BEQR-153 终端开关优先级 | 不适用 | 无终端开关。类型 1 |
| BEQR-154 综合体项目状态 | 部分/记录 | 项目六态不在 FloorPlanService；平面图自身状态闭集 `active/inactive/archived`（L33）是图形对象状态，不是项目状态，未把项目六态塞进平面图。类型 1+4 |
| BEQR-155 商户/运营主体状态 | 不适用 | 无商户状态机。类型 1 |
| BEQR-156 合同状态 | 不适用 | 无合同。类型 1 |
| BEQR-157 员工状态 | 不适用 | 无员工档案写路径。类型 1 |
| BEQR-158 设备状态 | 不适用 | 无设备。类型 1 |
| BEQR-159 权限状态 | 不适用 | 无权限对象状态。类型 1 |
| BEQR-166 合同审批链 | 不适用 | 无合同审批。发布高危审批 XM=011。类型 1 |
| BEQR-170 铺位租赁状态 | 不适用 | 平面图 shape_type≠租赁六态；铺位是图形类型 shop。租赁状态在 contract 仓（014）。无把租赁六态写入 SHAPE_TYPES。类型 1+4 |
| BEQR-189 管理费方式 | 不适用 | 无服务费。类型 1 |

### 4.2 BSM（14）

| ID | 适用 | 证据 |
|---|---|---|
| BSM-001 整体说明/策略可配 | 部分 | 营业时刻属策略参数；本服务保存即生效 vs 「涉及钱须草案转正」=010 XM 否。平面图本身非税率/提成。类型 1 |
| BSM-003 通用能力总述 | 适用（框架） | 分岗：Controller 权限点；可改：upsert/归档状态。引导新建属前端 M018。类型 1+4 |
| BSM-004 配置存档删除审批 | 部分 | 平面图 `archived` 状态可写；无本模块「申请删除+最高权限审批」流程。不发明物理 DELETE（Jdbc 无 DELETE SQL）。类型 4 |
| BSM-005 分步引导新建 | 不适用（本段后端） | 无 UI。API 仍一次 POST。不冲突。类型 1 |
| BSM-006 动态二维码 | 不适用 | 无核销码。类型 1 |
| **BSM-007 平台管理员日常功能** | **适用** | 平台管理员须能用平面图/营业时段日常功能。015 修复后 `isPlatformAdmin`→全量，不再 403。资金/敏感价不在本模块自动授予。类型 1+2 |
| BSM-009 基础管理 | 适用 | 楼层/空间/点位入口在本包。类型 1+4 |
| BSM-028 主要功能+红线④⑤ | 适用 | 建立楼层区域点位：创建平面图+图形。红线④看不到别的项目：projectScopeOrAll + checkProjectOwnership。红线⑤重复提交不重复生效：IdempotencyService.execute。类型 1+2 |
| BSM-029 和谁有关 | 适用 | 空间/楼层图给票务酒吧游乐提供「发生在哪」的几何；本包维护该几何。类型 1 |
| BSM-072 商户授权范围内使用 | 部分 | 商户带 merchantId 时列表按商户过滤（L73-77）；PGM-048 商户表未列平面图为必开能力，本模块不把商户做成平台全局。类型 1+4 |
| BSM-074 和谁有关（商户章） | 不适用/弱 | 无商户档案写。无冲突。类型 1 |
| BSM-114 各类入口 | 适用 | 后台 API 按权限+项目/商户过滤。类型 1+4 |
| BSM-115 相互关系 | 适用 | 平面图连空间/项目。类型 1 |
| BSM-120 已确认规则 | 部分 | 全中文报错见 GEN-015；本模块 005/006 已改的整句仍在。类型 1+4 |

### 4.3 GEN（18）

| ID | 适用 | 证据 |
|---|---|---|
| GEN-001 必要性 | 适用 | §3 三问；无纯转发空类。Repository default 空方法有门禁理由。类型 4 |
| GEN-002 可执行正确性 | 适用 | mvn exit 0；Flyway complex_test；015/016 行为与注释一致。类型 2 |
| GEN-003 测试有效性 | 适用 | 015 桩只覆盖 `currentActor()`，`isPlatformAdmin`/`currentProjectScopeQuietly` 走真 AuthorizationService（ServiceTests L508-526；Hours L90-104）。伪造「quietly 永不抛」已不在。016 例先发 1 再发剩余。类型 2+4 |
| GEN-004 信任边界 | 适用 | project_id 必填且 `projectExists`；venue 须 spaces 行；导入 payload 长度/节点/顶点上限；candidate 须属任务且 pending。类型 4 |
| GEN-005 授权最小权限 | 适用 | 见 §5 四类主体。默认拒绝：无项目员工 403 PROJECT_SCOPE_REQUIRED。类型 2+4 |
| GEN-006 完整性 | 适用 | 派生主键 `prefix+sha256[0:32]`=36 字符（L664-665）；发布多写在生产幂等事务内。Hours 单条 UPSERT 原子。类型 4 |
| GEN-007 并发幂等 | 适用 | 写路径 idempotencyKey；Hours 靠 UNIQUE(venue_id,day_of_week)。重复发布同一 pending 候选第二次拒「只有待处理的候选项才能发布」。类型 2 |
| GEN-008 一致性 | 适用 | SHAPE_TYPES 八值与 M016 V423 CHECK 同闭集（注释 L34-35）；发布 max 200 与契约。candidate_ids 中文不经 FieldLabels。类型 4 |
| GEN-009 错误与恢复 | 适用 | failed 任务不可发（L367-368 + 测试 importTaskWithOnlyErrorsIsFailedAndCannotBePublished）；分批发布不再把任务锁死（016）。类型 2 |
| GEN-010 安全 | 适用 | 无注入：命名参数 JDBC；无明文秘密。跨商户 `CROSS_MERCHANT_FORBIDDEN`。类型 4 |
| GEN-011 可观测 | 适用 | 成功/失败 auditEventService.recordSuccess/Failure。类型 4 |
| GEN-012 资源边界 | 适用 | payload 1e6 字符、候选 2000、顶点 1000、发布 200；PageResponse 诚实 pageSize=total（L90）。类型 4 |
| GEN-013 构建依赖 | 适用 | `-o` 离线 mvn 可跑。类型 2 |
| GEN-014 迁移 | 不适用本 23 文件 | 无 SQL 文件。服务层与已迁移表对齐；破坏性迁移不在本模块。类型 4 |
| GEN-015 文案中文 | 适用 | 005/006 点名的出网整句仍为中文（候选项列表不能为空、平面图名称不能为空、多边形至少需要 3 个顶点、导入内容超过 100 万字符上限、场地不能为空、016 文案「只有解析完成或已发布的导入任务才能继续发布」）。`Strings.requireText` 仍可能产生 `field is required`（公共类非本模块）；本模块 `requireField`/`FIELD_LABELS` 覆盖点名键。不把公共 Strings 升格为本轮 FAIL。类型 4 |
| GEN-016 文档可验证 | 不适用本 23 文件 | 无本模块 md。类型 4 |
| GEN-017 四分类 | 适用 | 本 raw 每条问题按冲突→细化→技术性→添加。无添加项。类型 4 |
| GEN-018 第一性证据 | 适用 | 本文件结论只落 1/2/3/4。类型 4 |

### 4.4 PGM（36）

| ID | 适用 | 证据 |
|---|---|---|
| PGM-001 界面全中文（治理原则） | 部分 | 与 GEN-015 同；解析器 error_message 中文。类型 1+4 |
| PGM-002…016 治理其它章（组织/审批/留痕等总则） | 部分/不适用 | 本包不实现职级模板/U 盘密钥/导出脱敏。留痕：审计事件。无与原文冲突的发明。类型 1 |
| PGM-017 综合体项目归属 | 适用 | 创建必填现存 project_id（L509-513）；记录带 projectId。类型 1+2 |
| PGM-018 物理项目与运营公司 | 不适用 | 无多对多运营公司代码。类型 1 |
| PGM-019 各主体职责 | 不适用 | 无收入主体代码。类型 1 |
| PGM-020 项目档案主要功能 | 部分 | ProjectRecord 含负责人/地址/经营范围字段（M009 在途）；本模块不写这些列。类型 4 观察 |
| PGM-021 业务意义 | 适用 | 项目隔离避免多综合体数据混。类型 1+4 |
| PGM-022 空间和点位总述 | 适用 | 楼层图影响合同/卡座/容量等——本包提供几何。类型 1 |
| **PGM-023 主要功能（导入发布）** | **适用** | HTML L472：「解析出候选区域（铺位、摊位、公共区），由人工复核候选与错误后发布…不可静默覆盖既有布局证据。」八值含 shop/booth/public_area；发布追加不删已有图形（`publishImportTaskKeepsExistingShapes`）；分批发布 016。二次确认仍 XM=011。用途/负责人仍 012。类型 1+2 |
| PGM-026/027/031/035 | 部分/不适用 | 无票务核销/酒吧低消实现。类型 1 |
| PGM-040 权限总述 | 适用 | 单一功能权限点 floor_plan.view/manage/import.*。类型 4 |
| **PGM-041 权限规则（项目隔离）** | **适用** | 与 BEQR-152 同文。「数据的项目与商户隔离不提供关闭开关」：无开关关闭隔离。类型 1+2 |
| PGM-042 职级规则 | 不适用 | 无职级绑定。类型 1 |
| PGM-043 外包与审计 | 部分 | 外包若无项目归属则 403；不自动全量。无审计只读专口（权限在模板）。类型 4 |
| PGM-044 桌台挂单经销 | 不适用 | 无。类型 1 |
| PGM-048 商户侧授权范围 | 部分 | 商户不能经 Hours 入口（Controller requirePlatformScope，非本文件）；平面图商户只见本 merchant_id。类型 1+4 |
| PGM-060 会员生物信息 | 不适用 | 无。类型 1 |
| PGM-065 连接关系 | 适用 | 治理连接楼宇：空间/点位。类型 1 |
| PGM-066 已拍板 | 部分 | 全链可点击属前端；全中文见 GEN-015；对象带综合体归属见 PGM-017。类型 1 |

90/90 已记录。无锁外主张。

---

## 5. 对抗式 grilling（四类主体 × 读写；授权真实现）

追问树前置事实（AuthorizationService，非本模块但为本模块前提，类型 4 读源）：

- `requireProjectScope` L289-294：`organizationId` 空或空白 → 抛 `PROJECT_SCOPE_REQUIRED`「此操作必须在项目范围内进行」。
- `currentProjectScopeQuietly` L504-507：`Optional.of(requireProjectScope(actor))`；注释「红线④：不因平台管理员身份返回空（全量）」。
- `isPlatformAdmin` L279-280 = `isPlatformAdminBridge` L509-511：`active` 且 `accountType=platform_admin`。
- `requirePlatformScope` L482-498：平台管理员直接 return；商户 merchantId 非空拒绝。
- `currentMerchantIdQuietly` L440-450：有 merchantId 返回之，**不因平台管理员改全量**；异常则 empty。

本模块桥：`projectScopeOrAll()`（FloorPlanService L616-621，VenueBusinessHoursService L57-62）：

```
if (authorizationService.isPlatformAdmin(authorizationService.currentActor())) {
    return Optional.empty(); // 全量
}
return authorizationService.currentProjectScopeQuietly();
```

### 5.1 平台管理员（无 organizationId，accountType=platform_admin）

| 路径 | 真实现 | 现态 |
|---|---|---|
| 列表平面图 | merchant quietly 空 → projectScopeOrAll empty → `findAllFloorPlans` | 可达全量。测试 `platformAdminWithoutOrganizationSeesAllFloorPlans` |
| 读单图/shapes/imports | checkMerchantOwnership 空过；checkProjectOwnership：scope empty 不 403 | 可达 |
| 创建平面图 | L514-520：merchant 空则 projectScopeOrAll.ifPresent 比对；empty 不限制项目，但仍要 projectExists | 可在任意现存项目下创建 |
| 导入/发布 | 经 ensureFloorPlanExists 同上 | 可达 |
| 营业时段 list/save | projectScopeOrAll empty → listAll；save 不 ifPresent 拒 | 可达全量。测试 `platformAdminWithoutOrganizationListsAllVenues` |

**015 回归检查：** 若仍直接 `currentProjectScopeQuietly()`，本主体第一次正常 GET list 即 403。现态已绕开 quietly、用 isPlatformAdmin。**现态成立。**

平台管理员**带** organizationId：仍 isPlatformAdmin → 全量（跨项目汇总，BEQR-152「项目之上的获授权岗位」）。不视为缺陷。

平台管理员**带** merchantId：`currentMerchantIdQuietly` 先命中，列表按商户切、**不**走全量。这是 quietly 商户语义（源注释 L438），要同时具备平台管理员+商户号才发生，非常态账号；按铁律 6 不升格。登记观察。

### 5.2 项目员工（staff，organizationId=prj_1，无 merchantId）

| 路径 | 现态 |
|---|---|
| 列表 | projectScopeOrAll → quietly 返回 prj_1 → `findAllFloorPlansByProject` |
| 读他项目图 | checkProjectOwnership → `CROSS_PROJECT_FORBIDDEN`「平面图属于其他综合体项目」 |
| 创建他项目 | L515-518 → 同码「只能在当前综合体项目下创建平面图」 |
| 营业时段 | listByProject；save 他项目场地 `CROSS_PROJECT_FORBIDDEN`「场地属于其他综合体项目」 |

测试：`listFloorPlansFiltersToProjectScope`、`accessingFloorPlanBelongingToDifferentProjectIsForbidden`、`createFloorPlanOutsideProjectScopeIsForbidden`、Hours `listFiltersByProjectScope` / `rejectsVenueOutsideProjectScope`。**003 不回潮。**

### 5.3 商户员工（merchant_staff，merchantId 非空，测试夹具 organizationId=null）

| 路径 | 现态 |
|---|---|
| 列表平面图 | merchant quietly 命中 → `findAllFloorPlansByMerchant`，**不调用** projectScopeOrAll，故 quietly 抛点达不到 |
| 读他商户图 | checkMerchantOwnership → `CROSS_MERCHANT_FORBIDDEN` |
| 读本商户图（即使项目不同） | 商户闸过；checkProjectOwnership 在 merchant present 时 **return**（L605-607） |
| 创建 | 跳过项目匹配（L514）；merchant_id 写入当前商户（createFloorPlanOnce L232-247） |
| 营业时段 | 服务无商户闸；生产入口 PlatformController `requirePlatformScope` 拒商户（非本 23 文件） |

第一次正常：商户只看本商户平面图。用请求体填写其它 `project_id` 可通过存在性校验（003 最小处置明确商户优先、不在本批加商户-项目绑定表）。不升格为新 FAIL（会发明绑定需求）。观察。

### 5.4 无归属员工（staff，organizationId=null，merchantId=null，非 platform_admin）

| 路径 | 现态 |
|---|---|
| 列表平面图 | merchant 空 → projectScopeOrAll → quietly → **403 PROJECT_SCOPE_REQUIRED** |
| 读单图 | checkProjectOwnership 同抛 |
| 营业时段 list/save | 同抛 |

测试：`staffWithoutProjectScopeIsRejected`（平面图+Hours）。红线④保持：缺归属不放行全量。**现态成立。**

### 5.5 分批发布 / 重复发布 / 幂等 / 派生主键 / 上限 / 中文

| 主题 | 追问 | 现态 | 判据 |
|---|---|---|---|
| 分批发布 | 人工只发 subset 后剩余 pending 能否再发？ | L367-368 接受 parsed **或** published；候选须 pending。测试 `publishRemainingPendingCandidatesAfterPartialPublish`：2 候选先发 1 再发 1，任务仍 published，图形 2，候选均 published | PGM-023 / GEN-009 / M015-016 |
| 重复发布同一候选 | 第二次同 candidate | 非 pending →「只有待处理的候选项才能发布」。`publishImportTaskRejectsAlreadyPublishedTask` | GEN-007 |
| failed/cancelled | failed 再发 | L367-368 拒，文案「只有解析完成或已发布的导入任务才能继续发布」。`importTaskWithOnlyErrorsIsFailedAndCannotBePublished`。cancelled 无写路径（014） | GEN-009 |
| 幂等 | 同键重放 | IdempotencyService.execute；冲突 IDEMPOTENCY_CONFLICT。Hours UNIQUE last-write-wins（014） | GEN-007 |
| 派生主键 | 跨图同客户端键 | `derivedId("fps_", floorPlanId+"_"+key)` = 前缀+sha256 前 32 hex，36 字符 < VARCHAR(64)。slug 仅用于候选/错误短号 | GEN-006 / M015-001 |
| 上限 | 发布 201 / payload / 节点 | L590-591 200；L558-559 100 万字符；L569-571 2000 节点；L575-577 1000 点 | GEN-012 / M015-008 |
| 中文 | 016 文案 | L368 整句中文，无「只有解析完成的导入任务才能发布」旧句 | GEN-015 / PGM-066 |
| 覆盖已有布局 | 先有图形再发布 | 追加写入，不删旧。`publishImportTaskKeepsExistingShapes` | PGM-023「不可静默覆盖既有布局证据」 |

### 5.6 ponytail-review（只列，不修）

- `FloorPlanRepository.findAllFloorPlansByProject` default 返回空列表：有隔离门禁注释，**不可删**。
- Hours `upsert` 每次生成新 UUID 但 ON CONFLICT 不改 id：浪费一次 UUID，非缺陷。
- `cancelled` 任务状态在闭集无写路径：014 观察，不发明取消 API。

`Lean already` 于本轮 B5 可修面。net: -0 作为本模块缺陷。

---

## 6. 已否 ID 独立复验（不把否当全实现；查回归与新证据）

| ID | 队列 | 本轮独立现态 |
|---|---|---|
| M015-001 | 否（已修） | `derivedId` L664-665 前缀+sha256[0:32]；图形/导入任务 ID 含平面图域。无回归 |
| M015-002 | 否（已修） | project_id `requireText` + `projectExists`；缺/未知项目分别 VALIDATION_FAILED / PROJECT_NOT_FOUND。无回归 |
| M015-003 | 否（已修） | 商户优先 + 项目 scope；平台管理员全量改由 015 的 isPlatformAdmin 桥落地，003 项目员工支仍在。无回归 |
| M015-004 | 否（已修） | Parser 中文 error_message（全文阅读 L36-127 等）。无回归 |
| M015-005 | 否（已修） | 候选项列表/平面图名称/取值无效中文整句，不经 FieldLabels。无回归 |
| M015-006 | 否（已修） | 多边形/导入上限/场地/营业时间中文。无回归 |
| M015-007 | 否（已修） | PageResponse pageSize=max(size,1)、total=size（L90/106/144）。无回归 |
| M015-008 | 否（已修） | MAX_PUBLISH_CANDIDATES=200（L47, L590-591）。无回归 |
| M015-009 | 否（已修） | 场地须 spaces；项目过滤 JOIN。无回归 |
| M015-010 | 否（XM） | Hours 仍保存即生效、is_draft=停用（L24-26, L90）。**维持否，不升格** |
| M015-011 | 否（XM） | PublishRequest 仍无确认字段（dto L4-12）；服务无确认闸。**维持否** |
| M015-012 | 否（全局遗漏） | SpaceRecord 仍无用途/负责人（L8-21）。**维持否** |
| M015-013 | 否（XM 已落地白名单） | SHAPE_TYPES 八值含 booth/public_area（L35）。**维持否（迁移属 M016）** |
| M015-014 | 否（观察） | Hours 无 IdempotencyService；cancelled 无写路径；operatorNote 创建平面图不落库。**维持否** |
| **M015-015** | **否（已修，B3-r3）** | 见 §5.1–5.4。五处均 `projectScopeOrAll()`。测试桩生产语义。**复验成立，无回归** |
| **M015-016** | **否（已修，B3-r3）** | L366-368 parsed\|published；文案「只有解析完成或已发布的导入任务才能继续发布」；分批测试绿；failed 仍拒。**复验成立，无回归** |

### 6.1 跨模块观察（非本票 FAIL）

- **ProjectRecord** 相对 git HEAD 增加 `ownerPerson/address/businessScope` 及 10 参兼容构造；属 M009 在途暂存。floorplan 包零引用。after tree 已包含该 blob。本模块不裁决、不当 015/016 回归。
- 商户创建时可提交任意现存 project_id（003 设计）。
- 平台管理员兼 merchantId 时列表被商户切开（AuthorizationService 商户 quietly 语义）。
- `Strings.requireText(..., "floor_no")` 等仍可能出网 `floor_no is required`（公共模块）。005/006 定稿点名键已中文。不升格。

无新的「第一次正常跑就会撞上」的工程缺陷。无添加项。无 HTML 申请。

---

## 7. 问题清单与四分类

本轮收齐后的缺陷票：**空**。

（若有问题须含：席位临时编号、文件:行、原句、正常触发、四分类顺序、已有判据、证据类型 1/2/4。本次无。）

观察（工程缺陷否，不进修复队列）：见 §6.1 与 010–014 维持。

---

## 8. 三线执行核对

| 线 | 执行 |
|---|---|
| 第一性原理 | §3 对 23 文件簇回答三问；依据 1/2/4 |
| 顺从式 | §4 90/90 逐项适用+证据 |
| 对抗式 | §5 grilling 四主体读写、分批/重复发布、幂等、派生主键、上限、中文；授权追到 AuthorizationService L289-294/440-450/482-511/504-507 |

全部文件审完、全部问题收齐后统一结论，未中途转修。

---

## 9. 结论

**PASS**

条件核对：

- 23/23 全文覆盖表
- 90/90 映射判据已核对
- 三线均执行
- 可执行声称有本次 mvn 证据（exit 0，XML 88/0/0/0，库 `complex_test`）
- 无未答 questions
- 无未定稿缺陷
- 第一行锁哈希为本席独立实算
- 未读本次 self-raw
- 015/016 按授权真实现复验成立，001–009 无回归，010–014 不升格
- 无添加项、无锁外判据、无 HTML 修改

本票是席乙单席 B5-r3。双席合取与四值校验由席甲在两份本次完整 raw 封存后进行。本席 FAIL 具否决权——本席不行使否决。
