CRITERIA-LOCK-SHA256: 16def02336743500166af4cfd5fff48d02731d63d45a8f5f72721ca8ebd408e3

# M001 B4-r3 — Codex 修复完整差异复审

版本：V0.01
日期：2026-09-08
本席结论：B4 PASS。仅当前修复diff通过，不是模块PASS；后续锁同步与B5不可省。
范围：本模块第3次B3的全部before/after tree差异。Grok本次B4已完成且交付exit0；本席在未读该raw或CLI JSON的前提下执行下列独立检查并完整写本报告。没有第三席/子代理。

## 本轮双树与未过滤完整diff

before tree：289ed630b653d87e56032dd5c428ea3ae59d0271
after tree：9d8a60e50598e1eb01559f8636635dd769c32bd7

原始命令：`git -C /home/james/complex-management diff --binary 289ed630b653d87e56032dd5c428ea3ae59d0271 9d8a60e50598e1eb01559f8636635dd769c32bd7`
未加任何路径过滤；该命令的全部输出如下：

```diff
diff --git a/docs/business-system-map-for-operations-v0.1.html b/docs/business-system-map-for-operations-v0.1.html
index 16783edc..d71ca742 100644
--- a/docs/business-system-map-for-operations-v0.1.html
+++ b/docs/business-system-map-for-operations-v0.1.html
@@ -1,5 +1,5 @@
 <!doctype html>
-<!-- 文件版本：V0.11；更新日期：2026-09-07 -->
+<!-- 文件版本：V0.12；更新日期：2026-09-08 -->
 <html lang="zh-CN">
 <head>
   <meta charset="utf-8">
@@ -496,7 +496,7 @@
           <a href="#terminals">各类入口</a>
           <a href="#relations">相互关系</a>
           <a href="#delivery">系统交付与上线</a>
-          <a href="#questions">已拍板/待确认</a>
+          <a href="#questions">已确认规则和实施前输入项</a>
         </nav>
       </aside>
 
@@ -2000,7 +2000,7 @@
           </div>
         </section>
 
-        <p class="footer-note">更新日期：2026-09-01。用途：给不懂产品和技术的业务人员理解综合体管理系统范围。<br><strong>解释权声明：</strong>本《业务系统地图》与《平台治理地图》《业务枚举速查表》共同组成三份最高业务解释权 HTML。发生冲突时，只以这三份 HTML 中经用户批准的内容为准；PRD、其他文档、后台配置、设计和实现均不得覆盖。某项业务内容若尚未写入三份 HTML，不因出现在 PRD、配置或实现中而取得效力，必须先逐项获得用户批准并补入相应 HTML。三份 HTML 之间若出现冲突，按确认时间线判定：凡经用户确认过的内容，确认时间越晚的优先级越高，后确认的覆盖先确认的（以决策台账记录的裁决时间为准）；无法确定确认时间先后的，仍须逐项重新裁决。</p>
+        <p class="footer-note">更新日期：2026-09-08。用途：给不懂产品和技术的业务人员理解综合体管理系统范围。<br><strong>解释权声明：</strong>本《业务系统地图》与《平台治理地图》《业务枚举速查表》共同组成三份最高业务解释权 HTML。发生冲突时，只以这三份 HTML 中经用户批准的内容为准；PRD、其他文档、后台配置、设计和实现均不得覆盖。某项业务内容若尚未写入三份 HTML，不因出现在 PRD、配置或实现中而取得效力，必须先逐项获得用户批准并补入相应 HTML。三份 HTML 之间若出现冲突，按确认时间线判定：凡经用户确认过的内容，确认时间越晚的优先级越高，后确认的覆盖先确认的（以决策台账记录的裁决时间为准）；无法确定确认时间先后的，仍须逐项重新裁决。</p>
       </div>
     </main>
   </div>
```

## 每块及每条增删行归因

| hunk | 原/新行 | 定稿且修复队列归属是的ID | 判据/批准/检查 |
|---|---|---|---|
| @@ -1,5 +1,5 @@ | 第2行一删一增 | M001-012 | GEN-016；批准第1处整注释改V0.12/2026-09-08。单次版本递增归本ID，无其他头部变化 |
| @@ -496,7 +496,7 @@ | 第499行一删一增 | M001-013 | GEN-016、BSM-120；第3处看到完整业务上下文后用户答“可以”；目录沿用原h2文字，href保持，其他导航不变 |
| @@ -2000,7 +2000,7 @@ | 第2003行一删一增 | M001-012 | GEN-016；批准第2处只改日期，整行其余用途/解释权逐字相同 |

唯一变化路径是BSM。完整3个hunk、3增3删均可归因；没有无ID行、业务扩写、格式化或额外文件。其他两份HTML与before/after/current字节相同。BSM其余2029行逐行相同。文件仍2032行，严格UTF-8。

## 修复效果与正常路径反例

1. 日期：从实际修改后HTML的头部与可见footer分别解析，均2026-09-08；V0.11只递增一次为V0.12。旧文件同命令不相等的实际反例已经存在B1 raw，现相等，不是永绿检查。
2. 目录：实际nav文字与目标h2均“已确认规则和实施前输入项”；href仍#questions，目标章节正文原样保留。L1994“第三方接口资料当前未确定”保持，不把confirmed误当资料齐全。
3. 边界：逐行比较tree blob，不依赖已提交diff，不以git status空当通过。通过逐一替换获批原句，证明只有这三个预期差异；真实工作区文件等于after tree blob。
4. BSM-120冻结摘录仍含旧页脚日期，是本次已登记的获批HTML同步事项；本轮B3没有改lock/sidecar/基准，B3前后四值通过。后续必须严格LOCK-AMEND双审、旧hash逆向校验、更新基准、从M001 B1重开；本票不宣称当前401条摘录与修改后HTML已全匹配。

## 实跑命令与输出

```bash
python3 - <<'PY'
from pathlib import Path
import subprocess,hashlib,re,json
p=Path('/home/james/complex-management');before='289ed630b653d87e56032dd5c428ea3ae59d0271';after='9d8a60e50598e1eb01559f8636635dd769c32bd7';path='docs/business-system-map-for-operations-v0.1.html'
def git(*args):return subprocess.check_output(['git','-C',str(p),*args])
old=git('show',before+':'+path);new=git('show',after+':'+path);actual=(p/path).read_bytes()
assert actual==new
paths=git('diff','--name-only',before,after).decode().splitlines();assert paths==[path]
a,b=old.decode('utf-8').splitlines(),new.decode('utf-8').splitlines();assert len(a)==len(b)==2032
changed=[i for i,(x,y) in enumerate(zip(a,b),1) if x!=y];assert changed==[2,499,2003]
assert b[1]=='<!-- 文件版本：V0.12；更新日期：2026-09-08 -->'
assert b[498]==a[498].replace('已拍板/待确认','已确认规则和实施前输入项')
assert b[2002]==a[2002].replace('更新日期：2026-09-01。','更新日期：2026-09-08。')
text=new.decode('utf-8')
header=re.search(r'<!-- 文件版本：[^\n]+更新日期：(\d{4}-\d{2}-\d{2})',text)[1]
footer=re.search(r'<p class="footer-note">更新日期：(\d{4}-\d{2}-\d{2})',text)[1]
nav=re.search(r'<a href="#questions">([^<]+)</a>',text)[1]
title=re.search(r'<section id="questions">\s*<h2>([^<]+)</h2>',text)[1]
assert header==footer=='2026-09-08' and nav==title
diff=git('diff','--binary',before,after).decode('utf-8');assert len(re.findall(r'^@@ ',diff,re.M))==3
for f in ['docs/business-enum-quick-reference-v0.1.html','docs/platform-governance-map-for-operations-v0.1.html']:
 assert git('show',before+':'+f)==git('show',after+':'+f)==(p/f).read_bytes()
lock=p/'docs/audit-2026-09/plan/criteria-lock.md'
print(json.dumps({'before_tree':before,'after_tree':after,'changed_paths':paths,'changed_lines':changed,'hunks':3,'date_consistency':'PASS','nav_title_consistency':'PASS','other_HTMLs_unchanged':True,'all_other_lines_unchanged':True,'HTML_sha256':hashlib.sha256(new).hexdigest(),'lock_sha256':hashlib.sha256(lock.read_bytes()).hexdigest(),'attribution':{'2':'M001-012','499':'M001-013','2003':'M001-012'}},ensure_ascii=False))
PY
```

实际成功输出：
```json
{"before_tree": "289ed630b653d87e56032dd5c428ea3ae59d0271", "after_tree": "9d8a60e50598e1eb01559f8636635dd769c32bd7", "changed_paths": ["docs/business-system-map-for-operations-v0.1.html"], "changed_lines": [2, 499, 2003], "hunks": 3, "date_consistency": "PASS", "nav_title_consistency": "PASS", "other_HTMLs_unchanged": true, "all_other_lines_unchanged": true, "HTML_sha256": "86a73b25db127f2c7d5aecacecdb1dc0ad9cbd872031add30bc52efc744e4b3f", "lock_sha256": "16def02336743500166af4cfd5fff48d02731d63d45a8f5f72721ca8ebd408e3", "attribution": {"2": "M001-012", "499": "M001-013", "2003": "M001-012"}}
```

## 技能与产物边界

实际使用此前已完整读取的requesting-code-review（仅派Grok，基线用双tree）、verification-before-completion、receiving-code-review、systematic-debugging/diagnosing-bugs，以及ponytail最小修改结构。HTML内容修复按V0.17 §6用类型4命令验证，不新造测试文件。未修改业务/计划/台账来通过复审；本席唯一写本raw。Grok结果尚未读取，下一步读它的完整B4报告做合取；任何FAIL都不能被本票覆盖。

当前B3累计3，本次已执行1；012/013当前仍在队列，只有B4双PASS之后才能改为否。本席B4 PASS，尚未双席合取、未模块收口、未提交推送。
