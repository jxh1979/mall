CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
# M003 B5-r2 本方整模块三线审计

版本：V0.01

时间：2026-09-05T11:50:45.671855+00:00

本方结论：PASS。完整逐行重新读取8文件621行、完整18GEN；全部三线审完，未发现新的确定缺陷或业务添加项，既有26ID无待修问题。本结论不是模块合取PASS：Grok当前结果未知，本方完整报告落盘前没有读对方本轮raw或其JSON。源码在B5只读，B3仍累计2；不提前写ledger PASS，不提前发布或启动M004。

## 本次完整输入

控制V0.17，SHA c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21；criteria-lock V0.06，COUNT0；findings V0.40；ledger V0.84 / IN_PROGRESS。M003映射18GEN；三HTML按GEN-017作业务上位，未扩大映射/新增判据。模块原始切分625行，当前实际621行。

| 文件 | 行数 | 字节 | SHA256 |
|---|---:|---:|---|
| .env.example | 22 | 893 | e377b54d9cd829ddeed30e0ae730f55d51fbc8c272f3df5ca611fdbdba125049 |
| .gitignore | 55 | 1199 | 75861ded5d3859bcda593aafe3f3862e765470e7f8b6636a643bf59de9d7c51b |
| .semgrepignore | 6 | 253 | 45f3dbc9f36891b4b94d2b641fabfd66db81df59b642bcde80a29bece24f1aba |
| AGENTS.md | 74 | 3695 | ea470f256c8e9f1e23e9b346d1b3828f2e4e1edd84e9ea83d7564725759bfcf0 |
| CONTEXT.md | 163 | 10197 | d5acdb1c3eb174c532983c823931d1cbe36444712e379b755991243f75df52b7 |
| PROGRESS.md | 36 | 3246 | 91eaedfc0c8428514355b82a79bd1b6add257bc80269e831d2e76aafbc0baf9d |
| README.en.md | 116 | 7470 | dd83a2943d401ec25bfa2043ef4762662c78d1526763eb3faf2cc20008d092db |
| README.md | 149 | 10854 | 28a46aea13227b0e3e101f9dc0a2970b6e3e581ca5995d291288aa59cb2d23b5 |

第一次批量显示超过工具总输出预算，读取结果未丢失；随后从已保存工具结果完整补读18GEN尾部和PROGRESS全文。没有把截断内容冒称已读。下面每文件覆盖均基于当前全部行，不只审9个diff块。

## 每文件三线与第一性三问

### .env.example:1-22

必要性：本地开发复制配置入口；删掉不直接使HTML业务不可实现，分类为工程运行配置，P0种子沿用HTML已有平台管理员身份，不创造生产权限。

顺从/第一性证据：1版本、2profile、3-4API网络、6-10DB及连接池、12-13构建元数据、15-16密钥安装缺省、18-22local种子逐行与application.yml/application-local.yml绑定核对。其消费者名字与当前实际变量相同；旧5个死变量与PII空赋值都不存在。bash -n实际0。

对抗/未核前提：示例口令在local profile、注释禁止生产；测试只用三DB变量和complex_test，不source整个env。真实Spring+当前LocalKeyVault的旧空值失败/未设local32字节/prod默认拒绝已经在本模块B4-r1实跑，本轮引用其同字节行为证据并下附原输出，未伪称重跑；没有启动整库或prod服务。本文件并不承诺穷尽所有可配变量，PII变量名可由注释直系yml查到，不新增“必须列全参数”门槛。

### .gitignore:1-55

必要性：排除构建、缓存、秘密和运行态文件；全55行是技术性，无新业务规则。

顺从/第一性证据：1-21 Node/Maven/Gradle/Android/env输出；23-39测试/会话/扫描日志；41-51工具临时文件；53-55本地图片/证据文件。当前git check-ignore实际命中env、node_modules、target及两运行目录；.env.example/AGENTS/CONTEXT未被忽略。

对抗/未核前提：没有整体忽略正式源或审计文档；滚动扫描日志排除不把dated findings JSON/SARIF/MD全部抹去；历史SQLite后缀不等于采用SQLite正式栈。未证明历史绝无误提交，不据此做历史秘密清洗；无新的工程缺陷。

### .semgrepignore:1-6

必要性：扫描输入边界/历史解析限制记录；全6行技术性，未定义业务权限或规则。

顺从/第一性：逐行读取OpenAPI yaml/json、docs/_archive、ops/keyvault清单，均指实际存在的生成物/归档/运维脚本；没有把全应用源码排除。

对抗/未核前提：不声称本轮重跑Semgrep或所有解析器均不能处理所有被排除文件；忽略策略本身不证明有漏洞，也没有冻结的“必须全部Semgrep”要求。原扫描论据争议已存018之前的017证据，当前没有新的实际工程失败依据；只登记技术配置，不以HTML沉默删除，也不扩成新扫描项目。

### AGENTS.md:1-74

必要性：代理协作与仓库工程入口；1-40/58-74技术规程，42-56涉及已有业务权威/删除/展示规则，先按HTML核对。

顺从/第一性：issue/triage/domain、propose/merge指针存在；代码修改worktree/禁止直推/fetch/PRD同步逐条阅读。当前控制明确B3就地主工作区审，提交仍走worktree；没有把当前用户授权反置于AGENTS之下。52-53配置主数据最高权限审批+个人信息注销例外对BSM567/570/862；55封闭四样对PGM287/965；56全端适用保留。63-67迁移gate确在delivery-gates、不在predeploy，69-74测试隔离与部署skipTests均为工程操作。

对抗/未核前提：平台管理员不是最高权限，不把种子账号赋予删除审批；不能把字段名/枚举值作为第五类可展示英文例外。67多一个缩进空格不改语义，不为喜好排版扩修；没有本轮执行gh创建PR或跑数据库迁移来证明指针；没有实际部署或生产CI豁免。当前无已证矛盾。

### CONTEXT.md:1-163

必要性：统一业务词表；必须服从三HTML，不能以PRD反向定义业务。

顺从/第一性：1-12解释权；14-45平台/项目/业态/商户按PGM413/415/417/418/420/428-433；47-69会员、成长、积分、待追回按BEQR65/66/70/75/78/82；71-87两价格和积分中心按BEQR54/75/80/85；89-93营销对BSM1725；95-121四绑定/功能/临时/高危对PGM497/644/652/655/656/659/660；123-135外协/审计对675-677；137-149开关和十一端对731/732/736/738/739；151-156留痕对同处治理规则；158-163配置主数据生命周期对BSM567/569/570/862。所有定义、边界、别名逐行检查；没有跳过未修段。

对抗/未核前提：119列层级不等于发起人自选并跳过中间链；652有层级、528/660明确最终人自动补链，词表没规定反向行为，因此不复活已撤回的假设界面反例。79/80“项目独立审批”是目录与规则隔离，未写城市项目公司代全域审批；严格按54对应域。免费注册/不设付费身份是66全部注册条件与自动有效身份的下位，不虚造订阅商品。73是取价词条摘要，不声称不存在新品首价分支，80已有完整规则；不能把未抄全当明文相反。024实际保留，1725确有多主体/登记赞助/分别锁预算；023L4已删。未跑整个会员/结算/权限后端，PASS仅本模块文本与上位一致，不等于业务实现通过。

### PROGRESS.md:1-36

必要性：历史分支建设记录，技术实现过程/既有退租业务细化；不是当前测试看板。

顺从/第一性：1-4历史标题/V0.01；5-15触发、快照、台账、幂等与当时零迁移；17-28具体变更点；30-36当时门禁和完成。35明确数字不代表当前测试结果；readme当前基线独立。当前所列直系源路径/settleTerminationIfNeeded触发已在本模块前轮实读，源未变化；本轮不把它们的测试数当实际运行。

对抗/未核前提：“当时未用V303”不约束未来迁移编号，“当时7/32/15绿”不被当前用例增加证伪；未声明本轮mvn/vitest全绿。无新结算计算执行/数据库写入；不为刷新历史台账伪造结果。

### README.en.md:1-116

必要性：国际开发者与供应商入口，技术性为主；产品定位与业务终端先按上位业务核对，开发英文文档不属于产品界面语言例外。

顺从/第一性：1-25定位/技术栈；27-41目录；43-78全部npm键/运行命令；80-82基线转引；84-98架构/典型写入；100-116读者/生产边界。package/pom真实声明与版本吻合，JDBC已对；33/90只写staff/admin Web，不再客人Web。98典型保护动作不强行包括登录。

对抗/未核前提：反复STAFF同ID同标签仅复用一节点，不增角色；没有被修后遗漏的Guest/customerWeb旧句；未列客人App并非宣称客人没有App，图可摘要且不画相反边。命令存在不证明全量执行；MOCK_IS_NOT_PRODUCTION_EVIDENCE继续明确外部接入/凭证不足时不能生产。未跑全套后端/E2E/生产闸，不编造全绿。

### README.md:1-149

必要性：中文仓库入口与可复现版本/路径导航，业务概览有三HTML上位，工程事实须可复查。

顺从/第一性：1-27概览与栈，29-51目录，53-94npm命令，96-110基线/快照，112-127读者/地图，129-149架构/边界全行。当前POM有JDBC/aspectjweaver，无JPA；ActionGateAspect.java实际@Aspect，AOP不要求名为starter-aop的依赖才成立。135改staff→业务人员Web，目录38“管理端”不肯定客人端。README生成块720/V385/363--check实际0，99正确解释无surefire时862保留快照。

对抗/未核前提：verify:*是命令族占位，不是实际npm键；本轮检查脚本第一次误截为verify:失败，修正解析后所有具体键存在，错误归验证器不归项目。当前声明单综合体起步/集团预留由PGM413明文支持；无“必须所有读者节点全列出”的新条件。Markdown明确链接均存在；未跑Mermaid图形渲染或全量业务测试，不以结构检查冒充这些运行结果。

## 当前全部26ID逐项状态与证据

| ID | 现象核验 | 当前结果 |
|---|---|---|
| M003-001 | env无五个旧认证死变量，当前使用者均有yml绑定 | 无待修问题/队列否 |
| M003-002 | env无PII空赋值，未设置→默认与生产拒绝有真实负例证据 | 无待修问题/队列否 |
| M003-003 | AGENTS52-53主数据/账户边界符合BSM，最高权限不是平台管理员 | 无待修问题/队列否 |
| M003-004 | 迁移CI说明不再有与实际fetch-depth不符的理由 | 无待修问题/队列否 |
| M003-005 | CONTEXT8-12明确三HTML上位，会员PRD不再优先 | 无待修问题/队列否 |
| M003-006 | CONTEXT21-38项目/业态多对多及城市资产边界对PGM | 无待修问题/队列否 |
| M003-007 | CONTEXT32/35票务域/非散台服务费代收代付对PGM428/429 | 无待修问题/队列否 |
| M003-008 | CONTEXT49/50短信验证手机号唯一识别对BEQR65/66 | 无待修问题/队列否 |
| M003-009 | CONTEXT73可配窗口/default30/无成交向前延对BEQR80 | 无待修问题/队列否 |
| M003-010 | CONTEXT85功能中心/非独立公司顶层收款对BEQR75/85 | 无待修问题/队列否 |
| M003-011 | CONTEXT139/140/146十一端及留痕例外对PGM738/739 | 无待修问题/队列否 |
| M003-012 | CONTEXT160/161逐条与批量存档/主数据删除对BSM569/570 | 无待修问题/队列否 |
| M003-013 | README目录迁移存在，无写死79工具数量 | 无待修问题/队列否 |
| M003-014 | EN引用现行README基线，无V124/386旧当前基线 | 无待修问题/队列否 |
| M003-015 | 中英写动作均典型受保护业务、认证例外 | 无待修问题/队列否 |
| M003-016 | ignore模式用途明确、实际敏感/产物命中，源/样例不被忽略 | 无待修问题/队列否 |
| M003-017 | Semgrep输入策略无已证工程失败，不发明全覆盖门槛 | 无待修问题/队列否 |
| M003-018 | 中英JDBC/starter-jdbc与完整pom一致；其它版本/路径/命令核对 | 无待修问题/队列否 |
| M003-019 | 其余词表与历史记录上位/证据存在；高危反例附加假设不成立 | 无待修问题/队列否 |
| M003-020 | CONTEXT79涵盖商品服务项目内部价，不限兑换SKU | 无待修问题/队列否 |
| M003-021 | README99与106明确snapshot，surefire当前不存在不伪称重测 | 无待修问题/队列否 |
| M003-022 | PROGRESS历史标题及35明确当时测试数字 | 无待修问题/队列否 |
| M003-023 | L4已删除，实际答复删除已落实，不重问 | 无待修问题/队列否 |
| M003-024 | 营销词条完整保留且BSM1725确有明文，实际答复保留 | 无待修问题/队列否 |
| M003-025 | AGENTS55仅四样展示例外，字段/枚举不另放开 | 无待修问题/队列否 |
| M003-026 | 中英图及英文树已无客人Web肯定关系，只画staff/adminWeb | 无待修问题/队列否 |

018/025/026不是只按B4 PASS打勾，而是本轮重新读全部文档、POM与PGM原文核实；其余23项同样重新对现文判断，没有跳过登记项。

## 18GEN逐项结论

| GEN | 覆盖与结论 |
|---|---|
| GEN-001 | 8文件有具体用途，源码最小收窄；未增加抽象/节点/依赖。PASS。 |
| GEN-002 | shell语法、npm具体键、版本路径、基线命令可复现；不以键存在宣称所有门禁运行。PASS。 |
| GEN-003 | 文档类不强制造镜像测试；PII历史同字节真实目标负例附证，不编造全套测试；无测试文件待审。PASS。 |
| GEN-004 | 环境变量真实消费者与边界配置对照，未新增信任入口；本模块不替代后端参数校验审计。PASS。 |
| GEN-005 | 主数据审批/身份与终端上位一致，无在词表授予客人Web或管理员最高权限。PASS。 |
| GEN-006 | 无本模块新增状态/金额写入；PROGRESS仅历史记录，不据之证明全系统事务完整性。不适用执行核验，说明已核。 |
| GEN-007 | 无当前并发执行逻辑；历史幂等描述没有被改成新业务，未声明并发实测。不适用执行核验。 |
| GEN-008 | JDBC、终端、权限、生命周期和词表与直接上位/工程定义对齐。PASS。 |
| GEN-009 | 密钥空值/缺省区别明确，生产证据不足保持门禁，开关留痕例外正确；无吞错。PASS。 |
| GEN-010 | local示例非生产秘密，ignore覆盖env，敏感能力遵循HTML；不打印真实凭证。PASS。 |
| GEN-011 | 快照/历史测试时态明确，独立raw含SHA/命令/未验证边界，未用旧票证明通过。PASS。 |
| GEN-012 | 连接池样例有界；其余无运行循环/线程/文件句柄逻辑。不适用新增执行路径。 |
| GEN-013 | POM/JDBC/AOP/package版本/具体脚本和门禁层次核实；配置引用有消费者。PASS。 |
| GEN-014 | 当前基线V385/363与目录生成器一致；历史V303当时不用未误作现行；本模块无SQL迁移。PASS。 |
| GEN-015 | AGENTS全中文及四样例外符合HTML；开发英文文档非运行界面；Markdown链接和结构无阻断。PASS。 |
| GEN-016 | 621行的命令/路径/状态/版本全部阅读；当前快照证据与脚本匹配、26问题均无待修。PASS。 |
| GEN-017 | 业务逐项先HTML对照，工程技术项详列无发明，024保留不误加永久新规则，未产生新添加。PASS。 |
| GEN-018 | 结论以HTML逐字/POM及解析/grep/实际命令为据；未验证项目明确，校验器误报单独解释。PASS。 |

## 技术性逐条无业务发明登记

环境网络/DB连接/池大小/版本构建字段/密钥注释是当前启动参数；local管理员夹具是既有身份的本地种子，不授权生产。ignore的构建、缓存、本机配置、临时脚本和运行态附件排除是版本管理边界；Semgrep清单是工具输入策略。AGENTS issue/triage/domain指针、worktree/PR/fetch、迁移串行/三变量测试/skipTests是工程规程，产品中文和主数据删除另按HTML细化。README技术栈/目录/命令/基线/读者路径/英文开发说明是工程入口；业务域定位/终端使用者对HTML，不能以画图为由免业务分类。PROGRESS为历史工程建设描述，既有退租流程不由它创造。CONTEXT业务词条不是“技术性免审”，均已按上述各段找HTML上位。没有借HTML沉默新增业务实体、收费、状态、审批或者固定业务数值。

## 本次原文核查证据

````text
FILE docs/platform-governance-map-for-operations-v0.1.html
287:           <p class="lead" style="margin-top:1.2rem"><strong>全局原则 · 界面全中文（所有终端零英文）</strong>：平台所有终端——后台管理端、客人手机端、现场平板与设备端、安卓终端——展示给用户的一切文字，包括菜单、页面标题、字段名、状态、枚举取值、按钮、提示、报错、导出文件表头与内容、短信与通知、日期时间等，<strong>必须全部为中文，不得出现任何英文</strong>（如 active/draft 英文状态、mm/dd/yyyy 英文日期格式、英文按钮或提示）。可以不用中文的只有四样，没有第五样：单据号、设备编号、二维码内容、系统内部标识符。这四样之外，凡是展示给人看的文字一律中文，不得以“这是标识”为由保留英文。<strong>此为强制规则，适用平台全部终端与模块，新增页面与功能一并遵守。</strong></p>
413:           <p class="lead">当前先按一个综合体落地，但所有基础对象都要带“综合体项目归属”。未来需要集团视图和跨项目汇总。</p>
415:             <h3>物理项目与运营公司：多对多协作，不是固定隶属层级</h3>
417:               <li><strong>顶层平台</strong>：制定跨项目、跨经营域的统一治理规则，管理统一系统，并审批跨域规则；不因为处于顶层就自动取得某张票、某个项目或某个商户的收入。</li>
418:               <li><strong>物理项目</strong>：是空间、价格、经营数据和权限隔离维度。一个运营公司可以服务多个项目，一个项目也可以同时由多家运营公司分域负责。</li>
420:               <li><strong>城市项目公司</strong>：每个城市项目公司同时承担本城市项目的资产职责，只负责资产、场地、物业、租赁、直接管理的第三方商户及自身人员和设备；它不管理酒吧、游乐、娱乐平台公司、传媒公司、中央餐饮公司及其人员、设备、定价、票务或结算。历史合同、财务底稿与既往文件中出现的“资产公司”“资产管理公司”，指的就是本主体，同一主体两个叫法；资产业态不另设独立主体，由城市项目公司兼任。但作为综合体通用储值卡的发行与收款主体，长沙项目公司应当就通用卡余额的实际消费向各业态的经营主体直接付款结算，按三个付账日结清；此项付款只是按各业态既定规则兑付客人已付的钱，不构成对该业态定价、票务或结算规则的管理权。</li>
428:                 <tr><th scope="row">娱乐平台公司</th><td>负责酒吧经营域、酒吧通票以及跨域组合通票中的酒吧部分；审批酒吧商户提交的项目内部结算价，决定酒吧域采用内部结算价、固定金额、固定单价或排名档位等规则，并承担其决定或出资权益的结算责任。它参股并管理各独立酒吧商户的相关经营关系，但不取得单一酒吧门票收入；只按生效规则向酒吧商户另收门票综合服务费。纯游乐票、纯游乐通票和演出票与其无关。</td></tr>
429:                 <tr><th scope="row">独立酒吧商户</th><td>负责自身人员、设备、经营和履约。设夜班值班经理，为夜间现场的最高负责人，每天一名，由系统根据当日排班自动指定；凡需其批准或知会的事项系统自动抄送该人，其须立即处理；白天没有夜班值班经理的时段，现场最高负责人为项目负责人。设弱电工程师，隶属娱乐平台公司，负责店面弱电设备的检查与维护，其检查安排在下班之后进行、与店内岗位错峰。自行定价、销售并确认单一酒吧门票已核销部分的收入（票款由娱乐平台公司统一收款后结付，到期未核销部分不归本商户），订台低消、二次消费、额外酒水归本商户；非散台服务费由本商户代收，按后台选定的分配方式发放给当班服务提供人，月结定格的应发部分作代收代付处理、不计本商户收入，月结定格的扣留部分计入本商户收入；后续实际发放只结清应付款、不改变收入归属。酒吧商户可提交价格资料、履约事实和异议，但不能自行决定酒吧通票域内分配方式；酒吧商户提出的异议由客户关怀部协调处理：经内部工单流程流转至相关部门处理，处理结果反馈商户并必须形成完整闭环；处理时限为业务参数（按项目可配，出厂默认二十个工作日，须配上下限）；处理期间按原有效数值先行结算，结论生效后按差额补退。</td></tr>
430:                 <tr><th scope="row">游乐公司</th><td>负责游乐经营域、纯游乐票和纯游乐通票的定价、售票、收款、内部结算审批与域内分配；纯游乐业务不向娱乐平台公司支付门票综合服务费。</td></tr>
431:                 <tr><th scope="row">传媒公司</th><td>负责演出和传媒经营域、其自办场次演出票的定价、售票、收款、内部结算审批与域内分配；演出票不向娱乐平台公司支付门票综合服务费。</td></tr>
433:                 <tr><th scope="row">城市项目公司</th><td>同时承担本城市项目的资产职责，收取场地租金、物业等资产相关收入，并管理直接第三方商户；仅审批其直接管理第三方商户的内部结算价。它不负责酒吧、游乐、演出等经营域的定价、售票和结算。</td></tr>
497:                 <li>把一个商户的“经营对象、运营主体、合同主体、结算主体”绑成一组唯一关系（四类主体可分属不同公司）；合同、结算、权限、看板、第三方接入都引用同一组绑定。</li>
528:                 <li>审批链不由发起人选择档位：发起单据时只需指定该事项的最终审批人，系统按组织层级自动补全从发起人到该最终审批人之间的全部直接上级节点，逐级审批；需要平行线审批的（如法务、财务），另行指定该条线的最终审批人，该条线的层级同样由系统向下自动补全，每个部门或业务线须预先设定其平行审批的最低起点层级。任一节点驳回则整单退回起草人。但下列七类是红线，任何层级的审批都不得绕过：①项目坐满即不再放人进；②当天没有做过合格巡检的设施不得开放；③未满 18 岁一律拒入、拒售酒；④看不到别的商户和别的项目的数据；⑤同一笔操作重复提交不重复生效（重复点击、网络重发、渠道重复回调都只算一次）；⑥分出去的每一分加起来必须等于收进来的，对不上就不让过账。守恒校验不通过的当日即会显现，不得跨日；财务须立即追查，涉及的相关工作人员一同追查；必须逐笔拆解、细算重算，一律走正向更正，不得以手工冲账的方式调平；⑦普通退款只限票种规则明确允许、活动或场次取消、平台或履约方无法提供服务、重复扣款、法律法规要求这五种情形；通票安全例外是客人出现重大情绪不稳定、安全问题或其他必要问题，经夜班值班经理及以上批准免除其通票费用，即使已核销也可退款。持授权 U 盘密钥的最高权限只能逐笔放行五种普通退款理由之外的未核销票，不解除已开始核销后的退款禁止。其中第⑤⑥⑦类，持授权 U 盘密钥的最高权限可以逐笔开放，每次只能对当前这一笔放行、不得整类关闭校验；第①②③类（项目坐满即不再放人进、当天没有做过合格巡检的设施不得开放、未满 18 岁一律拒入拒售酒）连最高权限持密钥也不得开放，系统不提供开放入口；第④类看不到别的商户和别的项目的数据，持授权 U 盘密钥的最高权限可以逐笔开放，每次只对当前这一笔生效并留痕。上级审批不是万能通行证。上述七类校验不设关闭开关：不得通过全局、综合体、商户、业务系统、模块、版本/设备、灰度中的任何一档关闭这七类校验。审批不得绕过与开关不得关闭，两条并列适用。</li>
644:           <p class="lead">权限不能只按岗位粗略分。一个单一功能就是一个权限点，例如“票务核销”“查看卡座预定状态”。角色可以批量配置，员工也可以单独增减权限。</p>
652:                 <li>敏感动作可要求上一级、上两级或最高权限审批。</li>
654:                 <li>敏感经营数据不限于内部结算价和成本价，还包括采购价、毛利、分账底价、域内补贴缺口、供应链价格以及其他被标记为敏感的数据。查看、导出和修改分别授权；普通用户、会员、前线员工以及没有相应价格权限的商户人员不得查看。系统自动使用某个价格完成计算，不代表当前操作人取得查看或修改权限。</li>
655:                 <li>最终权限 = 角色权限 + 基础模板 + 有效临时模板 + 员工单独授予 − 显式拒绝；显式拒绝优先级最高。</li>
656:                 <li>临时授权可设有效期或单次使用；权限的授予、收回、到期、单次消耗、失效全部留痕。</li>
659:                 <li>临时权限（已确认）：员工可申请临时权限，按层级对应主管审批（一级/二级/三级直到最高），有时间限制、到期自动失效；某岗位层级频繁申请同类临时权限时系统自动总结、建议平台管理员转为固定权限，某个人频繁申请时提交异常总结供复核。</li>
660:                 <li>高危授权审批（已确认）：以下五类权限授出前须先审批才生效——资金/核销/退款/调账/分红发放类、提权或跨商户跨项目、敏感经营数据的查看/导出/修改、高危业务（沽清/反结/存酒作废）；税费规则与税费调整。审批链按上述“只指定最终审批人、系统自动补全中间层级”的方式生成，不由发起人选择档位。持股分红的方案配置、算单、审批和发放属于资金类高危动作，由最高权限审批，提交人不能审批自己的单据。税费规则与税费调整须经财务审核、税务审核、法务审核，再由顶层平台公司的老板最终确认后生效，提交人不得审批自己提交的调整。查看内部结算价、查看企业微信会话完整原文、导出客人或员工个人信息与结算价格数据这几类高敏能力，不因“平台管理员”身份自动获得，必须逐项单独授予并留痕。新上线的功能必须在上线前先归类：归入上述高危类别之一，或者明确定为日常功能；没有完成归类的一律先按高危处理，需逐项单独授予并留痕，不得因为“还没定”就当成日常功能自动放给平台管理员。归类结果日后要改，须重新走一次同级审批并留痕。</li>
675:                 <li>外包/服务商人员（设备维保单位、安保与保洁、工程施工/装修队等）可拥有自己的系统账号登录，但只能处理被指派的工单、负责的区域或任务，看不到平台全局，也不能跨出本综合体；开通哪些外包账号、能做什么由管理员分配。在管理员设定的机构额度与权限范围内，外部机构（如维保单位）也可获得一个自助链接，自行开通或停用其下属人员账号、查看下属工作完成情况；平台为每家机构设定下属账号数量上限，下属账号有效期随合同周期。</li>
676:                 <li>审计人员分两类：内部稽核可查看本综合体全部数据，外部审计/监管只能查看划定的范围与时间段；两类都只能查看、不能修改任何业务数据。</li>
677:                 <li>审计人员查看或导出敏感信息（资金流水、人脸与身份等个人信息、完整操作记录导出）需先经审批并全程留痕。</li>
731:                 <li>关闭后菜单不可见或不可点，接口也要拒绝业务动作。</li>
732:                 <li>访问被拒绝也要留痕，便于排查是权限问题还是开关问题。</li>
736:                 <li>开关默认取值：任何能力在后台没有显式开关配置行时一律默认关闭（opt-in），要启用必须有人显式打开；开关缺行不得视为开启，也不得由代码或部署脚本代为「视同打开」。这与拉取失败时的「兜底关闭」是两件事——兜底关闭针对读不到配置的运行时故障，本条针对配置里压根没有这一行。</li>
738:                 <li>开关服务、支付通路、核销与入场等营业必需的能力，不得在营业时段进行任何可能导致中断的变更、升级或维护，所有此类操作只能安排在非营业时段并事先报备；开关拉取失败时按“兜底关闭”处理，写操作一律拒绝；只有留痕是例外——留痕是一种特殊的写操作，不受兜底关闭影响，开关拉不到期间发生过什么、谁被拒绝了，照样要记下来。前端隐藏只是辅助，后端接口必须独立校验。</li>
739:                 <li>受控的十一类终端：客人手机应用、客人小程序、Web 前端（业务人员，客人无网页端）、工作人员平板、员工工作手机、员工私人手机、商户网页门户、商户手机应用、触摸屏一体机、移动管理端、独立经销商 Web 门户。经销商门户适用与其他终端相同的后台开关、权限、数据范围、访问拒绝留痕和兜底关闭规则，并与商户门户及内部员工端隔离账号、权限和数据。</li>
965:             <div class="question confirmed">已拍板：界面全中文（所有终端零英文）——后台/客人端/现场设备/安卓端展示给用户的一切文字（菜单、字段、状态、枚举、按钮、提示、报错、导出、短信、日期时间）必须全中文，不得出现英文；可以不用中文的只有单据号、设备编号、二维码内容、系统内部标识符四样，此外一律中文；强制规则，适用全部终端与模块。</div>
FILE docs/business-enum-quick-reference-v0.1.html
54:     <tr><td class="k">项目内部结算价</td><td>每个项目分别设“内部结算价管理”页面和独立价格目录，不得跨项目引用。实际提供商品或服务的主体提交，对应域主管公司审批，提交人与审批人分离：酒吧由娱乐平台公司审批，游乐由游乐公司审批，演出传媒由传媒公司审批，中央餐饮供应的酒水或小食由中央餐饮公司内部差人审批；餐饮与零售商户（含独立、联营、租赁各种合作模式）统一由城市项目公司审批，其中中央餐饮供货部分仍由中央餐饮公司审批；业态为“其他”的商户，由其商户档案上指定的主管公司审批。任何商户在没有获批内部结算价之前，不得为其开通需要该价格结算的权益或券。价格变更形成新版本并只对后续生效，核销或结算时锁定当时版本。</td></tr>
65:     <tr><td class="k">会员唯一识别</td><td>以完成短信验证的手机号作为判断新会员的唯一标准。同一已验证手机号在全国平台内只建立一个会员账户，并跨城市、跨项目复用会员身份、成长值和等级；不同手机号不得仅凭姓名、生日等弱信息自动合并，系统也不得声称已经识别为同一自然人。</td></tr>
66:     <tr><td class="k">会员注册</td><td>会员主动提交注册，完成手机号短信验证、填写生日、满足平台年龄要求，并同意当前会员条款和隐私条款后，系统自动注册成功并直接创建有效会员身份。任一条件不满足时注册失败且不创建会员身份，不产生“注册中”或“未通过”状态；微信身份不能代替手机号标准。</td></tr>
70:     <tr><td class="k">等级与成长值</td><td>会员共六个等级，正式名称依次为新乐会员、银卡、金卡、白金、钻石、黑钻（业务参数，全平台统一可配，此六个名称为出厂默认）；等级依据后台配置的滚动统计周期（业务参数，全平台统一可配，出厂默认十二个月，须同时配置上下限）内的成长值计算，客人在全国任一项目的消费均计入该周期内的成长值计算。达到门槛立即升级，升级后享有后台配置的保护期（业务参数，全平台统一可配，出厂默认十二个月，须同时配置上下限）；保护期结束后按届时滚动周期内的成长值重新定级，保护期结束前按后台配置的提前通知天数（业务参数，全平台统一可配，出厂默认三十天，须同时配置上下限）发出降级预警，告知按发出预警当日的成长值测算将降至哪一等级；预警只是提醒、不是降级决定，最终等级以保护期结束当日按届时滚动周期成长值重新定级的结果为准。预警发出后至保护期结束前重新达到原等级门槛的不降级，并发出「已保级」通知；保护期结束时应当降级但此前未曾发出预警的，降级照常执行并在生效后立即补发通知。上述三项参数的变更须经审批、只对后续周期生效，不追溯已确认的等级。成长值与可用积分是两套独立账，积分兑换不扣成长值。</td></tr>
75:     <tr><td class="k">有效消费与冻结</td><td>符合规则的有效消费在履约完成后产生积分和成长值。消费积分在退款期内冻结，退款期结束后才转为可用；只有转为可用才形成商户积分发行费。积分发行费 ＝ 转为可用的积分数 × 全国统一积分发行单价；收款方为全国积分结算中心，当全国积分结算中心不是独立公司时，收款方为顶层平台公司。积分加现金兑换整单不属于有效消费，不产生积分或成长值，也不刷新积分有效期。会员积分已被兑换后又发生特殊退款、可用积分不足以追回时，差额形成「待追回积分」挂账，由该会员后续获得的积分依次抵扣；待追回积分清偿完毕之前，该会员不得再进行任何积分兑换。待追回积分不形成负积分余额，也不形成对会员的现金债务。待追回积分只有四类出口，除此之外不得以其他方式消除：①追平——后续获得的积分依次抵扣至清零后自动解除兑换限制并留痕；②小额豁免——待追回额度不超过后台配置的豁免线的（业务参数，全平台统一可配，出厂默认 100 积分及以下，须同时配置上下限），由财务豁免并留痕；③不设时效——未清偿的一直挂账，不因时间流逝自动消灭；④会员注销——注销时待追回额度作废登记、不形成现金债权，也不再追偿。券抵扣的部分不属于有效消费，不产生积分与成长值，也不刷新积分有效期；客人实付部分照常。</td></tr>
78:     <tr><td class="k">积分兑换定价</td><td>积分只兑换虚拟券。纯积分方案同时受项目零售参考价下限和内部结算价下限约束；积分加现金方案同时覆盖零售参考价和内部结算价，会员现金支付额不得超过内部结算价。积分兑换结算单价＝全国统一积分发行单价×全国统一平台兑付系数，兑付系数由后台配置（业务参数，全平台统一可配，出厂默认 0.7，须同时配置上下限），仅用于定价覆盖校验。低于任一或两个下限必须填写原因、目标、期限、数量、缺口、成本承担主体和预算，由全国积分结算中心授权岗位审批；预算不足不得发券。</td></tr>
80:     <tr><td class="k">项目零售参考价</td><td>同一项目、同一商品在后台配置的滚动取价窗口内（业务参数，按项目可配，出厂默认三十天，须同时配置上下限）；窗口内没有有效成交的，自动向前延长至找到最近一笔有效成交为止的最低有效实际成交价，不使用其他项目或项目外渠道数据。只纳入已完成且未取消、未作废的普通有价交易，排除纯积分、积分加现金、零元赠送、测试和异常人工记录；全额退款移除样本，部分退款按保留数量和最终现金支付额及原商品规格分摊重新计算。新商品规格无有效成交时，由实际销售或提供主体提交本项目首发公开销售价、对应域主管公司审批，暂作参考价；产生首笔有效成交后自动切换。价格不得直接手改，只能纠正源交易后由系统重算并保留版本与审计。</td></tr>
82:     <tr><td class="k">重复兑换与转赠</td><td>同一种兑换券允许同一会员多次兑换，除非券种设置了会员累计兑换次数上限。积分余额不可转赠；虚拟券是否可转赠由券种配置，默认不可转赠，资格类券强制不可转赠。</td></tr>
85:     <tr><td class="k">会员系统服务费</td><td>单独收取并作为独立账单行；每份商户合同在“按有效会员消费金额比例”或“固定月费”中二选一，初期可配置为 0；收款方同积分发行费——全国积分结算中心，当其不是独立公司时为顶层平台公司。按比例收取的，对应交易发生退款时，该笔服务费按实退金额同比例退回；按固定月费收取的，不因交易退款而退还。</td></tr>
FILE docs/business-system-map-for-operations-v0.1.html
567:           <p>过去"东西一旦录进去就改不了、删不掉"的问题解决了。对配置类主数据——<strong>商户档案、票种、价格规则、酒吧商品/分类/套餐</strong>等——现在可以：</p>
569:             <div class="explain-item"><strong>存档（先下线）</strong>：配错了或暂时不用，点"存档"就从各处的选择列表里隐藏，不再被新业务选用；但历史照样能查、能对账。想恢复点"取消存档"即可。逐条存档属日常操作，不用审批；但一次性存档超过后台配置条数的，或整店、整类存档的，须经审批后执行（业务参数，按综合体可配，须同时配置上下限）。后台另设「已存档内容」页，进去可以看到全部被存档的对象，按分类查阅、可逐条恢复。</div>
570:             <div class="explain-item"><strong>删除（走审批·软删除）</strong>：要删除，得先存档，再"申请删除"、填写删除理由，由<strong>具备最高权限的人</strong>审批通过后执行（申请人不能审批自己的申请）。删除走审批、要留痕。</div>
588:           <p><strong>平台管理员与最高权限是两个不同的角色。</strong>平台管理员是日常运维岗位，人数上限由后台配置（业务参数，全平台统一可配，须同时配置上下限）：拥有系统里全部<strong>日常功能</strong>权限；今后每上线一个新功能，都必须先定好它算日常功能还是高危功能才允许上线，定为日常功能的会自动同步给他，不会出现"管理员反而没权限"的情况，还没定类的一律先当高危功能对待、不自动同步；但资金操作（退款、调账、分红发放、税费调整）、敏感数据查看与导出（内部结算价、成本价、分账底价、企业微信会话原文、客人与员工个人信息）、删除，以及最终业务审批这几类，不因"平台管理员"这个身份自动获得，必须逐项单独授予并留痕，平台管理员即使同时具备业务权限，也不得审批自己发起的上述事项。<strong>最高权限是另一个角色，全平台只设一人，即持有授权 U 盘密钥的那个人</strong>：凭密钥可以执行系统启动、破窗登录、手动清理留存数据等最高权限动作，其权限由密钥的物理保管来把关，不再另设审批环节。其他岗位（营销员、服务员、收银、外包、审计等）按岗位分配各自能看能做的范围。此外，权限模板选中后可以直接查看和修改已配的权限，勾选权限的区域也按左侧菜单的板块归类，照着目录勾即可。</p>
862:                 <li>账户注销：客人可以自行申请注销账户。发起注销时系统须提醒其名下尚有存酒与储值卡余额，客人可以选择不取存酒、放弃余额，径行注销；放弃的存酒按存酒到期规则转为所在酒吧的自有库存资产，放弃的储值卡余额归发卡主体（通用卡归长沙项目公司，商户级卡归发卡商户）。注销后：客人的姓名、手机号、人脸、证件等个人信息一律删除；仅有一项例外：注销时点与该手机号的对应事件记录，仅为执行枚举速查表「会员身份状态」条的『同一手机号一年内注销与注册合计次数』异常预警及注销后重新注册的新人资格判定而留存，留存至该一年统计窗口届满即删除，不得用于其他目的、不与新账户建立任何关联；积分、券、未核销的票一律清零；消费记录、支付流水、退款记录等钱账留存，但脱去个人标识、改以不可反查的编号记录，以保证历史账目仍然对得平。个人信息的查询与导出按来源判定：属该商户的客户的，该商户可以查询、可以导出，但不得删除。</li>
1725:                   <tr><th scope="row">营销费用</th><td>活动发起方与费用承担方可以不同；费用承担方必须锁定到具体法律主体，可以是商户、娱乐平台公司、游乐公司、传媒公司、城市项目公司、中央餐饮公司、全国平台或其他登记赞助主体。同一场活动支持多个主体共同承担，按固定金额或按比例分摊，采用比例分摊时各方比例合计必须为 100%；各承担方的预算分别锁定、分别扣减，任一方预算用尽不得自动占用其他方的预算。</td></tr><!-- param-tier:ignore 100% -->
````

## 本次技术核查命令与输出

使用POM XML依赖解析、package.json脚本/版本解析、Markdown显式链接存在性、bash -n、baseline --check、git diff --check、git check-ignore --no-index --stdin、yml变量原文检索；下列为实际结果：

````text
POM_DEPENDENCIES ["spring-boot-starter-actuator", "aspectjweaver", "spring-boot-flyway", "spring-boot-starter-jdbc", "spring-boot-starter-security", "spring-boot-starter-validation", "spring-boot-starter-webmvc", "flyway-core", "flyway-database-postgresql", "postgresql", "springdoc-openapi-starter-webmvc-ui", "spring-boot-starter-test", "spring-boot-webmvc-test", "spring-security-test"]
SCRIPT_KEYS ["build", "dev:backend", "dev:web", "test:backend", "typecheck", "verify:backend-env", "verify:closed-loop-runtime", "verify:delivery-gates", "verify:migration-version-order", "verify:openapi", "verify:operation-matrix", "verify:p0-contract", "verify:production-blockers", "verify:tenant-isolation", "verify:ui", "verify:ui:real-backend:seeded", "verify:unit"]
DOCUMENTED_GLOB_NOT_COMMAND ["verify:*"]
FRONTEND_DECLARATIONS {"@ant-design/icons": "^5.5.2", "antd": "^5.22.5", "qrcode.react": "^4.2.0", "react": "^19.0.0", "react-dom": "^19.0.0", "@testing-library/jest-dom": "^6.9.1", "@testing-library/react": "^16.3.2", "@testing-library/user-event": "^14.6.1", "@types/react": "^19.2.16", "@types/react-dom": "^19.2.3", "@vitejs/plugin-react": "^6.0.2", "jsdom": "^29.1.1", "typescript": "^5.7.2", "vite": "^8.0.16", "vitest": "^4.1.9"}
MIGRATION_GATE {'delivery': True, 'predeploy': False}
LINKS README.md []
LINKS README.en.md []
LINKS AGENTS.md []
LINKS CONTEXT.md []
LINKS PROGRESS.md []
SUREFIRE_EXISTS False
DIRECT_GUIDANCE_PATHS_EXIST
CHECK ["bash", "-n", ".env.example"] EXIT 0  
CHECK ["node", "tools/refresh-entry-doc-baseline.mjs", "--check"] EXIT 0 [entry-doc-baseline] PASS: 快照与实际一致（720 ops / V385（363 迁移））。
 
CHECK ["git", "diff", "--check"] EXIT 0  
IGNORE_CHECK 0 .gitignore:4:.env	.env
.gitignore:5:.env.local	.env.local
.gitignore:1:node_modules	node_modules/x
.gitignore:3:backend/target	backend/target/x
.gitignore:54:backend/data/bar-images/	backend/data/bar-images/x
.gitignore:55:backend/data/evidence-files/	backend/data/evidence-files/x

ENV_BINDINGS
backend/src/main/resources/application.yml:2:  port: ${COMPLEX_API_PORT:8080}
backend/src/main/resources/application.yml:3:  address: ${COMPLEX_API_HOST:127.0.0.1}
backend/src/main/resources/application.yml:17:  profiles:
backend/src/main/resources/application.yml:20:    url: ${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex}
backend/src/main/resources/application.yml:21:    username: ${COMPLEX_DATABASE_USERNAME:complex}
backend/src/main/resources/application.yml:22:    password: ${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}
backend/src/main/resources/application.yml:24:      maximum-pool-size: ${COMPLEX_DATABASE_POOL_SIZE:10}
backend/src/main/resources/application.yml:25:      minimum-idle: ${COMPLEX_DATABASE_MIN_IDLE:2}
backend/src/main/resources/application.yml:53:    version: ${COMPLEX_API_VERSION:0.1.0}
backend/src/main/resources/application.yml:54:    build-time: ${COMPLEX_API_BUILD_TIME:local}
backend/src/main/resources/application.yml:107:    # 运行态只读本地、不依赖 U 盘（模型①）。生产必须由 env COMPLEX_KEYVAULT_PII_KEY 注入强随机密钥
backend/src/main/resources/application.yml:109:    pii: ${COMPLEX_KEYVAULT_PII_KEY:complex-dev-pii-encryption-key-change-me}
backend/src/main/resources/application-local.yml:10:      # 落库（bcrypt），并绑定 role_platform_admin。仅 local profile 生效，
backend/src/main/resources/application-local.yml:11:      # prod profile 不含此配置，不会带入生产。
backend/src/main/resources/application-local.yml:12:      # 环境变量沿用项目既有约定 P0_ADMIN_USERNAME / P0_ADMIN_PASSWORD，
backend/src/main/resources/application-local.yml:16:        username: ${P0_ADMIN_USERNAME:admin}
backend/src/main/resources/application-local.yml:17:        password: ${P0_ADMIN_PASSWORD:admin-pass}
AOP_DECLARATION 1 
````

AOP精确源码路径核验：先在common目录检索得到exit1无命中（搜索目录过窄，不表示全项目没有AOP）；随即rg --files backend/src/main -g *ActionGate*定位backend/src/main/java/com/complex/platform/gate/ActionGateAspect.java，20行@Aspect、24行class ActionGateAspect。POM声明aspectjweaver且实际有切面，未据“没有starter-aop”造缺陷。

校验器首轮错误（保留）：regex把文档命令族verify:*截为verify:，断言失败。后将完整通配词先提取再排除“命令族非具体键”，全部具体npm键实际存在。只修内存临时校验脚本，未改项目来迁就工具：

````text
POM_DEPENDENCIES ["spring-boot-starter-actuator", "aspectjweaver", "spring-boot-flyway", "spring-boot-starter-jdbc", "spring-boot-starter-security", "spring-boot-starter-validation", "spring-boot-starter-webmvc", "flyway-core", "flyway-database-postgresql", "postgresql", "springdoc-openapi-starter-webmvc-ui", "spring-boot-starter-test", "spring-boot-webmvc-test", "spring-security-test"]
SCRIPT_KEYS ["build", "dev:backend", "dev:web", "test:backend", "typecheck", "verify:", "verify:backend-env", "verify:closed-loop-runtime", "verify:delivery-gates", "verify:migration-version-order", "verify:openapi", "verify:operation-matrix", "verify:p0-contract", "verify:production-blockers", "verify:tenant-isolation", "verify:ui", "verify:ui:real-backend:seeded", "verify:unit"]
Traceback (most recent call last):
  File "<stdin>", line 11, in <module>
AssertionError
````

## 已执行行为的准确引用及边界

本模块B4-r1曾对当前同字节crypto源编译并实际调用Spring与KeyVault，下面是当时真实输出，本B5未重复运行、也不把它写成本B5新执行；本轮env仍为同一893字节SHA e377b54d9cd829ddeed30e0ae730f55d51fbc8c272f3df5ca611fdbdba125049，运行配置关系已重新核对：

````text
JAVAC_CURRENT_SOURCES_EXIT 0

REAL_KEYVAULT_EXIT 0
OLD_EMPTY_REJECTED; CURRENT_UNSET_LOCAL_AES_BYTES=32; PROD_DEFAULT_REJECTED

ENV_TEMPLATE_PRESENCE_CHECK old_empty=True current_unset=True; no env file sourced; no DB
MARKDOWN_LINKS README.md missing=[]
MARKDOWN_LINKS README.en.md missing=[]
MARKDOWN_LINKS CONTEXT.md missing=[]
MARKDOWN_LINKS AGENTS.md missing=[]
MARKDOWN_LINKS PROGRESS.md missing=[]
No source writes; temporary helper removed automatically.
````

未执行全后端mvn/vitest/E2E/DB迁移/生产部署/全量Semgrep/Mermaid图形渲染。本模块是根配置与文档入口，按runbook产物类型用1/4类证据足以证明本模块声明；以上未执行不被伪称通过，也不凭空变成失败。

## 本次完整结论

本方B5-r2 PASS；新CX发现0，既有26ID待修0；621行全部读完、18GEN全部核完，三线均完整。026客人Web、025展示例外、018JPA已按现文与上位实际确认消除；019高危分档假设不复活。023删除/024保留落实。等待Grok本次完整raw和真实exit code，完成B5后四值全等才可写ledger PASS并做收口和发布；任何另一席FAIL均整批回退。写本report时源码、lock、findings、ledger未改，B3=2。


## 收到外审后的补充事实核验
版本：V0.02

本节在Grok本次完整报告读完之后追加，保留前49514字节独立报告，不伪称以下核验发生于互盲期、不改变原独立PASS。未改源码或Grok原文，不是新一轮B5。

实跑现有Spring StandardEnvironment：通过真实子进程环境变量SPRING_PROFILES_ACTIVE=local，使用已安装Temurin 21与本机Maven库spring-core/spring-context/commons-logging编译并运行最小Java，getActiveProfiles()实际含local；没有setActiveProfiles、没有启动服务或连接数据库。临时Java目录自动删除。类型2/4的本方执行证据支持环境绑定，不把Grok提及但未给官方原文的“官方绑定”字样当类型3证据。

PROFILE_CHECK_EXIT 0 ACTUAL_SPRING_ENV_PROFILE=local
 
docs/business-system-map-for-operations-v0.1.html:898:              <h3>会员注册、认证与收费边界</h3>
docs/business-enum-quick-reference-v0.1.html:89:    <tr><td class="k">会员注册与开卡</td><td>会员注册免费；本人完成手机号验证并同意后注册；后台不得直接新增或批量导入。付费内容作为独立付费权益产品，不收会员开卡费。</td></tr>
B5_AFTER_FOUR=["23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de", "23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de", "23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de", "23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de"]
SOURCE_WRITES=0; TEMP_HELPER_REMOVED

Grok verify:backend-env因宿主JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64不存在而exit1，原失败完整保留；已安装Java在/usr/lib/jvm/temurin-21-jdk-amd64。没有修改宿主持久环境，没有将失败命令说成通过；后续若需Java仅为命令指定已安装路径。BEQR89免费会员明文为本方追加的类型1直接核验。追加检查无新缺陷，当前26项队列仍全否。


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M003 B5-r2 当前471计划完整独立自审
版本：V0.03
日期：2026-09-08
本席：当前Codex；远程审计与文件写入：直接SSH。
本席B5结论：PASS。
阶段B5-r2；ledger V1.62，M003 IN_PROGRESS，B3累计2。B2队列空，本轮未执行B3/B4；PASS仅是本席完整模块结论，尚不能单方写ledger PASS。

## 1. 完整范围与独立性
当前模块“治理门禁与 CI 基线／配置与工程入口／第1段”，只审.gitignore全55行与package.json全147行，共202行。当前锁M003映射在6382行，完整GEN-001—018；旧322同号的其他文件不冒作当前范围。
按串行互盲：先由Grok完成本次B5，适配器只返回指定REPORT_PATH=/home/james/complex-management/docs/audit-2026-09/modules/M003-b5-r2-grok-raw.md及EXIT_CODE=0。随后本席重新全读202行、锁general全文/M003映射和相关HTML原句，重新执行当前入口检查；本完整self报告保存前未读Grok本次B5任何正文或JSON。
本次实际Git main HEAD 242f487233d028173cb9198eecd079f5d877405c；git status --porcelain -- .gitignore package.json无输出。
- .gitignore：1199bytes，SHA256 75861ded5d3859bcda593aafe3f3862e765470e7f8b6636a643bf59de9d7c51b。
- package.json：15583bytes，SHA256 350999a90ddf85b2d3dcbee090dd593fb7faff18d0f467f0a608d8b3664d473f，documentVersion V0.01，应用SemVer0.1.0。
- criteria-lock.md V0.11实算见首行，未改锁、sidecar或基准；LOCK-AMEND-COUNT 1。四值中Grok本次字段尚未读取，不能提前宣称B5四值完成。
- 三HTML本次实算BSM a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa；PGM c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c；BEQR 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66，保持授权后内容。
旧self B5-r2为51460bytes、SHA256 4fdd804f891c5bc412064b98027cb27224c3e39ec50708485088ff3ac1d0059d，V0.02；本次V0.03全文追加保留旧前缀。

## 2. 技能使用
当前轮适用SKILL.md已在本席环境实际读取：using-superpowers、verification-before-completion、receiving-code-review、systematic-debugging、grilling、codebase-design、diagnosing-bugs、writing-for-agents和ponytail-review；可选batch-grill-me/ubiquitous-language按存在版本收窄使用。实际应用为完整问题追问、正常首跑反例、追直接入口、证据核对、必要性收窄，不只是列名。无源码变更，TDD不在本节点新写测试。无第三席、无技能外发/新文档/提交/人闸。没有Claude hooks已生效的证据，不伪称其运行。

## 3. 第一性原理线
### 3.1 为什么存在
.gitignore承担构建/测试/本机配置/运行产物的版本控制边界。当前21个正负例实际证明：依赖、缓存、真实.env及运行附件匹配；.env.example、正式tools调试名、dated findings JSON、审计文档不被吞掉。忽略不是删除业务数据，也不替代保留/授权规则。
package.json是127项命令及唯一apps/web工作区的根入口。当前全量命令图、75个Node目标和真实npm转发证明用途；开发/测试/检查工具有具体入口承载。删除全部入口会使npm调用失去声明；无须为npm语法硬造HTML业务规则。
必要性仍逐项审视：四对忽略模式重叠，前项完整覆盖后项；真实匹配未发生错误/漏保护。这是可收窄观察，不是本轮必须修的工程故障，不为删除4行触发新的修复批次。命令别名也有用户/组合链入口用途，未因纯命名重叠机械全部删除。此判断依据当前源码、Git规范及当前行为，不依据旧PASS。
### 3.2 声称行为是否执行
本次重新运行127条shell语法、75Node目标node --check、workspace与根命令引用/循环检查；两个env入口；两套UI发现；嵌套npm参数转交；三个缺少任务/环境负例；21忽略样本与断言；predeploy真实首项失败传播；entry-anchors独立真实红灯。命令和输出见第6节。
本次未运行全量业务UI、后端DB集成测试、Android构建/安装、生产部署、文档生成器写模式、依赖安装；不将--list/--dry/--check冒称它们通过。两文件不定义这些被调用工具的业务实现，该实现属于各自模块。
### 3.3 哪些前提仍未全面验证
Node当前v24.20.0满足声明；Java/Maven/Gradle/SDK由env实际核对。当前npm11.19.0与声明npm@10.9.8不同，未在所有npm/Node组合重建依赖，不因此捏造兼容性问题或新增版本门禁。根lock manifest镜像当前匹配；未把整个package-lock纳入本模块重新审完。
业务与发布证据没有被默认视为干净：本次predeploy及entry-anchors仍按实际坏输入失败，详列限制。不通过改HTML/CSV/台账或忽略目录刷绿。

## 4. 四分类与全行覆盖
先冲突→细化→技术性→添加。没有发现两文件与三HTML明文冲突、新业务发明或新HTML自身问题。配置声明无业务参数/价格/状态/权限取值；命令名提及领域不自动等于创造业务。以下技术性项目均无业务发明、只服务工程运行；下列细化项目给出具体上位完整原句。

| 文件与行 | 覆盖内容 | 分类/用途 |
|---|---|---|
| .gitignore1—9 | Node/Web/Java产物、.env、DB临时文件、覆盖率和Vite | 技术性 |
| .gitignore10—21 | Gradle/Kotlin/build与安装产物、本地SDK路径；包括4对重叠观察 | 技术性；无当前行为故障 |
| .gitignore22—27 | Playwright两套报告/test-results及空行 | 技术性 |
| .gitignore28—32 | 本机Claude配置/TS增量缓存及注释 | 技术性 |
| .gitignore33—39 | 扫描日志、滚动产物和归档日志；保留dated findings | 技术性 |
| .gitignore40—43 | loop运行产物及空行 | 技术性 |
| .gitignore44—51 | 仅仓库根探针名称匹配及说明 | 技术性 |
| .gitignore52—55 | 空行、商品图片注释、运行图片/证据目录 | 53—55细化；不取消业务保留义务 |
| package1—9 | 工程身份、private、应用/文档版本、ESM、workspace | 技术性 |
| package10—138 | 127脚本及结构括号 | 下表逐项 |
| package139—147 | npm声明、Node引擎、Playwright/yaml开发依赖、闭合结构 | 技术性 |

| 原文位置 | 本次重新核读的完整业务原句 | 对应内容 |
|---|---|---|
| BSM959 | 商品分类、商品图片、套餐都在后台维护：分类可自定义、图片后台上传保存、套餐由后台从已有商品组合而成。 | .gitignore54运行商品图片 |
| PGM893 | 平台治理层要保证每个重要动作能追溯。谁发起、谁销售、谁服务、谁第一个处理异常、为什么、谁审批、结果是什么，都要能查到。 | .gitignore55运行留痕证据 |
| PGM907 | 查看企业微信会话原文、查看视频证据和查看财务敏感信息。 | 同上，忽略Git提交不取消授权 |
| BEQR23 | 本表与《业务系统地图》《平台治理地图》共同组成三份最高业务解释权 HTML；只有经批准写入这三份 HTML 的内容才是最终业务口径，PRD、其他文档和后台配置均不得覆盖。 | package32—35解释权校验入口 |
| PGM870 | 客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放。 | package97核验入口 |
| PGM447 | 集团下钻细则（已确认）：集团视图能看什么按职位配置模板决定——不同职位有不同可见范围（汇总、下钻各项目明细、跨项目对标）；数据隔离仍是综合体级，只有集团高层按其职位模板可跨综合体查看。职位配置模板只决定汇总与下钻到哪一层，不是可自定义的数据范围；数据范围没有后台设置项。 | package95/137隔离核验入口 |

.env忽略本身不执行开关/加密/授权，技术性。所有输入引用只为验证当前2文件，不将跨文件目标扩为已审模块。没有添加项或三HTML修改申请。

### package.json全部127脚本逐项登记
每条当前命令都已重新机器解析目标/工作区/引号并逐条阅读；此表列的是声明分类，不谎称127项业务动作全部运行。
| 行 | 脚本名 | 分类 |
|---|---|---|
| 11 | `dev` | 技术性（工程入口） |
| 12 | `dev:backend` | 技术性（工程入口） |
| 13 | `test:backend` | 技术性（工程入口） |
| 14 | `verify:backend-env` | 技术性（工程入口） |
| 15 | `dev:api` | 技术性（工程入口） |
| 16 | `dev:web` | 技术性（工程入口） |
| 17 | `verify:openapi` | 技术性（工程入口） |
| 18 | `audit:client-routes` | 技术性（工程入口） |
| 19 | `verify:client-routes` | 技术性（工程入口） |
| 20 | `audit:client-openapi` | 技术性（工程入口） |
| 21 | `verify:client-openapi` | 技术性（工程入口） |
| 22 | `audit:client-payload` | 技术性（工程入口） |
| 23 | `verify:client-payload` | 技术性（工程入口） |
| 24 | `docs:openapi-examples` | 技术性（工程入口） |
| 25 | `verify:openapi-examples` | 技术性（工程入口） |
| 26 | `docs:operation-matrix` | 技术性（工程入口） |
| 27 | `verify:operation-matrix` | 技术性（工程入口） |
| 28 | `docs:openapi-closure` | 技术性（工程入口） |
| 29 | `verify:openapi-closure` | 技术性（工程入口） |
| 30 | `docs:security-baseline` | 技术性（工程入口） |
| 31 | `docs:entry-baseline` | 技术性（工程入口） |
| 32 | `verify:html-ledger-sync` | 细化（见原文表） |
| 33 | `verify:entry-anchors` | 细化（见原文表） |
| 34 | `verify:html-param-tier` | 细化（见原文表） |
| 35 | `verify:entry-doc-baseline` | 细化（见原文表） |
| 36 | `verify:security-baseline` | 技术性（工程入口） |
| 37 | `verify:prod-docs-access` | 技术性（工程入口） |
| 38 | `verify:ops160-execution-policy` | 技术性（工程入口） |
| 39 | `verify:ops160-backup-restore` | 技术性（工程入口） |
| 40 | `verify:production-credentials` | 技术性（工程入口） |
| 41 | `verify:p0a-ops-hardening` | 技术性（工程入口） |
| 42 | `verify:ops160-recovery` | 技术性（工程入口） |
| 43 | `verify:p0-contract` | 技术性（工程入口） |
| 44 | `verify:p0-web` | 技术性（工程入口） |
| 45 | `verify:p0-env` | 技术性（工程入口） |
| 46 | `build` | 技术性（工程入口） |
| 47 | `typecheck` | 技术性（工程入口） |
| 48 | `smoke:p0` | 技术性（工程入口） |
| 49 | `verify:android-env` | 技术性（工程入口） |
| 50 | `android:p0:build` | 技术性（工程入口） |
| 51 | `android:p0:smoke` | 技术性（工程入口） |
| 52 | `android:p0:apk` | 技术性（工程入口） |
| 53 | `verify:p0` | 技术性（工程入口） |
| 54 | `verify:no-legacy-api-prototype` | 技术性（工程入口） |
| 55 | `verify:coding-host-protection` | 技术性（工程入口） |
| 56 | `verify:t3-coding-package` | 技术性（工程入口） |
| 57 | `verify:t3b-supplier-mock-kit` | 技术性（工程入口） |
| 58 | `verify:t3d-local-llm-ai-quality` | 技术性（工程入口） |
| 59 | `verify:external-dependency-gates` | 技术性（工程入口） |
| 60 | `verify:delivery-gates-docs` | 技术性（工程入口） |
| 61 | `verify:delivery-gates` | 技术性（工程入口） |
| 62 | `verify:nas-smb-mount-evidence` | 技术性（工程入口） |
| 63 | `verify:production-readiness-evidence` | 技术性（工程入口） |
| 64 | `docs:production-blockers` | 技术性（工程入口） |
| 65 | `verify:production-blockers` | 技术性（工程入口） |
| 66 | `verify:production-evidence-intake` | 技术性（工程入口） |
| 67 | `docs:production-owner-actions` | 技术性（工程入口） |
| 68 | `verify:production-owner-actions` | 技术性（工程入口） |
| 69 | `docs:production-evidence-submission-kit` | 技术性（工程入口） |
| 70 | `verify:production-evidence-submission-kit` | 技术性（工程入口） |
| 71 | `docs:production-evidence-submission-status` | 技术性（工程入口） |
| 72 | `verify:production-evidence-submission-status` | 技术性（工程入口） |
| 73 | `docs:production-evidence-manifest-templates` | 技术性（工程入口） |
| 74 | `verify:production-evidence-manifest-templates` | 技术性（工程入口） |
| 75 | `docs:production-evidence-owner-guides` | 技术性（工程入口） |
| 76 | `verify:production-evidence-owner-guides` | 技术性（工程入口） |
| 77 | `verify:production-evidence-submissions-guard` | 技术性（工程入口） |
| 78 | `docs:production-evidence-backfill-priority` | 技术性（工程入口） |
| 79 | `verify:production-evidence-backfill-priority` | 技术性（工程入口） |
| 80 | `docs:production-evidence-backfill-execution-ledger` | 技术性（工程入口） |
| 81 | `verify:production-evidence-backfill-execution-ledger` | 技术性（工程入口） |
| 82 | `test:ui` | 技术性（工程入口） |
| 83 | `verify:ui` | 技术性（工程入口） |
| 84 | `test:ui:update` | 技术性（工程入口） |
| 85 | `verify:ui:real-backend` | 技术性（工程入口） |
| 86 | `verify:ui:real-backend:seeded` | 技术性（工程入口） |
| 87 | `verify:android-p0-smoke-wrapper` | 技术性（工程入口） |
| 88 | `verify:full-admin-matrix` | 技术性（工程入口） |
| 89 | `test:unit` | 技术性（工程入口） |
| 90 | `verify:unit` | 技术性（工程入口） |
| 91 | `docs:domain-ownership-scope` | 技术性（工程入口） |
| 92 | `verify:domain-ownership-scope` | 技术性（工程入口） |
| 93 | `verify:matrix-vs-migration` | 技术性（工程入口） |
| 94 | `verify:migration-version-order` | 技术性（工程入口） |
| 95 | `verify:tenant-isolation` | 细化（见原文表） |
| 96 | `verify:predeploy` | 技术性（工程入口） |
| 97 | `verify:pii-encryption` | 细化（见原文表） |
| 98 | `verify:test-db-isolation` | 技术性（工程入口） |
| 99 | `docs:production-closure-gap-reports` | 技术性（工程入口） |
| 100 | `verify:production-closure-gap-reports` | 技术性（工程入口） |
| 101 | `verify:final-closed-loop-audit` | 技术性（工程入口） |
| 102 | `verify:production-evidence-negative-fixtures` | 技术性（工程入口） |
| 103 | `docs:nas-current-state-drift` | 技术性（工程入口） |
| 104 | `verify:nas-current-state-drift` | 技术性（工程入口） |
| 105 | `docs:closed-loop-current-state` | 技术性（工程入口） |
| 106 | `verify:closed-loop-current-state` | 技术性（工程入口） |
| 107 | `verify:closed-loop-runtime` | 技术性（工程入口） |
| 108 | `verify:nas-it-handoff` | 技术性（工程入口） |
| 109 | `docs:external-evidence-requirement-traceability` | 技术性（工程入口） |
| 110 | `verify:external-evidence-requirement-traceability` | 技术性（工程入口） |
| 111 | `docs:nas-diagnostics` | 技术性（工程入口） |
| 112 | `verify:nas-diagnostics` | 技术性（工程入口） |
| 113 | `docs:production-evidence-p0-quickstart` | 技术性（工程入口） |
| 114 | `verify:production-evidence-p0-quickstart` | 技术性（工程入口） |
| 115 | `docs:runtime-acceptance-evidence` | 技术性（工程入口） |
| 116 | `verify:runtime-acceptance-evidence` | 技术性（工程入口） |
| 117 | `docs:ui-experience-acceptance` | 技术性（工程入口） |
| 118 | `verify:ui-experience-acceptance` | 技术性（工程入口） |
| 119 | `docs:production-evidence-owner-execution-packets` | 技术性（工程入口） |
| 120 | `verify:production-evidence-owner-execution-packets` | 技术性（工程入口） |
| 121 | `prepare:production-evidence-drafts` | 技术性（工程入口） |
| 122 | `verify:production-evidence-draft-prep` | 技术性（工程入口） |
| 123 | `verify:production-evidence-draft-prep-fixtures` | 技术性（工程入口） |
| 124 | `verify:production-evidence-positive-fixtures` | 技术性（工程入口） |
| 125 | `verify:production-evidence-positive-propagation-fixtures` | 技术性（工程入口） |
| 126 | `verify:production-ready-no-go-fixtures` | 技术性（工程入口） |
| 127 | `verify:production-blocker-register-no-go-fixtures` | 技术性（工程入口） |
| 128 | `verify:production-ready-positive-fixtures` | 技术性（工程入口） |
| 129 | `verify:production-evidence-crosslinks` | 技术性（工程入口） |
| 130 | `docs:partial-requirement-mock-coverage` | 技术性（工程入口） |
| 131 | `verify:partial-requirement-mock-coverage` | 技术性（工程入口） |
| 132 | `docs:production-no-go-command-center` | 技术性（工程入口） |
| 133 | `verify:production-no-go-command-center` | 技术性（工程入口） |
| 134 | `docs:integration-capability-switches` | 技术性（工程入口） |
| 135 | `verify:integration-capability-switches` | 技术性（工程入口） |
| 136 | `docs:data-isolation-spec` | 技术性（工程入口） |
| 137 | `verify:data-isolation-spec` | 细化（见原文表） |

## 5. 顺从式完整18GEN表
| ID | 本席结论 | 具体证据/适用说明 |
|---|---|---|
| GEN-001 | PASS | 所有入口/忽略项用途逐项登记，当前图无不可达引用/循环。四条重叠是观察，当前匹配无错误；按正常首跑和最小动作范围不强制清理。未因HTML未写npm而删除构建配置。 |
| GEN-002 | PASS | 127shell语法与75Node目标检查通过；真实env、npm工作区转发、Playwright两个配置选择成功；缺任务/环境负例返回1。未把工具内全部业务路径混入这两个入口文件。 |
| GEN-003 | PASS | 本次断言检查入口成功/失败、发现非空套件、21路径的期望忽略布尔值及source状态。它们能在错误退出/匹配变化时失败；不以业务套件未跑谎称业务测试绿。 |
| GEN-004 | PASS | 固定命令无用户文本拼接；真实后端env标记由入口固定；无标记负例显式拒绝。本两文件无网络/DB用户输入解析实现。 |
| GEN-005 | PASS | private防误发布包；无业务授权端点、角色/权限新增。Git忽略不作为授权，未声称它可代替服务端校验。 |
| GEN-006 | PASS | 本两文件无金额/状态事务。&&链首项真实失败后不继续，未运行写业务数据的生成/DB任务；未把编排当事务替代。 |
| GEN-007 | PASS | 无业务消费者、锁或并发状态机；根脚本图无循环、组合顺序固定，无无界重试。此处不声称验证了其他模块幂等。 |
| GEN-008 | PASS | 根/工作区命令目标全部可解析，lock的name/version/workspaces/engines/devDependencies镜像匹配；已有closed-loop --verify受目标支持。 |
| GEN-009 | PASS | 缺Maven/Gradle任务、缺REAL_BACKEND_E2E、predeploy及entry-anchors坏输入均非0。未吞错/改输入伪成功；完整原因可定位。 |
| GEN-010 | PASS | 两文件无秘密值；.env/.env.local、本机配置与运行附件被忽略；.env.example保留；实际已跟踪又被忽略清单空。没有外发凭据命令。 |
| GEN-011 | PASS | 实跑输出给出入口/原因/退出码；扫描dated findings未被日志规则吞掉，审计文件不被根忽略隐藏。本报告区别“发现测试”和“执行测试”。 |
| GEN-012 | PASS | 当前127固定脚本图无递归/无界循环；本两文件无连接池/线程/缓存数据实现；未为极端场景增加限流或守护脚本。 |
| GEN-013 | PASS | Node满足当前引擎，Java/Maven/SDK真实存在，75目标语法有效，workspace目标和依赖镜像一致；如实记录npm版本差异及未覆盖环境。 |
| GEN-014 | PASS | 本两文件无SQL或直接破坏性DB操作；迁移核验入口解析到真实工具。未在本模块执行DB迁移或凭入口存在宣布迁移通过。 |
| GEN-015 | PASS | 本两文件无产品UI；CLI标识不是新增界面文案。两套UI发现验证选择器，不代替页面可访问性验收。 |
| GEN-016 | PASS | 路径/脚本/版本均核当前源，无失实“全绿/已发布”声明；documentVersion与应用SemVer区别清楚。新报告有自身V0.03。 |
| GEN-017 | PASS | 全部202行/127项依次分类；图片/证据/解释权/隔离/PII核验有准确HTML上位；其余为无业务发明的工程配置。无添加，B/C不重判。 |
| GEN-018 | PASS | 结论来自重新读取现文、当前可复现命令、实际正负输出及Git规范；不以历史PASS或本次尚未读取的Grok意见作本席依据。 |

## 6. 对抗式：本次重新执行的命令与输出
全部命令在/home/james/complex-management，通过直接SSH执行，UTF-8；没有创建新试验库/安装依赖/新脚本文件。
### E1 全量静态入口检查
重新实际运行下列Python，真实exit0：
```python
from pathlib import Path
import json,subprocess,shlex,re,hashlib
r=Path("/home/james/complex-management")
p=json.loads((r/"package.json").read_text()); w=json.loads((r/"apps/web/package.json").read_text()); l=json.loads((r/"package-lock.json").read_text())
print("WORKSPACE_PACKAGE")
print(json.dumps(w,ensure_ascii=False))
print("ROOT_LOCK_MIRROR")
print(json.dumps({k:l["packages"][""].get(k) for k in ["name","version","workspaces","engines","devDependencies"]},ensure_ascii=False))
problems=[];targets=set();edges=[];envs=[]
for name,command in p["scripts"].items():
 result=subprocess.run(["sh","-n","-c",command],cwd=r,capture_output=True,text=True)
 if result.returncode:problems.append([name,"shell",result.stderr])
 for piece in command.split(" && "):
  a=shlex.split(piece)
  if a and "=" in a[0] and not a[0].startswith("-"):
   envs.append([name,a.pop(0)])
  if a[:2]==["npm","run"]:
   target=a[2];wp="--workspace" in a
   manifest=w if wp else p
   if target not in manifest["scripts"]:problems.append([name,"missing script",target,wp])
   edges.append([name,target,"apps/web" if wp else "root"])
  elif a and a[0]=="node":
   targets.add(a[1])
   if not (r/a[1]).is_file():problems.append([name,"missing node target",a[1]])
  elif a and a[0]=="playwright":
   if not (r/"node_modules/.bin/playwright").exists():problems.append([name,"missing playwright binary"])
  else:problems.append([name,"unhandled command",a])
for target in sorted(targets):
 c=subprocess.run(["node","--check",str(r/target)],cwd=r,capture_output=True,text=True)
 if c.returncode:problems.append([target,"node syntax",c.stderr])
seen=set();active=set()
adj={k:[e[1] for e in edges if e[0]==k and e[2]=="root"] for k in p["scripts"]}
def walk(n):
 if n in active:problems.append([n,"script cycle"]);return
 if n in seen:return
 active.add(n)
 for t in adj[n]:walk(t)
 active.remove(n);seen.add(n)
for n in adj:walk(n)
print(json.dumps({"scripts":len(p["scripts"]),"node_targets":len(targets),"all_targets":sorted(targets),"forward_edges":edges,"environment_assignments":envs,"problems":problems},ensure_ascii=False))
for args in [["node","--version"],["npm","--version"],["git","ls-files",".env*","**/.env*"]]:
 c=subprocess.run(args,cwd=r,capture_output=True,text=True);print(json.dumps({"command":args,"exit":c.returncode,"stdout":c.stdout,"stderr":c.stderr},ensure_ascii=False))
assert not problems

```
本次输出摘要：scripts127、node_targets75、problems[]；全部shell语法与75次node --check均0；所有根/工作区转发目标存在、根脚本图无环；唯一环境赋值REAL_BACKEND_E2E=1。根lock镜像与manifest字段一致。当前node v24.20.0、npm11.19.0；受控.env匹配清单仅.env.example。

### E2 真实入口/负例/忽略断言
本次实际运行下列完整Python，真实exit0，末行M003_B5_ENTRY_ASSERTIONS_PASS：
```python
from pathlib import Path
import subprocess,json,hashlib,os,re
r=Path('/home/james/complex-management')
commands=[
 (['npm','run','verify:backend-env'],0),
 (['npm','run','verify:android-env'],0),
 (['npm','run','verify:ui','--','--list','--reporter=list'],0),
 (['npm','run','verify:ui:real-backend','--','--list','--reporter=list'],0),
 (['npm','run','typecheck','--','--','--dry'],0),
 (['node','tools/run-backend-maven.mjs'],1),
 (['node','tools/run-android-gradle.mjs'],1),
 (['node','node_modules/@playwright/test/cli.js','test','--config=playwright.real-backend.config.ts','--list','--reporter=list'],1),
 (['npm','run','verify:predeploy'],1),
 (['npm','run','verify:entry-anchors'],1)
]
for cmd,expected in commands:
 env=os.environ.copy()
 if cmd[0]=='node' and 'node_modules/@playwright/test/cli.js' in cmd:env.pop('REAL_BACKEND_E2E',None)
 z=subprocess.run(cmd,cwd=r,capture_output=True,text=True,env=env)
 if '--list' in cmd and expected==0:
  summary=re.findall(r'^Total:.*$',z.stdout,re.M)
  print(json.dumps({'command':cmd,'exit':z.returncode,'stdout_summary':summary,'stderr':z.stderr},ensure_ascii=False),flush=True)
  assert summary
 else:
  print(json.dumps({'command':cmd,'exit':z.returncode,'stdout':z.stdout,'stderr':z.stderr},ensure_ascii=False),flush=True)
 assert z.returncode==expected,(cmd,z.returncode)
samples=['.env','apps/web/.env','apps/web/.env.local','.env.example','node_modules/x.js','apps/web/node_modules/x.js','backend/target/x.class','apps/android-terminal/.gradle/cache','apps/android-terminal/.kotlin/cache','apps/android-terminal/build/out.class','apps/android-terminal/local.properties','security-scans/dated/results.json','security-scans/dated/run.log','security-scans/semgrep-current/findings.json','docs/_archive/old/run.log','debug-one.mjs','tools/debug-one.mjs','probe-one.mjs','backend/data/bar-images/a.png','backend/data/evidence-files/a.pdf','docs/audit-2026-09/findings.md']
unignored={'.env.example','security-scans/dated/results.json','tools/debug-one.mjs','docs/audit-2026-09/findings.md'}
z=subprocess.run(['git','check-ignore','--no-index','--verbose','--non-matching','--stdin'],input='\n'.join(samples)+'\n',cwd=r,capture_output=True,text=True)
print(json.dumps({'command':['git','check-ignore','--no-index','--verbose','--non-matching','--stdin'],'exit':z.returncode,'stdout':z.stdout,'stderr':z.stderr},ensure_ascii=False))
assert z.returncode==0
assert len(z.stdout.splitlines())==len(samples)==21
for row in z.stdout.splitlines():
 rule,path=row.split('\t',1);assert (rule=='::')==(path in unignored),(rule,path)
for cmd in [['git','ls-files','-c','-i','--exclude-standard'],['git','status','--porcelain','--','.gitignore','package.json'],['git','rev-parse','HEAD']]:
 z=subprocess.run(cmd,cwd=r,capture_output=True,text=True)
 print(json.dumps({'command':cmd,'exit':z.returncode,'stdout':z.stdout,'stderr':z.stderr},ensure_ascii=False))
 assert z.returncode==0
 if cmd[-1]!='HEAD':assert not z.stdout
for f in ['.gitignore','package.json','docs/audit-2026-09/plan/criteria-lock.md','docs/business-system-map-for-operations-v0.1.html','docs/platform-governance-map-for-operations-v0.1.html','docs/business-enum-quick-reference-v0.1.html']:
 print('SHA256',f,hashlib.sha256((r/f).read_bytes()).hexdigest())
print('M003_B5_ENTRY_ASSERTIONS_PASS')

```
输出逐命令摘要：
| 本次命令 | 实际退出/关键输出 |
|---|---|
| npm run verify:backend-env | 0；后端环境前置校验通过；openjdk21.0.12.1、Maven3.9.11 |
| npm run verify:android-env | 0；Android环境前置校验通过；SDK /home/james/android-sdk |
| npm run verify:ui -- --list --reporter=list | 0；Total: 32 tests in 6 files |
| npm run verify:ui:real-backend -- --list --reporter=list | 0；Total: 18 tests in 4 files |
| npm run typecheck -- -- --dry | 0；tsc -b --noEmit --dry；2:19:33 PM - A non-dry build would build project '/home/james/complex-management/apps/web/tsconfig.json' |
| node tools/run-backend-maven.mjs | 1；缺少Maven执行目标 |
| node tools/run-android-gradle.mjs | 1；缺少Gradle执行任务 |
| 删除REAL_BACKEND_E2E后直接node本地Playwright cli+real配置+--list | 1；“真实后端 E2E 须设置 REAL_BACKEND_E2E=1，请使用 npm run verify:ui:real-backend。” |
| npm run verify:predeploy | 1；首项html-ledger-sync拒绝BSM已变更而决策台账未同步；后续链未运行 |
| npm run verify:entry-anchors（独立调用） | 1；6处BSM CSV镜像不合格，完整分类如下 |
| git check-ignore --no-index --verbose --non-matching --stdin | 0；21/21输出，4个预期不忽略均“::”，其余17个按声明规则匹配；每项布尔断言通过 |
| git ls-files -c -i --exclude-standard | 0，输出空 |
| git status --porcelain -- .gitignore package.json | 0，输出空 |

对抗问答：
1. 已存在入口会不会把坏输入当成功？三个负例和真实门禁均1；链首停止，不伪造后半链通过。
2. npm多层转发有没有把--dry交错层？本次明确两个分隔层，实际tsc命令带--dry；不沿用B1初次错误写法。
3. mock入口是否错误包含真实后端用例？实际发现mock32/6，real18/4，配置文件/目标不同；未启动业务测试。
4. ignore会否吞掉示例或正式工具？当前.env.example、dated results.json、tools/debug-one.mjs、audit findings4负例均不忽略，根debug-one被忽略；真实输出与声明范围一致。
5. 后置递归模式是否增加保护？按[Git官方gitignore模式规范](https://git-scm.com/docs/gitignore#_pattern_format)是前项子集或等价，当前样本无错；没有因此证明工程故障，保持观察，不为清理而改文件。

### E3 全局红灯完整范围
本次predeploy首项输出：
```text
[html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。比对范围：工作区（含未提交改动）↔ 基线 origin/main@242f4872。
  已改动的 HTML：
    - docs/business-system-map-for-operations-v0.1.html
  必须在同一批改动内一并更新：docs/prd-audit/business-decision-ledger.md
```
本次独立entry-anchors输出6项：
- BSM-E0433 注册表回访概括在现HTML找不到。
- BSM-E0736 注册表“文档版本”镜像对不上。
- BSM-0283账本source_text回访概括对不上。
- BSM-0530账本“文档版本”镜像对不上。
- BSM-0283 preconditions仍含旧回访句。
- BSM-0283 state_transition仍含旧回访句。
这仍是4处A回访概括、2处此前M001-012页脚日期镜像（非D版本注释），对应CSV属于M425/M427。没有新HTML自身问题；此处只确认调用正确暴露问题，不把外部坏数据定成package入口缺陷，不修这些文件，不扩M003范围。用户B/C原句就是其原意，保持不判缺陷。

## 7. 完整本席结论
三线全部完成；202/202行、127/127脚本、18/18冻结GEN已覆盖。当前无新增工程缺陷、无冲突、无业务添加、无新HTML自身问题；全部内容按技术性/细化正常审计而非因HTML沉默删除。当前重复观察不会作为“有缺陷拒修”残留。
本席B5 PASS。未看本次Grok正文，不能以本席PASS宣布双PASS或模块收口。下一步保存此完整报告后读Grok本次完整报告，核验实算/sidecar/ledger/Grok四值；两席确实PASS才能先写ledger PASS再写模块收口文件。未修改源码或增加B3次数。
