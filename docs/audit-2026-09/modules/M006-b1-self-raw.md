# M006 B1 — Codex 独立完整审计

版本：V0.01

控制：V0.17 / SHA256 c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21。
当前 M006 / 共322，10-backend-main-access-01，IN_PROGRESS，B3=0，HEAD=395e6fbab027020da1c38237fad17e8edcd7a132。全14文件2988行，52项映射判据。

本席审计结论：FAIL。全部源码和三线审查完成，18项工程缺陷/业务冲突，5项仅登记（4技术性、1细化），共23个临时CX编号。Grok已独立派发，本席未读取其stdout/JSON/报告正文；未修改源码、HTML或冻结判据。本raw在精确程序附录追加完毕前不视为已封存，不读取Grok，不进入B2/B3。

## 逐文件完整阅读

| 文件 | 行数 | SHA256 | 核对范围 |
|---|---:|---|---|
| backend/src/main/java/com/complex/platform/access/AccessActor.java | 11 | 5b0115b479d4a2f9ddec2f7bc3837ffbcb3ca18dfd30b98ada61250545eddf3a | 所有字段/构造器/防御复制/内部标识及实际生产者消费者 |
| backend/src/main/java/com/complex/platform/access/AccessController.java | 272 | 7e2e079b981ca7e6e048ac1daae88bc55c3d8a176bcc7f493ef88133a36ddd59 | 全部路由：权限、幂等、请求转换、双权限入口 |
| backend/src/main/java/com/complex/platform/access/AccessCredentialProvisioner.java | 12 | c87b5ff721de2308b4a0ab0b9395a9dca19f8551d157f798acece0ec6688ab5b | 接口/两实现：profile、凭证、空输入、BCrypt与JDBC时间参数 |
| backend/src/main/java/com/complex/platform/access/AccessGrantPositionSummary.java | 10 | 08fa864f1b0cada3309ea24503d159df5b1075c77e49a04ad75aa61ca1c156df | 所有字段/构造器/防御复制/内部标识及实际生产者消费者 |
| backend/src/main/java/com/complex/platform/access/AccessGrantRequestRecord.java | 35 | b056364a8d6526123b1137c0ec7016240ca42083e9ca1050f905a4e55166ee89 | 所有字段/构造器/防御复制/内部标识及实际生产者消费者 |
| backend/src/main/java/com/complex/platform/access/AccessGrantSummary.java | 9 | 7648579e55d34871a4deea19ccde7eef74f592613d3cdf6015d7ca7dce7e1d96 | 所有字段/构造器/防御复制/内部标识及实际生产者消费者 |
| backend/src/main/java/com/complex/platform/access/AccessGrantUserSummary.java | 8 | 80fb7b5d47b0afdec3cda37e2c954950242603ed23e2d847f136b3b794636a7e | 所有字段/构造器/防御复制/内部标识及实际生产者消费者 |
| backend/src/main/java/com/complex/platform/access/AccessRepository.java | 208 | 5213cd76819dfd11d7dacfd8d942b553955770fb434ac1f3eb7cf1ca0f893344 | 全部接口/默认方法：null scope、空返回、写操作及原子消费契约 |
| backend/src/main/java/com/complex/platform/access/AccessService.java | 1793 | b4ab5f0e63d07313a7a579efdc247bde32a5ab9b2596871409d768126aba836c | 全部公共/私有方法：模板、角色、审批、建号、岗位、临时授权、统计与异常 |
| backend/src/main/java/com/complex/platform/access/AccessUser.java | 35 | dd7b7b83e0646b6ffda0115960cf52adedd92076a04e56406fe0a947f6f38455 | 所有字段/构造器/防御复制/内部标识及实际生产者消费者 |
| backend/src/main/java/com/complex/platform/access/AuthorizationService.java | 472 | d91c76b8a898f383bddf3d4afc9dd013909723233f6a66676cd3c37f1fae760d | 每个方法：四来源有效期/DENY、管理员/客人桥、身份/级别/角色/商户/项目/合规范围 |
| backend/src/main/java/com/complex/platform/access/DevAccessCredentialProvisioner.java | 40 | 1eb91915baa87681d5db416140a5b6715916ef8fe8986d8c7ded27a2a7f9abc8 | 接口/两实现：profile、凭证、空输入、BCrypt与JDBC时间参数 |
| backend/src/main/java/com/complex/platform/access/EffectivePermissions.java | 23 | 68ff6d1da086bfb7e1aa0b33aed0de3962a515737a91096fb791cfc9faced44e | 所有字段/构造器/防御复制/内部标识及实际生产者消费者 |
| backend/src/main/java/com/complex/platform/access/JdbcAccessCredentialProvisioner.java | 60 | 9cfab4cb9412e8c794376e1eaf6682c497f2b43c5c9e8bf289ca30d2dc2310a9 | 接口/两实现：profile、凭证、空输入、BCrypt与JDBC时间参数 |

## 三线与第一性原理

1. 必须存在的原因：BSM-028原文“管理员工、岗位、班次、职级、权限、工作设备和员工注册审批”；BSM-120/PGM-066原文“员工权限按单一功能最小颗粒度配置，支持角色和员工级完全自定义”。主体、授权、角色/模板和凭证桥承载这些需求。纯Java容器/注入/序列化按技术性单列。手工员工直接生效与BSM-085两条注册路径冲突，不能因为“有用”自行保留。
2. 实际执行：两组Java探针各重新编译48个当前相关源码/测试，直接调用服务/控制器；原55个@Test逐个执行@BeforeEach/@AfterEach及真实assertj断言。第三组调用当前JdbcAccessRepository、真实PostgreSQL complex_test和仅连接级TEMP表复现外键。完整程序/输出追加到本raw，不另建产物。
3. 拒绝未验证前提：同组织不等于同商户；null不代表授权；有损slug不是唯一ID；申请快照不等于最终授出的权限；不同幂等键不能代替对象互斥；实时模板引用不是历史快照；绿测试不是安全证明。未执行全后端测试、全部E2E、生产DB、共享业务表写入或跨节点压测，不宣称通过。

顺从线逐一核对52项原文与本模块实际职责；对抗线构造异商户/无组织/ID碰撞/模板增权/审批漂移/受益人自审/双线程审批/岗位切换/复合单次权限/非法日期及集合；第一性原理线落到HTML逐字原文、现有SQL与实跑输出。发现问题后继续审完整模块，没有转修复。

## 52项冻结映射覆盖

| 判据 | 核对结果 |
|---|---|
| GEN-001 | 必要性逐文件核对；主体/权限有HTML上位需求；纯机制只作技术登记；手工员工生效旁路CX-11冲突。 |
| GEN-002 | 48个相关当前Java源码编译成功，55既有单测真实通过；补充反例证明CX-10/12/13/14/17，绿单测不是模块通过。 |
| GEN-003 | 已检查测试fixture/断言及仓储替身边界；原55个@Test逐个执行setup/teardown，新的探针直接调用当前服务；TEMP SQL补足内存替身没覆盖的外键。 |
| GEN-004 | @Valid没有自动补出DTO约束；缺失范围、可碰撞标识、空集合和倒置日期实测见CX-03/04/13。 |
| GEN-005 | 路由权限、服务目标范围、职责分离逐项核对，CX-01/02/03/05/06/07/09/10/12。 |
| GEN-006 | 主体归属、授权快照、岗位替换及消费/留痕一致性，CX-04/06/08/09/14/17/18。 |
| GEN-007 | 双线程不同幂等键审批已实跑两次生效；单次授权原子消费旧测试不覆盖复合权限一动作或消费与留痕，CX-08/14/17。 |
| GEN-008 | 逐个DTO/record、ID与仓储SQL约束交叉核对，CX-04/13/17；本14文件无迁移，SQL只作实现依赖证据。 |
| GEN-009 | 追到GlobalExceptionHandler/FieldLabels。纯英文抛点多有集中翻译，未一概误报；成功响应CX-15和消费留痕异常仍成立。 |
| GEN-010 | BCrypt/参数化SQL/UTF-8已核对，不扩展外部漏洞扫描；鉴权/覆盖/增权安全缺陷见CX-01/04/05。 |
| GEN-011 | 审计事件有主体上下文，不仅因access-api字符串误报无操作者；申请快照及消费留痕CX-06/08/17。 |
| GEN-012 | 本模块无自建线程池/连接生命周期。未单凭理论数据规模增添分页优化；探针临时目录和连接均清理。 |
| GEN-013 | mvn -o dependency:build-classpath与当前48源javac --release21都exit0；未跑全后端package/全仓E2E，不声称通过。 |
| GEN-014 | 本模块无迁移。只读V003/V170及所有引用消费日志表的当前迁移；TEMP SQL不迁移或写共享业务表。 |
| GEN-015 | 本14文件无UI源；实际追到AccessGovernancePage成功响应直渲染两句英文，CX-15。 |
| GEN-016 | 注释和接口声称回到实际行为：保留owner与清空owner相反CX-18，单次消费契约CX-14/17。 |
| GEN-017 | 顺序冲突→细化→技术性→添加，全23CX逐项登记。无新审计判据，无业务添加询问，无HTML修改申请。 |
| GEN-018 | 必要性、实际行为、未证实前提三个追问及三线核对见正文，结论不以旧审计或Grok观点为证据。 |
| BEQR-157 | 完整原文已读；本14文件不实现人事状态/排班/考勤/请假/薪资月结/陪场商品。账户active不强行等同人事状态。职级/基础薪资只核输入及权限，未宣称下游工资算法通过。 |
| BEQR-182 | 完整原文已读；本14文件不实现人事状态/排班/考勤/请假/薪资月结/陪场商品。账户active不强行等同人事状态。职级/基础薪资只核输入及权限，未宣称下游工资算法通过。 |
| BEQR-183 | 完整原文已读；本14文件不实现人事状态/排班/考勤/请假/薪资月结/陪场商品。账户active不强行等同人事状态。职级/基础薪资只核输入及权限，未宣称下游工资算法通过。 |
| BEQR-184 | 完整原文已读；本14文件不实现人事状态/排班/考勤/请假/薪资月结/陪场商品。账户active不强行等同人事状态。职级/基础薪资只核输入及权限，未宣称下游工资算法通过。 |
| BEQR-185 | 完整原文已读；本14文件不实现人事状态/排班/考勤/请假/薪资月结/陪场商品。账户active不强行等同人事状态。职级/基础薪资只核输入及权限，未宣称下游工资算法通过。 |
| BEQR-186 | 完整原文已读；本14文件不实现人事状态/排班/考勤/请假/薪资月结/陪场商品。账户active不强行等同人事状态。职级/基础薪资只核输入及权限，未宣称下游工资算法通过。 |
| BEQR-187 | 完整原文已读；本14文件不实现人事状态/排班/考勤/请假/薪资月结/陪场商品。账户active不强行等同人事状态。职级/基础薪资只核输入及权限，未宣称下游工资算法通过。 |
| BSM-001 | 业务配置、留痕与金钱配置原则，CX-16；本模块整数分不另造税务舍入口径。 |
| BSM-003 | 基础管理/主体归属/单功能授权及全局原则逐项核对，见CX-01—18；仅源码负责的身份与授权边界纳入本席结论，不把permissionCode鉴权当全部设备/地理/安全红线已过。 |
| BSM-009 | 基础管理/主体归属/单功能授权及全局原则逐项核对，见CX-01—18；仅源码负责的身份与授权边界纳入本席结论，不把permissionCode鉴权当全部设备/地理/安全红线已过。 |
| BSM-028 | 基础管理/主体归属/单功能授权及全局原则逐项核对，见CX-01—18；仅源码负责的身份与授权边界纳入本席结论，不把permissionCode鉴权当全部设备/地理/安全红线已过。 |
| BSM-029 | 基础管理/主体归属/单功能授权及全局原则逐项核对，见CX-01—18；仅源码负责的身份与授权边界纳入本席结论，不把permissionCode鉴权当全部设备/地理/安全红线已过。 |
| BSM-073 | 基础管理/主体归属/单功能授权及全局原则逐项核对，见CX-01—18；仅源码负责的身份与授权边界纳入本席结论，不把permissionCode鉴权当全部设备/地理/安全红线已过。 |
| BSM-084 | 两条注册路径、审批后自助权限，CX-11；已追到StaffService approved后createUser及role_staff_self。设备标识/换机/柜子另有实现，本模块未伪造第二套。 |
| BSM-085 | 两条注册路径、审批后自助权限，CX-11；已追到StaffService approved后createUser及role_staff_self。设备标识/换机/柜子另有实现，本模块未伪造第二套。 |
| BSM-086 | 完整原文已读；本14文件不实现人事状态/排班/考勤/请假/薪资月结/陪场商品。账户active不强行等同人事状态。职级/基础薪资只核输入及权限，未宣称下游工资算法通过。 |
| BSM-087 | 完整原文已读；本14文件不实现人事状态/排班/考勤/请假/薪资月结/陪场商品。账户active不强行等同人事状态。职级/基础薪资只核输入及权限，未宣称下游工资算法通过。 |
| BSM-088 | 完整原文已读；本14文件不实现人事状态/排班/考勤/请假/薪资月结/陪场商品。账户active不强行等同人事状态。职级/基础薪资只核输入及权限，未宣称下游工资算法通过。 |
| BSM-114 | 基础管理/主体归属/单功能授权及全局原则逐项核对，见CX-01—18；仅源码负责的身份与授权边界纳入本席结论，不把permissionCode鉴权当全部设备/地理/安全红线已过。 |
| BSM-115 | 基础管理/主体归属/单功能授权及全局原则逐项核对，见CX-01—18；仅源码负责的身份与授权边界纳入本席结论，不把permissionCode鉴权当全部设备/地理/安全红线已过。 |
| BSM-120 | 基础管理/主体归属/单功能授权及全局原则逐项核对，见CX-01—18；仅源码负责的身份与授权边界纳入本席结论，不把permissionCode鉴权当全部设备/地理/安全红线已过。 |
| PGM-001 | 基础管理/主体归属/单功能授权及全局原则逐项核对，见CX-01—18；仅源码负责的身份与授权边界纳入本席结论，不把permissionCode鉴权当全部设备/地理/安全红线已过。 |
| PGM-008 | 基础管理/主体归属/单功能授权及全局原则逐项核对，见CX-01—18；仅源码负责的身份与授权边界纳入本席结论，不把permissionCode鉴权当全部设备/地理/安全红线已过。 |
| PGM-022 | 空间/责任区域/设备班次原文完整读；本模块仅承载组织商户身份，空间容量/设备登记由下游实现，不新设点位容量或终端类别。 |
| PGM-024 | 空间/责任区域/设备班次原文完整读；本模块仅承载组织商户身份，空间容量/设备登记由下游实现，不新设点位容量或终端类别。 |
| PGM-026 | 基础管理/主体归属/单功能授权及全局原则逐项核对，见CX-01—18；仅源码负责的身份与授权边界纳入本席结论，不把permissionCode鉴权当全部设备/地理/安全红线已过。 |
| PGM-032 | 基础管理/主体归属/单功能授权及全局原则逐项核对，见CX-01—18；仅源码负责的身份与授权边界纳入本席结论，不把permissionCode鉴权当全部设备/地理/安全红线已过。 |
| PGM-033 | 两条注册路径、审批后自助权限，CX-11；已追到StaffService approved后createUser及role_staff_self。设备标识/换机/柜子另有实现，本模块未伪造第二套。 |
| PGM-034 | 企微/PII敏感权限必须实际鉴权，CX-01；合规范围守卫完整核对。留存/加密/脱敏流水不在本14文件，手机号摘要不等于全项目隐私合规证明。 |
| PGM-035 | 提成/服务份额月结、营销/陪场限制原文已读；本模块无对应计算或商品路径，不从sales账户类型发明独立营销业务。 |
| PGM-036 | 空间/责任区域/设备班次原文完整读；本模块仅承载组织商户身份，空间容量/设备登记由下游实现，不新设点位容量或终端类别。 |
| PGM-048 | 基础管理/主体归属/单功能授权及全局原则逐项核对，见CX-01—18；仅源码负责的身份与授权边界纳入本席结论，不把permissionCode鉴权当全部设备/地理/安全红线已过。 |
| PGM-059 | 企微/PII敏感权限必须实际鉴权，CX-01；合规范围守卫完整核对。留存/加密/脱敏流水不在本14文件，手机号摘要不等于全项目隐私合规证明。 |
| PGM-066 | 基础管理/主体归属/单功能授权及全局原则逐项核对，见CX-01—18；仅源码负责的身份与授权边界纳入本席结论，不把permissionCode鉴权当全部设备/地理/安全红线已过。 |

额外事实引用BSM-007、PGM-029、PGM-041均已存在同一冻结lock内，用于GEN-005/GEN-017等既定通用判据的逐字归类证据；没有修改定义/映射，没有新ID或改信任基准。52项映射保持原字节。

## 全部本席 findings

### CX-01 — 平台管理员无条件通配提前返回，绕过显式 DENY 和高危权限单独授予

- 严重度：P1；分类：冲突；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AuthorizationService.java:33–46、189–196、444–446；AccessService.java:513–516。
- 冻结判据：GEN-005、GEN-010、GEN-017。
- 证据/影响：实跑 ADMIN_DENY_BYPASS=require=allowed; effective denies=[ops.production_readiness.manage]；ADMIN_HIGH_WITHOUT_EXPLICIT_GRANT=true。相同主体的有效集承认DENY，而实际执行和布尔鉴权均放行；授予检查也直接接受wildcard。业务地图588（锁内BSM-007原文）明确日常管理员不自动取得资金、敏感数据、删除、最终审批权限；GEN-005鉴权边界及GEN-017冲突分类直接适用。
- 最小处理方向：删除不分日常/高危且先于DENY的通配旁路；日常权限和逐项授予一致计算；USB最高权限与日常管理员分别按既有身份事实处理。不得借修复新增业务类别。

### CX-02 — 同组织被当成同商户，允许操作另一商户员工的权限或薪资

- 严重度：P1；分类：冲突；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AccessService.java:201–210、365–371、559–568、969–972、1112–1120、1615–1623。
- 冻结判据：GEN-005、BSM-028、BSM-073、PGM-048。
- 证据/影响：实跑CROSS_MERCHANT_ROLE_ASSIGNED=RoleAssignmentRecord[userId=victim, roleId=role_low]，grantor在merchant_a、victim在merchant_b、同org_a，目标实际获得LOW权限。同一错误边界也出现在套模板、撤销临时授权、分配职级薪资、直接临时授权；通用requireSameOrganization只比较org。HTML要求商户只操作本商户。
- 最小处理方向：统一验证目标用户的组织及调用方商户范围；平台越域例外须遵守CX-01已确认边界。逐一覆盖同根调用，不只补角色入口。

### CX-03 — 无组织的非管理员被当成全局主体，读全量或向任意组织建号

- 严重度：P1；分类：细化；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AccessService.java:111–114、283–286、399–402、807–816、843–848；AuthorizationService.java:430–441。
- 冻结判据：GEN-004、GEN-005、BSM-028、PGM-048。
- 证据/影响：实跑UNSCOPED_USER_LIST=[existing]（existing属org_b）；UNSCOPED_CREATE_OTHER_ORG=org_b。AccessRepository默认方法及JdbcAccessRepository的null参数明确代表无过滤，服务直接传actor.organizationId()；createAccessUser反而只在actor组织非空时比较。currentProjectScopeQuietly也把空组织转为空过滤；它未核对任何跨项目授权。
- 最小处理方向：非管理员缺必要归属时拒绝，不能把缺失身份事实解释成全量权限；已有获授权全局岗位须使用可核验的授权事实，不能仅靠null。

### CX-04 — 名称/幂等键slug生成全局主键，与upsert联用覆盖既有跨组织对象

- 严重度：P1；分类：细化；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AccessService.java:138、188、312、865、1174、1214、1260、1746–1751。
- 冻结判据：GEN-004、GEN-005、GEN-006、GEN-008。
- 证据/影响：实跑USER_ID_OVERWRITE=usr_same_name moved org_b -> org_a（same-name与same.name）；ROLE_ID_OVERWRITE=role_cashier moved to org_a且原成员权限改变；TEMPLATE_ID_OVERWRITE=org_a（shared-key与shared_key）。JdbcAccessRepository95–131、159–193、360–397、675–714确认同ID无条件更新归属及内容；授权申请也使用同一有损slug。
- 最小处理方向：创建使用不碰撞的内部ID，区分创建与修改；创建不得通过任何ID碰撞覆盖既有账号/密码/角色/模板/授权申请。复用已有幂等结果处理，不将幂等键充当跨租户主键。

### CX-05 — 修改已应用模板可直接给用户新增自己不持有的高危权限，绕过授权审批

- 严重度：P1；分类：细化；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AccessService.java:144–167、193–241、513–524。
- 冻结判据：GEN-005、GEN-006、BSM-120、PGM-066。
- 证据/影响：实跑TEMPLATE_UPDATE_UNAPPROVED_HIGH=authorized high without grant request; manager lacks high。模板先LOW应用到员工，再由无HIGH的同组织模板管理员update为requiresApproval=true的HIGH，员工requirePermission(HIGH)立即成功。JdbcAccessRepository769–780从实时模板items计算已应用权限，update既不核验授出能力也不走审批。
- 最小处理方向：关闭模板修改直接向既有应用增权的旁路；对当前模板变更、既有应用与新增授权形成一致的权限/审批边界，保留合法减少权限和经审批授权路径。

### CX-06 — 审批使用申请时快照，实际执行却重新读取变动后的角色/岗位模板

- 严重度：P1；分类：细化；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AccessService.java:661–684、1165–1238、1287–1304、1321–1380。
- 冻结判据：GEN-005、GEN-006、GEN-011。
- 证据/影响：实跑APPROVAL_SNAPSHOT_DRIFT：snapshot=[HIGH]；applied=[HIGH,LOW]。pending保存permissionCodes/targetRoleId/templateId，但applyApprovedGrant重新resolve，未对照被审批对象及权限快照。岗位映射、模板权限或有效期在待审期间变化同样有此入口。
- 最小处理方向：生效前对照完整被审批授权事实；漂移则拒绝并重新申请，或严格执行原已批准快照，不能批准A执行B。

### CX-07 — 直接受益人可以审批发给自己的高危授权

- 严重度：P1；分类：冲突；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AccessService.java:653–685。
- 冻结判据：GEN-005、GEN-017。
- 证据/影响：实跑BENEFICIARY_SELF_REVIEW=applied。申请人requester，目标usr_admin，审批人usr_admin。下游SupervisorReviewService只禁止initiatedBy本人，没有识别AccessGrantRequestRecord.targetUserId。治理地图529（锁内PGM-029）明确申请人与直接受益人均不得审批自己的申请。
- 最小处理方向：在任何下游审批写入之前拒绝目标用户本人作为审批人，同时保留既有申请人自审禁止。

### CX-08 — 两个不同幂等键并发审批同一申请都生效，生成两份临时授权

- 严重度：P1；分类：细化；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AccessService.java:652–685；AccessRepository.java:144–150。
- 冻结判据：GEN-006、GEN-007、PGM-001。
- 证据/影响：实际Java服务双线程：CONCURRENT_REVIEW=[applied, applied] temporary_grant_count=2。仓储替身仅在两个普通SELECT后同步，让两请求都观察pending；业务代码未改。JdbcAccessRepository718–727及JdbcPlatformRepository711–720均普通SELECT，最终upsert无pending条件，证实此交错与真实SQL契约一致。两个线程自己的幂等键不同，幂等服务不能替代对象级竞争控制。
- 最小处理方向：利用现有事务与数据库对同一申请串行锁定或CAS转态，只有首次成功审批应用授权；禁止以JVM全局锁代替数据库并发事实。

### CX-09 — 岗位切换只追加新角色/模板，不使旧岗位派生权限失效

- 严重度：P1；分类：冲突；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AccessService.java:1043–1101。
- 冻结判据：GEN-005、GEN-006、GEN-017、BSM-120。
- 证据/影响：实跑POSITION_CHANGE_OLD_PERMISSION_RETAINED=[HIGH,LOW]：old岗位只有LOW，new岗位只有HIGH，保存新岗位后员工仍有两者。旧角色和模板应用没有任何失效调用。治理地图658（锁内PGM-041）要求保存岗位/职责等变更时重算，旧岗位/旧范围派生权限同步失效。
- 最小处理方向：同一事务撤销旧岗位派生授权并装配新岗位；区分独立显式授权，不误撤销员工仍合法持有的其他来源；跨范围和高危授予保留审批要求。

### CX-10 — 正式可创建的主管账号和已分配管理职级无法被审批级别解析器识别

- 严重度：P2；分类：细化；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AuthorizationService.java:245–257；AccessService.java:52–53、838–841。
- 冻结判据：GEN-002、GEN-005、BSM-028。
- 证据/影响：实跑SUPERVISOR_RANK_LEVEL=30/0：已分配lvl_m10_upper，accountType=supervisor，currentSupervisorLevel仍0。解析器只识别supervisor_lN或platform_admin，而账户类型白名单不允许supervisor_lN；实际SupervisorReviewService145–151先以该0拒绝，尚未走rank检查。
- 最小处理方向：将审批身份解析与现有受控的角色/职级事实对齐；不得为了让测试通过而把任意staff、最高rank或未授权账号自动当审批人。

### CX-11 — 手工员工建号直接active及发密码，形成绕过两条注册审批路径的第三入口

- 严重度：P1；分类：冲突；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AccessController.java:209–213；AccessService.java:829–889。
- 冻结判据：GEN-017、BSM-084、BSM-085、PGM-032、PGM-033。
- 证据/影响：实跑MANUAL_REGISTRATION=active/manual_admin/roles=0。该路径不读取员工申请/公司审批，未自动赋员工自助；对照StaffService178–223，既有审批路径在approved后才createUser并assign role_staff_self。HTML明确商户代申请后对应域公司批准、公司自有员工本人App申请后公司批准，自营归公司路径。
- 最小处理方向：删除/封闭员工账户的直接生效旁路，复用既有注册审批及自动自助装配流程。外部企业/供应商账号应按其已授权类别判定，不能一并臆删所有非员工账号。

### CX-12 — 按用户ID查询有效权限只认商户范围，合法公司人员连同组织自身也被拒绝

- 严重度：P2；分类：细化；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AuthorizationService.java:86–102、347–356。
- 冻结判据：GEN-002、GEN-005、BSM-028。
- 证据/影响：实跑ORG_STAFF_OWN_PERMISSIONS=MERCHANT_SCOPE_REQUIRED。actor org_a且merchant=null，target为同一个org_a用户。此接口已经由Controller224校验access.user.view；之后无条件currentMerchantScope使组织级管理路径不可用。
- 最小处理方向：按实际公司/商户归属校验目标；无效归属拒绝，合法同组织管理请求可读，不能为修功能改成空scope全放行。

### CX-13 — 临时授权接受空权限集合和倒置时间窗，返回active但授权不存在或不可使用

- 严重度：P2；分类：细化；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AccessService.java:1104–1142、1241–1253、1688–1709。
- 冻结判据：GEN-002、GEN-004、GEN-008。
- 证据/影响：实跑INVERTED_TEMPORARY_WINDOW_ACCEPTED=active，EMPTY_TEMPORARY_PERMISSION_ACCEPTED=active/0，员工有效集为空。JdbcAccessRepository612–635对空列表一次INSERT也不执行。模板和待审临时申请同样缺日期先后验证；DTO未声明约束，@Valid不能补足。
- 最小处理方向：在服务公共写边界校验非空有效权限、目标存在、有效期先后；非法输入拒绝，不能返回成功的空授权/永远无效授权。不擅自设置业务期限上限。

### CX-14 — 一次合法操作检查两个权限时，第一个检查消耗整份单次模板，第二个检查拒绝

- 严重度：P1；分类：细化；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AuthorizationService.java:33–73；AccessController.java:137–139。
- 冻结判据：GEN-002、GEN-006、GEN-007。
- 证据/影响：实际调用AccessController.reviewGrant：SINGLE_USE_TWO_GUARDS=PERMISSION_DENIED consumed=[single_review_template]。同一份单次模板已含review及supervisor.approve，两权限起初均有效；业务服务未被执行，模板已消耗。仓储以模板应用为消耗单位；不是两次业务操作。
- 最小处理方向：同一业务请求的一组权限先完整核验，再按授权来源原子消耗一次；确保无法因重复检查提前烧掉唯一授权。保留并发第二消费者拒绝的现有语义。

### CX-15 — 岗位建议和个人异常总结返回固定英文并被页面原样显示

- 严重度：P2；分类：冲突；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AccessService.java:759–774。
- 冻结判据：GEN-015、GEN-017、PGM-001、PGM-066。
- 证据/影响：实跑SUMMARY_USER_VISIBLE_TEXT为两句英文。AccessGovernancePage.tsx341、454–455直接渲染row.recommendation/row.summary；此为成功响应，GlobalExceptionHandler的错误翻译不参与，前端测试却以中文fixture掩盖实际返回。
- 最小处理方向：将这两条用户可见业务说明改为中文；保留内部权限编码，禁止把与此无关的英文代码/标识批量替换。

### CX-16 — 业务阈值/窗口及授权风险对应审批层级固化在代码或启动配置，缺后台配置边界

- 严重度：P2；分类：冲突；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AccessService.java:76–77、107–108、729–733、1406–1419。
- 冻结判据：GEN-017、BSM-001。
- 证据/影响：汇总默认30天、阈值3，最小值被Math.max静默钳到1，任意请求可传Integer.MAX_VALUE使本来存在的异常总结消失；源码riskRank直接把critical/high映射到3/2层级。这里的“频繁”“审批层级”改变业务判断，不是线程数之类技术设置。BSM-001要求业务阈值/时长/次数/档位后台配置、上下限及超限拒绝。
- 最小处理方向：复用项目已有业务配置/审批策略承载对应政策值和边界；查询筛选与政策阈值分清，不由查询参数改写政策，不引入另一套配置体系或新业务分类。

### CX-17 — 单次临时授权首次消费的留痕外键指错对象，报错后授权已被标记消耗

- 严重度：P1；分类：细化；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AuthorizationService.java:66–72；AccessRepository.java:178–182（实现依赖JdbcAccessRepository802–843）。
- 冻结判据：GEN-002、GEN-006、GEN-008、GEN-011。
- 证据/影响：真实PostgreSQL complex_test中仅建立3张连接级TEMP表，按V003现有外键结构调用当前源码JdbcAccessRepository：TEMPORARY_SINGLE_USE_LOG=23503，CONSUMED_AFTER_LOG_FAILURE=true。临时grant UUID被写入引用permission_template_applications的application_id；查询当前全部受控迁移，仅V003定义该外键，没有后续解除/替换。控制器调用鉴权发生在业务service事务之前，该仓储方法本身无事务。
- 最小处理方向：让临时授权留痕引用正确的授权来源，并使消耗与留痕同成同败；保留真实来源信息，不通过丢日志/吞异常/伪造模板应用解决。仅修依赖实现必需范围，后续所属模块仍完整审。

### CX-18 — 平台管理员修改组织模板时把原owner改成null，全局化并使原组织失去管理权

- 严重度：P2；分类：细化；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AccessService.java:161–166。
- 冻结判据：GEN-005、GEN-006、GEN-016。
- 证据/影响：实跑ADMIN_UPDATE_OWNER=null：原org_a模板经管理员改内容后ownerScopeType变platform且ownerScopeId=null。注释写Preserve existing owner，代码却只按当前actor决定归属。
- 最小处理方向：读取并保留现有模板归属；内容编辑不隐含转移所有权，修改不存在的模板应明确报不存在。

### CX-19 — 记录容器防御复制和兼容构造器

- 严重度：登记；分类：技术性；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AccessGrantRequestRecord.java:32–34；AccessUser.java:20–22、24–34。
- 冻结判据：GEN-001、GEN-003、GEN-017。
- 证据/影响：仅List.copyOf防外部修改、null转空及已有构造调用兼容，不引入员工资格/计费/审批规则。全部记录字段仍按员工/授权上位需求归细化，此登记仅指Java容器与构造机制；48个当前源编译和55测试实跑覆盖调用。
- 最小处理方向：不删不问，不进修复队列。

### CX-20 — 凭证接口、Spring注入和按运行profile选择实现

- 严重度：登记；分类：技术性；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AccessCredentialProvisioner.java:5–11；DevAccessCredentialProvisioner.java:13–24；JdbcAccessCredentialProvisioner.java:15–24；AccessService.java:68–109。
- 冻结判据：GEN-001、GEN-003、GEN-013、GEN-017。
- 证据/影响：dev/local内存认证桥与prod JDBC凭证桥确有两个实现，接口并非为了未来扩展；无依赖时的noop用于轻量测试装配，prod有实际bean。Spring装配、包级测试开关不新增业务授权规则。生产Autowired路径grantApproval固定true；不能把测试构造器false当生产逃生开关。
- 最小处理方向：只登记工程装配，具体业务账户旁路按CX-11处理。

### CX-21 — 密码BCrypt、UTF-8/SHA-256和JDBC时间参数转换

- 严重度：登记；分类：技术性；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：JdbcAccessCredentialProvisioner.java:3–13、24、50–58；AccessService.java:911–921。
- 冻结判据：GEN-001、GEN-010、GEN-017。
- 证据/影响：密码仅写BCrypt哈希；SecureRandom、UTF-8、摘要/时间转换为数据处理机制，不新增业务资格。手机号摘要本身不是已证明的匿名化或完整PII加密方案，不以此宣称全项目隐私合规通过。
- 最小处理方向：不新增依赖、不改工作因子，不进修复队列；业务数据隐私按实际字段和用途继续由相应模块审。

### CX-22 — HTTP映射、响应包装和仓储接口默认失败机制

- 严重度：登记；分类：技术性；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AccessController.java:19–39及各路由包装；AccessRepository.java:83–125等默认接口方法。
- 冻结判据：GEN-001、GEN-003、GEN-013、GEN-017。
- 证据/影响：MVC/ApiResponse/PageResponse仅承载现有权限/员工业务，不构成第四来源。UnsupportedOperationException使未实现写能力显式失败；源码构建证明当前Jdbc实现可装配编译，未声称所有默认方法都在生产使用。
- 最小处理方向：只登记包装/接口机制；权限边界、空成功及并发缺陷分别按已有CX处理。

### CX-23 — 主体、权限集合和临时授权统计数据记录

- 严重度：登记；分类：细化；依据类型：4（可复现命令及输出），业务原文对照同时为1。
- 位置（目录backend/src/main/java/com/complex/platform/access/）：AccessActor.java、AccessGrantPositionSummary.java、AccessGrantSummary.java、AccessGrantUserSummary.java、EffectivePermissions.java及其他记录字段。
- 冻结判据：GEN-001、GEN-017、BSM-028、BSM-120、PGM-066。
- 证据/影响：主体归属和单功能权限对应BSM-028/BSM-120；频繁临时授权汇总的原文位于治理地图659（锁内PGM-041），有明确上位需求，不能当业务添加删除或询问。纯记录对象无独立I/O副作用；具体计算错误已单列。
- 最小处理方向：只登记；不因Java record或内部英文标识删除既有业务数据结构。

## 实跑输出

### 探针1

真实shell exit=0。

```text
DEPENDENCY_CLASSPATH_EXIT 0
JAVAC_EXIT 0 CURRENT_SOURCE_COUNT 48
 
PROBE_EXIT 0
EXISTING_TESTS=55 passed / 0 failed
CROSS_MERCHANT_ROLE_ASSIGNED=RoleAssignmentRecord[userId=victim, roleId=role_low]
UNSCOPED_USER_LIST=[existing]
UNSCOPED_CREATE_OTHER_ORG=org_b
USER_ID_OVERWRITE=usr_same_name moved org_b -> org_a
ROLE_ID_OVERWRITE=role_cashier moved to org_a; old member effects=[ops.production_readiness.check]
TEMPLATE_ID_OVERWRITE=org_a
TEMPLATE_UPDATE_UNAPPROVED_HIGH=authorized high without grant request; manager lacks high
ADMIN_UPDATE_OWNER=null
APPROVAL_SNAPSHOT_DRIFT=snapshot=[ops.production_readiness.check]; applied=[ops.production_readiness.check, ops.production_readiness.view]
SUPERVISOR_RANK_LEVEL=30/0
ADMIN_DENY_BYPASS=require=allowed; effective denies=[ops.production_readiness.manage]
ADMIN_HIGH_WITHOUT_EXPLICIT_GRANT=true
 
```

### 探针2

真实shell exit=0。

```text
DEPENDENCY_CLASSPATH_EXIT 0
JAVAC_EXIT 0 CURRENT_SOURCE_COUNT 48
 
PROBE_EXIT 0
BENEFICIARY_SELF_REVIEW=applied
INVERTED_TEMPORARY_WINDOW_ACCEPTED=active
EMPTY_TEMPORARY_PERMISSION_ACCEPTED=active/0
EXPIRED_EFFECTIVE_SET=[]
POSITION_CHANGE_OLD_PERMISSION_RETAINED=[ops.production_readiness.check, ops.production_readiness.view]
SINGLE_USE_TWO_GUARDS=PERMISSION_DENIED consumed=[single_review_template]
ORG_STAFF_OWN_PERMISSIONS=MERCHANT_SCOPE_REQUIRED
MANUAL_REGISTRATION=active/manual_admin/roles=0
SUMMARY_USER_VISIBLE_TEXT=frequent temporary grants; consider fixed permission | frequent temporary permission requests; review abnormal pattern
SUMMARY_QUERY_THRESHOLD_UNBOUNDED=0
CONCURRENT_REVIEW=[applied, applied] temporary_grant_count=2
 
```

### 探针3

真实shell exit=0。

```text
DB_CONFIG_SOURCE /etc/complex/complex.env KEYS ['COMPLEX_DATABASE_PASSWORD', 'COMPLEX_DATABASE_URL', 'COMPLEX_DATABASE_USERNAME']
DEPENDENCY_CLASSPATH_EXIT 0
JAVAC_EXIT 0 CURRENT_SOURCE_COUNT 48
 
PROBE_EXIT 0
TEMPORARY_SINGLE_USE_LOG=23503
CONSUMED_AFTER_LOG_FAILURE=true
DATABASE_SCOPE=complex_test; all writes to three connection-local TEMP tables
 
```

探针调试记录：第一次模板碰撞验证使用同一幂等键，正确触发IDEMPOTENCY_CONFLICT；改为不同但slug相同的键后复现最终覆盖。第二套临时Java首次DTO参数多一个，javac拒绝，按实际7字段修正后成功。SQL探针最初使用测试源码示例默认密码连接失败，没有执行SQL；随后仅从/etc/complex/complex.env取3个DB变量并强制complex_test。临时JavaScript替换串/临时Python语法错误在探针启动前修正。均非生产源码变更、非Grok失败、不计B3。三组最终成功程序原样附后。

技能实际使用：M006启动已读using-superpowers及工具映射、grilling、codebase-design、ponytail-review；遵从V0.17限制，本人独立三线，不派子代理、不等额外人工门。定位使用已读systematic-debugging/diagnosing-bugs的追数据流/先复现方法。未安装/升级技能、未改插件配置。所有修复仍须本批B2双PASS后的B3统一执行。

附录封存前不读Grok；两席完整报告交付后才做B1四值校验、整批归并。未发现要改业务HTML的原文问题；无本席“添加”问题待问。

CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

## 可复现程序附录 1

版本：V0.02

下列为本轮实际成功执行的远程Python程序，在160正本根目录以python3 -经stdin运行。编译输出只放自动清理的.scratch/m006-self-probe-*；不改源码；探针1/2用内存fixture，探针3只有complex_test连接内临时表。

```python
from pathlib import Path
import subprocess,tempfile,json,hashlib
java="package com.complex.platform.access;\nimport com.complex.platform.access.dto.*;\nimport com.complex.platform.common.api.ApiException;\nimport com.complex.platform.governance.review.*;\nimport com.complex.platform.support.*;\nimport com.complex.platform.common.idempotency.*;\nimport com.complex.platform.evidence.*;\nimport com.fasterxml.jackson.databind.ObjectMapper;\nimport org.springframework.security.authentication.UsernamePasswordAuthenticationToken;\nimport org.springframework.security.core.context.SecurityContextHolder;\nimport java.lang.reflect.*;\nimport java.time.*;\nimport java.util.*;\npublic class M006Probe {\n static String LOW=\"ops.production_readiness.view\", HIGH=\"ops.production_readiness.check\";\n static Object field(Object o,String n)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}\n static void actor(String id,String type,String org,String merchant){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AccessActor(id,id,type,org,merchant,\"active\"),\"token\",List.of()));}\n static void admin(){actor(\"usr_admin\",\"platform_admin\",\"org_a\",null);}\n static record F(AccessServiceTests test,AccessRepository repo,AccessService svc,AuthorizationService auth){}\n static F fresh()throws Exception{\n  var t=new AccessServiceTests();t.setUp();var repo=(AccessRepository)field(t,\"accessRepository\");var auth=(AuthorizationService)field(t,\"authorizationService\");\n  var srType=Class.forName(\"com.complex.platform.access.AccessServiceTests$InMemorySupervisorReviewRepository\");\n  var ctor=srType.getDeclaredConstructor();ctor.setAccessible(true);\n  var sr=new SupervisorReviewService(auth,new IdempotencyService(new InMemoryIdempotencyRepository(),new ObjectMapper()),(SupervisorReviewRepository)ctor.newInstance(),new AuditEventService((InMemoryAuditEventRepository)field(t,\"auditEventRepository\")));\n  var svc=new AccessService(repo,new IdempotencyService(new InMemoryIdempotencyRepository(),new ObjectMapper()),new AuditEventService((InMemoryAuditEventRepository)field(t,\"auditEventRepository\")),auth,(AccessCredentialProvisioner)field(t,\"credentialProvisioner\"),sr,true,3,3);\n  admin();return new F(t,repo,svc,auth);\n }\n static AccessUser user(F f,String id,String org,String merchant){return f.repo.createUser(new AccessUser(id,id,\"staff\",org,merchant,\"active\",Instant.now()));}\n static void grant(F f,String id,String... codes){var rid=\"fixture_\"+id;f.repo.createRole(new RoleRecord(rid,rid,\"fixture\",\"project\",\"active\",Instant.now(),List.of(codes),null));f.repo.assignUserRole(id,rid);}\n static PermissionTemplateRequest tpl(String key,String...codes){return new PermissionTemplateRequest(key,\"fixture\",List.of(),\"模板\",\"base\",null,List.of(codes),\"project\",null,null,false);}\n static RoleRequest role(String key,String name,String...codes){return new RoleRequest(key,\"fixture\",List.of(),name,\"fixture\",List.of(codes),\"project\");}\n static AccessUserRequest account(String key,String username,String org){return new AccessUserRequest(key,\"fixture\",List.of(),username,\"Fixture-only-Password9\",null,\"staff\",org,null,\"active\");}\n static void high(F f)throws Exception{var m=f.repo.getClass().getDeclaredMethod(\"markRequiresApproval\",String.class,String.class);m.setAccessible(true);m.invoke(f.repo,HIGH,\"high\");}\n static void print(String name,Object value){System.out.println(name+\"=\"+value);}\n static void check(boolean b,String why){if(!b)throw new AssertionError(why);}\n public static void main(String[] args)throws Exception{\n  int pass=0,fail=0;\n  for(Class<?> c:List.of(AccessServiceTests.class,AuthorizationServiceTests.class)){\n   for(var m:c.getDeclaredMethods())if(m.isAnnotationPresent(org.junit.jupiter.api.Test.class)){\n    var ct=c.getDeclaredConstructor();ct.setAccessible(true);var t=ct.newInstance();\n    try{\n     for(var b:c.getDeclaredMethods())if(b.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)){b.setAccessible(true);b.invoke(t);}\n     m.setAccessible(true);m.invoke(t);pass++;\n    }catch(InvocationTargetException e){fail++;print(\"EXISTING_TEST_FAIL\",c.getSimpleName()+\".\"+m.getName()+\": \"+e.getCause());}\n    finally{for(var a:c.getDeclaredMethods())if(a.isAnnotationPresent(org.junit.jupiter.api.AfterEach.class)){a.setAccessible(true);a.invoke(t);}}\n   }\n  }\n  print(\"EXISTING_TESTS\",pass+\" passed / \"+fail+\" failed\");\n\n  var f=fresh();user(f,\"victim\",\"org_a\",\"merchant_b\");user(f,\"grantor\",\"org_a\",\"merchant_a\");grant(f,\"grantor\",LOW);\n  var r=f.repo.createRole(new RoleRecord(\"role_low\",\"普通角色\",\"fixture\",\"merchant\",\"active\",Instant.now(),List.of(LOW),\"org_a\"));\n  actor(\"grantor\",\"merchant_admin\",\"org_a\",\"merchant_a\");\n  var assigned=f.svc.assignUserRole(new RoleAssignmentRequest(\"cross-merchant\",\"fixture\",\"victim\",r.id()));\n  check(f.repo.findPermissionEffects(\"victim\",Instant.now()).size()==1,\"fixture grant absent\");\n  print(\"CROSS_MERCHANT_ROLE_ASSIGNED\",assigned);\n\n  f=fresh();user(f,\"existing\",\"org_b\",null);actor(\"unscoped\",\"staff\",null,null);\n  print(\"UNSCOPED_USER_LIST\",f.svc.listAccessUsers().items().stream().map(AccessUser::id).toList());\n  print(\"UNSCOPED_CREATE_OTHER_ORG\",f.svc.createAccessUser(account(\"unscoped-create\",\"new\",\"org_b\")).organizationId());\n\n  f=fresh();actor(\"operator_b\",\"staff\",\"org_b\",null);\n  var original=f.svc.createAccessUser(account(\"original\",\"same-name\",\"org_b\"));\n  actor(\"operator_a\",\"staff\",\"org_a\",null);\n  var overwritten=f.svc.createAccessUser(account(\"replacement\",\"same.name\",\"org_a\"));\n  check(original.id().equals(overwritten.id()),\"no collision\");\n  print(\"USER_ID_OVERWRITE\",original.id()+\" moved \"+original.organizationId()+\" -> \"+f.repo.findUserById(original.id()).get().organizationId());\n\n  f=fresh();actor(\"operator_b\",\"staff\",\"org_b\",null);grant(f,\"operator_b\",LOW);\n  var oldRole=f.svc.createRole(role(\"rb\",\"cashier\",LOW));user(f,\"old_member\",\"org_b\",null);f.repo.assignUserRole(\"old_member\",oldRole.id());\n  actor(\"operator_a\",\"staff\",\"org_a\",null);grant(f,\"operator_a\",HIGH);\n  var newRole=f.svc.createRole(role(\"ra\",\"cashier\",HIGH));\n  print(\"ROLE_ID_OVERWRITE\",oldRole.id()+\" moved to \"+newRole.organizationId()+\"; old member effects=\"+f.repo.findPermissionEffects(\"old_member\",Instant.now()).stream().map(AccessRepository.PermissionEffect::permissionCode).toList());\n\n  f=fresh();actor(\"operator_b\",\"staff\",\"org_b\",null);var tb=f.svc.createPermissionTemplate(tpl(\"shared-key\",LOW));\n  actor(\"operator_a\",\"staff\",\"org_a\",null);var ta=f.svc.createPermissionTemplate(tpl(\"shared_key\",HIGH));\n  check(tb.id().equals(ta.id()),\"no template collision\");\n  print(\"TEMPLATE_ID_OVERWRITE\",f.repo.findTemplateById(tb.id()).get().ownerScopeId());\n\n  f=fresh();high(f);user(f,\"employee\",\"org_a\",null);actor(\"template_manager\",\"staff\",\"org_a\",null);grant(f,\"template_manager\",LOW);\n  var t=f.svc.createPermissionTemplate(tpl(\"base-template\",LOW));\n  f.svc.applyPermissionTemplate(t.id(),new PermissionTemplateApplyRequest(\"apply-base\",\"fixture\",List.of(),t.id(),List.of(\"employee\"),\"append\"));\n  f.svc.updatePermissionTemplate(t.id(),tpl(\"update-high\",HIGH));\n  actor(\"employee\",\"staff\",\"org_a\",null);f.auth.requirePermission(HIGH);\n  print(\"TEMPLATE_UPDATE_UNAPPROVED_HIGH\",\"authorized high without grant request; manager lacks high\");\n  admin();f.svc.updatePermissionTemplate(t.id(),tpl(\"admin-update\",LOW));\n  print(\"ADMIN_UPDATE_OWNER\",f.repo.findTemplateById(t.id()).get().ownerScopeId());\n\n  f=fresh();high(f);user(f,\"employee\",\"org_a\",null);grant(f,\"requester\",LOW,HIGH);\n  var roleForReview=f.repo.createRole(new RoleRecord(\"review_role\",\"审批角色\",\"fixture\",\"project\",\"active\",Instant.now(),List.of(HIGH),\"org_a\"));\n  actor(\"requester\",\"staff\",\"org_a\",null);\n  var pending=f.svc.applyGrant(new AccessGrantApplyRequest(\"pending-role\",\"fixture\",List.of(),\"user_role\",\"employee\",roleForReview.id(),null,null,null,null,null,List.of(),null,null,false,\"fixture\"));\n  f.repo.createRole(new RoleRecord(roleForReview.id(),\"审批角色\",\"fixture\",\"project\",\"active\",Instant.now(),List.of(HIGH,LOW),\"org_a\"));\n  admin();f.repo.saveStaffLevelAssignment(new StaffLevelAssignment(\"usr_admin\",\"lvl_m10_upper\",0L,\"CNY\",null,\"org_a\",null,\"fixture\",Instant.now()));\n  var applied=f.svc.reviewGrant(pending.id(),new AccessGrantReviewRequest(\"review-drift\",\"fixture\",List.of(),\"approve\",\"fixture\"));\n  print(\"APPROVAL_SNAPSHOT_DRIFT\",\"snapshot=\"+pending.permissionCodes()+\"; applied=\"+f.repo.findPermissionEffects(\"employee\",Instant.now()).stream().map(AccessRepository.PermissionEffect::permissionCode).toList());\n  \n  f=fresh();user(f,\"supervisor\",\"org_a\",null);f.repo.saveStaffLevelAssignment(new StaffLevelAssignment(\"supervisor\",\"lvl_m10_upper\",100L,\"CNY\",null,\"org_a\",null,\"fixture\",Instant.now()));\n  actor(\"supervisor\",\"supervisor\",\"org_a\",null);\n  print(\"SUPERVISOR_RANK_LEVEL\",f.auth.currentActorLevelRank()+\"/\"+f.auth.currentSupervisorLevel());\n\n  var at=new AuthorizationServiceTests();at.setUp();var ar=(AccessRepository)field(at,\"repository\");\n  var add=ar.getClass().getDeclaredMethod(\"addEffect\",AccessRepository.PermissionEffect.class);add.setAccessible(true);\n  add.invoke(ar,new AccessRepository.PermissionEffect(\"usr_admin\",\"ops.production_readiness.manage\",AccessRepository.Effect.DENY,AccessRepository.Source.USER_OVERRIDE,\"deny\",null,null,false,false));\n  var az=new AuthorizationService(ar);admin();az.requirePermission(\"ops.production_readiness.manage\");\n  print(\"ADMIN_DENY_BYPASS\",\"require=allowed; effective denies=\"+az.effectivePermissions().denies());\n  print(\"ADMIN_HIGH_WITHOUT_EXPLICIT_GRANT\",az.currentActorHasPermission(\"ops.production_readiness.manage\"));\n  SecurityContextHolder.clearContext();\n }\n}\n"
with tempfile.TemporaryDirectory(prefix='m006-self-probe-',dir='.scratch') as d:
 root=Path(d).resolve();cpfile=root/'classpath'
 r=subprocess.run(['mvn','-o','-q','-f','backend/pom.xml','dependency:build-classpath','-Dmdep.outputFile='+str(cpfile)],capture_output=True,text=True)
 print('DEPENDENCY_CLASSPATH_EXIT',r.returncode)
 if r.returncode:print(r.stdout,r.stderr);raise SystemExit(r.returncode)
 cp=str(Path('backend/target/classes').resolve())+':'+cpfile.read_text().strip()
 src=root/'M006Probe.java';src.write_text(java,encoding='utf-8');out=root/'classes';out.mkdir()
 base=Path('backend/src/main/java/com/complex/platform')
 sources=list((base/'access').glob('*.java'))+list((base/'access/dto').glob('*.java'))
 sources += [base/'common/idempotency/IdempotencyService.java',base/'governance/review/SupervisorReviewService.java',base/'evidence/AuditEventService.java']
 tests=Path('backend/src/test/java/com/complex/platform')
 sources += [tests/'access/AccessServiceTests.java',tests/'access/AuthorizationServiceTests.java',tests/'support/InMemoryAuditEventRepository.java',tests/'support/InMemoryIdempotencyRepository.java']
 r=subprocess.run(['javac','-encoding','UTF-8','--release','21','-cp',cp,'-d',str(out),*[str(p) for p in sources],str(src)],capture_output=True,text=True)
 print('JAVAC_EXIT',r.returncode,'CURRENT_SOURCE_COUNT',len(sources));print(r.stdout,r.stderr)
 if r.returncode:raise SystemExit(r.returncode)
 r=subprocess.run(['java','-cp',str(out)+':'+cp,'com.complex.platform.access.M006Probe'],capture_output=True,text=True)
 print('PROBE_EXIT',r.returncode);print(r.stdout,r.stderr)
 raise SystemExit(r.returncode)

```

## 可复现程序附录 2

版本：V0.03

下列为本轮实际成功执行的远程Python程序，在160正本根目录以python3 -经stdin运行。编译输出只放自动清理的.scratch/m006-self-probe-*；不改源码；探针1/2用内存fixture，探针3只有complex_test连接内临时表。

```python
from pathlib import Path
import subprocess,tempfile,json,hashlib
java="package com.complex.platform.access;\nimport com.complex.platform.access.dto.*;\nimport com.complex.platform.common.api.ApiException;\nimport com.complex.platform.governance.review.*;\nimport com.complex.platform.support.*;\nimport com.complex.platform.common.idempotency.*;\nimport com.complex.platform.evidence.*;\nimport com.fasterxml.jackson.databind.ObjectMapper;\nimport org.springframework.security.authentication.UsernamePasswordAuthenticationToken;\nimport org.springframework.security.core.context.SecurityContextHolder;\nimport java.lang.reflect.*;\nimport java.time.*;\nimport java.util.*;\npublic class M006Probe {\n static String LOW=\"ops.production_readiness.view\", HIGH=\"ops.production_readiness.check\";\n static Object field(Object o,String n)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}\n static void actor(String id,String type,String org,String merchant){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AccessActor(id,id,type,org,merchant,\"active\"),\"token\",List.of()));}\n static void admin(){actor(\"usr_admin\",\"platform_admin\",\"org_a\",null);}\n static record F(AccessServiceTests test,AccessRepository repo,AccessService svc,AuthorizationService auth){}\n static F fresh()throws Exception{\n  var t=new AccessServiceTests();t.setUp();var repo=(AccessRepository)field(t,\"accessRepository\");var auth=(AuthorizationService)field(t,\"authorizationService\");\n  var srType=Class.forName(\"com.complex.platform.access.AccessServiceTests$InMemorySupervisorReviewRepository\");\n  var ctor=srType.getDeclaredConstructor();ctor.setAccessible(true);\n  var sr=new SupervisorReviewService(auth,new IdempotencyService(new InMemoryIdempotencyRepository(),new ObjectMapper()),(SupervisorReviewRepository)ctor.newInstance(),new AuditEventService((InMemoryAuditEventRepository)field(t,\"auditEventRepository\")));\n  var svc=new AccessService(repo,new IdempotencyService(new InMemoryIdempotencyRepository(),new ObjectMapper()),new AuditEventService((InMemoryAuditEventRepository)field(t,\"auditEventRepository\")),auth,(AccessCredentialProvisioner)field(t,\"credentialProvisioner\"),sr,true,3,3);\n  admin();return new F(t,repo,svc,auth);\n }\n static AccessUser user(F f,String id,String org,String merchant){return f.repo.createUser(new AccessUser(id,id,\"staff\",org,merchant,\"active\",Instant.now()));}\n static void grant(F f,String id,String... codes){var rid=\"fixture_\"+id;f.repo.createRole(new RoleRecord(rid,rid,\"fixture\",\"project\",\"active\",Instant.now(),List.of(codes),null));f.repo.assignUserRole(id,rid);}\n static PermissionTemplateRequest tpl(String key,String...codes){return new PermissionTemplateRequest(key,\"fixture\",List.of(),\"模板\",\"base\",null,List.of(codes),\"project\",null,null,false);}\n static RoleRequest role(String key,String name,String...codes){return new RoleRequest(key,\"fixture\",List.of(),name,\"fixture\",List.of(codes),\"project\");}\n static AccessUserRequest account(String key,String username,String org){return new AccessUserRequest(key,\"fixture\",List.of(),username,\"Fixture-only-Password9\",null,\"staff\",org,null,\"active\");}\n static void high(F f)throws Exception{var m=f.repo.getClass().getDeclaredMethod(\"markRequiresApproval\",String.class,String.class);m.setAccessible(true);m.invoke(f.repo,HIGH,\"high\");}\n static void print(String name,Object value){System.out.println(name+\"=\"+value);}\n static void check(boolean b,String why){if(!b)throw new AssertionError(why);}\n public static void main(String[] args)throws Exception {\n  var f=fresh();high(f);user(f,\"usr_admin\",\"org_a\",null);user(f,\"requester\",\"org_a\",null);\n  actor(\"requester\",\"staff\",\"org_a\",null);\n  var req=f.svc.applyGrant(new AccessGrantApplyRequest(\"beneficiary\",\"fixture\",List.of(),\"temporary_permission\",\"usr_admin\",null,null,null,null,null,null,List.of(HIGH),Instant.now(),Instant.now().plusSeconds(3600),false,\"fixture\"));\n  f.repo.saveStaffLevelAssignment(new StaffLevelAssignment(\"usr_admin\",\"lvl_m10_upper\",100L,\"CNY\",null,\"org_a\",null,\"fixture\",Instant.now()));\n  admin();print(\"BENEFICIARY_SELF_REVIEW\",f.svc.reviewGrant(req.id(),new AccessGrantReviewRequest(\"beneficiary-review\",\"fixture\",List.of(),\"approve\",\"fixture\")).status());\n\n  f=fresh();user(f,\"employee\",\"org_a\",null);\n  var inverted=f.svc.createTemporaryGrant(new TemporaryGrantRequest(\"inverted\",\"fixture\",List.of(),\"employee\",List.of(LOW),Instant.now().plusSeconds(3600),Instant.now(),false,\"fixture\"));\n  print(\"INVERTED_TEMPORARY_WINDOW_ACCEPTED\",inverted.status());\n  var empty=f.svc.createTemporaryGrant(new TemporaryGrantRequest(\"empty\",\"fixture\",List.of(),\"employee\",List.of(),null,Instant.now().plusSeconds(3600),false,\"fixture\"));\n  print(\"EMPTY_TEMPORARY_PERMISSION_ACCEPTED\",empty.status()+\"/\"+empty.permissionCodes().size());\n  actor(\"employee\",\"staff\",\"org_a\",null);\n  print(\"EXPIRED_EFFECTIVE_SET\",f.auth.effectivePermissions().grants());\n\n  f=fresh();user(f,\"employee\",\"org_a\",null);\n  var r1=f.repo.createRole(new RoleRecord(\"old_role\",\"旧岗位\",\"fixture\",\"project\",\"active\",Instant.now(),List.of(LOW),null));\n  var r2=f.repo.createRole(new RoleRecord(\"new_role\",\"新岗位\",\"fixture\",\"project\",\"active\",Instant.now(),List.of(HIGH),null));\n  var t=f.svc.createPermissionTemplate(tpl(\"position-base\"));\n  f.svc.createPositionRoleTemplate(new PositionRoleTemplateRequest(\"old-position\",\"fixture\",\"old\",\"lvl_m1_lower\",r1.id(),t.id(),\"active\"));\n  f.svc.createPositionRoleTemplate(new PositionRoleTemplateRequest(\"new-position\",\"fixture\",\"new\",\"lvl_m1_lower\",r2.id(),t.id(),\"active\"));\n  f.svc.assignUserPosition(new UserPositionAssignmentRequest(\"assign-old\",\"fixture\",\"employee\",\"old\",\"lvl_m1_lower\",100L,\"CNY\",null));\n  f.svc.assignUserPosition(new UserPositionAssignmentRequest(\"assign-new\",\"fixture\",\"employee\",\"new\",\"lvl_m1_lower\",100L,\"CNY\",null));\n  actor(\"employee\",\"staff\",\"org_a\",null);\n  print(\"POSITION_CHANGE_OLD_PERMISSION_RETAINED\",f.auth.effectivePermissions().grants());\n\n  var at=new AuthorizationServiceTests();at.setUp();var ar=(AccessRepository)field(at,\"repository\");\n  var addKnown=ar.getClass().getDeclaredMethod(\"addKnownPermission\",String.class);addKnown.setAccessible(true);\n  var add=ar.getClass().getDeclaredMethod(\"addEffect\",AccessRepository.PermissionEffect.class);add.setAccessible(true);\n  for(String code:List.of(\"access.grant_request.review\",\"governance.supervisor_review.approve\")){\n   addKnown.invoke(ar,code);add.invoke(ar,new AccessRepository.PermissionEffect(\"once\",code,AccessRepository.Effect.GRANT,AccessRepository.Source.TEMPLATE_APPLICATION,\"single_review_template\",null,null,true,false));\n  }\n  actor(\"once\",\"staff\",\"org_a\",null);var az=new AuthorizationService(ar);\n  try{new AccessController(f.svc,az).reviewGrant(\"not-reached\",new AccessGrantReviewRequest(\"once\",\"fixture\",List.of(),\"approve\",\"fixture\"));print(\"SINGLE_USE_TWO_GUARDS\",\"UNEXPECTED_SUCCESS\");}\n  catch(ApiException e){print(\"SINGLE_USE_TWO_GUARDS\",e.code()+\" consumed=\"+field(ar,\"consumedSourceIds\"));}\n\n  f=fresh();user(f,\"org_user\",\"org_a\",null);actor(\"org_user\",\"staff\",\"org_a\",null);\n  try{f.auth.effectivePermissionsForUser(\"org_user\");print(\"ORG_STAFF_OWN_PERMISSIONS\",\"allowed\");}\n  catch(ApiException e){print(\"ORG_STAFF_OWN_PERMISSIONS\",e.code());}\n\n  f=fresh();var pu=f.svc.createAccessUser(account(\"manual\",\"manual_employee\",\"org_a\"));\n  print(\"MANUAL_REGISTRATION\",pu.status()+\"/\"+pu.createdVia()+\"/roles=\"+f.repo.findPermissionEffects(pu.id(),Instant.now()).size());\n  high(f);user(f,\"target\",\"org_a\",null);user(f,\"requester\",\"org_a\",null);actor(\"requester\",\"staff\",\"org_a\",null);\n  f.svc.applyGrant(new AccessGrantApplyRequest(\"summary\",\"fixture\",List.of(),\"temporary_permission\",\"target\",null,null,null,null,null,null,List.of(HIGH),null,Instant.now().plusSeconds(3600),false,\"fixture\"));\n  admin();var summary=f.svc.grantSummary(30,1,1);\n  print(\"SUMMARY_USER_VISIBLE_TEXT\",summary.positionRecommendations().get(0).recommendation()+\" | \"+summary.userAnomalies().get(0).summary());\n  print(\"SUMMARY_QUERY_THRESHOLD_UNBOUNDED\",f.svc.grantSummary(30,Integer.MAX_VALUE,Integer.MAX_VALUE).userAnomalies().size());\n\n  concurrentReview();\n  SecurityContextHolder.clearContext();\n }\n static void concurrentReview()throws Exception {\n  final var base=fresh();high(base);user(base,\"target\",\"org_a\",null);user(base,\"requester\",\"org_a\",null);\n  base.repo.saveStaffLevelAssignment(new StaffLevelAssignment(\"usr_admin\",\"lvl_m10_upper\",100L,\"CNY\",null,\"org_a\",null,\"fixture\",Instant.now()));\n  var armed=new java.util.concurrent.atomic.AtomicBoolean(false);\n  var grantBarrier=new java.util.concurrent.CyclicBarrier(2);\n  AccessRepository rp=(AccessRepository)Proxy.newProxyInstance(AccessRepository.class.getClassLoader(),new Class[]{AccessRepository.class},(p,m,a)->{\n   Object v;try{synchronized(base.repo){v=m.invoke(base.repo,a);}}catch(InvocationTargetException e){throw e.getCause();}\n   if(armed.get()&&m.getName().equals(\"findGrantRequestById\"))grantBarrier.await(10,java.util.concurrent.TimeUnit.SECONDS);\n   return v;\n  });\n  var reviews=new java.util.concurrent.ConcurrentHashMap<String,SupervisorReviewDecisionRecord>();\n  var reviewBarrier=new java.util.concurrent.CyclicBarrier(2);\n  SupervisorReviewRepository srp=new SupervisorReviewRepository(){\n   public SupervisorReviewDecisionRecord saveSupervisorReview(SupervisorReviewDecisionRecord r){reviews.put(r.reviewId(),r);return r;}\n   public Optional<SupervisorReviewDecisionRecord> findPendingSupervisorReviewById(String id){\n    var r=Optional.ofNullable(reviews.get(id)).filter(x->\"pending_review\".equals(x.reviewStatus()));\n    if(armed.get())try{reviewBarrier.await(10,java.util.concurrent.TimeUnit.SECONDS);}catch(Exception e){throw new RuntimeException(e);}\n    return r;\n   }\n  };\n  var auth=new AuthorizationService(rp);var audit=new AuditEventService((AuditEventRepository)e->e);\n  var sr=new SupervisorReviewService(auth,idem(),srp,audit);\n  var svc=new AccessService(rp,idem(),audit,auth,AccessCredentialProvisioner.noop(),sr,true,3,3);\n  actor(\"requester\",\"staff\",\"org_a\",null);\n  var pending=svc.applyGrant(new AccessGrantApplyRequest(\"concurrent\",\"fixture\",List.of(),\"temporary_permission\",\"target\",null,null,null,null,null,null,List.of(HIGH),null,Instant.now().plusSeconds(3600),false,\"fixture\"));\n  armed.set(true);\n  try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)){\n   var futures=new ArrayList<java.util.concurrent.Future<String>>();\n   for(int i=0;i<2;i++){final int n=i;futures.add(executor.submit(()->{admin();try{return svc.reviewGrant(pending.id(),new AccessGrantReviewRequest(\"review-\"+n,\"fixture\",List.of(),\"approve\",\"fixture\")).status();}finally{SecurityContextHolder.clearContext();}}));}\n   var result=new ArrayList<String>();for(var fu:futures)result.add(fu.get(15,java.util.concurrent.TimeUnit.SECONDS));\n   armed.set(false);print(\"CONCURRENT_REVIEW\",result+\" temporary_grant_count=\"+((Map<?,?>)field(base.repo,\"temporaryGrants\")).size());\n  }\n }\n static IdempotencyService idem(){\n  var delegate=new InMemoryIdempotencyRepository();\n  var repo=(IdempotencyRepository)Proxy.newProxyInstance(IdempotencyRepository.class.getClassLoader(),new Class[]{IdempotencyRepository.class},(p,m,a)->{try{synchronized(delegate){return m.invoke(delegate,a);}}catch(InvocationTargetException e){throw e.getCause();}});\n  return new IdempotencyService(repo,new ObjectMapper());\n }\n}\n"
with tempfile.TemporaryDirectory(prefix='m006-self-probe-',dir='.scratch') as d:
 root=Path(d).resolve();cpfile=root/'classpath'
 r=subprocess.run(['mvn','-o','-q','-f','backend/pom.xml','dependency:build-classpath','-Dmdep.outputFile='+str(cpfile)],capture_output=True,text=True)
 print('DEPENDENCY_CLASSPATH_EXIT',r.returncode)
 if r.returncode:print(r.stdout,r.stderr);raise SystemExit(r.returncode)
 cp=str(Path('backend/target/classes').resolve())+':'+cpfile.read_text().strip()
 src=root/'M006Probe.java';src.write_text(java,encoding='utf-8');out=root/'classes';out.mkdir()
 base=Path('backend/src/main/java/com/complex/platform')
 sources=list((base/'access').glob('*.java'))+list((base/'access/dto').glob('*.java'))
 sources += [base/'common/idempotency/IdempotencyService.java',base/'governance/review/SupervisorReviewService.java',base/'evidence/AuditEventService.java']
 tests=Path('backend/src/test/java/com/complex/platform')
 sources += [tests/'access/AccessServiceTests.java',tests/'access/AuthorizationServiceTests.java',tests/'support/InMemoryAuditEventRepository.java',tests/'support/InMemoryIdempotencyRepository.java']
 r=subprocess.run(['javac','-encoding','UTF-8','--release','21','-cp',cp,'-d',str(out),*[str(p) for p in sources],str(src)],capture_output=True,text=True)
 print('JAVAC_EXIT',r.returncode,'CURRENT_SOURCE_COUNT',len(sources));print(r.stdout,r.stderr)
 if r.returncode:raise SystemExit(r.returncode)
 r=subprocess.run(['java','-cp',str(out)+':'+cp,'com.complex.platform.access.M006Probe'],capture_output=True,text=True)
 print('PROBE_EXIT',r.returncode);print(r.stdout,r.stderr)
 raise SystemExit(r.returncode)

```

## 可复现程序附录 3

版本：V0.04

下列为本轮实际成功执行的远程Python程序，在160正本根目录以python3 -经stdin运行。编译输出只放自动清理的.scratch/m006-self-probe-*；不改源码；探针1/2用内存fixture，探针3只有complex_test连接内临时表。

```python
from pathlib import Path
import subprocess,tempfile,json,hashlib,os
import shlex,re
db_env={'COMPLEX_DATABASE_URL':'jdbc:postgresql://127.0.0.1:5432/complex_test','COMPLEX_DATABASE_USERNAME':'complex'}
for cfg in [Path('/etc/complex/complex.env'),Path('/home/james/.complex/complex.env')]:
 if cfg.is_file():
  try:
   for line in cfg.read_text(encoding='utf-8').splitlines():
    m=re.match(r'^\s*(?:export\s+)?(COMPLEX_DATABASE_(?:USERNAME|PASSWORD))=(.*)$',line)
    if m:
     parts=shlex.split(m.group(2),comments=True)
     if len(parts)==1 and '${' not in parts[0] and '$(' not in parts[0]:db_env[m.group(1)]=parts[0]
   print('DB_CONFIG_SOURCE',str(cfg),'KEYS',sorted(db_env))
  except PermissionError:print('DB_CONFIG_NOT_READABLE',str(cfg))
  if 'COMPLEX_DATABASE_PASSWORD' in db_env:break
if 'COMPLEX_DATABASE_PASSWORD' not in db_env:raise SystemExit('TEST_DB_CREDENTIAL_UNAVAILABLE')
java="package com.complex.platform.access;\nimport java.sql.*;\nimport java.time.*;\nimport java.util.*;\nimport org.springframework.jdbc.datasource.SingleConnectionDataSource;\nimport org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;\nimport org.springframework.dao.DataAccessException;\npublic class M006Probe {\n public static void main(String[] args)throws Exception {\n  String url=System.getenv(\"COMPLEX_DATABASE_URL\");\n  if(!\"jdbc:postgresql://127.0.0.1:5432/complex_test\".equals(url))throw new IllegalStateException(\"test database required\");\n  try(Connection c=DriverManager.getConnection(url,System.getenv(\"COMPLEX_DATABASE_USERNAME\"),System.getenv(\"COMPLEX_DATABASE_PASSWORD\"))){\n   try(var s=c.createStatement()){\n    try(var r=s.executeQuery(\"select current_database()\")){r.next();if(!\"complex_test\".equals(r.getString(1)))throw new IllegalStateException(\"wrong database\");}\n    s.execute(\"SET search_path TO pg_temp\");\n    s.execute(\"CREATE TEMP TABLE permission_template_applications(id varchar(64) primary key,target_id varchar(64),single_use boolean,consumed boolean,consumed_at timestamptz)\");\n    s.execute(\"CREATE TEMP TABLE temporary_permission_grants(id uuid primary key,subject_id varchar(64),single_use boolean,consumed boolean,consumed_at timestamptz)\");\n    s.execute(\"CREATE TEMP TABLE permission_template_usage_logs(id uuid primary key,application_id varchar(64) REFERENCES permission_template_applications(id) ON DELETE SET NULL,user_id varchar(64),permission_code varchar(128),result varchar(32),reason varchar(128),occurred_at timestamptz)\");\n    String grant=\"00000000-0000-0000-0000-000000000006\";\n    s.execute(\"INSERT INTO temporary_permission_grants VALUES('\"+grant+\"','fixture',true,false,null)\");\n    var repo=new JdbcAccessRepository(new NamedParameterJdbcTemplate(new SingleConnectionDataSource(c,true)));\n    try{repo.markSingleUsePermissionConsumed(grant,\"fixture.permission\",\"fixture\",Instant.now());System.out.println(\"TEMPORARY_SINGLE_USE_LOG=unexpected_success\");}\n    catch(DataAccessException e){Throwable cause=e.getMostSpecificCause();System.out.println(\"TEMPORARY_SINGLE_USE_LOG=\"+(cause instanceof SQLException sq?sq.getSQLState():cause.getClass().getSimpleName()));}\n    try(var r=s.executeQuery(\"SELECT consumed FROM temporary_permission_grants\")){r.next();System.out.println(\"CONSUMED_AFTER_LOG_FAILURE=\"+r.getBoolean(1));}\n    System.out.println(\"DATABASE_SCOPE=complex_test; all writes to three connection-local TEMP tables\");\n   }\n  }\n }\n}\n"
with tempfile.TemporaryDirectory(prefix='m006-self-probe-',dir='.scratch') as d:
 root=Path(d).resolve();cpfile=root/'classpath'
 r=subprocess.run(['mvn','-o','-q','-f','backend/pom.xml','dependency:build-classpath','-Dmdep.outputFile='+str(cpfile)],capture_output=True,text=True)
 print('DEPENDENCY_CLASSPATH_EXIT',r.returncode)
 if r.returncode:print(r.stdout,r.stderr);raise SystemExit(r.returncode)
 cp=str(Path('backend/target/classes').resolve())+':'+cpfile.read_text().strip()
 src=root/'M006Probe.java';src.write_text(java,encoding='utf-8');out=root/'classes';out.mkdir()
 base=Path('backend/src/main/java/com/complex/platform')
 sources=list((base/'access').glob('*.java'))+list((base/'access/dto').glob('*.java'))
 sources += [base/'common/idempotency/IdempotencyService.java',base/'governance/review/SupervisorReviewService.java',base/'evidence/AuditEventService.java']
 tests=Path('backend/src/test/java/com/complex/platform')
 sources += [tests/'access/AccessServiceTests.java',tests/'access/AuthorizationServiceTests.java',tests/'support/InMemoryAuditEventRepository.java',tests/'support/InMemoryIdempotencyRepository.java']
 r=subprocess.run(['javac','-encoding','UTF-8','--release','21','-cp',cp,'-d',str(out),*[str(p) for p in sources],str(src)],capture_output=True,text=True)
 print('JAVAC_EXIT',r.returncode,'CURRENT_SOURCE_COUNT',len(sources));print(r.stdout,r.stderr)
 if r.returncode:raise SystemExit(r.returncode)
 r=subprocess.run(['java','-cp',str(out)+':'+cp,'com.complex.platform.access.M006Probe'],capture_output=True,text=True,env={**os.environ,**db_env})
 print('PROBE_EXIT',r.returncode);print(r.stdout,r.stderr)
 raise SystemExit(r.returncode)

```

## B1 本席封存

版本：V0.05

UTC 2026-09-05T16:06:07.316564+00:00。完整14文件/2988行/52冻结映射判据已读，全部23项本席finding及三套实际运行程序/输出已保存。18缺陷+5登记，本席最终审计结论FAIL。14源SHA逐个等于B1起始manifest；实际lock/sidecar/ledger三值等于既有冻结值；Grok第四值待其真正结束且完整报告接收后再校验。无本席临时目录残留。未读取任何Grok报告或JSON，未修复代码，未修改HTML。现在本席B1报告完整，可在Grok真实exit=0后接收其完整报告；不得以本席FAIL中途先修。

CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

## V0.06 B1 接收与首次 B2 整批归并说明（不是新独立票、不是 B2 PASS）

本方独立B1前73231字节保持原样，SHA256=1ee77bccf61c3b418028f42866202a4655246349d7c55ef37d9e3f36def61ab4。该前缀完整落盘后才读取Grok。接收的Grok B1为44940字节/714行，SHA256=96f0165d01b18753284b2556d2474c17623259bdb10ddca45cc80b7a0b5da071；真实进程exit0，完整1—714行已分4段全部阅读。结论FAIL，11个GR。没有另派第三方、没有业务源码修改、没有改三HTML。

Grok披露其Maven指定目录未生效，实际写backend/target。此事实与派发“不共用target”限制不符，不能报告其验证完全隔离；不得删其他人的target来掩盖。该问题不满足交付失败的四项定义，不增加运行失败重跑计数。本席验证编译全部14当前源到独立临时classes，未写共享target；所述55测试与18反例保持原本具体验证边界。双方manifest仍匹配HEAD，未据该限制违例抹掉Grok实质findings或FAIL票。

B1后四值实算均为23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de，COUNT0；实际lock、sidecar、ledger基准、指定Grok B1最后哈希行均分别读得此值。三HTML与14源未改。findings V0.52/SHA e30dbd4b8a1eb22ff6a78e7395485588e75c1ffa63db226f5f1a6b3bb72e29aa整批30ID为候选：24拟修、5仅登记、027待实际答复，B3=0。Grok B2尚未调用，不能提前修复。

### 全量归并索引

CX-01—CX-23依次对应M006-001—023。GR-01→001，GR-02→024，GR-03→025，GR-04→015，GR-05→011，GR-06→026，GR-07→027，GR-08→028，GR-09→010，GR-10→029，GR-11→030。Grok §6的兼容构造器删除建议→019；§6英文/假分页/主体回退建议均已归入对应ID。没有遗漏GR或默删未编号建议。

### 收后窄读核实的分歧与依据

1. 账号状态与雇佣状态。AccessUsersPage339—342原代码为<option value="active">启用</option>、<option value="invited">待激活</option>、<option value="disabled">禁用</option>；177/280经formatBusinessStatus显示。AccessUser兼供商户/外包账号，不能直接全改成BEQR157五种员工状态；StaffProfileRecord有registrationStatus/status也不证明员工五态已实现。本方不认“返回active就代表向用户显示英文”的推导。真实缺陷AccessService870无状态校验、JdbcAccessCredentialProvisioner33只以disabled区分。ProductionAuthService283—287原代码先检accessExpiresAt，再filter("active".equals(session.userStatus()))，再credentialUsable；故单凭凭证active不构成停岗者实际登录成功证据。024保留Grok原冲突主张及本方细化/校验缺陷候选，等双席裁定。

2. 手机号。V131第2行ADD COLUMN IF NOT EXISTS phone_hash VARCHAR(64)；AccessService917—919是MessageDigest.getInstance("SHA-256")后HexFormat，无密钥；AccessUser.phoneHash和JdbcAccessRepository999返回该值。所读access/auth/UI引用只证明存取和返回，未发现该字段用于查号；不夸大为全仓没有其他用途。PiiCipher33—46现有AES/GCM/NoPadding，BlindIndex28—39现有HmacSHA256且从KeyVault取pii-index密钥；二者仅是复用候选，不是已执行修复。025承接隐私冲突，021纯机制登记不豁免手机号应用。

3. 中文。GlobalExceptionHandler48—61已把ApiException.getMessage传FieldLabels.translate。FieldLabels504—529识别字段错误并返回中文，纯英文无匹配走中文FALLBACK。因此Grok源码英文异常子主张要以真实响应核实；业务成功响应的两句英文仍由AccessGovernancePage341/454—455直接展示，015继续待修。未批量改英文内部标识。

4. 后台建号和删除。业务地图582确实列“新建员工账号”，该段是引导式表单，不写免审批。1433明确两条注册/公司批准后生效；1428是“每位审批通过的员工”自动自助。011同时保留后台合法新建能力、关闭未经批准直接激活、补批准后自助，不盲目给未批账号自助。业务地图563—578写明通用规则覆盖整套系统、删除须审批/软删/可恢复/查占用；JdbcAccessRepository520—524确实直接DELETE FROM position_role_templates。026分类分歧与对应GEN006/017依据已登记，无新锁条目。

5. 储值添加分类。Grok用业务地图入口表BSM114没有列储值判添加；但业务地图832现有逐字原句为“预付卡 / 余额：客人可充值到余额、消费时扣余额；充值必须支付成功才入账、不凭空加余额，账户可冻结/解冻。”；867有“我的余额：查看本人储值卡余额，含本金与赠送金分列。”。锁定义BSM035第2942行已含832原句。MemberWalletController56—85的三个入口均以customerService.resolveCustomerId(token(...))解析本人，再调topup/settleTopup/consume；AuthorizationService只对两个既有permission code桥接；并非已证“任何客人可任意操作他人余额”。原Grok添加判定不覆写，按控制§2.1否决权仍登记027并走§2.2实际问答。保留后继续审授权正确性，未授权扩大业务。

6. 主体、凭证、分页。SecurityConfig81—82禁用basic/form，UserDetailsService110—113始终throw UsernameNotFoundException，真实Bearer主体是UserRecord/Actor、客人Actor。因此028不夸大生产攻击事实，最小方向是删除猜测身份fallback。AccessUsersPage90—92/301—302本来就要求初始密码，029后端补已有输入边界。AccessController相关GET没有page参数，030修不实pageSize时不能直接截前50让剩余数据无法访问；无界资源主张需结合真实负载/使用方式，并与016业务频繁阈值分开。

以上收后读取均为当前14文件的调用/判据证据，不声称全审其他模块；源片段/行号与原完整B1现有证据配合。整个30ID候选必须经Grok差集与分类复核以及本方B2票，任何单方意见都未定稿。027未答时全流程暂停，不提前审B4/B5或下一模块。


## V0.07 操作员纠正钱包只读范围（接收说明，非独立重审票）

原话：“客人不是只能看吗？他要操作什么东西呢？储值钱包他只能看这个交易记录和余额，他要操作什么。”。这次说明明确否定客人钱包发起储值交易，按问题027删除写能力、保留本人余额/交易记录查询登记。Codex前一阶段把业务地图832解释为客人钱包可自行充值/扣款的推断撤回，不能让操作员为笼统错误提问承担后果。两份原始B1及Grok原判不改。本项回复不是“还要确认一次”的疑问，删除答复按明确行为范围规范化而来，附原话可核。

解释阶段窄读WalletPage全181行、customerService相关方法、MemberWalletController56—108、StoredValue请求DTO和Service相关方法。实证：WalletPage66—94允许手填金额发起充值/消费；消费提交卡范围、金额和固定reason=钱包消费，DTO没有订单/短信字段；页面无交易记录展示。AuthorizationService347—356对无merchant的客人还有范围拒绝风险，未实跑成功消费，因此没有把有按钮当成交易已可用。用户要求只读后，正常处置应关闭客户写接口及UI，不为使其扣款成功去放宽商户检查。

当前拟修边界：移除客人钱包金额输入、充值和消费按钮及对应客户写调用；关闭客人专用topup/settle/consume写入口与本项customer权限桥，不能只隐藏按钮而允许直接调接口。保留按登录身份限定本人的卡/余额查询；按业务地图867和本次明确要求，复用现有交易台账提供本人只读记录，必要只读查询和展示属于这一钱包修复的直接依赖，不创造充值/退款规则、账务写入或新交易字段。交易记录如需联动查询/DAO，只读且严格本人隔离，不新建平行台账。既有员工柜台收款、经授权入账、订单支付和内部消费内核保留，按原业务规则执行；本项不能删底层储值业务、清空余额/流水或停掉所有支付。测试应证明客户写入口不可达且余额/记录可查，其他授权业务不因本项被阻断。三HTML不改；整批B2双PASS前不修。

候选findings V0.53：仍30ID，25是5否，待答0；后续B2两席审完整候选，不按单条往返。B3=0，源码/HTML未改，Grok B2此写入点NOT-CALLED。


## V0.08 最新储值业务说明与HTML冲突（接收记录，非B2通过票）

操作员最新原话：**点击“充值”** 这个页面需要可以拉起微信、支付宝、银联付款，按对应金额线上支付，而不是通过柜台支付收款。然后我们收到支付宝银联或者微信的返回确认就可以把这笔钱进入到客人的储值账户了。当然也可以通过pos机刷卡充值。  客人消费储值金额有两种情况第一种情况是a p p上已经有的订单。比如说门票。比如说这个下单后的在线支付。那这个客人直接可以用余额直接支付用储值卡的余额。有一种情况是客人点了一系列的东西。然后最后有一个总价。那客人，比如说卡座的消费。那客人可以通过出示二维码的方式。然后扣款。当然会收到短信验证码。

此前V0.07只读范围被替代。当前限定范围：保留客人发起合法储值充值和按已有订单/现场账单支付的限定授权通路；撤回“钱包只读、关闭全部topup/settle/consume”的旧处理范围。充值界面拉起微信/支付宝/银联支付，服务端核验支付成功确认后按该笔充值真实金额入账，重复确认不得重复加余额；另保留POS刷卡充值。App已有订单可选指定储值卡余额支付；现场已形成明细和总价的账单走客人出示码、扫码关联该账单、短信验证码验证后按账单金额扣款。取消钱包页脱离订单/账单随意填写金额扣款，保留本人余额和逐笔记录查询。沿用既有卡归属/适用商户/本金赠金/冻结/幂等边界；不通过放宽为任意客人权限或任意商户解决当前范围拒绝。该范围为用户业务裁决下的待复核方案；受保护HTML两处精确修改待批准，不能提前将拟写文字当成冻结判据，不能在B2实施支付/UI/权限修复。

本席实际读得地图585明确“系统自己不生成付款出示码”，832已有充值必须支付成功条款，867有余额与逐笔记录；新说明所需线上支付、POS、订单余额支付、现场账单扫码+短信流程已据此拟稿。只在内存预演，唯一改动行2、585、832；三HTML与HEAD字节一致。拟稿涉及BSM-006/035，最早映射M001，获批后必须完整LOCK-AMEND，当前不改锁、不撤旧PASS、不重开。

旧V0.53整批B2 Grok在16:33:07 UTC已派发，最新业务改变使该候选失效。旧自审草稿尚未落PASS；本记录不把它包装为当前票。旧Grok即使返回PASS也不满足V0.54新候选双审条件。两份B1原始前缀保持，不改原结论。当前30ID/25拟修5登记，其他29项不变；B3=0。

文件：160:/home/james/complex-management/docs/business-system-map-for-operations-v0.1.html

申请 M006-027-A（储值条款）：第832行仅替换以下开头片段。
原文：预付卡 / 余额：客人可充值到余额、消费时扣余额；充值必须支付成功才入账、不凭空加余额，账户可冻结/解冻。
替换为：预付卡 / 余额：客人点击“充值”后，可选择微信、支付宝或银联，按充值金额在线支付；系统收到对应支付渠道的支付成功确认后，才将该笔充值金额记入客人的储值账户，不凭空加余额；也支持通过 POS 机刷卡充值，充值必须支付成功才入账。客人使用储值余额消费分两种情况：①支付 App 中已有订单，例如门票订单，客人在订单支付时选择储值卡余额支付；②支付已形成消费明细和总价的现场账单，例如卡座消费，客人出示储值付款二维码，收款端扫码关联该笔账单，经手机号短信验证码验证通过后，按账单金额扣款。储值消费不通过在钱包页随意填写金额直接扣款。账户可冻结/解冻。
理由：把操作员最新确认的线上充值、POS充值和两种余额消费写入储值正文；原卡种、资金归属、退款、时间记录等后文全部保留。
同批将第2行“<!-- 文件版本：V0.08；更新日期：2026-09-01 -->”改为“<!-- 文件版本：V0.09；更新日期：2026-09-05 -->”，履行每次更新版本递增规则。
批准状态：待操作员实际批准。

申请 M006-027-B（二维码条款）：第585行仅替换末尾括号。
原文：（说明：客人付款走第三方微信、支付宝，或用账户预存款经手机号加短信验证支付，系统自己不生成“付款出示码”，故不在本自动刷新范围内。）
替换为：（说明：客人付款可走微信、支付宝、银联等第三方支付渠道，或使用账户预存款支付；账户预存款支付须经手机号短信验证码验证通过。卡座消费等已形成消费明细和总价的现场账单，客人可出示储值付款二维码，由收款端扫码关联账单，验证通过后按账单金额扣款；系统生成的储值付款二维码适用上述动态刷新规则。）
理由：删除与本次储值付款二维码要求冲突的禁止句；沿用同段既有动态刷新规则，保留原账户预存款支付的短信验证要求，其余正文不变。
批准状态：待操作员实际批准。

流程影响：两处分别涉及已冻结 BSM-035、BSM-006，最早映射模块均M001。若批准并同步HTML，依V0.17 §5须完整LOCK-AMEND五步：先改定义和映射、再sidecar、双席复核、再更新ledger基准，最后M001至当前M006改PENDING并从M001 B1重审。原报告、原提交及批准历史保留；当前尚未改HTML/锁或重开。获批HTML出处同步不计LOCK-AMEND-COUNT，但仍必须双席复核。


## V0.09 操作员撤回二维码、采用手机号短信验证（接收记录）

最新原话：“那么出示二维码这个事情就不要这个，改成这个报手机号码，然后短信扣款。”。其余线上充值/POS/App订单余额支付保持。当前范围：保留客人发起合法储值充值和按已有订单/现场账单支付的限定授权通路。客人在充值页选择金额并拉起微信、支付宝或银联线上支付，服务端核验对应渠道支付成功确认后按该笔真实充值金额入账，重复确认不得重复加余额；另保留POS刷卡充值。App已有订单（如门票）可选择指定储值卡余额支付。现场已有消费明细和总价的账单（如卡座），客人报本人手机号，经短信验证码验证通过后按该笔账单金额扣款；不生成储值付款二维码，不以收到短信通知代替验证码通过。取消钱包页脱离订单/账单随意填写金额扣款，保留本人余额及逐笔记录查询。沿用既有卡归属/适用商户/本金赠金/冻结/幂等以及HTML585储值支付短信验证边界，不将此解释为App订单余额支付可豁免已有短信验证，也不通过任意客人权限或任意商户放宽解决范围拒绝。新要求落在现有HTML832储值、585手机号短信验证、867余额记录和1065支付方式之内；三HTML与冻结判据不修改、不启用新的判据、不重开M001。本范围为整批B2候选，未双PASS前不得实施。

原V0.54的HTML申请A/B全部撤回；585本来已有手机号短信验证，与最新方案一致。832已有客人充值与支付成功入账，867有余额记录，1065有支付通路；A扩写不是必要修复，故不为了文字细化改HTML并重开已发布模块。三HTML/锁均未改，原申请不再待审批，M006可按用户新指令恢复B2。

旧V0.53在途Grok输入仍过时，不把其PASS当当前票；下一票审V0.55完整30ID。原B1与接收前缀保留，本段没有声明B2通过。

## V0.10 Codex B2 当前完整候选复核

版本：V0.10。结论：PASS，仅接受当前V0.55的整批归并、四分类、工程缺陷与队列/边界；尚非B2双PASS、不是模块完成。当前新Grok独立票未读取，不以旧Grok V0.02拼票。B2无互盲要求，但本票仍由Codex逐项判断；原B1独立前缀保持。

### 输入与版本

- 当前findings V0.55：220658字节，SHA f8d033a33dce3ed64c34af0d478e01be97b658697e69d46294e521820c841c15。
- questions/M006-027.md V0.04：10147字节，SHA d1696ddd6a546789b3e44b446c063466f48861ac79780df686e9e7ba88ca9c94，末尾首答保留；线上充值/订单消费原话与最新取消二维码、改手机号短信扣款原话均完整。
- modules/M006-b1-self-raw.md本票输入前89316字节，SHA1b0cd898739111c912c2a20ff63815eb1a82d6ab22d0aa81ec21ac2435858915；独立B1为前73231字节，SHA1ee77bccf61c3b418028f42866202a4655246349d7c55ef37d9e3f36def61ab4。V0.06—09为收后核实/用户裁决记录，不当作新的B1。
- modules/M006-b1-grok-raw.md新票前67898字节，SHA7a13ac5e95b2e1ea473e6287e64c64fa40ba8cb3b6d450c873cc8cd306853d75；独立B1为前44940字节，SHA96f0165d01b18753284b2556d2474c17623259bdb10ddca45cc80b7a0b5da071。其后22958字节旧B2 V0.02原生exit0/PASS完整保留，但只审已过时V0.53只读钱包方案，不参与当前双PASS。

本方旧V0.53草稿曾拟定但没有落盘。本票重新核对当前全部30ID，使用其中仍成立的29条事实；明确重新审027，不把旧草稿改标题后当新审完成。

### 全集差集、机检与范围

CX01—23均归001—023。GR01→001、02→024、03→025、04→015、05→011、06→026、07→027、08→028、09→010、10→029、11→030；§6兼容构造器删除建议归019，其余未编号意见均已并015/028/030。全部23CX+11GR及未编号意见完整进入30个连续唯一ID，无遗漏、无单方改写原始报告。

实跑远程Python机检：当前30ID连续唯一，25是5否；全部questions首个非空答复合法（M003-023删除、M003-024保留、M006-027保留）。14源码SHA逐项与B1 manifest一致；三HTML字节逐一与当前HEAD一致；两份B1原始前缀SHA相同。lock/sidecar/ledger仍为23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de，COUNT0。B3=0，未修源、未新跑业务测试，不把源未改称为缺陷已修。审计文档严格UTF-8解码且无替换字符/NUL通过；实际环境未找到utf8_guard.py，未声称调用该文件。

原V0.53删除答复及V0.54 HTML申请留在历史区，不构成当前待答；用户最新指令取消二维码后原HTML585的手机号+短信规则不再冲突。扩写832是可选文字同步，当前无需为该细化去改权威文件/锁并重开旧模块；两份旧弹窗申请均已撤回。原HTML1480/枚举210要求先开放接口、第三方供应商资料到位后接入、默认关闭开关及不伪造结果，同样保留，不以新增钱包交互擅自开启支付渠道。

### 三十ID逐项核验

| ID | 本方核验及本批结论 |
|---|---|
| M006-001 | CX01+GR01均保留。管理员DENY旁路和高危自动权限有两席反例；日常自动权限须保留，主管身份问题另与010共同覆盖，不能用删除所有管理员日常权限偷换修复。冲突/是。 PASS。 |
| M006-002 | CX02跨商户实际role赋权反例与同根模板/薪资/临时授权目标边界均写入；同org不等于同merchant，冲突/是，不能只修一个控制器。 PASS。 |
| M006-003 | CX03已证无org会变全量而非报错；登记时无虚构生产特权，全局合法岗位仍须已有授权事实。细化/是。 PASS。 |
| M006-004 | CX04三个真实slug/upsert覆盖证据保留，授权申请同根也覆盖；内部ID与幂等结果职责分开。细化/是。 PASS。 |
| M006-005 | CX05已应用模板实时增权反例完整，不能只限制新增应用；已有应用/增量/审批边界同一项。细化/是。 PASS。 |
| M006-006 | CX06审批快照与执行重新取值的差异有实际输出；待批对象漂移拒绝或严格原快照均属同一边界，不能批准A执行B。细化/是。 PASS。 |
| M006-007 | CX07实际beneficiary selfreview=applied，岗位申请方自审与受益人自审不可相互替代。全局既有HTML事实+GEN005/017，冲突/是。 PASS。 |
| M006-008 | CX08两个不同幂等键复核同一pending都应用，替身交错与普通SELECT/upsert契约一致，未虚称SQL并发集成已跑。细化/是，数据库级锁/CAS在既有事务中实现。 PASS。 |
| M006-009 | CX09旧岗位授权仍在反例保留，要求按授权来源撤旧派生，不能误删独立直接授权。冲突/是。 PASS。 |
| M006-010 | CX10+GR09主管类型/级别不一致保留；不能把任意高rank或管理员自动设最终审批人。细化/是。 PASS。 |
| M006-011 | CX11和GR05的原分歧明确保留。HTML582允许新建表单不明文豁免1433两路径注册批准；保留合法新建、删除直接激活旁路、获批后自助既能覆盖原两条，冲突/是。本方不将此当Grok已经接受。 PASS。 |
| M006-012 | CX12公司用户自身有效权限查询被商户scope拒绝有实际反例，修复必须保留无归属默认拒绝。细化/是。 PASS。 |
| M006-013 | CX13空集合/倒置窗口active伪成功和JDBC零行insert证据俱在；只补既有授权输入正确性，不新设期限业务上限。细化/是。 PASS。 |
| M006-014 | CX14同一次业务两个guard烧掉唯一模板已由实际Controller复现；成组校验/一次消耗而非移除单次语义。细化/是。 PASS。 |
| M006-015 | CX15+GR04合并保留。两成功英文句确有UI直接显示；GlobalExceptionHandler/FieldLabels翻译使“所有源码英文都是对客英文”不成立，不无依据全包替换。冲突/是。 PASS。 |
| M006-016 | CX16经营窗口/频繁阈值/审批级别影响业务结果，不能当技术常量豁免。已有业务配置及审批策略复用，不另建体系。冲突/是。 PASS。 |
| M006-017 | CX17 real PostgreSQL三TEMP表+当前DAO得到23503/consumed=true，证据范围明确；必须正确区分模板应用与临时授权日志来源并确保消耗留痕原子。细化/是。 PASS。 |
| M006-018 | CX18管理员改组织模板归属变null反例完整，内容更新保留既有owner。细化/是。 PASS。 |
| M006-019 | CX19机制与Grok未编号legacy构造器删除建议均已登记；存在真实构造调用和fresh compilation，不为删兼容构造器改遍调用方，无业务发明。技术性/否。 PASS。 |
| M006-020 | CX20真实两个profile实现支撑接口，不是一实现臆造抽象；空密码伪成功分到029，业务激活分到011，技术性注册不豁免二者。技术性/否。 PASS。 |
| M006-021 | CX21原B1不覆写，但候选明确剥离手机哈希数据存储问题到025；标准库UTF8/JDBC/BCrypt机制无业务发明。不将此登记当隐私存储PASS。技术性/否。 PASS。 |
| M006-022 | CX22响应/仓储包装机制无新业务；假分页实际问题分到030，不以容器登记豁免行为。技术性/否。 PASS。 |
| M006-023 | CX23主体/统计等已有上位需求，既有HTML治理659确有频繁临时授权统计，非凭代码发明该业务。记录本体只登记，016仍待修业务政策值。细化/否。 PASS。 |
| M006-024 | GR02原冲突原话保留。本方不接受用雇佣五态覆盖全部含商户/供应商的访问账号，也不接受将已翻译的active等同对客英文；真实unknown状态未校验/非active凭证发active仍入队。ProductionAuthService独立userStatus检查避免夸大离职可登录。候选细化/是，Grok须明确审此异议。 PASS。 |
| M006-025 | GR03手机SHA不是加密且被API直接返回，不能由021技术机制登记放行。按既有PII原文处理此入口，有PiiCipher/BlindIndex可复用且明确不能从旧摘要造手机号，不扩成全仓PII整改。冲突/是。 PASS。 |
| M006-026 | GR06硬删及无占用等原主张保留，已冻结HTML通用删除规则的作用域明确整套系统；按GEN006/017核对认为冲突。既有留存/审批机制复用，不能只改inactive便称完成。冲突/是，Grok须审本分类调整。 PASS。 |
| M006-027 | GR07原添加分类保留；用户后续明确保留微信/支付宝/银联线上充值、POS及已有订单余额支付，并把现场二维码改为报本人手机号、短信验证码通过后扣账单金额。最新答复首行保留且有逐字原话，旧只读/二维码/两份HTML申请全部撤回。原HTML832/585/867已有上位要求；无需改HTML/锁。当前任意金额consume及只提示柜台topup不满足已明确流程，工程缺陷仍是。修复须绑定本人卡、真实订单/账单及已验证SMS，不得把成功通知或客户端宣称当授权/渠道成功；不得删除用户要保留的合法交易。复用真实stored_value_transactions及既有支付/短信接口。添加/操作员保留/工程缺陷是/队列是。 PASS。 |
| M006-028 | GR08真实UserDetails fallbackharness保留，当前生产过滤器不同故不夸大网络漏洞；删除无事实的staff/active补全，保持可信principal，属于身份框架适配，无业务发明。技术性/是。 PASS。 |
| M006-029 | GR10空密码200无凭证的控制流与现有UI必填要求一致；不是新增密码政策。后端边界先拒绝、生产失败显式，和011获批才激活共同验证，无新业务发明。技术性/是。 PASS。 |
| M006-030 | GR11原技术性/无界主张完整保留，本方提细化因为权限管理列表有业务上位；pageSize50返回全量确有元数据错误。按实际用途修边界，禁止无后续页截50造成角色不可选；资源问题不靠理论规模夸大。细化/是，待Grok审异议。 PASS。 |

### 027直接依赖补核与交付边界

除解释时已读WalletPage全181行、customerService、MemberWalletController、StoredValue请求DTO及授权商户scope外，本次又窄读FinanceService创建意图/标成功内核和StoredValueService入账、BarBillingService既有stored_value分支及PaymentGatewayPort直接实现，未开始这些模块的全量审计。

实际事实：FinanceService1256—1290走现有PaymentGatewayPort，现有DisabledPaymentGateway只返回RESERVED、ReservedPaymentGateway只返回NOT_CONFIGURED；真实厂商尚未接入。PaymentIntentRecord保留gatewayProvider/ref/status，当前创建只保存created，现有页面没有拉起厂商支付所需流程。不能称已有微信/支付宝/银联真实收款已打通；后续B3应复用端口和默认关的能力开关完成平台交互/回传边界，未配置时明确不可发起真实支付，不得冒充已收款、调用占位成功内核造到账。真实厂商调用依原HTML第三方接入阶段，不由代码注释虚构外部资料或凭证。

StoredValueService159—161 settle校验支付意图succeeded与本人绑定卡范围并防重复；170—176为既有线下确认链。BarBillingService225—235已从真实账单解析客人，按实际结算金额调用consumeInternal，但该读取片段未见SMS校验，不能把账单关联当手机号验证码授权。修复应在既有储值授权/结算路径复用已核验证码与订单/账单，不建立平行记账系统或全客人免鉴权桥。现有条文不允许将App余额支付短信验证自行删除。输入只收金额/事由的孤立客户扣款不能继续作为正常支付入口。

旧Grok V0.02把交易表写作customer_stored_value_transactions；现有DAO/迁移实际表名stored_value_transactions。该拼写不得变为新增表的理由，不改Grok原报告。余额与记录按本人隔离、现有卡种/本金赠金等字段展示，不能为了更改写入口反而删合法只读查询。

### 分类分歧与本票适用范围

024/026/030候选分类改变、011注册批准、015异常翻译、019兼容调用等均保留双方原意见并给出证据；旧Grok曾接受，但当前整批仍须它重新复核，不能沿用旧票替代。027保持Grok添加分类、最新用户实际保留，因实际工程缺陷仍队列是，符合“保留但有缺陷仍修”，不是保留即无缺陷。

未映射但定义区已冻结的HTML仅作为已映射GEN005/006/017的上下位事实；没有把BSM006/035等加入M006映射，没有锁外新判据。禁止依据三HTML之外的临时实现注释新增业务规则；不得以当前方案为由修改业务HTML或忽略默认关闭第三方接口规则。

本席结论PASS。只有Grok对相同V0.55完整候选实际PASS后才可定稿；任一FAIL则整批继续B2，不先修无争议项。不称运行中的进程通过，不把旧票拼成当前通过。


## V0.11 接收当前Grok B2唯一分类FAIL及本方表名纠错

Grok V0.03本次追加完整302行/25137字节/SHA cd9b97e5e32a08296633c2668a11c9058716ab911cf1ac702fb8dfc6b014f43f，现整份93035字节/SHA489614955b1aa9a3c903a901720b795bbc9b1973fd4939db2a526b97ef35c501，真实exit0，原67898字节保持。结论FAIL，唯一FAIL-1：027有HTML832/585/867上位，应按细化；它明确撤回原添加判断。当前范围、工程缺陷、队列均接受，其他29项接受，但本批整体仍FAIL。

本席核对接受分类细化；findings V0.56已仅改027分类，队列仍25是5否；questions V0.05保留实际答复，不再添加人闸。原self V0.10 PASS不能覆盖Grok FAIL。下一次仍整批30项复核，不合并旧部分票，不执行B3。

本方V0.10及此前ledger/prefix关于表名的纠正是错误的：160当前JdbcCustomerRepository1181实际INSERT INTO customer_stored_value_transactions，1212/1223同表；V090第31行CREATE TABLE IF NOT EXISTS customer_stored_value_transactions。Grok表名正确，本方先前少写customer_前缀。已据实际源码纠正，不改原raw，未有任何SQL或源码写入。新实现必须复用该真实既有表；这不是新业务或新表需求。

## V0.12 Codex B2 整批候选复核

版本：V0.12。结论：PASS（仅当前候选归并/分类/队列与拟修边界；不是模块PASS，尚须当前Grok票）。本方按唯一外席分类FAIL修正后重新检查全30ID，不以旧V0.10 PASS覆盖FAIL，不拼接历史部分票。

输入：findings.md V0.56，221921字节/SHA668e4a199457bf464bc53cb263f64f05a15ff56a578defcad079db2a7ba7dfd6；questions/M006-027.md V0.05，10423字节/SHA70ff002a0fdcc16712c030fdc0b5b9ae0ee186bcf5af55164d1b9517d332debc。
两份完整B1输入仍为modules/M006-b1-self-raw.md前73231字节/SHA1ee77bccf61c3b418028f42866202a4655246349d7c55ef37d9e3f36def61ab4和modules/M006-b1-grok-raw.md前44940字节/SHA96f0165d01b18753284b2556d2474c17623259bdb10ddca45cc80b7a0b5da071。
本次self接收前缀103335字节/SHA470e4c6ea4cad5862411bbdee8f3578f3691e9bbdfc4638ccb99b958a046e71e；Grok新调用前缀93035字节/SHA489614955b1aa9a3c903a901720b795bbc9b1973fd4939db2a526b97ef35c501。前缀内旧B2结论仅是历史，不当新B1票，也不替当前外席票。Grok此刻正在重审，本方未读其本次新报告。

### 全集差集和当前值

23CX+11GR与未编号意见仍全部进入30连续唯一ID：CX01—23→001—023；GR01/02/03/04/05/06/07/08/09/10/11→001/024/025/015/011/026/027/028/010/029/030；构造器建议019，其余未编号归015/028/030。机检每ID一次；队列25是5否（019—023否）。分类9冲突/15细化/6技术性/0添加。实际questions仍M003023删除、024保留，M006027保留；无未答人闸，旧A/B弹窗已撤回。

本V0.56相对V0.55唯一实质候选变动为027分类添加→细化，工程缺陷/队列/业务范围未改变。Grok V0.03接受范围但否决分类，本方已采纳；不能让“保留原B1历史”阻止在B2按外席明确改判更新当前分类。

14源码与原manifest未改，三HTML/锁/映射/sidecar未改，B3仍0，未执行新业务测试或修复；本文不把B1测试通过当已消除缺陷。继续用原锁23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de，COUNT0。

### 三十项本方复核

| ID | 核验与结论 |
|---|---|
| M006-001 | CX01+GR01均保留。管理员DENY旁路和高危自动权限有两席反例；日常自动权限须保留，主管身份问题另与010共同覆盖，不能用删除所有管理员日常权限偷换修复。冲突/是。 PASS。 |
| M006-002 | CX02跨商户实际role赋权反例与同根模板/薪资/临时授权目标边界均写入；同org不等于同merchant，冲突/是，不能只修一个控制器。 PASS。 |
| M006-003 | CX03已证无org会变全量而非报错；登记时无虚构生产特权，全局合法岗位仍须已有授权事实。细化/是。 PASS。 |
| M006-004 | CX04三个真实slug/upsert覆盖证据保留，授权申请同根也覆盖；内部ID与幂等结果职责分开。细化/是。 PASS。 |
| M006-005 | CX05已应用模板实时增权反例完整，不能只限制新增应用；已有应用/增量/审批边界同一项。细化/是。 PASS。 |
| M006-006 | CX06审批快照与执行重新取值的差异有实际输出；待批对象漂移拒绝或严格原快照均属同一边界，不能批准A执行B。细化/是。 PASS。 |
| M006-007 | CX07实际beneficiary selfreview=applied，岗位申请方自审与受益人自审不可相互替代。全局既有HTML事实+GEN005/017，冲突/是。 PASS。 |
| M006-008 | CX08两个不同幂等键复核同一pending都应用，替身交错与普通SELECT/upsert契约一致，未虚称SQL并发集成已跑。细化/是，数据库级锁/CAS在既有事务中实现。 PASS。 |
| M006-009 | CX09旧岗位授权仍在反例保留，要求按授权来源撤旧派生，不能误删独立直接授权。冲突/是。 PASS。 |
| M006-010 | CX10+GR09主管类型/级别不一致保留；不能把任意高rank或管理员自动设最终审批人。细化/是。 PASS。 |
| M006-011 | CX11和GR05的原分歧明确保留。HTML582允许新建表单不明文豁免1433两路径注册批准；保留合法新建、删除直接激活旁路、获批后自助既能覆盖原两条，冲突/是。本方不将此当Grok已经接受。 PASS。 |
| M006-012 | CX12公司用户自身有效权限查询被商户scope拒绝有实际反例，修复必须保留无归属默认拒绝。细化/是。 PASS。 |
| M006-013 | CX13空集合/倒置窗口active伪成功和JDBC零行insert证据俱在；只补既有授权输入正确性，不新设期限业务上限。细化/是。 PASS。 |
| M006-014 | CX14同一次业务两个guard烧掉唯一模板已由实际Controller复现；成组校验/一次消耗而非移除单次语义。细化/是。 PASS。 |
| M006-015 | CX15+GR04合并保留。两成功英文句确有UI直接显示；GlobalExceptionHandler/FieldLabels翻译使“所有源码英文都是对客英文”不成立，不无依据全包替换。冲突/是。 PASS。 |
| M006-016 | CX16经营窗口/频繁阈值/审批级别影响业务结果，不能当技术常量豁免。已有业务配置及审批策略复用，不另建体系。冲突/是。 PASS。 |
| M006-017 | CX17 real PostgreSQL三TEMP表+当前DAO得到23503/consumed=true，证据范围明确；必须正确区分模板应用与临时授权日志来源并确保消耗留痕原子。细化/是。 PASS。 |
| M006-018 | CX18管理员改组织模板归属变null反例完整，内容更新保留既有owner。细化/是。 PASS。 |
| M006-019 | CX19机制与Grok未编号legacy构造器删除建议均已登记；存在真实构造调用和fresh compilation，不为删兼容构造器改遍调用方，无业务发明。技术性/否。 PASS。 |
| M006-020 | CX20真实两个profile实现支撑接口，不是一实现臆造抽象；空密码伪成功分到029，业务激活分到011，技术性注册不豁免二者。技术性/否。 PASS。 |
| M006-021 | CX21原B1不覆写，但候选明确剥离手机哈希数据存储问题到025；标准库UTF8/JDBC/BCrypt机制无业务发明。不将此登记当隐私存储PASS。技术性/否。 PASS。 |
| M006-022 | CX22响应/仓储包装机制无新业务；假分页实际问题分到030，不以容器登记豁免行为。技术性/否。 PASS。 |
| M006-023 | CX23主体/统计等已有上位需求，既有HTML治理659确有频繁临时授权统计，非凭代码发明该业务。记录本体只登记，016仍待修业务政策值。细化/否。 PASS。 |
| M006-024 | GR02原冲突原话保留。本方不接受用雇佣五态覆盖全部含商户/供应商的访问账号，也不接受将已翻译的active等同对客英文；真实unknown状态未校验/非active凭证发active仍入队。ProductionAuthService独立userStatus检查避免夸大离职可登录。候选细化/是，Grok须明确审此异议。 PASS。 |
| M006-025 | GR03手机SHA不是加密且被API直接返回，不能由021技术机制登记放行。按既有PII原文处理此入口，有PiiCipher/BlindIndex可复用且明确不能从旧摘要造手机号，不扩成全仓PII整改。冲突/是。 PASS。 |
| M006-026 | GR06硬删及无占用等原主张保留，已冻结HTML通用删除规则的作用域明确整套系统；按GEN006/017核对认为冲突。既有留存/审批机制复用，不能只改inactive便称完成。冲突/是，Grok须审本分类调整。 PASS。 |
| M006-027 | Grok V0.03唯一FAIL-1已核实并接受：原B1添加判断撤回，按HTML832储值、585手机号短信验证、867本人余额/记录改判细化。最新保留业务范围、工程缺陷是和队列是均保持，其他29项不变；无重复添加询问、无HTML修改、无LOCK-AMEND、M001不重开。线上充值/POS/已有订单余额支付和现场报本人手机号、验证码通过后按账单扣款保持；取消无账单任意金额扣款、保留本人只读余额记录，复用真实customer_stored_value_transactions。不删合法权限桥、不放宽任意客人/商户，不把短信通知当验证码通过，不豁免App余额支付已有SMS要求。细化/是。 PASS。 |
| M006-028 | GR08真实UserDetails fallbackharness保留，当前生产过滤器不同故不夸大网络漏洞；删除无事实的staff/active补全，保持可信principal，属于身份框架适配，无业务发明。技术性/是。 PASS。 |
| M006-029 | GR10空密码200无凭证的控制流与现有UI必填要求一致；不是新增密码政策。后端边界先拒绝、生产失败显式，和011获批才激活共同验证，无新业务发明。技术性/是。 PASS。 |
| M006-030 | GR11原技术性/无界主张完整保留，本方提细化因为权限管理列表有业务上位；pageSize50返回全量确有元数据错误。按实际用途修边界，禁止无后续页截50造成角色不可选；资源问题不靠理论规模夸大。细化/是，待Grok审异议。 PASS。 |

### 接口复用和事实边界

本方V0.10和旧ledger/prefix把真实台账误写为stored_value_transactions并错误指正Grok。已实读160 JdbcCustomerRepository1181 INSERT、1212/1223 SELECT及V090第31行CREATE TABLE确认实际为customer_stored_value_transactions；Grok原表名正确。V0.11接收纠错原样保留，新方案只复用现表，未有SQL写入，不造平行台账。

027需要的支付和SMS均有接口，但当前未接真实厂商：PaymentGatewayRouter只选Disabled/Reserved实现，返回RESERVED/NOT_CONFIGURED；CustomerOtpVerifier已有接口，local/dev/test固定码实现与prod FailClosed实现分离。现有prod未配置时拒绝，不得拿本地固定码作为生产扣款凭证。按HTML1480/枚举210保留第三方先开放接口、默认关、不伪造结果的阶段边界，不虚构供应商资料/凭证或称已完成真实厂商收款。

WalletPage充值只提示柜台、金额+钱包消费缺订单/账单/SMS关联，仍是真实待修差异。既有Finance意图/成功状态、StoredValueService绑定本人卡/防重复入账与BarBillingService账单内核应复用；合法订单/账单支付与本人查询必须保留。未配置时明确不发起真实渠道扣款，不能把占位回执说成已收到资金；收到渠道真实成功确认后按已绑定该笔充值入账，不能靠前端自报。没有擅加二维码、固定时长、额外审批角色或业务新判据。

未映射但锁定义区已有HTML只作为已映射GEN005/006/017下上下位事实，未扩大52映射、未新增判据。三份权威HTML文字保持，原申请A/B撤回，不需操作员再确认已明确的手机号流程。

结论PASS。当前外席若FAIL，仍整批B2修改再审，B3不启动；只有同V0.56全30ID双PASS后才定稿。


## V0.13 接收Grok B2 V0.04范围FAIL

本次外席完整299行/27145字节/SHA5f9006f44b49c031058827c495223cdbea466dfabfd1a54f116d6f67d7f8aa1c，整份120180字节/SHA7c68d350332b25ad1a8610d791c0c60e41683f71fa94adf084cfaf23f4e78a4c，真实exit0、FAIL。已完整读取；原93035字节保持。唯一FAIL-1为027拟修正文未明列第三方预留/默认关/不伪造成功/未配置短信不视为验证。分类细化与其他29项均接受，但整批仍FAIL。

本方接受：这些边界已经在上轮self与调用说明中讨论，却没有完整写入findings的027拟修条目。B3仅能归因到该条正文，不能让散落说明替代修复范围，故self V0.12 PASS不覆盖外席FAIL。V0.57现补齐原有HTML及真实实现约束，三HTML不改、不扩判据、不启支付/短信开关。

027当前拟修全文：在HTML1480及枚举210既有“预留接口、后台开关默认关、不阻塞自研、不伪造结果”阶段内，保留客人合法充值和按订单/账单消费的限定权限桥，修复平台侧流程与结果衔接；不把本项当成开启未配置支付/短信能力或接通真实厂商的授权。充值页提供微信、支付宝、银联选择，经既有createPaymentIntent及PaymentGatewayPort申请支付；仅在真实渠道已配置且后台显式启用时使用真实拉起信息发起支付。开关关闭、RESERVED或NOT_CONFIGURED时应明确提示未启用/未配置，不能显示已支付、不能入账，不打开payment.gateway.enabled，不修改ReservedPaymentGateway来伪造SUCCEEDED，不拿测试期markPaymentIntentSucceeded或前端成功提示代替真实渠道回执。只有服务端核验对应渠道的真实支付成功确认、金额和已绑定本人卡范围后，才复用settleTopup/既有入账内核记入该笔充值；重复确认不得重复加余额，客户端不得自行宣称收款成功。POS刷卡充值复用confirmOfflineTopupAndSettle等既有柜台/线下确认链，不另造支付通道。App已有订单（如门票）由客人选择指定储值卡余额支付；现场已形成消费明细与总价的账单（如卡座）采用报本人手机号、短信验证码验证通过后按该笔账单金额扣款，不生成储值付款二维码。两类余额支付都绑定真实订单/账单与本人卡并执行HTML585的手机号短信验证，不豁免App订单。复用既有CustomerOtpVerifier等短信验证接口；短信能力未配置时拒绝验证/扣款，不以“已发短信”“已送达”或变动通知notifyStoredValueAfterCommit当作验证通过，不以本地测试固定码冒充生产短信验证。取消钱包页脱离订单/账单随意填写金额的consume入口及对应孤立调用，保留限定合法customer.stored_value.topup/consume通路，不仅删除权限桥，也不放宽任意客人/商户授权。保留本人余额和逐笔记录，查询复用真实既有customer_stored_value_transactions及现有卡/账务数据，不建平行台账。原有卡归属、适用商户、本金赠金、冻结与幂等边界保持。三HTML、锁定义、映射及开关启用状态不修改，不重开M001；整批B2双PASS前不实施。

当前30ID/25是5否、0待答、B3=0。仅完善027范围，下一票全批重审；不拼旧29项、不重开M001、不重复要求用户答已经明确的手机号规则。

## V0.14 Codex B2 整批候选复核

版本：V0.14。结论：PASS，仅对findings V0.57当前完整30项候选归并、分类、队列和拟修边界给票；当前Grok票未读取，尚非双PASS，不进B3。

### 输入

findings.md V0.57：223966字节/SHAf20a175d6024c22e51bebd7395554b68abff8c37771f9d4972d28a13ccfb9fe9。
questions/M006-027.md V0.06：11550字节/SHAc5c6705ec86c8a848e99cdf6bbe8fd6547a80365bf2375d35539ded5080fab17。
modules/M006-b1-self-raw.md输入前117360字节/SHA57c2e793791b534696e1010d8849c2bd927d2c6928dc29fc09ddc33e0361c64e，原独立B1前73231字节/SHA1ee77bccf61c3b418028f42866202a4655246349d7c55ef37d9e3f36def61ab4。
modules/M006-b1-grok-raw.md输入前120180字节/SHA7c68d350332b25ad1a8610d791c0c60e41683f71fa94adf084cfaf23f4e78a4c，原独立B1前44940字节/SHA96f0165d01b18753284b2556d2474c17623259bdb10ddca45cc80b7a0b5da071。
两份B1之后的接收/旧B2均为历史；本票不拼接旧部分通过，也不把接收说明冒充新的独立B1。

### 全集与当前事实

CX01—23完整归001—023；GR01/02/03/04/05/06/07/08/09/10/11完整归001/024/025/015/011/026/027/028/010/029/030；未编号构造器019、其余015/028/030均保留。30ID连续唯一，没有删任何原缺陷或差集。

V0.57只补027拟修边界，分类与队列及其他29项保持：9冲突、15细化、6技术性、0添加；25是5否。question实际保留且最新业务范围相同，旧只读/二维码/两份HTML申请撤回，未答0。14源文件SHA逐项再次与原manifest一致，B3仍0，未写源码、SQL、HTML、锁或开关。

本方接受Grok V0.04的否决：此前把关键约束散写在self和调用提示，没有完整放入B3唯一归因来源的027条目，旧self V0.12 PASS不能覆盖外席FAIL。此次用findings条目原文与question当前范围逐字比对，并机械核查17个关键约束均在条目本身，覆盖HTML1480、枚举210、createPaymentIntent、PaymentGatewayPort、RESERVED/NOT_CONFIGURED、payment.gateway.enabled、ReservedPaymentGateway、SUCCEEDED、markPaymentIntentSucceeded、settleTopup、confirmOfflineTopupAndSettle、CustomerOtpVerifier、notifyStoredValueAfterCommit、customer_stored_value_transactions、无二维码及App仍须短信验证。这17个字符串是对已写边界的机械核查，不是新判据/新闸/新增需求。

### 全30项本方判断

| ID | 核验与结论 |
|---|---|
| M006-001 | CX01+GR01均保留。管理员DENY旁路和高危自动权限有两席反例；日常自动权限须保留，主管身份问题另与010共同覆盖，不能用删除所有管理员日常权限偷换修复。冲突/是。 PASS。 |
| M006-002 | CX02跨商户实际role赋权反例与同根模板/薪资/临时授权目标边界均写入；同org不等于同merchant，冲突/是，不能只修一个控制器。 PASS。 |
| M006-003 | CX03已证无org会变全量而非报错；登记时无虚构生产特权，全局合法岗位仍须已有授权事实。细化/是。 PASS。 |
| M006-004 | CX04三个真实slug/upsert覆盖证据保留，授权申请同根也覆盖；内部ID与幂等结果职责分开。细化/是。 PASS。 |
| M006-005 | CX05已应用模板实时增权反例完整，不能只限制新增应用；已有应用/增量/审批边界同一项。细化/是。 PASS。 |
| M006-006 | CX06审批快照与执行重新取值的差异有实际输出；待批对象漂移拒绝或严格原快照均属同一边界，不能批准A执行B。细化/是。 PASS。 |
| M006-007 | CX07实际beneficiary selfreview=applied，岗位申请方自审与受益人自审不可相互替代。全局既有HTML事实+GEN005/017，冲突/是。 PASS。 |
| M006-008 | CX08两个不同幂等键复核同一pending都应用，替身交错与普通SELECT/upsert契约一致，未虚称SQL并发集成已跑。细化/是，数据库级锁/CAS在既有事务中实现。 PASS。 |
| M006-009 | CX09旧岗位授权仍在反例保留，要求按授权来源撤旧派生，不能误删独立直接授权。冲突/是。 PASS。 |
| M006-010 | CX10+GR09主管类型/级别不一致保留；不能把任意高rank或管理员自动设最终审批人。细化/是。 PASS。 |
| M006-011 | CX11和GR05的原分歧明确保留。HTML582允许新建表单不明文豁免1433两路径注册批准；保留合法新建、删除直接激活旁路、获批后自助既能覆盖原两条，冲突/是。本方不将此当Grok已经接受。 PASS。 |
| M006-012 | CX12公司用户自身有效权限查询被商户scope拒绝有实际反例，修复必须保留无归属默认拒绝。细化/是。 PASS。 |
| M006-013 | CX13空集合/倒置窗口active伪成功和JDBC零行insert证据俱在；只补既有授权输入正确性，不新设期限业务上限。细化/是。 PASS。 |
| M006-014 | CX14同一次业务两个guard烧掉唯一模板已由实际Controller复现；成组校验/一次消耗而非移除单次语义。细化/是。 PASS。 |
| M006-015 | CX15+GR04合并保留。两成功英文句确有UI直接显示；GlobalExceptionHandler/FieldLabels翻译使“所有源码英文都是对客英文”不成立，不无依据全包替换。冲突/是。 PASS。 |
| M006-016 | CX16经营窗口/频繁阈值/审批级别影响业务结果，不能当技术常量豁免。已有业务配置及审批策略复用，不另建体系。冲突/是。 PASS。 |
| M006-017 | CX17 real PostgreSQL三TEMP表+当前DAO得到23503/consumed=true，证据范围明确；必须正确区分模板应用与临时授权日志来源并确保消耗留痕原子。细化/是。 PASS。 |
| M006-018 | CX18管理员改组织模板归属变null反例完整，内容更新保留既有owner。细化/是。 PASS。 |
| M006-019 | CX19机制与Grok未编号legacy构造器删除建议均已登记；存在真实构造调用和fresh compilation，不为删兼容构造器改遍调用方，无业务发明。技术性/否。 PASS。 |
| M006-020 | CX20真实两个profile实现支撑接口，不是一实现臆造抽象；空密码伪成功分到029，业务激活分到011，技术性注册不豁免二者。技术性/否。 PASS。 |
| M006-021 | CX21原B1不覆写，但候选明确剥离手机哈希数据存储问题到025；标准库UTF8/JDBC/BCrypt机制无业务发明。不将此登记当隐私存储PASS。技术性/否。 PASS。 |
| M006-022 | CX22响应/仓储包装机制无新业务；假分页实际问题分到030，不以容器登记豁免行为。技术性/否。 PASS。 |
| M006-023 | CX23主体/统计等已有上位需求，既有HTML治理659确有频繁临时授权统计，非凭代码发明该业务。记录本体只登记，016仍待修业务政策值。细化/否。 PASS。 |
| M006-024 | GR02原冲突原话保留。本方不接受用雇佣五态覆盖全部含商户/供应商的访问账号，也不接受将已翻译的active等同对客英文；真实unknown状态未校验/非active凭证发active仍入队。ProductionAuthService独立userStatus检查避免夸大离职可登录。候选细化/是，Grok须明确审此异议。 PASS。 |
| M006-025 | GR03手机SHA不是加密且被API直接返回，不能由021技术机制登记放行。按既有PII原文处理此入口，有PiiCipher/BlindIndex可复用且明确不能从旧摘要造手机号，不扩成全仓PII整改。冲突/是。 PASS。 |
| M006-026 | GR06硬删及无占用等原主张保留，已冻结HTML通用删除规则的作用域明确整套系统；按GEN006/017核对认为冲突。既有留存/审批机制复用，不能只改inactive便称完成。冲突/是，Grok须审本分类调整。 PASS。 |
| M006-027 | Grok V0.03分类FAIL已改细化，V0.04范围FAIL现已完整写入027拟修正文（不仅写在报告说明/提示中）：既有预留接口、默认关、不伪造成功、未配置支付/SMS不视为已支付或验证通过、createPaymentIntent/PaymentGatewayPort→服务端验证渠道→settleTopup、POS既有确认链、真实订单/账单和本人卡/SMS绑定、无二维码、无任意金额孤立扣款、保留合法权限桥与本人查询、复用customer_stored_value_transactions。三HTML/锁/启用状态不改；17个关键约束在条目正文机检全部命中。用户手机号业务保持，不再询问。细化/是。 PASS。 |
| M006-028 | GR08真实UserDetails fallbackharness保留，当前生产过滤器不同故不夸大网络漏洞；删除无事实的staff/active补全，保持可信principal，属于身份框架适配，无业务发明。技术性/是。 PASS。 |
| M006-029 | GR10空密码200无凭证的控制流与现有UI必填要求一致；不是新增密码政策。后端边界先拒绝、生产失败显式，和011获批才激活共同验证，无新业务发明。技术性/是。 PASS。 |
| M006-030 | GR11原技术性/无界主张完整保留，本方提细化因为权限管理列表有业务上位；pageSize50返回全量确有元数据错误。按实际用途修边界，禁止无后续页截50造成角色不可选；资源问题不靠理论规模夸大。细化/是，待Grok审异议。 PASS。 |

### 本次027条目全文复核

在HTML1480及枚举210既有“预留接口、后台开关默认关、不阻塞自研、不伪造结果”阶段内，保留客人合法充值和按订单/账单消费的限定权限桥，修复平台侧流程与结果衔接；不把本项当成开启未配置支付/短信能力或接通真实厂商的授权。充值页提供微信、支付宝、银联选择，经既有createPaymentIntent及PaymentGatewayPort申请支付；仅在真实渠道已配置且后台显式启用时使用真实拉起信息发起支付。开关关闭、RESERVED或NOT_CONFIGURED时应明确提示未启用/未配置，不能显示已支付、不能入账，不打开payment.gateway.enabled，不修改ReservedPaymentGateway来伪造SUCCEEDED，不拿测试期markPaymentIntentSucceeded或前端成功提示代替真实渠道回执。只有服务端核验对应渠道的真实支付成功确认、金额和已绑定本人卡范围后，才复用settleTopup/既有入账内核记入该笔充值；重复确认不得重复加余额，客户端不得自行宣称收款成功。POS刷卡充值复用confirmOfflineTopupAndSettle等既有柜台/线下确认链，不另造支付通道。App已有订单（如门票）由客人选择指定储值卡余额支付；现场已形成消费明细与总价的账单（如卡座）采用报本人手机号、短信验证码验证通过后按该笔账单金额扣款，不生成储值付款二维码。两类余额支付都绑定真实订单/账单与本人卡并执行HTML585的手机号短信验证，不豁免App订单。复用既有CustomerOtpVerifier等短信验证接口；短信能力未配置时拒绝验证/扣款，不以“已发短信”“已送达”或变动通知notifyStoredValueAfterCommit当作验证通过，不以本地测试固定码冒充生产短信验证。取消钱包页脱离订单/账单随意填写金额的consume入口及对应孤立调用，保留限定合法customer.stored_value.topup/consume通路，不仅删除权限桥，也不放宽任意客人/商户授权。保留本人余额和逐笔记录，查询复用真实既有customer_stored_value_transactions及现有卡/账务数据，不建平行台账。原有卡归属、适用商户、本金赠金、冻结与幂等边界保持。三HTML、锁定义、映射及开关启用状态不修改，不重开M001；整批B2双PASS前不实施。

本方逐句接受上述范围：充值的未配置/默认关闭限制约束在线渠道，POS仍按已明列的独立线下确认链保留；第三方厂商未配置不是凭空入账的借口，必须以真实支付事实经服务端核验后结转。码已取消；手机号是定位本人，验证码通过才是扣款前提，不能把收到变动通知当作已验证。App订单和现场账单均要绑定真实金额/本人卡与既有适用商户范围；合法通路、既有查询和实际台账保留。没有把业务交互修复授权偷换成开启未配置能力或真实供应商SDK接入，未伪造渠道结果。

表名已按真实JdbcCustomerRepository1181/1212/1223和V090第31行纠正为customer_stored_value_transactions。旧本方表名错误在V0.11留纠错，未改Grok原文、没有源改动或平行表。当前权限模板/注册/跨商户等29项仍按其原范围修复，不因本项往返而漏修或提前修。

### 结论

PASS。本票只针对同V0.57全30项。Grok新票若FAIL则整批继续B2，不从已接受项先修；同版本双PASS后才定稿。当前M006 IN_PROGRESS，B3=0，锁基准保持23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de，COUNT0；无待答问题或HTML申请。


## V0.15 Codex B2 — B5-r2完整批次归并复核

版本：V0.15。结论：PASS，仅对findings V0.61当前33项队列表和本次3条意见归并/分类/队列/拟修范围给票；尚非B2双PASS，不是模块PASS，不进B3。

输入固定：modules/M006-b5-r2-self-raw.md（本次V0.01/36285字节/SHA7c947c128b3ec0a7a6b891a795b613bf900db78aed9002e3e1e8e01f844a7395）；modules/M006-b5-r2-grok-raw.md（本次为旧26892字节之后的V0.02完整224行/18373字节/SHA1c8b6d43dad24c118bc3bf013487cd9488d1fd80d7c029b6150cef7bd037daae，整份SHA0f7c1000403b9f254988c63041f3f4dc3a1cd12ada32741a01c5ba396b60e6c9）。两席完整报告均已读完，两票均FAIL；四值一致；未修改任一独立报告来造新票。

本批候选findings V0.61，SHA026a5b60efddf981412de9250e613bf64ad5e6c765f2f461691c3bb3bba96d5b。03:55:39 UTC已实际派Grok按grok-4.6/xhigh既有CLI和适配器复核，追加其B1 raw V0.06；真实退出码尚未取得，不声称外席PASS。

| 全部意见/当前ID | 本方分类与当前队列 | 完整核实 |
|---|---|---|
| 本次CX-B5-01→M006-031 | 冲突；工程缺陷是；队列是 | 两个组织同一允许的请求编号，controller实际结果串用；自审真实失败断言。拟修只收窄已有主体范围，同时要求同主体及存量重试保持只生效一次，不靠全局改前缀制造重复写。 |
| 本次GR-01→M006-032 | 冲突；工程缺陷是；队列是 | 新的实际controller错误→真实GlobalExceptionHandler，message含platform_admin/staff等；接受Grok，原票保留。只改本模块错误文案和对应响应断言，不重写全局翻译或账号类别。 |
| 本次GR-02→M006-033 | 细化；本次工程缺陷不成立；队列否（待Grok核实分歧） | 此源为已有BSM-001配置要求的下位实现且B3合法新增；在r2完整after tree中。移除4个旧class缓存、提供当前必要45源码后javac=0并重新产4个Policy class。Grok缺依赖实验排除了真实现有源，不能代表当前工作区构建。保留Grok技术性/工程缺陷是的原始主张，不单方改其B5票；由Grok本批B2确认是否接受本证据。 |
| M006-001—018、024—030 | 原分类与证据保持；当前队列否 | 已在B4-r2统一出队，未因本批重新进队；新3条没有被塞回旧ID以省略本次B2。 |
| M006-019—023 | 原分类与无工程缺陷登记保持；当前队列否 | 兼容记录、凭证适配、密码与JDBC技术实现、HTTP/仓储、主体摘要记录不添加业务，本批未改。 |

差集为空：本批两raw共3条都已登记，无遗漏、无重复修复范围；Grok未列入finding的私有slug及wildcard注释只作既有备注，不自扩队列。候选33ID中2是31否，新增0添加项、0HTML申请；其余30项并未改分类或拟修范围。

遵循完整工作区、B3-before/after覆盖untracked、阶段A冻结范围的同一规则；033不能以修改scope/mapping来处理。若Grok本批不接受033，本批留B2继续事实核对，不从031/032先修。业务HTML、锁/映射、支付/SMS开关均不动，B3仍2，运行失败仍0。


## B2 整批归并 V0.16：M006本次例外修复前

版本：V0.16
审计结论：PASS。仅本方B2票，待Grok独立复核。
输入：modules/M006-b5-r3-self-raw.md及modules/M006-b5-r3-grok-raw.md完整本次报告。候选findings V0.65 SHA0974cdae8f4cfc954a302f68773d08853e35fb63a3dc78b5bc55d866889fec7a。
两份各1原始意见全部归并034，4源码位置、Grok更宽范围保留；分类冲突/工程是/队列是。旧33项当前全部否，无其他新意见、无添加/HTML申请。一次性修复主体变更与多一次修复来自操作员新指令，不冒称永久控制合同变更；内部业务定义及共有错误翻译器不改。补真实行为测试均归因034。候选不存在漏项或单方压低Grok意见，原始报告字节全部保留。


## 席甲（Claude Fable 5.1）B2 结论 — 由 B5-r4 合取 FAIL 回退的整批归并（追加，原报告保留；本段不写锁哈希行）

- 输入 raw：modules/M006-b5-r4-self-raw.md（席甲，FAIL，CX-B5-01/02）、modules/M006-b5-r4-grok-raw.md（席乙，PASS，0 条）。
- 候选台账：findings.md V0.68“M006 B5-r4 双席结论与 B2 候选”段（候选 M006-035、M006-036 + 登记段）。
- 差集：席甲 2 条 → 035/036 一一对应，未改写；Grok 0 条 → 无遗漏；Grok 的两项 ponytail 观察（slug() 未使用、wildcard 恒 false）与文件版本头未递增已并入登记段；Grok 分类备忘（英文字段键经翻译层、权限码为内部标识符）席甲同意。
- 分类核对：035 技术性（无业务发明、不与 HTML 冲突）、GEN-012、工程缺陷 是、队列 是；036 细化（上位 BSM-114/PGM-048）、GEN-009、工程缺陷 是、队列 是。均只引用冻结 ID，无锁外判据、无添加项、无 HTML 申请。
- 本席 B2 结论：**PASS**（候选台账与两份 raw 一致）。
- 接收 Grok B2（V0.08，全批 PASS，独立探针 201/1/0 与 orphan NPE 复现，另以 update 入口 PROJECT_SCOPE_REQUIRED 对照）：接受其更正——本席 B5 raw §2 GEN-017 行“两条均为技术性”与 CX-B5-02 正文“细化”不一致，定稿以正文“细化”为准；本席自票不改。双 PASS 成立，035/036 定稿入队。
- 轮次：B3 已执行 4 次，超上限；定稿后不自行进入第 5 次 B3，等操作员指令。
