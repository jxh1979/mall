CRITERIA-LOCK-SHA256: 23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
# M004 B1 本方完整独立审计
版本：V0.01

时间：2026-09-05T12:16:42.890101+00:00；控制V0.17；M004/322，IN_PROGRESS，B3=0。唯一源package-lock.json：114212字节/3320行/SHA256=0e468712d5240cb669b847ecb4250c03e35e38a52aaf11e3013eb8b717dec60d；HEAD=9f7955ebc8816e4fec39b5fd2c9920ab79a29d9b。本方本次独立报告完成前未读Grok本次raw或JSON，仅见运行会话未结束。
## 范围与完整读取方式

只审计划M004单文件，OVERSIZE-SINGLE-FILE不重切、不抽样。原始1—1100行首次显示受工具长度截断；随后用完整JSON严格解析并按全部220个包记录读取全部语义字段，缺失显示段1368—1961另外完整输出补读。每条原始行范围列在下表，顶层1—6和闭合3319—3320另核；字段未抽样。217条registry tarball的resolved/integrity是完整机械检查，不冒称逐字人工理解不透明摘要：HTTPS唯一registry.npmjs.org、路径包名与version一致、sha512和64字节base64均过；对216个去重name/version逐个请求官方registry元数据，全部tarball/SRI精确相等（pluginutils同版本重复位置导致217/216差异）。完整数组、依赖边、engines、平台条件、license/funding/bin均按记录核对。

没有新业务角色、收费、资格、审批或状态机。这是安装、构建、测试及现有UI库的技术性闭包；三HTML沉默不导致删除依赖。HTML不改；直系package.json、apps/web/package.json和predeploy workflow仅核入口一致性，不重开M003或提前开启M005。

## 第一性原理

必要性：锁记录当前根清单及apps/web工作区、固定全部传递版本与来源；CI predeploy-gates.yml第22行真实npm ci --ignore-scripts依赖此文件。删掉后该确定安装入口无法照常运行，不能以“自动生成”免审或删掉。

真实行为：npm ls --all --package-lock-only --json真实exit0；npm ci --dry-run --ignore-scripts --no-audit --no-fund --loglevel=error真实exit0，预演188 added/186 removed是预演输出，不是本次真的安装删除，也不证明tarball脚本或全构建通过。当前root/web两个清单与锁中name/version/dependencies/devDependencies/workspaces/engines逐项相等。

未核前提逐一验证：严格JSON无重键；全部220记录覆盖；全部217 tarball/216去重官方来源比对无差异；npm实际Arborist loadVirtual核582依赖边无无效必需边，当前Node v24.20.0满足全部记录engines。内存拆掉react后立即检出50个无效react引用，反例有真实可失败断言；不修改源或node_modules。原输出inventory219是拆掉react后的数量，不能写成原锁少一条。

根engines >=22允许的范围比vite需22.12/jsdom需22.13宽，当前160 Node24.20.0及CI所取最新22实跑正常。记录该前提限制，不把未实际使用的旧22.0/奇数Node环境假定为本次正常运行故障，不在本模块擅改根清单。npm宿主11.19.0与packageManager10.9.8是现环境差异，本次原生解析结果明确对应11.19.0；没有谎称跑过10.9.8。

## 顺从式18条冻结判据

| ID | 结果与具体证据 |
|---|---|
| GEN-001 | 技术用途成立；全部节点为安装闭包，平台optional包服务跨平台分发，不把当前Linux不用的Darwin包当死代码。 |
| GEN-002 | JSON/npm ls/ci预演成功；只证明锁解析与安装计划，不是完整业务执行。 |
| GEN-003 | Arborist删react内存反例真实检出50错误；库级负输入与合成map实测，详下文；不造镜像源码断言来声称业务测试。 |
| GEN-004 | 全URL HTTPS官方域、无URL凭据、tarball/SRI逐项匹配；库层负输入缺陷见CX-01—03，项目外部输入可达性另明确界限。 |
| GEN-005 | 无本文件提供的服务端授权/主体/动作，不适用业务授权执行；依赖名不是新增业务权限。 |
| GEN-006 | workspace链接apps/web存在且元数据相等，582边一致；无数据库或金额持久化。 |
| GEN-007 | 是静态锁，不实现业务并发/幂等；不以可重复JSON解析代替交易幂等证明。 |
| GEN-008 | 根/web清单精确一致；react/react-dom及UI/test peer均被npm虚拟树验证；无OpenAPI/SQL模式声明。 |
| GEN-009 | audit实际exit1明确暴露安全告警；ls/ci预演没有吞错；脚本一次括号笔误在执行前失败并纠正，不计Grok重跑。 |
| GEN-010 | FAIL：锁仍固定3个有已发布安全修复的开发依赖。下文分别给真实库级反例与官方信息；不宣称生产业务可利用。 |
| GEN-011 | 每个record有锁定version/resolved/integrity，可从官方元数据复核；B1命令/exit保留。 |
| GEN-012 | 锁体静态有界；库级nanoid负输入/零长挂起已以1秒子进程终止重现，见CX-01；未在运行服务做DoS。 |
| GEN-013 | FAIL：可复现安装当前已知缺陷版本；三个修复版本均在父现有semver范围且engines相容，无需新依赖或改业务规则。 |
| GEN-014 | 无SQL/迁移行为，不适用数据库执行；未连库。 |
| GEN-015 | 不提供UI页面或可访问性实现；现有UI依赖只是技术锁，不用本文件替应用无障碍背书。 |
| GEN-016 | 所有版本/URL/bin/platform元数据来源可核；没有业务完成声明；lockfileVersion=3是格式号，包semver是生态协议版本，不等于审计文档Vx.xx。 |
| GEN-017 | 全部220记录按冲突→细化→技术性→添加核；没有具体业务发明，不走添加人闸。每条技术登记在覆盖表。 |
| GEN-018 | 核心为类型2/4真实解析/库调用/命令；上游修复范围为类型3官方公告/registry；不凭席位意见放行。 |

## 对抗式全范围检查

尝试重复键、缺边、错误workspace镜像、恶意下载域、URL凭据、缺/坏SRI、平台强制依赖、engines不符、漂移peer、安全范围命中。当前锁无结构/图/来源坏项；内存缺react的负例能被实际解析模型检出。所有optional平台bindings版本与父rolldown/lightningcss精确一致，Darwin fsevents安装脚本由CI ignore-scripts跳过；没有在审计中执行安装脚本。

安全扫描全开发闭包实际命中3个high依赖告警，生产集合--omit=dev实际0。不得将“prod0”写成“全闭包无漏洞”，亦不得把依赖告警写成线上消费客人已能攻击。下列三项是锁配置携带已知库缺陷的工程问题；具体可利用性如实限定。

## 本次全部发现

### CX-01 — nanoid锁3.3.12含已修复无限循环（中；开发依赖）
- 位置：package-lock.json:1929—1947，version第1930行；由postcss:2069的^3.3.12传递引入（精确依赖段见下表）。
- 分类：技术性；工程缺陷成立；GEN-010/012/013；证据类型2/3/4。
- npm audit命中GHSA-28wg-ghj8-5hjv和GHSA-2v37-7h3g-55p8。官方上游3.3.18发布及registry确认修复版本存在且兼容现有^3.3.12；不要求转5.x。
- 本地当前安装3.3.12，nanoid/non-secure.nanoid(-1)与customAlphabet('abc',0)()均超过1秒，子进程被安全终止；真实postcss调用nanoid(6)正常长度6。证明库缺陷，不是业务请求可控size的证明。未发现业务源码直接调用nanoid。
- 候选最小处理：在整批B2定稿后只更新锁中该包到已修补同主版及官方元数据，不在业务代码额外造参数闸。

### CX-02 — postcss锁8.5.15保留source map越界读取缺陷（中；构建依赖）
- 位置：package-lock.json:2049—2077，version第2050行；vite:3063等依赖^8.5.15。
- 分类：技术性；工程缺陷成立；GEN-004/010/013；证据类型2/3/4。
- 官方PostCSS公告GHSA-fxqj-rqcc-2cmp：<=8.5.22受影响、8.5.23修复。当前8.5.15代码loadFile只限.map扩展、不阻止路径越界。
- 实际postcss.parse合成CSS中的绝对sourceMappingURL，读入TemporaryDirectory的合成outside.map及AUDIT_SYNTHETIC_MARKER；没有读取真实秘密，临时目录自动删。Vite实际runPostCSS给from:source，意味着“缺from”这一特定新版残留条件不能直接套为当前Vite线上可利用性；旧8.5.15无目录约束仍为已验证库缺陷。
- 候选最小处理：仅锁同主版已修补8.5.23及官方依赖字段（其nanoid范围^3.3.16与CX-01的3.3.18兼容），不新增构建插件/业务能力。

### CX-03 — undici锁7.28.0含缓存头处理缺陷（中；测试依赖）
- 位置：package-lock.json:3043—3052，version第3044行；jsdom:1596的^7.25.0传递引入（精确行以完整下表及源段复核）。
- 分类：技术性；工程缺陷成立；GEN-004/009/010/013；证据类型2/3/4。
- 官方nodejs/undici公告GHSA-4cwx-7wf7-3272确认7.x <7.29.0受影响、7.29.0修复，npm另返回同版其他4条公告；完整audit结果保留。
- 直接调用当前7.28.0的parseCacheControlHeader，private空值解析为数组空串；混合private与private=hdr抛TypeError output[key].concat is not a function，真实结果如下。无服务器/用户数据/网络利用。
- 当前vitest用jsdom；jsdom默认userInterceptors=[]只加decompress，未证明其已启用共享cache或本项目可达这些攻击条件。不将上游跨用户泄漏条件当作已发生业务泄漏。
- 候选最小处理：锁同主版7.29.0及官方元数据；不修改jsdom或业务缓存设计。

除这三项外无新CX；技术性无缺陷项全部登记如下，不当作待修。没有添加项或HTML申请。所有发现待整批B2双席定稿，当前严禁修复。

## 每条技术性登记与全文件覆盖

1—6顶层name/version/lockfileVersion/requires/packages：技术性安装元数据，JSON与根清单验证，不产生业务规则。下表每条均类型4（JSON/npm虚拟树/registry原生输出），必要时结合类型2；“保留”只指没有本席已证缺陷，不替B2投票。

| 原始行 | 包/记录 | 用途与无业务发明理由 | 核验与处置 |
|---|---|---|---|
| 7—20 | (root) 0.1.0 | 技术性：根包与工作区安装声明；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 21—43 | apps/web 0.1.0 | 技术性：工作区UI构建/测试清单镜像；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 44—50 | apps/web/node_modules/@rolldown/pluginutils 1.0.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 51—76 | apps/web/node_modules/@vitejs/plugin-react 6.0.2 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 77—83 | node_modules/@adobe/css-tools 4.5.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 84—92 | node_modules/@ant-design/colors 7.2.1 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 93—111 | node_modules/@ant-design/cssinjs 1.24.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 112—126 | node_modules/@ant-design/cssinjs-utils 1.1.3 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 127—138 | node_modules/@ant-design/fast-color 2.0.6 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 139—158 | node_modules/@ant-design/icons 5.6.1 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 159—164 | node_modules/@ant-design/icons-svg 4.4.2 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 165—180 | node_modules/@ant-design/react-slick 1.1.2 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 181—197 | node_modules/@asamuzakjp/css-color 5.1.11 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 198—214 | node_modules/@asamuzakjp/dom-selector 7.1.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 215—224 | node_modules/@asamuzakjp/generational-cache 1.0.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 225—231 | node_modules/@asamuzakjp/nwsapi 2.3.9 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 232—247 | node_modules/@babel/code-frame 7.29.7 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 248—258 | node_modules/@babel/helper-validator-identifier 7.29.7 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 259—267 | node_modules/@babel/runtime 7.29.7 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 268—280 | node_modules/@bramus/specificity 2.4.2 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 281—300 | node_modules/@csstools/color-helpers 6.0.2 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 301—324 | node_modules/@csstools/css-calc 3.2.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 325—352 | node_modules/@csstools/css-color-parser 4.1.7 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 353—375 | node_modules/@csstools/css-parser-algorithms 4.0.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 376—400 | node_modules/@csstools/css-syntax-patches-for-csstree 1.1.5 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 401—420 | node_modules/@csstools/css-tokenizer 4.0.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 421—432 | node_modules/@emnapi/core 1.10.0 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 433—443 | node_modules/@emnapi/runtime 1.10.0 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 444—454 | node_modules/@emnapi/wasi-threads 1.2.1 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 455—460 | node_modules/@emotion/hash 0.8.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 461—466 | node_modules/@emotion/unitless 0.7.5 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 467—484 | node_modules/@exodus/bytes 1.15.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 485—491 | node_modules/@jridgewell/sourcemap-codec 1.5.5 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 492—510 | node_modules/@napi-rs/wasm-runtime 1.1.5 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 511—520 | node_modules/@oxc-project/types 0.133.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 521—536 | node_modules/@playwright/test 1.61.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 537—548 | node_modules/@rc-component/async-validator 5.1.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 549—564 | node_modules/@rc-component/color-picker 2.0.1 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 565—578 | node_modules/@rc-component/context 1.4.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 579—590 | node_modules/@rc-component/mini-decimal 1.1.3 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 591—608 | node_modules/@rc-component/mutate-observer 1.1.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 609—626 | node_modules/@rc-component/portal 1.1.2 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 627—642 | node_modules/@rc-component/qrcode 1.1.1 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 643—662 | node_modules/@rc-component/tour 1.15.1 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 663—683 | node_modules/@rc-component/trigger 2.3.1 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 684—700 | node_modules/@rolldown/binding-android-arm64 1.0.3 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 701—717 | node_modules/@rolldown/binding-darwin-arm64 1.0.3 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 718—734 | node_modules/@rolldown/binding-darwin-x64 1.0.3 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 735—751 | node_modules/@rolldown/binding-freebsd-x64 1.0.3 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 752—768 | node_modules/@rolldown/binding-linux-arm-gnueabihf 1.0.3 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 769—785 | node_modules/@rolldown/binding-linux-arm64-gnu 1.0.3 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 786—802 | node_modules/@rolldown/binding-linux-arm64-musl 1.0.3 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 803—819 | node_modules/@rolldown/binding-linux-ppc64-gnu 1.0.3 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 820—836 | node_modules/@rolldown/binding-linux-s390x-gnu 1.0.3 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 837—853 | node_modules/@rolldown/binding-linux-x64-gnu 1.0.3 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 854—870 | node_modules/@rolldown/binding-linux-x64-musl 1.0.3 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 871—887 | node_modules/@rolldown/binding-openharmony-arm64 1.0.3 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 888—906 | node_modules/@rolldown/binding-wasm32-wasi 1.0.3 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 907—923 | node_modules/@rolldown/binding-win32-arm64-msvc 1.0.3 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 924—940 | node_modules/@rolldown/binding-win32-x64-msvc 1.0.3 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 941—947 | node_modules/@standard-schema/spec 1.1.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 948—968 | node_modules/@testing-library/dom 10.4.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 969—988 | node_modules/@testing-library/jest-dom 6.9.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 989—995 | node_modules/@testing-library/jest-dom/node_modules/dom-accessibility-api 0.6.3 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 996—1023 | node_modules/@testing-library/react 16.3.2 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1024—1037 | node_modules/@testing-library/user-event 14.6.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1038—1048 | node_modules/@tybys/wasm-util 0.10.2 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1049—1056 | node_modules/@types/aria-query 5.0.4 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1057—1067 | node_modules/@types/chai 5.2.3 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1068—1074 | node_modules/@types/deep-eql 4.0.2 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1075—1081 | node_modules/@types/estree 1.0.9 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1082—1091 | node_modules/@types/react 19.2.16 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1092—1101 | node_modules/@types/react-dom 19.2.3 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1102—1119 | node_modules/@vitest/expect 4.1.9 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1120—1146 | node_modules/@vitest/mocker 4.1.9 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1147—1159 | node_modules/@vitest/pretty-format 4.1.9 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1160—1173 | node_modules/@vitest/runner 4.1.9 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1174—1189 | node_modules/@vitest/snapshot 4.1.9 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1190—1199 | node_modules/@vitest/spy 4.1.9 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1200—1214 | node_modules/@vitest/utils 4.1.9 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1215—1225 | node_modules/ansi-regex 5.0.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1226—1239 | node_modules/ansi-styles 5.2.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1240—1304 | node_modules/antd 5.29.3 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1305—1308 | node_modules/apps-web  | 技术性：工作区本地链接；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1309—1318 | node_modules/aria-query 5.3.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1319—1328 | node_modules/assertion-error 2.0.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1329—1338 | node_modules/bidi-js 1.0.3 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1339—1348 | node_modules/chai 6.2.2 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1349—1354 | node_modules/classnames 2.5.1 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1355—1360 | node_modules/compute-scroll-into-view 3.1.1 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1361—1367 | node_modules/convert-source-map 2.0.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1368—1376 | node_modules/copy-to-clipboard 3.3.3 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1377—1390 | node_modules/css-tree 3.2.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1391—1397 | node_modules/css.escape 1.5.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1398—1403 | node_modules/csstype 3.2.3 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1404—1417 | node_modules/data-urls 7.0.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1418—1423 | node_modules/dayjs 1.11.21 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1424—1430 | node_modules/decimal.js 10.6.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1431—1440 | node_modules/dequal 2.0.3 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1441—1450 | node_modules/detect-libc 2.1.2 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1451—1458 | node_modules/dom-accessibility-api 0.5.16 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1459—1471 | node_modules/entities 8.0.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1472—1478 | node_modules/es-module-lexer 2.1.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1479—1488 | node_modules/estree-walker 3.0.3 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1489—1498 | node_modules/expect-type 1.3.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1499—1516 | node_modules/fdir 6.5.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1517—1531 | node_modules/fsevents 2.3.3 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1532—1544 | node_modules/html-encoding-sniffer 6.0.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1545—1554 | node_modules/indent-string 4.0.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1555—1561 | node_modules/is-potential-custom-element-name 1.0.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1562—1569 | node_modules/js-tokens 4.0.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1570—1610 | node_modules/jsdom 29.1.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1611—1619 | node_modules/json2mq 0.2.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1620—1649 | node_modules/lightningcss 1.32.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1650—1670 | node_modules/lightningcss-android-arm64 1.32.0 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1671—1691 | node_modules/lightningcss-darwin-arm64 1.32.0 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1692—1712 | node_modules/lightningcss-darwin-x64 1.32.0 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1713—1733 | node_modules/lightningcss-freebsd-x64 1.32.0 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1734—1754 | node_modules/lightningcss-linux-arm-gnueabihf 1.32.0 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1755—1775 | node_modules/lightningcss-linux-arm64-gnu 1.32.0 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1776—1796 | node_modules/lightningcss-linux-arm64-musl 1.32.0 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1797—1817 | node_modules/lightningcss-linux-x64-gnu 1.32.0 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1818—1838 | node_modules/lightningcss-linux-x64-musl 1.32.0 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1839—1859 | node_modules/lightningcss-win32-arm64-msvc 1.32.0 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1860—1880 | node_modules/lightningcss-win32-x64-msvc 1.32.0 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1881—1890 | node_modules/lru-cache 11.5.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1891—1901 | node_modules/lz-string 1.5.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1902—1911 | node_modules/magic-string 0.30.21 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1912—1918 | node_modules/mdn-data 2.27.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1919—1928 | node_modules/min-indent 1.0.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1929—1947 | node_modules/nanoid 3.3.12 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | CX-01，待B2 |
| 1948—1961 | node_modules/obug 2.1.3 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1962—1974 | node_modules/parse5 8.0.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1975—1981 | node_modules/pathe 2.0.3 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1982—1988 | node_modules/picocolors 1.1.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 1989—2001 | node_modules/picomatch 4.0.4 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2002—2020 | node_modules/playwright 1.61.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2021—2033 | node_modules/playwright-core 1.61.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2034—2048 | node_modules/playwright/node_modules/fsevents 2.3.2 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2049—2077 | node_modules/postcss 8.5.15 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | CX-02，待B2 |
| 2078—2093 | node_modules/pretty-format 27.5.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2094—2101 | node_modules/pretty-format/node_modules/react-is 17.0.2 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2102—2111 | node_modules/punycode 2.3.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2112—2120 | node_modules/qrcode.react 4.2.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2121—2137 | node_modules/rc-cascader 3.34.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2138—2152 | node_modules/rc-checkbox 3.5.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2153—2168 | node_modules/rc-collapse 3.9.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2169—2185 | node_modules/rc-dialog 9.6.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2186—2202 | node_modules/rc-drawer 7.3.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2203—2218 | node_modules/rc-dropdown 4.2.1 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2219—2236 | node_modules/rc-field-form 2.7.1 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2237—2254 | node_modules/rc-image 7.12.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2255—2269 | node_modules/rc-input 1.8.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2270—2286 | node_modules/rc-input-number 9.5.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2287—2305 | node_modules/rc-mentions 2.20.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2306—2323 | node_modules/rc-menu 9.16.1 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2324—2338 | node_modules/rc-motion 2.9.5 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2339—2357 | node_modules/rc-notification 5.6.4 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2358—2373 | node_modules/rc-overflow 1.5.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2374—2388 | node_modules/rc-pagination 5.1.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2389—2427 | node_modules/rc-picker 4.11.3 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2428—2442 | node_modules/rc-progress 4.0.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2443—2460 | node_modules/rc-rate 2.13.1 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2461—2476 | node_modules/rc-resize-observer 1.4.3 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2477—2492 | node_modules/rc-segmented 2.7.1 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2493—2514 | node_modules/rc-select 14.16.8 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2515—2532 | node_modules/rc-slider 11.1.9 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2533—2550 | node_modules/rc-steps 6.0.1 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2551—2565 | node_modules/rc-switch 4.1.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2566—2586 | node_modules/rc-table 7.54.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2587—2608 | node_modules/rc-tabs 15.7.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2609—2625 | node_modules/rc-textarea 1.10.2 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2626—2641 | node_modules/rc-tooltip 6.4.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2642—2661 | node_modules/rc-tree 5.13.1 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2662—2678 | node_modules/rc-tree-select 5.27.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2679—2693 | node_modules/rc-upload 4.11.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2694—2707 | node_modules/rc-util 5.44.4 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2708—2726 | node_modules/rc-virtual-list 3.19.2 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2727—2735 | node_modules/react 19.2.7 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2736—2747 | node_modules/react-dom 19.2.7 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2748—2753 | node_modules/react-is 18.3.1 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2754—2767 | node_modules/redent 3.0.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2768—2777 | node_modules/require-from-string 2.0.2 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2778—2783 | node_modules/resize-observer-polyfill 1.5.1 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2784—2817 | node_modules/rolldown 1.0.3 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2818—2824 | node_modules/rolldown/node_modules/@rolldown/pluginutils 1.0.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2825—2837 | node_modules/saxes 6.0.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2838—2843 | node_modules/scheduler 0.27.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2844—2852 | node_modules/scroll-into-view-if-needed 3.1.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2853—2859 | node_modules/siginfo 2.0.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2860—2869 | node_modules/source-map-js 1.2.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2870—2876 | node_modules/stackback 0.0.2 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2877—2883 | node_modules/std-env 4.1.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2884—2889 | node_modules/string-convert 0.2.1 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2890—2902 | node_modules/strip-indent 3.0.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2903—2908 | node_modules/stylis 4.4.0 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2909—2915 | node_modules/symbol-tree 3.2.4 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2916—2924 | node_modules/throttle-debounce 5.0.2 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2925—2931 | node_modules/tinybench 2.9.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2932—2941 | node_modules/tinyexec 1.2.4 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2942—2958 | node_modules/tinyglobby 0.2.17 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2959—2968 | node_modules/tinyrainbow 3.1.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2969—2981 | node_modules/tldts 7.4.3 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2982—2988 | node_modules/tldts-core 7.4.3 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2989—2994 | node_modules/toggle-selection 1.0.6 | 技术性：现有UI/React工具链依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 2995—3007 | node_modules/tough-cookie 6.0.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 3008—3020 | node_modules/tr46 6.0.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 3021—3028 | node_modules/tslib 2.8.1 | 技术性：跨平台可选构建支撑；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 3029—3042 | node_modules/typescript 5.9.3 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 3043—3052 | node_modules/undici 7.28.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | CX-03，待B2 |
| 3053—3130 | node_modules/vite 8.0.16 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 3131—3220 | node_modules/vitest 4.1.9 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 3221—3233 | node_modules/w3c-xmlserializer 5.0.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 3234—3243 | node_modules/webidl-conversions 8.0.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 3244—3253 | node_modules/whatwg-mimetype 5.0.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 3254—3268 | node_modules/whatwg-url 16.0.1 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 3269—3285 | node_modules/why-is-node-running 2.3.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 3286—3295 | node_modules/xml-name-validator 5.0.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 3296—3302 | node_modules/xmlchars 2.2.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |
| 3303—3318 | node_modules/yaml 2.9.0 | 技术性：开发构建/测试传递依赖；只锁软件元数据，不定义收费/角色/流程 | 结构/图/来源核验通过；只登记保留 |

3319—3320：packages和根对象正常闭合；全部3320行结构范围无空洞。完整语义字段由本方逐记录读取，原始不透明SRI由全记录机械比对补足，不是只抽样某几个包。

## 完整安全查询真实输出

````text
COMMAND ["npm", "audit", "--package-lock-only", "--ignore-scripts", "--json"] EXIT 1
{"auditReportVersion": 2, "vulnerabilities": {"nanoid": {"name": "nanoid", "severity": "high", "isDirect": false, "via": [{"source": 1138811, "name": "nanoid", "dependency": "nanoid", "title": "nanoid: non-secure generators can loop indefinitely with negative size", "url": "https://github.com/advisories/GHSA-28wg-ghj8-5hjv", "severity": "high", "cwe": ["CWE-835"], "cvss": {"score": 5.9, "vectorString": "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:H"}, "range": "<3.3.16"}, {"source": 1139427, "name": "nanoid", "dependency": "nanoid", "title": "nanoid: custom generators can loop indefinitely when size is zero", "url": "https://github.com/advisories/GHSA-2v37-7h3g-55p8", "severity": "high", "cwe": ["CWE-835"], "cvss": {"score": 5.9, "vectorString": "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:H"}, "range": "<3.3.18"}], "effects": [], "range": "<=3.3.17", "nodes": ["node_modules/nanoid"], "fixAvailable": true}, "postcss": {"name": "postcss", "severity": "high", "isDirect": false, "via": [{"source": 1130709, "name": "postcss", "dependency": "postcss", "title": "PostCSS: incomplete fix of GHSA-6g55-p6wh-862q — attacker-controlled sourceMappingURL reads arbitrary .map files when `from` is unset", "url": "https://github.com/advisories/GHSA-fxqj-rqcc-2cmp", "severity": "moderate", "cwe": ["CWE-22", "CWE-200"], "cvss": {"score": 0, "vectorString": null}, "range": "<=8.5.22"}, {"source": 1139510, "name": "postcss", "dependency": "postcss", "title": "PostCSS: Path Traversal in Previous Source Map Auto-Loading (sourceMappingURL) leads to Arbitrary .map File Disclosure", "url": "https://github.com/advisories/GHSA-r28c-9q8g-f849", "severity": "high", "cwe": ["CWE-22"], "cvss": {"score": 7.5, "vectorString": "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N"}, "range": "<=8.5.17"}], "effects": [], "range": "<=8.5.22", "nodes": ["node_modules/postcss"], "fixAvailable": true}, "undici": {"name": "undici", "severity": "high", "isDirect": false, "via": [{"source": 1130715, "name": "undici", "dependency": "undici", "title": "undici vulnerable to downstream response desynchronization via retry interceptor", "url": "https://github.com/advisories/GHSA-8xcm-r25x-g524", "severity": "moderate", "cwe": ["CWE-444"], "cvss": {"score": 4.8, "vectorString": "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:L/I:L/A:N"}, "range": ">=7.0.0 <7.29.0"}, {"source": 1130718, "name": "undici", "dependency": "undici", "title": "undici vulnerable to cross-user information disclosure and parse-time crash via degenerate private cache directives", "url": "https://github.com/advisories/GHSA-4cwx-7wf7-3272", "severity": "high", "cwe": ["CWE-200", "CWE-248", "CWE-525"], "cvss": {"score": 7.4, "vectorString": "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:N/A:H"}, "range": ">=7.0.0 <7.29.0"}, {"source": 1130726, "name": "undici", "dependency": "undici", "title": "undici vulnerable to CRLF Injection via blob-like body 'type' property", "url": "https://github.com/advisories/GHSA-m8rv-5g2x-5cg5", "severity": "moderate", "cwe": ["CWE-93"], "cvss": {"score": 4.2, "vectorString": "CVSS:3.1/AV:N/AC:H/PR:N/UI:R/S:U/C:L/I:L/A:N"}, "range": ">=7.0.0 <7.29.0"}, {"source": 1130729, "name": "undici", "dependency": "undici", "title": "undici vulnerable to cross-user information disclosure via whitespace around equals in Cache-Control directives", "url": "https://github.com/advisories/GHSA-jr45-8vmc-qm54", "severity": "moderate", "cwe": ["CWE-436", "CWE-524"], "cvss": {"score": 5.9, "vectorString": "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:N/A:N"}, "range": ">=7.0.0 <7.29.0"}, {"source": 1130731, "name": "undici", "dependency": "undici", "title": "undici vulnerable to cookie attribute injection via unsanitized domain and unparsed setCookie fields", "url": "https://github.com/advisories/GHSA-v3r7-h72x-cjcm", "severity": "moderate", "cwe": ["CWE-74"], "cvss": {"score": 4.8, "vectorString": "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:L/I:L/A:N"}, "range": ">=7.0.0 <7.29.0"}], "effects": [], "range": "7.0.0 - 7.28.0", "nodes": ["node_modules/undici"], "fixAvailable": true}}, "metadata": {"vulnerabilities": {"info": 0, "low": 0, "moderate": 0, "high": 3, "critical": 0, "total": 3}, "dependencies": {"prod": 74, "dev": 146, "optional": 34, "peer": 11, "peerOptional": 0, "total": 219}}}
STDERR 
LOCK_UNCHANGED

````

## 依赖图/负例/生产扫描与修复版本元数据

````text
GRAPH_CHECK_EXIT 0 {"manifestsMatch":true,"inventory":219,"edges":582,"optionalMissing":0,"invalidEdges":[],"currentEngineBad":[],"negativeMissingReactDetected":50,"mode":"loadVirtual memory only; no reify or writes"}
 
CMD ["npm", "audit", "--omit=dev", "--package-lock-only", "--json"] EXIT 0 {
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
 
CMD ["npm", "view", "nanoid@3.3.18", "version", "engines", "dist.integrity", "--json"] EXIT 0 {
  "version": "3.3.18",
  "engines": {
    "node": "^10 || ^12 || ^13.7 || ^14 || >=15.0.1"
  },
  "dist.integrity": "sha512-DTg4MJbGMWkfi6VZFdNt2/caMbQy4Ou+Op/hJQvGEWcnVfoA1QA+xzRKAzw9jD6+GVOOeYr/mIcuDSdug6F6+w=="
}
 
CMD ["npm", "view", "postcss@8.5.23", "version", "engines", "dependencies", "dist.integrity", "--json"] EXIT 0 {
  "version": "8.5.23",
  "engines": {
    "node": "^10 || ^12 || >=14"
  },
  "dependencies": {
    "nanoid": "^3.3.16",
    "picocolors": "^1.1.1",
    "source-map-js": "^1.2.1"
  },
  "dist.integrity": "sha512-g50586zr4bZmwFiTlflMu8E0bDTb5I5gertgwAKmsdUlTQIhZtunzUlD1WSzwcVWPoAVpsrA6vlfCD7oXvRwgg=="
}
 
CMD ["npm", "view", "undici@7.29.0", "version", "engines", "dist.integrity", "--json"] EXIT 0 {
  "version": "7.29.0",
  "engines": {
    "node": ">=20.18.1"
  },
  "dist.integrity": "sha512-IDxfleLmmbSskfWSUATiN1nfn2rDuvnMOqb5CWR92iIfojA0Ud+ulOAAEQ57LPr9rWmsreUyf5lwyao+7GNNVw=="
}
 

````

## 库级反例真实输出

````text
INSTALLED nanoid 3.3.12
INSTALLED postcss 8.5.15
INSTALLED undici 7.28.0
CASE nanoid_negative TIMEOUT_1S_LIBRARY_INPUT_REPRO_ONLY
CASE nanoid_zero TIMEOUT_1S_LIBRARY_INPUT_REPRO_ONLY
CASE nanoid_actual_call EXIT 0 ACTUAL_CALL_LENGTH=6 
POSTCSS_CASE_EXIT 0 POSTCSS_LOADED_SYNTHETIC_ABSOLUTE_MAP
 
SYNTHETIC_TEMP_REMOVED; NO_REAL_SECRET_READ; NO_APPLICATION_EXPLOIT_CLAIM

````

## undici本地解析反例

````text
UNDICI_LOCAL_ONLY_EXIT 0 PRIVATE_EMPTY {"public":true,"max-age":300,"private":[""]}
MIXED_THROW TypeError output[key].concat is not a function
 

````

## 全registry源字段复核

````text
OFFICIAL_REGISTRY_ALL_RECORDS 216 DIFFERENCES []
217 tarball记录按name/version去重为216；每个记录的dist.tarball和dist.integrity与官方registry精确相等。
root Node v24.20.0; npm11.19.0; npm ls --all --package-lock-only --json exit0；npm ci --dry-run --ignore-scripts --no-audit --no-fund --loglevel=error exit0；LOCK_UNCHANGED。
````

## 上游信息与限制

已读取[PostCSS官方安全公告](https://github.com/postcss/postcss/security/advisories/GHSA-fxqj-rqcc-2cmp)、[undici官方安全公告](https://github.com/nodejs/undici/security/advisories/GHSA-4cwx-7wf7-3272)与nanoid上游修复发布/registry；nanoid仓库同名security/advisories路径404，故未以该404页面为证。GHSA数据库原文及官方发布互相核对，具体库行为另由实际调用验证。

初次读取脚本有多一个右括号，Python在运行前SyntaxError，纠正后上述源码全读成功，源无写入。一次Grok进程检查实际为启动后4分30秒，较约定5分钟提前30秒；只看到仍运行，未读报告/未判失败/未重跑；下一次按最后检查完成12:15:26 UTC起满5分钟再查，后续遵守5分钟间隔。不隐去此调度偏差。

未执行npm真实安装、全站build、全unit/E2E、数据库迁移或部署；本B1是完整锁文件审计，已实跑原生解析/安装预演/安全查询及范围限定的库级反例。生产omit-dev扫描0仅限当时已知数据库及生产依赖集合，不保证全仓无漏洞。源SHA本阶段保持，业务HTML、lock判据、findings和ledger未在本次报告生成过程中改动。

## 最终结论

本方B1 FAIL：CX-01—03三项已知依赖工程缺陷；全部220记录/3320行及18GEN已审完，无添加或HTML申请。全部发现一次性留本raw，待Grok完整raw交付后统一B2归并；不因首先发现问题中途修、不以本方判断推翻Grok。此时M004仍IN_PROGRESS、B3=0。


## B1同次报告精确引用校正
版本：V0.02

仍未读取Grok本次报告。对上文近似父依赖行重新rg核实：postcss的nanoid约束实际为package-lock.json:2070（非2069），jsdom的undici约束实际为1592（非1596），vite的postcss约束实际为3062（非3063）。三个包节点的原始范围与version行均保持正确；此处为当前精确引文，旧字节保留、不改发现内容或结论。

原生命令复核输出：
1592: "undici": "^7.25.0",
1942: "nanoid": "bin/nanoid.cjs",
2070: "nanoid": "^3.3.12",
3062: "postcss": "^8.5.15",

nanoid上游3.3.16发布说明明确修复负size无限循环；3.3.18发布说明为React Native async循环修复，因此不把3.3.18那一句当CJS零长漏洞的单独原文证据。零长CJS问题有本方当前3.3.12真实1秒超时、npm audit受影响/修复范围，以及上游历史修复引用；同主版3.3.18的可安装性与SRI已从registry实际核对。B3若实施后仍须对实际修后包验证原库级反例，不以版本字符串替代。

B1结论继续FAIL，CX-01—03均待两席整批归并，无源修改。原63804字节完整保留，末尾追加不是新一轮B1。


## B2 首批完整归并复核
版本：V0.03

结论：PASS（仅本席B2票；Grok本次B2尚未读、未判双PASS）。候选输入findings.md V0.43，155585字节，SHA256 cdeb13e34549eecdc985f0d3644f22877ea750ad0762480cd0048f0ba343019f。
原始输入：modules/M004-b1-self-raw.md V0.02前64989字节/SHA256 cde9d4c51416b2122e90051759c05c9be3a770d126102db64c28405e10be3f32；modules/M004-b1-grok-raw.md V0.01前49550字节/SHA256 f1d03742777444c383fc857265779a4bd92f52209251ba2f462fb264e3c14adb。已读完两席本次完整B1；Grok原报告显示截断的表格97—175行已单独补读，原始结论未改。此次B2追加无独占判据哈希行。

### 全部来源差集与分类

| 正式ID | 对应全部来源/归并意见 | 类别、缺陷、队列 |
|---|---|---|
| M004-001 | CX01全部负size与零size现象归同一旧nanoid节点；没有Grok对应GR，不冒写外审同意；候选保留本方证据及实际调用size6正常的限制。 | 技术性/是/是 |
| M004-002 | CX02全部map问题归旧PostCSS节点；实际合成map读取+上游公告成立，候选明确Vite设置from、不夸大项目可利用性。 | 技术性/是/是 |
| M004-003 | CX03缓存头反例与其余同版公告合并旧undici节点；默认jsdom无cache interceptor的反证保留，只有库缺陷判断。 | 技术性/是/是 |
| M004-004 | Grok T01—T04与本方顶层/根/web/link登记完全合并，纯安装格式与元数据，无缺陷不动。 | 技术性/否/否 |
| M004-005 | Grok T05—T07/T09—T12与本方全220记录图/平台/peer/engines/嵌套登记合并；438普通依赖边和582含peer边为不同口径；未把已登记安全问题再编号。 | 技术性/否/否 |
| M004-006 | Grok T08和本方全部源/SRI比对合并；217次真实tarball核验与216个去重registry元数据比对各自保留，完整性不等于没有漏洞。 | 技术性/否/否 |

六ID覆盖两席所有临时发现与逐条技术登记，未漏T01—T12或220条覆盖表，无丢弃/改写Grok原B1 PASS。本方B1 FAIL和GrokB1 PASS的差异在候选明列：Grok没查漏洞数据库，本方查了且构造出当前库行为反例。不是用本方覆盖Grok；只有本批Grok复核实际同意，B2才定稿。

四分类按冲突→细化→技术性→添加：这些是锁格式、现有工具软件版本、完整性与解析字段，没有HTML业务命题、收费/权限/状态/业务接口发明，也无已知HTML冲突。缺陷不因此转为业务添加；三项仅在已有范围内修补。其余技术项保留，不能因为缺少HTML行级锚点删除原生安装字段。没有需要操作员裁决的添加项或三HTML变更。

### 必要性、最小边界与证据限制

GEN010/013等已冻结通用安全/依赖正确性涵盖正常npm audit发现的现存工具链缺陷；本方实际库级反例和兼容修补版均已核到，不因三项都是dev就谎称没缺陷。生产omit-dev扫描0和真实应用暴露未证说明严重度与触达限制，候选中等工程缺陷处置合理，不能直接借公告high标签声称线上高危。

依赖修复限定package-lock.json三个已有节点version/resolved/integrity共9值及postcss→nanoid内部约束1值。父postcss/vite/jsdom范围均接受候选版本，应用清单无需改变。PostCSS官方8.5.23包声明nanoid ^3.3.16，故更新该字段属于002，同批锁3.3.18满足；其余217记录逐字保留。未批准全树升级、audit fix自动改父包或新增overrides。B3后才验证实际修后库，不能拿版本号或未运行测试当结果。

文件自身版本要求独立于npm应用semver与lockfileVersion协议值。候选顶层documentVersion: V0.01为用户要求的文件版本记录，归001同文件维护；不改应用semver、协议数值或安装行为。已读宿主Arborist shrinkwrap.js 520—526及1009—1011确认保留顶层扩展。进一步真实内存测试如下：
const t=await new Arborist({path:process.cwd()}).loadVirtual();
const baseline=structuredClone(t.meta.toJSON());
const before=edges(t);
t.meta.data.documentVersion='V0.01';
const out=structuredClone(t.meta.toJSON());
assert.equal(out.documentVersion,'V0.01'); delete out.documentVersion;
assert.deepEqual(out,baseline); assert.equal(edges(t),before);
实际输出：metadataPreserved=true, allOtherNormalizedFieldsEqual=true, edgesUnchanged=true, records=220, diskWrite=false；exit0；源SHA仍0e468712d5240cb669b847ecb4250c03e35e38a52aaf11e3013eb8b717dec60d。
edges签名遍历全部inventory节点edgesOut记录location/name/spec/target/valid，未写盘、未reify或save。

如实保留检验过程：第一次断言把loadVirtual后的临时packages数据JSON字符串与toJSON标准化结果比较，因临时name/_id属性清理和序变化失败；不是documentVersion兼容失败，也没有写源。改为两次相同toJSON标准化结构deepEqual后通过，不能把第一次说成成功。此内存反例验证配置字段必要性与无行为影响；B4仍须逐行审归因，外席可以否决，不得先落源。

### 本席复核结果

全部六ID、三是三否、GEN边界、原始分歧、无业务发明理由、最小修复和后续实际验证均匹配候选，故本席B2 PASS。没有新门禁/计数器/产物/判据/角色；M004仍IN_PROGRESS、B3=0、失败重跑0。Grok B2在同候选字节上运行，当前未读其追加、无源写入，须其完整报告交付并读完后方可作双席定稿。


## B2 修订候选 V0.44 完整复核
版本：V0.04

本席结论：PASS。仅本席本次B2票，不覆盖首批Grok FAIL，当前不得B3。此次Grok新报告尚未读。输入findings.md V0.44，157999字节/SHA256 209fa3cc53db35726159760acfbd8e22f4c911e7afd83f105b1a13dc3df31fd2；原始B1仍是本文件前64989字节与Grok原前49550字节；另完整读取首批B2修订依据：本方前70453字节/SHA f600e1b16b80084d6a72ec8f2cbaf57c8e5ee00c6c0f484885c3093c21d62e27，Grok前66531字节/SHA 888887e239ab1b3bfc75aa6158ed7755e9fdd48d41d7f7e696dfacde15568a7f（真实EXIT_CODE=0）。所有原始结论和字节保留。

### 完整六项重新核对

M004-001对应CX01，002对应CX02，003对应CX03。Grok首批B2实际重跑audit及原库级反例后接受三项技术性/工程缺陷是/队列是；没有回改B1 PASS。这三个完整差集在本候选正确保留。新候选仍只改三节点的version/resolved/integrity及PostCSS内部nanoid范围共10已有值，不升父包或业务功能。

004=T01—T04+本方格式/根/web/link，005=T05—T07/T09—T12+全220图/平台/peer/engines/嵌套，006=T08+217来源/SRI证据。三项均技术性/工程缺陷否/队列否，完整技术登记不漏。Grok438与本方582边口径不同已说明。全部220条登记继续引用两席完整B1表；不把有ID当成要修改，无重复分配，无添加项与HTML修改。

安全严重度仍仅开发工具链中等，生产omitdev为0；真实业务利用均未证。NanoID实际调用size6、Vite设置from、jsdom默认不启用cache均保留。候选没有把B1的已锁旧包改为“已修”。

### 唯一争议核对与修订依据

Grok首批FAIL理由：非npm标准字段documentVersion无安装用途，建议删并仅由审计文档承载Vx.xx；如操作员坚持则再问。前半事实“非标准字段/无安装用途”接受；后半处置与本轮操作员直接提供的AGENTS.md instructions全文要求不相容。本方不能把“任何对话中只要更新文件内容，该文件自身版本号末位必须前进一位”擅自改成仅文档。该约束已明示，不是临时发明的审计规则，不能用本方偏好的最少字段忽略它。

V0.44收紧字段定义：仅用户指定的文件自身版本元数据扩展，不声称npm识别的标准字段，不改应用semver/格式值，不新增产物或审计计数器。necessity来自用户明确要求；compatibility另由源码/真实运行证明，不把兼容当必要性。

npm11.19.0内存loadVirtual/toJSON标准化往返：扩展保留、除扩展外全部对象deepEqual、完整图边签名不变、220记录不变。首次非规范化JSON字符串比较失误仍保留于上一追加，不伪装全部首跑成功。
本次实际核项目声明的npm10.9.8官方tgz（registry URL https://registry.npmjs.org/npm/-/npm-10.9.8.tgz），全文件sha512与dist.integrity相等；不落盘，仅tar内存读取@npmcli/arborist/lib/shrinkwrap.js：
490 this.data = {
491 ...data,
492 lockfileVersion,
493 requires: true,
494 packages: data.packages || {},
495 dependencies: data.dependencies || {},
959 if (this.lockfileVersion >= 3) {
960 const { dependencies, ...data } = this.data
961 return data
这与宿主npm11保留未知顶层扩展一致。swKeyOrder用于排序，不能据其中缺名推导拒绝键的schema闭集。字段虽非标准，但不破坏原生读取/安装图。应用name/version或单独新文件方案修改更大或不符合“自身”，故当前一行元数据为最小实现；归001的该文件版本维护可逐行审，不新增业务。

本次只给自己观点，外审新票须实际完成才能合取。首批FAIL是事实，不改写、不以本席新PASS绕过。如果外审给与用户原文相容的更小处置，按证据继续B2处理。

### 同候选真实软件验证（未改主锁、不是B3）

Python urllib下载nanoid3.3.18/postcss8.5.23/undici7.29.0官方tgz，逐一sha512核验；在本主工作区.scratch下TemporaryDirectory解包，用node -e加载真实库。PostCSS的picocolors/source-map-js引用既有安装依赖，不改main node_modules。
实际输出：
FIXED_PACKAGE nanoid 3.3.18 SRI_PASS
FIXED_PACKAGE postcss 8.5.23 SRI_PASS
FIXED_PACKAGE undici 7.29.0 SRI_PASS
CASE nanoid_negative EXIT 0 RETURN ""
CASE nanoid_zero EXIT 0 EMPTY_RETURN_PASS
CASE nanoid_actual_call EXIT 0 LENGTH6_PASS
CASE undici_cache EXIT 0 PRIVATE_EMPTY {"public":true,"max-age":300,"private":true}; MIXED {"public":true,"max-age":60,"private":true}
CASE postcss_absolute_map EXIT 0 MAP null
TEMP_CONTEXT_REMOVED True
MAIN_LOCK_UNCHANGED True 0e468712d5240cb669b847ecb4250c03e35e38a52aaf11e3013eb8b717dec60d

复现调用为require('nanoid/non-secure').nanoid(-1)、require('nanoid').customAlphabet('abc',0)()、正常nanoid(6)；undici parseCacheControlHeader用'public, max-age=300, private=""'及'public, max-age=60, private, private="hdr"'；PostCSS使用只有合成AUDIT_SYNTHETIC_MARKER的绝对outside.map且不设from。没有真实秘密或线上请求。旧版输出在B1，本次候选库真实结果证明原反例改变，但不能声称主锁已修。所有临时文件自动删除。

额外对官方三修补版engines/bin/license/dependencies/peerDependencies/peerDependenciesMeta/optionalDependencies/os/cpu逐字段比对原锁：仅postcss dependencies.nanoid由^3.3.12为^3.3.16，其他差集为空。与10值候选边界相等。

### 本席结论

整批V0.44的六项来源、分类、三是三否、最小源边界、用户版本约束及补充证据均可核，故本席PASS；Grok新票待交付。M004仍IN_PROGRESS，B3=0，重跑0，判据未改；当前没有人闸问题。修订候选B2不是运行失败重试，不增加失败重跑计数。


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M004 B1 — 当前471计划 Codex独立完整报告
版本：V0.05

本席步骤结论：PASS。当前模块生命周期仍IN_PROGRESS；B3历史累计1、方案修复2、Grok交付失败0。不能将本席PASS或Grok交付exit0写成模块PASS。
本轮先Grok实际结束（exit0），本席才独立审计；写本段时未读当前Grok报告正文、findings或旧M004审计结论。旧self前76213字节原样保留，旧322同号PASS不继承。

## 1. 当前输入与完整覆盖

正本/home/james/complex-management，main。M004仅package-lock.json；计划标记OVERSIZE-SINGLE-FILE，3321行/114258字节，自身documentVersion V0.02，SHA256 d3676e896b885b6bbb979440bb51b9dfe60728735cee000e528ab5d1791bcfef。
当前冻结锁V0.11；M004映射GEN-001—GEN-018共18项，无HTML派生ID。已读general完整18条、当前映射、runbook V0.02与本模块范围。实际读取root package.json、apps/web/package.json及两个直接消费方.github/workflows/predeploy-gates.yml和tools/verify-no-legacy-api-prototype.mjs；这些只用于验证锁，不宣布其模块审完。
结构阅读覆盖：L1—7全部顶层键；L8—3319全部220个包条目（逐条范围见表）；L3320—3321闭合括号。全部字段集合21种，无未知业务字段。217个完整integrity原值由Python逐值严格解码并校验64字节，再由npm ssri严格解析；展示阅读时省去重复摘要字串，未假称下载远端tarball验证。其余元数据与所有依赖/peer/optional关系按220条全部读取，不用抽样替代。
root manifest SHA256 350999a90ddf85b2d3dcbee090dd593fb7faff18d0f467f0a608d8b3664d473f；workspace manifest SHA256 b8017d9782b4c00eac1c992204dad2b009e23db7c4c804737bdf8fcbc90b8f1f。root与workspace的name/version/dependencies/devDependencies/optionalDependencies/engines/workspaces原生deepEqual全部成立。
L2项目名、L3应用版本0.1.0、L4 npm锁协议3、L5用户文件版本V0.02、L6 requires及L7容器均为技术性元数据。documentVersion不是应用版本或lockfileVersion；本次文件未改，不补版本、不改协议。

## 2. 四分类与逐条登记

依序检查冲突→细化→技术性→添加。此文件只锁定既有工程依赖树，没有业务实体、状态、枚举、权限、金额规则或业务默认值；UI/二维码/颜色等依赖包名仅为库标识，不等于新业务概念。没有明文HTML冲突，没有需要HTML上位解释的新增行为；按提示词明确的“依赖声明”技术性通道登记。三HTML哈希已核对保持，没有把其静默当删包理由或发现新HTML问题。
每个包条目与顶层键均归技术性、无工程缺陷、建议修复队列否。软件库的“颜色/金额小数/日期/二维码”能力不自动产生项目定价、支付或门禁规则；实际调用中的业务参数由后续所属模块按HTML审计。
“npm下载源”均为registry.npmjs.org的HTTPS tarball路径，217/217没有用户信息、query或fragment。这里只证明本文件值的来源形态和摘要语法，不保证外部registry可达或供应商产物全部安全。

| 源行 | 条目 | 锁定版本 | 分类 | 无业务发明及用途 |
|---|---|---|---|---|
| 1—7 | 顶层元数据及packages容器 | lock协议3 / 文档V0.02 | 技术性 | 构建安装元数据；不引入业务规则 |
| 8—21 | (root) | 0.1.0 | 技术性 | 根清单镜像及工作区选择；不引入业务规则 |
| 22—44 | apps/web | 0.1.0 | 技术性 | Web清单镜像；不引入业务规则 |
| 45—51 | apps/web/node_modules/@rolldown/pluginutils | 1.0.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 52—77 | apps/web/node_modules/@vitejs/plugin-react | 6.0.2 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 78—84 | node_modules/@adobe/css-tools | 4.5.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 85—93 | node_modules/@ant-design/colors | 7.2.1 | 技术性 | UI运行依赖；不引入业务规则 |
| 94—112 | node_modules/@ant-design/cssinjs | 1.24.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 113—127 | node_modules/@ant-design/cssinjs-utils | 1.1.3 | 技术性 | UI运行依赖；不引入业务规则 |
| 128—139 | node_modules/@ant-design/fast-color | 2.0.6 | 技术性 | UI运行依赖；不引入业务规则 |
| 140—159 | node_modules/@ant-design/icons | 5.6.1 | 技术性 | UI运行依赖；不引入业务规则 |
| 160—165 | node_modules/@ant-design/icons-svg | 4.4.2 | 技术性 | UI运行依赖；不引入业务规则 |
| 166—181 | node_modules/@ant-design/react-slick | 1.1.2 | 技术性 | UI运行依赖；不引入业务规则 |
| 182—198 | node_modules/@asamuzakjp/css-color | 5.1.11 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 199—215 | node_modules/@asamuzakjp/dom-selector | 7.1.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 216—225 | node_modules/@asamuzakjp/generational-cache | 1.0.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 226—232 | node_modules/@asamuzakjp/nwsapi | 2.3.9 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 233—248 | node_modules/@babel/code-frame | 7.29.7 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 249—259 | node_modules/@babel/helper-validator-identifier | 7.29.7 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 260—268 | node_modules/@babel/runtime | 7.29.7 | 技术性 | UI运行依赖；不引入业务规则 |
| 269—281 | node_modules/@bramus/specificity | 2.4.2 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 282—301 | node_modules/@csstools/color-helpers | 6.0.2 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 302—325 | node_modules/@csstools/css-calc | 3.2.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 326—353 | node_modules/@csstools/css-color-parser | 4.1.7 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 354—376 | node_modules/@csstools/css-parser-algorithms | 4.0.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 377—401 | node_modules/@csstools/css-syntax-patches-for-csstree | 1.1.5 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 402—421 | node_modules/@csstools/css-tokenizer | 4.0.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 422—433 | node_modules/@emnapi/core | 1.10.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 434—444 | node_modules/@emnapi/runtime | 1.10.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 445—455 | node_modules/@emnapi/wasi-threads | 1.2.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 456—461 | node_modules/@emotion/hash | 0.8.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 462—467 | node_modules/@emotion/unitless | 0.7.5 | 技术性 | UI运行依赖；不引入业务规则 |
| 468—485 | node_modules/@exodus/bytes | 1.15.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 486—492 | node_modules/@jridgewell/sourcemap-codec | 1.5.5 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 493—511 | node_modules/@napi-rs/wasm-runtime | 1.1.5 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 512—521 | node_modules/@oxc-project/types | 0.133.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 522—537 | node_modules/@playwright/test | 1.61.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 538—549 | node_modules/@rc-component/async-validator | 5.1.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 550—565 | node_modules/@rc-component/color-picker | 2.0.1 | 技术性 | UI运行依赖；不引入业务规则 |
| 566—579 | node_modules/@rc-component/context | 1.4.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 580—591 | node_modules/@rc-component/mini-decimal | 1.1.3 | 技术性 | UI运行依赖；不引入业务规则 |
| 592—609 | node_modules/@rc-component/mutate-observer | 1.1.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 610—627 | node_modules/@rc-component/portal | 1.1.2 | 技术性 | UI运行依赖；不引入业务规则 |
| 628—643 | node_modules/@rc-component/qrcode | 1.1.1 | 技术性 | UI运行依赖；不引入业务规则 |
| 644—663 | node_modules/@rc-component/tour | 1.15.1 | 技术性 | UI运行依赖；不引入业务规则 |
| 664—684 | node_modules/@rc-component/trigger | 2.3.1 | 技术性 | UI运行依赖；不引入业务规则 |
| 685—701 | node_modules/@rolldown/binding-android-arm64 | 1.0.3 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 702—718 | node_modules/@rolldown/binding-darwin-arm64 | 1.0.3 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 719—735 | node_modules/@rolldown/binding-darwin-x64 | 1.0.3 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 736—752 | node_modules/@rolldown/binding-freebsd-x64 | 1.0.3 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 753—769 | node_modules/@rolldown/binding-linux-arm-gnueabihf | 1.0.3 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 770—786 | node_modules/@rolldown/binding-linux-arm64-gnu | 1.0.3 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 787—803 | node_modules/@rolldown/binding-linux-arm64-musl | 1.0.3 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 804—820 | node_modules/@rolldown/binding-linux-ppc64-gnu | 1.0.3 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 821—837 | node_modules/@rolldown/binding-linux-s390x-gnu | 1.0.3 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 838—854 | node_modules/@rolldown/binding-linux-x64-gnu | 1.0.3 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 855—871 | node_modules/@rolldown/binding-linux-x64-musl | 1.0.3 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 872—888 | node_modules/@rolldown/binding-openharmony-arm64 | 1.0.3 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 889—907 | node_modules/@rolldown/binding-wasm32-wasi | 1.0.3 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 908—924 | node_modules/@rolldown/binding-win32-arm64-msvc | 1.0.3 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 925—941 | node_modules/@rolldown/binding-win32-x64-msvc | 1.0.3 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 942—948 | node_modules/@standard-schema/spec | 1.1.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 949—969 | node_modules/@testing-library/dom | 10.4.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 970—989 | node_modules/@testing-library/jest-dom | 6.9.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 990—996 | node_modules/@testing-library/jest-dom/node_modules/dom-accessibility-api | 0.6.3 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 997—1024 | node_modules/@testing-library/react | 16.3.2 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1025—1038 | node_modules/@testing-library/user-event | 14.6.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1039—1049 | node_modules/@tybys/wasm-util | 0.10.2 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1050—1057 | node_modules/@types/aria-query | 5.0.4 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1058—1068 | node_modules/@types/chai | 5.2.3 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1069—1075 | node_modules/@types/deep-eql | 4.0.2 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1076—1082 | node_modules/@types/estree | 1.0.9 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1083—1092 | node_modules/@types/react | 19.2.16 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1093—1102 | node_modules/@types/react-dom | 19.2.3 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1103—1120 | node_modules/@vitest/expect | 4.1.9 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1121—1147 | node_modules/@vitest/mocker | 4.1.9 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1148—1160 | node_modules/@vitest/pretty-format | 4.1.9 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1161—1174 | node_modules/@vitest/runner | 4.1.9 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1175—1190 | node_modules/@vitest/snapshot | 4.1.9 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1191—1200 | node_modules/@vitest/spy | 4.1.9 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1201—1215 | node_modules/@vitest/utils | 4.1.9 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1216—1226 | node_modules/ansi-regex | 5.0.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1227—1240 | node_modules/ansi-styles | 5.2.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1241—1305 | node_modules/antd | 5.29.3 | 技术性 | UI运行依赖；不引入业务规则 |
| 1306—1309 | node_modules/apps-web | 链接 | 技术性 | 工作区符号链接；不引入业务规则 |
| 1310—1319 | node_modules/aria-query | 5.3.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1320—1329 | node_modules/assertion-error | 2.0.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1330—1339 | node_modules/bidi-js | 1.0.3 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1340—1349 | node_modules/chai | 6.2.2 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1350—1355 | node_modules/classnames | 2.5.1 | 技术性 | UI运行依赖；不引入业务规则 |
| 1356—1361 | node_modules/compute-scroll-into-view | 3.1.1 | 技术性 | UI运行依赖；不引入业务规则 |
| 1362—1368 | node_modules/convert-source-map | 2.0.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1369—1377 | node_modules/copy-to-clipboard | 3.3.3 | 技术性 | UI运行依赖；不引入业务规则 |
| 1378—1391 | node_modules/css-tree | 3.2.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1392—1398 | node_modules/css.escape | 1.5.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1399—1404 | node_modules/csstype | 3.2.3 | 技术性 | UI运行依赖；不引入业务规则 |
| 1405—1418 | node_modules/data-urls | 7.0.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1419—1424 | node_modules/dayjs | 1.11.21 | 技术性 | UI运行依赖；不引入业务规则 |
| 1425—1431 | node_modules/decimal.js | 10.6.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1432—1441 | node_modules/dequal | 2.0.3 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1442—1451 | node_modules/detect-libc | 2.1.2 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1452—1459 | node_modules/dom-accessibility-api | 0.5.16 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1460—1472 | node_modules/entities | 8.0.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1473—1479 | node_modules/es-module-lexer | 2.1.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1480—1489 | node_modules/estree-walker | 3.0.3 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1490—1499 | node_modules/expect-type | 1.3.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1500—1517 | node_modules/fdir | 6.5.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1518—1532 | node_modules/fsevents | 2.3.3 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 1533—1545 | node_modules/html-encoding-sniffer | 6.0.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1546—1555 | node_modules/indent-string | 4.0.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1556—1562 | node_modules/is-potential-custom-element-name | 1.0.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1563—1570 | node_modules/js-tokens | 4.0.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1571—1611 | node_modules/jsdom | 29.1.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1612—1620 | node_modules/json2mq | 0.2.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 1621—1650 | node_modules/lightningcss | 1.32.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1651—1671 | node_modules/lightningcss-android-arm64 | 1.32.0 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 1672—1692 | node_modules/lightningcss-darwin-arm64 | 1.32.0 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 1693—1713 | node_modules/lightningcss-darwin-x64 | 1.32.0 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 1714—1734 | node_modules/lightningcss-freebsd-x64 | 1.32.0 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 1735—1755 | node_modules/lightningcss-linux-arm-gnueabihf | 1.32.0 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 1756—1776 | node_modules/lightningcss-linux-arm64-gnu | 1.32.0 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 1777—1797 | node_modules/lightningcss-linux-arm64-musl | 1.32.0 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 1798—1818 | node_modules/lightningcss-linux-x64-gnu | 1.32.0 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 1819—1839 | node_modules/lightningcss-linux-x64-musl | 1.32.0 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 1840—1860 | node_modules/lightningcss-win32-arm64-msvc | 1.32.0 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 1861—1881 | node_modules/lightningcss-win32-x64-msvc | 1.32.0 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 1882—1891 | node_modules/lru-cache | 11.5.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1892—1902 | node_modules/lz-string | 1.5.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1903—1912 | node_modules/magic-string | 0.30.21 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1913—1919 | node_modules/mdn-data | 2.27.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1920—1929 | node_modules/min-indent | 1.0.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1930—1948 | node_modules/nanoid | 3.3.18 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1949—1962 | node_modules/obug | 2.1.3 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1963—1975 | node_modules/parse5 | 8.0.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1976—1982 | node_modules/pathe | 2.0.3 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1983—1989 | node_modules/picocolors | 1.1.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 1990—2002 | node_modules/picomatch | 4.0.4 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2003—2021 | node_modules/playwright | 1.61.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2022—2034 | node_modules/playwright-core | 1.61.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2035—2049 | node_modules/playwright/node_modules/fsevents | 2.3.2 | 技术性 | 跨平台可选依赖；不引入业务规则 |
| 2050—2078 | node_modules/postcss | 8.5.23 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2079—2094 | node_modules/pretty-format | 27.5.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2095—2102 | node_modules/pretty-format/node_modules/react-is | 17.0.2 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2103—2112 | node_modules/punycode | 2.3.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2113—2121 | node_modules/qrcode.react | 4.2.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2122—2138 | node_modules/rc-cascader | 3.34.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2139—2153 | node_modules/rc-checkbox | 3.5.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2154—2169 | node_modules/rc-collapse | 3.9.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2170—2186 | node_modules/rc-dialog | 9.6.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2187—2203 | node_modules/rc-drawer | 7.3.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2204—2219 | node_modules/rc-dropdown | 4.2.1 | 技术性 | UI运行依赖；不引入业务规则 |
| 2220—2237 | node_modules/rc-field-form | 2.7.1 | 技术性 | UI运行依赖；不引入业务规则 |
| 2238—2255 | node_modules/rc-image | 7.12.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2256—2270 | node_modules/rc-input | 1.8.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2271—2287 | node_modules/rc-input-number | 9.5.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2288—2306 | node_modules/rc-mentions | 2.20.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2307—2324 | node_modules/rc-menu | 9.16.1 | 技术性 | UI运行依赖；不引入业务规则 |
| 2325—2339 | node_modules/rc-motion | 2.9.5 | 技术性 | UI运行依赖；不引入业务规则 |
| 2340—2358 | node_modules/rc-notification | 5.6.4 | 技术性 | UI运行依赖；不引入业务规则 |
| 2359—2374 | node_modules/rc-overflow | 1.5.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2375—2389 | node_modules/rc-pagination | 5.1.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2390—2428 | node_modules/rc-picker | 4.11.3 | 技术性 | UI运行依赖；不引入业务规则 |
| 2429—2443 | node_modules/rc-progress | 4.0.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2444—2461 | node_modules/rc-rate | 2.13.1 | 技术性 | UI运行依赖；不引入业务规则 |
| 2462—2477 | node_modules/rc-resize-observer | 1.4.3 | 技术性 | UI运行依赖；不引入业务规则 |
| 2478—2493 | node_modules/rc-segmented | 2.7.1 | 技术性 | UI运行依赖；不引入业务规则 |
| 2494—2515 | node_modules/rc-select | 14.16.8 | 技术性 | UI运行依赖；不引入业务规则 |
| 2516—2533 | node_modules/rc-slider | 11.1.9 | 技术性 | UI运行依赖；不引入业务规则 |
| 2534—2551 | node_modules/rc-steps | 6.0.1 | 技术性 | UI运行依赖；不引入业务规则 |
| 2552—2566 | node_modules/rc-switch | 4.1.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2567—2587 | node_modules/rc-table | 7.54.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2588—2609 | node_modules/rc-tabs | 15.7.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2610—2626 | node_modules/rc-textarea | 1.10.2 | 技术性 | UI运行依赖；不引入业务规则 |
| 2627—2642 | node_modules/rc-tooltip | 6.4.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2643—2662 | node_modules/rc-tree | 5.13.1 | 技术性 | UI运行依赖；不引入业务规则 |
| 2663—2679 | node_modules/rc-tree-select | 5.27.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2680—2694 | node_modules/rc-upload | 4.11.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2695—2708 | node_modules/rc-util | 5.44.4 | 技术性 | UI运行依赖；不引入业务规则 |
| 2709—2727 | node_modules/rc-virtual-list | 3.19.2 | 技术性 | UI运行依赖；不引入业务规则 |
| 2728—2736 | node_modules/react | 19.2.7 | 技术性 | UI运行依赖；不引入业务规则 |
| 2737—2748 | node_modules/react-dom | 19.2.7 | 技术性 | UI运行依赖；不引入业务规则 |
| 2749—2754 | node_modules/react-is | 18.3.1 | 技术性 | UI运行依赖；不引入业务规则 |
| 2755—2768 | node_modules/redent | 3.0.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2769—2778 | node_modules/require-from-string | 2.0.2 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2779—2784 | node_modules/resize-observer-polyfill | 1.5.1 | 技术性 | UI运行依赖；不引入业务规则 |
| 2785—2818 | node_modules/rolldown | 1.0.3 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2819—2825 | node_modules/rolldown/node_modules/@rolldown/pluginutils | 1.0.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2826—2838 | node_modules/saxes | 6.0.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2839—2844 | node_modules/scheduler | 0.27.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2845—2853 | node_modules/scroll-into-view-if-needed | 3.1.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2854—2860 | node_modules/siginfo | 2.0.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2861—2870 | node_modules/source-map-js | 1.2.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2871—2877 | node_modules/stackback | 0.0.2 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2878—2884 | node_modules/std-env | 4.1.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2885—2890 | node_modules/string-convert | 0.2.1 | 技术性 | UI运行依赖；不引入业务规则 |
| 2891—2903 | node_modules/strip-indent | 3.0.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2904—2909 | node_modules/stylis | 4.4.0 | 技术性 | UI运行依赖；不引入业务规则 |
| 2910—2916 | node_modules/symbol-tree | 3.2.4 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2917—2925 | node_modules/throttle-debounce | 5.0.2 | 技术性 | UI运行依赖；不引入业务规则 |
| 2926—2932 | node_modules/tinybench | 2.9.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2933—2942 | node_modules/tinyexec | 1.2.4 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2943—2959 | node_modules/tinyglobby | 0.2.17 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2960—2969 | node_modules/tinyrainbow | 3.1.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2970—2982 | node_modules/tldts | 7.4.3 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2983—2989 | node_modules/tldts-core | 7.4.3 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 2990—2995 | node_modules/toggle-selection | 1.0.6 | 技术性 | UI运行依赖；不引入业务规则 |
| 2996—3008 | node_modules/tough-cookie | 6.0.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 3009—3021 | node_modules/tr46 | 6.0.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 3022—3029 | node_modules/tslib | 2.8.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 3030—3043 | node_modules/typescript | 5.9.3 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 3044—3053 | node_modules/undici | 7.29.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 3054—3131 | node_modules/vite | 8.0.16 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 3132—3221 | node_modules/vitest | 4.1.9 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 3222—3234 | node_modules/w3c-xmlserializer | 5.0.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 3235—3244 | node_modules/webidl-conversions | 8.0.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 3245—3254 | node_modules/whatwg-mimetype | 5.0.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 3255—3269 | node_modules/whatwg-url | 16.0.1 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 3270—3286 | node_modules/why-is-node-running | 2.3.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 3287—3296 | node_modules/xml-name-validator | 5.0.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 3297—3303 | node_modules/xmlchars | 2.2.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |
| 3304—3319 | node_modules/yaml | 2.9.0 | 技术性 | 开发/构建/测试间接或直接依赖；不引入业务规则 |

| 3320—3321 | JSON闭合结构 | — | 技术性 | 序列化结构；不引入业务规则 |

## 3. 第一性原理线

1. 为什么有此文件：CI真实入口.github/workflows/predeploy-gates.yml:23执行npm ci --ignore-scripts。npm本机ci.js实际loadVirtual→buildIdealTree→validateLockfile，缺失或不匹配时报错，因此锁是可复现安装的直接输入。全部220条原生inventory可从root/link/workspace边遍历到，extraneous和unreachable均空；没有仅凭注释保留的孤立条目。
2. 是否存在可删除重复：两处@rolldown/pluginutils 1.0.1是现有树的不同解析位置，两个fsevents为2.3.3与2.3.2不同声明；react-is 18.3.1和pretty-format内部17.0.2分别满足不同消费者。可选平台包用于跨平台树，不因当前Linux用不到就删除。正常ci能重建；未发现必须手删/收窄的缺陷。进一步去重是安装工具重排优化，不是本轮正常首跑故障。
3. 未验证前提：已验证本机Node v24.20.0/npm11.19.0实际解析、当前所有116个node engine及root允许范围均兼容；并没有实际运行packageManager标注的npm10.9.8，也没在所有平台安装。原生协议3与规范支持范围可核，但未将规范替代实际全平台安装成功。
4. 安装是否真的执行：仅npm ci --dry-run --ignore-scripts --offline --no-audit --no-fund --json；exit0，计划add188/remove186/change0/audited0。没有安装、没有运行安装脚本或npm audit；root/workspace清单与lock字节及两个node_modules目录mtime前后相等。npm源代码确认dryRun绕过删除分支，save:false不保存清单。
5. 声称与实际是否一致：现有node_modules含自指node_modules符号链接，且既有undici/postcss/nanoid等版本与锁不同；预演所列remove/add反映当前安装上下文。该目录排除，不审其业务、不修改，不把旧安装运行结果冒称为当前锁完整安装验证。锁内图本身可达且一致，ci正确提出重建，不构成当前锁缺陷。

## 4. 顺从式18项冻结判据

| ID | 本模块核查与证据 | 本席结论 |
|---|---|---|
| GEN-001 | 220条全部可达，无extraneous；重复版本/平台分支按真实依赖用途核实，不人为删锁节点 | PASS |
| GEN-002 | JSON严格全解析；npm Arborist真实loadVirtual和ci预演成功；原生validator缺包/错版本负例准确报错 | PASS |
| GEN-003 | 配置无新执行逻辑或测试文件；217错误payload被ssri拒绝，缺失react/错版本及engine下界反例都有assert，非只grep | PASS |
| GEN-004 | JSON无重复key；源URL无凭据、仅HTTPS官方registry；217 SRI格式和原生校验行为；范围引用交给原生解析器全检 | PASS |
| GEN-005 | 本文件没有主体、对象或服务端授权实现；依赖声明未引入新权限规则。不能据此宣布服务端授权已审 | PASS（本文件无该行为） |
| GEN-006 | 根/workspace镜像与582条依赖边一致；无坏引用。无数据库事务/金额状态写入 | PASS |
| GEN-007 | 静态锁文件无请求重试或并发写逻辑；禁止把库名等同业务幂等实现，未增锁或门禁 | PASS（本文件无该行为） |
| GEN-008 | npm锁协议3、SemVer、documentVersion分工明确；root/workspace清单字段逐个deepEqual；单一workspace链接正确 | PASS |
| GEN-009 | npm真实validator缺包/错版返回具体错误；ci dry-run正确无改动，未把退出0当下载/构建完成 | PASS |
| GEN-010 | 没有明文秘密或含凭据URL；包路径/HTTPS/SRI检查全覆盖。没有声称做远端tarball/全依赖漏洞审计 | PASS |
| GEN-011 | 命令、真实exit、hash、完整220行表及运行边界均记录；未沿用旧PASS | PASS |
| GEN-012 | 静态有限220条/582边，遍历用seen终止；本文件无业务资源循环或外部操作，不新增并发资源规则 | PASS |
| GEN-013 | 实际Arborist220条、582边均有效；root范围涵盖116个engine；28受平台限制包均optional；29个缺失peerOptional被原生机制允许；ci预演通过 | PASS |
| GEN-014 | 无数据库或迁移内容，未运行数据库操作 | PASS（本文件无该行为） |
| GEN-015 | 仅UI依赖元数据，无界面控件/文案/焦点行为；不据依赖存在推导产品可访问性通过 | PASS（本文件无该行为） |
| GEN-016 | 当前行数/版本/hash/CI调用可复核，明确未全安装构建；未修改文件，不触发其版本递增 | PASS |
| GEN-017 | 顶层与220条全部顺序分类为技术性，理由逐条登记；没有新增业务规则或HTML改动 | PASS |
| GEN-018 | 结论依据本机npm实现、规范原文、完整当前配置、可复现命令与负例；旧报告与席位意见不是依据 | PASS |

## 5. 对抗式线

| 质疑/反例 | 实际核查 | 判断 |
|---|---|---|
| JSON隐藏同名键让不同解析器取不同值 | Python object_pairs_hook对全部层级逐键断言唯一，220条定位与packages顺序全等 | 未发现 |
| root或workspace声明漂移 | name/version/依赖三类/engines/workspaces与两份实际manifest逐字段deepEqual | 未发现 |
| 缺包会被吞掉 | 对当前原生inventory的内存副本删react，调用npm原生validate-lockfile | 返回Missing: react@19.2.7 from lock file |
| 错版被当成满足声明 | 内存副本react版本置0.0.0，同一个原生validator | 返回Invalid: lock file's react@0.0.0 does not satisfy react@19.2.7 |
| peer缺失全部算坏边 | 实际29条peerOptional没有目标；原生valid允许，真正invalid为空 | 不误判为必须新增依赖 |
| 同名嵌套包是无用重复 | 220条全部从root可达；不同解析位置/版本服务实际边 | 不直接手删 |
| 非Linux包导致本机安装拒绝 | 28个有os/cpu约束条目均optional，原生ci离线预演0 | 当前Linux预演成立，不宣称跨平台实装 |
| root声明22.13却依赖要求更高 | semver.subset逐116条成立；22.12不满足root、22.13满足 | 下界真实验证 |
| SRI只是看起来像摘要 | ssri strict解析217/217；同一错误payload对217条全部返回false | 拒绝错内容行为成立；真实tarball未验证 |
| lock文档版本覆盖npm协议/应用版本 | 分别documentVersion V0.02 / lockfileVersion3 / version0.1.0；原生loadVirtual/ci读取成功 | 未发现冲突 |
| npm ci预演偷偷删依赖 | 先读实际ci.js的!dryRun分支，命令带dry-run/ignore-scripts，检查manifest及目录mtime未变 | 无实际安装删除证据 |
| 当前node_modules可运行证明锁可安装 | 安装上下文存在旧版本与自指链接；明确不作此推论 | 限制已登记 |
| 全局HTML门禁红是锁问题 | 锁消费方原型遗留检查0；先前html-ledger-sync/entry-anchors红灯有独立BSM镜像原因 | 不扩模块、不修CSV/HTML、不报全局绿 |

## 6. 实际命令、输出与可复现性

所有远程命令均经直接SSH，在/home/james/complex-management执行；无MCP_Cloud_160，无第三席。启动时8个已读适用SKILL.md路径存在性再次确认。using-superpowers用于节点核验；grilling用于上述反例；codebase-design核对最小依赖图；systematic-debugging/diagnosing-bugs追ci原生消费路径；ponytail-review核对重复可删除性；verification-before-completion按实际命令及hash；writing-for-agents用于本报告可追溯结构。沿用本会话此前实际全文读取的技能，不伪称本模块重新读取了全部技能正文。纯配置未改，不新增TDD文件。

原生图/负例实际完整脚本（node -，exit0）：
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
本次最终原生输出和ci预演摘要：
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
另实际执行node tools/verify-no-legacy-api-prototype.mjs → exit0 / NO_LEGACY_API_PROTOTYPE_PASS。
git status --porcelain -- package-lock.json → exit0/空；git diff --check -- package-lock.json → exit0/空。
完整Python读取/验证步骤：Path.read_text UTF8→json.loads(object_pairs_hook逐键唯一)→全部220条定位与字典顺序相同→对全部217完整integrity base64.b64decode(validate=True)且64字节→urlparse全部resolved必须HTTPS、无username/password/query/fragment→逐条展示全部元数据、依赖/peer/optional/平台字段；全部执行exit0。完整字段集合21种，root顶层6键（含packages）均读取。
调度层有两次JavaScript模板字面量语法错误，均在远程命令执行前失败；修正嵌套反引号后才执行，不是源文件错误或Grok交付失败。首次原生图把missingOptional限制在invalid条件内，字段空集只证明无无效缺失项；最终脚本已改为完整列出29条缺失peerOptional并重跑。首次ci输出2290行被工具截断，保留其执行事实；为完整取到关键信息又执行同一无副作用预演，并按JSON结构打印add188/remove186/change0，未将丢失部分冒称已逐行读取。

一次落盘前的条目定位断言因调度脚本正则转义失败，未发生文件写入；改为按该JSON实际四空格键行定位后重跑，不修改源文件。

规范依据（仅解释配置协议，不增冻结判据）：
- https://docs.npmjs.com/cli/v11/configuring-npm/package-lock-json/ ：锁描述确定依赖树；packages键为位置；版本3属npm9及以上使用的格式；link/optional/integrity是安装元数据。
- https://docs.npmjs.com/cli/v11/commands/npm-ci/ ：ci检查manifest与锁一致性，dry-run仅报告拟动作；实际安全性同时用本机ci.js的分支核实。文档显示v11.19.1，本机npm11.19.0，版本差异明确，不用网页替代本机执行。

## 7. Grok实际派发与阶段边界

实际派发命令（模型grok-4.6、xhigh及其它参数不变；完整提示保留）：
```powershell
$remoteScript = @'
cd /home/james/complex-management && python3 - <<'PY'
from pathlib import Path
import subprocess,sys,stat
target=Path('/home/james/complex-management/docs/audit-2026-09/modules/M004-b1-grok-raw.md')
def meta():
 if not target.exists(): return None
 s=target.stat()
 return (s.st_size,s.st_mtime_ns,s.st_ctime_ns,s.st_ino)
before=meta()
prompt="执行160-audit-prompt V0.17当前471计划M004 B1完整独立审计。唯一正本/home/james/complex-management main；模块名“治理门禁与 CI 基线／配置与工程入口／第2段”；唯一受审文件package-lock.json（3321行，OVERSIZE-SINGLE-FILE），当前documentVersion V0.02。ledger M001/M002/M003已PASS，M004 IN_PROGRESS、历史B3累计1、方案修复2、交付失败0，不清零。旧322同号报告只作历史，不继承旧PASS。\n先自读docs/audit-2026-09/plan/modules.md当前M004完整范围、runbook.md、criteria-lock.md general全文与M004映射；完整18项GEN逐一覆盖。全3321行按结构分段完整阅读，不只抽样。为核实锁文件允许只读root/workspace package.json、node/npm当前环境、既有npm解析器/依赖工具/CI中锁入口；属于验证直接依赖，不扩大其他模块审计。仅三权威HTML为业务来源，需时读准确上位全文。\n严格互盲：不读任何self raw/findings/其他模块报告收口/本模块旧报告正文；自己的旧raw仅版本元数据以便追加。ledger仅顶部基准和M004状态行。只允许Grok此一外审，不派任何subagent，不用MCP_Codex_160或MCP_Cloud_160。只能写指定grok raw，禁止改依赖/代码/HTML/配置/计划/锁/ledger/findings/questions，禁提交推送部署、安装依赖、新worktree、新测试或额外产物。禁npm install/npm ci实装/npx/npm exec自动下载；命令预演须先核实无安装无源码改动，必要纯计算副本仅/tmp并说明。不要跑未读懂或可能外发、删数据的命令。\n三线完整：（1）第一性原理：锁文件为什么存在、每类元数据/包条目用途、无效重复和未经验证前提；（2）顺从式：18GEN、root/workspaces声明一致性、版本/引擎/依赖解析/完整性/平台可选分支/链接是否满足真实消费方；（3）对抗式：正常首次安装/构建可能触发的解析错误、清单漂移、缺失引用/错版本/工作区链接/peer/optional/engines/本机平台差异/下载源与完整性字段。用当前已安装工具和无副作用可复现断言验证，不能把JSON parse、节点数、仅grep、dry-run当依赖下载/全构建/业务测试PASS。不要为极端组合或仅可设想场景新增门禁/计数/文件。若不联网则明确没有证明registry可达/远端tarball字节；不要伪称npm audit或全安装完成。现有文件本次不改不强制补版本；不要改应用SemVer/lockfileVersion/documentVersion。\n按V0.17§6技能先实查并读取适用SKILL.md：using-superpowers、grilling/grill-me自行追问、codebase-design只冻结general、diagnosing-bugs/systematic-debugging只读复现、ponytail-review删除价值、verification-before-completion证据；可选batch-grill-me/ubiquitous-language有则收窄使用，无则记缺口。技能本体第三席/向人提问/写额外产物条款不适用。纯锁配置无行为改动不新增TDD文件。\n四分类严格冲突→细化→技术性→添加。锁的纯依赖/构建元数据不是业务发明；每类/每包有完整技术性登记及无业务发明理由，不得用包名就推断新业务需求。若指向HTML上位就给准确文件行号+完整原文，不把有上位的内容跳归技术性。真正业务发明不能隐藏。每个finding含临时GR编号、准确路径行号、完整依据/命令与输出、冻结GEN ID/2.3类型、分类、工程缺陷是/否、队列建议。全模块审完再收齐报告，不中途修或问。\n已知上下文：全局html-ledger-sync/entry-anchors因BSM工作区与CSV镜像不一致而红，镜像属M425/M427；本模块不要为刷绿改HTML/CSV或重开M001—003。如发现锁文件自身可复现缺陷仍独立举证。用户已确认BSM1020来源枚举按项目可配、1068打折原因分类可配就是原意，不再判为冲突。任何新HTML自身问题只完整报告，不能擅改。\n完整报告要有全3321行连续覆盖表、每包/条目技术性分类、18GEN逐项、三线完整证据、命令真实exit/输出/运行边界、所有新finding、本席B1 PASS或FAIL和是否HTML问题。先独立实算criteria-lock.md SHA，本次报告第一行严格CRITERIA-LOCK-SHA256: <独立实算64位小写hex>，不提供预期值，正文别再写独占字段行。\n唯一报告路径/home/james/complex-management/docs/audit-2026-09/modules/M004-b1-grok-raw.md。已有全部字节保留，在末尾追加本次完整报告；自身版本V0.03→V0.04。完整落盘后结束，不要求stdout任何机器标记。"
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
适配器实际结果：
```text
REPORT_PATH=/home/james/complex-management/docs/audit-2026-09/modules/M004-b1-grok-raw.md
EXIT_CODE=0
```
进程真实结束，本席仍未读该raw正文。本段写完以后才允许根读取当前Grok完整追加报告并做B1后四值校验。本席写入时实算/sidecar/ledger三个已知值相等；未冒称已取本次Grok字段或双PASS。

## 8. 当前发现与结论

新finding：空集。无工程缺陷、无冲突、无添加项，无新HTML自身问题或需操作员答复事项。本席B1 = PASS。接下来收齐Grok本次报告、四值校验、B2整批归并；不省略B2/B5，不提前标模块PASS。
未修改package-lock.json、root/workspace manifest、三HTML、冻结锁。三HTML当前SHA分别BSM a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa、BEQR 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66、PGM c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c，保持现基准。用户B/C原意与M001 A/D一次授权边界保留，不重开M001—003。
没有全依赖下载安装、远端tarball字节校验、npm audit、全前端/后端/Android构建、数据库迁移、业务UI或生产部署结论。全局已知BSM镜像门禁红灯仍保留，不作为本模块锁文件自动缺陷。


## M004 当前471计划 B2补核与整批归并票
版本：V0.06

本席B2结论：PASS。该票是对findings V0.93整批候选的归并及处置复核，不是新的独立B1，不是模块PASS，也不是npm audit通过。
候选316366字节，SHA256=2b1e47a4b23bc40a0c90dcd31ede7784fe9d775fa04c72f814147dcfc011be78。当前7ID，队列候选全否；M004 IN_PROGRESS/B3累计1，尚待Grok独立B2票。

### 完整输入及B1四值
当前B1 self V0.05完整文件前132363字节、SHA256 079da8b31892d00232e26f4097fac609b270df926149d4fc15fd872f20e66a4f，报告实际L540—1129。上次落盘工具元数据将起行写539；按实际独占行核实应为540，此处纠正定位，不改原报告。
当前B1 Grok V0.04完整145659字节、SHA256 d2707c6c5aa9e4a6e585f079f88aa1f6a0615f2f196525b514c79b6db4160607，报告L873—1536。全部665追加行含首空行已读；两席本次B1均PASS/当时finding空集。两份旧322前缀保留，不继承旧结论。
B1后四值：实算=b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a；sidecar=b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a；ledger=b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a；指定M004-b1-grok-raw本次最后独占行=b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a。四值全等，COUNT1不变。

### B2新增核查CX-B2-01与原票边界
收齐B1后按原001—003已知库问题核对当前记录，本席额外执行npm公告查询。原V0.05所述“未npm audit”是当时事实，没有改成当时已跑；不能把B1双PASS冒写为Grok同意本B2新观察。
实际完整输出：
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
npm audit --package-lock-only --ignore-scripts --json真实exit1；两个moderate条目来自同一个GHSA-82fw-gwwq-j7x9，归新正式M004-007。没有执行audit fix。父Python脚本exit0仅表示检查与字节保护断言完成，不能覆盖子命令exit1。

本次主来源：https://github.com/vitest-dev/vitest/security/advisories/GHSA-82fw-gwwq-j7x9 。已完整读公告：当前4.1.9命中，4.1.11为修补版；公开插件和Vitest浏览器模式注册路径有不同前提。当前配置不启用公告所述通道，不能仅凭npm两个条目就宣称本项目已出现同一文件读取行为。
直接调用上下文完整原文与检索输出：
```text
CONFIG_FILES EXIT 0
apps/web/vite.config.ts
apps/web/vitest.config.ts

FILE apps/web/vite.config.ts
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET ?? "http://127.0.0.1:8080";

export default defineConfig(({ mode }) => ({
  // 子路径部署：构建产物落在 /complex/ 前缀下（门户 portal.py 把 /complex/ 代理到 preview）；
  // dev（mode!=='serve'）仍在根路径，方便本地直接调试。
  base: "/",
  plugins: [react()],
  server: {
    proxy: {
      "/api": {
        target: apiProxyTarget,
        changeOrigin: true,
      },
    },
  },
  build: {
    // 真实分包已完成关键优化：admin 32 个页面经 React.lazy 路由级切分，
    // AdminApp 从 ~224kB 降到 ~19kB，首屏不再一次性加载全部页面代码。
    // 剩余最大单块 vendor-antd（antd 框架本体）约 586kB，是框架固有体积；
    // 实测在 rolldown 的 manualChunks 下进一步剥离 rc-*/dayjs 会触发反向合并、
    // 生成更大的单块（~1MB），属退化，故不强拆。
    // 阈值设为 650kB：覆盖 antd 框架块、消除噪音警告，又能在未来出现新的
    // 真实回归（如某业务块异常膨胀）时仍然报警。
    chunkSizeWarningLimit: 650,
    rollupOptions: {
      input: {
        main: "index.html",
        ticketing: "ticketing/index.html",
        bar: "bar/index.html",
        amusement: "amusement/index.html",
      },
      output: {
        manualChunks(id) {
          if (id.includes("node_modules/react") || id.includes("node_modules/react-dom")) return "vendor-react";
          if (id.includes("node_modules/@ant-design/icons")) return "vendor-antd-icons";
          if (id.includes("node_modules/antd") || id.includes("node_modules/@ant-design")) return "vendor-antd";
          if (id.includes("node_modules")) return "vendor";
        },
      },
    },
  },
}));

FILE apps/web/vitest.config.ts
import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
    include: ["src/**/*.test.{ts,tsx}"],
    globals: false,
    restoreMocks: true,
  },
});

CALLER_SEARCH ["rg", "-n", "@vitest/(mocker|browser)|mockerPlugin|interceptorPlugin", "apps/web", "--glob", "!node_modules/**", "--glob", "!package-lock.json"] EXIT 1


```
未读写真实秘密、未启动公开测试服务、未制作攻击请求。以上是既有路径核实，三份HTML不是软件协议规范；未据上游描述发明业务要求。公开dev host不等于已注册mockerPlugin；jsdom不等于Vitest浏览器模式。

### 整批差集与分类
- 001—003沿用历史永久ID，原缺陷/修复史保留；当前nanoid3.3.18、postcss8.5.23及^3.3.16、undici7.29.0均已在本次逐字段读取和原生图中确认，公告当前也未列它们。不用旧整体SHA宣称现字节完全相同：把当前documentVersion在内存改回V0.01得f637ce6cf83b9c580c04bcfee0b668851a2b1d29a595bda9dec8b5435bf04db4，不等于旧65254915…；该试算没有写文件，当前按本次完整审查判断。
- 004/005/006覆盖两席全220条无业务发明登记，root/清单/link、依赖图/平台/peer、来源/SRI分别沿用既有ID。没有把同一个问题复制到结构登记中。
- 新007准确来自本席B2 CX-B2-01，GEN004/010/013，证据3/4；上游缺陷命中事实保留。按V0.17铁律6当前正常入口不具备注册条件，候选当前工程缺陷否、队列否；没有宣称库无缺陷、公告误报或永不触发。这个处置尚未Grok认可，须其本批独立审查。
- 无冲突/添加或HTML自身问题；当前没有人闸。全部7项均候选技术性/队列否，但任一B2 FAIL就继续整批归并，不能用本席票覆盖Grok。
- 若双PASS，空队列跳B3/B4进入完整B5-r1；本轮没有修复，没有增加B3次数。当前全局HTML镜像红灯及npm audit两个moderate均公开保留，不能写全仓门禁绿。

### 本轮回执
步骤M004 B2/471；ledger V1.64 IN_PROGRESS；B3累计1、方案修复2、交付失败0。实算锁hash如上，锁V0.11/COUNT1保持。B1 Grok命令与exit0已完整在V0.05保存，B2 Grok在本段写入时NOT-CALLED，将立即按同一适配器派发；不调用MCP_Cloud_160/Claude Agent，不新增第三席或改全局MCP配置。

## M004 B2 双席收齐与定稿回执
版本：V0.07
本追加只记录本批收齐/定稿，不改写本席V0.06 PASS或前V0.05独立B1。当前M004/471，B2 PASS；ledger IN_PROGRESS，历史B3=1/方案修复2/失败重跑0。输入及全部7项判定见前V0.06和findings V0.94当前定稿表。
Grok本次实际命令：
```powershell
$remoteScript = @'
cd /home/james/complex-management && python3 - <<'PY'
from pathlib import Path
import subprocess,sys,stat
target=Path('/home/james/complex-management/docs/audit-2026-09/modules/M004-b1-grok-raw.md')
def meta():
 if not target.exists(): return None
 s=target.stat()
 return (s.st_size,s.st_mtime_ns,s.st_ctime_ns,s.st_ino)
before=meta()
prompt="执行160-audit-prompt V0.17当前471计划M004 B2整批归并独立复核。只读/home/james/complex-management main；模块仅package-lock.json3321行/220条/18GEN，历史B3=1、计划修复2、失败重跑0，ledger IN_PROGRESS。源SHA d3676e896b885b6bbb979440bb51b9dfe60728735cee000e528ab5d1791bcfef，documentVersion V0.02；M001—M003保持PASS。全程直接已有shell/SSH工具，不用MCP_Cloud_160、MCP_Codex_160或任何子代理。\n审核同一findings V0.93候选：docs/audit-2026-09/findings.md，316366bytes，SHA256 2b1e47a4b23bc40a0c90dcd31ede7784fe9d775fa04c72f814147dcfc011be78。只本文件最上方当前M004 B2候选及相关旧M004历史ID；其它模块不变，不重新审findings本身。\n本批完整输入：modules/M004-b1-self-raw.md 当前V0.06，141623bytes，SHA256 460c7f02e679e5631cf30d410af8e6b3d2f90f3ab5959c391bf3235a3ad068f3。本次独立B1为其原前132363bytes、SHA079da8b31892d00232e26f4097fac609b270df926149d4fc15fd872f20e66a4f，L540—1129；其后V0.06是B2补核和本席票，不假称独立B1。你的当前B1 V0.04已在modules/M004-b1-grok-raw.md，145659bytes、SHA d2707c6c5aa9e4a6e585f079f88aa1f6a0615f2f196525b514c79b6db4160607，本次报告L873—1536。两席本次B1完整三线PASS，当时finding空；先你结束、根独立保存后才读你全文。B1后四值已相等，但B2禁止追加独占锁行。\n须完整核对全部7个ID、差集、分类、工程缺陷、队列，不能只看新增项。001—003旧nanoid/postcss/undici工程缺陷/修复史完整保留，当前版本3.3.18/8.5.23及内部nanoid^3.3.16/7.29.0，当前队列否；004—006为元数据、完整220条图、HTTPS/SRI技术登记，继续否。旧322 PASS不继承，当前全量B1/原生解析证据与旧整体源SHA不同的限制均须准确。\n新增007来自根B2 CX-B2-01：按历史修复核对补跑npm audit --package-lock-only --ignore-scripts --json，真实exit1，moderate2/high0/critical0；两个包vitest和@vitest/mocker4.1.9对应同一GHSA-82fw-gwwq-j7x9，合并1个ID不分两项。上游公告 https://github.com/vitest-dev/vitest/security/advisories/GHSA-82fw-gwwq-j7x9 可直接读取，若不用联网按来源明确的全文与当前原文验证；不能把版本命中否定。根V0.06附真实完整audit输出及两配置全文。当前vitest.config.ts使用jsdom、plugins仅react；vite.config.ts插件仅react；精确rg @vitest/(mocker|browser)|mockerPlugin|interceptorPlugin在apps/web无匹配。dev有host0.0.0.0不等于注册mockerPlugin，jsdom不等于browser模式。候选按V0.17铁律6的正常首跑边界判007技术性/当前工程缺陷否/队列否，只登记上游公告，不升级。这个判断尚未外席认可，你须独立确认或明确否决；不要因为Codex PASS就跟随。\n若你认为007应修，须按冻结GEN004/010/013说明当前可观察事实如何支持，而非凭CVE标题/两包计数自动排队；若认同仅登记，明确保留npm audit非0与告警事实，不写audit全绿、不写公告误报或永远不可触发。不向人提问，不借上游公告新增业务要求/门禁/判据/技能/文件，不在B2修改依赖或任何源文件。\n按V0.17§6使用已核实技能 receiving-code-review、grilling、systematic-debugging/diagnosing-bugs、codebase-design、verification-before-completion及适用ponytail-review；沿用本会话实际方法，缺口如实记。只读直接对照配置以验证锁，不宣布其他模块审完。不安装依赖、不npm audit fix、不提交推送部署、不新worktree、不写其它产物。\n结论必须完整给出两份当前raw输入、逐ID差集与归属、007事实和边界、是否遗漏/分歧/需人闸、明确B2 PASS或FAIL及整批下一步。root候选认为7项队列全否则跳B3/B4但必须完整B5-r1，不能现在标模块PASS。任一FAIL不定稿不修，原票保留；禁止你代改findings或self。\n唯一报告路径/home/james/complex-management/docs/audit-2026-09/modules/M004-b1-grok-raw.md。已有所有字节保留，把本次完整B2归并结论追加到末尾，自身版本V0.04→V0.05。B2追加段不得出现任何独占CRITERIA-LOCK-SHA256:行；不另建b2文件。完整落盘后结束，不要求stdout机器标记。"
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
实际适配器返回REPORT_PATH=/home/james/complex-management/docs/audit-2026-09/modules/M004-b1-grok-raw.md、EXIT_CODE=0。真实进程结束后才读取其本次完整215追加行；当前完整文件163323bytes，SHA c3cf6898805d515b7bfcb49f106d996e056b0b78ed8e51220fa1edec3012a3b1，V0.05，B2 PASS，旧145659bytes SHA d2707c6c5aa9e4a6e585f079f88aa1f6a0615f2f196525b514c79b6db4160607保持。无新独占锁行。
两席对全部001—007分类/队列一致，无漏并无改写，无人闸。根V0.06与GrokV0.05共同批准的候选V0.93 SHA2b1e47a4b23bc40a0c90dcd31ede7784fe9d775fa04c72f814147dcfc011be78已标为V0.94定稿；未改变其实质。007只登记，保留npm audit exit1和版本命中，不改锁、不称全绿。
本次实算锁/sidecar/ledger基准/当前B1 Grok最后独占字段仍均b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a，COUNT1。B2段不写独占锁字段；这不是未来B5后四值检查。
B3/B4跳过，B3计数保持1。下一步先Grok完整B5-r1→真实结束→根独立全量审计保存→读取Grok→B5后四值。现在不能标模块PASS。全局6处BSM镜像红灯保持；无HTML问题或添加人闸。通道直接SSH，无MCP_Cloud_160/第三席。
