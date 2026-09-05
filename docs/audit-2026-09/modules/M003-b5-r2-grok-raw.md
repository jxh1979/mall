CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M003 B5-r2 Grok 独立全量审计（修复后整模块重审）

版本：V0.01  
席位：Grok（乙），只读  
步骤：B5-r2（B3 已执行次数 = 2）  
模块：M003 `01-root-config-and-guidance-01` / 共 322  
控制：V0.17  
ledger：M003 `IN_PROGRESS`，B3=2，方案复核已修 2，子代理失败重跑 0  
结论：**PASS**  
临时 GR 缺陷：**无**（不分配 GR-01 起的 FAIL 号）  
未答 questions：无（M003-023 首个非空行=`删除`；M003-024 首个非空行=`保留`）  
三份 HTML：只读对照，未改、未申请改  
本席未读 `M003-b5-r2-self-raw.md`、未读本次分析/原始 JSON、未读其他席 B5 报告；未派子代理；未扩判据/阶段/模块；除本文件外无写入。

---

## 0. 技能预检（Grok 席自核）

按提示词 §6 在本运行环境核路径，存在则用收窄口径，缺则记缺口。本体「问人 / 派子代理 / 写 §7 外文件 / 无 ID 改码」一律作废。

| 技能 | 本环境 | 本次用法 |
|---|---|---|
| `using-superpowers` | 存在 `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工自检 |
| `verification-before-completion` | 存在 | PASS 前实跑命令，不以旧结论替代 |
| `grilling` / `grill-me` | 存在 | 对抗式追问结构；事实自读；不等操作员、不派子代理 |
| `batch-grill-me` | **缺口**：mattpocock 技能树无此目录 | 不冒充已调用 |
| `ubiquitous-language` | **缺口** | 不冒充已调用 |
| `codebase-design` | 存在 | 只借「删除测试 / 接口是否过宽」；不改架构、不扩判据 |
| `diagnosing-bugs` | 存在 | 只借「先建可红信号再下结论」；B5 禁止加探针/改码 |
| `systematic-debugging` | 存在 | 读错误→复现→追数据流；不改生产代码 |
| `ponytail-review` | 存在 | 找可删除；本模块当前无可再删的已证缺陷 |
| `ponytail` | 存在 | 仅 B3 修复强度；本步不修 |
| `tdd` / `test-driven-development` | 存在 | 文档模块不强制造测试；未造假测试 |
| `writing-for-agents` | 存在 | 本步不写收口 `M003-*.md` |
| `requesting-code-review` / `receiving-code-review` | 存在 | B5 是整模块重审不是 B4 diff；不引入第三席 |
| `resolving-merge-conflicts` | 存在 | 本步无合并冲突 |
| `git-guardrails-claude-code` | 可选，存在于多宿主 skills 目录 | 本步无 git 写 |
| `i-have-adhd` | 存在 `.../i-have-adhd/0.1.0/skills/i-have-adhd` | 报告无寒暄；**第一行被控制锁哈希格式覆盖** |
| `brainstorming` / `writing-plans` | 存在 | 阶段 A 技能，本 B5 不用 |
| 划掉的 skill（executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、research、ponytail-audit/debt/gain 等） | 按提示词不用 | 未调用 |

---

## 1. 输入清单（本席实算）

### 1.1 判据锁

独立命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`  
结果：`23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`  
sidecar `plan/criteria-lock.sha256`：同值  
ledger 顶部 `CRITERIA-LOCK-SHA256:`：同值  
`LOCK-AMEND-COUNT:`：0  
M003 映射（criteria-lock.md:6383）：`[GEN-001 … GEN-018]` 共 18 条。本席自读 `## general` GEN-001–GEN-018 全文。不重开 M001/M002，不引用 HTML 派生判据作为本模块映射外的新闸。

### 1.2 模块范围（plan/modules.md）

```
.env.example
.gitignore
.semgrepignore
AGENTS.md
CONTEXT.md
PROGRESS.md
README.en.md
README.md
```

`git ls-files -z -- <8 paths> | xargs -0 cat | wc -l` = **621**  
`xargs -0 cat | wc -c` = **37807**  
modules.md 历史有效行 625；当前工作区实算 621（与派发声明一致）。

| 路径 | 换行行数 | 字节 | SHA256 |
|---|---:|---:|---|
| `.env.example` | 22 | 893 | e377b54d9cd829ddeed30e0ae730f55d51fbc8c272f3df5ca611fdbdba125049 |
| `.gitignore` | 55 | 1199 | 75861ded5d3859bcda593aafe3f3862e765470e7f8b6636a643bf59de9d7c51b |
| `.semgrepignore` | 6 | 253 | 45f3dbc9f36891b4b94d2b641fabfd66db81df59b642bcde80a29bece24f1aba |
| `AGENTS.md` | 74 | 3695 | ea470f256c8e9f1e23e9b346d1b3828f2e4e1edd84e9ea83d7564725759bfcf0 |
| `CONTEXT.md` | 163 | 10197 | d5acdb1c3eb174c532983c823931d1cbe36444712e379b755991243f75df52b7 |
| `PROGRESS.md` | 36 | 3246 | 91eaedfc0c8428514355b82a79bd1b6add257bc80269e831d2e76aafbc0baf9d |
| `README.en.md` | 116 | 7470 | dd83a2943d401ec25bfa2043ef4762662c78d1526763eb3faf2cc20008d092db |
| `README.md` | 149 | 10854 | 28a46aea13227b0e3e101f9dc0a2970b6e3e581ca5995d291288aa59cb2d23b5 |
| **合计** | **621** | **37807** | — |

`git status --porcelain`（仅 8 文件）：`.gitignore` / `.semgrepignore` 无改；其余 6 个 ` M`（B3-r1+r2 工作区未提交修复，属本模块当前事实）。`HEAD=origin/main=df580198172b2c497ef3cb7e25d2e8e5dc058920`。

### 1.3 台账与问题

- `docs/audit-2026-09/findings.md` V0.40：26 个 M003 ID，修复队列全部为否。
- `questions/M003-023.md`：`## 操作员答复` 首个非空行精确 `删除`。
- `questions/M003-024.md`：`## 操作员答复` 首个非空行精确 `保留`。
- 三份 HTML 只作业务声明逐字上位。检索限定 8 源文件 + 三 HTML + 必要直系引用（`backend/pom.xml`、`package.json`、`apps/web/package.json`、`application.yml`、`application-local.yml`、`LocalKeyVault.java`、`AuthController.java`、`SecurityConfig.java`、`ContractService.java`、`predeploy-gates.yml`、`run-backend-maven.mjs`、`.git/hooks/pre-push`、列名路径存在性）。未全仓 rg，未读 `docs/audit-2026-09/modules` 其他 raw。

### 1.4 本批已声明修复（须在整模块中重验，不以 B4 PASS 替代）

- JPA → 当前 JDBC（README / README.en）
- 中文例外收成 HTML 封闭四样（AGENTS.md）
- 两个 README 删除 GuestWeb / 客人 Web 边

---

## 2. 真实命令与输出（类型 4；未跑的不写 PASS）

### 2.1 锁哈希

```
$ sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
$ cat docs/audit-2026-09/plan/criteria-lock.sha256
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
```

### 2.2 基线块

```
$ node tools/refresh-entry-doc-baseline.mjs --check
[entry-doc-baseline] PASS: 快照与实际一致（720 ops / V385（363 迁移））。
exit 0
```

Python 实算：`backend/src/main/resources/db/migration` glob `V*.sql` → 363 个文件，最大 V=385；`docs/openapi/openapi-p0.json` 方法计数 → 720。`backend/target/surefire-reports` **不存在**。

### 2.3 Playwright 列表（只 list，不宣称执行测试）

```
$ npx playwright test --list
...
Total: 47 tests in 9 files
exit 0
```

README 不写死 E2E 数，声明以该命令为准。本席跑通 list；**未跑** `verify:ui` / 真实后端 E2E / `mvn test`。

### 2.4 技术栈直系引用

`backend/pom.xml`：`<java.version>21</java.version>`；parent `spring-boot-starter-parent` **4.1.0**；`<springdoc.version>3.0.3</springdoc.version>`；**有** `spring-boot-starter-jdbc`、`spring-boot-starter-webmvc`、`spring-boot-starter-security`、`spring-boot-starter-validation`、`spring-boot-starter-actuator`、`spring-boot-flyway`、`flyway-core`、`flyway-database-postgresql`、`aspectjweaver`；**无** `spring-boot-starter-data-jpa`；全文 `jpa`/`hibernate` 计数 0。AOP 注释写明离线 `.m2` 无 `starter-aop`，用 `aspectjweaver` 触发切面。

`apps/web/package.json`：react ^19.0.0、vite ^8.0.16、antd ^5.22.5、typescript ^5.7.2、vitest ^4.1.9。  
根 `package.json`：`engines.node >=22`（本机 `node v24.20.0`）；`@playwright/test` ^1.61.0。下列脚本键均存在：`dev:web` `typecheck` `build` `verify:unit` `verify:backend-env` `test:backend` `dev:backend` `verify:openapi` `verify:p0-contract` `verify:tenant-isolation` `verify:delivery-gates` `verify:production-blockers` `verify:ui` `verify:ui:real-backend:seeded` `verify:closed-loop-runtime` `verify:migration-version-order` `verify:predeploy`。  
`verify:delivery-gates` 含 `verify:migration-version-order`。`verify:predeploy` **不含** `verify:migration-version-order`（与 AGENTS「未挂 verify:predeploy」一致）。

### 2.5 `verify:backend-env`（环境失败 ≠ 文档缺陷，也不当 PASS）

```
$ java -version
openjdk version "21.0.12.1" 2026-08-18 LTS
$ echo JAVA_HOME=$JAVA_HOME
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
$ test -d "$JAVA_HOME" → JAVA_HOME_DIR_MISSING
$ npm run verify:backend-env
[backend-env] Maven 版本检查执行失败。
- The JAVA_HOME environment variable is not defined correctly,
exit 1
```

无 `backend/mvnw`。`tools/run-backend-maven.mjs` 先找 wrapper 再回退系统 `mvn`（与 README「优先 Wrapper、回退系统 Maven」一致）。当前宿主 `JAVA_HOME` 指向不存在目录，Maven 检查失败。这是本机环境，不是 8 文件内容错误。**不把 Maven/后端测试写成已通过。**

### 2.6 忽略与扫描排除

`git check-ignore --no-index -v --stdin`：`.env` `.env.local` `node_modules/` `dist/` `backend/target/` `coverage/` `.vite/` Gradle/Kotlin/`*.class`/`*.apk`/`local.properties`/playwright 报告/`test-results`/Claude settings/`*.tsbuildinfo`/semgrep 滚动日志/`docs/_archive/**/*.log`/`.loop-engineering/`/根调试 glob/`backend/data/bar-images/`/`evidence-files/` 均命中对应 `.gitignore` 行。  
`.env.example`：`git check-ignore` exit 1（不被忽略）。`docs/audit-2026-09/findings.md`、`AGENTS.md` 不被忽略。

`.semgrepignore` 四条路径均 EXISTS。`ops/keyvault/` 7 个文件 `bash -n` 全部 exit 0。**未跑 semgrep 规则扫描，不宣称已扫描通过。**

### 2.7 目录 / 链接 / 类 / 钩子

目录树声称路径（`backend/` `apps/web/` `apps/android-terminal/` `docs/openapi/` `ops/160/` `tools/` `tests/e2e/` `security-scans/` `package.json` `playwright.config.ts` `playwright.real-backend.config.ts` `backend/src/main/resources/db/migration`）均存在。`database/migrations` **不存在**（树中已无此项）。`apps/api` 不存在（与「已物理移除」一致）。`tools/*.mjs` = **78**（树中已无「79 个脚本」）。

README.md 三个 markdown 链接均 EXISTS。README.en.md 三个 markdown 链接均 EXISTS。读者路径所列 `docs/*` 文件（含 `v099-v124-feature-implementation-and-acceptance-spec-v0.1.md`）`test -e` 全部 EXISTS。AGENTS 指针 `docs/agents/issue-tracker.md` `triage-labels.md` `domain.md`、`ops/160/propose-change.sh` `merge-pr.sh` 均 EXISTS。

`.git/hooks/pre-push` 可执行；正文拒绝直推 `refs/heads/main`（除非 `ALLOW_MAIN_PUSH=1`）。

`GuardedAction.java` / `AdmissionGuard.java` / `ActionGateAspect.java` 存在。`AuthController.java:24` `@PostMapping("/login")` **无** `@GuardedAction`。`SecurityConfig.java:118` 将 `"/api/v1/auth/login"` 列入公开路径。

`ContractService.java:522/1357` 有 `settleTerminationIfNeeded`。PROGRESS 所列 Java/TS 路径及 `JdbcFinanceRepository.java` 均 EXISTS。`TerminationSettlementMathTests.java` 存在；**本席未跑这些测试，数字不当当前绿。**

### 2.8 环境变量接线（直系引用，非全仓）

在 `backend/src/main|test`、`apps/web/src`、`apps/android-terminal`、`tools`、`ops` 内按文件名后缀窄搜：

| 变量 | 命中 |
|---|---|
| `COMPLEX_API_PORT/HOST`、`COMPLEX_DATABASE_*`、`COMPLEX_API_VERSION`、`COMPLEX_API_BUILD_TIME` | `application.yml`（及部分 test/tools） |
| `P0_ADMIN_USERNAME/PASSWORD` | `application-local.yml`、`AuthService.java`、Android smoke/tools |
| `COMPLEX_KEYVAULT_PII_KEY` | `application.yml:109` `pii: ${COMPLEX_KEYVAULT_PII_KEY:complex-dev-pii-encryption-key-change-me}` |
| `SPRING_PROFILES_ACTIVE` | 源码字符串 0 次；Spring Boot 标准环境变量（类型 3：框架官方绑定），激活 `application-local.yml` |
| `ACCESS_TOKEN_TTL_SECONDS` 等已删五变量 | **当前 `.env.example` 无** |

`LocalKeyVault.java:36-40`：`environment.getProperty(property, "")` 再经 `SigningSecretGuard.requireUsableHmacSecret`。yaml 默认仅在**未设置**时生效；显式空串会覆盖默认。当前 `.env.example` **不再赋值**该键。

### 2.9 8 文件针探（修复回归）

在 8 文件内扫描：`ACCESS_TOKEN`/`AUTH_FAILURE`/`starter-data-jpa`/`Data JPA`/`GuestWeb`/`GUEST→WEB`/`admin/customer`/`L4`/`自然人`/`十类终端`/`浅克隆`/`role_platform_admin`/`领域权威`/`兑换SKU`/`database/migrations`/`枚举值、字段名` 作为现行规则 → **均无现行命中**。  
现存相关命中均为已修口径：AGENTS 四样例外；CONTEXT 十一类+客人无网页端；README JDBC/`GuardedAction` 典型路径；历史快照句中的 V124；英文读者表里的 `v099-v124-...md` 文件名（文件存在）。

---

## 3. 第一性原理线（2.3 三问 × 8 文件）

判定顺序全程：冲突 → 细化 → 技术性 → 添加。省略 HTML 节点本身不新造冲突。

### 3.1 `.env.example`（22 行）

**必要性：** 给本地/安装提供可复制的进程、数据源、构建元数据与 local 种子约定。删掉后三份 HTML 无一条做不到（HTML 不规定 env 样例）。分类：**技术性**（无业务发明：不定义票种/价格/角色权限规则；`P0_ADMIN` 只接线已有 local 种子，角色名来自 HTML 但不在本文件发明审批或权限）。  
**声称行为：** `SPRING_PROFILES_ACTIVE=local` 激活 local 种子；DB 三变量被后端读取；PII **不设置**才用 dev 默认；`P0_ADMIN_*` 留空则不创建、禁止生产。证据：`application-local.yml:16-17`、`application.yml` 数据源与 `keyvault.keys.pii` 默认、AuthService 消费 P0_ADMIN。类型 **4**（加类型 3：Spring 标准 profile 变量）。  
**未核前提：** 未在运行中的 Spring 进程里注入空串/未设置做一次启动对照；凭 yaml 默认与 LocalKeyVault 源码。不把未启动后端当行为 PASS。  
**段落分类：** L1 版本头技术性；L2–13 进程/数据源/元数据技术性；L15–16 PII 注释技术性（无空赋值，无变量名——见观察，不升缺陷）；L18–22 local 种子技术性。无冲突/添加。

### 3.2 `.gitignore`（55 行）

**必要性：** 阻止构建产物、密钥、测试报告、本机配置入库。删掉会污染纳入清单，但不使 HTML 业务条款失效。**技术性**，无业务状态机。  
**行为：** `git check-ignore` 见 §2.6，模式有效。类型 **4**。  
**未核：** 未用真实 `.env` 文件做一次误提交演练。  
**分类：** 全文件构建/密钥/测试/本机/运行产物忽略，技术性无缺陷（M003-016）。无冲突。

### 3.3 `.semgrepignore`（6 行）

**必要性：** 把 semgrep 解析器不能处理的 OpenAPI/归档/keyvault heredoc 排除出该扫描器。**技术性**。  
**行为：** 路径存在；`bash -n` 脚本语法 0。未跑 semgrep AST/规则。类型 **4**。  
**未核：** semgrep 是否仍对 yaml/bash 报解析错（017 已登记，不把未扫描当通过）。  
**分类：** 技术性无已证新缺陷。

### 3.4 `AGENTS.md`（74 行）

**必要性：** 代理在本仓库的铁律。删掉不影响客人购票等 HTML 条款本身，但 L42–56 是 HTML 解释权/软删除/中文展示的操作化。  
**行为：**  
- L25–35 worktree/禁直推/fetch：`.git/hooks/pre-push` 实文件拒绝直推 main；`propose-change.sh`/`merge-pr.sh` 存在。类型 4。  
- L44–48 三份 HTML 终权：与 PGM:974 解释权声明同向。类型 1。细化。  
- L52–53 配置类主数据软删除 + 账户注销走 HTML 专项：对 BSM:567/570/862，不再用「一律/唯一例外人脸」覆盖注销。类型 1。曾冲突已修（003）。  
- L55–56 展示中文例外四样：与 PGM:287/299/965 逐字同一闭集。类型 1。曾冲突已修（025）。  
- L63–67 迁移 V 号闸在 `verify:delivery-gates`、未挂 `verify:predeploy`：package.json 实链证实。类型 4。  
- L69–74 测试三 DB 变量 / `complex_test` / `MAVEN_ARGS=-DskipTests`：工程约束，技术性。  
**未核：** 未实跑 `verify:migration-version-order`、未实推 hook。  
**分类：** 铁律 1–4、9–11 技术性或工程流程；5–7 细化 HTML；8 存量不兼容为工程约束（技术性，无业务实体发明）。无现行冲突。无浅克隆失实句（004 已修）。

### 3.5 `CONTEXT.md`（163 行）

**必要性：** 词表。删掉不自动删除 HTML 规则，但本文件声称「只收录已被 HTML 或用户裁决明确的术语」。  
**行为（类型 1，HTML 原文）：**  
- L6–12 三份 HTML 终权：PGM:974。细化。005 的「三份领域 Markdown 压过 HTML」已不在。  
- L14–38 平台/项目公司/业态主体：PGM:415 多对多非固定隶属；PGM:420/433 城市项目公司资产职责、不管理各域票务；PGM:428 娱乐平台公司只负责酒吧域；PGM:429 独立酒吧商户与代收代付服务费。006/007/023（L4 已删）已修。  
- L47–51 会员：BEQR:65 手机号唯一、禁止宣称同一自然人；BSM:898 / BEQR:89 会员注册免费。008 已修。  
- L53–69 成长值/积分/待追回：BEQR:70/75。细化。  
- L71–75 零售参考价可配窗口默认三十天+前延：BEQR:80。009 已修。  
- L77–81 内部结算价=项目商品/服务：BEQR:54。020 已修，不再限兑换 SKU。  
- L83–87 积分结算中心可为非独立公司：BEQR:75/85。010 已修。  
- L89–93 营销费用承担主体：BSM:1725 登记赞助+分别锁预算。024 细化，操作员保留，无缺陷。  
- L95–100 四类主体：BEQR:226、PGM:497。细化。  
- L102–107 功能权限点 vs 职级、显式拒绝最高：PGM:643/655/669。细化。  
- L109–114 临时授权：PGM:656。细化。  
- L116–121 高危五类、上一级/上两级/最高、禁自审：PGM:652 允许分档要求；PGM:660 不由发起人选档、系统补链。正文**没有**「发起人选档」。019 维持细化/无缺陷。  
- L123–135 外包/审计：PGM:673–677。细化。  
- L137–149 十一类终端+客人无网页端+拉取失败业务写关、留痕例外：PGM:734–739。011 已修。  
- L151–156 操作留痕：PGM 留痕条款。细化。  
- L158–163 存档逐条不审批、批量/整店须审批、删除最高权限、禁自审：BSM:566/569/570。012 已修。  
**未核：** 未跑会员/结算代码。词表不是可执行模块。  
**分类：** 全文有 HTML 上位者为细化；无现行明文相反。无添加项残留（L4 已删）。

### 3.6 `PROGRESS.md`（36 行）

**必要性：** 单分支历史建设台账。删掉不影响 HTML。**技术性**（工作日志，不发明新主体）。  
**行为：** 所列源路径存在；`settleTerminationIfNeeded` 存在。L1/34–36 标明「历史建设进度台账」「测试数字不代表当前工作区」。类型 4。022 已修时态。  
**未核：** 未跑 32/15/7 那些历史测试；不当当前绿（GEN-016/003）。  
**分类：** 技术性无新缺陷。

### 3.7 `README.md`（149 行）

**必要性：** 仓库入口。命令/栈/基线须可复现（GEN-016）。业务定位句「单综合体起步」有 PGM:412「当前一个项目，未来能看集团」上位 → 细化；技术栈/脚本 → 技术性。  
**行为：**  
- L18–19 JDBC / starter-jdbc / 无 Data JPA：与 pom 一致。018 组件事实已修。类型 4。  
- L29–50 目录树：路径存在，无 `database/migrations`、无 79 脚本。013 已修。  
- L59–93 脚本名：package.json 全有。**未把这些脚本跑通当 PASS。**  
- L101–108 基线：`--check` 720/V385/363；表外说明「现有 surefire 报告」=生成器快照，目录实际不存在。021 已修。  
- L135–136 `STAFF → WEB(业务人员)` + `STAFF → ANDROID`，无 Guest/customer Web。026 已修。与 PGM:739「客人无网页端」同向。类型 1+4。  
- L143 受保护业务写的**典型**路径 + 登录按自身逻辑：与 AuthController 无 GuardedAction、SecurityConfig permitAll login 一致。015 已修。  
**未核：** 未跑 delivery-gates / 后端测试 / 前端 build。  
**分类：** §1 业务概览细化；§2–5、§9 技术性；§6–7 文档指针技术性（链接存在）；§8 架构图细化十一端/员工 Web。无冲突。

### 3.8 `README.en.md`（116 行）

**必要性：** 英文入口；自声明以中文 README 为维护基线。  
**行为：** L16 JDBC 非 Data JPA；L90 `STAFF → Web (staff)`；L33 `admin web`；L80–82 转引中文 §5 与 `--check`/`playwright --list`，无 V124/386 当**当前**基线（014 已修）。类型 4。  
**未核：** 同中文，未跑全部门禁。英文读者表反引号是文档名而非仓库根路径；且声明中文为权威基线。不把省略 `docs/` 前缀升为缺陷（非明文相反、非已证工程断裂）。  
**分类：** 与中文对称。无 GuestWeb。无冲突。

---

## 4. 顺从式：18 GEN 逐项

假定当前实现意图正确，核映射的 18 条。每条：适用面 / 证据 / 结果。

| ID | 适用 | 核对 | 结果 |
|---|---|---|---|
| GEN-001 必要性与最小性 | 8 文件均问「删掉 HTML 哪条做不到」 | 死变量/空 PII 赋值/L4/GuestWeb/JPA 误称/过宽架构句已删；剩余均有用途或 HTML 上位 | 满足 |
| GEN-002 可执行正确性 | 文档声称的入口 | 脚本键存在；基线 `--check` 0；playwright `--list` 0；verify:backend-env 因宿主 JAVA_HOME 失败，不记源缺陷也不记命令 PASS | 文档入口存在；可执行全路径未在本席环境跑通 |
| GEN-003 测试有效性 | 本模块无测试文件 | 不造假测试；PROGRESS/README 测试数不当成本次执行 | 豁免（文档）+ 未把未跑测试当 PASS |
| GEN-004 信任边界 | `.env.example` 输入 | 样例不再用空 PII 覆盖默认；生产注释要求 ≥32 且不含 change-me；P0 注明禁生产 | 满足样例层 |
| GEN-005 授权与最小权限 | AGENTS 删除审批、CONTEXT 高危/终端、README 典型写路径 | 与 BSM/PGM 最高权限/禁自审/不由发起人选档/登录公开一致；不把文档写成每个 POST 都 GuardedAction | 满足 |
| GEN-006 数据完整性 | PROGRESS 幂等键叙述 | 只作历史说明；未跑 DB 约束 | 无现行虚假完成声明 |
| GEN-007 并发与幂等 | 同上 | 未执行并发 | 不把未跑当 PASS |
| GEN-008 接口与模式一致性 | README 栈 vs pom/package；中英 README | JDBC/Java21/Boot4.1.0/springdoc3.0.3/React19/Vite8/antd5/TS5.7/Vitest4/Playwright1.61 一致；Flyway 构件 `spring-boot-flyway` 在 pom:56 | 满足已核字段 |
| GEN-009 错误处理 | CONTEXT 开关拉取失败；PII 空值 | 留痕例外保留；样例不再制造空密钥 | 满足文档层 |
| GEN-010 安全与敏感信息 | gitignore `.env`；样例密码；PII | `.env` 忽略、`.env.example` 跟踪；local 种子有禁生产注释。明文样例口令属本地约定，先前 018 已登记无缺陷，本次无新泄漏路径 | 满足既有口径 |
| GEN-011 可观测 | 基线 surefire 措辞；PROGRESS 时态 | 021/022 澄清仍在 | 满足 |
| GEN-012 资源边界 | gitignore 构建产物 | 忽略规则有效 | 满足 |
| GEN-013 构建配置 | pom/package/CI fetch-depth/maven runner | fetch-depth:0；无浅克隆谎言；wrapper 优先逻辑在 runner 中 | 满足文档/配置声称 |
| GEN-014 迁移 | AGENTS V 号串行 | 脚本存在且挂在 delivery-gates、不在 predeploy；本模块无 SQL | 满足其文档声称 |
| GEN-015 界面与中文 | AGENTS L55–56；CONTEXT 十一端 | 四样例外=PGM287/965；适用后台/客人端/设备/安卓。本模块无 UI 渲染，未做对比度/读屏 | 文案约定满足；未冒充 UI 实测 |
| GEN-016 文档可验证 | 路径/命令/版本/完成态 | 链接与基线数字可复现；历史绿数已标历史；无 GuestWeb/JPA 假栈 | 满足已核项 |
| GEN-017 四分类 | 全文件 | 见各段；无把业务发明标成技术性；024 有 BSM1725 上位故细化；023 添加已删 | 满足 |
| GEN-018 证据 | 本报告 | 结论落到 HTML 原文或命令输出；不用「B4 已 PASS」「上一轮我判过」 | 本席遵守 |

顺从式结论：映射 18 GEN 均已核对；无未答问题；无锁外新判据主张。

---

## 5. 对抗式（grilling 结构，自答事实）

Q1 客人是否仍被连到 Web？  
现行 README mermaid 只有 STAFF→WEB(业务人员)/Web(staff)。8 文件无 GUEST→WEB、admin/customer web。CONTEXT:146 与 PGM:739 同为「客人无网页端」。**不成立冲突。** 目录「管理端」=员工 Web，不能反证客人网页端。

Q2 中文例外是否仍大于四样？  
AGENTS:55 仅单据号、设备编号、二维码内容、系统内部标识符。与 PGM:287/965 闭集相同。无「枚举值/字段名」例外。**025 已修。**

Q3 README 是否仍写 Data JPA？  
无 `Data JPA`/`starter-data-jpa`。pom 无 JPA。**018 已修。** AOP 用 aspectjweaver 而非 starter-aop：能力真实（ActionGateAspect 存在），不构成假栈。

Q4 高危是否仍写成发起人选档？  
CONTEXT:119 写分档来源+禁自审，无选择界面。PGM:652 允许上一级/上两级/最高；660 禁止发起人选档。**019 不升冲突。**

Q5 `.env.example` 是否仍导出死认证变量或空 PII？  
五变量已不在文件中。PII 无 `KEY=""`。yaml 未设置才用默认。**001/002 已修。** 注释写「该变量」未写出 `COMPLEX_KEYVAULT_PII_KEY`：yaml 有正式名；不把省略升为缺陷。

Q6 英文是否仍把 V124/386 当当前基线？  
§5 转引中文自动块；`--check` 720/V385/363。V124 仅出现在历史文件名与中文「旧基线」句。**014 已修。**

Q7 目录是否仍指向不存在的 `database/migrations` 或 79 脚本？  
否。tools mjs=78 且树中不再写死该数。**013 已修。**

Q8 「每个写动作都经 GuardedAction/JDBC」是否还在？  
已改为典型受保护路径+登录例外。AuthController 登录无注解。**015 已修。**

Q9 surefire 缺失时是否仍暗示本轮跑过测试？  
表外说明已写快照口径；目录不存在。**021 已修。** 本席 `--check` 只验证 ops/迁移，不验证 862 tests。

Q10 PROGRESS「32 绿」是否像 main 现状？  
L34–36 历史时态。**022 已修。** 未跑测试。

Q11 L4 / 自然人唯一 / 十类终端 / 兑换 SKU 限定 / 固定 30 天 / 必须法律主体 / 领域 Markdown 上位 / 存档一律不审批 / 浅克隆 / role_platform_admin 是否还在？  
8 文件针探无现行句。**005–012、020、023、004、003 已修。**

Q12 营销词条是否发明 HTML 没有的赞助/分锁预算？  
BSM:1725 原文有。操作员保留。**024 细化成立。**

Q13 未抄全 HTML 是否该报冲突？  
按控制：只有明文相反才记录。CONTEXT 未逐字抄「并管理经营关系」「填写生日」等，属省略，**不新造冲突。**

Q14 可删除项（ponytail-review）？  
已删死变量、空 PII 赋值、L4、Guest 边、JPA 误称。剩余忽略规则/词表/入口均有用途。`Lean already` 于本模块当前 8 文件。不把「还能更短」当 FAIL。

Q15 `verify:backend-env` 失败是否证明 README 撒谎？  
命令存在且被调用；失败原因是本机 `JAVA_HOME` 目录不存在，而 `java -version` 仍为 21。GEN-002 要求声称可执行，不要求审计席宿主 Maven 一定绿。**不记缺陷，也不记后端环境 PASS。**

对抗式结论：构造的反例均被当前文件+HTML+命令打回；无新 FAIL。

---

## 6. 技术性登记（无业务发明理由 + 证据）

| 段 | 位置 | 无业务发明理由 | 证据 | 缺陷 |
|---|---|---|---|---|
| 忽略构建/密钥/测试/本机/运行目录 | `.gitignore:1-55` | 产物与密钥，无状态/资格 | check-ignore | 否（016） |
| 排除 OpenAPI/归档/keyvault | `.semgrepignore:1-6` | 扫描器输入 | 路径存在；bash -n 0；未跑 semgrep | 否（017） |
| DB/API/profile/build 元数据 | `.env.example:2-13` | 进程配置 | application.yml 消费 | 否 |
| P0 local 种子 | `.env.example:18-22` | 仅 local 自动种子，禁生产 | application-local.yml | 否 |
| PII 注释（无赋值） | `.env.example:15-16` | 密钥安装说明 | yaml 默认；LocalKeyVault | 否 |
| worktree/禁直推/fetch/PRD 同步 | `AGENTS.md:23-40` | 多会话工程 | pre-push；ops 脚本 | 否 |
| 迁移串行/三变量/skipTests | `AGENTS.md:61-74` | 工程闸 | package.json 链 | 否 |
| 技术栈/脚本/基线/目录 | 两 README | 构建与入口 | pom/package/`--check`/`--list`/ls | 否（013/014/018/021 已修部分不再谎报） |
| GuardedAction 典型路径 | README 架构段 | 实现分层描述 | 类存在；登录例外与代码一致 | 否（015） |
| PROGRESS 路径与历史绿 | `PROGRESS.md` | 分支日志 | 路径存在；时态已标 | 否（022） |

Grok 不把上表改判为添加。

---

## 7. 既有 26 ID 重验（当前队列全否；源码整文件核对）

| ID | 分类 | 队列 | 当前事实 | 残留缺陷 |
|---|---|---|---|---|
| 001 | 技术性 | 否 | `.env.example` 无五条认证死变量 | 无 |
| 002 | 技术性 | 否 | 无 `COMPLEX_KEYVAULT_PII_KEY=""`；注释为未设置用默认 | 无 |
| 003 | 冲突 | 否 | 软删除限于配置主数据；注销走 HTML 专项；无 role_platform_admin=删除人 | 无 |
| 004 | 技术性 | 否 | 无浅克隆句；workflow `fetch-depth: 0` | 无 |
| 005 | 冲突 | 否 | CONTEXT 三 HTML 终权，无领域 Markdown 压过 HTML | 无 |
| 006 | 冲突 | 否 | 多对多非隶属；项目公司不管理各域票务结算 | 无 |
| 007 | 冲突 | 否 | 娱乐平台公司只酒吧域；服务费代收代付口径 | 无 |
| 008 | 冲突 | 否 | 会员以短信手机号识别，无自然人唯一 | 无 |
| 009 | 冲突 | 否 | 零售参考价可配窗口+前延，非写死 30 天唯一 | 无 |
| 010 | 冲突 | 否 | 结算中心可为非独立公司 | 无 |
| 011 | 冲突 | 否 | 十一类+客人无网页+留痕例外 | 无 |
| 012 | 冲突 | 否 | 逐条存档免批；批量/整店须批 | 无 |
| 013 | 技术性 | 否 | 无 database/migrations；无 79 脚本 | 无 |
| 014 | 技术性 | 否 | 英文当前基线转引中文自动块 | 无 |
| 015 | 技术性 | 否 | 典型路径+登录例外 | 无 |
| 016 | 技术性 | 否 | 忽略规则仍有效，无漏忽略的已证密钥文件 | 无 |
| 017 | 技术性 | 否 | 排除路径仍在；不宣称 semgrep 已扫过 | 无 |
| 018 | 技术性 | 否 | JDBC 与 pom 一致；命令键存在 | 无 |
| 019 | 细化 | 否 | 高危无发起人选档句 | 无 |
| 020 | 冲突 | 否 | 内部结算价不限兑换 SKU | 无 |
| 021 | 技术性 | 否 | 快照口径表外说明仍在；surefire 目录无 | 无 |
| 022 | 技术性 | 否 | 历史时态仍在 | 无 |
| 023 | 添加 | 否 | CONTEXT 无 L4；答复删除已落实 | 无 |
| 024 | 细化 | 否 | 词条仍在；BSM1725 上位；答复保留 | 无 |
| 025 | 冲突 | 否 | 四样例外=PGM287/965 | 无 |
| 026 | 冲突 | 否 | 两 README 无客人 Web 边 | 无 |

无 ID 且不在队列的新缺陷：**无**。因此无回 B2 的新项，无未修残留。

---

## 8. 非缺陷观察（不分 GR、不入修复队列）

1. mermaid 两个 `STAFF["员工 Staff"]` 同 ID，渲染为同一节点两条边，不是重复业务角色。  
2. 「管理端」/ `admin web` 指员工 Web，与「客人无网页端」不相反。  
3. AOP 依赖是 `aspectjweaver` 而非 `starter-aop`，README 列 AOP 能力与切面类存在一致。  
4. `.env.example` PII 段未写出变量名；接线在 `application.yml`。省略≠空串回归。  
5. 本席 `verify:backend-env` 因 JAVA_HOME 目录缺失失败；Java 21 二进制可用。失败不是 README 假命令。  
6. 英文读者表用文档名而非 `docs/` 路径；文件声明中文 README 为维护基线。  
7. `SPRING_PROFILES_ACTIVE` 不出现在 yml 字符串，属 Spring Boot 官方环境绑定。

---

## 9. 覆盖证明

| 文件 | 行 | 三线 | 18GEN | 四分类 |
|---|---:|---|---|---|
| `.env.example` | 1–22 | 已 | 已 | 技术性 |
| `.gitignore` | 1–55 | 已 | 已 | 技术性 |
| `.semgrepignore` | 1–6 | 已 | 已 | 技术性 |
| `AGENTS.md` | 1–74 | 已 | 已 | 细化+技术性 |
| `CONTEXT.md` | 1–163 | 已 | 已 | 细化（024 保留） |
| `PROGRESS.md` | 1–36 | 已 | 已 | 技术性 |
| `README.en.md` | 1–116 | 已 | 已 | 技术性+细化 |
| `README.md` | 1–149 | 已 | 已 | 技术性+细化 |

8/8 文件逐行读完。无抽样宣称整模块。无 HTML 改动。无锁外判据。无未答问题。

---

## 10. 最终结论

**PASS**

条件核对：整模块 621 行覆盖；18 GEN 逐项；三线完整；26 ID 重验无残留；无新 GR FAIL；无未答 questions；证据为类型 1 或 4（环境失败的 Maven 未当 PASS）；未把未运行的后端/UI/semgrep 测试写成通过。

B5 步骤结论 = PASS。模块生命周期仍由 Codex 在双席合取且四值校验后写入 ledger；本席不改 ledger、不写收口文件。
