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
