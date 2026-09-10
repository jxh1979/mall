CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U06 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 只读全量三线（未发布材料第 6 段；不做 B3/B4/B5；处置写给发布者，本轮不修源）
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 段：U06 — 未发布材料第 6 段「员工服务与数据访问」
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 末尾附录；`plan/runbook.md` V0.02 §11；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 审计对象（不得冒称 main 或已发布修复）：
  - 未发布提交：`03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb`（本仓 `git cat-file -t` **无此 commit 对象**）
  - 未发布树：`e28dad059b2c7e08bcc95a8db18552d3f76edf4b`（`git cat-file -t` = **tree**，可解析）
  - 发布基线：`242f487233d028173cb9198eecd079f5d877405c`（commit，可解析）
  - 只读输入根：`/home/james/complex-management/.scratch/module-recheck-20260908/unpublished-03acdb98`
  - 清单：`.scratch/module-recheck-20260908/unpublished-03acdb98-input.json` V0.01，76 文件
- 本段：9 文件，物理行 1664，标记 NORMAL；main 主归属 M054（StaffService、StaffRegistrationRequest）+ M288（StaffShiftRecord、StaffShiftBatchResult、StaffShiftRequest）；4 个新增 DTO main 无此文件
- 冻结判据：M054 与 M288 映射行并集 52 项（两行逐字相同；python 集合差集 MISSING=[] EXTRA=[]）：GEN-001…GEN-018、BEQR-157、BEQR-182…187、BSM-001/003/009/028/029/073/084–088/114/115/120、PGM-001/008/022/024/026/032–036/048/059/066
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 且发布前须修：GR-U06-001 … GR-U06-009（添加项 GR-U06-005 走 2.2，不代用户回答）
- 添加项（B1 只登记，不代用户回答 2.2）：GR-U06-005（考勤日结果多出「未打下班卡 / missing_checkout」）
- 同根已定稿、不新开 2.2：GR-U06-001＝U05-003/U01-007/U02-002；GR-U06-006＝U05-005；GR-U06-007＝U05-010；GR-U06-008＝U05-008 发布分叉面；GR-U06-009＝U05-001/U04-001（操作员已答删除独立自营布尔）
- 只登记、建议队列「否」：观察见 §8
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做写操作
- 本报告不表示全仓、main 模块或发布门禁通过。M001–M012 检查点已合入当前 HEAD（PR #428/#429）。原 322/471 模块结论不继承到本未发布段。不得把本副本冒称 main 源码或已发布修复。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`（1872 bytes，SHA256 `fa5c1e5ee76b1c8f1ae56101f52c9e239de75d5c578adc61227b92d10b7e52ef`） | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md`（SHA256 `b9339b09ee3980808d8c5a35c7251b891b8b1e0036ec4ca37812b976ebddf6b6`） | `/tmp/u06-b1-grok` Map.of 空值探针 + 把本段 9 文件盖到 main 源码树的离线 `mvn -o compile`；不改生产代码 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流（本段 9 文件 → U05 仓储/注册/审批 → U07 SecurityConfig → U08 MerchantRecord.selfOperated → U09 督导策略 → U10 V400–V409 → main 对照 → HTML） |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；未跑的后端集成 / 共享库测试不写通过 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度（字段注入、兼容构造器、未用 slug），不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md`（SHA256 `6f9ae7c1c295d7e67eda1e3a616c337c057f45f1a5bf2955efb07d539a655b79`） | 本段验证均秒级，未起 monitor |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：套件文档写明已并入 `grilling`，无独立技能可装
- `ubiquitous-language`：本机技能树未找到
- Arc `audit` 会派评审子代理：与双席/§7 冲突，**不调用**
- `using-superpowers` 要求技能优先于探索：与「禁止子代理 / 本提示指定 controller」冲突处，按用户指令收窄

明确不用：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、MCP、第三主体。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`，属正常）
- `criteria-lock.md` 版本 V0.11；判据总数 401
- 映射行：`M054 →` L6433、`M288 →` L6667，两行闭集逐字相同 52 项；python 集合相对开工提示 MISSING=[] EXTRA=[]

HEAD（独立实算）：

- `git rev-parse HEAD` = `git rev-parse origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`
- `git log -1 --oneline`：`445a49f4 Merge pull request #429 from jxh1979/audit/20260909/M011-M012-checkpoint`
- 本席对照 main 已修项时以 **当前 HEAD=445a49f4** 为准（含 M001–M012）
- 本席未设 `GIT_INDEX_FILE`，未动主索引

ledger 只读核验（不改）：V2.58，478067 bytes，SHA256 `fc4be8e0539ee7972b7bc209c87610acbbe2a3b490cfde65c2fdb7999a75d2b5`（与开工提示一致）。findings V1.53，586122 bytes，SHA256 `34b4735567aabf72fa2ff63f662b89e653d597b1138d9afdf481ce6174608d16`（与开工提示一致）。台账中与本段直接相关的已定稿项（背景，不继承 PASS）：

- U04-001：`selfOperated` 第二轴为添加，操作员 2026-09-09 答「删除」；处置写明 **U05 `StaffRegistrationService` 改读 `cooperationForm`**
- U05-001 / U05-003 / U05-005 / U05-008 / U05-010：注册分流、自助注册设备标识、可见天数上下限、发布分叉同批、`company_id` 空值兼容——本段 StaffService/DTO 是其调用面
- U01-007 / U02-002：自助注册契约缺设备公钥/指纹（Java record 在本段）
- M009-003：main 合作方式六类技术码（改的是 `PlatformService` / 商户页，**未改** 本段五份同名 Java；见 §6）

本席独立对照见 §6，不继承其 PASS。

其它只读哈希：

| 文件 | 字节 | SHA256 |
|---|---:|---|
| `plan/runbook.md` V0.02 | 10409 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `plan/modules.md` V0.04 | 421826 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |
| `160-audit-prompt.md` V0.17 | 65856 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `unpublished-03acdb98-input.json` V0.01 | 17547 | `10eb8240ee08ee2fae053235250f19335cacc2ff8d9a2ed5edbe69be366293e9` |
| `docs/business-enum-quick-reference-v0.1.html` | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 304482 | `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 103015 | `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c` |

三 HTML 只读、未改。锁内本段用到的锚点行对本席读取的现行 HTML **全部命中**（见 §1.1）。三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。

清单本段 9 条与独立 `sha256sum`/`wc`/`git cat-file` **9/9 match**。副本 SHA 与树 `e28dad05:<path>` blob **9/9 相同**。全 76 文件相对基线 numstat 合计 **+4745 / −1084**（独立 `git diff --shortstat` 实算，与开工提示一致）。

对象核验：`git cat-file -t 03acdb98…` fatal（无 commit）；`e28dad05…` = tree；`242f4872…` = commit。

### 1.1 HTML 锚点实核（锁出处行 → 现行正本）

| 判据 | 锁出处 | 现行该行含 |
|---|---|---|
| BEQR-157 | beqr:240 | `员工状态` / `待入职、在职、试岗、停岗、离职` |
| BEQR-182 | beqr:279 | `考勤日结果` 出勤/缺勤/迟到/早退/请假；无排班=休息 |
| BEQR-183 | beqr:280 | `打卡方式`（领取工作手机三方式，取最早一次） |
| BEQR-184 | beqr:281 | `请假类型` 事假、病假、年假、调休 |
| BEQR-185 | beqr:282 | `请假单状态` 含已撤销、已销假 |
| BEQR-186 | beqr:283 | `薪资单构成`（20% 上限、总薪资定义、事假另算、人力财务法务三审） |
| BEQR-187 | beqr:285 | `员工提成发放` 月结、银行、不许现金现抽 |
| BSM-001 | bsm:505 | `整体说明：这套系统到底管什么` |
| BSM-003 | bsm:563 | `系统通用能力：数据可改可删、操作有引导、权限分岗` |
| BSM-009 | bsm:596 | `基础管理` 员工/设备/权限 |
| BSM-028 | bsm:698 | `主要功能` 六类合作方式 + 员工注册审批 |
| BSM-029 | bsm:714 | 用户权限要知道员工注册信息、设备绑定… |
| BSM-073 | bsm:1313 | 商户授权范围内操作本商户员工 |
| BSM-084 | bsm:1427 | 员工手机端 h2 |
| BSM-085 | bsm:1431 | 日常工作；L1433 两条注册路径 + 自营视同公司自有 |
| BSM-086 | bsm:1441 | 排班上一级审批、可见天数出厂 7 天须上下限、打卡、请假、顶班、班次开始即锁定 |
| BSM-087 | bsm:1455 | 我的薪资（20% 上限、月结可见） |
| BSM-088 | bsm:1467 | 提成进月结、不许现抽 |
| BSM-114 | bsm:1827 | 各类入口只看自己该看的 |
| BSM-115 | bsm:1897 | 基础管理连所有业务 |
| BSM-120 | bsm:1957 | 已拍板：私人手机设备标识；寄存柜扫码自动留痕不设手工登记 |
| PGM-001 | pgm:256 | 治理层底座；见名可点 |
| PGM-008 | pgm:362 | 员工和手机入口卡 |
| PGM-022 | pgm:463 | 空间和点位 |
| PGM-024 | pgm:476 | 设备和员工班次要知道责任区域 |
| PGM-026 | pgm:492 | 六类合作方式 |
| PGM-032 | pgm:572 | 人、私人手机、工作手机要能对应起来 |
| PGM-033 | pgm:576 | 两条注册路径；安装时绑定设备标识 |
| PGM-034 | pgm:586 | 工作手机和企业微信（本段无企微实现面） |
| PGM-035 | pgm:599 | 提成进月结银行发放 |
| PGM-036 | pgm:611 | 设备绑定员工、班次、点位 |
| PGM-048 | pgm:749 | 商户侧授权范围内全开放、不能跨商户 |
| PGM-059 | pgm:868 | 个人信息加密存放；日常界面只显示脱敏号码 |
| PGM-066 | pgm:951 | 已拍板：界面全中文（所有终端零英文） |

对照用（非本段冻结 ID，不支撑 finding）：BEQR 合作模式六类技术码 `self_operated`（main M009-003）；U04-001 已答删除。BSM-004「申请人不得自审 / 最高权限本人发起不需审批」不在本段映射闭集，只作阅读背景。

## 2. 9 文件覆盖表（全文已读；行=`\n` 计数）

副本根：`unpublished-03acdb98/`。清单本段条目 sha256/行/字节与独立实算 **9/9 match**。副本与树 blob **9/9 相同**。文件以 `\n` 结尾、无 CRLF。

| # | 路径 | 行 | 字节 | SHA256 | 树 blob | 全文 |
|---|---|---:|---:|---|---|---|
| 1 | `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | 1553 | 97552 | `605e857533d44e16a0bb0db08b03488fc78e2f8aea13f6fc0ae27b3f53c28371` | `440ab03ee9d7bd0199e0b3fbeca5d8ea7180e1e1` | 已读 L1–L1553 |
| 2 | `…/StaffShiftRecord.java` | 33 | 1167 | `bd9d8aabf0003874ace4cff5bd86b21886e5190563a45d56f30d2ee4a6d3f27a` | `6393d5765f26a27600a1f0c9aca360f4cae5887e` | 已读 L1–L33 |
| 3 | `…/dto/StaffEmploymentStatusRequest.java` | 4 | 158 | `6f6298f7793df2586cf3a2d4ee0b31e7e50035b4fbe4a1cb285ea1c3abf556d8` | `70ab7c76217e05e2f8172edf8bb3bda3dac42d98` | 已读 L1–L4 |
| 4 | `…/dto/StaffNumericSettingRequest.java` | 4 | 182 | `484d62c04e1be7b2bd7f9a7d739efe886dde6f67be8619632b6f70dba0afd4c5` | `036cb1b187eecdcb3ac8e7a100019aeae4062aa1` | 已读 L1–L4 |
| 5 | `…/dto/StaffPenaltyCapRequest.java` | 9 | 442 | `ab6931adbb43e7ca9fe40709a31c1b184aa6d785013eb5cbb628350dd43787c9` | `635b2c7b4db523b7dceeab7379618c54ec446400` | 已读 L1–L9 |
| 6 | `…/dto/StaffRegistrationRequest.java` | 19 | 503 | `c258e6cc7f201e1299451b2744585547b487984cea187415d2dad5a2f5aa6b5f` | `d426ce5b52a52637613dacd1bd7904fb1e9f9126` | 已读 L1–L19 |
| 7 | `…/dto/StaffSelfRegistrationRequest.java` | 5 | 227 | `a601974a2fdf31b31da2d1aa0722c0338af7e6b13a0db6193d142a0baab58839` | `3696d160e46050bdf2bce38d3603cdfe2b1a747a` | 已读 L1–L5 |
| 8 | `…/dto/StaffShiftBatchResult.java` | 13 | 561 | `8962a3d6620e6463960a6cf79a46dcdbec62c0f52640e8b4a977567b454ece01` | `1ff73ddde0edde1e03df9a0e4c864374115fd2a6` | 已读 L1–L13 |
| 9 | `…/dto/StaffShiftRequest.java` | 24 | 763 | `acf512fc6c18a6e0f6504aba43d80492bcdf5b9060b49b391157eb04b882c9b2` | `99cc05d0986cacc4a35dcea14c011526671abaa1` | 已读 L1–L24 |

合计 1664 行 / 101555 字节。本段无测试文件、无迁移、无 OpenAPI（调用链只读到副本 U05/U07/U09/U10 与 main）。

### 2.1 相对发布基线的逐文件 diff stat（真实改动）

命令：`git diff --stat 242f487233d028173cb9198eecd079f5d877405c e28dad059b2c7e08bcc95a8db18552d3f76edf4b -- <本段 9 路径>`

```
 .../platform/access/staff/StaffService.java        | 430 +++++++++++++++------
 .../access/staff/StaffShiftRecord.java             |  15 +-
 .../dto/StaffEmploymentStatusRequest.java          |   4 +
 .../dto/StaffNumericSettingRequest.java            |   4 +
 .../dto/StaffPenaltyCapRequest.java                |   9 +
 .../dto/StaffRegistrationRequest.java              |  10 +-
 .../dto/StaffSelfRegistrationRequest.java          |   5 +
 .../dto/StaffShiftBatchResult.java                 |  18 +-
 .../dto/StaffShiftRequest.java                     |  11 +-
 9 files changed, 364 insertions(+), 142 deletions(-)
```

numstat：

| 路径 | + | − |
|---|---:|---:|
| StaffService.java | 306 | 124 |
| StaffShiftRecord.java | 14 | 1 |
| StaffEmploymentStatusRequest.java | 4 | 0 |
| StaffNumericSettingRequest.java | 4 | 0 |
| StaffPenaltyCapRequest.java | 9 | 0 |
| StaffRegistrationRequest.java | 6 | 4 |
| StaffSelfRegistrationRequest.java | 5 | 0 |
| StaffShiftBatchResult.java | 7 | 11 |
| StaffShiftRequest.java | 9 | 2 |
| **合计** | **364** | **142** |

基线 blob：五份已有文件与 main HEAD **逐字节相同**（见 §6）；四份新文件基线不存在。

### 2.2 StaffService 结构覆盖（1553 行，不能只看 +306）

| 行区间 | 块 | 覆盖 |
|---|---|---|
| L1–L119 | 包头 V0.17、字段注入（AccessService / TerminalDeviceRepository / StaffApprovalService / StaffRegistrationService）、三构造器、Clock | 已读 |
| L121–L273 | 商户代注册、公司审批、自审拦截、开通账号+role_staff_self+职级 | 已读 |
| L275–L427 | 列表隔离、存档、雇佣五态、销售岗、删除申请/最高权限批准 | 已读 |
| L429–L613 | 设备绑定/换机审核/完整性上报/吊销 | 已读 |
| L615–L816 | 排班草稿+上一级审批、批量、历史、可见天数、处罚上限三审 | 已读 |
| L821–L934 | 本人排班窗、打卡（工作手机+三方式）、考勤列表 | 已读 |
| L936–L1006 | 通知确认/创建 | 已读 |
| L1010–L1159 | 自助档案、scope 守卫、companyId 空值分支 | 已读 |
| L1161–L1553 | 底薪/奖惩/提成/月结/发放/税表/请假/月视图/工具 | 已读 |

## 3. 三线审计

### 3.1 第一性原理

| 文件 | 为什么必须存在 | 声称是否执行 | 未验证前提 |
|---|---|---|---|
| StaffService | 删除则 BSM-009/028/084–087、PGM-032/033/059 的注册审批、排班发布、打卡、薪资入口做不到 | 方法体把权限闸、幂等、审计、仓储调用串起来；新路径依赖 U05 类型/方法（单独盖 main 编译失败，§6） | 目标库是否已有 V400–V409；督导链是否已为发起人维护直属上级 |
| StaffShiftRecord | BSM-086 排班要带点位与发布状态 | 12 字段 + 兼容 10 参构造默认 `publicationStatus=published` + `awaitingReview` | 历史 JSON 无新字段时 Jackson 解码（U05 `StaffApprovalService.decode`） |
| StaffEmploymentStatusRequest | BEQR-157 五态写入面 | 服务白名单五码；DTO 本身不校验 | 无 |
| StaffNumericSettingRequest | BSM-001/086 可配阈值形状 | `requireNumericSetting` 强制 value∈[min,max] 且理由 | GET 缺行仍返回空上下限（GR-U06-006） |
| StaffPenaltyCapRequest | BEQR-186 20% 上限三审 | 平行线 List 原样交给 U09 `requireDefaultLines`（策略固定 hr/finance/legal） | 平行线种子是否在 U10 `approval_lines` 齐 |
| StaffRegistrationRequest | BSM-085 商户代申请 | phone/idDoc 原值；服务端加密在 U05 仓储 | 与 main OpenAPI `phone_hash`/`id_doc_hash` 分叉 |
| StaffSelfRegistrationRequest | BSM-085/PGM-033 公司自有半边契约 | 六字段无设备公钥/指纹（GR-U06-001） | U07 permitAll；U05 `selfRegister` |
| StaffShiftBatchResult | BSM-086 整批一张审批单 | 增加 reviewId/publicationStatus；兼容三参默认 published | 无 |
| StaffShiftRequest | 排班/改班申请 | 增加 shiftId；七参兼容构造 | 无 |

删除任一本段新 DTO：雇佣五态写入、可见天数/处罚上限配置、公开自助注册契约、排班审批回执会从本材料消失。五份旧文件若回退到 main/基线：PII 回到不可逆哈希字段名、排班立即落库无上一级审批、打卡仍接受 `manual`。

### 3.2 顺从式（假定意图正确）

意图：把 HTML 两条员工注册路径、排班上一级审批、可见天数/处罚上限可配、雇佣五态、工作手机三方式打卡、PII 原值加密接到现有员工三表服务。

与意图同向的证据：

- 商户代注册 `submitRegistration` 记发起人、禁止自审（最高权限除外）、须对应域公司审批；通过后建 `usr_` 账号、发 `role_staff_self`、按职级分配
- 排班不再 `repository.upsertShift` 直写，改为 `staffApprovals.submitShifts`；`StaffShiftRecord.awaitingReview`；班次已开始拒绝改写（BSM-086「一经开始即锁定」）
- 打卡白名单 `nfc/qr/charging_disconnect`，NFC/离充只记上班，须已验证 `staff_work_phone` 且 `device.staffId==profile.id`；首次上班卡去重
- 雇佣五态技术码与 BEQR-157 中文闭集对应（pending_entry/employed/trial/suspended/departed）
- 处罚上限出厂 2000 bps=20%，变更走 `requestPenaltyCap` → U09 策略 `top_authority` + 三条平行线；`requireNumericSetting` 写路径强制上下限
- 请假四类技术码 personal/sick/comp/annual；重叠提交 `LEAVE_OVERLAP`；月视图批准假覆盖为 leave
- PII：DTO 收原值；U05 仓储加密；U05 控制器 `maskedPersonalInfo`（本段服务返回值由控制器脱敏）
- 商户隔离：`requireUserInScope` / `staffMerchantScope` / `staffCompanyScope`；平台管理员放行
- 删除档案：先存档、占用计数、最高权限批准；`isMasterAuthority` 申请人自动批准与 BSM-004「持授权 U 盘密钥的最高权限本人发起不需审批」同向（该条不在本段映射闭集，不升 finding）

意图未闭环处升级为 §7 finding，不在顺从式里改口。

### 3.3 对抗式（grilling 追问树，事实自取）

Frontier 已结算、不向操作员发问：

1. **单独发布本 9 文件能否编译？** 否。`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 mvn -o -f /tmp/u06-b1-grok/backend-only9/pom.xml compile -DskipTests` → **MVN_EXIT=1**，`cannot find symbol` 84 处 + 签名不匹配 10 处。缺 `StaffApprovalService`/`StaffRegistrationService`/`StaffNumericSetting`，以及 main `StaffProfileRecord`/`JdbcStaffRepository` 无 `withCompanyRoute`/`companyId`/`numericSetting`/`findShiftByUserStart` 等。→ GR-U06-008
2. **会不会覆盖 M009–M012 已修字节？** 本段五份同名文件 `git rev-parse HEAD:<path>` = `242f4872:<path>`，M009–M012 **未改这些文件正文**。覆盖不会回退这五文件内的 M009–M012 补丁（因为没有）。会冲突的是 main 生态：OpenAPI `phone_hash`、V210 `chk_att_method IN ('nfc','qr','manual')`、main 无 V400–V409（main 已 V410）。
3. **自助注册 DTO 能否在安装时绑定设备标识？** 不能。record 仅 idempotencyKey/companyId/merchantId/name/phone/idDoc。→ GR-U06-001
4. **用户可见报错是否全中文？** 否。`id is required`；`approve 时必须设置…(initial_password)`；`通过（approved）或驳回（rejected）`。→ GR-U06-002
5. **班次开始后顶班有入口吗？** `SHIFT_STARTED` 文案叫人「记录顶班安排」，本类无顶班方法。→ GR-U06-003
6. **请假能撤销/销假吗？** 只有 pending/approved/rejected。→ GR-U06-004
7. **月视图是否发明第六态？** `missing_checkout` 不在 BEQR-182 闭集。→ GR-U06-005
8. **可见天数缺行是否带上下限？** `getShiftVisibility` → `numericSetting(..., 7)`，U05 仓储缺行 `orElse(new StaffNumericSetting(7,null,null))`；V402 只种子处罚上限。→ GR-U06-006
9. **companyId==null 是否放行？** 审批 L204 对非最高权限是拒绝；列表 L288 与 `requireProfileInScope` L1067、`listMyShifts` L828 把空值当项目/可见。→ GR-U06-007
10. **商户代注册是否仍读独立自营布尔？** `submitRegistration` L128 调 `registrations.merchantApprovingCompany`，U05 实现读 `merchant.selfOperated()`。→ GR-U06-009
11. **Map.of 空雇佣状态会不会 NPE？** 兼容构造默认 `employmentStatus="pending_entry"`。探针：`Map.of("before", null, …)` → `NPE_ON_NULL_VALUE`；非空 → `OK_NON_NULL`。首跑新档不撞。不升 finding。
12. **字段注入在单测构造器路径？** 副本 `StaffScopeTests` 已 `ReflectionTestUtils.setField(staffApprovals)`。生产 Spring 会注入。观察，不升。

理论极端（两件不巧才成立，按铁律 6 不修）：并发双写雇佣状态无幂等键；`listMyAttendance` 用 `Instant.now()` 而不用注入 Clock；Jackson 解码缺 `reviewId` 的旧快照。

## 4. 适用冻结判据逐项

每项：适用面 / 结论（本段范围内）。

| ID | 本段适用面 | 核对结论 |
|---|---|---|
| GEN-001 | 必要性 | 九文件均有 HTML 上位；`slug()` 无调用（观察）；兼容构造器为调用便利非业务发明 |
| GEN-002 | 可执行正确性 | **FAIL**：单独盖 main 编译失败（GR-U06-008）；自助注册契约不能完成「安装即绑定」（GR-U06-001） |
| GEN-003 | 测试 | 本段无测试文件。副本 `StaffScopeTests`/`StaffAttendanceLocalZoneTests` 为 mock（U11）。JDBC 测试会写库，**未跑、不写通过** |
| GEN-004 | 输入校验 | 注册/排班/打卡/数值设置有校验；自助 DTO 不收设备标识（并入 001） |
| GEN-005 | 授权 | `@GuardedAction` / `requirePermission` / `requireOwnProfile` / 工作手机归属；跨商户拒绝 |
| GEN-006 | 完整性 | 注册/排班/处罚变更有 `@Transactional`；`bindCredential` 无事务（观察） |
| GEN-007 | 幂等 | 注册/设备/排班/打卡/奖惩走 `IdempotencyService`；`setShiftVisibility`/`setEmploymentStatus` 无幂等键（观察，upsert 同值可重放） |
| GEN-008 | 接口一致 | **FAIL**：phone vs phone_hash；本段类型 main 无；打卡 method 与 V210 CHECK（GR-U06-008） |
| GEN-009 | 错误 | 校验失败抛 `ApiException`；英文报错见 GR-U06-002 |
| GEN-010 | 安全/PII | DTO 收原值、仓储加密（U05）、控制器脱敏；公开自助无设备绑定（001） |
| GEN-011 | 审计 | 写路径 `recordSuccess`/`recordFailure` |
| GEN-012 | 边界 | `MAX_BATCH_SHIFTS=500`；列表有的真分页、有的内存 `page()`（观察） |
| GEN-013 | 构建 | 9 文件 overlay `mvn -o compile` EXIT 1；全量 76 文件 overlay 属席甲先前日志，本席不采信为己证 |
| GEN-014 | 迁移 | 本段无 SQL；行为依赖 U10 V402/V405/V406/V407；main 最大 V410，编号须重排 |
| GEN-015 | 展示语言 | **FAIL** 英文报错（GR-U06-002） |
| GEN-016 | 文档可验证 | `SHIFT_STARTED` 声称顶班安排但无入口（GR-U06-003） |
| GEN-017 | 分类顺序 | 本席按冲突→细化→技术性→添加；添加仅 005 |
| GEN-018 | 证据 | 锁原文、HTML 现行行、命令输出、编译日志 |
| BEQR-157 | 雇佣五态 | 白名单五码与原文一致（细化技术码） |
| BEQR-182 | 考勤日结果 | **FAIL** 多 `missing_checkout`（GR-U06-005 添加） |
| BEQR-183 | 打卡方式 | 三方式 + 最早一次上班卡；拒 `manual`（相对 main 更贴原文） |
| BEQR-184 | 请假类型 | 四类技术码 + 主管审 |
| BEQR-185 | 请假单状态 | **FAIL** 无已撤销/已销假（GR-U06-004） |
| BEQR-186 | 薪资构成 | 上限 20% 与三审入口在本段；分母/事假公式在 U05 仓储（已定稿 U05-002/004，本段观察不重复升根因）；结构化补贴/绩效档无本段入口（观察） |
| BEQR-187 | 提成发放 | `addCommission`+`closePayroll`+`markPayrollPaid`；不在打卡路径发现金 |
| BSM-001 | 可配原则 | 数值设置须上下限（写路径） |
| BSM-003 | 可改可删 | 存档/删除申请/最高权限批准在本段 |
| BSM-009 | 基础管理 | 员工/设备/权限入口 |
| BSM-028 | 注册审批 | 商户代申请+公司审；自营分流读布尔（GR-U06-009） |
| BSM-029 | 设备绑定 | bind/rebind/revoke/integrity |
| BSM-073 | 商户范围 | merchant scope 过滤 |
| BSM-084 | 员工手机端 | 自助班次/考勤/请假/薪资方法 |
| BSM-085 | 两条路径 | 自助 DTO+商户 DTO；设备标识缺（001）；自营布尔（009） |
| BSM-086 | 排班考勤请假 | 上一级审批同向；顶班缺（003）；可见天数 GET 空上下限（006） |
| BSM-087 | 我的薪资 | 月结后 `listMyPayroll`；上限入口在 `requestPenaltyCap` |
| BSM-088 | 提成月结 | 提成入台账，不在当日现金抽 |
| BSM-114 | 入口隔离 | scope 守卫存在；companyId 空值列表放行（007） |
| BSM-115 | 连基础 | 项目/商户/公司路由 |
| BSM-120 | 设备标识 | 自助 DTO 不能完成安装绑定（001）；柜机不在本段 |
| PGM-001 | 治理底座 | 资格/责任链：注册审批、排班审批 |
| PGM-008 | 员工和手机 | 注册+私人手机绑定+工作手机打卡 |
| PGM-022 | 空间点位 | `pointSpaceIds` 随班次 |
| PGM-024 | 班次责任区域 | 同上 |
| PGM-026 | 六类合作 | 本段不存合作方式码；读路径在 U05（009） |
| PGM-032 | 人-手机对应 | 工作手机打卡对应；自助注册不绑私人手机（001） |
| PGM-033 | 主要功能 | 两条路径有；安装绑定无（001） |
| PGM-034 | 企微 | **无实现面**：本段无企微会话读写 |
| PGM-035 | 提成治理 | 月结银行发放入口存在 |
| PGM-036 | 设备管理 | 凭证绑定员工；班次带点位 |
| PGM-048 | 商户侧隔离 | `CROSS_MERCHANT_FORBIDDEN` |
| PGM-059 | PII | 原值传输+加密存放（U05）+界面脱敏（U05 控制器）；与「不可逆哈希日常无法脱敏」的 main 口径相比本材料更贴原文 |
| PGM-066 | 全中文/留痕 | **FAIL** 英文报错（002）；写路径有审计 |

## 5. 调用链（只读）

```
本段 DTO/StaffService
  → 副本 U05 StaffController（转发 employment-status / self-registrations / shifts / penalty-cap / punch）
  → 副本 U05 StaffRegistrationService.selfRegister(StaffSelfRegistrationRequest) / merchantApprovingCompany（selfOperated）
  → 副本 U05 StaffApprovalService.submitShifts / submitPenaltyCap
  → 副本 U05 JdbcStaffRepository（加密、numericSetting 缺行、修订表）
  → 副本 U05 StaffProfileRecord / StaffNumericSetting
  → 副本 U07 SecurityConfig publicApiPaths 含 /self-registrations
  → 副本 U08 MerchantRecord.selfOperated（U04-001 已答删除）
  → 副本 U09 SupervisorReviewService 策略 staff.shift.publish / staff.payroll.penalty_cap.adjust（hr_review,finance_review,legal_review）
  → 副本 U10 V400–V409（V402 只种子处罚上限；V405 CHECK 含 charging_disconnect+manual）
  → main 445a49f4 同名五文件＝基线；OpenAPI StaffRegistrationRequest.properties=phone_hash/id_doc_hash；V210 CHECK nfc/qr/manual；已有 V410
```

未发布 OpenAPI 已有 `/api/v1/staff/self-registrations` 等路径（本段文件之外，观察）。

## 6. 与 main 分叉对照（对抗式固定问题）

五份同名文件：`git log 242f4872..HEAD -- <path>` 空；`git rev-parse HEAD:<path>` = `242f4872:<path>`。

| 路径 | main 行/SHA256 | 与基线 | 发布覆盖含义 |
|---|---|---|---|
| StaffService.java | 1371 / `90c0702a…dd81` | SAME blob `4b601fb3` | 快进到 1553 行 V0.17；**不回退 M009–M012 字节**（那些修复不在此文件） |
| StaffShiftRecord.java | 20 / `19dda39f…2f62` | SAME | 加 reviewId/publicationStatus |
| StaffRegistrationRequest.java | 17 / `ca988b3a…f3bbd` | SAME | `phoneHash/idDocHash` → `phone/idDoc` |
| StaffShiftBatchResult.java | 17 / `98aa6235…2bb2` | SAME | 加审批回执字段 |
| StaffShiftRequest.java | 17 / `2ad0cf9f…8c9b2` | SAME | 加 shiftId |
| 四个新 DTO | main 无 | 新增 | 必须同 PR 带 U05 类型 |

main 仍为英文 `project_id is required` / `name is required` / `id is required`；未发布修了前两处、留下 `id is required` 与 `approve 时…initial_password`。

main 打卡：`method` 默认 `manual`，白名单 nfc/qr/manual。未发布改为强制工作手机事件、白名单含 `charging_disconnect`、拒 manual。

独立 9 文件 overlay 编译（本席，不改仓库）：

```
JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
mvn -o -f /tmp/u06-b1-grok/backend-only9/pom.xml compile -DskipTests
START=2026-09-09T21:30:58Z  MVN_EXIT=1  END=2026-09-09T21:31:01Z
cannot find symbol ×84；cannot be applied ×10
首条：StaffService.java:74 cannot find symbol class StaffApprovalService
```

因此：**禁止整文件覆盖 main 当作快进**；须以 445a49f4 为底重放 diff，与 U05+U07 同 PR，迁移重编号 ≥V411（U10），打卡 CHECK 与 OpenAPI 字段同批。

## 7. Findings（临时 ID；正式 ID 仅 B2）

### GR-U06-001 — 自助注册 DTO 无设备公钥/指纹

- 文件:行号：`StaffSelfRegistrationRequest.java:4-5`（调用：U05 `StaffRegistrationService.selfRegister`、U05 `StaffController:51-56`、U07 permitAll）
- 完整原文：`public record StaffSelfRegistrationRequest(String idempotencyKey, String companyId, String merchantId, String name, String phone, String idDoc) {}`
- 业务影响：公司自有员工按 HTML 应在私人手机 App 安装时绑定该手机专属设备标识；契约层无法提交标识，审批落档后私人手机与员工仍脱节
- 可复现：`python3 -c "print(open('…/StaffSelfRegistrationRequest.java').read())"` — 字段闭集无 publicKey/fingerprint；对照 HTML PGM L576/BSM L1957
- 冻结判据：PGM-032、PGM-033、BSM-120、BSM-085、GEN-002、GEN-004、GEN-010
- 2.3 依据类型：1（HTML 原文）
- 四分类：**冲突**（明文「安装时生成并绑定」vs 请求无该字段）
- 工程缺陷：是。建议登记：是。发布前须修：是
- 同根：U05-003 / U01-007 / U02-002。不新开 2.2
- 最小处置（给发布者）：DTO 增加私人手机设备公钥与指纹（或等价专属标识）；U05 落档或批准时绑定；不要求先有 staffProfileId；与 U01/U02/U05/U07 同一 PR

### GR-U06-002 — 用户可见报错含英文

- 文件:行号：`StaffService.java:215-216`、`:410`、`:1330`；同类 `:843`（check_in/check_out 夹注）
- 完整原文：
  - `"approve 时必须设置员工初始登录密码 (initial_password)"`
  - `"审批结论必须为通过（approved）或驳回（rejected）"`
  - `"id is required"`
- 业务影响：后台/人事在缺字段或删档审批时看到英文，违反「所有终端零英文」
- 可复现：`rg -n 'id is required|initial_password|（approved）' <副本 StaffService.java>`
- 冻结判据：PGM-066、GEN-015、GEN-009
- 2.3 依据类型：1（HTML PGM L951「界面全中文…报错必须全中文」）
- 四分类：**冲突**
- 工程缺陷：是。建议登记：是
- 无业务发明：否（本项不是技术性）
- 最小处置：改为全中文，例如「必须填写编号」「通过审批时必须设置员工初始登录密码」「审批结论必须为通过或驳回」。技术码可留在 `ApiException.code`，不要进 message

### GR-U06-003 — 班次开始后仅拒绝改写、无顶班入口

- 文件:行号：`StaffService.java:636-637`（`draftShift`）；全文件无 cover/顶班方法
- 完整原文：`throw ApiException.conflict("SHIFT_STARTED", "班次已经开始，不能改写排班；临时顶班须记录顶班安排");`
- 业务影响：当班未到需要当场顶班时，系统拦住改班却不提供 HTML 要求的领班顶班记录，小费/服务费份额无法按顶班归属
- 可复现：构造 `startAt <= now` 的 `StaffShiftRequest` 调 `upsertShift`/`batchUpsertShifts`；`rg -n '顶班|coverShift' StaffService.java` 仅命中该 throw
- 冻结判据：BSM-086、GEN-002、GEN-016
- 2.3 依据类型：1（HTML BSM L1441「由领班直接安排其他工作人员顶班并在系统中记录…不受本条审批流程限制」）
- 四分类：**冲突**（遗漏：实现声称顶班却无入口；不是 HTML 沉默）
- 工程缺陷：是。建议登记：是
- 最小处置：增加顶班记录入口（不走上一级排班审批），写入顶班人员与原班次；或先删除误导文案并在发布说明标明本迭代不做顶班——后者须走添加/删范围的 2.2，本席按冲突建议补入口

### GR-U06-004 — 请假状态机缺少已撤销、已销假

- 文件:行号：`StaffService.java:1396-1418` `doSubmitLeave`；`:1429-1457` `reviewLeave`；无 withdraw/complete 方法
- 完整原文：`status` 只写 `"pending"`，审批只切 `"approved"`/`"rejected"`；`listLeaves` 透传该三态
- 业务影响：假期开始前不能自行撤回、开始后不能销假；已扣薪无法按 HTML 在当月冲回
- 可复现：`rg -n 'withdraw|销假|cancelled|completed' <副本 StaffService.java>` 无业务方法
- 冻结判据：BEQR-185、BSM-086、GEN-002
- 2.3 依据类型：1（HTML beqr:282 闭集含已撤销、已销假及考勤恢复/冲回）
- 四分类：**冲突**（遗漏）
- 工程缺陷：是。建议登记：是
- 最小处置：提交人在 `startDate` 前撤回→已撤销；已批准且已开始由主管确认销假并填实际结束日→已销假；两态后考勤按打卡重算、已月结则当期冲回。main 基线同样缺，本材料未补，发布前仍须补或明确砍范围（砍范围=添加/删需求，须 2.2）

### GR-U06-005 — 考勤日结果发明「未打下班卡」

- 文件:行号：`StaffService.java:1514-1516` `myAttendanceMonthView`
- 完整原文：`if (checkOut == null) statuses.add("missing_checkout");`
- 业务影响：月历出现 HTML 枚举之外的第六态，前端/对账若按五态+休息渲染会无法映射或另造文案
- 可复现：有排班、有上班卡、无下班卡的月份调 `myAttendanceMonthView`，日状态含 `missing_checkout`
- 冻结判据：BEQR-182、GEN-017、GEN-008
- 2.3 依据类型：1（HTML 闭集：出勤、缺勤、迟到、早退、请假；无排班=休息）
- 四分类：**添加**（HTML 无此日结果；不能伪装成早退——早退定义为早于结束打卡）
- 工程缺陷：是否修复待 2.2。建议登记：是。B1 不代用户回答
- 添加项全文（供主调度按 2.2）：是否允许考勤日结果在 HTML 五态+休息之外增加「未打下班卡」？允许 / 删除该态（无下班卡时改按缺勤、早退或出勤之一计）。本席不代答
- 最小处置：等 2.2；若删，则无 checkout 不得再写 `missing_checkout`

### GR-U06-006 — 排班可见天数缺行返回空上下限

- 文件:行号：`StaffService.java:772-775` `getShiftVisibility`（实现：U05 `JdbcStaffRepository.numericSetting:61-67`；种子：U10 V402 只插入处罚上限）
- 完整原文：`return repository.numericSetting("shift_visible_days", staffSettingScope(), 7);` — 缺行时 value=7、min=max=null
- 业务影响：出厂「7 天且须同时配置上下限」在首次 GET 不成立；写路径 `setShiftVisibility` 才强制上下限
- 可复现：对无 `shift_visible_days` 行的 scope 调 GET；对照 V402 INSERT 仅 `payroll_penalty_cap_bps`
- 冻结判据：BSM-086、BSM-001、GEN-002、GEN-009
- 2.3 依据类型：4（读仓储实现+迁移种子）兼 1（HTML「出厂默认 7 天，须同时配置上下限」）
- 四分类：**冲突**（同根 U05-005）
- 工程缺陷：是。建议登记：是。不新开 2.2（U05 已定：种子由发布者批准或 fail-closed）
- 最小处置：GET 缺行 fail-closed 或发布时按批准值写入 min/max；本段 `getShiftVisibility` 不得把空上下限当作出厂完成

### GR-U06-007 — `companyId==null` 列表/守卫兼容放行

- 文件:行号：`StaffService.java:288-289` `listProfiles`；`:1067-1072` `requireProfileInScope`；`:828` `listMyShifts` 用 `projectId` 冒充公司 scope
- 完整原文：`.filter(p -> p.companyId() == null || p.companyId().equals(registrations.actorCompany(actor)))`；`listMyShifts`：`profile.companyId() == null ? profile.projectId() : profile.companyId()`
- 业务影响：无公司路由的档案对公司角色可见；可见天数 scope 落到项目 id，跨经营主体串配置。L204 审批对 null companyId 是拒绝（非放行），不并入本条
- 可复现：构造 `companyId=null` 档案，公司角色调 `listProfiles`；读 L828
- 冻结判据：GEN-001、GEN-005、BSM-114（仓库铁律 8：不为存量做兼容）
- 2.3 依据类型：4（源码分支）
- 四分类：**技术性**（无业务发明：不新增「无公司员工」业务规则，只是空值退化；理由：HTML 两条路径都有所属公司，空 companyId 不是新业态）
- 工程缺陷：是。建议登记：是。同根 U05-010
- 最小处置：去掉 null 放行；写入强制 `company_id`；可见天数 scope 不得回落到 projectId

### GR-U06-008 — 相对 main 不可单独合入（编译/契约/CHECK）

- 文件:行号：本段 9 文件整体；证据 `/tmp/u06-b1-grok/only9-compile.log` MVN_EXIT=1
- 完整原文（编译首条）：`StaffService.java:[74,13] cannot find symbol class StaffApprovalService`
- 业务影响：若只合本段，构建失败；若强行改 CHECK/OpenAPI 不同批，打卡 `charging_disconnect` 会被 V210 拒绝，注册字段与安卓/OpenAPI `phone_hash` 对不上
- 可复现：见 §6 命令；`git rev-parse HEAD:…StaffService.java` = 基线；main OpenAPI `StaffRegistrationRequest.properties=['…','phone_hash','id_doc_hash',…]`；main V210 `CHECK (method IN ('nfc', 'qr', 'manual'))`；副本 V405 才加入 `charging_disconnect`
- 冻结判据：GEN-002、GEN-008、GEN-013、GEN-014
- 2.3 依据类型：4（编译日志+blob+OpenAPI+迁移）
- 四分类：**技术性**（无业务发明：是发布切片与依赖切分问题，不新增经营规则）
- 工程缺陷：是。建议登记：是。同根 U05-008
- 最小处置：以 main 445a49f4 为底重放；U05+U06+U07 同一 PR；自营读路径按 U04-001 改六类码、不带 U08 插拔；迁移 ≥V411；禁止整树覆盖。M009–M012 未改本段五文件，无「回退这五文件内已修项」问题，但有「打卡 CHECK / 注册字段 / 缺 U05 类型」生态冲突

### GR-U06-009 — 商户代注册仍经独立自营布尔分流

- 文件:行号：`StaffService.java:128` `registrations.merchantApprovingCompany(request.merchantId())`；DTO 注释 `StaffRegistrationRequest.java:7`「merchant_id 为空表示公司自有员工」
- 完整原文：`String approvingCompany = registrations.merchantApprovingCompany(request.merchantId());` → U05 `:89-91` `if (merchant.selfOperated()) throw … "自营商户员工须在私人手机自助注册"`
- 业务影响：发布后若仍带 U08 布尔，自营与合作方式可分叉；操作员已答删除该布尔
- 可复现：读 L128 + U05 `StaffRegistrationService.java:89-91`
- 冻结判据：BSM-085、BSM-028、PGM-033、PGM-026、GEN-008、GEN-017
- 2.3 依据类型：1 + 已答 2.2（questions/U04-001.md「删除」）
- 四分类：**冲突**（同根 U05-001 / U04-001：六类已含「自营」，独立布尔与「标注为自营视同公司自有」不是第二轴）
- 工程缺陷：是。建议登记：是。不新开 2.2
- 最小处置：本段不必改 DTO 字段；`merchantApprovingCompany` 改读 `cooperationForm=="self_operated"`。注释「空 merchant=公司自有」可保留（HTML 公司自有路径）

## 8. 观察（无新增根因，完整登记不漏并）

1. `slug()` L1540–1545 无调用（GEN-001 死代码）。
2. `@Autowired` 字段注入 `accessService`/`terminalRepository`/`staffApprovals`/`registrations`；单测靠 `ReflectionTestUtils`。生产 Spring 首跑会注入。不升。
3. `bindCredential`/`reportIntegrity`/`revokeCredential` 无 `@Transactional`（基线既有模式）。
4. `setShiftVisibility`/`setEmploymentStatus`/`setSalesPosition` 无幂等键；同值重放可接受。
5. `listMyAttendance` 用 `Instant.now()` 而非 `clock`；`setSalesPosition` 用 `Instant.now()` 而雇佣状态用 `clock`。
6. `listProfiles`/`listMyShifts`/`listMyPayroll` 走内存 `page()`，与排班真分页不一致。
7. `StaffShiftRecord`/`StaffShiftBatchResult` 兼容构造默认 `publicationStatus="published"`：草稿先走 10 参再 `awaitingReview`，内部自洽。
8. 删除申请遇 `isMasterAuthority` 自动批准：与 BSM-004「最高权限本人发起不需审批」同向；BSM-004 不在本段映射，不升。
9. 处罚上限平行线校验在 U09 `requireDefaultLines`，本段 DTO 注释与策略码 `hr_review/finance_review/legal_review` 同向；V407 只 INSERT `hr_review` 线（U10 观察）。
10. 结构化薪资项目（全勤奖/补贴/绩效 ABC）无本段入口；月结公式与封顶分母在 U05，已定稿 U05-002/004。本段 `closePayroll` 只做权限与调用。不重复升根因。
11. PGM-034 企微：本段无实现面。
12. 旷工辞退规则（BSM-086）无实现面：月视图无「旷工」态，一律 absent。不把 HTML 有而代码未声称的辞退自动化升冲突。
13. 未发布 OpenAPI 已有 self-registrations 等 path，本段新 DTO 的 schema 名称未全部出现在 `components.schemas`（契约段另审）。
14. Map.of 对 null 值 NPE（探针 EXIT 0：`NPE_ON_NULL_VALUE` / `OK_NON_NULL`）；新档雇佣状态默认 pending_entry，首跑不撞。
15. 全 76 文件 overlay 编译日志 `/tmp/u_backend_compile.log` 写 MVN_EXIT=0，是席甲先前产物；本席不以它为通过证据。
16. `reportIntegrity` 无 `@GuardedAction`，靠 `requireOwnProfile`；无认证时 `currentActor` 失败。
17. main 打卡仍允许 `manual`，与 BEQR-183 三方式冲突在 **main** 侧；本材料改为拒 manual，方向与 HTML 同向，不把「删 manual」判添加。

## 9. 命令证据（只读）

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

git rev-parse HEAD origin/main
# 445a49f409f0cfbe6dd616ae079be1299824c48f ×2

git cat-file -t e28dad059b2c7e08bcc95a8db18552d3f76edf4b   # tree
git cat-file -t 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb     # fatal
git cat-file -t 242f487233d028173cb9198eecd079f5d877405c     # commit

git diff --shortstat 242f4872 e28dad05
# 76 files changed, 4745 insertions(+), 1084 deletions(-)

git rev-parse HEAD:backend/src/main/java/com/complex/platform/access/staff/StaffService.java
git rev-parse 242f4872:backend/src/main/java/com/complex/platform/access/staff/StaffService.java
# 4b601fb3aa64a6db7dca091f9749a679131f2144 = SAME

# 9 文件副本 sha256 与开工提示 9/9 MATCH（见 §2 表）

JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 mvn -o -f /tmp/u06-b1-grok/backend-only9/pom.xml compile -DskipTests
# MVN_EXIT=1；cannot find symbol ×84

# /tmp/u06-b1-grok/mapof MapOfNull.java
# NPE_ON_NULL_VALUE
# OK_NON_NULL
# EXIT_MAPOF=0
```

未跑：任何 `complex_test*` JDBC、Flyway、会清库的脚本、完整 `mvn test`。未跑的不写通过。

## 10. 结论

**FAIL**

理由（任一即不得 PASS）：存在未定稿工程缺陷 GR-U06-001…009；添加项 GR-U06-005 未答 2.2；单独合入本段不可构建。无有条件通过。

发布者最小集：以 main 445a49f4 为底重放本段 diff + U05/U07；自助注册契约补设备标识；中文报错；顶班或删误导文案（按 003）；请假撤销/销假（按 004）；考勤第六态等 2.2；可见天数上下限；去掉 companyId 空值兼容；注册分流改读六类码。不在本轮修复。

## U06 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 46165 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，独立）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02 §7/§11；criteria-lock V0.11；modules.md V0.04 附录 U06（未发布材料第 6 段，9 文件 1664 行，main 归属 M054/M288）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`。本段五份既有 Java `HEAD:<path>` = `242f4872:<path>`（`StaffService` blob `4b601fb3aa64a6db7dca091f9749a679131f2144`）；main 自基线未改这些文件。树 `e28dad05:StaffService.java` blob `440ab03ee9d7bd0199e0b3fbeca5d8ea7180e1e1`。
- 审计对象仍是只读输入副本 `.scratch/module-recheck-20260908/unpublished-03acdb98`（树 `e28dad059b2c7e08bcc95a8db18552d3f76edf4b`，基线 `242f487233d028173cb9198eecd079f5d877405c`），不是 main。三份 HTML 取 main 正本。未发布提交对象 `03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb` 本仓 `git cat-file -t` 仍不可解析。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B2 核实结论。
- 本席 B2-r1：**PASS**（001–011 逐 ID 全 PASS；添加 1 为 main 既有不弹窗；发布前须修「是」=001–007，「否」=008–011）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。映射行 `M054 →` L6433 与 `M288 →` L6667 两行闭集逐字相同 COUNT=52 unique=52 MISSING=[] EXTRA=[]。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要核对只读（输入哈希、blob、`git show 242f4872:…StaffService.java` 落到 `/tmp`、HTML 锚点、`questions/U04-001.md` 答复首行「删除」）；未重跑 mvn/测试、未装依赖、未碰 `complex_test*`。

此票只判断 findings.md V1.54 顶部「当前 U06 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、「发布前须修」是/否与最小处置。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。材料未发布，定稿后状态应记 REVIEWED-FAIL（有已定稿缺陷，处置写给发布者），不做 B3。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/U06-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 46165 | `c6733dbfb2e1e09c5d46afd10ba51cf2de29468e5cfc82950aad312e089e0ec0` |
| `docs/audit-2026-09/modules/U06-b1-self-raw.md` | V0.01 | 32823 | `2de4492e61dfc2b378f7d6b39a74fc655295e92a085c83c62c224fe407c3efed` |
| `docs/audit-2026-09/findings.md` | V1.54 | 594332 | `0a3ea5211051847fc22c6bb565afee1fa8aaa4e16679f9609bc3e6ae38b13570` |
| `docs/audit-2026-09/ledger.md`（只读） | V2.59 | 479695 | `2626c5579d194e59fc1302393aebf0a885a71295bac37c3211054f92202e5bef` |
| `docs/audit-2026-09/questions/U04-001.md`（只读，沿用） | V0.01 | — | 答复首行精确「删除」 |

候选形态：V1.54 顶部 U06-001…011；questions 0（003 沿用 U04-001 已答「删除」；010 为 main 既有添加不弹窗）；无 HTML 修改申请。U06 ledger 行 IN_PROGRESS。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

HTML 正本哈希与 B1 一致：BEQR `4396bb71…1d66`、BSM `a7f7b7a3…66fa`、PGM `c4238afd…e71c`。副本本段 9/9 sha256/行/字节与树 `e28dad05` blob 全等（本轮复算）。`git diff --stat 242f4872 e28dad05 -- <9 路径>` = `9 files changed, 364 insertions(+), 142 deletions(-)`。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers | 先核技能；用户/提示词覆盖技能本体 |
| receiving-code-review | 先核 HTML/diff/blob/grep 再接受或反驳席甲改判；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：三项 main 既有是否被本材料加重、001 是否改写 GR-007、004 补入口是否越界、011 是否吞掉 §8.15/§8.17 |
| verification-before-completion | 声称前先跑 sha256/git rev-parse/git show/diff/sed/rg；本票不声称本轮新编译绿 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流（本段 9 文件 → U05 仓储/注册/审批 → HTML → 基线 blob） |
| ponytail-review | 只评最小删除/收窄，不落地。001 不造新经营主体；004 只记顶班人员与原班次；010 不为本段弹窗 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到**；未冒充调用 |
| Arc audit | 会派子代理；与双席冲突；**未用** |

### 完整差集（GR-001…009、§8 观察 1–17、席甲 CX-001、OBS-1…9）

漏并：无。两席 B1 全部临时 ID 仍落在 001–011。本席 GR-005 的添加分类保留，按 U01 定稿「main 既有」口径改登记为「添加（main 既有）」不弹窗，不另留 2.2。席甲独有 CX-001 与本席 GR-007 的 `listMyShifts` 回落 `projectId` 部分并列进 001；GR-007 其余空值放行进 006。席甲 OBS-2/8/9 引用 U05 已定稿，不重复入队。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/须修 | 本席票 |
|---|---|---|---|
| U06-001 | CX-U06-001 + GR-U06-007（`listMyShifts` 回落 `projectId` 部分） | 技术性/是/是 | **PASS（技术性/是/是）** |
| U06-002 | GR-U06-001 | 冲突（同根 U05-003/U01-007/U02-002）/是/是 | **PASS（冲突同根/是/是）** |
| U06-003 | GR-U06-009 | 冲突（同根 U05-001/U04-001 已答删除）/是/是 | **PASS（冲突同根/是/是）** |
| U06-004 | GR-U06-003 | 冲突（本材料新引入）/是/是 | **PASS（冲突/是/是）** |
| U06-005 | GR-U06-006 | 冲突（同根 U05-005）/是/是 | **PASS（冲突同根/是/是）** |
| U06-006 | GR-U06-007（列表/守卫空值放行） | 技术性（同根 U05-010）/是/是 | **PASS（技术性同根/是/是）** |
| U06-007 | GR-U06-008 + 席甲 §4 分叉对照 | 技术性（发布分叉，同根 U05-008）/是/是 | **PASS（技术性同根/是/是）** |
| U06-008 | GR-U06-002 | 冲突（main 既有）/是/否（归 M054） | **PASS（冲突 main 既有/是/否）** |
| U06-009 | GR-U06-004 | 冲突（main 既有）/是/否（归 M054） | **PASS（冲突 main 既有/是/否）** |
| U06-010 | GR-U06-005 | 添加（main 既有）/是/否（归 M054） | **PASS（添加 main 既有/是/否）** |
| U06-011 | 双方观察 | 登记/否/否 | **PASS（登记/否/否）** |

冲突入队 4（002、003、004、005）。技术性入队 3（001、006、007）。冲突（main 既有）2（008、009）。添加（main 既有）1（010）。登记观察 1（011）。

发布前须修「是」=001、002、003、004、005、006、007（7）；「否」=008、009、010、011（4）。与候选汇总一致。

### 重点 (a) main 既有裁定 — **PASS（口径合规；008/009/010 确为 main 既有，本材料未加重）**

口径沿用 U01 定稿：main HEAD 同位置已有且本材料未加重该缺陷本体 → 冲突/添加/技术性（main 既有）、发布前须修否、归主归属模块（本段 M054）。材料不得加重。本席按「缺陷本体是否出现在 `git diff 242f4872 e28dad05 -- StaffService.java` 的 +/- 中、出现后是收窄还是加码」独立核三项。对照物：`git show 242f4872:backend/src/main/java/com/complex/platform/access/staff/StaffService.java` 落到 `/tmp/u06-b2-grok-baseline-StaffService.java`（1371 行），副本 1553 行；HEAD blob = 基线 blob。

| 项 | 本席命令 | 基线/main | 副本 | diff 是否触及缺陷本体 | 加重？ |
|---|---|---|---|---|---|
| 008 非全中文报错 | `rg -n 'is required\|initial_password\|（approved）'` | `is required` **4** 处（`:127` `project_id` / `:131` `name` / `:322` `sales_position` / `:1139` `id`）；`(initial_password)` `:198`；`（approved）或驳回（rejected）` `:363` | `is required` **1** 处（`:1330` `id`）；另两句行号平移仍在（`:216` / `:410`） | `-` 删掉 3 处 `is required`；保留的三句不在新增侧 | **否（收窄）** |
| 009 请假无撤销/销假 | `rg -n 'withdraw\|cancelled\|销假'`；读 `doSubmitLeave`/`reviewLeave` | 0 业务方法；只写 `pending`→`approved`/`rejected`（基线 `:1222/:1255`） | 同构（副本 `:1413/:1446`）；0 命中 | 状态机三态不在 +/- 当新增实现 | **否（未补、也未改坏）** |
| 010 第六态 `missing_checkout` | `rg -n missing_checkout`；读 `myAttendanceMonthView` | 1 处基线 `:1330` `if (checkOut == null) { statuses.add("missing_checkout"); }` | 1 处副本 `:1515` 同条件 | 函数改归期（`workDate`→`endAt`）且 `-`/`+` 各 1 行该枚举 | **否（见下）** |

010 未加重的独立理由，不是照抄席甲「只改归期」四个字：

1. 第六态分支条件未变：有上班卡且无下班卡才写入 `missing_checkout`；没有新开第七态、没有新 API 输出该码。
2. 归期从 `workDate` 改到 `LocalDate.ofInstant(endAt)`，并改走 `listShiftsEndingBetween`：这是 BSM L1449 原句「该班次的工时、考勤……一律归属班次结束时点所在的月份与年度」的实现，不是把第六态发明得更勤。跨月夜班只是把**同一枚举**从起始月挪到结束月，全年出现次数不因归期本身增加。
3. 同日多班次由 `putIfAbsent` 改成按日聚合：BEQR L279 已写「同日多个班次并列显示」。多班次日子上第六态与其它态并列，是闭集外那一枚举被更多班次看见，不是新规则。
4. NFC/离充只记上班（副本 `:852-854`）贴 BSM L1445「三种方式记的是同一个上班时刻」；下班仍可扫码。不把「领取≠下班」算作本段把第六态加码。

因此：008/009/010 发布前须修「否」、归 M054 成立。010 作为「添加（main 既有）」不为本段弹窗 2.2 成立——问的是 main 早已存在的第六态，不是本材料发明。本席 B1 把 GR-005 标添加、走 2.2，是当时未做基线 blob 对照；本票按 U01 口径改登记，**不改添加分类本身**（HTML 闭集仍无「未打下班卡」，不能伪装成早退）。

不把 008 的 `:843`「上班（check_in）或下班（check_out）」夹注单列须修：基线 `:721` 同句，属 008 同类，仍归 M054。副本 `reviewRegistration` 结论句已收成「必须为通过或驳回」（`:263-264`），只留删档审批 `:410` 夹注，亦属收窄。

### 重点 (b) U06-004（GR-003）顶班 — **PASS（冲突成立；补入口不越界）**

基线 `SHIFT_STARTED|顶班|coverShift` **0 命中**；`upsertShift` 基线 `:577-598` 直接 `repository.upsertShift`，无开始锁定。副本 `:636-637` 新增：

> `throw ApiException.conflict("SHIFT_STARTED", "班次已经开始，不能改写排班；临时顶班须记录顶班安排");`

全类仍无顶班方法。HTML 现行 BSM **L1443**（本席 B1 写成 L1441，那是 `<h3>`；句子在该 `<li>`）：「当班有人未到、需要当场顶班的，由领班直接安排其他工作人员顶班并在系统中记录顶班人员，不受本条审批流程限制」。实现声称顶班却无入口，是遗漏，不是 HTML 沉默。分类冲突、须修是。

处置「补顶班记录入口（不走上一级审批，写入顶班人员与原班次）或去文案并走 2.2 砍需求」与本席 B1 原文同向。席甲按冲突建议补入口**不越界**：补的是 HTML 已写的「记录顶班人员」，不是发明小费公式、加班费或岗位例外（那些句子同条已有，本段最小集只要求有记录入口且不走排班上一级审批）。去文案=砍 HTML 已要求的能力，必须另走 2.2，候选已写明，不是本席代答。

禁止：把顶班做成上一级审批；在本段规定小费/服务费份额怎么算。

### 重点 (c) U06-001 并列 CX-001 与 GR-007 的 listMyShifts — **PASS（未改写原意；最懒成立）**

两件不是同一句话，但读侧是同一行：

- CX-001：写键 `staffSettingScope`（`:766-770`，无商户 → `"company:"+registrations.actorCompany(actor)`，`actorCompany` = 档案公司 → 岗位组织 → 令牌组织）vs 读键 `listMyShifts` `:828`（`"company:"+(companyId ?? projectId)`）。同一参数两套推导，配置可永不生效。HTML BSM L1444。
- GR-007 的 listMyShifts 部分：`:828` 用 `projectId` 冒充公司 scope。铁律 8，技术性。L204 审批对 null 是拒绝，本席 B1 已排除，候选未并入 001，未改写。

001 只取 GR-007 的读侧回落，把列表/守卫空值放行留给 006：原意未丢、未把审批拒绝说成放行。

处置「抽一个经营主体键函数供写读共用、不得回落 projectId」相对席甲 B1「缺省按 projectOf 推导」是收窄，对齐本席 GR-007，不是改写 CX-001 的写读不一致。最懒边界：

- 函数入参是 `(merchantId, companyId)`：写侧填**配置者**的商户/公司，读侧填**该员工档案**的商户/公司。禁止员工读侧无参调用 `staffSettingScope()`（那是当前 actor，身份会串）。
- `company:` 只跟档案 `companyId`；缺则 fail-closed 或出厂 7，**不得** `projectId`。
- 不发明第三种经营主体、不把项目当公司。

补 `company:` 写读回归属 GEN-003，与抽函数同一处置，不另开 finding。

### 重点 (d) 002/003/005/006/007 同根 — **PASS（同根成立；只引用、不重复发明）**

| 本段 | 本席 B1 | 已定稿同根 | 候选处置 | 裁定 |
|---|---|---|---|---|
| 002 | GR-001 DTO 六字段无设备标识 | U05-003 / U01-007 / U02-002 | 随 U05-003 加公钥/指纹，同 PR | 契约在本段 record；根因与处置已在 U05-003。本段不另发明频控 |
| 003 | GR-009 `merchantApprovingCompany` 读 `selfOperated()` | U05-001 / U04-001 已答删除 | 本段不改；U05-001 改读六类码后收敛 | `StaffService.java:128` 只是调用面；布尔在 U05 `:89-91`。不再问 |
| 005 | GR-006 GET `(7,null,null)` | U05-005 | 随 U05-005 种子或 fail-closed | 缺行在 U05 `numericSetting:61-67`；本段 GET 不得把空上下限当出厂完成 |
| 006 | GR-007 列表/守卫 null 放行 | U05-010（已含本段 `:204/:288`） | 随 U05-010 去 null、强制 `company_id`；可见天数不得回落 projectId（与 001 同修） | 服务侧与仓储 COALESCE 同一兼容；不另造「无公司员工」业态 |
| 007 | GR-008 only-9 `MVN_EXIT=1` | U05-008 | 以 main 为底重放，U05+U06+U07 同 PR，CHECK/OpenAPI 同批，迁移 ≥V411 | 席甲 76 文件 overlay 绿不能消解 only-9 失败（本席 §8.15） |

U05-010 定稿句已点名本段 `:204/:288`。006 把 `:1067-1072` `requireProfileInScope`（`companyId!=null` 才走公司校验，null 掉进项目/商户）和 `:828` 读侧一并归入，是同一铁律 8，不是新根。

### 重点 (e) U06-011 观察 — **PASS（未写错本席 §8；第 15/17 条在）**

011 列席甲 OBS-1…9，并以「Grok §8 1–17（含 3 `bindCredential` 无事务、5 `Instant.now()` 与 clock 混用、6 内存分页、12 旷工辞退无实现面、13 OpenAPI schema 名未全出现（第 14 段）、**15 不采信席甲编译日志为己证**、**17 拒 manual 与 HTML 同向**）」收本席观察。「含」是抽样不是闭集，覆盖 slug 死代码、字段注入、无幂等键、兼容构造默认 published、Map.of NPE、`reportIntegrity` 无 `@GuardedAction`、企微无实现面。未把本席任何 finding 降成观察，也未把 §8 升格入队。

第 15 条：席甲 B1 §2 记 `/tmp/u-overlay` `MVN_EXIT=0` 与一次性空库 92/92。本席 B1 明确不以 76 文件 overlay 为己证；007 用的是本席 only-9 `MVN_EXIT=1`。011 写明「不采信席甲编译日志为己证」，空库 92/92 仍只证明第 11 段用例自身绿，不能消解 001/004/005。

第 17 条：main 打卡白名单 `nfc/qr/manual`；本材料改为 `nfc/qr/charging_disconnect` 并拒 `manual`，与 BEQR-183 / BSM L1445 同向。011 未把「删 manual」判添加。

席甲 OBS-4「`work_date` / `end_at` 两把尺子」维持登记：`listMyShifts` 仍按工作日窗口取班（L1444 看未来排班），月历按结束时点（L1449）。两把尺子服务不同句子，不升冲突。

定稿时 011 不必把 §8 逐条抄全；不得写成「Grok 无观察」或把 15/17 改成缺陷入队。

### 001 独立裁定（技术性／须修是）

见 (c)。写读键漂移是工程缺陷，无业务发明。须修是。票 **PASS**。

### 002 独立裁定（冲突同根／须修是）

`StaffSelfRegistrationRequest` 仅 idempotencyKey/companyId/merchantId/name/phone/idDoc。HTML PGM L579 / BSM L935「安装时生成并绑定该手机专属的设备标识」。冲突、须修是。处置只引用 U05-003。票 **PASS**。

### 003 独立裁定（冲突同根／须修是）

`:128` → U05 `:89-91` `merchant.selfOperated()`。操作员已答删除独立布尔。本段不改、不新开 2.2。票 **PASS**。

### 004 独立裁定（冲突／须修是）

见 (b)。本材料新引入锁定+无入口。票 **PASS**。

### 005 独立裁定（冲突同根／须修是）

`getShiftVisibility` 缺行 `(7,null,null)`。U05-005 已定种子或 fail-closed、上下限数值不发 2.2。本段 GET 不得把空上下限当出厂完成。票 **PASS**。

### 006 独立裁定（技术性同根／须修是）

`listProfiles` `:288`、`requireProfileInScope` `:1067`、`listMyShifts` `:828` 空值兼容。同根 U05-010。与 001 同修读侧。票 **PASS**。

### 007 独立裁定（技术性同根／须修是）

only-9 overlay 本席 B1：`MVN_EXIT=1`，`cannot find symbol` ×84 + 签名不匹配 ×10，首条缺 `StaffApprovalService`。五文件 HEAD=基线，覆盖不回退 M009–M012 字节（那些修复不在此文件），但 OpenAPI `phone_hash`、V210 CHECK 无 `charging_disconnect`、main 已 V410 会冲突。最小处置与 U05-008 同一句话。票 **PASS**。

### 008 独立裁定（冲突 main 既有／须修否）

见 (a)。PGM L287/L965（锁出处行 PGM-066 → L951 为 h2）报错须全中文。三句基线已有，副本收窄。归 M054。票 **PASS**。

### 009 独立裁定（冲突 main 既有／须修否）

BEQR L282 闭集含已撤销、已销假及考勤恢复/冲回。基线与副本均无实现，diff 未补也未改坏。归 M054。票 **PASS**。

### 010 独立裁定（添加 main 既有／须修否）

见 (a)。BEQR L279 闭集无「未打下班卡」。分类保持添加。基线已有同一状态，本段不弹窗。归 M054 的 2.2。票 **PASS**。

### 011 独立裁定（登记／否）

见 (e)。票 **PASS**。

### 分类 / 队列 / 给发布者的最小边界

本批可定稿（须席甲本批亦 PASS）。发布前须修是=001–007。否=008–011。添加 1 为 main 既有，不进 BLOCKED-ASK。questions 0。

发布者最小集（不在本轮改源）：

- 001：经营主体键写读同一格式（`merchant:` / `company:<companyId>`），不得回落 `projectId`；补 `company:` 写读回归。
- 002：自助注册 DTO 随 U05-003 加私人手机设备公钥与指纹。
- 003：调用面不必改；U05-001 改读 `cooperationForm=="self_operated"`。
- 004：顶班记录入口（不走上一级审批，写入顶班人员与原班次）；或去误导文案并 2.2 砍范围。
- 005：随 U05-005；本段 GET 不得把空上下限当出厂完成。
- 006：随 U05-010 去 null 放行、写入强制 `company_id`。
- 007：以 main 445a49f4 为底重放；U05+U06+U07 同一 PR；打卡 CHECK 与 OpenAPI 字段同批；迁移 ≥V411；禁止整树覆盖。
- 008/009/010 不在本材料关门，记入 M054。
- 禁止：整树覆盖 main；为本段 `missing_checkout` 再弹 2.2；把顶班做成上一级审批；员工读侧无参复用 `staffSettingScope()`；把「删 manual」判添加。

非阻断措辞修正（不定稿作废，不必 r2）：

1. 004 HTML 行号以 **L1443** 为准（本席 B1 写 L1441 是标题）。
2. 010「未加重」宜点名 BSM L1449 归期原句与 BEQR L279 同日多班次，避免读成「函数改写了却仍称未加重」。
3. 001 写明函数入参 `(merchantId, companyId)`，写侧=配置者、读侧=员工档案。
4. 008 可并列 `:843` 夹注，仍归 M054。
5. 002 锚点 PGM L579 / BSM L935 比本席 B1 的 L576 / L1957 更贴句子。

### 结论

全批差集无漏并。008/009/010 按 U01「main 既有」口径成立，本材料未加重（010 归期贴 L1449，不构成本段把第六态加码）。004 冲突成立，补入口不越界。001 并列未改写 GR-007，不得回落 `projectId` 是最懒。002/003/005/006/007 同根只引用。011 未写错 §8，第 15/17 条在。逐 ID 001–011 全 **PASS**。本席 B2-r1 **PASS**。未经双 PASS 不得把候选当定稿，更不称 U06 或 main 模块 PASS。步骤仍 B2；U06 IN_PROGRESS；材料未发布，不做 B3。
