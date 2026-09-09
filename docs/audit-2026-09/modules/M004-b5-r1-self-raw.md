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

CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M004 当前471计划 B5-r1 Codex独立全量审计
版本：V0.02

本席B5结论：PASS。M004生命周期仍IN_PROGRESS；历史B3累计1、方案修复2、失败重跑0。本次B2队列为空，未执行新的B3/B4。不能以本席票或Grok交付exit0代替双PASS。
实际顺序：先Grok完整B5真实结束并经适配器返回路径/exit0，根才重读整个锁文件并独立执行下述全部验证。本次报告保存前未读Grok当前B5正文或其原始CLI stdout/stderr。旧self前89089字节原样保留，不继承旧322同号PASS，也不以本次B1/B2证据替代本次新检查。

## 1. 本次完整输入、范围和消费链
正本/home/james/complex-management，main，HEAD=origin/main=242f487233d028173cb9198eecd079f5d877405c。唯一受审文件package-lock.json：3321行、114258字节、SHA256 d3676e896b885b6bbb979440bb51b9dfe60728735cee000e528ab5d1791bcfef、documentVersion V0.02。模块映射为criteria-lock.md:6383的GEN001—018全部18项；锁V0.11，COUNT1。
本次按结构重新读取全部220条及21种包字段，含所有依赖/peer/optional/engine/os/cpu/bin/license/funding/link/install标志；217个integrity使用完整原值严格Base64+64字节校验，显示时仅省略摘要字面量，不声称人工逐字符核摘要。顶层1—7行+全部条目8—3319行+闭合3320—3321行连续覆盖。
原始展示第一段过长被工具截去855—1102附近，随后单独重新输出并阅读这一完整区间；未把丢失输出当作已经读完。不是源缺陷或Grok交付失败。
直接对照已读root/web package.json、vite/vitest.config.ts全文、实际npm11.19.0的ci.js/validate-lockfile.js全文。消费链为root/workspace声明→package-lock虚拟图→npm buildIdealTree→原生validateLockfile→reify；ci设置save:false，只有!dryRun分支才清node_modules，ignoreScripts阻止生命周期脚本。本次仅离线预演，无安装删除。
当前Node v24.20.0、npm11.19.0；根声明packageManager npm10.9.8。本次没有冒称已用10.9.8实际执行。根node范围^22.13.0或>=24.0.0覆盖全部116条依赖node范围，两个manifest字段与锁深比较一致，共17条直接依赖声明。
三HTML只是业务来源，不将软件锁新增为第四业务来源；无具体HTML上位业务条目落到包元数据、无业务发明，因此依序排除冲突/细化后逐条登记技术性。以下每条目的用途为当前锁结构和原生可达图所支持，不从包名发明业务规则。

## 2. 逐条技术性分类与连续覆盖
所有行的冲突/添加/当前工程缺陷均为否；“技术性”不替代下面工程检查。依赖包的自身功能不在本模块全量源审范围；只审当前锁记录及直接消费边界。
| 行号 | 条目位置 | 锁版本或目标 | 分类 | 必要用途与无业务发明依据 |
|---|---|---|---|---|
| 1—7 | 顶层协议字段 | lockfileVersion3 / documentVersion V0.02 / app0.1.0 | 技术性 | 安装清单格式、应用标识与文档版本分离，无业务状态或规则 |
| 8—21 | <root> | 0.1.0 | 技术性 | 根清单及工作区镜像；由实际根图可达，无业务规则发明 |
| 22—44 | apps/web | 0.1.0 | 技术性 | Web清单镜像；由实际根图可达，无业务规则发明 |
| 45—51 | apps/web/node_modules/@rolldown/pluginutils | 1.0.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 52—77 | apps/web/node_modules/@vitejs/plugin-react | 6.0.2 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 78—84 | node_modules/@adobe/css-tools | 4.5.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 85—93 | node_modules/@ant-design/colors | 7.2.1 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 94—112 | node_modules/@ant-design/cssinjs | 1.24.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 113—127 | node_modules/@ant-design/cssinjs-utils | 1.1.3 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 128—139 | node_modules/@ant-design/fast-color | 2.0.6 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 140—159 | node_modules/@ant-design/icons | 5.6.1 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 160—165 | node_modules/@ant-design/icons-svg | 4.4.2 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 166—181 | node_modules/@ant-design/react-slick | 1.1.2 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 182—198 | node_modules/@asamuzakjp/css-color | 5.1.11 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 199—215 | node_modules/@asamuzakjp/dom-selector | 7.1.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 216—225 | node_modules/@asamuzakjp/generational-cache | 1.0.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 226—232 | node_modules/@asamuzakjp/nwsapi | 2.3.9 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 233—248 | node_modules/@babel/code-frame | 7.29.7 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 249—259 | node_modules/@babel/helper-validator-identifier | 7.29.7 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 260—268 | node_modules/@babel/runtime | 7.29.7 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 269—281 | node_modules/@bramus/specificity | 2.4.2 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 282—301 | node_modules/@csstools/color-helpers | 6.0.2 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 302—325 | node_modules/@csstools/css-calc | 3.2.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 326—353 | node_modules/@csstools/css-color-parser | 4.1.7 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 354—376 | node_modules/@csstools/css-parser-algorithms | 4.0.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 377—401 | node_modules/@csstools/css-syntax-patches-for-csstree | 1.1.5 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 402—421 | node_modules/@csstools/css-tokenizer | 4.0.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 422—433 | node_modules/@emnapi/core | 1.10.0 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 434—444 | node_modules/@emnapi/runtime | 1.10.0 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 445—455 | node_modules/@emnapi/wasi-threads | 1.2.1 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 456—461 | node_modules/@emotion/hash | 0.8.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 462—467 | node_modules/@emotion/unitless | 0.7.5 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 468—485 | node_modules/@exodus/bytes | 1.15.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 486—492 | node_modules/@jridgewell/sourcemap-codec | 1.5.5 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 493—511 | node_modules/@napi-rs/wasm-runtime | 1.1.5 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 512—521 | node_modules/@oxc-project/types | 0.133.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 522—537 | node_modules/@playwright/test | 1.61.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 538—549 | node_modules/@rc-component/async-validator | 5.1.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 550—565 | node_modules/@rc-component/color-picker | 2.0.1 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 566—579 | node_modules/@rc-component/context | 1.4.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 580—591 | node_modules/@rc-component/mini-decimal | 1.1.3 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 592—609 | node_modules/@rc-component/mutate-observer | 1.1.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 610—627 | node_modules/@rc-component/portal | 1.1.2 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 628—643 | node_modules/@rc-component/qrcode | 1.1.1 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 644—663 | node_modules/@rc-component/tour | 1.15.1 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 664—684 | node_modules/@rc-component/trigger | 2.3.1 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 685—701 | node_modules/@rolldown/binding-android-arm64 | 1.0.3 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 702—718 | node_modules/@rolldown/binding-darwin-arm64 | 1.0.3 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 719—735 | node_modules/@rolldown/binding-darwin-x64 | 1.0.3 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 736—752 | node_modules/@rolldown/binding-freebsd-x64 | 1.0.3 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 753—769 | node_modules/@rolldown/binding-linux-arm-gnueabihf | 1.0.3 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 770—786 | node_modules/@rolldown/binding-linux-arm64-gnu | 1.0.3 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 787—803 | node_modules/@rolldown/binding-linux-arm64-musl | 1.0.3 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 804—820 | node_modules/@rolldown/binding-linux-ppc64-gnu | 1.0.3 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 821—837 | node_modules/@rolldown/binding-linux-s390x-gnu | 1.0.3 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 838—854 | node_modules/@rolldown/binding-linux-x64-gnu | 1.0.3 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 855—871 | node_modules/@rolldown/binding-linux-x64-musl | 1.0.3 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 872—888 | node_modules/@rolldown/binding-openharmony-arm64 | 1.0.3 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 889—907 | node_modules/@rolldown/binding-wasm32-wasi | 1.0.3 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 908—924 | node_modules/@rolldown/binding-win32-arm64-msvc | 1.0.3 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 925—941 | node_modules/@rolldown/binding-win32-x64-msvc | 1.0.3 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 942—948 | node_modules/@standard-schema/spec | 1.1.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 949—969 | node_modules/@testing-library/dom | 10.4.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 970—989 | node_modules/@testing-library/jest-dom | 6.9.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 990—996 | node_modules/@testing-library/jest-dom/node_modules/dom-accessibility-api | 0.6.3 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 997—1024 | node_modules/@testing-library/react | 16.3.2 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1025—1038 | node_modules/@testing-library/user-event | 14.6.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1039—1049 | node_modules/@tybys/wasm-util | 0.10.2 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 1050—1057 | node_modules/@types/aria-query | 5.0.4 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1058—1068 | node_modules/@types/chai | 5.2.3 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1069—1075 | node_modules/@types/deep-eql | 4.0.2 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1076—1082 | node_modules/@types/estree | 1.0.9 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1083—1092 | node_modules/@types/react | 19.2.16 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1093—1102 | node_modules/@types/react-dom | 19.2.3 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1103—1120 | node_modules/@vitest/expect | 4.1.9 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1121—1147 | node_modules/@vitest/mocker | 4.1.9 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1148—1160 | node_modules/@vitest/pretty-format | 4.1.9 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1161—1174 | node_modules/@vitest/runner | 4.1.9 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1175—1190 | node_modules/@vitest/snapshot | 4.1.9 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1191—1200 | node_modules/@vitest/spy | 4.1.9 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1201—1215 | node_modules/@vitest/utils | 4.1.9 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1216—1226 | node_modules/ansi-regex | 5.0.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1227—1240 | node_modules/ansi-styles | 5.2.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1241—1305 | node_modules/antd | 5.29.3 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 1306—1309 | node_modules/apps-web | apps/web | 技术性 | 唯一工作区链接；由实际根图可达，无业务规则发明 |
| 1310—1319 | node_modules/aria-query | 5.3.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1320—1329 | node_modules/assertion-error | 2.0.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1330—1339 | node_modules/bidi-js | 1.0.3 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1340—1349 | node_modules/chai | 6.2.2 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1350—1355 | node_modules/classnames | 2.5.1 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 1356—1361 | node_modules/compute-scroll-into-view | 3.1.1 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 1362—1368 | node_modules/convert-source-map | 2.0.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1369—1377 | node_modules/copy-to-clipboard | 3.3.3 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 1378—1391 | node_modules/css-tree | 3.2.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1392—1398 | node_modules/css.escape | 1.5.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1399—1404 | node_modules/csstype | 3.2.3 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 1405—1418 | node_modules/data-urls | 7.0.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1419—1424 | node_modules/dayjs | 1.11.21 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 1425—1431 | node_modules/decimal.js | 10.6.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1432—1441 | node_modules/dequal | 2.0.3 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1442—1451 | node_modules/detect-libc | 2.1.2 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1452—1459 | node_modules/dom-accessibility-api | 0.5.16 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1460—1472 | node_modules/entities | 8.0.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1473—1479 | node_modules/es-module-lexer | 2.1.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1480—1489 | node_modules/estree-walker | 3.0.3 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1490—1499 | node_modules/expect-type | 1.3.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1500—1517 | node_modules/fdir | 6.5.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1518—1532 | node_modules/fsevents | 2.3.3 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 1533—1545 | node_modules/html-encoding-sniffer | 6.0.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1546—1555 | node_modules/indent-string | 4.0.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1556—1562 | node_modules/is-potential-custom-element-name | 1.0.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1563—1570 | node_modules/js-tokens | 4.0.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1571—1611 | node_modules/jsdom | 29.1.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1612—1620 | node_modules/json2mq | 0.2.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 1621—1650 | node_modules/lightningcss | 1.32.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1651—1671 | node_modules/lightningcss-android-arm64 | 1.32.0 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 1672—1692 | node_modules/lightningcss-darwin-arm64 | 1.32.0 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 1693—1713 | node_modules/lightningcss-darwin-x64 | 1.32.0 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 1714—1734 | node_modules/lightningcss-freebsd-x64 | 1.32.0 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 1735—1755 | node_modules/lightningcss-linux-arm-gnueabihf | 1.32.0 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 1756—1776 | node_modules/lightningcss-linux-arm64-gnu | 1.32.0 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 1777—1797 | node_modules/lightningcss-linux-arm64-musl | 1.32.0 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 1798—1818 | node_modules/lightningcss-linux-x64-gnu | 1.32.0 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 1819—1839 | node_modules/lightningcss-linux-x64-musl | 1.32.0 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 1840—1860 | node_modules/lightningcss-win32-arm64-msvc | 1.32.0 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 1861—1881 | node_modules/lightningcss-win32-x64-msvc | 1.32.0 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 1882—1891 | node_modules/lru-cache | 11.5.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1892—1902 | node_modules/lz-string | 1.5.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1903—1912 | node_modules/magic-string | 0.30.21 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1913—1919 | node_modules/mdn-data | 2.27.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1920—1929 | node_modules/min-indent | 1.0.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1930—1948 | node_modules/nanoid | 3.3.18 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1949—1962 | node_modules/obug | 2.1.3 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1963—1975 | node_modules/parse5 | 8.0.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1976—1982 | node_modules/pathe | 2.0.3 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1983—1989 | node_modules/picocolors | 1.1.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 1990—2002 | node_modules/picomatch | 4.0.4 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2003—2021 | node_modules/playwright | 1.61.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2022—2034 | node_modules/playwright-core | 1.61.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2035—2049 | node_modules/playwright/node_modules/fsevents | 2.3.2 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 2050—2078 | node_modules/postcss | 8.5.23 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2079—2094 | node_modules/pretty-format | 27.5.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2095—2102 | node_modules/pretty-format/node_modules/react-is | 17.0.2 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2103—2112 | node_modules/punycode | 2.3.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2113—2121 | node_modules/qrcode.react | 4.2.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2122—2138 | node_modules/rc-cascader | 3.34.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2139—2153 | node_modules/rc-checkbox | 3.5.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2154—2169 | node_modules/rc-collapse | 3.9.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2170—2186 | node_modules/rc-dialog | 9.6.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2187—2203 | node_modules/rc-drawer | 7.3.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2204—2219 | node_modules/rc-dropdown | 4.2.1 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2220—2237 | node_modules/rc-field-form | 2.7.1 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2238—2255 | node_modules/rc-image | 7.12.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2256—2270 | node_modules/rc-input | 1.8.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2271—2287 | node_modules/rc-input-number | 9.5.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2288—2306 | node_modules/rc-mentions | 2.20.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2307—2324 | node_modules/rc-menu | 9.16.1 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2325—2339 | node_modules/rc-motion | 2.9.5 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2340—2358 | node_modules/rc-notification | 5.6.4 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2359—2374 | node_modules/rc-overflow | 1.5.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2375—2389 | node_modules/rc-pagination | 5.1.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2390—2428 | node_modules/rc-picker | 4.11.3 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2429—2443 | node_modules/rc-progress | 4.0.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2444—2461 | node_modules/rc-rate | 2.13.1 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2462—2477 | node_modules/rc-resize-observer | 1.4.3 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2478—2493 | node_modules/rc-segmented | 2.7.1 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2494—2515 | node_modules/rc-select | 14.16.8 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2516—2533 | node_modules/rc-slider | 11.1.9 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2534—2551 | node_modules/rc-steps | 6.0.1 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2552—2566 | node_modules/rc-switch | 4.1.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2567—2587 | node_modules/rc-table | 7.54.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2588—2609 | node_modules/rc-tabs | 15.7.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2610—2626 | node_modules/rc-textarea | 1.10.2 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2627—2642 | node_modules/rc-tooltip | 6.4.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2643—2662 | node_modules/rc-tree | 5.13.1 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2663—2679 | node_modules/rc-tree-select | 5.27.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2680—2694 | node_modules/rc-upload | 4.11.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2695—2708 | node_modules/rc-util | 5.44.4 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2709—2727 | node_modules/rc-virtual-list | 3.19.2 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2728—2736 | node_modules/react | 19.2.7 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2737—2748 | node_modules/react-dom | 19.2.7 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2749—2754 | node_modules/react-is | 18.3.1 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2755—2768 | node_modules/redent | 3.0.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2769—2778 | node_modules/require-from-string | 2.0.2 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2779—2784 | node_modules/resize-observer-polyfill | 1.5.1 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2785—2818 | node_modules/rolldown | 1.0.3 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2819—2825 | node_modules/rolldown/node_modules/@rolldown/pluginutils | 1.0.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2826—2838 | node_modules/saxes | 6.0.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2839—2844 | node_modules/scheduler | 0.27.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2845—2853 | node_modules/scroll-into-view-if-needed | 3.1.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2854—2860 | node_modules/siginfo | 2.0.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2861—2870 | node_modules/source-map-js | 1.2.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2871—2877 | node_modules/stackback | 0.0.2 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2878—2884 | node_modules/std-env | 4.1.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2885—2890 | node_modules/string-convert | 0.2.1 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2891—2903 | node_modules/strip-indent | 3.0.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2904—2909 | node_modules/stylis | 4.4.0 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2910—2916 | node_modules/symbol-tree | 3.2.4 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2917—2925 | node_modules/throttle-debounce | 5.0.2 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2926—2932 | node_modules/tinybench | 2.9.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2933—2942 | node_modules/tinyexec | 1.2.4 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2943—2959 | node_modules/tinyglobby | 0.2.17 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2960—2969 | node_modules/tinyrainbow | 3.1.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2970—2982 | node_modules/tldts | 7.4.3 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2983—2989 | node_modules/tldts-core | 7.4.3 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 2990—2995 | node_modules/toggle-selection | 1.0.6 | 技术性 | UI运行依赖；由实际根图可达，无业务规则发明 |
| 2996—3008 | node_modules/tough-cookie | 6.0.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 3009—3021 | node_modules/tr46 | 6.0.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 3022—3029 | node_modules/tslib | 2.8.1 | 技术性 | 平台或WASM可选依赖；由实际根图可达，无业务规则发明 |
| 3030—3043 | node_modules/typescript | 5.9.3 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 3044—3053 | node_modules/undici | 7.29.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 3054—3131 | node_modules/vite | 8.0.16 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 3132—3221 | node_modules/vitest | 4.1.9 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 3222—3234 | node_modules/w3c-xmlserializer | 5.0.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 3235—3244 | node_modules/webidl-conversions | 8.0.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 3245—3254 | node_modules/whatwg-mimetype | 5.0.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 3255—3269 | node_modules/whatwg-url | 16.0.1 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 3270—3286 | node_modules/why-is-node-running | 2.3.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 3287—3296 | node_modules/xml-name-validator | 5.0.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 3297—3303 | node_modules/xmlchars | 2.2.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |
| 3304—3319 | node_modules/yaml | 2.9.0 | 技术性 | 开发构建测试所需依赖；由实际根图可达，无业务规则发明 |

| 3320—3321 | JSON闭合 | — | 技术性 | 完整协议结构，无业务含义 |

## 3. 第一性原理全线
锁的目的不是重复罗列package.json，而是固定位置、版本、下载源和可选平台图供npm一致安装；删除它会失去当前ci消费的确定输入。root/web/link三项承接唯一工作区，其余217项是清单实际可达依赖；源声明17项不等于完整图只有17项。
普通runtime包、dev包、peer工具和不同平台optional是安装角色，不是业务权限/业务默认值。34个optional中只有28带os/cpu，其余6个WASM辅助包仍可选；29个缺失peerOptional是原生允许状态。嵌套同名不同版和同版不同位置不等于可直接删除的重复文件。
信任边界在npm从锁确定下载位置及完整性并在消费时校验；本次证明锁字段与原生校验行为，没有下载217个tarball，未把形态证明成远端字节真实性。
版本命中公告不自动等于当前正常入口接通缺陷通道。007须核实际配置及上游条件；下面保留命中和exit1，在冻结GEN/铁律6范围作处置，无新判据。

## 4. 顺从式全线：18项冻结判据
| GEN | 本次证据及适用边界 | 本席 |
|---|---|---|
| GEN-001 | 220条全部由根图可达、无extraneous/unreachable；嵌套解析位置和平台可选依赖有实际消费者，不为去重手删。 | PASS |
| GEN-002 | 原生Arborist解析220条/582边，无无效必需边；npm ci离线dry-run真实0。只证明当前解析/安装预演，不称全构建或实装通过。 | PASS |
| GEN-003 | 本模块无测试实现；本次缺react、错react版本、错误SRI payload、Node下界断言能实际失败，不用计数充当行为测试。 | PASS |
| GEN-004 | 全部217个resolved为registry.npmjs.org的HTTPS，无URL凭据/query/fragment；全部217完整SRI合法且ssri拒绝错payload。007通道边界另述。 | PASS |
| GEN-005 | 锁无主体/对象授权动作，未引入角色权限业务规则；无将客户端隐藏当服务端授权的实现。 | PASS |
| GEN-006 | JSON逐键唯一、根/web镜像deepEqual、全部引用有效；原生版本校验失败会报告Missing/Invalid，未发生源写入。 | PASS |
| GEN-007 | 锁为静态安装图，无业务重试/事务/并发写路径；没有增设新锁或幂等框架的事实依据。 | PASS |
| GEN-008 | 17项直接声明与锁镜像一致；react-dom19.2.7的react要求由react19.2.7满足；workspaces与apps-web链接匹配。 | PASS |
| GEN-009 | 原生validator反例显式返回Missing/Invalid；npm audit真实exit1保留，不转为全绿；ci预演exit0和audit非0分开报告。 | PASS |
| GEN-010 | 没有URL凭据或明文秘密；217HTTPS/SRI形态通过。Vitest公告版本命中仍在，当前配置无对应公开插件入口，不写公告误报。 | PASS |
| GEN-011 | 本次命令、真实输出、源SHA和当前阶段分别记录；应用日志不在此锁内，不虚构已跑部署/全测试。 | PASS |
| GEN-012 | 所有包有有限静态记录、实际图无不可达垃圾；本模块未实现运行期无界循环/查询/缓存，未设新性能门禁。 | PASS |
| GEN-013 | 实际Node/npm记录；116个node范围相容、28带平台约束条目均optional、29缺peerOptional允许；原生ci预演与清单匹配通过。 | PASS |
| GEN-014 | 本模块无SQL迁移或破坏性数据库动作；未以审锁为理由执行数据库变更。 | PASS |
| GEN-015 | 锁携带UI/测试依赖但未实现界面、焦点或标签；不把依赖存在当UI可访问性通过，不扩大到产品页面。 | PASS |
| GEN-016 | 应用SemVer、lock协议、文件documentVersion分开核验；本次SHA/命令可复现；未借旧整体hash、旧PASS或旧tarball结果。 | PASS |
| GEN-017 | 上表220项及顶层/闭合均依序分类为技术性；未引入业务实体/枚举/权限/金额/流程；无添加项或新HTML问题。 | PASS |
| GEN-018 | 依据为当前锁与消费方可执行事实、npm协议/ci规范、主公告原文和本次可复现命令，未把席位意见当唯一依据。 | PASS |

## 5. 对抗式全线
| 反例或挑战 | 本次事实 | 判定 |
|---|---|---|
| 缺一条必要包仍通过 | 原生validator内存删react→Missing: react@19.2.7 from lock file | 反例被识别 |
| 错版本仍通过 | react改0.0.0→Invalid与期望19.2.7 | 反例被识别 |
| peerOptional缺失全部必须补 | 原生29个缺失optional，真正无效边0 | 不造29个修复项 |
| 其它平台包阻碍本机正常入口 | 28有平台约束均optional；ci离线预演0 | 当前Linux预演有效，不声称跨平台实装 |
| 根Node下界和依赖矛盾 | 116条semver.subset通过，22.12拒绝/22.13接受 | 下界无漂移 |
| 摘要仅貌似合法 | 217个strict sha512解析通过；同一错误payload全部拒绝 | 行为成立，真实tarball未下载 |
| 清单偷偷漂移/链接错误 | 根/web字段深比较通过；apps-web→apps/web实际目标正确 | 未见漂移 |
| 预演实际上删node_modules | 先前已读同版ci.js，今次再全文读；dry-run/ignore-scripts，清单hash及目录mtime不变 | 无实际安装删除 |
| 库版本命中就认当前入口有缺陷 | 007当前vite/vitest仅react，jsdom，无公开mock插件/browser接线 | 保留告警，不自动升级 |
| 旧安装目录能运行证明锁全部正确 | 本次只以明确来自锁的虚拟图及原生预演举证；既有目录并非完全等于锁 | 不使用此错误推论 |

## 6. 本次实际执行证据
所有远程操作经直接SSH；仅根Codex此席，没有Claude Agent、MCP_Cloud_160、MCP_Codex_160或第三席。技能沿用本会话已实读的using-superpowers、grilling、codebase-design、systematic-debugging/diagnosing-bugs、ponytail-review、verification-before-completion、writing-for-agents的收窄用法：自行追问、只读追消费链、真实校验后给票。没有伪称本节点重新阅读全文技能；无行为变更，不新造TDD文件；可选技能缺口沿既有实际核验记录，不冒充新增调用。
结构读取命令为SSH python3 -：UTF8全文→json.loads逐键唯一→全部220位置对应源码→逐条全部元数据字段→217完整Base64/URL检查。分段索引0—79/80—159/160—219，本席均已读；因第一段显示截断补读855—1102，顶层/闭合另完整输出。
原生行为检查本次实际执行的JavaScript（node -，真实exit0）：
```javascript
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {createRequire}=require('node:module');
const nr=createRequire('/usr/lib/node_modules/npm/package.json');
const Arborist=nr('@npmcli/arborist'),semver=nr('semver'),ssri=nr('ssri');
const validate=nr('./lib/utils/validate-lockfile.js');
(async()=>{
const root=process.cwd(),lock=JSON.parse(fs.readFileSync('package-lock.json','utf8'));
const manifests=['','apps/web'].map(p=>[p,JSON.parse(fs.readFileSync(path.join(p,'package.json'),'utf8'))]);
for(const [p,m] of manifests) for(const field of ['name','version','dependencies','devDependencies','optionalDependencies','engines','workspaces']){
 assert.deepEqual(lock.packages[p][field],m[field],(p||'root')+' '+field);
}
const npmVersion=nr('./package.json').version;
const integrity=[],engine=[],platform=[];
for(const [loc,p] of Object.entries(lock.packages)){
 if(p.integrity){const sri=ssri.parse(p.integrity,{strict:true});assert(sri?.sha512?.length===1,loc);assert.equal(ssri.checkData(Buffer.from('M004-negative-not-package-tarball'),p.integrity),false);integrity.push(loc);}
 if(p.engines?.node){assert(semver.satisfies(process.versions.node,p.engines.node),loc);assert(semver.subset(manifests[0][1].engines.node,p.engines.node),loc+' root range mismatch');engine.push(loc);}
 if(p.engines?.npm)assert(semver.satisfies(npmVersion,p.engines.npm),loc+' npm engine');
 if(p.os||p.cpu){assert.equal(p.optional,true,loc);platform.push(loc);}
 if(p.link){assert(loc.startsWith('node_modules/'));assert(lock.packages[p.resolved],loc);assert.equal(manifests.find(([mp])=>mp===p.resolved)[1].name,loc.substring('node_modules/'.length));}
}
const arb=new Arborist({path:root,offline:true,audit:false,ignoreScripts:true,save:false});
const tree=await arb.loadVirtual();
const invalid=[],missingOptional=[],extra=[];let edges=0;
for(const n of tree.inventory.values()){
 if(n.extraneous)extra.push(n.location);
 for(const e of n.edgesOut.values()){edges++;if(e.optional&&!e.to)missingOptional.push({from:n.location,name:e.name,type:e.type});if(!e.valid&&!(e.optional&&!e.to))invalid.push({from:n.location,name:e.name,spec:e.spec,type:e.type,error:e.error,to:e.to?.location});}
}
assert.deepEqual(invalid,[]);assert.deepEqual(extra,[]);
const expected=new Map(tree.inventory);
assert.deepEqual(validate(new Map(expected),expected),[]);
const victim='node_modules/react';assert(expected.has(victim));
const deleted=new Map(expected);deleted.delete(victim);const miss=validate(deleted,expected);assert(miss.some(x=>x.startsWith('Missing: react@')));
const changed=new Map(expected);changed.set(victim,{...expected.get(victim),version:'0.0.0'});
const bad=validate(changed,expected);assert(bad.some(x=>x.includes("react@0.0.0")));
assert.equal(semver.satisfies('22.12.0',manifests[0][1].engines.node),false);
assert.equal(semver.satisfies('22.13.0',manifests[0][1].engines.node),true);
const seen=new Set(),stack=[tree];while(stack.length){const n=stack.pop();if(seen.has(n))continue;seen.add(n);if(n.isLink)stack.push(n.target);for(const e of n.edgesOut.values())if(e.to)stack.push(e.to);}
const unreachable=[...tree.inventory.values()].filter(n=>!seen.has(n)).map(n=>n.location);assert.deepEqual(unreachable,[]);
console.log(JSON.stringify({node:process.version,npm:npmVersion,lockEntries:Object.keys(lock.packages).length,inventory:tree.inventory.size,edges,invalid,extra,unreachable,missingOptional,integrityFormatPass:integrity.length,wrongPayloadRejected:integrity.length,engineRangesPass:engine.length,platformOptional:platform.length,manifestsMatch:manifests.map(([p])=>p||'root'),negativeMissing:miss,negativeVersion:bad},null,2));
console.log('M004_NATIVE_LOCK_GRAPH_ASSERTIONS_PASS');
})().catch(e=>{console.error(e);process.exit(1)});
```
输出及同次ci预演摘要：
```text
NATIVE_GRAPH_FINAL EXIT 0
{
  "node": "v24.20.0",
  "npm": "11.19.0",
  "lockEntries": 220,
  "inventory": 220,
  "edges": 582,
  "invalid": [],
  "extra": [],
  "unreachable": [],
  "missingOptional": [
    {
      "from": "apps/web/node_modules/@vitejs/plugin-react",
      "name": "@rolldown/plugin-babel",
      "type": "peerOptional"
    },
    {
      "from": "apps/web/node_modules/@vitejs/plugin-react",
      "name": "babel-plugin-react-compiler",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/@exodus/bytes",
      "name": "@noble/hashes",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/@vitest/mocker",
      "name": "msw",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/jsdom",
      "name": "canvas",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/rc-picker",
      "name": "date-fns",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/rc-picker",
      "name": "luxon",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/rc-picker",
      "name": "moment",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vite",
      "name": "@types/node",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vite",
      "name": "@vitejs/devtools",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vite",
      "name": "esbuild",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vite",
      "name": "jiti",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vite",
      "name": "less",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vite",
      "name": "sass",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vite",
      "name": "sass-embedded",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vite",
      "name": "stylus",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vite",
      "name": "sugarss",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vite",
      "name": "terser",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vite",
      "name": "tsx",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vitest",
      "name": "@edge-runtime/vm",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vitest",
      "name": "@opentelemetry/api",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vitest",
      "name": "@types/node",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vitest",
      "name": "@vitest/browser-playwright",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vitest",
      "name": "@vitest/browser-preview",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vitest",
      "name": "@vitest/browser-webdriverio",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vitest",
      "name": "@vitest/coverage-istanbul",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vitest",
      "name": "@vitest/coverage-v8",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vitest",
      "name": "@vitest/ui",
      "type": "peerOptional"
    },
    {
      "from": "node_modules/vitest",
      "name": "happy-dom",
      "type": "peerOptional"
    }
  ],
  "integrityFormatPass": 217,
  "wrongPayloadRejected": 217,
  "engineRangesPass": 116,
  "platformOptional": 28,
  "manifestsMatch": [
    "root",
    "apps/web"
  ],
  "negativeMissing": [
    "Missing: react@19.2.7 from lock file"
  ],
  "negativeVersion": [
    "Invalid: lock file's react@0.0.0 does not satisfy react@19.2.7"
  ]
}
M004_NATIVE_LOCK_GRAPH_ASSERTIONS_PASS

CI_DRY_COMMAND ["npm", "ci", "--dry-run", "--ignore-scripts", "--offline", "--no-audit", "--no-fund", "--json"] EXIT 0
CI_DRY_SUMMARY {"add": 188, "added": 188, "audited": 0, "change": 0, "changed": 0, "funding": 0, "remove": 186, "removed": 186}
STDERR 
CONSUMER_CONTEXT {"path": "node_modules", "exists": true, "symlink": false, "link_target": null}
CONSUMER_CONTEXT {"path": "node_modules/node_modules", "exists": true, "symlink": true, "link_target": "/home/james/complex-management/node_modules"}
CONSUMER_CONTEXT {"path": "apps/web/node_modules", "exists": true, "symlink": false, "link_target": null}
CONSUMER_CONTEXT {"path": "npm-shrinkwrap.json", "exists": false, "symlink": false, "link_target": null}
MANIFEST_BYTES_AND_NODE_MODULES_DIR_MTIMES_UNCHANGED
LOCK_SHA d3676e896b885b6bbb979440bb51b9dfe60728735cee000e528ab5d1791bcfef

```
npm audit实际本次输出（注意命令exit1，包装Python exit0仅表示源hash未变断言成功）：
```text
COMMAND ["npm", "audit", "--package-lock-only", "--ignore-scripts", "--json"] EXIT 1
{
  "auditReportVersion": 2,
  "vulnerabilities": {
    "@vitest/mocker": {
      "name": "@vitest/mocker",
      "severity": "moderate",
      "isDirect": false,
      "via": [
        {
          "source": 1193684,
          "name": "@vitest/mocker",
          "dependency": "@vitest/mocker",
          "title": "Vitest: Path Traversal / Arbitrary File Read via @vitest/mocker Redirect Mock",
          "url": "https://github.com/advisories/GHSA-82fw-gwwq-j7x9",
          "severity": "moderate",
          "cwe": [
            "CWE-22"
          ],
          "cvss": {
            "score": 5.9,
            "vectorString": "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:N/A:N"
          },
          "range": ">=2.1.0 <4.1.11"
        }
      ],
      "effects": [
        "vitest"
      ],
      "range": "2.1.0 - 4.1.10",
      "nodes": [
        "node_modules/@vitest/mocker"
      ],
      "fixAvailable": true
    },
    "vitest": {
      "name": "vitest",
      "severity": "moderate",
      "isDirect": true,
      "via": [
        "@vitest/mocker",
        {
          "source": 1193683,
          "name": "vitest",
          "dependency": "vitest",
          "title": "Vitest: Path Traversal / Arbitrary File Read via @vitest/mocker Redirect Mock",
          "url": "https://github.com/advisories/GHSA-82fw-gwwq-j7x9",
          "severity": "moderate",
          "cwe": [
            "CWE-22"
          ],
          "cvss": {
            "score": 5.9,
            "vectorString": "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:N/A:N"
          },
          "range": ">=2.1.0 <4.1.11"
        }
      ],
      "effects": [],
      "range": "2.1.0-beta.1 - 4.1.10",
      "nodes": [
        "node_modules/vitest"
      ],
      "fixAvailable": true
    }
  },
  "metadata": {
    "vulnerabilities": {
      "info": 0,
      "low": 0,
      "moderate": 2,
      "high": 0,
      "critical": 0,
      "total": 2
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
PROTECTED_UNCHANGED {"package-lock.json": "d3676e896b885b6bbb979440bb51b9dfe60728735cee000e528ab5d1791bcfef", "package.json": "350999a90ddf85b2d3dcbee090dd593fb7faff18d0f467f0a608d8b3664d473f", "apps/web/package.json": "b8017d9782b4c00eac1c992204dad2b009e23db7c4c804737bdf8fcbc90b8f1f"}

```
另本次真实运行：
- rg -n --glob '!node_modules/**' --glob '!package-lock.json' '@vitest/(mocker|browser)|mockerPlugin|interceptorPlugin' apps/web：exit1、无匹配。
- node tools/verify-no-legacy-api-prototype.mjs：exit0，NO_LEGACY_API_PROTOTYPE_PASS。
- git status --porcelain -- package-lock.json package.json apps/web/package.json：exit0、空；git diff --check -- package-lock.json：exit0、空。
- git rev-parse --abbrev-ref HEAD：main；git rev-parse HEAD origin/main：均242f487233d028173cb9198eecd079f5d877405c。

## 7. 007观察与当前发现
本次audit仍只有同一GHSA-82fw-gwwq-j7x9对应两包；4.1.9在受影响范围、修补版4.1.11，moderate2/high0/critical0，exit1。当前配置全文已重新读：vite plugins仅react，vitest plugins仅react且environment jsdom，无browser配置；上述rg无注册点。dev的host0.0.0.0不等于公开mock插件已注册。
本会话已直接读的主公告https://github.com/vitest-dev/vitest/security/advisories/GHSA-82fw-gwwq-j7x9明确公开插件注册通道和自身browser鉴权通道之别；本次未用Grok B5结论佐证，基于当前配置/锁/本次audit独立判断。按冻结GEN004/010/013与铁律6，当前正常入口未接通公告读取通道，007维持技术性/无当前待修工程缺陷/队列否。不能说公告误报、永远不可触发或npm audit全绿。这里没有新finding或新增编号。
001—003当前nanoid3.3.18/postcss8.5.23及nanoid^3.3.16/undici7.29.0，当前audit未列出它们；保留旧实际缺陷及修复史，不说从未有问题。004—006的元数据/全图/源完整性登记由本次全量新检查重新支持。
本次全部新finding：空集。没有冲突、添加、新HTML自身问题或未答人闸。锁的纯技术性分类没有业务HTML上位锚点，不以HTML沉默强制删除技术配置。

## 8. 派发回执、保护与结论
Grok本次完整实际命令：
```powershell
$remoteScript = @'
cd /home/james/complex-management && python3 - <<'PY'
from pathlib import Path
import subprocess,sys,stat
target=Path('/home/james/complex-management/docs/audit-2026-09/modules/M004-b5-r1-grok-raw.md')
def meta():
 if not target.exists(): return None
 s=target.stat()
 return (s.st_size,s.st_mtime_ns,s.st_ctime_ns,s.st_ino)
before=meta()
prompt="执行160-audit-prompt V0.17当前471计划M004 B5-r1完整独立审计。正本仅/home/james/complex-management main。模块名“治理门禁与 CI 基线／配置与工程入口／第2段”；仅package-lock.json全3321行/220条目/OVERSIZE-SINGLE-FILE，documentVersion V0.02。当前B2双PASS已定稿findings V0.94，7项当前修复队列空，跳过本次B3/B4；历史B3累计1、方案修复2、交付失败0保持，M004仍IN_PROGRESS。不能用旧322同号PASS、B1或B2票替代本次完整B5。\n先读当前plan/modules.md的M004完整范围、runbook.md、criteria-lock.md全部GEN和M004映射，18GEN逐条覆盖。完整重新读取package-lock.json的所有字段/每条依赖/平台/版本/完整性，给连续3321行覆盖和全部220条技术分类。允许只读根/web清单、真实npm原生消费实现、CI中的锁入口、vite/vitest配置及公告相关当前调用路径作为直接对照，不扩大其它模块审计。\n独立互盲：不读本次或历史任何self raw、其他模块审计/收口，也不读本模块旧B5正文或旧审计结论；旧raw只取版本/大小/hash便于追加。findings只可取当前M004定稿ID和队列，不凭已有票或结论通过。先你独立结束、根再独立全审保存，最后根才读你的当前完整报告。只此Grok一外审，禁任何subagent、MCP_Codex_160、MCP_Cloud_160；直接已有shell/SSH工具。\n只审不修：只写下方指定Grok raw，禁止任何依赖/代码/HTML/计划/锁/ledger/findings/questions修改，禁提交推送部署/新worktree/安装依赖/npm audit fix；不得新增审计产物。可运行已读懂且明确无副作用的npm锁解析、offline/dry-run、公告只读查询；真实exit逐一记录，不能把dry-run等同安装/构建/测试通过。既有node_modules不是必然等于锁；排除范围不审，只作为工具/直接消费上下文。\n三线必须整个模块均覆盖：第一性原理追锁/每项元数据的最小用途及真实消费行为；顺从式逐18GEN、root/workspace一致性、所有包与edges/optional/peer/engine/source/SRI验证；对抗式构造正常首跑的错误版本/缺失引用/清单漂移/源完整性/平台等反例，用既有原生工具或纯内存断言。不要只JSON.parse/计数/grep就声称全审，不要为极端理论路径加内容或判据。\n四分类依次冲突→细化→技术性→添加。技术性须逐条理由：仅安装/构建/测试/运行所需且无业务发明，不强求每行HTML锚点。若有具体HTML上位，读准确文件+原文并先归细化；不把业务实体/枚举/权限伪装技术性。三HTML只读。用户B/C“就是我的原意”继续有效，不重判冲突。\n已知观察只给事实，不预给你本次票：npm audit在B2返回真实exit1，moderate2，vitest及@vitest/mocker4.1.9同一GHSA-82fw-gwwq-j7x9。你应本次核当前公告/调用通道并依冻结GEN004/010/013及铁律6独立判断，不能宣称audit绿/公告误报/永不可触发。主来源https://github.com/vitest-dev/vitest/security/advisories/GHSA-82fw-gwwq-j7x9，若不能获取说明真实限制；当前vite/vitest配置可只读核。不要凭标题/包计数自造门禁，也不得忽略本次真实工程缺陷。\n全局已知html-ledger-sync/entry-anchors因BSM与镜像CSV不一致而红，归属M425/M427等既定模块；本模块不为刷绿改CSV/HTML，不重开M001—M003。全局红不自动证明锁坏，也不因为模块PASS称全仓/发布绿。\n按§6先实际核技能/读适用SKILL.md，复用已读方法须如实说明：using-superpowers、grilling/grill-me自行追问、codebase-design、diagnosing-bugs/systematic-debugging只读追根、ponytail-review最小删除、verification-before-completion；可选batch-grill-me/ubiquitous-language有则收窄用、无则记缺口。不派第三席、不向人新增问题、不写额外文件。无源行为变更不新增TDD文件。\n每条新finding给GR临时号、准确路径行号、证据/真实命令输出、冻结GEN ID、类型1—4、四分类、工程缺陷和队列建议。整个模块全部审完把全部finding收齐再给本席B5 PASS或FAIL，不中途转修复。报告需包含18GEN逐项、全部220条/3321行连续覆盖、三线证据、实际检查边界、未做事项、HTML问题/人闸是否有、源前后不变核验；不能以旧报告代替本次新证据。\n独立实算criteria-lock.md SHA，本次报告第一行独占CRITERIA-LOCK-SHA256: <实际64位小写hex>，不给预期值，正文不再写独占同字段。判据不得扩锁或静默对齐。唯一报告路径/home/james/complex-management/docs/audit-2026-09/modules/M004-b5-r1-grok-raw.md；当前旧V0.01全部95358字节保留，在末尾追加本次完整报告，自身版本V0.02；不能覆盖旧报告。完整落盘后结束，不要求stdout机器标记。"
cmd=['/home/james/.local/bin/grok','-p',prompt,'-m','grok-4.6','--reasoning-effort','xhigh','--output-format','json','--permission-mode','bypassPermissions','--no-subagents','--disable-web-search','--cwd','/home/james/complex-management']
proc=subprocess.Popen(cmd,cwd='/home/james/complex-management',stdout=subprocess.PIPE,stderr=subprocess.PIPE)
while True:
 try:
  output,error=proc.communicate(timeout=300)
  break
 except subprocess.TimeoutExpired: pass
output=error=None
after=meta()
code='GROK_EXIT' if proc.returncode else ('REPORT_MISSING' if after is None else ('PATH_MISMATCH' if not target.is_file() or target.resolve()!=target else ('REPORT_EMPTY' if after[0]==0 else ('REPORT_STALE' if after==before else None))))
if code:
 print('ADAPTER_ERROR='+code)
 print('EXIT_CODE='+str(proc.returncode))
 sys.exit(proc.returncode or 1)
print('REPORT_PATH='+str(target))
print('EXIT_CODE=0')
PY
'@
& 'C:\Windows\System32\OpenSSH\ssh.exe' -o BatchMode=yes james@192.168.88.160 $remoteScript
```
适配器真实输出：
REPORT_PATH=/home/james/complex-management/docs/audit-2026-09/modules/M004-b5-r1-grok-raw.md
EXIT_CODE=0
根在此回执之后开始本次全量审计。上述只证明交付；写本报告时Grok正文未读，当前不能写双PASS或模块PASS。

本次三个可见基准实算/sidecar/ledger均b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a；尚未取本次Grok独占行，B5后四值检查须本报告保存后进行。COUNT1未变。
源保护本次SHA：锁d3676e896b885b6bbb979440bb51b9dfe60728735cee000e528ab5d1791bcfef；root350999a90ddf85b2d3dcbee090dd593fb7faff18d0f467f0a608d8b3664d473f；webb8017d9782b4c00eac1c992204dad2b009e23db7c4c804737bdf8fcbc90b8f1f。三HTML为BEQR4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66、BSMa7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa、PGMc4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c，均保持。
上游协议参考本会话已读npm package-lock-json及npm-ci官方文档；本机真实解析器优先于网上不同补丁版说明。没有下载真实tarball、npm实装、全前后端构建、业务UI、数据库迁移或部署结论。已知全局BSM镜像门禁红灯保留，未重跑/修CSV或以本模块PASS冒称全仓发布通过。
本席B5 PASS。M004仍IN_PROGRESS、B3=1。下一步读Grok本次完整B5、校验四值；任一FAIL收齐整批再按V0.17路由，双PASS且四值一致才先写ledger PASS、再写模块收口。

## M004 当前B5双席接收、版本登记更正与四值回执
版本：V0.03

当前文件版本为V0.03。收口时读取旧收口及旧前缀发现，本文件旧前89089字节中L407已有“V0.02 外席接收与事实校正”标题；本次独立追加段误用了V0.02（只核了旧首个版本行V0.01）。此处更正当前文件版本并明确报告定位，原始字节不覆盖、独立票不改写、不重新冒称互盲审计。
本次独立Codex完整B5位于旧89089字节之后，保存时全文件150825字节、SHA bc8c0d5beb4b67b5d86900bdd8c7521e94d4afe0f1d8cc2a91310c00eb1ff2ba，追加从L418首空行开始、独占锁行为L419；完整668追加行。它已在读取本次Grok正文前保存。误用版本号不是源修复，本次接收不改变其PASS及全部内容。
当前Grok完整B5 V0.02：modules/M004-b5-r1-grok-raw.md，162406字节/SHA951be0608835cd8ed9df191bee4646cb5aeafcc97dd07b96ec1514116f97ffd1；旧95358字节SHA31f7bd9586520ad2ab89fd4b3bbc5a95fa596d081b68a2a343ce60af5ced264f保持。本次追加L430—935（首空行，独占锁行L431），全部506行已读。实际Grok命令和路径型回执见前报告§8；真实exit0。两席全3321行/220条/18GEN三线完整PASS，新finding空集，007观察维持现定稿，无待答闸。
四值已在两份本次报告齐全后实算：actual=sidecar=ledger=本次指定Grok最后独占行=b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a，LOCK-AMEND-COUNT=1。没有对齐或修改判据。
接收时不采信Grok报告中三处统计展示作为额外事实：§3.2的Arborist edgesOut563未由本次根原生复算重现（同Arborist9.9.1实得582；Grok自己的§9.1也给声明边582）；表中root/web/link三行“SRI”标志不表示存在摘要（其正文也明确三行无此协议要求，实值只有217）；yaml表末行止3321包含两个闭合符，其正文§8已明确实际3304—3319。原文保留，不代改其PASS；当前采信原始字段、完整220条表、原生有效性检查及两席明示边界，不声称两席所有计数字段逐项一致。
边数差异实核命令为node -，脚本：
```javascript
const {createRequire}=require('node:module');const nr=createRequire('/usr/lib/node_modules/npm/package.json');const A=nr('@npmcli/arborist');(async()=>{let t=await new A({path:process.cwd(),offline:true,audit:false,ignoreScripts:true,save:false}).loadVirtual();let sums={all:0,nonRoot:0,nonWorkspace:0,nonLink:0,nonTop:0};let specials=[];for(let n of t.inventory.values()){const v=n.edgesOut.size;sums.all+=v;if(!n.isRoot)sums.nonRoot+=v;if(!n.isWorkspace)sums.nonWorkspace+=v;if(!n.isLink)sums.nonLink+=v;if(n.location&&n.location.startsWith('node_modules/'))sums.nonTop+=v;if(n.isRoot||n.isWorkspace||n.isLink)specials.push({location:n.location,size:v,isRoot:n.isRoot,isWorkspace:n.isWorkspace,isLink:n.isLink});}console.log(JSON.stringify({arborist:nr('@npmcli/arborist/package.json').version,sums,specials}));})();
```
实际exit0：arborist9.9.1；all582、nonRoot579、nonWorkspace567、nonLink582、nonTop560；root3、web15、link0。未把563硬解释成已证实的另一统计口径；它不改变完整条目/已验证的边有效性和两席无新工程缺陷结论。本次没有任何源改动。
当前步骤B5 / M004共471，B3历史累计1；ledger此时IN_PROGRESS。B2定稿findings V0.94七项当前队列全否，无添加/HTML问题/待答闸，无新修复。两席audit均保留exit1、同一GHSA中等2包，不报全绿。全局BSM镜像门禁红仍保留。本次双PASS与四值相等成立，下一步先写ledger PASS，再写收口并进入下一模块。
