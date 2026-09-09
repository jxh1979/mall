CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M008 B1 Codex完整独立审计
版本：V0.01

## 1. 输入与独立性
当前471模块计划M008“治理门禁与 CI 基线／检查及生成工具／第1段”，17文件1530行，19判据GEN-001–018及PGM-036。不是M007的79判据。本席Codex，经SSH在/home/james/complex-management主工作区执行；HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c。ledger V1.90 IN_PROGRESS，B3次数0。
Grok真实结束exit0，仅见适配器指定路径M008-b1-grok-raw.md及退出码。本席随后独立全文读取17文件和19定义，实际回读PGM-036原句，再执行语法、真实入口、已有回归与独立反例；本报告写入前未读取Grok正文/原始JSON。当前不修复。先完整审完再收本批问题。
本席结论：FAIL。六项缺陷候选详见下文，永久ID及修复归属待完整双席B2，不能先改。
本轮开工前实际核实17个技能文件存在；重读using-superpowers，首次完整读取batch-grill-me与ubiquitous-language，其余按本会话已读grilling/codebase-design/diagnosing-bugs/systematic-debugging/verification-before-completion/ponytail-review等使用。遵V0.17收窄：只借追问、术语、根因和最小性结构，自取事实、不派第三主体、不新增词汇表/审计文件类型、不向用户问事实。

## 2. 逐文件三线覆盖与四分类
| 范围 | 第一性原理用途 | 顺从式/对抗式事实 | 分类 |
|---|---|---|---|
| html-param-tier-baseline.json 全10行 | 保存已登记例外的稳定key；不是新的业务参数表 | JSON有效、entries空；现有回归验证新增key拒绝、保持不变通过；根工具全读 | 技术性，不发明业务 |
| refresh-entry-doc-baseline.mjs 全122行 | 从OpenAPI/迁移/surefire写文档事实快照，历史变更不改 | 正本--check正确报README从862到当前55tests漂移；隔离check红→write0→check绿，历史句保留；无报告保留旧数 | 技术性，文档生成，非业务数字定义 |
| verify-delivery-gates.mjs 全325行 | 确保既有门禁入口、说明标记、CI镜像/完整历史/部署列表一致 | 正本通过，已有反例能红；但echo替代必跑检查仍通过，重复条目造成同一错误3次 | 技术性；CX-01/05 |
| verify-entry-anchors.mjs 全275行 | CSV解析/HTML正文/台账矩阵和文档引用一致性检查 | 正本正确报6处输入漂移；CSV有效/非法回归通过；空entry_body仍PASS见CX-02 | 技术性，解析与引用校验 |
| verify-html-ledger-sync.mjs 全68行 | 当前批HTML改动必须同步既有决策台账 | 当前正本缺同步正确FAIL；隔离删除台账却PASS；非法基线命令被拆执行而PASS | 技术性；CX-03/06 |
| verify-html-param-tier.mjs 全198行 | 检查已经要求的数值层级标注及基线不扩增 | 当前正本PASS，豁免与基线回归通过；物理换行导致错误判定；非法基线被拆执行 | 技术性；CX-04/06，不改HTML业务层级原意 |
| 99-predeploy-final.log 全138行 | 保存当时完整链输出和EXIT=0 | 本地路径wt-gates、720ops、当时注册表/业务计数均属历史；不是本次当前验证 | 技术性历史证据 |
| A-worktree-uncommitted.log 全42行 | 当时未提交HTML对照/台账同步与未同步/参数负例 | 旧0、新相应0/1记录完整；本次独立回归覆核同类路径；日志不修改 | 技术性历史证据 |
| B-committed.log 全17行 | 已提交HTML而台账未提交时拒绝记录 | 与提交区口径一致；不是承诺未提交台账能覆盖已提交HTML | 技术性历史证据 |
| C-no-baseline.log 全39行 | 缺基线旧假绿/新红的当时对照 | 本次普通absent-ref确实1；特殊参数缺口另记CX-06 | 技术性历史证据 |
| D-baseline-legacy-pass.log 全10行 | 当时6条存量警告放行 | 当前entries为0不推翻当时记录，也不能把6说成当前数 | 技术性历史证据 |
| E-ignore-marker.log 全5行 | 记录历史裸标记放行结果 | 当前已改为拒绝，已有回归实证；旧log原文保留，不充当前规范 | 技术性历史证据 |
| F-ci-baseline-evidence.log 全7行 | 2026-08-07特定CI run的基线日志摘录 | 有run/date/commit上下文；本轮不声称已在线重新核验该run | 技术性历史证据 |
| G-merge-pr-guards.log 全24行 | 旧main/dirty/no-remote参数及gh查询记录 | 本次m001-regression实测merge stable/watch/late；不拿旧未测真合并当本次真发布 | 技术性历史证据 |
| I1-baseline-ratchet.log 全13行 | 旧基线增删key行为 | 当前回归新增key红、不变绿；不把旧9条当当前0条 | 技术性历史证据 |
| I2-ignore-granularity.log 全21行 | 旧带数值豁免部分/全部的差别 | 当前回归相同边界1/0，语义保持 | 技术性历史证据 |
| m001-regression.mjs 全216行 | 对真实门禁/部署基线与模拟merge留可失败回归 | 本次实际exit0；断言涵盖红/绿，未覆盖的新反例另列，不因旧测试绿省略全量审计 | 技术性回归，无业务发明 |

所有源文件当前blob均与M007 B3 after树9f63094e21e4ed94a29b29071af5bd07cf99fd6a相同，Grok阶段未改这17文件。主索引文件字节SHA与此前不同，当前为eeb0e20f0c85989b6b902fba051c6b7b2f3051b870cacabd0c2ab6e2211b42aa；git diff --cached --raw为空，不能仅凭stat缓存变化断言新增暂存修改，也不擅自复原索引。
PGM-036实际原句“设备包括工作手机、平板、一体机、扫码设备、综合体入场三道环节设备……设备要绑定员工、班次、点位和状态。”本模块entry指文档入口，gate指代码检查，不是物理三道设备；没有设备绑定实现或相反规则。按冲突→细化→技术性→添加逐条判后，本批均技术性；无冲突、无添加、无修改HTML申请。
术语对齐：历史log=过去一次运行的证据；当前验证=本轮实际命令；技术性门禁=查已有工程约定的程序；源码PASS≠全系统上线。用此边界避免把“工程问题”误解为改写业务原意。

## 3. 全部临时finding
| 临时号 | 文件行 | 事实与复现 | 冻结判据/类型 | 工程缺陷与最小方向 |
|---|---|---|---|---|
| CX-01 | verify-delivery-gates.mjs:153–156、312–315 | 隔离复制真实被读输入，原始PASS；仅把predeploy中npm run verify:html-param-tier换成echo verify:html-param-tier，仍PASS，实际必跑命令已消失；includes只看字串 | GEN-002/004/008/009/013，类型2/4 | 是；按既有命令结构识别实际执行，不能用echo/注释/相似名称冒充，不新增门禁项目 |
| CX-02 | verify-entry-anchors.mjs:113–116 | 有效CSV一行与HTML正文匹配时PASS；只清空该行entry_body仍PASS并打印“注册表条目正文逐字…一致”，全正文保护被跳过。当前真实1228行无非历史空body | GEN-002/004/009/016，类型2/4 | 是；现有必查正文缺失须失败，不为所有CSV重造框架 |
| CX-03 | verify-html-ledger-sync.mjs:50–59 | 隔离Git基线含完整台账；HTML改动且台账不改→1；把台账删掉→0，输出“决策台账已同步”。changed只判断路径出现，不区分删除 | GEN-002/006/009/011，类型2/4 | 是；收窄“已同步”为台账仍存在且有效改动，保留原同批规则 |
| CX-04 | verify-html-param-tier.mjs:54–85 | 同一句“测试期限30天（业务参数，按项目可配）”整行0，仅在数值后换行1；另一个独立p中3次无标层级、与有层级p放同物理行，反而0。物理行被当作业务句 | GEN-002/008/009/016，类型2/4 | 是；按既有句/块语义检查，不由排版决定合格与否，不改业务层级规则/豁免既有作用 |
| CX-05 | verify-delivery-gates.mjs:123–149 | requiredDeliveryCommands中5类重复，共6个额外重复项；移除positive-propagation一条实际命令，完全相同错误打印3次。重复始终执行，无独立用途 | GEN-001/011，类型2/4 | 是（冗余）；删除重复常量，保留每类一次，勿顺便格式化无关行 |
| CX-06 | verify-html-ledger-sync.mjs:12/30–50；verify-html-param-tier.mjs:22/122–164 | 隔离Git中absent-ref都exit1；参数“HEAD;echo M008_BASE_ARG”不是有效ref，却两工具exit0，stderr有git merge-base用法错误。基线被拼入shell并拆成命令，后半段成功遮掉前半段错误 | GEN-002/004/009/010/018，类型2/4 | 是；Git进程使用参数数组传递，把基线当一个值；不增加新角色/审批/自创策略。只用无害echo实测，无敏感数据/生产动作 |

这6条是首个相应输入便能触发的可重现行为，不依赖并发/网络/多重偶发组合。CX-06是标准非法输入应拒绝而错误通过，不要求编造攻击叙事。全部队列归属须B2双席定稿，本阶段不动源。
额外观察（须B2一并归并、不进当前建议修复队列）：
- CX-07：生成器正常check/write/check成立；README当前漂移是其输入快照问题，README属M435、00-entry属M436；这里不改。生成器未承诺自动版本管理，不擅加该功能；如本审计B3实际更新交付文件，仍须由本席按用户规则递增其自身版本。隔离样例保留版本头只记录实际行为。
- CX-08：正本entry-anchors的6错误及其既有51行关联测试的首次assert失败都来自当前HTML/输入镜像未同步；与本模块源码是否正确拒绝要分开。registry属M427，authority ledger属M425，coverage属M428；decision ledger属M426。留待所属模块，不重写HTML或为了绿而豁免。
- CX-09：10份log为历史不可改证据；rg在非审计docs/AGENTS/README中未找到这些.log路径的引用。没有证据证明它们被当作当前承诺，不删历史、不改旧PASS。裸豁免log与当前拒绝差异明确记录。
- CX-10：m001-regression本次实际通过，正常失败断言能执行；它的覆盖不含CX01–06全部反例，不把测试绿当本模块绿。无额外测试工程建议，B3如修执行行为再把最小测试归到定稿ID。

## 4. 现有验证及限制
本轮6个.mjs的node --check均0；m001-regression完整执行0；delivery gate0；param-tier0；refresh --check=1明确README漂移；entry-anchors=1报告6处输入漂移；ledger-sync=1报告业务HTML变更尚未同步台账。后3个真实失败全保留，不伪称全绿、不归为Grok交付失败。
相关tools/verify-entry-anchors.test.mjs全文51行已作为最窄依赖阅读；实际exit1，首次对复制正本CSV的基线断言遇上述同6错误，未走到后续raw排除反例。未说本轮这个关联测试通过。
本轮未跑全仓verify:predeploy父链，未访问生产DB、未改正本业务HTML/源码/基线/日志、未真实push/PR/merge/部署。生成器、删除台账、无害参数和门禁反例全在.scratch可丢弃资料中运行。历史CI日志未重新在线查询，不冒称当前CI绿。

## 5. 全部19判据逐项结果
| ID | 结果与依据 |
|---|---|
| GEN-001 | FAIL：CX-05重复常量及重复报错；其余17文件用途均已登记，不把历史log当死代码删除。 |
| GEN-002 | FAIL：CX-01–04/06均有当前真实入口与隔离反例；语法均0不是行为PASS。 |
| GEN-003 | 本次m001-regression实际0，红绿断言有效；关联51行测试因当前输入漂移在第一断言FAIL，限制明示；新反例证明覆盖不足但不另建工程。 |
| GEN-004 | FAIL：CX-01/02的命令/CSV内容校验及CX-06基线参数语义缺口已实证。 |
| GEN-005 | 不含产品服务端权限；CLI/文件检查不替代业务授权，未添加业务角色。 |
| GEN-006 | FAIL：CX-03删除台账被报同步，完整性丢失；无数据库事务路径。 |
| GEN-007 | baseline棘轮新增key拒绝与原值通过；生成器check/write/check验证收敛；无并发业务消费路径。 |
| GEN-008 | FAIL：CX-01实际命令和字串不等价；CX-04物理行和句语义不等价。原CSV严格引号/列数回归已执行。 |
| GEN-009 | FAIL：上述错误伪成功/误拒绝；普通缺ref确实1，非法shell参数却0，不能笼统判保护有效。 |
| GEN-010 | FAIL：CX-06未将基线作为独立参数，实际无害命令被解释执行；本次未触碰秘密。 |
| GEN-011 | FAIL：删除台账的“已同步”和空正文的“逐字一致”属错误证据；CX-05重复同因噪声。历史log与当前运行已区分。 |
| GEN-012 | 目录扫描有限、maxBuffer32MB、源回归finally清理；无DB连接/线程。未为理论超大目录增新限制。 |
| GEN-013 | package调用方实际匹配；Node/YAML依赖可执行；delivery falsePASS由CX-01定位，不通过新增外部门槛解决。 |
| GEN-014 | 没有SQL迁移执行/修改；生成器只统计迁移文件名，不是迁移引擎。 |
| GEN-015 | 开发CLI无产品GUI；错误输出可见，不把技术命令标识符当业务端英文文案问题。 |
| GEN-016 | FAIL：CX-02/04等声称与实际不一致；生成器正确报输入漂移，历史log不冒充当前事实。 |
| GEN-017 | 17文件及所有发现按序分类均技术性；PGM036物理设备不由这些文档工具实现；无添加/冲突。 |
| GEN-018 | 每结论落到源码完整行、现行HTML原文、实际命令输出。无用上一模块PASS代替本次。 |
| PGM-036 | 已读实际PGM-036原文：物理工作手机/三道设备绑定等，本模块不承载该业务，不适用执行义务；无冲突/发明。 |

## 6. 本次17文件完整逐行覆盖

### tools/html-param-tier-baseline.json
bytes=596 SHA256=9332010bd445a64ff8f1fb5e3b6d7bbe387c7b76543b68424f0209dd7ddbfdc8
~~~~text
1: {
2:   "_说明": [
3:     "存量待补层级标注的句子清单（门禁 tools/verify-html-param-tier.mjs 用）。",
4:     "草案批准一条删一条，只许缩不许增：新写的带数值业务句必须当场标层级，不得往这里补登记。",
5:     "只缩不增由门禁机检（与基线 commit 里的同名文件比 key 集合），手工加 key 会被拦下。",
6:     "稳定键 key = sha1(文件路径 + 换行 + 归一化句子).slice(0,16)，不含行号（DEC-R8-17 已废行号锚点）。",
7:     "命中基线内 → 警告放行；基线外 → 阻断。"
8:   ],
9:   "entries": []
10: }
~~~~

### tools/refresh-entry-doc-baseline.mjs
bytes=7209 SHA256=76744e9a6285e6683beec1e2f7ec30105cb53cf878b6a5ec7d29ae6932647d8f
~~~~text
1: // 00-entry + README 事实基线快照自动回填（审计 P2-4 建议"计数由工具自动回填"的落地，根治手工刷反复过期）。
2: // 权威来源：openapi-p0.json（operation 数）/ db/migration（最新迁移+迁移数）/ surefire-reports（测试数）。
3: // 用法：node tools/refresh-entry-doc-baseline.mjs        —— 回填快照（写文件）
4: //       node tools/refresh-entry-doc-baseline.mjs --check —— 只校验漂移，漂移则退出码 1（可挂进可选链，
5: //       不入 delivery-gates：文档快照漂移闸属"自指证据闸"，按 2026-07-02 B2 裁决留在可选链）。
6: // 只改带"本轮/当前事实基线/取证"快照语义的行与 README baseline:auto 标记块；
7: // 历史变更记录（§changelog 旧快照数字）不动。无 surefire 报告时保留原测试数（不臆造），其余数字照常回填。
8: import fs from "node:fs";
9: import path from "node:path";
10: 
11: const root = process.cwd();
12: const entryPath = path.join(root, "docs", "00-current-effective-document-entry-v0.1.md");
13: const readmePath = path.join(root, "README.md");
14: const openapiPath = path.join(root, "docs", "openapi", "openapi-p0.json");
15: const migrationDir = path.join(root, "backend", "src", "main", "resources", "db", "migration");
16: const surefireDir = path.join(root, "backend", "target", "surefire-reports");
17: const checkOnly = process.argv.includes("--check");
18: 
19: // ---- 权威值 ----
20: const methods = ["get", "post", "put", "patch", "delete", "head", "options"];
21: const openapi = JSON.parse(fs.readFileSync(openapiPath, "utf8"));
22: let operationCount = 0;
23: for (const item of Object.values(openapi.paths ?? {})) {
24:   for (const method of methods) {
25:     if (item?.[method]) operationCount += 1;
26:   }
27: }
28: 
29: const migrations = fs.readdirSync(migrationDir).filter((name) => /^V\d+__.*\.sql$/.test(name));
30: const migrationCount = migrations.length;
31: const latestMigration = migrations
32:   .map((name) => ({ name, version: Number(/^V(\d+)__/.exec(name)[1]) }))
33:   .sort((a, b) => a.version - b.version)
34:   .at(-1);
35: const latestMigrationTag = `V${latestMigration.version}`;
36: 
37: // surefire：汇总 xml testsuite tests 属性；无报告目录则保留原快照数字（不臆造）。
38: let testCount = null;
39: if (fs.existsSync(surefireDir)) {
40:   testCount = 0;
41:   for (const file of fs.readdirSync(surefireDir)) {
42:     if (!file.endsWith(".xml")) continue;
43:     const head = fs.readFileSync(path.join(surefireDir, file), "utf8").slice(0, 500);
44:     const match = /<testsuite[^>]*\btests="(\d+)"/.exec(head);
45:     if (match) testCount += Number(match[1]);
46:   }
47:   if (testCount === 0) testCount = null;
48: }
49: 
50: const today = new Date().toISOString().slice(0, 10);
51: 
52: // ---- 00-entry 快照行替换（正则均限定"快照语义"上下文，不碰历史 changelog 行）----
53: // tests 数无新报告时用捕获组保留原数字，ops/迁移照常回填（修 AUD-008：此前无报告会整行跳过导致 ops 也不刷）。
54: let entryText = fs.readFileSync(entryPath, "utf8");
55: const entryBefore = entryText;
56: entryText = entryText.replace(/本轮（\d{4}-\d{2}-\d{2}）核验为 \d+ operations/g,
57:     `本轮（${today}）核验为 ${operationCount} operations`);
58: entryText = entryText.replace(
59:     /当前事实基线（后端 surefire 现有报告 (\d+) tests \/ OpenAPI \d+ operations \/ Flyway 最新 V\d+/g,
60:     (m, oldTests) => `当前事实基线（后端 surefire 现有报告 ${testCount ?? oldTests} tests / OpenAPI ${operationCount} operations / Flyway 最新 ${latestMigrationTag}`);
61: // §2.1 句尾"YYYY-MM-DD 刷新"日期随脚本刷新（2026-07-18 收尾：此前写死 2026-07-02）。
62: entryText = entryText.replace(
63:     /(以 `npm run verify:\*` 输出为准，)\d{4}-\d{2}-\d{2}( 刷新）)/g,
64:     `$1${today}$2`);
65: entryText = entryText.replace(
66:     /本轮 \d{4}-\d{2}-\d{2} 取证 Flyway 最新 V\d+、OpenAPI \d+ operations、后端 surefire 现有报告 (\d+) tests/g,
67:     (m, oldTests) => `本轮 ${today} 取证 Flyway 最新 ${latestMigrationTag}、OpenAPI ${operationCount} operations、后端 surefire 现有报告 ${testCount ?? oldTests} tests`);
68: entryText = entryText.replace(/查看本轮核验 \d+ 个 operation/g,
69:     `查看本轮核验 ${operationCount} 个 operation`);
70: entryText = entryText.replace(/本轮核验 \d+ 个 OpenAPI operation/g,
71:     `本轮核验 ${operationCount} 个 OpenAPI operation`);
72: entryText = entryText.replace(/示例、\d+ 个 operation 矩阵/g,
73:     `示例、${operationCount} 个 operation 矩阵`);
74: 
75: // ---- README baseline:auto 标记块整块重生成 ----
76: let readmeText = fs.readFileSync(readmePath, "utf8");
77: const readmeBefore = readmeText;
78: const blockRe = /(<!-- baseline:auto-begin[^>]*-->)([\s\S]*?)(<!-- baseline:auto-end -->)/;
79: const blockMatch = blockRe.exec(readmeText);
80: if (blockMatch) {
81:   const oldTests = /\*\*(\d+)\*\* tests/.exec(blockMatch[2]);
82:   const effectiveTests = testCount ?? (oldTests ? Number(oldTests[1]) : null);
83:   const testsCell = effectiveTests === null
84:       ? "以 `mvn test` 后 surefire 报告为准（无现存快照）"
85:       : `**${effectiveTests}** tests（现有 surefire 报告）`;
86:   const block = [
87:     "",
88:     "| 指标 | 当前值 | 取证方式 |",
89:     "|---|---|---|",
90:     `| Flyway 最新迁移 | **${latestMigrationTag}**（${migrationCount} 个迁移） | \`backend/src/main/resources/db/migration\` 实时统计 |`,
91:     `| OpenAPI operation 数 | **${operationCount}** | \`docs/openapi/openapi-p0.json\` 方法统计（与 \`npm run verify:operation-matrix\` 同口径） |`,
92:     `| 后端测试 | ${testsCell} | \`backend/target/surefire-reports\` 合计；无报告时保留上次快照，不声明刚完成全量测试 |`,
93:     "| 前端 E2E | 以 `npx playwright test --list` 实时输出为准 | 不写死快照数 |",
94:     "",
95:   ].join("\n");
96:   readmeText = readmeText.replace(blockRe, `$1${block}$3`);
97: }
98: 
99: const summary = `${operationCount} ops / ${latestMigrationTag}（${migrationCount} 迁移）${testCount ? ` / ${testCount} tests` : ""}`;
100: // --check 只比数字不比日期：上面 A/C/D 三条替换会把 today 无条件戳进快照句，
101: // 否则回填次日起必红（日历闹钟，不是漂移闸）。回填模式 norm 为恒等，日期照常刷新。
102: const norm = (s) => (checkOnly ? s.replace(/[0-9]{4}-[0-9]{2}-[0-9]{2}/g, "") : s);
103: const drifted = [];
104: if (norm(entryText) !== norm(entryBefore)) drifted.push("docs/00-current-effective-document-entry-v0.1.md");
105: if (norm(readmeText) !== norm(readmeBefore)) drifted.push("README.md");
106: 
107: if (checkOnly) {
108:   if (drifted.length > 0) {
109:     console.error(`[entry-doc-baseline] DRIFT: ${drifted.join(" + ")} 快照落后于实际（应为 ${summary}）。运行 node tools/refresh-entry-doc-baseline.mjs 回填。`);
110:     process.exit(1);
111:   }
112:   console.log(`[entry-doc-baseline] PASS: 快照与实际一致（${summary}）。`);
113:   process.exit(0);
114: }
115: 
116: if (drifted.length === 0) {
117:   console.log(`[entry-doc-baseline] 无漂移，未改动（${summary}）。`);
118: } else {
119:   if (entryText !== entryBefore) fs.writeFileSync(entryPath, entryText);
120:   if (readmeText !== readmeBefore) fs.writeFileSync(readmePath, readmeText);
121:   console.log(`[entry-doc-baseline] 已回填 ${drifted.join(" + ")}：${summary}（${today}）。`);
122: }
~~~~

### tools/verify-delivery-gates.mjs
bytes=15949 SHA256=d75d32a0b5586e35e66e758ae95ff44e72dbb030eef2e8de1188b4878b399cac
~~~~text
1: // 文件版本：V0.02
2: import fs from "node:fs";
3: import path from "node:path";
4: import { parse } from "yaml";
5: 
6: const repoRoot = process.cwd();
7: const failures = [];
8: 
9: function readText(relativePath) {
10:   const absolutePath = path.join(repoRoot, relativePath);
11:   if (!fs.existsSync(absolutePath)) {
12:     failures.push(`${relativePath} is missing`);
13:     return "";
14:   }
15:   return fs.readFileSync(absolutePath, "utf8");
16: }
17: 
18: function readJson(relativePath) {
19:   const text = readText(relativePath);
20:   if (!text) return null;
21:   try {
22:     return JSON.parse(text);
23:   } catch (error) {
24:     failures.push(`${relativePath} is not valid JSON: ${error.message}`);
25:     return null;
26:   }
27: }
28: 
29: function requireIncludes(relativePath, markers) {
30:   const text = readText(relativePath);
31:   for (const marker of markers) {
32:     if (!text.includes(marker)) failures.push(`${relativePath} must include marker: ${marker}`);
33:   }
34:   return text;
35: }
36: 
37: const packageJson = readJson("package.json");
38: if (packageJson) {
39:   const scripts = packageJson.scripts ?? {};
40:   const docsScript = scripts["verify:delivery-gates-docs"];
41:   const deliveryScript = scripts["verify:delivery-gates"];
42:   if (docsScript !== "node tools/verify-delivery-gates.mjs") {
43:     failures.push("package.json must expose verify:delivery-gates-docs");
44:   }
45:   if (scripts["docs:production-closure-gap-reports"] !== "node tools/generate-production-closure-gap-report.mjs") {
46:     failures.push("package.json must expose docs:production-closure-gap-reports");
47:   }
48:   if (scripts["docs:ui-experience-acceptance"] !== "node tools/generate-ui-experience-acceptance-report.mjs") {
49:     failures.push("package.json must expose docs:ui-experience-acceptance");
50:   }
51:   if (scripts["verify:ui-experience-acceptance"] !== "node tools/generate-ui-experience-acceptance-report.mjs --check") {
52:     failures.push("package.json must expose verify:ui-experience-acceptance");
53:   }
54:   if (scripts["docs:production-evidence-owner-execution-packets"] !== "node tools/generate-production-evidence-owner-execution-packets.mjs") {
55:     failures.push("package.json must expose docs:production-evidence-owner-execution-packets");
56:   }
57:   if (scripts["verify:production-evidence-owner-execution-packets"] !== "node tools/generate-production-evidence-owner-execution-packets.mjs --check") {
58:     failures.push("package.json must expose verify:production-evidence-owner-execution-packets");
59:   }
60:   if (scripts["docs:production-no-go-command-center"] !== "node tools/generate-production-no-go-resolution-command-center.mjs") {
61:     failures.push("package.json must expose docs:production-no-go-command-center");
62:   }
63:   if (scripts["verify:production-no-go-command-center"] !== "node tools/generate-production-no-go-resolution-command-center.mjs --check") {
64:     failures.push("package.json must expose verify:production-no-go-command-center");
65:   }
66:   if (scripts["verify:production-evidence-crosslinks"] !== "node tools/verify-production-evidence-crosslinks.mjs") {
67:     failures.push("package.json must expose verify:production-evidence-crosslinks");
68:   }
69:   if (scripts["docs:partial-requirement-mock-coverage"] !== "node tools/generate-partial-requirement-mock-coverage.mjs") {
70:     failures.push("package.json must expose docs:partial-requirement-mock-coverage");
71:   }
72:   if (scripts["verify:partial-requirement-mock-coverage"] !== "node tools/generate-partial-requirement-mock-coverage.mjs --check") {
73:     failures.push("package.json must expose verify:partial-requirement-mock-coverage");
74:   }
75:   if (scripts["prepare:production-evidence-drafts"] !== "node tools/prepare-production-evidence-manifest-drafts.mjs") {
76:     failures.push("package.json must expose prepare:production-evidence-drafts");
77:   }
78:   if (scripts["verify:production-evidence-draft-prep"] !== "node tools/prepare-production-evidence-manifest-drafts.mjs --check") {
79:     failures.push("package.json must expose verify:production-evidence-draft-prep");
80:   }
81:   if (scripts["verify:production-evidence-draft-prep-fixtures"] !== "node tools/verify-production-evidence-draft-prep-fixtures.mjs") {
82:     failures.push("package.json must expose verify:production-evidence-draft-prep-fixtures");
83:   }
84:   if (scripts["verify:production-evidence-positive-fixtures"] !== "node tools/verify-production-evidence-positive-fixtures.mjs") {
85:     failures.push("package.json must expose verify:production-evidence-positive-fixtures");
86:   }
87:   if (scripts["verify:production-evidence-positive-propagation-fixtures"] !== "node tools/verify-production-evidence-positive-propagation-fixtures.mjs") {
88:     failures.push("package.json must expose verify:production-evidence-positive-propagation-fixtures");
89:   }
90:   if (scripts["verify:production-ready-no-go-fixtures"] !== "node tools/verify-production-ready-no-go-fixtures.mjs") {
91:     failures.push("package.json must expose verify:production-ready-no-go-fixtures");
92:   }
93:   if (scripts["verify:production-blocker-register-no-go-fixtures"] !== "node tools/verify-production-blocker-register-no-go-fixtures.mjs") {
94:     failures.push("package.json must expose verify:production-blocker-register-no-go-fixtures");
95:   }
96:   if (scripts["verify:production-ready-positive-fixtures"] !== "node tools/verify-production-ready-positive-fixtures.mjs") {
97:     failures.push("package.json must expose verify:production-ready-positive-fixtures");
98:   }
99:   const requiredDeliveryCommands = [
100:     "verify:delivery-gates-docs",
101:     "verify:full-admin-matrix",
102:     "verify:domain-ownership-scope",
103:     "verify:unit",
104:     "verify:coding-host-protection",
105:     "verify:external-dependency-gates",
106:     "verify:external-evidence-requirement-traceability",
107:     "verify:partial-requirement-mock-coverage",
108:     "verify:t3-coding-package",
109:     "verify:t3b-supplier-mock-kit",
110:     "verify:t3d-local-llm-ai-quality",
111:     "verify:p0-contract",
112:     "verify:security-baseline",
113:     "verify:production-readiness-evidence",
114:     "verify:production-blockers",
115:     "verify:production-owner-actions",
116:     "verify:production-evidence-submission-kit",
117:     "verify:production-evidence-manifest-templates",
118:     "verify:production-evidence-owner-guides",
119:     "verify:production-evidence-submission-status",
120:     "verify:production-evidence-submissions-guard",
121:     "verify:production-evidence-negative-fixtures",
122:     "verify:production-evidence-positive-fixtures",
123:   "verify:production-evidence-positive-propagation-fixtures",
124:   "verify:production-ready-no-go-fixtures",
125:   "verify:production-blocker-register-no-go-fixtures",
126:   "verify:production-ready-positive-fixtures",
127:   "verify:production-evidence-positive-propagation-fixtures",
128:     "verify:production-evidence-positive-propagation-fixtures",
129:     "verify:production-ready-no-go-fixtures",
130:     "verify:production-blocker-register-no-go-fixtures",
131:     "verify:production-ready-positive-fixtures",
132:     "verify:production-evidence-backfill-priority",
133:     "verify:production-evidence-backfill-execution-ledger",
134:     "verify:production-evidence-p0-quickstart",
135:     "verify:production-evidence-owner-execution-packets",
136:     "verify:production-no-go-command-center",
137:   "verify:production-evidence-crosslinks",
138:     "verify:production-evidence-draft-prep",
139:     "verify:production-evidence-draft-prep-fixtures",
140:     "verify:production-closure-gap-reports",
141:     "verify:nas-it-handoff",
142:     "verify:nas-diagnostics",
143:     "verify:nas-current-state-drift",
144:     "verify:final-closed-loop-audit",
145:     "verify:closed-loop-current-state",
146:     "verify:runtime-acceptance-evidence",
147:     "verify:ui-experience-acceptance",
148:   "verify:production-no-go-command-center",
149:   "verify:production-evidence-intake",
150:   ];
151:   if (!deliveryScript) {
152:     failures.push("package.json must expose verify:delivery-gates");
153:   } else {
154:     for (const command of requiredDeliveryCommands) {
155:       if (!deliveryScript.includes(command)) failures.push(`verify:delivery-gates must include ${command}`);
156:     }
157:     for (const forbidden of ["test:backend", "android:p0", "dev:backend", "dev:web"]) {
158:       if (deliveryScript.includes(forbidden)) failures.push(`verify:delivery-gates must stay lightweight and not include ${forbidden}`);
159:     }
160:   }
161: }
162: 
163: requireIncludes("docs/delivery-readiness-gates-v0.1.md", [
164:   "DELIVERY_READINESS_GATES_READY",
165:   "VERIFY_DELIVERY_GATES_READY",
166:   "NO_DELIVERY_WITHOUT_EXTERNAL_DEPENDENCY_GATES",
167:   "NO_FORMAL_CODE_OUTSIDE_160",
168:   "NO_PRODUCTION_DEPLOYMENT_WITH_PENDING_EXTERNAL_BLOCKERS",
169:   "OWNER_ACTION_PACKS_FROM_PRODUCTION_BLOCKERS",
170:   "OWNER_ACTIONS_LINKED_TO_EVIDENCE_MANIFESTS",
171:   "PRODUCTION_EVIDENCE_SUBMISSION_KIT_READY",
172:   "PRODUCTION_EVIDENCE_MANIFEST_TEMPLATES_READY",
173:   "PRODUCTION_EVIDENCE_OWNER_GUIDES_READY",
174:   "PRODUCTION_EVIDENCE_SUBMISSION_STATUS_READY",
175:   "PRODUCTION_EVIDENCE_SUBMISSIONS_GUARD_READY",
176:   "PRODUCTION_EVIDENCE_NEGATIVE_FIXTURES_READY",
177:   "PRODUCTION_EVIDENCE_POSITIVE_FIXTURES_READY",
178:   "PRODUCTION_EVIDENCE_POSITIVE_PROPAGATION_FIXTURES_READY",
179:   "PRODUCTION_READY_NO_GO_FIXTURES_READY",
180:   "PRODUCTION_BLOCKER_REGISTER_NO_GO_FIXTURES_READY",
181:   "PRODUCTION_READY_POSITIVE_FIXTURES_READY",
182:   "PRODUCTION_EVIDENCE_BACKFILL_PRIORITY_READY",
183:   "PRODUCTION_EVIDENCE_BACKFILL_EXECUTION_LEDGER_READY",
184:   "PRODUCTION_EVIDENCE_P0_QUICKSTART_READY",
185:   "PRODUCTION_EVIDENCE_OWNER_EXECUTION_PACKETS_READY",
186:   "PRODUCTION_NO_GO_RESOLUTION_COMMAND_CENTER_READY",
187:   "PRODUCTION_EVIDENCE_CROSSLINKS_READY",
188:   "OWNER_PACKET_ONE_STOP_BACKFILL_READY",
189:   "PRODUCTION_EVIDENCE_DRAFT_PREP_READY",
190:   "PRODUCTION_EVIDENCE_DRAFT_PREP_FIXTURES_READY",
191:   "PRODUCTION_CLOSURE_GAP_REPORTS_READY",
192:   "EXTERNAL_EVIDENCE_REQUIREMENT_TRACEABILITY_READY",
193:   "PARTIAL_REQUIREMENTS_HAVE_OWNER_MANIFESTS",
194:   "PARTIAL_REQUIREMENT_MOCK_COVERAGE_READY",
195:   "PARTIAL_REQUIREMENTS_HAVE_MOCK_ACCEPTANCE_PATHS",
196:   "NO_FAKE_PRODUCTION_MANIFEST_CLOSURE",
197:   "NAS_DIAGNOSTIC_REPORT_READY",
198:   "VERIFY_NAS_DIAGNOSTICS_READY",
199:   "NAS_CURRENT_STATE_DRIFT_REPORT_READY",
200:   "VERIFY_NAS_CURRENT_STATE_DRIFT_READY",
201:   "HISTORICAL_NAS_EVIDENCE_NOT_CURRENT_CLOSURE",
202:   "VERIFY_RUNTIME_ACCEPTANCE_EVIDENCE_READY",
203:   "RUNTIME_ACCEPTANCE_EVIDENCE_READY",
204:   "UI_EXPERIENCE_ACCEPTANCE_REPORT_READY",
205:   "VERIFY_UI_EXPERIENCE_ACCEPTANCE_READY",
206:   "docs:production-closure-gap-reports",
207:   "verify:production-closure-gap-reports",
208:   "verify:external-evidence-requirement-traceability",
209:   "verify:partial-requirement-mock-coverage",
210:   "npm run verify:delivery-gates",
211:   "verify:external-dependency-gates",
212:     "verify:external-evidence-requirement-traceability",
213:   "verify:coding-host-protection",
214:   "verify:production-readiness-evidence",
215:   "verify:production-blockers",
216:   "verify:production-owner-actions",
217:   "verify:production-evidence-submission-kit",
218:   "verify:production-evidence-manifest-templates",
219:   "verify:production-evidence-owner-guides",
220:   "verify:production-evidence-submission-status",
221:   "verify:production-evidence-backfill-priority",
222:   "verify:production-evidence-backfill-execution-ledger",
223:   "verify:production-evidence-intake",
224:   "verify:production-evidence-positive-fixtures",
225:   "prepare:production-evidence-drafts",
226:   "verify:production-evidence-draft-prep",
227:   "verify:production-evidence-draft-prep-fixtures",
228:   "verify:nas-current-state-drift",
229: ]);
230: 
231: requireIncludes("docs/external-dependency-gates-v0.1.md", [
232:   "EXTERNAL_DEPENDENCY_GATES_READY",
233:   "NO_REAL_VENDOR_CONTROL_WITHOUT_SUPPLIER_READY",
234:   "NO_INDEPENDENT_PYTHON_AI_SERVICE_WITHOUT_GATE",
235:   "PRODUCTION_EXTERNAL_BLOCKERS_TRACKED",
236: ]);
237: 
238: requireIncludes("docs/00-current-effective-document-entry-v0.1.md", [
239:   "docs/delivery-readiness-gates-v0.1.md",
240:   "docs/external-dependency-gates-v0.1.md",
241:   "npm run verify:delivery-gates",
242:   "docs/nas-diagnostic-report-v0.1.md",
243:   "docs/nas-current-state-drift-report-v0.1.md",
244:   "npm run verify:nas-current-state-drift",
245:   "docs/production-evidence-p0-quickstart-v0.1.md",
246:   "docs/production-evidence-owner-execution-packets-v0.1.md",
247:   "npm run verify:production-evidence-owner-execution-packets",
248:   "npm run verify:production-evidence-crosslinks",
249:   "npm run prepare:production-evidence-drafts",
250:   "npm run verify:production-evidence-draft-prep",
251:   "npm run verify:production-evidence-draft-prep-fixtures",
252:   "npm run verify:runtime-acceptance-evidence",
253:   "npm run verify:ui-experience-acceptance",
254:   "docs/runtime-acceptance-evidence-latest.json",
255:   "docs/runtime-acceptance-evidence-v0.1.md",
256:   "docs/closed-loop-implementation-ui-acceptance-report-v0.1.md",
257:   "docs/partial-requirement-mock-coverage-v0.1.md",
258:   "npm run verify:partial-requirement-mock-coverage",
259: ]);
260: 
261: requireIncludes("docs/delivery-package-manifest-v0.1.md", [
262:   "delivery-readiness-gates-v0.1.md",
263:   "external-dependency-gates-v0.1.md",
264:   "npm run verify:delivery-gates",
265:   "docs/nas-diagnostic-report-v0.1.md",
266:   "docs/nas-current-state-drift-report-v0.1.md",
267:   "npm run verify:nas-current-state-drift",
268:   "docs/production-evidence-p0-quickstart-v0.1.md",
269:   "docs/production-evidence-owner-execution-packets-v0.1.md",
270:   "npm run verify:production-evidence-owner-execution-packets",
271:   "npm run verify:production-evidence-crosslinks",
272:   "npm run prepare:production-evidence-drafts",
273:   "npm run verify:production-evidence-draft-prep",
274:   "npm run verify:production-evidence-draft-prep-fixtures",
275:   "npm run verify:runtime-acceptance-evidence",
276:   "npm run verify:ui-experience-acceptance",
277:   "docs/runtime-acceptance-evidence-latest.json",
278:   "docs/runtime-acceptance-evidence-v0.1.md",
279:   "docs/closed-loop-implementation-ui-acceptance-report-v0.1.md",
280: ]);
281: 
282: // CI 定义唯一：模板与生效 workflow 必须字节一致，且 checkout 带完整历史（fetch-depth: 0）——
283: // 浅克隆会让 verify:html-ledger-sync / verify:html-param-tier 取不到基线（决策台账 2026-08-06 补注；M001 审计 2026-09-03）。
284: const lf = (text) => text.replace(/\r\n/g, "\n");
285: const liveWorkflow = lf(readText(".github/workflows/predeploy-gates.yml"));
286: const templateWorkflow = lf(readText("ops/ci/predeploy-gates.yml"));
287: if (liveWorkflow && templateWorkflow && liveWorkflow !== templateWorkflow) {
288:   failures.push("ops/ci/predeploy-gates.yml must be byte-identical to .github/workflows/predeploy-gates.yml");
289: }
290: const htmlGates = ["verify:html-ledger-sync", "verify:entry-anchors", "verify:html-param-tier"];
291: const eventBase = "${{ github.event_name == 'pull_request' && github.event.pull_request.base.sha || github.event.before }}";
292: for (const workflow of [".github/workflows/predeploy-gates.yml", "ops/ci/predeploy-gates.yml"]) {
293:   try {
294:     const steps = parse(readText(workflow))?.jobs?.predeploy?.steps;
295:     if (!Array.isArray(steps)) throw new Error("predeploy.steps must be an array");
296:     const checkout = steps.find(step => step?.uses?.startsWith("actions/checkout@"));
297:     if (String(checkout?.with?.["fetch-depth"]) !== "0") failures.push(`${workflow} must checkout with fetch-depth: 0`);
298:     const verify = steps.find(step => step?.run?.trim() === "npm run verify:predeploy");
299:     if (!verify) failures.push(`${workflow} must run npm run verify:predeploy`);
300:     if (verify?.env?.HTML_GATES_BASE !== eventBase) failures.push(`${workflow} must supply the PR/push event base as HTML_GATES_BASE`);
301:   } catch (error) {
302:     failures.push(`${workflow} has invalid predeploy configuration: ${error.message}`);
303:   }
304: }
305: for (const script of ["ops/160/complex-deploy-api.sh", "ops/160/complex-deploy-web.sh"]) {
306:   const array = /^REQUIRED_GATES=\(([\s\S]*?)^\)/m.exec(readText(script))?.[1] ?? "";
307:   const gates = new Set(array.replace(/#.*$/gm, "").match(/\bverify:[a-z0-9:-]+\b/g) ?? []);
308:   for (const gate of htmlGates) {
309:     if (!gates.has(gate)) failures.push(`${script} REQUIRED_GATES must include ${gate}`);
310:   }
311: }
312: if (packageJson) {
313:   const predeploy = packageJson.scripts?.["verify:predeploy"] ?? "";
314:   for (const gate of htmlGates) {
315:     if (!predeploy.includes(gate)) failures.push(`verify:predeploy must include ${gate}`);
316:   }
317: }
318: 
319: if (failures.length > 0) {
320:   console.error("DELIVERY_GATES_DOCS_FAIL");
321:   for (const failure of failures) console.error(`- ${failure}`);
322:   process.exit(1);
323: }
324: 
325: console.log("DELIVERY_GATES_DOCS_PASS");
~~~~

### tools/verify-entry-anchors.mjs
bytes=16185 SHA256=89a0d40c03b6d8c659575f432e84e0fc3df7cffdf6d1704916dddc9e9994ec2c
~~~~text
1: #!/usr/bin/env node
2: // 文件版本：V0.02
3: // 门禁：三份业务最终解释权 HTML 的引用一律用「条目名」，不得再用行号。
4: // 依据：DEC-R8-17（用户 2026-08-06 裁 A：彻底废行号、只留条目名）。
5: // 病史：R8 两批 6 轮返工里有 3 轮因行号锚点漂移返工（760 行漂移 → 287 处重建 → +2 位移 19 处漏改）。
6: // 本门禁不依赖 git diff 基线，任何时候跑都是全量实检，不会空转。
7: //
8: // 检三件事：
9: //   ① 注册表 docs/prd-audit/entry-registry.csv 内，条目名在同一份 HTML 内唯一；
10: //      且每条的**整条正文**（entry_body 列，与本文件 flatEntry() 同源、去全部空白）逐字仍在
11: //      对应 HTML 现文里（注册表没跟上 HTML 改动即红）。
12: //      R12 之前该列存的是首句前 24 字指纹，条目中段被改动检不出，
13: //      R8-U-02「HTML 内容无保护落点」的缺口即由此而来。换成整条正文后，注册表全部行
14: //      （entry ＋ section）都进入逐字保护面，不再依赖该条目有没有账本行。
15: //   ② 全库 Markdown（历史留痕文件除外）里的 文件代号「名」 引用，
16: //      必须能在该 HTML 内唯一定位——要么等于注册表条目名，要么是恰好一条条目正文的子串。
17: //   ③ 正文里不得再出现「实测 LNNN」式行号锚点；
18: //   ④ 账本与承接矩阵两份机读台账的 entry_name 必须同名同源（矩阵是从账本抄过来的，抄错即红）；
19: //   ⑤ 账本每行 source_text 必须仍逐字存在于对应 HTML 现文里——这是**账本侧**的逐字实检；
20: //      **条目侧**的整条正文保护由检查① 承担，两者互补，不再有「未登记进账本就没人管」的条目。
21: //   ⑥ 账本每行必须在承接矩阵 docs/prd-audit/html-prd-coverage.csv 里有对应行
22: //      （反向：④ 只检「矩阵→账本」，缺了「账本→矩阵」这一向，账本新增条目可以长期不进矩阵而门禁不响）；
23: //      矩阵里 9 条 *-MPS-000N 占位伪 ID 按白名单放行（member-points 重建 Phase4 待映射，账本无对应行是设计如此）。
24: //   ⑦ 账本另外九个句子类镜像列（trigger / preconditions / state_transition / value_flow / permission /
25: //      approval / separation_of_duties / failure_recovery / evidence_required）同样逐字实检：
26: //      检查⑤ 只覆盖 source_text 一列，这九列在此之前命中数一直是 0——HTML 改了、这九列不跟，门禁不会响。
27: //      trigger 列是抽取时合成的触发场景概括，定性豁免；哨兵值 NOT_* 豁免；短于 12 字的片段不检。
28: import fs from "node:fs";
29: import path from "node:path";
30: 
31: const HTML = {
32:   BSM: "docs/business-system-map-for-operations-v0.1.html",
33:   PGM: "docs/platform-governance-map-for-operations-v0.1.html",
34:   ENUM: "docs/business-enum-quick-reference-v0.1.html",
35: };
36: const REGISTRY = "docs/prd-audit/entry-registry.csv";
37: const LEDGER = "docs/prd-audit/authority-rule-ledger.csv";
38: const COVERAGE = "docs/prd-audit/html-prd-coverage.csv";
39: // 历史留痕文件：记录的是「当时」的事实，一律不改、不检
40: const EXCLUDE = /^docs\/audit-2026-09\/modules\/M\d{3}-b(?:1|4-r\d+|5-r\d+)-(?:self|grok)-raw\.md$|(changelog|change-log|-report|\.report\.|diff-matrix|anchor-rebuild|html-map|reanchor|html-mirror-scan|history-index|[\\/]rounds[\\/])/i;
41: 
42: const errors = [];
43: const warns = [];
44: 
45: function readCsv(file) {
46:   let t = fs.readFileSync(file, "utf8");
47:   if (t.charCodeAt(0) === 0xfeff) t = t.slice(1);
48:   const rows = [];
49:   let row = [], cell = "", q = false, closed = false;
50:   for (let i = 0; i < t.length; i++) {
51:     const c = t[i];
52:     if (q) {
53:       if (c === '"') { if (t[i + 1] === '"') { cell += '"'; i++; } else { q = false; closed = true; } }
54:       else cell += c;
55:     } else if (closed && ![",", "\r", "\n"].includes(c)) {
56:       throw new Error(`${file}: CSV 记录 ${rows.length + 1} 引号闭合后出现非分隔字符`);
57:     } else if (c === '"') {
58:       if (cell) throw new Error(`${file}: CSV 记录 ${rows.length + 1} 字段中出现未转义引号`);
59:       q = true;
60:     } else if (c === ",") { row.push(cell); cell = ""; closed = false; }
61:     else if (c === "\n") { row.push(cell); rows.push(row); row = []; cell = ""; closed = false; }
62:     else if (c !== "\r") cell += c;
63:   }
64:   if (q) throw new Error(`${file}: CSV 记录 ${rows.length + 1} 引号未闭合`);
65:   if (cell !== "" || row.length || closed) { row.push(cell); rows.push(row); }
66:   const head = rows.shift();
67:   if (!head || head.some(h => !h.trim()) || new Set(head).size !== head.length) {
68:     throw new Error(`${file}: CSV 表头为空或含重复列名`);
69:   }
70:   return rows.flatMap((r, i) => {
71:     if (r.length === 1 && !r[0].trim()) return [];
72:     if (r.length !== head.length) throw new Error(`${file}: CSV 记录 ${i + 2} 应有 ${head.length} 列，实际 ${r.length} 列`);
73:     return [Object.fromEntries(head.map((h, j) => [h, r[j]]))];
74:   });
75: }
76: 
77: function plainText(file) {
78:   return fs.readFileSync(file, "utf8")
79:     .replace(/<!--[\s\S]*?-->/g, " ")
80:     .replace(/<style[\s\S]*?<\/style>/gi, " ")
81:     .replace(/<script[\s\S]*?<\/script>/gi, " ")
82:     .replace(/<[^>]*>/g, " ")
83:     .replace(/&nbsp;/g, " ").replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&quot;/g, '"')
84:     .replace(/\s+/g, "");
85: }
86: 
87: // 引号写法在 PRD 与 HTML 之间历来混用（「」『』“”""），比对前一律剥掉，只比文字
88: function norm(s) {
89:   return s.replace(/[「」『』“”"'‘’]/g, "").replace(/\s+/g, "");
90: }
91: 
92: function walk(dir, out = []) {
93:   for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
94:     const p = path.join(dir, e.name);
95:     if (e.isDirectory()) { if (e.name === "node_modules" || e.name === "_archive") continue; walk(p, out); }
96:     else if (e.name.endsWith(".md")) out.push(p);
97:   }
98:   return out;
99: }
100: 
101: // ---- ① 注册表自身 ----
102: const rows = readCsv(REGISTRY);
103: const fileOf = Object.fromEntries(Object.entries(HTML).map(([k, v]) => [path.basename(v), k]));
104: const flat = Object.fromEntries(Object.entries(HTML).map(([k, v]) => [k, plainText(v)]));
105: const byCode = { BSM: [], PGM: [], ENUM: [] };
106: const nameSeen = {};
107: for (const r of rows) {
108:   const code = fileOf[r.html_file];
109:   if (!code) { errors.push(`注册表出现未知 HTML 文件：${r.html_file}（${r.entry_id}）`); continue; }
110:   const key = code + "|" + r.entry_name;
111:   if (nameSeen[key]) errors.push(`条目名在同一份 HTML 内重复：${code}「${r.entry_name}」（${nameSeen[key]} 与 ${r.entry_id}）`);
112:   nameSeen[key] = r.entry_id;
113:   const entryBody = (r.entry_body || "").replace(/\s+/g, "");
114:   if (entryBody && !flat[code].includes(entryBody)) {
115:     errors.push(`注册表条目正文在 HTML 现文中找不到：${r.entry_id} ${code}「${r.entry_name}」——HTML 已改而注册表未重建`);
116:   }
117:   byCode[code].push(r);
118: }
119: 
120: const entryTextCache = new Map();
121: function flatEntry(code, r) {
122:   const k = r.entry_id;
123:   if (!entryTextCache.has(k)) {
124:     const lines = fs.readFileSync(HTML[code], "utf8").split("\n");
125:     const start = Number(r.line_now) - 1;
126:     let end = start;
127:     const raw = lines[start] || "";
128:     if (/<tr[\s>]/.test(raw) && !/<\/tr>/.test(raw)) { while (end < lines.length && !/<\/tr>/.test(lines[end])) end++; }
129:     const seg = lines.slice(start, end + 1).join(" ")
130:       .replace(/<!--[\s\S]*?-->/g, " ").replace(/<[^>]*>/g, " ")
131:       .replace(/&nbsp;/g, " ").replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">").replace(/&quot;/g, '"')
132:       .replace(/\s+/g, "");
133:     entryTextCache.set(k, seg);
134:   }
135:   return entryTextCache.get(k);
136: }
137: 
138: // ---- ④ 账本 / 承接矩阵的条目名同源 ----
139: const ledRows = readCsv(LEDGER);
140: const ledName = {};
141: for (const r of ledRows) {
142:   ledName[r.rule_id] = r.entry_name;
143:   if (r.entry_name === "ORPHANED") continue;          // R8 保留的口径变更史，现行 HTML 已无原文
144:   if (!nameSeen[fileOf[r.html_file] + "|" + r.entry_name]) {
145:     errors.push(`账本条目名不在注册表里：${r.rule_id} ${r.html_file}「${r.entry_name}」`);
146:     continue;
147:   }
148:   // 逐行比 source_text 与 HTML 现文，挡住「HTML 改了台账没跟上」
149:   const st = norm(r.source_text);
150:   if (st && !norm(flat[fileOf[r.html_file]]).includes(st)) {
151:     errors.push(`账本正文与 HTML 现文对不上：${r.rule_id}「${r.entry_name}」——HTML 已改而账本 source_text 未重取`);
152:   }
153: }
154: const covRows = readCsv(COVERAGE);
155: let covMapped = 0, covNotInLedger = 0;
156: for (const r of covRows) {
157:   if (r.entry_name === "NOT_IN_LEDGER") {
158:     if (ledName[r.rule_id] !== undefined) errors.push(`承接矩阵 ${r.rule_id} 标 NOT_IN_LEDGER，但账本里有这条`);
159:     covNotInLedger++; continue;
160:   }
161:   if (ledName[r.rule_id] === undefined) { errors.push(`承接矩阵 ${r.rule_id} 在账本中不存在，却写了条目名「${r.entry_name}」`); continue; }
162:   if (ledName[r.rule_id] !== r.entry_name) {
163:     errors.push(`承接矩阵与账本条目名不一致：${r.rule_id} 矩阵「${r.entry_name}」≠ 账本「${ledName[r.rule_id]}」`);
164:     continue;
165:   }
166:   covMapped++;
167: }
168: 
169: // ---- ⑥ 账本每行必须在承接矩阵里有对应行（R10 新增）----
170: // 矩阵里的占位伪 ID：member-points 重建 Phase4 待映射，账本无对应行，按设计保留。
171: const FAKE_COVERAGE_IDS = new Set([
172:   "BSM-MPS-0001", "BSM-MPS-0002", "BSM-MPS-0003",
173:   "PGM-MPS-0001", "PGM-MPS-0002", "PGM-MPS-0003",
174:   "BEQ-MPS-0001", "BEQ-MPS-0002", "BEQ-MPS-0003",
175: ]);
176: const covIdSet = new Set(covRows.map(r => r.rule_id));
177: for (const r of ledRows) {
178:   if (!covIdSet.has(r.rule_id)) {
179:     errors.push(`账本行未登记进承接矩阵：${r.rule_id}「${r.entry_name}」——账本与承接矩阵须一一对应（检查⑥）`);
180:   }
181: }
182: for (const r of covRows) {
183:   if (ledName[r.rule_id] === undefined && !FAKE_COVERAGE_IDS.has(r.rule_id)) {
184:     errors.push(`承接矩阵出现账本没有、又不在伪 ID 白名单里的 rule_id：${r.rule_id}（检查⑥）`);
185:   }
186: }
187: 
188: // ---- ⑦ 账本九个句子类镜像列逐字实检（R12 新增）----
189: // 检查⑤ 只管 source_text 一列。另外九列（trigger / preconditions / state_transition / value_flow /
190: // permission / approval / separation_of_duties / failure_recovery / evidence_required）从来没被机检过，
191: // 命中数一直是 0——HTML 改了、这九列不跟，门禁不会响。本检把它们纳入保护范围。
192: //
193: // 【片段分隔符口径：宽口径 /[｜；;\s]+/】
194: // 这九列的值是「从条目正文里挑若干句拼起来」，抽取时用过三种拼接符——全角竖线 ｜、分号 ；/;，
195: // 以及**一个普通空格**（被挑的句子在原文里不相邻时用它）。
196: // 例：BSM-0001 preconditions = source_text 的第 1 句 + " " + 第 3 句，中间第 2 句跳过。
197: // norm() 会把空格抹掉，于是两段不相邻的原文被焊成一条现文里根本不存在的串，纯属误报。
198: // 实测对比（本轮基线 a4f31f56，阈值 12）：
199: //   只切 ｜        ：checked=1663，非 trigger 失配 26
200: //   切 ｜；;        ：checked=1901，非 trigger 失配 17
201: //   切 ｜；; + 空白 ：checked=1965，非 trigger 失配 12   ← 取此口径
202: // 宽口径检的片段更多、误报更少，两头都占优，故取宽口径。
203: //
204: // 【trigger 列整列豁免】该列存的是抽取时**合成**的触发场景概括，设计上就不逐字复制原文
205: // （实测 301 处失配里 289 处落在这一列，占 96%）。对一个合成列做逐字检，等于要求它不再是概括，
206: // 属口径冲突而非数据错误，故定性豁免；其余八列逐字检。
207: //
208: // 【阈值 12 字】低于 12 字的片段多是「不用审批」「自动留痕」这类标签式短语，全库大量重复出现，
209: // 逐字命中与否不构成「账本跟丢了 HTML」的证据，检了只增噪声。
210: //
211: // 【尾部标点容差】单元格习惯以 。 收尾，而原文同一处常是 ；或 、。终止标点不承载规则语义，
212: // 比对前一律剥掉，免掉一整类纯标点误报。
213: const MIRROR_COLS = ["trigger", "preconditions", "state_transition", "value_flow",
214:   "permission", "approval", "separation_of_duties", "failure_recovery", "evidence_required"];
215: const SENTINEL = /^NOT_[A-Z0-9_-]*$/;   // NOT_STATED / NOT_EXTRACTED_R8 / _R8-H2 / _R9：原文本就没说，不是失配
216: const MIRROR_MIN = 12;
217: const flatNorm = Object.fromEntries(Object.entries(flat).map(([k, v]) => [k, norm(v)]));
218: let mirrorChecked = 0;
219: for (const r of ledRows) {
220:   if (r.entry_name === "ORPHANED") continue;   // 同检查⑤：现行 HTML 已无原文
221:   const code = fileOf[r.html_file];
222:   if (!code) continue;                          // 未知 HTML 文件已由检查④ 报错
223:   for (const col of MIRROR_COLS) {
224:     if (col === "trigger") continue;            // 见上：合成列，定性豁免
225:     const v = (r[col] || "").trim();
226:     if (!v || SENTINEL.test(v)) continue;
227:     for (const frag of v.split(/[｜；;\s]+/)) {
228:       const n = norm(frag).replace(/[。；;，、．.]+$/, "");
229:       if (n.length < MIRROR_MIN) continue;
230:       mirrorChecked++;
231:       if (!flatNorm[code].includes(n)) {
232:         errors.push(`账本镜像列与 HTML 现文对不上：${r.rule_id}「${r.entry_name}」${col} 片段「${frag.trim()}」——HTML 已改而账本未重取（检查⑦）`);
233:       }
234:     }
235:   }
236: }
237: 
238: // ---- ②③ 全库引用 ----
239: const mdFiles = walk("docs").filter(f => !EXCLUDE.test(f.replace(/\\/g, "/")));
240: const REF = /(BSM|PGM|ENUM|BEQ)\s*「([^」]{2,})」/g;
241: const LINE_ANCHOR = /实测\s*\*{0,2}L\d+/g;   // 加粗写法（实测 **L123**）同样算行号锚点
242: let refTotal = 0, refByName = 0, refByText = 0;
243: for (const f of mdFiles) {
244:   const txt = fs.readFileSync(f, "utf8");
245:   const rel = f.replace(/\\/g, "/");
246:   const la = txt.match(LINE_ANCHOR);
247:   if (la) errors.push(`${rel}：仍有行号锚点 ${[...new Set(la)].slice(0, 5).join("、")}（共 ${la.length} 处）`);
248:   let m;
249:   REF.lastIndex = 0;
250:   while ((m = REF.exec(txt))) {
251:     const code = m[1] === "BEQ" ? "ENUM" : m[1];
252:     const name = m[2];
253:     refTotal++;
254:     if (nameSeen[code + "|" + name]) { refByName++; continue; }
255:     // 省略号引用（头……尾）算作对同一条的引用：各段必须同时出现在该条正文里
256:     const parts = name.split(/……|…|\.\.\./).map(norm).filter(p => p.length >= 2);
257:     const hitAll = (t) => parts.every(p => t.includes(p));
258:     const hits = byCode[code].filter(r => hitAll(norm(r.entry_name)));
259:     const textHits = hits.length ? hits : byCode[code].filter(r => hitAll(norm(flatEntry(code, r))));
260:     if (textHits.length === 1) { refByText++; continue; }
261:     const at = txt.slice(0, m.index).split("\n").length;
262:     if (textHits.length === 0) errors.push(`${rel}:${at}：${code}「${name}」在该 HTML 中定位不到任何条目`);
263:     else errors.push(`${rel}:${at}：${code}「${name}」在该 HTML 中命中 ${textHits.length} 条，无法唯一定位`);
264:   }
265: }
266: console.log(`[entry-anchors] 注册表 ${rows.length} 行；账本 ${ledRows.length} 行；承接矩阵 ${covRows.length} 行（同名 ${covMapped}、账本中无此 rule_id ${covNotInLedger}）；账本镜像列片段 ${mirrorChecked} 条；被检 Markdown ${mdFiles.length} 份；条目名引用 ${refTotal} 处（注册表原名 ${refByName} 处，正文唯一定位 ${refByText} 处）。`);
267: if (warns.length) warns.forEach(w => console.log("  提示：" + w));
268: if (errors.length) {
269:   console.error(`[entry-anchors] FAIL：${errors.length} 处不合格。`);
270:   errors.slice(0, 60).forEach(e => console.error("  - " + e));
271:   if (errors.length > 60) console.error(`  ……另有 ${errors.length - 60} 处未列出。`);
272:   console.error("  处置：条目改名或 HTML 改动后，重建 docs/prd-audit/entry-registry.csv 并把引用改为现行条目名；一律不得写回行号。");
273:   process.exit(1);
274: }
275: console.log("[entry-anchors] PASS：条目名唯一、注册表条目正文逐字与 HTML 现文一致、引用可唯一定位、正文无行号锚点、账本正文与九个镜像列均与 HTML 现文一致、账本与承接矩阵一一对应。");
~~~~

### tools/verify-html-ledger-sync.mjs
bytes=3804 SHA256=f325b4ccd9a4948b1eb7cc196fed4b6767c2a68d10f84005bf69943ddcb21944
~~~~text
1: #!/usr/bin/env node
2: // 文件版本：V0.01
3: // 门禁：三份业务最终解释权 HTML 与决策台账必须同一批改动内同步。
4: // 依据：用户 2026-08-03 拍板「台账保持完整，但改 HTML 必须在同一个 PR 内同步台账」。
5: // 病史：2026-07-22~08-01 三份 HTML 被 12 笔提交改过、台账停在改之前，挖出 36 条「已拍板未落 HTML」；
6: //       PR #326 合并后方向反转，台账 4 处反而落后。靠人记性两次都没守住，故加机械闸。
7: // 2026-08-07 门禁升级批 1（用户弹窗批立项）修两处空转假绿：
8: //   ① 只比对提交区 → 改 HTML 但没 commit 时门禁看不见，一路绿到 push 前。现按工作区脏净自动切范围。
9: //   ② 取不到基线时「跳过 exit 0」→ 无法判定不等于通过，改为 exit 1 并提示先 fetch。
10: import { execSync } from "node:child_process";
11: 
12: const git = (cmd) => execSync(`git ${cmd}`, { encoding: "utf8", maxBuffer: 32 * 1024 * 1024 });
13: 
14: // 一律切到仓根再干活：在子目录里跑时 git 只看当前目录，会静默变绿。
15: try {
16:   process.chdir(git("rev-parse --show-toplevel").trim());
17: } catch {
18:   console.error("[html-ledger-sync] FAIL: 当前目录不在 git 仓库内，无法定位仓根。");
19:   process.exit(1);
20: }
21: 
22: const HTML = [
23:   "docs/business-system-map-for-operations-v0.1.html",
24:   "docs/platform-governance-map-for-operations-v0.1.html",
25:   "docs/business-enum-quick-reference-v0.1.html",
26: ];
27: const LEDGER = "docs/prd-audit/business-decision-ledger.md";
28: 
29: // CI 提供事件发生前的基线；push main 后 origin/main 已指向 HEAD。
30: const base = process.argv[2] || process.env.HTML_GATES_BASE || "origin/main";
31: // 基线取不到就必须红：本门禁唯一的作用就是判定「改没改 HTML」，判不了不等于没改。
32: let mergeBase;
33: try {
34:   mergeBase = git(`merge-base ${base} HEAD`).trim();
35: } catch {
36:   console.error(`[html-ledger-sync] FAIL: 取不到基线 ${base}，无法判定本次改动范围。`);
37:   console.error("  无法判定不等于通过。请先 git fetch origin（CI 侧 actions/checkout 需 fetch-depth: 0）。");
38:   console.error("  如需换基线：node tools/verify-html-ledger-sync.mjs <基线ref>");
39:   process.exit(1);
40: }
41: 
42: // 三份 HTML 有未提交改动 → 连工作区一起比（基线↔工作区，含已提交的那部分）；
43: // 干净 → 维持原提交区口径 base...HEAD。
44: const dirty = git(`status --porcelain -- ${HTML.join(" ")}`).trim() !== "";
45: const range = dirty ? mergeBase : `${base}...HEAD`;
46: const scope = dirty
47:   ? `工作区（含未提交改动）↔ 基线 ${base}@${mergeBase.slice(0, 8)}`
48:   : `提交区 ${base}...HEAD（工作区三份 HTML 无未提交改动）`;
49: 
50: const changed = git(`diff --name-only ${range}`)
51:   .split("\n").map(s => s.trim()).filter(Boolean);
52: 
53: const touchedHtml = HTML.filter(f => changed.includes(f));
54: if (touchedHtml.length === 0) {
55:   console.log(`[html-ledger-sync] PASS: 本次未改动业务最终解释权 HTML。比对范围：${scope}。`);
56:   process.exit(0);
57: }
58: if (changed.includes(LEDGER)) {
59:   console.log(`[html-ledger-sync] PASS: 改动了 ${touchedHtml.length} 份业务 HTML，决策台账已同步。比对范围：${scope}。`);
60:   process.exit(0);
61: }
62: console.error(`[html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。比对范围：${scope}。`);
63: console.error("  已改动的 HTML：");
64: touchedHtml.forEach(f => console.error(`    - ${f}`));
65: console.error(`  必须在同一批改动内一并更新：${LEDGER}`);
66: console.error("  依据：用户 2026-08-03 拍板——台账保持完整，改 HTML 必须同 PR 同步台账。");
67: console.error("  若本次确为纯排版/错别字、不涉及口径，请在台账内追加一行说明后再提交。");
68: process.exit(1);
~~~~

### tools/verify-html-param-tier.mjs
bytes=10942 SHA256=f0010963d1d004f124da724cd52f990704b7b6466735fccd92edd653c67fb43e
~~~~text
1: #!/usr/bin/env node
2: // 文件版本：V0.01
3: // 门禁：三份业务最终解释权 HTML 里带具体数值的业务句，必须在原句里标出所属层级。
4: // 依据：用户 2026-08-03 拍板——参数分三层（法定值不可配／安全阈值可配但须最高权限审批并留痕／
5: //       其余业务参数一律可配），且三层一律在参数原句里字面标出，不设临场判定。
6: // 2026-08-07 门禁升级批 1（用户弹窗批立项）：由「只警告永不阻断」改为「棘轮阻断」。
7: //   ① 先剥掉 <style> 块与 HTML 注释再取正文——原版把 CSS 里的 width: 100%、border-radius: 50% 之类
8: //      当成业务参数：三份 HTML 全量实测 18 条命中里 9 条是 CSS，真业务句只有 9 条。
9: //      正是这一半噪声让人相信「这门禁只会瞎报」，于是它响了也没人看，等于没有。
10: //   ② 存量未标注句登记进 tools/html-param-tier-baseline.json：命中基线内 → 警告放行；基线外 → 阻断。
11: //      基线只许缩不许增，且该约束由本脚本机检（拿基线 commit 里的同一份文件比 key 集合，多出来即红）——
12: //      否则 --write-baseline 跑一遍就能给新违规发通行证，棘轮等于没上。
13: //   ③ 行内豁免：<!-- param-tier:ignore 0 元;1 月 --> 只豁免标记里列出的数值，同行其余数值照检；
14: //      不列数值的裸标记不再整行豁免（DEC-R8-81 豁免须带数值；2026-09-03 M001 审计改为阻断）——
15: //      整行豁免等于给这行以后新增的数值也预先发了通行证。
16: //   ④ 取不到基线一律红——无法判定不等于通过。
17: //   ⑤ 改动了 HTML 就全量重扫这几份文件（不只看 diff 增行）：句子被就地改数值、被挪位置都逃不掉。
18: import { execSync } from "node:child_process";
19: import fs from "node:fs";
20: import crypto from "node:crypto";
21: 
22: const git = (cmd) => execSync(`git ${cmd}`, { encoding: "utf8", maxBuffer: 32 * 1024 * 1024 });
23: 
24: // 一律切到仓根再干活：在子目录里跑时相对路径读不到文件、git 也只看当前目录，会静默变绿。
25: try {
26:   process.chdir(git("rev-parse --show-toplevel").trim());
27: } catch {
28:   console.error("[html-param-tier] FAIL: 当前目录不在 git 仓库内，无法定位仓根。");
29:   process.exit(1);
30: }
31: 
32: const HTML = [
33:   "docs/business-system-map-for-operations-v0.1.html",
34:   "docs/platform-governance-map-for-operations-v0.1.html",
35:   "docs/business-enum-quick-reference-v0.1.html",
36: ];
37: const BASELINE = "tools/html-param-tier-baseline.json";
38: 
39: // 具体数值 + 单位：形如 30 天、10%、3 个工作日、2 次、100 分、0.7
40: const VALUE = /(\d+(?:\.\d+)?)\s*(%|个工作日|工作日|个月|分钟|小时|天|月|年|次|杯|元|分)/g;
41: // 层级标注词：出现任一即视为已标注
42: const TIER = /(法定值|安全阈值|业务参数|(?<!不)可配|出厂默认)/;
43: // 行内豁免标记，可带数值清单：<!-- param-tier:ignore 0 元;1 月 -->
44: const IGNORE = /param-tier:ignore([^>]*)/;
45: 
46: // 取出一段文字里所有「数值+单位」，去掉空格后比较（「0 元」与「0元」视为同一个）
47: const valuesIn = (s) => (s.match(VALUE) || []).map((v) => v.replace(/\s+/g, ""));
48: const norm = (s) => s.replace(/<[^>]*>/g, " ").replace(/\s+/g, " ").trim();
49: const keyOf = (file, text) =>
50:   crypto.createHash("sha1").update(`${file}\n${text}`).digest("hex").slice(0, 16);
51: 
52: // 全量扫一份 HTML，返回「带数值但没标层级」的句子。稳定键=文件+归一化句子哈希，不含行号
53: // （DEC-R8-17 已废行号锚点，基线用行号必然随改动漂移）。
54: function scan(file) {
55:   const raw = fs.readFileSync(file, "utf8");
56:   // <style> 块与 HTML 注释整块屏蔽成等量空白：行号、行数都不变，行内豁免标记仍从原文取。
57:   const masked = raw
58:     .replace(/<style[\s\S]*?<\/style>/gi, (m) => m.replace(/[^\n]/g, " "))
59:     .replace(/<!--[\s\S]*?-->/g, (m) => m.replace(/[^\n]/g, " "));
60:   const rawLines = raw.split("\n");
61:   const seen = new Set();
62:   const hits = [];
63:   masked.split("\n").forEach((line, i) => {
64:     const text = norm(line);
65:     if (!text || TIER.test(text)) return;
66:     let vals = valuesIn(text);
67:     if (!vals.length) return;
68:     const mark = IGNORE.exec(rawLines[i] || "");
69:     if (mark) {
70:       const listed = valuesIn(mark[1]);
71:       if (!listed.length) {
72:         // DEC-R8-81：豁免只能带数值。裸标记不再整行放行，按未标层级句处理（M001 审计 2026-09-03）。
73:         console.error(
74:           `[html-param-tier] ${file} 有一处裸豁免（不列数值，含 ${vals.join("、")}）：` +
75:           `请改成带数值豁免 <!-- param-tier:ignore ${vals.join(";")} -->。`
76:         );
77:       } else {
78:         vals = vals.filter((v) => !listed.includes(v));
79:         if (!vals.length) return;
80:       }
81:     }
82:     const key = keyOf(file, text);
83:     if (seen.has(key)) return;
84:     seen.add(key);
85:     hits.push({ file, key, text, vals });
86:   });
87:   return hits;
88: }
89: 
90: function readBaseline(json, where) {
91:   try {
92:     const parsed = JSON.parse(json);
93:     if (!Array.isArray(parsed.entries)) throw new Error("entries 不是数组");
94:     return parsed;
95:   } catch (e) {
96:     console.error(`[html-param-tier] FAIL: ${where}的基线文件读不动（${e.message}）。`);
97:     console.error(`  期望格式 {"entries":[{"file":…,"key":…,"text":…}]}；重建：node tools/verify-html-param-tier.mjs --write-baseline`);
98:     process.exit(1);
99:   }
100: }
101: 
102: // 基线重建：node tools/verify-html-param-tier.mjs --write-baseline
103: // 只在「用户批准的补标草案落地后、存量条数变少」时用；想靠它给新违规发通行证会被下面的只缩不增机检拦住。
104: if (process.argv.includes("--write-baseline")) {
105:   const entries = HTML.flatMap(scan).map(({ file, key, text }) => ({ file, key, text }));
106:   const json = {
107:     _说明: [
108:       "存量待补层级标注的句子清单（门禁 tools/verify-html-param-tier.mjs 用）。",
109:       "草案批准一条删一条，只许缩不许增：新写的带数值业务句必须当场标层级，不得往这里补登记。",
110:       "只缩不增由门禁机检（与基线 commit 里的同名文件比 key 集合），手工加 key 会被拦下。",
111:       "稳定键 key = sha1(文件路径 + 换行 + 归一化句子).slice(0,16)，不含行号（DEC-R8-17 已废行号锚点）。",
112:       "命中基线内 → 警告放行；基线外 → 阻断。",
113:     ],
114:     entries,
115:   };
116:   fs.writeFileSync(BASELINE, JSON.stringify(json, null, 2) + "\n");
117:   console.log(`[html-param-tier] 已重建基线 ${BASELINE}：${entries.length} 条。请人工核对条数只减不增。`);
118:   process.exit(0);
119: }
120: 
121: // CI 提供事件发生前的基线；push main 后 origin/main 已指向 HEAD。
122: const base = process.argv[2] || process.env.HTML_GATES_BASE || "origin/main";
123: 
124: let mergeBase;
125: try {
126:   mergeBase = git(`merge-base ${base} HEAD`).trim();
127: } catch {
128:   console.error(`[html-param-tier] FAIL: 取不到基线 ${base}，无法判定本次改动范围。`);
129:   console.error("  无法判定不等于通过。请先 git fetch origin（CI 侧 actions/checkout 需 fetch-depth: 0）。");
130:   console.error("  如需换基线：node tools/verify-html-param-tier.mjs <基线ref>");
131:   process.exit(1);
132: }
133: 
134: const local = readBaseline(fs.readFileSync(BASELINE, "utf8"), "本地");
135: 
136: // 棘轮只缩不增机检：本地基线出现基线 commit 所没有的 key，一律红。
137: // 基线 commit 上没有这份文件 = 本批首次引入，放行。
138: let prevJson = null;
139: try {
140:   // 用 stdio 选项而不是 shell 重定向：Windows 上 cmd 会把 2>/dev/null 当成路径而报错，导致棘轮比较被静默跳过。
141:   prevJson = execSync(`git show ${mergeBase}:${BASELINE}`, { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] });
142: } catch {
143:   prevJson = null;
144: }
145: if (prevJson !== null) {
146:   const prevKeys = new Set(readBaseline(prevJson, `基线 ${mergeBase.slice(0, 8)} `).entries.map((e) => e.key));
147:   const added = local.entries.filter((e) => !prevKeys.has(e.key));
148:   if (added.length) {
149:     console.error(`[html-param-tier] FAIL: 存量基线只许缩不许增，本次多出 ${added.length} 条 key。`);
150:     console.error(`  基线 ${base}@${mergeBase.slice(0, 8)} 有 ${prevKeys.size} 条，本地 ${BASELINE} 有 ${local.entries.length} 条。`);
151:     added.forEach((e) => console.error(`  + ${e.key} ${e.file}\n      ${String(e.text).slice(0, 100)}`));
152:     console.error("  新写的带数值业务句必须当场标层级或用 param-tier:ignore 豁免，不得补登记进基线。");
153:     process.exit(1);
154:   }
155: }
156: 
157: const dirty = git(`status --porcelain -- ${HTML.join(" ")}`).trim() !== "";
158: const range = dirty ? mergeBase : `${base}...HEAD`;
159: const scope = dirty
160:   ? `工作区（含未提交改动）↔ 基线 ${base}@${mergeBase.slice(0, 8)}`
161:   : `提交区 ${base}...HEAD（工作区三份 HTML 无未提交改动）`;
162: 
163: const changed = git(`diff --name-only ${range}`)
164:   .split("\n").map((s) => s.trim()).filter(Boolean);
165: const touched = HTML.filter((f) => changed.includes(f));
166: 
167: if (touched.length === 0) {
168:   console.log(`[html-param-tier] PASS: 本次未改动业务最终解释权 HTML。比对范围：${scope}。`);
169:   process.exit(0);
170: }
171: 
172: const known = new Set(local.entries.map((e) => e.key));
173: const hits = touched.flatMap(scan);
174: const legacy = hits.filter((h) => known.has(h.key));
175: const fresh = hits.filter((h) => !known.has(h.key));
176: const show = (h) =>
177:   `  - ${h.file}（未标层级的数值：${h.vals.join("、")}）\n      ${h.text.slice(0, 160)}${h.text.length > 160 ? "…" : ""}`;
178: 
179: if (legacy.length) {
180:   console.warn(`[html-param-tier] 警告 ${legacy.length} 处：已登记在册的存量未标层级句（${BASELINE}），本次放行。`);
181:   legacy.forEach((h) => console.warn(show(h)));
182: }
183: 
184: if (fresh.length === 0) {
185:   console.log(
186:     `[html-param-tier] PASS: 改动了 ${touched.length} 份业务 HTML，全量重扫无基线外的未标层级句` +
187:     `（存量 ${legacy.length} 处已登记放行）。比对范围：${scope}；扫描口径：改动文件全文，已剥除 <style> 与注释。`
188:   );
189:   process.exit(0);
190: }
191: 
192: console.error(`[html-param-tier] FAIL: ${fresh.length} 处带具体数值的语句未标出所属层级，且不在存量基线内。`);
193: console.error(`  比对范围：${scope}；扫描口径：改动文件全文，已剥除 <style> 与注释。`);
194: console.error("  三层写法：（法定值，不可配）／（安全阈值，按X可配，调整须最高权限审批并留痕）／（业务参数，按X可配，出厂默认 N）");
195: fresh.forEach((h) => console.error(show(h)));
196: console.error("  确为法条编号、举例日期等非参数表述：在该行内加 <!-- param-tier:ignore 数值1;数值2 --> 只豁免这些数值。");
197: console.error(`  存量待补标的老句子在 ${BASELINE}——那份只许缩不许增，新句子不得往里补登记。`);
198: process.exit(1);
~~~~

### tools/gate-hardening-tests/99-predeploy-final.log
bytes=7027 SHA256=61c74dc4e62f27c5f75c3a39735c93f73ba8054a26c063822da7d05348804421
~~~~text
1: 
2: > entertainment-complex-management@0.1.0 verify:predeploy
3: > npm run verify:html-ledger-sync && npm run verify:entry-anchors && npm run verify:html-param-tier && npm run verify:delivery-gates-docs && npm run verify:full-admin-matrix && npm run verify:domain-ownership-scope && npm run verify:matrix-vs-migration && npm run verify:data-isolation-spec && npm run verify:tenant-isolation && npm run verify:p0-contract && npm run verify:security-baseline && npm run verify:production-readiness-evidence && npm run verify:production-blockers && npm run verify:pii-encryption && npm run verify:test-db-isolation
4: 
5: 
6: > entertainment-complex-management@0.1.0 verify:html-ledger-sync
7: > node tools/verify-html-ledger-sync.mjs
8: 
9: [html-ledger-sync] PASS: 本次未改动业务最终解释权 HTML。比对范围：提交区 origin/main...HEAD（工作区三份 HTML 无未提交改动）。
10: 
11: > entertainment-complex-management@0.1.0 verify:entry-anchors
12: > node tools/verify-entry-anchors.mjs
13: 
14: [entry-anchors] 注册表 1210 行；账本 1225 行；承接矩阵 1234 行（同名 1225、账本中无此 rule_id 9）；被检 Markdown 341 份；条目名引用 1040 处（注册表原名 935 处，正文唯一定位 105 处）。
15: [entry-anchors] PASS：条目名唯一、引用可唯一定位、正文无行号锚点、账本已登记条目正文与 HTML 现文一致（未登记条目不在保护范围，见 R8-U-02）、账本与承接矩阵一一对应。
16: 
17: > entertainment-complex-management@0.1.0 verify:html-param-tier
18: > node tools/verify-html-param-tier.mjs
19: 
20: [html-param-tier] PASS: 本次未改动业务最终解释权 HTML。比对范围：提交区 origin/main...HEAD（工作区三份 HTML 无未提交改动）。
21: 
22: > entertainment-complex-management@0.1.0 verify:delivery-gates-docs
23: > node tools/verify-delivery-gates.mjs
24: 
25: DELIVERY_GATES_DOCS_PASS
26: 
27: > entertainment-complex-management@0.1.0 verify:full-admin-matrix
28: > node tools/verify-full-admin-page-matrix.mjs
29: 
30: 完整后台页面矩阵校验通过：90 个页面均有路由、权限、UI 与测试覆盖
31: 
32: > entertainment-complex-management@0.1.0 verify:domain-ownership-scope
33: > node tools/generate-domain-ownership-scope-audit.mjs --check
34: 
35: DOMAIN_OWNERSHIP_SCOPE_PASS: 325 tables classified
36: 
37: > entertainment-complex-management@0.1.0 verify:matrix-vs-migration
38: > node tools/verify-matrix-vs-migration.mjs
39: 
40: matrix-vs-migration verification passed: 338 tables parsed; 142 present-claims confirmed, 6 absent-claims still-absent (tracked gaps).
41: 
42: > entertainment-complex-management@0.1.0 verify:data-isolation-spec
43: > node tools/generate-data-isolation-spec.mjs --check
44: 
45: data isolation spec is in sync with Flyway migration scope baseline.
46: 
47: > entertainment-complex-management@0.1.0 verify:tenant-isolation
48: > node tools/verify-tenant-isolation.mjs
49: 
50: [tenant-isolation] 扫描范围：
51:   - 权威表清单（CSV）：325 表（direct=172，白名单豁免=78，inherited 为信息性盲区）
52:   - Jdbc*Repository 源：53 个文件
53:   - 本次扫出 finding：5（已登记 baseline：5，新增回归：0）
54: [tenant-isolation] KNOWN-BLINDSPOT B1（inherited，仅信息性，不硬失败）：69 表
55:     · amusement_incident_actions (scope_source: inspection/incident id)
56:     · amusement_safety_actions (scope_source: inspection/incident id)
57:     · analytics_ai_thresholds (scope_source: dimension/project/merchant)
58:     · analytics_customer_intents (scope_source: dimension/project/merchant)
59:     · analytics_metric_anomalies (scope_source: dimension/project/merchant)
60:     · analytics_metric_definitions (scope_source: dimension/project/merchant)
61:     · …其余 63 表略
62: 
63: [tenant-isolation] PASS（棘轮）：无新增回归；5 条已知缺口在台账跟踪闭环中（见 findings-register-v1.md tenant-isolation 小节）。隔离面未劣化。
64: 
65: > entertainment-complex-management@0.1.0 verify:p0-contract
66: > npm run verify:no-legacy-api-prototype && npm run verify:openapi && npm run verify:openapi-examples && npm run verify:operation-matrix && npm run verify:openapi-closure && npm run verify:client-routes && npm run verify:client-openapi && npm run verify:client-payload
67: 
68: 
69: > entertainment-complex-management@0.1.0 verify:no-legacy-api-prototype
70: > node tools/verify-no-legacy-api-prototype.mjs
71: 
72: NO_LEGACY_API_PROTOTYPE_PASS
73: 
74: > entertainment-complex-management@0.1.0 verify:openapi
75: > node tools/verify-openapi-contract.mjs
76: 
77: OpenAPI contract verification passed.
78: 
79: > entertainment-complex-management@0.1.0 verify:openapi-examples
80: > node tools/verify-openapi-p0-examples.mjs
81: 
82: OpenAPI P0 example verification passed: 8 request bodies and 3 responses checked.
83: 
84: > entertainment-complex-management@0.1.0 verify:operation-matrix
85: > node tools/generate-openapi-operation-matrix.mjs --check
86: 
87: docs/p0-openapi-operation-acceptance-matrix-v0.1.md is in sync with docs/openapi/openapi-p0.json; 720 operations checked.
88: 
89: > entertainment-complex-management@0.1.0 verify:openapi-closure
90: > node tools/generate-openapi-contract-closure-appendix.mjs --check
91: 
92: /home/james/wt-gates/docs/p0-openapi-contract-closure-appendix-v0.1.md is in sync with /home/james/wt-gates/docs/openapi/openapi-p0.json; 720 operations checked.
93: 
94: > entertainment-complex-management@0.1.0 verify:client-routes
95: > node tools/audit-client-api-routes.mjs --strict
96: 
97: [client-api-route-audit] PASS: client code only references formal /api/v1 or /api/open paths.
98: 
99: > entertainment-complex-management@0.1.0 verify:client-openapi
100: > node tools/audit-client-openapi-coverage.mjs --strict
101: 
102: [client-openapi-coverage] PASS: 59 client route template(s) are covered by OpenAPI; 59 method-specific call(s) match OpenAPI methods.
103: 
104: > entertainment-complex-management@0.1.0 verify:client-payload
105: > node tools/verify-client-payload-policy.mjs --strict
106: 
107: [client-payload-policy] PASS: client payload normalization and OpenAPI request field casing are enforced.
108: 
109: > entertainment-complex-management@0.1.0 verify:security-baseline
110: > npm run verify:prod-docs-access && node tools/verify-formal-security-baseline.mjs --strict
111: 
112: 
113: > entertainment-complex-management@0.1.0 verify:prod-docs-access
114: > node tools/verify-prod-docs-access-policy.mjs
115: 
116: production OpenAPI/Swagger docs access policy verification passed.
117: Formal security baseline verification passed: security-scans/formal-security-baseline-v1.md and security-scans/findings-register-v1.md.
118: 
119: > entertainment-complex-management@0.1.0 verify:production-readiness-evidence
120: > node tools/verify-production-readiness-evidence.mjs
121: 
122: PRODUCTION_READINESS_EVIDENCE_PASS
123: 
124: > entertainment-complex-management@0.1.0 verify:production-blockers
125: > node tools/generate-production-blocker-register.mjs --check
126: 
127: PRODUCTION_BLOCKER_REGISTER_PASS
128: 
129: > entertainment-complex-management@0.1.0 verify:pii-encryption
130: > node tools/verify-pii-encryption.mjs
131: 
132: PII_ENCRYPTION_PASS: 9 个 PII 列均经 piiCipher.encrypt 落库
133: 
134: > entertainment-complex-management@0.1.0 verify:test-db-isolation
135: > node tools/verify-test-db-isolation.mjs
136: 
137: TEST_DB_ISOLATION_PASS: 测试源未引用生产库 complex（均用 complex_test）
138: EXIT=0
~~~~

### tools/gate-hardening-tests/A-worktree-uncommitted.log
bytes=5776 SHA256=14ada1aa68187b672b10bcd98b0843df7177c147adafee8ded0cd826053942c4
~~~~text
1: ===== 测试 A（返工后复跑）：三份 HTML 之一在工作区被改、未提交（塞一句含数值的假句） =====
2: 注：本批 I-3 已改台账，ledger-sync 会因此判 PASS（语义正确）。
3:     为验证红路径，A1 前临时把台账还原成基线版（测完立即恢复本批改动）。
4: --- A0 对照：origin/main 上的旧脚本（预期假绿 exit 0） ---
5: [html-ledger-sync] PASS: 本次未改动业务最终解释权 HTML。
6: OLD-LEDGER EXIT=0
7: [html-param-tier] PASS: 本次未改动业务最终解释权 HTML。
8: OLD-PARAM  EXIT=0
9: --- A1a 新 ledger-sync（台账已同步 → 预期绿，本批真实状态） ---
10: [html-ledger-sync] PASS: 改动了 1 份业务 HTML，决策台账已同步。比对范围：工作区（含未提交改动）↔ 基线 origin/main@7da3aaa1。
11: NEW-LEDGER EXIT=0
12: --- A1b 新 ledger-sync（临时撤掉台账改动 → 预期红 exit 1） ---
13: [html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。比对范围：工作区（含未提交改动）↔ 基线 origin/main@7da3aaa1。
14:   已改动的 HTML：
15:     - docs/business-system-map-for-operations-v0.1.html
16:   必须在同一批改动内一并更新：docs/prd-audit/business-decision-ledger.md
17:   依据：用户 2026-08-03 拍板——台账保持完整，改 HTML 必须同 PR 同步台账。
18:   若本次确为纯排版/错别字、不涉及口径，请在台账内追加一行说明后再提交。
19: NEW-LEDGER EXIT=1
20: （台账本批改动已恢复）
21: --- A2 新 param-tier（预期检出假句、基线外 → 红 exit 1） ---
22: [html-param-tier] 警告 6 处：已登记在册的存量未标层级句（tools/html-param-tier-baseline.json），本次放行。
23:   - docs/business-system-map-for-operations-v0.1.html（未标层级的数值：0元）
24:       所有商品可录入内部结算价（成本价口径）；销售赠送的菜品/酒对客算 0 元、不计入消费额。赠送分两种，分别处理：一是营销人员赠送，其成本按内部结算价从这笔销售的提成基数中扣除，扣减后的提成基数以零为下限、不得为负，也不得倒扣该员工其他单据的提成，超出本单销售额的赠送成本由该店承担，营销人员的赠送不另设额度上限、由扣减提成…
25:   - docs/business-system-map-for-operations-v0.1.html（未标层级的数值：51%）
26:       新员工注册分两条路径：商户员工由商户代该员工提交注册申请，经对应域公司批准后账号生效——城市项目公司、娱乐平台公司或游乐公司，按该商户所属经营域确定；公司自有员工由本人在私人手机 App 自助注册提交资料，经所属公司后台审批。公司持股超过 51% 的商户视同公司自有，其员工按公司自有员工路径办理。
27:   - docs/business-system-map-for-operations-v0.1.html（未标层级的数值：1月、2月、12月、1月）
28:       请假扣薪：本综合体实行综合计算工时。只有事假（无薪假）扣薪，按该员工当期请假工时占当期总工时的比例，从其当期应发薪资中扣减；扣减后的当期薪资以零为下限，不得为负，超出部分不再扣减也不结转下月。病假不扣薪，年假、调休等带薪假也不扣；参照国家规定，法定带薪假一律不扣。班次起止跨越月份、周或年度边界的（如 1 月 31 日 …
29:   - docs/business-system-map-for-operations-v0.1.html（未标层级的数值：100%）
30:       营销费用 活动发起方与费用承担方可以不同；费用承担方必须锁定到具体法律主体，可以是商户、娱乐平台公司、游乐公司、传媒公司、城市项目公司、中央餐饮公司、全国平台或其他登记赞助主体。同一场活动支持多个主体共同承担，按固定金额或按比例分摊，采用比例分摊时各方比例合计必须为 100%；各承担方的预算分别锁定、分别扣减，任一方预…
31:   - docs/business-system-map-for-operations-v0.1.html（未标层级的数值：80%、20%）
32:       部分业态（典型是酒吧）采用“公司参股 + 主理人经营”。酒吧股权由娱乐平台公司、主理人及其他真实股东按工商登记和出资协议持有；80%／20%只作为新酒吧默认建议，实际以每家酒吧的工商登记比例为准。酒吧经营牌照登记在实际经营法律主体名下；场地由具备物业权利的城市项目公司按有效合同提供；资金由实际出资或贷款主体按真实资金关…
33:   - docs/business-system-map-for-operations-v0.1.html（未标层级的数值：80%、20%）
34:       娱乐平台公司、主理人和其他股东按每家酒吧工商登记的真实持股比例分配；新酒吧可预填 80%／20%建议值，但必须以获批登记版本生效，修改不能追溯已经确认或发放的分红。
35: [html-param-tier] FAIL: 1 处带具体数值的语句未标出所属层级，且不在存量基线内。
36:   比对范围：工作区（含未提交改动）↔ 基线 origin/main@7da3aaa1；扫描口径：改动文件全文，已剥除 <style> 与注释。
37:   三层写法：（法定值，不可配）／（安全阈值，按X可配，调整须最高权限审批并留痕）／（业务参数，按X可配，出厂默认 N）
38:   - docs/business-system-map-for-operations-v0.1.html（未标层级的数值：30天、3次）
39:       门禁反向测试假句：会员保级通知须在到期前 30 天发出，每人每期最多补发 3 次。
40:   确为法条编号、举例日期等非参数表述：在该行内加 <!-- param-tier:ignore 数值1;数值2 --> 只豁免这些数值。
41:   存量待补标的老句子在 tools/html-param-tier-baseline.json——那份只许缩不许增，新句子不得往里补登记。
42: NEW-PARAM EXIT=1
~~~~

### tools/gate-hardening-tests/B-committed.log
bytes=1692 SHA256=3dce69862e4c9c340f9db0d1769c6e383172b05b3447140c4113f550bbe63339
~~~~text
1: ===== 测试 B（返工后复跑）：同一假改动已 commit（三份 HTML 工作区干净 → 提交区口径） =====
2: 测试 commit = adfd6489；台账本批改动仍未提交，故 ledger-sync 应判红
3: --- B0 对照：origin/main 旧 ledger-sync（提交区本就看得见，预期红） ---
4: [html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。
5: OLD-LEDGER EXIT=1
6: --- B1 新 ledger-sync（预期红 exit 1，与 A1b 一致） ---
7: [html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。比对范围：提交区 origin/main...HEAD（工作区三份 HTML 无未提交改动）。
8: NEW-LEDGER EXIT=1
9: --- B2 新 param-tier（预期红 exit 1，与 A2 一致） ---
10: [html-param-tier] 警告 6 处：已登记在册的存量未标层级句（tools/html-param-tier-baseline.json），本次放行。
11: [html-param-tier] FAIL: 1 处带具体数值的语句未标出所属层级，且不在存量基线内。
12:   比对范围：提交区 origin/main...HEAD（工作区三份 HTML 无未提交改动）；扫描口径：改动文件全文，已剥除 <style> 与注释。
13:   三层写法：（法定值，不可配）／（安全阈值，按X可配，调整须最高权限审批并留痕）／（业务参数，按X可配，出厂默认 N）
14:   - docs/business-system-map-for-operations-v0.1.html（未标层级的数值：30天、3次）
15:   确为法条编号、举例日期等非参数表述：在该行内加 <!-- param-tier:ignore 数值1;数值2 --> 只豁免这些数值。
16:   存量待补标的老句子在 tools/html-param-tier-baseline.json——那份只许缩不许增，新句子不得往里补登记。
17: NEW-PARAM EXIT=1
~~~~

### tools/gate-hardening-tests/C-no-baseline.log
bytes=2462 SHA256=5f33afad72fc0a3a1375da16a08293097b74bb7795e05ab6ca6d341baf10ec67
~~~~text
1: ===== 测试 C（返工后复跑）：取不到基线 =====
2: --- C1 传入不存在的基线 ref ---
3: fatal: ambiguous argument 'origin/no-such-base...HEAD': unknown revision or path not in the working tree.
4: Use '--' to separate paths from revisions, like this:
5: 'git <command> [<revision>...] -- [<file>...]'
6: [html-ledger-sync] 无法比对基线 origin/no-such-base，跳过（本地未 fetch 时属正常）
7: OLD-LEDGER EXIT=0
8: fatal: bad revision 'origin/no-such-base...HEAD'
9: [html-param-tier] 无法比对基线 origin/no-such-base，跳过（本地未 fetch 时属正常）
10: OLD-PARAM  EXIT=0
11: fatal: Not a valid object name origin/no-such-base
12: [html-ledger-sync] FAIL: 取不到基线 origin/no-such-base，无法判定本次改动范围。
13:   无法判定不等于通过。请先 git fetch origin（CI 侧 actions/checkout 需 fetch-depth: 0）。
14:   如需换基线：node tools/verify-html-ledger-sync.mjs <基线ref>
15: NEW-LEDGER EXIT=1
16: fatal: Not a valid object name origin/no-such-base
17: [html-param-tier] FAIL: 取不到基线 origin/no-such-base，无法判定本次改动范围。
18:   无法判定不等于通过。请先 git fetch origin（CI 侧 actions/checkout 需 fetch-depth: 0）。
19:   如需换基线：node tools/verify-html-param-tier.mjs <基线ref>
20: NEW-PARAM  EXIT=1
21: --- C2 /tmp/nobase 无 origin 远端，默认基线 origin/main ---
22: fatal: ambiguous argument 'origin/main...HEAD': unknown revision or path not in the working tree.
23: Use '--' to separate paths from revisions, like this:
24: 'git <command> [<revision>...] -- [<file>...]'
25: [html-ledger-sync] 无法比对基线 origin/main，跳过（本地未 fetch 时属正常）
26: OLD-LEDGER EXIT=0
27: fatal: bad revision 'origin/main...HEAD'
28: [html-param-tier] 无法比对基线 origin/main，跳过（本地未 fetch 时属正常）
29: OLD-PARAM  EXIT=0
30: fatal: Not a valid object name origin/main
31: [html-ledger-sync] FAIL: 取不到基线 origin/main，无法判定本次改动范围。
32:   无法判定不等于通过。请先 git fetch origin（CI 侧 actions/checkout 需 fetch-depth: 0）。
33:   如需换基线：node tools/verify-html-ledger-sync.mjs <基线ref>
34: NEW-LEDGER EXIT=1
35: fatal: Not a valid object name origin/main
36: [html-param-tier] FAIL: 取不到基线 origin/main，无法判定本次改动范围。
37:   无法判定不等于通过。请先 git fetch origin（CI 侧 actions/checkout 需 fetch-depth: 0）。
38:   如需换基线：node tools/verify-html-param-tier.mjs <基线ref>
39: NEW-PARAM  EXIT=1
~~~~

### tools/gate-hardening-tests/D-baseline-legacy-pass.log
bytes=1100 SHA256=7fbcd28464611e031881424bb4478767c33118c5c621a2e48da2fb59471ec164
~~~~text
1: ===== 测试 D（返工后复跑）：HTML 有改动、命中全在存量基线内 → 警告放行不红 =====
2: [html-param-tier] 警告 6 处：已登记在册的存量未标层级句（tools/html-param-tier-baseline.json），本次放行。
3:   - docs/business-system-map-for-operations-v0.1.html（未标层级的数值：0元）
4:   - docs/business-system-map-for-operations-v0.1.html（未标层级的数值：51%）
5:   - docs/business-system-map-for-operations-v0.1.html（未标层级的数值：1月、2月、12月、1月）
6:   - docs/business-system-map-for-operations-v0.1.html（未标层级的数值：100%）
7:   - docs/business-system-map-for-operations-v0.1.html（未标层级的数值：80%、20%）
8:   - docs/business-system-map-for-operations-v0.1.html（未标层级的数值：80%、20%）
9: [html-param-tier] PASS: 改动了 1 份业务 HTML，全量重扫无基线外的未标层级句（存量 6 处已登记放行）。比对范围：工作区（含未提交改动）↔ 基线 origin/main@7da3aaa1；扫描口径：改动文件全文，已剥除 <style> 与注释。
10: NEW-PARAM EXIT=0
~~~~

### tools/gate-hardening-tests/E-ignore-marker.log
bytes=803 SHA256=0c29f64c5510c204dc6be1a48293b5df69a1e58e53674b5e40d3469db5ab8633
~~~~text
1: ===== 测试 E：裸豁免标记（不列数值）→ 整行豁免 + 提醒改成带数值豁免，不红 =====
2: [html-param-tier] 提醒：docs/business-system-map-for-operations-v0.1.html 有一处整行豁免（该行 41 字，含 30天、3次），建议改成带数值豁免 <!-- param-tier:ignore 30天;3次 -->，否则这行以后新增的数值也一并被放过。
3: [html-param-tier] 警告 6 处：已登记在册的存量未标层级句（tools/html-param-tier-baseline.json），本次放行。
4: [html-param-tier] PASS: 改动了 1 份业务 HTML，全量重扫无基线外的未标层级句（存量 6 处已登记放行）。比对范围：工作区（含未提交改动）↔ 基线 origin/main@7da3aaa1；扫描口径：改动文件全文，已剥除 <style> 与注释。
5: NEW-PARAM EXIT=0
~~~~

### tools/gate-hardening-tests/F-ci-baseline-evidence.log
bytes=926 SHA256=5e07e04d48d5b3312dac8bd6fd5e107ebe5965fcd6414d4e8041847fbdfca14f
~~~~text
1: ===== F：CI 侧 origin/main 可解析性取证（决定「无基线转 exit 1」会不会把 CI 变红） =====
2: 取证对象：最近一次 pull_request 事件 run 31145223559（docs/rule-registry-expansion）
3: 判据：若 CI 里取不到 origin/main，旧脚本会打印「无法比对基线 origin/main，跳过」；
4:       实际打印的是「PASS: 本次未改动…」，说明基线在 CI 里取得到（actions/checkout fetch-depth: 0 生效）。
5: predeploy	Run actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5	2026-08-07T03:43:47.4940554Z  * [new branch]        main                                     -> origin/main
6: predeploy	verify:predeploy (governance gates)	2026-08-07T03:43:57.9172702Z [html-ledger-sync] PASS: 本次未改动业务最终解释权 HTML。
7: predeploy	verify:predeploy (governance gates)	2026-08-07T03:43:59.9433767Z [html-param-tier] PASS: 本次未改动业务最终解释权 HTML。
~~~~

### tools/gate-hardening-tests/G-merge-pr-guards.log
bytes=1108 SHA256=59ebf364d51ad9caa6b03234eced31ef6d8cce2fbebf5c4f64828bb2ffe489ae
~~~~text
1: ===== merge-pr.sh 守卫路径实测（全部在任何 mutating 动作之前中止；真合并未测，见报告） =====
2: --- G1 非法合并方式 ---
3: 合并方式只能是 --squash|--merge|--rebase
4: EXIT=1
5: --- G2 在 main 上执行（借主工作区，只读） ---
6: [merge] main 上没有待合并的 PR;请切到 feature 分支。
7: EXIT=1
8: --- G3 脏工作区（A-3 新增守卫；本工作区当前就是脏的） ---
9: [merge] 工作区有未提交改动;CI 跑的是远端 commit,这些改动不在里面。
10:         先提交并 push,或 stash 掉再合。
11:  M AGENTS.md
12:  M docs/agents/issue-tracker.md
13:   （…省略 porcelain 清单…）
14: --- G4 工作区干净但分支未 push（stash 掉全部改动后验证下一道守卫） ---
15: [merge] origin 上没有 tools/gate-hardening-batch1;先跑 ops/160/propose-change.sh 推分支开 PR。
16: EXIT=1
17:   （改动已 stash pop 还原）
18: --- G5 gh 查询语句实测（只读） ---
19: commit=7da3aaa152181f8d89bdd5d1a42abdecf71d8627
20: [{"conclusion":"success","databaseId":31145269210,"event":"push"}]
21: gh EXIT=0
22: --- G6 bash -n ---
23: merge-pr.sh OK
24: propose-change.sh OK
~~~~

### tools/gate-hardening-tests/I1-baseline-ratchet.log
bytes=1055 SHA256=fa1a0a89f762063635f0711a5b57ed4f97be5a57a1e091cc0b33d28475d7b483
~~~~text
1: ===== 测试 I-1：棘轮基线只许缩不许增（机检） =====
2: 对照基线 commit = 828796f5（含 tools/html-param-tier-baseline.json 9 条）
3: --- I-1① 手工往本地基线追加一条 key → 预期红 exit 1 ---
4: [html-param-tier] FAIL: 存量基线只许缩不许增，本次多出 1 条 key。
5:   基线 HEAD@828796f5 有 9 条，本地 tools/html-param-tier-baseline.json 有 10 条。
6:   + deadbeefdeadbeef docs/business-system-map-for-operations-v0.1.html
7:       伪造条目：想靠补登记给新违规发通行证，上限 99 次。
8:   新写的带数值业务句必须当场标层级或用 param-tier:ignore 豁免，不得补登记进基线。
9: NEW-PARAM EXIT=1
10: --- I-1② 手工从本地基线删掉一条 key（模拟补标草案落地）→ 预期绿 exit 0 ---
11: 删掉的是 02be773ab1346dba 所有商品可录入内部结算价（成本价口径）；销售赠送
12: [html-param-tier] PASS: 本次未改动业务最终解释权 HTML。比对范围：提交区 HEAD...HEAD（工作区三份 HTML 无未提交改动）。
13: NEW-PARAM EXIT=0
~~~~

### tools/gate-hardening-tests/I2-ignore-granularity.log
bytes=4264 SHA256=75394c0dd0e5e3c8296edb036ea053af444a0ea678277919e301478837e3a25b
~~~~text
1: ===== 测试 I-2：带数值豁免的粒度 =====
2: --- I-2a 标记列全两个数值（30 天;3 次）→ 预期绿 ---
3: [html-param-tier] 警告 6 处：已登记在册的存量未标层级句（tools/html-param-tier-baseline.json），本次放行。
4: [html-param-tier] PASS: 改动了 1 份业务 HTML，全量重扫无基线外的未标层级句（存量 6 处已登记放行）。比对范围：工作区（含未提交改动）↔ 基线 origin/main@7da3aaa1；扫描口径：改动文件全文，已剥除 <style> 与注释。
5: NEW-PARAM EXIT=0
6: --- I-2b 标记只列 30 天，同行还有未列的 3 次 → 预期红 exit 1 ---
7: [html-param-tier] 警告 6 处：已登记在册的存量未标层级句（tools/html-param-tier-baseline.json），本次放行。
8:       所有商品可录入内部结算价（成本价口径）；销售赠送的菜品/酒对客算 0 元、不计入消费额。赠送分两种，分别处理：一是营销人员赠送，其成本按内部结算价从这笔销售的提成基数中扣除，扣减后的提成基数以零为下限、不得为负，也不得倒扣该员工其他单据的提成，超出本单销售额的赠送成本由该店承担，营销人员的赠送不另设额度上限、由扣减提成…
9:       新员工注册分两条路径：商户员工由商户代该员工提交注册申请，经对应域公司批准后账号生效——城市项目公司、娱乐平台公司或游乐公司，按该商户所属经营域确定；公司自有员工由本人在私人手机 App 自助注册提交资料，经所属公司后台审批。公司持股超过 51% 的商户视同公司自有，其员工按公司自有员工路径办理。
10:       请假扣薪：本综合体实行综合计算工时。只有事假（无薪假）扣薪，按该员工当期请假工时占当期总工时的比例，从其当期应发薪资中扣减；扣减后的当期薪资以零为下限，不得为负，超出部分不再扣减也不结转下月。病假不扣薪，年假、调休等带薪假也不扣；参照国家规定，法定带薪假一律不扣。班次起止跨越月份、周或年度边界的（如 1 月 31 日 …
11:       营销费用 活动发起方与费用承担方可以不同；费用承担方必须锁定到具体法律主体，可以是商户、娱乐平台公司、游乐公司、传媒公司、城市项目公司、中央餐饮公司、全国平台或其他登记赞助主体。同一场活动支持多个主体共同承担，按固定金额或按比例分摊，采用比例分摊时各方比例合计必须为 100%；各承担方的预算分别锁定、分别扣减，任一方预…
12:       部分业态（典型是酒吧）采用“公司参股 + 主理人经营”。酒吧股权由娱乐平台公司、主理人及其他真实股东按工商登记和出资协议持有；80%／20%只作为新酒吧默认建议，实际以每家酒吧的工商登记比例为准。酒吧经营牌照登记在实际经营法律主体名下；场地由具备物业权利的城市项目公司按有效合同提供；资金由实际出资或贷款主体按真实资金关…
13:       娱乐平台公司、主理人和其他股东按每家酒吧工商登记的真实持股比例分配；新酒吧可预填 80%／20%建议值，但必须以获批登记版本生效，修改不能追溯已经确认或发放的分红。
14: [html-param-tier] FAIL: 1 处带具体数值的语句未标出所属层级，且不在存量基线内。
15:   比对范围：工作区（含未提交改动）↔ 基线 origin/main@7da3aaa1；扫描口径：改动文件全文，已剥除 <style> 与注释。
16:   三层写法：（法定值，不可配）／（安全阈值，按X可配，调整须最高权限审批并留痕）／（业务参数，按X可配，出厂默认 N）
17:   - docs/business-system-map-for-operations-v0.1.html（未标层级的数值：3次）
18:       门禁反向测试假句：会员保级通知须在到期前 30 天发出，每人每期最多补发 3 次。
19:   确为法条编号、举例日期等非参数表述：在该行内加 <!-- param-tier:ignore 数值1;数值2 --> 只豁免这些数值。
20:   存量待补标的老句子在 tools/html-param-tier-baseline.json——那份只许缩不许增，新句子不得往里补登记。
21: NEW-PARAM EXIT=1
~~~~

### tools/gate-hardening-tests/m001-regression.mjs
bytes=11967 SHA256=edb313704c4efbfae3946a9898e2c94771ed5a7990feed7bcccecc959415889b
~~~~text
1: // 文件版本：V0.05
2: // 实际运行治理脚本；输入和模拟合并均在临时目录，绝不操作真实 PR。
3: import assert from "node:assert/strict";
4: import fs from "node:fs";
5: import os from "node:os";
6: import path from "node:path";
7: import { fileURLToPath } from "node:url";
8: import { spawnSync } from "node:child_process";
9: 
10: const root = fileURLToPath(new URL("../../", import.meta.url));
11: const tempParent = fs.realpathSync(os.tmpdir());
12: const temp = fs.mkdtempSync(path.join(tempParent, "m001-gates-"));
13: const failures = [];
14: const put = (file, text) => {
15:   fs.mkdirSync(path.dirname(file), { recursive: true });
16:   fs.writeFileSync(file, text, "utf8");
17: };
18: const exec = (command, args, cwd, env = {}) => spawnSync(command, args, {
19:   cwd, encoding: "utf8", env: { ...process.env, HTML_GATES_BASE: "", ...env },
20: });
21: const git = (cwd, ...args) => {
22:   const result = exec("git", args, cwd);
23:   assert.equal(result.status, 0, result.error?.message || result.stderr);
24:   return result.stdout.trim();
25: };
26: const expectExit = (name, result, expected) => {
27:   try {
28:     assert.equal(result.status, expected, result.error?.message || result.stdout + result.stderr);
29:     console.log(`PASS ${name}`);
30:   } catch (error) {
31:     failures.push(name);
32:     console.error(`FAIL ${name}: ${error.message}`);
33:   }
34: };
35: const run = (file, cwd, args = [], env = {}) => exec(process.execPath, [path.join(root, file), ...args], cwd, env);
36: const bashPath = (file) => process.platform === "win32"
37:   ? "/" + file.replaceAll("\\", "/").replace(":", "") : file;
38: const html = [
39:   "docs/business-system-map-for-operations-v0.1.html",
40:   "docs/platform-governance-map-for-operations-v0.1.html",
41:   "docs/business-enum-quick-reference-v0.1.html",
42: ];
43: const ledger = "docs/prd-audit/business-decision-ledger.md";
44: const baseline = "tools/html-param-tier-baseline.json";
45: 
46: try {
47:   const gates = path.join(temp, "html");
48:   for (const file of html) put(path.join(gates, file), "<p>临时验证资料</p>\n");
49:   put(path.join(gates, ledger), "# 临时验证台账\n");
50:   put(path.join(gates, baseline), '{"entries":[]}\n');
51:   git(gates, "init", "-q", "-b", "main");
52:   git(gates, "config", "user.name", "M001 regression");
53:   git(gates, "config", "user.email", "m001@example.invalid");
54:   git(gates, "config", "core.autocrlf", "false");
55:   git(gates, "config", "core.hooksPath", path.join(temp, "empty-hooks"));
56:   git(gates, "config", "commit.gpgSign", "false");
57:   git(gates, "add", ".");
58:   git(gates, "commit", "-qm", "temporary baseline");
59:   const before = git(gates, "rev-parse", "HEAD");
60:   git(gates, "update-ref", "refs/remotes/origin/main", before);
61:   const ledgerGate = "tools/verify-html-ledger-sync.mjs";
62:   const paramGate = "tools/verify-html-param-tier.mjs";
63:   put(path.join(gates, html[0]), "<p>临时期限 30 天。</p>\n");
64:   for (const file of [ledgerGate, paramGate]) expectExit(`未提交错误 ${file}`, run(file, gates), 1);
65:   git(gates, "add", ".");
66:   git(gates, "commit", "-qm", "temporary first change");
67:   for (const file of [ledgerGate, paramGate]) expectExit(`PR 错误 ${file}`, run(file, gates), 1);
68:   put(path.join(gates, "unrelated.txt"), "第二个临时提交\n");
69:   git(gates, "add", ".");
70:   git(gates, "commit", "-qm", "temporary second change");
71:   git(gates, "update-ref", "refs/remotes/origin/main", git(gates, "rev-parse", "HEAD"));
72:   for (const file of [ledgerGate, paramGate]) {
73:     expectExit(`push 多提交 ${file}`, run(file, gates, [], { HTML_GATES_BASE: before }), 1);
74:     expectExit(`明确基线 ${file}`, run(file, gates, [before]), 1);
75:     expectExit(`缺失基线 ${file}`, run(file, gates, [], { HTML_GATES_BASE: "absent-ref" }), 1);
76:   }
77:   for (const [name, body, expected] of [
78:     ["空豁免", "临时期限 30 天 <!-- param-tier:ignore -->", 1],
79:     ["部分豁免", "临时期限 30 天，每期 3 次 <!-- param-tier:ignore 30 天 -->", 1],
80:     ["明确豁免", "临时期限 30 天，每期 3 次 <!-- param-tier:ignore 30 天;3 次 -->", 0],
81:     ["否定词", "临时期限 30 天，该天数不可配。", 1],
82:     ["法定层级", "临时期限 30 天（法定值，不可配）。", 0],
83:     ["业务层级", "临时期限 30 天（业务参数，按项目可配）。", 0],
84:   ]) {
85:     put(path.join(gates, html[0]), `<p>${body}</p>\n`);
86:     expectExit(name, run(paramGate, gates, [before]), expected);
87:   }
88:   put(path.join(gates, ledger), "# 临时验证台账\n已记录本次变更。\n");
89:   expectExit("台账已同步", run(ledgerGate, gates, [before]), 0);
90:   put(path.join(gates, html[0]), "<p>临时验证资料</p>\n");
91:   put(path.join(gates, baseline), '{"entries":[{"key":"new-key","file":"temporary","text":"temporary"}]}\n');
92:   expectExit("基线新增 key", run(paramGate, gates, [before]), 1);
93:   put(path.join(gates, baseline), '{"entries":[]}\n');
94:   expectExit("基线保持不变", run(paramGate, gates, [before]), 0);
95:   git(gates, "add", ".");
96:   git(gates, "commit", "-qm", "temporary deployment baseline");
97:   put(path.join(gates, html[0]), "<p>临时期限 60 天。</p>\n");
98:   git(gates, "add", ".");
99:   git(gates, "commit", "-qm", "temporary deployment change");
100:   git(gates, "update-ref", "refs/remotes/origin/main", git(gates, "rev-parse", "HEAD"));
101:   for (const deploy of ["ops/160/complex-deploy-api.sh", "ops/160/complex-deploy-web.sh"]) {
102:     const assignment = fs.readFileSync(path.join(root, deploy), "utf8").match(/^export HTML_GATES_BASE=.*$/m)?.[0] ?? ":";
103:     for (const file of [ledgerGate, paramGate]) {
104:       expectExit(`部署主线比较 ${deploy} ${file}`, exec("bash", ["--noprofile", "--norc", "-c",
105:         assignment + '\nexec "$1" "$2"', "m001", bashPath(process.execPath), bashPath(path.join(root, file))], gates), 1);
106:     }
107:   }
108: 
109:   const csv = path.join(temp, "csv");
110:   for (const file of html) put(path.join(csv, file), "<p>临时条目正文,后续内容</p>\n");
111:   const csvInputs = {
112:     "docs/prd-audit/entry-registry.csv": 'html_file,entry_id,entry_name,entry_body,line_now\nbusiness-system-map-for-operations-v0.1.html,T001,临时条目,"临时条目正文,\n后续内容",1\n',
113:     "docs/prd-audit/authority-rule-ledger.csv": "rule_id,html_file,entry_name,source_text\nR001,business-system-map-for-operations-v0.1.html,临时条目,临时条目正文\n",
114:     "docs/prd-audit/html-prd-coverage.csv": "rule_id,entry_name\nR001,临时条目\n",
115:   };
116:   for (const [file, text] of Object.entries(csvInputs)) put(path.join(csv, file), text);
117:   const anchors = "tools/verify-entry-anchors.mjs";
118:   expectExit("CSV 引号逗号及跨行字段", run(anchors, csv), 0);
119:   for (const [file, text] of Object.entries(csvInputs)) {
120:     put(path.join(csv, file), text + "损坏记录,多一列,多一列,多一列,多一列,多一列\n");
121:     expectExit(`CSV 列数错误 ${file}`, run(anchors, csv), 1);
122:     put(path.join(csv, file), text + '损坏记录,"未闭合引号');
123:     expectExit(`CSV 未闭合引号 ${file}`, run(anchors, csv), 1);
124:     put(path.join(csv, file), "\ufeff" + text.replaceAll("\n", "\r\n"));
125:     expectExit(`CSV BOM/CRLF ${file}`, run(anchors, csv), 0);
126:     put(path.join(csv, file), text);
127:   }
128: 
129:   const delivery = path.join(temp, "delivery");
130:   git(temp, "clone", "--quiet", "--no-hardlinks", "-c", "core.autocrlf=false", "-c", "core.longpaths=true", root, delivery);
131:   const workflow = ".github/workflows/predeploy-gates.yml";
132:   const mirror = "ops/ci/predeploy-gates.yml";
133:   const validWorkflow = fs.readFileSync(path.join(root, workflow), "utf8");
134:   for (const file of [workflow, mirror]) put(path.join(delivery, file), validWorkflow);
135:   const deployScripts = ["ops/160/complex-deploy-api.sh", "ops/160/complex-deploy-web.sh"];
136:   for (const file of deployScripts) put(path.join(delivery, file), fs.readFileSync(path.join(root, file), "utf8"));
137:   const deliveryGate = "tools/verify-delivery-gates.mjs";
138:   expectExit("一致的 CI 定义", run(deliveryGate, delivery), 0);
139:   put(path.join(delivery, mirror), validWorkflow.replace("fetch-depth: 0", "fetch-depth: 1"));
140:   expectExit("CI 镜像分叉", run(deliveryGate, delivery), 1);
141:   put(path.join(delivery, workflow), validWorkflow.replace("fetch-depth: 0", "fetch-depth: 1"));
142:   expectExit("两份 CI 都缺完整历史", run(deliveryGate, delivery), 1);
143:   for (const file of [workflow, mirror]) put(path.join(delivery, file), validWorkflow);
144:   for (const [name, broken] of [
145:     ["CI 删除事件基线", validWorkflow.replace(/^.*HTML_GATES_BASE:.*\n/gm, "")],
146:     ["CI 注释不能代替完整历史", validWorkflow.replace("fetch-depth: 0", "fetch-depth: 1 # was fetch-depth: 0")],
147:     ["CI 未执行预部署检查", validWorkflow.replace("run: npm run verify:predeploy", "run: echo skipped")],
148:   ]) {
149:     for (const file of [workflow, mirror]) put(path.join(delivery, file), broken);
150:     expectExit(name, run(deliveryGate, delivery), 1);
151:   }
152:   for (const file of [workflow, mirror]) put(path.join(delivery, file), validWorkflow);
153:   for (const file of deployScripts) {
154:     const validScript = fs.readFileSync(path.join(root, file), "utf8");
155:     for (const gate of ["verify:html-ledger-sync", "verify:entry-anchors", "verify:html-param-tier"]) {
156:       put(path.join(delivery, file), validScript.replace(gate, ""));
157:       expectExit(`部署缺少 ${gate} ${file}`, run(deliveryGate, delivery), 1);
158:     }
159:     put(path.join(delivery, file), validScript);
160:   }
161:   const pkg = JSON.parse(fs.readFileSync(path.join(delivery, "package.json"), "utf8"));
162:   pkg.scripts["verify:predeploy"] = "true";
163:   put(path.join(delivery, "package.json"), JSON.stringify(pkg));
164:   expectExit("检查入口缺少 HTML 门禁", run(deliveryGate, delivery), 1);
165: 
166:   const merge = path.join(temp, "merge");
167:   const bin = path.join(merge, "bin");
168:   put(path.join(bin, "git"), `#!/usr/bin/env bash
169: set -eu
170: case "$*" in
171:  'rev-parse --abbrev-ref HEAD') echo feature/m001;;
172:  'rev-parse HEAD') echo aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;;
173:  'status --porcelain') :;;
174:  'ls-remote origin refs/heads/feature/m001')
175:   if [[ -f advanced ]]; then value=bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb; else value=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa; fi
176:   printf '%s\\trefs/heads/feature/m001\\n' "$value";;
177:  *) echo "unexpected fake git: $*" >&2; exit 97;;
178: esac
179: `);
180:   put(path.join(bin, "gh"), `#!/usr/bin/env bash
181: set -eu
182: case "$1 $2" in
183:  'pr view')
184:   if [[ "$*" == *headRefOid* ]]; then if [[ -f advanced ]]; then echo bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb; else echo aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa; fi; fi;;
185:  'run list') echo 101;;
186:  'run watch') if [[ "$M001_MODE" == watch ]]; then touch advanced; fi;;
187:  'pr merge')
188:   if [[ "$M001_MODE" == late ]]; then touch advanced; fi
189:   expected=''
190:   while [[ $# -gt 0 ]]; do if [[ "$1" == --match-head-commit ]]; then shift; expected="$1"; fi; shift; done
191:   if [[ -f advanced && "$expected" != bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb && -n "$expected" ]]; then exit 1; fi
192:   touch merged;;
193:  *) echo "unexpected fake gh: $*" >&2; exit 98;;
194: esac
195: exit 0
196: `);
197:   for (const file of ["git", "gh"]) fs.chmodSync(path.join(bin, file), 0o755);
198:   for (const [mode, expected] of [["stable", 0], ["watch", 1], ["late", 1]]) {
199:     for (const file of ["advanced", "merged"]) {
200:       const target = path.join(merge, file);
201:       if (fs.existsSync(target)) fs.unlinkSync(target);
202:     }
203:     const result = exec("bash", ["--noprofile", "--norc", "-c",
204:       'export PATH="$1:/usr/bin:/bin"; exec bash "$2"', "m001",
205:       bashPath(bin), bashPath(path.join(root, "ops/160/merge-pr.sh"))], merge, { M001_MODE: mode });
206:     expectExit(`合并提交 ${mode}`, result, expected);
207:     if (fs.existsSync(path.join(merge, "merged")) !== (expected === 0)) failures.push(`合并副作用 ${mode}`);
208:   }
209:   assert.deepEqual(failures, [], `治理回归失败：${failures.join("、")}`);
210:   console.log("M001_GOVERNANCE_REGRESSION_PASS");
211: } finally {
212:   const resolved = fs.realpathSync(temp);
213:   assert.equal(path.dirname(resolved), tempParent);
214:   assert.ok(path.basename(resolved).startsWith("m001-gates-"));
215:   fs.rmSync(resolved, { recursive: true });
216: }
~~~~

## 7. 实际执行输出

### 当前入口与现有回归完整输出
~~~~text
CMD ["node", "--check", "tools/refresh-entry-doc-baseline.mjs"] EXIT 0


CMD ["node", "--check", "tools/verify-delivery-gates.mjs"] EXIT 0


CMD ["node", "--check", "tools/verify-entry-anchors.mjs"] EXIT 0


CMD ["node", "--check", "tools/verify-html-ledger-sync.mjs"] EXIT 0


CMD ["node", "--check", "tools/verify-html-param-tier.mjs"] EXIT 0


CMD ["node", "--check", "tools/gate-hardening-tests/m001-regression.mjs"] EXIT 0


CMD ["node", "tools/refresh-entry-doc-baseline.mjs", "--check"] EXIT 1

[entry-doc-baseline] DRIFT: README.md 快照落后于实际（应为 720 ops / V399（377 迁移） / 55 tests）。运行 node tools/refresh-entry-doc-baseline.mjs 回填。

CMD ["node", "tools/verify-delivery-gates.mjs"] EXIT 0
DELIVERY_GATES_DOCS_PASS


CMD ["node", "tools/verify-entry-anchors.mjs"] EXIT 1
[entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1565 条；被检 Markdown 360 份；条目名引用 1517 处（注册表原名 1358 处，正文唯一定位 159 处）。

[entry-anchors] FAIL：6 处不合格。
  - 注册表条目正文在 HTML 现文中找不到：BSM-E0433 BSM「处理后回访客人或商户，验收合格才关闭」——HTML 已改而注册表未重建
  - 注册表条目正文在 HTML 现文中找不到：BSM-E0736 BSM「文档版本」——HTML 已改而注册表未重建
  - 账本正文与 HTML 现文对不上：BSM-0283「处理后回访客人或商户，验收合格才关闭」——HTML 已改而账本 source_text 未重取
  - 账本正文与 HTML 现文对不上：BSM-0530「文档版本」——HTML 已改而账本 source_text 未重取
  - 账本镜像列与 HTML 现文对不上：BSM-0283「处理后回访客人或商户，验收合格才关闭」preconditions 片段「处理后回访客人或商户，验收合格才关闭。」——HTML 已改而账本未重取（检查⑦）
  - 账本镜像列与 HTML 现文对不上：BSM-0283「处理后回访客人或商户，验收合格才关闭」state_transition 片段「处理后回访客人或商户，验收合格才关闭。」——HTML 已改而账本未重取（检查⑦）
  处置：条目改名或 HTML 改动后，重建 docs/prd-audit/entry-registry.csv 并把引用改为现行条目名；一律不得写回行号。

CMD ["node", "tools/verify-html-ledger-sync.mjs"] EXIT 1

[html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。比对范围：工作区（含未提交改动）↔ 基线 origin/main@242f4872。
  已改动的 HTML：
    - docs/business-system-map-for-operations-v0.1.html
  必须在同一批改动内一并更新：docs/prd-audit/business-decision-ledger.md
  依据：用户 2026-08-03 拍板——台账保持完整，改 HTML 必须同 PR 同步台账。
  若本次确为纯排版/错别字、不涉及口径，请在台账内追加一行说明后再提交。

CMD ["node", "tools/verify-html-param-tier.mjs"] EXIT 0
[html-param-tier] PASS: 改动了 1 份业务 HTML，全量重扫无基线外的未标层级句（存量 0 处已登记放行）。比对范围：工作区（含未提交改动）↔ 基线 origin/main@242f4872；扫描口径：改动文件全文，已剥除 <style> 与注释。


CMD ["node", "tools/gate-hardening-tests/m001-regression.mjs"] EXIT 0
PASS 未提交错误 tools/verify-html-ledger-sync.mjs
PASS 未提交错误 tools/verify-html-param-tier.mjs
PASS PR 错误 tools/verify-html-ledger-sync.mjs
PASS PR 错误 tools/verify-html-param-tier.mjs
PASS push 多提交 tools/verify-html-ledger-sync.mjs
PASS 明确基线 tools/verify-html-ledger-sync.mjs
PASS 缺失基线 tools/verify-html-ledger-sync.mjs
PASS push 多提交 tools/verify-html-param-tier.mjs
PASS 明确基线 tools/verify-html-param-tier.mjs
PASS 缺失基线 tools/verify-html-param-tier.mjs
PASS 空豁免
PASS 部分豁免
PASS 明确豁免
PASS 否定词
PASS 法定层级
PASS 业务层级
PASS 台账已同步
PASS 基线新增 key
PASS 基线保持不变
PASS 部署主线比较 ops/160/complex-deploy-api.sh tools/verify-html-ledger-sync.mjs
PASS 部署主线比较 ops/160/complex-deploy-api.sh tools/verify-html-param-tier.mjs
PASS 部署主线比较 ops/160/complex-deploy-web.sh tools/verify-html-ledger-sync.mjs
PASS 部署主线比较 ops/160/complex-deploy-web.sh tools/verify-html-param-tier.mjs
PASS CSV 引号逗号及跨行字段
PASS CSV 列数错误 docs/prd-audit/entry-registry.csv
PASS CSV 未闭合引号 docs/prd-audit/entry-registry.csv
PASS CSV BOM/CRLF docs/prd-audit/entry-registry.csv
PASS CSV 列数错误 docs/prd-audit/authority-rule-ledger.csv
PASS CSV 未闭合引号 docs/prd-audit/authority-rule-ledger.csv
PASS CSV BOM/CRLF docs/prd-audit/authority-rule-ledger.csv
PASS CSV 列数错误 docs/prd-audit/html-prd-coverage.csv
PASS CSV 未闭合引号 docs/prd-audit/html-prd-coverage.csv
PASS CSV BOM/CRLF docs/prd-audit/html-prd-coverage.csv
PASS 一致的 CI 定义
PASS CI 镜像分叉
PASS 两份 CI 都缺完整历史
PASS CI 删除事件基线
PASS CI 注释不能代替完整历史
PASS CI 未执行预部署检查
PASS 部署缺少 verify:html-ledger-sync ops/160/complex-deploy-api.sh
PASS 部署缺少 verify:entry-anchors ops/160/complex-deploy-api.sh
PASS 部署缺少 verify:html-param-tier ops/160/complex-deploy-api.sh
PASS 部署缺少 verify:html-ledger-sync ops/160/complex-deploy-web.sh
PASS 部署缺少 verify:entry-anchors ops/160/complex-deploy-web.sh
PASS 部署缺少 verify:html-param-tier ops/160/complex-deploy-web.sh
PASS 检查入口缺少 HTML 门禁
PASS 合并提交 stable
PASS 合并提交 watch
PASS 合并提交 late
M001_GOVERNANCE_REGRESSION_PASS



~~~~

### 独立生成/门禁/CSV/删除/排版反例
~~~~text
CASE refresh-before-check EXIT 1

[entry-doc-baseline] DRIFT: docs/00-current-effective-document-entry-v0.1.md + README.md 快照落后于实际（应为 2 ops / V12（1 迁移））。运行 node tools/refresh-entry-doc-baseline.mjs 回填。

CASE refresh-write-isolated EXIT 0
[entry-doc-baseline] 已回填 docs/00-current-effective-document-entry-v0.1.md + README.md：2 ops / V12（1 迁移）（2026-09-09）。


CASE refresh-after-check EXIT 0
[entry-doc-baseline] PASS: 快照与实际一致（2 ops / V12（1 迁移））。


GENERATED_ENTRY '# 入口\n版本：V0.03\n本轮（2026-09-09）核验为 2 operations\n历史 2019-01-01 99 operations\n'
GENERATED_README '# README\n版本：V0.08\n<!-- baseline:auto-begin -->\n| 指标 | 当前值 | 取证方式 |\n|---|---|---|\n| Flyway 最新迁移 | **V12**（1 个迁移） | `backend/src/main/resources/db/migration` 实时统计 |\n| OpenAPI operation 数 | **2** | `docs/openapi/openapi-p0.json` 方法统计（与 `npm run verify:operation-matrix` 同口径） |\n| 后端测试 | **99** tests（现有 surefire 报告） | `backend/target/surefire-reports` 合计；无报告时保留上次快照，不声明刚完成全量测试 |\n| 前端 E2E | 以 `npx playwright test --list` 实时输出为准 | 不写死快照数 |\n<!-- baseline:auto-end -->\n'
CASE delivery-clean-copy EXIT 0
DELIVERY_GATES_DOCS_PASS


CASE delivery-html-gate-echo-not-executed EXIT 0
DELIVERY_GATES_DOCS_PASS


CASE anchors-clean EXIT 0
[entry-anchors] 注册表 1 行；账本 1 行；承接矩阵 1 行（同名 1、账本中无此 rule_id 0）；账本镜像列片段 0 条；被检 Markdown 0 份；条目名引用 0 处（注册表原名 0 处，正文唯一定位 0 处）。
[entry-anchors] PASS：条目名唯一、注册表条目正文逐字与 HTML 现文一致、引用可唯一定位、正文无行号锚点、账本正文与九个镜像列均与 HTML 现文一致、账本与承接矩阵一一对应。


CASE anchors-empty-entry-body EXIT 0
[entry-anchors] 注册表 1 行；账本 1 行；承接矩阵 1 行（同名 1、账本中无此 rule_id 0）；账本镜像列片段 0 条；被检 Markdown 0 份；条目名引用 0 处（注册表原名 0 处，正文唯一定位 0 处）。
[entry-anchors] PASS：条目名唯一、注册表条目正文逐字与 HTML 现文一致、引用可唯一定位、正文无行号锚点、账本正文与九个镜像列均与 HTML 现文一致、账本与承接矩阵一一对应。


CASE param-valid-one-line EXIT 0
[html-param-tier] PASS: 改动了 1 份业务 HTML，全量重扫无基线外的未标层级句（存量 0 处已登记放行）。比对范围：工作区（含未提交改动）↔ 基线 origin/main@c93b699c；扫描口径：改动文件全文，已剥除 <style> 与注释。


CASE ledger-changed-html-only EXIT 1

[html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。比对范围：工作区（含未提交改动）↔ 基线 origin/main@c93b699c。
  已改动的 HTML：
    - docs/business-system-map-for-operations-v0.1.html
  必须在同一批改动内一并更新：docs/prd-audit/business-decision-ledger.md
  依据：用户 2026-08-03 拍板——台账保持完整，改 HTML 必须同 PR 同步台账。
  若本次确为纯排版/错别字、不涉及口径，请在台账内追加一行说明后再提交。

CASE ledger-deleted-is-not-updated EXIT 0
[html-ledger-sync] PASS: 改动了 1 份业务 HTML，决策台账已同步。比对范围：工作区（含未提交改动）↔ 基线 origin/main@c93b699c。


CASE param-valid-same-sentence-newline EXIT 1

[html-param-tier] FAIL: 1 处带具体数值的语句未标出所属层级，且不在存量基线内。
  比对范围：工作区（含未提交改动）↔ 基线 origin/main@c93b699c；扫描口径：改动文件全文，已剥除 <style> 与注释。
  三层写法：（法定值，不可配）／（安全阈值，按X可配，调整须最高权限审批并留痕）／（业务参数，按X可配，出厂默认 N）
  - docs/business-system-map-for-operations-v0.1.html（未标层级的数值：30天）
      测试期限 30 天
  确为法条编号、举例日期等非参数表述：在该行内加 <!-- param-tier:ignore 数值1;数值2 --> 只豁免这些数值。
  存量待补标的老句子在 tools/html-param-tier-baseline.json——那份只许缩不许增，新句子不得往里补登记。

CASE param-unmarked-second-paragraph-same-line EXIT 0
[html-param-tier] PASS: 改动了 1 份业务 HTML，全量重扫无基线外的未标层级句（存量 0 处已登记放行）。比对范围：工作区（含未提交改动）↔ 基线 origin/main@c93b699c；扫描口径：改动文件全文，已剥除 <style> 与注释。


ISOLATED_FIXTURES_COMPLETE; no canonical source/HTML write

~~~~

### 重复检查反例
~~~~text
MISSING_COMMAND_EXIT 1
DELIVERY_GATES_DOCS_FAIL
- verify:delivery-gates must include verify:production-evidence-positive-propagation-fixtures
- verify:delivery-gates must include verify:production-evidence-positive-propagation-fixtures
- verify:delivery-gates must include verify:production-evidence-positive-propagation-fixtures

IDENTICAL_ERROR_COUNT 3

~~~~

### 基线参数反例
~~~~text
BASE_ARG tools/verify-html-ledger-sync.mjs 'absent-ref' EXIT 1

fatal: Not a valid object name absent-ref
[html-ledger-sync] FAIL: 取不到基线 absent-ref，无法判定本次改动范围。
  无法判定不等于通过。请先 git fetch origin（CI 侧 actions/checkout 需 fetch-depth: 0）。
  如需换基线：node tools/verify-html-ledger-sync.mjs <基线ref>

BASE_ARG tools/verify-html-ledger-sync.mjs 'HEAD;echo M008_BASE_ARG' EXIT 0
[html-ledger-sync] PASS: 本次未改动业务最终解释权 HTML。比对范围：提交区 HEAD;echo M008_BASE_ARG...HEAD（工作区三份 HTML 无未提交改动）。

usage: git merge-base [-a | --all] <commit> <commit>...
   or: git merge-base [-a | --all] --octopus <commit>...
   or: git merge-base --is-ancestor <commit> <commit>
   or: git merge-base --independent <commit>...
   or: git merge-base --fork-point <ref> [<commit>]

    -a, --[no-]all        output all common ancestors
    --octopus             find ancestors for a single n-way merge
    --independent         list revs not reachable from others
    --is-ancestor         is the first one ancestor of the other?
    --fork-point          find where <commit> forked from reflog of <ref>


BASE_ARG tools/verify-html-param-tier.mjs 'absent-ref' EXIT 1

fatal: Not a valid object name absent-ref
[html-param-tier] FAIL: 取不到基线 absent-ref，无法判定本次改动范围。
  无法判定不等于通过。请先 git fetch origin（CI 侧 actions/checkout 需 fetch-depth: 0）。
  如需换基线：node tools/verify-html-param-tier.mjs <基线ref>

BASE_ARG tools/verify-html-param-tier.mjs 'HEAD;echo M008_BASE_ARG' EXIT 0
[html-param-tier] PASS: 本次未改动业务最终解释权 HTML。比对范围：提交区 HEAD;echo M008_BASE_ARG...HEAD（工作区三份 HTML 无未提交改动）。

usage: git merge-base [-a | --all] <commit> <commit>...
   or: git merge-base [-a | --all] --octopus <commit>...
   or: git merge-base --is-ancestor <commit> <commit>
   or: git merge-base --independent <commit>...
   or: git merge-base --fork-point <ref> [<commit>]

    -a, --[no-]all        output all common ancestors
    --octopus             find ancestors for a single n-way merge
    --independent         list revs not reachable from others
    --is-ancestor         is the first one ancestor of the other?
    --fork-point          find where <commit> forked from reflog of <ref>


Only isolated directory and harmless echo used; no canonical writes

~~~~

### 调用方、数据边界与实际冻结归属
~~~~text
本次rg实际核对package.json:31/35为docs:entry-baseline与verify:entry-doc-baseline；:32–34为三个HTML/entry门禁，:60为delivery-docs。生成器目标README当前V0.02，baseline块101–108，106为862tests；实际本地surefire为55，--check正确报README漂移。entry文档标题V0.02，当前无匹配活跃快照句，历史504行1606tests不应改。当前registry1228行entry_body非历史空值0；ledger1235行source_text非历史空值0。requiredDeliveryCommands重复5类：positive-propagation 3次，其余ready-no-go/blocker-no-go/ready-positive/command-center各2次，共6个额外重复项。实际冻结归属README M435、00-entry M436、entry-registry M427、authority-rule-ledger M425、coverage M428、business-decision-ledger M426。不要再把全部输入文件都标成M425/M427。
~~~~

## 8. 独立反例完整命令载荷
正本根固定；临时目录由TemporaryDirectory清理，不对正本HTML修改。

### 隔离主反例 Python，通过SSH python3 -执行
~~~~python
from pathlib import Path
import subprocess,tempfile,os,json,hashlib,csv,io,shutil,re
r=Path('/home/james/complex-management');scratch=r/'.scratch';scratch.mkdir(exist_ok=True)
html=['docs/business-system-map-for-operations-v0.1.html','docs/platform-governance-map-for-operations-v0.1.html','docs/business-enum-quick-reference-v0.1.html']
def put(root,rel,text):
 p=root/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf-8')
def run(label,args,cwd,env=None):
 p=subprocess.run(args,cwd=cwd,env=env,text=True,capture_output=True)
 print('CASE',label,'EXIT',p.returncode);print(p.stdout);print(p.stderr)
 return p
with tempfile.TemporaryDirectory(prefix='m008-checks-',dir=scratch) as td:
 t=Path(td)
 # 原生生成器：正常漂移/修复/不漂移，以及目标版本是否递增
 g=t/'generator'
 put(g,'docs/openapi/openapi-p0.json',json.dumps({'paths':{'/a':{'get':{},'post':{}}}}))
 put(g,'backend/src/main/resources/db/migration/V12__sample.sql','select 1;\n')
 put(g,'README.md','# README\n版本：V0.08\n<!-- baseline:auto-begin -->\n**99** tests\n旧快照\n<!-- baseline:auto-end -->\n')
 put(g,'docs/00-current-effective-document-entry-v0.1.md','# 入口\n版本：V0.03\n本轮（2020-01-01）核验为 99 operations\n历史 2019-01-01 99 operations\n')
 run('refresh-before-check',['node',str(r/'tools/refresh-entry-doc-baseline.mjs'),'--check'],g)
 run('refresh-write-isolated',['node',str(r/'tools/refresh-entry-doc-baseline.mjs')],g)
 run('refresh-after-check',['node',str(r/'tools/refresh-entry-doc-baseline.mjs'),'--check'],g)
 print('GENERATED_ENTRY',repr((g/'docs/00-current-effective-document-entry-v0.1.md').read_text()))
 print('GENERATED_README',repr((g/'README.md').read_text()))
 # delivery所读最窄文件拷贝，无真实部署
 gate=(r/'tools/verify-delivery-gates.mjs').read_text();dd=t/'delivery';dd.mkdir()
 deps=set(re.findall(r'(?:readText|readJson|requireIncludes)\("([^"]+)"',gate))
 deps.update(['.github/workflows/predeploy-gates.yml','ops/ci/predeploy-gates.yml','ops/160/complex-deploy-api.sh','ops/160/complex-deploy-web.sh'])
 for rel in deps:put(dd,rel,(r/rel).read_text())
 run('delivery-clean-copy',['node',str(r/'tools/verify-delivery-gates.mjs')],dd)
 pkg=json.loads((dd/'package.json').read_text())
 pkg['scripts']['verify:predeploy']=pkg['scripts']['verify:predeploy'].replace('npm run verify:html-param-tier','echo verify:html-param-tier')
 put(dd,'package.json',json.dumps(pkg))
 run('delivery-html-gate-echo-not-executed',['node',str(r/'tools/verify-delivery-gates.mjs')],dd)
 # CSV正文空值仍登记其它同源字段
 a=t/'anchors'
 for h in html:put(a,h,'<p>测试条目正文足够长原文存在</p>\n')
 registry='html_file,entry_id,entry_name,entry_body,line_now\nbusiness-system-map-for-operations-v0.1.html,T001,测试条目,测试条目正文足够长原文存在,1\n'
 put(a,'docs/prd-audit/entry-registry.csv',registry)
 put(a,'docs/prd-audit/authority-rule-ledger.csv','rule_id,html_file,entry_name,source_text\nR001,business-system-map-for-operations-v0.1.html,测试条目,测试条目正文足够长原文存在\n')
 put(a,'docs/prd-audit/html-prd-coverage.csv','rule_id,entry_name\nR001,测试条目\n')
 run('anchors-clean',['node',str(r/'tools/verify-entry-anchors.mjs')],a)
 put(a,'docs/prd-audit/entry-registry.csv',registry.replace('测试条目,测试条目正文足够长原文存在,1','测试条目,,1'))
 run('anchors-empty-entry-body',['node',str(r/'tools/verify-entry-anchors.mjs')],a)
 # 临时Git：删除台账、跨行参数均只改临时资料
 q=t/'git';q.mkdir()
 for h in html:put(q,h,'<p>测试资料</p>\n')
 ledger='docs/prd-audit/business-decision-ledger.md';baseline='tools/html-param-tier-baseline.json'
 put(q,ledger,'# 测试台账\n');put(q,baseline,'{"entries":[]}\n')
 env={**os.environ,'HTML_GATES_BASE':'','GIT_CONFIG_GLOBAL':'/dev/null','GIT_CONFIG_NOSYSTEM':'1'}
 def git(*args):
  p=subprocess.run(['git',*args],cwd=q,env=env,text=True,capture_output=True);assert p.returncode==0,(args,p.stderr);return p.stdout.strip()
 git('init','-q','-b','main');git('config','user.name','M008 check');git('config','user.email','m008@example.invalid');git('config','core.hooksPath',str(t/'empty-hooks'));git('config','commit.gpgSign','false')
 git('add','.');git('commit','-qm','test baseline');git('update-ref','refs/remotes/origin/main',git('rev-parse','HEAD'))
 put(q,html[0],'<p>测试期限 30 天（业务参数，按项目可配）。</p>\n')
 run('param-valid-one-line',['node',str(r/'tools/verify-html-param-tier.mjs')],q,env)
 run('ledger-changed-html-only',['node',str(r/'tools/verify-html-ledger-sync.mjs')],q,env)
 (q/ledger).unlink()
 run('ledger-deleted-is-not-updated',['node',str(r/'tools/verify-html-ledger-sync.mjs')],q,env)
 put(q,html[0],'<p>测试期限 30 天\n（业务参数，按项目可配）。</p>\n')
 run('param-valid-same-sentence-newline',['node',str(r/'tools/verify-html-param-tier.mjs')],q,env)
 put(q,html[0],'<p>测试期限 30 天（业务参数，按项目可配）。</p><p>另一事项每期最多 3 次。</p>\n')
 run('param-unmarked-second-paragraph-same-line',['node',str(r/'tools/verify-html-param-tier.mjs')],q,env)
print('ISOLATED_FIXTURES_COMPLETE; no canonical source/HTML write')

~~~~

### 基线参数 Python
~~~~python
from pathlib import Path
import subprocess,tempfile,os
r=Path('/home/james/complex-management')
with tempfile.TemporaryDirectory(prefix='m008-basearg-',dir=r/'.scratch') as td:
 q=Path(td);env={**os.environ,'HTML_GATES_BASE':'','GIT_CONFIG_GLOBAL':'/dev/null','GIT_CONFIG_NOSYSTEM':'1'}
 for rel,body in [('docs/business-system-map-for-operations-v0.1.html','<p>测试资料</p>'),('docs/platform-governance-map-for-operations-v0.1.html','<p>测试资料</p>'),('docs/business-enum-quick-reference-v0.1.html','<p>测试资料</p>'),('docs/prd-audit/business-decision-ledger.md','# 测试台账'),('tools/html-param-tier-baseline.json','{"entries":[]}')]:
  p=q/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body+'\n',encoding='utf-8')
 for args in [['init','-q','-b','main'],['config','user.name','M008'],['config','user.email','m008@example.invalid'],['config','core.hooksPath',str(q/'empty-hooks')],['config','commit.gpgSign','false'],['add','.'],['commit','-qm','test baseline']]:
  p=subprocess.run(['git',*args],cwd=q,env=env,text=True,capture_output=True);assert p.returncode==0,p.stderr
 for rel in ['tools/verify-html-ledger-sync.mjs','tools/verify-html-param-tier.mjs']:
  for base in ['absent-ref','HEAD;echo M008_BASE_ARG']:
   p=subprocess.run(['node',str(r/rel),base],cwd=q,env=env,text=True,capture_output=True)
   print('BASE_ARG',rel,repr(base),'EXIT',p.returncode);print(p.stdout);print(p.stderr[:1000])
print('Only isolated directory and harmless echo used; no canonical writes')

~~~~

### 重复项 Python
~~~~python
from pathlib import Path
import tempfile,re,json,subprocess
r=Path('/home/james/complex-management')
gate=(r/'tools/verify-delivery-gates.mjs').read_text()
with tempfile.TemporaryDirectory(prefix='m008-duplicate-',dir=r/'.scratch') as td:
 d=Path(td)
 deps=set(re.findall(r'(?:readText|readJson|requireIncludes)\("([^"]+)"',gate))
 deps.update(['.github/workflows/predeploy-gates.yml','ops/ci/predeploy-gates.yml','ops/160/complex-deploy-api.sh','ops/160/complex-deploy-web.sh'])
 for rel in deps:
  p=d/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((r/rel).read_bytes())
 pkg=json.loads((d/'package.json').read_text());needle='verify:production-evidence-positive-propagation-fixtures'
 pkg['scripts']['verify:delivery-gates']=pkg['scripts']['verify:delivery-gates'].replace('npm run '+needle,'true')
 (d/'package.json').write_text(json.dumps(pkg),encoding='utf-8')
 p=subprocess.run(['node',str(r/'tools/verify-delivery-gates.mjs')],cwd=d,text=True,capture_output=True)
 print('MISSING_COMMAND_EXIT',p.returncode);print(p.stderr)
 print('IDENTICAL_ERROR_COUNT',p.stderr.count('verify:delivery-gates must include '+needle))
 assert p.returncode==1 and p.stderr.count('verify:delivery-gates must include '+needle)==3

~~~~

## 9. 本次已读冻结定义与HTML核对
### GEN-001 — 必要性与最小性

- 来源：通用工程判据
- 判据：每项实现都须有可验证用途；重复、不可达、纯转发或无需求承载的内容应删除或收窄，不为假设场景增加抽象。

### GEN-002 — 可执行正确性

- 来源：通用工程判据
- 判据：声明的入口、主路径和错误路径须能构建或执行，并与其接口、断言和文档声称的行为一致。

### GEN-003 — 测试有效性

- 来源：通用工程判据
- 判据：测试须实际执行目标行为、具备能失败的断言，并覆盖本模块关键分支；测试通过本身不替代行为证据。

### GEN-004 — 信任边界校验

- 来源：通用工程判据
- 判据：来自用户、网络、文件、环境变量、数据库或第三方的输入须在首次可信使用前完成类型、范围、格式和所有权校验。

### GEN-005 — 授权与最小权限

- 来源：通用工程判据
- 判据：受保护操作须在服务端按主体、对象和动作授权；默认拒绝，避免仅靠界面隐藏、调用方约定或可伪造字段。

### GEN-006 — 数据完整性

- 来源：通用工程判据
- 判据：状态转换、唯一性、引用关系和金额数量须由事务、约束或等价原子机制保护，失败不得留下部分写入。

### GEN-007 — 并发与幂等

- 来源：通用工程判据
- 判据：可重试、重复投递或并发执行的路径须保持幂等和一致；锁、去重键及事务边界须覆盖真实竞争窗口。

### GEN-008 — 接口与模式一致性

- 来源：通用工程判据
- 判据：前端、后端、数据库、OpenAPI、迁移、DTO 和测试夹具对名称、类型、可空性、枚举及错误语义的约定须一致。

### GEN-009 — 错误处理与恢复

- 来源：通用工程判据
- 判据：错误须可检测、可定位并安全失败；不得吞错、伪成功、泄漏半成品或用不相关默认值掩盖失败。

### GEN-010 — 安全与敏感信息

- 来源：通用工程判据
- 判据：代码和配置不得引入注入、越权、路径穿越、弱加密、明文秘密或敏感信息泄露；日志与错误响应执行最小披露。

### GEN-011 — 可观测与审计证据

- 来源：通用工程判据
- 判据：关键操作须留下可关联、可复现的结构化证据；日志、指标和审计记录不得相互矛盾或依赖不可验证描述。

### GEN-012 — 资源与性能边界

- 来源：通用工程判据
- 判据：循环、查询、批处理、缓存、连接、线程和文件句柄须有与实际负载相称的边界并正确释放，不制造无界放大。

### GEN-013 — 构建配置与依赖

- 来源：通用工程判据
- 判据：依赖、脚本、环境配置、CI 与部署入口须可复现、版本相容且失败显式；不得依赖未声明的本机状态。

### GEN-014 — 数据库迁移正确性

- 来源：通用工程判据
- 判据：迁移须顺序确定、可在目标数据库执行并保持模式与代码一致；破坏性动作须受明确业务授权约束。

### GEN-015 — 界面可用性与可访问性

- 来源：通用工程判据
- 判据：用户界面须保持键盘、焦点、标签、反馈和可读性基本可用，且展示文案与项目声明的语言约定一致。

### GEN-016 — 文档可验证性

- 来源：通用工程判据
- 判据：文档中的路径、命令、链接、版本、状态和完成声明须可从当前纳入范围复现，不得把计划或旧证据写成当前事实。

### GEN-017 — 业务来源分类

- 来源：通用工程判据
- 判据：每项内容按冲突→细化→技术性→添加顺序分类；不得把业务发明伪装成技术实现，也不得因 HTML 沉默直接删除。

### GEN-018 — 第一性原理证据

- 来源：通用工程判据
- 判据：审计结论须落到 HTML 原文、可执行产物、外部规范原文或可复现命令输出之一；惯例、注释、历史结论或席位意见本身不是依据。

### PGM-036 — 设备管理：谁领了什么设备，设备能不能用

- 来源：`docs/platform-governance-map-for-operations-v0.1.html:611`；锚点：`h2`
- 适用标签：access、entry、gate、terminal、ticketing
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
          <h2>设备管理：谁领了什么设备，设备能不能用</h2>
          <p class="lead">设备包括工作手机、平板、一体机、扫码设备、综合体入场三道环节设备（安检环节的 X 光设备、年龄核验环节的人脸扫描设备、检票环节的检票闸机）和其他现场设备。设备要绑定员工、班次、点位和状态。</p>
          <div class="detail-grid">
            <div class="info-block">
````


## 10. 最终结论
FAIL。本席完整17文件/1530行、19冻结判据三线已完成，6项缺陷候选及4项观察全部收齐。当前没有正式修复ID、不修源码；本报告完整落盘后才允许读取Grok本轮报告并做B1四值校验及整批B2归并。


## M008 B2-r1 Codex独立归并复核票
文件自身版本：V0.02（原V0.01完整143625bytes前缀保留，本段追加）
本席本批结论：PASS；仅为归并/分类/队列复核票，未与Grok合取前不得定稿或修复。
输入：modules/M008-b1-self-raw.md 原V0.01 143625bytes SHA98dcb89462c00b3065764642043f61d08da478d742a7b99961bd9369b9aa27a3；modules/M008-b1-grok-raw.md 原V0.01 21297bytes SHA8dd0fb2ef8b803c49b745f67e2852afe1f5109dec0271319b26112f0690f5af3。候选findings.md V1.10，379232bytes SHAd1b4c899c1771290e18a981639e187f072d118cbfed4df4c456698895bee8f35；当前M008共001–016。本席未读Grok本轮B2追加票，独立写下此结论。

| ID | 本席票及具体依据 |
|---|---|
| M008-001 | PASS。已有门禁的实际命令消失，仅echo同名仍通过，原隔离正反例成立。修已有命令识别即可，不创造门禁集合。 |
| M008-002 | PASS。空正文不得被声称全文一致，真实注册表无非历史空正文，拒绝缺失属现有校验职责。 |
| M008-003 | PASS。删除台账与“同批台账已同步”直接相反；保留文件存在是既有前提。与任意加一行是否详细描述口径不同，不扩语义审查。 |
| M008-004 | PASS。同一业务句只换物理行就由0变1；两个独立p同一行由前段标注掩盖后段，无需多重偶发。修读取句/块边界，保留数值豁免及业务原意。 |
| M008-005 | PASS（本席意见）。6项重复常量无独立用途，实际缺失同一命令会重复报告3次；依GEN001/011只删除重复可成立。Grok B1认为幂等无缺陷的观点完整保留，若其本批仍否决则不得靠本票强行修。 |
| M008-006 | PASS。两席发现同一根因：基线被当shell源码，具体探针后续退出不同不矛盾。普通非法ref应拒绝，HEAD;echo实测变成PASS。两脚本所有Git调用包括show实际均走execSync字符串，适合一次改为参数数组，无需新校验框架。 |
| M008-007 | PASS。Grok发现当前126活跃句未维护是真缺陷，本席收报后复制真实资料重现write绿/check绿但126旧。此前本方CX07的合成正常样例不足，明确纠正遗漏；不伪装为原B1已发现，不改写原raw。 |
| M008-008 | PASS。仅登记生成器其它正常用途及输入资料漂移；把真实README从862到55差异写入007/009上下文不代表应本轮直接回填其它模块。用户自身版本规则由实际编辑执行，不加自动版本新功能。 |
| M008-009 | PASS。6处真实失配正确失败、关联测试首次断言失败均如实保留。按实际计划registry M427/authority M425/coverage M428/decision M426/README M435/entry M436处理，不能本模块扩修。 |
| M008-010 | PASS。10历史日志不能改写为当前证据。裸豁免、6条基线和旧完整链绿均有明确历史上下文；未证明当前引用冒充，不删除。 |
| M008-011 | PASS。49条现有回归实际通过；覆盖不含全部新问题不应单独发明测试工程，B3可为正式是项落最小失败测试并归因。 |
| M008-012 | PASS。文件级同步、未变HTML跳过是既有职责；Grok全3HTML扫描与本方仅当前BSM扫描边界分开，不能夸大本方范围。 |
| M008-013 | PASS。-report理论宽排除没有本轮正常首跑实际误放证据，仅登记。 |
| M008-014 | PASS。内部CLI不涉及设备或产品UI，不将B/C业务确认扩为CLI批准，不顺手翻译。 |
| M008-015 | PASS。工程UTC时间与本地日不同本身不违反业务营业日判据，不加时区参数。 |
| M008-016 | PASS。PGM036原句是物理设备绑定要求，本模块文档入口/CI检查无此实现面；全部17文件无业务枚举/流程发明，技术性成立。 |

差集检查：CX01–06分别001–006；CX07→007/008、08→009、09→010、10→011；GR001→007、GR002→006；Grok观察1→005、2/3→012、4/5/6→010、7→013、8/10→011、9→014、11→015、12→016，16ID唯一连续，无遗漏，无冲突/添加/未答项。当前候选是001–007，否008–016。所有依据均来自已有GEN定义或PGM036适用判断，没有新增判据/门/状态/角色。
Grok误主仓--write-baseline同字节写入与本方原报告“17文件未改”应区分动作与内容：内容hash相同，确有误写尝试。保留原自述，不修改基线或主索引作掩盖。本席现场cached diff为空，不声称索引字节没变。后续写模式验证只隔离cwd。未读Grok本轮B2追加票前提交此自证。

本席B2最小核验（只读原前缀，不接触新Grok票）：
```text
ORIGINAL_PREFIX_OK modules/M008-b1-self-raw.md 143625 98dcb89462c00b3065764642043f61d08da478d742a7b99961bd9369b9aa27a3
ORIGINAL_PREFIX_OK modules/M008-b1-grok-raw.md 21297 8dd0fb2ef8b803c49b745f67e2852afe1f5109dec0271319b26112f0690f5af3
CANDIDATE_IDS M008-001,M008-002,M008-003,M008-004,M008-005,M008-006,M008-007,M008-008,M008-009,M008-010,M008-011,M008-012,M008-013,M008-014,M008-015,M008-016
GIT_CALL_SOURCE tools/verify-html-ledger-sync.mjs
10 import { execSync } from "node:child_process";
12 const git = (cmd) => execSync(`git ${cmd}`, { encoding: "utf8", maxBuffer: 32 * 1024 * 1024 });
16   process.chdir(git("rev-parse --show-toplevel").trim());
34   mergeBase = git(`merge-base ${base} HEAD`).trim();
44 const dirty = git(`status --porcelain -- ${HTML.join(" ")}`).trim() !== "";
50 const changed = git(`diff --name-only ${range}`)
GIT_CALL_SOURCE tools/verify-html-param-tier.mjs
18 import { execSync } from "node:child_process";
22 const git = (cmd) => execSync(`git ${cmd}`, { encoding: "utf8", maxBuffer: 32 * 1024 * 1024 });
26   process.chdir(git("rev-parse --show-toplevel").trim());
68     const mark = IGNORE.exec(rawLines[i] || "");
126   mergeBase = git(`merge-base ${base} HEAD`).trim();
141   prevJson = execSync(`git show ${mergeBase}:${BASELINE}`, { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] });
157 const dirty = git(`status --porcelain -- ${HTML.join(" ")}`).trim() !== "";
163 const changed = git(`diff --name-only ${range}`)
CACHED_DIFF ''
BASELINE_SHA 9332010bd445a64ff8f1fb5e3b6d7bbe387c7b76543b68424f0209dd7ddbfdc8
CHECK_ACTUAL_LOCK_SHA b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

```

收报后对007真实入口126补验的完整可复现命令（仅隔离复制，不改正本）：
```python
from pathlib import Path
import subprocess,tempfile,json,re,hashlib
r=Path('/home/james/complex-management');entry='docs/00-current-effective-document-entry-v0.1.md'
lines=(r/entry).read_text(encoding='utf-8').splitlines()
print('ACTUAL_ENTRY_CONTEXT\n'+'\n'.join(str(i)+': '+l for i,l in enumerate(lines,1) if 119<=i<=132))
with tempfile.TemporaryDirectory(prefix='m008-refresh-current-',dir=r/'.scratch') as td:
 t=Path(td)
 for rel in [entry,'README.md','docs/openapi/openapi-p0.json']:
  p=t/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((r/rel).read_bytes())
 md='backend/src/main/resources/db/migration';(t/md).mkdir(parents=True)
 for p in (r/md).glob('*.sql'):
  if re.match(r'^V\d+__.*\.sql$',p.name):(t/md/p.name).touch()
 sf='backend/target/surefire-reports';counts=[]
 if (r/sf).exists():
  (t/sf).mkdir(parents=True)
  for p in (r/sf).glob('*.xml'):
   m=re.search(r'<testsuite[^>]*\btests="(\d+)"',p.read_text(encoding='utf-8')[:500])
   if m:
    counts.append(int(m.group(1)));(t/sf/p.name).write_text('<testsuite tests="'+m.group(1)+'"/>',encoding='utf-8')
 print('LIVE_SUREFIRE_TEST_SUM',sum(counts))
 for args in [['--check'],[],['--check']]:
  p=subprocess.run(['node',str(r/'tools/refresh-entry-doc-baseline.mjs'),*args],cwd=t,text=True,capture_output=True)
  print('ISOLATED_CURRENT_REFRESH',args,'EXIT',p.returncode);print(p.stdout);print(p.stderr)
 print('AFTER_LINE126',(t/entry).read_text(encoding='utf-8').splitlines()[125])
 print('AFTER_README_TEST',next(l for l in (t/'README.md').read_text().splitlines() if '| 后端测试 |' in l))
print('BASELINE_AFTER',hashlib.sha256((r/'tools/html-param-tier-baseline.json').read_bytes()).hexdigest())

```
实际输出：
```text
ACTUAL_ENTRY_CONTEXT
119: | 一体机 | 统一终端码 `kiosk`；全体一体机统一具备权益展示、订台、吧台买酒、游乐状态和定期检修记录五项基础能力，通过叠加式售票能力开关 `ticketing_enabled` 区分是否允许授权服务人员辅助到店客人购票 |
120: | 启用售票能力的一体机 | 承担授权服务人员辅助到店购票、订单查询职责（**门票为动态二维码，不提供取票功能**）；绑定酒吧门口/前台时只能售卖本酒吧单店门票，不得售卖综合体通票（依据 BSM「启用售票能力的一体机」条；DEC-R7-05） |
121: | 卡座提成 | 一张账单的销售提成只计一次、只归帮该客人预订的人；自来客不产生销售提成；协助服务、加单、送达等其他经手人只留责任痕迹、不产生销售提成；服务员提成跟服务责任链和台位会话走：转台后记到新台会话，销售提成仍按原归属不变；并台时各服务员既有服务/提成记录分别保留，不把他人提成合并到一人，也不重算历史提成；存在预订归属时，按员工档案是否设有「销售岗」标识判定销售提成费率；费率可配（业务参数，按酒吧可配，出厂默认销售 15%、非销售 10%，须同时配置上下限），按可计提消费额计；不计销售提成：通票及票务权益整类（不论其中权益收费或免费、由谁出资）、卡座入场名额、赠送成本、退款、免单 |
122: | 卡座续卡/续时 | 平台不提供续卡、续时类产品。拆单、合单、转台、并台、拆台和撤销转台按业务 HTML 留痕，不作为续卡或续时产品 |
123: 
124: ### 2.1 R1~R4 审计交付物登记（2026-06-18 附加）
125: 
126: 本节附加式登记 R1~R4 阶段审计与回填交付物，以及配套的数据隔离规格与 SMS 占位规格，便于审计入口统一追溯。本节为附加内容，不删除上文任一原有登记；当前事实基线以 `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` 与 `npm run verify:*` 实时输出为准。本轮 2026-09-01 实测 OpenAPI 720 operations、Flyway 最新 V385；当前工作树无 `backend/target/surefire-reports`，故不声明当前测试数量。
127: 
128: | 交付物 | 入口 |
129: |---|---|
130: | R1 需求文档审计报告 | `docs/r1-requirements-documentation-audit-report-20260618.md` |
131: | R1 文档地图 | `docs/r1-documentation-map-20260618.md` |
132: | R1 需求-API-页面-表-测试 映射矩阵 | `docs/r1-requirement-api-page-table-test-mapping-matrix-20260618.md` |
LIVE_SUREFIRE_TEST_SUM 55
ISOLATED_CURRENT_REFRESH ['--check'] EXIT 1

[entry-doc-baseline] DRIFT: README.md 快照落后于实际（应为 720 ops / V399（377 迁移） / 55 tests）。运行 node tools/refresh-entry-doc-baseline.mjs 回填。

ISOLATED_CURRENT_REFRESH [] EXIT 0
[entry-doc-baseline] 已回填 docs/00-current-effective-document-entry-v0.1.md + README.md：720 ops / V399（377 迁移） / 55 tests（2026-09-09）。


ISOLATED_CURRENT_REFRESH ['--check'] EXIT 0
[entry-doc-baseline] PASS: 快照与实际一致（720 ops / V399（377 迁移） / 55 tests）。


AFTER_LINE126 本节附加式登记 R1~R4 阶段审计与回填交付物，以及配套的数据隔离规格与 SMS 占位规格，便于审计入口统一追溯。本节为附加内容，不删除上文任一原有登记；当前事实基线以 `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` 与 `npm run verify:*` 实时输出为准。本轮 2026-09-01 实测 OpenAPI 720 operations、Flyway 最新 V385；当前工作树无 `backend/target/surefire-reports`，故不声明当前测试数量。
AFTER_README_TEST | 后端测试 | **55** tests（现有 surefire 报告） | `backend/target/surefire-reports` 合计；无报告时保留上次快照，不声明刚完成全量测试 |
BASELINE_AFTER 9332010bd445a64ff8f1fb5e3b6d7bbe387c7b76543b68424f0209dd7ddbfdc8

```

最终本席票：PASS。B3=0，尚未修复；等待真实Grok B2结束后读取其本次完整票合取。


## M008 B2-r2 Codex独立归并复核票
文件自身版本：V0.03（保留V0.02全文155764bytes前缀，本段追加）
结论：PASS（本批归并复核票，仍须Grok本批合取）。
输入B1：modules/M008-b1-self-raw.md 原V0.01 143625bytes SHA98dcb89462c00b3065764642043f61d08da478d742a7b99961bd9369b9aa27a3；modules/M008-b1-grok-raw.md 原V0.01 21297bytes SHA8dd0fb2ef8b803c49b745f67e2852afe1f5109dec0271319b26112f0690f5af3。上批Grok B2-r1完整票为该文件V0.02总42982bytes SHA49a890bb74e88f32f8e3fbf9b8c053c2991de1a7a5006db0dbd79edea5600866。
当前候选findings.md V1.11，387743bytes SHA947406075f723a0adb103aa9985930538f958741aaa2c454ac004cf1d39044ee；M008 B2-r2 001–016，永久ID不变；ledger V1.92，B3=0。本票写入前未读取Grok本批B2-r2追加内容。

| ID | 本批独立票 |
|---|---|
| M008-001 | PASS，技术性/缺陷是/队列是；echo伪装实际必跑命令，原正反例及最小识别方向不变。 |
| M008-002 | PASS，是/是；缺正文不能通过全文一致，原有效/空值对照不变。 |
| M008-003 | PASS，是/是；删除台账不是同步，仍只修有效比较范围内文件级前提，不增语义检查。 |
| M008-004 | PASS，是/是；物理换行和同行段落反例不变，按逻辑块修且不改HTML原意。 |
| M008-005 | PASS，否/否。本方重新衡量后接受仅登记：3条均是真实的相同错误，退出及是否缺命令判定正确；本批不把可选精简列为必须修。原本方冗余意见及Grok否决保留，不假称005已修。GEN001/011全文保持，不外推新通用门槛。 |
| M008-006 | PASS，是/是；两个Git helper及show都改argv，探针差异只来自dirty/clean后续，不分裂ID。 |
| M008-007 | PASS，是/是；实际126漏匹配已复现，修工具与最小测试；M435/M436正本不在本批回填。 |
| M008-008 | PASS，否/否；生成器正常部分与输入资料漂移仅登记，不加自动版本管理。 |
| M008-009 | PASS，否/否；真实6项CSV失配和决策台账未同步均被正确拒绝，父链红按各归属模块后续处理。 |
| M008-010 | PASS，否/否；10历史日志不覆写、不当当前绿证。 |
| M008-011 | PASS，否/否；既有49回归已实际通过，B3仅为正式六项补最小失败测试。 |
| M008-012 | PASS，否/否；文件级同步和变更触发职责保留，各席实际验证范围分开。 |
| M008-013 | PASS，否/否；理论-report宽排除不加策略。 |
| M008-014 | PASS，否/否；CLI内部标识不借B/C另做授权，不改文案。 |
| M008-015 | PASS，否/否；UTC工程时间，不加业务时区规则。 |
| M008-016 | PASS，否/否；物理设备PGM036无实现面，全17项技术用途无业务发明。 |

差集复核：CX01–06→001–006，CX07→007/008、08→009、09→010、10→011；GR001→007/GR002→006；Grok观察1→005、2/3→012、4/5/6→010、7→013、8/10→011、9→014、11→015、12→016；没有遗漏、无新ID。除005两列及其解释外其余15行与r1相同；全部技术性，无添加/冲突/未答questions/新增判据。
Grok r1“1228行非空body=0”明确是记录笔误，按原实算空body=0理解；不是新增缺陷。原raw不覆盖。此前错误git show argv描述已在双方B2纠正。baseline同字节误写动作事实保持，不能改称没有写尝试。
本次本方只对候选归并做校验，没有重跑已完成的B1，也没有修源码。最小修复队列定为001–004、006–007；005/008–016不改。Grok本批票未读，不能据本票提前B3。

最终本席票：PASS。下一步合取Grok本批完整票，双PASS后才定稿并进入B3。

---

## M008 B2-r3 Codex整批归并复核（B4-r1回退）

文件自身版本：V0.04

输入为本批完整B4报告：M008-b4-r1-self-raw.md V0.01 52322bytes SHA583370935346bbe77bfc25efb8e8efea3d6fb68130ad2d4a5549a8f702c11068；M008-b4-r1-grok-raw.md V0.01 41557bytes SHA01904baab6ca22f9b74fc6eababfb0a293879fa2c08796eda33df837b78c0012。两份均在本方B4完整报告落盘后才读取，Grok原报告31141字符已全文分段读完，无截断结论。
候选：findings.md V1.13 409554bytes SHA0d6aa46b6d7db8ded0596afcd3a75daa5cb8e53788a3d3298c734cd6c9dd3a0f。B3累计1，当前源仍逐字节等于r1 after，未做新修复。
实算锁SHA b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a；本B2不新增独占锁行。沿用已实读receiving-code-review/diagnosing-bugs/systematic-debugging意图：先读原意见、以源码和隔离输出验证、再判分类；没有新增代理或调用未经核验技能，不把未调用工具写成已调用。

### 本批差集与逐ID票
| ID | 本方归并票 | 缺陷/队列与理由 |
|---|---|---|
| M008-001 | PASS | 是/是。B4反例是原命令实际执行识别未闭合，沿用ID；两个实际入口LF分别可导致分命令或语法错，helper仍绿。合法&&后LF须保留。 |
| M008-002 | PASS | 是/是。r1内容证据通过，但整批B4未双PASS，不提前出队；无需重复编辑已正确的逻辑。 |
| M008-003 | PASS | 是/是。同上，删除台账已被正确拒绝，后续保持真实同批范围。 |
| M008-004 | PASS | 是/是。同上，逻辑块/精确值豁免/棘轮已验证，不改业务三层规则。 |
| M008-005 | PASS | 否/否。50项44唯一数组与before完全相同，保留已定稿仅登记结论，不借本批删项。 |
| M008-006 | PASS | 是/是。两个脚本全部Git已argv，r1内容通过、整批未出队；无重复编辑需要。 |
| M008-007 | PASS | 是/是。GR-M008-B4-OBS-01正式并入同一个生成器当前快照漏匹配根因；当前160漏掉的证据可复现，不新建重复ID。 |
| M008-008 | PASS | 否/否。仅涵盖正常有效生成和M435/M436输入资料归属；它没有裁定“生成器漏160却check绿正确”。新事实在007，008不扩成免修口径。 |
| M008-009 | PASS | 否/否。当前父链六处anchors红/ledger红是其它资料失配被正常拒绝，原样登记。 |
| M008-010 | PASS | 否/否。历史十份日志不改、不当当前全绿证据。 |
| M008-011 | PASS | 否/否。当前71回归通过不覆盖本方新增LF反例；修001/007的最小失败验证仍归各自ID，不单开测试工程。 |
| M008-012 | PASS | 否/否。文件级同步职责和只扫描改动HTML不变；B3/B4本方已对三HTML副本全扫描0，不覆盖B1仅扫BSM的历史陈述。 |
| M008-013 | PASS | 否/否。没有新增真实误排证据，保持仅登记。 |
| M008-014 | PASS | 否/否。内部CLI语言非产品业务配置；B/C原意不扩成其它授权。 |
| M008-015 | PASS | 否/否。UTC取证日期是工程记录，不发明营业日/时区配置。 |
| M008-016 | PASS | 否/否。PGM036物理设备规则未被17个治理工具实现/改写，技术分类无业务发明。 |

根因集合完整：本方B4唯一FAIL001；GrokB4唯一新观察GR-M008-B4-OBS-01并007。其余全部修复归因、旧重复常量、输入红、日志、CLI/UTC/物理设备及扫描观察都有既有ID承接。本候选无遗漏、无改写原始票、无添加/HTML修改；不以Grok的B4 PASS覆盖本方FAIL，也不代其写B2票。

### 007争议核实
原007正式处置：“修生成器对当前活跃快照句的匹配，保留历史变更句。”位置举证126不是把工具职责永久限成126。原008处置明确“007单列真实工具漏匹配”。工具自身第7/8行明确本轮/当前/取证快照与历史变更的区别。实际160写“本轮2026-09-01取证为Flyway最新V385…当前工作树无…”，与126同为当前快照。其所在历史登记节标题不使“本轮/当前”变成旧changelog。因此候选修工具对160的识别仍是原007，既不修改历史记录，也不在本阶段回填M436正本。不同意GrokB4仅登记理由，保留其原文并经本B2重审，不私改其票。
本方收报后独立最小复现（所有写模式仅临时副本）：
```python
from pathlib import Path
import subprocess,tempfile,json,re,hashlib
r=Path('/home/james/complex-management');entry='docs/00-current-effective-document-entry-v0.1.md'
lines=(r/entry).read_text(encoding='utf-8').splitlines()
print('ACTUAL_ENTRY_CONTEXT\n'+'\n'.join(str(i)+': '+l for i,l in enumerate(lines,1) if i in (126,160)))
with tempfile.TemporaryDirectory(prefix='m008-refresh-current-',dir=r/'.scratch') as td:
 t=Path(td)
 for rel in [entry,'README.md','docs/openapi/openapi-p0.json']:
  p=t/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((r/rel).read_bytes())
 md='backend/src/main/resources/db/migration';(t/md).mkdir(parents=True)
 for p in (r/md).glob('*.sql'):
  if re.match(r'^V\d+__.*\.sql$',p.name):(t/md/p.name).touch()
 sf='backend/target/surefire-reports';counts=[]
 if (r/sf).exists():
  (t/sf).mkdir(parents=True)
  for p in (r/sf).glob('*.xml'):
   m=re.search(r'<testsuite[^>]*\btests="(\d+)"',p.read_text(encoding='utf-8')[:500])
   if m:
    counts.append(int(m.group(1)));(t/sf/p.name).write_text('<testsuite tests="'+m.group(1)+'"/>',encoding='utf-8')
 print('LIVE_SUREFIRE_TEST_SUM',sum(counts))
 for args in [['--check'],[],['--check']]:
  p=subprocess.run(['node',str(r/'tools/refresh-entry-doc-baseline.mjs'),*args],cwd=t,text=True,capture_output=True)
  print('ISOLATED_CURRENT_REFRESH',args,'EXIT',p.returncode);print(p.stdout);print(p.stderr)
 print('AFTER_LINE126',(t/entry).read_text(encoding='utf-8').splitlines()[125])
 print('AFTER_LINE160',(t/entry).read_text(encoding='utf-8').splitlines()[159])
 print('AFTER_README_TEST',next(l for l in (t/'README.md').read_text().splitlines() if '| 后端测试 |' in l))
print('BASELINE_AFTER',hashlib.sha256((r/'tools/html-param-tier-baseline.json').read_bytes()).hexdigest())

```
```text
ACTUAL_ENTRY_CONTEXT
126: 本节附加式登记 R1~R4 阶段审计与回填交付物，以及配套的数据隔离规格与 SMS 占位规格，便于审计入口统一追溯。本节为附加内容，不删除上文任一原有登记；当前事实基线以 `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` 与 `npm run verify:*` 实时输出为准。本轮 2026-09-01 实测 OpenAPI 720 operations、Flyway 最新 V385；当前工作树无 `backend/target/surefire-reports`，故不声明当前测试数量。
160: 本节附加式登记业务决策批次与本会话已落地新功能，不删除上文任一原有内容。本轮 2026-09-01 取证为 Flyway 最新 V385、OpenAPI 720 operations；当前工作树无后端 surefire 报告，不声明当前测试数量。各项以 `docs/openapi/openapi-p0.yaml`、`backend/src/main/resources/db/migration`、`backend/target/surefire-reports`、`npx playwright test --list` 与 `npm run verify:*` 实时输出为最终权威。
LIVE_SUREFIRE_TEST_SUM 55
ISOLATED_CURRENT_REFRESH ['--check'] EXIT 1

[entry-doc-baseline] DRIFT: docs/00-current-effective-document-entry-v0.1.md + README.md 快照落后于实际（应为 720 ops / V399（377 迁移） / 55 tests）。运行 node tools/refresh-entry-doc-baseline.mjs 回填。

ISOLATED_CURRENT_REFRESH [] EXIT 0
[entry-doc-baseline] 已回填 docs/00-current-effective-document-entry-v0.1.md + README.md：720 ops / V399（377 迁移） / 55 tests（2026-09-09）。


ISOLATED_CURRENT_REFRESH ['--check'] EXIT 0
[entry-doc-baseline] PASS: 快照与实际一致（720 ops / V399（377 迁移） / 55 tests）。


AFTER_LINE126 本节附加式登记 R1~R4 阶段审计与回填交付物，以及配套的数据隔离规格与 SMS 占位规格，便于审计入口统一追溯。本节为附加内容，不删除上文任一原有登记；当前事实基线以 `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` 与 `npm run verify:*` 实时输出为准。本轮 2026-09-09 实测 OpenAPI 720 operations、Flyway 最新 V399；后端 surefire 现有报告 55 tests。
AFTER_LINE160 本节附加式登记业务决策批次与本会话已落地新功能，不删除上文任一原有内容。本轮 2026-09-01 取证为 Flyway 最新 V385、OpenAPI 720 operations；当前工作树无后端 surefire 报告，不声明当前测试数量。各项以 `docs/openapi/openapi-p0.yaml`、`backend/src/main/resources/db/migration`、`backend/target/surefire-reports`、`npx playwright test --list` 与 `npm run verify:*` 实时输出为最终权威。
AFTER_README_TEST | 后端测试 | **55** tests（现有 surefire 报告） | `backend/target/surefire-reports` 合计；无报告时保留上次快照，不声明刚完成全量测试 |
BASELINE_AFTER 9332010bd445a64ff8f1fb5e3b6d7bbe387c7b76543b68424f0209dd7ddbfdc8

```

### 定稿前只读机械核验
```text
CANDIDATE_ROWS 16
| M008-001 | CX-01；Grok includes观察；Codex B4-r1换行证据 | 技术性 | 是 | 是 |
| M008-002 | CX-02 | 技术性 | 是 | 是 |
| M008-003 | CX-03；与Grok文件级同步观察区分 | 技术性 | 是 | 是 |
| M008-004 | CX-04 | 技术性 | 是 | 是 |
| M008-005 | CX-05 + Grok观察1；B2-r1外审否决后本方收窄 | 技术性 | 否（本批仅登记） | 否 |
| M008-006 | CX-06 + GR-M008-002 | 技术性 | 是 | 是 |
| M008-007 | GR-M008-001；Codex收报后补验；GR-M008-B4-OBS-01同根因并入 | 技术性 | 是 | 是 |
| M008-008 | CX-07其余生成器观察 | 技术性 | 否 | 否 |
| M008-009 | CX-08 + Grok现行数据红 | 技术性 | 否（本模块源码） | 否 |
| M008-010 | CX-09 + Grok观察4/5/6 | 技术性 | 否 | 否 |
| M008-011 | CX-10 + Grok观察8/10 | 技术性 | 否 | 否 |
| M008-012 | Grok观察2/3 | 技术性 | 否 | 否 |
| M008-013 | Grok观察7 | 技术性 | 否 | 否 |
| M008-014 | Grok观察9 | 技术性 | 否 | 否 |
| M008-015 | Grok观察11 | 技术性 | 否 | 否 |
| M008-016 | Grok观察12 + 全17文件分类 | 技术性 | 否 | 否 |
plan/criteria-lock.md 889020 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
modules/M008-b4-r1-self-raw.md 52322 583370935346bbe77bfc25efb8e8efea3d6fb68130ad2d4a5549a8f702c11068
LOCK_LINES [b'b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a']
modules/M008-b4-r1-grok-raw.md 41557 01904baab6ca22f9b74fc6eababfb0a293879fa2c08796eda33df837b78c0012
LOCK_LINES [b'b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a']
modules/M008-b1-self-raw.md 159438 facb9efcdac72790881da00601984223ff021f3209aa28aea63f9b48e9188b9b
LOCK_LINES [b'b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a']
CURRENT_EQUALS_B3_R1_AFTER tools/gate-hardening-tests/m001-regression.mjs True
CURRENT_EQUALS_B3_R1_AFTER tools/refresh-entry-doc-baseline.mjs True
CURRENT_EQUALS_B3_R1_AFTER tools/verify-delivery-gates.mjs True
CURRENT_EQUALS_B3_R1_AFTER tools/verify-entry-anchors.mjs True
CURRENT_EQUALS_B3_R1_AFTER tools/verify-html-ledger-sync.mjs True
CURRENT_EQUALS_B3_R1_AFTER tools/verify-html-param-tier.mjs True

```
Grok raw嵌入完整binary diff已机械检查出现1次，21637bytes、SHA996b9954f7b4782aafca8ef5904a556dcee90067c0919311651b85139c8089d2；两完整报告自身锁行各1条。B4归因双PASS，因此保留r1合法变更，没有越界整批回滚的触发事实。

**本批B2-r3本方结论：PASS。** 这只是一席的候选归并票；须Grok本批也PASS后才能定稿、进B3-r2。源文件、正本README/入口/HTML、基线和主索引未编辑。本方B1/B2旧159438bytes完整保留，本追加不冒充新B1全量报告。
