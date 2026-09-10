CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U08 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 只读全量三线（未发布材料第 8 段；不做 B3/B4/B5；处置写给发布者，本轮不修源）
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 段：U08 — 未发布材料第 8 段「组织与商户联动」
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 末尾附录；`plan/runbook.md` V0.02 §11；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 审计对象（不得冒称 main 或已发布修复）：
  - 未发布提交：`03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb`（本仓 `git cat-file -t` **无此 commit 对象**）
  - 未发布树：`e28dad059b2c7e08bcc95a8db18552d3f76edf4b`（`git cat-file -t` = **tree**，可解析）
  - 发布基线：`242f487233d028173cb9198eecd079f5d877405c`（commit，可解析）
  - 只读输入根：`/home/james/complex-management/.scratch/module-recheck-20260908/unpublished-03acdb98`
  - 清单：`.scratch/module-recheck-20260908/unpublished-03acdb98-input.json` V0.01，76 文件
- 本段：5 文件，物理行 2191，标记 NORMAL；main 主归属 M009（4 个既有文件）+ 1 个未发布新增 DTO（main 无此文件）
- 冻结判据：M009 映射行并集 87 项（python 集合相对开工提示 MISSING=[] EXTRA=[]）
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 且发布前须修：GR-U08-001 … GR-U08-005
- 添加项（B1 只登记，不代用户回答 2.2）：GR-U08-001＝已定稿 U04-001，操作员已答「删除」，**不再弹窗**
- 同根已定稿、不新开 2.2：GR-U08-001＝U04-001/U05-001；GR-U08-002＝U05-008 同形态（以 main 为底重放，禁止整树覆盖）；GR-U08-003＝U04-002/U03-007
- 只登记、建议队列「否」：观察见 §8
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做写操作
- 本报告不表示全仓、main 模块或发布门禁通过。M001–M012 检查点已合入当前 HEAD（PR #428/#429）。原 322/471 模块结论不继承到本未发布段。不得把本副本冒称 main 源码或已发布修复。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`（SHA256 `fa5c1e5ee76b1c8f1ae56101f52c9e239de75d5c578adc61227b92d10b7e52ef`） | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md`（SHA256 `b9339b09ee3980808d8c5a35c7251b891b8b1e0036ec4ca37812b976ebddf6b6`） | `/tmp/u08-b1-grok` 把本段 5 文件盖到 main 后端源码树的离线 `mvn -o -DskipTests compile`；不改生产代码 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md`（SHA256 `808fc5717aa88ad65efff312b11c186294d3e6ee301afb584e2f86599b137787`） | 只借读错误→复现→追数据流（本段 5 文件 → U03 页/U04 types/U05 StaffRegistrationService/U10 V408/U11 回归测试 → main 对照 → SQL/HTML） |
| verification-before-completion | superpowers 同名（SHA256 `2befe7fc55bcadaa3d97dd9e8efeb633d2561c0ebe74c5a8b17c4d9e7e4520b3`） | 先跑命令再声称；未跑的共享库集成测试不写通过 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md`（SHA256 `40df33b58fc6ef889b93585733feb9566b76e9586efa7f376785c1e995197ac0`） | 只列可删复杂度（独立布尔、双写审计、19 参构造），不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md`（SHA256 `a842323e664e5af104eac5c97ad22fda929ebeb62d81c501161ac1f6f482db58`） | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md`（SHA256 `6f9ae7c1c295d7e67eda1e3a616c337c057f45f1a5bf2955efb07d539a655b79`） | 本段验证均秒级，未起 monitor |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：套件文档写明已并入 `grilling`，无独立技能可装
- `ubiquitous-language`：本机技能树未找到
- Arc `audit` 会派评审子代理：与双席/§7 冲突，**不调用**
- `using-superpowers` 要求技能优先于探索：与「禁止子代理 / 本提示指定 controller」冲突处，按用户指令收窄

明确不用：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、MCP、第三主体。

内部 grilling 树（自答，未问操作员）：

1. 独立布尔必须存在吗？HTML BSM L701 / PGM L495 六类合作方式已含「自营」；BSM L1433 / PGM L578「标注为自营」是同一轴的标注，不是第二开关。本段把 `is_self_operated` 与 `cooperation_form` 并列且互不校验。操作员已对 U04-001 答「删除」。不必再问。
2. 整文件覆盖当前 main 会不会发生？会。四个同名文件 main 自基线已被 M009 改过（PlatformService +198/−61）。本段相对基线只 +61/−15，几乎全是自营插拔。覆盖即回退 M009。
3. 只把 5 文件合到当前 main，首轮会不会编不过？会。未发布 `PlatformService:122` 与 `JdbcPlatformRepository:497/:582` 仍读/写 `parentEntityId`，main `BusinessEntityRecord`/`BusinessEntityRequest` 已无该字段（M009-012）。`/tmp/u08-b1-grok` 离线编译 3 error。
4. PUT 更新档案会不会抹掉存档？会。未发布 `saveMerchant` 仍走 19 参构造（archive/delete 置 null/none），再 `.withSelfOperated(已有布尔)`。M009-001 已在 main 修好。首轮正常改档就会撞上。
5. 双写审计是不是要两三件不巧才发生？不是。每次成功 POST self-operated 都会先在闭包里 `recordSuccess`，`executeWrite` 成功后再记一条同名事件。首轮必发。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`，属正常）
- `criteria-lock.md` 版本 V0.11；判据总数 401
- 映射行：`M009 →` L6388；python 并集 87，相对开工提示 MISSING=[] EXTRA=[]

HEAD（独立实算）：

- `git rev-parse HEAD` = `git rev-parse origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`
- `git log -1 --oneline`：`445a49f4 Merge pull request #429 from jxh1979/audit/20260909/M011-M012-checkpoint`
- 本席对照 main 已修项时以 **当前 HEAD=445a49f4** 为准（含 M001–M012）
- 本席未设 `GIT_INDEX_FILE`，未动主索引

ledger 只读核验（不改）：V2.64，486596 bytes，SHA256 `cc06df57c0de52327174ec2f616f148850b04bc86d61a510876cc2c176a57a42`（与开工提示一致）。findings V1.57，604820 bytes，SHA256 `6836e3a8c34cbb53e12c76a9e9e871df83168261607ac7b5f9dc0f2b19bb0f3f`（与开工提示一致）。台账中与本段直接相关的已定稿项（背景，不继承 PASS）：

- U04-001：独立自营布尔＝添加，操作员答「删除」；明确点名 U08 端点/DTO/仓储/`MerchantRecord.selfOperated`/V408 列随之删除
- U04-002：类型/页面/路由/列四件须同一 PR；随 001 删除消失
- U05-001：`StaffRegistrationService` 改读 `cooperationForm=="self_operated"`，去掉对 U08 布尔的依赖
- U05-008：以 main 为底重放，不带 U08 插拔
- U03-001/006/007：自营动作/幂等键/testid；U04 定稿后收敛为去掉该动作
- M009-001…036：当前 main 已修；本段四个同名文件仍是基线+布尔，覆盖会回退

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

三 HTML 只读、未改。锁内本段用到的锚点行对本席读取的现行 HTML **全部命中**（87/87，见 §1.1）。三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。

清单本段 5 条与独立 `sha256sum`/`wc`/`git cat-file` **5/5 match**。副本 SHA 与树 `e28dad05:<path>` blob **5/5 相同**。全 76 文件相对基线 numstat 合计 **+4745 / −1084**（独立 `git diff --shortstat` 实算，与开工提示一致）。

对象核验：`git cat-file -t 03acdb98…` fatal（无 commit）；`e28dad05…` = tree；`242f4872…` = commit。

本段相对发布基线逐文件 numstat（`git diff --numstat 242f4872 e28dad05 -- <path>`）：

| 路径 | + | − |
|---|---:|---:|
| `JdbcPlatformRepository.java` | 13 | 10 |
| `MerchantRecord.java` | 15 | 4 |
| `PlatformController.java` | 10 | 0 |
| `PlatformService.java` | 19 | 1 |
| `dto/MerchantSelfOperatedRequest.java`（新文件） | 4 | 0 |
| **合计** | **61** | **15** |

`git diff --stat` 汇总：`5 files changed, 61 insertions(+), 15 deletions(-)`。

### 1.1 HTML 锚点实核（锁出处行 → 现行正本）

锁定义区 87 条全部读过。GEN 无 HTML 行。其余 69 条锁出处行在现行三 HTML 上均存在，标题/词条与锁摘录一致。抽样（本段直接用到的原文）：

| 判据 | 锁出处 | 现行该行含 |
|---|---|---|
| BEQR-148 | beqr:231 | `合作模式` 自营、租赁、联营、代运营、服务商接入、其他 |
| BEQR-154 | beqr:237 | `综合体项目状态` 草稿、待启用、已启用、暂停、终止、归档 |
| BEQR-155 | beqr:238 | `商户/运营主体状态` 待准入、正常、暂停、终止、归档、黑名单 |
| BEQR-143 | beqr:226 | `四类主体` 经营对象、运营主体、合同主体、结算主体 |
| BSM-004 | bsm:566 | `配置数据可以存档、也可以删除（删除要审批）`；删除只隐藏不抹除 |
| BSM-028 | bsm:698 | `主要功能`；L701 建立商户档案，区分六类合作方式 |
| BSM-072 | bsm:1308 | 商户在授权范围内完整使用自己的系统 |
| PGM-018 | pgm:415 | 物理项目与运营公司：多对多协作，不是固定隶属层级 |
| PGM-026 | pgm:492 | `主要功能`；L495 区分六类合作方式；L498 四类主体绑成一组 |
| PGM-027 | pgm:507 | 商户不能查看平台全局和其他商户数据 |
| PGM-008 | pgm:362 | `员工和手机`（L578「标注为自营」属其子节，本段映射含 PGM-008 不含 PGM-033） |

BSM L1433 / PGM L578「在商户档案上标注为自营的商户视同公司自有」为 U04-001 已引用原文，本席实读现行 HTML 仍在；本段映射未含 BSM-085/PGM-033，001 的冻结支撑用已映射的 BEQR-148/BSM-028/PGM-026/GEN-017，不把未映射 ID 当本段判据。

## 2. 逐文件覆盖与第一性原理

五文件均全文读完（Jdbc 1103 行按结构三段覆盖，不是只看 diff）。副本字节/行/SHA 与开工提示及清单 **5/5 一致**：

| 输入相对路径 | 行 | 字节 | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| `.../JdbcPlatformRepository.java` | 1103 | 61753 | `b2552172af0cec506ee1c753e12ea839abbf645d3589c78e5c412c66bf880fec` | 1–400 商户/分页；401–800 参数/映射/主管复核写；801–1103 审批链/节假日/readObjectMap/archive_reason |
| `.../MerchantRecord.java` | 90 | 6233 | `750a0227662feeb14ad686c6c3c96270644acc559ef5a2c0dc044e9d03953ef8` | 全文 |
| `.../PlatformController.java` | 237 | 13611 | `41d1beec574dd21f873f780e9b7bd515bc829563c09f7a83603a8bfd05a9598d` | 全文 |
| `.../PlatformService.java` | 757 | 35843 | `e079db05d677f269585b5f0a6c95aa807f86b9ca71f0bb11728e43d4023ab456` | 全文 |
| `.../dto/MerchantSelfOperatedRequest.java` | 4 | 175 | `84a9f41426492cf83d22c7a08d50f18c92bc72b46cef43fe276e504fb6ea7d91` | 全文 |

### 2.1 `MerchantSelfOperatedRequest.java`（未发布新增；main 无此文件）

全文：`public record MerchantSelfOperatedRequest(String idempotencyKey, Boolean selfOperated, String reason) {}`。无 Bean Validation。服务层要求 `selfOperated != null` 且 `reason` 非空。

- 为什么必须存在：承载「独立于合作方式的自营开关」的写入口。删掉它，HTML 六类合作方式仍能表达「自营」。
- 声称的行为：作为 POST body。本段无测试执行该 DTO。
- 未验证前提：HTML 允许第二轴。已否（U04-001 删除）。

### 2.2 `MerchantRecord.java`

相对基线：canonical 记录末位加 `boolean selfOperated`；补 25 参委托构造（默认 false）与 `withSelfOperated`；`withArchive`/`withMaskedPhones`/`withDelete` 透传该字段。注释仍写「删除=硬删走审批」。

- 为什么必须存在：商户档案是 PGM-026/BSM-028 的承载。`selfOperated` 字段本身不是。
- main 对照：main V0.02 无 `selfOperated`，有 `withEntities`（M009-008），存档注释已改软删除（M009-026）。覆盖会丢掉 `withEntities`，main `PlatformService.syncMerchantEntities` 将无法编译。
- PII：`withMaskedPhones` 仍掩第一/第二联系人手机，透传 `selfOperated`。写库加密在 Jdbc。本文件无明文密钥。

### 2.3 `PlatformController.java`

相对基线：在 archive 端点前插入 `POST /merchants/{merchant_id}/self-operated`，权限 `merchant.profile.manage`，`requireIdempotencyKey`，返回前 `maskPhonesForUnauthorized`。无 `@Valid`。GET 列表仍只 `requirePermission`，**无** `requirePlatformScope`（main 已在 L46/65/129/145/152/175/193 补上，M009-015）。未发布本文件仅场地营业时间 POST 有 `requirePlatformScope`（L185）。

- 未使用 import：`ApiException`（与 M009-034 同形态，基线既有）。
- 自营 POST 若与 U03 页一起发布：页 L325 不传 `idempotencyKey`，本控制器会拒（U03-001）。后端要求本身正确。

### 2.4 `PlatformService.java`

相对基线净增：`setMerchantSelfOperated`（L177–191）+ `saveMerchant` 末尾 `.withSelfOperated(findMerchantById…orElse(false))`。其余仍是基线：19 参 `saveMerchant`、无合作方式/状态/角色白名单、存档酒吧调用 `ticketingService.expireBarSingleStoreTickets`、删除 javadoc 写「物理删除」、`approveDelete` 无 `isMasterAuthority`、`page()` 固定 50、`requireDate` 死代码（英文 `is required`）、`createBusinessEntity` 写 `request.parentEntityId()`。

`setMerchantSelfOperated` 数据流：`requirePlatformScope` → 校验布尔/事由 → `executeWrite(operationId=merchantSelfOperated:{id})` → `findMerchantById`（含已删行）→ `saveMerchant(withSelfOperated)` → **再** `auditEventService.recordSuccess(...)`。`executeWrite` 成功路径本身会再 `recordSuccess`（`AuditedOperationExecutor:130-138`，metadata 为本方法传入的空 `Map.of()`）。同事件名两条。

不读 `cooperationForm`，不拦已存档/已批准删除。

### 2.5 `JdbcPlatformRepository.java`

相对基线：所有 merchants 的 SELECT/INSERT/UPSERT/`merchantParameters`/`mapMerchant` 增加 `is_self_operated`。`getBoolean("is_self_operated")`：列缺失则 SQLException（无 V408 必炸）；NULL 则 false。

未改、但本文件仍承载的基线行为（覆盖 main 会回退）：

- `deleteMerchant`：`UPDATE … SET delete_status='approved'`（软删，行为对；注释 L179 仍「>0 拒硬删」）
- `listMerchants(boolean)` / `listMerchantsPaged`：`delete_status <> 'approved'` **即使** `include_archived=true`（main M009-010 已改为 include 时含已删行）
- 无 `businessEntityExists` / `projectExists` 主键覆写、无 `findSpaceById` 主键查询（main M009-006/007/027）
- `mapBusinessEntity` 仍 10 参含 `parent_entity_id`（main 记录已删该字段 → 覆盖编译失败）
- `readObjectMap` + `OBJECT_MAP_TYPE` 无调用（main M009-016 已删）
- `import java.math.BigDecimal` 无类型使用（M009-034）
- 主管复核/审批链/节假日 SQL：本段 diff 未触及；全文已读，未发现本段新增根因

PII：`merchantParameters` 对两个 phone 走 `piiCipher.encrypt`，`mapMerchant` decrypt。自营列非 PII。

## 3. 顺从式：87 项冻结判据

每个 ID 都对照本段 5 文件（及只读调用链）。无实现面的写明原因。分类顺序：冲突 → 细化 → 技术性 → 添加。

| ID | 本段结论 | 证据 |
|---|---|---|
| GEN-001 | **FAIL** 独立布尔无必要；死代码 requireDate/readObjectMap | GR-U08-001；002 含 M009-016 |
| GEN-002 | **FAIL** 覆盖 main 编译 3 error；PUT 抹存档；无 V408 则 SQL 炸 | GR-U08-002/003 |
| GEN-003 | **FAIL** 本段 5 文件 0 测试覆盖 `setMerchantSelfOperated` HTTP/服务 | GR-U08-005 |
| GEN-004 | **FAIL** 合作方式/状态任意串（回退 M009-003/004）；GET 无平台范围（回退 015） | 002 |
| GEN-005 | **FAIL** 读端缺 `requirePlatformScope`；删除审批无最高权限闸（回退 009/015） | 002 |
| GEN-006 | **FAIL** 更新走 19 参构造抹 archive/delete；绑定不校验主体存在 | 002＝M009-001/006 |
| GEN-007 | 自营写走幂等键，本段无对象级锁；与既有商户更新同形态 | 观察；不新开 |
| GEN-008 | **FAIL** 布尔与六类码双轴；DTO/端点/列/页/OpenAPI 分四段；page() 50 vs 全量 | 001/003/002 |
| GEN-009 | PUT 不存在 id 仍 upsert（回退 M009-002）；非法状态落库 | 002 |
| GEN-010 | 联系人手机仍加密+读端脱敏；自营列非 PII。GET 范围属 005 | PII 路径通过；范围见 002 |
| GEN-011 | **FAIL** 自营成功路径双写同名审计事件 | GR-U08-004 |
| GEN-012 | `countMerchantReferences` 逐表 COUNT、`listSpaces` LIMIT 500 无主键覆写 | 002＝M009-023/027；观察 |
| GEN-013 | **FAIL** 整文件覆盖与 main 分叉；依赖未发布 V408 | 002/003 |
| GEN-014 | 本段无迁移文件；写 `is_self_operated` 依赖 U10 V408（main 最大已发布迁移 ≥V410） | 003；迁移本体归 U10 |
| GEN-015 | 本段无 UI；`requireDate` 英文死代码 | 002；UI 归 U03 |
| GEN-016 | **FAIL** 「物理删除/硬删」注释与 UPDATE 软删不一致 | 002＝M009-010/026 |
| GEN-017 | **FAIL** 独立布尔按顺序落到「添加」 | GR-U08-001 |
| GEN-018 | 本报告落到 HTML 原文、编译输出、diff | 本文件 |
| BEQR-063 | 无实现面：本段无反结/沽清/免单 | 原因：非商户档案写口 |
| BEQR-074 | 无实现面：无桌台 | — |
| BEQR-143 | 绑定仍四 ID；不校验存在（回退 006） | 002 |
| BEQR-144 | 未发布仍写 parent 单链（回退 012，且覆盖 main 编译失败） | 002 |
| BEQR-145 | 无实现面：城市项目公司职责文本不在这 5 文件落地 | — |
| BEQR-146 | 无实现面 | — |
| BEQR-147 | 无实现面：收入归属 | — |
| BEQR-148 | **FAIL** 六类无白名单；另加布尔第二轴 | 001/002＝M009-003 |
| BEQR-149 | 无实现面：职级 | — |
| BEQR-150 | 无实现面：终端清单 | — |
| BEQR-151 | 无实现面：权限计算公式 | — |
| BEQR-152 | 读端脱敏存在；跨项目隔离靠 scope（回退 015） | 002 |
| BEQR-153 | 无实现面：终端开关 | — |
| BEQR-154 | **FAIL** 项目 status 无白名单（回退 004） | 002 |
| BEQR-155 | **FAIL** 商户 status 无白名单；存档即停票（回退 004/011） | 002 |
| BEQR-156 | 无实现面：合同状态 | — |
| BEQR-157 | 无实现面：员工状态（分流在 U05） | 只读调用链见 §5 |
| BEQR-158 | 无实现面：设备 | — |
| BEQR-159 | 无实现面：权限状态 | — |
| BEQR-166 | 本文件含主管复核 SQL，本段 diff 未改；审批链引擎非本段新增 | 无新增根因 |
| BEQR-189 | 无实现面：管理费 | — |
| BSM-001 | 商户档案能力存在；自营第二轴添加 | 001 |
| BSM-003 | 存档/删除入口存在；口径回退 | 002 |
| BSM-004 | **FAIL** PUT 抹存档；注释硬删；includeArchived 仍排除已批准删除；删除无最高权限闸 | 002 |
| BSM-005 | 无实现面：分步引导在页面（U03/M012） | — |
| BSM-006 | 无实现面：动态二维码 | — |
| BSM-007 | 删除审批未限制最高权限（回退 009） | 002 |
| BSM-009 | 基础管理入口存在 | 通过（档案 CRUD） |
| BSM-028 | **FAIL** 六类无白名单 + 第二轴 | 001/002 |
| BSM-029 | 财务要知道合作方式：双轴会分叉 | 001 |
| BSM-072 | 商户不能看平台全局：GET 无平台范围 | 002＝M009-015 |
| BSM-074 | 同上 | 002 |
| BSM-114 | 入口隔离：GET 范围 | 002 |
| BSM-115 | 基础管理连接业务：本段提供档案 | 通过（必要性） |
| BSM-120 | 无本段新增拍板项；不发明 | 无实现面新增 |
| PGM-001 | 治理底座含商户档案 | 通过 |
| PGM-002 | 无本段新增卡点 | 无实现面 |
| PGM-003 | 模块入口索引 | 无实现面 |
| PGM-004 | 综合体/集团索引 | 无实现面 |
| PGM-005 | 空间创建存在；status 无 V203 白名单（回退 036） | 002 |
| PGM-006 | 商户和主体：本段主场 | 001/002 |
| PGM-007 | 合同准入不在这 5 文件 | 无实现面 |
| PGM-008 | 员工路径依赖自营标注；本段做成独立布尔 | 001；读路径 U05-001 |
| PGM-009 | 权限码 `merchant.profile.*` 存在 | 通过（码级） |
| PGM-010 | 无实现面：终端开关 | — |
| PGM-011 | 无实现面：设备 | — |
| PGM-012 | 商户侧边界 | 无实现面 |
| PGM-013 | 无实现面：开放接口 | OpenAPI 在 U14/U15 |
| PGM-014 | 自营写有留痕；双写污染 | 004 |
| PGM-015 | 无实现面：楼宇 | — |
| PGM-016 | 无实现面：应急隐私（手机脱敏属 010） | PII 通过 |
| PGM-017 | 项目记录字段不在本 5 文件 | 无实现面＝M009-013 |
| PGM-018 | parent 单链回退 | 002＝M009-012 |
| PGM-019 | 主体角色无白名单 | 002＝M009-005 |
| PGM-020 | 项目主要功能不全在本段 | 无实现面 |
| PGM-021 | 业务意义文本 | 无实现面 |
| PGM-026 | **FAIL** 六类 + 四类主体投影缺失 | 001/002＝M009-003/008 |
| PGM-027 | GET 无平台范围 | 002 |
| PGM-031 | 无实现面：经销商 | — |
| PGM-035 | 无实现面：提成发放 | — |
| PGM-040 | 功能级权限码存在 | 通过 |
| PGM-041 | 读端缺平台范围 | 002 |
| PGM-042 | 无实现面：职级规则 | — |
| PGM-043 | 无实现面：外包审计人员 | — |
| PGM-044 | 无实现面：桌台挂单 | — |
| PGM-048 | 无实现面：商户侧全开放 | — |
| PGM-060 | 无实现面：会员生物信息 | — |
| PGM-065 | 连接关系：档案被员工注册读取 | §5 |
| PGM-066 | 无本段待排期发明 | 不 LOCK-AMEND |

无实现面的判据：本段 5 文件没有对应可执行入口或字段，不是「HTML 沉默所以冲突」，也不是删除建议。

## 4. 对抗式

假定实现错误，构造最小反例（只读/隔离，未写库）：

1. **`cooperationForm=lease` 且 `selfOperated=true`**：`setMerchantSelfOperated` 不读合作方式；`saveMerchant` 也不把布尔与六类对齐。U04 类型探针已证双轴可分叉。本席在副本源码复核：`PlatformService.java` 自营方法 0 处 `cooperationForm`。首轮点「标记为自营」即发生。
2. **整文件覆盖 main**：`/tmp/u08-b1-grok` 将 5 文件盖到 main 后端，`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 mvn -o -DskipTests compile` → **3 errors**，全是 `parentEntityId`（Jdbc:497/:582，Service:122）。exit 1。
3. **PUT 抹存档**：未发布 `saveMerchant` 对更新与创建共用 19 参构造（`MerchantRecord.java:48-58` 默认 `archivedAt=null, deleteStatus=none`），再 `withSelfOperated`。与 M009-001 同一构造。首轮改档发生。
4. **无 V408 跑 JDBC**：`mapMerchant` `getBoolean("is_self_operated")` 列不存在则 SQLException。main 无该列。只合 U08 不合 U10，首轮读商户列表即炸。
5. **双审计**：每次成功 POST。不是极端。
6. **前端无幂等键**：U03 L325 POST body 无 `idempotencyKey`；本控制器 `requireIdempotencyKey` 必拒。功能在完整 76 文件同批时仍不可用（U03-001）。本段后端要求正确，记观察。
7. **已删除商户仍可打标**：`findMerchantById` 无 `delete_status` 过滤。需已知 id，UI 默认不列出。按铁律 6 收窄为观察（要「知道已删 id + 直接打 API」两件）。
8. **并发双 POST 不同幂等键**：后写覆盖，无版本列。与既有 `saveMerchant` 同窗，不新开闸。

未把理论极端升 finding。

## 5. 跨段数据流（副本 → 其它未发布段 → main → SQL → HTML）

```
U03 OrganizationMerchantPage.tsx:320-326
  POST /api/v1/platform/merchants/{id}/self-operated {selfOperated, reason}  // 无幂等键
    → U08 PlatformController:86-91 requirePermission+requireIdempotencyKey
      → U08 PlatformService.setMerchantSelfOperated
        → U08 JdbcPlatformRepository.saveMerchant (is_self_operated)
          → U10 V408 ALTER merchants ADD is_self_operated BOOLEAN NOT NULL DEFAULT FALSE
U04 types.ts:1121 selfOperated?: boolean  // 与 cooperationForm 并列
U05 StaffRegistrationService:66/91 merchant.selfOperated() 分流
U11 M010RegressionTests:321-330 走 repository.saveMerchant(withSelfOperated)，不走 HTTP
U14/U15 OpenAPI 声明该 POST
main 445a49f4：无 DTO、无端点、无列、无 selfOperated()；有 M009 白名单/软删/平台范围/withEntities
HTML：六类合作方式＝自营/租赁/…（BEQR-148/BSM-028/PGM-026）
```

操作员已答删除后，发布者应切断整条链：U08 端点/DTO/字段/SQL 插拔全部去掉；U05 改读 `cooperationForm`。

## 6. 与 main 分叉对照（对抗式固定问题）

四个既有文件 **main blob ≠ 基线 ≠ 未发布**：

| 文件 | 基线→未发布 | 基线→main HEAD | HEAD→未发布（覆盖方向） |
|---|---|---|---|
| JdbcPlatformRepository | +13/−10 | +34/−22 | +35/−44 |
| MerchantRecord | +15/−4 | +12/−1 | +16/−16 |
| PlatformController | +10/−0 | +9/−0 | +10/−9 |
| PlatformService | +19/−1 | +198/−61 | +80/−199 |
| MerchantSelfOperatedRequest | +4 新 | main 无 | +4 新 |

main 文件 SHA：Jdbc `10efae64…77139e2`（1112 行）、MerchantRecord `66a840d9…cb12ace2`（90 行）、Controller `05bd7cf4…d92b743`（236 行）、Service `720b13d9…2eb9aa94`（876 行）。

覆盖会回退的已定稿 M009 项（均落在本段 4 个同名文件内，不是「别的模块的事」）：

| ID | 未发布仍是 | main 已修 |
|---|---|---|
| M009-001/002 | 19 参构造抹 archive；更新可 upsert 新 id | 更新 requireMerchant，保留 archive/delete/createdAt |
| M009-003 | `trimToNull(cooperationForm)` 任意串 | 六类白名单 |
| M009-004 | status 任意串 | SUBJECT/PROJECT 白名单 |
| M009-005 | entity_role 只非空 | 三角色白名单 |
| M009-006/007 | 绑定/项目/空间不按主键存在性校验 | Jdbc 覆写 exists + Service 校验 |
| M009-008 | 无 `withEntities`、档案写三主体列 | 激活绑定投影，档案更新不覆盖 |
| M009-009 | 有权限码即可批删；最高权限不能自执行 | `isMasterAuthority` 闸 + 本人发起直软删 |
| M009-010/026 | includeArchived 仍排除 approved；注释「硬删/物理删除」 | 全量可查；注释软删 |
| M009-011 | 存档酒吧 `expireBarSingleStoreTickets` | 已删除该调用与五参构造 |
| M009-012 | 仍写 `parentEntityId` | 记录/DTO 已删字段 → **覆盖编译失败** |
| M009-014 | `page()` pageSize=50 全量 items | `PageResponse.of(..., LIST_MAX_PAGE_SIZE)` |
| M009-015 | GET 无 `requirePlatformScope` | 读端与写端同法 fail-closed |
| M009-016 | `requireDate`/`readObjectMap` 仍在 | main 已删 |
| M009-027 | 无主键 `findSpaceById` | Jdbc 覆写 |
| M009-028 | 已存档商户仍可绑新项目/空间 | `requireActiveMerchant` |
| M009-036 | 空间 status 任意串 | V203 二值白名单 |

隔离编译命令与输出见 §9 CMD-OVL。未跑共享库测试（禁写 `complex_test*`）。

## 7. Findings

### GR-U08-001 — 独立自营布尔（添加；同根 U04-001，已答删除）

- 文件:行号：`MerchantSelfOperatedRequest.java:4`；`MerchantRecord.java:36-43`；`PlatformController.java:86-91`；`PlatformService.java:177-191`、`:655`；`JdbcPlatformRepository.java:90-123`、`:532`、`:622`
- 完整原文（HTML）：「建立商户档案，区分自营、租赁、联营、代运营、服务商接入、其他这六类合作方式。」（BSM L701 / PGM L495）；「合作模式：自营、租赁、联营、代运营、服务商接入、其他。」（BEQR-148）
- 业务影响：出现第二套「是否自营」。租赁商户可被标自营，员工注册走公司自有路径（U05 `:66/:91`），合同/结算仍按租赁。维护两套开关。
- 可复现：源码 `setMerchantSelfOperated` 0 处读取 `cooperationForm`；`withSelfOperated` 不改 `cooperationForm`。U04 已用类型探针证明 `lease+true` 与 `self_operated+false` 均可。
- 冻结判据：BEQR-148、BSM-028、PGM-026、GEN-001、GEN-008、GEN-017
- 2.3 依据：类型 1 HTML 原文 + 类型 4 源码/既有探针
- 四分类：**添加**（HTML 无一句允许独立于六类的布尔或规定同步；按冲突→细化→技术性→添加落到添加）
- 工程缺陷：是
- 建议登记：是（正式 ID 待 B2；建议并入 U04-001，不再 2.2）
- 最小处置（写给发布者）：按 `questions/U04-001.md` 已答「删除」——删除本 DTO、控制器端点、服务方法、记录字段/25 参委托/`withSelfOperated`、Jdbc 一切 `is_self_operated` 插拔。员工分流改读 `cooperationForm=="self_operated"`（U05-001）。**不要**再问操作员。

### GR-U08-002 — 整文件覆盖回退 M009 已修项（技术性；同形态 U05-008）

- 文件:行号：四个同名文件整体（相对 main 的 PlatformService 尤甚：HEAD→未发布 +80/−199）
- 业务影响：若按「未发布树原样合入」或「用这 5 文件覆盖 main」，M009 已合并的合作方式白名单、软删口径、读端平台范围、主体投影、最高权限删批、存档≠停票等全部回退；且对当前 main **不能通过编译**。
- 可复现命令：见 §9 CMD-OVL（exit 1，3 errors，全 `parentEntityId`）。`git diff --numstat HEAD e28dad05 -- <五路径>`。
- 冻结判据：GEN-002、GEN-008、GEN-013；回退面另触 BSM-004、BEQR-148、PGM-018/026/027、GEN-004/005/006
- 2.3 依据：类型 4 编译与 diff
- 四分类：**技术性**（无业务发明：不是新规则，是发布方式与已修 main 冲突）
- 无业务发明理由：六类/软删/平台范围均已在 main 按 HTML 落地；本 finding 只禁止用旧文件盖掉它们。
- 工程缺陷：是
- 建议登记：是
- 最小处置：以 main `445a49f4` 为底三方合并，**禁止**整文件覆盖。按 001 连布尔也不迁。U08 相对 main 的净发布物应为空（或仅文档版本行，可留可删）。

### GR-U08-003 — 端点/DTO/列/页面/OpenAPI 跨段同批（技术性；同根 U04-002）

- 文件:行号：本段 5 文件全部插拔；依赖 U10 `V408__staff_registration_company_routes.sql:2`、U04 `types.ts:1121`、U03 页 `:320-326`、U14/U15 OpenAPI `setMerchantSelfOperated`
- 业务影响：只合本段 → 编译或运行期缺列；只合 DTO → 404；缺 V408 → 列表 SQLException。
- 可复现：main 无 `MerchantSelfOperatedRequest.java`；main 无 `is_self_operated` 列；CMD-OVL 即使修 parentEntityId，运行期仍要列。
- 冻结判据：GEN-002、GEN-008、GEN-013、GEN-014
- 2.3 依据：类型 4 文件存在性
- 四分类：**技术性**（无业务发明：是发布切分问题）
- 工程缺陷：是
- 建议登记：是（并 U04-002）
- 最小处置：随 001 删除一并消失。若发布者违背 001 保留布尔，则 U03/U04/U08/U10/OpenAPI 同一 PR，迁移重编号 ≥V411，且仍须以 main 为底而不是覆盖。

### GR-U08-004 — 自营成功路径双写审计（技术性）

- 文件:行号：`PlatformService.java:183-188` 闭包内 `auditEventService.recordSuccess("merchant.self_operated_changed", … Map.of("self_operated",…,"reason",reason))`；同方法 `executeWrite(..., Map.of())` → `AuditedOperationExecutor.java:130-138` 再记一条同名成功事件（空 metadata）
- 业务影响：每次成功变更两条 `merchant.self_operated_changed`，一条无事由。留痕不可单条对账。
- 可复现：静态两条调用；`executeWrite` 实现已读。未写库。首轮正常 POST 必发。
- 冻结判据：GEN-011、PGM-014
- 2.3 依据：类型 4 源码
- 四分类：**技术性**（无业务发明：未改「要留痕」的业务规则，只是实现记了两次）
- 工程缺陷：是
- 建议登记：是（若 001 删除端点则本项随消失，B2 可并入 001 处置）
- 最小处置：随 001 删除。若保留端点：删闭包内 `recordSuccess`，把 `self_operated`/`reason` 传入 `executeWrite` 的 successMetadata。

### GR-U08-005 — 本段无端点测试（技术性）

- 文件:行号：本段 5 文件 0 测试；U11 `M010RegressionTests.java:321-330` 只 `platform.saveMerchant(merchant.withSelfOperated(true)…)`，不断言 HTTP、幂等键、空事由、双轴、权限
- 业务影响：001/004 无红灯。只合 U08 时回归套件不会覆盖新端点。
- 可复现：`unpublished-03acdb98-input.json` 76 文件中无 `PlatformServiceTests`/`PlatformControllerTests`/`*SelfOperated*`。
- 冻结判据：GEN-003
- 2.3 依据：类型 4 清单检索
- 四分类：**技术性**（无业务发明）
- 工程缺陷：是
- 建议登记：是（随 001 删除则不必补测）
- 最小处置：随 001 删除。若违背 001：补可失败用例（空事由 400、无幂等键 400、成功后 cooperationForm 不变——后者会暴露 001，正是应红）。

## 8. 观察（无新增根因，完整登记，不漏并）

1. U03 页 POST 无 `idempotencyKey`：后端正确拒绝。已定稿 U03-001；本段不重开。001 删除动作后消失。
2. `import ApiException`（Controller:8）与 `import java.math.BigDecimal`（Jdbc:16）无引用：M009-034，基线既有。
3. `requireDate` 英文死代码、`readObjectMap` 无调用：M009-016，main 已删，覆盖会带回。并入 002。
4. `listMerchants()` 无过滤版被其它模块使用：M009-020，本段 diff 未改。
5. `createSpaceType` 控制器无 `requireIdempotencyKey`：M009-022，行为仍被幂等服务必填拦住。
6. `saveChainNodes` 无方法级事务：M009-023；本段 API 不调用。
7. 已批准删除行仍可 `findMerchantById` 后打自营标：需已知 id，铁律 6 收窄，不单列 finding。
8. 无乐观锁：与既有商户更新同窗，不新开闸。
9. OpenAPI/矩阵/附录在 U14–U17，本段只提供 Java 端点。001 删除后那些段须摘条。
10. `complex_test` flyway 含未发布 V400–V409 与 `is_self_operated`：M009-017/M010-009 环境项。本席未连库、未写库。
11. `MerchantRecord` 25 参委托把缺省布尔当 false：对旧构造点是兼容技巧；001 删除后应连委托一起删，避免「看起来像 25 参主构造」。
12. 联系人姓名明文、读列表每次 `merchant.viewed`：M009-023 已登记，HTML 未要求姓名脱敏。
13. `setMerchantSelfOperated` 方法上有 `@Transactional` 且 `executeWrite` 再包一层：不构成新缺陷。
14. 本段未改三 HTML。BSM L1433/PGM L578「标注为自营」与六类同轴，不是 HTML 错误，不上报人闸。
15. 未发布提交对象本仓不存在，只凭树 `e28dad05` 与输入副本审计。不得把副本写成「commit 03acdb98 在本仓可检出」。

## 9. 命令证据

```
CMD-LOCK: sha256sum docs/audit-2026-09/plan/criteria-lock.md
OUT: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  exit 0

CMD-HEAD: git rev-parse HEAD && git rev-parse origin/main
OUT: 445a49f409f0cfbe6dd616ae079be1299824c48f ×2  exit 0

CMD-OBJ:
  git cat-file -t 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb
  → fatal: git cat-file: could not get object info
  git cat-file -t e28dad059b2c7e08bcc95a8db18552d3f76edf4b → tree
  git cat-file -t 242f487233d028173cb9198eecd079f5d877405c → commit

CMD-COPY: sha256sum + wc -l -c 五文件；与清单、树 blob 5/5 相同（见 §2 表）

CMD-DIFF-BASE:
  git diff --shortstat 242f4872 e28dad05
  → 76 files changed, 4745 insertions(+), 1084 deletions(-)
  git diff --numstat 242f4872 e28dad05 -- <本段五路径>
  → 13 10 / 15 4 / 10 0 / 19 1 / 4 0

CMD-DIFF-MAIN:
  git diff --numstat HEAD e28dad05 -- <本段五路径>
  → 35 44 Jdbc; 16 16 MerchantRecord; 10 9 Controller; 80 199 Service; 4 0 DTO

CMD-MAP: python 解析 M009 → 87 IDs，MISSING=[] EXTRA=[]；69 条 HTML 出处行现行正本全命中

CMD-OVL: 复制 main backend → /tmp/u08-b1-grok，覆盖本段 5 文件；
  JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 mvn -o -DskipTests compile
  （第一次误用 JAVA_HOME=java-21-openjdk-amd64（无 javac）→ "release version 21 not supported"；
   不是业务 finding。改 Temurin 后真实编译。）
  EXIT=1
  ERROR JdbcPlatformRepository.java:497 cannot find symbol parentEntityId()
  ERROR JdbcPlatformRepository.java:582 BusinessEntityRecord 构造 10 参 vs 9 参
  ERROR PlatformService.java:122 cannot find symbol request.parentEntityId()
  3 errors

未跑：complex_test 集成、会清库的脚本、部署、安装依赖。
```

## 10. 结论

**FAIL**

理由（任一即不得 PASS）：存在未定稿工程缺陷 GR-U08-001…005；001 为添加项（虽已有操作员「删除」答复，B2 尚未对本段定稿）；三线已做、87 项已核、五文件已全文覆盖；无未答新 2.2、无 HTML 人闸；Grok 哈希行有效。不存在有条件通过。

发布者最小集：以 main `445a49f4` 为底，**不要**合入本段 5 文件；自营独立布尔按 U04-001 删除；员工分流按 U05-001 改读六类码。本材料未发布，本轮不修源。

## U08 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 39529 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，独立）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02 §7/§11；criteria-lock V0.11；modules.md V0.04 附录 U08（未发布材料第 8 段「组织与商户联动」，5 文件 2191 行，main 归属 M009，判据＝M009 映射行并集 87 项）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`。四个既有治理文件 `HEAD:<path>` ≠ `242f4872:<path>`（M009 已改）。未发布树 `e28dad05` 对应副本五 blob：Jdbc `763382a061`、MerchantRecord `0f893bd20b`、Controller `8f3495d8ef`、Service `36405812e9`、DTO `e230ab6c69`。
- 审计对象仍是只读输入副本 `.scratch/module-recheck-20260908/unpublished-03acdb98`（树 `e28dad059b2c7e08bcc95a8db18552d3f76edf4b`，基线 `242f487233d028173cb9198eecd079f5d877405c`），不是 main。三份 HTML 取 main 正本。未发布提交对象 `03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb` 本仓 `git cat-file -t` 仍不可解析。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B2 核实结论。
- 本席 B2-r1：**PASS**（001–006 逐 ID 全 PASS；添加 1＝U04-001 已答删除，不发新 2.2；发布前须修「是」=001–005，「否」=006）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`（ledger 第 2 行）。映射行 `M009 →` L6388；python 并集 87，MISSING=[] EXTRA=[]。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要核对只读（输入哈希、blob、numstat、main `PlatformService`/`MerchantRecord`/`BusinessEntityRecord`、副本五文件、HTML 锚点、`questions/U04-001.md` 答复首行「删除」、76 文件清单检索）；未重跑 mvn/测试、未装依赖、未碰 `complex_test*`。追加稿仅经 `/tmp/u08-b2-append.md` 中转，落盘后删除。

此票只判断 findings.md V1.58 顶部「当前 U08 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、「发布前须修」是/否与最小处置。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。材料未发布，定稿后状态应记 REVIEWED-FAIL（有已定稿缺陷，处置写给发布者），不做 B3。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/U08-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 39529 | `1ea87cac35dc91138db42ee68eaf120eb45152ff0fe021390ccb2135fd8add60` |
| `docs/audit-2026-09/modules/U08-b1-self-raw.md` | V0.01 | 26725 | `fb1a1e5706598e5907ec3a976deb024be8e760eb7994f4d99ccd4cf6ba9c7bf6` |
| `docs/audit-2026-09/findings.md` | V1.58 | 610588 | `adaf8ec8da1c485b8a1610350c940e2ef91b3ce0d535448d2004140dc805fcff` |
| `docs/audit-2026-09/ledger.md`（只读） | V2.65 | 487952 | `2030fdeac4d33cafa143dfe0eb38e7329ed042d9aa19e30e8f8113a2c4f7a88c` |
| `docs/audit-2026-09/questions/U04-001.md`（只读，沿用） | V0.01 | — | 答复首行精确「删除」（文件 SHA `5a9e9dab904fe9b82ccb2e414ae07515ea7192c7ed404685c31828a126c2b3d1`） |

候选形态：V1.58 顶部 U08-001…006；questions 0（沿用 U04-001 已答）；添加 1；无 HTML 修改申请。U08 ledger 行 IN_PROGRESS。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

HTML 正本哈希与 B1 一致：BEQR `4396bb71…1d66`、BSM `a7f7b7a3…66fa`、PGM `c4238afd…e71c`。副本本段 5/5 sha256/行/字节与树 `e28dad05` blob 全等（本轮复算）。`git diff --numstat 242f4872 e28dad05 -- <五路径>` = 13/10、15/4、10/0、19/1、4/0，合计 +61/−15。`git diff --numstat HEAD e28dad05 -- <五路径>` = Jdbc 35/44、Record 16/16、Controller 10/9、Service 80/199、DTO 4/0。`git diff --numstat 242f4872 HEAD -- <四既有路径>` = 34/22、12/1、9/0、198/61（席甲 B1 §2 与本席 B1 §6 同值，本轮复算相同）。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers | 先核技能；用户/提示词覆盖技能本体 |
| receiving-code-review | 先核 HTML/diff/blob/main 源码再接受或反驳席甲 OBS-2；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：001 添加是否因已答删除改判、002「三方合并+净空」是否最懒、003/004/005 应否并入 001、006 是否写错 §8 与 main 存档路径 |
| verification-before-completion | 声称前先跑 sha256/git rev-parse/git cat-file/diff/python 并集/rg；本票不声称本轮新编译绿 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流（本段 5 文件 → U03 页/U04 类型/U05 分流/U10 V408/U11 回归 → main 对照 → HTML） |
| ponytail-review | 只评最小删除/收窄，不落地。001 整段布尔不合入；002 不要覆盖四文件；004 只删闭包内 recordSuccess |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到**；未冒充调用 |
| Arc audit | 会派子代理；与双席冲突；**未用** |

### 完整差集（GR-001…005、§8 观察 1–15、席甲 CX-001…003、OBS-1…5）

漏并：无。两席 B1 全部临时 ID 仍落在 001–006。未把 finding 降观察、未把观察升格入队。未把本席 GR-002 的 M009 回退面拆成新 ID。席甲独有项无超出 CX-001…003 / OBS-1…5。

| 临时 ID | 落入正式 ID | 是否改写本席原意 |
|---|---|---|
| GR-U08-001 | U08-001 | 否：添加、已答删除、不再问 |
| GR-U08-002 | U08-002 | 否：覆盖回退 + 叠 main 编译失败；与 CX-003 并列 |
| GR-U08-003 | U08-003 | 否：独立入队、同根 U04-002 |
| GR-U08-004 | U08-004 | 否：独立入队；「B2 可并入 001」为许可不是强制 |
| GR-U08-005 | U08-005 | 否：独立入队、随 001 删除不必补测 |
| §8 1–15 | U08-006 | 第 7/11/14 条在；「含」为抽样（见 (d)） |
| CX-U08-001 | U08-001 | 与 GR-001 同一布尔实现面 |
| CX-U08-002 | U08-004 | 与 GR-004 同一双写审计 |
| CX-U08-003 | U08-002 | 与 GR-002 同一分叉/覆盖 |
| OBS-1…5 | U08-006 | OBS-2 对 main `:759` 的「归 M009 核查」子句事实错误，非阻断措辞（见 (d)） |

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/须修 | 本席票 |
|---|---|---|---|
| U08-001 | GR-U08-001 + CX-U08-001 | 添加（操作员已答删除＝U04-001）/是/是 | **PASS（添加已答删除/是/是）** |
| U08-002 | GR-U08-002 + CX-U08-003 | 技术性（分叉，main 已修 M009）/是/是 | **PASS（技术性分叉/是/是）** |
| U08-003 | GR-U08-003 | 技术性（跨段同批，同根 U04-002）/是/是 | **PASS（技术性同根/是/是；独立入队）** |
| U08-004 | GR-U08-004 + CX-U08-002 | 技术性/是/是 | **PASS（技术性/是/是；独立入队）** |
| U08-005 | GR-U08-005 | 技术性/是/是 | **PASS（技术性/是/是；独立入队）** |
| U08-006 | 双方观察 | 登记/否/否 | **PASS（登记/否/否；OBS-2 子句须改写）** |

添加入队 1（001，已答删除）。技术性入队 4（002 分叉、003 同批、004 双写、005 无测试）。登记观察 1（006）。冲突 0。questions 0。无 HTML 修改申请。

发布前须修「是」=001、002、003、004、005（5）；「否」=006（1）。与候选汇总一致。

### 重点 (a) U08-001 添加已答删除、须修是、整体不合入且不再问 — **PASS（成立）**

独立核，不采信席甲「全部是插拔」四个字为己证：

1. 本段相对基线 +61/−15 五文件。副本检索：Jdbc `:90/:97/:123/:135/:150/:166/:219/:396/:532/:622` 十处 `is_self_operated`；`MerchantRecord` canonical 末位 `boolean selfOperated` + 25 参委托默认 false + `withSelfOperated`；Controller `:86-91` `POST /merchants/{merchant_id}/self-operated`；Service `:178-191` `setMerchantSelfOperated`、`:655` `updateMerchant` 回读布尔；新文件 DTO 4 行。main 治理目录 `selfOperated|is_self_operated|self-operated` **0 命中**。
2. HTML 现行正本：BSM L701「区分自营、租赁、联营、代运营、服务商接入、其他这六类合作方式」；PGM L495「…其他等合作方式」、L498「合作模式分六类：…」；BEQR L231 合作模式闭集同六类；BSM L1433 / PGM L578「在商户档案上标注为自营的商户视同公司自有」——是同一轴的标注，不是第二开关。副本 `setMerchantSelfOperated` **0 处**读 `cooperationForm`。
3. `questions/U04-001.md` 答复首行精确「删除」；该答复已约束 U08 端点/DTO/仓储/`MerchantRecord.selfOperated`/V408 列。U04 定稿分类为**添加**，不因已答删除改判（U04 B2 已锁）。本段是该布尔的后端实现面，不是 U05 那种「读第二轴」的冲突消费方。

四分类：**添加**（GEN-017 顺序：HTML 无一句允许独立于六类的布尔或规定同步 → 落到添加）。工程缺陷是。发布前须修是。不再弹窗。

最小处置与本席 GR-001 同向、最懒：本段自营插拔整体不合入（删 DTO、端点、服务方法、记录字段与 25 参委托/`withSelfOperated`、Jdbc 全部 `is_self_operated` 插拔）；U05-001 已定稿改读六类码，本段只引用不发明。禁止：再问 2.2；把布尔改成与 `cooperationForm` 强制同步（那是新规则）；只删端点留列。

「整体不合入」指本段 +61/−15 的布尔实现面，不是把 M009 已修四文件从 main 摘走。四文件如何对待见 002。

票 **PASS**。

### 重点 (b) U08-002 并列 GR-002 与 CX-003、净发布物为空 — **PASS（成立且最懒）**

两席同一发布方式事实，CX-003 较窄（只写分叉/覆盖），GR-002 补上回退清单与叠 main 编译失败。并列不改写。

独立实核（本轮源码，未重跑 mvn）：

1. 四文件三向 blob 均不等：基线 ≠ HEAD ≠ 未发布。HEAD→未发布 numstat 与 B1 全等。
2. 未发布仍写 `parentEntityId`：Service `:122` `request.parentEntityId()`、Jdbc `:497` `businessEntity.parentEntityId()`、`:582-586` `BusinessEntityRecord` 10 参含 `parent_entity_id`。main `BusinessEntityRecord.java` 9 字段、治理目录 `parentEntityId` **0 命中**（M009-012）。把本段 5 文件盖到当前 main，符号缺失仍在——与 B1 CMD-OVL 3 errors 同一根。席甲 B1 把 76 文件盖到**基线**编译绿，只证明未发布树内部自洽，**不能**否定叠 main 失败。
3. 回退面：候选所列 M009-001/002/003/004/005/006/007/008/009/010/011/012/014/015/016/026/027/028/036 均落在这四个同名文件内（本席 B1 §6 表 16 行、19 个 ID）。intro「共 16 项」＝表行，定稿可写清，不改票。

四分类：**技术性**（无业务发明：不是新规则，是发布方式与已修 main 冲突）。须修是。

最小处置与本席 GR-002 原文同向，且最懒：

- 以 main `445a49f4` 为底三方合并，**禁止**整文件/整树覆盖。
- 按 001 连布尔也不迁。
- 本段相对 main 净发布物为空（版本行可留可删）。

「三方合并 + 净空」＝本席 B1 §10「不要合入本段 5 文件」。不是先合再剥。禁止：为迁布尔去改 main 已修的白名单/软删/平台范围/`withEntities`。

票 **PASS**。

### 重点 (c) U08-003/004/005 独立入队、随 001 删除消失 — **PASS（应保持独立；处置不发明业务）**

不得并入 001。理由（沿 U04-002 定稿，不新造口径）：

| ID | 本席 B1 | 缺陷本体（删布尔后是否还在） | 若违背 001 仍发布布尔 |
|---|---|---|---|
| 003 | GR-003 建议并 U04-002 | 跨段切分；001 执行后链消失 | U03/U04/U08/U10/OpenAPI 同一 PR，迁移 ≥V411，仍以 main 为底 |
| 004 | GR-004「B2 **可**并入 001」为许可 | 双写只活在自营端点上 | 删闭包内 `recordSuccess`，事由进 `executeWrite` 成功明细 |
| 005 | GR-005 随 001 则不必补测 | 无端点测试 | 补可失败用例：空事由 400、无幂等键 400、成功后 `cooperationForm` 不变 |

001 是「加了不该有的开关」。003 是切分、004 是记了两次、005 是没红灯。并进 001 会丢掉「违背删除答复时仍须修」的残面。本席 GR-004 的「可并入」不是「必须并入」。候选保持独立，与 U04-002 同一句话，不发明业务。

独立实核：

- 003：main 无 DTO/端点/列；副本 V408 `:2` `ALTER TABLE merchants ADD COLUMN is_self_operated …`；U11 不在本段 5 文件。只合本段、main 无列 → `getBoolean("is_self_operated")` SQLException。
- 004：副本 Service `:183-188` 闭包内 `recordSuccess("merchant.self_operated_changed", … Map.of("self_operated",…,"reason",reason))`；同方法 `executeWrite(..., Map.of())` → main `AuditedOperationExecutor.java:130-138` 再记一条同名成功事件。`archiveMerchant` 闭包只 `saveMerchant` + `setMerchantArchiveReason`，不另 `recordSuccess`。
- 005：76 文件 0 条 `PlatformServiceTests`/`PlatformControllerTests`/`*SelfOperated*`（仅 DTO 文件名命中）；U11 `:321-330` 只 `saveMerchant(withSelfOperated(true))`，不断言 HTTP/幂等/空事由/双轴/权限。

「成功后 `cooperationForm` 不变」是暴露 001 双轴的可失败灯，不是规定「自营必须改合作方式」。若执行 001，这条不必写。

票 **PASS**。

### 重点 (d) U08-006 观察 — **PASS（未写错 §8 第 7/11/14 条；OBS-2 与 main 源码矛盾，非阻断措辞）**

006 列席甲 OBS-1…5，并以「Grok §8 1–15（含 1 U03-001 不重开；2/3/4/5/6/12 归 M009 既有项；7 已批删行可打自营标铁律 6 收窄；9 U14–U17 随 001 摘条；10 complex_test 含 V400–V409 环境项；11 25 参委托随 001 一并删；14 不上报人闸；15 仅凭树审计）」收本席观察。「含」是抽样不是闭集。第 8 条（无乐观锁、与既有更新同窗）、第 13 条（方法上 `@Transactional` 再包 `executeWrite` 不构成新缺陷）未抽样写出，定稿不必逐条抄全，不得写成「Grok 无这两条」或把它们升格入队。

第 7 条：本席原文「已批准删除行仍可 `findMerchantById` 后打自营标：需已知 id，铁律 6 收窄，不单列 finding」。006 写「铁律 6 收窄」，未立项。独立核：副本 `setMerchantSelfOperated` 走 `findMerchantById`，无 `delete_status` 过滤。维持观察。

第 11 条：本席原文「25 参委托把缺省布尔当 false……001 删除后应连委托一起删」。006 写「25 参委托随 001 一并删」。未改写成「保留兼容构造」。001 处置已含「记录字段与 25 参委托」。

第 14 条：本席原文「本段未改三 HTML。BSM L1433/PGM L578『标注为自营』与六类同轴，不是 HTML 错误，不上报人闸」。006 写「不上报人闸」。未把 HTML 送人闸。

席甲 OBS-2 vs 本席 M009-001/002——以 main 源码裁定，不以席甲 grep 为准。

| 核项 | 未发布副本 | main HEAD `445a49f4` |
|---|---|---|
| `saveMerchant` 重建 | `:635-655` **19 参** `new MerchantRecord(…, now, now)` 再 `.withSelfOperated(回读)`；19 参委托把 `archivedAt=null, deleteStatus=none` | `:757` `existing = update ? requireMerchant(merchantId) : null`；`:759` `new MerchantRecord(` 后接 **25 参**：`:777-784` 传入 `existing.createdAt()` / `existing.archivedAt()` / `archivedBy` / `deleteStatus` / `deleteRequestedBy` / `deleteRequestedAt` / `deleteReason` |
| 注释 | 无 M009-001/002 保留句 | `:755-756` 明文「M009-001/002：更新须命中已有商户，并保留存档/删除申请状态与 createdAt」 |
| 19 参委托是否仍在 | 在（`MerchantRecord.java:48-58`） | 仍在（`:38-48`，给其它构造点）；**更新路径不用它** |

结论：席甲 OBS-2「main HEAD `:759` 仍 `new MerchantRecord(` 重建、归 M009 核查」半真半假——语法上确有 `new MerchantRecord(`，语义上已是 25 参保留 archive，与本席 GR-002 表「main 已修：更新 requireMerchant，保留 archive/delete/createdAt」不矛盾于代码事实、矛盾于席甲的「未修/须核查」读法。M009-001/002 已在当前 HEAD 落地（B4-r1 队列改否）；覆盖未发布 19 参会把已修面打回去，这件事已经在 U08-002 回退清单里，不是「main 既有、归 M009 再审」的 U01 口径项。U01「main 既有」＝HEAD 同位置已有且本材料 diff 未触及；此处未发布更新路径仍是 19 参，main 不是。

006 分类登记、须修否：成立（观察桶不把抹档从 002 拆成新 finding）。定稿 OBS-2 必须改写成：「未发布 `saveMerchant` 19 参抹存档；main `:759` 为 25 参保留 archive（M009-001/002 已修）；覆盖回退见 002；不得写成 main 未修或再开 M009 核查」。不改 006 票。

第 3 条抽样写成「2/3/4/5/6/12 归 M009 既有项」略混：本席 §8.3 `requireDate`/`readObjectMap` 是 **并入 002**（M009-016，main 已删，覆盖会带回），不是 U01 那种「main 同位置未碰 → 须修否」。002 清单已含 016。定稿把第 3 条从「既有项」改「并入 002」即可，不改票。

票 **PASS**。

### 001 独立裁定（添加已答删除／须修是）

见 (a)。布尔第二轴无 HTML 上位。处置整体不合入、不再问。票 **PASS**。

### 002 独立裁定（技术性分叉／须修是）

见 (b)。以 main 为底，连布尔也不迁，净空。票 **PASS**。

### 003 独立裁定（技术性同根 U04-002／须修是；独立）

见 (c)。随 001 消失；违背则同 PR、≥V411。票 **PASS**。

### 004 独立裁定（技术性／须修是；独立）

见 (c)。随 001 消失；保留则去重审计。无业务发明（未改「要留痕」）。票 **PASS**。

### 005 独立裁定（技术性／须修是；独立）

见 (c)。随 001 不必补测。票 **PASS**。

### 006 独立裁定（登记／须修否）

见 (d)。票 **PASS**。

### 最小修正意见（非阻断，不改票）

1. 006 OBS-2 定稿改写：未发布 19 参抹档；main `:759` 25 参保留 archive；回退在 002；禁止「归 M009 核查」。
2. 006 抽样第 3 条改「并入 002（M009-016）」，不要跟 2/4/5/6/12 捆成「main 既有须修否」。
3. 002 intro「共 16 项」可注「16 行 / 19 个 ID」。
4. 001 六类闭集主锚用 BSM L701 / PGM L498 / BEQR L231；PGM L495 原文是「等合作方式」，不单独当闭集句。
5. ledger 开工节「+75/−16」与实算 +61/−15 不符；本票不改 ledger。

逐 ID：001 PASS，002 PASS，003 PASS，004 PASS，005 PASS，006 PASS。发布前须修是=001/002/003/004/005，否=006。添加 1（已答删除）、questions 0、无 HTML 修改申请、无 LOCK-AMEND。合取尚待席甲本批 B2；本席票单独不能定稿。材料未发布，定稿后 U08 → REVIEWED-FAIL，处置写给发布者，不做 B3/B4/B5。共同处置：本段相对 main 净发布物为空；禁止整文件覆盖 M009 已修四文件。

本批最终：**PASS**
