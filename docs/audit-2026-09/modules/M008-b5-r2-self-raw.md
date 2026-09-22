CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M008 B5-r2 Codex 本方全模块审计
文件自身版本：V0.01

本方审计结论：PASS。本轮完整审计17文件、1605行、19项冻结判据，第一性原理/顺从式/对抗式三线均覆盖全模块。未发现新增工程缺陷、业务冲突或添加项。本方报告完整落盘前未读取本次Grok B5报告；本票不代替双席合取或B5后四值校验，不单方改模块PASS。

## 1. 范围与独立性
正本/home/james/complex-management，main HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c。控制提示词V0.17；plan/modules V0.04固定FILES块17项。实际修复后1605行，冻结计划1530行为切分时计数，不为凑行改计划。B3累计2，B4-r2已双PASS且六项出队。当前ledger V1.98 / findings V1.15，16项当前修复队列全部否，LOCK-AMEND-COUNT为1。
先等Grok B5-r2进程真实exit0和唯一路径回执，再开始本方fresh完整阅读（四组，anchors145–213显式补读以恢复输出显示截断）。本方实跑六份语法、77条回归、五个实际入口、真实文档隔离回填、三HTML隔离全量扫描、两个入口12个原生shell对照。未接触Grok正文或JSON，未将历史B4结果作为本次PASS依据。
继续按此前已读技能的适用意图执行：verification-before-completion提供本次新鲜证据，receiving-code-review待自己报告落盘后接收外审；grilling/diagnosing-bugs追问主路径、输入、错误行为；writing-for-agents写可复核证据；ponytail不把理论路径加成门槛。这里不宣称再次读取、调用未使用的工具或新增第三席。仅SSH，没有MCP、子代理、业务写入、PR/merge/push/部署。

## 2. 当前全部文件字节清单
每份全文读完；下列SHA在本轮开始及测试前实算复核。完整版本与行数以实际文件为准。
| 文件 | 行数 | bytes | SHA256 |
|---|---:|---:|---|
| tools/html-param-tier-baseline.json | 10 | 596 | 9332010bd445a64ff8f1fb5e3b6d7bbe387c7b76543b68424f0209dd7ddbfdc8 |
| tools/refresh-entry-doc-baseline.mjs | 126 | 7807 | 8bd1ba5ac141ef47dbcd409616fcacc3303546b06cceba998852895fac4f8cbb |
| tools/verify-delivery-gates.mjs | 333 | 16444 | cbdfe7c023c3a5cc623b4f1742c56ca46238f039d701150d8d1b6abbe9102b8f |
| tools/verify-entry-anchors.mjs | 275 | 16186 | 7e62c6a19e36be5c13b166fd40aa4cb75e6ad057dfd6e97090323cae17853b82 |
| tools/verify-html-ledger-sync.mjs | 68 | 3894 | 1c9f974f4c49c3886469e6b86236675393317b7b0379bbbd1d354599f911214d |
| tools/verify-html-param-tier.mjs | 199 | 11193 | c79cdb39969b123264e917aa2921e4442e2ac1679f5f78ae6fbc0c70a5e71be9 |
| tools/gate-hardening-tests/99-predeploy-final.log | 138 | 7027 | 61c74dc4e62f27c5f75c3a39735c93f73ba8054a26c063822da7d05348804421 |
| tools/gate-hardening-tests/A-worktree-uncommitted.log | 42 | 5776 | 14ada1aa68187b672b10bcd98b0843df7177c147adafee8ded0cd826053942c4 |
| tools/gate-hardening-tests/B-committed.log | 17 | 1692 | 3dce69862e4c9c340f9db0d1769c6e383172b05b3447140c4113f550bbe63339 |
| tools/gate-hardening-tests/C-no-baseline.log | 39 | 2462 | 5f33afad72fc0a3a1375da16a08293097b74bb7795e05ab6ca6d341baf10ec67 |
| tools/gate-hardening-tests/D-baseline-legacy-pass.log | 10 | 1100 | 7fbcd28464611e031881424bb4478767c33118c5c621a2e48da2fb59471ec164 |
| tools/gate-hardening-tests/E-ignore-marker.log | 5 | 803 | 0c29f64c5510c204dc6be1a48293b5df69a1e58e53674b5e40d3469db5ab8633 |
| tools/gate-hardening-tests/F-ci-baseline-evidence.log | 7 | 926 | 5e07e04d48d5b3312dac8bd6fd5e107ebe5965fcd6414d4e8041847fbdfca14f |
| tools/gate-hardening-tests/G-merge-pr-guards.log | 24 | 1108 | 59ebf364d51ad9caa6b03234eced31ef6d8cce2fbebf5c4f64828bb2ffe489ae |
| tools/gate-hardening-tests/I1-baseline-ratchet.log | 13 | 1055 | fa1a0a89f762063635f0711a5b57ed4f97be5a57a1e091cc0b33d28475d7b483 |
| tools/gate-hardening-tests/I2-ignore-granularity.log | 21 | 4264 | 75394c0dd0e5e3c8296edb036ea053af444a0ea678277919e301478837e3a25b |
| tools/gate-hardening-tests/m001-regression.mjs | 278 | 17155 | c64e3048e7525c6158e102f9cd9eaf9cd10972c1255f9a2deb5528a8299383a6 |

## 3. 逐文件三线审计
各项分类按冲突→细化→技术性→添加依次核对。下面17项均为技术性：服务于已有资料一致性、执行链检查、工程计数或其验证记录，未发明业务实体、枚举、计费、权限审批或现场流程；HTML未覆盖工程实现细节并不构成添加。PGM-036物理设备业务不由这些CLI实现。全部依据类型2（可执行产物）/4（可复现输出），需要对业务边界确认时使用类型1的PGM原文。

| 文件与全文范围 | 第一性原理 | 顺从式 | 对抗式与结论 |
|---|---|---|---|
| html-param-tier-baseline.json:1–10 | entries=[]真实JSON；SHA1稳定键口径是检查豁免载体，无新增参数含义 | 只缩不增、无行号键的说明与param脚本49–50/137–155一致 | 新增key反例在回归中真实失败；实际三HTML副本重建仍0项。PASS，关联012/016 |
| refresh-entry-doc-baseline.mjs:1–126 | JSON方法、迁移文件名、XML tests属性是三类工程事实；UTC是取证日期 | 活跃快照包括126实测/160取证；历史变更句保持；无报告保留旧tests；日期单独变化不算check漂移 | 真实副本check1→write0→check0，两处都到V399/55tests；77回归含扰动和无报告。PASS，007闭合，008/015保持仅登记 |
| verify-delivery-gates.mjs:1–333 | 读现行package/CI/YAML/deploy及文档条目，核真正声明执行的npm链 | 两个调用入口共用严格helper；CI镜像/fetch-depth/事件基线/预部署调用与部署REQUIRED_GATES检查均有实际断言 | 12原生shell对照确认非法LF被拒、合法续行/tab/边界LF通过；打印/相似名/注释反例均拒绝；当前入口0。005重复50/44保持。PASS |
| verify-entry-anchors.mjs:1–275 | 三CSV从字节到字段，注册正文/来源原文/双向覆盖/镜像片段/Markdown引用逐段核 | 空body拒绝、重复和格式错误拒绝；注册表全文比HTML，不把历史行提示提升业务权威；9占位行明确白名单 | 77回归含引号逗号跨行/BOM/坏CSV/空body；当前六处失配全部明确报红。现有原文和字段空值检查均无新增正常路径缺陷。PASS，002闭合，009/013仅登记 |
| verify-html-ledger-sync.mjs:1–68 | base→merge-base→脏净范围→changed HTML→同批未删除ledger路径，逐步Git真实argv | CLI优先env后origin；取不到基线失败；可从子目录定位仓根；文件级同批定义不升格为自然语言审阅 | 删除ledger、已提交删除加未跟踪副本、非法base、脏/PR/push/合法同步均有可失败回归；当前HTML脏而台账未同步正确exit1。PASS，003/006闭合，009/012仅登记 |
| verify-html-param-tier.mjs:1–199 | 等长mask剥style/comment，逻辑块归一化，数值+单位、层级、精确值豁免、稳定key、基线棘轮串联 | 按变更HTML全文扫描；不改业务三层含义；Git全argv包括show；基线取不到失败 | 正常段内换行不误红、同物理行邻段不漏红、豁免不串后段、裸/部分豁免拒绝、否定词及新增key反例覆盖；现行BSM及独立全3HTML扫描0新项。PASS，004/006闭合 |
| gate-hardening-tests/99-predeploy-final.log:1–138 | 旧时命令输出/EXIT0是历史运行证据 | 保留原运行数量和旧入口输出，不称当前计数 | 以本次5入口输出对照，实际3类红明确保留；未把旧全链绿作为本次PASS依据。PASS，010 |
| gate-hardening-tests/A-worktree-uncommitted.log:1–42 | 历史未提交变更场景展示旧失效与新拒绝 | 当前dirty分支仍读取基线至工作区；存量数量按历史解释 | 本次未提交错误的两个工具反例实跑通过。PASS，010 |
| gate-hardening-tests/B-committed.log:1–17 | 历史提交变更场景可追到Git比较语义 | 当前clean分支仍base...HEAD，CI需真实事件前基线 | 本次PR/push多提交/明确基线覆盖，不复用历史结论。PASS，010 |
| gate-hardening-tests/C-no-baseline.log:1–39 | 记录缺基线旧跳过与新失败 | 当前两个工具均显式exit1和fetch提示 | 本次缺失基线反例真实失败；无静默降级。PASS，010 |
| gate-hardening-tests/D-baseline-legacy-pass.log:1–10 | 历史6项已登记值的警告放行 | 当前baseline为空不反向修改旧证据 | 新增key拒绝与原基线不变通过再次实跑；不称当前仍有6项。PASS，010 |
| gate-hardening-tests/E-ignore-marker.log:1–5 | 旧裸ignore放行记录与目前规则有明确时代差异 | 当前代码注释14–15说明后续收窄，原log不承担新承诺 | 本次空豁免/部分豁免拒绝，明确豁免通过。PASS，010 |
| gate-hardening-tests/F-ci-baseline-evidence.log:1–7 | 历史run31145223559的checkout配置证据 | 当前CI正文由delivery实时验证，不用旧成功当当前运行成功 | 镜像分叉/fetch0/事件基线/执行预部署缺失反例都可失败；本次未声称重新调用GitHub CI。PASS，010 |
| gate-hardening-tests/G-merge-pr-guards.log:1–24 | 旧拒绝路径、旧head、未跑真实merge的限制均可读 | 测试只验证guard边界，没有真实合并副作用 | 回归stable/watch/late桩验证；没有把桩变成线上merge证明。PASS，010 |
| gate-hardening-tests/I1-baseline-ratchet.log:1–13 | 旧9key新增拒绝/缩减通过与棘轮定义相符 | 新实现仍比较key集合，当前0项不覆盖旧记录 | 本次新增key拒绝与baseline不变通过。PASS，010 |
| gate-hardening-tests/I2-ignore-granularity.log:1–21 | 同句两个值与部分豁免用于证明粒度 | 当前valuesIn及同块raw marker保留值级识别 | 本次裸、部分、全部、紧随段尾、不可扩大到后段反例覆盖。PASS，010 |
| gate-hardening-tests/m001-regression.mjs:1–278 | 临时Git仓/文件与命令实际执行；输出由断言产生，测试退出码可失败 | before→after可区分；CSV/CI/deploy/命令文本/Git/参数/计数关键分支覆盖；隔离清理且不动正本 | 本次77PASS/exit0，裸assert在失败时提前退出为真实exit1而非假绿；after也运行无报告分支。PASS，011，未加框架/强制npm入口 |

## 4. 冻结19项逐项判定
已实核M008映射GEN-001–018、PGM-036，无新增判据。通用判据不以“代码是技术性”免审；无对应实现面的项说明适用边界。
| 判据 | 本轮证据与结论 |
|---|---|
| GEN-001 必要性最小性 | 5工具/1测试/1基线/10日志用途逐项见上；无空转抽象。005重复常量现仅重复准确诊断，不改变检查行为，遵既有双票仅登记，不借本轮删。PASS |
| GEN-002 可执行正确性 | 六语法0、77回归0、现行5入口与实际数据对应；12原生对照与真实生成副本支持修复行为。资料失配被正确拒绝，未假称全链绿。PASS |
| GEN-003 测试有效性 | 回归执行真实目标脚本并有成功/失败断言；无报告计数与历史句保留、CSV解析、Git比较、CI内容、命令位置均覆盖；裸assert非假成功。PASS |
| GEN-004 信任边界校验 | CSV字段结构/空body、JSON、Git ref argv、npm命令文本及基线key在使用时检查；当前CSV必需正文无空值。非法ref被当参数拒绝而非执行其它shell。PASS |
| GEN-005 授权最小权限 | 无服务端业务主体/对象/动作授权实现面；读取治理文件/隔离生成未建立权限旁路，CI检查不代替业务授权。PASS（无业务授权实现面） |
| GEN-006 数据完整性 | 删除ledger不计同步，stablekey棘轮不放大已登记集合；生成仅本地工程文档，读取计算完成后写，未涉及业务金额/数据库事务。未为假设双文件磁盘故障引入事务框架。PASS |
| GEN-007 并发幂等 | CLI按单次本地命令执行，check无写；隔离write后再check为0，重复生成同日无漂移。无业务投递/共享缓存/并发消费实现面。PASS |
| GEN-008 接口模式一致性 | CSV三方字段/覆盖/镜像与HTML对齐；现行六红是工具抓到上游资料差异。CI/package/deploy入口约定已有回归。无DTO/API迁移实现变更。PASS |
| GEN-009 错误恢复 | 缺基线/坏CSV/空正文/非法链/删除台账/快照漂移都明确非0；成功路径确实运行；不吞资料错误变绿。PASS |
| GEN-010 安全敏感信息 | 两脚本全部Git用execFileSync argv包括show；固定业务文档路径，不构造外发；报告不含key，CLI日志不泄露业务秘密。PASS |
| GEN-011 可观测审计证据 | 文件、范围、计数、失败项能追到实际输出；旧log明确历史。005重复但内容真实，011失败可见；不把旧EXIT0或桩当当前发布证据。PASS |
| GEN-012 资源性能 | 有限仓库文件线性扫描、同步CLI生命周期、临时目录finally清理；Git大输出已有32MiB边界；实际1605行模块和1228/1235/1244行数据可完成；无常驻无界缓存。PASS |
| GEN-013 构建依赖 | node语法通过，现行实际依赖可解析；npm/CI/deploy一致性有正反例；missing base明确红，不依赖静默默认成功。PASS |
| GEN-014 数据库迁移 | 仅列名计数和取最大版本，不执行SQL/模式变更。现行377个V*文件最新V399被独立副本验证；不将计数等同迁移质量。PASS（无迁移执行面） |
| GEN-015 界面可用性 | 内部CLI提供可读路径与错误，没有产品界面/焦点/键盘UI实现；英文机器标识不引入业务文案。PASS（无产品UI实现面） |
| GEN-016 文档可验证性 | 17文件路径/说明/历史标记按实际核；旧log没有当前引用承诺；fresh输出与现行数据一致。008/009跨资料模块待审按原ID记录，不作为工具缺陷隐去。PASS |
| GEN-017 业务来源分类 | 全17逐项技术性，未发明业务；PGM-036物理设备与CI gate并非同一对象；B/C原意不扩成CLI授权。PASS |
| GEN-018 第一性原理证据 | 源码逐行、HTML原文、实际命令与隔离反例支持各结论；没有以B1/B4票或惯例替代本轮证据；不需要外部新规范。PASS |
| PGM-036 设备管理 | 锁指向PGM:611：“设备管理：谁领了什么设备，设备能不能用”；原文要求设备绑定员工、班次、点位和状态。全17文件没有现场设备绑定实现，不与其冲突，也不据此宣称设备功能通过。PASS（映射已检查，无该业务实现面） |

## 5. 本次全部观察与既有ID归并建议
无新根因、无新临时ID、无新增工程修复队列；下列本方观察均对应当前16项，避免漏并或把历史描述当现况。所有项分类技术性、无业务发明，依据类型2/4；016辅以PGM原文类型1。
| ID | 本轮位置及证据 | 当前处置 |
|---|---|---|
| M008-001 | delivery共享helper及两个调用点；native12例与回归文本误判反例通过 | 已修，队列否；GEN-002/004/008/009/013 |
| M008-002 | anchors:113–115空正文拒绝；当前1228行entry_body空0；坏CSV/空body反例通过 | 已修，否；GEN-002/004/009/016 |
| M008-003 | ledger:58同范围diff-filter排除删除；删除两类反例通过 | 已修，否；GEN-002/006/009/011 |
| M008-004 | param:54–88按逻辑块；排版/相邻段/豁免反例通过 | 已修，否；GEN-002/008/009/016 |
| M008-005 | delivery既有requiredDeliveryCommands全字节保持50项44唯一 | 仅登记，否；GEN-001/011，不重开可选精简 |
| M008-006 | ledger:12、param:22/142全部argv；CLI/env非法base各拒绝 | 已修，否；GEN-002/004/009/010/018 |
| M008-007 | refresh:67–71与真实entry126/160；实际副本两句均V399/55tests且check0 | 已修，否；GEN-002/009/016 |
| M008-008 | refresh:79–126；现行check同时报entry+README漂移，副本可正确回填 | 仅登记输入资料漂移，否；GEN-001/002/016，待M435/M436，不新加版本管理 |
| M008-009 | anchors实际6红、ledger实际台账未同步1，输出详下；CSV完整数据有据 | 仅登记本工具正确拒绝，否；GEN-002/008/016，对应M425–M428资料待其模块 |
| M008-010 | 全10log及本次rg历史引用查询，无非审计README/docs/package引用 | 历史记录保留，否；GEN-001/011/016 |
| M008-011 | regression:1–278，fresh77PASS；裸assert可真实失败 | 仅登记测试结构，否；GEN-003/013/018，本轮非旧49条 |
| M008-012 | param:158–199变更全文、ledger文件级；本方本次确实另扫全3HTML，entries0 | 职责符合，否；GEN-002/006/009，不把删除ledger与普通追加混淆 |
| M008-013 | anchors排除规则；实际360份Markdown受检，未见真实误排错误通过 | 仅登记，否；GEN-002/016，不以理论宽泛新增策略 |
| M008-014 | 全CLI内部PASS/FAIL/DRIFT；无产品UI与业务授权实现 | 仅登记，否；GEN-015/017 |
| M008-015 | refresh:51，本次UTC2026-09-09 | 工程日期，否；GEN-002/016 |
| M008-016 | PGM:611与全17文件用途 | 无物理设备实现，否；PGM-036/GEN-017/018 |

## 6. 本轮新鲜验证命令与完整结果
以下所有命令均在本方B5全量阅读后新运行，非复制B4结果。运行目录为正本，写模式只对自动清理隔离副本。现有测试自身生成临时夹具，未执行真实npm发布链或PR/merge。

### 6.1 六份node --check与node tools/gate-hardening-tests/m001-regression.mjs
```text
SYNTAX tools/refresh-entry-doc-baseline.mjs 0 
SYNTAX tools/verify-delivery-gates.mjs 0 
SYNTAX tools/verify-entry-anchors.mjs 0 
SYNTAX tools/verify-html-ledger-sync.mjs 0 
SYNTAX tools/verify-html-param-tier.mjs 0 
SYNTAX tools/gate-hardening-tests/m001-regression.mjs 0 
FRESH_B5_R2_REGRESSION 0 PASS_COUNT 77
PASS M008-006 参数基线 tools/verify-html-ledger-sync.mjs
PASS M008-006 环境基线 tools/verify-html-ledger-sync.mjs
PASS M008-006 参数基线 tools/verify-html-param-tier.mjs
PASS M008-006 环境基线 tools/verify-html-param-tier.mjs
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
PASS M008-004 换行标注
PASS M008-004 同行两段
PASS M008-004 段后豁免
PASS M008-004 豁免不扩到后段
PASS 台账已同步
PASS M008-003 删除台账
PASS M008-003 已提交删除不被未跟踪副本掩盖
PASS 基线新增 key
PASS 基线保持不变
PASS 部署主线比较 ops/160/complex-deploy-api.sh tools/verify-html-ledger-sync.mjs
PASS 部署主线比较 ops/160/complex-deploy-api.sh tools/verify-html-param-tier.mjs
PASS 部署主线比较 ops/160/complex-deploy-web.sh tools/verify-html-ledger-sync.mjs
PASS 部署主线比较 ops/160/complex-deploy-web.sh tools/verify-html-param-tier.mjs
PASS CSV 引号逗号及跨行字段
PASS M008-002 空条目正文
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
PASS M008-001 verify:predeploy 打印
PASS M008-001 verify:predeploy 相似名
PASS M008-001 verify:predeploy 整串打印
PASS M008-001 verify:predeploy 注释
PASS M008-001 verify:predeploy 命令内换行
PASS M008-001 verify:predeploy && 前换行
PASS M008-001 verify:predeploy && 后续行
PASS M008-001 verify:delivery-gates 打印
PASS M008-001 verify:delivery-gates 相似名
PASS M008-001 verify:delivery-gates 整串打印
PASS M008-001 verify:delivery-gates 注释
PASS M008-001 verify:delivery-gates 命令内换行
PASS M008-001 verify:delivery-gates && 前换行
PASS M008-001 verify:delivery-gates && 后续行
PASS 检查入口缺少 HTML 门禁
PASS 合并提交 stable
PASS 合并提交 watch
PASS 合并提交 late
PASS M008-007 回填快照
PASS M008-007 回填后校验
PASS M008-007 无报告仍回填计数
M001_GOVERNANCE_REGRESSION_PASS

```

### 6.2 五个真实入口
依次node tools/verify-delivery-gates.mjs；node tools/verify-html-param-tier.mjs；node tools/verify-entry-anchors.mjs；node tools/verify-html-ledger-sync.mjs；node tools/refresh-entry-doc-baseline.mjs --check。重复常量用正则取完整array，与r1 before同块逐字节比对。
```text
CURRENT tools/verify-delivery-gates.mjs EXIT 0
DELIVERY_GATES_DOCS_PASS

CURRENT tools/verify-html-param-tier.mjs EXIT 0
[html-param-tier] PASS: 改动了 1 份业务 HTML，全量重扫无基线外的未标层级句（存量 0 处已登记放行）。比对范围：工作区（含未提交改动）↔ 基线 origin/main@242f4872；扫描口径：改动文件全文，已剥除 <style> 与注释。

CURRENT tools/verify-entry-anchors.mjs EXIT 1
[entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1565 条；被检 Markdown 360 份；条目名引用 1517 处（注册表原名 1358 处，正文唯一定位 159 处）。
[entry-anchors] FAIL：6 处不合格。
  - 注册表条目正文在 HTML 现文中找不到：BSM-E0433 BSM「处理后回访客人或商户，验收合格才关闭」——HTML 已改而注册表未重建
  - 注册表条目正文在 HTML 现文中找不到：BSM-E0736 BSM「文档版本」——HTML 已改而注册表未重建
  - 账本正文与 HTML 现文对不上：BSM-0283「处理后回访客人或商户，验收合格才关闭」——HTML 已改而账本 source_text 未重取
  - 账本正文与 HTML 现文对不上：BSM-0530「文档版本」——HTML 已改而账本 source_text 未重取
  - 账本镜像列与 HTML 现文对不上：BSM-0283「处理后回访客人或商户，验收合格才关闭」preconditions 片段「处理后回访客人或商户，验收合格才关闭。」——HTML 已改而账本未重取（检查⑦）
  - 账本镜像列与 HTML 现文对不上：BSM-0283「处理后回访客人或商户，验收合格才关闭」state_transition 片段「处理后回访客人或商户，验收合格才关闭。」——HTML 已改而账本未重取（检查⑦）
  处置：条目改名或 HTML 改动后，重建 docs/prd-audit/entry-registry.csv 并把引用改为现行条目名；一律不得写回行号。

CURRENT tools/verify-html-ledger-sync.mjs EXIT 1
[html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。比对范围：工作区（含未提交改动）↔ 基线 origin/main@242f4872。
  已改动的 HTML：
    - docs/business-system-map-for-operations-v0.1.html
  必须在同一批改动内一并更新：docs/prd-audit/business-decision-ledger.md
  依据：用户 2026-08-03 拍板——台账保持完整，改 HTML 必须同 PR 同步台账。
  若本次确为纯排版/错别字、不涉及口径，请在台账内追加一行说明后再提交。

CURRENT tools/refresh-entry-doc-baseline.mjs EXIT 1
[entry-doc-baseline] DRIFT: docs/00-current-effective-document-entry-v0.1.md + README.md 快照落后于实际（应为 720 ops / V399（377 迁移） / 55 tests）。运行 node tools/refresh-entry-doc-baseline.mjs 回填。

M008_005_UNTOUCHED 50 44
```

### 6.3 真实入口及三HTML的隔离副本
复制实际entry/README/OpenAPI，按真实377迁移名建空文件、按实际XML属性复现55tests；运行绝对路径生成器check/write/check。三HTML另复制原始完整字节和baseline到独立临时Git仓，运行绝对路径param --write-baseline，全扫而非只触发当前改动文件，断言entries为0；正本基线没有写入。
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
AFTER_LINE160 本节附加式登记业务决策批次与本会话已落地新功能，不删除上文任一原有内容。本轮 2026-09-09 取证为 Flyway 最新 V399、OpenAPI 720 operations；后端 surefire 现有报告 55 tests。各项以 `docs/openapi/openapi-p0.yaml`、`backend/src/main/resources/db/migration`、`backend/target/surefire-reports`、`npx playwright test --list` 与 `npm run verify:*` 实时输出为最终权威。
AFTER_README_TEST | 后端测试 | **55** tests（现有 surefire 报告） | `backend/target/surefire-reports` 合计；无报告时保留上次快照，不声明刚完成全量测试 |
BASELINE_AFTER 9332010bd445a64ff8f1fb5e3b6d7bbe387c7b76543b68424f0209dd7ddbfdc8
ISOLATED_REAL_THREE_HTML_SCAN 0 entries 0
[html-param-tier] 已重建基线 tools/html-param-tier-baseline.json：0 条。请人工核对条数只减不增。

```

### 6.4 原生shell独立对照
复制delivery真实全部静态依赖，把两个package入口分别替换为正常链、&&后LF、命令内LF、&&前LF、tab、边界LF，运行r2before与当前实际脚本；/bin/sh -c只走隔离npm/run桩。桩npm打印到达参数，run桩显式exit127。不执行生产链，SHELL列为隔离桩结果。
```text
FRESH_B5_NATIVE verify:delivery-gates canonical BEFORE 0 AFTER 0 SHELL 0 NPM_TARGET True
FRESH_B5_NATIVE verify:delivery-gates valid-after-and BEFORE 0 AFTER 0 SHELL 0 NPM_TARGET True
FRESH_B5_NATIVE verify:delivery-gates LF-in-command BEFORE 0 AFTER 1 SHELL 127 NPM_TARGET False
FRESH_B5_NATIVE verify:delivery-gates LF-before-and BEFORE 0 AFTER 1 SHELL 2 NPM_TARGET False
FRESH_B5_NATIVE verify:delivery-gates tabs BEFORE 0 AFTER 0 SHELL 0 NPM_TARGET True
FRESH_B5_NATIVE verify:delivery-gates outer-LF BEFORE 0 AFTER 0 SHELL 0 NPM_TARGET True
FRESH_B5_NATIVE verify:predeploy canonical BEFORE 0 AFTER 0 SHELL 0 NPM_TARGET True
FRESH_B5_NATIVE verify:predeploy valid-after-and BEFORE 0 AFTER 0 SHELL 0 NPM_TARGET True
FRESH_B5_NATIVE verify:predeploy LF-in-command BEFORE 0 AFTER 1 SHELL 127 NPM_TARGET False
FRESH_B5_NATIVE verify:predeploy LF-before-and BEFORE 0 AFTER 1 SHELL 2 NPM_TARGET False
FRESH_B5_NATIVE verify:predeploy tabs BEFORE 0 AFTER 0 SHELL 0 NPM_TARGET True
FRESH_B5_NATIVE verify:predeploy outer-LF BEFORE 0 AFTER 0 SHELL 0 NPM_TARGET True
Only boundary stubs ran; run stub explicitly exits127; no real production npm chain executed.
```

### 6.5 当前数据与历史证据引用
Python csv.DictReader(UTF-8-sig,newline="")真实读取：entry-registry1228行，entry_body/entry_name空0；authority1235行，source_text/rule_id/entry_name空0；coverage1244行，rule_id/entry_name空0。
rg -n 'gate-hardening-tests/(99-predeploy-final|[A-G]-|I[12]-)' README.md docs package.json，并排除本轮审计报告路径，返回剩余引用0。未泛称全仓绝无引用；本次受检入口没有把这些日志作为现行事实。

## 7. 完整性与最终本方票
本轮全17文件开始及测试前SHA一致；随后三HTML、baseline、README、entry均与本轮输入保护SHA相同；主索引SHA eeb0e20f0c85989b6b902fba051c6b7b2f3051b870cacabd0c2ab6e2211b42aa，cached diff空，main/HEAD不变。锁实算与sidecar/ledger原基准一致；未提前读取Grok所以此处不假称完成B5四值外审校验。
没有本模块未闭合缺陷、待问添加或需改HTML事项。本轮本方B5-r2全模块审计PASS。操作员授权继续模块，无常规确认；必须在本报告完整落盘后读取Grok本次全文，只有双PASS且B5后四值全等，才按先ledger PASS再closure的顺序收口。


CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M008现锁B5-r2 Codex完整独立审计
版本：V0.02
旧V0.01前30565bytes SHA1f7c74f79eddf7eadaee556c982e243ec27b7cd083f51bd3bd644c5bfcc75951完整保留。
当前471计划M008，B3历史累计2；B2 selfV0.06/GrokV0.06双PASS、findingsV3.46全16当前队列否，故跳本批B3/B4，不清零计数。本次先派Grok后本席独立整模块B5，封存前未读取本次Grok B5 raw/JSON。B5不是只审差异，也不是继承旧结论。

## 当前全量取证
17FILES当前1605行，19判据。对17文件全部现字节重新取SHA，与本轮B1全文阅读逐一比较全等；三HTML也与本轮全读字节全等。故复用同字节全文阅读，以下对整个模块重新判断适用/顺从/对抗。没有源码修改，真实index/hook保持。当前数据入口五命令本节点重新执行；77项回归和隔离真实副本的结果复用本轮B1已实跑证据，未重复执行，绝不把旧历史日志充作本节点命令。
{
  "files": {
    "tools/html-param-tier-baseline.json": {
      "sha": "9332010bd445a64ff8f1fb5e3b6d7bbe387c7b76543b68424f0209dd7ddbfdc8",
      "lines": 10
    },
    "tools/refresh-entry-doc-baseline.mjs": {
      "sha": "8bd1ba5ac141ef47dbcd409616fcacc3303546b06cceba998852895fac4f8cbb",
      "lines": 126
    },
    "tools/verify-delivery-gates.mjs": {
      "sha": "cbdfe7c023c3a5cc623b4f1742c56ca46238f039d701150d8d1b6abbe9102b8f",
      "lines": 333
    },
    "tools/verify-entry-anchors.mjs": {
      "sha": "7e62c6a19e36be5c13b166fd40aa4cb75e6ad057dfd6e97090323cae17853b82",
      "lines": 275
    },
    "tools/verify-html-ledger-sync.mjs": {
      "sha": "1c9f974f4c49c3886469e6b86236675393317b7b0379bbbd1d354599f911214d",
      "lines": 68
    },
    "tools/verify-html-param-tier.mjs": {
      "sha": "c79cdb39969b123264e917aa2921e4442e2ac1679f5f78ae6fbc0c70a5e71be9",
      "lines": 199
    },
    "tools/gate-hardening-tests/99-predeploy-final.log": {
      "sha": "61c74dc4e62f27c5f75c3a39735c93f73ba8054a26c063822da7d05348804421",
      "lines": 138
    },
    "tools/gate-hardening-tests/A-worktree-uncommitted.log": {
      "sha": "14ada1aa68187b672b10bcd98b0843df7177c147adafee8ded0cd826053942c4",
      "lines": 42
    },
    "tools/gate-hardening-tests/B-committed.log": {
      "sha": "3dce69862e4c9c340f9db0d1769c6e383172b05b3447140c4113f550bbe63339",
      "lines": 17
    },
    "tools/gate-hardening-tests/C-no-baseline.log": {
      "sha": "5f33afad72fc0a3a1375da16a08293097b74bb7795e05ab6ca6d341baf10ec67",
      "lines": 39
    },
    "tools/gate-hardening-tests/D-baseline-legacy-pass.log": {
      "sha": "7fbcd28464611e031881424bb4478767c33118c5c621a2e48da2fb59471ec164",
      "lines": 10
    },
    "tools/gate-hardening-tests/E-ignore-marker.log": {
      "sha": "0c29f64c5510c204dc6be1a48293b5df69a1e58e53674b5e40d3469db5ab8633",
      "lines": 5
    },
    "tools/gate-hardening-tests/F-ci-baseline-evidence.log": {
      "sha": "5e07e04d48d5b3312dac8bd6fd5e107ebe5965fcd6414d4e8041847fbdfca14f",
      "lines": 7
    },
    "tools/gate-hardening-tests/G-merge-pr-guards.log": {
      "sha": "59ebf364d51ad9caa6b03234eced31ef6d8cce2fbebf5c4f64828bb2ffe489ae",
      "lines": 24
    },
    "tools/gate-hardening-tests/I1-baseline-ratchet.log": {
      "sha": "fa1a0a89f762063635f0711a5b57ed4f97be5a57a1e091cc0b33d28475d7b483",
      "lines": 13
    },
    "tools/gate-hardening-tests/I2-ignore-granularity.log": {
      "sha": "75394c0dd0e5e3c8296edb036ea053af444a0ea678277919e301478837e3a25b",
      "lines": 21
    },
    "tools/gate-hardening-tests/m001-regression.mjs": {
      "sha": "c64e3048e7525c6158e102f9cd9eaf9cd10972c1255f9a2deb5528a8299383a6",
      "lines": 278
    }
  },
  "html": {
    "business-enum-quick-reference-v0.1.html": "4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66",
    "business-system-map-for-operations-v0.1.html": "eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66",
    "platform-governance-map-for-operations-v0.1.html": "5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f"
  },
  "lock": "b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458",
  "head": "e456ba96c84185c8d39f18f548011daac48a6466",
  "index": "d9a65108a1e87bd300c299ca7640f832bd5c2feb771f35a7a2413d32a1748331",
  "hook": "46aabdf33cb3496d83a71e4357ca85ba2ed9dbbfd4582ab53747bc50bb748d6b",
  "source_status": "",
  "checks": {
    "verify-entry-anchors.mjs": {
      "exit": 0,
      "stdout": "[entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1565 条；被检 Markdown 440 份；条目名引用 1564 处（注册表原名 1391 处，正文唯一定位 173 处）。\n[entry-anchors] PASS：条目名唯一、注册表条目正文逐字与 HTML 现文一致、引用可唯一定位、正文无行号锚点、账本正文与九个镜像列均与 HTML 现文一致、账本与承接矩阵一一对应。\n",
      "stderr": ""
    },
    "verify-delivery-gates.mjs": {
      "exit": 0,
      "stdout": "DELIVERY_GATES_DOCS_PASS\n",
      "stderr": ""
    },
    "verify-html-ledger-sync.mjs": {
      "exit": 1,
      "stdout": "",
      "stderr": "[html-ledger-sync] FAIL: 改动了业务最终解释权 HTML，但决策台账未同步。比对范围：工作区（含未提交改动）↔ 基线 origin/main@88c7b465。\n  已改动的 HTML：\n    - docs/platform-governance-map-for-operations-v0.1.html\n  必须在同一批改动内一并更新：docs/prd-audit/business-decision-ledger.md\n  依据：用户 2026-08-03 拍板——台账保持完整，改 HTML 必须同 PR 同步台账。\n  若本次确为纯排版/错别字、不涉及口径，请在台账内追加一行说明后再提交。\n"
    },
    "verify-html-param-tier.mjs": {
      "exit": 0,
      "stdout": "[html-param-tier] PASS: 改动了 1 份业务 HTML，全量重扫无基线外的未标层级句（存量 0 处已登记放行）。比对范围：工作区（含未提交改动）↔ 基线 origin/main@88c7b465；扫描口径：改动文件全文，已剥除 <style> 与注释。\n",
      "stderr": ""
    },
    "refresh-entry-doc-baseline.mjs": {
      "exit": 1,
      "stdout": "",
      "stderr": "[entry-doc-baseline] DRIFT: docs/00-current-effective-document-entry-v0.1.md + README.md 快照落后于实际（应为 732 ops / V431（399 迁移） / 147 tests）。运行 node tools/refresh-entry-doc-baseline.mjs 回填。\n"
    }
  }
}

## 第一性原理重新判断
为何存在：六个工具/基线（JSON和五mjs）用于现有工程可验证性，回归用于可失败行为检查，十日志保留历史时间证据。删工具会失去已经使用的命令/引用/台账/参数约束机器检查；删历史证据无用户要求。这里“六个工具/基线”不含回归，合计17。
声称是否执行：当前真实delivery/anchors/param绿、ledger/refresh红均与当前输入契合；本轮B1的77正反例和真实副本check1→write0→check0有效且工具未变。通过不等于全链绿或部署完成。
未证前提：台账同步仍文件级，不能证明任意新增行语义正确；参数扫描只覆盖被改HTML，三HTML副本全扫证据并未赋予全仓语义判断；日志日期/数值是旧事实；147只现有XML，不是本轮全后端验收。

## 全17文件三线重判
1. baseline JSON10行：entries空与实际三HTML扫描0吻合；稳定key说明/只缩不增由真实脚本比较执行，无新豁免。
2. refresh126行：OpenAPI七方法、数字迁移排序、XML现有数及无报告保留旧数；当前两活跃句126/160均在67—71正则覆盖，真实副本证明旧数会替换。日期归一化只剔除日期、数字漂移仍1。历史句保留。不自动升级资料版本是原职责，不增功能。
3. delivery333行：命令集合在真实运行位置识别，YAML镜像/checkout深度/事件基线/部署三检查核对；打印/注释/相似命令/错误LF拒绝，合法&&后LF允许。50/44重复保持既有不强制精简处置，不推导GEN001只认红绿。
4. anchors275行：CSV格式/BOM/引号/跨行/列宽、必查body空拒绝、原文定位/镜像匹配/矩阵，真实数据1228/1235/1244通过。-report宽仅理论限制，不新造规则。
5. ledger68行：argvref与工作区/提交基线判断、删除台账拒绝；当前未同步PGM退出1符合职责。不添加自然语言审批器。
6. param199行：style/comment屏蔽、p/li/tr/div/h逻辑块、数值精确豁免及稳定key棘轮。当前PGM扫描0、全三HTML本轮副本0；跨行标注/同行双p隔离证据有效。源码明确复杂嵌套限制，无当前真实失败，不加框架。
7—16. 99/A/B/C/D/E/F/G/I1/I2十日志合计316行逐一同字节，历史99全链PASS/E裸ignore绿/A/D/I2基线6与当前不同，保留证据，不改写不当本次验收。G旧stash记录不是现在执行指令。
17. m001-regression278行：现有标准库临时夹具真实执行当前工具，77断言正反例覆盖旧001—004/006—007、CI/merge边界；绝对temp目录与finally父路径检查，主仓零发布动作。无npm挂载和裸assert早退出不使失败变绿，不新立测试工程。

## 十九条判据完整适用矩阵
- GEN-001：各检查/生成器/JSON基线有实际执行用途；10旧日志保留历史证据，005六个重复命令保持旧双席处置，非本批强制精简；不增解析框架。
- GEN-002：77回归及真实入口结果与源码吻合；两个当前红是输入漂移，不是错误通过；真实副本生成后检查0。
- GEN-003：回归实际调用当前脚本，反例预期exit1、正例0；关键空正文/删除台账/命令伪造/换行/注入/两快照句均执行，assert可失败；不以旧日志替代。
- GEN-004：CSV列数、引号、必查entry_body，JSON entries数组与Git参数边界均检查；非法ref非0，未把文件内容当shell。
- GEN-005：工程脚本非业务授权服务；未改变商户范围或最高权限；不以CLI门禁替代服务器授权。
- GEN-006：只读检查不写业务；生成器只更新两份工程快照，当前用隔离副本检验。删除台账不能充当同步；文件级同步不是语义证明。
- GEN-007：脚本一次性串行检查、无消息重投/共享数据库事务；临时回归各mkdtemp独立。不增加并发锁。
- GEN-008：CSV/命令/JSON字段、生成器真实两活跃句式和现有计数口径匹配。数据红分配资料所属模块。
- GEN-009：非法ref/错误CSV/新增baselinekey/缺门禁显式非0；真实快照过期被检查1，不吞成绿。
- GEN-010：Git全部argv，包括gitshow；CLI/环境恶意基线77回归拒绝。只读主仓；无真实密钥输出。
- GEN-011：本次实际输出与历史10日志区分，77项输出可复现；主索引/hook内容前后不变。
- GEN-012：遍历受控文件/CSV/HTML/已有报告，无网络无限重试；Git maxBuffer32MB；回归finally核验绝对temp父再清理，未见首次正常资源失控。
- GEN-013：Node标准库+已有yaml依赖；CI镜像/全history/事件基线/部署三门禁正反例通过；工具本身不安装依赖或部署。
- GEN-014：无本模块数据库迁移写入；仅数迁移文件并按数字排最新，真实副本V431/399。不能代表数据库迁移已验收。
- GEN-015：无产品UI；内部PASS/DRIFT与错误中文解释可读，不给CLI添UI功能。
- GEN-016：十历史日志不是当前声明；真实entryanchors0、delivery0，快照与decisionledger红未隐藏；README/entry数字属后续资料回填。
- GEN-017：按冲突→细化→技术性→添加判，17文件均工程工具/证据；没有可归物理设备的业务实现，也无业务发明；逐文件见下。
- GEN-018：本报告证据为全源码/JSON/日志实读类型2与实际命令类型4，PGM原文类型1；不以席位历史票替代复现。
- PGM-036：治理HTML611原文“设备要绑定员工、班次、点位和状态”；本模块CI gate/entry不是实体设备，适用审查后N/A，不宣称设备功能已实现。

## 当前十六项全部重新判定
| ID | 当前全部来源 | 分类 | 当前缺陷 | 当前修复队列候选 | 当前证据/判据/处置 |
|---|---|---|---|---|---|
| M008-001 | Codex V0.05旧001重判；Grok V0.05旧001重判及对应技术观察 | 技术性 | 否（历史已修） | 否 | delivery7–11/161–164/321–324；精确npmCommands，伪打印/相似名/注释/LF反例均正确拒绝；GEN-002/004/008/009/013，证据类型2/4。无业务发明：检查/生成/证据，不增加业务对象或规则 |
| M008-002 | Codex V0.05旧002重判；Grok V0.05旧002重判及对应技术观察 | 技术性 | 否（历史已修） | 否 | anchors113–116；空entry_body回归拒绝，现行真实anchors0；GEN-002/004/009/016，证据类型2/4。无业务发明：检查/生成/证据，不增加业务对象或规则 |
| M008-003 | Codex V0.05旧003重判；Grok V0.05旧003重判及对应技术观察 | 技术性 | 否（历史已修） | 否 | ledger58；工作区及提交删除均拒绝，PGM未同步现红正确；GEN-002/006/009/011，证据类型2/4。无业务发明：检查/生成/证据，不增加业务对象或规则 |
| M008-004 | Codex V0.05旧004重判；Grok V0.05旧004重判及对应技术观察 | 技术性 | 否（历史已修） | 否 | param60–88；逻辑块替代物理行，换行/双p/段后豁免4回归通过；GEN-002/008/009/016，证据类型2/4。无业务发明：检查/生成/证据，不增加业务对象或规则 |
| M008-005 | Codex V0.05旧005重判；Grok V0.05旧005重判及对应技术观察 | 技术性 | 否 | 否 | delivery106–157；50项44唯一，重复真诊断无误判，保留不删6常量；不收窄GEN001定义；GEN-001/011，证据类型2/4。无业务发明：检查/生成/证据，不增加业务对象或规则 |
| M008-006 | Codex V0.05旧006重判；Grok V0.05旧006重判及对应技术观察 | 技术性 | 否（历史已修） | 否 | ledger12/param22及141；Git含show均argv，参数与环境注入拒绝；GEN-002/004/009/010/018，证据类型2/4。无业务发明：检查/生成/证据，不增加业务对象或规则 |
| M008-007 | Codex V0.05旧007重判；Grok V0.05旧007重判及对应技术观察 | 技术性 | 否（历史已修） | 否 | refresh67–71；实际副本126/160均回填732/V431/147，check1→write0→check0；无漏匹配；GEN-002/009/016，证据类型2/4。无业务发明：检查/生成/证据，不增加业务对象或规则 |
| M008-008 | Codex V0.05旧008重判；Grok V0.05旧008重判及对应技术观察 | 技术性 | 否 | 否 | refreshREADME块与其它快照；主README/entry仍旧，工具正确报漂移，归M435/M436输入；不加自动版本管理；GEN-001/002/016，证据类型2/4。无业务发明：检查/生成/证据，不增加业务对象或规则 |
| M008-009 | Codex V0.05旧009重判；Grok V0.05旧009重判及对应技术观察 | 技术性 | 否 | 否 | anchors当前0，旧六红不再现；ledger当前仅PGM无同步红归M426，工具不放宽；GEN-002/008/016，证据类型2/4。无业务发明：检查/生成/证据，不增加业务对象或规则 |
| M008-010 | Codex V0.05旧010重判；Grok V0.05旧010重判及对应技术观察 | 技术性 | 否 | 否 | 十历史log均完整读，E裸ignore旧绿现红、基线旧6现0、99旧全链绿不作本轮验证；保留历史；GEN-001/011/016，证据类型2/4。无业务发明：检查/生成/证据，不增加业务对象或规则 |
| M008-011 | Codex V0.05旧011重判；Grok V0.05旧011重判及对应技术观察 | 技术性 | 否 | 否 | m001-regression278行，本次双方实际运行通过；Codex77PASS，正常断言有效；未挂npm/裸assert早失败不强制改；GEN-003/013/018，证据类型2/4。无业务发明：检查/生成/证据，不增加业务对象或规则 |
| M008-012 | Codex V0.05旧012重判；Grok V0.05旧012重判及对应技术观察 | 技术性 | 否 | 否 | 文件级ledger检查/变更HTML才扫，双方隔离3HTML全扫0；无新增语义检查；GEN-002/006/009，证据类型2/4。无业务发明：检查/生成/证据，不增加业务对象或规则 |
| M008-013 | Codex V0.05旧013重判；Grok V0.05旧013重判及对应技术观察 | 技术性 | 否 | 否 | anchors40 -report偏宽，尚无正常误排误绿证据，仅登记，不新增规则；GEN-002/016，证据类型2/4。无业务发明：检查/生成/证据，不增加业务对象或规则 |
| M008-014 | Codex V0.05旧014重判；Grok V0.05旧014重判及对应技术观察 | 技术性 | 否 | 否 | CLI内部PASS/FAIL/DRIFT非产品UI；B/C业务授权不延伸CLI；GEN-015/017，证据类型2/4。无业务发明：检查/生成/证据，不增加业务对象或规则 |
| M008-015 | Codex V0.05旧015重判；Grok V0.05旧015重判及对应技术观察 | 技术性 | 否 | 否 | refresh51 UTC取证日期非营业日，不新增时区参数；GEN-002/016，证据类型2/4。无业务发明：检查/生成/证据，不增加业务对象或规则 |
| M008-016 | Codex V0.05旧016重判；Grok V0.05旧016重判及对应技术观察 | 技术性 | 否 | 否 | 17文件1605行均工程工具/证据；PGM036:611物理设备绑定无本模块实现面；PGM-036/GEN-017/018，证据类型2/4/1。无业务发明：检查/生成/证据，不增加业务对象或规则 |
全部当前工程缺陷否/队列否。分类逐项技术性，非业务发明。原六项历史已修证据保留，不因有ID再重复修。B2观察6仓根调用仅路由008，不改变008资料漂移对象；观察10外模块runtime仅路由009，不把M409/M415偷换为M426。

## 顺从与对抗完整结论
顺从：5个当前入口输出符合约定；6工具/基线和回归有用途，历史日志没有写成现行实现。GEN及PGM036全覆盖，PGM611“设备要绑定员工、班次、点位和状态”无本模块物理设备实现面，N/A不等于设备域通过。
对抗：本轮77反例覆盖echo/相似名/注释、LF边界、CSV空body、台账删除、baseline注入/key扩张、豁免范围、生成器两实际句遗漏；对应源字节本节点全等，无新引入绕过。当前实跑红没有被旁路，限定资料处理归M426/M435/M436；runtime另属M409/M415。未发现需要扩判据/新增角色/新功能的首次正常失败。

## 本轮复用证据位置与边界
本席modules/M008-b1-self-raw.md V0.05封存前189813bytes SHAc2f218e30a811a7e064faae25f35e99b6b7f44a01f3d9104d6e75ceb07f47702含77回归输出摘要及隔离副本完整命令结果：三HTML重建0entries，实际entry126/160与README更新732/V431/399/147后check0。该命令运行在绝对临时目录，主资料未改。本次B5没有宣称重跑77；五真实入口的fresh输出完整列在上面。
源码全17 gitstatus空，主HEAD e456ba96…未变、实际index d9a65108…和hook46aabdf…未变。未fetch/推送/提交/部署，未改三HTML/判据；无新增finding/待答questions/修改申请。

## 本席B5结论
PASS。这是本次完整M008 B5独立一票，不是继承B1/B2票。模块仍IN_PROGRESS，必须等待Grok当前B5完整报告并四值全等才收口；B3仍2。两项资料红保留，不称全链通过。
