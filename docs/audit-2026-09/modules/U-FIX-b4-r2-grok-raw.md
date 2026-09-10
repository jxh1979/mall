CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# U-FIX B4-r2 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r2
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/U-FIX-b4-r2-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r2 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B3 自报红→绿代替本轮完整审计。席甲=Claude Fable 5.1（作者自审票、唯一修复者）；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。

本轮只审 B3 第 2 次整批修复的「修复内容」本身（before/after 两树全文 diff），不是 B1/B5 重审、不是修复、不是模块 PASS。背景：B4-r1 双 PASS 后本席 B5-r1 整模块 FAIL（U01-007 未闭合 + 无 ID 新缺陷 2 条）→ 回退 B2 双 PASS 定稿 U01-016、U13-011（findings V1.83 顶部节）→ B3-r2 只修三项：U01-007、U01-016、U13-011。其余 116 个「是」项已在 B5-r1 裁定闭合，本轮不得重修。「否」项（含 U01-004 两屏 secure）不得被修。定稿依据：docs/audit-2026-09/findings.md V1.83。只用既有冻结判据。未新造判据/角色，未询问操作员。落盘顺序：本席先派发；席甲独立落盘 self-raw 后本席才对照其归因表，本席归因不依赖席甲表。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 全文一行同值。ledger 文首锁行同值。criteria-lock 版本仍 V0.11。本批 B3 两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）。

| 项 | 值 |
|---|---|
| 模块 | U-FIX 合成修复模块（U01–U18 已定稿「是」项整批） |
| 阶段 | B4-r2 只读审计 B3 第 2 次整批修复的「修复内容」本身 |
| 本轮只修 | U01-007、U01-016、U13-011 |
| 其余 116 个「是」 | B5-r1 已裁定闭合，本轮不得重修 |
| 「否」项（不得修） | 含 U01-004 两屏 secure；U01-005/010–015 等登记/归 M375 项 |
| B3 | 已执行 2 次（本轮为第 2 次），模块仍 IN_PROGRESS |
| before（对象类型 tree，可解析） | `e3fd14b6e61f284e3a9dc7380d529e7486c71b8a`（＝B3-r1 的 after tree，B4-r1/B5-r1 审计对象） |
| after（对象类型 tree，可解析） | `c0ec9b7ab6bc38f8197830b7283af4dc2356b61b` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary e3fd14b6e61f284e3a9dc7380d529e7486c71b8a c0ec9b7ab6bc38f8197830b7283af4dc2356b61b`（无路径过滤，覆盖 tracked/staged/untracked） |
| 实际差异 | 11931 bytes；SHA256 `051fd0df6cfa899720f003651712026c830875bc019605efaccaa08da609faab`；UTF-8；3 文件（M 3 / A 0 / D 0）；+61 / −9；默认上下文 **7** 块；`-U0` **11** 块 |
| 与 `/tmp/ufix_b4_r2.diff` | 字节与 SHA256 全等（本席自算为准，副本仅对照）；`cmp` 相等 |
| `git diff --check` | exit 0（无空白错误） |
| 当前工作区临时索引 `git add -A && git write-tree`（worktree 内、仓库外 GIT_INDEX_FILE） | `c0ec9b7ab6bc38f8197830b7283af4dc2356b61b`＝after |
| worktree HEAD | `445a49f409f0cfbe6dd616ae079be1299824c48f`（树 `28dcfec9…`＝origin/main；未提交） |
| 主工作区 HEAD | `445a49f409f0cfbe6dd616ae079be1299824c48f` = `origin/main` |
| findings.md / ledger.md | 两树 blob 相同（本 diff 未改）：findings `c9e2a00b513a4b4460d439100cbc467570c2d027`，ledger `aaa74447e99bed7876c393d3f107a84440ff5036` |
| 三 HTML 两树 blob | BSM `12334b13f3c27a815c9bbf9eea216403fb0d76d1` / PGM `c2588ca1c65a3f196b39a23f299bd56967b5652e` / BEQR `86a717930f9303d4f131ee2864478baa14450e6d`，完全相同且＝main HEAD |
| 锁/sidecar 两树 blob | 相同（未改） |
| 三文件版本行 | 两树相同：MainActivity `V0.05`、android-app TerminalApiClient `V0.04`、T1 `V0.13`。本轮无新版本递增（同一未发布批次已在 B3-r1 递增一次，本席不要求再递增） |

`git diff --shortstat --binary`：`3 files changed, 61 insertions(+), 9 deletions(-)`。

全文 diff 见文末附录 A（与本席自取 `/tmp/ufix_b4_r2_grok.diff` 字节全等）。

---

## 2. name-status 全表（3）

```
M	apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt
M	apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt
M	docs/r5-spec-T1-staff-tables-dev-design-20260618.md
```

路径增量：A 0 / D 0。未触及 B3-r1 已改的其余 82 路径，也未触及三 HTML / 锁 / sidecar / ledger / findings。

三文件 after blob：

| 路径 | before blob | after blob |
|---|---|---|
| `MainActivity.kt` | `edb6404f109a30923c4802d6e374257e00469d66` | `32a68cc545b2a9a3725ce53c83d550a5e0ba313d` |
| android-app `TerminalApiClient.kt` | `42e8c288ed780f99aae0d6e300145e5cc37aa436` | `1e2a0f69d115f220696156f877b92f0b988cb6da` |
| T1 规格 | `04732ef6e6f465176e2a3e9f2d3e7ce683b91b02` | `4d56a568eb64096d757621d8385666f2dc36dc66` |

---

## 3. §5 闸 3 独立逐块 / 逐行归因

归因规则（本席自定、与派发一致）：本轮每一行须归到 U01-007 / U01-016 / U13-011 之一。「文件版本」行若出现则随所属 ID；本轮 diff 无版本行。禁止「否」项 ID。注释里出现 U01-001 字样不另开重修 001——U01-007 定稿本就写「与 U01-001 同步」「入口只在私人手机通道」，该注释是同根约束说明，本席把这些行归 U01-007。

### 3.1 默认上下文 7 块

| 路径 | hunk | + | − | 本席归属 |
|---|---|---:|---:|---|
| `MainActivity.kt` | `@@ -124,6 +124,8 @@ class MainActivity : Activity() {` | 2 | 0 | U01-007 |
| `MainActivity.kt` | `@@ -222,7 +224,9 @@ class MainActivity : Activity() {` | 3 | 1 | U01-007 |
| `MainActivity.kt` | `@@ -1192,13 +1196,50 @@ class MainActivity : Activity() {` | 38 | 1 | 混合：见 −U0 拆分（37 行 U01-007 + 1 行 U01-016） |
| android-app `TerminalApiClient.kt` | `@@ -24,6 +24,9 @@ import kotlinx.serialization.json.decodeFr` | 3 | 0 | U01-007 |
| android-app `TerminalApiClient.kt` | `@@ -104,6 +107,14 @@ class TerminalApiClient(` | 8 | 0 | U01-007 |
| android-app `TerminalApiClient.kt` | `@@ -254,26 +265,26 @@ class TerminalApiClient(` | 6 | 6 | U01-007 |
| `docs/r5-spec-T1-staff-tables-dev-design-20260618.md` | `@@ -4,7 +4,7 @@` | 1 | 1 | U13-011 |

UNMAPPED（默认块）= 0。混合块不单独算未归因，以 −U0 为准拆到三 ID 之一。

席甲默认表把上述混合块整块标成 U01-016。过粗：该块含 `showStaffSelfRegistration()` 全文（U01-007）与商户代提交文案一行（U01-016）。席甲 −U0 表已拆开。闸 3 要求「每一行能归因到三 ID 之一」，不是两席标签集逐字相等。本席以 −U0 为准。

### 3.2 −U0 细粒度 11 块（本席独立解析，与 `grep -c '^@@'` 于 `-U0` diff 一致）

| 路径 | hunk | + | − | 本席归属 |
|---|---|---:|---:|---|
| `MainActivity.kt` | `@@ -126,0 +127,2 @@ class MainActivity : Activity() {` | 2 | 0 | U01-007 |
| `MainActivity.kt` | `@@ -225 +227,3 @@ class MainActivity : Activity() {` | 3 | 1 | U01-007 |
| `MainActivity.kt` | `@@ -1194,0 +1199,37 @@ class MainActivity : Activity() {` | 37 | 0 | U01-007 |
| `MainActivity.kt` | `@@ -1201 +1242 @@ class MainActivity : Activity() {` | 1 | 1 | U01-016 |
| android-app `TerminalApiClient.kt` | `@@ -26,0 +27,3 @@ import kotlinx.serialization.json.decodeFr` | 3 | 0 | U01-007 |
| android-app `TerminalApiClient.kt` | `@@ -106,0 +110,8 @@ class TerminalApiClient(` | 8 | 0 | U01-007 |
| android-app `TerminalApiClient.kt` | `@@ -257,2 +268,2 @@ class TerminalApiClient(` | 2 | 2 | U01-007 |
| android-app `TerminalApiClient.kt` | `@@ -262,2 +273,2 @@ class TerminalApiClient(` | 2 | 2 | U01-007 |
| android-app `TerminalApiClient.kt` | `@@ -270 +281 @@ class TerminalApiClient(` | 1 | 1 | U01-007 |
| android-app `TerminalApiClient.kt` | `@@ -276 +287 @@ class TerminalApiClient(` | 1 | 1 | U01-007 |
| T1 规格 | `@@ -7 +7 @@` | 1 | 1 | U13-011 |

- **UNMAPPED = 0**（每一行命中 U01-007 / U01-016 / U13-011 之一）。
- **命中「否」队列 ID = 0**。
- **命中本轮三 ID 以外的定稿「是」项（作为唯一归属）= 0**。席甲表额外并列 U01-001，本席不把 001 当本轮目标 ID；那些行仍同时是 U01-007 的最小处置（入口只在私人手机通道、只声明一次）。
- 计数：U01-007 共 10 块（+59/−7），U01-016 1 块（+1/−1），U13-011 1 块（+1/−1）。合计 +61/−9，与 shortstat 一致。

对照席甲 `/tmp/ufix_b4_r2/attrib.md`（7 块）与 `/tmp/ufix_b4_r2_u0/attrib.md`（11 块）：−U0 路径/hunk 头/行数与本席全等；席甲在 U01-007 行并列 U01-001。交集非空且本席归属均在三 ID 内 → 闸 3 PASS。

---

## 4. 修复内容：逐 ID 核对

分类顺序：冲突 → 细化 → 技术性 → 添加。判定：是否与定稿最小处置一致、是否最懒可行、是否引入新缺陷或新业务发明。

### 4.1 U01-007（冲突；有 ID，处置不变）

定稿（U01 B2-r1 + V1.83 顶部）：私人手机端注册/安装时以 Keystore 公钥/指纹提交并绑定；入口只在私人手机通道。HTML PGM L579 / BSM L935：「私人手机 App 安装时生成并绑定一个该手机专属的设备标识，把手机和员工绑定；不读取手机本机的硬件识别码。」B5-r1 否决点：B3-r1 删除 `showStaffSelfRegistration` 而非补 Keystore。

after 树实际（本席读源，非抽样）：

1. android-app `TerminalApiClient`：顶层 `STAFF_PRIVATE_PHONE = "staff_private_phone"`（与 JVM 客户端同值；android-app 排除 JVM 同名文件，无重复定义）。仅 `listStaffRegistrationCompanies()` / `submitStaffSelfRegistration()` 两条公开路径把该码传入 `get`/`post` 的 `terminalTypeOverride`。`submitStaffRegistration` 及其余 `get`/`post` 调用不传覆盖，仍走绑定后的 `terminalType`。`send()` 仅在 `deviceId != null && deviceSigner != null && path != login` 时签名；自助提交在 `onSuccess` 之前 `deviceId` 仍为空 → 公开路径未绑定不签名。与 JVM `applyHeaders` 同形，不是按路径改报 `app`。
2. `MainActivity.showStaffSelfRegistration`：`StaffSelfRegistrationRequest(requestKey, companyId, name, DeviceKeystore.devicePublicKeyBase64(), deviceFingerprintHash(), merchant?, phone?, idDoc?)`，参数序与 `TerminalModels.kt` 八字段 data class 一致（公钥/指纹为必填 String）。不读 IMEI/ANDROID_ID 等硬件识别码。
3. 成功后：`private_phone.device_id = fingerprint`，`api.deviceId = fingerprint`，`api.terminalType = STAFF_PRIVATE_PHONE`。`onCreate` 在工作手机/平板 prefs 之后按该指纹恢复私人手机身份。`api` 懒加载已设 `deviceSigner = DeviceKeystore::sign`（B3-r1 既有），故绑定后非登录请求可签名；公开提交当时不签名。
4. 入口：`showLogin()` 仅当 `api.terminalType == null` 显示按钮「公司员工自助注册（私人手机）」。工作手机/平板已恢复类型时不显示。与 U01-001「工作手机应用去掉自助注册入口」同机共存：未绑定才进私人手机通道。
5. 后端公开口本轮未改：`DeviceCredentialInterceptor` 豁免 `/api/v1/staff/self-registrations` 与 `/registration-companies`（身份证明在请求体，B5-r1 已闭合）。本轮不重修 U05/U07。
6. 新屏 `screen(...)` 未传 `secure = true`。U01-004 为「否」（登记，不单独阻断）；本轮未修否项，与派发一致。

对抗式：删掉 Keystore 两行则请求体缺设备证明，服务端 400（B3-r1 `M010RegistrationHttpTests` 已可失败）。工作手机已绑定后登录页无按钮，不能从工作手机通道再走自助注册。未发明第七类终端、未自报 `app`、未读取硬件识别码。

裁定：**修好**。最小处置落地，无业务发明。

### 4.2 U01-016（技术性）

定稿：随 U01-007 把商户代提交页 `MainActivity.kt:1201` 文案改为指向私人手机 App 的「公司员工自助注册」实际入口；判据 GEN-016、PGM-001；不发明新流程。回退 B2 非阻断修正：文案以登录页按钮名为准。

after：旧句「自营商户员工请使用登录页的公司员工自助注册。」**0 处**。新句「自营商户员工请在本人私人手机安装本应用后，从登录页「公司员工自助注册（私人手机）」提交。」**1 处**（`:1242`）。按钮文案同为「公司员工自助注册（私人手机）」（`:229`）。指向实际入口，未另开流程。

裁定：**修好**。

### 4.3 U13-011（技术性）

定稿：T1:7 上界改 V422 并补「V422 为顶班记录表」；不改其它段落。

after `:7`：「连续编号为 V411—V422……V421 为寄存柜扫码凭证表、V422 为顶班记录表」。本文件其余 hunk 为 0。用语「顶班记录」与 V422 文件名/U06-004 一致。

裁定：**修好**。只改 T1:7。

### 4.4 版本铁律

三文件版本行两树相同（V0.05 / V0.04 / V0.13），diff 无版本行。同一未发布批次已在 B3-r1 递增一次，本轮不再递增。本席不因此 FAIL。

### 4.5 新缺陷 / 新业务发明

本席未把下列现象升为无 ID 新缺陷（故不触发回退二分）：

- `onCreate` 私人手机 prefs 写在工作手机/平板之后，多份 prefs 同时存在时后写覆盖——与 B3-r1 已闭合的工作手机→平板后写覆盖同形；双角色同装属 U01-010「否」首页全按钮，本轮不修否项。
- 本轮未新增 android 仪器测试——U01-008 已在 B5-r1 按「允许 JVM 侧」闭合；JVM `TerminalApiClientHeadersTest` 5 例仍在（本轮未改 JVM 源，30/30 仍绿）。
- 新屏未加 FLAG_SECURE——U01-004「否」，明确不得修。

无业务发明（终端码、字段、按钮名、迁移上界均为既有契约与定稿处置）。

---

## 5. 越界检查（闸 3）

| 禁区 | 结果 |
|---|---|
| 三份业务 HTML | 两树 blob＝HEAD，name-status 无 html |
| 锁 / sidecar | 两树 blob 全等；本席实算＝sidecar＝ledger 顶行 |
| ledger 基准 / findings（两树内） | blob 全等；本 diff 未改这两路径 |
| 主工作区业务源 | HEAD 仍 445a49f4＝origin/main；本席未写 worktree/主仓源（仅本 raw） |
| 本轮三 ID 以外的定稿「是」项 | name-status 仅三文件；未改 JVM 客户端、后端、迁移、契约、其余 U13 段落 |
| 「否」项（含 U01-004 两屏 secure） | 新屏未加 FLAG_SECURE；抽查「跳过，进入首页」等否项不在本 diff |
| 本席写范围 | 仅本 raw；验证在 `/tmp/ufix_b4_r2_grok_copy`（`git archive` after 树）；未 commit/push/PR/merge/部署；未 source 整份 env；未写 `complex_test*` |

---

## 6. 独立复跑验证（只读）

副本：`git archive c0ec9b7ab6bc38f8197830b7283af4dc2356b61b` → `/tmp/ufix_b4_r2_grok_copy`（3665 文件）。`hash-object` MainActivity＝after blob `32a68cc5…`。JAVA_HOME=`/usr/lib/jvm/temurin-21-jdk-amd64`，ANDROID_HOME=`/home/james/android-sdk`。未在 worktree/主仓跑写模式构建。

席甲声称 worktree `compileDebugKotlin` / JVM test 日志 `/tmp/ufix_r2_android_wt.log`、`/tmp/ufix_r2_jvm_wt.log`：**本席所见均为 0 字节**，不以之为据。`/tmp/ufix_r2_predeploy.log` 3612 字节含各闸 PASS 句（本席核读，未在本席副本复跑 predeploy；T1 单行文档不作为本轮可执行门禁否决点）。

| 检查 | 本席结果 |
|---|---|
| 临时副本 JVM `gradle --no-daemon test`（`apps/android-terminal`） | EXIT 0；BUILD SUCCESSFUL in 6s；XML 合计 **30 tests / 0 failures / 0 errors / 0 skipped**（HeadersTest 5、DeviceRequestHeadersTest 1、FieldWorkflow 9、Serialization 8、Ndef 3、Store 2、P0Fixture 2） |
| 临时副本 Android `gradle --no-daemon compileDebugKotlin`（`apps/android-terminal/android-app`，ANDROID_HOME=/home/james/android-sdk） | EXIT 0；BUILD SUCCESSFUL in 7s；17 actionable；仅既有 IntentIntegrator/Parcelable 弃用警告，无 error |
| 旧死链文案 | after MainActivity 0 处「请使用登录页的公司员工自助注册」 |
| T1:7 | 含「V411—V422」与「V422 为顶班记录表」 |
| `git diff --check` | 0 |

本轮未改 JVM 源；30/30 证明共享客户端与 Headers 断言未被这次 android-app 传输层签名改动破坏（android-app 排除 JVM `TerminalApiClient.kt`）。

---

## 7. 发现的问题（有 ID / 无 ID 二分）

有 ID：无。
无 ID：无。

不触发回退统一规则。本席 B4-r2 票 PASS，不行使否决。

---

## 8. 覆盖记录

| 文件 | 覆盖 |
|---|---|
| android-app `MainActivity.kt` | 本 diff 全部 hunk；onCreate 恢复序、showLogin 入口闸、showStaffSelfRegistration 全文、商户代提交文案、screen() 默认 secure=false、与工作手机/POS 绑定成功写 prefs 对照 |
| android-app `TerminalApiClient.kt` | 常量、两条公开方法、get/post/open 默认参数、send() 签名条件、其余调用不传 override |
| T1 规格 | 仅 :7；其余段落无 hunk |
| 共享 `TerminalModels.kt` / DeviceKeystore / 拦截器豁免 | 本 diff 未改；只读核对构造函数序与公开口豁免，确认 U01-007 调用面可编译、公开提交不走设备闸 |

三线：第一性原理（删除入口则 HTML L579/L935 在 U01 三文件再次落空——本轮已补回 Keystore 提交）；顺从式（三 ID 最小处置逐条对照 after 源）；对抗式（未绑定签名、工作手机已绑无按钮、构造函数序、override 泄漏、否项被顺手修、版本再递增、T1 其它段被改）。grilling 工具本会话无独立入口，按 runbook 记缺口、不冒充调用。

---

## 附录 A — 无 pathspec 完整 binary diff 原文

本席命令：`git diff --binary e3fd14b6e61f284e3a9dc7380d529e7486c71b8a c0ec9b7ab6bc38f8197830b7283af4dc2356b61b`（worktree 内）。11931 bytes，SHA256 `051fd0df6cfa899720f003651712026c830875bc019605efaccaa08da609faab`。

```diff
diff --git a/apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt b/apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt
index edb6404f..32a68cc5 100644
--- a/apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt
+++ b/apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt
@@ -124,6 +124,8 @@ class MainActivity : Activity() {
     // U01-001：终端身份由本机绑定类型决定并只声明一次，重启后从本地绑定记录恢复（工作手机 / 平板 POS）。
     workPhonePrefs.getString("device_id", null)?.takeIf { it.isNotBlank() }?.let { api.deviceId = it; api.terminalType = "staff_work_phone" }
     posPrefs.getString("device_id", null)?.takeIf { it.isNotBlank() }?.let { api.deviceId = it; api.terminalType = "tablet" }
+    // U01-007：私人手机在本人注册时已把 Keystore 公钥/指纹绑定为员工设备凭证，重启后按登记的指纹恢复私人手机身份（只声明一次）。
+    getSharedPreferences("private_phone", MODE_PRIVATE).getString("device_id", null)?.takeIf { it.isNotBlank() }?.let { api.deviceId = it; api.terminalType = STAFF_PRIVATE_PHONE }
     // U01-002：拔线接收器由常驻前台服务持有并在开机后重注册；本页只消费投递（进程被回收期间的拔线在下次登录后补记）。
     ChargingReceiptBridge.handler = { runOnUiThread { recordWorkPhoneReceipt("charging_disconnect") } }
     ChargingReceiptService.start(this)
@@ -222,7 +224,9 @@ class MainActivity : Activity() {
     setContentView(
       screen(
         title = "综合体终端 · 登录",
-        children = listOf(status, username, password, loginButton),
+        // U01-001/U01-007：自助注册只属于员工私人手机通道——本机已绑定为工作手机/平板时不显示。
+        children = listOfNotNull(status, username, password, loginButton,
+          if (api.terminalType == null) button("公司员工自助注册（私人手机）") { showStaffSelfRegistration() } else null),
         root = true,
       ),
     )
@@ -1192,13 +1196,50 @@ class MainActivity : Activity() {
     ))
   }
 
+  /** U01-007（PGM L579 / BSM L935）：公司员工在本人私人手机上注册；本机 Keystore 生成的设备公钥与指纹随申请提交，服务端在登记时绑定本人。 */
+  private fun showStaffSelfRegistration() {
+    val name = editText("姓名")
+    val phone = editText("手机号")
+    val idDoc = editText("证件号码")
+    val merchant = editText("自营商户编号（公司总部员工留空）")
+    val company = android.widget.Spinner(this)
+    var companies: List<StaffRegistrationCompany> = emptyList()
+    val status = statusText("选择所属公司并提交资料；本机将生成并随申请绑定该手机专属的设备标识（Keystore 公钥/指纹）。公司审批通过后，用本机登录。")
+    val requestKey = java.util.UUID.randomUUID().toString()
+    val submit = button("提交本人注册申请") {
+      val selected = companies.getOrNull(company.selectedItemPosition)
+      if (selected == null || name.text.toString().trim().isEmpty()) { status.text = "请选择公司并填写姓名"; return@button }
+      val fingerprint = DeviceKeystore.deviceFingerprintHash()
+      val request = StaffSelfRegistrationRequest(requestKey, selected.id, name.text.toString().trim(),
+        DeviceKeystore.devicePublicKeyBase64(), fingerprint,
+        merchant.text.toString().trim().ifEmpty { null }, phone.text.toString().trim().ifEmpty { null }, idDoc.text.toString().trim().ifEmpty { null })
+      runBackground(
+        action = { api.submitStaffSelfRegistration(request) },
+        onSuccess = { receipt ->
+          // 绑定成功：本机以登记的指纹作为设备编号、按私人手机终端码声明身份并签名（U01-001 只声明一次）。
+          getSharedPreferences("private_phone", MODE_PRIVATE).edit().putString("device_id", fingerprint).apply()
+          api.deviceId = fingerprint
+          api.terminalType = STAFF_PRIVATE_PHONE
+          status.text = "申请已提交，本机设备标识已随申请绑定；等待所属公司审批，通过后用本机登录。申请编号：${receipt.id}"
+        },
+        onError = { status.text = "提交失败：${friendly(it)}" },
+      )
+    }
+    runBackground(
+      action = { api.listStaffRegistrationCompanies() },
+      onSuccess = { choices -> companies = choices; company.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, choices.map { it.name }) },
+      onError = { status.text = "公司列表读取失败：${friendly(it)}" },
+    )
+    setContentView(screen(title = "公司员工自助注册（私人手机）", children = listOf(status, company, name, phone, idDoc, merchant, submit, button("返回登录") { showLogin() })))
+  }
+
   private fun showStaffRegistration() {
     val name = editText("新员工姓名")
     val projectId = editText("所属项目编号")
     val merchantId = editText("本商户编号")
     val phone = editText("员工手机号")
     val idDoc = editText("员工证件号码")
-    val status = statusText("商户代员工提交资料，由对应域公司审批。自营商户员工请使用登录页的公司员工自助注册。")
+    val status = statusText("商户代员工提交资料，由对应域公司审批。自营商户员工请在本人私人手机安装本应用后，从登录页「公司员工自助注册（私人手机）」提交。")
     val requestKey = java.util.UUID.randomUUID().toString()
     val submitButton = button("提交商户员工注册申请") {
       val n = name.text.toString().trim()
diff --git a/apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt b/apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt
index 42e8c288..1e2a0f69 100644
--- a/apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt
+++ b/apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt
@@ -24,6 +24,9 @@ import kotlinx.serialization.json.decodeFromJsonElement
  * android.jar 不包含 java.net.http 包，故此处改用 [HttpURLConnection]。
  * 业务逻辑（TerminalWorkflow / TerminalModels / InMemoryLocalStore）保持复用，不重复实现。
  */
+/** U01-001/U01-007：员工私人手机通道的终端码（与 JVM 版 TerminalApiClient 常量同值；该文件不在 Android 源集内）。 */
+const val STAFF_PRIVATE_PHONE = "staff_private_phone"
+
 class ApiException(
   val statusCode: Int,
   val path: String,
@@ -104,6 +107,14 @@ class TerminalApiClient(
   fun submitStaffRegistration(request: StaffRegistrationSubmitRequest): StaffProfileRecord =
     post("/api/v1/staff/registrations", request)
 
+  // ── U01-007（PGM L579/BSM L935）：公司员工本人在私人手机 App 注册；两条公开路径只声明私人手机终端码，
+  //    设备公钥/指纹随请求体提交并在服务端绑定本人（无设备证明即 4xx）。未绑定前无设备编号，不做设备签名（公开路径）。 ──
+  fun listStaffRegistrationCompanies(): List<StaffRegistrationCompany> =
+    get("/api/v1/staff/registration-companies", STAFF_PRIVATE_PHONE)
+
+  fun submitStaffSelfRegistration(request: StaffSelfRegistrationRequest): StaffRegistrationReceipt =
+    post("/api/v1/staff/self-registrations", request, STAFF_PRIVATE_PHONE)
+
   // ── G批:服务点单(公司代收口径:收款码原文由服务端返回,App 重绘 QR) ──
 
   fun listServiceCatalog(): List<ServiceCatalogRow> = get<Page<ServiceCatalogRow>>("/api/v1/services/catalog").items
@@ -254,26 +265,26 @@ class TerminalApiClient(
   // 传输层（HttpURLConnection）
   // --------------------------------------------------------------------------
 
-  private inline fun <reified T> get(path: String): T {
-    val connection = open(path, "GET")
+  private inline fun <reified T> get(path: String, terminalTypeOverride: String? = null): T {
+    val connection = open(path, "GET", terminalTypeOverride)
     return send(path, connection, body = null)
   }
 
-  private inline fun <reified RequestBody, reified ResponseBody> post(path: String, body: RequestBody): ResponseBody {
-    val connection = open(path, "POST")
+  private inline fun <reified RequestBody, reified ResponseBody> post(path: String, body: RequestBody, terminalTypeOverride: String? = null): ResponseBody {
+    val connection = open(path, "POST", terminalTypeOverride)
     connection.setRequestProperty("content-type", "application/json")
     connection.doOutput = true
     return send(path, connection, json.encodeToString(body))
   }
 
   @PublishedApi
-  internal fun open(path: String, method: String): HttpURLConnection {
+  internal fun open(path: String, method: String, terminalTypeOverride: String? = null): HttpURLConnection {
     val connection = URL("$baseUrl$path").openConnection() as HttpURLConnection // nosemgrep: baseUrl is validated to https/loopback in init.
     connection.requestMethod = method
     connection.connectTimeout = connectTimeoutMs
     connection.readTimeout = readTimeoutMs
     // 3.19 二期：声明终端类型，接受 terminal.enabled 总开关逐请求后端强制（矩阵页关端=接口硬拒）。
-    terminalType?.let { connection.setRequestProperty("X-Terminal-Type", it) }
+    (terminalTypeOverride ?: terminalType)?.let { connection.setRequestProperty("X-Terminal-Type", it) }
     accessToken?.takeIf { it.isNotBlank() }?.let { connection.setRequestProperty("authorization", "Bearer $it") }
     return connection
   }
diff --git a/docs/r5-spec-T1-staff-tables-dev-design-20260618.md b/docs/r5-spec-T1-staff-tables-dev-design-20260618.md
index 04732ef6..4d56a568 100644
--- a/docs/r5-spec-T1-staff-tables-dev-design-20260618.md
+++ b/docs/r5-spec-T1-staff-tables-dev-design-20260618.md
@@ -4,7 +4,7 @@
 > **裁决依据**：R5 裁决⑨——员工三表 + Android 端凭证本期 P0；MDM 自有侧字段暂缓。
 > **关键边界**：本规格的 `staff_device_credentials`（员工私人手机 Keystore 公钥/不可逆指纹）与既有 `terminal_device_credentials`（V011，kiosk 一体机随机 token）是**两套完全不同实体**，互不复用、互不串值。
 > **工程约定**（对齐既有迁移）：`id VARCHAR(64)` 主键；`TIMESTAMPTZ ... DEFAULT now()`；CHECK 命名 `chk_<表>_<字段>`、唯一 `uq_<表>_<列>`；隔离列 `project_id VARCHAR(64) NOT NULL`、`merchant_id VARCHAR(64)`（NULL=平台员工）；姓名、手机号与证件使用既有 PiiCipher 加密存放；phone/id_doc 接收原值，日常 HTTP 响应强制脱敏，不从旧哈希恢复。
-> **迁移**：三表最初由 V066 建立；本次员工域迁移以 main 已应用的 V410 之后连续编号为 V411—V421（原稿 V400—V409 低于 V410，按 U10-001 重编号；V421 为寄存柜扫码凭证表）；测试用 `MigrationVersions.latest()` 动态读版本（R3 已去硬钉，无需改 12 处 JDBC 测试）。落地后把 `matrix-migration-claims.json` 中 `staff_profiles`/`staff_device_credentials`/`employee_notices` 由 absent 改 present。
+> **迁移**：三表最初由 V066 建立；本次员工域迁移以 main 已应用的 V410 之后连续编号为 V411—V422（原稿 V400—V409 低于 V410，按 U10-001 重编号；V421 为寄存柜扫码凭证表、V422 为顶班记录表）；测试用 `MigrationVersions.latest()` 动态读版本（R3 已去硬钉，无需改 12 处 JDBC 测试）。落地后把 `matrix-migration-claims.json` 中 `staff_profiles`/`staff_device_credentials`/`employee_notices` 由 absent 改 present。
 
 ## M010 当前实现（2026-09-08）
 
```
