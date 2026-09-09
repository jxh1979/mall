CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# M007 B1 Codex 独立全量审计
版本：V0.01

结论：FAIL。5文件156行全部读完，79项冻结判据逐项核对；3项工程缺陷、2项仅登记技术性内容，无业务冲突或添加。本方未读本轮Grok raw/原始JSON。Grok真实exit0后才开始本方审计，本方完整落盘后才读取它。这里只是本席结论，不是B2定稿。

## 第一性原理与完整范围
安装脚本把已有pre-push放到Git真正加载的位置；删除会让新环境缺少开发者提示。pre-push按远端main限制直推；删除会失去现有PR流程提示。propose负责本地验证→推feature→创建/显示PR；merge负责干净且已推送的同一提交→等待CI→再次校验远端和PR head→按match-head合并。CI文件是GitHub可见信号的镜像、不是业务上线审批或最终生产准入。删除这些入口不会删除一条业务枚举，却移除可执行工程约束；因此均属技术性，不能因HTML未写Git/npm而判添加。
三HTML的BSM-116/117/118/119说明生产专门把关、强密码/U盘/留痕/安装教程；这5文件本身不负责生产密钥、数据库、业务授权、现场安装包，不能把其PASS冒称已满足全部生产要求。其余游乐/楼宇/合同/营业日/看板/主体/入口规则在本范围没有对应业务状态或数据写入，不新增业务含义。

## 本席全部finding
### CX-01（技术性，工程缺陷是）
ops/160/hooks/pre-push:5-8整行匹配refs/heads/main，误把localref当remoteref。输入local refs/heads/main→remote refs/heads/feature/a会exit1；正确行为只阻止目标main。原注释“源ref含空格”前提不成立，git check-ref-format含空格返回1。
证据类型2可执行产物、4可复现输出；GEN-002、004、008、009、016、018。无业务发明：控制Git分支，不控制客人/员工业务权限或审批。
业务影响：合法推送被拒，诱导操作者使用紧急覆盖。最小修法：按Git四字段格式读取，只比较第三字段，删误导注释，保留既有紧急覆盖；不扩大为强制安全边界。下方HOOK main-to-feature明确复现。

### CX-02（技术性，工程缺陷是）
ops/160/install-git-hooks.sh:6-14用git config --get读取core.hooksPath原始字符串，然后直接当目录；~/custom-hooks被写成仓库内字面~目录，Git实际加载HOME/custom-hooks。脚本返回0并打印已安装，但有效目标不存在。
证据类型2、4；GEN-002、009、013、016、018。无业务发明：只解析Git配置路径；合法Git路径常规首次安装就可复现，不需要多重偶发条件。
最小修法：使用Git原生路径解析（如config --path --get或Git解析后的hook路径），保持默认/相对/绝对分支；不加新配置。下方真实Git四种路径仅tilde失效。当前主仓core.hooksPath未设、实际hook等于源码，不夸大成当前已完全失效。

### CX-03（技术性，工程缺陷是）
ops/160/propose-change.sh:8-15没有验证待推提交与本地检查的工作树一致。已提交gate=bad、本地未提交gate=good时，真实npm验证本地good并通过，脚本随后推送仍为bad的HEAD，exit0。此问题由未提交修改单一常规条件触发。
证据类型2、4；GEN-002、004、008、009、011、013、018。无业务发明：限制的是被验证/推送的源码版本一致性，不改变业务准入规则。
最小修法：复用merge现有干净工作区前提，在本地验证前拒绝未提交修改；写针对原脚本的最小失败测试。不是声称能直接绕过最终merge：merge另有clean/SHA/CI保护，这里缺陷是本地“红则不推”的版本绑定错误。

### CX-04（技术性，工程缺陷否，仅登记）
ops/160/merge-pr.sh:1-73。12类隔离执行检查通过：正常两run成功、非法方法、main、dirty、远端不存在、不等SHA、无PR、30次无run、红CI、等待时更新、最后合并时更新、明确skip。成功调用含--match-head-commit；普通失败不触发模拟合并。watch API错误/未成功会退出，末段remote/pr head仍核对。
GEN-002/004/007/009/011/013/017/018；证据类型2、4。无业务发明：Git合并前提和既有可见skip提示，不是业务安全红线开关。未执行真实合并/推送，不以模拟证明远端服务器实际接受。
一个SHA有多个历史失败run导致保守拒绝等组合情形未按单个正常首跑缺陷扩大修复；未引入新门禁或规则。

### CX-05（技术性，工程缺陷否，仅登记）
ops/ci/predeploy-gates.yml:1-27。PyYAML BaseLoader解析成功，pull_request/main与push/main完整保留；checkout/setup-node完整commit固定；fetch-depth0；Node22；npm ci --ignore-scripts后运行已有predeploy父链（15子入口全部存在）；HTML_GATES_BASE取PR base.sha或push before；与实际.github/workflows文件逐字节相同，当前GitHub只读GET为active。
GEN-002/013/016/017/018；证据类型2、4。无业务发明：仅构建/验证时机与依赖，不定义经营准入。未跑生产发布/数据库/整个全仓父链；父链消费者的已知范围外失败仍由M425/M427处理，不冒称全仓已绿，不把可见CI当服务器required保护。

## 三线结论
第一性原理：真实git/npm/bash行为证伪3处注释/流程主张，必要性及删除代价如上；无从注释推定正确。
顺从式：逐行读全部156行；语法及配置正常；成功和失败分支覆盖；79冻结条目逐条列于下方。所有61段HTML摘录与当前HTML逐字匹配，无未读摘录。
对抗式：源main/目标feature、tilde路径、干净提交与dirty工作树版本差异是已实证反例。另实测CI红、无run、提交前移、push失败、skip、现有PR，未扩大到理论组合或未轮到模块。
技能：本轮已实际列出并读using-superpowers及Codex适配说明；继续按先前实际读过的grilling/codebase-design/diagnosing-bugs/systematic-debugging/verification-before-completion/ponytail-review收窄规则应用。事实自查，不派第三主体。无修复，TDD节点尚未触发。当前宿主无Claude hooks，使用不force push/not hard reset约定，不冒称插件hooks执行。

## 证据说明
以下原样保存本席实际执行的隔离复现代码与输出。临时目录在正本.scratch之下，原脚本/业务HTML未改；git/gh外发动作由明确stub拦截。第二段使用真实Git隔离仓、真实npm，只把push/PR写动作换为stub；隔离fixture commit不是主仓提交。观察到缺陷的断言是确认复现，不是声称目标正确。


## 79判据逐项覆盖
| ID | 当前范围核验 |
|---|---|
| BEQR-085 | 已读项目开放状态及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-086 | 已读场次状态及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-087 | 已读游乐「当天／每天」口径及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-088 | 已读容量双阈值及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-089 | 已读园区容量及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-090 | 已读设备关键/辅助分级及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-091 | 已读全园故障与补偿通票及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-092 | 已读安全严重度及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-093 | 已读巡检任务状态及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-094 | 已读越界类型及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-095 | 已读团体/包场及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-096 | 已读游乐增强阶段及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-118 | 已读运营营业日及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-146 | 已读运营公司职责及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-155 | 已读商户/运营主体状态及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-160 | 已读招商线索状态及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-161 | 已读招商意向阶段及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-162 | 已读品牌/客户库类型及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-163 | 已读合同变更类型及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-164 | 已读退租结算状态及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-165 | 已读内部应付执行状态及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-166 | 已读合同审批链及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-167 | 已读租金/账单类型及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-168 | 已读营业额抽成模式及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-169 | 已读公区能源费及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-170 | 已读铺位租赁状态及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-171 | 已读物业工单类型及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-172 | 已读物业工单状态及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-173 | 已读商户服务工单状态及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-174 | 已读工单时效及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-175 | 已读楼宇机电设备状态及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-176 | 已读设备维保类型及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-177 | 已读能耗表计类型及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-178 | 已读抄表状态及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-179 | 已读能耗归集对象及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-180 | 已读楼宇巡检任务状态及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BEQR-181 | 已读楼宇巡检类型及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BSM-020 | 已读楼宇运营及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BSM-028 | 已读主要功能及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BSM-081 | 已读楼宇运营：机电设备、能耗抄表、楼宇巡检及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BSM-082 | 已读主要功能及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BSM-083 | 已读和谁有关及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BSM-111 | 已读经营看板：让管理层和运营看得懂、能追问及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BSM-112 | 已读主要指标及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BSM-113 | 已读谁会看及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BSM-114 | 已读各类入口：不同人只看自己该看的、只做自己该做的及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BSM-115 | 已读相互关系：为什么这些子系统必须连在一起及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| BSM-116 | 已核HTML原段：此处为源码交付工具，仅间接支撑上线专门把关；无生产密钥/现场安装包实现，不从工具通过推断生产要求已完成。 |
| BSM-117 | 已核HTML原段：此处为源码交付工具，仅间接支撑上线专门把关；无生产密钥/现场安装包实现，不从工具通过推断生产要求已完成。 |
| BSM-118 | 已核HTML原段：此处为源码交付工具，仅间接支撑上线专门把关；无生产密钥/现场安装包实现，不从工具通过推断生产要求已完成。 |
| BSM-119 | 已核HTML原段：此处为源码交付工具，仅间接支撑上线专门把关；无生产密钥/现场安装包实现，不从工具通过推断生产要求已完成。 |
| GEN-001 | 5文件用途及删后损失已实证；不为Git未在HTML字面出现而删除。 |
| GEN-002 | FAIL：CX01/02/03。12 merge/6 propose/4 hook/4 install场景及真实dirty反例。 |
| GEN-003 | 审计复现执行原脚本，有可失败断言；B3前不改/新增业务测试文件。 |
| GEN-004 | FAIL：hook输入字段/CX01、propose检查版本/CX03；merge方法/SHA/工作区前提正确。 |
| GEN-005 | 开发者Git入口不承担业务服务端授权；既有紧急覆盖明确非安全边界。 |
| GEN-006 | 无业务事务/金额写入；push失败不创建PR、合并失败不报成功，脚本不能当分布式事务。 |
| GEN-007 | merge已验SHA与--match-head-commit两阶段更新反例均阻断；现有PR分支不重复创建。 |
| GEN-008 | FAIL：CX01字段语义、CX03验证/推送版本不同。 |
| GEN-009 | FAIL：CX02伪成功；其余红CI/无PR/缺远端/推送失败均可见退出。 |
| GEN-010 | 无内嵌凭证；参数均引用，未输出敏感配置；不把开发者提示当授权界限。 |
| GEN-011 | FAIL：CX03本地green不对应推送版本；merge日志和SHA一致，skip警示明确。 |
| GEN-012 | CI查询30次、每次10秒；run上限10；临时复现目录自动释放，未新增缓存循环。 |
| GEN-013 | FAIL：CX02路径配置、CX03版本绑定；CI YAML/镜像/15入口/安装标准路径核验。 |
| GEN-014 | 此5文件没有SQL或数据库迁移，未据此冒称迁移已通过。 |
| GEN-015 | 面向开发者CLI，无产品UI；关键错误中文，内部Git标識不转成业务文案。 |
| GEN-016 | FAIL：CX01注释字段前提、CX02安装成功声称；当前CI镜像和active成立。 |
| GEN-017 | 全部5项按冲突→细化→技术性→添加核过，技术性理由与证据逐项记录。 |
| GEN-018 | 每项依据是当前产物/可复现命令/当前HTML；不采用旧raw结论。 |
| PGM-015 | 已读楼宇运营及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| PGM-018 | 已读物理项目与运营公司：多对多协作，不是固定隶属层级及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| PGM-026 | 已读主要功能及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| PGM-039 | 已读智能设备场景联动范围及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| PGM-052 | 已读楼宇运营：管好建筑本身的设备、能耗和巡检及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| PGM-053 | 已读楼宇机电设备及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| PGM-054 | 已读能耗计量及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| PGM-055 | 已读楼宇巡检及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| PGM-056 | 已读和谁有关及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |
| PGM-066 | 已读已拍板规则和待排期问题及对应HTML原段；5文件无该业务对象/金额/审批/状态写入，不冲突、不发明，不把缺少该业务实现误列脚本缺陷。 |

## 关键分支隔离执行

````python
from pathlib import Path
import os,subprocess,tempfile,shutil,json
r=Path('/home/james/complex-management');scratch=r/'.scratch';scratch.mkdir(exist_ok=True)
fake='''#!/usr/bin/python3
import sys,os,json
from pathlib import Path
name=Path(sys.argv[0]).name;a=sys.argv[1:];mode=os.environ["CASE"];root=Path(os.environ["CASE_ROOT"]);sha="a"*40
with (root/"calls").open("a") as f:f.write(json.dumps([name]+a)+"\\n")
def stop(n=0,s=""):
 if s: print(s)
 sys.exit(n)
if name=="sleep":stop()
if name=="npm":stop(1 if mode=="red-local" else 0)
if name=="git":
 if a==["rev-parse","--abbrev-ref","HEAD"]:stop(s="main" if mode=="main" else "feature/audit")
 if a==["status","--porcelain"]:stop(s=" M pending.txt" if mode=="dirty" else "")
 if a==["rev-parse","HEAD"]:stop(s=sha)
 if a[:2]==["ls-remote","origin"]:
  if mode=="no-remote":stop()
  stop(s=("b"*40 if mode=="mismatch" or (root/"advanced").exists() else sha)+"\\trefs/heads/feature/audit")
 if a and a[0]=="push":stop(1 if mode=="push-fail" else 0)
if name=="gh":
 if a[:2]==["pr","view"]:
  if mode=="no-pr" or mode in ["clean-create","red-local","push-fail","dirty"]:stop(1)
  stop(s=("b"*40 if (root/"advanced").exists() else sha) if "headRefOid" in a else ("https://example.invalid/pr" if "url" in a else ""))
 if a[:2]==["run","list"]:stop(s="" if mode=="no-runs" else "101\\n102")
 if a[:2]==["run","watch"]:
  if mode=="advance":(root/"advanced").touch()
  stop(1 if mode=="red-ci" else 0)
 if a[:2]==["run","view"]:stop(s="https://example.invalid/run")
 if a[:2]==["pr","merge"]:
  assert "--match-head-commit" in a and a[a.index("--match-head-commit")+1]==sha
  if mode=="late":stop(1)
  (root/"merged").touch();stop()
 if a[:2]==["pr","create"]:(root/"created").touch();stop()
stop(97,"UNEXPECTED "+repr([name]+a))
'''
with tempfile.TemporaryDirectory(prefix='m007-b1-',dir=scratch) as td:
 t=Path(td);bin=t/'bin';bin.mkdir()
 for n in ['git','gh','npm','sleep']:
  p=bin/n;p.write_text(fake);p.chmod(0o755)
 for mode,expected in [('stable',0),('invalid',1),('main',1),('dirty',1),('no-remote',1),('mismatch',1),('no-pr',1),('no-runs',1),('red-ci',1),('advance',1),('late',1),('skip',0)]:
  c=t/('merge-'+mode);c.mkdir()
  env={**os.environ,'PATH':str(bin)+':/usr/bin:/bin','CASE':mode,'CASE_ROOT':str(c),'SKIP_CI_WAIT':'1' if mode=='skip' else '0'}
  p=subprocess.run(['bash',str(r/'ops/160/merge-pr.sh')]+(['--bad'] if mode=='invalid' else []),cwd=c,env=env,text=True,capture_output=True)
  calls=[json.loads(x) for x in (c/'calls').read_text().splitlines()] if (c/'calls').exists() else []
  merged=(c/'merged').exists();assert p.returncode==expected and merged==(expected==0),(mode,p.returncode,p.stderr)
  print('MERGE',mode,'exit',p.returncode,'merged',merged,'calls',len(calls),'watch',sum(x[:3]==['gh','run','watch'] for x in calls))
  if mode in ['red-ci','advance','late','skip']:print('MESSAGE',p.stderr.strip())
 for mode,expected in [('clean-create',0),('existing',0),('red-local',1),('push-fail',1),('main',1),('dirty',0)]:
  c=t/('propose-'+mode);c.mkdir()
  env={**os.environ,'PATH':str(bin)+':/usr/bin:/bin','CASE':mode,'CASE_ROOT':str(c)}
  p=subprocess.run(['bash',str(r/'ops/160/propose-change.sh'),'审计标题','审计正文'],cwd=c,env=env,text=True,capture_output=True)
  calls=[json.loads(x) for x in (c/'calls').read_text().splitlines()]
  assert p.returncode==expected,(mode,p.returncode,p.stderr)
  push=any(x[:2]==['git','push'] for x in calls);create=(c/'created').exists()
  if mode in ['red-local','main']:assert not push and not create
  if mode=='push-fail':assert not create
  print('PROPOSE',mode,'exit',p.returncode,'push',push,'create',create,'CALLS',json.dumps(calls,ensure_ascii=False))
 for label,local,remote,allow,expected in [('feature','feature/a','feature/a',False,0),('to-main','feature/a','main',False,1),('main-to-feature','main','feature/a',False,1),('allow','feature/a','main',True,0)]:
  line=f'refs/heads/{local} {"a"*40} refs/heads/{remote} {"b"*40}\n'
  p=subprocess.run(['bash',str(r/'ops/160/hooks/pre-push')],cwd=r,input=line,env={**os.environ,'ALLOW_MAIN_PUSH':'1' if allow else '0'},text=True,capture_output=True)
  assert p.returncode==expected
  print('HOOK',label,'exit',p.returncode,'INPUT',line.strip(),'MESSAGE',p.stderr.strip())
 for mode in ['default','relative','absolute','tilde']:
  repo=t/('install-'+mode);(repo/'ops/160/hooks').mkdir(parents=True)
  for src in ['ops/160/install-git-hooks.sh','ops/160/hooks/pre-push']:shutil.copyfile(r/src,repo/src)
  home=t/('home-'+mode);home.mkdir()
  env={**os.environ,'HOME':str(home),'GIT_CONFIG_GLOBAL':'/dev/null','GIT_CONFIG_NOSYSTEM':'1'}
  def g(*args):return subprocess.run(['/usr/bin/git',*args],cwd=repo,env=env,text=True,capture_output=True,check=True).stdout.strip()
  g('-c','init.defaultBranch=main','init','-q')
  custom={'relative':'.custom-hooks','absolute':str(repo/'absolute-hooks'),'tilde':'~/custom-hooks'}.get(mode)
  if custom:g('config','core.hooksPath',custom)
  target=Path(g('rev-parse','--git-path','hooks/pre-push'))
  if not target.is_absolute():target=repo/target
  p=subprocess.run(['bash','ops/160/install-git-hooks.sh'],cwd=repo,env=env,text=True,capture_output=True)
  assert p.returncode==0
  active=target.exists();assert active==(mode!='tilde'),(mode,target)
  print('INSTALL',mode,'exit',p.returncode,'git_expected',str(target.relative_to(t)),'effective_exists',active,'stdout',p.stdout.strip())
print('HARNESS_FINISHED: source copies unchanged; all expected observed outcomes asserted; three defect candidates are observations, not PASS.')

````

````text
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
PROPOSE clean-create exit 0 push True create True CALLS [["git", "rev-parse", "--abbrev-ref", "HEAD"], ["npm", "run", "verify:predeploy"], ["git", "push", "-u", "origin", "feature/audit"], ["gh", "pr", "view", "feature/audit"], ["gh", "pr", "create", "--base", "main", "--head", "feature/audit", "--title", "审计标题", "--body", "审计正文"]]
PROPOSE existing exit 0 push True create False CALLS [["git", "rev-parse", "--abbrev-ref", "HEAD"], ["npm", "run", "verify:predeploy"], ["git", "push", "-u", "origin", "feature/audit"], ["gh", "pr", "view", "feature/audit"], ["gh", "pr", "view", "feature/audit", "--json", "url", "-q", ".url"]]
PROPOSE red-local exit 1 push False create False CALLS [["git", "rev-parse", "--abbrev-ref", "HEAD"], ["npm", "run", "verify:predeploy"]]
PROPOSE push-fail exit 1 push True create False CALLS [["git", "rev-parse", "--abbrev-ref", "HEAD"], ["npm", "run", "verify:predeploy"], ["git", "push", "-u", "origin", "feature/audit"]]
PROPOSE main exit 1 push False create False CALLS [["git", "rev-parse", "--abbrev-ref", "HEAD"]]
PROPOSE dirty exit 0 push True create True CALLS [["git", "rev-parse", "--abbrev-ref", "HEAD"], ["npm", "run", "verify:predeploy"], ["git", "push", "-u", "origin", "feature/audit"], ["gh", "pr", "view", "feature/audit"], ["gh", "pr", "create", "--base", "main", "--head", "feature/audit", "--title", "审计标题", "--body", "审计正文"]]
HOOK feature exit 0 INPUT refs/heads/feature/a aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa refs/heads/feature/a bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb MESSAGE 
HOOK to-main exit 1 INPUT refs/heads/feature/a aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa refs/heads/main bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb MESSAGE [pre-push] 拒绝直推 main。请走 PR 流程(ops/160/propose-change.sh)。
[pre-push] 确需直推:ALLOW_MAIN_PUSH=1 git push ...(并在 PR/说明记录原因)。
HOOK main-to-feature exit 1 INPUT refs/heads/main aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa refs/heads/feature/a bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb MESSAGE [pre-push] 拒绝直推 main。请走 PR 流程(ops/160/propose-change.sh)。
[pre-push] 确需直推:ALLOW_MAIN_PUSH=1 git push ...(并在 PR/说明记录原因)。
HOOK allow exit 0 INPUT refs/heads/feature/a aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa refs/heads/main bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb MESSAGE [pre-push] ALLOW_MAIN_PUSH=1 覆盖:放行直推 main(已留痕提醒)。
INSTALL default exit 0 git_expected install-default/.git/hooks/pre-push effective_exists True stdout installed pre-push hook -> .git/hooks/pre-push
INSTALL relative exit 0 git_expected install-relative/.custom-hooks/pre-push effective_exists True stdout [install-hooks] 检测到 core.hooksPath=.custom-hooks,安装到该路径(否则钩子不生效)。
installed pre-push hook -> .custom-hooks/pre-push
INSTALL absolute exit 0 git_expected install-absolute/absolute-hooks/pre-push effective_exists True stdout [install-hooks] 检测到 core.hooksPath=/home/james/complex-management/.scratch/m007-b1-u6j757s3/install-absolute/absolute-hooks,安装到该路径(否则钩子不生效)。
installed pre-push hook -> /home/james/complex-management/.scratch/m007-b1-u6j757s3/install-absolute/absolute-hooks/pre-push
INSTALL tilde exit 0 git_expected home-tilde/custom-hooks/pre-push effective_exists False stdout [install-hooks] 检测到 core.hooksPath=~/custom-hooks,安装到该路径(否则钩子不生效)。
installed pre-push hook -> ~/custom-hooks/pre-push
HARNESS_FINISHED: source copies unchanged; all expected observed outcomes asserted; three defect candidates are observations, not PASS.

````

## 工作树与提交实际不一致复现

````python
from pathlib import Path
import subprocess,tempfile,os,json,shutil
r=Path('/home/james/complex-management')
with tempfile.TemporaryDirectory(prefix='m007-dirty-',dir=r/'.scratch') as td:
 t=Path(td);repo=t/'repo';repo.mkdir();bin=t/'bin';bin.mkdir()
 env={**os.environ,'GIT_CONFIG_GLOBAL':'/dev/null','GIT_CONFIG_NOSYSTEM':'1'}
 def run(a,**kw):return subprocess.run(a,cwd=repo,env=env,text=True,capture_output=True,**kw)
 for args in [['init','-q','-b','feature/check'],['config','user.name','Isolated audit'],['config','user.email','audit@example.invalid'],['config','commit.gpgSign','false']]:
  assert run(['/usr/bin/git',*args]).returncode==0
 (repo/'gate.txt').write_text('bad\n')
 (repo/'package.json').write_text(json.dumps({'scripts':{'verify:predeploy':"node -e \"process.exit(require('fs').readFileSync('gate.txt','utf8').trim()==='good'?0:1)\""}}))
 assert run(['/usr/bin/git','add','.']).returncode==0
 assert run(['/usr/bin/git','commit','-qm','isolated fixture']).returncode==0
 p=run(['npm','run','verify:predeploy']);print('COMMITTED_VERSION_GATE_EXIT',p.returncode);assert p.returncode==1
 (repo/'gate.txt').write_text('good\n')
 (bin/'git').write_text('#!/usr/bin/env bash\nif [[ "$1" == push ]]; then /usr/bin/git show HEAD:gate.txt > "'+str(t/'pushed-content')+'"; exit 0; fi\nexec /usr/bin/git "$@"\n')
 (bin/'gh').write_text('#!/usr/bin/env bash\nif [[ "$1 $2" == "pr view" ]]; then exit 1; fi\nif [[ "$1 $2" == "pr create" ]]; then exit 0; fi\nexit 97\n')
 for p in bin.iterdir():p.chmod(0o755)
 env['PATH']=str(bin)+':'+os.environ['PATH']
 print('REAL_STATUS',run(['git','status','--porcelain']).stdout.strip())
 p=run(['bash',str(r/'ops/160/propose-change.sh'),'隔离测试'])
 print('PROPOSE_REAL_DIRTY_EXIT',p.returncode);print(p.stdout,p.stderr)
 print('PUSHED_COMMIT_CONTENT',(t/'pushed-content').read_text().strip())
 print('TESTED_WORKTREE_CONTENT',(repo/'gate.txt').read_text().strip())
 assert p.returncode==0 and (t/'pushed-content').read_text().strip()=='bad' and (repo/'gate.txt').read_text().strip()=='good'
print('NO_REAL_PUSH_OR_PR; only isolated fixture commit')

````

````text
COMMITTED_VERSION_GATE_EXIT 1
REAL_STATUS M gate.txt
PROPOSE_REAL_DIRTY_EXIT 0
[propose] 本地跑 predeploy 门禁(红则不推)...

> verify:predeploy
> node -e "process.exit(require('fs').readFileSync('gate.txt','utf8').trim()==='good'?0:1)"

[propose] 推 feature 分支 feature/check ...
[propose] 下一步:①独立复核(codex/评审)②合并一律走 ops/160/merge-pr.sh
[propose]   merge-pr.sh 会先等 GitHub Actions predeploy-gates 对本 commit 变绿再 gh pr merge;
[propose]   本仓是私有库免费版设不了 required status check,别再手敲 gh pr merge 绕过这道检查。
 
PUSHED_COMMIT_CONTENT bad
TESTED_WORKTREE_CONTENT good
NO_REAL_PUSH_OR_PR; only isolated fixture commit

````

## 补充当前验证与源码哈希

````python
上述命令：npm run verify:delivery-gates-docs；读取package.json父链；sha256sum所有范围/三HTML/锁；git config --get core.hooksPath；git check-ref-format 'refs/heads/with space'
````

````text
CMD npm run verify:delivery-gates-docs EXIT 0

> entertainment-complex-management@0.1.0 verify:delivery-gates-docs
> node tools/verify-delivery-gates.mjs

DELIVERY_GATES_DOCS_PASS
 
CI_PARENT_LINKS 15 MISSING []
FINAL_SOURCE_SHA ops/160/install-git-hooks.sh 677 badf746b715a52239361a80671cf863a8aeadaf66b52bc579741a88138ea25d3
FINAL_SOURCE_SHA ops/160/merge-pr.sh 4199 8688f7dc7e9b688c2d8583ff1942137089df55dbb9e5cfda95f69f344edbd514
FINAL_SOURCE_SHA ops/160/propose-change.sh 1437 913a12cae30d4448ac546d56ed54c9b25e7687c966a3181bd9324ebd194865e1
FINAL_SOURCE_SHA ops/160/hooks/pre-push 997 46aabdf33cb3496d83a71e4357ca85ba2ed9dbbfd4582ab53747bc50bb748d6b
FINAL_SOURCE_SHA ops/ci/predeploy-gates.yml 1335 4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d
FINAL_SOURCE_SHA docs/business-system-map-for-operations-v0.1.html 304482 a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa
FINAL_SOURCE_SHA docs/business-enum-quick-reference-v0.1.html 133961 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66
FINAL_SOURCE_SHA docs/platform-governance-map-for-operations-v0.1.html 103015 c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c
FINAL_SOURCE_SHA docs/audit-2026-09/plan/criteria-lock.md 889020 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
CURRENT_HOOK_CONFIG_EXIT 1 VALUE 
ACTUAL_HOOK_EQUAL True
Git ref space exit 1
SELF_EXISTS False

````

## 当前全部源码（156行完整覆盖）

### ops/160/install-git-hooks.sh SHA256 badf746b715a52239361a80671cf863a8aeadaf66b52bc579741a88138ea25d3
````text
1: #!/usr/bin/env bash
2: # 安装仓库 git 钩子到生效的 hooks 目录(worktree 共用)。新克隆/新机首次须跑一次。
3: # 注意:若仓库/全局设了 core.hooksPath,Git 只认该路径——装到别处会静默失效,故在此显式处理。
4: set -euo pipefail
5: cd "$(dirname "$0")/../.."
6: custom="$(git config --get core.hooksPath || true)"
7: if [[ -n "$custom" ]]; then
8:   dest="$custom"
9:   echo "[install-hooks] 检测到 core.hooksPath=$custom,安装到该路径(否则钩子不生效)。"
10: else
11:   dest="$(git rev-parse --git-common-dir)/hooks"
12: fi
13: mkdir -p "$dest"
14: install -m 0755 ops/160/hooks/pre-push "$dest/pre-push"
15: echo "installed pre-push hook -> $dest/pre-push"
````

### ops/160/merge-pr.sh SHA256 8688f7dc7e9b688c2d8583ff1942137089df55dbb9e5cfda95f69f344edbd514
````text
1: #!/usr/bin/env bash
2: # 文件版本：V0.01
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
44:     run_ids="$(gh run list --commit "$head_sha" --workflow predeploy-gates.yml --limit 10 --json databaseId -q '.[].databaseId' 2>/dev/null || true)"
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
````

### ops/160/propose-change.sh SHA256 913a12cae30d4448ac546d56ed54c9b25e7687c966a3181bd9324ebd194865e1
````text
1: #!/usr/bin/env bash
2: # 变更提议流程(2026-07-05 起替代直推 main):
3: #   推 feature 分支 → 开/更新 PR → (调用方另跑独立复核) → predeploy 门禁绿 → ops/160/merge-pr.sh。
4: #   合并动作一律走 merge-pr.sh(它会等 CI 绿),不要手敲 gh pr merge;CI 故障需 SKIP_CI_WAIT=1 时须在 PR 说明原因。
5: # 用法:在含已提交改动的 feature 分支上执行:ops/160/propose-change.sh "<PR标题>" ["<PR正文>"]
6: set -euo pipefail
7: title="${1:?需要 PR 标题}"; body="${2:-}"
8: branch="$(git rev-parse --abbrev-ref HEAD)"
9: [[ "$branch" == "main" ]] && { echo "不要在 main 上提议;先切 feature 分支。" >&2; exit 1; }
10: 
11: echo "[propose] 本地跑 predeploy 门禁(红则不推)..."
12: npm run verify:predeploy
13: 
14: echo "[propose] 推 feature 分支 $branch ..."
15: git push -u origin "$branch"
16: 
17: if gh pr view "$branch" >/dev/null 2>&1; then
18:   echo "[propose] PR 已存在,已更新:"; gh pr view "$branch" --json url -q .url
19: else
20:   gh pr create --base main --head "$branch" --title "$title" --body "${body:-自动创建;详见提交说明。独立复核见 PR 评论。}"
21: fi
22: echo "[propose] 下一步:①独立复核(codex/评审)②合并一律走 ops/160/merge-pr.sh"
23: echo "[propose]   merge-pr.sh 会先等 GitHub Actions predeploy-gates 对本 commit 变绿再 gh pr merge;"
24: echo "[propose]   本仓是私有库免费版设不了 required status check,别再手敲 gh pr merge 绕过这道检查。"
````

### ops/160/hooks/pre-push SHA256 46aabdf33cb3496d83a71e4357ca85ba2ed9dbbfd4582ab53747bc50bb748d6b
````text
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
````

### ops/ci/predeploy-gates.yml SHA256 4c20c029b7c5b0786d56164f2657444a9b9ee3d3694c18013f2e50c1e966049d
````text
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
````

最终本席结论：FAIL。下一步读取Grok本次完整报告、B1四值校验及B2整批归并；不得在此先修。


## M007 B2-r1 Codex 独立归并复核票
文件自身版本：V0.02（保留原45354bytes V0.01完整B1前缀，本段追加；不是新增B1审计）
结论：PASS（本席对候选归并/分类/队列的票；须与Grok合取才定稿）。
输入完整B1：modules/M007-b1-self-raw.md V0.01前缀45354bytes SHA74e5402de05dfa5154e4ae8bcb59de8ff441edd0baf7ee6fdf68fdc73a81dc03；modules/M007-b1-grok-raw.md V0.01前缀32337bytes SHA8ffc35495fe4328d34b643ae7fd6686241ba89014193bb4e0aeb27667cd53504。
候选：findings.md V1.07，362782bytes SHAbae7d7a4e9b32ae4731fa980009b6cd51bfcb6dfc924ee4f6bc09878ef4a0f21，当前M007 B2-r1完整001–011。本席独立检查候选未变、11ID唯一，所有CX01–05/GR01–02/R1–R9齐全；候选队列001–004是，005–011否。未读取本轮Grok B2追加意见前写下本票。

| ID | 本席票与依据 |
|---|---|
| M007-001 | PASS：合法四字段输入本地main→远端feature一次即误拒，无第二偶发条件；只改比较字段可恢复既有“远端main”职责，不加新门。保留Grok B1仅登记原话并请求独立重新判断，不覆盖其票。 |
| M007-002 | PASS：真实Git配置~/custom-hooks，Git生效目标与安装目录不同，exit0但无有效hook。默认/相对/绝对对照成立。技术路径解析缺陷。 |
| M007-003 | PASS：CX03/GR01为同一树版本错配，真实Git/npm复现已证明，不仅靠stub断言。工作区干净前提足够，不增加新参数。 |
| M007-004 | PASS：GR02事实独立补验，gh exit2，30次重试，原错误不可见，最终仍未合并。错误定位缺陷成立，收窄为非0退出时保留错误即可。 |
| M007-005 | PASS：既有merge保护/覆盖提示技术性，不改业务留痕或新增审批。GR02被拆为004，不遗漏原CX04其它正常路径。 |
| M007-006 | PASS：CI镜像必要且当前一致。R8是验证限制，不是被证明有错；本席现补读官方GitHub tag对象确认两SHA等于注释版本，见下文；不能冒称Grok B1也核过。R9不删除。 |
| M007-007 | PASS：测试按当前缺陷写最小可失败行为检查即可，不能因测试文件归M008判本模块业务遗漏。 |
| M007-008 | PASS：CLI英文不等于产品中文界面冲突，无独立缺陷则不“顺手改”。 |
| M007-009 | PASS：多个run全AND组合限制仅登记，不凭假设改策略。 |
| M007-010 | PASS：propose已固定base main，偏离正道的PR目标问题没有常规首跑缺陷证据，不扩门。 |
| M007-011 | PASS：未有实际正常输入导致缺少set-euo误放的证据，不为理论路径加兜底。 |

三HTML未改，无添加/冲突；技术性逐项无业务发明，001–004有工程缺陷，005–011只登记。所有finding仅用冻结GEN及既有HTML分类条目；不扩判据。全批修复只针对正式定稿是的ID及可归因最小测试；任何文件更新要进自身版本；不改CI镜像、显式覆盖、HTML或其它模块业务。B3仍为0。

补验GR02命令：使用本B1相同原脚本/隔离fake边界，仅将gh run list设置为stderr AUDIT_QUERY_AUTH_FAILURE并exit2；原脚本执行结果如下。
```text
LIST_FAILURE_EXIT 1 LIST_ATTEMPTS 30 MERGED False CAUSE_VISIBLE False
FINAL_STDERR [merge] 等了 5 分钟仍没有 aaaaaaaa 的 predeploy-gates run,中止合并。
        可能原因:workflow 被禁用、push 的不是这个 commit、或 Actions 排队异常。
        CI 确实故障时可用 SKIP_CI_WAIT=1 强行合并(会打印警告并需在 PR 说明原因)。


```
官方tag只读核验（gh api repos/actions/checkout/git/ref/tags/v4.3.1；gh api repos/actions/setup-node/git/ref/tags/v4.4.0）：两次exit0，均type=commit；分别34e114876b0b11c390a56381ad16ebd13914f8d5、49933ea5288caeca8642d1e84afbd3f7d6820020，与当前YAML一致。无远端写入。这里只消除本席R8相关不确定性，保留Grok历史限制。
最终本批本席票：PASS。下一步才读取Grok本轮B2全文并合取，未先修复。
