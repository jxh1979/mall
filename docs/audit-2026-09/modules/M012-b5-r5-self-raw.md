CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# M012 B5-r5 Codex 整模块独立三线复审
版本：V0.01

本席在本次 Grok B5 原文/原始 JSON 未读取时封存本报告；不是照抄外席结论。审计仅正本 /home/james/complex-management。沿用本轮 B1 已读的依赖调用链，重新通读六源全文，核本次两源改动和全部既有回归。B3 累计5，本次唯一获准批次仅018/019，无第6次修复。当前生命周期 IN_PROGRESS，最终双席合取前不称模块PASS。

## 范围与字节
全六源见下方实算清单；合计2129行，与aftertree 3eca41eb9aa21c6791ef70d5695deb940abeb992完全同字节。冻结映射仅21项，不扩锁。HTML三份只读，无变更。

## 第一性原理线
- 本模块是主体/商户/项目/空间关系展示、商户档案生命周期、既有绑定状态请求。必要数据来自真实API，不能用测试自造字段证明功能。
- 商户→项目依据merchant.projectId，空间→项目依据space.projectId；项目列表真实后端受currentProject主键限制，不制造多项目501条假前提。主体后页未加载明确提示，不伪造不存在。
- 三表按真实蛇形响应经过AdminApi转camelCase；绑定四类对象是merchantId及operation/contract/settlementEntityId。action取后台既有active/inactive/suspended。数据来源为HTML上位归属要求的细化，网络/分页/组件与测试机制为技术性。
- 删除提示须匹配后端最高权限审批和真实USB密钥本人直接执行；019仅纠正文案，未使任意platform_admin获得最高权。
- 下钻成功必须是当前视图真有该行；018以加载结束、无读错、商户表可见、visibleMerchants命中同时为真显示成功。其它状态提示筛选/翻页，不声称增加跨页自动定位。
- M012-009登记边界保留：不新增项目分配、附件上传、绑定创建、项目三资料输入等功能，不把未实现功能写为完成。

## 对抗式线
已反向核查：merchant_id旧反向依赖；venue_id假挂接；后页主体/商户/空间/绑定不可达；正常/暂停/黑名单混同；子按钮Enter/单选Space冒泡；重试旧全局错误；末页最后商户存档；无主体查看权副资源403；绑定财税拒绝；KPI隐藏目标和第二页目标错误定位。
现有真实请求和断言覆盖这些路径，未见当前待修问题。源/测试中历史注释不替代代码证据。
前端权限只负责显示与交互，后台Controller/Service继续对象/动作/项目范围检查；最高职级不是密钥最高权。主体绑定mock成功仅证明请求形状，真实财税保护仍由后台拒绝路径执行，测试保留原原因并显示错误。
联系人字段按当前授权HTML例外及后台返回展示，未从本模块新增查看权或导出权。URL参数用于本地表筛选，不授权数据。日期为数字ISO，内部ID保留；中文状态/提示覆盖业务枚举。
并发及事务不在React层发明新规则；Date.now幂等键的既有观察保留，本次无新增收费/结算/扣款路径，不以无浏览器实跑冒称全系统并发验证。

## 顺从式逐判据覆盖
| 判据 | 当前证据及适用边界 |
|---|---|
| BSM-001 | 真实商户/主体/项目/空间归属展示与商户来源入口；500为技术分页，不是写死业务政策。无涉钱配置编辑。 |
| BSM-003 | 既有引导创建、存档/删除申请/审批按钮分权，未将标题扩成新业务要求。 |
| PGM-001 | 三页行详情、KPI聚焦、商户TraceLink和auditTraceField；中文状态与真实归属；资金精度/尾差本模块无计算，不适用；服务端财税拒绝未绕过。 |
| GEN-001 | 仅必要字段与既有组件，无新增定位器/权限架构/依赖。 |
| GEN-002 | 三页加载、筛选、翻页、详情、已有写请求；018/019实际UI条件与动作一致。 |
| GEN-003 | 三文件59测试，真实AdminApi信封转换；先红4再绿，保留请求和理由断言，不靠快照。 |
| GEN-004 | 名称/业态/电话非空与绑定action/reason交互校验；信任边界最终后台校验，未前端冒充。 |
| GEN-005 | hasPermissions/PermissionButton/RowActions/HighRiskConfirm；无主体权不发主体请求，服务端对象与项目约束延续。 |
| GEN-006 | 本模块无数据库事务变更；关联展示使用真实ID，绑定写提交原对象ID，后台状态/财税保护继续。 |
| GEN-007 | 现有幂等请求字段与绑定submitting保留，未声明新增全并发保证；既有009观察不扩修。 |
| GEN-008 | 三组真实snake_case→AdminApi→camelCase字段、现行枚举与payload匹配接口；无假附件或主体。 |
| GEN-009 | 多资源错误状态/重试清错，创建与生命周期catch，绑定拒绝保留原因；018加载/错误不报定位成功。 |
| GEN-010 | JSX文本不拼HTML；无新增秘密/明文授权/导出；联系人按当前后台授权响应。 |
| GEN-011 | 商户auditTraceField及绑定真实viewed/changed事件；已删除不存在的拓扑/组织AuditTrailHint。 |
| GEN-012 | 服务端分页500及total/pager，后页可达，本页计数不冒全量；popstate监听有清理。 |
| GEN-013 | 本轮正本typecheck exit0，既有依赖，无配置/新增包变化。 |
| GEN-014 | 六文件无SQL/迁移，本项不适用；未借此前数据库修复作本页新成果。 |
| GEN-015 | Enter/Space目标判断、radio选择、详情Esc/遮罩、中文状态/标签、错误反馈有回归；未做真实浏览器视觉测试。 |
| GEN-016 | 页面说明与实际档案/绑定能力相符；未声称附件/全链新功能已补；本报告区分mock与真实后端。 |
| GEN-017 | 顺序冲突→细化→技术性→添加已核；018/019沿用B2细化分类，无新增添加问题。 |
| GEN-018 | 证据来自三HTML/冻结锁、可执行源码和本轮正本命令，历史PASS不是依据。 |

## 文件逐项结论
1. GovernanceTopologyPage.tsx（274行）：真实projectId连接、无权主体不请求、字典中文、分页与本页挂接、keyboard/详情/TraceLink已核，PASS。
2. GovernanceTopologyPage.test.tsx（308行）：19例涵盖字段、权限、501分页、主体后页、字典和keyboard及retry，PASS。
3. OrganizationMerchantPage.tsx（524行）：实际三表/字段/生命周期、归属/筛选/末页、错误catch、018/019修复全核，PASS。
4. OrganizationMerchantPage.test.tsx（475行）：24例；原POST及理由断言保留，新增后页与两类KPI隐藏路径有实际DOM/翻页与清筛选断言，PASS。
5. SubjectBindingsPage.tsx（257行）：真实绑定DTO、action/reason/权限确认、错误保留、分页/中文/keyboard已核，PASS。
6. SubjectBindingsPage.test.tsx（291行）：16例真实信封字段、请求payload、空理由/状态、选择键盘、501页和后台409拒绝，PASS。

## 本轮验证与旧项处置
B3本席正本实跑：只加测试时Organization24例中4失败20通过；源码修复后3文件59通过（19+24+16）、typecheck exit0、两树diff --check exit0。B4-self V0.01已有原始命令与输出证据，不重复跑已绿且字节未改的测试。本席不采Grok B4无效V0.01的/tmp证据；B4有效V0.02仅作阶段输入，不替代本次独立B5。
001—019定稿队列全否；001—008及010—019已修，009为登记观察，无未答复question（006/007均“删除”）。本次未发现新增问题、无新添加、无HTML改动需要。
未实跑真实浏览器、HTTP、Maven或数据库；59项为前端单元/组件测试，不声称端到端全量通过。

最终本席整模块审计结论：PASS。

## 实算记录
2026-09-23T00:34:11.552490+00:00
apps/web/src/admin/pages/GovernanceTopologyPage.tsx: 274行 SHA256=6338225cb0367502fefb3db2348673fe602ee9e6bed3cdae321feeea9a9b7036
apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx: 308行 SHA256=985196ad40864145f1be814cad1f5db6667883a114aed27e61453188219a28cb
apps/web/src/admin/pages/OrganizationMerchantPage.tsx: 524行 SHA256=55dc9a69205df3254c4031015bfe53907a3b1fbd7605e97c8f64a8ab515cb328
apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx: 475行 SHA256=3e91c3bbd589558e50bab1192036db2179b0dbdd4158fb0841eba1b0184d33e1
apps/web/src/admin/pages/SubjectBindingsPage.tsx: 257行 SHA256=94f4d4dcfcb24c85701450794544574efe26f209a4aed242ff5c428af98d247c
apps/web/src/admin/pages/SubjectBindingsPage.test.tsx: 291行 SHA256=ec22a9250ed24f61031f1997119b5537d77d1af537e388eff2018fa947d8591b
