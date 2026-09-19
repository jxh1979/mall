CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M013 B5-r4 席甲（Claude）全模块三线审计
文件版本：V0.01
审计结论：FAIL
当前步骤：B5-r4；M013/471；ledger 为 IN_PROGRESS；B3 已执行 4 次（3 次额度 + 操作员 2026-09-19 批准追加的 1 次，均已使用）。本票只代表席甲；Grok 本轮报告落盘并完成 B5 后四值校验后才统一判定。

## 1. 范围、身份与独立性
- 席甲执行者：Claude（claude-fable-5-1，接续本轮早先由 claude-opus-5 执行的 B2/B3/B4 工作），只读审计，不改业务文件、HTML、锁、findings、ledger；不提交、不推送。Grok 席乙不变。
- 正本 /home/james/complex-management，main，HEAD e456ba96c84185c8d39f18f548011daac48a6466；审计对象为主工作区当前内容（含已暂存未提交修复）。B3-r4 after 树 ddff52dad5669cf17a598b75490df0bcfb454429（临时索引，未动主索引）。
- 输入：findings V2.33（019–023 已于 B4-r4 双 PASS 后转否，001–023 全否）；B4-r4 双票 self SHA256 4d7a6f5b77b77cc2d683360b5a024f65c4b081486d5caf95abb0e4cc06eea1c7、Grok SHA256 f5ca9babcc4b1838a3c2fc07c464dc5ba33d394f15d74c897882410b312751d7（62894 bytes，实际 EXIT_CODE=0）。
- 独立性声明：本报告落盘前未读取 modules/M013-b5-r4-grok-raw.md 正文，也未接触任何 Grok 原始 JSON。可读旧 B1/B2/B4/B5 报告。未派子代理、未用 MCP、未问操作员、未边审边修。
- 判据锁：criteria-lock.md V0.11 本席在 160 实算 b012328f…（见首行）；sidecar、ledger 基准、modules/M013-b5-r3-grok-raw.md 末条同值；未扩判据，LOCK-AMEND-COUNT=1。M013 映射 84 项（BEQR 21、BSM 12、GEN 18、PGM 33）。
- 技能如实记录：未经技能工具调用 grilling / verification-before-completion；按提示词 §6 收窄口径的结构自行执行（按意图执行）：事实自读、逐证核查、不派子代理、不问操作员、不边审边修。

## 2. 四文件全文覆盖
| 文件 | 实际通读行 | SHA256 |
|---|---|---|
| docs/business-entity-binding-development-design-v0.1.md | 1–154 | 9cdec650f49335a42a8769aaea0fbfa0881f15b77994171b0d381538edf001b8 |
| docs/group-governance-rebuild-prd-v1.0.md | 1–117 | 7defd0840e09bd16b2bf35afe913141a9ebf3610e5d2d4c2153017311be35f52 |
| docs/platform-governance-acceptance-pack-v0.2.md | 1–291 | 56aca4616e8c77b5a8e474c597ebbc613a6b9e8b1aef9d3b5f73ca62693053a0 |
| docs/platform-governance-prd-v0.1.md | 1–971（分 3 段读完） | e0f00b02ab8e7e7ec4b049ce643bbfbcc235612e9e8b79e4975f60652ba1730c |
共 1533 行（计划冻结 1517，不回写）。本席在 160 取得四文件副本后 sha256 与上表相同、与 after 树 blob 一致。三份 HTML 当前哈希：ENUM 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66、BSM eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66、PGM a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1（与 B5-r3 相同，未改）。

## 3. 第一性原理三问
1. 为什么存在：同 B5-r3 §3.1——四份文档是 PGM-017/018/026（项目归属、多对多、四类主体唯一绑定）、PGM-041/042（权限、职级、审批）、BSM-004/007/028 到产品与实现的中间口径；「当前实现/差距」栏是给接入方读的事实陈述，必须与源码、迁移一致。
2. 声称是否被实际验证：文档类产物不强制跑测试（runbook §5）。本席对 B3-r4 新写入的全部当前实现声称逐条追到源码/迁移（类型 4，只读）：V427 `CREATE TABLE business_entity_projects`（迁移 L13）；创建主体经 `creationProject(request.projectId(), true)`（PlatformService:121，最高权限可指定已存在项目 :419–429）；列表 `listBusinessEntitiesPaged(currentProject())`（:92）；`POST /business-entities/{entity_id}/projects`（PlatformController:97；服务 :363–381 `requirePlatformScope` + `isMasterAuthority`）；`SUBJECT_STATUSES` 五值（:54–56）；`requireSubjectStatus`（:893–900）；OpenAPI `BusinessEntityRequest.status` 四值；V392 六类 CHECK 与 AccessService:56–57/:1580–1585 申请入口三类；PGM L658/L669 与 AccessService:1305–1313 改职级重算。B5-r3 已核的其余声称（错误码、唯一索引、V410 三值、集团总览守卫、访问治理端点、lifecycle、档位映射、自审拒绝、准入 evaluate、字典八码、31 个权限码）本轮未改动、抽核未回归。以上除 §5 一条外全部一致。
3. 未经核实就当真的前提：「主体状态 `archived` 走存档端点」——L49 括注把共用校验函数的提示语当成了主体存档入口存在的证据；实查 PlatformController 对 business-entities 只有 GET / POST / POST projects 三个入口，没有存档端点（见 §5）。B5-r3 的三个前提（DEC-R8-91 排除责任范围、CHECK 四类、所有对象带 project_id）已由 019/020/021 消除。

## 4. 顺从式与对抗式全审
顺从式：逐节核对四文档对映射判据的承接。绑定设计 §1–§2、§3.2、§4、§6–§8 与 BEQR-143/144/155、PGM-018/026、BSM-004 一致；§3.1 新增 V427 关联句、§5 新增关联端点行、§8 新增 V427 与代码一致；§3.1 status 行括注见 §5。集团 PRD §0–§4 业务句与 PGM-041 一致；§1 表 request_type 六类与 V392 一致，§3 三类入口与两类服务内生成句与 AccessService 一致；§3 角色装配句删除职级/责任范围排除后与 PGM L658/L669 同向；§变更记录三条按时序追加。验收包 §2A/§4–§7/§10A 与 HTML 七类红线、终端十一端、开关优先级、ENUM 状态闭集一致；G7 删去「所有对象继续携带 project_id」后说明栏与 PGM L412–413 要求句、绑定设计 §3 实况一致；§11 新行接在 2026-09-14 之后。主 PRD §2A–§15E 逐节核对，§6.10 第 9 条、§7.8 验收标准、§7.8.2、§12 第 14B 条四处删除后句子完整，与 PGM L658/L669、AccessService:1305 同向；§12 第 6 条「职级只绑定薪资大范围」、第 11 条「职级只影响默认权限模板和审批层级建议」与 PGM-042 一致（O-B5-01 只登记）；§16 新行与 §6.10/§7.8/§7.8.2/§12 位置对应。历史记录（集团 PRD L5、PRD L966）与本次日志行引述被删词属当时记录，不构成现行规则。
对抗式（按 grilling 追问结构自问自答）：
- 「接入方按文档实现会不会做错？」→ 职级/申请类型/项目归属三处已消除。新：按绑定设计 L49「归档走存档端点」去找主体存档接口会得到 404（§5 CX-B5R4-01）。
- 「完成声明能否复现？」→ V427 关联表、关联端点、列表过滤、创建关联均可在源码/迁移复现；G7 假完成声明已删；L49 括注不可复现。
- 「同一文档内是否自相矛盾？」→ 集团 PRD L5 与 L76 已同向。新：绑定设计 L49「归档走存档端点」与 L108「完整存档/删除流程未在当前接口落地」、L121「也不存在主体完整 CRUD」相反（并入 CX-B5R4-01）。
- 「修复是否越界或发明规则？」→ B4-r4 双 PASS 已核 22 处逐字落地、无越界；019 未把职级写入正向触发清单；021 未新写要求句；022 保留「主体表本身无 project_id 列」；023 未改 OpenAPI。
- 「本轮是否把观察升级成缺陷或重开已批口径？」→ 否。O-B5-01/O-B5-03、BSM-114/120 旧摘录、009→M392、017→M348 保持登记。
- 旧已修 001–008、011–016、018 抽查未回归（B4-r4 diff 仅 22 处）。

## 5. 本轮新发现（阻断）
### CX-B5R4-01 绑定设计 L49 括注「归档走存档端点」：主体档案当前没有存档端点，与同文 L108/L121 相反
- 位置：docs/business-entity-binding-development-design-v0.1.md:49「| `status` | `VARCHAR(32)` | 当前代码状态，尚无 DB CHECK；服务只接受 `pending_admission` / `active` / `suspended` / `terminated` / `blacklisted`（归档走存档端点） |」。同文对照：:108「`business_entities` 有 `archive_reason`，完整存档/删除流程未在当前接口落地」；:121「当前不存在 `/active?businessObjectId=` 和 `/{id}/usages` 两条接口，也不存在主体完整 CRUD」；:57「当前 `status` 未实现该完整词表和缓冲期闭环」。
- 可复现证据（类型 4）：①PlatformController.java 全部 business-entities 映射仅三处：L42 `GET /business-entities`、L52 `POST /business-entities`、L97 `POST /business-entities/{entity_id}/projects`；存档/取消存档端点只有 L108/L117 `/merchants/{merchant_id}/archive|unarchive` 与 L190/L199 `/space-types/{id}/archive|unarchive`。②PlatformService.java:893–900 `requireSubjectStatus` 提示「状态必须为 待准入/正常/暂停/终止/黑名单 之一；归档请使用存档操作」，被 :119（创建主体）与 :1001（商户档案更新）共用；「存档操作」对商户成立，对主体档案没有对应入口。③JdbcPlatformRepository 无 business_entities 存档写法（`archiveBusinessEntity` / `UPDATE business_entities` 零命中）。④PlatformService.java:53 `SUBJECT_STATUSES` 注释原文「M009-004：BEQR-155 商户/运营主体可设状态（待准入/正常/暂停/终止/黑名单）；「归档」只经存档端点（archived_at 轴），不作可设代码。」——该注释针对商户（M009-004，商户有 archive/unarchive 端点），文档把它照搬到主体档案 `business_entities` 的字段表上；主体档案当前没有对应端点。
- 来源：该括注为 B3-r4 按 M013-023 定稿拟改写入（B2 两席与 B4-r4 两席均以服务提示语为据接受，未核主体存档入口是否存在）；023 已于 B4-r4 双 PASS 后转否，现为「有 ID 但不在修复队列」。
- 正常触发：接入方读 §3.1 会认为主体 `archived` 可经存档端点写入，去调用不存在的接口；与 §4.3/§5 自相矛盾。
- 分类：细化（主体状态实现说明的当前事实订正；HTML 无相反规则，不是冲突；无业务发明，不是添加）。判据：GEN-002、GEN-016、GEN-001（同文自洽）、PGM-006。工程缺陷：成立（当前 API 事实误述）。
- 最小处置（二选一，交 B2 定）：A. 删去括注「（归档走存档端点）」，句末保留「`blacklisted`」——L57/L108/L121 已如实登记归档与存档流程差距，不需重复；B. 改为「（`archived` 不由该字段写入；主体存档流程未在当前接口落地，见 §4.3）」。本席建议 A（同 021 先例：删不可复现声明、不扩写）。版本 V0.12→V0.13。不改代码、HTML、OpenAPI。
- 与 B3 额度：M013 B3 累计 4/4（含获批追加一次）。本条须先回 B2 定稿再修；本席不自行修复，按提示词记本轮 FAIL，是否再追加由操作员定。

## 6. 非阻断观察（只登记，沿 M013-010 续记）
- O-B5R4-01：`requireSubjectStatus` 提示语「归档请使用存档操作」对主体档案调用方是误导（代码侧措辞，属后台实现模块范畴，本模块文档不据此立法）；建议在收口文件 cross_module_observations 登记到 PlatformService 所属模块，不在本模块修。
- O-B5-01、O-B5-03（B5-r3）维持只登记；019 同根句在 M014/M421/M439、023 OpenAPI status 四值→M392 已在 findings V2.31 候选段登记为跨模块观察，收口时写入收口文件。

## 7. 84 项判据矩阵（较 B5-r3 变化处标注）
|判据|本模块承接与证据|结论|
|---|---|---|
|BEQR-063|集团 PRD §3 高危业务例举码未改|PASS|
|BEQR-074|四文档无承接，无冲突陈述|PASS|
|BEQR-143|绑定设计 §1–§2 四类主体唯一绑定与 HTML 一致|PASS|
|BEQR-144|绑定设计 §1 规则 2、§8 与 PRD §5 多对多一致；V410 已删父链|PASS|
|BEQR-145|PRD §5 城市项目公司职责与排除清单逐字对齐|PASS|
|BEQR-146|PRD §5 域主管公司逐家职责引 PGM 原文|PASS|
|BEQR-147|绑定设计 §2 权责、PRD §5 娱乐平台公司条一致|PASS|
|BEQR-148|六类合作模式与「其他」须指定收费模板一致|PASS|
|BEQR-149|PRD §6.10/§12 M1–M10 上中下|PASS|
|BEQR-150|十一端逐字一致|PASS|
|BEQR-151|最终权限公式一致；职级排除已删（019），与 PGM L658/L669 同向|PASS（较 r3 改）|
|BEQR-152|项目隔离与敏感数据口径一致；G7 假完成声明已删（021）|PASS（较 r3 改）|
|BEQR-153|开关优先级、兜底关闭、留痕不受影响一致|PASS|
|BEQR-154|项目六态一致|PASS|
|BEQR-155|商户六态、缓冲期 30–60/默认 30、客人三选一一致；绑定设计 L49 服务五值陈述本身与 SUBJECT_STATUSES 一致，括注问题归 GEN-002/016|PASS|
|BEQR-156|合同八态、当地时间一致|PASS|
|BEQR-157|员工五态一致|PASS|
|BEQR-158|设备七态一致|PASS|
|BEQR-159|权限五态一致|PASS|
|BEQR-166|合同审批链一致|PASS|
|BEQR-189|四文档无承接，无冲突陈述|PASS|
|BSM-004|存档、软删除、删前占用、最高权限审批、U 盘本人免审规则一致；主体存档流程差距在 L108 如实登记|PASS|
|BSM-005|四文档无承接|PASS|
|BSM-006|四文档无承接|PASS|
|BSM-007|平台管理员/最高权限两角色一致|PASS|
|BSM-009|四文档无承接|PASS|
|BSM-028|七类红线开放边界逐字一致|PASS|
|BSM-029|结算、设备绑定、权限归属相关陈述一致|PASS|
|BSM-072|业态六值一致|PASS|
|BSM-074|商户隔离与价格权限一致|PASS|
|BSM-114|私人手机「查看考勤」一致；锁旧摘录为已登记观察|PASS|
|BSM-115|基础管理连所有业务、先卡后放行一致|PASS|
|BSM-120|已拍板项一致；页脚日期为已登记观察|PASS|
|GEN-001|四文档用途明确；绑定设计 L49 与 L108/L121 自相矛盾|FAIL（CX-B5R4-01）|
|GEN-002|实现声称逐条追到源码/迁移，新增 V427/关联端点/列表过滤/五值/四值/六类均一致；L49 括注指向不存在的主体存档端点|FAIL（CX-B5R4-01）|
|GEN-003|文档类不强制测试|PASS|
|GEN-004|入参校验陈述与代码一致|PASS|
|GEN-005|授权陈述与 requirePermission/requirePlatformScope/isMasterAuthority 一致（含新增关联端点行）|PASS|
|GEN-006|唯一索引、CHECK、事务陈述与迁移一致（含 V392 六类、V427）|PASS|
|GEN-007|幂等陈述一致|PASS|
|GEN-008|request_type 六类与 CHECK 一致（020）；OpenAPI status/entity_role 裂口已披露归 M392|PASS（较 r3 改）|
|GEN-009|错误码陈述与代码一致|PASS|
|GEN-010|无秘密、无越权说明|PASS|
|GEN-011|留痕陈述一致|PASS|
|GEN-012|无资源边界陈述|PASS|
|GEN-013|无构建/依赖内容|PASS|
|GEN-014|迁移号与内容（含新增 V427、V392）实查存在|PASS|
|GEN-015|无界面实现|PASS|
|GEN-016|B5-r3 三处不可复现声明已消除；新增 L49 括注不可复现|FAIL（CX-B5R4-01）|
|GEN-017|职级排除已删，四文档无与 HTML 相反内容；L49 括注是实现事实误述、不是业务发明|PASS（较 r3 改）|
|GEN-018|本报告全部结论落到 HTML 原文或命令输出|PASS|
|PGM-002|治理顺序五步一致|PASS|
|PGM-003|四文档无承接|PASS|
|PGM-004|集团视图预留一致；G7 已订正|PASS（较 r3 改）|
|PGM-005|九类空间对象、字典与契约对照一致|PASS|
|PGM-006|商户、主体、联系人、合作方式一致；主体存档入口事实归 CX-B5R4-01（GEN-002）|PASS|
|PGM-007|准入先卡后放行一致|PASS|
|PGM-008|私人手机/工作手机、设备凭证口径一致|PASS|
|PGM-009|单一功能权限点一致|PASS|
|PGM-010|终端开关一致|PASS|
|PGM-011|设备台账与领还一致|PASS|
|PGM-012|商户侧边界一致|PASS|
|PGM-013|第三方身份核验陈述一致|PASS|
|PGM-014|留痕复核一致|PASS|
|PGM-015|楼宇保安一致|PASS|
|PGM-016|破窗、钥匙更换闭环一致|PASS|
|PGM-017|「所有基础对象带项目归属」为要求；G7 已删假完成；绑定设计 §3.1 补 V427 关联并保留「主体表无列」事实|PASS（较 r3 改）|
|PGM-018|多对多、域主管公司、城市项目公司一致|PASS|
|PGM-019|娱乐平台公司条逐字照抄|PASS|
|PGM-020|集团下钻由职位模板决定一致|PASS|
|PGM-021|集团总览业务意义一致|PASS|
|PGM-026|四类主体唯一绑定、权责三类、财税并审一致|PASS|
|PGM-027|商户侧全开放、本商户客户信息例外一致|PASS|
|PGM-031|经销商治理逐条一致|PASS|
|PGM-035|提成月结一致|PASS|
|PGM-040|单一功能授权一致|PASS|
|PGM-041|「HR 定层级/职位即自动适配」不再被排除条款抵触（019）|PASS（较 r3 改）|
|PGM-042|「职级影响默认权限模板」不再被排除条款抵触（019）；PRD §12 第 6/11 条一致|PASS（较 r3 改）|
|PGM-043|外包、审计人员、导出脱敏一致|PASS|
|PGM-044|跨范围每次审批与 PRD §6.10 一致|PASS|
|PGM-048|商户侧开放范围一致|PASS|
|PGM-060|会员与人脸口径一致（仅引用）|PASS|
|PGM-065|治理连接各业务一致|PASS|
|PGM-066|已拍板项一致|PASS|

## 7A. 结论
三线均已完成、四文件全覆盖、84 项判据逐项核对。B5-r3 的 5 条（019–023）修复已落地并与 HTML、源码一致，未发现回归。新发现 1 条阻断（CX-B5R4-01，细化，工程缺陷成立，由 023 拟改括注引入），有 ID（023）但不在修复队列，按回退统一规则须先回 B2 定稿；M013 B3 累计 4/4（含获批追加），本次 B5 审完仍 FAIL 时按提示词记 BLOCKED 并交操作员。本席不以自审 PASS 覆盖任何一方，也不因额度用满或获批追加降低标准。

结论：FAIL。
