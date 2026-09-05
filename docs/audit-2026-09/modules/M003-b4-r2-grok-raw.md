CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M003 B4-r2 Grok 独立审计报告

版本：V0.01
席位：Grok（审计席乙）
步骤：B4-r2（只审本轮修复内容全 diff，不修源、不回退、不读另一席 raw）
控制：V0.17
模块：M003 `01-root-config-and-guidance-01` / 共 322
ledger 状态（本席实读）：`IN_PROGRESS`
B3 已执行次数（本席实读 ledger）：2
findings 台账：V0.39
LOCK-AMEND-COUNT（本席实读 ledger）：0
本席未读取 `docs/audit-2026-09/modules/M003-b4-r2-self-raw.md` 或其本次分析；未用本席 B4-r1 或任何历史 PASS 替代当前两树证据。

结论先行（全部 hunk / 全部行审完后给出，见文末复核）：
- B4 越界闸（§5 闸 3）：**PASS**
- 本席 B4-r2 总体：**PASS**
- GR 临时缺陷发现：**无**
- 无 ID 改动：**无**

---

## 1. 输入版本与哈希（本席独立实算）

| 项 | 本席实算 / 实读 |
|---|---|
| criteria-lock.md SHA256 | `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de` |
| sidecar `plan/criteria-lock.sha256` | `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`（与实算全等） |
| ledger 顶部 `CRITERIA-LOCK-SHA256` | `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`（与实算全等） |
| LOCK-AMEND-COUNT | 0 |
| 真实 `.git/index` SHA256 | `6a25d7b677e4dc3eb39c076f8e6a204799c1a92693734b773194a036ad59561b` |
| before tree | `6318ad55b33dc901d19038d1d643bbb483c72f7a`（`git cat-file -t` = tree） |
| after tree | `fbe3b6b9726663cd0ab40bcffd405ebd9c1e1110`（`git cat-file -t` = tree） |
| `git diff --binary <before> <after>` 字节 | 4996 |
| 同上 SHA256 | `7c02b71c6fd9665ccbfbe2b6be7c2642e94f9be6c07fdbb180d66c7ba446420e` |
| 文件数 / hunk 数 / 行变更 | 3 文件、9 hunk、10 insertion / 10 deletion |
| 路径 | `AGENTS.md`、`README.en.md`、`README.md`（`name-status` 均为 M） |
| findings.md | 版本 V0.39；唯一队列表 26 ID，队列「是」= M003-018 / M003-025 / M003-026 共 3；其余 23「否」 |
| questions | `M003-023.md` 答复段首个非空行=`删除`；`M003-024.md` 答复段首个非空行=`保留`；无未答 |
| 模块文件闭集（modules.md） | 8：`.env.example` `.gitignore` `.semgrepignore` `AGENTS.md` `CONTEXT.md` `PROGRESS.md` `README.en.md` `README.md` |
| M003 映射判据 | 仅 18 条 GEN（GEN-001 … GEN-018）；PGM287/965/739 作 2.3 类型 1 的 HTML 原文，不是 M003 映射区 ID |

命令（独立、无路径过滤、不用 HEAD）：

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'
git cat-file -t 6318ad55b33dc901d19038d1d643bbb483c72f7a
git cat-file -t fbe3b6b9726663cd0ab40bcffd405ebd9c1e1110
git diff --binary 6318ad55b33dc901d19038d1d643bbb483c72f7a fbe3b6b9726663cd0ab40bcffd405ebd9c1e1110 | sha256sum
git diff --binary 6318ad55b33dc901d19038d1d643bbb483c72f7a fbe3b6b9726663cd0ab40bcffd405ebd9c1e1110 | wc -c
sha256sum .git/index
```

与冻结声明逐项核对：字节 4996、SHA256 `7c02b71c…420e`、3 文件、9 hunk、10 处替换，全部一致。

---

## 2. 技能预检（Grok 席自核）

按控制 §6 只核存在性并按收窄口径使用；不派子代理、不等操作员、不写 §7 以外文件。

| 技能 | 存在 | 本次用法 |
|---|---|---|
| using-superpowers | 存在 | 开工自检 |
| grilling / grill-me | 存在 | 只借追问结构；事实自读；作废等用户/派子代理 |
| verification-before-completion | 存在 | PASS 前先跑命令 |
| systematic-debugging | 存在 | 只借读错误→复现→追数据流；不改生产代码 |
| requesting-code-review | 存在 | 本席即 B4 外审；不用 HEAD~1 / 子代理 / 无 ID 即修 |
| receiving-code-review | 存在 | B4 期间禁止修复，本席不实施 |
| test-driven-development / tdd | 存在 | 文档/说明性 md 豁免，未造测试 |
| codebase-design | 存在 | 核 mermaid 未新增 app 节点或浅层包装 |
| diagnosing-bugs | 存在 | 018/025/026 根因落到 pom/HTML |
| ponytail / ponytail-review | 存在 | 核最小替换；结论：Lean already. Ship. |
| writing-for-agents | 存在 | 本 raw 供合取阅读 |
| resolving-merge-conflicts | 存在 | 无合并冲突，未调用本体 |
| git-guardrails-claude-code | 存在于 `skills/misc/`（非默认路径） | 本席无 Git 写，未调用本体 |
| batch-grill-me | **不存在** | 缺口；不冒充已调用 |
| ubiquitous-language | **不存在** | 缺口；不冒充已调用 |

划掉的技能（dispatching-parallel-agents / subagent-driven-development / research / ponytail-audit 等）均未调用。未起子代理。

---

## 3. 冻结两树完整 diff（原样 4996 字节）

下列代码块内字节与 `git diff --binary 6318ad55b33dc901d19038d1d643bbb483c72f7a fbe3b6b9726663cd0ab40bcffd405ebd9c1e1110` 输出逐字节相同（SHA256=`7c02b71c6fd9665ccbfbe2b6be7c2642e94f9be6c07fdbb180d66c7ba446420e`）。

```
diff --git a/AGENTS.md b/AGENTS.md
index ded7f72a..450ec8e3 100644
--- a/AGENTS.md
+++ b/AGENTS.md
@@ -1,6 +1,6 @@
 # AGENTS.md
 
-版本：V0.01
+版本：V0.02
 
 ## Agent skills
 
@@ -52,7 +52,7 @@ single-context 布局，词表在仓库根的 `CONTEXT.md`。详见 `docs/agents
 6. **配置类主数据删除走审批、软删除**：先存档、填理由，由具备最高权限的人审批，申请人不得自审；存库、前台不显、可恢复、分类并留痕。
    账户注销等个人信息删除按三份业务 HTML 的专项生命周期规则执行。
 
-7. **所有展示文字全中文**，仅机读标识（枚举值、字段名、日志键）保留原文。
+7. **所有展示文字全中文**，可以不用中文的只有单据号、设备编号、二维码内容、系统内部标识符四样。
    后台、客人端、设备端、安卓端一律适用。
 
 8. **不为存量数据做兼容、退化或回填设计。** 库里数据最后都要删光，
diff --git a/README.en.md b/README.en.md
index c4140398..271e4566 100644
--- a/README.en.md
+++ b/README.en.md
@@ -1,6 +1,6 @@
 # Entertainment Complex Management System
 
-Version: V0.01
+Version: V0.02
 
 > English entry for international developers and third-party suppliers. See [`README.md`](./README.md) for the maintained baseline and verification commands.
 
@@ -13,7 +13,7 @@ A multi-domain management platform for offline entertainment complexes, covering
 | Layer | Technology | Evidence |
 |---|---|---|
 | Backend language | **Java 21** | `backend/pom.xml` |
-| Backend framework | **Spring Boot 4.1.0** (Web MVC / Data JPA / Security / Validation / Actuator / AOP) | `backend/pom.xml` |
+| Backend framework | **Spring Boot 4.1.0** (Web MVC / JDBC / Security / Validation / Actuator / AOP) | `backend/pom.xml` |
 | Database | **PostgreSQL** | `backend/pom.xml`, migration dir |
 | Migration | **Flyway** | `backend/src/main/resources/db/migration` |
 | Build | **Maven** (Maven Wrapper preferred, system Maven 3.9+ fallback) | `backend/README.md`, `tools/run-backend-maven.mjs` |
@@ -30,7 +30,7 @@ A multi-domain management platform for offline entertainment complexes, covering
 complex-management/
 ├── backend/                # Java 21 + Spring Boot 4 backend (Maven, Flyway migrations)
 ├── apps/
-│   ├── web/                # React + Vite + antd admin/customer web
+│   ├── web/                # React + Vite + antd admin web
 │   └── android-terminal/   # Android terminals (Gradle)
 ├── docs/                   # all product/dev/contract/acceptance/ops docs (entry: docs/00-...)
 │   └── openapi/            # machine-readable P0 contract: openapi-p0.yaml / openapi-p0.json
@@ -87,7 +87,7 @@ See **[`docs/architecture-overview-v0.1.md`](./docs/architecture-overview-v0.1.m
 
 ```mermaid
 graph LR
-  GUEST["Guest"] --> WEB["Web (admin/customer)"]
+  STAFF["Staff"] --> WEB["Web (staff)"]
   STAFF["Staff"] --> ANDROID["Android terminals"]
   WEB --> API["Backend API<br/>Java 21 + Spring Boot 4"]
   ANDROID --> API
diff --git a/README.md b/README.md
index f647ca80..a73d2078 100644
--- a/README.md
+++ b/README.md
@@ -1,6 +1,6 @@
 # 综合体管理系统（Entertainment Complex Management）
 
-版本：V0.01
+版本：V0.02
 
 > 文档goal(2026-06-20)：本 README 为仓库第一入口，供产品、开发、测试、运营、第三方供应商快速理解项目并上手。所有命令与数字均经实证（`ls`/`grep`/OpenAPI 统计/Surefire 报告/Playwright `--list`），不臆造。
 >
@@ -15,8 +15,8 @@
 | 层 | 技术 | 实证来源 |
 |---|---|---|
 | 后端语言/平台 | **Java 21** | `backend/pom.xml`（`<java.version>21</java.version>`） |
-| 后端框架 | **Spring Boot 4.1.0**（spring-boot-starter-parent 4.1.0；Web MVC / Data JPA / Security / Validation / Actuator / AOP） | `backend/pom.xml` |
-| 数据库 | **PostgreSQL** | `backend/pom.xml`（starter-data-jpa + jdbc）、迁移目录 |
+| 后端框架 | **Spring Boot 4.1.0**（spring-boot-starter-parent 4.1.0；Web MVC / JDBC / Security / Validation / Actuator / AOP） | `backend/pom.xml` |
+| 数据库 | **PostgreSQL** | `backend/pom.xml`（starter-jdbc）、迁移目录 |
 | 迁移 | **Flyway**（`spring-boot-flyway`） | `backend/pom.xml`、`backend/src/main/resources/db/migration` |
 | 构建 | **Maven**（P0 唯一构建工具，优先 Maven Wrapper，回退系统 Maven 3.9+） | `backend/README.md`、`tools/run-backend-maven.mjs` |
 | API 文档 | **springdoc-openapi 3.0.3** | `backend/pom.xml`（`<springdoc.version>3.0.3</springdoc.version>`） |
@@ -132,7 +132,7 @@ npm run verify:closed-loop-runtime      # 闭环运行态总验收（后端测
 
 ```mermaid
 graph LR
-  GUEST["客人 Guest"] --> WEB["前端 Web<br/>(admin/customer)"]
+  STAFF["员工 Staff"] --> WEB["前端 Web<br/>(业务人员)"]
   STAFF["员工 Staff"] --> ANDROID["Android 终端"]
   WEB --> API["后端 API<br/>Java 21 + Spring Boot 4"]
   ANDROID --> API
```

---

## 4. 逐块 / 逐行 ID 归因（闸 3）

B2 定稿最小边界（findings V0.39，队列「是」仅 018/025/026）：

- **M003-018**（技术性 / 是）：仅纠正 README.md:18–19 与 README.en.md:16 的 JPA/JDBC 事实；不改依赖。
- **M003-025**（冲突 / 是）：AGENTS.md:55 例外清单改为 HTML 封闭四样；第 56 行终端适用范围保留。
- **M003-026**（冲突 / 是）：删除两份 README 的 Guest→Web 与 admin/customer Web 标签，Web 使用者改为业务人员；英文第 90 行改为 STAFF→Web(staff)，第 33 行 admin/customer web→admin web；不新增 app 节点、不改真实应用代码。
- 三文件各自身 V0.01→V0.02，版本行归该文件正在修的已定稿 ID。

| # | 文件 | hunk | 删除行 | 新增行 | 归因 |
|---|---|---|---|---|---|
| 1 | AGENTS.md | `@@ -1,6 +1,6 @@` | `版本：V0.01` | `版本：V0.02` | **M003-025**（该文件本批唯一业务修复 ID 的版本递增；B2 明文要求各文件只 +1 到 V0.02） |
| 2 | AGENTS.md | `@@ -52,7 +52,7 @@` | `7. **所有展示文字全中文**，仅机读标识（枚举值、字段名、日志键）保留原文。` | `7. **所有展示文字全中文**，可以不用中文的只有单据号、设备编号、二维码内容、系统内部标识符四样。` | **M003-025** |
| 3 | README.en.md | `@@ -1,6 +1,6 @@` | `Version: V0.01` | `Version: V0.02` | **M003-018 + M003-026**（该文件正在修的两个是项；一次版本递增） |
| 4 | README.en.md | `@@ -13,7 +13,7 @@` | `… (Web MVC / Data JPA / Security / Validation / Actuator / AOP) …` | `… (Web MVC / JDBC / Security / Validation / Actuator / AOP) …` | **M003-018** |
| 5 | README.en.md | `@@ -30,7 +30,7 @@` | `# React + Vite + antd admin/customer web` | `# React + Vite + antd admin web` | **M003-026**（B2 指定第 33 行改法） |
| 6 | README.en.md | `@@ -87,7 +87,7 @@` | `GUEST["Guest"] --> WEB["Web (admin/customer)"]` | `STAFF["Staff"] --> WEB["Web (staff)"]` | **M003-026**（B2 指定第 90 行改法） |
| 7 | README.md | `@@ -1,6 +1,6 @@` | `版本：V0.01` | `版本：V0.02` | **M003-018 + M003-026** |
| 8a | README.md | `@@ -15,8 +15,8 @@` | `… Web MVC / Data JPA / …` | `… Web MVC / JDBC / …` | **M003-018** |
| 8b | README.md | 同上 hunk | `…（starter-data-jpa + jdbc）、迁移目录` | `…（starter-jdbc）、迁移目录` | **M003-018** |
| 9 | README.md | `@@ -132,7 +132,7 @@` | `GUEST["客人 Guest"] --> WEB["前端 Web<br/>(admin/customer)"]` | `STAFF["员工 Staff"] --> WEB["前端 Web<br/>(业务人员)"]` | **M003-026**（B2 指定第 135 行改法） |

上下文未改行（hunk 内空格开头的保留行）不计入变更，不需要独立 ID。

10 处精确替换 = 上表 1–9（其中 hunk 8 含两处）。无第 11 处。无二进制、无模式位、无新增/删除文件。

**未改动（两树 `git diff --binary` 为空，exit 0）：**

- 其余 5 个模块文件：`.env.example` `.gitignore` `.semgrepignore` `CONTEXT.md` `PROGRESS.md`
- 三份业务 HTML
- `backend/pom.xml`
- README.md `<!-- baseline:auto-begin -->` … auto-end 块：前后 tree 同行号 101–108，块 SHA256 均为 `62fd5c2af21ab3a0c53bdce18c2ef9a2d6ee24c3ce809e25722a05bb6c029e47`（684 字节 / 8 行）

闸 3：每一新增/删除行都能指到 findings.md 当前值为「是」的已定稿 ID。无无 ID 行。**越界闸 PASS。**

---

## 5. 第一性原理线（2.3 三问 + 四类证据）

适用冻结 18GEN 在本 diff 上逐条见 §8。本线把三处修复追到不可再拆事实。

### 5.1 M003-018 — 为什么必须改 README 的 Data JPA 声明

- **删掉这处纠正，哪条做不到？** 不是 HTML 业务条目，是文档声称与可复现构建声明不一致（GEN-013 / GEN-016）。不改则入口 README 继续把未声明的 Data JPA 写成当前事实。
- **声称行为是否验证？** 类型 4：实读 `backend/pom.xml`。
  - `<artifactId>spring-boot-starter-parent</artifactId>` / `<version>4.1.0</version>`
  - `<java.version>21</java.version>`
  - 存在 `spring-boot-starter-jdbc`、`spring-boot-starter-webmvc`、`spring-boot-starter-security`、`spring-boot-starter-validation`、`spring-boot-starter-actuator`
  - `data-jpa` / `starter-data-jpa` 命中 0；全文件 `jpa` 大小写不敏感命中 0
- **未验证就当真的前提？** 无。修复未改 pom、未新增 JPA、未把版本号一致拿来掩盖组件差异。与 B2「不改依赖」一致。
- 分类复核：技术性、无业务发明（只陈述已声明框架组件）。GEN-017 通过。

### 5.2 M003-025 — 为什么必须改 AGENTS.md 中文例外

类型 1：`docs/platform-governance-map-for-operations-v0.1.html`

- **PGM287**（文件第 287 行，全局原则·界面全中文）：「可以不用中文的只有四样，没有第五样：单据号、设备编号、二维码内容、系统内部标识符。」并写明字段名、状态、枚举取值必须中文，不得以“这是标识”为由保留英文。
- **PGM965**（文件第 965 行，已拍板）：「可以不用中文的只有单据号、设备编号、二维码内容、系统内部标识符四样，此外一律中文；强制规则，适用全部终端与模块。」
- 旧文「仅机读标识（枚举值、字段名、日志键）保留原文」把 HTML 明确要求中文的枚举/字段名列成例外，与 287/965 冲突。
- 新文与 965 封闭四样逐字对齐；第 56 行「后台、客人端、设备端、安卓端一律适用」保留，与 287「全终端覆盖」及 965「全部终端」同向。
- **未验证前提？** 无第五样被引入。未改三 HTML。

### 5.3 M003-026 — 为什么必须删 Guest→Web

类型 1：同 HTML **PGM739**（文件第 739 行）：「受控的十一类终端：客人手机应用、客人小程序、Web 前端（业务人员，客人无网页端）、……」

- 旧图 `GUEST → WEB(admin/customer)` 肯定「客人使用名为 Web 的网页入口」，与「客人无网页端」直接相反。
- 新图 Web 使用者改为「业务人员」/ `staff`，复用已有 STAFF 节点连到 WEB，不新增终端种类、不新增 app 目录、不改 `apps/web` 源码。
- 英文目录注释 `admin/customer web`→`admin web` 是 B2 已写死的最小改法，不是新业务入口。
- Guest 节点因只靠被删的那条边存在，边删除后节点消失；未发明 Guest→小程序 等新边（那会越界扩图）。

### 5.4 after tree 与工作区三文件

`git cat-file -p after:AGENTS.md|README.md|README.en.md` 的 SHA256 与工作区文件全等（`ea470f25…` / `28a46aea…` / `dd83a294…`）。本席审的 after tree 即当前三文件内容。

---

## 6. 顺从式线（假定修复意图正确，核 B2 最小边界）

| B2 要求 | 实证 | 结果 |
|---|---|---|
| README.md:18 Data JPA→JDBC | after 第 18 行含 `Web MVC / JDBC /`，三文件内 `Data JPA`/`data-jpa`/`GUEST`/`admin/customer`/`机读标识` 命中 0 | 满足 |
| README.md:19 starter-data-jpa + jdbc→starter-jdbc | after 第 19 行 `（starter-jdbc）` | 满足 |
| README.en.md:16 Data JPA→JDBC | after 第 16 行 `Web MVC / JDBC /` | 满足 |
| 不改 pom/依赖 | 两树对 `backend/pom.xml` diff 空 | 满足 |
| AGENTS.md:55 改为四样原文 | after 第 55 行与 B2 指定替换句一致 | 满足 |
| AGENTS.md:56 保留 | 仍为「后台、客人端、设备端、安卓端一律适用。」 | 满足 |
| README.md:135 GUEST→WEB 改为 STAFF→WEB(业务人员)，保留下一行 STAFF→ANDROID | after 135–136 行正是如此 | 满足 |
| README.en.md:90 同改 STAFF→Web(staff) | after 第 90 行 | 满足 |
| README.en.md:33 admin/customer web→admin web | after 第 33 行 | 满足 |
| 不新增 app 节点 / 不改产品源码 | diff 仅 3 个根文档；无 `apps/`、无 Java/TS | 满足 |
| 三文件各 V0.01→V0.02 一次 | 各文件第 3 行 | 满足 |
| 023 删除 / 024 保留已生效且本批不动 CONTEXT | CONTEXT.md 两树 diff 空；questions 首行已是删除/保留 | 满足 |
| 队列否的 23 项不动 | 无对应 hunk | 满足 |

18GEN 顺从核对见 §8。文档类按 runbook §5：不强造后端/DB 全测；用 HTML 对照、路径存在、`git diff --check`、结构解析。

`git diff --binary --check <before> <after>` exit 0。

交接称 `node tools/refresh-entry-doc-baseline.mjs --check`、`verify:entry-anchors`、`verify:html-ledger-sync` 亦 exit 0。本席评估：auto 块与三 HTML 均不在本 diff 内，这些命令不能证明 018/025/026 的替换句，**不采信为本次 PASS 证据**。本席未复跑它们。

---

## 7. 对抗式线（grilling 结构，自答，不等操作员）

Q1 正常路径是否失败？
- 读 AGENTS 铁律 7 的代理会把四样当封闭例外，不再把枚举/字段名当可英文展示。与 PGM287/965 同向。
- 读 README 技术栈的人会找 `starter-jdbc` 而非 JPA；pom 确有 jdbc、无 jpa。
- mermaid：同一节点 ID `STAFF` 出现两行是 graph LR 的合法合并，结果为 Staff→Web 与 Staff→Android，不是第二名员工角色。未跑 mermaid CLI（避免写产物）；结构解析足够。
- **不构成缺陷。**

Q2 是否过度改动？
- 10 替换 / 9 hunk / 3 文件，与 B2 清单一一对应。
- 未扩十一端、未改 CONTEXT/PROGRESS/ignore/HTML/pom/apps。
- 同句保留 AOP：018 只要求 Data JPA→JDBC，扩修 AOP 会无 ID。ponytail-review：Lean already. Ship.
- **不构成缺陷。**

Q3 是否越界 / 无 ID？
- 见 §4。版本行由 B2 写进「各文件正在修的 ID」。README 中英各含 018+026，一次 +0.01 不能拆成两次版本行。
- **无无 ID 行。**

Q4 是否业务发明？
- 「业务人员」「系统内部标识符」来自 HTML 原文，不是新角色/新例外。
- `admin web` 是 B2 指定英文树注释，不是新客人网页端。
- 未把商户门户/经销商 Web 画进本图（那会新增节点，越界）。
- **无业务发明。**

Q5 是否修不完整（finding 未落地）？
- 018 三处指定行均改。
- 025 指定句均改，56 行保留。
- 026 中英 mermaid 与英文树注释均改。中文目录树本就是「管理端」、不含 admin/customer，B2 未要求改那一行。
- 三文件内旧字符串 `GUEST` / `admin/customer` / `Data JPA` / `data-jpa` / `机读标识` 现命中 0。
- **未修残。**

Q6 是否把冲突写成技术性或反过来？
- 018 仍是技术性（组件事实）。025/026 仍是冲突（对 HTML 封闭四样 / 客人无网页端）。本 diff 未改分类。
- **无。**

Q7 README.md 目录「管理端」与 mermaid「业务人员」并存是否新冲突？
- 「管理端」不在本 diff。B4 只审这次 diff。残留表述交给 B5 整模块，不能在 B4 扩成无 ID 修复。
- **本席不升 GR 缺陷。**

---

## 8. 冻结 18GEN 在本 diff 上的适用结论

M003 映射只有 GEN-001…018。阶段 B 不启用锁外判据。

| ID | 对本 diff | 结论 |
|---|---|---|
| GEN-001 必要性与最小性 | 10 处均为三 ID 所需；无额外抽象 | PASS |
| GEN-002 可执行正确性 | 文档声称 JDBC 与 pom 一致；mermaid 结构可解析。文档不强造运行测试 | PASS |
| GEN-003 测试有效性 | 文档修复豁免；未新增测试、未伪称测过 | N/A（豁免，不挡） |
| GEN-004 信任边界 | 无输入面变更 | N/A |
| GEN-005 授权 | 无 | N/A |
| GEN-006 数据完整性 | 无 | N/A |
| GEN-007 并发幂等 | 无 | N/A |
| GEN-008 接口/模式一致 | README 组件名与 pom artifactId 对齐（JDBC vs jdbc；Web MVC vs webmvc） | PASS |
| GEN-009 错误处理 | 无 | N/A |
| GEN-010 安全 | 无秘密、无注入面 | N/A |
| GEN-011 可观测 | 无 | N/A |
| GEN-012 资源 | 无 | N/A |
| GEN-013 构建配置与依赖 | 只改文档声明，pom 字节未动；声明现与真实依赖一致 | PASS |
| GEN-014 迁移 | 无 SQL | N/A |
| GEN-015 界面语言约定 | AGENTS 展示例外与 HTML 语言约定一致 | PASS |
| GEN-016 文档可验证性 | JDBC/四样/业务人员均可由 pom 或 HTML 复现；auto 块未手改 | PASS |
| GEN-017 业务来源分类 | 018 技术性无发明；025/026 冲突删除；无新添加项 | PASS |
| GEN-018 第一性原理证据 | 结论落到 HTML 原文与 pom/命令输出，不用历史 PASS 或席位意见 | PASS |

适用项无 FAIL。N/A 项本 diff 无作用面，不视为未审。

---

## 9. 证据命令与输出摘要（本席实跑）

1. `sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`
2. sidecar / ledger 顶部同值；COUNT=0
3. `sha256sum .git/index` → `6a25d7b677e4dc3eb39c076f8e6a204799c1a92693734b773194a036ad59561b`
4. `git cat-file -t` 两 SHA 均为 `tree`
5. `git diff --binary <before> <after>` → 4996 字节，SHA256 `7c02b71c6fd9665ccbfbe2b6be7c2642e94f9be6c07fdbb180d66c7ba446420e`；`grep -c '^@@'`=9；`grep -c '^diff --git'`=3
6. `--name-only`：AGENTS.md / README.en.md / README.md
7. `--numstat`：2/2、4/4、4/4
8. `--check` exit 0
9. 其余 5 模块文件、三 HTML、pom 的两树 diff 为空
10. after blob SHA256 = 工作区三文件 SHA256
11. pom：parent 4.1.0；java 21；有 starter-jdbc / webmvc / security / validation / actuator；data-jpa=0
12. `test -f backend/pom.xml`、`test -d backend/src/main/resources/db/migration` 真
13. PGM287/965/739 原文实读（见 §5）
14. findings 队列表 26 行：是 3 / 否 23
15. questions 023/024 答复首行 删除 / 保留
16. 三源文件内旧错误串命中 0
17. README auto 块前后 tree 哈希相同

未跑：后端测试、Maven、psql、全仓 rg、verify:entry-anchors、verify:html-ledger-sync、baseline --check。理由：文档修复 + 那些入口不在本 diff 证明链上。

---

## 10. GR 临时发现

**缺陷类 GR 发现：无。**

观察（不构成 FAIL、不分配新 ID、不要求本轮回滚）：

- GR-OBS-01：中英 mermaid 在改写 Guest 边后，`STAFF[…]` 节点文本在相邻两行各出现一次。此为 mermaid 同 ID 合并写法，B2 要求保留下一行 STAFF→ANDROID。非缺陷。
- GR-OBS-02：README.md 目录树「管理端」不在本 diff / 不在 026 最小清单。B4 不升为无 ID 修复项。
- GR-OBS-03：被 018 编辑的同一单元格仍列出 AOP；pom 用 aspectjweaver 而非 starter-aop。018 边界只替换 Data JPA。扩修会越界。非本批缺陷。

---

## 11. 闸与总体结论

| 闸 | 结果 | 依据 |
|---|---|---|
| 铁律 4 三 HTML | 未改 | 两树 HTML diff 空 |
| 闸 3 逐行归因 | **PASS** | §4 全部 ± 行落到 018/025/026 |
| 无 ID 改动 | 无 | 有则必须 FAIL；未发现 |
| 18GEN 适用项 | **PASS** | §8 |
| 三线 | 已完整执行 | §5–7 |
| 未答 questions | 无 | 023/024 已答 |
| 越界扩锁 | 无 | 未引用锁外判据 |
| B4 越界闸 | **PASS** | |
| **本席 B4-r2 总体** | **PASS** | |

本席不修源、不回退、不改 findings/ledger/lock。B4 双 PASS 之后的出队与 B5 由调度席在合取后处理。本文件为 V0.01 完整报告，不是进度条。
