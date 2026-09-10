CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U07 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 只读全量三线（未发布材料第 7 段；不做 B3/B4/B5；处置写给发布者，本轮不修源）
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 段：U07 — 未发布材料第 7 段「设备与接口访问联动」
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 末尾附录；`plan/runbook.md` V0.02 §11；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 审计对象（不得冒称 main 或已发布修复）：
  - 未发布提交：`03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb`（本仓 `git cat-file -t` **无此 commit 对象**）
  - 未发布树：`e28dad059b2c7e08bcc95a8db18552d3f76edf4b`（`git cat-file -t` = **tree**，可解析）
  - 发布基线：`242f487233d028173cb9198eecd079f5d877405c`（commit，可解析）
  - 只读输入根：`/home/james/complex-management/.scratch/module-recheck-20260908/unpublished-03acdb98`
  - 清单：`.scratch/module-recheck-20260908/unpublished-03acdb98-input.json` V0.01，76 文件
- 本段：3 文件，物理行 1585，标记 NORMAL；main 主归属 M396（SecurityConfig）+ M063（DeviceCredentialVerifier）+ M064（TerminalDeviceService）
- 冻结判据：M063 ∪ M064 ∪ M396 映射行并集 71 项（python 集合相对开工提示 MISSING=[] EXTRA=[]；M063=M064=50 项逐字相同，M396=52 项）
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 且发布前须修：GR-U07-001 … GR-U07-006
- 添加项（B1 只登记，不代用户回答 2.2）：无
- 同根已定稿、不新开 2.2：GR-U07-001＝U05-003/U01-007/U02-002；GR-U07-002＝U02-004；GR-U07-003＝U01-001/U02-001；GR-U07-006＝U05-008/U06-007
- 只登记、建议队列「否」：观察见 §8
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做写操作
- 本报告不表示全仓、main 模块或发布门禁通过。M001–M012 检查点已合入当前 HEAD（PR #428/#429）。原 322/471 模块结论不继承到本未发布段。不得把本副本冒称 main 源码或已发布修复。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md`（1872 bytes，SHA256 `fa5c1e5ee76b1c8f1ae56101f52c9e239de75d5c578adc61227b92d10b7e52ef`） | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md`（本体转发 grilling） | 内部 sharpen |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md`（SHA256 `b9339b09ee3980808d8c5a35c7251b891b8b1e0036ec4ca37812b976ebddf6b6`） | `/tmp/u07-b1-grok` 把本段 3 文件盖到 main 后端源码树的离线 `mvn -o -Dtest=… test`；不改生产代码 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md`（SHA256 `808fc5717aa88ad65efff312b11c186294d3e6ee301afb584e2f86599b137787`） | 只借读错误→复现→追数据流（本段 3 文件 → U05 StaffController/StaffRegistrationService → U12 DeviceCredentialVerifierTests → interceptor/NonceStore → main 对照 → HTML） |
| verification-before-completion | superpowers 同名（SHA256 `2befe7fc55bcadaa3d97dd9e8efeb633d2561c0ebe74c5a8b17c4d9e7e4520b3`） | 先跑命令再声称；未跑的后端集成 / 共享库测试不写通过 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md`（SHA256 `40df33b58fc6ef889b93585733feb9566b76e9586efa7f376785c1e995197ac0`） | 只列可删复杂度（`throw unreachable()` 死代码、Mode 非法回落），不落地 |
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

1. 自助注册必须公开吗？HTML PGM-033 公司自有员工在私人手机 App 注册、审批前无账号 → 允许无会话；但同条要求安装时绑定该手机专属设备标识。本段只做了 permitAll，没做设备标识。
2. 工作手机改读设备级公钥是否与 HTML 冲突？公司工作手机≠私人手机 Keystore，可作技术选型；前提是领用时把人绑到设备。本段没把员工写进 bind。
3. 无 `X-Device-Id` 时用自报类型，首轮会不会发生？U01/U02 客户端对公开注册自报 `app`、其余恒报 `staff_work_phone`。会发生。
4. 单独合入本段 3 文件会不会盖掉 M009–M012？这 3 个同名文件 main blob＝基线，正文没有 M009–M012 补丁可盖；但钥源/公开路径会与当前 main 测试和 verifier 行为对打，且同批 76 文件里的员工/商户文件会盖 M009–M012。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`，属正常）
- `criteria-lock.md` 版本 V0.11；判据总数 401
- 映射行：`M063 →` L6442、`M064 →` L6443（两行闭集逐字相同 50 项）、`M396 →` L6775（52 项）；python 并集 71，相对开工提示 MISSING=[] EXTRA=[]

HEAD（独立实算）：

- `git rev-parse HEAD` = `git rev-parse origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`
- `git log -1 --oneline`：`445a49f4 Merge pull request #429 from jxh1979/audit/20260909/M011-M012-checkpoint`
- 本席对照 main 已修项时以 **当前 HEAD=445a49f4** 为准（含 M001–M012）
- 本席未设 `GIT_INDEX_FILE`，未动主索引

ledger 只读核验（不改）：V2.61，482409 bytes，SHA256 `65e4f1170b9c53bbdc7d3367f68f8f22c6f604e53d379adbebb411127775ad41`（与开工提示一致）。findings V1.55，596623 bytes，SHA256 `0f45cbcb85c7df65e4553b4fa0a632151d46d9a96385263312561f183d0ecce8`（与开工提示一致）。台账中与本段直接相关的已定稿项（背景，不继承 PASS）：

- U05-003：自助注册无设备公钥/指纹；明确点名 U07 `SecurityConfig:121-122` permitAll
- U01-007 / U02-002：自助注册契约缺设备标识
- U01-001 / U02-001：客户端自报 `app` / `staff_work_phone`
- U02-004：main verifier 工作手机用员工凭证公钥，副本改设备级公钥（正是本段 diff）
- U05-008 / U06-007：以 main 为底重放，U05+U06+U07 同一 PR
- U00-000：76 文件相对旧基线，发布会与 main 分叉；本段 3 文件的 in-file 分叉见 §6（这 3 个同名文件 main＝基线，M009–M012 未改它们）

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

清单本段 3 条与独立 `sha256sum`/`wc`/`git cat-file` **3/3 match**。副本 SHA 与树 `e28dad05:<path>` blob **3/3 相同**。全 76 文件相对基线 numstat 合计 **+4745 / −1084**（独立 `git diff --shortstat` 实算，与开工提示一致）。

对象核验：`git cat-file -t 03acdb98…` fatal（无 commit）；`e28dad05…` = tree；`242f4872…` = commit。

### 1.1 HTML 锚点实核（锁出处行 → 现行正本）

| 判据 | 锁出处 | 现行该行含 |
|---|---|---|
| BEQR-150 | beqr:233 | `终端十一端`（客人手机应用…员工工作手机、员工私人手机…触摸屏一体机…） |
| BEQR-153 | beqr:236 | `终端开关优先级` 全局>…>灰度；拉取失败兜底关闭；留痕不受关 |
| BEQR-157 | beqr:240 | `员工状态` 待入职、在职、试岗、停岗、离职 |
| BEQR-182 | beqr:279 | `考勤日结果` |
| BEQR-183 | beqr:280 | `打卡方式` 领取工作手机三方式取最早 |
| BEQR-184 | beqr:281 | `请假类型` |
| BEQR-185 | beqr:282 | `请假单状态` |
| BEQR-186 | beqr:283 | `薪资单构成` |
| BEQR-187 | beqr:285 | `员工提成发放` |
| BSM-001 | bsm:505 | `整体说明：这套系统到底管什么` |
| BSM-002 | bsm:526 | `一笔业务怎么走` |
| BSM-003 | bsm:563 | `系统通用能力：数据可改可删、操作有引导、权限分岗` |
| BSM-009 | bsm:596 | `基础管理` |
| BSM-012 | bsm:611 | `酒吧平板/工作手机` 平板点酒；工作手机核销 |
| BSM-013 | bsm:616 | `触摸屏一体机` |
| BSM-026 | bsm:686 | `各类入口` 并由后台统一控制各端开关 |
| BSM-028 | bsm:698 | `主要功能`（基础管理） |
| BSM-029 | bsm:714 | 用户权限要知道员工注册信息、设备绑定、私人手机、公司工作手机 |
| BSM-044 | bsm:929 | 平板、公司工作手机、员工私人手机 App 是不同设备和不同用途 |
| BSM-045 | bsm:933 | 安装时生成并绑定该手机专属设备标识 |
| BSM-046 | bsm:953 | `平板图形点单（POS）` |
| BSM-047 | bsm:963 | 连接基础管理，确认员工注册、审核、设备绑定 |
| BSM-048 | bsm:978 | `触摸屏一体机：权益领取、现场服务和辅助售票` |
| BSM-049 | bsm:982 | 一体机主要功能 |
| BSM-050 | bsm:996 | 一体机和谁有关 |
| BSM-073 | bsm:1313 | 商户授权范围内操作本商户员工 |
| BSM-084 | bsm:1427 | 员工手机端 |
| BSM-085 | bsm:1431 | 日常工作；两条注册路径 |
| BSM-086 | bsm:1441 | 排班考勤请假 |
| BSM-087 | bsm:1455 | 我的薪资 |
| BSM-088 | bsm:1467 | 提成与人员服务边界 |
| BSM-114 | bsm:1827 | 各类入口只看自己该看的 |
| BSM-115 | bsm:1897 | 基础管理连所有业务 |
| BSM-120 | bsm:1957 | 已拍板：私人手机 App 绑定该手机专属设备标识 |
| PGM-001 | pgm:256 | 平台治理层是底座 |
| PGM-008 | pgm:362 | 员工和手机：注册、私人手机设备绑定、工作手机 |
| PGM-010 | pgm:372 | 终端开关 |
| PGM-022 | pgm:463 | 空间和点位 |
| PGM-024 | pgm:476 | 影响业务 |
| PGM-026 | pgm:492 | 主要功能（空间） |
| PGM-032 | pgm:572 | 人、私人手机、工作手机要能对应起来 |
| PGM-033 | pgm:576 | 两条注册路径；安装时绑定设备标识 |
| PGM-034 | pgm:586 | 工作手机和企业微信 |
| PGM-035 | pgm:599 | 提成发放 |
| PGM-036 | pgm:611 | 设备要绑定员工、班次、点位和状态 |
| PGM-037 | pgm:615 | 建设备台账；领用、归还、维修、挂失、报废 |
| PGM-038 | pgm:624 | 工作手机负责扫码核销 |
| PGM-045 | pgm:712 | 终端开关：后台说了算 |
| PGM-046 | pgm:716 | 能控制哪些端（含工作手机和私人手机 App） |
| PGM-047 | pgm:728 | 关闭后接口拒绝；留痕不受关 |
| PGM-048 | pgm:749 | 商户侧 |
| PGM-059 | pgm:868 | 客人与员工的个人信息保护 |
| PGM-066 | pgm:951 | 已拍板规则 |

## 2. 本段文件核验与逐文件 diff stat

相对发布基线（`git diff --numstat 242f4872 e28dad05 -- <path>`）：

| 路径 | 副本行/字节/SHA256 | 树 blob | 基线→未发布 | main 当前行/SHA256 | 基线→main |
|---|---|---|---|---|---|
| `backend/.../config/SecurityConfig.java` | 146 / 8150 / `85a40911482a1388db5fe3ef55df7a80d7ddcd0b8a37d839752e4a605acb3421` | 同 SHA | +4 / −0 | 142 / `05d06be4eb7ad7bfbca11d587c407d57648aed52c780325e22433283806be7ca` | **空** |
| `backend/.../gate/DeviceCredentialVerifier.java` | 371 / 17363 / `6361fa97f4ddbcc3552f3c3ac5e2ed144ad9a5982e0bd7914ca0b4a18e03dc4f` | 同 SHA | +4 / −7 | 374 / `99f83fa63fa9862b0cf85f6e4b274ed2961e83610c528c6be8061527dde9fae1` | **空** |
| `backend/.../terminal/TerminalDeviceService.java` | 1068 / 47901 / `c2aa2681909b6e45b0b8e5449dff577651cc395047ff663b12e632e2c5fa0168` | 同 SHA | +3 / −1 | 1066 / `37ba6510230bc6dab537c3d7cdfb240251bb35ada229a9c1487b9af5789979e1` | **空** |

`git diff --stat` 三文件合计 **3 files changed, 11 insertions(+), 8 deletions(-)**。

`git diff 242f4872 445a49f4 -- <本段三路径>` 输出为空：这三份同名文件 **main 当前 ≡ 发布基线**，M009–M012 **没有**改它们。发布时「覆盖 main 已修项」不发生在这三份正文里，而发生在：(1) 把未发布钥源/公开路径叠到仍走员工凭证公钥的 main verifier 测试与生产行为上；(2) 同批其它段文件（U03/U05/U08/U10）会盖 M009–M012。对抗式固定问题见 GR-U07-002、GR-U07-006。

未发布相对基线的实质 hunk（去掉「文件版本：V0.01」注释后）：

1. `SecurityConfig.publicApiPaths` 增加 `"/api/v1/staff/registration-companies"`、`"/api/v1/staff/self-registrations"`（permitAll）。
2. `DeviceCredentialVerifier.resolveSigningKey`：删除 `staffRepository.findActiveCredential(profile.id())` → `StaffDeviceCredentialRecord.devicePublicKey`；工作手机在 staffId 归属校验后与平板/一体机一样走 `deviceSigningKeyStore.findActivePublicKey(deviceId)`。
3. `TerminalDeviceService.bind`：`staffId` 从恒 `null` 改为 `existingDevice == null ? null : existingDevice.staffId()`。

## 3. 文件覆盖表（全量阅读，非只看 diff）

| 文件 | 覆盖段 | 结构 |
|---|---|---|
| SecurityConfig.java | L1–146 一次读完 | 构造器、401 入口点、authorizeHttpRequests、安全头、过滤器顺序、`publicApiPaths`、`apiDocumentationPaths` |
| DeviceCredentialVerifier.java | L1–371 一次读完 | 模式/构造、`verify`、`tryVerify`、`resolveSigningKey`、验签、reject/audit、isWrite、Outcome |
| TerminalDeviceService.java | L1–400、L400–800、L800–1068 三段覆盖 | 签名钥登记、bind/return/unbind、生命周期、商户归属、能力开关、kiosk config/heartbeat、executeWrite、令牌哈希、工具函数 |

无 OVERSIZE；无抽样后宣称整文件 PASS。

## 4. 数据流（副本本段 → 副本其它段 → main → SQL/HTML）

```
HTTP
  → SecurityConfig permitAll / authenticated
  → DeviceCredentialBodyCachingFilter（写 + X-Device-Signature 才缓存 body）
  → Bearer/Cookie/客人过滤器
  → DeviceCredentialInterceptor (/api/**，豁免 /api/v1/terminals/kiosks/)
       → DeviceCredentialVerifier.verify  [本段]
            → TerminalDeviceRepository.findByDeviceId
            → JdbcStaffRepository.findProfileByUserId（工作手机）
            → DeviceSigningKeyStore.findActivePublicKey  [未发布改为工作手机也走这里]
            → （main 同行还走 findActiveCredential / StaffDeviceCredentialRecord）
       → NonceStore.tryConsume（verified 才消费）
       → TerminalIdentityHolder
  → TerminalMasterSwitchInterceptor（写且无派生类型 → TERMINAL_NOT_ALLOWED；豁免 auth/open/kiosks/capability-switches，**不含** /staff/self-registrations）
  → StaffController.selfRegister / registrationCompanies  [U05 副本]
  → TerminalDeviceController → TerminalDeviceService  [本段]
       → terminal_devices UPSERT（staff_id = EXCLUDED.staff_id）
       → terminal_device_signing_keys / terminal_device_credentials / status events
```

HTML 对照主句：PGM-033/BSM-045/BSM-120 安装绑定设备标识；PGM-032/036 人与工作手机、设备绑定员工；BSM-044 三端不同用途；PGM-047 关开关后端拒绝。

## 5. 三线（按文件）

### 5.1 SecurityConfig.java（M396）

**第一性原理。** 必须存在：全站 HTTP 鉴权与 401 口径。删除后登录/权限/CSRF 替代链无入口。未发布增量把两条员工自助注册路径放进 `permitAll`：审批前无账号，公开提交本身可成立，但 HTML 要求提交时已有私人手机设备标识（PGM-033 L576 锚、BSM-120 L1935 附近已拍板句）。本文件只放开端口，不校验设备标识。

**顺从式。** 401 中文「登录已过期，请重新登录」；prod 关掉 swagger；CSRF 改双提交；客人过滤器在员工 Bearer 之后。与 main 除公开路径外一致。`RateLimitInterceptor` 规则列表无 `/staff/self-registrations`（只限登录/支付/退款）。

**对抗式。** 未认证 POST 姓名/手机/证件；无限速；Spring CSRF 关、公开路径无 Cookie 则双提交也不挡。主开关对无类型头的写请求 403（`TERMINAL_NOT_ALLOWED`），U01/U02 自报 `app` 才能进。公司列表 GET 无类型头可过主开关。见 GR-U07-001。

### 5.2 DeviceCredentialVerifier.java（M063）

**第一性原理。** 必须存在：服务端派生终端身份，避免自报头绕过（注释 2026-08-03 收口）。有 `X-Device-Id` 时登记类型压过自报——与 BSM-044「不同设备不同用途」、PGM-047「接口拒绝」同向。无设备号时仍用自报类型，把收口留了口子。

**顺从式。** DEVICE_BOUND_TYPES = staff_work_phone/tablet/kiosk。工作手机校验 `device.staffId == profile.id()`，无 staffId → INVALID_OWNER（grace 也拒，因为是 INVALID 不是 NO_PROOF）。验签 canonical = deviceId/ts/nonce/METHOD/URI/sha256(body)。默认 grace。web enforce 拒 Bearer 自报；app enforce 拒写。

**对抗式。**

- 改报 web 但带已登记 kiosk 的 deviceId：登记类型压过，进设备闸。未发布测试 `selfReportedWebCannotEscapeDeviceBoundGate` 覆盖。
- 不带 deviceId、自报 app/web：不进设备闸。U01/U02 公开注册正是这样。
- 工作手机钥源改为设备级后，main 测试仍 stub `findActiveCredential`、不 stub `signingKeyStore` → 有效签名变 `credential_required`；错误签名在 grace 下因无钥变成 NO_PROOF 被放行。EXP A 实跑。
- 缺钥 + 乱签名 ≡ 缺签名（grace 放行）。enforce 才拒。默认 grace，首轮正常是这条。收窄：不单独立项为新攻击面，并入分叉项与观察。

### 5.3 TerminalDeviceService.java（M064）

**第一性原理。** 必须存在：PGM-036/037 台账、领用、归还、维修、挂失、报废。生命周期 CAS、终态封锁、kiosk 令牌只回一次、商户归属改绑留痕，与 HTML「谁领了什么、能不能用」同向。

**顺从式。** bind 拒绝 lost/scrapped/repair/blocked 复活；不传商户则保留已有商户（K16）；幂等；kiosk SHA-256 令牌；`registerSigningKey` 要求已 bound。控制器侧 `requirePermission`。main `TerminalDeviceServiceTests` 37/37 叠本段 bind 改动仍绿。

**对抗式。** `DeviceBindRequest` 无 staffId；首次 bind staffId 恒 null。Verifier 对 null staffId 的工作手机，只要凑齐签名头就 INVALID（grace 也 403）。`/tmp` 探针：首次 bind `staffId==null`；归还写入 `staff_1` 后再 bind，未发布保留 `staff_1`（main 会写回 null）。HTML「设备要绑定员工」「领用记录责任人」在领用入口不成立。见 GR-U07-004。

## 6. 与 main 分叉对照（对抗式固定题）

| 事实 | 命令证据 |
|---|---|
| 本段 3 文件 main blob ＝ 基线 | `git diff 242f4872 445a49f4 -- <三路径>` 空 |
| 未发布相对基线/相对 main 的 diff **相同** | `git diff 445a49f4 e28dad05 -- <三路径>` 等于基线→未发布 |
| 叠未发布 verifier 跑 **main** 测试 | EXP A：Tests run: 22, Failures: 1, Errors: 2。`validSignatureDerivesTrustedTypeVerified` / `bodySignedMatchesVerifies` → `设备凭据校验未通过：credential_required`；`invalidSignatureRejectedEvenInGrace` 期望抛却未抛 |
| 叠未发布 verifier + **未发布** 测试 | EXP B：Tests run: 23, Failures: 0, Errors: 0 |
| 叠未发布 TerminalDeviceService 跑 main 服务测试 | EXP C：Tests run: 37, Failures: 0, Errors: 0 |
| 未发布 bind 保留归还写入的 staffId | EXP D 探针 1/1 PASS：`first.staffId()` null；rebind 后 `staff_1` |

结论：单独把本段合进当前 main，工作手机验签与 main 测试/员工 Keystore 路径对打（U02-004 的后端面）。这 3 个文件不会回滚 M009-003/M012-* 的源码行，但同批整树覆盖会。处置：以 main 445a49f4 为底重放本段 hunk，与 U01/U02/U05/U06/U12 同一 PR，客户端与后端只认同一把钥。

## 7. Findings（临时 ID；正式 ID 仅 B2）

### GR-U07-001 — 公开自助注册/公司列表 permitAll，无设备标识、无限速

- 文件:行号：`SecurityConfig.java:118-122`（副本）；调用链 U05 `StaffController.java:46-56`、`StaffRegistrationService.java:51-79`
- 完整原文：`"/api/v1/staff/registration-companies",` / `"/api/v1/staff/self-registrations",` 进入 `permitAll()`。`RateLimitInterceptor` RULES 无这两条。
- 业务影响：未登录可 GET 公司名单、POST 姓名/手机/证件。HTML 要求私人手机安装时绑定专属设备标识；本段端口不要求。PII 无认证、无设备证明、无该路径频控。
- 可复现：读副本 L118-122；`rg self-registrations` RateLimitInterceptor 无命中。同根 U05-003 已定稿。
- 冻结判据：PGM-032、PGM-033、BSM-120、BSM-029、GEN-004、GEN-010、PGM-059
- 2.3 依据类型：1 HTML 原文 + 4 可复现命令（读路径/限速表）
- 四分类：**冲突**
- 工程缺陷：成立。建议登记：是（同根 U05-003，不新开 2.2）
- 无业务发明：不适用（冲突项）
- 最小处置（发布者）：随 U05-003 — 自助注册请求带私人手机设备公钥/指纹；公开端口加频控/来源校验；SecurityConfig 仅在该契约落地后保持 permitAll。U01/U02/U05/U07 同一 PR。

### GR-U07-002 — 工作手机验签改读设备级公钥，与当前 main 员工凭证公钥分叉

- 文件:行号：`DeviceCredentialVerifier.java:236-251`（副本）；main 同行仍 `return staffRepository.findActiveCredential(profile.id()).map(StaffDeviceCredentialRecord::devicePublicKey)…`
- 完整原文（副本）：staffId 校验后 `return deviceSigningKeyStore.findActivePublicKey(deviceId).filter(k -> !k.isBlank()).orElse(null);` 注释改为「所有设备绑定型终端均使用设备级签名公钥」。
- 业务影响：main 工作手机签名验员工 Keystore；未发布验 `terminal_device_signing_keys`。只合本段：enforce 下工作手机写请求 `credential_required`/`bad_signature`；只合 U01/U02 客户端登记设备钥而 main verifier 不读。默认 grace 时缺钥走 NO_PROOF 放行，错误签名也不再当 INVALID。
- 可复现命令：`/tmp/u07-b1-grok/be` 叠三份未发布源 + main 测试，`JAVA_HOME=temurin-21 mvn -o -Dtest=com.complex.platform.gate.DeviceCredentialVerifierTests test` → 22 run / 1 fail / 2 error，见 §6 EXP A。未发布测试 23/23 见 EXP B。
- 冻结判据：GEN-002、GEN-008、GEN-003、PGM-032
- 2.3 依据类型：4 可复现命令
- 四分类：**技术性**（钥存放位置，不是新的业务端或角色）
- 无业务发明理由：HTML 要求工作手机与人对上、公司设备受管；未规定公钥必须在 `staff_device_credentials` 还是 `terminal_device_signing_keys`。这是实现面切换。缺陷是与 main/客户端不一致，不是新造业务规则。
- 工程缺陷：成立。建议登记：是（同根 U02-004）
- 最小处置：以当前 main 为底重放本 hunk；U01/U02/U07/U12 同一 PR，只认一把钥；不得整树覆盖。

### GR-U07-003 — 无设备号时终端类型仍信自报头

- 文件:行号：`DeviceCredentialVerifier.java:138-149`、`174-186`
- 完整原文：`TerminalDeviceRecord device = deviceId == null ? null : deviceRepository.findByDeviceId(deviceId).orElse(null);` 然后 `terminalType = device != null && … ? device.terminalType() : claimed;` 无设备号时 `claimed` 为 web/app 即不进 DEVICE_BOUND_TYPES。
- 业务影响：U01/U02 公开注册自报 `app`、现场路径恒报 `staff_work_phone`。私人手机做现场收银/核销可被标成工作手机；自助注册被标成客人端。HTML BSM-044 三端不同用途。有 deviceId 的收口有效，无 deviceId 的首轮路径仍在。
- 可复现：`nonDeviceBoundTypePassesThrough`（未发布/main 测试均有）；对照 U01 `applyHeaders`。
- 冻结判据：BEQR-150、BSM-012、BSM-044、BSM-114、PGM-032、PGM-045、GEN-004、GEN-005、GEN-008
- 2.3 依据类型：1 HTML 原文 + 4 测试/代码
- 四分类：**冲突**
- 工程缺陷：成立。建议登记：是（同根 U01-001/U02-001）
- 最小处置：与 U01-001 同一处置 — 身份由绑定/登记设备类型派生并只声明一次；无设备号的写请求 fail-close（主开关已对完全空类型 403，但自报 app/web 仍过）。现场资金只信 verifier 派生身份。

### GR-U07-004 — 领用入口不写员工，工作手机验签却要求 staffId

- 文件:行号：`TerminalDeviceService.java:635-676`；`DeviceBindRequest`（调用链，无 staffId 字段）；`DeviceCredentialVerifier.java:246-248`
- 完整原文：bind 构造 `existingDevice == null ? null : existingDevice.staffId()`；请求体不能传入员工。Verifier：`if (device.staffId() == null || !device.staffId().equals(profile.id())) return INVALID_OWNER;` INVALID 在 grace/enforce 都 `reject`。
- 业务影响：PGM-036「设备要绑定员工」、PGM-037「领用…责任人」、PGM-032「人、工作手机要能对应起来」。首次 bind 后工作手机 staffId 为空。客户端一旦按未发布契约带齐签名头，默认 grace 也会 403 `device_owner_mismatch`。U01/U02 非公开路径发签名，首轮会发生。归还可写入 staffId，领用与归还职责反置。
- 可复现命令：EXP D `/tmp` 探针 `u07ProbeBindLeavesStaffIdNullAndRebindKeepsReturnStaff` Tests run: 1 Failures: 0 — `first.staffId()` null，归还 `staff_1` 后再 bind 保留。main 测试 `staffWorkPhoneWithNullStaffIdRejected` 覆盖 verifier 侧。
- 冻结判据：PGM-036、PGM-037、PGM-032、BSM-029、BSM-047、GEN-002、GEN-005、GEN-008
- 2.3 依据类型：1 HTML 原文 + 4 可复现命令
- 四分类：**冲突**（HTML 要求绑定员工/责任人，领用入口不写；不是新发明字段）
- 工程缺陷：成立。建议登记：是。根因在基线已有「bind 写 null」；未发布只在再绑时保留旧值，未补赋值口，又把客户端推向签名路径，缺陷从「grace 无签名可过」变成「一签名就拒」。
- 最小处置：`DeviceBindRequest` 增加员工（工作手机必填）；bind 写入 `staffId`；平板/一体机按 HTML 绑点位/商户而非人。禁止只靠归还回填。与 verifier 同一 PR。

### GR-U07-005 — 设备凭据 403 把英文 reason 拼进中文展示句

- 文件:行号：`DeviceCredentialVerifier.java:309-312`、`165`、`176`、`182`、`201`、`204`、`210`、`217`、`224`、`231`
- 完整原文：`throw ApiException.forbidden("DEVICE_CREDENTIAL_INVALID", "设备凭据校验未通过：" + reason);` reason ∈ {credential_required, bad_signature, device_owner_mismatch, web_requires_cookie_session, app_requires_customer_proof, …}
- 业务影响：安卓/现场无翻译层。铁律 7 / GEN-015 展示全中文。错误码本身可英文；拼进 message 的 reason 会出网。
- 可复现：EXP A 实测 `ApiException: 设备凭据校验未通过：credential_required`。jshell：`FieldLabels.translate("设备凭据校验未通过：credential_required")` 仍原样；对比 `idempotency_key is required` →「请勿重复提交…」，`terminal_type is invalid` →「终端类型格式不正确」。
- 冻结判据：GEN-015、GEN-009、GEN-010
- 2.3 依据类型：4 可复现命令
- 四分类：**技术性**
- 无业务发明理由：只改展示语言，不新增业务状态。reason 键可留日志，响应体用中文原因。
- 工程缺陷：成立。建议登记：是（main 既有抛点，但本段改钥源后 EXP A 已打出该中英混排；FieldLabels 收不住）
- 最小处置：reject 用全中文原因表（缺凭据/签名不符/设备不属于当前员工/…）；reason 只进审计 payload。归 M063 发布时一并改。

### GR-U07-006 — 发布不得整树覆盖；本段须以 main 为底重放

- 文件:行号：本段三文件整体；对照 main `445a49f4`
- 完整原文：见 §2/§6。三文件 in-file 无 M009–M012 行级补丁；未发布增量叠到 main 等于叠到基线。同批 U05 `StaffProfileRecord` 等已与 main 分叉。
- 业务影响：整树覆盖会回滚其它段已合入的 M009–M012，并让工作手机验签与 main 测试对打。
- 可复现：§2 空 diff 基线↔main；§6 EXP A。
- 冻结判据：GEN-002、GEN-008、GEN-013
- 2.3 依据类型：4
- 四分类：**技术性**
- 无业务发明理由：合并策略，不改业务口径。
- 工程缺陷：成立。建议登记：是（同根 U05-008/U06-007）
- 最小处置：以 main 445a49f4 为底 cherry-pick 本段 11 行业务 hunk + 配套测试；U05+U06+U07+U01+U02 同一 PR；迁移编号随 U10 ≥V411。

## 8. 观察（无新根因或首轮收窄，完整登记不漏并）

1. 本段 3 同名文件 main＝基线，M009–M012 未改它们。与开工提示「同名文件都已分叉」在**这三份**上不成立；分叉在未发布增量。不升为与 U03 同类「覆盖已修正文」。
2. 未发布测试新增 `privatePhoneKeyCannotReplaceWorkPhoneKey`：清空设备钥、只留员工凭证，期望 403。证明测试已按新钥源改，GEN-003 在副本内自洽（EXP B 23/23）。测试文件属 U12，本段只作调用链。
3. grace 下无设备钥时，错误签名走 NO_PROOF 而非 INVALID（EXP A `invalidSignatureRejectedEvenInGrace` 未抛）。与「不签名」同等放行，不比缺签名更糟。默认 grace + 注释「真正翻 enforce 待客户端」。不单独立项。
4. `TerminalMasterSwitchInterceptor` 豁免名单无 `/staff/self-registrations`。无 `X-Terminal-Type` 的公开 POST 得 403 `TERMINAL_NOT_ALLOWED`；U01 自报 `app` 才能过。观察，并入 001/003。
5. 公开公司列表 GET 不进写 fail-close，可未认证枚举公司 id/名称。U05-012 OBS-4 已登记。
6. `listDevices` 用 DEFAULT_PAGE/SIZE=50 却一次返回全量。main 既有。观察 GEN-012。
7. bind 把 `boundSpaceId` 同时写入 `locationPointId`。main 测试断言 `locationPointId==point_1`。字段复用，不升冲突。
8. `parseMode` 非法字符串静默回落 GRACE。配置打错 enforce 不会 fail-closed。观察 GEN-009。首轮正常配置正确则不触发。
9. `audit(...)` catch RuntimeException 只 debug。留痕失败不挡请求。观察 GEN-011。
10. `reject` 后 `throw unreachable()` 死代码。ponytail 可删。观察 GEN-001。
11. `executeWrite` 权限码只写审计，授权在 Controller。生产经 Controller。观察纵深。
12. `TERMINAL_TYPES` 仅三码，BEQR-150 十一端其余不走本 bind。不把 HTML 沉默写成「bind 必须支持私人手机」。私人手机跳过设备闸 → 003。
13. PGM-034 企微、PGM-035 提成、BEQR-157/182–187 人事状态与薪资、BSM-084–088 员工端页面、PGM-048 商户侧：本段无实现面（SecurityConfig 不实现这些规则；Device/Terminal 不发薪、不聊企微）。
14. PGM-037「型号」字段本段台账无独立列。HTML 要求台账含型号。可能在 MDM 侧。不把沉默当删除；观察缺型号列。
15. PGM-036「绑定班次」本段无班次字段。班次在员工排班模块。观察，不升冲突。
16. kiosk config/heartbeat permitAll + `X-Device-Token` 常时间比较。本段未改。观察。
17. `registerSigningKey` 任何 bound 设备可登记设备钥；未发布后工作手机依赖它。Controller 注释仍写 kiosk/tablet。注释漂移，文件不在本段。观察。
18. 未跑共享库集成、未跑 `complex_test`。EXP A/B/C/D 为 `/tmp` 离线 surefire，不写库。
19. `StaffProfileRecord` 副本相对 main 已加 companyId 等（U05）；本段测试 15 参构造器仍能在 main 类上编译。跨段发布须同 PR。
20. 本席未读 U07-b1-self-raw 或其它本轮本方结论。

## 9. 适用冻结判据逐项

| ID | 结论 | 证据 |
|---|---|---|
| GEN-001 | 部分：三文件均有用途；`unreachable()` 死代码观察 10 | 全文 |
| GEN-002 | **FAIL** GR-002/004/006；EXP A | 命令 |
| GEN-003 | 副本内测试与新钥源一致（EXP B 23/23）；叠 main 测试红（EXP A） | 命令 |
| GEN-004 | **FAIL** GR-001/003 | HTML+代码 |
| GEN-005 | **FAIL** GR-003/004；Controller 权限闸仍在 | 代码 |
| GEN-006 | 生命周期 CAS 成立；bind UPSERT 非 CAS。观察 | L270-278 |
| GEN-007 | 写路径有幂等；bind 并发 last-write-wins。观察 | executeWrite |
| GEN-008 | **FAIL** GR-002/003/004/006 | EXP A |
| GEN-009 | GR-005 混排；非法 Mode 回落观察 8 | jshell |
| GEN-010 | **FAIL** GR-001/005；kiosk 令牌哈希仍在 | 代码 |
| GEN-011 | 审计调用存在；失败吞掉观察 9 | L315-328 |
| GEN-012 | listDevices 假分页观察 6 | L301-313 |
| GEN-013 | **FAIL** GR-006 整树覆盖 | §6 |
| GEN-014 | 本段无新迁移；staff_id 列沿用既有 UPSERT | JDBC 调用链 |
| GEN-015 | **FAIL** GR-005；`is required/invalid` 经 FieldLabels 已中文 | jshell |
| GEN-016 | 本段 Java 非文档；Controller 注释 kiosk/tablet 观察 17 | — |
| GEN-017 | 冲突→细化→技术性→添加 已按序；无添加项 | §7 |
| GEN-018 | 结论落到 HTML/命令/jshell | 本文 |
| BEQR-150 | 部分：三端绑定型有闸；自报绕过 GR-003 | L58, L150 |
| BEQR-153 | 实现面在 MasterSwitch/Guard 调用链，本段提供派生类型 | interceptor |
| BEQR-157 | 无实现面（人事状态不在本三文件） | — |
| BEQR-182 | 无实现面 | — |
| BEQR-183 | 无实现面（领取工作手机打卡在员工模块）；本段只提供设备绑定 | — |
| BEQR-184 | 无实现面 | — |
| BEQR-185 | 无实现面 | — |
| BEQR-186 | 无实现面 | — |
| BEQR-187 | 无实现面 | — |
| BSM-001 | 底座安全配置存在 | SecurityConfig |
| BSM-002 | 设备领用/验签是现场核销前置；004 人机未在领用绑定 | — |
| BSM-003 | 权限分岗：Controller requirePermission；公开注册例外 GR-001 | — |
| BSM-009 | 基础管理入口，SecurityConfig 不实现业务 | 无实现面 |
| BSM-012 | 平板/工作手机类型码存在；用途靠派生身份，GR-003 | DEVICE_BOUND_TYPES |
| BSM-013 | kiosk 类型+token | getKioskConfig |
| BSM-026 | 各端开关依赖派生类型 | 调用链 |
| BSM-028 | 无实现面（基础管理功能清单） | — |
| BSM-029 | **FAIL** GR-001/004 设备绑定/工作手机对应 | HTML L714 |
| BSM-044 | **FAIL** GR-003 三端可自报混淆 | HTML L929 |
| BSM-045 | **FAIL** GR-001 安装绑定设备标识 | HTML L933 |
| BSM-046 | 平板 POS 不在本段实现 | 无实现面 |
| BSM-047 | **FAIL** GR-004 设备绑定员工 | HTML L963 |
| BSM-048 | kiosk 台账/心跳存在 | recordKioskHeartbeat |
| BSM-049 | 售票/订台能力不在本段服务实现 | 无实现面（开关行存在） |
| BSM-050 | 一体机连接关系不在本段编造 | 无实现面 |
| BSM-073 | 无实现面 | — |
| BSM-084 | 无实现面 | — |
| BSM-085 | GR-001 注册路径端口 | SecurityConfig |
| BSM-086 | 无实现面 | — |
| BSM-087 | 无实现面 | — |
| BSM-088 | 无实现面 | — |
| BSM-114 | **FAIL** GR-003 | — |
| BSM-115 | 设备模块连接员工/现场；004 断领用链 | — |
| BSM-120 | **FAIL** GR-001 已拍板设备标识 | HTML L1935 句 |
| PGM-001 | 治理底座含本配置 | — |
| PGM-008 | **FAIL** GR-001/004 | HTML L362 |
| PGM-010 | 开关依赖本段派生身份 | — |
| PGM-022 | bind 有 boundSpaceId | L668 |
| PGM-024 | 无实现面 | — |
| PGM-026 | 无实现面 | — |
| PGM-032 | **FAIL** GR-001/002/003/004 | HTML L572 |
| PGM-033 | **FAIL** GR-001 | HTML L576 |
| PGM-034 | 无实现面（企微） | — |
| PGM-035 | 无实现面 | — |
| PGM-036 | **FAIL** GR-004 | HTML L611 |
| PGM-037 | 领用/归还/维修/挂失/报废有；责任人在领用缺失 GR-004；型号观察 14 | L176-203 |
| PGM-038 | 工作手机核销依赖验签；004 使签名路径在未绑人时失败 | — |
| PGM-045 | 派生身份供总开关；GR-003 自报仍可当类型 | — |
| PGM-046 | 代码只强制三设备绑定型；其余端靠自报/空 | L58 |
| PGM-047 | 关开关拒绝在 MasterSwitch；本段提供类型。留痕 audit 调用存在 | — |
| PGM-048 | 无实现面 | — |
| PGM-059 | GR-001 公开提交证件/手机 | self-register 链 |
| PGM-066 | 已拍板设备标识句同 BSM-120 | — |

无实现面项均不因 HTML 沉默升冲突或删除。

## 10. 命令证据摘要

```
sha256sum docs/audit-2026-09/plan/criteria-lock.md
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

git rev-parse HEAD origin/main
# 445a49f409f0cfbe6dd616ae079be1299824c48f ×2

git cat-file -t 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb
# fatal: git cat-file: could not get object info
git cat-file -t e28dad059b2c7e08bcc95a8db18552d3f76edf4b   # tree
git cat-file -t 242f487233d028173cb9198eecd079f5d877405c   # commit

git diff --shortstat 242f4872 e28dad05
# 76 files changed, 4745 insertions(+), 1084 deletions(-)

git diff --numstat 242f4872 e28dad05 -- <U07 三路径>
# 4 0 SecurityConfig.java
# 4 7 DeviceCredentialVerifier.java
# 3 1 TerminalDeviceService.java

git diff 242f4872 445a49f4 -- <U07 三路径>
# （空）

# EXP A /tmp overlay unpublished 3 sources + MAIN DeviceCredentialVerifierTests
# Tests run: 22, Failures: 1, Errors: 2
# validSignature… / bodySignedMatchesVerifies → 设备凭据校验未通过：credential_required
# invalidSignatureRejectedEvenInGrace → Expecting code to raise a throwable

# EXP B unpublished tests + unpublished verifier
# Tests run: 23, Failures: 0, Errors: 0  BUILD SUCCESS

# EXP C unpublished TerminalDeviceService + MAIN TerminalDeviceServiceTests
# Tests run: 37, Failures: 0, Errors: 0  BUILD SUCCESS

# EXP D /tmp 探针 u07ProbeBindLeavesStaffIdNullAndRebindKeepsReturnStaff
# Tests run: 1, Failures: 0, Errors: 0
# first.staffId()=null；return staff_1 后再 bind staffId=staff_1

# jshell FieldLabels.translate
# MIXED=设备凭据校验未通过：credential_required
# IDEM=请勿重复提交，请刷新后再试
# TYPE=终端类型格式不正确
```

隔离编译在 `/tmp/u07-b1-grok/be`，`JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`，`mvn -o`，未 `source` env、未碰 `complex_test*`。Ubuntu `JAVA_HOME` 指向无 javac 的 JRE 时 `release version 21 not supported`，改 Temurin 后可编译。该失败不是业务 finding。

## 11. 审计结论

**FAIL**

三文件全覆盖，71 条冻结判据均有核对记录，三线均执行。存在未定稿缺陷 GR-U07-001…006。无未答 questions（无添加项）。无有条件通过。

发布者最小集：以 main `445a49f4` 为底重放本段 hunk；自助注册与设备标识同 U05-003；工作手机钥与客户端同 U02-004；自报类型同 U01-001；领用写员工；403 文案全中文。不在本轮修源。

## U07 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 41384 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，独立）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02 §7/§11；criteria-lock V0.11；modules.md V0.04 附录 U07（未发布材料第 7 段，3 文件 1585 行，main 归属 M396/M063/M064）
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`。本段三份 Java `HEAD:<path>` = `242f4872:<path>`（SecurityConfig blob `bd0b4b2409…`、DeviceCredentialVerifier blob `9088318d52…`、TerminalDeviceService blob `fef0276f6e…`）；main 自基线未改这些文件。树 `e28dad05` 对应副本三 blob `47f8dfa2` / `44436f9c` / `d2781dab`。
- 审计对象仍是只读输入副本 `.scratch/module-recheck-20260908/unpublished-03acdb98`（树 `e28dad059b2c7e08bcc95a8db18552d3f76edf4b`，基线 `242f487233d028173cb9198eecd079f5d877405c`），不是 main。三份 HTML 取 main 正本。未发布提交对象 `03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb` 本仓 `git cat-file -t` 仍不可解析。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B2 核实结论。
- 本席 B2-r1：**PASS**（001–007 逐 ID 全 PASS；添加 0，不发 2.2；发布前须修「是」=001–005，「否」=006–007）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 内容同值（sidecar 文件自身 SHA `e27adda448fbf46d50c9c9f5afada2a382ef94bf7bbbbfef75aacd0580130289`）；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`（ledger 第 2 行）。映射行 `M063 →` L6442 与 `M064 →` L6443 两行闭集逐字相同 50 项，`M396 →` L6775 为 52 项；python 并集 71，MISSING=[] EXTRA=[]。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要核对只读（输入哈希、blob、`git show 242f4872:…` 落到 `/tmp/u07-b2-grok`、HTML 锚点、`questions/U04-001.md` 答复首行「删除」）；未重跑 mvn/测试、未装依赖、未碰 `complex_test*`。核对副本结束即删 `/tmp/u07-b2-grok`。

此票只判断 findings.md V1.56 顶部「当前 U07 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、「发布前须修」是/否与最小处置。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。材料未发布，定稿后状态应记 REVIEWED-FAIL（有已定稿缺陷，处置写给发布者），不做 B3。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/U07-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 41384 | `ce3747c9cb17e41fca7007314e25f47cdfba9bce5a84b0c0daa2c6f4ebb7bbaf` |
| `docs/audit-2026-09/modules/U07-b1-self-raw.md` | V0.01 | 25888 | `e1018b9c75ed1a930cad56697f513587128eb70c95051224994c6b3bc8035f12` |
| `docs/audit-2026-09/findings.md` | V1.56 | 603063 | `065143206fcc3c4cc423bd4c5b2feb6487ce3055e5dc23bb270b21bbdeaedde2` |
| `docs/audit-2026-09/ledger.md`（只读） | V2.62 | 483791 | `658cb0c8f2fa107c9a13d1e6f81139c7b877c4b14d3467a0bc4e5eadc5efb4cd` |
| `docs/audit-2026-09/questions/U04-001.md`（只读，沿用） | V0.01 | 2282 | 答复首行精确「删除」 |

候选形态：V1.56 顶部 U07-001…007；questions 0；添加 0；无 HTML 修改申请。U07 ledger 行 IN_PROGRESS。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

HTML 正本哈希与 B1 一致：BEQR `4396bb71…1d66`、BSM `a7f7b7a3…66fa`、PGM `c4238afd…e71c`。副本本段 3/3 sha256/行/字节与树 `e28dad05` blob 全等（本轮复算）。`git diff --stat 242f4872 e28dad05 -- <3 路径>` = `3 files changed, 11 insertions(+), 8 deletions(-)`。`git diff 242f4872 HEAD -- <3 路径>` 空。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers | 先核技能；用户/提示词覆盖技能本体 |
| receiving-code-review | 先核 HTML/diff/blob/grep 再接受或反驳席甲改判；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：006 拼接是否被钥源改加重、001 并冲突是否发明业务、002「一把钥」是否最懒、007 是否写错 §8.3/4/19 |
| verification-before-completion | 声称前先跑 sha256/git rev-parse/git show/diff/sed/rg；本票不声称本轮新编译绿 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流（本段 3 文件 → U05 注册/U01 类型头/U06 打卡 → HTML → 基线 blob） |
| ponytail-review | 只评最小删除/收窄，不落地。001 复用 bind 不造领用端点；002 不指定钥表；006 不把 M063 积压改成 U07 发布门 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到**；未冒充调用 |
| Arc audit | 会派子代理；与双席冲突；**未用** |

### 完整差集（GR-001…006、§8 观察 1–20、席甲 CX-001、OBS-1…5）

漏并：无。两席 B1 全部临时 ID 仍落在 001–007。席甲独有 CX-001 与本席 GR-004 同一领用写口事实，并列进 001，分类取冲突（见 (b)）。本席 GR-005 的技术性保留，按 U01 定稿「main 既有」口径改登记为「技术性（main 既有）」发布前须修否、归 M063，不把 B1 文首「发布前须修：001…006」总述当成 006 的关门票。席甲 OBS-1…5 与本席 §8 1–20 进 007，未把 finding 降观察、未把观察升格入队。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/须修 | 本席票 |
|---|---|---|---|
| U07-001 | CX-U07-001 + GR-U07-004 | 冲突/是/是 | **PASS（冲突/是/是）** |
| U07-002 | GR-U07-002 | 技术性（同根 U02-004）/是/是 | **PASS（技术性同根/是/是）** |
| U07-003 | GR-U07-003 | 冲突（同根 U01-001/U02-001）/是/是 | **PASS（冲突同根/是/是）** |
| U07-004 | GR-U07-001 | 冲突（同根 U05-003/U01-007/U02-002）/是/是 | **PASS（冲突同根/是/是）** |
| U07-005 | GR-U07-006 + 席甲 §4 | 技术性（发布分叉，同根 U05-008/U06-007）/是/是 | **PASS（技术性同根/是/是）** |
| U07-006 | GR-U07-005 | 技术性（main 既有）/是/否（归 M063） | **PASS（技术性 main 既有/是/否）** |
| U07-007 | 双方观察 | 登记/否/否 | **PASS（登记/否/否）** |

冲突入队 3（001、003 同根 U01-001、004 同根 U05-003）。技术性入队 2（002 同根 U02-004、005 同根 U05-008）。技术性（main 既有）1（006，归 M063）。登记观察 1（007）。添加 0。

发布前须修「是」=001、002、003、004、005（5）；「否」=006、007（2）。与候选汇总一致。

### 重点 (a) U07-006（GR-005）须修裁定 — **PASS（口径合规；须修否；归 M063）**

口径沿用 U01 定稿、U06 已用同一把尺子：main HEAD 同位置已有且本材料未加重该缺陷本体 → 技术性/冲突/添加（main 既有）、发布前须修否、归主归属模块（本段拼接在 M063 的 verifier）。材料不得加重。本席按「缺陷本体是否出现在 `git diff 242f4872 e28dad05 -- DeviceCredentialVerifier.java` 的 +/- 中、出现后是收窄还是加码」独立核，不照抄席甲「基线已有」四个字。

对照物：`git show 242f4872:backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java` 落到 `/tmp/u07-b2-grok/base-verifier.java`（与 HEAD 逐字节相同）；副本 371 行。

| 核项 | 本席命令 | 基线/main | 副本 | diff 是否触及缺陷本体 | 加重？ |
|---|---|---|---|---|---|
| 拼接模板 `"设备凭据校验未通过：" + reason` | `rg` 该句；`git diff 242f4872 e28dad05 -- …DeviceCredentialVerifier.java` 检索该句 | `:315` 整句 | `:312` 整句（仅因文件头注释/import 平移） | **空**（该 diff 只有：删 `StaffDeviceCredentialRecord` import、改钥源注释、删 `findActiveCredential` 四行） | **否** |
| reason 键集合 | `rg Outcome.invalid` 与 `credential_required` | 已有 credential_required / bad_signature / device_owner_mismatch / web_requires_cookie_session / app_requires_customer_proof 等 | 同键 | 无新 reason 键进 +/- | **否** |
| `reject()` 把 reason 拼进 message | 读 `reject` | 基线 `:312-315` | 副本 `:309-312` 同构 | 方法体不在 +/- | **否** |

钥源切换（工作手机改读 `deviceSigningKeyStore`）在 +/- 里，那是 **002** 的缺陷本体，不是本条 GEN-015 模板。EXP A 打出 `设备凭据校验未通过：credential_required` 是 002 的触发器走了同一条既有抛点，不能把「更常打到这条模板」算成模板本身被加码。001 修好领用、002 对齐钥之后，这条中英混排仍在——所以登记进 006、发布前须修否、归 M063 发布时改响应体为全中文原因表、reason 键只进审计。

本席 B1 正文已写「main 既有抛点……归 M063 发布时一并改」；文首把 GR-001…006 一并列进「发布前须修」是当时未按 U01 口径拆「登记」与「本段发布门」。本票以正文+U01 口径为准：**须修否**。候选与此一致。不把 M063 积压改成 U07 发布门。

### 重点 (b) U07-001 并 CX-001 与 GR-004 — **PASS（冲突成立；须修是；处置不发明）**

两席同一事实，不是两句话硬拧：

- 席甲 CX-001：`DeviceBindRequest` 无 `staffId`；bind `:676` 只保留 `existingDevice.staffId()`；verifier `:246-248` 与 U06 打卡 `:864-865` 要求 `device.staffId==profile.id`。HTML 台账「领还」。
- 本席 GR-004：首次 bind `staffId` 恒 null；INVALID_OWNER 在 grace/enforce 都 `reject`；归还可写 `staffId`，领用与归还职责反置。

独立实核（不采信席甲探针为己证，本轮只读 blob）：

1. 基线 bind 构造第 13 个业务字段是字面 `null`（`/tmp/u07-b2-grok/base-tds.java` 约 `:675`）。副本改成 `existingDevice == null ? null : existingDevice.staffId()`——**该行在 +/- 中**。首次 bind 仍 null；再绑只保留已有值，请求体仍不能传入员工。
2. `DeviceBindRequest`（副本与 main 同构）字段到 `merchantId` 为止，无 `staffId`。`DeviceReturnRequest` / `DeviceLifecycleRequest` 有 `staffId`。归还副本 `:735` `firstNonBlank(actual.staffId(), existing.staffId())` 会写入。
3. 归属检查本身不在本段 +/-：基线 `resolveSigningKey` 已有 `device.staffId()` 为空或不等于档案则 `INVALID_OWNER`。INVALID 走 `reject`，grace 也 403。
4. 本材料把工作手机钥源改到设备级（002），同批 U01/U02 非公开路径发签名。基线/main 客户端缺签名时 grace 走 NO_PROOF 放行；带齐签名后 null staffId 变成 `device_owner_mismatch`。这是 **加重**：洞还在，流量从「无签名可过」变成「一签名就拒」。U01 口径「材料不得加重」→ 不得按纯 main 既有放过。
5. U06 打卡副本 `StaffService.java:864-865` `WORK_PHONE_OWNER_REQUIRED` 同一归属谓词，是调用链，不另开根因。

分类：**冲突**（GEN-017 先于技术性）。HTML 现行 PGM **L572**「人、私人手机、工作手机要能对应起来」、**L612**「设备要绑定员工、班次、点位和状态」、**L617**「建设备台账，记录编号、型号、点位、责任人和状态」、**L618**「记录设备领用、归还、维修、挂失和报废」、BSM **L1445**「领到自己那台工作手机」。领用入口不写员工，不是 HTML 下位细节，也不是新造字段。席甲 B1 标技术性但引用的是同一组 HTML 原句——缺的是 HTML 已要求的写路径，应升冲突；候选取冲突，与本席 B1 一致，未改写席甲事实。

须修是。工程缺陷是。无业务发明（冲突项不适用「无业务发明理由」条款；字段名 `staffId` 已在归还/生命周期 DTO 与列上）。

最小处置与本席 GR-004 原文同向，且比席甲「或独立领用端点」更懒：

- `DeviceBindRequest` 增加员工；工作手机必填；bind 写入 `staffId`。
- 平板/一体机继续用请求里已有的 `boundSpaceId` / `merchantId`（BSM L942 平板先绑酒吧再绑桌位；本条不强制平板 staffId，因为 verifier 只对 `staff_work_phone` 查归属）。不是新造「平板不许绑人」规则，是不把 001 最小集扩到观察 15 的班次/全设备责任人。
- 禁止只靠归还回填。
- 与 verifier 同一 PR。

禁止：发明第三种经营主体；把班次列塞进本段台账（§8.15 已观察）；用归还/送修旁路冒充领用。

非阻断措辞：候选事实栏写「PGM L611/L617」。L611 是 h2「谁领了什么设备」；绑定员工在 **L612**；责任人在 **L617**；领用在 **L618**。定稿可把行号写全，不改票。

### 重点 (c) 002/003/004/005 同根 — **PASS（同根成立；只引用、不重复发明；002「一把钥」最懒）**

| 本段 | 本席 B1 | 已定稿同根 | 候选处置 | 裁定 |
|---|---|---|---|---|
| 002 | GR-002 工作手机改读设备级公钥 | U02-004 | 以 main 为底重放本 hunk；U01/U02/U07/U12 同一 PR，客户端与后端只认一把钥 | 钥存放位置 HTML 沉默。缺陷是与 main/客户端不一致。处置不点名必须哪张表 |
| 003 | GR-003 无 deviceId 时 `terminalType=claimed` | U01-001 / U02-001 | 同 U01-001：身份由绑定/登记设备类型派生、只声明一次；无设备号的写请求 fail-close；现场资金只信派生身份 | 本段 +/- **未改** L138-149 回落（基线=副本）。须修是因为同批客户端改报头让这条接收端承载真实流量，且 U01-001 处置已含第 7 段后端。不另造终端码 |
| 004 | GR-001 两路 permitAll | U05-003 / U01-007 / U02-002 | 随 U05-003（请求带设备公钥/指纹）；公开端口加频控/来源校验；permitAll 仅在契约落地后保留 | 这两条路径 **在本段 +/- 中**（SecurityConfig 公开路径 +2 行）。`RateLimitInterceptor` RULES 无 `self-registrations`（只限登录/支付/退款）。频控句是 U05-003 已有处置，不是本段新发明 |
| 005 | GR-006 不得整树覆盖 | U05-008 / U06-007 | 以 main 445a49f4 为底重放本段 hunk + 配套测试；U01/U02/U05/U06/U07/U12 同一 PR；迁移 ≥V411（U10） | 三文件 in-file 无 M009–M012 行级补丁（`git diff 242f4872 HEAD -- 三路径` 空）；整树覆盖会回滚其它段已合入修复。合并策略，不改业务口径 |

002「只认一把钥」为何最懒：HTML 未规定公钥必须在 `staff_device_credentials` 还是 `terminal_device_signing_keys`。可选 (i) 撤回本 hunk、客户端继续签员工凭证；(ii) 留下设备级钥、main verifier 改读同一把。两条都是一把钥。指定第三张表或双钥并存是加复杂度。候选不选定哪一张，只禁止分叉——与 U02-004 定稿句同一句话，本段不重复发明。

003 虽接收端行未进 +/-，不得改登记为「main 既有/须修否」：U01-001 已把「副本 verifier 无设备号时以自报值为类型」写进须修链，且本材料客户端改写了类型头。若 003 须修否，发布者会把后端回落留给 M063、只改 U01 客户端，洞仍在本段将要合入的 verifier 里。同根须修是 = 本段与 U01-001 同一 PR 落地，不是新开根因。

### 重点 (d) U07-007 观察 — **PASS（未写错本席 §8；第 3/4/19 条在）**

007 列席甲 OBS-1…5，并以「Grok §8 1–20（含 1 三文件 main＝基线；3 grace 下错误签名走 NO_PROOF 不单独立项；4 `TerminalMasterSwitchInterceptor` 豁免名单无 self-registrations（并入 003/004）；6 `listDevices` 分页 main 既有；8 `parseMode` 非法值回落 GRACE；10 `throw unreachable()` 死代码；14/15 台账无型号列、无班次字段 HTML 沉默；17 `registerSigningKey` 注释漂移；19 副本测试 15 参构造在 main 类可编译）」收本席观察。「含」是抽样不是闭集，覆盖钥源测试属 U12、公开公司列表枚举（U05-012 OBS-4）、bind 点位字段复用、audit 失败吞掉、executeWrite 权限只写审计、TERMINAL_TYPES 三码、PGM-034/035 无实现面、kiosk token、本席未跑共享库。未把本席任何 finding 降成观察，也未把 §8 升格入队。

第 3 条：本席原文「grace 下无设备钥时，错误签名走 NO_PROOF 而非 INVALID……不比缺签名更糟……不单独立项」。007 写「不单独立项」，未立项。独立核：缺钥返回 `Outcome.noProof()`；错误签名在无钥时到不了验签函数。默认 grace。并入 002 分叉说明即可。

第 4 条：本席 B1 写「观察，并入 001/003」——当时 001/003 是 **GR-U07-001 / GR-U07-003**（permitAll 与自报类型）。正式 ID 映射后是 **U07-004 / U07-003**。候选写「并入 003/004」是换号，不是写错。独立核：`TerminalMasterSwitchInterceptor` 豁免前缀为 `/api/v1/auth/`、`capability-switches`、`/api/open/`、`kiosks/`，无 `self-registrations`。无类型头的公开 POST 得 403 `TERMINAL_NOT_ALLOWED`；U01 自报 `app` 才能过。并入 003/004，不单列。

第 19 条：未发布 `DeviceCredentialVerifierTests` 的 `profile()` / `otherProfile()` 确为 15 实参 `StaffProfileRecord(...)`（id…updatedAt）。main 同类仍有该 15 参兼容构造，故测试在 main 类上能编译；副本 record 另加 `companyId` 等（U05）。跨段发布须同 PR。007 未写成「main 已有 companyId」，也未升格入队。

席甲 OBS-1（`registerSigningKey` 不校验调用方与设备归属，归 M064）维持登记：本席 B1 只把注释漂移写成 §8.17，未把缺归属校验升 finding。007 用「OBS-1…5」收口，不丢。OBS-2 缺省 grace 与第 3 条同向。OBS-4 公开路径出处与 004 同根，不重复入队。

定稿时 007 不必把 §8 逐条抄全；不得写成「Grok 无观察」或把第 3 条改成缺陷入队。

### 001 独立裁定（冲突／须修是）

见 (b)。领用入口不写员工 vs HTML 领用/责任人/人机对应。须修是。票 **PASS**。

### 002 独立裁定（技术性同根／须修是）

副本 `resolveSigningKey` 删 `findActiveCredential`，工作手机与平板/一体机一样走设备级公钥存储。main 同行仍读员工凭证公钥。只合本段则 main 测试走 `credential_required`（本席 B1 EXP A）。技术性、无业务发明、须修是。处置只引用 U02-004「一把钥」。票 **PASS**。

### 003 独立裁定（冲突同根／须修是）

无设备号时终端类型取自报头。U01 公开注册报 `app`、其余恒 `staff_work_phone`。HTML BSM L930 三端不同用途。冲突、须修是。处置只引用 U01-001。票 **PASS**。

### 004 独立裁定（冲突同根／须修是）

`SecurityConfig.publicApiPaths` 新增两路 permitAll。HTML PGM L579 / BSM L935 安装时绑定设备标识。冲突、须修是。处置只引用 U05-003。`questions/U04-001.md` 已答「删除」与本条无新 2.2。票 **PASS**。

### 005 独立裁定（技术性同根／须修是）

三文件 HEAD=基线。覆盖不回退这三份里的 M009–M012 字节（那些修复不在此文件），但同批整树会回退其它段，且钥源与 main 测试对打。最小处置与 U05-008 / U06-007 同一句话。票 **PASS**。

### 006 独立裁定（技术性 main 既有／须修否）

见 (a)。拼接句基线 `:315` = 副本 `:312`，不在 +/-。归 M063。票 **PASS**。

### 007 独立裁定（登记／须修否）

见 (d)。票 **PASS**。

### 最小修正意见（非阻断，不改票）

1. 001 事实栏行号写成 PGM L611（标题）+ L612（绑定员工）+ L617（责任人）+ L618（领用）；BSM-029 锁出处是 L714 的 h3，句子在 L720。
2. 007 定稿保持「并入 003/004」正式号；若回溯 B1 原文，注明本席当时写的 001/003 是 GR 临时号。
3. 本席 B1 文首「发布前须修：GR-001…006」以 GR-005 正文与本票 (a) 为准，006 不进本段发布门。

逐 ID：001 PASS，002 PASS，003 PASS，004 PASS，005 PASS，006 PASS，007 PASS。发布前须修是=001/002/003/004/005，否=006/007。无添加、无 questions、无 HTML 修改申请、无 LOCK-AMEND。合取尚待席甲本批 B2；本席票单独不能定稿。材料未发布，定稿后 U07 → REVIEWED-FAIL，处置写给发布者，不做 B3/B4/B5。

本批最终：**PASS**
