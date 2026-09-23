CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M012 B5-r4 Codex 全模块三线审计
文件版本：V0.01
审计结论：PASS
当前步骤：B5-r4；M012/471；ledger为IN_PROGRESS；B3累计4，操作员额外授权一次已使用。本票只代表本席，待Grok完整报告与B5后四锁校验才能模块收口。

## 1. 范围、身份与独立性
正本/home/james/complex-management，main，HEAD e456ba96c84185c8d39f18f548011daac48a6466。通过直接SSH审计，不使用MCP、第三席或子代理；不提交/推送/合并/部署，不改HTML、计划、锁、业务代码或测试。
先启动Grok，同时独立完成三线；本报告落盘前未读取M012-b5-r4-grok-raw.md或CLI原始JSON。Grok完整实际命令已写ledger V3.96“当前M012 B5-r4完整双审派发”，指定modules/M012-b5-r4-grok-raw.md；本席封存时未消费其交付。
计划modules.md V0.04六文件1485行是冻结时计量；本次全部6文件2085行，不回写旧计划。逐个全文已读：Topology274+308、Organization520+435、Bindings257+291；大输出截断的Binding121–178、Organization测试290–435均已补读，未以截断输出代替覆盖。
criteria-lock.md V0.11独立实算见首行；BSM-001、BSM-003、GEN-001至GEN-018、PGM-001共21项定义全读。三条HTML冻结摘录与当前HTML逐字匹配，三HTML哈希附后。未扩判据，LOCK-AMEND-COUNT=1。
已核using-superpowers、receiving-code-review、verification-before-completion及grilling的实际SKILL.md，本阶段沿用§6收窄结构：事实自读、逐证核查，不问操作员、不派第三席、不边审边修。B4 requesting-code-review只派Grok，未套用第三审。更早技能存在记录保留在runbook，不冒称本B5重新调用所有技能。

## 2. 六文件全文覆盖
|文件|实际通读行|字节|SHA256|
|---|---|---|---|
|apps/web/src/admin/pages/GovernanceTopologyPage.tsx|1–274|18046|6338225cb0367502fefb3db2348673fe602ee9e6bed3cdae321feeea9a9b7036|
|apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx|1–308|16571|985196ad40864145f1be814cad1f5db6667883a114aed27e61453188219a28cb|
|apps/web/src/admin/pages/OrganizationMerchantPage.tsx|1–520|32061|d3ddb2cc423cc901190e8a5d9c5c6f2e4095919d956a19a6c866124947fa6722|
|apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx|1–435|26133|d40c83d948e0b07f83f6eb5bcbb2a18af12b186ddd93fdcc651557d5870408a7|
|apps/web/src/admin/pages/SubjectBindingsPage.tsx|1–257|14784|94f4d4dcfcb24c85701450794544574efe26f209a4aed242ff5c428af98d247c|
|apps/web/src/admin/pages/SubjectBindingsPage.test.tsx|1–291|15482|ec22a9250ed24f61031f1997119b5537d77d1af537e388eff2018fa947d8591b|
所有六文件字节与r4 after tree f2366e35ef257fbc349cbbc5e00e0052cc2b389c相同；测试内容从修前保持不变。真实index SHA256 1657ef91568fc6c00e7fd66f58f4c00461260651323d2ecbf616238602b973a2，未使用主索引生成快照。

## 3. 第一性原理三问
1. 为什么存在：BSM-001要求管清综合体、经营者与位置；PGM-001要求归属、资格、责任可追。拓扑呈现merchant.projectId→project.id与space.projectId；组织页呈现真实商户/主体/项目和建档生命周期；绑定页呈现实际经营/合同/结算主体关系与既有状态变更。移除这些页面会失去本模块已有入口；分页、抽屉、API封装和测试属现有业务的细化/技术实现，不额外发明管理层或主体。
2. 声称是否实做：真实AdminApi/fetch组件用例本会话r4实际56通过，tsc -b --noEmit实际exit0，完整输出在B4-r4-self-raw §5。三页从读取、字段变换、权限、渲染、写请求到错误返回逐流核对。测试mock200仅证明请求/反馈，不证明后端财税审批放行。
3. 哪个前提须核实：项目不能用legacy merchantId反连；当前/projects不是真全局项目清单。Controller144–150→PlatformService444–445→JdbcPlatformRepository1287–1300，COUNT/SELECT都WHERE id=:project_id，V005 projects.id PRIMARY KEY，故该真实入口每范围至多1条；商户同currentProject过滤。501商户/空间/主体不推出501项目。重读这条实际链后不采用已由Grok B2撤回的虚构响应。另一前提“留痕提示就是真实日志”已经017修正；实际auditTraceField与后端记录保留，不能靠注释认定事件存在。

## 4. 顺从式与对抗式全审
顺从式逐文件验证真实DTO、所属项目、中文合法状态/枚举、分页元数据、创建空初始、action/reason必填、后端权限与审计；原001–008与010–016修复行为仍在。017只删除两个错误提示节点，Organization:319 auditTraceField(navigateTo,"merchant",row.id)保留；SubjectBindings:191实际entity_binding.viewed/entity_binding.changed提示仍对应后台，接口证据未删。
对抗式检查：
- 无entity.binding.view：不发business-entities请求，商户主内容可显示且主体列/抽屉为“-”；有权限而主体在第2页：呈现未加载与分页入口，翻页后真实名称。
- 501条商户/空间/绑定和主体后页、服务端total/page、当前页去重与生效数、归档/业态筛选保留、存档末页唯一商户后的退页均由可失败测试覆盖。项目按真实服务合同最多当前1条，不拿手造跨项目列表当正常入口。
- 三页503后成功清全局错误，仍失败由onError保留反馈；字典onStart清旧错后失败上报。当前useServerPagedRows取消标志、fetchRef及依赖数组已全文复读，不新增无界请求循环。旧r3连续503四个只读探针只作此前同逻辑证据，本轮没重跑、不伪称新结果。
- 子按钮Enter、单选Space保留原生行为，行自身Enter仍打开详情；input/select标签、错误、禁用/高风险确认保留。
- 绑定409财税复核拒绝保留reason及错误。真实Service对active→非active、未来日期、已结束绑定、三主体有效与同步投影的约束未变；前端mock不扩大后台能力。
- Organization新建不预填验收资料，不提交假主体/假证据/假面积；联系电话不擅设大陆手机号限定；各生命周期动作catch并上报真实失败。已删除商户恢复和新引用禁止区分requireMerchant/requireActiveMerchant，遵循既有M009批准边界。
- 本模块未引入钱的计算、税务结果改写、SQL迁移、文件上传或新依赖。不因HTML沉默而删技术实现，不以模拟恶意数据或多重巧合扩大队列。

## 5. 21项冻结判据矩阵
|判据|当前证据与适用范围|结论|
|---|---|---|
|BSM-001|Merchant.projectId→Project.id、Space.projectId；列表/抽屉与真实审计引用保持，017错误说明已移除；009边界仍登记。|PASS|
|BSM-003|冻结h2/导语未扩成新锚点；现有引导/存档/删除入口追到后台权限和生命周期，不发明规则。|PASS|
|PGM-001|真实归属/权限/责任，中文合法状态与字典，LocalDate日期；标识符例外不作为错误事件理由。两处假事件提示已删。|PASS|
|GEN-001|复用现成getPaged/hooks/TablePager/抽屉；017删除为最小修复，未新增抽象或跨模块改造。|PASS|
|GEN-002|三页读写主路径和失败路径可执行；56渲染/交互测试通过且类型检查0。|PASS|
|GEN-003|真实AdminApi+snake_case信封，断言业务结果与禁止请求、后页、键盘、503、409；19/21/16共56，静态it声明52因参数化不同。|PASS|
|GEN-004|创建name/type/phone与绑定action/reason前置校验，动态路径编码；后端最终校验当前范围和闭集。|PASS|
|GEN-005|UI门槛不取代Controller权限/platform scope；SQL按当前项目过滤，后台按权限掩码电话。|PASS|
|GEN-006|本页无直写DB；AuditedOperationExecutor与IdempotencyService事务包action+complete、绑定锁商户再读，引用/状态后台约束。|PASS|
|GEN-007|API401以稳定请求体/键重放，后端callerScope预留/重放；绑定submitting防重复操作。009双击观察不冒称已补。|PASS|
|GEN-008|真实绑定DTO、merchant.projectId、蛇形/驼峰、page/size/total、闭集状态/角色/合作方式与夹具对齐。|PASS|
|GEN-009|读取catch/onError、重试开始clear、字典onStart、创建与生命周期/绑定写入错误反馈均可见；成功不残旧错。|PASS|
|GEN-010|React文本渲染、后台脱敏/权限，不暴露密钥或注入式HTML；内部ID不当秘密数据。|PASS|
|GEN-011|两页虚构Hint已删，真实商户auditTraceField仍可关联对象；后台成功/失败留痕和绑定事件一致。|PASS|
|GEN-012|500为每页技术边界、有total/页码；当前页派生指标标本页，取消标志防旧响应，无客户端全量无界抓取。|PASS|
|GEN-013|使用已有npm脚本与依赖；当前相同字节56测试/tsc0，无构建配置变更。|PASS|
|GEN-014|六文件无SQL/迁移实现面；直接阅读SQL只为验证契约，本B5未运行迁移、不声称数据库验收。|PASS（无直接实现面）|
|GEN-015|三页中文闭集、暂停非封禁、字典中文优先，原生子控件键盘、行焦点与标签保留；真实浏览器未验证如实声明。|PASS|
|GEN-016|导语仅声称实际状态/原因表单；017不再列不可复现审计事件；版本、路径、测试范围与完成声称按当前事实。|PASS|
|GEN-017|按冲突→细化→技术性→添加核查；017属已有溯源的细化，不新增后台业务事件；006/007首答删除已落实，无新添加。|PASS|
|GEN-018|依据来自冻结HTML精确摘录、当前代码/DTO/SQL和实际命令输出；不以旧票、注释、惯例替代本次全审。|PASS|

## 6. 既有17项的当前状态与未扩范围
findings.md V2.23顶部B4-r4验收为当前有效状态：001–017全否；001–008在历史B4-r1验收转否，010–016在B4-r3验收转否，017在B4-r4双PASS才转否。文件下方历史候选表保留“是/待答”是时间线，不重新当成当前队列。
本次逐项回看001真实绑定字段，002action，003space.projectId，004中文，005真实夹具，006去假默认/证据，007去手机格式业务发明，008写失败反馈；010正向项目关联，011测试前提，012导语，013分页/恢复，014状态中文，015键盘，016字典，017提示删除。无未修新发现。
009为已登记边界，不冒称补齐附件上传/绑定创建/项目分配器/全链下钻；项目三资料已批准仅后端，不要求本页输入框。历史NULL项目商户不放宽隔离；日期/财税guard以真实后端为准。既有内部ID/KPI URL、双击键观察不顺修。
questions/M012-006.md与007.md“操作员答复”首个非空行均精确“删除”；无本模块未答问题。累计4次B3中最后一次是操作员明确额外批准，不抹去前三轮或历史FAIL。

## 7. 验证命令、结果与限制
本会话r4修改后实际执行于160 apps/web：
```sh
npm run test:unit -- GovernanceTopologyPage.test.tsx OrganizationMerchantPage.test.tsx SubjectBindingsPage.test.tsx
npm run typecheck
```
第一条真实exit0，3文件/56通过（19/21/16）；第二条真实exit0，tsc -b --noEmit。完整stdout与命令保存在M012-b4-r4-self-raw.md V0.01 SHA447999538638727fb9d5b9c2939b7141afe333424cbad28d7b20896349379c3e §5。本B5核当前六SHA与当时after tree逐项一致后引用，未无理由重复同组测试，不冒称Grok已新跑。
本B5只读命令：逐行打印六文件及21定义；按冻结三个html代码块与实际HTML检查子串；hashlib.sha256实算六源/锁/HTML；git show after:文件与working bytes比较；直接Controller/Service/JDBC项目链及hooks/AuditTrailHint全文读取。结果六源全same_r4_after、三HTML摘录全true；16项先前已全文审读的直接依赖与r3 after字节相同（附表）。
一次只读核对命令因ledger首列是“M012+标题”而非裸ID出现StopIteration；改为真实前缀后完整重跑exit0。StatusChangeRequest同名多处，选定实际governance/dto，不从错误目录作结论。无文件修改、未把异常输出当PASS。
没有真浏览器/真实HTTP/后端Maven/数据库执行。jsdom scrollTo/getComputedStyle提示仍保留；当前PASS是本模块审计结论，不是线上业务端到端验收。

## 8. 身份与依赖证据
三HTML当前SHA：
- docs/business-enum-quick-reference-v0.1.html：4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66
- docs/business-system-map-for-operations-v0.1.html：eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66
- docs/platform-governance-map-for-operations-v0.1.html：a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1
以下依赖只用于本模块追流；本B5并非重新审完整backend。当前工作区与本席上次B5-r3全文追读时树9b1e71aa9eef993d24abdb1c7d8b2d4d8c0e1f2f对应blob完全相同；本B5重新读了hooks/AuditTrailHint及项目Controller/Service/JDBC关键链。其余同字节复用已实读证据，未声称新跑后端测试：
```json
{
  "backend/src/main/java/com/complex/platform/governance/dto/StatusChangeRequest.java": {
    "sha256": "75b75cfaee897096f6fce0886fc4e71da8c5762774a901739cff244e1425f60f",
    "same_prior_b5_r3": true
  },
  "apps/web/src/admin/client/adminClient.ts": {
    "sha256": "01127146ffc4ea4e1243fc10ae76c1f582a830ca35e5222562d33c6a67c2ec82",
    "same_prior_b5_r3": true
  },
  "apps/web/src/admin/hooks/useSelectedRow.ts": {
    "sha256": "ca801d91b05b154fde70c54c43a15ceceabe91238579b6951a8497d50fb18d55",
    "same_prior_b5_r3": true
  },
  "apps/web/src/admin/components/HighRiskConfirm.tsx": {
    "sha256": "49815863a435cac4cc9a769fce84bbf6e0aa2e9397b0d7d9d9c606719d4a3b0e",
    "same_prior_b5_r3": true
  },
  "apps/web/src/admin/components/TechDetails.tsx": {
    "sha256": "94273bdaf75dd5f62395c9bdb246c78dd404554f9b6bdb5cef786595ad9be72e",
    "same_prior_b5_r3": true
  },
  "backend/src/main/java/com/complex/platform/governance/PlatformService.java": {
    "sha256": "984d57a4ba7a5a1059734cac427045ce4ee73143249cd93d0bd70f36488226b1",
    "same_prior_b5_r3": true
  },
  "backend/src/main/java/com/complex/platform/governance/PlatformController.java": {
    "sha256": "9e4cef62553160bbd9b2c8bba8779feadec81a4dd26cf3d28664fb160777c3d2",
    "same_prior_b5_r3": true
  },
  "backend/src/main/java/com/complex/platform/governance/JdbcPlatformRepository.java": {
    "sha256": "2816e96cd73867d2702b660854a1d342254c68b72b7c1e68fa12fb460e0bc98a",
    "same_prior_b5_r3": true
  },
  "backend/src/main/java/com/complex/platform/evidence/AuditedOperationExecutor.java": {
    "sha256": "211ce3bf58d3290ef8a969277e67d3fbacb1d0509dcdaac00189d6f17d9ad210",
    "same_prior_b5_r3": true
  },
  "backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java": {
    "sha256": "b71b659ce968221a0727d5263faa04d2acfac57ba1f08e2e477078fa55b5a275",
    "same_prior_b5_r3": true
  },
  "backend/src/main/java/com/complex/platform/governance/ProjectRecord.java": {
    "sha256": "3d18793172d90f57cd5296ae76f2d15983a61a60a5ff93b24dd8bd1697284487",
    "same_prior_b5_r3": true
  },
  "backend/src/main/java/com/complex/platform/governance/MerchantRecord.java": {
    "sha256": "dda319c37032c33c3cc5d260a0c9f43c7740d21b80a106c4a4efd49a19cdef00",
    "same_prior_b5_r3": true
  },
  "backend/src/main/java/com/complex/platform/governance/BusinessEntityBindingRecord.java": {
    "sha256": "f97af4de8ed4d45134196b4fcb83169591d5991e6aefaa4aa22411a49e78f422",
    "same_prior_b5_r3": true
  },
  "backend/src/main/java/com/complex/platform/governance/dto/MerchantProfileRequest.java": {
    "sha256": "7b6dd241328b57dc646eb9b3c531e01e3a3a3d1bb1b848711c7ce43106973cf1",
    "same_prior_b5_r3": true
  },
  "apps/web/src/admin/adminRoutes.ts": {
    "sha256": "b88500ae13ca94036f7a8e0df5d7bd4d4e7d18fdeb6031bf499d7f55e57d1112",
    "same_prior_b5_r3": true
  },
  "backend/src/main/resources/db/migration/V005__p0_platform_master_data.sql": {
    "sha256": "5118942eea90829a2c8d59fa88e70e7b9467ca87201acff653d2ccc2e4054905",
    "same_prior_b5_r3": true
  }
}
```

## 9. 本席终票
本席三线全模块结论PASS，无新finding、无未答questions；当前仍IN_PROGRESS，等待Grok最终票。本报告不代其填写哈希、不改其历史结论。
只有两席B5-r4完整PASS且四锁一致才能先写ledger PASS、再写模块收口。若Grok FAIL，则本批FAIL，额外一次已耗尽，不能自行执行第5次B3或进入M013。
