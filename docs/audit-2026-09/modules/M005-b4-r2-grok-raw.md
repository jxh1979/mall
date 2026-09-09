CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M005 B4-r2 Grok席乙 raw（自身 V0.01）

- 阶段：B4-r2 只读完整修复 diff 审计；首次 r2，本文件新建。
- 正本：`/home/james/complex-management` 当前 `main`（`HEAD`=`origin/main`=`242f487233d028173cb9198eecd079f5d877405c`）。本审计 **未** 用 HEAD 差异替代下述两 tree。
- 定稿：`docs/audit-2026-09/findings.md` V0.98 最上「当前 M005 B2 双PASS定稿」；紧下候选仅作对照。唯一队列 **M005-014**；**M005-012 / M005-013 否**。未读任何 self/旧 raw 结论。
- 冻结锁：独立读取 `docs/audit-2026-09/plan/criteria-lock.md` general GEN-001–018（锁文件版本 V0.11）。`sha256sum` 与 Python `hashlib` 均得上行 64 位 hex；不接收期望值。
- before tree：`f6390b5328ba446786d04408d3780ef68c269425`（`git cat-file -t` = tree）
- after tree：`7abf713d9c735c93b3c27ff5af5d6e9ff4c6f5d0`（`git cat-file -t` = tree）
- 命令：`git diff --binary f6390b5328ba446786d04408d3780ef68c269425 7abf713d9c735c93b3c27ff5af5d6e9ff4c6f5d0`（无路径过滤）
- 席位方法：本席自审（requesting/receiving review 清单 + verification 先跑后判 + ponytail 最短证据）。禁止子代理/MCP/源码修复/改计划；未开 M007 审计。

## 结论

**PASS**（范围 PASS，归因 PASS）。无未绑定 ID 的变更行，无 012/013 改动，无行为/业务添加。

## 1. 完整 diff 原文

`git diff --binary --stat`：

```
 .github/workflows/predeploy-gates.yml | 4 ++--
 ops/ci/predeploy-gates.yml            | 4 ++--
 2 files changed, 4 insertions(+), 4 deletions(-)
```

`git diff --binary --name-status`：`M` 仅上述两路径。`--numstat`：各 `2 2`。无新增/删除/改名/模式位。

完整补丁（命令原文）：

```
diff --git a/.github/workflows/predeploy-gates.yml b/.github/workflows/predeploy-gates.yml
index 1d189b42..3857c0f9 100644
--- a/.github/workflows/predeploy-gates.yml
+++ b/.github/workflows/predeploy-gates.yml
@@ -1,9 +1,9 @@
-# 文件版本：V0.01
+# 文件版本：V0.02
 name: predeploy-gates
 # 独立治理门禁(GitHub 侧,与 .160 部署机隔离):PR 到 main 与 push 到 main 时静态跑
 # delivery-gates 快子集(scope 登记/安全基线/后台矩阵/契约/阻断台账),catch 治理漂移。
 # 无需数据库(全静态分析)。因私有库免费版无法设 required status check,此为可见信号 + 独立复现,
-# 硬阻断由 .160 部署脚本的 verify:predeploy 与本地 pre-push 钩子承担。
+# 部署前硬阻断由 .160 部署脚本执行治理门禁；本地 pre-push 仅拦截直接推送 main。
 on:
   pull_request:
     branches: [main]
diff --git a/ops/ci/predeploy-gates.yml b/ops/ci/predeploy-gates.yml
index 1d189b42..3857c0f9 100644
--- a/ops/ci/predeploy-gates.yml
+++ b/ops/ci/predeploy-gates.yml
@@ -1,9 +1,9 @@
-# 文件版本：V0.01
+# 文件版本：V0.02
 name: predeploy-gates
 # 独立治理门禁(GitHub 侧,与 .160 部署机隔离):PR 到 main 与 push 到 main 时静态跑
 # delivery-gates 快子集(scope 登记/安全基线/后台矩阵/契约/阻断台账),catch 治理漂移。
 # 无需数据库(全静态分析)。因私有库免费版无法设 required status check,此为可见信号 + 独立复现,
-# 硬阻断由 .160 部署脚本的 verify:predeploy 与本地 pre-push 钩子承担。
+# 部署前硬阻断由 .160 部署脚本执行治理门禁；本地 pre-push 仅拦截直接推送 main。
 on:
   pull_request:
     branches: [main]
```

## 2. 逐块逐行归因（全部 M005-014）

两文件 before/after 全文已读（各 27 行）。blob：before `1d189b421e45aece50513c0474e9e76ae1ecef63`，after `3857c0f9b3ca98e4d2e8d94771e91be606e0e732`；两路径 before 互等、after 互等。SHA256 before `5c62bef3bfb18005319f9433c481d9293bba2626b1982bebbc420a6faa564974`（1315 bytes），after `4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d`（1335 bytes）。纯 LF，无 CRLF。工作区两文件字节 == after blob。

行级 diff（仅 L1、L6；其余 25 行字节级相同）：

| 块 | 路径 | 行 | before | after | ID |
|---|---|---|---|---|---|
| H1 | `.github/workflows/predeploy-gates.yml` | L1 | `# 文件版本：V0.01` | `# 文件版本：V0.02` | M005-014 |
| H1 | 同上 | L6 | `# 硬阻断由 .160 部署脚本的 verify:predeploy 与本地 pre-push 钩子承担。` | `# 部署前硬阻断由 .160 部署脚本执行治理门禁；本地 pre-push 仅拦截直接推送 main。` | M005-014 |
| H2 | `ops/ci/predeploy-gates.yml` | L1 | 同 H1 L1 | 同 H1 L1 | M005-014 |
| H2 | 同上 | L6 | 同 H1 L6 | 同 H1 L6 | M005-014 |

Hunk 上下文（L2–L5、L7–L9 起 `on:`）未改，不是新 finding。无第三文件、无 012/013 路径（`tsconfig.base.json` / `.github/CODEOWNERS` 不在 diff）。`ops/ci/predeploy-gates.yml` 属计划 M007 文件列表，本轮仅作 014 镜像伴随，不提前审 M007。

## 3. 定稿边界核对

V0.98 定稿允许且仅允许：两 YAML 各 L1 V0.01→V0.02，各 L6 换成上表 after 原文；共两文件四行。实测四行原文与定稿逐字相同（含全角 `：`、`；`、句末 `。`）。未改事件、步骤、action SHA、权限、hooks、部署脚本、package.json、三份业务 HTML。012/013 队列否，diff 未触及。

## 4. GEN-001–018 适用项（对本 diff）

适用并 PASS：

- **GEN-001** 最小性：仅定稿四行；无抽象/无多余文件。
- **GEN-002** 说明与可执行入口一致：见 §5 hook/部署脚本。
- **GEN-003** 配置说明不强制造测试文件；用既有 `verify:delivery-gates-docs` + AST/镜像。diff 无测试文件。
- **GEN-008** YAML AST before==after，两文件 after 字节相等。
- **GEN-011** 职责说明与 hook/部署脚本证据不矛盾。
- **GEN-013** 自身版本随注释修正递增 V0.02；CI 可执行字段未动。
- **GEN-016** L6 可从当前 hook 与 `.160` 部署脚本复现，不是把旧错误注释写成事实。
- **GEN-017** 技术性职责说明；无业务角色/枚举/审批发明。
- **GEN-018** 结论落到 tree diff、blob、YAML parse、hook probe、docs 门禁输出。

不适用（本 diff 未改这些面）：GEN-004/005/006/007/009/010/012/014/015。未改 timeout、权限、迁移、UI。

无行为改变：`yaml.parse` AST 与修前相同（`name` / `on.pull_request|push.branches=[main]` / `jobs.predeploy.runs-on=ubuntu-latest` / checkout `34e114876b0b11c390a56381ad16ebd13914f8d5` + `fetch-depth: 0` / setup-node `49933ea5288caeca8642d1e84afbd3f7d6820020` + `node-version: "22"` / `npm ci --ignore-scripts` / `npm run verify:predeploy` + 原 `HTML_GATES_BASE` 表达式）。

## 5. 上下文事实（允许精确路径，只读）

`ops/160/hooks/pre-push` 全文 17 行：只匹配 stdin 中 `refs/heads/main`；`ALLOW_MAIN_PUSH` 非 `1` 则 stderr 拒绝并 exit 1。不调用 npm / `verify:predeploy`。第 11 行 echo 提示字符串含 `git push` 字样，不是执行 `git push`。

`ops/160/complex-deploy-api.sh:197-210` 与 `ops/160/complex-deploy-web.sh:24-37`：`REQUIRED_GATES` 循环 `npm run "$gate"`，非 0 则 `DEPLOY ABORTED` 并 exit 1。硬阻断在部署脚本执行治理门禁，不经 pre-push。

`tools/verify-delivery-gates.mjs:282-289`：两 YAML 先 `\r\n`→`\n` 再比较；本轮原始字节亦相等。不冒称 checker 以原始字节为唯一条件。`:292-303` 解析 `jobs.predeploy.steps`（fetch-depth / verify:predeploy / HTML_GATES_BASE）。未改此工具。

`package.json`：`verify:delivery-gates-docs` = `node tools/verify-delivery-gates.mjs`（本轮所跑）；`verify:predeploy` 仍为 workflow `run` 目标，AST 未改。未跑 `verify:delivery-gates` / `verify:predeploy` 父串。

## 6. 最窄验证命令与输出

未部署、未 `git push`、未安装、未跑父串全仓门禁。不以已知下游 HTML 红刷绿。

### 6.1 `npm run verify:delivery-gates-docs`

```
> entertainment-complex-management@0.1.0 verify:delivery-gates-docs
> node tools/verify-delivery-gates.mjs

DELIVERY_GATES_DOCS_PASS
```

真实 exit **0**。

### 6.2 内存 YAML AST / 镜像断言（node `yaml.parse` + git cat-file，无落盘）

```
AST_EQUAL_BEFORE_AFTER_GH true
AST_EQUAL_BEFORE_AFTER_OPS true
AST_GH_EQ_OPS_AFTER true
LF_LIVE_EQ_TMPL_AFTER true
LF_LIVE_EQ_TMPL_BEFORE true
RAW_LIVE_EQ_TMPL_AFTER true
FETCH_DEPTH 0
VERIFY_RUN npm run verify:predeploy
```

Python：before 两文件互等、after 两文件互等；工作区 == after；仅 L1/L6 变；L1/L6 与定稿逐字相等。

### 6.3 hook 无推送 probe

`python3` `subprocess.run(['bash','ops/160/hooks/pre-push'], input=..., env=...)`，stdin 为 `localref localsha remoteref remotesha` 行。未调用 `git push`、未部署。

| probe | exit | stderr 要点 |
|---|---|---|
| feature 分支 / 默认 env | 0 | 空 |
| `refs/heads/main` / 默认 env | 1 | `拒绝直推 main。请走 PR 流程(ops/160/propose-change.sh)。` |
| `refs/heads/main` / `ALLOW_MAIN_PUSH=1` | 0 | `ALLOW_MAIN_PUSH=1 覆盖:放行直推 main(已留痕提醒)。` |

与改后 L6「本地 pre-push 仅拦截直接推送 main」及部署脚本硬阻断分工一致。

## 7. 问题清单

空。范围、归因、最小性、镜像、版本、无行为改变均成立。本席 **PASS**。
