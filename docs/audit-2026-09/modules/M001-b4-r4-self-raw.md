CRITERIA-LOCK-SHA256: 08a4dcc31d10624ca846560d70dcc79e391aeae5ebcdc62a9341f30ecb8de314
版本：V0.01
# M001 B4-r4 修复内容与范围归因审计（Codex 独立席）
日期：2026-09-08
结论：内容 PASS；范围归因 PASS；本席 B4 PASS。尚未读取本次Grok B4报告，不能以本席票替代双PASS；不是模块PASS。

## 输入与本次边界
main HEAD=242f487233d028173cb9198eecd079f5d877405c。findings V0.83已由根b1 V0.35和Grok b1 V0.15完成整批B2双PASS；新ID M001-016为唯一执行项。旧005保持已修/否，新014/015保留Grok冲突票但用户拒绝本批修复，不能以拒绝当无缺陷或换位置修。用户裁决为A/D同意、B/C拒绝，随后授权原话为“授权仅为 A、D 再执行 1 次修复”，ledger V1.52记录。额外仅1次已用尽，累计B3=4，不清零历史3次，没有第五次授权。本阶段审全量两树diff，不替代B5全模块。
before tree=4263c8c1338031a38d977741ffb111a0752b7092
after tree=7d6c4f7544de2957faec09e04aa2a262ba4aad86
完整diff SHA256=57e3f181e0415c093f50322a26c826eb26623484992492499e30eadd65d0be46；2013字节；唯一路径docs/business-system-map-for-operations-v0.1.html。两次新临时索引运行git add -A/write-tree，实际暂存区SHA未变；两个tree覆盖tracked/staged/untracked完整工作树。以下是实际未过滤git diff --binary全文：

~~~diff
diff --git a/docs/business-system-map-for-operations-v0.1.html b/docs/business-system-map-for-operations-v0.1.html
index d71ca742..12334b13 100644
--- a/docs/business-system-map-for-operations-v0.1.html
+++ b/docs/business-system-map-for-operations-v0.1.html
@@ -1,5 +1,5 @@
 <!doctype html>
-<!-- 文件版本：V0.12；更新日期：2026-09-08 -->
+<!-- 文件版本：V0.13；更新日期：2026-09-08 -->
 <html lang="zh-CN">
 <head>
   <meta charset="utf-8">
@@ -1377,7 +1377,7 @@
                 <!-- 2026-08-04 第三轮用户裁决（DEC-R6-14）：物业工单来源补「客人」——客人端投诉，客人即提单方、关闭须其确认 -->
                 <li>工单来源包括：商户（商户侧自助提单）、客人（消费客人通过客人端投诉，客人即提单方；客户关怀部记录问题解决方案及客人反馈后可以关闭）、现场工作人员，以及楼宇设备告警和巡检异常自动生成。</li>
                 <li>派单到具体人员或班组，记录处理过程、用料和结果。用料必须从指定库房出库并按当前平均进货价计成本，同时在工单上标明费用承担方：公区设施与楼宇主体设备的用料由物业承担；商户自有设施、以及因商户责任导致损坏的用料由该商户承担，并自动生成一笔应收进入其费用账单。承担方有争议的，工单挂起转上级判定，判定结果与依据留痕后才能结案。</li>
-                <li>处理后回访客人或商户，验收合格才关闭。</li>
+                <li>处理后的回访、验收和关闭，按下述已确认的工单类型分支规则执行。</li>
                 <li>按响应时限和处理时限统计时效，超时自动升级通知主管。</li>
                 <li>记录满意度评价，作为服务质量和商户表现的依据。</li>
                 <!-- 2026-06-25 修改阶段：补物业工单运营细则，依据 M1 需求采集（complex-m1-requirements §4）。 -->
~~~

## 全部hunk与逐行归因
1. 第1个hunk，L2版本注释V0.12→V0.13，日期2026-09-08不变：M001-016的获批D，属于该业务正文修订必须携带的自身版本递增。只有该注释一行变化，doctype/lang/charset均不动。
2. 第2个hunk，L1380概括句：“处理后回访客人或商户，验收合格才关闭。”→“处理后的回访、验收和关闭，按下述已确认的工单类型分支规则执行。”：M001-016的获批A，逐字一致。上下文中的来源、承担方、响应时限、满意度及详细分支未改。没有第三hunk、其他路径或无ID改动。D/A合计2增2删。

## 内容、业务效果与对抗核验
原概括把全部工单称“验收合格才关闭”；其下明确保洁完成即结、商户投诉/消费客人投诉按已确认条件关闭、复杂报修双确认。A将概括显式接到这些原有分支，未新增关闭条件、未取消已有客户关怀部职责，也不改投诉/报修分支。BEQR262和BSM1384在新旧字节完全一致。旧005详细投诉规则保持，不把本次新016说成旧005未修。
本次B/C两个拒绝位置及L521全局分类原文完全不动，用户批准从未被扩大。014/015整模块业务冲突仍由后续全量审计独立判定；它们不属于此次diff新引入的问题，不能作为修改未授权位置的理由。
类型1证据：批准原句/新句和上方完整diff；类型4证据：实际tree版本比较、当前文件与after逐字一致、唯一新句/旧句消失、完整反向还原原字节、标签序列一致、numstat2/2、UTF-8严格解码。无业务系统可执行行为改变，按runbook文档类豁免Maven/UI集成测试，不冒称运行业务测试或浏览器验收。
校验脚本首次将HTML版本注释也计入结构字符串，导致结构相等断言失败；这是检查口径未排除本次获批版本变化，不是HTML内容失败。仅修正检查为比较真实HTML标签序列后重跑通过；没有再写HTML、没有第二次B3。之前的原文/反向字节断言均未失败。
本次实跑正确检查命令：
~~~bash
python3 - <<'PY'
from pathlib import Path
import subprocess,json,hashlib,re
b=Path('/home/james/complex-management');before='4263c8c1338031a38d977741ffb111a0752b7092';after='7d6c4f7544de2957faec09e04aa2a262ba4aad86';path='docs/business-system-map-for-operations-v0.1.html'
old=subprocess.check_output(['git','show',before+':'+path],cwd=b);new=subprocess.check_output(['git','show',after+':'+path],cwd=b)
changes=[i for i,(x,y) in enumerate(zip(old.splitlines(),new.splitlines()),1) if x!=y];assert changes==[2,1380]
assert new==(b/path).read_bytes()
assert b'V0.13' in new.splitlines()[1]
assert '处理后的回访、验收和关闭，按下述已确认的工单类型分支规则执行。'.encode() in new.splitlines()[1379]
assert '处理后回访客人或商户，验收合格才关闭。'.encode() not in new
assert '处理后的回访、验收和关闭，按下述已确认的工单类型分支规则执行。'.encode() not in old
reverse=new.replace('处理后的回访、验收和关闭，按下述已确认的工单类型分支规则执行。'.encode(),'处理后回访客人或商户，验收合格才关闭。'.encode(),1).replace(b'V0.13',b'V0.12',1)
assert reverse==old
assert re.findall(rb'</?[A-Za-z][^>]*>',old)==re.findall(rb'</?[A-Za-z][^>]*>',new)
diff=subprocess.check_output(['git','diff','--binary',before,after],cwd=b);assert hashlib.sha256(diff).hexdigest()=='57e3f181e0415c093f50322a26c826eb26623484992492499e30eadd65d0be46'
num=subprocess.check_output(['git','diff','--numstat',before,after],cwd=b,text=True).strip();assert num=='2\t2\t'+path
print(json.dumps({'before_tree':before,'after_tree':after,'changed_lines':changes,'old_missing_approved_text':True,'new_has_approved_text':True,'reverse_exact':True,'tags_structure_unchanged':True,'numstat':num,'full_diff_sha256':hashlib.sha256(diff).hexdigest(),'utf8_strict':bool(new.decode('utf-8',errors='strict'))},ensure_ascii=False))
PY
~~~
实际输出：
~~~json
{"before_tree": "4263c8c1338031a38d977741ffb111a0752b7092", "after_tree": "7d6c4f7544de2957faec09e04aa2a262ba4aad86", "changed_lines": [2, 1380], "old_missing_approved_text": true, "new_has_approved_text": true, "reverse_exact": true, "tags_structure_unchanged": true, "numstat": "2\t2\tdocs/business-system-map-for-operations-v0.1.html", "full_diff_sha256": "57e3f181e0415c093f50322a26c826eb26623484992492499e30eadd65d0be46", "utf8_strict": true}
~~~

## 判据完整性与已知必要后续
B3前四值：
- 实算=08a4dcc31d10624ca846560d70dcc79e391aeae5ebcdc62a9341f30ecb8de314
- sidecar=08a4dcc31d10624ca846560d70dcc79e391aeae5ebcdc62a9341f30ecb8de314
- ledger=08a4dcc31d10624ca846560d70dcc79e391aeae5ebcdc62a9341f30ecb8de314
- 入场b1 Grok最后独占行=08a4dcc31d10624ca846560d70dcc79e391aeae5ebcdc62a9341f30ecb8de314
B3后四值：
- 实算=08a4dcc31d10624ca846560d70dcc79e391aeae5ebcdc62a9341f30ecb8de314
- sidecar=08a4dcc31d10624ca846560d70dcc79e391aeae5ebcdc62a9341f30ecb8de314
- ledger=08a4dcc31d10624ca846560d70dcc79e391aeae5ebcdc62a9341f30ecb8de314
- 同一b1 Grok最后独占行=08a4dcc31d10624ca846560d70dcc79e391aeae5ebcdc62a9341f30ecb8de314
本次锁、sidecar、ledger基准没有修改，COUNT1。BSM-079（来源BSM1374起）摘录仍含修前A句；这是事先登记的获批HTML必要同步，不是本次B3越界。B4双PASS后必须另走LOCK-AMEND完整五步：登记全部变化ID及HTML批准出处→定义/映射一起核→sidecar→双席完整diff反向旧hash及全量一致→④更新基准COUNT按获批同步保持1→⑤最早引用M001 PENDING后B1重开。不能在本B4跳过锁同步或宣布模块PASS。D不在任何锁摘录内。

## 技能与结论边界
沿用本会话已实读并按提示词收窄的verification-before-completion（命令先于结论）、receiving-code-review（修正B2错误编号）、requesting-code-review（唯一Grok实审、禁派第三席）、systematic-debugging/diagnosing-bugs（先定位概括与分支，检查脚本失败区分内容与验证口径）、grilling（全diff/批准/业务效果自问自答）、ponytail与ponytail-review（只两行、无额外业务或测试框架）、writing-for-agents（证据准确）。文档内容修订不强制新增TDD测试文件。batch-grill-me/ubiquitous-language仍是此前已实核缺口，不冒称调用；未派任何子代理。
本席完整diff内容PASS、范围归因PASS。两项批准已实施，但Grok本次票尚未接收，016尚不能出队，M001仍IN_PROGRESS，尚非模块PASS。无提交、推送、部署；未开下一模块。
