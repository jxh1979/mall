CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U01 未发布材料第 1 段（安卓应用与设备密钥）B1 只读全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-09T18:22:28Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `71460a0130e255bf0b667ead41a523861cf4cb3d`；工作区 tree `28dcfec971f6eca651213368c27ec7e893a08305`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 对象：未发布提交 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb（树 e28dad059b2c7e08bcc95a8db18552d3f76edf4b，发布基线 242f4872）的只读输入副本第 1 段，3 文件 2217 行（DeviceKeystore.kt 56 / MainActivity.kt 1842 / TerminalApiClient.kt 319），main 主归属 M375；适用判据＝M375 映射行并集 50 项（GEN-001–018 + M375 的 BEQR/BSM/PGM 项）。步骤：B1（只读全量三线审计；材料不在 main，不做 B3）。
- 文件来源：上列路径均读自输入副本根目录 `.scratch/module-recheck-20260908/unpublished-03acdb98`（非 main）；行数/字节/SHA256 与清单 `unpublished-03acdb98-input.json` 逐文件核对；三份业务 HTML 取 main 当前正本；main 当前版本仅作分叉对照（只读）。
- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `U01-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：M001–M012 已闭环（M001–M010 已合并 PR #428，origin/main=bd445728；M011/M012 修复与台账已由 Grok 运维任务提交为 71460a01（特性分支 audit/20260909/M011-M012-checkpoint），PR/合并进行中；本报告与 Grok 的 U01 报告经 .git/info/exclude 临时排除，不进入该检查点）。未发布材料只读审查按 ledger 开工节口径：B1 双席 → B2 定稿登记（正式 ID U01-00N，处置写给发布者），不做 B3/B4/B5。
- 只读依赖追流（不审）：未发布副本内 `apps/android-terminal/src/main/kotlin/com/complex/terminal/DeviceRequestHeaders.kt`（第 2 段）、`backend/.../gate/DeviceCredentialVerifier.java`（第 7 段）、`backend/.../access/staff/StaffService.java`（第 6 段）、`V405__staff_work_phone_receipts.sql`（第 10 段）；main 当前同名文件。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/DeviceKeystore.kt | 56 | 2201 | b97b14ca6bd0e260b4afb9fc784e555c2c4f2622f79120d96303163b09ab86d2 | 全读 |
| apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt | 1842 | 83960 | 29e48ea029e79a6c6d8cf5d8ae07151c9e1ade93079b4e76542cbde57eccad97 | 全读 |
| apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt | 319 | 14492 | 45d886f2b0ca94230a27b883a3cae3d23c13f83ffe63f682c99e308e35d21e90 | 全读 |

## 2. 可执行证据原文（/tmp/u01_b1_run.log）

```
START=2026-09-09T18:21:48Z
HEAD=71460a0130e255bf0b667ead41a523861cf4cb3d
review_tree_type=tree
=== unpublished copy: lines/bytes/sha (must match manifest) ===
apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/DeviceKeystore.kt | 56 | 2201 | b97b14ca6bd0e260b4afb9fc784e555c2c4f2622f79120d96303163b09ab86d2
apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt | 1842 | 83960 | 29e48ea029e79a6c6d8cf5d8ae07151c9e1ade93079b4e76542cbde57eccad97
apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt | 319 | 14492 | 45d886f2b0ca94230a27b883a3cae3d23c13f83ffe63f682c99e308e35d21e90
=== manifest entries ===
apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/DeviceKeystore.kt 56 2201 b97b14ca6bd0e260b4afb9fc784e555c2c4f2622f79120d96303163b09ab86d2
apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt 1842 83960 29e48ea029e79a6c6d8cf5d8ae07151c9e1ade93079b4e76542cbde57eccad97
apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt 319 14492 45d886f2b0ca94230a27b883a3cae3d23c13f83ffe63f682c99e308e35d21e90
=== baseline 242f4872 -> unpublished tree e28dad05 (this segment) ===
 .../kotlin/com/complex/terminal/DeviceKeystore.kt  |  12 ++
 .../kotlin/com/complex/terminal/MainActivity.kt    | 200 +++++++++++++--------
 .../com/complex/terminal/TerminalApiClient.kt      |  20 ++-
 3 files changed, 155 insertions(+), 77 deletions(-)
=== main current vs unpublished copy (divergence) ===
apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/DeviceKeystore.kt: main_sha=df83580075b71c6bcd5ae891fd5b8f3d200ac5a5872c6cb2b0c32ea000c86777 unpublished_sha=b97b14ca6bd0e260b4afb9fc784e555c2c4f2622f79120d96303163b09ab86d2 diff_lines=12
apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt: main_sha=3ef9abfce027188f3639f3bceae8e665a955378e60a5ea77fae5a63d8a61ab71 unpublished_sha=29e48ea029e79a6c6d8cf5d8ae07151c9e1ade93079b4e76542cbde57eccad97 diff_lines=200
apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt: main_sha=8f2b539b0362196398bbccf28cfa7283556618beea540c2bca3d708d06bb77cb unpublished_sha=45d886f2b0ca94230a27b883a3cae3d23c13f83ffe63f682c99e308e35d21e90 diff_lines=20
=== main_vs_baseline: is main's copy unchanged since 242f4872 (blob ids) ===
apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/DeviceKeystore.kt baseline=d44ae47c41f7280773dc81b189ac0e8fe23e69e4 head=d44ae47c41f7280773dc81b189ac0e8fe23e69e4 unpublished=7a07c585ec11be612b46bfa891afb881f7020a83 main_unchanged_since_baseline=yes
apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt baseline=eb8b34e643f426a88b55c5f16882027255a14dac head=eb8b34e643f426a88b55c5f16882027255a14dac unpublished=b50c331823ab3229acb3663b351717ca9e8ef9c2 main_unchanged_since_baseline=yes
apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt baseline=58a040e4842b1dece76ce7eb7ad049045158adb8 head=58a040e4842b1dece76ce7eb7ad049045158adb8 unpublished=e3d0cc3fae7c9b6203599eb7fa508f09c74a818d main_unchanged_since_baseline=yes
=== probes: terminal-type switching / signing exclusions / unsigned image GET ===
277:    connection.setRequestProperty("X-Terminal-Type", if (path == "/api/v1/staff/registration-companies" || path == "/api/v1/staff/self-registrations") "app" else "staff_work_phone")
200:  fun catalogImageBytes(imageUrl: String): ByteArray {
202:    val connection = open(path, "GET")
259:    val connection = open(path, "GET")
288:        deviceRequestHeaders(id, connection.requestMethod, path, body ?: "", signer)
=== probes: charging receiver / punch semantics ===
101:      if (intent?.action == Intent.ACTION_POWER_DISCONNECTED) recordWorkPhoneReceipt("charging_disconnect")
105:  private fun recordWorkPhoneReceipt(method: String, tag: String? = null, type: String = "check_in", status: TextView? = null) {
114:        val text = "${if (type == "check_in") "上班领取" else "下班"}已记录：${row.punchedAt ?: "-"}"
129:    val filter = android.content.IntentFilter(Intent.ACTION_POWER_DISCONNECTED)
130:    if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(chargingReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
131:    else registerReceiver(chargingReceiver, filter)
157:    unregisterReceiver(chargingReceiver)
853:    val status = statusText("领取工作手机即记录上班；碰 NFC、扫码或取离充电设施，以最早一次为准。请保持工作手机应用运行。"
859:      recordWorkPhoneReceipt(method, tag, if (method == "nfc" || type.selectedItemPosition == 0) "check_in" else "check_out", status)
=== probes: PII screens / FLAG_SECURE / claims ===
203:    val status = statusText("请使用员工账号登录\nAPI: ${BuildConfig.COMPLEX_API_BASE_URL}")
953:      secure = true,
984:      secure = true,
1125:      secure = true,
1184:      secure = true,
1190:    val phone = editText("手机号")
1191:    val idDoc = editText("证件号码")
1220:    val phone = editText("员工手机号")
1221:    val idDoc = editText("员工证件号码")
1793:        status.text = "档案状态: $reg / ${staffStatusZh(p.status)}(手机、证件仅存不可逆加密，不可回显)"
=== probes: raw English status rendering (PGM-001) ===
394:            status.text = "核销结果：${r.verificationResult}\n记录：${r.id}\n${r.failureReason ?: ""}"
1558:    val wechatRadio = android.widget.RadioButton(this).apply { text = "微信 wechat"; id = View.generateViewId() }
1559:    val alipayRadio = android.widget.RadioButton(this).apply { text = "支付宝 alipay"; id = View.generateViewId() }
1563:    return group to { if (alipayRadio.isChecked) "alipay" else "wechat" }
1685:    else -> raw ?: "-"
1692:    else -> raw ?: "-"
1713:    else -> raw ?: "-"
1720:    else -> raw ?: "-"
1726:    else -> raw ?: ""
1732:    else -> raw ?: "-"
=== probes: idempotency keys ===
11
4
=== probes: DeviceKeystore ===
30:    val signature = java.security.Signature.getInstance("SHA256withECDSA").run {
45:    val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
49:        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
=== isolated compile in /tmp/u-overlay (baseline 242f4872 git archive + 76 unpublished files, 76/76 sha verified; SDK sdk.dir from main local.properties) ===
--- JVM terminal core: gradle -p apps/android-terminal --offline build (log /tmp/u02_gradle.log) ---
1:START=2026-09-09T18:06:47Z
21:BUILD SUCCESSFUL in 12s
23:GRADLE_EXIT=0
24:END=2026-09-09T18:07:00Z
TEST-com.complex.terminal.DeviceRequestHeadersTest.xml:tests="1" skipped="0" failures="0" errors="0"
TEST-com.complex.terminal.InMemoryLocalStoreTest.xml:tests="2" skipped="0" failures="0" errors="0"
TEST-com.complex.terminal.NdefTextTest.xml:tests="3" skipped="0" failures="0" errors="0"
TEST-com.complex.terminal.P0FixtureTest.xml:tests="2" skipped="0" failures="0" errors="0"
TEST-com.complex.terminal.TerminalFieldWorkflowTest.xml:tests="9" skipped="0" failures="0" errors="0"
TEST-com.complex.terminal.TerminalRequestSerializationTest.xml:tests="8" skipped="0" failures="0" errors="0"
--- android-app: gradle -p apps/android-terminal/android-app --offline :app:compileDebugKotlin (log /tmp/u01_gradle.log) ---
1:START=2026-09-09T18:07:28Z
34:BUILD SUCCESSFUL in 14s
36:GRADLE_EXIT=0
37:END=2026-09-09T18:07:43Z
=== HTML greps (main 正本): 工作手机/充电/领取/打卡/终端类型 ===
docs/business-system-map-for-operations-v0.1.html:464:        <div class="notice">一句话理解：官方 App 和小程序同步建设，门票主路径由客人通过官方 App/小程序或第三方平台自助购买�
docs/business-system-map-for-operations-v0.1.html:480:          <a href="#staff">酒吧平板/工作手机</a>
docs/business-system-map-for-operations-v0.1.html:552:              <span>进场之后，工作手机扫客人二维码核销各酒吧入场、赠饮、游乐等场内权益；酒吧平板绑定卡座后点酒下单。</
docs/business-system-map-for-operations-v0.1.html:611:              <div class="card-top"><h3>酒吧平板/工作手机</h3><span class="tag">现场</span></div>
docs/business-system-map-for-operations-v0.1.html:612:              <p>平板只做酒吧点酒下单；工作手机负责门票和权益扫码核销。</p>
docs/business-system-map-for-operations-v0.1.html:720:                <li>用户权限要知道员工注册信息、设备绑定、私人手机、公司工作手机、职级和审批状态。</li>
docs/business-system-map-for-operations-v0.1.html:764:                <li>和员工工作手机相连，现场员工用工作手机扫码核销。</li>
docs/business-system-map-for-operations-v0.1.html:879:                <li>连接员工工作手机核销，核销后客人能看到使用记录。</li>
docs/business-system-map-for-operations-v0.1.html:929:          <h2>酒吧平板/工作手机：平板点酒，工作手机核销</h2>
docs/business-system-map-for-operations-v0.1.html:930:          <p class="lead">平板、公司工作手机、员工私人手机 App 是不同设备和不同用途。平板只做酒吧点酒下单；公司工作手机用
docs/business-system-map-for-operations-v0.1.html:938:                <li>员工私人手机和公司发放的工作手机建立一一对应关系，进入办公区后私人手机需要寄存，寄存柜不固定，先�
docs/business-system-map-for-operations-v0.1.html:945:                <li>工作手机扫描客人 App/小程序或第三方票码二维码核销门票。</li>
docs/business-system-map-for-operations-v0.1.html:946:                <li>工作手机扫描权益二维码，核销赠饮、礼物或其他领取资格。</li>
docs/business-system-map-for-operations-v0.1.html:947:                <li>工作手机上的微信必须是企业微信，员工工作聊天每天自动上传到后台，永久保存、不自动到期删除；确需清�
docs/business-system-map-for-operations-v0.1.html:949:                <li>正式发放的受管工作手机不允许安装除预装软件之外的任何软件，只允许访问白名单网站；清单由后台维护，�
docs/business-system-map-for-operations-v0.1.html:967:                <li>连接票务权益，工作手机只负责门票和权益核销。</li>
docs/business-system-map-for-operations-v0.1.html:1047:                <li>连接员工工作手机和一体机，完成赠饮或权益领取核销与展示。</li>
docs/business-system-map-for-operations-v0.1.html:1131:                <li>点单员代客下单、向客人出示收款码和点完成履约都是现场操作，一律在公司工作手机上办理，不在自己的私�
docs/business-system-map-for-operations-v0.1.html:1181:                <li>连接机器人和工作手机，机器人到达是被动留痕，员工扫码交接是主动留痕。</li>
docs/business-system-map-for-operations-v0.1.html:1445:                <li>考勤打卡从领取工作手机开始算：员工到岗领到自己那台工作手机的时刻，就是上班打卡的时刻。领取可以用�
docs/business-system-map-for-operations-v0.1.html:1446:                <li>严格口径：晚于排班开始打卡就是迟到，早于排班结束打卡就是早退，不设宽限；以员工所在门店／点位的当�
docs/business-system-map-for-operations-v0.1.html:1447:                <li>考勤月历：每天标出勤、缺勤、迟到、早退、请假；没排班的日子是休息。工作手机损坏或不可用的，由夜班�
docs/business-system-map-for-operations-v0.1.html:1470:                <li>不建设独立营销打卡；营销活动通过任务、材料上传、结果确认和审批留痕，不生成考勤记录，也不用于出勤�
docs/business-system-map-for-operations-v0.1.html:1592:                <li>工作手机私装软件、访问非白名单网站、绕过企业微信沟通。</li>
docs/business-system-map-for-operations-v0.1.html:1599:                <li>工作手机、平板、一体机绑定点位后，系统自动知道“在哪”。核销、收款、退款、放行等敏感操作，操作设�
docs/business-system-map-for-operations-v0.1.html:1858:                <td>现场作业端/员工工作手机</td>
docs/business-system-map-for-operations-v0.1.html:1865:                <td>注册、提交资料审批、收公告通知、查自己的工单、帮客人订位、看排班、考勤打卡、请假、查薪资等可离场�
docs/business-system-map-for-operations-v0.1.html:1912:              <tr><th scope="row">工作手机连留痕</th><td>因为门票核销、赠饮领取、权益领取、企业微信沟通、补录、巡检、设备异�
docs/business-system-map-for-operations-v0.1.html:1913:              <tr><th scope="row">工作手机连设备管控</th><td>因为正式发放的受管工作手机只能安装预装软件，只能访问白名单网站�
docs/business-system-map-for-operations-v0.1.html:1978:            <div class="question confirmed">已拍板：工作手机预装软件清单和网站白名单由后台维护，通过企业级手机统一管控手段阻�
docs/platform-governance-map-for-operations-v0.1.html:328:              <span>工作手机、平板、一体机或扫码设备是否受管且状态正常。</span>
docs/platform-governance-map-for-operations-v0.1.html:363:              <p>管理员工注册、私人手机设备绑定、工作手机、企微和岗位状态。</p>
docs/platform-governance-map-for-operations-v0.1.html:373:              <p>控制客人 App、小程序、Web 前端、工作人员平板、员工工作手机、员工私人手机 App、商户侧 Web 门户、商户侧 A
docs/platform-governance-map-for-operations-v0.1.html:379:              <p>管理工作手机、平板、一体机和扫码设备的台账、领还、受管策略和状态。</p>
docs/platform-governance-map-for-operations-v0.1.html:572:          <h2>员工和手机：人、私人手机、工作手机要能对应起来</h2>
docs/platform-governance-map-for-operations-v0.1.html:573:          <p class="lead">员工不是只建账号。系统要知道员工是谁、审核是否通过、私人手机是否绑定、工作手机是否合规、上�
docs/platform-governance-map-for-operations-v0.1.html:586:              <h3>工作手机和企业微信</h3>
docs/platform-governance-map-for-operations-v0.1.html:588:                <li>工作手机必须使用企业微信进行工作沟通。</li>
docs/platform-governance-map-for-operations-v0.1.html:602:                <li>营销活动用任务、材料和审批留痕，不建设独立营销打卡，不混入考勤。</li>
docs/platform-governance-map-for-operations-v0.1.html:612:          <p class="lead">设备包括工作手机、平板、一体机、扫码设备、综合体入场三道环节设备（安检环节的 X 光设备、年龄�
=== cross-check (unpublished backend copy, read-only): verifier scope / V405 ===
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java:191:        String timestamp = trim(re
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java:193:        String signature = trim(re
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/config/SecurityConfig.java:77:                    authorize.requestMatche
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/config/SecurityConfig.java:79:                        authorize.requestMa
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/config/SecurityConfig.java:121:                "/api/v1/staff/registratio
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/java/com/complex/platform/config/SecurityConfig.java:122:                "/api/v1/staff/self-regist
-- 文件版本：V0.01
ALTER TABLE staff_attendance_records ALTER COLUMN method TYPE VARCHAR(24);
ALTER TABLE staff_attendance_records DROP CONSTRAINT chk_att_method;
-- 历史手动记录仍可读取；新增打卡由服务仅接受工作手机事件。
ALTER TABLE staff_attendance_records ADD CONSTRAINT chk_att_method
    CHECK (method IN ('nfc', 'qr', 'manual', 'charging_disconnect'));
END=2026-09-09T18:21:48Z
```

## 3. 三线审计方法与覆盖

- 对象口径：未发布提交 03acdb98 的只读输入副本第 1 段（3 文件 2217 行），不是 main；三份业务 HTML 取 main 正本；判据锁 V0.11 不变。本段文件的真实改动 = `git diff 242f4872 e28dad05 -- <本段路径>`（§2 diffstat），main 当前版本仅作分叉对照。
- 第一性原理线：从 HTML 原文反推「工作手机」这一端应当做什么、不应当做什么——BSM L930「平板、公司工作手机、员工私人手机 App 是不同设备和不同用途」、BSM L1445「考勤打卡从领取工作手机开始算…拿离充电设施这个动作本身也算领取…以最早发生的那一次为准」、PGM L578/BSM L1433「公司自有员工由本人在私人手机 App 注册…商户员工由商户代该员工提交注册申请」、PGM L739/BEQR L233 十一端、PGM L870–871「个人信息一律加密存放…只显示脱敏号码」；逐一对照三文件的行为与文案。
- 顺从式线：50 项判据逐项（§5）。
- 对抗式线：客户端自报终端身份能否绕过服务端闸（追到未发布副本 `DeviceCredentialVerifier` L138–186 与 main `TerminalMasterSwitchInterceptor` 读 `TerminalIdentityHolder`）；未签名 GET 是否被拒；充电事件在应用未运行时是否丢失；多次插拔是否重复计；PII 明文/截屏/文案是否与后端实际一致（`V403`、`JdbcStaffRepository` L892–893/L1200–1201）；JSON 命名（客户端 `JsonNamingStrategy.SnakeCase` ↔ 后端 `property-naming-strategy: SNAKE_CASE`）；请求/响应 DTO 字段（`StaffSelfRegistrationRequest`、`StaffAttendancePunchRequest`、`Receipt`）是否对得上。
- 只读依赖追流（不审、不计入本段结论）：副本内 `TerminalModels.kt`、`DeviceRequestHeaders.kt`（第 2 段）、`DeviceCredentialVerifier.java`、`SecurityConfig.java`（第 7 段）、`StaffService.java`、`StaffRegistrationService.java`、`StaffController.java`、`JdbcStaffRepository.java`（第 6 段）、`V403`/`V405`（第 10 段）；main 当前 `TerminalCanaryService`、`TerminalMasterSwitchInterceptor`、`TerminalSwitchGuard`。
- 3 文件全文逐行读完（§1）；与 main 的逐文件 diff 逐块读完（§2 diffstat）。可执行验证：在 `/tmp/u-overlay`（`git archive 242f4872` + 76 个未发布文件覆盖，76/76 哈希核对，不触主仓）内以 main 的 `local.properties` SDK 路径离线编译安卓应用 Kotlin 与 JVM 终端核心（§2）。本材料未发布，不做 B3；处置写给发布者。

## 4. 与发布基线 / main 的分叉对照

- 三文件在未发布树中均与 main 不同（§2 sha 对照）；改动全部是「新增功能」型：设备签名（`DeviceKeystore.sign`、`registerDeviceSigningKey`、`send()` 内加签）、工作手机领取（充电断开接收器、NFC 只记上班、扫码下班）、公司员工自助注册（登录页新按钮 + 两条公开路径）、商户代提交注册字段扩展（merchantId/phone/idDoc）、排班改为提交审批、设备编号改为管理员填写并持久化。
- M009–M012 未改动本段任何文件（安卓端不在其单元集），发布时不会覆盖 main 已修项；main 侧对本段三文件自基线以来是否有改动见 §2 `main_vs_baseline` 行（无改动=快进合并；有改动=需三方合并）。
- 与 main 已定稿项的关系：M375 尚未开审（PENDING），本段发现中「main 已存在」的项（§6 CX-U01-005、§7 OBS）明确标注，不算入本材料引入。

## 5. 50 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001 必要性 | 不通过 | 自助注册/商户代提交放在工作手机应用内并借用「app」身份——HTML 未把这两条路径给工作手机（CX-U01-001） |
| GEN-002 可执行正确性 | 不通过 | 充电断开领取只在 Activity 存活时可记（CX-U01-002）；app 强制模式下自助注册 POST 被硬拒（CX-U01-001 工程面） |
| GEN-003 测试有效性 | 不适用 | 本段无测试文件；安卓工程无单测（副本内 `android-app` 无 test 目录） |
| GEN-004 信任边界 | 不通过 | 终端身份由客户端按路径自选（`TerminalApiClient.kt:277`），公开路径不带设备身份即以「app」入闸（CX-U01-001） |
| GEN-005 授权 | 不通过 | 同上：自报「app」使未登记设备的写请求绕过设备绑定型 `credential_required`（副本 verifier L162–166 仅对设备绑定型强制） |
| GEN-006 数据完整性 | 通过 | 打卡以服务端最早一次为准（`StaffService` L879–883 `findFirstCheckIn`）；绑定后才写 prefs |
| GEN-007 并发幂等 | 通过（登记） | 新流程 UUID 幂等键；旧流程 `prefix_${currentTimeMillis}` 为 main 既有（OBS-4） |
| GEN-008 接口一致性 | 通过 | 客户端 SnakeCase ↔ 后端 SNAKE_CASE；`StaffSelfRegistrationRequest`/`AttendancePunchRequest`/`Receipt` 字段与副本 DTO 一致；`registerDeviceSigningKey` 显式 snake 键；签名规范串两端均不含 query |
| GEN-009 错误处理 | 通过 | 各写路径 onError 显示 `friendly()`；充电事件失败 Toast |
| GEN-010 安全 | 不通过 | PII 录入屏无 FLAG_SECURE（CX-U01-004）；「不可逆加密」文案与后端可解密事实不符（CX-U01-003）；身份自选（CX-U01-001） |
| GEN-011 审计证据 | 通过 | 打卡/注册均经后端幂等+审计（`recordPunch` 审计事件；副本 `StaffController` requireIdempotencyKey） |
| GEN-012 资源 | 通过（登记） | 每次拔线一次网络打卡 + Toast，服务端返回首条（OBS-2 噪声，不重复计） |
| GEN-013 构建 | 通过 | 隔离副本 `/tmp/u-overlay`（基线 242f4872 + 76 未发布文件）内 `gradle --offline :app:compileDebugKotlin` BUILD SUCCESSFUL、JVM 核心 `build` 含 6 个测试类 25 例全绿（§2） |
| GEN-014 迁移 | 不适用 | 本段无迁移（V405 在第 10 段） |
| GEN-015 界面 | 不通过 | 英文状态/渠道直出为 main 既有（CX-U01-005，归 M375）；本段新增文案全中文 |
| GEN-016 文档 | 不通过 | `MainActivity.kt:1793` 「仅存不可逆加密，不可回显」不可验证且与 V403/仓储解密事实相反（CX-U01-003） |
| GEN-017 分类 | 通过 | 本段四分类见 §6；未把业务发明伪装技术性；未把 HTML 沉默判冲突 |
| GEN-018 证据 | 通过 | 结论落到文件:行、HTML 行、副本后端行、§2 命令 |
| BEQR-150 终端十一端 | 不通过 | 员工自助注册以「app」（客人手机应用）身份进入，员工私人手机端未承载（CX-U01-001） |
| BEQR-153 终端开关优先级 | 不通过 | 客人 App 总开关会关掉员工自助注册，员工私人手机/工作手机开关反而管不到（CX-U01-001） |
| BSM-001 整体说明 | 通过 | 无实现面冲突 |
| BSM-002 一笔业务怎么走 | 通过 | 无实现面 |
| BSM-003 通用能力 | 通过 | 操作有引导（状态文案）；数据可改可删不在本段 |
| BSM-012 酒吧平板/工作手机 | 通过 | 工作手机核销/交接功能未改动 |
| BSM-013 触摸屏一体机 | 不适用 | 本段无一体机实现 |
| BSM-026 各类入口 | 通过 | 登录/绑定/首页入口未改动权限门 |
| BSM-029 和谁有关 | 通过 | 无实现面 |
| BSM-044 平板点酒/工作手机核销 | 不通过 | 「公司工作手机用于扫码核销…私人手机 App 是不同设备」（L930）——工作手机应用承载自助注册（CX-U01-001） |
| BSM-045 主要功能 | 通过 | 核销/交接未改 |
| BSM-046 平板图形点单 | 通过 | POS 未改 |
| BSM-047 和谁有关 | 通过 | 无实现面 |
| BSM-048 一体机 | 不适用 | 无实现面 |
| BSM-049 主要功能 | 不适用 | 无实现面 |
| BSM-050 和谁有关 | 不适用 | 无实现面 |
| BSM-114 各类入口 | 通过 | 未新增越权入口；跳过绑定后打卡被拒 |
| BSM-115 相互关系 | 通过 | 无实现面 |
| BSM-120 已确认规则 | 不通过 | L1433 注册路径（CX-U01-001）；L1445 拿离充电即领取（CX-U01-002 实现不全）；L1443 排班审批（文案一致，逻辑在第 9 段） |
| PGM-001 整体定位 | 不通过 | 零英文：main 既有直出（CX-U01-005，归 M375） |
| PGM-010 终端开关 | 不通过 | 同 BEQR-153 |
| PGM-032 员工和手机 | 不通过 | L578 私人手机 App 注册路径（CX-U01-001） |
| PGM-033 主要功能 | 不通过 | 同上 |
| PGM-034 工作手机和企业微信 | 不适用 | 无实现面 |
| PGM-035 提成发放/人员服务治理 | 不适用 | 无实现面 |
| PGM-036 设备管理 | 通过 | 设备编号改由管理员填写、绑定后登记签名公钥，符合「谁领了什么设备」 |
| PGM-037 主要功能 | 通过（登记） | 受管策略未体现（OBS-2 与 CX-U01-002 相关） |
| PGM-038 和现场业务的关系 | 通过 | 无冲突 |
| PGM-045 终端开关 | 不通过 | 同 BEQR-153 |
| PGM-046 能控制哪些端 | 不通过 | 同 BEQR-150 |
| PGM-047 怎么控制 | 不通过 | 端身份由客户端按路径改报（CX-U01-001） |
| PGM-066 已拍板规则 | 通过 | 无实现面冲突 |

## 6. 本次 Findings（临时 ID CX-U01-00N；正式 ID 由 B2 分配）

### CX-U01-001 · 冲突 · 发布前须修：是

- 位置：`TerminalApiClient.kt:277`（`X-Terminal-Type` 按路径改报「app」）、`:285–289`（两条公开路径与登录跳过设备签名）；`MainActivity.kt:226`（登录页「公司员工自助注册」）、`:1188–1214`（`showStaffSelfRegistration`）、`:311`/`:1216–1244`（工作手机首页「商户员工注册申请（商户代提交）」）。
- HTML 原句：PGM L578「员工注册分两条路径：公司自有员工由本人在私人手机 App 注册、提交资料后后台审批；商户员工由商户代该员工提交注册申请…」；BSM L1433 同义；BSM L930「平板、公司工作手机、员工私人手机 App 是不同设备和不同用途」；PGM L373/L739、BEQR L233 十一端中「员工工作手机」「员工私人手机」「客人手机应用」是三个不同的端。
- 判据：PGM-032/033、BSM-120、BSM-044、BEQR-150/153、PGM-045/046/047、GEN-004/005；2.3 依据类型：HTML 原句。
- 业务影响：（a）自助注册被放进公司发放的工作手机应用——未注册的新员工没有工作手机，HTML 指定的通道是本人私人手机 App；（b）请求自报「app」= 客人手机应用：后台矩阵页关掉客人 App 会把员工自助注册一起关掉，而关员工私人手机/工作手机端管不到它（副本 verifier L146–149 无 `X-Device-Id` 时以自报值为终端类型；main `TerminalMasterSwitchInterceptor` L69 读同一派生身份）；（c）app 模式一旦 enforce，`POST /api/v1/staff/self-registrations` 被硬拒 `app_requires_customer_proof`（副本 verifier L180–183），自助注册整体失效；（d）改报「app」的实际效果是让未登记设备的写请求绕开设备绑定型的 `credential_required`（L162–166）——终端身份由客户端按路径自选，信任边界被客户端决定。
- 最小反例：`grep -n "X-Terminal-Type" TerminalApiClient.kt` → 一行三态；对副本后端以 `X-Terminal-Type: app`、无设备头 POST self-registrations：app=enforce → 403；客人 App 开关关闭 → 拒；改报 `staff_work_phone` → enforce 下 `credential_required`。
- 工程缺陷：是（终端身份自报可变、公开写路径身份错配）。建议登记：是。
- 最小处置（给发布者）：自助注册按 HTML 归到员工私人手机 App 通道，以该端登记的终端身份（main `TerminalCanaryService` 列出的 `staff_personal_app`/`staff_private_phone`）声明一次，不按路径改报；工作手机应用去掉登录页自助注册入口；商户代提交是否可经工作手机由 HTML 沉默处，登记不发明（见 OBS-8）。不在本轮修复。

### CX-U01-002 · 技术性 · 发布前须修：是

- 位置：`MainActivity.kt:99–103`（`chargingReceiver`）、`:128–131`（`onCreate` 动态注册）、`:157`（`onDestroy` 注销）、`:853`（文案「请保持工作手机应用运行」）、`:105–125`（`recordWorkPhoneReceipt`）。
- HTML 原句：BSM L1445「考勤打卡从领取工作手机开始算…工作手机被拿离充电设施这个动作本身也算领取。三种方式记的是同一个上班时刻，以最早发生的那一次为准，不重复计。」
- 判据：BSM-120、GEN-002、PGM-037；依据类型：HTML 原句。
- 业务影响：规则说「动作本身」算领取；实现只在应用进程存活且 Activity 未销毁时才收到 `ACTION_POWER_DISCONNECTED`。工作手机整夜充电、应用被系统回收或未启动时，拔线不被记录；随后 NFC/扫码的时间成为「最早一次」，员工被判迟到——首轮正常使用即会发生。服务端「最早一次」去重本身正确（`StaffService` L879–883）。
- 最小反例：应用未启动 → 拔线 → 无请求（接收器仅在 `onCreate` 注册，无 manifest 接收器/前台服务/开机自启）。
- 无业务发明理由：规则与三种方式均为 HTML 原句，缺的是「事件在任何时刻都能被捕获」的执行保障。
- 工程缺陷：是。建议登记：是。
- 最小处置（给发布者）：以前台服务或受管设备策略（PGM-036/037 工作手机受管）保证接收器常驻并在启动/开机后重注册；文案提示不能替代。不在本轮修复。

### CX-U01-003 · 技术性 · 发布前须修：是

- 位置：`MainActivity.kt:1793`「档案状态: … (手机、证件仅存不可逆加密，不可回显)」。
- 事实：副本 `V403__staff_profile_encrypted_personal_fields.sql` 注释「接收可解密原值」；`JdbcStaffRepository.java:892–893` `piiCipher.encrypt(...)`、`:1200–1201` `piiCipher.decrypt(...)` 读取时解密——可逆、可回显。HTML PGM L870「一律加密存放」、L871「员工日常界面只显示脱敏号码，不见明文」。
- 判据：GEN-016、GEN-010；依据类型：HTML 原句 + 代码事实。
- 业务影响：向员工陈述了系统并不具备的隐私属性（不可逆）；与 HTML「加密存放 + 脱敏显示」口径不一致。
- 无业务发明理由：只改文案使之与 HTML/实现一致，不新增规则。
- 工程缺陷：是（文档可验证性）。建议登记：是。最小处置：文案改为「加密存放、界面脱敏」。不在本轮修复。

### CX-U01-004 · 技术性 · 发布前须修：否（登记）

- 位置：`MainActivity.kt:1188–1214`、`:1216–1244` 两个采集手机号/证件号码的屏未设 `secure = true`；`:953/:984/:1125/:1184` 薪资/考勤/服务分账屏已设；`:1585–1588` 注释自述「敏感屏设 FLAG_SECURE」。
- 判据：GEN-010；依据类型：技术性（HTML 无截屏规则，PGM L870–871 仅要求存储加密与界面脱敏）。
- 业务影响：证件号码录入屏可被截屏/最近任务缩略图留存；与应用自身敏感屏规则不一致。
- 最小处置：两屏 `secure = true`。不在本轮修复。

### CX-U01-005 · 冲突 · 发布前须修：否（main 既有，归 M375）

- 位置（副本）：`MainActivity.kt:1558`「微信 wechat」/「支付宝 alipay」；`:203`「API: <url>」；`:1685/:1692/:1713/:1720/:1726/:1732` 中文映射兜底直出英文码；首页汇总/酒吧订单/支付意向/游乐核验直出 `status` 原码（行号见 §2 PGM-001 探针）。
- HTML 原句：PGM L287「全局原则 · 界面全中文（所有终端零英文）」、L965「已拍板：界面全中文（所有终端零英文）——后台/客人端/现场设备/安卓端展示给用户的一切文字」。main 同位置存在同样文本（main `MainActivity.kt:1511–1516` 等），非本材料引入。
- 判据：PGM-001、GEN-015；依据类型：HTML 原句。
- 处置：登记到 M375（main 审计）处理；本材料发布不加重也不解决。

## 7. 观察（登记、不入队）

- OBS-1：登录后若 prefs 有 `device_id` 直接进首页（`MainActivity.kt:216`），设备编号来自本机 prefs；enforce 下服务端按登记公钥验签，伪造编号过不了闸；grace 下任何编号可写并留审计。绑定成功后才写 prefs、`registerDeviceSigningKey` 在绑定之后调用（`:262`），若公钥登记失败则绑定已成、prefs 未写，下次重绑（服务端幂等性属第 7 段）。
- OBS-2：每次拔线都发一次打卡并 Toast「上班领取已记录：<首条时间>」（服务端返回首条，不重复计）；仅噪声。
- OBS-3：`catalogImageBytes()` 绕过 `send()` 不加签；副本 verifier 对设备绑定型的无凭证 GET 在 enforce 下也放行（L162–171 仅拒写），不破功能；签名规范串两端均不含 query（`DeviceRequestHeaders.kt:22` `substringBefore('?')` ↔ 服务端 `getRequestURI()`），query 参数不在签名覆盖内。
- OBS-4：旧流程幂等键仍为 `prefix_${System.currentTimeMillis()}`（main 既有，M375）。
- OBS-5：POS 绑定仍自动生成 `BARPOS-<millis>` 设备编号（`:1270`，main 既有），与工作手机改为管理员填写编号的口径不一致（M375/M376）。
- OBS-6：`ApiException.message` 内嵌完整响应体经 `friendly()` 显示（main 既有）。
- OBS-7：`AttendancePunchRequest` 不再发 `deviceFingerprint`；服务端改用已验签终端身份（`StaffService` L856–866 要求 `verified && staff_work_phone`），因此 staff_work_phone 模式为 OFF 或无签名时打卡一律 `WORK_PHONE_REQUIRED`——与 HTML「用本人工作手机领取」一致，属配置前提，登记。
- OBS-8：商户代提交注册放在工作手机首页（`:311`），HTML 只说「由商户代该员工提交」未指定端；本席不判冲突，随 CX-U01-001 一并交发布者定位通道。
- OBS-9：排班屏文案「修改仅限原发起人在班次开始前申请」与 HTML L1443「当事员工可以申请修改…由作出该排班表的负责人填写原因后修改」措辞不同（发起修改的主体），逻辑在第 9 段核。
- OBS-10：`DeviceKeystore` EC P-256 密钥未绑定用户认证（无 `setUserAuthenticationRequired`），为设备身份而非人身份，符合「设备绑定」用途。
- 技能：`using-superpowers`、`verification-before-completion`（所有事实附文件:行或 §2 命令）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**FAIL**。3 文件全文三线审计、50 项判据逐项核对；发现 1 项冲突须修（CX-U01-001）、2 项技术性须修（CX-U01-002/003）、1 项技术性登记（CX-U01-004）、1 项 main 既有冲突归 M375（CX-U01-005）；无添加项；无 HTML 修改申请；未修源、未提交/推送。本报告落盘前未读取 `U01-b1-grok-raw.md` 任何正文。


---

# U01 B2-r1 席甲票（追加段）
版本：V0.02（本文件自身；B1 段原字节不变）

- 输入：findings V1.44「当前 U01 B2-r1 整批候选」U01-001…015；modules/U01-b1-grok-raw.md V0.01（49269bytes SHAbe1bcf2148ad47d1540b37f7a70554b24f3e23012332248a0a62ee25a09877f0）；本文件 B1 段（39022bytes SHAe4674e7c4bd1a202fb7bfdb17f90476294ee0ddfa8de4b9db830e133783885f2）。2026-09-09T18:43:27Z
- 席甲 B2 核实命令与结果见 ledger V2.44 U01 节（main/副本 grep 同值、blob 对照、HTML L1445 原句、verifier/TerminalDeviceService/ServiceOrderController 行号）。

## 逐 ID 票

| ID | 票 | 依据 |
|---|---|---|
| 001 | PASS | 席甲 B1 与 Grok GR-002 同根（终端身份由客户端决定）；HTML L578/L1433/L930；处置只回到「按登记设备类型声明一次」，不发明新端 |
| 002 | PASS | 规则为 BSM L1445 原句，非添加；工程面成立（接收器随 Activity 存活） |
| 003 | PASS | V403 注释与 `JdbcStaffRepository` 解密事实；文案改与 PGM L870–871 一致 |
| 004 | PASS | 登记「否」：HTML 无截屏规则，属应用内一致性 |
| 005 | PASS | main 同位置同文本，材料未加重；归 M375 |
| 006 | PASS | verifier `DEVICE_BOUND_TYPES` 含 tablet；材料引入签名却漏 POS 路径 |
| 007 | PASS | HTML L579/L935 安装时绑定设备标识；材料新增自助注册未实现 |
| 008 | PASS | android-app 零测试；材料新增关键分支无可失败测试 |
| 009 | PASS | blob 对照与依赖清单（U02/U06/U10、V210 CHECK、V410 编号） |
| 010 | PASS | main 既有（showHome 23 按钮同）；归 M375 |
| 011 | PASS | main 既有（`"bar_pos"` 2 处、`TERMINAL_TYPES` 闭集同）；归 M375/M064 |
| 012 | PASS | main 既有（0 处调用）；归 M375 |
| 013 | PASS | main 既有（0 处调用）；归 M375 |
| 014 | PASS | main 既有（POS 结账不在 diff）；归 M375 |
| 015 | PASS | 观察归并未改写双方原意 |

## 勘误（接受 Grok B2-r1 非阻断意见，定稿时以此为准）

- ledger V2.44 U01 节写「`"bar_pos"` main 2 处」，实为 3 处（`:48` prefs 文件名、`:1218` 注释、`:1236` 赋值）；缺陷本体是赋值行，结论不变。
- 候选 U01-001 来源行「GR-U01-001 身份面」应读作 GR-U01-002（身份自报的根）；GR-001 本体在 U01-010。

## 结论

席甲 B2-r1 票：**PASS**（候选 U01-001…015 成立；发布前须修「是」7 / 「否」8；添加 0；questions 0）。本票为作者自审票，以 Grok B2-r1 票合取；Grok FAIL 具否决权。本段落盘前已读 Grok B2-r1 追加段（V0.02，全文 71539bytes SHA09f33cf7f94ff937531b390b40e56d4222c5fabc72e119cd6d48cc4814e31eda，前 49269bytes 原字节不变）。
