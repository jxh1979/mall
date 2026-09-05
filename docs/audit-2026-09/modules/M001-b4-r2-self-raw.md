CRITERIA-LOCK-SHA256: d4ba138e924c6fb13020cf26e55aef537a9488ba84dfb72ca9d0f851b5090b80
# M001 B4-r2 Codex 完整修复内容审计
版本：V0.01

UTC 2026-09-05T05:10:14.902484+00:00

## 独立性、范围及结论

Grok本次已交付并退出0；截至本报告落盘，Codex未读取M001-b4-r2-grok-raw.md正文或其CLI JSON。Codex已独立完整阅读本轮188行binary diff（1–94、95–188两段，无截断）及当前定稿清单、相关冻结通用判据和原文上下文。

本席B4结论：PASS。越界判定：PASS。当前只判完整修复diff，不宣称模块B5或全仓PASS。范围为两个完整git add -A快照tree的全部差异，不按路径过滤。before_tree=fa2e80597255da1277291d5ba2e8146f9334bd53；after_tree=ff5573a74f5ed84f6bc84d73ad85af7a63a4d961。

## 逐项审计

1. M001-005：四处替换逐字复现申请14/18/19最终批准；两份HTML共用相同商户与消费客人两类规则。商户正常确认、既有规则无法解决、已解决而未确认/未点击的例外，以及消费客人记录方案与反馈后关闭均保留；没有引回已废弃的主管审批、确认超时、无回应条件。既有工单时效和复杂设备验收未变。GEN-016/017/018支持可复核授权和来源分类；两文件自身版本递增归因该已定稿ID。
2. M001-006：只填原空答案；枚举引用紧邻“容量双阈值”，地图引用紧邻“容量控制”，两处均为既有单项目实时在玩人数规则。没有新增园区总量控制或数字阈值。GEN-001/016/017成立。
3. M001-008：13表各补问题/答案两列scope=col表头，沿用td样式给th并左对齐；197个td.k标签、锚点和链接不变。GEN-015基本表头语义缺陷得到修复。HTMLParser确认标签配对、13组表头和原有导航；没有声称浏览器截图或辅助技术实机测试。
4. 对抗式核查：以before tree字节重新播放已批准20处替换+2版本，结果与after tree及现盘逐字节相等，排除顺手改文、漏改、陈旧未批准稿。对全部18个hunk的增删行逐行归因，21个不同源行均对应005/006/008；map1384含两处替换所以替换总数不等于源行数。治理地图、真实index及HEAD均未改变。
5. 边界：文档/HTML内容修复采用runbook允许的可复现命令，未生成伪业务测试、未宣称后端行为已实现。锁内旧引用尚未更新，不把本B4 PASS冒充新锁全量审计；若另一席也PASS，按既定LOCK-AMEND五步同步现有受影响引文后从最早模块B1复核。

## 全部hunk逐块归因

- docs/business-enum-quick-reference-v0.1.html @@ -1,5 +1,5 @@：M001-005；实际变更源行 2。
- docs/business-enum-quick-reference-v0.1.html @@ -11,7 +11,7 @@：M001-008；实际变更源行 14。
- docs/business-enum-quick-reference-v0.1.html @@ -25,7 +25,7 @@：M001-008；实际变更源行 28。
- docs/business-enum-quick-reference-v0.1.html @@ -61,7 +61,7 @@：M001-008；实际变更源行 64。
- docs/business-enum-quick-reference-v0.1.html @@ -96,7 +96,7 @@：M001-008；实际变更源行 99。
- docs/business-enum-quick-reference-v0.1.html @@ -136,14 +136,14 @@：M001-006,M001-008；实际变更源行 139,146。
- docs/business-enum-quick-reference-v0.1.html @@ -155,7 +155,7 @@：M001-008；实际变更源行 158。
- docs/business-enum-quick-reference-v0.1.html @@ -183,7 +183,7 @@：M001-008；实际变更源行 186。
- docs/business-enum-quick-reference-v0.1.html @@ -204,7 +204,7 @@：M001-008；实际变更源行 207。
- docs/business-enum-quick-reference-v0.1.html @@ -213,7 +213,7 @@：M001-008；实际变更源行 216。
- docs/business-enum-quick-reference-v0.1.html @@ -222,7 +222,7 @@：M001-008；实际变更源行 225。
- docs/business-enum-quick-reference-v0.1.html @@ -244,7 +244,7 @@：M001-008；实际变更源行 247。
- docs/business-enum-quick-reference-v0.1.html @@ -259,7 +259,7 @@：M001-005；实际变更源行 262。
- docs/business-enum-quick-reference-v0.1.html @@ -275,7 +275,7 @@：M001-008；实际变更源行 278。
- docs/business-enum-quick-reference-v0.1.html @@ -286,13 +286,13 @@：M001-008；实际变更源行 289,295。
- docs/business-system-map-for-operations-v0.1.html @@ -1,5 +1,5 @@：M001-005；实际变更源行 2。
- docs/business-system-map-for-operations-v0.1.html @@ -1292,7 +1292,7 @@：M001-006；实际变更源行 1295。
- docs/business-system-map-for-operations-v0.1.html @@ -1375,13 +1375,13 @@：M001-005；实际变更源行 1378,1384。

## 本次可复现检查命令

在160:/home/james/complex-management用python3标准输入执行下列完整脚本（只读，无Grok正文输入）。真实退出码0。

```python
from pathlib import Path
from html.parser import HTMLParser
import subprocess,re,json,hashlib
r=Path('/home/james/complex-management');a=r/'docs/audit-2026-09'
def git(*args):return subprocess.run(['git',*args],cwd=r,capture_output=True,check=True).stdout
bt='fa2e80597255da1277291d5ba2e8146f9334bd53';at='ff5573a74f5ed84f6bc84d73ad85af7a63a4d961'
f=(a/'findings.md').read_text();f=f.split('## 历史候选、原始意见及逐处批准')[0]
fence=chr(96)*3
edits=json.loads(re.search(re.escape(fence)+r'json\n(.*?)\n'+re.escape(fence),f,re.S).group(1))
assert len(edits)==20
queue=[l.split('|')[1].strip() for l in f.splitlines() if l.startswith('| M001-') and l.split('|')[5].strip()=='是']
assert queue==['M001-005','M001-006','M001-008']
paths=git('diff','--name-only',bt,at).decode().splitlines()
assert set(paths)==set(e['path'] for e in edits)
diff=git('diff','--binary',bt,at)
assert hashlib.sha256(diff).hexdigest()=='041e46f19586f8ed63cdf0cfd89895b3afcd5e79e1586567a029d8bb217e908e'
assert len(diff)==30865 and len(diff.splitlines())==188
changed={};sources={};allowed={}
class Parse(HTMLParser):
 def __init__(self):
  super().__init__(convert_charrefs=True);self.stack=[];self.labels=[];self.label=None;self.headers=[];self.head=None;self.tables=0;self.ids=[];self.hrefs=[]
 def handle_starttag(self,t,attrs):
  d=dict(attrs)
  if t not in {'meta','link','br','hr','img','input','source','wbr','area','base','col','embed','param','track'}:self.stack.append(t)
  if t=='td' and 'k' in d.get('class','').split():self.label=''
  if t=='th' and d.get('scope')=='col':self.head=''
  if t=='table':self.tables+=1
  if 'id' in d:self.ids.append(d['id'])
  if d.get('href','').startswith('#'):self.hrefs.append(d['href'][1:])
 def handle_data(self,t):
  if self.label is not None:self.label+=t
  if self.head is not None:self.head+=t
 def handle_endtag(self,t):
  assert self.stack.pop()==t
  if t=='td' and self.label is not None:self.labels.append(self.label);self.label=None
  if t=='th' and self.head is not None:self.headers.append(self.head);self.head=None
for path in paths:
 before=git('show',bt+':'+path);after=git('show',at+':'+path)
 assert (r/path).read_bytes()==after
 lines=before.decode('utf-8').splitlines(keepends=True);newlines=lines.copy();allowed[path]={}
 for e in [e for e in edits if e['path']==path]:
  assert newlines[e['line']-1].count(e['old'])==1
  newlines[e['line']-1]=newlines[e['line']-1].replace(e['old'],e['new'])
  allowed[path].setdefault(e['line'],[]).append(e['id'])
 v0,v1=('V0.06','V0.07') if 'enum-' in path else ('V0.07','V0.08')
 assert '文件版本：'+v0 in newlines[1]
 newlines[1]=newlines[1].replace('文件版本：'+v0,'文件版本：'+v1)
 allowed[path][2]=['M001-005']
 assert ''.join(newlines).encode()==after,'UNAPPROVED_CHANGE'
 assert len(newlines)==len(lines)
 changed[path]=[i for i,(x,y) in enumerate(zip(lines,newlines),1) if x!=y]
 assert set(changed[path])==set(allowed[path])
 text=after.decode('utf-8');assert '\ufffd' not in text
 p0=Parse();p0.feed(before.decode());p1=Parse();p1.feed(text)
 assert not p0.stack and not p1.stack
 assert p0.labels==p1.labels and p0.ids==p1.ids and p0.hrefs==p1.hrefs
 assert set(p1.hrefs)<=set(p1.ids) and len(p1.ids)==len(set(p1.ids))
 if 'enum-' in path:
  assert len(p1.labels)==197 and p1.tables==13 and p1.headers==['问题','答案']*13
  assert len(p0.headers)==0
 else:assert len(p1.hrefs)==45
 sources[path]={'before_sha256':hashlib.sha256(before).hexdigest(),'after_sha256':hashlib.sha256(after).hexdigest(),'bytes':len(after),'lines':len(newlines),'version':v1,'tables':p1.tables,'td.k':len(p1.labels),'headers':len(p1.headers),'hrefs':len(p1.hrefs)}
# Semantic checks: one approved complaint sentence is shared, preserving the existing tail.
enum=(r/paths[0]).read_text();mp=(r/paths[1]).read_text()
rule=edits[0]['new']
assert rule in mp.splitlines()[1383] and rule in enum.splitlines()[261]
assert edits[3]['new'] in mp.splitlines()[1377]
assert '其关闭按上述消费客人投诉规则执行。' in mp.splitlines()[1383]
assert '视为默认关闭' not in mp.splitlines()[1383]
assert '确认时限' not in mp.splitlines()[1383] and '未确认关闭' not in mp.splitlines()[1383]
assert '容量双阈值' in enum.splitlines()[144] and '容量控制' in mp.splitlines()[1293]
assert '只按单个项目' in enum.splitlines()[145] and '只按单个游乐项目' in mp.splitlines()[1294]
# Attribute every changed line in every hunk, including both sides.
hunks=[];path=None;oldline=newline=0;seen={p:set() for p in paths}
for line in diff.decode().splitlines():
 if line.startswith('diff --git '):path=line.split(' b/',1)[1]
 elif line.startswith('@@'):
  m=re.match(r'@@ -(\d+)(?:,\d+)? \+(\d+)(?:,\d+)? @@',line);oldline,newline=map(int,m.groups())
  hunks.append({'path':path,'hunk':line,'ids':set(),'lines':set()})
 elif line.startswith(('+++','---','index ')):continue
 elif line.startswith('-'):
  assert oldline in allowed[path];hunks[-1]['ids'].update(allowed[path][oldline]);hunks[-1]['lines'].add(oldline);seen[path].add(oldline);oldline+=1
 elif line.startswith('+'):
  assert newline in allowed[path];hunks[-1]['ids'].update(allowed[path][newline]);hunks[-1]['lines'].add(newline);seen[path].add(newline);newline+=1
 elif line.startswith(' '):oldline+=1;newline+=1
assert len(hunks)==18 and sum(len(v) for v in changed.values())==21
for p in paths:assert seen[p]==set(changed[p])
for h in hunks:h['ids']=sorted(h['ids']);h['lines']=sorted(h['lines'])
pg='docs/platform-governance-map-for-operations-v0.1.html'
assert git('show',bt+':'+pg)==git('show',at+':'+pg)==(r/pg).read_bytes()
lock=hashlib.sha256((a/'plan/criteria-lock.md').read_bytes()).hexdigest()
assert lock==(a/'plan/criteria-lock.sha256').read_text().strip()==re.search(r'^CRITERIA-LOCK-SHA256: (\w+)',(a/'ledger.md').read_text(),re.M).group(1)
assert git('rev-parse','HEAD').decode().strip()=='2416128dbacecd06b232f9f25ed8c5ec4ed4f3c2'
idx=Path(git('rev-parse','--git-path','index').decode().strip());idx=idx if idx.is_absolute() else r/idx
assert hashlib.sha256(idx.read_bytes()).hexdigest()=='b5c17f2c7f4f25c5fe9793a033e72aa5265ef5648caf416b4dd742cd2db0363b'
print(json.dumps({'verdict':'PASS','before_tree':bt,'after_tree':at,'diff_sha256':hashlib.sha256(diff).hexdigest(),'diff_bytes':len(diff),'diff_lines':len(diff.splitlines()),'hunks':hunks,'changed_lines':changed,'sources':sources,'independent_lock_sha256':lock,'checks':'exact authorization replay; complete diff every line attributed; strict UTF8; HTML balance; 197 labels unchanged; 13 paired column headers; links/ids unchanged; two-type complaint identical; capacity references resolve; governance/index/HEAD unchanged'},ensure_ascii=False))
```

## 实际检查输出

```json
{
  "verdict": "PASS",
  "before_tree": "fa2e80597255da1277291d5ba2e8146f9334bd53",
  "after_tree": "ff5573a74f5ed84f6bc84d73ad85af7a63a4d961",
  "diff_sha256": "041e46f19586f8ed63cdf0cfd89895b3afcd5e79e1586567a029d8bb217e908e",
  "diff_bytes": 30865,
  "diff_lines": 188,
  "hunks": [
    {
      "path": "docs/business-enum-quick-reference-v0.1.html",
      "hunk": "@@ -1,5 +1,5 @@",
      "ids": [
        "M001-005"
      ],
      "lines": [
        2
      ]
    },
    {
      "path": "docs/business-enum-quick-reference-v0.1.html",
      "hunk": "@@ -11,7 +11,7 @@",
      "ids": [
        "M001-008"
      ],
      "lines": [
        14
      ]
    },
    {
      "path": "docs/business-enum-quick-reference-v0.1.html",
      "hunk": "@@ -25,7 +25,7 @@",
      "ids": [
        "M001-008"
      ],
      "lines": [
        28
      ]
    },
    {
      "path": "docs/business-enum-quick-reference-v0.1.html",
      "hunk": "@@ -61,7 +61,7 @@",
      "ids": [
        "M001-008"
      ],
      "lines": [
        64
      ]
    },
    {
      "path": "docs/business-enum-quick-reference-v0.1.html",
      "hunk": "@@ -96,7 +96,7 @@",
      "ids": [
        "M001-008"
      ],
      "lines": [
        99
      ]
    },
    {
      "path": "docs/business-enum-quick-reference-v0.1.html",
      "hunk": "@@ -136,14 +136,14 @@",
      "ids": [
        "M001-006",
        "M001-008"
      ],
      "lines": [
        139,
        146
      ]
    },
    {
      "path": "docs/business-enum-quick-reference-v0.1.html",
      "hunk": "@@ -155,7 +155,7 @@",
      "ids": [
        "M001-008"
      ],
      "lines": [
        158
      ]
    },
    {
      "path": "docs/business-enum-quick-reference-v0.1.html",
      "hunk": "@@ -183,7 +183,7 @@",
      "ids": [
        "M001-008"
      ],
      "lines": [
        186
      ]
    },
    {
      "path": "docs/business-enum-quick-reference-v0.1.html",
      "hunk": "@@ -204,7 +204,7 @@",
      "ids": [
        "M001-008"
      ],
      "lines": [
        207
      ]
    },
    {
      "path": "docs/business-enum-quick-reference-v0.1.html",
      "hunk": "@@ -213,7 +213,7 @@",
      "ids": [
        "M001-008"
      ],
      "lines": [
        216
      ]
    },
    {
      "path": "docs/business-enum-quick-reference-v0.1.html",
      "hunk": "@@ -222,7 +222,7 @@",
      "ids": [
        "M001-008"
      ],
      "lines": [
        225
      ]
    },
    {
      "path": "docs/business-enum-quick-reference-v0.1.html",
      "hunk": "@@ -244,7 +244,7 @@",
      "ids": [
        "M001-008"
      ],
      "lines": [
        247
      ]
    },
    {
      "path": "docs/business-enum-quick-reference-v0.1.html",
      "hunk": "@@ -259,7 +259,7 @@",
      "ids": [
        "M001-005"
      ],
      "lines": [
        262
      ]
    },
    {
      "path": "docs/business-enum-quick-reference-v0.1.html",
      "hunk": "@@ -275,7 +275,7 @@",
      "ids": [
        "M001-008"
      ],
      "lines": [
        278
      ]
    },
    {
      "path": "docs/business-enum-quick-reference-v0.1.html",
      "hunk": "@@ -286,13 +286,13 @@",
      "ids": [
        "M001-008"
      ],
      "lines": [
        289,
        295
      ]
    },
    {
      "path": "docs/business-system-map-for-operations-v0.1.html",
      "hunk": "@@ -1,5 +1,5 @@",
      "ids": [
        "M001-005"
      ],
      "lines": [
        2
      ]
    },
    {
      "path": "docs/business-system-map-for-operations-v0.1.html",
      "hunk": "@@ -1292,7 +1292,7 @@",
      "ids": [
        "M001-006"
      ],
      "lines": [
        1295
      ]
    },
    {
      "path": "docs/business-system-map-for-operations-v0.1.html",
      "hunk": "@@ -1375,13 +1375,13 @@",
      "ids": [
        "M001-005"
      ],
      "lines": [
        1378,
        1384
      ]
    }
  ],
  "changed_lines": {
    "docs/business-enum-quick-reference-v0.1.html": [
      2,
      14,
      28,
      64,
      99,
      139,
      146,
      158,
      186,
      207,
      216,
      225,
      247,
      262,
      278,
      289,
      295
    ],
    "docs/business-system-map-for-operations-v0.1.html": [
      2,
      1295,
      1378,
      1384
    ]
  },
  "sources": {
    "docs/business-enum-quick-reference-v0.1.html": {
      "before_sha256": "1c3c25441655285812c3681d4f4baa5d0918e3182e5f7cc22299ef7ff34ad248",
      "after_sha256": "4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66",
      "bytes": 133961,
      "lines": 308,
      "version": "V0.07",
      "tables": 13,
      "td.k": 197,
      "headers": 26,
      "hrefs": 0
    },
    "docs/business-system-map-for-operations-v0.1.html": {
      "before_sha256": "7afff1dc5b6375557d995f5808d326fda9b4f0f6a4ea6adbd78fcb0f5a3b2ac3",
      "after_sha256": "3edff56a7640565d08983621e34d726d149261e70fd747ecb88b8e9cc27774ea",
      "bytes": 303828,
      "lines": 2032,
      "version": "V0.08",
      "tables": 13,
      "td.k": 0,
      "headers": 0,
      "hrefs": 45
    }
  },
  "independent_lock_sha256": "d4ba138e924c6fb13020cf26e55aef537a9488ba84dfb72ca9d0f851b5090b80",
  "checks": "exact authorization replay; complete diff every line attributed; strict UTF8; HTML balance; 197 labels unchanged; 13 paired column headers; links/ids unchanged; two-type complaint identical; capacity references resolve; governance/index/HEAD unchanged"
}
```

## 完整原始binary diff

命令：git diff --binary fa2e80597255da1277291d5ba2e8146f9334bd53 ff5573a74f5ed84f6bc84d73ad85af7a63a4d961
SHA256=041e46f19586f8ed63cdf0cfd89895b3afcd5e79e1586567a029d8bb217e908e；bytes=30865；lines=188。

```diff
diff --git a/docs/business-enum-quick-reference-v0.1.html b/docs/business-enum-quick-reference-v0.1.html
index 9fd83ea0..86a71793 100644
--- a/docs/business-enum-quick-reference-v0.1.html
+++ b/docs/business-enum-quick-reference-v0.1.html
@@ -1,5 +1,5 @@
 <!DOCTYPE html>
-<!-- 文件版本：V0.06；更新日期：2026-09-01 -->
+<!-- 文件版本：V0.07；更新日期：2026-09-01 -->
 <html lang="zh-CN">
 <head>
 <meta charset="utf-8">
@@ -11,7 +11,7 @@
   .sub { color:#6b7280; margin: 0 0 20px; font-size: 14px; }
   h2 { font-size: 18px; margin: 28px 0 10px; padding: 6px 12px; background:#1b1d21; color:#fff; border-radius: 8px; }
   table { width: 100%; border-collapse: collapse; background:#fff; box-shadow: 0 1px 2px rgba(0,0,0,.05); border-radius: 8px; overflow: hidden; margin-bottom: 6px; }
-  td { border-bottom: 1px solid #eef0f3; padding: 9px 12px; vertical-align: top; font-size: 14px; }
+  td, th { border-bottom: 1px solid #eef0f3; padding: 9px 12px; vertical-align: top; font-size: 14px; } th { text-align: left; }
   tr:last-child td { border-bottom: none; }
   td.k { width: 200px; font-weight: 600; background:#f6f7f9; color:#0f172a; }
   .note { color:#9ca3af; font-size: 12px; }
@@ -25,7 +25,7 @@
   <div class="tip">用法：左列是“问题”（如“账单有哪几个状态”），右列是“答案”。配套两份业务地图（业务系统地图 / 平台治理地图）使用。</div>
 
   <h2>一、票务与权益</h2>
-  <table>
+  <table><thead><tr><th scope="col">问题</th><th scope="col">答案</th></tr></thead>
     <tr><td class="k">票种</td><td>平日票、周末票、节假日票（基础价）；成人票、学生票；多人票（双人 / 团队）；新人票；会员价、渠道价、动态价；一人多次票、通票。派对票与活动票等价。单日票、夜场票。赠票作为票务产品类型。</td></tr>
     <tr><td class="k">票务策略状态</td><td>票务策略状态为：草稿（规则未发布）、待启用（规则已配置但未到生效时间）、售卖中（当前可售卖）、暂停售卖（临时停止售卖）、已结束（售卖周期结束）、归档（历史策略）。</td></tr>
     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-02）：单店酒吧门票收款主体统一——酒吧商户定价、销售并确认已核销部分收入，票款由娱乐平台公司统一收款后结付，与 BSM 一致 -->
@@ -61,7 +61,7 @@
   </table>
 
   <h2>二、会员与积分</h2>
-  <table>
+  <table><thead><tr><th scope="col">问题</th><th scope="col">答案</th></tr></thead>
     <tr><td class="k">会员唯一识别</td><td>以完成短信验证的手机号作为判断新会员的唯一标准。同一已验证手机号在全国平台内只建立一个会员账户，并跨城市、跨项目复用会员身份、成长值和等级；不同手机号不得仅凭姓名、生日等弱信息自动合并，系统也不得声称已经识别为同一自然人。</td></tr>
     <tr><td class="k">会员注册</td><td>会员主动提交注册，完成手机号短信验证、填写生日、满足平台年龄要求，并同意当前会员条款和隐私条款后，系统自动注册成功并直接创建有效会员身份。任一条件不满足时注册失败且不创建会员身份，不产生“注册中”或“未通过”状态；微信身份不能代替手机号标准。</td></tr>
     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-11）：注销后重新注册不穿透历史、可再享新人资格；同一手机号一年内注销+注册合计超阈值触发异常预警（异常预警页面见 BSM 留痕与异常章） -->
@@ -96,7 +96,7 @@
   </table>
 
   <h2>三、酒吧经营</h2>
-  <table>
+  <table><thead><tr><th scope="col">问题</th><th scope="col">答案</th></tr></thead>
     <tr><td class="k">桌台/卡座/吧台位状态</td><td>空闲、已预留、已开台、待清台、停用、异常。客人自助订台支付定金成功、或员工代订提交成功时，该台位即进入已预留。一个台位同一时刻只能存在一笔有效预留（排他锁，不是展示标记）；台位未被占用——既无有效预留也未开台——时即可再次接受预订，同一营业日内前一波客人接待完毕、台位恢复空闲后可以继续预订。冲突时先支付定金者得；员工代订不得覆盖已付定金的自助订单；两笔员工代订相撞先提交者得。超时自动释放后台位回到空闲，既可现场先到先接待，也可再次接受预订，不设补位队列。已预留只能经取消订台、超时释放、到店开台三条路径退出；店长及以上可人工取消预留，必须填原因并留痕。台位停用与恢复由店长及以上操作，填原因留痕；异常由系统告警或人工登记进入，处置完成后由当班主管确认恢复，全程留痕。关台指账单全部结清、或该台账单已按坏账流程转入坏账损耗处理中或已核销结案后，结束本次开台，由当班服务员或收银执行；除上述情形外，账单未结清（含挂起未结清）不得关台。关台成功后台位自动进入待清台；由负责该台位的服务员完成清理后在系统登记恢复空闲，散台与卡座同一动作。并台期间全部原台位保持已开台并归属该合并台组；拆台后各结果台位保持已开台、各自独立计账。待清台超过后台配置时限（业务参数，按酒吧可配，出厂默认 15 分钟，须同时配置上下限）未恢复空闲的，系统提醒当班主管。</td></tr>
     <tr><td class="k">账单状态</td><td>待结、部分支付、已结、部分退款、已退款、异常。部分支付：账单已有成功收款但未收足应结金额的状态；由分次收款产生，收足即转已结。同一张账单允许多笔、多种支付方式分笔收款，每笔独立记录支付方式、金额、时间与收款人。</td></tr>
     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-05 / DEC-R6-15）：定金不抵扣消费，达低消后原路退回，状态「到店已抵扣」→「达标已原路退回」；同行错别字「爬约」→「爽约」 -->
@@ -136,14 +136,14 @@
   </table>
 
   <h2>四、游乐运营</h2>
-  <table>
+  <table><thead><tr><th scope="col">问题</th><th scope="col">答案</th></tr></thead>
     <tr><td class="k">项目开放状态</td><td>未开放、准备中、开放中、暂停、检修中、已关闭、异常</td></tr>
     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-18）：「已满员」加括注——由项目实时在玩人数自动推算显示，不可人工设置 -->
     <tr><td class="k">场次状态</td><td>未开始、入场中、已满员（由该项目实时在玩人数自动推算显示，不可人工设置）、进行中、已结束、取消、异常</td></tr>
     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-10）：新增「游乐当天／每天」口径条目——一律按营业日（10:00→次日 10:00）计算，时区取项目所在地时间、禁用服务器默认时区 -->
     <tr><td class="k">游乐「当天／每天」口径</td><td>游乐规则中的「当天」「每天」（含一人多次票当天多次入场、当天不限次数一类表述）一律按营业日计算，不按自然日：当日 10:00 至次日 10:00，起止时点沿用营业日配置（安全阈值，按每个项目、每家酒吧可配，调整须最高权限审批并留痕，须同时配置上下限）；时区一律取项目所在地时间，禁止使用服务器默认时区。</td></tr>
     <tr><td class="k">容量双阈值</td><td>只按单个项目的实时在玩人数控制容量，不再按场次/时段或点位/设备另设容量；排队人数不占用项目容量，排队本身不设人数上限。运营容量（软上限，告警放行）、安全容量（硬上限，阻断+自动停运，不可绕过）；软上限处置方式后台可配：记录异常事件（出厂默认）/仅提示；不影响硬上限阻断。</td></tr>
-    <tr><td class="k">园区容量</td><td>园区容量。</td></tr>
+    <tr><td class="k">园区容量</td><td>只按单个项目的实时在玩人数控制容量，具体按本表「容量双阈值」条执行。</td></tr>
     <tr><td class="k">设备关键/辅助分级</td><td>一个游乐项目可以关联多台设备，并区分「关键安全设备」与「辅助设备」。某台设备巡检超期、巡检不合格或发生故障时，按其类别处置：辅助设备只停用该台设备本身，项目是否可以在其余条件下继续运行，由工程岗位或安全岗位判断，不得由当班运营人员自己判断；判断结论必须留痕，记下是谁判的、什么时间判的、依据了哪几项条件，没有留痕的一律按停止运行处理；关键安全设备则要看它与其余关键安全设备之间是否存在依赖关系——不存在依赖关系、且其余关键安全设备均无故障的，只停该台故障设备，项目可继续运行；存在依赖关系的，整个游乐项目停止新增核销并停止运行。是否互相依赖按客人走完这个游乐项目的实际流程来判：这台设备是客人必经的一个环节、少了它这个流程就走不下去的，属于依赖；这台设备有同类的其他设备可以顶替（多台各自独立、可以互相替代），或者这个环节可以绕过去、客人仍能走完整个项目的，属于不依赖。每台关键安全设备属于哪一种，须在设备台账中登记，作为故障时判定停机范围的依据；登记内容变更须经工程或安全审批。项目上线后要把某台关键安全设备改为辅助设备，必须重新经过工程或安全审批，并且只能在该项目停运后生效；普通系统管理员或普通运营人员不得直接降级。</td></tr>
     <tr><td class="k">全园故障与补偿通票</td><td>单台设备或单个游乐项目故障，不直接触发补偿通票。只有整个游乐园发生故障并影响整体履约时，才可以向受影响客人发放补偿通票；补偿对象包括故障发生时已经完成入场核销的客人，也包括尚未核销但对应门票或权益仍在有效期内的持票客人。补偿通票自发放之日起在后台配置的补偿有效期内有效（业务参数，按项目可配，出厂默认一年，须同时配置上下限），且只是把原票的核销权延长到该有效期内使用，不改变原票的核销次数——原票是单次核销的，有效期内任何时间可核销一次，核销后即用尽；家庭票、多人票按原票的人数与次数规则执行；多次票则是把其中某一次权益延长到该有效期内，这一次同样只能核销一次。补偿通票必须关联原票、游乐园整体故障事件和发放原因，防止重复补偿；批量发放前须由游乐公司负责人和工程或安全岗位共同确认。全园故障状态一经生效，必须立即停止所有纯游乐门票和所有包含游乐权益的通票的新销售；局部设备或单个项目故障，才允许在客人付款前明确提示后继续销售。</td></tr>
     <tr><td class="k">安全严重度</td><td>提醒、警告、阻断、重大</td></tr>
@@ -155,7 +155,7 @@
   </table>
 
   <h2>五、供应链与库存</h2>
-  <table>
+  <table><thead><tr><th scope="col">问题</th><th scope="col">答案</th></tr></thead>
     <tr><td class="k">供应合同模式</td><td>平台自用、商户自采、平台统一采购后分销给商户</td></tr>
     <tr><td class="k">库房分类</td><td>总库、酒吧库、吧台库存、消耗品库、礼品库、存酒区</td></tr>
     <tr><td class="k">物品大类</td><td>酒类、消耗品、礼品、其他物料</td></tr>
@@ -183,7 +183,7 @@
   </table>
 
   <h2>六、财务结算与对账</h2>
-  <table>
+  <table><thead><tr><th scope="col">问题</th><th scope="col">答案</th></tr></thead>
     <tr><td class="k">运营营业日</td><td>全系统的钱账统一按「营业日」归集。营业日从当日上午 10:00 起算，至次日上午 10:00 止；其中次日凌晨 07:00 至上午 10:00 为非营业时段（已闭店），该时段内发生的记录仍归属正在进行的这一个营业日。起止时点由后台配置（安全阈值，按每个项目、每家酒吧可配，调整须最高权限审批并留痕，出厂默认起点为上午 10:00、闭店为次日 07:00，须同时配置上下限）。日结批次、最低消费时段、票价的日期类型（平日/周末/节假日）、场次归属、巡检有效期、考勤日期（跨月、跨周、跨年班次的归属除外，一律按班次结束时点判定，见业务系统地图「请假扣薪」条）、销售提成月份，一律以开台或下单时点所属的营业日为准，不按自然日切分。第三方渠道按自然日提供的汇总数据，由系统按营业日重新归集后再核对，归集差额单独列示。营业日配置的变更只影响未来，不回改历史。</td></tr>
     <tr><td class="k">对账口径</td><td>按订单、按核销、按合同、按渠道、按日结</td></tr>
     <tr><td class="k">对账差异四类</td><td>金额差异、数量差异、状态差异、缺失差异（单边缺失）</td></tr>
@@ -204,7 +204,7 @@
   </table>
 
   <h2>七、第三方接入</h2>
-  <table>
+  <table><thead><tr><th scope="col">问题</th><th scope="col">答案</th></tr></thead>
     <tr><td class="k">接入的外部系统</td><td>团购渠道（美团/大众点评/抖音）、企业微信、餐饮 POS、零售 POS、聚合支付、配送机器人、视频监控、安检环节设备（X 光）、年龄核验环节设备（人脸扫描）、短信平台、微信/支付宝登录、税务/电子发票平台（开票，2026-06-26 裁决：开票走第三方对接、不自研开票引擎）</td></tr>
     <tr><td class="k">客户标签</td><td>需求、投诉、预订、存酒。自动打标签。</td></tr>
     <tr><td class="k">统一原则</td><td>预留接口 + 后台开关默认关 + 不阻塞自研 + 不伪造对账结果</td></tr>
@@ -213,7 +213,7 @@
   </table>
 
   <h2>八、留痕与风控</h2>
-  <table>
+  <table><thead><tr><th scope="col">问题</th><th scope="col">答案</th></tr></thead>
     <tr><td class="k">风险四级</td><td>低（进列表）、中（待复核）、高（生成工单+通知主管）、严重（可冻结/停用/强制审批）</td></tr>
     <tr><td class="k">事件状态</td><td>已记录、待补充、待复核、已确认、已驳回、已关闭、已归档（只能作废/冲正，不能删除；仅最高权限本人插入授权 U 盘密钥后可以清理，清理动作另行留痕）</td></tr>
     <tr><td class="k">主管复核强制项</td><td>免单、退款、调账、高价值报损/盘点差异/存酒作废、敏感视频或企微原文查看、人工补录</td></tr>
@@ -222,7 +222,7 @@
   </table>
 
   <h2>九、平台治理</h2>
-  <table>
+  <table><thead><tr><th scope="col">问题</th><th scope="col">答案</th></tr></thead>
     <tr><td class="k">四类主体</td><td>经营对象、运营主体、合同主体、结算主体（可分属不同公司，同一经营对象下唯一绑定）</td></tr>
     <tr><td class="k">经营主体关系</td><td>物理项目与运营公司是多对多关系，不是固定的“平台→项目公司→域公司→商户”行政隶属链。单一经营域的业务规则、定价和内部结算由对应域主管公司负责；跨域规则由项目之上的平台权限审批。</td></tr>
     <tr><td class="k">城市项目公司</td><td>每个城市项目公司同时承担本城市项目的资产职责，负责项目资产、场地、物业、租赁、直接管理的第三方商户及自身人员和设备。历史合同、财务底稿与既往文件中出现的“资产公司”“资产管理公司”，指的就是本主体，同一主体两个叫法；资产业态不另设独立主体，由城市项目公司兼任。它不管理酒吧、游乐、娱乐平台公司、传媒公司、中央餐饮公司及其人员、设备、定价、票务或结算。</td></tr>
@@ -244,7 +244,7 @@
 
   <!-- 2026-06-24 地产全链授权扩展：业务系统从「娱乐综合体」口径扩展为「地产+娱乐综合体」，新增招商/租赁全周期/物业服务/楼宇设备/能耗/楼宇巡检的业务枚举。依据：用户 2026-06-24 拍板「扩展 HTML 到地产全链」+ gap-analysis §7B（租金押金账单/铺位平面图租赁状态/物业工单/合同审批流已升正式条目）+ 原始需求 §15 平台治理范围。具体取值最终以后台配置为准。 -->
   <h2>十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）</h2>
-  <table>
+  <table><thead><tr><th scope="col">问题</th><th scope="col">答案</th></tr></thead>
     <tr><td class="k">招商线索状态</td><td>待跟进、跟进中、已转意向、洽谈中、已签约、已流失、已搁置</td></tr>
     <tr><td class="k">招商意向阶段</td><td>初步接触、需求确认、方案报价、商务谈判、待签约</td></tr>
     <tr><td class="k">品牌/客户库类型</td><td>意向品牌、储备品牌、合作品牌、黑名单品牌</td></tr>
@@ -259,7 +259,7 @@
     <tr><td class="k">铺位租赁状态</td><td>空置、洽谈中、已租、即将到期、已退租、不可租</td></tr>
     <tr><td class="k">物业工单类型</td><td>报修、保洁、投诉、整改、巡检、内部协作工单（内部部门间发起与流转）、其他服务请求</td></tr>
     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-13）：物业工单状态加注——回访／验收按工单类型分支适用（保洁类完成即结／投诉类提单方确认／复杂类主管验收+提单方确认双闸），非全类顺序链 -->
-    <tr><td class="k">物业工单状态</td><td>待派单、已派单、处理中、待回访、待验收、已验收、已关闭、已挂起、已超时。回访／验收按工单类型分支适用，不是全类都走同一条顺序链：保洁等无争议类由处理人完成即结；投诉类须提单方确认才能关闭；复杂类（大型设备报修等）须主管验收＋提单方确认双闸。</td></tr>
+    <tr><td class="k">物业工单状态</td><td>待派单、已派单、处理中、待回访、待验收、已验收、已关闭、已挂起、已超时。回访／验收按工单类型分支适用，不是全类都走同一条顺序链：保洁等无争议类由处理人完成即结；投诉分为商户投诉和消费客人投诉：商户投诉须由商户确认投诉已解决；按既有规则无法解决，或问题已解决但商户未确认解决或未点击关闭的，客户关怀部记录相关信息及关闭缘由后，可以关闭投诉。消费客人的投诉，由客户关怀部记录问题解决方案及客人反馈后，可以关闭；复杂类（大型设备报修等）须主管验收＋提单方确认双闸。</td></tr>
     <!-- 2026-07-03 回写：商户服务工单(商户专区发起)独立状态链。 -->
     <tr><td class="k">商户服务工单状态</td><td>已提交、已派单、处理中、待商户确认、已处理待关闭、已关闭、已取消（商户专区发起；超过时限自动升级并标记）</td></tr>
     <tr><td class="k">工单时效</td><td>响应时限、处理时限；超时自动升级通知主管。看板上设施工单与商户服务工单分行对照；商户单没有「首次响应」环节，不显示响应指标</td></tr>
@@ -275,7 +275,7 @@
 
   <!-- 2026-07-03 回写：员工排班考勤薪资/服务点单 新枚举(已上线口径)。 -->
   <h2>十一、员工排班考勤与薪资</h2>
-  <table>
+  <table><thead><tr><th scope="col">问题</th><th scope="col">答案</th></tr></thead>
     <tr><td class="k">考勤日结果</td><td>出勤、缺勤（有排班没打上班卡）、迟到、早退、请假；无排班=休息（同日多个班次并列显示）</td></tr>
     <tr><td class="k">打卡方式</td><td>上班打卡以领取工作手机为开始：工作手机 NFC 碰卡、扫码（工作手机不支持 NFC 时兜底）、工作手机离开充电设施，三者任一即可，取最早发生的一次；严格口径：晚于排班开始即迟到、早于排班结束即早退，无宽限，以所在门店／点位当地时间为准</td></tr>
     <tr><td class="k">请假类型</td><td>事假、病假、年假、调休（主管审批；批准区间考勤自动记请假）</td></tr>
@@ -286,13 +286,13 @@
   </table>
 
   <h2>十二、服务点单</h2>
-  <table>
+  <table><thead><tr><th scope="col">问题</th><th scope="col">答案</th></tr></thead>
     <tr><td class="k">服务订单状态</td><td>已下单、已收款（客人扫公司统一收款码付款，支付渠道回传到账信息后系统自动确认收款到公司账户，不需要点单员人工确认）、已完成（完成时自动分账；没有收到渠道回传的到账信息不能点完成）、已取消（已收款后取消的，由点单员点击取消，系统自动按原收款路径退款并自动留痕；服务完成后不可退）</td></tr>
     <tr><td class="k">管理费方式</td><td>不收、按比例、固定金额（按服务目录项配置；服务费全额入公司账（此处「公司」指该服务发生地所属酒吧的经营主体），管理费归公司收入，其余份额按薪资/提成名录进服务人员月结薪资）。按比例时配置值必须在 0% 至 100%（含边界）之间；固定金额必须在 0 至服务售价（含边界）之间；任一越界值在保存时直接拒绝，不得静默截断、封顶或改写。服务点单管理费的配置主体与审批、出厂默认值、变更版本化与指定生效日、单据下单时锁定费率版本、变更前向受影响方通告、不收管理费的排除项、退款时的冲回与反向结算，一律比照销售提成的对应规则执行；上下限约束按本条前述区间。服务点单份额与管理费的归属月份按服务完成时点所在营业日确定。<!-- param-tier:ignore 0%;100% --></td></tr>
   </table>
 
   <h2>十三、持股分红</h2>
-  <table>
+  <table><thead><tr><th scope="col">问题</th><th scope="col">答案</th></tr></thead>
     <tr><td class="k">分红基数</td><td>净利润制：一期经营收入 − 该期进货成本 − 该期归集的各项费用（能耗/物业/租金等）；亏损期不倒扣主理人</td></tr>
     <tr><td class="k">分红比例</td><td>参股方、主理人和其他股东按每家采用参股经营的经营主体工商登记的真实持股比例分配；酒吧的参股方为娱乐平台公司，引进参股的游乐项目的参股方为引进该项目的域主管公司；80% / 20%（业务参数，按经营主体可配，须同时配置上下限）仅为新酒吧默认建议值，必须以获批登记版本生效，修改不追溯已确认或已发放分红</td></tr>
     <tr><td class="k">分红税务</td><td>按发放时有效的中国税法、股东身份和交易实质处理，系统记录适用规则、代扣代缴、凭证和版本，税率以参数形式配置，不在业务页面硬编码。股东类型：个人（按「股息红利」代扣个人所得税，安全阈值，全平台统一可配，调整须经财务、税务、法务三审并由顶层平台公司最终确认，出厂默认 20%，须同时配置上下限）／公司（不代扣个人所得税）。</td></tr>
diff --git a/docs/business-system-map-for-operations-v0.1.html b/docs/business-system-map-for-operations-v0.1.html
index 49ba0ad6..24839e81 100644
--- a/docs/business-system-map-for-operations-v0.1.html
+++ b/docs/business-system-map-for-operations-v0.1.html
@@ -1,5 +1,5 @@
 <!doctype html>
-<!-- 文件版本：V0.07；更新日期：2026-09-01 -->
+<!-- 文件版本：V0.08；更新日期：2026-09-01 -->
 <html lang="zh-CN">
 <head>
   <meta charset="utf-8">
@@ -1292,7 +1292,7 @@
                   <tr><th scope="row">场次状态</th><td>未开始、入场中、已满员、进行中、已结束、取消、异常。</td></tr>
                   <!-- 2026-08-04 第二轮裁决：容量控制由「三维度取最紧余量」改为与本章主要功能行一致的「只按单个项目的实时在玩人数控制」，排队人数不占容量；双阈值口径不变；用户逐项批准。 -->
                   <tr><th scope="row">容量控制</th><td>只按单个游乐项目的实时在玩人数控制容量，不再按场次/时段或点位/设备另设容量；排队人数不占用容量。双阈值：运营容量（软上限）告警放行、安全容量（硬上限）任何情况都阻断并自动停运。软上限处置方式后台可配：记录异常事件（出厂默认）/仅提示；不影响硬上限阻断（DEC-R7-02）。</td></tr>
-                  <tr><th scope="row">园区容量</th><td>园区容量。</td></tr>
+                  <tr><th scope="row">园区容量</th><td>只按单个游乐项目的实时在玩人数控制容量，具体按本表「容量控制」条执行。</td></tr>
                   <tr><th scope="row">使用限制</th><td>身高、年龄、健康、安全告知、人数等，须现场核验留痕。</td></tr>
                   <tr><th scope="row">安全严重度</th><td>提醒、警告、阻断、重大；当日无合格巡检或有未复检阻断项则拒绝核销。</td></tr>
                   <!-- 2026-08-04 第三轮用户裁决（DEC-R6-09）扩展落点，R6-B 收口：按 UD-14「全域允许补录」同步放开巡检点位补录——删「也不存在事后补录」，改为附证据+主管复核后方生效 -->
@@ -1375,13 +1375,13 @@
               <ul>
                 <li>创建工单并分类：报修、保洁、投诉、整改、巡检和其他服务请求。</li>
                 <!-- 2026-08-04 第三轮用户裁决（DEC-R6-14）：物业工单来源补「客人」——客人端投诉，客人即提单方、关闭须其确认 -->
-                <li>工单来源包括：商户（商户侧自助提单）、客人（客人端投诉，客人即提单方，关闭须其确认）、现场工作人员，以及楼宇设备告警和巡检异常自动生成。</li>
+                <li>工单来源包括：商户（商户侧自助提单）、客人（消费客人通过客人端投诉，客人即提单方；客户关怀部记录问题解决方案及客人反馈后可以关闭）、现场工作人员，以及楼宇设备告警和巡检异常自动生成。</li>
                 <li>派单到具体人员或班组，记录处理过程、用料和结果。用料必须从指定库房出库并按当前平均进货价计成本，同时在工单上标明费用承担方：公区设施与楼宇主体设备的用料由物业承担；商户自有设施、以及因商户责任导致损坏的用料由该商户承担，并自动生成一笔应收进入其费用账单。承担方有争议的，工单挂起转上级判定，判定结果与依据留痕后才能结案。</li>
                 <li>处理后回访客人或商户，验收合格才关闭。</li>
                 <li>按响应时限和处理时限统计时效，超时自动升级通知主管。</li>
                 <li>记录满意度评价，作为服务质量和商户表现的依据。</li>
                 <!-- 2026-06-25 修改阶段：补物业工单运营细则，依据 M1 需求采集（complex-m1-requirements §4）。 -->
-                <li>运营细则（已确认）：响应/处理时限按「工单类型×优先级」矩阵后台配置，超时自动升级通知主管；验收按工单类型分别配置——保洁等无争议类由处理人完成即结束；投诉类一律须提单方确认才能关闭，提单方在后台配置的时限内未确认的视为默认关闭（业务参数，按工单类型可配，须同时配置上下限），但单独统计为“未确认关闭”；复杂类（大型设备报修）需主管/物业验收，并且一律须提单方确认才能关闭；满意度在回访/验收时由提单方评分；商户侧可自助提报修、保洁、投诉和其他服务请求，整改与巡检类工单由平台发起。客人投诉由客人自行在客人端提交，客人即为提单方，其关闭同样须提单方确认。工单由客户关怀部负责处理；响应时限与处理时限对商户公开。所有客诉必须有跟进、有记录、有结果，直至形成闭环；客诉须经客户关怀部确认后方可视为形成闭环，未经其确认的一律不算闭环。系统每周自动生成客诉周报，列明当周客诉的笔数、类型、处理进度与闭环情况——店内的报送该店主理人与店长，各公司的报送该公司最高负责人与平台最高负责人。一个月内，同一类投诉由三个不同的人分别提出的，立即上升到项目负责人、娱乐平台负责人与公司最高负责人；是否属于同一类由客户关怀部认定。</li>
+                <li>运营细则（已确认）：响应/处理时限按「工单类型×优先级」矩阵后台配置，超时自动升级通知主管；验收按工单类型分别配置——保洁等无争议类由处理人完成即结束；投诉分为商户投诉和消费客人投诉：商户投诉须由商户确认投诉已解决；按既有规则无法解决，或问题已解决但商户未确认解决或未点击关闭的，客户关怀部记录相关信息及关闭缘由后，可以关闭投诉。消费客人的投诉，由客户关怀部记录问题解决方案及客人反馈后，可以关闭；复杂类（大型设备报修）需主管/物业验收，并且一律须提单方确认才能关闭；满意度在回访/验收时由提单方评分；商户侧可自助提报修、保洁、投诉和其他服务请求，整改与巡检类工单由平台发起。客人投诉由客人自行在客人端提交，客人即为提单方，其关闭按上述消费客人投诉规则执行。工单由客户关怀部负责处理；响应时限与处理时限对商户公开。所有客诉必须有跟进、有记录、有结果，直至形成闭环；客诉须经客户关怀部确认后方可视为形成闭环，未经其确认的一律不算闭环。系统每周自动生成客诉周报，列明当周客诉的笔数、类型、处理进度与闭环情况——店内的报送该店主理人与店长，各公司的报送该公司最高负责人与平台最高负责人。一个月内，同一类投诉由三个不同的人分别提出的，立即上升到项目负责人、娱乐平台负责人与公司最高负责人；是否属于同一类由客户关怀部认定。</li>
               </ul>
             </div>
             <div class="info-block">
```

B4_VERDICT: PASS
ATTRIBUTION_VERDICT: PASS
本报告完整落盘后方可读取Grok本次报告；模块仍为IN_PROGRESS，B3=2。
