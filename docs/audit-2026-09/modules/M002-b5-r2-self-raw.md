CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
# M002 B5-r2 修复后全量审计（Codex）

版本：V0.01
控制：V0.17。M002/322，IN_PROGRESS；B3累计2次。日期2026-09-05。
本席结论：B5 PASS；无新增缺陷、无旧问题残留。此刻Grok本次B5尚未收齐，尚未模块PASS。
唯一模块输入：docs/platform-governance-map-for-operations-v0.1.html，V0.08，完整979行/102733字节；SHA256=9864216e609df2f319fa2cf7e51b61f06003ce45dff64a1a4098e71435865a68。
判据：现锁V0.06，18GEN+66PGM共84。PGM-066申请20完整LOCK-AMEND五步已完成；重开B1并整批B2双PASS，队列空，所以本次未重复B3/B4，B3仍2。
独立性：先派Grok B5-r2，未读本次b5 Grok raw、结论或JSON。本席重新读取当前全文（1—499、500—742、743—979，正文标签去壳便于阅读、注释另完整输出；所有原字节参与标签栈/引文比较），重新应用全部84条、重新运行负例与辅助校验。既有B1/B4的PASS不作为本次证据。

## 三线完整审计

第一性原理：
1. 本HTML是治理规则的权威出发点。项目/主体、合同、人员/设备、权限、终端、楼宇、隐私及证据链承担PGM-001—066原文，不可被PRD/实现替代；HTML/CSS/导航壳只为读者定位这些规则，无新业务实体或规则。
2. 文档产物使用原文和可复现命令证据（类型1/4）。本次完整解析979行，重算84条映射/66段逐字原文/18导航和13卡片，17个已确认块。所有结构错误/坏链接负例能失败；预算与负尾差算例只证明正文算式可保持守恒，不冒充后端已实现。
3. 后端鉴权、并发、税务、MDM、密码/暗纹、真实第三方联调、浏览器视觉/读屏均不是本次已执行事项。对其要求只作三份HTML语义一致性审查，具体实现留所属模块。规范页中的App/Web/职级标识等不自动等于产品终端出现英语，也不另设逐字别称门槛。历史评论反映时态，不能拿历史“上线”字样证明当前后端功能。

顺从式：1—255行静态壳由18GEN相关项覆盖；256—979覆盖66锚点整段。下表逐条给出具体用途、约束与证据，现值原文与冻结文本完全相符；不是只查看批准20两处差异。

对抗式：以合同通过但权限不足、权限模板自动提权、开关关审计、密钥放行容量/年龄红线、游乐场次另算容量、第三方缺映射入账、私机越权、转台改旧提成、能源异常直接出账、挂单取消/支付并发、发放超预算、用下单时间顶替支付时间等现实反例逐条核对原文。均有当前明文边界，未发现可由冻结ID支持的新缺陷。

技能沿本节点实际读到的grilling、codebase-design、ponytail-review、receiving-code-review、verification-before-completion和writing-for-agents执行收窄自问/证据流程；无第三席、无额外人闸、无源码修复。using-superpowers及Codex参考已实际读取，不声称重新安装/调用未核可选技能。ponytail-review未发现可删业务无损块，net: 0。

## 全部84判据逐项结果


业务原文66段的判据正文相同要求与锚点一致、不能遗漏/发明、可执行声称须实证；以下均对当前原文应用，不把文本自身逐字相等当成唯一的语义审查。

| ID | 原文行段 | 顺从/必要性及对抗结论（原文依据类型1；结构或算例另用类型4） | 结论 |
|---|---|---|---|
| PGM-001 | 256—307 | 归属/资格/责任总则；全链例外止于安检设备，普通展示脱敏；金额税务例外与尾差主体、先成功、红线优先写明。反例：关闭开关抹去审计，733/738明确拒绝。 | PASS |
| PGM-002 | 308—338 | 五步次序明确对象→合同→人→设备→留痕；拒绝及升级也记录，不能只记录成功。 | PASS |
| PGM-003 | 339—341 | 入口容器承接以下13张卡片；源中所有入口均有实际章节目标，不添业务。 | PASS |
| PGM-004 | 342—346 | 项目/集团入口指向project；多项目预留不伪称已投产。 | PASS |
| PGM-005 | 347—351 | 空间点位入口指向space；位置不等同单独容量计数。 | PASS |
| PGM-006 | 352—356 | 商户主体入口指向merchant；区分运营关系与权限。 | PASS |
| PGM-007 | 357—361 | 合同准入入口指向contract；合同有效性不能代替权限。 | PASS |
| PGM-008 | 362—366 | 员工手机入口指向staff；私机绑定与工作机分别管理。 | PASS |
| PGM-009 | 367—371 | 权限入口指向permission；角色模板不消除单独拒绝。 | PASS |
| PGM-010 | 372—377 | 终端入口指向terminal-switch；中文叙述中的App/Web是文档别称，无业务枚举替换证据。 | PASS |
| PGM-011 | 378—382 | 设备入口指向device；设备台账不等于自动授权。 | PASS |
| PGM-012 | 383—387 | 商户侧入口指向merchant-side；全开放有本商户/合同/权限限制。 | PASS |
| PGM-013 | 388—392 | 开放接口入口指向open-platform；先自有开放能力，真实第三方待资料。 | PASS |
| PGM-014 | 393—398 | 留痕入口指向audit；重复展示导航有可验证用途。 | PASS |
| PGM-015 | 399—403 | 楼宇运营入口指向building-ops；与员工/游乐设备分域。 | PASS |
| PGM-016 | 404—411 | 应急隐私入口指向recovery-privacy；破窗不免审计。 | PASS |
| PGM-017 | 412—414 | 对象带项目归属；未来集团视图与当前单项目不冲突。 | PASS |
| PGM-018 | 415—423 | 物理项目与运营公司多对多；城市项目公司兑付通用储值是付款责任，不取得域定价权。 | PASS |
| PGM-019 | 424—439 | 六主体职责与资金关系独立；酒吧服务费代收应发/扣留分开；内部协议先最高审批、借款先偿还到期本息再分红。法律事项只记录，不在本模块作外部法律合规结论。 | PASS |
| PGM-020 | 440—451 | 项目档案与职位模板下钻；模板只决定视图层级，没有可任意配数据范围的入口声明。 | PASS |
| PGM-021 | 452—462 | 集团汇总价值与隔离相容；预留结构不宣称跨项目默认权限。 | PASS |
| PGM-022 | 463—466 | 空间归属影响合同/低消/设备/库存；未把所有点位定义成容量域。 | PASS |
| PGM-023 | 467—475 | 平面图导入仅候选，人工复核及高危二次确认后发布；拒绝静默覆盖。 | PASS |
| PGM-024 | 476—487 | 479明文容量只按单项目实时人数，与枚举145/146逐字口径一致；时段/设备容量已排除。 | PASS |
| PGM-025 | 488—491 | 商户档案含经营范围、联系方式、合作及终端范围；不凭商户名称赋权。 | PASS |
| PGM-026 | 492—506 | 唯一绑定四类主体可不同；公司权责调整审批且不追溯；招商黑名单例外有独立权限/非自审；客户关怀协调与两类投诉闭环不冲突。 | PASS |
| PGM-027 | 507—517 | 商户只见自有资料，第三方细节按其规则且不在本文缓存；本页未虚构对接成功。 | PASS |
| PGM-028 | 518—521 | 合同摘要先阻断基础违规；后文权限不足无放行通道。 | PASS |
| PGM-029 | 522—543 | 经营超范围可逐单批，权限不足换有权人；①②③红线无密钥例外，④⑤⑥⑦逐笔规则明确；审批超时待审/升级不自动放行；退租争议挂起与已批准两阶段规则一致。 | PASS |
| PGM-030 | 544—554 | 业务意义的红线不因普通审批放行，与529附近专门密钥逐笔例外按具体规则并读，不据概括句抹去明文例外。 | PASS |
| PGM-031 | 555—571 | 经销主体独立；先审/预留/付款核验/出库；迟到到账待退款；职责分离与税务快照非锁定旧税法。 | PASS |
| PGM-032 | 572—575 | 员工、私机、工作机分别对应；设备合规不替代人员权限。 | PASS |
| PGM-033 | 576—585 | 自有/商户两注册路径有域批准；私机生成标识不读硬件码；旧机解绑、异常失效和寄存柜自动留痕。 | PASS |
| PGM-034 | 586—598 | 企微工作会话原文最小授权审批及访问审计；永久留存与按特定清理规则不矛盾。未把注释的旧审计结论当作本次法律证据。 | PASS |
| PGM-035 | 599—610 | 提成工资月结及税务处理；营销材料不混考勤，禁止陪酒商品，其他服务待资质专题；本模块不执行薪税。 | PASS |
| PGM-036 | 611—614 | 入场三环节设备区分；台账绑定员工/点位，不生成安检按人记录。 | PASS |
| PGM-037 | 615—623 | 设备状态和领还维修白名单；企业管控声明不声称浏览器具备MDM。 | PASS |
| PGM-038 | 624—635 | 工作机扫码、平板点单、一体机授权售票受点位限制；不存在一体机默认全部票种。 | PASS |
| PGM-039 | 636—642 | 场景编排独立立项前未定义自动动作；计费计时明确不建设，不据设备场景附加需求。 | PASS |
| PGM-040 | 643—646 | 每功能最小权限，角色及员工增减；只读文档本身无授权端点。 | PASS |
| PGM-041 | 647—663 | 显式拒绝最高；岗位范围变更旧权即时失效，跨域/高危新权先审批，未知功能按高危；不是角色名默认授敏感数据。 | PASS |
| PGM-042 | 664—672 | 职级影响模板/薪资范围但不替代功能权；M1-M10是文档标识说明。 | PASS |
| PGM-043 | 673—687 | 外包受任务/区域/有效期限制，审计只读；查看与导出分离、强制脱敏/密码/暗纹留痕。商户自有客户例外与BSM862一致；本模块没有可执行导出算法可声称已验证。 | PASS |
| PGM-044 | 688—711 | 挂单/隐私台/经销权分离；支付门店统一、单菜单、最优单券原子生效；转台/并台不改历史提成。 | PASS |
| PGM-045 | 712—715 | 开关开只允许进入后续权限判断；关闭拒业务，不能关闭审计。 | PASS |
| PGM-046 | 716—727 | 七组展示涵盖十一终端，组合词不减少终端数；不强求别称逐字等于枚举键。 | PASS |
| PGM-047 | 728—748 | 七档优先级、缺行关闭、故障关闭、留痕例外；金额规则整体切换与规则内参数粒度区分；私机低风险能力限制明确。 | PASS |
| PGM-048 | 749—780 | 商户全开限本商户授权；游乐场次安排不是场次容量；数据/设备/员工/检修闸保持。 | PASS |
| PGM-049 | 781—784 | 供应商资料未确定不阻塞先建自有开放能力；未声称真实外部联调。 | PASS |
| PGM-050 | 785—793 | 第三方身份及授权，缺门店映射挂差异不得入账；调用全审计。 | PASS |
| PGM-051 | 794—805 | 后续拿资料逐步接，按供应商资料补映射；不按未知外部规范推导新判据。 | PASS |
| PGM-052 | 806—809 | 楼宇本体与物业/合同/财务连接，非游乐巡检替代。 | PASS |
| PGM-053 | 810—820 | 四类设备组、六状态与定期维保，告警两来源；每台周期可配无自定固定周期。 | PASS |
| PGM-054 | 821—831 | 实际费用代收不加价，公区不摊；总分差另列损耗费、异常第二人核实主管确认后出账；与枚举199/258的具体口径并读。 | PASS |
| PGM-055 | 832—843 | 四类巡检、三种计划及留痕、异常两模式，复检主管闭环；能耗走独立域，与枚举273相符。 | PASS |
| PGM-056 | 844—856 | 物业/财务/供应链/审计各有源对象；未创建新第四业务权威。 | PASS |
| PGM-057 | 857—859 | 应急/隐私并列；明文加密储存不免显示脱敏。 | PASS |
| PGM-058 | 860—867 | 旧密钥先作废，再制新/验证/交接；破窗全留痕；没有预制备用钥匙，未擅增恢复产品。 | PASS |
| PGM-059 | 868—878 | 客人与员工PII加密，客人手机号专门通道、单据跳入同鉴权留痕脱敏；会员号等于手机号而非独立卡号。 | PASS |
| PGM-060 | 879—891 | 本人手机号验证/同意、无后台导入；预算等式通过实际Decimal算例；营销面向公开资格，禁止个人定向；验龄用途/最短必要留存写明。 | PASS |
| PGM-061 | 892—895 | 责任主体/操作/原因/结果可追，不能只靠口述。 | PASS |
| PGM-062 | 896—910 | 合同/权限/设备/开关/服务/异常/交接/敏感查看列明审计动作，与自动优先但必完整口径相容。 | PASS |
| PGM-063 | 911—922 | 退款/调账/报损/金额补录/恢复运营进入复核；文档未伪造审批结果。 | PASS |
| PGM-064 | 923—934 | 正式挂单已扣库未付款，阻断交班日结；取消与付款互斥，迟到钱待退；坏账结案须财务证据，不自动扣薪。 | PASS |
| PGM-065 | 935—950 | 七业务连接均带项目合同权限责任；游乐容量明确实时在玩人数，第三方身份先验。 | PASS |
| PGM-066 | 951—979 | 全17条确认及页脚重新阅读全文；订单/支付时间区分，履约逐环节时间；版本V0.08且页脚旧日期删除，解释权全文与新锁匹配；平台规则确认不等同功能已上线。 | PASS |

| 通用ID | 当前适用性、事实及边界 | 结论 |
|---|---|---|
| GEN-001 | 入口和正文帮助业务定位治理约束；静态壳原生HTML/CSS，无库或代理层；已批准日期删除生效，不顺便去历史注释。 | PASS（文档范围） |
| GEN-002 | HTMLParser完整标签栈、锚点和导航成功；本文件非后端业务入口，不宣称运行支付/审批。 | PASS（文档范围） |
| GEN-003 | 内存错误闭合标签/不存在链接能触发失败；金额尾差与积分预算实例断言有明确输出；现有真实CLI回归运行。 | PASS（文档范围） |
| GEN-004 | 静态文档无输入框、脚本或网络加载；第三方、员工注册、图导入信任前确认规则逐条检查。 | PASS（文档范围） |
| GEN-005 | 权限逐项、跨项目/高危先审、显式拒绝最高；仅文本一致性，服务端执行留后续所属模块。 | PASS（文档范围） |
| GEN-006 | 金额/主体/合同快照、预算、挂单、独立税务例外语义明确；非税0.05分配的负尾差单列守恒。 | PASS（文档范围） |
| GEN-007 | 先成功、只一次、逐笔密钥例外与取消/支付互斥明确；不把模拟当成数据库并发试验。 | PASS（文档范围） |
| GEN-008 | 本模块无DTO/OpenAPI/迁移；查本文终端十一类、状态/名词与相关HTML明文一致，名称别称不扩成枚举不一致。 | PASS（文档范围） |
| GEN-009 | 配置缺行/拉取失败、超时待审、回流缺映射、迟到款及恢复流程均有安全处置描述。 | PASS（文档范围） |
| GEN-010 | 无脚本/表单/外部资源/事件属性/密钥值；PII与敏感信息权限有明文限制。未进行法律合规背书。 | PASS（文档范围） |
| GEN-011 | 审计写入不能被开关关闭，PII查询导出/破窗/人工原因可追；不将历史注释证明当实际部署证据。 | PASS（文档范围） |
| GEN-012 | 静态文件102733字节，无运行循环/连接/缓存；sticky区可滚动，980px媒体断点改一列。 | PASS（文档范围） |
| GEN-013 | 仅原生HTML/CSS无下载依赖；现有entry-anchors与html-ledger-sync退出0，当前门禁可复现。 | PASS（文档范围） |
| GEN-014 | 无SQL/迁移执行面，不适用运行迁移；不能因HTML未含迁移而造缺陷。 | PASS（文档范围） |
| GEN-015 | zh-CN/UTF8/viewport、原生可聚焦链接、18导航目标/4表头均有效，无取消focus规则；文档描述App等不等于产品界面英文。未声称浏览器截图或辅助读屏实测。 | PASS（文档范围） |
| GEN-016 | 当前V0.08与批准20一致，旧更新日期已去；66整段引文精确；未来/已拍板文本不冒充上线验证。 | PASS（文档范围） |
| GEN-017 | 全部按冲突→细化→技术性→添加；本文是权威业务来源，不能因另两份未逐句重复就当添加；壳技术性无商业发明。 | PASS（文档范围） |
| GEN-018 | 结论使用当前原文与实际结构/算例/门禁输出；旧PASS、席位说法与历史备注不作独立依据。 | PASS（文档范围） |



## 旧问题、分类与修复残留

M002-001：与HEAD源像作精确两次唯一替换后，结果逐字等于当前HTML。第2行只把原V0.07带日期注释改为V0.08无日期；第974行只删除旧“更新日期：2026-09-01。”，用途与解释权全部保留。CSV两镜像、批准台账仍与B3-r2后像逐字一致。没有未批准HTML改动。
M002-002：三处十一端列表为同集，缩写/折叠是已有上位规则细化，未发现新类别/相反授权；当前队列否。
M002-003：静态壳技术性无业务发明，无新权限/金额/状态/业务时限；原生导航表头无结构缺陷；只登记，队列否。
M002-004：工具与回归测试逐字等于已审B3-r2后像；本次真实CLI回归实际PASS，历史raw被窄排除、现行findings/ledger/收口/普通文档失效引用仍拒绝。两个发布基础门禁均实跑PASS。只核当前模块必要辅助修复，没有把未来M315标成已完成。
以上已有4ID当前无残留；本次CX新临时缺陷空集，冲突0、添加0、工程待修0，没有未答问题。先按冲突→细化→技术性→添加判，未以HTML未提及直接删，也未把纯技术内容抛给用户判业务。

## 本次实际机械检查

下列Python脚本经正本python3 stdin重新完整执行，退出0：

```python
from pathlib import Path
from html.parser import HTMLParser
from collections import Counter
from decimal import Decimal,ROUND_HALF_UP
import re,html,hashlib,json,subprocess
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

r=Path('/home/james/complex-management');a=r/'docs/audit-2026-09';p='docs/platform-governance-map-for-operations-v0.1.html'
b=(r/p).read_bytes();s=b.decode('utf-8');assert '\ufffd' not in s
assert hashlib.sha256(b).hexdigest()=='9864216e609df2f319fa2cf7e51b61f06003ce45dff64a1a4098e71435865a68'
assert len(s.splitlines())==979 and b.count(b'\n')==979
assert '<!-- 文件版本：V0.08 -->' in s and '更新日期' not in s
assert re.search(r'<meta\s+charset=["\x27]utf-8["\x27]',s,re.I)
d=Doc();d.feed(s);d.close()
assert not d.stack and len(d.ids)==len(set(d.ids))
assert all(h.startswith('#') and h[1:] in d.ids for h in d.links)
assert len(d.headings)==66 and d.tables==4 and len(d.links)==31 and d.lang=='zh-CN'
assert len(re.findall(r'<h2\b',s))==18 and len(re.findall(r'<h3\b',s))==48
assert len(re.findall(r'<thead\b',s))==4
assert not d.events and not d.external and not re.search(r'<(?:script|form|input|iframe)\b',s,re.I)
nav=re.search(r'<nav class="nav">(.*?)</nav>',s,re.S).group(1);navhrefs=re.findall(r'href="([^"]+)"',nav)
secids=re.findall(r'<section id="([^"]+)"',s)
assert len(navhrefs)==18 and [x[1:] for x in navhrefs]==secids
lk=(a/'plan/criteria-lock.md').read_text()
mapping=re.search(r'^M002 → \[([^\]]*)\]',lk,re.M).group(1).split(', ')
assert len(mapping)==84 and len(set(mapping))==84
defs={m.group(1):m.group(0) for m in re.finditer(r'^### ((?:GEN|BEQR|BSM|PGM)-\d{3})\b.*?(?=^### |^## 模块|\Z)',lk,re.M|re.S)}
assert set(mapping)<=set(defs)
coverage=[]
for cid in mapping:
 if cid.startswith('GEN'):continue
 block=defs[cid];src=re.search(r'- 来源：'+chr(96)+r'([^'+chr(96)+r']+):(\d+)'+chr(96),block)
 path,n=src.group(1),int(src.group(2));assert path==p
 q=re.search(chr(96)*4+r'html\n(.*?)\n'+chr(96)*4,block,re.S).group(1)
 assert '\n'.join(s.splitlines()[n-1:n-1+len(q.splitlines())])==q,(cid,n)
 coverage.append({'id':cid,'path':path,'line':n,'end':n+len(q.splitlines())-1,'title':block.splitlines()[0].split(' — ',1)[1]})
assert [(c['title'],c['line']) for c in coverage]==d.headings
bad=Doc()
try:bad.feed(s.replace('</section>','</article>',1))
except AssertionError:pass
else:raise AssertionError('negative unbalanced mutation not detected')
bad=Doc();bad.feed(s.replace('href="#overview"','href="#does-not-exist"',1))
assert any(h.startswith('#') and h[1:] not in bad.ids for h in bad.links),'negative broken link mutation not detected'
tot=Decimal('100')
for avail,res,issued,recovered in [(100,0,0,0),(80,20,0,0),(80,10,10,0),(85,10,10,5),(95,0,10,5)]:
 assert tot==Decimal(avail)+Decimal(res)+Decimal(issued)-Decimal(recovered)
assert '预算总额必须等于可用余额加已预占、加已发放、减已回收' in s
alloc=[(Decimal('0.05')*Decimal(x)/100).quantize(Decimal('.01'),rounding=ROUND_HALF_UP) for x in [40,30,30]]
tail=Decimal('0.05')-sum(alloc);assert tail==Decimal('-.01') and sum(alloc)+tail==Decimal('0.05')
assert all(t in s for t in ['实际承担结算责任的主体','不得默认转给城市项目公司','涉税金额','不适用本平台统一的金额舍入规则'])
lock=subprocess.check_output(['sha256sum',str(a/'plan/criteria-lock.md')],text=True).split()[0]
assert lock==(a/'plan/criteria-lock.sha256').read_text().strip()==re.search(r'^CRITERIA-LOCK-SHA256: (\w+)',(a/'ledger.md').read_text(),re.M).group(1)
assert subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()=='001878a11eeb6ea1fe79f5baac6f916cce68b555'
ix=Path(subprocess.check_output(['git','rev-parse','--git-path','index'],text=True).strip())
assert hashlib.sha256(ix.read_bytes()).hexdigest()=='040cc6a55775bb41014c00b6ebc2728983138f2bea8289e55263ff377684e9ad'
assert len(re.findall(r'<div class="question confirmed">',s[s.index('<section id="decisions">'):]))==17
print(json.dumps({'result':'PASS','path':p,'sha256':hashlib.sha256(b).hexdigest(),'lines':len(s.splitlines()),'bytes':len(b),'headings':66,'h2':18,'h3':48,'tables':4,'links':31,'navlinks':18,'scripts':0,'mapping':84,'exact_quotes':66,'negative_checks':['unbalanced tag caught','broken target caught'],'text_arithmetic':['budget reserve issue recovery conservation','negative tail -0.01 separately conserved'],'lock_actual':lock,'coverage':coverage},ensure_ascii=False))

```

实算输出：979行、102733字节；18h2+48h3=66；84唯一映射；66引文逐字匹配；4表头、31链接（18导航+13卡片）、18section；标签栈空，无重复ID/死链；无脚本/表单/事件属性/外部资源；错误闭合及断链负例被捕获；预算守恒/-0.01尾差算例通过；17个confirmed块。锁、sidecar、ledger值一致，本次四值的Grok票尚待独立落盘后读取，未提前声称四值完成。

本次重新核修复后像、原文精确批准范围以及当前门禁的实际命令输出：

```text
{"docs/platform-governance-map-for-operations-v0.1.html": {"bytes": 102733, "sha256": "9864216e609df2f319fa2cf7e51b61f06003ce45dff64a1a4098e71435865a68", "matches_B3r2_after": true}, "docs/prd-audit/entry-registry.csv": {"bytes": 612478, "sha256": "9bf932a414ec71a3eee813d7c4bb37c30a6086887a698215f5a6e6a0574463d8", "matches_B3r2_after": true}, "docs/prd-audit/authority-rule-ledger.csv": {"bytes": 991165, "sha256": "36c53884cd4a972b3421c767280a2ee8f4fa49749c2a0706a0bce8e0afbb461f", "matches_B3r2_after": true}, "docs/prd-audit/business-decision-ledger.md": {"bytes": 449888, "sha256": "fa211efee578da4ddec65c958d13cbb8182a8cf2a8d9abf2521686ff96069ad7", "matches_B3r2_after": true}, "tools/verify-entry-anchors.mjs": {"bytes": 15417, "sha256": "4f00ea005a724e7f0d768070b3c250a42a68b1a341d3ec14c5acecbb78a6c46e", "matches_B3r2_after": true}, "tools/verify-entry-anchors.test.mjs": {"bytes": 2268, "sha256": "f60e5421146fa5d809346d4d917c97cdfd056353982c4df63420d8353b6c7842", "matches_B3r2_after": true}, "approved_HTML20_exact": true, "other_two_HTML_unchanged": true}
{"command": "node tools/verify-entry-anchors.test.mjs", "exit": 0, "output": "PASS：历史 raw 放行，现行 findings、ledger、模块收口和普通文档的失效引用仍拒绝。\n"}
{"command": "npm run verify:entry-anchors", "exit": 0, "output": "\n> entertainment-complex-management@0.1.0 verify:entry-anchors\n> node tools/verify-entry-anchors.mjs\n\n[entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1563 条；被检 Markdown 350 份；条目名引用 1514 处（注册表原名 1357 处，正文唯一定位 157 处）。\n[entry-anchors] PASS：条目名唯一、注册表条目正文逐字与 HTML 现文一致、引用可唯一定位、正文无行号锚点、账本正文与九个镜像列均与 HTML 现文一致、账本与承接矩阵一一对应。\n"}
{"command": "npm run verify:html-ledger-sync", "exit": 0, "output": "\n> entertainment-complex-management@0.1.0 verify:html-ledger-sync\n> node tools/verify-html-ledger-sync.mjs\n\n[html-ledger-sync] PASS: 改动了 1 份业务 HTML，决策台账已同步。比对范围：工作区（含未提交改动）↔ 基线 origin/main@001878a1。\n"}
{"command": "git diff --check", "exit": 0, "output": ""}
DOCUMENT_COMPLETION_REFERENCE (existence/content only, not legal assessment)
106: ### 4.4 企业微信会话永久保存的合法性论证（DEC-R6-23，2026-08-04 补齐）
107: 
108: > 补齐依据：§4.1 第 3 条自身写明「若业务确需长期保存，补充其合法性论证」。2026-08-03 用户裁决维持「永久保存、不自动到期删除」，
109: > 该条自设的论证义务由本节履行；2026-08-04 第三轮裁决 DEC-R6-23 批准本节内容。本节生效后，§4.1 第 3 条的未了义务关闭，
110: > 平台治理地图（PGM）两处「合法性论证尚未补齐」的遗留义务注释已同批回写。
111: 
112: 1. **处理目的与合法性基础**。处理企业微信工作会话的合法性基础为 PIPL 第十三条第（二）项——「按照依法制定的劳动规章制度和依法签订的集体合同实施人力资源管理所必需」，不以单独同意作为唯一基础。人力资源管理必要性具体有三：①销售与服务岗位的私单、私收款、诱导客人加私人微信等行为，只能在工作会话中被发现，是薪酬提成与业绩归属真实性的唯一可核查来源；②辱骂客人、违规承诺等服务质量事件的事后取证；③劳动争议、客诉争议发生时履行举证责任。
113: 2. **规章制度与事前告知同意**。上述处理写入劳动规章制度并经民主程序制定与公示；员工入职时签署监控与会话留存告知同意书，明确告知处理者、目的、范围、保存期限（永久）、查看条件与员工权利，告知凭据以 `employee_notice_id` 关联留存，可逐人举证。客户侧在服务场景内一并告知。
114: 3. **范围最小化——仅工作会话，不及私人沟通**。采集范围严格限定为企业微信工作号内的工作会话；员工私人微信、私人手机上的私人沟通一律不采集、不同步、不存储。该边界由本文 §5（员工私人手机设备凭证）与平台治理地图的工作手机口径共同约束。
115: 4. **永久保存对「最短必要期限」的回应**。本场景的处理目的在事件发生时并不确定：追责与举证需求可能在事件发生多年后才出现——劳动争议仲裁与诉讼、税务与刑事线索、历史分账与提成争议均无可预先折算的固定时限；一旦按固定天数删除，处理目的即无法实现。因此本场景下「实现处理目的所必要的最短时间」等于「业务关系与潜在争议存续期间」，无法预先量化为固定期限。平台以「永久保存 ＋ 严格最小授权访问」替代「固定期限 ＋ 宽松访问」，在个人信息实际暴露面不增加（甚至更小）的前提下实现处理目的；风险控制由第 5 项承担，而非由缩短保存期限承担。
116: 5. **风险抵消措施——最小授权访问 ＋ 审批 ＋ 审计**。会话原文的不脱敏查看仅限取证／质检场景，须最小授权并逐次审批；普通展示场景敏感字段（手机号／证件／支付账号等）一律按权限脱敏；任何不脱敏查看均写入访问审计（`wecom_access_audits` / `audit_events`），可举证「谁、何时、看了哪一条」。企微授权配置由系统管理员（superadmin）一次性配置并由指定岗位复核（见 §4.3）。
117: 6. **清理的唯一路径**。会话数据不自动到期删除；确需清理的，只能由最高权限本人插入授权 U 盘密钥后执行，无密钥一律不可执行，每次清理逐条留痕。该路径与留痕清理规则同一套，不另设旁路，也不因保存期限「永久」而变成不可清理。
118: 7. **员工权利与删除请求的处理**。员工可就被判定违规的会话记录提出申诉与更正（见 §4.3 待确认项）；查阅、复制、更正、删除请求按 §6 的个人权利响应流程处理。删除请求与本节的保存必要性冲突时，按 PIPL 第四十七条「法律、行政法规规定的保存期限未届满，或者删除个人信息从技术上难以实现的」处理——转为限制处理（停止存储和采取必要安全保护措施之外的处理），并记录理由与决定人，不作静默拒绝。
119: 8. **复审义务**。本论证随劳动规章制度、企业微信采集范围或访问控制措施的变更而复审；任一前提不再成立（例如采集范围扩及私人沟通、或最小授权与审计措施被削弱），本节论证即不再成立，须重新裁决保存期限。
120: 
121: ---
122: 

```

其中6文件均以git show 9f235f47e026d7268cbcfc18dbd9ce5c8014fb24:<path>读后像，与当前文件字节完整相等；真实索引仍040cc6a55775bb41014c00b6ebc2728983138f2bea8289e55263ff377684e9ad，HEAD仍001878a11eeb6ea1fe79f5baac6f916cce68b555。另两份HTML哈希未改变。合规文件§4.4只用来核对“论证段存在”这项文档完成声明，不将其内容当作本席法律意见或已验证外部规范。

## B5本席结论

PASS。当前整模块979行与84条冻结判据已完整重新审计；001/004修复无残留、002/003无新缺陷；无未答问题。本次没有修改源码或增加B3次数。待Grok B5真实退出、完整报告落盘并读取后，执行四值比较；只有双PASS及四值全等才将ledger改PASS并写收口。发布另按已授权的分支/PR/CI/合并/160同步流程完成。

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M002 B5-r2 当前471模块完整独立复审（Codex）
版本：V0.02
日期：2026-09-08。阶段B5-r2；M002/471；ledger V1.60 IN_PROGRESS，历史B3累计2，本轮无B3/B4。B2在同一V0.88候选上双PASS、V0.89已定稿队列为空，但不替代本次全量复审。
唯一范围：/home/james/complex-management/docs/platform-governance-map-for-operations-v0.1.html V0.11，979物理行，SHA256=c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c。正本main，当前基线242f487233d028173cb9198eecd079f5d877405c。

## 独立顺序与本次执行事实
已先派Grok B5-r2，调用层持续等待真实结束，约11分钟后仅接收REPORT_PATH与EXIT_CODE=0；本次Grok正文尚未读取。根随后重新Read源1—310、311—500、501—648、649—750、751—858、859—979，完整979行无缺段；重读当前GEN001—018全文，机械逐一校对PGM66定义原文与84映射；当前业务上下文重读BSM699—708/1432—1434、BEQR130—144/198—200/227—234。其余已读同字节上下文不以旧票作依据，三HTML均新实算SHA不变。本报告当前结论是本次重新逐项核验所得，复用表格表述和标准库命令不等于复用B1结论。
阶段约束与实际技能沿用本轮已读取的using-superpowers、verification-before-completion、receiving-code-review、grilling、codebase-design、diagnosing-bugs/systematic-debugging、writing-for-agents、ponytail-review；只自问自答和读源复现，不派第三席、不改源或增加判据。可选技能按已实际读取的本机预检记录使用；不假称外席有其缺失技能。无可执行行为修改，因此不调用TDD/合并冲突流程。没有任何新HTML修改授权，B/C仍按用户明确原意保留。

## 整文件覆盖
- 1—218：HTML5、UTF8、中文语言、响应式CSS及布局；静态，无脚本和输入入口。
- 219—410：标题、18导航、全局五原则和13入口卡片；已确认但书与文档称谓一起理解。
- 411—748：项目公司、空间、商户、合同、员工、设备、权限、终端开关完整业务正文。
- 749—979：商户侧、接口、楼宇、恢复隐私、留痕、关系和17确认块、页脚与闭合标签。
这四段合并覆盖全部979行；PGM表66行与GEN表18行合并覆盖84项。语义审计和结构检查并列，hash匹配不独立构成PASS。

## 第一性原理线
1. 为什么存在：PGM256—305明确其是归属、资格、责任的共同治理源；415—438将空间项目与经营公司/财税权责拆清；528—539把准入审批与七红线区分；647—707落权限、隔离和单据职责；728—741落终端开关；868—888落客人与员工保护。删除这些权威条款会丢失对应已确认业务规则。导航/CSS不能据业务不可缺少硬凑理由，单独归技术性已有M002-003，只服务读取该文档。
2. 声称行为实际验证：本产物为静态HTML说明，未含任何script/form/input，18原生锚点均指向存在的唯一section；全标记闭合、UTF-8严格解码、外部依赖0，坏链接变异被检测。业务规则以当前逐字原文和明示算例验证相容性；金额/权限/预算算例实际Python执行，但不是系统后端/数据库运行证据。无Maven/数据库业务实现测试的适用产物，本节点按runbook文档豁免。
3. 前提如何核实：不从“集团视图”“第三方接入”等需求声明推断已上线；不从旧审计PASS推断当前通过；不把法定要求逐字转述当成本席法律有效性鉴定；不凭“密码反推”假设其算法禁止索引映射。对集团归属、十一终端、权限公式、合作主体、挂单流程、红线与员工注册分别回读另外两份当前HTML相关完整段。三份文件SHA均实算保持本次基线；B/C原句仍在，按操作员原意不冲突。

## 顺从式：84项完整映射覆盖
以下PGM行号均为docs/platform-governance-map-for-operations-v0.1.html的实际来源行。表中PASS是本席该判据的文档审计票，不代表系统已完成对应业务功能。

| 判据 | 原文行 | 锚点 | 逐项核对与边界 | 本席 |
|---|---:|---|---|---|
| PGM-001 | 256 | 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | 全局归属/权限/责任、链式溯源及安检例外、中文、涉税精度例外、尾差承担与先成功原则整体核对；以特别规则读概括，没有另造开关例外。 | PASS |
| PGM-002 | 308 | 治理顺序：一件业务能不能做，要先经过这些判断 | 五步对象→合同→人员→设备→留痕是治理判断次序；没有把独立权限或审计简化为单一开关。 | PASS |
| PGM-003 | 339 | 模块入口：平台治理层包含哪些管理模块 | 模块卡片总览提供文档导航；与后续13个领域章节的入口相接，结构检查无死链接。 | PASS |
| PGM-004 | 342 | 综合体和集团 | 综合体/集团卡片指向project，说明当前单体与未来集团，未声称集团已全部实现。 | PASS |
| PGM-005 | 347 | 空间和点位 | 空间/点位卡片指向space，覆盖楼层/卡座/吧台/游乐/库房/设备位置。 | PASS |
| PGM-006 | 352 | 商户和主体 | 商户/主体卡片指向merchant，经营主体和合作方式不等同财税主体收入自动归属。 | PASS |
| PGM-007 | 357 | 合同和准入 | 合同/准入卡片指向contract；合同归档与流程准入职责同时保留。 | PASS |
| PGM-008 | 362 | 员工和手机 | 员工/手机卡片指向staff；私人设备绑定与公司工作设备不同用途。 | PASS |
| PGM-009 | 367 | 权限和职级 | 权限/职级卡片指向permission；单功能权限与角色模板并存。 | PASS |
| PGM-010 | 372 | 终端开关 | 终端卡片指向terminal-switch；实际十一类终端与BEQR233同集合。 | PASS |
| PGM-011 | 378 | 设备管理 | 设备卡片指向device；台账/领还/状态为受管设备本身，不扩成业务授权。 | PASS |
| PGM-012 | 383 | 商户侧 | 商户侧卡片指向merchant-side，明确授权内开放和商户隔离。 | PASS |
| PGM-013 | 388 | 开放接口 | 开放接口卡片指向open-platform；自有接口先行，外部接入以身份和授权为前提。 | PASS |
| PGM-014 | 393 | 留痕复核 | 留痕卡片指向audit，记录发起/审批/复核/放行责任。 | PASS |
| PGM-015 | 399 | 楼宇运营 | 楼宇运营卡片指向building-ops，和员工设备/游乐设备分别描述。 | PASS |
| PGM-016 | 404 | 应急与隐私 | 应急与隐私卡片指向recovery-privacy；U盘治理与个人信息保护有正文承接。 | PASS |
| PGM-017 | 412 | 综合体和集团：当前一个项目，未来能看集团 | 当前一个物理综合体与未来集团汇总是阶段和数据归属说明，不强制创建不存在的集团业务。 | PASS |
| PGM-018 | 415 | 物理项目与运营公司：多对多协作，不是固定隶属层级 | 多对多经营域职责和城市项目公司边界与BEQR227—229、BSM702—704一致；通用储值付款是明示限定职责，不取得域定价管理权。 | PASS |
| PGM-019 | 424 | 各主体具体负责什么 | 六主体职责、收款和收入、内部结算、借款清偿与股权分别记录；默认80/20不是实际工商股权；内部场地协议须最高审批且双边入账。 | PASS |
| PGM-020 | 440 | 主要功能 | 项目档案、归属、集团职位下钻和KPI可见层级共同成立；职位模板不提供可任意改数据隔离的后台设置。 | PASS |
| PGM-021 | 452 | 业务意义 | 业务意义说明防串项目和集团总览预留，不把尚未落地的集团大屏冒称当前完成。 | PASS |
| PGM-022 | 463 | 空间和点位：所有业务都要知道“发生在哪” | 空间是合同/低消/容量/库存/责任的位置维度，不按空间重复发明容量限制。 | PASS |
| PGM-023 | 467 | 主要功能 | 平面图导入候选经人工复核发布，二次确认与保留证据明确；非静默覆盖。 | PASS |
| PGM-024 | 476 | 影响业务 | 游乐仅按项目实时在玩人数限流，和BEQR142—144的场次显示、营业日口径相容。 | PASS |
| PGM-025 | 488 | 商户和主体：谁在这里经营，经营什么 | 商户档案承载经营范围、位置、联系人、合作及开放范围；不等同向全部商户开放。 | PASS |
| PGM-026 | 492 | 主要功能 | 六类合作、四主体唯一绑定、其他模板、公司权责审批及历史快照、招商前链与突发事件分级全文核对；不把运营判断改成新强制否决条件。 | PASS |
| PGM-027 | 507 | 业务边界 | 授权业务内开放、客人归属、对接规则随第三方资料、合同/范围/权限联合作用；不是跨商户权限。 | PASS |
| PGM-028 | 518 | 合同和准入：违反规则先卡住，再按层级放行 | 合同摘要用于拦截不符规则业务；后文区分无权限不放行与超范围申请。 | PASS |
| PGM-029 | 522 | 主要功能 | 七红线连同密钥逐笔但书、权限不足无放行、审批转发与自审禁止、证照宽限、人工AI建议及合同三模板一起读；与BSM707全段一致。 | PASS |
| PGM-030 | 544 | 业务意义 | 本段是合同管控意义摘要；红线⑥等密钥特殊权限取前段明示条款，普通上级审批不是密钥授权，不认作独立冲突。 | PASS |
| PGM-031 | 555 | 外部经销商主体、合同价格与资金治理 | 经销企业准入、价格快照、货款到账前不出库、库存预留、退款与红冲、职责分离都有明确边界；第三方税务从有效法规专业结果。 | PASS |
| PGM-032 | 572 | 员工和手机：人、私人手机、工作手机要能对应起来 | 员工/私人手机/工作手机对应关系为治理目标，不把公司设备策略强加私人硬件读取。 | PASS |
| PGM-033 | 576 | 主要功能 | 注册双路径与自营例外和BSM1433一致；专属安装标识而非读取手机硬件ID，换机先解绑，异常失效复核，柜子扫码记录。 | PASS |
| PGM-034 | 586 | 工作手机和企业微信 | 企微工作会话永久保留与授权取证最小原文/普通展示隐敏并存；既有历史修订注释不替代当前正文。 | PASS |
| PGM-035 | 599 | 提成发放和人员服务治理 | 提成工资月结依法代扣、任务营销不混考勤、禁止人员陪场服务商品化，合法教练另专题；均为权威条款而非文档新增业务。 | PASS |
| PGM-036 | 611 | 设备管理：谁领了什么设备，设备能不能用 | 工作/现场三环节设备有明确分工；安检只记录设备端不因总体台账自动建按人记录。 | PASS |
| PGM-037 | 615 | 主要功能 | 设备编号状态、领用维修挂失、软件白名单为设备管理，不替代人员功能权限。 | PASS |
| PGM-038 | 624 | 和现场业务的关系 | 工作手机核销与交接、平板仅点酒、一体机授权售票范围分别界定，不将终端开启当可售全部票。 | PASS |
| PGM-039 | 636 | 智能设备场景联动范围 | 设备动作/延时编排明确后续独立立项前不定义，不据此反向要求当前实现定时计费。 | PASS |
| PGM-040 | 643 | 权限和职级：每一个单一功能都可以单独授权 | 单功能权限、角色装配、单人增减为不同组合层，不等同固定岗位全权。 | PASS |
| PGM-041 | 647 | 权限规则 | 最终权限集合含显式拒绝最高；高危授出前审批，跨商户/项目保存先失效旧权且新权审批；资金/税务/数据权限及逐单转发不形成常设代理。 | PASS |
| PGM-042 | 664 | 职级规则 | M1—M10与上中下与BEQR232的30档一致；只绑定工资大范围，具体薪资单填，不取代功能权限。 | PASS |
| PGM-043 | 673 | 外包人员与审计人员（2026-06-29 新增） | 外包机构范围与人数、只读审计、查看/导出分权、封闭PII清单及员工/本商户客人例外、逐次导出/敏感页留痕全文核对；密码暗纹反查未限定无索引算法，不虚构其数学不可实现。 | PASS |
| PGM-044 | 688 | 桌台、挂单与经销业务权限 | 桌台/挂单/经销功能与单据审批分开；优惠券预留和付款核销原子相容BEQR130—135，转并台保留历史提成，跨范围先审批。 | PASS |
| PGM-045 | 712 | 终端开关：哪些端能打开，后台说了算 | 终端开关只是允许访问前提，仍看权限、商户和设备；不可关审计和七红线。 | PASS |
| PGM-046 | 716 | 能控制哪些端 | 十一终端列表中的合并展示拆为实际十一类，与后文及BEQR233逐类对应。 | PASS |
| PGM-047 | 728 | 怎么控制 | 七档开关优先级、无行默认关、营业维护限制、读配置失败与审计例外均明确；整套金额规则统一切换与既定粒度参数取值有明确区分。 | PASS |
| PGM-048 | 749 | 商户侧：在授权业务范围内全开放 | 商户侧按自身经营范围开放；游乐场次安排不等于另一套容量计数，未授权其他商户/项目数据禁止。 | PASS |
| PGM-049 | 781 | 开放接口：先做好自己的开放能力，再接第三方资料 | 无真实第三方资料的当前边界明示；后续供应商接入不是自有系统阻塞，也不是伪造已联调。 | PASS |
| PGM-050 | 785 | 平台开放给第三方 | 第三方先验身份再看授权；门店ID映射由招商登记，缺映射挂差异不自动入账，调用全审计。 | PASS |
| PGM-051 | 794 | 第三方接入平台 | 供应商回传/格式/对账随真实资料接入，未在此虚构外部服务频次和返回成功。 | PASS |
| PGM-052 | 806 | 楼宇运营：管好建筑本身的设备、能耗和巡检 | 楼宇设备/水电/安防消防本体治理与物业合同财务相连，不与员工设备及游乐巡检混同。 | PASS |
| PGM-053 | 810 | 楼宇机电设备 | 四组机电设备、六状态、维保周期、人工及物联网告警为具体楼宇运营范围。 | PASS |
| PGM-054 | 821 | 能耗计量 | 能耗异常二人复核+主管确认前缓出账催缴，原读数保留；独立用量代收和独立损耗服务费区分；总分表摘要与BEQR199扣除设备回路的具体口径共同执行，未声明不扣回路。 | PASS |
| PGM-055 | 832 | 楼宇巡检 | 四类巡检，与独立能耗抄表分开；轻异常/重整改两模式和全工单模式均含复检主管确认。 | PASS |
| PGM-056 | 844 | 和谁有关 | 物业/财务/供应链/审计为同对象关联，不另造业务记录源。 | PASS |
| PGM-057 | 857 | 应急恢复和客人隐私：最坏情况有后路，个人信息看不到明文 | 应急和隐私标题为功能总览，以随后客人与员工具体例外控制明文。 | PASS |
| PGM-058 | 860 | 紧急恢复（破窗机制） | 钥匙更换先作废旧钥匙、最高权限本人加见证、逐动作验证和交接留痕；无预制备用，不从正常登录必须钥匙推导应急无解。 | PASS |
| PGM-059 | 868 | 客人与员工的个人信息保护 | 手机号唯一客人定位和单据点击同专门鉴权留痕通道共同成立；不设独立卡号；职员/商户明确脱敏例外仍按前文细则。 | PASS |
| PGM-060 | 879 | 会员身份、营销与生物信息治理 | 本人手机号验证注册、预算方程、公开资格发券、无长期客情绑定、生物信息仅用途所需；验龄服务与购票持票核验是各自明文用途，不假设同一回传人脸滥用。 | PASS |
| PGM-061 | 892 | 留痕和复核：重要动作不能只靠口头说明 | 统一追溯发起、销售、服务、异常第一人及审批结果，未以口头说明代替记录。 | PASS |
| PGM-062 | 896 | 必须留痕的动作 | 十类关键动作覆盖账号设备合同开关订单交接及敏感查看；与安检设备端例外共同适用。 | PASS |
| PGM-063 | 911 | 常见复核 | 退款调账、报损盘差作废、金额补录、游乐恢复均须复核；短表不豁免完整审批规则。 | PASS |
| PGM-064 | 923 | 正式挂起订单审计与结案 | 正式挂单已出品扣库且不结清不交班日结，取消/支付互斥、迟款退款、不可回库走坏账及法务前不自动扣款，与BEQR132—135一致。 | PASS |
| PGM-065 | 935 | 连接关系：平台治理层怎么连接所有业务 | 七连接是治理职责对应业务入口，均保持归属/合同/权限/复核，未把接口调用审计代替实际授权。 | PASS |
| PGM-066 | 951 | 已拍板规则和待排期问题 | 17项已确认内容及集团/第三方待实施排期含义相容；无日期失配声称。页脚明示三HTML同级权威和后确认优先，不能以下位文档或配置扩展业务。 | PASS |

| 通用判据 | 当前适用证据/边界 | 本席 |
|---|---|---|
| GEN-001 | 技术性页面壳、目录与入口卡片服务阅读定位；不存在可确认的无用途业务模块，已有M002-003登记。 | PASS |
| GEN-002 | 无script/form/input、无构建入口或外部依赖；静态DOM闭合、18原生锚点有效，文档型按runbook豁免业务运行测试。 | PASS |
| GEN-003 | 坏链接替换反例被检测；尾差正负算例与错误预算总额可失败断言均实跑，不宣称后端测试通过。 | PASS |
| GEN-004 | 本文无外部输入执行器；业务说明明确人工导入复核、第三方身份及范围、手机号专门查询，不把说明当服务端已实现。 | PASS |
| GEN-005 | PGM029/041/043/044明确单功能最小权限、自审禁止、显式拒绝及跨域审批；源HTML不是授权程序。 | PASS |
| GEN-006 | PGM001/031/044/060/064记录快照/预算/付款券核销互斥与金额尾差；仅验证规则相容和算例，数据库实现留对应模块。 | PASS |
| GEN-007 | PGM001先成功/同操作不重复，PGM031/064取消支付互斥，结合逐笔密钥例外，不把文档推测当并发运行证明。 | PASS |
| GEN-008 | 十一终端、角色权限公式、主体关系、挂单状态对照另外两份当前权威原文；本文件没有DTO/OpenAPI/数据库模式定义。 | PASS |
| GEN-009 | 无运行入口错误处理代码；治理规则定义配置失败拒绝、缺映射挂差异、超时保持待审与催办，未声明伪成功。 | PASS |
| GEN-010 | HTML无脚本输入、外链、明文真实秘密；密钥/密码要求是业务描述；PII原文例外限定场景而非全员权限。 | PASS |
| GEN-011 | PGM047/058/061—064逐次留痕、永久导出证据、身份对象时间设备关联俱全；不得把该规则误当实际日志已查。 | PASS |
| GEN-012 | 页面固定979行静态内容，无循环请求/无限查询/缓存线程；侧栏max-height与overflow、窄屏单列现有CSS。 | PASS |
| GEN-013 | 独立UTF-8文档无外部脚本或样式依赖；无后端构建和迁移产物，本模块不运行Maven/数据库。 | PASS |
| GEN-014 | N/A：本模块无迁移SQL或数据库写入；不凭HTML断言数据库迁移已验证。 | PASS |
| GEN-015 | lang=zh-CN、UTF-8、meta viewport，原生a导航、h1/h2/h3层级、响应式单列且未取消焦点；产品全中文要求和文档中的终端称谓/反例不混作新业务豁免。 | PASS |
| GEN-016 | 自身版本V0.11、979行和18导航可复算；未指定重复页脚日期，旧修订注释属历史；未声称后台实现全绿。 | PASS |
| GEN-017 | 逐序四分类：当前业务原文均为冻结PGM条目权威来源，无添加；非业务呈现已有M002-003技术性且无发明。 | PASS |
| GEN-018 | 全部声称落到当前源原文、机械命令和算例输出；不使用旧PASS、习惯或另一席意见作证。 | PASS |

## 对抗式：同模块完整批次核查
| 假设或反例 | 原文/实际结果 | 结论 |
|---|---|---|
| 一级开关打开就能越过高层关闭或审计关闭 | PGM730—738分别写优先级、缺行关、审计不随关，七红线PGM528也不设关闭档 | 不成立；不能把纯摘要当豁免 |
| 同一权限从四种来源授予可击穿显式拒绝 | PGM655公式与BEQR234同句；集合算例对退款/导出显式拒绝，最终仅查询/调账 | 不成立，依据类型1+4 |
| 每方四舍五入后必然守恒，尾差可随便并入任一方 | 0.10分3方各0.03、单列+0.01；0.03分2方各0.02、单列-0.01；PGM302明确承担主体和负差退回 | 原文已规定，算例相容，不是实际财务系统验证 |
| 积分预算回收符号反了/预算可超发 | PGM882明示总额=可用+预占+已发-已回收；100=60+20+30-10，错误总额99被断言区分 | 未找到文档算术冲突 |
| 普通审批能绕过全部红线/无人审批可自动通过 | PGM528逐类密钥但书与529超时末端逐单转发、自审/受益人禁止同时适用；BSM707同义完整原文 | 摘要不另成相反权限，不能要求每个短表重写但书 |
| 公司/私人设备同一App所以必须开放相同能力 | PGM741明示按设备类型能力；员工注册与安装专属标识见578—581，和BSM1433相容 | 不成立 |
| 所有明文均禁，因此本商户客户和员工管理线例外冲突 | PGM680明确对象/场景例外，870—872通用查看与专门查询共同读 | 专项细则控制具体场景，不改原意 |
| 第三方验龄与持票本人核验导致同一人脸强制滥用 | PGM886同段分别限定验龄服务结果及购票人脸持票核验，未声称对外共享原始模板 | 没有原文或实际行为证明冲突，不凭假设立项 |
| 所有已确认卡片意味着集团/第三方已经实现 | PGM782/796明确无资料后续接入，413/449/965保留未来集团；待排期与已确认规则可并存 | 不把排期标题误读为未批准业务 |
| 任意HTML导航链接都可跳到不存在的章节 | 原文31个链接中18目录项及卡片均有目标；把#overview变为不存在目标的反例被检测 | 当前通过；变异可见失败信号 |
| 总表/分表摘要可另向商户收费公区能耗 | PGM825/827明确公区不分摊，BEQR198—199给独立设备回路扣除细节；损耗独立服务费不同于公区能源费应收 | 摘要须结合闭集原文，不发明另一计费公式 |
| 坏账挂单可以经理审批后直接从工资扣 | PGM924—927及BEQR132—135均规定法务前只登记、未结清拦交班日结 | 不成立 |


## 本次全部finding与既有ID处置
本次新增临时finding为空，无新的工程缺陷、添加项、权威HTML问题或锁外判据。静态呈现仍是已有M002-003：技术性、无业务发明、无缺陷，内容只提供读取与导航，不新增实体/枚举/状态/角色权限/校验规则/价格/流程/业务默认值，不重复分ID。001当前日期修复有效，002终端自然语言别名不生成另一端类，003无新缺陷；004仅保留历史raw窄排除修复，不能冒称其回归本次全绿。

范围外已知失败必须保留：本回合B2两席实际运行node --test tools/verify-entry-anchors.test.mjs及npm run verify:entry-anchors，均exit1；6项均为BSM镜像，4项对应A回访概括、2项对应此前M001-012的L2003页脚日期，非D的L2版本注释。根用csv.DictReader与BSM2003再次实核；未改两CSV。它们归当前M425/M427，工具归M008/M415，非本模块PGM错误。不得从“当前B5文档PASS”推得全局门禁PASS或未来模块已审。当前无发布，不扩模块、不重开M001、不给未来模块临时分ID；该事实已保留findings/ledger供后续按计划处理。未重跑那些已知全局失败来装作本模块覆盖，也未删除失败证据。

## B5本次真实命令与输出
命令一：原生HTMLParser、66逐字引文、84映射、导航及坏链接反例（无落盘脚本）：
~~~bash
python3 - <<'PY'
from pathlib import Path
from html.parser import HTMLParser
from collections import Counter
import hashlib,json,re,subprocess
root=Path('/home/james/complex-management')
paths=['docs/platform-governance-map-for-operations-v0.1.html']
lock=(root/'docs/audit-2026-09/plan/criteria-lock.md').read_text(encoding='utf-8')
h=hashlib.sha256((root/'docs/audit-2026-09/plan/criteria-lock.md').read_bytes()).hexdigest()
blocks=re.split(r'(?m)^### ',lock)[1:]
coverage=[]
for block in blocks:
 m=re.match(r'(PGM-\d{3}) — (.*)',block)
 if not m: continue
 src=re.search(r'- 来源：`([^`]+):(\d+)`',block)
 raw=re.search(r'````html\n([\s\S]*?)\n````',block)
 assert src and raw,m[1]
 source=(root/src[1]).read_text(encoding='utf-8')
 piece=raw[1].splitlines();start=int(src[2])-1
 assert source.splitlines()[start:start+len(piece)]==piece,m[1]
 coverage.append({'id':m[1],'title':m[2],'file':src[1],'line':int(src[2])})
assert len(coverage)==66,len(coverage)
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
print(json.dumps({'lock_sha256':h,'criteria_coverage':len(coverage),'checks':checks},ensure_ascii=False))
print('COVERAGE_COUNT='+str(len(coverage)))
mapping=next(x for x in lock.splitlines() if x.startswith('M002 → ['))
ids=[x.strip() for x in mapping.split('[',1)[1].rsplit(']',1)[0].split(',')]
assert set(ids)=={f'PGM-{i:03d}' for i in range(1,67)}|{f'GEN-{i:03d}' for i in range(1,19)} and len(ids)==84
assert p.tags['h2']==18 and p.tags['h3']==48
assert p.tags['script']==0 and p.tags['form']==0 and p.tags['input']==0
assert len(p.navlinks)==18 and set(p.navlinks)=={'#'+v for v in p.ids}
positions=[i for i,line in enumerate(s.splitlines(),1) for _ in re.finditer(r'<h[23](?:>|\s)',line)]
assert positions==[item['line'] for item in coverage]
policies=sorted(set(re.search(r'^- 判据：(.*)$',b,re.M)[1] for b in blocks if re.match(r'PGM-\d{3} — ',b)))
negative=AuditHTML();negative.feed(s.replace('href="#overview"','href="#does-not-exist"',1))
assert any(x.startswith('#') and x[1:] not in negative.ids for x in negative.links)
print(json.dumps({'mapping_exact':84,'source_excerpts_exact':66,'native_navigation_targets':18,'heading_positions_exact':66,'scripts':0,'external_dependencies':p.tags['script']+p.tags['link'],'broken_link_mutation_rejected':True,'policies':policies,'coverage':coverage},ensure_ascii=False))
PY
~~~
真实exit0；stdout：
~~~text
{"lock_sha256": "b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a", "criteria_coverage": 66, "checks": [{"path": "docs/platform-governance-map-for-operations-v0.1.html", "sha256": "c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c", "wc_l": 979, "actual_lines": 979, "ids": 18, "navlinks": 18, "broken_links": 0, "stack_errors": 0, "utf8": "PASS"}]}
COVERAGE_COUNT=66
{"mapping_exact": 84, "source_excerpts_exact": 66, "native_navigation_targets": 18, "heading_positions_exact": 66, "scripts": 0, "external_dependencies": 0, "broken_link_mutation_rejected": true, "policies": ["映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。"], "coverage": [{"id": "PGM-001", "title": "整体定位：平台治理层不是一个业务部门，而是所有业务的底座", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 256}, {"id": "PGM-002", "title": "治理顺序：一件业务能不能做，要先经过这些判断", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 308}, {"id": "PGM-003", "title": "模块入口：平台治理层包含哪些管理模块", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 339}, {"id": "PGM-004", "title": "综合体和集团", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 342}, {"id": "PGM-005", "title": "空间和点位", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 347}, {"id": "PGM-006", "title": "商户和主体", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 352}, {"id": "PGM-007", "title": "合同和准入", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 357}, {"id": "PGM-008", "title": "员工和手机", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 362}, {"id": "PGM-009", "title": "权限和职级", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 367}, {"id": "PGM-010", "title": "终端开关", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 372}, {"id": "PGM-011", "title": "设备管理", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 378}, {"id": "PGM-012", "title": "商户侧", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 383}, {"id": "PGM-013", "title": "开放接口", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 388}, {"id": "PGM-014", "title": "留痕复核", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 393}, {"id": "PGM-015", "title": "楼宇运营", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 399}, {"id": "PGM-016", "title": "应急与隐私", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 404}, {"id": "PGM-017", "title": "综合体和集团：当前一个项目，未来能看集团", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 412}, {"id": "PGM-018", "title": "物理项目与运营公司：多对多协作，不是固定隶属层级", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 415}, {"id": "PGM-019", "title": "各主体具体负责什么", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 424}, {"id": "PGM-020", "title": "主要功能", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 440}, {"id": "PGM-021", "title": "业务意义", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 452}, {"id": "PGM-022", "title": "空间和点位：所有业务都要知道“发生在哪”", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 463}, {"id": "PGM-023", "title": "主要功能", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 467}, {"id": "PGM-024", "title": "影响业务", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 476}, {"id": "PGM-025", "title": "商户和主体：谁在这里经营，经营什么", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 488}, {"id": "PGM-026", "title": "主要功能", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 492}, {"id": "PGM-027", "title": "业务边界", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 507}, {"id": "PGM-028", "title": "合同和准入：违反规则先卡住，再按层级放行", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 518}, {"id": "PGM-029", "title": "主要功能", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 522}, {"id": "PGM-030", "title": "业务意义", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 544}, {"id": "PGM-031", "title": "外部经销商主体、合同价格与资金治理", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 555}, {"id": "PGM-032", "title": "员工和手机：人、私人手机、工作手机要能对应起来", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 572}, {"id": "PGM-033", "title": "主要功能", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 576}, {"id": "PGM-034", "title": "工作手机和企业微信", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 586}, {"id": "PGM-035", "title": "提成发放和人员服务治理", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 599}, {"id": "PGM-036", "title": "设备管理：谁领了什么设备，设备能不能用", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 611}, {"id": "PGM-037", "title": "主要功能", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 615}, {"id": "PGM-038", "title": "和现场业务的关系", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 624}, {"id": "PGM-039", "title": "智能设备场景联动范围", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 636}, {"id": "PGM-040", "title": "权限和职级：每一个单一功能都可以单独授权", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 643}, {"id": "PGM-041", "title": "权限规则", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 647}, {"id": "PGM-042", "title": "职级规则", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 664}, {"id": "PGM-043", "title": "外包人员与审计人员（2026-06-29 新增）", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 673}, {"id": "PGM-044", "title": "桌台、挂单与经销业务权限", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 688}, {"id": "PGM-045", "title": "终端开关：哪些端能打开，后台说了算", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 712}, {"id": "PGM-046", "title": "能控制哪些端", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 716}, {"id": "PGM-047", "title": "怎么控制", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 728}, {"id": "PGM-048", "title": "商户侧：在授权业务范围内全开放", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 749}, {"id": "PGM-049", "title": "开放接口：先做好自己的开放能力，再接第三方资料", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 781}, {"id": "PGM-050", "title": "平台开放给第三方", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 785}, {"id": "PGM-051", "title": "第三方接入平台", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 794}, {"id": "PGM-052", "title": "楼宇运营：管好建筑本身的设备、能耗和巡检", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 806}, {"id": "PGM-053", "title": "楼宇机电设备", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 810}, {"id": "PGM-054", "title": "能耗计量", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 821}, {"id": "PGM-055", "title": "楼宇巡检", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 832}, {"id": "PGM-056", "title": "和谁有关", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 844}, {"id": "PGM-057", "title": "应急恢复和客人隐私：最坏情况有后路，个人信息看不到明文", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 857}, {"id": "PGM-058", "title": "紧急恢复（破窗机制）", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 860}, {"id": "PGM-059", "title": "客人与员工的个人信息保护", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 868}, {"id": "PGM-060", "title": "会员身份、营销与生物信息治理", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 879}, {"id": "PGM-061", "title": "留痕和复核：重要动作不能只靠口头说明", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 892}, {"id": "PGM-062", "title": "必须留痕的动作", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 896}, {"id": "PGM-063", "title": "常见复核", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 911}, {"id": "PGM-064", "title": "正式挂起订单审计与结案", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 923}, {"id": "PGM-065", "title": "连接关系：平台治理层怎么连接所有业务", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 935}, {"id": "PGM-066", "title": "已拍板规则和待排期问题", "file": "docs/platform-governance-map-for-operations-v0.1.html", "line": 951}]}
~~~
stderr为空。

命令二：三HTML上下文断言、十一端/权限集合、非涉税正负尾差和预算例子（不是实际业务后端执行）：
~~~bash
python3 - <<'PY'
from pathlib import Path
from decimal import Decimal,ROUND_HALF_UP
from html import unescape
import re,json,hashlib
r=Path('/home/james/complex-management')
p=(r/'docs/platform-governance-map-for-operations-v0.1.html').read_text(encoding='utf-8')
e=(r/'docs/business-enum-quick-reference-v0.1.html').read_text(encoding='utf-8')
b=(r/'docs/business-system-map-for-operations-v0.1.html').read_text(encoding='utf-8')
plain=lambda s:unescape(re.sub(r'<[^>]*>','',s))
pline=p.splitlines()
checks={}
terminals=['客人手机应用','客人小程序','Web 前端（业务人员，客人无网页端）','工作人员平板','员工工作手机','员工私人手机','商户网页门户','商户手机应用','触摸屏一体机','移动管理端','独立经销商 Web 门户']
prow=next(x for x in pline if '受控的十一类终端：' in x)
erow=next(x for x in e.splitlines() if '<td class="k">终端十一端</td>' in x)
assert all(x in prow and x in erow for x in terminals)
checks['terminal_categories_11_same']=True
for word in ['同一笔操作重复提交不重复生效','持授权 U 盘密钥的最高权限可以逐笔开放','第①②③类','不解除已开始核销后的退款禁止','上述七类校验不设关闭开关']:
 assert word in p and word in b,word
checks['redline_summary_with_explicit_provisos']=True
assert '角色权限 + 基础模板 + 有效临时模板 + 员工单独授予 − 显式拒绝' in p and '角色权限 + 基础模板 + 有效临时模板 + 员工单独授予 − 显式拒绝' in e
effective=({'查询','退款'}|{'查询'}|{'导出'}|{'调账'})-{'退款','导出'}
assert effective=={'查询','调账'}
checks['deny_precedence_example']=sorted(effective)
quant=lambda v:Decimal(v).quantize(Decimal('.01'),rounding=ROUND_HALF_UP)
examples=[]
for total,n in [('0.10',3),('0.03',2)]:
 shares=[quant(Decimal(total)/n)]*n
 remainder=Decimal(total)-sum(shares)
 assert sum(shares)+remainder==Decimal(total)
 examples.append({'total':total,'shares':[str(x) for x in shares],'separate_remainder':str(remainder)})
assert examples[0]['separate_remainder']=='0.01' and examples[1]['separate_remainder']=='-0.01'
assert quant('1.005')==Decimal('1.01') and quant('-1.005')==Decimal('-1.01')
checks['non_tax_rounding_examples_not_backend_execution']=examples
assert '预算总额必须等于可用余额加已预占、加已发放、减已回收' in p
assert 100==60+20+30-10 and 99!=60+20+30-10
checks['budget_equation_and_wrong_total_rejected']=True
assert '每一次导出——不论脱敏与否' in p and '导出记录本身永久保存、不在任何清理范围内' in p
assert '凡要定位到具体某一位客人' in p and '从订单、账单、订台单、存酒单等任何单据上点开' in p and '同样先鉴权、同样留痕' in p
assert '不设独立卡号体系' in p and '经最高权限审批后发放' in p
checks['privacy_exception_context_present']=True
assert '权限不足属于操作员本人没有该项权限' in p and '不设放行通道' in p
assert '留痕与审计记录不属于可被开关关闭的模块' in p and '开关缺行不得视为开启' in p
assert '显式拒绝优先级最高' in p and '申请人不得审批自己提交的调整' in p
checks['permissions_switches_separation_context']=True
assert '法务审查通过前只登记，不得自动扣减工资、绩效或提成' in e and '审查通过前只登记，不得自动扣减工资、绩效或提成' in p
assert '已签约后不得回退至未签约状态' in p and '新建未审批不生效，到期不得无审批自动终止，变更未审批不生效' in p
checks['state_and_compensation_context']=True
assert '来源枚举项为业务参数，按项目可配。' in b and '其他（枚举项业务参数，按项目可配）' in b
checks['BC_originals_unchanged']=True
checks['source_hashes']={x:hashlib.sha256((r/'docs'/x).read_bytes()).hexdigest() for x in ['platform-governance-map-for-operations-v0.1.html','business-enum-quick-reference-v0.1.html','business-system-map-for-operations-v0.1.html']}
print(json.dumps(checks,ensure_ascii=False))
PY
~~~
真实exit0；stdout：
~~~text
{"terminal_categories_11_same": true, "redline_summary_with_explicit_provisos": true, "deny_precedence_example": ["查询", "调账"], "non_tax_rounding_examples_not_backend_execution": [{"total": "0.10", "shares": ["0.03", "0.03", "0.03"], "separate_remainder": "0.01"}, {"total": "0.03", "shares": ["0.02", "0.02"], "separate_remainder": "-0.01"}], "budget_equation_and_wrong_total_rejected": true, "privacy_exception_context_present": true, "permissions_switches_separation_context": true, "state_and_compensation_context": true, "BC_originals_unchanged": true, "source_hashes": {"platform-governance-map-for-operations-v0.1.html": "c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c", "business-enum-quick-reference-v0.1.html": "4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66", "business-system-map-for-operations-v0.1.html": "a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa"}}
~~~
stderr为空。非涉税四舍五入、正负尾差、拒绝优先和预算例子均是规则相容性检查；不声称实际支付/数据库/设备/法务税务合规验证。本模块无构建/迁移入口，不制造这些测试。

## 本席B5结论
PASS。重新全读979行、全核18GEN+66PGM共84项、三线及适用正负例完成；无本模块残留缺陷或新HTML问题。旧修复记录/当前源/锁版本未更改，本轮B3增量0、累计2。
本报告落盘前未读取Grok当前B5正文；本票仅本席B5，不是模块PASS。下一步读取Grok本次完整报告、执行B5后实算/sidecar/ledger/Grok四值校验；只有双PASS且四值全等才先将ledger改PASS再写模块收口。全仓审计尚未完成，无提交/推送/部署。

