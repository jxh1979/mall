CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de

# M005 B4-r1 Grok 修复内容只读复审

版本：V0.01
席位：Grok（审计席乙）
控制：V0.17
模块：M005 / 共 322
步骤：B4-r1（B3 已执行次数 = 1）
ledger 给定状态：IN_PROGRESS（本席不写 ledger）
范围：只审 before/after 两树完整未过滤 diff；不修源/计划/findings/ledger；不启动 B5；不读本方 B1/B2；不读任何当前 B4/B5 raw。

## 0. 本席技能预检（实核路径，未核到不写已调用）

本席按合同收窄：只用 B4 复审席需要的技能，不派子代理、不增加角色/产物/闸、不装 hook、不问操作员。

存在并本席实际使用（收窄口径）：

1. `using-superpowers`：开工技能自检。路径 `/home/james/.claude/plugins/cache/claude-plugins-official/superpowers/6.3.0/skills/using-superpowers/SKILL.md`
2. `requesting-code-review`：B4 请审=本席自己审。不用 `HEAD~1`、不派同会话子代理、B4 期间不修。路径 `.../superpowers/6.3.0/skills/requesting-code-review/SKILL.md`。基线改用派发冻结的两 tree id。
3. `verification-before-completion`：PASS 前先跑命令。路径 `.../superpowers/6.3.0/skills/verification-before-completion/SKILL.md`
4. `grilling`：对抗/第一性原理自问自答，不等操作员、不派子代理。路径 `.../mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`
5. `diagnosing-bugs`：只借紧反馈环（导入抛错 / `--list` 计数），不加诊断埋点。路径 `.../mattpocock-skills/1.2.3/skills/engineering/diagnosing-bugs/SKILL.md`
6. `tdd` / `test-driven-development`：本批是配置修复，不强制落新测试；用类型 4 命令举证。路径分别 `.../engineering/tdd/SKILL.md`、`.../superpowers/6.3.0/skills/test-driven-development`
7. `codebase-design`：配置缝（env/CLI/选择器）深度与 locality。路径 `.../engineering/codebase-design/SKILL.md`
8. `writing-for-agents`：本 raw 按步骤+完成标准写。路径 `.../productivity/writing-for-agents/SKILL.md`
9. `ponytail-review`：只找可删的过度实现。路径 `/home/james/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md`
10. `i-have-adhd`：存在。路径 `/home/james/.claude/plugins/marketplaces/i-have-adhd/skills/i-have-adhd/SKILL.md`。本文件第一行按控制写锁哈希（控制优先于该技能「首行即下一步」）。
11. `systematic-debugging`：存在，只借读错误→复现，不改生产代码。路径 `.../superpowers/6.3.0/skills/systematic-debugging`

存在但本席不调用（合同划掉或会新增产物/人闸/第三席）：

- superpowers：`executing-plans` / `dispatching-parallel-agents` / `subagent-driven-development` / `using-git-worktrees` / `finishing-a-development-branch` / `receiving-code-review`（收 Grok 意见是 Codex 的 B3 后动作）
- ponytail：`ponytail-audit` / `ponytail-debt` / `ponytail-gain`；`ponytail` 本体是 B3 修复强度，本席 B4 不修
- mattpocock：`code-review` / `domain-modeling` / `research` / `wait-what` / `to-spec` / `to-tickets` / `to-questionnaire` / `handoff`
- `resolving-merge-conflicts`：存在，本 diff 无冲突，未调用
- `git-guardrails-claude-code`：存在于 `.../misc/git-guardrails-claude-code/SKILL.md`；本体要装 hook 并问安装范围，本席不安装。等价遵守：不 `git push` / `reset --hard` / `clean -f` / 不建第三 tree

缺口（目录实查不存在，不得冒充已调用）：

- `batch-grill-me`：未找到
- `ubiquitous-language`：未找到

## 1. 独立实算与输入核验

| 项 | 命令/方法 | 结果 |
|---|---|---|
| criteria-lock.md SHA256 | `sha256sum docs/audit-2026-09/plan/criteria-lock.md` | `23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de` |
| sidecar | `cat plan/criteria-lock.sha256` | 同值 |
| findings.md | `wc -c` + `sha256sum` | 172452 字节 / `2c3c1bbcd5ea0da7ddeb5a1f1857a692cf52105c01e04f4cbd1b17d28397137f` / 文首「版本：V0.49」 |
| 两树 diff | `git diff --binary 4e82fa54f5fcf4589bf2c8ad526ab64c9d2c2a4e 214490504e59405a1c6ef5f83f172a6f8be373fe` | 3529 字节 / SHA256 `42a88e82d010599b68b2e30414b2e35d2731cc5e6c6b81931ea55f4312779c26` / 4 路径 / 8 hunk |
| 第三 tree | 未执行 `git add`/`write-tree` | 未创建 |
| 当前工作区全 diff | 未读 | 避免混入本方 raw |
| 当前 4 源 vs after blob | sha256 比对 | 全等 |
| tsconfig.base.json before vs after | `git show` 两树 | 全等，未改 |

归因依据只读 `findings.md` 的 **M005 B2整批定稿（V0.49）** 正式 ID 表（唯一当前队列表）：001—006 当前修复队列=是；007—012=否。未读其他模块报告、未读 Codex B1/B2 论证、未读任何 B4/B5 raw。

M005 映射：`M005 → [GEN-001 … GEN-018]`。本批修复引用 GEN-001/002/003/004/009/010/011/013。

模块文件（`plan/modules.md` M005）：`package.json`、`playwright.config.ts`、`playwright.real-backend.config.ts`、`tsconfig.base.json`。`package-lock.json` 属 M004 文件；M005-001 明文批准「只同步该根镜像；不重开 M004、不改任何依赖包版本/URL/SRI/其他字段」。

## 2. 完整两树 diff 原文（逐字，未过滤）

命令：`git diff --binary 4e82fa54f5fcf4589bf2c8ad526ab64c9d2c2a4e 214490504e59405a1c6ef5f83f172a6f8be373fe`

before=`4e82fa54f5fcf4589bf2c8ad526ab64c9d2c2a4e`  
after=`214490504e59405a1c6ef5f83f172a6f8be373fe`

下列围栏内为该命令完整 stdout，3529 字节，SHA256 `42a88e82d010599b68b2e30414b2e35d2731cc5e6c6b81931ea55f4312779c26`。无 raw 路径。

````diff
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
````

## 3. 八 hunk 逐块 ID 归因（+/- 每一行）

闸 3：每一改动行必须落到 findings.md 已定稿且队列=是的 ID。007—012 为否，不得承担任何 +/- 行。

| hunk | 文件 | @@ | +/- 行 | 归因 |
|---|---|---|---|---|
| H1 | package-lock.json | @@ -2,7 +2,7 @@ | `- "documentVersion": "V0.01",` / `+ "documentVersion": "V0.02",` | **M005-001**（锁文件自身版本 V0.01→V0.02；应用 version 0.1.0 与 lockfileVersion 3 未改） |
| H2 | package-lock.json | @@ -16,7 +16,7 @@ | `- "node": ">=22"` / `+ "node": "^22.13.0 \|\| >=24.0.0"`（packages[""].engines.node） | **M005-001**（根 engines 镜像；深比较：非根 219 条 package 记录 0 变更，根除 engines 外 0 变更） |
| H3 | package.json | @@ -2,6 +2,7 @@ | `+ "documentVersion": "V0.01",` | **M005-001**（该文件首次加 documentVersion；004 写明版本与 001 共享一次、不重复递增） |
| H4 | package.json | @@ -45,7 +46,6 @@ | `- "smoke:p0-ops": "npm run test:backend",` | **M005-004**（删死别名；保留 `smoke:p0`） |
| H5 | package.json | @@ -138,7 +138,7 @@ | `- "node": ">=22"` / `+ "node": "^22.13.0 \|\| >=24.0.0"` | **M005-001**（根 engines 与锁镜像同值） |
| H6 | playwright.config.ts | @@ -1,8 +1,9 @@ | `+ // 版本：V0.01`；`- testIgnore: "web-ui-real-backend.spec.ts",` / `+ testIgnore: ["web-ui-real-backend.spec.ts", "*-live-evidence.spec.ts"],` | **M005-003**（默认配置首次版本行 + 两套选择器的默认侧） |
| H7 | playwright.config.ts | @@ -14,7 +15,7 @@ | `- command: "npm run dev:web",` / `+ command: "npm --workspace apps/web run dev -- --host 127.0.0.1",` | **M005-006**（默认 E2E loopback；未改 reuseExistingServer/超时/url/共享 apps/web dev 脚本） |
| H8 | playwright.real-backend.config.ts | @@ -1,12 +1,20 @@ | 见下行拆条 | 见下行 |

H8 逐行：

| 符号 | 文本 | ID |
|---|---|---|
| + | `// 版本：V0.01` | **M005-002**（该文件首次加版本行，定稿写「同文件首次加“// 版本：V0.01”，只加一次」） |
| + | 空行 | **M005-005**（flag 守卫块的分隔，与 throw 同一次插入） |
| + | `if (process.env.REAL_BACKEND_E2E !== "1") {` | **M005-005** |
| + | `throw new Error("真实后端 E2E 须设置 REAL_BACKEND_E2E=1，请使用 npm run verify:ui:real-backend。");` | **M005-005** |
| + | `}` | **M005-005** |
| + | 空行 | **M005-005** |
| - | `const webPort = process.env.REAL_BACKEND_WEB_PORT ?? "5174";` | **M005-002** |
| + | `const webPort = Number(process.env.REAL_BACKEND_WEB_PORT ?? "5174");` | **M005-002** |
| + | `if (!Number.isInteger(webPort) \|\| webPort < 1 \|\| webPort > 65535) {` | **M005-002** |
| + | `throw new Error("REAL_BACKEND_WEB_PORT 必须是 1 至 65535 的整数端口。");` | **M005-002** |
| + | `}` | **M005-002** |
| - | `testMatch: "web-ui-real-backend.spec.ts",` | **M005-003** |
| + | `testMatch: ["web-ui-real-backend.spec.ts", "*-live-evidence.spec.ts"],` | **M005-003** |

无未归因 +/- 行。无 007—012 行。无测试 spec / HTML / tsconfig.base.json / 依赖包记录 / 业务源码。

版本行记账（定稿「共享版本行按首次涉及本文件 ID」）：

- package.json `documentVersion` V0.01 → M005-001
- package-lock.json `documentVersion` V0.01→V0.02 → M005-001
- playwright.config.ts `// 版本：V0.01` → M005-003
- playwright.real-backend.config.ts `// 版本：V0.01` → M005-002

## 4. 最小性（GEN-001 / ponytail-review）

对照 V0.49 候选最小修复原文：

- 001：engines 字符串逐字 `^22.13.0 || >=24.0.0`；锁只动 `packages[""].engines.node` + 顶层 documentVersion；JSON 深比较非根包 0 变更。未改 `devDependencies`/`dependencies`/URL/SRI，未重开 M004。
- 002：`Number` + `Number.isInteger` + `[1,65535]` + 中文 throw；后续模板用 number。无新库。
- 003：两数组仅两个既有文件名模式，无新 spec、无新 npm 脚本。
- 004：只删 `smoke:p0-ops` 一项；`smoke:p0`/`test:backend` 保留。
- 005：只判断 `!== "1"`，沿用既有 flag，不新开关。
- 006：只改默认 `webServer.command`；`apps/web` 的 `"dev": "vite --host 0.0.0.0 --port 5173"` 未改（符合「不顺带改变共享脚本」）。

ponytail-review：相对已定稿最小修复，无额外抽象/新依赖/死灵活性。`Lean already. Ship.` `net: -0 lines possible`（再删就会偏离开冻修复句）。

## 5. 正确性 / 安全 / 验证效果

### 5.1 第一性原理三问

1. **为什么必须存在？** 删掉则 M005-001—006 的已定稿缺陷仍在：根 engines 宽于 jsdom；端口进 shell；15 条 live-evidence 默认 skip 且真实入口不收集；死别名；缺 flag 时 `--config` 假绿；默认 Vite 绑 0.0.0.0。三份 HTML 无对应业务锚点，分类为技术性工程缺陷（GEN-017 已在 B2 定稿），不是业务发明。
2. **声称行为是否执行验证？** 见 §6。`--list` 只证明收集集合，**不**证明 32/18 条业务用例通过（未跑用例、未启后端/DB）。
3. **未验证就当真的前提？** 无。Vite last-wins 读已安装 `vite@8.0.16` 的 `filterDuplicateOptions`；Playwright `--list` 走 `listMode` 任务（`createLoadTask` + `createReportBeginTask`），不跑 globalSetup/webServer。jsdom engines 从 after 锁记录读取，不是记忆。

### 5.2 顺从式（对冻结判据与定稿句）

| ID | GEN | 结果 |
|---|---|---|
| 001 | GEN-013/002 | 根 engines 与锁 `packages[""].engines` 相等；jsdom `^20.19.0 \|\| ^22.13.0 \|\| >=24.0.0`，根收窄后排除 22.0–22.12 与 23；当前 node v24.20.0 落在 `>=24.0.0` |
| 002 | GEN-004/010/009 | 端口在拼进 `webServer.command` 之前转为 number 并拒绝非整数/越界；`5174; printf INJECT; #` → Number=NaN → throw，命令未生成 |
| 003 | GEN-003/002 | 默认 32/6、真实 18/4、交集空、并 50/10，等于定稿预期 |
| 004 | GEN-001 | `git ls-files -z` 后排除 audit/scratch/raw 全文扫描 `smoke:p0-ops` 命中 0 |
| 005 | GEN-009/011/003 | 缺 flag：原生 import 与 `playwright test --list --config=playwright.real-backend.config.ts` 均非 0，中文错误；官方脚本仍是 `REAL_BACKEND_E2E=1 playwright test --config=playwright.real-backend.config.ts`；wrapper `tools/run-real-backend-ui-e2e.mjs:172` 仍设 `REAL_BACKEND_E2E: "1"` |
| 006 | GEN-004/010 | 默认 command 追加 `--host 127.0.0.1`；已安装 Vite CLI `filterDuplicateOptions` 对数组取 `value[value.length-1]`，故脚本里 `--host 0.0.0.0` 之后的 `--host 127.0.0.1` 生效。vite.config.ts 未设 `server.host`，CLI 为绑定来源 |

### 5.3 对抗式（本席自问，不等操作员）

- 只按源路径过滤 diff？否，读了完整 `--binary` 输出，恰 4 路径，无 raw。
- 锁重生夹带依赖？否，非根包记录 0 变更。
- `testIgnore` 误伤 `property-fee-evidence.spec.ts`？否，默认 `--list` 仍含该文件。
- `*-live-evidence.spec.ts` 不匹配长文件名？否，三条 live-evidence 均出现在真实 `--list`。
- 缺 flag 仍 exit 0？否，CLI `--list` exit 1。
- `REAL_BACKEND_E2E=true/0/空` 放行？否，均 throw（严格 `!== "1"`）。
- 端口注入进 shell？否，先 Number 失败。
- 006 改了共享 `apps/web` dev？diff 无该文件。
- 新增业务用例/脚本？无。
- 展示文案非中文？两条 throw 为中文；`REAL_BACKEND_E2E` / 端口环境变量名属系统内部标识符。
- `--list` 被当成业务绿？本席明确不声称用例通过。

未发现本 diff 的正确性/安全缺陷。端口 `1` 通过校验（定稿要求 1–65535）；特权端口运行时绑定失败不在本 ID 范围。

## 6. 只读验证命令与输出

环境：`/bin/node` v24.20.0；`/bin/npm` 11.19.0（未 `corepack prepare/activate`，不猜旧值回滚）；`packageManager` 仍声明 `npm@10.9.8`；未 `npm install/ci`；未启生产/数据库/部署；未跑未来模块。Playwright `--list` 未启动 webServer（全程数秒结束）。

直接消费者（`git ls-files -z` 列名后限定扫描，排除 `docs/audit-2026-09`、`.scratch`、raw）：

- `tsconfig.base.json` → 仅 `apps/web/tsconfig.json` `extends`
- playwright 配置 → `package.json` 脚本、`tools/run-real-backend-ui-e2e.mjs`（设 flag=1）、`tools/generate-runtime-acceptance-evidence.mjs`（只把两配置当指纹路径，不解析选择器）
- 006 命令消费者 → `apps/web/package.json` `dev` 仍 `vite --host 0.0.0.0 --port 5173`

### 6.1 JSON / engines / 别名

```
pj.documentVersion V0.01
lock.documentVersion V0.02
pj.version 0.1.0 lock.version 0.1.0 lockfileVersion 3
engines_equal True {'node': '^22.13.0 || >=24.0.0'}
smoke:p0 npm run test:backend
smoke:p0-ops in scripts? False
tracked 非 audit 源中 smoke:p0-ops 命中 0
verify:ui:real-backend REAL_BACKEND_E2E=1 playwright test --config=playwright.real-backend.config.ts
```

锁深比较：`packages` 键集 220 条相等；非根变更 0；顶层除 `documentVersion` 外相等。

### 6.2 原生配置导入（node --experimental-strip-types）

默认配置 exit 0：

```
{"testDir":"./tests/e2e","testIgnore":["web-ui-real-backend.spec.ts","*-live-evidence.spec.ts"],"command":"npm --workspace apps/web run dev -- --host 127.0.0.1","url":"http://127.0.0.1:5173","timeout":30000}
```

真实配置矩阵（每案独立进程，避免 ESM 缓存）：

| 案 | env | exit | 观察 |
|---|---|---|---|
| no-flag | 无 | 1 | `Error: 真实后端 E2E 须设置 REAL_BACKEND_E2E=1，请使用 npm run verify:ui:real-backend。` |
| flag-empty | REAL_BACKEND_E2E= | 1 | 同上 |
| flag-0 | =0 | 1 | 同上 |
| flag-true | =true | 1 | 同上 |
| flag1-default-port | =1 | 0 | `{"testMatch":["web-ui-real-backend.spec.ts","*-live-evidence.spec.ts"],"command":"npm --workspace apps/web run dev -- --host 127.0.0.1 --port 5174 --strictPort","url":"http://127.0.0.1:5174","baseURL":"http://127.0.0.1:5174"}` |
| port-1 | =1, PORT=1 | 0 | `{"testMatch":["web-ui-real-backend.spec.ts","*-live-evidence.spec.ts"],"command":"npm --workspace apps/web run dev -- --host 127.0.0.1 --port 1 --strictPort","url":"http://127.0.0.1:1","baseURL":"http://127.0.0.1:1"}` |
| port-5174 | =1, PORT=5174 | 0 | `{"testMatch":["web-ui-real-backend.spec.ts","*-live-evidence.spec.ts"],"command":"npm --workspace apps/web run dev -- --host 127.0.0.1 --port 5174 --strictPort","url":"http://127.0.0.1:5174","baseURL":"http://127.0.0.1:5174"}` |
| port-65535 | =1, PORT=65535 | 0 | `{"testMatch":["web-ui-real-backend.spec.ts","*-live-evidence.spec.ts"],"command":"npm --workspace apps/web run dev -- --host 127.0.0.1 --port 65535 --strictPort","url":"http://127.0.0.1:65535","baseURL":"http://127.0.0.1:65535"}` |
| port-0 | PORT=0 | 1 | `REAL_BACKEND_WEB_PORT 必须是 1 至 65535 的整数端口。` |
| port-65536 | PORT=65536 | 1 | 同上 |
| port-nan | PORT=abc | 1 | 同上 |
| port-semi | PORT=`5174; printf INJECT; #` | 1 | 同上（注入未进入 command） |
| port-float | PORT=5174.5 | 1 | 同上 |
| port-empty | PORT= | 1 | 同上（`??` 不把空串当缺省） |

### 6.3 Playwright --list

命令 1：`./node_modules/.bin/playwright test --list --config=playwright.config.ts`  
exit 0。stderr 仅 NO_COLOR/FORCE_COLOR 警告。stdout：

```
Listing tests:
  [chromium] › customer-journey.spec.ts:27:3 › customer journey acceptance (mock backend) › CUST-01 phone login then logout returns to login view
  [chromium] › customer-journey.spec.ts:36:3 › customer journey acceptance (mock backend) › CUST-02 buy ticket creates order and aggregate payment intent
  [chromium] › customer-journey.spec.ts:53:3 › customer journey acceptance (mock backend) › CUST-03 bar reservation creates reservation and deposit intent
  [chromium] › customer-journey.spec.ts:76:3 › customer journey acceptance (mock backend) › CUST-04 points page shows balance/level and check-in accrues streak
  [chromium] › customer-journey.spec.ts:92:3 › customer journey acceptance (mock backend) › CUST-05 wallet shows per-card stored value balances
  [chromium] › customer-journey.spec.ts:104:3 › customer journey acceptance (mock backend) › CUST-06 my orders reflect a freshly created ticket order
  [chromium] › customer-journey.spec.ts:130:3 › customer journey acceptance (mock backend) › CUST-07 buy ticket surfaces backend rejection as a visible error
  [chromium] › full-admin-mvp.spec.ts:20:3 › full admin MVP acceptance › ADM-MVP-01 login, overview route and eight MVP navigation entries
  [chromium] › full-admin-mvp.spec.ts:37:3 › full admin MVP acceptance › ADM-MVP-02 account and permission template workflows are actionable
  [chromium] › full-admin-mvp.spec.ts:85:3 › full admin MVP acceptance › ADM-MVP-03 ticketing policy validates weekday/weekend/holiday base price before publish
  [chromium] › full-admin-mvp.spec.ts:109:3 › full admin MVP acceptance › ADM-MVP-04 bar table session blocks over-limit check-in and records valid check-in
  [chromium] › full-admin-mvp.spec.ts:136:3 › full admin MVP acceptance › ADM-MVP-05 finance ledger requires adjustment reason and supports partial payment
  [chromium] › full-admin-mvp.spec.ts:152:3 › full admin MVP acceptance › ADM-MVP-06 integration adapter rejects plaintext secret and supports mock test
  [chromium] › full-admin-mvp.spec.ts:170:3 › full admin MVP acceptance › ADM-MVP-07 ops readiness cannot verify blocker without evidence ref
  [chromium] › full-admin-mvp.spec.ts:182:3 › full admin MVP acceptance › ADM-MVP-08 role permission and mobile layout are enforceable
  [chromium] › full-admin-mvp.spec.ts:195:3 › full admin MVP acceptance › ADM-MVP-09 failed login surfaces business wording and stays on auth screen
  [chromium] › full-admin-mvp.spec.ts:208:3 › full admin MVP acceptance › ADM-MVP-10 forbidden backend access surfaces business wording without hiding the audit trail
  [chromium] › full-admin-remaining.spec.ts:334:3 › full admin remaining pages acceptance › ADM-REM-01 exposes all non-first admin pages with route, evidence and readable content
  [chromium] › full-admin-remaining.spec.ts:673:3 › full admin remaining pages acceptance › ADM-REM-02 governance, contract, ticketing, bar and amusement actions are auditable
  [chromium] › full-admin-remaining.spec.ts:837:3 › full admin remaining pages acceptance › ADM-REM-03 risk, audit, analytics and integration pages cover empty/error/mobile states
  [chromium] › full-admin-remaining.spec.ts:887:3 › full admin remaining pages acceptance › ADM-REM-04 merchant scoped role can use authorized merchant workspace but is denied platform pages
  [chromium] › full-admin-remaining.spec.ts:940:3 › full admin remaining pages acceptance › ADM-REM-05 outsourced vendor can use assigned workbench pages only
  [chromium] › full-admin-ui-hardening.spec.ts:21:3 › full admin UI hardening acceptance › ADM-UI-HARDEN-01 restructured pages expose root + headered scrollable tables
  [chromium] › full-admin-ui-hardening.spec.ts:53:3 › full admin UI hardening acceptance › ADM-UI-HARDEN-02 shared empty and error states expose stable testids
  [chromium] › full-admin-ui-hardening.spec.ts:71:3 › full admin UI hardening acceptance › ADM-UI-HARDEN-03 overview date-range filter is operable and KPIs are addressable
  [chromium] › full-admin-ui-hardening.spec.ts:88:3 › full admin UI hardening acceptance › ADM-UI-HARDEN-04 money inputs reject non-positive values before any write
  [chromium] › full-admin-ui-hardening.spec.ts:111:3 › full admin UI hardening acceptance › ADM-UI-HARDEN-05 high-volume log pages support client-side search filter
  [chromium] › full-admin-ui-hardening.spec.ts:141:3 › full admin UI hardening acceptance › ADM-UI-HARDEN-06 log pages paginate filtered rows and reset on search
  [chromium] › full-admin-ui-hardening.spec.ts:185:3 › full admin UI hardening acceptance › ADM-UI-HARDEN-07 organization merchant create exposes editable fields with phone validation
  [chromium] › full-admin-visual.spec.ts:23:3 › full admin visual and responsive acceptance › ADM-VIS-01 desktop overview and production evidence pages keep a stable visual baseline
  [chromium] › full-admin-visual.spec.ts:47:3 › full admin visual and responsive acceptance › ADM-VIS-02 mobile admin routes keep nav, tables and primary actions usable
  [chromium] › property-fee-evidence.spec.ts:14:1 › ADM-FIN-004 物业费管理：费率设置 + 账单生成走通
Total: 32 tests in 6 files

```

命令 2：同 `--config=playwright.real-backend.config.ts`（不设 flag）  
exit 1。stdout 空。stderr：

```
(node:2801973) Warning: The 'NO_COLOR' env is ignored due to the 'FORCE_COLOR' env being set.
(Use `node --trace-warnings ...` to show where the warning was created)
Error: 真实后端 E2E 须设置 REAL_BACKEND_E2E=1，请使用 npm run verify:ui:real-backend。
    at file:///home/james/complex-management/playwright.real-backend.config.ts:5:9
    at ModuleJob.run (node:internal/modules/esm/module_job:561:25)
    at processTicksAndRejections (node:internal/process/task_queues:104:5)
    at node:internal/modules/esm/loader:647:26
    at requireOrImport (/home/james/complex-management/node_modules/playwright/lib/common/index.js:1146:12)
    at loadUserConfig (/home/james/complex-management/node_modules/playwright/lib/common/index.js:1333:46)
    at loadConfig (/home/james/complex-management/node_modules/playwright/lib/common/index.js:1340:22)
    at Object.loadConfigFromFile (/home/james/complex-management/node_modules/playwright/lib/common/index.js:1552:10)
    at runTests (/home/james/complex-management/node_modules/playwright/lib/cli/testActions.js:46:18)
    at _Command.<anonymous> (/home/james/complex-management/node_modules/playwright/lib/program.js:53:7)

```

命令 3：`REAL_BACKEND_E2E=1 ./node_modules/.bin/playwright test --list --config=playwright.real-backend.config.ts`  
exit 0。stdout：

```
Listing tests:
  [chromium] › customer-account-lifecycle-live-evidence.spec.ts:47:3 › V194 客人账号四态生命周期 · 真实后端活证据 › CUS-LIFE-01 封禁→拒动作拒登录→恢复→拉黑永久拒→自助注销后数据保留
  [chromium] › customer-account-lifecycle-live-evidence.spec.ts:120:3 › V194 客人账号四态生命周期 · 真实后端活证据 › CUS-LIFE-02 ADM-CUS-001 客人账号管理页在真实后端查询并截图
  [chromium] › leasing-contract-live-evidence.spec.ts:36:5 › 招商工作台 UI/后端契约对齐 · 真实后端活证据 › LEASE-CONTRACT 前端来源 source=outbound 被真实后端接受（2xx）
  [chromium] › leasing-contract-live-evidence.spec.ts:36:5 › 招商工作台 UI/后端契约对齐 · 真实后端活证据 › LEASE-CONTRACT 前端来源 source=online_inquiry 被真实后端接受（2xx）
  [chromium] › leasing-contract-live-evidence.spec.ts:36:5 › 招商工作台 UI/后端契约对齐 · 真实后端活证据 › LEASE-CONTRACT 前端来源 source=referral 被真实后端接受（2xx）
  [chromium] › leasing-contract-live-evidence.spec.ts:36:5 › 招商工作台 UI/后端契约对齐 · 真实后端活证据 › LEASE-CONTRACT 前端来源 source=exhibition 被真实后端接受（2xx）
  [chromium] › leasing-contract-live-evidence.spec.ts:48:5 › 招商工作台 UI/后端契约对齐 · 真实后端活证据 › LEASE-CONTRACT 已移除来源 source=walk_in 仍被真实后端 400 拒
  [chromium] › leasing-contract-live-evidence.spec.ts:48:5 › 招商工作台 UI/后端契约对齐 · 真实后端活证据 › LEASE-CONTRACT 已移除来源 source=online 仍被真实后端 400 拒
  [chromium] › leasing-contract-live-evidence.spec.ts:62:5 › 招商工作台 UI/后端契约对齐 · 真实后端活证据 › LEASE-CONTRACT 前端推进阶段 stage=requirement_confirmed 通过真实后端枚举校验
  [chromium] › leasing-contract-live-evidence.spec.ts:62:5 › 招商工作台 UI/后端契约对齐 · 真实后端活证据 › LEASE-CONTRACT 前端推进阶段 stage=proposal_quoted 通过真实后端枚举校验
  [chromium] › leasing-contract-live-evidence.spec.ts:62:5 › 招商工作台 UI/后端契约对齐 · 真实后端活证据 › LEASE-CONTRACT 前端推进阶段 stage=negotiation 通过真实后端枚举校验
  [chromium] › leasing-contract-live-evidence.spec.ts:62:5 › 招商工作台 UI/后端契约对齐 · 真实后端活证据 › LEASE-CONTRACT 前端推进阶段 stage=pending_signing 通过真实后端枚举校验
  [chromium] › leasing-contract-live-evidence.spec.ts:77:5 › 招商工作台 UI/后端契约对齐 · 真实后端活证据 › LEASE-CONTRACT 已移除阶段 stage=terms_agreed 仍被真实后端 400 拒
  [chromium] › leasing-contract-live-evidence.spec.ts:77:5 › 招商工作台 UI/后端契约对齐 · 真实后端活证据 › LEASE-CONTRACT 已移除阶段 stage=ready_to_sign 仍被真实后端 400 拒
  [chromium] › order-tickets-live-evidence.spec.ts:33:3 › 1.8 我的票 · 真实后端活证据 › TICKETS-LIVE-01 本人订单可列票实例，他人 403，不存在 404，无 token 401
  [chromium] › web-ui-real-backend.spec.ts:43:3 › real backend UI acceptance › REAL-UI-01 health, login, admin overview and logout use the real backend
  [chromium] › web-ui-real-backend.spec.ts:53:3 › real backend UI acceptance › REAL-UI-02 admin core business pages load real backend data without global errors
  [chromium] › web-ui-real-backend.spec.ts:77:3 › real backend UI acceptance › REAL-UI-03 protected business endpoints reject unauthenticated access (server-side denial, not just frontend hiding)
Total: 18 tests in 4 files

```

集合：默认文件 6 个与真实 4 个交集为空；并集 10 文件；32+18=50。`property-fee-evidence.spec.ts` 留在默认套件。

**未执行** `playwright test` 本体、未跑 32/18 条业务断言，故不声称业务用例全部通过。

### 6.4 Vite 已安装实现（last-wins）

`node_modules/vite/dist/node/cli.js`：

```
const filterDuplicateOptions = (options) => {
	for (const [key, value] of Object.entries(options)) if (Array.isArray(value)) options[key] = value[value.length - 1];
};
```

`cli.command("[root]"..."dev").option("--host [host]", ...)` 的 action 第一句即 `filterDuplicateOptions(options);`。重复 `--host` 取最后一项。未启动 Vite 进程。

### 6.5 scratch

曾建 `.scratch/m005-b4-r1-grok-verify/` 作隔离目录；实际反例直接对仓库内 after 源做进程隔离导入，未向 scratch 写入内容。目录已 `rm -rf` 清掉。

## 7. 结论

- **审计结论：PASS**
- **越界判定：PASS**（全部 +/- 行归因到队列=是的 M005-001—006；无锁外新判据；无无 ID 行）
- **本席本次 finding：无**
- **不启动 B5**（合同：本席只落本 raw 后退出）
- **未改** 任何源、计划、findings、ledger、三份业务 HTML
- **未把 Grok 原始 JSON 写入任何文件**

完成标准：两树 diff 全文已录、8 hunk 已逐行归因、验证命令已跑且输出已录、PASS/FAIL 已写、技能缺口已写。
