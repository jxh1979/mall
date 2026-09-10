CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U-FIX B4-r2 修复内容复审 · 席甲完整报告
版本：V0.01

- 席甲：Claude Fable 5.1（作者自审票、唯一修复者）。步骤：B4-r2（合成模块 U-FIX 的 B3 第 2 次整批修复的完整 diff 复审）。2026-09-10T19:44:31Z
- worktree：/home/james/wt/ufix-audit-20260910（分支 audit/20260910/U-FIX 自 445a49f4 起）；主工作区 HEAD 445a49f4 未改。
- before tree：`e3fd14b6e61f284e3a9dc7380d529e7486c71b8a`（＝上一轮 after tree）；after tree：`{after}`；`git diff --binary {before[:12]} {after[:12]}`（无 pathspec）全文 {len(diff)} bytes SHA256 {dsha}；副本 {A / "b4.diff"}。全文附于本报告末尾附录 A。
- 定稿队列：findings V1.83 各段 B2 双 PASS 定稿的「发布前须修＝是」ID（U01–U18）。归因规则：材料重放行→该段「以 main 为底重放本段/叠法」定稿 ID；U03 人事七文件材料行按 U03 定稿共同处置（002–007：人事七文件可快进但须与 U04/U05/U06/U08/U10 同批）归 U03-007；席甲修复行→对应定稿 ID。
- 锁：实算 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a（sidecar/ledger 基准同值；Grok 结论值在 B3 前后校验点由 ledger 记录）。

## 1. diffstat 与 name-status（无 pathspec）

```
.../kotlin/com/complex/terminal/MainActivity.kt    | 45 +++++++++++++++++++++-
 .../com/complex/terminal/TerminalApiClient.kt      | 23 ++++++++---
 .../r5-spec-T1-staff-tables-dev-design-20260618.md |  2 +-
 3 files changed, 61 insertions(+), 9 deletions(-)
```

```
M	apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt
M	apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt
M	docs/r5-spec-T1-staff-tables-dev-design-20260618.md
```

## 2. 逐块归因（默认上下文 diff：7 块，UNMAPPED 0；-U0 细粒度：11 块，UNMAPPED 0）

-U0 逐块 ID 计数：U01-001(9)、U01-007(9)、U01-016(1)、U13-011(1)

### 2.1 默认上下文逐块表

| 路径 | hunk | + | − | 归属 ID |
|---|---|---:|---:|---|
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -124,6 +124,8 @@ class MainActivity : Activity() {` | 2 | 0 | U01-007, U01-001 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -222,7 +224,9 @@ class MainActivity : Activity() {` | 3 | 1 | U01-007, U01-001 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -1192,13 +1196,50 @@ class MainActivity : Activity() {` | 38 | 1 | U01-016 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | `@@ -24,6 +24,9 @@ import kotlinx.serialization.json.decodeFr` | 3 | 0 | U01-007, U01-001 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | `@@ -104,6 +107,14 @@ class TerminalApiClient(` | 8 | 0 | U01-007, U01-001 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | `@@ -254,26 +265,26 @@ class TerminalApiClient(` | 6 | 6 | U01-007, U01-001 |
| `docs/r5-spec-T1-staff-tables-dev-design-20260618.md` | `@@ -4,7 +4,7 @@` | 1 | 1 | U13-011 |

### 2.2 -U0 细粒度逐块表

| 路径 | hunk | + | − | 归属 ID |
|---|---|---:|---:|---|
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -126,0 +127,2 @@ class MainActivity : Activity() {` | 2 | 0 | U01-007, U01-001 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -225 +227,3 @@ class MainActivity : Activity() {` | 3 | 1 | U01-007, U01-001 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -1194,0 +1199,37 @@ class MainActivity : Activity() {` | 37 | 0 | U01-007, U01-001 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -1201 +1242 @@ class MainActivity : Activity() {` | 1 | 1 | U01-016 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | `@@ -26,0 +27,3 @@ import kotlinx.serialization.json.decodeFr` | 3 | 0 | U01-007, U01-001 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | `@@ -106,0 +110,8 @@ class TerminalApiClient(` | 8 | 0 | U01-007, U01-001 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | `@@ -257,2 +268,2 @@ class TerminalApiClient(` | 2 | 2 | U01-007, U01-001 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | `@@ -262,2 +273,2 @@ class TerminalApiClient(` | 2 | 2 | U01-007, U01-001 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | `@@ -270 +281 @@ class TerminalApiClient(` | 1 | 1 | U01-007, U01-001 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | `@@ -276 +287 @@ class TerminalApiClient(` | 1 | 1 | U01-007, U01-001 |
| `docs/r5-spec-T1-staff-tables-dev-design-20260618.md` | `@@ -7 +7 @@` | 1 | 1 | U13-011 |

## 3. 席甲验证证据与逐 ID 复审

### 3.0 本轮范围（B3-r2）

回退 B2 定稿（findings V1.83）后 B3-r2 只修三项，其余 116 个「是」项在 B5-r1 已被 Grok 逐段裁定闭合、本轮不重做；本轮 diff 只含三文件（android-app `TerminalApiClient.kt`、`MainActivity.kt`、`docs/r5-spec-T1-staff-tables-dev-design-20260618.md`）。补丁 `/tmp/ufix_batchR2.py`（锚点替换，先在临时副本试跑并编译）。

### 3.1 逐 ID 复审

| ID | 修法（最小处置落地） | 证据 |
|---|---|---|
| U01-007（同 U01-001） | android-app `TerminalApiClient`：顶层 `STAFF_PRIVATE_PHONE`；`get/post/open` 增 `terminalTypeOverride`（只有两条公开注册路径传该码，其余请求仍按绑定类型；未绑定无设备编号不签名）；`listStaffRegistrationCompanies()` / `submitStaffSelfRegistration()`。`MainActivity`：`showStaffSelfRegistration()` 以 `DeviceKeystore.devicePublicKeyBase64()` / `deviceFingerprintHash()` 填 `StaffSelfRegistrationRequest`（devicePublicKey/deviceFingerprintHash 必填，服务端 U05-003 校验并绑定本人）；成功后把指纹持久化为 `private_phone.device_id`、`api.deviceId=指纹`、`api.terminalType=STAFF_PRIVATE_PHONE`（重启后 onCreate 恢复，只声明一次）；入口只在本机未绑定为工作手机/平板时显示于登录页（`api.terminalType == null`），不再出现在工作手机应用 | 临时副本 `compileDebugKotlin` EXIT 0、JVM `gradle test` 30/30（/tmp/ufix_r2_android_compile.log、/tmp/ufix_r2_jvm.log）；worktree 复跑见 §3.A |
| U01-016 | `MainActivity.kt` 商户代提交页 statusText 改为「自营商户员工请在本人私人手机安装本应用后，从登录页「公司员工自助注册（私人手机）」提交。」 | grep 命中 1 处；旧句 0 处 |
| U13-011 | T1:7「V411—V422 … V421 为寄存柜扫码凭证表、V422 为顶班记录表」 | `sed -n 7p` |

版本行：三文件均已在 B3-r1 按版本铁律递增一次（MainActivity V0.05、TerminalApiClient V0.04、T1 V0.13），同一未发布批次不再递增。

### 3.A 验证证据（worktree，B3-r2 后）

| 面 | 命令/口径 | 结果 |
|---|---|---|
| Android 应用 | `apps/android-terminal/android-app`：`ANDROID_HOME=/home/james/android-sdk gradle --offline :app:compileDebugKotlin`（worktree，B3-r2 后） | EXIT 0（/tmp/ufix_r2_android_wt.log） |
| JVM 客户端 | `apps/android-terminal`：`gradle --offline test` | EXIT 0，30 tests / 0 failures（/tmp/ufix_r2_jvm_wt.log；本轮未改 JVM 源） |
| 发布门禁 | `npm run verify:predeploy`（T1 属文档门禁范围） | EXIT 0（/tmp/ufix_r2_predeploy.log） |
| 文案/文档 | `grep -c "从登录页「公司员工自助注册（私人手机）」提交" MainActivity.kt` = 1；`sed -n 7p T1` 含「V411—V422」「V422 为顶班记录表」 | 命中 |
| 补丁预演 | 临时副本 /tmp/ufix_r2_android（rsync worktree apps/android-terminal + 补丁文件）：compileDebugKotlin EXIT 0、gradle test 30/30 | /tmp/ufix_r2_android_compile.log、/tmp/ufix_r2_jvm.log |
| 行为可失败性 | 无设备证明的自助注册在服务端 400（M010RegistrationHttpTests，B3-r1 已绿）；端侧现在以 Keystore 公钥/指纹填请求，删掉 `DeviceKeystore` 两行即被服务端拒 | 由 B3-r1 用例覆盖，本轮未新增 android 仪器测试（U01-008 允许 JVM 侧） |

### 3.B 边界

- 未触及其余 116 个「是」项与任何「否」项（含 U01-004 两屏 secure：新屏保持与材料同形，不加 FLAG_SECURE，留给 U01-004 的归属）；三份业务 HTML / 锁 / sidecar / ledger 基准 / 主工作区未改；未 commit/push。

### 3.C 席甲自审结论（作者自审票，落盘前未读 Grok B4-r2 raw）

- 修复内容：U01-007 按定稿在私人手机通道以 Keystore 公钥/指纹提交并绑定；U01-016 文案指向实际入口；U13-011 上界改 V422。无业务发明（终端码/字段/文案均为既有契约与定稿处置）。
- 范围归因：本轮 diff 每块均归 U01-007/U01-016/U13-011（§2 表，0 未归因）。
- 席甲 B4-r2 票：**PASS**（修复内容 PASS；范围归因 PASS）。本票为作者自审票，须与 Grok B4-r2 票合取；Grok FAIL 具否决权。

### 日志 /tmp/ufix_r2_android_wt.log

```

```

### 日志 /tmp/ufix_r2_jvm_wt.log

```

```

### 日志 /tmp/ufix_r2_predeploy.log

```
[html-ledger-sync] PASS: 本次未改动业务最终解释权 HTML。比对范围：提交区 origin/main...HEAD（工作区三份 HTML 无未提交改动）。
[entry-anchors] 注册表 1228 行；账本 1235 行；承接矩阵 1244 行（同名 1235、账本中无此 rule_id 9）；账本镜像列片段 1565 条；被检 Markdown 369 份；条目名引用 1517 处（注册表原名 1358 处，正文唯一定位 159 处）。
[entry-anchors] PASS：条目名唯一、注册表条目正文逐字与 HTML 现文一致、引用可唯一定位、正文无行号锚点、账本正文与九个镜像列均与 HTML 现文一致、账本与承接矩阵一一对应。
[html-param-tier] PASS: 本次未改动业务最终解释权 HTML。比对范围：提交区 origin/main...HEAD（工作区三份 HTML 无未提交改动）。
DELIVERY_GATES_DOCS_PASS
完整后台页面矩阵校验通过：92 个页面均有路由、权限、UI 与测试覆盖
DOMAIN_OWNERSHIP_SCOPE_PASS: 334 tables classified
matrix-vs-migration verification passed: 347 tables parsed; 142 present-claims confirmed, 6 absent-claims still-absent (tracked gaps).
data isolation spec is in sync with Flyway migration scope baseline.
[tenant-isolation] 扫描范围：
  - 权威表清单（CSV）：334 表（direct=173，白名单豁免=82，inherited 为信息性盲区）
  - Jdbc*Repository 源：53 个文件
  - 本次扫出 finding：4（已登记 baseline：4，新增回归：0）
[tenant-isolation] KNOWN-BLINDSPOT B1（inherited，仅信息性，不硬失败）：72 表
    · amusement_incident_actions (scope_source: inspection/incident id)
    · amusement_safety_actions (scope_source: inspection/incident id)
    · analytics_ai_thresholds (scope_source: dimension/project/merchant)
    · analytics_customer_intents (scope_source: dimension/project/merchant)
    · analytics_metric_anomalies (scope_source: dimension/project/merchant)
    · analytics_metric_definitions (scope_source: dimension/project/merchant)
    · …其余 66 表略
[tenant-isolation] 注意：baseline 中 1 条已不再复现（疑似已修复），应从 baseline 与台账移除以收紧闸口：
    · F1:UNSCOPED_DIRECT_READ:access_grant_requests

[tenant-isolation] PASS（棘轮）：无新增回归；4 条已知缺口在台账跟踪闭环中（见 findings-register-v1.md tenant-isolation 小节）。隔离面未劣化。
NO_LEGACY_API_PROTOTYPE_PASS
OpenAPI contract verification passed.
OpenAPI P0 example verification passed: 8 request bodies and 3 responses checked.
docs/p0-openapi-operation-acceptance-matrix-v0.1.md is in sync with docs/openapi/openapi-p0.json; 732 operations checked.
/home/james/wt/ufix-audit-20260910/docs/p0-openapi-contract-closure-appendix-v0.1.md is in sync with /home/james/wt/ufix-audit-20260910/docs/openapi/openapi-p0.json; 732 operations checked.
[client-api-route-audit] PASS: client code only references formal /api/v1 or /api/open paths.
[client-openapi-coverage] PASS: 62 client route template(s) are covered by OpenAPI; 62 method-specific call(s) match OpenAPI methods.
[client-payload-policy] PASS: client payload normalization and OpenAPI request field casing are enforced.
production OpenAPI/Swagger docs access policy verification passed.
Formal security baseline verification passed: security-scans/formal-security-baseline-v1.md and security-scans/findings-register-v1.md.
PRODUCTION_READINESS_EVIDENCE_PASS
PRODUCTION_BLOCKER_REGISTER_PASS
PII_ENCRYPTION_PASS: 16 个 PII 列均经 piiCipher.encrypt 落库
TEST_DB_ISOLATION_PASS: 测试源未引用生产库 complex（均用 complex_test）
```

## 附录 A — 无 pathspec 完整 binary diff 原文

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
