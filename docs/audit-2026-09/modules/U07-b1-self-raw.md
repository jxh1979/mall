CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U07 未发布材料第 7 段（设备与接口访问联动）B1 只读全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-09T22:10:19Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `445a49f409f0cfbe6dd616ae079be1299824c48f`；工作区 tree `c50f6bcdad512c635dbefd545828564340668cde`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 对象：未发布提交 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb（树 e28dad059b2c7e08bcc95a8db18552d3f76edf4b，发布基线 242f4872）的只读输入副本第 7 段，3 文件 1585 行（SecurityConfig.java 146 / DeviceCredentialVerifier.java 371 / TerminalDeviceService.java 1068；相对基线仅 +11/−8），main 主归属 M396、M063、M064；适用判据＝映射行并集。步骤：B1（只读全量三线审计；材料不在 main，不做 B3）。
- 文件来源：上列路径均读自输入副本根目录 `.scratch/module-recheck-20260908/unpublished-03acdb98`（非 main）；行数/字节/SHA256 与清单核对；三份业务 HTML 取 main 当前正本；main 当前版本仅作分叉对照（只读）。隔离验证在 `/tmp/u-overlay/backend`（基线 + 76 未发布文件；`mvn -o compile` 与 `DeviceCredentialVerifierTests` 实跑）内进行，不触主仓、不写任何 complex_test* 库。
- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `U07-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：M001–M012 已闭环并合并（PR #428/#429，HEAD=445a49f4）；U01–U06 已按 B1→B2 定稿口径处理（见 ledger）。未发布材料只读审查：B1 双席 → B2 定稿登记（正式 ID U07-00N，处置写给发布者），不做 B3/B4/B5。
- 只读依赖追流（不审）：副本内 `StaffService.punch`（第 6 段）、`StaffRegistrationService`（第 5 段）、`DeviceCredentialVerifierTests`（第 12 段）、`M010RegressionTests`（第 11 段）、安卓客户端签名与公钥登记调用（第 1/2 段）；main `TerminalDeviceController`、`dto/DeviceBindRequest`/`DeviceReturnRequest`/`DeviceLifecycleRequest`、`TerminalDevicesPage.tsx`、`V322`。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/java/com/complex/platform/config/SecurityConfig.java | 146 | 8150 | 85a40911482a1388db5fe3ef55df7a80d7ddcd0b8a37d839752e4a605acb3421 | 全读 |
| backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java | 371 | 17363 | 6361fa97f4ddbcc3552f3c3ac5e2ed144ad9a5982e0bd7914ca0b4a18e03dc4f | 全读 |
| backend/src/main/java/com/complex/platform/terminal/TerminalDeviceService.java | 1068 | 47901 | c2aa2681909b6e45b0b8e5449dff577651cc395047ff663b12e632e2c5fa0168 | 全读 |

## 2. 可执行证据原文（/tmp/u07_b1_run.log）

```
START=2026-09-09T20:30:09Z
HEAD=445a49f409f0cfbe6dd616ae079be1299824c48f
review_tree_type=tree
=== unpublished copy: lines/bytes/sha (must match manifest) ===
backend/src/main/java/com/complex/platform/config/SecurityConfig.java | 146 | 8150 | 85a40911482a1388db5fe3ef55df7a80d7ddcd0b8a37d839752e4a605acb3421
backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java | 371 | 17363 | 6361fa97f4ddbcc3552f3c3ac5e2ed144ad9a5982e0bd7914ca0b4a18e03dc4f
backend/src/main/java/com/complex/platform/terminal/TerminalDeviceService.java | 1068 | 47901 | c2aa2681909b6e45b0b8e5449dff577651cc395047ff663b12e632e2c5fa0168
=== manifest entries ===
backend/src/main/java/com/complex/platform/config/SecurityConfig.java 146 8150 85a40911482a1388db5fe3ef55df7a80d7ddcd0b8a37d839752e4a605acb3421
backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java 371 17363 6361fa97f4ddbcc3552f3c3ac5e2ed144ad9a5982e0bd7914ca0b4a18e03dc4f
backend/src/main/java/com/complex/platform/terminal/TerminalDeviceService.java 1068 47901 c2aa2681909b6e45b0b8e5449dff577651cc395047ff663b12e632e2c5fa0168
=== baseline 242f4872 -> unpublished tree e28dad05 (full diff, this segment) ===
diff --git a/backend/src/main/java/com/complex/platform/config/SecurityConfig.java b/backend/src/main/java/com/complex/platform/config/SecurityConfig.java
index bd0b4b24..47f8dfa2 100644
--- a/backend/src/main/java/com/complex/platform/config/SecurityConfig.java
+++ b/backend/src/main/java/com/complex/platform/config/SecurityConfig.java
@@ -1,5 +1,7 @@
 package com.complex.platform.config;
 
+// 文件版本：V0.01
+
 import com.complex.platform.auth.BearerAuthenticationFilter;
 import com.complex.platform.auth.CustomerTokenAuthenticationFilter;
 import com.complex.platform.auth.DoubleSubmitCsrfFilter;
@@ -116,6 +118,8 @@ public class SecurityConfig {
     private String[] publicApiPaths() {
         return new String[]{
                 "/api/v1/auth/login",
+                "/api/v1/staff/registration-companies",
+                "/api/v1/staff/self-registrations",
                 "/api/v1/auth/refresh",
                 "/api/v1/auth/master/**",
                 "/api/v1/customer/**",
diff --git a/backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java b/backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java
index 9088318d..44436f9c 100644
--- a/backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java
+++ b/backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java
@@ -1,8 +1,9 @@
 package com.complex.platform.gate;
 
+// 文件版本：V0.01
+
 import com.complex.platform.access.AccessActor;
 import com.complex.platform.access.AuthorizationService;
-import com.complex.platform.access.staff.StaffDeviceCredentialRecord;
 import com.complex.platform.access.staff.StaffProfileRecord;
 import com.complex.platform.access.staff.JdbcStaffRepository;
 import com.complex.platform.common.api.ApiException;
@@ -34,8 +35,8 @@ import org.springframework.web.util.WebUtils;
  * AUD-010 每请求设备凭证验签 + 服务端派生可信终端身份 + enforce 前置加固全套。
  *
  * <p>设备绑定型终端（staff_work_phone/tablet/kiosk）的<b>写</b>请求需对
- * {@code deviceId|timestamp|nonce|METHOD|path|sha256(body)} 的签名验签：staff_work_phone→员工 Keystore 公钥且
- * header deviceId 归属该员工（⑤）；kiosk/tablet→设备级签名公钥（{@code terminal_device_signing_keys}，②）。
+ * {@code deviceId|timestamp|nonce|METHOD|path|sha256(body)} 的签名验签：staff_work_phone→工作手机设备级公钥且
+ * header deviceId 归属该员工（⑤）；所有设备绑定型终端均使用设备级签名公钥（{@code terminal_device_signing_keys}，②）。
  * 验签量含 body SHA-256（④）；nonce 由拦截器落库防重放（③）。
  *
  * <p><b>2026-08-03 收口</b>：终端类型不再由自报的 {@code X-Terminal-Type} 决定入闸与否——带
@@ -246,10 +247,6 @@ public class DeviceCredentialVerifier {
             if (device.staffId() == null || !device.staffId().equals(profile.id())) {
                 return INVALID_OWNER;
             }
-            return staffRepository.findActiveCredential(profile.id())
-                    .map(StaffDeviceCredentialRecord::devicePublicKey)
-                    .filter(k -> k != null && !k.isBlank())
-                    .orElse(null);
         }
         return deviceSigningKeyStore.findActivePublicKey(deviceId).filter(k -> !k.isBlank()).orElse(null);
     }
diff --git a/backend/src/main/java/com/complex/platform/terminal/TerminalDeviceService.java b/backend/src/main/java/com/complex/platform/terminal/TerminalDeviceService.java
index fef0276f..d2781dab 100644
--- a/backend/src/main/java/com/complex/platform/terminal/TerminalDeviceService.java
+++ b/backend/src/main/java/com/complex/platform/terminal/TerminalDeviceService.java
@@ -1,5 +1,7 @@
 package com.complex.platform.terminal;
 
+// 文件版本：V0.01
+
 import com.complex.platform.common.api.ApiException;
 import com.complex.platform.common.api.PageResponse;
 import com.complex.platform.common.idempotency.IdempotencyService;
@@ -671,7 +673,7 @@ public class TerminalDeviceService {
                 null,
                 null,
                 trimToNull(actual.boundSpaceId()),
-                null,
+                existingDevice == null ? null : existingDevice.staffId(),
                 "bound",
                 now,
                 null,
=== main vs baseline vs unpublished (blob ids) ===
backend/src/main/java/com/complex/platform/config/SecurityConfig.java baseline=bd0b4b2409 head=bd0b4b2409 main_unchanged_since_baseline=yes diff_lines_main_vs_unpublished=4
backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java baseline=9088318d52 head=9088318d52 main_unchanged_since_baseline=yes diff_lines_main_vs_unpublished=11
backend/src/main/java/com/complex/platform/terminal/TerminalDeviceService.java baseline=fef0276f6e head=fef0276f6e main_unchanged_since_baseline=yes diff_lines_main_vs_unpublished=4
=== probes: staff_id assignment paths (unpublished TerminalDeviceService / DTOs / web page) ===
260:                firstNonBlank(actual.staffId(), existing.staffId()),
291:                saved.staffId(),
376:                                existing.staffId(),
394:                                saved.staffId(),
676:                existingDevice == null ? null : existingDevice.staffId(),
704:                saved.staffId(),
735:                firstNonBlank(actual.staffId(), existing.staffId()),
754:                saved.staffId(),
785:                existing.staffId(),
804:                saved.staffId(),
942:                record.staffId(),
964:                response.staffId(),
backend/src/main/java/com/complex/platform/terminal/dto/DeviceReturnRequest.java:10:        String staffId,
backend/src/main/java/com/complex/platform/terminal/dto/DeviceLifecycleRequest.java:13:        String staffId,
21:  staffId: string | null;
=== probes: verifier owner check / key source / modes ===
58:    static final Set<String> DEVICE_BOUND_TYPES = Set.of("staff_work_phone", "tablet", "kiosk");
62:    private static final String INVALID_OWNER = " OWNER_MISMATCH";
84:            @Value("${access.device_credential.enforcement.default:grace}") String defaultMode,
165:                        reject(terminalType, deviceId, "credential_required");
182:                reject("app", deviceId, "app_requires_customer_proof");
223:        if (INVALID_OWNER.equals(publicKey)) {
247:            if (device.staffId() == null || !device.staffId().equals(profile.id())) {
248:                return INVALID_OWNER;
251:        return deviceSigningKeyStore.findActivePublicKey(deviceId).filter(k -> !k.isBlank()).orElse(null);
=== probes: signing-key endpoint permission (main controller) / role_staff_self seeds ===
5:48-        authorizationService.requirePermission("terminal.device.bind");
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/resources/db/migration/V400__staff_profile_pii_and_self_device_permis
=== probes: registerSigningKey guards ===
    @Transactional
    public String registerSigningKey(String deviceId, String publicKey, String idempotencyKey) {
        String id = requireText(deviceId, "device_id");
        String key = requireText(publicKey, "public_key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "idempotency_key is required");
        }
        try {
            return idempotencyService.execute(
                    "registerDeviceSigningKey:" + id,
                    idempotencyKey,
                    java.util.Map.of("device_id", id),
                    () -> {
                        TerminalDeviceRecord device = repository.findByDeviceId(id)
                                .orElseThrow(() -> ApiException.notFound(
                                        "TERMINAL_DEVICE_NOT_FOUND", "终端设备不存在：" + id));
                        if (!"bound".equals(device.status())) {
                            throw ApiException.conflict(
                                    "TERMINAL_DEVICE_NOT_BOUND", "终端设备未绑定：" + id);
                        }
                        boolean rotating = deviceSigningKeyStore.findActivePublicKey(id).isPresent();
                        deviceSigningKeyStore.upsert(id, key);
                        String status = rotating ? "rotated" : "registered";
=== probes: public paths ===
    private String[] publicApiPaths() {
        return new String[]{
                "/api/v1/auth/login",
                "/api/v1/staff/registration-companies",
                "/api/v1/staff/self-registrations",
                "/api/v1/auth/refresh",
                "/api/v1/auth/master/**",
                "/api/v1/customer/**",
                "/api/open/**",
                "/api/v1/terminals/kiosks/*/config",
                "/api/v1/terminals/kiosks/*/heartbeat",
                "/api/v1/system/health",
                "/api/v1/system/version",
                "/actuator/health",
                "/actuator/info"
        };
    }
=== consumers: punch requires device.staffId == profile (U06) ===
864:        if (!profile.id().equals(device.staffId())) {
865:            throw ApiException.forbidden("WORK_PHONE_OWNER_REQUIRED", "只能用分配给本人的工作手机记录考勤");
=== tests seeding staff_id directly (U11) ===
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java:153: 
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java
=== overlay unit tests (U12 evidence, /tmp/u12_tests.log; surefire) ===
Tests run: 23, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.295 s -- in com.complex.platform.gate.DeviceCredentialVerifierTests
=== HTML greps (main 正本) ===
379:              <p>管理工作手机、平板、一体机和扫码设备的台账、领还、受管策略和状态。</p>
611:          <h2>设备管理：谁领了什么设备，设备能不能用</h2>
END=2026-09-09T20:30:09Z
```

## 3. 三线审计方法与覆盖

- 对象口径：未发布提交 03acdb98 的只读输入副本第 7 段（3 文件 1585 行；相对基线仅 +11/−8：`SecurityConfig` 两条公开路径、`DeviceCredentialVerifier` 工作手机改用设备级公钥并去掉员工凭证回落、`TerminalDeviceService` 重绑保留 staffId），不是 main；三份业务 HTML 取 main 正本；判据锁 V0.11 不变。本段全文逐行读完，改动行逐字对照（§2 全量 diff），main 当前版本仅作分叉对照。
- 第一性原理线：从 HTML 反推「谁领了什么设备、设备能不能用」——PGM L379「管理工作手机、平板、一体机和扫码设备的台账、领还、受管策略和状态」、PGM-036/037、BSM L930/L1445（本人工作手机领取即上班）、PGM L578（私人手机 App 注册）、PGM L739/BEQR L233 十一端；逐一对照验签、公开路径、设备绑定/归还/生命周期。
- 顺从式线：71 项判据逐项（§5）。
- 对抗式线：新验签口径下工作手机身份链是否闭合（设备级公钥 + `device.staffId == 本人档案`）——谁写 `staff_id`；公钥登记端点的权限与归属限制；公开路径的信任边界；grace/enforce 缺省对本材料安全声称的影响；重绑是否丢失或可篡改归属；与第 1/2/6 段调用方的契约是否一致。
- 只读依赖追流（不审、不计入本段结论）：副本 `StaffService.punch`（第 6 段）、`StaffRegistrationService`（第 5 段）、`DeviceCredentialVerifierTests`（第 12 段）、`M010RegressionTests`/`StaffJdbcIntegrationTests`（第 11 段）、安卓 `registerDeviceSigningKey`/`bindDevice`（第 1/2 段）；main `TerminalDeviceController`、`dto/DeviceBindRequest`/`DeviceReturnRequest`/`DeviceLifecycleRequest`、`TerminalDevicesPage.tsx`、`V322`。
- 可执行验证：`/tmp/u-overlay/backend` 离线 `mvn -o compile` 退出 0；`DeviceCredentialVerifierTests` 23/23（含新增 `privatePhoneKeyCannotReplaceWorkPhoneKey`）（§2）。本材料未发布，不做 B3；处置写给发布者。

## 4. 与发布基线 / main 的分叉对照

- 三文件 main 自基线以来是否有改动见 §2 blob 对照；M009–M012 未触及本段。改动量小，重放到 main 无结构性冲突；但语义面：main 验签器对工作手机读员工凭证公钥、副本改读设备级公钥——客户端（第 1/2 段）与后端必须同 PR（U02-004 已登记）。

## 5. 71 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001 必要性 | 通过 | 设备级公钥、重绑保留归属、公开注册路径均有 HTML 上位或为第 1/5 段所需 |
| GEN-002 可执行正确性 | 不通过 | 工作手机身份链缺「领用/分配」写路径：无任何端点或页面写 `terminal_devices.staff_id`，而验签与打卡都要求 `device.staffId == 本人档案`（CX-U07-001） |
| GEN-003 测试有效性 | 通过（登记） | 第 12 段验签测试覆盖新钥源与拒绝路径；第 11 段用仓储直写 `staff_id` 造夹具，掩盖了 CX-U07-001 的缺口（随其处置补 HTTP 级用例） |
| GEN-004 信任边界 | 不通过（跨段） | 两条公开路径 permitAll（`SecurityConfig:121-122`）——信任边界缺口本体在第 5 段 CX-U05-002，本段只登记出处 |
| GEN-005 授权 | 通过（登记） | `registerSigningKey` 仅要求 `terminal.device.bind`，不校验调用方与设备归属/类型（main 既有，OBS-1，归 M064）；员工自助角色不含该权限，登记须管理员会话 |
| GEN-006 数据完整性 | 通过 | 重绑保留 `staffId`（`:676`）、生命周期条件 UPDATE、终态封锁 |
| GEN-007 并发幂等 | 通过 | 公钥登记/绑定/归还/流转均幂等 |
| GEN-008 接口一致性 | 通过 | 验签规范串与第 1/2 段客户端一致（U02 §5）；`DEVICE_BOUND_TYPES` 与 `TERMINAL_TYPES` 同集 |
| GEN-009 错误处理 | 通过 | 拒绝路径统一 `DEVICE_CREDENTIAL_INVALID` + 审计 |
| GEN-010 安全 | 通过（登记） | 缺省 grace：设备绑定型无凭证写请求放行并审计（OBS-2）；enforce 依赖配置；app 写 enforce 拒 |
| GEN-011 审计证据 | 通过 | 公钥登记/轮换、验签拒绝、生命周期均记审计 |
| GEN-012 资源 | 通过 | 反查复用验签查询 |
| GEN-013 构建 | 通过 | 隔离编译与验签测试绿（§2） |
| GEN-014 迁移 | 不适用 | 本段无迁移 |
| GEN-015 界面 | 不适用 | 无界面 |
| GEN-016 文档 | 通过 | 类注释已改为「所有设备绑定型终端均使用设备级签名公钥」，与实现一致 |
| GEN-017 分类 | 通过 | 四分类见 §6 |
| GEN-018 证据 | 通过 | 结论落到文件:行、HTML 行、§2 命令 |
| BEQR-150 终端十一端 | 通过（登记） | 设备绑定型三类不含私人手机码（私人手机不签设备级钥）——与第 1/2 段 U01-001 处置一致；本段无冲突 |
| BEQR-153 终端开关优先级 | 通过 | 派生身份供开关拦截器读取（main） |
| BEQR-157/182/183/184/185/186/187 | 不适用 | 本段无员工状态/考勤/请假/薪资实现面 |
| BSM-001/002/003/009 | 通过 | 无冲突 |
| BSM-012 酒吧平板/工作手机 | 通过（登记） | 平板/工作手机设备级签名；领用路径缺口见 CX-U07-001 |
| BSM-013 一体机 | 通过 | kiosk 令牌与签名钥并存（既有） |
| BSM-026/028/029 | 通过 | 无冲突 |
| BSM-044 平板点酒/工作手机核销 | 不通过 | 工作手机「领到自己那台」在设备台账无写入路径（CX-U07-001） |
| BSM-045 主要功能 | 通过 | 无冲突 |
| BSM-046/047 | 通过 | 无冲突 |
| BSM-048/049/050 一体机 | 通过 | kiosk 路径未改 |
| BSM-073 | 不适用 | 无实现面 |
| BSM-084/085/086/087/088 | 不适用 | 本段无员工端实现面（打卡依赖见 CX-U07-001） |
| BSM-114 各类入口 | 通过（登记） | 公开路径出处（第 5 段登记） |
| BSM-115 | 通过 | 无实现面 |
| BSM-120 已确认规则 | 不通过 | L1445「领到自己那台工作手机」的设备归属无写路径（CX-U07-001） |
| PGM-001 | 通过 | 中文错误文案 |
| PGM-008 员工和手机 | 通过（登记） | 同 CX-U07-001 |
| PGM-010 终端开关 | 通过 | 未改 |
| PGM-022 空间和点位 | 通过 | 绑定带点位 |
| PGM-024 | 不适用 | 无实现面 |
| PGM-026 | 通过 | 无冲突 |
| PGM-032/033 员工和手机 | 不通过（跨段） | 公开注册无设备证明（CX-U05-002）；本段仅登记 permitAll 出处 |
| PGM-034/035 | 不适用 | 无实现面 |
| PGM-036 设备管理「谁领了什么设备」 | 不通过 | 无领用/分配写路径（CX-U07-001） |
| PGM-037 主要功能 | 不通过 | 同上（台账、领还） |
| PGM-038 | 通过 | 无冲突 |
| PGM-045/046/047 终端开关 | 通过 | 派生身份口径未改 |
| PGM-048 商户侧 | 通过 | 设备商户归属改绑留痕（既有） |
| PGM-059 个人信息保护 | 通过 | 本段不处理 PII |
| PGM-066 | 通过 | 无冲突 |

## 6. 本次 Findings（临时 ID CX-U07-00N；正式 ID 由 B2 分配）

### CX-U07-001 · 技术性 · 发布前须修：是

- 位置：`DeviceCredentialVerifier.java:236-252`（`resolveSigningKey`：工作手机须 `device.staffId()` 非空且等于当前员工档案，否则 `device_owner_mismatch`）；`TerminalDeviceService.java:635-708`（`bind`：`DeviceBindRequest` 无 `staffId` 字段，`:676` 只保留 `existingDevice.staffId()`）；`:260`/`:735`（仅 `DeviceLifecycleRequest`/`DeviceReturnRequest` 的 `staffId` 会写入——送修/挂失/找回/报废/归还，不是领用）；main `TerminalDevicesPage.tsx:21` 只展示 `staffId`；第 6 段 `StaffService.punch` L864 同样要求 `device.staffId == profile.id`；第 11 段 `M010RegressionTests.java:209-221`、`StaffJdbcIntegrationTests.java:1031` 以仓储直写 `staff_id` 造夹具。
- HTML 原句：PGM L379「管理工作手机、平板、一体机和扫码设备的台账、领还、受管策略和状态」；PGM-036「谁领了什么设备，设备能不能用」；BSM L1445「员工到岗领到自己那台工作手机的时刻，就是上班打卡的时刻」。
- 判据：GEN-002、BSM-044、BSM-120、PGM-036/037；依据类型：HTML 原句 + 代码事实。
- 业务影响：本材料把工作手机身份链改为「设备级公钥 + 设备归属员工」，但全仓（副本与 main）没有一条「把工作手机分配/领用给某员工」的写路径：新登记的工作手机 `staff_id` 为空 → 验签 `device_owner_mismatch`、打卡 `WORK_PHONE_REQUIRED`/`WORK_PHONE_OWNER_REQUIRED`，第 1 段的领取即上班在首次正常使用即失败；只有归还/生命周期请求夹带 `staff_id` 这种非领用语义的旁路。归属检查在 main 既有，但只有本材料让它承载真实流量。
- 最小反例：管理员按第 1 段流程绑定设备并登记公钥（无 staff_id）→ 员工登录后 NFC 领取 → `403 DEVICE_CREDENTIAL_INVALID: device_owner_mismatch`（enforce）或 `WORK_PHONE_REQUIRED`（grace，`verified=false`）。
- 无业务发明理由：HTML 已要求台账「领还」；缺的是领用写路径。
- 工程缺陷：是。建议登记：是。最小处置（给发布者）：增加领用/分配写路径（绑定请求带 `staff_id` 并留痕，或独立「领用」端点，校验档案在同一公司/商户范围），后台设备页可操作；第 11 段改为经该路径造夹具。不在本轮修复。

## 7. 观察（登记、不入队）

- OBS-1：`registerSigningKey`（`:87-131`）只校验设备存在且 bound，不校验调用方与设备的归属/类型；持 `terminal.device.bind` 者可为任意已绑定设备登记/轮换公钥（main 既有，AUD-010 ②，归 M064）。工作手机因归属检查仍需本人令牌；平板/一体机无归属检查。
- OBS-2：验签缺省 grace（`enforcement.default:grace`）：设备绑定型写请求无凭证时放行并审计；本材料的安全声称（签名、本人设备）只在 enforce 下成立，配置归运维。
- OBS-3：员工自助角色（V322/V394）不含 `terminal.device.bind`，第 1 段的绑定与公钥登记须在管理员会话下完成，与其 UI 文案「由设备管理员填写」一致；登记。
- OBS-4：公开路径 `registration-companies`/`self-registrations` 出处在本段（`SecurityConfig:121-122`），信任边界缺口本体已登记为第 5 段 CX-U05-002。
- OBS-5：`TERMINAL_TYPES`（`:49`）与验签 `DEVICE_BOUND_TYPES` 同为三类；私人手机码不在其中，属员工凭证体系（`staff_device_credentials`），与 U01-001 处置一致。
- 技能：`using-superpowers`、`verification-before-completion`（编译与验签测试实跑附 §2）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**FAIL**。3 文件全文三线审计、71 项判据逐项核对；发现 1 项技术性须修（CX-U07-001）、5 项观察；无添加项；无 HTML 修改申请；隔离编译与验签测试绿；未修源、未提交/推送。本报告落盘前未读取 `U07-b1-grok-raw.md` 任何正文。
---

# U07 B2-r1 席甲票（追加段）
版本：V0.02（本文件自身；B1 段原字节不变）

- 输入：findings V1.56「当前 U07 B2-r1 整批候选」U07-001…007；modules/U07-b1-grok-raw.md V0.01（41384bytes SHAce3747c9cb17e41fca7007314e25f47cdfba9bce5a84b0c0daa2c6f4ebb7bbaf）；本文件 B1 段（25888bytes SHAe1018b9c75ed1a930cad56697f513587128eb70c95051224994c6b3bc8035f12）。2026-09-09T22:27:04Z
- 席甲 B2 核实（只读，见 ledger V2.62 U07 节）：副本 `DeviceCredentialVerifier.java:236-251` 读设备级公钥、main 同位置读员工凭证（分叉成立）；`:138-149/:174-186` 无设备号时信自报；`:309-312` 英文 reason 拼接在基线同位置已有（main 既有）；`TerminalDeviceService.java:635-676` bind 不写 `staffId`（席甲 B1 已核）；Grok 离线 EXP A/B/D 与席甲 overlay 23/23 相互印证。

## 勘误（席甲 B1 段）

- 席甲 B1 只把 staffId 无写入路径单列，未把「验签改读设备级公钥」相对 main 的分叉与「无设备号信自报」单列（前者席甲 §4 只作分叉描述，后者归 U01-001 未在本段登记）；B2 采纳 Grok GR-002/003 分别为 U07-002/003。B1 段原字节不改。

## 逐 ID 票

| ID | 票 | 依据 |
|---|---|---|
| 001 | PASS | bind 不写 staffId、verifier 拒空、客户端被推向签名路径——加重事实经 Grok EXP D 与席甲 B1 §2 核实；HTML PGM L611/L617 明文绑定员工/责任人；处置最小 |
| 002 | PASS | 钥源分叉经 main 测试 22/1/2 与副本 23/23 印证；同根 U02-004，处置只引用「一把钥」 |
| 003 | PASS | 无设备号信自报为 U01-001 服务端面；同根登记 |
| 004 | PASS | permitAll + 无频控为 U05-003 的安全配置面；同根登记 |
| 005 | PASS | 三文件 main＝基线；同根 U05-008 |
| 006 | PASS | 拼接基线既有、本材料未改 → main 既有归 M063 |
| 007 | PASS | 观察归并未改写双方原意 |

## 勘误与采纳（接受 Grok B2-r1 非阻断措辞修正 3 条，定稿以此为准）

1. 001 事实栏 HTML 行号读作 PGM L611（标题）+ L612（绑定员工）+ L617（责任人）+ L618（领用）；BSM-029 锁出处为 L714 的 h3，句子在 L720。
2. 007 定稿保持「并入 003/004」正式号；回溯 Grok B1 原文时其写的 001/003 为 GR 临时号。
3. Grok B1 文首「发布前须修：GR-001…006」以 GR-005 正文与其 B2 (a) 为准：006 不进本段发布门（main 既有归 M063）。

## 结论

席甲 B2-r1 票：**PASS**（候选 U07-001…007 成立；发布前须修「是」5 / 「否」2；添加 0；questions 0）。本票为作者自审票，以 Grok B2-r1 票合取；Grok FAIL 具否决权。本段落盘前已读 Grok B2-r1 追加段（V0.02，全文 62765bytes SHA5ad6b6c57855e77db4508d2ea9a1933159e47b5c8558f48e7a69d83a704bf82d，前 41384bytes 原字节不变）。
