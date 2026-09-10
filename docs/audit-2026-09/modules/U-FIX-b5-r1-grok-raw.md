CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# U-FIX B5-r1 修复后全量审计（Grok 席乙）

文件版本：V0.01
步骤：B5-r1
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/U-FIX-b5-r1-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

整模块结论：**FAIL**。
不可用 B4-r1 双 PASS、B3 自报或席甲 B5 探针代替本轮整模块三线重审。席甲=Claude Fable 5.1（作者自审票、唯一修复者）；本席 FAIL 具否决权——本席行使否决。无有条件通过。

否决原因（回退统一规则）：定稿「是」项 U01-007 未按最小处置闭合（私人手机 App 自助注册被整段删除，而非在该端以 Keystore 公钥/指纹提交并绑定）；并引入无 ID 新缺陷 2 条（商户代提交页死链文案；T1:7 编号上界仍写 V421，after 树已有 V422）。全部已有 ID 且仍属修复队列 → 整批回 B3（本模块 B3 累计已 1，上限 3）。新问题只登记不修。

U-FIX=XM-009 先例的合成模块，承载 U01–U18 全部已定稿且「发布前须修」归属「是」的 finding ID（117）。定稿依据：docs/audit-2026-09/findings.md V1.81 各段「B2 双PASS定稿」节。操作员唯一业务答复：排班可见天数出厂行上下限＝1 天/31 天（值 7）；自营独立布尔＝删除（questions/U04-001.md 答复节「删除」）。只用既有冻结判据。未新造判据/角色，未询问操作员。本席在主仓 cwd 只写本指定路径；验证在 `/tmp/ufix_b5_grok_copy` 与一次性空库 `ufix_b5_grok_scratch`（已 DROP）。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。python3 hashlib 与 `openssl dgst -sha256` 三工具同值。

| 项 | 值 |
|---|---|
| 锁文件 | `docs/audit-2026-09/plan/criteria-lock.md` V0.11；889020 字节 |
| 独立 SHA256 | `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a` |
| sidecar `plan/criteria-lock.sha256` | 全文一行同值 |
| ledger 顶行 | 同行；`LOCK-AMEND-COUNT: 1`；ledger 版本 V3.04 |
| findings | V1.81 |
| 模块 | U-FIX 合成修复模块（U01–U18 已定稿「是」项整批） |
| 阶段 | B5-r1 整模块全量三线重审（不只看 diff） |
| B3 | 已执行 1 次 |
| before tree | `28dcfec971f6eca651213368c27ec7e893a08305`（＝445a49f4 的树，对象类型 tree） |
| after tree | `e3fd14b6e61f284e3a9dc7380d529e7486c71b8a`（对象类型 tree） |
| 本席仓库外临时索引 `git add -A && git write-tree` | 同 after（TREE_MATCH） |
| 主工作区 HEAD | `445a49f409f0cfbe6dd616ae079be1299824c48f` = `origin/main`；非审计业务源 porcelain=NONE |
| 差异规模 | `git diff --name-status` 85 路径（A 31 / M 54）；`85 files changed, 12156 insertions(+), 7145 deletions(-)` |
| 正式队列「是」（117） | U01-001/002/003/006/007/008/009；U02-001…004；U03-001…009；U04-001/002；U05-001…011；U06-001…007；U07-001…005；U08-001…005；U09-001…005；U10-001…003；U11-001…008；U12-001…005；U13-001…008/010；U14-001…008；U15-001…008；U16-001…007；U17-001…010；U18-001…004 |
| 正式队列「否」（不得据此判 FAIL） | U01-004/005/010–015；U02-005；U03-010–015；U04-003；U05-012；U06-008–011；U07-006/007；U08-006；U09-006；U10-004；U11-009；U12-006；U13-009；U14-009；U15-009；U16-008；U17-011；U18-005 |

三 HTML 取 main 正本；after 树 blob 与 HEAD 全等（BSM `12334b13f3c27a815c9bbf9eea216403fb0d76d1` / PGM `c2588ca1c65a3f196b39a23f299bd56967b5652e` / BEQR `86a717930f9303d4f131ee2864478baa14450e6d`）。工作区 SHA256（本席独立）：

| 文件 | 字节 | 换行 | SHA256 |
|---|---:|---:|---|
| `docs/business-system-map-for-operations-v0.1.html` | 304482 | 2032 | `a7f7b7a30ace54d2534a8cedbd8f3b6d20fdd7a89d3515b18e194d4878ad66fa` |
| `docs/platform-governance-map-for-operations-v0.1.html` | 103015 | 979 | `c4238afda7385693dc3c9b3e71b26edc05a1cbba1e713a4d1fe830f17bb0e71c` |
| `docs/business-enum-quick-reference-v0.1.html` | 133961 | 308 | `4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66` |

U08 取 main 侧四文件 after blob＝HEAD：`PlatformService.java` `fe67a293…`、`OrganizationMerchantPage.tsx` `7d5b3ee9…`、`.test.tsx` `823e37e3…`、`types.ts` `8d26e821…`。name-status 无这四路径（净发布物为空，符合 U04/U08 删除口径）。

适用判据＝U01–U18 各段映射行并集（锁 V0.11，本席 python 抽出）：unique **139**。分部落点：U01 M375=50；U02 M376=50；U03 M012∪M057∪M081∪M290∪M293=21；U04 M365=21；U05 M054=52；U06 M054∪M288=52；U07 M063∪M064∪M396=71；U08 M009=87；U09 M040=106；U10 无 main 映射行＝GEN-001–018；U11 M056=52；U12 M042∪M066=107；U13 M028∪M058∪M411∪M420（及 U13-010 挂 M419，并集仍 60）；U14 M391=18；U15 M392=18；U16 M389=43；U17 M390∪M394=43；U18 M029∪M053∪M394=18。MISSING=[]。闭集：

BEQR-063/074/097/115/143–159/163/166/182–187/189/192；BSM-001–007/009/012/013/026–029/044–050/072–077/084–088/114–120；GEN-001–018；PGM-001–022/024/026–038/040–048/057–060/065/066。

grilling / grill-me 本会话无独立工具入口，按 runbook 记缺口、不冒充调用；对抗式反例由本席自读源与可失败测试构造。

---

## 2. 三线方法（整模块，不只看 diff）

- **第一性原理**：逐段追问定稿缺陷是否真正闭合、修复是否引入新缺陷。删除工作手机登录页自助注册后，HTML「私人手机 App 安装时生成并绑定该手机专属设备标识」由哪一个 U01 文件实际执行；薪酬公式被减数/分母是否按 HTML；柜机是否单端；顶班是否有入口；出厂行是否存在且 GET 不把空上下限当合同；契约与生成物是否同一 json、版本可复现。
- **顺从式**：各段映射判据对照修复后全文（85 路径 + 调用/迁移/契约/生成物上下游）。门禁在副本实跑。
- **对抗式**：假定实现错误。最小反例：无设备号写请求、无设备证明自助注册、自营独立布尔残留、只叠部分生成物、测试只改断言、T1 编号钉、UI 文案指向已删入口。

证据类型：1=三 HTML 原文；2=可执行产物；4=可复现命令输出。

---

## 3. 85 路径覆盖（after 树全文，非抽样宣称）

name-status 85/85 均审。分组覆盖记录：

| 组 | 路径数 | 覆盖方式 |
|---|---:|---|
| U01 安卓应用（含新增 ChargingReceiptService / Manifest） | 5 | 整文件读 MainActivity 登录/绑定/POS/商户注册、TerminalApiClient 传输层、DeviceKeystore、ChargingReceiptService |
| U02 JVM 客户端+测试 | 5 | applyHeaders / StaffSelfRegistrationRequest / HeadersTest 5 例全文 |
| U03/U04 web | 7 | StaffProfilesPage 默认拉取、排班事由 validate、settings/guidedcreate 测试；types.ts / OrganizationMerchantPage＝main |
| U05/U06 员工服务 | 19 | RegistrationService 全文、Locker 单端全文、coverShift、settingScopeKey、payroll SQL 148–232、Controller 幂等两写口 |
| U07 闸/终端 | 7 | Verifier 142–288、SecurityConfig public 路径、RateLimit 规则、DeviceBindRequest.staffId、TerminalDeviceService 661–668 |
| U09 复核 | 1 | isMoneyAction、必填 ApplicationEventPublisher、处罚三线策略 |
| U10 迁移 V411–V422 | 12 | 连续号、V413 出厂行、V419 无 is_self_operated |
| U11/U12 测试 | 11 | M010 6000/7800/deviceProof/e.code；Verifier 23；复核 44（含 U12-003 三条） |
| U13 设计/安全产物 | 5 | T1/T9 V0.13 当前实现段；生成器 reminder_days；基线 --strict |
| U14–U18 契约与工具 | 14 | json 732 / V0.14 / 无 self-operated 路径；四闸 --check；PII 16 列；域归属 334 |

未把未改的 OrganizationMerchantPage / PlatformService 算进 85（取 main 侧，见 §1）。

---

## 4. 逐段结论（修复后全文）

| 段 | 定稿「是」 | 本席裁定 | 要害证据 |
|---|---|---|---|
| U01 | 001/002/003/006/007/008/009 | **段 FAIL**（007 未闭合；001 入口删除成立但留下死链） | 见 §5 |
| U02 | 001–004 | PASS | JVM `applyHeaders` 未绑定不发类型头；`StaffSelfRegistrationRequest` 必填公钥/指纹；HeadersTest 5/5；共享客户端重放 |
| U03 | 001–009 | PASS | 001–007＝不合入商户页（blob＝main）；008 默认不带 `include_archived`；009 编辑态空原因拦截；vitest 含对应例 |
| U04 | 001/002 | PASS | 独立布尔已删；types.ts＝main，无 `selfOperated` |
| U05 | 001–011 | PASS | 分流读 `cooperationForm=="self_operated"`；事假基数固定+台账提成+酒吧提成+奖金；封顶分母固定+奖金且注释禁止把两项称作 HTML 总薪资全部；出厂行 7/1/31 + 缺行 `POLICY_NOT_CONFIGURED`；柜机 kiosk 控制器+一次性 token；history `lockForUpdate=false`；写入强制 `company_id`；两写口 `requireIdempotencyKey` |
| U06 | 001–007 | PASS | `settingScopeKey` 写读共用不回落 projectId；DTO 设备证明；顶班 `coverShift`+V422；003 随 001 收敛 |
| U07 | 001–005 | PASS | bind 写 `staffId`，工作手机缺则拒；钥源私人手机≠工作手机；无设备号 enforce 拒；公开注册 AUTH 频控 |
| U08 | 001–005 | PASS | 四文件＝main；无 `setMerchantSelfOperated` |
| U09 | 001–005 | PASS | `isMoneyAction` 含 `staff.payroll.*`（库无该策略行时走 1 个工作日）；V418 种子 `hr_review`；发布器必填；用例在 U12 |
| U10 | 001–003 | PASS | V411–V422 连续、main V410 保留、无 V400–V409；V419 无独立布尔列；出厂行在 V413 |
| U11 | 001–008 | PASS | 空库 44+2+7+4+29+4+3：无 `withSelfOperated`；事假 6000/7800；deviceProof；无指纹 400 |
| U12 | 001–005 | PASS | Verifier 23（含 Cookie 会话放行、无设备号拒、`DEVICE_CREDENTIAL_INVALID`）；复核 44 含缺 hr 线/DEPARTMENT_REVIEWS_PENDING/排班 one_level_up |
| U13 | 001–008/010 | PASS（T1:7 上界漏 V422 记无 ID，不把 001 整条打回未修） | T1/T9 V0.13 口径句已改；`latest()`；reminder_days 读 canonical；`--strict` 绿 |
| U14 | 001–008 | PASS | 732 operation、V0.14、无 self-operated 路径、自助注册 required 含设备字段、终端仅 `staff_private_phone`、失败非 none、两写口幂等、可见天数描述含 7＋1..31 |
| U15 | 001–008 | PASS | yaml 由生成器同写；`verify:openapi` json↔yaml 绿 |
| U16 | 001–007 | PASS | 附录标题 V0.14；三口成功事件名与服务 `recordSuccess` 一致；STAFF-员工 96 |
| U17 | 001–010 | PASS | 矩阵标题跟 `info.version`；删变更表；taskGroup staff |
| U18 | 001–004 | PASS | 三工具以 main 为底重放；域归属 334；PII 16 列 |

「否」项抽查仍在、本批未以其为目标：U06-008 三处英文拼接仍在 `StaffService.java:216/:419/:1438`；U06-010 `missing_checkout` 仍在；U01-005「跳过，进入首页」仍 1 处；U01-011 注释仍写 `bar_pos` 但绑定实参已是 `tablet`（否项归 M375，不升本模块 FAIL）。

---

## 5. 未闭合「是」项与新缺陷

### 5.1 有 ID：U01-007 未按最小处置闭合

- **定稿**：冲突 / 须修是。位置＝未发布 `MainActivity.kt:1188-1214` 自助注册只交姓名/公司/商户/手机/证件，函数体无 `DeviceKeystore`。HTML PGM L579 / BSM L935「私人手机 App 安装时生成并绑定一个该手机专属的设备标识」。最小处置：与 U01-001 同步，在私人手机端注册/安装时以 Keystore 公钥/指纹提交并绑定。
- **after 树实际**：
  1. `showStaffSelfRegistration` 整函数消失；`showLogin()`（`:196-228`）只剩账号密码，无「公司员工自助注册」。
  2. android-app `TerminalApiClient.kt` 无 `submitStaffSelfRegistration` / `listStaffRegistrationCompanies` / `STAFF_PRIVATE_PHONE`（本席 `rg` 0 命中）。该文件是 U01 范围内私人手机 UI 的唯一 HTTP 客户端。
  3. JVM 客户端（U02）`:143-144` 有带 `STAFF_PRIVATE_PHONE` 的提交方法；后端 `StaffRegistrationService.selfRegister` 已强制公钥/指纹（U05-003 闭合）。这只证明契约与 JVM 侧，不证明 U01 三个安卓应用文件执行了 007。
  4. 未发布输入仍保留 `:1188-1214` 无 Keystore 的函数体；B3 选择删除而非补 Keystore。删除满足 U01-001「工作手机应用去掉自助注册入口」，**不满足** 007「在私人手机端……以 Keystore 提交并绑定」。本 APK 同时承载 BYOD 段（首页「员工自有手机」分组），T9:11 仍写私人手机 App 调用 `POST /self-registrations` 且须带 Keystore。
- **对抗式**：私人手机用户在本应用无法提交自助注册；HTML 该句在 U01 文件上仍落空。后端无证明即 400（`M010RegistrationHttpTests` 空库已绿）不能代替端侧提交。
- **不是** U01-004/005/010–015 否项，也不是 main 既有。

### 5.2 无 ID 新缺陷 1：商户代提交页死链文案

- **位置**：after `MainActivity.kt:1201`「自营商户员工请使用登录页的公司员工自助注册。」
- **事实**：登录页已无该入口（U01-001 删除）。用户按文案无法完成 HTML 公司自有员工自助注册。
- **分类**：技术性（界面文案与可执行行为不一致）。冻结判据 GEN-016、PGM-001。工程缺陷成立。无业务发明。
- **来源**：修复 001 时未改同文件引导句。不在 117 个「是」ID 的最小处置字面内 → 无 ID。

### 5.3 无 ID 新缺陷 2：T1:7 编号上界漏 V422

- **位置**：`docs/r5-spec-T1-staff-tables-dev-design-20260618.md:7` 仍写「连续编号为 V411—V421……V421 为寄存柜扫码凭证表」。
- **事实**：after 树迁移至 V422（`V422__staff_shift_covers.sql`，U06-004）。U13-001 最小处置含「T1 :7 改实际编号」。B3 后半段加 V422 未回写该句。
- **分类**：技术性。GEN-016。不把 U13-001 整条打成未修（口径句与 latest() 已改），但本句是本批引入的新漂移，无独立定稿 ID。

---

## 6. 越界检查

| 禁区 | 结果 |
|---|---|
| 三份业务 HTML | after＝HEAD blob，name-status 无 html |
| 锁 / sidecar | 未改；本席实算＝sidecar＝ledger 顶行 |
| ledger 基准 / findings（两树内） | 本 diff 未改这两路径 |
| 主工作区业务源 | HEAD 仍 445a49f4；非审计 porcelain 空 |
| 整树覆盖 main | 否。OrganizationMerchantPage / PlatformService / types.ts 取 main |
| 「否」项 | 抽查未作为本批目标修掉（§4） |
| 本席写范围 | 仅本 raw；验证在 /tmp 副本；未 commit/push/PR/merge/部署；未 source 整份 env；未写 `complex_test*` |

---

## 7. 独立复跑验证（只读）

副本：`git archive e3fd14b6…` → `/tmp/ufix_b5_grok_copy`（软链 node_modules）。后端 rsync 到 `/tmp/ufix_b5_grok_backend`，仅改 JDBC 字面量为一次性库 `ufix_b5_grok_scratch`。密码只 `grep '^COMPLEX_DATABASE_PASSWORD='`（USER_LEN=7 PASS_LEN=48）。库 CREATE 后测完 DROP。日志 `/tmp/ufix_b5_grok_verify.log`。

| 检查 | 本席结果 |
|---|---|
| `verify:openapi` / examples / operation-matrix / openapi-closure | 全 0；732 operation |
| `verify:domain-ownership-scope` | 0；334 表 |
| `verify:data-isolation-spec` | 0 |
| `verify:pii-encryption` | 0；16 列 |
| `verify:security-baseline`（含 --strict 链） | 0 |
| `verify:migration-version-order` | 副本无 `.git` 红（环境）；**worktree 只读复跑 EXIT 0**：本分支新增 12 个迁移，均在 origin/main 最大号 V410 之后 |
| web `npx tsc --noEmit` | EXIT 0 |
| web vitest 5 files | **33/33** EXIT 0（含 U03-008/009 与 OrganizationMerchantPage 15） |
| JVM `gradle test` | BUILD SUCCESSFUL；7 类 **30/30** F0 E0（HeadersTest 5、DeviceRequestHeadersTest 1） |
| Android `compileDebugKotlin`（ANDROID_HOME=/home/james/android-sdk） | BUILD SUCCESSFUL EXIT 0 |
| 后端 10 类一次性空库 | MVN_EXIT=0；Tests run **197** F0 E0；M010Regression 44、RegistrationHttp 2、PayrollSafety 7、Attendance 4、StaffJdbc 29、ArchiveDelete 4、StaffScope 3、Verifier **23**、SupervisorReview 44、TerminalDevice 37；flyway `max(installed_rank)=390` **`max(version)=422`**；库已 DROP |

新增用例能失败：HeadersTest 未绑定期望无类型头；guidedcreate 空原因 `lastPostBody===null`；RegistrationHttp 无 `device_public_key` → 400（日志含 `请填写私人手机设备公钥`）；M010 事假期望 6000/7800。

副本 `verify:migration-version-order` 因 archive 无 git 红，不折算为业务 finding；以 worktree 同命令绿为准。

---

## 8. 顺从式判据核对（并集 139，按族）

| 族 | 核对结论 |
|---|---|
| GEN-001/006/017 | 自营第二轴已删，只留六类码 `self_operated`；未发明第七类合作方式 |
| GEN-002/008/013/014 | 以 main 为底重放；迁移 V411 起；未整树覆盖 |
| GEN-003/018 | 新增分支有可失败测试（JVM/HTTP/空库）；android-app 仍无仪器测试（U01-008 允许 JVM 侧） |
| GEN-004/005/010 | 公开注册设备证明+频控；柜机设备令牌；PII 加密 |
| GEN-007/012 | 无锁外新闸 |
| GEN-009/011 | 失败事件非 none；成功名不串督导台 |
| GEN-015/016 | 展示中文（否项残留英文不升 FAIL）；**GEN-016 被 5.2/5.3 击中** |
| GEN-016 文档同步 | T1/T9 口径句已改；T1:7 上界漏 V422 |
| BSM-001/028/085/114/120 | 出厂行含上下限；删除仍走存档+最高权限（否项 BSM-004 未映射段不升） |
| BSM-044/045/047 / BEQR-150/153 | 终端类型不按路径改报；私人手机码仅显式通道；POS 登记签名 |
| BSM-086/087 | 事假当期应发；封顶分母按现有列先取全；处罚三线+最高权限 |
| BSM-012 / PGM-032/033/036/038/047 | 设备标识安装时绑定：后端+JVM 有，**android-app UI 无**（U01-007） |
| PGM-001 | 死链文案（5.2） |
| PGM-026 | 合作方式六类，无独立布尔 |
| PGM-029 | 不在 U09 映射，未当支撑 ID；处罚时限走 isMoneyAction 回落 1 日 |
| 其余并集 ID | 本模块无新增实现面或保持 main 既有，不升本批缺陷 |

---

## 9. 问题二分（回退）

**有 ID（队列「是」，未闭合 → FAIL）：** U01-007。

**无 ID（新缺陷，只登记不修 → 整批先回 B2 的规则不适用，因已有 ID 仍在队列；按「全部已有 ID 且仍属修复队列」回 B3）：**

1. 商户代提交页死链文案 `MainActivity.kt:1201`（GEN-016/PGM-001）。
2. T1:7 仍写 V411—V421，after 已有 V422（GEN-016）。

观察（不立项、不阻断以外的事实）：DeviceCredentialVerifierTests `@Test`＝23（与 U12 B1 基数一致）；席甲 B4 曾写 39 已在 B4 记笔误。副本 migration-order 因无 git 红。U01-011 注释 `bar_pos` 仍在，绑定实参 `tablet`，属否项。

---

## 10. 结论

整模块修复后全量审计：**FAIL**。

- 117 个「是」项中 **U01-007 未闭合**（最小处置是私人手机端 Keystore 提交，不是删除入口）。
- 新缺陷 2 条无 ID，只登记。
- 其余段定稿「是」项按最小边界闭合（含 19 个以不合入/取 main 侧完成的删除项）。
- 越界检查通过。
- 可执行验证：web 33/33、JVM 30/30、Android compile 绿、空库 197/197 flyway 422、四闸+PII+隔离+域归属+security-baseline 绿。

本票否决席甲 B5-r1 PASS。下一步按 runbook：有 ID 残留且仍属修复队列 → 整批回 B3（累计将 1→2）。本席未修源、未改三 HTML/锁/sidecar/ledger 基准。

技能（V0.17 收窄）：using-superpowers / verification-before-completion 按存在的意图执行（本席先复跑再写结论）；grilling 无独立工具入口，记缺口。未用 MCP、未派子代理、未问操作员。

## U-FIX B5-r1 回退 B2 归并复核（Grok 席乙）

文件自身版本：V0.02（本段追加；上方 V0.01 B5-r1 全文 20175 bytes 原样保留，不改写第 1 行锁段起点，本段不写独占锁哈希字段行）

- 席位：Grok（固定外审席乙）。步骤：U-FIX B5-r1 回退 B2 归并复核。不是 B1/B5 重审、不是修复、不是模块 PASS、不是 B3。B3-r2 尚未开始，本席不修 after 树。
- 触发：本文件 V0.01 整模块 **FAIL**（U01-007 未闭合 + 无 ID 新缺陷 2 条）否决席甲 B5-r1 PASS。按 runbook §8 / 回退统一规则第 2/3 条二分后整批先回 B2。
- 控制：160-audit-prompt.md V0.17；runbook V0.02 §7/§8；criteria-lock.md V0.11 不变。只用既有冻结判据，无 LOCK-AMEND。
- 正本：`/home/james/complex-management`。`HEAD` = `origin/main` = `445a49f409f0cfbe6dd616ae079be1299824c48f`（本席 `git rev-parse`）。修复 worktree `/home/james/wt/ufix-audit-20260910`；本席以仓库外临时索引 `GIT_INDEX_FILE=$(mktemp -d)/index git add -A && git write-tree` 复核 after 树仍为 `e3fd14b6e61f284e3a9dc7380d529e7486c71b8a`。
- 席甲：Claude Fable 5.1（作者自审票、唯一修复者）。本席 FAIL 具否决权——本批不行使否决。
- 本席本批 B2：**PASS**（U01-016、U13-011 逐 ID 全 PASS；U01-007 沿用原 ID 与原处置、不在候选重登；其余 116 个「是」项 B3-r2 不重做）
- 本追加段不写独占锁哈希字段行。正文实算（命令标签，非独占行）：`sha256sum docs/audit-2026-09/plan/criteria-lock.md` → `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`。三工具同值（`sha256sum` / `python3 hashlib` / `openssl dgst -sha256`）；锁文件 889020 字节、版本 V0.11；sidecar `plan/criteria-lock.sha256` 全文一行同值；ledger 顶行同值；`LOCK-AMEND-COUNT: 1`。未改锁/sidecar/ledger/findings/questions/HTML/源/主索引/after 树。
- 无 MCP、无子代理、无第三席、不问操作员。未 commit/push/PR/merge/部署。未在主仓 cwd 写除本指定路径外的任何文件。未对任何 `complex_test*` 库写。必要核对只读。追加稿经 `/tmp/ufix-b5r1-b2-append.md` 中转，落盘后删除。
- grilling / grill-me 本会话无独立工具入口，按 runbook 记缺口、不冒充调用；对抗式反例由本席自读 after 树原文构造。

此票只判断 findings.md V1.82 顶部「当前 U-FIX B5-r1 回退 B2 候选」相对触发回退的一对 B5-r1 raw（本文件 V0.01 + 席甲 self-raw V0.01）的全批差集、GEN-017 四分类、工程缺陷、「发布前须修」是/否与最小处置。必须另有席甲本批 B2 PASS 且本席亦 PASS 才能定稿。不覆盖上方 V0.01 B5 **FAIL** 原文。

### 本批输入（完整指定文件，本席实算）

| 路径 | 版本 | bytes | SHA256 |
|---|---|---:|---|
| `docs/audit-2026-09/modules/U-FIX-b5-r1-grok-raw.md` 前缀（本席 B5-r1，本段追加前） | V0.01 | 20175 | `6b7b91e158111c3caa087f6acc7dcdee4345603b738a23b25528e9cba8225edf` |
| `docs/audit-2026-09/modules/U-FIX-b5-r1-self-raw.md` | V0.01 | 14038 | `5ff8a17794279cf385cb6ae34060ee4cca5738692814d2501ed15b55df6ab456` |
| `docs/audit-2026-09/findings.md` 顶部候选节 | V1.82 | 719359（全文） | `06c3b4607c24a504bb738ccc519fbeacc7c1c1ed7c7288361c6969f1df9f3548` |
| `docs/audit-2026-09/ledger.md` 顶部节（只读） | V3.06 | 556709（全文） | `e8f14e18a95730b1a00d299708c28abce162202ec74da80e38cb7c08f90ec1eb` |

候选形态：V1.82 顶部新 ID 2（U01-016、U13-011）；U01-007 沿用不在此重登；添加 0；冲突 0；questions 0；无 HTML 修改申请。本文件独占锁哈希字段行仍仅文首一处，本段未新增。

本席独立核 after 树原文（不采信席甲转述）：

- `MainActivity.kt:1201` statusText 原文「商户代员工提交资料，由对应域公司审批。自营商户员工请使用登录页的公司员工自助注册。」
- `showLogin()` `:196-228` children 仅 status / username / password / loginButton，无「公司员工自助注册」；`fun showStaffSelfRegistration` 0 处；`TerminalApiClient.kt` 对 `submitStaffSelfRegistration` / `listStaffRegistrationCompanies` / `STAFF_PRIVATE_PHONE` 0 命中。
- T1 `:7` 原文「连续编号为 V411—V421……V421 为寄存柜扫码凭证表」；T1 全文仅此 1 处 V421/V422 编号句。
- 迁移目录含 `V422__staff_shift_covers.sql`（注释写明 U06-004 顶班记录）。
- 首页仍有「员工自有手机(BYOD 一期)」分组（`:304`）；T9 `:11` 已写「私人手机 App……工作手机应用不提供自助注册入口」——支撑 007 原处置与 016 改指向，不另开 ID。

### 差集（相对本席 B5-r1 §5.1/§5.2/§5.3 与 §4 逐段裁定）

| 本席 B5-r1 | 候选 ID | 差集结论 |
|---|---|---|
| §5.1 有 ID：U01-007 未按最小处置闭合（私人手机端 Keystore 提交并绑定，不是删除入口） | 不在本表重登 | **沿用原 ID**。与回退规则第 2 条及本席 §5.1 一致。不得改处置、不得改分类、不得并入 016。 |
| §5.2 无 ID 新缺陷 1：`MainActivity.kt:1201` 文案指向已删入口 | U01-016 | **并入**。原意未改写。ID 接 U01-015 之后，永久不复用。 |
| §5.3 无 ID 新缺陷 2：T1:7 上界仍 V421，after 已有 V422 | U13-011 | **并入**。原意未改写。不把 U13-001 整条打回未修（本席 §4/§5.3 已写明）。ID 接 U13-010 之后，永久不复用。 |
| §4 其余 17 段定稿「是」项 PASS（117−1＝116） | 不在本表 | **B3-r2 不重做**。与本席 §4 逐段裁定一致。 |
| §9 观察（Verifier `@Test`＝23；否项残留；副本无 git 的 migration-order 红） | 不新开 | 候选未另开 ID，正确。 |

漏并：无。多登：无。第三条根因：无。无 LOCK-AMEND、无 HTML 修改申请、无 2.2 添加项。席甲 B5-r1 票 PASS、本轮候选未把席甲「无新缺陷」写成差集项——正确（该票已被本席 B5 否决，不是本批归并对象）。

路由勘误（非阻断、不改 B5 原文）：本席 V0.01 §9 写「无 ID 新缺陷但已有 ID 仍在队列 → 回 B3」。runbook §8 明文「只要有一条没有定稿 ID……整批先回 B2」。混合批（有 ID 残留 007 + 无 ID 两条）必须先 B2。席甲按第 2/3 条回 B2 并分配 016/011，路由正确。本票接受该路由，不把 §9 那句当成否决理由。

### 沿用项：U01-007（不在候选重登）

定稿（findings U01 B2-r1）：冲突 / 工程缺陷是 / 发布前须修是。最小处置：「与 U01-001 同步——在私人手机端注册/安装时以 Keystore 公钥/指纹提交并绑定，服务端按 token 派生本人」。判据 PGM-033、BSM-045、BSM-120、GEN-002。

本席 §5.1 原意：B3-r1 删除工作手机登录页自助注册满足 U01-001，**不满足** 007；本 APK 同时承载 BYOD 段，android-app 无端侧提交。候选节与 ledger V3.06 顶部节均写「沿用原 ID，处置不变：私人手机端以 Keystore 公钥/指纹提交并绑定，不是删除入口」——与 §5.1 一致。

B3-r2 落地约束（不改处置原文）：入口加在私人手机/BYOD 通道，禁止加回 `showLogin()`；须在 android-app 实际调用提交（Keystore 公钥/指纹），不得只靠 JVM/后端已绿宣称 007 闭合。

### 116 个已闭合「是」项

本席 B5-r1 §4：U02–U12、U14–U18 段 PASS；U13 段 PASS（T1:7 上界另记无 ID）；U01 段 FAIL 仅 007 未闭合 + 001 删除留下死链（现 016）。117−007＝116。候选「B3-r2 不重做」与该逐段裁定一致。B3-r2 只修 U01-007、U01-016、U13-011。

### 逐 ID 票（U01-016、U13-011）

| ID | 分类 | 工程缺陷 | 发布前须修 | 本席票 | 与 §5.2/§5.3 原意 | 理由 |
|---|---|---|---|---|---|---|
| U01-016 | 技术性 | 是 | 是 | **PASS** | 一致 | 见重点 (a)。无业务发明。 |
| U13-011 | 技术性 | 是 | 是 | **PASS** | 一致 | 见重点 (b)。无业务发明。 |

发布前须修候选「是」= U01-016、U13-011（2）。添加 0。与候选汇总一致。

### 重点 (a) U01-016 — **PASS（技术性 / 是 / 是）**

四分类：不是冲突（HTML「公司自有员工由本人在私人手机 App 注册」已由 U01-001/007 定稿，本条不是新的 HTML↔代码业务对立）；不是细化；不是添加（不新造注册产品、不恢复登录页入口）。技术性成立：同文件引导句与可执行行为不一致，根因是 B3-r1 修 001 时未改 `:1201`。

冻结判据：GEN-016（界面完成声明不可从当前范围复现）、PGM-001（可执行声称须有实际行为证据）与本席 §5.2 引用一致，成立。非阻断补强：GEN-002（声明的入口须能执行）同样命中；不改候选、不改票。

最小处置「随 U01-007 落地私人手机端自助注册入口后，把该句改为指向实际入口（私人手机 App 的「公司员工自助注册」），不发明新流程」＝最懒可行：保留「自营商户员工走公司路径」这句已有分流（T1 `:11` / T9 `:11` / HTML 自营视同公司自有），只改地点。禁止读成：把按钮加回登录页（回退 U01-001）；删除该句而不给实际入口（比改指向更懒，但丢掉已有分流提示，不是本句的最小忠实修法）；发明第三套注册产品。

「发布前须修」是：合规。按文案无法完成 HTML 公司自有员工自助注册，属发布面缺陷，不得降为登记否项。

### 重点 (b) U13-011 — **PASS（技术性 / 是 / 是）**

四分类：HTML 不规定 Flyway 号，不是冲突；不是细化；不是添加（V422 已由 U06-004 落地）。技术性成立：文档编号上界与迁移目录漂移。

冻结判据 GEN-016 成立。U13-001 原处置含「T1 `:7` 改为实际编号」，B3-r1 已把 V400—V409 改成 V411—V421，口径句与 `latest()` 已改——本席 §4 因此不把 001 整条打回未修。V422 是后半段才加的顶班表，`:7` 未回写，是本批新漂移，单列 011 正确，禁止并回 001。

最小处置「该句上界改 V422 并补一句「V422 为顶班记录表」；不改其它段落」＝最懒可行。T1 仅 `:7` 一处编号句；只改上界不补末项注解，读者会把 V422 误当成寄存柜扫码凭证表。不扩改 T9、不改迁移、不发明新表。

「发布前须修」是：合规。GEN-016 命中纳入范围的设计说明；不得降为观察。

### 非阻断修正（不改票、不改分类、不发新 ID）

1. U01-016 冻结判据可并列 GEN-002；主支撑仍是 GEN-016。PGM-001 用其「可执行声称」句成立，锚点正文是治理层定位，不作唯一支撑。
2. U01-016 的「实际入口」标签沿用已有「公司员工自助注册」四字即可，不必另造按钮名。
3. U13-011 补句以 V422 文件头「顶班记录表」为准，与 U06-004 同名，不写 HTML 没有的审批规则。
4. B3-r2 三 ID 同批、其余 116 不动；修完取新 after tree → B4-r2 → B5-r2。本席不预先宣称那些步骤的结论。

### 本批最终

**PASS**

逐 ID：U01-016 PASS；U13-011 PASS。U01-007 沿用。差集空。最小修正仅上列非阻断四条。无有条件通过。本票不等于双席合取或模块 PASS；须席甲随后把 B2 票追加到 `modules/U-FIX-b5-r1-self-raw.md` 且亦 PASS 才能定稿进 B3-r2。

技能（V0.17 收窄）：using-superpowers / receiving-code-review（先核 after 树与候选差集再接受或反驳，禁止表演性同意，B2 不修复）/ verification-before-completion（先核哈希与原文再给票）/ ponytail-review（最懒可行，不落地）/ grilling 记缺口。未用 MCP、未派子代理、未问操作员。
