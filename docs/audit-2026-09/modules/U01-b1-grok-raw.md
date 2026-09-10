CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U01 B1 Grok 独立审计原文

- 席位：Grok 独立审计席乙（固定 grok-4.6 xhigh；操作员 2026-09-09 起席甲=Claude Fable 5.1，其票是作者自审票；本席 FAIL 具否决权）
- 阶段：B1 只读全量三线（未发布材料第 1 段；不做 B3/B4/B5；处置写给发布者，本轮不修源）
- 报告自身版本：V0.01（指定路径此前不存在，新建）
- 段：U01 — 未发布材料第 1 段「安卓应用与设备密钥」
- 计划源：`docs/audit-2026-09/plan/modules.md` V0.04 末尾附录；`plan/runbook.md` V0.02 §11；控制提示词 `.scratch/module-recheck-20260908/160-audit-prompt.md` V0.17
- 审计对象（不得冒称 main 或已发布修复）：
  - 未发布提交：`03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb`（本仓 `git cat-file -t` **无此 commit 对象**）
  - 未发布树：`e28dad059b2c7e08bcc95a8db18552d3f76edf4b`（`git cat-file -t` = **tree**，可解析）
  - 发布基线：`242f487233d028173cb9198eecd079f5d877405c`（commit，可解析）
  - 只读输入根：`/home/james/complex-management/.scratch/module-recheck-20260908/unpublished-03acdb98`
  - 清单：`.scratch/module-recheck-20260908/unpublished-03acdb98-input.json` V0.01，76 文件
- 本段：3 文件，物理行 2217，标记 NORMAL；main 主归属 M375
- 冻结判据：M375 映射行并集 50 项（与锁 `criteria-lock.md` L6754 闭集相同；提示词顺序为 GEN 在前，锁行顺序为 BEQR/BSM 在前，ID 集合一致，COUNT=50 unique=50 MISSING=[]）
- 结论：**FAIL**
- 工程缺陷成立、建议入 B2 且发布前须修：GR-U01-001、GR-U01-002、GR-U01-003、GR-U01-004、GR-U01-006、GR-U01-007、GR-U01-008、GR-U01-009、GR-U01-010、GR-U01-011、GR-U01-012
- 添加项（B1 只登记，不代用户回答 2.2；B2 须逐条发问）：GR-U01-005
- 只登记、建议队列「否」：观察见 §8
- 未修源码、未改三 HTML、未改台账/锁/主索引、未 push/PR/merge/部署、未装依赖、未写永久测试、未派子代理、未调 MCP、未读本轮 self-raw 或其它本方结论、未问操作员、未对任一 `complex_test*` 库做写操作
- 本报告不表示全仓、main 模块或发布门禁通过。M001–M010 检查点已合并；M011/M012 已闭环（修复在工作区、未作为本段对象）。原 322/471 模块结论不继承到本未发布段。

## 0. 技能自核（V0.17 §6，按 B1 收窄）

本席实际读取并按收窄口径使用：

| 技能 | 本机路径 | 用法 |
|---|---|---|
| grilling | `~/.claude/plugins/cache/claude-plugins-official/mattpocock-skills/1.2.3/skills/productivity/grilling/SKILL.md` | 只用追问树自己取证，不向操作员发问、不派子代理 |
| grill-me | 同套件 `grill-me/SKILL.md` | 内部 sharpen（本体只转发 grilling） |
| diagnosing-bugs | `.../engineering/diagnosing-bugs/SKILL.md` | 先建可红反馈环（diff/后端闭集/迁移 CHECK）再下结论；不改生产代码 |
| systematic-debugging | superpowers `systematic-debugging/SKILL.md` | 只借读错误→复现→追数据流（副本三文件→U02 DeviceRequestHeaders/Workflow→未发布后端→main 对照→HTML） |
| verification-before-completion | superpowers 同名 | 先跑命令再声称；未跑的 Android 构建不写通过 |
| ponytail-review | `~/.claude/plugins/cache/ponytail/ponytail/4.9.0/skills/ponytail-review/SKILL.md` | 只列可删复杂度，不落地 |
| writing-for-agents | `.../productivity/writing-for-agents/SKILL.md` | 本 raw 写给下游席读：步骤+完成判据同文件 |
| git-guardrails-claude-code | mattpocock `misc/git-guardrails-claude-code/SKILL.md` | 存在；本席未改 git |
| long-running-background-tasks | `~/.grok/bundled/skills/long-running-background-tasks/SKILL.md` | 本段验证均秒级，未起 monitor |

B1 不适用、按 controller 收窄不调用：brainstorming / writing-plans（A）、test-driven-development / tdd / ponytail（B3）、requesting-code-review（B4）、receiving-code-review（甲席收乙席）、resolving-merge-conflicts（无合并）。

缺口（不冒称调用）：

- `batch-grill-me`：本机技能树未找到
- `ubiquitous-language`：本机技能树未找到
- Arc `audit` 会派评审子代理：与双席/§7 冲突，**不调用**
- `using-superpowers` 要求技能优先于探索：与「禁止子代理 / 本提示指定 controller」冲突处，按用户指令收窄

明确不用：executing-plans、dispatching-parallel-agents、subagent-driven-development、using-git-worktrees、finishing-a-development-branch、research、domain-modeling、MCP、第三主体。

## 1. 锁哈希与范围核验

独立命令（独占锁行已写在本文第一行，此处不重复该标签）：

- CMD-LOCK：`sha256sum docs/audit-2026-09/plan/criteria-lock.md | awk '{print $1}'`
- OUT-LOCK：`b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`
- sidecar `plan/criteria-lock.sha256` 同值
- `criteria-lock.md` 版本 V0.11；判据总数 401
- 映射行 `M375 → [BEQR-150, BEQR-153, BSM-001, BSM-002, BSM-003, BSM-012, BSM-013, BSM-026, BSM-029, BSM-044, BSM-045, BSM-046, BSM-047, BSM-048, BSM-049, BSM-050, BSM-114, BSM-115, BSM-120, GEN-001, GEN-002, GEN-003, GEN-004, GEN-005, GEN-006, GEN-007, GEN-008, GEN-009, GEN-010, GEN-011, GEN-012, GEN-013, GEN-014, GEN-015, GEN-016, GEN-017, GEN-018, PGM-001, PGM-010, PGM-032, PGM-033, PGM-034, PGM-035, PGM-036, PGM-037, PGM-038, PGM-045, PGM-046, PGM-047, PGM-066]` 与开工 50 项闭集相同
- `plan/modules.md` 附录 U01 三路径与用户清单逐字相同

HEAD：`main` = `origin/main` = `bd445728a6ec7bb36a9e7ff19fe56927410ac61e`（`rev-parse HEAD` / `origin/main` / `merge-base` 三值相等）。本席未设 `GIT_INDEX_FILE`，未动主索引。

ledger 只读核验（不改）：V2.43，453017 bytes，SHA256 `b8bc3181388ac06249e86f94881d017636588ace3a80ee2a2702aacedc53b658`（与开工提示一致）。findings V1.43，534108 bytes，SHA256 `2a38408b884dd693dfc4b1205a1f85732e385bb08338dfefa15241a3b9c89794`。台账检索 `DeviceKeystore` / `android-app` / `M375-`：**无已定稿项**覆盖本三文件。U00-000 已登记「76 文件全部与 main 分叉」为跨段事实，本席独立复核本段三文件后收窄见 §6。

其它只读哈希：

| 文件 | 字节 | SHA256 |
|---|---:|---|
| `plan/runbook.md` V0.02 | 10409 | `c4ef224d35c3708b72086621fa80251e1363bc97de0ccf90cd6606518ad6539f` |
| `plan/modules.md` V0.04 | 421826 | `8df1c96711ad31985b09f5aa0b83db889fb4d8d91a7d8e5fcb352336181b5ffa` |
| `160-audit-prompt.md` V0.17 | 65856 | `c93ddca061831828a5dcabb1c83fe18a4a6b7a2a6cd839e3baaef63c66ad6f21` |
| `docs/business-enum-quick-reference-v0.1.html` | 133961 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |
| `docs/business-system-map-for-operations-v0.1.html` | 304482 | `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 103015 | `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c` |

三 HTML 只读、未改。锁内本段用到的锚点行对本席读取的现行 HTML **全部命中**（见 §1.1）。三 HTML 本身未发现需人闸修改的原文错误。HTML 沉默处不升冲突。

### 1.1 HTML 锚点实核（锁出处行 → 现行正本）

| 判据 | 锁出处 | 现行该行含 |
|---|---|---|
| BEQR-150 | beqr:233 | `终端十一端` |
| BEQR-153 | beqr:236 | `终端开关优先级` |
| BSM-001 | bsm:505 | `整体说明：这套系统到底管什么` |
| BSM-002 | bsm:526 | `一笔业务怎么走` |
| BSM-003 | bsm:563 | `系统通用能力：数据可改可删、操作有引导、权限分岗` |
| BSM-012 | bsm:611 | `酒吧平板/工作手机` |
| BSM-013 | bsm:616 | `触摸屏一体机` |
| BSM-026 | bsm:686 | `各类入口` |
| BSM-029 | bsm:714 | `和谁有关` |
| BSM-044 | bsm:929 | `酒吧平板/工作手机：平板点酒，工作手机核销` |
| BSM-045 | bsm:933 | `主要功能` |
| BSM-046 | bsm:953 | `平板图形点单（POS）` |
| BSM-047 | bsm:963 | `和谁有关` |
| BSM-048 | bsm:978 | `触摸屏一体机：权益领取、现场服务和辅助售票` |
| BSM-049 | bsm:982 | `主要功能` |
| BSM-050 | bsm:996 | `和谁有关` |
| BSM-114 | bsm:1827 | `各类入口：不同人只看自己该看的、只做自己该做的` |
| BSM-115 | bsm:1897 | `相互关系：为什么这些子系统必须连在一起` |
| BSM-120 | bsm:1957 | `已确认规则和实施前输入项` |
| PGM-001 | pgm:256 | `整体定位：平台治理层不是一个业务部门，而是所有业务的底座` |
| PGM-010 | pgm:372 | `终端开关` |
| PGM-032 | pgm:572 | `员工和手机：人、私人手机、工作手机要能对应起来` |
| PGM-033 | pgm:576 | `主要功能` |
| PGM-034 | pgm:586 | `工作手机和企业微信` |
| PGM-035 | pgm:599 | `提成发放和人员服务治理` |
| PGM-036 | pgm:611 | `设备管理：谁领了什么设备，设备能不能用` |
| PGM-037 | pgm:615 | `主要功能` |
| PGM-038 | pgm:624 | `和现场业务的关系` |
| PGM-045 | pgm:712 | `终端开关：哪些端能打开，后台说了算` |
| PGM-046 | pgm:716 | `能控制哪些端` |
| PGM-047 | pgm:728 | `怎么控制` |
| PGM-066 | pgm:951 | `已拍板规则和待排期问题`（含「界面全中文」） |

## 2. 3 文件覆盖表（全文已读；行=`\n` 计数 = splitlines）

副本根：`unpublished-03acdb98/`。清单三条目 sha256/行/字节与独立 `sha256sum`/`wc` **3/3 match**。副本 `git hash-object` 与树 `e28dad05:<path>` blob **3/3 相同**。

| # | 路径 | 行 | 字节 | SHA256 | 全文 |
|---|---|---:|---:|---|---|
| 1 | `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/DeviceKeystore.kt` | 56 | 2201 | `b97b14ca6bd0e260b4afb9fc784e555c2c4f2622f79120d96303163b09ab86d2` | 已读 L1–L56 |
| 2 | `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | 1842 | 83960 | `29e48ea029e79a6c6d8cf5d8ae07151c9e1ade93079b4e76542cbde57eccad97` | 已读 L1–L400、L401–L800、L801–L1197、L1198–L1842（结构段覆盖，无抽样） |
| 3 | `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | 319 | 14492 | `45d886f2b0ca94230a27b883a3cae3d23c13f83ffe63f682c99e308e35d21e90` | 已读 L1–L319 |

合计 2217 行 / 100653 字节。android-app 模块内无测试文件。

### 2.1 相对发布基线的逐文件 diff stat（真实改动）

命令：`git diff --numstat 242f487233d028173cb9198eecd079f5d877405c e28dad059b2c7e08bcc95a8db18552d3f76edf4b -- <path>`

| 路径 | + | − | shortstat |
|---|---:|---:|---|
| DeviceKeystore.kt | 12 | 0 | 12 insertions |
| MainActivity.kt | 124 | 76 | 200 行 hunk 合计 |
| TerminalApiClient.kt | 19 | 1 | 20 行 hunk 合计 |
| **本段合计** | **155** | **77** | 3 files changed |
| **全 76 文件** | **4745** | **1084** | 与提示一致 |

blob：

| 路径 | 基线=HEAD(main) git blob | 未发布树/副本 blob |
|---|---|---|
| DeviceKeystore.kt | `d44ae47c41f7280773dc81b189ac0e8fe23e69e4` | `7a07c585ec11be612b46bfa891afb881f7020a83` |
| MainActivity.kt | `eb8b34e643f426a88b55c5f16882027255a14dac` | `b50c331823ab3229acb3663b351717ca9e8ef9c2` |
| TerminalApiClient.kt | `58a040e4842b1dece76ce7eb7ad049045158adb8` | `e3d0cc3fae7c9b6203599eb7fa508f09c74a818d` |

`git diff 242f4872 HEAD -- <三路径>` 为空：**当前 main 这三文件与发布基线逐字节相同**。未发布相对 main 的 diff 即上表的逆。

本段相对基线的实质新增（不只版本注释）：

- DeviceKeystore：`sign()`（SHA256withECDSA + Base64.NO_WRAP）
- TerminalApiClient：`deviceId`/`deviceSigner`、`registerDeviceSigningKey`、`listStaffRegistrationCompanies`/`submitStaffSelfRegistration`、自助注册两路 `X-Terminal-Type=app`、其余恒 `staff_work_phone`、`send()` 附加 `deviceRequestHeaders`
- MainActivity：工作手机编号持久化、登录后若已有 deviceId 跳过绑定、绑定后登记签名公钥、公司员工自助注册屏、充电断开打卡、NFC/扫码回调改 `nfcResult`/`scanResult`、考勤屏改为「领取工作手机」、商户代提交注册改字段、POS 绑定仍用 `bar_pos`

## 3. 数据流（本段 → 未发布其它段 → main 对照 → SQL/HTML）

```
MainActivity
  ├─ DeviceKeystore.devicePublicKeyBase64 / sign / deviceFingerprintHash
  ├─ TerminalApiClient (android-app HttpURLConnection 版)
  │     ├─ open(): X-Terminal-Type + Bearer
  │     └─ send(): deviceRequestHeaders()  ← U02 新文件
  │           apps/android-terminal/src/main/kotlin/.../DeviceRequestHeaders.kt
  ├─ TerminalWorkflow / TerminalModels / InMemoryLocalStore
  │     （android-app build.gradle.kts sourceSets 复用 ../../src/main/kotlin，排除 JVM TerminalApiClient）
  └─ 后端（未发布副本只读）
        TerminalDeviceController.bind / signing-key
        DeviceCredentialVerifier  （登记 terminal_type 压过自报头）
        ServiceOrderController.requireCompanyTerminal  （仍读自报 X-Terminal-Type）
        StaffRegistrationService.selfRegister
        StaffService.punch  （未发布白名单 nfc/qr/charging_disconnect）
        V405 chk_att_method 含 charging_disconnect
```

main 对照（只读，非本段修复对象）：

- `DeviceKeystore.kt` **无** `sign()`
- `TerminalApiClient.kt` **无** device 签名头；`X-Terminal-Type` 恒 `staff_work_phone`
- `StaffService.punch` 白名单 `nfc/qr/manual`；缺省 method=`manual`
- `V210` `chk_att_method CHECK (method IN ('nfc','qr','manual'))`；**无 V405**
- `TerminalDeviceService.TERMINAL_TYPES = {staff_work_phone, tablet, kiosk}`（未发布副本同文）
- `DeviceCredentialVerifier.DEVICE_BOUND_TYPES = {staff_work_phone, tablet, kiosk}`（无 `bar_pos`）
- 后端 `registerDeviceSigningKey` 在 main **已存在**（本段客户端才开始调用）

android-app 构建把共享 JVM 源编进 APK，故 U01 的 `deviceRequestHeaders` 调用依赖 **同一未发布树里的 U02 新文件**。只发布本段 3 文件、不带 `DeviceRequestHeaders.kt`，Android 模块无法解析该符号。

## 4. 三线审计

### 4.1 第一性原理（每文件三问）

#### DeviceKeystore.kt（L1–L56）

1. **为何必须存在**：BSM-045/PGM-033「安装时生成并绑定该手机专属设备标识，不读取硬件识别码」。AndroidKeyStore EC 密钥 + SHA-256(公钥 DER) 是该要求的下位实现。删掉则无设备公钥/指纹，员工设备凭证与请求签名失去本地密钥。有上位。
2. **声称是否执行**：声称 EC P-256、私钥不可导出、`sign()` 用 SHA256withECDSA。代码：`KEY_ALGORITHM_EC` + `DIGEST_SHA256` + `PURPOSE_SIGN|VERIFY`；**未**显式 `ECGenParameterSpec("secp256r1")`（Android 文档默认 P-256，属未在本机 AndroidKeyStore 上跑通的前提）。`sign()` 为相对基线新增，main 无此函数。
3. **未验证前提**：「硬件保护」在无 StrongBox 的设备上是软件 TEE/TEE 模拟；本席无仪器测试。无用户认证、无 attestation。本段无测试调用 `sign()`。

#### TerminalApiClient.kt（L1–L319）

1. **为何必须存在**：Android SDK 无 `java.net.http`，需 HttpURLConnection 版与 JVM 客户端同签名。有上位（现场端必须能打 HTTPS API）。
2. **声称是否执行**：https/loopback + allowedHosts 在 `init` 强制。业务方法覆盖核销/酒吧/考勤/薪资/服务点单/设备凭证。新增签名头仅在 `deviceId!=null && deviceSigner!=null` 且非登录/自助注册两路时附加。`catalogImageBytes` 走 `open()` **绕过** `send()`，因此 **不签**设备头。
3. **未验证前提**：`X-Terminal-Type` 除两路自助注册发 `app` 外恒 `staff_work_phone`，与 POS `bar_pos`、私人手机、平板用途矛盾（GR-U01-002）。`deviceRequestHeaders` 不在本文件，依赖 U02。本席未跑 Android 编译。

#### MainActivity.kt（L1–L1842 全文）

1. **为何必须存在**：BSM-002 步骤 5、BSM-012/044 工作手机核销与平板点酒、PGM-033 私人手机自助注册。作为现场 UI 编排有上位。PGM-047 明文允许「同一个员工 App」按设备类型放权——**一份 APK 本身不是冲突**；冲突是首页不放权。
2. **声称是否执行**：注释写「能力分级由后台按设备类型下发」（约 L716–717），`showHome()`（L284–317）**无条件**列出核销、平板 POS、一体机检修、BYOD、服务点单。登录页新增自助注册。绑定工作手机后才 `registerDeviceSigningKey`。充电广播在 `onCreate` 即注册。
3. **未验证前提**：POS `terminalType="bar_pos"` 会被未发布/main 后端 `TERMINAL_TYPES` 拒绝（GR-U01-003）。拔充电即打卡依赖未发布 `StaffService`/`V405`，对当前 main 必 400/CHECK 失败（GR-U01-005/012）。自助注册不调用 DeviceKeystore、不绑定员工设备凭证（GR-U01-006）。

### 4.2 顺从式（50 条逐项）

| ID | 本段适用面 | 结论 |
|---|---|---|
| GEN-001 | 三文件均有现场端/设备身份上位 | Keystore/ApiClient 有必要。首页堆叠三类入口+一体机检修超出「只做/只负责」→ **FAIL** GR-U01-001 |
| GEN-002 | 声称 POS 可绑定、请求可签、充电可打卡、自助注册可提交 | **FAIL** GR-U01-003/004/012；充电对 main 不可执行 |
| GEN-003 | 本段 0 个测试文件；U02 `DeviceRequestHeadersTest` 只覆盖 canonical，不覆盖 Keystore.sign / 类型头 / 充电 / bar_pos / 自助注册 UI | **FAIL** GR-U01-011 |
| GEN-004 | 扫码/NFC/充电/注册证件号/catalogImageBytes | 充电把 `ACTION_POWER_DISCONNECTED` 当上班领取 → **FAIL** 并入 GR-U01-005。catalogImageBytes 信任服务端 imageUrl，夹具投毒才 SSRF，收窄为观察 |
| GEN-005 | 现场资金/核销必须公司工作手机；终端开关+权限 | 自报头冒充工作手机 + 首页不放权 → **FAIL** GR-U01-001/002 |
| GEN-006 | 绑定+登记公钥同一 `runBackground` | 后一步失败则整段 onError、服务端可能已 bind；同权限点下首次不必然发生。观察，不升 |
| GEN-007 | 请假/排班/服务单幂等键 `System.currentTimeMillis()` | 单击首次成功不撞；重试换键。铁律 6 收窄，观察 |
| GEN-008 | 终端类型码、考勤 method、签名 canonical | **FAIL** bar_pos vs tablet；`app` vs 十一端中文名；charging vs main 闭集。GR-U01-002/003/012。canonical 与未发布 `DeviceCredentialVerifier` 的 `deviceId\\ntimestamp\\nnonce\\nMETHOD\\nURI\\nsha256(body)` 一致（U02 测试正文 + 本席 python digest） |
| GEN-009 | 绑定失败提示、ApiException 转中文 | 友好词存在。POS 绑定失败会露出后端英文 `terminal_type is invalid`（经 message 透出）并入 GR-U01-003/009 |
| GEN-010 | Keystore 私钥不导出；FLAG_SECURE 薪资屏；证件号提交 | Keystore 方向对。NFC `FLAG_MUTABLE` 为系统写入 extras 的既有要求，不升。PII 注册字段明文提交给后端属注册必要，观察 |
| GEN-011 | workflow 写本地事件；后端 audit 在绑定/打卡 | 本段 UI 不展示留痕。无实现面于「可点击溯源」全链（PGM-001 见名可点主要在后台）。不单独开 finding |
| GEN-012 | 超时 10s/30s；购物车内存 | 相称。通过 |
| GEN-013 | 本段无 gradle/CI 文件 | **无实现面**：构建在 M375 其它文件。android-app `build.gradle.kts` 不在本段 |
| GEN-014 | 本段无 SQL | **无实现面**。跨段：充电依赖未发布 V405，记 GR-U01-012 |
| GEN-015 | 展示全中文 | **FAIL** GR-U01-009 |
| GEN-016 | 文件头版本注释、注释中的接口路径 | 路径与 `post/get` 字符串一致。通过（不把注释当 HTML） |
| GEN-017 | 分类纪律 | 充电打卡走添加；bar_pos 错码走技术性；混入口走冲突。未把 HTML 沉默当删除 |
| GEN-018 | 本报告依据 | 类型 1 HTML 逐字 + 类型 4 命令。未用历史席位意见 |
| BEQR-150 | 十一端分端 | 客户端用 `app`/`staff_work_phone`/`bar_pos`，不是十一端中文名；`bar_pos` 不在后端闭集。**FAIL** GR-U01-002/003 |
| BEQR-153 | 开关优先级与兜底关闭 | 客户端不拉开关、把拒绝交给后端（注释 3.19）。类型头错误会使后端按错误端执行开关。并入 GR-U01-002。拉取失败兜底关闭无客户端实现面（后端拦截器） |
| BSM-001 | 基础盘子 vs 经营动作 | 本段是核销/点酒/人员入口，不是综合体盘子页。有经营动作上位。不因无拓扑页升冲突 |
| BSM-002 | 步骤 5 工作手机核销、平板点酒 | 功能入口都在，但同一首页同时给三类设备。**FAIL** GR-U01-001 |
| BSM-003 | 锁摘录仅为 h2+导语；分岗/引导细节分属未映射子条 | 有分步提示与确认框。分岗未做 UI 隐藏，并入 GR-U01-001。不把未映射的存档删除子条当本段冲突 |
| BSM-012 | 「平板只做点酒；工作手机负责核销」 | **FAIL** GR-U01-001 |
| BSM-013 | 一体机卡片 | 本 APK 提供检修/游乐核销，HTML 归一体机。**FAIL** 并入 GR-U01-001 |
| BSM-026 | 各类入口由后台统一开关 | 菜单不按开关隐藏。辅助层缺失，后端应硬拒。并入 GR-U01-002（类型头错则开关对象错） |
| BSM-029 | 权限要知道设备绑定/私人手机/工作手机 | 绑定与凭证屏存在；私人手机安装绑定缺失 GR-U01-006 |
| BSM-044 | 三类设备不同用途；现场动作只在工作手机 | **FAIL** GR-U01-001/002。PGM-047 允许同一 App 按设备放权，本段未做放权 |
| BSM-045 | 安装绑定标识、换机先解绑、破解失效、平板绑定酒吧、支付码、工作手机核销 | **FAIL** GR-U01-006/007/008/010。Keystore 不读 IMEI，符合「不读硬件识别码」 |
| BSM-046 | 首次绑酒吧、平面图选桌、图片分类、购物车整桌结账 | 绑酒吧/分类卡片/购物车在。选桌是两列方块不是平面图→观察（点选桌台功能在）。结账无支付码 **FAIL** GR-U01-010 |
| BSM-047 | 平板只点酒支付；工作手机只核销 | **FAIL** GR-U01-001 |
| BSM-048/049/050 | 一体机权益/售票/订台/检修 | 本段有检修+主管确认，无售票一体机能力。把检修放进员工 App 并入 GR-U01-001。缺售票不升冲突（本段不是一体机模块的全部） |
| BSM-114 | 入口表：工作手机/私人手机/平板各做什么 | **FAIL** GR-U01-001。私人手机行禁止代客下单/收款码/履约/核销，首页仍展示 |
| BSM-115 | 平板连酒吧须校验最低消费；工作手机连留痕 | 低消在订位列表展示，POS 提交不校验。后端 `BarTableSessionService` 有低消快照，属服务端可能拦截。客户端遗漏收窄为观察。留痕走后端 |
| BSM-120 | 已拍板私人手机绑定/破解失效/工作手机管控/一体机检修/全中文 | 绑定/破解 **FAIL** 006/007；全中文 **FAIL** 009；检修入口放错设备并入 001。MDM 预装不在本三段 |
| PGM-001 | 归属/资格/责任；界面全中文在 PGM-066 | 设备绑定是资格的一部分。中文 **FAIL** 009 |
| PGM-010 | 终端开关卡片含工作手机/平板/私人手机 | 客户端不实现开关配置（后台事）。类型头错误并入 002 |
| PGM-032 | 人、私人手机、工作手机对应 | 工作手机 bind+prefs；私人手机安装绑定缺失 **FAIL** 006 |
| PGM-033 | 两条注册路径；安装绑定；换机解绑；破解失效 | 自助注册+商户代提交有。后三项 **FAIL** 006/008/007 |
| PGM-034 | 工作手机必须企业微信 | 本段无企微 SDK。沟通可走独立企微客户端；HTML 未要求本 APK 内嵌会话。**无实现面**（不升冲突） |
| PGM-035 | 提成月结、不混营销打卡进考勤 | 薪资/提成明细屏在。充电打卡不是营销打卡。添加项见 005 |
| PGM-036/037 | 设备台账/领还 | 客户端 bind/return API 存在；POS 自造 `BARPOS-${timestamp}` 观察。充电当作领取并入 005 |
| PGM-038 | 工作手机核销；平板只点酒；一体机检修 | **FAIL** GR-U01-001 |
| PGM-045/046 | 各端能关 | 无客户端拉开关。并入 002 |
| PGM-047 | 关后菜单不可见+接口拒绝；同一 App 按设备类型下发 | 菜单全开、类型头恒工作手机 → 放权无法发生。**FAIL** 001/002 |
| PGM-066 | 界面全中文（安卓端） | **FAIL** GR-U01-009 |

### 4.3 对抗式（grilling 自问自答，不派子代理）

Frontier 已结算，不向操作员发问：

1. **发布这三文件会不会覆盖 main 上 M009–M012 已修项？** 这三路径 main blob = 基线 blob，M009–M012 未改它们。发布未发布树会把 main 的旧实现换成未发布实现，**不会**覆盖拓扑/迁移/网页那批已修文件。固定问题的真实形态是：**同路径无已修可覆盖；跨路径会把未发布客户端行为打到仍停留在 V210/nfc-qr-manual 的 main 后端上**（GR-U01-012）。
2. **第一次打开首页，用户能同时点核销、POS、代客点服务吗？** 能。`showHome()` 无设备类型/开关过滤。首次正常会。
3. **第一次平板 POS「绑定并进入点单」后端收什么 terminal_type？** `"bar_pos"`。未发布与 main `TerminalDeviceService.bind` 均 `TERMINAL_TYPES.contains` 失败 → `VALIDATION_FAILED` `terminal_type is invalid`。首次正常会。
4. **POS 绑定成功后（若类型被改成 tablet）后续请求签谁的名？** `onSuccess` 只写 `posPrefs.bar_id`，**不**写 `api.deviceId`、**不** `registerDeviceSigningKey`。签名仍用工作手机 prefs 或空。首次 POS 路径会。
5. **私人手机登录后点「代客点服务」，X-Terminal-Type 是什么？** `staff_work_phone`。未发布 `ServiceOrderController.CREATE_AND_CODE_TERMINALS` 含该值，现场资金闸按工作手机放行。首次正常会。与 BSM-044「私人手机不允许现场收银」冲突。
6. **登录并绑定工作手机后拔充电器？** `chargingReceiver` 在 onCreate 已注册；`recordWorkPhoneReceipt("charging_disconnect")`。未发布 `StaffService` 接受该 method 且要求已验证工作手机；当前 main 拒绝该 method。在未发布全树下，家中拔充电会当上班领取（无点位标签）。首次绑定后只要拔充电就会走这条，不是两三件巧合。
7. **自助注册第一次是否生成并绑定设备标识？** `showStaffSelfRegistration` 只 POST 姓名/公司等，不调 DeviceKeystore，不调 `bindStaffDeviceCredential`。首次注册不会绑定。
8. **员工设备凭证屏能否换机？** 只有「绑定设备凭证」，无解绑、无 `approveStaffDeviceRebind`、无完整性上报。换机先解绑做不到。
9. **破解检测会不会在正常机首次启动跑？** 代码里没有检测。缺的是能力本身，不是极端路径。
10. **用户看见的状态有没有英文？** `homeSummary` 直接 `b.status`；渠道「微信 wechat」；分组「BYOD 一期」；游乐 `verificationResult` 原文；工单 `o.status`。首次打开首页/POS 结账即见。
11. **极端：并发双开绑定、Keystore 在 x86 模拟器无硬件、imageUrl 投毒。** 双开非首次必撞；无硬件不否定默认 P-256；imageUrl 需目录被写毒。铁律 6 不另造闸。
12. **commit 03acdb98 不在对象库是否影响 diff？** 不影响。改动按树 `e28dad05` 对基线计算，与提示一致。

ponytail-review（只列不改）：

- `showHome` 一张清单代替三类放权：该删的是无条件按钮，不是整份 APK（PGM-047 允许同一 App）。
- `chargingReceiver` 全局生命周期：可删，改回点位 NFC/扫码。
- POS 默认 `BARPOS-${timestamp}`：可删，改为只填已台账编号（工作手机绑定已改成这样）。
- `catalogImageBytes` 静默吞错：可保留降级，但应限制 path 前缀。

## 5. Findings（临时 ID，正式 ID 仅 B2 分配）

### GR-U01-001

- **标题**：同一首页无条件提供工作手机核销、平板 POS、私人手机 BYOD 与一体机检修，未按设备类型放权
- **文件:行号**：`MainActivity.kt:284-317`（`showHome`）；`MainActivity.kt:716-717`（声称后台按设备类型下发，代码未做）；`MainActivity.kt:276`（「跳过，进入首页」）；`MainActivity.kt:1036-1126`（代客点服务无 `requireBindingOr`）
- **完整原文**：`button("门票/权益核销")` … `button("平板点单 POS（图形）")` … `button("游乐检修提交 / 主管确认")` … `statusText("—— 员工自有手机(BYOD 一期) ——")` … `button("代客点服务")`
- **业务影响**：平板/工作手机/私人手机在 HTML 中是不同用途。现场收银与核销出现在未绑定、也未声明工作手机的会话里；一体机检修出现在员工 App。BSM-012/044/114/PGM-038/047 的「只做/只负责/按设备下发」在第一次打开首页即被打破。
- **可复现命令/最小反例**（类型 1+4）：读 `showHome` 无 `if (terminalType)`；对照 HTML BSM-044 导语与 BSM-114 表三行。`python3 -c` 统计 `showHome` 内 `button(` 数量=22 且无设备类型分支。
- **冻结判据**：BSM-012、BSM-044、BSM-047、BSM-114、PGM-038、PGM-047、GEN-001、GEN-005
- **2.3 依据类型**：1（HTML 明文「平板只做酒吧点酒下单；工作手机负责门票和权益扫码核销」；「必须在现场完成的操作……一律在公司工作手机上做」；PGM-047「能做什么由后台按设备类型统一下发」）
- **四分类**：**冲突**
- **工程缺陷**：是
- **建议入队 / 发布前须修**：是
- **最小处置（给发布者）**：按绑定/登记的终端类型（工作手机 / tablet / 私人手机）过滤首页；未绑定工作手机不得出现核销与代客收银；平板模式只留点酒结账；检修留在一体机。PGM-047 允许同一 APK，但必须真正按类型下发。

### GR-U01-002

- **标题**：除自助注册两路外，所有请求 `X-Terminal-Type` 恒为 `staff_work_phone`，私人手机与平板自报成工作手机
- **文件:行号**：`TerminalApiClient.kt:277`；对照 JVM 副本 `apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt:298-300`（同逻辑，U02）；未发布 `ServiceOrderController.java:35-53` 仍读取自报头
- **完整原文**：`connection.setRequestProperty("X-Terminal-Type", if (path == "/api/v1/staff/registration-companies" || path == "/api/v1/staff/self-registrations") "app" else "staff_work_phone")`
- **业务影响**：未发布验签器虽改为「登记类型压过自报」，服务点单现场闸仍信自报头。私人手机发 `staff_work_phone` 即可做下单/收款码。平板 POS 也报工作手机，开关矩阵与留痕记错端。BEQR-150 十一端、BSM-044 私人手机禁现场、PGM-047 接口按端拒绝均失效。
- **可复现命令/最小反例**（类型 4）：

```text
# 副本源码固定值（不连网）
path=/api/v1/services/orders  -> staff_work_phone
path=/api/v1/bar/orders       -> staff_work_phone
path=/api/v1/staff/self-registrations -> app
# 未发布 ServiceOrderController.CREATE_AND_CODE_TERMINALS
#   = {staff_work_phone, web, tablet, app}  → 自报 staff_work_phone 放行
```

- **冻结判据**：GEN-005、GEN-008、BEQR-150、BSM-044、BSM-114、PGM-047
- **2.3 依据类型**：1+4
- **四分类**：**冲突**
- **工程缺陷**：是
- **建议入队 / 发布前须修**：是
- **最小处置**：按真实设备模式发 `staff_work_phone` / `tablet` / 与后端闭集一致的私人手机码；现场资金接口改为只信 `DeviceCredentialVerifier` 派生类型，不信自报头（后端属其它段，发布须一起改）。

### GR-U01-003

- **标题**：平板 POS 绑定使用 `terminal_type=bar_pos`，绑定 API 只接受 `staff_work_phone|tablet|kiosk`
- **文件:行号**：`MainActivity.kt:1279-1287`；未发布与 main 同源 `TerminalDeviceService.java:49,638-640`
- **完整原文**：`terminalType = "bar_pos"`；`private static final Set<String> TERMINAL_TYPES = Set.of("staff_work_phone", "tablet", "kiosk");` + `throw ApiException.badRequest("VALIDATION_FAILED", "terminal_type is invalid");`
- **业务影响**：第一次「绑定并进入点单」必 400。BSM-046「平板第一次使用，先绑定自己所属的那家酒吧」在可执行层失败。
- **可复现命令/最小反例**（类型 4）：`git grep -n 'bar_pos' <副本 MainActivity>` 对 `git grep -n 'TERMINAL_TYPES' <副本 TerminalDeviceService>`。无需库。
- **冻结判据**：GEN-002、GEN-008、BSM-046、BEQR-150
- **2.3 依据类型**：4
- **四分类**：**技术性**
- **无业务发明理由**：HTML 要的是「工作人员平板」；后端已有 `tablet`。缺陷是客户端码与闭集不一致，没有新终端种类。
- **工程缺陷**：是
- **建议入队 / 发布前须修**：是
- **最小处置**：POS 绑定改发 `tablet`（或与开关/灰度词表对齐后的单一码），禁止 `bar_pos`。

### GR-U01-004

- **标题**：POS 绑定成功不设置 `api.deviceId`、不登记设备签名公钥
- **文件:行号**：`MainActivity.kt:1288-1293`（POS onSuccess）；对照工作手机绑定 `MainActivity.kt:260-266`（有 register + `api.deviceId=d`）；`TerminalApiClient.kt:286-289`（无 deviceId 则不签名）
- **完整原文**：POS `onSuccess` 仅 `posPrefs.edit().putString("bar_id", bar)` / `posBarId = bar` / `showPosTables()`。
- **业务影响**：即使 bar_pos 被改成合法类型，平板后续下单/结账仍无 `X-Device-*`，或误用工作手机的 deviceId。enforce 模式下写请求缺凭证；grace 模式下留痕记错设备。
- **可复现命令/最小反例**（类型 4）：对比两处 `onSuccess` 是否赋值 `api.deviceId`。
- **冻结判据**：GEN-002、GEN-008、PGM-001（设备是否合规）
- **2.3 依据类型**：4
- **四分类**：**技术性**
- **无业务发明理由**：签名与设备号已是工作手机路径的既有机制，POS 漏接。
- **工程缺陷**：是
- **建议入队 / 发布前须修**：是
- **最小处置**：POS 绑定成功后写入 `api.deviceId`、调用 `registerDeviceSigningKey`，与工作手机路径对称。

### GR-U01-005

- **标题**：将「取离充电设施」发明为考勤上班领取方式
- **文件:行号**：`MainActivity.kt:99-124`、`L129-131`、`L853`；未发布 `StaffService.java:846-854`；未发布 `V405__staff_work_phone_receipts.sql`；main `StaffService.java` 约 L724 `nfc/qr/manual`；main `V210` CHECK 无 `charging_disconnect`
- **完整原文**：`if (intent?.action == Intent.ACTION_POWER_DISCONNECTED) recordWorkPhoneReceipt("charging_disconnect")`；文案「领取工作手机即记录上班；碰 NFC、扫码或取离充电设施，以最早一次为准。」
- **业务影响**：HTML 考勤/领用未写充电桩事件。未发布全树下，已登录已绑定的工作手机在家拔充电即 `check_in`（无点位标签，服务端对 charging 豁免 tagRef）。打到当前 main：method 不在 `nfc/qr/manual`，400；即便绕过服务校验也会撞 V210 CHECK。
- **可复现命令/最小反例**（类型 1+4）：

```text
# main（当前 HEAD）
grep charging_disconnect backend/src/main/java/com/complex/platform/access/staff/StaffService.java
# 无
sed -n '32p' backend/src/main/resources/db/migration/V210__staff_shift_attendance.sql
# CONSTRAINT chk_att_method CHECK (method IN ('nfc', 'qr', 'manual'))
# 未发布副本
grep charging_disconnect .../StaffService.java  # 有白名单
# V405 不在 main；main 已有 V410
```

- **冻结判据**：GEN-004、GEN-017、PGM-036、BSM-045（考勤打卡存在，但方式清单未含充电）
- **2.3 依据类型**：1（三份 HTML 无「充电/充电设施/拔电」考勤原文；最近对照 PGM-033/BSM-045 只有 NFC/扫码类现场打卡与设备领用，未把 POWER_DISCONNECTED 定义为领取）
- **四分类**：**添加**
- **工程缺陷**：是（作为添加项的实现还把不可信的系统广播当出勤）
- **建议入队 / 发布前须修**：B2 按 2.2 询问；在得到「保留」前不得随 U01 发布到 main
- **最小处置**：默认删除充电广播；考勤只保留 NFC/扫码。若操作员保留，须先写入三份 HTML，并与 V405/StaffService 同树发布，且不得在工作场所外把拔电记为上班。
- **添加项 2.2 草稿（B2 发问用，本席不代答）**：
  - HTML 原文：三份 HTML 中无任何「充电设施/拔电记考勤」条目；最接近为 BSM-045「考勤打卡」与 PGM-036「记录设备领用」。
  - 添加了什么：`ACTION_POWER_DISCONNECTED` → `AttendancePunchRequest(method=charging_disconnect, punchType=check_in)`。
  - 添加后的代价：家中拔电可记上班；与 main 考勤闭集不兼容；需新迁移改 CHECK。
  - 删除的代价：失去「放回充电桩算下班/取离算领取」的未书面口径；需改回 NFC/扫码领取。

### GR-U01-006

- **标题**：私人手机自助注册/安装路径不生成、不绑定该手机专属设备标识
- **文件:行号**：`MainActivity.kt:1188-1213`（自助注册）；`MainActivity.kt:226`（登录页入口）；`DeviceKeystore.kt:18-40`（密钥只在后续凭证/绑定时才用）；`MainActivity.kt:563-588`（登录后手填员工档案再绑）
- **完整原文**：自助注册只提交 `StaffSelfRegistrationRequest(requestKey, selected.id, name, merchant, phone, idDoc)`，无公钥/指纹。
- **业务影响**：PGM-033/BSM-045/BSM-120「安装时生成并绑定该手机专属设备标识，把手机和员工绑定；不读取硬件识别码」在第一次自助注册不发生。换机/破解复核没有对象。
- **可复现命令/最小反例**（类型 1+4）：`showStaffSelfRegistration` 函数体无 `DeviceKeystore` 引用。
- **冻结判据**：PGM-033、BSM-045、BSM-120、GEN-002
- **2.3 依据类型**：1
- **四分类**：**冲突**（明文要求的安装时绑定被遗漏，首次注册即缺）
- **工程缺陷**：是
- **建议入队 / 发布前须修**：是
- **最小处置**：安装或自助注册成功时用 Keystore 公钥/指纹调用员工设备凭证绑定（服务端按 token 派生本人，禁止手填他人档案 ID）。

### GR-U01-007

- **标题**：无破解/篡改/异常环境检测；`reportStaffDeviceIntegrity` 未接线
- **文件:行号**：`TerminalApiClient.kt:245-249`（API 有）；`MainActivity.kt` 全文 0 处调用；无 SafetyNet/Play Integrity/root 检测
- **完整原文**：HTML「检测到手机被破解、篡改或处于异常环境时，私人手机 App 失效并复核。」客户端只有上报 API，没有检测与调用。
- **业务影响**：被破解的私人手机仍可登录做 BYOD 与（因 002）现场动作。
- **可复现命令/最小反例**（类型 4）：`grep -n reportStaffDeviceIntegrity MainActivity.kt` 空；`grep -nE 'SafetyNet|PlayIntegrity|isRooted|integrity' MainActivity.kt DeviceKeystore.kt` 空。
- **冻结判据**：BSM-045、PGM-033、BSM-120、GEN-002
- **2.3 依据类型**：1+4
- **四分类**：**冲突**（要求的自动失效能力不存在）
- **工程缺陷**：是
- **建议入队 / 发布前须修**：是
- **最小处置**：在私人手机模式启动时做完整性检测，失败则调 `reportStaffDeviceIntegrity` 并失效会话。

### GR-U01-008

- **标题**：换手机没有「先解绑再绑定」；`approveStaffDeviceRebind` 未接线
- **文件:行号**：`MainActivity.kt:563-588` 仅 `bindStaffDeviceCredential`；`TerminalApiClient.kt:239-243` 有审批换绑 API 但 UI 不用
- **完整原文**：`button("绑定设备凭证")` 唯一动作。
- **业务影响**：PGM-033/BSM-045「换手机必须先解绑旧手机，再绑定新手机」在 UI 不可执行。
- **可复现命令/最小反例**（类型 4）：`grep -n '解绑\\|approveStaffDeviceRebind\\|returnDevice' MainActivity.kt` 无解绑/换绑入口（`returnDevice` 仅在 ApiClient）。
- **冻结判据**：PGM-033、BSM-045、BSM-120
- **2.3 依据类型**：1+4
- **四分类**：**冲突**
- **工程缺陷**：是
- **建议入队 / 发布前须修**：是
- **最小处置**：凭证屏增加解绑/申请换绑；新机绑定必须在旧机解绑或审批通过之后。

### GR-U01-009

- **标题**：用户可见英文与原文状态码
- **文件:行号**：`MainActivity.kt:325`（`b.status` 原文）；`L297`（`BYOD 一期`）；`L394`（`r.verificationResult`）；`L437-438`/`L473`（订单/支付意图 `status`）；`L765`（工单 `o.status`）；`L1558-1559`（`微信 wechat` / `支付宝 alipay`）；`L203`（登录展示 `API: https://...`）
- **完整原文**：`if (b?.status != null) append("（").append(b.status).append("）")`；`text = "微信 wechat"`。同文件已有 `deviceStatusZh`/`statusZh` 却未用于首页。
- **业务影响**：PGM-066「界面全中文（所有终端零英文）……安卓端展示给用户的一切文字必须全中文」。第一次打开首页即可看到英文状态。
- **可复现命令/最小反例**（类型 1+4）：读 L325/L1558；HTML PGM-066「已拍板：界面全中文」。
- **冻结判据**：PGM-066、GEN-015、PGM-001
- **2.3 依据类型**：1
- **四分类**：**冲突**
- **工程缺陷**：是
- **建议入队 / 发布前须修**：是
- **最小处置**：首页用 `deviceStatusZh`；渠道只显示「微信」「支付宝」；去掉 BYOD 英文分组名；域状态走已有 `*Zh`；登录不展示 URL（或改「服务器已配置」）。

### GR-U01-010

- **标题**：平板结账不生成、不展示支付二维码
- **文件:行号**：`MainActivity.kt:1520-1550`（`showPosCheckout`）；对照服务点单 `L1109-1114`（有 `BarcodeEncoder`）；酒吧支付 `L465-476` 只展示 intent 文本
- **完整原文**：`status.text = "...引导顾客扫码支付后点「整桌结算」。"` 无 QR 控件、无 `createBarPaymentIntent` 调用。
- **业务影响**：BSM-045「平板生成支付二维码给客人手机扫码」；BSM-046 整桌结账。第一次 POS 结账只有金额与渠道，没有码。
- **可复现命令/最小反例**（类型 4）：`grep -n BarcodeEncoder MainActivity.kt` 只出现在服务点单；`showPosCheckout` 无 pay-intent。
- **冻结判据**：BSM-045、BSM-046、GEN-002
- **2.3 依据类型**：1+4
- **四分类**：**冲突**
- **工程缺陷**：是
- **建议入队 / 发布前须修**：是
- **最小处置**：结账创建支付意图并渲染返回的码；无码不得提示「扫码」。

### GR-U01-011

- **标题**：本段新增关键分支没有能失败的安卓测试
- **文件:行号**：本段 3 文件 0 测试；U02 `DeviceRequestHeadersTest.kt:15-35` 只断言 JVM 签名 canonical
- **完整原文**：android-app 下 `find ... -name '*Test*'` 为空。
- **业务影响**：`sign()`、类型头分流、充电打卡、bar_pos、自助注册、POS 不写 deviceId 均可在绿测下进入发布。GEN-003：通过本身不替代行为证据；本段连绿测都没有。
- **可复现命令/最小反例**（类型 4）：

```text
find unpublished-03acdb98/apps/android-terminal/android-app -name '*Test*'   # 空
# 本席未跑 Android 仪器测试：无模块 gradlew、PATH 无 kotlinc；不装依赖、不跑会拉包的 gradle assemble
# ANDROID_HOME=/home/james/android-sdk 存在，不构成已执行证据
```

- **冻结判据**：GEN-003、GEN-018
- **2.3 依据类型**：4
- **四分类**：**技术性**
- **无业务发明理由**：缺的是对已有行为的测试，不是新业务规则。
- **工程缺陷**：是
- **建议入队 / 发布前须修**：是
- **最小处置**：为类型头、bar_pos 拒绝、POS 必须登记 deviceId、充电路径（若 2.2 删除则测其不存在）补可失败测试；Keystore.sign 用测试 Keystore 或接口假件。

### GR-U01-012

- **标题**：未发布客户端考勤 method=`charging_disconnect` 与签名头依赖，对着当前 main 后端/库不可执行
- **文件:行号**：`MainActivity.kt:111`；`TerminalApiClient.kt:286-289`；main `StaffService` 打卡白名单 `nfc/qr/manual`；main `V210` CHECK；main 无 `DeviceRequestHeaders.kt`；main 本三文件 = 基线（无 M009–M012 补丁）
- **完整原文**：见 §2.1 blob 表与 §5 GR-U01-005 命令。
- **业务影响**：把 U01 三文件合进当前 `bd445728`：① 这三路径没有 M009–M012 修复可被覆盖；② 充电打卡对 main 服务/库失败；③ 无 U02 `DeviceRequestHeaders.kt` 则 android-app 无法编译 `deviceRequestHeaders`。发布必须按 main 重做合并，不能整树覆盖。
- **可复现命令/最小反例**（类型 4）：

```text
git diff --stat 242f4872 HEAD -- <三路径>     # 空
git diff --stat e28dad05 HEAD -- <三路径>     # 77+/155-（main 相对未发布少签名与充电）
ls backend/src/main/resources/db/migration/V405*   # 不存在
ls backend/src/main/resources/db/migration/V410*   # 存在（M010）
test -f apps/android-terminal/src/main/kotlin/com/complex/terminal/DeviceRequestHeaders.kt
# main: 不存在；unpublished: 存在
```

- **冻结判据**：GEN-002、GEN-008、GEN-013、GEN-014
- **2.3 依据类型**：4
- **四分类**：**技术性**
- **无业务发明理由**：这是未发布树与当前 main 的接口/迁移分叉，不是新业务概念。充电方式本身的业务发明已在 GR-U01-005 单列。
- **工程缺陷**：是
- **建议入队 / 发布前须修**：是
- **最小处置**：以 main 为基线重放客户端改动；签名头与 `DeviceRequestHeaders` 同 PR；充电项按 005 的 2.2 结果处理，未批准不得依赖 V405。

## 6. 与 main 分叉对照（对抗式固定问题）

| 问题 | 结论 |
|---|---|
| 本段三文件是否已与 main 分叉？ | 是。未发布 blob ≠ main blob。 |
| main 是否已在同路径含 M009–M012 修复？ | **否**。main = 基线。发布未发布版本是「换上新实现」，不是覆盖那批治理页/迁移修复。 |
| 发布会破坏 main 其它已修项吗？ | 不经这三路径。跨段：充电打卡 vs main 考勤闭集；`deviceRequestHeaders` 为 main 所无。U00-000 点名的 U03/U08/U10 覆盖风险不在本三文件。 |
| 建议 | 发布者不得把 `e28dad05` 整树打到 `bd445728` 上。本段只能 cherry-pick 到当前 main，并处理 001–012。 |

## 7. 命令证据（只读；失败如实）

```text
# 锁
sha256sum docs/audit-2026-09/plan/criteria-lock.md
# b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# HEAD
git rev-parse HEAD origin/main
# bd445728a6ec7bb36a9e7ff19fe56927410ac61e ×2

# 对象类型
git cat-file -t e28dad059b2c7e08bcc95a8db18552d3f76edf4b   # tree
git cat-file -t 242f487233d028173cb9198eecd079f5d877405c   # commit
git cat-file -t 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb   # fatal: could not get object info

# 76 文件总体
git diff --stat 242f4872 e28dad05 | tail -1
# 76 files changed, 4745 insertions(+), 1084 deletions(-)

# 本段 numstat 见 §2.1

# 副本哈希 3/3 与清单一致（见 §2）

# Android 构建：未执行
# - 无 apps/android-terminal/android-app/gradlew
# - command -v kotlinc → 无
# - command -v gradle → /usr/local/bin/gradle
# - ANDROID_HOME=/home/james/android-sdk 存在
# 原因：不装依赖、不跑会拉包的 assemble；不把未运行测试写通过

# 未写 complex_test、未 source env 文件
```

canonical 与 U02 测试正文一致的独立 python digest：body `{"method":"qr","tag_ref":"点位一"}` SHA-256 = `5209d89fa1ba0782e19465b98059b111c935b542502d0c4cf0d292a3960790bb`。

## 8. 观察（无新根因、不升 finding）

- Keystore 未写死 `secp256r1`：Android 默认 P-256，未在真机验证。
- NFC `PendingIntent.FLAG_MUTABLE`：系统要写入 tag extras，属平台约束。
- `catalogImageBytes` 把服务端 URL 拼到 `baseUrl`：需目录投毒才越权 GET，铁律 6 收窄。
- 请假/服务单幂等键用毫秒时间：单击首次不撞。
- POS 选桌为两列方块而非「平面图」：点选桌台功能在，属 UI 细化。
- POS 客户端不校验最低消费：后端会话服务有低消字段；HTML 要求平板校验，可能由服务端完成。
- PGM-034 企业微信：本 APK 不内嵌会话，可走独立企微；不升冲突。
- BSM-003 存档删除/引导弹窗子条未映射本段。
- GEN-013/014 在本三文件无实现面。
- `operatorNote = "Android terminal bar order"` 英文只进后端字段，本 UI 不展示。
- 工作手机绑定已去掉 `ANDROID-${timestamp}` 默认值，改为管理员填已登记编号（相对基线的改进，不是缺陷）。
- 自助注册 JSON 字段名与 Java record 按名映射，不因构造器参数顺序不同而错。
- `decodeNdefTextPayload` 在共享 `TerminalModels`，有 U02 `NdefTextTest`；本段只调用。

## 9. 结论

单席 B1 结论：**FAIL**。

理由：本段三文件全文三线已做、50 条判据已逐项、与 main/基线分叉已用命令核对；存在未定稿缺陷（冲突/技术性/添加）。不存在有条件通过。材料未发布，本轮不修；B2 分配正式 ID `U01-00N`，处置写给发布者。

## U01 B2-r1 整批归并复核（Grok 乙席）

- 文件自身版本：V0.02（本段追加；上方 V0.01 B1 前缀 49269 字节原样保留，不改写 B1 锁段起点，本段不写独占锁哈希字段行）
- 席位：Grok（固定外审席乙，独立）。步骤：B2-r1 整批候选复核，不是 B1 重审、不是修复、不是模块 PASS、不是 B3/B4/B5。
- 控制：160-audit-prompt.md V0.17；runbook V0.02 §7/§11；criteria-lock V0.11；modules.md V0.04 附录 U01
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`（检查点 2：M011/M012 经 PR #429 已合；M001–M010 在 PR #428）。三文件 blob 仍等于发布基线 `242f4872`（本席本轮 `git rev-parse HEAD:<path>` 与 `242f4872:<path>` 三对全等）。
- 审计对象仍是只读输入副本 `.scratch/module-recheck-20260908/unpublished-03acdb98`（树 `e28dad05`，基线 `242f4872`），不是 main。三份 HTML 取 main 正本。
- 席甲：Claude Fable 5.1（操作员 2026-09-09 接替指令），作者自审票；本席 FAIL 具否决权。本票不照抄席甲候选台账，不照抄席甲 B2 核实结论。
- 本席 B2-r1：**PASS**（001–015 逐 ID 全 PASS；无添加项、不发 2.2；发布前须修「是」=001/002/003/006/007/008/009，「否」=004/005/010/011/012/013/014/015）
- 本追加段不写独占锁哈希字段行。正文实算：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`；sidecar `plan/criteria-lock.sha256` 同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。映射行 `M375 → [BEQR-150 … PGM-066]` COUNT=50 unique=50 MISSING=[]。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。必要核对只读；未重跑安卓编译（席甲 B1 §2 隔离 `/tmp/u-overlay` 绿仅作构建旁证，不替代 U01-008）。

此票只判断 findings.md V1.44 顶部「当前 U01 B2-r1 整批候选」相对本次两份完整 B1 raw 的全批差集、GEN-017 四分类、工程缺陷、「发布前须修」是/否与最小处置。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B1 **FAIL** 原文。材料未发布，定稿后状态应记 REVIEWED-FAIL（有已定稿缺陷，处置写给发布者），不做 B3。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/U01-b1-grok-raw.md` 前缀（本席 B1，本段追加前） | V0.01 | 49269 | `be1bcf2148ad47d1540b37f7a70554b24f3e23012332248a0a62ee25a09877f0` |
| `docs/audit-2026-09/modules/U01-b1-self-raw.md` | V0.01 | 39022 | `e4674e7c4bd1a202fb7bfdb17f90476294ee0ddfa8de4b9db830e133783885f2` |
| `docs/audit-2026-09/findings.md` | V1.44 | 545280 | `0aae9ad72099920f5c92e469f62a18ca5193adea71914a0eed0c761b1a1f2af3` |
| `docs/audit-2026-09/ledger.md`（只读） | V2.44 | 456353 | `e104e6bd5446b3b80de2a6b94e654b2ee9657e241be9f15e9cf1f6181e9a8173` |

候选形态：V1.44 顶部 U01-001…015；无 questions、无 HTML 修改申请。U01 ledger 行 IN_PROGRESS。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

### 技能（V0.17 §6 收窄，本节点实读）

| 技能 | 本节点用法 |
|---|---|
| using-superpowers | 先核技能；用户/提示词覆盖技能本体 |
| receiving-code-review | 先核 HTML/diff/blob/grep 再接受或反驳席甲改判；禁止表演性同意；B2 不修复 |
| grilling | 只借追问结构自查：L1445 是否真有「拿离充电」、六项是否被本材料加重、001 是否发明终端码、003 文案行是否被 diff 改写、015 是否吞掉本席 finding |
| verification-before-completion | 声称前先跑 sed/sha256/git rev-parse/diff/grep；本票不声称安卓编译本轮新绿 |
| systematic-debugging / diagnosing-bugs | 只借读错→复现→数据流（三文件→U02 头→副本 verifier/StaffService/V405→main 对照→HTML） |
| ponytail-review | 只评最小删除/收窄，不落地。001 不造新终端种类；002 允许受管策略替代前台服务；006 只补 POS 已有 onSuccess；007 不把手填他人档案做成新产品 |
| writing-for-agents | 报告结构；未改 AGENTS.md |
| requesting-code-review | 本体要派同会话子代理 → 第四席；**未用** |
| tdd / test-driven-development / ponytail | B2 禁止修复，未落测试、未改源 |
| git-guardrails-claude-code | 存在；本席未改 git |
| resolving-merge-conflicts | 本节点无合并 |
| batch-grill-me / ubiquitous-language | **缺口：本环境未找到**；未冒充调用 |
| Arc audit | 会派子代理；与双席冲突；**未用** |

### 完整差集（GR-001…012、§8、席甲 CX-001…005、OBS-1…10）

漏并：无。两席 B1 全部临时 ID 仍落在 001–015。本席 GR-005 的「添加」分类在本票改判后拆到 002（工程面）+ 009（与 main 闭集/V210 不兼容），不另留添加项、不发 2.2。席甲独有 CX-003/004 收入 003/004，本席 B1 未单列，核源后接受。GR-001 首页本体在 010；候选来源行写「GR-001 身份面」并入 001 略松（身份自报的根是 GR-002），首页原意未丢。

| 正式ID | 本次来源（不改写原意） | 候选分类/缺陷/须修 | 本席票 |
|---|---|---|---|
| U01-001 | CX-U01-001 + GR-U01-002（同根）+ GR-001 身份面（实质=GR-002） | 冲突/是/是 | **PASS（冲突/是/是）** |
| U01-002 | CX-U01-002 + GR-U01-005 工程面（改判非添加） | 技术性/是/是 | **PASS（技术性/是/是）** |
| U01-003 | CX-U01-003 | 技术性/是/是 | **PASS（技术性/是/是）** |
| U01-004 | CX-U01-004 | 技术性/是/否（登记） | **PASS（技术性/是/否）** |
| U01-005 | CX-U01-005 + GR-U01-009 | 冲突（main 既有）/是/否 | **PASS（冲突 main 既有/是/否）** |
| U01-006 | GR-U01-004 | 技术性/是/是 | **PASS（技术性/是/是）** |
| U01-007 | GR-U01-006 | 冲突/是/是 | **PASS（冲突/是/是）** |
| U01-008 | GR-U01-011 | 技术性/是/是 | **PASS（技术性/是/是）** |
| U01-009 | GR-U01-012 + 席甲 §4 | 技术性（与 main 分叉）/是/是 | **PASS（技术性/是/是）** |
| U01-010 | GR-U01-001（首页无过滤） | 冲突（main 既有）/是/否 | **PASS（冲突 main 既有/是/否）** |
| U01-011 | GR-U01-003 | 技术性（main 既有）/是/否 | **PASS（技术性 main 既有/是/否）** |
| U01-012 | GR-U01-007 | 冲突（main 既有）/是/否 | **PASS（冲突 main 既有/是/否）** |
| U01-013 | GR-U01-008 | 冲突（main 既有）/是/否 | **PASS（冲突 main 既有/是/否）** |
| U01-014 | GR-U01-010 | 冲突（main 既有）/是/否 | **PASS（冲突 main 既有/是/否）** |
| U01-015 | 双方观察 | 登记/否/否 | **PASS（登记/否/否）** |

冲突入队 2（001、007）。冲突（main 既有）5（005、010、012、013、014）。技术性入队 5（002、003、006、008、009）。技术性登记 1（004）。技术性（main 既有）1（011）。登记观察 1（015）。添加 0。

发布前须修「是」=001、002、003、006、007、008、009（7）；「否」=004、005、010、011、012、013、014、015（8）。与候选汇总一致。

### 重点 (a) GR-U01-005 改判 — **PASS（改判成立，不发 2.2）**

本席 B1 把「取离充电设施记上班」标添加，依据是「三份 HTML 无充电/拔电考勤原文」。本轮独立 `sed -n '1445p' docs/business-system-map-for-operations-v0.1.html` 原文为：

> 考勤打卡从领取工作手机开始算：员工到岗领到自己那台工作手机的时刻，就是上班打卡的时刻。领取可以用工作手机碰 NFC 标签，工作手机不支持 NFC 时扫码；工作手机被拿离充电设施这个动作本身也算领取。三种方式记的是同一个上班时刻，以最早发生的那一次为准，不重复计。

BEQR 打卡方式行（`docs/business-enum-quick-reference-v0.1.html:280`）同义。本席 B1 漏读 L1445，添加分类不成立，2.2 草稿作废。规则是 HTML 原句，不是业务发明。

拆分成立：

- 工程面：接收器仅 `onCreate` 动态注册、`onDestroy` 注销，应用未启动/被回收时拔线丢失 → 随后 NFC/扫码成为「最早一次」→ 误判迟到。副本 `StaffService` `findFirstCheckIn`（约 L879–883）去重本身正确。归 U01-002，技术性，须修是。无业务发明：缺的是 HTML「动作本身」的捕获保障。
- 与当前 main 考勤闭集不兼容：main `StaffService` 白名单 `nfc/qr/manual`，`V210` CHECK 无 `charging_disconnect`，main 无 V405、已有 V410 → 归 U01-009。充电方式本身不再单列添加项。

副本 `charging_disconnect` 为相对基线新增（main MainActivity 0 处 / 副本 1 处），实现面仍须修，只是分类从添加改为技术性。

### 重点 (b)「main 既有」口径与六项 — **PASS（口径合规；六项确为 main 既有，本材料未加重）**

口径（材料 diff 未触及、main HEAD 同位置已有 → 冲突/技术性（main 既有）、发布前须修否、归 M375 PENDING）：对未发布段合规。本段对象是副本三文件相对基线的改动；main 已有缺陷由 M375 审计承接，不把 M375 积压改成 U01 发布门。材料不得加重。本席按「同句/同路径是否出现在 `git diff 242f4872 e28dad05` 的 +/- 中」独立核六项：

本轮对照（main HEAD blob = 基线 blob；副本 blob = 树 `e28dad05`）：

| 项 | 本席命令 | main | 副本 | diff 是否触及缺陷本体 | 加重？ |
|---|---|---|---|---|---|
| 005 英文直出 | 用户可见 `API:` / `BYOD 一期` / `b.status` / `verificationResult` / `微信 wechat` / `支付宝 alipay` | 有（`:160/:249/:277/:346/:1511-1512`） | 同文（行号平移） | +/- 未改这些展示句；副本还删了 main 的 `operatorNote = "BYOD 手机代提交"`（后端字段，非加重） | 否 |
| 010 首页无过滤 | `showHome` 内 `button(` | 23 | 23 | 唯一按钮文案改「员工注册申请(店长/HR 代提交)」→「商户员工注册申请（商户代提交）」，未增核销/POS/BYOD/检修按钮；「跳过，进入首页」两边各 1 | 否（自助注册新入口在登录页，归 001） |
| 011 `bar_pos` | `grep -n bar_pos MainActivity.kt` | **3**（`:48` prefs 文件名、`:1218` 注释、`:1236` `terminalType = "bar_pos"`） | 3 同构 | `terminalType = "bar_pos"` 不在 +/-；prefs 行只出现在邻近 hunk 上下文 | 否。席甲核实写「2 处」少计 prefs 文件名；缺陷是赋值，文件名属内部标识，不改票 |
| 012 破解检测 | `reportStaffDeviceIntegrity` | MainActivity 0 / ApiClient 1 | 同 | UI 未接线，API 两边都有 | 否 |
| 013 换绑 | `approveStaffDeviceRebind` | MainActivity 0 / ApiClient 1 | 同 | 同上 | 否 |
| 014 POS 无支付码 | `git diff … \| grep showPosCheckout` | 结账文案「引导顾客扫码」无 `BarcodeEncoder` | 同构（copy `:1521-1550` / main `:1474-1503`） | **空** | 否 |

六项发布前须修「否」、归 M375：成立。001 已把本材料改写的身份头单列须修，不与 010 重复关门。

### 重点 (c) U01-001 同根与处置 — **PASS（同根成立；最懒；不发明）**

GR-002（除两路自助注册外恒 `staff_work_phone`）与 CX-001（该两路改报 `app`、自助注册放在工作手机登录页）是同一根：终端身份由客户端按路径自选。核源：

- 副本 `TerminalApiClient.kt:277` 确为路径三态；main 同函数 `:265` 恒 `"staff_work_phone"`。该行在本材料 diff 中被改写，故「main 既有恒值头一并在此处修」成立，不属 (b) 的未触及。
- `:285-289` 公开路径与登录跳过设备签名：本材料新增。
- 登录页「公司员工自助注册」+ `showStaffSelfRegistration`：本材料新增。HTML PGM L578 / BSM L1433 指定公司自有员工在**私人手机 App**注册；BSM L930 三种设备不同用途；PGM L739 十一端把客人手机应用与员工私人手机分成两端。自报 `app` 会走进副本 verifier L180–183 的 `app_requires_customer_proof`，且无 `X-Device-Id` 时 L146–149 以自报值为类型。
- main `TerminalIdentityHolder.trustedTerminalType`（`:48-54`）无派生结果时回落自报头；`ServiceOrderController:110-113` 读该回落。与 GR-002 后端面一致。

最小处置不发明：HTML 已写私人手机通道与按设备声明；`TerminalCanaryService.TERMINAL_TYPES` 已含 `staff_personal_app`/`staff_private_phone`，候选写「闭集中的私人手机码」是从已有码里取，不新造码、不规定必须哪一个同义词。`TERMINAL_DEVICE` 绑定闭集仍是 `staff_work_phone|tablet|kiosk`，001 未要求把私人手机加进设备绑定闭集。商户代提交 HTML 沉默 → 登记不发明，与本席不把沉默升冲突一致。现场资金只信 verifier 派生身份是 GR-002 原文的后端半句，属第 6/7 段同 PR，不是 U01 发明新产品。

工作手机去掉登录页自助注册：HTML 原句的最懒切法。禁止：发明第四种终端、把自助注册改报 `web`、在工作手机上保留入口只改文案。

### 重点 (d) U01-006/007/008/009 处置 — **PASS（最懒，不发明业务）**

- **006**（GR-004）：本材料只把 `registerDeviceSigningKey`+`api.deviceId` 接到工作手机绑定 onSuccess（copy `:260-266`）；POS onSuccess（`:1288-1293`）仍只写 prefs。verifier `DEVICE_BOUND_TYPES` 含 tablet → 一旦绑定成功，enforce 下平板写请求 `credential_required`。最小处置=POS 成功后同样写 deviceId 并登记公钥，对称已有路径。括号「与 011 平板类型码一并处理」是发布者提示，不把 011 改回须修（011 为 main 既有 `bar_pos`，绑定目前必 400，006 的 onSuccess 要等类型码合法才跑；仍须把新签名协议接到该路径，否则 011 日后一改类型码会立刻缺凭证）。不发明新 POS 流程。
- **007**（GR-006）：自助注册函数体无 `DeviceKeystore`（copy `:1188-1214`）。HTML PGM L579 / BSM L935「安装时生成并绑定该手机专属的设备标识」。冲突、须修是。处置=私人手机端注册/安装时交 Keystore 公钥/指纹，服务端按 token 派生本人——禁止手填他人档案，是防越权，不是新产品。与 001 同 PR 落地（通道先对，再绑标识）。
- **008**（GR-011）：`android-app` 无测试文件（`find … -iname '*test*'` 空）。新增 `sign()`、类型头分流、充电领取、自助注册、编号持久化均无可失败断言。隔离编译绿 ≠ GEN-003。处置=为新增分支补 JVM/假件可失败测试；充电路径按 002 测其行为（不再按已作废的 2.2「删除则测不存在」）。不要求仪器测试或真机 Keystore。
- **009**（GR-012）：充电现为 HTML 原句，不能从客户端删掉等 2.2。单独合 U01：缺 U02 `DeviceRequestHeaders.kt`（main 无）则 android-app 无法解析 `deviceRequestHeaders`；缺 U06 白名单 / U10 CHECK 扩容则打卡 400/撞 V210。main 已有 V410、无 V405 → 迁移号须按 main 重排。最小处置=以 main 为基线重放，U01/U02/U06/U10 同一 PR，不得把 `e28dad05` 整树盖到 `445a49f`。不发明新考勤口径。

### 重点 (e) U01-003 文案与 U01-004 登记否 — **PASS**

PGM L870「一律加密存放」、L871「只显示脱敏号码，不见明文」。候选把 `:1793`「仅存不可逆加密，不可回显」改为「加密存放、界面脱敏」：与 HTML 逐字同向，不把存储改成哈希、不发明不可回显。副本 V403 注释「接收可解密原值」；`JdbcStaffRepository` `:891-893` encrypt、`:1199-1201` decrypt。main 无 V403、main 同文件无 `piiCipher` → 本树发布包会让「不可逆」从「声称一种不存在的密码学」变成「可逆加密却写成不可逆」。UI 行字节虽未进 +/-，与 U06/U10 对读后须修「是」成立，不按 005 那种纯展示英文延后。GEN-016/010，技术性，无业务发明。

004：本材料新增自助注册屏并给商户代提交屏加上手机号/证件号，两屏未设 `secure = true`；既有薪资/考勤等四屏已设。HTML 无截屏规则，PGM L870–871 只管存放与界面脱敏。技术性缺陷成立；发布前须修「否（登记，随发布顺手不单独阻断）」合规。本席 B1 未把 FLAG_SECURE 升 finding，不在本票改成关门项。

### 重点 (f) U01-015 观察 — **PASS（未写错本席 §8）**

015 列席甲 OBS-1…10，并以「Grok §8（NFC FLAG_MUTABLE、POS 两列方块、低消在服务端、企微独立、`operatorNote` 英文只进后端等）」收本席观察。「等」覆盖 secp256r1 未写死、`catalogImageBytes` 拼 URL、幂等键毫秒、BSM-003 未映射子条、工作手机绑定去掉 `ANDROID-${timestamp}`（正向）、JSON 按名映射、Ndef 测试在 U02。未把本席任何 finding 降成观察，也未把 §8 升格入队。BARPOS-millis、旧幂等键与席甲 OBS-4/5 同根，维持登记。票 PASS（否）。

定稿时 015 不必把 §8 逐条抄全；不得写成「Grok 无观察」或把 secp256r1/NFC 改成缺陷入队。

### 001 独立裁定（冲突／须修是）

见 (c)。分类冲突：HTML 明文通道与十一端被客户端改报打破，首次打开登录页即可走错端。工程缺陷是。须修是。票 **PASS**。

### 002 独立裁定（技术性／须修是）

见 (a)。`MainActivity.kt:99-103/:128-131/:157/:853`。文案「请保持应用运行」不能替代常驻。处置允许前台服务**或**受管策略（PGM-036/037 工作手机本就受管），不是强制新造一种产品形态。与 009 分工：002=进程内存活；009=后端/迁移/签名头同发。票 **PASS**。

### 003 独立裁定（技术性／须修是）

见 (e)。票 **PASS**。

### 004 独立裁定（技术性／须修否）

见 (e)。票 **PASS**。

### 005 独立裁定（冲突 main 既有／须修否）

见 (b)。用户可见英文与 PGM L287/L965、GEN-015 冲突成立；main 同句，本材料未加新的用户可见英文。归 M375。票 **PASS**。

### 006 独立裁定（技术性／须修是）

见 (d)。票 **PASS**。

### 007 独立裁定（冲突／须修是）

见 (d)。首次自助注册即缺 HTML 要求的安装绑定。票 **PASS**。

### 008 独立裁定（技术性／须修是）

见 (d)。GEN-003：通过本身不替代行为证据；本段连针对新分支的可失败测试都没有。票 **PASS**。

### 009 独立裁定（技术性／须修是）

见 (d) 与 (a)。充电不再是添加项之后，009 从「未批准不得依赖 V405」改为「须带 U06/U10（编号按 main 重排）」——这是改判后的正确最小集，不是发明新迁移语义。票 **PASS**。

### 010 独立裁定（冲突 main 既有／须修否）

`showHome` 无设备类型分支、23 按钮两边同构，PGM-047「按设备类型下发 / 关后菜单不可见」在 main 已破。本材料未加重该面。自助注册新入口归 001。处置归 M375；001 落地后按登记类型过滤首页是 M375 的活，不把 010 改回 U01 关门。票 **PASS**。

### 011 独立裁定（技术性 main 既有／须修否）

`terminalType = "bar_pos"` 两边都有；两边 `TERMINAL_TYPES` 均为 `staff_work_phone/tablet/kiosk`。首次 POS 绑定必 400 是 main 既有。本材料未改该赋值。归 M375（客户端改 `tablet`）。票 **PASS**。

### 012 独立裁定（冲突 main 既有／须修否）

HTML PGM L581 / BSM L937 要求破解失效。客户端无检测、MainActivity 0 调用，两边同。归 M375。票 **PASS**。

### 013 独立裁定（冲突 main 既有／须修否）

HTML「换手机必须先解绑再绑定」。UI 无解绑/换绑，两边同。归 M375。票 **PASS**。

### 014 独立裁定（冲突 main 既有／须修否）

BSM-045/046 平板生成支付码。`showPosCheckout` 不在本材料 diff 内，两边同构无码。归 M375。票 **PASS**。

### 015 独立裁定（登记／否）

见 (f)。票 **PASS**。

### 分类 / 队列 / 给发布者的最小边界

本批可定稿（须席甲本批亦 PASS）。发布前须修是=001、002、003、006、007、008、009。否=其余 8。添加 0，不进 BLOCKED-ASK。

发布者最小集（不在本轮改源）：

- 001：自助注册只走员工私人手机通道；`X-Terminal-Type` 按绑定/登记类型只声明一次，禁止 `app`、禁止按路径改报；工作手机登录页去掉自助注册；商户代提交不发明新端；现场资金只信派生身份（U06/U07 同 PR）。
- 002：充电领取接收器在应用未运行时也要能记（前台服务或受管常驻）；不要用文案代替。
- 003：档案句改为「加密存放、界面脱敏」。
- 006：POS 绑定成功写 `api.deviceId` 并登记公钥。
- 007：私人手机安装/注册提交 Keystore 公钥，服务端按本人 token 绑。
- 008：新分支可失败测试。
- 009：以 main 重放；U01+U02+U06+U10 同 PR；迁移号避开已占用的 V410。
- 004 顺手两屏 `secure = true`，不阻断。
- 005/010/011/012/013/014 不在本材料关门，记入 M375。
- 禁止：整树覆盖 main；把充电当添加去问 2.2；为 011 在 U01 发明新终端码 `bar_pos` 入后端闭集；把 010 无过滤首页当作本材料引入。

非阻断勘误（不定稿作废，不必 r2）：席甲核实命令把 MainActivity `"bar_pos"` 写成 2 处，实为 3 处（含 prefs 文件名）；001 来源行「GR-001 身份面」宜读作 GR-002。

### 结论

全批差集无漏并。本席 B1 对 GR-U01-005 的添加分类已用 HTML L1445 原句否决，改判成立。main 既有口径合规，六项未加重。001 同根与处置不发明。006–009 最懒。003 与 PGM L870–871 一致；004 登记否合规。015 未写错 §8。逐 ID 001–015 全 **PASS**。本席 B2-r1 **PASS**。未经双 PASS 不得把候选当定稿，更不称 U01 或 main 模块 PASS。步骤仍 B2；U01 IN_PROGRESS；材料未发布，不做 B3。
