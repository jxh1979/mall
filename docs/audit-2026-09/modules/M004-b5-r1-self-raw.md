CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
# M004 B5-r1 修复后全模块审计（本方）
版本：V0.01

本席结论：PASS。本次完整模块审完，无CX新finding，无待修、添加项或HTML修改申请。只是本席B5票，尚未读取Grok本次报告；不得据此提前模块PASS或发布。

M004/322，01-root-config-and-guidance-02，OVERSIZE-SINGLE-FILE，唯一源package-lock.json；当前IN_PROGRESS，B3累计1，运行失败重跑0，控制V0.17。全3321行/220记录/18GEN按第一性原理、顺从式、对抗式重新审查，不以B4通过代替全模块重审。

## 输入、逐字段阅读与独立性

当前源114242字节、3321行、SHA256 65254915c5c85e3bf5b26659d0acd27b2bdfc94c6b5954d15e4db81a6877e6bc。18GEN从冻结criteria-lock.md逐条读出；actual哈希见首行，sidecar/ledger已冻结同值，COUNT0。本次只映射GEN001—018，无HTML派生ID。findings V0.46、160801字节/SHA723b74bdc955a8f38a227aa1147893264be6574a1077eb81dd33c90ca6de7fef，六ID队列均否；这不是不再审它们的理由。

先派Grok B5到modules/M004-b5-r1-grok-raw.md，再完成本方完整源阅读/独立实际检查并写本raw；截至落盘未读Grok本次报告或原始JSON。没有读取其他席本次分析，没有新建当前工作区tree/完整diff去带出审计raw。对外席先前B4受正文暴露影响的旧票已作废、不借其结论；本B5只用当前源和实际事实。

全部220 records的所有语义字段分001—075、076—150、151—220三批规范化读取；resolved/integrity两个较长字段逐217条机械检查，再对216去重name/version逐个访问官方registry精确核同值。workspace link的resolved=apps/web另完整读取并由Arborist验证目标。没有将机器值未逐字朗读当作未校验。顶层L1—7包含name、应用version、lockfileVersion、documentVersion、requires与packages开口；末L3320—3321闭合由严格JSON解析及行覆盖确认。无重复JSON键。

元数据字段实际集合：bin/cpu/dependencies/dev/devDependencies/engines/funding/hasInstallScript/integrity/license/link/name/optional/optionalDependencies/os/peer/peerDependencies/peerDependenciesMeta/resolved/version/workspaces。逐记录语义完整列于下表；registry节点URL/SRI另外全量校验，没有把SRI匹配当安全无漏洞。保留上游许可证/资助字段不等于发明业务权限或收费。

## 18项冻结判据逐项裁决

| ID | 结果与当前完整模块证据 |
|---|---|
| GEN-001 必要性/最小性 | PASS。完整安装图从根遍历可达220/220、孤立记录0。根清单/工作区/链接、runtime/dev/optional/peer各类均承担既有npm安装功能。两条pluginutils相同版本在不同物理解析位置，不按去重name误删必要路径；不同fsevents/react-is/dom-accessibility-api嵌套版本满足各自父范围。非协议documentVersion只实现用户被改文件自身Vx.xx要求，不改应用semver，不构建多余业务抽象。 |
| GEN-002 可执行正确性 | PASS。本次npm ls lock-only和ci dry-run均exit0；Arborist完整582依赖边valid。相同源码SHA的npm10.9.8真实隔离ci已在本方B4实际exit0（见注明阶段的原输出），三包版本/正常解析与回归断言均执行；本次不把它冒记为重跑。没有声明主环境已安装新库或整站已部署。 |
| GEN-003 测试有效性 | PASS。本次内存摘除react反例令50个required引用无效，验证图校验能失败；断言不是只比较实现常量。配置修复按控制不强制新增测试文件。此前同源真实安装后的库断言补证行为，未加测试框架。 |
| GEN-004 信任边界 | PASS。全部217 tarball URL为官方HTTPS、包名/版本/文件名精确对应，无用户名密码、路径越界或任意git/file远端。每个sha512合法且decode64字节；216官方元数据实际返回并逐一与217条对比无差异。唯一workspace link是受控apps/web，有根workspaces与实际目录支持。 |
| GEN-005 授权/最小权限 | PASS（无此类运行路径）。全文件只有软件安装元数据，不含服务端主体/对象/动作或业务授权入口；没有以前端组件配置替代后端鉴权的命题，本模块不伪称验证了全仓权限。 |
| GEN-006 数据完整性 | PASS。JSON无重键、所有包引用可解析、根与web清单镜像字段deepEqual；完整路径图无无效边。无金额或业务状态写入，不能给静态锁凭空加事务。 |
| GEN-007 并发/幂等 | PASS（无此类运行路径）。静态锁无请求处理、消息重投、数据库事务或可变共享缓存；npm消费锁以确定版本安装，命令前后源码哈希相同。没有新增应用去重状态。 |
| GEN-008 接口/模式一致性 | PASS。lockfileVersion3、根name/version=0.1.0、工作区清单镜像及link目标一致；父semver与所有582解析边相容，peer optional缺失与required区别由原生工具判。documentVersion为非协议扩展，真实npm10消费保留，不占用version/lockfileVersion。 |
| GEN-009 错误处理/恢复 | PASS。全部实际命令记录真实exit0，图断链反例可失败；无吞错业务逻辑。Undici当前7.29.0消掉B1已证空/混合private解析异常，同源SRI与真实安装断言可核。 |
| GEN-010 安全/敏感信息 | PASS（当前模块事实）。全量npm audit与生产omitdev均exit0/total0；三旧库缺陷已经锁到兼容修补版。URL无凭据、无业务秘密。扫描0仅代表此次数据库及本锁，不声称所有漏洞均已知或线上全仓无风险；未把开发库问题说成生产已利用。 |
| GEN-011 可观测/证据 | PASS。name/version/URL/SRI/路径可追溯；当前源hash、原生命令/结构计数可复现。没有运行期关键交易操作，无须给静态锁新增业务日志或审计事件。 |
| GEN-012 资源/性能 | PASS。220节点582边为有限安装图；B1两个NanoID无界反例在3.3.18已消失，同源实际安装断言可核。平台optional包按os/cpu筛选，不能因跨平台记录多就删它们；验证临时目录已删除，主node_modules未动。 |
| GEN-013 构建配置/依赖 | PASS。根/web清单镜像、版本边、peer/engines、各平台记录均可解析；实际npm10隔离ci+宿主npm11预演成功。项目packageManager npm10.9.8、CI setup-node22与ci --ignore-scripts直接引用确认。锁内engines为官方包元数据；根>=22宽于少数包^22.12/^22.13作为既有宿主要求差异记录，不伪改上游engines。当前正常CI取22最新与宿主24符合，不由本模块扩修下一模块清单。 |
| GEN-014 数据库迁移 | PASS（无此类内容）。全文件无SQL/迁移次序/数据删除；未声称检查了全仓迁移。 |
| GEN-015 界面可用性 | PASS（无此类内容）。依赖元数据不是用户界面；未改焦点/标签/语言呈现，也不因含UI库名直接发明业务页面。 |
| GEN-016 文档可验证性 | PASS。当前软件名/版本和相应官方dist逐项同值；根/web文件存在。documentVersion是文件版本、0.1.0应用版本、3协议格式，语义已分开，不把计划修补写成既已安装到主环境。 |
| GEN-017 业务来源分类 | PASS。全220逐项按冲突→细化→技术性→添加审：没有HTML业务冲突或可指向具体业务条目的业务实现细节；仅决定现有构建/测试/runtime库如何被npm安装，无票种/计费/角色/状态/审批发明，归技术性；无需HTML逐包锚点。下表全登记。 |
| GEN-018 第一性原理证据 | PASS。类型3是npm/官方registry协议事实，类型4为本次原生解析/安全查询/图负例输出，类型2/4为同源码真实安装与库行为。过去席位PASS本身不作为依据；所有引用历史执行均明确相同源SHA和阶段。无类型1业务HTML推导请求。 |

## 全部220记录逐项技术登记

每条均归技术性/无业务发明/当前未见工程缺陷；类型3/4。各字段既服务安装元数据又没有另起业务规则。表中不省略语义字段；每个registry节点的resolved/integrity已以包名版本映射及全量registry查询独立校验，故表内以检验结论代替两段长机器值。当前文件完整行范围在表内逐条覆盖；顶层及闭合见上文。

| # | 源行 / 路径 | 无业务发明的用途 | 完整语义字段（URL/SRI另全量核验） |
|---|---|---|---|
| 1 | L8—21 (root) | 根安装清单镜像与workspace入口，不定义业务 | {"name":"entertainment-complex-management","version":"0.1.0","workspaces":["apps/web"],"devDependencies":{"@playwright/test":"^1.61.0","yaml":"^2.9.0"},"engines":{"node":">=22"}} |
| 2 | L22—44 apps/web | 现有web工作区runtime/dev清单镜像，不新增页面规则 | {"name":"apps-web","version":"0.1.0","dependencies":{"@ant-design/icons":"^5.5.2","antd":"^5.22.5","qrcode.react":"^4.2.0","react":"^19.0.0","react-dom":"^19.0.0"},"devDependencies":{"@testing-library/jest-dom":"^6.9.1","@testing-library/react":"^16.3.2","@testing-library/user-event":"^14.6.1","@types/react":"^19.2.16","@types/react-dom":"^19.2.3","@vitejs/plugin-react":"^6.0.2","jsdom":"^29.1.1","typescript":"^5.7.2","vite":"^8.0.16","vitest":"^4.1.9"}} |
| 3 | L45—51 apps/web/node_modules/@rolldown/pluginutils | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"1.0.1","dev":true,"license":"MIT"} |
| 4 | L52—77 apps/web/node_modules/@vitejs/plugin-react | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"6.0.2","dev":true,"license":"MIT","dependencies":{"@rolldown/pluginutils":"^1.0.0"},"engines":{"node":"^20.19.0 \|\| >=22.12.0"},"peerDependencies":{"@rolldown/plugin-babel":"^0.1.7 \|\| ^0.2.0","babel-plugin-react-compiler":"^1.0.0","vite":"^8.0.0"},"peerDependenciesMeta":{"@rolldown/plugin-babel":{"optional":true},"babel-plugin-react-compiler":{"optional":true}}} |
| 5 | L78—84 node_modules/@adobe/css-tools | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"4.5.0","dev":true,"license":"MIT"} |
| 6 | L85—93 node_modules/@ant-design/colors | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"7.2.1","license":"MIT","dependencies":{"@ant-design/fast-color":"^2.0.6"}} |
| 7 | L94—112 node_modules/@ant-design/cssinjs | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"1.24.0","license":"MIT","dependencies":{"@babel/runtime":"^7.11.1","@emotion/hash":"^0.8.0","@emotion/unitless":"^0.7.5","classnames":"^2.3.1","csstype":"^3.1.3","rc-util":"^5.35.0","stylis":"^4.3.4"},"peerDependencies":{"react":">=16.0.0","react-dom":">=16.0.0"}} |
| 8 | L113—127 node_modules/@ant-design/cssinjs-utils | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"1.1.3","license":"MIT","dependencies":{"@ant-design/cssinjs":"^1.21.0","@babel/runtime":"^7.23.2","rc-util":"^5.38.0"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 9 | L128—139 node_modules/@ant-design/fast-color | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"2.0.6","license":"MIT","dependencies":{"@babel/runtime":"^7.24.7"},"engines":{"node":">=8.x"}} |
| 10 | L140—159 node_modules/@ant-design/icons | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"5.6.1","license":"MIT","dependencies":{"@ant-design/colors":"^7.0.0","@ant-design/icons-svg":"^4.4.0","@babel/runtime":"^7.24.8","classnames":"^2.2.6","rc-util":"^5.31.1"},"engines":{"node":">=8"},"peerDependencies":{"react":">=16.0.0","react-dom":">=16.0.0"}} |
| 11 | L160—165 node_modules/@ant-design/icons-svg | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"4.4.2","license":"MIT"} |
| 12 | L166—181 node_modules/@ant-design/react-slick | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"1.1.2","license":"MIT","dependencies":{"@babel/runtime":"^7.10.4","classnames":"^2.2.5","json2mq":"^0.2.0","resize-observer-polyfill":"^1.5.1","throttle-debounce":"^5.0.0"},"peerDependencies":{"react":">=16.9.0"}} |
| 13 | L182—198 node_modules/@asamuzakjp/css-color | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"5.1.11","dev":true,"license":"MIT","dependencies":{"@asamuzakjp/generational-cache":"^1.0.1","@csstools/css-calc":"^3.2.0","@csstools/css-color-parser":"^4.1.0","@csstools/css-parser-algorithms":"^4.0.0","@csstools/css-tokenizer":"^4.0.0"},"engines":{"node":"^20.19.0 \|\| ^22.12.0 \|\| >=24.0.0"}} |
| 14 | L199—215 node_modules/@asamuzakjp/dom-selector | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"7.1.1","dev":true,"license":"MIT","dependencies":{"@asamuzakjp/generational-cache":"^1.0.1","@asamuzakjp/nwsapi":"^2.3.9","bidi-js":"^1.0.3","css-tree":"^3.2.1","is-potential-custom-element-name":"^1.0.1"},"engines":{"node":"^20.19.0 \|\| ^22.12.0 \|\| >=24.0.0"}} |
| 15 | L216—225 node_modules/@asamuzakjp/generational-cache | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"1.0.1","dev":true,"license":"MIT","engines":{"node":"^20.19.0 \|\| ^22.12.0 \|\| >=24.0.0"}} |
| 16 | L226—232 node_modules/@asamuzakjp/nwsapi | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"2.3.9","dev":true,"license":"MIT"} |
| 17 | L233—248 node_modules/@babel/code-frame | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"7.29.7","dev":true,"license":"MIT","peer":true,"dependencies":{"@babel/helper-validator-identifier":"^7.29.7","js-tokens":"^4.0.0","picocolors":"^1.1.1"},"engines":{"node":">=6.9.0"}} |
| 18 | L249—259 node_modules/@babel/helper-validator-identifier | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"7.29.7","dev":true,"license":"MIT","peer":true,"engines":{"node":">=6.9.0"}} |
| 19 | L260—268 node_modules/@babel/runtime | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"7.29.7","license":"MIT","engines":{"node":">=6.9.0"}} |
| 20 | L269—281 node_modules/@bramus/specificity | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"2.4.2","dev":true,"license":"MIT","dependencies":{"css-tree":"^3.0.0"},"bin":{"specificity":"bin/cli.js"}} |
| 21 | L282—301 node_modules/@csstools/color-helpers | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"6.0.2","dev":true,"funding":[{"type":"github","url":"https://github.com/sponsors/csstools"},{"type":"opencollective","url":"https://opencollective.com/csstools"}],"license":"MIT-0","engines":{"node":">=20.19.0"}} |
| 22 | L302—325 node_modules/@csstools/css-calc | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"3.2.1","dev":true,"funding":[{"type":"github","url":"https://github.com/sponsors/csstools"},{"type":"opencollective","url":"https://opencollective.com/csstools"}],"license":"MIT","engines":{"node":">=20.19.0"},"peerDependencies":{"@csstools/css-parser-algorithms":"^4.0.0","@csstools/css-tokenizer":"^4.0.0"}} |
| 23 | L326—353 node_modules/@csstools/css-color-parser | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"4.1.7","dev":true,"funding":[{"type":"github","url":"https://github.com/sponsors/csstools"},{"type":"opencollective","url":"https://opencollective.com/csstools"}],"license":"MIT","dependencies":{"@csstools/color-helpers":"^6.0.2","@csstools/css-calc":"^3.2.1"},"engines":{"node":">=20.19.0"},"peerDependencies":{"@csstools/css-parser-algorithms":"^4.0.0","@csstools/css-tokenizer":"^4.0.0"}} |
| 24 | L354—376 node_modules/@csstools/css-parser-algorithms | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"4.0.0","dev":true,"funding":[{"type":"github","url":"https://github.com/sponsors/csstools"},{"type":"opencollective","url":"https://opencollective.com/csstools"}],"license":"MIT","engines":{"node":">=20.19.0"},"peerDependencies":{"@csstools/css-tokenizer":"^4.0.0"}} |
| 25 | L377—401 node_modules/@csstools/css-syntax-patches-for-csstree | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"1.1.5","dev":true,"funding":[{"type":"github","url":"https://github.com/sponsors/csstools"},{"type":"opencollective","url":"https://opencollective.com/csstools"}],"license":"MIT-0","peerDependencies":{"css-tree":"^3.2.1"},"peerDependenciesMeta":{"css-tree":{"optional":true}}} |
| 26 | L402—421 node_modules/@csstools/css-tokenizer | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"4.0.0","dev":true,"funding":[{"type":"github","url":"https://github.com/sponsors/csstools"},{"type":"opencollective","url":"https://opencollective.com/csstools"}],"license":"MIT","engines":{"node":">=20.19.0"}} |
| 27 | L422—433 node_modules/@emnapi/core | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.10.0","dev":true,"license":"MIT","optional":true,"dependencies":{"@emnapi/wasi-threads":"1.2.1","tslib":"^2.4.0"}} |
| 28 | L434—444 node_modules/@emnapi/runtime | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.10.0","dev":true,"license":"MIT","optional":true,"dependencies":{"tslib":"^2.4.0"}} |
| 29 | L445—455 node_modules/@emnapi/wasi-threads | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.2.1","dev":true,"license":"MIT","optional":true,"dependencies":{"tslib":"^2.4.0"}} |
| 30 | L456—461 node_modules/@emotion/hash | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"0.8.0","license":"MIT"} |
| 31 | L462—467 node_modules/@emotion/unitless | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"0.7.5","license":"MIT"} |
| 32 | L468—485 node_modules/@exodus/bytes | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"1.15.1","dev":true,"license":"MIT","engines":{"node":"^20.19.0 \|\| ^22.12.0 \|\| >=24.0.0"},"peerDependencies":{"@noble/hashes":"^1.8.0 \|\| ^2.0.0"},"peerDependenciesMeta":{"@noble/hashes":{"optional":true}}} |
| 33 | L486—492 node_modules/@jridgewell/sourcemap-codec | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"1.5.5","dev":true,"license":"MIT"} |
| 34 | L493—511 node_modules/@napi-rs/wasm-runtime | 开发工具固定版本和上游引用；可选平台/运行支持；peer解析要求；全图可达、无业务规则 | {"version":"1.1.5","dev":true,"license":"MIT","optional":true,"dependencies":{"@tybys/wasm-util":"^0.10.2"},"funding":{"type":"github","url":"https://github.com/sponsors/Brooooooklyn"},"peerDependencies":{"@emnapi/core":"^1.7.1","@emnapi/runtime":"^1.7.1"}} |
| 35 | L512—521 node_modules/@oxc-project/types | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"0.133.0","dev":true,"license":"MIT","funding":{"url":"https://github.com/sponsors/Boshen"}} |
| 36 | L522—537 node_modules/@playwright/test | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"1.61.0","dev":true,"license":"Apache-2.0","dependencies":{"playwright":"1.61.0"},"bin":{"playwright":"cli.js"},"engines":{"node":">=18"}} |
| 37 | L538—549 node_modules/@rc-component/async-validator | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"5.1.0","license":"MIT","dependencies":{"@babel/runtime":"^7.24.4"},"engines":{"node":">=14.x"}} |
| 38 | L550—565 node_modules/@rc-component/color-picker | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"2.0.1","license":"MIT","dependencies":{"@ant-design/fast-color":"^2.0.6","@babel/runtime":"^7.23.6","classnames":"^2.2.6","rc-util":"^5.38.1"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 39 | L566—579 node_modules/@rc-component/context | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"1.4.0","license":"MIT","dependencies":{"@babel/runtime":"^7.10.1","rc-util":"^5.27.0"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 40 | L580—591 node_modules/@rc-component/mini-decimal | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"1.1.3","license":"MIT","dependencies":{"@babel/runtime":"^7.18.0"},"engines":{"node":">=8.x"}} |
| 41 | L592—609 node_modules/@rc-component/mutate-observer | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"1.1.0","license":"MIT","dependencies":{"@babel/runtime":"^7.18.0","classnames":"^2.3.2","rc-util":"^5.24.4"},"engines":{"node":">=8.x"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 42 | L610—627 node_modules/@rc-component/portal | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"1.1.2","license":"MIT","dependencies":{"@babel/runtime":"^7.18.0","classnames":"^2.3.2","rc-util":"^5.24.4"},"engines":{"node":">=8.x"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 43 | L628—643 node_modules/@rc-component/qrcode | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"1.1.1","license":"MIT","dependencies":{"@babel/runtime":"^7.24.7"},"engines":{"node":">=8.x"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 44 | L644—663 node_modules/@rc-component/tour | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"1.15.1","license":"MIT","dependencies":{"@babel/runtime":"^7.18.0","@rc-component/portal":"^1.0.0-9","@rc-component/trigger":"^2.0.0","classnames":"^2.3.2","rc-util":"^5.24.4"},"engines":{"node":">=8.x"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 45 | L664—684 node_modules/@rc-component/trigger | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"2.3.1","license":"MIT","dependencies":{"@babel/runtime":"^7.23.2","@rc-component/portal":"^1.1.0","classnames":"^2.3.2","rc-motion":"^2.0.0","rc-resize-observer":"^1.3.1","rc-util":"^5.44.0"},"engines":{"node":">=8.x"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 46 | L685—701 node_modules/@rolldown/binding-android-arm64 | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.0.3","cpu":["arm64"],"dev":true,"license":"MIT","optional":true,"os":["android"],"engines":{"node":"^20.19.0 \|\| >=22.12.0"}} |
| 47 | L702—718 node_modules/@rolldown/binding-darwin-arm64 | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.0.3","cpu":["arm64"],"dev":true,"license":"MIT","optional":true,"os":["darwin"],"engines":{"node":"^20.19.0 \|\| >=22.12.0"}} |
| 48 | L719—735 node_modules/@rolldown/binding-darwin-x64 | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.0.3","cpu":["x64"],"dev":true,"license":"MIT","optional":true,"os":["darwin"],"engines":{"node":"^20.19.0 \|\| >=22.12.0"}} |
| 49 | L736—752 node_modules/@rolldown/binding-freebsd-x64 | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.0.3","cpu":["x64"],"dev":true,"license":"MIT","optional":true,"os":["freebsd"],"engines":{"node":"^20.19.0 \|\| >=22.12.0"}} |
| 50 | L753—769 node_modules/@rolldown/binding-linux-arm-gnueabihf | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.0.3","cpu":["arm"],"dev":true,"license":"MIT","optional":true,"os":["linux"],"engines":{"node":"^20.19.0 \|\| >=22.12.0"}} |
| 51 | L770—786 node_modules/@rolldown/binding-linux-arm64-gnu | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.0.3","cpu":["arm64"],"dev":true,"license":"MIT","optional":true,"os":["linux"],"engines":{"node":"^20.19.0 \|\| >=22.12.0"}} |
| 52 | L787—803 node_modules/@rolldown/binding-linux-arm64-musl | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.0.3","cpu":["arm64"],"dev":true,"license":"MIT","optional":true,"os":["linux"],"engines":{"node":"^20.19.0 \|\| >=22.12.0"}} |
| 53 | L804—820 node_modules/@rolldown/binding-linux-ppc64-gnu | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.0.3","cpu":["ppc64"],"dev":true,"license":"MIT","optional":true,"os":["linux"],"engines":{"node":"^20.19.0 \|\| >=22.12.0"}} |
| 54 | L821—837 node_modules/@rolldown/binding-linux-s390x-gnu | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.0.3","cpu":["s390x"],"dev":true,"license":"MIT","optional":true,"os":["linux"],"engines":{"node":"^20.19.0 \|\| >=22.12.0"}} |
| 55 | L838—854 node_modules/@rolldown/binding-linux-x64-gnu | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.0.3","cpu":["x64"],"dev":true,"license":"MIT","optional":true,"os":["linux"],"engines":{"node":"^20.19.0 \|\| >=22.12.0"}} |
| 56 | L855—871 node_modules/@rolldown/binding-linux-x64-musl | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.0.3","cpu":["x64"],"dev":true,"license":"MIT","optional":true,"os":["linux"],"engines":{"node":"^20.19.0 \|\| >=22.12.0"}} |
| 57 | L872—888 node_modules/@rolldown/binding-openharmony-arm64 | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.0.3","cpu":["arm64"],"dev":true,"license":"MIT","optional":true,"os":["openharmony"],"engines":{"node":"^20.19.0 \|\| >=22.12.0"}} |
| 58 | L889—907 node_modules/@rolldown/binding-wasm32-wasi | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.0.3","cpu":["wasm32"],"dev":true,"license":"MIT","optional":true,"dependencies":{"@emnapi/core":"1.10.0","@emnapi/runtime":"1.10.0","@napi-rs/wasm-runtime":"^1.1.4"},"engines":{"node":"^20.19.0 \|\| >=22.12.0"}} |
| 59 | L908—924 node_modules/@rolldown/binding-win32-arm64-msvc | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.0.3","cpu":["arm64"],"dev":true,"license":"MIT","optional":true,"os":["win32"],"engines":{"node":"^20.19.0 \|\| >=22.12.0"}} |
| 60 | L925—941 node_modules/@rolldown/binding-win32-x64-msvc | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.0.3","cpu":["x64"],"dev":true,"license":"MIT","optional":true,"os":["win32"],"engines":{"node":"^20.19.0 \|\| >=22.12.0"}} |
| 61 | L942—948 node_modules/@standard-schema/spec | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"1.1.0","dev":true,"license":"MIT"} |
| 62 | L949—969 node_modules/@testing-library/dom | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"10.4.1","dev":true,"license":"MIT","peer":true,"dependencies":{"@babel/code-frame":"^7.10.4","@babel/runtime":"^7.12.5","@types/aria-query":"^5.0.1","aria-query":"5.3.0","dom-accessibility-api":"^0.5.9","lz-string":"^1.5.0","picocolors":"1.1.1","pretty-format":"^27.0.2"},"engines":{"node":">=18"}} |
| 63 | L970—989 node_modules/@testing-library/jest-dom | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"6.9.1","dev":true,"license":"MIT","dependencies":{"@adobe/css-tools":"^4.4.0","aria-query":"^5.0.0","css.escape":"^1.5.1","dom-accessibility-api":"^0.6.3","picocolors":"^1.1.1","redent":"^3.0.0"},"engines":{"node":">=14","npm":">=6","yarn":">=1"}} |
| 64 | L990—996 node_modules/@testing-library/jest-dom/node_modules/dom-accessibility-api | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"0.6.3","dev":true,"license":"MIT"} |
| 65 | L997—1024 node_modules/@testing-library/react | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"16.3.2","dev":true,"license":"MIT","dependencies":{"@babel/runtime":"^7.12.5"},"engines":{"node":">=18"},"peerDependencies":{"@testing-library/dom":"^10.0.0","@types/react":"^18.0.0 \|\| ^19.0.0","@types/react-dom":"^18.0.0 \|\| ^19.0.0","react":"^18.0.0 \|\| ^19.0.0","react-dom":"^18.0.0 \|\| ^19.0.0"},"peerDependenciesMeta":{"@types/react":{"optional":true},"@types/react-dom":{"optional":true}}} |
| 66 | L1025—1038 node_modules/@testing-library/user-event | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"14.6.1","dev":true,"license":"MIT","engines":{"node":">=12","npm":">=6"},"peerDependencies":{"@testing-library/dom":">=7.21.4"}} |
| 67 | L1039—1049 node_modules/@tybys/wasm-util | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"0.10.2","dev":true,"license":"MIT","optional":true,"dependencies":{"tslib":"^2.4.0"}} |
| 68 | L1050—1057 node_modules/@types/aria-query | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"5.0.4","dev":true,"license":"MIT","peer":true} |
| 69 | L1058—1068 node_modules/@types/chai | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"5.2.3","dev":true,"license":"MIT","dependencies":{"@types/deep-eql":"*","assertion-error":"^2.0.1"}} |
| 70 | L1069—1075 node_modules/@types/deep-eql | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"4.0.2","dev":true,"license":"MIT"} |
| 71 | L1076—1082 node_modules/@types/estree | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"1.0.9","dev":true,"license":"MIT"} |
| 72 | L1083—1092 node_modules/@types/react | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"19.2.16","dev":true,"license":"MIT","dependencies":{"csstype":"^3.2.2"}} |
| 73 | L1093—1102 node_modules/@types/react-dom | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"19.2.3","dev":true,"license":"MIT","peerDependencies":{"@types/react":"^19.2.0"}} |
| 74 | L1103—1120 node_modules/@vitest/expect | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"4.1.9","dev":true,"license":"MIT","dependencies":{"@standard-schema/spec":"^1.1.0","@types/chai":"^5.2.2","@vitest/spy":"4.1.9","@vitest/utils":"4.1.9","chai":"^6.2.2","tinyrainbow":"^3.1.0"},"funding":{"url":"https://opencollective.com/vitest"}} |
| 75 | L1121—1147 node_modules/@vitest/mocker | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"4.1.9","dev":true,"license":"MIT","dependencies":{"@vitest/spy":"4.1.9","estree-walker":"^3.0.3","magic-string":"^0.30.21"},"funding":{"url":"https://opencollective.com/vitest"},"peerDependencies":{"msw":"^2.4.9","vite":"^6.0.0 \|\| ^7.0.0 \|\| ^8.0.0"},"peerDependenciesMeta":{"msw":{"optional":true},"vite":{"optional":true}}} |
| 76 | L1148—1160 node_modules/@vitest/pretty-format | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"4.1.9","dev":true,"license":"MIT","dependencies":{"tinyrainbow":"^3.1.0"},"funding":{"url":"https://opencollective.com/vitest"}} |
| 77 | L1161—1174 node_modules/@vitest/runner | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"4.1.9","dev":true,"license":"MIT","dependencies":{"@vitest/utils":"4.1.9","pathe":"^2.0.3"},"funding":{"url":"https://opencollective.com/vitest"}} |
| 78 | L1175—1190 node_modules/@vitest/snapshot | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"4.1.9","dev":true,"license":"MIT","dependencies":{"@vitest/pretty-format":"4.1.9","@vitest/utils":"4.1.9","magic-string":"^0.30.21","pathe":"^2.0.3"},"funding":{"url":"https://opencollective.com/vitest"}} |
| 79 | L1191—1200 node_modules/@vitest/spy | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"4.1.9","dev":true,"license":"MIT","funding":{"url":"https://opencollective.com/vitest"}} |
| 80 | L1201—1215 node_modules/@vitest/utils | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"4.1.9","dev":true,"license":"MIT","dependencies":{"@vitest/pretty-format":"4.1.9","convert-source-map":"^2.0.0","tinyrainbow":"^3.1.0"},"funding":{"url":"https://opencollective.com/vitest"}} |
| 81 | L1216—1226 node_modules/ansi-regex | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"5.0.1","dev":true,"license":"MIT","peer":true,"engines":{"node":">=8"}} |
| 82 | L1227—1240 node_modules/ansi-styles | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"5.2.0","dev":true,"license":"MIT","peer":true,"engines":{"node":">=10"},"funding":{"url":"https://github.com/chalk/ansi-styles?sponsor=1"}} |
| 83 | L1241—1305 node_modules/antd | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"5.29.3","license":"MIT","dependencies":{"@ant-design/colors":"^7.2.1","@ant-design/cssinjs":"^1.23.0","@ant-design/cssinjs-utils":"^1.1.3","@ant-design/fast-color":"^2.0.6","@ant-design/icons":"^5.6.1","@ant-design/react-slick":"~1.1.2","@babel/runtime":"^7.26.0","@rc-component/color-picker":"~2.0.1","@rc-component/mutate-observer":"^1.1.0","@rc-component/qrcode":"~1.1.0","@rc-component/tour":"~1.15.1","@rc-component/trigger":"^2.3.0","classnames":"^2.5.1","copy-to-clipboard":"^3.3.3","dayjs":"^1.11.11","rc-cascader":"~3.34.0","rc-checkbox":"~3.5.0","rc-collapse":"~3.9.0","rc-dialog":"~9.6.0","rc-drawer":"~7.3.0","rc-dropdown":"~4.2.1","rc-field-form":"~2.7.1","rc-image":"~7.12.0","rc-input":"~1.8.0","rc-input-number":"~9.5.0","rc-mentions":"~2.20.0","rc-menu":"~9.16.1","rc-motion":"^2.9.5","rc-notification":"~5.6.4","rc-pagination":"~5.1.0","rc-picker":"~4.11.3","rc-progress":"~4.0.0","rc-rate":"~2.13.1","rc-resize-observer":"^1.4.3","rc-segmented":"~2.7.0","rc-select":"~14.16.8","rc-slider":"~11.1.9","rc-steps":"~6.0.1","rc-switch":"~4.1.0","rc-table":"~7.54.0","rc-tabs":"~15.7.0","rc-textarea":"~1.10.2","rc-tooltip":"~6.4.0","rc-tree":"~5.13.1","rc-tree-select":"~5.27.0","rc-upload":"~4.11.0","rc-util":"^5.44.4","scroll-into-view-if-needed":"^3.1.0","throttle-debounce":"^5.0.2"},"funding":{"type":"opencollective","url":"https://opencollective.com/ant-design"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 84 | L1306—1309 node_modules/apps-web | workspace受控路径链接，不是业务对象链接 | {"resolved":"apps/web","link":true} |
| 85 | L1310—1319 node_modules/aria-query | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"5.3.0","dev":true,"license":"Apache-2.0","dependencies":{"dequal":"^2.0.3"}} |
| 86 | L1320—1329 node_modules/assertion-error | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"2.0.1","dev":true,"license":"MIT","engines":{"node":">=12"}} |
| 87 | L1330—1339 node_modules/bidi-js | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"1.0.3","dev":true,"license":"MIT","dependencies":{"require-from-string":"^2.0.2"}} |
| 88 | L1340—1349 node_modules/chai | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"6.2.2","dev":true,"license":"MIT","engines":{"node":">=18"}} |
| 89 | L1350—1355 node_modules/classnames | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"2.5.1","license":"MIT"} |
| 90 | L1356—1361 node_modules/compute-scroll-into-view | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"3.1.1","license":"MIT"} |
| 91 | L1362—1368 node_modules/convert-source-map | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"2.0.0","dev":true,"license":"MIT"} |
| 92 | L1369—1377 node_modules/copy-to-clipboard | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"3.3.3","license":"MIT","dependencies":{"toggle-selection":"^1.0.6"}} |
| 93 | L1378—1391 node_modules/css-tree | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"3.2.1","dev":true,"license":"MIT","dependencies":{"mdn-data":"2.27.1","source-map-js":"^1.2.1"},"engines":{"node":"^10 \|\| ^12.20.0 \|\| ^14.13.0 \|\| >=15.0.0"}} |
| 94 | L1392—1398 node_modules/css.escape | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"1.5.1","dev":true,"license":"MIT"} |
| 95 | L1399—1404 node_modules/csstype | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"3.2.3","license":"MIT"} |
| 96 | L1405—1418 node_modules/data-urls | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"7.0.0","dev":true,"license":"MIT","dependencies":{"whatwg-mimetype":"^5.0.0","whatwg-url":"^16.0.0"},"engines":{"node":"^20.19.0 \|\| ^22.12.0 \|\| >=24.0.0"}} |
| 97 | L1419—1424 node_modules/dayjs | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"1.11.21","license":"MIT"} |
| 98 | L1425—1431 node_modules/decimal.js | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"10.6.0","dev":true,"license":"MIT"} |
| 99 | L1432—1441 node_modules/dequal | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"2.0.3","dev":true,"license":"MIT","engines":{"node":">=6"}} |
| 100 | L1442—1451 node_modules/detect-libc | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"2.1.2","dev":true,"license":"Apache-2.0","engines":{"node":">=8"}} |
| 101 | L1452—1459 node_modules/dom-accessibility-api | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"0.5.16","dev":true,"license":"MIT","peer":true} |
| 102 | L1460—1472 node_modules/entities | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"8.0.0","dev":true,"license":"BSD-2-Clause","engines":{"node":">=20.19.0"},"funding":{"url":"https://github.com/fb55/entities?sponsor=1"}} |
| 103 | L1473—1479 node_modules/es-module-lexer | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"2.1.0","dev":true,"license":"MIT"} |
| 104 | L1480—1489 node_modules/estree-walker | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"3.0.3","dev":true,"license":"MIT","dependencies":{"@types/estree":"^1.0.0"}} |
| 105 | L1490—1499 node_modules/expect-type | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"1.3.0","dev":true,"license":"Apache-2.0","engines":{"node":">=12.0.0"}} |
| 106 | L1500—1517 node_modules/fdir | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"6.5.0","dev":true,"license":"MIT","engines":{"node":">=12.0.0"},"peerDependencies":{"picomatch":"^3 \|\| ^4"},"peerDependenciesMeta":{"picomatch":{"optional":true}}} |
| 107 | L1518—1532 node_modules/fsevents | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"2.3.3","dev":true,"hasInstallScript":true,"license":"MIT","optional":true,"os":["darwin"],"engines":{"node":"^8.16.0 \|\| ^10.6.0 \|\| >=11.0.0"}} |
| 108 | L1533—1545 node_modules/html-encoding-sniffer | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"6.0.0","dev":true,"license":"MIT","dependencies":{"@exodus/bytes":"^1.6.0"},"engines":{"node":"^20.19.0 \|\| ^22.12.0 \|\| >=24.0.0"}} |
| 109 | L1546—1555 node_modules/indent-string | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"4.0.0","dev":true,"license":"MIT","engines":{"node":">=8"}} |
| 110 | L1556—1562 node_modules/is-potential-custom-element-name | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"1.0.1","dev":true,"license":"MIT"} |
| 111 | L1563—1570 node_modules/js-tokens | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"4.0.0","dev":true,"license":"MIT","peer":true} |
| 112 | L1571—1611 node_modules/jsdom | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"29.1.1","dev":true,"license":"MIT","dependencies":{"@asamuzakjp/css-color":"^5.1.11","@asamuzakjp/dom-selector":"^7.1.1","@bramus/specificity":"^2.4.2","@csstools/css-syntax-patches-for-csstree":"^1.1.3","@exodus/bytes":"^1.15.0","css-tree":"^3.2.1","data-urls":"^7.0.0","decimal.js":"^10.6.0","html-encoding-sniffer":"^6.0.0","is-potential-custom-element-name":"^1.0.1","lru-cache":"^11.3.5","parse5":"^8.0.1","saxes":"^6.0.0","symbol-tree":"^3.2.4","tough-cookie":"^6.0.1","undici":"^7.25.0","w3c-xmlserializer":"^5.0.0","webidl-conversions":"^8.0.1","whatwg-mimetype":"^5.0.0","whatwg-url":"^16.0.1","xml-name-validator":"^5.0.0"},"engines":{"node":"^20.19.0 \|\| ^22.13.0 \|\| >=24.0.0"},"peerDependencies":{"canvas":"^3.0.0"},"peerDependenciesMeta":{"canvas":{"optional":true}}} |
| 113 | L1612—1620 node_modules/json2mq | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"0.2.0","license":"MIT","dependencies":{"string-convert":"^0.2.0"}} |
| 114 | L1621—1650 node_modules/lightningcss | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"1.32.0","dev":true,"license":"MPL-2.0","dependencies":{"detect-libc":"^2.0.3"},"engines":{"node":">= 12.0.0"},"funding":{"type":"opencollective","url":"https://opencollective.com/parcel"},"optionalDependencies":{"lightningcss-android-arm64":"1.32.0","lightningcss-darwin-arm64":"1.32.0","lightningcss-darwin-x64":"1.32.0","lightningcss-freebsd-x64":"1.32.0","lightningcss-linux-arm-gnueabihf":"1.32.0","lightningcss-linux-arm64-gnu":"1.32.0","lightningcss-linux-arm64-musl":"1.32.0","lightningcss-linux-x64-gnu":"1.32.0","lightningcss-linux-x64-musl":"1.32.0","lightningcss-win32-arm64-msvc":"1.32.0","lightningcss-win32-x64-msvc":"1.32.0"}} |
| 115 | L1651—1671 node_modules/lightningcss-android-arm64 | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.32.0","cpu":["arm64"],"dev":true,"license":"MPL-2.0","optional":true,"os":["android"],"engines":{"node":">= 12.0.0"},"funding":{"type":"opencollective","url":"https://opencollective.com/parcel"}} |
| 116 | L1672—1692 node_modules/lightningcss-darwin-arm64 | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.32.0","cpu":["arm64"],"dev":true,"license":"MPL-2.0","optional":true,"os":["darwin"],"engines":{"node":">= 12.0.0"},"funding":{"type":"opencollective","url":"https://opencollective.com/parcel"}} |
| 117 | L1693—1713 node_modules/lightningcss-darwin-x64 | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.32.0","cpu":["x64"],"dev":true,"license":"MPL-2.0","optional":true,"os":["darwin"],"engines":{"node":">= 12.0.0"},"funding":{"type":"opencollective","url":"https://opencollective.com/parcel"}} |
| 118 | L1714—1734 node_modules/lightningcss-freebsd-x64 | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.32.0","cpu":["x64"],"dev":true,"license":"MPL-2.0","optional":true,"os":["freebsd"],"engines":{"node":">= 12.0.0"},"funding":{"type":"opencollective","url":"https://opencollective.com/parcel"}} |
| 119 | L1735—1755 node_modules/lightningcss-linux-arm-gnueabihf | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.32.0","cpu":["arm"],"dev":true,"license":"MPL-2.0","optional":true,"os":["linux"],"engines":{"node":">= 12.0.0"},"funding":{"type":"opencollective","url":"https://opencollective.com/parcel"}} |
| 120 | L1756—1776 node_modules/lightningcss-linux-arm64-gnu | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.32.0","cpu":["arm64"],"dev":true,"license":"MPL-2.0","optional":true,"os":["linux"],"engines":{"node":">= 12.0.0"},"funding":{"type":"opencollective","url":"https://opencollective.com/parcel"}} |
| 121 | L1777—1797 node_modules/lightningcss-linux-arm64-musl | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.32.0","cpu":["arm64"],"dev":true,"license":"MPL-2.0","optional":true,"os":["linux"],"engines":{"node":">= 12.0.0"},"funding":{"type":"opencollective","url":"https://opencollective.com/parcel"}} |
| 122 | L1798—1818 node_modules/lightningcss-linux-x64-gnu | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.32.0","cpu":["x64"],"dev":true,"license":"MPL-2.0","optional":true,"os":["linux"],"engines":{"node":">= 12.0.0"},"funding":{"type":"opencollective","url":"https://opencollective.com/parcel"}} |
| 123 | L1819—1839 node_modules/lightningcss-linux-x64-musl | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.32.0","cpu":["x64"],"dev":true,"license":"MPL-2.0","optional":true,"os":["linux"],"engines":{"node":">= 12.0.0"},"funding":{"type":"opencollective","url":"https://opencollective.com/parcel"}} |
| 124 | L1840—1860 node_modules/lightningcss-win32-arm64-msvc | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.32.0","cpu":["arm64"],"dev":true,"license":"MPL-2.0","optional":true,"os":["win32"],"engines":{"node":">= 12.0.0"},"funding":{"type":"opencollective","url":"https://opencollective.com/parcel"}} |
| 125 | L1861—1881 node_modules/lightningcss-win32-x64-msvc | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"1.32.0","cpu":["x64"],"dev":true,"license":"MPL-2.0","optional":true,"os":["win32"],"engines":{"node":">= 12.0.0"},"funding":{"type":"opencollective","url":"https://opencollective.com/parcel"}} |
| 126 | L1882—1891 node_modules/lru-cache | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"11.5.1","dev":true,"license":"BlueOak-1.0.0","engines":{"node":"20 \|\| >=22"}} |
| 127 | L1892—1902 node_modules/lz-string | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"1.5.0","dev":true,"license":"MIT","peer":true,"bin":{"lz-string":"bin/bin.js"}} |
| 128 | L1903—1912 node_modules/magic-string | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"0.30.21","dev":true,"license":"MIT","dependencies":{"@jridgewell/sourcemap-codec":"^1.5.5"}} |
| 129 | L1913—1919 node_modules/mdn-data | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"2.27.1","dev":true,"license":"CC0-1.0"} |
| 130 | L1920—1929 node_modules/min-indent | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"1.0.1","dev":true,"license":"MIT","engines":{"node":">=4"}} |
| 131 | L1930—1948 node_modules/nanoid | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"3.3.18","dev":true,"funding":[{"type":"github","url":"https://github.com/sponsors/ai"}],"license":"MIT","bin":{"nanoid":"bin/nanoid.cjs"},"engines":{"node":"^10 \|\| ^12 \|\| ^13.7 \|\| ^14 \|\| >=15.0.1"}} |
| 132 | L1949—1962 node_modules/obug | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"2.1.3","dev":true,"funding":["https://github.com/sponsors/sxzz","https://opencollective.com/debug"],"license":"MIT","engines":{"node":">=12.20.0"}} |
| 133 | L1963—1975 node_modules/parse5 | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"8.0.1","dev":true,"license":"MIT","dependencies":{"entities":"^8.0.0"},"funding":{"url":"https://github.com/inikulin/parse5?sponsor=1"}} |
| 134 | L1976—1982 node_modules/pathe | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"2.0.3","dev":true,"license":"MIT"} |
| 135 | L1983—1989 node_modules/picocolors | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"1.1.1","dev":true,"license":"ISC"} |
| 136 | L1990—2002 node_modules/picomatch | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"4.0.4","dev":true,"license":"MIT","engines":{"node":">=12"},"funding":{"url":"https://github.com/sponsors/jonschlinkert"}} |
| 137 | L2003—2021 node_modules/playwright | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"1.61.0","dev":true,"license":"Apache-2.0","dependencies":{"playwright-core":"1.61.0"},"bin":{"playwright":"cli.js"},"engines":{"node":">=18"},"optionalDependencies":{"fsevents":"2.3.2"}} |
| 138 | L2022—2034 node_modules/playwright-core | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"1.61.0","dev":true,"license":"Apache-2.0","bin":{"playwright-core":"cli.js"},"engines":{"node":">=18"}} |
| 139 | L2035—2049 node_modules/playwright/node_modules/fsevents | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"2.3.2","dev":true,"hasInstallScript":true,"license":"MIT","optional":true,"os":["darwin"],"engines":{"node":"^8.16.0 \|\| ^10.6.0 \|\| >=11.0.0"}} |
| 140 | L2050—2078 node_modules/postcss | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"8.5.23","dev":true,"funding":[{"type":"opencollective","url":"https://opencollective.com/postcss/"},{"type":"tidelift","url":"https://tidelift.com/funding/github/npm/postcss"},{"type":"github","url":"https://github.com/sponsors/ai"}],"license":"MIT","dependencies":{"nanoid":"^3.3.16","picocolors":"^1.1.1","source-map-js":"^1.2.1"},"engines":{"node":"^10 \|\| ^12 \|\| >=14"}} |
| 141 | L2079—2094 node_modules/pretty-format | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"27.5.1","dev":true,"license":"MIT","peer":true,"dependencies":{"ansi-regex":"^5.0.1","ansi-styles":"^5.0.0","react-is":"^17.0.1"},"engines":{"node":"^10.13.0 \|\| ^12.13.0 \|\| ^14.15.0 \|\| >=15.0.0"}} |
| 142 | L2095—2102 node_modules/pretty-format/node_modules/react-is | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"17.0.2","dev":true,"license":"MIT","peer":true} |
| 143 | L2103—2112 node_modules/punycode | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"2.3.1","dev":true,"license":"MIT","engines":{"node":">=6"}} |
| 144 | L2113—2121 node_modules/qrcode.react | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"4.2.0","license":"ISC","peerDependencies":{"react":"^16.8.0 \|\| ^17.0.0 \|\| ^18.0.0 \|\| ^19.0.0"}} |
| 145 | L2122—2138 node_modules/rc-cascader | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"3.34.0","license":"MIT","dependencies":{"@babel/runtime":"^7.25.7","classnames":"^2.3.1","rc-select":"~14.16.2","rc-tree":"~5.13.0","rc-util":"^5.43.0"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 146 | L2139—2153 node_modules/rc-checkbox | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"3.5.0","license":"MIT","dependencies":{"@babel/runtime":"^7.10.1","classnames":"^2.3.2","rc-util":"^5.25.2"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 147 | L2154—2169 node_modules/rc-collapse | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"3.9.0","license":"MIT","dependencies":{"@babel/runtime":"^7.10.1","classnames":"2.x","rc-motion":"^2.3.4","rc-util":"^5.27.0"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 148 | L2170—2186 node_modules/rc-dialog | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"9.6.0","license":"MIT","dependencies":{"@babel/runtime":"^7.10.1","@rc-component/portal":"^1.0.0-8","classnames":"^2.2.6","rc-motion":"^2.3.0","rc-util":"^5.21.0"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 149 | L2187—2203 node_modules/rc-drawer | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"7.3.0","license":"MIT","dependencies":{"@babel/runtime":"^7.23.9","@rc-component/portal":"^1.1.1","classnames":"^2.2.6","rc-motion":"^2.6.1","rc-util":"^5.38.1"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 150 | L2204—2219 node_modules/rc-dropdown | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"4.2.1","license":"MIT","dependencies":{"@babel/runtime":"^7.18.3","@rc-component/trigger":"^2.0.0","classnames":"^2.2.6","rc-util":"^5.44.1"},"peerDependencies":{"react":">=16.11.0","react-dom":">=16.11.0"}} |
| 151 | L2220—2237 node_modules/rc-field-form | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"2.7.1","license":"MIT","dependencies":{"@babel/runtime":"^7.18.0","@rc-component/async-validator":"^5.0.3","rc-util":"^5.32.2"},"engines":{"node":">=8.x"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 152 | L2238—2255 node_modules/rc-image | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"7.12.0","license":"MIT","dependencies":{"@babel/runtime":"^7.11.2","@rc-component/portal":"^1.0.2","classnames":"^2.2.6","rc-dialog":"~9.6.0","rc-motion":"^2.6.2","rc-util":"^5.34.1"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 153 | L2256—2270 node_modules/rc-input | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"1.8.0","license":"MIT","dependencies":{"@babel/runtime":"^7.11.1","classnames":"^2.2.1","rc-util":"^5.18.1"},"peerDependencies":{"react":">=16.0.0","react-dom":">=16.0.0"}} |
| 154 | L2271—2287 node_modules/rc-input-number | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"9.5.0","license":"MIT","dependencies":{"@babel/runtime":"^7.10.1","@rc-component/mini-decimal":"^1.0.1","classnames":"^2.2.5","rc-input":"~1.8.0","rc-util":"^5.40.1"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 155 | L2288—2306 node_modules/rc-mentions | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"2.20.0","license":"MIT","dependencies":{"@babel/runtime":"^7.22.5","@rc-component/trigger":"^2.0.0","classnames":"^2.2.6","rc-input":"~1.8.0","rc-menu":"~9.16.0","rc-textarea":"~1.10.0","rc-util":"^5.34.1"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 156 | L2307—2324 node_modules/rc-menu | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"9.16.1","license":"MIT","dependencies":{"@babel/runtime":"^7.10.1","@rc-component/trigger":"^2.0.0","classnames":"2.x","rc-motion":"^2.4.3","rc-overflow":"^1.3.1","rc-util":"^5.27.0"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 157 | L2325—2339 node_modules/rc-motion | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"2.9.5","license":"MIT","dependencies":{"@babel/runtime":"^7.11.1","classnames":"^2.2.1","rc-util":"^5.44.0"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 158 | L2340—2358 node_modules/rc-notification | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"5.6.4","license":"MIT","dependencies":{"@babel/runtime":"^7.10.1","classnames":"2.x","rc-motion":"^2.9.0","rc-util":"^5.20.1"},"engines":{"node":">=8.x"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 159 | L2359—2374 node_modules/rc-overflow | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"1.5.0","license":"MIT","dependencies":{"@babel/runtime":"^7.11.1","classnames":"^2.2.1","rc-resize-observer":"^1.0.0","rc-util":"^5.37.0"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 160 | L2375—2389 node_modules/rc-pagination | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"5.1.0","license":"MIT","dependencies":{"@babel/runtime":"^7.10.1","classnames":"^2.3.2","rc-util":"^5.38.0"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 161 | L2390—2428 node_modules/rc-picker | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"4.11.3","license":"MIT","dependencies":{"@babel/runtime":"^7.24.7","@rc-component/trigger":"^2.0.0","classnames":"^2.2.1","rc-overflow":"^1.3.2","rc-resize-observer":"^1.4.0","rc-util":"^5.43.0"},"engines":{"node":">=8.x"},"peerDependencies":{"date-fns":">= 2.x","dayjs":">= 1.x","luxon":">= 3.x","moment":">= 2.x","react":">=16.9.0","react-dom":">=16.9.0"},"peerDependenciesMeta":{"date-fns":{"optional":true},"dayjs":{"optional":true},"luxon":{"optional":true},"moment":{"optional":true}}} |
| 162 | L2429—2443 node_modules/rc-progress | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"4.0.0","license":"MIT","dependencies":{"@babel/runtime":"^7.10.1","classnames":"^2.2.6","rc-util":"^5.16.1"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 163 | L2444—2461 node_modules/rc-rate | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"2.13.1","license":"MIT","dependencies":{"@babel/runtime":"^7.10.1","classnames":"^2.2.5","rc-util":"^5.0.1"},"engines":{"node":">=8.x"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 164 | L2462—2477 node_modules/rc-resize-observer | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"1.4.3","license":"MIT","dependencies":{"@babel/runtime":"^7.20.7","classnames":"^2.2.1","rc-util":"^5.44.1","resize-observer-polyfill":"^1.5.1"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 165 | L2478—2493 node_modules/rc-segmented | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"2.7.1","license":"MIT","dependencies":{"@babel/runtime":"^7.11.1","classnames":"^2.2.1","rc-motion":"^2.4.4","rc-util":"^5.17.0"},"peerDependencies":{"react":">=16.0.0","react-dom":">=16.0.0"}} |
| 166 | L2494—2515 node_modules/rc-select | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"14.16.8","license":"MIT","dependencies":{"@babel/runtime":"^7.10.1","@rc-component/trigger":"^2.1.1","classnames":"2.x","rc-motion":"^2.0.1","rc-overflow":"^1.3.1","rc-util":"^5.16.1","rc-virtual-list":"^3.5.2"},"engines":{"node":">=8.x"},"peerDependencies":{"react":"*","react-dom":"*"}} |
| 167 | L2516—2533 node_modules/rc-slider | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"11.1.9","license":"MIT","dependencies":{"@babel/runtime":"^7.10.1","classnames":"^2.2.5","rc-util":"^5.36.0"},"engines":{"node":">=8.x"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 168 | L2534—2551 node_modules/rc-steps | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"6.0.1","license":"MIT","dependencies":{"@babel/runtime":"^7.16.7","classnames":"^2.2.3","rc-util":"^5.16.1"},"engines":{"node":">=8.x"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 169 | L2552—2566 node_modules/rc-switch | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"4.1.0","license":"MIT","dependencies":{"@babel/runtime":"^7.21.0","classnames":"^2.2.1","rc-util":"^5.30.0"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 170 | L2567—2587 node_modules/rc-table | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"7.54.0","license":"MIT","dependencies":{"@babel/runtime":"^7.10.1","@rc-component/context":"^1.4.0","classnames":"^2.2.5","rc-resize-observer":"^1.1.0","rc-util":"^5.44.3","rc-virtual-list":"^3.14.2"},"engines":{"node":">=8.x"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 171 | L2588—2609 node_modules/rc-tabs | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"15.7.0","license":"MIT","dependencies":{"@babel/runtime":"^7.11.2","classnames":"2.x","rc-dropdown":"~4.2.0","rc-menu":"~9.16.0","rc-motion":"^2.6.2","rc-resize-observer":"^1.0.0","rc-util":"^5.34.1"},"engines":{"node":">=8.x"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 172 | L2610—2626 node_modules/rc-textarea | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"1.10.2","license":"MIT","dependencies":{"@babel/runtime":"^7.10.1","classnames":"^2.2.1","rc-input":"~1.8.0","rc-resize-observer":"^1.0.0","rc-util":"^5.27.0"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 173 | L2627—2642 node_modules/rc-tooltip | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"6.4.0","license":"MIT","dependencies":{"@babel/runtime":"^7.11.2","@rc-component/trigger":"^2.0.0","classnames":"^2.3.1","rc-util":"^5.44.3"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 174 | L2643—2662 node_modules/rc-tree | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"5.13.1","license":"MIT","dependencies":{"@babel/runtime":"^7.10.1","classnames":"2.x","rc-motion":"^2.0.1","rc-util":"^5.16.1","rc-virtual-list":"^3.5.1"},"engines":{"node":">=10.x"},"peerDependencies":{"react":"*","react-dom":"*"}} |
| 175 | L2663—2679 node_modules/rc-tree-select | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"5.27.0","license":"MIT","dependencies":{"@babel/runtime":"^7.25.7","classnames":"2.x","rc-select":"~14.16.2","rc-tree":"~5.13.0","rc-util":"^5.43.0"},"peerDependencies":{"react":"*","react-dom":"*"}} |
| 176 | L2680—2694 node_modules/rc-upload | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"4.11.0","license":"MIT","dependencies":{"@babel/runtime":"^7.18.3","classnames":"^2.2.5","rc-util":"^5.2.0"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 177 | L2695—2708 node_modules/rc-util | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"5.44.4","license":"MIT","dependencies":{"@babel/runtime":"^7.18.3","react-is":"^18.2.0"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 178 | L2709—2727 node_modules/rc-virtual-list | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"3.19.2","license":"MIT","dependencies":{"@babel/runtime":"^7.20.0","classnames":"^2.2.6","rc-resize-observer":"^1.0.0","rc-util":"^5.36.0"},"engines":{"node":">=8.x"},"peerDependencies":{"react":">=16.9.0","react-dom":">=16.9.0"}} |
| 179 | L2728—2736 node_modules/react | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"19.2.7","license":"MIT","engines":{"node":">=0.10.0"}} |
| 180 | L2737—2748 node_modules/react-dom | 运行库固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"19.2.7","license":"MIT","dependencies":{"scheduler":"^0.27.0"},"peerDependencies":{"react":"^19.2.7"}} |
| 181 | L2749—2754 node_modules/react-is | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"18.3.1","license":"MIT"} |
| 182 | L2755—2768 node_modules/redent | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"3.0.0","dev":true,"license":"MIT","dependencies":{"indent-string":"^4.0.0","strip-indent":"^3.0.0"},"engines":{"node":">=8"}} |
| 183 | L2769—2778 node_modules/require-from-string | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"2.0.2","dev":true,"license":"MIT","engines":{"node":">=0.10.0"}} |
| 184 | L2779—2784 node_modules/resize-observer-polyfill | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"1.5.1","license":"MIT"} |
| 185 | L2785—2818 node_modules/rolldown | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"1.0.3","dev":true,"license":"MIT","dependencies":{"@oxc-project/types":"=0.133.0","@rolldown/pluginutils":"^1.0.0"},"bin":{"rolldown":"bin/cli.mjs"},"engines":{"node":"^20.19.0 \|\| >=22.12.0"},"optionalDependencies":{"@rolldown/binding-android-arm64":"1.0.3","@rolldown/binding-darwin-arm64":"1.0.3","@rolldown/binding-darwin-x64":"1.0.3","@rolldown/binding-freebsd-x64":"1.0.3","@rolldown/binding-linux-arm-gnueabihf":"1.0.3","@rolldown/binding-linux-arm64-gnu":"1.0.3","@rolldown/binding-linux-arm64-musl":"1.0.3","@rolldown/binding-linux-ppc64-gnu":"1.0.3","@rolldown/binding-linux-s390x-gnu":"1.0.3","@rolldown/binding-linux-x64-gnu":"1.0.3","@rolldown/binding-linux-x64-musl":"1.0.3","@rolldown/binding-openharmony-arm64":"1.0.3","@rolldown/binding-wasm32-wasi":"1.0.3","@rolldown/binding-win32-arm64-msvc":"1.0.3","@rolldown/binding-win32-x64-msvc":"1.0.3"}} |
| 186 | L2819—2825 node_modules/rolldown/node_modules/@rolldown/pluginutils | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"1.0.1","dev":true,"license":"MIT"} |
| 187 | L2826—2838 node_modules/saxes | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"6.0.0","dev":true,"license":"ISC","dependencies":{"xmlchars":"^2.2.0"},"engines":{"node":">=v12.22.7"}} |
| 188 | L2839—2844 node_modules/scheduler | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"0.27.0","license":"MIT"} |
| 189 | L2845—2853 node_modules/scroll-into-view-if-needed | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"3.1.0","license":"MIT","dependencies":{"compute-scroll-into-view":"^3.0.2"}} |
| 190 | L2854—2860 node_modules/siginfo | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"2.0.0","dev":true,"license":"ISC"} |
| 191 | L2861—2870 node_modules/source-map-js | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"1.2.1","dev":true,"license":"BSD-3-Clause","engines":{"node":">=0.10.0"}} |
| 192 | L2871—2877 node_modules/stackback | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"0.0.2","dev":true,"license":"MIT"} |
| 193 | L2878—2884 node_modules/std-env | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"4.1.0","dev":true,"license":"MIT"} |
| 194 | L2885—2890 node_modules/string-convert | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"0.2.1","license":"MIT"} |
| 195 | L2891—2903 node_modules/strip-indent | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"3.0.0","dev":true,"license":"MIT","dependencies":{"min-indent":"^1.0.0"},"engines":{"node":">=8"}} |
| 196 | L2904—2909 node_modules/stylis | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"4.4.0","license":"MIT"} |
| 197 | L2910—2916 node_modules/symbol-tree | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"3.2.4","dev":true,"license":"MIT"} |
| 198 | L2917—2925 node_modules/throttle-debounce | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"5.0.2","license":"MIT","engines":{"node":">=12.22"}} |
| 199 | L2926—2932 node_modules/tinybench | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"2.9.0","dev":true,"license":"MIT"} |
| 200 | L2933—2942 node_modules/tinyexec | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"1.2.4","dev":true,"license":"MIT","engines":{"node":">=18"}} |
| 201 | L2943—2959 node_modules/tinyglobby | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"0.2.17","dev":true,"license":"MIT","dependencies":{"fdir":"^6.5.0","picomatch":"^4.0.4"},"engines":{"node":">=12.0.0"},"funding":{"url":"https://github.com/sponsors/SuperchupuDev"}} |
| 202 | L2960—2969 node_modules/tinyrainbow | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"3.1.0","dev":true,"license":"MIT","engines":{"node":">=14.0.0"}} |
| 203 | L2970—2982 node_modules/tldts | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"7.4.3","dev":true,"license":"MIT","dependencies":{"tldts-core":"^7.4.3"},"bin":{"tldts":"bin/cli.js"}} |
| 204 | L2983—2989 node_modules/tldts-core | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"7.4.3","dev":true,"license":"MIT"} |
| 205 | L2990—2995 node_modules/toggle-selection | 运行库固定版本和上游引用；全图可达、无业务规则 | {"version":"1.0.6","license":"MIT"} |
| 206 | L2996—3008 node_modules/tough-cookie | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"6.0.1","dev":true,"license":"BSD-3-Clause","dependencies":{"tldts":"^7.0.5"},"engines":{"node":">=16"}} |
| 207 | L3009—3021 node_modules/tr46 | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"6.0.0","dev":true,"license":"MIT","dependencies":{"punycode":"^2.3.1"},"engines":{"node":">=20"}} |
| 208 | L3022—3029 node_modules/tslib | 开发工具固定版本和上游引用；可选平台/运行支持；全图可达、无业务规则 | {"version":"2.8.1","dev":true,"license":"0BSD","optional":true} |
| 209 | L3030—3043 node_modules/typescript | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"5.9.3","dev":true,"license":"Apache-2.0","bin":{"tsc":"bin/tsc","tsserver":"bin/tsserver"},"engines":{"node":">=14.17"}} |
| 210 | L3044—3053 node_modules/undici | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"7.29.0","dev":true,"license":"MIT","engines":{"node":">=20.18.1"}} |
| 211 | L3054—3131 node_modules/vite | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"8.0.16","dev":true,"license":"MIT","dependencies":{"lightningcss":"^1.32.0","picomatch":"^4.0.4","postcss":"^8.5.15","rolldown":"1.0.3","tinyglobby":"^0.2.17"},"bin":{"vite":"bin/vite.js"},"engines":{"node":"^20.19.0 \|\| >=22.12.0"},"funding":{"url":"https://github.com/vitejs/vite?sponsor=1"},"optionalDependencies":{"fsevents":"~2.3.3"},"peerDependencies":{"@types/node":"^20.19.0 \|\| >=22.12.0","@vitejs/devtools":"^0.1.18","esbuild":"^0.27.0 \|\| ^0.28.0","jiti":">=1.21.0","less":"^4.0.0","sass":"^1.70.0","sass-embedded":"^1.70.0","stylus":">=0.54.8","sugarss":"^5.0.0","terser":"^5.16.0","tsx":"^4.8.1","yaml":"^2.4.2"},"peerDependenciesMeta":{"@types/node":{"optional":true},"@vitejs/devtools":{"optional":true},"esbuild":{"optional":true},"jiti":{"optional":true},"less":{"optional":true},"sass":{"optional":true},"sass-embedded":{"optional":true},"stylus":{"optional":true},"sugarss":{"optional":true},"terser":{"optional":true},"tsx":{"optional":true},"yaml":{"optional":true}}} |
| 212 | L3132—3221 node_modules/vitest | 开发工具固定版本和上游引用；peer解析要求；全图可达、无业务规则 | {"version":"4.1.9","dev":true,"license":"MIT","dependencies":{"@vitest/expect":"4.1.9","@vitest/mocker":"4.1.9","@vitest/pretty-format":"4.1.9","@vitest/runner":"4.1.9","@vitest/snapshot":"4.1.9","@vitest/spy":"4.1.9","@vitest/utils":"4.1.9","es-module-lexer":"^2.0.0","expect-type":"^1.3.0","magic-string":"^0.30.21","obug":"^2.1.1","pathe":"^2.0.3","picomatch":"^4.0.3","std-env":"^4.0.0-rc.1","tinybench":"^2.9.0","tinyexec":"^1.0.2","tinyglobby":"^0.2.15","tinyrainbow":"^3.1.0","vite":"^6.0.0 \|\| ^7.0.0 \|\| ^8.0.0","why-is-node-running":"^2.3.0"},"bin":{"vitest":"vitest.mjs"},"engines":{"node":"^20.0.0 \|\| ^22.0.0 \|\| >=24.0.0"},"funding":{"url":"https://opencollective.com/vitest"},"peerDependencies":{"@edge-runtime/vm":"*","@opentelemetry/api":"^1.9.0","@types/node":"^20.0.0 \|\| ^22.0.0 \|\| >=24.0.0","@vitest/browser-playwright":"4.1.9","@vitest/browser-preview":"4.1.9","@vitest/browser-webdriverio":"4.1.9","@vitest/coverage-istanbul":"4.1.9","@vitest/coverage-v8":"4.1.9","@vitest/ui":"4.1.9","happy-dom":"*","jsdom":"*","vite":"^6.0.0 \|\| ^7.0.0 \|\| ^8.0.0"},"peerDependenciesMeta":{"@edge-runtime/vm":{"optional":true},"@opentelemetry/api":{"optional":true},"@types/node":{"optional":true},"@vitest/browser-playwright":{"optional":true},"@vitest/browser-preview":{"optional":true},"@vitest/browser-webdriverio":{"optional":true},"@vitest/coverage-istanbul":{"optional":true},"@vitest/coverage-v8":{"optional":true},"@vitest/ui":{"optional":true},"happy-dom":{"optional":true},"jsdom":{"optional":true},"vite":{"optional":false}}} |
| 213 | L3222—3234 node_modules/w3c-xmlserializer | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"5.0.0","dev":true,"license":"MIT","dependencies":{"xml-name-validator":"^5.0.0"},"engines":{"node":">=18"}} |
| 214 | L3235—3244 node_modules/webidl-conversions | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"8.0.1","dev":true,"license":"BSD-2-Clause","engines":{"node":">=20"}} |
| 215 | L3245—3254 node_modules/whatwg-mimetype | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"5.0.0","dev":true,"license":"MIT","engines":{"node":">=20"}} |
| 216 | L3255—3269 node_modules/whatwg-url | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"16.0.1","dev":true,"license":"MIT","dependencies":{"@exodus/bytes":"^1.11.0","tr46":"^6.0.0","webidl-conversions":"^8.0.1"},"engines":{"node":"^20.19.0 \|\| ^22.12.0 \|\| >=24.0.0"}} |
| 217 | L3270—3286 node_modules/why-is-node-running | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"2.3.0","dev":true,"license":"MIT","dependencies":{"siginfo":"^2.0.0","stackback":"0.0.2"},"bin":{"why-is-node-running":"cli.js"},"engines":{"node":">=8"}} |
| 218 | L3287—3296 node_modules/xml-name-validator | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"5.0.0","dev":true,"license":"Apache-2.0","engines":{"node":">=18"}} |
| 219 | L3297—3303 node_modules/xmlchars | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"2.2.0","dev":true,"license":"MIT"} |
| 220 | L3304—3319 node_modules/yaml | 开发工具固定版本和上游引用；全图可达、无业务规则 | {"version":"2.9.0","dev":true,"license":"ISC","bin":{"yaml":"bin.mjs"},"engines":{"node":">= 14.6"},"funding":{"url":"https://github.com/sponsors/eemeli"}} |

## 三线全模块复查与六ID现状

第一性原理：根/web两清单为何镜像？npm ci据此拒绝与锁不一致的依赖声明。link为何存在？根workspaces apps/web需要node_modules入口。版本/URL/SRI为何不能删？缺失会失去确定安装来源/版本与校验。dev/optional/peer/os/cpu/engines为何不统一删除？分别控制安装集合、平台、同宿主peer及运行下限，真实图全220可达；不同物理位置和版本不是仅凭名称可删的重复逻辑。许可证/资助/bin是软件包元数据，不是业务许可、支付或终端发明。documentVersion为何存在？直接用户铁律要求被改文件自身Vx.xx，npm应用semver不能替代；真实npm10消费同一源码保留并不改变安装。

顺从式：对全220记录和所有字段类别逐一核对上表18GEN，依赖图582边、全体官方来源、根web镜像、workspace link都重新实跑；不是仅重验3个变更包。六ID：001锁nanoid3.3.18、002锁postcss8.5.23/nanoid^3.3.16、003锁undici7.29.0；004原格式/镜像/link、005全图/平台/peer/engines/嵌套、006来源/SRI均当前无缺陷。六项当前队列否与本次验证一致，没有凭历史队列否跳审。

对抗式：尝试断链react会检出50错误；查找孤立节点得到0；查全217 URL版本/域名/包名/sha512格式与216官方dist无不一致；查所有父范围/required peer无无效边；可选peer缺失不冒作required错误；查root/workspace/link没有相互矛盾；查完整安全库当前0；查平台包os/cpu条件防止跨平台删包；查新增版本标记没有替换应用version或协议值。检查fsevents2.3.3与playwright嵌套2.3.2、react-is18/17、dom-accessibility-api0.5/0.6及pluginutils分处workspace/rolldown，保持原生解析有效，不因为同名就误归业务添加。沒有发现新问题。

## 本次实际命令与完整摘要

````text
GRAPH_EXIT 0 {"originalInventory":220,"edges":582,"invalidEdges":[],"optionalMissing":0,"reachable":220,"orphan":[],"enginesBad":[],"documentVersion":"V0.01","negativeMissingReact":50,"mode":"in-memory only"}
 
CMD ["npm", "ls", "--all", "--package-lock-only", "--json"] EXIT 0
LS_PROBLEMS []
STDERR 
CMD ["npm", "ci", "--dry-run", "--ignore-scripts", "--no-audit", "--no-fund", "--loglevel=error"] EXIT 0
DRY_RUN_SUMMARY added 188 packages, and removed 186 packages in 224ms
STDERR 
CMD ["npm", "audit", "--package-lock-only", "--ignore-scripts", "--json"] EXIT 0
{
  "auditReportVersion": 2,
  "vulnerabilities": {},
  "metadata": {
    "vulnerabilities": {
      "info": 0,
      "low": 0,
      "moderate": 0,
      "high": 0,
      "critical": 0,
      "total": 0
    },
    "dependencies": {
      "prod": 74,
      "dev": 146,
      "optional": 34,
      "peer": 11,
      "peerOptional": 0,
      "total": 219
    }
  }
}

STDERR 
CMD ["npm", "audit", "--omit=dev", "--package-lock-only", "--ignore-scripts", "--json"] EXIT 0
{
  "auditReportVersion": 2,
  "vulnerabilities": {},
  "metadata": {
    "vulnerabilities": {
      "info": 0,
      "low": 0,
      "moderate": 0,
      "high": 0,
      "critical": 0,
      "total": 0
    },
    "dependencies": {
      "prod": 74,
      "dev": 146,
      "optional": 34,
      "peer": 11,
      "peerOptional": 0,
      "total": 219
    }
  }
}

STDERR 
SOURCE_UNCHANGED 65254915c5c85e3bf5b26659d0acd27b2bdfc94c6b5954d15e4db81a6877e6bc

OFFICIAL_REGISTRY_NAMES_VERSIONS 216 RECORDS 217 DIFFERENCES []
SOURCE_SHA 65254915c5c85e3bf5b26659d0acd27b2bdfc94c6b5954d15e4db81a6877e6bc

ops/ci/predeploy-gates.yml:5:# 硬阻断由 .160 部署脚本的 verify:predeploy 与本地 pre-push 钩子承担。
ops/ci/predeploy-gates.yml:18:          node-version: "22"
ops/ci/predeploy-gates.yml:20:        run: npm ci --ignore-scripts
ops/ci/predeploy-gates.yml:21:      - name: verify:predeploy (governance gates)
ops/ci/predeploy-gates.yml:22:        run: npm run verify:predeploy
.github/workflows/predeploy-gates.yml:5:# 硬阻断由 .160 部署脚本的 verify:predeploy 与本地 pre-push 钩子承担。
.github/workflows/predeploy-gates.yml:20:          node-version: "22"
.github/workflows/predeploy-gates.yml:22:        run: npm ci --ignore-scripts
.github/workflows/predeploy-gates.yml:23:      - name: verify:predeploy (governance gates)
.github/workflows/predeploy-gates.yml:24:        run: npm run verify:predeploy
package.json:96:    "verify:predeploy": "npm run verify:html-ledger-sync && npm run verify:entry-anchors && npm run verify:html-param-tier && npm run verify:delivery-gates-docs && npm run verify:full-admin-matrix && npm run verify:domain-ownership-scope && npm run verify:matrix-vs-migration && npm run verify:data-isolation-spec && npm run verify:tenant-isolation && npm run verify:p0-contract && npm run verify:security-baseline && npm run verify:production-readiness-evidence && npm run verify:production-blockers && npm run verify:pii-encryption && npm run verify:test-db-isolation",
package.json:139:  "packageManager": "npm@10.9.8",
package.json:140:  "engines": {

````

### 本次图校验可复现代码（node -e，同仓cwd；不save/reify）
````javascript
const fs=require('node:fs'),assert=require('node:assert/strict'),Arborist=require('/usr/lib/node_modules/npm/node_modules/@npmcli/arborist'),semver=require('/usr/lib/node_modules/npm/node_modules/semver');
(async()=>{
const lock=JSON.parse(fs.readFileSync('package-lock.json','utf8'));
for(const [path,file] of [['','package.json'],['apps/web','apps/web/package.json']]){
 const p=JSON.parse(fs.readFileSync(file,'utf8')),n=lock.packages[path];
 for(const k of ['name','version','dependencies','devDependencies','optionalDependencies','workspaces','engines'])assert.deepEqual(n[k],p[k],path+':'+k);
}
const t=await new Arborist({path:process.cwd()}).loadVirtual();
const bad=[];let edges=0,optionalMissing=0;
for(const n of t.inventory.values())for(const e of n.edgesOut.values()){edges++;if(!e.valid){if(e.optional&&!e.to)optionalMissing++;else bad.push({from:n.location,name:e.name,spec:e.spec,error:e.error});}}
assert.equal(bad.length,0,JSON.stringify(bad));
const seen=new Set(),q=[t];while(q.length){const n=q.pop();if(!n||seen.has(n))continue;seen.add(n);if(n.isLink)q.push(n.target);for(const e of n.edgesOut.values())if(e.to)q.push(e.to);}
const orphan=[...t.inventory.values()].filter(n=>!seen.has(n)).map(n=>n.location);assert.equal(orphan.length,0,JSON.stringify(orphan));
const enginesBad=Object.entries(lock.packages).filter(([p,n])=>n.engines?.node&&!semver.satisfies(process.version,n.engines.node)).map(([p,n])=>[p,n.engines.node]);assert.equal(enginesBad.length,0);
assert.equal(t.meta.data.documentVersion,'V0.01');
const originalInventory=t.inventory.size;const react=t.inventory.get('node_modules/react');react.parent=null;
const negativeMissingReact=[...t.inventory.values()].flatMap(n=>[...n.edgesOut.values()].filter(e=>e.name==='react'&&!e.valid&&!e.optional)).length;assert(negativeMissingReact>0);
console.log(JSON.stringify({originalInventory,edges,invalidEdges:bad,optionalMissing,reachable:seen.size,orphan,enginesBad,documentVersion:'V0.01',negativeMissingReact,mode:'in-memory only'}));
})().catch(e=>{console.error(e.name,e.message);process.exit(1)});
````

### 相同源SHA已执行的真实隔离安装（明确是B4阶段，不冒称本次重跑）

本方B4在.scratch自动删除的临时目录复制根package.json/lock和apps/web/package.json，官方npm10.9.8 tarball经SRI核验，node npm-cli.js ci --ignore-scripts --no-audit --no-fund；真实安装188包并加载三修补库作最小断言。当前源仍精确相同SHA，故该实际执行证据仍适用。没有为了B5再制造无关测试或主环境安装。

````text
REAL_ISOLATED_NPM10_CI_EXIT 0 STDOUT 
added 188 packages in 2s
 STDERR 
ISOLATED_INSTALLED_ASSERT_EXIT 0 nanoid 3.3.18
postcss 8.5.23
undici 7.29.0
INSTALLED_LIBRARIES_ASSERT_PASS
 
TEMP_REMOVED True MAIN_SOURCE_UNCHANGED True 65254915c5c85e3bf5b26659d0acd27b2bdfc94c6b5954d15e4db81a6877e6bc

````

源/命令信息范围：当前源码UTF-8严格解码、无重复JSON键、217条HTTPS URL/SRI格式与官方元数据全匹配；引用源路径正本160。当前阶段没有编辑源码、三HTML、findings/ledger，只有本raw首次落盘。临时图负例只在内存，当前原生工具检查全程源码SHA保持。根环境>=22与若干上游包22.12/22.13下限差异是既有观察，不改写上游字段或把非当前宿主当已失败。未跑完整前端单测/E2E、后端测试、数据库迁移或部署；那些不是本锁文件当前声称实现的业务行为。npm审计数据库不是全知，结果限定当前查询。

技能按既有已读内容应用：grilling自问源/必要性，codebase-design将锁作为安装图接口，diagnosing-bugs用实际库反例及图断链，verification-before-completion依据上述命令，ponytail只保留需要的安装字段，writing-for-agents组织可复核逐条表；未派第三席或追加新判据。GEN读取第一次只匹配标题、随后精确读取完整18段；规范化读取时附带计划映射长输出被截断，但源076—150全部已在截断前完整读出，GEN正文随后单独完整读取，不以被截断的计划映射声称新模块已审。

## 最终本席结论

PASS。本次完整3321行/220条/18GEN审完，无新CXfinding、无工程残余、无业务添加/HTML申请，六项队列保持否。Grok本次报告尚未读取，本模块仍IN_PROGRESS，B3=1。只有本次两份完整B5均PASS且B5后四值实算全等，才可以先写ledger PASS、再写收口文件和执行已授权发布；本报告不冒称这些后续动作已完成。

## V0.02 外席接收与事实校正

本段在本方独立V0.01完整87159字节/SHA aec13af9f4be9998af84839dc7411e1dd2d058fd2d98da909d886863c2f3d43e落盘后追加，原前缀逐字保留；只是接收说明，不是新的盲审票或源修复。

Grok本次真实EXIT_CODE=0，原raw 95358字节/429行/SHA 31f7bd9586520ad2ab89fd4b3bbc5a95fa596d081b68a2a343ce60af5ced264f已经1—429行全部读完。全3321行/220记录/18GEN三线审完、无GR、新问题0、结论PASS。其B5未读任何本次self或B4 raw；先前findings中的已完成B4接收摘要不是本次self分析。

接收时排除两处不成立的报告表述：Grok L88所称npm ci原子替换保证失败不留半棵树，不是可采信的npm事务保证；本锁GEN006依据为重复键、清单镜像和引用完整性，不据此推定安装回滚。L164把apps/web称为客人端没有源依据；它只是现有web工作区，不能推导客人有Web端。两处均不改变当前锁任何字段或判据结果，本方原独立报告不依赖这些说法；保留Grok原文并标明不采信上述推论，不改写其结论。

Grok的426普通/optional依赖与115已满足peer计数、29未安装optional peer是字段分项统计；本方582为Arborist最终解析边，计数口径不同。两席完整npm ls通过，没有缺失必需依赖。Grok仅对三个修补tarball重算SRI、其余216去重元数据全比对；未把dry-run当真实安装。本方真实npm10.9.8隔离安装证据来自相同源SHA的B4，B5明确沿用，不伪称本轮又安装。

B5后actual、sidecar、ledger、Grok报告四值实测均为23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de；LOCK-AMEND-COUNT=0，源SHA保持65254915c5c85e3bf5b26659d0acd27b2bdfc94c6b5954d15e4db81a6877e6bc。六ID当前队列全否，无M004未答问题。原独立本方PASS和本次独立Grok PASS成立，无待补项；可以按B5既定顺序收口。
