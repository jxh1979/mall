CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U02 未发布材料第 2 段（安卓共享请求与模型）B1 只读全量审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-09T19:03:13Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `445a49f409f0cfbe6dd616ae079be1299824c48f`；工作区 tree `c3109e1eaeb5c5c63870cf4ce7aae6905992da26`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 对象：未发布提交 03acdb9844a25bcbc4a5312b9fd6ce9f02cc1ccb（树 e28dad059b2c7e08bcc95a8db18552d3f76edf4b，发布基线 242f4872）的只读输入副本第 2 段，4 文件 1180 行（DeviceRequestHeaders.kt 29 新增 / TerminalApiClient.kt 338 / TerminalModels.kt 777 / DeviceRequestHeadersTest.kt 36 新增），main 主归属 M376；适用判据＝M376 映射行并集。步骤：B1（只读全量三线审计；材料不在 main，不做 B3）。
- 文件来源：上列路径均读自输入副本根目录 `.scratch/module-recheck-20260908/unpublished-03acdb98`（非 main）；行数/字节/SHA256 与清单 `unpublished-03acdb98-input.json` 逐文件核对；三份业务 HTML 取 main 当前正本；main 当前版本仅作分叉对照（只读）。隔离编译/测试在 `/tmp/u-overlay`（发布基线 242f4872 `git archive` + 76 个未发布文件覆盖，76/76 哈希核对）内进行，不触主仓。
- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `U02-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：M001–M012 已闭环（M001–M010 已合并 PR #428，HEAD=bd445728；M011/M012 修复在工作区）；U01 已按 B1→B2 定稿口径处理（见 ledger）。未发布材料只读审查：B1 双席 → B2 定稿登记（正式 ID U02-00N，处置写给发布者），不做 B3/B4/B5。
- 只读依赖追流（不审）：未发布副本内 `android-app/.../TerminalApiClient.kt`、`MainActivity.kt`（第 1 段）、`backend/.../gate/DeviceCredentialVerifier.java`（第 7 段）、`backend/.../access/staff/dto/*.java`、`StaffController.java`（第 6 段）；main 当前同名文件。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| apps/android-terminal/src/main/kotlin/com/complex/terminal/DeviceRequestHeaders.kt | 29 | 1048 | f98cbc15aaa3faad2dd7b9e59d7e5f025fb9a2f29afdf59eaf02e66daa25508b | 全读 |
| apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt | 338 | 14671 | eb929512867a25ee3517d39ee806cbe25913461e91d2b3c5674088b2d7e2a6df | 全读 |
| apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt | 777 | 21435 | 548ca991be1481fc23a907c7b6a374082fe8cfa3a7c1ce1c44b4e36a4835de9c | 全读 |
| apps/android-terminal/src/test/kotlin/com/complex/terminal/DeviceRequestHeadersTest.kt | 36 | 1602 | 78fe72eaec7a3423ed203cd407b026a4e35a1598a0730f45fa01f4c64aeb216e | 全读 |

## 2. 可执行证据原文（/tmp/u02_b1_run.log）

```
START=2026-09-09T19:03:13Z
HEAD=445a49f409f0cfbe6dd616ae079be1299824c48f
review_tree_type=tree
=== unpublished copy: lines/bytes/sha (must match manifest) ===
apps/android-terminal/src/main/kotlin/com/complex/terminal/DeviceRequestHeaders.kt | 29 | 1048 | f98cbc15aaa3faad2dd7b9e59d7e5f025fb9a2f29afdf59eaf02e66daa25508b
apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt | 338 | 14671 | eb929512867a25ee3517d39ee806cbe25913461e91d2b3c5674088b2d7e2a6df
apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt | 777 | 21435 | 548ca991be1481fc23a907c7b6a374082fe8cfa3a7c1ce1c44b4e36a4835de9c
apps/android-terminal/src/test/kotlin/com/complex/terminal/DeviceRequestHeadersTest.kt | 36 | 1602 | 78fe72eaec7a3423ed203cd407b026a4e35a1598a0730f45fa01f4c64aeb216e
=== manifest entries ===
apps/android-terminal/src/main/kotlin/com/complex/terminal/DeviceRequestHeaders.kt 29 1048 f98cbc15aaa3faad2dd7b9e59d7e5f025fb9a2f29afdf59eaf02e66daa25508b
apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt 338 14671 eb929512867a25ee3517d39ee806cbe25913461e91d2b3c5674088b2d7e2a6df
apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt 777 21435 548ca991be1481fc23a907c7b6a374082fe8cfa3a7c1ce1c44b4e36a4835de9c
apps/android-terminal/src/test/kotlin/com/complex/terminal/DeviceRequestHeadersTest.kt 36 1602 78fe72eaec7a3423ed203cd407b026a4e35a1598a0730f45fa01f4c64aeb216e
=== baseline 242f4872 -> unpublished tree e28dad05 (this segment) ===
 .../com/complex/terminal/DeviceRequestHeaders.kt   | 29 +++++++++++++++++
 .../com/complex/terminal/TerminalApiClient.kt      | 33 ++++++++++++++++----
 .../kotlin/com/complex/terminal/TerminalModels.kt  | 26 ++++++++++++++++
 .../complex/terminal/DeviceRequestHeadersTest.kt   | 36 ++++++++++++++++++++++
 4 files changed, 118 insertions(+), 6 deletions(-)
=== main current vs unpublished copy (divergence) ===
apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt: main_sha=6dd72cdddc47b1884fe1ddd2d8abaec130222da1e351cc0ffbf3a60111ebf478 unpublished_sha=eb929512867a25ee3517d39ee806cbe25913461e91d2b3c5674088b2d7e2a6df diff_lines=33
apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt: main_sha=f497f624d7aa873b5cebe50d706410038bb012843cc09a96253959038985743b unpublished_sha=548ca991be1481fc23a907c7b6a374082fe8cfa3a7c1ce1c44b4e36a4835de9c diff_lines=26
apps/android-terminal/src/main/kotlin/com/complex/terminal/DeviceRequestHeaders.kt: main_exists=no
apps/android-terminal/src/test/kotlin/com/complex/terminal/DeviceRequestHeadersTest.kt: main_exists=no
=== main_vs_baseline: is main's copy unchanged since 242f4872 (blob ids) ===
apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt baseline=db95efd951592066f6719bb94a4860735ba273e7 head=db95efd951592066f6719bb94a4860735ba273e7 unpublished=006a5a4046eeadbd8a2f2c6781c9e177c722221d main_unchanged_since_baseline=yes
apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt baseline=363b1fdcf41869d3282dc517da74d3fc19db2bb9 head=363b1fdcf41869d3282dc517da74d3fc19db2bb9 unpublished=c227ce68452385d0f0ea7e5ab14d7f1d5fa5a00c main_unchanged_since_baseline=yes
=== probes: terminal-type switching / signing exclusions / private-phone comment ===
133:  /** 手机自助提交员工注册申请(审批通过后才有账号;staff_private_phone 终端契约)。 */
299:    val publicRegistration = path == "/api/v1/staff/self-registrations" || path == "/api/v1/staff/registration-companies"
300:    builder.header("X-Terminal-Type", if (publicRegistration) "app" else "staff_work_phone")
303:    if (id != null && signer != null && path != "/api/v1/auth/login" && !publicRegistration) {
304:      deviceRequestHeaders(id, method, path, body, signer).forEach { (name, value) -> builder.header(name, value) }
=== probes: canonical string (client) vs server verifier (unpublished copy) ===
21:  val canonical = listOf(deviceId, timestamp.toString(), nonce, method.uppercase(java.util.Locale.ROOT),
22:    path.substringBefore('?'), digest).joinToString("\n")
27:    "X-Device-Signature" to signer(canonical.toByteArray(Charsets.UTF_8)),
226:        String bodyDigest = sha256Hex(cachedBody(request));
227:        String canonical = deviceId + "\n" + ts + "\n" + nonce + "\n"
228:                + request.getMethod().toUpperCase(Locale.ROOT) + "\n" + request.getRequestURI()
229:                + "\n" + bodyDigest;
337:        String m = method == null ? "" : method.toUpperCase(Locale.ROOT);
346:            return Mode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
=== probes: models (self-registration / profile phone / shift review) ===
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt:38:    val explicitlyAllowed = Syste
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt:399:  val deviceFingerprintHash: String
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt:422:  val deviceFingerprintHash: String
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt:451:  val phone: String? = null,
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt:452:  val idDoc: String? = null,
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt:456:data class StaffRegistrationCompany
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt:459:data class StaffSelfRegistrationReq
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt:461:  val companyId: String,
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt:464:  val phone: String? = null,
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt:465:  val idDoc: String? = null,
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt:469:data class StaffRegistrationReceipt
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt:478:  val phone: String? = null,
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt:479:  val companyId: String? = null,
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt:623:  val reviewId: String? = null,
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt:624:  val publicationStatus: String? = 
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt:634:  val deviceFingerprint: String? = 
=== probes: Java DTO field names (unpublished backend copy) ===
package com.complex.platform.access.staff.dto;

// 文件版本：V0.01
public record StaffSelfRegistrationRequest(String idempotencyKey, String companyId, String merchantId,
        String name, String phone, String idDoc) {}
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/android-terminal/src/main/kotlin/com/complex/terminal/Termina
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/android-terminal/src/main/kotlin/com/complex/terminal/Termina
.scratch/module-recheck-20260908/unpublished-03acdb98/apps/android-terminal/src/main/kotlin/com/complex/terminal/Termina
.scratch/module-recheck-20260908/unpublished-03acdb98/backend/src/main/resources/application.yml:16:    property-naming-
=== probes: stale comments ===
133:  /** 手机自助提交员工注册申请(审批通过后才有账号;staff_private_phone 终端契约)。 */
189:  /** 主管排班(双端;同员工同开始时刻幂等覆盖)。 */
193:  /** 我的班次与点位(今天起 7 天,时间到分钟)。 */
=== isolated gradle build/test of overlay (/tmp/u-overlay = baseline 242f4872 + 76 unpublished files; log /tmp/u02_gradle.log) ===
/tmp/u02_gradle.done
1:START=2026-09-09T18:06:47Z
21:BUILD SUCCESSFUL in 12s
23:GRADLE_EXIT=0
24:END=2026-09-09T18:07:00Z
binary
TEST-com.complex.terminal.DeviceRequestHeadersTest.xml
TEST-com.complex.terminal.InMemoryLocalStoreTest.xml
TEST-com.complex.terminal.NdefTextTest.xml
TEST-com.complex.terminal.P0FixtureTest.xml
TEST-com.complex.terminal.TerminalFieldWorkflowTest.xml
TEST-com.complex.terminal.TerminalRequestSerializationTest.xml
tests="1" skipped="0" failures="0" errors="0"
=== HTML greps (main 正本) ===
docs/platform-governance-map-for-operations-v0.1.html:578:                <li>员工注册分两条路径：公司自有员工由本人在私人手机 App 注册、提交资料后后台审批；商户
docs/platform-governance-map-for-operations-v0.1.html:870:                <li>客人和员工的手机号、身份信息等个人信息，在系统里一律加密存放。</li>
docs/platform-governance-map-for-operations-v0.1.html:871:                <li>员工日常界面只显示脱敏号码，不见明文；界面脱敏不提供关闭开关。</li>
docs/business-system-map-for-operations-v0.1.html:1433:                <li>新员工注册分两条路径：商户员工由商户代该员工提交注册申请，经对应域公司批准后账号生�
END=2026-09-09T19:03:13Z
```

## 3. 三线审计方法与覆盖

- 对象口径：未发布提交 03acdb98 的只读输入副本第 2 段（4 文件 1180 行；其中 `DeviceRequestHeaders.kt`、`DeviceRequestHeadersTest.kt` 为 main 无对应的新增文件），不是 main；三份业务 HTML 取 main 正本；判据锁 V0.11 不变。本段文件的真实改动 = `git diff 242f4872 e28dad05 -- <本段路径>`（§2 diffstat），main 当前版本仅作分叉对照。
- 第一性原理线：从 HTML 反推共享客户端应当如何声明终端身份、如何证明「真实设备」、注册路径归哪一端——BSM L930「平板、公司工作手机、员工私人手机 App 是不同设备和不同用途」、PGM L578/BSM L1433 注册两条路径、PGM L739/BEQR L233 十一端、PGM L579/BSM L935「私人手机 App 安装时生成并绑定一个该手机专属的设备标识」、PGM L870–871 个人信息加密存放与脱敏显示；逐一对照 `applyHeaders`、`deviceRequestHeaders`、请求/响应模型。
- 顺从式线：50 项判据逐项（§5）。
- 对抗式线：客户端能否按路径自选终端身份（同 U01 根因）；签名规范串与服务端是否逐字节一致（含大小写、query、正文摘要）；重放/时间窗是否由客户端承担；模型字段名与后端 DTO 是否对得上（JSON 命名策略）；`StaffProfileRecord.phone` 回传是否走脱敏；测试是否只锁自身实现而不锁服务端契约；隔离编译/测试是否真跑。
- 只读依赖追流（不审、不计入本段结论）：副本内 `android-app/.../TerminalApiClient.kt`、`MainActivity.kt`（第 1 段）、`DeviceCredentialVerifier.java`（第 7 段）、`StaffController.java`、`StaffProfileRecord.java`、`dto/StaffSelfRegistrationRequest.java`、`dto/StaffAttendancePunchRequest.java`（第 5/6 段）、`application.yml`；main 当前 `TerminalCanaryService`。
- 4 文件全文逐行读完（§1）；与 main 的逐文件 diff 逐块读完（§2 diffstat）。可执行验证：在 `/tmp/u-overlay`（`git archive 242f4872` + 76 个未发布文件覆盖，76/76 哈希核对，不触主仓）内离线 `gradle -p apps/android-terminal --offline build`，含本段新增测试在内 6 个测试类全跑（§2）。本材料未发布，不做 B3；处置写给发布者。

## 4. 与发布基线 / main 的分叉对照

- `TerminalApiClient.kt`、`TerminalModels.kt` 与 main 不同，改动全部是新增（设备签名接线、自助注册两接口、注册/回执/档案/班次模型字段）；两个新增文件 main 无对应。main 侧这两文件自基线以来是否有改动见 §2 `main_vs_baseline` 行。
- M009–M012 未改动本段任何文件；发布不会覆盖 main 已修项。M376 尚未开审（PENDING），本段中「main 既有」的项（§7）明确标注，不算入本材料引入。

## 5. 50 项判据逐项核对

| 判据 | 结论 | 依据 |
|---|---|---|
| GEN-001 必要性 | 不通过 | 公开注册路径改报「app」身份无 HTML 上位（CX-U02-001，同 U01-001 根因） |
| GEN-002 可执行正确性 | 通过（本段） | 签名规范串与服务端逐字节一致（§2 探针：客户端 `path.substringBefore('?')`/`uppercase` ↔ 服务端 `getRequestURI()`/`toUpperCase`，正文 SHA-256 十六进制）；隔离 gradle 构建与测试全绿（§2） |
| GEN-003 测试有效性 | 通过（登记） | `DeviceRequestHeadersTest` 锁规范串布局、小写方法归一、query 剔除、正文篡改可检；未覆盖重放/时间窗（属服务端，登记 OBS-2） |
| GEN-004 信任边界 | 不通过 | 终端身份由客户端按路径自报（`TerminalApiClient.kt:299–300`），公开写路径不带设备身份（CX-U02-001） |
| GEN-005 授权 | 不通过 | 同上：自报「app」使未登记设备写请求绕过设备绑定型 `credential_required`（副本 verifier L162–166） |
| GEN-006 数据完整性 | 通过 | 签名覆盖 设备号/时间戳/nonce/方法/路径/正文摘要；query 不在覆盖内（OBS-1，服务端同口径） |
| GEN-007 并发幂等 | 通过 | 幂等键由调用方传入（第 1 段 UUID）；`registerDeviceSigningKey` 显式 `idempotency_key` |
| GEN-008 接口一致性 | 不通过（文档面） | 模型字段与副本 DTO 一致（`StaffSelfRegistrationRequest` 6 字段同名、`AttendancePunchRequest` 同名；SnakeCase ↔ SNAKE_CASE）；但 `TerminalApiClient.kt:133` 自注「staff_private_phone 终端契约」与 L300 实发「app」自相矛盾（CX-U02-001 工程面） |
| GEN-009 错误处理 | 通过 | `decode` 非 2xx 抛 `ApiException`；`require(deviceId.isNotBlank())` 早失败 |
| GEN-010 安全 | 不通过 | 身份自选（CX-U02-001）；`ALLOW_INSECURE_TERMINAL_HTTP` 环境开关为 main 既有（OBS-4） |
| GEN-011 审计证据 | 通过 | 设备头四元组随每个已登记设备请求发送，服务端据此审计 |
| GEN-012 资源 | 通过 | 每请求一次 SHA-256 与一次签名，无额外网络 |
| GEN-013 构建 | 通过 | `/tmp/u-overlay` 离线 gradle `build` BUILD SUCCESSFUL，6 测试类 25 例 0 失败（§2） |
| GEN-014 迁移 | 不适用 | 本段无迁移 |
| GEN-015 界面 | 不适用 | 本段无界面文案 |
| GEN-016 文档 | 不通过 | L133 注释与实现矛盾；L189「同员工同开始时刻幂等覆盖」、L193「今天起 7 天」与审批/可配天数新流程不符（OBS-3） |
| GEN-017 分类 | 通过 | 四分类见 §6 |
| GEN-018 证据 | 通过 | 结论落到文件:行、HTML 行、§2 命令 |
| BEQR-150 终端十一端 | 不通过 | 员工自助注册以「app」（客人手机应用）身份进入（CX-U02-001） |
| BEQR-153 终端开关优先级 | 不通过 | 同上：客人 App 开关管到员工注册，员工端开关管不到 |
| BSM-001 | 通过 | 无实现面冲突 |
| BSM-002 | 通过 | 无实现面 |
| BSM-003 | 通过 | 无实现面冲突 |
| BSM-012 酒吧平板/工作手机 | 通过 | 核销/交接接口未改 |
| BSM-013 一体机 | 不适用 | 无实现面 |
| BSM-026 各类入口 | 通过 | 未新增越权入口 |
| BSM-029 | 通过 | 无实现面 |
| BSM-044 平板点酒/工作手机核销 | 不通过 | 工作手机身份下承载私人手机 App 的注册契约（L133 自注）（CX-U02-001） |
| BSM-045 | 通过 | 未改 |
| BSM-046 | 通过 | POS 接口未改 |
| BSM-047 | 通过 | 无实现面 |
| BSM-048/049/050 | 不适用 | 无实现面 |
| BSM-114 各类入口 | 通过 | 无越权 |
| BSM-115 | 通过 | 无实现面 |
| BSM-120 已确认规则 | 不通过 | L1433 注册路径（CX-U02-001）；L1443/L1444 相关字段（reviewId/publicationStatus）仅模型承载，逻辑在第 6/9 段 |
| PGM-001 整体定位 | 通过（本段） | 本段无用户可见文字 |
| PGM-010 终端开关 | 不通过 | 同 BEQR-153 |
| PGM-032 员工和手机 | 不通过 | L578 私人手机 App 注册路径（CX-U02-001） |
| PGM-033 | 不通过 | 同上 |
| PGM-034 | 不适用 | 无实现面 |
| PGM-035 | 不适用 | 无实现面 |
| PGM-036 设备管理 | 通过 | 签名公钥登记接口与设备编号口径一致 |
| PGM-037 | 通过 | 无冲突 |
| PGM-038 | 通过 | 无冲突 |
| PGM-045 终端开关 | 不通过 | 同 BEQR-153 |
| PGM-046 能控制哪些端 | 不通过 | 同 BEQR-150 |
| PGM-047 怎么控制 | 不通过 | 端身份由客户端按路径改报 |
| PGM-066 | 通过 | 无实现面冲突 |

## 6. 本次 Findings（临时 ID CX-U02-00N；正式 ID 由 B2 分配）

### CX-U02-001 · 冲突 · 发布前须修：是 · 与 U01-001 同根因（共享客户端）

- 位置：`TerminalApiClient.kt:298–307`（`applyHeaders`：两条公开注册路径 `X-Terminal-Type: app`，其余 `staff_work_phone`；公开路径与登录跳过设备签名）；`:133` 注释「手机自助提交员工注册申请(…staff_private_phone 终端契约)」；`:134–137` 两接口。
- HTML 原句：PGM L578「公司自有员工由本人在私人手机 App 注册」；BSM L1433 同义；BSM L930 三种设备不同用途；PGM L739/BEQR L233 十一端。
- 判据：PGM-032/033、BSM-120、BSM-044、BEQR-150/153、PGM-045/046/047、GEN-004/005/008/016；依据类型：HTML 原句。
- 业务影响：与 U01-001 相同——注册契约按代码自注属员工私人手机端，实发身份却是客人手机应用；客人 App 总开关会关掉员工注册、员工端开关管不到它；app 模式 enforce 时 POST 被硬拒 `app_requires_customer_proof`（副本 verifier L180–183）；客户端自选身份绕过设备绑定型 `credential_required`。
- 最小反例：`grep -n "X-Terminal-Type\|staff_private_phone" TerminalApiClient.kt` → L133 说 staff_private_phone、L300 发 app。
- 工程缺陷：是。建议登记：是（与 U01-001 合并处置，避免两处各修一半）。
- 最小处置（给发布者）：与 U01-001 同一处置——注册契约归员工私人手机端，以该端登记身份声明一次，不按路径改报；共享客户端与安卓客户端同一处修，注释与实现一致。不在本轮修复。

## 7. 观察（登记、不入队）

- OBS-1：签名规范串两端均剔除 query（`DeviceRequestHeaders.kt:22` ↔ 服务端 `getRequestURI()`），GET 过滤参数不在签名覆盖内；服务端对设备绑定型的无凭证 GET 在 enforce 下也放行，故无功能影响，属完整性范围登记。
- OBS-2：`DeviceRequestHeadersTest` 不覆盖时间窗/nonce 重放，这两项在服务端 `tryVerify`（时间容差 300s；nonce 是否去重属第 7 段）。
- OBS-3：注释漂移：`:189`「同员工同开始时刻幂等覆盖」（现为审批流）、`:193`「今天起 7 天」（现为按经营主体可配天数）——GEN-016 轻微，随本段发布时一并改注释。
- OBS-4：`ALLOW_INSECURE_TERMINAL_HTTP=true` 环境开关放行非回环 http（`:38`），main 既有，归 M376。
- OBS-5：`StaffProfileRecord.phone` 新增回传字段；副本 `StaffController` 各档案接口均经 `maskedPersonalInfo()`（`PhoneMask.mask`，证件 `******`），与 PGM L871 一致；客户端不再二次脱敏，依赖服务端。
- OBS-6：`AttendancePunchRequest.deviceFingerprint` 字段保留但安卓端不再填写；服务端改用已验签终端身份（第 6 段），字段成为无用字段，登记。
- 技能：`using-superpowers`、`verification-before-completion`（gradle/测试实跑附 §2）、`receiving-code-review`（未读席乙报告）实际使用；未派子代理、未用 MCP。

## 8. 结论

本席 B1 票：**FAIL**。4 文件全文三线审计、50 项判据逐项核对；发现 1 项冲突须修（CX-U02-001，与 U01-001 同根因）、6 项观察；无添加项；无 HTML 修改申请；隔离构建与测试全绿；未修源、未提交/推送。本报告落盘前未读取 `U02-b1-grok-raw.md` 任何正文。


---

# U02 B2-r1 席甲票（追加段）
版本：V0.02（本文件自身；B1 段原字节不变）

- 输入：findings V1.46「当前 U02 B2-r1 整批候选」U02-001…005；modules/U02-b1-grok-raw.md V0.01（43997bytes SHA185da0389050d201b50b74123a26babeae016bfa7016d203005754ebddcba6c0）；本文件 B1 段（24246bytes SHA9f5e5aa5ad8fc5f11d201dd6ddd1559fd3cf706aaa1b41e8e1f7c6bb19900bad）。2026-09-09T19:16:26Z
- 席甲 B2 核实（只读，见 ledger V2.47 U02 节）：基线 JVM `applyHeaders(builder)` 不发类型头；`StaffSelfRegistrationRequest` 两端六字段无设备标识；U07 verifier 去掉员工凭证公钥回落。

## 逐 ID 票

| ID | 票 | 依据 |
|---|---|---|
| 001 | PASS | 与 U01-001 同根；Grok 补充的 fail-closed 面经 diff 核实；处置限定为身份声明规则（不含 U01 的 UI 去入口），不发明新端 |
| 002 | PASS | HTML L579/L935；契约层缺字段成立；处置与 U01-007、U05 审批/Controller、U06 Java record 同 PR |
| 003 | PASS | 席甲 B1 低估 GEN-003（写「通过（登记）」），B2 接受升为技术性入队；测试断言按修 001 后的契约写，不锁路径二分 |
| 004 | PASS | main 404、钥源分叉、缺头拒绝被打开——三点均有 diff/文件证据 |
| 005 | PASS | 观察归并未改写双方原意；席甲 OBS-2（测试缺口观察）保留登记于 005，不作 003 本体 |

## 勘误与采纳（接受 Grok B2-r1 非阻断措辞修正 5 条，定稿以此为准）

1. 003 来源行读作「GR-U02-003（席甲 B1 低估 GEN-003；B2 升技术性）」；席甲 OBS-2 留在 005。
2. 002 处置中「U05 后端」读作「U05 审批/Controller + U06 Java record」。
3. 001「与 U01-001 同一处置」限定为身份声明规则；U01 的登录页去自助注册入口属 U01-001 自身处置。
4. 003 的测试断言按修 001 后的契约，不锁「按路径二分」。
5. 005 补记 OBS-2；不写「Grok 无观察」，Grok §8 第 10 条维持观察。
6. 席甲 B1 §5 GEN-003 行「通过（登记）」按定稿改为技术性入队（U02-003）；B1 段原字节不改。

## 结论

席甲 B2-r1 票：**PASS**（候选 U02-001…005 成立；发布前须修「是」4 / 「否」1；添加 0；questions 0）。本票为作者自审票，以 Grok B2-r1 票合取；Grok FAIL 具否决权。本段落盘前已读 Grok B2-r1 追加段（V0.02，全文 60483bytes SHA4e4ec40521cb6fade699ae622c9318218583c7d1f1653077fb62b834127dbc52，前 43997bytes 原字节不变）。
