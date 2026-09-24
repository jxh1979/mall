CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M007 B5-r1 Codex整模块全量审计
版本：V0.01

## 1. 本次输入、角色与结论
本席Codex，SSH james@192.168.88.160，唯一正本/home/james/complex-management，main主工作区；HEAD与origin/main均242f487233d028173cb9198eecd079f5d877405c。当前471计划M007“治理门禁与 CI 基线／运行和发布脚本／第1段”；B3累计1，ledger V1.88 IN_PROGRESS，findings V1.09当前队列全否。
Grok B5-r1真实进程已结束，适配器只交付指定路径modules/M007-b5-r1-grok-raw.md与exit0；本席尚未读取该文件正文，也未接触原始JSON。本席之后重新完整读5源文件163行+直接回归102行，全部79冻结判据定义及61段实际HTML逐字比对，重新执行本报告检查。B4不是本次PASS依据。
本席最终审计结论：PASS。无新finding、无未验证且会阻断本模块正常用途的前提、无添加或未答项。本票不是模块双PASS；须此报告落盘后才能读取Grok并做B5四值校验。
技能按本会话已读实际文件复用using-superpowers、grilling、codebase-design、diagnosing-bugs、systematic-debugging、tdd/test-driven-development、verification-before-completion、requesting/receiving-code-review、writing-for-agents、ponytail-review。按V0.17收窄，以当前席独立执行，不派第三席，不新增业务或流程，不为仅登记边界加策略。

## 2. 全文件三线覆盖
| 文件与完整行数 | 第一性原理用途与删去影响 | 顺从式实际核对 | 对抗式实际证据 | 四分类 |
|---|---|---|---|---|
| ops/160/install-git-hooks.sh 1–16 | 把受控hook装到Git实际使用路径；删去将失去新clone安装入口 | 原生--path，default/common路径，install0755；命令失败受set-e管控 | 默认/相对/绝对/tilde隔离真实Git均有效；before tilde失败可复现 | 技术性，开发工具安装，不是BSM-118面向现场用户的产品安装包 |
| ops/160/merge-pr.sh 1–73 | 对指定远端提交等待CI再合并；删去会丢失命令自动检查 | 参数白名单、main/dirty拒绝、本地远端SHA、PR存在、30次空列表、每run watch、最后match-head | 12场景含红CI、等待中SHA前移、最后合并拒绝、显式skip；查询error同回归保留原因且只调用1次 | 技术性，GitHub代码交付流程，无业务审批发明 |
| ops/160/propose-change.sh 1–30 | 已提交分支本地检查后推送开PR；删去失去统一入口 | 标题必填、main拒绝、dirty先于npm、npm非0不推、PR存在分流、base main | 6场景逐条记录调用；dirty无npm/push，red-local不push，push失败不开PR | 技术性，开发者PR提议，不是业务单据审批 |
| ops/160/hooks/pre-push 1–17 | 开发者直推main提示，注释明示非安全边界 | Git四字段只比较第三字段；仅ALLOW_MAIN_PUSH精确1覆盖 | 4基础+delete-main/tag-main/nonliteral-allow，直接回归另有mainish；源main目标feature已放行 | 技术性，无业务权限/红线关闭开关 |
| ops/ci/predeploy-gates.yml 1–27 | 与实际workflow镜像一致、由现有门禁校验的CI说明配置 | YAML原生解析、main事件、完整actionSHA、fetch0、node22、ci忽略脚本、HTML基线、15子入口 | 镜像字节相等；GET实际workflow active；官方tag对象与两个SHA实核一致 | 技术性，静态代码治理，不声称生产上线完成 |
| tools/gate-hardening-tests/m007-regression.mjs 1–102 | 对001–004留下可失败最小自检 | 执行真实源码，仅stub远端及推送，unknown命令失败，finally只删自身临时目录 | 当前7输出PASS；before四脚本+同测试恰4旧缺陷FAIL；不是只测字面实现 | 技术性，直接修复回归，无新增业务 |

冻结源清单修前为156行，本次163行；不能用旧行数冒称当前覆盖。测试直接归本次修复，原计划其它测试仍属M008，不扩大本模块的正式FILES。源码全字节与B3 after树9f63094e21e4ed94a29b29071af5bd07cf99fd6a对应blob相等，主索引未动。

## 3. 四分类及全部观察
先排冲突：六文件不实现钱账、容量、角色、租赁/工单/巡检、业务留痕、密钥托管等业务条款；SKIP_CI_WAIT/ALLOW_MAIN_PUSH是代码流程提示，不是关闭业务七红线；与实际HTML无明文矛盾。再查细化：BSM-116–119的生产安全加固/面向现场安装产品不由Git hook安装或GitHub PR工具承载，不因文件在ops就硬挂业务条目。各条遂归技术性：全部只服务构建、验证和代码交付，无业务发明。冲突0、添加0，无新questions。
001–004当前实行为已修；005–011保持原定稿理由。多run全AND未在正常路径复现错误，不加过滤；propose已经base main，不加另一个base门；hook理论I/O组合不补set-e；英文Git安装消息不是PGM-066列举产品终端，未经缺陷ID不顺手改。以上不是将工程缺陷忽略：本轮独立复证4原缺陷能失败、当前能成功/正确拒绝。
安装后的真实.git/hooks/pre-push仍997bytes旧版本，SHA46aabdf33cb3496d83a71e4357ca85ba2ed9dbbfd4582ab53747bc50bb748d6b；本模块源码修复未重新部署hook。安装程序新clone首次须跑的说明真实保留；本次测试仅在隔离Git安装，不声称主仓运行时已升级。源文件变化不会自动替换.git副本。
现有“私有库免费版”注释解释历史引入动机；本次未重新核账户订阅计划，不用该历史注释证明当前产品授权限制。当前可执行SHA/CI保护用途与是否另有平台保护独立。

## 4. 命令与结果口径
本次全在160执行：四脚本bash -n均0；node tools/gate-hardening-tests/m007-regression.mjs exit0；隔离before+同回归exit1且四缺陷逐一FAIL；隔离边界29场景全部按预期通过；npm run verify:delivery-gates-docs exit0 DELIVERY_GATES_DOCS_PASS。PyYAML BaseLoader解析及镜像相等、package15入口存在、gh官方help和只读API均成功。
实际运行失误完整保留：先误调用npm run verify:doc-gates，exit1 Missing script；这是本席拼错入口，源文件没声明此命令。读取package确认实际verify:delivery-gates-docs后重跑成功；前一次脚本断言终止，后续before红证在更正命令里实际完成，不把未执行写成通过。不是Grok交付失败，不增加其重跑次数。
重复红绿在B5是验证当前测试与源码绑定的必要证据；未为原样低影响内容新增测试。运行中未写业务源码/HTML/锁/主索引/实际hook，未真实push、PR创建、merge、部署或执行生产操作。

## 5. 全部冻结判据覆盖
以下每ID已独立读定义并匹配对应HTML原句；HTML条目的适用性针对本模块六文件具体判断，不宣称该业务在全系统实现完成。18GEN逐项给证据，61HTML逐项给边界，后附读取原文。无新增判据。

| ID | 本席覆盖结论/证据 |
|---|---|
| BEQR-085 项目开放状态 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-086 场次状态 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-087 游乐「当天／每天」口径 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-088 容量双阈值 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-089 园区容量 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-090 设备关键/辅助分级 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-091 全园故障与补偿通票 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-092 安全严重度 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-093 巡检任务状态 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-094 越界类型 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-095 团体/包场 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-096 游乐增强阶段 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-118 运营营业日 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-146 运营公司职责 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-155 商户/运营主体状态 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-160 招商线索状态 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-161 招商意向阶段 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-162 品牌/客户库类型 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-163 合同变更类型 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-164 退租结算状态 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-165 内部应付执行状态 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-166 合同审批链 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-167 租金/账单类型 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-168 营业额抽成模式 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-169 公区能源费 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-170 铺位租赁状态 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-171 物业工单类型 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-172 物业工单状态 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-173 商户服务工单状态 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-174 工单时效 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-175 楼宇机电设备状态 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-176 设备维保类型 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-177 能耗表计类型 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-178 抄表状态 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-179 能耗归集对象 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-180 楼宇巡检任务状态 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BEQR-181 楼宇巡检类型 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BSM-020 楼宇运营 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BSM-028 主要功能 | 已读组织/权限/跨系统/七红线原句；本模块无业务对象、主体归属、收费或权限实现，Git覆盖不等于关闭业务校验。 |
| BSM-081 楼宇运营：机电设备、能耗抄表、楼宇巡检 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BSM-082 主要功能 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BSM-083 和谁有关 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BSM-111 经营看板：让管理层和运营看得懂、能追问 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BSM-112 主要指标 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BSM-113 谁会看 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | 已读组织/权限/跨系统/七红线原句；本模块无业务对象、主体归属、收费或权限实现，Git覆盖不等于关闭业务校验。 |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | 已读组织/权限/跨系统/七红线原句；本模块无业务对象、主体归属、收费或权限实现，Git覆盖不等于关闭业务校验。 |
| BSM-116 系统交付与上线：正式上线要做的安全加固和安装交付 | 已读实际HTML；开发Git/CI流程不承载产品生产密钥加固或面向现场安装，不以此源码PASS声明该业务已完成。 |
| BSM-117 正式上线时的安全加固 | 已读实际HTML；开发Git/CI流程不承载产品生产密钥加固或面向现场安装，不以此源码PASS声明该业务已完成。 |
| BSM-118 能自动识别环境的安装包 + 内附教程 | 已读实际HTML；开发Git/CI流程不承载产品生产密钥加固或面向现场安装，不以此源码PASS声明该业务已完成。 |
| BSM-119 和谁有关 | 已读实际HTML；开发Git/CI流程不承载产品生产密钥加固或面向现场安装，不以此源码PASS声明该业务已完成。 |
| GEN-001 必要性与最小性 | 六项各有独立工具/验证用途，CI镜像被现有机检约束；无新增抽象或可删纯转发。 |
| GEN-002 可执行正确性 | 四bash-n、当前7回归、29场景、文档门禁实跑；正常/拒绝/失败行为符合当前源声明。 |
| GEN-003 测试有效性 | 同测试对before四原缺陷失败，对当前7项通过；真实执行脚本，远端边界stub；所有关键修复有断言。 |
| GEN-004 信任边界校验 | merge方式白名单与本地/远端/PR SHA；propose标题/分支/dirty；hook第三字段目标与精确1覆盖，CLI参数双引号。 |
| GEN-005 授权与最小权限 | GitHub保护由凭证/远端授权，工具检查不代替服务端权限；两个覆盖不是业务授权开关，无业务权限路径。 |
| GEN-006 数据完整性 | 无数据库金额状态写入；失败不触发模拟合并，推失败不开PR；不承诺平台级事务。 |
| GEN-007 并发与幂等 | PR已存在只更新URL，等待后SHA前移及合并时match-head阻断实测；多run组合不发明新策略。 |
| GEN-008 接口与模式一致性 | Git四字段、gh watch及match-head官方help、workflow文件名/parent15子入口一致。 |
| GEN-009 错误处理与恢复 | query错误保留stderr且一次即退出；真实空列表30次；red-local/push/red-CI错误均可见且阻断。 |
| GEN-010 安全与敏感信息 | 无明文秘密；变量用CLI参数/双引号，无eval；git安装配置为本机管理员路径，不当作不可信业务上传路径。 |
| GEN-011 可观测与审计证据 | merge输出SHA/run/失败原因，两个覆盖醒目提示；明确是开发日志和留痕提醒，不冒称业务持久审计。 |
| GEN-012 资源与性能边界 | 空列表30轮×10秒、run限10；watch等待实际CI；临时目录finally清理，无业务批处理连接资源。 |
| GEN-013 构建配置与依赖 | CI完整SHA/fetch0/node22/npm ci ignore-scripts；15入口存在；本地Node24.20.0实际执行，未声称CI Node22全链实跑。 |
| GEN-014 数据库迁移正确性 | 不含SQL/迁移/数据库访问，适用性核查无该类动作。 |
| GEN-015 界面可用性与可访问性 | 开发者CLI，错误与使用提示可读；无产品键盘/焦点交互；不把PGM066产品全中文延伸成英文CLI缺陷。 |
| GEN-016 文档可验证性 | 逐行路径/命令/版本、实际workflow active/镜像/官方tag核实；主仓旧安装副本及未全仓运行如实记录。 |
| GEN-017 业务来源分类 | 逐条冲突→细化→技术性→添加，六文件技术性无业务发明；未擅改B/C原意。 |
| GEN-018 第一性原理证据 | 本轮源全文、HTML61段逐字、实际命令、官方API/help四类事实取证；未读取对方本轮结论。 |
| PGM-015 楼宇运营 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| PGM-018 物理项目与运营公司：多对多协作，不是固定隶属层级 | 已读组织/权限/跨系统/七红线原句；本模块无业务对象、主体归属、收费或权限实现，Git覆盖不等于关闭业务校验。 |
| PGM-026 主要功能 | 已读组织/权限/跨系统/七红线原句；本模块无业务对象、主体归属、收费或权限实现，Git覆盖不等于关闭业务校验。 |
| PGM-039 智能设备场景联动范围 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| PGM-052 楼宇运营：管好建筑本身的设备、能耗和巡检 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| PGM-053 楼宇机电设备 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| PGM-054 能耗计量 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| PGM-055 楼宇巡检 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| PGM-056 和谁有关 | 已读该业务条目全文并对照六文件；无该类业务对象、状态、金额/数量、界面或设备动作，不能把ops标签当作业务实现，不适用其执行要求，无冲突/发明。 |
| PGM-066 已拍板规则和待排期问题 | 已读产品各终端全中文、业务留痕等原句；本模块无产品UI或业务动作；CLI安装消息不升缺陷，源无业务开关。 |

## 6. 本轮完整源码覆盖记录

### ops/160/install-git-hooks.sh
bytes=707 SHA256=8609b7e20a6fc49cec6ffc90781310e73fb943c00ca7791e564e956166f9a37b

~~~~text
1: #!/usr/bin/env bash
2: # 文件版本：V0.01
3: # 安装仓库 git 钩子到生效的 hooks 目录(worktree 共用)。新克隆/新机首次须跑一次。
4: # 注意:若仓库/全局设了 core.hooksPath,Git 只认该路径——装到别处会静默失效,故在此显式处理。
5: set -euo pipefail
6: cd "$(dirname "$0")/../.."
7: custom="$(git config --path --get core.hooksPath || true)"
8: if [[ -n "$custom" ]]; then
9:   dest="$custom"
10:   echo "[install-hooks] 检测到 core.hooksPath=$custom,安装到该路径(否则钩子不生效)。"
11: else
12:   dest="$(git rev-parse --git-common-dir)/hooks"
13: fi
14: mkdir -p "$dest"
15: install -m 0755 ops/160/hooks/pre-push "$dest/pre-push"
16: echo "installed pre-push hook -> $dest/pre-push"
~~~~

### ops/160/merge-pr.sh
bytes=4180 SHA256=3e222657e568b852608eec39bc64efd2d618dce71b93f22864f3a076d68bcf06

~~~~text
1: #!/usr/bin/env bash
2: # 文件版本：V0.02
3: # 合并前置检查(2026-08-07 门禁升级批 1,用户弹窗批立项):CI 绿灯才允许合并。
4: # 病史:本仓是私有库免费版,设不了 required status check,.github/workflows/predeploy-gates.yml
5: #      自己在注释里写明只是「可见信号」。也就是说 gh pr merge 对红灯毫无抵抗力,全靠人记得去看一眼。
6: #      「靠人看一眼」这件事在本仓已经失手过(门禁加了整轮一次没跑过)。此脚本把那一眼变成机器动作。
7: # 用法:在已 push 且已开 PR 的 feature 分支上执行:
8: #      ops/160/merge-pr.sh [--squash|--merge|--rebase]   (默认 --squash)
9: # 逃生口:SKIP_CI_WAIT=1 ops/160/merge-pr.sh   —— 只在 CI 本身故障时用,会打印醒目警告。
10: set -euo pipefail
11: method="${1:---squash}"
12: case "$method" in --squash|--merge|--rebase) ;; *) echo "合并方式只能是 --squash|--merge|--rebase" >&2; exit 1;; esac
13: 
14: branch="$(git rev-parse --abbrev-ref HEAD)"
15: [[ "$branch" == "main" ]] && { echo "[merge] main 上没有待合并的 PR;请切到 feature 分支。" >&2; exit 1; }
16: 
17: if [[ -n "$(git status --porcelain)" ]]; then
18:   echo "[merge] 工作区有未提交改动;CI 跑的是远端 commit,这些改动不在里面。" >&2
19:   echo "        先提交并 push,或 stash 掉再合。" >&2
20:   git status --porcelain >&2
21:   exit 1
22: fi
23: 
24: head_sha="$(git rev-parse HEAD)"
25: remote_sha="$(git ls-remote origin "refs/heads/$branch" | cut -f1)"
26: if [[ -z "$remote_sha" ]]; then
27:   echo "[merge] origin 上没有 $branch;先跑 ops/160/propose-change.sh 推分支开 PR。" >&2; exit 1
28: fi
29: if [[ "$head_sha" != "$remote_sha" ]]; then
30:   echo "[merge] 本地 HEAD(${head_sha:0:8}) 与 origin/$branch(${remote_sha:0:8}) 不一致——" >&2
31:   echo "        CI 跑的是远端那个 commit,合并前必须先 push,否则等的是别的代码的绿灯。" >&2; exit 1
32: fi
33: gh pr view "$branch" >/dev/null 2>&1 || { echo "[merge] $branch 还没有 PR;先跑 ops/160/propose-change.sh。" >&2; exit 1; }
34: 
35: if [[ "${SKIP_CI_WAIT:-0}" == "1" ]]; then
36:   echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" >&2
37:   echo "!! SKIP_CI_WAIT=1:跳过 CI 绿灯检查,直接合并 ${head_sha:0:8} 到 main !!" >&2
38:   echo "!! 治理门禁(predeploy-gates)本次未被确认通过。请在 PR 里写明原因。   !!" >&2
39:   echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" >&2
40: else
41:   echo "[merge] 等 predeploy-gates 对 ${head_sha:0:8} 的 CI 结果..."
42:   run_ids=""
43:   for i in $(seq 1 30); do
44:     run_ids="$(gh run list --commit "$head_sha" --workflow predeploy-gates.yml --limit 10 --json databaseId -q '.[].databaseId' )"
45:     [[ -n "$run_ids" ]] && break
46:     echo "[merge]   还没有 run(第 $i/30 次),10s 后重试..."
47:     sleep 10
48:   done
49:   if [[ -z "$run_ids" ]]; then
50:     echo "[merge] 等了 5 分钟仍没有 ${head_sha:0:8} 的 predeploy-gates run,中止合并。" >&2
51:     echo "        可能原因:workflow 被禁用、push 的不是这个 commit、或 Actions 排队异常。" >&2
52:     echo "        CI 确实故障时可用 SKIP_CI_WAIT=1 强行合并(会打印警告并需在 PR 说明原因)。" >&2
53:     exit 1
54:   fi
55:   for rid in $run_ids; do
56:     if ! gh run watch "$rid" --exit-status --compact; then
57:       echo "[merge] CI predeploy-gates 未通过(run $rid),中止合并。" >&2
58:       gh run view "$rid" --json url -q .url >&2 || true
59:       exit 1
60:     fi
61:   done
62:   echo "[merge] CI 全绿($(echo $run_ids | wc -w) 个 run),继续合并。"
63: fi
64: 
65: # 合并必须钉住刚才看绿的那个 commit:等待 CI 期间若分支被推了新 commit,按分支名合并会把未验的 tip 合进 main。
66: remote_now="$(git ls-remote origin "refs/heads/$branch" | cut -f1)"
67: pr_head="$(gh pr view "$branch" --json headRefOid -q .headRefOid)"
68: if [[ "$remote_now" != "$head_sha" || "$pr_head" != "$head_sha" ]]; then
69:   echo "[merge] 分支在等待 CI 期间被更新:已验 ${head_sha:0:8},远端 ${remote_now:0:8},PR head ${pr_head:0:8}。中止,请对新 commit 重跑本脚本。" >&2
70:   exit 1
71: fi
72: gh pr merge "$branch" "$method" --match-head-commit "$head_sha"
73: echo "[merge] 已合并 $branch($method),head ${head_sha:0:8}。"
~~~~

### ops/160/propose-change.sh
bytes=1626 SHA256=88d3abe9a1c1beaecb7ce6645df537be088768620b72827ad293ba74f7000a8b

~~~~text
1: #!/usr/bin/env bash
2: # 文件版本：V0.01
3: # 变更提议流程(2026-07-05 起替代直推 main):
4: #   推 feature 分支 → 开/更新 PR → (调用方另跑独立复核) → predeploy 门禁绿 → ops/160/merge-pr.sh。
5: #   合并动作一律走 merge-pr.sh(它会等 CI 绿),不要手敲 gh pr merge;CI 故障需 SKIP_CI_WAIT=1 时须在 PR 说明原因。
6: # 用法:在含已提交改动的 feature 分支上执行:ops/160/propose-change.sh "<PR标题>" ["<PR正文>"]
7: set -euo pipefail
8: title="${1:?需要 PR 标题}"; body="${2:-}"
9: branch="$(git rev-parse --abbrev-ref HEAD)"
10: [[ "$branch" == "main" ]] && { echo "不要在 main 上提议;先切 feature 分支。" >&2; exit 1; }
11: 
12: if [[ -n "$(git status --porcelain)" ]]; then
13:   echo "[propose] 工作区有未提交改动;请先提交或 stash,再运行本地检查并推送。" >&2
14:   exit 1
15: fi
16: 
17: echo "[propose] 本地跑 predeploy 门禁(红则不推)..."
18: npm run verify:predeploy
19: 
20: echo "[propose] 推 feature 分支 $branch ..."
21: git push -u origin "$branch"
22: 
23: if gh pr view "$branch" >/dev/null 2>&1; then
24:   echo "[propose] PR 已存在,已更新:"; gh pr view "$branch" --json url -q .url
25: else
26:   gh pr create --base main --head "$branch" --title "$title" --body "${body:-自动创建;详见提交说明。独立复核见 PR 评论。}"
27: fi
28: echo "[propose] 下一步:①独立复核(codex/评审)②合并一律走 ops/160/merge-pr.sh"
29: echo "[propose]   merge-pr.sh 会先等 GitHub Actions predeploy-gates 对本 commit 变绿再 gh pr merge;"
30: echo "[propose]   本仓是私有库免费版设不了 required status check,别再手敲 gh pr merge 绕过这道检查。"
~~~~

### ops/160/hooks/pre-push
bytes=922 SHA256=e38ccd33e40065045c24fa8d4bb08d35f519a753ce2520884fee7103e4e77f85

~~~~text
1: #!/usr/bin/env bash
2: # 文件版本：V0.01
3: # 2026-07-05:禁止直推 main(补 GitHub 私有库免费版无分支保护的缺口)。开发者提示,非安全边界。
4: # 正道:推 feature 分支 → 开 PR → 独立复核 + predeploy 门禁绿 → gh pr merge。
5: # 紧急直推需 ALLOW_MAIN_PUSH=1 覆盖(留痕:请在 PR/提交说明记录原因)。
6: while read -r local_ref local_sha remote_ref remote_sha; do
7:   # Git 标准输入四字段,只按第三字段 remote_ref 判断目标分支。
8:   if [[ "$remote_ref" == "refs/heads/main" ]]; then
9:     if [[ "${ALLOW_MAIN_PUSH:-0}" != "1" ]]; then
10:       echo "[pre-push] 拒绝直推 main。请走 PR 流程(ops/160/propose-change.sh)。" >&2
11:       echo "[pre-push] 确需直推:ALLOW_MAIN_PUSH=1 git push ...(并在 PR/说明记录原因)。" >&2
12:       exit 1
13:     fi
14:     echo "[pre-push] ALLOW_MAIN_PUSH=1 覆盖:放行直推 main(已留痕提醒)。" >&2
15:   fi
16: done
17: exit 0
~~~~

### ops/ci/predeploy-gates.yml
bytes=1335 SHA256=4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d

~~~~text
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
~~~~

### tools/gate-hardening-tests/m007-regression.mjs
bytes=5239 SHA256=10e7b07bf603eb2a47d07fa7a9a8f9f022ba4ff8c3687a1f99cd2889af5781d8

~~~~text
1: // 文件版本：V0.01
2: // M007-001～004：执行真实脚本；只替换 GitHub/推送边界，禁止真实发布。
3: import assert from "node:assert/strict";
4: import fs from "node:fs";
5: import path from "node:path";
6: import { fileURLToPath } from "node:url";
7: import { spawnSync } from "node:child_process";
8: 
9: const root = fileURLToPath(new URL("../../", import.meta.url));
10: const scratch = path.join(root, ".scratch");
11: fs.mkdirSync(scratch, { recursive: true });
12: const temp = fs.mkdtempSync(path.join(scratch, "m007-regression-"));
13: const failures = [];
14: const exec = (cmd, args, cwd, env = {}, input) => {
15:   const result = spawnSync(cmd, args, { cwd, input, encoding: "utf8", env: { ...process.env, ...env } });
16:   assert.equal(result.error, undefined);
17:   return result;
18: };
19: const put = (file, text, mode) => {
20:   fs.mkdirSync(path.dirname(file), { recursive: true });
21:   fs.writeFileSync(file, text, "utf8");
22:   if (mode) fs.chmodSync(file, mode);
23: };
24: const check = (name, fn) => {
25:   try { fn(); console.log("PASS " + name); }
26:   catch (error) { failures.push(name); console.error("FAIL " + name + ": " + error.message); }
27: };
28: try {
29:   // M007-001：目的分支才决定是否拦截；源 main、目标 feature 应放行。
30:   for (const [local, remote, allow, want] of [
31:     ["main", "feature/a", "0", 0], ["feature/a", "main", "0", 1],
32:     ["feature/a", "main", "1", 0], ["feature/a", "mainish", "0", 0],
33:   ]) check("M007-001 " + local + " -> " + remote + " allow=" + allow, () => {
34:     const input = "refs/heads/" + local + " " + "a".repeat(40) +
35:       " refs/heads/" + remote + " " + "b".repeat(40) + "\n";
36:     const p = exec("bash", [path.join(root, "ops/160/hooks/pre-push")],
37:       root, { ALLOW_MAIN_PUSH: allow }, input);
38:     assert.equal(p.status, want, p.stderr);
39:   });
40: 
41:   // M007-002：真实 Git 解析 ~/ 路径；验证安装产物落到 Git 使用的位置。
42:   const installRepo = path.join(temp, "install");
43:   const home = path.join(temp, "home");
44:   fs.mkdirSync(home);
45:   for (const rel of ["ops/160/install-git-hooks.sh", "ops/160/hooks/pre-push"])
46:     put(path.join(installRepo, rel), fs.readFileSync(path.join(root, rel), "utf8"));
47:   const installEnv = { HOME: home, GIT_CONFIG_GLOBAL: "/dev/null", GIT_CONFIG_NOSYSTEM: "1" };
48:   check("M007-002 Git 生效路径", () => {
49:     for (const args of [["init", "-q"], ["config", "core.hooksPath", "~/custom-hooks"]]) {
50:       const p = exec("git", args, installRepo, installEnv);
51:       assert.equal(p.status, 0, p.stderr);
52:     }
53:     const p = exec("bash", ["ops/160/install-git-hooks.sh"], installRepo, installEnv);
54:     assert.equal(p.status, 0, p.stderr);
55:     const installed = path.join(home, "custom-hooks/pre-push");
56:     assert.ok(fs.existsSync(installed), "安装成功但 Git 的 HOME/custom-hooks/pre-push 不存在");
57:     assert.deepEqual(fs.readFileSync(installed), fs.readFileSync(path.join(root, "ops/160/hooks/pre-push")));
58:     assert.ok(fs.statSync(installed).mode & 0o111);
59:   });
60: 
61:   // M007-003/004：复用一个窄的进程边界；未知命令失败，真实源码不替换。
62:   const bin = path.join(temp, "bin");
63:   put(path.join(bin, "git"), [
64:     "#!/usr/bin/env bash", "set -eu",
65:     'case "$*" in',
66:     " 'rev-parse --abbrev-ref HEAD') echo feature/m007;;",
67:     " 'status --porcelain') if [[ $M007_CASE == dirty ]]; then echo ' M pending.txt'; fi;;",
68:     " 'rev-parse HEAD') echo aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa;;",
69:     " 'ls-remote origin refs/heads/feature/m007') printf 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\\trefs/heads/feature/m007\\n';;",
70:     " 'push -u origin feature/m007') touch pushed;;",
71:     ' *) echo "unexpected git: $*" >&2; exit 97;;', "esac", "",
72:   ].join("\n"), 0o755);
73:   put(path.join(bin, "npm"), "#!/usr/bin/env bash\nexit 0\n", 0o755);
74:   put(path.join(bin, "sleep"), "#!/usr/bin/env bash\nexit 0\n", 0o755);
75:   put(path.join(bin, "gh"), [
76:     "#!/usr/bin/env bash", "set -eu", 'case "$1 $2" in',
77:     " 'pr view') :;;",
78:     " 'run list') echo AUDIT_QUERY_AUTH_FAILURE >&2; echo query >> queries; exit 2;;",
79:     " 'pr merge') touch merged;;",
80:     ' *) echo "unexpected gh: $*" >&2; exit 98;;', "esac", "",
81:   ].join("\n"), 0o755);
82:   const env = { PATH: bin + ":/usr/bin:/bin", SKIP_CI_WAIT: "0" };
83:   check("M007-003 脏工作区不得推送", () => {
84:     const p = exec("bash", [path.join(root, "ops/160/propose-change.sh"), "验证标题"],
85:       temp, { ...env, M007_CASE: "dirty" });
86:     assert.notEqual(p.status, 0, p.stdout);
87:     assert.equal(fs.existsSync(path.join(temp, "pushed")), false);
88:   });
89:   check("M007-004 查询失败须保留原因并立即退出", () => {
90:     const p = exec("bash", [path.join(root, "ops/160/merge-pr.sh")],
91:       temp, { ...env, M007_CASE: "query-error" });
92:     assert.notEqual(p.status, 0, p.stdout);
93:     assert.ok(p.stderr.includes("AUDIT_QUERY_AUTH_FAILURE"), p.stderr);
94:     assert.equal(fs.readFileSync(path.join(temp, "queries"), "utf8").trim().split("\n").length, 1);
95:     assert.equal(fs.existsSync(path.join(temp, "merged")), false);
96:   });
97:   assert.deepEqual(failures, [], "M007 行为回归失败：" + failures.join("、"));
98: } finally {
99:   assert.equal(path.dirname(temp), scratch);
100:   assert.ok(path.basename(temp).startsWith("m007-regression-"));
101:   fs.rmSync(temp, { recursive: true });
102: }
~~~~

## 7. 本轮命令关键输出（均已实际阅读）

### 当前回归及误写辅助命令（真实失败，不删记录）
~~~~text
CMD ['node', 'tools/gate-hardening-tests/m007-regression.mjs'] EXIT 0
PASS M007-001 main -> feature/a allow=0
PASS M007-001 feature/a -> main allow=0
PASS M007-001 feature/a -> main allow=1
PASS M007-001 feature/a -> mainish allow=0
PASS M007-002 Git 生效路径
PASS M007-003 脏工作区不得推送
PASS M007-004 查询失败须保留原因并立即退出


CMD ['npm', 'run', 'verify:doc-gates'] EXIT 1

npm error Missing script: "verify:doc-gates"
npm error
npm error To see a list of scripts, run:
npm error   npm run
npm error A complete log of this run can be found in: /home/james/.npm/_logs/2026-09-09T03_29_49_666Z-debug-0.log

Traceback (most recent call last):
  File "<stdin>", line 7, in <module>
AssertionError

~~~~

### 更正实际入口与before红证
~~~~text
CMD ['npm', 'run', 'verify:delivery-gates-docs'] EXIT 0

> entertainment-complex-management@0.1.0 verify:delivery-gates-docs
> node tools/verify-delivery-gates.mjs

DELIVERY_GATES_DOCS_PASS


BEFORE_WITH_SAME_TEST_EXIT 1
PASS M007-001 feature/a -> main allow=0
PASS M007-001 feature/a -> main allow=1
PASS M007-001 feature/a -> mainish allow=0

FAIL M007-001 main -> feature/a allow=0: [pre-push] 拒绝直推 main。请走 PR 流程(ops/160/propose-change.sh)。
[pre-push] 确需直推:ALLOW_MAIN_PUSH=1 git push ...(并在 PR/说明记录原因)。


1 !== 0

FAIL M007-002 Git 生效路径: 安装成功但 Git 的 HOME/custom-hooks/pre-push 不存在
FAIL M007-003 脏工作区不得推送: [propose] 本地跑 predeploy 门禁(红则不推)...
[propose] 推 feature 分支 feature/m007 ...
[propose] PR 已存在,已更新:
[propose] 下一步:①独立复核(codex/评审)②合并一律走 ops/160/merge-pr.sh
[propose]   merge-pr.sh 会先等 GitHub Actions predeploy-gates 对本 commit 变绿再 gh pr merge;
[propose]   本仓是私有库免费版设不了 required status check,别再手敲 gh pr merge 绕过这道检查。

FAIL M007-004 查询失败须保留原因并立即退出: [merge] 等了 5 分钟仍没有 aaaaaaaa 的 predeploy-gates run,中止合并。
        可能原因:workflow 被禁用、push 的不是这个 commit、或 Actions 排队异常。
        CI 确实故障时可用 SKIP_CI_WAIT=1 强行合并(会打印警告并需在 PR 说明原因)。

node:internal/modules/run_main:107
    triggerUncaughtException(
    ^

AssertionError [ERR_ASSERTION]: M007 行为回归失败：M007-001 main -> feature/a allow=0、M007-002 Git 生效路径、M007-003 脏工作区不得推送、M007-004 查询失败须保留原因并立即退出
+ actual - expected

+ [
+   'M007-001 main -> feature/a allow=0',
+   'M007-002 Git 生效路径',
+   'M007-003 脏工作区不得推送',
+   'M007-004 查询失败须保留原因并立即退出'
+ ]
- []

    at file:///home/james/complex-management/.scratch/m007-b5-red-7ne1eyp_/tools/gate-hardening-tests/m007-regression.mjs:97:10
    at ModuleJob.run (node:internal/modules/esm/module_job:561:25)
    at async node:internal/modules/esm/loader:647:26
    at async asyncRunEntryPointWithESMLoader (node:internal/modules/run_main:101:5) {
  generatedMessage: false,
  code: 'ERR_ASSERTION',
  actual: [
    'M007-001 main -> feature/a allow=0',
    'M007-002 Git 生效路径',
    'M007-003 脏工作区不得推送',
    'M007-004 查询失败须保留原因并立即退出'
  ],
  expected: [],
  operator: 'deepStrictEqual',
  diff: 'simple'
}

Node.js v24.20.0


~~~~

### 29场景独立边界验证
~~~~text
MERGE stable exit 0 merged True calls 11 watch 2
MERGE invalid exit 1 merged False calls 0 watch 0
MERGE main exit 1 merged False calls 1 watch 0
MERGE dirty exit 1 merged False calls 3 watch 0
MERGE no-remote exit 1 merged False calls 4 watch 0
MERGE mismatch exit 1 merged False calls 4 watch 0
MERGE no-pr exit 1 merged False calls 5 watch 0
MERGE no-runs exit 1 merged False calls 65 watch 0
MERGE red-ci exit 1 merged False calls 8 watch 1
MESSAGE [merge] CI predeploy-gates 未通过(run 101),中止合并。
https://example.invalid/run
MERGE advance exit 1 merged False calls 10 watch 2
MESSAGE [merge] 分支在等待 CI 期间被更新:已验 aaaaaaaa,远端 bbbbbbbb,PR head bbbbbbbb。中止,请对新 commit 重跑本脚本。
MERGE late exit 1 merged False calls 11 watch 2
MESSAGE 
MERGE skip exit 0 merged True calls 8 watch 0
MESSAGE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
!! SKIP_CI_WAIT=1:跳过 CI 绿灯检查,直接合并 aaaaaaaa 到 main !!
!! 治理门禁(predeploy-gates)本次未被确认通过。请在 PR 里写明原因。   !!
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
PROPOSE clean-create exit 0 push True create True CALLS [["git", "rev-parse", "--abbrev-ref", "HEAD"], ["git", "status", "--porcelain"], ["npm", "run", "verify:predeploy"], ["git", "push", "-u", "origin", "feature/audit"], ["gh", "pr", "view", "feature/audit"], ["gh", "pr", "create", "--base", "main", "--head", "feature/audit", "--title", "审计标题", "--body", "审计正文"]]
PROPOSE existing exit 0 push True create False CALLS [["git", "rev-parse", "--abbrev-ref", "HEAD"], ["git", "status", "--porcelain"], ["npm", "run", "verify:predeploy"], ["git", "push", "-u", "origin", "feature/audit"], ["gh", "pr", "view", "feature/audit"], ["gh", "pr", "view", "feature/audit", "--json", "url", "-q", ".url"]]
PROPOSE red-local exit 1 push False create False CALLS [["git", "rev-parse", "--abbrev-ref", "HEAD"], ["git", "status", "--porcelain"], ["npm", "run", "verify:predeploy"]]
PROPOSE push-fail exit 1 push True create False CALLS [["git", "rev-parse", "--abbrev-ref", "HEAD"], ["git", "status", "--porcelain"], ["npm", "run", "verify:predeploy"], ["git", "push", "-u", "origin", "feature/audit"]]
PROPOSE main exit 1 push False create False CALLS [["git", "rev-parse", "--abbrev-ref", "HEAD"]]
PROPOSE dirty exit 1 push False create False CALLS [["git", "rev-parse", "--abbrev-ref", "HEAD"], ["git", "status", "--porcelain"]]
HOOK feature exit 0 INPUT refs/heads/feature/a aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa refs/heads/feature/a bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb MESSAGE 
HOOK to-main exit 1 INPUT refs/heads/feature/a aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa refs/heads/main bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb MESSAGE [pre-push] 拒绝直推 main。请走 PR 流程(ops/160/propose-change.sh)。
[pre-push] 确需直推:ALLOW_MAIN_PUSH=1 git push ...(并在 PR/说明记录原因)。
HOOK main-to-feature exit 0 INPUT refs/heads/main aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa refs/heads/feature/a bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb MESSAGE 
HOOK allow exit 0 INPUT refs/heads/feature/a aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa refs/heads/main bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb MESSAGE [pre-push] ALLOW_MAIN_PUSH=1 覆盖:放行直推 main(已留痕提醒)。
INSTALL default exit 0 git_expected install-default/.git/hooks/pre-push effective_exists True stdout installed pre-push hook -> .git/hooks/pre-push
INSTALL relative exit 0 git_expected install-relative/.custom-hooks/pre-push effective_exists True stdout [install-hooks] 检测到 core.hooksPath=.custom-hooks,安装到该路径(否则钩子不生效)。
installed pre-push hook -> .custom-hooks/pre-push
INSTALL absolute exit 0 git_expected install-absolute/absolute-hooks/pre-push effective_exists True stdout [install-hooks] 检测到 core.hooksPath=/home/james/complex-management/.scratch/m007-b1-9vvfinqv/install-absolute/absolute-hooks,安装到该路径(否则钩子不生效)。
installed pre-push hook -> /home/james/complex-management/.scratch/m007-b1-9vvfinqv/install-absolute/absolute-hooks/pre-push
INSTALL tilde exit 0 git_expected home-tilde/custom-hooks/pre-push effective_exists True stdout [install-hooks] 检测到 core.hooksPath=/home/james/complex-management/.scratch/m007-b1-9vvfinqv/home-tilde/custom-hooks,安装到该路径(否则钩子不生效)。
installed pre-push hook -> /home/james/complex-management/.scratch/m007-b1-9vvfinqv/home-tilde/custom-hooks/pre-push
HARNESS_FINISHED: source copies unchanged; all expected observed outcomes asserted; all current expected behaviors verified after the four fixes.
HOOK_PRESERVED delete-main 1
HOOK_PRESERVED tag-main 0
HOOK_PRESERVED nonliteral-allow 1

~~~~

### CI/依赖/官方help及只读API/最终指纹
~~~~text
WORKFLOW_MIRROR_EQUAL True
YAML_PARSED {"name": "predeploy-gates", "on": {"pull_request": {"branches": ["main"]}, "push": {"branches": ["main"]}}, "jobs": {"predeploy": {"runs-on": "ubuntu-latest", "steps": [{"uses": "actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5", "with": {"fetch-depth": "0"}}, {"uses": "actions/setup-node@49933ea5288caeca8642d1e84afbd3f7d6820020", "with": {"node-version": "22"}}, {"name": "install deps", "run": "npm ci --ignore-scripts"}, {"name": "verify:predeploy (governance gates)", "env": {"HTML_GATES_BASE": "${{ github.event_name == 'pull_request' && github.event.pull_request.base.sha || github.event.before }}"}, "run": "npm run verify:predeploy"}]}}}
PARENT npm run verify:html-ledger-sync && npm run verify:entry-anchors && npm run verify:html-param-tier && npm run verify:delivery-gates-docs && npm run verify:full-admin-matrix && npm run verify:domain-ownership-scope && npm run verify:matrix-vs-migration && npm run verify:data-isolation-spec && npm run verify:tenant-isolation && npm run verify:p0-contract && npm run verify:security-baseline && npm run verify:production-readiness-evidence && npm run verify:production-blockers && npm run verify:pii-encryption && npm run verify:test-db-isolation
PARENT_LINKS 15 MISSING []
HELP ['gh', 'run', 'watch', '--help'] EXIT 0
By default, all steps are displayed. The `--compact` option can be used to only
      --compact        Show only relevant/failed steps
      --exit-status    Exit with non-zero status if run fails
  $ gh run watch --compact
HELP ['gh', 'pr', 'merge', '--help'] EXIT 0
      --match-head-commit SHA   Commit SHA that the pull request head must match to allow merge
GET repos/jxh1979/mall/actions/workflows/predeploy-gates.yml EXIT 0
{"path": ".github/workflows/predeploy-gates.yml", "state": "active", "name": "predeploy-gates"}
GET repos/actions/checkout/git/ref/tags/v4.3.1 EXIT 0
{"object": {"sha": "34e114876b0b11c390a56381ad16ebd13914f8d5", "type": "commit", "url": "https://api.github.com/repos/actions/checkout/git/commits/34e114876b0b11c390a56381ad16ebd13914f8d5"}}
GET repos/actions/setup-node/git/ref/tags/v4.4.0 EXIT 0
{"object": {"sha": "49933ea5288caeca8642d1e84afbd3f7d6820020", "type": "commit", "url": "https://api.github.com/repos/actions/setup-node/git/commits/49933ea5288caeca8642d1e84afbd3f7d6820020"}}
FINAL_SHA docs/business-system-map-for-operations-v0.1.html 304482 a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa
FINAL_SHA docs/business-enum-quick-reference-v0.1.html 133961 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66
FINAL_SHA docs/platform-governance-map-for-operations-v0.1.html 103015 c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c
FINAL_SHA docs/audit-2026-09/plan/criteria-lock.md 889020 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
FINAL_SHA .git/hooks/pre-push 997 46aabdf33cb3496d83a71e4357ca85ba2ed9dbbfd4582ab53747bc50bb748d6b
FINAL_SHA .git/index 520296 ae1dfbf11bec123e6a662097d16de9bb7088fe8bc50d1e9ae4a78da24e90022c
HOOK_CONFIG 1 ''

~~~~

## 8. 已读冻结定义与实际HTML原文核对
### BEQR-085 — 项目开放状态

- 来源：`docs/business-enum-quick-reference-v0.1.html:140`；锚点：`td.k`；父锚点：四、游乐运营
- 适用标签：amusement、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">项目开放状态</td><td>未开放、准备中、开放中、暂停、检修中、已关闭、异常</td></tr>
````

### BEQR-086 — 场次状态

- 来源：`docs/business-enum-quick-reference-v0.1.html:142`；锚点：`td.k`；父锚点：四、游乐运营
- 适用标签：amusement、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">场次状态</td><td>未开始、入场中、已满员（由该项目实时在玩人数自动推算显示，不可人工设置）、进行中、已结束、取消、异常</td></tr>
````

### BEQR-087 — 游乐「当天／每天」口径

- 来源：`docs/business-enum-quick-reference-v0.1.html:144`；锚点：`td.k`；父锚点：四、游乐运营
- 适用标签：amusement、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">游乐「当天／每天」口径</td><td>游乐规则中的「当天」「每天」（含一人多次票当天多次入场、当天不限次数一类表述）一律按营业日计算，不按自然日：当日 10:00 至次日 10:00，起止时点沿用营业日配置（安全阈值，按每个项目、每家酒吧可配，调整须最高权限审批并留痕，须同时配置上下限）；时区一律取项目所在地时间，禁止使用服务器默认时区。</td></tr>
````

### BEQR-088 — 容量双阈值

- 来源：`docs/business-enum-quick-reference-v0.1.html:145`；锚点：`td.k`；父锚点：四、游乐运营
- 适用标签：amusement、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">容量双阈值</td><td>只按单个项目的实时在玩人数控制容量，不再按场次/时段或点位/设备另设容量；排队人数不占用项目容量，排队本身不设人数上限。运营容量（软上限，告警放行）、安全容量（硬上限，阻断+自动停运，不可绕过）；软上限处置方式后台可配：记录异常事件（出厂默认）/仅提示；不影响硬上限阻断。</td></tr>
````

### BEQR-089 — 园区容量

- 来源：`docs/business-enum-quick-reference-v0.1.html:146`；锚点：`td.k`；父锚点：四、游乐运营
- 适用标签：amusement、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">园区容量</td><td>只按单个项目的实时在玩人数控制容量，具体按本表「容量双阈值」条执行。</td></tr>
````

### BEQR-090 — 设备关键/辅助分级

- 来源：`docs/business-enum-quick-reference-v0.1.html:147`；锚点：`td.k`；父锚点：四、游乐运营
- 适用标签：amusement、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">设备关键/辅助分级</td><td>一个游乐项目可以关联多台设备，并区分「关键安全设备」与「辅助设备」。某台设备巡检超期、巡检不合格或发生故障时，按其类别处置：辅助设备只停用该台设备本身，项目是否可以在其余条件下继续运行，由工程岗位或安全岗位判断，不得由当班运营人员自己判断；判断结论必须留痕，记下是谁判的、什么时间判的、依据了哪几项条件，没有留痕的一律按停止运行处理；关键安全设备则要看它与其余关键安全设备之间是否存在依赖关系——不存在依赖关系、且其余关键安全设备均无故障的，只停该台故障设备，项目可继续运行；存在依赖关系的，整个游乐项目停止新增核销并停止运行。是否互相依赖按客人走完这个游乐项目的实际流程来判：这台设备是客人必经的一个环节、少了它这个流程就走不下去的，属于依赖；这台设备有同类的其他设备可以顶替（多台各自独立、可以互相替代），或者这个环节可以绕过去、客人仍能走完整个项目的，属于不依赖。每台关键安全设备属于哪一种，须在设备台账中登记，作为故障时判定停机范围的依据；登记内容变更须经工程或安全审批。项目上线后要把某台关键安全设备改为辅助设备，必须重新经过工程或安全审批，并且只能在该项目停运后生效；普通系统管理员或普通运营人员不得直接降级。</td></tr>
````

### BEQR-091 — 全园故障与补偿通票

- 来源：`docs/business-enum-quick-reference-v0.1.html:148`；锚点：`td.k`；父锚点：四、游乐运营
- 适用标签：amusement、ops、ticketing
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">全园故障与补偿通票</td><td>单台设备或单个游乐项目故障，不直接触发补偿通票。只有整个游乐园发生故障并影响整体履约时，才可以向受影响客人发放补偿通票；补偿对象包括故障发生时已经完成入场核销的客人，也包括尚未核销但对应门票或权益仍在有效期内的持票客人。补偿通票自发放之日起在后台配置的补偿有效期内有效（业务参数，按项目可配，出厂默认一年，须同时配置上下限），且只是把原票的核销权延长到该有效期内使用，不改变原票的核销次数——原票是单次核销的，有效期内任何时间可核销一次，核销后即用尽；家庭票、多人票按原票的人数与次数规则执行；多次票则是把其中某一次权益延长到该有效期内，这一次同样只能核销一次。补偿通票必须关联原票、游乐园整体故障事件和发放原因，防止重复补偿；批量发放前须由游乐公司负责人和工程或安全岗位共同确认。全园故障状态一经生效，必须立即停止所有纯游乐门票和所有包含游乐权益的通票的新销售；局部设备或单个项目故障，才允许在客人付款前明确提示后继续销售。</td></tr>
````

### BEQR-092 — 安全严重度

- 来源：`docs/business-enum-quick-reference-v0.1.html:149`；锚点：`td.k`；父锚点：四、游乐运营
- 适用标签：amusement、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">安全严重度</td><td>提醒、警告、阻断、重大</td></tr>
````

### BEQR-093 — 巡检任务状态

- 来源：`docs/business-enum-quick-reference-v0.1.html:150`；锚点：`td.k`；父锚点：四、游乐运营
- 适用标签：amusement、facility、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">巡检任务状态</td><td>草稿、已提交、通过/警告/阻断、整改中、已复检、等待主管恢复、已恢复、取消</td></tr>
````

### BEQR-094 — 越界类型

- 来源：`docs/business-enum-quick-reference-v0.1.html:151`；锚点：`td.k`；父锚点：四、游乐运营
- 适用标签：amusement、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">越界类型</td><td>物理越界（超空间）、经营范围越界（超合同范围）</td></tr>
````

### BEQR-095 — 团体/包场

- 来源：`docs/business-enum-quick-reference-v0.1.html:153`；锚点：`td.k`；父锚点：四、游乐运营
- 适用标签：amusement、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">团体/包场</td><td>团体（批量预订）、包场（整场/时段，包场期间该游乐项目整体停止对外核销，仅该团体客人可入）</td></tr>
````

### BEQR-096 — 游乐增强阶段

- 来源：`docs/business-enum-quick-reference-v0.1.html:154`；锚点：`td.k`；父锚点：四、游乐运营
- 适用标签：amusement、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">游乐增强阶段</td><td>P1：动态容量。P2：客流预测、自动调度、智能安全预警、游客体验分析。动态容量只在单个项目运营容量（软上限）的已批准配置范围内调整，不新增场次/时段或点位/设备容量维度，不改变安全容量（硬上限）的阻断与自动停运；P2 能力不得绕过核销、巡检、停运、恢复、权限和审批规则，也不得修改实时在玩人数。</td></tr>
````

### BEQR-118 — 运营营业日

- 来源：`docs/business-enum-quick-reference-v0.1.html:187`；锚点：`td.k`；父锚点：六、财务结算与对账
- 适用标签：finance、ops、settlement
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">运营营业日</td><td>全系统的钱账统一按「营业日」归集。营业日从当日上午 10:00 起算，至次日上午 10:00 止；其中次日凌晨 07:00 至上午 10:00 为非营业时段（已闭店），该时段内发生的记录仍归属正在进行的这一个营业日。起止时点由后台配置（安全阈值，按每个项目、每家酒吧可配，调整须最高权限审批并留痕，出厂默认起点为上午 10:00、闭店为次日 07:00，须同时配置上下限）。日结批次、最低消费时段、票价的日期类型（平日/周末/节假日）、场次归属、巡检有效期、考勤日期（跨月、跨周、跨年班次的归属除外，一律按班次结束时点判定，见业务系统地图「请假扣薪」条）、销售提成月份，一律以开台或下单时点所属的营业日为准，不按自然日切分。第三方渠道按自然日提供的汇总数据，由系统按营业日重新归集后再核对，归集差额单独列示。营业日配置的变更只影响未来，不回改历史。</td></tr>
````

### BEQR-146 — 运营公司职责

- 来源：`docs/business-enum-quick-reference-v0.1.html:229`；锚点：`td.k`；父锚点：九、平台治理
- 适用标签：governance、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">运营公司职责</td><td>娱乐平台公司负责酒吧经营域、酒吧通票和跨域组合通票中的酒吧部分，并管理其对独立酒吧商户的参股经营关系；游乐公司负责游乐域；传媒公司负责演出及传媒域；中央餐饮公司负责其供应的酒水、小食和餐饮供应链。独立酒吧商户负责自身经营、人员、设备、单一酒吧门票和店内收入。</td></tr>
````

### BEQR-155 — 商户/运营主体状态

- 来源：`docs/business-enum-quick-reference-v0.1.html:238`；锚点：`td.k`；父锚点：九、平台治理
- 适用标签：governance、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">商户/运营主体状态</td><td>待准入、正常、暂停、终止、归档、黑名单（试运营是商家自己的经营状态，系统不管、不记录）。暂停与列入黑名单由招商部门与运营部门决定；商户退出不是立即生效，一律设缓冲期（业务参数，按项目可配，上下限出厂为三十至六十天，出厂默认三十天）。缓冲期内须向该商户的客人说明情况并给出处理方式：转到其他酒吧、由客人自行取走、或者退款</td></tr>
````

### BEQR-160 — 招商线索状态

- 来源：`docs/business-enum-quick-reference-v0.1.html:248`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：facility、leasing、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">招商线索状态</td><td>待跟进、跟进中、已转意向、洽谈中、已签约、已流失、已搁置</td></tr>
````

### BEQR-161 — 招商意向阶段

- 来源：`docs/business-enum-quick-reference-v0.1.html:249`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：facility、leasing、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">招商意向阶段</td><td>初步接触、需求确认、方案报价、商务谈判、待签约</td></tr>
````

### BEQR-162 — 品牌/客户库类型

- 来源：`docs/business-enum-quick-reference-v0.1.html:250`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：facility、leasing、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">品牌/客户库类型</td><td>意向品牌、储备品牌、合作品牌、黑名单品牌</td></tr>
````

### BEQR-163 — 合同变更类型

- 来源：`docs/business-enum-quick-reference-v0.1.html:251`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：contract、facility、leasing、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">合同变更类型</td><td>续租、退租、面积变更、租金调整、经营范围变更、提前终止、经营范围授权（超范围下单申请经审批放行形成的授权记录）、位置变更（换铺位／位置调整子合同，与面积变更并列）（不支持转租，需换租户先退租再重新招商）</td></tr>
````

### BEQR-164 — 退租结算状态

- 来源：`docs/business-enum-quick-reference-v0.1.html:252`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：facility、leasing、ops、settlement
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">退租结算状态</td><td>待结算、押金抵扣中、欠费追缴中、已结清、有争议、「有争议，诉讼/仲裁」；退租 / 提前终止 / 合同到期三种终止统一走多级审批（财务、运营等多部门），审批全部通过后系统自动落账；押金退还款项以退租结算算账完成为准执行支付，且不早于等待期届满（等待期为业务参数，按项目可配，出厂默认 30 日，须同时配置上下限）；等待期用于让退租日之前按期出账的能耗费、物业费等全部形成应收并从押金中结清。押金退还支付不受三个付账日与账单异议期约束，商户对退租结算金额的异议在退租审批流程中提出并解决；算账晚于等待期完成的，顺延至算账完成之日支付，顺延原因与预计时间在商户专区对商户可见；同一合同结算只算一次、不重复出账</td></tr>
````

### BEQR-165 — 内部应付执行状态

- 来源：`docs/business-enum-quick-reference-v0.1.html:253`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：facility、leasing、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">内部应付执行状态</td><td>待付（含已发起、尚未确认到账且未收到失败结果的银行在途，在途不重付）、付款失败待处理、已结清（见业务系统地图付款执行失败条款；失败不属三种顺延，重付不迟于下一个付账日）</td></tr>
````

### BEQR-166 — 合同审批链

- 来源：`docs/business-enum-quick-reference-v0.1.html:255`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：contract、facility、governance、leasing、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">合同审批链</td><td>按新建、到期终止、变更三类分别配置流程模板，每类均按全平台统一的审批模型执行：发起时指定这条主线的最终审批人（默认总经理），系统按组织层级自动补全从起草人到该最终审批人之间的全部直接上级节点并逐级审批；业务审核、财务审核、法务审核作为平行的相关部门线，各指定该部门的最终审批人并同样逐级自动补全至该部门预设的最低起点层级；主线与各条平行线都通过才生效；阶段数与审批岗位按综合体可配，各类的阶段数与审批岗位可以不同；三类都不得录入即生效——新建未审批不生效，到期不得无审批自动终止，变更未审批不生效；任一节点驳回则整单退回起草</td></tr>
````

### BEQR-167 — 租金/账单类型

- 来源：`docs/business-enum-quick-reference-v0.1.html:256`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：facility、leasing、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">租金/账单类型</td><td>固定租金、每平米租金、营业额抽成租金、物业费、能源费、电损费、水损费、会员系统服务费、积分发行费、押金、违约金</td></tr>
````

### BEQR-168 — 营业额抽成模式

- 来源：`docs/business-enum-quick-reference-v0.1.html:257`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：facility、leasing、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">营业额抽成模式</td><td>保底与抽成取高、固定比例、阶梯比例、保底加抽成叠加。营业额口径全平台统一、不得逐户另议：营业额＝该商户当期在本综合体内含税实收成交总额，减去当期已成功的退款金额，减去由该商户以外的出资方承担的赠送与补贴；不论收款走平台还是走商户自有渠道、不论线上线下均计入；商户代收代付的第三方款项不计入；按开台或下单时点所属的营业日归属账期；该账期发生商品退款的，在退款发生的当期生成冲正，不回改历史月份；冲正金额一律按该笔款项在其原账期实际计费时所适用的档位与费率原额冲回，不按冲正发生当期的档位与费率重算，原账期已定格的账单与档位不因冲正而重算；多笔退款逐笔分别冲正，不合并折算。凡已计收过门票综合服务费的部分——包括通票或券核销划归本商户的部分，以及单店门票的票款——一律不再计入营业额抽成基数，同一笔钱不两头收费；未计收服务费的商户（餐饮、零售等），其通票或券核销划归本商户并已完成结算的部分计入。营业额抽成账单在该账期的薪资月结完成、该账期月结定格时确定的服务费应发数与扣留数定数之后出账，账单一次出准，不使用预估数、不在下期补差；出账日与付账日由后台配置（业务参数，按项目可配，须同时配置上下限），不预设具体日期；具体落在哪一个付账日仍由财务人员按业务系统地图的付账日归属规则指定；所指定的付账日在某一账期不满足「月结定格 → 账单出账 → 异议期届满 → 付账日」顺序的，该笔款项顺延至该账期内下一个满足该顺序的付账日，本账期三个付账日均不满足的，顺延至下一账期的第一个满足该顺序的付账日。营业额抽成账单的异议期长度为业务参数（按项目可配，须同时配置上下限），自该账单在商户专区可查看之日的次日起算；仅当商户专区在项目时区的一个完整自然日内全天不可用时，该自然日才不计入异议期；部分时段不可用不顺延。阶梯比例一律超额累进（每一档只对超出该档起点的部分适用该档费率）；“保底与抽成取高”按自然月逐月比较、不跨月合并。第三方收银未按约定回流的，按合同保底额与该商户在后台配置的参照期内（业务参数，按合同逐份可配，出厂默认近三个月，须同时配置上下限）最高月营业额孰高计租，并按合同计违约金；所用参照期随合同版本固化在账单上。</td></tr>
````

### BEQR-169 — 公区能源费

- 来源：`docs/business-enum-quick-reference-v0.1.html:258`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：energy、facility、leasing、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">公区能源费</td><td>不再向商户单独分摊。公区能源及其他公区公摊费用统一包含在物业费中，按每平米固定单价随物业费一并计收，不另行生成公区能源费账单。</td></tr>
````

### BEQR-170 — 铺位租赁状态

- 来源：`docs/business-enum-quick-reference-v0.1.html:259`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：facility、floorplan、leasing、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">铺位租赁状态</td><td>空置、洽谈中、已租、即将到期、已退租、不可租</td></tr>
````

### BEQR-171 — 物业工单类型

- 来源：`docs/business-enum-quick-reference-v0.1.html:260`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：facility、leasing、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">物业工单类型</td><td>报修、保洁、投诉、整改、巡检、内部协作工单（内部部门间发起与流转）、其他服务请求</td></tr>
````

### BEQR-172 — 物业工单状态

- 来源：`docs/business-enum-quick-reference-v0.1.html:262`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：facility、leasing、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">物业工单状态</td><td>待派单、已派单、处理中、待回访、待验收、已验收、已关闭、已挂起、已超时。回访／验收按工单类型分支适用，不是全类都走同一条顺序链：保洁等无争议类由处理人完成即结；投诉分为商户投诉和消费客人投诉：商户投诉须由商户确认投诉已解决；按既有规则无法解决，或问题已解决但商户未确认解决或未点击关闭的，客户关怀部记录相关信息及关闭缘由后，可以关闭投诉。消费客人的投诉，由客户关怀部记录问题解决方案及客人反馈后，可以关闭；复杂类（大型设备报修等）须主管验收＋提单方确认双闸。</td></tr>
````

### BEQR-173 — 商户服务工单状态

- 来源：`docs/business-enum-quick-reference-v0.1.html:264`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：facility、leasing、merchantservice、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">商户服务工单状态</td><td>已提交、已派单、处理中、待商户确认、已处理待关闭、已关闭、已取消（商户专区发起；超过时限自动升级并标记）</td></tr>
````

### BEQR-174 — 工单时效

- 来源：`docs/business-enum-quick-reference-v0.1.html:265`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：facility、leasing、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">工单时效</td><td>响应时限、处理时限；超时自动升级通知主管。看板上设施工单与商户服务工单分行对照；商户单没有「首次响应」环节，不显示响应指标</td></tr>
````

### BEQR-175 — 楼宇机电设备状态

- 来源：`docs/business-enum-quick-reference-v0.1.html:266`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：facility、leasing、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">楼宇机电设备状态</td><td>正常、告警、故障、维保中、停用、报废（区别于员工工作设备的领用/归还台账）</td></tr>
````

### BEQR-176 — 设备维保类型

- 来源：`docs/business-enum-quick-reference-v0.1.html:267`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：facility、leasing、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">设备维保类型</td><td>定期保养、例行巡检、故障维修、安全检测、部件更换</td></tr>
````

### BEQR-177 — 能耗表计类型

- 来源：`docs/business-enum-quick-reference-v0.1.html:268`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：facility、leasing、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">能耗表计类型</td><td>电表、水表、燃气表、冷/热量表</td></tr>
````

### BEQR-178 — 抄表状态

- 来源：`docs/business-enum-quick-reference-v0.1.html:269`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：facility、leasing、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">抄表状态</td><td>待抄、已抄录、已审核、异常（突增、突降、零读数、读数倒走）</td></tr>
````

### BEQR-179 — 能耗归集对象

- 来源：`docs/business-enum-quick-reference-v0.1.html:270`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：facility、leasing、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">能耗归集对象</td><td>公共区域、商户、设备/回路</td></tr>
````

### BEQR-180 — 楼宇巡检任务状态

- 来源：`docs/business-enum-quick-reference-v0.1.html:271`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：facility、leasing、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">楼宇巡检任务状态</td><td>待执行、执行中、正常、发现异常、整改中、已复检、已关闭（区别于游乐设备安全巡检）</td></tr>
````

### BEQR-181 — 楼宇巡检类型

- 来源：`docs/business-enum-quick-reference-v0.1.html:273`；锚点：`td.k`；父锚点：十、地产运营（招商 · 租赁 · 物业 · 设备 · 能耗 · 巡检）
- 适用标签：facility、leasing、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
    <tr><td class="k">楼宇巡检类型</td><td>安防、消防、设施设备、环境卫生，共四类。能耗抄表不属于楼宇巡检类型，走能耗域的独立抄表流程（与《平台治理地图》能耗口径一致）。</td></tr>
````

### BSM-020 — 楼宇运营

- 来源：`docs/business-system-map-for-operations-v0.1.html:656`；锚点：`h3`；父锚点：子系统入口
- 适用标签：ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
              <div class="card-top"><h3>楼宇运营</h3><span class="tag support">地产</span></div>
              <p>楼宇机电设备台账与维保、能耗表计抄表与归集、楼宇安防消防设施巡检；设备告警与巡检异常自动转工单。</p>
              <span class="open-link">查看说明</span>
            </a>
            <a class="system-card" href="#thirdparty">
````

### BSM-028 — 主要功能

- 来源：`docs/business-system-map-for-operations-v0.1.html:698`；锚点：`h3`；父锚点：基础管理：先把综合体的盘子管清楚
- 适用标签：access、amusement、bar、contract、customer、evidence、facility、finance、floorplan、governance、integration、inventory、leasing、ops、partner、settlement、ticketing
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
              <h3>主要功能</h3>
              <ul>
                <li>建立综合体项目、楼层、区域、点位；当前一个综合体，未来可扩展为多综合体。</li>
                <li>建立商户档案，区分自营、租赁、联营、代运营、服务商接入、其他这六类合作方式。</li>
                <li>物理项目与运营公司是多对多关系，不是固定的“平台→项目公司→域主管公司→商户”隶属链。单一经营域的规则、定价和结算由对应域主管公司负责，跨域规则由项目之上的平台权限审批。</li>
                <li>每个城市项目公司同时承担本城市项目的资产职责，只负责项目资产、场地、物业、租赁、直接管理的第三方商户以及自身人员和设备；它不管理酒吧、游乐、娱乐平台公司、传媒公司、中央餐饮公司及其人员、设备、定价、票务和结算。</li>
                <li>娱乐平台公司负责酒吧经营域、酒吧通票、跨域组合通票中的酒吧部分以及其对独立酒吧商户的参股经营关系；游乐公司负责游乐域；传媒公司负责演出和传媒域；中央餐饮公司负责其供应的酒水、小食和餐饮供应链。独立酒吧商户负责自己的经营、人员、设备、单一酒吧门票和店内收入。</li>
                <li>管理合同、租赁面积、合作期限、经营范围、结算口径摘要和核心条款摘要（招商、签约与合同履约详见“招商租赁与合同履约”专章）。</li>
                <li>合同变更走审批流，分续租、退租、面积变更、租金调整、经营范围变更、提前终止、经营范围授权（超范围下单申请经审批放行形成的授权记录）、位置变更（换铺位／位置调整子合同，与面积变更并列）八类；不支持转租，需退租后重新招商。</li>
                <li>把合同和基础定义变成准入规则：不符合合同或基础定义的后续流程必须先被卡住，再按审批链逐级审批放行——发起单据时只需指定该事项的最终审批人，系统按组织层级自动补全从发起人到该最终审批人之间的全部直接上级节点；需要平行线审批的（如法务、财务），另行指定该条线的最终审批人，层级同样自动补全。但下列七类是红线，任何层级的审批都不得绕过：①项目坐满即不再放人进；②当天没有做过合格巡检的设施不得开放；③未满 18 岁一律拒入、拒售酒；④看不到别的商户和别的项目的数据；⑤同一笔操作重复提交不重复生效；⑥分出去的每一分加起来必须等于收进来的，对不上就不让过账。守恒校验不通过的当日即会显现，不得跨日；财务须立即追查，涉及的相关工作人员一同追查；必须逐笔拆解、细算重算，一律走正向更正，不得以手工冲账的方式调平；⑦普通退款只限票种规则明确允许、活动或场次取消、平台或履约方无法提供服务、重复扣款、法律法规要求这五种情形；通票安全例外是客人出现重大情绪不稳定、安全问题或其他必要问题，经夜班值班经理及以上批准免除其通票费用，即使已核销也可退款。持授权 U 盘密钥的最高权限只能逐笔放行五种普通退款理由之外的未核销票，不解除已开始核销后的退款禁止。其中第⑤⑥⑦类，持授权 U 盘密钥的最高权限可以逐笔开放，每次只能对当前这一笔放行、不得整类关闭校验；第①②③类（项目坐满即不再放人进、当天没有做过合格巡检的设施不得开放、未满 18 岁一律拒入拒售酒）连最高权限持密钥也不得开放，系统不提供开放入口；第④类看不到别的商户和别的项目的数据，持授权 U 盘密钥的最高权限可以逐笔开放，每次只对当前这一笔生效并留痕。上述七类校验不设关闭开关：不得通过全局、综合体、商户、业务系统、模块、版本/设备、灰度中的任何一档关闭这七类校验。审批不得绕过与开关不得关闭，两条并列适用。</li>
                <li>合同里可以勾选这家商户被允许经营的品类；商户下单如果超出合同约定的经营范围，系统在下单当场就拦下，并提示该商户提交申请；申请要填写原因，提交后进入审批，审批通过才放行这笔、驳回则不予下单。</li>
                <li>管理员工、岗位、班次、职级、权限、工作设备和员工注册审批。</li>
                <li>支持票务管理、酒吧管理、游乐设施管理拆分为可独立部署的系统：拆出去单独运营时各自独立、互不相关、不依赖基础系统；在综合体内则统一接入基础平台，按归属、合同和权限管理。</li>
              </ul>
            </div>
            <div class="info-block">
````

### BSM-081 — 楼宇运营：机电设备、能耗抄表、楼宇巡检

- 来源：`docs/business-system-map-for-operations-v0.1.html:1401`；锚点：`h2`
- 适用标签：facility、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
          <h2>楼宇运营：机电设备、能耗抄表、楼宇巡检</h2>
          <p class="lead">物业不只是处理报修工单，还要让楼宇的机电设备、能耗和安全巡检都在线可控。设备有台账和维保，能耗有表计和抄表，巡检有任务和复检；三者发现的问题都能自动转成物业工单。</p>
          <div class="detail-grid">
            <div class="info-block">
````

### BSM-082 — 主要功能

- 来源：`docs/business-system-map-for-operations-v0.1.html:1405`；锚点：`h3`；父锚点：楼宇运营：机电设备、能耗抄表、楼宇巡检
- 适用标签：facility、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
              <h3>主要功能</h3>
              <ul>
                <li>机电设备：建立楼宇机电设备台账，状态分正常、告警、故障、维保中、停用、报废（区别于员工领用/归还的工作设备台账）；维保分定期保养、例行巡检、故障维修、安全检测、部件更换。</li>
                <li>能耗抄表：维护表计（电表、水表、燃气表、冷/热量表），抄表状态分待抄、已抄录、已审核、异常（突增、突降、零读数、读数倒走）；每次抄录必须同时上传表底照片，系统自动与上期读数比对；判为异常的读数一律不得直接出账，须由第二人复抄或到场核表，核实后填写原因并经主管确认才转为已审核，核实完成之前该商户当期能源费账单暂缓生成、暂缓催缴；核实须在限期内出结果、不得无限期暂缓（业务参数，按项目可配，出厂默认一周，须同时配置上下限）；异常读数被更正的，保留原始录入值与更正记录，两者都可查。能耗按公共区域、商户、设备/回路归集：公区能耗仍按公共区域归集，用于成本分析与能耗管理，但不形成对商户的公区能源费应收；商户能源费按其独立表计的实际用量与实际费用代收、一律不得加价，并记为代收代缴往来、不作为城市项目公司的经营收入。进楼总表用量减去各商户分表用量之和、再减去独立计量的设备回路用量所形成的差额（含线损、无表点位与表计误差），通过电损费与水损费按各商户实际用电度数、用水吨数另行独立计收，作为独立服务费项目单列并单独开具服务发票；电损费与水损费按后台配置的固定金额或比例，以服务费的形式向商户单独计收、单独开具服务发票（业务参数，按项目可配，须同时配置上下限）。</li>
                <!-- 2026-08-04 第三轮用户裁决（DEC-R6-12）补漏落点，R6-T1 收口 -->
                <li>楼宇巡检：巡检任务状态分待执行、执行中、正常、发现异常、整改中、已复检、已关闭（区别于游乐设备安全巡检）；类型分安防、消防、设施设备、环境卫生，共四类；能耗抄表走能耗域独立流程、不在巡检里重复抄。</li>
              </ul>
            </div>
            <div class="info-block">
````

### BSM-083 — 和谁有关

- 来源：`docs/business-system-map-for-operations-v0.1.html:1414`；锚点：`h3`；父锚点：楼宇运营：机电设备、能耗抄表、楼宇巡检
- 适用标签：facility、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
              <h3>和谁有关</h3>
              <ul>
                <li>连接物业服务，设备告警与巡检异常自动生成物业工单，派单到人、跟踪到验收。</li>
                <li>连接招商租赁与合同履约，商户能源费按归集与抄表形成结算依据；公区公摊费用已含在物业费的每平米单价内，不再单列结算依据。</li>
                <li>连接财务对账，能耗与维保成本进入结算与经营复盘。</li>
                <li>连接经营看板，能耗与楼宇设备告警、巡检与工单时效是楼宇运营指标。</li>
              </ul>
            </div>
          </div>
        </section>

        <!-- 2026-07-03 回写：员工自有手机端(注册/公告/工单/代订+排班+NFC考勤严格口径+请假+月结薪资)。 -->
        <section id="employee-phone">
````

### BSM-111 — 经营看板：让管理层和运营看得懂、能追问

- 来源：`docs/business-system-map-for-operations-v0.1.html:1781`；锚点：`h2`
- 适用标签：analytics、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
          <h2>经营看板：让管理层和运营看得懂、能追问</h2>
          <p class="lead">看板不是为了好看，而是为了判断综合体经营情况。它要能从总览下钻到票务、酒吧、供应链库存、游乐、商户、异常和对账明细。</p>
          <div class="detail-grid">
            <div class="info-block">
````

### BSM-112 — 主要指标

- 来源：`docs/business-system-map-for-operations-v0.1.html:1785`；锚点：`h3`；父锚点：经营看板：让管理层和运营看得懂、能追问
- 适用标签：analytics、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
              <h3>主要指标</h3>
              <ul>
                <li>收入：售票收入、酒吧收入、第三方回流收入、退款、净收入。</li>
                <li>客流：入场人数、酒吧入场、游乐核销人数、渠道客流。</li>
                <li>核销：权益发放数、核销数、核销率、拒绝数。</li>
                <li>会员：新增会员、积分发放、积分兑换、会员等级分布。</li>
                <li>供应链：采购到货、库存余额、吧台消耗、缺货预警、盘点差异、存酒余额。</li>
                <li>风险：待复核、异常事件、补录数量、第三方差异。</li>
                <li>财务：日结状态、对账差异、结算待确认。</li>
                <!-- 2026-06-24 地产全链授权扩展：经营看板补地产经营指标。 -->
                <li>地产：招商线索与成交、铺位出租率、租金与物业费应收、能耗与楼宇设备告警、物业工单时效。</li>
                <!-- 2026-07-03 回写：经营分析已上线能力(欠费账龄/出租率/工单时效双源对照/月度对比/节假日对比/异常检测/导出分权/商户汇总)。 -->
                <li>欠费：欠了多少、欠了多久（按账龄分档）、本月该收多少实际收回多少。</li>
                <li>出租：铺位出租率＝已租铺位面积 ÷ 本项目拥有的铺位总面积（总面积包含空置、洽谈中、已租、即将到期、已退租、不可租各种状态，不因状态变化而增减）；车位出租率单独列、已签约还没交付的单独列，不混在一起。</li>
                <li>工单时效：设施工单和商户工单分行对照——响应快不快、处理拖不拖、超时率、积压多少；商户工单没有「首次响应」环节，就不显示响应指标，不拿别的数字冒充。</li>
                <li>月度对比：本月对上月、对去年同月；数据积累不够就标「积累中」，不假造可比数。</li>
                <li>节假日对比：今年春节对去年春节，总额和日均两列一起看；节假日日历可给网址自动抓取，也可手工维护。</li>
                <li>异常提醒：经营指标环比突变自动报出来，灵敏度可调，节假日自动免报，避免误报刷屏。</li>
                <li>异常处置闭环：报出来的经营异常可以走处置流转——负责人标「已阅」「已处置」「误报」，或直接「转风险告警」升级；每一步都记下谁在什么时候、怎么处理的，形成可追溯的闭环，不是报完就没下文。</li>
                <li>异常预警入口：经营看板显示异常预警的入口与未处理条数计数卡片，点击进入异常预警页面；看板上的计数只是入口提示，预警的查看、转办与处置仍在异常预警页面完成。</li>
                <li>商户汇总：各商户经营情况汇总对比。</li>
                <!-- 2026-07-03 回写:T3-A 只读经营分析视图重建后上线。 -->
                <li>只读分析视图：商户健康分按投诉与履约情况计算：商户违反运营规则的，由城市项目公司的运营人员填写处罚记录，处罚即扣减健康分，扣分规则由后台配置（业务参数，按项目可配，须同时配置上下限）；健康分用于给予商户额外优惠，不作为处置依据；连续违反运营规则的由运营部门另行决定暂停或列入黑名单；具体计分与优惠规则待统一的商场运营规则出台后另定。把机器人运行、商户健康分、员工服务分、客户意向、指标定义与指标快照汇总成一个「只能看、不能操作」的分析屏，发不出任何控制指令，适合只读或审计账号安全查看；某一类数据没权限时只这一类不显示，不影响看其他。</li>
                <li>卡片明细对账：总控上每一张汇总卡片都能点开看明细——卡片上的数字就是它下面明细的合计，点进去能核到每一笔，数字对不上一眼看得出。</li>
                <li>看板按权限看，但把数据导出带走须单独授权并留痕（见平台治理地图）。</li>
              </ul>
            </div>
            <div class="info-block">
````

### BSM-113 — 谁会看

- 来源：`docs/business-system-map-for-operations-v0.1.html:1813`；锚点：`h3`；父锚点：经营看板：让管理层和运营看得懂、能追问
- 适用标签：analytics、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
              <h3>谁会看</h3>
              <ul>
                <li>管理层看整体收入、客流、风险、商户表现。</li>
                <li>运营看今日运行、核销、异常和第三方回流。</li>
                <li>财务看退款、差异、日结和结算依据。</li>
                <li>票务、酒吧、游乐负责人看各自业务表现。</li>
                <li>采购、库管和酒吧店长看采购周期、库存消耗、存酒和盘点差异。</li>
                <li>KPI 按管理层级分口径：平台级管理者看跨项目汇总的平台口径 KPI；项目级操作人员只看自己所在项目范围内的 KPI 和明细，看不到别的项目。</li>
              </ul>
            </div>
          </div>
        </section>

        <section id="terminals">
````

### BSM-114 — 各类入口：不同人只看自己该看的、只做自己该做的

- 来源：`docs/business-system-map-for-operations-v0.1.html:1827`；锚点：`h2`
- 适用标签：access、amusement、analytics、bar、contract、customer、evidence、facility、finance、governance、inventory、merchantportal、notification、ops、settlement、terminal、ticketing
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
          <h2>各类入口：不同人只看自己该看的、只做自己该做的</h2>
          <table class="plain-table">
            <thead>
              <tr>
                <th>入口</th>
                <th>谁使用</th>
                <th>主要做什么</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>管理后台</td>
                <td>管理层、运营、财务、系统管理员</td>
                <td>配置、查询、复核、对账、看板。</td>
              </tr>
              <tr>
                <td>Web 前端</td>
                <td>业务人员、管理层、运营、财务、部分商户人员</td>
                <td>查看经营、票务、酒吧、供应链库存、游乐、异常和对账信息，并允许少量业务确认动作。使用 Web 端必须打开地理识别，没有打开地理识别的无法正常使用。Web 端同样适用：核销、收款、退款、放行等敏感操作在授权位置范围之外一律拒绝并留痕。</td>
              </tr>
              <tr>
                <td>商户侧页面</td>
                <td>餐饮、零售、酒吧等商户</td>
                <td>在授权业务范围内全开放，处理本商户经营、供应/库存、对账、结算、员工和看板，不看平台全局。</td>
              </tr>
              <tr>
                <td>商户 App</td>
                <td>餐饮、零售、酒吧等商户</td>
                <td>商户侧的手机端，在授权业务范围内移动处理本商户经营、对账、结算和看板，不看平台全局。</td>
              </tr>
              <tr>
                <td>现场作业端/员工工作手机</td>
                <td>现场员工、服务员、游乐入口员工、巡检</td>
                <td>上岗、领设备、扫二维码核销门票和权益、补录、巡检、库房和酒水交接扫码。</td>
              </tr>
              <tr>
                <td>员工私人手机 App</td>
                <td>全体员工</td>
                <td>注册、提交资料审批、收公告通知、查自己的工单、帮客人订位、看排班、考勤打卡、请假、查薪资等可离场办理的事；必须在现场完成的操作（代客下单、出示公司收款码、完成履约、扫码核销）在公司工作手机上做；私人手机不允许带入工作现场，进办公区需寄存。</td>
              </tr>
              <tr>
                <td>工作人员平板 App</td>
                <td>酒吧现场工作人员、服务员</td>
                <td>只做酒吧点酒下单，绑定酒吧和卡座，校验最低消费，支持扫码支付。</td>
              </tr>
              <tr>
                <td>触摸屏一体机</td>
                <td>客人、现场工作人员</td>
                <td>扫码查看和领取权益，也可做订台下单、吧台买酒下单、游乐设施状态查看、定期检修记录和主管确认；启用售票能力的一体机可由授权服务人员辅助到店购票和订单查询。</td>
              </tr>
              <tr>
                <td>移动管理端</td>
                <td>管理层、运营主管</td>
                <td>看关键指标、处理待办、审批异常和补录。</td>
              </tr>
              <tr>
                <td>客人 App/小程序</td>
                <td>游客、会员、潜在客户</td>
                <td>官方购票、预定/订台、查订单、查权益、看积分摘要、出示验票二维码；礼物兑换。</td>
              </tr>
              <tr>
                <td>经销商 Web 门户</td>
                <td>企业资料、合同、开票和银行资料审核通过的外部经销商</td>
                <td>在合同和授权数据范围内查看合同销售价格目录、商品供货状态及预计交期，发起询价和订货，查看订单审核、付款核验、配送、签收、退货、红冲和电子发票状态；账号、权限和数据与商户门户及内部员工端隔离。</td>
              </tr>
            </tbody>
          </table>
        </section>

        <section id="relations">
````

### BSM-115 — 相互关系：为什么这些子系统必须连在一起

- 来源：`docs/business-system-map-for-operations-v0.1.html:1897`；锚点：`h2`
- 适用标签：access、amusement、analytics、bar、contract、customer、evidence、facility、finance、governance、integration、inventory、ops、settlement、terminal、ticketing
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
          <h2>相互关系：为什么这些子系统必须连在一起</h2>
          <table class="plain-table">
            <thead><tr><th style="width:150px">连接</th><th>为什么必须连</th></tr></thead>
            <tbody>
              <tr><th scope="row">基础管理连所有业务</th><td>因为票务、酒吧、游乐、商户、员工、设备都必须知道自己属于哪个综合体、哪个项目、哪个空间、哪个商户、由谁负责；如果合同或基础定义不允许，后续流程必须先被卡住，再按配置的更高级审核层级放行。</td></tr>
              <tr><th scope="row">票码连防伪核对</th><td>因为门票二维码不能只是普通图片，必须有防伪标记、有效期，并能防止被复制和篡改，现场防变造，事后核对时能按订单、票号、员工、设备、点位核对。</td></tr>
              <tr><th scope="row">票务连酒吧</th><td>因为客人自己购买的门票或套餐里可能送酒水，酒吧必须知道这个赠饮权益是否有效、是否已经核销。</td></tr>
              <tr><th scope="row">供应链连酒吧</th><td>因为酒吧吧台卖酒、按杯出品、按瓶出品、赠饮、兑换、报损和客人存酒，都会改变酒水数量和库存状态。</td></tr>
              <tr><th scope="row">供应链连基础管理</th><td>因为供应商、供应合同、库房、库位、采购人员、库管和审批权限都要先在基础管理中定义清楚。</td></tr>
              <tr><th scope="row">供应链连票务权益</th><td>因为门票赠饮和其他权益领取可能需要扣减酒水、礼品或消耗品库存；积分兑换的实物券发券时预占库存、核销领取时才实扣。</td></tr>
              <tr><th scope="row">供应链连财务</th><td>因为采购成本、库存消耗、报损、盘点差异、存酒余额和供应合同，会影响日结、成本判断和经营复盘。</td></tr>
              <tr><th scope="row">经销商门户连合同价格、供应链与财务</th><td>因为经销商只能按已生效合同价格或审批后的询价结果下单，外部库存只显示有货、紧张、缺货和预计交期；订单必须完成合同、价格、库存和交期审核，到账核验通过后才能出库，退货、退款、红冲和电子发票状态必须与供应链及财务记录一致。</td></tr>
              <tr><th scope="row">供应链连留痕</th><td>因为高价值酒水报损、盘点差异、存酒作废、异常调拨都可能产生责任争议，必须记录原因、员工、时间、点位、照片、视频和主管复核结果。</td></tr>
              <tr><th scope="row">票务连游乐</th><td>因为通票里可能包含游乐项目，游乐入口要判断客人能不能进、项目容量（实时在玩人数）够不够。</td></tr>
              <tr><th scope="row">平板连酒吧</th><td>因为平板只负责酒吧点酒下单，必须绑定具体酒吧和卡座，并按时间、卡座和位置校验最低消费后才能提交。</td></tr>
              <tr><th scope="row">工作手机连留痕</th><td>因为门票核销、赠饮领取、权益领取、企业微信沟通、补录、巡检、设备异常、订单销售归属、卡座服务责任、异常第一处理人都可能影响责任和服务质量，必须留下记录。</td></tr>
              <tr><th scope="row">工作手机连设备管控</th><td>因为正式发放的受管工作手机只能安装预装软件，只能访问白名单网站，并且工作沟通必须使用企业微信，设备策略和违规访问也要进入后台审计。</td></tr>
              <tr><th scope="row">第三方连财务</th><td>因为美团、大众点评、抖音、餐饮、零售、支付渠道等外部系统产生的订单、退款和核销结果，也要进入平台统一对账。</td></tr>
              <tr><th scope="row">票务、消费连会员积分</th><td>票务系统提交票务订单、支付、履约、退款、冲正和核销事实，会员与等级系统、积分系统分别按票务规则计算；商户消费才按商户所属业态规则计算。App 和小程序展示统一结果，兑换和库存由积分与供应链系统承载。</td></tr>
              <tr><th scope="row">一体机连票务</th><td>因为少量到店服务场景需要授权服务人员在售票一体机上辅助购票；一体机必须绑定点位、限制可售票种，并记录服务人员、设备、点位、购票原因、支付和出票。</td></tr>
              <tr><th scope="row">一体机连权益</th><td>因为客人领取赠饮、礼物或其他权益时，需要扫码后在一体机上清楚看到能领什么、能领多少、是否已经领取，并把领取结果回写到权益记录。</td></tr>
              <tr><th scope="row">一体机连酒吧</th><td>因为一体机可以做订台和吧台买酒下单，必须把卡座、桌台、吧台位、商品、金额、支付和服务责任写回酒吧账单。</td></tr>
              <tr><th scope="row">一体机连游乐</th><td>因为客人和员工需要在现场触摸屏上看到游乐设施是否开放、是否排队、是否暂停或故障，工程师的定期检修和主管的恢复营业确认也要进入游乐异常和巡检记录。</td></tr>
              <tr><th scope="row">风控连财务</th><td>因为退款、免单、调账、差异处理如果没有复核和原因，财务不能放心确认。</td></tr>
              <tr><th scope="row">财务连看板</th><td>因为管理层不能只看销售数字，还要知道这些数字是否已日结、是否有差异、是否能结算。</td></tr>
              <tr><th scope="row">看板连各入口</th><td>因为管理层、运营、财务、商户看到的是同一套经营事实，只是权限和展示范围不同。</td></tr>
            </tbody>
          </table>
        </section>

        <section id="delivery">
````

### BSM-116 — 系统交付与上线：正式上线要做的安全加固和安装交付

- 来源：`docs/business-system-map-for-operations-v0.1.html:1928`；锚点：`h2`
- 适用标签：compliance、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
          <h2>系统交付与上线：正式上线要做的安全加固和安装交付</h2>
          <p class="lead">系统从打包测试环境正式上线为生产环境，是一次要专门把关的动作。上线不是把测试那套原样搬过去，而是要完成一批只在生产环境才做的安全加固；同时，交付给现场的安装要让不懂技术的人也能照着装、照着用。</p>
          <div class="detail-grid">
            <div class="info-block">
````

### BSM-117 — 正式上线时的安全加固

- 来源：`docs/business-system-map-for-operations-v0.1.html:1932`；锚点：`h3`；父锚点：系统交付与上线：正式上线要做的安全加固和安装交付
- 适用标签：compliance、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
              <h3>正式上线时的安全加固</h3>
              <ul>
                <li>上线生产必须换掉开发默认的弱口令：系统数据的总管钥匙（最高权限口令）在测试阶段用的是方便调试的默认弱密码，正式上线为生产环境时，必须换成强密码，这是上线生产的必做一步、不可省略。</li>
                <li>这把最高权限强密码用 U 盘密钥托管方式设定和保管：密钥母本只在授权 U 盘里，安装或更换密钥时插入授权 U 盘进行授权，但是该机器不保留密钥，运行时不依赖 U 盘、断电重启无需插盘；破窗登录（应急直接登录）以及手动清理留存数据等最高权限动作，要插入授权 U 盘才能进行。持有授权 U 盘密钥的人才是最高权限；最高权限全平台只设一人，其权限不再另设审批环节，由密钥的物理保管来把关。U 盘由指定负责人保管，做到"人机分离、钥匙不落地"。钥匙U盘丢失、损坏或保管人变动时，按平台治理地图「紧急恢复（破窗机制）」的钥匙更换闭环处理：旧钥匙先作废、新钥匙当面验证可用后启用，全程留痕并事后复盘。</li>
                <li>上线加固要留痕：换密码、启用 U 盘托管、每一次插盘托管或更换密钥、每一次破窗登录，都要记录谁在什么时间做了什么，进入留痕与异常，事后可追溯。</li>
              </ul>
            </div>
            <div class="info-block">
````

### BSM-118 — 能自动识别环境的安装包 + 内附教程

- 来源：`docs/business-system-map-for-operations-v0.1.html:1940`；锚点：`h3`；父锚点：系统交付与上线：正式上线要做的安全加固和安装交付
- 适用标签：compliance、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
              <h3>能自动识别环境的安装包 + 内附教程</h3>
              <ul>
                <li>最终要提供一个能自动检测运行环境的安装包：安装时自己识别当前是测试环境还是生产环境，并自动完成相应的配置，不用现场人员手工去分辨和调参。</li>
                <li>安装包里附带一份简单的使用教程：用业务语言写清楚怎么安装、怎么日常使用，让不懂技术的人也能照着装、照着用；正式上线的安全加固步骤（换强密码、U 盘密钥托管）也要在教程里讲清楚，指引现场按步骤完成。</li>
              </ul>
            </div>
            <div class="info-block">
````

### BSM-119 — 和谁有关

- 来源：`docs/business-system-map-for-operations-v0.1.html:1947`；锚点：`h3`；父锚点：系统交付与上线：正式上线要做的安全加固和安装交付
- 适用标签：compliance、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
              <h3>和谁有关</h3>
              <ul>
                <li>连接留痕与异常，上线换密码、U 盘插盘托管、破窗登录都要留痕可追溯。</li>
                <li>连接各类入口和权限，最高权限口令是所有后台入口的总钥匙，托管好它才能守住整套系统。</li>
              </ul>
            </div>
          </div>
        </section>

        <section id="questions">
````

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

### PGM-015 — 楼宇运营

- 来源：`docs/platform-governance-map-for-operations-v0.1.html:399`；锚点：`h3`；父锚点：模块入口：平台治理层包含哪些管理模块
- 适用标签：governance、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
              <div class="card-top"><h3>楼宇运营</h3><span class="tag">基础</span></div>
              <p>管好建筑本身：电梯、暖通、配电、给排水等机电设备，水电气能耗，以及日常安防消防巡检。</p>
              <span class="open-link">查看说明</span>
            </a>
            <a class="system-card" href="#recovery-privacy">
````

### PGM-018 — 物理项目与运营公司：多对多协作，不是固定隶属层级

- 来源：`docs/platform-governance-map-for-operations-v0.1.html:415`；锚点：`h3`；父锚点：综合体和集团：当前一个项目，未来能看集团
- 适用标签：governance、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
            <h3>物理项目与运营公司：多对多协作，不是固定隶属层级</h3>
            <ul>
              <li><strong>顶层平台</strong>：制定跨项目、跨经营域的统一治理规则，管理统一系统，并审批跨域规则；不因为处于顶层就自动取得某张票、某个项目或某个商户的收入。</li>
              <li><strong>物理项目</strong>：是空间、价格、经营数据和权限隔离维度。一个运营公司可以服务多个项目，一个项目也可以同时由多家运营公司分域负责。</li>
              <li><strong>域主管公司</strong>：单一经营域的定价、售票、内部结算和分配规则由对应域主管公司负责；跨域组合通票才由项目之上的平台权限审批域间规则。</li>
              <li><strong>城市项目公司</strong>：每个城市项目公司同时承担本城市项目的资产职责，只负责资产、场地、物业、租赁、直接管理的第三方商户及自身人员和设备；它不管理酒吧、游乐、娱乐平台公司、传媒公司、中央餐饮公司及其人员、设备、定价、票务或结算。历史合同、财务底稿与既往文件中出现的“资产公司”“资产管理公司”，指的就是本主体，同一主体两个叫法；资产业态不另设独立主体，由城市项目公司兼任。但作为综合体通用储值卡的发行与收款主体，长沙项目公司应当就通用卡余额的实际消费向各业态的经营主体直接付款结算，按三个付账日结清；此项付款只是按各业态既定规则兑付客人已付的钱，不构成对该业态定价、票务或结算规则的管理权。</li>
            </ul>
          </div>
          <div class="info-block" style="margin:0 0 4px">
````

### PGM-026 — 主要功能

- 来源：`docs/platform-governance-map-for-operations-v0.1.html:492`；锚点：`h3`；父锚点：商户和主体：谁在这里经营，经营什么
- 适用标签：access、amusement、analytics、bar、compliance、contract、customer、energy、evidence、facility、finance、governance、integration、inventory、leasing、notification、ops、partner、settlement
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
              <h3>主要功能</h3>
              <ul>
                <li>建立商户档案、运营主体、负责人、第一联系人和第二联系人。</li>
                <li>区分自营、租赁、联营、代运营、服务商接入、其他等合作方式。</li>
                <li>绑定空间、业态、合同和经营范围。</li>
                <li>把一个商户的“经营对象、运营主体、合同主体、结算主体”绑成一组唯一关系（四类主体可分属不同公司）；合同、结算、权限、看板、第三方接入都引用同一组绑定。</li>
                <li>合作模式分六类：自营、租赁、联营、代运营、服务商接入、其他；各类合作模式不预设固定的收费项：平台出具合作模板，依合同约定生成对应的收费项与计费模板，再据以计算。准入不设统一的否决标准，由运营人员判断，唯一的硬性限制是违法的一律不得签约。“其他”用于上述五类无法覆盖的特殊合作，须备注说明，并必须在合同生效前指定套用哪几项收费模板（固定租金/物业费/能源费、销售分成、代运营服务费、供应商应付），未指定的合同不得生效。公司级配置只管理财务、税务和管理权三类权责：财务权责包括销售主体、合同主体、收款主体、收入主体、成本主体和结算主体。任何一笔业务的收款方、结算方或收入归属方需要变更时，只能通过“公司主体与权责配置”办理，不得由后台的普通开关或运营操作改动；财税主体的调整须经平台财务与税务、法务并行审核，管理权的调整由顶层平台公司终审，申请人不得审批自己提交的调整，紧急调整同样不能绕过审批。所有调整一律只对生效时间之后的业务有效，不追溯改变历史合同、订单、票据和结算快照；确需迁移历史数据的，必须另行完成移交、公司间资金结算和完整审批。</li>
                <!-- 2026-06-24 地产全链授权扩展：商户管理从「入驻后档案」扩展为「招商前链 + 商户档案」。依据用户 2026-06-24「扩展 HTML 到地产全链」拍板 + gap-analysis §7B.2「招商洽谈中状态与跟进」。 -->
                <li>招商获客前链：维护招商线索、储备品牌库、意向客户和洽谈跟进记录；线索经初步接触、需求确认、方案报价、商务谈判推进到签约，签约后自动转为正式商户档案。</li>
                <!-- 2026-06-25 修改阶段：补招商运营细则，依据 M1 需求采集（complex-m1-requirements §1）用户拍板。 -->
                <li>招商运营细则（已确认）：设客户关怀部：不区分对客与对商户，两类一并处理；职责为受理客人投诉与商户异议、监督与协调各责任部门处理、确认闭环，并负责工单处理；客诉与商户异议的实体回复由涉及部门与财务部门作出，客户关怀部不代作实体判断。平台须建立突发事件标准处理流程。在该流程建成之前，以下五条先行生效：（一）叫停营业：店长报主理人或夜班值班经理，可以让一家店当场停止营业；（二）留痕：突发事件期间以手机网络记录；手机网络与无线网络同时中断的，仅免除中断期间的实时线上记录，现场须以可用方式留证（如随身录音摄像设备、拍照/视频），网络恢复后必须将中断期间的处置过程补录进系统；（三）对外口径：公司指定唯一的对外口径负责人，其对外沟通时须携带随身录音摄像设备，其他工作人员未经书面许可不得对外发言；（四）与客诉挂钩：突发事件自动生成一条最高级别客诉，走客诉的同一闭环，不另起流程；（五）分级：突发事件的严重度直接套用现有的风险四级（低、中、高、严重），不另设一套。设楼宇保安团队，与安防为同一支队伍，由城市项目公司管理，职责为接收系统预警、调阅监控录像、核查异常出入。线索来源分主动拓展、官网/小程序留资、老商户转介绍、展会活动四类，录入即打来源；线索由招商主管手动指派负责人；储备品牌库分意向/储备/合作/黑名单，黑名单品牌拦截同品牌新线索，以及由拉黑前既有线索新建意向。确需继续的，须由具备单独授予的黑名单例外放行权限人员操作，按层级对应主管审批，申请人不得审批自己提交的申请，并记录操作人、审批人、理由、时间和关联线索或意向。品牌名用于线索去重；意向五阶段中进入方案报价、商务谈判须主管确认才能推进；招商线索在已签约前，可从待跟进、跟进中、已转意向或洽谈中转为已流失或已搁置；转换时必须填写原因，已签约后不得回退至未签约状态。线索或意向超过后台配置的未跟进天数（业务参数，按项目可配，出厂默认三天，须同时配置上下限）自动提醒负责人；签约转商户自动带入联系人与联系方式、意向业态/面积/位置、报价租金条款和跟进历史，四类主体默认指向同一公司、后续可拆为不同公司。</li>
                <li>支持酒吧和游乐/乐园商户未来作为独立系统对外使用。</li>
              </ul>
            </div>
            <div class="info-block">
````

### PGM-039 — 智能设备场景联动范围

- 来源：`docs/platform-governance-map-for-operations-v0.1.html:636`；锚点：`h3`；父锚点：设备管理：谁领了什么设备，设备能不能用
- 适用标签：ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
              <h3>智能设备场景联动范围</h3>
              <p>桌台不显示运营计时，不建设计费倒计时、钟点房、按时长收费或续时产品。设备场景动作和延时编排仅登记为后续独立立项；独立立项批准前不定义设备动作、触发条件、延时编排或自动执行。立项时必须明确硬件范围、故障兜底、人工接管和安全责任。</p>
            </div>
          </div>
        </section>

        <section id="permission">
````

### PGM-052 — 楼宇运营：管好建筑本身的设备、能耗和巡检

- 来源：`docs/platform-governance-map-for-operations-v0.1.html:806`；锚点：`h2`
- 适用标签：facility、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
          <h2>楼宇运营：管好建筑本身的设备、能耗和巡检</h2>
          <p class="lead">除了员工工作设备和游乐设备，综合体的建筑本身还有电梯、暖通、配电、给排水等机电设备，以及水电气能耗和日常安防消防巡检。这一层把“楼宇本体”的运营也管起来，和物业服务、合同、财务连通。</p>
          <div class="detail-grid">
            <div class="info-block">
````

### PGM-053 — 楼宇机电设备

- 来源：`docs/platform-governance-map-for-operations-v0.1.html:810`；锚点：`h3`；父锚点：楼宇运营：管好建筑本身的设备、能耗和巡检
- 适用标签：facility、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
              <h3>楼宇机电设备</h3>
              <ul>
                <li>建立楼宇机电设备台账：电梯、暖通空调、配电、给排水、消防等，记录位置、品牌、责任人和状态。</li>
                <li>管理定期保养、例行巡检、故障维修、安全检测和部件更换，维保单位作为供应商记录。</li>
                <li>设备告警和故障可自动生成物业工单，维修完成后回写状态。</li>
                <li>设备状态：正常、告警、故障、维保中、停用、报废。</li>
                <!-- 2026-06-25 修改阶段：补设备运营细则，依据 M1 需求采集（complex-m1-requirements §3）。 -->
                <li>运营细则（已确认）：设备分电梯扶梯、暖通空调、配电供电、给排水消防四组台账；告警来源支持人工上报与远程/物联自动采集两种；每台设备单独设维保周期，到期提醒生成维保单。</li>
              </ul>
            </div>
            <div class="info-block">
````

### PGM-054 — 能耗计量

- 来源：`docs/platform-governance-map-for-operations-v0.1.html:821`；锚点：`h3`；父锚点：楼宇运营：管好建筑本身的设备、能耗和巡检
- 适用标签：facility、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
              <h3>能耗计量</h3>
              <ul>
                <li>维护电表、水表、燃气表、冷热量表台账，按公共区域、商户、设备/回路归集。</li>
                <li>支持人工抄表，并预留远程表计接入；抄表数据经审核入账。</li>
                <li>识别能耗异常：突增、突降、零读数、读数倒走，异常进入复核。</li>
                <li>商户能耗按各商户独立表计的实际用量与实际费用代收（一律不得加价），作为能源费结算依据，连接财务；公区能耗只做归集与成本分析，不向商户分摊（与本章下文及枚举速查表能源费条一致）。</li>
                <!-- 2026-06-25 修改阶段：补能耗运营细则，依据 M1 需求采集（complex-m1-requirements §6）。 -->
                <li>运营细则（已确认）：表计按总表-分表分层、可总分表抹差核对；抄表以远程表自动读为主、异常才转人工复核；异常阈值后台按表计类型分别配置；每次抄录必须同时上传表底照片，系统自动与上期读数比对；判为异常（突增、突降、零读数、读数倒走）的读数一律不得直接出账，须由第二人复抄或到场核表，核实后填写原因并经主管确认才转为已审核，核实完成之前该商户当期能源费账单暂缓生成、暂缓催缴；异常读数被更正的，保留原始录入值与更正记录。公区能耗只做归集与成本分析，不向商户分摊；公区公摊费用统一含在物业费中，商户能源费只按其独立表计的实际用量与实际费用代收、一律不得加价，并在平台中记为代收代缴往来、不作为城市项目公司的经营收入。总表减分表所形成的差额（含线损、无表点位与表计误差），通过电损费与水损费按各商户实际用电度数、用水吨数另行独立计收并单独开具服务发票。</li>
              </ul>
            </div>
            <div class="info-block">
````

### PGM-055 — 楼宇巡检

- 来源：`docs/platform-governance-map-for-operations-v0.1.html:832`；锚点：`h3`；父锚点：楼宇运营：管好建筑本身的设备、能耗和巡检
- 适用标签：facility、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
              <h3>楼宇巡检</h3>
              <ul>
                <!-- 2026-08-04 第三轮用户裁决（DEC-R6-12）补漏落点，R6-T1b 收口：五类→四类，与 L774 及 ENUM「楼宇巡检类型」条一致 -->
                <li>制定巡检计划，按安防、消防、设施设备、环境卫生四类分类；能耗抄表走能耗域独立流程、不在巡检里重复抄（与本章下文一致）。</li>
                <li>巡检任务分派到人，现场扫码或定位留痕，发现异常生成整改工单。</li>
                <li>任务状态：待执行、执行中、正常、发现异常、整改中、已复检、已关闭。</li>
                <li>楼宇巡检与游乐设备安全巡检分开管理，但都要主管确认闭环。</li>
                <!-- 2026-06-25 修改阶段：补巡检运营细则，依据 M1 需求采集（complex-m1-requirements §5）。 -->
                <li>运营细则（已确认）：巡检计划支持按类型模板自动排、主管手动排、按点位路线排三种；现场留痕支持拍照、扫二维码、定位三种；发现异常支持「轻记录/重转整改工单」分级与「全部转整改工单」两种模式，整改后复检、主管确认闭环；能耗抄表以能耗域独立流程为主，不在巡检里重复抄。</li>
              </ul>
            </div>
            <div class="info-block">
````

### PGM-056 — 和谁有关

- 来源：`docs/platform-governance-map-for-operations-v0.1.html:844`；锚点：`h3`；父锚点：楼宇运营：管好建筑本身的设备、能耗和巡检
- 适用标签：facility、ops
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
              <h3>和谁有关</h3>
              <ul>
                <li>连接物业服务：设备告警、巡检异常自动转物业工单。</li>
                <li>连接财务：能耗计量是能源费结算依据。</li>
                <li>连接供应链：维保单位、备件复用供应商和库存。</li>
                <li>连接留痕复核：设备停用、异常整改、能耗异常都要留痕。</li>
              </ul>
            </div>
          </div>
        </section>

        <!-- 2026-07-03 回写：紧急恢复(破窗)与客人隐私保护(均已落地)。 -->
        <section id="recovery-privacy">
````

### PGM-066 — 已拍板规则和待排期问题

- 来源：`docs/platform-governance-map-for-operations-v0.1.html:951`；锚点：`h2`
- 适用标签：access、amusement、analytics、auth、bar、compliance、customer、entry、evidence、finance、governance、integration、inventory、ops、settlement、terminal、ticketing
- 判据：映射模块内的实现、测试和文档须与该锚点原文一致，不得冲突、遗漏或另行发明业务规则；可执行声称须有实际行为证据。
- HTML 原文：

````html
          <h2>已拍板规则和待排期问题</h2>
          <div class="question-list">
            <div class="question confirmed">已拍板：未来需要集团视图和跨项目汇总视图，保证所有对象带综合体项目归属。</div>
            <div class="question confirmed">已拍板：商户侧在授权业务范围内全开放，酒吧和游乐/乐园保留独立成单独系统的能力。</div>
            <div class="question confirmed">已拍板：日结锁定后不允许直接补录影响金额，必须上级审批通过后才能补录。</div>
            <div class="question confirmed">已拍板：员工权限按单一功能最小颗粒度配置，支持角色和员工级完全自定义。</div>
            <!-- 2026-08-04 第三轮用户裁决（DEC-R6-23）：企微会话永久保存的合法性论证已补齐（合规文件 §4.4），本处「尚未补齐」的未了义务关闭 -->
            <div class="question confirmed">已拍板：企业微信使用已有会话能力，平台负责统一同步、永久存储（不自动到期删除，清理按留痕清理的同一规则执行）、质检和审计。<!-- 修订说明（2026-08-04 第三轮审计裁决）：本条经用户裁决维持"永久存储、不自动到期删除"，与本页 #staff 及业务说明地图现行口径一致，不再存在内部漂移。2026-06-18 R5 审计曾把它统一为可配置留存，该处置已被推翻、不再适用。遗留义务已了结（2026-08-04 第三轮用户裁决 DEC-R6-23）：合规文件自身要求的合法性论证已补齐，见《合规与个人信息保护要求 V0.1》§4.4「企业微信会话永久保存的合法性论证」，本处未了事项就此关闭。 --></div>
            <div class="question confirmed">已拍板：第三方接口资料当前未确定，先做自有系统和对外对接接口示例，后续再接第三方对接。</div>
            <div class="question confirmed">已拍板：所有有过程的业务动作都必须留痕，酒水和可追责物品从库房到客人的全过程必须可追溯。已拍板例外：安检环节不产生按人记录，留痕与溯源以设备侧记录为终点。</div>
            <div class="question confirmed">已拍板：订单获取、销售归属、卡座服务、异常第一处理人、转派升级、复核关闭都必须进入统一留痕。</div>
            <div class="question confirmed">已拍板：留痕优先自动生成，依赖账号、设备、点位、班次、扫码、支付、机器人、企业微信和第三方回流，人工只补原因和证据。本条为操作指引，不作为合规判定依据；留痕是否完备以「所有有过程的业务动作都必须留痕」为准。</div>
            <div class="question confirmed">已拍板：所有端都必须有后台开关，关闭后该端或模块不可用，打开后仍受权限和数据范围控制。</div>
            <div class="question confirmed">已拍板：数据可溯源、全链可点击（无孤岛数据）——前端展示的所有数据、单据、对象和涉及人员/角色都必须可点击追溯到来源、可上下钻取、可正查反查，不允许孤立显示；适用平台全部模块。</div>
            <div class="question confirmed">已拍板：界面全中文（所有终端零英文）——后台/客人端/现场设备/安卓端展示给用户的一切文字（菜单、字段、状态、枚举、按钮、提示、报错、导出、短信、日期时间）必须全中文，不得出现英文；可以不用中文的只有单据号、设备编号、二维码内容、系统内部标识符四样，此外一律中文；强制规则，适用全部终端与模块。</div>
            <div class="question confirmed">已拍板：涉及任何下单的业务（票务、酒吧点单、游乐预定、服务点单、储值充值等）都必须记录「下单时间」和「支付时间」两个时间——下单时间=创建下单的时刻，支付时间=支付成功的时刻，二者分别独立记录，不得用下单时间冒充支付时间（未支付则支付时间为空）；供对账、时效统计与全链追溯。</div>
            <div class="question confirmed">已拍板：物品与履约全链凡是"有时间属性"的环节都必须记录该时间——商品出库、被服务人员拿取/交接、物品配送到预定位置、上台/送达、履约完成等每个流转阶段各记一个时间戳；状态流转不得只记状态不记时间；供对账、时效统计与从库房到客人的全程追溯。</div>
            <div class="question confirmed">已确认：集团视图预留项目归属和跨项目汇总口径，不阻塞单综合体运营。</div>
            <div class="question confirmed">已拍板：权限点首批由平台统一预置，实施阶段允许边配置边补充。</div>
            <div class="question confirmed">已拍板：企业微信后台配置由管理员一次性完成授权和安全凭据配置，系统自动做账号对应、同步、失败告警、基础规则质检和留存策略。</div>
          </div>
        </section>

        <p class="footer-note">用途：给不懂产品和技术的业务人员理解平台治理层范围。<br><strong>解释权声明：</strong>本《平台治理地图》与《业务系统地图》《业务枚举速查表》共同组成三份最高业务解释权 HTML。发生冲突时，只以这三份 HTML 中经用户批准的内容为准；PRD、其他文档、后台配置、设计和实现均不得覆盖。某项业务内容若尚未写入三份 HTML，不因出现在 PRD、配置或实现中而取得效力，必须先逐项获得用户批准并补入相应 HTML。三份 HTML 之间若出现冲突，按确认时间线判定：凡经用户确认过的内容，确认时间越晚的优先级越高，后确认的覆盖先确认的（以决策台账记录的裁决时间为准）；无法确定确认时间先后的，仍须逐项重新裁决。</p>
      </div>
    </main>
  </div>
</body>
</html>
````


## 9. 本席最终结论与验证限制
PASS。79/79定义已读，61/61原文匹配，6/6文件全覆盖，无新finding；仅本席本次票，等待Grok合取及B5四值。
未执行全仓verify:predeploy父链；只核15子入口与相关doc gate，不声称全仓门禁绿。未在GitHub创建真实CI run、push、PR、merge或部署；网络边界为stub，官方API只读。未重新安装真实hook；本地Node为24.20.0，CI配置为22，不声称本轮实际跑过Node22。worktree-common路径本次完整源码审查，但本方B5未另建worktree验证，默认/相对/绝对/tilde四路径已实跑。此次只审源码用途，不用此结论替代生产环境验收。


CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M007 B5-r1 Codex完整独立最终复审
版本：V0.02（原147755字节V0.01历史完整保留）
先派Grok，本报告封存前未读本次Grok B5。不是旧B5结论转抄；本次独立重判五源全部163行、79判据、必要直接回归。B2空队列跳新增B3/B4，累计B3仍1。

## 阅读复用与现时证据
本轮对五源与两直接回归文件共7个SHA逐一实算，全部与本轮B1全文阅读版本相同；复用那次完整源阅读，重新逐文件三线及全部79项判断。HTML/锁同字节阅读也复用，不复用旧PASS。实际本节点m007七项、m001merge三模式、docs检查重新运行均通过。
{"hashes":{"ops/160/install-git-hooks.sh":{"sha":"8609b7e20a6fc49cec6ffc90781310e73fb943c00ca7791e564e956166f9a37b","same":true},"ops/160/merge-pr.sh":{"sha":"3e222657e568b852608eec39bc64efd2d618dce71b93f22864f3a076d68bcf06","same":true},"ops/160/propose-change.sh":{"sha":"88d3abe9a1c1beaecb7ce6645df537be088768620b72827ad293ba74f7000a8b","same":true},"ops/160/hooks/pre-push":{"sha":"e38ccd33e40065045c24fa8d4bb08d35f519a753ce2520884fee7103e4e77f85","same":true},"ops/ci/predeploy-gates.yml":{"sha":"4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d","same":true},"tools/gate-hardening-tests/m007-regression.mjs":{"sha":"10e7b07bf603eb2a47d07fa7a9a8f9f022ba4ff8c3687a1f99cd2889af5781d8","same":true},"tools/gate-hardening-tests/m001-regression.mjs":{"sha":"c64e3048e7525c6158e102f9cd9eaf9cd10972c1255f9a2deb5528a8299383a6","same":true}},"checks":[{"args":["node","tools/gate-hardening-tests/m007-regression.mjs"],"exit":0,"out":"PASS M007-001 main -> feature/a allow=0\nPASS M007-001 feature/a -> main allow=0\nPASS M007-001 feature/a -> main allow=1\nPASS M007-001 feature/a -> mainish allow=0\nPASS M007-002 Git 生效路径\nPASS M007-003 脏工作区不得推送\nPASS M007-004 查询失败须保留原因并立即退出\n","err":""},{"args":["npm","run","verify:delivery-gates-docs"],"exit":0,"out":"\n> entertainment-complex-management@0.1.0 verify:delivery-gates-docs\n> node tools/verify-delivery-gates.mjs\n\nDELIVERY_GATES_DOCS_PASS\n","err":""},{"args":"m001 existing merge segment only","exit":0,"out":"PASS 合并提交 stable\nPASS 合并提交 watch\nPASS 合并提交 late\n","err":""}],"runtime_hook_sha":"46aabdf33cb3496d83a71e4357ca85ba2ed9dbbfd4582ab53747bc50bb748d6b","lock":"b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458","sidecar":"b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458","ledger_top":["CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458","LOCK-AMEND-COUNT: 1"],"status":""}
五源16+73+30+17+27=163行；历史计划156不改。m007回归102行和m001278行只为验证直接行为，不扩冻结FILES。全部技术性，无业务实体/价格/授权发明。BSM116生产上线章不被硬套成Git规则细化上位。

## 第一性原理三问
1. 必须存在的理由：安装器把钩子放到Git实际目录，hook按目标ref阻断main直推，propose把已验干净HEAD送PR，merge等待CI并钉SHA，CI镜像受现有一致性检查消费。删除这些并不能删去治理要求，只把检查推回手工。没有HTML逐字Git命令不等于添加或应删。
2. 行为证明：原脚本在隔离fixture真实运行，发布边界被fakegit/gh/npm或file远端替换。根本节点安装~/路径断言、dirty无push、查询错误非0一次保留原因、merge稳定0/等待变更1/末刻变更1均通过。B1额外dirty-before-npm/clean-npm-fail/clean-npm-pass同字节实跑记录明确复用，未谎称此节点再次跑3个额外探针。
3. 未证假设：未在主仓安装钩子、未真实推送合并部署/运行GitHub runner/全predeploy链；任何既有PGM台账红/入口漂移/runtime陈旧都未刷绿。主仓.git/hooks旧SHA当前仍不等于现源，不能声称运行副本已更新。

## 全部源三线重判
install1–16：shebang版本/用途、set-euo、根定位、Git原生--path、非空custom/默认common/hooks、mkdir/install0755/结果输出全部核。~合法输入实际回归有效；相对路径按根落点、绝对路径保留；无需自造展开器。失败命令set-e停止。仅工程安装维护，非产品界面英文规则。
hook1–17：四字段read-r、只remote_ref=refs/heads/main、覆盖只1、默认拒、其余继续最终0。源main→feature及mainish正例、feature→main反例实际通过。不存在旧整行匹配源缺陷。多ref遇main拒绝；无set-euo理论I/O不满足首跑故障标准，原011保持观察。
propose1–30：标题必需、main拒绝、clean先于npm、npm失败停、push失败停、既有PR或固定main创建，标题正文独立argv无eval。旧003工作树与提交不一致已堵；成功路径仅在fixture模拟不实际对GitHub发消息。注释不等于强制业务审批。
merge1–73：method白名单、branch/dirty/HEADremote/PR保护，显式skip警告；查询30次有界，错误直接非0保留stderr，空列表再等；每run必须绿，watch失败先输出URL再失败；最后remote及PRhead相同再match-head-commit。这些是已有逻辑，不为推测场景新增检查。稳定/等待/末刻三模式真实shell0/1/1支持竞态防护。query返回2而非1正确；判定非0不是吞错。多个run全部AND和未显式检查PRbase按既有正道/铁律6维持登记。
CI1–27：2个main事件、job/runner、完整actionSHA、fetchdepth0、Node22、ignore-scripts、事件base、predeploy运行；真实镜像一致且docs门禁通过。actiontag对应版本B1官方GET事实保留；不因缺超时/新权限项扩改配置。

## 对抗式
源main推feature是否仍被拦：无，当前四字段实现及正例通过。
~安装是否exit0但失效：无，实际HOME目标内容及执行位断言成立。
dirty会否验证好工作树却推坏HEAD：dirty在npm前失败，本节点旧回归也确认无push。
gh查不到和gh出错是否混淆：出错仅一次且stderr，真正空按原30次等待；不添加重试策略。
CI等待或最终merge窗口SHA变化是否误合：watch/late均拒；match-head参数保留。
覆盖开关是否假装不可绕：无，明确开发者提示/显式覆盖不代表业务授权。
非法空格ref是否应新加输入规则：非Git正常四字段，Grok原harness失败保留，不当产品缺陷。
主仓运行钩子是否现行源：不是；本节点实算仍旧46aabdf…，独立运行配置观察，不篡改它使审计变绿。

## 79判据逐项
| ID | 当前判断和证据 |
|---|---|
| BEQR-085 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-086 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-087 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-088 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-089 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-090 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-091 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-092 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-093 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-094 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-095 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-096 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-118 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-146 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-155 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-160 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-161 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-162 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-163 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-164 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-165 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-166 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-167 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-168 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-169 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-170 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-171 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-172 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-173 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-174 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-175 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-176 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-177 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-178 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-179 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-180 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BEQR-181 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BSM-020 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BSM-028 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BSM-081 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BSM-082 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BSM-083 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BSM-111 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BSM-112 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BSM-113 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BSM-114 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BSM-115 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| BSM-116 | 上线工程支撑适用：四道流程不能替代HTML强密码、USB托管及留痕、安装包和教程；代码没有宣称完成这些业务要求。 |
| BSM-117 | 上线工程支撑适用：四道流程不能替代HTML强密码、USB托管及留痕、安装包和教程；代码没有宣称完成这些业务要求。 |
| BSM-118 | 上线工程支撑适用：四道流程不能替代HTML强密码、USB托管及留痕、安装包和教程；代码没有宣称完成这些业务要求。 |
| BSM-119 | 上线工程支撑适用：四道流程不能替代HTML强密码、USB托管及留痕、安装包和教程；代码没有宣称完成这些业务要求。 |
| GEN-001 | 五文件职责安装/推送提示/提议/合并/CI镜像各异；复用原脚本和原回归，不新增抽象。 |
| GEN-002 | 实际7项回归+merge3模式+提议dirty/clean失败/clean成功均通过；入口及预期负向真实可失败。 |
| GEN-003 | 现有m007回归102行和m001直接merge段实际执行，非仅字符串断言；没有为无缺陷源新增测试文件。 |
| GEN-004 | shell参数均引用，gh固定JSON字段，四字段remote_ref精确匹配；无eval。合法~路径由Git --path展开。 |
| GEN-005 | ALLOW_MAIN_PUSH和SKIP_CI_WAIT是已明确工程逃生口，不代表业务审批或最高权限；不新增授权规则。 |
| GEN-006 | 不写业务金额/状态；推送/合并是外部工程副作用，前置clean/CI/head守卫覆盖。 |
| GEN-007 | remote再次核对+PRhead+match-head-commit覆盖等待变化与末刻竞态；三模式实际0/1/1。 |
| GEN-008 | CI镜像字节与生效文件一致，15个predeploy入口现有，验证的是已提交HEAD而非dirty工作树。 |
| GEN-009 | npm失败拒推；gh查询错误仅一次保留stderr立即退出；watch失败和未出现run退出源码清晰。 |
| GEN-010 | 五源无密钥文本；不输出运行配置凭证。fakegh/npm仅模拟边界，不访问真实GitHub写操作。 |
| GEN-011 | 错误原因/阻断/覆盖提示输出可追；本轮命令返回与assert成立，未用PASS覆盖陈旧证据红。 |
| GEN-012 | 查询至多30次10秒；watch由真实CLI管理，未以理论超时新增策略；资源回归临时目录finally清理。 |
| GEN-013 | 四shell bash-n0；原生Git --path实际安装位置有效；CI完整SHA与当前镜像一致。 |
| GEN-014 | 无数据库迁移，不适用；临时测试Git目录与主仓隔离。 |
| GEN-015 | 脚本CLI维护消息非产品终端UI，已有英文installed无需无缺陷翻译；YAML机器语法不应强译。 |
| GEN-016 | 五源163行=16/73/30/17/27，版本0.01/0.02/0.01/0.01/0.02，计划156是历史计量不改。 |
| GEN-017 | 全部技术性：不新增价格/主体/业务审批/角色/数据范围；无冲突、无添加。纯工程工作流不冒充HTML功能。 |
| GEN-018 | 类型4真实脚本回归/类型1已冻结HTML原句覆盖；不用旧PASS推导现结论。 |
| PGM-015 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| PGM-018 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| PGM-026 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| PGM-039 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| PGM-052 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| PGM-053 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| PGM-054 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| PGM-055 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| PGM-056 | 业务实现N/A：五个工程入口不定义该冻结业务锚点的实体/状态/价格/权限，也未声明实现完成；与已读同字节HTML原句无冲突。 |
| PGM-066 | 间接适用：CI调用既有外部依赖门禁，不新增第三方实现/业务终端；工程CLI语言不当业务UI要求。 |

## 全部旧ID当前重判
001远端目标判断技术性旧缺陷已修；002Git --path技术性旧缺陷已修；003dirty前置技术性旧缺陷已修；004查询错误不吞技术性旧缺陷已修。当前工程缺陷否/队列否。
005覆盖/SHA、006CI镜像、007现有直接回归、008CLI英语、009多runAND、010正道base、011理论I/O均技术性登记无当前缺陷/队列否。没有新CX，不复用编号，不删除旧FAIL事实，B3累计1。
OBS1安装副本旧/OBS2分叉而源blob一致/OBS3全局红灯分别保留。当前五源及两回归porcelain空。GrokB1harness两个FAIL原文不改，不伪称全探针通过；本席断言均0。

## 结论与限制
本席B5 PASS：5源163行、79判据三线完整，11项无当前待修，新CX空集，无添加/未答questions/新HTML问题。此票不证明主仓hook已同步、全仓门禁通过或生产上线完成。
本次未读GrokB5，须真实exit0、完整报告读取、四值一致及双PASS后先ledger再既有收口。不提前模块PASS，无源码/HTML/锁修改，无提交推送发布。
