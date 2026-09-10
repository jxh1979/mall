CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U-FIX B5-r2 修复后全量审计 · 席甲完整报告
版本：V0.01

- 席甲：Claude Fable 5.1（作者自审票）。步骤：B5-r2（合成模块 U-FIX 的 B3 第 2 次整批修复后的整模块全量三线重审，不只看 diff）。2026-09-10T19:59:03Z
- 对象：worktree `/home/james/wt/ufix-audit-20260910` after tree `c0ec9b7ab6bc38f8197830b7283af4dc2356b61b`（仓库外临时索引复核，探针开头再次实算同值）相对 main 445a49f4 的 85 路径全文及其上下游；三份业务 HTML 取 main 正本（worktree 三份与 main 逐字节相同）。判据：U01–U18 各段映射行并集（criteria-lock V0.11，锁值见首行）。
- 前置：B5-r1 Grok FAIL（U01-007 未闭合 + 无 ID 两条）→ 回退 B2 双 PASS 定稿 U01-016/U13-011（findings V1.83）→ B3-r2 只修 U01-007/U01-016/U13-011（after e3fd14b6 → c0ec9b7a，3 文件 +61/−9）→ B4-r2 双 PASS（modules/U-FIX-b4-r2-grok-raw.md V0.01，30715bytes SHA640b69f403cac8d22baab11e3f6f4b5504b6f5ed3605774fbdaf023c98fcb166，总 PASS；modules/U-FIX-b4-r2-self-raw.md 25355bytes SHA34c18ef3389d2d3faa2bb2a2d4fd441aad2c346e8b0e7968c71411c988193fef）；B3 累计 2。
- 证据：席甲只读探针 `/tmp/ufix_b5_probes.sh` → `/tmp/ufix_b5_run.log`（B3-r2 后重跑，全文附 §4）；B5-r1 已核的后端全量/web/JVM/Android/门禁证据见 modules/U-FIX-b5-r1-self-raw.md §4 与 Grok B5-r1 §7（B3-r2 未改后端/web/迁移/契约，只改 android-app 两文件与 T1）；本轮 worktree 复跑 Android `compileDebugKotlin` EXIT 0、JVM 30/30、`verify:predeploy` EXIT 0（B4-r2 raw §3.A）。本报告落盘前未读取 `U-FIX-b5-r2-grok-raw.md` 任何正文。

## 1. 三线方法

- 第一性原理：B5-r1 被否决的根因是「删除入口 ≠ 在私人手机端以 Keystore 提交并绑定」。本轮追问：私人手机用户能否在本 APK 端侧完成 HTML PGM L579/BSM L935 的「安装时生成并绑定设备标识」；工作手机应用是否仍无该入口；注册后身份是否只声明一次并可签名；文案是否指向实际入口；T1 编号是否与迁移目录一致。其余段追问 B3-r2 是否波及（diff 只含三文件，未波及）。
- 顺从式：各段判据逐项对照修复后全文；门禁全链实跑。
- 对抗式：假定端侧仍缺设备证明（grep `DeviceKeystore` 在 `showStaffSelfRegistration` 内两处调用）；假定入口在工作手机应用仍可见（按钮只在 `api.terminalType == null` 时构造）；假定公开路径带上了绑定设备的签名（`send` 只在 `deviceId != null` 时签名，注册前 deviceId 为空）；假定文案仍指向登录页旧入口（旧句 0 命中）。

## 2. 逐段结论（修复后）

| 段 | 定稿「是」ID | 修复后核实 | 结论 |
|---|---|---|---|
| U01 安卓应用 | 001/002/003/006/007/008/009 + 016 | 007：`TerminalApiClient` 两条公开注册路径只声明 `staff_private_phone`（`terminalTypeOverride`），`showStaffSelfRegistration` 以 `DeviceKeystore.devicePublicKeyBase64()/deviceFingerprintHash()` 填 `StaffSelfRegistrationRequest`，成功后指纹持久化并按私人手机码声明（onCreate 恢复）；入口只在未绑定为工作手机/平板时显示（001）；016：商户代提交页文案指向「公司员工自助注册（私人手机）」；002/003/006/008/009 与 B5-r1 相同（探针 L3–L7）；Android compileDebugKotlin EXIT 0 | PASS |
| U02 JVM 客户端 | 001–004 | 未改；JVM 30/30（探针 L7） | PASS |
| U03/U04 web | U03-001…009、U04-001/002 | 未改；探针 L9–L11 | PASS |
| U05/U06 员工服务 | U05-001…011、U06-001…007 | 未改；探针 L13–L18 | PASS |
| U07 设备闸 | 001–005 | 未改；探针 L20–L21 | PASS |
| U08 组织商户 | 001–005 | 未改（四文件＝main） | PASS |
| U09 主管复核 | 001–005 | 未改；探针 L23 | PASS |
| U10 迁移 | 001–003 | 未改；探针 L25–L26 | PASS |
| U11/U12 测试 | U11-001…008、U12-001…005 | 未改；探针 L28–L29 | PASS |
| U13 设计说明与安全产物 | 001…008、010 + 011 | 011：T1:7「V411—V422 … V421 为寄存柜扫码凭证表、V422 为顶班记录表」（探针 L31 含 V411/V422）；其余未改；`--strict` 绿（L32） | PASS |
| U14/U15 契约 | 001…008 各 | 未改；探针 L34–L45 | PASS |
| U16/U17/U18 生成物与工具 | U16-001…007、U17-001…010、U18-001…004 | 未改；四闸绿（L46–L52） | PASS |

## 3. 新发现（本轮只登记，不修）

- 无阻断项。观察：新屏「公司员工自助注册（私人手机）」采集手机号/证件号未设 FLAG_SECURE，与材料原屏同形，属 U01-004（否，登记）归属，本轮不动；DeviceCredentialVerifierTests 23 例（B4-r1 曾误写 39，已勘误）。

## 4. 探针原文（/tmp/ufix_b5_run.log，B3-r2 后重跑）

```
START=2026-09-10T19:44:51Z worktree_tree=c0ec9b7ab6bc38f8197830b7283af4dc2356b61b main_head=445a49f4
== U01/U02 terminal identity (android-app / JVM)
app selfRegistration refs: apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt:4 apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt:2 
app STAFF_PRIVATE_PHONE/terminalType: 10 ; ChargingReceiptService/BootReceiver: apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt:1 apps/android-terminal/android-app/app/src/main/AndroidManifest.xml:2 
app POS bind tablet+signing: 5 ; 文案 加密存放: 1
jvm applyHeaders override: 6 ; devicePublicKey/fingerprint in models: 6 ; staffId in BindDeviceRequest: 8
jvm tests xml: 7 files; totals: 30 tests, failures 0
== U03/U04 web
StaffProfilesPage include_archived default: 45:  // U03-008：默认不把已存档档案（含脱敏手机号）下到浏览器；勾选后才带 include_archived=true 重新拉取，不在客户端过滤。;53:      const items = await api.get<StaffProfileRow[]>(includeArchivedRef.current ? "/api/v1/staff/profiles?include_archived=true" : "/api/v1/staff/profiles");;
StaffShiftAttendancePage note validate: 12
types.ts == main: yes ; OrganizationMerchantPage == main: yes
== U05/U06 backend staff
cooperationForm self_operated: 2 ; selfOperated() refs: 0
leave base: 3 ; COALESCE(sp.company_id: 0 ; STAFF_COMPANY_REQUIRED: backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:1 backend/src/main/java/com/complex/platform/access/staff/StaffService.java:1 
settingScopeKey: 3 ; numericSetting(key, scope)+POLICY_NOT_CONFIGURED: backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:1 backend/src/main/java/com/complex/platform/access/staff/StaffService.java:0  ; shiftRevisions lockForUpdate: backend/src/main/java/com/complex/platform/access/staff/JdbcStaffRepository.java:3 backend/src/main/java/com/complex/platform/access/staff/StaffApprovalService.java:0 
locker single-end: issueScanToken backend/src/main/java/com/complex/platform/access/staff/StaffLockerService.java:1 backend/src/main/java/com/complex/platform/access/staff/StaffController.java:1  kiosk controller present ; covers: backend/src/main/java/com/complex/platform/access/staff/StaffService.java:2 backend/src/main/java/com/complex/platform/access/staff/StaffController.java:4 
idempotency employment-status/shift-visible-days: 0 (see controller) ; recordFailure sites: backend/src/main/java/com/complex/platform/access/staff/StaffService.java:21 backend/src/main/java/com/complex/platform/access/staff/StaffRegistrationService.java:1 backend/src/main/java/com/complex/platform/access/staff/StaffLockerService.java:2  ; success names: backend/src/main/java/com/complex/platform/access/staff/StaffService.java:3 backend/src/main/java/com/complex/platform/access/staff/StaffLockerService.java:1 
self-registration device proof: backend/src/main/java/com/complex/platform/access/staff/StaffRegistrationService.java:5 backend/src/main/java/com/complex/platform/access/staff/dto/StaffSelfRegistrationRequest.java:1 
== U07 gate/terminal
DEVICE_BOUND_TYPES: 47: * 旧实现在自报头为空/不在 DEVICE_BOUND_TYPES 时早退，客户端不发头或改报 web 即免签名写入， ; terminal_proof_required: 1 ; readAccessToken: 1
DeviceBindRequest staffId: 1 ; bind writes staffId: 17 ; RateLimit self-registrations rule: 1 ; interceptor exemptions: backend/src/main/java/com/complex/platform/gate/DeviceCredentialInterceptor.java:1 backend/src/main/java/com/complex/platform/gate/TerminalMasterSwitchInterceptor.java:1 
== U09 review
isMoneyAction staff.payroll: 53:            new SupervisorReviewPolicyRecord("staff_penalty_cap_adjust", "staff.payroll.penalty_cap.adjust", 0, 3, tr;216:                    if ("staff.payroll.penalty_cap.adjust".equals(pending.actionType()); ; required=false: 0 ; ctor publisher: 2
== U10 migrations
V410__m010_business_entity_topology_and_role_isolation.sql V411__staff_profile_pii_and_self_device_permission.sql V412__staff_registration_initiator.sql V413__staff_policy_settings.sql V414__staff_profile_encrypted_personal_fields.sql V415__staff_employment_status.sql V416__staff_work_phone_receipts.sql V417__staff_shift_revision_approvals.sql V418__staff_penalty_cap_approval_history.sql V419__staff_registration_company_routes.sql V420__staff_locker_scan_records.sql V421__staff_locker_scan_tokens.sql V422__staff_shift_covers.sql 
V413 seed: 3 ; V419 is_self_operated: 0 ; migration order gate: MIGRATION_VERSION_ORDER_PASS: 本分支新增 12 个迁移,均排在 origin/main 最大号 V410 之后
== U11/U12 tests
M010 codes: 4 ; withSelfOperated: 0 ; 6000/7800: 4 ; deviceProof: 4 files
Verifier tests DEVICE_CREDENTIAL_INVALID: 16 ; webWithCookieAllowedUnderEnforce: 1 ; review tests new: 8
== U13 docs & security
T1 header: V0.13 T9: V0.13 ; T1 试岗/停岗: 1 ; V066 钉: 4 ; T1 V411: 1
security-baseline strict: Formal security baseline verification passed: security-scans/formal-security-baseline-v1.md and security-scans/findings-register-v1.md.
== U14–U18 contract & generated
operations 732 info.version V0.14
self-operated path present: False
self-registration required: ['idempotency_key', 'company_id', 'name', 'device_public_key', 'device_fingerprint_hash'] terminals: ['staff_private_phone'] failure: staff.self_register_failed
none events: 0
/api/v1/staff/shifts staff.shift.submitted
/api/v1/staff/shifts/batch staff.shift.submitted
/api/v1/staff/settings/payroll-penalty-cap staff.payroll.penalty_cap_requested
/api/v1/staff/profiles/{id}/employment-status idem True
/api/v1/staff/settings/shift-visible-days idem True
visible-days GET desc: 按当前经营主体读取；经营主体无自有行时回落到全局出厂行（出厂 7 天，上下限 1..31 为操作员裁定的上线配置）；无出厂行则拒绝（POLICY_NOT_CON
locker ops: ['recordStaffLockerScan', 'issueStaffLockerScanToken']
cover ops: ['listTicketTypeGiftCoverage', 'coverStaffShift', 'listStaffShiftCovers']
verify:openapi-contract: verify:operation-matrix: docs/p0-openapi-operation-acceptance-matrix-v0.1.md is in sync with docs/openapi/openapi-p0.json; 732 operations checked
verify:openapi-closure: /home/james/wt/ufix-audit-20260910/docs/p0-openapi-contract-closure-appendix-v0.1.md is in sync with /home/james/wt/ufix
verify:domain-ownership-scope: DOMAIN_OWNERSHIP_SCOPE_PASS: 334 tables classified
verify:data-isolation-spec: data isolation spec is in sync with Flyway migration scope baseline.
verify:pii-encryption: PII_ENCRYPTION_PASS: 16 个 PII 列均经 piiCipher.encrypt 落库
appendix title: # P0 OpenAPI 权限、事件、任务追溯收口附录 V0.14 ; STAFF-员工: 96 ; matrix title: # P0 OpenAPI Operation 验收矩阵 V0.14
generator version sources: tools/generate-openapi-contract-closure-appendix.mjs:3 tools/generate-openapi-operation-matrix.mjs:3  ; taskGroup staff: 1 ; domain inherited new tables: 2
== boundaries
HTML sha (worktree == main): same same same 
main worktree status (non-audit files): 0
predeploy: EXIT=0 TEST_DB_ISOLATION_PASS: 测试源未引用生产库 complex（均用 complex_test）
END=2026-09-10T19:44:56Z
```

## 5. 结论

整模块修复后全量审计：**PASS**（U01–U18 各段定稿「是」ID 含回退 B2 定稿的 U01-016/U13-011 均已闭合；U01-007 按定稿在私人手机通道以 Keystore 公钥/指纹提交并绑定；无新缺陷；越界检查通过：三 HTML/锁/sidecar/ledger 基准/主工作区未改；未整树覆盖 main）。本票为作者自审票，须与 Grok B5-r2 票合取；Grok FAIL 具否决权。
