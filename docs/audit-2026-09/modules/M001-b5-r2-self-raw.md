CRITERIA-LOCK-SHA256: 73ce1c4523de229a11552ca343f594e1506da3dc10e68d3dac216dd4ca232439
# M001 B5-r2 Codex 独立完整报告
版本：V0.01

控制提示词V0.13（SHA256=3e26cace91d01b679b03fc3df943a90f90cfb4d60d29be8552131169dd93c77f）；按锁V0.05执行B5-r2整模块独立终审，首轮全仓审计没有重开。B2当前队列为空，故不增加B3/B4；先前B3-r2与B4-r2双PASS证据保留。
模块M001/322，ledger IN_PROGRESS，B3已执行2次，LOCK-AMEND-COUNT=0（V0.13计数口径，历史5次均为全ID有HTML来源及逐处批准的文字同步，18行证据保留于ledger V0.50；没有第6次LOCK-AMEND）。
独立性：Grok先派，本席独立复核全模块。Grok于2026-09-05 06:19:34 UTC确认进程结束、exit code 0，只返回指定B5 raw路径及退出码；本报告写入之前没有读取Grok本次B5 raw或原始JSON。全部阅读与检查由本席执行，未派第三席、未改业务文件。

## 范围与实际读取
- 枚举速查表全文1–308行，13章节/197个td.k条；文件V0.07，SHA256=4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66。
- 业务系统地图全文1–2032行，27个h2/93个h3；文件V0.08，SHA256=3edff56a7640565d08983621e34d726d149261e70fd747ecb88b8e9cc27774ea。
- 合计2340行；原始源文件全文由检查代码读取，人工重新逐段复核全部业务正文、结构/CSS和页尾实际脚本。本次enum按1–63、64–98、99–117、118–157、158–194、195–246、247–277、278–308全读；map1–504原文全读，505–2032按段输出全部独有正文，已在本次enum读过的完全相同长片段经SequenceMatcher和逐字断言后以enum行号引用，阈值80字；没有以摘要代替未读正文。map661–707首次显示截断，已单独完整重读。
- 地图按1–504结构样式导航、505–771总览与治理、772–928票务会员、929–1119终端与酒吧、1120–1307服务/供应/经销/游乐、1308–1478商户合同物业人力、1479–1635第三方留痕、1636–1826财务分红看板、1827–2032入口关系交付/脚本全部覆盖。
- 治理地图仅核对必要上位交叉依据：真实股权/到期借款436、项目和敏感权限653–680、U盘862–864及隐私870–872；不将该只读对照声称为M002完整审计。其SHA仍c63041a15599fbe4768b7131f78e9c6d6c0ffabe8b5ffc3a1151e24eac1c1dc0。

## 技能与证据边界
对应节点已实际读取using-superpowers及Codex工具映射、grill-me/grilling、batch-grill-me、ponytail-review、diagnosing-bugs、ubiquitous-language，使用第一性原理追问/反例/术语核对；codebase-design与verification-before-completion已在本次锁复核中读取并沿用。只按V0.13收窄技能：不派子代理、不另建术语文件、不增加人闸、不在B5修复。初始技能缺口沿用既有预检，不虚报工具调用。本文为HTML文档审计，按runbook豁免后端行为/TDD；未运行浏览器、辅助阅读设备、实际支付、数据库或法定财税系统，也不宣称这些已验证。

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
以上为本次B5重新读完全部范围后的结果，未形成新工程缺陷或新增业务添加项。另复核正式挂起订单先出品的显式例外（map1101–1112）及员工代订免定金对尾部摘要的细化（map1061/1437/1964）；没有把一般摘要与后述明文例外机械地当矛盾。

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
| GEN-017 | 固定冲突→细化→技术性→添加；技术性无发明项M001-004已由B2登记并确认，未新增业务需求或锁外判据。 |
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

## 既有技术性登记复核（本次无新增finding）
M001-004：既有技术性登记，工程缺陷否。来源为enum1–24及各表结构、map1–504/2008–2030；依据类型4，GEN-001/002/010/012/015/017/018。
无业务发明：结构/样式只决定本页呈现和跳转，不增加价格、资格、状态、主体或付款规则。13个问题/答案表头已存在并有效，不再重复报008。实际HTMLParser检验26张表、197标签、120地图标题、45有效内部链接；实际导航脚本仅选.nav内27链接。该登记复用现有正式ID，不新造重复ID。

## 可复现检查命令与结果
执行方式：在160正本主工作区通过python3 -标准输入执行下列完整代码，不写脚本文件。真实进程exit code 0。
本次B5重新执行下列完整检查，一次PASS。代码检查真实HTML，不是仅引用之前检查结果；实际导航选择器限定.nav内27个链接，金额测试从当前HTML默认行解析。

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
B5审计结论：PASS。新增工程缺陷0；新增添加项0；无新增finding。既有M001-004技术性分类及M001-007/009/010/011细化分类，经本次全量复核未发现需重新入队的问题。
当前仅为Codex本次B5票，不是模块PASS。只有本报告完整落盘后才读取Grok本次完整B5，按双席合取与B5后四值相等规则决定收口或回退。源文件、锁、正式索引及HEAD没有被本方审计改动。
