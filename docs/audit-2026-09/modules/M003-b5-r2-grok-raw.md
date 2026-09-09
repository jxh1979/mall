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

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M003 B5-r2 Grok 独立全量审计（471 计划整模块重审）

版本：V0.02  
席位：Grok（乙），只读外审  
步骤：B5-r2（本轮跳过 B3/B4；历史 B3 累计 = 2，不能清零）  
模块：M003 `治理门禁与 CI 基线／配置与工程入口／第1段` / 共 471  
控制：160-audit-prompt.md V0.17  
正本：`/home/james/complex-management` `main` = `origin/main` = `242f487233d028173cb9198eecd079f5d877405c`  
范围文件：`.gitignore` 55 行 + `package.json` 147 行 = 202 行（`git ls-files -z -- .gitignore package.json | xargs -0 cat | wc -l` = 202）  
映射：`criteria-lock.md` L6382 `M003 → [GEN-001 … GEN-018]`，完整 18 GEN，无 HTML 派生 ID  
ledger（只读顶部基准 + 本模块行）：顶部 `LOCK-AMEND-COUNT: 1`；M003 `IN_PROGRESS`，B3=2，方案复核已修 2，失败重跑 0  
B2：操作员声明已双 PASS、修复队列空；本席不读 findings 正文，不继承旧同号 PASS  
旧 raw：本文件原 33190 字节、版本 V0.01（322 计划 8 文件）仅取元数据用于追加；旧报告其他文件不在本次范围  
结论：**PASS**  
临时 GR 缺陷：**无**  
未答 questions：无（`questions/M003-023.md` 答复首行=`删除`、`M003-024.md` 答复首行=`保留`，均属旧 322 的 CONTEXT.md 添加项，不是本轮两文件的新发明）  
新 HTML 自身问题：**无**（不申请改三 HTML；铁律 4 未触发）  
本席未读任何 self raw、本轮 B5 根报告、findings 正文、其他模块报告/收口，未派第三席，未改源码/HTML/依赖/lock/ledger/findings/questions，未提交推送部署，未装依赖，未新建 worktree/试验库/脚本。

---

## 0. 技能预检（本环境实核；V0.17 §6 收窄）

按意图执行：先读实际 `SKILL.md`，技能不派子代理、不新增产物、不问操作员、不自动修。

| 技能 | 本环境 | 本节点用法 |
|---|---|---|
| using-superpowers | 已读 `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能自检 |
| grilling / grill-me | 已读 grilling 全文；grill-me 为转发 `/grilling` 的薄封装 | 对抗式自问自答，不等操作员 |
| codebase-design | 已读 | 只用冻结 GEN-001 删除测试/深度口径，不新设计 |
| diagnosing-bugs / systematic-debugging | 已读 | 只读源复现红灯：建可失败命令、看 exit/输出；不改代码、不加探针 |
| ponytail-review | 已读 | 只列可删；不改文件 |
| verification-before-completion | 已读 | 凡声称均附本席本轮命令与输出 |
| ubiquitous-language | 已读 `/home/james/.qwen/skills/ubiquitous-language/SKILL.md` | 只借术语对齐 CONTEXT.md / 三 HTML；**不写** `UBIQUITOUS_LANGUAGE.md` |
| batch-grill-me | **缺口**：本环境技能树无独立 `SKILL.md` | 不冒充已调用 |
| git-guardrails-claude-code | 技能存在，本体要装 hook 并问人 | 不安装、不问；等价约束已由仓库 pre-push / 本席写禁承担 |
| tdd / writing-for-agents / resolving-merge-conflicts | 存在 | B5 只审不修，未触发 |

划掉技能（executing-plans、dispatching-parallel-agents、research、ponytail-audit 等）一律不用。

---

## 1. 范围、哈希、不继承

- 独立实算 `sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（6850 行）。sidecar 同行；ledger 顶部同行。正文不再重复该独占字段。
- general 全文已读 L10–100（GEN-001…018）。映射位置先实核：`## 模块映射` 在 L6379，M003 在 **L6382**，18 个 GEN ID 齐全。
- `plan/modules.md` 当前 M003：2 文件、202 行、NORMAL。`scope.txt` / `module-union.txt` 含 `.gitignore` 与 `package.json`。
- 旧 322 同号 raw（V0.01）覆盖 `.env.example` / `.semgrepignore` / `AGENTS.md` / `CONTEXT.md` / `PROGRESS.md` / `README*.md` 等，**不在本次范围**，其 PASS **不继承**。两源相对旧 322 若未改，也不是免审理由；本轮对 202 行整模块重审。
- 允许只读以核对本模块声明：`apps/web/package.json`（工作区清单）、`package-lock.json` 根 `packages[""]`（lock 镜像，不审 M004）、75 个 `tools/*.mjs` 入口头/参数、`.github/workflows/predeploy-gates.yml` L27 与 `ops/ci/predeploy-gates.yml` L27、`ops/160/propose-change.sh` L12。未把这些文件扩成完整外模块审计。

---

## 2. 第一性原理（三问，逐文件）

### 2.1 `.gitignore`（L1–55）

**为什么必须存在？** 删掉它，三份 HTML 没有任何一条会少一句业务规则。它不承载票务/酒吧/商户流程。它防止把依赖树、构建物、本机密钥、运行态上传二进制提交进 git。答不上 HTML 条目 ≠ 该删 → 2.1 分类为**技术性**（无业务发明），其中 `backend/data/bar-images/` 能指到 HTML 上位，见 §4.4。

**声称行为有没有被实际执行？** 有。本席用 `git check-ignore -v --no-index` 做 43 条应忽略 + 20 条不应忽略 + 2 条 `*.db-journal`，全部与模式一致；`git ls-files -z | git check-ignore --stdin --no-index` 命中 **0** 个已跟踪文件。仓库根现存 `debug8.mjs` / `probe-admin.mjs` / `smoke-group2.mjs` / `btn-iso.mjs` / `eng-check.mjs` / `verify-fe.mjs` / `bizflow-audit.mjs` 均被对应根锚规则忽略，且不出现在 `git status --porcelain`。

**哪个前提未经验证就当真？** 未在本机制造真实 `.apk` 提交事故；未把忽略目录当「业务上传功能已实现」。忽略路径存在 ≠ 上传/POS 功能已交付（那些实现属后续模块）。

### 2.2 `package.json`（L1–147，127 个 scripts）

**为什么必须存在？** 它是 npm 工作区根与 CI/本地门禁的**唯一脚本接口**。删掉它：`.github/workflows/predeploy-gates.yml` 与 `ops/ci/predeploy-gates.yml` 的 `npm run verify:predeploy`、`ops/160/propose-change.sh` L12 同一入口会失去声明。三 HTML 仍不直接要求 npm。整体 **技术性**；其中 HTML 核验入口见 §4.4 **细化**。

**声称行为有没有被实际执行？** 分层取证，不把 `--check`/`--help` 写成全套业务测试通过：

| 声称 | 本轮实跑 | 边界 |
|---|---|---|
| JSON 合法、127 键、无重复、无 `\|\|`/`;`/`\|` | `json.loads` + `object_pairs_hook`；`npm run` 列出 127 | 语法与清单，不是业务 |
| 75 个 `node tools/*.mjs` 入口存在且可解析 | 75/75 `node --check` exit 0；缺失 0 | 语法，不是工具功能 PASS |
| 10 条 `&&` 链转发名均存在 | 全员 `npm run <name>` 键命中；workspace 形态脚本在 `apps/web` 有对应键 | 解析，不是 49 链跑完 |
| 工作区 `apps/web` | `npm run --workspace apps/web` 列出 `dev/build/typecheck/test:unit/...`；`npm pkg get` 与根转发名一致 | 未跑 vite/tsc/vitest |
| 坏输入失败 | 不存在工作区 exit 1；不存在脚本名 exit 1 | 负例 |
| `&&` 错误传播 | `npm run verify:html-ledger-sync && echo SHOULD_NOT_REACH` 未打印后半；`npm run verify:predeploy` 停在第一闸，未进 `verify:entry-anchors` | 真实 npm 11 脚本壳 |
| HTML 三闸 | ledger-sync exit 1；entry-anchors exit 1（6 处）；param-tier exit 0 | 见 §4.3，**正确拒绝 ≠ 本文件缺陷** |
| 引擎/依赖 | node v24.20.0 满足 `^22.13.0 \|\| >=24.0.0`；`node_modules/yaml` 可 `import`；`playwright --version` = 1.61.0 | 未跑 UI 套件 |
| `--check` 环境 | `verify:backend-env` exit 0；`verify:android-env` exit 0 | 只声明环境，不是 Maven test / Gradle build / APK |

**哪个前提未经验证就当真？** 未跑：`spring-boot:run`、`test:backend`、Playwright E2E、`android:p0:*` 构建、任一 `docs:*` 生成器（会写文件）、`verify:delivery-gates` 49 链、`verify:p0`、`verify:closed-loop-runtime`、`verify:pii-encryption`、`verify:test-db-isolation`、`verify:tenant-isolation`。这些实现与测试在别的模块。本席只核到「入口字符串 → 现存文件/工作区键/&& 短路」。

---

## 3. 顺从式：18 GEN 逐项（完整，不抽查）

适用口径：映射仅 18 GEN。每条写适用 / 不适用、证据、是否缺陷。

| ID | 适用 | 本模块证据 | 缺陷 |
|---|---|---|---|
| GEN-001 必要性与最小性 | 适用 | 删除测试：删 `package.json` 则 CI/`propose-change.sh` 失去 `verify:predeploy`；删 `.gitignore` 则 node_modules/密钥/运行态图会入库。127 脚本是门禁/构建/生成器入口，不是未引用死代码。别名 `dev`/`dev:api`→`dev:backend`、`smoke:p0`→`test:backend`、`test:ui`=`verify:ui`、`test:unit`=`verify:unit` 为一行具名入口，不是无需求抽象。ponytail：不因假设场景新增忽略规则（`.scratch/` 按 V0.17 §1.2 故意不忽略）。 | 无 |
| GEN-002 可执行正确性 | 适用 | JSON 合法；127/127 `npm run` 可列出；75/75 工具 `node --check`；工作区键对齐；坏工作区/坏脚本名 exit 1；三闸与 `--check` 环境入口实跑。长驻/全量测试未跑，不写成已通过。 | 无 |
| GEN-003 测试有效性 | 不适用本文件测试集 | 本模块无测试文件。`test:*`/`verify:unit`/`playwright test` 是转发，测试体在 M004 之后的模块。不得把未跑套件写成 package.json 通过。 | 无 |
| GEN-004 信任边界 | 弱适用 | 脚本字符串静态，无把未校验用户输入拼进 shell。工具对 git/HTML/CSV 的校验在工具模块。`.env` 忽略避免把秘密当源码。`engine-strict` 默认 false 是 npm 缺省，文件已声明 engines；首跑本机 node 24 匹配，不为假设的 node 18 新增字段。 | 无 |
| GEN-005 授权与最小权限 | 不适用 | 两文件无主体/对象/动作授权面。 | 无 |
| GEN-006 数据完整性 | 不适用 | 无事务/金额/状态机。 | 无 |
| GEN-007 并发与幂等 | 不适用 | 无重试投递窗口。脚本链是进程内 `&&`。 | 无 |
| GEN-008 接口与模式一致性 | 适用（工程接口） | 根 `workspaces: ["apps/web"]` 与 lock 镜像、`apps/web/package.json` 的 `dev/build/typecheck/test:unit` 同名。`engines.node` 与 lock 根块一致。CI 调 `npm run verify:predeploy` 与根脚本同名。`documentVersion`（本文件 V0.01）与 lock 文件自己的版本字段不是同一文件，不把 M004 当本模块缺陷。脚本名是系统内部标识符，允许英文。 | 无 |
| GEN-009 错误处理与恢复 | 适用 | 全库脚本无 `\|\|` / `;` 吞错。10 条 `&&` 链。实跑：ledger-sync 失败后 `SHOULD_NOT_REACH` 未出现；`verify:predeploy` 未进入第二闸。缺失脚本/工作区显式 npm error + 非 0。 | 无 |
| GEN-010 安全与敏感信息 | 适用 | `.env` / `.env.local` / `local.properties` / `*.apk` 忽略。`package.json` 无明文秘密。`.env.example` **不被** `.env` 误伤（负例）。根 debug 脚本忽略，避免把探针提交。 | 无 |
| GEN-011 可观测与审计证据 | 弱适用 | 门禁脚本失败时 stdout/stderr 给出范围与文件名（ledger-sync / entry-anchors 实输出见 §8）。本文件本身不是日志系统。 | 无 |
| GEN-012 资源与性能边界 | 弱适用 | 本文件无循环/连接池。49 链与 E2E 的资源边界在被调模块；本席未跑那些链。 | 无 |
| GEN-013 构建配置与依赖 | 适用 | `packageManager: npm@10.9.8` 已声明；本机实际 `npm 11.19.0` + corepack 存在但未切换——这是**本机状态**，脚本在 npm 11 下仍按 `&&` 失败显式，不构成 package.json 假声明。`devDependencies`：`yaml` 被 `verify-openapi-contract.mjs` / `apply-openapi-p0-examples.mjs` / `verify-delivery-gates.mjs` import；`@playwright/test@1.61.0` 与锁/二进制一致。Java 21 / Maven 3.9.11 / Android SDK 由 `--check` 包装器检查，不塞进 npm `engines`（npm engines 表达不了 JDK）。CI 调 `npm ci --ignore-scripts` 属工作流文件（M005），此处只核脚本名。 | 无 |
| GEN-014 数据库迁移正确性 | 不适用本文件迁移 | `verify:migration-version-order` 仅入口存在+语法；未跑迁移、未连 `complex_test`。 | 无 |
| GEN-015 界面可用性 | 不适用 | 无 UI。脚本名属内部标识符。gitignore 注释为中文。 | 无 |
| GEN-016 文档可验证性 | 适用（注释/字段） | gitignore 注释与实际匹配：security-scans 只忽略 `**/*.log` 与 `semgrep-current/`，json/md 负例通过；根锚 `/debug*.mjs` 不忽略 `tools/debug-foo.mjs`。`documentVersion` 只声明本 JSON 文件版本，未把计划写成现状。 | 无 |
| GEN-017 业务来源分类 | 适用 | 冲突→细化→技术性→添加 已逐项走。无冲突。细化仅限能摘 HTML 原句的核验入口与 POS 图片目录。其余包/构建/Git 规则技术性。不因 `tenant`/`evidence`/`bar` 等词判添加。无新添加项。 | 无 |
| GEN-018 第一性原理证据 | 适用 | 本报告结论落到：HTML 原句（类型 1）、可复现命令输出（类型 4）、入口语法/存在性（类型 4）。不引用旧 322 PASS、不引用另一席意见。 | 无 |

18/18 已核对。

---

## 4. 对抗式（grilling 自问自答；假定实现错误）

Frontier 一次问完，事实自读。

**Q1 入口是否存在/语法烂掉？** 推荐：JSON 解析失败或缺文件即缺陷。  
事实：解析成功、无重复键、无 BOM、尾随换行；75 工具 `node --check` 全过。

**Q2 npm 工作区与参数是否骗人？** 根只声明 `apps/web`。`npm run dev --workspace apps/web` 等 6 处与 `apps/web` 的 `dev/build/typecheck/test:unit` 对齐。`npm run build --workspace apps/does-not-exist` → `No workspaces found` exit 1。未使用 `npm exec -w apps/web -- node` / `npx`。

**Q3 `&&` 会不会在失败后继续？** 失败的 ledger-sync 后无 `SHOULD_NOT_REACH`；`verify:predeploy` 输出只出现第一闸，没有 entry-anchors 横幅。

**Q4 Git 忽略正负匹配是否只写在文件里？** 见 §8 矩阵：应忽略 45（含 db-journal）全中；不应忽略 20 全过，包括 `.env.example`、`.scratch/foo`、`docs/audit-2026-09/ledger.md`、三 HTML、`tools/debug-foo.mjs`。已跟踪文件匹配忽略规则 = 0。

**Q5 依赖/引擎/本机配置？** node 24 匹配 engines；playwright 1.61.0；yaml 可 import。`verify:backend-env` / `verify:android-env` 本机 `--check` 绿（Java 21、Maven 3.9.11、Gradle wrapper/系统 Gradle、SDK `/home/james/android-sdk`）。这只证明**检查器认为环境在**；不是 APK/单测通过。

**Q6 业务来源是否夹带发明？** 见 §4.4。无添加。

**Q7 全局红灯是不是 package.json 的错？** 见 §4.3。入口正确拒绝脏 HTML。不修 CSV/HTML、不改排除范围刷绿。

**Q8 两源未改能否免审？** 否。本轮仍逐行覆盖 202 行。

**Q9 要不要为 `.scratch/` 或 `docs/audit-2026-09/` 新增忽略？** 否。V0.17 故意不忽略 `.scratch/`；审计产物目录按路径排除出 scope，不是 gitignore 缺陷。不为假设误提交加规则。

**Q10 `type: "module"` 会不会弄坏根 CJS？** 仓根无受控 `.js/.cjs`；仅有已被忽略的调试 `.mjs`。

**Q11 `dev` 只启后端是否欺诈？** 脚本体是 `npm run dev:backend`，另有 `dev:web`。名称是别名不是全栈声称。

**Q12 越权/金额/状态跳转？** 两文件无这些面。

### 4.3 全局红灯（本席独立核实，区分范围）

工作区：`docs/business-system-map-for-operations-v0.1.html` 相对 `origin/main` **未提交** 8 行（4+/4-）。`docs/prd-audit/business-decision-ledger.md` **未**出现在同一脏集。PGM / BEQR 工作区干净。

`npm run verify:html-ledger-sync` exit **1**：

```
[html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。比对范围：工作区（含未提交改动）↔ 基线 origin/main@242f4872。
  已改动的 HTML：
    - docs/business-system-map-for-operations-v0.1.html
  必须在同一批改动内一并更新：docs/prd-audit/business-decision-ledger.md
```

这是门禁设计行为：HTML 相对基线有改、台账未同步 → 拒绝。 **不是 package.json 缺陷。** 不重开 M001。BSM 工作区 diff 含：页脚日期 2026-09-01→2026-09-08、回访句改写、导航标题、以及 HTML 注释 `<!-- 文件版本：V0.11 → V0.13 -->`。注释按工具 `plainText()` 会剥掉。

`npm run verify:entry-anchors` exit **1**，**6** 处（本席原文）：

1. 注册表 BSM-E0433「处理后回访客人或商户，验收合格才关闭」——HTML 已改而注册表未重建  
2. 注册表 BSM-E0736「文档版本」——HTML 已改而注册表未重建  
3. 账本 BSM-0283 source_text 同上回访句  
4. 账本 BSM-0530「文档版本」  
5. 账本 BSM-0283 preconditions 回访句片段  
6. 账本 BSM-0283 state_transition 回访句片段  

计数：回访句 4 处 +「文档版本」2 处。工具未把 D 注释 `<!-- 文件版本 -->` 列入（与「不是 D 注释」一致）。「文档版本」条目对应页脚/版本镜像，不是本模块脚本写错。 **不是 package.json 缺陷。** 禁止靠改 CSV/HTML 刷绿。

`npm run verify:html-param-tier` exit **0**：改动了 1 份业务 HTML，全量重扫无基线外未标层级句。

`npm run verify:predeploy` exit **1**，停在第一闸，未跑后续（含 test-db / pii）。证明 CI 入口接线与 `&&` 短路，**不**证明后 14 步通过。

### 4.4 四分类（冲突 → 细化 → 技术性 → 添加）

**冲突：** 无。不重判 BSM 来源可配 / 打折原因分类（用户已声明 B/C 原文即原意）。

**细化（先摘 HTML 原句）：**

1. **核验入口 `verify:html-ledger-sync` / `verify:entry-anchors` / `docs:entry-baseline` / `verify:entry-doc-baseline`**  
   HTML 原句（BSM 工作区页脚，`docs/business-system-map-for-operations-v0.1.html` 约 L2003；PGM L974、BEQR L306 同构）：  
   > 本《业务系统地图》与《平台治理地图》《业务枚举速查表》共同组成三份最高业务解释权 HTML。发生冲突时，只以这三份 HTML 中经用户批准的内容为准；PRD、其他文档、后台配置、设计和实现均不得覆盖。某项业务内容若尚未写入三份 HTML，不因出现在 PRD、配置或实现中而取得效力，必须先逐项获得用户批准并补入相应 HTML。三份 HTML 之间若出现冲突，按确认时间线判定：凡经用户确认过的内容，确认时间越晚的优先级越高，后确认的覆盖先确认的（**以决策台账记录的裁决时间为准**）；无法确定确认时间先后的，仍须逐项重新裁决。  
   下位实现：同 PR 同步决策台账、按条目名而非行号保护 HTML 正文。无新业务实体。

2. **运行态目录 `.gitignore` L53–54 `backend/data/bar-images/`**  
   HTML 原句（BSM `平板图形点单（POS）`，约 L953–959）：  
   > 进入带商品图片的分类菜单，按酒水、饮品、小食等分类挑选；也可以直接选后台配好的套餐。  
   > 商品分类、商品图片、套餐都在后台维护：分类可自定义、**图片后台上传保存**、套餐由后台从已有商品组合而成。  
   下位实现：运行态生成的本地图片目录不入库。不发明新 POS 流程。

**技术性（无业务发明）：**

- `.gitignore` 其余规则：依赖/构建/缓存/密钥/测试报告/根探针脚本。`backend/data/evidence-files/`：只忽略运行态生成文件，不新增证据类型、审批链或状态；不凭「证据」一词强拉工单/退酒/抄表等 HTML 锚点。  
- `package.json` 的 name/private/version/type/workspaces/engines/packageManager/devDependencies 与除上列外的脚本：构建、测试转发、OpenAPI/生产证据生成器、Android/Maven 包装。`verify:html-param-tier` 是对 HTML 数值层级标注的静态棘轮，三 HTML 无「必须在原句标注层级」独立业务条目，不凭 PGM「法定>证据>可配置」仲裁顺序强拉；无新域名词。  
- `verify:tenant-isolation` 等名称含业务词，本文件只提供 npm 入口字符串，业务规则在被调模块。

**添加：** 无。不为假设场景新增规则、文件或流程。

### 4.5 ponytail-review / codebase-design

- `package.json:L11,L15,L38,L72-73,L79-80`：yagni 候选为具名别名，各 1 行，保留入口稳定。Replacement：不删。  
- `.gitignore`：无死规则（本轮每条都有正例或设计负例）。  
**Lean already. Ship. net: 0 lines possible.**

删除测试：package.json 是深模块（小接口、127 行为）；复杂度会回到 CI 与开发者命令。gitignore 同理。

---

## 5. 202 行覆盖

### 5.1 `.gitignore` 1–55

| 行 | 内容 | 三线 | 分类 | 证据 |
|---:|---|---|---|---|
| 1 | `node_modules` | 已 | 技术性 | 忽略 `node_modules/foo`、`apps/web/node_modules/bar` |
| 2 | `dist` | 已 | 技术性 | `dist/index.js`、`apps/web/dist/x` |
| 3 | `backend/target` | 已 | 技术性 | `backend/target/classes` |
| 4 | `.env` | 已 | 技术性 | `.env`、`foo/.env`；`.env.example` 不忽略 |
| 5 | `.env.local` | 已 | 技术性 | 命中 |
| 6 | `*.db` | 已 | 技术性 | `data.db` |
| 7 | `*.db-journal` | 已 | 技术性 | `foo.db-journal` |
| 8 | `coverage` | 已 | 技术性 | `coverage/lcov.info` |
| 9 | `.vite` | 已 | 技术性 | 命中 |
| 10–11 | `.gradle` / `**/.gradle/` | 已 | 技术性 | 根与 android-app |
| 12–13 | `.kotlin` / `**/.kotlin/` | 已 | 技术性 | 同上 |
| 14–15 | `build` / `**/build/` | 已 | 技术性 | `build`、`apps/android-terminal/android-app/build/x`；已跟踪匹配 0 |
| 16–19 | `*.class/*.dex/*.apk/*.aab` | 已 | 技术性 | 正例命中 |
| 20–21 | `local.properties` / `**/local.properties` | 已 | 技术性 | 根与 android-app |
| 22 | 空行 | 已 | 技术性 | 分隔 |
| 23 | `playwright-report` | 已 | 技术性 | 命中 |
| 24 | 空行 | 已 | 技术性 | 分隔 |
| 25–26 | `test-results` / `playwright-report-real-backend` | 已 | 技术性 | 命中 |
| 27 | 空行 | 已 | 技术性 | 分隔 |
| 28–30 | Claude 本机 settings 注释+两路径 | 已 | 技术性 | 命中；注释中文 |
| 31–32 | tsbuildinfo 注释+模式 | 已 | 技术性 | 根与 `apps/web/foo.tsbuildinfo` |
| 33–36 | security-scans 日志/滚动产物 | 已 | 技术性 | `*.log` 忽略；`findings-register-v1.md`、dated json **不**忽略 |
| 37 | 空行 | 已 | 技术性 | |
| 38–39 | `docs/_archive/**/*.log` | 已 | 技术性 | 只日志；`docs/_archive/readme.md` 不忽略。归档路径本身按 §1.2 排除出审计 scope |
| 40 | 空行 | 已 | 技术性 | |
| 41–42 | `.loop-engineering/` | 已 | 技术性 | 命中 |
| 43 | 空行 | 已 | 技术性 | |
| 44–51 | 根探针脚本 | 已 | 技术性 | `/debug*.mjs` 等；真实 `debug8.mjs` 等被忽略；`tools/debug-foo.mjs`、`apps/web/debug-foo.mjs` 不忽略 |
| 52 | 空行 | 已 | 技术性 | |
| 53–54 | `backend/data/bar-images/` + 注释 | 已 | **细化** | 正例命中；HTML 原句见 §4.4 |
| 55 | `backend/data/evidence-files/` | 已 | 技术性 | 正例命中；无业务发明 |

55/55。

### 5.2 `package.json` 1–147

| 行 | 内容 | 三线 | 分类 |
|---:|---|---|---|
| 1,147 | `{` `}` | 已 | 技术性 |
| 2 | `name` | 已 | 技术性（内部标识符，非展示文案） |
| 3 | `private: true` | 已 | 技术性 |
| 4 | `version: 0.1.0` | 已 | 技术性 |
| 5 | `documentVersion: V0.01` | 已 | 技术性（本文件版本） |
| 6 | `type: module` | 已 | 技术性 |
| 7–9 | `workspaces: ["apps/web"]` | 已 | 技术性；工作区清单/lock 镜像一致 |
| 10,138 | `scripts` 开合 | 已 | 技术性 |
| 11–137 | 127 个脚本，一行一个 | 已 | 见 §6；L21–25 中 4 个 HTML 相关为细化，其余技术性 |
| 139 | `packageManager: npm@10.9.8` | 已 | 技术性 |
| 140–142 | `engines.node` | 已 | 技术性；本机 v24.20.0 匹配 |
| 143–146 | `devDependencies` playwright + yaml | 已 | 技术性；均被声明入口使用 |

147/147。合计 202/202。无抽样宣称整模块。

---

## 6. 全脚本分类（127/127）

图例：实跑=本轮执行该 npm 脚本；语法=仅 `node --check` 目标；链名=仅核 `&&` 成员存在；转发=核工作区/别名键；**未跑全量测试**。

| # | 脚本 | 分类 | 本轮边界 |
|---:|---|---|---|
| 1 | dev | 技术性 | 转发 `dev:backend`；未启服务器 |
| 2 | dev:backend | 技术性 | 读包装器：非 `--check` 会 `spring-boot:run`；未跑 |
| 3 | test:backend | 技术性 | 会跑 Maven test；未跑 |
| 4 | verify:backend-env | 技术性 | **实跑 --check exit 0** |
| 5 | dev:api | 技术性 | 别名 |
| 6 | dev:web | 技术性 | 工作区键存在；未跑 vite |
| 7–13 | verify/audit openapi·routes·payload | 技术性 | 目标存在+语法 |
| 14–20 | docs:openapi/security 生成器 | 技术性 | 会写文件；未跑 |
| 21 | docs:entry-baseline | 细化 | 会写；未跑 |
| 22 | verify:html-ledger-sync | 细化 | **实跑 exit 1（正确拒绝）** |
| 23 | verify:entry-anchors | 细化 | **实跑 exit 1（6 处 CSV/HTML 镜像，非本文件错）** |
| 24 | verify:html-param-tier | 技术性 | **实跑 exit 0** |
| 25 | verify:entry-doc-baseline | 细化 | `--check`；未跑（避免与生成器混淆） |
| 26–31 | security/ops160 单闸 | 技术性 | 目标+语法 |
| 32–35 | ops160-recovery / p0-contract / p0-web / p0-env | 技术性 | 链名齐全；p0-env 的两成员 `--check` 已分跑 |
| 36–37 | build / typecheck | 技术性 | 工作区键存在；未跑 tsc/vite（会写 tsbuildinfo） |
| 38 | smoke:p0 | 技术性 | 等于 test:backend；未跑 |
| 39 | verify:android-env | 技术性 | **实跑 --check exit 0** |
| 40–42 | android:p0:build/smoke/apk | 技术性 | 工程目录与 gradle 文件存在；未构建 |
| 43 | verify:p0 | 技术性 | 10 段链名齐全；未跑 |
| 44–50 | no-legacy / coding-host / t3* / external / delivery-gates-docs | 技术性 | 目标+语法 |
| 51 | verify:delivery-gates | 技术性 | 49 段链名齐全；**未跑** |
| 52–71 | 生产证据/阻断 docs+verify | 技术性 | 目标+语法；docs 未跑 |
| 72–76 | playwright / real-backend | 技术性 | 配置文件存在；cli `--version` 1.61.0；**未跑测试**；未 `--update-snapshots` |
| 77–80 | android-smoke-wrapper / admin-matrix / unit | 技术性 | 工作区 `test:unit` 键存在；未跑 vitest |
| 81–85 | domain-ownership / matrix-vs-migration / migration-version-order / tenant-isolation | 技术性 | 目标+语法；tenant 名称不构成添加 |
| 86 | verify:predeploy | 技术性（成员含细化闸） | **实跑，第一闸 exit 1 短路** |
| 87–88 | pii-encryption / test-db-isolation | 技术性 | 未跑（可能触 DB/密钥；读入口即止） |
| 89–127 | 其余 closed-loop / nas / production-evidence / data-isolation 等 | 技术性 | 目标+语法；`docs:*`/`prepare:*` 未跑；`--check` 未当作业务 PASS |

冲突 0；细化 4（#21–23、#25）；技术性 123；添加 0。

---

## 7. 三线执行记录

| 线 | 状态 |
|---|---|
| 第一性原理 | §2 两文件三问均落类型 1/4 证据 |
| 顺从式 | §3 十八 GEN 逐项 |
| 对抗式 | §4 入口/工作区/&&/忽略正负/依赖引擎/分类/红灯范围 |

---

## 8. 命令与真实输出（摘录）

本席实算锁哈希：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。

```
git rev-parse HEAD
242f487233d028173cb9198eecd079f5d877405c
git rev-parse origin/main
242f487233d028173cb9198eecd079f5d877405c
git ls-files -z -- .gitignore package.json | xargs -0 cat | wc -l
202
```

```
node -v    → v24.20.0
npm -v     → 11.19.0
java -version → openjdk 21.0.12.1 LTS
mvn -version → Apache Maven 3.9.11
```

`npm run` 列出 **127** 个脚本。  
`npm run --workspace apps/web` exit 0，含 `dev/build/typecheck/preview/test:unit/test:unit:watch`。  
`npm run build --workspace apps/does-not-exist` exit 1 `No workspaces found`。  
`npm run this-script-does-not-exist` exit 1 `Missing script`。

`node --check`：75/75 OK。

gitignore 摘要：positive_ok=43 + db-journal 2；negative_ok=20；tracked matching ignore=0。

```
> npm run verify:html-ledger-sync
[html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。… origin/main@242f4872
EXIT_LEDGER:1
```

`&& echo SHOULD_NOT_REACH`：后半未出现；AND_EXIT=1。

```
> npm run verify:predeploy
（只执行到 verify:html-ledger-sync 后失败）
EXIT_PREDEPLOY:1
```

```
> npm run verify:entry-anchors
[entry-anchors] FAIL：6 处不合格。
（六条原文见 §4.3）
EXIT_ANCHORS:1
```

```
> npm run verify:html-param-tier
[html-param-tier] PASS: 改动了 1 份业务 HTML，全量重扫无基线外的未标层级句…
EXIT_PARAM_TIER:0
```

```
> npm run verify:backend-env
[backend-env] 后端环境前置校验通过。
- Picked up JAVA_TOOL_OPTIONS:  -Djava.net.preferIPv4Stack=true
- system Maven: Apache Maven 3.9.11 …
EXIT_BE:0
```

```
> npm run verify:android-env
[android-env] Android P0 环境前置校验通过。
- … Java 21 …
- system Gradle: ------------------------------------------------------------
- Android SDK: /home/james/android-sdk
EXIT_AND:0
```

`node node_modules/@playwright/test/cli.js --version` → `Version 1.61.0`。

---

## 9. 执行边界（诚实）

**做了：** 读两源全文；解析 JSON；列脚本；工作区正负例；75 文件语法；gitignore 正负与真实根探针文件；三份 HTML 门禁实跑；predeploy 短路；两个 `--check` 环境入口；playwright/yaml 本机包探测；CI/propose-change 调用点。

**明确没做、因此不能声称：**

- 任何 Maven `test` / `spring-boot:run`（长驻、连库风险）  
- Playwright 用例、`--update-snapshots`  
- `npm run build/typecheck/test:unit --workspace apps/web`（会写 tsbuildinfo / 全量编译）  
- Android `clean build` / `assembleDebug` / p0 smoke  
- 全部 `docs:*` / `prepare:*`（写业务资料或生成物）  
- `verify:delivery-gates` 49 步、`verify:p0`、`verify:closed-loop-runtime`  
- `verify:pii-encryption`、`verify:test-db-isolation`、`verify:tenant-isolation` 的业务断言  
- `npm ci` / `npx` / `npm exec -w apps/web -- node`  
- 未读、会写、会删、会外发的命令  

临时检查未改仓库文件。npm 对缺失脚本写了 `~/.npm/_logs/`（仓外）。

---

## 10. Findings（临时 GR）

**空集。** 不分配 GR-01 起的号。

范围外已知红灯（登记，不进本模块修复队列）：html-ledger-sync / entry-anchors 因 BSM 工作区相对 origin/main 变更且 CSV/台账未同步而正确失败；归 HTML/台账镜像（计划中其他模块），不归 `.gitignore`/`package.json` 接线错误。

无添加项待 B2。无新 HTML 自身问题，不走铁律 4 申请。

---

## 11. 结论

单席 B5 步骤结论 = **PASS**。

核对：202 行覆盖；127 脚本分类；忽略规则全表；18 GEN 逐项；三线完整；声称均有类型 1 或 4 证据；无未答 questions；无未定稿本模块缺陷；未把未运行的后端/UI/Android/49 链写成通过；未继承 V0.01/322 PASS。

模块生命周期仍由 Codex 在双席合取且 B5 后四值校验后写入 ledger；本席不改 ledger、不写收口文件。
