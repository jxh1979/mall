CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M005 B4-r2 Codex完整修复内容审计
版本：V0.01

本席结论：PASS；逐行归因/范围判定：PASS。仅为B4本席票，仍须Grok本票合取，未宣称模块收口。Grok已先实际结束exit0，适配器仅返回指定B4路径；本报告落盘前未读取Grok本轮raw或其CLI JSON。
定稿依据findings.md V0.98最上当前M005双PASS段及其V0.97完整候选，唯一队列是M005-014。范围为整个before/after tree差异，不按路径筛选。before=f6390b5328ba446786d04408d3780ef68c269425；after=7abf713d9c735c93b3c27ff5af5d6e9ff4c6f5d0。本轮B3已完整执行一次，累计2；两树在本轮唯一修复前后由独立临时索引git add -A/write-tree生成，主索引未改。
本次重新运行完整git diff --binary，两处hunk全文见下方命令记录；name-only恰为.github/workflows/predeploy-gates.yml及ops/ci/predeploy-gates.yml，无其他路径。不是HEAD差异或过滤结果。

## 逐块归因与正确性
| 完整diff块 | 每条增删行归属 | 审计 |
|---|---|---|
| .github/workflows/predeploy-gates.yml hunk -1,9/+1,9 | M005-014：L1文件自身V0.01→V0.02，L6改正pre-push职责 | 定稿明确允许这两行，版本与实际内容同步；删除旧误述、替换为真实职责；PASS |
| ops/ci/predeploy-gates.yml hunk -1,9/+1,9 | M005-014：同L1/L6，保持直接消费镜像 | 现有checker284–288的LF归一等值约束使此伴随变更必需；不启动M007审计；PASS |

两个YAML完整27+27行、本轮改动所依赖hook完整17行、两个部署门禁循环均本次实读。hook真实执行feature/main/main-override分别0/1/0，无npm门禁；部署脚本真实源码先执行REQUIRED_GATES，任一失败exit1。新注释准确描述两者分工；“仅拦截直接推送main”描述hook用途，不声称无法ALLOW_MAIN_PUSH覆盖。
取两树实际blob分别交现有yaml解析器，AST deepEqual全部PASS；当前两个文件均等于after tree blob，各1335bytes、SHA4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d。事件、权限、actionSHA、Node、安装、基线及门禁入口没有改动。git diff --check exit0。现有npm run verify:delivery-gates-docs exit0/DELIVERY_GATES_DOCS_PASS；不把父verify:delivery-gates或下游全仓门禁冒称PASS。

## 冻结判据与三线
第一性原理：修复是因为注释声称与真实hook0/1/0分支不符；两文件同步来自可执行checker消费约束，版本递增来自操作员明确规则。没有业务HTML上位发明，不把无HTML锚点的CI内容删除。类型4命令/输出与真实blob对照。
顺从式：GEN-001最小两行；GEN-002/016改后声称可复现；GEN-003有效AST与真实hook分支断言；GEN-008/013原AST一致、镜像门禁通过；GEN-009仍有失败阻断；GEN-011/018证据含两树完整diff和实际命令；GEN-017仍技术性。GEN-004/005/010无输入/权限/秘密行为变化；GEN-006/007/012/014/015此次说明diff无业务存储、并发、资源、迁移或界面行为，不新增对应要求。
对抗式：排查单改live造成镜像漂移、改注释误伤YAML配置、版本漏升、超出两个路径、hook声称过宽、误以HEADdiff为本轮diff；实际blob/AST、现有门禁、完整name-only和hook三probe共同排除。所有增删行均归014，无无ID修改。
技能：本次实际读取requesting-code-review和ponytail SKILL.md，按V0.17改为仅Grok复审与tree基线；此前已读receiving/verification方法用于先复现再判定。未派其他审计主体，未执行部署/真实push/安装或源码额外修改。

## 复现命令与完整输出
以下Python编排实际运行git diff --binary/--name-only/--check两树，git show两树对应blob并用node yaml原生AST深比较，完整读取上述源，执行npm run verify:delivery-gates-docs及bash hook三个stdin。所有测试只读；命令中没有实际git push或部署。

```text
CRITERIA b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
FULL_DIFF
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

NAMES .github/workflows/predeploy-gates.yml
ops/ci/predeploy-gates.yml

SOURCE .github/workflows/predeploy-gates.yml SHA 4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d BYTES 1335
1:# 文件版本：V0.02
2:name: predeploy-gates
3:# 独立治理门禁(GitHub 侧,与 .160 部署机隔离):PR 到 main 与 push 到 main 时静态跑
4:# delivery-gates 快子集(scope 登记/安全基线/后台矩阵/契约/阻断台账),catch 治理漂移。
5:# 无需数据库(全静态分析)。因私有库免费版无法设 required status check,此为可见信号 + 独立复现,
6:# 部署前硬阻断由 .160 部署脚本执行治理门禁；本地 pre-push 仅拦截直接推送 main。
7:on:
8:  pull_request:
9:    branches: [main]
10:  push:
11:    branches: [main]
12:jobs:
13:  predeploy:
14:    runs-on: ubuntu-latest
15:    steps:
16:      - uses: actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5 # v4.3.1 (AUD-068：固定完整 commit SHA，防可变标签漂移)
17:        with:
18:          fetch-depth: 0  # 门禁需完整历史；取不到基线时必须失败
19:      - uses: actions/setup-node@49933ea5288caeca8642d1e84afbd3f7d6820020 # v4.4.0 (AUD-068：固定完整 commit SHA，防可变标签漂移)
20:        with:
21:          node-version: "22"
22:      - name: install deps
23:        run: npm ci --ignore-scripts
24:      - name: verify:predeploy (governance gates)
25:        env:
26:          HTML_GATES_BASE: ${{ github.event_name == 'pull_request' && github.event.pull_request.base.sha || github.event.before }}
27:        run: npm run verify:predeploy
AST_TREE_CHECK 0 ACTUAL_TREE_AST_EQUAL

SOURCE ops/ci/predeploy-gates.yml SHA 4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d BYTES 1335
1:# 文件版本：V0.02
2:name: predeploy-gates
3:# 独立治理门禁(GitHub 侧,与 .160 部署机隔离):PR 到 main 与 push 到 main 时静态跑
4:# delivery-gates 快子集(scope 登记/安全基线/后台矩阵/契约/阻断台账),catch 治理漂移。
5:# 无需数据库(全静态分析)。因私有库免费版无法设 required status check,此为可见信号 + 独立复现,
6:# 部署前硬阻断由 .160 部署脚本执行治理门禁；本地 pre-push 仅拦截直接推送 main。
7:on:
8:  pull_request:
9:    branches: [main]
10:  push:
11:    branches: [main]
12:jobs:
13:  predeploy:
14:    runs-on: ubuntu-latest
15:    steps:
16:      - uses: actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5 # v4.3.1 (AUD-068：固定完整 commit SHA，防可变标签漂移)
17:        with:
18:          fetch-depth: 0  # 门禁需完整历史；取不到基线时必须失败
19:      - uses: actions/setup-node@49933ea5288caeca8642d1e84afbd3f7d6820020 # v4.4.0 (AUD-068：固定完整 commit SHA，防可变标签漂移)
20:        with:
21:          node-version: "22"
22:      - name: install deps
23:        run: npm ci --ignore-scripts
24:      - name: verify:predeploy (governance gates)
25:        env:
26:          HTML_GATES_BASE: ${{ github.event_name == 'pull_request' && github.event.pull_request.base.sha || github.event.before }}
27:        run: npm run verify:predeploy
AST_TREE_CHECK 0 ACTUAL_TREE_AST_EQUAL

SOURCE ops/160/hooks/pre-push SHA 46aabdf33cb3496d83a71e4357ca85ba2ed9dbbfd4582ab53747bc50bb748d6b BYTES 997
1:#!/usr/bin/env bash
2:# 2026-07-05:禁止直推 main(补 GitHub 私有库免费版无分支保护的缺口)。开发者提示,非安全边界。
3:# 正道:推 feature 分支 → 开 PR → 独立复核 + predeploy 门禁绿 → gh pr merge。
4:# 紧急直推需 ALLOW_MAIN_PUSH=1 覆盖(留痕:请在 PR/提交说明记录原因)。
5:while read -r line; do
6:  # 稳健取 remote ref:git 传入「<localref> <localsha> <remoteref> <remotesha>」,
7:  # remoteref 目标 refs/heads/main 本身无空格;直接整行匹配该目标,避免源 ref 含空格致字段错位漏拦。
8:  if [[ " $line " == *" refs/heads/main "* ]]; then
9:    if [[ "${ALLOW_MAIN_PUSH:-0}" != "1" ]]; then
10:      echo "[pre-push] 拒绝直推 main。请走 PR 流程(ops/160/propose-change.sh)。" >&2
11:      echo "[pre-push] 确需直推:ALLOW_MAIN_PUSH=1 git push ...(并在 PR/说明记录原因)。" >&2
12:      exit 1
13:    fi
14:    echo "[pre-push] ALLOW_MAIN_PUSH=1 覆盖:放行直推 main(已留痕提醒)。" >&2
15:  fi
16:done
17:exit 0
CONTEXT ops/160/complex-deploy-api.sh
197:REQUIRED_GATES=(
198:  verify:html-ledger-sync verify:entry-anchors verify:html-param-tier
199:  verify:delivery-gates-docs verify:full-admin-matrix verify:domain-ownership-scope
200:  verify:matrix-vs-migration verify:data-isolation-spec verify:tenant-isolation
201:  verify:p0-contract verify:security-baseline verify:production-readiness-evidence
202:  verify:production-blockers verify:pii-encryption verify:test-db-isolation
203:)
204:for gate in "${REQUIRED_GATES[@]}"; do
205:  echo "[$(date -Iseconds)] pre-deploy gate: npm run $gate" | tee -a "$LOG_DIR/deploy.log"
206:  if ! npm run "$gate"; then
207:    echo "[$(date -Iseconds)] DEPLOY ABORTED: $gate 未过,拒绝部署。" | tee -a "$LOG_DIR/deploy.log" >&2
208:    exit 1
209:  fi
210:done
CONTEXT ops/160/complex-deploy-web.sh
24:REQUIRED_GATES=(
25:  verify:html-ledger-sync verify:entry-anchors verify:html-param-tier
26:  verify:delivery-gates-docs verify:full-admin-matrix verify:domain-ownership-scope
27:  verify:matrix-vs-migration verify:data-isolation-spec verify:tenant-isolation
28:  verify:p0-contract verify:security-baseline verify:production-readiness-evidence
29:  verify:production-blockers verify:pii-encryption verify:test-db-isolation
30:)
31:for gate in "${REQUIRED_GATES[@]}"; do
32:  echo "[$(date -Iseconds)] pre-deploy gate: npm run $gate" | tee -a "$LOG_DIR/deploy.log"
33:  if ! npm run "$gate"; then
34:    echo "[$(date -Iseconds)] DEPLOY ABORTED: $gate 未过,拒绝部署。" | tee -a "$LOG_DIR/deploy.log" >&2
35:    exit 1
36:  fi
37:done
CHECK ["git", "diff", "--check", "f6390b5328ba446786d04408d3780ef68c269425", "7abf713d9c735c93b3c27ff5af5d6e9ff4c6f5d0"] EXIT 0

CHECK ["npm", "run", "verify:delivery-gates-docs"] EXIT 0

> entertainment-complex-management@0.1.0 verify:delivery-gates-docs
> node tools/verify-delivery-gates.mjs

DELIVERY_GATES_DOCS_PASS

HOOK feature EXIT 0 
HOOK main EXIT 1 [pre-push] 拒绝直推 main。请走 PR 流程(ops/160/propose-change.sh)。
[pre-push] 确需直推:ALLOW_MAIN_PUSH=1 git push ...(并在 PR/说明记录原因)。

HOOK main-override EXIT 0 [pre-push] ALLOW_MAIN_PUSH=1 覆盖:放行直推 main(已留痕提醒)。

NO_REAL_PUSH_OR_DEPLOY_EXECUTED

```
