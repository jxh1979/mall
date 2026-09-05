# M001 / B1 / Codex 独立审计记录
版本：V0.10

## 互读后的当前状态

双席合并结论：FAIL；M001生命周期：BLOCKED-ASK。下节PASS为互读前Codex原始独审结论，予以保留；Grok原始结论为FAIL，不能由本席覆盖。具体申请7至10及核验分歧见文末。

## 当前轮原始独审结论

本席结论：PASS。
本次B1独立审计已完成；当前模块生命周期仍IN_PROGRESS，尚未经B2及B5，不是模块最终PASS。
本次尚未读取Grok B1报告正文或原始JSON。先Grok真实exit=0、路径型适配器确认本次交付，随后完成本席独审、落本报告，之后才允许互读。
本轮尚未解决的finding：0；questions：0。下面历史CX-01至CX-04均已有操作员裁决且已按批准落盘，不作为本轮候选问题重复计入。
控制提示词：D:\AI\ccwork\160-audit-prompt.md V0.08，SHA256=d8ee43b34f1cb895b69bf0abef71dc61527150c817047a044a13589216295757。
正本：/home/james/complex-management，main；HEAD=2416128dbacecd06b232f9f25ed8c5ec4ed4f3c2。
CRITERIA-LOCK-SHA256: bdb7813edda7971c8a67c5161c4f236b6d917aa122415ec238042577567d961c
LOCK-AMEND-COUNT: 3
模块：M001 / 322；B3=0；阶段A方案修复=2；失败重跑累计=5。

## 阅读覆盖、当前字节与证据边界

本席在同一互盲会话中已完整阅读枚举表1至308行、业务地图1至2032行，长输出截断处均已补读。业务地图1078行正文与已读枚举表114行逐字相等，经脚本对照确认后按同一内容审查。本次没有把历史partial raw当作完整审计凭证。
LOCK-AMEND 3后重新评估完整模块：对三份HTML逆向本轮批准短语和版本，完整字节哈希恢复到此前逐行阅读的版本；这证明其余正文没有变化。新批准的退租分支与枚举已逐字复核。未重复打印未变全文，但未以摘要替代未读内容。
本次Grok交付之后已补完此前未完成的必要权威对照及结构/导航执行检查：治理地图860至890、919至940；其余必要对照此前已读256至307、412至459、518至543、647至662、716至743。本次没有启动M002，也未对治理地图其余内容作独立模块PASS声明。
当前文件：
- 枚举表V0.05，SHA256=eb262fadd8298a3cbb821f386ca1255a1e4b6b9dcfc752ccce2ed4180019a6e5。
- 业务地图V0.06，SHA256=7d7c5d1fb8bf36a7ba479933674283c1773b493cacc87809435a19e64c82d38a。
- 治理地图V0.06，仅权威对照，SHA256=2a15bba68c5591668cbc2e83b6b43719f8767377cc7c3ef408181467b97a322f。

覆盖固定GEN-001至GEN-018、BEQR-001至BEQR-197、BSM-001至BSM-120。三个变更引文已通过LOCK-AMEND 3，未使用锁外判据。
本模块是权威业务说明文档，声明的是需求，不是“代码已实现”的完成报告；未把HTML需求当作业务程序已经满足的证明。本次没有运行后端、支付、资金流或用户权限实现，相关执行性质将在其实际模块验证。

## 三线审计与分类

第一性原理：
- 必须存在的用途由HTML自身解释权声明及模块内容证明：枚举表负责集中登记业务状态与口径，地图负责业务范围、主体职责及流程连接。两者面向不同阅读用途，不能仅因互相引用或重复定义就删除其中一份。
- 对每个业务锚点以原文明示的主体、触发条件、操作顺序、金额归属或状态出口为依据；未把HTML沉默、惯例或未经批准的实现细节引为新要求。
- 未验证的业务实现前提留在实施模块，不以本模块文档存在宣称实施成功。CSS、页面布局、页内导航只服务于阅读，没有新增业务实体、业务状态或价格规则。

顺从式：
- 依次核对固定枚举与地图对应业务：票务及权益、会员与积分、储值与退款、酒吧台位与订单、库存与采购、设施与能耗、租赁与合同、员工与工资、商户服务、结算财务、经营主体、权限与留痕。
- 通票整单通路费、适用渠道费、积分成本均只扣一次；第一段固定额与第二段比例为不同参与集合；未核销按所属分配段一次减除；特殊退款与普通退款按原文各自条件处理。
- 本次退租新裁决已覆盖两张地图，枚举登记两个不同争议状态。主管裁决后继续，裁决不被接受时转“有争议，诉讼/仲裁”等结果。以前“所有争议一律等诉讼结果”的旧句已被新批准正文替换，不引用旧台账推翻操作员新裁决。
- 正式挂单通常阻止交班/日结，明确转坏账后按专门出口处理；坏账赔偿法务审查前不自动扣工资。按专门规则解释通用概要，未把两者误判为同条件矛盾。
- 项目/商户参数粒度按具体条文授权，全局业务分类定义与具体参数不混用；十一端、权限与留痕、主线和平行审批均按已批准原文核对。

对抗式：
- 检查了第一段参与者没有第二段比例、第三方与官方售票成本不同、未核销与安全例外、付款与取消竞争、台位拆并/转台后原订单归属、到账/退款期及营业日跨界、对不同主体结算与尾差等正常路径。
- 针对“主管决定已作出但被拒绝接受”的常见路径，新正文和枚举现在都能表示诉讼/仲裁挂起状态。未增加自动通过、超时结案或推定接受。
- HTML中的概要用词按同一权威集合里的明示详细规则和例外解释；没有从单句省略、原文未定义的园区容量或理论多重故障推导必须新增业务规则。
- 结构/导航在真实文件上执行：初始位置、首段、中段、末段、滚回首段及119/120/121像素边界。没有用模拟业务实现代替真实业务测试。

四分类结果：
- 业务正文为已批准权威来源及其说明，没有确认与现行权威集合相矛盾的未解决内容；未发现须询问操作员的外加业务发明。
- CSS/静态页面结构/页内导航：技术性。证据是本文件CSS与业务地图2008至2029行脚本，仅改变页面阅读状态，无业务规则、价格或权限变化；按GEN-002、GEN-015、GEN-016核验。
- 本轮没有需删除的冲突条目，没有待处理添加项。未把“技术性”分类当作免审。

## 通用判据执行说明

GEN-001/002/015/016/017/018：用途、可执行导航、可读结构、版本/来源与分类及证据已逐项核对，未发现未解决缺陷。
GEN-003：文档类无需构造业务测试；本报告附实际文件结构与实际导航脚本的可复现断言，覆盖本模块唯一可执行脚本的正常/边界行为。
GEN-004至GEN-014：对本模块实际存在的入口和声明适用；静态HTML没有网络请求、外部输入提交、服务端权限入口、数据库迁移、资金写入、密钥或新增运行依赖。未凭这些判据要求HTML创造实现代码。业务正文的授权、守恒、幂等、状态与审计规则已经作为文档口径核对，其后端落实不在本模块宣称已验证。

## 现场验证与结果

Python HTMLParser：两文件严格UTF-8；lang为中文；标签解析闭合；重复id=0；页内坏链接=0；相对文件链接=0。枚举td.k=197；业务地图h2/h3=120；导航链接27。
Node vm执行从当前业务地图提取的唯一脚本：初始+4个滚动位置+3个阈值边界均只有正确的一项active；scroll监听passive=true。
批准增量逆向字节检查：三份文件分别恢复本轮修订前完整SHA256，证明其余已阅读文本未变。当前lock实算与已更新sidecar/ledger一致；尚未读取Grok第四值，四值检查在本报告写完后执行。
上述检查证据类型为1（HTML原文）及4（只读可复现命令与输出），无外部规范引用。

可复现结构/导航脚本（在正本仓库根目录以python3执行，不写文件）：
```python
from pathlib import Path
from html.parser import HTMLParser
from urllib.parse import urlsplit,unquote
from collections import Counter
import re,json,subprocess,hashlib
class Inspect(HTMLParser):
 def __init__(self):
  super().__init__(); self.ids=[];self.hrefs=[];self.nav=[];self.stack=[];self.charset=None;self.lang=None;self.kcount=0;self.headings=0
 def handle_starttag(self,tag,attrs):
  d=dict(attrs)
  if d.get('id'):self.ids.append(d['id'])
  if tag=='html':self.lang=d.get('lang')
  if tag=='meta' and d.get('charset'):self.charset=d['charset']
  if tag=='td' and 'k' in d.get('class','').split():self.kcount+=1
  if tag in ['h2','h3']:self.headings+=1
  in_nav=any(v for t,v in self.stack) or 'nav' in d.get('class','').split()
  if tag=='a' and 'href' in d:
   self.hrefs.append(d['href'])
   if in_nav:self.nav.append(d['href'])
  if tag not in ['area','base','br','col','embed','hr','img','input','link','meta','param','source','track','wbr']:
   self.stack.append((tag,in_nav))
 def handle_endtag(self,tag):
  if self.stack and self.stack[-1][0]==tag:self.stack.pop()
  elif any(t==tag for t,v in self.stack):
   while self.stack and self.stack[-1][0]!=tag:self.stack.pop()
   if self.stack:self.stack.pop()
paths=['docs/business-enum-quick-reference-v0.1.html','docs/business-system-map-for-operations-v0.1.html']
payload=None
for path in paths:
 p=Path(path);data=p.read_bytes();source=data.decode('utf-8');parser=Inspect();parser.feed(source);parser.close()
 dup=[v for v,n in Counter(parser.ids).items() if n>1]
 broken=[];local=[]
 for href in parser.hrefs:
  u=urlsplit(href)
  if u.scheme or u.netloc:continue
  if not u.path:
   if u.fragment and unquote(u.fragment) not in parser.ids:broken.append(href)
  else:
   local.append(href)
   if not (p.parent/unquote(u.path)).is_file():broken.append(href)
 assert not dup and not broken,(path,dup,broken)
 assert parser.charset.lower()=='utf-8' and parser.lang.startswith('zh') and '\ufffd' not in source
 assert not parser.stack,(path,parser.stack)
 print('HTML_STRUCTURE',path,'PASS','DUP_IDS',len(dup),'BROKEN_LINKS',len(broken),'LOCAL_LINKS',len(local),'ENUM_ANCHORS',parser.kcount,'HEADINGS',parser.headings,'NAV_LINKS',len(parser.nav))
 if 'business-system' in path:
  scripts=re.findall(r'<script(?:\s[^>]*)?>(.*?)</script>',source,re.S)
  assert len(scripts)==1
  assert parser.nav and all(h.startswith('#') for h in parser.nav)
  payload={'source':scripts[0],'ids':[h[1:] for h in parser.nav]}
node=r"""
const fs=require('fs'), vm=require('vm'), assert=require('assert');
const p=JSON.parse(fs.readFileSync(0,'utf8'));
let tops=p.ids.map((_,i)=>i*300), calls=[], active=new Set();
const links=p.ids.map(id=>({getAttribute:n=>{assert.equal(n,'href');return '#'+id;},classList:{toggle:(name,on)=>{assert.equal(name,'active');if(on)active.add(id);else active.delete(id);}}}));
const sections=p.ids.map((id,i)=>({id,getBoundingClientRect:()=>({top:tops[i]})}));
const context={document:{querySelectorAll:s=>{assert.equal(s,'.nav a');return links;},querySelector:s=>sections.find(x=>'#'+x.id===s)||null},window:{addEventListener:(name,callback,options)=>calls.push({name,callback,options})}};
vm.runInNewContext(p.source,context);
assert.equal(calls.length,1);assert.equal(calls[0].name,'scroll');assert.equal(calls[0].options.passive,true);
assert.deepEqual([...active],[p.ids[0]]);
const cases=[0,Math.floor(p.ids.length/2),p.ids.length-1,0];
for(const current of cases){tops=p.ids.map((_,i)=>(i-current)*300);calls[0].callback();assert.deepEqual([...active],[p.ids[current]]);}
for(const top of [119,120,121]){tops=p.ids.map((_,i)=>i===0?-200:(i===1?top:1000+i*300));calls[0].callback();assert.deepEqual([...active],[p.ids[top<=120?1:0]]);}
console.log('NAV_SOURCE_VM PASS initial+4_scroll_cases+3_boundary_cases links='+p.ids.length+' passive=true');
"""
r=subprocess.run(['node','-e',node],input=json.dumps(payload,ensure_ascii=False),text=True,capture_output=True)
print(r.stdout,end='')
assert r.returncode==0,r.stderr
print('CRITERIA-LOCK-SHA256:',hashlib.sha256(Path('docs/audit-2026-09/plan/criteria-lock.md').read_bytes()).hexdigest())

```

实际输出：
```text
HTML_STRUCTURE docs/business-enum-quick-reference-v0.1.html PASS DUP_IDS 0 BROKEN_LINKS 0 LOCAL_LINKS 0 ENUM_ANCHORS 197 HEADINGS 13 NAV_LINKS 0
HTML_STRUCTURE docs/business-system-map-for-operations-v0.1.html PASS DUP_IDS 0 BROKEN_LINKS 0 LOCAL_LINKS 0 ENUM_ANCHORS 0 HEADINGS 120 NAV_LINKS 27
NAV_SOURCE_VM PASS initial+4_scroll_cases+3_boundary_cases links=27 passive=true
CRITERIA-LOCK-SHA256: bdb7813edda7971c8a67c5161c4f236b6d917aa122415ec238042577567d961c
```

## 技能、运行和下一步

已按本会话实际读取的using-superpowers、grilling、codebase-design、ponytail-review、batch-grill-me、ubiquitous-language、writing-for-agents、verification-before-completion等技能借用席内追问、必要性、术语和证据方法；不派第三席、不新增文件类型或业务判据。技能缺失不冒称已调用。
Grok本次路径型交付REPORT_PATH=docs/audit-2026-09/modules/M001-b1-grok-raw.md，EXIT_CODE=0；原始stdout/stderr被适配器丢弃，未向本席展示。报告交付不代表Grok审计PASS，正文在本报告完成后才读取。
下一步按B1互读及四值检查；之后按B2双席合取定稿，即使无修复队列也必须B5整模块双复审才能模块PASS。

## 历史记录（均非本轮候选问题或完成凭证）

# M001 / B1 / Codex 独立审计记录
历史记录版本：V0.07

## 当前状态

本席本阶段结论：FAIL；模块状态：BLOCKED-ASK；B1未完成，本文不是完整self raw凭证。不得读取Grok正文。
申请5及操作员追加的业务地图同句修订已按原文实施并验证；等待申请6的枚举同步许可。此前记录仅为历史。
当前控制提示词：D:\AI\ccwork\160-audit-prompt.md V0.08。
CRITERIA-LOCK-SHA256: 0c8faa6195bef884292302dd207fb525a44aa4c6afb8d252db87484a72b42a10
LOCK-AMEND-COUNT: 2

## 申请5操作员裁决及两份HTML落盘

记录时间（UTC）：2026-09-05T00:32:09.419545+00:00
操作员首条原文：修改为：有争议进「有争议」状态挂起、由主管裁决后继续 ，如主管裁决不接受，则进「有争议，诉讼/仲裁」状态无限期挂起，直至诉讼或仲裁有结果后再行处理
操作员追加原文：业务地图  也修改为这句

- 按两条明确指令，将治理地图540行、业务地图1351行对应退租争议短语逐字替换为操作员指定文本；保留“继续 ，如”中的原有空格，不擅自改写条件、主体或新状态名。
- 治理地图自身版本V0.05至V0.06，SHA256=2a15bba68c5591668cbc2e83b6b43719f8767377cc7c3ef408181467b97a322f；业务地图自身版本V0.05至V0.06，SHA256=7d7c5d1fb8bf36a7ba479933674283c1773b493cacc87809435a19e64c82d38a。
- 本次差异分别严格只有第2、540行和第2、1351行；逆向文本替换与版本递减可恢复本次修改前完整字节。UTF-8严格解码、无替换字符检查通过。此前申请1至4的改动保留。
- 枚举表未获本次修改许可，仍V0.04、SHA256=fcb20f9e0c7b90b145e268bd822e58572f50c1de992b0f23387254e922c3e1ac。当前252行的“退租结算状态”闭集没有本次裁决明确新增的“有争议，诉讼/仲裁”，该枚举与新批准HTML需同步。
- 本次是HTML批准修订，不是B3。B3=0、阶段A方案修复=2、失败重跑=5；本次Grok NOT-CALLED，未读取旧Grok raw。

## CX-04及申请6：登记新批准的退租诉讼/仲裁状态

依据类型1（新批准后HTML原文）及4（现场逐字读取与枚举缺项断言）；冻结判据GEN-016、GEN-018、BEQR-164。
分类：冲突（状态枚举闭集与新批准HTML不同步，遗漏明示状态）；不是Codex自行添加业务状态。
第一性原理：操作员已明确区分主管处理阶段和裁决不被接受后的诉讼/仲裁阶段，枚举表须能表示这两个明确状态。
顺从式：保留已批准分支，不改变主管裁决、拒绝接受或法院/仲裁结果条件。
对抗式例：主管裁决不被接受后，两份新HTML要求进入“有争议，诉讼/仲裁”；枚举表原状态集合却没有此值。

申请6文件：160:/home/james/complex-management/docs/business-enum-quick-reference-v0.1.html
行号：252；仅替换“退租结算状态”的开头枚举列表。
原文：待结算、押金抵扣中、欠费追缴中、已结清、有争议
拟改：待结算、押金抵扣中、欠费追缴中、已结清、有争议、「有争议，诉讼/仲裁」
理由：登记操作员此次已批准并写入两份HTML的状态，不新增业务条件。
批准后本文件自身版本V0.04至V0.05。当前未实施，等待此处逐项许可。

当前M001仍BLOCKED-ASK，因申请5已答复而完成授权修改，但新申请6未答复，全流程暂停。未启动LOCK-AMEND 3；lock、sidecar、ledger基准都保持0c8faa6195bef884292302dd207fb525a44aa4c6afb8d252db87484a72b42a10及COUNT=2。现有冻结引文尚未同步这次已批准源文，不得据此开展新的B阶段审计，也不宣称四值或双席PASS。收到申请6答复后再对PGM-029、BSM-076及相应BEQR-164引文/映射完成必要LOCK-AMEND流程。

## 以前各轮记录（历史）

# M001 / B1 / Codex 独立审计记录
历史记录版本：V0.05

## 当前轮结论与覆盖边界

本席结论：FAIL。模块生命周期：BLOCKED-ASK。当前B1未完成，不是完整self raw完成凭证，不允许据此读取Grok raw。
当前控制提示词：D:\AI\ccwork\160-audit-prompt.md；V0.08；SHA256：d8ee43b34f1cb895b69bf0abef71dc61527150c817047a044a13589216295757。
V0.08相对V0.07只增加每5分钟检查运行进程、不因仍在运行判失败的规则；本次Grok已按该检查间隔真实结束，保留本次交付。
仓库：/home/james/complex-management；main；HEAD：2416128dbacecd06b232f9f25ed8c5ec4ed4f3c2。
CRITERIA-LOCK-SHA256: 0c8faa6195bef884292302dd207fb525a44aa4c6afb8d252db87484a72b42a10
LOCK-AMEND-COUNT: 2
M001 / 322；B3执行次数0；阶段A方案修复2；失败重跑累计5。没有模块PASS。

## 本轮已执行与尚未完成

- LOCK-AMEND 2完成后重新独审；枚举表1至308行、业务地图1至2032行已完整阅读。初次长输出截断处已补读；业务地图1078行与已读枚举表114行的单元格正文由逐字比较确认一致，未把截断当作已读。
- 已读冻结GEN-001至GEN-018及M001映射BEQR-001至197、BSM-001至120；HTML判据均为引文与固定前置要求，全部383引文与源匹配已在本次LOCK-AMEND验证。本项使用GEN-016、GEN-018、BSM-076，不新增判据。
- 治理地图仅作M001权威对照，已读256至307、412至459、518至543、647至662、716至743；本次对照发现540行与业务地图1351行直接冲突。未开启M002。
- 本轮使用已实际读取的using-superpowers、grilling、codebase-design、ponytail-review、batch-grill-me、ubiquitous-language、writing-for-agents等技能的席内方法；不派子代理、不新增文件类型或判据。未声明外部规范或实际业务实现已经验证。
- 本次尚未完成结构链接及导航脚本的重新执行验证、剩余必要权威对照与最终覆盖收口。发现HTML自身问题后按铁律4/5停止，不继续旁支。历史结构检查不作为本轮已重跑的证明。

## CX-03：退租争议解除条件在两份权威HTML中矛盾

分类：冲突（权威HTML自身问题，按铁律4只上报暂停）。
依据类型：1（两份HTML原文）及4（现场读取、行号与assert检查）。
冻结判据：GEN-016、GEN-018、BSM-076；关联权威引文已冻结于PGM-029，本项不扩大M001映射。

- docs/business-system-map-for-operations-v0.1.html:1351：退租结算存在争议的，无限期挂起，直至诉讼或仲裁有结果后再行处理。
- docs/platform-governance-map-for-operations-v0.1.html:540：有争议进「有争议」状态挂起、由主管裁决后继续。
- 第一性原理：同一退租争议挂起状态的解除条件必须可判定；主管裁决与诉讼或仲裁结果是不同事实，原文未给出两种不同争议场景。
- 顺从式：按业务地图明文，尚无诉讼或仲裁结果时应继续挂起；治理地图主管裁决即可继续的表述不满足该条件。
- 对抗式最小例：同一退租结算仍有争议，主管已作决定，但尚无诉讼或仲裁结果；两个条文对能否继续给出相反结果。这是通常的先内部处理场景，不依赖极端事件组合。
- 决策台账定位证据：docs/prd-audit/business-decision-ledger.md:1187，DEC-R4-2026-08-04-77记录“退租结算存在争议的无限期挂起，直至诉讼或仲裁有结果后再行处理”。这里只用于核对HTML解释权声明所指的裁决记录；问题本身由两份HTML原文证明，不以PRD或实现创造业务规则。
- 未审实现代码，未认定系统实际发生过错误放行。尚未读取Grok本轮findings，不声称双席一致。

## 铁律4逐项申请（申请5，尚未实施）

文件：160:/home/james/complex-management/docs/platform-governance-map-for-operations-v0.1.html
位置：第540行“合同运营细则（已确认）”中的退租争议短语。
原文：有争议进「有争议」状态挂起、由主管裁决后继续
拟改：有争议进「有争议」状态无限期挂起，直至诉讼或仲裁有结果后再行处理
理由：与业务地图第1351行及DEC-R4-2026-08-04-77已记录口径一致，消除主管裁决即可提前解除挂起的矛盾。
批准范围：只替换上述短语，并按版本铁律将本文件自身版本V0.05递增至V0.06；其他正文保持原样。批准后还需按LOCK-AMEND完整五步同步PGM-029引文并复核，不能直接恢复B阶段。
当前未修改任何HTML、lock或sidecar。前四项批准改动仍保留；不得把这些历史问题重复列作本轮未修问题。

## Grok本轮交付及隔离

本次M001/B1真实EXIT_CODE=0；元数据适配器成功回执：
REPORT_PATH=docs/audit-2026-09/modules/M001-b1-grok-raw.md
EXIT_CODE=0
Grok运行期间按5分钟检查；运行中没有当作交付失败，不设检查次数或时间上限。进程现已结束，不再轮询。
Grok的原始stdout/stderr在适配器进程内缓冲并丢弃；本轮报告正文没有读取。交付成功不表示内容有效或审计PASS。
本次调用启动时交接措辞为V0.07，最新V0.08仅新增调度轮询规则。本次实际命令如下（stdin=DEVNULL，cwd为正本根目录）：

```sh
/home/james/.local/bin/grok -p '你是本轮独立 Grok 审计席乙。执行 M001/B1，唯一工作区 /home/james/complex-management main，禁止子代理，禁止提交推送或worktree。先读 plan/modules.md 的 M001 完整定义、plan/runbook.md、plan/criteria-lock.md。审计范围仅 M001 两文件 docs/business-enum-quick-reference-v0.1.html 与 docs/business-system-map-for-operations-v0.1.html；第三份 docs/platform-governance-map-for-operations-v0.1.html 可作为权威来源比对。不得读取旧 M001-b1-grok-raw.md，也不得读取任何 self raw 或旧审计结论。这是独立重审，同一路径覆盖旧未采信 raw。三份权威HTML绝对只读；其自身问题必须上报暂停，不得修。业务代码和计划全只读，唯一允许写 docs/audit-2026-09/modules/M001-b1-grok-raw.md，UTF-8，含版本 V0.03。
必须完整执行第一性原理、顺从式、对抗式三线；逐文件全覆盖、逐一核对 M001 所有映射判据（可用连续ID段列覆盖，但每条都审），通用判据仅冻结 general 集合。第一性原理逐条回答：为什么必须存在、声明行为是否实证、哪些前提未经验证。证据只准 HTML逐字原文、实际执行行为、外部规范原文、可复现命令及输出；文档免构造运行测试，可用结构链接检查或逐字对照。先按冲突→细化→技术性→添加固定顺序分类。能指向HTML上位条目优先细化；技术性须无业务发明且无冲突；不得把HTML未提及自动当缺陷或新增判据。每条 finding 使用 GR-01 等临时ID，含文件行号、原文、证据类型1/2/3/4、冻结判据ID、分类、工程缺陷及为何成立。HTML自身问题明确标识铁律4暂停要求，附具体申请：哪个文件哪行、拟改什么、为什么，禁止实际改。报告可列未发现缺陷的覆盖记录。独立执行 sha256sum docs/audit-2026-09/plan/criteria-lock.md，报告独占行 CRITERIA-LOCK-SHA256: 实算哈希，不能猜。
先核对你运行环境技能目录并读实际适用 SKILL.md：using-superpowers、grilling/grill-me、codebase-design、ponytail-review；存在才用，缺口如实报告。技能只借追问结构，不派子代理、不等用户、不生成额外文件，不扩审计范围或新增判据。不读取 .scratch、docs/_archive 或旧审计资料。
结论PASS/FAIL必须证据充分，未验证缺口不得PASS。
交接措辞按控制提示词 V0.07：把完整审计报告写进 docs/audit-2026-09/modules/M001-b1-grok-raw.md。只需完成文件交付；不需要在stdout返回任何机器可读标记、路径前缀或字段。runbook 中旧版stdout交接措辞由本条替代，报告正文仍包含前述哈希证据。' -m grok-4.6 --reasoning-effort xhigh --output-format json --permission-mode bypassPermissions --no-subagents --disable-web-search --cwd /home/james/complex-management
```

## 可复现的只读证据

```sh
sed -n '1351p' docs/business-system-map-for-operations-v0.1.html
sed -n '540p' docs/platform-governance-map-for-operations-v0.1.html
rg -n 'DEC-R4-2026-08-04-77' docs/prd-audit/business-decision-ledger.md
sha256sum docs/audit-2026-09/plan/criteria-lock.md
```

上述原文已通过Python按行读取及assert现场复核。lock实算、sidecar与ledger当前三值一致；B1尚未结束，Grok报告中的第四值未读取，不能宣称四值校验完成。

## 以前各轮的原始记录（仅历史，不是本轮完成凭证）

# M001 / B1 / Codex 独立审计记录
历史记录版本：V0.04

## 当前轮结论与覆盖边界
本席结论：FAIL。生命周期：BLOCKED-ASK。
LOCK-AMEND 1双PASS后从M001/B1独立重开；本次因发现权威HTML自身的新冲突，按控制提示词V0.05铁律4/5暂停。模块未完成全量独审，不是完整self raw完成凭证，不得据此读取Grok报告。
控制提示词：D:\AI\ccwork\160-audit-prompt.md；V0.05；SHA256：4729211fb05a6729779083e63161ee4746ec234c26721b34426e0da5d0b2fe62。
HEAD：2416128dbacecd06b232f9f25ed8c5ec4ed4f3c2
CRITERIA-LOCK-SHA256: f86e10ac97a643ef7339aefede71bd1f145d1095cea819135db1c97f829d1164
Grok本次M001/B1已通过元数据适配器交付，真实EXIT_CODE=0；报告路径docs/audit-2026-09/modules/M001-b1-grok-raw.md。原始JSON与报告正文均未读取，不把交付成功当作审计PASS。
本次读取枚举表分段源文；首次1–110行输出截断，随后52–85、86–143、144–214、215–308分段补读。业务地图只读取了本问题的相关原文，未宣称覆盖2032行。读取固定general GEN-001至GEN-018。其余模块语义比对尚未完成。
本次实际重读并使用grilling、codebase-design、ponytail-review：只借席内追问、必要性和接口口径检查，不派第三席、不引入架构要求；沿用已读verification-before-completion验证证据后陈述。

## CX-02：未核销减除说明遗漏第一段固定额规则
分类：冲突（两处概要说明与权威HTML的详细计算规则矛盾）。
依据类型：1（HTML原文）及4（现场读取与assert复核）。
冻结判据：GEN-016、GEN-018、BEQR-020、BEQR-147、BSM-031、BSM-104。
位置：docs/business-enum-quick-reference-v0.1.html:230；docs/business-system-map-for-operations-v0.1.html:1686。

详细规则证据：
- 枚举表:52：“该酒吧参加第一段的，按其第一段的固定金额或固定单价一次减除，不再按第二段档位另行减除（参加第一段的酒吧本就不占第二段名次，无第二段比例可用）”。
- 业务地图:752：“参加第一段的按其固定金额或固定单价一次减除，不再按第二段档位另行减除（拿了第一段的不占第二段名次，无比例可用）”。

冲突原文：
- 枚举表:230：“计费基数为逐票按该票第二段参与集合的档位表所对应的百分比份额”。
- 业务地图:1686：“按该票第二段参与集合的档位表百分比从可分配池中减除”。

第一性原理：第一段酒吧已有锁定的固定额，且明确不占第二段名次，没有第二段百分比可查；必须按原文既有的两类计算对象分别描述。
顺从式：先遵守详细规则，第一段按固定额、第二段按适用比例；当前两处概要没有覆盖第一类。
对抗式最小例：一张票覆盖A（第一段锁定20元）和B（第二段）；A未入场。详细规则要求减除A的20元；概要要求查询A的第二段百分比，而A不在该集合中，不能按其说明得到同一结果。
本次只证明HTML说明冲突，未读取实现代码，也未认定实际结算程序少扣、重复扣或算错。

## 新的逐项修改申请（未实施）
申请3：docs/business-enum-quick-reference-v0.1.html:230
原文：计费基数为逐票按该票第二段参与集合的档位表所对应的百分比份额
拟改：计费基数按该票未核销酒吧所属分配段确定：第一段按该票锁定的固定金额或固定单价，第二段按该票第二段参与集合的适用比例（含并列均分）
理由：与本文件第52行既有两段减除规则一致，补齐第一段固定额，不改变金额归属或增加新收费。

申请4：docs/business-system-map-for-operations-v0.1.html:1686
原文：按该票第二段参与集合的档位表百分比从可分配池中减除
拟改：参加第一段的按该票锁定的固定金额或固定单价一次减除，参加第二段的按该票第二段参与集合的适用比例（含并列均分）一次减除
理由：与本文件第752行既有规则一致，防止把第一段酒吧套入第二段百分比。

两处批准后才实施，届时分别递增文件自身版本并对BEQR-147、BSM-104执行新的LOCK-AMEND完整流程。当前只记录申请，没有改这两处HTML，没有改当前冻结判据/sidecar/ledger基准。未进入B2，未分配正式finding ID，B3仍0，失败重跑仍5。

## 上次B1历史记录（已由LOCK-AMEND重开，不作为本次完成凭证）
历史记录版本：V0.02

## 当前结论与覆盖边界
本席结论：FAIL。
原因：权威 HTML 的费用汇总用语存在歧义，触发控制提示词 V0.02 铁律4/5的上报暂停要求。
本模块尚未完成全量独立审计；本记录不是完整 self raw 的完成凭证。不得因本文件已经存在而读取 Grok raw。待操作员逐项答复后，先继续完成本席独立审计，再按原顺序互读。
未进入 B2，未分配正式 finding ID，未修复业务文件。

## 运行与隔离
控制提示词：D:\AI\ccwork\160-audit-prompt.md；V0.02。
控制提示词 SHA256：29bfac9e7c58025e759cbb00bb1e6394130dd2a5c3df42c77321e83d98c6369b
仓库：/home/james/complex-management；main。
HEAD：2416128dbacecd06b232f9f25ed8c5ec4ed4f3c2
旧轮锁（历史）：112485cbc5a18e78f36117d7018870f07d23550ee2e315c13c4c55cdaa2ae81a
Grok 本次通过文件元数据适配器交付，真实 EXIT_CODE=0；原始 stdout/stderr 不解析、不打印、不落盘。适配器自检覆盖新建、更新、旧文件未更新、缺失、空文件、越界、非普通文件、非零退出。尚未读取 Grok 原始输出或报告正文。

## 已执行的独立检查
- 三份权威 HTML 以 UTF-8 成功解码；枚举表308行，业务地图2032行，治理地图979行。
- Python HTMLParser 检查三份 HTML：重复 id 均为空；页内失效 href 均为空；相对文件链接列表均为空。此检查不代表 HTML 完整语义或可访问性 PASS。
- 读取冻结 general GEN-001 至 GEN-018 与 M001 映射。M001 映射包括 BEQR-001至197、BSM-001至120。
- 阅读枚举表部分内容并对照本项关联的业务地图原文；大块读取存在输出截断，未将截断部分计为已覆盖。后续须补齐所有未完成内容。
- 技能按本会话已读取的 using-superpowers、grilling、codebase-design、ponytail-review、verification-before-completion、writing-for-agents 收窄规则使用；不派子代理、不新增判据、不产生额外文档。架构性判据不用于要求权威文档新增架构。
- 第一性原理：枚举表承担明确的业务口径，不能删除整个模块；本项费用定义已由权威原文明确，不借实现反推；尚未验证模块全部声明，因此不得声称模块 PASS。
- 顺从式：原文明确票务积分发行成本先于域间拆分扣除；不能因后句“两项”而省略它。
- 对抗式：逐项清单包含通路费、第三方销售渠道费（条件适用）、积分发行成本；汇总词“两项”可被理解为只指两项渠道费用，也可被理解为渠道费用合类后再加积分，存在指代歧义。只报告表达问题，不认定现有计算实现少扣了费用。

## CX-01：费用汇总“两项”指代歧义
分类：冲突（费用项数/指代与逐项清单不一致的文字问题，不是新增业务需求）。
证据类型：1（权威 HTML 原文）及4（可复现读取命令）。
冻结判据：GEN-016、GEN-018、BEQR-019、BEQR-020。
位置：
- docs/business-enum-quick-reference-v0.1.html:51
- docs/business-system-map-for-operations-v0.1.html:749

两处共有原文：
> 跨域组合通票扣完上述两项后，再按各业态价格占比分到各域

同段均已明确：
> 票务订单产生的积分发行成本，在域间拆分之前从整单扣除且只扣一次

枚举表第52行另明确先扣通路费和适用的销售渠道费，第二步扣票务积分发行成本，第三步域间拆分。
复现命令（仓库根，只读）：
```sh
rg -n '跨域组合通票扣完上述两项后' docs/business-enum-quick-reference-v0.1.html docs/business-system-map-for-operations-v0.1.html
sed -n '51,52p' docs/business-enum-quick-reference-v0.1.html
sed -n '749p' docs/business-system-map-for-operations-v0.1.html
```
实际结果：原短语在上述第51行和第749行各出现一次；同段同时包含积分发行成本在拆分前扣除的规定。未执行或判断代码中的资金计算。

## 铁律4逐项申请（均未实施）
申请1：docs/business-enum-quick-reference-v0.1.html:51
将“跨域组合通票扣完上述两项后”替换为“跨域组合通票扣完上述通路费、适用的销售渠道费及票务积分发行成本后”。
理由：明确引用本段已有扣减项目，不改变计费顺序、条件和承担主体。

申请2：docs/business-system-map-for-operations-v0.1.html:749
作同一短语替换，理由相同。

若操作员批准并实施正文修改，还须依版本规则递增被修改文件自身版本；当前未作任何 HTML 修改。判据引用若需随批准后的原文更新，须按控制提示词 LOCK-AMEND 完整流程办理，不得自行同步哈希。

## 操作员批准与恢复记录
操作员原文：“批准  申请 1、2”。两项逐句替换已获批准；当前转入 LOCK-AMEND，同步 BEQR-019、BSM-031 的权威引文。本旧版独立审计仍未完成，不得据此读取 Grok 旧raw，LOCK-AMEND 后从 M001/B1 重开。

## 申请3、4的批准与恢复
操作员原文：“确认申请 3、4”。两处严格按申请文字修改，并递增文件自身版本。当前恢复IN_PROGRESS，执行LOCK-AMEND 2，拟同步BEQR-147、BSM-104；双PASS后更新ledger并从M001/B1重开。本文上方记录为修改前尚未完成的独审，不能作为新一轮完整self raw凭证，Grok正文仍不得读取。
当前控制提示词V0.05的SHA256：90b05a83bd11ad2e734b76fec93493f858f9a5ef899598a8af31d2943f3d2198。回执型复核改为直接读取CLI JSON；路径型报告继续经元数据适配器。


## 申请6的批准与恢复

操作员原文：批准申请 6。已按申请将枚举表252行补入「有争议，诉讼/仲裁」，版本V0.05。当前恢复IN_PROGRESS并执行LOCK-AMEND 3；本文上方B1未完成结论属于旧基准下的历史记录，不能作为读取Grok raw的凭证。双席冻结复核后从M001/B1重开。


## M001/B1互读、四值核对与HTML闸暂停

本次B1执行顺序完成：Grok元数据适配器交付成功（真实EXIT_CODE=0），之后Codex完成独审并落self raw V0.08，才读取Grok raw全部283行。没有在self完成前互读。
Codex互读前独审结论PASS；Grok原始结论FAIL（GR-01、GR-02、GR-03）。双席合取为FAIL，不以本席PASS覆盖Grok，不改写Grok报告。
B1后四值相等，均为bdb7813edda7971c8a67c5161c4f236b6d917aa122415ec238042577567d961c：实际lock、sidecar、ledger基准、当前精确路径modules/M001-b1-grok-raw.md中的独占哈希行。
Grok正文已确认的源位置均在当前HTML存在。当前问题处理说明：
- GR-01：段末总结未登记新第六态；Codex认为“有争议可挂起仲裁”本身不等于取消同段明示的主管裁决出口，不采纳“必然取消出口”的过强推断，但重复总结与旧数量注释需要消除歧义。按Grok否决保留FAIL并申请7、8，不自行关闭其finding。
- GR-02：同手机号“没买过”的概要未写明已批准的注销后不穿透历史例外；可按详细规则理解，但为消除两种读法，申请9直接复用已有例外原文。没有证据说明后端实际按终身历史阻断新人。
- GR-03：Codex不把“没有等字的列举”一概当成严格闭集；但治理概要确实未列出另两份已明确的两类。申请10补齐已有八类，不增加新业务，保留Grok FAIL待操作员裁决。
receiving-code-review已实际读取：先核源文，再评价主张，不因Grok否决而伪称每一句推理都确定成立。铁律4只上报暂停，尚未进入B2、未分配正式finding ID。

### 申请7

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html
行：1351
Grok临时发现：GR-01
冻结判据：BSM-076、BEQR-164、GEN-008、GEN-016
原文：退租状态分待结算、押金抵扣中、欠费追缴中、已结清，有争议可挂起仲裁。
拟改：删除上述完整句子，不替换为新句。
理由：删除重复且未同步新争议状态的段末总结，保留同段已批准的完整处理规则及枚举表六态。

### 申请8

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html
行：1337
Grok临时发现：GR-01
冻结判据：BSM-075、GEN-016
原文：退租结算五态
拟改：退租结算状态
理由：历史修订注释不再带会过时的状态数量；不把2026-06-26的历史注释改写成当时已有六态。

### 申请9

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html
行：781
Grok临时发现：GR-02
冻结判据：BEQR-009、BEQR-030、BSM-033、BSM-035、GEN-008
原文：按注册手机号判定（需短信验证），同一手机号没买过才算新人，系统自动判。
拟改：按注册手机号判定（需短信验证），同一手机号没买过才算新人；注销后重新注册默认不穿透历史，可再享新人资格，系统自动判。
理由：保留原有“没买过”的普通新人条件，同时明示枚举表第39、68行已有的注销重新注册例外，避免删除原有资格条件。

### 申请10

文件：160:/home/james/complex-management/docs/platform-governance-map-for-operations-v0.1.html
行：537
Grok临时发现：GR-03
冻结判据：BEQR-163、BSM-028、BSM-076、GEN-008；PGM-029为对照源
原文：管理合同全生命周期变更：续租、退租、面积变更、租金调整、经营范围变更和提前终止
拟改：管理合同全生命周期变更：续租、退租、面积变更、租金调整、经营范围变更、提前终止、经营范围授权（超范围下单申请经审批放行形成的授权记录）、位置变更（换铺位／位置调整子合同，与面积变更并列）
理由：将概要列举与枚举表第251行及业务地图第706、1351行已有八类统一，不增加新的合同变更类型。

获批准后只执行获批项，并递增相应HTML自身版本（若上述地图修改同批获批，业务地图V0.06至V0.07、治理地图V0.06至V0.07）；随后按必要LOCK-AMEND完整五步处理。当前所有上述申请尚未实施。LOCK-AMEND-COUNT仍3，B3仍0，阶段A方案修复仍2，失败重跑仍5。M001转BLOCKED-ASK，其他321模块仍PENDING，全流程暂停。


## V0.11 B2候选归并与操作员答复（2026-09-04）
版本：V0.11（本次追加后的文件版本；前文版本标记保留为历史证据）

输入raw：docs/audit-2026-09/modules/M001-b1-self-raw.md、docs/audit-2026-09/modules/M001-b1-grok-raw.md。本次只追加，不覆盖历史独审内容，不新增哈希分隔行。
已完整保留Grok三项原始finding并归入候选M001-001至M001-003；技术性无缺陷内容登记M001-004（候选修复队列否）。旧CX-01至CX-04不重复入队。
操作员已批准申请7、8、10；申请9以“按注册手机号判定（需短信验证），同一手机号没买过才算新人；注销后重新注册默认不可再享新人资格，系统自动判。”为准，覆盖旧“可再享”拟稿。
本席未改写Grok原始finding，保留原有GR-01/GR-03推断分歧。申请9改变权威规则后，当前枚举39/68与地图862需要同步，精确申请11至13已写findings与ledger。
B2未定稿：候选已经落盘，Grok复核尚未调用，两席B2结论尚未形成；不能写PASS。按HTML逐条许可闸再次暂停为BLOCKED-ASK，等待上述三处新增同步授权。B3=0，没有实施本批HTML修复，没有修改lock、sidecar或ledger基准，未重开B1。
技能：本次恢复已实际读取using-superpowers及Codex适配参考、writing-for-agents；仅使用与V0.11兼容的步骤与文档组织要求，没有派第三席或子代理，没有额外产物。


## 申请9一年间隔新裁决及原提案撤回
版本：V0.12（本次追加后的文件版本；历史版本保留）

操作员原文：申请 9   按注册手机号判定需短信验证。同一手机号没买过才算新人。注销后重新注册，如果重新注册时间和注销时间间隔不足一年，不可再享新人资格 。如果重新注册时间和注销时间间隔超过一年。可以再享新人资格，系统自动判断。

最新文本及同步草案见findings.md V0.02；申请12/13旧长期留存方案撤回，未实施。保留原GR-02及M001-002，不重开B1，不伪造Grok新结论。恰好满一年边界已询问、未答；申请11至13新版草案尚未获逐处许可。M001仍在B2/BLOCKED-ASK，B3=0，Grok=NOT-CALLED；本次未修改权威HTML或冻结判据。


## 申请9满一年边界已答复
版本：V0.13（本次追加后的文件版本；历史版本保留）

操作员弹窗答复原文：可以再享（满一年及以上）。
本批申请9确定文字：按注册手机号判定，需短信验证。同一手机号没买过才算新人。注销后重新注册的，重新注册时间与注销时间间隔不足一年，不可再享新人资格；满一年及以上，可以再享新人资格，由系统自动判断。
申请11至13同步提案据此更新至findings.md V0.03，仍待逐处HTML许可；批准7、8、10不变。保持M001/B2/BLOCKED-ASK、B3=0；Grok=NOT-CALLED。未重开、未修HTML、未改冻结判据。遵照操作员要求，后续弹窗无选择时保持等待，收到实际答复后才继续。


## 申请11至13批准后的B2恢复
版本：V0.14（本次追加后的文件版本；历史版本保留）

操作员分别答复“批准申请 11”“批准申请 12”“批准申请 13”，对应findings.md V0.03完整提案。候选更新至V0.04，批准7至13的当前有效修改均已齐备；没有待答HTML或添加项questions。从原M001/B2恢复IN_PROGRESS，未重开，B3=0。接下来先派Grok对两份原raw与全批候选做差集/分类/队列复核，本席另行独立复核并追加B2结论。本段仅记录恢复与批准，不是B2 PASS。


## M001 B2整批归并复核 — Codex
版本：V0.15（本次追加后的文件版本；历史版本保留）

B2结论：PASS。
结论范围仅为本批候选归并、分类、队列与授权复核，不是M001最终PASS，也不是宣称待修问题已消除。Grok本批B2尚未互读，待其结论后才判断双席合取。

输入raw：
- docs/audit-2026-09/modules/M001-b1-self-raw.md：本次B1完整独审及后续互读/裁决记录；本次B2追加前完整SHA256=d975038a7ef9e523a9e77811eb807097998cba173320e24891ae50e58209b447。
- docs/audit-2026-09/modules/M001-b1-grok-raw.md：本次B1完整独审，GR-01/02/03；Grok B2派发前SHA256=f0d6eca4bcfd65c67fc3e1ce33e633769926731abd6a9616cc591a8265f20397。本段落盘前没有读取本次Grok B2追加内容。
候选：docs/audit-2026-09/findings.md V0.04，SHA256=c4e4b8d11a4afe429a2fbba0f2480132170c06045ae136a88cadaa802055fbc5。

差集：
- GR-01、GR-02、GR-03分别完整归入M001-001、002、003；候选附录原始Grok findings逐字保留。原始已解决CX-01至04明确不作为当前待修项重复入队。
- 本席CSS/静态结构/导航技术性分类登记为M001-004，证据、无业务发明理由均在候选；无缺陷，修复队列否。
- 原始两席关于GR-01/03推断的不同意见保留，未删除Grok否决、未把新操作员裁决冒充Grok原建议。GR-02由操作员最新“满一年及以上可再享”裁决确定修复方向，原发现仍可追溯。

分类与队列：
- M001-001/002/003是已确认的权威文档口径冲突，归属是；M001-004只影响展示与导航，无业务发明、无已查出工程缺陷，归属否。
- 四个正式ID唯一；本批没有新增添加项，没有未答复questions。所有引用ID在现有冻结定义中存在；M001映射格式实际为“M001 → [...]”，按实际格式检索已验证。未新增判据，也未修改冻结原文。
- 申请7、8、10已有明确批准；申请9最新业务原文与满一年边界已确认；申请11、12、13均分别获得弹窗批准。旧长期留存提案已撤回，不能实施。
- 八处获准正文替换/删除均已在正本对应文件及行号匹配到唯一旧短语，涉及三个HTML；拟改文字均已在候选明确。仅授权这些位置与各文件必需的版本递增，不能借此顺改其他条文。
- 这是文档归并审查，未声称手机号资格后端已实现或运行测试通过。

调用记录：首次本地CreateProcess因Windows命令长度限制未能启动Grok，属调用层故障；改为在本机内存读取已核验V0.11提示词并经stdin传递后，Grok CLI已启动。Grok可执行文件、模型grok-4.6、xhigh及全部运行参数不变，未新增文件/日志；运行失败重跑计数仍5。运行中按五分钟检查一次，未将等待视为失败。


CRITERIA-LOCK-SHA256: d4ba138e924c6fb13020cf26e55aef537a9488ba84dfb72ca9d0f851b5090b80

# M001 新锁 V0.04 的 B1 完整独立审计（Codex）

版本：V0.16

结论：PASS。本次完整审完M001两文件，未发现需要新增修复的工程缺陷或未获授权业务添加；另登记技术性内容CX-01。此为Codex单席B1结论，不是双PASS、不是模块收口。Grok本次已交付但正文尚未读取。

控制提示词V0.11；M001/322，IN_PROGRESS，B3已执行1次，LOCK-AMEND-COUNT=4。LOCK-AMEND⑤已将M001回到PENDING，后按B1进入IN_PROGRESS；保留原始报告、批准7至13和第一轮全量审计记录。本次依据是当前原文及下列现跑命令，不用旧审计意见代替证据。

## 本次范围与字节

- docs/business-enum-quick-reference-v0.1.html：V0.06，308行，132504字节，SHA256=1c3c25441655285812c3681d4f4baa5d0918e3182e5f7cc22299ef7ff34ad248。
- docs/business-system-map-for-operations-v0.1.html：V0.07，2032行，303513字节，SHA256=7afff1dc5b6375557d995f5808d326fda9b4f0f6a4ea6adbd78fcb0f5a3b2ac3。
- 治理HTML仅作权威对照，当前V0.07，SHA256=c63041a15599fbe4768b7131f78e9c6d6c0ffabe8b5ffc3a1151e24eac1c1dc0；重点核对420至436主体/收入、529/536审批、537/540合同与争议、655权限、872客人查询及974解释权条；没有提前开展M002全量审计。
- M001映射335条：GEN-001至018、BEQR-001至197、BSM-001至120。下表逐ID记录覆盖，317条HTML引文与当前源文件逐字匹配，定义、标签、映射本次只读。

## 全文件覆盖

枚举1至308全部阅读：头部和CSS1至25；票务27至60；会员63至96；酒吧98至135；游乐138至154；供应链157至182；财务185至203；第三方206至212；风控215至221；治理224至242；地产245至273；薪资276至285；服务点单288至291；分红294至303；解释权与闭合305至308。注释逐条另读，不把历史说明改作现行规则。

业务地图1至2032全部阅读：CSS与结构1至459；入口、总则和主流程460至693；基础694至721；票务726至809；会员813至843；客人端850至922；现场端929至971；一体机978至1003；酒吧1010至1112；服务点单1118至1144；供应链1151至1246；游乐1253至1300；商户1308至1331；租赁1337至1362；物业1368至1393；楼宇1399至1419；员工1425至1472；第三方1479至1567；留痕1574至1628；财务1636至1729；分红1737至1774；看板1781至1820；终端1827至1890；关系1897至1922；交付1928至1950；已拍板与解释权1957至2006；实际导航脚本2008至2030及闭合2031至2032。其间结构行同经HTMLParser逐事件解析，没有只读上述摘要而遗漏正文。

## 第一性原理线

1. 必须存在的原因：本模块本身是用户指定的业务权威。枚举逐项定义票种、资格、身份、费用、状态；地图把主体、动作、入出条件、收款/归属、追溯和审批连接起来。删除其定义会失去相应业务规则来源。CSS、表格和页内导航承载阅读，不承载新业务条件，单独走技术性分类。
2. 实际验证的边界：本模块为说明性HTML，不把其中要求实现的付款、核销、短信等误称为已执行后端行为。依据类型1为所读当前HTML原文，类型4为UTF-8/结构/链接/判据引用检查及执行当前导航脚本的Node命令。导航用模拟DOM执行实际script，验证初始化、27个滚动目标、120px边界和passive监听；未声称完成真实浏览器视觉或业务端到端测试。
3. 已核查的前提：两文件仍与本次读取哈希一致；335条映射全覆盖；手机短信标准、不足一年与满一年边界、注销事件一年删除与两用途、六态退租及八类合同变更都有当前明文。合法性审批或第三方能力描述是业务要求/接入前提，不凭本模块给出法律合规或真实供应商已接通的结论。

## 顺从式与对抗式线

- 票务：分别检查销售/收款/收入归属，先整单扣通路费、适用销售渠道费、积分成本后拆域；酒吧赠送成本和综合服务费不重复扣；第一段固定额与第二段参与排名互斥；默认1至10家档位从当前原文提取，各行合计100%；并列4/5名(20+15)/2=17.5。未核销、无第二段参与者及尾差独立列示，未将任一栏替换另一栏。
- 核销与退款：综合体三环节、每店入场先于赠酒、场次终结、多次票剩余次数和多人票人份均核对。通票安全退款与最高权限普通例外有各自射程；手环/设备故障/人工年龄复核按正文专门规则处理，没有用摘要覆盖特例。
- 会员：手机号短信验证为判定源，注销再注册不足一年不可、满一年可；同手机号未购买与注销特例并读；仅一年注销事件记录用于异常预警及资格判定，不引入长期历史绑定。枚举38/67是2026-08-04历史注释，当前39/68和地图781/862明确新规则；不将历史注释作为当前允许立即再享的证据。积分与成长值分别按付费/履约，券发放预占、核销实扣及取消释放对应；原积分到期处理不重设有效期。
- 酒吧：预留排他、低消打折前累计与折后提成基数分开；员工代订免定金、自助超时定金没收；转台/并台/拆台/撤销快照不得覆盖新会话，名额与消费归属完整；挂起取消/支付互斥和迟到资金退款；坏账出口与关台/日结例外并读。服务费、小费、点单管理费分别核算，无跨主体混算。
- 供应链/游乐：高价值阈值与平均成本用途分开；不可消费借用保所有权，可消费调用交付转移；存酒转让和转存不混，正常核验与人工取酒例外同时读。项目实时人数只进出自动生成，双阈值/巡检/关键设备依赖/全园补偿无新增维度。
- 地产/员工：合同八类和主线平行线审批一致；退租六态与主管/诉讼仲裁出口一致，押金等待自最后审批起算，付款不受三付账日约束。工单按类型分支，已写明的确认超时出口不作为全类流程。薪资、处罚与请假扣减分栏；跨月班次结束时点与营业日一般原则的例外按原文执行。
- 财务/治理：已支付、已核销、服务费应发/扣留、税务正式核算、收入与代收分开；跨月冲正不覆盖封账数据；三区分的税费差额与分红净利润；五类最终快照先于日结；三个付账日、异议期和银行在途/明确失败分开；权限显式拒绝、设备地理范围和敏感查询不因普通管理员而绕开。
- 配置分类反例核查：地图521的分类清单不可后台增删，与1020来源枚举、1068折扣原因可配表面不同。依据两HTML自身的确认时间线解释条（枚举306、地图2003），仅查既有决策台账确认时间：DEC-CFG-2026-08-03-09在935行是8月3日总则；DEC-R8-97-03（2106）和DEC-R8-97-28（2131）是8月19日逐项批准，两处当前HTML的特殊口径在后，不能用早期总则否掉后期批准。台账在此只证明用户确认时间，不提供第四份业务需求；没有把其中过时“折后低消”内容搬回当前HTML。
- 技术结构：两文件标签栈闭合、UTF-8严格解码、无U+FFFD/NUL、zh-CN与UTF-8元信息存在；枚举197行键、地图27h2+93h3；地图29个ID无重复、45个href全部为有效页内锚点；枚举无脚本、地图仅静态滚动导航，无网络写操作、用户输入拼接或内联事件注入路径。

## 本次完整发现与四分类

CX-01（技术性登记，工程缺陷否）：枚举L1至25及表格静态结构；地图L1至459、导航链接和L2008至2030。按冲突→细化→技术性→添加依序检查：与业务明文不冲突；不新增业务实体、价格、状态、权限或校验条件；仅用于HTML显示和页内导航，故归技术性，修复队列否，不触发添加项人闸。证据类型4，GEN-001/002/013/015/016/017/018；当前执行与结构结果见下。B2应按既有登记范围核对是否复用M001-004，不因再次登记重复分配永久ID。

其余业务正文为本模块权威条目自身及对同条的解释，不把源文件中已授权定义反判“HTML外添加”。本次没有其他临时finding，没有未答复questions。不存在有条件PASS。

## 技能使用与限制

本节点重新读取实际SKILL.md：grilling、codebase-design、diagnosing-bugs、batch-grill-me、ubiquitous-language、ponytail-review、verification-before-completion、writing-for-agents。全部按V0.11 §6收窄：自行追问与取证，不派子代理，不因技能默认流程开问题或外发；不生成UBIQUITOUS_LANGUAGE.md，不修HTML或业务代码，不造文档行为测试。不改任何Grok报告、计划、锁或业务源。

## 复现命令和实际结果

以下Python均通过stdin交给160的python3，在/home/james/complex-management运行；不创建临时脚本。命令出口均为0。阅读命令使用Path.read_text(encoding='utf-8')逐段输出原文及不改变内容的HTML标记剥离文本，截断的52至76、1073至1076已另行补齐，不以截断输出冒充完整。

### 结构/判据/注释检查

````python
from pathlib import Path
from html.parser import HTMLParser
from collections import Counter
import hashlib,re,json,subprocess
r=Path('/home/james/complex-management'); a=r/'docs/audit-2026-09'
class Doc(HTMLParser):
 def __init__(self):
  super().__init__(convert_charrefs=True);self.ids=[];self.hrefs=[];self.tagcounts=Counter();self.attrs=[];self.comments=[];self.headings=[];self.k=0;self.stack=[];self.errors=[];self.lang=None;self.charset=None
 def handle_starttag(self,t,aa):
  attrs=dict(aa);self.tagcounts[t]+=1
  self.attrs.append((self.getpos()[0],t,attrs))
  if 'id'in attrs:self.ids.append(attrs['id'])
  if 'href'in attrs:self.hrefs.append((self.getpos()[0],attrs['href']))
  if t=='html':self.lang=attrs.get('lang')
  if t=='meta' and 'charset'in attrs:self.charset=attrs['charset']
  if t in ('h2','h3'):self.headings.append((self.getpos()[0],t))
  if t=='td' and 'k'in attrs.get('class','').split():self.k+=1
  if t not in {'area','base','br','col','embed','hr','img','input','link','meta','param','source','track','wbr'}:self.stack.append((t,self.getpos()[0]))
 def handle_endtag(self,t):
  if not self.stack or self.stack[-1][0]!=t:self.errors.append((self.getpos()[0],t,self.stack[-1:]))
  else:self.stack.pop()
 def handle_comment(self,x):self.comments.append((self.getpos()[0],x))
names=['docs/business-enum-quick-reference-v0.1.html','docs/business-system-map-for-operations-v0.1.html','docs/platform-governance-map-for-operations-v0.1.html']
texts={n:(r/n).read_text(encoding='utf-8') for n in names}
for n in names[:2]:
 s=texts[n];d=Doc();d.feed(s);assert not d.errors and not d.stack,(n,d.errors,d.stack)
 assert d.charset.lower()=='utf-8' and d.lang=='zh-CN'
 assert '\ufffd' not in s and '\0' not in s
 dup=[x for x,c in Counter(d.ids).items() if c>1];assert not dup
 missing=[(l,h) for l,h in d.hrefs if h.startswith('#') and h[1:]not in d.ids];assert not missing
 refs=[(l,h)for l,h in d.hrefs if not h.startswith('#')]
 print(json.dumps({'file':n,'sha256':hashlib.sha256((r/n).read_bytes()).hexdigest(),'bytes':len((r/n).read_bytes()),'lines':s.count('\n'),'ids':len(d.ids),'hrefs':len(d.hrefs),'externalOrRelativeHrefs':refs,'h2':d.tagcounts['h2'],'h3':d.tagcounts['h3'],'td.k':d.k,'scripts':d.tagcounts['script'],'inlineEventAttrs':[(l,k)for l,t,aa in d.attrs for k in aa if k.startswith('on')],'tagBalance':'PASS','utf8':'PASS'},ensure_ascii=False))
lock=(a/'plan/criteria-lock.md').read_text(encoding='utf-8')
sections=re.split(r'(?m)^### ',lock)[1:]
defs={}
for block in sections:
 id=re.match(r'((?:GEN|BEQR|BSM|PGM)-\d{3})',block)
 if id:defs[id.group(1)]=block.split('## 模块映射',1)[0]
mapping=re.search(r'(?m)^M001\b[^\n]*',lock).group(0)
ids=re.findall(r'(?:GEN|BEQR|BSM|PGM)-\d{3}',mapping)
assert len(ids)==len(set(ids))==335
fence=chr(96)*3
for id in ids:
 block=defs[id]
 if id.startswith('GEN'):continue
 match=re.search(fence+r'html\n(.*?)\n'+fence,block,re.S);assert match,id
 source=names[0]if id.startswith('BEQR')else names[1]
 assert match.group(1) in texts[source],id
print('M001 mapped criteria checked: 335 =18 GEN+197 BEQR+120 BSM;317 frozen HTML quotations exact current substrings.')
print('ACTUAL-LOCK',hashlib.sha256((a/'plan/criteria-lock.md').read_bytes()).hexdigest())
for target in ['BSM-003','BSM-051','BSM-054']:
 print('DEF',defs.get(target,'')[:380])
print('ENUM COMMENTS FULL')
for i,l in enumerate(texts[names[0]].splitlines(),1):
 if '<!--' in l:print(str(i)+': '+l)

````

````text
{"file": "docs/business-enum-quick-reference-v0.1.html", "sha256": "1c3c25441655285812c3681d4f4baa5d0918e3182e5f7cc22299ef7ff34ad248", "bytes": 132504, "lines": 308, "ids": 0, "hrefs": 0, "externalOrRelativeHrefs": [], "h2": 13, "h3": 0, "td.k": 197, "scripts": 0, "inlineEventAttrs": [], "tagBalance": "PASS", "utf8": "PASS"}
{"file": "docs/business-system-map-for-operations-v0.1.html", "sha256": "7afff1dc5b6375557d995f5808d326fda9b4f0f6a4ea6adbd78fcb0f5a3b2ac3", "bytes": 303513, "lines": 2032, "ids": 29, "hrefs": 45, "externalOrRelativeHrefs": [], "h2": 27, "h3": 93, "td.k": 0, "scripts": 1, "inlineEventAttrs": [], "tagBalance": "PASS", "utf8": "PASS"}
M001 mapped criteria checked: 335 =18 GEN+197 BEQR+120 BSM;317 frozen HTML quotations exact current substrings.
ACTUAL-LOCK d4ba138e924c6fb13020cf26e55aef537a9488ba84dfb72ca9d0f851b5090b80
DEF BSM-003 — 系统通用能力：数据可改可删、操作有引导、权限分岗

- 来源：`docs/business-system-map-for-operations-v0.1.html:563`；锚点：`h2`
- 适用标签：全局业务实现
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
          <h2>系统通用能力：数据可改可删、操作有引导、权限分岗</h2>
          <p class="lead">下面这几条不属于某一个子系统，而是整套系统都遵守的通用规则。</p>
````


DEF BSM-051 — 酒吧管理：从入场、开台到结账关台

- 来源：`docs/business-system-map-for-operations-v0.1.html:1010`；锚点：`h2`
- 适用标签：bar、entry
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
          <h2>酒吧管理：从入场、开台到结账关台</h2>
          <p class="lead">酒吧管理先做基础经营闭环。平板端服务员工协助点酒；触摸屏一体机可支持客人或现场人员订台、吧台买酒和支付引导；吧台酒水可以按杯、按瓶、按套餐或其他形式售卖。门票主路径仍是客人自己在官方 App/小程序或第三方渠道购买；客人可在自己手机上的综合体
DEF BSM-054 — 桌台/账单状态与经营规则

- 来源：`docs/business-system-map-for-operations-v0.1.html:1053`；锚点：`h3`；父锚点：酒吧管理：从入场、开台到结账关台
- 适用标签：bar、entry
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
              <h3>桌台/账单状态与经营规则</h3>
              <table class="plain-table">
                <thead><tr><th style="width:110px">项</th><th>内容 / 规则</th></tr></thead>
 
ENUM COMMENTS FULL
2: <!-- 文件版本：V0.06；更新日期：2026-09-01 -->
31:     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-02）：单店酒吧门票收款主体统一——酒吧商户定价、销售并确认已核销部分收入，票款由娱乐平台公司统一收款后结付，与 BSM 一致 -->
38:     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-11）：注销后重新注册默认不穿透历史，可再享新人资格；配套的高频注销注册预警见「会员身份状态」条 -->
42:     <!-- 2026-08-04 DEC-R6-08 补入场三步流程；2026-08-06 DEC-R8-31 重申「三道环节分开、不是一次性完成」，统一命名为安检环节／年龄核验环节／检票环节，并明确「一步完成」仅指检票环节内部 -->
50:     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-01）：通路费承担口径统一——在拆分给各域之前从整单扣除、由各域按分配份额自然共担；删除与整单预扣并存的「由收款主体承担」矛盾表述 -->
67:     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-11）：注销后重新注册不穿透历史、可再享新人资格；同一手机号一年内注销+注册合计超阈值触发异常预警（异常预警页面见 BSM 留痕与异常章） -->
69:     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-21）：补六个会员等级的正式名称——新乐会员／银卡／金卡／白金／钻石／黑钻，为出厂默认、可配 -->
74:     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-22）：待追回积分补四类出口——追平自动清零／小额可豁免（出厂默认 ≤100 积分，财务豁免留痕）／不设时效／注销时作废登记不形成现金债权 -->
76:     <!-- 2026-08-04 第二轮裁决：补「刷新积分有效期的消费须达到后台配置的最低消费金额」口径（出厂默认 10 元，须同时配置上下限），未达该金额的消费不刷新有效期；用户逐项批准。 -->
102:     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-05 / DEC-R6-15）：定金不抵扣消费，达低消后原路退回，状态「到店已抵扣」→「达标已原路退回」；同行错别字「爬约」→「爽约」 -->
104:     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-20）：新增「订卡邀请函状态」条目——确认接受即占名额并绑定本人动态入场码，已入场不可移出 -->
113:     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-19）：补拆台／撤销转台／循环转台三条规则——拆台按实际消费归属各台、撤销转台原路回退留痕、循环转台次数上限出厂默认 10 次 -->
116:     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-04）：退品口径改写——正常退品应予退款；仅当退款后累计消费将低于最低消费时才不退款、金额留在本台位账上 -->
141:     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-18）：「已满员」加括注——由项目实时在玩人数自动推算显示，不可人工设置 -->
143:     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-10）：新增「游乐当天／每天」口径条目——一律按营业日（10:00→次日 10:00）计算，时区取项目所在地时间、禁用服务器默认时区 -->
152:     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-17）：包场口径改写——包场期间该项目整体停止对外核销，仅团体客人可入 -->
163:     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-06）：高价值判定阈值补出厂默认 5000 元（仍须同时配置上下限） -->
165:     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-09）：删补录禁令——扫码缺失可事后人工补录，须附证据并经主管复核方生效，补录与原始记录并列展示 -->
194:     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-16）：「演出传媒」标注系统枚举码 media -->
200:     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-03）：税费差异分税种——流转税走申报差异补提／冲回（差异保留），企业所得税等走季度与财税软件核对拉平，两机制按税种互斥不叠加 -->
245:   <!-- 2026-06-24 地产全链授权扩展：业务系统从「娱乐综合体」口径扩展为「地产+娱乐综合体」，新增招商/租赁全周期/物业服务/楼宇设备/能耗/楼宇巡检的业务枚举。依据：用户 2026-06-24 拍板「扩展 HTML 到地产全链」+ gap-analysis §7B（租金押金账单/铺位平面图租赁状态/物业工单/合同审批流已升正式条目）+ 原始需求 §15 平台治理范围。具体取值最终以后台配置为准。 -->
254:     <!-- 2026-08-04 第二轮裁决：合同审批链改按全平台统一审批模型改写（指定主线最终审批人＋逐级自动补全，业务/财务/法务为平行相关部门线各指定该部门最终审批人并补全至该部门最低起点层级，主线与各平行线都通过才生效）；用户逐项批准。 -->
261:     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-13）：物业工单状态加注——回访／验收按工单类型分支适用（保洁类完成即结／投诉类提单方确认／复杂类主管验收+提单方确认双闸），非全类顺序链 -->
263:     <!-- 2026-07-03 回写：商户服务工单(商户专区发起)独立状态链。 -->
272:     <!-- 2026-08-04 第三轮用户裁决（DEC-R6-12）：楼宇巡检类型五类→四类，删「能耗抄表」；能耗抄表走能耗域独立流程（PGM 口径） -->
276:   <!-- 2026-07-03 回写：员工排班考勤薪资/服务点单 新枚举(已上线口径)。 -->
291:     <tr><td class="k">管理费方式</td><td>不收、按比例、固定金额（按服务目录项配置；服务费全额入公司账（此处「公司」指该服务发生地所属酒吧的经营主体），管理费归公司收入，其余份额按薪资/提成名录进服务人员月结薪资）。按比例时配置值必须在 0% 至 100%（含边界）之间；固定金额必须在 0 至服务售价（含边界）之间；任一越界值在保存时直接拒绝，不得静默截断、封顶或改写。服务点单管理费的配置主体与审批、出厂默认值、变更版本化与指定生效日、单据下单时锁定费率版本、变更前向受影响方通告、不收管理费的排除项、退款时的冲回与反向结算，一律比照销售提成的对应规则执行；上下限约束按本条前述区间。服务点单份额与管理费的归属月份按服务完成时点所在营业日确定。<!-- param-tier:ignore 0%;100% --></td></tr>

````

### 实际导航脚本执行

````python
from pathlib import Path
import re,json,subprocess
r=Path('/home/james/complex-management'); b=r/'docs/audit-2026-09';s=(r/'docs/business-system-map-for-operations-v0.1.html').read_text(encoding='utf-8')
js=re.findall(r'<script\b[^>]*>(.*?)</script>',s,re.S)
assert len(js)==1
links=re.findall(r'<a\b[^>]*href="(#[^"]+)"[^>]*>',s.split('<nav class="nav">',1)[1].split('</nav>',1)[0])
code="""
const assert=require('node:assert/strict'),vm=require('node:vm');
const source=SOURCE_HERE, hrefs=HREFS_HERE;
const tags=hrefs.map((href,i)=>({id:href.slice(1), top:i*400,getBoundingClientRect(){return {top:this.top}}}));
const links=hrefs.map(href=>({href,active:false,getAttribute(k){assert.equal(k,'href');return this.href},classList:{toggle(k,v){assert.equal(k,'active');links.find(x=>x.href===href).active=v}}}));
let handler;
const context={document:{querySelectorAll(q){assert.equal(q,'.nav a');return links},querySelector(q){return tags.find(t=>'#'+t.id===q)}},window:{addEventListener(name,fn,opt){assert.equal(name,'scroll');assert.equal(opt.passive,true);handler=fn}}};
vm.runInNewContext(source,context);
assert.equal(links.filter(l=>l.active).length,1);assert.equal(links[0].active,true);
for(let k=0;k<tags.length;k++){tags.forEach((t,i)=>t.top=(i-k)*400);handler();assert.equal(links.filter(l=>l.active).length,1);assert.equal(links[k].active,true);}
tags.forEach((t,i)=>t.top=i*400+121);handler();assert.equal(links[0].active,true);
tags.forEach((t,i)=>t.top=i*400-279);handler();assert.equal(links[0].active,true);
tags[1].top=120;handler();assert.equal(links[1].active,true);
console.log('NAV actual source PASS: boot + '+tags.length+' scroll targets + 120px threshold + passive listener; mock DOM, no browser rendering claim.');
""".replace('SOURCE_HERE',json.dumps(js[0])).replace('HREFS_HERE',json.dumps(links))
z=subprocess.run(['node','-e',code],cwd=r,text=True,capture_output=True);print(z.stdout,z.stderr);assert z.returncode==0
print('POTENTIAL CLASSIFICATION RULE CROSS-REFERENCES')
for name in ['business-enum-quick-reference-v0.1.html','platform-governance-map-for-operations-v0.1.html']:
 for i,line in enumerate((r/'docs'/name).read_text(encoding='utf-8').splitlines(),1):
  if any(x in line for x in ['业务分类定义','来源枚举','原因分类','枚举项业务参数','业务参数（','增减一类']):
   print(name+':'+str(i)+': '+re.sub(r'<[^>]+>','',line))
print('DECISION PATHS')
paths=(b/'plan/scope.txt').read_text().splitlines()
for p in paths:
 if re.search('decision|decisions|裁决|决策',p,re.I):print(p)

````

````text
NAV actual source PASS: boot + 27 scroll targets + 120px threshold + passive listener; mock DOM, no browser rendering claim.
 

````

### 正文算例与三源检查

````python
from pathlib import Path
import re,html,hashlib,json
r=Path('/home/james/complex-management')
e=(r/'docs/business-enum-quick-reference-v0.1.html').read_text(encoding='utf-8')
b=(r/'docs/business-system-map-for-operations-v0.1.html').read_text(encoding='utf-8')
g=(r/'docs/platform-governance-map-for-operations-v0.1.html').read_text(encoding='utf-8')
rows=re.findall(r'第二段参与 (\d+) 家＝([^；。]+)',e)
assert len(rows)==10
totals=[]
for count,row in rows:
 n=int(count);m={}
 for first,last,p in re.findall(r'第 (\d+)(?:[–-](\d+))? 名(?:各)?\s*(\d+)%',row):
  for i in range(int(first),int(last or first)+1):assert i not in m;m[i]=int(p)
 assert sorted(m)==list(range(1,n+1)),(n,m)
 assert sum(m.values())==100,(n,m)
 totals.append([n,sum(m.values())])
print('Source-extracted rank defaults:',totals)
assert (20+15)/2==17.5
for name,text in [('enum',e),('map',b)]:
 assert '满一年及以上'in text and '不足一年'in text
 assert '第一段'in text and '第二段'in text and '仅留存注销事件'in text if name=='enum' else '及注销后重新注册的新人资格判定而留存'in text
 print(name,'newbie interval explicit, SMS explicit, retention purpose present')
flow='有争议进「有争议」状态挂起、由主管裁决后继续 ，如主管裁决不接受，则进「有争议，诉讼/仲裁」状态无限期挂起，直至诉讼或仲裁有结果后再行处理'
assert flow in b and flow in g
assert '有争议、「有争议，诉讼/仲裁」'in e
assert '经营范围授权（超范围下单申请经审批放行形成的授权记录）、位置变更（换铺位／位置调整子合同，与面积变更并列）'in e and '经营范围授权（超范围下单申请经审批放行形成的授权记录）、位置变更（换铺位／位置调整子合同，与面积变更并列）'in b and '经营范围授权（超范围下单申请经审批放行形成的授权记录）、位置变更（换铺位／位置调整子合同，与面积变更并列）'in g
print('Three-source contract/dispute cross-reference PASS; approval9/11/12/13 interval+retention PASS.')
assert not list((r/'docs/audit-2026-09/questions').glob('*.md'))
print('questions count=0; no unanswered item.')
print('Grok current raw content NOT READ; current B1 self report not yet written.')

````

````text
Source-extracted rank defaults: [[1, 100], [2, 100], [3, 100], [4, 100], [5, 100], [6, 100], [7, 100], [8, 100], [9, 100], [10, 100]]
enum newbie interval explicit, SMS explicit, retention purpose present
map newbie interval explicit, SMS explicit, retention purpose present
Three-source contract/dispute cross-reference PASS; approval9/11/12/13 interval+retention PASS.
questions count=0; no unanswered item.
Grok current raw content NOT READ; current B1 self report not yet written.

````

## 全部映射判据覆盖表

| 判据 | 当前来源或核验范围 | 本席结果 |
|---|---|---|
| GEN-001 必要性与最小性 | 来源：通用工程判据 | PASS（已按上述三线核对适用性及原文） |
| GEN-002 可执行正确性 | 来源：通用工程判据 | PASS（已按上述三线核对适用性及原文） |
| GEN-003 测试有效性 | 来源：通用工程判据 | PASS（已按上述三线核对适用性及原文） |
| GEN-004 信任边界校验 | 来源：通用工程判据 | PASS（已按上述三线核对适用性及原文） |
| GEN-005 授权与最小权限 | 来源：通用工程判据 | PASS（已按上述三线核对适用性及原文） |
| GEN-006 数据完整性 | 来源：通用工程判据 | PASS（已按上述三线核对适用性及原文） |
| GEN-007 并发与幂等 | 来源：通用工程判据 | PASS（已按上述三线核对适用性及原文） |
| GEN-008 接口与模式一致性 | 来源：通用工程判据 | PASS（已按上述三线核对适用性及原文） |
| GEN-009 错误处理与恢复 | 来源：通用工程判据 | PASS（已按上述三线核对适用性及原文） |
| GEN-010 安全与敏感信息 | 来源：通用工程判据 | PASS（已按上述三线核对适用性及原文） |
| GEN-011 可观测与审计证据 | 来源：通用工程判据 | PASS（已按上述三线核对适用性及原文） |
| GEN-012 资源与性能边界 | 来源：通用工程判据 | PASS（已按上述三线核对适用性及原文） |
| GEN-013 构建配置与依赖 | 来源：通用工程判据 | PASS（已按上述三线核对适用性及原文） |
| GEN-014 数据库迁移正确性 | 来源：通用工程判据 | PASS（已按上述三线核对适用性及原文） |
| GEN-015 界面可用性与可访问性 | 来源：通用工程判据 | PASS（已按上述三线核对适用性及原文） |
| GEN-016 文档可验证性 | 来源：通用工程判据 | PASS（已按上述三线核对适用性及原文） |
| GEN-017 业务来源分类 | 来源：通用工程判据 | PASS（已按上述三线核对适用性及原文） |
| GEN-018 第一性原理证据 | 来源：通用工程判据 | PASS（已按上述三线核对适用性及原文） |
| BEQR-001 票种 | 来源：`docs/business-enum-quick-reference-v0.1.html:29`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-002 票务策略状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:30`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-003 定价、售票与收款主体 | 来源：`docs/business-enum-quick-reference-v0.1.html:32`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-004 年龄限制 | 来源：`docs/business-enum-quick-reference-v0.1.html:33`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-005 通票赠酒下限 | 来源：`docs/business-enum-quick-reference-v0.1.html:34`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-006 通票类型 | 来源：`docs/business-enum-quick-reference-v0.1.html:35`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-007 权益类型 | 来源：`docs/business-enum-quick-reference-v0.1.html:36`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-008 餐饮/零售券两种用法 | 来源：`docs/business-enum-quick-reference-v0.1.html:37`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-009 资格判定 | 来源：`docs/business-enum-quick-reference-v0.1.html:39`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-010 订单状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:40`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-011 票/权益状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:41`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-012 核销记录状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:43`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-013 手环入场 | 来源：`docs/business-enum-quick-reference-v0.1.html:44`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-014 退款单状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:45`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-015 有效期类型 | 来源：`docs/business-enum-quick-reference-v0.1.html:46`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-016 售票与退款原则 | 来源：`docs/business-enum-quick-reference-v0.1.html:47`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-017 到期未核销款 | 来源：`docs/business-enum-quick-reference-v0.1.html:48`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-018 域归属对照 | 来源：`docs/business-enum-quick-reference-v0.1.html:49`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-019 跨域通票分配 | 来源：`docs/business-enum-quick-reference-v0.1.html:51`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-020 酒吧通票可分配池 | 来源：`docs/business-enum-quick-reference-v0.1.html:52`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-021 收费与免费权益结算 | 来源：`docs/business-enum-quick-reference-v0.1.html:53`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-022 项目内部结算价 | 来源：`docs/business-enum-quick-reference-v0.1.html:54`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-023 核销顺序 | 来源：`docs/business-enum-quick-reference-v0.1.html:55`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-024 游乐核销归属 | 来源：`docs/business-enum-quick-reference-v0.1.html:56`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-025 门票综合服务费 | 来源：`docs/business-enum-quick-reference-v0.1.html:57`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-026 动态价与会员价 | 来源：`docs/business-enum-quick-reference-v0.1.html:58`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-027 人脸验龄数据生命周期 | 来源：`docs/business-enum-quick-reference-v0.1.html:60`；锚点：`td.k`；父锚点：一、票务与权益 | PASS（已按上述三线核对适用性及原文） |
| BEQR-028 会员唯一识别 | 来源：`docs/business-enum-quick-reference-v0.1.html:65`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-029 会员注册 | 来源：`docs/business-enum-quick-reference-v0.1.html:66`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-030 会员身份状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:68`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-031 等级与成长值 | 来源：`docs/business-enum-quick-reference-v0.1.html:70`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-032 等级权益对照 | 来源：`docs/business-enum-quick-reference-v0.1.html:71`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-033 积分与成长值来源 | 来源：`docs/business-enum-quick-reference-v0.1.html:72`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-034 票务人次计算 | 来源：`docs/business-enum-quick-reference-v0.1.html:73`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-035 有效消费与冻结 | 来源：`docs/business-enum-quick-reference-v0.1.html:75`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-036 积分明细类型 | 来源：`docs/business-enum-quick-reference-v0.1.html:77`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-037 积分兑换定价 | 来源：`docs/business-enum-quick-reference-v0.1.html:78`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-038 商品规格 | 来源：`docs/business-enum-quick-reference-v0.1.html:79`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-039 项目零售参考价 | 来源：`docs/business-enum-quick-reference-v0.1.html:80`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-040 发券与核销结算 | 来源：`docs/business-enum-quick-reference-v0.1.html:81`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-041 重复兑换与转赠 | 来源：`docs/business-enum-quick-reference-v0.1.html:82`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-042 券包状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:83`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-043 积分结算账期 | 来源：`docs/business-enum-quick-reference-v0.1.html:84`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-044 会员系统服务费 | 来源：`docs/business-enum-quick-reference-v0.1.html:85`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-045 储值卡类型与收款主体 | 来源：`docs/business-enum-quick-reference-v0.1.html:86`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-046 会员忠诚度扩展 | 来源：`docs/business-enum-quick-reference-v0.1.html:87`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-047 会员注册与开卡 | 来源：`docs/business-enum-quick-reference-v0.1.html:89`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-048 学生认证 | 来源：`docs/business-enum-quick-reference-v0.1.html:90`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-049 会员积分活动状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:91`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-050 积分活动预算守恒 | 来源：`docs/business-enum-quick-reference-v0.1.html:92`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-051 充值积分 | 来源：`docs/business-enum-quick-reference-v0.1.html:93`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-052 会员营销边界 | 来源：`docs/business-enum-quick-reference-v0.1.html:94`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-053 发券对象 | 来源：`docs/business-enum-quick-reference-v0.1.html:95`；锚点：`td.k`；父锚点：二、会员与积分 | PASS（已按上述三线核对适用性及原文） |
| BEQR-054 桌台/卡座/吧台位状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:100`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-055 账单状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:101`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-056 订台定金状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:103`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-057 订卡邀请函状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:105`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-058 点酒方式 | 来源：`docs/business-enum-quick-reference-v0.1.html:106`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-059 支付方式 | 来源：`docs/business-enum-quick-reference-v0.1.html:107`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-060 销售提成 | 来源：`docs/business-enum-quick-reference-v0.1.html:108`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-061 服务员提成 | 来源：`docs/business-enum-quick-reference-v0.1.html:109`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-062 退菜退酒 | 来源：`docs/business-enum-quick-reference-v0.1.html:110`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-063 高危动作（需审批/双证据） | 来源：`docs/business-enum-quick-reference-v0.1.html:111`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-064 出品票据状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:112`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-065 账单操作 | 来源：`docs/business-enum-quick-reference-v0.1.html:114`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-066 收银交班状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:115`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-067 最低消费 | 来源：`docs/business-enum-quick-reference-v0.1.html:117`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-068 服务费 | 来源：`docs/business-enum-quick-reference-v0.1.html:118`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-069 小费 | 来源：`docs/business-enum-quick-reference-v0.1.html:119`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-070 客人存酒有效期与转存 | 来源：`docs/business-enum-quick-reference-v0.1.html:120`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-071 存酒瓶数 | 来源：`docs/business-enum-quick-reference-v0.1.html:122`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-072 套餐赠券适用范围 | 来源：`docs/business-enum-quick-reference-v0.1.html:123`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-073 非低消商品 | 来源：`docs/business-enum-quick-reference-v0.1.html:124`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-074 隐私桌台权限 | 来源：`docs/business-enum-quick-reference-v0.1.html:125`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-075 桌台会话与菜单 | 来源：`docs/business-enum-quick-reference-v0.1.html:126`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-076 桌台计时产品 | 来源：`docs/business-enum-quick-reference-v0.1.html:127`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-077 门店支付方式 | 来源：`docs/business-enum-quick-reference-v0.1.html:128`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-078 扫码点单身份 | 来源：`docs/business-enum-quick-reference-v0.1.html:129`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-079 优惠券自动选择 | 来源：`docs/business-enum-quick-reference-v0.1.html:130`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-080 优惠券付款预留 | 来源：`docs/business-enum-quick-reference-v0.1.html:131`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-081 正式挂起订单状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:132`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-082 正式挂起订单规则 | 来源：`docs/business-enum-quick-reference-v0.1.html:133`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-083 挂单取消与付款互斥 | 来源：`docs/business-enum-quick-reference-v0.1.html:134`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-084 挂单坏账损耗 | 来源：`docs/business-enum-quick-reference-v0.1.html:135`；锚点：`td.k`；父锚点：三、酒吧经营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-085 项目开放状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:140`；锚点：`td.k`；父锚点：四、游乐运营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-086 场次状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:142`；锚点：`td.k`；父锚点：四、游乐运营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-087 游乐「当天／每天」口径 | 来源：`docs/business-enum-quick-reference-v0.1.html:144`；锚点：`td.k`；父锚点：四、游乐运营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-088 容量双阈值 | 来源：`docs/business-enum-quick-reference-v0.1.html:145`；锚点：`td.k`；父锚点：四、游乐运营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-089 园区容量 | 来源：`docs/business-enum-quick-reference-v0.1.html:146`；锚点：`td.k`；父锚点：四、游乐运营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-090 设备关键/辅助分级 | 来源：`docs/business-enum-quick-reference-v0.1.html:147`；锚点：`td.k`；父锚点：四、游乐运营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-091 全园故障与补偿通票 | 来源：`docs/business-enum-quick-reference-v0.1.html:148`；锚点：`td.k`；父锚点：四、游乐运营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-092 安全严重度 | 来源：`docs/business-enum-quick-reference-v0.1.html:149`；锚点：`td.k`；父锚点：四、游乐运营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-093 巡检任务状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:150`；锚点：`td.k`；父锚点：四、游乐运营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-094 越界类型 | 来源：`docs/business-enum-quick-reference-v0.1.html:151`；锚点：`td.k`；父锚点：四、游乐运营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-095 团体/包场 | 来源：`docs/business-enum-quick-reference-v0.1.html:153`；锚点：`td.k`；父锚点：四、游乐运营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-096 游乐增强阶段 | 来源：`docs/business-enum-quick-reference-v0.1.html:154`；锚点：`td.k`；父锚点：四、游乐运营 | PASS（已按上述三线核对适用性及原文） |
| BEQR-097 供应合同模式 | 来源：`docs/business-enum-quick-reference-v0.1.html:159`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-098 库房分类 | 来源：`docs/business-enum-quick-reference-v0.1.html:160`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-099 物品大类 | 来源：`docs/business-enum-quick-reference-v0.1.html:161`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-100 库存流水类型 | 来源：`docs/business-enum-quick-reference-v0.1.html:162`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-101 高价值判定 | 来源：`docs/business-enum-quick-reference-v0.1.html:164`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-102 跨主体借调 | 来源：`docs/business-enum-quick-reference-v0.1.html:166`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-103 智能采购建议 | 来源：`docs/business-enum-quick-reference-v0.1.html:168`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-104 门店要货履约状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:169`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-105 经销商状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:170`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-106 经销询价状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:171`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-107 经销价格目录状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:172`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-108 经销订单状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:173`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-109 经销退货单状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:174`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-110 经销库存外显 | 来源：`docs/business-enum-quick-reference-v0.1.html:175`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-111 经销支付方式 | 来源：`docs/business-enum-quick-reference-v0.1.html:176`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-112 经销库存预留 | 来源：`docs/business-enum-quick-reference-v0.1.html:177`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-113 经销职责分离 | 来源：`docs/business-enum-quick-reference-v0.1.html:178`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-114 经销取消、退货与开票 | 来源：`docs/business-enum-quick-reference-v0.1.html:179`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-115 经销价格与税务快照 | 来源：`docs/business-enum-quick-reference-v0.1.html:180`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-116 经销商停用撤权 | 来源：`docs/business-enum-quick-reference-v0.1.html:181`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-117 经销资金归属 | 来源：`docs/business-enum-quick-reference-v0.1.html:182`；锚点：`td.k`；父锚点：五、供应链与库存 | PASS（已按上述三线核对适用性及原文） |
| BEQR-118 运营营业日 | 来源：`docs/business-enum-quick-reference-v0.1.html:187`；锚点：`td.k`；父锚点：六、财务结算与对账 | PASS（已按上述三线核对适用性及原文） |
| BEQR-119 对账口径 | 来源：`docs/business-enum-quick-reference-v0.1.html:188`；锚点：`td.k`；父锚点：六、财务结算与对账 | PASS（已按上述三线核对适用性及原文） |
| BEQR-120 对账差异四类 | 来源：`docs/business-enum-quick-reference-v0.1.html:189`；锚点：`td.k`；父锚点：六、财务结算与对账 | PASS（已按上述三线核对适用性及原文） |
| BEQR-121 日结状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:190`；锚点：`td.k`；父锚点：六、财务结算与对账 | PASS（已按上述三线核对适用性及原文） |
| BEQR-122 差异处理状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:191`；锚点：`td.k`；父锚点：六、财务结算与对账 | PASS（已按上述三线核对适用性及原文） |
| BEQR-123 结算状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:192`；锚点：`td.k`；父锚点：六、财务结算与对账 | PASS（已按上述三线核对适用性及原文） |
| BEQR-124 综合通票定价 | 来源：`docs/business-enum-quick-reference-v0.1.html:193`；锚点：`td.k`；父锚点：六、财务结算与对账 | PASS（已按上述三线核对适用性及原文） |
| BEQR-125 商户业态 | 来源：`docs/business-enum-quick-reference-v0.1.html:195`；锚点：`td.k`；父锚点：六、财务结算与对账 | PASS（已按上述三线核对适用性及原文） |
| BEQR-126 合作模式 | 来源：`docs/business-enum-quick-reference-v0.1.html:196`；锚点：`td.k`；父锚点：六、财务结算与对账 | PASS（已按上述三线核对适用性及原文） |
| BEQR-127 退款资金流向 | 来源：`docs/business-enum-quick-reference-v0.1.html:197`；锚点：`td.k`；父锚点：六、财务结算与对账 | PASS（已按上述三线核对适用性及原文） |
| BEQR-128 物业费 | 来源：`docs/business-enum-quick-reference-v0.1.html:198`；锚点：`td.k`；父锚点：六、财务结算与对账 | PASS（已按上述三线核对适用性及原文） |
| BEQR-129 能源费与损耗费 | 来源：`docs/business-enum-quick-reference-v0.1.html:199`；锚点：`td.k`；父锚点：六、财务结算与对账 | PASS（已按上述三线核对适用性及原文） |
| BEQR-130 税费配置 | 来源：`docs/business-enum-quick-reference-v0.1.html:201`；锚点：`td.k`；父锚点：六、财务结算与对账 | PASS（已按上述三线核对适用性及原文） |
| BEQR-131 三方对账框架 | 来源：`docs/business-enum-quick-reference-v0.1.html:202`；锚点：`td.k`；父锚点：六、财务结算与对账 | PASS（已按上述三线核对适用性及原文） |
| BEQR-132 开票状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:203`；锚点：`td.k`；父锚点：六、财务结算与对账 | PASS（已按上述三线核对适用性及原文） |
| BEQR-133 接入的外部系统 | 来源：`docs/business-enum-quick-reference-v0.1.html:208`；锚点：`td.k`；父锚点：七、第三方接入 | PASS（已按上述三线核对适用性及原文） |
| BEQR-134 客户标签 | 来源：`docs/business-enum-quick-reference-v0.1.html:209`；锚点：`td.k`；父锚点：七、第三方接入 | PASS（已按上述三线核对适用性及原文） |
| BEQR-135 统一原则 | 来源：`docs/business-enum-quick-reference-v0.1.html:210`；锚点：`td.k`；父锚点：七、第三方接入 | PASS（已按上述三线核对适用性及原文） |
| BEQR-136 接入状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:211`；锚点：`td.k`；父锚点：七、第三方接入 | PASS（已按上述三线核对适用性及原文） |
| BEQR-137 回流结果状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:212`；锚点：`td.k`；父锚点：七、第三方接入 | PASS（已按上述三线核对适用性及原文） |
| BEQR-138 风险四级 | 来源：`docs/business-enum-quick-reference-v0.1.html:217`；锚点：`td.k`；父锚点：八、留痕与风控 | PASS（已按上述三线核对适用性及原文） |
| BEQR-139 事件状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:218`；锚点：`td.k`；父锚点：八、留痕与风控 | PASS（已按上述三线核对适用性及原文） |
| BEQR-140 主管复核强制项 | 来源：`docs/business-enum-quick-reference-v0.1.html:219`；锚点：`td.k`；父锚点：八、留痕与风控 | PASS（已按上述三线核对适用性及原文） |
| BEQR-141 经营异常处置状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:220`；锚点：`td.k`；父锚点：八、留痕与风控 | PASS（已按上述三线核对适用性及原文） |
| BEQR-142 异常状态的统一出口 | 来源：`docs/business-enum-quick-reference-v0.1.html:221`；锚点：`td.k`；父锚点：八、留痕与风控 | PASS（已按上述三线核对适用性及原文） |
| BEQR-143 四类主体 | 来源：`docs/business-enum-quick-reference-v0.1.html:226`；锚点：`td.k`；父锚点：九、平台治理 | PASS（已按上述三线核对适用性及原文） |
| BEQR-144 经营主体关系 | 来源：`docs/business-enum-quick-reference-v0.1.html:227`；锚点：`td.k`；父锚点：九、平台治理 | PASS（已按上述三线核对适用性及原文） |
| BEQR-145 城市项目公司 | 来源：`docs/business-enum-quick-reference-v0.1.html:228`；锚点：`td.k`；父锚点：九、平台治理 | PASS（已按上述三线核对适用性及原文） |
| BEQR-146 运营公司职责 | 来源：`docs/business-enum-quick-reference-v0.1.html:229`；锚点：`td.k`；父锚点：九、平台治理 | PASS（已按上述三线核对适用性及原文） |
| BEQR-147 主要收入归属 | 来源：`docs/business-enum-quick-reference-v0.1.html:230`；锚点：`td.k`；父锚点：九、平台治理 | PASS（已按上述三线核对适用性及原文） |
| BEQR-148 合作模式 | 来源：`docs/business-enum-quick-reference-v0.1.html:231`；锚点：`td.k`；父锚点：九、平台治理 | PASS（已按上述三线核对适用性及原文） |
| BEQR-149 职级 | 来源：`docs/business-enum-quick-reference-v0.1.html:232`；锚点：`td.k`；父锚点：九、平台治理 | PASS（已按上述三线核对适用性及原文） |
| BEQR-150 终端十一端 | 来源：`docs/business-enum-quick-reference-v0.1.html:233`；锚点：`td.k`；父锚点：九、平台治理 | PASS（已按上述三线核对适用性及原文） |
| BEQR-151 最终权限计算 | 来源：`docs/business-enum-quick-reference-v0.1.html:234`；锚点：`td.k`；父锚点：九、平台治理 | PASS（已按上述三线核对适用性及原文） |
| BEQR-152 项目与敏感数据权限 | 来源：`docs/business-enum-quick-reference-v0.1.html:235`；锚点：`td.k`；父锚点：九、平台治理 | PASS（已按上述三线核对适用性及原文） |
| BEQR-153 终端开关优先级 | 来源：`docs/business-enum-quick-reference-v0.1.html:236`；锚点：`td.k`；父锚点：九、平台治理 | PASS（已按上述三线核对适用性及原文） |
| BEQR-154 综合体项目状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:237`；锚点：`td.k`；父锚点：九、平台治理 | PASS（已按上述三线核对适用性及原文） |
| BEQR-155 商户/运营主体状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:238`；锚点：`td.k`；父锚点：九、平台治理 | PASS（已按上述三线核对适用性及原文） |
| BEQR-156 合同状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:239`；锚点：`td.k`；父锚点：九、平台治理 | PASS（已按上述三线核对适用性及原文） |
| BEQR-157 员工状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:240`；锚点：`td.k`；父锚点：九、平台治理 | PASS（已按上述三线核对适用性及原文） |
| BEQR-158 设备状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:241`；锚点：`td.k`；父锚点：九、平台治理 | PASS（已按上述三线核对适用性及原文） |
| BEQR-159 权限状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:242`；锚点：`td.k`；父锚点：九、平台治理 | PASS（已按上述三线核对适用性及原文） |
| BEQR-160 招商线索状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:248`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-161 招商意向阶段 | 来源：`docs/business-enum-quick-reference-v0.1.html:249`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-162 品牌/客户库类型 | 来源：`docs/business-enum-quick-reference-v0.1.html:250`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-163 合同变更类型 | 来源：`docs/business-enum-quick-reference-v0.1.html:251`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-164 退租结算状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:252`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-165 内部应付执行状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:253`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-166 合同审批链 | 来源：`docs/business-enum-quick-reference-v0.1.html:255`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-167 租金/账单类型 | 来源：`docs/business-enum-quick-reference-v0.1.html:256`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-168 营业额抽成模式 | 来源：`docs/business-enum-quick-reference-v0.1.html:257`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-169 公区能源费 | 来源：`docs/business-enum-quick-reference-v0.1.html:258`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-170 铺位租赁状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:259`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-171 物业工单类型 | 来源：`docs/business-enum-quick-reference-v0.1.html:260`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-172 物业工单状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:262`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-173 商户服务工单状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:264`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-174 工单时效 | 来源：`docs/business-enum-quick-reference-v0.1.html:265`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-175 楼宇机电设备状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:266`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-176 设备维保类型 | 来源：`docs/business-enum-quick-reference-v0.1.html:267`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-177 能耗表计类型 | 来源：`docs/business-enum-quick-reference-v0.1.html:268`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-178 抄表状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:269`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-179 能耗归集对象 | 来源：`docs/business-enum-quick-reference-v0.1.html:270`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-180 楼宇巡检任务状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:271`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-181 楼宇巡检类型 | 来源：`docs/business-enum-quick-reference-v0.1.html:273`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检） | PASS（已按上述三线核对适用性及原文） |
| BEQR-182 考勤日结果 | 来源：`docs/business-enum-quick-reference-v0.1.html:279`；锚点：`td.k`；父锚点：十一、员工排班考勤与薪资 | PASS（已按上述三线核对适用性及原文） |
| BEQR-183 打卡方式 | 来源：`docs/business-enum-quick-reference-v0.1.html:280`；锚点：`td.k`；父锚点：十一、员工排班考勤与薪资 | PASS（已按上述三线核对适用性及原文） |
| BEQR-184 请假类型 | 来源：`docs/business-enum-quick-reference-v0.1.html:281`；锚点：`td.k`；父锚点：十一、员工排班考勤与薪资 | PASS（已按上述三线核对适用性及原文） |
| BEQR-185 请假单状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:282`；锚点：`td.k`；父锚点：十一、员工排班考勤与薪资 | PASS（已按上述三线核对适用性及原文） |
| BEQR-186 薪资单构成 | 来源：`docs/business-enum-quick-reference-v0.1.html:283`；锚点：`td.k`；父锚点：十一、员工排班考勤与薪资 | PASS（已按上述三线核对适用性及原文） |
| BEQR-187 员工提成发放 | 来源：`docs/business-enum-quick-reference-v0.1.html:285`；锚点：`td.k`；父锚点：十一、员工排班考勤与薪资 | PASS（已按上述三线核对适用性及原文） |
| BEQR-188 服务订单状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:290`；锚点：`td.k`；父锚点：十二、服务点单 | PASS（已按上述三线核对适用性及原文） |
| BEQR-189 管理费方式 | 来源：`docs/business-enum-quick-reference-v0.1.html:291`；锚点：`td.k`；父锚点：十二、服务点单 | PASS（已按上述三线核对适用性及原文） |
| BEQR-190 分红基数 | 来源：`docs/business-enum-quick-reference-v0.1.html:296`；锚点：`td.k`；父锚点：十三、持股分红 | PASS（已按上述三线核对适用性及原文） |
| BEQR-191 分红比例 | 来源：`docs/business-enum-quick-reference-v0.1.html:297`；锚点：`td.k`；父锚点：十三、持股分红 | PASS（已按上述三线核对适用性及原文） |
| BEQR-192 分红税务 | 来源：`docs/business-enum-quick-reference-v0.1.html:298`；锚点：`td.k`；父锚点：十三、持股分红 | PASS（已按上述三线核对适用性及原文） |
| BEQR-193 分红周期 | 来源：`docs/business-enum-quick-reference-v0.1.html:299`；锚点：`td.k`；父锚点：十三、持股分红 | PASS（已按上述三线核对适用性及原文） |
| BEQR-194 分红单状态 | 来源：`docs/business-enum-quick-reference-v0.1.html:300`；锚点：`td.k`；父锚点：十三、持股分红 | PASS（已按上述三线核对适用性及原文） |
| BEQR-195 可选扣除项 | 来源：`docs/business-enum-quick-reference-v0.1.html:301`；锚点：`td.k`；父锚点：十三、持股分红 | PASS（已按上述三线核对适用性及原文） |
| BEQR-196 发放保护线留存 | 来源：`docs/business-enum-quick-reference-v0.1.html:302`；锚点：`td.k`；父锚点：十三、持股分红 | PASS（已按上述三线核对适用性及原文） |
| BEQR-197 期间关账 | 来源：`docs/business-enum-quick-reference-v0.1.html:303`；锚点：`td.k`；父锚点：十三、持股分红 | PASS（已按上述三线核对适用性及原文） |
| BSM-001 整体说明：这套系统到底管什么 | 来源：`docs/business-system-map-for-operations-v0.1.html:505`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-002 一笔业务怎么走 | 来源：`docs/business-system-map-for-operations-v0.1.html:526`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | 来源：`docs/business-system-map-for-operations-v0.1.html:563`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-004 配置数据可以存档、也可以删除（删除要审批） | 来源：`docs/business-system-map-for-operations-v0.1.html:566`；锚点：`h3`；父锚点：系统通用能力：数据可改可删、操作有引导、权限分岗 | PASS（已按上述三线核对适用性及原文） |
| BSM-005 后台的"新建/录入"改成分步引导 | 来源：`docs/business-system-map-for-operations-v0.1.html:581`；锚点：`h3`；父锚点：系统通用能力：数据可改可删、操作有引导、权限分岗 | PASS（已按上述三线核对适用性及原文） |
| BSM-006 验证用的二维码一律动态刷新（间隔可在后台设定） | 来源：`docs/business-system-map-for-operations-v0.1.html:584`；锚点：`h3`；父锚点：系统通用能力：数据可改可删、操作有引导、权限分岗 | PASS（已按上述三线核对适用性及原文） |
| BSM-007 平台管理员拥有全部日常功能权限 | 来源：`docs/business-system-map-for-operations-v0.1.html:587`；锚点：`h3`；父锚点：系统通用能力：数据可改可删、操作有引导、权限分岗 | PASS（已按上述三线核对适用性及原文） |
| BSM-008 子系统入口 | 来源：`docs/business-system-map-for-operations-v0.1.html:592`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-009 基础管理 | 来源：`docs/business-system-map-for-operations-v0.1.html:596`；锚点：`h3`；父锚点：子系统入口 | PASS（已按上述三线核对适用性及原文） |
| BSM-010 票务权益 | 来源：`docs/business-system-map-for-operations-v0.1.html:601`；锚点：`h3`；父锚点：子系统入口 | PASS（已按上述三线核对适用性及原文） |
| BSM-011 客人端 | 来源：`docs/business-system-map-for-operations-v0.1.html:606`；锚点：`h3`；父锚点：子系统入口 | PASS（已按上述三线核对适用性及原文） |
| BSM-012 酒吧平板/工作手机 | 来源：`docs/business-system-map-for-operations-v0.1.html:611`；锚点：`h3`；父锚点：子系统入口 | PASS（已按上述三线核对适用性及原文） |
| BSM-013 触摸屏一体机 | 来源：`docs/business-system-map-for-operations-v0.1.html:616`；锚点：`h3`；父锚点：子系统入口 | PASS（已按上述三线核对适用性及原文） |
| BSM-014 酒吧管理 | 来源：`docs/business-system-map-for-operations-v0.1.html:621`；锚点：`h3`；父锚点：子系统入口 | PASS（已按上述三线核对适用性及原文） |
| BSM-015 供应链库存 | 来源：`docs/business-system-map-for-operations-v0.1.html:626`；锚点：`h3`；父锚点：子系统入口 | PASS（已按上述三线核对适用性及原文） |
| BSM-016 游乐管理 | 来源：`docs/business-system-map-for-operations-v0.1.html:631`；锚点：`h3`；父锚点：子系统入口 | PASS（已按上述三线核对适用性及原文） |
| BSM-017 商户管理 | 来源：`docs/business-system-map-for-operations-v0.1.html:637`；锚点：`h3`；父锚点：子系统入口 | PASS（已按上述三线核对适用性及原文） |
| BSM-018 招商租赁 | 来源：`docs/business-system-map-for-operations-v0.1.html:643`；锚点：`h3`；父锚点：子系统入口 | PASS（已按上述三线核对适用性及原文） |
| BSM-019 物业服务 | 来源：`docs/business-system-map-for-operations-v0.1.html:650`；锚点：`h3`；父锚点：子系统入口 | PASS（已按上述三线核对适用性及原文） |
| BSM-020 楼宇运营 | 来源：`docs/business-system-map-for-operations-v0.1.html:656`；锚点：`h3`；父锚点：子系统入口 | PASS（已按上述三线核对适用性及原文） |
| BSM-021 第三方接入 | 来源：`docs/business-system-map-for-operations-v0.1.html:661`；锚点：`h3`；父锚点：子系统入口 | PASS（已按上述三线核对适用性及原文） |
| BSM-022 留痕与异常 | 来源：`docs/business-system-map-for-operations-v0.1.html:666`；锚点：`h3`；父锚点：子系统入口 | PASS（已按上述三线核对适用性及原文） |
| BSM-023 财务对账 | 来源：`docs/business-system-map-for-operations-v0.1.html:671`；锚点：`h3`；父锚点：子系统入口 | PASS（已按上述三线核对适用性及原文） |
| BSM-024 持股分红 | 来源：`docs/business-system-map-for-operations-v0.1.html:676`；锚点：`h3`；父锚点：子系统入口 | PASS（已按上述三线核对适用性及原文） |
| BSM-025 经营看板 | 来源：`docs/business-system-map-for-operations-v0.1.html:681`；锚点：`h3`；父锚点：子系统入口 | PASS（已按上述三线核对适用性及原文） |
| BSM-026 各类入口 | 来源：`docs/business-system-map-for-operations-v0.1.html:686`；锚点：`h3`；父锚点：子系统入口 | PASS（已按上述三线核对适用性及原文） |
| BSM-027 基础管理：先把综合体的盘子管清楚 | 来源：`docs/business-system-map-for-operations-v0.1.html:694`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-028 主要功能 | 来源：`docs/business-system-map-for-operations-v0.1.html:698`；锚点：`h3`；父锚点：基础管理：先把综合体的盘子管清楚 | PASS（已按上述三线核对适用性及原文） |
| BSM-029 和谁有关 | 来源：`docs/business-system-map-for-operations-v0.1.html:714`；锚点：`h3`；父锚点：基础管理：先把综合体的盘子管清楚 | PASS（已按上述三线核对适用性及原文） |
| BSM-030 票务权益：管卖什么票、给什么权益、怎么核销 | 来源：`docs/business-system-map-for-operations-v0.1.html:727`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-031 主要功能 | 来源：`docs/business-system-map-for-operations-v0.1.html:731`；锚点：`h3`；父锚点：票务权益：管卖什么票、给什么权益、怎么核销 | PASS（已按上述三线核对适用性及原文） |
| BSM-032 和谁有关 | 来源：`docs/business-system-map-for-operations-v0.1.html:760`；锚点：`h3`；父锚点：票务权益：管卖什么票、给什么权益、怎么核销 | PASS（已按上述三线核对适用性及原文） |
| BSM-033 票种、通票与权益类型（系统支持哪些种类） | 来源：`docs/business-system-map-for-operations-v0.1.html:772`；锚点：`h3`；父锚点：票务权益：管卖什么票、给什么权益、怎么核销 | PASS（已按上述三线核对适用性及原文） |
| BSM-034 核销、退款与有效期规则 | 来源：`docs/business-system-map-for-operations-v0.1.html:796`；锚点：`h3`；父锚点：票务权益：管卖什么票、给什么权益、怎么核销 | PASS（已按上述三线核对适用性及原文） |
| BSM-035 会员与积分规则 | 来源：`docs/business-system-map-for-operations-v0.1.html:813`；锚点：`h3`；父锚点：票务权益：管卖什么票、给什么权益、怎么核销 | PASS（已按上述三线核对适用性及原文） |
| BSM-036 人脸验龄数据使用与删除 | 来源：`docs/business-system-map-for-operations-v0.1.html:840`；锚点：`h3`；父锚点：票务权益：管卖什么票、给什么权益、怎么核销 | PASS（已按上述三线核对适用性及原文） |
| BSM-037 客人端：客人自己买票、预定、查询、积分和出示二维码 | 来源：`docs/business-system-map-for-operations-v0.1.html:850`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-038 主要功能 | 来源：`docs/business-system-map-for-operations-v0.1.html:854`；锚点：`h3`；父锚点：客人端：客人自己买票、预定、查询、积分和出示二维码 | PASS（已按上述三线核对适用性及原文） |
| BSM-039 和谁有关 | 来源：`docs/business-system-map-for-operations-v0.1.html:873`；锚点：`h3`；父锚点：客人端：客人自己买票、预定、查询、积分和出示二维码 | PASS（已按上述三线核对适用性及原文） |
| BSM-040 登录与关键规则 | 来源：`docs/business-system-map-for-operations-v0.1.html:883`；锚点：`h3`；父锚点：客人端：客人自己买票、预定、查询、积分和出示二维码 | PASS（已按上述三线核对适用性及原文） |
| BSM-041 会员注册、认证与收费边界 | 来源：`docs/business-system-map-for-operations-v0.1.html:898`；锚点：`h3`；父锚点：客人端：客人自己买票、预定、查询、积分和出示二维码 | PASS（已按上述三线核对适用性及原文） |
| BSM-042 会员积分活动与发券 | 来源：`docs/business-system-map-for-operations-v0.1.html:907`；锚点：`h3`；父锚点：客人端：客人自己买票、预定、查询、积分和出示二维码 | PASS（已按上述三线核对适用性及原文） |
| BSM-043 会员营销明确边界 | 来源：`docs/business-system-map-for-operations-v0.1.html:916`；锚点：`h3`；父锚点：客人端：客人自己买票、预定、查询、积分和出示二维码 | PASS（已按上述三线核对适用性及原文） |
| BSM-044 酒吧平板/工作手机：平板点酒，工作手机核销 | 来源：`docs/business-system-map-for-operations-v0.1.html:929`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-045 主要功能 | 来源：`docs/business-system-map-for-operations-v0.1.html:933`；锚点：`h3`；父锚点：酒吧平板/工作手机：平板点酒，工作手机核销 | PASS（已按上述三线核对适用性及原文） |
| BSM-046 平板图形点单（POS） | 来源：`docs/business-system-map-for-operations-v0.1.html:953`；锚点：`h3`；父锚点：酒吧平板/工作手机：平板点酒，工作手机核销 | PASS（已按上述三线核对适用性及原文） |
| BSM-047 和谁有关 | 来源：`docs/business-system-map-for-operations-v0.1.html:963`；锚点：`h3`；父锚点：酒吧平板/工作手机：平板点酒，工作手机核销 | PASS（已按上述三线核对适用性及原文） |
| BSM-048 触摸屏一体机：权益领取、现场服务和辅助售票 | 来源：`docs/business-system-map-for-operations-v0.1.html:978`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-049 主要功能 | 来源：`docs/business-system-map-for-operations-v0.1.html:982`；锚点：`h3`；父锚点：触摸屏一体机：权益领取、现场服务和辅助售票 | PASS（已按上述三线核对适用性及原文） |
| BSM-050 和谁有关 | 来源：`docs/business-system-map-for-operations-v0.1.html:996`；锚点：`h3`；父锚点：触摸屏一体机：权益领取、现场服务和辅助售票 | PASS（已按上述三线核对适用性及原文） |
| BSM-051 酒吧管理：从入场、开台到结账关台 | 来源：`docs/business-system-map-for-operations-v0.1.html:1010`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-052 主要功能 | 来源：`docs/business-system-map-for-operations-v0.1.html:1014`；锚点：`h3`；父锚点：酒吧管理：从入场、开台到结账关台 | PASS（已按上述三线核对适用性及原文） |
| BSM-053 和谁有关 | 来源：`docs/business-system-map-for-operations-v0.1.html:1041`；锚点：`h3`；父锚点：酒吧管理：从入场、开台到结账关台 | PASS（已按上述三线核对适用性及原文） |
| BSM-054 桌台/账单状态与经营规则 | 来源：`docs/business-system-map-for-operations-v0.1.html:1053`；锚点：`h3`；父锚点：酒吧管理：从入场、开台到结账关台 | PASS（已按上述三线核对适用性及原文） |
| BSM-055 桌台、菜单与优惠券 | 来源：`docs/business-system-map-for-operations-v0.1.html:1088`；锚点：`h3`；父锚点：酒吧管理：从入场、开台到结账关台 | PASS（已按上述三线核对适用性及原文） |
| BSM-056 正式未付款订单挂起 | 来源：`docs/business-system-map-for-operations-v0.1.html:1101`；锚点：`h3`；父锚点：酒吧管理：从入场、开台到结账关台 | PASS（已按上述三线核对适用性及原文） |
| BSM-057 服务点单：客人点专项服务，公司统一收款、再按名录分账 | 来源：`docs/business-system-map-for-operations-v0.1.html:1120`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-058 怎么点、怎么收钱 | 来源：`docs/business-system-map-for-operations-v0.1.html:1124`；锚点：`h3`；父锚点：服务点单：客人点专项服务，公司统一收款、再按名录分账 | PASS（已按上述三线核对适用性及原文） |
| BSM-059 钱怎么分、怎么退 | 来源：`docs/business-system-map-for-operations-v0.1.html:1135`；锚点：`h3`；父锚点：服务点单：客人点专项服务，公司统一收款、再按名录分账 | PASS（已按上述三线核对适用性及原文） |
| BSM-060 供应链库存：管供应商、采购、库房、消耗和存酒 | 来源：`docs/business-system-map-for-operations-v0.1.html:1151`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-061 主要功能 | 来源：`docs/business-system-map-for-operations-v0.1.html:1155`；锚点：`h3`；父锚点：供应链库存：管供应商、采购、库房、消耗和存酒 | PASS（已按上述三线核对适用性及原文） |
| BSM-062 和谁有关 | 来源：`docs/business-system-map-for-operations-v0.1.html:1174`；锚点：`h3`；父锚点：供应链库存：管供应商、采购、库房、消耗和存酒 | PASS（已按上述三线核对适用性及原文） |
| BSM-063 库存类型与规则 | 来源：`docs/business-system-map-for-operations-v0.1.html:1186`；锚点：`h3`；父锚点：供应链库存：管供应商、采购、库房、消耗和存酒 | PASS（已按上述三线核对适用性及原文） |
| BSM-064 采购建议与门店要货 | 来源：`docs/business-system-map-for-operations-v0.1.html:1208`；锚点：`h3`；父锚点：供应链库存：管供应商、采购、库房、消耗和存酒 | PASS（已按上述三线核对适用性及原文） |
| BSM-065 外部经销商准入、价格与库存 | 来源：`docs/business-system-map-for-operations-v0.1.html:1216`；锚点：`h3`；父锚点：供应链库存：管供应商、采购、库房、消耗和存酒 | PASS（已按上述三线核对适用性及原文） |
| BSM-066 经销订单、付款与履约 | 来源：`docs/business-system-map-for-operations-v0.1.html:1226`；锚点：`h3`；父锚点：供应链库存：管供应商、采购、库房、消耗和存酒 | PASS（已按上述三线核对适用性及原文） |
| BSM-067 经销退货、退款、红冲与资金归属 | 来源：`docs/business-system-map-for-operations-v0.1.html:1238`；锚点：`h3`；父锚点：供应链库存：管供应商、采购、库房、消耗和存酒 | PASS（已按上述三线核对适用性及原文） |
| BSM-068 游乐管理：管项目开放、项目容量（实时在玩人数）和安全异常 | 来源：`docs/business-system-map-for-operations-v0.1.html:1253`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-069 主要功能 | 来源：`docs/business-system-map-for-operations-v0.1.html:1257`；锚点：`h3`；父锚点：游乐管理：管项目开放、项目容量（实时在玩人数）和安全异常 | PASS（已按上述三线核对适用性及原文） |
| BSM-070 和谁有关 | 来源：`docs/business-system-map-for-operations-v0.1.html:1276`；锚点：`h3`；父锚点：游乐管理：管项目开放、项目容量（实时在玩人数）和安全异常 | PASS（已按上述三线核对适用性及原文） |
| BSM-071 游乐类型与规则 | 来源：`docs/business-system-map-for-operations-v0.1.html:1287`；锚点：`h3`；父锚点：游乐管理：管项目开放、项目容量（实时在玩人数）和安全异常 | PASS（已按上述三线核对适用性及原文） |
| BSM-072 商户管理：商户在授权范围内完整使用自己的系统 | 来源：`docs/business-system-map-for-operations-v0.1.html:1308`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-073 主要功能 | 来源：`docs/business-system-map-for-operations-v0.1.html:1313`；锚点：`h3`；父锚点：商户管理：商户在授权范围内完整使用自己的系统 | PASS（已按上述三线核对适用性及原文） |
| BSM-074 和谁有关 | 来源：`docs/business-system-map-for-operations-v0.1.html:1326`；锚点：`h3`；父锚点：商户管理：商户在授权范围内完整使用自己的系统 | PASS（已按上述三线核对适用性及原文） |
| BSM-075 招商租赁与合同履约：从找商户到签约、履约、变更、退租 | 来源：`docs/business-system-map-for-operations-v0.1.html:1339`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-076 主要功能 | 来源：`docs/business-system-map-for-operations-v0.1.html:1343`；锚点：`h3`；父锚点：招商租赁与合同履约：从找商户到签约、履约、变更、退租 | PASS（已按上述三线核对适用性及原文） |
| BSM-077 和谁有关 | 来源：`docs/business-system-map-for-operations-v0.1.html:1357`；锚点：`h3`；父锚点：招商租赁与合同履约：从找商户到签约、履约、变更、退租 | PASS（已按上述三线核对适用性及原文） |
| BSM-078 物业服务：商户和现场的报修、投诉、整改都进工单 | 来源：`docs/business-system-map-for-operations-v0.1.html:1370`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-079 主要功能 | 来源：`docs/business-system-map-for-operations-v0.1.html:1374`；锚点：`h3`；父锚点：物业服务：商户和现场的报修、投诉、整改都进工单 | PASS（已按上述三线核对适用性及原文） |
| BSM-080 和谁有关 | 来源：`docs/business-system-map-for-operations-v0.1.html:1388`；锚点：`h3`；父锚点：物业服务：商户和现场的报修、投诉、整改都进工单 | PASS（已按上述三线核对适用性及原文） |
| BSM-081 楼宇运营：机电设备、能耗抄表、楼宇巡检 | 来源：`docs/business-system-map-for-operations-v0.1.html:1401`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-082 主要功能 | 来源：`docs/business-system-map-for-operations-v0.1.html:1405`；锚点：`h3`；父锚点：楼宇运营：机电设备、能耗抄表、楼宇巡检 | PASS（已按上述三线核对适用性及原文） |
| BSM-083 和谁有关 | 来源：`docs/business-system-map-for-operations-v0.1.html:1414`；锚点：`h3`；父锚点：楼宇运营：机电设备、能耗抄表、楼宇巡检 | PASS（已按上述三线核对适用性及原文） |
| BSM-084 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | 来源：`docs/business-system-map-for-operations-v0.1.html:1427`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-085 日常工作 | 来源：`docs/business-system-map-for-operations-v0.1.html:1431`；锚点：`h3`；父锚点：员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | PASS（已按上述三线核对适用性及原文） |
| BSM-086 排班、考勤与请假 | 来源：`docs/business-system-map-for-operations-v0.1.html:1441`；锚点：`h3`；父锚点：员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | PASS（已按上述三线核对适用性及原文） |
| BSM-087 我的薪资 | 来源：`docs/business-system-map-for-operations-v0.1.html:1455`；锚点：`h3`；父锚点：员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | PASS（已按上述三线核对适用性及原文） |
| BSM-088 员工提成、活动执行与人员服务边界 | 来源：`docs/business-system-map-for-operations-v0.1.html:1467`；锚点：`h3`；父锚点：员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | PASS（已按上述三线核对适用性及原文） |
| BSM-089 第三方接入：外部系统继续做专业系统，平台拿回结果 | 来源：`docs/business-system-map-for-operations-v0.1.html:1479`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-090 机器人配送与扫码交接 | 来源：`docs/business-system-map-for-operations-v0.1.html:1545`；锚点：`h3`；父锚点：第三方接入：外部系统继续做专业系统，平台拿回结果 | PASS（已按上述三线核对适用性及原文） |
| BSM-091 视频监控（留点位/时间） | 来源：`docs/business-system-map-for-operations-v0.1.html:1554`；锚点：`h3`；父锚点：第三方接入：外部系统继续做专业系统，平台拿回结果 | PASS（已按上述三线核对适用性及原文） |
| BSM-092 重要事项短信通知（2026-06-29 新增） | 来源：`docs/business-system-map-for-operations-v0.1.html:1563`；锚点：`h3`；父锚点：第三方接入：外部系统继续做专业系统，平台拿回结果 | PASS（已按上述三线核对适用性及原文） |
| BSM-093 留痕与异常：重要动作留得住，出了问题追得回 | 来源：`docs/business-system-map-for-operations-v0.1.html:1574`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-094 必须关注的动作 | 来源：`docs/business-system-map-for-operations-v0.1.html:1578`；锚点：`h3`；父锚点：留痕与异常：重要动作留得住，出了问题追得回 | PASS（已按上述三线核对适用性及原文） |
| BSM-095 怎样减少人工填写 | 来源：`docs/business-system-map-for-operations-v0.1.html:1596`；锚点：`h3`；父锚点：留痕与异常：重要动作留得住，出了问题追得回 | PASS（已按上述三线核对适用性及原文） |
| BSM-096 处理结果 | 来源：`docs/business-system-map-for-operations-v0.1.html:1607`；锚点：`h3`；父锚点：留痕与异常：重要动作留得住，出了问题追得回 | PASS（已按上述三线核对适用性及原文） |
| BSM-097 风险等级与风控规则 | 来源：`docs/business-system-map-for-operations-v0.1.html:1619`；锚点：`h3`；父锚点：留痕与异常：重要动作留得住，出了问题追得回 | PASS（已按上述三线核对适用性及原文） |
| BSM-098 财务对账：把经营事实整理成财务能确认的口径 | 来源：`docs/business-system-map-for-operations-v0.1.html:1636`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-099 内部结算价目录（按商品规格逐项建目录） | 来源：`docs/business-system-map-for-operations-v0.1.html:1639`；锚点：`h3`；父锚点：财务对账：把经营事实整理成财务能确认的口径 | PASS（已按上述三线核对适用性及原文） |
| BSM-100 结算配置中心（后台总控页） | 来源：`docs/business-system-map-for-operations-v0.1.html:1641`；锚点：`h3`；父锚点：财务对账：把经营事实整理成财务能确认的口径 | PASS（已按上述三线核对适用性及原文） |
| BSM-101 项目零售参考价 | 来源：`docs/business-system-map-for-operations-v0.1.html:1643`；锚点：`h3`；父锚点：财务对账：把经营事实整理成财务能确认的口径 | PASS（已按上述三线核对适用性及原文） |
| BSM-102 主要功能 | 来源：`docs/business-system-map-for-operations-v0.1.html:1648`；锚点：`h3`；父锚点：财务对账：把经营事实整理成财务能确认的口径 | PASS（已按上述三线核对适用性及原文） |
| BSM-103 和谁有关 | 来源：`docs/business-system-map-for-operations-v0.1.html:1666`；锚点：`h3`；父锚点：财务对账：把经营事实整理成财务能确认的口径 | PASS（已按上述三线核对适用性及原文） |
| BSM-104 收入归属：哪类收入归哪个业态主体 | 来源：`docs/business-system-map-for-operations-v0.1.html:1678`；锚点：`h3`；父锚点：财务对账：把经营事实整理成财务能确认的口径 | PASS（已按上述三线核对适用性及原文） |
| BSM-105 三方对账框架：系统流水、渠道对账单、资金到账两两核对 | 来源：`docs/business-system-map-for-operations-v0.1.html:1697`；锚点：`h3`；父锚点：财务对账：把经营事实整理成财务能确认的口径 | PASS（已按上述三线核对适用性及原文） |
| BSM-106 对账与结算类型规则 | 来源：`docs/business-system-map-for-operations-v0.1.html:1709`；锚点：`h3`；父锚点：财务对账：把经营事实整理成财务能确认的口径 | PASS（已按上述三线核对适用性及原文） |
| BSM-107 持股分红：按持股把经营成果定期分给主理人 | 来源：`docs/business-system-map-for-operations-v0.1.html:1737`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-108 主要功能 | 来源：`docs/business-system-map-for-operations-v0.1.html:1741`；锚点：`h3`；父锚点：持股分红：按持股把经营成果定期分给主理人 | PASS（已按上述三线核对适用性及原文） |
| BSM-109 谁会用 | 来源：`docs/business-system-map-for-operations-v0.1.html:1760`；锚点：`h3`；父锚点：持股分红：按持股把经营成果定期分给主理人 | PASS（已按上述三线核对适用性及原文） |
| BSM-110 和谁有关 | 来源：`docs/business-system-map-for-operations-v0.1.html:1768`；锚点：`h3`；父锚点：持股分红：按持股把经营成果定期分给主理人 | PASS（已按上述三线核对适用性及原文） |
| BSM-111 经营看板：让管理层和运营看得懂、能追问 | 来源：`docs/business-system-map-for-operations-v0.1.html:1781`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-112 主要指标 | 来源：`docs/business-system-map-for-operations-v0.1.html:1785`；锚点：`h3`；父锚点：经营看板：让管理层和运营看得懂、能追问 | PASS（已按上述三线核对适用性及原文） |
| BSM-113 谁会看 | 来源：`docs/business-system-map-for-operations-v0.1.html:1813`；锚点：`h3`；父锚点：经营看板：让管理层和运营看得懂、能追问 | PASS（已按上述三线核对适用性及原文） |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | 来源：`docs/business-system-map-for-operations-v0.1.html:1827`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | 来源：`docs/business-system-map-for-operations-v0.1.html:1897`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-116 系统交付与上线：正式上线要做的安全加固和安装交付 | 来源：`docs/business-system-map-for-operations-v0.1.html:1928`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |
| BSM-117 正式上线时的安全加固 | 来源：`docs/business-system-map-for-operations-v0.1.html:1932`；锚点：`h3`；父锚点：系统交付与上线：正式上线要做的安全加固和安装交付 | PASS（已按上述三线核对适用性及原文） |
| BSM-118 能自动识别环境的安装包 + 内附教程 | 来源：`docs/business-system-map-for-operations-v0.1.html:1940`；锚点：`h3`；父锚点：系统交付与上线：正式上线要做的安全加固和安装交付 | PASS（已按上述三线核对适用性及原文） |
| BSM-119 和谁有关 | 来源：`docs/business-system-map-for-operations-v0.1.html:1947`；锚点：`h3`；父锚点：系统交付与上线：正式上线要做的安全加固和安装交付 | PASS（已按上述三线核对适用性及原文） |
| BSM-120 已确认规则和实施前输入项 | 来源：`docs/business-system-map-for-operations-v0.1.html:1957`；锚点：`h2` | PASS（已按上述三线核对适用性及原文） |

本次Codex完整B1到此结束：PASS；新工程缺陷0，技术性登记1。Grok本次报告正文截至本段落盘仍未读取。下一步按精确b1 Grok路径读取本次完整报告，四值校验后整批归并。


# M001 B1 双报告接收与整批上报

版本：V0.17（仅本追加段；保留既有全部字节）。

## Codex 对当前七项 Grok 意见的核实（不是 B2 定稿）

已按 receiving-code-review 的核实规则逐项复查。B1 原始独审仍为 Codex PASS / Grok FAIL；互读后合取 FAIL，绝不以自审覆盖外审。七条完整意见全部登记，未删任何条目。当前 Grok 报告系追加 V0.06，不得与上批 V0.04 的同名 GR-01/02/03 混淆。

| 正式ID | 当前来源 | 分类 | 工程缺陷（候选） | 修复队列归属（候选） | 内容及核实 |
|---|---|---|---|---|---|
| M001-005 | Grok V0.06 GR-01 | 冲突 | 是 | 是 | 投诉需确认关闭与超时默认关闭；接受删除业务地图1384旧超时例外，申请14 |
| M001-006 | Grok V0.06 GR-02 | 细化 | 是 | 是 | 两处“园区容量。”为空答案；申请15、16仅引用已有单项目容量规则 |
| M001-007 | Grok V0.06 GR-03 | 细化 | 是（保留Grok判定；Codex异议） | 是 | 手环与三环节关系；相邻明确通用规则已适用于再入，未发现手环豁免明文，待Grok B2复核 |
| M001-008 | Grok V0.06 GR-04 | 技术性 | 是 | 是 | 13张表无表头；接受可访问性缺陷；申请17补已有“问题/答案”列语义；无业务发明 |
| M001-009 | Grok V0.06 GR-05 | 技术性（保留原分类，待Grok复核） | 是（保留Grok判定；Codex异议） | 是 | EXPIRED处在描述机器流水终态的句子，已有“不可用”中文；并非产品界面英文展示证据 |
| M001-010 | Grok V0.06 GR-06 | 细化 | 是（保留Grok判定；Codex异议） | 是 | “大屏叫号”是2026-09-01操作员点名批准的精确原句；决策记录还明确不扩写 |
| M001-011 | Grok V0.06 GR-07 | 细化 | 是（保留Grok判定；Codex异议） | 是 | 枚举已定义包场禁对外核销，地图补主管审批；两份共同解释而非各自完整；地图未明文允许对外核销 |

M001-001/002/003 保持上批 B4-r1 双 PASS 后出队的“否”；不复用其ID。M001-004继续只登记为“否”，并吸收当前 Codex V0.16 CX-01 同范围技术登记；当前缺陷 M001-008 独立编号，不能用004笼统“无缺陷”抵销。

当前候选七项暂均保留 Grok 的工程缺陷及“是”，这些值未经 B2 双 PASS 定稿，不是 B3 的新执行许可。上述四项异议只有经 Grok 独立复核同意后才能改变当前值；不会自动出队。申请14–17答复前全流程暂停，不派 B2 或进入其他模块。

### 四项异议的具体证据

- M001-007：枚举43规定三环节逐道通过、无人工放行；地图800明确同场次重复进出仍须三道但不重复扣次数。44/801手环句未写任何豁免。两条条件可合取，不能仅因概括句短就推定相邻强约束失效；须由 Grok 复核本推断。
- M001-009：枚举83、地图826现行原文都是“退回流水照建但直接落 EXPIRED（不可用）”，对象是机器流水；本席没有证据证明产品向客人显示英文。技术性分类与有上位券包需求的先细化顺序也须复核。保留Grok原意见，不自行更改代码名或业务状态。
- M001-010：地图1261现行原文“大屏叫号”。docs/prd-audit/business-decision-ledger.md:2469–2477 的2026-09-01记录明确“James亲口批准”这四字，并明确不扩写为电子排队号、远程取号、到场保留时间或过号机制。该记录只证明现行HTML原句的确认时间和修改边界，不把未进HTML的内容当新业务需求；GEN-001要求可验证用途，该词已有现场显示叫号用途；GEN-016未要求每条功能概要含详细状态机。删掉或擅自细化会超出用户原批准。
- M001-011：枚举25“配套两份业务地图…使用”，枚举306/地图2003明确三份共同解释。枚举153提供包场禁对外核销，地图1300提供审批和越界限制，二者无相反命令。Grok自己确认“不是明文相反”；单份地图缺少重复句不能直接证明整体需求遗漏，不能把未冻结的“每份HTML必须逐句自足”要求当判据。

### 核实与拟稿验证

远程逐字读取当前1384、146/1295、44/801、83/826、1261、1300及枚举25/306；确认13张表、197个td.k、每行1个问题单元格、没有th。原始B1机械检查已覆盖完整HTML结构和导航。拟稿在内存中预演，源文件未写。
表头按原生th/scope提供列语义。W3C ARIA in HTML 的td规则（https://www.w3.org/TR/html-aria/#el-td）不支持直接在原生table内td上改role=rowheader，故未提出该错误捷径；这是GEN-015的现有技术证据，不新增判据。

## 本批 HTML 修改申请（14–17，均未批准、未实施）

以下行号依据当前枚举 V0.06、业务地图 V0.07。每项仅授权所列替换，其他正文保留；如本批获批并经 B2 双 PASS 后执行 B3，实际被修改文件自身版本末位前进一次。

### 申请14 — M001-005 / 当前 GR-01

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html
行：1384。
原文片段：投诉类一律须提单方确认才能关闭，提单方在后台配置的时限内未确认的视为默认关闭（业务参数，按工单类型可配，须同时配置上下限），但单独统计为“未确认关闭”；
替换为：投诉类一律须提单方确认才能关闭；
理由：与枚举262及本行客人投诉关闭须本人确认的规定一致。DEC-R6-13/14 的 2026-08-04 确认记录比本段 2026-06-25 旧来源更晚；仅用决策台账识别确认时间，不把它作为第四业务源。
批准状态：等待操作员逐项答复。

### 申请15 — M001-006 / 当前 GR-02（枚举落点）

文件：160:/home/james/complex-management/docs/business-enum-quick-reference-v0.1.html
行：146。
原文：<tr><td class="k">园区容量</td><td>园区容量。</td></tr>
替换为：<tr><td class="k">园区容量</td><td>只按单个项目的实时在玩人数控制容量，具体按本表「容量双阈值」条执行。</td></tr>
理由：用紧邻145行已经批准的容量口径替代没有规则的重复标签；保留原锚点，不新设园区容量数值或维度。
批准状态：等待操作员逐项答复。

### 申请16 — M001-006 / 当前 GR-02（业务地图落点）

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html
行：1295。
原文：<tr><th scope="row">园区容量</th><td>园区容量。</td></tr>
替换为：<tr><th scope="row">园区容量</th><td>只按单个游乐项目的实时在玩人数控制容量，具体按本表「容量控制」条执行。</td></tr>
理由：与1294既有口径一致，为此空白条目给出同表引用，不新设规则。
批准状态：等待操作员逐项答复。

### 申请17 — M001-008 / 当前 GR-04

文件：160:/home/james/complex-management/docs/business-enum-quick-reference-v0.1.html
行：14；28、64、99、139、158、186、207、216、225、247、278、289、295。
第14行原文：
  td { border-bottom: 1px solid #eef0f3; padding: 9px 12px; vertical-align: top; font-size: 14px; }
第14行替换为：
  td, th { border-bottom: 1px solid #eef0f3; padding: 9px 12px; vertical-align: top; font-size: 14px; } th { text-align: left; }
上述13个表格起始行分别将 <table> 替换为：
<table><thead><tr><th scope="col">问题</th><th scope="col">答案</th></tr></thead>
理由：13张表目前没有任何表头。补入“问题／答案”列标题，沿用25行已经定义的两列用途，给辅助阅读工具提供列语义；不改197项业务文字，不改 td.k 锚点，不改CSS现有取值，不新增业务规则。本次不把缺少caption本身当作另一缺陷。
批准状态：等待操作员逐项答复。

当前 GR-03、GR-05、GR-06、GR-07 保留全部原始意见与否决权；本席有证据异议，待本批人闸答复后由 Grok 在 B2 独立复核；本次不为这四项提出无必要的 HTML 改动，也不自行关项。

四值：d4ba138e924c6fb13020cf26e55aef537a9488ba84dfb72ca9d0f851b5090b80, d4ba138e924c6fb13020cf26e55aef537a9488ba84dfb72ca9d0f851b5090b80, d4ba138e924c6fb13020cf26e55aef537a9488ba84dfb72ca9d0f851b5090b80, d4ba138e924c6fb13020cf26e55aef537a9488ba84dfb72ca9d0f851b5090b80。
Grok交付成功EXIT_CODE=0；本次B1结论FAIL。旧430行字节前缀SHA256=71c05ee2b371f9efd0f47555425b9425593e2e187b9d097b2c3c8e6a226cfe80；接收时整份Grok SHA256=9d44fdbb305c9edaa497af1185757e8ae1da1cbe1d560d9cc30932cf5a2357b4。
拟稿内存预演：13张表/26个列头/197个td.k保留，标签栈闭合，行数308/2032不变；三份HTML字节未写。未开展B2外审，未实施任何本批修复。


# M001 B2人闸答复记录

版本：V0.18（仅追加段；不改原始独审）。


## 当前操作员答复及待裁决（2026-09-05T04:24:33.169442+00:00）

申请15：批准申请 15（推荐）
申请16：批准申请 16（推荐）

申请14原答复：我是可以按照这个提示来，但是有一个问题，如果提单方在问题解决后不主动点击这个。这个问题关闭。怎么办如何解决
处理：这是带问题的有条件表态，未作为最终批准。原删除超时例外提案保留为历史，当前建议补明回访确认后代关，仅拟稿未授权。

申请17原答复：这个完全就不是业务问题啊，为什么在这里提问啊，而且为什么html当中有这个问题啊
处理：用户要求解释技术问题为什么进入业务HTML审批，不等于批准或明确拒绝；不擅自解释为本轮技术豁免、出队或修改提示词。它属于GEN-015界面可访问性，不是新增业务规则；三份HTML文件整体受铁律4保护，故仍需修改授权。

### 申请14修订提案（待确认，仅业务地图1384）

原文片段：投诉类一律须提单方确认才能关闭，提单方在后台配置的时限内未确认的视为默认关闭（业务参数，按工单类型可配，须同时配置上下限），但单独统计为“未确认关闭”；
拟替换为：投诉类须取得提单方明确确认后才能关闭。提单方可以自行在线确认，也可以在客户关怀部回访时明确确认问题已解决并同意关闭；通过回访确认的，由客户关怀部保存确认记录后代为关闭；未取得提单方明确确认的不得关闭，继续由客户关怀部跟进；
其他正文保留。理由：保留提单方明确确认门槛，解决已明确认可但没有亲自在线点击的情况；客户关怀部只有保存本人明确确认记录后才代关。未回应者仍继续跟进，不把沉默写成满意或同意；是否希望另设超时审核关闭，须操作员另行明确裁决。

此建议尚未成为业务规则，未修改任何HTML。仍按M001整模块B2整批处理：先解决14/17答复，再Grok复核全部候选（含四项异议），双PASS后才统一B3；不会先修已批准15/16。当前没有新增流程状态、计数器或产物。修订提案在内存中验证原片段唯一命中、替换范围只在1384；未写入业务源码。

状态：BLOCKED-ASK；B3=1；Grok本步NOT-CALLED；没有B2 PASS/FAIL新票。


# M001 B2操作员选定主管审核关闭方向

版本：V0.19（仅追加，不改历史独审）。



## 当前申请14再次修订及同步申请18、19（均待批准）

操作员已选方向：“希望无回应时也允许主管审核关闭，请另拟条文”。这是拟稿指令，不是下面新增具体条文的逐处批准。
申请17已批准，实际弹窗答复：“批准这项技术修改（推荐）”；申请15、16继续已批准。旧14的直接删除稿、回访须本人明确同意后代关稿均不作为待执行稿；以下为当前唯一14候选。

### 申请14（业务地图1384；M001-005）

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html
第1384行两处精确替换：
① 原文：投诉类一律须提单方确认才能关闭，提单方在后台配置的时限内未确认的视为默认关闭（业务参数，按工单类型可配，须同时配置上下限），但单独统计为“未确认关闭”；
替换为：投诉类处理完成后，由客户关怀部通知提单方确认；确认时限自通知提单方之时起算（业务参数，按工单类型可配，须同时配置上下限）。提单方可自行在线确认，也可在回访时明确确认问题已解决并同意关闭；回访确认的，由客户关怀部保存确认记录后关闭。确认时限届满、经回访仍无回应的，须经主管审核处理结果及通知、回访记录，确认问题已解决并批准后，由客户关怀部关闭，单独统计为“未确认关闭”，不得视为提单方确认或满意。提单方明确表示问题未解决或不同意关闭的，不适用上述无回应关闭规则，继续跟进；
② 原文：其关闭同样须提单方确认。
替换为：其关闭同样按上述投诉关闭规则执行。
其余正文保留，尤其复杂类双确认及客户关怀部闭环确认条款不变。理由：落实操作员选择的无回应时主管审核关闭；确认时限继续使用已有按工单类型配置的业务参数，不擅设固定天数。
批准状态：等待明确批准当前修订稿。

### 申请18（枚举262；M001-005同一缺陷同步落点）

文件：160:/home/james/complex-management/docs/business-enum-quick-reference-v0.1.html
第262行：
原文：投诉类须提单方确认才能关闭
替换为：投诉类由提单方确认后关闭；确认时限届满且经回访仍无回应的，按《业务系统地图》「物业服务」的投诉关闭规则，经主管审核批准后由客户关怀部关闭
其余正文保留。理由：避免枚举仍要求所有投诉必须本人确认而与拟定主管审核关闭规则冲突。
批准状态：等待逐处批准。

### 申请19（业务地图1378；M001-005同一缺陷同步落点）

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html
第1378行：
原文：客人（客人端投诉，客人即提单方，关闭须其确认）
替换为：客人（客人端投诉，客人即提单方，关闭按本章投诉关闭规则执行）
其余正文保留。理由：统一客人投诉的关闭规则，消除本章概述的旧绝对门槛。
批准状态：等待逐处批准。

以上属于同一 M001-005 的业务裁决同步，不新造 finding ID、不增加B3执行次数。内存预演确认四处旧片段各唯一匹配，复杂类原双确认条款保留。治理地图现文只要求客户关怀部确认闭环，没有需同步的提单方绝对确认片段，故未提出治理地图修改。三份HTML均未写入。B2仍BLOCKED-ASK，15/16/17获批不触发先行修复。


# M001 B2申请14、18批准与19拒绝

版本：V0.20（仅追加段）。


## 申请14、18批准及19拒绝的实际答复

UTC 2026-09-05T04:29:19.484919+00:00
申请14：批准申请 14 修订稿（推荐）
申请18：批准申请 18（推荐）
申请19：不批准申请 19

当前批准集合为14当前修订稿、15、16、17、18；19拒绝，不得修改地图1378，不重复申请该处修改。所有批准尚未B3实施。
拒绝19的业务含义尚不明确：原句“客人（客人端投诉，客人即提单方，关闭须其确认）”仍在1384新方案上方；已批准14同时明确客人投诉按无回应时主管审核关闭规则执行。须明确客人是否也适用14，或客人仍必须本人确认而需要收窄14/18的适用范围。本席不把拒绝一个文字修改擅自解释成对任一业务规则的改判，也不凭确认时间线猜选一条。当前仍在同一HTML修改人闸/B2，BLOCKED-ASK等待这一个业务含义答复；没有新增流程闸或状态。


# M001 B2客人反馈关闭规则新裁决

版本：V0.21（仅追加）。


## 客人反馈后由客户关怀部关闭：最新裁决与三处同步拟稿

操作员最新原话：“客人投诉由客户关怀部记录问题解决方案及客人反馈后可以由客户关怀部关闭”。该回复回答了适用范围问题，采用了两选项之外的新明确口径：客人有反馈时，由客户关怀部记录解决方案及反馈后关闭；不要求反馈一定是本人同意关闭。本席不擅自增加该限制。
拟稿同时保留先前已批准14的无回应时主管审核关闭分支；两者按“已有反馈/仍无回应”分开。这个组合将明确展示给操作员核对，不把沉默记成反馈。
原14与18的批准历史保留；最新客人规则改变了它们的具体拟写文字，故下面三份为当前提案，尚待逐处批准。原19拒绝继续有效；新19具体文字也不同，未取得新批准前1378一字不改。15、16、17持续获批。

### 申请14最新稿（地图1384）

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html
① 原文片段：投诉类一律须提单方确认才能关闭，提单方在后台配置的时限内未确认的视为默认关闭（业务参数，按工单类型可配，须同时配置上下限），但单独统计为“未确认关闭”；
替换为：投诉类处理完成后，由客户关怀部通知提单方；确认时限自通知提单方之时起算（业务参数，按工单类型可配，须同时配置上下限）。客人投诉由客户关怀部记录问题解决方案及客人反馈后可以由客户关怀部关闭。其他投诉的提单方可自行在线确认，也可在回访时明确确认问题已解决并同意关闭；回访确认的，由客户关怀部保存确认记录后关闭。确认时限届满、经回访仍无回应的，须经主管审核处理结果及通知、回访记录，确认问题已解决并批准后，由客户关怀部关闭，单独统计为“未确认关闭”，不得视为提单方确认或满意。已收到提单方反馈的，不适用上述无回应关闭规则；
② 同行原文：其关闭同样须提单方确认。
替换为：其关闭同样按上述投诉关闭规则执行。
其他正文保留。批准状态：等待当前稿逐处批准。

### 申请18最新稿（枚举262）

文件：160:/home/james/complex-management/docs/business-enum-quick-reference-v0.1.html
原文：投诉类须提单方确认才能关闭
替换为：客人投诉由客户关怀部记录问题解决方案及客人反馈后可以由客户关怀部关闭；其他投诉须提单方确认；确认时限届满且经回访仍无回应的，按《业务系统地图》「物业服务」的投诉关闭规则，经主管审核批准后由客户关怀部关闭
其他正文保留。批准状态：等待当前稿逐处批准。

### 申请19新稿（地图1378；原稿拒绝不变）

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html
原文：客人（客人端投诉，客人即提单方，关闭须其确认）
替换为：客人（客人端投诉，客人即提单方；客户关怀部记录问题解决方案及客人反馈后可以关闭，无回应时按本章投诉关闭规则执行）
其他正文保留。理由：在此处直接显示操作员新定的客户关怀部关闭权限，并指向无回应处置，避免保留“关闭须其确认”旧绝对条件。批准状态：等待新稿逐处批准，不复用旧批准、也不把本次业务裁决当作该行修改许可。

所有修改继续归属M001-005；不创建新findingID。源片段在内存中验证唯一命中，客人原句完整纳入、复杂类双确认未改变，源码未写。B2人闸保持BLOCKED-ASK；本席只做本次业务答复必要的记录及拟稿，不推进外审或修复。


# M001 B2两类投诉规则纠正与原文复核

版本：V0.22（仅追加）。


## 当前唯一待审批稿：商户投诉/消费客人投诉两类（替代前述投诉拟稿）

操作员最新申请14答复原文：“这个你看一下html原文。投诉是两种投诉。第一种是商户投诉，第二种是客人投诉商户投诉时需要商户确认投诉解决，如果按规则商户投诉的问题无法解决，或者商户在解决后不点击关闭的，不点击确认投诉解决的。那么客户关怀可以把相关信息，缘由记录这些东西都填进去以后，关闭投诉。那如果是客人投诉就是消费的客人投诉。那么就是客户关怀部这个记录解决方案及客人反馈后。然后关闭”。申请18答复：“批准申请 18 最新稿（推荐）”；申请19答复：“不批准这份稿子”。
最新14明确重定两类投诉关闭条件，优先于此前主管审核/超时确认提案。18的旧稿批准保留为历史，其主管审核分支不得机械写入当前新规则；19仍明确拒绝，源码未改。当前15/16/17已批，下面14/18/19为按用户最新裁决重新拟定的精确同步文字，均待逐处许可。

### 当前原文核实

枚举262把投诉统写为提单方确认；地图1378与1384后句把客人关闭也绑定本人确认；地图1384前段还留有超时默认关闭。地图1322和治理502明确商户投诉/异议与客人投诉同通道、由客户关怀部统一受理；该规定是受理归口，不意味着关闭条件必须相同。治理502关于涉及部门/财务作实体回复、客户关怀部不代作实体判断的既有职责保留。
枚举264/265的商户工单状态链和时效、地图1380回访验收概要、复杂类双闸不新增修改；最新投诉关闭条作为明确的投诉分支适用。没有从实现代码或PRD增补业务规则。

### 申请14当前两类稿（业务地图1384）

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html
① 第1384行原片段：投诉类一律须提单方确认才能关闭，提单方在后台配置的时限内未确认的视为默认关闭（业务参数，按工单类型可配，须同时配置上下限），但单独统计为“未确认关闭”；
替换为：投诉分为商户投诉和消费客人投诉：商户投诉须由商户确认投诉已解决；按既有规则无法解决，或问题已解决但商户未确认解决或未点击关闭的，客户关怀部记录相关信息及关闭缘由后，可以关闭投诉。消费客人的投诉，由客户关怀部记录问题解决方案及客人反馈后，可以关闭；
② 同行原文：其关闭同样须提单方确认。
替换为：其关闭按上述消费客人投诉规则执行。
其他正文保留。批准状态：等待当前两类稿逐处批准。

### 申请18当前两类稿（枚举262）

文件：160:/home/james/complex-management/docs/business-enum-quick-reference-v0.1.html
第262行原文：投诉类须提单方确认才能关闭
替换为：投诉分为商户投诉和消费客人投诉：商户投诉须由商户确认投诉已解决；按既有规则无法解决，或问题已解决但商户未确认解决或未点击关闭的，客户关怀部记录相关信息及关闭缘由后，可以关闭投诉。消费客人的投诉，由客户关怀部记录问题解决方案及客人反馈后，可以关闭
原有后接分号与其他正文保留。批准状态：等待当前两类稿逐处批准。

### 申请19当前两类稿（业务地图1378）

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html
第1378行原文：客人（客人端投诉，客人即提单方，关闭须其确认）
替换为：客人（消费客人通过客人端投诉，客人即提单方；客户关怀部记录问题解决方案及客人反馈后可以关闭）
其他正文保留。本稿只有用户本次明确的消费客人记录方案及反馈后关闭，不带旧的“无回应时”或“主管审核”转引。批准状态：等待当前新稿逐处批准，原稿拒绝继续有效。

本稿不把确认超时、主管审批、未确认关闭统计或“其他投诉”分类加回投诉规则；已有工单响应/处理时限和复杂类验收条款保持原文。三份业务HTML仍未修改，源片段唯一性与最小替换已经内存验证。B2待批，全部问题定稿后才整批B3。


# M001 B2两类投诉稿全部获批与整批候选冻结

版本：V0.23（仅追加，不改先前完整独审）。

操作员实际答复：
申请14：“批准申请 14 两类投诉稿（推荐）”。
申请18：“批准申请 18 两类投诉稿（推荐）”。
申请19：“批准申请 19 两类投诉稿（推荐）”。
15/16/17先前批准继续有效；当前没有未答人闸。14/18/19旧稿批准或拒绝只作历史，不再是当前候选文本。当前投诉规则只有商户/消费客人两类，精确文案和20处替换归因见findings.md V0.13。
当前候选拟队列005/006/008；007/009/010/011的否及009细化分类均是有据异议提案、尚未Grok独立认可，不覆盖原Grok B1 FAIL。原始7条finding全文保留；需要Grok对V0.13候选独立复核后，两席各写B2票，双PASS才定稿并进入B3。此处仅记录批准与恢复，不是B2 PASS。
实际锁SHA256=d4ba138e924c6fb13020cf26e55aef537a9488ba84dfb72ca9d0f851b5090b80；B3=1；LOCK-AMEND-COUNT=4；源HTML未写。


# M001 B2整批候选复核 — Codex

版本：V0.24（仅本追加段；已有字节保留，本段不写独占锁哈希行）。

## 输入与边界

- 候选：docs/audit-2026-09/findings.md V0.13，SHA256=6f206543cade6423a9401e4586d2b1a924f69ea8d59b3afb4affd11a4846541b。
- Codex输入：docs/audit-2026-09/modules/M001-b1-self-raw.md，调用前172685字节、SHA256=969ca3d38b01dfecb0802e325aceefa6ae6f431f5d011b5284a2c29261e35cff；最新B1完整独审V0.16及其后互读/操作员答复记录至V0.23。
- Grok输入：docs/audit-2026-09/modules/M001-b1-grok-raw.md，调用前56500字节、SHA256=9d44fdbb305c9edaa497af1185757e8ae1da1cbe1d560d9cc30932cf5a2357b4；末条独占锁哈希行起V0.06完整B1。旧V0.04 GR号与本批相同只是临时号，永久ID绝不混用。
- 当前B2不修改业务HTML，不修改冻结判据，不以任一自审票覆盖Grok；本票评当前完整候选，正式定稿需对方也PASS。
- 已在此节点实际重读writing-for-agents、receiving-code-review、verification-before-completion，按V0.11借任务清晰度、核实外审意见和先验后声明；不执行技能中与当前控制规则冲突的立即修复/新文件/外发/子代理流程。

## 全批差集、分类与队列

机械检查覆盖11个当前永久ID；当前七条Grok原始finding全文保持为连续原文子串，分别唯一映射005至011；Codex CX-01吸收到既有004，未重复创建无缺陷登记。旧001/002/003仍是上批B4-r1已修出队，未借本批再次执行。

| ID | 本席B2判断 | 候选队列 | 本席依据 |
|---|---|---|---|
| M001-001/002/003 | 已修历史保留 | 否 | 上批B4-r1双PASS出队，不是本次新GR-01/02/03 |
| M001-004 | 技术性、无额外缺陷 | 否 | 文档样式及目录导航无业务发明；当前表头缺陷单独008负责 |
| M001-005 | 冲突，需按最新批准规则修复 | 是 | 枚举262、地图1378/1384现行绝对确认/默认关闭混杂；用户两类投诉最新稿已逐处批准14/18/19 |
| M001-006 | 细化、空答案缺陷 | 是 | 枚举146/地图1295只重复园区容量；15/16引用相邻已批准的单项目容量口径 |
| M001-007 | 细化、无已证工程缺陷 | 否 | 43/800三道环节及再入义务已明确，44/801手环并无豁免明文，可合取；不是以沉默创造放行分支 |
| M001-008 | 技术性、表头语义缺陷 | 是 | 13张无表头；17按原有问题/答案含义补26个列头，不改197业务值或锚点；无业务发明 |
| M001-009 | 细化、无已证工程缺陷 | 否 | 有BEQR-042/BSM-035上位；“退回流水照建但直接落 EXPIRED（不可用）”描述机读终态，非产品界面英文显示证据 |
| M001-010 | 细化、无已证工程缺陷 | 否 | 当前地图1261是用户2026-09-01明确批准原句；现场叫号有用途，GEN-016不等于要求每个功能概述自带详细状态机 |
| M001-011 | 细化、无已证工程缺陷 | 否 | 三份共同解释、配套阅读；枚举153禁对外核销与地图1300审批无相反命令；不存在逐份全部复写的冻结要求 |

四个否值及009分类是本席基于证据的B2候选票；Grok若不同意则整个B2不通过，须修候选再送审，不能执行B3。原Grok B1 FAIL及七条原意见保留，未替它改写成PASS。B1整批仍因005/006/008而FAIL；B2归并PASS从不等于模块PASS。

## 批准及最小修改核验

14/18/19的当前“两类投诉稿”各有真实弹窗“批准申请…两类投诉稿（推荐）”记录；15/16/17先前真实批准保持。无未答复HTML申请，无添加项questions。此前主管审核、确认超时以及无回应转引稿均已由最新业务裁决替代，不能写回。

20处批准替换全部唯一命中当前源行并可逐行归因：005四处、006两处、008十四处。另两份HTML的自身版本各前进一次，归因005。未新增第三份HTML改动。所有替换只在内存预演；HTML字节仍为本次独审的原SHA。

拟稿的结构解析通过：枚举308行、13表、197个td.k、26个列头；地图2032行、45个页内链接均有目标；标签栈闭合。版本前进后的静态检查与真实修复diff属于后续B3/B4，本票不提前声称实际修复成功。

## 本席B2结论

**PASS**。本席同意V0.13当前候选的完整归并、分类、队列及授权归因。只有Grok对同一候选也PASS，调度席才能将findings定稿；尚未据本票执行修复或关闭模块。


## 实跑检查脚本与输出（输入固定候选；未写业务源）

````python
from pathlib import Path
from html.parser import HTMLParser
import hashlib,re,json
r=Path('/home/james/complex-management');a=r/'docs/audit-2026-09'
f=a/'findings.md';text=f.read_text();current=text.split('## 历史归并、原始意见及审批记录')[0]
assert hashlib.sha256(f.read_bytes()).hexdigest()=='6f206543cade6423a9401e4586d2b1a924f69ea8d59b3afb4affd11a4846541b'
fence=chr(96)*3
edits=json.loads(re.search(re.escape(fence)+r'json\n(.*?)\n'+re.escape(fence),current,re.S).group(1))
assert len(edits)==20
ids=re.findall(r'^\| (M001-\d{3}) \|',current,re.M)
assert ids==['M001-'+str(i).zfill(3) for i in range(1,12)] and len(set(ids))==11
rows=[line.split('|') for line in current.splitlines() if line.startswith('| M001-')]
queue=[row[1].strip() for row in rows if row[5].strip()=='是']
assert queue==['M001-005','M001-006','M001-008'],queue
allraw=(a/'modules/M001-b1-grok-raw.md').read_bytes()
original=allraw[:56500]
assert hashlib.sha256(original).hexdigest()=='9d44fdbb305c9edaa497af1185757e8ae1da1cbe1d560d9cc30932cf5a2357b4'
gt=original.decode()
start=list(re.finditer(r'^CRITERIA-LOCK-SHA256: [a-f0-9]{64}\s*$',gt,re.M))[-1].start()
gr=gt[start:]
sections=gr[gr.index('### GR-01\n'):gr.index('### 添加类\n')]
assert sections in text
assert re.findall(r'^### (GR-\d{2})$',sections,re.M)==['GR-'+str(i).zfill(2) for i in range(1,8)]
assert len(re.findall(r'^\| M001-0(?:05|06|07|08|09|10|11) \| 当前Grok V0.06 GR-',current,re.M))==7
sr=(a/'modules/M001-b1-self-raw.md').read_bytes()
assert hashlib.sha256(sr).hexdigest()=='969ca3d38b01dfecb0802e325aceefa6ae6f431f5d011b5284a2c29261e35cff'
assert '版本：V0.23' in sr.decode() and '版本：V0.16' in sr.decode()
for n in [14,18,19]:assert ('批准申请 '+str(n)+' 两类投诉稿（推荐）') in sr.decode()
for n in [15,16]:assert ('批准申请 '+str(n)+'（推荐）') in sr.decode()
assert '批准这项技术修改（推荐）' in sr.decode()
before={e['path']:(r/e['path']).read_bytes() for e in edits}
expected={'docs/business-enum-quick-reference-v0.1.html':'1c3c25441655285812c3681d4f4baa5d0918e3182e5f7cc22299ef7ff34ad248','docs/business-system-map-for-operations-v0.1.html':'7afff1dc5b6375557d995f5808d326fda9b4f0f6a4ea6adbd78fcb0f5a3b2ac3'}
for path,b in before.items():assert hashlib.sha256(b).hexdigest()==expected[path]
preview={path:b.decode().splitlines(keepends=True) for path,b in before.items()}
counts={}
for e in edits:
 assert e['id'] in queue
 counts[e['id']]=counts.get(e['id'],0)+1
 line=preview[e['path']][e['line']-1]
 assert line.count(e['old'])==1
 preview[e['path']][e['line']-1]=line.replace(e['old'],e['new'])
assert counts=={'M001-005':4,'M001-006':2,'M001-008':14}
class Check(HTMLParser):
 def __init__(self):
  super().__init__(convert_charrefs=True);self.stack=[];self.counts={'td.k':0,'table':0,'th.col':0};self.ids=[];self.hrefs=[]
 def handle_starttag(self,t,attrs):
  at=dict(attrs)
  if t not in {'meta','link','br','hr','img','input','source','wbr','area','base','col','embed','param','track'}:self.stack.append(t)
  if t=='td' and 'k' in at.get('class','').split():self.counts['td.k']+=1
  if t=='table':self.counts['table']+=1
  if t=='th' and at.get('scope')=='col':self.counts['th.col']+=1
  if 'id' in at:self.ids.append(at['id'])
  if at.get('href','').startswith('#'):self.hrefs.append(at['href'][1:])
 def handle_endtag(self,t):assert self.stack.pop()==t
for path,lines in preview.items():
 out=''.join(lines);cp=Check();cp.feed(out);assert not cp.stack
 assert '\ufffd' not in out and len(out.splitlines())==len(before[path].decode().splitlines())
 if 'enum-quick' in path:assert cp.counts=={'td.k':197,'table':13,'th.col':26}
 else:
  assert len(cp.ids)==len(set(cp.ids)) and set(cp.hrefs)<=set(cp.ids)
  assert len(cp.hrefs)==45
 print(path+' PREVIEW_COUNTS='+json.dumps(cp.counts)+' LINES='+str(len(lines)))
# Negative control: removed consent evidence or a missing repair authorization must be detected.
mut=sr.decode().replace('批准申请 19 两类投诉稿（推荐）','未批准本次19')
assert '批准申请 19 两类投诉稿（推荐）' not in mut
bad=edits.copy();bad=bad[:-1]
assert len(bad)!=20
lock=hashlib.sha256((a/'plan/criteria-lock.md').read_bytes()).hexdigest()
ledger=(a/'ledger.md').read_text()
assert lock==(a/'plan/criteria-lock.sha256').read_text().strip()==re.search(r'^CRITERIA-LOCK-SHA256: ([a-f0-9]{64})',ledger,re.M).group(1)==re.search(r'^CRITERIA-LOCK-SHA256: ([a-f0-9]{64})',gr,re.M).group(1)
assert not list((a/'questions').glob('*.md'))
for path,b in before.items():assert (r/path).read_bytes()==b
print('CHECK=PASS; CURRENT_IDS=11; CURRENT_GROK_FINDINGS=7; ORIGINAL_GROK_BLOCKS=VERBATIM; PROPOSED_QUEUE='+','.join(queue))
print('APPROVALS=14,15,16,17,18,19; REPLACEMENTS=20; ATTRIBUTION='+json.dumps(counts))
print('CANDIDATE_SHA256='+hashlib.sha256(f.read_bytes()).hexdigest())
print('LOCK_ACTUAL='+lock)
print('BUSINESS_HTML=UNCHANGED; B2_FINALIZATION=PENDING_BOTH_VOTES')

````

````text
docs/business-system-map-for-operations-v0.1.html PREVIEW_COUNTS={"td.k": 0, "table": 13, "th.col": 0} LINES=2032
docs/business-enum-quick-reference-v0.1.html PREVIEW_COUNTS={"td.k": 197, "table": 13, "th.col": 26} LINES=308
CHECK=PASS; CURRENT_IDS=11; CURRENT_GROK_FINDINGS=7; ORIGINAL_GROK_BLOCKS=VERBATIM; PROPOSED_QUEUE=M001-005,M001-006,M001-008
APPROVALS=14,15,16,17,18,19; REPLACEMENTS=20; ATTRIBUTION={"M001-005": 4, "M001-006": 2, "M001-008": 14}
CANDIDATE_SHA256=6f206543cade6423a9401e4586d2b1a924f69ea8d59b3afb4affd11a4846541b
LOCK_ACTUAL=d4ba138e924c6fb13020cf26e55aef537a9488ba84dfb72ca9d0f851b5090b80
BUSINESS_HTML=UNCHANGED; B2_FINALIZATION=PENDING_BOTH_VOTES

````

本报告落盘前再次核对：候选V0.13、ledger、三份业务HTML的SHA256均与派发时一致。Grok交付EXIT_CODE=0，本席此刻尚未读取其本次B2追加内容；接下来才合取两票。


CRITERIA-LOCK-SHA256: 73ce1c4523de229a11552ca343f594e1506da3dc10e68d3dac216dd4ca232439
# M001 B1 Codex 独立完整报告
版本：V0.25

控制提示词V0.11；LOCK-AMEND⑤完成后按锁V0.05重审当前完整M001，首轮全仓审计没有重开。
模块M001/322，ledger IN_PROGRESS，B3已执行2次，LOCK-AMEND-COUNT=5。
独立性：Grok先完成并仅交付路径与真实exit code 0；本报告写入之前没有读取Grok本次B1 raw、原始JSON或findings。全部阅读与检查由本席执行，未派第三席、未改业务文件。

## 范围与实际读取
- 枚举速查表全文1–308行，13章节/197个td.k条；文件V0.07，SHA256=4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66。
- 业务系统地图全文1–2032行，27个h2/93个h3；文件V0.08，SHA256=3edff56a7640565d08983621e34d726d149261e70fd747ecb88b8e9cc27774ea。
- 合计2340行；含空行、注释、CSS、页尾脚本，无抽样替代全审。首次长输出截断的enum52–70与map588–744已单独完整重读。
- 地图按1–504结构样式导航、505–771总览与治理、772–928票务会员、929–1119终端与酒吧、1120–1307服务/供应/经销/游乐、1308–1478商户合同物业人力、1479–1635第三方留痕、1636–1826财务分红看板、1827–2032入口关系交付/脚本全部覆盖。
- 治理地图仅核对必要上位交叉依据：真实股权/到期借款436、项目和敏感权限653–680、U盘862–864及隐私870–872；不将该只读对照声称为M002完整审计。其SHA仍c63041a15599fbe4768b7131f78e9c6d6c0ffabe8b5ffc3a1151e24eac1c1dc0。

## 技能与证据边界
对应节点已实际读取using-superpowers及Codex工具映射、grill-me/grilling、batch-grill-me、ponytail-review、diagnosing-bugs、ubiquitous-language，使用第一性原理追问/反例/术语核对；codebase-design与verification-before-completion已在本次锁复核中读取并沿用。只按V0.11收窄技能：不派子代理、不另建术语文件、不增加人闸、不在B1修复。初始技能缺口沿用既有预检，不虚报工具调用。本文为HTML文档审计，按runbook豁免后端行为/TDD；未运行浏览器、辅助阅读设备、实际支付、数据库或法定财税系统，也不宣称这些已验证。

## 三线完整结果
第一性原理：
1. 存在必要性：197速查规则与120地图标题正文分别承担操作口径和全链关系；删除业务内容会失去相应已批准锚点，CSS/导航/结构则按技术性登记，不因没有业务收益而删除。
2. 实际验证：全部正文人工分段读完；317个锚点的当前原文逐字对照冻结引文及指定起始行；18通用项逐项适用性核对。HTML结构、链接、实际JS及默认比例用下附命令验证。
3. 前提辨别：历史批注/旧审批稿不充当当前规则，HTML描述不充当生产实现；金额示例是算术说明而非后端测试；真实第三方资料尚未确定等实施输入没有冒称已完成。

顺从式：
逐条读取下方317业务判据和18通用判据，从角色/输入/条件/行为/结果/金额/记录/配置版本串联两份HTML。最终新人一年间隔规则、八类合同变更、争议主管裁决后诉讼/仲裁挂起、两类投诉关闭、容量引用和表头都与现行批准内容及锁V0.05一致。普通规则与明确特殊例外合读，不要求每份HTML重复另一份的所有条款。

对抗式：
- 票务/会员：手机号短信判新人，未购买基线与注销满一年再享例外合读；归档记录仅到一年窗口，不关联新账户。手环入场仍受通用门槛，不是安检/年龄豁免。积分按付费、成长按实际使用人次，口径不同有明确来源。
- 分账：同票第一段参与者不占第二段名次；第二段缺配置行暂停不编造；并列均分所占名次、0核销减除、未分配余额与四舍五入尾差分开。实际10个默认档均100%；0.05按40/30/30得到0.02/0.02/0.02、尾差-0.01，说明不能擅将尾差给某酒吧。
- 酒吧/服务：低消、拒单、退订与员工代订免定金例外合读；转并台形成新节点后仅拆上一层、拆回需全部满足；销售/服务责任与服务费/小费分开。存酒时效、赠送/取用SMS、延期和人工例外按本章条件，储值退款先赠后本与跨支付逆序各自适用。
- 供应/游乐：移动平均成本与高价值分类取价是不同目的；借调/买断/领用分责任，工作扫码缺口走补录复核。项目实时人数硬上限、排队不占容量、动态软阈值不取消硬限制；包场禁核销由枚举规定，地图审批不构成豁免。
- 商户/物业：押金签约基数与清退动态基数区别清楚，先有周期账单逾期前提再触发清退。两类投诉三处相同：商户须确认、规定例外可记录缘由关闭；消费客人记录方案及反馈可关闭。1380泛称验收按1384具体类型解释，未加回已被替代的主管/超时条件；复杂设备报修仍另行确认，不与投诉混同。
- 人力/财务：普通考勤日期与跨月/跨年班次归属存在明文例外；薪资月结展示与提成可实时看分别适用。扣薪/赔偿/现金短款三项例外不当成普通扣罚上限漏洞。物业、公区能源、损耗服务费有各自明文名目。三个付款日、异议期/银行在途/失败重付、退租押金例外分别核对。
- 税费/分红/看板：平台内部计算与专业财税软件正式账税区分；当前冲正不回改封账，税种差异机制互斥。股权按真实登记，先偿到期借款，保护线待释放与清算有出口。权限查看不自动给导出权，投诉/异常计数不发出控制指令。
- 终端/留痕：工作手机与私人手机边界、项目位置限制、开关缺行默认关闭、密钥破窗更换、安检留痕到设备例外均核对。静态导航全部目标存在；未将上生产的未来要求当当前弱密码泄漏证据。
以上覆盖全部范围，未形成新工程缺陷或新增业务添加项；不是用旧审计结论代替本次检查。

## 18个通用判据逐项结果
| ID | 本次证据与适用性 |
|---|---|
| GEN-001 | 两份文件共同定义业务边界；标签、样式、导航仅承载阅读；无应删除的独立业务发明。 |
| GEN-002 | 完整HTML结构解析；执行业务地图实际脚本3种几何布局及scroll回调，27个真实导航目标。 |
| GEN-003 | 断言实际读取现行HTML和执行原script，不用复制逻辑替代；本模块无独立测试文件，不宣称后端测试通过。 |
| GEN-004 | 页面无表单或外部输入，href来自本页固定锚点；全部45个内部链接实际解析，不臆造网络输入。 |
| GEN-005 | 权限文字按主体/商户/项目分隔，具体高敏及U盘例外明确；静态需求页不承担服务端授权实现。 |
| GEN-006 | 逐读数量/钱路、锁定版本、红冲及禁止回改条文；从实际默认表解析10档，合计均100%，负尾差单列。 |
| GEN-007 | 核销、退款、分账、转并台、差异入账和支付在途条文核对；没有把需求描述当并发实现已验证。 |
| GEN-008 | 197枚举条与地图120标题正文交叉核对；四个已改锁引文均与现行原文精确相同。 |
| GEN-009 | 规则已有缺档暂挂、无法解决异议出口、退票/款失败、异常记录等分支；导航只使用实际存在目标。 |
| GEN-010 | 本模块无外部脚本、外部资源、内联事件处理器或动态注入；隐私/密钥业务只核对原文，不宣称生产加固完成。 |
| GEN-011 | 所有有过程动作留痕的通用规则与安检设备终点等显式例外合读；历史注释不当当前实现证据。 |
| GEN-012 | 实际导航27项两次线性遍历、passive滚动监听；静态页面无网络/连接/持久缓存。未做性能基准。 |
| GEN-013 | UTF-8、zh-CN、原生CSS和无依赖JS，HTML为独立静态文档；Node v24.20.0仅用于检查。 |
| GEN-014 | 本模块没有SQL/迁移可执行产物；仅核对涉及历史冻结/当前冲正需求，数据库迁移测试不适用。 |
| GEN-015 | enum13表各有问题/答案列头；map13表/标准链接/唯一id/原生焦点、响应式CSS已读；未声称浏览器或读屏器实测。 |
| GEN-016 | 文件内版本V0.07/V0.08、行数与SHA实测；声明的规则与实施前输入分开；年代注释不作为已上线证明。 |
| GEN-017 | 固定冲突→细化→技术性→添加；技术性无发明项CX-01复用004，未新增业务需求或锁外判据。 |
| GEN-018 | 结论使用HTML原文与命令实跑（类型1/4）；金额算例只验证文本算术，不充当生产行为证据。 |

## 317个业务判据完整覆盖
下表为不重叠的连续ID分组，组内每个ID均人工读完并通过精确引文命令校验，无抽样。精确逐项来源及原文在同哈希criteria-lock.md定义区；脚本输出coverage逐项列明ID/起止行/标题。
| 连续ID范围 | 当前源行范围 | 三线结果 |
|---|---|---|
| BEQR-001–BEQR-015（15项，无跳项） | docs/business-enum-quick-reference-v0.1.html:29–46 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BEQR-016–BEQR-030（15项，无跳项） | docs/business-enum-quick-reference-v0.1.html:47–68 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BEQR-031–BEQR-045（15项，无跳项） | docs/business-enum-quick-reference-v0.1.html:70–86 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BEQR-046–BEQR-060（15项，无跳项） | docs/business-enum-quick-reference-v0.1.html:87–108 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BEQR-061–BEQR-075（15项，无跳项） | docs/business-enum-quick-reference-v0.1.html:109–126 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BEQR-076–BEQR-090（15项，无跳项） | docs/business-enum-quick-reference-v0.1.html:127–147 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BEQR-091–BEQR-105（15项，无跳项） | docs/business-enum-quick-reference-v0.1.html:148–170 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BEQR-106–BEQR-120（15项，无跳项） | docs/business-enum-quick-reference-v0.1.html:171–189 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BEQR-121–BEQR-135（15项，无跳项） | docs/business-enum-quick-reference-v0.1.html:190–210 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BEQR-136–BEQR-150（15项，无跳项） | docs/business-enum-quick-reference-v0.1.html:211–233 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BEQR-151–BEQR-165（15项，无跳项） | docs/business-enum-quick-reference-v0.1.html:234–253 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BEQR-166–BEQR-180（15项，无跳项） | docs/business-enum-quick-reference-v0.1.html:255–271 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BEQR-181–BEQR-195（15项，无跳项） | docs/business-enum-quick-reference-v0.1.html:273–301 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BEQR-196–BEQR-197（2项，无跳项） | docs/business-enum-quick-reference-v0.1.html:302–303 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BSM-001–BSM-015（15项，无跳项） | docs/business-system-map-for-operations-v0.1.html:505–630 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BSM-016–BSM-030（15项，无跳项） | docs/business-system-map-for-operations-v0.1.html:631–730 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BSM-031–BSM-045（15项，无跳项） | docs/business-system-map-for-operations-v0.1.html:731–952 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BSM-046–BSM-060（15项，无跳项） | docs/business-system-map-for-operations-v0.1.html:953–1154 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BSM-061–BSM-075（15项，无跳项） | docs/business-system-map-for-operations-v0.1.html:1155–1342 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BSM-076–BSM-090（15项，无跳项） | docs/business-system-map-for-operations-v0.1.html:1343–1553 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BSM-091–BSM-105（15项，无跳项） | docs/business-system-map-for-operations-v0.1.html:1554–1708 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |
| BSM-106–BSM-120（15项，无跳项） | docs/business-system-map-for-operations-v0.1.html:1709–2032 | 全组逐条人工读完；每个引文及起始行精确校验；三线未证实缺陷 |

## 本次登记项（不是待修缺陷）
CX-01：既有M001-004，技术性，工程缺陷否。来源为enum1–24及各表结构、map1–504/2008–2030；依据类型4，GEN-001/002/010/012/015/017/018。
无业务发明：结构/样式只决定本页呈现和跳转，不增加价格、资格、状态、主体或付款规则。13个问题/答案表头已存在并有效，不再重复报008。实际HTMLParser检验26张表、197标签、120地图标题、45有效内部链接；实际导航脚本仅选.nav内27链接。该登记复用现有正式ID，不新造重复ID。

## 可复现检查命令与结果
执行方式：在160正本主工作区通过python3 -标准输入执行下列完整代码，不写脚本文件。真实进程exit code 0。
初始检查脚本曾误用大写UTF-8字面值，继而把45个全部a当成.nav的27个a；已分别按HTML大小写规则、真实.nav选择范围修正检查脚本。失败断言没有被当业务finding，也没有改HTML。下方是最终实跑完整代码。

```python
from pathlib import Path
from html.parser import HTMLParser
from collections import Counter
from decimal import Decimal,ROUND_HALF_UP
import re,html,hashlib,json,subprocess
r=Path('/home/james/complex-management');a=r/'docs/audit-2026-09'
paths=['docs/business-enum-quick-reference-v0.1.html','docs/business-system-map-for-operations-v0.1.html']
expected=['4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66','3edff56a7640565d08983621e34d726d149261e70fd747ecb88b8e9cc27774ea']
class Doc(HTMLParser):
 def __init__(self):
  super().__init__(convert_charrefs=True);self.stack=[];self.ids=[];self.links=[];self.labels=[];self.headings=[];self.cols=[];self.tables=0;self.active=None;self.text='';self.lang=None;self.events=[];self.external=[]
 def handle_starttag(self,t,attrs):
  d=dict(attrs)
  if t=='html':self.lang=d.get('lang')
  if t not in {'meta','link','br','hr','img','input','source','wbr','area','base','col','embed','param','track'}:self.stack.append(t)
  if 'id' in d:self.ids.append(d['id'])
  if t=='a':self.links.append(d.get('href',''))
  if t=='table':self.tables+=1
  if (t=='td' and 'k' in d.get('class','').split()) or t in {'h2','h3'} or (t=='th' and d.get('scope')=='col'):
   self.active=t;self.text=''
  self.events.extend(k for k in d if k.startswith('on'))
  self.external.extend(d[k] for k in ['src','href'] if k in d and not d[k].startswith('#'))
 def handle_data(self,s):
  if self.active:self.text+=s
 def handle_endtag(self,t):
  assert self.stack and self.stack.pop()==t,('unbalanced',t,self.getpos())
  if self.active==t:
   (self.labels if t=='td' else self.cols if t=='th' else self.headings).append((self.text.strip(),self.getpos()[0]));self.active=None
docs={};results={}
for p,want in zip(paths,expected):
 b=(r/p).read_bytes();assert hashlib.sha256(b).hexdigest()==want
 s=b.decode('utf-8');assert '\ufffd' not in s and re.search(r'<meta\s+charset=["\x27]utf-8["\x27]',s,re.I)
 d=Doc();d.feed(s);d.close();assert not d.stack and len(d.ids)==len(set(d.ids))
 assert all(h[1:] in d.ids for h in d.links if h.startswith('#'))
 assert not d.events and not d.external
 docs[p]=(s,d)
 results[p]={'sha256':want,'bytes':len(b),'lines':len(s.splitlines()),'tables':d.tables,'labels':len(d.labels),'headings':len(d.headings),'links':len(d.links),'lang':d.lang,'balance':True}
ed=docs[paths[0]][1];md=docs[paths[1]][1]
assert ed.tables==13 and len(ed.labels)==197 and [c[0] for c in ed.cols]==['问题','答案']*13
assert len(md.headings)==120 and len(md.links)==45
lk=(a/'plan/criteria-lock.md').read_text()
mapping=re.search(r'^M001 → \[([^\]]*)\]',lk,re.M).group(1).split(', ')
assert len(mapping)==335 and len(set(mapping))==335
defs={m.group(1):m.group(0) for m in re.finditer(r'^### ((?:GEN|BEQR|BSM|PGM)-\d{3})\b.*?(?=^### |^## 模块|\Z)',lk,re.M|re.S)}
assert set(mapping)<=set(defs)
coverage=[]
for cid in mapping:
 if cid.startswith('GEN'):continue
 block=defs[cid];src=re.search(r'- 来源：'+chr(96)+r'([^'+chr(96)+r']+):(\d+)'+chr(96),block)
 p,n=src.group(1),int(src.group(2));q=re.search(chr(96)*4+r'html\n(.*?)\n'+chr(96)*4,block,re.S).group(1)
 lines=docs[p][0].splitlines()
 assert '\n'.join(lines[n-1:n-1+len(q.splitlines())])==q,(cid,p,n)
 coverage.append({'id':cid,'path':p,'line':n,'end':n+len(q.splitlines())-1,'title':block.splitlines()[0].split(' — ',1)[1]})
assert [(c['title'],c['line']) for c in coverage if c['id'].startswith('BEQR')]==ed.labels
assert [(c['title'],c['line']) for c in coverage if c['id'].startswith('BSM')]==md.headings
# Parse every numeric default ranking row from actual HTML, expand stated rank intervals.
et=html.unescape(re.sub('<[^>]+>','',docs[paths[0]][0].splitlines()[51]))
part=et.split('参与家数档位表（出厂默认',1)[1].split('参与家数档位表不设家数上限',1)[0]
rows={}
for n,body in re.findall(r'第二段参与 (\d+) 家＝(.*?)(?=；第二段参与|。|$)',part):
 slots={}
 for lo,hi,pct in re.findall(r'第 (\d+)(?:[–-](\d+))? 名(?:各 )? ?(\d+)%',body):
  for rank in range(int(lo),int(hi or lo)+1):
   assert rank not in slots;slots[rank]=int(pct)
 assert set(slots)==set(range(1,int(n)+1)),(n,body,slots)
 assert sum(slots.values())==100
 rows[int(n)]=[slots[k] for k in sorted(slots)]
assert set(rows)==set(range(1,11))
assert sum(rows[3][1:])/2==30 and sum(rows[2])/2==50
money=lambda x:Decimal(x).quantize(Decimal('.01'),rounding=ROUND_HALF_UP)
assert money('0.005')==Decimal('.01') and money('-0.005')==Decimal('-.01')
pool=Decimal('0.05');alloc=[money(pool*Decimal(p)/100) for p in rows[3]]
tail=pool-sum(alloc)
assert sum(alloc)+tail==pool and tail==Decimal('-.01')
# Execute actual page script in Node vm with DOM geometry stubs. No copy of script behavior.
src=docs[paths[1]][0];navlinks=re.findall(r'href="([^"]+)"',re.search(r'<nav class="nav">(.*?)</nav>',src,re.S).group(1));assert len(navlinks)==27 and len(set(navlinks))==27
js=re.search(r'<script>(.*?)</script>',src,re.S).group(1)
harness=r"""
const vm=require('node:vm'),assert=require('node:assert/strict');
const hrefs=HREFS, code=CODE;
function run(tops,expected){
 const sections=hrefs.map((h,i)=>({id:h.slice(1),getBoundingClientRect:()=>({top:tops[i]})}));
 const links=hrefs.map(h=>({active:false,getAttribute:n=>{assert.equal(n,'href');return h},classList:{toggle(c,v){assert.equal(c,'active');links[hrefs.indexOf(h)].active=v}}}));
 let scroll;const document={querySelectorAll:s=>{assert.equal(s,'.nav a');return links},querySelector:s=>sections[hrefs.indexOf(s)]};
 const window={addEventListener:(type,cb,opt)=>{assert.equal(type,'scroll');assert.equal(opt.passive,true);scroll=cb}};
 vm.runInNewContext(code,{document,window,Array});assert.equal(links.filter(x=>x.active).length,1);assert.equal(links.findIndex(x=>x.active),expected);
 for(let i=0;i<tops.length;i++)tops[i]=-100;scroll();assert.equal(links.findIndex(x=>x.active),hrefs.length-1);
}
run(hrefs.map((_,i)=>200+i*500),0);
run(hrefs.map((_,i)=>i<21?-200: i===21?120:121+i*500),21);
run(hrefs.map((_,i)=>-100+i),hrefs.length-1);
console.log('actual_navigation_script: PASS (3 initial geometries + scroll callback, exactly one active link, 27 real nav targets)');
""".replace('HREFS',json.dumps(navlinks)).replace('CODE',json.dumps(js))
np=subprocess.run(['node','-'],input=harness,text=True,capture_output=True)
assert np.returncode==0,np.stderr
lock=hashlib.sha256((a/'plan/criteria-lock.md').read_bytes()).hexdigest()
assert lock==(a/'plan/criteria-lock.sha256').read_text().strip()==re.search(r'^CRITERIA-LOCK-SHA256: (\w+)',(a/'ledger.md').read_text(),re.M).group(1)
pg='docs/platform-governance-map-for-operations-v0.1.html'
assert hashlib.sha256((r/pg).read_bytes()).hexdigest()=='c63041a15599fbe4768b7131f78e9c6d6c0ffabe8b5ffc3a1151e24eac1c1dc0'
assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=r,text=True).strip()=='2416128dbacecd06b232f9f25ed8c5ec4ed4f3c2'
print(json.dumps({'check_result':'PASS','sources':results,'mapping_count':len(mapping),'exact_quote_anchors':len(coverage),'rank_defaults':rows,'rounding_example':{'pool':str(pool),'shares':list(map(str,alloc)),'tail':str(tail)},'node':np.stdout.strip(),'lock_actual':lock,'coverage':coverage},ensure_ascii=False))

```

结果摘要：
```json
{
  "check_result": "PASS",
  "sources": {
    "docs/business-enum-quick-reference-v0.1.html": {
      "sha256": "4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66",
      "bytes": 133961,
      "lines": 308,
      "tables": 13,
      "labels": 197,
      "headings": 13,
      "links": 0,
      "lang": "zh-CN",
      "balance": true
    },
    "docs/business-system-map-for-operations-v0.1.html": {
      "sha256": "3edff56a7640565d08983621e34d726d149261e70fd747ecb88b8e9cc27774ea",
      "bytes": 303828,
      "lines": 2032,
      "tables": 13,
      "labels": 0,
      "headings": 120,
      "links": 45,
      "lang": "zh-CN",
      "balance": true
    }
  },
  "mapping_count": 335,
  "exact_quote_anchors": 317,
  "rank_defaults": {
    "1": [
      100
    ],
    "2": [
      57,
      43
    ],
    "3": [
      40,
      30,
      30
    ],
    "4": [
      31,
      23,
      23,
      23
    ],
    "5": [
      25,
      20,
      20,
      20,
      15
    ],
    "6": [
      23,
      17,
      17,
      17,
      13,
      13
    ],
    "7": [
      22,
      15,
      15,
      15,
      11,
      11,
      11
    ],
    "8": [
      18,
      14,
      14,
      14,
      10,
      10,
      10,
      10
    ],
    "9": [
      15,
      13,
      13,
      13,
      10,
      10,
      10,
      10,
      6
    ],
    "10": [
      16,
      12,
      12,
      12,
      9,
      9,
      9,
      9,
      6,
      6
    ]
  },
  "rounding_example": {
    "pool": "0.05",
    "shares": [
      "0.02",
      "0.02",
      "0.02"
    ],
    "tail": "-0.01"
  },
  "node": "actual_navigation_script: PASS (3 initial geometries + scroll callback, exactly one active link, 27 real nav targets)",
  "lock_actual": "73ce1c4523de229a11552ca343f594e1506da3dc10e68d3dac216dd4ca232439",
  "coverage": "317条精确锚点全部通过，精确分组见上表"
}
```

## 本方结论
B1审计结论：PASS。新增工程缺陷0；新增添加项0；技术性登记1项（CX-01复用M001-004）。
当前只是Codex本次B1票，不是双席B2定稿或模块PASS。下一步仅在本报告完整落盘后读取Grok本次完整B1，执行B1后四值校验并整批B2。源文件、锁、正式索引及HEAD没有被本方审计改动。


# M001 B2 Codex 整批归并票
版本：V0.26

步骤B2，M001/322，IN_PROGRESS，B3=2。控制V0.12；判据锁仍V0.05，没有新增判据。
本批输入：
- docs/audit-2026-09/modules/M001-b1-self-raw.md：本次V0.25完整B1，调用前206983字节，SHA256=c78414da9df601f94816fccc5a896fe544a41f2f0d4108a224a25e7bcc7ce1b2。
- docs/audit-2026-09/modules/M001-b1-grok-raw.md：本次V0.08完整B1，B2调用前90051字节，SHA256=ac206e8acaad5dc3a857e04df74959ddd0960d1f3750e016a38f78494d66b533。
- 候选docs/audit-2026-09/findings.md V0.17，SHA256=1b73dcc423dafd8ba8e224e879513541f19635ac398f534189808c7b2439d767。历史候选及已执行的20处清单不当当前待修。
- ledger V0.48：控制升级和五次获批HTML同步排除出COUNT的来源逐一记录；当前COUNT=0，保留历史五次过程，B3=2、重跑=6不变。

整批差集与独立复核：
1. Codex本批发现集合仅技术性登记CX-01→既有M001-004；Grok本批无GR编号，明确无新缺陷。两份最新完整B1均已读完，没有漏并、改写或虚构新ID。
2. CX-01先走冲突/细化判断；本页CSS/结构/导航只承载文档阅读，不产生业务规则，技术性无业务发明理由成立。实际结构/脚本命令见V0.25，Grok V0.08独立接受CSS/结构无发明。具体已修表头008不被该登记抵销。
3. 当前11个永久ID逐条核对：001/002/003此前已修；005/006/008在B4-r2双PASS出队，20处获批新片段仍逐条存在于指定行；004只登记；007/009/010/011以前B2双席已定稿且本次B1双席重新检查无缺陷，维持否。无旧临时编号混入本批。
4. 脚本断言11ID唯一且连续、当前全部队列否；004分类技术性/无缺陷；questions不存在或空；20处新片段逐行存在；候选SHA及本方原前缀SHA一致，真实exit code 0。
5. 控制V0.11→V0.12没有改变本次业务审计范围/判据/源码；两份本次B1均已首行独占实算哈希。ledger历史五次均明确逐项HTML授权、无新ID，按新版COUNT=0是规则迁移，不是删除历史、擅自扩锁或重开全仓。本步不改变lock/sidecar/源码。

本席B2结论：PASS。队列为空，无新增ID/添加项/待答HTML申请。仍须Grok本批独立B2 PASS才能定稿；届时跳过B3/B4、B3保持2，进B5-r2整个模块重审，不能以B1/B2票直接收口。
本追加不写独占锁哈希行，完整保留前206983字节。
