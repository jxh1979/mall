CRITERIA-LOCK-SHA256: 73ce1c4523de229a11552ca343f594e1506da3dc10e68d3dac216dd4ca232439

# M002 B4-r1 Codex完整复审
版本：V0.01
步骤：B4；控制V0.16；M002/322，IN_PROGRESS，B3=1。M001已发布，不重开。
本席已读取完整未过滤binary diff及4文件现值，未读取本次Grok B4 raw/JSON；本报告完整落盘后才允许读对方。

## 1. 完整输入与取证
before tree：bf400f2ebb21a7e112bfddfc8514b9d0cd4560ca
after tree：edc03163560e2373adb503a97cd22d76928d5ce0
完整diff SHA256：409889c17bfbc24392efbc7ee3cfc38e9bedb963bcb566d89c45c243f44247ac
当前真实索引SHA256：040cc6a55775bb41014c00b6ebc2728983138f2bea8289e55263ff377684e9ad，B3未改。
B2输入findings V0.24双PASS，V0.25只登记定稿。申请20实际答“可以”，仅批准治理HTML2/974两处日期删除和V0.08；队列仅M002-001。B3辅助镜像及批准台账与同一001对应，执行前已列findings定稿记录。
首次B3已写before tree但验证断言失败导致未打印。恢复验证：从4文件已实核HEAD前像及其余未变路径在临时索引复算，与首次B3已存在Git tree对象完全一致；不是事后口头编造基线。随后取得after tree才将B3次数加1，完整diff恰4文件。

复审实际使用requesting-code-review（只派Grok、不派第三席）、receiving-code-review（先核事实、不边审边修）、verification-before-completion、ponytail最小范围口径，实际SKILL.md已读取。本节点不额外修复。

## 2. 完整binary diff（无路径过滤）
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

## 3. 逐块与逐行归因

| diff文件/块 | ID | 批准与正确性核实 |
|---|---|---|
| 治理HTML第2行 | M002-001 | 逐字等于申请20：仅日期删除、V0.07→V0.08；无新日期猜测 |
| 治理HTML第974行 | M002-001 | 仅删除指定日期句；用途及完整解释权逐字保留 |
| authority-rule-ledger.csv PGM-0192 source_text | M002-001 | 镜像同一旧日期删除，1235行不变，其他所有字段逐字不变 |
| entry-registry.csv PGM-E0307 entry_body | M002-001 | 镜像同一旧日期删除，1228行不变，ID/名称/行锚/所有其他字段不变 |
| business-decision-ledger.md V0.17→V0.18及当前事实源 | M002-001 | 为本次批准记录递增版本，将实际M002批准纳入来源，未反向改写业务规则 |
| business-decision-ledger.md末尾新增8行 | M002-001 | 精确记录批准、镜像范围、当前源SHA与尚待锁同步，不伪称后续LOCK-AMEND已完成 |

4文件全部新增/删除行可归因001。没有源码越界、没有改002/003、没有改M001三份已审业务规则。CSV原schema没有版本字段，本次镜像版本由V0.18现有台账记录，与已有格式一致；没有擅加列。
治理HTML当前979行、V0.08，SHA256=9864216e609df2f319fa2cf7e51b61f06003ce45dff64a1a4098e71435865a68。在内存红绿验证里，旧元数据同时有两个日期（FAIL_EXPECTED），拟稿日期集合空（PASS），66标题、31链接、4表、解释权正文不变；B4对现值与获批旧文本精确替换再次断言全等。

## 4. 实际验证输出（原脚本失败未掩盖）
```json
[
  {
    "command": "npm run verify:html-ledger-sync",
    "exit": 0,
    "stdout": "\n> entertainment-complex-management@0.1.0 verify:html-ledger-sync\n> node tools/verify-html-ledger-sync.mjs\n\n[html-ledger-sync] PASS: 改动了 1 份业务 HTML，决策台账已同步。比对范围：工作区（含未提交改动）↔ 基线 origin/main@001878a1。\n",
    "stderr": ""
  },
  {
    "command": "npm run verify:entry-anchors",
    "exit": 1,
    "stdout": "\n> entertainment-complex-management@0.1.0 verify:entry-anchors\n> node tools/verify-entry-anchors.mjs\n\n[entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1563 条；被检 Markdown 360 份；条目名引用 1516 处（注册表原名 1357 处，正文唯一定位 158 处）。\n",
    "stderr": "[entry-anchors] FAIL：1 处不合格。\n  - docs/audit-2026-09/modules/M002-b1-grok-raw.md:213：BSM「容量属于点位」在该 HTML 中定位不到任何条目\n  处置：条目改名或 HTML 改动后，重建 docs/prd-audit/entry-registry.csv 并把引用改为现行条目名；一律不得写回行号。\n"
  }
]
```
html-ledger-sync PASS；entry-anchors当前原脚本FAIL。注册表/账本/镜像检查没有报错，唯一错误来自已封存的本轮Grok B1历史报告。下方纯内存诊断不能替代原命令PASS，也未写门禁代码。

## 5. 本方全部发现：CX-01，历史审计raw被引用门禁当作现行要求

- 文件：tools/verify-entry-anchors.mjs:38–39、226–250；本次可复现场景由docs/audit-2026-09/modules/M002-b1-grok-raw.md:213触发。
- 原命令：npm run verify:entry-anchors，exit=1，错误全文见上一节。
- 根因：门禁自身L14和L38已有“历史留痕文件除外/一律不改、不检”的口径，但EXCLUDE仅识别旧报告命名，没有识别本轮固定路径B1/B4/B5的*-raw.md；raw中的审计提问/已否定猜想被REF匹配成现行条目名。该raw必须原样保留，不能为了门禁改写Grok证据。
- 第一次正常跑就已发生，直接阻止M002按操作员要求通过发布门禁，不是猜想极端场景。此问题由验证修复包的实际命令发现；不是声称原HTML源日期仍未修好。
- 分类候选：技术性（无业务发明）；工程缺陷是；依据类型4（实际失败命令和可复现内存诊断）；冻结GEN-013（脚本/CI入口可复现且与其声明相容）、GEN-016（历史证据不得被当作当前文档事实）。不是HTML新业务规则，不要求修改业务HTML。B2仍须Grok独立复核分类和处理范围，当前不擅分配正式ID。
- 最小候选：只让既有“历史留痕不检”识别本次指定modules目录下M编号+B1/B4/B5的self/grok原始报告；findings、ledger、模块收口文件和普通文档继续检查。不得整目录跳过、不改REF内容校验、不改业务镜像规则。
- 诊断：仅在内存将EXCLUDE增加精确raw路径，当前全量门禁变PASS；源脚本仍逐字未变。正例覆盖b1/b4-r1/b5-r0，负例保留findings/ledger/模块收口/普通文档/非规范raw名称均不排除。完整诊断命令见本报告附录；不是已修。
- 无其他CX项。已批准日期和镜像本身正确；本次diff行归因无越界。

## 6. 本方B4结论

FAIL。

原因是修复包仍无法通过既有发布引用门禁，存在上列无ID的真实技术性阻塞，不把诊断PASS冒充原脚本PASS。等待Grok完整审完本轮diff后，按B4统一回退规则整批归并；若本问题成立，须B2分配新ID及双席定稿后才修，不能现在改门禁。001尚未在双PASS B4后出队，当前不擅自改队列；是否重复源码操作必须按现值判断，不能二次删除日期或二次升级HTML版本。

判据锁仍V0.05原字节，四值锚未变。PGM-066引文同步属后续完整LOCK-AMEND，尚未执行；本次不因未来步骤尚未执行而假报其完成，也不将其作新业务缺陷。M002尚未模块PASS，M001保持已发布PASS。

## 7. 内存诊断（不写源文件）
```javascript
import fs from 'node:fs';import assert from 'node:assert/strict';
const oldLine="const EXCLUDE = /(changelog|change-log|-report|\\.report\\.|diff-matrix|anchor-rebuild|html-map|reanchor|html-mirror-scan|history-index|[\\\\/]rounds[\\\\/])/i;";const newLine="const EXCLUDE = /^docs\\/audit-2026-09\\/modules\\/M\\d{3}-b(?:1|4-r\\d+|5-r\\d+)-(?:self|grok)-raw\\.md$|(changelog|change-log|-report|\\.report\\.|diff-matrix|anchor-rebuild|html-map|reanchor|html-mirror-scan|history-index|[\\\\/]rounds[\\\\/])/i;";
const s=fs.readFileSync('tools/verify-entry-anchors.mjs','utf8');assert.equal(s.split(oldLine).length-1,1);
const declaration=newLine.replace("const EXCLUDE = ","").slice(0,-1); const excludes=eval(declaration);
assert(excludes.test('docs/audit-2026-09/modules/M002-b1-grok-raw.md'));assert(excludes.test('docs/audit-2026-09/modules/M002-b4-r1-self-raw.md'));assert(excludes.test('docs/audit-2026-09/modules/M002-b5-r0-grok-raw.md'));
for(const p of ['docs/audit-2026-09/findings.md','docs/audit-2026-09/ledger.md','docs/audit-2026-09/modules/M002-00-authority-html-02.md','docs/domain/guide.md','docs/audit-2026-09/modules/not-a-raw.md'])assert(!excludes.test(p),p);
console.log('DIAGNOSTIC_ONLY: original file unchanged; exact historical raw matcher checked'); await import('data:text/javascript;base64,'+Buffer.from(s.replace(oldLine,newLine)).toString('base64'));

```
诊断实际输出：
```text
DIAGNOSTIC_ONLY: original file unchanged; exact historical raw matcher checked
[entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1563 条；被检 Markdown 350 份；条目名引用 1514 处（注册表原名 1357 处，正文唯一定位 157 处）。
[entry-anchors] PASS：条目名唯一、注册表条目正文逐字与 HTML 现文一致、引用可唯一定位、正文无行号锚点、账本正文与九个镜像列均与 HTML 现文一致、账本与承接矩阵一一对应。

```
