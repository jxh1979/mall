CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M013 B5-r5 席甲（Claude）全模块三线审计
文件版本：V0.01
审计结论：PASS
当前步骤：B5-r5；M013/471；ledger 为 IN_PROGRESS；B3 已执行 5 次（3 次额度 + 操作员 2026-09-19 两次批准追加各 1 次，均已使用）。本票只代表席甲；Grok 本轮报告落盘并完成 B5 后四值校验后才统一判定。

## 1. 范围、身份与独立性
- 席甲执行者：Claude（claude-fable-5-1），只读审计，不改业务文件、HTML、锁、findings、ledger；不提交、不推送。Grok 席乙不变。
- 正本 /home/james/complex-management，main，HEAD e456ba96c84185c8d39f18f548011daac48a6466；审计对象为主工作区当前内容（含已暂存未提交修复）。B3-r5 after 树 2720a4e02b917ab6e88b5ba20d4912d59c007fae（临时索引，未动主索引）。
- 输入：findings V2.37（024 已于 B4-r5 双 PASS 后转否，001–024 全否）；B4-r5 双票 self SHA256 9ca77c99acb5aa25df8fcd197fffb6df67433cad951244adb7aabcb26385f575（5351 bytes）、Grok SHA256 573147e3fddce301518324c90102370e505b438bcb226d8419119748cb5b7d67（26592 bytes，实际 EXIT_CODE=0）。
- 独立性声明：本报告落盘前未读取 modules/M013-b5-r5-grok-raw.md 正文，也未接触任何 Grok 原始 JSON。可读旧 B1/B2/B4/B5 报告。未派子代理、未用 MCP、未问操作员、未边审边修。
- 判据锁：criteria-lock.md V0.11 本席在 160 实算 b012328f…（见首行）；sidecar、ledger 基准、modules/M013-b5-r4-grok-raw.md 末条同值；未扩判据，LOCK-AMEND-COUNT=1。M013 映射 84 项（BEQR 21、BSM 12、GEN 18、PGM 33）。
- 技能如实记录：未经技能工具调用 grilling / verification-before-completion；按提示词 §6 收窄口径的结构自行执行（按意图执行）：事实自读、逐证核查、不派子代理、不问操作员、不边审边修。

## 2. 四文件全文覆盖
| 文件 | 实际通读行 | SHA256 | 较 B5-r4 审本 |
|---|---|---|---|
| docs/business-entity-binding-development-design-v0.1.md | 1–154（本轮全文重读） | b85d4d42a6bf7bfcdba197da90cf132ed07b8012080a311c3110f97506f25143 | 变：L1/L3 V0.13、L49 删括注（024） |
| docs/group-governance-rebuild-prd-v1.0.md | 1–117（B5-r4 全文审本，本轮 SHA 相同，抽核 §1/§3/§变更记录未变） | 7defd0840e09bd16b2bf35afe913141a9ebf3610e5d2d4c2153017311be35f52 | 不变 |
| docs/platform-governance-acceptance-pack-v0.2.md | 1–291（B5-r4 全文审本，本轮 SHA 相同，抽核 §10A G7/§11 未变） | 56aca4616e8c77b5a8e474c597ebbc613a6b9e8b1aef9d3b5f73ca62693053a0 | 不变 |
| docs/platform-governance-prd-v0.1.md | 1–971（B5-r4 全文审本，本轮 SHA 相同，抽核 §6.10/§7.8/§12/§16 未变） | e0f00b02ab8e7e7ec4b049ce643bbfbcc235612e9e8b79e4975f60652ba1730c | 不变 |
共 1533 行（计划冻结 1517，不回写）。B4-r5 完整 diff 证明 B3-r5 只改绑定设计 3 行，其他三文件字节与 B5-r4 审本全等，故 B5-r4 对三文件的全文三线结论直接适用；本席仍按 84 项逐条复核。三份 HTML 当前哈希：ENUM 4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66、BSM eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66、PGM a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1（未改）。

## 3. 第一性原理三问
1. 为什么存在：同 B5-r3/r4 §3.1——四份文档是 PGM-017/018/026、PGM-041/042、BSM-004/007/028 到产品与实现的中间口径；「当前实现/差距」栏是给接入方读的事实陈述，必须与源码、迁移一致。
2. 声称是否被实际验证：文档类产物不强制跑测试（runbook §5）。本轮唯一改动为删除一条不可复现声明，无新增声称。B4-r5 后只读复核：PlatformController 对 business-entities 映射仍仅 L42 GET、L52 POST、L97 POST projects；PlatformService:54–56 `SUBJECT_STATUSES` 五值与 L49 一致；L55 status 四值契约缺口披露、L56 V427 关联、L116 关联端点、L154 V427 依据均与源码/迁移一致（B5-r4 已逐条核，本轮 SHA 未变部分不重列）。
3. 未经核实就当真的前提：B5-r4 的前提（主体存档端点存在）已由 024 消除。本轮再问：「删掉括注后归档差距是否失去承载？」→ L57「当前 status 未实现该完整词表和缓冲期闭环」与 L108「完整存档/删除流程未在当前接口落地」仍在，差距有承载，不需新句。

## 4. 顺从式与对抗式全审
顺从式：绑定设计 §1–§8 逐节复核：§3.1 status 行现只陈述五值；§3.1 差距三条（V410/OpenAPI 双裂口、V427 关联、HTML 六态与缓冲期差距）与代码一致；§3.2/§4/§5/§6/§7/§8 未变，与 B5-r4 结论一致。其他三文件 SHA 未变，B5-r4 顺从式结论（集团 PRD 六类/三类入口/角色装配同向；验收包 G7；PRD 四处删除后同向、§12 第 6/11 条与 PGM-042 一致）保持。
对抗式（按 grilling 追问结构自问自答）：
- 「接入方按文档实现会不会做错？」→ 主体存档入口误导已删；其余入口（GET/POST/POST projects、绑定三接口）与 Controller 一致。
- 「完成声明能否复现？」→ 本轮无新增完成声明；版本 V0.13 可验证（L1=L3）。
- 「同一文档内是否自相矛盾？」→ L49 与 L57/L108/L121 不再相反；L55 023 披露与 L49 五值并存无冲突（一个是契约、一个是服务）。
- 「修复是否越界或发明规则？」→ B4-r5 双 PASS 已核 3 处逐字、仅一文件；未改走方案 B、未重开 023、未写「主体存档已实现」。
- 「凡写端点/入口的句子是否都按对象核过 Controller？」→ 本轮对绑定设计全部端点句逐条核：L90/L99/L114–L119/L121 均与 PlatformController business-entities / business-entity-bindings 映射一致（GET/POST/POST projects；GET/POST/POST change）；L121 明写不存在的两条接口与主体完整 CRUD，与映射一致。
- 「本轮是否把观察升级成缺陷或重开已批口径？」→ 否。O-B5-01/O-B5-03/O-B5R4-01、BSM-114/120 旧摘录、009→M392、017→M348 保持登记。
- 旧已修 001–023 未回归（B4-r5 diff 仅 3 行；三文件 SHA 未变）。

## 5. 本轮新发现（阻断）
无。

## 6. 非阻断观察（只登记，沿 M013-010 续记；收口时写入收口文件 cross_module_observations）
- O-B5R4-01（维持）：PlatformService `requireSubjectStatus` 提示语「归档请使用存档操作」对主体档案调用方误导，代码措辞归 PlatformService 所属后台模块。
- 019 同根句：M014 docs/platform-governance-product-spec-v0.2.md:1979；M421 docs/business-html-source-of-truth-acceptance-spec-v0.1.md:67；M439 docs/product-requirements-v0.1.md:601、:650（:688 为历史日志）。
- 023：docs/openapi/openapi-p0.yaml BusinessEntityRequest.status 四值 → M392（同 009 entity_role 六值）。
- O-B5-01、O-B5-03（B5-r3）维持只登记。

## 7. 84 项判据矩阵（较 B5-r4 变化处标注）
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
|BEQR-151|最终权限公式一致；职级排除已删（019）|PASS|
|BEQR-152|项目隔离与敏感数据口径一致；G7 已订正（021）|PASS|
|BEQR-153|开关优先级、兜底关闭、留痕不受影响一致|PASS|
|BEQR-154|项目六态一致|PASS|
|BEQR-155|商户六态、缓冲期 30–60/默认 30、客人三选一一致；绑定设计 L49 五值与 SUBJECT_STATUSES 一致，L57 登记词表差距|PASS|
|BEQR-156|合同八态、当地时间一致|PASS|
|BEQR-157|员工五态一致|PASS|
|BEQR-158|设备七态一致|PASS|
|BEQR-159|权限五态一致|PASS|
|BEQR-166|合同审批链一致|PASS|
|BEQR-189|四文档无承接，无冲突陈述|PASS|
|BSM-004|存档、软删除、删前占用、最高权限审批、U 盘本人免审规则一致；主体存档流程差距在 L108 如实登记，L49 不再指向不存在的入口|PASS|
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
|GEN-001|四文档用途明确；绑定设计 L49 与 L57/L108/L121 不再相反|PASS（较 r4 改）|
|GEN-002|实现声称逐条追到源码/迁移；绑定设计全部端点句与 Controller 映射一致|PASS（较 r4 改）|
|GEN-003|文档类不强制测试|PASS|
|GEN-004|入参校验陈述与代码一致|PASS|
|GEN-005|授权陈述与 requirePermission/requirePlatformScope/isMasterAuthority 一致|PASS|
|GEN-006|唯一索引、CHECK、事务陈述与迁移一致|PASS|
|GEN-007|幂等陈述一致|PASS|
|GEN-008|request_type 六类与 CHECK 一致；OpenAPI status/entity_role 裂口已披露归 M392|PASS|
|GEN-009|错误码陈述与代码一致|PASS|
|GEN-010|无秘密、无越权说明|PASS|
|GEN-011|留痕陈述一致|PASS|
|GEN-012|无资源边界陈述|PASS|
|GEN-013|无构建/依赖内容|PASS|
|GEN-014|迁移号与内容（含 V427、V392）实查存在|PASS|
|GEN-015|无界面实现|PASS|
|GEN-016|B5-r4 的 L49 不可复现括注已删；版本 V0.13 可验证；四文档无不可复现完成声明|PASS（较 r4 改）|
|GEN-017|四文档无与 HTML 相反内容；本轮无新缺陷需分类|PASS|
|GEN-018|本报告全部结论落到 HTML 原文或命令输出|PASS|
|PGM-002|治理顺序五步一致|PASS|
|PGM-003|四文档无承接|PASS|
|PGM-004|集团视图预留一致；G7 已订正|PASS|
|PGM-005|九类空间对象、字典与契约对照一致|PASS|
|PGM-006|商户、主体、联系人、合作方式一致；主体档案当前入口陈述与 Controller 一致|PASS|
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
|PGM-017|「所有基础对象带项目归属」为要求；G7 已删假完成；绑定设计 §3.1 补 V427 关联并保留「主体表无列」事实|PASS|
|PGM-018|多对多、域主管公司、城市项目公司一致|PASS|
|PGM-019|娱乐平台公司条逐字照抄|PASS|
|PGM-020|集团下钻由职位模板决定一致|PASS|
|PGM-021|集团总览业务意义一致|PASS|
|PGM-026|四类主体唯一绑定、权责三类、财税并审一致|PASS|
|PGM-027|商户侧全开放、本商户客户信息例外一致|PASS|
|PGM-031|经销商治理逐条一致|PASS|
|PGM-035|提成月结一致|PASS|
|PGM-040|单一功能授权一致|PASS|
|PGM-041|「HR 定层级/职位即自动适配」不再被排除条款抵触（019）|PASS|
|PGM-042|「职级影响默认权限模板」不再被排除条款抵触（019）；PRD §12 第 6/11 条一致|PASS|
|PGM-043|外包、审计人员、导出脱敏一致|PASS|
|PGM-044|跨范围每次审批与 PRD §6.10 一致|PASS|
|PGM-048|商户侧开放范围一致|PASS|
|PGM-060|会员与人脸口径一致（仅引用）|PASS|
|PGM-065|治理连接各业务一致|PASS|
|PGM-066|已拍板项一致|PASS|

## 7A. 结论
三线均已完成、四文件全覆盖（绑定设计本轮全文重读，其他三文件 SHA 与 B5-r4 全文审本相同）、84 项判据逐项核对。019–024 修复已落地并与 HTML、源码一致，未发现回归；本轮无新发现。001–024 全否不等于功能全实现：017 项目结果过滤缺口（M348）、009/023 契约裂口（M392）、完整审批链等差距仍按文档诚实登记。本席不以自审 PASS 覆盖任何一方，也不因两次获批追加或额度用尽降低标准。

结论：PASS。
