CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a
# U-FIX B4-r1 修复内容独立审计（Grok 席乙）

文件版本：V0.01
步骤：B4-r1
席位：固定 Grok 外审席乙
计划：160 审计 V0.17（本席不派子代理、不用 MCP、不修源/HTML/台账/findings/ledger/锁）
本文件路径：docs/audit-2026-09/modules/U-FIX-b4-r1-grok-raw.md
本文件自身版本：V0.01（路径原先不存在，本次新建完整报告）
本次完整报告范围：本文件自本行上一行独占锁行起至文末。正文不再出现同名独占字段行。

修复内容结论：PASS。
范围归因结论：PASS。
总结论：PASS。
以上仅为席乙本轮 B4-r1 票，不等于 B4 双 PASS，也不等于 B5 或模块 PASS。不可用 B3 自报红→绿代替本轮完整审计。席甲=Claude Fable 5.1（作者自审票、唯一修复者）；本席 FAIL 具否决权。§5 闸 3 越界判定同样须双 PASS。本席不行使否决。

本轮只审 B3 第 1 次整批修复的「修复内容」本身（before/after 两树全文 diff），不是 B1/B5 重审、不是修复、不是模块 PASS。U-FIX=XM-009 先例的合成模块，承载 U01–U18 全部已定稿且「发布前须修」归属「是」的 finding ID。定稿依据：docs/audit-2026-09/findings.md V1.81 各段「B2 双PASS定稿」节。操作员唯一业务答复：排班可见天数出厂行上下限＝1 天/31 天（值 7）；自营独立布尔＝删除（questions/U04-001.md）。只用既有冻结判据。未新造判据/角色，未询问操作员。落盘顺序：本席先派发；席甲独立落盘 self-raw 后本席才对照其归因表，本席归因不依赖席甲表。

---

## 1. 输入与独立锁实算

本席哈希命令：`sha256sum docs/audit-2026-09/plan/criteria-lock.md`（独立实算，未抄 sidecar / ledger / 派发期望值）。三工具同值。

LOCK-FILE-SHA256SUM: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a  docs/audit-2026-09/plan/criteria-lock.md

python3 hashlib 同值；`openssl dgst -sha256` 同值；889020 字节。sidecar `docs/audit-2026-09/plan/criteria-lock.sha256` 全文一行同值。ledger 文首锁行同值。criteria-lock 版本仍 V0.11。本批 B3 两树 lock blob 均为 `8db3fc02ce8a6dc998863a9784e7e88af511b960`（未改锁）。

| 项 | 值 |
|---|---|
| 模块 | U-FIX 合成修复模块（U01–U18 已定稿「是」项整批） |
| 阶段 | B4-r1 只读审计 B3 第 1 次整批修复的「修复内容」本身 |
| 正式队列「是」（117） | U01-001/002/003/006/007/008/009；U02-001…004；U03-001…009；U04-001/002；U05-001…011；U06-001…007；U07-001…005；U08-001…005；U09-001…005；U10-001…003；U11-001…008；U12-001…005；U13-001…008/010；U14-001…008；U15-001…008；U16-001…007；U17-001…010；U18-001…004 |
| 正式队列「否」（不得修） | U01-004/005/010–015；U02-005；U03-010–015；U04-003；U05-012；U06-008–011；U07-006/007；U08-006；U09-006；U10-004；U11-009；U12-006；U13-009；U14-009；U15-009；U16-008；U17-011；U18-005 |
| B3 | 已执行 1 次（本轮为第 1 次），模块仍 IN_PROGRESS |
| before（对象类型 tree，可解析） | `28dcfec971f6eca651213368c27ec7e893a08305`（＝445a49f4 的树） |
| after（对象类型 tree，可解析） | `e3fd14b6e61f284e3a9dc7380d529e7486c71b8a` |
| 本席自取 | `git cat-file -t` 两者均为 `tree` |
| 完整 diff 命令 | `git diff --binary 28dcfec971f6eca651213368c27ec7e893a08305 e3fd14b6e61f284e3a9dc7380d529e7486c71b8a`（无路径过滤，覆盖 tracked/staged/untracked） |
| 实际差异 | 1410429 bytes；SHA256 `864ef041c5874c0e1fbc00d01fd79078cb7f4c1962f63120abf9743c076119c1`；UTF-8；85 文件（A 31 / M 54）；+12156 / −7145；默认上下文 606 块 |
| 与 `/tmp/ufix_b4_r1.diff` | 字节与 SHA256 全等（本席自算为准，副本仅对照）；`cmp` 相等 |
| `git diff --check` | exit 0（无空白错误） |
| 当前工作区临时索引 `git add -A && git write-tree`（worktree 内、仓库外 GIT_INDEX_FILE） | `e3fd14b6e61f284e3a9dc7380d529e7486c71b8a`＝after |
| worktree HEAD 树 | `28dcfec971f6eca651213368c27ec7e893a08305`＝before＝origin/main 445a49f4 树 |
| 主工作区 HEAD | `445a49f409f0cfbe6dd616ae079be1299824c48f` = `origin/main`（未改业务源） |
| findings.md / ledger.md | 两树 blob 相同（本 diff 未改）：findings `c9e2a00b513a4b4460d439100cbc467570c2d027`，ledger `aaa74447e99bed7876c393d3f107a84440ff5036` |
| 三 HTML 两树 blob | BSM `12334b13f3c27a815c9bbf9eea216403fb0d76d1` / PGM `c2588ca1c65a3f196b39a23f299bd56967b5652e` / BEQR `86a717930f9303d4f131ee2864478baa14450e6d`，完全相同 |
| 锁/sidecar 两树 blob | 相同（未改） |

全文 diff 可从两 tree id 无过滤复现。本报告不内嵌 1410429 字节正文，以 SHA256 + name-status 全表为可复核指纹。

`git diff --shortstat --binary`：`85 files changed, 12156 insertions(+), 7145 deletions(-)`。

---

## 2. name-status 全表（85）

```
M	apps/android-terminal/android-app/app/src/main/AndroidManifest.xml
A	apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/ChargingReceiptService.kt
M	apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/DeviceKeystore.kt
M	apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt
M	apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt
A	apps/android-terminal/src/main/kotlin/com/complex/terminal/DeviceRequestHeaders.kt
M	apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt
M	apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt
A	apps/android-terminal/src/test/kotlin/com/complex/terminal/DeviceRequestHeadersTest.kt
A	apps/android-terminal/src/test/kotlin/com/complex/terminal/TerminalApiClientHeadersTest.kt
M	apps/web/src/admin/pages/StaffPayrollLeavePage.tsx
M	apps/web/src/admin/pages/StaffProfilesPage.test.tsx
M	apps/web/src/admin/pages/StaffProfilesPage.tsx
M	apps/web/src/admin/pages/StaffShiftAttendancePage.batchshift.test.tsx
M	apps/web/src/admin/pages/StaffShiftAttendancePage.guidedcreate.test.tsx
A	apps/web/src/admin/pages/StaffShiftAttendancePage.settings.test.tsx
M	apps/web/src/admin/pages/StaffShiftAttendancePage.tsx
M	backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java
A	backend/src/main/java/com/complex/platform/access/staff/StaffApprovalService.java
M	backend/src/main/java/com/complex/platform/access/staff/StaffController.java
A	backend/src/main/java/com/complex/platform/access/staff/StaffLockerKioskController.java
A	backend/src/main/java/com/complex/platform/access/staff/StaffLockerService.java
A	backend/src/main/java/com/complex/platform/access/staff/StaffNumericSetting.java
M	backend/src/main/java/com/complex/platform/access/staff/StaffProfileRecord.java
A	backend/src/main/java/com/complex/platform/access/staff/StaffRegistrationService.java
M	backend/src/main/java/com/complex/platform/access/staff/StaffService.java
A	backend/src/main/java/com/complex/platform/access/staff/StaffShiftCoverRecord.java
M	backend/src/main/java/com/complex/platform/access/staff/StaffShiftRecord.java
A	backend/src/main/java/com/complex/platform/access/staff/dto/StaffEmploymentStatusRequest.java
A	backend/src/main/java/com/complex/platform/access/staff/dto/StaffNumericSettingRequest.java
A	backend/src/main/java/com/complex/platform/access/staff/dto/StaffPenaltyCapRequest.java
M	backend/src/main/java/com/complex/platform/access/staff/dto/StaffRegistrationRequest.java
A	backend/src/main/java/com/complex/platform/access/staff/dto/StaffSelfRegistrationRequest.java
M	backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftBatchResult.java
A	backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftCoverRequest.java
M	backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftRequest.java
M	backend/src/main/java/com/complex/platform/common/ratelimit/RateLimitInterceptor.java
M	backend/src/main/java/com/complex/platform/config/SecurityConfig.java
M	backend/src/main/java/com/complex/platform/gate/DeviceCredentialInterceptor.java
M	backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java
M	backend/src/main/java/com/complex/platform/gate/TerminalMasterSwitchInterceptor.java
M	backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java
M	backend/src/main/java/com/complex/platform/terminal/TerminalDeviceService.java
M	backend/src/main/java/com/complex/platform/terminal/dto/DeviceBindRequest.java
A	backend/src/main/resources/db/migration/V411__staff_profile_pii_and_self_device_permission.sql
A	backend/src/main/resources/db/migration/V412__staff_registration_initiator.sql
A	backend/src/main/resources/db/migration/V413__staff_policy_settings.sql
A	backend/src/main/resources/db/migration/V414__staff_profile_encrypted_personal_fields.sql
A	backend/src/main/resources/db/migration/V415__staff_employment_status.sql
A	backend/src/main/resources/db/migration/V416__staff_work_phone_receipts.sql
A	backend/src/main/resources/db/migration/V417__staff_shift_revision_approvals.sql
A	backend/src/main/resources/db/migration/V418__staff_penalty_cap_approval_history.sql
A	backend/src/main/resources/db/migration/V419__staff_registration_company_routes.sql
A	backend/src/main/resources/db/migration/V420__staff_locker_scan_records.sql
A	backend/src/main/resources/db/migration/V421__staff_locker_scan_tokens.sql
A	backend/src/main/resources/db/migration/V422__staff_shift_covers.sql
M	backend/src/test/java/com/complex/platform/access/AccessServiceTests.java
M	backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java
A	backend/src/test/java/com/complex/platform/access/staff/M010RegistrationHttpTests.java
A	backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java
M	backend/src/test/java/com/complex/platform/access/staff/PayrollSafetyJdbcIntegrationTests.java
M	backend/src/test/java/com/complex/platform/access/staff/StaffAttendanceLocalZoneTests.java
M	backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java
M	backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java
M	backend/src/test/java/com/complex/platform/access/staff/StaffScopeTests.java
A	backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java
M	backend/src/test/java/com/complex/platform/gate/DeviceCredentialVerifierTests.java
M	backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java
M	backend/src/test/java/com/complex/platform/terminal/TerminalDeviceServiceTests.java
M	docs/data-isolation-and-tenant-scope-security-spec-v0.1.md
M	docs/domain-ownership-scope-audit-v0.1.csv
M	docs/openapi/openapi-p0.json
M	docs/openapi/openapi-p0.yaml
M	docs/p0-openapi-contract-closure-appendix-v0.1.md
M	docs/p0-openapi-operation-acceptance-matrix-v0.1.md
M	docs/production-readiness-evidence/security-baseline-latest.json
M	docs/r5-spec-T1-staff-tables-dev-design-20260618.md
M	docs/r5-spec-T9-staff-device-android-dev-design-20260618.md
M	security-scans/findings-register-v1.md
M	security-scans/formal-security-baseline-v1.md
M	tools/generate-domain-ownership-scope-audit.mjs
M	tools/generate-formal-security-baseline.mjs
M	tools/generate-openapi-contract-closure-appendix.mjs
M	tools/generate-openapi-operation-matrix.mjs
M	tools/verify-pii-encryption.mjs
```

路径增量为 31 个 A（无删除路径）。before/after 相对 main HEAD 的 OrganizationMerchantPage.tsx / PlatformService.java 不在本表（见 §5 三方合并）。

---

## 3. §5 闸 3 独立逐块归因

归因规则（本席自定、与派发一致）：材料重放行→该段定稿「以 main 为底重放本段/叠法」ID；U03 人事七文件材料行归 U03-007；席甲修复行→对应定稿 ID；「文件版本」行随该文件所属 ID。禁止「否」项 ID。

本席独立解析默认上下文 diff 得 **606 块**（与 `grep -c '^@@'` 一致）。对每块：先按路径赋叠法默认 ID，再按块正文关键词叠加具体修复 ID。结果：

- **UNMAPPED = 0**（每一块至少命中一个队列「是」ID）。
- **命中「否」队列 ID = 0**。
- 席甲表 `/tmp/ufix_b4_r1/attrib.md` 606 行、0 未归因、0 队列外 ID；本席对照后，席甲 ID 集合 ⊆ 117 个「是」ID。本席与席甲在部分多因块上 ID 集合不完全相同（关键词会把同根项一并标上，例如终端类型块同时标 U01-001/U02-001/U07-003），但交集非空且全部为「是」——闸 3 要求的是「每一行能归因到定稿是项之一」，不是两席标签集逐字相等。
- `-U0` 细粒度 2126 块：同一文件默认映射覆盖全部路径，0 文件落在无默认 ID 的路径上。

无 hunk 的「是」项（19，属删除/取 main 侧/同根被吸收，见 §4.B）：U03-001…006、U04-001/002、U06-003（同根 U05-001）、U08-001…005、U09-004（同根 U12-003）、U14-002/U15-002/U16-002/U17-002（删除自营布尔，OpenAPI 净效果并入 U14-001/U15-001 重生）。闸 3 不要求「每个是项都有 hunk」，要求「每个 diff 行都有是项」。

独立逐块表见文末附录 A（606 行）。

---

## 4. 修复内容：逐 ID 核对

分类顺序：冲突 → 细化 → 技术性 → 添加。判定：是否修好、是否最懒可行、是否引入新缺陷或新业务发明。

### 4.A 有净 diff 的「是」项（本席源码核对）

| ID | 分类 | 本席裁定 | 证据（AFTER 树） |
|---|---|---|---|
| U01-001 | 冲突 | 修好 | verifier 注释与实现：终端类型不再由自报 `X-Terminal-Type` 单独入闸；无设备号写请求 `DEVICE_CREDENTIAL_INVALID`；私人手机钥＝员工生效凭证且指纹须等于设备号。JVM 客户端 `unboundClientDeclaresNoTerminalTypeAndNoDeviceHeaders` 可失败。 |
| U01-002 | 技术性 | 修好 | 新增 `ChargingReceiptService` 前台服务 + Manifest；MainActivity 注册充电断开。最懒：常驻接收器，不发明新领取规则。 |
| U01-003 | 技术性 | 修好 | 文案「仅存不可逆加密，不可回显」→「加密存放，界面只显示脱敏」（`:1777`）。最小改一句。 |
| U01-006 | 技术性 | 修好 | POS 绑定成功写 `registerDeviceSigningKey`（`:266` 与 `:1268`）。未改 bar_pos 类型码（U01-011 否项仍在）。 |
| U01-007 | 冲突 | 修好 | 自助注册提交 `devicePublicKey`/`deviceFingerprintHash`；函数体用 DeviceKeystore。 |
| U01-008 | 技术性 | 修好 | 新增 `TerminalApiClientHeadersTest` 5 例（绑定类型不按路径改报、签名规范串、注册设备证明、bind staffId），断言指向真实 header/JSON 字段，空绑定即失败。 |
| U01-009 | 技术性叠法 | 修好 | android-app 三文件以 main 为底重放，未整树覆盖。 |
| U02-001 | 冲突 | 修好 | JVM `applyHeaders` 无绑定时不发 `X-Terminal-Type`。 |
| U02-002 | 冲突 | 修好 | `StaffSelfRegistrationRequest`/`TerminalModels` 增设备公钥与指纹。 |
| U02-003 | 技术性 | 修好 | `DeviceRequestHeadersTest` + `TerminalApiClientHeadersTest`。 |
| U02-004 | 技术性叠法 | 修好 | 共享客户端四文件重放。 |
| U03-007 | 技术性叠法 | 修好 | 人事七文件以 main 为底快进；自营动作未迁入（见 4.B）。 |
| U03-008 | 技术性 | 修好 | 默认不带 `include_archived`；勾选后才 `true` 重拉。测试覆盖。 |
| U03-009 | 技术性 | 修好 | 编辑态 `validate: editingShift && !note.trim() → "修改排班必须填写原因"`；guidedcreate 用例先拦截空原因再 POST。 |
| U05-001 | 冲突 | 修好 | 分流读 `cooperationForm=="self_operated"`，无独立布尔。 |
| U05-002 | 冲突 | 修好 | 事假注释与 SQL：基数＝固定工资+台账提成+酒吧提成+奖金；个税/处罚另算。未把小费/服务费卷入。 |
| U05-003 | 冲突 | 修好 | DTO 八字段含设备证明；注册服务校验指纹与公钥。 |
| U05-004 | 冲突 | 修好 | 封顶分母注释改为 HTML 总薪资（现行列先取固定工资与奖金）；禁止「底薪+奖金＝总薪资」误称。 |
| U05-005 | 冲突 | 修好 | 出厂行 `shift_visible_days/global/7/1/31`（操作员裁定）；读路径经营主体行缺失回落 global，两者皆无 `POLICY_NOT_CONFIGURED`。未另开 2.2。 |
| U05-006 | 技术性 | 修好 | 柜机单端 `StaffLockerKioskController` + V421 一次性扫码凭证；员工身份由扫码载荷换回。 |
| U05-007 | 技术性 | 修好 | `history()` 只读路径去 FOR UPDATE；审批落地仍行锁。 |
| U05-008 | 技术性叠法 | 修好 | staff 仓储/控制器以 main 为底重放，不带 U08 自营插拔。 |
| U05-009 | 技术性 | 修好 | `companyChain` 按生效运营主体绑定，无 `parent_entity_id`。 |
| U05-010 | 技术性 | 修好 | 写入强制 `company_id`；读路径注释去掉以 project_id 兜底的 COALESCE。工资合计 COALESCE 是金额空值，不是 scope 兜底。 |
| U05-011 | 技术性 | 修好 | 两写口及多处 `requireIdempotencyKey`。 |
| U06-001 | 技术性 | 修好 | `settingScopeKey(merchantId,companyId)` 写读共用，不回落 projectId。 |
| U06-002 | 冲突 | 修好 | 同 U05-003 DTO。 |
| U06-004 | 冲突 | 修好 | `coverShift` 追加记录 + V422；不走上一级审批。最懒入口。 |
| U06-005 | 冲突 | 修好 | 同 U05-005 读路径。 |
| U06-006 | 技术性 | 修好 | 同 U05-010 去 null 放行。 |
| U06-007 | 技术性叠法 | 修好 | StaffService 以 main 为底重放。 |
| U07-001 | 冲突 | 修好 | `DeviceBindRequest.staffId`；工作手机缺 staffId 拒。 |
| U07-002 | 技术性 | 修好 | `resolveSigningKey` 私人手机/工作手机各认一把钥；测试 `privatePhoneKeyCannotReplaceWorkPhoneKey`。 |
| U07-003 | 冲突 | 修好 | 同 U01-001 fail-closed。 |
| U07-004 | 冲突 | 修好 | 公开自助注册进 AUTH 频控；SecurityConfig 放行该路径。 |
| U07-005 | 技术性叠法 | 修好 | SecurityConfig/verifier/TerminalDeviceService 以 main 为底。 |
| U09-001 | 技术性叠法 | 修好 | SupervisorReviewService 重放。 |
| U09-002 | 技术性 | 修好 | `isMoneyAction` 含 `staff.payroll.*`。 |
| U09-003 | 技术性 | 修好 | 策略含 hr/finance/legal；测试补 hr_review。 |
| U09-005 | 技术性 | 修好 | 构造函数必填 `ApplicationEventPublisher`；AccessServiceTests 测试桩补 `event -> {}`。 |
| U10-001 | 技术性 | 修好 | V411–V420 连续号；main V410 保留；无 V400–V409。 |
| U10-002 | 添加已答删除 | 修好 | 迁移无 `is_self_operated` 列；V419 只加 company_id/registration_source。 |
| U10-003 | 冲突 | 修好 | 同 U05-005 出厂行。 |
| U11-001…008 | 技术性/冲突 | 修好 | 回归测试随实现改断言：自营改合作方式、事假/封顶期望、可见天数种子、柜机单端、无设备指纹 4xx、`e.code()`。本席空库实跑 44+2+7+4+29+4+3 全绿。 |
| U12-001…005 | 技术性/冲突 | 修好 | verifier 23/23、审批 44/44：钥源、DEPARTMENT_REVIEWS_PENDING、DEVICE_CREDENTIAL_INVALID、无设备号 fail-closed。 |
| U13-001…008/010 | 技术性/冲突 | 修好 | T1/T9 八处口径；隔离规格仅自动块 V410→V422（main 侧重生，非整树覆盖）；生成器 reminder_days 读 canonical。 |
| U14-001…008 | 技术性/冲突/添加已删 | 修好 | json 732 operation、info.version V0.14；无 setMerchantSelfOperated；自助注册设备字段；可见天数 description 含 7＋1..31＋缺行拒绝；locker 单端；幂等；失败事件非 none；StaffRegistrationRequest required 含 merchant_id。yaml 由生成器同写（`--check` 绿）。 |
| U15-001…008 | 同根 U14 | 修好 | yaml 与 json 深比较绿。 |
| U16-001…007 | 技术性 | 修好 | 附录随契约重生；三口成功事件 `staff.shift.submitted` / `staff.payroll.penalty_cap_requested`；测试对 `audit_events` 计数，空库可失败。 |
| U17-001…010 | 技术性 | 修好 | 矩阵标题跟 info.version；删变更表；taskGroup staff→STAFF-员工；附录生成器同口径。 |
| U18-001…004 | 技术性/细化 | 修好 | 三工具以 main 为底重放；域归属 334 表 `--check` 绿；PII 登记 staff_profiles 三列，门禁 16 列绿。 |

### 4.B 无净 diff 的「是」项（删除 / 取 main 侧＝修好）

| ID | 本席裁定 | 证据 |
|---|---|---|
| U03-001…006 / U04-001/002 / U08-001…005 | 修好（最懒＝不合入） | OrganizationMerchantPage.tsx / PlatformService.java AFTER blob＝main HEAD＝before（`7d5b3ee9` / `fe67a293`）。types.ts blob 仍 `8d26e821`，无 `selfOperated`。无 MerchantSelfOperatedRequest。未发布 types.ts `:1121` 的独立布尔未进入 AFTER。 |
| U06-003 | 修好 | 同根 U05-001，读六类码后自然收敛。 |
| U09-004 | 修好 | 用例落在 U12 测试文件（U12-003）。 |
| U14-002 / U15-002 / U16-002 / U17-002 | 修好 | OpenAPI 无 self-operated 路径/布尔字段。 |

### 4.C 「否」项与 main 既有项：确认未修

抽查仍在 AFTER、且本 diff 未以其为目标：

- U06-008 三处仍在：`(initial_password)`、`（approved）或驳回（rejected）`、`id is required`。
- U06-010 `missing_checkout` 仍在。
- U06-009 请假仍无撤销/销假入口。
- U01-005/010：`跳过，进入首页` 仍 1 处；`showHome` 按钮仍 23。
- U01-011：`terminal_type=bar_pos` 仍在（未改 tablet）。
- U03-010：`today()` 仍 `toISOString().slice`（UTC）。
- U07-006：`reject` 仍拼接英文 `reason`。
- 未发布材料把 `project_id is required` 等三处收窄为中文，属 U06-007 重放「本材料已收窄」既有口径，不是去修 U06-008 留下的三处。

### 4.D 新缺陷 / 新业务发明

未发现超出定稿处置的业务发明。V421/V422 分别是 U05-006/U06-004 最小表；可见天数 1/31/7 是操作员已答上线配置；自营独立布尔已删除。顶班不走审批。柜机不要求同一请求带员工会话。

---

## 5. 越界检查（闸 3）

| 禁区 | 结果 |
|---|---|
| 三份业务 HTML | 两树 blob 全等，name-status 无 html |
| 锁 / sidecar | 两树 blob 全等 |
| ledger 基准 / findings（两树内） | blob 全等；本 diff 未改 |
| 主工作区业务源 | HEAD 仍 445a49f4；本席未写 worktree/主仓源（仅本 raw） |
| 整树覆盖 main | 否。OrganizationMerchantPage.tsx / PlatformService.java 取 main 侧。data-isolation 仅自动块 V410→V422 与版本行（main 生成器重生），不是未发布整文件 |
| 「否」项 / main 既有归属项 | 抽查未作为本批目标修掉（§4.C） |
| 材料未定稿项 | 无 |

---

## 6. 独立复跑验证（只读；禁写 complex_test*；未 source 整份 env）

副本：`git archive` AFTER 树 → `/tmp/ufix_b4_grok_copy`（软链 node_modules）。后端另 rsync 到 `/tmp/ufix_b4_grok_backend`，JDBC 改指一次性空库 `ufix_b4_grok_scratch`（CREATE 后测完 DROP）。密码只 `grep '^COMPLEX_DATABASE_PASSWORD='`。

| 检查 | 本席结果 |
|---|---|
| web `npx tsc --noEmit` | EXIT 0 |
| web vitest 5 files | 33/33 passed EXIT 0（含 OrganizationMerchantPage 15、U03-008/009 新例） |
| JVM `gradle test` | BUILD SUCCESSFUL；7 类 **30/30** F0 E0（含 HeadersTest 5、DeviceRequestHeadersTest 1） |
| Android `compileDebugKotlin`（ANDROID_HOME=/home/james/android-sdk） | BUILD SUCCESSFUL EXIT 0 |
| 后端员工/验签/审批/设备 10 类空库 | MVN_EXIT=0；M010Regression 44、RegistrationHttp 2、PayrollSafety 7、Attendance 4、StaffJdbc 29、ArchiveDelete 4、StaffScope 3、Verifier 23、SupervisorReview 44、TerminalDevice 37；**flyway_max=422**；库已 DROP |
| 副本 `verify-openapi-contract` / examples / matrix / closure / data-isolation / domain-ownership / pii / security-baseline --strict / matrix-vs-migration | 全 0；732 operation；334 表；PII 16 列 |
| 席甲全量日志 `/tmp/ufix_full_tests.log` | 3402 run / 6F+14E / 16 skip；失败类＝Finance 12E、Amusement 1E、Compliance 3F、Facility 2F+1E、CustomerGuarded 1F。`/tmp/ufix_base_tests.log` 纯净 main 同五类，且 main 空库员工三类红、本分支绿。本席**未**再跑 3402（耗时），以域内 197 例独立绿 + 全量日志失败类对照为证。席甲 `/tmp/ufix_android.log` 与 `/tmp/ufix_web_tsc.log` 为空、`/tmp/ufix_jvm.log` 不完整——本席不以那些空日志为证，已自跑。 |
| U16-006 TDD 日志 `/tmp/ufix_tests.log` | GREEN 段 44+29；断言为 `audit_events` 计数，缺事件即失败。 |

新增用例能失败：HeadersTest 在未绑定时期待 null 头；guidedcreate 空原因期望含「原因」且 `lastPostBody===null`；RegistrationHttp 无设备证明走 4xx；U16-006 COUNT(*) 对具体 event_name。

---

## 7. 问题二分（回退统一规则）

**有 ID（队列「是」，已修，不构成 FAIL）：** 无残留须修项。

**无 ID：** 无。未发现 diff 行不能归到 117 个「是」ID 之一。未发现新业务发明。未发现应升队的新缺陷。

观察（不阻断、不立项）：席甲 B4 文称 DeviceCredentialVerifierTests 39 例，AFTER 源码 `@Test` 与本席实跑均为 23（与 U12 B1 基数一致，本 diff 只 +22/−2 行）；属计数笔误，不是缺测。

---

## 8. 结论

修复内容成立：117 个「是」项均按定稿最小边界落地（含 19 个以「不合入 / 取 main 侧」完成的删除项）。范围归因：606/606 块可独立归到定稿「是」ID，0 未归因，0「否」项被修。越界检查通过。本席票 **PASS**。本票须与席甲 B4-r1 合取；本席不行使否决。

本席未 commit / push / PR / merge / 部署；未改三 HTML、锁、sidecar、ledger 基准、主工作区源码。

---

## 附录 A. 本席独立逐块归因表（默认上下文 606 块）

| 路径 | hunk | + | − | 本席独立归属 ID |
|---|---|---:|---:|---|
| `apps/android-terminal/android-app/app/src/main/AndroidManifest.xml` | `@@ -2,6 +2,11 @@` | 5 | 0 | U01-009 |
| `apps/android-terminal/android-app/app/src/main/AndroidManifest.xml` | `@@ -11,8 +16,20 @@` | 12 | 0 | U01-009, U01-002 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/ChargingReceiptService.kt` | `@@ -0,0 +1,93 @@` | 93 | 0 | U01-009, U01-002 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/DeviceKeystore.kt` | `@@ -1,5 +1,7 @@` | 2 | 0 | U01-009 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/DeviceKeystore.kt` | `@@ -21,6 +23,16 @@ object DeviceKeystore {` | 10 | 0 | U01-009, U01-007, U02-002, U05-003, U06-002, U14-003 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -1,5 +1,7 @@` | 2 | 0 | U01-009 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -47,6 +49,7 @@ class MainActivity : Activity() {` | 1 | 0 | U01-009 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -56,16 +59,22 @@ class MainActivity : Activity() {` | 7 | 1 | U01-009 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -76,8 +85,10 @@ class MainActivity : Activity() {` | 4 | 2 | U01-009 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -85,11 +96,37 @@ class MainActivity : Activity() {` | 27 | 1 | U01-009, U01-002, U01-001, U02-001, U07-003 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -114,6 +151,11 @@ class MainActivity : Activity() {` | 5 | 0 | U01-009, U01-002 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -130,7 +172,6 @@ class MainActivity : Activity() {` | 0 | 1 | U01-009 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -145,7 +186,7 @@ class MainActivity : Activity() {` | 1 | 1 | U01-009 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -171,7 +212,8 @@ class MainActivity : Activity() {` | 2 | 1 | U01-009, U01-002 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -191,10 +233,11 @@ class MainActivity : Activity() {` | 3 | 2 | U01-009, U07-001 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -202,19 +245,31 @@ class MainActivity : Activity() {` | 14 | 2 | U01-009, U01-001, U02-001, U07-003, U01-007, U02-002, U05-003, U06-002, U14-003, U07-001 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -260,7 +315,7 @@ class MainActivity : Activity() {` | 1 | 1 | U01-009 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -786,7 +841,7 @@ class MainActivity : Activity() {` | 1 | 1 | U01-009 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -802,52 +857,23 @@ class MainActivity : Activity() {` | 16 | 45 | U01-009, U01-007, U02-002, U05-003, U06-002, U14-003, U05-011, U14-006 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -857,8 +883,8 @@ class MainActivity : Activity() {` | 2 | 2 | U01-009 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -877,7 +903,7 @@ class MainActivity : Activity() {` | 1 | 1 | U01-009 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -1168,26 +1194,26 @@ class MainActivity : Activity() {` | 14 | 14 | U01-009, U05-011, U14-006 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -1230,17 +1256,22 @@ class MainActivity : Activity() {` | 8 | 3 | U01-009, U01-007, U02-002, U05-003, U06-002, U14-003 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -1535,6 +1566,7 @@ class MainActivity : Activity() {` | 1 | 0 | U01-009 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt` | `@@ -1742,7 +1774,7 @@ class MainActivity : Activity() {` | 1 | 1 | U01-009 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | `@@ -1,5 +1,7 @@` | 2 | 0 | U01-009 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | `@@ -57,6 +59,13 @@ class TerminalApiClient(` | 7 | 0 | U01-009, U05-011, U14-006 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | `@@ -90,6 +99,8 @@ class TerminalApiClient(` | 2 | 0 | U01-009 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | `@@ -262,7 +273,7 @@ class TerminalApiClient(` | 1 | 1 | U01-009, U01-001, U02-001, U07-003 |
| `apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | `@@ -270,6 +281,12 @@ class TerminalApiClient(` | 6 | 0 | U01-009 |
| `apps/android-terminal/src/main/kotlin/com/complex/terminal/DeviceRequestHeaders.kt` | `@@ -0,0 +1,29 @@` | 29 | 0 | U02-004 |
| `apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | `@@ -1,5 +1,7 @@` | 2 | 0 | U02-004 |
| `apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | `@@ -24,6 +26,9 @@ class ApiException(` | 3 | 0 | U02-004, U01-001, U02-001, U07-003 |
| `apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | `@@ -48,6 +53,14 @@ class TerminalApiClient(` | 8 | 0 | U02-004, U01-001, U02-001, U07-003, U05-006, U11-006, U14-005, U05-011, U14-006 |
| `apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | `@@ -123,7 +136,13 @@ class TerminalApiClient(` | 7 | 1 | U02-004, U01-001, U02-001, U07-003 |
| `apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt` | `@@ -249,39 +268,48 @@ class TerminalApiClient(` | 17 | 8 | U02-004, U01-001, U02-001, U07-003 |
| `apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt` | `@@ -1,5 +1,7 @@` | 2 | 0 | U02-004 |
| `apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt` | `@@ -53,6 +55,9 @@ data class BindDeviceRequest(` | 3 | 0 | U02-004, U07-001, U05-006, U11-006, U14-005, U05-011, U14-006 |
| `apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt` | `@@ -445,14 +450,39 @@ data class StaffRegistrationSubmitRequest(` | 25 | 0 | U02-004, U01-007, U02-002, U05-003, U06-002, U14-003, U05-011, U14-006 |
| `apps/android-terminal/src/main/kotlin/com/complex/terminal/TerminalModels.kt` | `@@ -596,6 +626,8 @@ data class StaffShiftRow(` | 2 | 0 | U02-004 |
| `apps/android-terminal/src/test/kotlin/com/complex/terminal/DeviceRequestHeadersTest.kt` | `@@ -0,0 +1,36 @@` | 36 | 0 | U02-004 |
| `apps/android-terminal/src/test/kotlin/com/complex/terminal/TerminalApiClientHeadersTest.kt` | `@@ -0,0 +1,84 @@` | 84 | 0 | U02-004, U01-001, U02-001, U07-003, U01-007, U02-002, U05-003, U06-002, U14-003, U07-001, U05-010, U06-006, U05-011, U14-006 |
| `apps/web/src/admin/pages/StaffPayrollLeavePage.tsx` | `@@ -1,3 +1,4 @@` | 1 | 0 | U03-007 |
| `apps/web/src/admin/pages/StaffPayrollLeavePage.tsx` | `@@ -16,7 +17,7 @@ type LeaveRow = { id: string; userId?: string; leaveType?: string; startDate?: s` | 1 | 1 | U03-007, U03-009 |
| `apps/web/src/admin/pages/StaffPayrollLeavePage.tsx` | `@@ -39,6 +40,11 @@ function cnMonth(value?: string) {` | 5 | 0 | U03-007, U09-003, U09-004, U12-003 |
| `apps/web/src/admin/pages/StaffPayrollLeavePage.tsx` | `@@ -180,6 +186,38 @@ export function StaffPayrollLeavePage({ api, setGlobalError, setActionResult, ef` | 32 | 0 | U03-007, U03-009, U05-004, U11-004, U09-003, U09-004, U12-003 |
| `apps/web/src/admin/pages/StaffProfilesPage.test.tsx` | `@@ -1,3 +1,4 @@` | 1 | 0 | U03-007 |
| `apps/web/src/admin/pages/StaffProfilesPage.test.tsx` | `@@ -34,6 +35,40 @@ function renderPage(api: AdminApiClient) {` | 34 | 0 | U03-007, U03-009 |
| `apps/web/src/admin/pages/StaffProfilesPage.test.tsx` | `@@ -75,3 +110,14 @@ describe("StaffProfilesPage 员工档案销售岗维护（ADM-STF-004）", () =>` | 11 | 0 | U03-007, U03-008 |
| `apps/web/src/admin/pages/StaffProfilesPage.tsx` | `@@ -1,11 +1,11 @@` | 2 | 2 | U03-007 |
| `apps/web/src/admin/pages/StaffProfilesPage.tsx` | `@@ -16,35 +16,65 @@ type StaffProfileRow = {` | 33 | 3 | U03-007, U03-008, U03-009 |
| `apps/web/src/admin/pages/StaffProfilesPage.tsx` | `@@ -56,7 +86,7 @@ export function StaffProfilesPage({ api, setActionResult, setGlobalError, effect` | 1 | 1 | U03-007 |
| `apps/web/src/admin/pages/StaffProfilesPage.tsx` | `@@ -64,6 +94,19 @@ export function StaffProfilesPage({ api, setActionResult, setGlobalError, effect` | 13 | 0 | U03-007, U03-009 |
| `apps/web/src/admin/pages/StaffProfilesPage.tsx` | `@@ -76,7 +119,7 @@ export function StaffProfilesPage({ api, setActionResult, setGlobalError, effect` | 1 | 1 | U03-007, U03-009 |
| `apps/web/src/admin/pages/StaffProfilesPage.tsx` | `@@ -92,10 +135,11 @@ export function StaffProfilesPage({ api, setActionResult, setGlobalError, effect` | 3 | 2 | U03-007, U03-008 |
| `apps/web/src/admin/pages/StaffProfilesPage.tsx` | `@@ -115,18 +159,36 @@ export function StaffProfilesPage({ api, setActionResult, setGlobalError, effect` | 21 | 3 | U03-007 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.batchshift.test.tsx` | `@@ -1,3 +1,4 @@` | 1 | 0 | U03-007 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.batchshift.test.tsx` | `@@ -28,7 +29,7 @@ function makeApi(): AdminApi {` | 1 | 1 | U03-007 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.batchshift.test.tsx` | `@@ -102,8 +103,8 @@ describe("StaffShiftAttendancePage 按周批量排班", () => {` | 2 | 2 | U03-007 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.guidedcreate.test.tsx` | `@@ -1,3 +1,4 @@` | 1 | 0 | U03-007 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.guidedcreate.test.tsx` | `@@ -22,15 +23,17 @@ function raw(data: unknown): Response {` | 4 | 2 | U03-007 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.guidedcreate.test.tsx` | `@@ -57,6 +60,7 @@ describe("StaffShiftAttendancePage 引导式新增排班(phase-10)", () => {` | 1 | 0 | U03-007 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.guidedcreate.test.tsx` | `@@ -88,7 +92,7 @@ describe("StaffShiftAttendancePage 引导式新增排班(phase-10)", () => {` | 1 | 1 | U03-007 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.guidedcreate.test.tsx` | `@@ -103,4 +107,28 @@ describe("StaffShiftAttendancePage 引导式新增排班(phase-10)", () => {` | 24 | 0 | U03-007 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.settings.test.tsx` | `@@ -0,0 +1,61 @@` | 61 | 0 | U03-007, U03-009, U05-004, U11-004, U05-005, U06-005, U10-003, U11-005, U09-003, U09-004, U12-003 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.tsx` | `@@ -1,3 +1,4 @@` | 1 | 0 | U03-007 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.tsx` | `@@ -9,17 +10,17 @@ import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDra` | 3 | 3 | U03-007, U01-002, U01-007, U02-002, U05-003, U06-002, U14-003 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.tsx` | `@@ -41,7 +42,12 @@ function today() {` | 6 | 1 | U03-007 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.tsx` | `@@ -136,15 +142,18 @@ export function StaffShiftAttendancePage({ api, setGlobalError, setActionResult,` | 5 | 2 | U03-007 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.tsx` | `@@ -167,7 +176,7 @@ export function StaffShiftAttendancePage({ api, setGlobalError, setActionResult,` | 1 | 1 | U03-007 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.tsx` | `@@ -179,8 +188,8 @@ export function StaffShiftAttendancePage({ api, setGlobalError, setActionResult,` | 2 | 2 | U03-007 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.tsx` | `@@ -205,9 +214,31 @@ export function StaffShiftAttendancePage({ api, setGlobalError, setActionResult,` | 24 | 2 | U03-007, U03-009 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.tsx` | `@@ -225,7 +256,19 @@ export function StaffShiftAttendancePage({ api, setGlobalError, setActionResult,` | 13 | 1 | U03-007, U03-009 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.tsx` | `@@ -255,7 +298,7 @@ export function StaffShiftAttendancePage({ api, setGlobalError, setActionResult,` | 1 | 1 | U03-007 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.tsx` | `@@ -270,7 +313,7 @@ export function StaffShiftAttendancePage({ api, setGlobalError, setActionResult,` | 1 | 1 | U03-007 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.tsx` | `@@ -278,7 +321,9 @@ export function StaffShiftAttendancePage({ api, setGlobalError, setActionResult,` | 3 | 1 | U03-007 |
| `apps/web/src/admin/pages/StaffShiftAttendancePage.tsx` | `@@ -301,7 +346,7 @@ export function StaffShiftAttendancePage({ api, setGlobalError, setActionResult,` | 1 | 1 | U03-007 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -1,4 +1,8 @@` | 4 | 0 | U05-008 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -18,8 +22,12 @@ public class JdbcStaffRepository {` | 5 | 1 | U05-008, U18-004, U05-010 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -29,6 +37,52 @@ public class JdbcStaffRepository {` | 46 | 0 | U05-008, U05-004, U11-004, U05-005, U06-005, U10-003, U11-005, U05-007 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -81,24 +135,34 @@ public class JdbcStaffRepository {` | 15 | 5 | U05-008, U05-002, U11-003, U05-004, U11-004, U05-010, U06-006 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -134,24 +198,46 @@ public class JdbcStaffRepository {` | 35 | 13 | U05-008, U05-002, U11-003, U05-004, U11-004, U05-010, U06-006 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -173,7 +259,8 @@ public class JdbcStaffRepository {` | 2 | 1 | U05-008 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -223,17 +310,34 @@ public class JdbcStaffRepository {` | 20 | 3 | U05-008, U05-010, U06-006 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -245,16 +349,23 @@ public class JdbcStaffRepository {` | 10 | 3 | U05-008, U05-010, U06-006 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -266,14 +377,21 @@ public class JdbcStaffRepository {` | 10 | 3 | U05-008, U05-010, U06-006 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -285,15 +403,22 @@ public class JdbcStaffRepository {` | 10 | 3 | U05-008, U05-010, U06-006 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -462,6 +587,85 @@ public class JdbcStaffRepository {` | 79 | 0 | U05-008, U05-007, U06-004 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -536,6 +740,27 @@ public class JdbcStaffRepository {` | 21 | 0 | U05-008 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -568,6 +793,23 @@ public class JdbcStaffRepository {` | 17 | 0 | U05-008, U05-007 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -629,26 +871,35 @@ public class JdbcStaffRepository {` | 15 | 6 | U05-008, U05-010, U06-006 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -662,15 +913,17 @@ public class JdbcStaffRepository {` | 6 | 4 | U05-008, U05-010, U06-006, U18-004 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -685,15 +938,28 @@ public class JdbcStaffRepository {` | 16 | 3 | U05-008, U05-010, U06-006 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -704,18 +970,18 @@ public class JdbcStaffRepository {` | 4 | 4 | U05-008, U05-010, U06-006 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -725,9 +991,9 @@ public class JdbcStaffRepository {` | 2 | 2 | U05-008, U03-008, U05-010, U06-006 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -752,6 +1018,10 @@ public class JdbcStaffRepository {` | 4 | 0 | U05-008, U05-006, U11-006, U14-005 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -763,9 +1033,9 @@ public class JdbcStaffRepository {` | 2 | 2 | U05-008, U05-010, U06-006 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -820,6 +1090,21 @@ public class JdbcStaffRepository {` | 15 | 0 | U05-008, U01-007, U02-002, U05-003, U06-002, U14-003 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -881,6 +1166,15 @@ public class JdbcStaffRepository {` | 9 | 0 | U05-008 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -905,9 +1199,9 @@ public class JdbcStaffRepository {` | 2 | 2 | U05-008, U05-010, U06-006 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -944,9 +1238,9 @@ public class JdbcStaffRepository {` | 3 | 3 | U05-008, U18-004, U05-010 |
| `backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java` | `@@ -961,7 +1255,7 @@ public class JdbcStaffRepository {` | 1 | 1 | U05-008, U05-010, U06-006 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffApprovalService.java` | `@@ -0,0 +1,104 @@` | 104 | 0 | U05-008, U09-005, U05-004, U11-004, U05-011, U14-006, U09-002, U14-008, U16-004 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffController.java` | `@@ -1,4 +1,6 @@` | 2 | 0 | U05-008, U05-011, U14-006 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffController.java` | `@@ -26,15 +28,38 @@ import org.springframework.web.bind.annotation.RestController;` | 24 | 1 | U05-008, U05-006, U11-006, U14-005, U05-011, U14-006 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffController.java` | `@@ -43,40 +68,50 @@ public class StaffController {` | 16 | 6 | U05-008, U03-008, U05-011, U14-006 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffController.java` | `@@ -135,6 +170,24 @@ public class StaffController {` | 18 | 0 | U05-008, U05-011, U14-006, U06-004 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffController.java` | `@@ -145,6 +198,30 @@ public class StaffController {` | 24 | 0 | U05-008, U05-004, U11-004, U05-011, U14-006 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffController.java` | `@@ -291,14 +368,14 @@ public class StaffController {` | 2 | 2 | U05-008, U05-002, U11-003, U05-011, U14-006 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffLockerKioskController.java` | `@@ -0,0 +1,29 @@` | 29 | 0 | U05-006, U11-006, U14-005 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffLockerService.java` | `@@ -0,0 +1,159 @@` | 159 | 0 | U05-006, U11-006, U14-005, U05-007, U05-011, U14-006, U09-005, U14-008, U16-006, U12-004, U12-005, U16-004 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffNumericSetting.java` | `@@ -0,0 +1,5 @@` | 5 | 0 | U05-005 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffProfileRecord.java` | `@@ -1,11 +1,13 @@` | 4 | 2 | U05-008, U05-010, U06-006 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffProfileRecord.java` | `@@ -14,8 +16,8 @@ public record StaffProfileRecord(` | 2 | 2 | U05-008 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffProfileRecord.java` | `@@ -24,7 +26,7 @@ public record StaffProfileRecord(` | 1 | 1 | U05-008 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffProfileRecord.java` | `@@ -33,7 +35,10 @@ public record StaffProfileRecord(` | 4 | 1 | U05-008 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffProfileRecord.java` | `@@ -43,8 +48,8 @@ public record StaffProfileRecord(` | 2 | 2 | U05-008 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffProfileRecord.java` | `@@ -54,32 +59,81 @@ public record StaffProfileRecord(` | 57 | 8 | U05-008 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffRegistrationService.java` | `@@ -0,0 +1,161 @@` | 161 | 0 | U05-008, U05-001, U05-003, U01-007, U02-002, U06-002, U14-003, U06-003, U11-002, U05-009, U11-001, U05-010, U06-006, U05-011, U14-006, U09-005, U14-008, U16-006, U16-004 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -1,6 +1,6 @@` | 1 | 1 | U06-007 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -66,6 +66,14 @@ public class StaffService {` | 8 | 0 | U06-007 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -115,7 +123,9 @@ public class StaffService {` | 2 | 0 | U06-007, U05-011, U14-006, U14-008, U16-004 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -124,11 +134,11 @@ public class StaffService {` | 2 | 2 | U06-007, U14-008, U16-004 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -142,8 +152,8 @@ public class StaffService {` | 2 | 2 | U06-007 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -153,7 +163,8 @@ public class StaffService {` | 2 | 1 | U06-007, U05-011, U14-006, U09-005, U14-008, U16-006 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -184,8 +195,15 @@ public class StaffService {` | 9 | 2 | U06-007 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -211,11 +229,13 @@ public class StaffService {` | 7 | 5 | U06-007, U05-011, U14-006 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -230,11 +250,8 @@ public class StaffService {` | 2 | 5 | U06-007, U14-008, U16-004 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -266,8 +283,12 @@ public class StaffService {` | 6 | 2 | U06-007, U03-008 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -287,11 +308,11 @@ public class StaffService {` | 2 | 2 | U06-007 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -299,7 +320,7 @@ public class StaffService {` | 1 | 1 | U06-007, U09-005, U14-008, U16-006 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -313,16 +334,43 @@ public class StaffService {` | 29 | 2 | U06-007, U05-011, U14-006, U09-005, U14-008, U16-006, U16-004 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -342,10 +390,15 @@ public class StaffService {` | 6 | 1 | U06-007, U09-005, U14-008, U16-006 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -353,6 +406,9 @@ public class StaffService {` | 3 | 0 | U06-007 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -362,10 +418,6 @@ public class StaffService {` | 0 | 4 | U06-007, U09-005, U14-008, U16-006, U16-004 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -375,7 +427,7 @@ public class StaffService {` | 1 | 1 | U06-007, U09-005, U14-008, U16-006 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -405,11 +457,11 @@ public class StaffService {` | 2 | 2 | U06-007, U01-007, U02-002, U05-003, U06-002, U14-003, U07-001 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -420,9 +472,9 @@ public class StaffService {` | 3 | 3 | U06-007, U07-001, U05-011, U14-006, U09-005, U14-008, U16-006 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -493,9 +545,8 @@ public class StaffService {` | 2 | 3 | U06-007 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -572,29 +623,49 @@ public class StaffService {` | 38 | 18 | U06-007, U05-011, U14-006, U06-004, U09-005, U14-008, U16-006, U16-004 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -605,12 +676,30 @@ public class StaffService {` | 20 | 2 | U06-007, U05-011, U14-006, U09-005, U14-008, U16-006, U16-004 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -643,24 +732,20 @@ public class StaffService {` | 3 | 7 | U06-007, U05-011, U14-006 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -690,23 +775,161 @@ public class StaffService {` | 141 | 3 | U06-007, U03-009, U05-004, U11-004, U05-005, U06-005, U10-003, U11-005, U05-011, U14-006, U06-001, U06-004, U09-005, U14-008, U16-006, U16-004 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -714,21 +937,58 @@ public class StaffService {` | 40 | 3 | U06-007, U01-002, U01-001, U02-001, U07-003, U07-001, U05-011, U14-006, U09-005, U14-008, U16-006, U16-004 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -740,7 +1000,7 @@ public class StaffService {` | 1 | 1 | U06-007, U01-007, U02-002, U05-003, U06-002, U14-003, U05-011, U14-006, U09-005, U14-008, U16-006 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -778,12 +1038,12 @@ public class StaffService {` | 2 | 2 | U06-007, U09-005, U14-008, U16-006 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -800,7 +1060,7 @@ public class StaffService {` | 1 | 1 | U06-007 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -889,12 +1149,12 @@ public class StaffService {` | 2 | 2 | U06-007, U05-011, U14-006, U09-005, U14-008, U16-006 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -911,7 +1171,22 @@ public class StaffService {` | 16 | 1 | U06-007 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -951,13 +1226,37 @@ public class StaffService {` | 25 | 1 | U06-007 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -1073,7 +1372,7 @@ public class StaffService {` | 1 | 1 | U06-007, U05-011, U14-006, U09-005, U14-008, U16-006, U16-004 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -1175,7 +1474,7 @@ public class StaffService {` | 1 | 1 | U06-007 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -1273,7 +1572,7 @@ public class StaffService {` | 1 | 1 | U06-007 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffService.java` | `@@ -1287,55 +1586,46 @@ public class StaffService {` | 31 | 40 | U06-007 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffShiftCoverRecord.java` | `@@ -0,0 +1,17 @@` | 17 | 0 | U06-007, U06-004 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffShiftRecord.java` | `@@ -1,5 +1,7 @@` | 2 | 0 | U06-007 |
| `backend/src/main/java/com/complex/platform/access/staff/StaffShiftRecord.java` | `@@ -15,6 +17,17 @@ public record StaffShiftRecord(` | 12 | 1 | U06-007 |
| `backend/src/main/java/com/complex/platform/access/staff/dto/StaffEmploymentStatusRequest.java` | `@@ -0,0 +1,5 @@` | 5 | 0 | U06-007, U05-011, U14-006 |
| `backend/src/main/java/com/complex/platform/access/staff/dto/StaffNumericSettingRequest.java` | `@@ -0,0 +1,5 @@` | 5 | 0 | U06-007, U05-011, U14-006 |
| `backend/src/main/java/com/complex/platform/access/staff/dto/StaffPenaltyCapRequest.java` | `@@ -0,0 +1,9 @@` | 9 | 0 | U06-007, U05-004, U11-004, U05-011, U14-006 |
| `backend/src/main/java/com/complex/platform/access/staff/dto/StaffRegistrationRequest.java` | `@@ -1,8 +1,10 @@` | 4 | 2 | U06-007, U05-011, U14-006 |
| `backend/src/main/java/com/complex/platform/access/staff/dto/StaffRegistrationRequest.java` | `@@ -10,8 +12,8 @@ public record StaffRegistrationRequest(` | 2 | 2 | U06-007 |
| `backend/src/main/java/com/complex/platform/access/staff/dto/StaffSelfRegistrationRequest.java` | `@@ -0,0 +1,6 @@` | 6 | 0 | U06-002, U05-003, U06-007, U01-007, U02-002, U14-003, U05-011, U14-006 |
| `backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftBatchResult.java` | `@@ -1,17 +1,13 @@` | 7 | 11 | U06-007 |
| `backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftCoverRequest.java` | `@@ -0,0 +1,6 @@` | 6 | 0 | U06-007, U03-009, U05-011, U14-006, U06-004 |
| `backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftRequest.java` | `@@ -1,10 +1,12 @@` | 3 | 1 | U06-007, U05-011, U14-006 |
| `backend/src/main/java/com/complex/platform/access/staff/dto/StaffShiftRequest.java` | `@@ -12,6 +14,11 @@ public record StaffShiftRequest(` | 6 | 1 | U06-007, U05-011, U14-006 |
| `backend/src/main/java/com/complex/platform/common/ratelimit/RateLimitInterceptor.java` | `@@ -31,6 +31,8 @@ public class RateLimitInterceptor implements HandlerInterceptor {` | 2 | 0 | U07-004 |
| `backend/src/main/java/com/complex/platform/config/SecurityConfig.java` | `@@ -1,5 +1,7 @@` | 2 | 0 | U07-005 |
| `backend/src/main/java/com/complex/platform/config/SecurityConfig.java` | `@@ -116,12 +118,15 @@ public class SecurityConfig {` | 3 | 0 | U07-005, U05-006, U11-006, U14-005 |
| `backend/src/main/java/com/complex/platform/gate/DeviceCredentialInterceptor.java` | `@@ -20,7 +20,9 @@ import org.springframework.web.servlet.HandlerInterceptor;` | 3 | 1 | U07-005, U05-006, U11-006, U14-005 |
| `backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java` | `@@ -1,9 +1,13 @@` | 5 | 1 | U07-005 |
| `backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java` | `@@ -34,8 +38,8 @@ import org.springframework.web.util.WebUtils;` | 2 | 2 | U07-005, U01-001, U02-001, U07-003, U05-006, U11-006, U14-005 |
| `backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java` | `@@ -54,7 +58,9 @@ public class DeviceCredentialVerifier {` | 3 | 1 | U07-005, U01-001, U02-001, U07-003, U05-006, U11-006, U14-005 |
| `backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java` | `@@ -170,17 +176,29 @@ public class DeviceCredentialVerifier {` | 22 | 10 | U07-005 |
| `backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java` | `@@ -202,17 +220,19 @@ public class DeviceCredentialVerifier {` | 6 | 4 | U07-005, U01-001, U02-001, U07-003 |
| `backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java` | `@@ -229,10 +249,27 @@ public class DeviceCredentialVerifier {` | 18 | 1 | U07-005, U01-001, U02-001, U07-003, U01-007, U02-002, U05-003, U06-002, U14-003 |
| `backend/src/main/java/com/complex/platform/gate/DeviceCredentialVerifier.java` | `@@ -246,10 +283,6 @@ public class DeviceCredentialVerifier {` | 0 | 4 | U07-005, U01-007, U02-002, U05-003, U06-002, U14-003, U07-001 |
| `backend/src/main/java/com/complex/platform/gate/TerminalMasterSwitchInterceptor.java` | `@@ -40,7 +40,10 @@ public class TerminalMasterSwitchInterceptor implements HandlerInterceptor {` | 4 | 1 | U07-005, U05-006, U11-006, U14-005 |
| `backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java` | `@@ -1,6 +1,6 @@` | 1 | 1 | U09-001 |
| `backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java` | `@@ -36,6 +36,8 @@ public class SupervisorReviewService {` | 2 | 0 | U09-001, U09-005, U14-008, U16-006 |
| `backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java` | `@@ -47,6 +49,10 @@ public class SupervisorReviewService {` | 4 | 0 | U09-001, U05-004, U11-004, U09-002, U09-003, U09-004, U12-003 |
| `backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java` | `@@ -64,12 +70,14 @@ public class SupervisorReviewService {` | 3 | 1 | U09-001, U05-011, U14-006, U09-005, U14-008, U16-006 |
| `backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java` | `@@ -205,6 +213,12 @@ public class SupervisorReviewService {` | 6 | 0 | U09-001, U05-004, U11-004, U05-009, U11-001, U09-002, U09-003, U09-004, U12-003 |
| `backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java` | `@@ -277,6 +291,7 @@ public class SupervisorReviewService {` | 1 | 0 | U09-001, U05-011, U14-006 |
| `backend/src/main/java/com/complex/platform/governance/review/SupervisorReviewService.java` | `@@ -612,7 +627,8 @@ public class SupervisorReviewService {` | 2 | 1 | U09-001, U09-002 |
| `backend/src/main/java/com/complex/platform/terminal/TerminalDeviceService.java` | `@@ -1,5 +1,7 @@` | 2 | 0 | U07-005, U07-001, U05-011, U14-006 |
| `backend/src/main/java/com/complex/platform/terminal/TerminalDeviceService.java` | `@@ -656,6 +658,14 @@ public class TerminalDeviceService {` | 8 | 0 | U07-005, U07-001, U01-001, U02-001, U07-003, U14-008, U16-004 |
| `backend/src/main/java/com/complex/platform/terminal/TerminalDeviceService.java` | `@@ -671,7 +681,7 @@ public class TerminalDeviceService {` | 1 | 1 | U07-005, U07-001 |
| `backend/src/main/java/com/complex/platform/terminal/dto/DeviceBindRequest.java` | `@@ -13,6 +13,14 @@ public record DeviceBindRequest(` | 9 | 1 | U07-005, U07-001, U05-006, U11-006, U14-005, U05-011, U14-006 |
| `backend/src/main/resources/db/migration/V411__staff_profile_pii_and_self_device_permission.sql` | `@@ -0,0 +1,6 @@` | 6 | 0 | U10-001, U18-004, U05-010 |
| `backend/src/main/resources/db/migration/V412__staff_registration_initiator.sql` | `@@ -0,0 +1,3 @@` | 3 | 0 | U10-001 |
| `backend/src/main/resources/db/migration/V413__staff_policy_settings.sql` | `@@ -0,0 +1,21 @@` | 21 | 0 | U10-001, U05-004, U11-004, U05-005, U06-005, U10-003, U11-005 |
| `backend/src/main/resources/db/migration/V414__staff_profile_encrypted_personal_fields.sql` | `@@ -0,0 +1,4 @@` | 4 | 0 | U10-001 |
| `backend/src/main/resources/db/migration/V415__staff_employment_status.sql` | `@@ -0,0 +1,4 @@` | 4 | 0 | U10-001 |
| `backend/src/main/resources/db/migration/V416__staff_work_phone_receipts.sql` | `@@ -0,0 +1,6 @@` | 6 | 0 | U10-001, U01-002 |
| `backend/src/main/resources/db/migration/V417__staff_shift_revision_approvals.sql` | `@@ -0,0 +1,14 @@` | 14 | 0 | U10-001 |
| `backend/src/main/resources/db/migration/V418__staff_penalty_cap_approval_history.sql` | `@@ -0,0 +1,17 @@` | 17 | 0 | U10-001, U05-004, U11-004, U05-005, U06-005, U10-003, U11-005, U09-003, U09-004, U12-003 |
| `backend/src/main/resources/db/migration/V419__staff_registration_company_routes.sql` | `@@ -0,0 +1,5 @@` | 5 | 0 | U10-001, U05-010, U06-006 |
| `backend/src/main/resources/db/migration/V420__staff_locker_scan_records.sql` | `@@ -0,0 +1,13 @@` | 13 | 0 | U10-001, U05-006, U11-006, U14-005 |
| `backend/src/main/resources/db/migration/V421__staff_locker_scan_tokens.sql` | `@@ -0,0 +1,13 @@` | 13 | 0 | U05-006, U11-006, U14-005 |
| `backend/src/main/resources/db/migration/V422__staff_shift_covers.sql` | `@@ -0,0 +1,13 @@` | 13 | 0 | U06-004 |
| `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -1,6 +1,6 @@` | 1 | 1 | U11-001, U05-011, U14-006 |
| `backend/src/test/java/com/complex/platform/access/AccessServiceTests.java` | `@@ -1444,7 +1444,7 @@ class AccessServiceTests {` | 1 | 1 | U11-001, U05-011, U14-006 |
| `backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java` | `@@ -1,6 +1,6 @@` | 1 | 1 | U07-004, U05-003 |
| `backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java` | `@@ -14,6 +14,7 @@ import com.complex.platform.support.InMemoryAuditEventRepository;` | 1 | 0 | U07-004, U05-003, U05-011, U14-006 |
| `backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java` | `@@ -400,7 +401,7 @@ class M006SecurityRegressionTests {` | 1 | 1 | U07-004, U05-003, U05-011, U14-006 |
| `backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java` | `@@ -451,7 +452,7 @@ class M006SecurityRegressionTests {` | 1 | 1 | U07-004, U05-003 |
| `backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java` | `@@ -462,8 +463,10 @@ class M006SecurityRegressionTests {` | 3 | 1 | U07-004, U05-003, U05-011, U14-006 |
| `backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java` | `@@ -473,7 +476,7 @@ class M006SecurityRegressionTests {` | 1 | 1 | U07-004, U05-003 |
| `backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java` | `@@ -488,6 +491,7 @@ class M006SecurityRegressionTests {` | 1 | 0 | U07-004, U05-003, U05-011, U14-006 |
| `backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java` | `@@ -574,7 +578,7 @@ class M006SecurityRegressionTests {` | 1 | 1 | U07-004, U05-003, U05-011, U14-006 |
| `backend/src/test/java/com/complex/platform/access/M006SecurityRegressionTests.java` | `@@ -613,6 +617,25 @@ class M006SecurityRegressionTests {` | 19 | 0 | U07-004, U05-003, U05-011, U14-006 |
| `backend/src/test/java/com/complex/platform/access/staff/M010RegistrationHttpTests.java` | `@@ -0,0 +1,44 @@` | 44 | 0 | U11-001, U01-001, U02-001, U07-003, U01-007, U02-002, U05-003, U06-002, U14-003, U05-010, U06-006, U05-011, U14-006 |
| `backend/src/test/java/com/complex/platform/access/staff/M010RegressionTests.java` | `@@ -0,0 +1,741 @@` | 741 | 0 | U11-001, U01-002, U01-001, U02-001, U07-003, U01-007, U02-002, U05-003, U06-002, U14-003, U07-001, U03-009, U05-002, U11-003, U05-004, U11-004, U05-005, U06-005, U10-003, U11-005, U05-006, U11-006, U14-005, U05-001, U06-003, U11-002, U05-011, U14-006, U06-004, U09-003, U09-004, U12-003, U12-004, U12-005, U16-006, U10-001, U04-001, U10-002, U14-002, U14-008, U16-004 |
| `backend/src/test/java/com/complex/platform/access/staff/PayrollSafetyJdbcIntegrationTests.java` | `@@ -1,5 +1,7 @@` | 2 | 0 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/PayrollSafetyJdbcIntegrationTests.java` | `@@ -42,8 +44,11 @@ class PayrollSafetyJdbcIntegrationTests {` | 3 | 0 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/PayrollSafetyJdbcIntegrationTests.java` | `@@ -170,10 +175,10 @@ class PayrollSafetyJdbcIntegrationTests {` | 3 | 3 | U11-001, U05-010, U06-006, U18-004 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffAttendanceLocalZoneTests.java` | `@@ -1,5 +1,7 @@` | 2 | 0 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffAttendanceLocalZoneTests.java` | `@@ -50,11 +52,22 @@ class StaffAttendanceLocalZoneTests {` | 14 | 3 | U11-001, U01-001, U02-001, U07-003, U05-011, U14-006 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffAttendanceLocalZoneTests.java` | `@@ -76,7 +89,7 @@ class StaffAttendanceLocalZoneTests {` | 1 | 1 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffAttendanceLocalZoneTests.java` | `@@ -96,7 +109,7 @@ class StaffAttendanceLocalZoneTests {` | 1 | 1 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffAttendanceLocalZoneTests.java` | `@@ -116,31 +129,31 @@ class StaffAttendanceLocalZoneTests {` | 9 | 9 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` | `@@ -1,5 +1,7 @@` | 2 | 0 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` | `@@ -30,7 +32,7 @@ import org.springframework.transaction.annotation.Transactional;` | 1 | 1 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` | `@@ -53,14 +55,26 @@ class StaffJdbcIntegrationTests {` | 12 | 0 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` | `@@ -155,13 +169,12 @@ class StaffJdbcIntegrationTests {` | 3 | 4 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` | `@@ -221,7 +234,8 @@ class StaffJdbcIntegrationTests {` | 2 | 1 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` | `@@ -275,15 +289,13 @@ class StaffJdbcIntegrationTests {` | 6 | 8 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` | `@@ -306,65 +318,61 @@ class StaffJdbcIntegrationTests {` | 47 | 51 | U11-001, U01-001, U02-001, U07-003 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` | `@@ -460,7 +468,7 @@ class StaffJdbcIntegrationTests {` | 1 | 1 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` | `@@ -476,8 +484,8 @@ class StaffJdbcIntegrationTests {` | 2 | 2 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` | `@@ -494,8 +502,7 @@ class StaffJdbcIntegrationTests {` | 1 | 2 | U11-001, U05-002, U11-003 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` | `@@ -512,7 +519,7 @@ class StaffJdbcIntegrationTests {` | 1 | 1 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` | `@@ -530,6 +537,7 @@ class StaffJdbcIntegrationTests {` | 1 | 0 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` | `@@ -623,7 +631,7 @@ class StaffJdbcIntegrationTests {` | 1 | 1 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` | `@@ -632,6 +640,11 @@ class StaffJdbcIntegrationTests {` | 5 | 0 | U11-001, U16-006 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` | `@@ -653,6 +666,7 @@ class StaffJdbcIntegrationTests {` | 1 | 0 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` | `@@ -747,10 +761,10 @@ class StaffJdbcIntegrationTests {` | 3 | 3 | U11-001, U05-010, U06-006, U18-004 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` | `@@ -850,9 +864,9 @@ class StaffJdbcIntegrationTests {` | 2 | 2 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` | `@@ -868,7 +882,7 @@ class StaffJdbcIntegrationTests {` | 1 | 1 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffJdbcIntegrationTests.java` | `@@ -890,21 +904,26 @@ class StaffJdbcIntegrationTests {` | 8 | 3 | U11-001, U16-006 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java` | `@@ -1,5 +1,7 @@` | 2 | 0 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java` | `@@ -32,6 +34,12 @@ class StaffProfileArchiveDeleteJdbcIntegrationTests {` | 6 | 0 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java` | `@@ -50,7 +58,7 @@ class StaffProfileArchiveDeleteJdbcIntegrationTests {` | 1 | 1 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java` | `@@ -81,7 +89,7 @@ class StaffProfileArchiveDeleteJdbcIntegrationTests {` | 1 | 1 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java` | `@@ -89,7 +97,7 @@ class StaffProfileArchiveDeleteJdbcIntegrationTests {` | 1 | 1 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffProfileArchiveDeleteJdbcIntegrationTests.java` | `@@ -97,6 +105,6 @@ class StaffProfileArchiveDeleteJdbcIntegrationTests {` | 1 | 1 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffScopeTests.java` | `@@ -1,5 +1,7 @@` | 2 | 0 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffScopeTests.java` | `@@ -30,9 +32,14 @@ class StaffScopeTests {` | 6 | 1 | U11-001, U05-011, U14-006 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffScopeTests.java` | `@@ -41,8 +48,8 @@ class StaffScopeTests {` | 2 | 2 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffScopeTests.java` | `@@ -51,7 +58,7 @@ class StaffScopeTests {` | 1 | 1 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffScopeTests.java` | `@@ -67,7 +74,7 @@ class StaffScopeTests {` | 2 | 2 | U11-001 |
| `backend/src/test/java/com/complex/platform/access/staff/StaffTestActors.java` | `@@ -0,0 +1,44 @@` | 44 | 0 | U11-001 |
| `backend/src/test/java/com/complex/platform/gate/DeviceCredentialVerifierTests.java` | `@@ -1,5 +1,7 @@` | 2 | 0 | U12-001 |
| `backend/src/test/java/com/complex/platform/gate/DeviceCredentialVerifierTests.java` | `@@ -59,8 +61,7 @@ class DeviceCredentialVerifierTests {` | 1 | 2 | U12-001, U01-001, U02-001, U07-003 |
| `backend/src/test/java/com/complex/platform/gate/DeviceCredentialVerifierTests.java` | `@@ -91,11 +92,20 @@ class DeviceCredentialVerifierTests {` | 9 | 0 | U12-001, U01-001, U02-001, U07-003, U12-004, U12-005 |
| `backend/src/test/java/com/complex/platform/gate/DeviceCredentialVerifierTests.java` | `@@ -108,6 +118,14 @@ class DeviceCredentialVerifierTests {` | 8 | 0 | U12-001, U12-004, U12-005 |
| `backend/src/test/java/com/complex/platform/gate/DeviceCredentialVerifierTests.java` | `@@ -239,6 +257,8 @@ class DeviceCredentialVerifierTests {` | 2 | 0 | U12-001, U01-001, U02-001, U07-003 |
| `backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java` | `@@ -1,3 +1,4 @@` | 1 | 0 | U12-001 |
| `backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java` | `@@ -28,6 +29,8 @@ import org.junit.jupiter.api.Test;` | 2 | 0 | U12-001 |
| `backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java` | `@@ -41,7 +44,7 @@ class SupervisorReviewServiceTests {` | 1 | 1 | U12-001, U05-011, U14-006 |
| `backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java` | `@@ -54,11 +57,12 @@ class SupervisorReviewServiceTests {` | 3 | 2 | U12-001, U05-004, U11-004, U09-002 |
| `backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java` | `@@ -304,7 +308,7 @@ class SupervisorReviewServiceTests {` | 1 | 1 | U12-001, U05-011, U14-006 |
| `backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java` | `@@ -334,7 +338,7 @@ class SupervisorReviewServiceTests {` | 1 | 1 | U12-001, U05-011, U14-006 |
| `backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java` | `@@ -369,7 +373,7 @@ class SupervisorReviewServiceTests {` | 1 | 1 | U12-001, U05-011, U14-006 |
| `backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java` | `@@ -421,7 +425,7 @@ class SupervisorReviewServiceTests {` | 1 | 1 | U12-001, U05-011, U14-006 |
| `backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java` | `@@ -490,6 +494,90 @@ class SupervisorReviewServiceTests {` | 84 | 0 | U12-001, U05-004, U11-004, U09-002, U09-003, U09-004, U12-003, U10-001 |
| `backend/src/test/java/com/complex/platform/governance/review/SupervisorReviewServiceTests.java` | `@@ -787,7 +875,7 @@ class SupervisorReviewServiceTests {` | 1 | 1 | U12-001, U05-011, U14-006 |
| `backend/src/test/java/com/complex/platform/terminal/TerminalDeviceServiceTests.java` | `@@ -635,6 +635,7 @@ class TerminalDeviceServiceTests {` | 1 | 0 | U07-001, U12-001, U05-011, U14-006 |
| `backend/src/test/java/com/complex/platform/terminal/TerminalDeviceServiceTests.java` | `@@ -645,7 +646,8 @@ class TerminalDeviceServiceTests {` | 2 | 1 | U07-001, U12-001 |
| `backend/src/test/java/com/complex/platform/terminal/TerminalDeviceServiceTests.java` | `@@ -1105,13 +1107,13 @@ class TerminalDeviceServiceTests {` | 2 | 2 | U07-001, U12-001, U01-001, U02-001, U07-003 |
| `backend/src/test/java/com/complex/platform/terminal/TerminalDeviceServiceTests.java` | `@@ -1122,7 +1124,7 @@ class TerminalDeviceServiceTests {` | 1 | 1 | U07-001, U12-001, U01-001, U02-001, U07-003 |
| `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md` | `@@ -1,4 +1,4 @@` | 1 | 1 | U13-001 |
| `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md` | `@@ -56,7 +56,7 @@ scope 类型（与盘点 CSV 的 `scope_type` 对齐）：` | 1 | 1 | U13-001, U06-004, U10-001 |
| `docs/domain-ownership-scope-audit-v0.1.csv` | `@@ -1,329 +1,335 @@` | 335 | 329 | U13-001, U18-003, U05-002, U11-003, U05-004, U11-004, U05-005, U06-005, U10-003, U11-005, U05-006, U11-006, U14-005, U05-011, U14-006, U06-004 |
| `docs/openapi/openapi-p0.json` | `@@ -2,7 +2,7 @@` | 1 | 1 | U14-001 |
| `docs/openapi/openapi-p0.json` | `@@ -12057,13 +12057,15 @@` | 6 | 4 | U14-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.json` | `@@ -12101,7 +12103,7 @@` | 1 | 1 | U14-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.json` | `@@ -12242,7 +12244,7 @@` | 1 | 1 | U14-001, U05-006, U11-006, U14-005, U14-003, U15-003 |
| `docs/openapi/openapi-p0.json` | `@@ -29604,7 +29606,7 @@` | 1 | 1 | U14-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.json` | `@@ -29612,7 +29614,7 @@` | 1 | 1 | U14-001, U01-001, U02-001, U07-003, U05-011, U14-006, U16-006, U14-008, U16-004, U15-006 |
| `docs/openapi/openapi-p0.json` | `@@ -29665,8 +29667,13 @@` | 6 | 1 | U14-001 |
| `docs/openapi/openapi-p0.json` | `@@ -29745,7 +29752,7 @@` | 1 | 1 | U14-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.json` | `@@ -29753,7 +29760,7 @@` | 1 | 1 | U14-001, U01-001, U02-001, U07-003, U05-011, U14-006, U16-006, U14-008, U16-004, U15-006 |
| `docs/openapi/openapi-p0.json` | `@@ -29860,7 +29867,7 @@` | 1 | 1 | U14-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.json` | `@@ -29891,12 +29898,11 @@` | 2 | 3 | U14-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.json` | `@@ -29910,7 +29916,8 @@` | 2 | 1 | U14-001, U05-011, U14-006 |
| `docs/openapi/openapi-p0.json` | `@@ -29928,11 +29935,11 @@` | 2 | 2 | U14-001, U01-002 |
| `docs/openapi/openapi-p0.json` | `@@ -29947,7 +29954,7 @@` | 1 | 1 | U14-001, U01-007, U02-002, U05-003, U06-002, U14-003 |
| `docs/openapi/openapi-p0.json` | `@@ -30677,7 +30684,7 @@` | 1 | 1 | U14-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.json` | `@@ -36314,6 +36321,890 @@` | 884 | 0 | U14-001, U01-001, U02-001, U07-003, U01-007, U02-002, U05-003, U06-002, U14-003, U03-009, U05-004, U11-004, U05-005, U06-005, U10-003, U11-005, U05-006, U11-006, U14-005, U05-010, U06-006, U05-011, U14-006, U06-004, U09-003, U09-004, U12-003, U16-006, U14-008, U16-004, U15-003, U15-006 |
| `docs/openapi/openapi-p0.json` | `@@ -37847,10 +38738,10 @@` | 3 | 3 | U14-001, U05-011, U14-006 |
| `docs/openapi/openapi-p0.json` | `@@ -40551,6 +41442,11 @@` | 5 | 0 | U14-001, U01-001, U02-001, U07-003, U07-001 |
| `docs/openapi/openapi-p0.json` | `@@ -45658,12 +46554,13 @@` | 3 | 2 | U14-001, U05-011, U14-006 |
| `docs/openapi/openapi-p0.json` | `@@ -45683,7 +46580,7 @@` | 1 | 1 | U14-001 |
| `docs/openapi/openapi-p0.json` | `@@ -45691,14 +46588,14 @@` | 4 | 4 | U14-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -1,8 +1,9 @@` | 3 | 2 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -31,902 +32,6 @@ tags:` | 0 | 896 | U15-001, U01-001, U02-001, U07-003, U05-002, U11-003, U05-004, U11-004, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -1287,7 +392,8 @@ paths:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -1350,7 +456,8 @@ paths:` | 2 | 1 | U15-001, U05-011, U14-006, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -1371,7 +478,8 @@ paths:` | 2 | 1 | U15-001, U05-011, U14-006, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -1401,7 +509,8 @@ paths:` | 2 | 1 | U15-001, U05-011, U14-006, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -1434,7 +543,8 @@ paths:` | 2 | 1 | U15-001, U05-011, U14-006, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -1468,7 +578,8 @@ paths:` | 2 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -1510,7 +621,8 @@ paths:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -2148,7 +1260,10 @@ paths:` | 4 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -2210,7 +1325,9 @@ paths:` | 3 | 1 | U15-001, U05-002, U11-003, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -2282,7 +1399,8 @@ paths:` | 2 | 1 | U15-001, U05-011, U14-006, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -2481,7 +1599,8 @@ paths:` | 2 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -2528,7 +1647,12 @@ paths:` | 6 | 1 | U15-001, U14-008, U16-004, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -2698,7 +1822,8 @@ paths:` | 2 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -5155,7 +4280,8 @@ paths:` | 2 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -5182,7 +4308,8 @@ paths:` | 2 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -5212,7 +4339,10 @@ paths:` | 4 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -6109,7 +5239,8 @@ paths:` | 2 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -6633,7 +5764,8 @@ paths:` | 2 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -6656,7 +5788,8 @@ paths:` | 2 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -6686,7 +5819,8 @@ paths:` | 2 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -6726,7 +5860,8 @@ paths:` | 2 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -6766,7 +5901,8 @@ paths:` | 2 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -7166,6 +6302,53 @@ paths:` | 47 | 0 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -7231,15 +6414,17 @@ paths:` | 5 | 3 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -7253,9 +6438,12 @@ paths:` | 6 | 3 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -7278,10 +6466,14 @@ paths:` | 8 | 4 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -7304,10 +6496,14 @@ paths:` | 8 | 4 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -7335,7 +6531,8 @@ paths:` | 2 | 1 | U15-001, U01-001, U02-001, U07-003, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -7374,7 +6571,9 @@ paths:` | 3 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -7425,7 +6624,8 @@ paths:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -7713,7 +6913,8 @@ paths:` | 2 | 1 | U15-001, U05-011, U14-006, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -7734,7 +6935,8 @@ paths:` | 2 | 1 | U15-001, U05-011, U14-006, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -7762,7 +6964,8 @@ paths:` | 2 | 1 | U15-001, U05-011, U14-006, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -7795,7 +6998,8 @@ paths:` | 2 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -8006,7 +7210,8 @@ paths:` | 2 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -8122,7 +7327,10 @@ paths:` | 4 | 1 | U15-001, U05-006, U11-006, U14-005 |
| `docs/openapi/openapi-p0.yaml` | `@@ -8445,68 +7653,6 @@ paths:` | 0 | 62 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -8744,7 +7890,8 @@ paths:` | 2 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -8843,53 +7990,6 @@ paths:` | 0 | 47 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -9001,7 +8101,8 @@ paths:` | 2 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -9039,7 +8140,8 @@ paths:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -9055,7 +8157,8 @@ paths:` | 2 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -9565,13 +8668,15 @@ paths:` | 5 | 3 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -9597,7 +8702,7 @@ paths:` | 1 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -9697,7 +8802,7 @@ paths:` | 1 | 1 | U15-001, U05-006, U11-006, U14-005, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -9730,7 +8835,8 @@ paths:` | 2 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -9766,7 +8872,8 @@ paths:` | 2 | 1 | U15-001, U14-008, U16-004, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -9895,7 +9002,9 @@ paths:` | 3 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -9957,7 +9066,9 @@ paths:` | 3 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -10024,8 +9135,11 @@ paths:` | 5 | 2 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -11256,7 +10370,8 @@ paths:` | 2 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -11310,7 +10425,8 @@ paths:` | 2 | 1 | U15-001, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -13247,49 +12363,6 @@ paths:` | 0 | 43 | U15-001, U14-008, U16-004, U14-003, U15-003 |
| `docs/openapi/openapi-p0.yaml` | `@@ -16221,7 +15294,8 @@ paths:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -16366,303 +15440,800 @@ paths:` | 679 | 182 | U15-001, U01-001, U02-001, U07-003, U05-006, U11-006, U14-005, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -16670,58 +16241,58 @@ paths:` | 33 | 33 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -16729,30 +16300,60 @@ paths:` | 43 | 13 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -16764,26 +16365,21 @@ paths:` | 10 | 15 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -16794,48 +16390,69 @@ paths:` | 44 | 23 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -16843,153 +16460,144 @@ paths:` | 73 | 82 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -16997,120 +16605,124 @@ paths:` | 73 | 69 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17118,48 +16730,21 @@ paths:` | 8 | 35 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17167,25 +16752,80 @@ paths:` | 64 | 9 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17193,32 +16833,21 @@ paths:` | 8 | 19 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17226,25 +16855,26 @@ paths:` | 10 | 9 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17252,34 +16882,22 @@ paths:` | 9 | 21 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17287,72 +16905,75 @@ paths:` | 48 | 45 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17360,25 +16981,34 @@ paths:` | 17 | 8 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17386,22 +17016,31 @@ paths:` | 17 | 8 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17415,20 +17054,28 @@ paths:` | 15 | 7 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17436,30 +17083,30 @@ paths:` | 10 | 10 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17473,30 +17120,61 @@ paths:` | 41 | 10 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17510,42 +17188,61 @@ paths:` | 34 | 15 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17559,31 +17256,31 @@ paths:` | 11 | 11 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17597,31 +17294,60 @@ paths:` | 40 | 11 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17635,42 +17361,28 @@ paths:` | 15 | 29 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17684,30 +17396,22 @@ paths:` | 8 | 16 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17721,59 +17425,56 @@ paths:` | 24 | 27 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17787,43 +17488,67 @@ paths:` | 37 | 13 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17837,31 +17562,31 @@ paths:` | 10 | 10 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17875,21 +17600,21 @@ paths:` | 7 | 7 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17899,7 +17624,7 @@ paths:` | 1 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17913,50 +17638,59 @@ paths:` | 27 | 18 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17966,7 +17700,7 @@ paths:` | 1 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -17980,64 +17714,51 @@ paths:` | 18 | 31 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18046,133 +17767,99 @@ paths:` | 44 | 78 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18181,36 +17868,42 @@ paths:` | 19 | 13 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18219,65 +17912,51 @@ paths:` | 18 | 32 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18286,62 +17965,93 @@ paths:` | 53 | 22 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18350,33 +18060,37 @@ paths:` | 17 | 13 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18385,26 +18099,37 @@ paths:` | 18 | 7 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18413,33 +18138,37 @@ paths:` | 17 | 13 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18448,36 +18177,37 @@ paths:` | 12 | 11 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18486,36 +18216,37 @@ paths:` | 12 | 11 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18524,36 +18255,37 @@ paths:` | 12 | 11 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18562,36 +18294,35 @@ paths:` | 13 | 14 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18600,36 +18331,29 @@ paths:` | 10 | 17 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18638,19 +18362,19 @@ paths:` | 6 | 6 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18664,18 +18388,18 @@ paths:` | 6 | 6 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18691,19 +18415,19 @@ paths:` | 6 | 6 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18714,30 +18438,21 @@ paths:` | 7 | 16 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18753,74 +18468,63 @@ paths:` | 20 | 31 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18836,20 +18540,28 @@ paths:` | 14 | 6 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18862,18 +18574,26 @@ paths:` | 14 | 6 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18889,86 +18609,161 @@ paths:` | 104 | 29 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -18984,69 +18779,52 @@ paths:` | 22 | 39 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -19062,30 +18840,30 @@ paths:` | 10 | 10 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -19101,69 +18879,62 @@ paths:` | 26 | 33 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -19179,30 +18950,22 @@ paths:` | 8 | 16 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -19218,28 +18981,22 @@ paths:` | 10 | 16 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -19255,22 +19012,22 @@ paths:` | 9 | 9 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -19286,19 +19043,19 @@ paths:` | 8 | 8 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -19311,19 +19068,19 @@ paths:` | 8 | 8 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -19339,44 +19096,30 @@ paths:` | 18 | 32 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -19392,63 +19135,67 @@ paths:` | 27 | 23 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -19464,28 +19211,29 @@ paths:` | 15 | 14 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -19497,27 +19245,19 @@ paths:` | 8 | 16 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -19533,30 +19273,31 @@ paths:` | 12 | 11 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -19572,30 +19313,75 @@ paths:` | 62 | 17 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -19611,44 +19397,58 @@ paths:` | 33 | 19 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -19664,67 +19464,148 @@ paths:` | 113 | 32 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -19734,138 +19615,100 @@ paths:` | 36 | 74 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -19874,60 +19717,51 @@ paths:` | 21 | 30 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -19936,29 +19770,34 @@ paths:` | 18 | 13 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -19967,197 +19806,219 @@ paths:` | 131 | 109 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -20165,395 +20026,345 @@ paths:` | 213 | 263 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -20561,126 +20372,175 @@ paths:` | 114 | 65 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -20688,635 +20548,732 @@ paths:` | 484 | 387 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U16-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -21324,98 +21281,110 @@ paths:` | 71 | 59 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U16-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -21424,46 +21393,61 @@ paths:` | 36 | 21 | U15-001, U01-002, U01-001, U02-001, U07-003, U01-007, U02-002, U05-003, U06-002, U14-003, U05-011, U14-006, U14-008, U16-004, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -21474,78 +21458,49 @@ paths:` | 20 | 49 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -21554,215 +21509,343 @@ paths:` | 260 | 132 | U15-001, U01-001, U02-001, U07-003, U03-009, U05-002, U11-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -21773,73 +21856,81 @@ paths:` | 38 | 30 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -21849,26 +21940,22 @@ paths:` | 11 | 15 | U15-001, U05-011, U14-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -21876,109 +21963,82 @@ paths:` | 40 | 67 | U15-001, U01-001, U02-001, U07-003, U05-004, U11-004, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -21986,18 +22046,59 @@ paths:` | 45 | 4 | U15-001, U05-011, U14-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -22008,104 +22109,110 @@ paths:` | 60 | 54 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -22115,39 +22222,30 @@ paths:` | 14 | 23 | U15-001, U05-011, U14-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -22158,22 +22256,23 @@ paths:` | 8 | 7 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -22181,12 +22280,16 @@ paths:` | 6 | 2 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -22195,21 +22298,49 @@ paths:` | 37 | 9 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -22219,60 +22350,11 @@ paths:` | 0 | 49 | U15-001, U05-011, U14-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -22280,47 +22362,80 @@ paths:` | 53 | 20 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -22330,37 +22445,16 @@ paths:` | 4 | 25 | U15-001, U01-007, U02-002, U05-003, U06-002, U14-003, U05-011, U14-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -22368,74 +22462,100 @@ paths:` | 54 | 28 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -22445,21 +22565,15 @@ paths:` | 3 | 9 | U15-001, U05-011, U14-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -22467,23 +22581,47 @@ paths:` | 32 | 8 | U15-001, U01-001, U02-001, U07-003, U03-009, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -22493,39 +22631,16 @@ paths:` | 4 | 27 | U15-001, U03-009, U05-011, U14-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -22536,62 +22651,42 @@ paths:` | 33 | 53 | U15-001, U05-002, U11-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -22599,43 +22694,33 @@ paths:` | 20 | 30 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -22643,122 +22728,86 @@ paths:` | 44 | 80 | U15-001, U01-001, U02-001, U07-003, U03-009, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -22766,109 +22815,196 @@ paths:` | 140 | 53 | U15-001, U01-002, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -22878,58 +23014,136 @@ paths:` | 105 | 27 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -22938,143 +23152,105 @@ paths:` | 66 | 104 | U15-001, U01-001, U02-001, U07-003, U05-004, U11-004, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23082,84 +23258,54 @@ paths:` | 30 | 60 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23167,105 +23313,94 @@ paths:` | 53 | 64 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23275,23 +23410,10 @@ paths:` | 2 | 15 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23299,20 +23421,25 @@ paths:` | 18 | 13 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23322,50 +23449,80 @@ paths:` | 65 | 35 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23373,27 +23530,26 @@ paths:` | 13 | 14 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23401,39 +23557,38 @@ paths:` | 19 | 20 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23441,25 +23596,12 @@ paths:` | 4 | 17 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23467,24 +23609,29 @@ paths:` | 22 | 17 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23494,64 +23641,79 @@ paths:` | 53 | 38 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23559,45 +23721,99 @@ paths:` | 75 | 21 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23607,31 +23823,69 @@ paths:` | 54 | 16 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23639,85 +23893,171 @@ paths:` | 126 | 40 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23731,30 +24071,52 @@ paths:` | 37 | 15 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23764,76 +24126,95 @@ paths:` | 60 | 41 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23841,25 +24222,22 @@ paths:` | 5 | 8 | U15-001, U05-011, U14-006, U14-008, U16-004, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23881,11 +24259,11 @@ paths:` | 2 | 2 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23894,8 +24272,8 @@ paths:` | 2 | 2 | U15-001, U05-011, U14-006, U14-008, U16-004, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23928,11 +24306,11 @@ paths:` | 2 | 2 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23941,8 +24319,8 @@ paths:` | 2 | 2 | U15-001, U05-011, U14-006, U14-008, U16-004, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23961,11 +24339,11 @@ paths:` | 2 | 2 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23974,8 +24352,8 @@ paths:` | 2 | 2 | U15-001, U05-011, U14-006, U14-008, U16-004, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -23994,11 +24372,11 @@ paths:` | 2 | 2 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24007,8 +24385,8 @@ paths:` | 2 | 2 | U15-001, U05-011, U14-006, U14-008, U16-004, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24017,11 +24395,11 @@ paths:` | 2 | 2 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24030,8 +24408,8 @@ paths:` | 2 | 2 | U15-001, U05-011, U14-006, U14-008, U16-004, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24040,89 +24418,332 @@ paths:` | 291 | 48 | U15-001, U05-002, U11-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24130,36 +24751,42 @@ paths:` | 25 | 19 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24167,51 +24794,49 @@ paths:` | 35 | 37 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24219,33 +24844,48 @@ paths:` | 36 | 21 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24253,25 +24893,25 @@ paths:` | 11 | 11 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24279,31 +24919,33 @@ paths:` | 18 | 16 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24311,38 +24953,59 @@ paths:` | 36 | 15 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24350,82 +25013,25 @@ paths:` | 10 | 67 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24433,65 +25039,38 @@ paths:` | 18 | 45 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24505,36 +25084,41 @@ paths:` | 28 | 23 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24548,34 +25132,82 @@ paths:` | 64 | 16 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24583,40 +25215,57 @@ paths:` | 39 | 22 | U15-001, U05-004, U11-004, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24624,69 +25273,32 @@ paths:` | 13 | 50 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24694,29 +25306,44 @@ paths:` | 30 | 15 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24726,69 +25353,101 @@ paths:` | 72 | 40 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24796,25 +25455,32 @@ paths:` | 20 | 13 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24822,104 +25488,133 @@ paths:` | 79 | 50 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24933,24 +25628,21 @@ paths:` | 10 | 13 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24958,9 +25650,29 @@ paths:` | 22 | 2 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -24974,52 +25686,53 @@ paths:` | 41 | 40 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -25027,30 +25740,54 @@ paths:` | 39 | 15 | U15-001, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -25058,51 +25795,51 @@ paths:` | 25 | 25 | U15-001, U03-008, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -25112,94 +25849,150 @@ paths:` | 99 | 43 | U15-001, U01-002, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -25208,31 +26001,30 @@ paths:` | 18 | 19 | U15-001, U01-001, U02-001, U07-003, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -25241,214 +26033,355 @@ paths:` | 287 | 146 | U15-001, U01-001, U02-001, U07-003, U01-007, U02-002, U05-003, U06-002, U14-003, U03-009, U05-004, U11-004, U05-005, U06-005, U10-003, U11-005, U05-010, U06-006, U05-011, U14-006, U09-003, U09-004, U12-003, U16-006, U14-008, U16-004, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -25457,56 +26390,42 @@ paths:` | 25 | 39 | U15-001, U05-006, U11-006, U14-005, U05-011, U14-006, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -25515,227 +26434,185 @@ paths:` | 122 | 164 | U15-001, U01-001, U02-001, U07-003, U03-008, U03-009, U05-006, U11-006, U14-005, U05-011, U14-006, U06-004, U14-008, U16-004, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -25955,15 +26832,6 @@ components:` | 0 | 9 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -26127,18 +26995,6 @@ components:` | 0 | 12 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -26169,12 +27025,6 @@ components:` | 0 | 6 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -26233,8 +27083,8 @@ components:` | 2 | 2 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -26783,10 +27633,10 @@ components:` | 2 | 2 | U15-001, U05-011, U14-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -27448,6 +28298,33 @@ components:` | 27 | 0 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -27499,21 +28376,6 @@ components:` | 0 | 15 | U15-001, U03-009 |
| `docs/openapi/openapi-p0.yaml` | `@@ -27647,7 +28509,8 @@ components:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -28253,7 +29116,8 @@ components:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -28691,6 +29555,10 @@ components:` | 4 | 0 | U15-001, U01-001, U02-001, U07-003, U07-001, U05-006, U11-006, U14-005 |
| `docs/openapi/openapi-p0.yaml` | `@@ -28774,56 +29642,6 @@ components:` | 0 | 50 | U15-001, U05-006, U11-006, U14-005 |
| `docs/openapi/openapi-p0.yaml` | `@@ -29027,41 +29845,6 @@ components:` | 0 | 35 | U15-001, U05-011, U14-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -29110,11 +29893,6 @@ components:` | 0 | 5 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -29157,6 +29935,11 @@ components:` | 5 | 0 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -29232,7 +30015,8 @@ components:` | 2 | 1 | U15-001, U05-006, U11-006, U14-005 |
| `docs/openapi/openapi-p0.yaml` | `@@ -29293,7 +30077,8 @@ components:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -29386,7 +30171,8 @@ components:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -29815,7 +30601,8 @@ components:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -30058,7 +30845,8 @@ components:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -31828,7 +32616,8 @@ components:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -31917,7 +32706,8 @@ components:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -31952,7 +32742,8 @@ components:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -32034,7 +32825,8 @@ components:` | 2 | 1 | U15-001, U05-011, U14-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -32057,7 +32849,8 @@ components:` | 2 | 1 | U15-001, U05-011, U14-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -32802,12 +33595,13 @@ components:` | 2 | 1 | U15-001, U05-011, U14-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -32823,19 +33617,19 @@ components:` | 5 | 5 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -33709,7 +34503,8 @@ components:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -33796,7 +34591,8 @@ components:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -33822,7 +34618,9 @@ components:` | 3 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -33848,7 +34646,8 @@ components:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -33886,7 +34685,9 @@ components:` | 3 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -33933,7 +34734,9 @@ components:` | 3 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -33980,7 +34783,8 @@ components:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -34001,7 +34805,8 @@ components:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -34037,7 +34842,8 @@ components:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -34060,7 +34866,8 @@ components:` | 2 | 1 | U15-001, U05-011, U14-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -34069,7 +34876,8 @@ components:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -34097,7 +34905,8 @@ components:` | 2 | 1 | U15-001, U05-011, U14-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -34243,7 +35052,8 @@ components:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -34262,7 +35072,8 @@ components:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -34293,7 +35104,8 @@ components:` | 2 | 1 | U15-001 |
| `docs/openapi/openapi-p0.yaml` | `@@ -34388,7 +35200,9 @@ components:` | 3 | 1 | U15-001, U05-011, U14-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -36175,7 +36989,8 @@ components:` | 2 | 1 | U15-001, U05-011, U14-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -36281,7 +37096,8 @@ components:` | 2 | 1 | U15-001, U05-011, U14-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -36323,7 +37139,8 @@ components:` | 2 | 1 | U15-001, U05-011, U14-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -36454,13 +37271,116 @@ components:` | 105 | 2 | U15-001, U03-009, U05-011, U14-006, U14-003, U15-003, U15-006 |
| `docs/openapi/openapi-p0.yaml` | `@@ -36469,7 +37389,8 @@ x-contract-hardening:` | 2 | 1 | U15-001 |
| `docs/p0-openapi-contract-closure-appendix-v0.1.md` | `@@ -1,4 +1,4 @@` | 1 | 1 | U16-001 |
| `docs/p0-openapi-contract-closure-appendix-v0.1.md` | `@@ -14,11 +14,11 @@` | 5 | 5 | U16-001 |
| `docs/p0-openapi-contract-closure-appendix-v0.1.md` | `@@ -277,7 +277,7 @@` | 1 | 1 | U16-001 |
| `docs/p0-openapi-contract-closure-appendix-v0.1.md` | `@@ -286,7 +286,7 @@` | 1 | 1 | U16-001 |
| `docs/p0-openapi-contract-closure-appendix-v0.1.md` | `@@ -312,12 +312,13 @@` | 4 | 3 | U16-001, U05-002, U11-003, U05-004, U11-004, U05-006, U11-006, U14-005, U06-004 |
| `docs/p0-openapi-contract-closure-appendix-v0.1.md` | `@@ -1556,57 +1557,81 @@` | 64 | 40 | U16-001, U05-002, U11-003, U05-004, U11-004, U05-006, U11-006, U14-005, U06-004, U16-006, U17-009, U14-008, U16-004 |
| `docs/p0-openapi-contract-closure-appendix-v0.1.md` | `@@ -1955,7 +1980,7 @@` | 1 | 1 | U16-001, U01-001, U02-001, U07-003, U14-008, U16-004 |
| `docs/p0-openapi-contract-closure-appendix-v0.1.md` | `@@ -2272,8 +2297,6 @@` | 0 | 2 | U16-001, U03-009, U05-002, U11-003, U14-008, U16-004 |
| `docs/p0-openapi-contract-closure-appendix-v0.1.md` | `@@ -2281,14 +2304,12 @@` | 0 | 2 | U16-001, U01-001, U02-001, U07-003, U14-008, U16-004 |
| `docs/p0-openapi-contract-closure-appendix-v0.1.md` | `@@ -2357,13 +2378,8 @@` | 0 | 5 | U16-001, U01-001, U02-001, U07-003, U14-008, U16-004 |
| `docs/p0-openapi-contract-closure-appendix-v0.1.md` | `@@ -2371,10 +2387,6 @@` | 0 | 4 | U16-001, U01-001, U02-001, U07-003, U14-008, U16-004 |
| `docs/p0-openapi-contract-closure-appendix-v0.1.md` | `@@ -2383,9 +2395,7 @@` | 0 | 2 | U16-001, U01-001, U02-001, U07-003, U14-008, U16-004 |
| `docs/p0-openapi-contract-closure-appendix-v0.1.md` | `@@ -2399,7 +2409,6 @@` | 0 | 1 | U16-001, U01-001, U02-001, U07-003, U14-008, U16-004 |
| `docs/p0-openapi-contract-closure-appendix-v0.1.md` | `@@ -2409,7 +2418,6 @@` | 0 | 1 | U16-001, U01-001, U02-001, U07-003, U14-008, U16-004 |
| `docs/p0-openapi-contract-closure-appendix-v0.1.md` | `@@ -2443,7 +2451,6 @@` | 0 | 1 | U16-001, U01-001, U02-001, U07-003, U14-008, U16-004 |
| `docs/p0-openapi-contract-closure-appendix-v0.1.md` | `@@ -2451,11 +2458,41 @@` | 32 | 2 | U16-001, U01-001, U02-001, U07-003, U03-009, U05-002, U11-003, U05-004, U11-004, U05-006, U11-006, U14-005, U06-004, U16-006, U17-009, U14-008, U16-004 |
| `docs/p0-openapi-contract-closure-appendix-v0.1.md` | `@@ -2577,8 +2614,4 @@ Given 文档中仍保留 `/api/admin/*`、`/api/mobile/*`、`/api/integration/*`` | 1 | 5 | U16-001, U05-006, U11-006, U14-005, U17-007, U17-008, U18-002, U16-007, U14-003, U15-003 |
| `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` | `@@ -1,4 +1,4 @@` | 1 | 1 | U17-001 |
| `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` | `@@ -17,7 +17,7 @@` | 1 | 1 | U17-001 |
| `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` | `@@ -303,7 +303,7 @@` | 1 | 1 | U17-001, U01-001, U02-001, U07-003, U14-008, U16-004 |
| `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` | `@@ -625,10 +625,10 @@` | 3 | 3 | U17-001, U01-001, U02-001, U07-003, U16-006, U14-008, U16-004 |
| `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` | `@@ -741,6 +741,18 @@` | 12 | 0 | U17-001, U01-001, U02-001, U07-003, U05-004, U11-004, U05-006, U11-006, U14-005, U06-004, U16-006, U14-008, U16-004 |
| `docs/p0-openapi-operation-acceptance-matrix-v0.1.md` | `@@ -799,14 +811,4 @@ Given Java Spring Boot 代码完成，When 通过 springdoc 生成运行时 Open` | 1 | 11 | U17-001, U17-007, U17-008, U18-002, U16-007, U14-003, U15-003 |
| `docs/production-readiness-evidence/security-baseline-latest.json` | `@@ -1,13 +1,14 @@` | 5 | 4 | U13-001, U05-006, U11-006, U14-005, U06-004, U13-010, U04-001, U10-002, U14-002 |
| `docs/r5-spec-T1-staff-tables-dev-design-20260618.md` | `@@ -1,10 +1,19 @@` | 12 | 3 | U13-001, U01-001, U02-001, U07-003, U05-002, U11-003, U05-004, U11-004, U05-006, U11-006, U14-005, U18-004, U05-010, U10-001 |
| `docs/r5-spec-T1-staff-tables-dev-design-20260618.md` | `@@ -19,9 +28,9 @@ CREATE TABLE IF NOT EXISTS staff_profiles (` | 3 | 3 | U13-001 |
| `docs/r5-spec-T1-staff-tables-dev-design-20260618.md` | `@@ -85,7 +94,7 @@ CREATE INDEX IF NOT EXISTS idx_employee_notices_staff ON employee_notices(staff_` | 1 | 1 | U13-001, U14-008, U16-004 |
| `docs/r5-spec-T1-staff-tables-dev-design-20260618.md` | `@@ -102,7 +111,7 @@ CREATE INDEX IF NOT EXISTS idx_employee_notices_staff ON employee_notices(staff_` | 1 | 1 | U13-001, U07-001 |
| `docs/r5-spec-T1-staff-tables-dev-design-20260618.md` | `@@ -118,10 +127,18 @@ CREATE INDEX IF NOT EXISTS idx_employee_notices_staff ON employee_notices(staff_` | 10 | 2 | U13-001, U05-006, U11-006, U14-005, U06-004 |
| `docs/r5-spec-T1-staff-tables-dev-design-20260618.md` | `@@ -167,7 +184,7 @@ CREATE INDEX IF NOT EXISTS idx_employee_notices_staff ON employee_notices(staff_` | 1 | 1 | U13-001 |
| `docs/r5-spec-T9-staff-device-android-dev-design-20260618.md` | `@@ -1,18 +1,18 @@` | 5 | 5 | U13-001, U01-001, U02-001, U07-003, U01-007, U02-002, U05-003, U06-002, U14-003 |
| `docs/r5-spec-T9-staff-device-android-dev-design-20260618.md` | `@@ -20,15 +20,25 @@` | 12 | 2 | U13-001, U01-007, U02-002, U05-003, U06-002, U14-003, U07-001 |
| `security-scans/findings-register-v1.md` | `@@ -3,8 +3,8 @@` | 2 | 2 | U13-001 |
| `security-scans/formal-security-baseline-v1.md` | `@@ -3,8 +3,8 @@` | 2 | 2 | U13-001 |
| `security-scans/formal-security-baseline-v1.md` | `@@ -39,10 +39,10 @@` | 3 | 3 | U13-001 |
| `security-scans/formal-security-baseline-v1.md` | `@@ -57,25 +57,25 @@` | 9 | 9 | U13-001, U01-001, U02-001, U07-003, U05-006, U11-006, U14-005 |
| `tools/generate-domain-ownership-scope-audit.mjs` | `@@ -1,3 +1,4 @@` | 1 | 0 | U18-001, U18-003 |
| `tools/generate-domain-ownership-scope-audit.mjs` | `@@ -19,6 +20,10 @@ const directColumns = [` | 4 | 0 | U18-001, U18-003, U05-006, U11-006, U14-005, U06-004 |
| `tools/generate-domain-ownership-scope-audit.mjs` | `@@ -65,6 +70,7 @@ const globalTables = new Set([` | 1 | 0 | U18-001, U18-003, U05-004, U11-004 |
| `tools/generate-domain-ownership-scope-audit.mjs` | `@@ -141,6 +147,11 @@ const ANALYTICS_SCOPE_OVERRIDES = new Map([` | 5 | 0 | U18-001, U18-003, U05-004, U11-004, U05-005, U06-005, U10-003, U11-005 |
| `tools/generate-domain-ownership-scope-audit.mjs` | `@@ -189,10 +200,11 @@ function buildCsv() {` | 2 | 1 | U18-001, U18-003 |
| `tools/generate-formal-security-baseline.mjs` | `@@ -10,6 +10,8 @@ const paths = {` | 2 | 0 | U13-010 |
| `tools/generate-formal-security-baseline.mjs` | `@@ -107,6 +109,7 @@ const openapi = JSON.parse(openapiText);` | 1 | 0 | U13-010 |
| `tools/generate-formal-security-baseline.mjs` | `@@ -190,7 +193,7 @@ const sourceFindings = {` | 1 | 1 | U13-010 |
| `tools/generate-openapi-contract-closure-appendix.mjs` | `@@ -1,3 +1,4 @@` | 1 | 0 | U17-007 |
| `tools/generate-openapi-contract-closure-appendix.mjs` | `@@ -61,6 +62,7 @@ function taskGroup(tag, operationId) {` | 1 | 0 | U17-007, U17-009 |
| `tools/generate-openapi-contract-closure-appendix.mjs` | `@@ -170,7 +172,9 @@ const exampleRequestBodies = Array.isArray(exampleCoverage.request_bodies)` | 3 | 1 | U17-007, U17-008, U18-002, U16-007 |
| `tools/generate-openapi-contract-closure-appendix.mjs` | `@@ -264,11 +268,7 @@ Given 文档中仍保留 \`/api/admin/*\`、\`/api/mobile/*\`、\`/api/integrati` | 1 | 5 | U17-007, U05-006, U11-006, U14-005, U17-008, U18-002, U16-007, U14-003, U15-003 |
| `tools/generate-openapi-operation-matrix.mjs` | `@@ -1,3 +1,4 @@` | 1 | 0 | U18-002, U17-008 |
| `tools/generate-openapi-operation-matrix.mjs` | `@@ -79,7 +80,9 @@ const tableRows = rows` | 3 | 1 | U18-002, U17-008, U17-007, U16-007 |
| `tools/generate-openapi-operation-matrix.mjs` | `@@ -161,17 +164,7 @@ Given Java Spring Boot 代码完成，When 通过 springdoc 生成运行时 Open` | 1 | 11 | U18-002, U17-008, U17-007, U16-007, U14-003, U15-003 |
| `tools/verify-pii-encryption.mjs` | `@@ -1,4 +1,5 @@` | 1 | 0 | U18-004, U05-010 |
| `tools/verify-pii-encryption.mjs` | `@@ -10,6 +11,9 @@ import { readFileSync } from "node:fs";` | 3 | 0 | U18-004, U05-010 |


（表由本席从两 tree 的 `git diff --binary` 独立解析生成，不复制席甲表。）
