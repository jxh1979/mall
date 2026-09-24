# M009 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段
版本：V0.04

当前：PASS。B5-r6双席整模块审计与四值校验均通过，已先登记ledger；B3累计6，本次操作员批准追加一次发布修复，实际只执行1次。原历史收口全文保留下方。

本次3项发布缺陷均修复：060未发布V427纯改名V432（SQL字节不变）；061原生成器机械刷新CSV及自身版本；062原工具刷新隔离规格AUTO及自身版本。063原测试库保持历史，只作验证边界。当前30ID队列全否，不回开已修013/038。没有新增业务或修改三HTML。
范围仍20源3381行/88判据，两席逐源SHA核同字节复用已读全文后重新判断，未冒称本次新读3381行；发布所需Auth/权限/审批/Leasing直接依赖也经双席核对。M011当前双席报告与其已审测试随同发布，不冒称其它模块全审完。

## 本次证据
- [B4 Codex](M009-b4-r6-self-raw.md) V0.01 SHA98547a2468c655ae3a12422e0b730d0b0c4a8d15dd05fdaa4179d0cc165eda24。
- [B4 Grok](M009-b4-r6-grok-raw.md) V0.01 SHAa41c1d2ef347559f1855ea2c6d5bffab2b87244f1118493615bab2545947d743。
- [B5 Codex](M009-b5-r6-self-raw.md) V0.01 SHAba855ec799942cc9e3f2e01df735ccffe23ae706615da2bbf91704e16f5615b4。
- [B5 Grok](M009-b5-r6-grok-raw.md) V0.01 SHA29eb43323a2fc9ae0cfc54145b31ce2f2d40318bc6c6cc05e454481a545cf8af；真实EXIT_CODE=0。
- B3 before6919c011af93e31ccbba13f57398f821b13c8c15 / aftere2871dc1226a626aa47b6f33ab2d805ec1a542ff，完整125823字节diffSHA108fb84a3facc14d745f2d878bc906ce4a1b962038343d94aa22a4d9b00857af，两席完整内嵌并逐块归因。

## 验证及发布
本次独立空库全399迁移到432、430升级只执行431/432两条并validate399，均成功；旧项目/商户原字段哈希保持、新列NULL、bep PK/2FK/索引正确。原complex_test历史MD5仍6258acd4a7f89f16d545dcf0048bbbc5，未repair/改history或业务行，不称该旧未发布库已衔接432。
编号闸、两清单check、候选完整predeploy通过；1199生产Java/399SQL与正本逐字一致。299测试零失败/错误/跳过为本次发布准备阶段的既有同源clean结果，本B5未重复跑；本次迁移另行实跑。
四值全等b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458，LOCK-AMEND-COUNT1。三HTML与真实index均未改。
发布分支audit/20260922/m009-m011-publish，将本报告及已审源码/测试/迁移经PR和CI合并。实际是否合并以GitHub该分支PR状态为准；本文件不预写尚未发生的合并。用户授权已覆盖提交推送合并，不部署。
项目三资料仅后端/DB、可空，旧姓名不回填；其它模块观察保留。不是全仓或生产发布验收。

## 历史V0.03全文（原内容保留，非当前发布状态）

# M009 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段
版本：V0.03

状态：PASS。当前现锁B5-r5双席完成，B3累计5（第5批为操作员明确追加一次授权）。20文件3381行、88判据、26项当前队列全否，无新finding，无未答审批问题。先写ledger PASS再写本收口。下方V0.02及更早原文完整保留，仅作历史，不代表本次结论。

## 本次批准的三项与实际结果
1. 治理地图L871已按逐句批准明确：获单独有效查看权者及真实USB最高权限，可在获准范围内的商户档案列表/操作结果看两联系号码；其它人员脱敏。
2. L680已明确上述页面例外不授予未脱敏导出/打印；原保护及留痕要求保持。治理地图当前V0.13。
3. L656单次授权原句保持；修复Controller七个商户响应入口，复用已有requirePermissions同时核功能权与可选电话权，同来源只消费一次，消费成功的本次结果供掩码使用。无电话权保留原功能检查与脱敏；消费失败403且不进业务服务。

本批B3-r5仅PlatformController.java V0.03、PlatformControllerTests.java V0.04，全部归M009-058，无新增端点、权限码、全局授权语义、数据库迁移或回填。此前获批merchant.viewed对象上下文和两联系人姓名保存加密保持。

## 本次双席证据
- B4-r5：Codex V0.01 SHA f6a9f380a0f8d66319846b53e827f37c9c28117cbff3b6eba475e2c83d3220b7；Grok V0.01 SHA 5cec16daea696cc44eefe0248c6bf1bdc815de96f7073acb324c6fd67e87d560。完整差异正确性/逐行归因均PASS。
- B5-r5：Codex V0.01 SHA 124330bfdfed389261ac9ff8ef24f45245fbc9b8b6d8b5e360ea4a82c462a443；Grok当前V0.02全文件SHA 9683f41677418d747e9ba0e1fed82ef8a824a7ff8feb206ec00e92255c9cd71d。两席完整20/3381/88三线PASS；报告在同目录对应self/grok raw。
- Grok首次V0.01已交付PASS，但用户中断导致原本机会话回执丢失，真实退出码不可取，未用其直接收口。确认远端已结束后，从ledger原派发块取出同一完整CLI参数（含原-p）原样重跑。V0.01原29897字节SHA c89c8160e5099f3faba910b81246bd9c2be97db3d07e568b970d4fe2625bacc5完整保留，追加V0.02。
- 本次恢复调用实际回执：REPORT_PATH=/home/james/complex-management/docs/audit-2026-09/modules/M009-b5-r5-grok-raw.md；EXIT_CODE=0。完整实际命令保留ledger“ M009 B5-r5派发 ”代码块。固定grok-4.6、xhigh、无子代理，未更换审计席。
- B3树before 4ddfdc255c4bbf0da9d42837e9b1bd7c0124b6be；after a685f41c8cc2333c5bb937acce746d5258f9eade；完整diffSHA 190cf338324bd6be153d5afabe5ee87d948cb53bedfff8f881443f1269db3148。

## 验证与限制
Codex：114单测（19 Controller/70 Service/25 Authorization）及24 JDBC集成共138有效通过；数据库初次凭据错误造成24errors，纠正测试进程取既有配置后仅重跑失败类，24/0/0/0。不是一次完整138命令成功，不隐瞒初次环境错误。Grok本次独立93单测（19 Controller/70 Service/4 PiiCipher）通过及隔离加密探针通过；与Codex范围重叠，不相加。旧码新增5用例曾红，修后绿。
项目三资料仅后端/数据库且可空，无页面/旧项目补录承诺；联系人姓名仅新建及后续保存加密，旧行未批量回填。其它控制器、其它姓名生日、脸票比对、测试库卫生仍属其它模块B1输入。V427顺序、M426台账、M435/M436入口快照、M409/M415运行输入等既有红保留，不称全仓或部署通过。
Grok V0.01的空间类型并发双新建观察保留，按提示词极端竞态口径不入本轮队列；不因本轮没有新增项删除历史观察。M007早先hook事故已恢复并披露，本报告不声称此前全程无事故。

## 收口完整性
实算/sidecar/ledger/本次Grok末条四值全等：
b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
LOCK-AMEND-COUNT: 1

20源码指纹与本席封存及GrokV0.02一致；HEAD e456ba96c84185c8d39f18f548011daac48a6466，真实indexSHA d9a65108a1e87bd300c299ca7640f832bd5c2feb771f35a7a2413d32a1748331，旧主hookSHA46aabdf33cb3496d83a71e4357ca85ba2ed9dbbfd4582ab53747bc50bb748d6b不变。
三HTML当前SHA：BEQR4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66；BSMeebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66；PGM5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f。
未提交、推送、合并、部署。M001—M009现锁PASS。后续依冻结模块顺序继续。

## 历史V0.02全文（原字节内容保留，非当前结论）

# M009 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段
版本：V0.02

状态：PASS。当前步骤B5-r3已完成；B3整批修复3/3；LOCK-AMEND-COUNT=1。
本次为操作员要求的Codex复审与必要修复闭环；此前Claude/Grok V0.01历史全文附后，不将旧PASS或旧finding状态充当本次事实。

## 当前结果
冻结范围20文件，当前3369行，冻结计划2476行不改；87项判据（BEQR21、BSM14、GEN18、PGM34）。Codex与Grok分别完成第一性原理、顺从式、对抗式全模块复审，最终均PASS。当前24项队列全否：18已修复、6仅登记，无未答问题、无添加业务、无HTML修改申请。
原14项r1修复；054/055/056在r2解决真实最高权限入口与测试资料缺口；057在r3只纠正Java注释并补V0.01。每轮先完整归并定稿再整批修复，最后一轮1文件1块，运行token未变。054-056先改后确认的历史如实保留；用户随后“如果不是修改html，那么修复没有问题”的授权适用，不倒填为事先批准。

## 你关心的三项修复
1. 真实密钥最高权限能够进入岗位模板已有生命周期和审批入口；普通管理员不能代替最高权限，功能权限、占用、申请人/受益人限制及留痕保留。
2. 资金、通知、档案测试使用明确且合法的商户范围；原非法范围调用仍拒绝，金额/余额/通知/隐私断言保留，生产商户隔离不放宽。没有声称项目卡消费已补齐。
3. 审批身份测试补直接受益人和既有schedule资料，继续验证普通最高职级不能代替真实密钥最高权限；生产受益人检查不变。

额外057仅把SpaceTypeRequest旧“录入/修改…存在则更新”注释改为“空间类型字典新建请求（后台管理员维护）。typeCode 不得重复，新建状态固定为启用。”，与现有行为一致。

## 当前完整双审证据
|席位/阶段|版本|字节|SHA256|报告|
|---|---|---:|---|---|
|Codex B4-r3|V0.02|16177|0b5c47a44a2181386eb85e49e965b96209d45cdbec233aeab0fb74ee4aac0136|[完整差异自审](M009-b4-r3-self-raw.md)|
|Grok B4-r3|V0.02|42243|d30e00b45e2ccfef1fe06d84c0bc2684f677b3f0bb11c7a5bc8eb337b329607f|[完整差异外审](M009-b4-r3-grok-raw.md)|
|Codex B5-r3|V0.02|46511|402ae2dc748390fea009fb9cb8d5fc787d6e4728d830d5d9cda65a8836d9bf7b|[完整模块自审](M009-b5-r3-self-raw.md)|
|Grok B5-r3|V0.02|78917|f6c27bc22155acff3a35feec3f6ccc4ac5e3e52398539b5e99836c53dc07e1b9|[完整模块外审](M009-b5-r3-grok-raw.md)|

两席各自本次报告从最后独占哈希行起至末尾；同名旧报告字节前缀均保留。先派Grok，同时独立审计，本方完整raw封存后才读取Grok。本次Grok真实EXIT_CODE=0，REPORT_PATH=/home/james/complex-management/docs/audit-2026-09/modules/M009-b5-r3-grok-raw.md。
三轮完整差异分别在M009-b4-r1、r2、r3的self/grok raw，均保留旧前缀和本次完整diff；最终r3 before=aed0f2d9c35c0ffdecf7bbd8e2341d27d5cd1f3d，after=39b7324829f44339c8c2afb4d2800dfce674ed2c，859bytes，SHA256 2c7445ea8b5b36d3c7bb868379f7986ef5b61c850771203906d7698b5880fb3a。

## 验证与实际限制
当前源码逐blob核对after树，所有非审计文件无漂移，HEAD仍e456ba96c84185c8d39f18f548011daac48a6466；真实暂存区未改。最后仅Java注释变化，去注释token全等，两树git diff --check exit0。
Codex本轮实际运行11类283项通过；Grok在B5-r2独立运行10类239项通过（范围重叠，不相加）。两席均零失败/错误/跳过。最后纯注释修复后复用同源已封存测试证据，没有冒称重跑。测试仅JDK21与complex_test，未以本结果代替全仓、生产或全端页面验收。
项目三资料仅后端和数据库，按批准暂时可空；没有页面和旧项目编辑补录入口、集团汇总页面、新调度或财税审批系统承诺。已批准修复保持最小范围。

## 哈希与HTML
B5后四值（现算/sidecar/ledger/Grok本次报告）全等：
b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

三份业务HTML本轮未修改，当前SHA256：
- business-enum-quick-reference-v0.1.html：4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66
- business-system-map-for-operations-v0.1.html：eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66
- platform-governance-map-for-operations-v0.1.html：a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1
锁内69业务全文片段67匹配，BSM-114考勤文字及BSM-120页脚日期有历史差异；本模块不承载两处，不将首片段邻域匹配误写为全文一致，不修改HTML或锁。既有空间类型事件名/测试名观察不扩为057。
无BLOCKED，无待答问题，未触发铁律5人闸。按顺序先核四值，再把ledger M009改PASS，最后写本收口报告。未提交、推送、合并、部署；仅M009收口，不声称全仓审计完成。

## 最后一次Grok实际命令
经既有回执适配器运行；正常退出0。进程等待期间曾核实存活与持续网络通信，没有终止/重启它。
```text
/home/james/.local/bin/grok -p '你是Grok独立审计席乙，执行M009当前B5-r3修复后完整模块三线审计。正本/home/james/complex-management main；160-audit-prompt.md V0.17和后续用户指令，固定Codex/Grok两席，无子代理/MCP/修复/提交推送部署/新产物。唯一可写本次指定Grok raw。不得读本次B5-r3-self任何正文。先独算criteria-lock全文SHA作为本次报告第一行CRITERIA-LOCK-SHA256: <值>。

B4-r3已内容与归因双PASS：self V0.02 SHA256 0b5c47a44a2181386eb85e49e965b96209d45cdbec233aeab0fb74ee4aac0136；Grok V0.02 SHA256 d30e00b45e2ccfef1fe06d84c0bc2684f677b3f0bb11c7a5bc8eb337b329607f，两票含859byte完整diff。before aed0f2d9c35c0ffdecf7bbd8e2341d27d5cd1f3d，after 39b7324829f44339c8c2afb4d2800dfce674ed2c；只有SpaceTypeRequest注释及V0.01改动，无运行token变化。findings V2.14 SHA256 881f53efae81521efecf48b8413b62dbd3273a47e03d0db2a81c36ecb0da3dd0当前24全否=18已修+6登记；057在B4双PASS后才清队列，旧事实描述保留。ledger V3.71 M009 IN_PROGRESS，B3=3/3，尚未B5或模块PASS。用户非HTML修复授权已收到，013/038批准保留，054-056先改后确认历史不倒填。三业务HTML只读。

本次必须按冻结plan/modules.md的M009完整20文件/当前3369行和criteria-lock映射全部87项做第一性原理、顺从式、对抗式全审；不能只审注释或复用B4票。计划冻结2476不改。逐文件完整覆盖、每一映射的适用性和证据，读现行源码及三份HTML；直接依赖仅ProjectRecord/ProjectAssociationRequest/V427/AuthorizationService/SessionIssuer/Leasing/Access lifecycle及原相关测试，不扩大未轮到模块。18已修逐项核真实现状，6登记不能因NO而删。只用已冻结ID，冲突→细化→技术性→添加。每个问题临时GR编号，文件行、HTML原句/行为证据、四分类与工程缺陷、冻结ID；发现继续审完整模块，不中途修。
重点：项目/商户五组列表COUNT隔离、真实USB和普通高职级区分、专用null项目补录只改project_id与公司M:N、项目三字段可空旧调用兼容/无页面；财务三主体非空不得普通改、未来inactive不提前投影/merchant锁防重复、历史不重开、空间类型恢复/删除审批历史/审计/占用、非负数与中文message。057注释现与create-only拒重复/固定active一致。不将旧事件名或旧测试名（本批B2已明确非阻断）扩为057改动。发现独立真问题仍需完整报告，不能因第3次而强行PASS。
测试证据：B5-r2 Codex283/Grok239已实跑成功，包含69服务/23JDBC/14Controller与直接依赖；B3-r3仅注释且去注释token一致，未改任何运行逻辑或测试。不必无理由重复相同套件，不新写测试；若发现新疑点需要验证，只在complex_test/JDK21，只解析三个数据库变量，不source全env/泄露凭据。区分封存证据与本次实跑，不能冒称重跑。
已知观察：BSM-114锁片段“考勤打卡”对当前“查看考勤”，BSM-120页脚日期不同；完整69业务片段是67匹配，原87/87只是首片段邻域命中。本模块不承载两处，不改HTML/锁、不新增判据，报告如实说明。三HTML自身未因本轮改动。

本次两席同时独立；只在完成整模块后给明确PASS/FAIL，不能条件PASS或只交片段。技能按控制提示词收窄，只记录实际读取/使用，不派第三席。完整报告写/home/james/complex-management/docs/audit-2026-09/modules/M009-b5-r3-grok-raw.md；同名旧文件原样保留前缀，末尾追加本次完整报告并推进自身Vx.xx。只采本次最后哈希行至末尾，不复用旧同名结论。CLI无需机器标记，适配器只返回路径和exit。' -m grok-4.6 --reasoning-effort xhigh --output-format json --permission-mode bypassPermissions --no-subagents --disable-web-search --cwd /home/james/complex-management
```

## 历史V0.01原文（保留证据，不作为当前结论）

# M009 综合体/集团/项目公司/业态主体拓扑与四类主体绑定／后台实现／第1段
版本：V0.01

状态：PASS。范围：plan/modules.md V0.04 的 20 文件（冻结 2476 行，当前工作区行数见两席 B5-r3 报告）；判据 87 项（BEQR 21、BSM 14、GEN 18、PGM 34）。B3 整批修复累计 3 次（上限）。M009-001–012、014–016（第一批 15 项）、026–028（第二批 3 项）与 036（第三批 1 项）已修复；013、017–025、029–035、037 仅登记；当前 37 项修复队列全否。

B5-r3 按 Grok 真实退出→席甲（Claude Fable 5.1，作者自审票）主仓实跑并独立落盘→读取 Grok 顺序完成，两席全模块三线 PASS：
- [席甲完整报告](M009-b5-r3-self-raw.md)：V0.01，12761 bytes，SHA256 84fd3ef70a63b3bc2d6bac59d92b4864de3e16e0fcb72e6843deaa828e71c05c。
- [Grok完整报告](M009-b5-r3-grok-raw.md)：V0.01，45189 bytes，SHA256 490ef1c60cf8a11ecabe35818a26b840210dc0b8a8faea219082318ccd47945a。
- B5 后 actual/sidecar/ledger/精确本次 Grok raw 四值全等：b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。已先写 ledger V2.15 为 PASS，再创建本文件；LOCK-AMEND-COUNT 仍 1。

修复摘要：更新档案保留存档/删除申请态且须命中已有商户（001/002）；合作方式六类、商户/主体五态、项目六态、主体三角色白名单（003/004/005）；绑定与项目/空间引用存在性（006/007/027）；生效绑定同步商户三主体列（008）；删除审批仅最高权限、最高权限本人发起免审批（009）；软删除口径与已存档内容可查可恢复（010/026）；存档不再触发停票（011）；M009 面删除主体父链字段（012）；分页元数据（014）；治理读端平台作用域（015）；死代码删除（016）；已下线商户不得再被新项目/空间/绑定选用（028）；空间状态按已发布 V203 闭集前置校验（036）。伴生最小改动：LeasingService 签约不再写 `leasing_auto`、OrganizationMerchantPage 六类中文选项、M011 三测试与签约原子性夹具。

环境项 M009-017：complex_test 已含未发布 V400–V409 与 merchants.is_self_operated，两条 MigrationVersions 断言红，属环境非源码，须操作员处置（回滚到 V399 或新建测试库）。跨模块登记：013/018（项目档案字段与退出缓冲期等无实现面）、019（同构审批人模式）、020（无过滤 listMerchants() 消费方）、025（spv/operation 转 M010）。正本三 HTML/锁/权限种子/迁移未修改，未提交/合并/推送/部署。
