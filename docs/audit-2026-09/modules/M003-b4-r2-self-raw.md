CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
# M003 B4-r2 本方完整修复内容审计

版本：V0.01

时间：2026-09-05T11:30:40.193058+00:00

本方结论：B4 PASS；闸3逐行归因 PASS。完整4996字节/9hunk/3文件审完后给票，无新CX发现。先派Grok后本方独立审完并写本raw；未读Grok本次raw或原始JSON。另一席票未知，不以本方PASS收口、出队或开始B5。

模块M003/322，IN_PROGRESS，B3累计2；控制V0.17；findings V0.39已双PASS定稿。本批018/025/026是，其他23项否。before tree=6318ad55b33dc901d19038d1d643bbb483c72f7a；after tree=fbe3b6b9726663cd0ab40bcffd405ebd9c1e1110。两树由全路径git add -A和临时不存在索引write-tree构造，不使用HEAD~1或路径过滤。完整diff SHA256=7c02b71c6fd9665ccbfbe2b6be7c2642e94f9be6c07fdbb180d66c7ba446420e，4996字节。真实索引SHA保持6a25d7b677e4dc3eb39c076f8e6a204799c1a92693734b773194a036ad59561b。

## 本轮完整原始diff

````diff
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
````

## 全9hunk逐块归因（包含全部新增/删除行）

|文件|hunk|唯一归因|理由|
|---|---|---|---|
|AGENTS.md|@@ -1,6 +1,6 @@|M003-025|文件自身版本V0.01→V0.02；归该文件本批修复ID|
|AGENTS.md|@@ -52,7 +52,7 @@ single-context 布局，词表在仓库根的 `CONTEXT.md`。详见 `docs/agents|M003-025|展示语言例外清单对齐PGM封闭四样|
|README.en.md|@@ -1,6 +1,6 @@|M003-018|文件自身版本V0.01→V0.02；归该文件本批修复ID|
|README.en.md|@@ -13,7 +13,7 @@ A multi-domain management platform for offline entertainment complexes, covering|M003-018|将不实JPA组件名改为POM实际JDBC；不新增依赖|
|README.en.md|@@ -30,7 +30,7 @@ A multi-domain management platform for offline entertainment complexes, covering|M003-026|删除客人Web肯定关系/customer标签，改业务人员Web；其余边保持|
|README.en.md|@@ -87,7 +87,7 @@ See **[`docs/architecture-overview-v0.1.md`](./docs/architecture-overview-v0.1.m|M003-026|删除客人Web肯定关系/customer标签，改业务人员Web；其余边保持|
|README.md|@@ -1,6 +1,6 @@|M003-018|文件自身版本V0.01→V0.02；归该文件本批修复ID|
|README.md|@@ -15,8 +15,8 @@|M003-018|将不实JPA组件名改为POM实际JDBC；不新增依赖|
|README.md|@@ -132,7 +132,7 @@ npm run verify:closed-loop-runtime      # 闭环运行态总验收（后端测|M003-026|删除客人Web肯定关系/customer标签，改业务人员Web；其余边保持|

未归因变更行=0。三个源文件仅10处定稿精确替换；整diff没有其它tracked/staged/untracked路径。025的AGENTS版本行归025；两README版本行归018，这是文件实际内容更新的必要附随版本进位，不是新增无ID修复。CONTEXT不变，已撤回019未偷偷改；024保留段、两ignore、.env、PROGRESS和三HTML保持；README自动基线块字节相同。

## 三线逐项审计

### 018 技术栈组件事实

第一性：README中DataJPA/starter-data-jpa声称来自pom；当前backend/pom.xml实际声明spring-boot-starter-jdbc。修复后三处均为JDBC/starter-jdbc，与直接构建定义一致（类型4），不把读取pom冒充执行JPA编译失败测试。必要性是保证新开发者按当前依赖选持久化接口；删除不实声明无需新增库。

顺从：GEN-008/013/016/017/018；仅两README的三处旧组件文本及文件版本更改，Java21/Boot4.1.0/PostgreSQL/AOP等原字段保留，与B2一致。

对抗：没有为让文档成真安装JPA；没有把PostgreSQL写成JDBC数据库类型；框架列表JDBC为既有组件，数据库行用starter-jdbc作pom实证。未改Maven文件、不改变启动路径，无需新后端测试。无业务发明：框架事实不定义业务角色/收费/状态/权限。

### 025 展示文字四样例外

第一性：PGM287与965分别给出全局原则和已拍板条目，两处封闭清单恰为单据号、设备编号、二维码内容、系统内部标识符。AGENTS55修复后逐字列这四样，保留中文总要求和下一行全端适用（类型1/4）。

顺从：GEN-015/016/017/018；按B2只改55例外及3版本，不改配置主数据、账户注销、测试/发布规则。

对抗：新句不把枚举值/字段名作为额外可展示英文例外；系统内部标识符这一HTML允许项保留，不要求代码常量全翻译。只有四样意味着没有第五样，无新的业务发明，也没有把开发说明英文文档当产品终端强制翻译。无需修改权威HTML或重新询问用户。

### 026 客人Web错误关系

第一性：PGM739明确客人手机应用/小程序以及业务人员Web，客人无网页端。当前两个图首边均为STAFF→WEB，英文目录注释admin web，已不再肯定消费客人使用customer Web。图是摘要，不要求列全11端；删除错误Guest边不发明App节点（类型1/4）。

顺从：GEN-008/016/017/018；中文135、英文33/90恰按B2定稿最小边界，保留员工Android、Web→API、Android→API、DB/EXT及典型写入/认证例外。

对抗：两个STAFF声明的同一ID与同一标签一致，不产生两个不同员工节点；只有原图ID/文字变化，箭头/引号/括号/围栏结构仍完整。未进行真实Mermaid渲染，不把静态检查冒充图形运行；现有Markdown链接/文档门禁通过。没有将客人改成管理员业务角色或改应用权限代码，只给现有业务人员Web连接正确使用者。英文文档的Staff是研发翻译，非产品界面英文例外。

## 冻结判据适用覆盖

GEN-001最小性：没有新终端/依赖/抽象；GEN-002入口正确性：静态文本/图结构，不新增程序入口；GEN-003测试有效性：不为可逆说明改动写镜像测试，不声称后端全测；GEN-004信任边界：无输入处理新增；GEN-005授权：不从客人错误边反向授权客人Web；GEN-006/007：无状态/事务/并发逻辑变化；GEN-008一致性：三缺陷逐项根因修复；GEN-009错误处理：无执行错误路径变更；GEN-010安全：明确展示例外但不修改秘密；GEN-011证据：两树全diff与逐块归因；GEN-012资源：不新建运行资源；GEN-013依赖：仅文案JDBC，与pom吻合；GEN-014迁移：无新增；GEN-015展示：PGM四样闭集；GEN-016路径/版本/文档真实性：V0.02各进一位、基线门禁通过；GEN-017分类：018技术缺陷、025/026冲突，均既有上位/现有构建事实；GEN-018：下方直接原文/命令，非历史PASS替代依据。

## 直接证据与实跑检查

PGM:287:           <p class="lead" style="margin-top:1.2rem"><strong>全局原则 · 界面全中文（所有终端零英文）</strong>：平台所有终端——后台管理端、客人手机端、现场平板与设备端、安卓终端——展示给用户的一切文字，包括菜单、页面标题、字段名、状态、枚举取值、按钮、提示、报错、导出文件表头与内容、短信与通知、日期时间等，<strong>必须全部为中文，不得出现任何英文</strong>（如 active/draft 英文状态、mm/dd/yyyy 英文日期格式、英文按钮或提示）。可以不用中文的只有四样，没有第五样：单据号、设备编号、二维码内容、系统内部标识符。这四样之外，凡是展示给人看的文字一律中文，不得以“这是标识”为由保留英文。<strong>此为强制规则，适用平台全部终端与模块，新增页面与功能一并遵守。</strong></p>

PGM:739:                 <li>受控的十一类终端：客人手机应用、客人小程序、Web 前端（业务人员，客人无网页端）、工作人员平板、员工工作手机、员工私人手机、商户网页门户、商户手机应用、触摸屏一体机、移动管理端、独立经销商 Web 门户。经销商门户适用与其他终端相同的后台开关、权限、数据范围、访问拒绝留痕和兜底关闭规则，并与商户门户及内部员工端隔离账号、权限和数据。</li>

PGM:965:             <div class="question confirmed">已拍板：界面全中文（所有终端零英文）——后台/客人端/现场设备/安卓端展示给用户的一切文字（菜单、字段、状态、枚举、按钮、提示、报错、导出、短信、日期时间）必须全中文，不得出现英文；可以不用中文的只有单据号、设备编号、二维码内容、系统内部标识符四样，此外一律中文；强制规则，适用全部终端与模块。</div>

backend/pom.xml:60:             <artifactId>spring-boot-starter-jdbc</artifactId>

下列为本批B3-r2紧邻执行的真实命令/输出，输入字节在本B4已与after tree及当前工作区逐文件核对相同，不重复假造测试：

````text
BEFORE_TREE=6318ad55b33dc901d19038d1d643bbb483c72f7a
FOUR_BEFORE=["23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de", "23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de", "23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de", "23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de"]
REAL_INDEX_SHA256=6a25d7b677e4dc3eb39c076f8e6a204799c1a92693734b773194a036ad59561b
SOURCE_WRITES=["AGENTS.md", "README.en.md", "README.md"]
{"cmd": ["node", "tools/refresh-entry-doc-baseline.mjs", "--check"], "exit": 0, "stdout": "[entry-doc-baseline] PASS: 快照与实际一致（720 ops / V385（363 迁移））。\n", "stderr": ""}
{"cmd": ["npm", "run", "verify:entry-anchors"], "exit": 0, "stdout": "\n> entertainment-complex-management@0.1.0 verify:entry-anchors\n> node tools/verify-entry-anchors.mjs\n\n[entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1563 条；被检 Markdown 353 份；条目名引用 1517 处（注册表原名 1358 处，正文唯一定位 159 处）。\n[entry-anchors] PASS：条目名唯一、注册表条目正文逐字与 HTML 现文一致、引用可唯一定位、正文无行号锚点、账本正文与九个镜像列均与 HTML 现文一致、账本与承接矩阵一一对应。\n", "stderr": ""}
{"cmd": ["npm", "run", "verify:html-ledger-sync"], "exit": 0, "stdout": "\n> entertainment-complex-management@0.1.0 verify:html-ledger-sync\n> node tools/verify-html-ledger-sync.mjs\n\n[html-ledger-sync] PASS: 本次未改动业务最终解释权 HTML。比对范围：提交区 origin/main...HEAD（工作区三份 HTML 无未提交改动）。\n", "stderr": ""}
{"cmd": ["git", "diff", "--check"], "exit": 0, "stdout": "", "stderr": ""}
AFTER_TREE=fbe3b6b9726663cd0ab40bcffd405ebd9c1e1110
FULL_DIFF_SHA256=7c02b71c6fd9665ccbfbe2b6be7c2642e94f9be6c07fdbb180d66c7ba446420e
FULL_DIFF_BYTES=4996
SOURCE_MANIFEST={".env.example": {"sha256": "e377b54d9cd829ddeed30e0ae730f55d51fbc8c272f3df5ca611fdbdba125049", "bytes": 893, "lines": 22}, ".gitignore": {"sha256": "75861ded5d3859bcda593aafe3f3862e765470e7f8b6636a643bf59de9d7c51b", "bytes": 1199, "lines": 55}, ".semgrepignore": {"sha256": "45f3dbc9f36891b4b94d2b641fabfd66db81df59b642bcde80a29bece24f1aba", "bytes": 253, "lines": 6}, "AGENTS.md": {"sha256": "ea470f256c8e9f1e23e9b346d1b3828f2e4e1edd84e9ea83d7564725759bfcf0", "bytes": 3695, "lines": 74}, "CONTEXT.md": {"sha256": "d5acdb1c3eb174c532983c823931d1cbe36444712e379b755991243f75df52b7", "bytes": 10197, "lines": 163}, "PROGRESS.md": {"sha256": "91eaedfc0c8428514355b82a79bd1b6add257bc80269e831d2e76aafbc0baf9d", "bytes": 3246, "lines": 36}, "README.en.md": {"sha256": "dd83a2943d401ec25bfa2043ef4762662c78d1526763eb3faf2cc20008d092db", "bytes": 7470, "lines": 116}, "README.md": {"sha256": "28a46aea13227b0e3e101f9dc0a2970b6e3e581ca5995d291288aa59cb2d23b5", "bytes": 10854, "lines": 149}}
LEDGER=V0.83 B3=2 STATE=IN_PROGRESS
````

## 结论

本方B4 PASS、越界闸PASS，3项修复均按定稿最小范围解决，9hunk全部归因，零新finding。源码在B4只读，报告写入不属于B3源diff。Grok票未知，必须等本轮完整Grok raw与真实exit code；双PASS才将3项出队，之后仍须完整B5-r2双审621行，不能直接模块PASS或发布。
