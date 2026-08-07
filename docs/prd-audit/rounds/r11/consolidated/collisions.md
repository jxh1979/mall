# R11 Consolidator · 跨 shard 碰撞处置

基线 `1c5c0c7d`。本文件只做归并裁断，不改任何被审计文件。

已知碰撞 C-1 ～ C-6 逐条处置；自扫另发现 C-7 ～ C-10，一并列出。

---

## C-1 · `spec:59`「四类」被 shard-05(FINAL-20) 与 shard-06(A-11) 同时认领

**处置：采用 shard-05 EV-05-29~33（FINAL-20 整行拟改）。弃用 shard-06 EV-06-03 第 9 项作为独立改动。**

弃用理由（三条，均经实读两侧拟改文本核对）：

1. **shard-06 第 9 项的内容已被 shard-05 的改后文本完全吸收。** shard-05 对
   `spec:59`「规则要点」列的改后首句逐字是「**五类**高危权限授出前须先审批才生效——…、
   税费规则与税费调整；…」——数词与第五类内容都在里面。若两份都落，shard-06 的
   单字替换会被 shard-05 的整行替换覆盖或反之，产生「后改的覆盖先改的」。
2. **shard-06 自己给出了同向建议。** shard-06「跨 shard 修正」节原文：
   「建议归并方向：取 shard-05 的实质范围（它覆盖更全），本 shard 的数词改法作为其中一步；
   但**不要两份都落**。」本处置与该建议一致。
3. **shard-06 第 9 项明确自限于数词**，并写明「补税费三审链＋顶层终审＋禁自审」
   「审计敏感查看留痕补需先经审批」**不在本 shard 拟改内**——而这两项恰是
   FINAL-19 登记的实质要求，只落 shard-06 会留下 FINAL-19 未闭环。

**不弃用的部分（重要）**：shard-06 EV-06-02/03/04/05 的**其余 17 处**（`spec:59` 之外）
全部保留且不可替代——shard-05 只审 spec 一份文件，从未做全库检索。
以 shard-06 的「活文件 11 个 18 处」清单为下游同步的唯一依据（FINAL-19 登记的 5 处偏低，
漏 `CONTEXT.md`×2、`html-prd-coverage.csv:212`×2、`business-decision-ledger.md:21`、
`AccessService.java:81` 共 6 个活文件）。

---

## C-2 · `spec:60` 同一列被 FINAL-21 与 FINAL-25 同时改（shard-05 内部）

**处置：核对通过，确认采用 FINAL-21 给出的改后文本，一次性合并落笔。**

核对过程（逐字比对两处拟改）：

- FINAL-25 ③ 要求删「403」与「terminal.access_rejected」两个技术口径，移入「验证入口」列，
  断言列改为业务表述「菜单不可见或不可点、后端接口拒绝该业务动作，且被拒访问本身留痕；
  前端隐藏只是辅助，绕过前端直调接口同样被拒」。
- FINAL-21 的改后断言文本实读为：「关端/关模块 → 后端接口拒绝该业务动作并写入被拒留痕；
  **配置表中不存在该能力的开关行时该能力处于关闭态**…开关拉取失败时写操作全拒但留痕照写；
  营业时段内对营业必需能力的变更、升级、维护被拒…高层级关闭时低层级打开无效；
  **前端隐藏入口不作为唯一防线，绕过前端直调接口同样被拒**。」

**结论：FINAL-21 文本已内含 FINAL-25 的全部去技术化结论**（无「403」、无
`terminal.access_rejected`，且保留了「绕过前端直调接口同样被拒」这一句），
并额外补齐了 FINAL-21 自身的三项 GAP。shard-05 的合并方案成立，予以确认。

**落笔约束**：`spec:60` 只改一次；FINAL-25 ③ 的「验证入口」列处置（把 403/事件码移入该列）
仍需单独执行——该列不与 FINAL-21 冲突（FINAL-21 只动「规则要点」「可验收断言」「HTML 来源」三列）。

---

## C-3 · `finance §4B.4` 被 shard-04(REGX-11) 插入；shard-03(REGX-12) 落在 points 系

**处置：无冲突，核对通过。句序确认为「出结果 → 清账 → 账期沿用」。**

核对依据（两侧落点实读）：

| shard | finding | 落点 | 是否碰 finance §4B.4 |
|---|---|---|---|
| shard-04 | REGX-11 | `finance-settlement-product-prd-v0.1.md` §4B.4，现 L329 之后、L330 之前 | 是（唯一改动者） |
| shard-03 | REGX-12 | `points-and-voucher-product-prd-v1.0.md` §11A.1 + `merchant-points-settlement-rules-v1.0.md` §16.1 | **否** |

**句序核对**：shard-04 EV-04-01 摘出 BSM L1501 同一 `<p>` 内相邻三句，实读句序为
①「凡属电子数据、且计算比例固定的，一律在月结定格时即出结果…」（HTML 第 2 句，= §4B.4 现有第 1 条 bullet）
→ ②「大部分账目应在当月内完全清理清楚，只有平台无法控制的（第三方相关）才顺延至下个月。」（HTML 第 3 句，= 待插入行）
→ ③「通票分账、单店门票结付…一律沿用与积分结算相同的账期」（HTML 第 8 句，= §4B.4 现有第 2 条 bullet）。

即插入后 §4B.4 三条 bullet 的顺序为 **出结果 → 清账 → 账期沿用**，与任务书要求逐字一致。**核对通过。**

**旁注（不影响结论）**：`BSM-0741`（REGX-11）与 `BSM-0742`（REGX-12）是 BSM L1501
同一段内相邻的两句，被账本拆成两条 `rule_id`；两条同源不同落点，是有意分流，不是遗漏。

---

## C-4 · `value_flow` 副本率：shard-04 报 176/441(39.9%) vs shard-08 复核 406 有效 → 43.3%

**处置：以 shard-08 为准。shard-04 的两个数都没算错，但分母口径失真，须整体修正。**

修正后的权威数值（shard-08 实跑，`csv` 模块解析非目测）：

| 项 | shard-04 报 | shard-08 复核 | 采用 |
|---|---|---|---|
| 账本数据行 | 1225 | 1225 | 1225 |
| `value_flow != NOT_STATED` | 441 | 441 | 441（**但含 35 行哨兵，不是「有内容」**） |
| 真有文字 | 未区分 | **406** | **406** |
| 逐字副本（原始串） | 176 | 176 | 176 |
| 逐字副本（门禁 `norm()` 归一后） | 未跑 | **204** | **204** |
| 副本占比 | 39.9%（分母 441） | **43.3%**（分母 406） | **43.3%** |

三点修正：

1. **441 里有 35 行是哨兵 `NOT_EXTRACTED_R9`，不是内容。** 账本里的哨兵至少四种
   （`NOT_STATED`／`NOT_EXTRACTED_R9`／`NOT_EXTRACTED_R8`／`NOT_EXTRACTED_R8-H2`），
   任何脚本必须用 `== "NOT_STATED"` 精确匹配，**不得用 `startswith("NOT_")`**。
2. **按门禁同源 `norm()` 归一后副本是 204 格不是 176 格**——另有 28 格只差空白或引号写法。
   假信号面比 shard-04 报的还大 16%。
3. **这不是 `value_flow` 独有的毛病，是全表 9 个句子类镜像列的通病，合计 771 格**
   （value_flow 176 / state_transition 132 / trigger 127 / evidence_required 79 /
   preconditions 78 / failure_recovery 72 / approval 63 / permission 44 / separation_of_duties 0）。
   且「9 列全是副本」的行数 = **0**，说明不是整行机械填充，是**逐格**在找不到贴切切片时抄整句。

**shard-04 的实例判定予以保留并确认**：`BSM-0741` 的 `value_flow` 是 `source_text` 逐字副本，
该句只规定清账时限、不改金额/收付方向/分账比例，「假涉钱信号」判定**成立**（shard-08 独立复核属实）。

**连带待办（转 D 类，见 d-class-open.md D-01）**：`rounds/final/code-round-input-money.md`／
`code-round-input-nonmoney.md` 两份代码轮输入报告是否用过「`value_flow` 非空 ⇒ 涉钱」做筛选。
若用过，其钱路清单会因 204 行假信号而虚高。**两个 shard 都未核，本 Consolidator 亦未核。**

---

## C-5 · 企微：shard-02(R8-B-02 三子缺口) × shard-07b(P0-2 拆两半) × shard-05(FINAL-18)

**处置：三份合并成一条完整叙事。三者不重叠、互为不同层面，合并后才成立一条闭环叙述。**

### 合并叙事（口径层 → 验收层 → 实现层）

**① 口径层（shard-02，A-4）——「合法性论证未了」这半句已经闭合，但台账没回写。**

- 台账 `:1036`（2026-08-04）写「企业微信会话原文永久保存的合法性论证（PR #328 遗留，**仍未完成**）」；
- 台账 `:1274` DEC-R6-23（**同为 2026-08-04**）写「补齐企微会话永久保存的合法性论证」；
- 台账 `:1349` DEC-R7-26（2026-08-05）写「遗留义务**已了结**」；
- 外部四处实证一致指向已了结：合规文件 `:81`／`:101-105`／`:338`，
  以及 `data-risk-ai-development-design.md:685`。
- **危险点**：`:1036` 与 `:1274` 同日，**仅凭日期无法判定先后**，下游按「取日期较新者」的
  常规启发式判读会得到不确定结果。
- 处置：`:1036` 原行同行加括注（精确限定到第 1 条，不波及同节仍未了的第 2、3 条）＋ 文件末尾追加归口块。

**② 验收层（shard-05，FINAL-18）——spec 的「留存策略可配」为「到期自动删除」背书。**

- `spec:57`「验证入口」列写「留存策略**可配**」；
- H3 检索：「留存策略」BSM 0 / PGM 1（=PGM:880）/ ENUM 0；「留存策略**可配**」三份**均 0 命中**；
- PGM:880 说的是「系统自动做……留存策略」（自动执行既定策略），而该既定策略由
  BSM:899／PGM:570／BSM:1474 三处锁死为「**永久保存、不自动到期删除**」；
- HTML 注释还明确记载「2026-06-08 三轮收口曾把留存期改为可配置，该处置**已被推翻**」——
  即「可配」正是被推翻的旧处置。
- **危险点**：按现行断言，一个实现只要提供「留存天数」配置项就算符合。

**③ 实现层（shard-07b，P0-2）——代码里那个「留存天数配置项」真的存在，而且默认开着。**

这一层把 ①② 从文档矛盾变成了真实风险，是三份里唯一有代码证据的一份：

- **种子（ACTIVE）**：`V087__wecom_compliance.sql:70-73` 是 **Flyway 迁移里的 INSERT**——
  任何跑完迁移的环境必然存在 `retention_days=180, expiry_action='anonymize', enabled=true`；
  全仓无任何后续迁移改写或删除该种子行。
- **代码兜底同向**：`WecomConversationService.java:56` `DEFAULT_RETENTION_DAYS = 180`；
  `:725-728` 查不到策略时同样返回 180/anonymize/true——**手删种子行也没用**。
- **到期戳已落库**：`:218` 每次同步落库时就写 `retentionUntil = businessDate.plusDays(retentionDays)`，
  到期时点不是运行时才算。
- **「永久」在这套配置里根本无法表达**：`:720` 校验 `retentionDays` 必须在 **1..3650** 之间，
  最长 10 年，连人工绕开都做不到。
- **HTML 要求的清理通道不存在**：「最高权限本人 + 授权 U 盘密钥手动执行」在本模块无实现，
  唯一清理路径是定时批处理。

**④ 唯一须降级的一半（shard-07b 的自我修正，必须保留）**：

转述所称「会真删/真匿名」**当前不可达**，应标 `IMPLEMENTATION_INERT`：
`applyRetention` 全仓唯一调用点是 `WecomRetentionJob.java:44-51`，受
`@ConditionalOnProperty(compliance.wecom-retention.scheduled.enabled, havingValue="true")`
+ 布尔早返回**双重关闭**，且 `application-prod.yml:26` 明写「按用户选择保持默认关」，
**无任何 Controller/REST 端点**调用它。

**⑤ 合并后的风险陈述（这是三份合起来才说得完整的一句）**：

> 口径三处一致要求永久保存；spec 的验收断言为「可配」背书；代码里那个「可配」已经以
> 180 天 anonymize、`enabled=true` 的形态写进迁移种子并给每条会话盖了到期戳；
> 匿名化不可逆（`content_raw_ref` 置 NULL、脱敏文替换为 `[ANONYMIZED]`）；
> 唯一没发生的是执行——而执行与不执行之间**只隔一个配置项**。
> 而企微会话是私单、私收款、违规承诺的取证面（BSM:1450），删了不可逆。

**⑥ 三份的欠账登记也须合并**（`findings.csv:79` R8-B-02 三子缺口）：

- ①「旧留存口径未删」= **已闭合（文档层）**（GAP-25 + DEC-R8-16）；
- ③「合法性论证未了」= **已了结**（六处实证，即上文 ①）；
- ②「无机械门禁」= **仍未闭合**——shard-02 实跑 `git ls-tree tools/` 全量 + 关键词筛，
  命中全属生产证据回填／集成开关／安全基线类脚本，**无任何企微留存口径校验脚本**
  （对比 HTML 侧已有 `verify-html-param-tier.mjs` 等三个）。
- 故 `status` 保持 `in-progress` 正确，须改的是 `disposition`（把三子缺口拆开写明剩什么）。

### 落笔分工（避免三处各改一半）

| 层 | 文件 | 由哪份 shard 的拟改执行 |
|---|---|---|
| 口径 | `business-decision-ledger.md:1036` + 末尾追加块 | shard-02 EV-02-15 |
| 口径 | `business-decision-log-v0.1.md:551`（**P0，任务书未点名**） | shard-02 EV-02-04 |
| 登记 | `rounds/r8/findings.csv:79` R8-B-02 disposition | shard-02 EV-02-17 |
| 验收 | `spec:57` 四列 | shard-05 EV-05-23~28（FINAL-18） |
| 实现 | 代码轮（种子/1..3650 校验/缺 U 盘密钥通道） | shard-07b P0-2，**不在本轮文档轮范围** |

**注**：shard-02 EV-02-18 已把 FINAL-18 登记为「不在本 shard A-4 范围、未处置」并转编排——
本处置即为该转交的落点（见 C-9）。

---

## C-6 · 人工放行：shard-07b(三条链) × shard-07c(废词批 B)

**处置：合并。两者是同一问题的两个层面，且 07c 的「废词」框架不足以覆盖 07b 的实质冲突。**

### 层面一：词（shard-07c）——用户可见串未下线，3 处实测 3/3 仍在

| # | 落点 | 性质 |
|---|---|---|
| 1 | `apps/web/src/admin/pages/SupervisorReviewPage.tsx:255` | 主管复核页动作类型下拉，用户可见 |
| 2 | `apps/web/src/customer/i18n/statusLabels.ts:166` | `manual_release: "人工放行"`，用户可见 |
| 3 | `db/migration/V270__ticket_manual_override_permission.sql:10` | 权限种子 `permission_name`，会在权限管理页展示 |

### 层面二：功能本体（shard-07b）——不是废词，是口径与实现相反，且实为**三条**链非两条

| 链 | 证据强度 | 关键实测 |
|---|---|---|
| 链 1 票务核销 | **四重齐**：HTTP route + 迁移建权限 + 迁移授给 `role_platform_admin` + DB CHECK 纳入合法状态全集 | `TicketingController.java:483-490` `POST /ticketing/verifications/manual-override`；`V270:8-17`；`V067:4-6`；执行体 `TicketVerificationService.java:211-229` **不跑任何核销规则校验**直写 `manual_overridden` |
| 链 2 治理督导 | route + UI 入口 + `status="active"` 策略 | `SupervisorReviewService.java:36` `manual_release_l2`；`SupervisorReviewController.java:27/33/40` 三条 route |
| 链 3 游乐（**转述未列，shard-07b 实测发现**） | route + 迁移建表 + `@GuardedAction` wiring | `AmusementController.java:39/45`；`V014:123/133` `CHECK (override_type IN ('allow',...))` |

### 合并后的完整判定

1. **口径**：ENUM:41 逐字「**系统不提供人工放行**：核销要么成功、要么拒绝或失败，
   **任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过**」（全仓唯一一处原文）。
2. **shard-07c 的「废词」框架不够用**——它自己也点出了这一点：
   「文档说『不提供』、代码真实提供，属**口径与实现相反**，比废词严重一档」。
3. **07c 漏计的功能本体坐标**（javadoc/注释，但描述的路径真实可调）：
   `TicketVerificationService.java:208/218/243`、`TicketingController.java:483`、
   `TicketingService.java:918`、`dto/VerificationManualOverrideRequest.java:7`、`V067:6`。
4. **shard-07b 补充的第三态问题**：`manual_overridden` 是 ENUM 六态
   （成功/拒绝/失败/已冲正/待复核/异常）之外的**第七态**，
   所有按六态写的下游统计/对账/BI 分桶会漏计或落 default 分支。
5. **shard-07c 的登记状态订正**（须保留）：台账 `:1564` 把「人工放行」记进
   「P1 深层残留清扫」，同节 `:1559` 写「三份 HTML 零字节改动」、`:1566` 写「P2 PRD 同步」，
   **通读像已完成，实际代码零改动**；各设计文档自己写的是「待代码轮下线」。
   建议**订正既有条目状态为「仅文档侧完成」**，而非新建 finding。

**合并结论**：作为**一条** finding 处置，严重度取 shard-07b 的 **P0/Blocking**（不是 07c 的 P1），
处置面 = 3 处词 + 3 条链 + 6 处功能本体坐标 + 第七态下游影响 + 台账登记状态订正。
**风险陈述取 shard-07b**：票务链无规则校验直写通过记录，免票入场、超期票入场、
年龄闸绕过均可由一个高权限账号完成。

---

# 自扫新发现的碰撞

## C-7 · `V270` 迁移文件名两 shard 不一致（**已实测裁定**）

| shard | 引用的文件名 |
|---|---|
| shard-07b（P0-3 链 1） | `V270__verification_manual_override_permission.sql` |
| shard-07c（EV-07c-03） | `V270__ticket_manual_override_permission.sql` |

**实测裁定**：
```
$ git ls-tree --name-only 1c5c0c7d backend/src/main/resources/db/migration/ | grep V270
backend/src/main/resources/db/migration/V270__ticket_manual_override_permission.sql
```
**shard-07c 正确，shard-07b 的文件名有误**（`verification_` 应为 `ticket_`）。
行号 `:8-17` / `:10` 与内容两侧一致，仅文件名一处笔误。
**下游引用一律以 `V270__ticket_manual_override_permission.sql` 为准。**

> 该订正与 shard-07b 自报的另两处引证订正同类
> （P0-5 的 `V068:46` 张冠李戴 → 应为 `V010:64-75`；P0-3 漏第三条链）。
> 合计 shard-07b 引证订正 **3 处**，其中 1 处由本 Consolidator 发现。

---

## C-8 · `spec:59` 上叠着两个 finding（FINAL-19 与 FINAL-20），shard-05 只按 FINAL-20 命名

实读 `findings.csv:227/228`：

- `FINAL-19`（STALE，important）= `§2B RULE-GOV-AUTH-01 **规则要点**`：四类→五类 + 断言补税费三审链、顶层终审、禁自审 + 审计敏感查看补「需先经审批」；
- `FINAL-20`（ORPHAN，advisory）= `§2B RULE-GOV-AUTH-01「**且不可配置关闭**」`。

**同一行、不同列/不同缺陷，是两条独立 finding。**

**核对结论**：shard-05 的 FINAL-20 整行拟改**同时闭合了 FINAL-19 的 spec 侧要求**——
其改后文本已含「五类」「税费规则与税费调整」「财务/税务/法务三审后由顶层平台公司老板最终确认」
「提交人不能审批自己的单据」「敏感查看与导出须先经审批并全程留痕」。

**处置**：`spec:59` 落笔时**同批销 FINAL-19 与 FINAL-20 两条**，
不得只销 FINAL-20 而留 FINAL-19 open。
`FINAL-19` 剩余的**下游同步**部分（其登记的 5 处 + shard-06 补出的 6 处活文件）
由 shard-06 EV-06-02/03/04/05 执行，与 spec 侧分开销案。

---

## C-9 · shard-02 EV-02-18 与 shard-05 FINAL-18 指向同一 spec 行

shard-02 在 A-4 收工时把 `findings.csv:226` FINAL-18 登记为
「同属企微域但主题不同…**不在本 shard A-4 范围**，未处置、未改。此处仅登记，供编排判断是否并批。」

而 shard-05 对同一条给出了完整的四列拟改（EV-05-23~28）。

**处置：并批。** shard-02 的转交请求由 C-5 的合并叙事接住；
FINAL-18 的实际改法取 shard-05，**shard-02 侧不再另出方案**（它本来也没出）。
两者无文本冲突（shard-02 未提任何 spec 拟改文字）。

---

## C-10 · shard-02 与 shard-07c 都碰 `business-decision-ledger.md`，另 shard-06 也碰 —— 三方写入不重叠但须串行

任务书点名要注意 shard-02/07c 都碰该台账，实读三方落点：

| shard | 落点 | 动作 |
|---|---|---|
| shard-02 | `:47`／`:56`／`:62`／`:1036` 同行加括注 + **文件末尾**追加 DEC-R8-85 块与企微归口块 | 纯追加，旧行一字不改 |
| shard-06 | `:21`（四类→五类，EV-06-03 第 12 项）／`:350` 小节标题（EV-06-12 附） | **就地改字** |
| shard-07c | **只读引用** `:1564`／`:1559`／`:1566` 作为「登记状态误导」的证据 | **不改**（只建议订正既有条目状态） |

**结论：三方落点行号互不相交，无内容碰撞。** 但：

1. 该台账是「**只追加**」型，shard-06 的两处**就地改字**与该体例存在张力——
   `:21` 是索引表状态格、`:350` 是小节标题，均非裁决正文，改动可接受；
   但须与 shard-02 的追加块**同一 PR 内串行落笔**，不得并发（单文件 CRLF/BOM，
   参照记忆铁律「共享库迁移代理不能并发」同理）。
2. shard-06 已自行剔除两处**不得改**的行并写明理由：`:61`（引述旧文档原话，历史留痕）
   与 `:1753`（DEC-R8-84 裁决论证中的引用改前值）。**本 Consolidator 核对该剔除正确**，
   予以确认——若误改这两处会毁掉裁决自身的论证链。

---

# 碰撞处置汇总表

| # | 碰撞 | 处置 | 弃用/修正对象 |
|---|---|---|---|
| C-1 | `spec:59` 双认领 | 采用 shard-05 FINAL-20 整行拟改 | 弃用 shard-06 EV-06-03#9 独立数词改（其余 17 处保留） |
| C-2 | `spec:60` FINAL-21 × FINAL-25 | 核对通过，采用 FINAL-21 文本一次落笔 | 无（FINAL-25 的「验证入口」列处置仍单独执行） |
| C-3 | `finance §4B.4` | 无冲突；句序「出结果→清账→账期沿用」核对成立 | 无 |
| C-4 | `value_flow` 副本率 | 以 shard-08 为准：406 有效／204 归一副本／**43.3%**／全表 9 列 771 格 | 修正 shard-04 的 39.9%（分母含 35 行哨兵） |
| C-5 | 企微三份 | 合并为口径→验收→实现三层一条叙事；shard-07b「会真删」半边降 INERT | 无弃用，三份互补 |
| C-6 | 人工放行 | 合并为一条；严重度取 shard-07b P0；处置面 = 3 词 + 3 链 + 6 坐标 + 第七态 + 台账状态订正 | 07c 的「废词」框架不足，不作为独立条目 |
| C-7 | `V270` 文件名 | **实测裁定**：`V270__ticket_manual_override_permission.sql`（07c 对） | 订正 shard-07b 的 `verification_` 笔误 |
| C-8 | `spec:59` 叠两个 finding | 同批销 FINAL-19 + FINAL-20；FINAL-19 下游部分由 shard-06 分开销 | 无 |
| C-9 | FINAL-18 转交 | 并批，改法取 shard-05 | 无 |
| C-10 | 决策台账三方写入 | 行号互不相交；**须串行落笔**；确认 shard-06 对 `:61`/`:1753` 的剔除正确 | 无 |
