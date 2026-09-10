CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U05 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 只读全量三线（未发布材料第 5 段；不做 B3/B4/B5；处置写给发布者，本轮不修源）
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 段：U05 — 未发布材料第 5 段「员工服务与数据访问」
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 末尾附录；`plan/runbook.md` V0.02 §11；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 审计对象（不得冒称 main 或已发布修复）：
  - 未发布提交：`03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb`（本仓 `git cat-file -t` **无此 commit 对象**）
  - 未发布树：`e28dad059b2c7e08bcc95a8db18552d3f76edf4b`（`git cat-file -t` = **tree**，可解析）
  - 发布基线：`242f487233d028173cb9198eecd079f5d877405c`（commit，可解析）
  - 只读输入根：`/home/james/complex-management/.scratch/module-recheck-20260908/unpublished-03acdb98`
  - 清单：`.scratch/module-recheck-20260908/unpublished-03acdb98-input.json` V0.01，76 文件
- 本段：7 文件，物理行 2098，标记 NORMAL；main 主归属 M054（4 个新增文件 main 无此文件）
- 冻结判据：M054 映射行并集 52 项（与锁行并集相同；COUNT=52 unique=52 MISSING=[] EXTRA=[]）：GEN-001…GEN-018、BEQR-157、BEQR-182…187、BSM-001/003/009/028/029/073/084–088/114/115/120、PGM-001/008/022/024/026/032–036/048/059/066
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 且发布前须修：GR-U05-001 … GR-U05-010
- 添加项（B1 只登记，不代用户回答 2.2）：无。GR-U05-001 按冲突分类（六类合作方式已含「自营」）；与已定稿 U04-001（添加、操作员已答「删除」）同根读路径，发布者按该答复改读 `cooperationForm == "self_operated"`，本席不新开 2.2
- 只登记、建议队列「否」：观察见 §8
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做写操作
- 本报告不表示全仓、main 模块或发布门禁通过。M001–M012 检查点已合入当前 HEAD（PR #428/#429）。原 322/471 模块结论不继承到本未发布段。不得把本副本冒称 main 源码或已发布修复。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`（1872 bytes，SHA256 `fa5c1e5ee76b1c8f1ae56101f52c9e239de75d5c578adc61227b92d10b7e52ef`） | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | `/tmp/u05-b1-javac` 单文件 javac + `/tmp/u05-b1-mapof` `Map.of` 空值探针 + 七文件覆盖 main staff 目录的只读 overlay 对照；不改生产代码 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流（本段 7 文件 → U06 StaffService/DTO → U07 SecurityConfig → U08 MerchantRecord.selfOperated → U10 V400–V409 → main 对照 → HTML） |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；未跑的后端集成 / 共享库测试不写通过 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度（字段注入、兼容构造器、仓储双写入口），不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本段验证均秒级，未起 monitor |

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
- sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA 不同，属正常）
- `criteria-lock.md` 版本 V0.11；判据总数 401
- 映射行：`M054 →` L6433 闭集 52 项，python 集合差集 MISSING=[] EXTRA=[]，与开工提示闭集相同
- `plan/modules.md` 附录 U05 七路径、行数、main 归属与用户清单逐字相同

HEAD（独立实算）：

- `git rev-parse HEAD` = `git rev-parse origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`
- `git log -1 --oneline`：`445a49f4 Merge pull request #429 from jxh1979/audit/20260909/M011-M012-checkpoint`
- 本席对照 main 已修项时以 **当前 HEAD=445a49f4** 为准（含 M001–M012）
- 本席未设 `GIT_INDEX_FILE`，未动主索引

ledger 只读核验（不改）：V2.55，473161 bytes，SHA256 `b281db9efb2078eb8d78410a0b92a2d73be129315f923ca4fd2516d7ad320c4d`（与开工提示一致）。findings V1.51，573200 bytes，SHA256 `9deaeb0b1a60a7f124f88b1ad880e1c209cf02ef0c2acab301652a8436aaa506`（与开工提示一致）。台账中与本段直接相关的已定稿项（背景，不继承 PASS）：

- U04-001：`selfOperated` 第二轴为添加，操作员 2026-09-09 答「删除」；处置写明 **U05 `StaffRegistrationService` 改读 `cooperationForm`**
- U03-007 / U02-002 / U01-007：自营布尔同批、自助注册契约缺设备公钥/指纹
- U01-003：安卓文案「不可逆加密」vs 本段 `PiiCipher.encrypt` 可逆——本段实现面与 HTML「加密存放 + 界面脱敏」同向，安卓文案归 U01
- M009-003：main 合作方式六类技术码（改的是 `PlatformService` / 商户页，**未改** 本段三份同名 Java）

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

清单本段 7 条与独立 `sha256sum`/`wc`/`hash-object` **7/7 match**。副本 `git hash-object` 与树 `e28dad05:<path>` blob **7/7 相同**。全 76 文件相对基线 numstat 合计 **+4745 / −1084**（独立 `git diff --numstat` 实算，与开工提示一致）。

对象核验：`git cat-file -t 03acdb98…` fatal（无 commit）；`e28dad05…` = tree；`242f4872…` = commit。

### 1.1 HTML 锚点实核（锁出处行 → 现行正本）

| 判据 | 锁出处 | 现行该行含 |
|---|---|---|
| BEQR-157 | beqr:240 | `员工状态` / `待入职、在职、试岗、停岗、离职` |
| BEQR-182 | beqr:279 | `考勤日结果` |
| BEQR-183 | beqr:280 | `打卡方式`（领取工作手机三方式） |
| BEQR-184 | beqr:281 | `请假类型` 事假、病假、年假、调休 |
| BEQR-185 | beqr:282 | `请假单状态` |
| BEQR-186 | beqr:283 | `薪资单构成`（20% 上限、总薪资定义、事假另算） |
| BEQR-187 | beqr:285 | `员工提成发放` 月结、银行、不许现金现抽 |
| BSM-001 | bsm:505 | `整体说明：这套系统到底管什么`（含可配原则与上下限） |
| BSM-003 | bsm:563 | `系统通用能力：数据可改可删、操作有引导、权限分岗`（存档删除细则 L566–573） |
| BSM-009 | bsm:596 | `基础管理` 员工/设备/权限 |
| BSM-028 | bsm:698 | `主要功能` 六类合作方式 + 员工注册审批 |
| BSM-029 | bsm:714 | 用户权限要知道员工注册信息、设备绑定… |
| BSM-073 | bsm:1313 | 商户授权范围内操作本商户员工 |
| BSM-084 | bsm:1427 | 员工手机端 h2 |
| BSM-085 | bsm:1431 | 日常工作；L1433 两条注册路径 + 自营视同公司自有 |
| BSM-086 | bsm:1441 | 排班上一级审批、可见天数出厂 7 天须上下限、打卡、请假扣薪 |
| BSM-087 | bsm:1455 | 我的薪资（20% 上限、月结可见） |
| BSM-088 | bsm:1467 | 提成进月结、不许现抽 |
| BSM-114 | bsm:1827 | 各类入口只看自己该看的 |
| BSM-115 | bsm:1897 | 基础管理连所有业务 |
| BSM-120 | bsm:1957 | 已拍板：私人手机设备标识；寄存柜扫码自动留痕不设手工登记 |
| PGM-001 | pgm:256 | 治理层底座；见名可点；界面全中文 |
| PGM-008 | pgm:362 | 员工和手机入口卡 |
| PGM-022 | pgm:463 | 空间和点位 |
| PGM-024 | pgm:476 | 设备和员工班次要知道责任区域 |
| PGM-026 | pgm:492 | 六类合作方式 |
| PGM-032 | pgm:572 | 人、私人手机、工作手机要能对应起来 |
| PGM-033 | pgm:576 | 两条注册路径；安装时绑定设备标识；寄存柜扫码不设手工登记 |
| PGM-034 | pgm:586 | 工作手机和企业微信（本段无企微实现面） |
| PGM-035 | pgm:599 | 提成进月结银行发放 |
| PGM-036 | pgm:611 | 设备绑定员工、班次、点位 |
| PGM-048 | pgm:749 | 商户侧授权范围内全开放、不能跨商户 |
| PGM-059 | pgm:868 | 个人信息加密存放；日常界面只显示脱敏号码，脱敏不提供关闭开关 |
| PGM-066 | pgm:951 | 已拍板规则（全中文、留痕、无孤岛） |

对照用（非本段冻结 ID，不支撑 finding）：BEQR 合作模式六类技术码 `self_operated`（main M009-003）；U04-001 已答删除。

## 2. 7 文件覆盖表（全文已读；行=`\n` 计数 = splitlines）

副本根：`unpublished-03acdb98/`。清单本段条目 sha256/行/字节与独立实算 **7/7 match**。副本 `git hash-object` 与树 blob **7/7 相同**。文件以 `\n` 结尾、无 CRLF。

| # | 路径 | 行 | 字节 | SHA256 | 树 blob | 全文 |
|---|---|---:|---:|---|---|---|
| 1 | `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | 1257 | 80826 | `ac0df6e6ce81d86c71366daa5348cf1a5d5fd36190f4d9c713efee0552b163a6` | `f4cd20ff1b3077976a749451cb6f1e7c036927c6` | 已读 L1–L1257 |
| 2 | `…/StaffApprovalService.java` | 104 | 5530 | `d1b6358d34e2603974b6b481e33fc19ab199edbd1821f49df688695debee1497` | `9680a321ed2c62176e1dee57862ae190480049ae` | 已读 L1–L104 |
| 3 | `…/StaffController.java` | 370 | 19424 | `746276dccc2c5a46c92b9999a7ecbce0fd34e8818656250243ffcb72d6c6e6da` | `c7993946d571a8245c0a8536be601e66385494f7` | 已读 L1–L370 |
| 4 | `…/StaffLockerService.java` | 85 | 5723 | `89cd15c99d1d59dd8e20ede570509dadaaf5749ca1778d4dd6ffb06a3d4b6271` | `40e5fc873b2ea151ebce2f70b1e20076315b7434` | 已读 L1–L85 |
| 5 | `…/StaffNumericSetting.java` | 5 | 241 | `2e6b6eeeb98346251fac08f046b62e8bf8214f27177a85803ffe99d5255780ed` | `f69137b9e28ca97ad727b0e3dd6841cc084364b1` | 已读 L1–L5 |
| 6 | `…/StaffProfileRecord.java` | 139 | 8048 | `2ec7d1330133c0cf338da3a8b5cb9a17e64bd8c635364e7fd39318db5acebd83` | `1d55dd758f8f3e2967b5d65cf264f1059d2a63e0` | 已读 L1–L139 |
| 7 | `…/StaffRegistrationService.java` | 138 | 7892 | `418bf16647a304a4c5fe6e4b5ac2f106638f9ef945aae739be227a946a283bdf` | `8f55deb771cfa4860503bb7f089ead4d569eddb5` | 已读 L1–L138 |

合计 2098 行 / 127684 字节。本段无测试文件、无迁移、无 OpenAPI（调用链只读到副本 U06/U07/U10 与 main）。

### 2.1 相对发布基线的逐文件 diff stat（真实改动）

命令：`git diff --stat 242f487233d028173cb9198eecd079f5d877405c e28dad059b2c7e08bcc95a8db18552d3f76edf4b -- <本段 7 路径>`

```
 .../platform/access/staff/JdbcStaffRepository.java | 370 +++++++++++++++++----
 .../access/staff/StaffApprovalService.java         | 104 ++++++
 .../platform/access/staff/StaffController.java     |  79 ++++-
 .../platform/access/staff/StaffLockerService.java  |  85 +++++
 .../platform/access/staff/StaffNumericSetting.java |   5 +
 .../platform/access/staff/StaffProfileRecord.java  |  84 ++++-
 .../access/staff/StaffRegistrationService.java     | 138 ++++++++
 7 files changed, 782 insertions(+), 83 deletions(-)
```

numstat：

| 路径 | + | − |
|---|---:|---:|
| JdbcStaffRepository.java | 311 | 59 |
| StaffApprovalService.java | 104 | 0 |
| StaffController.java | 70 | 9 |
| StaffLockerService.java | 85 | 0 |
| StaffNumericSetting.java | 5 | 0 |
| StaffProfileRecord.java | 69 | 15 |
| StaffRegistrationService.java | 138 | 0 |
| **合计** | **782** | **83** |

基线 blob：三份已有文件与 main HEAD **逐字节相同**（见 §6）；四份新文件基线不存在。

### 2.2 JdbcStaffRepository 结构覆盖（1257 行，不能只看 +311）

| 行区间 | 块 | 覆盖 |
|---|---|---|
| L1–L35 | 仓储头、PiiCipher、`STAFF_PROJECT_SUBSELECT` | 已读 |
| L39–L78 | 处罚上限修订 / `staff_policy_settings` 读写 | 已读 |
| L80–L255 | 底薪/奖惩/提成写入 + `closePayrollMonth`（封顶、事假工时、company_scope） | 已读 |
| L257–L302 | 发放、薪资单映射 | 已读 |
| L304–L420 | 排班/考勤/薪资/请假真分页 + `STAFF_MERCHANT_FILTER` + company_scope | 已读 |
| L423–L563 | 税表、请假 CRUD、重叠、批准日期展开 | 已读 |
| L579–L840 | 排班修订发布、upsertShift 直写、考勤、点位 JSON | 已读 |
| L844–L998 | 档案加密 UPSERT、发起人、按公司/项目列表、引用计数（含柜未取） | 已读 |
| L1013–L1256 | 设备凭证、通知、佣金行、mapProfile 解密、存档理由 | 已读 |

## 3. 三线审计

### 3.1 第一性原理

| 文件 | 为什么必须存在 | 声称是否执行 | 未验证前提 |
|---|---|---|---|
| JdbcStaffRepository | 删除则 BSM-009/028/084–087、PGM-059 的员工持久化、月结、隔离、加密读写做不到 | SQL 与 record 字段对齐；月结/修订/加密路径有副本测试文件但**未跑**（会写 `complex_test`） | 目标库是否已有 V400–V409；`hashtextextended`/`pg_advisory_xact_lock` 仅柜机服务使用 |
| StaffApprovalService | BSM-086「排班表最终发布须经该主管的上一级审批」；BEQR-186 扣除上限三审 | `evaluate` 策略 `blocksByDefault=true` 且 `amountCent=0>=0` → 必进 pending；`afterDecision` 只在 `approve()` `publishEvent` 后跑（evaluate **不**发事件，无「先事件后写修订」竞态） | 审批链节点是否已为发起人维护直属上级（失败时 HTML 要求卡住，实现抛 `APPROVAL_CHAIN_UNRESOLVED`） |
| StaffController | 员工三表与本段新入口的 HTTP 面 | 转发 + `maskedPersonalInfo`；公开注册两路依赖 U07 `permitAll` | 单独合入本文件、main `StaffService` 无新方法 → 编译失败（§6/GR-U05-005） |
| StaffLockerService | BSM-120/PGM-033 寄存柜扫码自动留痕、不设手工登记 | 存/取、占用、幂等、未取计入删除引用；身份模型见 GR-U05-003 | 柜机适配「实际扫码」在本段之外 |
| StaffNumericSetting | BSM-001/086 可配阈值形状 | 出厂缺省 min/max 为 null（GR-U05-007） | 首次配置由 U06 `requireNumericSetting` 强制上下限 |
| StaffProfileRecord | 档案状态机 + 雇佣五态 + 公司路由 + 脱敏副本 | 五态与 BEQR-157 对上（技术码）；脱敏只盖手机/证件 | 兼容构造器把 `companyId` 置 null（§8） |
| StaffRegistrationService | BSM-085/PGM-033 两条注册路径的公司自有半边 | 公开申请只落 `submitted`、`userId=null`、不发账号；自营分流读 **独立布尔**（GR-U05-001）且 **不绑设备**（GR-U05-002） | `MerchantRecord.selfOperated()` 只存在于未发布 U08，main 无此方法 |

删除任一本段新文件：公开自助注册、柜机留痕、排班审批落库、雇佣五态、处罚上限版本化会从本材料消失。旧三文件若回退到 main/基线：PII 回到不可逆哈希（日常无法脱敏回显）、无公司路由、无柜/自助入口。

### 3.2 顺从式（假定意图正确）

意图：把 HTML 两条员工注册路径、寄存柜自动留痕、排班上一级审批、可见天数/处罚上限可配、雇佣五态、PII 加密+接口脱敏接到现有员工三表。

与意图同向的证据：

- 自助注册公开、管理/审批仍 401（副本 `M010RegistrationHttpTests`，本席未跑；代码上 U07 `publicApiPaths` 仅列 `registration-companies` 与 `self-registrations`）
- 档案 `saveProfile` 对 name/phone/id_doc 走 AES-GCM 信封；控制器列表/详情/自助一律 `maskedPersonalInfo`（`138****8000` / `******`），与 PGM-059「日常界面脱敏、无关闭开关」同向
- 排班用户路径改走 `submitShifts` → 修订表，批准前 `staff_shift_schedules` 无行（副本回归测试声称；仓储仍保留 `upsertShift` 直写给测试/种子）
- 处罚上限 2000 bps=20%，变更走人力/财务/法务平行线 + 主线最高权限；已月结行锁定 `penalty_cap_bps`
- 事假扣薪按班次重叠秒、只计 `leave_type='personal'`，分母为当月结束班次工时（相对基线「按日历天数」更贴近 BSM-086）
- 雇佣五态 CHECK 与前端中文映射 `pending_entry→待入职` 等一致
- 柜格 `FOR UPDATE` + 部分唯一索引 + advisory lock；取出后他人可存

意图未闭环处升级为 §7 finding，不在顺从式里改口。

### 3.3 对抗式（假定实现错误；grilling 树，不问操作员）

Frontier 已自行取证：

1. **第二自营轴能否与六类分叉？** 能。`StaffRegistrationService:66` `merchant.selfOperated()`，不读 `cooperationForm`。main `MerchantRecord` 无 `selfOperated`。副本 U08 记录二者并列。→ GR-U05-001
2. **自助注册能否在安装时绑设备标识？** 不能。`StaffSelfRegistrationRequest`（U06）与 `selfRegister` 均无公钥/指纹；只插档案。→ GR-U05-002
3. **柜机留痕能否由柜子单独完成？** 不能。必须 `staff.profile.self` 当前员工档案 **且** `TerminalIdentityHolder` 为已验证 `kiosk`。私人手机不是 kiosk；无员工 Bearer 的柜机请求过不了 `currentActor`。测试只覆盖「两身份同时塞进同一请求」。→ GR-U05-003
4. **读历史是否写锁？** 是。`shiftRevisions` SQL 恒 `FOR UPDATE`，`history()` GET 走它。→ GR-U05-004
5. **只合本 7 文件到当前 main？** `StaffRegistrationService` 调用 `selfOperated()` 而 main 无此方法；`StaffController` 调用 `setEmploymentStatus`/`getPenaltyCap`/`shiftHistory` 等而 main `StaffService` 无这些方法；依赖 U10 `V400–V409` 而 main 已有 `V410`。三同名文件 **文本上 main==基线**，覆盖不会丢掉 M009–M012 在这些文件里的修补（那些修补不在这些文件），但语义/编译/迁移仍分叉。→ GR-U05-005
6. **20% 封顶分母是否 HTML 总薪资？** SQL 只用 `monthly_salary_cent + bonus`，提成排除（对），补贴/绩效栏不存在也不入分母。→ GR-U05-006
7. **可见天数出厂有无上下限？** `numericSetting` 缺行返回 `(7,null,null)`；V402 只种子处罚上限。→ GR-U05-007
8. **自营商户+所选公司校验是否过宽？** `companyChain(merchant).getFirst().id()` 等于最近一家公司，不是「所选公司是否在链上」。→ GR-U05-008
9. **company_id 空是否漏隔离？** `COALESCE(company_id, project_id)` 与 list 谓词把未填公司的档案并进项目/公司范围。→ GR-U05-009
10. **新写接口是否幂等？** `POST .../employment-status`、`POST .../settings/shift-visible-days` 无 `requireIdempotencyKey`。→ GR-U05-010

未把理论极端当 finding：例如「无审批链时排班应自动生效」——HTML 要求卡住，实现拒绝，首轮不会静默发布。`evaluate` 同步发事件导致修订未写——evaluate 不 `publishEvent`，首轮不发生。

## 4. 适用冻结判据逐项

| ID | 本段适用 | 证据 | 缺口 |
|---|---|---|---|
| GEN-001 | 是 | 七文件均有 HTML 承载；兼容构造器/`COALESCE` 为存量设计 | GR-U05-009 |
| GEN-002 | 是 | 单文件 `StaffNumericSetting` javac 0；整段合入 main 缺方法/缺列 | GR-U05-005/008 |
| GEN-003 | 是 | 本段 0 测试文件；副本 `M010RegressionTests`/`M010RegistrationHttpTests` 覆盖本段行为，**本席未跑**（写库） | 不写通过；柜机测试缺口见 GR-U05-003 |
| GEN-004 | 是 | 公开注册有长度校验；柜机事件未把「柜扫码」与员工身份拆开 | GR-U05-003 |
| GEN-005 | 是 | 管理接口经 `StaffService` `@GuardedAction`；公开两路 permitAll 符合「尚无账号」；列表 company 回落见 009 | GR-U05-003/009 |
| GEN-006 | 是 | 柜格部分唯一索引、修订 `updated_at` 乐观锁、处罚 `before_value` 比对 | GR-U05-001 第二轴与六类可分叉 |
| GEN-007 | 是 | 注册/柜/排班草稿走幂等；employment-status、shift-visible-days 无键；history FOR UPDATE | GR-U05-004/010 |
| GEN-008 | 是 | 雇佣五态前后端映射一致；自营布尔 vs 六类码不一致；仓储缺省 `(7,null,null)` vs 表 NOT NULL | GR-U05-001/007 |
| GEN-009 | 是 | 快照编解码失败抛 conflict；`parsePoints` 吞 IO 返空列表（基线既有，diff 未改，观察） | — |
| GEN-010 | 是 | 落库加密、接口脱敏、脱敏无开关 | 与 U01-003 文案面分离；本段实现同向 HTML |
| GEN-011 | 是 | 注册/柜/雇佣/可见天数/上限均 `audit.recordSuccess`；排班提案含姓名 | — |
| GEN-012 | 是 | 分页封顶 500/1000；history 行锁 | GR-U05-004 |
| GEN-013 | 是 | 依赖 U06/U07/U08/U10 同树；单独合入失败 | GR-U05-005 |
| GEN-014 | 是 | 本段无迁移文件；行为依赖副本 V400–V409；main 最大已是 V410，原号段不可直接落地 | GR-U05-005 |
| GEN-015 | 无界面实现面 | 本段纯后端；展示中文由 U03 页 `EMPLOYMENT` 映射。错误文案中文。API 技术码属「系统内部标识符」四例外 | 不升冲突 |
| GEN-016 | 是 | 控制器注释「权限由 StaffService 执行」与转发相符；`StaffNumericSetting` 注释「上下限留空」与缺省实现相符（该相符本身违反 BSM-001） | GR-U05-007 |
| GEN-017 | 是 | 分类顺序冲突→细化→技术性→添加；未把 HTML 沉默当冲突 | 见 §7 |
| GEN-018 | 是 | 结论落到 HTML 原句、git/sha256/javac/`Map.of` 输出 | — |
| BEQR-157 | 是 | 五态技术码+CHECK+接口校验文案列中文五态 | 观察：JSON 仍英文码 |
| BEQR-182 | 仓储承载 judgement/请假展开 | 判定逻辑在 U06 `StaffService` | 本段无冲突 |
| BEQR-183 | 仓储 `findFirstCheckIn`/method 列 | 打卡三方式在 U06 | 本段无冲突 |
| BEQR-184 | `leave_type` 透传 | 枚举闭集在 U06 | 本段无冲突 |
| BEQR-185 | 重叠只计 pending/approved | 撤销/销假列在 U06 | 本段无冲突 |
| BEQR-186 | 新增 20% 封顶 SQL | 分母≠HTML 总薪资；无结构化薪资项表 | GR-U05-006 |
| BEQR-187 | 提成入月结、`markPayrollPaid` 无现金口 | 银行发放在本段之外 | 本段无冲突 |
| BSM-001 | 可配+上下限 | 可见天数出厂无上下限行 | GR-U05-007 |
| BSM-003 | 档案存档/删除字段与仓储 | 审批链在 U06 | 本段无新冲突 |
| BSM-009 | 员工/岗位/班次/注册 | 入口存在 | GR-U05-001/002 |
| BSM-028 | 六类合作+员工注册审批 | 注册分流不读六类 | GR-U05-001 |
| BSM-029 | 注册信息/设备绑定 | 自助注册不绑设备 | GR-U05-002 |
| BSM-073 | 商户范围员工 | `STAFF_MERCHANT_FILTER` | GR-U05-009 公司回落 |
| BSM-084 | 员工自助 API 前缀 | `profiles/mine` 等仍转发 | — |
| BSM-085 | 两条路径 | 公司自有半边有；自营读布尔；商户半边 `merchantApprovingCompany` 在本文件 | GR-U05-001/008 |
| BSM-086 | 上一级审批、可见天数、班次锁定 | 审批服务+修订表；可见天数缺省无上下限 | GR-U05-007 |
| BSM-087 | 处罚/事假分栏、20%、月结可见 | 封顶公式分母窄 | GR-U05-006 |
| BSM-088 | 提成进月结 | 佣金行 UNION 酒吧提成 | — |
| BSM-114 | 只看自己的 | 公开公司列表为注册所需；company_id 空回落 | GR-U05-009 |
| BSM-115 | 基础定义卡住后续 | 自营布尔与合同合作方式可分叉 | GR-U05-001 |
| BSM-120 | 设备标识；寄存柜自动留痕 | 柜表+服务存在；身份组合错误 | GR-U05-002/003 |
| PGM-001 | 底座/中文/可点 | API 技术码；本段无 UI | — |
| PGM-008 | 注册/绑定/岗位状态 | 雇佣五态新增；设备绑定仍在凭证表 | GR-U05-002 |
| PGM-022 | 班次点位 JSON | `point_space_ids` | — |
| PGM-024 | 班次责任区域 | 同上 | — |
| PGM-026 | 六类合作 | 同 GR-U05-001 | GR-U05-001 |
| PGM-032 | 人-私人手机-工作手机对应 | 自助注册不绑私人手机标识 | GR-U05-002 |
| PGM-033 | 两条路径+安装绑定+柜 | 路径有；绑定无；柜身份组合错 | GR-U05-001/002/003 |
| PGM-034 | 企微 | 本段无实现面 | 不升冲突 |
| PGM-035 | 提成月结银行 | 发放台账存在 | — |
| PGM-036 | 设备绑定员工 | 凭证表 CRUD 仍在 | — |
| PGM-048 | 商户不跨户 | merchant_id 过滤 | GR-U05-009 |
| PGM-059 | 加密+日常脱敏 | 控制器强制 mask | — |
| PGM-066 | 留痕/中文已拍板 | 审计事件有 | — |

## 5. 数据流（本段 → 副本其它段 → main → SQL/迁移 → HTML）

```
StaffController (U05)
  ├─ registrations.companies / selfRegister → StaffRegistrationService (U05)
  │     ├─ platform.listBusinessEntities / findMerchantById / findActiveBusinessEntityBinding
  │     │     → 副本 U08 JdbcPlatformRepository / MerchantRecord.selfOperated
  │     │     → main MerchantRecord 仅 cooperationForm，无 selfOperated（编译断）
  │     └─ staff.saveProfile → JdbcStaffRepository 加密写入 staff_profiles
  │           → 副本 V403 phone/id_doc、V404 employment_status、V408 company_id/registration_source/is_self_operated
  │           → main 无这些迁移；最大 V410
  ├─ lockers.record → StaffLockerService (U05)
  │     ├─ TerminalIdentityHolder + TerminalDeviceRepository（U07 设备段）
  │     └─ staff_locker_records ← 副本 V409
  ├─ service.* → 副本 U06 StaffService（main 无 setEmploymentStatus/getPenaltyCap/shiftHistory…）
  └─ maskedPersonalInfo → PhoneMask（main 有）+ 证件 ******

StaffApprovalService (U05)
  ├─ SupervisorReviewService.evaluate/approve（副本 U09；main 亦有审批链，策略条目以副本为准）
  └─ JdbcStaffRepository.saveShiftRevision / publishShiftRevision / penalty cap revisions
        ← 副本 V406/V407/V402
```

HTML：BSM L1433 / PGM L578 两条路径与自营视同公司自有；BSM L701 / PGM L495 六类已含自营；PGM L579 / BSM L1957 安装绑定设备标识；BSM L1957 寄存柜扫码自动留痕。

## 6. 与当前 main 分叉对照（对抗式固定问题）

命令：`git rev-parse HEAD:<path>` vs `242f4872:<path>` vs `e28dad05:<path>`

| 文件 | 基线 blob | main HEAD blob | 未发布树 blob | 关系 |
|---|---|---|---|---|
| JdbcStaffRepository.java | `936b34cd…cde678` | **相同** `936b34cd…cde678` | `f4cd20ff…36927c6` | main **未**改此文件；与基线无文本分叉 |
| StaffController.java | `6d4e6dc4…cc941b` | **相同** | `c7993946…5494f7` | 同上 |
| StaffProfileRecord.java | `cf066a43…72894da` | **相同** | `1d55dd75…f264f105` | 同上；main 仍为 `phoneHash`/`idDocHash` |
| 其余 4 文件 | 不存在 | 不存在 | 见 §2 | 未发布新增 |

`git diff --stat HEAD e28dad05 -- <7 路径>` = 与基线相同的 **+782/−83**（因 main==基线）。

因此：开工提示「与 main 同名的文件都已与 main 分叉（main 含 M009–M012 修复）」**对本段三份同名 Java 不成立**——M009–M012 没有改这些文件，覆盖它们 **不会** 丢掉那些检查点在本文件内的补丁（本文件内没有）。分叉在 **调用面与模式**：

- 未发布 `StaffRegistrationService` 调用 `MerchantRecord.selfOperated()`；main 记录无此成员（python 探针：`calls selfOperated True` / `main defines False`）
- 未发布 `StaffController` 调用 main `StaffService` 没有的 `setEmploymentStatus`、`getPenaltyCap`、`requestPenaltyCap`、`getShiftVisibility`、`setShiftVisibility`、`shiftHistory`
- 未发布 SecurityConfig（U07）才把两路注册 `permitAll`；main SecurityConfig **无** `/api/v1/staff/` 公开路径
- 模式：副本独有 `V400–V409`；main 已有 `V410__m010_business_entity_topology_and_role_isolation.sql`，缺 V400–V409。按仓库铁律 9，发布时必须把本材料迁移 **重编号到 >410**，不得原号落地
- 若发布者整树覆盖 U08 `MerchantRecord`/`PlatformService`，会打到 main 已修的六类合作方式（M009-003）——那是 U08 的覆盖风险，本段读路径是触发器

`/tmp/u05-b1-overlay`：把本段 7 文件覆盖进从 main 拷出的 `access/staff/` 后，main `StaffService.java` 仍无 `setEmploymentStatus`，与未发布 `StaffController` 并置即符号残缺。

## 7. Findings（临时 ID，正式号仅 B2 分配）

### GR-U05-001 — 员工注册分流读独立 `selfOperated` 布尔，可与六类合作方式分叉

- 位置：`StaffRegistrationService.java:66`、`:91`；调用 `MerchantRecord.selfOperated()`（副本 U08）；列 `V408` `merchants.is_self_operated`（U10，本段依赖）
- 原文：`if (!merchant.selfOperated() || !companyId.equals(companyChain(merchant).getFirst().id()))`；`if (merchant.selfOperated()) throw … "自营商户员工须在私人手机自助注册"`
- HTML：BSM L701/PGM L495 六类含「自营」；BSM L1433/PGM L578「在商户档案上标注为自营的商户视同公司自有」——标注落在既有「自营」类，而不是第二开关
- 业务影响：`cooperation_form=lease` 且 `is_self_operated=true` 走公司自助；`cooperation_form=self_operated` 且布尔 false 走商户代提交。两条路径、审批公司、账号生效条件被第二轴改写
- 可复现：`rg -n 'selfOperated' unpublished-…/StaffRegistrationService.java`；`rg -n 'boolean selfOperated' main/…/MerchantRecord.java` → main 0 命中。U04 类型探针已证两字段无约束（台账 U04-001）
- 冻结判据：BSM-028、BSM-085、PGM-026、PGM-033、GEN-006、GEN-008、GEN-017
- 2.3 依据：HTML 原文（类型 1）+ 可复现命令（类型 4）
- 四分类：**冲突**（六类已是「有哪几种」闭集；第二轴与之可分叉。不把 BSM-085「标注为自营」读成新布尔——那是对六类「自营」的引用）
- 工程缺陷：是。建议登记：是
- 最小处置（发布者）：删除布尔读路径；`selfOperated()` 改为 ` "self_operated".equals(merchant.cooperationForm())`（或等价六类码）。与 U04-001 已答「删除」同一处置，**不新开 2.2**

### GR-U05-002 — 公开自助注册不接收、不绑定私人手机设备标识

- 位置：`StaffRegistrationService.java:51–79`；`StaffController.java:51–56`；DTO 在 U06 `StaffSelfRegistrationRequest`（六字段，无公钥/指纹）
- 原文：`selfRegister` 只 `saveProfile`+审计；无 `saveCredential` / 设备表写入
- HTML：PGM L579 / BSM L1957「私人手机 App 安装时生成并绑定一个该手机专属的设备标识」
- 业务影响：审批通过后的档案可以没有安装时绑定；换机/不可信环境复核没有注册时点的设备锚。与 U01-007/U02-002 同根，本段是服务端未接契约
- 可复现：读 `selfRegister` 方法体；`rg -n 'device|fingerprint|publicKey' StaffRegistrationService.java` → 无业务绑定
- 冻结判据：PGM-032、PGM-033、BSM-120、BSM-029、GEN-002
- 2.3：HTML 原文（1）
- 四分类：**冲突**
- 工程缺陷：是。建议登记：是
- 最小处置：自助注册请求增加设备公钥/指纹（或等价安装标识），在申请落档或批准时绑定；不要求先有 `staffProfileId`。与 U01-007/U02-002 同一 PR

### GR-U05-003 — 柜机留痕要求「已登录员工 + 已验证 kiosk」同一 HTTP 请求

- 位置：`StaffLockerService.java:41–49`；入口 `StaffController.java:40–44`（**不在** `permitAll` 列表）
- 原文：`authorization.requirePermission("staff.profile.self")` 后 `TerminalIdentityHolder.current().filter(t -> t.verified() && "kiosk".equals(t.terminalType()))`
- HTML：BSM L1957 / PGM L578「开箱与取件一律扫码，是谁、哪一格、存取时间由柜子自动记录，不设手工登记」
- 业务影响：私人手机 App 不是 `kiosk`，单独打 `/locker-events` 403；柜机无员工 Bearer 则 `currentActor` 失败。首轮正常「员工扫柜码 / 柜扫员工码」无法由单一端完成自动留痕
- 可复现：读 L41–49；副本 `M010RegressionTests.lockerScanRecords…` 同时 `actor(staff)` **和** `setAttribute(verified_terminal kiosk)` 才成功——没有单端用例
- 冻结判据：BSM-120、PGM-033、GEN-004、GEN-005、GEN-003
- 2.3：HTML 原文（1）+ 代码/测试结构（4）
- 四分类：**技术性**（无业务发明：HTML 已要求自动留痕；缺陷是身份组合。不另造「必须手工登记」规则）
- 工程缺陷：是。建议登记：是
- 最小处置：柜机完成事件以已验证 kiosk 设备身份为准，员工身份从扫码内容/短时凭证派生，不要同时要求 `staff.profile.self` 会话与 kiosk 终端类型绑在同一请求；补单端可失败用例

### GR-U05-004 — 排班历史只读查询带 `FOR UPDATE`

- 位置：`JdbcStaffRepository.java:602–610` `shiftRevisions`；`StaffApprovalService.java:88–90` `history`；`StaffController.java:171–173` GET
- 原文：`SELECT * FROM staff_shift_revisions … FOR UPDATE`
- 业务影响：主管打开历史即持有行锁，与批准发布/并发修改抢同一修订行；读路径写锁
- 可复现：`rg -n 'FOR UPDATE' JdbcStaffRepository.java` 见 L47 处罚修订（写路径，合理）、L582/L587 找班次（提交时合理）、L607 历史（GET 不合理）
- 冻结判据：GEN-007、GEN-012
- 2.3：可复现命令（4）
- 四分类：**技术性**（无业务发明）
- 工程缺陷：是。建议登记：是
- 最小处置：`shiftRevisions` 按调用方拆读写；`history()` 去掉 `FOR UPDATE`

### GR-U05-005 — 相对当前 main 的编译/迁移发布分叉（本段固定对抗项）

- 位置：本段 7 文件整体 vs `445a49f4`；依赖 U06/U07/U08/U10
- 事实：三同名文件 main blob==基线；四新文件 main 不存在；`StaffController` 新方法在 main `StaffService` 为 false（python 逐项打印）；`selfOperated` main 为 false；main 迁移最大 V410，副本 V400–V409 号段已被越过
- 业务影响：只合本段 → 编译失败或运行期缺列/404；原号迁移落地会与 V410 乱序（铁律 9）；整树覆盖 U08 会打 M009 六类修复
- 可复现命令（exit 0 探针）：见 §6 python；`ls backend/src/main/resources/db/migration | rg '^V40'` main 仅 V410，副本 V400–V409
- 冻结判据：GEN-002、GEN-008、GEN-013、GEN-014
- 2.3：可复现命令（4）
- 四分类：**技术性**（无业务发明：是发布接缝，不是新业务规则）
- 工程缺陷：是。建议登记：是
- 最小处置：以当前 main 为底重放 diff；U05+U06+U07 同一 PR；自营读路径按 GR-U05-001 改六类码，不要带 U08 布尔插拔；迁移重编号 >410。禁止整树覆盖

### GR-U05-006 — 处罚封顶分母不是 HTML「总薪资」

- 位置：`JdbcStaffRepository.java:195–206`（相对基线 **新增** `CROSS JOIN LATERAL` 封顶；基线是 `COALESCE(adj.penalty,0)` 全额扣）
- 原文：`LEAST(COALESCE(adj.penalty,0), ROUND((COALESCE(b.monthly_salary_cent,0)+COALESCE(adj.bonus,0))*cap.value/10000))`；注释写「按总薪资比例封顶；提成不入分母」
- HTML：BEQR-186/BSM-087「总薪资指基础工资、各项补贴、绩效工资与奖金之和」；提成确实不在该定义内（排除提成正确）
- 业务影响：无补贴/绩效栏时，上限按「底薪+奖金」计算，低于 HTML 总薪资口径，可能少扣或与「结构化薪资配置」对不上。本材料改写了月结 SQL 却未按定义取分母
- 可复现：`git show 242f4872:…JdbcStaffRepository.java | rg 'adj.penalty'` vs 现行 L203–206
- 冻结判据：BEQR-186、BSM-087、GEN-008
- 2.3：HTML 原文（1）+ diff（4）
- 四分类：**冲突**
- 工程缺陷：是。建议登记：是
- 最小处置：封顶分母改为 HTML 总薪资各项之和（无独立补贴/绩效列则先把结构化薪资项落地，再封顶；不得用注释把底薪+奖金称作总薪资）

### GR-U05-007 — 排班可见天数出厂缺省没有上下限

- 位置：`StaffNumericSetting.java:4` 注释「未配置时采用出厂值；上下限留空」；`JdbcStaffRepository.java:61–67` `orElse(new StaffNumericSetting(defaultValue,null,null))`；U06 `getShiftVisibility` 缺省 7
- HTML：BSM L1441「可见天数由后台配置（…出厂默认 7 天，须同时配置上下限）」；BSM-001「未填写上下限的参数不得上线」
- 业务影响：未首次保存前，运行中的参数没有上下限，与「不得上线」相反。V402 只 INSERT 了处罚上限 0–10000，没有 `shift_visible_days` 出厂行
- 可复现：读 L61–67；`cat V402__staff_policy_settings.sql` 仅一行 penalty；隔离 `Map.of("min",(Integer)null)` → `NPE_AS_EXPECTED`（仓储 `saveNumericSetting` 用 `Map.of`，与缺省 null 形状不一致，首次保存被 U06 拦住故运行期不一定炸）
- 冻结判据：BSM-001、BSM-086、GEN-008、GEN-016
- 2.3：HTML 原文（1）
- 四分类：**冲突**
- 工程缺陷：是。建议登记：是
- 最小处置：迁移种子 `shift_visible_days` 出厂 7 且同时写上下限（HTML 未给默认上下限则先 2.2 或按「须同时配置」由发布者填批准值）；缺行不得返回 null 上下限

### GR-U05-008 — 自营商户归属校验用「链上最近一家」而非「所选公司在链中」

- 位置：`StaffRegistrationService.java:66–67`
- 原文：`!companyId.equals(companyChain(merchant).getFirst().id())`
- HTML：BSM-085 自营视同公司自有、经所属公司审批。所属公司应是链上对应主体，不是任意最近节点
- 业务影响：商户绑在酒吧经营主体（`management_company`）时，申请人选项目公司（祖先）会被拒；选错近节点才过。首轮自营+多层主体就会误拒
- 可复现：读 `companyChain` L122–136（自下而上收集 `COMPANY_ROLES`）+ L66 `getFirst()`
- 冻结判据：BSM-085、GEN-002
- 2.3：代码结构（4）+ HTML（1）
- 四分类：**技术性**（无业务发明：仍是「是否该公司自营商户」的实现错误）
- 工程缺陷：是。建议登记：是
- 最小处置：改为 `companyChain(merchant).stream().anyMatch(c -> c.id().equals(companyId))`（在 GR-U05-001 改读六类码之后）

### GR-U05-009 — `company_id` 空值回落到 `project_id` / 列表放行，属存量兼容设计

- 位置：`JdbcStaffRepository.java:231` `COALESCE(sp.company_id,sp.project_id)`；L328/L354/L382/L409 同类谓词；`StaffProfileRecord.java:44–64`「向后兼容构造器」默认 `companyId=null`
- 铁律 8：不为存量数据做兼容。新列应在写入路径强制；缺值应失败而不是并进项目范围
- 业务影响：未打公司路由的档案进入公司主管的排班/考勤/薪资/请假列表（BSM-114/红线④ 的公司粒度）。U06 `listProfiles` 对 `companyId()==null` 亦放行（调用链）
- 可复现：`rg -n 'COALESCE\\(p.company_id' JdbcStaffRepository.java`
- 冻结判据：GEN-001、GEN-005、BSM-114
- 2.3：可复现命令（4）
- 四分类：**技术性**（无业务发明：隔离回落不是新业务口径）
- 工程缺陷：是。建议登记：是
- 最小处置：去掉 COALESCE 与「companyId==null 则可见」；写入强制 `company_id`；兼容构造器不要为新字段填空

### GR-U05-010 — 新写接口缺少幂等键

- 位置：`StaffController.java:95–98` `POST /profiles/{id}/employment-status`；`:203–206` `POST /settings/shift-visible-days`（对比 `:40–56` 与薪资/排班均 `requireIdempotencyKey`）
- 业务影响：雇佣状态与可见天数重复提交可双写审计、双更新；与同控制器其它写口不一致
- 可复现：读上述行无 `requireIdempotencyKey`；U06 `setEmploymentStatus` 亦无 `idempotencyService.execute`
- 冻结判据：GEN-007、GEN-008
- 2.3：代码对照（4）
- 四分类：**技术性**（无业务发明）
- 工程缺陷：是。建议登记：是
- 最小处置：与其它员工写口相同：校验幂等键并按操作命名空间去重

## 8. 观察（无新根因 finding，完整登记不漏并）

1. 本段三同名文件 **main==发布基线**，M009–M012 未改这些路径；「同名即分叉」对本段不成立，分叉在依赖面（§6）。
2. 未发布提交对象 `03acdb98` 本仓不可解析，审查以树 `e28dad05` 与输入副本为准。
3. 雇佣五态 JSON 为英文技术码，后台页映射中文；按「系统内部标识符」四例外不升 PGM-066 冲突。
4. `selfRegister` 把 `status=active` 写在尚未批准、`userId=null` 的申请上；登录发账号在审批（U06）。不构成越权，易误读。
5. 公开 `GET /registration-companies` 列出全部在册平台/项目/域公司名称——自助注册选公司所需；HTML 未要求对匿名隐藏公司名。
6. 自助注册不校验手机号格式、不短信、不查重手机/证件；HTML 对员工注册沉默，不升冲突/添加。
7. `StaffController` 对 `StaffRegistrationService`/`StaffLockerService` 字段注入而非构造器注入（ponytail：可删复杂度）。
8. 仓储仍公开 `upsertShift` 直写生效表；生产 `StaffService.upsertShift` 已改审批。调用方仅测试种子。不另开缺陷。
9. `parsePoints` IO 失败返回空列表（基线既有，diff 未改）。
10. `PiiCipher` 对无 `enc:v1:` 前缀透传明文（存量兼容，cipher 不在本段）。本段写入走 encrypt。库将清空，不把回填当约束。
11. V086 仍留 `phone_hash` 列；本段 INSERT 改写 `phone`/`id_doc`，哈希列不再维护。
12. 本段 0 测试文件。副本 `M010RegressionTests`（含柜、雇佣、自助、上限、排班审批、加密脱敏）与 `M010RegistrationHttpTests` 会写 `complex_test`，本席未跑，不写通过。
13. `StaffNumericSetting` 隔离 javac --release 21 exit 0；`Map.of` 空值探针 `NPE_AS_EXPECTED`。
14. `COMPLEX_DATABASE_URL` 本环境 unset；未对数据库做只读查询。
15. 排班 `evaluate` 的 `amountCent=0` 且策略 `minAmountCent=0` → `amountHit` 恒真，故 `blocksByDefault` 必进审批。与「必须上一级批」同向。
16. 处罚上限策略固定 `hr_review/finance_review/legal_review` + `top_authority`，与 BEQR-186「人力、财务、法务三审并由顶层平台公司最终确认」同向。
17. `countProfileReferences` 计入未取柜记录，删除前须取空，与「离开拿走」同向。
18. `maskedPersonalInfo` 不按 `pii.contact_phone.view` 放明文，与「脱敏不提供关闭开关」同向（比 PhoneMask 注释更严，正确）。
19. U06 `StaffScopeTests` 仍 stub `repo.upsertShift`，与现服务改走 `StaffApprovalService` 可能脱节——测试文件不在本段，观察。
20. 控制器 `listShifts` 默认 size 500、考勤/薪资 1000，有封顶。

## 9. 验证命令与证据（只读）

未跑：任何 `SpringBootTest`、Flyway、会连 `complex_test*` 的脚本。不把副本测试写通过。

| 命令 | exit | 要点 |
|---|---:|---|
| `sha256sum docs/audit-2026-09/plan/criteria-lock.md` | 0 | `b012328f…6695a` |
| `git rev-parse HEAD origin/main` | 0 | 均为 `445a49f4…c48f` |
| `git cat-file -t 03acdb98…` | 128 | 无 commit 对象 |
| `git cat-file -t e28dad05…` | 0 | `tree` |
| `git diff --numstat 242f4872 e28dad05` 全 76 文件 | 0 | +4745 / −1084 |
| 本段 `--stat/--numstat` | 0 | +782 / −83，见 §2.1 |
| 七文件 `sha256sum`+`wc`+`hash-object` vs 树 vs 清单 | 0 | 7/7 |
| python M054 并集 | 0 | 52/52 MISSING=[] |
| python 清单 7 条哈希 | 0 | 7/7 MATCH |
| `javac --release 21 StaffNumericSetting.java` | 0 | `/tmp/u05-b1-javac` |
| `java MapOfNull` | 0 | `NPE_AS_EXPECTED` |
| python main vs 未发布方法探针 | 0 | `selfOperated` main false；六方法 main StaffService 全 false |
| overlay 拷贝 main staff + 覆盖 7 文件 | 0 | `StaffService` 仍无 `setEmploymentStatus` |
| `rg` HTML 锚点行 | 0 | §1.1 均命中 |

## 10. 结论

单席 B1：**FAIL**。

7/7 文件全文三线已覆盖；52 项冻结判据已逐项；与 main 分叉已对照（三同名文件无文本分叉，调用/迁移有硬分叉）；10 项工程缺陷成立；无未答 2.2（无添加项）；无有条件通过。发布前须按 §7 处理后再进入 B2 定稿编号。

## U05 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 46953 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，独立）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02 §7/§11；criteria-lock V0.11；modules.md V0.04 附录 U05（未发布材料第 5 段，7 文件 2098 行，main 归属 M054）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`。本段三份既有 Java HEAD blob = 基线 blob（`JdbcStaffRepository` `936b34cd…cde678`、`StaffController` `6d4e6dc4…cc941b`、`StaffProfileRecord` `cf066a43…93a106`）；main 自基线未改这些文件。
- 审计对象仍是只读输入副本 `.scratch/module-recheck-20260908/unpublished-03acdb98`（树 `e28dad059b2c7e08bcc95a8db18552d3f76edf4b`，基线 `242f487233d028173cb9198eecd079f5d877405c`），不是 main。三份 HTML 取 main 正本。未发布提交对象 `03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb` 本仓 `git cat-file -t` 仍不可解析。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B1 分类，不照抄席甲 B2 核实结论。
- 本席 B2-r1：**PASS**（001–012 逐 ID 全 PASS；添加 0，不新开 2.2；发布前须修「是」=001–011，「否」=012）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。映射行 `M054 → [BEQR-157 … PGM-066]` COUNT=52 unique=52 MISSING=[] EXTRA=[]。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要核对只读（副本 7/7 哈希、blob、HTML 锚点、SQL、`questions/U04-001.md` 答复首行、`/tmp/u11_tests.log`）；未重跑 mvn/测试、未装依赖、未碰 `complex_test*`。

此票只判断 findings.md V1.52 顶部「当前 U05 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、「发布前须修」是/否与最小处置。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。材料未发布，定稿后状态应记 REVIEWED-FAIL（有已定稿缺陷，处置写给发布者），不做 B3。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/U05-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 46953 | `863c75bd05f19a74d81b7ba8ed542659a26b8ab1f486816297a62d94ae99f7d3` |
| `docs/audit-2026-09/modules/U05-b1-self-raw.md` | V0.01 | 37895 | `c28190d2385dc9ef0a8dbade2aae03a2ef8305021946beabcfaefe72357df98c` |
| `docs/audit-2026-09/findings.md` | V1.52 | 583215 | `a6bf4393084441461480054bde76fa15a174f04f4674b1a87528807f2ec3029d` |
| `docs/audit-2026-09/ledger.md`（只读） | V2.56 | 475268 | `5089f0d2e755928a175607274e604c5aa67b4ea0ed1dab30293f4c2ba5763332` |
| `docs/audit-2026-09/questions/U04-001.md` | V0.01 | 2282 | `5a9e9dab904fe9b82ccb2e414ae07515ea7192c7ed404685c31828a126c2b3d1` |

候选形态：V1.52 顶部 U05-001…012；questions 0（001 沿用 U04-001 已答「删除」；005 上下限数值不发问）；无 HTML 修改申请。U05 ledger 行 IN_PROGRESS。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

HTML 正本哈希与 B1 一致：BEQR `4396bb71…1d66`、BSM `a7f7b7a3…66fa`、PGM `c4238afd…e71c`。副本本段 7/7 sha256/行/字节与树 `e28dad05` blob 全等（本轮复算）。`git diff --stat 242f4872 e28dad05 -- <7 路径>` = `+782/−83`。HEAD 三同名文件对 `selfOperated`/`setEmploymentStatus` 0 命中。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers | 先核技能；用户/提示词覆盖技能本体 |
| receiving-code-review | 先核 HTML/SQL/blob/grep/已答 2.2 再接受或反驳候选；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：002 应发是否=固定工资、001 冲突归并是否改写添加、003/004 升冲突是否成立、005 上下限是否须 2.2、008 是否写错 §6、012 是否写错 §8 |
| verification-before-completion | 声称前先跑 sha256/git rev-parse/diff/grep/HTML 锚点/questions 首行/u11 日志；本票不声称新的测试绿 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流（本段 7 文件 → U06 StaffService/DTO → U07 SecurityConfig → U08 MerchantRecord → U10 V400–V409 → main 对照 → HTML） |
| ponytail-review | 只评最小删除/收窄，不落地。001 改读六类码不新开 2.2；004 现有列先取全不新建补贴表；005 种子或 fail-closed 不发明上下限数字 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到**；未冒充调用 |
| Arc audit | 会派子代理；与双席冲突；**未用** |

### 完整差集（GR-U05-001…010、§8 观察 1–20；席甲 CX-U05-001…004、OBS-1…9）

漏并：无。两席 B1 全部临时 ID 仍落在 001–012。席甲独有 CX-U05-001 入 002，由本席本轮独立裁定，不因 B1 未列而否决。席甲 B1 把 CX-004 标添加、CX-002 标技术性、OBS-3 标观察——候选按本席 B1 升为冲突，本票复核该升格，不照抄席甲 B1。本席观察 1–20 全部落入 012（点名 1/4/12/15–19），未把 001–011 降成观察。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/须修 | 本席票 |
|---|---|---|---|
| U05-001 | GR-U05-001 + 席甲 CX-U05-004（勘误改分类） | 冲突（同根 U04-001 已答删除）/是/是 | **PASS（冲突已答删除读路径/是/是；不新开 2.2）** |
| U05-002 | 席甲 CX-U05-001（本席 B1 未提） | 冲突/是/是 | **PASS（冲突/是/是；应发≠固定工资）** |
| U05-003 | GR-U05-002 + 席甲 CX-U05-002（勘误改分类） | 冲突（同根 U01-007/U02-002）/是/是 | **PASS（冲突/是/是）** |
| U05-004 | GR-U05-006 + 席甲 OBS-3（B1 低估升队） | 冲突/是/是 | **PASS（冲突/是/是；现有列先取全最懒）** |
| U05-005 | GR-U05-007 | 冲突/是/是 | **PASS（冲突/是/是；上下限不发 2.2）** |
| U05-006 | GR-U05-003 + 席甲 CX-U05-003 | 技术性/是/是 | **PASS（技术性/是/是）** |
| U05-007 | GR-U05-004 | 技术性/是/是 | **PASS（技术性/是/是）** |
| U05-008 | GR-U05-005 + 席甲 §4 | 技术性（发布分叉）/是/是 | **PASS（技术性/是/是；与本席 §6 一致）** |
| U05-009 | GR-U05-008 | 技术性/是/是 | **PASS（技术性/是/是）** |
| U05-010 | GR-U05-009 | 技术性/是/是 | **PASS（技术性/是/是）** |
| U05-011 | GR-U05-010 + 席甲预注 | 技术性/是/是 | **PASS（技术性/是/是）** |
| U05-012 | 双方观察归并 | 登记/否/否 | **PASS（登记/否/否；未写错 §8）** |

添加入队 0。冲突入队 5（001–005）。技术性入队 6（006–011）。登记观察 1（012）。questions 0。无 HTML 修改申请。

发布前须修「是」=001–011（11）；「否」=012（1）。与候选汇总一致。

### 重点 (a) U05-002 事假扣薪基数 — **PASS（成立，应发不可读作固定工资，须修是）**

本席 B1 未列此项。按 receiving-code-review 本轮独立核 HTML 原句与 SQL，不沿用席甲票。

HTML 原句（main 正本，本轮实读）：

- BSM L1450：「只有事假（无薪假）扣薪，按该员工当期请假工时占当期总工时的比例，**从其当期应发薪资中扣减**；扣减后的当期薪资以零为下限」。
- BSM L1458 / BEQR L283：请假扣薪不计入 20% 合计上限，「按事假工时比例单独扣减，其自身以当期应发薪资为零作为下限」；同条把 **总薪资** 定义为「基础工资、各项补贴、绩效工资与奖金之和」。
- BSM L1457：薪资单「**固定工资**、提成、激励奖金、处罚扣款、请假扣薪，各算各的分开列」。
- BSM L1437：「业绩仅为统计与考核口径，本身不产生任何**应发的钱**；产生钱的有销售提成、服务员提成、服务费、小费四条」。

SQL（副本 `JdbcStaffRepository.java:210-212`）：`ROUND(COALESCE(b.monthly_salary_cent,0)::numeric * Σleave_seconds / Σshift_seconds)`，只取固定工资列。工时比例与 `GREATEST(0, …)` 零下限两点与 HTML 同向。

第 11 段（文件止于 L676，席甲 B1 写 `:698-716` 为行号误，候选已改为 `:637-675`，属实）：

- `personalLeaveUsesScheduledHoursAndTotalCannotBecomeNegative`（`:637-655`）：底薪 10000、两班各 9h、事假一天 → 断言扣 5000。
- `ordinaryPenaltyCapAggregatesEntriesAndExcludesCommissionAndPersonalLeave`（`:657-675`）：底薪 10000 + 奖金 1000 + 提成 9000 + 处罚封顶 2200 + 事假半天 → `total_cent=12800`，反推事假仍按 10000 的一半=5000，把「基数＝固定工资」锁进第二条。

**「应发薪资」可否读作固定工资？否，无 HTML 依据。** HTML 在相邻段落同时使用「固定工资」（L1457 栏名）与「应发薪资」（L1450 扣减对象），没有一句把二者等同。L1458 需要「总薪资」时写了定义，需要「应发」时没有改口成「固定工资」或「基础薪资」。本席不把沉默写成「应发=底薪」。

**冲突是否成立？成立。** 实现用 `monthly_salary_cent` 替换 HTML「应发薪资」，有提成/奖金的员工少扣、无底薪只靠提成的员工不扣，月结与 L1450 系统性偏差。本材料改写了月结 SQL（基线按日历天，本树改按排班工时——分母更贴近 HTML，分子仍只用底薪），不是 main 既有、diff 未触及。分类 **冲突**（GEN-017：先于细化；不是 HTML 下位细节，是扣减对象换词）。工程缺陷是。须修是。BSM-086 / GEN-002 足够；BSM-120 锚点是设备标识与柜机，不宜作本条主支撑（非阻断）。

**处置是否发明业务？否（收窄读）。** 候选「基数改为当期应发薪资合计（固定+提成+奖金等应发项，个税与处罚另算）」：

- 提成计入：L1437 把销售/服务提成列为「应发的钱」；L1457 把提成列为薪资单应发栏；HTML 用「应发薪资」而非已定义的「总薪资」作事假扣减对象——含提成不是新规则。
- 个税与处罚另算：二者是扣减项，不是应发。
- 「等」不得卷进小费/服务费：BEQR-186「小费归集与服务费归集……全额发放」，全额发放栏不入事假比例。
- 补贴/绩效：属 L1458 总薪资已列项，结构化薪资落地后并入应发，本条不要求本 PR 建新表。

工时比例与零下限保留。同批改第 11 段两处期望值（U11）。票 **PASS**。

### 重点 (b) U05-001 冲突归并与读路径 — **PASS（成立，不新开 2.2）**

GR-U05-001 原意：分流读独立 `selfOperated()`，可与六类合作方式分叉；四分类 **冲突**；处置改 `"self_operated".equals(merchant.cooperationForm())`，与 U04-001 已答删除同一处置，**不新开 2.2**。

席甲 B1 CX-004 标「添加消费方」。候选勘误改冲突并与 GR-001 合并。本席复核：六类闭集已含「自营」（BSM L701 / PGM L495）；读第二轴使 `lease+true` 走公司自助、`self_operated+false` 走商户代提交——与闭集冲突。U04-001 是类型层 **添加**（第二开关本身），本段是该开关的读路径 **冲突**；同根不同层，不改写本席原意。

`questions/U04-001.md`「## 操作员答复」首个非空行精确「删除」（本席实读）。2.2 已答删除 → 入发布前须修。分类保持冲突，不改成添加。处置改读 `cooperationForm=="self_operated"` 是删除第二轴后的读路径，不是新的投影产品句、不是第七类。去掉对 U08 布尔 / V408 列的依赖是同一发明的机械后果。票 **PASS**。

### 重点 (c) U05-003 / U05-004 升为冲突 — **PASS（成立；004 处置最懒且不发明）**

**003：** GR-U05-002 原分类即 **冲突**（PGM L579 / BSM L935「安装时生成并绑定该手机专属的设备标识」，`selfRegister` 无公钥/指纹）。席甲 CX-002 标技术性。候选升冲突并与 U01-007/U02-002 同根——与本席 B1 一致，不是把技术性硬抬。公开无认证符合「尚无账号」；冲突核心是不绑设备，不是 permitAll 本身。频控/来源校验是执行加固，HTML 沉默，不作为本条冲突必修核心（非阻断）。最小处置「请求增加设备公钥/指纹，不要求先有 staffProfileId，与 U01-007/U02-002 同一 PR」= GR-002 原文。票 **PASS**。

**004：** GR-U05-006 原分类 **冲突**（封顶分母 `monthly_salary_cent+bonus`，注释称「总薪资」；HTML L1458 总薪资=基础工资+各项补贴+绩效工资+奖金；提成排除正确）。席甲 OBS-3 标结构性细化。升冲突成立：本材料新增封顶 LATERAL 却用注释把底薪+奖金称作总薪资，与已写明的定义冲突；缺列不等于可以把子集改名为定义。

处置「现有列先取全（固定+奖金），结构化薪资项落地后并入，注释不得把底薪+奖金称作总薪资，提成排除保持」相对本席 B1「无独立列则先落地结构化再封顶」更懒：不在本段发明补贴/绩效表，现有应入总薪资的列先取全，缺项待结构化薪资落地后并入。不发明业务。须修是（注释必改；公式须能并入后续列）。票 **PASS**。

### 重点 (d) U05-005 可见天数上下限 — **PASS（合规，不必为数值再问操作员）**

GR-U05-007：`StaffNumericSetting` 注释「上下限留空」；`numericSetting` 缺行 `orElse(new StaffNumericSetting(defaultValue,null,null))`（`:61-67`）；V402 只种子 `payroll_penalty_cap_bps` 2000/0/10000，无 `shift_visible_days` 行。HTML BSM L1444「出厂默认 7 天，须同时配置上下限」；BSM-001 L521「未填写上下限的参数不得上线」。表 CHECK 要求 `min_value`/`max_value` NOT NULL 且 `shift_visible_days` 的 `min_value>=1`。分类冲突成立。

本席 B1 原文即「HTML 未给默认上下限则先 2.2 **或** 按须同时配置由发布者填批准值」。候选取后一支：出厂行由发布者在迁移种子同时写入 7 与**批准的**上下限，或缺行 fail-closed。HTML 已要求「须配置」上下限，没有给出数字；2.2 只问不在 HTML 里的产品规则，不把上线配置值当成新规则。处罚上限 V402 的 0–10000 同样未出现在 HTML，先例是种子配置而非弹窗。不新开 2.2。禁止把某一组数字写进审计当作 HTML 出厂值。票 **PASS**。

### 重点 (e) U05-008 发布分叉 — **PASS（与本席 §6 一致）**

本轮复算三份既有文件 HEAD blob = 基线 blob，未发布树 blob 不同；四新文件 main 不存在。分叉不在这三份文本，而在调用面/迁移：`StaffRegistrationService` 调 main 无的 `selfOperated()`；`StaffController` 调 main `StaffService` 无的 `setEmploymentStatus`/`getPenaltyCap`/`requestPenaltyCap`/`getShiftVisibility`/`setShiftVisibility`/`shiftHistory`（HEAD 0 命中）；U07 才 permitAll 两路注册；副本 V400–V409，main 已有 `V410__m010_business_entity_topology_and_role_isolation.sql`。

GR-U05-005 最小处置：以当前 main 为底重放；U05+U06+U07 同一 PR；自营读路径按 001 改六类码、不带 U08 布尔插拔；迁移重编号 **>410**；禁止整树覆盖。候选「≥V411」与 `>410` 等价。与 §6 一致，未写成「三份文本已与 main 分叉」。票 **PASS**。

### 重点 (f) U05-012 观察 — **PASS（未写错本席 §8；92/92 可作执行证据、不能消解缺陷）**

012 以「Grok §8 1–20」收本席观察，点名：1 三同名文件 main＝基线；4 `status=active` 写在未批准申请；12 本席未跑 DB 测试、席甲一次性空库 92/92 补证；15/16/17/18 与 HTML 同向；19 `StaffScopeTests` stub 归 U11。席甲 OBS-1/2/4/5/6/7/8/9 并入，不升缺陷。未把 001–011 降成观察。定稿不必把 §8 逐条抄全（同 U04-003 口径）。

本轮实读 `/tmp/u11_tests.log`：`u11_scratch`（非 `complex_test*`）Flyway V001–V409，7 类 `2+43+7+4+29+4+3=92`，MVN_EXIT=0，随后 DROP。ledger 环境决定与日志一致。该证据可证明「第 11 段用例在空库+V409 上按**自身断言**可执行并通过」，**不能**证明 HTML 合规——`:637-675` 正把错误基数锁绿。012 未用 92/92 消解 002/004，写法成立。

OBS-19：`StaffScopeTests.java:74` 仍 `when(repo.upsertShift(any()))`，同时 `:39-41` 注入 `StaffApprovalService` mock；测试文件不在本段，归 U11 观察属实。票 **PASS**。

### 006 / 007 / 009 / 010 / 011 独立裁定

- **006** 柜机同一请求既要 `staff.profile.self` 又要已验签 kiosk（`StaffLockerService.java:40-49`）；副本无单端调用方。技术性，无业务发明。处置与 GR-003 一致。**PASS**。
- **007** `shiftRevisions` 恒 `FOR UPDATE`（`:602-610`），`history()` GET（`StaffApprovalService.java:88-90` / `StaffController.java:171`）走它。技术性。**PASS**。
- **009** `companyChain(merchant).getFirst().id()`（`:66` / `:122-137` 自下而上）。技术性；处置 `anyMatch` 在 001 改读六类码之后。**PASS**。
- **010** `COALESCE(sp.company_id,sp.project_id)` 五处（`:231/:328/:354/:382/:409`）；U06 `:204/:288` `companyId()==null` 放行；兼容构造 `:44-64` 默认 `companyId=null`。铁律 8。技术性。**PASS**。
- **011** `POST …/employment-status`（`:95-98`）、`POST …/settings/shift-visible-days`（`:203-206`）无 `requireIdempotencyKey`；同文件 22 次调用有键（候选「23 处」为计数口径，非阻断）。技术性。**PASS**。

### 分类 / 队列 / 给发布者的最小边界

本批可定稿（须席甲本批亦 PASS）。发布前须修是=001–011。否=012。添加 0，不进 BLOCKED-ASK，不新开 questions。

发布者最小集（不在本轮改源）：

- 001：两处分流改读 `"self_operated".equals(merchant.cooperationForm())`；去掉对 U08 布尔 / V408 列的依赖；引用 `questions/U04-001.md`，不再问。
- 002：事假扣薪基数改为当期应发合计（月结已列应发栏：固定工资+提成+激励奖金；补贴/绩效落地后并入）；个税与处罚不入基数；小费/服务费全额发放不入；工时比例与零下限保留；同批改 U11 两处期望值。不得把应发读成仅固定工资。
- 003：自助注册增加私人手机设备公钥/指纹，落档或批准时绑定，不要求先有 staffProfileId；与 U01-007/U02-002 同一 PR。
- 004：封顶分母按 HTML 总薪资——现有列先取全（固定+奖金），结构化项落地后并入；注释不得称底薪+奖金为总薪资；提成排除保持。
- 005：`shift_visible_days` 缺行不得返回空上下限；迁移种子同时写 7 与批准的上下限，或缺行 fail-closed。上下限数值属上线配置，不发 2.2，审计不指定数字。
- 006：柜机以已登记 kiosk 设备签名上报，员工身份由扫码短时凭证承载；补单端可失败用例。
- 007：`history()` 去掉 `FOR UPDATE`。
- 008：以 main `445a49f4` 为底重放；U05+U06+U07 同一 PR；不带 U08 自营插拔；迁移重编号 ≥V411（U10）；禁止整树覆盖。
- 009：公司校验改为链上 `anyMatch`（在 001 之后）。
- 010：去掉 COALESCE 与 null 放行；写入强制 `company_id`；兼容构造不为新字段填空。
- 011：两处新写口与其它员工写口同样校验幂等键并按操作命名空间去重。
- 012：登记，不修。

禁止：整树覆盖 main；为 005 把某一组上下限写成 HTML 出厂；为 002 把小费/服务费卷入事假基数；为 004 本 PR 发明补贴/绩效产品表；就自营布尔或可见天数上下限数字再开 2.2。

非阻断勘误（不定稿作废，不必 r2）：

1. 002 冻结判据以 BSM-086/GEN-002 为主；BSM-120 不宜作本条主支撑。
2. 003 「无认证」本身符合尚无账号；频控不是冲突必修核心。
3. 004 「现有列先取全」不要求本 PR 新建结构化薪资表。
4. 005 种子上下限由发布者批准，不得写成 HTML 出厂闭集。
5. 011 「23 处写口」本席实算 `requireIdempotencyKey` 22 次调用；archive/unarchive/sales-position/delete-* 无键属本席 B1 未单列，不另开 ID。
6. 012 的 92/92 只证明用例自身绿，不能消解 002/004 把错误口径锁死。
7. 席甲 B1 CX-001 行号 `:698-716` 已由候选改为 `:637-675`。

### 结论

全批差集无漏并。002 独立裁定成立：应发薪资不可读作固定工资，冲突/须修是，含提成有 L1437/L1457 依据、不发明业务。001 冲突归并与已答删除读路径成立。003/004 升冲突成立，004 现有列先取全最懒。005 上下限不发 2.2。008 与本席 §6 一致。012 未写错 §8，空库 92/92 可作执行证据、不能消解缺陷。逐 ID 001–012 全 **PASS**。本席 B2-r1 **PASS**。未经双 PASS 不得把候选当定稿，更不称 U05 或 main 模块 PASS。步骤仍 B2；U05 IN_PROGRESS；材料未发布，不做 B3。
