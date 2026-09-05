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
