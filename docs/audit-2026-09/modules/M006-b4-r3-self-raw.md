# M006 B4-r3 Codex 修复内容审计原始报告

版本：V0.01
模块：M006 `10-backend-main-access-01`，6/322。
步骤：B4；ledger IN_PROGRESS；B3已执行3次；方案复核已修2；运行失败0。
审计结论：PASS。范围归因结论：PASS。仅本方票，不等于B4双PASS或模块PASS。
CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

## 输入与独立顺序

本批B2由两席完整B5-r2报告回退，全部3条已统一归并：031/032队列是，033细化且工程缺陷否、队列否；Grok B2 V0.06独立接受033事实更正，原B5意见完整保留。findings V0.62/SHA abdf0b89301bd74b50b1f25c6218085e033114fc2f25e28da1b50044b62d6f2f；旧30项不重新入队。
本轮先派Grok：2026-09-06 04:16:26 UTC，既有grok-4.6/xhigh及回执适配器，目标 modules/M006-b4-r3-grok-raw.md；本方落盘前未读Grok原始JSON、stderr、raw正文或意见，真实exit尚未取得。此后独立核对完整diff、必要直接调用边界，再写本报告。
before tree 11319ccfc6c0a3b609bc1cfdd4579766a7118705
after tree c77d4453db7eb9b92388de630fdd5121cdef7959
完整未过滤git diff --binary：14687字节，SHA a04dfea8324fd7a11d5b5cfebf3585fd93c9cb32ec3cd05fde2bb8b610956717，6路径/12hunk。临时索引覆盖tracked/staged/untracked，真实索引SHA824f33c20ee9b3c04744aadfede5f7bdbb2303a35b0035b1f8496ad729cd0322前后未改。
B3前后四值实算均等于上方锁值，④来自触发本批回退的M006-b5-r2-grok-raw.md最后一条对应行。HTML三份与HEAD逐字相同；控制提示词V0.17/SHA c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21未变。未更改锁、scope、模块映射、业务开关或数据库结构。

## 整份diff逐块归因

| 文件/块 | 正式ID | 每一行的必要性 |
|---|---|---|
| AccessService：版本头、921错误分支，共2块 | M006-032 | 只删除给人看的原始英文账号类型/可选枚举拼接，改中文；保留错误码与原允许值集合。版本按用户规则递增。 |
| AuthorizationService：版本头、193主体范围，共2块 | M006-031 | 删除非商户主体共用p的计算；复用currentActor承接已有AccessActor/UserRecord，已有ID+组织形成范围；商户及无认证系统范围保留；拒绝未知认证主体，不回退共享空间。版本随根因修复。 |
| CallerScopeResolver：唯一块 | M006-031 | 沿用既有函数式接口，加默认legacyScope以保持原有lambda/实现的范围；让新范围能识别其旧记录。没有新增接口文件或表。旧说明已不适用于本方修复，按实际语义收窄；补文件版本。 |
| IdempotencyService：版本、execute入口、scope私有方法，共3块 | M006-031 | 复用hashJson为变化后的主体+操作生成有界键（现表operation_id VARCHAR128）；在占位前检查旧共享记录；无法确认归属则用已有冲突错误拒绝重复执行和错误结果返回。原reserve/CAS/事务/complete/replay不改。私有方法收取已解析scope，避免把旧scope再次计算为新scope。 |
| M006SecurityRegressionTests：版本、末尾两测试，共2块 | M006-031、M006-032 | 版本同批变更。第一个新增方法全部归031：真实controller+Service+Authorization+Idempotency，两组织同合法编号/同body各写一次，同主体重试返回同ID且总写入2。第二个全部归032：真实controller错误经真实GlobalExceptionHandler，断言中文消息不含拉丁字母。间隔空行与注释分别随对应方法。 |
| IdempotencyServiceTests：版本、新测试，共2块 | M006-031 | 旧OK/IN_PROGRESS/FAILED_RETRYABLE记录均保持原样、不执行回调；新范围新请求可正常首次+重试一次；用正常数据库标识长度检查operation_id不超过128。默认接口方法在首个红测试时尚不存在，测试匿名实现预先声明同名方法而不写@Override，因此红测是行为断言失败，不是编译失败。 |

所有12块均归已定稿且当前队列为是的031/032；没有033代码改动、没有无ID改进。范围PASS。

## 根因与三线复核

本轮只审上述完整修复diff及直接必要边界，不把B4称为全模块B5。

- 第一性原理：回放查询键必须区分真实主体；旧表只有operation_id/idempotency_key/request_hash/response等字段，没有可靠的操作人归属。新scope依据服务端已识别的主体ID与organizationId，长度前缀使ID/组织连接可区分；IdempotencyService再用既有JSON摘要生成67字符键，不改128字符列。旧共享记录存在时不猜归属、不回放其正文、也不重新执行；409提示先核实原操作。这是保留已有成功效果的必要兼容限制，不承诺旧共享记录仍能自动重放成功响应。
- 顺从式：GEN-002/004/005/007/009/010与BSM-028/114、PGM-001：两个组织都可完成各自首次创建；同主体重试只执行一次；已有成功记录不能因前缀变化再次生效。GEN-002/015、PGM-001/066：账号类型失败说明全中文，原错误码和类别规则保持。只用冻结判据。
- 反例核验：真实双组织请求在红测只写org-a，org-b断言失败；绿测两个组织角色ID不同，各自重试ID保持，总数2。旧共享记录3种状态都在回调前拒绝，原记录不改，新请求继续可用。中文反例在真实错误响应中出现platform_admin等，绿测不再含拉丁字母。
- 调用边界：先rg全体CallerScopeResolver及IdempotencyService引用与execute调用，确认只有AuthorizationService生产实现，公共服务和AuditedOperationExecutor都走同一修复入口。生产Bearer过滤器写UserRecord/AccessActor，客人过滤器写AccessActor；两个调度Job已有SCHEDULER_ACTOR。额外实际核验两种主体表示范围相同、merchant原范围和空认证系统原范围保留（4断言PASS）。
- 公开入口必要核对：SecurityConfig仍有默认匿名上下文；不能把匿名当业务主体。本次识别未知主体改为明确失败，检查了受影响共享执行入口：客人写操作先requireCustomer并由CustomerTokenAuthenticationFilter绑定客人；开放回调IntegrationService使用endpoint/provider自身事件去重，ThirdPartyScaffoldingService也没有公共IdempotencyService执行调用；自助机heartbeat直接保存并校验设备凭据，不走本方法。没有把这些外围文件纳为新的全审模块，未修改它们。

已逐行读完整diff；再读当前CallerScopeResolver与IdempotencyService全文，核当前AuthorizationService修改块和其currentActor，原AccessService创建角色/账号类型/异常响应调用链在本批B5、B2已完整核实，当前除已列一行消息外未变。当前6文件与after tree一致；其余M006/B3源保持上一冻结内容。

## 实际验证与边界

当前60源fresh javac完成后，红测95PASS/3FAIL，失败均为行为断言；改生产实现后同一命令98PASS/0FAIL（M006Security33、AccessService35、Authorization21、Idempotency9）。全后端test-compile真实exit0，表示完整编译，不称跑了全部后端测试。B4追加主体兼容核验4断言PASS，临时文件自动清理。
未运行共享数据库迁移；未重复前端测试（本轮无前端改动）；未声称真实厂商支付/SMS已接入。既有B3-r2的377测试为历史证据，本票不把它们计作本轮实跑。
按本会话已读技能实际执行systematic-debugging/diagnosing-bugs根因追查、TDD先红后绿、requesting-code-review仅派Grok、receiving-code-review整批归并、verification-before-completion证据先行。using-superpowers已预检；grilling仅自问自答；codebase-design用于已有公共入口；ponytail采用现有接口/摘要和最小错误说明；无第三席。未核到utf8_guard.py，不声称运行；6文件严格UTF8解码且无替换符实际通过。

## 验证命令与完整输出

### 先红后绿（同一条命令）

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

红测：
```text
JAVAC_EXIT 0 SOURCES 60
 Note: backend/src/main/java/com/complex/platform/access/staff/StaffService.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.

FAIL com.complex.platform.access.M006SecurityRegressionTests.companyRequestsHaveIndependentResultsAndRetryOnlyOnce: java.lang.AssertionError: 
Expecting actual:
  ["org-a"]
to contain exactly in any order:
  ["org-a", "org-b"]
but could not find the following elements:
  ["org-b"]

FAIL com.complex.platform.access.M006SecurityRegressionTests.invalidAccountTypeResponseIsChinese: java.lang.AssertionError: 
Expecting actual:
  "账号类型「platform_admin」不允许，可选值：[sales, supervisor, outsourced_vendor, merchant_admin, merchant, staff]"
not to contain pattern:
  "[A-Za-z]"
com.complex.platform.access.M006SecurityRegressionTests 31 PASS / 2 FAIL
com.complex.platform.access.AccessServiceTests 35 PASS / 0 FAIL
com.complex.platform.access.AuthorizationServiceTests 21 PASS / 0 FAIL
FAIL com.complex.platform.common.idempotency.IdempotencyServiceTests.changedCallerScopePreservesOldOperationsAndBoundsNewKeys: java.lang.AssertionError: 
Expecting code to raise a throwable.
com.complex.platform.common.idempotency.IdempotencyServiceTests 8 PASS / 1 FAIL
RESULT 95 PASS / 3 FAIL
 Mockito is currently self-attaching to enable the inline-mock-maker. This will no longer work in future releases of the JDK. Please add Mockito as an agent to your build as described in Mockito's documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org.mockito/org/mockito/Mockito.html#0.3
WARNING: A Java agent has been loaded dynamically (/home/james/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.10/byte-buddy-agent-1.18.10.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended


```

绿测：
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


```

### 完整编译

```powershell
ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && mvn -o -q -f backend/pom.xml test-compile -DskipTests'
```
```text
exit_code=0
stdout/stderr为空
```

### 主体兼容核验

```powershell
@'
# V0.01 read-only runtime caller compatibility verification
from pathlib import Path
import subprocess,tempfile
source="import com.complex.platform.access.*;\nimport com.complex.platform.auth.AuthService;\nimport org.springframework.security.authentication.UsernamePasswordAuthenticationToken;\nimport org.springframework.security.core.context.SecurityContextHolder;\nimport java.time.Instant;\nimport java.util.List;\npublic class ScopeCheck {\n static void bind(Object p) { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p, null, List.of())); }\n public static void main(String[] args) {\n  AuthorizationService service=new AuthorizationService(null);\n  try {\n   bind(new AccessActor(\"user\", \"name\", \"staff\", \"org\", null, \"active\"));\n   String actorScope=service.resolve();assert service.legacyScope().equals(\"p\");\n   bind(new AuthService.UserRecord(\"user\", \"name\", \"unused\", \"staff\", \"org\", null, \"active\", Instant.now()));\n   assert actorScope.equals(service.resolve());\n   bind(new AccessActor(\"user\", \"name\", \"merchant\", \"org\", \"merchant-1\", \"active\"));\n   assert service.resolve().equals(\"m:merchant-1\");\n   SecurityContextHolder.clearContext();assert service.resolve().isEmpty();\n   System.out.println(\"SCOPE_CHECK 4 ASSERTIONS PASS\");\n  } finally { SecurityContextHolder.clearContext(); }\n }\n}"
with tempfile.TemporaryDirectory(prefix="m006-b4-scope-",dir=".scratch") as d:
 root=Path(d).resolve();cpfile=root/"classpath"
 r=subprocess.run(["mvn","-o","-q","-f","backend/pom.xml","dependency:build-classpath","-Dmdep.outputFile="+str(cpfile)],capture_output=True,text=True)
 if r.returncode:print(r.stdout,r.stderr);raise SystemExit(r.returncode)
 cp=str(Path("backend/target/classes").resolve())+":"+cpfile.read_text().strip()
 p=root/"ScopeCheck.java";p.write_text(source,encoding="utf-8")
 r=subprocess.run(["javac","-encoding","UTF-8","--release","21","-cp",cp,"-d",str(root),str(p),"backend/src/main/java/com/complex/platform/access/AuthorizationService.java","backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java"],capture_output=True,text=True)
 print("JAVAC_EXIT",r.returncode);print(r.stdout,r.stderr)
 if r.returncode:raise SystemExit(r.returncode)
 r=subprocess.run(["java","-ea","-cp",str(root)+":"+cp,"ScopeCheck"],capture_output=True,text=True)
 print(r.stdout,r.stderr);raise SystemExit(r.returncode)

'@ | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -'
```
```text
JAVAC_EXIT 0
 
SCOPE_CHECK 4 ASSERTIONS PASS
 

```

### Grok本轮实际调用

```powershell
$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$auditPrompt = '仅执行 M006 第3次整批修复后的B4：只读审核本轮完整diff，不执行B3或B5，不重开其他模块。模块M006/322，ledger IN_PROGRESS，B3=3。当前定稿findings V0.62（031/032队列是；其他31项否），本次B2双方PASS已落B1self V0.15和B1grok V0.06。本轮before tree 11319ccfc6c0a3b609bc1cfdd4579766a7118705，after tree c77d4453db7eb9b92388de630fdd5121cdef7959；git diff --binary这两个tree的完整未过滤输出为14687字节、SHA256 a04dfea8324fd7a11d5b5cfebf3585fd93c9cb32ec3cd05fde2bb8b610956717、6路径12hunk。实算对照后必须读完整diff，写入本次raw全文和逐块ID归因。031：AuthorizationService主体+组织范围；复用CallerScopeResolver加默认legacyScope以保留既有实现；IdempotencyService复用hashJson把新范围键压到既有VARCHAR128并核对旧共享记录，旧归属不明记录报现有conflict而不返回其内容或再次执行，同主体新请求可重试；对应M006SecurityRegressionTests公司角色同编号与IdempotencyServiceTests旧请求/键长检查。032：AccessService非法账号类型中文提示，对应真实controller→GlobalExceptionHandler响应断言。所有文件自身版本头随改动更新，归所属ID。不得只看HEAD已提交差异，不漏untracked；不要新增判据或要求改冻结scope。核根因修复、行为正确性、必要直接调用边界和每行归因，整份diff审完收齐全部意见后才给审计PASS/FAIL和范围PASS/FAIL。当前真实验证：60源fresh javac，先95PASS/3FAIL，修复后98PASS/0FAIL；全后端test-compile -DskipTests真实exit0。不可把这些自审证据当你已实跑，你可独立验证但只用仓库.scratch临时隔离且清理自己的临时文件，不编译共享target，不写共享业务库、不修改源码/HTML/锁/台账，不创建额外审计产物，不派第三席。必要source依赖包含合法B3新文件，阶段A冻结scope不变。先实际预检适用技能，按完整V0.17全部B4规则。操作员明确这是对自己写/修改代码的开发调试和BUG质量修复，问题用具体行为/影响描述。完整报告追加到指定 docs/audit-2026-09/modules/M006-b4-r3-grok-raw.md，若不存在创建；版本从V0.01起，若已有内容须完整保留前缀再追加本次完整报告。写独立实算CRITERIA-LOCK-SHA256独占行。CLI不承担回执标记，适配器生成。以下V0.17完整原文继续适用：
' + [IO.File]::ReadAllText('D:\AI\ccwork\160-audit-prompt.md', [Text.Encoding]::UTF8)
@{prompt=$auditPrompt} | ConvertTo-Json -Compress | ssh -o BatchMode=yes james@192.168.88.160 'cd /home/james/complex-management && python3 -c ''import sys,json,subprocess,pathlib
root=pathlib.Path("/home/james/complex-management").resolve()
p=root/"docs/audit-2026-09/modules/M006-b4-r3-grok-raw.md"
def meta():
 if not p.exists(): return None
 s=p.stat(); return (s.st_size,s.st_mtime_ns,s.st_ctime_ns,s.st_ino)
before=meta()
prompt=json.load(sys.stdin)["prompt"]
r=subprocess.run(["/home/james/.local/bin/grok","-p",prompt,"-m","grok-4.6","--reasoning-effort","xhigh","--output-format","json","--permission-mode","bypassPermissions","--no-subagents","--disable-web-search","--cwd",str(root)],cwd=root,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
code=r.returncode
err="GROK_EXIT" if code!=0 else "PATH_MISMATCH" if not p.resolve().is_relative_to(root) or p.is_symlink() else "REPORT_MISSING" if not p.is_file() else "REPORT_EMPTY" if p.stat().st_size==0 else "REPORT_STALE" if meta()==before else None
print("ADAPTER_ERROR="+err if err else "REPORT_PATH=docs/audit-2026-09/modules/M006-b4-r3-grok-raw.md")
print("EXIT_CODE="+str(code))
sys.exit(1 if err else 0)
'''
```

本方落盘时尚未取得真实exit，报告正文未读取。

## 本轮完整diff（未过滤）

```diff
diff --git a/backend/src/main/java/com/complex/platform/access/AccessService.java b/backend/src/main/java/com/complex/platform/access/AccessService.java
index 37ddec45..aded7caf 100644
--- a/backend/src/main/java/com/complex/platform/access/AccessService.java
+++ b/backend/src/main/java/com/complex/platform/access/AccessService.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.12
+// 文件版本：V0.13
 
 import com.complex.platform.access.dto.AccessGrantApplyRequest;
 import com.complex.platform.access.dto.AccessGrantReviewRequest;
@@ -920,7 +920,7 @@ public class AccessService {
                         String accountType = normalizeAccountType(trimToDefault(request.accountType(), "staff"));
                         if (!ALLOWED_ACCOUNT_TYPES.contains(accountType)) {
                             throw ApiException.badRequest("INVALID_ACCOUNT_TYPE",
-                                    "账号类型「" + accountType + "」不允许，可选值：" + ALLOWED_ACCOUNT_TYPES);
+                                    "账号类型不允许，请重新选择");
                         }
                         String organizationId = requireText(request.organizationId(), "organization_id");
                         if (!authorizationService.isPlatformAdmin(actor)
diff --git a/backend/src/main/java/com/complex/platform/access/AuthorizationService.java b/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
index 9ed82813..7221c16f 100644
--- a/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
+++ b/backend/src/main/java/com/complex/platform/access/AuthorizationService.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.03
+// 文件版本：V0.04
 
 import com.complex.platform.auth.AuthService;
 import com.complex.platform.common.api.ApiException;
@@ -190,32 +190,30 @@ public class AuthorizationService implements CallerScopeResolver {
         throw ApiException.unauthorized("UNAUTHENTICATED", "无法识别的认证主体");
     }
 
-    /**
-     * Returns the caller scope token for idempotency key isolation.
-     * "m:{merchantId}" for merchant users, "p" for platform admins/staff.
-     * Returns "" on any error so IdempotencyService falls back to unscoped (safe degradation).
-     */
+    /** 非商户请求按已有主体及组织隔离；商户与系统请求沿用原范围。 */
     @Override
     public String resolve() {
-        try {
-            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
-            if (auth == null || !auth.isAuthenticated()) {
-                return "";
-            }
-            Object principal = auth.getPrincipal();
-            String merchantId = null;
-            if (principal instanceof AccessActor actor) {
-                merchantId = actor.merchantId();
-            } else if (principal instanceof AuthService.UserRecord user) {
-                merchantId = user.merchantId();
-            }
-            if (merchantId != null && !merchantId.isBlank()) {
-                return "m:" + merchantId;
-            }
-            return "p";
-        } catch (Exception e) {
+        String legacy = legacyScope();
+        if (!"p".equals(legacy)) {
+            return legacy;
+        }
+        AccessActor actor = currentActor();
+        if (actor.id() == null || actor.id().isBlank()) {
+            throw ApiException.unauthorized("UNAUTHENTICATED", "无法识别的认证主体");
+        }
+        return "u:" + actor.id().length() + ":" + actor.id() + ":"
+                + java.util.Objects.toString(actor.organizationId(), "");
+    }
+
+    @Override
+    public String legacyScope() {
+        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
+        if (auth == null || !auth.isAuthenticated()) {
             return "";
         }
+        AccessActor actor = currentActor();
+        String merchantId = actor.merchantId();
+        return merchantId == null || merchantId.isBlank() ? "p" : "m:" + merchantId;
     }
 
     private boolean knownPermission(String permissionCode) {
diff --git a/backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java b/backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java
index 8d4c9c08..a98a5ede 100644
--- a/backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java
+++ b/backend/src/main/java/com/complex/platform/common/idempotency/CallerScopeResolver.java
@@ -1,12 +1,14 @@
 package com.complex.platform.common.idempotency;
 
-/**
- * Resolves the current caller's scope token for idempotency key isolation.
- * Returns "m:{merchantId}" for merchant callers, "p" for platform admins,
- * or "" (empty) when no scope is available (e.g., in tests).
- * An empty scope leaves the operationId unmodified (backward-compatible).
- */
+// 文件版本：V0.01
+
+/** Resolves the trusted caller scope used to isolate idempotent operations. */
 @FunctionalInterface
 public interface CallerScopeResolver {
     String resolve();
+
+    /** Existing scopes keep their keys; changed scopes must also check their old records. */
+    default String legacyScope() {
+        return resolve();
+    }
 }
diff --git a/backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java b/backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java
index 65260597..05f286a3 100644
--- a/backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java
+++ b/backend/src/main/java/com/complex/platform/common/idempotency/IdempotencyService.java
@@ -1,4 +1,7 @@
 package com.complex.platform.common.idempotency;
+
+// 文件版本：V0.01
+
 import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;
 
 import com.complex.platform.common.api.ApiException;
@@ -70,7 +73,15 @@ public class IdempotencyService {
             Function<T, T> storedResponseTransformer
     ) {
         requireIdempotencyKey(idempotencyKey);
-        String scopedId = scopedOperationId(operationId);
+        String scope = callerScopeResolver.resolve();
+        String legacyScope = callerScopeResolver.legacyScope();
+        String legacyId = scopedOperationId(operationId, legacyScope);
+        boolean changedScope = !java.util.Objects.equals(scope, legacyScope);
+        String scopedId = changedScope ? "caller:" + hashJson(java.util.List.of(scope, operationId)) : legacyId;
+        // 旧共享记录没有可靠主体归属：保留结果，阻止串用或再次执行。
+        if (changedScope && repository.findByOperationIdAndIdempotencyKey(legacyId, idempotencyKey).isPresent()) {
+            throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "该请求编号存在旧处理记录，请先核实原操作结果");
+        }
         String requestHash = hashJson(request);
         Instant now = Instant.now();
         IdempotencyRecord reservation = new IdempotencyRecord(
@@ -111,8 +122,7 @@ public class IdempotencyService {
                 "请求正在处理中，请稍后刷新查看结果");
     }
 
-    private String scopedOperationId(String operationId) {
-        String scope = callerScopeResolver.resolve();
+    private String scopedOperationId(String operationId, String scope) {
         if (scope == null || scope.isEmpty()) {
             return operationId;
         }
diff --git a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
index cbf28cd3..7b63bf74 100644
--- a/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
+++ b/backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
@@ -1,6 +1,6 @@
 package com.complex.platform.access;
 
-// 文件版本：V0.19
+// 文件版本：V0.20
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 import static org.mockito.ArgumentMatchers.*;
@@ -592,4 +592,54 @@ class M006SecurityRegressionTests {
         assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class,
                 e -> assertThat(e.code()).isEqualTo("PERMISSION_DENIED"));
     }
+
+    // M006-031：实际控制器须按组织和操作人区分首次请求。
+
+    @Test void companyRequestsHaveIndependentResultsAndRetryOnlyOnce() {
+        var idem = new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper());
+        idem.setCallerScopeResolver(authorization);
+        service = new AccessService(repository, idem, new AuditEventService(new InMemoryAuditEventRepository()),
+                authorization, credentials, null, true, 3, 3);
+        when(repository.listPermissions()).thenReturn(List.of(
+                new PermissionDefinition(LOW, "查看", "ops", "medium", false, "active"),
+                new PermissionDefinition("access.role.manage", "管理角色", "access", "medium", false, "active")));
+        for(String id: List.of("manager-a", "manager-b")) {
+            effect(id, LOW, AccessRepository.Effect.GRANT, id+"-read", false);
+            effect(id, "access.role.manage", AccessRepository.Effect.GRANT, id+"-manage", false);
+        }
+        var controller = new AccessController(service, authorization);
+        var request = new RoleRequest("company-first-role", "正常首次创建", List.of(), "巡检员", "只读", List.of(LOW), "project");
+        actor("manager-a", "staff", "org-a", null);
+        var first = controller.createRole(request).data();
+        actor("manager-b", "staff", "org-b", null);
+        var result = controller.createRole(request);
+        assertThat(roles.values()).extracting(RoleRecord::organizationId).containsExactlyInAnyOrder("org-a", "org-b");
+        assertThat(result.data().id()).isNotEqualTo(first.id());
+        assertThat(controller.createRole(request).data().id()).isEqualTo(result.data().id());
+        actor("manager-a", "staff", "org-a", null);
+        assertThat(controller.createRole(request).data().id()).isEqualTo(first.id());
+        assertThat(roles).hasSize(2);
+    }
+
+    // M006-032：实际错误响应须保持中文。
+
+    @Test void invalidAccountTypeResponseIsChinese() throws Exception {
+        when(repository.listPermissions()).thenReturn(List.of(
+            new PermissionDefinition("access.user.manage", "管理账号", "access", "medium", false, "active")));
+        var controller = new AccessController(service, authorization);
+        ApiException failure = null;
+        try {
+            controller.createAccessUser(new AccessUserRequest("b5-invalid", "检查错误提示", List.of(),
+                "example", "password", null, "platform_admin", "org", null, "active"));
+        } catch(ApiException e) { failure = e; }
+        assertThat(failure).isNotNull();
+        assertThat(failure.code()).isEqualTo("INVALID_ACCOUNT_TYPE");
+        var handler = new com.complex.platform.common.api.GlobalExceptionHandler();
+        var method = handler.getClass().getDeclaredMethod("handleApiException", ApiException.class);
+        method.setAccessible(true);
+        var response = (org.springframework.http.ResponseEntity<?>) method.invoke(handler, failure);
+        var body = (com.complex.platform.common.api.ApiResponse<?>) response.getBody();
+        assertThat(body.message()).contains("账号类型").doesNotContainPattern("[A-Za-z]");
+    }
+
 }
diff --git a/backend/src/test/java/com/complex/platform/common/idempotency/IdempotencyServiceTests.java b/backend/src/test/java/com/complex/platform/common/idempotency/IdempotencyServiceTests.java
index 02bda0c1..f604c646 100644
--- a/backend/src/test/java/com/complex/platform/common/idempotency/IdempotencyServiceTests.java
+++ b/backend/src/test/java/com/complex/platform/common/idempotency/IdempotencyServiceTests.java
@@ -1,5 +1,7 @@
 package com.complex.platform.common.idempotency;
 
+// 文件版本：V0.01
+
 import static org.assertj.core.api.Assertions.assertThat;
 import static org.assertj.core.api.Assertions.assertThatThrownBy;
 
@@ -300,6 +302,38 @@ class IdempotencyServiceTests {
         assertThat(responseB).isNotEqualTo(new TestResponse("secret-A"));
     }
 
+
+    // M006-031：范围变更不能重放归属不明的旧结果，也不能再次执行旧请求。
+    @Test
+    void changedCallerScopePreservesOldOperationsAndBoundsNewKeys() {
+        service.setCallerScopeResolver(new CallerScopeResolver() {
+            public String resolve() { return "u:" + "user".repeat(16) + ":" + "organization".repeat(6); }
+            public String legacyScope() { return "p"; }
+        });
+        TestRequest request = new TestRequest("same-payload");
+        AtomicInteger actions = new AtomicInteger();
+        for (String code : java.util.List.of("OK", "IN_PROGRESS", "FAILED_RETRYABLE")) {
+            IdempotencyRecord old = new IdempotencyRecord(UUID.randomUUID(), "p:operation", code,
+                    hashJson(request), code, code.equals("OK") ? "{\"value\":\"old-result\"}" : null,
+                    code.equals("OK") ? 200 : 202, Instant.now(), Instant.now().plusSeconds(86400));
+            repository.put(old);
+            assertThatThrownBy(() -> service.execute("operation", code, request,
+                    () -> { actions.incrementAndGet(); return new TestResponse("duplicate"); }, TestResponse.class))
+                    .isInstanceOfSatisfying(ApiException.class, failure ->
+                            assertThat(failure.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
+            assertThat(repository.findByOperationIdAndIdempotencyKey("p:operation", code)).contains(old);
+        }
+        assertThat(actions).hasValue(0);
+        String operation = "createRole:" + "r".repeat(64);
+        TestResponse first = service.execute(operation, "fresh", request,
+                () -> { actions.incrementAndGet(); return new TestResponse("created"); }, TestResponse.class);
+        TestResponse retry = service.execute(operation, "fresh", request,
+                () -> { actions.incrementAndGet(); return new TestResponse("duplicate"); }, TestResponse.class);
+        assertThat(retry).isEqualTo(first);
+        assertThat(actions).hasValue(1);
+        assertThat(repository.records.values()).allMatch(record -> record.operationId().length() <= 128);
+    }
+
     private String hashJson(Object value) {
         try {
             MessageDigest digest = MessageDigest.getInstance("SHA-256");

```


## V0.02 数字说明勘误
版本：V0.02。原V0.01独立报告与PASS/范围PASS原票完整保留；本备注在读完Grok后追加，未重做或替换独立审计。原报告将新键长度写为67字符，算术有误，应为caller:的7字符加64字符摘要，共71字符，仍小于既有128列长度。实际代码与测试均未使用67常量；不改变任何代码、判据或审计结论。
