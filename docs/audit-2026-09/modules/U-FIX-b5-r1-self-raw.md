CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# U-FIX B5-r1 修复后全量审计 · 席甲完整报告
版本：V0.01

- 席甲：Claude Fable 5.1（作者自审票）。步骤：B5-r1（合成模块 U-FIX 的 B3 第 1 次整批修复后的整模块全量三线重审，不只看 diff）。2026-09-10T17:57:15Z
- 对象：worktree `/home/james/wt/ufix-audit-20260910` after tree `e3fd14b6e61f284e3a9dc7380d529e7486c71b8a`（仓库外临时索引复核，探针开头再次实算同值）相对 main 445a49f4 的 85 路径全文及其上下游（迁移/契约/生成物/测试）；三份业务 HTML 取 main 正本（worktree 三份与 main 逐字节相同）。判据：U01–U18 各段映射行并集（criteria-lock V0.11，锁值见首行）。
- 前置：B4-r1 双 PASS（modules/U-FIX-b4-r1-grok-raw.md V0.01，121287bytes SHAbcaad009f2d59472bba0a453eabb9bc8b7b023cbd2c35dae5c0ef8758d56483b，总 PASS；modules/U-FIX-b4-r1-self-raw.md）；B3 累计 1。
- 证据：席甲只读探针 `/tmp/ufix_b5_probes.sh` → `/tmp/ufix_b5_run.log`（全文附 §4）；后端全量 `/tmp/ufix_full_tests.log`；web/JVM/Android `/tmp/ufix_final_verify.log`；`verify:predeploy` `/tmp/ufix_b5_predeploy.log`。本报告落盘前未读取 `U-FIX-b5-r1-grok-raw.md` 任何正文。

## 1. 三线方法

- 第一性原理线：逐段追问「定稿缺陷是否真正闭合、修复是否引入新缺陷」：终端身份是否只由绑定/登记设备类型声明一次并在无设备号写请求 fail-close；自助注册是否必须带设备证明并在注册即绑定；薪酬公式是否按 HTML（当期应发为被减数、总薪资各项之和为分母）；柜机是否单端上报；顶班是否有入口；参数出厂行是否存在且 GET 不把空上下限当合同；契约与生成物是否由同一 json 生成、版本可复现。
- 顺从式线：各段判据逐项（GEN-001–018、BSM/PGM/BEQR 映射行）对照修复后文件；门禁全链实跑。
- 对抗式线：只叠部分文件是否红（不适用——本模块以整批同一 worktree 交付）；整树覆盖是否发生（否：6 文件回退 main、U08 四文件＝main）；测试是否只改断言迁就实现（否：U16-006 先红后绿，U11/U12 断言指向真实错误码与 audit_events 行）；生成物是否手改（否：四生成器 `--check` 全绿）。

## 2. 逐段结论（修复后）

| 段 | 定稿「是」ID | 修复后核实（探针 §4 行号） | 结论 |
|---|---|---|---|
| U01 安卓应用 | 001/002/003/006/007/008/009 | 自助注册引用 0（L5）；类型头取绑定/override、STAFF_PRIVATE_PHONE（L6）；前台服务+开机接收器（L6）；POS 绑定 tablet+登记签名（L7）；文案「加密存放」（L7）；Android compileDebugKotlin EXIT 0；行为测试落在 JVM 侧 TerminalApiClientHeadersTest（U01-008 定稿允许「JVM 侧或接口假件」） | PASS |
| U02 JVM 客户端 | 001/002/003/004 | applyHeaders override 6 处、模型含 devicePublicKey/deviceFingerprintHash、BindDeviceRequest.staffId（L8）；JVM 30/30（L9） | PASS |
| U03/U04 web | U03-001…009、U04-001/002 | OrganizationMerchantPage.tsx / types.ts ＝ main（L13）；StaffProfilesPage 默认不带 include_archived（L11）；编辑态事由校验（L12）；tsc 0、vitest 33/33 | PASS |
| U05/U06 员工服务 | U05-001…011、U06-001…007 | 分流读 cooperationForm、selfOperated() 0 引用（L15）；事假基数含提成/奖惩（L16）；COALESCE 0、STAFF_COMPANY_REQUIRED（L16）；settingScopeKey 共用、POLICY_NOT_CONFIGURED 回落（L17）；history 只读（lockForUpdate，L17）；柜机单端 issueScanToken + kiosk 控制器、顶班 cover 入口（L18）；两写口幂等（控制器 L96-99/L218-221）；失败/成功事件（L19）；注册设备证明（L20）；员工域 9 类 DB 测试全绿 | PASS |
| U07 设备闸 | 001/002/003/004/005 | DEVICE_BOUND_TYPES 含私人手机、无设备号 fail-close（terminal_proof_required）、web 需 Cookie（L22）；DeviceBindRequest.staffId、bind 写入；频控规则；拦截器豁免公开注册（L23）；DeviceCredentialVerifierTests 23 例绿（席甲 B4 raw 曾误写 39，Grok B4 观察已指出，以 23 为准） | PASS |
| U08 组织商户 | 001…005 | 四文件＝main、DTO 删除（B4 §3.0）；json 无 self-operated（L37） | PASS |
| U09 主管复核 | 001/002/003/004/005 | isMoneyAction 纳入 staff.payroll（L25）；required=false 0、构造注入发布器（L25）；hr_review 种子在 V418；SupervisorReviewServiceTests 44 绿含三条新用例（L31） | PASS |
| U10 迁移 | 001/002/003 | V411–V422 连续（L27）；V413 出厂行（1..31，值 7）（L28）；V419 无 is_self_operated（L28）；MIGRATION_VERSION_ORDER_PASS（L28）；隔离说明/CSV 重生 in sync（L50-51） | PASS |
| U11/U12 测试 | U11-001…008、U12-001…005 | 错误码断言 4、withSelfOperated 0、期望 6000/7800、deviceProof（L30）；DEVICE_CREDENTIAL_INVALID、webWithCookieAllowedUnderEnforce、复核新用例（L31）；全量跑通 | PASS |
| U13 设计说明与安全产物 | 001…008、010 | T1/T9 V0.13、试岗/停岗、V411 编号句、V066 只留历史草案标签（L33）；`verify:security-baseline --strict` 绿（L34）；CSV/隔离说明重生（L50-51） | PASS |
| U14/U15 契约 | 001…008 各 | 732 口、info.version V0.14、无 self-operated、自助注册 required 含设备公钥/指纹且终端仅私人手机、失败事件非 none、none 事件 0、三口成功名、两口幂等 true、可见天数描述（L36-45）；`verify:openapi`（json↔yaml）绿 | PASS |
| U16/U17/U18 生成物与工具 | U16-001…007、U17-001…010、U18-001…004 | 附录/矩阵标题 V0.14 跟随 info.version、变更表已删、STAFF-员工 96（L53）；生成器版本源 info.version、taskGroup staff、域归属新表 inherited 规则（L54）；四闸 `--check` 全绿（L48-52）；PII 16 列 | PASS |

## 3. 新发现（本轮只登记，不修）

- 无阻断项。观察（不入队，B4 已登记）：V411–V420 中 8 个仅重编号的迁移保持 V0.01（与材料逐字节相同）；Android 编译需 ANDROID_HOME（环境，非代码）；后端全量 20 个失败均为 main 既有基线五类（Finance/Amusement/Compliance/Facility/CustomerGuarded），与纯净 main 逐类相同，归对应 main 模块。

## 4. 探针原文（/tmp/ufix_b5_run.log）

```
START=2026-09-10T17:42:54Z worktree_tree=e3fd14b6e61f284e3a9dc7380d529e7486c71b8a main_head=445a49f4
== U01/U02 terminal identity (android-app / JVM)
app selfRegistration refs: apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt:0 apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/TerminalApiClient.kt:0 
app STAFF_PRIVATE_PHONE/terminalType: 2 ; ChargingReceiptService/BootReceiver: apps/android-terminal/android-app/app/src/main/kotlin/com/complex/terminal/MainActivity.kt:1 apps/android-terminal/android-app/app/src/main/AndroidManifest.xml:2 
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
END=2026-09-10T17:42:59Z
```

## 5. 结论

整模块修复后全量审计：**PASS**（U01–U18 各段定稿「是」ID 均已闭合；无新缺陷；越界检查通过：三 HTML/锁/sidecar/ledger 基准/主工作区未改；未整树覆盖 main）。本票为作者自审票，须与 Grok B5-r1 票合取；Grok FAIL 具否决权。

---

# U-FIX 回退 B2 席甲票（追加段）
版本：V0.02（本文件自身；B5-r1 段原字节不变）

- 输入：findings V1.82「当前 U-FIX B5-r1 回退 B2 候选」U01-016、U13-011；modules/U-FIX-b5-r1-grok-raw.md V0.01 B5 段（20175bytes SHA6b7b91e158111c3caa087f6acc7dcdee4345603b738a23b25528e9cba8225edf）；本文件 B5 段（14038bytes SHA5ff8a17794279cf385cb6ae34060ee4cca5738692814d2501ed15b55df6ab456）。2026-09-10T19:42:36Z
- 席甲核实（只读，after 树 e3fd14b6）：`MainActivity.kt:1201` 文案与 `showLogin()`（:196-228）无入口属实；T1:7「V411—V421 … V421 为寄存柜扫码凭证表」而迁移目录含 `V422__staff_shift_covers.sql` 属实；U01-007 原处置（私人手机端 Keystore 公钥/指纹提交并绑定）与 Grok §5.1 一致——B3-r1 删除入口不满足该处置，席甲 B5 探针漏判。

## 勘误（席甲 B5-r1 段）

- 席甲 B5 探针只核「工作手机应用无自助注册引用」（探针 L5），未追问 HTML「私人手机 App 安装时生成并绑定设备标识」在 U01 三文件上由谁执行；本 APK 同时承载 BYOD 段，删除即让私人手机用户无法端侧提交。席甲 B5-r1 票 PASS 被 Grok 否决，合取 FAIL；B5 段原字节不改。

## 逐 ID 票

| ID | 票 | 依据 |
|---|---|---|
| U01-016 | PASS | 文案指向已删入口属实；技术性（GEN-016/PGM-001）；随 U01-007 改指向实际入口，不发明流程 |
| U13-011 | PASS | T1:7 上界漏 V422 属实；技术性（GEN-016）；只改该句上界并补 V422 一句 |

U01-007：沿用原 ID 与原处置（有 ID 且仍在队列），不在本批重登；其余 116 个「是」项按 Grok B5-r1 §4 逐段裁定闭合，B3-r2 不重做。

## 勘误与采纳（接受 Grok 回退 B2 非阻断修正 4 条，定稿以此为准）

1. U01-016 支撑判据只留 GEN-016、PGM-001（不引 GEN-002）。
2. U01-016「实际入口」以登录页按钮名「公司员工自助注册（私人手机）」为准写入文案，不另造入口名。
3. U13-011 对 V422 的描述以迁移文件头/迁移记录用语「顶班记录」为准，不写 HTML 没有的新短语。
4. B3-r2 只修 U01-007/U01-016/U13-011，其余 116 项不重做；取新 after tree → B4-r2 → B5-r2。

## 结论

席甲回退 B2 票：**PASS**（候选 U01-016、U13-011 成立；发布前须修「是」2；添加 0；questions 0）。本票为作者自审票，以 Grok 回退 B2 票合取；Grok FAIL 具否决权。本段落盘前已读 Grok 回退 B2 追加段（V0.02，全文 31944bytes SHAc264e9caf1ccc13df163a11000c11ec9b51dc40f6142e5772e4f9ce9d46ad345，前 20175bytes 原字节不变）。
