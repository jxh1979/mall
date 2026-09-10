CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# U-FIX B5-r2 修复后全量审计（Grok 席乙）

文件版本：V0.01
步骤：B5-r2
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/U-FIX-b5-r2-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

整模块结论：**PASS**。
不可用 B4-r2 双 PASS、B3 自报或席甲 B5 探针代替本轮整模块三线重审。席甲=Claude Fable 5.1（作者自审票、唯一修复者）；本席 FAIL 具否决权——本席本轮**不行使否决**。无有条件通过。

闭合结论：定稿「是」项 119 个（原 117 + 回退 B2 定稿 U01-016/U13-011）均按最小处置闭合；B5-r1 否决点 U01-007 未闭合、以及当时两条无 ID 新缺陷，已分别按原处置与回退定稿落地。本轮未发现新的有 ID 未闭合项，未发现无 ID 新缺陷，未发现业务发明。新问题只登记不修——本轮无新问题可登记。

U-FIX=XM-009 先例的合成模块，承载 U01–U18 全部已定稿且「发布前须修」归属「是」的 finding ID。定稿依据：docs/audit-2026-09/findings.md V1.83 各段「B2 双PASS定稿」节与顶部回退 B2 定稿节。操作员唯一业务答复：排班可见天数出厂行上下限＝1 天/31 天（值 7）；自营独立布尔＝删除（questions/U04-001.md 答复节「删除」）。只用既有冻结判据。未新造判据/角色，未询问操作员。本席在主仓 cwd 只写本指定路径；验证在 `/tmp/ufix_b5r2_grok_copy` 与一次性空库 `ufix_b5r2_grok_scratch`（已 DROP）。未读席甲 `U-FIX-b5-r2-self-raw.md`。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。python3 hashlib 与 `openssl dgst -sha256` 三工具同值。

| 项 | 值 |
|---|---|
| 锁文件 | `docs/audit-2026-09/plan/criteria-lock.md` V0.11；889020 字节 |
| 独立 SHA256 | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` |
| sidecar `plan/criteria-lock.sha256` | 全文一行同值 |
| ledger 顶行 | 同行；`LOCK-AMEND-COUNT: 1`；ledger 版本 V3.09 |
| findings | V1.83 |
| 模块 | U-FIX 合成修复模块（U01–U18 已定稿「是」项整批） |
| 阶段 | B5-r2 整模块全量三线重审（不只看 diff） |
| B3 | 已执行 2 次（本轮审计对象＝B3-r2 之后） |
| before tree | `28dcfec971f6eca651213368c27ec7e893a08305`（＝445a49f4 的树，对象类型 tree） |
| after tree | `c0ec9b7ab6bc38f8197830b7283af4dc2356b61b`（对象类型 tree） |
| B3-r2 相对 B3-r1 | before `e3fd14b6e61f284e3a9dc7380d529e7486c71b8a` → after `c0ec9b7…`；仅 3 文件 M |
| 主工作区 HEAD | `445a49f409f0cfbe6dd616ae079be1299824c48f` = `origin/main` |
| 差异规模 | `git diff --name-status` 85 路径（A 31 / M 54）；`85 files changed, 12214 insertions(+), 7151 deletions(-)` |
| 正式队列「是」（119） | U01-001/002/003/006/007/008/009/016；U02-001…004；U03-001…009；U04-001/002；U05-001…011；U06-001…007；U07-001…005；U08-001…005；U09-001…005；U10-001…003；U11-001…008；U12-001…005；U13-001…008/010/011；U14-001…008；U15-001…008；U16-001…007；U17-001…010；U18-001…004 |
| 正式队列「否」（不得据此判 FAIL） | U01-004/005/010–015；U02-005；U03-010–015；U04-003；U05-012；U06-008–011；U07-006/007；U08-006；U09-006；U10-004；U11-009；U12-006；U13-009；U14-009；U15-009；U16-008；U17-011；U18-005 |

两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）。三 HTML 取 main 正本；after 树 blob 与 HEAD 全等（BSM `12334b13f3c27a815c9bbf9eea216403fb0d76d1` / PGM `c2588ca1c65a3f196b39a23f299bd56967b5652e` / BEQR `86a717930f9303d4f131ee2864478baa14450e6d`）。工作区 SHA256（本席独立）：

| 文件 | 字节 | 换行 | SHA256 |
|---|---:|---:|---|
| `docs/business-system-map-for-operations-v0.1.html` | 304482 | 2032 | `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 103015 | 979 | `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c` |
| `docs/business-enum-quick-reference-v0.1.html` | 133961 | 308 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |

worktree 85 路径全文 hash 与 after 树 85/85 相等。U08 取 main 侧四文件 after blob＝HEAD：`PlatformService.java` `fe67a293…`、`OrganizationMerchantPage.tsx` `7d5b3ee9…`、`.test.tsx` `823e37e3…`、`apps/web/src/admin/types.ts` `8d26e821…`。name-status 无这四路径（净发布物为空，符合 U04/U08 删除口径）。`MerchantAdmissionRow` 仅 `cooperationForm`，无 `selfOperated`。

适用判据＝U01–U18 各段映射行并集（锁 V0.11，本席 python 抽出）：unique **139**。分部落点：U01 M375=50；U02 M376=50；U03 M012∪M057∪M081∪M290∪M293=21；U04 M365=21；U05 M054=52；U06 M054∪M288=52；U07 M063∪M064∪M396=71；U08 M009=87；U09 M040=106；U10 无 main 映射行＝GEN-001–018；U11 M056=52；U12 M042∪M066=107；U13 M028∪M058∪M411∪M420=60；U14 M391=18；U15 M392=18；U16 M389=43；U17 M390∪M394=43；U18 M029∪M053∪M394=18。MISSING=[]。闭集：

BEQR-063/074/097/115/143–159/163/166/182–187/189/192；BSM-001–007/009/012/013/026–029/044–050/072–077/084–088/114–120；GEN-001–018；PGM-001–022/024/026–038/040–048/057–060/065/066。

grilling / grill-me 本会话无独立工具入口，按 runbook 记缺口、不冒充调用；对抗式反例由本席自读源与可失败测试构造。

HTML 锚点（main 正本）：PGM L578 公司自有员工由本人在私人手机 App 注册；PGM L579 / BSM L935「私人手机 App 安装时生成并绑定一个该手机专属的设备标识，把手机和员工绑定；不读取手机本机的硬件识别码」；BSM L1433 同旨注册两条路径。

---

## 2. 三线方法（整模块，不只看 diff）

- **第一性原理**：逐段追问定稿缺陷是否真正闭合、修复是否引入新缺陷。私人手机 App 是否在注册时以 Keystore 公钥/指纹提交并绑定；商户代提交文案是否指向实际入口；T1:7 编号上界是否含 V422；薪酬公式被减数/分母是否按 HTML；柜机是否单端；顶班是否有入口；出厂行是否存在且 GET 不把空上下限当合同；契约与生成物是否同一 json、版本可复现。
- **顺从式**：各段映射判据对照修复后全文（85 路径 + 调用/迁移/契约/生成物上下游）。门禁在副本实跑。B3-r2 只改三文件，其余 82 路径相对 B5-r1 after 树 `e3fd14b6` 字节不变，本轮仍按整模块口径复核行为与上下游，不只信任「未改」。
- **对抗式**：假定实现错误。最小反例：无设备号写请求、无设备证明自助注册、自营独立布尔残留、只叠部分生成物、测试只改断言、T1 编号钉、UI 文案指向已删入口、工作手机已绑定仍露出自助注册、公开路径改报 `app`、指纹读硬件识别码。

证据类型：1=三 HTML 原文；2=可执行产物；4=可复现命令输出。

---

## 3. 85 路径覆盖（after 树全文，非抽样宣称）

name-status 85/85 均审。分组覆盖记录：

| 组 | 路径数 | 覆盖方式 |
|---|---:|---|
| U01 安卓应用（含新增 ChargingReceiptService / Manifest） | 5 | 整文件读 MainActivity 登录/绑定/POS/商户注册/自助注册、TerminalApiClient 传输层与 override、DeviceKeystore、ChargingReceiptService |
| U02 JVM 客户端+测试 | 5 | applyHeaders / StaffSelfRegistrationRequest / HeadersTest 5 例 / DeviceRequestHeadersTest |
| U03/U04 web | 7 | StaffProfilesPage 默认拉取、排班事由 validate、settings/guidedcreate 测试；types.ts / OrganizationMerchantPage＝main |
| U05/U06 员工服务 | 19 | RegistrationService 全文关键路径、Locker 单端、coverShift、settingScopeKey、payroll SQL 148–232、Controller 幂等写口 |
| U07 闸/终端 | 7 | Verifier DEVICE_BOUND_TYPES 含私人手机、无设备号写请求 fail-close、SecurityConfig public 路径、RateLimit AUTH、DeviceBindRequest.staffId |
| U09 复核 | 1 | isMoneyAction 含 `staff.payroll.*`、发布器必填 |
| U10 迁移 V411–V422 | 12 | 连续号、V413 出厂行 7/1/31、V419 无 is_self_operated、无 V400–V409、main V410 保留 |
| U11/U12 测试 | 11 | M010 6000/7800/deviceProof；RegistrationHttp 无公钥 400；Verifier 23；复核 44 |
| U13 设计/安全产物 | 5 | T1/T9 V0.13 当前实现段；T1:7 V411—V422；生成器 reminder_days；基线 --strict |
| U14–U18 契约与工具 | 14 | json 732 / V0.14 / 无 self-operated 路径；四闸 --check；PII 16 列；域归属 334 |

未把未改的 OrganizationMerchantPage / PlatformService / types.ts 算进 85（取 main 侧，见 §1）。

B3-r2 三文件 after blob（本席 `rev-parse` + worktree `hash-object` 全等）：

| 路径 | after blob |
|---|---|
| `MainActivity.kt` | `32a68cc545b2a9a3725ce53c83d550a5e0ba313d` |
| android-app `TerminalApiClient.kt` | `1e2a0f69d115f220696156f877b92f0b988cb6da` |
| T1 规格 | `4d56a568eb64096d757621d8385666f2dc36dc66` |

---

## 4. 逐段结论（修复后全文）

| 段 | 定稿「是」 | 本席裁定 | 要害证据 |
|---|---|---|---|
| U01 | 001/002/003/006/007/008/009/**016** | **PASS**（007/016 本轮重点闭合） | 见 §5 |
| U02 | 001–004 | PASS | JVM `applyHeaders` 未绑定不发类型头；`StaffSelfRegistrationRequest` 必填公钥/指纹；HeadersTest 5/5（含 override 私人手机码）；共享客户端重放。B3-r2 未改本段文件 |
| U03 | 001–009 | PASS | 001–007＝不合入商户页（blob＝main）；008 默认不带 `include_archived`；009 编辑态空原因拦截（guidedcreate `lastPostBody===null`）；vitest 含对应例 |
| U04 | 001/002 | PASS | 独立布尔已删；`types.ts`＝main `8d26e821`，`MerchantAdmissionRow` 无 `selfOperated` |
| U05 | 001–011 | PASS | 分流读 `cooperationForm=="self_operated"`；事假基数固定+台账提成+酒吧提成+奖金（JdbcStaffRepository `:216-221`）；封顶分母固定+奖金且注释禁止把两项称作 HTML 总薪资全部（`:201-213`）；出厂行 7/1/31 + 缺行 `POLICY_NOT_CONFIGURED`；柜机 kiosk 控制器+一次性 token；history `lockForUpdate=false`（StaffApprovalService `:88-90`）；写入强制 `company_id`；两写口 `requireIdempotencyKey` |
| U06 | 001–007 | PASS | `settingScopeKey` 写读共用不回落 projectId；DTO 设备证明；顶班 `coverShift`+V422；003 随 001 收敛 |
| U07 | 001–005 | PASS | bind 写 `staffId`，工作手机缺则拒；钥源私人手机≠工作手机；无设备号 enforce 拒；公开注册 AUTH 频控（`self-registrations`）；`DEVICE_BOUND_TYPES` 含 `staff_private_phone` |
| U08 | 001–005 | PASS | 四文件＝main；无 `setMerchantSelfOperated` |
| U09 | 001–005 | PASS | `isMoneyAction` 含 `staff.payroll.*`；V418 种子 `hr_review`；发布器必填；用例在 U12 |
| U10 | 001–003 | PASS | V411–V422 连续、main V410 保留、无 V400–V409；V419 无独立布尔列；出厂行在 V413 `VALUES (…, 7, 1, 31)` |
| U11 | 001–008 | PASS | 空库 44+2+7+4+29+4+3：无 `withSelfOperated`；事假 6000/7800；deviceProof；无指纹 400（日志「请填写私人手机设备公钥」） |
| U12 | 001–005 | PASS | Verifier 23（含 Cookie 会话放行、无设备号拒、`DEVICE_CREDENTIAL_INVALID`）；复核 44 含缺 hr 线/DEPARTMENT_REVIEWS_PENDING/排班 one_level_up |
| U13 | 001–008/010/**011** | **PASS**（011 本轮重点闭合；001–008/010 未因 :7 一句被打回） | 见 §5.3；T1/T9 V0.13 口径句已改；`latest()`；reminder_days 读 canonical；`--strict` 绿 |
| U14 | 001–008 | PASS | 732 operation、V0.14、无 self-operated 路径、自助注册 required 含 `device_public_key`/`device_fingerprint_hash`、终端仅 `staff_private_phone`、失败非 none、两写口幂等、可见天数描述含 7＋1..31 |
| U15 | 001–008 | PASS | yaml 由生成器同写；`verify:openapi` json↔yaml 绿；yaml `info.version` V0.14 |
| U16 | 001–007 | PASS | 附录标题 V0.14；三口成功事件名与服务 `recordSuccess` 一致；732 口 `--check` |
| U17 | 001–010 | PASS | 矩阵标题 V0.14 跟 `info.version`；taskGroup staff |
| U18 | 001–004 | PASS | 三工具以 main 为底重放；域归属 334；PII 16 列 |

「否」项抽查仍在、本批未以其为目标：U06-010 `missing_checkout` 仍在 `StaffService.java:1623`；U01-005「跳过，进入首页」仍 1 处（`:287`）；U01-004 两屏仍无 `secure = true`（`:1233`/`:1257`）；U01-011 注释仍写 `bar_pos` 但绑定实参已是 `tablet`。否项归登记/M375，不升本模块 FAIL。

B3-r2 波及面：`git diff --name-status e3fd14b6 c0ec9b7` 仅 MainActivity.kt、android-app TerminalApiClient.kt、T1 规格。U02–U12、U14–U18 源/契约/生成物相对 B5-r1 after 树字节不变；本席仍核对关键闭合并复跑验证，未见回退。

---

## 5. 本轮重点闭合（B5-r1 否决点）

### 5.1 有 ID：U01-007 已按最小处置闭合

- **定稿**（U01 B2-r1 + V1.83 顶部）：冲突 / 须修是。最小处置：与 U01-001 同步，在私人手机端注册/安装时以 Keystore 公钥/指纹提交并绑定；入口只在私人手机通道。HTML PGM L579 / BSM L935。
- **B5-r1 否决**：B3-r1 删除 `showStaffSelfRegistration` 而非补 Keystore；android-app 客户端无提交方法。
- **after 树实际（本轮）**：
  1. `showStaffSelfRegistration()`（`:1199-1234`）存在。提交前取 `DeviceKeystore.deviceFingerprintHash()` / `devicePublicKeyBase64()`，构造 `StaffSelfRegistrationRequest` 字段序与 Kotlin 数据类一致（idempotencyKey/companyId/name/pubkey/fingerprint/merchant/phone/idDoc），经 `api.submitStaffSelfRegistration` 发送。
  2. `DeviceKeystore`：EC P-256 存 AndroidKeyStore，指纹＝SHA-256(公钥 X.509 DER) 十六进制，不读 IMEI/NFC 硬件识别码。首次 `ensurePublicKey()` 生成、之后复用同一别名——对应「安装/注册时生成」。
  3. android-app `TerminalApiClient.kt` 顶层 `STAFF_PRIVATE_PHONE`；`listStaffRegistrationCompanies()` / `submitStaffSelfRegistration()` 仅这两条公开路径 `terminalTypeOverride=STAFF_PRIVATE_PHONE`；其余 `open()` 用 `override ?: terminalType`，未绑定不发类型头。`send()` 仅当 `deviceId != null && deviceSigner != null` 且非登录路径才签；未绑定无设备编号不签名。
  4. 成功后把指纹写入 `private_phone.device_id`，`api.deviceId`/`terminalType` 按私人手机码声明。`onCreate` 恢复该身份。
  5. 登录页（`:224-229`）仅 `api.terminalType == null` 时显示按钮「公司员工自助注册（私人手机）」——已绑定工作手机/平板不显示。满足 U01-001「工作手机应用去掉自助注册入口」与 007「入口只在私人手机通道」。
  6. 后端 `StaffRegistrationService.selfRegister` 仍强制公钥/指纹并校验指纹＝SHA-256(DER)、同指纹不可绑他人（U05-003，空库 RegistrationHttp 无公钥 → 400）。端侧提交 + 服务端绑定构成 HTML「生成并绑定」。
- **对抗式**：
  - 已绑定工作手机：按钮不渲染，无法从工作手机通道提交自助注册。
  - 未绑定：公开路径声明私人手机码，无设备号不签名。
  - 不按路径改报 `app`（android-app 客户端 0 处 `"app"` 类型头）。
  - 构造器参数槽位未把公钥填进 phone/idDoc（与 Kotlin 数据类对齐；JSON 按蛇形字段名绑定 Java record，不按构造器位次）。
- **不是** U01-004/005/010–015 否项。

### 5.2 有 ID：U01-016 已按最小处置闭合

- **定稿**（V1.83 回退 B2）：技术性 / 是 / 是。最小处置：随 007 落地入口后，把商户代提交页文案改为指向实际入口（私人手机 App 的「公司员工自助注册」），不发明新流程。判据 GEN-016、PGM-001。
- **after**：`MainActivity.kt:1242`「自营商户员工请在本人私人手机安装本应用后，从登录页「公司员工自助注册（私人手机）」提交。」按钮文案（`:229`）与屏标题（`:1233`）同字。登录页在未绑定态可见该按钮。无死链。

### 5.3 有 ID：U13-011 已按最小处置闭合

- **定稿**：技术性 / 是 / 是。最小处置：T1:7 上界改 V422 并补「V422 为顶班记录表」；不改其它段落。判据 GEN-016。
- **after**：`:7`「连续编号为 V411—V422（……V421 为寄存柜扫码凭证表、V422 为顶班记录表）」。迁移目录实有 `V411`…`V422__staff_shift_covers.sql`，无 V400–V409。T1 其余当前实现段（注册 Keystore、五态、封顶/事假、柜机单端、latest()）与 B5-r1 闭合口径一致，本句修改未回写其它段落口径。

---

## 6. 新发现清单

### 6.1 有 ID

无。119 个定稿「是」项均闭合。

### 6.2 无 ID

无。

### 6.3 非阻断观察（不构成缺陷、不升 FAIL、不进回退）

同一 APK 若先后写入 `private_phone` 与 `work_phone`/`bar_pos` 两套 prefs，`onCreate` 按工作手机 → 平板 → 私人手机顺序恢复，后写覆盖先写。HTML 把三种设备定为不同用途（BSM L930），不是同一机双绑合同；本席不把它升为无 ID 缺陷。工作手机绑定成功路径当场改 `terminalType=staff_work_phone` 并隐藏自助注册按钮，主路径成立。

grilling / grill-me 无独立工具入口，对抗式由本席自构反例，缺口已记。

---

## 7. 越界检查

| 禁区 | 结果 |
|---|---|
| 三份业务 HTML | after＝HEAD blob，name-status 无 html |
| 锁 / sidecar | 未改；本席实算＝sidecar＝ledger 顶行 |
| ledger 基准 / findings（两树内） | 本 diff 未改这两路径 |
| 主工作区业务源 | HEAD 仍 445a49f4＝origin/main；porcelain 仅审计产物（findings/ledger/modules raw），无业务源改动 |
| 整树覆盖 main | 否。OrganizationMerchantPage / PlatformService / types.ts 取 main |
| 「否」项 | 抽查未作为本批目标修掉（§4） |
| B3-r2 范围 | 只三文件，未顺手修否项或 116 个非本轮目标「是」项 |
| 本席写范围 | 仅本 raw；验证在 /tmp 副本；未 commit/push/PR/merge/部署；未 source 整份 env；未写 `complex_test*`（一次性库 `ufix_b5r2_grok_scratch` 测完 DROP；`complex_test` 仍在库列表、本席未连） |

---

## 8. 独立复跑验证（只读）

副本：`git archive c0ec9b7…` → `/tmp/ufix_b5r2_grok_copy`（软链 node_modules）。后端 rsync 到 `/tmp/ufix_b5r2_grok_backend`，仅改 JDBC 字面量为一次性库 `ufix_b5r2_grok_scratch`。密码只 `grep '^COMPLEX_DATABASE_PASSWORD='`（USER_LEN=7 PASS_LEN=48）。库 CREATE 后测完 DROP。日志 `/tmp/ufix_b5r2_grok_verify.log`。ANDROID_HOME=/home/james/android-sdk。JAVA_HOME=temurin-21。

| 检查 | 本席结果 |
|---|---|
| `verify:openapi` | EXIT 0；contract passed |
| `verify:openapi-examples` | EXIT 0；8 request + 3 response |
| `verify:operation-matrix` | EXIT 0；732 operations |
| `verify:openapi-closure` | EXIT 0；732 operations |
| `verify:domain-ownership-scope` | EXIT 0；334 表 |
| `verify:data-isolation-spec` | EXIT 0 |
| `verify:pii-encryption` | EXIT 0；16 列 |
| `verify:security-baseline`（含 --strict 链） | EXIT 0 |
| `verify:migration-version-order` | 副本无 `.git` 红（环境，EXIT 1）；**worktree 只读复跑 EXIT 0**：本分支新增 12 个迁移，均在 origin/main 最大号 V410 之后 |
| web `npx tsc --noEmit` | EXIT 0 |
| web vitest 5 files | **33/33** EXIT 0（含 U03-008/009 与 OrganizationMerchantPage 15） |
| JVM `gradle test` | BUILD SUCCESSFUL；**30/30** EXIT 0（HeadersTest 含在内，日志 `:test > 30 tests completed`） |
| Android `compileDebugKotlin` | BUILD SUCCESSFUL EXIT 0（含新 `showStaffSelfRegistration` / Keystore 调用；仅 ZXing deprecated 警告） |
| 后端 10 类一次性空库 | MVN_EXIT=0；Tests run **197** F0 E0；M010Regression 44、RegistrationHttp 2、PayrollSafety 7、Attendance 4、StaffJdbc 29、ArchiveDelete 4、StaffScope 3、Verifier **23**、SupervisorReview 44、TerminalDevice 37；flyway `max(installed_rank)=390` **`max(version)=422`**；库已 DROP |

新增/既有可失败断言仍能失败：HeadersTest 未绑定期望无类型头；guidedcreate 空原因 `lastPostBody===null`；RegistrationHttp 无 `device_public_key` → 400（日志含 `请填写私人手机设备公钥`）；M010 事假期望 6000/7800。

副本 `verify:migration-version-order` 因 archive 无 git 红，不折算为业务 finding；以 worktree 同命令绿为准。

---

## 9. 顺从式判据核对（并集 139，按族）

| 族 | 核对结论 |
|---|---|
| GEN-001/006/017 | 自营第二轴已删，只留六类码 `self_operated`；未发明第七类合作方式 |
| GEN-002/008/013/014 | 以 main 为底重放；迁移 V411 起；未整树覆盖 |
| GEN-003/018 | 新增分支有可失败测试（JVM/HTTP/空库）；android-app 仍无仪器测试（U01-008 允许 JVM 侧） |
| GEN-004/005/010 | 公开注册设备证明+频控；柜机设备令牌；PII 加密；两屏 FLAG_SECURE 仍缺＝U01-004 否 |
| GEN-007/012 | 无锁外新闸 |
| GEN-009/011 | 失败事件非 none；成功名不串督导台 |
| GEN-015/016 | 展示中文（否项残留英文不升 FAIL）；016 文案与 T1:7 已与可执行行为一致 |
| BSM-001/028/085/114/120 | 出厂行含上下限；删除仍走存档+最高权限（否项 BSM-004 未映射段不升） |
| BSM-044/045/047 / BEQR-150/153 | 终端类型不按路径改报；私人手机码仅显式通道；POS 登记签名 |
| BSM-086/087 | 事假当期应发；封顶分母按现有列先取全；处罚三线+最高权限 |
| BSM-012 / PGM-032/033/036/038/047 | 设备标识安装时绑定：后端+JVM+**android-app UI 均执行**（U01-007 闭合） |
| PGM-001 | 016 文案全中文且指向真实按钮名 |

---

## 10. 总结论

整模块 **PASS**。

- 三线均执行；85 路径全覆盖；并集 139 判据已核。
- 119 个定稿「是」项闭合，含 B5-r1 否决的 U01-007 与回退定稿 U01-016/U13-011。
- 有 ID 未闭合：无。无 ID 新缺陷：无。业务发明：无。
- 越界检查通过。可执行验证绿（四闸 + 域归属/隔离/PII/基线 + web 33/33 + JVM 30/30 + Android 编译 + 空库 197/197 flyway 422）。
- 本席不行使否决。本票不等于席甲票，也不等于模块 ledger PASS；双 PASS 且 B5 后四值全等后由席甲改状态。
