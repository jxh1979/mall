# M006 B5-r2 Codex 完整复审原始报告

版本：V0.01

审计结论：FAIL

## 当前范围与独立性

M006 / 322，10-backend-main-access-01，B3已执行2次，ledger IN_PROGRESS。提示词V0.17，SHA256 c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21。审计对象是main主工作区14个文件3264实际行，不是仅5文件r2 diff。

03:35:45 UTC先派Grok本次完整复审，指定同名B5-r2 raw追加。旧调用session28812在通信恢复后无法取得真实退出，旧26892字节/SHA52c17c3c1aac346d0ea12998b283f7a5b7956e90420d17f17bbafd397451f2a7报告仅做metadata核对，正文未读，未称其失败或成功。本次self完整落盘前未读任何B5 Grok正文/CLI JSON；没有第三席，没有修改生产源或台账。

本报告继续本次已完成的逐文件阅读；通信恢复后实算14文件SHA与已读内容一致，55个已修源均与r2 after tree逐字一致。本次新增两条独立隔离执行命令复核当前源；完整判据52项再次逐条读取。发现问题继续审完，最后统一结论。

## 全部文件覆盖

| 文件 | 完整行数 | SHA256 | 检查内容 |
|---|---:|---|---|
| backend/src/main/java/com/complex/platform/access/AccessActor.java | 11 | 5b0115b479d4a2f9ddec2f7bc3837ffbcb3ca18dfd30b98ada61250545eddf3a | 身份DTO；认证主体、组织、商户、状态字段，无旁路身份推断。 |
| backend/src/main/java/com/complex/platform/access/AccessController.java | 299 | 42e79bc67140cf31ad60cfb5a930b56b023f47e5cf70106d109b8482a0fa8d0c | 全部读写入口和权限码；幂等键、请求体、批量权限复核和生命周期路由。 |
| backend/src/main/java/com/complex/platform/access/AccessCredentialProvisioner.java | 12 | c87b5ff721de2308b4a0ab0b9395a9dca19f8551d157f798acece0ec6688ab5b | 凭证适配接口及测试用noop；生产自动装配缺提供者时拒绝创建。 |
| backend/src/main/java/com/complex/platform/access/AccessGrantPositionSummary.java | 10 | 08fa864f1b0cada3309ea24503d159df5b1075c77e49a04ad75aa61ca1c156df | 岗位/职级/权限码次数摘要；不承载工资与考勤计算。 |
| backend/src/main/java/com/complex/platform/access/AccessGrantRequestRecord.java | 35 | b056364a8d6526123b1137c0ec7016240ca42083e9ca1050f905a4e55166ee89 | 授权申请完整快照、申请与决定主体、项目商户字段；与审批指纹调用对应。 |
| backend/src/main/java/com/complex/platform/access/AccessGrantSummary.java | 9 | 7648579e55d34871a4deea19ccde7eef74f592613d3cdf6015d7ca7dce7e1d96 | 两个统计结果集合，不隐藏额外业务判断。 |
| backend/src/main/java/com/complex/platform/access/AccessGrantUserSummary.java | 8 | 80fb7b5d47b0afdec3cda37e2c954950242603ed23e2d847f136b3b794636a7e | 用户申请次数与中文建议DTO。 |
| backend/src/main/java/com/complex/platform/access/AccessRepository.java | 259 | c4f48cc0d0bd62bfbc356eb531f18e990cc96f8de5a04dac2e746a4190a6f680 | 全部仓储契约；单次消费原子更新、指纹锁、岗位来源、分页和策略配置接口。 |
| backend/src/main/java/com/complex/platform/access/AccessService.java | 1946 | 79449d002be76914aa1e74681e112b77b25b6673ed7ad650117e9d0992dc82d0 | 完整1—1946行：模板1—275、权限/角色276—405、岗位模板与生命周期406—560、岗位与临时授权561—650、申请与复核651—772、查询统计773—950、账号与薪资951—1103、直接授权及快照1104—1507、策略/校验/工具1508—1946；边界按实际方法覆盖，不作抽样。 |
| backend/src/main/java/com/complex/platform/access/AccessUser.java | 37 | 717ebbd9d9ce56217a6045f20b42f47ec4aa76fdd2a14ac97c6b49e94387ceb1 | 员工访问记录、个人信息隐藏、附证列表拷贝；账号状态不冒充员工五状态。 |
| backend/src/main/java/com/complex/platform/access/AuthorizationService.java | 498 | 6c76917544710d8f49e0f098377e09922f4d462266f0eae3533ecb612eb5cb3d | 完整1—498行：认证主体、DENY优先、日常与高敏、有效权限、单次消费、主体作用域、审批等级和项目商户边界。 |
| backend/src/main/java/com/complex/platform/access/DevAccessCredentialProvisioner.java | 48 | 8d7ea478ed913838609ec974b06152b744191d38c3ecd5844c7fb3e086a92f39 | local/dev凭证路径；非空密码、真实账号、active/invited/disabled有效性。 |
| backend/src/main/java/com/complex/platform/access/EffectivePermissions.java | 23 | 68ff6d1da086bfb7e1aa0b33aed0de3962a515737a91096fb791cfc9faced44e | 有效权限结果及兼容构造；不再通过wildcard无界放权。 |
| backend/src/main/java/com/complex/platform/access/JdbcAccessCredentialProvisioner.java | 69 | 06a4e3f9f09c2902eabb9d53161330b05b1fdfe3c7f2ae5821c129ed121a082e | 真实JDBC凭证适配，状态与密码校验、BCrypt、受影响行数拒绝伪成功。 |

## 三线结论

1. 第一性原理：身份、角色、权限模板、临时授权和复核分别承载锁内基础管理需求。审批与真实凭证不能被手工账号创建取代；查看有效权限不消耗授权；技术DTO和测试适配不发明业务。逐方法追溯已完成。
2. 顺从式：全部52冻结判据见下表；现有31项安全回归与56项Access/Authorization回归共87项通过，不能据此忽略新增反例。14源及直接依赖已隔离编译，未重跑全仓构建或共享数据库迁移。
3. 对抗式：复核DENY先于管理员默认权、高敏需实际授权、跨商户/缺组织、模板扩权、快照漂移、自审、单次批消费、真实凭证、岗位来源替换及统计覆盖。构造两个已授权公司操作人的正常首次API创建，发现不同组织的幂等范围合并；正式问题仅下列CX-B5-01。二维码现场扣款已按用户要求移除，保留本人手机号短信验证，不将第三方未配置伪装为成功。

## 本次全部临时finding

### CX-B5-01 — 无商户的不同组织共享幂等命名空间，创建角色回放另一组织的结果

- 严重度：P1。
- 文件：backend/src/main/java/com/complex/platform/access/AuthorizationService.java:199（resolve整体199—218）；backend/src/main/java/com/complex/platform/access/AccessService.java:300（createRole整体300—330）。直接消费者：common/idempotency/IdempotencyService.java:72及JdbcIdempotencyRepository.java:23/37。
- 2.1分类：冲突；工程缺陷成立。依据类型：2（可执行代码/测试）＋4（下方完整复现命令与实际输出），并以锁内HTML原文为类型1上位依据。
- 冻结判据：GEN-002、GEN-004、GEN-005、GEN-007、GEN-009、GEN-010；BSM-028、BSM-114、PGM-001。BSM-028原文“看不到别的商户和别的项目的数据”；BSM-114“不同人只看自己该看的、只做自己该做的”；PGM-001按归属、权限、责任及“同一个操作被重复提交，系统只认一次”。不同主体各自创建不是同一个操作。
- 实际链：两个组织员工均具有access.role.manage及被授予的LOW权限；各自controller.createRole使用契约允许的同一非空请求编号company-first-role及相同创建正文。AuthorizationService.resolve分别返回p，IdempotencyService把两次都写成p:createRole；第二次走replay跳过创建lambda及其授权内容验证。真实Jdbc的去重和查找也仅使用(operation_id,idempotency_key)，与内存复现键一致。
- 输出：scope_a=p、scope_b=p、roles_written=1；org-b controller返回success=true和org-a创建的role_91975467-7699-47eb-9ff4-2303317a41c4角色内容。RoleRecord组织字段带@JsonIgnore，因此响应中的organizationId为null，不能误写为返回明文org-a；存储只有org-a的角色，断言org-b自身新角色失败。
- 正常性边界：接口只要求非空幂等键，没有规定键必须全平台全球唯一。反例是两个已授权主体各自合法首次请求，未要求随机UUID碰撞、修改数据库或猜另一个用户账号。已读AccessUsersPage使用randomUUID，故不声称标准UI会日常随机碰撞；也不能依赖某个前端习惯替代服务端主体隔离（GEN-005）。
- 最小处置方向：删除无merchant即共享p的范围归并，按已有可信主体/组织字段收窄已有resolve结果；保留幂等机制，不增加审批、状态、判据、业务配置或HTML内容。正式ID及队列须待两份完整raw统一进入B2后确定；本报告没有授权直接修复。

## 已核而未列问题的假说

ActionGateAspect仅在GuardedAction.permission非空时调用requirePermission。现场账单相关BarService注解使用capability，所以“每个capability都会重复消费一次权限”的假说被现有实现否证，不列finding。未验证的英文提示猜测不作为问题；未开展别的模块全量审计。

## 全部冻结判据逐项核对

| ID / 标题 | 本模块检查与结果 |
|---|---|
| GEN-001 必要性与最小性 | 角色、模板、账号、审批、统计分别对应基础权限治理；无额外抽象或业务扩写。 |
| GEN-002 可执行正确性 | 当前源隔离javac成功；现有87项成功，新增跨组织反例失败，见CX-B5-01。 |
| GEN-003 测试有效性 | 实际调用controller/service/resolver/idempotency，断言真实角色写入；不是字符串匹配测试。 |
| GEN-004 信任边界校验 | 正常授权写入校验存在，但幂等回放未保持主体边界，CX-B5-01。 |
| GEN-005 授权与最小权限 | DENY、跨商户、缺组织、受益人/申请人自审等已检；跨组织回放仍失败，CX-B5-01。 |
| GEN-006 数据完整性 | 申请审批锁/快照、角色来源替换与凭证同事务链已核，继承B4-r2相同字节的TEMP实证，不冒称本次重跑数据库。 |
| GEN-007 并发与幂等 | 相同业务重试有已有预留与完成机制；不同主体却共享p作用域，CX-B5-01。 |
| GEN-008 接口与模式一致性 | DTO、仓储、controller和当前fixture联合编译；组织@JsonIgnore导致回放组织字段为空，仍暴露另一组织角色id/内容，非修复。 |
| GEN-009 错误处理与恢复 | 凭证缺失及请求错误失败显式；跨组织创建回放另一结果为成功，CX-B5-01。 |
| GEN-010 安全与敏感信息 | phoneHash不序列化，账号手工手机号摘要已删；跨组织回放仍可返回其他组织角色资料，CX-B5-01。 |
| GEN-011 可观测与审计证据 | 读写/失败审计、来源事件与审批记录逐方法检查；没有把技术审计元数据转成业务输入。 |
| GEN-012 资源与性能边界 | 申请列表真实分页和窗口；统计取配置窗口；全量引用字典不冒充分页，无新增负载假设。 |
| GEN-013 构建配置与依赖 | 当前依赖classpath、Java21隔离编译与执行成功；未运行修改共享target的编译命令。 |
| GEN-014 数据库迁移正确性 | 本模块仓储契约与V386相符；55变更源仍等于r2冻结树，具体TEMP迁移实证引用此前B4-r2，不重复迁移。 |
| GEN-015 界面可用性与可访问性 | 模块无页面；直接连接的账号/授权统计中文建议与DTO检查；不把内部权限码当业务中文标签。 |
| GEN-016 文档可验证性 | 当前路径、行数、SHA及测试输出直接留存；不把B4或旧Grok文件当B5通过。 |
| GEN-017 业务来源分类 | 按冲突→细化→技术性→添加；CX-B5-01为隔离明文冲突，019—023技术性记录仍无新业务发明。 |
| GEN-018 第一性原理证据 | 读取全部模块源和锁内原文；新问题以可复现命令与实际控制器执行证实。 |
| BEQR-157 员工状态 | 员工五状态与账号active/invited/disabled区分；账号建立受注册审批控制。 |
| BEQR-182 考勤日结果 | 本模块提供主体与权限，不含考勤日计算；无替代该枚举或考勤算法。 |
| BEQR-183 打卡方式 | 本模块不处理打卡；未扩写扫码/NFC/宽限规则。 |
| BEQR-184 请假类型 | 本模块不创建请假类别；无新增业务类型。 |
| BEQR-185 请假单状态 | 本模块不处理请假状态机；授权申请pending不混作请假状态。 |
| BEQR-186 薪资单构成 | 薪资配置入口只验证已有职级/人民币非负金额并保存结构化详情；不在此发明绩效/扣薪计算。 |
| BEQR-187 员工提成发放 | 没有现金现抽、提成发放接口。 |
| BSM-001 整体说明：这套系统到底管什么 | 基础归属与授权策略配置已查；跨组织回放违背归属隔离，CX-B5-01。 |
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | 按已有权限执行配置/查询/审批；岗位模板软删除生命周期保留。 |
| BSM-009 基础管理 | 模块即基础管理权限主体入口；归属回放问题在本模块定义内。 |
| BSM-028 主要功能 | 明文跨主体/项目数据红线与重复操作原则；CX-B5-01。 |
| BSM-029 和谁有关 | 身份、组织、商户、职级作为授权与责任输入；不以请求字段伪造调用者。 |
| BSM-073 主要功能 | 商户操作受permission与merchant限定；没有新增商户投诉或结算规则。 |
| BSM-084 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | 注册审批后员工自助基础角色，查询本人和授权查询分路；非创建通用staff旁路。 |
| BSM-085 日常工作 | 公司员工/商户员工注册路径在直接StaffService依赖中核对；已有两项回归实际通过。 |
| BSM-086 排班、考勤与请假 | 模块不实现排班/考勤/请假计算，没有改动该业务规则；权限服务不取代设备/排班闸。 |
| BSM-087 我的薪资 | 薪资详情字段不计算或提前展示未月结工资，不添加绩效类型。 |
| BSM-088 员工提成、活动执行与人员服务边界 | 无营销打卡、陪场商品或现金提成接口。 |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | controller按动作和目标限制；跨组织创建的回放错误仍成立，CX-B5-01。 |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | 权限与各域通过主体/归属联动，读取必要直接调用不等于开启其他模块。 |
| BSM-120 已确认规则和实施前输入项 | 单功能权限、M职级、可配置政策与预留接口边界核实；没有启用支付/SMS开关。 |
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | 责任/中文/金额/先成功原则核实；回放其他主体的操作结果属于隔离与正确性缺陷，CX-B5-01。 |
| PGM-008 员工和手机 | 注册审批和真实凭证适配作为本模块职责，设备绑定具体实现在后续模块。 |
| PGM-022 空间和点位：所有业务都要知道“发生在哪” | 组织/商户/项目标识继续作为数据范围；不新增空间模型。 |
| PGM-024 影响业务 | 不含低消/容量/库存算法；仅承载既有访问权限。 |
| PGM-026 主要功能 | 组织与商户来源由认证及目标记录决定；不开放财税主体普通开关。 |
| PGM-032 员工和手机：人、私人手机、工作手机要能对应起来 | 真实账号/审批状态进入有效权限计算；不以账号记录替代整套设备治理。 |
| PGM-033 主要功能 | 员工账号由对应注册审批产生；不跳过注册审批。 |
| PGM-034 工作手机和企业微信 | 权限服务不保存/清理企微原文，没有扩展原文访问入口。 |
| PGM-035 提成发放和人员服务治理 | 不新增提成现金发放、营销打卡、人员服务商品。 |
| PGM-036 设备管理：谁领了什么设备，设备能不能用 | 权限计算与设备闸职责分离；直接ActionGateAspect中capability检查不重复消耗permission。 |
| PGM-048 商户侧：在授权业务范围内全开放 | 商户范围明确；本次反例为无merchant的不同公司组织，商户m作用域并不覆盖该缺口。 |
| PGM-059 客人与员工的个人信息保护 | 手机号摘要返回已删，DTO个人信息隐藏；储值现场按本人手机号专用链，不通过角色ID反查客人。 |
| PGM-066 已拍板规则和待排期问题 | 中文、审计、已有单据时间与第三方预留边界保留；不把未接通的支付/SMS声称为已上线。 |

## 技能与运行边界

按当前已读取的using-superpowers、grilling/diagnosing-bugs/systematic-debugging结构执行事实追踪与反例；按verification-before-completion实际读本次输出再作结论。沿用此前本轮实际技能读取证据，本次又实际读取using-superpowers及其Codex适配说明、verification-before-completion；不声称再次运行全部技能。controller§6覆盖技能本体的子代理/额外文档/即时修复指示。接收Grok前仍须按receiving-code-review收窄执行。

## 命令与原始输出

### 当前安全回归及跨组织反例

```powershell
@'
# V0.01 temporary B5 read-only verification
from pathlib import Path
import subprocess,tempfile
probe="\n    @Test void b5CompanyRequestsCannotReplayAnotherOrganizationsRole() {\n        var idem = new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper());\n        idem.setCallerScopeResolver(authorization);\n        service = new AccessService(repository, idem, new AuditEventService(new InMemoryAuditEventRepository()),\n                authorization, credentials, null, true, 3, 3);\n        when(repository.listPermissions()).thenReturn(List.of(\n                new PermissionDefinition(LOW, \"查看\", \"ops\", \"medium\", false, \"active\"),\n                new PermissionDefinition(\"access.role.manage\", \"管理角色\", \"access\", \"medium\", false, \"active\")));\n        for(String id: List.of(\"manager-a\", \"manager-b\")) {\n            effect(id, LOW, AccessRepository.Effect.GRANT, id+\"-read\", false);\n            effect(id, \"access.role.manage\", AccessRepository.Effect.GRANT, id+\"-manage\", false);\n        }\n        var controller = new AccessController(service, authorization);\n        var request = new RoleRequest(\"company-first-role\", \"正常首次创建\", List.of(), \"巡检员\", \"只读\", List.of(LOW), \"project\");\n        actor(\"manager-a\", \"staff\", \"org-a\", null);\n        String scopeA = authorization.resolve();\n        controller.createRole(request);\n        actor(\"manager-b\", \"staff\", \"org-b\", null);\n        String scopeB = authorization.resolve();\n        var result = controller.createRole(request);\n        System.out.println(\"B5_PROBE scope_a=\"+scopeA+\" scope_b=\"+scopeB+\" roles_written=\"+roles.size()+\" response=\"+result);\n        assertThat(roles.values()).extracting(RoleRecord::organizationId).contains(\"org-b\");\n    }\n"
runner="""package com.complex.platform.access;
public class M006Runner {
 public static void main(String[] args)throws Exception {
 int passed=0,failed=0;
 var c=M006SecurityRegressionTests.class;
 for(var m:c.getDeclaredMethods())if(m.isAnnotationPresent(org.junit.jupiter.api.Test.class)){
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
 sources += [base/"common/idempotency/IdempotencyService.java",base/"governance/review/SupervisorReviewService.java",base/"evidence/AuditEventService.java"]
 sources += [Path("backend/src/test/java/com/complex/platform/support/InMemoryAuditEventRepository.java"),Path("backend/src/test/java/com/complex/platform/support/InMemoryIdempotencyRepository.java"),test,src]
 r=subprocess.run(["javac","-encoding","UTF-8","--release","21","-cp",cp,"-d",str(out),*[str(p) for p in sources]],capture_output=True,text=True)
 print("JAVAC_EXIT",r.returncode,"SOURCES",len(sources));print(r.stdout,r.stderr)
 if r.returncode:raise SystemExit(r.returncode)
 r=subprocess.run(["java","-cp",str(out)+":"+cp,"com.complex.platform.access.M006Runner"],capture_output=True,text=True)
 print(r.stdout,r.stderr);raise SystemExit(r.returncode)

'@ | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -'
```

exit_code=1

```text
JAVAC_EXIT 0 SOURCES 49
 
PASS administratorDenyWinsAndHighPermissionNeedsExplicitGrant
PASS administratorKeepsDailyPermissionsWithoutAutomaticHighRole
PASS untrustedPrincipalIsRejected
PASS missingOrganizationCannotListAllUsersOrResolveGlobalProject
PASS organizationReadWorksButCrossMerchantWriteDoesNot
PASS collidingUserSlugsRemainDifferentUsers
PASS templateEditCannotGrantAnUnownedHighPermission
PASS templateEditPreservesOwnerAndRejectsMissingTemplate
PASS emptyPermissionsAndInvertedWindowAreRejectedBeforeGrantWrite
PASS invalidStatusAndBlankPasswordHaveNoWrites
PASS manualAccountDoesNotPersistOrReturnPhoneDigest
PASS administratorWithoutApprovalRoleHasNoSupervisorLevel
PASS twoPermissionReviewConsumesOneSourceOnlyAfterBothChecksPass
PASS credentialAdapterDisablesInvitedAccountsAndRejectsBlankPassword
PASS developmentCredentialAdapterRejectsMissingAccount
PASS changedRoleCannotBeAppliedUnderAnOldApproval
PASS beneficiaryCannotApproveOwnGrant
PASS positionChangeDropsDerivedRoleButKeepsIndependentAssignment
PASS appliedTemplateCannotExtendTimeOrRemoveSingleUse
PASS pendingTemporaryGrantRejectsInvertedTimeWindow
PASS topAuthorityReviewRequiresMasterSessionRatherThanRankAlone
PASS manualEmployeeCreationCannotIssueCredentialsBeforeRegistrationApproval
PASS accountJsonDoesNotExposeStoredPhoneDigest
PASS missingCredentialAdapterCannotReportSuccessfulCreation
PASS employeeApprovalWithoutCredentialAdapterWritesNoAccount
PASS registrationCreatesAccountOnlyAfterCompanyApproval
PASS summaryQueryCannotOverrideBusinessThresholds
PASS referenceDictionaryDoesNotPretendItWasTruncatedAtFifty
PASS grantListHasADefaultTimeWindow
PASS directPositionTemplateDeleteCannotBypassArchiveAndApproval
PASS positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval
B5_PROBE scope_a=p scope_b=p roles_written=1 response=ApiResponse[success=true, code=OK, detailCode=null, message=success, data=RoleRecord[id=role_91975467-7699-47eb-9ff4-2303317a41c4, roleName=巡检员, description=只读, dataScope=project, status=active, createdAt=2026-09-06T03:36:41.190314779Z, permissionCodes=[ops.production_readiness.view], organizationId=null], requestId=3f4d5637-1063-4167-ad3c-7a97b6408820, serverTime=2026-09-06T03:36:41.193208769Z, timestamp=2026-09-06T03:36:41.193208769Z]
FAIL b5CompanyRequestsCannotReplayAnotherOrganizationsRole: java.lang.AssertionError: 
Expecting ArrayList:
  ["org-a"]
to contain:
  ["org-b"]
but could not find the following element(s):
  ["org-b"]

RESULT 31 PASS / 1 FAIL
 Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build as described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3
WARNING: A Java agent has been loaded dynamically (/home/james/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.10/byte-buddy-agent-1.18.10.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended


```

### 现有Access/Authorization完整测试

```powershell
@'
# V0.01 temporary B5 read-only verification
from pathlib import Path
import subprocess,tempfile
probe="\n    @Test void b5CompanyRequestsCannotReplayAnotherOrganizationsRole() {\n        var idem = new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper());\n        idem.setCallerScopeResolver(authorization);\n        service = new AccessService(repository, idem, new AuditEventService(new InMemoryAuditEventRepository()),\n                authorization, credentials, null, true, 3, 3);\n        when(repository.listPermissions()).thenReturn(List.of(\n                new PermissionDefinition(LOW, \"查看\", \"ops\", \"medium\", false, \"active\"),\n                new PermissionDefinition(\"access.role.manage\", \"管理角色\", \"access\", \"medium\", false, \"active\")));\n        for(String id: List.of(\"manager-a\", \"manager-b\")) {\n            effect(id, LOW, AccessRepository.Effect.GRANT, id+\"-read\", false);\n            effect(id, \"access.role.manage\", AccessRepository.Effect.GRANT, id+\"-manage\", false);\n        }\n        var controller = new AccessController(service, authorization);\n        var request = new RoleRequest(\"company-first-role\", \"正常首次创建\", List.of(), \"巡检员\", \"只读\", List.of(LOW), \"project\");\n        actor(\"manager-a\", \"staff\", \"org-a\", null);\n        String scopeA = authorization.resolve();\n        controller.createRole(request);\n        actor(\"manager-b\", \"staff\", \"org-b\", null);\n        String scopeB = authorization.resolve();\n        var result = controller.createRole(request);\n        System.out.println(\"B5_PROBE scope_a=\"+scopeA+\" scope_b=\"+scopeB+\" roles_written=\"+roles.size()+\" response=\"+result);\n        assertThat(roles.values()).extracting(RoleRecord::organizationId).contains(\"org-b\");\n    }\n"
runner="""package com.complex.platform.access;
public class M006Runner {
 public static void main(String[] args)throws Exception {
 int passed=0,failed=0;
 for(String name: args){var c=Class.forName(name);
 for(var m:c.getDeclaredMethods())if(m.isAnnotationPresent(org.junit.jupiter.api.Test.class)){
 var ctor=c.getDeclaredConstructor();ctor.setAccessible(true);var t=ctor.newInstance();
 try {for(var b:c.getDeclaredMethods())if(b.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)){b.setAccessible(true);b.invoke(t);}
 m.setAccessible(true);m.invoke(t);passed++;System.out.println("PASS "+m.getName());
 }catch(java.lang.reflect.InvocationTargetException e){failed++;System.out.println("FAIL "+m.getName()+": "+e.getCause());}
 finally{for(var a:c.getDeclaredMethods())if(a.isAnnotationPresent(org.junit.jupiter.api.AfterEach.class)){a.setAccessible(true);a.invoke(t);}}
 }
 } System.out.println("RESULT "+passed+" PASS / "+failed+" FAIL");if(failed>0)System.exit(1);
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
 sources += [base/"access/staff/StaffService.java",base/"common/idempotency/IdempotencyService.java",base/"governance/review/SupervisorReviewService.java",base/"evidence/AuditEventService.java"]
 sources += [Path("backend/src/test/java/com/complex/platform/support/InMemoryAuditEventRepository.java"),Path("backend/src/test/java/com/complex/platform/support/InMemoryIdempotencyRepository.java"),Path("backend/src/test/java/com/complex/platform/support/TestSecurityActors.java"),Path("backend/src/test/java/com/complex/platform/access/AccessServiceTests.java"),Path("backend/src/test/java/com/complex/platform/access/AuthorizationServiceTests.java"),src]
 r=subprocess.run(["javac","-encoding","UTF-8","--release","21","-cp",cp,"-d",str(out),*[str(p) for p in sources]],capture_output=True,text=True)
 print("JAVAC_EXIT",r.returncode,"SOURCES",len(sources));print(r.stdout,r.stderr)
 if r.returncode:raise SystemExit(r.returncode)
 r=subprocess.run(["java","-cp",str(out)+":"+cp,"com.complex.platform.access.M006Runner","com.complex.platform.access.AccessServiceTests","com.complex.platform.access.AuthorizationServiceTests"],capture_output=True,text=True)
 print(r.stdout,r.stderr);raise SystemExit(r.returncode)

'@ | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -'
```

exit_code=0

```text
JAVAC_EXIT 0 SOURCES 52
 Note: backend/src/main/java/com/complex/platform/access/staff/StaffService.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.

PASS copyPermissionTemplateKeepsSnapshotWhenSourceChangesAndWritesAuditEvent
PASS applyPermissionTemplateRequiresApprovalWhenTemplateHasHighRiskPermission
PASS applyPermissionTemplateAppliesToManyUsersAndWritesAuditEvent
PASS temporaryGrantCreateAndRevokeWriteAuditEvents
PASS createAccessUserWithPlatformAdminAccountTypeThrowsValidationError
PASS createAccessUserWithAllowedAccountTypeSucceeds
PASS platformAdminWithMerchantAffiliationCanCreateUserForAnyMerchant
PASS createAccessUserRequiresOrganizationScope
PASS createAccessUserStoresManualEvidenceAndProvisionsCredential
PASS merchantScopedCreatorCannotCreateUserForAnotherMerchant
PASS assignStaffLevelSetsLevelAndDefaultsCurrencyToCny
PASS assignStaffLevelRejectsUnknownLevel
PASS assignStaffLevelRejectsNonCnyCurrency
PASS assignStaffLevelRejectsCrossOrganization
PASS listStaffLevelsReturnsSeededLevels
PASS assignAndUnassignUserRoleUpdateUserRoles
PASS assignUserPositionAppliesMappedRoleAndTemplate
PASS assignUserRoleRejectsRolePermissionsActorDoesNotHold
PASS assignUserRoleRequiresApprovalWhenHighRiskGrantFlagIsEnabled
PASS assignUserPositionRequiresApprovalWhenMappedRoleOrTemplateIsHighRisk
PASS grantPolicyRejectsAnyUnclassifiedPermissionInTheRequestedRole
PASS grantRequestRejectsPermissionsThatDoNotRequireApproval
PASS approvedUserRoleGrantRequestUsesSupervisorReviewAndLandsAssignment
PASS approvedTemporaryGrantExpiresByEffectivePermissionRead
PASS auditorOutsideTemplatePermissionNeedsApprovedTemporaryGrantBeforeUse
PASS grantSummaryReportsFrequentTemporaryRequestsByPositionAndUser
PASS applyPermissionTemplateWithCrossOrgTargetUserThrowsForbidden
PASS revokeTemporaryGrantOfOtherOrgUserThrowsForbidden
PASS updatePermissionTemplateOfOtherOrgThrowsForbidden
PASS createTemporaryGrantRejectsGrantingPermissionActorDoesNotHold
PASS applyPlatformOwnedTemplateByOrgUserToSameOrgUserIsForbidden
PASS listAccessUsersFiltersToCurrentOrganization
PASS listAccessUsersPlatformAdminSeesAllOrganizations
PASS listAccessUsersFiltersToCurrentMerchantWhenMerchantScoped
PASS listGrantRequestsFiltersRowsStampedWithOtherProjects
PASS denyWinsWhenGrantAndDenyExistForSamePermission
PASS currentMerchantScopeReturnsEmptyForPlatformAdmin
PASS isPlatformAdminReturnsTrueForPlatformAdminActor
PASS expiredTemporaryTemplateGrantDoesNotAuthorize
PASS outsourcedVendorIsNotPlatformScopeAndRequiresProjectScope
PASS requirePermissionReturnsForbiddenPermissionDeniedWhenUserLacksPermission
PASS singleUseTemplateGrantIsConsumedAfterFirstSuccessfulAuthorization
PASS localSeedBridgeAuthorizesPlatformAdminButNotArbitraryStaffWithSmokeRole
PASS singleUsePermissionConcurrentConsumerIsDeniedWhenAtomicUpdateReturnsZeroRows
PASS isPlatformAdminReturnsFalseForStaffActor
PASS outsourcedFacilityTemporaryGrantAllowsAssignedWorkAndEquipmentReadButNotAccept
PASS outsourcedTemporaryGrantExpiresAndNoLongerAuthorizes
PASS outsourcedSecurityCleaningTemporaryGrantOnlyGrantsAssignedInspectionAndExceptionPermissions
PASS currentMerchantIdQuietlyReturnsEmptyForPlatformAdminEvenWhenSeedBoundMerchantId
PASS currentMerchantIdQuietlyReturnsMerchantIdForRealMerchantUser
PASS currentMerchantScopeReturnsMerchantIdForMerchantUser
PASS currentMerchantScopeThrowsForbiddenWhenMerchantIdIsAbsent
PASS effectivePermissionsRemovesDenyCoveredCodesFromGrantsAndListsDenies
PASS effectivePermissionsDoesNotGiveAdministratorAnUnboundedWildcard
PASS effectivePermissionsQueryDoesNotConsumeSingleUseGrant
PASS effectivePermissionsExcludesExpiredGrant
RESULT 56 PASS / 0 FAIL
 

```

### 当前源及权威完整性

```powershell
@'
from pathlib import Path
import subprocess,hashlib,json,re
root=Path("docs/audit-2026-09")
before="f49ef25cdadc0d4572f169d8c33b5e971c21f024";after="2e346ad1b72a5df60d8e10eb1df9cc2b1add2cf9"
names=subprocess.check_output(["git","diff","--name-only",before,after],text=True).splitlines()
names=[p for p in names if not p.startswith("docs/audit-2026-09/")]
bad=[]
for name in names:
 data=subprocess.check_output(["git","show",after+":"+name])
 if Path(name).read_bytes()!=data:bad.append(name)
print("CURRENT_SOURCE_PATHS",len(names),"MISMATCH",json.dumps(bad))
for name in ["docs/business-enum-quick-reference-v0.1.html","docs/business-system-map-for-operations-v0.1.html","docs/platform-governance-map-for-operations-v0.1.html"]:
 print("AUTHORITY_UNCHANGED",name,Path(name).read_bytes()==subprocess.check_output(["git","show","HEAD:"+name]))
print("CRITERIA_ACTUAL",hashlib.sha256((root/"plan/criteria-lock.md").read_bytes()).hexdigest())
print("CRITERIA_SIDECAR",(root/"plan/criteria-lock.sha256").read_text().strip())
print("\n".join((root/"ledger.md").read_text().splitlines()[:2]))
print("HEAD",subprocess.check_output(["git","rev-parse","HEAD"],text=True).strip())
print("FINDINGS_SHA",hashlib.sha256((root/"findings.md").read_bytes()).hexdigest())

'@ | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -'
```

exit_code=0

```text
CURRENT_SOURCE_PATHS 55 MISMATCH []
AUTHORITY_UNCHANGED docs/business-enum-quick-reference-v0.1.html True
AUTHORITY_UNCHANGED docs/business-system-map-for-operations-v0.1.html True
AUTHORITY_UNCHANGED docs/platform-governance-map-for-operations-v0.1.html True
CRITERIA_ACTUAL 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
CRITERIA_SIDECAR 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
LOCK-AMEND-COUNT: 0
HEAD 395e6fbab027020da1c38237fad17e8edcd7a132
FINDINGS_SHA 32665375af0b696a921c7fd3086e6a467b7f9a95144fbc2570ed2a15bf4db1d5

```

## 结论

本席整模块B5结论FAIL，全部临时finding共1项CX-B5-01，87条既有测试通过不覆盖该失败。两份完整报告落齐后统一处理；不得此时修复、出具模块PASS、发布或开启M007。无新增HTML申请、无未答新增问题。B3仍2，当前findings V0.60全30项队列否保持，本次若回退必须整批B2。

CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# 当前471计划 M006 B5-r2 Codex独立完整模块复审
版本：V0.02

结论：PASS。全量两文件206行、全部79条冻结判据已核，三线完成；无新临时finding、无待答questions、无本模块待修项目。此时M006仍IN_PROGRESS；只有读完Grok本次报告并完成B5四值后才可合取收口。

## 独立性、范围与当前指纹
Grok B5-r2先运行并真实exit0，适配器只返回指定报告路径；本席尚未读取本次Grok正文或日志。现在重新审完全部两文档及全部映射，先追加本席完整raw。旧36285bytes及V0.01保留，本段为当前471计划的本次完整报告，不以旧backend同号报告作审计结论。
范围：docs/delivery-readiness-gates-v0.1.md，V0.12，158行15751bytes，SHA0615cd1b3beaffc097f96cc6140b16bfa3ec0a6be4e91c493dca2f0fa6217e09；docs/deploy-and-change-flow-v0.1.md，V0.07，48行7117bytes，SHA543a1543ca264f0741706fb5007d813f67ea84dd3acaafe40c2380cce9babda1。冻结计划207为修前断行计量，不改计划或映射。
HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c。B3累计2。三HTMLSHA未变：BSM a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa；BEQR 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66；PGM c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c。全部61业务定义摘录现与真实HTML匹配，HTML_QUOTE_MISMATCHES=[]，另18GEN合79。
只通过SSH当前规范仓库审计；本步骤未写源/HTML/生成快照/开关/生产配置，未部署、提交、推送。既有已实读技能按V0.17复用grilling、diagnosing-bugs、receiving-code-review、verification-before-completion、writing-for-agents、ponytail-review问法；不派第三席，不新增判据。文档采用类型4，HTML分类采用类型1。

## 第一性原理
delivery存在用途是最低交付入口/证据边界与完整上线要求之间的操作说明；删除会丢失BSM116–119的交付义务与具体工程入口的联系，也会让机器消费标记缺失。deploy提供当前四道工程门禁的实际操作和限制；删除会丢失固定数组、PR、同SHA合并与仓外权限限制的说明。两文件都不代替经营模块实现。
本次重新核实际行为：6个相关命令真实exit0；67独立npm引用都存在；父链49项与“完整清单以package.json为准”吻合；真实Markdown表47行均两列且无空单元格；17个显式本地路径/通配引用全部存在。不是仅凭doc gate绿判正确。
未验证前提逐一落证：GitHub GET当前workflow active、保护403，最早run_number1确实success；当前已安装pre-push与仓内源完全一致；生产/运行env的非敏感URL/username与两个测试配置证明同host/账号而不同库。未输出密码、未连接生产库，不声称已经隔离。当前runtime生成器默认30天、源码指纹与超龄检查真实存在。

## 全文覆盖、顺从与对抗结果
| 文件/行 | 核查及分类 | 对抗结论 |
|---|---|---|
| delivery:1–7 | 细化：BSM116系统交付；最低准入不是完整回归/上线完成 | 明确定位，无假完成 |
| delivery:9–53 | 技术性：机器标记被既有verify消费，无业务实体/状态发明 | 不把READY标记当真实生产证据；保留消费者接口 |
| delivery:55–104 | 技术性及既有证据细化；全部命令引用、父链角色、表结构、实际check核实 | 039坏表/顺序/父链描述已修；040旧40/31已去；047的L96准确包含证据映射或关闭接入分支 |
| delivery:106–112 | 细化：第三方资料未定、工作手机纳管、当前AI只读建议 | 不把外部mock当真实控制；MDM/AI上位现读核实，未创设业务处罚或替代确定性自动规则 |
| delivery:114–126 | 细化：强密码、U盘托管留痕、自动环境安装包、业务语言教程 | 工程验证明确不能代替HTML既有生产要求，043保持有效 |
| delivery:128–139 | 技术性：失败定位、既有生成/verify入口和正式主机 | 没有跳过/掩盖错误的新策略，恢复入口存在；本审计按类型4不执行发布/生成 |
| delivery:141–158 | 细化+技术性：partial8条、mock验收报告、真实存储证据与指挥板入口 | 不关闭生产blocker；对象存储是已有留存载体，不新增保留期或供应商 |
| deploy:1–10 | 技术性：固定15项部署数组、实装hook、CI、同SHA合并 | 无将本地可绕过规则冒充仓外强制；042描述保持准确 |
| deploy:12–24 | 技术性：feature/PR/独立模型复核与可绕过限制 | 相关脚本主路径现读，SKIP_CI_WAIT/ALLOW_MAIN_PUSH限制明示 |
| deploy:26–31 | 技术性：实际workflow与镜像、fetch-depth、事件base和HEAD~1说明 | 两YAML字节一致；取不到基线须失败的文档/直接消费一致 |
| deploy:33–48 | 技术性：当前状态、gh类型、仓外权限/DB隔离待办 | active/403/首跑success实证；041的-F bool/int/null保持，未发真实PUT；账号隔离未宣称完成 |

GEN017按冲突→细化→技术性→添加逐段核：无与三HTML明文冲突，无待询添加。044 MDM仍细化：PGM619–620企业级手机统一管控；045对象存储仍细化：PGM590–592/958永久留存，BEQR HTML164与BSM1615留证/留存（HTML行号不是同号锁ID）；046当前AI建议仍细化：PGM532待人工确认，本文限制当前大模型直接业务副作用，不推翻已有非AI确定性自动规则。三句未改，未因缩写未出现而判添加。
广泛映射的游乐/财务/地产/楼宇等业务在本模块只需核不冲突/不创设规则，两md没有其状态机、金额计算或API；不把没有在交付说明里重复全套业务要求判为遗漏。逐条边界见下表。

## 047反例的复验及已修项目
原问题是8/8 traceable不等于8项都有manifest责任/路径。当前真实JSON的requirement_traceability恰8行，mapped/owners/paths全空、disabled非空；policy明确owner_manifest_or_disabled_switch，mock不等于生产证据。L96现在用“已追踪到…路径，或已记录对应接入开关关闭”，当前正常数据落入第二支，不再被“均已”虚称为第一支。后半防伪生产边界及全部标记保留。相关源码238/246/267刚在本轮修复复审实际核过且未改；本次又实跑--check/完整读当前快照，未用生成器散文代替实际行为。
039–043与047均已修且不在当前队列；037/038/044–046只登记不修。此次全量重新核，不用B4 PASS直接决定B5。无新CX项。
正负ready夹具当前实跑：15份模拟证据有效但external_partial=9时ready=false；全部模拟条件满足时ready=true。9为该夹具消费者计数，traceability文档8对应自己的快照，不混称同一计数。夹具只写自建临时目录，未将模拟数据写入真实submissions。

## 验证范围
当前6次实跑：delivery-gates-docs、production-evidence-p0-quickstart、partial-requirement-mock-coverage、external-evidence-requirement-traceability、production-ready-no-go-fixtures、production-ready-positive-fixtures，输出附后。
真实GitHub只读GET：workflow active；branch protection403明确Upgrade/公开；989条历史run最早id28778061141/run_number1/2026-07-06T08:24:42Z/pull_request/success。当前hook .git/hooks/pre-push与ops/160/hooks/pre-push字节相同，SHA46aabdf33cb3496d83a71e4357ca85ba2ed9dbbfd4582ab53747bc50bb748d6b。
重新全文读merge-pr/propose-change、两部署数组和循环；15项红即exit1在build前；merge等待后重核远端/PR head、--match-head-commit；propose先predeploy。gh示例仍保持B4实际验证的类型化参数，本次未重新发loopback/PUT。
路径抽取辅助正则第一次因Markdown代码围栏得到空集，没有把该空集当通过证据；改为按明确仓库路径前缀提取，实际17项全部存在，最终结果附后。命令引用/表结构用独立非空计数证明。
未跑49项交付父链或后端/Web/Android完整构建、未审后续M414/M425/M427整模块，不宣称生产可上线或全仓已绿。当前工程文档范围内无未验证完成声明。保存本席完整raw后才读取Grok本次完整报告；B5四值尚待双报告收齐后核对。


## 79项冻结判据逐条覆盖

| 判据 | 本模块核查/作用边界 | 结果 |
|---|---|---|
| BEQR-085 — 项目开放状态 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-086 — 场次状态 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-087 — 游乐「当天／每天」口径 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-088 — 容量双阈值 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-089 — 园区容量 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-090 — 设备关键/辅助分级 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-091 — 全园故障与补偿通票 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-092 — 安全严重度 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-093 — 巡检任务状态 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-094 — 越界类型 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-095 — 团体/包场 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-096 — 游乐增强阶段 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-118 — 运营营业日 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-146 — 运营公司职责 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-155 — 商户/运营主体状态 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-160 — 招商线索状态 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-161 — 招商意向阶段 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-162 — 品牌/客户库类型 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-163 — 合同变更类型 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-164 — 退租结算状态 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-165 — 内部应付执行状态 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-166 — 合同审批链 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-167 — 租金/账单类型 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-168 — 营业额抽成模式 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-169 — 公区能源费 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-170 — 铺位租赁状态 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-171 — 物业工单类型 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-172 — 物业工单状态 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-173 — 商户服务工单状态 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-174 — 工单时效 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-175 — 楼宇机电设备状态 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-176 — 设备维保类型 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-177 — 能耗表计类型 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-178 — 抄表状态 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-179 — 能耗归集对象 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-180 — 楼宇巡检任务状态 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BEQR-181 — 楼宇巡检类型 | 全文定义及真实HTML匹配；两md没有该业务枚举/状态/收费/审批的实现或改写。提到核销/退款仅当前AI副作用边界，不创设业务规则。 | PASS |
| BSM-020 — 楼宇运营 | 完整业务定义与HTML已核；两md不实现经营指标、基础权责或楼宇状态；工程门禁不修改该规则，也不宣称业务已完成。 | PASS |
| BSM-028 — 主要功能 | 完整业务定义与HTML已核；两md不实现经营指标、基础权责或楼宇状态；工程门禁不修改该规则，也不宣称业务已完成。 | PASS |
| BSM-081 — 楼宇运营：机电设备、能耗抄表、楼宇巡检 | 完整业务定义与HTML已核；两md不实现经营指标、基础权责或楼宇状态；工程门禁不修改该规则，也不宣称业务已完成。 | PASS |
| BSM-082 — 主要功能 | 完整业务定义与HTML已核；两md不实现经营指标、基础权责或楼宇状态；工程门禁不修改该规则，也不宣称业务已完成。 | PASS |
| BSM-083 — 和谁有关 | 完整业务定义与HTML已核；两md不实现经营指标、基础权责或楼宇状态；工程门禁不修改该规则，也不宣称业务已完成。 | PASS |
| BSM-111 — 经营看板：让管理层和运营看得懂、能追问 | 完整业务定义与HTML已核；两md不实现经营指标、基础权责或楼宇状态；工程门禁不修改该规则，也不宣称业务已完成。 | PASS |
| BSM-112 — 主要指标 | 完整业务定义与HTML已核；两md不实现经营指标、基础权责或楼宇状态；工程门禁不修改该规则，也不宣称业务已完成。 | PASS |
| BSM-113 — 谁会看 | 完整业务定义与HTML已核；两md不实现经营指标、基础权责或楼宇状态；工程门禁不修改该规则，也不宣称业务已完成。 | PASS |
| BSM-114 — 各类入口：不同人只看自己该看的、只做自己该做的 | 完整业务定义与HTML已核；两md不实现经营指标、基础权责或楼宇状态；工程门禁不修改该规则，也不宣称业务已完成。 | PASS |
| BSM-115 — 相互关系：为什么这些子系统必须连在一起 | 工作手机管控/留痕的交付证据上位已核；文档不重定义其它跨域连接或资金规则。 | PASS |
| BSM-116 — 系统交付与上线：正式上线要做的安全加固和安装交付 | delivery116明确引用系统交付、强密码/U盘留痕、自动识别环境安装包/业务教程；工程验证不能代替，未声称已交付。 | PASS |
| BSM-117 — 正式上线时的安全加固 | delivery116明确引用系统交付、强密码/U盘留痕、自动识别环境安装包/业务教程；工程验证不能代替，未声称已交付。 | PASS |
| BSM-118 — 能自动识别环境的安装包 + 内附教程 | delivery116明确引用系统交付、强密码/U盘留痕、自动识别环境安装包/业务教程；工程验证不能代替，未声称已交付。 | PASS |
| BSM-119 — 和谁有关 | delivery116明确引用系统交付、强密码/U盘留痕、自动识别环境安装包/业务教程；工程验证不能代替，未声称已交付。 | PASS |
| GEN-001 — 必要性与最小性 | 两文档用途与删除代价明确；命令索引/现有消费者需要，无新增抽象。 | PASS |
| GEN-002 — 可执行正确性 | 67命令存在、实际6校验绿；L96正常关闭分支描述已准确；部署/合并主路径核。 | PASS |
| GEN-003 — 测试有效性 | 说明性Markdown采用类型4；实际ready正负夹具具能失败断言。 | PASS |
| GEN-004 — 信任边界校验 | 无新入站解析实现；gh示例类型保持正确，不执行生产写。 | PASS |
| GEN-005 — 授权与最小权限 | hook/CI权限局限明示；实装hook存在，不冒称仓外强制。 | PASS |
| GEN-006 — 数据完整性 | 无业务事务/金额写路径，未创设资金守恒例外。 | PASS |
| GEN-007 — 并发与幂等 | 同SHA合并说明核远端与PR head及match-head-commit；无新并发业务实现。 | PASS |
| GEN-008 — 接口与模式一致性 | 命令/路径/workflow镜像及gh类型一致，无新DTO/枚举。 | PASS |
| GEN-009 — 错误处理与恢复 | 红闸即exit1、失败先定位，文档未建议掩盖失败。 | PASS |
| GEN-010 — 安全与敏感信息 | 无真实秘密输出；只读GET/非敏感配置比较；正文不引入明文密钥。 | PASS |
| GEN-011 — 可观测与审计证据 | 实际SHA/版本/当前API/标记消费可核；生产证据未伪完成。 | PASS |
| GEN-012 — 资源与性能边界 | 本模块无资源循环/运行时批处理实现；最低准入与完整构建边界清楚。 | PASS |
| GEN-013 — 构建配置与依赖 | 67脚本、49父链、15部署数组、有效YAML/安装hook/DB配置可复核。 | PASS |
| GEN-014 — 数据库迁移正确性 | 无迁移定义或数据库操作；不增加不可回滚迁移。 | PASS |
| GEN-015 — 界面可用性与可访问性 | 47表行两列/无空格单元，面向工程文档不是业务终端UI；无旧混语残留。 | PASS |
| GEN-016 — 文档可验证性 | 两源全文、17路径、67命令、30天/8条/真实GitHub状态已核；不把B4/历史结论当当前PASS。 | PASS |
| GEN-017 — 业务来源分类 | 四分类完整；MDM/存储/AI/接入开关有HTML上位，无待询添加。 | PASS |
| GEN-018 — 第一性原理证据 | 基于HTML原文、实际消费者/数据与本次可复现输出，无锁外判据。 | PASS |
| PGM-015 — 楼宇运营 | 完整治理/楼宇/主体定义已核；本模块只做工程交付文档，不创建或覆盖主体拓扑、商户权责、能耗收费、巡检规则。 | PASS |
| PGM-018 — 物理项目与运营公司：多对多协作，不是固定隶属层级 | 完整治理/楼宇/主体定义已核；本模块只做工程交付文档，不创建或覆盖主体拓扑、商户权责、能耗收费、巡检规则。 | PASS |
| PGM-026 — 主要功能 | 完整治理/楼宇/主体定义已核；本模块只做工程交付文档，不创建或覆盖主体拓扑、商户权责、能耗收费、巡检规则。 | PASS |
| PGM-039 — 智能设备场景联动范围 | 第三方待定/开关关闭与设备动作后置、当前AI只读边界核；L96两分支说明无虚称生产证据，未另设业务。 | PASS |
| PGM-052 — 楼宇运营：管好建筑本身的设备、能耗和巡检 | 完整治理/楼宇/主体定义已核；本模块只做工程交付文档，不创建或覆盖主体拓扑、商户权责、能耗收费、巡检规则。 | PASS |
| PGM-053 — 楼宇机电设备 | 完整治理/楼宇/主体定义已核；本模块只做工程交付文档，不创建或覆盖主体拓扑、商户权责、能耗收费、巡检规则。 | PASS |
| PGM-054 — 能耗计量 | 完整治理/楼宇/主体定义已核；本模块只做工程交付文档，不创建或覆盖主体拓扑、商户权责、能耗收费、巡检规则。 | PASS |
| PGM-055 — 楼宇巡检 | 完整治理/楼宇/主体定义已核；本模块只做工程交付文档，不创建或覆盖主体拓扑、商户权责、能耗收费、巡检规则。 | PASS |
| PGM-056 — 和谁有关 | 完整治理/楼宇/主体定义已核；本模块只做工程交付文档，不创建或覆盖主体拓扑、商户权责、能耗收费、巡检规则。 | PASS |
| PGM-066 — 已拍板规则和待排期问题 | 第三方待定/开关关闭与设备动作后置、当前AI只读边界核；L96两分支说明无虚称生产证据，未另设业务。 | PASS |

## 本次真实命令与配置证据
````text
CMD npm run verify:delivery-gates-docs EXIT 0

> entertainment-complex-management@0.1.0 verify:delivery-gates-docs
> node tools/verify-delivery-gates.mjs

DELIVERY_GATES_DOCS_PASS
 
CMD npm run verify:production-evidence-p0-quickstart EXIT 0

> entertainment-complex-management@0.1.0 verify:production-evidence-p0-quickstart
> node tools/generate-production-evidence-p0-quickstart.mjs --check

PRODUCTION_EVIDENCE_P0_QUICKSTART_PASS
 
CMD npm run verify:partial-requirement-mock-coverage EXIT 0

> entertainment-complex-management@0.1.0 verify:partial-requirement-mock-coverage
> node tools/generate-partial-requirement-mock-coverage.mjs --check

PARTIAL_REQUIREMENT_MOCK_COVERAGE_PASS
 
CMD npm run verify:external-evidence-requirement-traceability EXIT 0

> entertainment-complex-management@0.1.0 verify:external-evidence-requirement-traceability
> node tools/generate-external-evidence-requirement-traceability.mjs --check

EXTERNAL_EVIDENCE_REQUIREMENT_TRACEABILITY_PASS
 
CMD npm run verify:production-ready-no-go-fixtures EXIT 0

> entertainment-complex-management@0.1.0 verify:production-ready-no-go-fixtures
> node tools/verify-production-ready-no-go-fixtures.mjs

PRODUCTION_READY_NO_GO_FIXTURES_READY: valid=15, external_partial=9, production_ready=false
 
CMD npm run verify:production-ready-positive-fixtures EXIT 0

> entertainment-complex-management@0.1.0 verify:production-ready-positive-fixtures
> node tools/verify-production-ready-positive-fixtures.mjs

PRODUCTION_READY_POSITIVE_FIXTURES_READY: valid=15, blockers=0, production_ready=true
 
NPM_REFS 67 MISSING []
PARENT_CHAIN 49 ['verify:delivery-gates-docs', 'verify:full-admin-matrix', 'verify:domain-ownership-scope', 'verify:matrix-vs-migration', 'verify:migration-version-order', 'verify:data-isolation-spec', 'verify:tenant-isolation', 'verify:unit', 'verify:coding-host-protection', 'verify:external-dependency-gates', 'verify:integration-capability-switches', 'verify:external-evidence-requirement-traceability', 'verify:partial-requirement-mock-coverage', 'verify:t3-coding-package', 'verify:t3b-supplier-mock-kit', 'verify:t3d-local-llm-ai-quality', 'verify:p0-contract', 'verify:security-baseline', 'verify:final-closed-loop-audit', 'verify:closed-loop-current-state', 'verify:runtime-acceptance-evidence', 'verify:ui-experience-acceptance', 'verify:production-readiness-evidence', 'verify:nas-current-state-drift', 'verify:nas-diagnostics', 'verify:nas-it-handoff', 'verify:production-blocker-register-no-go-fixtures', 'verify:production-blockers', 'verify:production-closure-gap-reports', 'verify:production-evidence-backfill-execution-ledger', 'verify:production-evidence-backfill-priority', 'verify:production-evidence-crosslinks', 'verify:production-evidence-draft-prep', 'verify:production-evidence-draft-prep-fixtures', 'verify:production-evidence-intake', 'verify:production-evidence-manifest-templates', 'verify:production-evidence-negative-fixtures', 'verify:production-evidence-owner-execution-packets', 'verify:production-evidence-owner-guides', 'verify:production-evidence-p0-quickstart', 'verify:production-evidence-positive-fixtures', 'verify:production-evidence-positive-propagation-fixtures', 'verify:production-evidence-submission-kit', 'verify:production-evidence-submissions-guard', 'verify:production-evidence-submission-status', 'verify:production-no-go-command-center', 'verify:production-owner-actions', 'verify:production-ready-no-go-fixtures', 'verify:production-ready-positive-fixtures']
TABLE_ROWS 47 WIDTHS [2] EMPTY 0
DEPENDENCY_SHA .github/workflows/predeploy-gates.yml 1335 4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d
# 文件版本：V0.02
name: predeploy-gates
# 独立治理门禁(GitHub 侧,与 .160 部署机隔离):PR 到 main 与 push 到 main 时静态跑
# delivery-gates 快子集(scope 登记/安全基线/后台矩阵/契约/阻断台账),catch 治理漂移。
# 无需数据库(全静态分析)。因私有库免费版无法设 required status check,此为可见信号 + 独立复现,
# 部署前硬阻断由 .160 部署脚本执行治理门禁；本地 pre-push 仅拦截直接推送 main。
on:
  pull_request:
    branches: [main]
  push:
    branches: [main]
jobs:
  predeploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5 # v4.3.1 (AUD-068：固定完整 commit SHA，防可变标签漂移)
        with:
          fetch-depth: 0  # 门禁需完整历史；取不到基线时必须失败
      - uses: actions/setup-node@49933ea5288caeca8642d1e84afbd3f7d6820020 # v4.4.0 (AUD-068：固定完整 commit SHA，防可变标签漂移)
        with:
          node-version: "22"
      - name: install deps
        run: npm ci --ignore-scripts
      - name: verify:predeploy (governance gates)
        env:
          HTML_GATES_BASE: ${{ github.event_name == 'pull_request' && github.event.pull_request.base.sha || github.event.before }}
        run: npm run verify:predeploy

DEPENDENCY_SHA ops/ci/predeploy-gates.yml 1335 4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d
# 文件版本：V0.02
name: predeploy-gates
# 独立治理门禁(GitHub 侧,与 .160 部署机隔离):PR 到 main 与 push 到 main 时静态跑
# delivery-gates 快子集(scope 登记/安全基线/后台矩阵/契约/阻断台账),catch 治理漂移。
# 无需数据库(全静态分析)。因私有库免费版无法设 required status check,此为可见信号 + 独立复现,
# 部署前硬阻断由 .160 部署脚本执行治理门禁；本地 pre-push 仅拦截直接推送 main。
on:
  pull_request:
    branches: [main]
  push:
    branches: [main]
jobs:
  predeploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5 # v4.3.1 (AUD-068：固定完整 commit SHA，防可变标签漂移)
        with:
          fetch-depth: 0  # 门禁需完整历史；取不到基线时必须失败
      - uses: actions/setup-node@49933ea5288caeca8642d1e84afbd3f7d6820020 # v4.4.0 (AUD-068：固定完整 commit SHA，防可变标签漂移)
        with:
          node-version: "22"
      - name: install deps
        run: npm ci --ignore-scripts
      - name: verify:predeploy (governance gates)
        env:
          HTML_GATES_BASE: ${{ github.event_name == 'pull_request' && github.event.pull_request.base.sha || github.event.before }}
        run: npm run verify:predeploy

DEPENDENCY_SHA ops/160/complex-deploy-api.sh 9194 8bd3cd0c514257531315b1efee6b137e0fe0b6dba3e7bca41e98ad7a53fd815b
DEPENDENCY_SHA ops/160/complex-deploy-web.sh 2853 5549661743a48cf724e66ea3d0d8d656a5f2eaa7f2642bdbad31c665d02828c1
DEPENDENCY_SHA ops/160/merge-pr.sh 4199 8688f7dc7e9b688c2d8583ff1942137089df55dbb9e5cfda95f69f344edbd514
DEPENDENCY_SHA ops/160/propose-change.sh 1437 913a12cae30d4448ac546d56ed54c9b25e7687c966a3181bd9324ebd194865e1
READONLY_GET repos/jxh1979/mall/actions/workflows/predeploy-gates.yml EXIT 0
{"state": "active", "created_at": "2026-07-06T01:24:42.000-07:00"}
READONLY_GET repos/jxh1979/mall/branches/main/protection EXIT 1
{"message": "Upgrade to GitHub Pro or make this repository public to enable this feature.", "status": "403"}
RUN_COUNT_GET_EXIT 0
TOTAL_RUNS 989
EARLIEST_RUN_GET_EXIT 0
[{"id": 28778061141, "run_number": 1, "created_at": "2026-07-06T08:24:42Z", "conclusion": "success", "event": "pull_request"}]
TRACE_COUNTS {"partial_requirements": 8, "traceable_partial_requirements": 8, "mapped_evidence_items": 0, "mapped_ledger_rows": 0, "p0_evidence_rows": 0, "awaiting_evidence_rows": 0, "invalid_evidence_rows": 0, "valid_evidence_rows": 0, "production_blocking_requirements": 0}
TRACE_POLICY {"no_fake_external_evidence_closure": true, "mock_or_sandbox_is_not_production_evidence": true, "partial_requirement_must_map_to_owner_manifest_or_disabled_switch": true, "integration_capability_switches_applied": true, "raw_evidence_stays_external": true, "credential_ref_only_no_plaintext_secret": true, "this_report_does_not_close_blockers": true}
TRACE_ROWS [{"requirement_id": "REQ-17", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-PAY-001", "PRD-PAY-002", "PRD-FACE-001", "PRD-CRED-3P-001", "PRD-CRED-OBJ-001"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-21", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-PAY-001", "PRD-PAY-002"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-23b", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-FACE-001", "PRD-CRED-3P-001"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-25", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-PAY-001", "PRD-PAY-002"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-32", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-CRED-OBJ-001", "PRD-3P-001", "PRD-CRED-3P-001", "SEC-P1-WEBHOOK-REPLAY-001"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-40", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-3P-001", "PRD-CRED-OPEN-001", "PRD-CRED-3P-001", "SEC-P1-WEBHOOK-REPLAY-001"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-41", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-3P-001", "PRD-CRED-3P-001", "SEC-P1-WEBHOOK-REPLAY-001"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-44", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-3P-001", "PRD-CRED-3P-001", "SEC-P1-WEBHOOK-REPLAY-001", "PRD-MDM-001", "SEC-P0-MDM-DEVICE-001"], "production_trace_status": "covered_by_disabled_integration_switches"}]
HEAD 242f487233d028173cb9198eecd079f5d877405c
242f487233d028173cb9198eecd079f5d877405c


ACTIVE_HOOK_PATH /home/james/complex-management/.git/hooks/pre-push EXISTS True
ACTIVE_HOOK_SHA 46aabdf33cb3496d83a71e4357ca85ba2ed9dbbfd4582ab53747bc50bb748d6b TRACKED_MATCH True
DB_NON_SECRET_COMPARISON /etc/complex/complex.env URL_IS_LOCAL_COMPLEX True USERNAME_IS_COMPLEX True
DB_NON_SECRET_COMPARISON /home/james/.complex/complex.env URL_IS_LOCAL_COMPLEX True USERNAME_IS_COMPLEX True
TEST_FILES_USING_COMPLEX_TEST 109
TEST_DB_CONFIG backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java
21:        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
TEST_DB_CONFIG backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java
31:        assumeTrue(url != null && url.matches("jdbc:postgresql:.*?/complex_test(?:\\?.*)?"));
32:        connection = DriverManager.getConnection(url, System.getenv("COMPLEX_DATABASE_USERNAME"),
TEST_DB_CONFIG backend/src/test/java/com/complex/platform/access/staff/PayrollSafetyJdbcIntegrationTests.java
32:        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
RUNTIME_CONSUMER 33 const MAX_AGE_DAYS = Number(process.env.RUNTIME_EVIDENCE_MAX_AGE_DAYS ?? 30);
RUNTIME_CONSUMER 88     source_fingerprint: source.fingerprint,
RUNTIME_CONSUMER 89     source_fingerprint_file_count: source.file_count,
RUNTIME_CONSUMER 90     source_fingerprint_paths: SOURCE_FINGERPRINT_PATHS,
RUNTIME_CONSUMER 91     max_age_days: MAX_AGE_DAYS,
RUNTIME_CONSUMER 334 | 源码指纹 sha256 | \`${state.git.source_fingerprint}\` |
RUNTIME_CONSUMER 335 | 指纹覆盖文件数 | ${state.git.source_fingerprint_file_count} |
RUNTIME_CONSUMER 435       if (!git.source_fingerprint) {
RUNTIME_CONSUMER 436         failures.push(`${jsonPath} must record git.source_fingerprint`);
RUNTIME_CONSUMER 439         if (git.source_fingerprint !== current) {
RUNTIME_CONSUMER 442               `(evidence fingerprint ${git.source_fingerprint.slice(0, 12)}… != current ${current.slice(0, 12)}…). ` +
RUNTIME_CONSUMER 453       if (ageDays > MAX_AGE_DAYS) {
RUNTIME_CONSUMER 456             `RUNTIME_EVIDENCE_MAX_AGE_DAYS=${MAX_AGE_DAYS}. Rerun the closed-loop runtime command and regenerate.`,

NON_SECRET_TEST_USERNAME backend/src/test/java/com/complex/platform/access/AccessJdbcIntegrationTests.java 22 "spring.datasource.username=complex",
NON_SECRET_TEST_USERNAME backend/src/test/java/com/complex/platform/access/staff/PayrollSafetyJdbcIntegrationTests.java 33 "spring.datasource.username=complex",

EXPLICIT_PATH_COUNT 17
[[".github/CODEOWNERS", true], [".github/workflows/predeploy-gates.yml", true], ["docs/nas-current-state-drift-report-v0.1.md", true], ["docs/nas-diagnostic-report-v0.1.md", true], ["docs/nas-it-controlled-mount-handoff-v0.1.md", true], ["docs/partial-requirement-mock-coverage-latest.json", true], ["docs/partial-requirement-mock-coverage-v0.1.md", true], ["docs/production-readiness-evidence/external-evidence-requirement-traceability-latest.json", true], ["docs/requirements-code-implementation-matrix-v0.1.csv", true], ["ops/160/complex-deploy-*.sh", true], ["ops/160/complex-nas-system-mount.example.sh", true], ["ops/160/merge-pr.sh", true], ["ops/160/propose-change.sh", true], ["ops/ci/predeploy-gates.yml", true], ["tools/gate-hardening-tests/m001-regression.mjs", true], ["tools/generate-partial-requirement-mock-coverage.mjs", true], ["tools/verify-*.mjs", true]]

````
