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
