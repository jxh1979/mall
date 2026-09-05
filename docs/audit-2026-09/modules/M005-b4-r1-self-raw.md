CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
# M005 B4-r1 本方完整修复差异复审
版本：V0.01

本席审计结论：PASS。越界判定：PASS。完整4路径8hunk均审完，六个定稿ID的实际修复及最小验证通过。本票不代表双席PASS或模块收口；尚未读Grok本轮正文。B3=1；模块M005/322，ledger IN_PROGRESS；运行失败重跑0。

## 依据及独立顺序

控制V0.17/SHA c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21；源HEAD 134d75d158ca6ff1343f616cce47676ecfc83e02，main主工作区。15:00:55 UTC已先派Grok B4-r1（session22277）；此报告在未读M005 B4 Grok raw、原始JSON或其分析前独立完成。请求审计参数未变，所有回执由既有内存适配器只报路径/真实退出码。双方不通过索引或当前全工作区diff接触对方raw。

定稿findings V0.49/172452字节/SHA 2c3c1bbcd5ea0da7ddeb5a1f1857a692cf52105c01e04f4cbd1b17d28397137f；M005-001—006当前队列是，007—012否。整批B2双方PASS证据在本模块b1 raw的V0.05/V0.03追加，输入V0.48候选；本次不改其定稿、不加ID。B3前后四值均全等原锁，COUNT0；本次再次验证actual/sidecar/ledger三值，无重写。

实际读requesting-code-review SKILL.md（本机claude-plugins-official/superpowers/6.3.0）；按合同只请求Grok，不用HEAD~1、不派第三席、不即刻修。verification-before-completion、TDD/tdd、ponytail和diagnosing已在本模块对应节点实际读取；验证证据如下，非口头引用替代工具。文档/配置按合同类型4最小可复现命令豁免生产测试文件，本报告保留可执行回归检查。

## 固定两树及全部差异

before: 4e82fa54f5fcf4589bf2c8ad526ab64c9d2c2a4e
after: 214490504e59405a1c6ef5f83f172a6f8be373fe
完整binary diff：3529字节；SHA256 42a88e82d010599b68b2e30414b2e35d2731cc5e6c6b81931ea55f4312779c26。
两次用尚不存在的临时GIT_INDEX_FILE、git add -A / git write-tree获得，包含tracked/staged/untracked。B3全部完成并取after后才加计数。此处真实执行 git diff --binary BEFORE AFTER，不按路径过滤，不新增第三个当前tree。

```diff
diff --git a/package-lock.json b/package-lock.json
index 7db9f06c..63ecc802 100644
--- a/package-lock.json
+++ b/package-lock.json
@@ -2,7 +2,7 @@
   "name": "entertainment-complex-management",
   "version": "0.1.0",
   "lockfileVersion": 3,
-  "documentVersion": "V0.01",
+  "documentVersion": "V0.02",
   "requires": true,
   "packages": {
     "": {
@@ -16,7 +16,7 @@
         "yaml": "^2.9.0"
       },
       "engines": {
-        "node": ">=22"
+        "node": "^22.13.0 || >=24.0.0"
       }
     },
     "apps/web": {
diff --git a/package.json b/package.json
index a7c852f3..0281eaa9 100644
--- a/package.json
+++ b/package.json
@@ -2,6 +2,7 @@
   "name": "entertainment-complex-management",
   "private": true,
   "version": "0.1.0",
+  "documentVersion": "V0.01",
   "type": "module",
   "workspaces": [
     "apps/web"
@@ -45,7 +46,6 @@
     "build": "npm run build --workspace apps/web",
     "typecheck": "npm run typecheck --workspace apps/web",
     "smoke:p0": "npm run test:backend",
-    "smoke:p0-ops": "npm run test:backend",
     "verify:android-env": "node tools/run-android-gradle.mjs --check --require-android-sdk --project apps/android-terminal/android-app",
     "android:p0:build": "node tools/run-android-gradle.mjs --project apps/android-terminal clean build",
     "android:p0:smoke": "node tools/run-android-p0-smoke.mjs",
@@ -138,7 +138,7 @@
   },
   "packageManager": "npm@10.9.8",
   "engines": {
-    "node": ">=22"
+    "node": "^22.13.0 || >=24.0.0"
   },
   "devDependencies": {
     "@playwright/test": "^1.61.0",
diff --git a/playwright.config.ts b/playwright.config.ts
index 6fe1ab73..0feddca2 100644
--- a/playwright.config.ts
+++ b/playwright.config.ts
@@ -1,8 +1,9 @@
+// 版本：V0.01
 import { defineConfig, devices } from "@playwright/test";
 
 export default defineConfig({
   testDir: "./tests/e2e",
-  testIgnore: "web-ui-real-backend.spec.ts",
+  testIgnore: ["web-ui-real-backend.spec.ts", "*-live-evidence.spec.ts"],
   globalSetup: "./tests/e2e/support/global-setup.ts",
   timeout: 30_000,
   expect: { timeout: 8_000 },
@@ -14,7 +15,7 @@ export default defineConfig({
     screenshot: "only-on-failure",
   },
   webServer: {
-    command: "npm run dev:web",
+    command: "npm --workspace apps/web run dev -- --host 127.0.0.1",
     url: "http://127.0.0.1:5173",
     reuseExistingServer: !process.env.CI,
     timeout: 120_000,
diff --git a/playwright.real-backend.config.ts b/playwright.real-backend.config.ts
index 37c12a2b..63f7eb06 100644
--- a/playwright.real-backend.config.ts
+++ b/playwright.real-backend.config.ts
@@ -1,12 +1,20 @@
+// 版本：V0.01
 import { defineConfig, devices } from "@playwright/test";
 
+if (process.env.REAL_BACKEND_E2E !== "1") {
+  throw new Error("真实后端 E2E 须设置 REAL_BACKEND_E2E=1，请使用 npm run verify:ui:real-backend。");
+}
+
 const backendBaseUrl = process.env.REAL_BACKEND_BASE_URL ?? "http://127.0.0.1:8080";
-const webPort = process.env.REAL_BACKEND_WEB_PORT ?? "5174";
+const webPort = Number(process.env.REAL_BACKEND_WEB_PORT ?? "5174");
+if (!Number.isInteger(webPort) || webPort < 1 || webPort > 65535) {
+  throw new Error("REAL_BACKEND_WEB_PORT 必须是 1 至 65535 的整数端口。");
+}
 const webBaseUrl = `http://127.0.0.1:${webPort}`;
 
 export default defineConfig({
   testDir: "./tests/e2e",
-  testMatch: "web-ui-real-backend.spec.ts",
+  testMatch: ["web-ui-real-backend.spec.ts", "*-live-evidence.spec.ts"],
   timeout: 45_000,
   expect: { timeout: 10_000 },
   fullyParallel: false,
```

## 八个hunk逐行归因

| hunk | 全部变化 | 定稿ID | 逐行越界判定 |
|---|---|---|---|
| 1 package-lock 顶层 | documentVersion V0.01→V0.02 | M005-001 | PASS；同文件用户版本铁律，B2明确批准 |
| 2 package-lock root | packages[""].engines.node收窄 | M005-001 | PASS；必要根镜像一致性，非重开M004 |
| 3 package.json 顶部 | documentVersion首次V0.01 | M005-001 | PASS；文件版本，不改变应用semver |
| 4 package.json scripts | 删除smoke:p0-ops一键 | M005-004 | PASS；未证调用的重复别名，smoke:p0/test:backend保持 |
| 5 package.json engines | >=22→^22.13.0或>=24.0.0 | M005-001 | PASS；跟随既定依赖真实相容范围 |
| 6 默认配置顶部 | 版本注释及testIgnore数组 | M005-003 | PASS；现有15真实case移出mock选择器，只有对应文件版本一行 |
| 7 默认webServer | 唯一command改成workspace dev并追加loopback host | M005-006 | PASS；保持共享dev命令、端口、复用策略 |
| 8 真实配置顶部 | 版本注释、flag护栏、number端口及整型范围护栏、testMatch数组 | M005-002/M005-005/M005-003 | PASS；版本归002，flag及附属空行归005，端口转换/护栏及附属行归002，testMatch归003 |

共同上下文行不构成修改；八hunk中所有新增/删除行及空行都有上述归因，无无ID优化、库升级、业务正文或HTML变化。只有四source路径，现有35个scratch文件全部SHA保持。

## 正确性、安全性及最小性

001：根声明与lock根一致；npm原生checkEngine实际拒22.0/22.12/23，接受22.13/24。把更低Node20支持加回来不在需求内；未增加npm限制或升级依赖。JSON规范化回滚仅本批字段后与before逐字段深相等，packages非根219记录及根其余字段全部相同，应用version0.1.0与lockfileVersion3保持。原生兼容统计含根记录，所以修后失败数27/2/10较修前26/1/9各多1；不把它误称新依赖失败。

002：Number先把环境输入收敛为number，再按整数1..65535限制；URL和shell模板只用已验证数字。非法NaN/小数/0/65536/路径分号在加载时非0，shell标记没有机会进入命令。Number允许空白包围或数值表示法，不加无需求字符串限制；无穷、空串成0同样拒绝。仅论本地/CI可控环境的命令构造，不声称公网RCE。

003：原生Playwright收集默认32条6文件、真实18条4文件、两集合无交集且并集10文件，原50条全归位。已存在三个live-evidence共15条与原真实3条进入正确入口；不改业务spec，不把--list说成真实执行50条通过。

004：删除别名后git grep排除审计/归档/scratch对受控源码搜索exit1无引用；底层test:backend及smoke:p0保持，package其余127scripts未变。

005：真实配置缺REAL_BACKEND_E2E=1时import即明确非0，错误提示现有npm入口。显式1可导入/列18条，npm入口及seeded wrapper原已传1；不自动授权真实调用。

006：实际执行修复后webServer.command，由npm原生workspace转发到原Vite CLI。进程内Node load hook只在真实CLI重复参数归并后打印options并return，避免服务启动；实际最终host127.0.0.1、port5173。不是重写一个自制参数解析器。默认reuseExistingServer等字段保留；不声称验证外网可达/业务服务。

## 可复现回归检查：同一代码先红后绿

下列Python在160仓库根以python3 -运行，不写源码；Node真实配置导入及Playwright --list，环境在子进程中隔离，不启动后端/数据库。
```python
from pathlib import Path
import os,subprocess,json,re,sys
checks=[]
js="import('./playwright.real-backend.config.ts').then(({default:c})=>console.log(JSON.stringify({url:c.use.baseURL})))"
for port,want in [('1',True),('5174',True),('65535',True),('0',False),('65536',False),('abc',False),('5174.5',False),('5174/; printf M005_NO_EXEC; #',False)]:
 env=os.environ.copy();env['REAL_BACKEND_E2E']='1';env['REAL_BACKEND_WEB_PORT']=port
 p=subprocess.run(['node','--input-type=module','-e',js],env=env,capture_output=True,text=True)
 checks.append({'case':'port '+port,'ok':(p.returncode==0)==want,'exit':p.returncode})
env=os.environ.copy();env.pop('REAL_BACKEND_E2E',None);env.pop('REAL_BACKEND_WEB_PORT',None)
p=subprocess.run(['node','--input-type=module','-e',js],env=env,capture_output=True,text=True)
checks.append({'case':'missing real flag rejects before starting services','ok':p.returncode!=0,'exit':p.returncode})
files=[]
for config,want in [('playwright.config.ts',(32,6)),('playwright.real-backend.config.ts',(18,4))]:
 env=os.environ.copy();env['REAL_BACKEND_E2E']='1';env.pop('REAL_BACKEND_WEB_PORT',None)
 p=subprocess.run(['node','node_modules/@playwright/test/cli.js','test','--config='+config,'--list','--reporter=list'],env=env,capture_output=True,text=True)
 hit=re.search(r'Total: (\d+) tests in (\d+) files?',p.stdout);got=tuple(map(int,hit.groups())) if hit else None
 selected=set(re.findall(r'› ([^\s:]+\.spec\.ts):',p.stdout));files.append(selected)
 checks.append({'case':config+' selects intended suite','ok':p.returncode==0 and got==want,'exit':p.returncode,'got':got,'want':want})
checks.append({'case':'mock and real partition all existing spec files','ok':not files[0]&files[1] and len(files[0]|files[1])==10,'intersection':sorted(files[0]&files[1]),'union':len(files[0]|files[1])})
print(json.dumps({'checks':checks,'failures':sum(not r['ok'] for r in checks)},ensure_ascii=False))
sys.exit(1 if any(not r['ok'] for r in checks) else 0)
```

修复前实测exit1：
```text
{"checks": [{"case": "port 1", "ok": true, "exit": 0}, {"case": "port 5174", "ok": true, "exit": 0}, {"case": "port 65535", "ok": true, "exit": 0}, {"case": "port 0", "ok": false, "exit": 0}, {"case": "port 65536", "ok": false, "exit": 0}, {"case": "port abc", "ok": false, "exit": 0}, {"case": "port 5174.5", "ok": false, "exit": 0}, {"case": "port 5174/; printf M005_NO_EXEC; #", "ok": false, "exit": 0}, {"case": "missing real flag rejects before starting services", "ok": false, "exit": 0}, {"case": "playwright.config.ts selects intended suite", "ok": false, "exit": 0, "got": [47, 9], "want": [32, 6]}, {"case": "playwright.real-backend.config.ts selects intended suite", "ok": false, "exit": 0, "got": [3, 1], "want": [18, 4]}, {"case": "mock and real partition all existing spec files", "ok": true, "intersection": [], "union": 10}], "failures": 8}
```

修复后实测exit0：
```text
{"checks": [{"case": "port 1", "ok": true, "exit": 0}, {"case": "port 5174", "ok": true, "exit": 0}, {"case": "port 65535", "ok": true, "exit": 0}, {"case": "port 0", "ok": true, "exit": 1}, {"case": "port 65536", "ok": true, "exit": 1}, {"case": "port abc", "ok": true, "exit": 1}, {"case": "port 5174.5", "ok": true, "exit": 1}, {"case": "port 5174/; printf M005_NO_EXEC; #", "ok": true, "exit": 1}, {"case": "missing real flag rejects before starting services", "ok": true, "exit": 1}, {"case": "playwright.config.ts selects intended suite", "ok": true, "exit": 0, "got": [32, 6], "want": [32, 6]}, {"case": "playwright.real-backend.config.ts selects intended suite", "ok": true, "exit": 0, "got": [18, 4], "want": [18, 4]}, {"case": "mock and real partition all existing spec files", "ok": true, "intersection": [], "union": 10}], "failures": 0}
```

此检查保留为一份可运行检查，12个断言覆盖合法/非法端口、flag缺失与两套选择器；不是新测试框架或额外产物。

## 原生引擎、真实npm/Vite解析及删除引用验证
```python
from pathlib import Path
import subprocess,json,os,base64,shlex
nodejs="""const {createRequire}=require('node:module'); const requireNpm=createRequire('/usr/lib/node_modules/npm/package.json');const semver=requireNpm('semver');const {checkEngine}=requireNpm('npm-install-checks'); const p=require('./package.json'),l=require('./package-lock.json');
for(const v of ['22.0.0','22.12.0','22.13.0','23.0.0','24.0.0','24.20.0']) {let code='PASS';try{checkEngine(p,process.versions.npm||'11.19.0',v)}catch(e){code=e.code};const bad=Object.entries(l.packages).filter(([k,p])=>p.engines?.node&&!semver.satisfies(v,p.engines.node)); console.log(JSON.stringify({node:v,root:code,incompatible:bad.length})); if((code==='PASS')!==(bad.length===0))process.exitCode=1}
"""
p=subprocess.run(['node','-e',nodejs],capture_output=True,text=True);print('ENGINE_CHECK',p.returncode,p.stdout);assert p.returncode==0
js="""import {registerHooks} from 'node:module';import {pathToFileURL} from 'node:url';
const target=pathToFileURL('/home/james/complex-management/node_modules/vite/dist/node/cli.js').href;
registerHooks({load(url,ctx,next){const r=next(url,ctx);if(url===target){const s=String(r.source);const needle='filterDuplicateOptions(options);';if(!s.includes(needle))throw Error('missing native anchor');return {...r,source:s.replace(needle,needle+' console.log("M005_PARSED="+JSON.stringify(options)); return;')}}return r}});
"""
env=os.environ.copy();env['NODE_OPTIONS']='--import=data:text/javascript;base64,'+base64.b64encode(js.encode()).decode()
config=json.loads(subprocess.check_output(['node','--input-type=module','-e',"import('./playwright.config.ts').then(({default:c})=>console.log(JSON.stringify(c.webServer)))"],text=True))
p=subprocess.run(shlex.split(config['command']),env=env,capture_output=True,text=True);print('ACTUAL_NPM_VITE_PARSER',p.returncode,p.stdout,p.stderr)
hit=next(x.split('=',1)[1] for x in p.stdout.splitlines() if x.startswith('M005_PARSED='))
opts=json.loads(hit);assert p.returncode==0 and opts['host']=='127.0.0.1' and int(opts['port'])==5173
print('NO_SERVER_STARTED native action returned after option parse')
p=subprocess.run(['git','grep','-n','-F','smoke:p0-ops','--',':(exclude)docs/audit-2026-09/**',':(exclude)docs/_archive/**',':(exclude).scratch/**'],capture_output=True,text=True)
print('DELETED_ALIAS_REFERENCE_SEARCH',p.returncode,p.stdout);assert p.returncode==1
```

实际exit0输出：
```text
ENGINE_CHECK 0 {"node":"22.0.0","root":"EBADENGINE","incompatible":27}
{"node":"22.12.0","root":"EBADENGINE","incompatible":2}
{"node":"22.13.0","root":"PASS","incompatible":0}
{"node":"23.0.0","root":"EBADENGINE","incompatible":10}
{"node":"24.0.0","root":"PASS","incompatible":0}
{"node":"24.20.0","root":"PASS","incompatible":0}

ACTUAL_NPM_VITE_PARSER 0 
> apps-web@0.1.0 dev
> vite --host 0.0.0.0 --port 5173 --host 127.0.0.1

M005_PARSED={"host":"127.0.0.1","port":5173}
 
NO_SERVER_STARTED native action returned after option parse
DELETED_ALIAS_REFERENCE_SEARCH 1 
```

原生Vite CLI node_modules/vite/dist/node/cli.js:650—652为last-wins，704—716进入serve action；load hook在该action的既有filterDuplicateOptions之后return，所以不会加载server或打开端口。NODE_OPTIONS只在该次子进程传入，不改持久环境/任何磁盘源码。

结构检查实际执行git diff --check BEFORE AFTER exit0；两JSON解析和最小字段回滚深相等断言全过，实时四源等于after各blob，tsconfig base未变；没有运行npm install/ci/corepack激活。未声称Node22/23二进制构建或真实后端业务用例执行。

## 当前源快照
| 文件 | 行数 | SHA256 |
|---|---:|---|
| package.json | 147 | 350999a90ddf85b2d3dcbee090dd593fb7faff18d0f467f0a608d8b3664d473f |
| package-lock.json | 3321 | d3676e896b885b6bbb979440bb51b9dfe60728735cee000e528ab5d1791bcfef |
| playwright.config.ts | 26 | c1c72998765d8aa42b83f11c557a1e8ea886eb1c557207aab1ded8d5d5bd4700 |
| playwright.real-backend.config.ts | 46 | 956e9484517c7620833962e3ec45d63e940e8de452a201ab9a9fd148e5355132 |

本次新finding：无。审计PASS；越界PASS。等待Grok独立本次完整报告，合取前不修改六ID的队列，不进入B5、不写模块PASS。


## B4-r1接收回执
版本：V0.02

15:16:39 UTC取得Grok真实EXIT_CODE=0，指定modules/M005-b4-r1-grok-raw.md 32356字节/438行/SHA46f9501ac070309af410587efed1384e8919f502d9cc344e348affff4de10c52。已完整读438行；审计PASS、越界PASS、新finding无；明示未接触本方B1/B2和B4/B5 raw、未创建第三tree、未改源/全局配置。其围栏内完整3529字节diff实算与固定两树命令逐字相同。全部8hunk归001—006，含必要lock根镜像，验证范围充分。无待答项。

本方独立原前17530字节/SHA2092d5f24b1b23cb86a7d22d967917fd35d53aac570ece461f2bcb649c9cdf2d保留；本节为收后事实说明，不作为新盲审票。双席本次完整报告共同构成B4 PASS；actual/sidecar/ledger/本次Grok四值均为23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de，COUNT0。四源及35既有scratch哈希保持。六已修ID队列统一置否，B3仍1、失败重跑仍0，进入B5完整模块重审。模块尚未PASS。
