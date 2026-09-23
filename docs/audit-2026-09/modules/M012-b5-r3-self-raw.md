CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M012 B5-r3 Codex 全模块三线审计
文件版本：V0.01
审计结论：FAIL
当前步骤B5-r3；M012/471；ledger当前IN_PROGRESS；B3累计3（历史1+本次2），未执行第4次。
本席已读完六文件全部2088行及21冻结判据，再完成直接依赖追流；不是用B4票替代B5。先派Grok，同时独立工作；本报告落盘之前未读取本次M012-b5-r3-grok-raw.md或Grok原始JSON。本次只有Codex/Grok两席，无第三席、MCP、源代码修改。
Grok实际完整命令见ledger V3.87“当前M012 B5-r3完整双审派发”，指定路径modules/M012-b5-r3-grok-raw.md；本席封存时尚未收齐其完整报告，不替其定票。

## 1. 范围与独立性
正本 /home/james/complex-management，main，HEAD e456ba96c84185c8d39f18f548011daac48a6466。
冻结计划modules.md V0.04：六文件1485行是计划时计数，不回写；当前六文件2088行，实际哈希表附后。
锁V0.11；映射BSM-001、BSM-003、GEN-001至GEN-018、PGM-001，共21项。三份HTML的当前SHA附后，冻结三段原文与当前HTML逐字匹配，未修改需求或锁。锁四值的B5后校验须待Grok完整报告，不能提前声称双席一致。
生产全文覆盖：GovernanceTopologyPage.tsx 1–276、OrganizationMerchantPage.tsx 1–521、SubjectBindingsPage.tsx 1–257。
测试全文覆盖：GovernanceTopologyPage.test.tsx 1–308、OrganizationMerchantPage.test.tsx 1–435、SubjectBindingsPage.test.tsx 1–291。工具大输出中组织页350–445和绑定页被截部分另行补读；没有把截断输出当完整覆盖。

## 2. 全部发现：CX-B5-01（新问题，尚无正式ID）
标题：两个页面的“操作留痕”提示列出后台并不记录的事件名。
位置：GovernanceTopologyPage.tsx:245；OrganizationMerchantPage.tsx:419；直接依据AuditTrailHint.tsx:47起、PlatformService.java:94/152/447/514/528/993。
第一次正常触发：有页面查看权限的操作员打开拓扑或组织商户页，展开现有“操作留痕”。不需要异常数据或并发；正常创建商户后也仍展示同一提示。
当前完整提示/代码：
- 拓扑：<AuditTrailHint event="governance_topology.viewed" />；展开后为“操作留痕：governance_topology.viewed”。
- 组织：<AuditTrailHint event="organization.viewed / merchant_profile.created" />；展开后为“操作留痕：organization.viewed / merchant_profile.created”。
实际行为：两页读取的既有端点由PlatformService分别记录entity_binding.viewed、merchant.viewed、project.viewed，拓扑另有space.viewed、space_type.viewed；组织页POST /api/v1/platform/merchants进入saveMerchant，executeWrite的成功事件为merchant.updated。backend/src精确搜索三个页面标识没有命中；这不是仅凭注释猜测：本席完整读页面、AdminApi与AuditTrailHint，组件只渲染文字，未另发审计请求；AuditedOperationExecutor执行实际结构化成功/失败留痕。
正常影响：按页面所示事件名称核对操作记录会找不到对应名称；页面提供的证据说明与实际记录不一致。实际记录并未因此丢失，商户创建/主体绑定并非因此失败，不把问题说成“系统没有审计日志”。
四分类按顺序：不判业务HTML冲突；可指向BSM-001“数据可溯源、全链可点击”和PGM-001“来源、归属和上下游关联”“证据与留痕能力”，归细化；有工程缺陷，无业务发明，不走添加项询问。冻结引用GEN-011（证据不得互相矛盾）、GEN-016（状态/完成声称可从当前范围复现），依据类型=可执行产物（实际调用/事件常量/组件渲染），并附可复现只读命令。
历史OBS-10允许内部标识符出现在折叠技术区，解决语言/展示位置，不证明标识符与实际事件一致；本席不以英文为缺陷、不重开TraceLink范围，不改共享TechDetails组件。旧报告“留痕事件名在页外/本身不判FAIL”也不能替代当前源码事实。
最小拟处置（仅供B2候选，未实施）：删掉两页各这一条错误提示节点即可，真实日志及其他接口证据/对象操作审计链接保留；若选择保留提示，则两处只改为当前读取/创建实际事件的中文说明，不能新增后台事件来迎合旧文案。不改HTML、权限、数据库、接口或后台留痕。修复后的最小验证应检查这两个错误节点消失/文字对应实际常量、原56用例及类型检查，避免另造审计系统。
三条审计线都完成后本席仍确认此项；当前新发现总数1，不因B3上限降级为PASS。

## 3. 三条审计线全覆盖
第一性原理：
- 关系来源是MerchantRecord.projectId→ProjectRecord.id、SpaceRecord.projectId；不把项目legacy merchantId当唯一经营商户，不发明parentEntity层级或三类假主体。
- 绑定真实DTO为merchantId/operationEntityId/contractEntityId/settlementEntityId/status/effectiveFrom/effectiveTo/changeReason；日期LocalDate，不是时间戳；本页不承担金额计算或改写涉税结果。
- 真实端点Controller→Service→Repository与AdminApi蛇形/驼峰序列化追通；审计证据独立追到executeWrite，发现CX-B5-01，不能只见AuditTrailHint就说事件正确。
顺从式：
- 正连所属项目、主体角色/合作方式已批准闭集、状态中文与字典中文、权限门槛、真实总数/页码、创建空初始/不传假主体和假证据、绑定必填action/reason全部核当前实现及测试。
- 001–008历史修复保留；010–016达到B2边界并已B4双PASS后转否；本席未把旧009登记限制写成完成的业务功能。
对抗式：
- 无entity.binding.view进入仍可读页面：主体端点不请求，表/抽屉“-”；有权但关联在另一页：未加载提示、翻页真实主体名。
- 501条后页、筛选保留归档/业态、末页唯一商户存档、当前页衍生统计及total/pageCount差异均检查，未用items.length冒充全量。
- 请求503后成功恢复清旧全局错误；请求仍503真实失败保留；拓扑字典失败同样反馈。四项只读连续失败探针证据来自本会话B4-r3，未修改文件。
- 子按钮Enter/单选Space不会冒泡触发行自身默认逻辑，行本身Enter仍可选，标签和确认控件保留。
- 绑定409财税审批守卫失败展示原错误；前端mock200不证明真实生效中绑定可停用。恢复已删除商户走现有档案保留机制，requireMerchant校验归属而requireActiveMerchant限制新引用；与M009-010获批恢复一致，不另造缺陷。
- 展开操作留痕并沿真实端点反查，确认CX-B5-01；全范围审完后才落FAIL。

## 4. 21项冻结判据矩阵
|判据|适用与证据|本席结果|
|---|---|---|
|BSM-001|Merchant.projectId、Space.projectId正连；列表对象抽屉与审计引用保持。与实际记录不符的提示见CX-B5-01；登记009的既有下钻边界不冒称实现。|FAIL（CX-B5-01）|
|BSM-003|只引用冻结h2与导语，其下子锚点未伪装成该原文；现有引导/存档/删除动作按后端权限与生命周期追流，无新增规则。|PASS|
|PGM-001|归属、权限、责任由真实端点校验；局部状态与字典中文、LocalDate日期无T/Z；内部标识例外不代表可显示错误的审计标识。|FAIL（CX-B5-01）|
|GEN-001|复用getPaged/useServerPagedRows/TablePager/useSelectedRow；局部兼容类型，不添框架或跨模块改造；新增问题可删两行解决。|PASS|
|GEN-002|三页读写控制流完整，所有相关原行为测试通过；旧错误开始清理、失败继续反馈、无权限不调接口。错误证据文案独立列011/016。|PASS|
|GEN-003|56真实AdminApi/fetch渲染测试19/21/16，真实snake_case，含否定路径、501后页、键盘、503重试及409；未测试的真实HTTP/浏览器不冒称。|PASS|
|GEN-004|创建name/type/phone非空、后台request校验；绑定action闭集与reason非空、URL id编码；后台最终验证，不依赖前端替代。|PASS|
|GEN-005|route和useCan只是UI；Controller全权限+platformscope，Repository按当前project过滤；电话后台按权限掩码。|PASS|
|GEN-006|无直接数据库写；Service audited.executeWrite及IdempotencyService.transactionTemplate包action+complete，绑定锁merchant再读，不在UI制造多写事务。|PASS（直接依赖核实）|
|GEN-007|AdminApi401重放使用同一stableBody/key；服务端按caller scope预留/重放，绑定高风险按钮submitting；Date.now双击旧观察不改写为本页新保证。|PASS（既有观察保留）|
|GEN-008|列表保留items,total,page,pageSize，Merchant.projectId与绑定DTO对齐；snake/camel两向；历史全局types虚字段未拿来读。|PASS|
|GEN-009|读取onError/catch、重试开始clear、字典onStart；创建/生命周期/绑定写入异常全有反馈；连续失败探针4过。|PASS|
|GEN-010|React文本展示，无注入/秘密；Controller对电话掩码，前端不绕开；技术标识非秘密。|PASS|
|GEN-011|真实成功/失败audit由后台落；两页Hint却列出不存在的事件名，完整证据CX-B5-01。|FAIL|
|GEN-012|每页500技术边界、实际分页total与页码，关联统计标本页；无客户端无界全量抓取；取消标志防过期响应覆盖。|PASS|
|GEN-013|本模块无配置/依赖变更；现成npm脚本56绿、tsc0，可从160当前源复现。|PASS|
|GEN-014|六文件无SQL/迁移，不适用直接迁移执行；读取实际DTO和SQL只为证明契约，不宣称新跑数据库迁移。|PASS（无实现面）|
|GEN-015|三页局部中文状态、暂停≠封禁、空间字典优先、键盘行/子控件/标签/禁用反馈；现有TechDetails内部标识例外不新增业务例外。|PASS|
|GEN-016|导语不再声称附件上传；版本/路径和实跑如实。两页操作留痕名称不能复现，CX-B5-01。|FAIL|
|GEN-017|010–016既有分类未变；新CX-B5-01指向HTML上位溯源归细化，无发明新业务/权限/事件。|PASS（分类纪律）|
|GEN-018|结论落到HTML精确原文、真实代码、现有实跑；注释“用户裁决/原样保留”仅说明折叠策略，不是三个事件真实存在的证据。|PASS|

## 5. 直接依赖覆盖及边界
只为本模块追流：adminClient.ts完整序列化/错误/401重放；useServerPagedRows/useAdminResource/useSelectedRow、HighRiskConfirm、AuditTrailHint/TechDetails、routes对应行；PlatformController读写权限；PlatformService读取/创建/存档/恢复/删除/绑定change；JdbcPlatformRepository同predicate count/list与project隔离；MerchantRecord/ProjectRecord/BusinessEntityBindingRecord、dto/StatusChangeRequest与MerchantProfileRequest；AuditedOperationExecutor完整、IdempotencyService完整。
绑定active→inactive/suspended须财税审批409、未来生效不得提前、ended不得重启、实体校验/锁/同步均在Service，前端不替代。当前项目3项补充字段仅后端的已批准范围不扩成页面需求；未做绑定创建/上传器/项目分配器。
审计命令一次错写apps/api/src不存在，修正为实际backend/src后执行；一次DTO目录误读已用rg --files找到governance/dto，补读成功。两次均无修改、未把报错当成功证据。
56测试未覆盖错误Hint事件名，因此绿测不推翻CX-B5-01。静态文案不为本阶段额外写入测试，更不在B5修源。

## 6. 实跑与可复现命令
本会话B3-r3当前相同六SHA：
cd apps/web && npm run test:unit -- GovernanceTopologyPage.test.tsx OrganizationMerchantPage.test.tsx SubjectBindingsPage.test.tsx
实际3文件56通过（Topology19/Organization21/Bindings16），ACTUAL_EXIT0。
cd apps/web && npm run typecheck
实际tsc -b --noEmit，ACTUAL_EXIT0。
原B3修前4失败52通过→修后56通过；B4-r3本席4只读连续503探针PASS/56SKIP、NODE_EXIT0；GrokB4-r3自行56绿和typecheck0，重复运行不相加。
完整命令/标准输出/红绿过程/内存探针源码在M012-b4-r3-self-raw.md V0.01 SHA febb028c00874d7696f687f0c9b26d593916e0c53b0bcf7afad686fb73fddc79；本次B5读取当前六SHA逐一与after tree匹配，复用相同字节证据，未声称重跑。jsdom scrollTo/getComputedStyle提示仍有，不能当真浏览器验收。
本次CX-B5-01只读复现：
rg -n 'governance_topology.viewed|organization.viewed|merchant_profile.created|merchant.updated|entity_binding.changed' backend/src
前三事件0命中；merchant.updated位于PlatformService.java:993及服务测试断言；entity_binding.changed现存。
rg -n 'viewed"|merchant.updated"' backend/src/main/java/com/complex/platform/governance/PlatformService.java
94 entity_binding.viewed；152 merchant.viewed；447 project.viewed；514 space.viewed；528 space_type.viewed；733 entity_binding.viewed；993 merchant.updated。
两页245/419及组件完整读给出实际呈现内容。绑定页entity_binding.viewed/entity_binding.changed与后台一致，不纳入缺陷。
本B5没有业务HTML/代码/测试/DB修改、无fetch/commit/push/merge/deploy。没有宣称线上或真实HTTP测试通过。

## 7. 最终票及待合取
本席全范围审计FAIL，唯一新依据CX-B5-01。原001–016当前NO是既有B4验收事实，不等于没有新B5问题。
收齐Grok完整B5后才做全批判定及B5四锁；若本批仍FAIL，按V0.17 §4“第3次B3后的B4/B5仍FAIL”置BLOCKED-3ROUNDS并交三轮diff与全部findings，不能自行B3第4次或进入M013。本报告不替对方填锁或改结论。

## 8. 封存时实算文件指纹
{
  "six_files": {
    "apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx": {
      "sha256": "985196ad40864145f1be814cad1f5db6667883a114aed27e61453188219a28cb",
      "bytes": 16571,
      "lines": 308,
      "version": "// 文件版本：V0.04"
    },
    "apps/web/src/admin/pages/GovernanceTopologyPage.tsx": {
      "sha256": "6204706ba5c97e53bfeb200799ddc7a39b615a18f69e1d71607dfb56765a5a89",
      "bytes": 18169,
      "lines": 276,
      "version": "// 文件版本：V0.03"
    },
    "apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx": {
      "sha256": "d40c83d948e0b07f83f6eb5bcbb2a18af12b186ddd93fdcc651557d5870408a7",
      "bytes": 26133,
      "lines": 435,
      "version": "// 文件版本：V0.07"
    },
    "apps/web/src/admin/pages/OrganizationMerchantPage.tsx": {
      "sha256": "28f31ae9b3b731c7e3695cb3d928fc69613b1124b7d5dd970a3586e58b2c4d49",
      "bytes": 32157,
      "lines": 521,
      "version": "// 文件版本：V0.04"
    },
    "apps/web/src/admin/pages/SubjectBindingsPage.test.tsx": {
      "sha256": "ec22a9250ed24f61031f1997119b5537d77d1af537e388eff2018fa947d8591b",
      "bytes": 15482,
      "lines": 291,
      "version": "// 文件版本：V0.04"
    },
    "apps/web/src/admin/pages/SubjectBindingsPage.tsx": {
      "sha256": "94f4d4dcfcb24c85701450794544574efe26f209a4aed242ff5c428af98d247c",
      "bytes": 14784,
      "lines": 257,
      "version": "// 文件版本：V0.03"
    }
  },
  "html_sha256": {
    "business-enum-quick-reference-v0.1.html": "4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66",
    "business-system-map-for-operations-v0.1.html": "eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66",
    "platform-governance-map-for-operations-v0.1.html": "a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1"
  }
}
