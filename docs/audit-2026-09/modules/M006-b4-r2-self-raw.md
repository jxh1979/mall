# M006 B4-r2 本席完整修复内容复审
版本：V0.01

审计结论：PASS
越界判定：PASS
CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

控制V0.17；M006/322；B3已完成2次；生命周期IN_PROGRESS。先派Grok（2026-09-05 19:48:05 UTC，session57687），本方随后独立逐字读完本轮5路径12hunk及直接调用、旧来源事实，形成此完整报告。本报告写完前未读取本轮Grok报告、进程原始JSON或其正文。此为本席票，不能替代Grok；两席完整报告齐后统一合取。本节点没有修复代码。

## 全部输入与归因边界

before=7fecb8e739b6b04475755fffbb8e106fa0759713；after=2e346ad1b72a5df60d8e10eb1df9cc2b1add2cf9。
完整未过滤git diff --binary：17644字节、SHA256=cc2fc0f92106bd49386038da79b4b1e1b862cf447bc6bc753322417efd413a41。tree涵盖tracked/staged/untracked，未按路径过滤。实际暂存区SHA=824f33c20ee9b3c04744aadfede5f7bdbb2303a35b0035b1f8496ad729cd0322前后相同。
findings V0.59，225477字节，SHA30902ffb71d71f6f3c42e7f944da490cff5eecc95f589ac2f4556d33db815f89；沿用B2双PASS定稿25项，019—023仍否。r1两席全部依据已归并，3项FAIL均为009/026/027在队列旧ID，整批直接回B3合法。本轮不分配新ID、不扩锁、不改HTML/厂商或短信开关。
三个业务源文件及两个既有测试的改动，全部归009/026/027。版本维护与临时表装配属于同项修复验证，不是无ID优化。

## 第一性原理、顺从式与对抗式结论

009：before版本岗位入口AccessService1043—1101实际写user_roles及模板应用，并产生access.user_position.assigned成功事件，其metadata有role_id/template_id及user目标。独立角色入口产生access.role.assigned/access.role.unassigned事件；独立套用模板仅在AccessService235调用applyTemplate并固定applied_by=access-api，岗位来源用当前actor.id（1152—1158）。JdbcAccessRepository573—611保持该来源事实。V386按成功事件接续当前岗位角色与所有旧岗位模板应用；迁移后新岗位列负责派生角色，旧user_roles中已证实的派生关系删除；最近明确独立授予继续留在独立角色表。旧应用仅在后续换岗时由既有replacePositionGrants事务撤销，独立access-api应用不动。没有按几秒内、名称相同或权限相同猜来源。
反查旧写入的直接SQL来源：ProductionAuthService106—109的引导管理员角色、JdbcPartnerRepository125—135的合作方注册角色，以及StaffService已有自助角色来源，均作为已有独立基础角色保留，不对这些来源误删。无成功岗位事件的异常历史行不猜来源；正常岗位入口成功会落事件。权限来源缺失与人工越过入口导入属于数据事实不足，不新增授权来源或臆造历史。旧基础角色与自定义岗位复用的组合保守保留基础关系，不扩展业务规则。
对抗：真实PostgreSQL临时表同时包含更早岗位角色、当前岗位角色、同角色独立授予、同模板独立应用、缺新增来源列的旧岗位。实际迁移后旧角色转移、过往派生角色移除；显式角色保留；后续换岗只撤销旧派生应用，独立/新应用仍active。不是只在新列已填的夹具上测试。原r1临时反例省略了真实入口的成功审计记录，故本次以补齐真实旧写入形态的迁移测试验证，不伪称旧无来源夹具变成可推断来源。

026：countPositionTemplateUsage保留新assigned_position_template_id的精确计数；只有该列NULL的旧行，才按旧position_code/staff_level_id及组织/全局匹配计入占用。同一行是OR谓词，不会重复计数；另一组织定义不匹配。历史无法确定原具体mapping ID时保守计入可适用配置，不能把NULL当未占用；新换岗后按具体ID计数。已有存档/审批/软删除逻辑及权限闸没有被改变。旧配置占用3、异组织占用0的实际断言通过。

027：topup持有loadOrCreateAccountForUpdate返回的同一卡事实，把原从商户当前配置取模式的一处调用改为既有effectiveFundOwnership(card)。开户仍从商户配置定格，旧卡续充按卡上值；确认分支原来也按同一effectiveFundOwnership核对回执。旧卡NULL模式按已有merchant_prepaid兜底，通用卡仍null，没加新模式。原手机号/SMS、订单金额/本人卡、支付渠道真实确认、POS、冻结/幂等边界保留。旧卡merchant_prepaid、商户改platform_collect的失败反例现通过；原16钱包用例、11新钱包、4真实JDBC及247调用者回归通过。没有接通真实支付/SMS或把预留接口改为成功。

全12hunk审完；本席无新增FAIL依据。r1其他已修条目在本轮before/after均保留，未借旧ID改019—023。5文件严格UTF8、版本递增、无替换字符或NUL；diff --check通过。完整diff没有三HTML/锁/审计正文/.scratch变化。B4本席审计PASS与越界PASS；仍待外席，不出队、不进B5、不宣称模块PASS。

## 实际验证及限制

B3-r2最小失败：钱包11项中的新反例期望merchant_prepaid、实得platform_collect；岗位12项中的新反例期望占用3、实得0。两者fresh javac成功，实际行为断言失败。测试初次装配曾遗漏created_at/new mapping，已补齐并单独重跑取得业务断言失败，不把装配失败作缺陷证据；中间Java类型笔误编译失败已更正，最终全编译通过。
最终377测试通过：31安全+56Access/Authorization+247Customer/Bar/Finance+16旧钱包+11新钱包+4钱包JDBC+12授权JDBC。附真实命令和返回输出；247输出曾由工具中段截断，保留实际截断标记及RESULT247/exit0，不补写没收到的行。
全后端mvn test-compile -DskipTests exit0，仅表示编译；既有两项Spring共享库集成测试未运行。JDBC只在complex_test连接级TEMP表执行迁移与断言，不迁移共享库。前端本轮无变化，未重复r1的45测试。真实厂商/SMS仍未接入；本报告不声称可线上收款或已上线。
实际按已读systematic-debugging、TDD、requesting-code-review、receiving-code-review、verification-before-completion的合同收窄节点执行；没有第三席。未核到utf8_guard.py，使用真实严格UTF8检查，不冒称工具调用。

## 逐块归因
| hunk | 路径 | 原始块头 | 定稿ID |
|---|---|---|---|
| H001 | backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java | @@ -1,5 +1,5 @@ | M006-026 |
| H002 | backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java | @@ -511,7 +511,14 @@ class JdbcAccessRepository implements AccessRepository { | M006-026 |
| H003 | backend/src/main/java/com/complex/platform/customer/StoredValueService.java | @@ -1,6 +1,6 @@ | M006-027 |
| H004 | backend/src/main/java/com/complex/platform/customer/StoredValueService.java | @@ -130,7 +130,7 @@ public class StoredValueService { | M006-027 |
| H005 | backend/src/main/java/com/complex/platform/customer/StoredValueService.java | @@ -146,7 +146,7 @@ public class StoredValueService { | M006-027 |
| H006 | backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql | @@ -1,4 +1,4 @@ | M006-009 / M006-026 |
| H007 | backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql | @@ -27,3 +27,45 @@ ALTER TABLE user_position_assignments | M006-009 / M006-026 |
| H008 | backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java | @@ -1,6 +1,6 @@ | M006-009 / M006-026 |
| H009 | backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java | @@ -136,10 +136,7 @@ class M006JdbcRegressionTests { | M006-009 / M006-026 |
| H010 | backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java | @@ -257,4 +254,53 @@ class M006JdbcRegressionTests { | M006-009 / M006-026 |
| H011 | backend/src/test/java/com/complex/platform/customer/M006StoredValuePaymentTests.java | @@ -1,6 +1,6 @@ | M006-027 |
| H012 | backend/src/test/java/com/complex/platform/customer/M006StoredValuePaymentTests.java | @@ -220,4 +220,22 @@ class M006StoredValuePaymentTests { | M006-027 |

## 真实命令与输出

### 验证 1

```powershell
@'
from pathlib import Path
import subprocess,tempfile,os,re
for line in Path('/etc/complex/complex.env').read_text().splitlines():
 for key in ['COMPLEX_DATABASE_URL','COMPLEX_DATABASE_USERNAME','COMPLEX_DATABASE_PASSWORD']:
  if line.startswith(key+'='):os.environ[key]=line.split('=',1)[1].strip().strip(chr(34)+chr(39))
os.environ['COMPLEX_DATABASE_URL']=re.sub(r'/[^/?]+(?:\?.*)?$','/complex_test',os.environ['COMPLEX_DATABASE_URL'])
assert os.environ['COMPLEX_DATABASE_URL'].endswith('/complex_test')
runner="package com.complex.platform.access;\npublic class M006Runner {\n public static void main(String[] args)throws Exception {\n  int passed=0,failed=0;\n  for(String name:args){var c=Class.forName(\"com.complex.platform.access.\"+name);\n   for(var m:c.getDeclaredMethods())if(m.isAnnotationPresent(org.junit.jupiter.api.Test.class)){\n    var ctor=c.getDeclaredConstructor();ctor.setAccessible(true);var t=ctor.newInstance();\n    try {for(var b:c.getDeclaredMethods())if(b.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)){b.setAccessible(true);b.invoke(t);}\n     m.setAccessible(true);m.invoke(t);passed++;System.out.println(\"PASS \"+m.getName());\n    }catch(java.lang.reflect.InvocationTargetException e){failed++;System.out.println(\"FAIL \"+m.getName()+\": \"+e.getCause());}\n    finally{for(var a:c.getDeclaredMethods())if(a.isAnnotationPresent(org.junit.jupiter.api.AfterEach.class)){a.setAccessible(true);a.invoke(t);}}\n   }\n  }\n  System.out.println(\"RESULT \"+passed+\" PASS / \"+failed+\" FAIL\");if(failed>0)System.exit(1);\n }\n}"
with tempfile.TemporaryDirectory(prefix="m006-b3-check-",dir=".scratch") as d:
 root=Path(d).resolve();cpfile=root/"classpath"
 r=subprocess.run(["mvn","-o","-q","-f","backend/pom.xml","dependency:build-classpath","-Dmdep.outputFile="+str(cpfile)],capture_output=True,text=True)
 if r.returncode:print(r.stdout,r.stderr);raise SystemExit(r.returncode)
 cp=str(Path("backend/target/classes").resolve())+":"+cpfile.read_text().strip()
 src=root/"M006Runner.java";src.write_text(runner,encoding="utf-8");out=root/"classes";out.mkdir()
 base=Path("backend/src/main/java/com/complex/platform");tests=Path("backend/src/test/java/com/complex/platform")
 sources=list((base/"access").glob("*.java"))+list((base/"access/dto").glob("*.java"))
 sources += [base/"common/idempotency/IdempotencyService.java",base/"governance/review/SupervisorReviewService.java",base/"evidence/AuditEventService.java"]
 sources += [tests/"access/M006JdbcRegressionTests.java",tests/"support/InMemoryAuditEventRepository.java",tests/"support/InMemoryIdempotencyRepository.java"]
 r=subprocess.run(["javac","-encoding","UTF-8","--release","21","-cp",cp,"-d",str(out),*[str(p) for p in sources],str(src)],capture_output=True,text=True)
 print("JAVAC_EXIT",r.returncode,"SOURCES",len(sources));print(r.stdout,r.stderr)
 if r.returncode:raise SystemExit(r.returncode)
 r=subprocess.run(["java","-cp",str(out)+":"+cp,"com.complex.platform.access.M006Runner","M006JdbcRegressionTests"],capture_output=True,text=True)
 print(r.stdout,r.stderr);raise SystemExit(r.returncode)

'@ | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -'
```

```text
{
  "chunk_id": "2b6136",
  "wall_time_seconds": 5.6065595,
  "exit_code": 0,
  "original_token_count": 233,
  "output": "JAVAC_EXIT 0 SOURCES 48\n Note: backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java uses or overrides a deprecated API.\nNote: Recompile with -Xlint:deprecation for details.\n\nPASS temporaryGrantLogsItsActualSourceAndConsumesOnlyOnce\nPASS auditFailureRollsBackConsumptionThroughActualSpringTransaction\nPASS inheritedTemplateSingleUseCanBeConsumed\nPASS grantReviewReadLocksTheRequestRow\nPASS grantFactsRemainStableUntilTheGrantTransactionCommits\nPASS migrationAppliesAndGrantPolicyRoundTripsInPostgres\nPASS approvalFingerprintIsWriteOnceAndMissingIsExplicit\nPASS positionReplacementRevokesOnlyItsOwnPreviousApplications\nPASS createCollisionCannotOverwriteExistingUserOrRole\nPASS grantPaginationKeepsScopeAndCanReachTheSecondPage\nPASS positionTemplateSoftDeleteCanBeRestoredWithoutLosingItsDefinition\nPASS legacyPositionMigrationPreservesIndependentGrantsAndCountsOccupiedDefinitions\nRESULT 12 PASS / 0 FAIL\n \n"
}
```

### 验证 2

```powershell
@'
from pathlib import Path
import subprocess,tempfile
runner="package com.complex.platform.access;\npublic class M006Runner {\n public static void main(String[] args)throws Exception {\n  int passed=0,failed=0;\n  for(String name:args){var c=Class.forName(name);\n   for(var m:c.getDeclaredMethods())if(m.isAnnotationPresent(org.junit.jupiter.api.Test.class)){\n    var ctor=c.getDeclaredConstructor();ctor.setAccessible(true);var t=ctor.newInstance();\n    try {for(var b:c.getDeclaredMethods())if(b.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)){b.setAccessible(true);b.invoke(t);}\n     m.setAccessible(true);m.invoke(t);passed++;System.out.println(\"PASS \"+m.getName());\n    }catch(java.lang.reflect.InvocationTargetException e){failed++;System.out.println(\"FAIL \"+m.getName()+\": \"+e.getCause());}\n    finally{for(var a:c.getDeclaredMethods())if(a.isAnnotationPresent(org.junit.jupiter.api.AfterEach.class)){a.setAccessible(true);a.invoke(t);}}\n   }\n  }\n  System.out.println(\"RESULT \"+passed+\" PASS / \"+failed+\" FAIL\");if(failed>0)System.exit(1);\n }\n}"
with tempfile.TemporaryDirectory(prefix="m006-b3-check-",dir=".scratch") as d:
 root=Path(d).resolve();cpfile=root/"classpath"
 r=subprocess.run(["mvn","-o","-q","-f","backend/pom.xml","dependency:build-classpath","-Dmdep.outputFile="+str(cpfile)],capture_output=True,text=True)
 if r.returncode:print(r.stdout,r.stderr);raise SystemExit(r.returncode)
 cp=str(Path("backend/target/classes").resolve())+":"+cpfile.read_text().strip()
 src=root/"M006Runner.java";src.write_text(runner,encoding="utf-8");out=root/"classes";out.mkdir()
 base=Path("backend/src/main/java/com/complex/platform");tests=Path("backend/src/test/java/com/complex/platform")
 sources=list((base/"access").glob("*.java"))+list((base/"access/dto").glob("*.java"))
 sources += [base/"access/staff/StaffService.java",base/"common/idempotency/IdempotencyService.java",base/"governance/review/SupervisorReviewService.java",base/"evidence/AuditEventService.java"]
 sources += [base/"customer/CustomerService.java",base/"customer/CustomerOtpVerifier.java",base/"customer/FixedCustomerOtpVerifier.java",base/"customer/FailClosedCustomerOtpVerifier.java",base/"customer/StoredValueService.java",base/"bar/BarBillingService.java",base/"bar/BarBillSettleRequest.java",base/"bar/BarService.java",base/"bar/BarController.java",base/"customer/MemberWalletController.java",base/"customer/CustomerRepository.java",base/"customer/JdbcCustomerRepository.java",base/"customer/dto/StoredValueConsumeRequest.java",base/"ticketing/TicketingRepository.java",base/"ticketing/JdbcTicketingRepository.java",base/"finance/PaymentIntentRecord.java",base/"finance/FinanceService.java",base/"finance/FinanceRepository.java",base/"finance/JdbcFinanceRepository.java",base/"finance/gateway/PaymentGatewayResult.java"]
 sources += [tests/"customer/M006StoredValuePaymentTests.java",tests/"support/InMemoryAuditEventRepository.java",tests/"support/InMemoryIdempotencyRepository.java"]
 r=subprocess.run(["javac","-encoding","UTF-8","--release","21","-cp",cp,"-d",str(out),*[str(p) for p in sources],str(src)],capture_output=True,text=True)
 print("JAVAC_EXIT",r.returncode,"SOURCES",len(sources));print(r.stdout,r.stderr)
 if r.returncode:raise SystemExit(r.returncode)
 r=subprocess.run(["java","-cp",str(out)+":"+cp,"com.complex.platform.access.M006Runner","com.complex.platform.customer.M006StoredValuePaymentTests"],capture_output=True,text=True)
 print(r.stdout,r.stderr);raise SystemExit(r.returncode)

'@ | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -'
```

```text
{
  "chunk_id": "fe01a0",
  "wall_time_seconds": 6.2936854,
  "exit_code": 0,
  "original_token_count": 431,
  "output": "JAVAC_EXIT 0 SOURCES 69\n Note: backend/src/main/java/com/complex/platform/access/staff/StaffService.java uses unchecked or unsafe operations.\nNote: Recompile with -Xlint:unchecked for details.\n\nPASS appOrderPaysItsAmountOnceFromTheSelectedOwnCard\nPASS appOrderRejectsForeignOwnershipWrongSmsFrozenBalanceAndWrongCardScope\nPASS onlyMatchingGatewayReceiptCreditsTheRechargeAndRepeatDoesNotCreditAgain\nPASS onlineTopupCannotCreditAPlaceholderSuccessFlag\nPASS walletHasNoArbitraryAmountConsumeEndpoint\nPASS requestingCodeWithUnconfiguredProviderFailsInsteadOfReportingSent\nPASS validPhoneAndSmsPayTheActualUnboundBill\nPASS missingSmsCannotDebitAnAlreadyBoundCustomer\nPASS incorrectSmsDoesNotDebit\nPASS unavailableProductionSmsDoesNotDebit\nPASS rechargeAfterMerchantConfigChangeUsesTheCardsFrozenFundMode\nRESULT 11 PASS / 0 FAIL\n Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build as described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3\nWARNING: A Java agent has been loaded dynamically (/home/james/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.10/byte-buddy-agent-1.18.10.jar)\nWARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning\nWARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information\nWARNING: Dynamic loading of agents will be disallowed by default in a future release\nOpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended\n\n"
}
```

### 验证 3

```powershell
@'
from pathlib import Path
import subprocess,tempfile
runner="package com.complex.platform.access;\npublic class M006Runner {\n public static void main(String[] args)throws Exception {\n  int passed=0,failed=0;\n  for(String name:args){var c=Class.forName(name);\n   for(var m:c.getDeclaredMethods())if(m.isAnnotationPresent(org.junit.jupiter.api.Test.class)){\n    var ctor=c.getDeclaredConstructor();ctor.setAccessible(true);var t=ctor.newInstance();\n    try {for(var b:c.getDeclaredMethods())if(b.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)){b.setAccessible(true);b.invoke(t);}\n     m.setAccessible(true);m.invoke(t);passed++;System.out.println(\"PASS \"+m.getName());\n    }catch(java.lang.reflect.InvocationTargetException e){failed++;System.out.println(\"FAIL \"+m.getName()+\": \"+e.getCause());}\n    finally{for(var a:c.getDeclaredMethods())if(a.isAnnotationPresent(org.junit.jupiter.api.AfterEach.class)){a.setAccessible(true);a.invoke(t);}}\n   }\n  }\n  System.out.println(\"RESULT \"+passed+\" PASS / \"+failed+\" FAIL\");if(failed>0)System.exit(1);\n }\n}"
with tempfile.TemporaryDirectory(prefix="m006-b3-check-",dir=".scratch") as d:
 root=Path(d).resolve();cpfile=root/"classpath"
 r=subprocess.run(["mvn","-o","-q","-f","backend/pom.xml","dependency:build-classpath","-Dmdep.outputFile="+str(cpfile)],capture_output=True,text=True)
 if r.returncode:print(r.stdout,r.stderr);raise SystemExit(r.returncode)
 cp=str(Path("backend/target/classes").resolve())+":"+cpfile.read_text().strip()
 src=root/"M006Runner.java";src.write_text(runner,encoding="utf-8");out=root/"classes";out.mkdir()
 base=Path("backend/src/main/java/com/complex/platform");tests=Path("backend/src/test/java/com/complex/platform")
 sources=list((base/"access").glob("*.java"))+list((base/"access/dto").glob("*.java"))
 sources += [base/"access/staff/StaffService.java",base/"common/idempotency/IdempotencyService.java",base/"governance/review/SupervisorReviewService.java",base/"evidence/AuditEventService.java"]
 sources += [base/"customer/CustomerService.java",base/"customer/CustomerOtpVerifier.java",base/"customer/FixedCustomerOtpVerifier.java",base/"customer/FailClosedCustomerOtpVerifier.java",base/"customer/StoredValueService.java",base/"bar/BarBillingService.java",base/"bar/BarBillSettleRequest.java",base/"bar/BarService.java",base/"bar/BarController.java",base/"customer/MemberWalletController.java",base/"customer/CustomerRepository.java",base/"customer/JdbcCustomerRepository.java",base/"customer/dto/StoredValueConsumeRequest.java",base/"ticketing/TicketingRepository.java",base/"ticketing/JdbcTicketingRepository.java",base/"finance/PaymentIntentRecord.java",base/"finance/FinanceService.java",base/"finance/FinanceRepository.java",base/"finance/JdbcFinanceRepository.java",base/"finance/gateway/PaymentGatewayResult.java"]
 sources += [tests/"customer/MemberWalletServiceTests.java",tests/"support/InMemoryAuditEventRepository.java",tests/"support/InMemoryIdempotencyRepository.java"]
 r=subprocess.run(["javac","-encoding","UTF-8","--release","21","-cp",cp,"-d",str(out),*[str(p) for p in sources],str(src)],capture_output=True,text=True)
 print("JAVAC_EXIT",r.returncode,"SOURCES",len(sources));print(r.stdout,r.stderr)
 if r.returncode:raise SystemExit(r.returncode)
 r=subprocess.run(["java","-cp",str(out)+":"+cp,"com.complex.platform.access.M006Runner","com.complex.platform.customer.MemberWalletServiceTests"],capture_output=True,text=True)
 print(r.stdout,r.stderr);raise SystemExit(r.returncode)

'@ | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -'
```

```text
{
  "chunk_id": "4185b3",
  "wall_time_seconds": 5.8799839,
  "exit_code": 0,
  "original_token_count": 497,
  "output": "JAVAC_EXIT 0 SOURCES 69\n Note: backend/src/main/java/com/complex/platform/access/staff/StaffService.java uses unchecked or unsafe operations.\nNote: Recompile with -Xlint:unchecked for details.\n\nPASS topupAddsBalanceOnlyAfterPaymentSucceeds\nPASS consumeRejectedWhenInsufficient\nPASS freezeBlocksConsumeAndUnfreezeRestores\nPASS refundReturnsBalanceAndClosesAccount\nPASS settleTopupRejectsWhenScopeDiffersFromTopupBinding\nPASS refundFailedAtGatewayThrowsSoTransactionRollsBack\nPASS merchantCardIsolatedFromProjectCardAndOnlyConsumableByItsMerchant\nPASS merchantCardRefundRuleRejectsWhenNotAllowedAndConfigRoundTrips\nPASS merchantStaffCanOnlyOperateOwnMerchantCards\nPASS storedValueRefundWithGatewayOnDoesNotCrashAndReturnsBalance\nPASS consumeRejectsBlankIdempotencyKey\nPASS refundRejectsBlankIdempotencyKey\nPASS merchantCardTopupRejectedWithoutIssuanceThenAllowedAfterPlatformEnables\nPASS fundModeFrozenAtOpenAndPlatformOnly\nPASS platformCollectConsumeGeneratesMerchantPayableAndPrepaidDoesNot\nPASS merchantPrepaidCardRefundRejectedOnlineWhilePlatformCollectAllowed\nRESULT 16 PASS / 0 FAIL\n Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build as described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3\nWARNING: A Java agent has been loaded dynamically (/home/james/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.10/byte-buddy-agent-1.18.10.jar)\nWARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning\nWARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information\nWARNING: Dynamic loading of agents will be disallowed by default in a future release\nOpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended\n\n"
}
```

### 验证 4

```powershell
@'
from pathlib import Path
import subprocess,tempfile
runner="package com.complex.platform.access;\npublic class M006Runner {\n public static void main(String[] args)throws Exception {\n  int passed=0,failed=0;\n  for(String name:args){var c=Class.forName(\"com.complex.platform.access.\"+name);\n   for(var m:c.getDeclaredMethods())if(m.isAnnotationPresent(org.junit.jupiter.api.Test.class)){\n    var ctor=c.getDeclaredConstructor();ctor.setAccessible(true);var t=ctor.newInstance();\n    try {for(var b:c.getDeclaredMethods())if(b.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)){b.setAccessible(true);b.invoke(t);}\n     m.setAccessible(true);m.invoke(t);passed++;System.out.println(\"PASS \"+m.getName());\n    }catch(java.lang.reflect.InvocationTargetException e){failed++;System.out.println(\"FAIL \"+m.getName()+\": \"+e.getCause());}\n    finally{for(var a:c.getDeclaredMethods())if(a.isAnnotationPresent(org.junit.jupiter.api.AfterEach.class)){a.setAccessible(true);a.invoke(t);}}\n   }\n  }\n  System.out.println(\"RESULT \"+passed+\" PASS / \"+failed+\" FAIL\");if(failed>0)System.exit(1);\n }\n}"
with tempfile.TemporaryDirectory(prefix="m006-b3-check-",dir=".scratch") as d:
 root=Path(d).resolve();cpfile=root/"classpath"
 r=subprocess.run(["mvn","-o","-q","-f","backend/pom.xml","dependency:build-classpath","-Dmdep.outputFile="+str(cpfile)],capture_output=True,text=True)
 if r.returncode:print(r.stdout,r.stderr);raise SystemExit(r.returncode)
 cp=str(Path("backend/target/classes").resolve())+":"+cpfile.read_text().strip()
 src=root/"M006Runner.java";src.write_text(runner,encoding="utf-8");out=root/"classes";out.mkdir()
 base=Path("backend/src/main/java/com/complex/platform");tests=Path("backend/src/test/java/com/complex/platform")
 sources=list((base/"access").glob("*.java"))+list((base/"access/dto").glob("*.java"))
 sources += [base/"access/staff/StaffService.java",base/"common/idempotency/IdempotencyService.java",base/"governance/review/SupervisorReviewService.java",base/"evidence/AuditEventService.java"]
 sources += [tests/"access/M006SecurityRegressionTests.java",tests/"support/InMemoryAuditEventRepository.java",tests/"support/InMemoryIdempotencyRepository.java"]
 r=subprocess.run(["javac","-encoding","UTF-8","--release","21","-cp",cp,"-d",str(out),*[str(p) for p in sources],str(src)],capture_output=True,text=True)
 print("JAVAC_EXIT",r.returncode,"SOURCES",len(sources));print(r.stdout,r.stderr)
 if r.returncode:raise SystemExit(r.returncode)
 r=subprocess.run(["java","-cp",str(out)+":"+cp,"com.complex.platform.access.M006Runner","M006SecurityRegressionTests"],capture_output=True,text=True)
 print(r.stdout,r.stderr);raise SystemExit(r.returncode)

'@ | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -'
```

```text
{
  "chunk_id": "085b95",
  "wall_time_seconds": 8.0046169,
  "exit_code": 0,
  "original_token_count": 719,
  "output": "JAVAC_EXIT 0 SOURCES 49\n Note: backend/src/main/java/com/complex/platform/access/staff/StaffService.java uses unchecked or unsafe operations.\nNote: Recompile with -Xlint:unchecked for details.\n\nPASS administratorDenyWinsAndHighPermissionNeedsExplicitGrant\nPASS administratorKeepsDailyPermissionsWithoutAutomaticHighRole\nPASS untrustedPrincipalIsRejected\nPASS missingOrganizationCannotListAllUsersOrResolveGlobalProject\nPASS organizationReadWorksButCrossMerchantWriteDoesNot\nPASS collidingUserSlugsRemainDifferentUsers\nPASS templateEditCannotGrantAnUnownedHighPermission\nPASS templateEditPreservesOwnerAndRejectsMissingTemplate\nPASS emptyPermissionsAndInvertedWindowAreRejectedBeforeGrantWrite\nPASS invalidStatusAndBlankPasswordHaveNoWrites\nPASS manualAccountDoesNotPersistOrReturnPhoneDigest\nPASS administratorWithoutApprovalRoleHasNoSupervisorLevel\nPASS twoPermissionReviewConsumesOneSourceOnlyAfterBothChecksPass\nPASS credentialAdapterDisablesInvitedAccountsAndRejectsBlankPassword\nPASS developmentCredentialAdapterRejectsMissingAccount\nPASS changedRoleCannotBeAppliedUnderAnOldApproval\nPASS beneficiaryCannotApproveOwnGrant\nPASS positionChangeDropsDerivedRoleButKeepsIndependentAssignment\nPASS appliedTemplateCannotExtendTimeOrRemoveSingleUse\nPASS pendingTemporaryGrantRejectsInvertedTimeWindow\nPASS topAuthorityReviewRequiresMasterSessionRatherThanRankAlone\nPASS manualEmployeeCreationCannotIssueCredentialsBeforeRegistrationApproval\nPASS accountJsonDoesNotExposeStoredPhoneDigest\nPASS missingCredentialAdapterCannotReportSuccessfulCreation\nPASS employeeApprovalWithoutCredentialAdapterWritesNoAccount\nPASS registrationCreatesAccountOnlyAfterCompanyApproval\nPASS summaryQueryCannotOverrideBusinessThresholds\nPASS referenceDictionaryDoesNotPretendItWasTruncatedAtFifty\nPASS grantListHasADefaultTimeWindow\nPASS directPositionTemplateDeleteCannotBypassArchiveAndApproval\nPASS positionTemplateLifecyclePreservesDataAndRequiresIndependentMasterApproval\nRESULT 31 PASS / 0 FAIL\n Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build as described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3\nWARNING: A Java agent has been loaded dynamically (/home/james/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.10/byte-buddy-agent-1.18.10.jar)\nWARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning\nWARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information\nWARNING: Dynamic loading of agents will be disallowed by default in a future release\nOpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended\n\n"
}
```

### 验证 5

```powershell
@'
from pathlib import Path
import subprocess,tempfile
runner="package com.complex.platform.access;\npublic class M006Runner {\n public static void main(String[] args)throws Exception {\n  int passed=0,failed=0;\n  for(String name:args){var c=Class.forName(name);\n   for(var m:c.getDeclaredMethods())if(m.isAnnotationPresent(org.junit.jupiter.api.Test.class)){\n    var ctor=c.getDeclaredConstructor();ctor.setAccessible(true);var t=ctor.newInstance();\n    try {for(var b:c.getDeclaredMethods())if(b.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)){b.setAccessible(true);b.invoke(t);}\n     m.setAccessible(true);m.invoke(t);passed++;System.out.println(\"PASS \"+m.getName());\n    }catch(java.lang.reflect.InvocationTargetException e){failed++;System.out.println(\"FAIL \"+m.getName()+\": \"+e.getCause());}\n    finally{for(var a:c.getDeclaredMethods())if(a.isAnnotationPresent(org.junit.jupiter.api.AfterEach.class)){a.setAccessible(true);a.invoke(t);}}\n   }\n  }\n  System.out.println(\"RESULT \"+passed+\" PASS / \"+failed+\" FAIL\");if(failed>0)System.exit(1);\n }\n}"
with tempfile.TemporaryDirectory(prefix="m006-b3-check-",dir=".scratch") as d:
 root=Path(d).resolve();cpfile=root/"classpath"
 r=subprocess.run(["mvn","-o","-q","-f","backend/pom.xml","dependency:build-classpath","-Dmdep.outputFile="+str(cpfile)],capture_output=True,text=True)
 if r.returncode:print(r.stdout,r.stderr);raise SystemExit(r.returncode)
 cp=str(Path("backend/target/classes").resolve())+":"+cpfile.read_text().strip()
 src=root/"M006Runner.java";src.write_text(runner,encoding="utf-8");out=root/"classes";out.mkdir()
 base=Path("backend/src/main/java/com/complex/platform");tests=Path("backend/src/test/java/com/complex/platform")
 sources=list((base/"access").glob("*.java"))+list((base/"access/dto").glob("*.java"))
 sources += [base/"access/staff/StaffService.java",base/"common/idempotency/IdempotencyService.java",base/"governance/review/SupervisorReviewService.java",base/"evidence/AuditEventService.java"]
 sources += [base/"customer/CustomerService.java",base/"customer/CustomerOtpVerifier.java",base/"customer/FixedCustomerOtpVerifier.java",base/"customer/FailClosedCustomerOtpVerifier.java",base/"customer/StoredValueService.java",base/"bar/BarBillingService.java",base/"bar/BarBillSettleRequest.java",base/"bar/BarService.java",base/"bar/BarController.java",base/"customer/MemberWalletController.java",base/"customer/CustomerRepository.java",base/"customer/JdbcCustomerRepository.java",base/"customer/dto/StoredValueConsumeRequest.java",base/"ticketing/TicketingRepository.java",base/"ticketing/JdbcTicketingRepository.java",base/"finance/PaymentIntentRecord.java",base/"finance/FinanceService.java",base/"finance/FinanceRepository.java",base/"finance/JdbcFinanceRepository.java",base/"finance/gateway/PaymentGatewayResult.java"]
 sources += [tests/"access/AccessServiceTests.java",tests/"access/AuthorizationServiceTests.java",tests/"support/TestSecurityActors.java",tests/"support/InMemoryAuditEventRepository.java",tests/"support/InMemoryIdempotencyRepository.java"]
 r=subprocess.run(["javac","-encoding","UTF-8","--release","21","-cp",cp,"-d",str(out),*[str(p) for p in sources],str(src)],capture_output=True,text=True)
 print("JAVAC_EXIT",r.returncode,"SOURCES",len(sources));print(r.stdout,r.stderr)
 if r.returncode:raise SystemExit(r.returncode)
 r=subprocess.run(["java","-cp",str(out)+":"+cp,"com.complex.platform.access.M006Runner","com.complex.platform.access.AccessServiceTests","com.complex.platform.access.AuthorizationServiceTests"],capture_output=True,text=True)
 print(r.stdout,r.stderr);raise SystemExit(r.returncode)

'@ | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -'
```

```text
{
  "chunk_id": "fb079c",
  "wall_time_seconds": 7.0647295,
  "exit_code": 0,
  "original_token_count": 924,
  "output": "JAVAC_EXIT 0 SOURCES 71\n Note: backend/src/main/java/com/complex/platform/access/staff/StaffService.java uses unchecked or unsafe operations.\nNote: Recompile with -Xlint:unchecked for details.\n\nPASS copyPermissionTemplateKeepsSnapshotWhenSourceChangesAndWritesAuditEvent\nPASS applyPermissionTemplateRequiresApprovalWhenTemplateHasHighRiskPermission\nPASS applyPermissionTemplateAppliesToManyUsersAndWritesAuditEvent\nPASS temporaryGrantCreateAndRevokeWriteAuditEvents\nPASS createAccessUserWithPlatformAdminAccountTypeThrowsValidationError\nPASS createAccessUserWithAllowedAccountTypeSucceeds\nPASS platformAdminWithMerchantAffiliationCanCreateUserForAnyMerchant\nPASS createAccessUserRequiresOrganizationScope\nPASS createAccessUserStoresManualEvidenceAndProvisionsCredential\nPASS merchantScopedCreatorCannotCreateUserForAnotherMerchant\nPASS assignStaffLevelSetsLevelAndDefaultsCurrencyToCny\nPASS assignStaffLevelRejectsUnknownLevel\nPASS assignStaffLevelRejectsNonCnyCurrency\nPASS assignStaffLevelRejectsCrossOrganization\nPASS listStaffLevelsReturnsSeededLevels\nPASS assignAndUnassignUserRoleUpdateUserRoles\nPASS assignUserPositionAppliesMappedRoleAndTemplate\nPASS assignUserRoleRejectsRolePermissionsActorDoesNotHold\nPASS assignUserRoleRequiresApprovalWhenHighRiskGrantFlagIsEnabled\nPASS assignUserPositionRequiresApprovalWhenMappedRoleOrTemplateIsHighRisk\nPASS grantPolicyRejectsAnyUnclassifiedPermissionInTheRequestedRole\nPASS grantRequestRejectsPermissionsThatDoNotRequireApproval\nPASS approvedUserRoleGrantRequestUsesSupervisorReviewAndLandsAssignment\nPASS approvedTemporaryGrantExpiresByEffectivePermissionRead\nPASS auditorOutsideTemplatePermissionNeedsApprovedTemporaryGrantBeforeUse\nPASS grantSummaryReportsFrequentTemporaryRequestsByPositionAndUser\nPASS applyPermissionTemplateWithCrossOrgTargetUserThrowsForbidden\nPASS revokeTemporaryGrantOfOtherOrgUserThrowsForbidden\nPASS updatePermissionTemplateOfOtherOrgThrowsForbidden\nPASS createTemporaryGrantRejectsGrantingPermissionActorDoesNotHold\nPASS applyPlatformOwnedTemplateByOrgUserToSameOrgUserIsForbidden\nPASS listAccessUsersFiltersToCurrentOrganization\nPASS listAccessUsersPlatformAdminSeesAllOrganizations\nPASS listAccessUsersFiltersToCurrentMerchantWhenMerchantScoped\nPASS listGrantRequestsFiltersRowsStampedWithOtherProjects\nPASS denyWinsWhenGrantAndDenyExistForSamePermission\nPASS currentMerchantScopeReturnsEmptyForPlatformAdmin\nPASS isPlatformAdminReturnsTrueForPlatformAdminActor\nPASS expiredTemporaryTemplateGrantDoesNotAuthorize\nPASS isPlatformAdminReturnsFalseForStaffActor\nPASS outsourcedVendorIsNotPlatformScopeAndRequiresProjectScope\nPASS requirePermissionReturnsForbiddenPermissionDeniedWhenUserLacksPermission\nPASS singleUseTemplateGrantIsConsumedAfterFirstSuccessfulAuthorization\nPASS localSeedBridgeAuthorizesPlatformAdminButNotArbitraryStaffWithSmokeRole\nPASS singleUsePermissionConcurrentConsumerIsDeniedWhenAtomicUpdateReturnsZeroRows\nPASS outsourcedFacilityTemporaryGrantAllowsAssignedWorkAndEquipmentReadButNotAccept\nPASS outsourcedTemporaryGrantExpiresAndNoLongerAuthorizes\nPASS outsourcedSecurityCleaningTemporaryGrantOnlyGrantsAssignedInspectionAndExceptionPermissions\nPASS currentMerchantIdQuietlyReturnsEmptyForPlatformAdminEvenWhenSeedBoundMerchantId\nPASS currentMerchantIdQuietlyReturnsMerchantIdForRealMerchantUser\nPASS currentMerchantScopeReturnsMerchantIdForMerchantUser\nPASS currentMerchantScopeThrowsForbiddenWhenMerchantIdIsAbsent\nPASS effectivePermissionsRemovesDenyCoveredCodesFromGrantsAndListsDenies\nPASS effectivePermissionsDoesNotGiveAdministratorAnUnboundedWildcard\nPASS effectivePermissionsQueryDoesNotConsumeSingleUseGrant\nPASS effectivePermissionsExcludesExpiredGrant\nRESULT 56 PASS / 0 FAIL\n \n"
}
```

### 验证 6

```powershell
@'
from pathlib import Path
import subprocess,tempfile,os,re
for line in Path('/etc/complex/complex.env').read_text().splitlines():
 for key in ['COMPLEX_DATABASE_URL','COMPLEX_DATABASE_USERNAME','COMPLEX_DATABASE_PASSWORD']:
  if line.startswith(key+'='):os.environ[key]=line.split('=',1)[1].strip().strip(chr(34)+chr(39))
os.environ['COMPLEX_DATABASE_URL']=re.sub(r'/[^/?]+(?:\?.*)?$','/complex_test',os.environ['COMPLEX_DATABASE_URL'])
assert os.environ['COMPLEX_DATABASE_URL'].endswith('/complex_test')
runner="package com.complex.platform.access;\npublic class M006Runner {\n public static void main(String[] args)throws Exception {\n  int passed=0,failed=0;\n  for(String name:args){var c=Class.forName(name);\n   for(var m:c.getDeclaredMethods())if(m.isAnnotationPresent(org.junit.jupiter.api.Test.class)){\n    var ctor=c.getDeclaredConstructor();ctor.setAccessible(true);var t=ctor.newInstance();\n    try {for(var b:c.getDeclaredMethods())if(b.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)){b.setAccessible(true);b.invoke(t);}\n     m.setAccessible(true);m.invoke(t);passed++;System.out.println(\"PASS \"+m.getName());\n    }catch(java.lang.reflect.InvocationTargetException e){failed++;System.out.println(\"FAIL \"+m.getName()+\": \"+e.getCause());}\n    finally{for(var a:c.getDeclaredMethods())if(a.isAnnotationPresent(org.junit.jupiter.api.AfterEach.class)){a.setAccessible(true);a.invoke(t);}}\n   }\n  }\n  System.out.println(\"RESULT \"+passed+\" PASS / \"+failed+\" FAIL\");if(failed>0)System.exit(1);\n }\n}"
with tempfile.TemporaryDirectory(prefix="m006-b3-check-",dir=".scratch") as d:
 root=Path(d).resolve();cpfile=root/"classpath"
 r=subprocess.run(["mvn","-o","-q","-f","backend/pom.xml","dependency:build-classpath","-Dmdep.outputFile="+str(cpfile)],capture_output=True,text=True)
 if r.returncode:print(r.stdout,r.stderr);raise SystemExit(r.returncode)
 cp=str(Path("backend/target/classes").resolve())+":"+cpfile.read_text().strip()
 src=root/"M006Runner.java";src.write_text(runner,encoding="utf-8");out=root/"classes";out.mkdir()
 base=Path("backend/src/main/java/com/complex/platform");tests=Path("backend/src/test/java/com/complex/platform")
 sources=list((base/"access").glob("*.java"))+list((base/"access/dto").glob("*.java"))
 sources += [base/"access/staff/StaffService.java",base/"common/idempotency/IdempotencyService.java",base/"governance/review/SupervisorReviewService.java",base/"evidence/AuditEventService.java"]
 sources += [base/"customer/CustomerService.java",base/"customer/CustomerOtpVerifier.java",base/"customer/FixedCustomerOtpVerifier.java",base/"customer/FailClosedCustomerOtpVerifier.java",base/"customer/StoredValueService.java",base/"bar/BarBillingService.java",base/"bar/BarBillSettleRequest.java",base/"bar/BarService.java",base/"bar/BarController.java",base/"customer/MemberWalletController.java",base/"customer/CustomerRepository.java",base/"customer/JdbcCustomerRepository.java",base/"customer/dto/StoredValueConsumeRequest.java",base/"ticketing/TicketingRepository.java",base/"ticketing/JdbcTicketingRepository.java",base/"finance/PaymentIntentRecord.java",base/"finance/FinanceService.java",base/"finance/FinanceRepository.java",base/"finance/JdbcFinanceRepository.java",base/"finance/gateway/PaymentGatewayResult.java"]
 sources += [tests/"customer/M006StoredValueJdbcTests.java",tests/"support/InMemoryAuditEventRepository.java",tests/"support/InMemoryIdempotencyRepository.java"]
 r=subprocess.run(["javac","-encoding","UTF-8","--release","21","-cp",cp,"-d",str(out),*[str(p) for p in sources],str(src)],capture_output=True,text=True)
 print("JAVAC_EXIT",r.returncode,"SOURCES",len(sources));print(r.stdout,r.stderr)
 if r.returncode:raise SystemExit(r.returncode)
 r=subprocess.run(["java","-cp",str(out)+":"+cp,"com.complex.platform.access.M006Runner","com.complex.platform.customer.M006StoredValueJdbcTests"],capture_output=True,text=True)
 print(r.stdout,r.stderr);raise SystemExit(r.returncode)

'@ | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -'
```

```text
{
  "chunk_id": "5cb0ac",
  "wall_time_seconds": 8.2598959,
  "exit_code": 0,
  "original_token_count": 380,
  "output": "JAVAC_EXIT 0 SOURCES 69\n Note: backend/src/test/java/com/complex/platform/customer/M006StoredValueJdbcTests.java uses or overrides a deprecated API.\nNote: Recompile with -Xlint:deprecation for details.\nNote: backend/src/main/java/com/complex/platform/access/staff/StaffService.java uses unchecked or unsafe operations.\nNote: Recompile with -Xlint:unchecked for details.\n\nPASS historyUsesBothOwnerAndCardAndReadsLaterPages\nPASS financePaymentReadTakesPostgresRowLock\nPASS posRechargeUsesOfflineConfirmationAndCreditsTheBalanceOnlyOnce\nPASS failedOrderUpdateRollsBackBothActualBalanceAndDebitLedger\nRESULT 4 PASS / 0 FAIL\n Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build as described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3\nWARNING: A Java agent has been loaded dynamically (/home/james/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.10/byte-buddy-agent-1.18.10.jar)\nWARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning\nWARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information\nWARNING: Dynamic loading of agents will be disallowed by default in a future release\nOpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended\n\n"
}
```

### 验证 7

```powershell
@'
from pathlib import Path
import subprocess,tempfile
runner="package com.complex.platform.access;\npublic class M006Runner {\n public static void main(String[] args)throws Exception {\n  int passed=0,failed=0;\n  for(String name:args){var c=Class.forName(name);\n   for(var m:c.getDeclaredMethods())if(m.isAnnotationPresent(org.junit.jupiter.api.Test.class)){\n    var ctor=c.getDeclaredConstructor();ctor.setAccessible(true);var t=ctor.newInstance();\n    try {for(var b:c.getDeclaredMethods())if(b.isAnnotationPresent(org.junit.jupiter.api.BeforeEach.class)){b.setAccessible(true);b.invoke(t);}\n     m.setAccessible(true);m.invoke(t);passed++;System.out.println(\"PASS \"+m.getName());\n    }catch(java.lang.reflect.InvocationTargetException e){failed++;System.out.println(\"FAIL \"+m.getName()+\": \"+e.getCause());}\n    finally{for(var a:c.getDeclaredMethods())if(a.isAnnotationPresent(org.junit.jupiter.api.AfterEach.class)){a.setAccessible(true);a.invoke(t);}}\n   }\n  }\n  System.out.println(\"RESULT \"+passed+\" PASS / \"+failed+\" FAIL\");if(failed>0)System.exit(1);\n }\n}"
with tempfile.TemporaryDirectory(prefix="m006-b3-check-",dir=".scratch") as d:
 root=Path(d).resolve();cpfile=root/"classpath"
 r=subprocess.run(["mvn","-o","-q","-f","backend/pom.xml","dependency:build-classpath","-Dmdep.outputFile="+str(cpfile)],capture_output=True,text=True)
 if r.returncode:print(r.stdout,r.stderr);raise SystemExit(r.returncode)
 cp=str(Path("backend/target/classes").resolve())+":"+cpfile.read_text().strip()
 src=root/"M006Runner.java";src.write_text(runner,encoding="utf-8");out=root/"classes";out.mkdir()
 base=Path("backend/src/main/java/com/complex/platform");tests=Path("backend/src/test/java/com/complex/platform")
 sources=list((base/"access").glob("*.java"))+list((base/"access/dto").glob("*.java"))
 sources += [base/"access/staff/StaffService.java",base/"common/idempotency/IdempotencyService.java",base/"governance/review/SupervisorReviewService.java",base/"evidence/AuditEventService.java"]
 sources += [base/"customer/CustomerService.java",base/"customer/CustomerOtpVerifier.java",base/"customer/FixedCustomerOtpVerifier.java",base/"customer/FailClosedCustomerOtpVerifier.java",base/"customer/StoredValueService.java",base/"bar/BarBillingService.java",base/"bar/BarBillSettleRequest.java",base/"bar/BarService.java",base/"bar/BarController.java",base/"customer/MemberWalletController.java",base/"customer/CustomerRepository.java",base/"customer/JdbcCustomerRepository.java",base/"customer/dto/StoredValueConsumeRequest.java",base/"ticketing/TicketingRepository.java",base/"ticketing/JdbcTicketingRepository.java",base/"finance/PaymentIntentRecord.java",base/"finance/FinanceService.java",base/"finance/FinanceRepository.java",base/"finance/JdbcFinanceRepository.java",base/"finance/gateway/PaymentGatewayResult.java"]
 sources += [tests/"customer/CustomerServiceTests.java",tests/"bar/BarServiceTests.java",tests/"finance/FinanceServiceTests.java",tests/"support/TestSecurityActors.java",tests/"support/InMemoryAuditEventRepository.java",tests/"support/InMemoryIdempotencyRepository.java"]
 r=subprocess.run(["javac","-encoding","UTF-8","--release","21","-cp",cp,"-d",str(out),*[str(p) for p in sources],str(src)],capture_output=True,text=True)
 print("JAVAC_EXIT",r.returncode,"SOURCES",len(sources));print(r.stdout,r.stderr)
 if r.returncode:raise SystemExit(r.returncode)
 r=subprocess.run(["java","-cp",str(out)+":"+cp,"com.complex.platform.access.M006Runner","com.complex.platform.customer.CustomerServiceTests","com.complex.platform.bar.BarServiceTests","com.complex.platform.finance.FinanceServiceTests"],capture_output=True,text=True)
 print(r.stdout,r.stderr);raise SystemExit(r.returncode)

'@ | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -'
```

```text
{
  "chunk_id": "1892cb",
  "wall_time_seconds": 8.8419541,
  "exit_code": 0,
  "original_token_count": 3864,
  "output": "Warning: truncated output (original token count: 3864)\nTotal output lines: 259\n\nJAVAC_EXIT 0 SOURCES 72\n Note: backend/src/main/java/com/complex/platform/access/staff/StaffService.java uses unchecked or unsafe operations.\nNote: Recompile with -Xlint:unchecked for details.\n\nPASS barScanMenuReturnsSafeProjectionWithoutInternalPrice\nPASS customerScanOrderDelegatesToBarOrderServiceWithCustomerId\nPASS phoneLoginReturnsCustomerTokenAndMasksPhone\nPASS phoneLoginCustomerIdDoesNotLeakPhone\nPASS accountLifecycleSuspendBlacklistDeregisterEnforcedAtLoginAndActions\nPASS logoutRevokesServerSideSessionAndIsIdempotent\nPASS suspensionAutoRestoresWhenExpiredOnNextLogin\nPASS listOrderTicketsDelegatesAndEnforcesAccountGate\nPASS phoneLoginRejectsInvalidSmsCodeWithoutCreatingSession\nPASS ticketProductsFilterByBarAndResolveName\nPASS ticketProductsArePublicListable\nPASS customerTicketOrderUsesCustomerIdentityAndIdempotency\nPASS facePhotoOrderRecordsConsentTraceableToCustomerAndNoticeVersion\nPASS facePhotoOrderWithoutConsentIsRejectedAndNothingIsPersisted\nPASS orderWithoutFacePhotoRecordsNoFaceConsent\nPASS customerOrdersAndQrAreScopedToCustomerToken\nPASS consentAndPointsUseCustomerTokenBoundary\nPASS consentIdsDoNotCollideForLongKeysDifferingPastTruncationCutoff\nPASS barReservationOptionsAndCreateUseCustomerTokenWithoutStaffPermission\nPASS barReservationComputesDepositFromFixedRule\nPASS barReservationComputesDepositFromRatioRule\nPASS pointsLedgerAndRedemptionItemsAreCustomerScoped\nPASS redeemPointsDeductsBalanceIssuesUnifiedVoucherWithoutInventoryCall\nPASS redeemPointsRejectsInsufficientPointsWithoutInventoryCall\nPASS redeemPointsRejectsFrozenAccountWithoutTouchingInventoryOrPoints\nPASS redeemPointsSeesCurrentBalanceAfterConcurrentModification\nPASS noShowForfeitsDeposit\nPASS refundWithoutTieredEngineFailsClosed\nPASS refundOverSelfServiceLimitIsBlockedByTieredAuthorization\nPASS createTableLandsIndependentCapacitySeparateFromFreeEntryCount\nPASS createTableWithoutCapacityFallsBackToFreeEntryCount\nPASS createTableSessionForPlatformDirectFillsNonNullMerchantPlaceholder\nPASS scanOrderBindsOpenSessionInheritsEntryGateAndSourcesApp\nPASS scanOrderRejectedWhenFeatureFlagDisabled\nPASS settleBillRejectsAlreadySettledBillToPreventDoubleSettlement\nPASS settleBillRejectsAmountExceedingPayableToPreventOverpaymentManipulation\nPASS settleBillRejectsWhenBelowTableMinimumConsumption\nPASS createProductRejectsDirectInternalPriceSinceV380\nPASS tableAvailabilityMarksActivelyReservedTab…2664 tokens truncated…VarianceIsPostedAsPayableToSupplier\nPASS overageVarianceIsPostedAsPayableCostReversal\nPASS unreviewedVarianceCannotBePosted\nPASS postingVarianceTwiceIsIdempotentReturningSameItem\nPASS concurrentDoublePostInventoryVarianceOnlyCreatesOneLedgerItem\nPASS depositActionReadsBalanceUnderRowLockAndRejectsOverdraw\nPASS refundLocksTargetPaymentBeforeCumulativeCheck\nPASS turnoverRentClampsBetweenFloorAndCap\nPASS turnoverRentStampsAccountingPeriodWithPeriodStartMonth\nPASS turnoverRentAppliesFloorWhenComputedBelowMinimum\nPASS turnoverRentAppliesCapWhenComputedAboveMaximum\nPASS p0eTurnoverRentRuleLandsAsDraftAndCannotBeBilledBeforeActivation\nPASS p0eTurnoverRentRuleBillsOnlyAfterActivation\nPASS p0eArchivedTurnoverRentRuleCannotBeBilled\nPASS depositFourActionsMaintainRunningBalance\nPASS depositRefundExceedingBalanceIsRejected\nPASS createDepositRefundPayablePostsPayableAndWritesLedgerRefundDecrementingBalance\nPASS propertyFeeBilledAtFixedRatePerSquareMeter\nPASS propertyFeeBillingIsIdempotentAndNoRefundNoTopUp\nPASS propertyFeeSkipsMerchantWithMissingArea\nPASS propertyFeeRejectsWhenNoActiveRateConfigured\nPASS propertyFeeExactPeriodRateOverridesProjectDefault\nPASS dailyCloseChannelTicketAndThirdpartySourcesAreNonEmpty\nPASS paymentGatewayOff_createPaymentIntentRecordsReservedNoExternalCall\nPASS paymentGatewayOn_createPaymentIntentRoutesToPortReturnsNotConfigured\nPASS paymentGatewayOff_refundUsesInternalManualChannel\nPASS paymentGatewayOn_refundDoesNotCrashAndStillRecorded\nRESULT 247 PASS / 0 FAIL\n Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build as described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3\nWARNING: A Java agent has been loaded dynamically (/home/james/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.10/byte-buddy-agent-1.18.10.jar)\nWARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning\nWARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information\nWARNING: Dynamic loading of agents will be disallowed by default in a future release\nOpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended\n\n"
}
```

### 验证 8

```powershell
ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && mvn -o -q -f backend/pom.xml test-compile -DskipTests'
```

```text
{
  "chunk_id": "6aa3c7",
  "exit_code": 0,
  "output": "",
  "wall_time_seconds": 9.6803585
}
```

## 两树完整diff逐字原文

```diff
diff --git a/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java b/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
index 8ea5d417..5344c1f3 100644
--- a/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
+++ b/backend/src/main/java/com/complex/platform/access/JdbcAccessRepository.java
@@ -1,5 +1,5 @@
 package com.complex.platform.access;
-// 文件版本：V0.10
+// 文件版本：V0.11
 import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
 import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;
 
@@ -511,7 +511,14 @@ class JdbcAccessRepository implements AccessRepository {
     @Override
     public long countPositionTemplateUsage(String id) {
         return jdbcTemplate.queryForObject("""
-                SELECT count(*) FROM user_position_assignments WHERE assigned_position_template_id = :id
+                SELECT count(*) FROM user_position_assignments assignment
+                 WHERE assigned_position_template_id = :id
+                    OR (assigned_position_template_id IS NULL AND EXISTS (
+                        SELECT 1 FROM position_role_templates mapping
+                         WHERE mapping.id = :id AND mapping.position_code = assignment.position_code
+                           AND mapping.staff_level_id = assignment.staff_level_id
+                           AND (mapping.organization_id IS NULL
+                                OR mapping.organization_id = assignment.organization_id)))
                 """, Map.of("id", id), Long.class);
     }
 
diff --git a/backend/src/main/java/com/complex/platform/customer/StoredValueService.java b/backend/src/main/java/com/complex/platform/customer/StoredValueService.java
index eed489a5..cc0af2e3 100644
--- a/backend/src/main/java/com/complex/platform/customer/StoredValueService.java
+++ b/backend/src/main/java/com/complex/platform/customer/StoredValueService.java
@@ -1,6 +1,6 @@
 package com.complex.platform.customer;
 
-// 文件版本：V0.05
+// 文件版本：V0.07
 
 import com.complex.platform.common.api.ApiException;
 import com.complex.platform.common.money.MoneyRounding;
@@ -130,7 +130,7 @@ public class StoredValueService {
         // 发卡资格门控（AUD-061）：商户卡充值前须平台已给该商户开通「发卡资格」，默认关→拒。通用卡不受限。
         requireCardIssuanceEnabled(merchantId);
         // 提前建账户（active），充值入账时复用（开卡即从商户配置定格资金模式）。
-        loadOrCreateAccountForUpdate(customerId, projectId, merchantId);
+        var card = loadOrCreateAccountForUpdate(customerId, projectId, merchantId);
         String key = idempotencyKey == null || idempotencyKey.isBlank()
                 ? "sv_topup_" + UUID.randomUUID() : idempotencyKey;
         // B1d⑤ 资金归属：商户卡走 stored_value_topup_merchant（finance 据此把预收记入发卡商户，按开关结算）；
@@ -146,7 +146,7 @@ public class StoredValueService {
         PaymentIntentRecord intent = financeService.createPaymentIntent(new PaymentIntentRequest(
                 scopedTopupKey, null, null, businessOrderType, customerId, amountCent, "CNY",
                 channel == null || channel.isBlank() ? "wechat" : channel, null,
-                merchantId, resolveMerchantFundOwnership(merchantId)));
+                merchantId, effectiveFundOwnership(card)));
         // AUD-058 + AUD 复核 P1-1②：把本次充值的卡范围（customer/project/merchant）绑定到支付意图，settle 只认此绑定范围入账。
         // bind「冲突即校验一致」：同一支付意图已绑定的卡范围与本次不同 → 抛 409（不再 DO UPDATE 静默改绑）；完全一致才幂等放过。
         if (repository.bindStoredValueTopupIntent(intent.id(), customerId, projectId, merchantId, now()) == 0) {
diff --git a/backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql b/backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql
index f90c67ee..20db4b23 100644
--- a/backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql
+++ b/backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql
@@ -1,4 +1,4 @@
--- 文件版本：V0.04
+-- 文件版本：V0.07
 -- M006-006：授权申请的审批事实快照；不回填既有申请，缺快照须重新申请。
 ALTER TABLE access_grant_requests ADD COLUMN authorization_fingerprint VARCHAR(64);
 
@@ -27,3 +27,45 @@ ALTER TABLE user_position_assignments
     ADD COLUMN assigned_role_id VARCHAR(64) REFERENCES roles(id),
     ADD COLUMN assigned_position_template_id VARCHAR(64) REFERENCES position_role_templates(id),
     ADD COLUMN position_application_ids VARCHAR(64)[] NOT NULL DEFAULT '{}';
+
+-- M006-009：旧入口仅岗位应用使用实际操作员ID；独立套用固定写access-api。
+-- 只接续成功岗位事件能证明的来源，不按时间接近或相同权限猜测来源。
+UPDATE user_position_assignments AS position
+   SET assigned_role_id = (
+           SELECT CASE WHEN EXISTS (
+                      SELECT 1 FROM user_roles legacy WHERE legacy.user_id = position.user_id
+                        AND legacy.role_id = event.metadata->>'role_id')
+                  THEN event.metadata->>'role_id' END
+             FROM audit_events event
+            WHERE event.event_name = 'access.user_position.assigned' AND event.event_result = 'success'
+              AND event.business_type = 'user_position_assignment' AND event.business_id = position.user_id
+            ORDER BY event.occurred_at DESC, event.id DESC LIMIT 1),
+       position_application_ids = ARRAY(
+           SELECT application.id FROM permission_template_applications application
+            WHERE application.target_type = 'user' AND application.target_id = position.user_id
+              AND application.applied_by IS NOT NULL AND application.applied_by <> 'access-api'
+              AND application.status = 'active'
+              AND EXISTS (SELECT 1 FROM audit_events event
+                           WHERE event.event_name = 'access.user_position.assigned'
+                             AND event.event_result = 'success' AND event.business_type = 'user_position_assignment'
+                             AND event.business_id = position.user_id
+                             AND event.metadata->>'template_id' = application.template_id));
+
+-- 角色表没有来源列；成功岗位事件识别派生角色，独立显式授予及既有注册/引导角色保留。
+DELETE FROM user_roles AS legacy
+ WHERE EXISTS (SELECT 1 FROM user_position_assignments position WHERE position.user_id = legacy.user_id)
+   AND EXISTS (SELECT 1 FROM audit_events event
+                WHERE event.event_name = 'access.user_position.assigned' AND event.event_result = 'success'
+                  AND event.business_type = 'user_position_assignment' AND event.business_id = legacy.user_id
+                  AND event.metadata->>'role_id' = legacy.role_id)
+   AND COALESCE((SELECT event.event_name FROM audit_events event
+                  WHERE event.event_name IN ('access.role.assigned', 'access.role.unassigned')
+                    AND event.event_result = 'success' AND event.business_type = 'user_role'
+                    AND event.business_id = legacy.user_id || ':' || legacy.role_id
+                  ORDER BY event.occurred_at DESC, event.id DESC LIMIT 1), '') <> 'access.role.assigned'
+   AND NOT EXISTS (
+       SELECT 1 FROM access_users account WHERE account.id = legacy.user_id
+         AND ((account.account_type = 'platform_admin' AND legacy.role_id = 'role_platform_admin')
+           OR (account.partner_org_id IS NOT NULL AND legacy.role_id IN
+               ('role_outsourced_security_cleaning', 'role_outsourced_construction', 'role_outsourced_facility'))
+           OR (account.account_type = 'staff' AND legacy.role_id = 'role_staff_self')));
diff --git a/backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java b/backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java
index f66f8d4c..0b636c0c 100644
--- a/backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java
+++ b/backend/src/test/java/com/complex/platform/access/M006JdbcRegressionTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.10
+// 文件版本：V0.13
 import static org.assertj.core.api.Assertions.*;
 import static org.junit.jupiter.api.Assumptions.assumeTrue;
 
@@ -136,10 +136,7 @@ class M006JdbcRegressionTests {
 
     // M006-006/009/016/026：执行完整迁移 SQL，仅作用于本连接临时表；验证政策可持久化修改。
     @Test void migrationAppliesAndGrantPolicyRoundTripsInPostgres() throws Exception {
-        jdbc.execute("ALTER TABLE pg_temp.access_grant_requests DROP COLUMN authorization_fingerprint");
-        for (String table : java.util.List.of("supervisor_review_policies", "position_role_templates", "user_position_assignments", "roles")) {
-            jdbc.execute("CREATE TEMP TABLE " + table + " (LIKE public." + table + " INCLUDING ALL)");
-        }
+        preparePositionMigrationTables();
         jdbc.execute(java.nio.file.Files.readString(java.nio.file.Path.of(
                 "backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql")));
         var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
@@ -257,4 +254,53 @@ class M006JdbcRegressionTests {
         jdbc.update("INSERT INTO temporary_permission_grants (id, subject_id, permission_code, single_use) VALUES (?, 'user', 'permission', true)", id);
         return id;
     }
+
+    // M006-009/026：旧版本真实写入形态经迁移后仍区分岗位来源与独立来源。
+    @Test void legacyPositionMigrationPreservesIndependentGrantsAndCountsOccupiedDefinitions() throws Exception {
+        preparePositionMigrationTables();
+        jdbc.update("INSERT INTO roles (id, role_name, data_scope, status, created_at) VALUES ('old-role','旧岗位','project','active',now()), ('prior-role','更早岗位','project','active',now()), ('new-role','新岗位','project','active',now())");
+        jdbc.update("INSERT INTO access_users (id,username,account_type,status,organization_id,created_at) VALUES ('legacy','legacy','staff','active','org',now()), ('explicit','explicit','staff','active','org',now()), ('no-audit','no-audit','staff','active','org',now())");
+        jdbc.update("INSERT INTO position_role_templates (id,position_code,staff_level_id,role_id,template_id,organization_id) VALUES ('old-mapping','old-position','level','old-role','old-template','org'), ('outside','old-position','level','old-role','old-template','other-org'), ('new-mapping','new-position','level','new-role','new-template','org')");
+        jdbc.update("INSERT INTO user_position_assignments (user_id,position_code,staff_level_id,organization_id,assigned_by) VALUES ('legacy','old-position','level','org','operator'), ('explicit','old-position','level','org','operator'), ('no-audit','old-position','level','org','operator')");
+        jdbc.update("INSERT INTO user_roles (user_id,role_id) VALUES ('legacy','old-role'), ('legacy','prior-role'), ('explicit','old-role')");
+        jdbc.update("""
+                INSERT INTO permission_template_applications (id,template_id,target_id,target_type,single_use,applied_by,applied_at)
+                VALUES ('legacy-app','old-template','legacy','user',false,'operator',now()),
+                       ('independent-app','old-template','legacy','user',false,'access-api',now()),
+                       ('new-app','new-template','legacy','user',false,'operator',now())
+                """);
+        for (String user : java.util.List.of("legacy", "explicit")) {
+            jdbc.update("""
+                    INSERT INTO audit_events (id,occurred_at,event_name,event_result,business_type,business_id,metadata)
+                    VALUES (?,now(),'access.user_position.assigned','success','user_position_assignment',?,
+                            '{"position_code":"old-position","staff_level_id":"level","role_id":"old-role","template_id":"old-template"}')
+                    """, UUID.randomUUID(), user);
+        }
+        jdbc.update("""
+                INSERT INTO audit_events (id,occurred_at,event_name,event_result,business_type,business_id,metadata)
+                VALUES (?,now() - interval '1 day','access.user_position.assigned','success','user_position_assignment','legacy',
+                        '{"position_code":"prior-position","staff_level_id":"level","role_id":"prior-role","template_id":"prior-template"}'),
+                       (?,now(),'access.role.assigned','success','user_role','explicit:old-role','{"role_id":"old-role"}')
+                """, UUID.randomUUID(), UUID.randomUUID());
+        jdbc.execute(java.nio.file.Files.readString(java.nio.file.Path.of(
+                "backend/src/main/resources/db/migration/V386__access_grant_authorization_fingerprint.sql")));
+        assertThat(repository.countPositionTemplateUsage("old-mapping")).isEqualTo(3);
+        assertThat(repository.countPositionTemplateUsage("outside")).isZero();
+        assertThat(jdbc.queryForObject("SELECT assigned_role_id FROM user_position_assignments WHERE user_id='legacy'", String.class)).isEqualTo("old-role");
+        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_roles WHERE user_id='legacy'", Integer.class)).isZero();
+        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_roles WHERE user_id='explicit' AND role_id='old-role'", Integer.class)).isEqualTo(1);
+        repository.replacePositionGrants("legacy", "new-role", "new-mapping", java.util.List.of("new-app"), "operator", Instant.now());
+        assertThat(jdbc.queryForObject("SELECT status FROM permission_template_applications WHERE id='legacy-app'", String.class)).isEqualTo("revoked");
+        assertThat(jdbc.queryForObject("SELECT status FROM permission_template_applications WHERE id='independent-app'", String.class)).isEqualTo("active");
+        assertThat(jdbc.queryForObject("SELECT status FROM permission_template_applications WHERE id='new-app'", String.class)).isEqualTo("active");
+    }
+
+    private void preparePositionMigrationTables() {
+        jdbc.execute("ALTER TABLE pg_temp.access_grant_requests DROP COLUMN authorization_fingerprint");
+        for (String table : java.util.List.of("supervisor_review_policies", "position_role_templates",
+                "user_position_assignments", "roles", "user_roles", "access_users", "audit_events")) {
+            jdbc.execute("CREATE TEMP TABLE " + table + " (LIKE public." + table + " INCLUDING ALL)");
+        }
+        jdbc.execute("ALTER TABLE pg_temp.permission_template_applications ADD COLUMN target_type varchar, ADD COLUMN applied_by varchar, ADD COLUMN applied_at timestamptz, ADD COLUMN revoked_by varchar, ADD COLUMN revoked_at timestamptz");
+    }
 }
diff --git a/backend/src/test/java/com/complex/platform/customer/M006StoredValuePaymentTests.java b/backend/src/test/java/com/complex/platform/customer/M006StoredValuePaymentTests.java
index 51e84045..c1bb8b39 100644
--- a/backend/src/test/java/com/complex/platform/customer/M006StoredValuePaymentTests.java
+++ b/backend/src/test/java/com/complex/platform/customer/M006StoredValuePaymentTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.customer;
 
-// 文件版本：V0.11
+// 文件版本：V0.12
 // M006-027：现场储值扣款必须先核验报出的手机号和短信验证码。
 import static org.assertj.core.api.Assertions.*;
 import static org.mockito.Mockito.*;
@@ -220,4 +220,22 @@ class M006StoredValuePaymentTests {
                 .isInstanceOf(ApiException.class).hasMessageContaining("短信服务未配置");
         assertUnchanged();
     }
+
+    // M006-027：修改商户发卡配置后，已开卡续充仍使用卡本身定格的资金模式。
+    @Test void rechargeAfterMerchantConfigChangeUsesTheCardsFrozenFundMode() {
+        var finance=mock(FinanceService.class);
+        var gateway=mock(com.complex.platform.finance.gateway.PaymentGatewayRouter.class);
+        when(gateway.isGatewayEnabled()).thenReturn(true);
+        var service=new StoredValueService(customers,finance,mock(AuditEventService.class),gateway,auth,null);
+        when(customers.findStoredValueMerchantRefundRule("merchant_1")).thenReturn(Optional.of(
+                new StoredValueMerchantRefundRuleRecord("merchant_1",true,10000,true,"platform_collect",now,now)));
+        when(customers.findStoredValueAccount("customer_1",null,"merchant_1")).thenAnswer(i->Optional.of(balance.get()));
+        var intent=new com.complex.platform.finance.PaymentIntentRecord("pay","stored_value_topup_merchant","customer_1",5000L,"CNY","wechat","created",now,now);
+        when(finance.createPaymentIntent(any())).thenReturn(intent);
+        when(customers.bindStoredValueTopupIntent(eq("pay"),eq("customer_1"),isNull(),eq("merchant_1"),any())).thenReturn(1);
+        service.topup("customer_1",null,"merchant_1",5000L,"wechat","regular-recharge");
+        var request=org.mockito.ArgumentCaptor.forClass(com.complex.platform.finance.dto.PaymentIntentRequest.class);
+        verify(finance).createPaymentIntent(request.capture());
+        assertThat(request.getValue().fundMode()).isEqualTo(balance.get().fundOwnership());
+    }
 }
```


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# 当前471计划 M006 B4-r2 Codex 独立完整差异复审
版本：V0.02

修复内容结论：PASS。
范围归因结论：PASS。
综合结论：PASS。没有新finding。M006仍IN_PROGRESS；B3累计2。本票不是整模块B5或模块PASS。

## 独立性与范围
Grok B4-r2真实exit0且回执路径正确；本席尚未读本次Grok正文/日志，现按顺序重新审完本轮完整差异，先保存本席完整raw。未以B3运行结果替代B4。本raw原70271bytes保留，旧V0.01属于历史报告，本次独立V0.02起完整有效。
本批findings V1.05双B2已定稿，047为唯一仍在队列的ID；039–043已修出队、037/038/044–046不动。读取两树不按路径过滤，tracked/staged/untracked全部受diff覆盖，不用HEAD~1或旧backend模块替代。
before-tree=b50b33c1f1c61ab9a1c87f6cf79ea97ffa593ccd
after-tree=ece59cbd0b81d612f3cc6844d389415cb9394d31
全diff仅docs/delivery-readiness-gates-v0.1.md，2增2删；当前文件字节等于after-tree。全文差异随实际输出附于下方。

## 逐行归因与修复内容
| 改动 | 正式ID | 核验 |
|---|---|---|
| L1 V0.11→V0.12 | M006-047 | 同一文件内容变化必须递增自身版本；精确只变标题版本 |
| L96去“均”，加“或已记录对应接入开关关闭” | M006-047 | 与定稿候选完全一致；保留8、npm入口、映射分支字段、后半防伪生产边界 |

本席从before-tree原文按批准的两个精确字符串变换重建预期，实算expected==after-tree==当前源；变化行号只有1、96，所有npm引用逐项相等。没有未登记行、没有额外文件、没有移动/删除；不存在越界需回滚项。
GEN-002/016/018：当前真实正常路径中8项traceability都走disabled分支，但原句保证“均有责任/manifest”，现句明确容纳该分支。重新读取生成器166–206、227–268、408–421：238允许全关闭时跳过manifest要求，246算traceable，267policy明确OR；两分支被说明覆盖。不是将disabled变成生产证据，policy仍mock_or_sandbox_is_not_production_evidence，L96后半保持。无需改M414生成器/快照/开关。
GEN-017：B2已双核细化，有PGM066第三方未定/各端开关及BEQR HTML:210原文上位；无新业务规则、角色、状态、价格或审批。B4仅修字义，没扩大业务权限，不改锁/HTML。
GEN-001：选择收窄原一句，未增加脚本、测试文件、门禁或重复说明。Markdown类型4，不额外造测试。

## 本次新鲜验证
在Grok真实结束后，本席直接SSH重新读取两树完整diff、当前两源与三HTML SHA；重新实跑verify:delivery-gates-docs、verify:external-evidence-requirement-traceability，均exit0。真实Markdown解析47行均2列、无空格单元，npm引用完全保持。
首个通用打印小脚本尝试requirements/rows字段得到空数组，仅是调试辅助字段选择错误，不作为快照为空的证据；随后读顶层keys确定实际字段requirement_traceability，完整读出8行，当前mapped_item_ids/owner_groups/target_manifest_paths全空，switch_disabled非空，状态全covered_by_disabled_integration_switches。实际输出两段均保留，避免把[]冒充业务事实。无需改任何项目文件。
delivery现V0.12/15751bytes/SHA0615cd1b3beaffc097f96cc6140b16bfa3ec0a6be4e91c493dca2f0fa6217e09；deploy仍V0.07/7117bytes/SHA543a1543ca264f0741706fb5007d813f67ea84dd3acaafe40c2380cce9babda1。三HTML SHA与冻结来源不变，详下。
未执行父链49项、真实GitHubPUT/部署/推送；文档型最小验证不声称生产上线或其它工具全模块通过。既有源码非本次diff没有被“顺手修复”。本次无待答questions/HTML自身问题/新判据。
两席本次完整raw落齐后再读Grok作合取；仅双PASS可将047出队并启动B5-r2。

## 完整diff与新鲜命令输出

````text
CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
TREE_BEFORE b50b33c1f1c61ab9a1c87f6cf79ea97ffa593ccd TREE_AFTER ece59cbd0b81d612f3cc6844d389415cb9394d31 ALL_PATHS ["docs/delivery-readiness-gates-v0.1.md"]
FULL_UNFILTERED_DIFF
diff --git a/docs/delivery-readiness-gates-v0.1.md b/docs/delivery-readiness-gates-v0.1.md
index 88a00e9a..d262d35e 100644
--- a/docs/delivery-readiness-gates-v0.1.md
+++ b/docs/delivery-readiness-gates-v0.1.md
@@ -1,4 +1,4 @@
-# 交付总闸口与上线准入验证 V0.11
+# 交付总闸口与上线准入验证 V0.12
 
 ## 1. 定位
 
@@ -93,7 +93,7 @@ npm run verify:delivery-gates
 | `npm run verify:production-evidence-draft-prep` | 校验生产证据 manifest 草稿准备器默认 dry-run、拒绝越界写入和覆盖已有 manifest，且不会关闭生产阻断 |
 | `npm run verify:production-evidence-draft-prep-fixtures` | 在临时目录实测草稿准备器 dry-run 不写文件、execute 复制模板、再次 execute 不覆盖已有 manifest，并拒绝无选择器的 broad execute |
 | `npm run verify:production-closure-gap-reports` | 从当前运行态、Semgrep、NAS 当前状态、生产证据提交状态和外部证据追踪校验生产闭环缺口总报告，防止旧报告或手改报告伪关闭生产证据 |
-| `npm run verify:external-evidence-requirement-traceability` | 校验 8 条外部证据型 partial 需求均已追踪到责任组、证据 item_id、模板和目标 manifest 路径，防止 mock/沙箱被误判为生产闭环 |
+| `npm run verify:external-evidence-requirement-traceability` | 校验 8 条外部证据型 partial 需求已追踪到责任组、证据 item_id、模板和目标 manifest 路径，或已记录对应接入开关关闭，防止 mock/沙箱被误判为生产闭环 |
 | `npm run verify:nas-it-handoff` | 校验 NAS IT 受控挂载交接包、默认 dry-run 脚本、manifest 映射和无明文密钥边界 |
 | `npm run verify:nas-diagnostics` | 校验 NAS 只读诊断报告与当前网络、445 端口、CIFS 挂载和 preflight 根因一致，不关闭生产 manifest |
 | `npm run verify:nas-current-state-drift` | 校验 NAS 历史通过证据没有被误当作当前生产自动挂载、定时备份、告警和恢复演练闭环 |

EXACT_APPROVED_TRANSFORM true; both changed lines belong M006-047
CHANGED_LINES [1, 96]
MARKDOWN 47 rows all2cols noempty; NPM REFERENCES IDENTICAL
CMD npm run verify:delivery-gates-docs EXIT 0

> entertainment-complex-management@0.1.0 verify:delivery-gates-docs
> node tools/verify-delivery-gates.mjs

DELIVERY_GATES_DOCS_PASS
 
CMD npm run verify:external-evidence-requirement-traceability EXIT 0

> entertainment-complex-management@0.1.0 verify:external-evidence-requirement-traceability
> node tools/generate-external-evidence-requirement-traceability.mjs --check

EXTERNAL_EVIDENCE_REQUIREMENT_TRACEABILITY_PASS
 
TRACE_COUNTS {"partial_requirements": 8, "traceable_partial_requirements": 8, "mapped_evidence_items": 0, "mapped_ledger_rows": 0, "p0_evidence_rows": 0, "awaiting_evidence_rows": 0, "invalid_evidence_rows": 0, "valid_evidence_rows": 0, "production_blocking_requirements": 0}
POLICY {"no_fake_external_evidence_closure": true, "mock_or_sandbox_is_not_production_evidence": true, "partial_requirement_must_map_to_owner_manifest_or_disabled_switch": true, "integration_capability_switches_applied": true, "raw_evidence_stays_external": true, "credential_ref_only_no_plaintext_secret": true, "this_report_does_not_close_blockers": true}
TRACE_ROWS []
CURRENT_SOURCE docs/delivery-readiness-gates-v0.1.md 15751 0615cd1b3beaffc097f96cc6140b16bfa3ec0a6be4e91c493dca2f0fa6217e09
CURRENT_SOURCE docs/deploy-and-change-flow-v0.1.md 7117 543a1543ca264f0741706fb5007d813f67ea84dd3acaafe40c2380cce9babda1
CURRENT_SOURCE docs/business-enum-quick-reference-v0.1.html 133961 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66
CURRENT_SOURCE docs/business-system-map-for-operations-v0.1.html 304482 a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa
CURRENT_SOURCE docs/platform-governance-map-for-operations-v0.1.html 103015 c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c
B4_REPAIR_PASS SCOPE_PASS NO_NEW_FINDINGS

SNAPSHOT_KEYS ['traceability_version', 'status', 'generated_from', 'policy', 'submission_status_counts', 'counts', 'requirement_traceability']
ACTUAL_ROW_KEY requirement_traceability COUNT 8
[{"requirement_id": "REQ-17", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-PAY-001", "PRD-PAY-002", "PRD-FACE-001", "PRD-CRED-3P-001", "PRD-CRED-OBJ-001"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-21", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-PAY-001", "PRD-PAY-002"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-23b", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-FACE-001", "PRD-CRED-3P-001"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-25", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-PAY-001", "PRD-PAY-002"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-32", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-CRED-OBJ-001", "PRD-3P-001", "PRD-CRED-3P-001", "SEC-P1-WEBHOOK-REPLAY-001"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-40", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-3P-001", "PRD-CRED-OPEN-001", "PRD-CRED-3P-001", "SEC-P1-WEBHOOK-REPLAY-001"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-41", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-3P-001", "PRD-CRED-3P-001", "SEC-P1-WEBHOOK-REPLAY-001"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-44", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-3P-001", "PRD-CRED-3P-001", "SEC-P1-WEBHOOK-REPLAY-001", "PRD-MDM-001", "SEC-P0-MDM-DEVICE-001"], "production_trace_status": "covered_by_disabled_integration_switches"}]
DIRECT_SOURCE tools/generate-external-evidence-requirement-traceability.mjs
166:const traceRows = partialRequirements.map((requirement) => {
167:  const requirementId = requirement["需求编号"];
168:  const mapping = evidenceMappings[requirementId];
169:  const rawItemIds = mapping?.evidence_item_ids ?? [];
170:  const effectiveItemIds = rawItemIds.filter((itemId) => !disabledEvidenceItemIds.has(itemId) && !disabledSecurityFindingIds.has(itemId));
171:  const switchDisabledItemIds = rawItemIds.filter((itemId) => !effectiveItemIds.includes(itemId));
172:  const evidenceRows = effectiveItemIds.flatMap((itemId) => ledgerByItemId.get(itemId) ?? []);
173:  const missingItemIds = effectiveItemIds.filter((itemId) => !ledgerByItemId.has(itemId));
174:  const awaiting = evidenceRows.filter((row) => row.current_status === "awaiting_submission").length;
175:  const invalid = evidenceRows.filter((row) => row.current_status === "invalid_submission").length;
176:  const valid = evidenceRows.filter((row) => row.current_status === "valid_submission").length;
177:  const p0Rows = evidenceRows.filter((row) => row.priority === "P0");
178:  const ownerGroups = [...new Set(evidenceRows.map((row) => row.owner_group))].sort((a, b) => a.localeCompare(b, "zh-Hans-CN"));
179:  const productionBlocking = effectiveItemIds.length > 0 && p0Rows.some((row) => row.current_status !== "valid_submission");
180:  return {
181:    requirement_id: requirementId,
182:    module: requirement["业务模块"],
183:    acceptance: requirement["验收标准"],
184:    current_status: requirement["当前实现状态"],
185:    matrix_gap: requirement["缺口"],
186:    evidence_mapping_reason: mapping?.reason ?? "",
187:    mapped_item_ids: effectiveItemIds,
188:    switch_disabled_item_ids: switchDisabledItemIds,
189:    owner_groups: ownerGroups,
190:    evidence_rows_total: evidenceRows.length,
191:    p0_evidence_rows: p0Rows.length,
192:    awaiting_evidence_rows: awaiting,
193:    invalid_evidence_rows: invalid,
194:    valid_evidence_rows: valid,
195:    missing_item_ids: missingItemIds,
196:    production_blocking: productionBlocking,
197:    production_trace_status: effectiveItemIds.length === 0
198:      ? "covered_by_disabled_integration_switches"
199:      : missingItemIds.length > 0
200:        ? "traceability_missing_manifest"
201:        : invalid > 0
202:          ? "invalid_evidence_manifest_blocks"
203:          : awaiting > 0
204:            ? "awaiting_real_external_evidence"
205:            : "all_mapped_evidence_valid",
206:    target_manifest_paths: [...new Set(evidenceRows.map((row) => row.target_manifest_path).filter(Boolean))],
227:const failures = [];
228:for (const requirement of partialRequirements) {
229:  if (!evidenceMappings[requirement["需求编号"]]) failures.push(`${requirement["需求编号"]} is partial but has no external evidence mapping`);
230:}
231:for (const [requirementId, mapping] of Object.entries(evidenceMappings)) {
232:  if (!requirementRows.some((row) => row["需求编号"] === requirementId)) failures.push(`${requirementId} mapping does not exist in ${matrixPath}`);
233:  for (const itemId of mapping.evidence_item_ids.filter((id) => !disabledEvidenceItemIds.has(id) && !disabledSecurityFindingIds.has(id))) {
234:    if (!ledgerByItemId.has(itemId)) failures.push(`${requirementId} maps to ${itemId}, but ${itemId} is missing from ${ledgerPath}`);
235:  }
236:}
237:for (const row of traceRows) {
238:  if (row.mapped_item_ids.length === 0 && row.switch_disabled_item_ids.length > 0) continue;
239:  if (row.evidence_rows_total === 0) failures.push(`${row.requirement_id} has no evidence ledger rows`);
240:  if (row.missing_item_ids.length > 0) failures.push(`${row.requirement_id} has missing item ids: ${row.missing_item_ids.join(", ")}`);
241:  if (row.target_manifest_paths.length === 0) failures.push(`${row.requirement_id} has no target manifest paths`);
242:}
243:
244:const counts = {
245:  partial_requirements: partialRequirements.length,
246:  traceable_partial_requirements: traceRows.filter((row) => (row.evidence_rows_total > 0 && row.missing_item_ids.length === 0) || (row.mapped_item_ids.length === 0 && row.switch_disabled_item_ids.length > 0)).length,
247:  mapped_evidence_items: new Set(traceRows.flatMap((row) => row.mapped_item_ids)).size,
248:  mapped_ledger_rows: traceRows.reduce((sum, row) => sum + row.evidence_rows_total, 0),
249:  p0_evidence_rows: traceRows.reduce((sum, row) => sum + row.p0_evidence_rows, 0),
250:  awaiting_evidence_rows: traceRows.reduce((sum, row) => sum + row.awaiting_evidence_rows, 0),
251:  invalid_evidence_rows: traceRows.reduce((sum, row) => sum + row.invalid_evidence_rows, 0),
252:  valid_evidence_rows: traceRows.reduce((sum, row) => sum + row.valid_evidence_rows, 0),
253:  production_blocking_requirements: traceRows.filter((row) => row.production_blocking).length,
254:};
255:
256:const output = {
257:  traceability_version: "v0.1",
258:  status: failures.length > 0
259:    ? "traceability_gap"
260:    : counts.production_blocking_requirements > 0
261:      ? "traceable_but_awaiting_real_external_evidence"
262:      : "all_mapped_external_evidence_valid",
263:  generated_from: [matrixPath, ledgerPath, statusPath],
264:  policy: {
265:    no_fake_external_evidence_closure: true,
266:    mock_or_sandbox_is_not_production_evidence: true,
267:    partial_requirement_must_map_to_owner_manifest_or_disabled_switch: true,
268:    integration_capability_switches_applied: true,
408:if (checkMode) {
409:  const checkFailures = [...failures];
410:  for (const relativePath of [jsonPath, csvPath, mdPath]) {
411:    if (!fs.existsSync(absolute(relativePath))) checkFailures.push(`${relativePath} is missing; run npm run docs:external-evidence-requirement-traceability`);
412:  }
413:  if (checkFailures.length === 0) {
414:    if (normalize(readText(jsonPath)) !== json) checkFailures.push(`${jsonPath} is not in sync`);
415:    if (fs.readFileSync(absolute(csvPath), "utf8").replace(/\r\n/g, "\n") !== csv) checkFailures.push(`${csvPath} is not in sync`);
416:    if (normalize(readText(mdPath)) !== md) checkFailures.push(`${mdPath} is not in sync`);
417:  }
418:  if (counts.partial_requirements !== counts.traceable_partial_requirements) checkFailures.push("not every partial requirement is traceable to owner manifests");
419:  if (counts.partial_requirements === 0) checkFailures.push("expected at least one external evidence partial requirement while production evidence is open");
420:  if (checkFailures.length > 0) failWith(checkFailures);
421:  console.log("EXTERNAL_EVIDENCE_REQUIREMENT_TRACEABILITY_PASS");

````


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# 当前471计划 M006 B4-r2 Codex 独立完整差异复审
版本：V0.03

修复内容结论：PASS。
范围归因结论：PASS。
综合结论：PASS。没有新finding。M006仍IN_PROGRESS；B3累计2。本票不是整模块B5或模块PASS。

本次V0.03完整重附：V0.02原始命令输出含同名独占锁行，会被“最后一个锁行”规则误切报告边界；现仅将证据内该打印标签改为CHECK_ACTUAL_LOCK_SHA，原哈希数值、全部证据与结论不变，旧V0.02完整保留。此为本席报告格式恢复，不是源修或Grok交付失败。

## 独立性与范围
Grok B4-r2真实exit0且回执路径正确；本席尚未读本次Grok正文/日志，现按顺序重新审完本轮完整差异，先保存本席完整raw。未以B3运行结果替代B4。本raw原70271bytes保留，旧V0.01属于历史报告，本次独立V0.03起完整有效。
本批findings V1.05双B2已定稿，047为唯一仍在队列的ID；039–043已修出队、037/038/044–046不动。读取两树不按路径过滤，tracked/staged/untracked全部受diff覆盖，不用HEAD~1或旧backend模块替代。
before-tree=b50b33c1f1c61ab9a1c87f6cf79ea97ffa593ccd
after-tree=ece59cbd0b81d612f3cc6844d389415cb9394d31
全diff仅docs/delivery-readiness-gates-v0.1.md，2增2删；当前文件字节等于after-tree。全文差异随实际输出附于下方。

## 逐行归因与修复内容
| 改动 | 正式ID | 核验 |
|---|---|---|
| L1 V0.11→V0.12 | M006-047 | 同一文件内容变化必须递增自身版本；精确只变标题版本 |
| L96去“均”，加“或已记录对应接入开关关闭” | M006-047 | 与定稿候选完全一致；保留8、npm入口、映射分支字段、后半防伪生产边界 |

本席从before-tree原文按批准的两个精确字符串变换重建预期，实算expected==after-tree==当前源；变化行号只有1、96，所有npm引用逐项相等。没有未登记行、没有额外文件、没有移动/删除；不存在越界需回滚项。
GEN-002/016/018：当前真实正常路径中8项traceability都走disabled分支，但原句保证“均有责任/manifest”，现句明确容纳该分支。重新读取生成器166–206、227–268、408–421：238允许全关闭时跳过manifest要求，246算traceable，267policy明确OR；两分支被说明覆盖。不是将disabled变成生产证据，policy仍mock_or_sandbox_is_not_production_evidence，L96后半保持。无需改M414生成器/快照/开关。
GEN-017：B2已双核细化，有PGM066第三方未定/各端开关及BEQR HTML:210原文上位；无新业务规则、角色、状态、价格或审批。B4仅修字义，没扩大业务权限，不改锁/HTML。
GEN-001：选择收窄原一句，未增加脚本、测试文件、门禁或重复说明。Markdown类型4，不额外造测试。

## 本次新鲜验证
在Grok真实结束后，本席直接SSH重新读取两树完整diff、当前两源与三HTML SHA；重新实跑verify:delivery-gates-docs、verify:external-evidence-requirement-traceability，均exit0。真实Markdown解析47行均2列、无空格单元，npm引用完全保持。
首个通用打印小脚本尝试requirements/rows字段得到空数组，仅是调试辅助字段选择错误，不作为快照为空的证据；随后读顶层keys确定实际字段requirement_traceability，完整读出8行，当前mapped_item_ids/owner_groups/target_manifest_paths全空，switch_disabled非空，状态全covered_by_disabled_integration_switches。实际输出两段均保留，避免把[]冒充业务事实。无需改任何项目文件。
delivery现V0.12/15751bytes/SHA0615cd1b3beaffc097f96cc6140b16bfa3ec0a6be4e91c493dca2f0fa6217e09；deploy仍V0.07/7117bytes/SHA543a1543ca264f0741706fb5007d813f67ea84dd3acaafe40c2380cce9babda1。三HTML SHA与冻结来源不变，详下。
未执行父链49项、真实GitHubPUT/部署/推送；文档型最小验证不声称生产上线或其它工具全模块通过。既有源码非本次diff没有被“顺手修复”。本次无待答questions/HTML自身问题/新判据。
两席本次完整raw落齐后再读Grok作合取；仅双PASS可将047出队并启动B5-r2。

## 完整diff与新鲜命令输出

````text
CHECK_ACTUAL_LOCK_SHA: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
TREE_BEFORE b50b33c1f1c61ab9a1c87f6cf79ea97ffa593ccd TREE_AFTER ece59cbd0b81d612f3cc6844d389415cb9394d31 ALL_PATHS ["docs/delivery-readiness-gates-v0.1.md"]
FULL_UNFILTERED_DIFF
diff --git a/docs/delivery-readiness-gates-v0.1.md b/docs/delivery-readiness-gates-v0.1.md
index 88a00e9a..d262d35e 100644
--- a/docs/delivery-readiness-gates-v0.1.md
+++ b/docs/delivery-readiness-gates-v0.1.md
@@ -1,4 +1,4 @@
-# 交付总闸口与上线准入验证 V0.11
+# 交付总闸口与上线准入验证 V0.12
 
 ## 1. 定位
 
@@ -93,7 +93,7 @@ npm run verify:delivery-gates
 | `npm run verify:production-evidence-draft-prep` | 校验生产证据 manifest 草稿准备器默认 dry-run、拒绝越界写入和覆盖已有 manifest，且不会关闭生产阻断 |
 | `npm run verify:production-evidence-draft-prep-fixtures` | 在临时目录实测草稿准备器 dry-run 不写文件、execute 复制模板、再次 execute 不覆盖已有 manifest，并拒绝无选择器的 broad execute |
 | `npm run verify:production-closure-gap-reports` | 从当前运行态、Semgrep、NAS 当前状态、生产证据提交状态和外部证据追踪校验生产闭环缺口总报告，防止旧报告或手改报告伪关闭生产证据 |
-| `npm run verify:external-evidence-requirement-traceability` | 校验 8 条外部证据型 partial 需求均已追踪到责任组、证据 item_id、模板和目标 manifest 路径，防止 mock/沙箱被误判为生产闭环 |
+| `npm run verify:external-evidence-requirement-traceability` | 校验 8 条外部证据型 partial 需求已追踪到责任组、证据 item_id、模板和目标 manifest 路径，或已记录对应接入开关关闭，防止 mock/沙箱被误判为生产闭环 |
 | `npm run verify:nas-it-handoff` | 校验 NAS IT 受控挂载交接包、默认 dry-run 脚本、manifest 映射和无明文密钥边界 |
 | `npm run verify:nas-diagnostics` | 校验 NAS 只读诊断报告与当前网络、445 端口、CIFS 挂载和 preflight 根因一致，不关闭生产 manifest |
 | `npm run verify:nas-current-state-drift` | 校验 NAS 历史通过证据没有被误当作当前生产自动挂载、定时备份、告警和恢复演练闭环 |

EXACT_APPROVED_TRANSFORM true; both changed lines belong M006-047
CHANGED_LINES [1, 96]
MARKDOWN 47 rows all2cols noempty; NPM REFERENCES IDENTICAL
CMD npm run verify:delivery-gates-docs EXIT 0

> entertainment-complex-management@0.1.0 verify:delivery-gates-docs
> node tools/verify-delivery-gates.mjs

DELIVERY_GATES_DOCS_PASS
 
CMD npm run verify:external-evidence-requirement-traceability EXIT 0

> entertainment-complex-management@0.1.0 verify:external-evidence-requirement-traceability
> node tools/generate-external-evidence-requirement-traceability.mjs --check

EXTERNAL_EVIDENCE_REQUIREMENT_TRACEABILITY_PASS
 
TRACE_COUNTS {"partial_requirements": 8, "traceable_partial_requirements": 8, "mapped_evidence_items": 0, "mapped_ledger_rows": 0, "p0_evidence_rows": 0, "awaiting_evidence_rows": 0, "invalid_evidence_rows": 0, "valid_evidence_rows": 0, "production_blocking_requirements": 0}
POLICY {"no_fake_external_evidence_closure": true, "mock_or_sandbox_is_not_production_evidence": true, "partial_requirement_must_map_to_owner_manifest_or_disabled_switch": true, "integration_capability_switches_applied": true, "raw_evidence_stays_external": true, "credential_ref_only_no_plaintext_secret": true, "this_report_does_not_close_blockers": true}
TRACE_ROWS []
CURRENT_SOURCE docs/delivery-readiness-gates-v0.1.md 15751 0615cd1b3beaffc097f96cc6140b16bfa3ec0a6be4e91c493dca2f0fa6217e09
CURRENT_SOURCE docs/deploy-and-change-flow-v0.1.md 7117 543a1543ca264f0741706fb5007d813f67ea84dd3acaafe40c2380cce9babda1
CURRENT_SOURCE docs/business-enum-quick-reference-v0.1.html 133961 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66
CURRENT_SOURCE docs/business-system-map-for-operations-v0.1.html 304482 a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa
CURRENT_SOURCE docs/platform-governance-map-for-operations-v0.1.html 103015 c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c
B4_REPAIR_PASS SCOPE_PASS NO_NEW_FINDINGS

SNAPSHOT_KEYS ['traceability_version', 'status', 'generated_from', 'policy', 'submission_status_counts', 'counts', 'requirement_traceability']
ACTUAL_ROW_KEY requirement_traceability COUNT 8
[{"requirement_id": "REQ-17", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-PAY-001", "PRD-PAY-002", "PRD-FACE-001", "PRD-CRED-3P-001", "PRD-CRED-OBJ-001"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-21", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-PAY-001", "PRD-PAY-002"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-23b", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-FACE-001", "PRD-CRED-3P-001"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-25", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-PAY-001", "PRD-PAY-002"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-32", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-CRED-OBJ-001", "PRD-3P-001", "PRD-CRED-3P-001", "SEC-P1-WEBHOOK-REPLAY-001"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-40", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-3P-001", "PRD-CRED-OPEN-001", "PRD-CRED-3P-001", "SEC-P1-WEBHOOK-REPLAY-001"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-41", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-3P-001", "PRD-CRED-3P-001", "SEC-P1-WEBHOOK-REPLAY-001"], "production_trace_status": "covered_by_disabled_integration_switches"}, {"requirement_id": "REQ-44", "mapped_item_ids": [], "owner_groups": [], "target_manifest_paths": [], "switch_disabled_item_ids": ["PRD-3P-001", "PRD-CRED-3P-001", "SEC-P1-WEBHOOK-REPLAY-001", "PRD-MDM-001", "SEC-P0-MDM-DEVICE-001"], "production_trace_status": "covered_by_disabled_integration_switches"}]
DIRECT_SOURCE tools/generate-external-evidence-requirement-traceability.mjs
166:const traceRows = partialRequirements.map((requirement) => {
167:  const requirementId = requirement["需求编号"];
168:  const mapping = evidenceMappings[requirementId];
169:  const rawItemIds = mapping?.evidence_item_ids ?? [];
170:  const effectiveItemIds = rawItemIds.filter((itemId) => !disabledEvidenceItemIds.has(itemId) && !disabledSecurityFindingIds.has(itemId));
171:  const switchDisabledItemIds = rawItemIds.filter((itemId) => !effectiveItemIds.includes(itemId));
172:  const evidenceRows = effectiveItemIds.flatMap((itemId) => ledgerByItemId.get(itemId) ?? []);
173:  const missingItemIds = effectiveItemIds.filter((itemId) => !ledgerByItemId.has(itemId));
174:  const awaiting = evidenceRows.filter((row) => row.current_status === "awaiting_submission").length;
175:  const invalid = evidenceRows.filter((row) => row.current_status === "invalid_submission").length;
176:  const valid = evidenceRows.filter((row) => row.current_status === "valid_submission").length;
177:  const p0Rows = evidenceRows.filter((row) => row.priority === "P0");
178:  const ownerGroups = [...new Set(evidenceRows.map((row) => row.owner_group))].sort((a, b) => a.localeCompare(b, "zh-Hans-CN"));
179:  const productionBlocking = effectiveItemIds.length > 0 && p0Rows.some((row) => row.current_status !== "valid_submission");
180:  return {
181:    requirement_id: requirementId,
182:    module: requirement["业务模块"],
183:    acceptance: requirement["验收标准"],
184:    current_status: requirement["当前实现状态"],
185:    matrix_gap: requirement["缺口"],
186:    evidence_mapping_reason: mapping?.reason ?? "",
187:    mapped_item_ids: effectiveItemIds,
188:    switch_disabled_item_ids: switchDisabledItemIds,
189:    owner_groups: ownerGroups,
190:    evidence_rows_total: evidenceRows.length,
191:    p0_evidence_rows: p0Rows.length,
192:    awaiting_evidence_rows: awaiting,
193:    invalid_evidence_rows: invalid,
194:    valid_evidence_rows: valid,
195:    missing_item_ids: missingItemIds,
196:    production_blocking: productionBlocking,
197:    production_trace_status: effectiveItemIds.length === 0
198:      ? "covered_by_disabled_integration_switches"
199:      : missingItemIds.length > 0
200:        ? "traceability_missing_manifest"
201:        : invalid > 0
202:          ? "invalid_evidence_manifest_blocks"
203:          : awaiting > 0
204:            ? "awaiting_real_external_evidence"
205:            : "all_mapped_evidence_valid",
206:    target_manifest_paths: [...new Set(evidenceRows.map((row) => row.target_manifest_path).filter(Boolean))],
227:const failures = [];
228:for (const requirement of partialRequirements) {
229:  if (!evidenceMappings[requirement["需求编号"]]) failures.push(`${requirement["需求编号"]} is partial but has no external evidence mapping`);
230:}
231:for (const [requirementId, mapping] of Object.entries(evidenceMappings)) {
232:  if (!requirementRows.some((row) => row["需求编号"] === requirementId)) failures.push(`${requirementId} mapping does not exist in ${matrixPath}`);
233:  for (const itemId of mapping.evidence_item_ids.filter((id) => !disabledEvidenceItemIds.has(id) && !disabledSecurityFindingIds.has(id))) {
234:    if (!ledgerByItemId.has(itemId)) failures.push(`${requirementId} maps to ${itemId}, but ${itemId} is missing from ${ledgerPath}`);
235:  }
236:}
237:for (const row of traceRows) {
238:  if (row.mapped_item_ids.length === 0 && row.switch_disabled_item_ids.length > 0) continue;
239:  if (row.evidence_rows_total === 0) failures.push(`${row.requirement_id} has no evidence ledger rows`);
240:  if (row.missing_item_ids.length > 0) failures.push(`${row.requirement_id} has missing item ids: ${row.missing_item_ids.join(", ")}`);
241:  if (row.target_manifest_paths.length === 0) failures.push(`${row.requirement_id} has no target manifest paths`);
242:}
243:
244:const counts = {
245:  partial_requirements: partialRequirements.length,
246:  traceable_partial_requirements: traceRows.filter((row) => (row.evidence_rows_total > 0 && row.missing_item_ids.length === 0) || (row.mapped_item_ids.length === 0 && row.switch_disabled_item_ids.length > 0)).length,
247:  mapped_evidence_items: new Set(traceRows.flatMap((row) => row.mapped_item_ids)).size,
248:  mapped_ledger_rows: traceRows.reduce((sum, row) => sum + row.evidence_rows_total, 0),
249:  p0_evidence_rows: traceRows.reduce((sum, row) => sum + row.p0_evidence_rows, 0),
250:  awaiting_evidence_rows: traceRows.reduce((sum, row) => sum + row.awaiting_evidence_rows, 0),
251:  invalid_evidence_rows: traceRows.reduce((sum, row) => sum + row.invalid_evidence_rows, 0),
252:  valid_evidence_rows: traceRows.reduce((sum, row) => sum + row.valid_evidence_rows, 0),
253:  production_blocking_requirements: traceRows.filter((row) => row.production_blocking).length,
254:};
255:
256:const output = {
257:  traceability_version: "v0.1",
258:  status: failures.length > 0
259:    ? "traceability_gap"
260:    : counts.production_blocking_requirements > 0
261:      ? "traceable_but_awaiting_real_external_evidence"
262:      : "all_mapped_external_evidence_valid",
263:  generated_from: [matrixPath, ledgerPath, statusPath],
264:  policy: {
265:    no_fake_external_evidence_closure: true,
266:    mock_or_sandbox_is_not_production_evidence: true,
267:    partial_requirement_must_map_to_owner_manifest_or_disabled_switch: true,
268:    integration_capability_switches_applied: true,
408:if (checkMode) {
409:  const checkFailures = [...failures];
410:  for (const relativePath of [jsonPath, csvPath, mdPath]) {
411:    if (!fs.existsSync(absolute(relativePath))) checkFailures.push(`${relativePath} is missing; run npm run docs:external-evidence-requirement-traceability`);
412:  }
413:  if (checkFailures.length === 0) {
414:    if (normalize(readText(jsonPath)) !== json) checkFailures.push(`${jsonPath} is not in sync`);
415:    if (fs.readFileSync(absolute(csvPath), "utf8").replace(/\r\n/g, "\n") !== csv) checkFailures.push(`${csvPath} is not in sync`);
416:    if (normalize(readText(mdPath)) !== md) checkFailures.push(`${mdPath} is not in sync`);
417:  }
418:  if (counts.partial_requirements !== counts.traceable_partial_requirements) checkFailures.push("not every partial requirement is traceable to owner manifests");
419:  if (counts.partial_requirements === 0) checkFailures.push("expected at least one external evidence partial requirement while production evidence is open");
420:  if (checkFailures.length > 0) failWith(checkFailures);
421:  console.log("EXTERNAL_EVIDENCE_REQUIREMENT_TRACEABILITY_PASS");

````
