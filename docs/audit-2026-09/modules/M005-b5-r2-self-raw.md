CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M005 B5-r2 Codex全模块完整三线复审
版本：V0.01

本席B5：PASS。当前3文件52行、18GEN均已完整复审；没有新finding、业务添加或新HTML自身问题。本报告只是本席票，须读取Grok本次完整报告并做B5后四值核对才能收口。Grok先真实结束exit0、只暴露指定B5路径；本报告保存前未读本次Grok正文/CLI JSON/工具轨迹，不用其结论证明本席通过。
正本/home/james/complex-management，main，HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c；冻结criteria V0.11实算首行SHA，映射L6384明确GEN001—018。B3累计2，修复前全批归并/修复diff阶段的历史事实不替代本次全审。

## 1. 全文件与三线覆盖
| 文件 | 连续全覆盖 | 本次验证 |
|---|---|---|
| tsconfig.base.json | L1–13，全部九项选项 | 原生TS5.9.3解析真实web extends353输入、选项/global诊断及正负片段 |
| .github/CODEOWNERS | L1–3前提说明，L4–12九规则 | 全规则目标与反例；GitHub当前errors、owner权限和保护API |
| .github/workflows/predeploy-gates.yml | L1–6版本用途；L7–14事件/job；L15–27四步骤全部字段 | 原生YAML2.9.0、action pin当前API、镜像门禁、实际npm失败传播、hook职责 |
13+12+27=52行，全部原文及SHA见证据区。另完整读web tsconfig10行、mirror27行、hook17行、第一闸脚本68行，只用作本模块直接消费上下文；两部署脚本实际REQUIRED_GATES循环本次重新逐行读取。既有325行delivery checker本任务已全文读，本次再次实跑，不宣称本模块完成了M007/M008审计。

## 2. 第一性原理与全部技术性内容
| 登记 | 必要性/删除影响 | 四分类及证据 |
|---|---|---|
| CX-B5-01，对应012 | ES2022、ESNext、Bundler规定真实web编译/解析；strict、esModuleInterop、forceConsistentCasingInFileNames、skipLibCheck、resolveJsonModule、noUncheckedIndexedAccess是九项工程编译策略；删除会损坏web extends或丢失这些选项 | 冲突否→业务细化无特定上位锚点→技术性是→添加否。无业务实体/状态/价格/权限发明。实际原生解析全部九选项符合配置，353输入；正确.at示例0错，null/缺索引2322、隐式any7006、非法moduleResolution6046被拒绝。类型4，工程缺陷否 |
| CX-B5-02，对应013 | 九条根路径映射把部署/hook/CI/tools/package/迁移工程改动交给已有仓库负责人；删除会丢失评审路由 | 技术性：GitHub代码所有者不是产品角色。全部9规则匹配非空，self/workflow正例，嵌套/类似前缀/大小写负例不误中；实时API errors=[]，owner admin，保护403提示升级，符合当前受限说明。L3的“升级即生效”受L2启用保护+review前提约束，不视为付费自动配置承诺。类型4，工程缺陷否 |
| CX-B5-03，对应014 | 生效workflow是GitHub在main PR/push消费的入口；删除会丢失可见治理信号。镜像用于本地工程检查，不替代.github入口 | 技术性：版本/事件/步骤/基线不发明业务流程。YAML2事件4步骤、full checkout、Node22、固定actionSHA、安装及执行字符串全部核对；默认token read且不可approve。镜像相等/现有docs门禁0；真实npm入口在第一闸失败返回1。L6改后职责用hook0/1/0及部署循环重新证明。类型4，当前缺陷否、历史014已修保持 |

三问中的未验证前提明确：未触发新的远程GitHub作业、未执行npm ci安装、未对353个应用文件做全量语义/业务编译审计、未造Linux大小写冲突文件；不据此宣称这些验证已完成。配置与说明按2.3类型4验证，不强制执行部署。此前实际读过的GitHub/TS官方文档仅作语义辅助；本次现态结论由原生工具与实时API支撑，不以历史PASS为证。

## 3. 顺从式18项
| 判据 | 本次判定与实际依据 |
|---|---|
| GEN-001 | 三文件有实际TS/GitHub消费者；无多余抽象，保留工程必要内容。 |
| GEN-002 | TS/YAML原生解析有效；门禁真实入口可执行并暴露当前失败，L6职责与hook/部署实现一致。 |
| GEN-003 | 有可失败断言：正确TS0错，三个错误片段均拒绝，非法选项6046；CODEOWNERS正/负路径；实际缺基线和npm失败码。 |
| GEN-004 | action为完整commitSHA，基线只取GitHub事件SHA；缺基线实际非0，不插入外部任意文本。 |
| GEN-005 | token当前默认read/can_approve=false，无显式提权；代码所有者admin已核，CODEOWNERS不是业务授权边界。 |
| GEN-006 | 本模块不改变业务存储/金额/状态；CI无部署写入，无事务新要求。 |
| GEN-007 | 静态CI无共享业务状态或重复收费；无幂等/锁新增必要性。 |
| GEN-008 | web真实继承九项基础选项且自身JSX等覆盖一致；镜像原始字节相等；无业务DTO/模式发明。 |
| GEN-009 | 第一闸及npm/bash真实exit1，&&后续不跑；workflow没有continue-on-error/条件跳过掩盖失败。 |
| GEN-010 | 源无凭据/动态shell拼接；固定action，普通pull_request而非pull_request_target；不新增权限或输出秘密。 |
| GEN-011 | 命名步骤、明确基线/BSM/台账错误及非0可复现；描述可见CI而非当前required check。 |
| GEN-012 | 单job四步骤无无界业务循环/查询，托管平台默认job超时360分钟为此前直接读取官方原文；不把缺timeout误写无上限，也不增加字段。 |
| GEN-013 | 当前TS5.9.3/YAML2.9.0解析，Node22/两pin与API相符，npm ci --ignore-scripts和完整历史配置一致；版本V0.02正确。 |
| GEN-014 | 本模块没有SQL/数据库执行，CODEOWNERS仅路由迁移文件；不虚构迁移验证。 |
| GEN-015 | 无产品UI/表单；中文说明可读，没有界面交互适用项。 |
| GEN-016 | 所有路径/原生配置/注释/版本/owner及action状态可复现；修后L6准确，不把计划称部署完成。 |
| GEN-017 | 九项TS、九条CODEOWNERS与全部workflow均技术性，无业务发明；业务B/C原意未重判。 |
| GEN-018 | 当前源逐行、原生断言/API/命令输出齐全；没有以席位结论或注释本身作最终依据。 |

## 4. 对抗式与结论边界
配置假生效：从真实apps/web/tsconfig.json原生extends展开而非手工合并，选项全比对；内存program正反诊断证明关键严格性。配置解析/global通过不等于全业务代码通过。
路径错配：9条实际规则逐条有目标，/.github/覆盖自身及workflow；nested/tools、package.json.bak、tools-extra、Tools负例不命中。当前保护功能受限如实记录，无新增全仓owner覆盖要求。
门禁假绿：实际不存在基线exit1；当前合法HEAD下仍因BSM改动/决策台账未同步而exit1，bash -e/npm同样非0。该已知下游问题归M425等既有后续范围，工作流正确暴露失败；不修改CSV/HTML刷绿，不称verify:predeploy全绿。
职责误述回归：本次完整hook读取+feature/main/override0/1/0，部署循环逐行读；改后没有再声称pre-push运行门禁。权限、超时、concurrency等未实现假设不是现存业务添加项，不新建questions。
本次所有源/三HTML SHA前后相等；未修源码、未安装、未部署、未提交推送。应用已读grilling/receiving/verification/ponytail-review方法，自己追问/复现/分类，不派第三席。两席B5合取及四值未核之前，ledger保持IN_PROGRESS。

## 5. 当前完整源、只读API及输出

```text
SOURCE tsconfig.base.json BYTES 303 SHA 13d49b947d918bf064b5f98ceaa049e1c954f88eafe736c438cd4372b44696db
1: {
2:   "compilerOptions": {
3:     "target": "ES2022",
4:     "module": "ESNext",
5:     "moduleResolution": "Bundler",
6:     "strict": true,
7:     "esModuleInterop": true,
8:     "forceConsistentCasingInFileNames": true,
9:     "skipLibCheck": true,
10:     "resolveJsonModule": true,
11:     "noUncheckedIndexedAccess": true
12:   }
13: }
SOURCE .github/CODEOWNERS BYTES 743 SHA b8b7a9f196172ca7a6220ab31b1d2909a773b5cdf796913b56c8722927ccef3d
1: # 强制点文件(2026-07-06):改动应经独立复核。
2: # 注意:CODEOWNERS 仅在"受保护分支 + require review"启用时被 GitHub 自动强制;
3: # 当前私有库免费版无法启用分支保护,故此文件当前仅"记录意图 + 一旦升级即生效"。
4: /ops/160/complex-deploy-api.sh              @jxh1979
5: /ops/160/complex-deploy-web.sh              @jxh1979
6: /ops/160/hooks/                             @jxh1979
7: /ops/160/propose-change.sh                  @jxh1979
8: /ops/ci/                                    @jxh1979
9: /.github/                                   @jxh1979
10: /tools/                                     @jxh1979
11: /package.json                               @jxh1979
12: /backend/src/main/resources/db/migration/   @jxh1979
SOURCE .github/workflows/predeploy-gates.yml BYTES 1335 SHA 4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d
1: # 文件版本：V0.02
2: name: predeploy-gates
3: # 独立治理门禁(GitHub 侧,与 .160 部署机隔离):PR 到 main 与 push 到 main 时静态跑
4: # delivery-gates 快子集(scope 登记/安全基线/后台矩阵/契约/阻断台账),catch 治理漂移。
5: # 无需数据库(全静态分析)。因私有库免费版无法设 required status check,此为可见信号 + 独立复现,
6: # 部署前硬阻断由 .160 部署脚本执行治理门禁；本地 pre-push 仅拦截直接推送 main。
7: on:
8:   pull_request:
9:     branches: [main]
10:   push:
11:     branches: [main]
12: jobs:
13:   predeploy:
14:     runs-on: ubuntu-latest
15:     steps:
16:       - uses: actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5 # v4.3.1 (AUD-068：固定完整 commit SHA，防可变标签漂移)
17:         with:
18:           fetch-depth: 0  # 门禁需完整历史；取不到基线时必须失败
19:       - uses: actions/setup-node@49933ea5288caeca8642d1e84afbd3f7d6820020 # v4.4.0 (AUD-068：固定完整 commit SHA，防可变标签漂移)
20:         with:
21:           node-version: "22"
22:       - name: install deps
23:         run: npm ci --ignore-scripts
24:       - name: verify:predeploy (governance gates)
25:         env:
26:           HTML_GATES_BASE: ${{ github.event_name == 'pull_request' && github.event.pull_request.base.sha || github.event.before }}
27:         run: npm run verify:predeploy
SOURCE apps/web/tsconfig.json BYTES 230 SHA 4cd04b3f2cb9cb3a95cd06f11f09f3cad61458159398a530f1c99e1817de1ac9
1: {
2:   "extends": "../../tsconfig.base.json",
3:   "compilerOptions": {
4:     "jsx": "react-jsx",
5:     "module": "ESNext",
6:     "moduleResolution": "Bundler",
7:     "types": ["vite/client"]
8:   },
9:   "include": ["src/**/*.ts", "src/**/*.tsx"]
10: }
SOURCE ops/ci/predeploy-gates.yml BYTES 1335 SHA 4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d
[镜像27行本次已全文读取，与上方生效文件字节相同，此处省略重复源码。]
SOURCE ops/160/hooks/pre-push BYTES 997 SHA 46aabdf33cb3496d83a71e4357ca85ba2ed9dbbfd4582ab53747bc50bb748d6b
1: #!/usr/bin/env bash
2: # 2026-07-05:禁止直推 main(补 GitHub 私有库免费版无分支保护的缺口)。开发者提示,非安全边界。
3: # 正道:推 feature 分支 → 开 PR → 独立复核 + predeploy 门禁绿 → gh pr merge。
4: # 紧急直推需 ALLOW_MAIN_PUSH=1 覆盖(留痕:请在 PR/提交说明记录原因)。
5: while read -r line; do
6:   # 稳健取 remote ref:git 传入「<localref> <localsha> <remoteref> <remotesha>」,
7:   # remoteref 目标 refs/heads/main 本身无空格;直接整行匹配该目标,避免源 ref 含空格致字段错位漏拦。
8:   if [[ " $line " == *" refs/heads/main "* ]]; then
9:     if [[ "${ALLOW_MAIN_PUSH:-0}" != "1" ]]; then
10:       echo "[pre-push] 拒绝直推 main。请走 PR 流程(ops/160/propose-change.sh)。" >&2
11:       echo "[pre-push] 确需直推:ALLOW_MAIN_PUSH=1 git push ...(并在 PR/说明记录原因)。" >&2
12:       exit 1
13:     fi
14:     echo "[pre-push] ALLOW_MAIN_PUSH=1 覆盖:放行直推 main(已留痕提醒)。" >&2
15:   fi
16: done
17: exit 0
SOURCE tools/verify-html-ledger-sync.mjs BYTES 3804 SHA f325b4ccd9a4948b1eb7cc196fed4b6767c2a68d10f84005bf69943ddcb21944
[直接依赖68行本次全文已读；原始文件可按SHA复核，实际行为命令输出见下文。]
SCRIPTS {"verify:predeploy": "npm run verify:html-ledger-sync && npm run verify:entry-anchors && npm run verify:html-param-tier && npm run verify:delivery-gates-docs && npm run verify:full-admin-matrix && npm run verify:domain-ownership-scope && npm run verify:matrix-vs-migration && npm run verify:data-isolation-spec && npm run verify:tenant-isolation && npm run verify:p0-contract && npm run verify:security-baseline && npm run verify:production-readiness-evidence && npm run verify:production-blockers && npm run verify:pii-encryption && npm run verify:test-db-isolation", "verify:delivery-gates-docs": "node tools/verify-delivery-gates.mjs"}
HEAD 242f487233d028173cb9198eecd079f5d877405c ORIGIN 242f487233d028173cb9198eecd079f5d877405c BRANCH main
GH_API repos/jxh1979/mall/codeowners/errors EXIT 0
{"errors":[]}
GH_API repos/jxh1979/mall/collaborators/jxh1979/permission EXIT 0
[API输出仅保留相关字段] {"permission":"admin","login":"jxh1979","role_name":"admin"}
GH_API repos/jxh1979/mall/actions/permissions/workflow EXIT 0
{"default_workflow_permissions":"read","can_approve_pull_request_reviews":false}
GH_API repos/jxh1979/mall/branches/main/protection EXIT 1
{"message":"Upgrade to GitHub Pro or make this repository public to enable this feature.","documentation_url":"https://docs.github.com/rest/branches/branch-protection#get-branch-protection","status":"403"}gh: Upgrade to GitHub Pro or make this repository public to enable this feature. (HTTP 403)

GH_API repos/actions/checkout/git/ref/tags/v4.3.1 EXIT 0
{"ref":"refs/tags/v4.3.1","node_id":"MDM6UmVmMTk3ODE0NjI5OnJlZnMvdGFncy92NC4zLjE=","url":"https://api.github.com/repos/actions/checkout/git/refs/tags/v4.3.1","object":{"sha":"34e114876b0b11c390a56381ad16ebd13914f8d5","type":"commit","url":"https://api.github.com/repos/actions/checkout/git/commits/34e114876b0b11c390a56381ad16ebd13914f8d5"}}
GH_API repos/actions/setup-node/git/ref/tags/v4.4.0 EXIT 0
{"ref":"refs/tags/v4.4.0","node_id":"MDM6UmVmMTg5NDc2OTA0OnJlZnMvdGFncy92NC40LjA=","url":"https://api.github.com/repos/actions/setup-node/git/refs/tags/v4.4.0","object":{"sha":"49933ea5288caeca8642d1e84afbd3f7d6820020","type":"commit","url":"https://api.github.com/repos/actions/setup-node/git/commits/49933ea5288caeca8642d1e84afbd3f7d6820020"}}
LOCK_SHA b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

```

## 6. 原生断言可复现代码
```javascript
const fs=require('node:fs'),cp=require('node:child_process'),assert=require('node:assert/strict');
const ts=require('typescript'),YAML=require('yaml');
const root=process.cwd(),read=ts.readConfigFile('apps/web/tsconfig.json',ts.sys.readFile);
assert.equal(read.error,undefined);
const cfg=ts.parseJsonConfigFileContent(read.config,ts.sys,root+'/apps/web');
assert.equal(cfg.errors.length,0);
for(const key of ['strict','esModuleInterop','forceConsistentCasingInFileNames','skipLibCheck','resolveJsonModule','noUncheckedIndexedAccess']) assert.equal(cfg.options[key],true);
assert.equal(cfg.options.target,ts.ScriptTarget.ES2022);assert.equal(cfg.options.module,ts.ModuleKind.ESNext);assert.equal(cfg.options.moduleResolution,ts.ModuleResolutionKind.Bundler);assert.equal(cfg.options.jsx,ts.JsxEmit.ReactJSX);
const options={...cfg.options,noEmit:true,types:[]};
function diagnose(source){
 const file=root+'/__m005_in_memory_only__.ts',host=ts.createCompilerHost(options);
 const orig=host.getSourceFile.bind(host);
 host.getSourceFile=(name,...rest)=>name===file?ts.createSourceFile(name,source,options.target,true):orig(name,...rest);
 const program=ts.createProgram([file],options,host);
 return ts.getPreEmitDiagnostics(program).map(d=>({code:d.code,message:ts.flattenDiagnosticMessageText(d.messageText,' ')}));
}
assert.deepEqual(diagnose('export const amount: number = [1, 2].at(0) ?? 0;'),[]);
const strictNull=diagnose('export const amount: number = null;'),unchecked=diagnose('const values: Record<string,number> = {}; export const amount: number = values["missing"];'),implicit=diagnose('export function twice(value) { return value * 2; }');
assert(strictNull.some(d=>d.code===2322));assert(unchecked.some(d=>d.code===2322));assert(implicit.some(d=>d.code===7006));
const bad=ts.parseJsonConfigFileContent({compilerOptions:{moduleResolution:'not-a-real-mode'}},ts.sys,root);
assert(bad.errors.some(d=>d.code===6046));
console.log(JSON.stringify({typescript:ts.version,configInputs:cfg.fileNames.length,options:cfg.options,positive:[],strictNull,unchecked,implicit,invalidConfig:bad.errors.map(d=>d.code)},null,2));
const doc=YAML.parseDocument(fs.readFileSync('.github/workflows/predeploy-gates.yml','utf8'));
assert.equal(doc.errors.length,0);const w=doc.toJS();assert.deepEqual(Object.keys(w.on).sort(),['pull_request','push']);
assert.deepEqual(w.on.pull_request.branches,['main']);assert.deepEqual(w.on.push.branches,['main']);
const steps=w.jobs.predeploy.steps;assert.equal(steps.length,4);assert.equal(steps[0].with['fetch-depth'],0);assert.equal(steps[1].with['node-version'],'22');
assert.equal(steps[2].run,'npm ci --ignore-scripts');assert.equal(steps[3].run,'npm run verify:predeploy');
assert.equal(steps[3].env.HTML_GATES_BASE,'$'+"{{ github.event_name == 'pull_request' && github.event.pull_request.base.sha || github.event.before }}");
assert(!w.jobs.predeploy['continue-on-error']);assert(steps.every(s=>!s['continue-on-error']&&!s.if));
assert(fs.readFileSync('.github/workflows/predeploy-gates.yml').equals(fs.readFileSync('ops/ci/predeploy-gates.yml')));
console.log(JSON.stringify({yaml:require('yaml/package.json').version,workflow:w,mirrorExact:true},null,2));
const entries=fs.readFileSync('.github/CODEOWNERS','utf8').split(/\r?\n/).map((x,i)=>({line:i+1,parts:x.trim().split(/\s+/)})).filter(x=>x.parts[0]&&!x.parts[0].startsWith('#'));
const tracked=cp.execFileSync('git',['ls-files','-z'],{encoding:'utf8'}).split('\0').filter(Boolean);
function matches(p,f){assert(p.startsWith('/')&&!/[*?![\]]/.test(p));return p.endsWith('/')?f.startsWith(p.slice(1)):f===p.slice(1);}
assert.equal(entries.length,9);const coverage=entries.map(e=>{assert.deepEqual(e.parts.slice(1),['@jxh1979']);const paths=tracked.filter(f=>matches(e.parts[0],f));assert(paths.length>0);return {line:e.line,pattern:e.parts[0],count:paths.length,first:paths[0],last:paths.at(-1)};});
function owners(f){const e=entries.filter(e=>matches(e.parts[0],f)).at(-1);return e?e.parts.slice(1):[];}
assert.deepEqual(owners('.github/CODEOWNERS'),['@jxh1979']);assert.deepEqual(owners('.github/workflows/predeploy-gates.yml'),['@jxh1979']);
assert.deepEqual(owners('nested/tools/test.mjs'),[]);assert.deepEqual(owners('package.json.bak'),[]);assert.deepEqual(owners('tools-extra/test.mjs'),[]);assert.deepEqual(owners('Tools/check.mjs'),[]);
console.log(JSON.stringify({codeownersLiteralPatternsOnly:true,coverage,positiveSelfAndWorkflow:true,negativePrefixAndCase:true},null,2));

```

## 7. 本次原生/入口执行输出
```text
{
  "typescript": "5.9.3",
  "configInputs": 353,
  "options": {
    "target": 9,
    "module": 99,
    "moduleResolution": 100,
    "strict": true,
    "esModuleInterop": true,
    "forceConsistentCasingInFileNames": true,
    "skipLibCheck": true,
    "resolveJsonModule": true,
    "noUncheckedIndexedAccess": true,
    "jsx": 4,
    "types": [
      "vite/client"
    ]
  },
  "positive": [],
  "strictNull": [
    {
      "code": 2322,
      "message": "Type 'null' is not assignable to type 'number'."
    }
  ],
  "unchecked": [
    {
      "code": 2322,
      "message": "Type 'number | undefined' is not assignable to type 'number'.   Type 'undefined' is not assignable to type 'number'."
    }
  ],
  "implicit": [
    {
      "code": 7006,
      "message": "Parameter 'value' implicitly has an 'any' type."
    }
  ],
  "invalidConfig": [
    6046
  ]
}
{
  "yaml": "2.9.0",
  "workflow": {
    "name": "predeploy-gates",
    "on": {
      "pull_request": {
        "branches": [
          "main"
        ]
      },
      "push": {
        "branches": [
          "main"
        ]
      }
    },
    "jobs": {
      "predeploy": {
        "runs-on": "ubuntu-latest",
        "steps": [
          {
            "uses": "actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5",
            "with": {
              "fetch-depth": 0
            }
          },
          {
            "uses": "actions/setup-node@49933ea5288caeca8642d1e84afbd3f7d6820020",
            "with": {
              "node-version": "22"
            }
          },
          {
            "name": "install deps",
            "run": "npm ci --ignore-scripts"
          },
          {
            "name": "verify:predeploy (governance gates)",
            "env": {
              "HTML_GATES_BASE": "${{ github.event_name == 'pull_request' && github.event.pull_request.base.sha || github.event.before }}"
            },
            "run": "npm run verify:predeploy"
          }
        ]
      }
    }
  },
  "mirrorExact": true
}
{
  "codeownersLiteralPatternsOnly": true,
  "coverage": [
    {
      "line": 4,
      "pattern": "/ops/160/complex-deploy-api.sh",
      "count": 1,
      "first": "ops/160/complex-deploy-api.sh",
      "last": "ops/160/complex-deploy-api.sh"
    },
    {
      "line": 5,
      "pattern": "/ops/160/complex-deploy-web.sh",
      "count": 1,
      "first": "ops/160/complex-deploy-web.sh",
      "last": "ops/160/complex-deploy-web.sh"
    },
    {
      "line": 6,
      "pattern": "/ops/160/hooks/",
      "count": 1,
      "first": "ops/160/hooks/pre-push",
      "last": "ops/160/hooks/pre-push"
    },
    {
      "line": 7,
      "pattern": "/ops/160/propose-change.sh",
      "count": 1,
      "first": "ops/160/propose-change.sh",
      "last": "ops/160/propose-change.sh"
    },
    {
      "line": 8,
      "pattern": "/ops/ci/",
      "count": 1,
      "first": "ops/ci/predeploy-gates.yml",
      "last": "ops/ci/predeploy-gates.yml"
    },
    {
      "line": 9,
      "pattern": "/.github/",
      "count": 2,
      "first": ".github/CODEOWNERS",
      "last": ".github/workflows/predeploy-gates.yml"
    },
    {
      "line": 10,
      "pattern": "/tools/",
      "count": 90,
      "first": "tools/apply-openapi-p0-examples.mjs",
      "last": "tools/verify-test-db-isolation.mjs"
    },
    {
      "line": 11,
      "pattern": "/package.json",
      "count": 1,
      "first": "package.json",
      "last": "package.json"
    },
    {
      "line": 12,
      "pattern": "/backend/src/main/resources/db/migration/",
      "count": 377,
      "first": "backend/src/main/resources/db/migration/V001__baseline.sql",
      "last": "backend/src/main/resources/db/migration/V399__pii_idempotency_response_body_envelope.sql"
    }
  ],
  "positiveSelfAndWorkflow": true,
  "negativePrefixAndCase": true
}

COMMAND ["node", "tools/verify-html-ledger-sync.mjs", "242f487233d028173cb9198eecd079f5d877405c"] ENV_HTML_GATES_BASE 242f487233d028173cb9198eecd079f5d877405c EXIT 1 EXPECTED 1
[html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。比对范围：工作区（含未提交改动）↔ 基线 242f487233d028173cb9198eecd079f5d877405c@242f4872。
  已改动的 HTML：
    - docs/business-system-map-for-operations-v0.1.html
  必须在同一批改动内一并更新：docs/prd-audit/business-decision-ledger.md
  依据：用户 2026-08-03 拍板——台账保持完整，改 HTML 必须同 PR 同步台账。
  若本次确为纯排版/错别字、不涉及口径，请在台账内追加一行说明后再提交。

COMMAND ["node", "tools/verify-html-ledger-sync.mjs", "M005-NONEXISTENT-BASE"] ENV_HTML_GATES_BASE 242f487233d028173cb9198eecd079f5d877405c EXIT 1 EXPECTED 1
fatal: Not a valid object name M005-NONEXISTENT-BASE
[html-ledger-sync] FAIL: 取不到基线 M005-NONEXISTENT-BASE，无法判定本次改动范围。
  无法判定不等于通过。请先 git fetch origin（CI 侧 actions/checkout 需 fetch-depth: 0）。
  如需换基线：node tools/verify-html-ledger-sync.mjs <基线ref>

COMMAND ["bash", "-e", "-c", "npm run verify:predeploy"] ENV_HTML_GATES_BASE 242f487233d028173cb9198eecd079f5d877405c EXIT 1 EXPECTED 1

> entertainment-complex-management@0.1.0 verify:predeploy
> npm run verify:html-ledger-sync && npm run verify:entry-anchors && npm run verify:html-param-tier && npm run verify:delivery-gates-docs && npm run verify:full-admin-matrix && npm run verify:domain-ownership-scope && npm run verify:matrix-vs-migration && npm run verify:data-isolation-spec && npm run verify:tenant-isolation && npm run verify:p0-contract && npm run verify:security-baseline && npm run verify:production-readiness-evidence && npm run verify:production-blockers && npm run verify:pii-encryption && npm run verify:test-db-isolation


> entertainment-complex-management@0.1.0 verify:html-ledger-sync
> node tools/verify-html-ledger-sync.mjs

[html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。比对范围：工作区（含未提交改动）↔ 基线 242f487233d028173cb9198eecd079f5d877405c@242f4872。
  已改动的 HTML：
    - docs/business-system-map-for-operations-v0.1.html
  必须在同一批改动内一并更新：docs/prd-audit/business-decision-ledger.md
  依据：用户 2026-08-03 拍板——台账保持完整，改 HTML 必须同 PR 同步台账。
  若本次确为纯排版/错别字、不涉及口径，请在台账内追加一行说明后再提交。

COMMAND ["node", "-e", "const ts=require('typescript'); const r=ts.readConfigFile('apps/web/tsconfig.json',ts.sys.readFile); const c=ts.parseJsonConfigFileContent(r.config,ts.sys,process.cwd()+'/apps/web'); const p=ts.createProgram(c.fileNames,{...c.options,noEmit:true}); const d=[...p.getOptionsDiagnostics(),...p.getGlobalDiagnostics()]; console.log(JSON.stringify({files:c.fileNames.length,optionsAndGlobalDiagnostics:d.map(x=>({code:x.code,message:ts.flattenDiagnosticMessageText(x.messageText,' ')}))})); process.exit(d.length?1:0)"] ENV_HTML_GATES_BASE 242f487233d028173cb9198eecd079f5d877405c EXIT 0 EXPECTED 0
{"files":353,"optionsAndGlobalDiagnostics":[]}

SOURCE_HTML_SHA_UNCHANGED {"tsconfig.base.json": "13d49b947d918bf064b5f98ceaa049e1c954f88eafe736c438cd4372b44696db", ".github/CODEOWNERS": "b8b7a9f196172ca7a6220ab31b1d2909a773b5cdf796913b56c8722927ccef3d", ".github/workflows/predeploy-gates.yml": "4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d", "docs/business-enum-quick-reference-v0.1.html": "4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66", "docs/business-system-map-for-operations-v0.1.html": "a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa", "docs/platform-governance-map-for-operations-v0.1.html": "c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c"}
WORKTREE_SOURCE_DIFF
 .github/workflows/predeploy-gates.yml | 4 ++--
 1 file changed, 2 insertions(+), 2 deletions(-)

LOCK_ACTUAL b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
LOCK_SIDECAR b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
LOCK-AMEND-COUNT: 1
| M005 `治理门禁与 CI 基线／配置与工程入口／第3段` | IN_PROGRESS | 2 | 2 | - | 0 | B4-r2双PASS，014已修出队；进行B5-r2完整模块双审 |
DOCS_GATE_EXIT 0

> entertainment-complex-management@0.1.0 verify:delivery-gates-docs
> node tools/verify-delivery-gates.mjs

DELIVERY_GATES_DOCS_PASS


```

## 8. 当前hook与部署职责证据
```text
LOCK_MAPPING_LINE 6384 M005 → [GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018]
DEPLOY_CONTEXT ops/160/complex-deploy-api.sh
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
DEPLOY_CONTEXT ops/160/complex-deploy-web.sh
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
HOOK feature EXIT 0 
HOOK main EXIT 1 [pre-push] 拒绝直推 main。请走 PR 流程(ops/160/propose-change.sh)。
[pre-push] 确需直推:ALLOW_MAIN_PUSH=1 git push ...(并在 PR/说明记录原因)。

HOOK override EXIT 0 [pre-push] ALLOW_MAIN_PUSH=1 覆盖:放行直推 main(已留痕提醒)。


```


CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
UTC 2026-09-22T10:47:37.609729+00:00

# M005 当前锁B5-r2完整独立审计
版本：V0.02

当前模块三源52行18GEN；B2本轮已双PASS，findings V3.40三当前项queue空；跳新增B3/B4，B3累计2。先派Grok，本票封存前未读本次Grok B5。此轮是完整模块重新判断，不只检查旧014修复。

## 源阅读及全字段覆盖
本B5先对三源和直接依赖实算SHA，均与本轮B1 self V0.10所完整读取字节相同。明确复用该次完整源码阅读；下面按所有字段重新判断，不继承B1/B2 PASS。源SHA见末尾本次实算。
- tsconfig.base.json L1–13：根对象只有compilerOptions，九项target ES2022/module ESNext/moduleResolution Bundler/strict true/esModuleInterop true/forceConsistentCasingInFileNames true/skipLibCheck true/resolveJsonModule true/noUncheckedIndexedAccess true。B5原生解析354 web输入，errors[]，继承存在。strict与unchecked两个TS2322负例、正例无错误。
- .github/CODEOWNERS L1–12：三个注释声明独立复核意图/保护条件/当前限制；九工程路径为api部署、web部署、hooks目录、propose-change、ops/ci、.github、tools、package.json、migration目录，owner均@jxh1979。B5逐路径存在/语法验证，当前B1真实保护403与owner admin不冒称此步重新API。路由不构成产品岗位；不增第二owner。
- .github/workflows/predeploy-gates.yml L1–27：V0.02/name；L3–6说明静态CI信号与部署/hook责任。on两事件branches main；job ubuntu-latest；checkout固定34e114…+fetch-depth0；setup-node固定49933e…+22；npm ci --ignore-scripts；HTML_GATES_BASE精确PRbase.sha/pushbefore表达式；npm run verify:predeploy，无continue-on-error。B5 YAML全部原生解析并与镜像全字节相等，docs checker pass，hook与真实verify失败传播再跑。

## 18GEN全量三线
- GEN-001：三工程入口均有消费者：web extends、仓库路径路由、PR/push治理；删除会移除现有约束。无新的业务功能。
- GEN-002：当前52行声明均解析正确；原生354输入配置/正负诊断与hook三分支实跑；不等同全业务编译。
- GEN-003：负例undefined和数组越界均TS2322，正例无诊断；hook拒绝exit1，predeploy红exit1。断言真正能失败。
- GEN-004：事件SHA经环境传给工具，非法ref在本轮B1已实测拒绝，源码同字节复用此证据。无不可信标题拼shell。
- GEN-005：路由账户当前B1只读API=admin、保护403；本B5不重复API，明确沿用同轮证据不冒称新查。未放宽产品权限。
- GEN-006：无业务状态转换或金额写入，配置解析/镜像一致性已核。事务边界不适用。
- GEN-007：只有静态检查与编译配置，重复运行不会重复扣款。无依据新增concurrency。
- GEN-008：TS九选项真实继承；YAML live/mirror字节一致、eventBase逐字、Node22。接口业务契约无改。
- GEN-009：verify链用&&且无continue-on-error；当前PGM台账红真实传出exit1，没有吞错/假绿。
- GEN-010：三文件无秘密、动作pin官方SHA在同轮B1 API匹配；不安装依赖/不发业务数据。未宣称全仓安全。
- GEN-011：具名步骤及明确错误指向PGM台账；本B5原生检查输出留档。记录失败不替换为通过。
- GEN-012：9选项/9规则/4步骤均有限；无本模块无界业务负载。沿用已核官方默认，不因未显式timeout发明缺陷。
- GEN-013：TS原生解析与镜像门禁B5再实跑；Node22是声明，宿主24不假冒22全构建。未真npm ci。
- GEN-014：没有DDL/SQL；CODEOWNERS对migration目录只是路由，不等于迁移执行与验收。
- GEN-015：没有业务UI，键盘焦点页面验收不适用，不冒称界面完成。
- GEN-016：V0.02 L6与当前hook0/1/0和此前已读部署循环对应；同字节沿用阅读，未重复修注释。
- GEN-017：全部技术性；冲突/细化/添加均无当前命题。编译/仓库治理非产品岗位或审批，012–014有出处证据。
- GEN-018：本B5以当前文件哈希+源完整阅读复用+重新运行证据下结论，未读当前Grok；不拿任何旧PASS替代。

## 第一性原理三问
1. 为什么存在：共享编译约束、工程评审路由、GitHub独立静态门禁各有真实消费，没有HTML指定某工具名称也不该删。四分类技术性成立，不借此加业务要求。
2. 什么实际执行：本B5重跑TS原生配置/两负一正诊断、YAML/9owner路径、镜像门禁、hook3分支、显式HEAD predeploy，原始输出在末尾；本轮B1 GitHub API证据明确复用，不冒称本B5全CI或构建完成。
3. 哪些前提未证：未安装/真实npm ci、未全业务编译/构建、未触发GitHubCI；当前保护不强制且ref与工作区分叉；不把既有CI成功当本工作区绿。以上不是本模块新增待修要求。

## 顺从及对抗
顺从：52行声明和当前工具原生解析、消费者、职责逐项匹配。对抗：TS非法赋值/数组索引不能悄然成功；main默认拒绝而override明确放行，证明hook不是不可绕过安全边界；真正PGM台账红保留并终止链，证明没有continue-on-error吞错。基线语义按同轮B2已核：origin ref ebb1eb99，merge-base88c7b465；本次显式HEAD取e456ba96，两种比较不冒称同次。
未发现新问题。不为理论配置增强增timeout/concurrency/permissions/files[]/owner覆盖；没有HTML修改或业务添加。

## 全部finding与队列
M005-012当前TS技术性/无缺陷/否；M005-013当前CODEOWNERS技术性/无缺陷/否；M005-014当前workflow技术性/历史缺陷已修/否。原014错误注释已由旧B3r2修复，当前镜像和实际hook一致，本轮不重修。
001—006原范围历史缺陷已修队列否、007—011原范围登记队列否，保留旧ID不复用；不冒称其现归属模块已完成。当前新CX无，未答questions无，三文件HTML冲突无。
真实PGM业务决策台账同步红→M426/工具M008，入口快照漂移原登记M435/M436；不在本模块跨界修源。不称全仓或发布PASS。

## 本次真实命令输出
原生命令与探针正文对应本轮B1相同命令，本B5重新执行；以下非复制旧结果。
```
{"native": {"cmd": ["node", "-"], "exit": 0, "out": "{\"ts\":{\"version\":\"5.9.3\",\"files\":354,\"errors\":[],\"options\":{\"target\":9,\"module\":99,\"moduleResolution\":100,\"strict\":true,\"esModuleInterop\":true,\"forceConsistentCasingInFileNames\":true,\"skipLibCheck\":true,\"resolveJsonModule\":true,\"noUncheckedIndexedAccess\":true,\"jsx\":4,\"types\":[\"vite/client\"]}},\"negative\":[{\"code\":2322,\"msg\":\"Type 'undefined' is not assignable to type 'number'.\"},{\"code\":2322,\"msg\":\"Type 'number | undefined' is not assignable to type 'number'.   Type 'undefined' is not assignable to type 'number'.\"}],\"positive\":[],\"workflow\":{\"events\":{\"pull_request\":{\"branches\":[\"main\"]},\"push\":{\"branches\":[\"main\"]}},\"runner\":\"ubuntu-latest\",\"steps\":[{\"uses\":\"actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5\",\"with\":{\"fetch-depth\":0}},{\"uses\":\"actions/setup-node@49933ea5288caeca8642d1e84afbd3f7d6820020\",\"with\":{\"node-version\":\"22\"}},{\"name\":\"install deps\",\"run\":\"npm ci --ignore-scripts\"},{\"name\":\"verify:predeploy (governance gates)\",\"env\":{\"HTML_GATES_BASE\":\"${{ github.event_name == 'pull_request' && github.event.pull_request.base.sha || github.event.before }}\"},\"run\":\"npm run verify:predeploy\"}],\"mirror\":true},\"owners\":[{\"path\":\"/ops/160/complex-deploy-api.sh\",\"owner\":\"@jxh1979\",\"exists\":true},{\"path\":\"/ops/160/complex-deploy-web.sh\",\"owner\":\"@jxh1979\",\"exists\":true},{\"path\":\"/ops/160/hooks/\",\"owner\":\"@jxh1979\",\"exists\":true},{\"path\":\"/ops/160/propose-change.sh\",\"owner\":\"@jxh1979\",\"exists\":true},{\"path\":\"/ops/ci/\",\"owner\":\"@jxh1979\",\"exists\":true},{\"path\":\"/.github/\",\"owner\":\"@jxh1979\",\"exists\":true},{\"path\":\"/tools/\",\"owner\":\"@jxh1979\",\"exists\":true},{\"path\":\"/package.json\",\"owner\":\"@jxh1979\",\"exists\":true},{\"path\":\"/backend/src/main/resources/db/migration/\",\"owner\":\"@jxh1979\",\"exists\":true}]}\n", "err": ""}, "docs": {"cmd": ["npm", "run", "verify:delivery-gates-docs"], "exit": 0, "out": "\n> entertainment-complex-management@0.1.0 verify:delivery-gates-docs\n> node tools/verify-delivery-gates.mjs\n\nDELIVERY_GATES_DOCS_PASS\n", "err": ""}, "repo": {"cmd": ["gh", "repo", "view", "--json", "nameWithOwner,isPrivate"], "exit": 0, "out": "{\"isPrivate\":true,\"nameWithOwner\":\"jxh1979/mall\"}\n", "err": ""}, "hook": [{"name": "feature", "exit": 0, "out": "", "err": ""}, {"name": "main", "exit": 1, "out": "", "err": "[pre-push] 拒绝直推 main。请走 PR 流程(ops/160/propose-change.sh)。\n[pre-push] 确需直推:ALLOW_MAIN_PUSH=1 git push ...(并在 PR/说明记录原因)。\n"}, {"name": "override", "exit": 0, "out": "", "err": "[pre-push] ALLOW_MAIN_PUSH=1 覆盖:放行直推 main(已留痕提醒)。\n"}]}

{"files": {"tsconfig.base.json": {"sha": "13d49b947d918bf064b5f98ceaa049e1c954f88eafe736c438cd4372b44696db", "lines": 13}, ".github/CODEOWNERS": {"sha": "b8b7a9f196172ca7a6220ab31b1d2909a773b5cdf796913b56c8722927ccef3d", "lines": 12}, ".github/workflows/predeploy-gates.yml": {"sha": "4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d", "lines": 27}, "ops/ci/predeploy-gates.yml": {"sha": "4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d", "lines": 27}, "apps/web/tsconfig.json": {"sha": "4cd04b3f2cb9cb3a95cd06f11f09f3cad61458159398a530f1c99e1817de1ac9", "lines": 10}, "ops/160/hooks/pre-push": {"sha": "e38ccd33e40065045c24fa8d4bb08d35f519a753ce2520884fee7103e4e77f85", "lines": 17}, "tools/verify-delivery-gates.mjs": {"sha": "cbdfe7c023c3a5cc623b4f1742c56ca46238f039d701150d8d1b6abbe9102b8f", "lines": 333}, "ops/160/complex-deploy-api.sh": {"sha": "8bd3cd0c514257531315b1efee6b137e0fe0b6dba3e7bca41e98ad7a53fd815b", "lines": 247}, "ops/160/complex-deploy-web.sh": {"sha": "5549661743a48cf724e66ea3d0d8d656a5f2eaa7f2642bdbad31c665d02828c1", "lines": 72}}, "predeploy": {"exit": 1, "out": "\n> entertainment-complex-management@0.1.0 verify:predeploy\n> npm run verify:html-ledger-sync && npm run verify:entry-anchors && npm run verify:html-param-tier && npm run verify:delivery-gates-docs && npm run verify:full-admin-matrix && npm run verify:domain-ownership-scope && npm run verify:matrix-vs-migration && npm run verify:data-isolation-spec && npm run verify:tenant-isolation && npm run verify:p0-contract && npm run verify:security-baseline && npm run verify:production-readiness-evidence && npm run verify:production-blockers && npm run verify:pii-encryption && npm run verify:test-db-isolation\n\n\n> entertainment-complex-management@0.1.0 verify:html-ledger-sync\n> node tools/verify-html-ledger-sync.mjs\n\n", "err": "[html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。比对范围：工作区（含未提交改动）↔ 基线 HEAD@e456ba96。\n  已改动的 HTML：\n    - docs/platform-governance-map-for-operations-v0.1.html\n  必须在同一批改动内一并更新：docs/prd-audit/business-decision-ledger.md\n  依据：用户 2026-08-03 拍板——台账保持完整，改 HTML 必须同 PR 同步台账。\n  若本次确为纯排版/错别字、不涉及口径，请在台账内追加一行说明后再提交。\n"}, "lock": "b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458"}

```

## 结论
本席B5 PASS。当前整个52行/18GEN无待修finding。必须等当前Grok完整报告并做四值合取后才能模块PASS；该票不代外审。未修源码/HTML/锁，不改B3计数。
