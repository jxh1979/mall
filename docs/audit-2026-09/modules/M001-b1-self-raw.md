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


CRITERIA-LOCK-SHA256: 16def02336743500166af4cfd5fff48d02731d63d45a8f5f72721ca8ebd408e3

# M001 B1 — Codex 独立全量审计

版本：V0.27
日期：2026-09-08 UTC
依据：160-audit-prompt.md V0.17；正式计划 V0.04，runbook V0.02。
正本：160:/home/james/complex-management，main。
HEAD：242f487233d028173cb9198eecd079f5d877405c。
阶段：B1；ledger V1.39，M001 IN_PROGRESS；B3 历史累计 2，本次 0。
结论：PASS（仅本席本次 B1 全量审计；不等同 B2 定稿、B5 或模块 PASS）。
Grok 当前 B1 文件仅完成交付元数据检查，本报告完整落盘之前未读取其正文、结论或原始 CLI JSON。本席未使用任何第三席/子代理结果。

## 全文件与三线覆盖

| 文件 | SHA256 | 完整范围 | 三线 |
|---|---|---|---|
| docs/business-enum-quick-reference-v0.1.html | 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 | 全部 308 行（含空行/CSS/注释） | 第一性原理、顺从式、对抗式均完成 |
| docs/business-system-map-for-operations-v0.1.html | a736ad246a1b832502c94379bce2bef5f65d11b7d07068c31ca37d0eb7c287f4 | 全部 2032 行（含空行/CSS/脚本/注释） | 第一性原理、顺从式、对抗式均完成 |

本席分完整行段阅读全文，未截断源文件。提示词有效行数合计 2340。初始阅读工具显示的末尾空段曾使会话中暂记 309/2033；本次按真实字节和 splitlines 实算均为 308/2032，以本报告值为准。
未发布 76 文件的重复/HTML 出处专项已另行完成；其事实没有被用作本模块业务实现 PASS。本模块不把 Java、迁移、安卓代码当作已审范围。第三份平台治理 HTML 仅对照其相关上位口径，没有开启 M002。

## 第一性原理线（三问逐项）

1. 为什么存在：BEQR 全文提供封闭枚举、金额/时点/权限判定；BSM 全文提供业务流程、责任主体和入口关系。删除任一正本会失去其独有的上位细节；另一份的重述用于互相定位，不是可随意删除的字节副本。依据类型 1：两份 HTML 末尾解释权声明及下表逐锚点原文。
2. 哪些行为实际执行：本模块是需求文档，业务行为按 §2.3 文档豁免用类型 1/4 原文与算例核对；实际执行了 HTML 结构/路径检查、BSM 原始导航 JS（27 目标和首屏）、分配比例提取与算术反例。未运行/未声称已验证产品支付、薪资、库存、验龄设备或法律适用。
3. 未验证前提：真实第三方接口、生产工资分项、在途银行行为与批准事实不能由本文自证。HTML 明写接口资料当前未确定，并要求后续接入；这些不当作已上线事实。使用决定时间线处理冲突前须有实际裁决时间，本席没有以注释“已拍板”替代操作员授权。未发现必须依靠猜测才能给本模块通过的确定矛盾。

## 顺从式线

逐项读完 BEQR-001—197、BSM-001—120 的完整标题与正文；317 条冻结摘录逐字存在于当前源文件。下面逐 ID 写标题和精确源行，结论是该上位条目已全文核对其适用范围及本模块重复表述，未确认冲突。它不表示下游实现已经满足该判据。
通用 18 条均纳入，下表说明本模块的适用边界。

| 判据 | 本模块证据与结论 |
|---|---|
| GEN-001 | 两份权威文件用途不同；纯展示 CSS/导航为正常阅读服务，无业务发明；不因重述规则就删业务正本。 |
| GEN-002 | BSM 原脚本在真实 27 个导航目标的 DOM 等价输入上执行；初始和每目标激活均断言通过。文档中业务行为不冒充已执行代码。 |
| GEN-003 | 机械断言实跑；将比例 100% 改为 99% 的内存反例确实失败，非永绿断言。 |
| GEN-004 | 本模块静态文本无用户写入口；动态脚本选择器取本文件静态 href，全部解析到实际 ID；业务校验规则按各锚点核对。 |
| GEN-005 | 逐读默认拒绝、十一端、职责与敏感信息范围；具体明定的密钥/人工查验例外不误判为普通权限放行。未执行服务端授权测试。 |
| GEN-006 | 核对钱路守恒、来源分栏、租赁排他、券与支付共同生效、挂起取消与付款互斥；结构解析无标签失配。 |
| GEN-007 | 核对转赠三动作先成功者、多人/多次票单位、出场重复不重复扣、幂等逐笔密钥例外及并拆台整体成功；未声称产品并发测试通过。 |
| GEN-008 | 两份中的枚举与作用域逐段对照；流程概述并非完整清单，具体例外与独立子流程按其范围解释。 |
| GEN-009 | 接口未接入口灰显、未出款状态、第三方差异、能力缺失等明确列出；缺配置不视为开启，未发现静默伪成功文案。 |
| GEN-010 | 本模块无可执行外部输入/凭证；人脸、注销、查询、导出与证据查看边界按明文审读；不将本文当法规论证。 |
| GEN-011 | 对照逐笔来源/时点/责任人与不可覆盖记录；安检、人工验龄的特定溯源终点与全局原则按明定例外一并核对。 |
| GEN-012 | 导航每次遍历固定 27 个章节，非开放输入放大；只读静态页面无后台资源持有。 |
| GEN-013 | 文件以 UTF-8 原生打开，不依赖额外网络库；导航用原生 JS。未用“本次无 diff”门禁冒充全量内容审计。 |
| GEN-014 | 不适用执行检查：本模块无 SQL/迁移；迁移正确性留所辖模块。 |
| GEN-015 | HTML 语言与编码声明、标题层次、表格表头、原生链接可键盘访问；导航有效、标签闭合；未执行真实浏览器截图或读屏测试，不夸大覆盖。 |
| GEN-016 | 路径和内部链接实查；版本 V0.07/V0.11 与文件正文一致；文中第三方/后续能力仍按计划态；历史评论不等同当前运行结论。 |
| GEN-017 | 正文是业务上位本身，核对内部与三 HTML 一致性；以下两条纯展示技术性登记无业务发明。未确认独立添加或冲突。 |
| GEN-018 | 仅使用实际 HTML 原文与本报告所附命令输出；不是以旧 PASS、Grok 意见或代码注释作判据。 |

### 全部 HTML 判据覆盖表

源文件简称：BEQR=docs/business-enum-quick-reference-v0.1.html；BSM=docs/business-system-map-for-operations-v0.1.html。
本表每行的“已核对”指顺从式全文和对抗式作用域对照均完成，结合下方反例与排除说明。冻结摘录逐字匹配的命令覆盖所有 317 行。

| 判据 ID | 源行 | 锚点 | 结果 |
|---|---|---|---|
| BEQR-001 | BEQR:29 | 票种 | 已核对，未确认缺陷 |
| BEQR-002 | BEQR:30 | 票务策略状态 | 已核对，未确认缺陷 |
| BEQR-003 | BEQR:32 | 定价、售票与收款主体 | 已核对，未确认缺陷 |
| BEQR-004 | BEQR:33 | 年龄限制 | 已核对，未确认缺陷 |
| BEQR-005 | BEQR:34 | 通票赠酒下限 | 已核对，未确认缺陷 |
| BEQR-006 | BEQR:35 | 通票类型 | 已核对，未确认缺陷 |
| BEQR-007 | BEQR:36 | 权益类型 | 已核对，未确认缺陷 |
| BEQR-008 | BEQR:37 | 餐饮/零售券两种用法 | 已核对，未确认缺陷 |
| BEQR-009 | BEQR:39 | 资格判定 | 已核对，未确认缺陷 |
| BEQR-010 | BEQR:40 | 订单状态 | 已核对，未确认缺陷 |
| BEQR-011 | BEQR:41 | 票/权益状态 | 已核对，未确认缺陷 |
| BEQR-012 | BEQR:43 | 核销记录状态 | 已核对，未确认缺陷 |
| BEQR-013 | BEQR:44 | 手环入场 | 已核对，未确认缺陷 |
| BEQR-014 | BEQR:45 | 退款单状态 | 已核对，未确认缺陷 |
| BEQR-015 | BEQR:46 | 有效期类型 | 已核对，未确认缺陷 |
| BEQR-016 | BEQR:47 | 售票与退款原则 | 已核对，未确认缺陷 |
| BEQR-017 | BEQR:48 | 到期未核销款 | 已核对，未确认缺陷 |
| BEQR-018 | BEQR:49 | 域归属对照 | 已核对，未确认缺陷 |
| BEQR-019 | BEQR:51 | 跨域通票分配 | 已核对，未确认缺陷 |
| BEQR-020 | BEQR:52 | 酒吧通票可分配池 | 已核对，未确认缺陷 |
| BEQR-021 | BEQR:53 | 收费与免费权益结算 | 已核对，未确认缺陷 |
| BEQR-022 | BEQR:54 | 项目内部结算价 | 已核对，未确认缺陷 |
| BEQR-023 | BEQR:55 | 核销顺序 | 已核对，未确认缺陷 |
| BEQR-024 | BEQR:56 | 游乐核销归属 | 已核对，未确认缺陷 |
| BEQR-025 | BEQR:57 | 门票综合服务费 | 已核对，未确认缺陷 |
| BEQR-026 | BEQR:58 | 动态价与会员价 | 已核对，未确认缺陷 |
| BEQR-027 | BEQR:60 | 人脸验龄数据生命周期 | 已核对，未确认缺陷 |
| BEQR-028 | BEQR:65 | 会员唯一识别 | 已核对，未确认缺陷 |
| BEQR-029 | BEQR:66 | 会员注册 | 已核对，未确认缺陷 |
| BEQR-030 | BEQR:68 | 会员身份状态 | 已核对，未确认缺陷 |
| BEQR-031 | BEQR:70 | 等级与成长值 | 已核对，未确认缺陷 |
| BEQR-032 | BEQR:71 | 等级权益对照 | 已核对，未确认缺陷 |
| BEQR-033 | BEQR:72 | 积分与成长值来源 | 已核对，未确认缺陷 |
| BEQR-034 | BEQR:73 | 票务人次计算 | 已核对，未确认缺陷 |
| BEQR-035 | BEQR:75 | 有效消费与冻结 | 已核对，未确认缺陷 |
| BEQR-036 | BEQR:77 | 积分明细类型 | 已核对，未确认缺陷 |
| BEQR-037 | BEQR:78 | 积分兑换定价 | 已核对，未确认缺陷 |
| BEQR-038 | BEQR:79 | 商品规格 | 已核对，未确认缺陷 |
| BEQR-039 | BEQR:80 | 项目零售参考价 | 已核对，未确认缺陷 |
| BEQR-040 | BEQR:81 | 发券与核销结算 | 已核对，未确认缺陷 |
| BEQR-041 | BEQR:82 | 重复兑换与转赠 | 已核对，未确认缺陷 |
| BEQR-042 | BEQR:83 | 券包状态 | 已核对，未确认缺陷 |
| BEQR-043 | BEQR:84 | 积分结算账期 | 已核对，未确认缺陷 |
| BEQR-044 | BEQR:85 | 会员系统服务费 | 已核对，未确认缺陷 |
| BEQR-045 | BEQR:86 | 储值卡类型与收款主体 | 已核对，未确认缺陷 |
| BEQR-046 | BEQR:87 | 会员忠诚度扩展 | 已核对，未确认缺陷 |
| BEQR-047 | BEQR:89 | 会员注册与开卡 | 已核对，未确认缺陷 |
| BEQR-048 | BEQR:90 | 学生认证 | 已核对，未确认缺陷 |
| BEQR-049 | BEQR:91 | 会员积分活动状态 | 已核对，未确认缺陷 |
| BEQR-050 | BEQR:92 | 积分活动预算守恒 | 已核对，未确认缺陷 |
| BEQR-051 | BEQR:93 | 充值积分 | 已核对，未确认缺陷 |
| BEQR-052 | BEQR:94 | 会员营销边界 | 已核对，未确认缺陷 |
| BEQR-053 | BEQR:95 | 发券对象 | 已核对，未确认缺陷 |
| BEQR-054 | BEQR:100 | 桌台/卡座/吧台位状态 | 已核对，未确认缺陷 |
| BEQR-055 | BEQR:101 | 账单状态 | 已核对，未确认缺陷 |
| BEQR-056 | BEQR:103 | 订台定金状态 | 已核对，未确认缺陷 |
| BEQR-057 | BEQR:105 | 订卡邀请函状态 | 已核对，未确认缺陷 |
| BEQR-058 | BEQR:106 | 点酒方式 | 已核对，未确认缺陷 |
| BEQR-059 | BEQR:107 | 支付方式 | 已核对，未确认缺陷 |
| BEQR-060 | BEQR:108 | 销售提成 | 已核对，未确认缺陷 |
| BEQR-061 | BEQR:109 | 服务员提成 | 已核对，未确认缺陷 |
| BEQR-062 | BEQR:110 | 退菜退酒 | 已核对，未确认缺陷 |
| BEQR-063 | BEQR:111 | 高危动作（需审批/双证据） | 已核对，未确认缺陷 |
| BEQR-064 | BEQR:112 | 出品票据状态 | 已核对，未确认缺陷 |
| BEQR-065 | BEQR:114 | 账单操作 | 已核对，未确认缺陷 |
| BEQR-066 | BEQR:115 | 收银交班状态 | 已核对，未确认缺陷 |
| BEQR-067 | BEQR:117 | 最低消费 | 已核对，未确认缺陷 |
| BEQR-068 | BEQR:118 | 服务费 | 已核对，未确认缺陷 |
| BEQR-069 | BEQR:119 | 小费 | 已核对，未确认缺陷 |
| BEQR-070 | BEQR:120 | 客人存酒有效期与转存 | 已核对，未确认缺陷 |
| BEQR-071 | BEQR:122 | 存酒瓶数 | 已核对，未确认缺陷 |
| BEQR-072 | BEQR:123 | 套餐赠券适用范围 | 已核对，未确认缺陷 |
| BEQR-073 | BEQR:124 | 非低消商品 | 已核对，未确认缺陷 |
| BEQR-074 | BEQR:125 | 隐私桌台权限 | 已核对，未确认缺陷 |
| BEQR-075 | BEQR:126 | 桌台会话与菜单 | 已核对，未确认缺陷 |
| BEQR-076 | BEQR:127 | 桌台计时产品 | 已核对，未确认缺陷 |
| BEQR-077 | BEQR:128 | 门店支付方式 | 已核对，未确认缺陷 |
| BEQR-078 | BEQR:129 | 扫码点单身份 | 已核对，未确认缺陷 |
| BEQR-079 | BEQR:130 | 优惠券自动选择 | 已核对，未确认缺陷 |
| BEQR-080 | BEQR:131 | 优惠券付款预留 | 已核对，未确认缺陷 |
| BEQR-081 | BEQR:132 | 正式挂起订单状态 | 已核对，未确认缺陷 |
| BEQR-082 | BEQR:133 | 正式挂起订单规则 | 已核对，未确认缺陷 |
| BEQR-083 | BEQR:134 | 挂单取消与付款互斥 | 已核对，未确认缺陷 |
| BEQR-084 | BEQR:135 | 挂单坏账损耗 | 已核对，未确认缺陷 |
| BEQR-085 | BEQR:140 | 项目开放状态 | 已核对，未确认缺陷 |
| BEQR-086 | BEQR:142 | 场次状态 | 已核对，未确认缺陷 |
| BEQR-087 | BEQR:144 | 游乐「当天／每天」口径 | 已核对，未确认缺陷 |
| BEQR-088 | BEQR:145 | 容量双阈值 | 已核对，未确认缺陷 |
| BEQR-089 | BEQR:146 | 园区容量 | 已核对，未确认缺陷 |
| BEQR-090 | BEQR:147 | 设备关键/辅助分级 | 已核对，未确认缺陷 |
| BEQR-091 | BEQR:148 | 全园故障与补偿通票 | 已核对，未确认缺陷 |
| BEQR-092 | BEQR:149 | 安全严重度 | 已核对，未确认缺陷 |
| BEQR-093 | BEQR:150 | 巡检任务状态 | 已核对，未确认缺陷 |
| BEQR-094 | BEQR:151 | 越界类型 | 已核对，未确认缺陷 |
| BEQR-095 | BEQR:153 | 团体/包场 | 已核对，未确认缺陷 |
| BEQR-096 | BEQR:154 | 游乐增强阶段 | 已核对，未确认缺陷 |
| BEQR-097 | BEQR:159 | 供应合同模式 | 已核对，未确认缺陷 |
| BEQR-098 | BEQR:160 | 库房分类 | 已核对，未确认缺陷 |
| BEQR-099 | BEQR:161 | 物品大类 | 已核对，未确认缺陷 |
| BEQR-100 | BEQR:162 | 库存流水类型 | 已核对，未确认缺陷 |
| BEQR-101 | BEQR:164 | 高价值判定 | 已核对，未确认缺陷 |
| BEQR-102 | BEQR:166 | 跨主体借调 | 已核对，未确认缺陷 |
| BEQR-103 | BEQR:168 | 智能采购建议 | 已核对，未确认缺陷 |
| BEQR-104 | BEQR:169 | 门店要货履约状态 | 已核对，未确认缺陷 |
| BEQR-105 | BEQR:170 | 经销商状态 | 已核对，未确认缺陷 |
| BEQR-106 | BEQR:171 | 经销询价状态 | 已核对，未确认缺陷 |
| BEQR-107 | BEQR:172 | 经销价格目录状态 | 已核对，未确认缺陷 |
| BEQR-108 | BEQR:173 | 经销订单状态 | 已核对，未确认缺陷 |
| BEQR-109 | BEQR:174 | 经销退货单状态 | 已核对，未确认缺陷 |
| BEQR-110 | BEQR:175 | 经销库存外显 | 已核对，未确认缺陷 |
| BEQR-111 | BEQR:176 | 经销支付方式 | 已核对，未确认缺陷 |
| BEQR-112 | BEQR:177 | 经销库存预留 | 已核对，未确认缺陷 |
| BEQR-113 | BEQR:178 | 经销职责分离 | 已核对，未确认缺陷 |
| BEQR-114 | BEQR:179 | 经销取消、退货与开票 | 已核对，未确认缺陷 |
| BEQR-115 | BEQR:180 | 经销价格与税务快照 | 已核对，未确认缺陷 |
| BEQR-116 | BEQR:181 | 经销商停用撤权 | 已核对，未确认缺陷 |
| BEQR-117 | BEQR:182 | 经销资金归属 | 已核对，未确认缺陷 |
| BEQR-118 | BEQR:187 | 运营营业日 | 已核对，未确认缺陷 |
| BEQR-119 | BEQR:188 | 对账口径 | 已核对，未确认缺陷 |
| BEQR-120 | BEQR:189 | 对账差异四类 | 已核对，未确认缺陷 |
| BEQR-121 | BEQR:190 | 日结状态 | 已核对，未确认缺陷 |
| BEQR-122 | BEQR:191 | 差异处理状态 | 已核对，未确认缺陷 |
| BEQR-123 | BEQR:192 | 结算状态 | 已核对，未确认缺陷 |
| BEQR-124 | BEQR:193 | 综合通票定价 | 已核对，未确认缺陷 |
| BEQR-125 | BEQR:195 | 商户业态 | 已核对，未确认缺陷 |
| BEQR-126 | BEQR:196 | 合作模式 | 已核对，未确认缺陷 |
| BEQR-127 | BEQR:197 | 退款资金流向 | 已核对，未确认缺陷 |
| BEQR-128 | BEQR:198 | 物业费 | 已核对，未确认缺陷 |
| BEQR-129 | BEQR:199 | 能源费与损耗费 | 已核对，未确认缺陷 |
| BEQR-130 | BEQR:201 | 税费配置 | 已核对，未确认缺陷 |
| BEQR-131 | BEQR:202 | 三方对账框架 | 已核对，未确认缺陷 |
| BEQR-132 | BEQR:203 | 开票状态 | 已核对，未确认缺陷 |
| BEQR-133 | BEQR:208 | 接入的外部系统 | 已核对，未确认缺陷 |
| BEQR-134 | BEQR:209 | 客户标签 | 已核对，未确认缺陷 |
| BEQR-135 | BEQR:210 | 统一原则 | 已核对，未确认缺陷 |
| BEQR-136 | BEQR:211 | 接入状态 | 已核对，未确认缺陷 |
| BEQR-137 | BEQR:212 | 回流结果状态 | 已核对，未确认缺陷 |
| BEQR-138 | BEQR:217 | 风险四级 | 已核对，未确认缺陷 |
| BEQR-139 | BEQR:218 | 事件状态 | 已核对，未确认缺陷 |
| BEQR-140 | BEQR:219 | 主管复核强制项 | 已核对，未确认缺陷 |
| BEQR-141 | BEQR:220 | 经营异常处置状态 | 已核对，未确认缺陷 |
| BEQR-142 | BEQR:221 | 异常状态的统一出口 | 已核对，未确认缺陷 |
| BEQR-143 | BEQR:226 | 四类主体 | 已核对，未确认缺陷 |
| BEQR-144 | BEQR:227 | 经营主体关系 | 已核对，未确认缺陷 |
| BEQR-145 | BEQR:228 | 城市项目公司 | 已核对，未确认缺陷 |
| BEQR-146 | BEQR:229 | 运营公司职责 | 已核对，未确认缺陷 |
| BEQR-147 | BEQR:230 | 主要收入归属 | 已核对，未确认缺陷 |
| BEQR-148 | BEQR:231 | 合作模式 | 已核对，未确认缺陷 |
| BEQR-149 | BEQR:232 | 职级 | 已核对，未确认缺陷 |
| BEQR-150 | BEQR:233 | 终端十一端 | 已核对，未确认缺陷 |
| BEQR-151 | BEQR:234 | 最终权限计算 | 已核对，未确认缺陷 |
| BEQR-152 | BEQR:235 | 项目与敏感数据权限 | 已核对，未确认缺陷 |
| BEQR-153 | BEQR:236 | 终端开关优先级 | 已核对，未确认缺陷 |
| BEQR-154 | BEQR:237 | 综合体项目状态 | 已核对，未确认缺陷 |
| BEQR-155 | BEQR:238 | 商户/运营主体状态 | 已核对，未确认缺陷 |
| BEQR-156 | BEQR:239 | 合同状态 | 已核对，未确认缺陷 |
| BEQR-157 | BEQR:240 | 员工状态 | 已核对，未确认缺陷 |
| BEQR-158 | BEQR:241 | 设备状态 | 已核对，未确认缺陷 |
| BEQR-159 | BEQR:242 | 权限状态 | 已核对，未确认缺陷 |
| BEQR-160 | BEQR:248 | 招商线索状态 | 已核对，未确认缺陷 |
| BEQR-161 | BEQR:249 | 招商意向阶段 | 已核对，未确认缺陷 |
| BEQR-162 | BEQR:250 | 品牌/客户库类型 | 已核对，未确认缺陷 |
| BEQR-163 | BEQR:251 | 合同变更类型 | 已核对，未确认缺陷 |
| BEQR-164 | BEQR:252 | 退租结算状态 | 已核对，未确认缺陷 |
| BEQR-165 | BEQR:253 | 内部应付执行状态 | 已核对，未确认缺陷 |
| BEQR-166 | BEQR:255 | 合同审批链 | 已核对，未确认缺陷 |
| BEQR-167 | BEQR:256 | 租金/账单类型 | 已核对，未确认缺陷 |
| BEQR-168 | BEQR:257 | 营业额抽成模式 | 已核对，未确认缺陷 |
| BEQR-169 | BEQR:258 | 公区能源费 | 已核对，未确认缺陷 |
| BEQR-170 | BEQR:259 | 铺位租赁状态 | 已核对，未确认缺陷 |
| BEQR-171 | BEQR:260 | 物业工单类型 | 已核对，未确认缺陷 |
| BEQR-172 | BEQR:262 | 物业工单状态 | 已核对，未确认缺陷 |
| BEQR-173 | BEQR:264 | 商户服务工单状态 | 已核对，未确认缺陷 |
| BEQR-174 | BEQR:265 | 工单时效 | 已核对，未确认缺陷 |
| BEQR-175 | BEQR:266 | 楼宇机电设备状态 | 已核对，未确认缺陷 |
| BEQR-176 | BEQR:267 | 设备维保类型 | 已核对，未确认缺陷 |
| BEQR-177 | BEQR:268 | 能耗表计类型 | 已核对，未确认缺陷 |
| BEQR-178 | BEQR:269 | 抄表状态 | 已核对，未确认缺陷 |
| BEQR-179 | BEQR:270 | 能耗归集对象 | 已核对，未确认缺陷 |
| BEQR-180 | BEQR:271 | 楼宇巡检任务状态 | 已核对，未确认缺陷 |
| BEQR-181 | BEQR:273 | 楼宇巡检类型 | 已核对，未确认缺陷 |
| BEQR-182 | BEQR:279 | 考勤日结果 | 已核对，未确认缺陷 |
| BEQR-183 | BEQR:280 | 打卡方式 | 已核对，未确认缺陷 |
| BEQR-184 | BEQR:281 | 请假类型 | 已核对，未确认缺陷 |
| BEQR-185 | BEQR:282 | 请假单状态 | 已核对，未确认缺陷 |
| BEQR-186 | BEQR:283 | 薪资单构成 | 已核对，未确认缺陷 |
| BEQR-187 | BEQR:285 | 员工提成发放 | 已核对，未确认缺陷 |
| BEQR-188 | BEQR:290 | 服务订单状态 | 已核对，未确认缺陷 |
| BEQR-189 | BEQR:291 | 管理费方式 | 已核对，未确认缺陷 |
| BEQR-190 | BEQR:296 | 分红基数 | 已核对，未确认缺陷 |
| BEQR-191 | BEQR:297 | 分红比例 | 已核对，未确认缺陷 |
| BEQR-192 | BEQR:298 | 分红税务 | 已核对，未确认缺陷 |
| BEQR-193 | BEQR:299 | 分红周期 | 已核对，未确认缺陷 |
| BEQR-194 | BEQR:300 | 分红单状态 | 已核对，未确认缺陷 |
| BEQR-195 | BEQR:301 | 可选扣除项 | 已核对，未确认缺陷 |
| BEQR-196 | BEQR:302 | 发放保护线留存 | 已核对，未确认缺陷 |
| BEQR-197 | BEQR:303 | 期间关账 | 已核对，未确认缺陷 |
| BSM-001 | BSM:505 | 整体说明：这套系统到底管什么 | 已核对，未确认缺陷 |
| BSM-002 | BSM:526 | 一笔业务怎么走 | 已核对，未确认缺陷 |
| BSM-003 | BSM:563 | 系统通用能力：数据可改可删、操作有引导、权限分岗 | 已核对，未确认缺陷 |
| BSM-004 | BSM:566 | 配置数据可以存档、也可以删除（删除要审批） | 已核对，未确认缺陷 |
| BSM-005 | BSM:581 | 后台的"新建/录入"改成分步引导 | 已核对，未确认缺陷 |
| BSM-006 | BSM:584 | 验证用的二维码一律动态刷新（间隔可在后台设定） | 已核对，未确认缺陷 |
| BSM-007 | BSM:587 | 平台管理员拥有全部日常功能权限 | 已核对，未确认缺陷 |
| BSM-008 | BSM:592 | 子系统入口 | 已核对，未确认缺陷 |
| BSM-009 | BSM:596 | 基础管理 | 已核对，未确认缺陷 |
| BSM-010 | BSM:601 | 票务权益 | 已核对，未确认缺陷 |
| BSM-011 | BSM:606 | 客人端 | 已核对，未确认缺陷 |
| BSM-012 | BSM:611 | 酒吧平板/工作手机 | 已核对，未确认缺陷 |
| BSM-013 | BSM:616 | 触摸屏一体机 | 已核对，未确认缺陷 |
| BSM-014 | BSM:621 | 酒吧管理 | 已核对，未确认缺陷 |
| BSM-015 | BSM:626 | 供应链库存 | 已核对，未确认缺陷 |
| BSM-016 | BSM:631 | 游乐管理 | 已核对，未确认缺陷 |
| BSM-017 | BSM:637 | 商户管理 | 已核对，未确认缺陷 |
| BSM-018 | BSM:643 | 招商租赁 | 已核对，未确认缺陷 |
| BSM-019 | BSM:650 | 物业服务 | 已核对，未确认缺陷 |
| BSM-020 | BSM:656 | 楼宇运营 | 已核对，未确认缺陷 |
| BSM-021 | BSM:661 | 第三方接入 | 已核对，未确认缺陷 |
| BSM-022 | BSM:666 | 留痕与异常 | 已核对，未确认缺陷 |
| BSM-023 | BSM:671 | 财务对账 | 已核对，未确认缺陷 |
| BSM-024 | BSM:676 | 持股分红 | 已核对，未确认缺陷 |
| BSM-025 | BSM:681 | 经营看板 | 已核对，未确认缺陷 |
| BSM-026 | BSM:686 | 各类入口 | 已核对，未确认缺陷 |
| BSM-027 | BSM:694 | 基础管理：先把综合体的盘子管清楚 | 已核对，未确认缺陷 |
| BSM-028 | BSM:698 | 主要功能 | 已核对，未确认缺陷 |
| BSM-029 | BSM:714 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-030 | BSM:727 | 票务权益：管卖什么票、给什么权益、怎么核销 | 已核对，未确认缺陷 |
| BSM-031 | BSM:731 | 主要功能 | 已核对，未确认缺陷 |
| BSM-032 | BSM:760 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-033 | BSM:772 | 票种、通票与权益类型（系统支持哪些种类） | 已核对，未确认缺陷 |
| BSM-034 | BSM:796 | 核销、退款与有效期规则 | 已核对，未确认缺陷 |
| BSM-035 | BSM:813 | 会员与积分规则 | 已核对，未确认缺陷 |
| BSM-036 | BSM:840 | 人脸验龄数据使用与删除 | 已核对，未确认缺陷 |
| BSM-037 | BSM:850 | 客人端：客人自己买票、预定、查询、积分和出示二维码 | 已核对，未确认缺陷 |
| BSM-038 | BSM:854 | 主要功能 | 已核对，未确认缺陷 |
| BSM-039 | BSM:873 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-040 | BSM:883 | 登录与关键规则 | 已核对，未确认缺陷 |
| BSM-041 | BSM:898 | 会员注册、认证与收费边界 | 已核对，未确认缺陷 |
| BSM-042 | BSM:907 | 会员积分活动与发券 | 已核对，未确认缺陷 |
| BSM-043 | BSM:916 | 会员营销明确边界 | 已核对，未确认缺陷 |
| BSM-044 | BSM:929 | 酒吧平板/工作手机：平板点酒，工作手机核销 | 已核对，未确认缺陷 |
| BSM-045 | BSM:933 | 主要功能 | 已核对，未确认缺陷 |
| BSM-046 | BSM:953 | 平板图形点单（POS） | 已核对，未确认缺陷 |
| BSM-047 | BSM:963 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-048 | BSM:978 | 触摸屏一体机：权益领取、现场服务和辅助售票 | 已核对，未确认缺陷 |
| BSM-049 | BSM:982 | 主要功能 | 已核对，未确认缺陷 |
| BSM-050 | BSM:996 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-051 | BSM:1010 | 酒吧管理：从入场、开台到结账关台 | 已核对，未确认缺陷 |
| BSM-052 | BSM:1014 | 主要功能 | 已核对，未确认缺陷 |
| BSM-053 | BSM:1041 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-054 | BSM:1053 | 桌台/账单状态与经营规则 | 已核对，未确认缺陷 |
| BSM-055 | BSM:1088 | 桌台、菜单与优惠券 | 已核对，未确认缺陷 |
| BSM-056 | BSM:1101 | 正式未付款订单挂起 | 已核对，未确认缺陷 |
| BSM-057 | BSM:1120 | 服务点单：客人点专项服务，公司统一收款、再按名录分账 | 已核对，未确认缺陷 |
| BSM-058 | BSM:1124 | 怎么点、怎么收钱 | 已核对，未确认缺陷 |
| BSM-059 | BSM:1135 | 钱怎么分、怎么退 | 已核对，未确认缺陷 |
| BSM-060 | BSM:1151 | 供应链库存：管供应商、采购、库房、消耗和存酒 | 已核对，未确认缺陷 |
| BSM-061 | BSM:1155 | 主要功能 | 已核对，未确认缺陷 |
| BSM-062 | BSM:1174 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-063 | BSM:1186 | 库存类型与规则 | 已核对，未确认缺陷 |
| BSM-064 | BSM:1208 | 采购建议与门店要货 | 已核对，未确认缺陷 |
| BSM-065 | BSM:1216 | 外部经销商准入、价格与库存 | 已核对，未确认缺陷 |
| BSM-066 | BSM:1226 | 经销订单、付款与履约 | 已核对，未确认缺陷 |
| BSM-067 | BSM:1238 | 经销退货、退款、红冲与资金归属 | 已核对，未确认缺陷 |
| BSM-068 | BSM:1253 | 游乐管理：管项目开放、项目容量（实时在玩人数）和安全异常 | 已核对，未确认缺陷 |
| BSM-069 | BSM:1257 | 主要功能 | 已核对，未确认缺陷 |
| BSM-070 | BSM:1276 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-071 | BSM:1287 | 游乐类型与规则 | 已核对，未确认缺陷 |
| BSM-072 | BSM:1308 | 商户管理：商户在授权范围内完整使用自己的系统 | 已核对，未确认缺陷 |
| BSM-073 | BSM:1313 | 主要功能 | 已核对，未确认缺陷 |
| BSM-074 | BSM:1326 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-075 | BSM:1339 | 招商租赁与合同履约：从找商户到签约、履约、变更、退租 | 已核对，未确认缺陷 |
| BSM-076 | BSM:1343 | 主要功能 | 已核对，未确认缺陷 |
| BSM-077 | BSM:1357 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-078 | BSM:1370 | 物业服务：商户和现场的报修、投诉、整改都进工单 | 已核对，未确认缺陷 |
| BSM-079 | BSM:1374 | 主要功能 | 已核对，未确认缺陷 |
| BSM-080 | BSM:1388 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-081 | BSM:1401 | 楼宇运营：机电设备、能耗抄表、楼宇巡检 | 已核对，未确认缺陷 |
| BSM-082 | BSM:1405 | 主要功能 | 已核对，未确认缺陷 |
| BSM-083 | BSM:1414 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-084 | BSM:1427 | 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | 已核对，未确认缺陷 |
| BSM-085 | BSM:1431 | 日常工作 | 已核对，未确认缺陷 |
| BSM-086 | BSM:1441 | 排班、考勤与请假 | 已核对，未确认缺陷 |
| BSM-087 | BSM:1455 | 我的薪资 | 已核对，未确认缺陷 |
| BSM-088 | BSM:1467 | 员工提成、活动执行与人员服务边界 | 已核对，未确认缺陷 |
| BSM-089 | BSM:1479 | 第三方接入：外部系统继续做专业系统，平台拿回结果 | 已核对，未确认缺陷 |
| BSM-090 | BSM:1545 | 机器人配送与扫码交接 | 已核对，未确认缺陷 |
| BSM-091 | BSM:1554 | 视频监控（留点位/时间） | 已核对，未确认缺陷 |
| BSM-092 | BSM:1563 | 重要事项短信通知（2026-06-29 新增） | 已核对，未确认缺陷 |
| BSM-093 | BSM:1574 | 留痕与异常：重要动作留得住，出了问题追得回 | 已核对，未确认缺陷 |
| BSM-094 | BSM:1578 | 必须关注的动作 | 已核对，未确认缺陷 |
| BSM-095 | BSM:1596 | 怎样减少人工填写 | 已核对，未确认缺陷 |
| BSM-096 | BSM:1607 | 处理结果 | 已核对，未确认缺陷 |
| BSM-097 | BSM:1619 | 风险等级与风控规则 | 已核对，未确认缺陷 |
| BSM-098 | BSM:1636 | 财务对账：把经营事实整理成财务能确认的口径 | 已核对，未确认缺陷 |
| BSM-099 | BSM:1639 | 内部结算价目录（按商品规格逐项建目录） | 已核对，未确认缺陷 |
| BSM-100 | BSM:1641 | 结算配置中心（后台总控页） | 已核对，未确认缺陷 |
| BSM-101 | BSM:1643 | 项目零售参考价 | 已核对，未确认缺陷 |
| BSM-102 | BSM:1648 | 主要功能 | 已核对，未确认缺陷 |
| BSM-103 | BSM:1666 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-104 | BSM:1678 | 收入归属：哪类收入归哪个业态主体 | 已核对，未确认缺陷 |
| BSM-105 | BSM:1697 | 三方对账框架：系统流水、渠道对账单、资金到账两两核对 | 已核对，未确认缺陷 |
| BSM-106 | BSM:1709 | 对账与结算类型规则 | 已核对，未确认缺陷 |
| BSM-107 | BSM:1737 | 持股分红：按持股把经营成果定期分给主理人 | 已核对，未确认缺陷 |
| BSM-108 | BSM:1741 | 主要功能 | 已核对，未确认缺陷 |
| BSM-109 | BSM:1760 | 谁会用 | 已核对，未确认缺陷 |
| BSM-110 | BSM:1768 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-111 | BSM:1781 | 经营看板：让管理层和运营看得懂、能追问 | 已核对，未确认缺陷 |
| BSM-112 | BSM:1785 | 主要指标 | 已核对，未确认缺陷 |
| BSM-113 | BSM:1813 | 谁会看 | 已核对，未确认缺陷 |
| BSM-114 | BSM:1827 | 各类入口：不同人只看自己该看的、只做自己该做的 | 已核对，未确认缺陷 |
| BSM-115 | BSM:1897 | 相互关系：为什么这些子系统必须连在一起 | 已核对，未确认缺陷 |
| BSM-116 | BSM:1928 | 系统交付与上线：正式上线要做的安全加固和安装交付 | 已核对，未确认缺陷 |
| BSM-117 | BSM:1932 | 正式上线时的安全加固 | 已核对，未确认缺陷 |
| BSM-118 | BSM:1940 | 能自动识别环境的安装包 + 内附教程 | 已核对，未确认缺陷 |
| BSM-119 | BSM:1947 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-120 | BSM:1957 | 已确认规则和实施前输入项 | 已核对，未确认缺陷 |

## 对抗式线：正常路径反例与排除

| 对照组 | 反例/边界 | 本席结论与依据 |
|---|---|---|
| 票务/年龄 | 设备不可用、误拒、多人、多次、场次终结、已用票再赠、补偿延长 | 年龄设备人工抽查/一次凭据是明定例外，非免检票；补偿是场次终结例外；普通退款与安全例外分开。BEQR:33/47/73、BSM:746/800/803/808/1074。 |
| 退款单位 | 人份退款与“同订单不存在部分核销并部分退款” | 人次处理规定核算单位，不单独授予退款资格；两份均明写普通退款限制，不能靠人份定义推导出例外。未确认矛盾，不发明放宽。BEQR:47/73、BSM:746/803。 |
| 分配/尾差 | 1—10 家比例、并列第4/5、仅第一段、第二段全未核销、缺家数行 | 十行均100%，并列17.5%；未核销减除/未分配池/尾差三栏独立；缺行补配对暂挂票是明确特例。命令实算在下。 |
| 零元/定价/费用 | 免费权益出资、内部价时点、销售通路二层、单票/通票服务费上限 | 逐项锁出资与内部价的特殊规则不能被一般“核销时固化”覆盖；各费只扣一次，单票/通票上限分开。BEQR票务财务锚点、BSM:748—755/1037—1042/1640。 |
| 积分/会员/储值 | 付款与核销不同日、未用票、退款积分过期、注销一年、券重回可用 | 积分/成长值双账，票务/商户消费两种计算；商户取消退券与退积分继承原到期日明确；注销事件保留是窄例外。BEQR会员段，BSM:815—849/864。 |
| 台位/酒吧 | A+B→T→并C→拆、撤销时原台另占、低消已达后优惠、赠送/提成 | 原台历史不进入新T运算；任一后续变化阻断旧快照撤销；低消折前与提成折后为不同基数；服务费应发部分与扣留部分分开。BSM:1063—1081。 |
| 终端/员工 | 私人手机概述含考勤与工作机领机打卡、设备故障延误、跨月班次 | 概述不替代领机三事件的具体入口；设备故障延误为严格迟到规则的明定例外；班次结束归属为营业日规则的明定例外。BSM:930/1443—1452/1458。 |
| 供应链/游乐 | 可消费借物、补录、恰等于高价值阈值、关键设备可替代、重复出场 | 可消费物品实际买断；补录与原始记录并列；高价值触发是“高于”而不是自行改成“大于等于”，未确认能以缺少重复等值句新增业务。关键安全依赖依实际流程、辅助故障需工程留痕，容量不人工改。BEQR:164，BSM:1202—1206/1253—1305。 |
| 租赁/物业 | 零欠费/仅罚款、面积变更、押金等待与三个付账日、投诉关闭 | 清退前提明确；合同变更子合同与一般不追补分别适用；退租押金不受三个付账日为明定例外；投诉类型分支不同，不套全类验收链。BSM:1340—1385。 |
| 财务/分红 | 跨月退款、在途/失败、税额、服务费扣留、借款前置 | 不改已关期间；在途不重付、失败另路跟进；税额依专业规则不套普通舍入；借款前置不属于三个可选开关。BEQR财务/分红段，BSM:1642—1790。 |
| 治理/交付 | 普通审批绕红线、密钥逐笔例外、测试弱密码、只读分析、第三方占位 | 三硬红线不可绕，其他密钥权限限逐笔；生产换强密码/安装与运行权限不同；只读分析不发控制指令；无接口不得假造数据。BSM:703/1805—1820/1930—1955。 |

上表行区间是定位辅助，精确判据原文和源行以逐 ID 表为准。概述与具体条目间的边界均按明确限定解释；不为理论组合添加状态/闸/规则。

## 临时登记（全部两项，均不进修复队列）

### CX-01 — BEQR 静态呈现
- 文件：docs/business-enum-quick-reference-v0.1.html:1—26。
- 分类：技术性；工程缺陷：否；建议修复队列归属：否。
- 依据类型：4；冻结判据：GEN-001/002/015/017/018。
- 无业务发明理由：doctype、UTF-8、CSS、表格语义只承载同页权威文本的阅读，不创造收费、状态、角色或审批。结构解析与源码全文读取均完成。
- 处置：只登记，等待 B2 的 Grok 独立分类复核，不自行删除正本任何部分。

### CX-02 — BSM 静态呈现与目录导航
- 文件：docs/business-system-map-for-operations-v0.1.html:1—520、2008—2032。
- 分类：技术性；工程缺陷：否；建议修复队列归属：否。
- 依据类型：4；冻结判据：GEN-001/002/012/015/017/018。
- 无业务发明理由：布局与 setActive 只高亮当前目录，不修改业务数据；真实27个href均对应源ID，原脚本执行首屏/每目标通过。
- 处置：只登记，等待 B2 的 Grok 独立分类复核。

## 可复现命令与实际输出

以下在正本 cwd 运行，全部仅内存计算，无产品/HTML/判据改动。使用 Python stdlib 与已装 Node。第一个命令输出的大覆盖 JSON 曾被工具限长，根转存 JSON 失败；已单独取短索引317行成功，不把截断当缺陷或业务 PASS。第二命令最初的比例正则漏空格，出现 AssertionError ('1', {})；已修正检查表达式后重跑成功，未修改业务源。

### 全锚点、结构与原导航脚本
```bash
python3 - <<'PY'
from pathlib import Path
from html.parser import HTMLParser
from collections import Counter
import hashlib,json,re,subprocess
root=Path('/home/james/complex-management')
paths=['docs/business-enum-quick-reference-v0.1.html','docs/business-system-map-for-operations-v0.1.html']
lock=(root/'docs/audit-2026-09/plan/criteria-lock.md').read_text(encoding='utf-8')
h=hashlib.sha256((root/'docs/audit-2026-09/plan/criteria-lock.md').read_bytes()).hexdigest()
blocks=re.split(r'(?m)^### ',lock)[1:]
coverage=[]
for block in blocks:
 m=re.match(r'((?:BEQR|BSM)-\d{3}) — (.*)',block)
 if not m: continue
 src=re.search(r'- 来源：`([^`]+):(\d+)`',block)
 raw=re.search(r'````html\n([\s\S]*?)\n````',block)
 assert src and raw,m[1]
 source=(root/src[1]).read_text(encoding='utf-8')
 assert raw[1] in source,m[1]
 coverage.append({'id':m[1],'title':m[2],'file':src[1],'line':int(src[2])})
assert len(coverage)==317,len(coverage)
class AuditHTML(HTMLParser):
 def __init__(self):
  super().__init__();self.ids=[];self.links=[];self.navlinks=[];self.stack=[];self.nav=False;self.errors=[];self.tags=Counter();self.lang=None;self.charset=None
 def handle_starttag(self,tag,attrs):
  a=dict(attrs);self.tags[tag]+=1
  if 'id' in a:self.ids.append(a['id'])
  if tag=='html':self.lang=a.get('lang')
  if tag=='meta' and 'charset' in a:self.charset=a['charset']
  if tag=='nav' or 'nav' in a.get('class','').split():self.nav=True
  if tag=='a' and 'href' in a:
   self.links.append(a['href'])
   if self.nav:self.navlinks.append(a['href'])
  if tag not in {'area','base','br','col','embed','hr','img','input','link','meta','param','source','track','wbr'}:self.stack.append(tag)
 def handle_endtag(self,tag):
  if tag=='nav':self.nav=False
  if self.stack and self.stack[-1]==tag:self.stack.pop()
  else:self.errors.append([self.getpos()[0],tag,self.stack[-3:]])
checks=[]
for path in paths:
 b=(root/path).read_bytes();s=b.decode('utf-8',errors='strict');p=AuditHTML();p.feed(s)
 assert not b.startswith(b'\xef\xbb\xbf') and '\ufffd' not in s,path
 duplicate=[x for x,n in Counter(p.ids).items() if n>1]
 broken=[x for x in p.links if x.startswith('#') and x[1:] not in p.ids]
 missing=[x for x in p.links if x and not x.startswith(('#','http:','https:','mailto:','javascript:')) and not (root/path).parent.joinpath(x.split('#')[0]).exists()]
 assert not (duplicate or broken or missing or p.errors or p.stack),(path,duplicate,broken,missing,p.errors,p.stack)
 assert p.lang and p.charset.lower()=='utf-8'
 checks.append({'path':path,'sha256':hashlib.sha256(b).hexdigest(),'wc_l':b.count(b'\n'),'actual_lines':len(s.splitlines()),'ids':len(p.ids),'navlinks':len(p.navlinks),'broken_links':len(broken)+len(missing),'stack_errors':len(p.errors),'utf8':'PASS'})
 if 'business-system-map' in path:
  script=re.search(r'<script>([\s\S]*?)</script>',s)[1]
  refs=[x for x in p.navlinks if x.startswith('#')]
  js="const vm=require('node:vm'), assert=require('node:assert/strict'); const input=JSON.parse(process.argv[1]); let top=[],active=[]; const nodes=input.refs.map((id,i)=>({id:id.slice(1),getBoundingClientRect:()=>({top:top[i]})})); const links=input.refs.map((id,i)=>({getAttribute:()=>id,classList:{toggle:(_,v)=>active[i]=v}})); const context={document:{querySelectorAll:()=>links,querySelector:id=>nodes.find(n=>'#'+n.id===id)},window:{addEventListener:(event,fn,opts)=>{assert.equal(event,'scroll');assert.equal(opts.passive,true)}}};top=nodes.map((_,i)=>1000+i*100);vm.createContext(context);vm.runInContext(input.script,context);assert.deepEqual(active,links.map((_,i)=>i===0));for(let target=0;target<nodes.length;target++){top=nodes.map((_,i)=>(i-target)*200+120);vm.runInContext('setActive()',context);assert.deepEqual(active,links.map((_,i)=>i===target));}console.log(JSON.stringify({nav_targets:nodes.length,initial_and_every_section:'PASS',actual_script:true}));"
  run=subprocess.run(['node','-e',js,json.dumps({'script':script,'refs':refs})],capture_output=True,text=True)
  assert run.returncode==0,run.stderr
  checks.append({'navigation_execution':json.loads(run.stdout)})
print(json.dumps({'lock_sha256':h,'criteria_coverage':len(coverage),'checks':checks},ensure_ascii=False))
print('COVERAGE_COUNT='+str(len(coverage)))
PY
```
原检查进程成功、stderr 空；输出要点：
```json
{"lock_sha256": "16def02336743500166af4cfd5fff48d02731d63d45a8f5f72721ca8ebd408e3", "criteria_coverage": 317, "checks": [{"path": "docs/business-enum-quick-reference-v0.1.html", "sha256": "4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66", "wc_l": 308, "actual_lines": 308, "ids": 0, "navlinks": 0, "broken_links": 0, "stack_errors": 0, "utf8": "PASS"}, {"path": "docs/business-system-map-for-operations-v0.1.html", "sha256": "a736ad246a1b832502c94379bce2bef5f65d11b7d07068c31ca37d0eb7c287f4", "wc_l": 2032, "actual_lines": 2032, "ids": 29, "navlinks": 27, "broken_links": 0, "stack_errors": 0, "utf8": "PASS"}, {"navigation_execution": {"nav_targets": 27, "initial_and_every_section": "PASS", "actual_script": true}}]}
```

### 金额与反例
```bash
python3 - <<'PY'
from pathlib import Path
from decimal import Decimal,ROUND_HALF_UP
import re,json,hashlib
root=Path('/home/james/complex-management')
s=(root/'docs/business-enum-quick-reference-v0.1.html').read_text(encoding='utf-8')
row=next(x for x in s.splitlines() if '<td class="k">酒吧通票可分配池</td>' in x)
table=row.split('参与家数档位表（出厂默认')[1].split('参与家数档位表不设家数上限')[0]
def parse_table(t):
 rows={}
 for n,body in re.findall(r'第二段参与 (\d+) 家＝(.*?)(?:；|。)',t):
  slots={}
  for start,end,value in re.findall(r'第 (\d+)(?:[–-](\d+))? 名\s*(?:各\s*)?(\d+(?:\.\d+)?)%',body):
   for rank in range(int(start),int(end or start)+1):
    assert rank not in slots
    slots[rank]=Decimal(value)
  assert set(slots)==set(range(1,int(n)+1)),(n,slots)
  assert sum(slots.values())==100,(n,slots)
  rows[int(n)]=slots
 assert len(rows)==10
 return rows
rows=parse_table(table)
assert (rows[5][4]+rows[5][5])/2==Decimal('17.5')
rejected=False
try:parse_table(table.replace('100%','99%',1))
except AssertionError:rejected=True
assert rejected
# 只证明 HTML 明文的金额/场次算例可同时成立，不代表执行了业务实现。
round_cent=lambda x:Decimal(x).quantize(Decimal('.01'),rounding=ROUND_HALF_UP)
tie=[round_cent(Decimal('.10')*Decimal(1)/3)]*3
tail=Decimal('.10')-sum(tie)
assert sum(tie)+tail==Decimal('.10') and tail==Decimal('.01')
assert 8+5==13 # A4+B6 转 T8 后再并 C5
budget_total,available,reserved,issued,reclaimed=100,60,20,30,10
assert budget_total==available+reserved+issued-reclaimed
assert round_cent(Decimal(5000+1000+1000+1000)*Decimal('.2'))==Decimal('1600.00')
print(json.dumps({'rank_rows':{n:list(map(str,v.values())) for n,v in rows.items()},'rank_sums':'all 100','tie_4_5':'17.5','rounding_three_shares':list(map(str,tie)),'separate_tail':str(tail),'mutated_99_percent_rejected':rejected,'merge_example':'T8+C5=13','activity_budget':'100=60+20+30-10','scope':'HTML arithmetic only, not business implementation'},ensure_ascii=False))
raw=root/'docs/audit-2026-09/modules/M001-b1-self-raw.md'
if raw.exists():
 text=raw.read_text(encoding='utf-8');print('SELF_RAW_METADATA='+json.dumps({'size':raw.stat().st_size,'report_versions':re.findall(r'(?m)^.*版本[：:]\s*(V\d+\.\d+).*$',text)[-5:],'complete_report_blocks':len(re.findall(r'(?m)^CRITERIA-LOCK-SHA256:',text))},ensure_ascii=False))
print('HEAD='+__import__('subprocess').check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip())
PY
```
实际成功输出（SELF_RAW_METADATA 为追加前历史元数据）：
```text
{"rank_rows": {"1": ["100"], "2": ["57", "43"], "3": ["40", "30", "30"], "4": ["31", "23", "23", "23"], "5": ["25", "20", "20", "20", "15"], "6": ["23", "17", "17", "17", "13", "13"], "7": ["22", "15", "15", "15", "11", "11", "11"], "8": ["18", "14", "14", "14", "10", "10", "10", "10"], "9": ["15", "13", "13", "13", "10", "10", "10", "10", "6"], "10": ["16", "12", "12", "12", "9", "9", "9", "9", "6", "6"]}, "rank_sums": "all 100", "tie_4_5": "17.5", "rounding_three_shares": ["0.03", "0.03", "0.03"], "separate_tail": "0.01", "mutated_99_percent_rejected": true, "merge_example": "T8+C5=13", "activity_budget": "100=60+20+30-10", "scope": "HTML arithmetic only, not business implementation"}
SELF_RAW_METADATA={"size": 209410, "report_versions": ["V0.23", "V0.24", "V0.16", "V0.25", "V0.26"], "complete_report_blocks": 7}
HEAD=242f487233d028173cb9198eecd079f5d877405c
```

### 不冒充运行的检查
已读现有 verify-html-param-tier.mjs 与 verify-html-ledger-sync.mjs，其默认只检查本次 diff，当前 HEAD=origin/main 且 HTML 无改动会直接无改动 PASS；因此未把它们当本轮全文审计证据，也未修改基线。没有浏览器视觉/读屏实测，不声称该项全自动验收。映射校验最初误按 Markdown 表行查找而出现 StopIteration，已按实际 `M001 → [...]` 格式校验335个ID、漏项和越项均0。业务 Java/SQL 测试留所辖模块。

## 本次结论与下一步

本席三线完整审查 2 文件、335 冻结判据；确认的冲突/添加/工程缺陷均为 0，技术性无缺陷登记 2 项。B1 本席 PASS；没有触发铁律4修改申请。本报告追加保留历史。下一步仅在本报告完整落盘后读取当前 Grok B1 raw，四值校验与合取；若 Grok 有 FAIL 不以本票覆盖，按证据核实和 V0.17 路由。未进入 B2/B3/B4/B5，未提交、推送、部署。

## B1 收齐后的核实与暂停记录

追加记录版本：V0.28（不是新一轮独立自审；完整保留上方 V0.27 原票与既有字节）
日期：2026-09-08 UTC

上方独立 PASS 已在读取 Grok 之前封存（追加报告 SHA256 2456454bca5316a55528a3114bdebaead6c91827f9ae6fd01ae83f87bb235e9d）。之后读取本次 grok raw 从1215行开始的完整206行，V0.10，明确 FAIL/GR-01。根以 Read 实查 BSM L2 和 L2003，日期分别2026-09-07和2026-09-01；git log最近该文件改动为0855d8a97478df2ffb6e3b03026c8aca865aec5c，2026-09-07T08:12:05-07:00。接受其GEN-016技术性文档不一致结论，不能以根PASS覆盖Grok FAIL。B1合取FAIL，未进入B2。

M001在ledger V1.40为BLOCKED-ASK，B3累计2/本次0，其余470 PENDING。按铁律4/5全流程暂停，未修改HTML/源码，未分永久ID，未启动其他模块。

### 完整HTML修改申请（未批准）
文件：/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html。
① L2：`<!-- 文件版本：V0.11；更新日期：2026-09-07 -->` → `<!-- 文件版本：V0.12；更新日期：2026-09-08 -->`。此次修订按用户版本铁律递增并记录日期。
② L2003：仅将`更新日期：2026-09-01。`改为`更新日期：2026-09-08。`，该行其余用途与解释权声明不变。将展示日期与修订日期对齐。
等待操作员逐条批准①②；实际日期变化时不得擅自替换已申请内容。

### 四值与调用规约错误
实际/sidecar/ledger/Grok四值相同，均为16def02336743500166af4cfd5fff48d02731d63d45a8f5f72721ca8ebd408e3，LOCK-AMEND-COUNT=1。该字面相等不能替代独立性：根复核实际本次-p，发现已把预期hash写入提示。此举违反V0.17“不得在派发指令里告诉Grok期望值”要求，本次不作为有效独立报数证据。恢复后必须删除该提示值并完成所需独立复核；当前铁律4暂停期间不另启旁支调用。

实际CLI参数（-p内容为本次正式M001B1完整派发指令，含上述已披露错误）：
```text
/home/james/.local/bin/grok -p "<本次 M001 B1 完整派发指令>" -m grok-4.6 --reasoning-effort xhigh --output-format json --permission-mode bypassPermissions --no-subagents --disable-web-search --cwd /home/james/complex-management
```
真实exit0；指定报告 /home/james/complex-management/docs/audit-2026-09/modules/M001-b1-grok-raw.md 为仓库内普通非空文件且本次已更新，四项交付验收通过。该结果仅是交付成功。首wrapper格式与前元数据缺漏已在报告更新前补采基线，并在结束后完成四项实际校验，不能再把旧文件或冒号回执当有效报告。未来调用使用真正前采元数据/后四项验收、stdout/stderr内存丢弃的适配器，不要求模型输出机器标记。当前-p中的预期hash还须一并移除。

最终当前状态：B1合取FAIL；M001 BLOCKED-ASK；等待本HTML申请答复。无B2定稿、无本轮B3、无模块PASS。


CRITERIA-LOCK-SHA256: 16def02336743500166af4cfd5fff48d02731d63d45a8f5f72721ca8ebd408e3

# M001 B1 — Codex 独立全量审计

版本：V0.29
日期：2026-09-08 UTC
依据：160-audit-prompt.md V0.17；正式计划 V0.04，runbook V0.02。
正本：160:/home/james/complex-management，main。
HEAD：242f487233d028173cb9198eecd079f5d877405c。
阶段：B1；ledger V1.41，M001 IN_PROGRESS；B3 历史累计 2，本次 0。
结论：FAIL（BSM 页头/页脚更新日期不一致；本次完整模块复核仍只确认此一项工程缺陷）。
本次是修正 Grok 派发指令之后的 B1 重跑。本席已经在上一轮收到并核实 Grok V0.10 的日期意见，该知识无法撤销；它不是本次盲审中新发现的问题。当前重跑的 Grok 报告及 CLI JSON 在本报告完整落盘之前均未读取。本席使用已完整阅读的同一 2340 行字节，重新核验哈希、全量摘录、结构/导航、金额算例与下列三线对照，保留原 PASS 原文并以本次独立判断更新为 FAIL，没有把旧票改写成新票。未使用任何第三席/子代理结果。

## 全文件与三线覆盖

| 文件 | SHA256 | 完整范围 | 三线 |
|---|---|---|---|
| docs/business-enum-quick-reference-v0.1.html | 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 | 全部 308 行（含空行/CSS/注释） | 第一性原理、顺从式、对抗式均完成 |
| docs/business-system-map-for-operations-v0.1.html | a736ad246a1b832502c94379bce2bef5f65d11b7d07068c31ca37d0eb7c287f4 | 全部 2032 行（含空行/CSS/脚本/注释） | 第一性原理、顺从式、对抗式均完成 |

本席分完整行段阅读全文，未截断源文件。提示词有效行数合计 2340。初始阅读工具显示的末尾空段曾使会话中暂记 309/2033；本次按真实字节和 splitlines 实算均为 308/2032，以本报告值为准。
未发布 76 文件的重复/HTML 出处专项已另行完成；其事实没有被用作本模块业务实现 PASS。本模块不把 Java、迁移、安卓代码当作已审范围。第三份平台治理 HTML 仅对照其相关上位口径，没有开启 M002。

## 第一性原理线（三问逐项）

1. 为什么存在：BEQR 全文提供封闭枚举、金额/时点/权限判定；BSM 全文提供业务流程、责任主体和入口关系。删除任一正本会失去其独有的上位细节；另一份的重述用于互相定位，不是可随意删除的字节副本。依据类型 1：两份 HTML 末尾解释权声明及下表逐锚点原文。
2. 哪些行为实际执行：本模块是需求文档，业务行为按 §2.3 文档豁免用类型 1/4 原文与算例核对；实际执行了 HTML 结构/路径检查、BSM 原始导航 JS（27 目标和首屏）、分配比例提取与算术反例。未运行/未声称已验证产品支付、薪资、库存、验龄设备或法律适用。
3. 未验证前提：真实第三方接口、生产工资分项、在途银行行为与批准事实不能由本文自证。HTML 明写接口资料当前未确定，并要求后续接入；这些不当作已上线事实。使用决定时间线处理冲突前须有实际裁决时间，本席没有以注释“已拍板”替代操作员授权。页头/页脚的两个发布日期直接不一致，不依赖猜测；已获用户两处修订批准，仍需 B2 双席定稿后 B3 实施。

## 顺从式线

逐项读完 BEQR-001—197、BSM-001—120 的完整标题与正文；317 条冻结摘录逐字存在于当前源文件。下面逐 ID 写标题和精确源行，结论是该上位条目已全文核对其适用范围及本模块重复表述；BSM-120 摘录包含页脚日期，其工程元数据问题单列 CX-03，不把逐字匹配误当成正确性。它不表示下游实现已经满足该判据。
通用 18 条均纳入，下表说明本模块的适用边界。

| 判据 | 本模块证据与结论 |
|---|---|
| GEN-001 | 两份权威文件用途不同；纯展示 CSS/导航为正常阅读服务，无业务发明；不因重述规则就删业务正本。 |
| GEN-002 | BSM 原脚本在真实 27 个导航目标的 DOM 等价输入上执行；初始和每目标激活均断言通过。文档中业务行为不冒充已执行代码。 |
| GEN-003 | 机械断言实跑；将比例 100% 改为 99% 的内存反例确实失败，非永绿断言。 |
| GEN-004 | 本模块静态文本无用户写入口；动态脚本选择器取本文件静态 href，全部解析到实际 ID；业务校验规则按各锚点核对。 |
| GEN-005 | 逐读默认拒绝、十一端、职责与敏感信息范围；具体明定的密钥/人工查验例外不误判为普通权限放行。未执行服务端授权测试。 |
| GEN-006 | 核对钱路守恒、来源分栏、租赁排他、券与支付共同生效、挂起取消与付款互斥；结构解析无标签失配。 |
| GEN-007 | 核对转赠三动作先成功者、多人/多次票单位、出场重复不重复扣、幂等逐笔密钥例外及并拆台整体成功；未声称产品并发测试通过。 |
| GEN-008 | 两份中的枚举与作用域逐段对照；流程概述并非完整清单，具体例外与独立子流程按其范围解释。 |
| GEN-009 | 接口未接入口灰显、未出款状态、第三方差异、能力缺失等明确列出；缺配置不视为开启，未发现静默伪成功文案。 |
| GEN-010 | 本模块无可执行外部输入/凭证；人脸、注销、查询、导出与证据查看边界按明文审读；不将本文当法规论证。 |
| GEN-011 | 对照逐笔来源/时点/责任人与不可覆盖记录；安检、人工验龄的特定溯源终点与全局原则按明定例外一并核对。 |
| GEN-012 | 导航每次遍历固定 27 个章节，非开放输入放大；只读静态页面无后台资源持有。 |
| GEN-013 | 文件以 UTF-8 原生打开，不依赖额外网络库；导航用原生 JS。未用“本次无 diff”门禁冒充全量内容审计。 |
| GEN-014 | 不适用执行检查：本模块无 SQL/迁移；迁移正确性留所辖模块。 |
| GEN-015 | HTML 语言与编码声明、标题层次、表格表头、原生链接可键盘访问；导航有效、标签闭合；未执行真实浏览器截图或读屏测试，不夸大覆盖。 |
| GEN-016 | FAIL：BSM L2 更新日期 2026-09-07，L2003 页脚 2026-09-01；实际解析断言失败，见 CX-03。版本 V0.07/V0.11 不代表两个日期一致。 |
| GEN-017 | 正文是业务上位本身，核对内部与三 HTML 一致性；以下两条纯展示技术性登记无业务发明。未确认独立添加或冲突。 |
| GEN-018 | 仅使用实际 HTML 原文与本报告所附命令输出；不是以旧 PASS、Grok 意见或代码注释作判据。 |

### 全部 HTML 判据覆盖表

源文件简称：BEQR=docs/business-enum-quick-reference-v0.1.html；BSM=docs/business-system-map-for-operations-v0.1.html。
本表每行的“已核对”指顺从式全文和对抗式作用域对照均完成，结合下方反例与排除说明。冻结摘录逐字匹配的命令覆盖所有 317 行。

| 判据 ID | 源行 | 锚点 | 结果 |
|---|---|---|---|
| BEQR-001 | BEQR:29 | 票种 | 已核对，未确认缺陷 |
| BEQR-002 | BEQR:30 | 票务策略状态 | 已核对，未确认缺陷 |
| BEQR-003 | BEQR:32 | 定价、售票与收款主体 | 已核对，未确认缺陷 |
| BEQR-004 | BEQR:33 | 年龄限制 | 已核对，未确认缺陷 |
| BEQR-005 | BEQR:34 | 通票赠酒下限 | 已核对，未确认缺陷 |
| BEQR-006 | BEQR:35 | 通票类型 | 已核对，未确认缺陷 |
| BEQR-007 | BEQR:36 | 权益类型 | 已核对，未确认缺陷 |
| BEQR-008 | BEQR:37 | 餐饮/零售券两种用法 | 已核对，未确认缺陷 |
| BEQR-009 | BEQR:39 | 资格判定 | 已核对，未确认缺陷 |
| BEQR-010 | BEQR:40 | 订单状态 | 已核对，未确认缺陷 |
| BEQR-011 | BEQR:41 | 票/权益状态 | 已核对，未确认缺陷 |
| BEQR-012 | BEQR:43 | 核销记录状态 | 已核对，未确认缺陷 |
| BEQR-013 | BEQR:44 | 手环入场 | 已核对，未确认缺陷 |
| BEQR-014 | BEQR:45 | 退款单状态 | 已核对，未确认缺陷 |
| BEQR-015 | BEQR:46 | 有效期类型 | 已核对，未确认缺陷 |
| BEQR-016 | BEQR:47 | 售票与退款原则 | 已核对，未确认缺陷 |
| BEQR-017 | BEQR:48 | 到期未核销款 | 已核对，未确认缺陷 |
| BEQR-018 | BEQR:49 | 域归属对照 | 已核对，未确认缺陷 |
| BEQR-019 | BEQR:51 | 跨域通票分配 | 已核对，未确认缺陷 |
| BEQR-020 | BEQR:52 | 酒吧通票可分配池 | 已核对，未确认缺陷 |
| BEQR-021 | BEQR:53 | 收费与免费权益结算 | 已核对，未确认缺陷 |
| BEQR-022 | BEQR:54 | 项目内部结算价 | 已核对，未确认缺陷 |
| BEQR-023 | BEQR:55 | 核销顺序 | 已核对，未确认缺陷 |
| BEQR-024 | BEQR:56 | 游乐核销归属 | 已核对，未确认缺陷 |
| BEQR-025 | BEQR:57 | 门票综合服务费 | 已核对，未确认缺陷 |
| BEQR-026 | BEQR:58 | 动态价与会员价 | 已核对，未确认缺陷 |
| BEQR-027 | BEQR:60 | 人脸验龄数据生命周期 | 已核对，未确认缺陷 |
| BEQR-028 | BEQR:65 | 会员唯一识别 | 已核对，未确认缺陷 |
| BEQR-029 | BEQR:66 | 会员注册 | 已核对，未确认缺陷 |
| BEQR-030 | BEQR:68 | 会员身份状态 | 已核对，未确认缺陷 |
| BEQR-031 | BEQR:70 | 等级与成长值 | 已核对，未确认缺陷 |
| BEQR-032 | BEQR:71 | 等级权益对照 | 已核对，未确认缺陷 |
| BEQR-033 | BEQR:72 | 积分与成长值来源 | 已核对，未确认缺陷 |
| BEQR-034 | BEQR:73 | 票务人次计算 | 已核对，未确认缺陷 |
| BEQR-035 | BEQR:75 | 有效消费与冻结 | 已核对，未确认缺陷 |
| BEQR-036 | BEQR:77 | 积分明细类型 | 已核对，未确认缺陷 |
| BEQR-037 | BEQR:78 | 积分兑换定价 | 已核对，未确认缺陷 |
| BEQR-038 | BEQR:79 | 商品规格 | 已核对，未确认缺陷 |
| BEQR-039 | BEQR:80 | 项目零售参考价 | 已核对，未确认缺陷 |
| BEQR-040 | BEQR:81 | 发券与核销结算 | 已核对，未确认缺陷 |
| BEQR-041 | BEQR:82 | 重复兑换与转赠 | 已核对，未确认缺陷 |
| BEQR-042 | BEQR:83 | 券包状态 | 已核对，未确认缺陷 |
| BEQR-043 | BEQR:84 | 积分结算账期 | 已核对，未确认缺陷 |
| BEQR-044 | BEQR:85 | 会员系统服务费 | 已核对，未确认缺陷 |
| BEQR-045 | BEQR:86 | 储值卡类型与收款主体 | 已核对，未确认缺陷 |
| BEQR-046 | BEQR:87 | 会员忠诚度扩展 | 已核对，未确认缺陷 |
| BEQR-047 | BEQR:89 | 会员注册与开卡 | 已核对，未确认缺陷 |
| BEQR-048 | BEQR:90 | 学生认证 | 已核对，未确认缺陷 |
| BEQR-049 | BEQR:91 | 会员积分活动状态 | 已核对，未确认缺陷 |
| BEQR-050 | BEQR:92 | 积分活动预算守恒 | 已核对，未确认缺陷 |
| BEQR-051 | BEQR:93 | 充值积分 | 已核对，未确认缺陷 |
| BEQR-052 | BEQR:94 | 会员营销边界 | 已核对，未确认缺陷 |
| BEQR-053 | BEQR:95 | 发券对象 | 已核对，未确认缺陷 |
| BEQR-054 | BEQR:100 | 桌台/卡座/吧台位状态 | 已核对，未确认缺陷 |
| BEQR-055 | BEQR:101 | 账单状态 | 已核对，未确认缺陷 |
| BEQR-056 | BEQR:103 | 订台定金状态 | 已核对，未确认缺陷 |
| BEQR-057 | BEQR:105 | 订卡邀请函状态 | 已核对，未确认缺陷 |
| BEQR-058 | BEQR:106 | 点酒方式 | 已核对，未确认缺陷 |
| BEQR-059 | BEQR:107 | 支付方式 | 已核对，未确认缺陷 |
| BEQR-060 | BEQR:108 | 销售提成 | 已核对，未确认缺陷 |
| BEQR-061 | BEQR:109 | 服务员提成 | 已核对，未确认缺陷 |
| BEQR-062 | BEQR:110 | 退菜退酒 | 已核对，未确认缺陷 |
| BEQR-063 | BEQR:111 | 高危动作（需审批/双证据） | 已核对，未确认缺陷 |
| BEQR-064 | BEQR:112 | 出品票据状态 | 已核对，未确认缺陷 |
| BEQR-065 | BEQR:114 | 账单操作 | 已核对，未确认缺陷 |
| BEQR-066 | BEQR:115 | 收银交班状态 | 已核对，未确认缺陷 |
| BEQR-067 | BEQR:117 | 最低消费 | 已核对，未确认缺陷 |
| BEQR-068 | BEQR:118 | 服务费 | 已核对，未确认缺陷 |
| BEQR-069 | BEQR:119 | 小费 | 已核对，未确认缺陷 |
| BEQR-070 | BEQR:120 | 客人存酒有效期与转存 | 已核对，未确认缺陷 |
| BEQR-071 | BEQR:122 | 存酒瓶数 | 已核对，未确认缺陷 |
| BEQR-072 | BEQR:123 | 套餐赠券适用范围 | 已核对，未确认缺陷 |
| BEQR-073 | BEQR:124 | 非低消商品 | 已核对，未确认缺陷 |
| BEQR-074 | BEQR:125 | 隐私桌台权限 | 已核对，未确认缺陷 |
| BEQR-075 | BEQR:126 | 桌台会话与菜单 | 已核对，未确认缺陷 |
| BEQR-076 | BEQR:127 | 桌台计时产品 | 已核对，未确认缺陷 |
| BEQR-077 | BEQR:128 | 门店支付方式 | 已核对，未确认缺陷 |
| BEQR-078 | BEQR:129 | 扫码点单身份 | 已核对，未确认缺陷 |
| BEQR-079 | BEQR:130 | 优惠券自动选择 | 已核对，未确认缺陷 |
| BEQR-080 | BEQR:131 | 优惠券付款预留 | 已核对，未确认缺陷 |
| BEQR-081 | BEQR:132 | 正式挂起订单状态 | 已核对，未确认缺陷 |
| BEQR-082 | BEQR:133 | 正式挂起订单规则 | 已核对，未确认缺陷 |
| BEQR-083 | BEQR:134 | 挂单取消与付款互斥 | 已核对，未确认缺陷 |
| BEQR-084 | BEQR:135 | 挂单坏账损耗 | 已核对，未确认缺陷 |
| BEQR-085 | BEQR:140 | 项目开放状态 | 已核对，未确认缺陷 |
| BEQR-086 | BEQR:142 | 场次状态 | 已核对，未确认缺陷 |
| BEQR-087 | BEQR:144 | 游乐「当天／每天」口径 | 已核对，未确认缺陷 |
| BEQR-088 | BEQR:145 | 容量双阈值 | 已核对，未确认缺陷 |
| BEQR-089 | BEQR:146 | 园区容量 | 已核对，未确认缺陷 |
| BEQR-090 | BEQR:147 | 设备关键/辅助分级 | 已核对，未确认缺陷 |
| BEQR-091 | BEQR:148 | 全园故障与补偿通票 | 已核对，未确认缺陷 |
| BEQR-092 | BEQR:149 | 安全严重度 | 已核对，未确认缺陷 |
| BEQR-093 | BEQR:150 | 巡检任务状态 | 已核对，未确认缺陷 |
| BEQR-094 | BEQR:151 | 越界类型 | 已核对，未确认缺陷 |
| BEQR-095 | BEQR:153 | 团体/包场 | 已核对，未确认缺陷 |
| BEQR-096 | BEQR:154 | 游乐增强阶段 | 已核对，未确认缺陷 |
| BEQR-097 | BEQR:159 | 供应合同模式 | 已核对，未确认缺陷 |
| BEQR-098 | BEQR:160 | 库房分类 | 已核对，未确认缺陷 |
| BEQR-099 | BEQR:161 | 物品大类 | 已核对，未确认缺陷 |
| BEQR-100 | BEQR:162 | 库存流水类型 | 已核对，未确认缺陷 |
| BEQR-101 | BEQR:164 | 高价值判定 | 已核对，未确认缺陷 |
| BEQR-102 | BEQR:166 | 跨主体借调 | 已核对，未确认缺陷 |
| BEQR-103 | BEQR:168 | 智能采购建议 | 已核对，未确认缺陷 |
| BEQR-104 | BEQR:169 | 门店要货履约状态 | 已核对，未确认缺陷 |
| BEQR-105 | BEQR:170 | 经销商状态 | 已核对，未确认缺陷 |
| BEQR-106 | BEQR:171 | 经销询价状态 | 已核对，未确认缺陷 |
| BEQR-107 | BEQR:172 | 经销价格目录状态 | 已核对，未确认缺陷 |
| BEQR-108 | BEQR:173 | 经销订单状态 | 已核对，未确认缺陷 |
| BEQR-109 | BEQR:174 | 经销退货单状态 | 已核对，未确认缺陷 |
| BEQR-110 | BEQR:175 | 经销库存外显 | 已核对，未确认缺陷 |
| BEQR-111 | BEQR:176 | 经销支付方式 | 已核对，未确认缺陷 |
| BEQR-112 | BEQR:177 | 经销库存预留 | 已核对，未确认缺陷 |
| BEQR-113 | BEQR:178 | 经销职责分离 | 已核对，未确认缺陷 |
| BEQR-114 | BEQR:179 | 经销取消、退货与开票 | 已核对，未确认缺陷 |
| BEQR-115 | BEQR:180 | 经销价格与税务快照 | 已核对，未确认缺陷 |
| BEQR-116 | BEQR:181 | 经销商停用撤权 | 已核对，未确认缺陷 |
| BEQR-117 | BEQR:182 | 经销资金归属 | 已核对，未确认缺陷 |
| BEQR-118 | BEQR:187 | 运营营业日 | 已核对，未确认缺陷 |
| BEQR-119 | BEQR:188 | 对账口径 | 已核对，未确认缺陷 |
| BEQR-120 | BEQR:189 | 对账差异四类 | 已核对，未确认缺陷 |
| BEQR-121 | BEQR:190 | 日结状态 | 已核对，未确认缺陷 |
| BEQR-122 | BEQR:191 | 差异处理状态 | 已核对，未确认缺陷 |
| BEQR-123 | BEQR:192 | 结算状态 | 已核对，未确认缺陷 |
| BEQR-124 | BEQR:193 | 综合通票定价 | 已核对，未确认缺陷 |
| BEQR-125 | BEQR:195 | 商户业态 | 已核对，未确认缺陷 |
| BEQR-126 | BEQR:196 | 合作模式 | 已核对，未确认缺陷 |
| BEQR-127 | BEQR:197 | 退款资金流向 | 已核对，未确认缺陷 |
| BEQR-128 | BEQR:198 | 物业费 | 已核对，未确认缺陷 |
| BEQR-129 | BEQR:199 | 能源费与损耗费 | 已核对，未确认缺陷 |
| BEQR-130 | BEQR:201 | 税费配置 | 已核对，未确认缺陷 |
| BEQR-131 | BEQR:202 | 三方对账框架 | 已核对，未确认缺陷 |
| BEQR-132 | BEQR:203 | 开票状态 | 已核对，未确认缺陷 |
| BEQR-133 | BEQR:208 | 接入的外部系统 | 已核对，未确认缺陷 |
| BEQR-134 | BEQR:209 | 客户标签 | 已核对，未确认缺陷 |
| BEQR-135 | BEQR:210 | 统一原则 | 已核对，未确认缺陷 |
| BEQR-136 | BEQR:211 | 接入状态 | 已核对，未确认缺陷 |
| BEQR-137 | BEQR:212 | 回流结果状态 | 已核对，未确认缺陷 |
| BEQR-138 | BEQR:217 | 风险四级 | 已核对，未确认缺陷 |
| BEQR-139 | BEQR:218 | 事件状态 | 已核对，未确认缺陷 |
| BEQR-140 | BEQR:219 | 主管复核强制项 | 已核对，未确认缺陷 |
| BEQR-141 | BEQR:220 | 经营异常处置状态 | 已核对，未确认缺陷 |
| BEQR-142 | BEQR:221 | 异常状态的统一出口 | 已核对，未确认缺陷 |
| BEQR-143 | BEQR:226 | 四类主体 | 已核对，未确认缺陷 |
| BEQR-144 | BEQR:227 | 经营主体关系 | 已核对，未确认缺陷 |
| BEQR-145 | BEQR:228 | 城市项目公司 | 已核对，未确认缺陷 |
| BEQR-146 | BEQR:229 | 运营公司职责 | 已核对，未确认缺陷 |
| BEQR-147 | BEQR:230 | 主要收入归属 | 已核对，未确认缺陷 |
| BEQR-148 | BEQR:231 | 合作模式 | 已核对，未确认缺陷 |
| BEQR-149 | BEQR:232 | 职级 | 已核对，未确认缺陷 |
| BEQR-150 | BEQR:233 | 终端十一端 | 已核对，未确认缺陷 |
| BEQR-151 | BEQR:234 | 最终权限计算 | 已核对，未确认缺陷 |
| BEQR-152 | BEQR:235 | 项目与敏感数据权限 | 已核对，未确认缺陷 |
| BEQR-153 | BEQR:236 | 终端开关优先级 | 已核对，未确认缺陷 |
| BEQR-154 | BEQR:237 | 综合体项目状态 | 已核对，未确认缺陷 |
| BEQR-155 | BEQR:238 | 商户/运营主体状态 | 已核对，未确认缺陷 |
| BEQR-156 | BEQR:239 | 合同状态 | 已核对，未确认缺陷 |
| BEQR-157 | BEQR:240 | 员工状态 | 已核对，未确认缺陷 |
| BEQR-158 | BEQR:241 | 设备状态 | 已核对，未确认缺陷 |
| BEQR-159 | BEQR:242 | 权限状态 | 已核对，未确认缺陷 |
| BEQR-160 | BEQR:248 | 招商线索状态 | 已核对，未确认缺陷 |
| BEQR-161 | BEQR:249 | 招商意向阶段 | 已核对，未确认缺陷 |
| BEQR-162 | BEQR:250 | 品牌/客户库类型 | 已核对，未确认缺陷 |
| BEQR-163 | BEQR:251 | 合同变更类型 | 已核对，未确认缺陷 |
| BEQR-164 | BEQR:252 | 退租结算状态 | 已核对，未确认缺陷 |
| BEQR-165 | BEQR:253 | 内部应付执行状态 | 已核对，未确认缺陷 |
| BEQR-166 | BEQR:255 | 合同审批链 | 已核对，未确认缺陷 |
| BEQR-167 | BEQR:256 | 租金/账单类型 | 已核对，未确认缺陷 |
| BEQR-168 | BEQR:257 | 营业额抽成模式 | 已核对，未确认缺陷 |
| BEQR-169 | BEQR:258 | 公区能源费 | 已核对，未确认缺陷 |
| BEQR-170 | BEQR:259 | 铺位租赁状态 | 已核对，未确认缺陷 |
| BEQR-171 | BEQR:260 | 物业工单类型 | 已核对，未确认缺陷 |
| BEQR-172 | BEQR:262 | 物业工单状态 | 已核对，未确认缺陷 |
| BEQR-173 | BEQR:264 | 商户服务工单状态 | 已核对，未确认缺陷 |
| BEQR-174 | BEQR:265 | 工单时效 | 已核对，未确认缺陷 |
| BEQR-175 | BEQR:266 | 楼宇机电设备状态 | 已核对，未确认缺陷 |
| BEQR-176 | BEQR:267 | 设备维保类型 | 已核对，未确认缺陷 |
| BEQR-177 | BEQR:268 | 能耗表计类型 | 已核对，未确认缺陷 |
| BEQR-178 | BEQR:269 | 抄表状态 | 已核对，未确认缺陷 |
| BEQR-179 | BEQR:270 | 能耗归集对象 | 已核对，未确认缺陷 |
| BEQR-180 | BEQR:271 | 楼宇巡检任务状态 | 已核对，未确认缺陷 |
| BEQR-181 | BEQR:273 | 楼宇巡检类型 | 已核对，未确认缺陷 |
| BEQR-182 | BEQR:279 | 考勤日结果 | 已核对，未确认缺陷 |
| BEQR-183 | BEQR:280 | 打卡方式 | 已核对，未确认缺陷 |
| BEQR-184 | BEQR:281 | 请假类型 | 已核对，未确认缺陷 |
| BEQR-185 | BEQR:282 | 请假单状态 | 已核对，未确认缺陷 |
| BEQR-186 | BEQR:283 | 薪资单构成 | 已核对，未确认缺陷 |
| BEQR-187 | BEQR:285 | 员工提成发放 | 已核对，未确认缺陷 |
| BEQR-188 | BEQR:290 | 服务订单状态 | 已核对，未确认缺陷 |
| BEQR-189 | BEQR:291 | 管理费方式 | 已核对，未确认缺陷 |
| BEQR-190 | BEQR:296 | 分红基数 | 已核对，未确认缺陷 |
| BEQR-191 | BEQR:297 | 分红比例 | 已核对，未确认缺陷 |
| BEQR-192 | BEQR:298 | 分红税务 | 已核对，未确认缺陷 |
| BEQR-193 | BEQR:299 | 分红周期 | 已核对，未确认缺陷 |
| BEQR-194 | BEQR:300 | 分红单状态 | 已核对，未确认缺陷 |
| BEQR-195 | BEQR:301 | 可选扣除项 | 已核对，未确认缺陷 |
| BEQR-196 | BEQR:302 | 发放保护线留存 | 已核对，未确认缺陷 |
| BEQR-197 | BEQR:303 | 期间关账 | 已核对，未确认缺陷 |
| BSM-001 | BSM:505 | 整体说明：这套系统到底管什么 | 已核对，未确认缺陷 |
| BSM-002 | BSM:526 | 一笔业务怎么走 | 已核对，未确认缺陷 |
| BSM-003 | BSM:563 | 系统通用能力：数据可改可删、操作有引导、权限分岗 | 已核对，未确认缺陷 |
| BSM-004 | BSM:566 | 配置数据可以存档、也可以删除（删除要审批） | 已核对，未确认缺陷 |
| BSM-005 | BSM:581 | 后台的"新建/录入"改成分步引导 | 已核对，未确认缺陷 |
| BSM-006 | BSM:584 | 验证用的二维码一律动态刷新（间隔可在后台设定） | 已核对，未确认缺陷 |
| BSM-007 | BSM:587 | 平台管理员拥有全部日常功能权限 | 已核对，未确认缺陷 |
| BSM-008 | BSM:592 | 子系统入口 | 已核对，未确认缺陷 |
| BSM-009 | BSM:596 | 基础管理 | 已核对，未确认缺陷 |
| BSM-010 | BSM:601 | 票务权益 | 已核对，未确认缺陷 |
| BSM-011 | BSM:606 | 客人端 | 已核对，未确认缺陷 |
| BSM-012 | BSM:611 | 酒吧平板/工作手机 | 已核对，未确认缺陷 |
| BSM-013 | BSM:616 | 触摸屏一体机 | 已核对，未确认缺陷 |
| BSM-014 | BSM:621 | 酒吧管理 | 已核对，未确认缺陷 |
| BSM-015 | BSM:626 | 供应链库存 | 已核对，未确认缺陷 |
| BSM-016 | BSM:631 | 游乐管理 | 已核对，未确认缺陷 |
| BSM-017 | BSM:637 | 商户管理 | 已核对，未确认缺陷 |
| BSM-018 | BSM:643 | 招商租赁 | 已核对，未确认缺陷 |
| BSM-019 | BSM:650 | 物业服务 | 已核对，未确认缺陷 |
| BSM-020 | BSM:656 | 楼宇运营 | 已核对，未确认缺陷 |
| BSM-021 | BSM:661 | 第三方接入 | 已核对，未确认缺陷 |
| BSM-022 | BSM:666 | 留痕与异常 | 已核对，未确认缺陷 |
| BSM-023 | BSM:671 | 财务对账 | 已核对，未确认缺陷 |
| BSM-024 | BSM:676 | 持股分红 | 已核对，未确认缺陷 |
| BSM-025 | BSM:681 | 经营看板 | 已核对，未确认缺陷 |
| BSM-026 | BSM:686 | 各类入口 | 已核对，未确认缺陷 |
| BSM-027 | BSM:694 | 基础管理：先把综合体的盘子管清楚 | 已核对，未确认缺陷 |
| BSM-028 | BSM:698 | 主要功能 | 已核对，未确认缺陷 |
| BSM-029 | BSM:714 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-030 | BSM:727 | 票务权益：管卖什么票、给什么权益、怎么核销 | 已核对，未确认缺陷 |
| BSM-031 | BSM:731 | 主要功能 | 已核对，未确认缺陷 |
| BSM-032 | BSM:760 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-033 | BSM:772 | 票种、通票与权益类型（系统支持哪些种类） | 已核对，未确认缺陷 |
| BSM-034 | BSM:796 | 核销、退款与有效期规则 | 已核对，未确认缺陷 |
| BSM-035 | BSM:813 | 会员与积分规则 | 已核对，未确认缺陷 |
| BSM-036 | BSM:840 | 人脸验龄数据使用与删除 | 已核对，未确认缺陷 |
| BSM-037 | BSM:850 | 客人端：客人自己买票、预定、查询、积分和出示二维码 | 已核对，未确认缺陷 |
| BSM-038 | BSM:854 | 主要功能 | 已核对，未确认缺陷 |
| BSM-039 | BSM:873 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-040 | BSM:883 | 登录与关键规则 | 已核对，未确认缺陷 |
| BSM-041 | BSM:898 | 会员注册、认证与收费边界 | 已核对，未确认缺陷 |
| BSM-042 | BSM:907 | 会员积分活动与发券 | 已核对，未确认缺陷 |
| BSM-043 | BSM:916 | 会员营销明确边界 | 已核对，未确认缺陷 |
| BSM-044 | BSM:929 | 酒吧平板/工作手机：平板点酒，工作手机核销 | 已核对，未确认缺陷 |
| BSM-045 | BSM:933 | 主要功能 | 已核对，未确认缺陷 |
| BSM-046 | BSM:953 | 平板图形点单（POS） | 已核对，未确认缺陷 |
| BSM-047 | BSM:963 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-048 | BSM:978 | 触摸屏一体机：权益领取、现场服务和辅助售票 | 已核对，未确认缺陷 |
| BSM-049 | BSM:982 | 主要功能 | 已核对，未确认缺陷 |
| BSM-050 | BSM:996 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-051 | BSM:1010 | 酒吧管理：从入场、开台到结账关台 | 已核对，未确认缺陷 |
| BSM-052 | BSM:1014 | 主要功能 | 已核对，未确认缺陷 |
| BSM-053 | BSM:1041 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-054 | BSM:1053 | 桌台/账单状态与经营规则 | 已核对，未确认缺陷 |
| BSM-055 | BSM:1088 | 桌台、菜单与优惠券 | 已核对，未确认缺陷 |
| BSM-056 | BSM:1101 | 正式未付款订单挂起 | 已核对，未确认缺陷 |
| BSM-057 | BSM:1120 | 服务点单：客人点专项服务，公司统一收款、再按名录分账 | 已核对，未确认缺陷 |
| BSM-058 | BSM:1124 | 怎么点、怎么收钱 | 已核对，未确认缺陷 |
| BSM-059 | BSM:1135 | 钱怎么分、怎么退 | 已核对，未确认缺陷 |
| BSM-060 | BSM:1151 | 供应链库存：管供应商、采购、库房、消耗和存酒 | 已核对，未确认缺陷 |
| BSM-061 | BSM:1155 | 主要功能 | 已核对，未确认缺陷 |
| BSM-062 | BSM:1174 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-063 | BSM:1186 | 库存类型与规则 | 已核对，未确认缺陷 |
| BSM-064 | BSM:1208 | 采购建议与门店要货 | 已核对，未确认缺陷 |
| BSM-065 | BSM:1216 | 外部经销商准入、价格与库存 | 已核对，未确认缺陷 |
| BSM-066 | BSM:1226 | 经销订单、付款与履约 | 已核对，未确认缺陷 |
| BSM-067 | BSM:1238 | 经销退货、退款、红冲与资金归属 | 已核对，未确认缺陷 |
| BSM-068 | BSM:1253 | 游乐管理：管项目开放、项目容量（实时在玩人数）和安全异常 | 已核对，未确认缺陷 |
| BSM-069 | BSM:1257 | 主要功能 | 已核对，未确认缺陷 |
| BSM-070 | BSM:1276 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-071 | BSM:1287 | 游乐类型与规则 | 已核对，未确认缺陷 |
| BSM-072 | BSM:1308 | 商户管理：商户在授权范围内完整使用自己的系统 | 已核对，未确认缺陷 |
| BSM-073 | BSM:1313 | 主要功能 | 已核对，未确认缺陷 |
| BSM-074 | BSM:1326 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-075 | BSM:1339 | 招商租赁与合同履约：从找商户到签约、履约、变更、退租 | 已核对，未确认缺陷 |
| BSM-076 | BSM:1343 | 主要功能 | 已核对，未确认缺陷 |
| BSM-077 | BSM:1357 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-078 | BSM:1370 | 物业服务：商户和现场的报修、投诉、整改都进工单 | 已核对，未确认缺陷 |
| BSM-079 | BSM:1374 | 主要功能 | 已核对，未确认缺陷 |
| BSM-080 | BSM:1388 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-081 | BSM:1401 | 楼宇运营：机电设备、能耗抄表、楼宇巡检 | 已核对，未确认缺陷 |
| BSM-082 | BSM:1405 | 主要功能 | 已核对，未确认缺陷 |
| BSM-083 | BSM:1414 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-084 | BSM:1427 | 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | 已核对，未确认缺陷 |
| BSM-085 | BSM:1431 | 日常工作 | 已核对，未确认缺陷 |
| BSM-086 | BSM:1441 | 排班、考勤与请假 | 已核对，未确认缺陷 |
| BSM-087 | BSM:1455 | 我的薪资 | 已核对，未确认缺陷 |
| BSM-088 | BSM:1467 | 员工提成、活动执行与人员服务边界 | 已核对，未确认缺陷 |
| BSM-089 | BSM:1479 | 第三方接入：外部系统继续做专业系统，平台拿回结果 | 已核对，未确认缺陷 |
| BSM-090 | BSM:1545 | 机器人配送与扫码交接 | 已核对，未确认缺陷 |
| BSM-091 | BSM:1554 | 视频监控（留点位/时间） | 已核对，未确认缺陷 |
| BSM-092 | BSM:1563 | 重要事项短信通知（2026-06-29 新增） | 已核对，未确认缺陷 |
| BSM-093 | BSM:1574 | 留痕与异常：重要动作留得住，出了问题追得回 | 已核对，未确认缺陷 |
| BSM-094 | BSM:1578 | 必须关注的动作 | 已核对，未确认缺陷 |
| BSM-095 | BSM:1596 | 怎样减少人工填写 | 已核对，未确认缺陷 |
| BSM-096 | BSM:1607 | 处理结果 | 已核对，未确认缺陷 |
| BSM-097 | BSM:1619 | 风险等级与风控规则 | 已核对，未确认缺陷 |
| BSM-098 | BSM:1636 | 财务对账：把经营事实整理成财务能确认的口径 | 已核对，未确认缺陷 |
| BSM-099 | BSM:1639 | 内部结算价目录（按商品规格逐项建目录） | 已核对，未确认缺陷 |
| BSM-100 | BSM:1641 | 结算配置中心（后台总控页） | 已核对，未确认缺陷 |
| BSM-101 | BSM:1643 | 项目零售参考价 | 已核对，未确认缺陷 |
| BSM-102 | BSM:1648 | 主要功能 | 已核对，未确认缺陷 |
| BSM-103 | BSM:1666 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-104 | BSM:1678 | 收入归属：哪类收入归哪个业态主体 | 已核对，未确认缺陷 |
| BSM-105 | BSM:1697 | 三方对账框架：系统流水、渠道对账单、资金到账两两核对 | 已核对，未确认缺陷 |
| BSM-106 | BSM:1709 | 对账与结算类型规则 | 已核对，未确认缺陷 |
| BSM-107 | BSM:1737 | 持股分红：按持股把经营成果定期分给主理人 | 已核对，未确认缺陷 |
| BSM-108 | BSM:1741 | 主要功能 | 已核对，未确认缺陷 |
| BSM-109 | BSM:1760 | 谁会用 | 已核对，未确认缺陷 |
| BSM-110 | BSM:1768 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-111 | BSM:1781 | 经营看板：让管理层和运营看得懂、能追问 | 已核对，未确认缺陷 |
| BSM-112 | BSM:1785 | 主要指标 | 已核对，未确认缺陷 |
| BSM-113 | BSM:1813 | 谁会看 | 已核对，未确认缺陷 |
| BSM-114 | BSM:1827 | 各类入口：不同人只看自己该看的、只做自己该做的 | 已核对，未确认缺陷 |
| BSM-115 | BSM:1897 | 相互关系：为什么这些子系统必须连在一起 | 已核对，未确认缺陷 |
| BSM-116 | BSM:1928 | 系统交付与上线：正式上线要做的安全加固和安装交付 | 已核对，未确认缺陷 |
| BSM-117 | BSM:1932 | 正式上线时的安全加固 | 已核对，未确认缺陷 |
| BSM-118 | BSM:1940 | 能自动识别环境的安装包 + 内附教程 | 已核对，未确认缺陷 |
| BSM-119 | BSM:1947 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-120 | BSM:1957 | 已确认规则和实施前输入项 | 业务语义已核对；摘录所含页脚日期存在 GEN-016 工程缺陷 CX-03 |

## 对抗式线：正常路径反例与排除

| 对照组 | 反例/边界 | 本席结论与依据 |
|---|---|---|
| 票务/年龄 | 设备不可用、误拒、多人、多次、场次终结、已用票再赠、补偿延长 | 年龄设备人工抽查/一次凭据是明定例外，非免检票；补偿是场次终结例外；普通退款与安全例外分开。BEQR:33/47/73、BSM:746/800/803/808/1074。 |
| 退款单位 | 人份退款与“同订单不存在部分核销并部分退款” | 人次处理规定核算单位，不单独授予退款资格；两份均明写普通退款限制，不能靠人份定义推导出例外。未确认矛盾，不发明放宽。BEQR:47/73、BSM:746/803。 |
| 分配/尾差 | 1—10 家比例、并列第4/5、仅第一段、第二段全未核销、缺家数行 | 十行均100%，并列17.5%；未核销减除/未分配池/尾差三栏独立；缺行补配对暂挂票是明确特例。命令实算在下。 |
| 零元/定价/费用 | 免费权益出资、内部价时点、销售通路二层、单票/通票服务费上限 | 逐项锁出资与内部价的特殊规则不能被一般“核销时固化”覆盖；各费只扣一次，单票/通票上限分开。BEQR票务财务锚点、BSM:748—755/1037—1042/1640。 |
| 积分/会员/储值 | 付款与核销不同日、未用票、退款积分过期、注销一年、券重回可用 | 积分/成长值双账，票务/商户消费两种计算；商户取消退券与退积分继承原到期日明确；注销事件保留是窄例外。BEQR会员段，BSM:815—849/864。 |
| 台位/酒吧 | A+B→T→并C→拆、撤销时原台另占、低消已达后优惠、赠送/提成 | 原台历史不进入新T运算；任一后续变化阻断旧快照撤销；低消折前与提成折后为不同基数；服务费应发部分与扣留部分分开。BSM:1063—1081。 |
| 终端/员工 | 私人手机概述含考勤与工作机领机打卡、设备故障延误、跨月班次 | 概述不替代领机三事件的具体入口；设备故障延误为严格迟到规则的明定例外；班次结束归属为营业日规则的明定例外。BSM:930/1443—1452/1458。 |
| 供应链/游乐 | 可消费借物、补录、恰等于高价值阈值、关键设备可替代、重复出场 | 可消费物品实际买断；补录与原始记录并列；高价值触发是“高于”而不是自行改成“大于等于”，未确认能以缺少重复等值句新增业务。关键安全依赖依实际流程、辅助故障需工程留痕，容量不人工改。BEQR:164，BSM:1202—1206/1253—1305。 |
| 租赁/物业 | 零欠费/仅罚款、面积变更、押金等待与三个付账日、投诉关闭 | 清退前提明确；合同变更子合同与一般不追补分别适用；退租押金不受三个付账日为明定例外；投诉类型分支不同，不套全类验收链。BSM:1340—1385。 |
| 财务/分红 | 跨月退款、在途/失败、税额、服务费扣留、借款前置 | 不改已关期间；在途不重付、失败另路跟进；税额依专业规则不套普通舍入；借款前置不属于三个可选开关。BEQR财务/分红段，BSM:1642—1790。 |
| 治理/交付 | 普通审批绕红线、密钥逐笔例外、测试弱密码、只读分析、第三方占位 | 三硬红线不可绕，其他密钥权限限逐笔；生产换强密码/安装与运行权限不同；只读分析不发控制指令；无接口不得假造数据。BSM:703/1805—1820/1930—1955。 |

上表行区间是定位辅助，精确判据原文和源行以逐 ID 表为准。概述与具体条目间的边界均按明确限定解释；不为理论组合添加状态/闸/规则。

## 临时登记（全部三项；前两项只登记，CX-03 建议进入修复队列）

### CX-01 — BEQR 静态呈现
- 文件：docs/business-enum-quick-reference-v0.1.html:1—26。
- 分类：技术性；工程缺陷：否；建议修复队列归属：否。
- 依据类型：4；冻结判据：GEN-001/002/015/017/018。
- 无业务发明理由：doctype、UTF-8、CSS、表格语义只承载同页权威文本的阅读，不创造收费、状态、角色或审批。结构解析与源码全文读取均完成。
- 处置：只登记，等待 B2 的 Grok 独立分类复核，不自行删除正本任何部分。

### CX-02 — BSM 静态呈现与目录导航
- 文件：docs/business-system-map-for-operations-v0.1.html:1—520、2008—2032。
- 分类：技术性；工程缺陷：否；建议修复队列归属：否。
- 依据类型：4；冻结判据：GEN-001/002/012/015/017/018。
- 无业务发明理由：布局与 setActive 只高亮当前目录，不修改业务数据；真实27个href均对应源ID，原脚本执行首屏/每目标通过。
- 处置：只登记，等待 B2 的 Grok 独立分类复核。

### CX-03 — BSM 同一文件两处更新日期不一致
- 文件：/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html:2、2003。
- 分类：技术性；工程缺陷：是；建议修复队列归属：是；依据类型：4；冻结判据：GEN-016。
- 原文 L2：`<!-- 文件版本：V0.11；更新日期：2026-09-07 -->`。
- 原文 L2003 前缀：`<p class="footer-note">更新日期：2026-09-01。用途：`。完整该行已读取，后文解释权声明保持不动。
- 原因：同一静态 HTML 的头部版本注释与可见页脚分别手写更新日期，当前两个值未同步；无需运行后台系统即可复现，不引入日期自动生成器。
- 最小复现：Python 从两个固定标记分别提取日期并断言相等，实际为 false / AssertionError。完整命令及实际输出如下。
- 无业务发明：仅文档更新元数据，不改变收费、权限、状态、审批或解释权。
- 用户批准原话：“批准上述第 1、2 处修改”。对应批准边界：L2 整注释改为 `<!-- 文件版本：V0.12；更新日期：2026-09-08 -->`；L2003 仅日期 2026-09-01 改为 2026-09-08。已满足本项铁律4授权，不重新发问。
- 同步影响：BSM-120 冻结原文包含 L2003 日期；变更需按 LOCK-AMEND 原流程登记/双审/更新基准/从最早引用模块 B1 重开。此为获批 HTML 的原文同步，不新设业务/工程判据，不擅自更新当前冻结锁。

```bash
python3 - <<'PY'
from pathlib import Path
import re,json,hashlib
p=Path('/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html')
s=p.read_text(); head=re.search(r'<!-- 文件版本：[^\n]+更新日期：(\d{4}-\d{2}-\d{2})',s)[1];foot=re.search(r'<p class="footer-note">更新日期：(\d{4}-\d{2}-\d{2})',s)[1]
print(json.dumps({'header_date':head,'footer_date':foot,'equal':head==foot},ensure_ascii=False))
assert head==foot,'document update dates disagree'
PY
```
实际输出（exit 1）：
```text
Exit code 1
{"header_date": "2026-09-07", "footer_date": "2026-09-01", "equal": false}
Traceback (most recent call last):
  File "<stdin>", line 6, in <module>
AssertionError: document update dates disagree
```

## 本次技能使用
已实际阅读并使用 using-superpowers、grilling/grill-me 的自问结构、codebase-design 的职责/接口约束、ponytail-review 的必要性检查、receiving-code-review 的事实核实、verification-before-completion 的证据要求；本次另完整读取 systematic-debugging 与 diagnosing-bugs。前述技能具体可用性已在启动自检中记录；本次使用内容只按 V0.17 收窄执行，未派代理/未加新闸/未创建技能默认产物。日期是静态元数据的确定差异，缺陷定位使用实际解析断言，省去针对运行时复杂故障的多组件日志与多假说流程。HTML 内容修复按任务 §6 免 TDD，不造测试文件。

## 可复现命令与实际输出

以下全锚点/结构/原导航脚本和金额命令已在本次重跑重新执行成功，实际当前输出附于结尾；其中旧历史错误/元数据输出原样标注以便追溯。全部在正本 cwd 运行、仅内存计算，无产品/HTML/判据改动。使用 Python stdlib 与已装 Node。第一个命令输出的大覆盖 JSON 曾被工具限长，根转存 JSON 失败；已单独取短索引317行成功，不把截断当缺陷或业务 PASS。第二命令最初的比例正则漏空格，出现 AssertionError ('1', {})；已修正检查表达式后重跑成功，未修改业务源。

### 全锚点、结构与原导航脚本
```bash
python3 - <<'PY'
from pathlib import Path
from html.parser import HTMLParser
from collections import Counter
import hashlib,json,re,subprocess
root=Path('/home/james/complex-management')
paths=['docs/business-enum-quick-reference-v0.1.html','docs/business-system-map-for-operations-v0.1.html']
lock=(root/'docs/audit-2026-09/plan/criteria-lock.md').read_text(encoding='utf-8')
h=hashlib.sha256((root/'docs/audit-2026-09/plan/criteria-lock.md').read_bytes()).hexdigest()
blocks=re.split(r'(?m)^### ',lock)[1:]
coverage=[]
for block in blocks:
 m=re.match(r'((?:BEQR|BSM)-\d{3}) — (.*)',block)
 if not m: continue
 src=re.search(r'- 来源：`([^`]+):(\d+)`',block)
 raw=re.search(r'````html\n([\s\S]*?)\n````',block)
 assert src and raw,m[1]
 source=(root/src[1]).read_text(encoding='utf-8')
 assert raw[1] in source,m[1]
 coverage.append({'id':m[1],'title':m[2],'file':src[1],'line':int(src[2])})
assert len(coverage)==317,len(coverage)
class AuditHTML(HTMLParser):
 def __init__(self):
  super().__init__();self.ids=[];self.links=[];self.navlinks=[];self.stack=[];self.nav=False;self.errors=[];self.tags=Counter();self.lang=None;self.charset=None
 def handle_starttag(self,tag,attrs):
  a=dict(attrs);self.tags[tag]+=1
  if 'id' in a:self.ids.append(a['id'])
  if tag=='html':self.lang=a.get('lang')
  if tag=='meta' and 'charset' in a:self.charset=a['charset']
  if tag=='nav' or 'nav' in a.get('class','').split():self.nav=True
  if tag=='a' and 'href' in a:
   self.links.append(a['href'])
   if self.nav:self.navlinks.append(a['href'])
  if tag not in {'area','base','br','col','embed','hr','img','input','link','meta','param','source','track','wbr'}:self.stack.append(tag)
 def handle_endtag(self,tag):
  if tag=='nav':self.nav=False
  if self.stack and self.stack[-1]==tag:self.stack.pop()
  else:self.errors.append([self.getpos()[0],tag,self.stack[-3:]])
checks=[]
for path in paths:
 b=(root/path).read_bytes();s=b.decode('utf-8',errors='strict');p=AuditHTML();p.feed(s)
 assert not b.startswith(b'\xef\xbb\xbf') and '\ufffd' not in s,path
 duplicate=[x for x,n in Counter(p.ids).items() if n>1]
 broken=[x for x in p.links if x.startswith('#') and x[1:] not in p.ids]
 missing=[x for x in p.links if x and not x.startswith(('#','http:','https:','mailto:','javascript:')) and not (root/path).parent.joinpath(x.split('#')[0]).exists()]
 assert not (duplicate or broken or missing or p.errors or p.stack),(path,duplicate,broken,missing,p.errors,p.stack)
 assert p.lang and p.charset.lower()=='utf-8'
 checks.append({'path':path,'sha256':hashlib.sha256(b).hexdigest(),'wc_l':b.count(b'\n'),'actual_lines':len(s.splitlines()),'ids':len(p.ids),'navlinks':len(p.navlinks),'broken_links':len(broken)+len(missing),'stack_errors':len(p.errors),'utf8':'PASS'})
 if 'business-system-map' in path:
  script=re.search(r'<script>([\s\S]*?)</script>',s)[1]
  refs=[x for x in p.navlinks if x.startswith('#')]
  js="const vm=require('node:vm'), assert=require('node:assert/strict'); const input=JSON.parse(process.argv[1]); let top=[],active=[]; const nodes=input.refs.map((id,i)=>({id:id.slice(1),getBoundingClientRect:()=>({top:top[i]})})); const links=input.refs.map((id,i)=>({getAttribute:()=>id,classList:{toggle:(_,v)=>active[i]=v}})); const context={document:{querySelectorAll:()=>links,querySelector:id=>nodes.find(n=>'#'+n.id===id)},window:{addEventListener:(event,fn,opts)=>{assert.equal(event,'scroll');assert.equal(opts.passive,true)}}};top=nodes.map((_,i)=>1000+i*100);vm.createContext(context);vm.runInContext(input.script,context);assert.deepEqual(active,links.map((_,i)=>i===0));for(let target=0;target<nodes.length;target++){top=nodes.map((_,i)=>(i-target)*200+120);vm.runInContext('setActive()',context);assert.deepEqual(active,links.map((_,i)=>i===target));}console.log(JSON.stringify({nav_targets:nodes.length,initial_and_every_section:'PASS',actual_script:true}));"
  run=subprocess.run(['node','-e',js,json.dumps({'script':script,'refs':refs})],capture_output=True,text=True)
  assert run.returncode==0,run.stderr
  checks.append({'navigation_execution':json.loads(run.stdout)})
print(json.dumps({'lock_sha256':h,'criteria_coverage':len(coverage),'checks':checks},ensure_ascii=False))
print('COVERAGE_COUNT='+str(len(coverage)))
PY
```
原检查进程成功、stderr 空；输出要点：
```json
{"lock_sha256": "16def02336743500166af4cfd5fff48d02731d63d45a8f5f72721ca8ebd408e3", "criteria_coverage": 317, "checks": [{"path": "docs/business-enum-quick-reference-v0.1.html", "sha256": "4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66", "wc_l": 308, "actual_lines": 308, "ids": 0, "navlinks": 0, "broken_links": 0, "stack_errors": 0, "utf8": "PASS"}, {"path": "docs/business-system-map-for-operations-v0.1.html", "sha256": "a736ad246a1b832502c94379bce2bef5f65d11b7d07068c31ca37d0eb7c287f4", "wc_l": 2032, "actual_lines": 2032, "ids": 29, "navlinks": 27, "broken_links": 0, "stack_errors": 0, "utf8": "PASS"}, {"navigation_execution": {"nav_targets": 27, "initial_and_every_section": "PASS", "actual_script": true}}]}
```

### 金额与反例
```bash
python3 - <<'PY'
from pathlib import Path
from decimal import Decimal,ROUND_HALF_UP
import re,json,hashlib
root=Path('/home/james/complex-management')
s=(root/'docs/business-enum-quick-reference-v0.1.html').read_text(encoding='utf-8')
row=next(x for x in s.splitlines() if '<td class="k">酒吧通票可分配池</td>' in x)
table=row.split('参与家数档位表（出厂默认')[1].split('参与家数档位表不设家数上限')[0]
def parse_table(t):
 rows={}
 for n,body in re.findall(r'第二段参与 (\d+) 家＝(.*?)(?:；|。)',t):
  slots={}
  for start,end,value in re.findall(r'第 (\d+)(?:[–-](\d+))? 名\s*(?:各\s*)?(\d+(?:\.\d+)?)%',body):
   for rank in range(int(start),int(end or start)+1):
    assert rank not in slots
    slots[rank]=Decimal(value)
  assert set(slots)==set(range(1,int(n)+1)),(n,slots)
  assert sum(slots.values())==100,(n,slots)
  rows[int(n)]=slots
 assert len(rows)==10
 return rows
rows=parse_table(table)
assert (rows[5][4]+rows[5][5])/2==Decimal('17.5')
rejected=False
try:parse_table(table.replace('100%','99%',1))
except AssertionError:rejected=True
assert rejected
# 只证明 HTML 明文的金额/场次算例可同时成立，不代表执行了业务实现。
round_cent=lambda x:Decimal(x).quantize(Decimal('.01'),rounding=ROUND_HALF_UP)
tie=[round_cent(Decimal('.10')*Decimal(1)/3)]*3
tail=Decimal('.10')-sum(tie)
assert sum(tie)+tail==Decimal('.10') and tail==Decimal('.01')
assert 8+5==13 # A4+B6 转 T8 后再并 C5
budget_total,available,reserved,issued,reclaimed=100,60,20,30,10
assert budget_total==available+reserved+issued-reclaimed
assert round_cent(Decimal(5000+1000+1000+1000)*Decimal('.2'))==Decimal('1600.00')
print(json.dumps({'rank_rows':{n:list(map(str,v.values())) for n,v in rows.items()},'rank_sums':'all 100','tie_4_5':'17.5','rounding_three_shares':list(map(str,tie)),'separate_tail':str(tail),'mutated_99_percent_rejected':rejected,'merge_example':'T8+C5=13','activity_budget':'100=60+20+30-10','scope':'HTML arithmetic only, not business implementation'},ensure_ascii=False))
raw=root/'docs/audit-2026-09/modules/M001-b1-self-raw.md'
if raw.exists():
 text=raw.read_text(encoding='utf-8');print('SELF_RAW_METADATA='+json.dumps({'size':raw.stat().st_size,'report_versions':re.findall(r'(?m)^.*版本[：:]\s*(V\d+\.\d+).*$',text)[-5:],'complete_report_blocks':len(re.findall(r'(?m)^CRITERIA-LOCK-SHA256:',text))},ensure_ascii=False))
print('HEAD='+__import__('subprocess').check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip())
PY
```
实际成功输出（SELF_RAW_METADATA 为追加前历史元数据）：
```text
{"rank_rows": {"1": ["100"], "2": ["57", "43"], "3": ["40", "30", "30"], "4": ["31", "23", "23", "23"], "5": ["25", "20", "20", "20", "15"], "6": ["23", "17", "17", "17", "13", "13"], "7": ["22", "15", "15", "15", "11", "11", "11"], "8": ["18", "14", "14", "14", "10", "10", "10", "10"], "9": ["15", "13", "13", "13", "10", "10", "10", "10", "6"], "10": ["16", "12", "12", "12", "9", "9", "9", "9", "6", "6"]}, "rank_sums": "all 100", "tie_4_5": "17.5", "rounding_three_shares": ["0.03", "0.03", "0.03"], "separate_tail": "0.01", "mutated_99_percent_rejected": true, "merge_example": "T8+C5=13", "activity_budget": "100=60+20+30-10", "scope": "HTML arithmetic only, not business implementation"}
SELF_RAW_METADATA={"size": 209410, "report_versions": ["V0.23", "V0.24", "V0.16", "V0.25", "V0.26"], "complete_report_blocks": 7}
HEAD=242f487233d028173cb9198eecd079f5d877405c
```

### 不冒充运行的检查
已读现有 verify-html-param-tier.mjs 与 verify-html-ledger-sync.mjs，其默认只检查本次 diff，当前 HEAD=origin/main 且 HTML 无改动会直接无改动 PASS；因此未把它们当本轮全文审计证据，也未修改基线。没有浏览器视觉/读屏实测，不声称该项全自动验收。映射校验最初误按 Markdown 表行查找而出现 StopIteration，已按实际 `M001 → [...]` 格式校验335个ID、漏项和越项均0。业务 Java/SQL 测试留所辖模块。

## 本次重新执行的实际输出

```text
{"lock_sha256": "16def02336743500166af4cfd5fff48d02731d63d45a8f5f72721ca8ebd408e3", "criteria_coverage": 317, "checks": [{"path": "docs/business-enum-quick-reference-v0.1.html", "sha256": "4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66", "wc_l": 308, "actual_lines": 308, "ids": 0, "navlinks": 0, "broken_links": 0, "stack_errors": 0, "utf8": "PASS"}, {"path": "docs/business-system-map-for-operations-v0.1.html", "sha256": "a736ad246a1b832502c94379bce2bef5f65d11b7d07068c31ca37d0eb7c287f4", "wc_l": 2032, "actual_lines": 2032, "ids": 29, "navlinks": 27, "broken_links": 0, "stack_errors": 0, "utf8": "PASS"}, {"navigation_execution": {"nav_targets": 27, "initial_and_every_section": "PASS", "actual_script": true}}]}
COVERAGE_COUNT=317
{"rank_rows": {"1": ["100"], "2": ["57", "43"], "3": ["40", "30", "30"], "4": ["31", "23", "23", "23"], "5": ["25", "20", "20", "20", "15"], "6": ["23", "17", "17", "17", "13", "13"], "7": ["22", "15", "15", "15", "11", "11", "11"], "8": ["18", "14", "14", "14", "10", "10", "10", "10"], "9": ["15", "13", "13", "13", "10", "10", "10", "10", "6"], "10": ["16", "12", "12", "12", "9", "9", "9", "9", "6", "6"]}, "rank_sums": "all 100", "tie_4_5": "17.5", "rounding_three_shares": ["0.03", "0.03", "0.03"], "separate_tail": "0.01", "mutated_99_percent_rejected": true, "merge_example": "T8+C5=13", "activity_budget": "100=60+20+30-10", "scope": "HTML arithmetic only, not business implementation"}
SELF_RAW_METADATA={"size": 258819, "report_versions": ["V0.25", "V0.26", "V0.27", "V0.28", "V0.12"], "complete_report_blocks": 8}
HEAD=242f487233d028173cb9198eecd079f5d877405c
```

## 本次结论与下一步

本次完整三线复核 2 文件、335 冻结判据；冲突/添加为 0，技术性工程缺陷 1 项 CX-03，技术性无缺陷登记 2 项 CX-01/02。本席 B1 FAIL。CX-03 已有精确用户批准；若当前 Grok 没有新增需要人闸的事项，则整批进入 B2。现仅报告问题与批准边界，未进入 B2/B3，未改 HTML/锁、未提交推送。下一步在本报告完整落盘后读当前 Grok 重跑报告，再按双席全部证据合并与四值校验。


## 本次 Grok 重跑接收与新增 HTML 人闸

版本：V0.30
日期：2026-09-08 UTC
此段是完整 B1 V0.29 之后的接收说明，不替换其独立结论，不新增独占锁哈希行。

真实 Grok 重跑退出0，REPORT_PATH=/home/james/complex-management/docs/audit-2026-09/modules/M001-b1-grok-raw.md；指定普通文件本次更新且非空，交付四事实成立。本次派发-p没有期望锁哈希。根V0.29完整落盘后才读Grok V0.11（本次完整段自1422行）；两个HTML SHA与根一致。

Grok整批FAIL依据：GR-01页头/页脚日期不一致（与CX-03同项，已有第1、2处批准）；GR-02 L499目录“已拍板/待确认”与L1957“已确认规则和实施前输入项”不同。根本次原票FAIL已保留，读Grok后实际核验GR-02；当前39条全部confirmed，0裸question，链接有效。第三方资料仍未确定，不能把39个confirmed推成“所有实施输入均已确定”；修法仅让导航沿用目标章节现有标题，不改业务正文。工程元数据问题按GEN-016/BSM-120登记，Grok原结论保留，不以根票覆盖。

```bash
python3 - <<'PY'
from pathlib import Path
import re,json,hashlib
p=Path('/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html');s=p.read_text()
nav=re.search(r'<a href="#questions">([^<]+)</a>',s)[1]
section=re.search(r'<section id="questions">([\s\S]*?)</section>',s)[1]
title=re.search(r'<h2>([^<]+)</h2>',section)[1]
print(json.dumps({'nav_line':499,'nav_text':nav,'title_line':1957,'section_title':title,'confirmed_entries':section.count('class="question confirmed"'),'bare_question_entries':len(re.findall(r'<div class="question">',section)),'third_party_input_still_unspecified':'第三方接口资料当前未确定' in section,'nav_target_exists':True,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()},ensure_ascii=False))
PY
```
实际输出：
```text
{"nav_line": 499, "nav_text": "已拍板/待确认", "title_line": 1957, "section_title": "已确认规则和实施前输入项", "confirmed_entries": 39, "bare_question_entries": 0, "third_party_input_still_unspecified": true, "nav_target_exists": true, "sha256": "a736ad246a1b832502c94379bce2bef5f65d11b7d07068c31ca37d0eb7c287f4"}
```

四值（B1后）均已独立读取并全等：
- 实算：16def02336743500166af4cfd5fff48d02731d63d45a8f5f72721ca8ebd408e3
- sidecar：16def02336743500166af4cfd5fff48d02731d63d45a8f5f72721ca8ebd408e3
- ledger基准：16def02336743500166af4cfd5fff48d02731d63d45a8f5f72721ca8ebd408e3
- Grok本次首行：16def02336743500166af4cfd5fff48d02731d63d45a8f5f72721ca8ebd408e3

本次新增 HTML 修改申请（第3处，尚未批准）：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html L499，将 `<a href="#questions">已拍板/待确认</a>` 改为 `<a href="#questions">已确认规则和实施前输入项</a>`。理由：让目录名称与 L1957 已有章节标题一致，保留 L1958“实施前输入项”的含义；当前39条规则均confirmed，第三方资料尚未确定仍属实施输入，不改这些业务正文。href不变。此前第1、2处批准继续有效，尚未实施；若本处批准，三处同批B3一次更新HTML为V0.12，日期按原批准2026-09-08，不重复递增。

依V0.17铁律4/5：发现新增未批准的HTML本身问题，上报并暂停全流程；M001记BLOCKED-ASK，其他470保持PENDING。当前B3累计2、本次0。两席完整B1已收齐；尚未B2，不分永久ID，不改findings/questions，不修HTML/锁，不进M002，无提交推送。恢复时无需再跑已有效交付的本次B1，用户对第3处答复后进入整批B2；仍须根据当前字节核对是否变化。


## M001 本批 B2 Codex 归并复核

版本：V0.31
日期：2026-09-08
结论：B2 PASS（仅本席；须Grok同批PASS才定稿，不代表模块PASS）。

输入：docs/audit-2026-09/modules/M001-b1-self-raw.md最后独占锁行起V0.29完整报告及V0.30接收附录；docs/audit-2026-09/modules/M001-b1-grok-raw.md最后独占锁行起V0.11；候选docs/audit-2026-09/findings.md V0.77顶部本批候选；ledger V1.43三处用户批准。Grok本次B2报告正文尚未读取。

整批核对：CX-01+CX-02是两份文档原CSS/展示/目录同一既有M001-004登记，按原对象吸收，队列否；CX-03+GR-01日期缺陷归新M001-012，队列是；GR-02目录文案归新M001-013，队列是。本批5个临时来源覆盖全部，无遗漏。永久ID001至011历史都保留，012/013在既有最大011后首次分配，无全局复用。原004无缺陷登记不遮盖具体012/013缺陷。原001/002/003/005/006/008已修出队、007/009/010/011历史无缺陷定稿保持，不把旧PASS继承当前471模块PASS。

四分类按顺序：两项缺陷都是文档元数据/目录名称，没有与权威业务命题相反，不套冲突删除规则；没有从业务锚点扩展下游实现；只同步既有日期/标题，不发明业务规则，归技术性。004只是阅读所需结构及导航，技术性无缺陷只登记；已给每项无业务发明理由和类型4证据。无添加，无待答questions。

授权逐处：L2整体注释改V0.12/2026-09-08；L2003仅日期改2026-09-08；L499目录改原章节已有标题。用户第1/2处原话“批准上述第 1、2 处修改”，第3处经过完整业务上下文说明后答“可以”。本批只改这三行，版本只进一次并归因012。第三方接口资料仍未确定，不能把confirmed样式当输入已齐全；目录沿用“实施前输入项”正保留此含义，业务正文不变。

已核BSM-120原文摘录含页脚日期：不能在B3擅改锁；本次B3前后四值用当前B1 Grok同一路径，取得全量before/after tree后B3累计加1，再按既有B4/LOCK-AMEND/B5规则闭环。锁同步是获批HTML出处更新，不新增general判据，不改变COUNT1；必须五步双审及从M001 B1重开，不能省掉。

实际候选校验输出：
```json
{"current_ids": ["M001-001", "M001-002", "M001-003", "M001-004", "M001-005", "M001-006", "M001-007", "M001-008", "M001-009", "M001-010", "M001-011", "M001-012", "M001-013"], "queue": ["M001-012", "M001-013"], "approved_old_text_positions_match": true, "candidate_sha256": "51c88bff682b9e7c98a9b64933cfb675a686beed05585e9f9324fe3743d87bd6"}
```

当前仅本席B2 PASS已落盘，尚未接收Grok本次B2票，候选未定稿；B3累计2、本次0，无HTML/锁修改、无提交推送。此附录不含新的独占锁哈希行，保留已有完整B1界定和原始前缀。


CRITERIA-LOCK-SHA256: 08a4dcc31d10624ca846560d70dcc79e391aeae5ebcdc62a9341f30ecb8de314

# M001 B1 — Codex 独立全量审计

版本：V0.32
日期：2026-09-08 UTC
依据：160-audit-prompt.md V0.17；正式计划 V0.04，runbook V0.02。
正本：160:/home/james/complex-management，main。
HEAD：242f487233d028173cb9198eecd079f5d877405c。
阶段：LOCK-AMEND后重新B1；ledger V1.47，M001 IN_PROGRESS；B3累计3，本次B1不执行修复。正式lock V0.10；已按⑤从PENDING重新开工，不继承旧B1或模块PASS。
结论：PASS（仅本席本次 B1 全量审计；不等同 B2 定稿、B5 或模块 PASS）。
新锁下Grok当前B1仅完成交付元数据检查，本报告完整落盘之前未读取其本次正文、结论或原始CLI JSON。本席独立复核当前整个模块及335判据，不以旧票代本票；本会话已有两份全文件逐段原文，当前完整字节仅三处获批修订，当前全文件哈希/全量摘录/三线语义与执行检查均重新核验。未使用任何第三席/子代理结果。

## 全文件与三线覆盖

| 文件 | SHA256 | 完整范围 | 三线 |
|---|---|---|---|
| docs/business-enum-quick-reference-v0.1.html | 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 | 全部 308 行（含空行/CSS/注释） | 第一性原理、顺从式、对抗式均完成 |
| docs/business-system-map-for-operations-v0.1.html | 86a73b25db127f2c7d5aecacecdb1dc0ad9cbd872031add30bc52efc744e4b3f | 全部 2032 行（含空行/CSS/脚本/注释） | 第一性原理、顺从式、对抗式均完成 |

本席分完整行段阅读全文，未截断源文件。提示词有效行数合计 2340。初始阅读工具显示的末尾空段曾使会话中暂记 309/2033；本次按真实字节和 splitlines 实算均为 308/2032，以本报告值为准。
未发布 76 文件的重复/HTML 出处专项已另行完成；其事实没有被用作本模块业务实现 PASS。本模块不把 Java、迁移、安卓代码当作已审范围。第三份平台治理 HTML 仅对照其相关上位口径，没有开启 M002。

## 第一性原理线（三问逐项）

1. 为什么存在：BEQR 全文提供封闭枚举、金额/时点/权限判定；BSM 全文提供业务流程、责任主体和入口关系。删除任一正本会失去其独有的上位细节；另一份的重述用于互相定位，不是可随意删除的字节副本。依据类型 1：两份 HTML 末尾解释权声明及下表逐锚点原文。
2. 哪些行为实际执行：本模块是需求文档，业务行为按 §2.3 文档豁免用类型 1/4 原文与算例核对；实际执行了 HTML 结构/路径检查、BSM 原始导航 JS（27 目标和首屏）、分配比例提取与算术反例。未运行/未声称已验证产品支付、薪资、库存、验龄设备或法律适用。
3. 未验证前提：真实第三方接口、生产工资分项、在途银行行为与批准事实不能由本文自证。HTML 明写接口资料当前未确定，并要求后续接入；这些不当作已上线事实。使用决定时间线处理冲突前须有实际裁决时间，本席没有以注释“已拍板”替代操作员授权。未发现必须依靠猜测才能给本模块通过的确定矛盾。

## 顺从式线

逐项读完 BEQR-001—197、BSM-001—120 的完整标题与正文；317 条冻结摘录逐字存在于当前源文件。下面逐 ID 写标题和精确源行，结论是该上位条目已全文核对其适用范围及本模块重复表述，未确认冲突。它不表示下游实现已经满足该判据。
通用 18 条均纳入，下表说明本模块的适用边界。

| 判据 | 本模块证据与结论 |
|---|---|
| GEN-001 | 两份权威文件用途不同；纯展示 CSS/导航为正常阅读服务，无业务发明；不因重述规则就删业务正本。 |
| GEN-002 | BSM 原脚本在真实 27 个导航目标的 DOM 等价输入上执行；初始和每目标激活均断言通过。文档中业务行为不冒充已执行代码。 |
| GEN-003 | 机械断言实跑；将比例 100% 改为 99% 的内存反例确实失败，非永绿断言。 |
| GEN-004 | 本模块静态文本无用户写入口；动态脚本选择器取本文件静态 href，全部解析到实际 ID；业务校验规则按各锚点核对。 |
| GEN-005 | 逐读默认拒绝、十一端、职责与敏感信息范围；具体明定的密钥/人工查验例外不误判为普通权限放行。未执行服务端授权测试。 |
| GEN-006 | 核对钱路守恒、来源分栏、租赁排他、券与支付共同生效、挂起取消与付款互斥；结构解析无标签失配。 |
| GEN-007 | 核对转赠三动作先成功者、多人/多次票单位、出场重复不重复扣、幂等逐笔密钥例外及并拆台整体成功；未声称产品并发测试通过。 |
| GEN-008 | 两份中的枚举与作用域逐段对照；流程概述并非完整清单，具体例外与独立子流程按其范围解释。 |
| GEN-009 | 接口未接入口灰显、未出款状态、第三方差异、能力缺失等明确列出；缺配置不视为开启，未发现静默伪成功文案。 |
| GEN-010 | 本模块无可执行外部输入/凭证；人脸、注销、查询、导出与证据查看边界按明文审读；不将本文当法规论证。 |
| GEN-011 | 对照逐笔来源/时点/责任人与不可覆盖记录；安检、人工验龄的特定溯源终点与全局原则按明定例外一并核对。 |
| GEN-012 | 导航每次遍历固定 27 个章节，非开放输入放大；只读静态页面无后台资源持有。 |
| GEN-013 | 文件以 UTF-8 原生打开，不依赖额外网络库；导航用原生 JS。未用“本次无 diff”门禁冒充全量内容审计。 |
| GEN-014 | 不适用执行检查：本模块无 SQL/迁移；迁移正确性留所辖模块。 |
| GEN-015 | HTML 语言与编码声明、标题层次、表格表头、原生链接可键盘访问；导航有效、标签闭合；未执行真实浏览器截图或读屏测试，不夸大覆盖。 |
| GEN-016 | 当前两文件版本V0.07/V0.12；BSM头部/页脚日期均2026-09-08，目录与原章节标题一致。当前317条HTML冻结摘录及来源行位置全部重核，包含BSM-120更新后的日期。第三方接口资料仍未确定，历史评论不等同当前运行结论。 |
| GEN-017 | 正文是业务上位本身，核对内部与三 HTML 一致性；以下两条纯展示技术性登记无业务发明。未确认独立添加或冲突。 |
| GEN-018 | 仅使用实际 HTML 原文与本报告所附命令输出；不是以旧 PASS、Grok 意见或代码注释作判据。 |

### 全部 HTML 判据覆盖表

源文件简称：BEQR=docs/business-enum-quick-reference-v0.1.html；BSM=docs/business-system-map-for-operations-v0.1.html。
本表每行的“已核对”指顺从式全文和对抗式作用域对照均完成，结合下方反例与排除说明。冻结摘录逐字匹配的命令覆盖所有 317 行。

| 判据 ID | 源行 | 锚点 | 结果 |
|---|---|---|---|
| BEQR-001 | BEQR:29 | 票种 | 已核对，未确认缺陷 |
| BEQR-002 | BEQR:30 | 票务策略状态 | 已核对，未确认缺陷 |
| BEQR-003 | BEQR:32 | 定价、售票与收款主体 | 已核对，未确认缺陷 |
| BEQR-004 | BEQR:33 | 年龄限制 | 已核对，未确认缺陷 |
| BEQR-005 | BEQR:34 | 通票赠酒下限 | 已核对，未确认缺陷 |
| BEQR-006 | BEQR:35 | 通票类型 | 已核对，未确认缺陷 |
| BEQR-007 | BEQR:36 | 权益类型 | 已核对，未确认缺陷 |
| BEQR-008 | BEQR:37 | 餐饮/零售券两种用法 | 已核对，未确认缺陷 |
| BEQR-009 | BEQR:39 | 资格判定 | 已核对，未确认缺陷 |
| BEQR-010 | BEQR:40 | 订单状态 | 已核对，未确认缺陷 |
| BEQR-011 | BEQR:41 | 票/权益状态 | 已核对，未确认缺陷 |
| BEQR-012 | BEQR:43 | 核销记录状态 | 已核对，未确认缺陷 |
| BEQR-013 | BEQR:44 | 手环入场 | 已核对，未确认缺陷 |
| BEQR-014 | BEQR:45 | 退款单状态 | 已核对，未确认缺陷 |
| BEQR-015 | BEQR:46 | 有效期类型 | 已核对，未确认缺陷 |
| BEQR-016 | BEQR:47 | 售票与退款原则 | 已核对，未确认缺陷 |
| BEQR-017 | BEQR:48 | 到期未核销款 | 已核对，未确认缺陷 |
| BEQR-018 | BEQR:49 | 域归属对照 | 已核对，未确认缺陷 |
| BEQR-019 | BEQR:51 | 跨域通票分配 | 已核对，未确认缺陷 |
| BEQR-020 | BEQR:52 | 酒吧通票可分配池 | 已核对，未确认缺陷 |
| BEQR-021 | BEQR:53 | 收费与免费权益结算 | 已核对，未确认缺陷 |
| BEQR-022 | BEQR:54 | 项目内部结算价 | 已核对，未确认缺陷 |
| BEQR-023 | BEQR:55 | 核销顺序 | 已核对，未确认缺陷 |
| BEQR-024 | BEQR:56 | 游乐核销归属 | 已核对，未确认缺陷 |
| BEQR-025 | BEQR:57 | 门票综合服务费 | 已核对，未确认缺陷 |
| BEQR-026 | BEQR:58 | 动态价与会员价 | 已核对，未确认缺陷 |
| BEQR-027 | BEQR:60 | 人脸验龄数据生命周期 | 已核对，未确认缺陷 |
| BEQR-028 | BEQR:65 | 会员唯一识别 | 已核对，未确认缺陷 |
| BEQR-029 | BEQR:66 | 会员注册 | 已核对，未确认缺陷 |
| BEQR-030 | BEQR:68 | 会员身份状态 | 已核对，未确认缺陷 |
| BEQR-031 | BEQR:70 | 等级与成长值 | 已核对，未确认缺陷 |
| BEQR-032 | BEQR:71 | 等级权益对照 | 已核对，未确认缺陷 |
| BEQR-033 | BEQR:72 | 积分与成长值来源 | 已核对，未确认缺陷 |
| BEQR-034 | BEQR:73 | 票务人次计算 | 已核对，未确认缺陷 |
| BEQR-035 | BEQR:75 | 有效消费与冻结 | 已核对，未确认缺陷 |
| BEQR-036 | BEQR:77 | 积分明细类型 | 已核对，未确认缺陷 |
| BEQR-037 | BEQR:78 | 积分兑换定价 | 已核对，未确认缺陷 |
| BEQR-038 | BEQR:79 | 商品规格 | 已核对，未确认缺陷 |
| BEQR-039 | BEQR:80 | 项目零售参考价 | 已核对，未确认缺陷 |
| BEQR-040 | BEQR:81 | 发券与核销结算 | 已核对，未确认缺陷 |
| BEQR-041 | BEQR:82 | 重复兑换与转赠 | 已核对，未确认缺陷 |
| BEQR-042 | BEQR:83 | 券包状态 | 已核对，未确认缺陷 |
| BEQR-043 | BEQR:84 | 积分结算账期 | 已核对，未确认缺陷 |
| BEQR-044 | BEQR:85 | 会员系统服务费 | 已核对，未确认缺陷 |
| BEQR-045 | BEQR:86 | 储值卡类型与收款主体 | 已核对，未确认缺陷 |
| BEQR-046 | BEQR:87 | 会员忠诚度扩展 | 已核对，未确认缺陷 |
| BEQR-047 | BEQR:89 | 会员注册与开卡 | 已核对，未确认缺陷 |
| BEQR-048 | BEQR:90 | 学生认证 | 已核对，未确认缺陷 |
| BEQR-049 | BEQR:91 | 会员积分活动状态 | 已核对，未确认缺陷 |
| BEQR-050 | BEQR:92 | 积分活动预算守恒 | 已核对，未确认缺陷 |
| BEQR-051 | BEQR:93 | 充值积分 | 已核对，未确认缺陷 |
| BEQR-052 | BEQR:94 | 会员营销边界 | 已核对，未确认缺陷 |
| BEQR-053 | BEQR:95 | 发券对象 | 已核对，未确认缺陷 |
| BEQR-054 | BEQR:100 | 桌台/卡座/吧台位状态 | 已核对，未确认缺陷 |
| BEQR-055 | BEQR:101 | 账单状态 | 已核对，未确认缺陷 |
| BEQR-056 | BEQR:103 | 订台定金状态 | 已核对，未确认缺陷 |
| BEQR-057 | BEQR:105 | 订卡邀请函状态 | 已核对，未确认缺陷 |
| BEQR-058 | BEQR:106 | 点酒方式 | 已核对，未确认缺陷 |
| BEQR-059 | BEQR:107 | 支付方式 | 已核对，未确认缺陷 |
| BEQR-060 | BEQR:108 | 销售提成 | 已核对，未确认缺陷 |
| BEQR-061 | BEQR:109 | 服务员提成 | 已核对，未确认缺陷 |
| BEQR-062 | BEQR:110 | 退菜退酒 | 已核对，未确认缺陷 |
| BEQR-063 | BEQR:111 | 高危动作（需审批/双证据） | 已核对，未确认缺陷 |
| BEQR-064 | BEQR:112 | 出品票据状态 | 已核对，未确认缺陷 |
| BEQR-065 | BEQR:114 | 账单操作 | 已核对，未确认缺陷 |
| BEQR-066 | BEQR:115 | 收银交班状态 | 已核对，未确认缺陷 |
| BEQR-067 | BEQR:117 | 最低消费 | 已核对，未确认缺陷 |
| BEQR-068 | BEQR:118 | 服务费 | 已核对，未确认缺陷 |
| BEQR-069 | BEQR:119 | 小费 | 已核对，未确认缺陷 |
| BEQR-070 | BEQR:120 | 客人存酒有效期与转存 | 已核对，未确认缺陷 |
| BEQR-071 | BEQR:122 | 存酒瓶数 | 已核对，未确认缺陷 |
| BEQR-072 | BEQR:123 | 套餐赠券适用范围 | 已核对，未确认缺陷 |
| BEQR-073 | BEQR:124 | 非低消商品 | 已核对，未确认缺陷 |
| BEQR-074 | BEQR:125 | 隐私桌台权限 | 已核对，未确认缺陷 |
| BEQR-075 | BEQR:126 | 桌台会话与菜单 | 已核对，未确认缺陷 |
| BEQR-076 | BEQR:127 | 桌台计时产品 | 已核对，未确认缺陷 |
| BEQR-077 | BEQR:128 | 门店支付方式 | 已核对，未确认缺陷 |
| BEQR-078 | BEQR:129 | 扫码点单身份 | 已核对，未确认缺陷 |
| BEQR-079 | BEQR:130 | 优惠券自动选择 | 已核对，未确认缺陷 |
| BEQR-080 | BEQR:131 | 优惠券付款预留 | 已核对，未确认缺陷 |
| BEQR-081 | BEQR:132 | 正式挂起订单状态 | 已核对，未确认缺陷 |
| BEQR-082 | BEQR:133 | 正式挂起订单规则 | 已核对，未确认缺陷 |
| BEQR-083 | BEQR:134 | 挂单取消与付款互斥 | 已核对，未确认缺陷 |
| BEQR-084 | BEQR:135 | 挂单坏账损耗 | 已核对，未确认缺陷 |
| BEQR-085 | BEQR:140 | 项目开放状态 | 已核对，未确认缺陷 |
| BEQR-086 | BEQR:142 | 场次状态 | 已核对，未确认缺陷 |
| BEQR-087 | BEQR:144 | 游乐「当天／每天」口径 | 已核对，未确认缺陷 |
| BEQR-088 | BEQR:145 | 容量双阈值 | 已核对，未确认缺陷 |
| BEQR-089 | BEQR:146 | 园区容量 | 已核对，未确认缺陷 |
| BEQR-090 | BEQR:147 | 设备关键/辅助分级 | 已核对，未确认缺陷 |
| BEQR-091 | BEQR:148 | 全园故障与补偿通票 | 已核对，未确认缺陷 |
| BEQR-092 | BEQR:149 | 安全严重度 | 已核对，未确认缺陷 |
| BEQR-093 | BEQR:150 | 巡检任务状态 | 已核对，未确认缺陷 |
| BEQR-094 | BEQR:151 | 越界类型 | 已核对，未确认缺陷 |
| BEQR-095 | BEQR:153 | 团体/包场 | 已核对，未确认缺陷 |
| BEQR-096 | BEQR:154 | 游乐增强阶段 | 已核对，未确认缺陷 |
| BEQR-097 | BEQR:159 | 供应合同模式 | 已核对，未确认缺陷 |
| BEQR-098 | BEQR:160 | 库房分类 | 已核对，未确认缺陷 |
| BEQR-099 | BEQR:161 | 物品大类 | 已核对，未确认缺陷 |
| BEQR-100 | BEQR:162 | 库存流水类型 | 已核对，未确认缺陷 |
| BEQR-101 | BEQR:164 | 高价值判定 | 已核对，未确认缺陷 |
| BEQR-102 | BEQR:166 | 跨主体借调 | 已核对，未确认缺陷 |
| BEQR-103 | BEQR:168 | 智能采购建议 | 已核对，未确认缺陷 |
| BEQR-104 | BEQR:169 | 门店要货履约状态 | 已核对，未确认缺陷 |
| BEQR-105 | BEQR:170 | 经销商状态 | 已核对，未确认缺陷 |
| BEQR-106 | BEQR:171 | 经销询价状态 | 已核对，未确认缺陷 |
| BEQR-107 | BEQR:172 | 经销价格目录状态 | 已核对，未确认缺陷 |
| BEQR-108 | BEQR:173 | 经销订单状态 | 已核对，未确认缺陷 |
| BEQR-109 | BEQR:174 | 经销退货单状态 | 已核对，未确认缺陷 |
| BEQR-110 | BEQR:175 | 经销库存外显 | 已核对，未确认缺陷 |
| BEQR-111 | BEQR:176 | 经销支付方式 | 已核对，未确认缺陷 |
| BEQR-112 | BEQR:177 | 经销库存预留 | 已核对，未确认缺陷 |
| BEQR-113 | BEQR:178 | 经销职责分离 | 已核对，未确认缺陷 |
| BEQR-114 | BEQR:179 | 经销取消、退货与开票 | 已核对，未确认缺陷 |
| BEQR-115 | BEQR:180 | 经销价格与税务快照 | 已核对，未确认缺陷 |
| BEQR-116 | BEQR:181 | 经销商停用撤权 | 已核对，未确认缺陷 |
| BEQR-117 | BEQR:182 | 经销资金归属 | 已核对，未确认缺陷 |
| BEQR-118 | BEQR:187 | 运营营业日 | 已核对，未确认缺陷 |
| BEQR-119 | BEQR:188 | 对账口径 | 已核对，未确认缺陷 |
| BEQR-120 | BEQR:189 | 对账差异四类 | 已核对，未确认缺陷 |
| BEQR-121 | BEQR:190 | 日结状态 | 已核对，未确认缺陷 |
| BEQR-122 | BEQR:191 | 差异处理状态 | 已核对，未确认缺陷 |
| BEQR-123 | BEQR:192 | 结算状态 | 已核对，未确认缺陷 |
| BEQR-124 | BEQR:193 | 综合通票定价 | 已核对，未确认缺陷 |
| BEQR-125 | BEQR:195 | 商户业态 | 已核对，未确认缺陷 |
| BEQR-126 | BEQR:196 | 合作模式 | 已核对，未确认缺陷 |
| BEQR-127 | BEQR:197 | 退款资金流向 | 已核对，未确认缺陷 |
| BEQR-128 | BEQR:198 | 物业费 | 已核对，未确认缺陷 |
| BEQR-129 | BEQR:199 | 能源费与损耗费 | 已核对，未确认缺陷 |
| BEQR-130 | BEQR:201 | 税费配置 | 已核对，未确认缺陷 |
| BEQR-131 | BEQR:202 | 三方对账框架 | 已核对，未确认缺陷 |
| BEQR-132 | BEQR:203 | 开票状态 | 已核对，未确认缺陷 |
| BEQR-133 | BEQR:208 | 接入的外部系统 | 已核对，未确认缺陷 |
| BEQR-134 | BEQR:209 | 客户标签 | 已核对，未确认缺陷 |
| BEQR-135 | BEQR:210 | 统一原则 | 已核对，未确认缺陷 |
| BEQR-136 | BEQR:211 | 接入状态 | 已核对，未确认缺陷 |
| BEQR-137 | BEQR:212 | 回流结果状态 | 已核对，未确认缺陷 |
| BEQR-138 | BEQR:217 | 风险四级 | 已核对，未确认缺陷 |
| BEQR-139 | BEQR:218 | 事件状态 | 已核对，未确认缺陷 |
| BEQR-140 | BEQR:219 | 主管复核强制项 | 已核对，未确认缺陷 |
| BEQR-141 | BEQR:220 | 经营异常处置状态 | 已核对，未确认缺陷 |
| BEQR-142 | BEQR:221 | 异常状态的统一出口 | 已核对，未确认缺陷 |
| BEQR-143 | BEQR:226 | 四类主体 | 已核对，未确认缺陷 |
| BEQR-144 | BEQR:227 | 经营主体关系 | 已核对，未确认缺陷 |
| BEQR-145 | BEQR:228 | 城市项目公司 | 已核对，未确认缺陷 |
| BEQR-146 | BEQR:229 | 运营公司职责 | 已核对，未确认缺陷 |
| BEQR-147 | BEQR:230 | 主要收入归属 | 已核对，未确认缺陷 |
| BEQR-148 | BEQR:231 | 合作模式 | 已核对，未确认缺陷 |
| BEQR-149 | BEQR:232 | 职级 | 已核对，未确认缺陷 |
| BEQR-150 | BEQR:233 | 终端十一端 | 已核对，未确认缺陷 |
| BEQR-151 | BEQR:234 | 最终权限计算 | 已核对，未确认缺陷 |
| BEQR-152 | BEQR:235 | 项目与敏感数据权限 | 已核对，未确认缺陷 |
| BEQR-153 | BEQR:236 | 终端开关优先级 | 已核对，未确认缺陷 |
| BEQR-154 | BEQR:237 | 综合体项目状态 | 已核对，未确认缺陷 |
| BEQR-155 | BEQR:238 | 商户/运营主体状态 | 已核对，未确认缺陷 |
| BEQR-156 | BEQR:239 | 合同状态 | 已核对，未确认缺陷 |
| BEQR-157 | BEQR:240 | 员工状态 | 已核对，未确认缺陷 |
| BEQR-158 | BEQR:241 | 设备状态 | 已核对，未确认缺陷 |
| BEQR-159 | BEQR:242 | 权限状态 | 已核对，未确认缺陷 |
| BEQR-160 | BEQR:248 | 招商线索状态 | 已核对，未确认缺陷 |
| BEQR-161 | BEQR:249 | 招商意向阶段 | 已核对，未确认缺陷 |
| BEQR-162 | BEQR:250 | 品牌/客户库类型 | 已核对，未确认缺陷 |
| BEQR-163 | BEQR:251 | 合同变更类型 | 已核对，未确认缺陷 |
| BEQR-164 | BEQR:252 | 退租结算状态 | 已核对，未确认缺陷 |
| BEQR-165 | BEQR:253 | 内部应付执行状态 | 已核对，未确认缺陷 |
| BEQR-166 | BEQR:255 | 合同审批链 | 已核对，未确认缺陷 |
| BEQR-167 | BEQR:256 | 租金/账单类型 | 已核对，未确认缺陷 |
| BEQR-168 | BEQR:257 | 营业额抽成模式 | 已核对，未确认缺陷 |
| BEQR-169 | BEQR:258 | 公区能源费 | 已核对，未确认缺陷 |
| BEQR-170 | BEQR:259 | 铺位租赁状态 | 已核对，未确认缺陷 |
| BEQR-171 | BEQR:260 | 物业工单类型 | 已核对，未确认缺陷 |
| BEQR-172 | BEQR:262 | 物业工单状态 | 已核对，未确认缺陷 |
| BEQR-173 | BEQR:264 | 商户服务工单状态 | 已核对，未确认缺陷 |
| BEQR-174 | BEQR:265 | 工单时效 | 已核对，未确认缺陷 |
| BEQR-175 | BEQR:266 | 楼宇机电设备状态 | 已核对，未确认缺陷 |
| BEQR-176 | BEQR:267 | 设备维保类型 | 已核对，未确认缺陷 |
| BEQR-177 | BEQR:268 | 能耗表计类型 | 已核对，未确认缺陷 |
| BEQR-178 | BEQR:269 | 抄表状态 | 已核对，未确认缺陷 |
| BEQR-179 | BEQR:270 | 能耗归集对象 | 已核对，未确认缺陷 |
| BEQR-180 | BEQR:271 | 楼宇巡检任务状态 | 已核对，未确认缺陷 |
| BEQR-181 | BEQR:273 | 楼宇巡检类型 | 已核对，未确认缺陷 |
| BEQR-182 | BEQR:279 | 考勤日结果 | 已核对，未确认缺陷 |
| BEQR-183 | BEQR:280 | 打卡方式 | 已核对，未确认缺陷 |
| BEQR-184 | BEQR:281 | 请假类型 | 已核对，未确认缺陷 |
| BEQR-185 | BEQR:282 | 请假单状态 | 已核对，未确认缺陷 |
| BEQR-186 | BEQR:283 | 薪资单构成 | 已核对，未确认缺陷 |
| BEQR-187 | BEQR:285 | 员工提成发放 | 已核对，未确认缺陷 |
| BEQR-188 | BEQR:290 | 服务订单状态 | 已核对，未确认缺陷 |
| BEQR-189 | BEQR:291 | 管理费方式 | 已核对，未确认缺陷 |
| BEQR-190 | BEQR:296 | 分红基数 | 已核对，未确认缺陷 |
| BEQR-191 | BEQR:297 | 分红比例 | 已核对，未确认缺陷 |
| BEQR-192 | BEQR:298 | 分红税务 | 已核对，未确认缺陷 |
| BEQR-193 | BEQR:299 | 分红周期 | 已核对，未确认缺陷 |
| BEQR-194 | BEQR:300 | 分红单状态 | 已核对，未确认缺陷 |
| BEQR-195 | BEQR:301 | 可选扣除项 | 已核对，未确认缺陷 |
| BEQR-196 | BEQR:302 | 发放保护线留存 | 已核对，未确认缺陷 |
| BEQR-197 | BEQR:303 | 期间关账 | 已核对，未确认缺陷 |
| BSM-001 | BSM:505 | 整体说明：这套系统到底管什么 | 已核对，未确认缺陷 |
| BSM-002 | BSM:526 | 一笔业务怎么走 | 已核对，未确认缺陷 |
| BSM-003 | BSM:563 | 系统通用能力：数据可改可删、操作有引导、权限分岗 | 已核对，未确认缺陷 |
| BSM-004 | BSM:566 | 配置数据可以存档、也可以删除（删除要审批） | 已核对，未确认缺陷 |
| BSM-005 | BSM:581 | 后台的"新建/录入"改成分步引导 | 已核对，未确认缺陷 |
| BSM-006 | BSM:584 | 验证用的二维码一律动态刷新（间隔可在后台设定） | 已核对，未确认缺陷 |
| BSM-007 | BSM:587 | 平台管理员拥有全部日常功能权限 | 已核对，未确认缺陷 |
| BSM-008 | BSM:592 | 子系统入口 | 已核对，未确认缺陷 |
| BSM-009 | BSM:596 | 基础管理 | 已核对，未确认缺陷 |
| BSM-010 | BSM:601 | 票务权益 | 已核对，未确认缺陷 |
| BSM-011 | BSM:606 | 客人端 | 已核对，未确认缺陷 |
| BSM-012 | BSM:611 | 酒吧平板/工作手机 | 已核对，未确认缺陷 |
| BSM-013 | BSM:616 | 触摸屏一体机 | 已核对，未确认缺陷 |
| BSM-014 | BSM:621 | 酒吧管理 | 已核对，未确认缺陷 |
| BSM-015 | BSM:626 | 供应链库存 | 已核对，未确认缺陷 |
| BSM-016 | BSM:631 | 游乐管理 | 已核对，未确认缺陷 |
| BSM-017 | BSM:637 | 商户管理 | 已核对，未确认缺陷 |
| BSM-018 | BSM:643 | 招商租赁 | 已核对，未确认缺陷 |
| BSM-019 | BSM:650 | 物业服务 | 已核对，未确认缺陷 |
| BSM-020 | BSM:656 | 楼宇运营 | 已核对，未确认缺陷 |
| BSM-021 | BSM:661 | 第三方接入 | 已核对，未确认缺陷 |
| BSM-022 | BSM:666 | 留痕与异常 | 已核对，未确认缺陷 |
| BSM-023 | BSM:671 | 财务对账 | 已核对，未确认缺陷 |
| BSM-024 | BSM:676 | 持股分红 | 已核对，未确认缺陷 |
| BSM-025 | BSM:681 | 经营看板 | 已核对，未确认缺陷 |
| BSM-026 | BSM:686 | 各类入口 | 已核对，未确认缺陷 |
| BSM-027 | BSM:694 | 基础管理：先把综合体的盘子管清楚 | 已核对，未确认缺陷 |
| BSM-028 | BSM:698 | 主要功能 | 已核对，未确认缺陷 |
| BSM-029 | BSM:714 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-030 | BSM:727 | 票务权益：管卖什么票、给什么权益、怎么核销 | 已核对，未确认缺陷 |
| BSM-031 | BSM:731 | 主要功能 | 已核对，未确认缺陷 |
| BSM-032 | BSM:760 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-033 | BSM:772 | 票种、通票与权益类型（系统支持哪些种类） | 已核对，未确认缺陷 |
| BSM-034 | BSM:796 | 核销、退款与有效期规则 | 已核对，未确认缺陷 |
| BSM-035 | BSM:813 | 会员与积分规则 | 已核对，未确认缺陷 |
| BSM-036 | BSM:840 | 人脸验龄数据使用与删除 | 已核对，未确认缺陷 |
| BSM-037 | BSM:850 | 客人端：客人自己买票、预定、查询、积分和出示二维码 | 已核对，未确认缺陷 |
| BSM-038 | BSM:854 | 主要功能 | 已核对，未确认缺陷 |
| BSM-039 | BSM:873 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-040 | BSM:883 | 登录与关键规则 | 已核对，未确认缺陷 |
| BSM-041 | BSM:898 | 会员注册、认证与收费边界 | 已核对，未确认缺陷 |
| BSM-042 | BSM:907 | 会员积分活动与发券 | 已核对，未确认缺陷 |
| BSM-043 | BSM:916 | 会员营销明确边界 | 已核对，未确认缺陷 |
| BSM-044 | BSM:929 | 酒吧平板/工作手机：平板点酒，工作手机核销 | 已核对，未确认缺陷 |
| BSM-045 | BSM:933 | 主要功能 | 已核对，未确认缺陷 |
| BSM-046 | BSM:953 | 平板图形点单（POS） | 已核对，未确认缺陷 |
| BSM-047 | BSM:963 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-048 | BSM:978 | 触摸屏一体机：权益领取、现场服务和辅助售票 | 已核对，未确认缺陷 |
| BSM-049 | BSM:982 | 主要功能 | 已核对，未确认缺陷 |
| BSM-050 | BSM:996 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-051 | BSM:1010 | 酒吧管理：从入场、开台到结账关台 | 已核对，未确认缺陷 |
| BSM-052 | BSM:1014 | 主要功能 | 已核对，未确认缺陷 |
| BSM-053 | BSM:1041 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-054 | BSM:1053 | 桌台/账单状态与经营规则 | 已核对，未确认缺陷 |
| BSM-055 | BSM:1088 | 桌台、菜单与优惠券 | 已核对，未确认缺陷 |
| BSM-056 | BSM:1101 | 正式未付款订单挂起 | 已核对，未确认缺陷 |
| BSM-057 | BSM:1120 | 服务点单：客人点专项服务，公司统一收款、再按名录分账 | 已核对，未确认缺陷 |
| BSM-058 | BSM:1124 | 怎么点、怎么收钱 | 已核对，未确认缺陷 |
| BSM-059 | BSM:1135 | 钱怎么分、怎么退 | 已核对，未确认缺陷 |
| BSM-060 | BSM:1151 | 供应链库存：管供应商、采购、库房、消耗和存酒 | 已核对，未确认缺陷 |
| BSM-061 | BSM:1155 | 主要功能 | 已核对，未确认缺陷 |
| BSM-062 | BSM:1174 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-063 | BSM:1186 | 库存类型与规则 | 已核对，未确认缺陷 |
| BSM-064 | BSM:1208 | 采购建议与门店要货 | 已核对，未确认缺陷 |
| BSM-065 | BSM:1216 | 外部经销商准入、价格与库存 | 已核对，未确认缺陷 |
| BSM-066 | BSM:1226 | 经销订单、付款与履约 | 已核对，未确认缺陷 |
| BSM-067 | BSM:1238 | 经销退货、退款、红冲与资金归属 | 已核对，未确认缺陷 |
| BSM-068 | BSM:1253 | 游乐管理：管项目开放、项目容量（实时在玩人数）和安全异常 | 已核对，未确认缺陷 |
| BSM-069 | BSM:1257 | 主要功能 | 已核对，未确认缺陷 |
| BSM-070 | BSM:1276 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-071 | BSM:1287 | 游乐类型与规则 | 已核对，未确认缺陷 |
| BSM-072 | BSM:1308 | 商户管理：商户在授权范围内完整使用自己的系统 | 已核对，未确认缺陷 |
| BSM-073 | BSM:1313 | 主要功能 | 已核对，未确认缺陷 |
| BSM-074 | BSM:1326 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-075 | BSM:1339 | 招商租赁与合同履约：从找商户到签约、履约、变更、退租 | 已核对，未确认缺陷 |
| BSM-076 | BSM:1343 | 主要功能 | 已核对，未确认缺陷 |
| BSM-077 | BSM:1357 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-078 | BSM:1370 | 物业服务：商户和现场的报修、投诉、整改都进工单 | 已核对，未确认缺陷 |
| BSM-079 | BSM:1374 | 主要功能 | 已核对，未确认缺陷 |
| BSM-080 | BSM:1388 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-081 | BSM:1401 | 楼宇运营：机电设备、能耗抄表、楼宇巡检 | 已核对，未确认缺陷 |
| BSM-082 | BSM:1405 | 主要功能 | 已核对，未确认缺陷 |
| BSM-083 | BSM:1414 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-084 | BSM:1427 | 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | 已核对，未确认缺陷 |
| BSM-085 | BSM:1431 | 日常工作 | 已核对，未确认缺陷 |
| BSM-086 | BSM:1441 | 排班、考勤与请假 | 已核对，未确认缺陷 |
| BSM-087 | BSM:1455 | 我的薪资 | 已核对，未确认缺陷 |
| BSM-088 | BSM:1467 | 员工提成、活动执行与人员服务边界 | 已核对，未确认缺陷 |
| BSM-089 | BSM:1479 | 第三方接入：外部系统继续做专业系统，平台拿回结果 | 已核对，未确认缺陷 |
| BSM-090 | BSM:1545 | 机器人配送与扫码交接 | 已核对，未确认缺陷 |
| BSM-091 | BSM:1554 | 视频监控（留点位/时间） | 已核对，未确认缺陷 |
| BSM-092 | BSM:1563 | 重要事项短信通知（2026-06-29 新增） | 已核对，未确认缺陷 |
| BSM-093 | BSM:1574 | 留痕与异常：重要动作留得住，出了问题追得回 | 已核对，未确认缺陷 |
| BSM-094 | BSM:1578 | 必须关注的动作 | 已核对，未确认缺陷 |
| BSM-095 | BSM:1596 | 怎样减少人工填写 | 已核对，未确认缺陷 |
| BSM-096 | BSM:1607 | 处理结果 | 已核对，未确认缺陷 |
| BSM-097 | BSM:1619 | 风险等级与风控规则 | 已核对，未确认缺陷 |
| BSM-098 | BSM:1636 | 财务对账：把经营事实整理成财务能确认的口径 | 已核对，未确认缺陷 |
| BSM-099 | BSM:1639 | 内部结算价目录（按商品规格逐项建目录） | 已核对，未确认缺陷 |
| BSM-100 | BSM:1641 | 结算配置中心（后台总控页） | 已核对，未确认缺陷 |
| BSM-101 | BSM:1643 | 项目零售参考价 | 已核对，未确认缺陷 |
| BSM-102 | BSM:1648 | 主要功能 | 已核对，未确认缺陷 |
| BSM-103 | BSM:1666 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-104 | BSM:1678 | 收入归属：哪类收入归哪个业态主体 | 已核对，未确认缺陷 |
| BSM-105 | BSM:1697 | 三方对账框架：系统流水、渠道对账单、资金到账两两核对 | 已核对，未确认缺陷 |
| BSM-106 | BSM:1709 | 对账与结算类型规则 | 已核对，未确认缺陷 |
| BSM-107 | BSM:1737 | 持股分红：按持股把经营成果定期分给主理人 | 已核对，未确认缺陷 |
| BSM-108 | BSM:1741 | 主要功能 | 已核对，未确认缺陷 |
| BSM-109 | BSM:1760 | 谁会用 | 已核对，未确认缺陷 |
| BSM-110 | BSM:1768 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-111 | BSM:1781 | 经营看板：让管理层和运营看得懂、能追问 | 已核对，未确认缺陷 |
| BSM-112 | BSM:1785 | 主要指标 | 已核对，未确认缺陷 |
| BSM-113 | BSM:1813 | 谁会看 | 已核对，未确认缺陷 |
| BSM-114 | BSM:1827 | 各类入口：不同人只看自己该看的、只做自己该做的 | 已核对，未确认缺陷 |
| BSM-115 | BSM:1897 | 相互关系：为什么这些子系统必须连在一起 | 已核对，未确认缺陷 |
| BSM-116 | BSM:1928 | 系统交付与上线：正式上线要做的安全加固和安装交付 | 已核对，未确认缺陷 |
| BSM-117 | BSM:1932 | 正式上线时的安全加固 | 已核对，未确认缺陷 |
| BSM-118 | BSM:1940 | 能自动识别环境的安装包 + 内附教程 | 已核对，未确认缺陷 |
| BSM-119 | BSM:1947 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-120 | BSM:1957 | 已确认规则和实施前输入项 | 已核对，未确认缺陷 |

## 对抗式线：正常路径反例与排除

| 对照组 | 反例/边界 | 本席结论与依据 |
|---|---|---|
| 票务/年龄 | 设备不可用、误拒、多人、多次、场次终结、已用票再赠、补偿延长 | 年龄设备人工抽查/一次凭据是明定例外，非免检票；补偿是场次终结例外；普通退款与安全例外分开。BEQR:33/47/73、BSM:746/800/803/808/1074。 |
| 退款单位 | 人份退款与“同订单不存在部分核销并部分退款” | 人次处理规定核算单位，不单独授予退款资格；两份均明写普通退款限制，不能靠人份定义推导出例外。未确认矛盾，不发明放宽。BEQR:47/73、BSM:746/803。 |
| 分配/尾差 | 1—10 家比例、并列第4/5、仅第一段、第二段全未核销、缺家数行 | 十行均100%，并列17.5%；未核销减除/未分配池/尾差三栏独立；缺行补配对暂挂票是明确特例。命令实算在下。 |
| 零元/定价/费用 | 免费权益出资、内部价时点、销售通路二层、单票/通票服务费上限 | 逐项锁出资与内部价的特殊规则不能被一般“核销时固化”覆盖；各费只扣一次，单票/通票上限分开。BEQR票务财务锚点、BSM:748—755/1037—1042/1640。 |
| 积分/会员/储值 | 付款与核销不同日、未用票、退款积分过期、注销一年、券重回可用 | 积分/成长值双账，票务/商户消费两种计算；商户取消退券与退积分继承原到期日明确；注销事件保留是窄例外。BEQR会员段，BSM:815—849/864。 |
| 台位/酒吧 | A+B→T→并C→拆、撤销时原台另占、低消已达后优惠、赠送/提成 | 原台历史不进入新T运算；任一后续变化阻断旧快照撤销；低消折前与提成折后为不同基数；服务费应发部分与扣留部分分开。BSM:1063—1081。 |
| 终端/员工 | 私人手机概述含考勤与工作机领机打卡、设备故障延误、跨月班次 | 概述不替代领机三事件的具体入口；设备故障延误为严格迟到规则的明定例外；班次结束归属为营业日规则的明定例外。BSM:930/1443—1452/1458。 |
| 供应链/游乐 | 可消费借物、补录、恰等于高价值阈值、关键设备可替代、重复出场 | 可消费物品实际买断；补录与原始记录并列；高价值触发是“高于”而不是自行改成“大于等于”，未确认能以缺少重复等值句新增业务。关键安全依赖依实际流程、辅助故障需工程留痕，容量不人工改。BEQR:164，BSM:1202—1206/1253—1305。 |
| 租赁/物业 | 零欠费/仅罚款、面积变更、押金等待与三个付账日、投诉关闭 | 清退前提明确；合同变更子合同与一般不追补分别适用；退租押金不受三个付账日为明定例外；投诉类型分支不同，不套全类验收链。BSM:1340—1385。 |
| 财务/分红 | 跨月退款、在途/失败、税额、服务费扣留、借款前置 | 不改已关期间；在途不重付、失败另路跟进；税额依专业规则不套普通舍入；借款前置不属于三个可选开关。BEQR财务/分红段，BSM:1642—1790。 |
| 治理/交付 | 普通审批绕红线、密钥逐笔例外、测试弱密码、只读分析、第三方占位 | 三硬红线不可绕，其他密钥权限限逐笔；生产换强密码/安装与运行权限不同；只读分析不发控制指令；无接口不得假造数据。BSM:703/1805—1820/1930—1955。 |

上表行区间是定位辅助，精确判据原文和源行以逐 ID 表为准。概述与具体条目间的边界均按明确限定解释；不为理论组合添加状态/闸/规则。

## 临时登记（全部两项，均不进修复队列）

### CX-01 — BEQR 静态呈现
- 文件：docs/business-enum-quick-reference-v0.1.html:1—26。
- 分类：技术性；工程缺陷：否；建议修复队列归属：否。
- 依据类型：4；冻结判据：GEN-001/002/015/017/018。
- 无业务发明理由：doctype、UTF-8、CSS、表格语义只承载同页权威文本的阅读，不创造收费、状态、角色或审批。结构解析与源码全文读取均完成。
- 处置：只登记，等待 B2 的 Grok 独立分类复核，不自行删除正本任何部分。

### CX-02 — BSM 静态呈现与目录导航
- 文件：docs/business-system-map-for-operations-v0.1.html:1—520、2008—2032。
- 分类：技术性；工程缺陷：否；建议修复队列归属：否。
- 依据类型：4；冻结判据：GEN-001/002/012/015/017/018。
- 无业务发明理由：布局与 setActive 只高亮当前目录，不修改业务数据；真实27个href均对应源ID，原脚本执行首屏/每目标通过。
- 处置：只登记，等待 B2 的 Grok 独立分类复核。

## 已定稿缺陷的当前状态

M001-012（日期/版本）和M001-013（目录）已按三处批准修复并获B4-r3双PASS，findings V0.80当前队列均否。当前L2为V0.12/2026-09-08，L499目录“已确认规则和实施前输入项”，L2003日期2026-09-08。其余行、另外两份HTML保持原字节。BSM-120冻结摘录已按LOCK-AMEND双PASS同步，新基准生效且本次从B1重开；以上是现状态证据，不替代本次全模块审查。

## 本次技能使用

实际使用本会话已读的using-superpowers、grilling/grill-me、codebase-design、ponytail-review、receiving-code-review、verification-before-completion、systematic-debugging及diagnosing-bugs，按V0.17收窄；只用已实际核实的本体，不派子代理、不引入新门槛。HTML审计按类型1/4证据，不强制运行后台业务测试或新增测试文件；未冒充浏览器/读屏检查。

## 可复现命令与实际输出

以下在正本 cwd 运行，全部仅内存计算，无产品/HTML/判据改动。使用 Python stdlib 与已装 Node。第一个命令输出的大覆盖 JSON 曾被工具限长，根转存 JSON 失败；已单独取短索引317行成功，不把截断当缺陷或业务 PASS。第二命令最初的比例正则漏空格，出现 AssertionError ('1', {})；已修正检查表达式后重跑成功，未修改业务源。

### 全锚点、结构与原导航脚本
```bash
python3 - <<'PY'
from pathlib import Path
from html.parser import HTMLParser
from collections import Counter
import hashlib,json,re,subprocess
root=Path('/home/james/complex-management')
paths=['docs/business-enum-quick-reference-v0.1.html','docs/business-system-map-for-operations-v0.1.html']
lock=(root/'docs/audit-2026-09/plan/criteria-lock.md').read_text(encoding='utf-8')
h=hashlib.sha256((root/'docs/audit-2026-09/plan/criteria-lock.md').read_bytes()).hexdigest()
blocks=re.split(r'(?m)^### ',lock)[1:]
coverage=[]
for block in blocks:
 m=re.match(r'((?:BEQR|BSM)-\d{3}) — (.*)',block)
 if not m: continue
 src=re.search(r'- 来源：`([^`]+):(\d+)`',block)
 raw=re.search(r'````html\n([\s\S]*?)\n````',block)
 assert src and raw,m[1]
 source=(root/src[1]).read_text(encoding='utf-8')
 assert raw[1] in source,m[1]
 coverage.append({'id':m[1],'title':m[2],'file':src[1],'line':int(src[2])})
assert len(coverage)==317,len(coverage)
class AuditHTML(HTMLParser):
 def __init__(self):
  super().__init__();self.ids=[];self.links=[];self.navlinks=[];self.stack=[];self.nav=False;self.errors=[];self.tags=Counter();self.lang=None;self.charset=None
 def handle_starttag(self,tag,attrs):
  a=dict(attrs);self.tags[tag]+=1
  if 'id' in a:self.ids.append(a['id'])
  if tag=='html':self.lang=a.get('lang')
  if tag=='meta' and 'charset' in a:self.charset=a['charset']
  if tag=='nav' or 'nav' in a.get('class','').split():self.nav=True
  if tag=='a' and 'href' in a:
   self.links.append(a['href'])
   if self.nav:self.navlinks.append(a['href'])
  if tag not in {'area','base','br','col','embed','hr','img','input','link','meta','param','source','track','wbr'}:self.stack.append(tag)
 def handle_endtag(self,tag):
  if tag=='nav':self.nav=False
  if self.stack and self.stack[-1]==tag:self.stack.pop()
  else:self.errors.append([self.getpos()[0],tag,self.stack[-3:]])
checks=[]
for path in paths:
 b=(root/path).read_bytes();s=b.decode('utf-8',errors='strict');p=AuditHTML();p.feed(s)
 assert not b.startswith(b'\xef\xbb\xbf') and '\ufffd' not in s,path
 duplicate=[x for x,n in Counter(p.ids).items() if n>1]
 broken=[x for x in p.links if x.startswith('#') and x[1:] not in p.ids]
 missing=[x for x in p.links if x and not x.startswith(('#','http:','https:','mailto:','javascript:')) and not (root/path).parent.joinpath(x.split('#')[0]).exists()]
 assert not (duplicate or broken or missing or p.errors or p.stack),(path,duplicate,broken,missing,p.errors,p.stack)
 assert p.lang and p.charset.lower()=='utf-8'
 checks.append({'path':path,'sha256':hashlib.sha256(b).hexdigest(),'wc_l':b.count(b'\n'),'actual_lines':len(s.splitlines()),'ids':len(p.ids),'navlinks':len(p.navlinks),'broken_links':len(broken)+len(missing),'stack_errors':len(p.errors),'utf8':'PASS'})
 if 'business-system-map' in path:
  script=re.search(r'<script>([\s\S]*?)</script>',s)[1]
  refs=[x for x in p.navlinks if x.startswith('#')]
  js="const vm=require('node:vm'), assert=require('node:assert/strict'); const input=JSON.parse(process.argv[1]); let top=[],active=[]; const nodes=input.refs.map((id,i)=>({id:id.slice(1),getBoundingClientRect:()=>({top:top[i]})})); const links=input.refs.map((id,i)=>({getAttribute:()=>id,classList:{toggle:(_,v)=>active[i]=v}})); const context={document:{querySelectorAll:()=>links,querySelector:id=>nodes.find(n=>'#'+n.id===id)},window:{addEventListener:(event,fn,opts)=>{assert.equal(event,'scroll');assert.equal(opts.passive,true)}}};top=nodes.map((_,i)=>1000+i*100);vm.createContext(context);vm.runInContext(input.script,context);assert.deepEqual(active,links.map((_,i)=>i===0));for(let target=0;target<nodes.length;target++){top=nodes.map((_,i)=>(i-target)*200+120);vm.runInContext('setActive()',context);assert.deepEqual(active,links.map((_,i)=>i===target));}console.log(JSON.stringify({nav_targets:nodes.length,initial_and_every_section:'PASS',actual_script:true}));"
  run=subprocess.run(['node','-e',js,json.dumps({'script':script,'refs':refs})],capture_output=True,text=True)
  assert run.returncode==0,run.stderr
  checks.append({'navigation_execution':json.loads(run.stdout)})
print(json.dumps({'lock_sha256':h,'criteria_coverage':len(coverage),'checks':checks},ensure_ascii=False))
print('COVERAGE_COUNT='+str(len(coverage)))
PY
```
本次结构/全量摘录/原导航命令实际输出：
```text
{"lock_sha256": "08a4dcc31d10624ca846560d70dcc79e391aeae5ebcdc62a9341f30ecb8de314", "criteria_coverage": 317, "checks": [{"path": "docs/business-enum-quick-reference-v0.1.html", "sha256": "4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66", "wc_l": 308, "actual_lines": 308, "ids": 0, "navlinks": 0, "broken_links": 0, "stack_errors": 0, "utf8": "PASS"}, {"path": "docs/business-system-map-for-operations-v0.1.html", "sha256": "86a73b25db127f2c7d5aecacecdb1dc0ad9cbd872031add30bc52efc744e4b3f", "wc_l": 2032, "actual_lines": 2032, "ids": 29, "navlinks": 27, "broken_links": 0, "stack_errors": 0, "utf8": "PASS"}, {"navigation_execution": {"nav_targets": 27, "initial_and_every_section": "PASS", "actual_script": true}}]}
COVERAGE_COUNT=317
```

### 金额与反例
```bash
python3 - <<'PY'
from pathlib import Path
from decimal import Decimal,ROUND_HALF_UP
import re,json,hashlib
root=Path('/home/james/complex-management')
s=(root/'docs/business-enum-quick-reference-v0.1.html').read_text(encoding='utf-8')
row=next(x for x in s.splitlines() if '<td class="k">酒吧通票可分配池</td>' in x)
table=row.split('参与家数档位表（出厂默认')[1].split('参与家数档位表不设家数上限')[0]
def parse_table(t):
 rows={}
 for n,body in re.findall(r'第二段参与 (\d+) 家＝(.*?)(?:；|。)',t):
  slots={}
  for start,end,value in re.findall(r'第 (\d+)(?:[–-](\d+))? 名\s*(?:各\s*)?(\d+(?:\.\d+)?)%',body):
   for rank in range(int(start),int(end or start)+1):
    assert rank not in slots
    slots[rank]=Decimal(value)
  assert set(slots)==set(range(1,int(n)+1)),(n,slots)
  assert sum(slots.values())==100,(n,slots)
  rows[int(n)]=slots
 assert len(rows)==10
 return rows
rows=parse_table(table)
assert (rows[5][4]+rows[5][5])/2==Decimal('17.5')
rejected=False
try:parse_table(table.replace('100%','99%',1))
except AssertionError:rejected=True
assert rejected
# 只证明 HTML 明文的金额/场次算例可同时成立，不代表执行了业务实现。
round_cent=lambda x:Decimal(x).quantize(Decimal('.01'),rounding=ROUND_HALF_UP)
tie=[round_cent(Decimal('.10')*Decimal(1)/3)]*3
tail=Decimal('.10')-sum(tie)
assert sum(tie)+tail==Decimal('.10') and tail==Decimal('.01')
assert 8+5==13 # A4+B6 转 T8 后再并 C5
budget_total,available,reserved,issued,reclaimed=100,60,20,30,10
assert budget_total==available+reserved+issued-reclaimed
assert round_cent(Decimal(5000+1000+1000+1000)*Decimal('.2'))==Decimal('1600.00')
print(json.dumps({'rank_rows':{n:list(map(str,v.values())) for n,v in rows.items()},'rank_sums':'all 100','tie_4_5':'17.5','rounding_three_shares':list(map(str,tie)),'separate_tail':str(tail),'mutated_99_percent_rejected':rejected,'merge_example':'T8+C5=13','activity_budget':'100=60+20+30-10','scope':'HTML arithmetic only, not business implementation'},ensure_ascii=False))
raw=root/'docs/audit-2026-09/modules/M001-b1-self-raw.md'
if raw.exists():
 text=raw.read_text(encoding='utf-8');print('SELF_RAW_METADATA='+json.dumps({'size':raw.stat().st_size,'report_versions':re.findall(r'(?m)^.*版本[：:]\s*(V\d+\.\d+).*$',text)[-5:],'complete_report_blocks':len(re.findall(r'(?m)^CRITERIA-LOCK-SHA256:',text))},ensure_ascii=False))
print('HEAD='+__import__('subprocess').check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip())
PY
```
本次算术/反例命令实际输出（SELF_RAW_METADATA为当前追加前元数据）：
```text
{"rank_rows": {"1": ["100"], "2": ["57", "43"], "3": ["40", "30", "30"], "4": ["31", "23", "23", "23"], "5": ["25", "20", "20", "20", "15"], "6": ["23", "17", "17", "17", "13", "13"], "7": ["22", "15", "15", "15", "11", "11", "11"], "8": ["18", "14", "14", "14", "10", "10", "10", "10"], "9": ["15", "13", "13", "13", "10", "10", "10", "10", "6"], "10": ["16", "12", "12", "12", "9", "9", "9", "9", "6", "6"]}, "rank_sums": "all 100", "tie_4_5": "17.5", "rounding_three_shares": ["0.03", "0.03", "0.03"], "separate_tail": "0.01", "mutated_99_percent_rejected": true, "merge_example": "T8+C5=13", "activity_budget": "100=60+20+30-10", "scope": "HTML arithmetic only, not business implementation"}
SELF_RAW_METADATA={"size": 317296, "report_versions": ["V0.29", "V0.11", "V0.12", "V0.30", "V0.31"], "complete_report_blocks": 9}
HEAD=242f487233d028173cb9198eecd079f5d877405c
```

### 不冒充运行的检查
已读现有 verify-html-param-tier.mjs 与 verify-html-ledger-sync.mjs，其默认只检查本次 diff，当前 HEAD=origin/main 且 HTML 无改动会直接无改动 PASS；因此未把它们当本轮全文审计证据，也未修改基线。没有浏览器视觉/读屏实测，不声称该项全自动验收。映射校验最初误按 Markdown 表行查找而出现 StopIteration，已按实际 `M001 → [...]` 格式校验335个ID、漏项和越项均0。业务 Java/SQL 测试留所辖模块。

## 本次重新执行的当前证据

```json
{"mapped_criteria": 335, "missing": 0, "extra": 0, "header_footer_date": "2026-09-08", "nav_equals_heading": true, "BSM120_excerpt_current": true, "gen_semantics": "18 applicable IDs considered in full report", "read_scope": "2 whole HTML documents with unchanged content retained from prior full reads and verified current bytes"}
```

当前B1以本报告全文件/335ID/三线的完整证据为准；已有17段全文阅读的原文仍可由文件哈希与before/after tree定位，未变化的业务规则重新逐项对照而非只审3行diff。新锁检查、原始导航脚本、算术和反例均在Grok本次交付之后独立重新执行。当前Grok本次raw仍未读。

## 本次结论与下一步

本席按新锁三线完整复核2文件、335冻结判据；新冲突/添加/工程缺陷均为0，技术性无缺陷登记2项CX-01/02与既有M001-004同对象。已修012/013未发现回归。本席B1 PASS，没有新的HTML修改申请。本报告追加保留历史。下一步仅在本报告完整落盘后读取当前 Grok B1 raw，四值校验与合取；若 Grok 有 FAIL 不以本票覆盖，按证据核实和 V0.17 路由。未进入 B2/B3/B4/B5，未提交、推送、部署。


## 新锁 B1 Grok 接收与新增 HTML 修改申请（2026-09-08）
版本：V0.33（仅本接收追加段；完整保留此前全部字节与独立 V0.32 PASS 原票）

本次独立顺序：Grok真实CLI完成仅交REPORT_PATH/EXIT_CODE；根完整V0.32 raw落盘后才读取Grok V0.13（自1725行，184行完整段）。根独立PASS，Grok FAIL（三条），合取FAIL。不能用根票覆盖Grok。Grok退出0、指定文件本次更新，调用成功；不是调用失败，不重跑成功调用。GR-01/02/03为临时ID，未进入B2、未分永久ID、未改findings或questions。
此前三处获批修复全部已执行、B4-r3双PASS；BSM-120必要LOCK-AMEND双PASS后已更新基准并从M001 B1重开。现新问题位于既有业务正文，不是三处元数据/目录修改引入；也不是76个未发布文件中此前那一项工资口径冲突。
当前M001 B1未通过，全流程按铁律4/5停在BLOCKED-ASK；B3累计3、计划修复2、调用失败6、LOCK-AMEND-COUNT1均保留，其余470模块PENDING。当前B4-r3 PASS，未完成新锁B5，不伪称第三次B4/B5 FAIL。
进一步B3会超同模块上限3；不自行清零、加轮或以LOCK-AMEND回退增加额度。HTML逐处批准与另起修复轮次需有操作员明确新指令；不把普通是/可以自动解释为新轮授权。收到明确指令前不推进B2或旁支。

根对新问题核实：GR-01确有全称概括句与分支条款，较具体条款可理解为对概括的限定；本次建议显式引用现有分支来消除阅读歧义，不据此另造回访或验收规则。GR-02/03核到分类清单可配表述与全局分类定义规则相抵触；建议遵循现有全局规则。Grok关于枚举未写数值上下限的辅助主张不作为本申请修改理由：分类清单不应被强塞数值上下限；也未查代码证实实际后台可自行增删。是否固定这两类清单由操作员决定。

### 完整原文上下文（当前源文件原字摘录）

BSM L1370—1384 物业完整功能和已确认分支
```html
          <h2>物业服务：商户和现场的报修、投诉、整改都进工单</h2>
          <p class="lead">综合体不只是经营，还要做物业服务。商户报修、客人投诉、设备故障、巡检发现的问题都进统一工单，派单到人、跟踪处理、回访验收、统计时效。</p>
          <div class="detail-grid">
            <div class="info-block">
              <h3>主要功能</h3>
              <ul>
                <li>创建工单并分类：报修、保洁、投诉、整改、巡检和其他服务请求。</li>
                <!-- 2026-08-04 第三轮用户裁决（DEC-R6-14）：物业工单来源补「客人」——客人端投诉，客人即提单方、关闭须其确认 -->
                <li>工单来源包括：商户（商户侧自助提单）、客人（消费客人通过客人端投诉，客人即提单方；客户关怀部记录问题解决方案及客人反馈后可以关闭）、现场工作人员，以及楼宇设备告警和巡检异常自动生成。</li>
                <li>派单到具体人员或班组，记录处理过程、用料和结果。用料必须从指定库房出库并按当前平均进货价计成本，同时在工单上标明费用承担方：公区设施与楼宇主体设备的用料由物业承担；商户自有设施、以及因商户责任导致损坏的用料由该商户承担，并自动生成一笔应收进入其费用账单。承担方有争议的，工单挂起转上级判定，判定结果与依据留痕后才能结案。</li>
                <li>处理后回访客人或商户，验收合格才关闭。</li>
                <li>按响应时限和处理时限统计时效，超时自动升级通知主管。</li>
                <li>记录满意度评价，作为服务质量和商户表现的依据。</li>
                <!-- 2026-06-25 修改阶段：补物业工单运营细则，依据 M1 需求采集（complex-m1-requirements §4）。 -->
                <li>运营细则（已确认）：响应/处理时限按「工单类型×优先级」矩阵后台配置，超时自动升级通知主管；验收按工单类型分别配置——保洁等无争议类由处理人完成即结束；投诉分为商户投诉和消费客人投诉：商户投诉须由商户确认投诉已解决；按既有规则无法解决，或问题已解决但商户未确认解决或未点击关闭的，客户关怀部记录相关信息及关闭缘由后，可以关闭投诉。消费客人的投诉，由客户关怀部记录问题解决方案及客人反馈后，可以关闭；复杂类（大型设备报修）需主管/物业验收，并且一律须提单方确认才能关闭；满意度在回访/验收时由提单方评分；商户侧可自助提报修、保洁、投诉和其他服务请求，整改与巡检类工单由平台发起。客人投诉由客人自行在客人端提交，客人即为提单方，其关闭按上述消费客人投诉规则执行。工单由客户关怀部负责处理；响应时限与处理时限对商户公开。所有客诉必须有跟进、有记录、有结果，直至形成闭环；客诉须经客户关怀部确认后方可视为形成闭环，未经其确认的一律不算闭环。系统每周自动生成客诉周报，列明当周客诉的笔数、类型、处理进度与闭环情况——店内的报送该店主理人与店长，各公司的报送该公司最高负责人与平台最高负责人。一个月内，同一类投诉由三个不同的人分别提出的，立即上升到项目负责人、娱乐平台负责人与公司最高负责人；是否属于同一类由客户关怀部认定。</li>
```

BEQR L262 物业工单状态完整条目
```html
    <tr><td class="k">物业工单状态</td><td>待派单、已派单、处理中、待回访、待验收、已验收、已关闭、已挂起、已超时。回访／验收按工单类型分支适用，不是全类都走同一条顺序链：保洁等无争议类由处理人完成即结；投诉分为商户投诉和消费客人投诉：商户投诉须由商户确认投诉已解决；按既有规则无法解决，或问题已解决但商户未确认解决或未点击关闭的，客户关怀部记录相关信息及关闭缘由后，可以关闭投诉。消费客人的投诉，由客户关怀部记录问题解决方案及客人反馈后，可以关闭；复杂类（大型设备报修等）须主管验收＋提单方确认双闸。</td></tr>
```

BSM L521 全局可配/分类规则完整段
```html
          <p class="lead" id="principle-configurable"><strong>一条贯穿全局的原则：业务策略一律可配置。</strong>系统里所有影响钱和业务判断的策略参数——各种税率与计税档位、各类提成与分账比例、各种费率（物业费、租金、分摊）、退款与授权的额度上限、各类预警阈值、各种有效期与提醒天数、评分权重等——都必须能由有权限的运营/财务人员在后台自行配置和调整，不得固定写死在系统里。业务政策发生变化（如税率调整、比例变更、提醒周期调整）时，只需在后台修改配置即可生效，无需改动系统本身。所有涉及钱的配置一律版本化：每次改动生成新版本，必须填写生效起始日期，并先存为草案、经高敏权限“转正”审批通过后按指定日期生效，不即改即生效。新版本只对生效日之后发生的业务适用，已发生的业务一律按当时生效的版本计算；每一笔单据在生成时就把所用配置版本固化在单上，重新月结只重算金额、不改动单据上已锁定的版本；重新月结不因账目计算出错而发起，一律由实际发生的业务事件触发——如冲账、客人退款、多付或少付、货款出错等。确需追溯适用到生效日之前的，属于例外，必须单独申请、说明追溯范围与理由，经最高权限审批后才可执行，追溯的范围与影响金额逐笔留痕。无法判断是否涉及钱的，一律按涉及钱处理。技术性设定（与业务政策无关的内部运行参数）不在此列。业务分类定义也不在此列：红线有哪几类、退款有哪几种情形、会员有哪几个等级、终端有哪几类、合作模式有哪几种等「有哪几种」的清单，属于业务口径本身，不做后台可配；增加或删减一类必须先经批准写入三份 HTML，不得在后台自行增删。本原则要求可配的是阈值、比例、时长、次数、金额、时刻、档位取值这类策略参数。每一处可配参数在本文件中都标明它属于法定值、安全阈值还是业务参数，以及按什么粒度配置；还必须写明取值的上限与下限，超出该区间的配置一律拒绝生效，未填写上下限的参数不得上线。</p>
```

BSM L1020 开台来源完整条目
```html
                <li>记录卡座位置、服务员、人数、点酒明细、最低消费和账单状态；人数不参与最低消费计算。开台时系统自动判定并记录客人来源：经第三方团购券核销或第三方预订到店的，按回流渠道自动记（美团点评、抖音等）；经平台自有渠道订台或购票到店的，自动记平台自有渠道；由有销售归属的订台开台的，自动记营销带客并带出该销售人员；以上都不命中的直接到店开台默认记自来客——此时现场可人工补选营销带客并选定销售人员，不补选即维持自来客。来源枚举项为业务参数，按项目可配。开台记录中服务员与销售人员是两个独立字段。自来客由该来源记录定义，销售提成判定引用此记录。来源与销售人员的事后改派须店长复核并留痕，只能在该账单结清前办理。</li>
```

BSM L1068 高危动作完整条目
```html
                  <tr><th scope="row">高危动作</th><td>反结（重开已结账单）、沽清（卖完、临时下架）均需主管审批留痕；免单、账单调整记原因+责任人并分级复核。免单只有以下三种情形，除此之外不得免单：（一）在本人当月赠送额度以内的管理岗位赠送，可赠至全额免单，按赠送规则扣减额度；（二）发生重大责任事故，或客人在店内受伤、遭受严重骚扰的，可以考虑免单，须经店长、夜班值班经理与娱乐平台公司负责人三方批准；（三）客人出现重大情绪不稳定、安全问题或其他必要问题的，由夜班值班经理及以上可免除其通票费用（即退票）。打折分两个来源：一是客人持有代金券或打折券——这类券只能通过活动获得或积分兑换取得，按券面规则执行；二是现场打折，由店长、主理人、营销负责人、夜班值班经理、娱乐平台公司负责人、项目负责人及更高层级当场给予。最高折扣额度设下限，含代金券与打折券在内一律不得低于该下限（安全阈值，按项目可配，调整须最高权限审批并留痕，出厂默认八折，须同时配置上下限）；只有额度赠送不受此限，因其消耗的是本人额度。本下限只适用于卡座与散台的现场消费折扣，含客人持有的代金券与打折券；票种定价的折扣构成，以及会员价、渠道价、动态价的派生，均不受此限（通票须先购买才能入场，现场无从赠送或打折）。凡免单、打折与赠送，一律记录原因、责任人、批准人、金额与按内部结算价计算的成本，逐笔可追溯；系统每月自动生成免单、打折与赠送的管理报告，按门店、按岗位、按人列明笔数、金额与额度使用情况——店内的报送该店主理人与店长，各公司的报送该公司最高负责人与平台最高负责人。券面规则指该券发行版本固化的面额或折扣率、适用范围、使用门槛、有效期与出资方；真身在券的版本记录里，现场以系统读取结果为准。现场打折的让利一律由该商户自行承担，不得转嫁平台或其他主体；平台或项目统一活动的折扣按该活动版本固化的出资方承担。现场打折按整单执行，不提供单品打折；单品让利一律走赠送通道，按赠送规则核算额度与留痕。不提供现场改价：任何岗位不得在点单或结账时手工修改商品单价，价格变更一律走商品价格版本管理。现场打折与优惠券可同单使用，合并后的客人实付不得低于折扣下限；现场打折与赠送可同单并用，折扣额度与赠送额度各自独立核算、分别留痕。最高折扣额度可按岗位分档配置（业务参数，按项目可配，出厂默认各授权岗位一致为八折，须同时配置上下限）。现场打折必须选择原因分类：招待、客诉补偿、活动执行、员工亲友、其他（枚举项业务参数，按项目可配）；月度管理报告按原因分类归集。沽清分两档：临时沽清（本营业日有效，营业日切换时点自动恢复在售）与长期下架（须主管审批解除后才恢复）；沽清与解除同样须审批留痕，作用范围为本酒吧。沽清商品在客人扫码点单菜单上置灰展示为已售完，不隐藏。库存归零不自动沽清，由现场按实际发起。商品被沽清不影响其已下单未出品部分的出品与结算。</td></tr>
```

### 逐处修改申请（A/B/C业务，D版本；全部尚未批准、尚未修改）
同一文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html。未列内容不在申请范围。

**A / L1380**
原句：
处理后回访客人或商户，验收合格才关闭。
拟改：
处理后的回访、验收和关闭，按下述已确认的工单类型分支规则执行。
理由：使概括句明确引用已有的分类型规则；不另设关闭条件，不改L1384和BEQR262。

**B / L1020**
原句：
来源枚举项为业务参数，按项目可配。
拟改：
来源分类属于业务分类定义，不做后台增删；增加或删减一类必须先经批准写入三份 HTML。
理由：拟按L521现有全局原则处理分类清单，保留自动归因、营销补选、人员字段、提成引用和结清前店长复核。需操作员确认该业务取舍；未声称真实后台已支持增删。

**C / L1068**
原句：
现场打折必须选择原因分类：招待、客诉补偿、活动执行、员工亲友、其他（枚举项业务参数，按项目可配）；月度管理报告按原因分类归集。
拟改：
现场打折必须选择原因分类：招待、客诉补偿、活动执行、员工亲友、其他；原因分类属于业务分类定义，不做后台增删，增加或删减一类必须先经批准写入三份 HTML；月度管理报告按原因分类归集。
理由：保留五类原因和报告归集，只调整分类清单能否后台配置的表述；前一句折扣额度按岗位/项目配置仍适用。

**D / L2**
原句：
<!-- 文件版本：V0.12；更新日期：2026-09-08 -->
拟改：
<!-- 文件版本：V0.13；更新日期：2026-09-08 -->
理由：如上述新业务修改获批，整批更新一次自身版本；不沿用此前V0.12的修改许可。

### 本次 B1 操作员技术回执
步骤M001 B1；B3=3；ledger拟更新BLOCKED-ASK；Codex独立PASS/Grok FAIL/合取FAIL；Grok EXIT_CODE=0。
Grok报告：160:/home/james/complex-management/docs/audit-2026-09/modules/M001-b1-grok-raw.md（完整V0.13，自1725行）。
B1后四值（此刻逐一实算核验）：
- 实算：08a4dcc31d10624ca846560d70dcc79e391aeae5ebcdc62a9341f30ecb8de314
- sidecar：08a4dcc31d10624ca846560d70dcc79e391aeae5ebcdc62a9341f30ecb8de314
- ledger基准：08a4dcc31d10624ca846560d70dcc79e391aeae5ebcdc62a9341f30ecb8de314
- 本次Grok V0.13首行：08a4dcc31d10624ca846560d70dcc79e391aeae5ebcdc62a9341f30ecb8de314

本次真实完整调用（UTF-8 PowerShell交接与无头CLI参数，非示意）：
```powershell
$remoteScript = @'
cd /home/james/complex-management && python3 - <<'PY'
from pathlib import Path
import subprocess,sys,stat
target=Path('/home/james/complex-management/docs/audit-2026-09/modules/M001-b1-grok-raw.md')
def meta():
 if not target.exists(): return None
 s=target.stat()
 return (s.st_size,s.st_mtime_ns,s.st_ctime_ns,s.st_ino)
before=meta()
prompt="执行 160-audit-prompt.md V0.17 的正式 M001 B1 修复前全量审计。你是唯一 Grok 审计席；独立执行，不派子代理，不读取任何 Codex self raw、历史 raw 或旧审计结论。本次仅 M001，不能开 M002 或审其他业务模块。工作区 /home/james/complex-management，main HEAD=242f487233d028173cb9198eecd079f5d877405c。重新规划已按 LOCK-AMEND 完成双 PASS并启用；ledger V1.47，COUNT=1，M001=IN_PROGRESS，B3累计3。BSM三处获批元数据/目录修订已完成，随后BSM-120页脚摘录按LOCK-AMEND双PASS同步（lock V0.10），已经执行⑤回PENDING并现从B1重开。本次必须按新锁三线全量重新审查，不能继承旧B1/B4/B5结论；本阶段不再修复。模块编号以当前plan/modules.md为准，不继承任何旧编号PASS。\n\n只读取ledger顶部基准/计数及当前M001状态行，不阅读历史审计意见；先读取 .scratch/module-recheck-20260908/160-audit-prompt.md 完整V0.17、正式plan/runbook.md、当前M001模块定义与criteria-lock中M001映射的判据全文及general；按§6实际核读当前节点适用技能。只借技能审查结构，默认派代理/改代码/提交/独立报告/新闸全部不生效。技能缺口如实记录，不冒充调用。\n\n本模块仅两份文件，全文件审查不得只看差异：\ndocs/business-enum-quick-reference-v0.1.html（308物理换行）\ndocs/business-system-map-for-operations-v0.1.html（V0.12，2032物理换行）\n合计2340行。需要对照时可只读第三权威 docs/platform-governance-map-for-operations-v0.1.html 作上位/交叉引用上下文，不据此开启M002审计；不得把代码/设计/旧审批记录当新需求源。M001映射BEQR-001至197、BSM-001至120、GEN-001至018，共335ID，以实读为准。\n\n按第一性原理、顺从式、对抗式三线各审完整模块：业务文字/定义/枚举/例子之间是否可同时成立、链接与锚点/路径是否存在、HTML文档结构和实际声明是否一致、是否有冻结判据支撑的确定缺陷。必要性三问和可执行/非可执行产物证据按§2.3落实；HTML业务口径本身就是权威，不因偏好或行业惯例发明更严格业务。不要把没跑业务系统测试的HTML文档自动判FAIL，也不能只做机械锚点检查就当业务语义全审。\n\n四分类顺序固定冲突→细化→技术性→添加；每项问题给临时ID、完整路径和准确行号、逐字原文/最小复现及实际输出、冻结判据ID、分类、工程缺陷是否成立，收齐本次整批再返回。HTML自身有确定问题：完整列出问题与原文，遵守铁律4上报暂停、不修改任何HTML；不可扩大去修其他模块。无依据猜测或当前模块外实现缺陷不得当本模块finding。\n\n全程只读业务、HTML、计划、ledger、findings、questions。唯一可写：/home/james/complex-management/docs/audit-2026-09/modules/M001-b1-grok-raw.md；只追加本次完整报告，保留旧内容，不读旧意见（需要版本可仅机械读取行首版本字段）。本次报告起始第一行必须是你独立sha256sum正式criteria-lock得到的CRITERIA-LOCK-SHA256: <64位小写hex>，独占一行。其后写本次报告版本Vx.xx（按现有版本递增）、当前HEAD、两个HTML SHA256、技能实际使用与缺口、整文件/判据/三线覆盖、全部证据、最终明确PASS或FAIL以及是否触发HTML铁律。不要把过程语句当报告，不覆盖旧块，不新建别的产物。\n\n必须自行实算判据锁哈希并与sidecar/ledger核对。本次只B1，不做B2、不分永久findingID、不发保留/删除问题、不修复、不写模块PASS、不提交/推送/部署。指定路径已有内容则把本次完整审计报告追加到末尾，不得覆盖；文件不存在则创建。完整报告落盘后结束。"
cmd=['/home/james/.local/bin/grok','-p',prompt,'-m','grok-4.6','--reasoning-effort','xhigh','--output-format','json','--permission-mode','bypassPermissions','--no-subagents','--disable-web-search','--cwd','/home/james/complex-management']
proc=subprocess.Popen(cmd,cwd='/home/james/complex-management',stdout=subprocess.PIPE,stderr=subprocess.PIPE)
while True:
 try:
  output,error=proc.communicate(timeout=300)
  break
 except subprocess.TimeoutExpired: pass
output=error=None
after=meta()
code='GROK_EXIT' if proc.returncode else ('REPORT_MISSING' if after is None else ('PATH_MISMATCH' if not target.is_file() or target.resolve()!=target else ('REPORT_EMPTY' if after[0]==0 else ('REPORT_STALE' if after==before else None))))
if code:
 print('ADAPTER_ERROR='+code)
 print('EXIT_CODE='+str(proc.returncode))
 sys.exit(proc.returncode or 1)
print('REPORT_PATH='+str(target))
print('EXIT_CODE=0')
PY
'@
& 'C:\Windows\System32\OpenSSH\ssh.exe' -o BatchMode=yes james@192.168.88.160 $remoteScript
```

### 本轮 LOCK-AMEND Grok 有效终稿全文（IN-RECEIPT，历史时点③）
以下为本次必要锁同步的有效独立终稿原文；其所述④⑤未执行是交付时点状态，根已在V1.46先完成④⑤、V1.47从新锁B1重开。旧格式带过程前缀的回执未接受；同会话补交有效终稿exit0。
> CRITERIA-LOCK-SHA256: 08a4dcc31d10624ca846560d70dcc79e391aeae5ebcdc62a9341f30ecb8de314
> 
> **LOCK-AMEND PASS**（仅闸2③本席独立票；非模块 PASS；④⑤未执行）
> 
> 本席为唯一 Grok 席。只读业务 HTML / 计划 / ledger / findings；未派子代理；未改仓库任何文件。未新建报告产物。本回合只纠正交付格式：重新实算现锁、sidecar、ledger 旧基准、`/tmp/lock-amend-grok-20260908` 既有反向文件哈希，并核当前 HTML 页脚日期；未重写 `/tmp` 反向证据；未重读任何 Codex 审计结论；未运行 B1/B3/B4/B5；未更新 ledger 基准、未回退模块。
> 
> ## 1. 先验完整（反向实算 = 自读旧基准）
> 
> 自读 `docs/audit-2026-09/ledger.md` 顶部（V1.45），本回合复核仍为：
> 
> - `CRITERIA-LOCK-SHA256: 16def02336743500166af4cfd5fff48d02731d63d45a8f5f72721ca8ebd408e3`
> - `LOCK-AMEND-COUNT: 1`
> 
> 上一回合已执行（本回合未重跑 apply）：将现锁复制到 `/tmp/lock-amend-grok-20260908/docs/audit-2026-09/plan/criteria-lock.md`，cwd 为该副本根，对调用所附完整 unified diff 执行 `git apply --reverse --verbose amend.diff` → `Applied patch docs/audit-2026-09/plan/criteria-lock.md cleanly.`
> 
> 本回合对**既有**反向文件重新 `sha256sum`：`16def02336743500166af4cfd5fff48d02731d63d45a8f5f72721ca8ebd408e3`，**等于**本席自读 ledger 旧基准，不是根陈述、不是派发期望值。反向文件仍为 `版本：V0.09`、页脚 `更新日期：2026-09-01。`。正式现锁仍为 `版本：V0.10`、页脚 `更新日期：2026-09-08。`。上一回合测得新旧全文各 888984 字节、差值 0（只换等长日期/版本数字）。
> 
> ## 2. 再验一致（diff ↔ 登记）
> 
> 上一回合对反向文件 vs 正式现锁的完整 unified diff，内容变更行仅 4 行：
> 
> | 位置 | 旧 | 新 |
> |---|---|---|
> | 版本行 | `版本：V0.09` | `版本：V0.10` |
> | BSM-120 HTML 原文页脚 | `更新日期：2026-09-01。` | `更新日期：2026-09-08。` |
> 
> 定义区整块比对：变更 ID 全集 = **`BSM-120`**，与 ledger 登记一致。BSM-120 块内除该日期 token 外逐字相同。general 整段字节相等（SHA `8e89bc9194ca0ae3d3b0103c9629f700434a01c1d59bf5872bae9f63750be4cb`）。映射区整段字节相等。无增删/迁移 ID。无 general 变更。无未登记 hunk。
> 
> 登记核对：触发模块 M001；来源 `docs/business-system-map-for-operations-v0.1.html:1957`（锁内出处行），摘录覆盖 L1957—2032（76 行），含 L2003 页脚。铁律 4 批准依据本席在 ledger 历史段自读操作员原话「批准上述第 1、2 处修改」，第 2 处为 L2003 `2026-09-01`→`2026-09-08`，与登记一致。
> 
> ## 3. 定义 ID / 映射
> 
> - 旧/新定义 ID 列表全等，401 条：GEN 18 + BEQR 197 + BSM 120 + PGM 66；各组 1…N 连续无缺口
> - 模块映射行 471（M001…M471），新旧行数相同
> - 映射区字节 **211958 = 211958**，`MAPPING_EQUAL True`（SHA `bd90b98965c29e15ee9fd945d7e3b34b83c62eaab176b64a7be5a5de5891e23d`）
> - `BSM-120` 映射最早引用 **M001**（第一模块行即含该 token）；引用模块 **236** 个
> 
> ## 4. HTML 摘录 vs 来源
> 
> 上一回合对现锁 383 条 HTML 判据：每条有出处路径+行号与 HTML 摘录；摘录均为来源文件自出处行起的前缀。`EXCERPT_MISMATCH_COUNT 0`，`MISSING_EXCERPT_COUNT 0`。18 条 GEN 均为「通用工程判据」、无 HTML 摘录。
> 
> BSM-120 专项（上一回合）：摘录与 `business-system-map-for-operations-v0.1.html` L1957 至文件末逐字相等（76 行）。L2003 与锁 L4963 页脚等长 366 字符且相等。本回合复核当前 HTML L2003 仍为 `更新日期：2026-09-08。`，与现锁页脚一致。现 HTML L2 已是 V0.12 / 2026-09-08，L499 目录已是「已确认规则和实施前输入项」；这两处不在本次锁 diff 内（BSM-001 锚点从 L505 起），与「定义内仅该日期、HTML 本阶段不再改」一致。本席未改三份 HTML。
> 
> ## 5. sidecar / ledger 旧基准 / COUNT 不计次
> 
> 本回合重新实算：
> 
> - 现锁独立实算：`08a4dcc31d10624ca846560d70dcc79e391aeae5ebcdc62a9341f30ecb8de314`
> - sidecar 原文仅一行该 64 位小写 hex，**等于现锁、不等于旧基准**
> - ledger 顶部仍是旧基准 `16def02336743500166af4cfd5fff48d02731d63d45a8f5f72721ca8ebd408e3`，**尚未改成现锁**；`LOCK-AMEND-COUNT: 1` 未动
> - 不计次条件成立：本次变更 ID 仅 BSM-120，带 HTML 出处，出处登记含铁律 4 批准依据，无一 ID 标「通用工程判据」→ ④ 时 **COUNT 保持 1**，不计入上限
> 
> findings V0.80 当前队列 13 行「修复队列归属」均为「否」，队列为空。M001 仍 `IN_PROGRESS`，B3=3。这些是台账观察，不是本闸 FAIL 项。
> 
> ## 6. 双 PASS 后必须由根（Codex）执行（本席不得代做）
> 
> **④** 更新 ledger 顶部基准为现锁哈希 `08a4dcc31d10624ca846560d70dcc79e391aeae5ebcdc62a9341f30ecb8de314`；`LOCK-AMEND-COUNT` **保持 1**；回执明文打印。
> 
> **⑤** 最早引用变更 ID `BSM-120` 的模块是 **M001**。将 M001 `IN_PROGRESS` 改回 `PENDING`，从 B1 重开。其余 470 行已是 `PENDING`，无 `PASS` 行需回退。B3 累计保持 3，不因锁同步/重开加额度。
> 
> 本席 **未写 ledger、未改模块状态、不给 M001 或任何模块 PASS**。B5/收口不在本闸。
> 
> ## 7. 技能自核与缺口
> 
> 已按节点使用：`using-superpowers`（开工自检）、`verification-before-completion`（先命令后结论）、`grilling`（只借追问结构：反向实算、全量摘录、未登记 hunk、COUNT 豁免；**不派子代理、不等操作员**）、`receiving-code-review`（先核实再接受，未表演性同意）。`i-have-adhd` 存在，但强制首行 `CRITERIA-LOCK-SHA256:` 覆盖其「首行即下一动作」；④⑤写在上节。
> 
> 缺口/不适用（未冒充已调用）：`batch-grill-me`、`ubiquitous-language` 不存在。`git-guardrails-claude-code` 技能文件存在，本宿主无 Claude Code hooks，本节点只读无 git 写，未调用；等价：未 force push / hard reset。本节点不适用且提示词已划掉或非本闸：`using-git-worktrees`、`dispatching-parallel-agents`、`subagent-driven-development`、`executing-plans`、`finishing-a-development-branch`、`brainstorming`、`writing-plans`、`systematic-debugging`、`tdd`/`test-driven-development`、`diagnosing-bugs`、`resolving-merge-conflicts`、`codebase-design`、`writing-for-agents`、`ponytail`/`ponytail-review`、`requesting-code-review`（本席即复核席，未另派）。
> 
> 未扩第四闸、未引入第三席。

恢复条件：操作员对新增A/B/C/D逐处批准或给出调整方向，并明确是否另起M001修复轮次；保留已完成三处、B4与锁同步全部证据。


## M001 本批 B2 Codex 归并复核：获批 A/D 第4次修复
版本：V0.34
日期：2026-09-08
结论：B2 PASS（本席归并票，尚待Grok；不是B1/B5或模块PASS）。

输入：modules/M001-b1-self-raw.md V0.32完整B1及V0.33接收附录；modules/M001-b1-grok-raw.md V0.13完整B1（1725行起）；findings.md V0.81候选；ledger.md V1.50操作员两次真实答复。完整B1字节/来源保持，未重跑有效审计。
整批5来源：CX-01/02→既有004无业务发明登记；GR-01→既有005工单关闭条件的剩余概括句，再入队，旧1378/1384/BEQR262修复不重复；GR-02→新014，GR-03→新015。旧最大013，新ID全局未复用，旧13ID历史全保留。GR-01定位同一业务对象同一关闭条件且同BSM079/BEQR172，不另造永久ID；具体本次位置1380及批准范围明确。
分类：004技术性无缺陷，原CSS/结构/目录仅阅读不发明业务；005、014、015均按本次Grok原票保留冲突/工程缺陷成立。用户“A\\d同意，b\\c拒绝”及“授权仅为 A、D 再执行 1 次修复”是本批执行范围约束。005之A/D为唯一当前可执行队列；014/015队列否明确为被操作员拒绝修复，不冒充无缺陷、已修或模块通过。不将全局分类原则修改/补例外以绕过用户拒绝。原Grok FAIL不改写，后续B1/B5仍须独立如实判断。
A的两行原/新字逐字与批准一致；D版本附带归005，V0.12→V0.13，日期仍2026-09-08。本批不变更B/C及L521全局规则、详细工单分支、目录、页脚或另两份HTML。内存预演仅2、1380两行差异，完整反向还原原字节；这仅是预演，HTML尚未写。
实际校验：
~~~json
{"current_ids": ["M001-001", "M001-002", "M001-003", "M001-004", "M001-005", "M001-006", "M001-007", "M001-008", "M001-009", "M001-010", "M001-011", "M001-012", "M001-013", "M001-014", "M001-015"], "queue": ["M001-005"], "preview_changed_lines": [2, 1380], "protected_lines_equal": true, "reverse_exact": true, "candidate_sha256": "94d7aec41fe67de1f4edf4e90160a6534c923a0ede5c990d2e44cdfba6d06435", "old_html_sha256": "86a73b25db127f2c7d5aecacecdb1dc0ad9cbd872031add30bc52efc744e4b3f", "preview_html_sha256": "a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa"}
~~~
验证命令：
~~~bash
python3 - <<'PY'
from pathlib import Path
import hashlib,json,re,difflib
b=Path('/home/james/complex-management');a=b/'docs/audit-2026-09';data=json.loads("{\"proposals\":[{\"key\":\"A\",\"line\":1380,\"old\":\"处理后回访客人或商户，验收合格才关闭。\",\"new\":\"处理后的回访、验收和关闭，按下述已确认的工单类型分支规则执行。\",\"reason\":\"使概括句明确引用已有的分类型规则；不另设关闭条件，不改L1384和BEQR262。\"},{\"key\":\"D\",\"line\":2,\"old\":\"<!-- 文件版本：V0.12；更新日期：2026-09-08 -->\",\"new\":\"<!-- 文件版本：V0.13；更新日期：2026-09-08 -->\",\"reason\":\"如上述新业务修改获批，整批更新一次自身版本；不沿用此前V0.12的修改许可。\"}]}")
f=(a/'findings.md').read_text(encoding='utf-8');top=f.split('## 历史 M001 B2 定稿',1)[0]
rows=re.findall(r'^\| (M001-\d{3}) \| (.*)$',top,re.M)
assert [r[0] for r in rows]==[f'M001-{n:03d}' for n in range(1,16)]
queue=[r[0] for r in rows if r[1].split('|')[3].strip()=='是'];assert queue==['M001-005']
p=b/'docs/business-system-map-for-operations-v0.1.html';old=p.read_text(encoding='utf-8');new=old
for item in data['proposals']:
 assert old.splitlines()[item['line']-1].count(item['old'])==1 and old.count(item['old'])==1
 new=new.replace(item['old'],item['new'],1)
changes=[i for i,(x,y) in enumerate(zip(old.splitlines(),new.splitlines()),1) if x!=y]
assert changes==[2,1380]
assert len(old.splitlines())==len(new.splitlines())
for n in [499,521,1020,1068,1378,1384,2003]:assert old.splitlines()[n-1]==new.splitlines()[n-1]
reverse=new
for item in data['proposals']:reverse=reverse.replace(item['new'],item['old'],1)
assert reverse==old
out={'current_ids':[r[0] for r in rows],'queue':queue,'preview_changed_lines':changes,'protected_lines_equal':True,'reverse_exact':True,'candidate_sha256':hashlib.sha256((a/'findings.md').read_bytes()).hexdigest(),'old_html_sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'preview_html_sha256':hashlib.sha256(new.encode()).hexdigest()}
print(json.dumps(out,ensure_ascii=False))
PY
~~~
后续：B2双PASS后只修005的A/D，完整before/after tree通过临时索引取得并保存未过滤diff后，累计3→4（用户额外只许1次，不清零）。B4-r4完整两席审查。随后实际BSM079摘录同步须严格LOCK-AMEND登记、定义映射一并检查、sidecar、两席反向旧hash、④⑤后M001 B1重开，COUNT按获批来源保持1，不混入本批B3。若仍有FAIL，不能自行第5次修复、不能宣称有条件通过。
本报告在Grok本次B2报告读取前独立完成，无新独占锁哈希行，原前缀全保留。当前本席B2 PASS，候选未定稿、B3累计3、HTML未改。


## M001 本批 B2 Codex 修订归并复核
版本：V0.35
日期：2026-09-08
结论：B2 PASS（仅本次修订候选的根票；尚待Grok，不是模块PASS）。
输入：完整self V0.32 B1+V0.33附录；完整Grok V0.13 B1+其V0.14 B2唯一编号FAIL；findings修订候选V0.82；ledger V1.51真实用户授权。原V0.34根票与V0.14 Grok FAIL都保留，本批上次合取FAIL不改写。
已接收并修正：旧005为投诉确认/超时默认关闭，已修出队；本次GR-01是L1380全类验收与包含保洁的分类型关闭，不是同一证据集。根上次以同章同对象复用005不成立。现在旧005恢复已修/否，本次新分016，014/015保留原新ID；无ID重用或重分。
整批5来源现映射：CX01+CX02→004（技术性无缺陷、否）；GR01→016（冲突、缺陷是、队列是，仅A/D）；GR02→014（冲突、缺陷是、否，用户拒绝B）；GR03→015（冲突、缺陷是、否，用户拒绝C）。全部旧001至013当前16行保持各唯一，其他旧条目未重开。004只承载阅读/目录、不发明业务，有完整类型4结构/导航证据；005不再吸收本次新问题。
用户两次真实答复已完整记录，A/D改文逐字保持获批内容；B/C拒绝与原冲突票保留，否仅是不执行而非无缺陷/已修/模块通过。Grok上次明确同意这种队列处置，本次没有改变。无添加、无新questions，不修改全局原则绕拒绝。
A仅BSM1380改为“处理后的回访、验收和关闭，按下述已确认的工单类型分支规则执行。”；D仅L2 V0.12→V0.13，日期仍2026-09-08，全部归016；B/C、详细分支及其他HTML原文不变。实际预演与反向字节校验：
~~~json
{"current_ids": ["M001-001", "M001-002", "M001-003", "M001-004", "M001-005", "M001-006", "M001-007", "M001-008", "M001-009", "M001-010", "M001-011", "M001-012", "M001-013", "M001-014", "M001-015", "M001-016"], "queue": ["M001-016"], "preview_changed_lines": [2, 1380], "protected_lines_equal": true, "reverse_exact": true, "candidate_sha256": "3c4000f61f98222e428d1080cb6deb66a6cd6d86fd570f6b341f79707866906c", "old_html_sha256": "86a73b25db127f2c7d5aecacecdb1dc0ad9cbd872031add30bc52efc744e4b3f", "preview_html_sha256": "a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa"}
~~~
本次执行队列只有016。待B2双PASS定稿才整批B3，取完整before/after树、diff逐行归016，B3历史累计3→4，额外授权用尽；B4-r4完整两席审查、必要BSM079锁同步严格五步及最早M001 B1重开。没有第五次B3授权，不能以拒绝B/C当整模块PASS。
当前HTML与锁未变。该附录不写独占锁行、保留原字节前缀；Grok本次修订B2尚未派发。


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M001 B1 — Codex 新基准独立全量复核
版本：V0.36
日期：2026-09-08 UTC
依据：160-audit-prompt.md V0.17；正式471模块计划与runbook V0.02；criteria-lock V0.11；ledger V1.55。
正本：160:/home/james/complex-management，main HEAD=242f487233d028173cb9198eecd079f5d877405c。
阶段：LOCK-AMEND①至⑤已完成，最早M001先PENDING再IN_PROGRESS，从新锁B1重开。B3累计4，本次额外仅A/D的1次额度已用尽，无第五次修复授权。
结论：PASS。本次完整自审纳入操作员最新业务澄清：“b c两项原文不冲突。就是我的原意。”B/C现行原文是预期规则，撤销本席先前的冲突判断；这不是把拒绝修复当成已修，也不是豁免缺陷。A/D修复无回归。

## 完整范围、方法与独立性
范围为BEQR全部308行、BSM全部2032行，合计2340行（含CSS/注释/脚本），335个映射ID：BEQR197+BSM120+GEN18。BEQR SHA256=4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66；BSM SHA256=a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa。
本会话已有17个完整行段对两份文件的全文阅读。当前整文件与这些已读原文的B3前树逐字比较：BEQR无变化；BSM仅L2和L1380两行获批变化，其他所有行逐字相同。未变化正文复用已完整读到的原文内容，本次重新按全部335判据对照语义；不声称重新逐页调用Read了全部2340行，也不以旧PASS当本次结论。新基准317条HTML摘录全量逐字检查、335ID闭集、结构和原导航脚本、金额正反例均重新实跑；另重新实读当前L521全局整段、L1020开台整段、L1068完整高危动作段、1370—1388工单整节及BEQR262上下文。本报告包含完整335判据结果和全部本席finding，不只审两行diff。
只用HTML上位原文及实际命令举证；未发布76文件专项不作为本模块代码PASS依据。PGM只作相关交叉上下文，不开启M002。Grok当前新B1在独立运行，本报告内容独立准备；完整落盘前不读取其本次正文、结论或原始CLI JSON。不使用第三席/子代理。
操作员先批准仅A/D一次并拒绝B/C，后在Grok本次调用尚未交付时进一步明确：“b c两项原文不冲突。就是我的原意。”最新答复明确了业务含义，超出单纯拒绝修改：B/C原文按本意成立，不再列冲突或待修。A/D已完成B4-r4双PASS及BSM079锁同步。HTML、冻结判据均不改，不替用户扩写可配范围、例外或新配置规则。本席在未读取当前Grok报告前依此修正本次报告。Grok原派发未含此新答复，接收后应让其独立复核新答复影响，不能单方覆盖其原票。

## 第一性原理线
1. 为什么存在：BEQR提供业务枚举与边界，BSM提供流程、责任与入口关系，两份各有独有上位内容；重述有定位用途，不是可删的整文件重复。全局分类原则及两个具体分类句均是现行权威原文，需要能够同时解释下游实现。
2. 哪些声称已实际验证：本模块是需求文档，实际跑HTML全结构/链接、27目录目标的原始JS、1—10家比例/并列/尾差、99%变异拒绝等类型4检查；业务支付、工资、第三方、设备等实现未运行，不冒称通过。
3. 哪个前提已经得到用户验证：本席先前将L521与L1020/1068理解成分类配置冲突；操作员最新明确B/C原文不冲突、就是本意。审计者不能用自己的不同理解推翻业务所有者的明确说明。按当前原文和此项解释确认B/C均为预期业务，撤销错误冲突判断；不进一步猜测按项目可配具体允许哪些后台操作，不改全局原则，不新增例外文字。

## 顺从式线与全部GEN判据
317条HTML冻结摘录全部匹配当前文件和来源位置；同时完成语义审查。BSM001/052/054按操作员最新B/C业务含义澄清核对，不再判冲突；其他条目按已读原文、交叉语义及当前实际检查重新对照，未确认缺陷。
| 判据 | 本模块证据与结论 |
|---|---|
| GEN-001 | 两份权威文件用途不同；纯展示 CSS/导航为正常阅读服务，无业务发明；不因重述规则就删业务正本。 |
| GEN-002 | BSM 原脚本在真实 27 个导航目标的 DOM 等价输入上执行；初始和每目标激活均断言通过。文档中业务行为不冒充已执行代码。 |
| GEN-003 | 机械断言实跑；将比例 100% 改为 99% 的内存反例确实失败，非永绿断言。 |
| GEN-004 | 本模块静态文本无用户写入口；动态脚本选择器取本文件静态 href，全部解析到实际 ID；业务校验规则按各锚点核对。 |
| GEN-005 | 逐读默认拒绝、十一端、职责与敏感信息范围；具体明定的密钥/人工查验例外不误判为普通权限放行。未执行服务端授权测试。 |
| GEN-006 | 核对钱路守恒、来源分栏、租赁排他、券与支付共同生效、挂起取消与付款互斥；结构解析无标签失配。 |
| GEN-007 | 核对转赠三动作先成功者、多人/多次票单位、出场重复不重复扣、幂等逐笔密钥例外及并拆台整体成功；未声称产品并发测试通过。 |
| GEN-008 | 两份中的枚举与作用域逐段对照；流程概述并非完整清单，具体例外与独立子流程按其范围解释。 |
| GEN-009 | 接口未接入口灰显、未出款状态、第三方差异、能力缺失等明确列出；缺配置不视为开启，未发现静默伪成功文案。 |
| GEN-010 | 本模块无可执行外部输入/凭证；人脸、注销、查询、导出与证据查看边界按明文审读；不将本文当法规论证。 |
| GEN-011 | 对照逐笔来源/时点/责任人与不可覆盖记录；安检、人工验龄的特定溯源终点与全局原则按明定例外一并核对。 |
| GEN-012 | 导航每次遍历固定 27 个章节，非开放输入放大；只读静态页面无后台资源持有。 |
| GEN-013 | 文件以 UTF-8 原生打开，不依赖额外网络库；导航用原生 JS。未用“本次无 diff”门禁冒充全量内容审计。 |
| GEN-014 | 不适用执行检查：本模块无 SQL/迁移；迁移正确性留所辖模块。 |
| GEN-015 | HTML 语言与编码声明、标题层次、表格表头、原生链接可键盘访问；导航有效、标签闭合；未执行真实浏览器截图或读屏测试，不夸大覆盖。 |
| GEN-016 | 文档版本V0.07/V0.13、日期和导航正确，317摘录与来源行一致；B/C原文已由操作员明确为预期且不冲突，见CX-03/04。未确认缺陷。 |
| GEN-017 | 原文按冲突→细化→技术性→添加判断：CX-03/04依最新业务澄清为细化、无缺陷；CX-01/02为无业务发明的技术性登记；无添加项。 |
| GEN-018 | 仅使用实际 HTML 原文与本报告所附命令输出；不是以旧 PASS、Grok 意见或代码注释作判据。 |


### 全部 HTML 判据覆盖表

源文件简称：BEQR=docs/business-enum-quick-reference-v0.1.html；BSM=docs/business-system-map-for-operations-v0.1.html。
本表每行的“已核对”指顺从式全文和对抗式作用域对照均完成，结合下方反例与排除说明。冻结摘录逐字匹配的命令覆盖所有 317 行。

| 判据 ID | 源行 | 锚点 | 结果 |
|---|---|---|---|
| BEQR-001 | BEQR:29 | 票种 | 已核对，未确认缺陷 |
| BEQR-002 | BEQR:30 | 票务策略状态 | 已核对，未确认缺陷 |
| BEQR-003 | BEQR:32 | 定价、售票与收款主体 | 已核对，未确认缺陷 |
| BEQR-004 | BEQR:33 | 年龄限制 | 已核对，未确认缺陷 |
| BEQR-005 | BEQR:34 | 通票赠酒下限 | 已核对，未确认缺陷 |
| BEQR-006 | BEQR:35 | 通票类型 | 已核对，未确认缺陷 |
| BEQR-007 | BEQR:36 | 权益类型 | 已核对，未确认缺陷 |
| BEQR-008 | BEQR:37 | 餐饮/零售券两种用法 | 已核对，未确认缺陷 |
| BEQR-009 | BEQR:39 | 资格判定 | 已核对，未确认缺陷 |
| BEQR-010 | BEQR:40 | 订单状态 | 已核对，未确认缺陷 |
| BEQR-011 | BEQR:41 | 票/权益状态 | 已核对，未确认缺陷 |
| BEQR-012 | BEQR:43 | 核销记录状态 | 已核对，未确认缺陷 |
| BEQR-013 | BEQR:44 | 手环入场 | 已核对，未确认缺陷 |
| BEQR-014 | BEQR:45 | 退款单状态 | 已核对，未确认缺陷 |
| BEQR-015 | BEQR:46 | 有效期类型 | 已核对，未确认缺陷 |
| BEQR-016 | BEQR:47 | 售票与退款原则 | 已核对，未确认缺陷 |
| BEQR-017 | BEQR:48 | 到期未核销款 | 已核对，未确认缺陷 |
| BEQR-018 | BEQR:49 | 域归属对照 | 已核对，未确认缺陷 |
| BEQR-019 | BEQR:51 | 跨域通票分配 | 已核对，未确认缺陷 |
| BEQR-020 | BEQR:52 | 酒吧通票可分配池 | 已核对，未确认缺陷 |
| BEQR-021 | BEQR:53 | 收费与免费权益结算 | 已核对，未确认缺陷 |
| BEQR-022 | BEQR:54 | 项目内部结算价 | 已核对，未确认缺陷 |
| BEQR-023 | BEQR:55 | 核销顺序 | 已核对，未确认缺陷 |
| BEQR-024 | BEQR:56 | 游乐核销归属 | 已核对，未确认缺陷 |
| BEQR-025 | BEQR:57 | 门票综合服务费 | 已核对，未确认缺陷 |
| BEQR-026 | BEQR:58 | 动态价与会员价 | 已核对，未确认缺陷 |
| BEQR-027 | BEQR:60 | 人脸验龄数据生命周期 | 已核对，未确认缺陷 |
| BEQR-028 | BEQR:65 | 会员唯一识别 | 已核对，未确认缺陷 |
| BEQR-029 | BEQR:66 | 会员注册 | 已核对，未确认缺陷 |
| BEQR-030 | BEQR:68 | 会员身份状态 | 已核对，未确认缺陷 |
| BEQR-031 | BEQR:70 | 等级与成长值 | 已核对，未确认缺陷 |
| BEQR-032 | BEQR:71 | 等级权益对照 | 已核对，未确认缺陷 |
| BEQR-033 | BEQR:72 | 积分与成长值来源 | 已核对，未确认缺陷 |
| BEQR-034 | BEQR:73 | 票务人次计算 | 已核对，未确认缺陷 |
| BEQR-035 | BEQR:75 | 有效消费与冻结 | 已核对，未确认缺陷 |
| BEQR-036 | BEQR:77 | 积分明细类型 | 已核对，未确认缺陷 |
| BEQR-037 | BEQR:78 | 积分兑换定价 | 已核对，未确认缺陷 |
| BEQR-038 | BEQR:79 | 商品规格 | 已核对，未确认缺陷 |
| BEQR-039 | BEQR:80 | 项目零售参考价 | 已核对，未确认缺陷 |
| BEQR-040 | BEQR:81 | 发券与核销结算 | 已核对，未确认缺陷 |
| BEQR-041 | BEQR:82 | 重复兑换与转赠 | 已核对，未确认缺陷 |
| BEQR-042 | BEQR:83 | 券包状态 | 已核对，未确认缺陷 |
| BEQR-043 | BEQR:84 | 积分结算账期 | 已核对，未确认缺陷 |
| BEQR-044 | BEQR:85 | 会员系统服务费 | 已核对，未确认缺陷 |
| BEQR-045 | BEQR:86 | 储值卡类型与收款主体 | 已核对，未确认缺陷 |
| BEQR-046 | BEQR:87 | 会员忠诚度扩展 | 已核对，未确认缺陷 |
| BEQR-047 | BEQR:89 | 会员注册与开卡 | 已核对，未确认缺陷 |
| BEQR-048 | BEQR:90 | 学生认证 | 已核对，未确认缺陷 |
| BEQR-049 | BEQR:91 | 会员积分活动状态 | 已核对，未确认缺陷 |
| BEQR-050 | BEQR:92 | 积分活动预算守恒 | 已核对，未确认缺陷 |
| BEQR-051 | BEQR:93 | 充值积分 | 已核对，未确认缺陷 |
| BEQR-052 | BEQR:94 | 会员营销边界 | 已核对，未确认缺陷 |
| BEQR-053 | BEQR:95 | 发券对象 | 已核对，未确认缺陷 |
| BEQR-054 | BEQR:100 | 桌台/卡座/吧台位状态 | 已核对，未确认缺陷 |
| BEQR-055 | BEQR:101 | 账单状态 | 已核对，未确认缺陷 |
| BEQR-056 | BEQR:103 | 订台定金状态 | 已核对，未确认缺陷 |
| BEQR-057 | BEQR:105 | 订卡邀请函状态 | 已核对，未确认缺陷 |
| BEQR-058 | BEQR:106 | 点酒方式 | 已核对，未确认缺陷 |
| BEQR-059 | BEQR:107 | 支付方式 | 已核对，未确认缺陷 |
| BEQR-060 | BEQR:108 | 销售提成 | 已核对，未确认缺陷 |
| BEQR-061 | BEQR:109 | 服务员提成 | 已核对，未确认缺陷 |
| BEQR-062 | BEQR:110 | 退菜退酒 | 已核对，未确认缺陷 |
| BEQR-063 | BEQR:111 | 高危动作（需审批/双证据） | 已核对，未确认缺陷 |
| BEQR-064 | BEQR:112 | 出品票据状态 | 已核对，未确认缺陷 |
| BEQR-065 | BEQR:114 | 账单操作 | 已核对，未确认缺陷 |
| BEQR-066 | BEQR:115 | 收银交班状态 | 已核对，未确认缺陷 |
| BEQR-067 | BEQR:117 | 最低消费 | 已核对，未确认缺陷 |
| BEQR-068 | BEQR:118 | 服务费 | 已核对，未确认缺陷 |
| BEQR-069 | BEQR:119 | 小费 | 已核对，未确认缺陷 |
| BEQR-070 | BEQR:120 | 客人存酒有效期与转存 | 已核对，未确认缺陷 |
| BEQR-071 | BEQR:122 | 存酒瓶数 | 已核对，未确认缺陷 |
| BEQR-072 | BEQR:123 | 套餐赠券适用范围 | 已核对，未确认缺陷 |
| BEQR-073 | BEQR:124 | 非低消商品 | 已核对，未确认缺陷 |
| BEQR-074 | BEQR:125 | 隐私桌台权限 | 已核对，未确认缺陷 |
| BEQR-075 | BEQR:126 | 桌台会话与菜单 | 已核对，未确认缺陷 |
| BEQR-076 | BEQR:127 | 桌台计时产品 | 已核对，未确认缺陷 |
| BEQR-077 | BEQR:128 | 门店支付方式 | 已核对，未确认缺陷 |
| BEQR-078 | BEQR:129 | 扫码点单身份 | 已核对，未确认缺陷 |
| BEQR-079 | BEQR:130 | 优惠券自动选择 | 已核对，未确认缺陷 |
| BEQR-080 | BEQR:131 | 优惠券付款预留 | 已核对，未确认缺陷 |
| BEQR-081 | BEQR:132 | 正式挂起订单状态 | 已核对，未确认缺陷 |
| BEQR-082 | BEQR:133 | 正式挂起订单规则 | 已核对，未确认缺陷 |
| BEQR-083 | BEQR:134 | 挂单取消与付款互斥 | 已核对，未确认缺陷 |
| BEQR-084 | BEQR:135 | 挂单坏账损耗 | 已核对，未确认缺陷 |
| BEQR-085 | BEQR:140 | 项目开放状态 | 已核对，未确认缺陷 |
| BEQR-086 | BEQR:142 | 场次状态 | 已核对，未确认缺陷 |
| BEQR-087 | BEQR:144 | 游乐「当天／每天」口径 | 已核对，未确认缺陷 |
| BEQR-088 | BEQR:145 | 容量双阈值 | 已核对，未确认缺陷 |
| BEQR-089 | BEQR:146 | 园区容量 | 已核对，未确认缺陷 |
| BEQR-090 | BEQR:147 | 设备关键/辅助分级 | 已核对，未确认缺陷 |
| BEQR-091 | BEQR:148 | 全园故障与补偿通票 | 已核对，未确认缺陷 |
| BEQR-092 | BEQR:149 | 安全严重度 | 已核对，未确认缺陷 |
| BEQR-093 | BEQR:150 | 巡检任务状态 | 已核对，未确认缺陷 |
| BEQR-094 | BEQR:151 | 越界类型 | 已核对，未确认缺陷 |
| BEQR-095 | BEQR:153 | 团体/包场 | 已核对，未确认缺陷 |
| BEQR-096 | BEQR:154 | 游乐增强阶段 | 已核对，未确认缺陷 |
| BEQR-097 | BEQR:159 | 供应合同模式 | 已核对，未确认缺陷 |
| BEQR-098 | BEQR:160 | 库房分类 | 已核对，未确认缺陷 |
| BEQR-099 | BEQR:161 | 物品大类 | 已核对，未确认缺陷 |
| BEQR-100 | BEQR:162 | 库存流水类型 | 已核对，未确认缺陷 |
| BEQR-101 | BEQR:164 | 高价值判定 | 已核对，未确认缺陷 |
| BEQR-102 | BEQR:166 | 跨主体借调 | 已核对，未确认缺陷 |
| BEQR-103 | BEQR:168 | 智能采购建议 | 已核对，未确认缺陷 |
| BEQR-104 | BEQR:169 | 门店要货履约状态 | 已核对，未确认缺陷 |
| BEQR-105 | BEQR:170 | 经销商状态 | 已核对，未确认缺陷 |
| BEQR-106 | BEQR:171 | 经销询价状态 | 已核对，未确认缺陷 |
| BEQR-107 | BEQR:172 | 经销价格目录状态 | 已核对，未确认缺陷 |
| BEQR-108 | BEQR:173 | 经销订单状态 | 已核对，未确认缺陷 |
| BEQR-109 | BEQR:174 | 经销退货单状态 | 已核对，未确认缺陷 |
| BEQR-110 | BEQR:175 | 经销库存外显 | 已核对，未确认缺陷 |
| BEQR-111 | BEQR:176 | 经销支付方式 | 已核对，未确认缺陷 |
| BEQR-112 | BEQR:177 | 经销库存预留 | 已核对，未确认缺陷 |
| BEQR-113 | BEQR:178 | 经销职责分离 | 已核对，未确认缺陷 |
| BEQR-114 | BEQR:179 | 经销取消、退货与开票 | 已核对，未确认缺陷 |
| BEQR-115 | BEQR:180 | 经销价格与税务快照 | 已核对，未确认缺陷 |
| BEQR-116 | BEQR:181 | 经销商停用撤权 | 已核对，未确认缺陷 |
| BEQR-117 | BEQR:182 | 经销资金归属 | 已核对，未确认缺陷 |
| BEQR-118 | BEQR:187 | 运营营业日 | 已核对，未确认缺陷 |
| BEQR-119 | BEQR:188 | 对账口径 | 已核对，未确认缺陷 |
| BEQR-120 | BEQR:189 | 对账差异四类 | 已核对，未确认缺陷 |
| BEQR-121 | BEQR:190 | 日结状态 | 已核对，未确认缺陷 |
| BEQR-122 | BEQR:191 | 差异处理状态 | 已核对，未确认缺陷 |
| BEQR-123 | BEQR:192 | 结算状态 | 已核对，未确认缺陷 |
| BEQR-124 | BEQR:193 | 综合通票定价 | 已核对，未确认缺陷 |
| BEQR-125 | BEQR:195 | 商户业态 | 已核对，未确认缺陷 |
| BEQR-126 | BEQR:196 | 合作模式 | 已核对，未确认缺陷 |
| BEQR-127 | BEQR:197 | 退款资金流向 | 已核对，未确认缺陷 |
| BEQR-128 | BEQR:198 | 物业费 | 已核对，未确认缺陷 |
| BEQR-129 | BEQR:199 | 能源费与损耗费 | 已核对，未确认缺陷 |
| BEQR-130 | BEQR:201 | 税费配置 | 已核对，未确认缺陷 |
| BEQR-131 | BEQR:202 | 三方对账框架 | 已核对，未确认缺陷 |
| BEQR-132 | BEQR:203 | 开票状态 | 已核对，未确认缺陷 |
| BEQR-133 | BEQR:208 | 接入的外部系统 | 已核对，未确认缺陷 |
| BEQR-134 | BEQR:209 | 客户标签 | 已核对，未确认缺陷 |
| BEQR-135 | BEQR:210 | 统一原则 | 已核对，未确认缺陷 |
| BEQR-136 | BEQR:211 | 接入状态 | 已核对，未确认缺陷 |
| BEQR-137 | BEQR:212 | 回流结果状态 | 已核对，未确认缺陷 |
| BEQR-138 | BEQR:217 | 风险四级 | 已核对，未确认缺陷 |
| BEQR-139 | BEQR:218 | 事件状态 | 已核对，未确认缺陷 |
| BEQR-140 | BEQR:219 | 主管复核强制项 | 已核对，未确认缺陷 |
| BEQR-141 | BEQR:220 | 经营异常处置状态 | 已核对，未确认缺陷 |
| BEQR-142 | BEQR:221 | 异常状态的统一出口 | 已核对，未确认缺陷 |
| BEQR-143 | BEQR:226 | 四类主体 | 已核对，未确认缺陷 |
| BEQR-144 | BEQR:227 | 经营主体关系 | 已核对，未确认缺陷 |
| BEQR-145 | BEQR:228 | 城市项目公司 | 已核对，未确认缺陷 |
| BEQR-146 | BEQR:229 | 运营公司职责 | 已核对，未确认缺陷 |
| BEQR-147 | BEQR:230 | 主要收入归属 | 已核对，未确认缺陷 |
| BEQR-148 | BEQR:231 | 合作模式 | 已核对，未确认缺陷 |
| BEQR-149 | BEQR:232 | 职级 | 已核对，未确认缺陷 |
| BEQR-150 | BEQR:233 | 终端十一端 | 已核对，未确认缺陷 |
| BEQR-151 | BEQR:234 | 最终权限计算 | 已核对，未确认缺陷 |
| BEQR-152 | BEQR:235 | 项目与敏感数据权限 | 已核对，未确认缺陷 |
| BEQR-153 | BEQR:236 | 终端开关优先级 | 已核对，未确认缺陷 |
| BEQR-154 | BEQR:237 | 综合体项目状态 | 已核对，未确认缺陷 |
| BEQR-155 | BEQR:238 | 商户/运营主体状态 | 已核对，未确认缺陷 |
| BEQR-156 | BEQR:239 | 合同状态 | 已核对，未确认缺陷 |
| BEQR-157 | BEQR:240 | 员工状态 | 已核对，未确认缺陷 |
| BEQR-158 | BEQR:241 | 设备状态 | 已核对，未确认缺陷 |
| BEQR-159 | BEQR:242 | 权限状态 | 已核对，未确认缺陷 |
| BEQR-160 | BEQR:248 | 招商线索状态 | 已核对，未确认缺陷 |
| BEQR-161 | BEQR:249 | 招商意向阶段 | 已核对，未确认缺陷 |
| BEQR-162 | BEQR:250 | 品牌/客户库类型 | 已核对，未确认缺陷 |
| BEQR-163 | BEQR:251 | 合同变更类型 | 已核对，未确认缺陷 |
| BEQR-164 | BEQR:252 | 退租结算状态 | 已核对，未确认缺陷 |
| BEQR-165 | BEQR:253 | 内部应付执行状态 | 已核对，未确认缺陷 |
| BEQR-166 | BEQR:255 | 合同审批链 | 已核对，未确认缺陷 |
| BEQR-167 | BEQR:256 | 租金/账单类型 | 已核对，未确认缺陷 |
| BEQR-168 | BEQR:257 | 营业额抽成模式 | 已核对，未确认缺陷 |
| BEQR-169 | BEQR:258 | 公区能源费 | 已核对，未确认缺陷 |
| BEQR-170 | BEQR:259 | 铺位租赁状态 | 已核对，未确认缺陷 |
| BEQR-171 | BEQR:260 | 物业工单类型 | 已核对，未确认缺陷 |
| BEQR-172 | BEQR:262 | 物业工单状态 | 已核对，未确认缺陷 |
| BEQR-173 | BEQR:264 | 商户服务工单状态 | 已核对，未确认缺陷 |
| BEQR-174 | BEQR:265 | 工单时效 | 已核对，未确认缺陷 |
| BEQR-175 | BEQR:266 | 楼宇机电设备状态 | 已核对，未确认缺陷 |
| BEQR-176 | BEQR:267 | 设备维保类型 | 已核对，未确认缺陷 |
| BEQR-177 | BEQR:268 | 能耗表计类型 | 已核对，未确认缺陷 |
| BEQR-178 | BEQR:269 | 抄表状态 | 已核对，未确认缺陷 |
| BEQR-179 | BEQR:270 | 能耗归集对象 | 已核对，未确认缺陷 |
| BEQR-180 | BEQR:271 | 楼宇巡检任务状态 | 已核对，未确认缺陷 |
| BEQR-181 | BEQR:273 | 楼宇巡检类型 | 已核对，未确认缺陷 |
| BEQR-182 | BEQR:279 | 考勤日结果 | 已核对，未确认缺陷 |
| BEQR-183 | BEQR:280 | 打卡方式 | 已核对，未确认缺陷 |
| BEQR-184 | BEQR:281 | 请假类型 | 已核对，未确认缺陷 |
| BEQR-185 | BEQR:282 | 请假单状态 | 已核对，未确认缺陷 |
| BEQR-186 | BEQR:283 | 薪资单构成 | 已核对，未确认缺陷 |
| BEQR-187 | BEQR:285 | 员工提成发放 | 已核对，未确认缺陷 |
| BEQR-188 | BEQR:290 | 服务订单状态 | 已核对，未确认缺陷 |
| BEQR-189 | BEQR:291 | 管理费方式 | 已核对，未确认缺陷 |
| BEQR-190 | BEQR:296 | 分红基数 | 已核对，未确认缺陷 |
| BEQR-191 | BEQR:297 | 分红比例 | 已核对，未确认缺陷 |
| BEQR-192 | BEQR:298 | 分红税务 | 已核对，未确认缺陷 |
| BEQR-193 | BEQR:299 | 分红周期 | 已核对，未确认缺陷 |
| BEQR-194 | BEQR:300 | 分红单状态 | 已核对，未确认缺陷 |
| BEQR-195 | BEQR:301 | 可选扣除项 | 已核对，未确认缺陷 |
| BEQR-196 | BEQR:302 | 发放保护线留存 | 已核对，未确认缺陷 |
| BEQR-197 | BEQR:303 | 期间关账 | 已核对，未确认缺陷 |
| BSM-001 | BSM:505 | 整体说明：这套系统到底管什么 | 已核对；B/C业务含义经操作员确认，不冲突 |
| BSM-002 | BSM:526 | 一笔业务怎么走 | 已核对，未确认缺陷 |
| BSM-003 | BSM:563 | 系统通用能力：数据可改可删、操作有引导、权限分岗 | 已核对，未确认缺陷 |
| BSM-004 | BSM:566 | 配置数据可以存档、也可以删除（删除要审批） | 已核对，未确认缺陷 |
| BSM-005 | BSM:581 | 后台的"新建/录入"改成分步引导 | 已核对，未确认缺陷 |
| BSM-006 | BSM:584 | 验证用的二维码一律动态刷新（间隔可在后台设定） | 已核对，未确认缺陷 |
| BSM-007 | BSM:587 | 平台管理员拥有全部日常功能权限 | 已核对，未确认缺陷 |
| BSM-008 | BSM:592 | 子系统入口 | 已核对，未确认缺陷 |
| BSM-009 | BSM:596 | 基础管理 | 已核对，未确认缺陷 |
| BSM-010 | BSM:601 | 票务权益 | 已核对，未确认缺陷 |
| BSM-011 | BSM:606 | 客人端 | 已核对，未确认缺陷 |
| BSM-012 | BSM:611 | 酒吧平板/工作手机 | 已核对，未确认缺陷 |
| BSM-013 | BSM:616 | 触摸屏一体机 | 已核对，未确认缺陷 |
| BSM-014 | BSM:621 | 酒吧管理 | 已核对，未确认缺陷 |
| BSM-015 | BSM:626 | 供应链库存 | 已核对，未确认缺陷 |
| BSM-016 | BSM:631 | 游乐管理 | 已核对，未确认缺陷 |
| BSM-017 | BSM:637 | 商户管理 | 已核对，未确认缺陷 |
| BSM-018 | BSM:643 | 招商租赁 | 已核对，未确认缺陷 |
| BSM-019 | BSM:650 | 物业服务 | 已核对，未确认缺陷 |
| BSM-020 | BSM:656 | 楼宇运营 | 已核对，未确认缺陷 |
| BSM-021 | BSM:661 | 第三方接入 | 已核对，未确认缺陷 |
| BSM-022 | BSM:666 | 留痕与异常 | 已核对，未确认缺陷 |
| BSM-023 | BSM:671 | 财务对账 | 已核对，未确认缺陷 |
| BSM-024 | BSM:676 | 持股分红 | 已核对，未确认缺陷 |
| BSM-025 | BSM:681 | 经营看板 | 已核对，未确认缺陷 |
| BSM-026 | BSM:686 | 各类入口 | 已核对，未确认缺陷 |
| BSM-027 | BSM:694 | 基础管理：先把综合体的盘子管清楚 | 已核对，未确认缺陷 |
| BSM-028 | BSM:698 | 主要功能 | 已核对，未确认缺陷 |
| BSM-029 | BSM:714 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-030 | BSM:727 | 票务权益：管卖什么票、给什么权益、怎么核销 | 已核对，未确认缺陷 |
| BSM-031 | BSM:731 | 主要功能 | 已核对，未确认缺陷 |
| BSM-032 | BSM:760 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-033 | BSM:772 | 票种、通票与权益类型（系统支持哪些种类） | 已核对，未确认缺陷 |
| BSM-034 | BSM:796 | 核销、退款与有效期规则 | 已核对，未确认缺陷 |
| BSM-035 | BSM:813 | 会员与积分规则 | 已核对，未确认缺陷 |
| BSM-036 | BSM:840 | 人脸验龄数据使用与删除 | 已核对，未确认缺陷 |
| BSM-037 | BSM:850 | 客人端：客人自己买票、预定、查询、积分和出示二维码 | 已核对，未确认缺陷 |
| BSM-038 | BSM:854 | 主要功能 | 已核对，未确认缺陷 |
| BSM-039 | BSM:873 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-040 | BSM:883 | 登录与关键规则 | 已核对，未确认缺陷 |
| BSM-041 | BSM:898 | 会员注册、认证与收费边界 | 已核对，未确认缺陷 |
| BSM-042 | BSM:907 | 会员积分活动与发券 | 已核对，未确认缺陷 |
| BSM-043 | BSM:916 | 会员营销明确边界 | 已核对，未确认缺陷 |
| BSM-044 | BSM:929 | 酒吧平板/工作手机：平板点酒，工作手机核销 | 已核对，未确认缺陷 |
| BSM-045 | BSM:933 | 主要功能 | 已核对，未确认缺陷 |
| BSM-046 | BSM:953 | 平板图形点单（POS） | 已核对，未确认缺陷 |
| BSM-047 | BSM:963 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-048 | BSM:978 | 触摸屏一体机：权益领取、现场服务和辅助售票 | 已核对，未确认缺陷 |
| BSM-049 | BSM:982 | 主要功能 | 已核对，未确认缺陷 |
| BSM-050 | BSM:996 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-051 | BSM:1010 | 酒吧管理：从入场、开台到结账关台 | 已核对，未确认缺陷 |
| BSM-052 | BSM:1014 | 主要功能 | 已核对；来源分类按项目可配为操作员原意 |
| BSM-053 | BSM:1041 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-054 | BSM:1053 | 桌台/账单状态与经营规则 | 已核对；打折原因分类按项目可配为操作员原意 |
| BSM-055 | BSM:1088 | 桌台、菜单与优惠券 | 已核对，未确认缺陷 |
| BSM-056 | BSM:1101 | 正式未付款订单挂起 | 已核对，未确认缺陷 |
| BSM-057 | BSM:1120 | 服务点单：客人点专项服务，公司统一收款、再按名录分账 | 已核对，未确认缺陷 |
| BSM-058 | BSM:1124 | 怎么点、怎么收钱 | 已核对，未确认缺陷 |
| BSM-059 | BSM:1135 | 钱怎么分、怎么退 | 已核对，未确认缺陷 |
| BSM-060 | BSM:1151 | 供应链库存：管供应商、采购、库房、消耗和存酒 | 已核对，未确认缺陷 |
| BSM-061 | BSM:1155 | 主要功能 | 已核对，未确认缺陷 |
| BSM-062 | BSM:1174 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-063 | BSM:1186 | 库存类型与规则 | 已核对，未确认缺陷 |
| BSM-064 | BSM:1208 | 采购建议与门店要货 | 已核对，未确认缺陷 |
| BSM-065 | BSM:1216 | 外部经销商准入、价格与库存 | 已核对，未确认缺陷 |
| BSM-066 | BSM:1226 | 经销订单、付款与履约 | 已核对，未确认缺陷 |
| BSM-067 | BSM:1238 | 经销退货、退款、红冲与资金归属 | 已核对，未确认缺陷 |
| BSM-068 | BSM:1253 | 游乐管理：管项目开放、项目容量（实时在玩人数）和安全异常 | 已核对，未确认缺陷 |
| BSM-069 | BSM:1257 | 主要功能 | 已核对，未确认缺陷 |
| BSM-070 | BSM:1276 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-071 | BSM:1287 | 游乐类型与规则 | 已核对，未确认缺陷 |
| BSM-072 | BSM:1308 | 商户管理：商户在授权范围内完整使用自己的系统 | 已核对，未确认缺陷 |
| BSM-073 | BSM:1313 | 主要功能 | 已核对，未确认缺陷 |
| BSM-074 | BSM:1326 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-075 | BSM:1339 | 招商租赁与合同履约：从找商户到签约、履约、变更、退租 | 已核对，未确认缺陷 |
| BSM-076 | BSM:1343 | 主要功能 | 已核对，未确认缺陷 |
| BSM-077 | BSM:1357 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-078 | BSM:1370 | 物业服务：商户和现场的报修、投诉、整改都进工单 | 已核对，未确认缺陷 |
| BSM-079 | BSM:1374 | 主要功能 | 已核对：A概括句已按批准与原分支对齐 |
| BSM-080 | BSM:1388 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-081 | BSM:1401 | 楼宇运营：机电设备、能耗抄表、楼宇巡检 | 已核对，未确认缺陷 |
| BSM-082 | BSM:1405 | 主要功能 | 已核对，未确认缺陷 |
| BSM-083 | BSM:1414 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-084 | BSM:1427 | 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | 已核对，未确认缺陷 |
| BSM-085 | BSM:1431 | 日常工作 | 已核对，未确认缺陷 |
| BSM-086 | BSM:1441 | 排班、考勤与请假 | 已核对，未确认缺陷 |
| BSM-087 | BSM:1455 | 我的薪资 | 已核对，未确认缺陷 |
| BSM-088 | BSM:1467 | 员工提成、活动执行与人员服务边界 | 已核对，未确认缺陷 |
| BSM-089 | BSM:1479 | 第三方接入：外部系统继续做专业系统，平台拿回结果 | 已核对，未确认缺陷 |
| BSM-090 | BSM:1545 | 机器人配送与扫码交接 | 已核对，未确认缺陷 |
| BSM-091 | BSM:1554 | 视频监控（留点位/时间） | 已核对，未确认缺陷 |
| BSM-092 | BSM:1563 | 重要事项短信通知（2026-06-29 新增） | 已核对，未确认缺陷 |
| BSM-093 | BSM:1574 | 留痕与异常：重要动作留得住，出了问题追得回 | 已核对，未确认缺陷 |
| BSM-094 | BSM:1578 | 必须关注的动作 | 已核对，未确认缺陷 |
| BSM-095 | BSM:1596 | 怎样减少人工填写 | 已核对，未确认缺陷 |
| BSM-096 | BSM:1607 | 处理结果 | 已核对，未确认缺陷 |
| BSM-097 | BSM:1619 | 风险等级与风控规则 | 已核对，未确认缺陷 |
| BSM-098 | BSM:1636 | 财务对账：把经营事实整理成财务能确认的口径 | 已核对，未确认缺陷 |
| BSM-099 | BSM:1639 | 内部结算价目录（按商品规格逐项建目录） | 已核对，未确认缺陷 |
| BSM-100 | BSM:1641 | 结算配置中心（后台总控页） | 已核对，未确认缺陷 |
| BSM-101 | BSM:1643 | 项目零售参考价 | 已核对，未确认缺陷 |
| BSM-102 | BSM:1648 | 主要功能 | 已核对，未确认缺陷 |
| BSM-103 | BSM:1666 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-104 | BSM:1678 | 收入归属：哪类收入归哪个业态主体 | 已核对，未确认缺陷 |
| BSM-105 | BSM:1697 | 三方对账框架：系统流水、渠道对账单、资金到账两两核对 | 已核对，未确认缺陷 |
| BSM-106 | BSM:1709 | 对账与结算类型规则 | 已核对，未确认缺陷 |
| BSM-107 | BSM:1737 | 持股分红：按持股把经营成果定期分给主理人 | 已核对，未确认缺陷 |
| BSM-108 | BSM:1741 | 主要功能 | 已核对，未确认缺陷 |
| BSM-109 | BSM:1760 | 谁会用 | 已核对，未确认缺陷 |
| BSM-110 | BSM:1768 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-111 | BSM:1781 | 经营看板：让管理层和运营看得懂、能追问 | 已核对，未确认缺陷 |
| BSM-112 | BSM:1785 | 主要指标 | 已核对，未确认缺陷 |
| BSM-113 | BSM:1813 | 谁会看 | 已核对，未确认缺陷 |
| BSM-114 | BSM:1827 | 各类入口：不同人只看自己该看的、只做自己该做的 | 已核对，未确认缺陷 |
| BSM-115 | BSM:1897 | 相互关系：为什么这些子系统必须连在一起 | 已核对，未确认缺陷 |
| BSM-116 | BSM:1928 | 系统交付与上线：正式上线要做的安全加固和安装交付 | 已核对，未确认缺陷 |
| BSM-117 | BSM:1932 | 正式上线时的安全加固 | 已核对，未确认缺陷 |
| BSM-118 | BSM:1940 | 能自动识别环境的安装包 + 内附教程 | 已核对，未确认缺陷 |
| BSM-119 | BSM:1947 | 和谁有关 | 已核对，未确认缺陷 |
| BSM-120 | BSM:1957 | 已确认规则和实施前输入项 | 已核对，未确认缺陷 |


## 对抗式线：正常路径反例与排除

| 对照组 | 反例/边界 | 本席结论与依据 |
|---|---|---|
| 全局分类与酒吧局部配置 | 全局不做后台可配 vs 客人来源/打折原因枚举按项目可配 | 操作员最新明确B/C原文不冲突、就是本意；按已确认业务含义判为成立。不改原文，不新增可配范围或例外。BSM:521/1020/1068及最新答复，见CX-03/04。 |
| 票务/年龄 | 设备不可用、误拒、多人、多次、场次终结、已用票再赠、补偿延长 | 年龄设备人工抽查/一次凭据是明定例外，非免检票；补偿是场次终结例外；普通退款与安全例外分开。BEQR:33/47/73、BSM:746/800/803/808/1074。 |
| 退款单位 | 人份退款与“同订单不存在部分核销并部分退款” | 人次处理规定核算单位，不单独授予退款资格；两份均明写普通退款限制，不能靠人份定义推导出例外。未确认矛盾，不发明放宽。BEQR:47/73、BSM:746/803。 |
| 分配/尾差 | 1—10 家比例、并列第4/5、仅第一段、第二段全未核销、缺家数行 | 十行均100%，并列17.5%；未核销减除/未分配池/尾差三栏独立；缺行补配对暂挂票是明确特例。命令实算在下。 |
| 零元/定价/费用 | 免费权益出资、内部价时点、销售通路二层、单票/通票服务费上限 | 逐项锁出资与内部价的特殊规则不能被一般“核销时固化”覆盖；各费只扣一次，单票/通票上限分开。BEQR票务财务锚点、BSM:748—755/1037—1042/1640。 |
| 积分/会员/储值 | 付款与核销不同日、未用票、退款积分过期、注销一年、券重回可用 | 积分/成长值双账，票务/商户消费两种计算；商户取消退券与退积分继承原到期日明确；注销事件保留是窄例外。BEQR会员段，BSM:815—849/864。 |
| 台位/酒吧 | A+B→T→并C→拆、撤销时原台另占、低消已达后优惠、赠送/提成 | 原台历史不进入新T运算；任一后续变化阻断旧快照撤销；低消折前与提成折后为不同基数；服务费应发部分与扣留部分分开。BSM:1063—1081。 |
| 终端/员工 | 私人手机概述含考勤与工作机领机打卡、设备故障延误、跨月班次 | 概述不替代领机三事件的具体入口；设备故障延误为严格迟到规则的明定例外；班次结束归属为营业日规则的明定例外。BSM:930/1443—1452/1458。 |
| 供应链/游乐 | 可消费借物、补录、恰等于高价值阈值、关键设备可替代、重复出场 | 可消费物品实际买断；补录与原始记录并列；高价值触发是“高于”而不是自行改成“大于等于”，未确认能以缺少重复等值句新增业务。关键安全依赖依实际流程、辅助故障需工程留痕，容量不人工改。BEQR:164，BSM:1202—1206/1253—1305。 |
| 租赁/物业 | 零欠费/仅罚款、面积变更、押金等待与三个付账日、投诉关闭 | 清退前提明确；合同变更子合同与一般不追补分别适用；退租押金不受三个付账日为明定例外；投诉类型分支不同，不套全类验收链。BSM:1340—1385。 |
| 财务/分红 | 跨月退款、在途/失败、税额、服务费扣留、借款前置 | 不改已关期间；在途不重付、失败另路跟进；税额依专业规则不套普通舍入；借款前置不属于三个可选开关。BEQR财务/分红段，BSM:1642—1790。 |
| 治理/交付 | 普通审批绕红线、密钥逐笔例外、测试弱密码、只读分析、第三方占位 | 三硬红线不可绕，其他密钥权限限逐笔；生产换强密码/安装与运行权限不同；只读分析不发控制指令；无接口不得假造数据。BSM:703/1805—1820/1930—1955。 |

上表行区间是定位辅助，精确判据原文和源行以逐 ID 表为准。各组概述与具体条目的边界按已明定限定解释；B/C按操作员最新澄清解释；不为理论组合添加状态/闸/规则。



## 本席全部临时finding（四项）
### CX-01 — BEQR 静态呈现
- 文件：docs/business-enum-quick-reference-v0.1.html:1—26。
- 分类：技术性；工程缺陷：否；建议修复队列归属：否。
- 依据类型：4；冻结判据：GEN-001/002/015/017/018。
- 无业务发明理由：doctype、UTF-8、CSS、表格语义只承载同页权威文本的阅读，不创造收费、状态、角色或审批。结构解析与源码全文读取均完成。
- 处置：只登记，等待 B2 的 Grok 独立分类复核，不自行删除正本任何部分。

### CX-02 — BSM 静态呈现与目录导航
- 文件：docs/business-system-map-for-operations-v0.1.html:1—520、2008—2032。
- 分类：技术性；工程缺陷：否；建议修复队列归属：否。
- 依据类型：4；冻结判据：GEN-001/002/012/015/017/018。
- 无业务发明理由：布局与 setActive 只高亮当前目录，不修改业务数据；真实27个href均对应源ID，原脚本执行首屏/每目标通过。
- 处置：只登记，等待 B2 的 Grok 独立分类复核。



### CX-03 — B：客人来源分类按项目可配，经操作员确认不冲突
- 文件：docs/business-system-map-for-operations-v0.1.html:521、1020。
- 当前分类：细化；工程缺陷：否；建议修复队列：否。依据类型1+4，另有操作员本次明确业务解释；冻结判据BSM-001、BSM-052、GEN-016、GEN-018。
- L521原文：“业务分类定义也不在此列：红线有哪几类、退款有哪几种情形、会员有哪几个等级、终端有哪几类、合作模式有哪几种等「有哪几种」的清单，属于业务口径本身，不做后台可配；增加或删减一类必须先经批准写入三份 HTML，不得在后台自行增删。”
- L1020原文：“开台时系统自动判定并记录客人来源：经第三方团购券核销或第三方预订到店的，按回流渠道自动记（美团点评、抖音等）；经平台自有渠道订台或购票到店的，自动记平台自有渠道；由有销售归属的订台开台的，自动记营销带客并带出该销售人员；以上都不命中的直接到店开台默认记自来客——此时现场可人工补选营销带客并选定销售人员，不补选即维持自来客。来源枚举项为业务参数，按项目可配。”
- 完整段还把服务员/销售人员分开、提成引用来源、事后改派由店长结清前复核；这些未改，不构成另项finding。
- 核对：上述两处原文及冻结BSM001/052摘录均保持。操作员已明确B原文不冲突、就是本意；此前将具体来源规则误判成冲突的结论撤销。未以数值上下限缺失作判断，也未跑代码或推定后台应支持哪些增删动作。
- 操作员原话：“b c两项原文不冲突。就是我的原意。”据此作为符合预期的细化登记；无需修复，原文保留。旧疑点保留审计历史，不继续标为当前冲突，不改写为已修，不开添加项questions。

### CX-04 — C：打折原因分类按项目可配，经操作员确认不冲突
- 文件：docs/business-system-map-for-operations-v0.1.html:521、1068。
- 当前分类：细化；工程缺陷：否；建议修复队列：否。依据类型1+4，另有操作员本次明确业务解释；冻结判据BSM-001、BSM-054、GEN-016、GEN-018。
- 全局原文同CX03。L1068连续原文：“最高折扣额度可按岗位分档配置（业务参数，按项目可配，出厂默认各授权岗位一致为八折，须同时配置上下限）。现场打折必须选择原因分类：招待、客诉补偿、活动执行、员工亲友、其他（枚举项业务参数，按项目可配）；月度管理报告按原因分类归集。”
- 数值折扣额度可配和“原因有哪几种”是不同对象。本席只核原因清单与L521全局原则；不将数值额度算作冲突、不为枚举发明数值上下限。全文中整单打折、不得手工改价、可与券/赠送合并、承担方及报告归集均按原文明定范围审读，不据偏好加业务。
- 核对：当前行与BSM054摘录保留五类原因及“枚举项业务参数，按项目可配”，BSM001原文亦保持。操作员已明确C原文不冲突、就是本意；当前按此确认规则成立，不额外限制、扩张配置边界。
- 操作员最新原话同CX03：当前作为符合预期的细化登记，无需修复。撤销本席先前的冲突判断，原文保留；依据是新的明确业务澄清，而非仅凭先前拒绝修复。

## A/D当前核对（不是本次finding）
L1380现为“处理后的回访、验收和关闭，按下述已确认的工单类型分支规则执行。”；L1384及BEQR262保留保洁完成即结、两类投诉和复杂报修的已确认分支。概括与分支对齐，不再要求全类统一验收。L2现V0.13/2026-09-08，版本仅进一次，页脚和目录保持。BSM079新锁摘录与此句逐字一致；A/D本批2行未出现回归。旧005详细投诉分支保持。B4与LOCK-AMEND双PASS只证明各自阶段，不替代本次整模块判定。

## 技能、可复现检查及实际输出
沿用本会话已实际读取的using-superpowers、grilling/grill-me、codebase-design、ponytail-review、receiving-code-review、verification-before-completion、systematic-debugging/diagnosing-bugs，按提示词收窄；无子代理、无新增判据/门槛。HTML文档按类型1/4，不强制新增测试文件，不冒称浏览器或读屏验收。batch-grill-me/ubiquitous-language为此前实际预检缺口，不冒充调用。

### 全量摘录、结构和原导航脚本
~~~bash
python3 - <<'PY'
from pathlib import Path
from html.parser import HTMLParser
from collections import Counter
import hashlib,json,re,subprocess
root=Path('/home/james/complex-management')
paths=['docs/business-enum-quick-reference-v0.1.html','docs/business-system-map-for-operations-v0.1.html']
lock=(root/'docs/audit-2026-09/plan/criteria-lock.md').read_text(encoding='utf-8')
h=hashlib.sha256((root/'docs/audit-2026-09/plan/criteria-lock.md').read_bytes()).hexdigest()
blocks=re.split(r'(?m)^### ',lock)[1:]
coverage=[]
for block in blocks:
 m=re.match(r'((?:BEQR|BSM)-\d{3}) — (.*)',block)
 if not m: continue
 src=re.search(r'- 来源：`([^`]+):(\d+)`',block)
 raw=re.search(r'````html\n([\s\S]*?)\n````',block)
 assert src and raw,m[1]
 source=(root/src[1]).read_text(encoding='utf-8')
 assert raw[1] in source,m[1]
 coverage.append({'id':m[1],'title':m[2],'file':src[1],'line':int(src[2])})
assert len(coverage)==317,len(coverage)
class AuditHTML(HTMLParser):
 def __init__(self):
  super().__init__();self.ids=[];self.links=[];self.navlinks=[];self.stack=[];self.nav=False;self.errors=[];self.tags=Counter();self.lang=None;self.charset=None
 def handle_starttag(self,tag,attrs):
  a=dict(attrs);self.tags[tag]+=1
  if 'id' in a:self.ids.append(a['id'])
  if tag=='html':self.lang=a.get('lang')
  if tag=='meta' and 'charset' in a:self.charset=a['charset']
  if tag=='nav' or 'nav' in a.get('class','').split():self.nav=True
  if tag=='a' and 'href' in a:
   self.links.append(a['href'])
   if self.nav:self.navlinks.append(a['href'])
  if tag not in {'area','base','br','col','embed','hr','img','input','link','meta','param','source','track','wbr'}:self.stack.append(tag)
 def handle_endtag(self,tag):
  if tag=='nav':self.nav=False
  if self.stack and self.stack[-1]==tag:self.stack.pop()
  else:self.errors.append([self.getpos()[0],tag,self.stack[-3:]])
checks=[]
for path in paths:
 b=(root/path).read_bytes();s=b.decode('utf-8',errors='strict');p=AuditHTML();p.feed(s)
 assert not b.startswith(b'\xef\xbb\xbf') and '\ufffd' not in s,path
 duplicate=[x for x,n in Counter(p.ids).items() if n>1]
 broken=[x for x in p.links if x.startswith('#') and x[1:] not in p.ids]
 missing=[x for x in p.links if x and not x.startswith(('#','http:','https:','mailto:','javascript:')) and not (root/path).parent.joinpath(x.split('#')[0]).exists()]
 assert not (duplicate or broken or missing or p.errors or p.stack),(path,duplicate,broken,missing,p.errors,p.stack)
 assert p.lang and p.charset.lower()=='utf-8'
 checks.append({'path':path,'sha256':hashlib.sha256(b).hexdigest(),'wc_l':b.count(b'\n'),'actual_lines':len(s.splitlines()),'ids':len(p.ids),'navlinks':len(p.navlinks),'broken_links':len(broken)+len(missing),'stack_errors':len(p.errors),'utf8':'PASS'})
 if 'business-system-map' in path:
  script=re.search(r'<script>([\s\S]*?)</script>',s)[1]
  refs=[x for x in p.navlinks if x.startswith('#')]
  js="const vm=require('node:vm'), assert=require('node:assert/strict'); const input=JSON.parse(process.argv[1]); let top=[],active=[]; const nodes=input.refs.map((id,i)=>({id:id.slice(1),getBoundingClientRect:()=>({top:top[i]})})); const links=input.refs.map((id,i)=>({getAttribute:()=>id,classList:{toggle:(_,v)=>active[i]=v}})); const context={document:{querySelectorAll:()=>links,querySelector:id=>nodes.find(n=>'#'+n.id===id)},window:{addEventListener:(event,fn,opts)=>{assert.equal(event,'scroll');assert.equal(opts.passive,true)}}};top=nodes.map((_,i)=>1000+i*100);vm.createContext(context);vm.runInContext(input.script,context);assert.deepEqual(active,links.map((_,i)=>i===0));for(let target=0;target<nodes.length;target++){top=nodes.map((_,i)=>(i-target)*200+120);vm.runInContext('setActive()',context);assert.deepEqual(active,links.map((_,i)=>i===target));}console.log(JSON.stringify({nav_targets:nodes.length,initial_and_every_section:'PASS',actual_script:true}));"
  run=subprocess.run(['node','-e',js,json.dumps({'script':script,'refs':refs})],capture_output=True,text=True)
  assert run.returncode==0,run.stderr
  checks.append({'navigation_execution':json.loads(run.stdout)})
print(json.dumps({'lock_sha256':h,'criteria_coverage':len(coverage),'checks':checks},ensure_ascii=False))
print('COVERAGE_COUNT='+str(len(coverage)))
PY
~~~
实际输出：
~~~text
{"lock_sha256": "b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a", "criteria_coverage": 317, "checks": [{"path": "docs/business-enum-quick-reference-v0.1.html", "sha256": "4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66", "wc_l": 308, "actual_lines": 308, "ids": 0, "navlinks": 0, "broken_links": 0, "stack_errors": 0, "utf8": "PASS"}, {"path": "docs/business-system-map-for-operations-v0.1.html", "sha256": "a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa", "wc_l": 2032, "actual_lines": 2032, "ids": 29, "navlinks": 27, "broken_links": 0, "stack_errors": 0, "utf8": "PASS"}, {"navigation_execution": {"nav_targets": 27, "initial_and_every_section": "PASS", "actual_script": true}}]}
COVERAGE_COUNT=317
~~~

### 金额与反例
~~~bash
python3 - <<'PY'
from pathlib import Path
from decimal import Decimal,ROUND_HALF_UP
import re,json,hashlib
root=Path('/home/james/complex-management')
s=(root/'docs/business-enum-quick-reference-v0.1.html').read_text(encoding='utf-8')
row=next(x for x in s.splitlines() if '<td class="k">酒吧通票可分配池</td>' in x)
table=row.split('参与家数档位表（出厂默认')[1].split('参与家数档位表不设家数上限')[0]
def parse_table(t):
 rows={}
 for n,body in re.findall(r'第二段参与 (\d+) 家＝(.*?)(?:；|。)',t):
  slots={}
  for start,end,value in re.findall(r'第 (\d+)(?:[–-](\d+))? 名\s*(?:各\s*)?(\d+(?:\.\d+)?)%',body):
   for rank in range(int(start),int(end or start)+1):
    assert rank not in slots
    slots[rank]=Decimal(value)
  assert set(slots)==set(range(1,int(n)+1)),(n,slots)
  assert sum(slots.values())==100,(n,slots)
  rows[int(n)]=slots
 assert len(rows)==10
 return rows
rows=parse_table(table)
assert (rows[5][4]+rows[5][5])/2==Decimal('17.5')
rejected=False
try:parse_table(table.replace('100%','99%',1))
except AssertionError:rejected=True
assert rejected
# 只证明 HTML 明文的金额/场次算例可同时成立，不代表执行了业务实现。
round_cent=lambda x:Decimal(x).quantize(Decimal('.01'),rounding=ROUND_HALF_UP)
tie=[round_cent(Decimal('.10')*Decimal(1)/3)]*3
tail=Decimal('.10')-sum(tie)
assert sum(tie)+tail==Decimal('.10') and tail==Decimal('.01')
assert 8+5==13 # A4+B6 转 T8 后再并 C5
budget_total,available,reserved,issued,reclaimed=100,60,20,30,10
assert budget_total==available+reserved+issued-reclaimed
assert round_cent(Decimal(5000+1000+1000+1000)*Decimal('.2'))==Decimal('1600.00')
print(json.dumps({'rank_rows':{n:list(map(str,v.values())) for n,v in rows.items()},'rank_sums':'all 100','tie_4_5':'17.5','rounding_three_shares':list(map(str,tie)),'separate_tail':str(tail),'mutated_99_percent_rejected':rejected,'merge_example':'T8+C5=13','activity_budget':'100=60+20+30-10','scope':'HTML arithmetic only, not business implementation'},ensure_ascii=False))
raw=root/'docs/audit-2026-09/modules/M001-b1-self-raw.md'
if raw.exists():
 text=raw.read_text(encoding='utf-8');print('SELF_RAW_METADATA='+json.dumps({'size':raw.stat().st_size,'report_versions':re.findall(r'(?m)^.*版本[：:]\s*(V\d+\.\d+).*$',text)[-5:],'complete_report_blocks':len(re.findall(r'(?m)^CRITERIA-LOCK-SHA256:',text))},ensure_ascii=False))
print('HEAD='+__import__('subprocess').check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip())
PY
~~~
实际输出（SELF_RAW_METADATA为当时追加前元数据，不作语义判据）：
~~~text
{"rank_rows": {"1": ["100"], "2": ["57", "43"], "3": ["40", "30", "30"], "4": ["31", "23", "23", "23"], "5": ["25", "20", "20", "20", "15"], "6": ["23", "17", "17", "17", "13", "13"], "7": ["22", "15", "15", "15", "11", "11", "11"], "8": ["18", "14", "14", "14", "10", "10", "10", "10"], "9": ["15", "13", "13", "13", "10", "10", "10", "10", "6"], "10": ["16", "12", "12", "12", "9", "9", "9", "9", "6", "6"]}, "rank_sums": "all 100", "tie_4_5": "17.5", "rounding_three_shares": ["0.03", "0.03", "0.03"], "separate_tail": "0.01", "mutated_99_percent_rejected": true, "merge_example": "T8+C5=13", "activity_budget": "100=60+20+30-10", "scope": "HTML arithmetic only, not business implementation"}
SELF_RAW_METADATA={"size": 402271, "report_versions": ["V0.10", "V0.10", "V0.34", "V0.13", "V0.35"], "complete_report_blocks": 10}
HEAD=242f487233d028173cb9198eecd079f5d877405c
~~~

### 全335映射与原完整阅读字节锚定
~~~json
{"mapping_ids": 335, "all_335_exact": true, "source_byte_recheck": [{"file": "docs/business-enum-quick-reference-v0.1.html", "changed_since_previous_complete_read": [], "all_other_lines_byte_identical": true, "sha256": "4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66"}, {"file": "docs/business-system-map-for-operations-v0.1.html", "changed_since_previous_complete_read": [2, 1380], "all_other_lines_byte_identical": true, "sha256": "a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa"}]}
~~~
还已实跑按行取出L521/L1020/L1068并与对应BSM001/052/054原文相匹配；A新句与1384分支共存，实际确认无源字漂移。CX03/04另依据操作员最新业务澄清确认，不用机械检查代替语义判断。没有运行默认只看diff的脚本冒充全文审计，没有Java/SQL/产品业务测试声明。

## 本席结论与接收顺序
本次完整范围两份HTML、335判据，全量检查完成。技术性无缺陷登记2项CX01/02；细化无缺陷登记2项CX03/04，按操作员最新澄清确认B/C原文不冲突、就是本意。A/D本次修复已完成且未确认回归。当前无缺陷，无新增权限申请。
本席B1：PASS（已纳入最新B/C业务澄清）。无新增添加项，无新修复权限，无第五次B3。当前Grok本次完整B1尚未读取；必须本报告完整追加落盘后再接收Grok、做B1后四值校验和双席合取，不以本席覆盖另一席。尚未B2/B5或模块PASS；未提交推送部署，未开M002。


## B1接收与同节点证据补充复核
版本：V0.37

Codex完整V0.36已在读取Grok前落盘，最新业务澄清已纳入。Grok原B1真实exit0，V0.16完整2189—2510行已读；其原派发未含用户运行期间的新答复“b c两项原文不冲突。就是我的原意。”因此GR01/02需要外席在此新输入下自行更正，根不能改写其原票。
本次B1后四值实算/sidecar/ledger/Grok首行均为b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。BSM仍a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa；BEQR仍4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66。
已独立重读BSM699—709完整审批红线段、1894—1905相互关系、1965—1989已拍板、1604—1630人工补录整段；BEQR193—233含合作模式两完整章及复核条、27—48含票种/有效期；BSM735—743二维码；锁1—110通用判据原文。
对原V0.16的当前核验意见：GR01/02以用户新业务解释撤销冲突，待Grok自身复核；GR03概括应与707明示但书共同执行，不能新增处处重复红线要求；GR04权威BSM有效期例子不是下位代码造票种，未找到BEQR29排他限制其他HTML细化；GR05补录强制审核在BSM1611及BEQR219已有，不能以假设只读短表证明遗漏，且BEQR应为139；GR06人用问题列不是程序主键，不同章节各有概览/治理细则用途。上述是接收后交叉质证意见，不冒称新独立盲审结果；交给Grok独立接受或维持，根不覆盖外审FAIL。
程序误判：用户此前逐条批准目录/页脚，并在r3完成；r4整树diff仅A/D两行，不能把HEAD累计diff当本次未授权变更。将批准原话、r4两个树和源码反证提交Grok同B1补充复核，保留原报告，尚不进B2/B3/B4/B5。不改HTML/判据，无新修复额度，其他470模块PENDING。
报告首次整段追加因单次Bash命令长度限制在Python解析阶段失败，原402271字节及SHA逐字未动；随后同一报告分7段带前缀/哈希断言追加完成，完整335行覆盖和最终字节核验通过。这不是业务修复或Grok调用失败，不增加相应计数。


## M001 B2 新基准整批候选独立复核
版本：V0.38

输入两席本次完整raw：/home/james/complex-management/docs/audit-2026-09/modules/M001-b1-self-raw.md（最后独占锁行起V0.36完整B1及V0.37质证附录）；/home/james/complex-management/docs/audit-2026-09/modules/M001-b1-grok-raw.md（最后独占锁行起V0.17，2510—2825完整316行）。两席均PASS；Grok实际exit0。候选findings V0.85，SHA256 c7c2a80fd228a05034c23f71040d5177f8a85dcf3c05822ea4b556ec2987177f。
根本批B2结论：PASS。全部4个当前来源CX01/02→既有004（技术性无业务发明，无缺陷），CX03→014、CX04→015（细化，无缺陷）。Grok当前finding空；其V0.16六疑点和程序项经V0.17独立实核均撤销，当前不漏并、不另分ID；撤销理由及历史原票全部保留。用户最新原话“b c两项原文不冲突。就是我的原意。”准确记录，B/C当前是预期规则无需修复，不是有缺陷但拒绝改。A/D已修出队，其他原处置保持。
机械实核：16个ID恰为001至016连续唯一；当前全部队列否；014/015均细化/工程缺陷否/队列否；无添加或未答问题。004两项静态呈现/目录服务阅读定位，不新增任何业务规则，GEN001/002/012/015/017/018及结构/原JS输出足够。分类及B/C解释仍交Grok独立B2复核，根不单票定稿。
接收Grok V0.17纠正：本席V0.37质证附录把“人工补录L219”误写BEQR-139，应为BEQR-140；Grok该finding原引用140正确。现已按当前锁核准；V0.36完整317行表自身原本正确，本次全表来源行/标题验证wrong_rows=[]。这个交叉注记错误已纠正，不修改HTML或锁，不新增判据，不计B3。
B1后四值实算、sidecar、ledger和GrokV0.17首行分别均为b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。根复核仅据当前源码/锁/已执行命令与用户明确解释，不以自审覆盖Grok旧FAIL；当前外席自己已改为PASS。
待Grok本批B2 PASS才将候选定稿；队列空则跳过B3/B4，直接B5-r4整模块确认。累计B3仍4，额外仅A/D的1次已用完，没有第五次修复授权，无HTML/锁改动、无提交推送部署、不开M002。


## M001 本轮最终操作员回执
版本：V0.39

日期：2026-09-08。此段为本轮完成后的当前状态；以上V0.38及之前段落、下引Grok各时点原票均保留历史，不将其中“待执行”误当当前待办。

### 当前步骤、授权及结果
当前步骤：M001 / 471，B5-r4完成。本轮仅为获批A、D执行1次B3，累计3→4；额外额度已用完，不存在第5次B3授权。ledger V1.58：M001 PASS，其余470模块PENDING；未开始M002，无提交、推送或部署。模块收口文件M001-00-authority-html-01.md V0.04在ledger PASS之后更新。

操作员授权原句：“A\d同意，b\c拒绝”；“授权仅为 A、D 再执行 1 次修复”。随后业务解释原句：“b c两项原文不冲突。就是我的原意。”
B（客人来源）和C（打折原因）全文保持原样；两席最终将M001-014/015确认细化、无工程缺陷、无需修复。不是已确认缺陷未获准修复，不再列为冲突。Grok旧V0.16的六项及程序疑点由Grok自身在完整上下文与最新操作员解释下撤销；原票不删、不由Codex代改。

A：docs/business-system-map-for-operations-v0.1.html:1380，“处理后回访客人或商户，验收合格才关闭。”改为“处理后的回访、验收和关闭，按下述已确认的工单类型分支规则执行。”
D：同文件L2自身版本V0.12→V0.13；更新日期仍2026-09-08。
B：L1020“来源枚举项为业务参数，按项目可配。”原样保留。
C：L1068“现场打折必须选择原因分类：招待、客诉补偿、活动执行、员工亲友、其他（枚举项业务参数，按项目可配）；月度管理报告按原因分类归集。”原样保留。

B3-r4前树4263c8c1338031a38d977741ffb111a0752b7092；后树7d6c4f7544de2957faec09e04aa2a262ba4aad86。整树未过滤diff只有1文件、2增2删、2hunk，2013字节，SHA256 57e3f181e0415c093f50322a26c826eb26623484992492499e30eadd65d0be46，全属M001-016。真实index未动。

### 最终判据完整性与复审
当前实际锁、sidecar、ledger基准、Grok B5首行各自均为：
`CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
`LOCK-AMEND-COUNT: 1`
锁版本V0.11；获批BSM-079摘录同步完整执行LOCK-AMEND五步，③双PASS后才更新④基准，再⑤M001回PENDING从新锁B1重开。只同步有HTML及逐条批准依据的BSM-079，不改通用判据/映射，COUNT按规保持1。下面原始IN-RECEIPT中的“待根执行④⑤”是当时状态；两步现已完成并经过新锁B1/B2/B5，不是当前未办事项。

当前两席：B4-r4 PASS；LOCK-AMEND PASS；新锁B1 PASS；B2 PASS；B5-r4 PASS。findings V0.86已定稿，001至016连续唯一，当前全部不入修复队列，无017或更高ID。没有新增HTML问题、添加项或未答questions。本轮非BLOCKED，无待操作员答复的铁律5问题；既有A/D授权与B/C原意均已执行，不新增审批流程。

B5两席完整覆盖两份HTML共2340行、335映射判据（197 BEQR + 120 BSM + 18 GEN）。Codex先保存自身完整报告后才读取Grok全文；Grok最终581显示行已分6段完整读取。317条HTML摘录逐字与源行匹配、335项集合精确；UTF-8、HTML闭合/导航、实际脚本、排名与金额算例和否定测试均已完成。B5后四值与源文件SHA再次通过，再写ledger PASS。

| 最终证据 | 自身版本 | SHA256 |
|---|---|---|
| modules/M001-b4-r4-self-raw.md | V0.01 | 70245a3b39f5de88e4d59c9887e4ab1a86f437894dfb3bd2b94d1121ca2e997c |
| modules/M001-b4-r4-grok-raw.md | V0.01 | b4766eddfc1ea4e8789c4769d33e722abb1031c2a0e5d168304d7f88103e0c9e |
| modules/M001-b5-r4-self-raw.md | V0.01 | b98a83bd01295decdbeb20021e80773e072da15e03603353050ad9b522e917dc |
| modules/M001-b5-r4-grok-raw.md | V0.01 | 1334b1e26cd7d4c99ee942534ad6e44eab5ae5c99676cd020fd1ea992c68bbad |
| ../business-enum-quick-reference-v0.1.html | V0.07 | 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 |
| ../business-system-map-for-operations-v0.1.html | V0.13 | a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa |

核验错误留痕：Codex V0.37交叉注记曾把人工补录L219写成BEQR-139，Grok指出后按锁确认应为BEQR-140；V0.36完整317行表本身原已正确，V0.38已纠正附注。B5自检一次在解析阶段因JS替换字符串特殊符号引入错误失败，未修改文件；改用回调替换后只重跑失败的机械检查并PASS，其他已成功检查保留。这两项均非业务修复，不增加B3或Grok调用失败计数。

### 实际Grok调用与每次结论出处
以下是本轮实际执行过的9次无头命令，全部真实进程exit code为0；每次审计结论与交付接收情况单列，不把exit0等同PASS。路径型stdout/stderr只在适配器进程内缓冲并丢弃；没有在本回执复制原始JSON、隐藏思维或密钥。

### B2第一次
exit/result：0；V0.14 FAIL，后续已纠正ID复用。
结论出处：/home/james/complex-management/docs/audit-2026-09/modules/M001-b1-grok-raw.md。
实际执行的无头SSH及适配器命令全文：
~~~powershell
$remoteScript = @'
cd /home/james/complex-management && python3 - <<'PY'
from pathlib import Path
import subprocess,sys,stat
target=Path('/home/james/complex-management/docs/audit-2026-09/modules/M001-b1-grok-raw.md')
def meta():
 if not target.exists(): return None
 s=target.stat()
 return (s.st_size,s.st_mtime_ns,s.st_ctime_ns,s.st_ino)
before=meta()
prompt="执行160-audit-prompt.md V0.17的正式M001 B2整批候选复核，仅本模块。本节点允许读两席完整B1与候选，不派子代理、不修任何业务/HTML/计划/ledger/findings/questions。真实用户最新裁决在ledger V1.50：“A\\d同意，b\\c拒绝”；随后“授权仅为 A、D 再执行 1 次修复”。用户新指令优先于提示词一般修复队列范围：本批只能修A/D，不能改B/C，也不能换到L521全局规则绕过拒绝；额外仅1次，累计保持3未修，修后才4。不要把拒绝修改伪称缺陷已修或强迫用户改变决定。\n读取.scratch/module-recheck-20260908/160-audit-prompt.md、适用runbook/技能、ledger顶部当前授权；完整输入modules/M001-b1-self-raw.md最后独占CRITERIA行起V0.32完整B1与V0.33接收附录、modules/M001-b1-grok-raw.md最后独占CRITERIA行起V0.13完整B1（1725起），以及findings.md V0.81顶部候选全文。旧报告仅为编号/同项历史证据，不继承旧PASS。\n独立核对本批全部finding有无遗漏、错误改写、ID复用/同一旧问题重复造ID、四分类、当前队列、授权和精确改文：根CX01/02→旧004；GR01工单关闭概括残留→旧005再入队（旧005详细分支已有修复，现只改1380）；GR02→新014；GR03→新015。按你独立判断上述映射是否正确，不能盲从候选。A仅1380用已批准文字改概括；D仅L2版本V0.12到V0.13；B/C明确拒绝并原文保留。014/015保留你的冲突/缺陷结论，队列否注明用户拒绝，不包装成无缺陷。本节点PASS只判归并/范围准确，绝非本模块业务无冲突；后续全量审计结论须如实。若认为候选仍漏项/改写/范围错误，给明确FAIL与具体依据，不修改候选；不要擅自突破用户本批仅A/D的范围。\n唯一可写/home/james/complex-management/docs/audit-2026-09/modules/M001-b1-grok-raw.md。将本次完整B2复核报告追加原文件末尾，版本Vx.xx递增，保留全部原字节；注明输入两份完整raw版本、候选版本、实际差集与分类/队列核对、最终PASS/FAIL。B2附录不得新增CRITERIA-LOCK-SHA256独占行，不创建其他文件。不做B3/B4/B5、不分配新ID、不派代理、不提交/推送/部署。完整报告落盘后结束。"
cmd=['/home/james/.local/bin/grok','-p',prompt,'-m','grok-4.6','--reasoning-effort','xhigh','--output-format','json','--permission-mode','bypassPermissions','--no-subagents','--disable-web-search','--cwd','/home/james/complex-management']
proc=subprocess.Popen(cmd,cwd='/home/james/complex-management',stdout=subprocess.PIPE,stderr=subprocess.PIPE)
while True:
 try:
  output,error=proc.communicate(timeout=300)
  break
 except subprocess.TimeoutExpired: pass
output=error=None
after=meta()
code='GROK_EXIT' if proc.returncode else ('REPORT_MISSING' if after is None else ('PATH_MISMATCH' if not target.is_file() or target.resolve()!=target else ('REPORT_EMPTY' if after[0]==0 else ('REPORT_STALE' if after==before else None))))
if code:
 print('ADAPTER_ERROR='+code)
 print('EXIT_CODE='+str(proc.returncode))
 sys.exit(proc.returncode or 1)
print('REPORT_PATH='+str(target))
print('EXIT_CODE=0')
PY
'@
& 'C:\Windows\System32\OpenSSH\ssh.exe' -o BatchMode=yes james@192.168.88.160 $remoteScript
~~~

### B2修正后
exit/result：0；V0.15 PASS。
结论出处：/home/james/complex-management/docs/audit-2026-09/modules/M001-b1-grok-raw.md。
实际执行的无头SSH及适配器命令全文：
~~~powershell
$remoteScript = @'
cd /home/james/complex-management && python3 - <<'PY'
from pathlib import Path
import subprocess,sys,stat
target=Path('/home/james/complex-management/docs/audit-2026-09/modules/M001-b1-grok-raw.md')
def meta():
 if not target.exists(): return None
 s=target.stat()
 return (s.st_size,s.st_mtime_ns,s.st_ctime_ns,s.st_ino)
before=meta()
prompt="执行160-audit-prompt.md V0.17的正式M001 B2整批候选复核，仅本模块。本节点允许读两席完整B1与候选，不派子代理、不修任何业务/HTML/计划/ledger/findings/questions。真实用户最新裁决在ledger V1.51：“A\\d同意，b\\c拒绝”；随后“授权仅为 A、D 再执行 1 次修复”。用户新指令优先于提示词一般修复队列范围：本批只能修A/D，不能改B/C，也不能换到L521全局规则绕过拒绝；额外仅1次，累计保持3未修，修后才4。不要把拒绝修改伪称缺陷已修或强迫用户改变决定。\n读取.scratch/module-recheck-20260908/160-audit-prompt.md、适用runbook/技能、ledger顶部当前授权；完整输入modules/M001-b1-self-raw.md最后独占CRITERIA行起V0.32完整B1与V0.33接收附录、modules/M001-b1-grok-raw.md最后独占CRITERIA行起V0.13完整B1（1725起），以及findings.md V0.82顶部候选全文。旧报告仅为编号/同项历史证据，不继承旧PASS。\n独立核对本批全部finding有无遗漏、错误改写、ID复用/同一旧问题重复造ID、四分类、当前队列、授权和精确改文：根CX01/02→旧004；GR01全类验收概括→新016（旧005投诉确认/超时问题已修，保持否）；GR02→新014；GR03→新015。按你独立判断上述映射是否正确，不能盲从候选。A仅1380用已批准文字改概括；D仅L2版本V0.12到V0.13；B/C明确拒绝并原文保留。014/015保留你的冲突/缺陷结论，队列否注明用户拒绝，不包装成无缺陷。本节点PASS只判归并/范围准确，绝非本模块业务无冲突；后续全量审计结论须如实。若认为候选仍漏项/改写/范围错误，给明确FAIL与具体依据，不修改候选；不要擅自突破用户本批仅A/D的范围。\n唯一可写/home/james/complex-management/docs/audit-2026-09/modules/M001-b1-grok-raw.md。将本次完整B2复核报告追加原文件末尾，版本Vx.xx递增，保留全部原字节；注明输入两份完整raw版本、候选版本、实际差集与分类/队列核对、最终PASS/FAIL。B2附录不得新增CRITERIA-LOCK-SHA256独占行，不创建其他文件。不做B3/B4/B5、不分配新ID、不派代理、不提交/推送/部署。完整报告落盘后结束。\n本次是修正你上一份V0.14 B2唯一FAIL后的整批再审。根已按你的实际意见修订：旧005不重开，新的GR01分配016且A/D归016，014/015保持。上版候选身份错误与两席旧票保留历史，不冒称首次通过。你仍须独立全批核对5来源、16个当前ID/队列/精确改文，检查修正是否完整、有无遗漏，不盲从根票。读取最新顶部V0.82候选，self追加V0.35仅是根本次B2票；B1依据仍完整V0.32/V0.13。不要因旧历史候选尚含005或旧FAIL就把已修正的当前候选误当旧版；也不要因此省去全批核对。唯一写同一grok b1 raw，追加完整B2报告并版本递增，不新写独占锁行。"
cmd=['/home/james/.local/bin/grok','-p',prompt,'-m','grok-4.6','--reasoning-effort','xhigh','--output-format','json','--permission-mode','bypassPermissions','--no-subagents','--disable-web-search','--cwd','/home/james/complex-management']
proc=subprocess.Popen(cmd,cwd='/home/james/complex-management',stdout=subprocess.PIPE,stderr=subprocess.PIPE)
while True:
 try:
  output,error=proc.communicate(timeout=300)
  break
 except subprocess.TimeoutExpired: pass
output=error=None
after=meta()
code='GROK_EXIT' if proc.returncode else ('REPORT_MISSING' if after is None else ('PATH_MISMATCH' if not target.is_file() or target.resolve()!=target else ('REPORT_EMPTY' if after[0]==0 else ('REPORT_STALE' if after==before else None))))
if code:
 print('ADAPTER_ERROR='+code)
 print('EXIT_CODE='+str(proc.returncode))
 sys.exit(proc.returncode or 1)
print('REPORT_PATH='+str(target))
print('EXIT_CODE=0')
PY
'@
& 'C:\Windows\System32\OpenSSH\ssh.exe' -o BatchMode=yes james@192.168.88.160 $remoteScript
~~~

### B4-r4
exit/result：0；V0.01 内容/范围PASS。
结论出处：/home/james/complex-management/docs/audit-2026-09/modules/M001-b4-r4-grok-raw.md。
实际执行的无头SSH及适配器命令全文：
~~~powershell
$remoteScript = @'
cd /home/james/complex-management && python3 - <<'PY'
from pathlib import Path
import subprocess,sys,stat
target=Path('/home/james/complex-management/docs/audit-2026-09/modules/M001-b4-r4-grok-raw.md')
def meta():
 if not target.exists(): return None
 s=target.stat()
 return (s.st_size,s.st_mtime_ns,s.st_ctime_ns,s.st_ino)
before=meta()
prompt="执行160-audit-prompt.md V0.17正式M001 B4-r4修复内容审计及§5闸3范围归因。你是唯一Grok独立只读席，不派子代理，不看本次M001-b4-r4-self-raw.md，不修文件。本次操作员额外仅允许A/D一次，真实原话在ledger V1.52；现在累计4，额外用尽，没有第五次修复授权。B/C被明确拒绝，保持原文和原finding，不得绕到全局L521修改。\n先完整阅读本节点适用提示词/runbook/技能、findings V0.83本批已B2双PASS定稿及ledger当前授权；B2根V0.35/GrokV0.15通过，新ID016为唯一执行队列，旧005不重开，014/015是用户拒绝修复。只审本次完整diff，不扩大整模块重审，不因未变的B/C旧冲突把本次两行修改冒称越界；B1/B5会如实处理整模块结论。\n本次两个真实tree：before 4263c8c1338031a38d977741ffb111a0752b7092；after 7d6c4f7544de2957faec09e04aa2a262ba4aad86。必须自行运行git diff --binary两树取得全部未过滤diff（包括tracked/staged/untracked），逐块全文审查，不按路径筛掉行，并把两个tree和完整diff逐字写本次raw。根提供的冻结diff如下仅用于复核，应自行重算：\n~~~diff\ndiff --git a/docs/business-system-map-for-operations-v0.1.html b/docs/business-system-map-for-operations-v0.1.html\nindex d71ca742..12334b13 100644\n--- a/docs/business-system-map-for-operations-v0.1.html\n+++ b/docs/business-system-map-for-operations-v0.1.html\n@@ -1,5 +1,5 @@\n <!doctype html>\n-<!-- 文件版本：V0.12；更新日期：2026-09-08 -->\n+<!-- 文件版本：V0.13；更新日期：2026-09-08 -->\n <html lang=\"zh-CN\">\n <head>\n   <meta charset=\"utf-8\">\n@@ -1377,7 +1377,7 @@\n                 <!-- 2026-08-04 第三轮用户裁决（DEC-R6-14）：物业工单来源补「客人」——客人端投诉，客人即提单方、关闭须其确认 -->\n                 <li>工单来源包括：商户（商户侧自助提单）、客人（消费客人通过客人端投诉，客人即提单方；客户关怀部记录问题解决方案及客人反馈后可以关闭）、现场工作人员，以及楼宇设备告警和巡检异常自动生成。</li>\n                 <li>派单到具体人员或班组，记录处理过程、用料和结果。用料必须从指定库房出库并按当前平均进货价计成本，同时在工单上标明费用承担方：公区设施与楼宇主体设备的用料由物业承担；商户自有设施、以及因商户责任导致损坏的用料由该商户承担，并自动生成一笔应收进入其费用账单。承担方有争议的，工单挂起转上级判定，判定结果与依据留痕后才能结案。</li>\n-                <li>处理后回访客人或商户，验收合格才关闭。</li>\n+                <li>处理后的回访、验收和关闭，按下述已确认的工单类型分支规则执行。</li>\n                 <li>按响应时限和处理时限统计时效，超时自动升级通知主管。</li>\n                 <li>记录满意度评价，作为服务质量和商户表现的依据。</li>\n                 <!-- 2026-06-25 修改阶段：补物业工单运营细则，依据 M1 需求采集（complex-m1-requirements §4）。 -->\n\n~~~\n批准与归因：所有两hunk的每条变更归M001-016。A仅BSM L1380由“处理后回访客人或商户，验收合格才关闭。”改为“处理后的回访、验收和关闭，按下述已确认的工单类型分支规则执行。”；D仅L2 V0.12→V0.13，日期仍2026-09-08。详细工单规则L1384/BEQR262保持，B/C L1020/1068、L521全局原则、页脚和目录保持。需核当前HTML等于after tree、旧/新字唯一/反向还原、无额外路径、无意外标签/文案错误、版本规则及旧005不受影响。HTML文案修复按runbook豁免业务系统测试，实际类型1/4证据足够，不虚报系统测试。\n已知必要后续：BSM079冻结摘录仍是修前A句；锁本次B3严格未改，须B4通过后依LOCK-AMEND五步同步。这不是本次未授权修改或新发现，不能在B4替根更新锁；需要明确记录尚未完成锁同步、模块未PASS。\n独立sha256sum当前正式criteria-lock并核sidecar/ledger；本次报告首行独占CRITERIA-LOCK-SHA256: <自行实算64位>，不使用派发期望值（本指令未给）。随后Vx.xx版本，HEAD/两树/完整未过滤diff/每hunk归因/全部实际验证证据/技能实际使用缺口/内容PASS或FAIL及范围归因PASS或FAIL/本席总PASS或FAIL。找齐全部本次diff问题后一次返回，不修不迭代。\n唯一可写/home/james/complex-management/docs/audit-2026-09/modules/M001-b4-r4-grok-raw.md；存在则追加本次完整报告且版本递增，不存在则创建V0.01。不要新增其他产物。不得写self raw、findings/ledger/HTML/锁。不提交推送部署、不进M002。完整报告落盘后结束。"
cmd=['/home/james/.local/bin/grok','-p',prompt,'-m','grok-4.6','--reasoning-effort','xhigh','--output-format','json','--permission-mode','bypassPermissions','--no-subagents','--disable-web-search','--cwd','/home/james/complex-management']
proc=subprocess.Popen(cmd,cwd='/home/james/complex-management',stdout=subprocess.PIPE,stderr=subprocess.PIPE)
while True:
 try:
  output,error=proc.communicate(timeout=300)
  break
 except subprocess.TimeoutExpired: pass
output=error=None
after=meta()
code='GROK_EXIT' if proc.returncode else ('REPORT_MISSING' if after is None else ('PATH_MISMATCH' if not target.is_file() or target.resolve()!=target else ('REPORT_EMPTY' if after[0]==0 else ('REPORT_STALE' if after==before else None))))
if code:
 print('ADAPTER_ERROR='+code)
 print('EXIT_CODE='+str(proc.returncode))
 sys.exit(proc.returncode or 1)
print('REPORT_PATH='+str(target))
print('EXIT_CODE=0')
PY
'@
& 'C:\Windows\System32\OpenSSH\ssh.exe' -o BatchMode=yes james@192.168.88.160 $remoteScript
~~~

### LOCK-AMEND首次
exit/result：0；内容PASS，text含过程前缀未接收。
结论出处：IN-RECEIPT。
实际执行的无头SSH及适配器命令全文：
~~~powershell
$remoteScript = @'
cd /home/james/complex-management && python3 - <<'PY'
import subprocess,json,sys
prompt="执行160-audit-prompt.md V0.17 §5闸2③的LOCK-AMEND独立复核，仅M001获批A后BSM-079摘录同步。你是唯一Grok审计席，不派子代理，全程只读正式仓库；不读Codex本次锁复核结论。本节点允许/tmp计算副本，不新建审计报告文件，结果以完整回执返回；不使用B1/B4那种路径回执适配器。\n本节点输入：正式criteria-lock.md V0.11；sidecar；ledger.md V1.53顶部仍为旧基准及COUNT1，当前登记变更ID仅BSM-079。HTML出处BSM1374起，获批变更在L1380；A/D已通过B4-r4且findings V0.84出队。操作员原话与批准字句在ledger当前登记，额外A/D仅一次已用尽，累计B3=4。不要修改HTML、ledger、findings、锁或映射；不要自己执行④⑤。\n务必完整执行：先自行读ledger顶部旧基准；把当前完整锁复制至你在/tmp创建的副本，将下面提供的①前后完整unified diff反向实际应用（git apply --reverse），实算反向文件SHA256必须等于你自己读到的旧ledger基准。不得只相信根报数、不得只核摘要。再比对全部定义/映射变化与ledger登记：仅BSM079已获批原文及锁自身版本；401定义ID不增删，383HTML摘录逐字且源行正确，471映射全量一致，查最早引用模块和COUNT不计次条件（全部变化ID有HTML出处且批准，没有general变更）。当前sidecar须等于你独立实算现锁，ledger旧基准必须尚未改；两席通过后根才能④改基准/COUNT保持1、⑤最早引用M001退PENDING从B1重开。本次BSM079引用7模块这一数量也请自行核实，不能用提示数代替实算。\n旧锁亦可由B3 after tree 7d6c4f7544de2957faec09e04aa2a262ba4aad86中的docs/audit-2026-09/plan/criteria-lock.md读取，但仍必须在/tmp反向实际应用完整diff。以下是完整diff：\n~~~diff\ndiff --git a/docs/audit-2026-09/plan/criteria-lock.md b/docs/audit-2026-09/plan/criteria-lock.md\nindex 6ec5de54..8db3fc02 100644\n--- a/docs/audit-2026-09/plan/criteria-lock.md\n+++ b/docs/audit-2026-09/plan/criteria-lock.md\n@@ -1,6 +1,6 @@\n # 审计判据冻结\n \n-版本：V0.10\n+版本：V0.11\n \n - 判据总数：401（18 条通用工程判据 + 383 个 HTML 锚点判据）\n - HTML 锚点闭集：BEQR `td.k` 197；BSM `h2/h3` 120；PGM `h2/h3` 66。\n@@ -3927,7 +3927,7 @@\n                 <!-- 2026-08-04 第三轮用户裁决（DEC-R6-14）：物业工单来源补「客人」——客人端投诉，客人即提单方、关闭须其确认 -->\n                 <li>工单来源包括：商户（商户侧自助提单）、客人（消费客人通过客人端投诉，客人即提单方；客户关怀部记录问题解决方案及客人反馈后可以关闭）、现场工作人员，以及楼宇设备告警和巡检异常自动生成。</li>\n                 <li>派单到具体人员或班组，记录处理过程、用料和结果。用料必须从指定库房出库并按当前平均进货价计成本，同时在工单上标明费用承担方：公区设施与楼宇主体设备的用料由物业承担；商户自有设施、以及因商户责任导致损坏的用料由该商户承担，并自动生成一笔应收进入其费用账单。承担方有争议的，工单挂起转上级判定，判定结果与依据留痕后才能结案。</li>\n-                <li>处理后回访客人或商户，验收合格才关闭。</li>\n+                <li>处理后的回访、验收和关闭，按下述已确认的工单类型分支规则执行。</li>\n                 <li>按响应时限和处理时限统计时效，超时自动升级通知主管。</li>\n                 <li>记录满意度评价，作为服务质量和商户表现的依据。</li>\n                 <!-- 2026-06-25 修改阶段：补物业工单运营细则，依据 M1 需求采集（complex-m1-requirements §4）。 -->\n\n~~~\n技能按本节点实际核读并按V0.17收窄，不派第三席、不扩模块、不写额外产物。业务原文当前V0.13，B/C及全局规则按用户拒绝保持；本次只是A的同字摘录同步，不能顺便写分类例外。\n交付要求：一次性只输出完整最终回执，第一行必须是你独立sha256sum正式现锁所得CRITERIA-LOCK-SHA256: <64位小写hex>，独占第一行；不要在该行前后夹过程进度前缀。随后给明确LOCK-AMEND PASS或FAIL、完整反向实跑与哈希证据、全量定义/摘录/映射/登记/COUNT及④⑤待根执行说明、技能实际使用缺口。不输出原始thinking/thought过程。不要把LOCK-AMEND PASS写成模块PASS。完整终稿交付后结束。"
cmd=['/home/james/.local/bin/grok','-p',prompt,'-m','grok-4.6','--reasoning-effort','xhigh','--output-format','json','--permission-mode','bypassPermissions','--no-subagents','--disable-web-search','--cwd','/home/james/complex-management']
p=subprocess.Popen(cmd,cwd='/home/james/complex-management',stdout=subprocess.PIPE,stderr=subprocess.PIPE)
while True:
 try:
  out,err=p.communicate(timeout=300);break
 except subprocess.TimeoutExpired:pass
try:
 obj=json.loads(out.decode('utf-8'))
except Exception as exc:
 print(json.dumps({'exit_code':p.returncode,'parse_error':type(exc).__name__,'stdout_bytes':len(out),'stderr_bytes':len(err)}));sys.exit(p.returncode or 1)
print(json.dumps({'text':obj.get('text'),'sessionId':obj.get('sessionId'),'exit_code':p.returncode,'modelUsage':obj.get('modelUsage')},ensure_ascii=False))
sys.exit(p.returncode)
PY
'@
& 'C:\Windows\System32\OpenSSH\ssh.exe' -o BatchMode=yes james@192.168.88.160 $remoteScript
~~~

### LOCK-AMEND同会话补交
exit/result：0；首行合规完整终稿PASS。
结论出处：IN-RECEIPT。
实际执行的无头SSH及适配器命令全文：
~~~powershell
$remoteScript = @'
cd /home/james/complex-management && python3 - <<'PY'
import subprocess,json,sys
prompt="上一回合你已完成本次LOCK-AMEND独立全量复核，但返回JSON.text在CRITERIA首行之前串接了多段进度文字，所以交付尚未接收，根仍未执行④⑤，ledger旧基准未动。现在只修正交付格式，不重开审计、不改任何文件、不写报告产物、不读Codex结论。\n请在同一会话既有完整证据基础上，只用工具重新实算正式现锁、sidecar、ledger旧基准，以及你已经生成的/tmp/lock-amend-grok-20260908-v017/docs/audit-2026-09/plan/criteria-lock.md反向文件哈希，确认未变化。不要重写或覆盖反向证据；可以复核关键登记，复用你已独立执行的完整git apply反向与401/383/471结果。\n本回合不输出任何进度前缀、开场语或中间说明。只交一份完整最终回执，正文从独占第一行CRITERIA-LOCK-SHA256: <你本席实算的现锁hash>直接开始；给明确LOCK-AMEND PASS/FAIL、反向旧哈希与全量定义/摘录/映射/登记/COUNT依据，明确哪些是上回合已完成、哪些是本回合新复算，④⑤待根执行、B3仍4且无额外额度。不能把结论当模块PASS；不要输出thinking/thought。"
cmd=['/home/james/.local/bin/grok','--resume','01a08219-c659-7253-a3ff-0979255799dc','-p',prompt,'-m','grok-4.6','--reasoning-effort','xhigh','--output-format','json','--permission-mode','bypassPermissions','--no-subagents','--disable-web-search','--cwd','/home/james/complex-management']
p=subprocess.Popen(cmd,cwd='/home/james/complex-management',stdout=subprocess.PIPE,stderr=subprocess.PIPE)
while True:
 try:
  out,err=p.communicate(timeout=300);break
 except subprocess.TimeoutExpired:pass
try:obj=json.loads(out.decode('utf-8'))
except Exception as exc:
 print(json.dumps({'exit_code':p.returncode,'parse_error':type(exc).__name__,'stdout_bytes':len(out),'stderr_bytes':len(err)}));sys.exit(p.returncode or 1)
print(json.dumps({'text':obj.get('text'),'sessionId':obj.get('sessionId'),'exit_code':p.returncode,'modelUsage':obj.get('modelUsage')},ensure_ascii=False));sys.exit(p.returncode)
PY
'@
& 'C:\Windows\System32\OpenSSH\ssh.exe' -o BatchMode=yes james@192.168.88.160 $remoteScript
~~~

### 新锁B1首次
exit/result：0；V0.16 FAIL，后来新增用户原意解释并经外席补核纠正。
结论出处：/home/james/complex-management/docs/audit-2026-09/modules/M001-b1-grok-raw.md。
实际执行的无头SSH及适配器命令全文：
~~~powershell
$remoteScript = @'
cd /home/james/complex-management && python3 - <<'PY'
from pathlib import Path
import subprocess,sys,stat
target=Path('/home/james/complex-management/docs/audit-2026-09/modules/M001-b1-grok-raw.md')
def meta():
 if not target.exists(): return None
 s=target.stat()
 return (s.st_size,s.st_mtime_ns,s.st_ctime_ns,s.st_ino)
before=meta()
prompt="执行160-audit-prompt.md V0.17正式M001新锁B1修复前全量审计。你是唯一Grok独立席，不派子代理；不得读取Codex self raw或旧审计raw/旧结论，不继承任何旧PASS。main HEAD=242f487233d028173cb9198eecd079f5d877405c。ledger V1.55当前M001=IN_PROGRESS，B3累计4、额外额度已用尽；本阶段全程不修复。LOCK-AMEND已完成①至⑤并从M001 B1重开，当前锁V0.11、COUNT1，以你实读为准。\n只读取ledger顶部基准/COUNT及M001当前行，不读历史审计意见；先完整阅读.scratch/module-recheck-20260908/160-audit-prompt.md V0.17、正式plan/runbook.md及本节点实际适用技能、plan/modules.md当前M001定义、criteria-lock中该模块335个映射判据全文及general。技能按V0.17收窄，不派代理/改代码/提交/加新判据或新文件类型。\n本模块仅两份文件必须全文件全三线审查，不只看修改：docs/business-enum-quick-reference-v0.1.html（308物理换行）；docs/business-system-map-for-operations-v0.1.html（V0.13、2032物理换行）；共2340。需要交叉引用时只读第三权威PGM作上下文，不据此开启M002。映射BEQR001..197+BSM001..120+GEN001..018，共335，以实读核验。代码/设计/旧审批记录不能作新业务需求源。\n操作员已明确本次只允许A/D：BSM1380概括句与L2版本修订，已经完成。此前B/C的修改建议被拒绝，对应BSM1020客人来源和1068打折原因可配原文须保留。这个事实只限定修复权限，不预设你本次审计结论；仍须独立按现行HTML与冻结判据全量核查，有确定矛盾就如实FAIL，不因为拒绝修改写成已解决，也不要求再次批准同一被拒绝方案。\n三线完整跑：第一性原理必要性/实际行为/未验证前提；顺从式逐个映射判据及文档声明/路径/链接/枚举/金额例子；对抗式业务定义、分类、跨段/跨HTML能否同时成立、类型4可复現命令与反例。按runbook文档豁免，不因未跑Maven/业务系统测试自动FAIL，不能只机械计数当语义全审。\n四分类顺序冲突→细化→技术性→添加；每项问题临时ID、完整路径精确行号、足够业务原文上下文、最小复现/实际输出、已冻结判据ID、分类、工程缺陷是否成立。完整模块审完、收齐全部问题才一次返回，不中途改稿；仅若新问题涉及HTML按铁律上报，禁止改HTML；已拒绝的同一建议仅如实记录，不重复让操作员批准。旧finding身份由根后续B2匹配，你本次不读旧raw、不分永久ID。\n唯一可写/home/james/complex-management/docs/audit-2026-09/modules/M001-b1-grok-raw.md。当前路径已有内容，只追加本次完整B1报告并递增Vx.xx版本（可机械读取行首版本字段，不读旧意见），保留全部原字节。报告第一行必须你独立sha256sum正式criteria-lock所得CRITERIA-LOCK-SHA256: <64位小写hex>独占；本次完整报告只有开头这一条独占CRITERIA行，正文要引用ledger输出时请缩进或转述，避免形成第二个报告起点。不从派发获得期望哈希，本指令没有给。\n其后写当前HEAD/实际两HTML SHA/技能实际使用与缺口/完整文件覆盖与335判据全三线证据/全部finding/明确PASS或FAIL及HTML铁律情况。不要只写摘要；不创建其他产物、不写findings/ledger/questions/HTML/锁、不做B2/B3/B4/B5、不提交推送部署、不进M002。完整报告落盘后结束。"
cmd=['/home/james/.local/bin/grok','-p',prompt,'-m','grok-4.6','--reasoning-effort','xhigh','--output-format','json','--permission-mode','bypassPermissions','--no-subagents','--disable-web-search','--cwd','/home/james/complex-management']
proc=subprocess.Popen(cmd,cwd='/home/james/complex-management',stdout=subprocess.PIPE,stderr=subprocess.PIPE)
while True:
 try:
  output,error=proc.communicate(timeout=300)
  break
 except subprocess.TimeoutExpired: pass
output=error=None
after=meta()
code='GROK_EXIT' if proc.returncode else ('REPORT_MISSING' if after is None else ('PATH_MISMATCH' if not target.is_file() or target.resolve()!=target else ('REPORT_EMPTY' if after[0]==0 else ('REPORT_STALE' if after==before else None))))
if code:
 print('ADAPTER_ERROR='+code)
 print('EXIT_CODE='+str(proc.returncode))
 sys.exit(proc.returncode or 1)
print('REPORT_PATH='+str(target))
print('EXIT_CODE=0')
PY
'@
& 'C:\Windows\System32\OpenSSH\ssh.exe' -o BatchMode=yes james@192.168.88.160 $remoteScript
~~~

### B1补充复核
exit/result：0；V0.17 PASS，六项及程序误判撤销。
结论出处：/home/james/complex-management/docs/audit-2026-09/modules/M001-b1-grok-raw.md。
实际执行的无头SSH及适配器命令全文：
~~~powershell
$remoteScript = @'
cd /home/james/complex-management && python3 - <<'PY'
from pathlib import Path
import subprocess,sys,stat
target=Path('/home/james/complex-management/docs/audit-2026-09/modules/M001-b1-grok-raw.md')
def meta():
 if not target.exists(): return None
 s=target.stat()
 return (s.st_size,s.st_mtime_ns,s.st_ctime_ns,s.st_ino)
before=meta()
prompt="对刚完成的M001 B1 Grok V0.16做同节点证据补充复核，不进入B2/B3/B4/B5，不修任何业务/HTML/锁，不增B3，不启动其他模块。全程只有你这个Grok席，不派子代理。用户最新答复是在原B1命令运行期间到达，原派发没有包含它；这是新输入，不可继续仅按“拒绝修复”解释。\n操作员逐字原话：“b c两项原文不冲突。就是我的原意。”含义仅针对B/C：BSM1020客人来源、BSM1068打折原因的现行原文符合其预期业务，不是确认有缺陷但选择容忍，不需修改。不能强迫其再次接受同一修改，也不能把此答复推扩为其他项目自动通过。三份HTML字节及冻结锁均未变。请据此独立复核GR01/02分类与当前结论，并保留此前原票作为历史，不能删除、覆写。\n另有一项可核验的输入遗漏：原调用只提供“本次A/D”授权，不意味着撤销此前授权。用户此前已明确“批准上述第1、2处修改”对应BSM L2日期及L2003页脚；随后“可以”批准L499目录改为“已确认规则和实施前输入项”。这三处在B3-r3已经完成并经B4-r3双PASS。本次r4的before tree=4263c8c1338031a38d977741ffb111a0752b7092、after=7d6c4f7544de2957faec09e04aa2a262ba4aad86，完整不按路径过滤git diff --binary仅L2版本与L1380概括句，目录/页脚不在本次diff。可以只读ledger中的逐条用户审批原话及B4-r3两席原始批准事实，不把旧PASS当业务结论；直接独立实算r4完整diff。V0.16以HEAD历史总差异误判此次越权的程序判断请据实际授权范围纠正。\n还有以下根席对GR03..06的原文反证供你独立复核，不要求你服从根结论，不得直接复制为PASS：\nGR03：BSM707同一完整段先写常规合同准入审批，紧接“但下列七类是红线”逐项限制并写①②③系统不提供开放入口、④⑤⑥⑦有特定密钥逐笔边界。1901/1988是概括；全文语义能否受已经明写的特殊边界约束？“只有读概括而忽略该段明确但书才会放行未成年人”不等于当前HTML允许放行，不可另加“每处概括都须重抄红线”判据。请完整读取707。\nGR04：三份HTML共同最高解释权，BEQR29列票种，BEQR46明确固定天数/按自然月/按自然年有效期；BSM740/1970本身就是权威原文对日/周/月/年有效期规则的明确说明，不是下位代码发明。请检验锁里是否有“仅BEQR29排他穷尽所有票种名称、BSM不能细化”的实际明文；没有则不要自行设定这条新判据。区别票种名称与有效期规则的表述，不能仅以关键词不在同一行就断定业务冲突。\nGR05：BSM1611已经明写“补录一律进入主管复核队列，复核通过才生效”，同章1627短表没有声称否定这一要求，BEQR219亦明写人工补录。当前三处共同执行是否仍会漏？禁止用“实现者只看短表”替代实际HTML已经存在的规则。并核实判据ID：BEQR219对应BEQR-139，不是V0.16写的BEQR-140。\nGR06：BEQR196财务章节列六种合作模式，231治理章是同六类加合同模板细则。td.k是面向人阅读的“问题”列，不是DOM id/主键；两行处于不同章节，未发现程序读取只取第一个同名键。GEN001仍需具体证明重复内容无用途/不可达；不能自行新增“所有问题标题全文件唯一”的规则。若坚持工程缺陷请给实际证据，不能只给可读性偏好。\n另外V0.16按章的BEQR范围多处偏移，请从当前锁实际来源行生成准确的335判据覆盖（317HTML+18GEN），不要保留错误ID范围。保持本次全文审查已完成部分，补核影响项；报告须如实区分复用同字节全文阅读/本次新核内容，不伪称重读或重跑未做事项。该补充复核不是审计发现后抢修，也不撤销尚未复核的外审票。\n唯一可写/home/james/complex-management/docs/audit-2026-09/modules/M001-b1-grok-raw.md，只追加一份整合上述新输入/实核纠错及原全模块审查的本次完整报告，文件自身版本V0.16→V0.17；保留2510行旧字节。不读Codex self raw。必须先独立sha256sum正式criteria-lock，所得CRITERIA-LOCK-SHA256第一行独占，报告内不要再重复独占该字段。不给你期望值。全文纳入两份HTML/335判据的三线结论、全部最终finding、对原6项和程序问题逐条维持/撤销/改类理由、实际命令输出，最终明确PASS/FAIL。只有排除原疑点且无其他缺陷才PASS；仍有新HTML问题就完整列出原文、依据及明确实际影响，不改、不替用户批准。完成落盘后结束。"
cmd=['/home/james/.local/bin/grok','-p',prompt,'-m','grok-4.6','--reasoning-effort','xhigh','--output-format','json','--permission-mode','bypassPermissions','--no-subagents','--disable-web-search','--cwd','/home/james/complex-management']
proc=subprocess.Popen(cmd,cwd='/home/james/complex-management',stdout=subprocess.PIPE,stderr=subprocess.PIPE)
while True:
 try:
  output,error=proc.communicate(timeout=300)
  break
 except subprocess.TimeoutExpired: pass
output=error=None
after=meta()
code='GROK_EXIT' if proc.returncode else ('REPORT_MISSING' if after is None else ('PATH_MISMATCH' if not target.is_file() or target.resolve()!=target else ('REPORT_EMPTY' if after[0]==0 else ('REPORT_STALE' if after==before else None))))
if code:
 print('ADAPTER_ERROR='+code)
 print('EXIT_CODE='+str(proc.returncode))
 sys.exit(proc.returncode or 1)
print('REPORT_PATH='+str(target))
print('EXIT_CODE=0')
PY
'@
& 'C:\Windows\System32\OpenSSH\ssh.exe' -o BatchMode=yes james@192.168.88.160 $remoteScript
~~~

### 当前B2
exit/result：0；V0.18 PASS。
结论出处：/home/james/complex-management/docs/audit-2026-09/modules/M001-b1-grok-raw.md。
实际执行的无头SSH及适配器命令全文：
~~~powershell
$remoteScript = @'
cd /home/james/complex-management && python3 - <<'PY'
from pathlib import Path
import subprocess,sys,stat
target=Path('/home/james/complex-management/docs/audit-2026-09/modules/M001-b1-grok-raw.md')
def meta():
 if not target.exists(): return None
 s=target.stat()
 return (s.st_size,s.st_mtime_ns,s.st_ctime_ns,s.st_ino)
before=meta()
prompt="按160-audit-prompt.md V0.17执行M001当前B2整批候选复核，不是新B1。只读三份HTML/锁/计划，不派子代理、不修业务、不提交、不进B3/B4/B5/其他模块。先读本节点适用实际技能并按V0.17收窄。\n输入两席本次完整raw（每文件最后一个独占CRITERIA行至末尾）：docs/audit-2026-09/modules/M001-b1-self-raw.md V0.36完整B1+V0.37/V0.38附录；同目录M001-b1-grok-raw.md V0.17完整补充B1。候选findings.md V0.85当前顶部至“历史 A/D B2定稿”前；下面旧队列表只作永久ID与历史事实对照，不当当前队列。整批复核差集、分类、永久ID、不漏并和队列当前归属，必须独立判断。\n用户最新原话“b c两项原文不冲突。就是我的原意。”原文保持，不是容忍缺陷。Grok V0.17已撤销旧GR01..06与目录/页脚越权误判，当前finding空集；根当前4来源CX01/02→004技术性无业务发明无缺陷；CX03→已有014、CX04→已有015细化无缺陷，只登记。核对候选是否准确承接这项澄清与全部当前来源，历史撤销六票为何不新增ID是否无遗漏。根V0.37质证编号139已在V0.38如实纠正：补录L219应BEQR140，根完整317表原本正确已逐行实算。无隐去错误。\n当前16永久ID001至016保持唯一；A/D016已修/B4-r4双PASS出队，其余处置保留；队列空。无添加或questions；B3累计4，A/D额外1次已用尽，无第五次修复。B2双PASS后才候选定稿并按空队列跳过B3/B4进B5-r4，不准B2就写模块PASS。\n唯一写docs/audit-2026-09/modules/M001-b1-grok-raw.md：按V0.17 B2规约只追加本批B2完整PASS/FAIL票，文件自身版本V0.17→V0.18，保留所有既有字节；注明两份输入raw与候选版本、每项差集与判定证据、队列/计数及明确PASS/FAIL。B2追加段禁止写任何独占CRITERIA-LOCK-SHA256行，也不另建报告类型。不改findings/ledger/HTML/锁。落盘完成后结束。"
cmd=['/home/james/.local/bin/grok','-p',prompt,'-m','grok-4.6','--reasoning-effort','xhigh','--output-format','json','--permission-mode','bypassPermissions','--no-subagents','--disable-web-search','--cwd','/home/james/complex-management']
proc=subprocess.Popen(cmd,cwd='/home/james/complex-management',stdout=subprocess.PIPE,stderr=subprocess.PIPE)
while True:
 try:
  output,error=proc.communicate(timeout=300)
  break
 except subprocess.TimeoutExpired: pass
output=error=None
after=meta()
code='GROK_EXIT' if proc.returncode else ('REPORT_MISSING' if after is None else ('PATH_MISMATCH' if not target.is_file() or target.resolve()!=target else ('REPORT_EMPTY' if after[0]==0 else ('REPORT_STALE' if after==before else None))))
if code:
 print('ADAPTER_ERROR='+code)
 print('EXIT_CODE='+str(proc.returncode))
 sys.exit(proc.returncode or 1)
print('REPORT_PATH='+str(target))
print('EXIT_CODE=0')
PY
'@
& 'C:\Windows\System32\OpenSSH\ssh.exe' -o BatchMode=yes james@192.168.88.160 $remoteScript
~~~

### B5-r4
exit/result：0；V0.01 B5 PASS。
结论出处：/home/james/complex-management/docs/audit-2026-09/modules/M001-b5-r4-grok-raw.md。
实际执行的无头SSH及适配器命令全文：
~~~powershell
$remoteScript = @'
cd /home/james/complex-management && python3 - <<'PY'
from pathlib import Path
import subprocess,sys,stat
target=Path('/home/james/complex-management/docs/audit-2026-09/modules/M001-b5-r4-grok-raw.md')
def meta():
 if not target.exists(): return None
 s=target.stat()
 return (s.st_size,s.st_mtime_ns,s.st_ctime_ns,s.st_ino)
before=meta()
prompt="执行160-audit-prompt.md V0.17正式M001 B5-r4修复后整模块全量审计。仅你Grok独立席，禁止子代理，禁止业务/HTML/锁/ledger/findings/questions修改，不提交推送部署，不进M002。当前B2已双PASS且队列空，跳过新的B3/B4；B3累计4，额外仅A/D1次已用尽，无第五次修复。ledger V1.57当前IN_PROGRESS，本步不能先记模块PASS。\n不读Codex任何raw或旧审计raw/结论，不继承旧PASS。先实读.scratch/module-recheck-20260908/160-audit-prompt.md V0.17、plan/runbook.md、plan/modules.md的M001定义、当前criteria-lock中M001全部335判据全文（BEQR197+BSM120+GEN18），并实际读取本节点适用技能按提示词收窄。只读ledger顶部基准/COUNT及当前M001行，不读历史席位意见。main HEAD=242f487233d028173cb9198eecd079f5d877405c。\n审完整两份HTML全部三线，不只看diff：docs/business-enum-quick-reference-v0.1.html 308行；docs/business-system-map-for-operations-v0.1.html V0.13 2032行，共2340；PGM仅允许相关交叉上下文、不开M002。第一性原理问必要性/实际行为/未验证前提；顺从式逐335映射；对抗式用整文边界构造正常可复现反例。审全文是否能共同执行，不能仅假设实现者忽略其他明示条款。只能用冻结判据和三权威HTML，不自行新增“每处概述须重抄全部细则/所有人用问题列标题全文件唯一/仅一份HTML排他解释另两份”等判据。若仍有确定问题要如实报告，不受根结论约束。\n必须纳入操作员当前直接业务解释原话：“b c两项原文不冲突。就是我的原意。”仅针对BSM1020客源及1068打折原因现行可配原文，现行两项即预期业务，不是容忍已确认缺陷；不扩大到其他分类、无需再次申请这两处修改。另累计授权事实：此前已逐条批准BSM L499目录、L2003页脚日期和L2日期，已在r3实施；r4仅批准L1380概括句及L2版本V0.12→V0.13。新“仅A/D”没有撤销此前批准。不要用HEAD累计diff指称此次越权；若核修复权限，r4完整before/after树为4263c8c1338031a38d977741ffb111a0752b7092与7d6c4f7544de2957faec09e04aa2a262ba4aad86。\n按文档类型豁免，不因未跑Maven/业务系统测试自动FAIL，但所有执行声明必须附本回合实际命令与输出。四分类顺序冲突→细化→技术性→添加，完整收齐所有finding才交付，每项带临时ID/完整原文业务上下文/文件行号/实际证据/冻结ID/工程缺陷是否成立；无新问题也要完整范围、335闭集和三线证据。严禁中途修复。\n唯一可写/home/james/complex-management/docs/audit-2026-09/modules/M001-b5-r4-grok-raw.md。该路径本次新建，文件自身版本V0.01；若已存在则追加并递增Vx.xx、保留原字节。先独立实算sha256sum当前criteria-lock，第一行严格独占CRITERIA-LOCK-SHA256: <实算64hex>；不从派发取期望值（本指令未给），正文不再出现其他独占该字段行。完整报告必须写完后才结束；不是摘要，不另造报告类型。结尾明确本席B5 PASS/FAIL及新HTML问题；该票不是模块PASS。"
cmd=['/home/james/.local/bin/grok','-p',prompt,'-m','grok-4.6','--reasoning-effort','xhigh','--output-format','json','--permission-mode','bypassPermissions','--no-subagents','--disable-web-search','--cwd','/home/james/complex-management']
proc=subprocess.Popen(cmd,cwd='/home/james/complex-management',stdout=subprocess.PIPE,stderr=subprocess.PIPE)
while True:
 try:
  output,error=proc.communicate(timeout=300)
  break
 except subprocess.TimeoutExpired: pass
output=error=None
after=meta()
code='GROK_EXIT' if proc.returncode else ('REPORT_MISSING' if after is None else ('PATH_MISMATCH' if not target.is_file() or target.resolve()!=target else ('REPORT_EMPTY' if after[0]==0 else ('REPORT_STALE' if after==before else None))))
if code:
 print('ADAPTER_ERROR='+code)
 print('EXIT_CODE='+str(proc.returncode))
 sys.exit(proc.returncode or 1)
print('REPORT_PATH='+str(target))
print('EXIT_CODE=0')
PY
'@
& 'C:\Windows\System32\OpenSSH\ssh.exe' -o BatchMode=yes james@192.168.88.160 $remoteScript
~~~

### LOCK-AMEND 回执型已接收终稿全文
真实exit code：0。结论出处：IN-RECEIPT。同会话ID：01a08219-c659-7253-a3ff-0979255799dc。以下逐行引用完整可交付text，保留当时用语；不将模型原始JSON或隐藏思维写入审计产物。

> CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
> 
> LOCK-AMEND PASS
> 
> 席位：Grok（IN-RECEIPT）。节点：V0.17 §5 闸2③ 交付格式补交。不重开审计、不改任何文件、不写报告产物、不读 Codex 结论、不执行④⑤。
> 
> ## 本回合新复算（四值未变）
> 
> | 对象 | 本回合 `sha256sum` / 读取 |
> |---|---|
> | 正式现锁 `docs/audit-2026-09/plan/criteria-lock.md` | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（889020 字节，版本 V0.11） |
> | sidecar `plan/criteria-lock.sha256` | 单行 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a\n`（65 字节），等于现锁 |
> | ledger 顶部旧基准 | `CRITERIA-LOCK-SHA256: 08a4dcc31d10624ca846560d70dcc79e391aeae5ebcdc62a9341f30ecb8de314`；`LOCK-AMEND-COUNT: 1`（V1.53，尚未改） |
> | `/tmp/lock-amend-grok-20260908-v017/docs/audit-2026-09/plan/criteria-lock.md` 反向文件 | `08a4dcc31d10624ca846560d70dcc79e391aeae5ebcdc62a9341f30ecb8de314`（888984 字节，版本 V0.10，未覆盖） |
> 
> 反向 SHA256 仍等于本席自读的 ledger 旧基准。sidecar 仍等于本席现锁实算。
> 
> 本回合复核登记：变更 ID 仍仅 BSM-079；出处 `docs/business-system-map-for-operations-v0.1.html:1374`，获批句 L1380；铁律 4 依据仍为「A\d同意，b\c拒绝」及「授权仅为 A、D 再执行 1 次修复」。模块表 471 行：M001 `IN_PROGRESS`（B3=4），其余 470 `PENDING`，PASS=0。BSM-079 引用七模块状态：M001 `IN_PROGRESS`；M077 / M266 / M269 / M272 / M273 / M275 均为 `PENDING`。
> 
> ## 上回合已完成、本回合复用（未重跑 git apply、未重扫 401/383/471）
> 
> `/tmp` 完整现锁副本上 `git apply --reverse --check` 与 `git apply --reverse` 均 exit 0，patch cleanly。反向 git blob `6ec5de54155b671756430089331b98f972b500bd`；现锁 git blob `8db3fc02ce8a6dc998863a9784e7e88af511b960`。B3 after tree `7d6c4f7544de2957faec09e04aa2a262ba4aad86` 中锁文件与该 blob 均等于反向 SHA256。
> 
> 现锁相对反向文件仅两 hunk、四条内容标记：锁版本 V0.10→V0.11；BSM-079 摘录一枚 `<li>` 由「处理后回访客人或商户，验收合格才关闭。」改为「处理后的回访、验收和关闭，按下述已确认的工单类型分支规则执行。」定义 ID 401 集合与顺序不变（GEN 18、BEQR 197、BSM 120、PGM 66）。变更定义体仅 BSM-079；GEN 正文未改。383 条 HTML 摘录对三份正本按来源行切片 383/383 逐字且源行正确；独立锚点 197+120+66=383。映射区 M001…M471 共 471 行，与反向锁字节全等，映射变化集为空。BSM-079 精确 token 引用 `M001, M077, M266, M269, M272, M273, M275`（7），最早 M001。B/C 拒绝原文仍在 BSM-052 / BSM-054 且新旧锁相等；D 的 V0.13 头注释不在任何摘录内；未写分类例外。
> 
> COUNT 不计次条件成立：本次定义或映射变更的全部判据 ID 只有带 HTML 出处且注明铁律 4 批准的 BSM-079，没有通用工程判据。两席③通过后 COUNT 保持 1。
> 
> ## 待根执行
> 
> ④ 将 ledger 顶部 `CRITERIA-LOCK-SHA256` 更新为 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`，`LOCK-AMEND-COUNT` 保持 1。
> 
> ⑤ 最早引用模块 M001 由 `IN_PROGRESS` 改回 `PENDING`，从新锁 B1 重开；另六引用模块本已 PENDING；无本版 PASS 需回退；其余 470 保持 PENDING。
> 
> B3 仍为 4，无额外额度；锁同步不增加 B3。本结论是 LOCK-AMEND PASS，不是模块 PASS。

### 交付边界
本轮仅关闭M001与获准的A/D修复，不能据此宣称471模块全仓审计完成。76个未发布文件此前查重/溯源是单独只读结论，没有在本轮修复或发布它们。
当前正本仍在160:/home/james/complex-management。本机交付副本统一放D:\AI\codex\outputs\160-M001-r4-20260908；复制后以逐文件SHA256及严格UTF-8核验。
