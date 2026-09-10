CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U12 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 只读全量三线（未发布材料第 12 段；不做 B3/B4/B5；处置写给发布者，本轮不修源）
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 段：U12 — 未发布材料第 12 段「设备与主管审批测试」
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 末尾附录；`plan/runbook.md` V0.02 §11；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 审计对象（不得冒称 main 或已发布修复）：
  - 未发布提交：`03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb`（本仓 `git cat-file -t` **无此 commit 对象**）
  - 未发布树：`e28dad059b2c7e08bcc95a8db18552d3f76edf4b`（`git cat-file -t` = **tree**，可解析）
  - 发布基线：`242f487233d028173cb9198eecd079f5d877405c`（commit，可解析）
  - 只读输入根：`/home/james/complex-management/.scratch/module-recheck-20260908/unpublished-03acdb98`
  - 清单：`.scratch/module-recheck-20260908/unpublished-03acdb98-input.json` V0.01，76 文件
- 本段：2 文件，物理行 1519，标记 NORMAL；main 主归属：**M066**（DeviceCredentialVerifierTests）+ **M042**（SupervisorReviewServiceTests）
- 冻结判据：M042 ∪ M066 映射行并集 107 项（相对开工提示 MISSING=[] EXTRA=[]；M042=87，M066=50，并集=107）
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 且发布前须修：GR-U12-001、GR-U12-002、GR-U12-003、GR-U12-004、GR-U12-005
- 添加项（B1 只登记，不代用户回答 2.2）：**无**。目录从 10→12 的 `staff.shift.publish` / `staff.payroll.penalty_cap.adjust` 能指到 BSM L1443「排班表最终发布须经该主管的上一级审批」与 L1458「各类扣除合计不得超过……20%……调整须经人力、财务、法务三审并由顶层平台公司最终确认」，属细化不是添加。工作手机改验设备级公钥能指到 PGM-032「人、私人手机、工作手机要能对应起来」与 BSM-044「平板、公司工作手机、员工私人手机 App 是不同设备和不同用途」，属细化。
- 同根已定稿、不新开 2.2：U07-002 / U07-003 / U07-005 / U09-001 / U09-002 / U09-003 / U09-004 / U09-005 / U05-008 / U01-001 / U10-001
- 只登记、建议队列「否」：观察见 §8
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做写操作
- 本报告不表示全仓、main 模块或发布门禁通过。M001–M012 检查点已合入当前 HEAD（PR #428/#429）。原 322/471 模块结论不继承到本未发布段。不得把本副本冒称 main 源码或已发布修复。
- 本席独立实跑：未发布树 `/tmp/u12_grok_unpublished/backend` `mvn -o -Dtest=DeviceCredentialVerifierTests,SupervisorReviewServiceTests test` **64/64 PASS**（23+41）；只把本段 2 个测试文件覆到 HEAD 源码 `/tmp/u12_grok_main_plus_u12tests` **64 run / 3 fail / 2 error**；再叠未发布 `DeviceCredentialVerifier.java` 后设备测 **23/23 PASS**；再叠未发布 `SupervisorReviewService.java` 后主管测 **41/41 PASS**。64/64 **只证明副本树自洽**，不能消解 HTML 冲突、测试缺口或对 main 的分叉。`complex_test` 事后只读 max=**410**，无 `hr_review` 行、无 `staff_penalty_cap_revisions` 表、无 `penalty_cap_bps` 列。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`（SHA256 `fa5c1e5ee76b1c8f1ae56101f52c9e239de75d5c578adc61227b92d10b7e52ef`） | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md`（SHA256 `b9339b09ee3980808d8c5a35c7251b891b8b1e0036ec4ca37812b976ebddf6b6`） | 隔离 `/tmp` 叠文件复现 overlay 红与钥源分叉 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md`（SHA256 `808fc5717aa88ad65efff312b11c186294d3e6ee301afb584e2f86599b137787`） | 只借读错误→复现→追数据流（本段测试 → U07/U09/U05 副本 → main 对照 → HTML） |
| verification-before-completion | superpowers 同名（SHA256 `2befe7fc55bcadaa3d97dd9e8efeb633d2561c0ebe74c5a8b17c4d9e7e4520b3`） | 先跑命令再声称；64/64 不能写成 HTML 通过 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md`（SHA256 `40df33b58fc6ef889b93585733feb9566b76e9586efa7f376785c1e995197ac0`） | 只列可删复杂度（未用 `any` 导入、4 参构造不注入发布器），不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md`（SHA256 `a842323e664e5af104eac5c97ad22fda929ebeb62d81c501161ac1f6f482db58`） | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md`（SHA256 `6f9ae7c1c295d7e67eda1e3a616c337c057f45f1a5bf2955efb07d539a655b79`） | 本段验证均秒～十几秒，未起 monitor |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：套件文档写明已并入 `grilling`，无独立技能可装
- `ubiquitous-language`：本机技能树未找到
- Arc `audit` 会派评审子代理：与双席/§7 冲突，**不调用**
- `using-superpowers` 要求技能优先于探索：与「禁止子代理 / 本提示指定 controller」冲突处，按用户指令收窄

明确不用：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、MCP、第三主体。

内部 grilling 树（自答，未问操作员）：

1. 这 2 个文件必须存在吗？必须有**等价回归**。删掉它们，PGM L572 人/私人手机/工作手机对应、BSM L929 三端不同用途、BSM L1443 排班发布须上一级审批、BSM L1458 扣除上限 20% 三审、设备绑定型写请求验签、主管审批链既有闸，在可执行断言上都没有本段这一层网。文件作为测试是细化，不是添加。
2. 整段按现文件叠进当前 main、首轮会不会过测试？**不会。** 只把 2 个测试文件覆到 HEAD 源码：`mvn -o -Dtest=DeviceCredentialVerifierTests,SupervisorReviewServiceTests test` 64 run / **3 fail / 2 error**（见 CMD-OVERLAY）。`listPolicies` 期望 12 实得 10；工作手机验签桩打在 `signingKeyStore` 而 main 读 `findActiveCredential`。
3. 只合本段测试、库仍是当前 `complex_test`（V410、无 `hr_review`、无处罚上限修订表），首轮会不会因库红？**本段两个类是纯单测，不连库。** 但发布时若把 U09/U10 一并合入而测试仍不覆盖 `DEPARTMENT_REVIEWS_PENDING` / `hr_review` 缺线，库侧缺口会被 41/41 绿测放行。只读：`approval_lines` 有 finance/legal/tax，**无 hr_review**。
4. 副本树自己能不能绿？能。`/tmp/u12_grok_unpublished` 未发布 backend **64/64 PASS**。这只证明「测试与未发布 U07/U09 实现一致」，**不能**证明与 HTML 或与当前 main 一致。
5. 64 个绿测是不是把错误口径锁死了？部分是。`privatePhoneKeyCannotReplaceWorkPhoneKey` 把「工作手机不得回落员工私人钥」锁成未发布设备级公钥路径；`listPolicies` 把目录数锁成 12 但不跑新闸；`nonDeviceBoundTypePassesThrough` 把无设备号自报 `web` 在 enforce 下通过锁绿——这是首轮正常路径上的锁，不是两三件不巧才暴露。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`，属正常）
- `criteria-lock.md` 版本 V0.11；判据总数 401
- M042 映射行 L6421（87 ID）；M066 映射行 L6445（50 ID）；并集 107 ID 与开工提示逐项相同（MISSING=[] EXTRA=[]）；定义区均从锁全文读过；HTML 出处行本席对现行正本逐条 HIT（见 §1.2）

HEAD（独立实算）：

- `git rev-parse HEAD` = `git rev-parse origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`
- `git log -1 --oneline`：`445a49f4 Merge pull request #429 from jxh1979/audit/20260909/M011-M012-checkpoint`
- 本席对照 main 已修项时以 **当前 HEAD=445a49f4** 为准（含 M001–M012）
- 本席未设 `GIT_INDEX_FILE`，未动主索引

ledger 只读核验（不改）：V2.76，504373 bytes，SHA256 `7670216136414a7f30774dea28fa1f8d1bd9fac94da927aceee9b0d726a90f14`（与开工提示一致）。findings V1.65，640973 bytes，SHA256 `062532983ce6f30a26838df5cc358f8b679674c7033c772db018ca2e4ba191e0`（与开工提示一致）。台账中与本段直接相关的已定稿项（背景，不继承 PASS）：

- U00-000：未发布树相对 242f4872 全 76 文件与 main 分叉
- U07-002：工作手机验签改读设备级公钥；只合 U07 则 main 设备测红；U01/U02/U07/U12 同一 PR、只认一把钥
- U07-003：无 `X-Device-Id` 时信自报终端类型（同根 U01-001）
- U07-005：以 main 为底重放，禁止整树覆盖
- U09-001：只合 U09 则 main M042 单测 `isEqualTo(10)` 红
- U09-002：`isMoneyAction` 未纳入 `staff.payroll.penalty_cap.adjust`
- U09-003：`hr_review` 只在 U10 V407 种子，main V391/V410 无
- U09-004：U12 `SupervisorReviewServiceTests:55-63` 只改目录 10→12，无新闸可失败用例
- U09-005：`eventPublisher` `required=false`，批准后不落地
- U05-008 / U10-001 / U11-001：以 main 为底重放，迁移 ≥V411，禁止整树覆盖；U05+U06+U07+U09+U10+U11+U12 同一 PR

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

三 HTML 只读、未改。本段冻结集含 HTML 派生 ID；分类对照现行正本原文（类型 1）。三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。

清单本段 2 条与独立 `sha256sum`/`wc -l`/`wc -c` **2/2 MATCH**。副本 SHA 与树 `e28dad05:<path>` blob（`git hash-object`）**2/2 COPY==TREE**。全 76 文件相对基线 `git diff --shortstat 242f4872 e28dad05` = **76 files changed, 4745 insertions(+), 1084 deletions(-)**（与开工提示一致）。

对象核验：`git cat-file -t 03acdb98…` fatal（无 commit）；`e28dad05…` = tree；`242f4872…` = commit。

本段相对发布基线逐文件 numstat（`git diff --numstat 242f4872 e28dad05 -- <path>`）：

| 路径 | + | − | 树 blob | 副本 SHA256 | 清单/字节/行 |
|---|---:|---:|---|---|---|
| `backend/src/test/java/com/complex/platform/gate/DeviceCredentialVerifierTests.java` | 11 | 2 | `98b57af68c2cdacbf6729982cc1319258a78d1a7` | `09abf3c2de8a035095bd8eec2a5becca12d79dd79c2463af4e54f0e3c64cc9cb` | MATCH 19456 / 392 |
| `backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java` | 4 | 2 | `95b1858656c9ab90a104c4b717fea38427d9feb1` | `bdf54d41a712ec9f38e18a023d6fe611ffd5029909c72012c46f16d80dde0972` | MATCH 76894 / 1127 |
| **合计** | **15** | **4** | | | 1519 行 |

`git diff --stat 242f4872 e28dad05 -- <2 paths>`：`2 files changed, 15 insertions(+), 4 deletions(-)`。

HEAD 同名：2/2 HAS 且 **BASE blob == HEAD blob**（`git log 242f4872..HEAD -- 两路径` 空；`git diff 242f4872 HEAD -- 两路径` 0 字节）。M009–M012 **未改本段既有测试文件**。对抗式分叉不在「这些测试文件被检查点改过」，而在测试所锁的生产符号/钥源/目录相对 main 已分叉。

`@Test` 计数：Device 23 + Supervisor 41 = **64**（main 同名 22+41=63；本段 Device 新增 1 个用例）。

### 1.1 与 main 分叉对照（对抗式固定问题）

本段 2 个既有文件 **BASE==HEAD**，按同名路径用 TREE 覆盖 HEAD **不会回退** M009–M012 在这些测试文件里的修复（那些检查点根本没碰它们）。冲突面是测试所锁的生产行为与整树：

1. **只合本段测试、不合 U07/U09（GR-U12-001）**。HEAD `DeviceCredentialVerifier.resolveSigningKey` 对 `staff_work_phone` 仍读 `staffRepository.findActiveCredential(profile.id())`（main `:249-252`）；未发布改读 `deviceSigningKeyStore.findActivePublicKey(deviceId)`（副本 `:251`）。HEAD `SupervisorReviewService` 策略目录 10 条，无 `staff.shift.publish` / `staff.payroll.penalty_cap.adjust`。本席 `/tmp/u12_grok_main_plus_u12tests`（main backend + 仅 2 个 U12 测试）`mvn -o test` **EXIT 1**：64 run / 3 fail / 2 error。首轮即红。
2. **钥源分叉的具体失败（GR-U12-002）**。未发布 `setUp` 只桩 `signingKeyStore.findActivePublicKey(DEVICE)`，不再桩 `findActiveCredential`。叠到 main 后：`validSignatureDerivesTrustedTypeVerified` / `bodySignedMatchesVerifies` 抛 `DEVICE_CREDENTIAL_INVALID`/`credential_required`；`invalidSignatureRejectedEvenInGrace` 因无公钥走 `noProof`+grace 而不抛；`privatePhoneKeyCannotReplaceWorkPhoneKey` 期望抛、main 用员工钥验签成功故**不抛**。把未发布 `DeviceCredentialVerifier.java` 叠回同一 tmp 后设备测 **23/23 PASS**——证明失败点就是钥源，不是夹具签名算法。
3. **目录 10→12（GR-U12-001/003）**。`SupervisorReviewServiceTests:58` `isEqualTo(12)`，main 服务返回 10。把未发布 `SupervisorReviewService.java` 叠到同一 tmp 后主管测 **41/41 PASS**。41/41 仍不经过 `DEPARTMENT_REVIEWS_PENDING`、`staff.shift.publish` 评估链、`publishEvent`。
4. **生产文件相对 HEAD 的真实改动**（本段测试所调用、不在本段文件列表）：`git diff --stat HEAD e28dad05 -- DeviceCredentialVerifier.java SupervisorReviewService.java` = `2 files changed, 18 insertions(+), 8 deletions(-)`；`git diff 242f4872 HEAD --` 这两生产文件 **空**（main 自基线未改它们）。M009–M012 未改这两处实现，但 **V410 已占编号、U10 V407 的 `hr_review` 种子不在当前库**。
5. **整棵未发布树覆盖**仍会回退其它段 M009–M012（U03/U08 已证实）。本段文件不是那些同名路径的载体，但发布说明必须禁止整树覆盖，且本段必须与 U05+U06+U07+U09+U10+U11 **同一 PR**、以 main 445a49f4 为底重放、迁移 ≥V411。

只读 `complex_test`（`docker exec complex-postgres psql -U complex -d complex_test`，仅 SELECT）：flyway 成功版本 **410,399,398,397,396**；`approval_lines` 有 `finance_review`/`legal_review`/`tax_review`，**无 `hr_review`**；`staff_penalty_cap_revisions` 表 **0**；`staff_payroll_statements.penalty_cap_bps` **0**。本段单测不连库，此只读用于证明 U10 种子未入当前库——U12 测试也不覆盖缺种子路径。

### 1.2 HTML 锚点（本席对现行正本逐行 HIT）

锁出处行 → 正本该行含锚点标题/关键字，89/89 HTML 派生 ID HIT + 18 GEN = **107/107**：

| ID | 锁出处 | HIT |
|---|---|---|
| GEN-001…018 | 通用工程判据 | 定义区已读 |
| BEQR-063 | beqr:111 高危动作（需审批/双证据） | HIT |
| BEQR-074 | beqr:125 隐私桌台权限 | HIT |
| BEQR-143 | beqr:226 四类主体 | HIT |
| BEQR-144 | beqr:227 经营主体关系 | HIT |
| BEQR-145 | beqr:228 城市项目公司 | HIT |
| BEQR-146 | beqr:229 运营公司职责 | HIT |
| BEQR-147 | beqr:230 主要收入归属 | HIT |
| BEQR-148 | beqr:231 合作模式 | HIT |
| BEQR-149 | beqr:232 职级 | HIT |
| BEQR-150 | beqr:233 终端十一端 | HIT 客人手机应用…工作人员平板、员工工作手机、员工私人手机… |
| BEQR-151 | beqr:234 最终权限计算 | HIT |
| BEQR-152 | beqr:235 项目与敏感数据权限 | HIT |
| BEQR-153 | beqr:236 终端开关优先级 | HIT |
| BEQR-154 | beqr:237 综合体项目状态 | HIT |
| BEQR-155 | beqr:238 商户/运营主体状态 | HIT |
| BEQR-156 | beqr:239 合同状态 | HIT |
| BEQR-157 | beqr:240 员工状态 | HIT |
| BEQR-158 | beqr:241 设备状态 | HIT 待领用、在用、待归还、维修中、挂失、报废、归档 |
| BEQR-159 | beqr:242 权限状态 | HIT |
| BEQR-166 | beqr:255 合同审批链 | HIT |
| BEQR-189 | beqr:291 管理费方式 | HIT |
| BSM-001 | bsm:505 整体说明 | HIT |
| BSM-002 | bsm:526 一笔业务怎么走 | HIT |
| BSM-003 | bsm:563 系统通用能力 | HIT |
| BSM-004 | bsm:566 配置数据可以存档、也可以删除 | HIT |
| BSM-005 | bsm:581 分步引导 | HIT |
| BSM-006 | bsm:584 动态刷新二维码 | HIT |
| BSM-007 | bsm:587 平台管理员 | HIT |
| BSM-009 | bsm:596 基础管理 | HIT |
| BSM-012 | bsm:611 酒吧平板/工作手机 | HIT |
| BSM-013 | bsm:616 触摸屏一体机 | HIT |
| BSM-026 | bsm:686 各类入口 | HIT |
| BSM-028 | bsm:698 主要功能 | HIT |
| BSM-029 | bsm:714 和谁有关 | HIT |
| BSM-044 | bsm:929 酒吧平板/工作手机：平板点酒，工作手机核销 | HIT 平板、公司工作手机、员工私人手机 App 是不同设备 |
| BSM-045 | bsm:933 主要功能 | HIT |
| BSM-046 | bsm:953 平板图形点单 | HIT |
| BSM-047 | bsm:963 和谁有关 | HIT |
| BSM-048 | bsm:978 触摸屏一体机 | HIT |
| BSM-049 | bsm:982 主要功能 | HIT |
| BSM-050 | bsm:996 和谁有关 | HIT |
| BSM-072 | bsm:1308 商户管理 | HIT |
| BSM-074 | bsm:1326 和谁有关 | HIT |
| BSM-114 | bsm:1827 各类入口：不同人只看自己该看的 | HIT |
| BSM-115 | bsm:1897 相互关系 | HIT |
| BSM-120 | bsm:1957 已确认规则 | HIT |
| PGM-001 | pgm:256 整体定位 | HIT |
| PGM-002 | pgm:308 治理顺序 | HIT |
| PGM-003 | pgm:339 模块入口 | HIT |
| PGM-004…016 | pgm:342–404 入口卡片 | HIT |
| PGM-017 | pgm:412 综合体和集团 | HIT |
| PGM-018…021 | pgm:415–452 | HIT |
| PGM-026 | pgm:492 主要功能 | HIT |
| PGM-027 | pgm:507 业务边界 | HIT |
| PGM-031 | pgm:555 外部经销商 | HIT |
| PGM-032 | pgm:572 员工和手机：人、私人手机、工作手机要能对应起来 | HIT |
| PGM-033 | pgm:576 主要功能 | HIT |
| PGM-034 | pgm:586 工作手机和企业微信 | HIT |
| PGM-035 | pgm:599 提成发放 | HIT |
| PGM-036 | pgm:611 设备管理：谁领了什么设备 | HIT |
| PGM-037 | pgm:615 主要功能 | HIT |
| PGM-038 | pgm:624 和现场业务的关系 | HIT |
| PGM-040 | pgm:643 权限和职级 | HIT |
| PGM-041…044 | pgm:647–688 | HIT |
| PGM-045 | pgm:712 终端开关 | HIT |
| PGM-046 | pgm:716 能控制哪些端 | HIT |
| PGM-047 | pgm:728 怎么控制 | HIT |
| PGM-048 | pgm:749 商户侧 | HIT |
| PGM-060 | pgm:879 会员身份 | HIT |
| PGM-065 | pgm:935 连接关系 | HIT |
| PGM-066 | pgm:951 已拍板规则 | HIT |

本段测试直接咬住的 HTML 原文（类型 1，分类用）：

- BSM L1443：「排班表最终发布须经该主管的上一级审批后生效。」
- BSM L1458：「各类扣除合计不得超过该员工当月总薪资的 20%（……调整须经人力、财务、法务三审并由顶层平台公司最终确认……）」
- BSM L929：「平板、公司工作手机、员工私人手机 App 是不同设备和不同用途。」
- PGM L572：「人、私人手机、工作手机要能对应起来」
- PGM L528/BSM L707：逐级审批、平行线、申请人不得自审（既有 41 测覆盖的主管链，非本段新增）

## 2. 文件覆盖（全量，不只 diff）

两文件均完整读完（Supervisor 1127 行按 L1–400 / L400–800 / L800–1127 三段覆盖，无抽样宣称 PASS）。

### 2.1 `DeviceCredentialVerifierTests.java`（392 行，23 @Test）

| 行段 | 内容 | 覆盖 |
|---|---|---|
| 1–72 | 包、V0.01 注释、mock 仓储、`setUp` 改桩 `signingKeyStore.findActivePublicKey`、`verifier` 构造 | 已读 |
| 73–118 | 签名请求夹具；`nonDeviceBoundTypePassesThrough`；`validSignatureDerivesTrustedTypeVerified`；**新增** `privatePhoneKeyCannotReplaceWorkPhoneKey` | 已读 |
| 119–212 | enforce/grace/过期戳/未绑定/读请求/类型篡改/未知设备 | 已读 |
| 213–321 | web Bearer/Cookie、app 写、归属不符、null staffId、kiosk 设备钥、body 摘要 | 已读 |
| 322–392 | 自报头不能逃设备闸；`deviceIdOnly`/`profile`/`credential` 夹具 | 已读 |

相对基线真实 diff（完整，非摘要）：

1. L3 增 `// 文件版本：V0.01`
2. `setUp`：删 `when(staffRepo.findActiveCredential("sp1")).thenReturn(Optional.of(credential(pubB64)))`，改 `when(signingKeyStore.findActivePublicKey(DEVICE)).thenReturn(Optional.of(pubB64))`
3. 新增 `@Test privatePhoneKeyCannotReplaceWorkPhoneKey`：把设备钥置空、把员工钥设为同一公钥，期望 `isInstanceOf(ApiException.class)`（**不断言 `e.code()`**）

未改但仍全量审的既有用例：`nonDeviceBoundTypePassesThrough`（无设备号自报 web，enforce 下 `verified=false` 且放行）、grace 缺凭证放行、登记类型压过自报 web、kiosk 用设备钥、body 篡改。

未使用导入：`import static org.mockito.ArgumentMatchers.any`（全文件 `any(` 调用 0）。观察，不升 finding。

### 2.2 `SupervisorReviewServiceTests.java`（1127 行，41 @Test）

| 行段 | 内容 | 覆盖 |
|---|---|---|
| 1–53 | V0.02 注释、4 参构造 `SupervisorReviewService`（**不注入** `ApplicationEventPublisher`）、tearDown | 已读 |
| 54–74 | **唯一业务 diff**：`listPolicies` total 10→12，`contains` 增 `staff.shift.publish`、`staff.payroll.penalty_cap.adjust` | 已读 |
| 75–297 | 未知动作、链外/跳级、逐节点通过、驳回整单、平行线、自审、档位下限、指定最终审批人 | 已读 |
| 298–411 | 无组织层级 fail-closed、specific_role 角色闸、R-003 组织链 | 已读 |
| 412–520 | 待审范围过滤、税费准入拒、`levelAwareRepository` 夹具、`SHARED_RANKS`/`chainL1` | 已读 |
| 521–798 | 证据 fail-closed、放行消费一次、转发、超时升级、主会话、涉钱时限、原因保留、重启读库 | 已读 |
| 799–995 | 幂等、零额免单、重复审批、跨租户、税费三线、链再生、节点 CAS、整单归约 | 已读 |
| 996–1127 | 内嵌 `InMemorySupervisorReviewRepository`（`lockSupervisorReview` 走接口 default 空实现） | 已读 |

相对基线真实 diff 仅 `listPoliciesReturnsBlockingCatalogAndWritesAudit` 三处断言。其余 40 个 @Test **字节级与 main 同名文件一致**（BASE==HEAD）。41 个用例名称中 **0** 含 `shift.publish` / `penalty_cap` / `DEPARTMENT_REVIEWS` / `publishEvent` / `hr_review`。

`isAlwaysBlocked`（未发布服务 `:860-864`）不含 `staff.shift.publish` / `staff.payroll.penalty_cap.adjust`；两策略 `minAmountCent=0`，`amountCent` 默认 0 时 `amountHit=true` 仍会 blocked。测试从未调用这两条 `evaluate`，此路径无断言。

## 3. 数据流（本段文件 → 未发布其它段 → main → SQL/HTML）

```
U12 DeviceCredentialVerifierTests
  → U07 副本 DeviceCredentialVerifier.resolveSigningKey (:236-251)
       工作手机：校验 staffId==当前档案后读 deviceSigningKeyStore.findActivePublicKey(deviceId)
  → main 同方法 (:236-254)
       工作手机：校验 staffId 后读 staffRepository.findActiveCredential(profile.id()).devicePublicKey()
  → U01/U02 安卓签名头 / DeviceRequestHeaders（本段不改客户端）
  → HTML PGM-032 / BSM-044 / BEQR-150 / BEQR-158

U12 SupervisorReviewServiceTests
  → U09 副本 SupervisorReviewService
       policies 12 条（:51-67 增 staff_shift_publish / staff_penalty_cap_adjust，后者 parallelLineCodes=hr_review,finance_review,legal_review）
       approve :214-218 DEPARTMENT_REVIEWS_PENDING
       :39-40 @Autowired(required=false) eventPublisher；:292 publishEvent(result)
       isMoneyAction :626-631 无 staff.payroll.*
  → U05 副本 StaffApprovalService
       submitShifts evaluate("staff.shift.publish")；submitPenaltyCap evaluate("staff.payroll.penalty_cap.adjust", request.parallelLines())
       @EventListener afterDecision 落地排班/上限（依赖 publishEvent）
  → U10 V407 INSERT approval_lines hr_review；staff_penalty_cap_revisions；penalty_cap_bps
  → main SupervisorReviewService 目录 10 条、无上述闸、无 eventPublisher 字段
  → 当前库 V410 无 hr_review / 无处罚上限表
  → HTML BSM L1443 / L1458 / PGM 1.7.1 逐级+平行线
```

本段测试**没有**实例化 `StaffApprovalService`，**没有**注入 `eventPublisher`，**没有**连 Flyway。绿测到不了落地与种子。

## 4. 三线

### 4.1 第一性原理

- **为何必须存在**：没有这两份回归，U07 钥源切换与 U09 两条员工策略在可失败断言上是空的。删除后 PGM-032/BSM-044 的设备闸、BSM L1443/L1458 的审批入口，只剩实现注释。答得上，不删文件。
- **声称是否执行**：副本树 64/64 执行且绿；叠 main 则声称的「工作手机验签通过」「目录 12」并不成立。见 CMD-UNPUB / CMD-OVERLAY。
- **未验证前提**：① 发布时生产代码与测试同 PR；② `hr_review` 种子随迁移 ≥V411 存在；③ `eventPublisher` 非 null 否则批准不落地。三前提均未在本段测试中被可失败断言锁住。

### 4.2 顺从式

假定实现意图正确，核对本段映射的 107 项：输入（设备头/审批请求）、状态（bound/pending_review）、输出（VerifiedTerminal / PageResponse）、错误码、权限（链上当前节点）、持久化（InMemory 仓库）、测试、文档。既有 40 个主管用例与 22 个设备用例在**未发布实现**上共同实现同一口径；**新增意图**（设备级工作手机钥、排班/上限策略）只有目录 contains 与一条弱异常断言，未与 U09 新分支、U05 落地、U10 种子共同实现。顺从式结论：既有闸覆盖充分，新增闸覆盖不足。

### 4.3 对抗式

先假定实现错误并构造反例（均在首轮正常发布路径，非极端巧合）：

| 反例 | 结果 | 收窄 |
|---|---|---|
| 只合 U12 测试到 445a49f4 | 64 run / 3 fail / 2 error | 首轮必红，GR-U12-001 |
| 工作手机签名用设备钥、main 读员工钥 | validSignature 变 credential_required；privatePhone 期望拒而 main 放行 | 首轮必红，GR-U12-002 |
| 目录断言 12、服务仍 10 | `isEqualTo(12)` 失败 | 首轮必红 |
| 目录 12 绿了但不跑 penalty_cap | master 先批不会被测到 DEPARTMENT_REVIEWS_PENDING | 首轮合 U09 仍绿，GR-U12-003 |
| 4 参构造、eventPublisher=null | 41/41 全绿，批准事件不发，U05 afterDecision 不跑 | 首轮只合测试+U09 服务仍绿，落地静默失败，GR-U12-003 |
| 无 X-Device-Id 自报 web | `nonDeviceBoundTypePassesThrough` 锁绿 | 与 U07-003 同根，GR-U12-005 |
| 并发双写 approve | InMemory `lockSupervisorReview` default 空；需 JDBC 测 | 非本段新增；要两三件不巧（真库+并发）才爆，按铁律 6 收窄为观察 |
| 整树覆盖 e28dad05 | 回退其它段 M009–M012 | 本段文件不是载体；发布说明禁止整树 |

## 5. 适用冻结判据逐项

判定：FAIL = 本段测试对该 ID 引入或锁住缺陷；COVERED = 本段有可失败断言且与 HTML 不冲突；N/A = 本段无实现面（测试文件不承载该业务），给原因。FAIL 不阻止其它 ID 继续审完。

| ID | 结论 | 依据 |
|---|---|---|
| GEN-001 | COVERED | 两文件有回归用途；新增用例是细化不是膨胀抽象 |
| GEN-002 | **FAIL GR-U12-001** | 叠 main 5 条红；副本 64/64 不能代替对 HEAD 的可执行正确性 |
| GEN-003 | **FAIL GR-U12-003/004** | 新闸无用例；新用例不断言错误码 |
| GEN-004 | **FAIL GR-U12-005** | 无设备号自报 web 被锁为通过；其余篡改/归属/body 摘要 COVERED |
| GEN-005 | COVERED | 主管链外/自审/跨租户/主会话既有用例仍在；新动作无授权链用例（并入 003） |
| GEN-006 | COVERED（既有） | 节点 CAS、整单归约既有；新动作落地不在本段测试 |
| GEN-007 | COVERED（既有） | evaluate/approve 幂等既有；lock 空实现观察 |
| GEN-008 | **FAIL GR-U12-001/002** | 测试夹具钥源/目录与 main 实现/库种子不一致 |
| GEN-009 | COVERED（既有） | 错误码既有覆盖充分；新路径弱断言并入 004 |
| GEN-010 | COVERED | 无密钥明文落盘；签名用临时 EC；PII 夹具为占位 `"ph"`/`"id"` 非生产秘密 |
| GEN-011 | COVERED（既有） | policies_viewed 等审计事件既有；publishEvent 无断言并入 003 |
| GEN-012 | N/A | 单测无无界批/连接；listPending(200) 既有上限 |
| GEN-013 | **FAIL GR-U12-001** | 发布叠法：只合测试则与 HEAD 编译后的运行行为不一致 |
| GEN-014 | N/A（本段无迁移文件） | 关联 U10 V407 未入当前库；本段测试不验证迁移。缺口并入 003 的种子可失败用例 |
| GEN-015 | N/A | 无 UI；方法名/注释英文不是展示文字 |
| GEN-016 | COVERED | 文件版本注释与 diff 可核对；不把 64/64 写成发布通过 |
| GEN-017 | COVERED | 本席按冲突→细化→技术性→添加；无把业务发明标技术性 |
| GEN-018 | COVERED | 结论落到 HTML 原文、命令输出、锁定义 |
| BEQR-063 | N/A | 高危酒吧动作不在本段测试动作码 |
| BEQR-074 | N/A | 隐私桌台不在本段 |
| BEQR-143…149 | N/A | 主体/职级枚举本段不定义；职级夹具沿用既有 rank 折算 |
| BEQR-150 | **FAIL GR-U12-005** | 十一端不同用途；无设备号自报 web 与「端由登记/绑定派生」冲突 |
| BEQR-151 | COVERED（既有） | specific_role 经角色持有审批权限既有用例 |
| BEQR-152 | COVERED（既有） | pendingList 项目隔离既有 |
| BEQR-153 | COVERED（既有） | 分级 enforce/grace/off 既有 |
| BEQR-154…157 | N/A | 项目/商户/合同/员工状态枚举本段不改 |
| BEQR-158 | 观察 | 夹具用内部码 `bound`/`returned`（系统内部标识符）；HTML 中文七态映射不在本段发明。不升冲突 |
| BEQR-159 | N/A | 权限状态枚举不在本段 |
| BEQR-166 | COVERED（既有） | 审批链模型既有覆盖；员工新动作未走链（003） |
| BEQR-189 | N/A | 管理费方式不在本段 |
| BSM-001 | COVERED | 测试服务治理/设备闸，属于系统该管的范围 |
| BSM-002 | N/A | 一笔客人业务走单不在本段 |
| BSM-003 | COVERED（既有） | 权限分岗、可删可审既有 |
| BSM-004 | N/A | 配置主数据删除审批不在本段测试 |
| BSM-005…007 | N/A | 引导/二维码/平台管理员全权限无本段实现面 |
| BSM-009 | N/A | 基础管理入口卡片 |
| BSM-012 | COVERED | 工作手机现场闸有测试 |
| BSM-013 | COVERED | kiosk 设备钥用例 |
| BSM-026 | N/A | 入口页面 |
| BSM-028 | COVERED（细化） | 排班发布审批属基础管理主要功能的下位；测试只锁目录名 |
| BSM-029 | N/A | 「和谁有关」连接叙述 |
| BSM-044 | **FAIL GR-U12-002/005** | 三端不同用途：钥源应对、无设备号自报 web 不应充工作手机旁路 |
| BSM-045…047 | COVERED / N/A | 工作手机核销相关闸 COVERED；POS 点单 N/A |
| BSM-048…050 | COVERED | kiosk 用例；一体机售票细节 N/A |
| BSM-072/074 | N/A | 商户侧全开不在本段 |
| BSM-114 | **FAIL GR-U12-005** | 不同人/端只做自己该做的；自报类型逃闸 |
| BSM-115 | COVERED | 设备闸连准入/审批的测试意图成立 |
| BSM-120 | N/A | 实施前输入项清单本段不承载 |
| PGM-001…007 | N/A | 治理底座/模块卡片，本段无实现面 |
| PGM-008 | COVERED | 员工和手机入口的测试网 |
| PGM-009 | COVERED（既有） | 职级/角色审批既有 |
| PGM-010 | COVERED | 终端开关分级 |
| PGM-011 | COVERED | 设备台账 bound 状态拒未绑定 |
| PGM-012…016 | N/A | 商户侧/开放接口/楼宇/应急无本段实现面；留痕复核既有审计事件 COVERED 于 PGM-014 既有用例 |
| PGM-014 | COVERED（既有） | 督导复核策略/待审/裁决留痕 |
| PGM-017…021 | N/A | 综合体集团细节 |
| PGM-026…027 | N/A | 商户主体功能/边界 |
| PGM-031 | N/A | 经销商资金 |
| PGM-032 | **FAIL GR-U12-002** | 人/私人手机/工作手机对应：测试把「私人钥不得替工作手机」锁在设备级公钥路径上，须与客户端同一把钥同 PR |
| PGM-033 | COVERED | 私人手机绑定/换机细节主要由 U05/U11 测；本段 privatePhone 用例咬「不能替工作手机钥」 |
| PGM-034 | COVERED | 工作手机验签 |
| PGM-035 | N/A | 提成发放不在本段 |
| PGM-036 | COVERED | 设备能不能用：unbound/unknown/owner |
| PGM-037…038 | COVERED / N/A | 领还状态 COVERED；现场业务连接叙述 N/A |
| PGM-040…044 | COVERED（既有） | 单一功能授权/职级/外包/桌台权限：本段不新增；既有角色闸仍在 |
| PGM-045…047 | COVERED | 终端开关怎么控制：enforce/grace/登记类型压过自报 |
| PGM-048 | N/A | 商户侧全开放 |
| PGM-060 | N/A | 会员生物信息 |
| PGM-065 | COVERED | 治理连接设备/审批 |
| PGM-066 | N/A | 待排期问题清单 |

无实现面的 ID 均因**本段是测试文件、不承载该业务模块的生产规则**；不把 HTML 沉默判冲突。

## 6. Findings

### GR-U12-001 — 只合本段测试叠当前 main，首轮测试红（发布叠法）

- 临时 ID：GR-U12-001
- 文件:行号：`DeviceCredentialVerifierTests.java:64,106,112-118,144,320`；`SupervisorReviewServiceTests.java:55-63`
- 完整原文（测试侧）：
  - Device `setUp`：`when(signingKeyStore.findActivePublicKey(DEVICE)).thenReturn(Optional.of(pubB64));`（不再桩 `findActiveCredential`）
  - Supervisor：`assertThat(page.total()).isEqualTo(12);` 且 `contains(..., "staff.shift.publish", "staff.payroll.penalty_cap.adjust")`
- 业务影响：按「尚未发布的这些」单独合进 445a49f4，CI/本地单测立即红；若整树覆盖则回退其它段已发布修复。本段两文件 BASE==HEAD，**不会**在这两路径上回退 M009–M012 文本，但会把未发布 U07/U09 口径的测试强加给仍是基线实现的 main。
- 可复现：CMD-OVERLAY（下节）。64 run，失败：`listPolicies…:58 expected 12L but was 10L`；`validSignature…:106 ApiException credential_required`；`bodySignedMatchesVerifies:320` 同；`invalidSignatureRejectedEvenInGrace:144` 未抛；`privatePhoneKey…:116` 未抛。
- 冻结判据：GEN-002、GEN-008、GEN-013
- 2.3 依据类型：类型 3（可复现命令输出）+ 对照 main 源码
- 四分类：**技术性**
- 无业务发明理由：叠法/符号面/测试夹具与 HEAD 实现不一致，不新造业务规则。同根 U05-008 / U07-005 / U09-001 / U10-001 / U11-001。
- 工程缺陷：是。建议登记：是（发布前须修）。
- 最小处置（写给发布者，本轮不修）：以 main `445a49f4` 为底重放本段 2 文件；与 U05+U06+U07+U09+U10+U11 **同一 PR**；迁移 ≥V411；禁止整树覆盖 `e28dad05`。

### GR-U12-002 — 新用例把工作手机验签锁到设备级公钥，与 main 员工 Keystore 路径对打

- 临时 ID：GR-U12-002
- 文件:行号：`DeviceCredentialVerifierTests.java:64`（默认桩）、`:112-118`（新用例全文）
- 完整原文：
```
    @Test
    void privatePhoneKeyCannotReplaceWorkPhoneKey() throws Exception {
        when(signingKeyStore.findActivePublicKey(DEVICE)).thenReturn(Optional.empty());
        when(staffRepo.findActiveCredential("sp1")).thenReturn(Optional.of(credential(pubB64)));
        assertThatThrownBy(() -> verifier("enforce").verify(signedRequest("POST", NOW, "n1", sign("POST", NOW, "n1"))))
                .isInstanceOf(ApiException.class);
    }
```
- 业务影响：未发布 U07 工作手机改读 `terminal_device_signing_keys`；main 仍读员工档案 Keystore。测试在未发布树上绿、在 main 上**方向相反**（main 会用员工钥验过）。客户端（U01/U02）与后端必须只认一把钥，否则现场工作手机核销会按错误钥失败或被私人钥冒充。
- 可复现：CMD-OVERLAY 中 `privatePhoneKeyCannotReplaceWorkPhoneKey` 未抛；CMD-VERIFIER-OVERLAY 把未发布 `DeviceCredentialVerifier.java` 叠到同一 tmp 后 **23/23 PASS**。main `:249-252` 仍 `return staffRepository.findActiveCredential(...)`。
- 冻结判据：GEN-002、GEN-003、GEN-008、PGM-032、BSM-044
- 2.3 依据类型：类型 1（PGM L572 / BSM L929）+ 类型 3（命令）
- 四分类：**技术性**（钥存放位置是实现选择；HTML 要求人/私人手机/工作手机能对应、三端不同用途，不规定表名。不把「设备级公钥」本身判添加或冲突。）
- 无业务发明理由：未新造「要不要验签」或「工作手机可否当私人手机」；只锁 U07 已有 hunk 的钥源。同根 U07-002 / U02-004。
- 工程缺陷：是。建议登记：是。
- 最小处置：与 U01/U02/U07 同一 PR 重放；测试与实现只认一把钥；补 `e.code()==DEVICE_CREDENTIAL_INVALID`（见 004）。

### GR-U12-003 — 目录 10→12 无新闸可失败用例（评估链 / 三审未齐 / 事件落地 / 时限）

- 临时 ID：GR-U12-003
- 文件:行号：`SupervisorReviewServiceTests.java:41-46`（4 参构造，无发布器）、`:55-63`（唯一新增断言）、全文 41 个 `@Test` 名称
- 完整原文：
```
        assertThat(page.total()).isEqualTo(12); // 含排班发布和普通扣除上限审批。
        assertThat(page.items())
                .extracting(SupervisorReviewPolicyRecord::actionType)
                .contains("manual_release", "bill_waiver", "contract_violation", "base_definition_violation",
                        "permission_grant_l1", "permission_grant_l2", "permission_grant_l3",
                        "staff.shift.publish", "staff.payroll.penalty_cap.adjust");
```
- 业务影响：U09 已把 `staff.shift.publish`（one_level_up）与 `staff.payroll.penalty_cap.adjust`（top_authority + hr/finance/legal）写入策略表，并在 approve 对 master 节点加 `DEPARTMENT_REVIEWS_PENDING`；落地靠 `eventPublisher.publishEvent` → U05 `StaffApprovalService.afterDecision`。本段 41/41 在未发布树上全绿且**不进入**这些分支：缺 `hr_review` 最终审批人不会红；master 先批不会红；排班链不会被生成断言；`eventPublisher==null` 时批准静默不落地也不会红。HTML L1443/L1458 的可执行网在本段是空的。同根已定稿 U09-004（明确点名本文件 `:55-63`）。
- 可复现：`rg -n "staff.shift.publish|penalty_cap|DEPARTMENT_REVIEWS|publishEvent|hr_review" SupervisorReviewServiceTests.java` 仅命中 `:63` contains 字符串。CMD-UNPUB 41/41 PASS。CMD-SVC-OVERLAY 叠未发布服务后仍 41/41 PASS。只读库无 `hr_review`。
- 冻结判据：GEN-003、GEN-002、GEN-011、BSM-028（排班发布下位）、PGM-014
- 2.3 依据类型：类型 1（BSM L1443/L1458）+ 类型 3
- 四分类：**技术性**
- 无业务发明理由：缺口是测试有效性，不新造审批档位。策略条目本身是 HTML 下位细化（见添加项节）。同根 U09-004 / U09-002（时限）/ U09-003（种子）/ U09-005（发布器）。
- 工程缺陷：是。建议登记：是。
- 最小处置：补可失败用例至少四条——① evaluate `staff.payroll.penalty_cap.adjust` 缺 `hr_review` 最终审批人 → `APPROVAL_LINE_FINAL_APPROVER_REQUIRED`；② 三平行线未齐时 master 先批 → `DEPARTMENT_REVIEWS_PENDING`；③ `staff.shift.publish` 走 one_level_up 链（上一级节点，申请人不能自批）；④ 构造注入 `ApplicationEventPublisher`（或必填构造），批准后断言 `publishEvent`。处罚上限时限按 U09-002 纳入涉钱 1 工作日并断言。与 U09/U05/U10 同 PR。

### GR-U12-004 — 本段唯一新增设备用例只 `isInstanceOf(ApiException)`，不断言错误码

- 临时 ID：GR-U12-004
- 文件:行号：`DeviceCredentialVerifierTests.java:116-117`
- 完整原文：`.isInstanceOf(ApiException.class);`（同文件其它拒绝路径均 `isInstanceOfSatisfying` + `e.code()==DEVICE_CREDENTIAL_INVALID`）
- 业务影响：绿测不能区分 `DEVICE_CREDENTIAL_INVALID` 与校验/鉴权其它码；与 U11-008 / U09-004「只 InstanceOf」同模式。本段仅此 1 处弱断言（既有 12 处 Satisfying 已带码）。
- 可复现：读 `:112-118`；对比 `:126-127` 等。
- 冻结判据：GEN-003、GEN-002
- 2.3 依据类型：类型 4（静态对照同文件既有模式）+ 类型 3
- 四分类：**技术性**
- 无业务发明理由：断言强度，无新业务规则。
- 工程缺陷：是。建议登记：是。
- 最小处置：改为 `isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID"))`；建议同时断言中文消息不含未翻译英文 reason（U07 既有拼接，本段可不升独立冲突）。

### GR-U12-005 — 既有用例把「无设备号自报 web」在 enforce 下通过锁绿（与 U07-003 同根）

- 临时 ID：GR-U12-005
- 文件:行号：`DeviceCredentialVerifierTests.java:94-100`（本段未改字节，全量审仍在）
- 完整原文：
```
    void nonDeviceBoundTypePassesThrough() {
        MockHttpServletRequest r = new MockHttpServletRequest("POST", "/api/v1/services/orders");
        r.addHeader("X-Terminal-Type", "web");
        TerminalIdentityHolder.VerifiedTerminal vt = verifier("enforce").verify(r);
        assertThat(vt.terminalType()).isEqualTo("web");
        assertThat(vt.verified()).isFalse();
    }
```
  未发布实现 `DeviceCredentialVerifier.java:138-148/:174-186`：无 `X-Device-Id` 时 `terminalType = claimed`，web/app 不进设备闸。`:247-254` `webWithCookieAllowedUnderEnforce` 同样无设备号。
- 业务影响：现场写请求不带设备号、自报 web 即可在 enforce 下绕过工作手机/平板/kiosk 验签。与 BSM L929 三端不同用途、PGM L572 人机对应、BEQR-150 十一端、BSM-114 只做自己该做的冲突。U01 客户端 `applyHeaders` 恒报终端类型。同根已定稿 U01-001 / U07-003。修实现时若不改本用例，不是锁绿冲突就是修完变红。
- 可复现：CMD-UNPUB 该用例 PASS；读 U07 副本 `:138-186`。
- 冻结判据：BEQR-150、BSM-044、BSM-114、PGM-032、PGM-045、GEN-004、GEN-005
- 2.3 依据类型：类型 1（BSM L929 / PGM L572 / BEQR L233）+ 类型 3
- 四分类：**冲突**（测试把与 HTML 明文相矛盾的行为写成期望）
- 工程缺陷：是。建议登记：是。
- 最小处置：随 U01-001/U07-003——身份由绑定/登记设备类型派生；无设备号的**写**请求 fail-close；改本用例为可失败断言（enforce 下无设备号写请求拒）。Cookie 会话的后台 web 若仍要放行，须有不可伪造的会话证明，不能只靠自报头。

## 7. 命令证据

环境：隔离 `/tmp`，`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`（本机默认 `JAVA_HOME` 指向无 `javac` 的 `java-21-openjdk-amd64`，maven-compiler `release 21 not supported`——属本席验证环境，**不是产品 finding**）。未 export `COMPLEX_DATABASE_*`。未 `source` env。未写 `complex_test*`。

**CMD-LOCK**  
`sha256sum docs/audit-2026-09/plan/criteria-lock.md`  
→ `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` EXIT 0

**CMD-HASH-U12**  
`sha256sum` / `wc -l` / `wc -c` / `git hash-object` 两文件 → 与清单及树 blob **2/2 MATCH**（见 §1 表）。EXIT 0

**CMD-DIFF**  
`git diff --numstat 242f487233d028173cb9198eecd079f5d877405c e28dad059b2c7e08bcc95a8db18552d3f76edf4b -- <两路径>`  
→ `11\t2\t…DeviceCredentialVerifierTests.java`；`4\t2\t…SupervisorReviewServiceTests.java`  
`git diff --shortstat 242f4872 e28dad05` → `76 files changed, 4745 insertions(+), 1084 deletions(-)`  
`git log --oneline 242f4872..HEAD -- <两路径>` → 空  
`git cat-file -t e28dad05…` = tree；`03acdb98…` fatal

**CMD-UNPUB**  
`/tmp/u12_grok_unpublished/backend`（`cp -a` 未发布 backend，不写副本目录）  
`mvn -o -Dtest=DeviceCredentialVerifierTests,SupervisorReviewServiceTests test`  
→ Tests run: **64**, Failures: 0, Errors: 0, Skipped: 0；Device 23/23 0.587s；Supervisor 41/41 0.570s；BUILD SUCCESS；Total time 9.867 s。EXIT 0

**CMD-OVERLAY**  
`/tmp/u12_grok_main_plus_u12tests/backend` = HEAD backend + 仅覆盖 2 个 U12 测试  
同 mvn 命令  
→ Tests run: **64**, Failures: **3**, Errors: **2**  
- `SupervisorReviewServiceTests.listPoliciesReturnsBlockingCatalogAndWritesAudit:58 expected: 12L but was: 10L`  
- `DeviceCredentialVerifierTests.invalidSignatureRejectedEvenInGrace:144 Expecting code to raise a throwable`  
- `DeviceCredentialVerifierTests.privatePhoneKeyCannotReplaceWorkPhoneKey:116 Expecting code to raise a throwable`  
- `DeviceCredentialVerifierTests.validSignatureDerivesTrustedTypeVerified:106 ApiException 设备凭据校验未通过：credential_required`  
- `DeviceCredentialVerifierTests.bodySignedMatchesVerifies:320` 同上  
BUILD FAILURE EXIT 1

**CMD-VERIFIER-OVERLAY**  
同一 tmp 再覆盖未发布 `DeviceCredentialVerifier.java`  
`mvn -o -Dtest=DeviceCredentialVerifierTests test` → **23/23 PASS** EXIT 0

**CMD-SVC-OVERLAY**  
同一 tmp 再覆盖未发布 `SupervisorReviewService.java`  
`mvn -o -Dtest=SupervisorReviewServiceTests test` → **41/41 PASS** EXIT 0

**CMD-DB-RO**（SELECT only，`psql -U complex -d complex_test`）  
flyway 成功版本 410,399,398,397,396；`approval_lines` 有 finance/legal/tax、**无 hr_review**；`staff_penalty_cap_revisions` 表 0；`penalty_cap_bps` 列 0。

未运行、不声称通过：全量后端测试、Flyway migrate、会清理共享库的脚本、对 `complex_test*` 的任何写。

## 8. 观察（无新根因，不漏并）

1. 本段两文件 BASE blob == HEAD blob；M009–M012 未改这两测试路径。分叉在测试所锁的 U07/U09 生产符号，不在检查点回退这两文件。
2. 未发布 commit 对象本仓不存在；树对象可解析。不得把副本称为已发布修复。
3. `import static org.mockito.ArgumentMatchers.any` 未使用。可删，不升缺陷。
4. 夹具设备状态 `bound`/`returned` 为内部标识符（铁律 7 允许）；HTML BEQR-158 中文七态的映射表不在本段。不把沉默当冲突。
5. `InMemorySupervisorReviewRepository` 不覆盖 `lockSupervisorReview`（接口 default 空）。并发双写要 JDBC。既有 `ApprovalChainNodeConcurrencyJdbcTests` 在 main、不在本段。铁律 6：非首轮单测路径，观察。
6. `isAlwaysBlocked` 不含两条员工动作；`minAmountCent=0` 使默认金额仍 blocked。无测试锁此耦合。
7. 未发布 `reject` 把英文 reason 拼进中文消息（U07 已登记 main 既有拼接）。本段测试不断言 message 文本，不新开。
8. 整树覆盖会回退其它段 M009–M012。本段不是载体。发布说明禁止整树。
9. `webWithBearerRejectedUnderEnforceButAllowedUnderGrace` 锁 web enforce 拒 Bearer、grace 放行——终端开关细化，与 005 的「无设备号自报即 web」不同。不并入 005。
10. 本席第一次 `mvn` 因 JAVA_HOME 无 javac 失败；改 Temurin 21 后复跑。属验证环境，不是产品缺陷。
11. U09-004 已点名本文件 `:55-63`；本席独立复现并升为 GR-U12-003（本段为测试所有者）。不重复开 2.2。
12. `StaffProfileRecord` 15 参紧凑构造在未发布与 main 均存在；夹具能编过。未发布额外 `employmentStatus` 字段由紧凑构造默认 `pending_entry`，本段不断言五态（BEQR-157），N/A。
13. 本段无 OpenAPI/SQL/UI 文件。U14/U15 契约不在范围。

## 9. 添加项清单（供主调度 2.2；本席不代答）

**空。** 目录两条动作码与「私人钥不得替工作手机」均能指向上位 HTML（§1.2 / 添加项节）。无 LOCK-AMEND 候选。

## 10. 结论

**FAIL**

没有有条件通过。两文件全量三线已做；107 项已逐项给结论；声称均有命令或 HTML 原文；无未答 2.2；Grok 哈希行有效。存在未定稿缺陷 GR-U12-001…005。席甲自审票不得覆盖本席 FAIL。

发布者最小打包（本轮不修）：以 main `445a49f4` 为底，U05+U06+U07+U09+U10+U11+U12 同一 PR 重放；迁移 ≥V411；禁止整树覆盖；设备测与实现只认一把钥；主管测补四条新闸可失败用例并注入发布器；无设备号写请求随 U07-003 fail-close 并改期望。

## U12 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 50166 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok 固定外审席乙（grok-4.6）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02 §7/§11；criteria-lock V0.11；modules.md V0.04 附录「未发布材料第 12 段：设备与主管审批测试」（2 文件 1519 行，main 主归属 M066+M042；适用判据＝M042 ∪ M066 映射行并集 107 项）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`。
- 审计对象仍是只读输入副本 `.scratch/module-recheck-20260908/unpublished-03acdb98`（树 `e28dad059b2c7e08bcc95a8db18552d3f76edf4b`，基线 `242f487233d028173cb9198eecd079f5d877405c`），不是 main。三份 HTML 取 main 正本。未发布提交对象 `03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb` 本仓 `git cat-file -t` 仍不可解析。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B2 核实结论。
- 本席 B2-r1：**PASS**（001–006 逐 ID 全 PASS；添加 0、不发新 2.2；发布前须修「是」=001–005，「否」=006）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。判据总数 401；M042 映射行 L6421 共 87 ID，M066 映射行 L6445 共 50 ID，并集 107，MISSING=[] EXTRA=[]。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要核对只读（输入哈希、blob、numstat、副本 2 测试原文、main 同名测试与验签器/审批服务、HTML L1443/L1458/L929/L572/BEQR L233、U09-004/U07-003/U01「main 既有」定稿原文、`questions/U04-001.md` 答复首行「删除」）；未重跑 overlay/mvn、未装依赖、未碰 `complex_test*`。追加稿仅经 `/tmp/u12-b2-append.md` 中转，落盘后删除。

此票只判断 findings.md V1.66 顶部「当前 U12 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、「发布前须修」是/否与最小处置。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。材料未发布，定稿后状态应记 REVIEWED-FAIL（有已定稿缺陷，处置写给发布者），不做 B3。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/U12-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 50166 | `583d81b933b1e9d70fc6edfcc4ec7e11a4584e7e14222b98017c09d558cc3fb1` |
| `docs/audit-2026-09/modules/U12-b1-self-raw.md` | V0.01 | 16586 | `e2b995457e8ba00e406d42c4e447bd98ebfcdae0ea952609f54b9cfd849a94ac` |
| `docs/audit-2026-09/findings.md` | V1.66 | 648074 | `f04f0cc36d992725cb4f13c59bbbb0d4d597eb5281e8a0615494807cce5a1434` |
| `docs/audit-2026-09/ledger.md`（只读） | V2.77 | 506234 | `6ef34074b3121b472d13aa9cb06b9a04bb449878851631e9cf1442927e40e9ee` |
| `docs/audit-2026-09/questions/U04-001.md`（只读，沿用） | V0.01 | 2282 | `5a9e9dab904fe9b82ccb2e414ae07515ea7192c7ed404685c31828a126c2b3d1`；答复首行精确「删除」 |

候选形态：V1.66 顶部 U12-001…006；questions 0；添加 0；无 HTML 修改申请。U12 ledger 行 IN_PROGRESS。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

HTML 正本哈希与 B1 一致：BEQR `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66`、BSM `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa`、PGM `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c`。副本本段 2/2 sha256/行/字节与树 `e28dad05` blob 全等（本轮复算：Device 392/19456/`09abf3c2…c9cb` blob `98b57af6`；Supervisor 1127/76894/`bdf54d41…0972` blob `95b18586`）。`git diff --numstat 242f4872 e28dad05 --` 本 2 路径 = `11 2` / `4 2`。全树 `git diff --shortstat 242f4872 e28dad05` 本轮未重跑，沿用 B1 已核 `76 files, +4745/−1084`。两文件 BASE blob == HEAD blob（`git diff 242f4872 HEAD --` 两路径空）。`git cat-file -t e28dad05…` = tree；`03acdb98…` fatal。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-arc / using-superpowers | 先核技能；用户/提示词覆盖技能本体；本节点为指定 B2 归并，不改走 Arc audit |
| receiving-code-review | 先核 HTML/测试原文/main 同名文件/已定稿 U01/U07/U09 再接受或反驳候选单元格；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：(a) U12-005 须修是与 U01「main 既有」是否相容、(b) 002 技术性是否与 GR-002 一致、(c) 003 四条是否只引用 U09-004、(d) 006 是否写错 §8 第 5/9/11 条 |
| verification-before-completion | 声称前先跑 sha256/git rev-parse/git cat-file/diff/原文行号；本票不声称本轮新跑 64/64 或 overlay |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流（本段测试 → U07/U09 副本 → main 445a49f4 → HTML）；本轮不建 overlay |
| ponytail-review | 只评最小删除/收窄，不落地。002 只认一把钥；003 只补四条可失败用例；004 只补 `e.code()`；005 只改期望随 U07-003 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到独立技能**；未冒充调用 |
| Arc audit | 会派子代理；与双席冲突；**未用** |

内部 grilling（自答，未问操作员）：

1. `:94-100` 方法体 main 既有、diff 未触及，候选仍记冲突/须修是，是否推翻 U01 口径？否。见 (a)。
2. 钥源测试面能否升冲突或添加？不能。见 (b)。
3. 四条最小用例有无发明第四条审批线或新错误码？无。见 (c)。
4. 006 第 5/9/11 条是否把 JDBC 并发、webWithBearer、U09-004 点名写错？未写错。见 (d)。

### 完整差集（GR-001…005、§8 观察 1–13、席甲 CX-001、OBS-1…3）

漏并：无。两席 B1 全部临时 ID / 观察仍落在 001–006。未把本席 finding 降观察。席甲独有项无超出 CX-U12-001 与 OBS-1…3 及 §4 定性。席甲 B1 漏判四处（只合本段叠 main 首轮红未立项、钥源测试面记 OBS-2、`:117` 弱断言、`:94-100` 锁绿）并把发布器 null 记 OBS-3——候选已勘误接受本席五项须修，本席独立复核后维持。

| 临时 ID | 落入正式 ID | 是否改写本席原意 |
|---|---|---|
| GR-U12-001 | U12-001 | 否：技术性叠法；重放、同 PR、≥V411、禁整树 |
| GR-U12-002 | U12-002 | 否：技术性；一把钥；不把设备级公钥判添加或冲突 |
| GR-U12-003 | U12-003 | 否：技术性；四条可失败用例 + 时限引用 U09-002；同根 U09-004 |
| GR-U12-004 | U12-004 | 否：技术性；补 `DEVICE_CREDENTIAL_INVALID` |
| GR-U12-005 | U12-005 | 否：冲突；无设备号写请求 fail-close；同根 U07-003/U01-001 |
| §8 1–13 | U12-006 | 「含」为闭集转述；第 5/9/11 条未把观察升冲突，也未把 finding 降观察（见 (d)） |
| 席甲 CX-U12-001 | U12-003 | 同向；席甲只立此一条，候选未把 001/002/004/005 并进 CX-001 |
| 席甲 §4 | U12-001 | 席甲只定性未立项；候选升队与 GR-001 同向 |
| 席甲 OBS-2 | U12-002 | 升队勘误，不改写本席「技术性/须修」 |
| 席甲 OBS-3 | U12-003 | 并入 003 处置（注入发布器），不另开 |
| 席甲 OBS-1 | U12-006 | 维持观察：U11 DB 回归不替代 003 |

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/须修 | 本席票 |
|---|---|---|---|
| U12-001 | GR-U12-001 + 席甲 §4 | 技术性（发布叠法，同根 U05-008/U07-005/U09-001/U10-001/U11-001）/是/是 | **PASS（技术性叠法/是/是；只引用既有重放、≥V411、同 PR）** |
| U12-002 | GR-U12-002 + 席甲 OBS-2 | 技术性（同根 U07-002/U02-004）/是/是 | **PASS（技术性/是/是；钥源为实现选择，不升冲突/添加）** |
| U12-003 | GR-U12-003 + 席甲 CX-U12-001 | 技术性（U09-004 定稿的测试面；同根 U09-002/003/005）/是/是 | **PASS（技术性/是/是；四条只引用不发明）** |
| U12-004 | GR-U12-004（席甲漏） | 技术性（同模式 U11-008）/是/是 | **PASS（技术性/是/是）** |
| U12-005 | GR-U12-005（席甲漏） | 冲突（U07-003 定稿的测试面；用例行 main 既有但同 PR 必改）/是/是 | **PASS（冲突/是/是；与 U01「main 既有」相容，见 (a)）** |
| U12-006 | 双方观察归并 | 登记/否/否 | **PASS（登记/否/否；第 5/9/11 条未写错 §8）** |

技术性入队 4（001 叠法、002 钥源测试面、003 新闸零覆盖、004 弱断言）。冲突入队 1（005 无设备号自报放行锁绿）。登记观察 1（006）。添加 0。questions 0。无 HTML 修改申请。

发布前须修「是」=001–005（5）；「否」=006（1）。与候选汇总一致。席甲 B1 票 FAIL 但漏 001/002/004/005，属低估；候选接受本席五项须修并勘误，本席独立复核后维持。

### 重点 (a) U12-005 用例行 main 既有，候选仍记「冲突/发布前须修是」——与 U01「main 既有→否、归 main 模块」是否相容 — **PASS（相容；不是口径例外，是同 PR 测试面）**

独立核，不采信席甲「同 PR 必改故记须修是」六个字为己证。

U01 定稿口径原文：「代码在 main HEAD 同位置存在且本材料 diff 未触及者，登记为「冲突/技术性（main 既有）」、发布前须修「否」、处置归其 main 主归属模块……；本材料不得加重。」

本轮原文：

1. 副本 `DeviceCredentialVerifierTests.java:93-100` `nonDeviceBoundTypePassesThrough`：无 `X-Device-Id`、自报 `X-Terminal-Type: web`，enforce 下 `verify` 通过且 `verified()==false`。方法体与 HEAD 同名方法逐字节相同。HEAD 同行号为 `:92-99`（副本多了版本注释两行，方法体下移 1 行）。两测试文件 BASE blob == HEAD blob；本材料 diff 未改该方法（diff 只改版本行、`setUp` 钥桩、新增 `privatePhoneKeyCannotReplaceWorkPhoneKey`）。
2. 未发布验签器 `:144-148` 无设备号则 `terminalType = claimed`；`:174-186` web/app 不进设备闸。main 同方法仍此口径。U07-003 已定稿冲突/须修是：无设备号不得信自报；写请求 fail-close。
3. U07-006 才是 U01 口径的正例：英文 reason 拼接在基线同位置、本材料未改、落地其它须修不会把它打红 → 须修否、归 M063。
4. U01-010 首页按钮 main 既有须修否：无测试锁，推迟到 M375 不会让本批 PR 的 CI 变红。
5. 本条不是 4。U07-003 最小处置改验签器之后，本方法从绿变红（期望「通过」vs 实现 fail-close）。本文件在 U12 发布集、与 U07 同一 PR。推迟到 M066 等于：按台账留下旧期望、按 U07-003 改实现、CI 红。这不是「main 既有缺陷交给主归属模块以后再修」，是**已经定稿须修的测试面**。

U01 口径管的是「本材料不加重、发布时不必改的既有行」。发布前须修问的是：不改这一处，本批已定稿须修能不能绿着落地。U12-005 做不到。与 U09-004 把测试缺口指派给本文件同一逻辑：测试所有者在发布 PR 里改期望，不另开 M066 审计轮。

「本材料不得加重」：未发布验签器仍放行无设备号自报 web，测试仍锁绿，相对 main **没有加重缺陷本身**。须修「是」来自与 U07-003 同 PR 的测试耦合，不是把 main 旧债算进本材料。

分类维持**冲突**（测试把与 HTML BSM L929 / PGM L572 / BEQR L233 明文相反的行为写成期望），不改成「冲突（main 既有）」。工程缺陷是。发布前须修是。归 M066 的「否」不适用。

票 **PASS**。

### 重点 (b) U12-002 记技术性而非冲突或添加 — **PASS（与 GR-U12-002 一致）**

独立核：

- 副本 `:64` `when(signingKeyStore.findActivePublicKey(DEVICE)).thenReturn(Optional.of(pubB64))`，不再桩 `findActiveCredential`。
- `:112-118` 设备钥空、员工凭证在 → 期望抛 `ApiException`。
- main 验签器 `:249-252` 仍 `staffRepository.findActiveCredential(profile.id()).devicePublicKey()`；未发布 `:251` `deviceSigningKeyStore.findActivePublicKey(deviceId)`。同一用例在 main 上方向相反（本席 B1 CMD-OVERLAY：该用例未抛；叠未发布验签器后 23/23）。
- HTML PGM L572「人、私人手机、工作手机要能对应起来」、BSM L929 三端不同用途：**不规定钥表名**。U07-002 已定稿技术性（一把钥）；U02-004 为客户端契约面。

GEN-017 顺序：先冲突。钥存放位置不是与 HTML 原文相反的业务规则，不能升冲突。也不是 HTML 沉默处的第二轴，不能升添加。技术性成立。无业务发明。同根 U07-002/U02-004。

票 **PASS**。

### 重点 (c) U12-003 四条最小用例与 U09-004 定稿处置 — **PASS（只引用不发明）**

U09-004 定稿处置原文：U12 补可失败用例（缺 hr_review 最终审批人；master 先批 → DEPARTMENT_REVIEWS_PENDING；排班 one_level_up 链；注入 ApplicationEventPublisher 断言发布）。

候选四条：① 缺 `hr_review` 最终审批人 → `APPROVAL_LINE_FINAL_APPROVER_REQUIRED`；② 三平行线未齐 master 先批 → `DEPARTMENT_REVIEWS_PENDING`；③ `staff.shift.publish` one_level_up 链（申请人不能自批）；④ 注入 `ApplicationEventPublisher` 断言终态发布；时限按 U09-002 涉钱 1 日。

本轮原文：

- 未发布服务 `:53-55` 处罚上限平行线 `hr_review,finance_review,legal_review`；`:162-169` `requireDefaultLines` 已抛 `APPROVAL_LINE_FINAL_APPROVER_REQUIRED`（既有税费用例 `:447`/`:922` 已用该码）。点名错误码是既有符号，不是新码。
- `:214-218` master 先批且平行线未齐 → `DEPARTMENT_REVIEWS_PENDING`。本文件 0 命中该字符串。
- `:52` 排班 `one_level_up`；`:204-207` 发起人自批 → `SUPERVISOR_SELF_REVIEW_FORBIDDEN`（PGM 1.7.1 既有闸，既有用例已覆盖其它动作）。「申请人不能自批」是把既有闸写进新动作链，不发明第四条线。
- `:39-40` `eventPublisher` `required=false`；测试 4 参构造不注入；`:292` null 则不发。U09-005 已定稿必填注入。
- 时限：`isMoneyAction` `:626-631` 无 `staff.payroll.*`。U09-002 已定稿纳入涉钱 1 日。不是 U09-004 的第五条新规则，是引用并列定稿项。

与 GR-U12-003 最小处置同句。不发明档位、不发明第四条平行线、不改 BSM L1443/L1458。U09-004 里「U11 补错误码」已由 U11-008 定稿，本条不重复。

票 **PASS**。

### 重点 (d) U12-006 是否写错本席 §8 — **PASS（第 5/9/11 条未写错；无应升队被降观察项）**

独立核本席 §8 原文，不采信候选转述为己证。

**第 5 条。** 本席原文：「`InMemorySupervisorReviewRepository` 不覆盖 `lockSupervisorReview`（接口 default 空）。并发双写要 JDBC。既有 `ApprovalChainNodeConcurrencyJdbcTests` 在 main、不在本段。」本轮：测试内嵌仓储至 `:1126` 无 `lockSupervisorReview` 方法；接口 `:102-104` `default void lockSupervisorReview` 空体；JDBC 测在 `governance/chain/ApprovalChainNodeConcurrencyJdbcTests.java`（main 有，不在本段 2 文件）。候选「并发在 main JDBC 测」未写成「本段已覆盖」或升冲突。铁律 6 收窄为观察仍成立。

**第 9 条。** 本席原文：`webWithBearerRejectedUnderEnforceButAllowedUnderGrace` 锁 web enforce 拒 Bearer、grace 放行，与 005「无设备号自报即 web」不同，不并入 005。本轮副本 `:234-244` 带 `Authorization: Bearer`、按 web 模式分 enforce/grace；`:94-100` 无 Authorization、默认 enforce、无设备号。两用例不同闸。候选「与 005 不同不并」未把 Cookie/Bearer 会话并进 005 的 fail-close 写请求。`:247-254` `webWithCookieAllowedUnderEnforce` 仍在 005 事实格作「同无设备号」对照，不升独立 ID——与 GR-005 原文一致。

**第 11 条。** 本席原文：「U09-004 已点名本文件 `:55-63`；本席独立复现并升为 GR-U12-003（本段为测试所有者）。不重复开 2.2。」候选把该交叉引用留在 006，finding 本体在 003，未把已升队项降回观察，也未再开 2.2。

**应升队而被降观察？无。** GR-001…005 全部入 001–005 且须修是。席甲 OBS-1（U11 `penaltyCapNeedsThreeDepartmentReviews…` 为 DB 回归且只断言类型）本席 B1 未编号，与「41/41 不进新分支」同向，维持观察合法。

票 **PASS**。

### 001 独立裁定（技术性叠法／须修是）

两文件 BASE==HEAD。只把本段 2 个测试叠到 445a49f4：本席 B1 `/tmp/u12_grok_main_plus_u12tests` 64 run / 3 fail / 2 error（`:58` expected 12 but was 10；`:106/:320` `credential_required`；`:144/:116` 未抛）。整树覆盖仍回退他段 M009–M012（本段两路径不是载体）。同根 U05-008/U07-005/U09-001/U10-001/U11-001。最小处置只引用既有重放、与 U05–U11 同 PR、迁移 ≥V411、禁止整树。候选同 PR 列补 U01/U02，与 GR-002/U07-002 并集一致，不发明业务。票 **PASS**。

### 002 独立裁定（技术性／须修是；同根 U07-002）

见 (b)。工程缺陷是。发布前须修是。测试与实现只认一把钥，随 U07-002；弱断言并 004。票 **PASS**。

### 003 独立裁定（技术性／须修是；U09-004 测试面）

见 (c)。副本 `:41-46` 4 参构造无发布器；`:55-63` 唯一新增断言（目录 12 + 两动作码）；全文 `DEPARTMENT_REVIEWS_PENDING`/`publishEvent`/`hr_review` 0 命中。41/41 不进入 U09 新分支。HTML BSM L1443/L1458。票 **PASS**。

### 004 独立裁定（技术性／须修是；同模式 U11-008）

副本 `:116-117` 仅 `.isInstanceOf(ApiException.class)`。本轮复算同文件 `isInstanceOfSatisfying`＝**13**、裸 `isInstanceOf(ApiException.class)`＝**1**（`:117`）。GR-004 写「12 处」为 B1 笔误，以 13 为准；候选 13 正确。改为 `isInstanceOfSatisfying` + `e.code()==DEVICE_CREDENTIAL_INVALID`。不改业务口径。票 **PASS**。

### 005 独立裁定（冲突／须修是；同根 U07-003）

见 (a)。随 U01-001/U07-003：身份由绑定/登记设备类型派生；无设备号的**写**请求 fail-close；改本用例为可失败断言。Cookie 会话后台 web 若仍要放行，须有不可伪造会话证明，不能只靠自报头——此句已在 GR-005，不是新业务。票 **PASS**。

### 006 独立裁定（登记／须修否）

见 (d)。双方观察归并，工程缺陷否，发布前须修否。票 **PASS**。

### 最小修正意见（非阻断，不改票）

1. U12-002 冻结判据格可补本段映射内的 BSM-044（本席 GR-002 有；候选只留 PGM-032）。分类仍为技术性。
2. U12-003 冻结判据格可补 BSM-028（排班发布下位，本席 GR-003 有）。不改四条用例。
3. U12-005 事实格钉「方法体与 main/基线逐字节相同；副本行号 `:94-100`，HEAD `:93-99`」，避免读成「副本 `:94-100` 与 main `:94-100` 行对齐」。分类/须修不变。
4. U12-001 同 PR 以 U01/U02/U05/U06/U07/U09/U10/U11 并集为准（GR-001 原文写 U05–U11，GR-002 写 U01/U02/U07）。
5. 006 「含」对席甲 OBS-1 维持观察；本席 §8.3 未使用 `any(`（import 在、调用 0）、§8.12 15 参构造未单独展开，定稿不得当成「Grok 无这两条」或升格入队。
6. GR-004「12 处 Satisfying」以本轮 13 为准，已由候选勘误，不必回改 B1 正文。

全批差集无漏并。四分类与 GR-001…005 一致。001 叠法成立且只引用既有门禁。002 技术性与 GR-002 一致。003 四条只引用 U09-004/002/005。005 须修是与 U01「main 既有」相容。006 未写错 §8 第 5/9/11 条。逐 ID 001–006 全 **PASS**。本席 B2-r1 **PASS**。未经双 PASS 不得把候选当定稿，更不称 U12 或 main 模块 PASS。步骤仍 B2；U12 IN_PROGRESS；材料未发布，不做 B3。
