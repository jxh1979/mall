CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
# M003 B5-r1 本方修复后全量审计

版本：V0.01

时间：2026-09-05T10:59:24.492897+00:00

结论：FAIL。完整重审8文件621行、18项冻结GEN；发现3项临时finding。源码未修改，B3累计1。先派Grok后本方独立完成当前报告；写入本报告前未读本轮Grok raw，也未接触其原始JSON。必须等待两席本次完整raw齐全再统一回B2。本报告不是B4 diff复核，也不是用前次PASS替代整模块审计。

## 输入固定与覆盖

|文件|行数|字节|SHA256|
|---|---:|---:|---|
|.env.example|22|893|e377b54d9cd829ddeed30e0ae730f55d51fbc8c272f3df5ca611fdbdba125049|
|.gitignore|55|1199|75861ded5d3859bcda593aafe3f3862e765470e7f8b6636a643bf59de9d7c51b|
|.semgrepignore|6|253|45f3dbc9f36891b4b94d2b641fabfd66db81df59b642bcde80a29bece24f1aba|
|AGENTS.md|74|3662|0635c31192e2bb20338e8091fbd40399f854fe722009683b2f0298298a81f14c|
|CONTEXT.md|163|10197|d5acdb1c3eb174c532983c823931d1cbe36444712e379b755991243f75df52b7|
|PROGRESS.md|36|3246|91eaedfc0c8428514355b82a79bd1b6add257bc80269e831d2e76aafbc0baf9d|
|README.en.md|116|7492|e3efb3f26bb3785ad9a31adb214515f5df6f5ff08ed79a90b4ed9224abb64355|
|README.md|149|10871|7687e5c1f376ac623243652de20b82c664d0fb694558f6402067700919ef0e4e|

控制V0.17 SHA c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21；锁V0.06，COUNT0。M001/M002已发布模块不重开；三HTML这里只读用于上位语义对照。不是以实现反向解释业务。未连接业务数据库，未执行发布/生产操作；没有改实际Git索引。

## 每文件完整三线与三个问题

### .env.example:1-22

必要性：提供本地开发启动的可复制配置，不发明业务默认值。

顺从：API/DB/pool/build、本地profile及管理员种子字段与既有配置入口对照，PII变量现在不赋空值；仅local/test缺省使用dev密钥，生产拒绝dev值。

对抗与第一性：复制旧空PII会压过默认，当前删除空赋值确实恢复缺省；B4同字节真实Spring和Java校验证据见下；明确local-only示例密码不误作生产密钥。不可验证前提：本轮未启动完整数据库/服务，不声称成功全系统启动。技术性逐项：版本注释、profile、网络地址、连接池、构建元数据、本地管理员及加密配置均为已有工程入口，未增业务身份或金额规则。

### .gitignore:1-55

必要性：排除构建、缓存、运行态附件与本机配置；保留可交付源文件。

顺从：逐行检查node/Maven/Gradle/Android输出、env、日志、会话配置、根临时探针、bar-images/evidence-files；与实际目录用途一致。

对抗与第一性：没有通配忽略整个业务源码/审计证据，dated审计JSON/SARIF/MD未被日志规则整体排除。历史原型db排除不等于声称SQLite正式存在。无可执行分支，不声称执行扫描。55行均技术性、无业务新规则；未发现需改事项，保留M003-016。

### .semgrepignore:1-6

必要性：明确扫描解析限制/重复大型生成物的忽略清单。

顺从：6行原样完整读取；OpenAPI两件生成物、归档、keyvault脚本被排除，非全应用排除。

对抗与第一性：扫描覆盖取舍不能伪装已扫描，但也不能仅因忽略即创设“所有文件必须Semgrep”新判据；B2已把旧GR14“exit0即解析成功”错误撤回，当前没有新增证明推翻。各条为工程工具范围、无业务发明；M003-017仅登记，不声称所有被排除文件都绝对无法由任意解析器解析。

### AGENTS.md:1-74

必要性：记录该仓库协作、权威和验证入口。

顺从：1-21 issue/triage/domain链接；23-40 Git及PRD同步；42-59中文、三HTML上位、配置主数据/账户例外；61-74迁移串行、测试库隔离及skipTests部署逐条重读。对应propose/merge脚本、实际package门禁层级及CI证据沿用输入未变的B1/B4事实。

对抗与第一性：不得把平台管理员等同最高审批人，当前52已纠正且53生命周期例外明确；迁移gate属于delivery-gates而非predeploy，67事实仍正确；额外1空格不改变Markdown语义，不新开格式修复。main不提交/PR策略不覆盖当前用户控制B3就地审计，发布仍feature worktree。无新业务发明；未知：没有本轮真的发布来“验证”文档命令。

### CONTEXT.md:1-163

必要性：提供统一业务词表，须服从三HTML已确认规则。

顺从：1-12 authority；14-45平台/项目/业态/商户；47-93会员、成长、积分、待追回、零售/结算/结算中心/营销；95-135绑定、权限、临时、高危、外协/审计；137-163开关、十一终端、留痕、存档删除。全部定义、边界、允许/禁止别名逐行对照。023删除L4、024保留营销得到实际答复；营销BSM1725确有上位。

对抗与第一性：高危119仍为旧审批分档，与PGM660专门链生成语义不一致（CX-01）；PGM652一般敏感层级没有被当作授权覆盖660。其余已修项目多对多/公司财务边界/手机号/配置取价窗口/非独立结算中心/内部价范围/十一端/留痕例外/存档粒度未复发。未展开全部业务实现模块；本模块是术语正确性，不声称运行了授权全链。

### PROGRESS.md:1-36

必要性：保留退租结算分支的历史实施证据，不把它当当前模块验收。

顺从：1-4明确历史建设/V0.01；5-15目标/零迁移/幂等设计，17-28历史改动点；30-36门禁和当时状态。当前所列源码/端口/服务触发点在B1已核对且此轮未修改这些实现。

对抗与第一性：35明确测试数字不代表当前工作区；不因为7/32/15与现在数量不同改历史，也不把“当时零迁移”强制成当前无任何V303。没有新执行算法或写DB；本轮无后端整套测试结论。历史建设描述为技术实现/已有退租业务细化，不产生新的结算权利义务。

### README.en.md:1-116

必要性：英文研发与供应商入口，不是产品运行界面，中文产品文案要求不禁止开发文档翻译。

顺从：1-25定位/构建版本；27-41目录；43-78全部启动验证命令；80-82统一基线链接；84-98架构；100-116读者入口/生产证据边界。逐项对当前package/pom以及实际路径；没有沿用旧V124/766统计。

对抗与第一性：33与90让Guest用Web，CX-02；16称Data JPA而pom无该依赖，CX-03。98典型受保护写入不再误称登录都用GuardedAction。命令存在≠本轮所有门禁执行；MOCK_IS_NOT_PRODUCTION_EVIDENCE边界保留，不给生产通过结论。业务概览有三HTML上位，技术目录、工具、门禁无业务发明。

### README.md:1-149

必要性：中文仓库第一入口，准确带读者到当前源码和权威文档。

顺从：1-27定位/技术栈；29-51目录；53-94全部启动/验证；96-110生成基线/快照；112-127路径地图；129-149架构/生产边界。实际baseline check为720/V385/363，未手改生成块；99说明报告快照不能证明本轮测试执行。

对抗与第一性：135客人Web冲突归CX-02；18-19不实DataJPA归CX-03。技术表其余版本与当前pom/package一致。143典型写入与认证例外一致；目录迁移真实；所有明确Markdown链接已在B4同字节核查。未知：此模块审计不构成全后端、端到端、生产联调通过。

## 18项冻结GEN逐项结论

|判据|本模块核查结果与边界|
|---|---|
|GEN-001|8文件均有具体入口/配置/术语/历史记录用途；删除023L4，无新抽象；其余不按喜好格式化。|
|GEN-002|shell语法和基线校验实际成功；npm键存在；没有声称全部运行命令均已执行。|
|GEN-003|PII旧空值负例/本地缺省/生产拒绝为真实目标代码断言，引用同字节B4记录；本模块不伪造后端绿数。|
|GEN-004|env首次使用依现有Spring/KeyVault校验；文件本身无新增外部输入处理器；无新越界输入逻辑。|
|GEN-005|当前主数据最高审批/账户例外正确；高危旧分档与当前指定最终人链语义矛盾CX-01。|
|GEN-006|历史退租记录描述幂等/回写；本模块没有新状态写入或金额计算，实现模块另审，未发现本批新完整性逻辑。|
|GEN-007|PROGRESS幂等设计为历史记录，不据此证明并发算法全通过；忽略列表无竞态执行。|
|GEN-008|发现授权术语CX-01、客人终端CX-02、技术组件CX-03不一致；其余已修语义本轮复核未复发。|
|GEN-009|本地密钥缺省与生产拒绝有反例；失败门禁不宣称可上线，开关留痕特例已恢复。|
|GEN-010|模板示例账号限制local，秘密env文件忽略；不把模板复制到生产；敏感高危授权见CX-01。|
|GEN-011|快照/历史绿数时态澄清仍生效；完整独立raw含输入SHA/行数/命令，非概括替代证据。|
|GEN-012|连接池示例具有限值；本批无循环/线程/资源创建；B4真实测试临时目录自动清理。|
|GEN-013|Java/Spring/React/Vite/TS/Vitest等实际声明核对，DataJPA例外CX-03；不添加依赖使错误文档成真。|
|GEN-014|迁移目录/当前V385/363与声明一致；历史V303未用不充当当前事实；本模块无新增迁移。|
|GEN-015|Markdown结构/链接无阻断，AGENTS中文要求针对产品；开发英文入口不构成产品英文；图角色语义CX-02。|
|GEN-016|8文件全行审，基线/路径/版本/命令实证；旧高危和客人Web、JPA共3项不可作为当前事实。|
|GEN-017|冲突→细化→技术性→添加顺序；CX01/02冲突，CX03技术缺陷，其余登记详列；024依HTML+实际保留不再误判业务发明。|
|GEN-018|新缺陷落到PGM660/739原文与当前完整pom/精确rg；不以常识、注释、历史PASS作为最终依据。|

## 本次全部findings（未归并定稿，不修复）

### CX-01 — 高危授权边界仍用固定审批档位，未按现行高危链生成口径

位置：CONTEXT.md:116-121（关键119）

2.1分类：冲突

2.3依据类型：1（HTML原文）

冻结判据：GEN-005, GEN-008, GEN-016, GEN-017, GEN-018

既有登记与路由：内容原登记在M003-019（当前队列否）；B2须沿用现有ID并限定新确认的修复片段，不把整个登记段改写。

不可拆证据：CONTEXT119：授出前必须先审批才生效，按上一级、上两级或最高权限分档。PGM652允许一般敏感动作要求不同审批层级，但PGM660对高危授权专门明确：审批链按只指定最终审批人、系统自动补全中间层级生成，不由发起人选择档位。通用层级存在不证明高危授权仍应分成三档；当前高危段没有写链生成方式，旧分档表述会让执行者实现旧模型。

对抗反例：实现者按词表为高危授权提供上一级/上两级/最高权限三个审批档位，按选定档位跳过其余层级；满足当前119的表面文字，却违反660专门规则。这里不把652自身判错，不申请更改HTML。

候选最小处置：B2确认后删除119中旧的“按上一级、上两级或最高权限分档”片段；可直接依现行HTML明确最终审批人/自动补全，但不得引入新审批条件。

### CX-02 — 中英文入口把消费客人引到Web，违反客人无网页端

位置：README.md:135；README.en.md:33,90

2.1分类：冲突

2.3依据类型：1（HTML原文）

冻结判据：GEN-008, GEN-016, GEN-017, GEN-018

既有登记与路由：这些段落原在M003-018的其余入口整体登记范围内（当前队列否）；没有另一个针对客人Web的正式ID。B2应按现有ID与内容身份核对，不能为躲避回退直接动手。

不可拆证据：PGM739受控十一端明确Web前端为业务人员、客人无网页端；当前CONTEXT146也如此。README135是GUEST→WEB(admin/customer)，英文90同样Guest→Web(admin/customer)，英文33把apps/web称admin/customer web。图直接声明的业务入口对象与HTML相反，不能以历史customer目录或别的架构文档作为业务授权。

对抗反例：供应商只按README图为消费客人建设customer Web入口；即使现有目录有customer字样，也不能反向授权客人网页端。图是当前系统上下文，不是历史原型示例。

候选最小处置：B2后删除客人→Web关系和customer Web标签；保留业务人员Web、员工Android、API/DB等正确图节点。若保留客人节点，只可依HTML连接手机应用/小程序，避免额外业务发明。

### CX-03 — 中英文技术栈仍声称使用Data JPA，当前POM只有JDBC

位置：README.md:18-19；README.en.md:16

2.1分类：技术性（有工程缺陷）

2.3依据类型：2/4（实际可执行构建定义与复现命令）

冻结判据：GEN-008, GEN-013, GEN-016, GEN-017, GEN-018

既有登记与路由：M003-018已登记这些技术栈行且曾误称版本与pom一致，当前队列否；应在B2纠正该ID的工程缺陷证据，不覆盖旧报告。

不可拆证据：完整读取backend/pom.xml，59-60声明spring-boot-starter-jdbc；未声明spring-boot-starter-data-jpa。精确rg查backend/pom.xml和backend/src/main所有Java/XML，无starter-data-jpa/jakarta.persistence/JpaRepository/EntityManager命中。README18和英文16仍列Data JPA，README19还直接把starter-data-jpa写成pom实证来源。数据库实际PostgreSQL、SpringMVC/Security/Validation/Actuator/AOP保持正确。

对抗反例：新开发者按README以为仓库已有JPA持久化支持，直接编写JpaRepository/Entity代码，实际POM不具该依赖。不能为了使文档正确反向新增JPA依赖。

候选最小处置：只把不实Data JPA/starter-data-jpa改成当前JDBC/starter-jdbc；不增加依赖、不改构建或业务实现。无业务发明理由：这是现有构建组件事实，不规定角色、收费、状态、流程或业务数值。

## 本次命令与实际输出

技术证据采集：完整读取8源文件和18GEN；PGM650-663/735-740精确读取；读取package.json、apps/web/package.json与backend/pom.xml；执行bash -n .env.example、node tools/refresh-entry-doc-baseline.mjs --check、git diff --check。下列是实际输出，不是预计结果：

```text
platform-governance-map-for-operations-v0.1.html:650:                 <li>支持角色级权限模板。</li>
platform-governance-map-for-operations-v0.1.html:651:                 <li>支持员工级完全自定义配置。</li>
platform-governance-map-for-operations-v0.1.html:652:                 <li>敏感动作可要求上一级、上两级或最高权限审批。</li>
platform-governance-map-for-operations-v0.1.html:653:                 <li>所有业务数据默认按项目隔离，普通岗位没有跨项目查看权；只有项目之上的获授权岗位才可以跨项目汇总。一个岗位能查看本项目普通业务数据，不代表能查看其他项目或敏感经营数据。数据的项目与商户隔离不提供关闭开关，与导出脱敏同例。</li>
platform-governance-map-for-operations-v0.1.html:654:                 <li>敏感经营数据不限于内部结算价和成本价，还包括采购价、毛利、分账底价、域内补贴缺口、供应链价格以及其他被标记为敏感的数据。查看、导出和修改分别授权；普通用户、会员、前线员工以及没有相应价格权限的商户人员不得查看。系统自动使用某个价格完成计算，不代表当前操作人取得查看或修改权限。</li>
platform-governance-map-for-operations-v0.1.html:655:                 <li>最终权限 = 角色权限 + 基础模板 + 有效临时模板 + 员工单独授予 − 显式拒绝；显式拒绝优先级最高。</li>
platform-governance-map-for-operations-v0.1.html:656:                 <li>临时授权可设有效期或单次使用；权限的授予、收回、到期、单次消耗、失效全部留痕。</li>
platform-governance-map-for-operations-v0.1.html:657:                 <!-- 2026-06-25 修改阶段：补角色装配与高危授权运营细则，依据 M1 需求采集（complex-m1-requirements §7）。 -->
platform-governance-map-for-operations-v0.1.html:658:                 <li>角色装配（已确认）：人力中心、财务中心与法务由整个平台共享；人力中心与财务中心可能分设在不同公司。在酒吧设人力业务伙伴（HRBP）与财务业务伙伴（财务 BP），设几名、由几家酒吧共用按实际规模与需求确定，也可以由几家酒吧共用一名。单店层面的财务工作——日结确认与锁定、商户费用账单出账、差异认定、异常单据处置、报损物品的财务确认——由该酒吧对应的财务 BP 负责；跨主体与平台层面的财务工作——三方对账、分账复核、封账、开票、付款执行、守恒校验不通过时的追查——由财务中心负责。平台管理员配置各岗位权限模板，并定「职位/层级对应哪个模板」；HR 部门定每个人的层级/职位，定完系统自动适配对应角色和权限模板，无需逐人配权限。保存岗位、职责或同一商户、同一项目范围内的所属组织变更时，系统立即按变更后的岗位权限模板重算有效权限，旧岗位或旧范围派生权限同步失效，同一商户、同一项目范围内的普通模板权限随保存立即生效。保存所属商户或发生跨项目变更时，旧岗位或旧范围派生权限立即失效，新范围权限每次均须先审批，审批通过后方可生效；属于既有高危权限的，不得因处于岗位模板内而免除授出前审批；超出新模板范围的增权保持未生效，审批通过后方可生效。</li>
platform-governance-map-for-operations-v0.1.html:659:                 <li>临时权限（已确认）：员工可申请临时权限，按层级对应主管审批（一级/二级/三级直到最高），有时间限制、到期自动失效；某岗位层级频繁申请同类临时权限时系统自动总结、建议平台管理员转为固定权限，某个人频繁申请时提交异常总结供复核。</li>
platform-governance-map-for-operations-v0.1.html:660:                 <li>高危授权审批（已确认）：以下五类权限授出前须先审批才生效——资金/核销/退款/调账/分红发放类、提权或跨商户跨项目、敏感经营数据的查看/导出/修改、高危业务（沽清/反结/存酒作废）；税费规则与税费调整。审批链按上述“只指定最终审批人、系统自动补全中间层级”的方式生成，不由发起人选择档位。持股分红的方案配置、算单、审批和发放属于资金类高危动作，由最高权限审批，提交人不能审批自己的单据。税费规则与税费调整须经财务审核、税务审核、法务审核，再由顶层平台公司的老板最终确认后生效，提交人不得审批自己提交的调整。查看内部结算价、查看企业微信会话完整原文、导出客人或员工个人信息与结算价格数据这几类高敏能力，不因“平台管理员”身份自动获得，必须逐项单独授予并留痕。新上线的功能必须在上线前先归类：归入上述高危类别之一，或者明确定为日常功能；没有完成归类的一律先按高危处理，需逐项单独授予并留痕，不得因为“还没定”就当成日常功能自动放给平台管理员。归类结果日后要改，须重新走一次同级审批并留痕。</li>
platform-governance-map-for-operations-v0.1.html:661:               </ul>
platform-governance-map-for-operations-v0.1.html:662:             </div>
platform-governance-map-for-operations-v0.1.html:663:             <div class="info-block">
platform-governance-map-for-operations-v0.1.html:735:                 <!-- 2026-08-04 第三轮用户裁决（DEC-R6-07）：补开关默认口径——后台无显式开关配置行时一律默认关闭（opt-in），缺行不得视为开启；与 BSM 已拍板第 35 条同步 -->
platform-governance-map-for-operations-v0.1.html:736:                 <li>开关默认取值：任何能力在后台没有显式开关配置行时一律默认关闭（opt-in），要启用必须有人显式打开；开关缺行不得视为开启，也不得由代码或部署脚本代为「视同打开」。这与拉取失败时的「兜底关闭」是两件事——兜底关闭针对读不到配置的运行时故障，本条针对配置里压根没有这一行。</li>
platform-governance-map-for-operations-v0.1.html:737:                 <li>与钱有关的规则——分账比例、结算口径、综合服务费、租金与抽成规则——一律不得使用灰度，也不得按商户、按设备做部分开放，只能按综合体整体统一切换；切换日期由后台配置（业务参数，按综合体统一可配，须同时配置上下限），切换前后各自适用完整的一套规则。灰度只用于不影响金额计算的功能开放。本条禁止的是规则的整体切换（换一套分账、结算、服务费或租金抽成规则）：必须按综合体整体统一切换、不得灰度、不得按商户按设备做部分开放。同一套规则之下的参数取值，按本文件为该参数写明的粒度配置，不属于灰度或部分开放。</li>
platform-governance-map-for-operations-v0.1.html:738:                 <li>开关服务、支付通路、核销与入场等营业必需的能力，不得在营业时段进行任何可能导致中断的变更、升级或维护，所有此类操作只能安排在非营业时段并事先报备；开关拉取失败时按“兜底关闭”处理，写操作一律拒绝；只有留痕是例外——留痕是一种特殊的写操作，不受兜底关闭影响，开关拉不到期间发生过什么、谁被拒绝了，照样要记下来。前端隐藏只是辅助，后端接口必须独立校验。</li>
platform-governance-map-for-operations-v0.1.html:739:                 <li>受控的十一类终端：客人手机应用、客人小程序、Web 前端（业务人员，客人无网页端）、工作人员平板、员工工作手机、员工私人手机、商户网页门户、商户手机应用、触摸屏一体机、移动管理端、独立经销商 Web 门户。经销商门户适用与其他终端相同的后台开关、权限、数据范围、访问拒绝留痕和兜底关闭规则，并与商户门户及内部员工端隔离账号、权限和数据。</li>
platform-governance-map-for-operations-v0.1.html:740:                 <!-- 2026-07-03 回写：设备分级放权(员工App已上线口径)。 -->
NOTFOUND M003-018
NOTFOUND M003-019

当前package声明核对：Java21、SpringBoot4.1.0、springdoc3.0.3、React^19.0.0、antd^5.22.5、TS^5.7.2、Vite^8.0.16、Vitest^4.1.9、Playwright^1.61.0、Node>=22；README全部具体npm run键均在root scripts存在。
backend/pom.xml f4a4a05ee21aa7c555a7bf0b11c7509a0963bdd22397e4dacfce3db89b039701
9         <artifactId>spring-boot-starter-parent</artifactId>
10         <version>4.1.0</version>
16     <version>0.1.0-SNAPSHOT</version>
21         <java.version>21</java.version>
22         <springdoc.version>3.0.3</springdoc.version>
32                 <version>2.21.5</version>
37                 <version>2.21.5</version>
45             <artifactId>spring-boot-starter-actuator</artifactId>
47         <!-- AOP：动作前置闸口切面 ActionGateAspect。离线 .m2 无 spring-boot-starter-aop，
56             <artifactId>spring-boot-flyway</artifactId>
60             <artifactId>spring-boot-starter-jdbc</artifactId>
64             <artifactId>spring-boot-starter-security</artifactId>
68             <artifactId>spring-boot-starter-validation</artifactId>
72             <artifactId>spring-boot-starter-webmvc</artifactId>
75             <groupId>org.flywaydb</groupId>
76             <artifactId>flyway-core</artifactId>
79             <groupId>org.flywaydb</groupId>
80             <artifactId>flyway-database-postgresql</artifactId>
88             <groupId>org.springdoc</groupId>
89             <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
90             <version>${springdoc.version}</version>
95             <artifactId>spring-boot-starter-test</artifactId>
100             <artifactId>spring-boot-webmvc-test</artifactId>
114                 <artifactId>spring-boot-maven-plugin</artifactId>
CHECK ["bash", "-n", ".env.example"] 0  
CHECK ["node", "tools/refresh-entry-doc-baseline.mjs", "--check"] 0 [entry-doc-baseline] PASS: 快照与实际一致（720 ops / V385（363 迁移））。
 
CHECK ["git", "diff", "--check"] 0  


```

追加JPA反例：rg -n starter-data-jpa|jakarta.persistence|JpaRepository|EntityManager backend/pom.xml backend/src/main -g *.java -g *.xml，无命中；完整pom直接证明starter-jdbc，未声明DataJPA。先误用不存在短文件名platform-governance-map-v0.1.html的只读请求得到MISSING；随后rg --files确认真实for-operations文件并读上述原文，不依据MISSING作业务结论。

## 复用但不伪称重跑的同字节运行证明

B4-r1已经真实编译当前三Java源码并调用Spring环境/KeyVault，输入在B5未变；以下明确是本轮较早B4的真实执行结果，不是B5重新执行：

```text
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
```

## 全批完整性与下一阶段

19个B3-r1已修ID001-015/020-023本轮未见原缺陷复发；016/017登记继续保留；018/019首次全量登记时漏掉了本次新确认的具体事实，当前均队列否。024营销成本实际答复保留、HTML有上位，无需改写。3项CX全数列出，其他范围也完整审完后才给FAIL。Grok本次结果未知，任何一席FAIL不能收口。两席完整B5-r1报告齐全并完成B5后四值核对后，整批回B2核定这些非队列已有ID/真正无ID事项；不得直接B3或边审边修。B3=1、无源变更，无M004启动，无业务HTML改动，无新判据。


## 报告证据类型澄清

版本：V0.02

CX-03的有效依据类型为4（完整POM与精确检索命令及实际输出）。上文“2/4”中的2不适用：本次并没有实际运行JPA依赖失败的编译用例，也不把读取构建定义称为运行行为证明。POM声明组件与README声称组件的直接差异已由类型4复现；不依赖猜测的运行失败立论。原始报告完整保留，三项finding及全量FAIL结论不变；本澄清在读取本次Grok报告之前完成。
