CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M003 B4-r1 修复内容完整自审（Codex）
版本：V0.01
时间：2026-09-05T10:39:20.026670+00:00。
步骤B4；M003/322，IN_PROGRESS，B3累计1。B2双PASS输入findings V0.35，V0.36已记录定稿，当前19项队列是。未读取本次Grok B4 raw/JSON/正文。

## 输入及覆盖

before tree=a339075ff27c89319739c4ca4e9fb802b31f376c；after tree=18e0fe08e0b08eb55f1390c93ddd0f92d6625cc7。
范围为两tree完整git diff --binary，19006字节，SHA256=c5d7661649fbabd3b003394242e2a4069d36251e994a9b5bc5b43255a4c92a55。本方独立实算两树完整diff，以下全文不按路径过滤。共6文件、22个hunk，每个hunk及变更行对应已定稿ID。临时索引包含tracked/staged/untracked全部路径，真实索引未变；仅六文件有diff，.gitignore/.semgrepignore/三份HTML不在diff。
当前8源文件全部SHA与after tree已记录manifest相等，共621行；原625行的变化来自本批修复，不更改8文件范围或18GEN映射。findings V0.36 SHA256=4a09eebe3826a6275ebf17430d00c26bf342b8819abc5d96a4fa8bef5d5ee386；当前是集合与本批归因ID集合相等。023实际删除、024实际保留已在questions V0.02，024词条本批逐字保留。

## 全部diff逐块归因

| 文件 | hunk | 已定稿归属 |
|---|---|---|
| .env.example | @@ -1,3 +1,4 @@ | M003-001,M003-002 |
| .env.example | @@ -11,15 +12,8 @@ COMPLEX_DATABASE_MIN_IDLE="2" | M003-001,M003-002 |
| AGENTS.md | @@ -1,5 +1,7 @@ | M003-003,M003-004 |
| AGENTS.md | @@ -47,8 +49,8 @@ single-context 布局，词表在仓库根的 `CONTEXT.md`。详见 `docs/agents | M003-003 |
| AGENTS.md | @@ -62,8 +64,7 @@ single-context 布局，词表在仓库根的 `CONTEXT.md`。详见 `docs/agents | M003-004 |
| CONTEXT.md | @@ -1,5 +1,7 @@ | M003-005 |
| CONTEXT.md | @@ -9,8 +11,6 @@ | M003-005 |
| CONTEXT.md | @@ -20,19 +20,19 @@ | M003-006,M003-007,M003-023 |
| CONTEXT.md | @@ -46,8 +46,8 @@ | M003-008 |
| CONTEXT.md | @@ -70,19 +70,19 @@ | M003-009,M003-010,M003-020 |
| CONTEXT.md | @@ -136,16 +136,16 @@ | M003-011 |
| CONTEXT.md | @@ -157,7 +157,7 @@ | M003-012 |
| PROGRESS.md | @@ -1,4 +1,6 @@ | M003-022 |
| PROGRESS.md | @@ -29,5 +31,6 @@ | M003-022 |
| README.en.md | @@ -1,6 +1,8 @@ | M003-014,M003-015 |
| README.en.md | @@ -26,7 +28,7 @@ A multi-domain management platform for offline entertainment complexes, covering | M003-014 |
| README.en.md | @@ -75,14 +77,9 @@ npm run verify:ui:real-backend:seeded   # real-backend E2E (seeded) | M003-014 |
| README.en.md | @@ -94,11 +91,11 @@ graph LR | M003-014,M003-015 |
| README.md | @@ -1,5 +1,7 @@ | M003-013,M003-015,M003-021 |
| README.md | @@ -35,13 +37,11 @@ complex-management/ | M003-013 |
| README.md | @@ -96,6 +96,7 @@ npm run verify:closed-loop-runtime      # 闭环运行态总验收（后端测 | M003-021 |
| README.md | @@ -139,7 +140,7 @@ graph LR | M003-015 |


逐行核对：版本/必要空行归相应文件修复ID并执行用户版本铁律，六文件原无自身版本，建立V0.01。CONTEXT酒吧行的L4删除归023，服务费/收入限定修正归007，两条在同一行发生；023没有吞掉商户身份。存档定义、审批条件及“禁止别名：存档审批”的移除同归012，消除不需审批绝对化根因，保留逐条豁免、批量审批及最高权限删除审批。
没有不属于当前是集合的变更行，无新增源码文件/HTML/计划/判据或旁支修复。B4 raw/ledger更新在after tree之后，不伪装成源diff；本次审计产物按范围排除。

## 第一性原理与顺从式核对（全部19项）

| ID | 实际修复与依据 |
|---|---|
| 001 | 五个无消费者变量已从样例删除，后台消费者未改，不新增接线。GEN-002/013/016；原B1 rg与Spring实证。 |
| 002 | 空PII赋值删除，注释区分未设置默认/生产强密钥。本次当前Java编译实跑：旧空值拒绝，当前未设/local派生32字节AES，prod默认仍拒绝。GEN-002/009/013/016。 |
| 003 | 删除规则收窄配置主数据，最高权限审批、申请人不得自审，注销等按HTML专项生命周期；移除平台管理员等同最高权限/唯一人脸例外。BSM567/570/588/862；GEN-005/016/017。 |
| 004 | 删浅克隆错误理由，保留是否接入predeploy的事实，不改CI。workflow实际fetch-depth:0；GEN-013/016。 |
| 005 | 删除会员三PRD覆盖三HTML的反向权威段，保留最高解释权清单。PGM974；GEN-016/017。 |
| 006 | 项目公司职责限资产等，物理项目隔离与公司主体区分，业态公司与项目多对多，各城市项目公司兼资产。PGM415/418/420；GEN-016/017。 |
| 007 | 娱乐公司限定酒吧域/跨域酒吧部分，收入范围PGM428；非散台应发代收代付、扣留计收入PGM429，未统占游乐收入。GEN-016/017。 |
| 008 | 会员唯一键为短信验证手机号，保留跨项目复用/非付费订阅。BEQR65/66；GEN-016/017。 |
| 009 | 固定30日改可配滚动窗口，默认三十天、上下限、无有效成交前延。BEQR80；GEN-016/017。 |
| 010 | 中心改功能主体，非独立公司时两类费用由顶层平台收款，移除无条件法律主体断言。BEQR75/85；GEN-016/017。 |
| 011 | 十一类清单对齐PGM739、特殊留痕写例外PGM738，旧十类/十端无残留，旧heading链接git grep无命中。GEN-009/011/016/017。 |
| 012 | 配置主数据逐条存档免批、超配置条数/整店/整类审批、删除最高权限审批，去误导别名。BSM566-570；GEN-005/016/017。 |
| 013 | 删除不存在目录树项/硬编码79，保留backend实际迁移路径。GEN-016。 |
| 014 | 英文不再复制失效当前数字，转引中文自动块/既有命令，树和图旧迁移范围同步删除。GEN-008/016；新增链接存在。 |
| 015 | 中英断言收窄为受保护业务写典型路径，登录按自身逻辑；AuthController/SecurityConfig事实支持，未改认证机制。GEN-002/005/016。 |
| 020 | 内部价改为商品/服务项目核销/结算价，保留项目目录隔离、审批、版本。BEQR54；GEN-008/016/017。 |
| 021 | auto块前一句明确快照，“现有”不表示当前有报告/本轮测试。块逐字不变、--check实际PASS，不造862新测试。GEN-011/016。 |
| 022 | 标题/状态明确历史建设，数字为当时；保留7/32/15，不冒充当前运行。GEN-001/011/016。 |
| 023 | 实际许可删除L4三字符，007的同一行其他修复独立有归属；保留独立商户。GEN-017/questions规范删除。 |

18GEN适用性：001最小性、002真实配置、005审批、008名录/镜像、009拒绝/留痕、010生产密钥不削弱、011证据时态、013样例/入口、016文档、017来源、018实证已逐项覆盖。003测试有效性用真实Java最小运行验证而非假文档测试；004无新增可执行输入边界、006无数据写、007无并发代码、012无资源逻辑、014无迁移、015 Markdown结构/链接无破坏。不从N/A制造跨模块门槛。

## 对抗式核验

旧显式空PII会失败，语法通过不足以证明修复。本次编译当前KeyVault/LocalKeyVault/SigningSecretGuard，使用实际application.yml表达式、StandardEnvironment/MapPropertySource分别注入旧/新变量形态；helper仅在/tmp计算后清理，未source整个env、未连数据库。新默认32字节，生产默认仍拒绝，已验证修复前失败及修复后行为。
手机号唯一、可配窗口、跨域职责、批量存档审批、经销商终端及留痕例外的原反例均由对应句子消除，无新增业务状态/价格/主体。021位于生成块外，--check不会吃掉解释；英文转引已有入口，未复制新数字。017/024/三HTML及无待修ID保持。
本步只审本轮完整diff，整个模块修复后全量审查仍须B5；本票不把未修改的内容当成本次新增或修复效果。

## 实跑证据

B3完成后的真实检查输出：
```text
CHECK ["bash", "-n", ".env.example"] EXIT=0

CHECK ["node", "tools/refresh-entry-doc-baseline.mjs", "--check"] EXIT=0
[entry-doc-baseline] PASS: 快照与实际一致（720 ops / V385（363 迁移））。

CHECK ["npm", "run", "verify:entry-anchors"] EXIT=0

> entertainment-complex-management@0.1.0 verify:entry-anchors
> node tools/verify-entry-anchors.mjs

[entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1563 条；被检 Markdown 353 份；条目名引用 1517 处（注册表原名 1358 处，正文唯一定位 159 处）。
[entry-anchors] PASS：条目名唯一、注册表条目正文逐字与 HTML 现文一致、引用可唯一定位、正文无行号锚点、账本正文与九个镜像列均与 HTML 现文一致、账本与承接矩阵一一对应。

CHECK ["npm", "run", "verify:html-ledger-sync"] EXIT=0

> entertainment-complex-management@0.1.0 verify:html-ledger-sync
> node tools/verify-html-ledger-sync.mjs

[html-ledger-sync] PASS: 本次未改动业务最终解释权 HTML。比对范围：提交区 origin/main...HEAD（工作区三份 HTML 无未提交改动）。

CHECK ["git", "diff", "--check"] EXIT=0


```

本次B4独立配置/链接检查：
```text
JAVAC_CURRENT_SOURCES_EXIT 0

REAL_KEYVAULT_EXIT 0
OLD_EMPTY_REJECTED; CURRENT_UNSET_LOCAL_AES_BYTES=32; PROD_DEFAULT_REJECTED

ENV_TEMPLATE_PRESENCE_CHECK old_empty=True current_unset=True; no env file sourced; no DB
MARKDOWN_LINKS README.md missing=[]
MARKDOWN_LINKS README.en.md missing=[]
MARKDOWN_LINKS CONTEXT.md missing=[]
MARKDOWN_LINKS AGENTS.md missing=[]
MARKDOWN_LINKS PROGRESS.md missing=[]
No source writes; temporary helper removed automatically.

```


以上仅证明配置默认、文档/链接与本批改动，不声称执行后端全量测试/真实数据库/E2E。文档/配置按§6豁免另落TDD文件，真实Java行为检查给类型2/4证据。已实际读取requesting-code-review，按控制只派Grok、不用HEAD~1/第三席/即刻修；receiving-code-review、verification-before-completion与ponytail依其收窄口径使用。

## 冻结完整diff（两tree之间，无路径过滤）

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


## 本方B4结论

B4：PASS。全部6文件/全部hunk/19个已定稿是ID覆盖，未发现本次diff的新缺陷或越界。尚未读取Grok本次报告，需其真实PASS后才能出队进入B5；不是模块PASS。B4期间未修改任何源文件。
REPORT-END: M003 B4-r1 Codex PASS
