CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M003 B4-r1 修复 diff 独立复审（Grok 席乙）

版本：V0.01（新文件）
席位：Grok（审计席乙）
步骤：B4-r1 修复内容完整独立审计
模块：M003 `01-root-config-and-guidance-01` / 共 322；控制提示词 V0.17；ledger 本模块 `IN_PROGRESS`；B3 已执行次数=1
本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`（独立实算，未抄 sidecar / ledger / 派发期望值）
实算值：`23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`
sidecar 同行：`23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`
ledger 顶部同行：`CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`；`LOCK-AMEND-COUNT: 0`
互盲：未打开 `docs/audit-2026-09/modules/M003-b4-r1-self-raw.md` 正文，未采信 Codex 本次 B4 结论。一次 `COMPLEX_KEYVAULT_PII_KEY` 全仓检索曾打印该路径的一行 diff 片段，与本席已独立实算的冻结 diff 相同，不作对方结论。已读 findings V0.36、criteria-lock、runbook、M003 文件清单、三份业务 HTML 直系原文。
写入约束：只写本文件；不改源码/测试/配置/HTML/findings/ledger/锁/计划；不派子代理；不提交推送部署；不写真实数据库；不进 B5/下一模块。
冻结树：before=`a339075ff27c89319739c4ca4e9fb802b31f376c`；after=`18e0fe08e0b08eb55f1390c93ddd0f92d6625cc7`（`git cat-file -t` 均为 `tree`，非 HEAD）
未过滤 binary diff SHA256=`c5d7661649fbabd3b003394242e2a4069d36251e994a9b5bc5b43255a4c92a55`（19006 字节 / 292 行 / 22 个 `@@` / 6 源文件）
本席 B4 结论：**PASS**

---

## 1. 本环境技能预检（实际核对，非声称）

开工前按 V0.17 §6 在本机路径实列目录并读对应 `SKILL.md`。B4 只审两树 diff，禁止修复、禁止子代理、禁止用 `HEAD~1` 替代两树。

### 1.1 已核到且本节点按收窄口径使用

| 技能 | 实核路径 | 本节点用法 |
|---|---|---|
| `using-superpowers` | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能自检 |
| `requesting-code-review` | `.../superpowers/6.3.0/skills/requesting-code-review/SKILL.md` | B4 请审=本席自审两树 diff；**不派子代理、不用 `HEAD~1`** |
| `verification-before-completion` | `.../superpowers/6.3.0/skills/verification-before-completion/SKILL.md` | PASS/FAIL 前先跑命令并贴输出 |
| `grilling` / `grill-me` | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` 与 `grill-me/SKILL.md` | 对抗式+第一性原理追问结构；**不等操作员、不派子代理**，自问自答 |
| `systematic-debugging` | `.../superpowers/6.3.0/skills/systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流；B4 **不改代码** |
| `diagnosing-bugs` | `.../mattpocock-skills/1.2.3/skills/engineering/diagnosing-bugs/SKILL.md` | 以两树 binary diff + 文档门禁 + HTML 逐字对照作红信号 |
| `codebase-design` | `.../mattpocock-skills/1.2.3/skills/engineering/codebase-design/SKILL.md` | 删除测试：本 diff 只改词表/入口说明，不新增模块缝 |
| `writing-for-agents` | `.../mattpocock-skills/1.2.3/skills/productivity/writing-for-agents/SKILL.md` | 本 raw 按下游可差集、可归因来写 |
| `ponytail-review` | `.../ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 找可再删的越界改动；B4 只列不修 |
| `ponytail` | `.../ponytail/4.9.0/skills/ponytail/SKILL.md` | 只评判本批是否偏胖，不修 |
| `i-have-adhd` | `.../i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 编号与直述；**首行格式以控制「独占 CRITERIA-LOCK-SHA256」为准**，覆盖该技能「首行即下一步动作」 |
| knowledge-work `code-review` | `.../knowledge-work-plugins/engineering/1.2.0/skills/code-review/SKILL.md` | 只借正确性维度；不派第三席 |

### 1.2 已核到、本节点不适用（不冒充已调用）

| 技能 | 原因 |
|---|---|
| `receiving-code-review` | 本席是出具 B4 意见的一方，不是收对方意见后去 IMPLEMENT |
| `brainstorming` / `writing-plans` | 阶段 A 已完成 |
| `test-driven-development` / `tdd` | 本批是文档/配置；runbook 文档类不强制落测试 |
| `resolving-merge-conflicts` | 无合并冲突 |
| `git-guardrails-claude-code` | 可选；本体要问人装 hook，本席不装、本批无提交 |
| `executing-plans` / `dispatching-parallel-agents` / `subagent-driven-development` / `using-git-worktrees` / `finishing-a-development-branch` | 提示词划掉 |
| `ponytail-audit` / `ponytail-debt` / `ponytail-gain` | 提示词划掉 |
| `research` / `domain-modeling` / mattpocock `code-review` | 提示词划掉 |
| Arc `review` / `audit` | 会派子代理，与双席冲突 |

### 1.3 缺口（未核到，不得冒充已调用）

| 技能 | 结果 |
|---|---|
| `batch-grill-me` | **缺口**。`mattpocock-skills/1.2.3/skills/productivity/` 下无此目录 |
| `ubiquitous-language` | **缺口**。全树无对应 `SKILL.md` |

对抗式追问改用已核到的 `grilling` 结构自问自答。

---

## 2. 范围、两树真实性

对象：`git diff --binary a339075ff27c89319739c4ca4e9fb802b31f376c 18e0fe08e0b08eb55f1390c93ddd0f92d6625cc7`，无路径过滤，不用 HEAD 差替代。

### 2.1 对象类型与计数

```text
git cat-file -t a339075ff27c89319739c4ca4e9fb802b31f376c
# tree
git cat-file -t 18e0fe08e0b08eb55f1390c93ddd0f92d6625cc7
# tree
git ls-tree -r --name-only <before> | wc -l
# 3503
git ls-tree -r --name-only <after> | wc -l
# 3503
comm -3 <(sort before names) <(sort after names)
# （空）
```

`--raw` 仅 6 个 `M`，模式均 `100644`→`100644`，无增删改名：

```text
:100644 100644 7944bb76 73ec4e6b M	.env.example
:100644 100644 2c1f9327 ded7f72a M	AGENTS.md
:100644 100644 14f81928 45534fc5 M	CONTEXT.md
:100644 100644 12ab936f 6bf56366 M	PROGRESS.md
:100644 100644 081ec3dc c4140398 M	README.en.md
:100644 100644 f9f06d08 f647ca80 M	README.md
```

`--shortstat`：`6 files changed, 47 insertions(+), 51 deletions(-)`
`--numstat`：`.env.example 2/8`；`AGENTS.md 5/4`；`CONTEXT.md 22/22`；`PROGRESS.md 5/2`；`README.en.md 8/11`；`README.md 5/4`

模块 8 文件中保持 blob 不变：`.gitignore`=`3f2ccfd2c6a957b2a9e25e2ad252be2634cb1f05`；`.semgrepignore`=`76b7a11c30b508648c9ea22153663d6117fa13ad`。
三份业务 HTML before/after blob 全等。
工作区 6 个改动文件 sha256 与 `git cat-file -p AFTER:path` 全等。

独立实算 `sha256(diff)` = `c5d7661649fbabd3b003394242e2a4069d36251e994a9b5bc5b43255a4c92a55`，`wc -c`=19006，与派发冻结字节一致。UTF-8 严格解码成功，无 `U+FFFD`、无 NUL。`git diff --check` exit 0。

---

## 3. 完整未过滤 diff 全文

以下为 `git diff --binary --no-color --no-ext-diff <before> <after>` 原文（19006 字节）：

```diff
diff --git a/.env.example b/.env.example
index 7944bb76..73ec4e6b 100644
--- a/.env.example
+++ b/.env.example
@@ -1,3 +1,4 @@
+# 版本：V0.01
 SPRING_PROFILES_ACTIVE="local"
 COMPLEX_API_PORT="8080"
 COMPLEX_API_HOST="127.0.0.1"
@@ -11,15 +12,8 @@ COMPLEX_DATABASE_MIN_IDLE="2"
 COMPLEX_API_VERSION="0.1.0"
 COMPLEX_API_BUILD_TIME="local"
 
-JWT_SECRET="replace-with-at-least-32-random-characters"
-CORS_ALLOWED_ORIGINS="http://localhost:5173,http://127.0.0.1:5173"
-ACCESS_TOKEN_TTL_SECONDS="900"
-USER_ACCESS_TOKEN_TTL_SECONDS="3600"
-AUTH_FAILURE_LOCK_THRESHOLD="3"
-
 # PII 字段加密密钥（AES-256-GCM）。安装期由 U 盘 provision 写入本地/环境；运行态不依赖 U 盘。
-# 生产必须为强随机、>=32 字符且不含 change-me（留空则用 dev 默认，仅限本地/测试）。
-COMPLEX_KEYVAULT_PII_KEY=""
+# 生产必须为强随机、>=32 字符且不含 change-me；仅本地/测试可不设置该变量以使用 dev 默认。
 
 # 本地临时管理员账号（仅 local profile 自动种子，平台管理员权限）
 # 沿用项目约定变量名，前端真实后端 UI 联调与 Android P0 smoke 共用
diff --git a/AGENTS.md b/AGENTS.md
index 2c1f9327..ded7f72a 100644
--- a/AGENTS.md
+++ b/AGENTS.md
@@ -1,5 +1,7 @@
 # AGENTS.md
 
+版本：V0.01
+
 ## Agent skills
 
 ### Issue tracker
@@ -47,8 +49,8 @@ single-context 布局，词表在仓库根的 `CONTEXT.md`。详见 `docs/agents
 
    PRD、开发设计、接口、数据库、UI、测试和实现均不得反向改写其中定义。
 
-6. **删除一律软删除**：存库 + 前台不显 + 可恢复 + 分类 + 填理由 + `role_platform_admin`
-   批准。唯一例外是 PIPL 约束下的人脸照。
+6. **配置类主数据删除走审批、软删除**：先存档、填理由，由具备最高权限的人审批，申请人不得自审；存库、前台不显、可恢复、分类并留痕。
+   账户注销等个人信息删除按三份业务 HTML 的专项生命周期规则执行。
 
 7. **所有展示文字全中文**，仅机读标识（枚举值、字段名、日志键）保留原文。
    后台、客人端、设备端、安卓端一律适用。
@@ -62,8 +64,7 @@ single-context 布局，词表在仓库根的 `CONTEXT.md`。详见 `docs/agents
    造成版本乱序漂移，翻过两次车。
    机器闸门：`npm run verify:migration-version-order`（已进 `verify:delivery-gates`）——
    本分支新增的迁移 V 号必须大于 `origin/main` 当前最大 V 号，否则红。
-   未挂 `verify:predeploy`：GitHub Actions 用 `actions/checkout` 默认浅克隆，
-   PR 分支上取不到 `origin/main` ref，挂上去会在 CI 里恒红。
+    未挂 `verify:predeploy`。
 
 10. **跑测试只 export 三个 DB 环境变量**（`COMPLEX_DATABASE_URL` / `_USERNAME` /
     `_PASSWORD`），**不要 `source` 整个 env 文件**。测试库是 `complex_test`，
diff --git a/CONTEXT.md b/CONTEXT.md
index 14f81928..45534fc5 100644
--- a/CONTEXT.md
+++ b/CONTEXT.md
@@ -1,5 +1,7 @@
 # 业务上下文与规范术语
 
+版本：V0.01
+
 状态：当前有效
 业务最终解释权：
 
@@ -9,8 +11,6 @@
 
 本文只收录已被上述 HTML 或用户裁决明确的业务术语。PRD、开发设计、接口、数据库、UI、测试和实现不得反向改写这些定义。
 
-会员、成长值、积分、兑换券和商户积分结算已由用户批准重构为三份领域权威文档：`docs/member-and-tier-product-prd-v1.0.md`、`docs/points-and-voucher-product-prd-v1.0.md`、`docs/merchant-points-settlement-rules-v1.0.md`。三个业务 HTML 中相关旧条款在逐条替代清单获批前保持原文；其他业务领域仍由三个 HTML 最终解释。
-
 ## 平台
 
 - 定义：最上层治理主体，可管辖多个城市和多个项目公司，制定统一规则、系统和治理口径。
@@ -20,19 +20,19 @@
 
 ## 项目公司
 
-- 定义：某个城市或单个综合体的经营决策与责任主体，对本地经营、定价、售票和分账负责。
-- 边界：所有基础对象必须带项目公司归属；项目公司不是平台，也不是业态主体。
+- 定义：某个城市项目的资产、场地、物业、租赁及直接管理的第三方商户的责任主体。
+- 边界：物理项目是空间、价格、经营数据和权限隔离维度；项目公司不是平台，也不是业态主体，不管理各域公司的人员、设备、定价、票务或结算。
 - 允许别名：单综合体经营主体、综合体项目归属。
 - 禁止歧义别名：平台、集团、业态项目。
 
 ## 管理公司／业态主体
 
-- 定义：项目公司下按业态设立的经营平台。现行专名（2026-07-28 用户拍板）为 **娱乐平台公司**、**中央餐饮公司**、**游乐公司**、**传媒公司**。
-- 边界：位于项目公司与商户之间；可承担供货职能的主体以平台级供货身份向商户供货。
-  - **娱乐平台公司**：统管娱乐业态运营；通票收入与门票综合服务费归此主体；赠送权益预估成本、兑付与差额均归此主体；**参股各酒吧商户**。
+- 定义：按业态分域负责的经营平台。现行专名（2026-07-28 用户拍板）为 **娱乐平台公司**、**中央餐饮公司**、**游乐公司**、**传媒公司**。
+- 边界：与物理项目多对多协作，不是固定隶属层级；可承担供货职能的主体以平台级供货身份向商户供货。
+  - **娱乐平台公司**：负责酒吧经营域、酒吧通票及跨域组合通票中的酒吧部分，承担其决定或出资权益的结算责任；不取得单一酒吧门票收入，只按生效规则向酒吧商户另收门票综合服务费；**参股各酒吧商户**。
   - **中央餐饮公司**：供应链主体，向各商户供货并收取货款，主要面向内部及参股商户，少量第三方商户。
-  - **资产业态不单设主体**：**资产公司即长沙项目公司**（同一主体两个叫法），场地租金与相关物业费用归长沙项目公司。
-  - **各酒吧不是业态主体**：酒吧为 **L4 独立商户**（娱乐平台公司参股），酒吧自身经营收入、非散台服务费（卡座等）、订台低消／二次消费／额外酒水均归各酒吧商户自己。
+  - **资产业态不单设主体**：各城市项目公司兼任资产角色，场地租金与相关物业费用归对应城市项目公司。
+  - **各酒吧不是业态主体**：酒吧为 **独立商户**（娱乐平台公司参股），订台低消／二次消费／额外酒水归各酒吧商户；非散台服务费由商户代收，月结定格的应发部分作代收代付、不计商户收入，扣留部分计商户收入。
 - 允许别名：业态主体。
 - 禁止歧义别名：商户、项目公司、泛称“组织”。
 - **废止别名**：「娱乐运营平台」（2026-07-28 起一律称娱乐平台公司）；「酒吧 SPV／酒吧经营主体」（一律理解为各酒吧商户）。
@@ -46,8 +46,8 @@
 
 ## 会员
 
-- 定义：主动注册、完成平台支持的身份核验并同意会员及隐私条款后形成的全国统一免费会员身份。
-- 边界：同一自然人全国唯一、跨城市和跨项目复用；不设置付费订阅身份。
+- 定义：主动注册、完成注册手机号短信验证并同意会员及隐私条款后形成的全国统一免费会员身份。
+- 边界：以短信验证后的手机号唯一识别、跨城市和跨项目复用；不设置付费订阅身份。
 - 禁止歧义别名：项目会员、付费会员身份。
 
 ## 成长值
@@ -70,19 +70,19 @@
 
 ## 零售参考价
 
-- 定义：同一项目、同一 SKU 在过去滚动 30 天的最低有效实际成交价。
+- 定义：同一项目、同一 SKU 在后台配置的滚动取价窗口内（按项目可配，出厂默认三十天，须配上下限）的最低有效实际成交价；窗口内无有效成交时，自动向前延长至找到最近一笔有效成交。
 - 边界：只使用项目内成交；其他项目和外部渠道不参与。
 - 禁止歧义别名：全国零售价、外部最低价。
 
 ## 内部结算价
 
-- 定义：项目与商户就兑换 SKU 约定、在实际核销后用于计算商户补偿的项目级价格。
+- 定义：每个项目对商品或服务独立管理、在实际核销或结算时使用的价格。
 - 边界：每个项目独立管理、审批和版本化；不得跨项目引用或合并为全国统一价格。
 - 禁止歧义别名：零售参考价、积分发行单价、全国内部价。
 
 ## 全国积分结算中心
 
-- 定义：与商户统一签约、收付款并执行积分结算的法律主体。
+- 定义：负责全国商户积分结算的功能主体；当其不是独立公司时，积分发行费及会员系统服务费由顶层平台公司收取。
 - 边界：汇总和审计项目价格但不拥有项目价格；不是商户商品或服务的转售方。
 - 禁止歧义别名：项目、商户、积分商城卖方。
 
@@ -136,16 +136,16 @@
 
 ## 终端开关
 
-- 定义：后台对十类终端及其项目、商户、业务系统、模块、版本／设备和灰度范围实施的启停控制。
-- 边界：关闭时前端入口不可用且后端拒绝业务动作；高层级关闭压倒低层级打开；拉取失败时写操作兜底关闭。打开后仍需校验身份、权限、商户范围和设备状态。
+- 定义：后台对十一类终端及其项目、商户、业务系统、模块、版本／设备和灰度范围实施的启停控制。
+- 边界：关闭时前端入口不可用且后端拒绝业务动作；高层级关闭压倒低层级打开；拉取失败时业务写操作兜底关闭，特殊留痕写操作继续记录。打开后仍需校验身份、权限、商户范围和设备状态。
 - 允许别名：终端能力开关。
 - 禁止歧义别名：前端菜单开关、权限替代品。
 
-## 十类终端
+## 十一类终端
 
-- 定义：客人 App、小程序、Web、工作人员平板、员工工作手机、员工私人手机、商户侧 Web 门户、商户侧 App、一体机、移动管理端。
+- 定义：客人手机应用、客人小程序、Web 前端（业务人员，客人无网页端）、工作人员平板、员工工作手机、员工私人手机、商户网页门户、商户手机应用、触摸屏一体机、移动管理端、独立经销商 Web 门户。
 - 边界：商户侧 Web 门户与商户侧 App 是两类独立终端；员工工作手机与私人手机也是两类独立终端。
-- 允许别名：受控十端。
+- 允许别名：受控十一端。
 - 禁止歧义别名：商户侧 App/Web、员工端（若合并计数）。
 
 ## 操作留痕
@@ -157,7 +157,7 @@
 
 ## 存档与删除
 
-- 定义：存档是把对象从新业务选择中下线但保留历史与恢复能力；删除是在先存档后，经另一人审批执行的软删除。
-- 边界：存档不需审批；删除须填写理由，申请人不得自审；删除只隐藏不物理抹除，存在业务占用时必须阻断。
+- 定义：以下术语适用于配置类主数据：存档是把对象从新业务选择中下线但保留历史与恢复能力；删除是在先存档后，经具备最高权限的人审批执行的软删除。
+- 边界：逐条存档不需审批；一次存档超过后台配置条数（按综合体可配，须配上下限），或整店、整类存档，须审批后执行。删除须填写理由，申请人不得自审；删除只隐藏不物理抹除，存在业务占用时必须阻断。
 - 允许别名：存档、软删除。
-- 禁止歧义别名：物理删除、存档审批、申请人自批。
+- 禁止歧义别名：物理删除、申请人自批。
diff --git a/PROGRESS.md b/PROGRESS.md
index 12ab936f..6bf56366 100644
--- a/PROGRESS.md
+++ b/PROGRESS.md
@@ -1,4 +1,6 @@
-# 退租结算落财务台账 — 建设进度台账（分支 feat/termination-settlement）
+# 退租结算落财务台账 — 历史建设进度台账（分支 feat/termination-settlement）
+
+版本：V0.01
 
 ## 目标
 合同退租变更（move_out/early_termination）终审通过 → 自动生成退租结算并落应收应付台账：
@@ -29,5 +31,6 @@
 operation-matrix→closure→security-baseline→latest.json→清 surefire→entry 基线；verify:predeploy。
 无迁移故跳 domain-ownership-scope/data-isolation-spec。
 
-## 状态
+## 当时状态
+以下为该分支当时的建设记录，测试数字不代表当前工作区的测试结果。
 完成。后端(单测32绿)+前端(vitest15绿+typecheck绿)+verify:predeploy全链绿。已 commit+push。
diff --git a/README.en.md b/README.en.md
index 081ec3dc..c4140398 100644
--- a/README.en.md
+++ b/README.en.md
@@ -1,6 +1,8 @@
 # Entertainment Complex Management System
 
-> 文档goalP2(2026-06-20): English mirror of [`README.md`](./README.md). The Chinese `README.md` is authoritative; this file tracks it for international developers and third-party suppliers. All commands and numbers are evidence-backed (`ls` / `grep` / OpenAPI counts / Surefire reports / Playwright `--list`).
+Version: V0.01
+
+> English entry for international developers and third-party suppliers. See [`README.md`](./README.md) for the maintained baseline and verification commands.
 
 ## 1. What it is
 
@@ -26,7 +28,7 @@ A multi-domain management platform for offline entertainment complexes, covering
 
 ```
 complex-management/
-├── backend/                # Java 21 + Spring Boot 4 backend (Maven, Flyway migrations V001..V124)
+├── backend/                # Java 21 + Spring Boot 4 backend (Maven, Flyway migrations)
 ├── apps/
 │   ├── web/                # React + Vite + antd admin/customer web
 │   └── android-terminal/   # Android terminals (Gradle)
@@ -75,14 +77,9 @@ npm run verify:ui:real-backend:seeded   # real-backend E2E (seeded)
 npm run verify:closed-loop-runtime      # closed-loop runtime acceptance (backend tests + frontend + UI)
 ```
 
-## 5. Current baseline (2026-06-20, evidence-backed)
+## 5. Current baseline
 
-| Metric | Value | Evidence |
-|---|---|---|
-| Latest Flyway migration | **V124** | `ls backend/src/main/resources/db/migration` |
-| OpenAPI operations | **386** | count HTTP methods in `docs/openapi/openapi-p0.yaml` |
-| Backend tests | **1251** passing | `backend/target/surefire-reports/*.txt` (2026-06-20) |
-| Frontend E2E | **42** (6 specs) | `npx playwright test --list` |
+See [`README.md`](./README.md) §5 for the generated baseline, its evidence sources and snapshot limits. Run `node tools/refresh-entry-doc-baseline.mjs --check` to check it and `npx playwright test --list` to list the current E2E tests.
 
 ## 6. Architecture
 
@@ -94,11 +91,11 @@ graph LR
   STAFF["Staff"] --> ANDROID["Android terminals"]
   WEB --> API["Backend API<br/>Java 21 + Spring Boot 4"]
   ANDROID --> API
-  API --> DB[("PostgreSQL<br/>Flyway V001..V124")]
+  API --> DB[("PostgreSQL<br/>Flyway migrations")]
   API -. "switch-gated + mock" .-> EXT["Third-party / Payment / WeCom / Robot / Video / MDM / NAS"]
 ```
 
-Each write action passes `@GuardedAction` (permission + terminal switch + AdmissionGuard) → Service (business + idempotency + traceability) → JDBC Repository (forced merchant_id/project_id scope) → PostgreSQL, emitting audit events; exceptions flow into the unified `risk_alerts` responsibility chain.
+A typical protected business write passes `@GuardedAction` (permission + terminal switch + AdmissionGuard) → Service (business + idempotency + traceability) → JDBC Repository (merchant/project scope) → PostgreSQL, emitting audit events; exceptions flow into the unified `risk_alerts` responsibility chain. Authentication endpoints such as login use their own handling logic.
 
 ## 7. Audience reading paths
 
diff --git a/README.md b/README.md
index f9f06d08..f647ca80 100644
--- a/README.md
+++ b/README.md
@@ -1,5 +1,7 @@
 # 综合体管理系统（Entertainment Complex Management）
 
+版本：V0.01
+
 > 文档goal(2026-06-20)：本 README 为仓库第一入口，供产品、开发、测试、运营、第三方供应商快速理解项目并上手。所有命令与数字均经实证（`ls`/`grep`/OpenAPI 统计/Surefire 报告/Playwright `--list`），不臆造。
 >
 > 🌐 English: [`README.en.md`](./README.en.md) ｜ 🏗️ 架构图集：[`docs/architecture-overview-v0.1.md`](./docs/architecture-overview-v0.1.md)
@@ -35,13 +37,11 @@ complex-management/
 ├── apps/
 │   ├── web/                # React + Vite + antd 管理端（含 ticketing/bar/amusement 模块）
 │   └── android-terminal/   # Android 终端（android-app/，Gradle）
-├── database/
-│   └── migrations/         # 历史/参考迁移脚本（正式迁移以 backend/.../db/migration 为权威）
 ├── docs/                   # 全部产品/开发/契约/验收/运营文档（第一入口见 docs/00-...）
 │   └── openapi/            # 机器可读 P0 契约：openapi-p0.yaml / openapi-p0.json
 ├── ops/
 │   └── 160/                # 160 主机运维脚本（部署/回滚/健康检查/备份/NAS 挂载/恢复演练）
-├── tools/                  # 79 个 Node 校验/生成脚本（verify:* / docs:* 闸口实现）
+├── tools/                  # Node 校验/生成脚本（verify:* / docs:* 闸口实现）
 ├── tests/
 │   └── e2e/                # Playwright 端到端用例（mock 与真实后端）
 ├── security-scans/         # Semgrep 等安全扫描产物
@@ -96,6 +96,7 @@ npm run verify:closed-loop-runtime      # 闭环运行态总验收（后端测
 ## 5. 当前基线（脚本自动回填，勿手改数字）
 
 本节数字由 `node tools/refresh-entry-doc-baseline.mjs` 从权威源（OpenAPI JSON、Flyway 迁移目录、surefire 报告）自动回填；`--check` 可校验漂移。
+表内“现有 surefire 报告”表示生成器保留的最近报告快照；无报告时，不代表当前工作区存在报告或本轮执行过测试。
 
 <!-- baseline:auto-begin（tools/refresh-entry-doc-baseline.mjs 生成，手改会被覆盖） -->
 | 指标 | 当前值 | 取证方式 |
@@ -139,7 +140,7 @@ graph LR
   API -. "开关化 + mock" .-> EXT["第三方/支付/企微/机器人/视频/MDM/NAS"]
 ```
 
-每个写动作经 `@GuardedAction`（权限点 + 终端开关 + 准入闸 AdmissionGuard）→ Service（业务+幂等+留痕）→ JDBC Repository（强制注入 merchant_id/project_id scope）→ PostgreSQL，并发布审计事件；异常进统一责任链 `risk_alerts`。
+受保护业务写动作的典型路径为 `@GuardedAction`（权限点 + 终端开关 + 准入闸 AdmissionGuard）→ Service（业务+幂等+留痕）→ JDBC Repository（商户/项目数据范围）→ PostgreSQL，并发布审计事件；异常进统一责任链 `risk_alerts`。登录等认证入口按自身处理逻辑执行。
 
 ## 9. 边界声明
 
```

---

## 4. 逐块 ID 归因（闸 3）

文件自身版本由无→V0.01：依据用户版本铁律与 `docs/prd-audit/business-decision-ledger.md` DEC-R8-96 第 3 条（三份业务 HTML 例外；其他被更新文件须执行 `Vx.xx`）。本批 6 文件 before 均无自身版本行，首次写入 V0.01，归该文件正在修的已定稿队列「是」ID。

| hunk | 文件 | 变更摘要 | 归因 ID | 依据 |
|---|---|---|---|---|
| H1 `@@ -1,3 +1,4 @@` | `.env.example` | 增加 `# 版本：V0.01` | M003-001 / M003-002 | 本文件正在修的 ID；版本铁律 |
| H2 `@@ -11,15 +12,8 @@` | `.env.example` | 删除 5 个无消费者变量 | M003-001 | 定稿处置：删除样例中的无效配置声明 |
| H2 同 hunk | `.env.example` | 删除 `COMPLEX_KEYVAULT_PII_KEY=""`，注释改为「不设置才用 dev 默认」 | M003-002 | 定稿处置：删除空值赋值 |
| H3 `@@ -1,5 +1,7 @@` | `AGENTS.md` | `版本：V0.01` | M003-003 / M003-004 | 本文件正在修的 ID；版本铁律 |
| H4 `@@ -47,8 +49,8 @@` | `AGENTS.md` | 删除一律/人脸唯一例外/`role_platform_admin`；收窄配置主数据删除+专项生命周期 | M003-003 | 冲突根因；BSM:567/570/862/588 |
| H5 `@@ -62,8 +64,7 @@` | `AGENTS.md` | 删除失实浅克隆理由，保留「未挂 verify:predeploy」 | M003-004 | 技术性缺陷；`.github/workflows/predeploy-gates.yml:17` `fetch-depth: 0` |
| H6 `@@ -1,5 +1,7 @@` | `CONTEXT.md` | `版本：V0.01` | M003-005—012 / 020 / 023 | 本文件正在修的 ID；版本铁律 |
| H7 `@@ -9,8 +11,6 @@` | `CONTEXT.md` | 删除三份领域 Markdown 置于 HTML 之上的段落 | M003-005 | 冲突；PGM:974 |
| H8 `@@ -20,19 +20,19 @@` 项目公司+管理公司定义/边界+资产业态 | `CONTEXT.md` | 删除固定上下级、越权定价售票分账、资产只称长沙 | M003-006 | 冲突；PGM:415/418/420 |
| H8 同 hunk 娱乐平台公司行 | `CONTEXT.md` | 删除统管/通票收入/赠权成本过宽断言，按酒吧域+门票综合服务费收窄 | M003-007 | 冲突；PGM:428 |
| H8 同 hunk 酒吧行：删除 `L4 ` | `CONTEXT.md` | `**L4 独立商户**` → `**独立商户**` | M003-023 | 添加项操作员答复「删除」；仅删 L4 标记 |
| H8 同 hunk 酒吧行其余 | `CONTEXT.md` | 删除「自身经营收入/非散台服务费归商户自己」，改为代收代付口径 | M003-007 | 冲突；PGM:429 |
| H9 `@@ -46,8 +46,8 @@` | `CONTEXT.md` | 自然人唯一→短信验证手机号 | M003-008 | 冲突；BEQR:65/66 |
| H10 `@@ -70,19 +70,19 @@` 零售参考价 | `CONTEXT.md` | 删除写死 30 天，改为可配窗口+回溯 | M003-009 | 冲突；BEQR:80 |
| H10 同 hunk 内部结算价 | `CONTEXT.md` | 删除仅兑换 SKU/仅商户补偿 | M003-020 | 冲突；BEQR:54 |
| H10 同 hunk 全国积分结算中心 | `CONTEXT.md` | 法律主体→功能主体+非独立时顶层平台收款 | M003-010 | 冲突；BEQR:75/85 |
| H11 `@@ -136,16 +136,16 @@` | `CONTEXT.md` | 十类→十一类名录；写操作兜底关闭保留留痕例外 | M003-011 | 冲突；PGM:738/739 |
| H12 `@@ -157,7 +157,7 @@` | `CONTEXT.md` | 收窄配置主数据；逐条/批量存档；删除审批人改最高权限 | M003-012 | 冲突；BSM:566/569/570。禁止别名「存档审批」见 §5 独立判断 |
| H13 `@@ -1,4 +1,6 @@` | `PROGRESS.md` | 标题加「历史」+`版本：V0.01` | M003-022 | 历史时态澄清；版本铁律 |
| H14 `@@ -29,5 +31,6 @@` | `PROGRESS.md` | 「状态」→「当时状态」+不代表当前测试结果 | M003-022 | 定稿处置 |
| H15 `@@ -1,6 +1,8 @@` | `README.en.md` | `Version: V0.01`；删除把旧数字当现行实证的 goal 行 | M003-014 | 技术性；版本铁律 |
| H16 `@@ -26,7 +28,7 @@` | `README.en.md` | 去掉目录树 `V001..V124` | M003-014 | 同上 |
| H17 `@@ -75,14 +77,9 @@` | `README.en.md` | 删除 V124/386/1251/42 现行基线表，转引中文自动基线与命令 | M003-014 | 同上 |
| H18 `@@ -94,11 +91,11 @@` mermaid `V001..V124` | `README.en.md` | 去掉图中旧迁移号 | M003-014 | 同上 |
| H18 同 hunk 写路径句 | `README.en.md` | 每个写动作→受保护业务写的典型路径+认证入口例外 | M003-015 | 技术性；`AuthController.java:24-33` 无 `@GuardedAction` |
| H19 `@@ -1,5 +1,7 @@` | `README.md` | `版本：V0.01` | M003-013 / 015 / 021 | 本文件正在修的 ID；版本铁律 |
| H20 `@@ -35,13 +37,11 @@` | `README.md` | 删除不存在的 `database/migrations`；去掉「79 个」 | M003-013 | `Path.exists()=False`；tools 顶层 mjs=78 |
| H21 `@@ -96,6 +96,7 @@` | `README.md` | 自动块前澄清 surefire 快照语义 | M003-021 | 表外说明；自动块字节未变 |
| H22 `@@ -139,7 +140,7 @@` | `README.md` | 与 H18 中文对称的典型路径收窄 | M003-015 | 同 015 |

**闸 3 结论**：22 个 hunk、每一个 `+`/`-` 内容行均可归因到 findings V0.36 已定稿且队列为「是」的 ID。无无 ID 行，无对队列「否」ID 的改动，无模块外路径。

---

## 5. 两处必须独立判断的交接点

### 5.1 CONTEXT 酒吧同一行：007 + 023（不按交接直接接受）

before：`酒吧为 **L4 独立商户**（娱乐平台公司参股），酒吧自身经营收入、非散台服务费（卡座等）、订台低消／二次消费／额外酒水均归各酒吧商户自己。`

after（CONTEXT.md:35）：`酒吧为 **独立商户**（娱乐平台公司参股），订台低消／二次消费／额外酒水归各酒吧商户；非散台服务费由商户代收，月结定格的应发部分作代收代付、不计商户收入，扣留部分计商户收入。`

对照：

- M003-023 定稿处置：**仅删**「L4 」。after 为 `**独立商户**`，全文件 `rg L4` 在 CONTEXT 无命中。HTML PGM:429 只有「独立酒吧商户」，无 L4。**023 根因已修。**
- M003-007 定稿处置：删除过宽收入/成本断言，保留 HTML 限定经营域及代收代付。同句删除「自身经营收入」「非散台服务费（卡座等）……归各酒吧商户自己」，代以 PGM:429 原文口径（应发代收代付不计收入、扣留计收入）；订台低消／二次消费／额外酒水保留。H8 娱乐平台公司行同步改为 PGM:428 酒吧域/通票酒吧部分/不取得单一酒吧门票收入/另收门票综合服务费。**007 根因已修。**
- 两 ID 同句共存合法：023 只动层级标记，007 动归属口径，没有把 L4 删除写成服务费修复，也没有用 023 覆盖 007。

### 5.2 禁止别名「存档审批」移除 — 独立判断归 012

before：`禁止歧义别名：物理删除、存档审批、申请人自批。`
after（CONTEXT.md:163）：`禁止歧义别名：物理删除、申请人自批。`

M003-012 根因是「存档不需审批」的**绝对化**（CONTEXT 旧边界）与 BSM:569 冲突：逐条存档不用审批；超过配置条数或整店、整类存档**须经审批后执行**。旧禁止别名把「存档审批」一律当作歧义，正是该绝对化在词表上的投射。after 边界已写明批量/整店/整类须审批，再保留「存档审批」为禁止别名会与 HTML 和新边界再冲突。

这不是新审批层（012 明确「不新增审批层」），也不是 003 越权改 CONTEXT：012 位置就是 CONTEXT.md:160-161 整段「存档与删除」。删除审批人由「另一人」改为「具备最高权限的人」与同段 BSM:570 原文一致，仍属该冲突段收窄，不是新 ID。

**独立结论：归因 M003-012，非越界，非新判据。**

---

## 6. 顺从式：19 项队列「是」是否修到根因（按冻结 GEN）

| ID | 队列 | 2.1 | 冻结 GEN（findings） | 根因是否落到 after | 证据类型 |
|---|---|---|---|---|---|
| 001 | 是 | 技术性 | GEN-002/013/016 | 5 变量从 `.env.example` 删除；6 文件内零残留 | 4 |
| 002 | 是 | 技术性 | GEN-002/009/013/016 | 空赋值删除；注释与 `application.yml:109` 占位符「未设置才用默认」一致；空串仍会走 `SigningSecretGuard:15-16` 抛错 | 4 |
| 003 | 是 | 冲突 | GEN-016/017/005 | AGENTS.md:52-53 收窄配置主数据+最高权限+账户注销转 HTML；无 `role_platform_admin`/「删除一律」/人脸唯一例外 | 1 |
| 004 | 是 | 技术性 | GEN-013/016 | 浅克隆理由删除；`predeploy-gates.yml:17` 仍为 `fetch-depth: 0`；未改 CI 门禁 | 4 |
| 005 | 是 | 冲突 | GEN-016/017 | CONTEXT 领域 PRD 权威段落删除；解释权仍三份 HTML | 1 |
| 006 | 是 | 冲突 | GEN-016/017 | 项目/管理公司去固定层级；资产改各城市项目公司兼任；不管理各域公司人员设备定价票务结算 | 1 |
| 007 | 是 | 冲突 | GEN-016/017 | 见 §5.1 | 1 |
| 008 | 是 | 冲突 | GEN-016/017 | CONTEXT.md:49-50 短信验证手机号；无「同一自然人」 | 1 |
| 009 | 是 | 冲突 | GEN-016/017 | CONTEXT.md:73 可配窗口默认三十天+回溯 | 1 |
| 010 | 是 | 冲突 | GEN-016/017 | CONTEXT.md:85 功能主体；非独立时顶层平台收取发行费与会员系统服务费 | 1 |
| 011 | 是 | 冲突 | GEN-009/011/016/017 | 十一类名录 11 项与 PGM:739 逐条相同；兜底关闭限业务写、留痕继续 | 1 |
| 012 | 是 | 冲突 | GEN-005/016/017 | 见 §5.2 | 1 |
| 013 | 是 | 技术性 | GEN-016 | `database/migrations` 不存在；目录项删除；「79 个」删除 | 4 |
| 014 | 是 | 技术性 | GEN-008/016 | EN 现行 V124/386/1251/42 表删除；转引 README.md §5；`--check` 720/V385/363 | 4 |
| 015 | 是 | 技术性 | GEN-002/005/016 | 中英均改为典型受保护业务写+登录按自身逻辑；`AuthController` 登录无 `@GuardedAction` | 4 |
| 020 | 是 | 冲突 | GEN-008/016/017 | CONTEXT.md:79 商品或服务、核销或结算；不再限兑换 SKU | 1 |
| 021 | 是 | 技术性 | GEN-011/016 | 自动块前增加快照说明；`<!-- baseline:auto-begin` 至 `auto-end` 前后 471 字节全等 | 4 |
| 022 | 是 | 技术性 | GEN-001/011/016 | 标题「历史建设进度台账」+「当时状态」；绿数原文保留 | 4 |
| 023 | 是 | 添加 | GEN-017 | 见 §5.1；questions 首个非空行=`删除` | 1 |

队列「否」5 项本批未改：

| ID | 队列 | 核验 |
|---|---|---|
| 016 | 否 | `.gitignore` blob 不变 |
| 017 | 否 | `.semgrepignore` blob 不变 |
| 018 | 否 | 未重写已登记无缺陷的命令/链接段；本 diff 只触及该文件中队列「是」的句子 |
| 019 | 否 | 未改登记范围内的其余术语 |
| 024 | 否 | CONTEXT「营销活动成本承担主体」91-93 与 before 逐字相同；questions 首个非空行=`保留` |

---

## 7. 第一性原理（2.3 三问，对本次 diff）

1. **这段改动为什么必须存在？** 删掉它，19 条已定稿冲突/缺陷会原样留在 M003 入口文件里：样例变量与 Spring 行为不符、词表与 BSM/PGM/BEQR 明文相反、英文现行基线与 `--check` 的 720/V385 矛盾。三份 HTML 自身未改（铁律 4）。
2. **声称的行为有没有被实际执行验证？** 文档/配置按类型 1/4：HTML 行号原文已读；`node tools/refresh-entry-doc-baseline.mjs --check` PASS（720 ops / V385（363 迁移））；`node tools/verify-entry-anchors.mjs` PASS；`bash -n .env.example` exit 0；`git diff --check` exit 0；`Path('database/migrations').exists()=False`；tools 顶层 mjs=78；`package.json` 仍有 `verify:ui:real-backend:seeded` / `verify:closed-loop-runtime`。未跑全后端/E2E，不伪称。
3. **哪些前提没验证就当真？** 未重跑 B1 的 jshell/javac Spring 空串实验；本轮以 `application.yml:109` 占位符原文 + `SigningSecretGuard.java:14-17` 空值抛错为 002 的类型 4 证据。未启动 Spring Boot。

---

## 8. 对抗式（grilling 自问自答）

❓ **Q1 012 删「存档审批」是不是发明新审批？**
➡️ 否。BSM:569 已有批量/整店/整类须审批。删禁止别名是去掉与 HTML 相反的绝对化，不是新流程。

❓ **Q2 007 有没有把游乐/演出票写进娱乐平台公司？**
➡️ after 限定酒吧经营域、酒吧通票及跨域组合通票中的酒吧部分；未写纯游乐/演出归该主体。与 PGM:428/430 不冲突。未把「纯游乐与其无关」整句抄入，属词表压缩，不构成相反断言。

❓ **Q3 023 有没有多删？**
➡️ 只去掉 `L4 `。独立商户、参股、订台/二次/酒水保留。服务费改写属 007。

❓ **Q4 001 删 JWT 等是否拆掉真实接线？**
➡️ 6 个改动文件内零命中。定稿证据是后端消费 `complex.auth.*` 而非这些 env 名。本 diff 未新增接线，符合「不为保留旧变量新增生产接线」。

❓ **Q5 002 新注释是否仍错？**
➡️ yaml：`pii: ${COMPLEX_KEYVAULT_PII_KEY:complex-dev-pii-encryption-key-change-me}`。未设置走默认；显式空串覆盖默认后 `requireUsableHmacSecret` 对 empty throw。after 不再给空串。生产「>=32 且不含 change-me」与 `isWeakProductionSecret` 一致。

❓ **Q6 014/021 有没有手改自动基线块？**
➡️ 自动块前后 471 字节相等。`--check` PASS。021 写在 `<!-- baseline:auto-begin` 之前。

❓ **Q7 015 弱化「强制注入 merchant_id」是否越界？**
➡️ 015 位置就是该整句绝对架构断言。同一句收窄为典型路径，JDBC 仍写商户/项目数据范围，未宣称取消隔离，未改控制器。

❓ **Q8 024/三份 HTML/.gitignore 是否被顺手改？**
➡️ 营销词条 before=after；三 HTML blob 全等；gitignore/semgrepignore blob 全等。

❓ **Q9 版本行无 ID？**
➡️ 否。版本铁律+DEC-R8-96 第 3 条；归各文件正在修的队列「是」ID。

❓ **Q10 越界/业务发明？**
➡️ 未发现无 ID 行。新词条均可指到 HTML 或 GEN。无新判据。

**ponytail-review：** `Lean already. Ship.` 净删 4 行。可再删的只有 AGENTS.md:67 相对邻行多 1 个前导空格，属 H5 替换残留空白，不是新抽象，不单列越界。

---

## 9. 全部新问题（临时 GR）

本席审完整 22 hunk / 6 文件后，**没有**须进修复队列的新缺陷。

观察项（不构成 FAIL、不分配新 ID、不启用锁外判据）：

| 临时号 | 位置 | 冻结 ID | 2.1 / 依据类型 | 已定稿 ID? / 队列 | 说明 |
|---|---|---|---|---|---|
| GR-OBS-01 | AGENTS.md:67 | M003-004 | 技术性 / 4 | 是 / 是（本批已修句） | 该行 4 空格，邻行 3 空格。内容仍是授权保留的「未挂 verify:predeploy。」 |
| GR-OBS-02 | CONTEXT.md:23 | M003-006 | 细化 / 1 | 是 / 是 | 定义未抄 PGM:420「自身人员和设备」。未写成「不管理自身人员」。006 最小处置是删除越权职责，不是全文复制 HTML。 |
| GR-OBS-03 | CONTEXT.md:147 | M003-011 | 细化 / 1 | 是 / 是 | 边界仍用「商户侧 Web 门户/App」旧称；定义名录已用 HTML 十一类名。禁止歧义别名仍用「商户侧 App/Web」。不与十一类名录冲突。 |

无越界 → 不整批 FAIL。无添加项新发明 → 不 BLOCKED-ASK。

---

## 10. 实际检查命令输出

```text
sha256sum docs/audit-2026-09/plan/criteria-lock.md
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

cat docs/audit-2026-09/plan/criteria-lock.sha256
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

grep '^CRITERIA-LOCK-SHA256:' docs/audit-2026-09/ledger.md
# ledger 顶部输出 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

git cat-file -t a339075ff27c89319739c4ca4e9fb802b31f376c
tree
git cat-file -t 18e0fe08e0b08eb55f1390c93ddd0f92d6625cc7
tree

git diff --binary <before> <after> | sha256sum
c5d7661649fbabd3b003394242e2a4069d36251e994a9b5bc5b43255a4c92a55
git diff --binary <before> <after> | wc -c
19006

git diff --check <before> <after> ; echo exit:$?
exit:0

bash -n .env.example ; echo exit:$?
exit:0

node tools/refresh-entry-doc-baseline.mjs --check
[entry-doc-baseline] PASS: 快照与实际一致（720 ops / V385（363 迁移））。
exit:0

node tools/verify-entry-anchors.mjs
[entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1563 条；被检 Markdown 353 份；条目名引用 1517 处（注册表原名 1358 处，正文唯一定位 159 处）。
[entry-anchors] PASS：条目名唯一、注册表条目正文逐字与 HTML 现文一致、引用可唯一定位、正文无行号锚点、账本正文与九个镜像列均与 HTML 现文一致、账本与承接矩阵一一对应。
exit:0

python3 Path('database/migrations').exists()
False
tools 顶层 .mjs 个数
78
ls backend/target/surefire-reports
ls: cannot access '...': No such file or directory
```

HTML 直系原文（本席读取，摘定位）：

- BSM:566/569/570/588/862/1725
- PGM:415/418/420/428/429/430/738/739/974
- BEQR:32/53/54/65/66/75/80/85

`questions/M003-023.md` 操作员答复首个非空行：`删除`
`questions/M003-024.md` 操作员答复首个非空行：`保留`

未跑：全后端测试、Maven 包、Playwright 执行（仅确认 README.en 引用的 npm script 键存在）、`verify:html-ledger-sync`（三份 HTML blob 未变，不扩全仓）。

---

## 11. B4 判定

单席 PASS 条件核对：

- 两树真实且未过滤 diff 已审完（6 文件 / 22 hunk / 19006 字节哈希一致）
- 闸 3：每一变更行归因到已定稿队列「是」ID
- 19 项队列「是」根因在 after 上闭合；5 项「否」未改
- 012「存档审批」、007/023 同行均独立判断，非照抄交接
- 三线已跑；证据类型 1/4；无锁外新判据
- 无越界、无业务发明、无未答 questions
- 未伪称全后端/E2E

**B4：PASS**

本席停止。不修复、不改 findings/ledger、不进入 B5。
