# M006 B5-r3 Codex 全模块复审原始报告

版本：V0.01
模块：M006 `10-backend-main-access-01`，6/322。
步骤：B5；ledger IN_PROGRESS；B3已执行3次；方案复核已修2；运行失败0。
审计结论：FAIL（本方完整审计共1条新意见CX-B5-01）。
CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

## 输入与独立顺序

控制提示词V0.17，SHA256 c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21。
M006原14文件与52冻结判据均完整审阅；实际当前源总3262行，不将原方案2988有效行指标误称为当前物理行。
B4-r3内容/范围双PASS已完成；findings V0.63 SHA728d950f7bc0f1c89300d250a9d8c89430b207e89d883fee7f7fb48c5dd7c254，33个旧ID全部出队。
先于本方审阅派Grok，2026-09-06 04:33:58 UTC。第一次5分钟检查04:39:12 UTC仍运行，session44329，真实退出码尚未知。
本方完整报告落盘前没有读取本次Grok raw正文/意见或CLI原始JSON。Grok回执适配器只暴露指定路径和真实exit；当前无Grok结论，不能冒称双PASS/FAIL。
此次为修复后全模块复审，未发现一条就停止，未只重看031/032或修复diff。B5期间未改业务代码、测试源、三HTML或锁。
延续本会话已读取技能的规定用法：grilling的自问与反例、systematic-debugging的真实数据流/最小复现、receiving-code-review先核事实及verification-before-completion查实证；不调第三席、不用旧票替代新审。

## 14文件完整覆盖

| 文件 | 当前行数/字节 | SHA256 | 全文核对结果 |
|---|---|---|---|
| backend/src/main/java/com/complex/platform/access/AccessActor.java | 11 / 223 | 5b0115b479d4a2f9ddec2f7bc3837ffbcb3ca18dfd30b98ada61250545eddf3a | 可信认证主体数据载体；六字段组织/商户/状态传递，无独立登录执行入口。 |
| backend/src/main/java/com/complex/platform/access/AccessController.java | 299 / 15252 | 42e79bc67140cf31ad60cfb5a930b56b023f47e5cf70106d109b8482a0fa8d0c | 全部Controller入口与权限、请求编号、路径目标覆盖；审批review入口147—155为新反例的真实入口。 |
| backend/src/main/java/com/complex/platform/access/AccessCredentialProvisioner.java | 12 / 335 | c87b5ff721de2308b4a0ab0b9395a9dca19f8551d157f798acece0ec6688ab5b | 既有凭证适配器契约；与Service必需依赖前置检查一致。 |
| backend/src/main/java/com/complex/platform/access/AccessGrantPositionSummary.java | 10 / 232 | 08fa864f1b0cada3309ea24503d159df5b1075c77e49a04ad75aa61ca1c156df | 岗位授权统计数据记录；数量与频繁标记来自Service政策计算。 |
| backend/src/main/java/com/complex/platform/access/AccessGrantRequestRecord.java | 35 / 934 | b056364a8d6526123b1137c0ec7016240ca42083e9ca1050f905a4e55166ee89 | 授权申请快照及状态/人员/时间记录；permissionCodes防空并复制。 |
| backend/src/main/java/com/complex/platform/access/AccessGrantSummary.java | 9 / 219 | 7648579e55d34871a4deea19ccde7eef74f592613d3cdf6015d7ca7dce7e1d96 | 汇总输出类型，独立展示岗位与人员统计。 |
| backend/src/main/java/com/complex/platform/access/AccessGrantUserSummary.java | 8 / 155 | 80fb7b5d47b0afdec3cda37e2c954950242603ed23e2d847f136b3b794636a7e | 人员授权汇总记录，未做额外业务判断。 |
| backend/src/main/java/com/complex/platform/access/AccessRepository.java | 259 / 10273 | c4f48cc0d0bd62bfbc356eb531f18e990cc96f8de5a04dac2e746a4190a6f680 | 完整仓储接口/默认方法；授权事实锁、指纹、分页、来源替换的契约与直接JDBC实现对应。 |
| backend/src/main/java/com/complex/platform/access/AccessService.java | 1946 / 97674 | 786c6c697e2d13b30e05731ca7585c5c8d3a00376cc045308ffca34f250c0044 | 完整1946行：模板、角色、岗位、临时授权、复核、账号、薪资/职级、统计、校验与私有辅助。发现CX-B5-01，其余本轮未形成新意见。 |
| backend/src/main/java/com/complex/platform/access/AccessUser.java | 37 / 1042 | 717ebbd9d9ce56217a6045f20b42f47ec4aa76fdd2a14ac97c6b49e94387ceb1 | 账号输出字段；手机号摘要JsonIgnore；身份与账号状态非人事状态。 |
| backend/src/main/java/com/complex/platform/access/AuthorizationService.java | 496 / 22963 | 1260154b681dd7027b17f3d371b2e0304ca4df034204e885b4dd743bd271e4ad | 完整496行：主体解析、DENY优先、一次性消费、审批级别、组织/商户/平台范围与新请求范围。 |
| backend/src/main/java/com/complex/platform/access/DevAccessCredentialProvisioner.java | 48 / 2000 | 8d7ea478ed913838609ec974b06152b744191d38c3ecd5844c7fb3e086a92f39 | 开发凭证实现只在dev/test profile；缺账号拒绝、状态激活遵循真实账号。 |
| backend/src/main/java/com/complex/platform/access/EffectivePermissions.java | 23 / 882 | 68ff6d1da086bfb7e1aa0b33aed0de3962a515737a91096fb791cfc9faced44e | 有效权限输出记录，集合复制；具体wildcard由Authorization给false，不凭注释授予。 |
| backend/src/main/java/com/complex/platform/access/JdbcAccessCredentialProvisioner.java | 69 / 3103 | 06a4e3f9f09c2902eabb9d53161330b05b1fdfe3c7f2ae5821c129ed121a082e | JDBC凭证实际用户更新、密码编码和登录状态；找不到账号时报错，不伪成功。 |

## 全部52冻结条目逐项检查

“本14文件无该业务执行器”表示按模块载体检查后无可归责实现，不是删除判据，也不是该业务全仓通过。

| 冻结ID | 本次核对与证据落点 |
|---|---|
| GEN-001 | Controller公开入口对应账号、模板、角色、岗位和授权审批；本轮只复核既有实现，不把命名或私有辅助方法的风格意见登记成行为缺陷。 |
| GEN-002 | 98项当前源码行为检查通过；新增无效审批结论反例实际进入Controller→Service→异常处理器并复现报错文案问题。全后端test-compile在B3-r3实跑exit0，相关源码未变。 |
| GEN-003 | 本轮重新编译60个真实源/测试文件；98项覆盖正常与错误分支；新增反例编译51源成功、执行断言失败，未用编译失败冒充BUG复现。 |
| GEN-004 | 账号类型、组织、商户、状态、密码、授权目标/时间、权限列表均在写入前校验；reviewGrant待审记录及申请人/受益人检查先于审批落地。 |
| GEN-005 | Controller按动作校验权限；Authorization按真实主体、DENY优先、有效期、一次性来源检查；平台管理员日常权限桥不自动授予高风险权限；33项专门回归覆盖旧问题。 |
| GEN-006 | 修改入口@Transactional，授权事实锁覆盖审批事实与落地；岗位派生来源按关系替换，独立来源保留；凭证配置缺失拒绝账号成功返回。 |
| GEN-007 | 当前重复请求按主体/组织隔离，真实Controller两组织同编号结果独立、各自重试只生效一次；遗留p记录三状态均拒绝混用或再次执行；一次性权限使用原子消费。 |
| GEN-008 | AccessActor/UserRecord两种认证主体使用一致范围；DTO与接口字段按现状检查，路径用户ID覆盖请求体目标；未增加账号类型或授权申请种类。文案例外见CX-B5-01。 |
| GEN-009 | 未知主体/缺少组织/凭证服务缺失/错误时间窗拒绝；旧请求记录返回可识别409而不伪成功；无效审批结论能够失败但提示仍含英文，见CX-B5-01。 |
| GEN-010 | 密码经既有编码器存储；账号输出手机号摘要被@JsonIgnore阻断；日志记录结构化错误码；新旧请求结果不跨组织共享。 |
| GEN-011 | 模板/授权/账号/岗位与职级写入关联操作编号及成功/失败事件；授权复核记录申请、审批和生效状态，查询授权汇总留痕。 |
| GEN-012 | 授权清单数据库分页及后台统计窗口；参考字典返回真实条数，不把50条截断伪装完整；本轮无性能基准或生产容量声明。 |
| GEN-013 | 全后端test-compile B3-r3真实exit0，58条本模块历次修复业务/测试路径仍与最终tree一致；本轮60源javac和98测试exit0。未把未跟踪的合法B3依赖人为移除后制造构建失败。 |
| GEN-014 | M006这14文件无迁移源；直接依赖V386来源保全修复在B3-r2完成，当前源码未改；本轮不执行共享数据库迁移，不声称已部署。 |
| GEN-015 | FAIL：授权审批无效decision报错保留approve/reject两种英文枚举；真实错误响应复现。除该新意见，已修账号类型错误提示本轮回归通过。 |
| GEN-016 | 核对当前方法、路径、版本和实际测试输出，未以旧PASS替代本轮；当前模块为IN_PROGRESS且未发布，不宣称全仓完成。 |
| GEN-017 | CX-B5-01按已冻结全中文原文判冲突，非添加业务；33条旧ID已B4双PASS出队，B5不自行修复、不重新解释批准规则。 |
| GEN-018 | 主张使用冻结HTML、真实可执行源和临时验证命令；未用工具名、惯例或旧审计结论充当最终行为证据。 |
| BEQR-157 | AccessUser账号状态是认证状态，不替代员工待入职/在职等人事状态；人工账号创建不能提前激活注册员工，注册审批对接回归通过。 |
| BEQR-182 | 14文件没有考勤日结果计算；本模块只承载主体与权限，无另发明出勤/休息口径。 |
| BEQR-183 | 14文件没有设备领取打卡执行器；无把账号登录当打卡的实现，具体考勤留在其既有模块。 |
| BEQR-184 | 14文件不定义请假类型或审批单；未添加请假类型。 |
| BEQR-185 | 14文件不执行请假/销假或薪资冲回；授权申请pending/applied不是请假状态，未混用。 |
| BEQR-186 | assignStaffLevel/resolvePositionGrant单独要求基础工资，负数拒绝且按分存储；未在此模块重写绩效/扣款/税率。 |
| BEQR-187 | 当前模块无现金提成发放入口或计算逻辑；职级/岗位赋值不等于发放。 |
| BSM-001 | 组织/商户归属和后台授权政策有现有来源；政策窗口、阈值、风险级别及上下限从已有配置读取；查询参数不能覆盖政策。 |
| BSM-003 | 模板修改、存档、恢复和删除申请沿用既有权限及记录；直接删除接口拒绝，不直接抹掉历史。 |
| BSM-009 | 账号、权限、岗位为业务准入基础；账户建立与凭证激活受审批和凭证配置前置约束。 |
| BSM-028 | 重复请求和数据归属规则在Controller/Authorization/共享幂等边界对齐；模板目标、角色目标跨组织/跨商户拒绝，031本轮实际回归通过。 |
| BSM-029 | AccessActor/User保存组织/商户关联，职级和员工注册直接依赖保持；本模块未建立个人手机设备绑定替代记录。 |
| BSM-073 | 普通商户只能列出/操作本商户用户与授权；平台列表视角保留单独判定；本模块不处理费用异议/投诉关闭。 |
| BSM-084 | 有效权限本人查询无消费一次性权限；他人查询需权限且同组织/商户范围；本模块不提供读取其他员工薪资的直接页面或入口。 |
| BSM-085 | 注册账号必须通过公司/商户既有审批后才创建；人工创建员工提示走注册流程；本模块没有把代订记名或爽约政策写成授权新规则。 |
| BSM-086 | 本模块不计算班次、考勤或请假；岗位分配与班次实际执行分离，没有在此修改月度归属口径。 |
| BSM-087 | 基础工资单独传入、职级不自动决定具体金额；本模块不实现月结可见性/绩效工资或税款算法。 |
| BSM-088 | 不存在现金现抽/人员服务商品/营销打卡实现；权限编码引用不等于新增相应业务。 |
| BSM-114 | 平台/员工/商户/客人认证主体按已有类型处理；共享幂等范围对客人及员工采用真实主体，商户保持商户范围；未在本模块建立新终端或支付入口。 |
| BSM-115 | 授权来源/角色/模板/岗位保留关联ID，审计事件关联操作编号；无修改跨域收入、库存、票务的业务判定。 |
| BSM-120 | 权限粒度按permissionCode；职级与具体薪资分离；后台开关未修改；仅检查M006对既有注册、归属、审批接口的承接，不声称涵盖全仓所有确认规则。 |
| PGM-001 | FAIL：全中文错误提示出现英文审批枚举，CX-B5-01；归属、责任与重复提交相关部分本轮已有真实测试支持。 |
| PGM-008 | 员工账号与注册审批、凭证建立相关连接存在；私人/工作手机行为非本14文件实现。 |
| PGM-022 | 当前主体project scope与组织范围执行默认拒绝；本模块没有修改空间分配或把商户代替项目。 |
| PGM-024 | 岗位和职级分配保留目标员工/组织；没有低消、容量、库位或排班计算实现。 |
| PGM-026 | 组织、商户、角色归属在服务层校验；更改授权不转移既有财税主体关系；无把普通授权配置当公司权责审批。 |
| PGM-032 | 员工注册状态及凭证建立使用现有StaffService衔接；开发/生产凭证适配器按profile生效，缺配置不可报告建号成功。 |
| PGM-033 | 公司审批前不创建已激活员工账号；手工创建员工不能替代规定注册。设备绑定/换机/柜子扫码非此模块载体。 |
| PGM-034 | Authorization.requirePlatformComplianceScope限制商户/外包主体；本模块无会话保存/清理接口，未改留存政策。 |
| PGM-035 | 当前无提成发放、营销打卡或人员服务商品执行路径；权限本身不承诺开展这些业务。 |
| PGM-036 | 本模块只提供主体和权限事实给设备端，未新增设备种类/绑定状态或核销执行器。 |
| PGM-048 | 商户角色与目标用户组织/商户一致性校验，商户列表隔离；平台聚合权限与商户范围分开，正常商户授权业务未整体禁用。 |
| PGM-059 | AccessUser及账号创建输出不返回phoneLookupDigest；手工输入手机号摘要不落入账户；凭证存密码摘要；客人查询专门通道不在本14文件另行实现。 |
| PGM-066 | FAIL：CX-B5-01违反已确认全中文口径；其他本模块可执行部分按权限点、时间戳和自动留痕核对，未改开关或支付时间规则。 |

## 三条审阅路线及必要直接依赖

1. 第一性原理：从主体是谁、属于哪个组织/商户、请求针对谁、实际授予哪些权限、哪个记录产生结果及失败是否落部分数据逐层追踪。全14文件数据类型→Controller→Service→Authorization/仓储→凭证与事件路径逐个核对。审计依据为冻结HTML、当前可执行产物与下述命令；不以业务注释自身作为依据。
2. 顺从式：52条逐一按载体核对，实际98测试覆盖账号创建与注册审批、角色与模板授予/编辑、岗位来源替换、临时授权时间窗、审批快照/申请人与受益人、权限有效集/一次性消费、范围过滤、政策统计与归档/删除审批；当前031/032均实际通过。接口返回phoneLookupDigest不可见，日常管理员权限保留，高风险权限需显式授予。
3. 反例：未知认证主体、缺组织、其他商户目标、超出本人权限、审批后角色变动、受益人自批、缺凭证提供者、时间倒置、一次性消费竞争、同编号两组织重复提交、旧幂等记录OK/IN_PROGRESS/FAILED_RETRYABLE均有可运行检查。另主动从无效审批decision走真实错误处理器，发现CX-B5-01；随后继续覆盖其余文件和判据后才落本报告。
直接依赖包括access/dto、IdempotencyService/CallerScopeResolver、FieldLabels/GlobalExceptionHandler、SupervisorReviewService、AuditEventService、StaffService与现存相关测试。本轮没有另开后续模块。此前同批读过真实认证Filter、CustomerService/IntegrationService/第三方接收及一体机心跳：正常公开写入口使用自身记录或先要求客人身份；未据推测宣称共享范围修复破坏公开接口。现存UserRecord/AccessActor范围一致及商户/无认证范围4断言在B4实跑通过。
新范围使用既有用户ID长度前缀+组织，结果存为caller:加64位哈希共71字符，原operation_id列128，未另加表。遇到旧无所有者p记录不猜其归属，拒绝复用和重复执行，保留原记录。

## 完整新意见

### CX-B5-01 — 无效授权审批结论的报错仍显示英文枚举

- 位置：backend/src/main/java/com/complex/platform/access/AccessService.java:1689—1694（根因）；同文件703—724（真实执行路径）；AccessController.java:147—155（入口）；common/api/GlobalExceptionHandler.java:49—61与FieldLabels.java:504—529（真实响应处理）。
- 严重程度：P2；2.1分类：冲突；工程缺陷：是。仅临时ID，未进入新一批B2定稿，不在B5修复。
- 冻结判据：PGM-001、PGM-066、GEN-015；2.3依据类型：4（下方完整可复现命令输出），并以现有冻结HTML引文/当前可执行源定位。
- 操作与实际结果：有审核权限的后台人员对待审批授权提交无效decision时，Controller先校验两项审核权限和请求编号，Service找到pending申请并校验申请人/受益人后，normalizeDecision抛VALIDATION_FAILED。实际错误响应message为“审批结论必须为通过（approve）或驳回（reject）”。
- 原因：业务抛错直接拼写给人看的英文审批枚举；FieldLabels对已经含中文的话术按原文返回。它不是仅日志、JSON字段名或内部编码；approve/reject在此明确作为错误提示的枚举取值展示，落在冻结HTML“提示、报错、枚举取值全部中文”的范围。
- 业务影响：后台审核人员遇到无效选项得到混合语言提示；本反例未显示审批被错误执行，影响限定在错误提示，不夸大为资金/客人扣款问题。
- 最小修复方向（尚未执行）：保留允许值approve/reject及错误码，仅把该校验错误的显示文字改为中文“审批结论必须为通过或驳回”，保留可失败的真实错误响应检查。
- 与旧M006-032区分：032的定稿边界只针对账号类型报错，B4已双PASS出队；本意见是授权审批分支，不擅自当作032仍在队列。到B3上限后应交操作员决定是否恢复，而非无ID直接修改。
- PGM-001原文关键约束：所有终端给用户看的提示/报错/枚举取值必须全部中文，仅单据号、设备编号、二维码内容和系统内部标识符可保留。PGM-066重复确认同一要求；未新增判据。

## 实际验证命令及输出

### 当前完整核心回归，B5-r3重新执行
命令在160主工作区执行，临时源码/runner/classes仅.scratch临时目录，退出自动清理；生产源重新javac，未改共享target。
```powershell
@'
# V0.01 M006-r3 current-source behavior checks
from pathlib import Path
import subprocess,tempfile
runner="package com.complex.platform.access;\npublic class M006Runner {\n public static void main(String[] args)throws Exception {\n int passed=0,failed=0;\n for(String name: args){var c=Class.forName(name);int localPass=0,localFail=0;\n for(var m:c.getDeclaredMethods())if(m.isAnnotationPresent(org.junit.jupiter.api.Test.class)){\n var ctor=c.getDeclaredConstructor();ctor.setAccessible(true);var t=ctor.newInstance();\n try {for(var b:c.getDeclaredMethods())if(b.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)){b.setAccessible(true);b.invoke(t);}\n m.setAccessible(true);m.invoke(t);passed++;localPass++;\n }catch(java.lang.reflect.InvocationTargetException e){failed++;localFail++;System.out.println(\"FAIL \"+name+\".\"+m.getName()+\": \"+e.getCause());}\n finally{for(var a:c.getDeclaredMethods())if(a.isAnnotationPresent(org.junit.jupiter.api.AfterEach.class)){a.setAccessible(true);a.invoke(t);}}\n }\n System.out.println(name+\" \"+localPass+\" PASS / \"+localFail+\" FAIL\");\n } System.out.println(\"RESULT \"+passed+\" PASS / \"+failed+\" FAIL\");if(failed>0)System.exit(1);\n }\n}"
with tempfile.TemporaryDirectory(prefix="m006-r3-tests-",dir=".scratch") as d:
 root=Path(d).resolve();cpfile=root/"classpath"
 r=subprocess.run(["mvn","-o","-q","-f","backend/pom.xml","dependency:build-classpath","-Dmdep.outputFile="+str(cpfile)],capture_output=True,text=True)
 if r.returncode:print(r.stdout,r.stderr);raise SystemExit(r.returncode)
 cp=str(Path("backend/target/classes").resolve())+":"+cpfile.read_text().strip()
 src=root/"M006Runner.java";src.write_text(runner,encoding="utf-8");out=root/"classes";out.mkdir()
 base=Path("backend/src/main/java/com/complex/platform");tests=Path("backend/src/test/java/com/complex/platform")
 sources=list((base/"access").glob("*.java"))+list((base/"access/dto").glob("*.java"))+list((base/"common/idempotency").glob("*.java"))
 sources += [base/p for p in ["access/staff/StaffService.java","common/api/FieldLabels.java","common/api/GlobalExceptionHandler.java","governance/review/SupervisorReviewService.java","evidence/AuditEventService.java"]]
 sources += [tests/p for p in ["support/InMemoryAuditEventRepository.java","support/InMemoryIdempotencyRepository.java","support/TestSecurityActors.java","access/M006SecurityRegressionTests.java","access/AccessServiceTests.java","access/AuthorizationServiceTests.java","common/idempotency/IdempotencyServiceTests.java"]]+[src]
 r=subprocess.run(["javac","-encoding","UTF-8","--release","21","-cp",cp,"-d",str(out),*[str(p) for p in sources]],capture_output=True,text=True)
 print("JAVAC_EXIT",r.returncode,"SOURCES",len(sources));print(r.stdout,r.stderr)
 if r.returncode:raise SystemExit(r.returncode)
 r=subprocess.run(["java","-cp",str(out)+":"+cp,"com.complex.platform.access.M006Runner","com.complex.platform.access.M006SecurityRegressionTests","com.complex.platform.access.AccessServiceTests","com.complex.platform.access.AuthorizationServiceTests","com.complex.platform.common.idempotency.IdempotencyServiceTests"],capture_output=True,text=True)
 print(r.stdout,r.stderr);raise SystemExit(r.returncode)

'@ | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -'
```
真实输出/exit：
```text
JAVAC_EXIT 0 SOURCES 60
 Note: backend/src/main/java/com/complex/platform/access/staff/StaffService.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.

com.complex.platform.access.M006SecurityRegressionTests 33 PASS / 0 FAIL
com.complex.platform.access.AccessServiceTests 35 PASS / 0 FAIL
com.complex.platform.access.AuthorizationServiceTests 21 PASS / 0 FAIL
com.complex.platform.common.idempotency.IdempotencyServiceTests 9 PASS / 0 FAIL
RESULT 98 PASS / 0 FAIL
 Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build as described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3
WARNING: A Java agent has been loaded dynamically (/home/james/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.10/byte-buddy-agent-1.18.10.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended


EXIT_CODE=0
```

### 新反例：真实Controller、Service和错误响应
```powershell
@'
# V0.01 temporary B5 read-only verification
from pathlib import Path
import subprocess,tempfile
probe="\n    @Test void b5InvalidDecisionResponseIsChinese() throws Exception {\n        when(repository.listPermissions()).thenReturn(List.of(\n            new PermissionDefinition(\"access.grant_request.review\", \"审核授权\", \"access\", \"medium\", false, \"active\"),\n            new PermissionDefinition(\"governance.supervisor_review.approve\", \"主管审核\", \"governance\", \"medium\", false, \"active\")));\n        requests.put(\"pending-language\", new ObjectMapper().readValue(\n            \"{\\\"id\\\":\\\"pending-language\\\",\\\"status\\\":\\\"pending\\\",\\\"targetUserId\\\":\\\"target\\\",\\\"requestedBy\\\":\\\"requester\\\",\\\"requestType\\\":\\\"user_role\\\"}\",\n            AccessGrantRequestRecord.class));\n        var controller = new AccessController(service, authorization);\n        ApiException failure = null;\n        try {\n            controller.reviewGrant(\"pending-language\", new AccessGrantReviewRequest(\n                \"b5-invalid-decision\", \"质量检查\", List.of(), \"wait\", \"测试无效选项\"));\n        } catch(ApiException e) { failure = e; }\n        assertThat(failure).isNotNull();\n        assertThat(failure.code()).isEqualTo(\"VALIDATION_FAILED\");\n        var handler = new com.complex.platform.common.api.GlobalExceptionHandler();\n        var method = handler.getClass().getDeclaredMethod(\"handleApiException\", ApiException.class);\n        method.setAccessible(true);\n        var response = (org.springframework.http.ResponseEntity<?>) method.invoke(handler, failure);\n        var body = (com.complex.platform.common.api.ApiResponse<?>) response.getBody();\n        System.out.println(\"B5_ERROR_RESPONSE=\"+body.message());\n        assertThat(body.message()).doesNotContainPattern(\"[A-Za-z]\");\n    }\n"
runner="""package com.complex.platform.access;
public class M006Runner {
 public static void main(String[] args)throws Exception {
 int passed=0,failed=0;
 var c=M006SecurityRegressionTests.class;
 for(var m:c.getDeclaredMethods())if(m.isAnnotationPresent(org.junit.jupiter.api.Test.class)&&m.getName().startsWith("b5Invalid")){
 var t=c.getDeclaredConstructor().newInstance();
 try {for(var b:c.getDeclaredMethods())if(b.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)){b.setAccessible(true);b.invoke(t);}
 m.setAccessible(true);m.invoke(t);passed++;System.out.println("PASS "+m.getName());
 }catch(java.lang.reflect.InvocationTargetException e){failed++;System.out.println("FAIL "+m.getName()+": "+e.getCause());}
 finally{for(var a:c.getDeclaredMethods())if(a.isAnnotationPresent(org.junit.jupiter.api.AfterEach.class)){a.setAccessible(true);a.invoke(t);}}
 }
 System.out.println("RESULT "+passed+" PASS / "+failed+" FAIL");if(failed>0)System.exit(1);
 }
}"""
with tempfile.TemporaryDirectory(prefix="m006-b5-self-",dir=".scratch") as d:
 root=Path(d).resolve();cpfile=root/"classpath"
 r=subprocess.run(["mvn","-o","-q","-f","backend/pom.xml","dependency:build-classpath","-Dmdep.outputFile="+str(cpfile)],capture_output=True,text=True)
 if r.returncode:print(r.stdout,r.stderr);raise SystemExit(r.returncode)
 cp=str(Path("backend/target/classes").resolve())+":"+cpfile.read_text().strip()
 original=Path("backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java").read_text()
 test=root/"M006SecurityRegressionTests.java";test.write_text(original[:original.rfind("}")]+probe+"\n}\n",encoding="utf-8")
 src=root/"M006Runner.java";src.write_text(runner,encoding="utf-8");out=root/"classes";out.mkdir()
 base=Path("backend/src/main/java/com/complex/platform")
 sources=list((base/"access").glob("*.java"))+list((base/"access/dto").glob("*.java"))
 sources += [base/"common/api/FieldLabels.java",base/"common/api/GlobalExceptionHandler.java",base/"common/idempotency/IdempotencyService.java",base/"governance/review/SupervisorReviewService.java",base/"evidence/AuditEventService.java"]
 sources += [Path("backend/src/test/java/com/complex/platform/support/InMemoryAuditEventRepository.java"),Path("backend/src/test/java/com/complex/platform/support/InMemoryIdempotencyRepository.java"),test,src]
 r=subprocess.run(["javac","-encoding","UTF-8","--release","21","-cp",cp,"-d",str(out),*[str(p) for p in sources]],capture_output=True,text=True)
 print("JAVAC_EXIT",r.returncode,"SOURCES",len(sources));print(r.stdout,r.stderr)
 if r.returncode:raise SystemExit(r.returncode)
 r=subprocess.run(["java","-cp",str(out)+":"+cp,"com.complex.platform.access.M006Runner"],capture_output=True,text=True)
 print(r.stdout,r.stderr);raise SystemExit(r.returncode)

'@ | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -'
```
真实相关stdout节选（仅省略JVM/Mockito环境提示）：
```text
JAVAC_EXIT 0 SOURCES 51
B5_ERROR_RESPONSE=审批结论必须为通过（approve）或驳回（reject）
FAIL b5InvalidDecisionResponseIsChinese: java.lang.AssertionError:
Expecting actual:
  "审批结论必须为通过（approve）或驳回（reject）"
not to contain pattern:
  "[A-Za-z]"
RESULT 0 PASS / 1 FAIL
EXIT_CODE=1
```
编译成功后仅断言英文展示失败，未把环境失败视为产品缺陷。夹具只提供待审记录和已有审核权限；没有mock错误文案或FieldLabels，后者均为当前真实源码。

### 同批构建与范围校验的准确界限
B3-r3实跑：`mvn -o -q -f backend/pom.xml test-compile -DskipTests`，真实exit0，空输出；本轮未重复宣称执行全后端测试。
B4-r3实跑当前Authorization及接口范围probe：JAVAC_EXIT0；SCOPE_CHECK 4 ASSERTIONS PASS；此前后源码未变。
本B5再次用git diff --name-only f49ef25cdadc0d4572f169d8c33b5e971c21f024 c77d4453db7eb9b92388de630fdd5121cdef7959列出历次修复路径，排除既定审计/.scratch产物后逐一git show after:path比较当前字节：CURRENT_REPAIR_PATHS 58，DIFFER_FROM_R3_AFTER []。
当前HEAD 395e6fbab027020da1c38237fad17e8edcd7a132。
三HTML均逐字等于HEAD，哈希：
- business-enum-quick-reference-v0.1.html：4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66
- business-system-map-for-operations-v0.1.html：3edff56a7640565d08983621e34d726d149261e70fd747ecb88b8e9cc27774ea
- platform-governance-map-for-operations-v0.1.html：9864216e609df2f319fa2cf7e51b61f06003ce45dff64a1a4098e71435865a68
本轮未执行共享数据库迁移/写数据、真实支付商户端到端交易、全终端浏览器或安卓真机测试；不将这些未跑项说成通过。支付渠道真实生产配置不因M006回归而完成。并发专项这里只实跑既有原子消费/重试行为检查，没有新一轮共享数据库并发压测。

## Grok实际投递命令及未完成项

以下为本次实际执行的命令全文；-p拼接的是上方hash对应的控制提示词。原始CLI stdout/stderr由既有适配器进程内缓冲并丢弃。本方落盘时EXIT_CODE未知，Grok报告仍未读。
```powershell
$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$auditPrompt = '仅执行M006第3次修复后的B5全模块审计，模块6/322，主工作区main，ledger IN_PROGRESS，B3=3。本轮B4已双席内容/范围PASS，findings V0.63 SHA728d950f7bc0f1c89300d250a9d8c89430b207e89d883fee7f7fb48c5dd7c254，33个ID当前队列均否。严格按plan/modules.md中M006原有14文件和criteria-lock.md该模块全部52冻结条目，第一性原理、顺从式、反例三线完整复审当前整个模块；不得只看上轮diff或031/032，不得以旧B4或旧B5票代替本次全审。当前最近完整after tree c77d4453db7eb9b92388de630fdd5121cdef7959；源码与该tree一致（审计产物后续有流程记录）。AccessService现V0.13、AuthorizationService现V0.04；其余既有源正常纳入完整阅读。允许为事实核对追必要直接依赖、HTML与现存测试；B3合法新增依赖就在当前工作区，不能刻意删掉其源后声称整体构建失败，冻结A范围不得改。完整审完、把本次所有意见一次写齐后才给PASS/FAIL；发现第一条仍继续审，不中途修/路由。本次两席独立：禁止读取当前M006-b5-r3-self-raw.md或其内容，本方也在自己的完整raw落盘前不读你报告。只读业务源码/HTML/锁/台账/问题；临时验证只在.scratch隔离并清理自己的文件，不编译共享target、不写共享业务库，不新建额外审计产物，不派第三席。实际预检合同适用技能，不把缺技能写为已调用。报告必须有14文件逐项覆盖及全部52条检查的结果、具体源码和行为证据、所有新意见的临时ID/四分类/依据类型，及真实测试命令输出和明确未跑边界；无条件通过概念不适用。操作员明确本工作是对自己写/修改的代码进行开发调试和BUG质量修复，按具体行为与影响描述。完整报告写到指定 docs/audit-2026-09/modules/M006-b5-r3-grok-raw.md，存在则保留旧内容完整追加本次报告，不存在创建V0.01；写你独立实算的CRITERIA-LOCK-SHA256独占行。CLI回执只由适配器生成，不承担标记格式。以下完整V0.17继续适用：
' + [IO.File]::ReadAllText('D:\AI\ccwork\160-audit-prompt.md', [Text.Encoding]::UTF8)
@{prompt=$auditPrompt} | ConvertTo-Json -Compress | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -c ''import sys,json,subprocess,pathlib
root=pathlib.Path("/home/james/complex-management").resolve()
p=root/"docs/audit-2026-09/modules/M006-b5-r3-grok-raw.md"
def meta():
 if not p.exists(): return None
 s=p.stat(); return (s.st_size,s.st_mtime_ns,s.st_ctime_ns,s.st_ino)
before=meta()
prompt=json.load(sys.stdin)["prompt"]
r=subprocess.run(["/home/james/.local/bin/grok","-p",prompt,"-m","grok-4.6","--reasoning-effort","xhigh","--output-format","json","--permission-mode","bypassPermissions","--no-subagents","--disable-web-search","--cwd",str(root)],cwd=root,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
code=r.returncode
err="GROK_EXIT" if code!=0 else "PATH_MISMATCH" if not p.resolve().is_relative_to(root) or p.is_symlink() else "REPORT_MISSING" if not p.is_file() else "REPORT_EMPTY" if p.stat().st_size==0 else "REPORT_STALE" if meta()==before else None
print("ADAPTER_ERROR="+err if err else "REPORT_PATH=docs/audit-2026-09/modules/M006-b5-r3-grok-raw.md")
print("EXIT_CODE="+str(code))
sys.exit(1 if err else 0)
'''
```
指定结论出处：docs/audit-2026-09/modules/M006-b5-r3-grok-raw.md。
当前无三道人闸问题。下一步只在本报告完整落盘后、按5分钟周期取真实交付结果，再读Grok完整报告；两席完整意见收齐与B5后四值检查完成后，按B3=3上限登记BLOCKED-3ROUNDS并交操作员。本方FAIL不允许略过尚未完成的外审或自动执行第四次修复。

落盘UTC：2026-09-06T04:41:55.657305+00:00


## V0.02 — 接收Grok完整报告及终态记录（非新独立票）
原V0.01的34586字节前缀/SHA72da471a14607a68d3c363a0a65c7ec426dfe7b2649da01da7f9b8f767f681a3完整保留。
2026-09-06 04:49:42 UTC按5分钟周期检查取得真实EXIT_CODE=0；指定Grok报告21532字节/317行/SHA06506fb252baac014c9ed4d1f89c546b92479e54aa84289255d6b78c3498b0bb全文读完，审计FAIL，1条GR-01。此前原本方全文已封存，独立顺序未破坏。
Grok GR-01与本方CX-B5-01共享审批文案主证，另指出515/1061/1491三处同类报错；本方接收后核对当前四处源码和既有错误翻译器，保留其完整范围，不将其意见缩成仅1694，不冒称它的探针是本方实跑。
本方与Grok均FAIL，14文件及52条实际全量覆盖；Grok表标题对BEQR/PGM计数有文字笔误，但列中实际ID集合为7BEQR/13PGM，合计52，未因此遗漏原映射条目。其99测试明确省略两项StaffService测试，未跑项保留；本方实际98包含该两项。Grok报表只保留测试摘要而未列出73源完整javac参数/99行，故这99项是外席自述测试证据，不写成已在本方完整重放。
B5后实算/sidecar/ledger/Grok四值：
1: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
2: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
3: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
4: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
LOCK-AMEND-COUNT: 0
三轮六份B4 raw经真实git diff --binary逐字包含校验全部true，完整树对/字节/SHA交付见findings V0.64。本轮仅登记终态，不修新意见。
## M006 B5-r3完整双席意见与三轮交付（V0.64）

两席本次整个模块均审完后统一判定：**B5 FAIL；模块 BLOCKED-3ROUNDS；B3已执行3次**。
Grok于2026-09-06 04:49:42 UTC检查取得真实EXIT_CODE=0，报告317行/21532字节，SHA06506fb252baac014c9ed4d1f89c546b92479e54aa84289255d6b78c3498b0bb，全文已读；交付成功不等于审计通过。
本方独立原始完整报告34586字节，SHA72da471a14607a68d3c363a0a65c7ec426dfe7b2649da01da7f9b8f767f681a3，先于读取Grok封存。原票FAIL，原1条意见保留；后续接收记录仅追加，不覆盖原票。

### 本批全部意见（终态待恢复；未做B2定稿）
| 原始来源 | 问题及真实影响 | 位置 | 原分类/工程缺陷 | 当前处理 |
|---|---|---|---|---|
| self CX-B5-01 | 无效审批结论报错给用户显示approve/reject，实际Controller→Service→错误响应复现；不改变审批结果 | AccessService.java:1694，入口147—155/703—724 | 冲突/是，PGM-001/066、GEN-015，类型4 | 原始意见保留，待操作员恢复M006后整批B2 |
| Grok GR-01（与上条同类，完整保留其更宽范围） | 同上；另有岗位模板删除审核拒绝提示USB，以及两处薪资币种错误提示CNY/输入币种 | AccessService.java:515、1061、1491、1694 | 冲突/是，PGM-001/066、GEN-002/015，类型1+4 | 原始意见保留，四处合为一类文案问题的归并建议，尚未正式ID |

原始源：[本方完整报告](modules/M006-b5-r3-self-raw.md)、[Grok完整报告](modules/M006-b5-r3-grok-raw.md)。
本方接收后核对四处当前源码及GlobalExceptionHandler:49—61/FieldLabels:504—529；中文中夹英文原样透传。Grok额外三处的运行探针证据归属Grok，本方不冒称重跑了其99项测试。
旧M006-031与032在本轮两席各自测试中通过，保持旧33ID全部队列否；不得把这批新问题直接塞入已出队旧ID或无ID修复。没有遗漏另一类新意见、没有新增业务规则、没有HTML申请。
潜在最小变更仅四处提示文本；保留内部代码、允许的decision/币种值、权限与业务判断。恢复后仍须先整批B2双PASS再修，不在本轮突破三次上限。
验证：本方当前源98项回归全通过；独立真实审批响应反例1失败；Grok报告99项所选单元/JDBC测试通过及报错探针确认英文，明确省略2个StaffService测试（本方98项中已经实际执行），不把两组数字相加称197个不同测试。未跑真实支付厂商、全端浏览器/安卓、全库迁移或完整mvn test。

### B5后四值与锁
四值均为 `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de`：①当前实算；②sidecar；③ledger顶部；④本次Grok raw最后一条独占哈希行。LOCK-AMEND-COUNT=0，校验实际命令/输出在本次操作员回执与本方raw接收追加段。
没有因失败修改锁、scope、模块映射或当前源码，三HTML仍与HEAD逐字相同。

### 三轮完整diff交付
以下所有六份raw都含对应两树**完整未过滤**git diff --binary；本次逐字包含校验均true，不仅是stat或节选。源码仍等于第三轮after，未回滚已通过修复。
| B3轮次 | before tree | after tree | 完整diff字节及SHA256 | 双席原始diff报告 |
|---|---|---|---|---|
| 1 | f49ef25cdadc0d4572f169d8c33b5e971c21f024 | e845764656fb7f9c166026371371b720f10fde2a | 384478 / f8caaf8d47ea2993a9ea1d4b8a43e0c4f47246618d327e1a5410428234f6f5cc | [self](modules/M006-b4-r1-self-raw.md) / [Grok](modules/M006-b4-r1-grok-raw.md) |
| 2 | 7fecb8e739b6b04475755fffbb8e106fa0759713 | 2e346ad1b72a5df60d8e10eb1df9cc2b1add2cf9 | 17644 / cc2fc0f92106bd49386038da79b4b1e1b862cf447bc6bc753322417efd413a41 | [self](modules/M006-b4-r2-self-raw.md) / [Grok](modules/M006-b4-r2-grok-raw.md) |
| 3 | 11319ccfc6c0a3b609bc1cfdd4579766a7118705 | c77d4453db7eb9b92388de630fdd5121cdef7959 | 14687 / a04dfea8324fd7a11d5b5cfebf3585fd93c9cb32ec3cd05fde2bb8b610956717 | [self](modules/M006-b4-r3-self-raw.md) / [Grok](modules/M006-b4-r3-grok-raw.md) |

停止原因：控制提示词V0.17第4节“第3次B3后……该次B4或B5审完仍FAIL→……状态记BLOCKED-3ROUNDS……是否重启由操作员另行下指令”。当前B5完整结束、两席均FAIL，故不自动回B2/B3、不加第四轮、不开始M007、不提交合并推送M006。
需要操作员：新指令恢复M006；恢复后保留现有修复/原始报告，先处理本批全部意见，而非重开M001或全仓。

