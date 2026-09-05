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
