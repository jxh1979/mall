CRITERIA-LOCK-SHA256: 73ce1c4523de229a11552ca343f594e1506da3dc10e68d3dac216dd4ca232439

# M002 B4-r1 Grok 席原始报告

- 报告版本：V0.01
- 席位：Grok（审计席乙）
- 步骤：B4-r1 修复 diff 独立复审
- 模块：M002 — `00-authority-html-02`；控制提示词 V0.16；`ledger` 本模块 `IN_PROGRESS`，B3=1
- 本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`（独立实算，未抄 sidecar / ledger / 派发期望值）
- 实算值：`73ce1c4523de229a11552ca343f594e1506da3dc10e68d3dac216dd4ca232439`
- sidecar 同行：`73ce1c4523de229a11552ca343f594e1506da3dc10e68d3dac216dd4ca232439`
- ledger 顶部同行：`CRITERIA-LOCK-SHA256: 73ce1c4523de229a11552ca343f594e1506da3dc10e68d3dac216dd4ca232439`；`LOCK-AMEND-COUNT: 0`
- 互盲：未读取 `M002-b4-r1-self-raw.md`、未读取 Codex 本次 B4 结论；B1/B2 定稿（`M002-b1-grok-raw.md`、findings V0.25、ledger V0.58/V0.59）可读且已读
- 写入约束：只写本文件；不改 HTML / 代码 / CSV / lock / sidecar / findings / ledger；不派子代理；不提交推送；不进 B5；不重开 M001
- 冻结树：before=`bf400f2ebb21a7e112bfddfc8514b9d0cd4560ca`；after=`edc03163560e2373adb503a97cd22d76928d5ce0`
- 未过滤 binary diff SHA256=`409889c17bfbc24392efbc7ee3cfc38e9bedb963bcb566d89c45c243f44247ac`（15055 字节 / 74 行）
- 本席 B4 结论：**PASS**

---

## 1. 本环境技能预检（实际核对，非声称）

开工前按 V0.16 §6 在本机路径实列目录 / 读 `SKILL.md`。按收窄口径：B4 只审 diff、禁止修复、禁止子代理、禁止用 `HEAD~1` 替代两树。

### 1.1 已核到且本节点按收窄口径使用

| 技能 | 实核路径 | 本节点用法 |
|---|---|---|
| `using-superpowers` | `.../superpowers/6.3.0/skills/using-superpowers/SKILL.md` | 开工技能自检 |
| `grilling` | `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 对抗式 + 第一性原理追问结构；**不等操作员、不派子代理** |
| `codebase-design` | `.../mattpocock-skills/1.2.3/skills/engineering/codebase-design/SKILL.md` | 删除测试：本 diff 只删互斥日期，不删治理 HTML 本体 |
| `diagnosing-bugs` | `.../mattpocock-skills/1.2.3/skills/engineering/diagnosing-bugs/SKILL.md` | 以两树 binary diff + 两项既有门禁 + 逐字替换等式作红信号 |
| `systematic-debugging` | `.../superpowers/6.3.0/skills/systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流；B4 **不改代码**；门禁 FAIL 追到引用正则与历史 raw，不修 |
| `verification-before-completion` | `.../superpowers/6.3.0/skills/verification-before-completion/SKILL.md` | 任何 PASS/FAIL 声明前用本报告内命令输出 |
| `writing-for-agents` | `.../mattpocock-skills/1.2.3/skills/productivity/writing-for-agents/SKILL.md` | 本 raw 按下游 B2/B5 可差集、可归因来写；未跑其 `agents/` 子流程 |
| `ponytail-review` | `.../ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 找可再删的越界改动；B4 只列不修 |
| `ponytail` | `.../ponytail/4.9.0/skills/ponytail/SKILL.md` | 只用于评判本批修复是否偏胖，不修 |
| `requesting-code-review` | `.../superpowers/6.3.0/skills/requesting-code-review/SKILL.md` | 提示词收窄：B4 请审=本席自己审两树 diff；**不派子代理、不用 `HEAD~1`** |
| `i-have-adhd` | `.../i-have-adhd/0.1.0/skills/i-have-adhd/SKILL.md` | 输出形态；本报告完整覆盖优先于短列表上限 |
| knowledge-work `code-review` | `.../knowledge-work-plugins/engineering/1.2.0/skills/code-review/SKILL.md` | 只借正确性维度（精确替换、无越权）；不用连接器、不派第三席 |

### 1.2 已核到、本节点不适用（不冒充已调用）

| 技能 | 原因 |
|---|---|
| `receiving-code-review` | 本席是出具 B4 意见的一方，不是收对方意见后去 IMPLEMENT |
| `brainstorming` / `writing-plans` | 阶段 A 已完成 |
| `test-driven-development` / `tdd` | 本批是 HTML/CSV/台账文字；runbook 文档类不强制落测试 |
| `resolving-merge-conflicts` | 无合并冲突 |
| `git-guardrails-claude-code` | 可选；本体要问人装 hook，本席不装 |
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

## 2. 范围、两树真实性（不盲信 ledger V0.59）

对象：`git diff --binary bf400f2ebb21a7e112bfddfc8514b9d0cd4560ca edc03163560e2373adb503a97cd22d76928d5ce0`，无路径过滤，不用 `HEAD` 差替代。

### 2.1 对象类型与计数

```text
git cat-file -t bf400f2ebb21a7e112bfddfc8514b9d0cd4560ca
# tree
git cat-file -t edc03163560e2373adb503a97cd22d76928d5ce0
# tree
git ls-tree -r --name-only bf400f2ebb21a7e112bfddfc8514b9d0cd4560ca | wc -l
# 3491
git ls-tree -r --name-only edc03163560e2373adb503a97cd22d76928d5ce0 | wc -l
# 3491
git ls-tree -r --name-only HEAD | wc -l
# 3454
git ls-files | wc -l
# 3454
git rev-parse HEAD origin/main HEAD^{tree}
# 001878a11eeb6ea1fe79f5baac6f916cce68b555
# 001878a11eeb6ea1fe79f5baac6f916cce68b555
# 60fd10702ab4c1e9316f793a3087d5a341e87cfe
```

`HEAD^{tree}` ≠ before。before/after 文件名集合相同；相对 HEAD 多 37 条（35 个 `.scratch/r14/issues/*` + `M002-b1-grok-raw.md` + `M002-b1-self-raw.md`），HEAD 没有、before 也不缺任何 HEAD 路径。两树各含 `docs/audit-2026-09/` 21 个路径。这与「空索引 `git add -A` 拍下当时工作区（含当时未跟踪）+ 4 文件仍为 HEAD 前像」相符，不是纯 `HEAD^{tree}`。

### 2.2 独立复算（临时索引，真实 index 未动）

```text
# after 树把 4 文件换回 HEAD blob → 必须等于 before
GIT_INDEX_FILE=<mktemp>/index
git read-tree edc03163560e2373adb503a97cd22d76928d5ce0
git update-index --cacheinfo 100644,$(git rev-parse HEAD:<4 files>),<4 files>
git write-tree
# bf400f2ebb21a7e112bfddfc8514b9d0cd4560ca
# equals_before? YES

# before 树把 4 文件换成当前工作区 blob → 必须等于 after
git read-tree bf400f2ebb21a7e112bfddfc8514b9d0cd4560ca
git update-index --cacheinfo 100644,$(git hash-object <4 files>),<4 files>
git write-tree
# edc03163560e2373adb503a97cd22d76928d5ce0
# equals_after? YES

sha256sum .git/index
# 040cc6a55775bb41014c00b6ebc2728983138f2bea8289e55263ff377684e9ad
```

真实索引 SHA256 与 ledger V0.59 所记一致。反证：`git read-tree HEAD && write-tree` = `60fd1070…` ≠ before；当前空索引 `git add -A && write-tree` = `1a2e6345…` ≠ after（冻结后 ledger 已写成 V0.60，且工作区另有未跟踪 `M002-b4-r1-self-raw.md`；本席未读其正文）。**现在**的 add -A 不等于冻结 after，不否定冻结当时的 before/after 对。

### 2.3 四文件 blob 三角

| 路径 | HEAD = before | after = 工作区 |
|---|---|---|
| `docs/platform-governance-map-for-operations-v0.1.html` | `435150085c3640d06b85102f0ea36f00529181ee` | `8be84143b8aa74c9388a0e7865f35bad505218e0` |
| `docs/prd-audit/authority-rule-ledger.csv` | `2d695f39e45c2f84ea11ec7e8948f3539223d2e2` | `38d470b6c520bf92ddfd0c9810aae23e37aa8738` |
| `docs/prd-audit/business-decision-ledger.md` | `1070f0b130ceba094a7b3069aa50bc7deabc8584` | `c1b085f55a252c2e0ccf3dd5659aac3d2c25b690` |
| `docs/prd-audit/entry-registry.csv` | `f90e88a3338cfafa727e8d45f5e210a2a8f80ff8` | `634dd2c5bc66261c1ece901371fe20c7b50e20df` |

`git diff-tree -r before after` 只这 4 个 `M`。暂存区空。工作区相对 HEAD 的已跟踪脏文件另有 `docs/audit-2026-09/findings.md` 与 `ledger.md`：findings 的 before=after=工作区（`0b376aba…`，B3 未改台账）；ledger 的 before=after（`2dd0ddf0…`）≠ 当前工作区（`2511df39…`，V0.60 写在取 after 之后，符合「先冻结 after 再写 B3 次数」）。两树 diff 不含 audit 产物。

### 2.4 覆盖口径

两树覆盖：当时 `git add -A` 能收入的全部路径（HEAD 已跟踪 3454 + 当时未跟踪 37 = 3491）。before 与 after 之间**没有**未跟踪独有变更。当前工作区比冻结 after 多出的未跟踪不进入本轮 diff 对象。本席按 `git diff --binary before after` 审完整 4 文件，不按路径子集。

---

## 3. 未过滤 binary diff 全文

命令：

```bash
git diff --binary bf400f2ebb21a7e112bfddfc8514b9d0cd4560ca edc03163560e2373adb503a97cd22d76928d5ce0
```

`sha256sum` = `409889c17bfbc24392efbc7ee3cfc38e9bedb963bcb566d89c45c243f44247ac`（与 ledger V0.59 所记一致，本席独立复算）。无 `GIT binary patch` / `Binary files differ`。全文如下：

```diff
diff --git a/docs/platform-governance-map-for-operations-v0.1.html b/docs/platform-governance-map-for-operations-v0.1.html
index 43515008..8be84143 100644
--- a/docs/platform-governance-map-for-operations-v0.1.html
+++ b/docs/platform-governance-map-for-operations-v0.1.html
@@ -1,5 +1,5 @@
 <!doctype html>
-<!-- 文件版本：V0.07；更新日期：2026-09-02 -->
+<!-- 文件版本：V0.08 -->
 <html lang="zh-CN">
 <head>
   <meta charset="utf-8">
@@ -971,7 +971,7 @@
           </div>
         </section>
 
-        <p class="footer-note">更新日期：2026-09-01。用途：给不懂产品和技术的业务人员理解平台治理层范围。<br><strong>解释权声明：</strong>本《平台治理地图》与《业务系统地图》《业务枚举速查表》共同组成三份最高业务解释权 HTML。发生冲突时，只以这三份 HTML 中经用户批准的内容为准；PRD、其他文档、后台配置、设计和实现均不得覆盖。某项业务内容若尚未写入三份 HTML，不因出现在 PRD、配置或实现中而取得效力，必须先逐项获得用户批准并补入相应 HTML。三份 HTML 之间若出现冲突，按确认时间线判定：凡经用户确认过的内容，确认时间越晚的优先级越高，后确认的覆盖先确认的（以决策台账记录的裁决时间为准）；无法确定确认时间先后的，仍须逐项重新裁决。</p>
+        <p class="footer-note">用途：给不懂产品和技术的业务人员理解平台治理层范围。<br><strong>解释权声明：</strong>本《平台治理地图》与《业务系统地图》《业务枚举速查表》共同组成三份最高业务解释权 HTML。发生冲突时，只以这三份 HTML 中经用户批准的内容为准；PRD、其他文档、后台配置、设计和实现均不得覆盖。某项业务内容若尚未写入三份 HTML，不因出现在 PRD、配置或实现中而取得效力，必须先逐项获得用户批准并补入相应 HTML。三份 HTML 之间若出现冲突，按确认时间线判定：凡经用户确认过的内容，确认时间越晚的优先级越高，后确认的覆盖先确认的（以决策台账记录的裁决时间为准）；无法确定确认时间先后的，仍须逐项重新裁决。</p>
       </div>
     </main>
   </div>
diff --git a/docs/prd-audit/authority-rule-ledger.csv b/docs/prd-audit/authority-rule-ledger.csv
index 2d695f39..38d470b6 100644
--- a/docs/prd-audit/authority-rule-ledger.csv
+++ b/docs/prd-audit/authority-rule-ledger.csv
@@ -720,7 +720,7 @@ PGM-0188,platform-governance-map-for-operations-v0.1.html,"物品与履约全链
 PGM-0189,platform-governance-map-for-operations-v0.1.html,已确认,L965,已确认：集团视图预留项目归属和跨项目汇总口径，不阻塞单综合体运营。,governance,NOT_STATED,NOT_STATED,NOT_STATED,NOT_STATED,NOT_STATED,NOT_STATED,NOT_STATED,NOT_STATED,NOT_STATED,NOT_STATED,NOT_STATED,AUTHORITATIVE
 PGM-0190,platform-governance-map-for-operations-v0.1.html,权限点首批由平台统一预置，实施阶段允许边配置边补充,L966,已拍板：权限点首批由平台统一预置，实施阶段允许边配置边补充。,governance,平台,权限,NOT_STATED,NOT_STATED,NOT_STATED,NOT_STATED,已拍板：权限点首批由平台统一预置，实施阶段允许边配置边补充。,NOT_STATED,NOT_STATED,NOT_STATED,NOT_STATED,AUTHORITATIVE
 PGM-0191,platform-governance-map-for-operations-v0.1.html,企业微信后台配置由管理员一次性完成授权和安全凭据配置,L967,已拍板：企业微信后台配置由管理员一次性完成授权和安全凭据配置，系统自动做账号对应、同步、失败告警、基础规则质检和留存策略。,governance,管理员|系统,NOT_STATED,已拍板：企业微信后台配置由管理员一次性完成授权和安全凭据配置，系统自动做账号对应、同步、失败告警、基础规则质检和留存策略。,NOT_STATED,NOT_STATED,NOT_STATED,NOT_STATED,NOT_STATED,NOT_STATED,已拍板：企业微信后台配置由管理员一次性完成授权和安全凭据配置，系统自动做账号对应、同步、失败告警、基础规则质检和留存策略。,NOT_STATED,AUTHORITATIVE
-PGM-0192,platform-governance-map-for-operations-v0.1.html,文档版本,L974,更新日期：2026-09-01。用途：给不懂产品和技术的业务人员理解平台治理层范围。解释权声明：本《平台治理地图》与《业务系统地图》《业务枚举速查表》共同组成三份最高业务解释权HTML。发生冲突时，只以这三份HTML中经用户批准的内容为准；PRD、其他文档、后台配置、设计和实现均不得覆盖。某项业务内容若尚未写入三份HTML，不因出现在PRD、配置或实现中而取得效力，必须先逐项获得用户批准并补入相应HTML。三份HTML之间若出现冲突，按确认时间线判定：凡经用户确认过的内容，确认时间越晚的优先级越高，后确认的覆盖先确认的（以决策台账记录的裁决时间为准）；无法确定确认时间先后的，仍须逐项重新裁决。,governance,系统|平台|用户,NOT_STATED,NOT_STATED,NOT_STATED,NOT_STATED,NOT_EXTRACTED_R9,NOT_STATED,NOT_STATED,NOT_STATED,NOT_STATED,NOT_EXTRACTED_R9,AUTHORITATIVE
+PGM-0192,platform-governance-map-for-operations-v0.1.html,文档版本,L974,用途：给不懂产品和技术的业务人员理解平台治理层范围。解释权声明：本《平台治理地图》与《业务系统地图》《业务枚举速查表》共同组成三份最高业务解释权HTML。发生冲突时，只以这三份HTML中经用户批准的内容为准；PRD、其他文档、后台配置、设计和实现均不得覆盖。某项业务内容若尚未写入三份HTML，不因出现在PRD、配置或实现中而取得效力，必须先逐项获得用户批准并补入相应HTML。三份HTML之间若出现冲突，按确认时间线判定：凡经用户确认过的内容，确认时间越晚的优先级越高，后确认的覆盖先确认的（以决策台账记录的裁决时间为准）；无法确定确认时间先后的，仍须逐项重新裁决。,governance,系统|平台|用户,NOT_STATED,NOT_STATED,NOT_STATED,NOT_STATED,NOT_EXTRACTED_R9,NOT_STATED,NOT_STATED,NOT_STATED,NOT_STATED,NOT_EXTRACTED_R9,AUTHORITATIVE
 "BEQ-0001","business-enum-quick-reference-v0.1.html","票种","L28","票种平日票、周末票、节假日票（基础价）；成人票、学生票；多人票（双人/团队）；新人票；会员价、渠道价、动态价；一人多次票、通票。派对票与活动票等价。单日票、夜场票。赠票作为票务产品类型。","ticketing","NOT_STATED","通票","NOT_STATED","NOT_STATED","NOT_STATED","票种平日票、周末票、节假日票（基础价）；成人票、学生票；多人票（双人 / 团队）；新人票；会员价、渠道价、动态价；一人多次票、通票","NOT_STATED","NOT_STATED","NOT_STATED","NOT_STATED","NOT_STATED","AUTHORITATIVE"
 "BEQ-0002","business-enum-quick-reference-v0.1.html","ORPHANED","旧记 L29","购票渠道主路径：客人通过官方 App/小程序或第三方平台自助购买；客人可在自己手机上的综合体官方小程序或 App 自助购买该酒吧单店门票；例外：授权服务人员在启用售票能力的一体机上辅助到店客人购票。综合体通票只能由项目公司售卖（官方 App/小程序或授权第三方渠道），商户门店与酒吧门口/前台一体机都不得售卖通票；单店门票可由授权售票一体机代售本酒吧门票。","ticketing","服务人员|客人|商户|平台","门票|通票","NOT_STATED","综合体通票只能由项目公司售卖（官方 App/小程序或授权第三方渠道），商户门店与酒吧门口/前台一体机都不得售卖通票；","NOT_STATED","NOT_STATED","综合体通票只能由项目公司售卖（官方 App/小程序或授权第三方渠道），商户门店与酒吧门口/前台一体机都不得售卖通票；","NOT_STATED","NOT_STATED","NOT_STATED","NOT_STATED","SUPERSEDED"
 "BEQ-0003","business-enum-quick-reference-v0.1.html","通票类型",L34,"通票类型酒吧通票、游乐通票、酒吧+游乐联合通票、含餐饮/零售优惠折扣的组合通票","ticketing","NOT_STATED","通票","NOT_STATED","NOT_STATED","NOT_STATED","NOT_STATED","NOT_STATED","NOT_STATED","NOT_STATED","NOT_STATED","NOT_STATED","AUTHORITATIVE"
diff --git a/docs/prd-audit/business-decision-ledger.md b/docs/prd-audit/business-decision-ledger.md
index 1070f0b1..c1b085f5 100644
--- a/docs/prd-audit/business-decision-ledger.md
+++ b/docs/prd-audit/business-decision-ledger.md
@@ -1,9 +1,9 @@
 # PRD 审计业务裁决台账
 
-文档版本：V0.17
+文档版本：V0.18
 状态：进行中
 迁移日期：2026-07-27
-当前事实源：三份立法 HTML 以既有冻结基线叠加 James 此前 M02 至 M08 的点名批准及本次全量审计 M001 的逐项批准；现行版本与字节哈希见本文件末节。
+当前事实源：三份立法 HTML 以既有冻结基线叠加 James 此前 M02 至 M08 的点名批准及本次全量审计 M001、M002 的逐项批准；现行版本与字节哈希见本文件末节。
 
 ## 当前使用规则
 
@@ -2532,3 +2532,11 @@ James 逐字批准的完整替换句：
 ### M001 发布门禁机械镜像同步
 
 同步 entry-registry.csv 的15行entry_body，以及 authority-rule-ledger.csv 的15行source_text及同条目句子镜像列；只承接上节已批准的现行HTML文字。条目ID、条目名、行锚、CSV列结构、总行数及html-prd-coverage.csv均保持不变；后者不重写既有PRD覆盖结论。verify:entry-anchors全量通过（注册表1228行、账本1235行、矩阵1244行、1563个镜像片段）。两份机器镜像沿用既有CSV格式，其本次版本记录归于本节V0.17：entry-registry.csv SHA256=717a1a9d9292c78d12670083e8e7481b1604ac810596613a7be098fe7651640a；authority-rule-ledger.csv SHA256=3757cc8a3a4d3cfbe32f7d80e478998bee8a5eaa6a988d67454674cbd79d9436。
+
+## V0.18 M002 申请20获批日期元数据同步
+
+来源：本次全量审计M002-001；操作员在申请20完整文字确认之后实际答复“可以”，批准治理地图第2行删除更新日期并将文件版本V0.07→V0.08、第974行仅删除“更新日期：2026-09-01。”。用途及完整解释权声明保留；未修改终端别名或任何业务规则。两处均逐项获批，按本批B2双PASS整批执行。
+
+本次只删除互相冲突的日期声明，避免猜测替代日期。入口注册表与规则账本各1行机械镜像同删该日期；ID、名称、行锚、列结构、行数与覆盖矩阵不变。CSV既有schema无文件版本字段，版本记录承接于本台账V0.18，不新增业务列。后续判据PGM-066引文按当前控制V0.15独立双席LOCK-AMEND同步，不能把源码修复当作判据同步已完成。
+
+治理地图当前V0.08，SHA256=9864216e609df2f319fa2cf7e51b61f06003ce45dff64a1a4098e71435865a68。枚举V0.07及业务地图V0.08字节保持M001交付版本。仅记录技术元数据获批改动，不创设业务裁决。
diff --git a/docs/prd-audit/entry-registry.csv b/docs/prd-audit/entry-registry.csv
index f90e88a3..634dd2c5 100644
--- a/docs/prd-audit/entry-registry.csv
+++ b/docs/prd-audit/entry-registry.csv
@@ -1046,7 +1046,7 @@ PGM-E0303,platform-governance-map-for-operations-v0.1.html,entry,"物品与履
 PGM-E0304,platform-governance-map-for-operations-v0.1.html,entry,已确认,968,已确认：集团视图预留项目归属和跨项目汇总口径，不阻塞单综合体运营。,已拍板规则和待排期问题,
 PGM-E0305,platform-governance-map-for-operations-v0.1.html,entry,权限点首批由平台统一预置，实施阶段允许边配置边补充,969,已拍板：权限点首批由平台统一预置，实施阶段允许边配置边补充。,已拍板规则和待排期问题,
 PGM-E0306,platform-governance-map-for-operations-v0.1.html,entry,企业微信后台配置由管理员一次性完成授权和安全凭据配置,970,已拍板：企业微信后台配置由管理员一次性完成授权和安全凭据配置，系统自动做账号对应、同步、失败告警、基础规则质检和留存策略。,已拍板规则和待排期问题,
-PGM-E0307,platform-governance-map-for-operations-v0.1.html,entry,文档版本,974,更新日期：2026-09-01。用途：给不懂产品和技术的业务人员理解平台治理层范围。解释权声明：本《平台治理地图》与《业务系统地图》《业务枚举速查表》共同组成三份最高业务解释权HTML。发生冲突时，只以这三份HTML中经用户批准的内容为准；PRD、其他文档、后台配置、设计和实现均不得覆盖。某项业务内容若尚未写入三份HTML，不因出现在PRD、配置或实现中而取得效力，必须先逐项获得用户批准并补入相应HTML。三份HTML之间若出现冲突，按确认时间线判定：凡经用户确认过的内容，确认时间越晚的优先级越高，后确认的覆盖先确认的（以决策台账记录的裁决时间为准）；无法确定确认时间先后的，仍须逐项重新裁决。,已拍板规则和待排期问题,
+PGM-E0307,platform-governance-map-for-operations-v0.1.html,entry,文档版本,974,用途：给不懂产品和技术的业务人员理解平台治理层范围。解释权声明：本《平台治理地图》与《业务系统地图》《业务枚举速查表》共同组成三份最高业务解释权HTML。发生冲突时，只以这三份HTML中经用户批准的内容为准；PRD、其他文档、后台配置、设计和实现均不得覆盖。某项业务内容若尚未写入三份HTML，不因出现在PRD、配置或实现中而取得效力，必须先逐项获得用户批准并补入相应HTML。三份HTML之间若出现冲突，按确认时间线判定：凡经用户确认过的内容，确认时间越晚的优先级越高，后确认的覆盖先确认的（以决策台账记录的裁决时间为准）；无法确定确认时间先后的，仍须逐项重新裁决。,已拍板规则和待排期问题,
 ENUM-E0001,business-enum-quick-reference-v0.1.html,entry,V0.1 ｜ 2026-08-01 ｜ 给业务同事快速查“系,23,2026-09-01｜给业务同事快速查“系统支持哪几种/有哪几个状态/按什么规则”。本表与《业务系统地图》《平台治理地图》共同组成三份最高业务解释权HTML；只有经批准写入这三份HTML的内容才是最终业务口径，PRD、其他文档和后台配置均不得覆盖。,,
 "ENUM-E0002","business-enum-quick-reference-v0.1.html","entry","用法",25,"用法：左列是“问题”（如“账单有哪几个状态”），右列是“答案”。配套两份业务地图（业务系统地图/平台治理地图）使用。","",""
 "ENUM-E0003","business-enum-quick-reference-v0.1.html","section","一、票务与权益",27,"一、票务与权益","一、票务与权益",""
```

---

## 4. 逐块归因（闸 3）

定稿来源：`docs/audit-2026-09/findings.md` V0.25。当前修复队列仅 `M002-001` = 是；`M002-002`/`M002-003` = 否。申请20操作员实际答复「可以」，只批治理 HTML 第2行删日期并 V0.07→V0.08、第974行仅删「更新日期：2026-09-01。」。V0.25 写明：两张既有镜像各 1 行及批准台账归同一 001。

上下文行（`PGM-0188…`/`BEQ-0001…`/`ENUM-E0001…`/`M001 发布门禁…`）不是变更。只对 `+`/`-` 行归因。

| 块 | 路径:行 | 变更 | 归因 | 闸 3 |
|---|---|---|---|---|
| A | `docs/platform-governance-map-for-operations-v0.1.html:2` | `<!-- 文件版本：V0.07；更新日期：2026-09-02 -->` → `<!-- 文件版本：V0.08 -->` | M002-001；申请20① | 合法 |
| B | 同上 `:974` | 仅删除前缀 `更新日期：2026-09-01。`；其后用途+解释权全文字节级保留 | M002-001；申请20② | 合法 |
| C | `docs/prd-audit/authority-rule-ledger.csv:723` `PGM-0192` | `source_text` 仅删同一前缀；ID/名称/`L974`/其余列不变 | M002-001 既有镜像 | 合法 |
| D | `docs/prd-audit/entry-registry.csv:1049` `PGM-E0307` | `entry_body` 仅删同一前缀；ID/名称/`974`/列结构不变 | M002-001 既有镜像 | 合法 |
| E | `docs/prd-audit/business-decision-ledger.md:3` | 文档版本 V0.17→V0.18 | M002-001 必要发布门禁台账 | 合法 |
| F | 同上 `:6` | 事实源补「、M002 的逐项批准」 | 同上 | 合法 |
| G | 同上 `:2535–2542` | 新增 `## V0.18 M002 申请20获批日期元数据同步` | 同上；`verify-html-ledger-sync.mjs` 改 HTML 必须同批改该台账 | 合法 |

无第五文件、无锁/sidecar/coverage 变更、无终端别名、无 M001 两份 HTML。`html-prd-coverage.csv` before blob == after blob。枚举 HTML / 业务地图 HTML 三树 blob 相同。

**闸 3：全部变更行都能指到队列为「是」的已定稿 ID M002-001。无越界。**

---

## 5. 正确性（申请20 / GEN-016 / 铁律 4）

依据类型：1 三份 HTML 逐字原文 + 4 可复现命令。冻结：GEN-016；铁律 4 申请20。

### 5.1 HTML 与批准逐字相等

对 before/after blob 做行对齐：979/979，仅 L2 与 L974 不同。

- L2 before 唯一命中申请20①旧句；after 唯一命中新句。
- L974：`after == before.replace('更新日期：2026-09-01。', '', 1)` 为真；after 不再含该前缀；「用途」与「解释权声明」仍在。
- 现行工作区 HTML SHA256=`9864216e609df2f319fa2cf7e51b61f06003ce45dff64a1a4098e71435865a68` = after blob = V0.18 台账所记。
- `V0.07` 已不在该 HTML；`更新日期` 计数 0；`客人 App` 仍 2 处（002 未动）。

铁律 4：两处均在申请20逐条范围内，未扩大到其它行、未猜替代日期、未改另外两份解释权 HTML。

GEN-016：修复前同一文件两个「更新日期」互斥（本席 B1 GR-01）。修复后只留可复现的 `文件版本：V0.08`，冲突消失。

### 5.2 两张镜像

`PGM-0192` / `PGM-E0307` 各仍 1 行。`row_after == row_before.replace('更新日期：2026-09-01。', '', 1)` 均为真。行锚仍 L974 / 974。覆盖矩阵未改。`wc -l`：注册表 1229、账本 1236、矩阵 1245（含表头；门禁计数 1228/1235/1244 数据行，与 M001 V0.17 基线同构）。

### 5.3 批准台账

只改版本行、事实源一行、文末追加 V0.18 节。V0.18 正文：来源 M002-001、答复「可以」、两处范围、不改终端别名、不猜日期、镜像各 1 行、不改 coverage、锁未同步。HTML 哈希本席复算相符。节内写「按当前控制V0.15」是 B3 落盘时的控制版本（ledger V0.59）；V0.16 是取 after 之后的控制迁移，不构成把未做的 LOCK-AMEND 写成已完成。节内明确「不能把源码修复当作判据同步已完成」。

### 5.4 锁未改（正确的负动作）

`criteria-lock.md` PGM-066 ````html` 块仍含 `更新日期：2026-09-01。用途：…`。本席实算锁哈希仍为 `73ce1c45…`，COUNT=0。按 V0.16 闸 2，获批 HTML 修复后的引文同步须完整 LOCK-AMEND，最早引用模块 M002；**该五步未发生**。本席不改锁，不把未来 LOCK-AMEND 或 B5 写成已完成，不重开 M001。

---

## 6. 既有门禁实跑（verification-before-completion）

本席只读 `tools/verify-html-ledger-sync.mjs` 与 `tools/verify-entry-anchors.mjs` 以核实测试含义，未改源码。

### 6.1 `npm run verify:html-ledger-sync`

含义：三份解释权 HTML 相对 `origin/main` merge-base 若有改（含工作区脏），必须同范围改 `docs/prd-audit/business-decision-ledger.md`。

```text
> entertainment-complex-management@0.1.0 verify:html-ledger-sync
> node tools/verify-html-ledger-sync.mjs

[html-ledger-sync] PASS: 改动了 1 份业务 HTML，决策台账已同步。比对范围：工作区（含未提交改动）↔ 基线 origin/main@001878a1。
html-ledger-sync_exit=0
```

**该命令：PASS。** 1 份治理 HTML + 台账同批，符合块 E–G。

### 6.2 `npm run verify:entry-anchors`

含义（源码）：① 注册表 `entry_body` 去空白后须在对应 HTML 现文；④⑤ 账本条目名同源且 `source_text` 在现文；⑥ 账本↔覆盖矩阵；⑦ 八个句子镜像列；② 全库 `docs/**/*.md`（EXCLUDE 不含 `audit-2026-09`）里 `BSM|PGM|ENUM|BEQ「…」` 须能落到唯一条目。检查②会扫进本轮审计 raw。

```text
> entertainment-complex-management@0.1.0 verify:entry-anchors
> node tools/verify-entry-anchors.mjs

[entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1563 条；被检 Markdown 361 份；条目名引用 1517 处（注册表原名 1357 处，正文唯一定位 158 处）。
[entry-anchors] FAIL：2 处不合格。
  - docs/audit-2026-09/modules/M002-b1-grok-raw.md:213：BSM「容量属于点位」在该 HTML 中定位不到任何条目
  - docs/audit-2026-09/modules/M002-b4-r1-self-raw.md:123：BSM「容量属于点位」在该 HTML 中定位不到任何条目
  处置：条目改名或 HTML 改动后，重建 docs/prd-audit/entry-registry.csv 并把引用改为现行条目名；一律不得写回行号。
entry-anchors_exit=1
```

**该命令：FAIL。不写成 PASS。**

根因（systematic-debugging，不修）：

1. 计数 1228/1235/1244 + 1563 片段与 M001 发布时绿基线相同；错误列表**没有**「注册表条目正文找不到」或「账本正文与 HTML 对不上」。①④⑤⑥⑦ 对 4 文件镜像同步未报警。
2. 两处 FAIL 都是检查②。`M002-b1-grok-raw.md:213` 本席实读为 B1 对抗式自问 `4. **Q BSM「容量属于点位」是否打脸本页容量条？**`——历史叙述，不是现行条目名引用。正则 `(BSM|PGM|ENUM|BEQ)\s*「([^」]{2,})」` 把它当成引用。`docs/audit-2026-09/` 在审计排除范围；提示词禁止修改原始审计报告。
3. 第二处路径是本次 self raw。本席**未读其正文**；只记录门禁输出。不得据此改 B1 raw 或门禁。

这不是 4 文件 diff 的工程缺陷，也不是无 ID 新业务问题。派发已说明 B3 脚本曾被该断言拦住、before 对象已写入。本席复现同一类 FAIL（现为 2 处，因冻结后多了一份 B4 self raw 被扫到）。**禁止把该 FAIL 折成「镜像未同步」。**

---

## 7. 三线（只覆盖本轮 diff）

### 7.1 第一性原理

- 为什么必须改？删掉块 A/B，申请20与 GEN-016 的互斥日期仍在。删掉 C/D，检查①⑤会红。删掉 E–G，`html-ledger-sync` 会红。
- 声称是否验证？第 5–6 节命令。HTML 两处与批准逐字相等；镜像等式成立；html-ledger-sync 绿。
- 未经验证就当真？无。锁引文未同步是已知负事实，已写进 V0.18，不冒充 LOCK-AMEND 完成。

### 7.2 顺从式

假定本批只执行队列为「是」的 001：HTML 两处、两行镜像、一台账记录，均落在 findings V0.25 与申请20内。002/003 队列否，diff 未碰终端别名与页面壳。通过。

### 7.3 对抗式（grilling 自答）

1. Q 有没有改未批准的第三处 HTML？无。另外两份 blob=HEAD=before=after。
2. Q 有没有顺手改终端别名（002）？`客人 App` 仍 2 处。
3. Q 有没有猜一个新日期？无；两处日期均删除。
4. Q 有没有改锁冒充 LOCK-AMEND？锁哈希未变，COUNT=0。
5. Q 有没有改 coverage / 增业务列？coverage 字节相同；CSV 列结构未变。
6. Q 台账 V0.18 有没有发明业务裁决？节内写「仅记录技术元数据」「不创设业务裁决」；正文是批准范围复述 + 哈希。
7. Q 未过滤 diff 是否藏了第五文件？`git diff --name-only --binary` 恰 4 路径。
8. Q entry-anchors FAIL 是否证明镜像错了？否；见 §6.2。

ponytail-review：`Lean already. Ship.` 可删的就是互斥日期，已经删了。`net: 重复日期声明已去掉。` 无多余抽象。

---

## 8. 发现

本轮 diff **无新的无 ID 工程缺陷**，不分配本席临时 GR 编号。

已定稿 ID 在本 diff 中的状态（B4 只记录，不出队、不改 findings）：

| ID | 分类 | 队列（定稿值） | 本 diff | 本席 |
|---|---|---|---|---|
| M002-001 | 冲突 | 是 | 已按申请20 + 三处辅助同步执行 | 修对了 |
| M002-002 | 细化 | 否 | 未出现 | 保持 |
| M002-003 | 技术性 | 否 | 未出现 | 保持 |

已知非 diff 事实（不升格为 B4 FAIL、不修）：

- PGM-066 锁内 HTML 原文块仍含旧页脚日期，待获批后的 V0.16 完整 LOCK-AMEND。
- `verify:entry-anchors` 因审计 raw 历史 `BSM「…」` 叙述而 FAIL。

未执行、因而**不声称已完成**：LOCK-AMEND、B5、模块 PASS、M001 重开。

---

## 9. B4 结论

**PASS**

不是有条件通过。合取项均已核对：两树真实且 diff 恰 4 文件；未过滤 binary diff 全文已收录；每一变更行归因 M002-001；HTML 两处与申请20逐字相符；镜像与台账属同一 ID 的必要同步；html-ledger-sync 实跑 PASS；entry-anchors 实跑 FAIL 已据源码与 B1:213 记录原因，未谎报为 PASS，且不构成 4 文件越界或未修完 001；锁未改。无待补项、无未答复添加项、无新 GR。

本节点结束。不进入 B5、不改 findings 队列、不写模块收口、不派子代理、不提交推送。
