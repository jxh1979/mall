# T1 员工三表 可编码开发规格 V0.14（2026-06-18）

> **任务**：R5 backlog **T1（员工档案/设备凭证/通知三表）**，W0 底座，**解锁 T9（Android 设备凭证）**。
> **裁决依据**：R5 裁决⑨——员工三表 + Android 端凭证本期 P0；MDM 自有侧字段暂缓。
> **关键边界**：本规格的 `staff_device_credentials`（员工私人手机 Keystore 公钥/不可逆指纹）与既有 `terminal_device_credentials`（V011，kiosk 一体机随机 token）是**两套完全不同实体**，互不复用、互不串值。
> **工程约定**（对齐既有迁移）：`id VARCHAR(64)` 主键；`TIMESTAMPTZ ... DEFAULT now()`；CHECK 命名 `chk_<表>_<字段>`、唯一 `uq_<表>_<列>`；隔离列 `project_id VARCHAR(64) NOT NULL`、`merchant_id VARCHAR(64)`（NULL=平台员工）；姓名、手机号与证件使用既有 PiiCipher 加密存放；phone/id_doc 接收原值，日常 HTTP 响应强制脱敏，不从旧哈希恢复。
> **迁移**：三表最初由 V066 建立；本次员工域迁移以 main 已应用的 V410 之后连续编号为 V411—V422（原稿 V400—V409 低于 V410，按 U10-001 重编号；V421 为寄存柜扫码凭证表、V422 为顶班记录表）；测试用 `MigrationVersions.latest()` 动态读版本（R3 已去硬钉，无需改 12 处 JDBC 测试）。落地后把 `matrix-migration-claims.json` 中 `staff_profiles`/`staff_device_credentials`/`employee_notices` 由 absent 改 present。

## M010 当前实现（2026-09-08）

- 注册分两条路径：商户通过 `/staff/registrations` 代提交，审批域公司＝该商户当前生效的经营主体公司（main 主体拓扑为三角色多对多，无固定父链）；公司员工在私人手机 App 安装时由 Keystore 生成设备密钥，以私人手机通道（终端类型 `staff_private_phone`）调用公开 `/staff/self-registrations` 本人提交，请求必须携带该手机的设备公钥与指纹并随申请绑定本人（缺失、不符或指纹已绑他人即拒），公开入口按认证类频控；公司列表来自 `/staff/registration-companies`。合作方式为「自营」的商户视同公司自有，其员工使用公司员工路径。公开提交只返回申请编号与待审状态，审核前不产生账号。提交人不得自审；公司审批与商户提交分开。
- 姓名、手机号和证件由现有 PiiCipher 加密；日常接口固定脱敏手机和证件。任职状态独立保存待入职、在职、试岗、停岗、离职，与审批、存档分开。通知和私人手机操作仅限本人；首次私人手机绑定直接生效，换机及异常后的复核保留。删除须先存档、最高权限批准且无现存引用；未结束排班和未取寄存物也属于引用。
- 单次或整周排班先完整验证，再通过现有主管上一级审批。批准前不写入已发布排班；修改由原发起人在班次开始前填写原因，前后快照和审批编号追加留存。审批单包含人员、前后时间、点位和原因。可见天数按经营主体设置，出厂含今天共七天，配置须同时填写上下限。
- 普通扣除合计按当月总薪资的比例封顶，默认20%；总薪资按 HTML 为基础工资、各项补贴、绩效工资与奖金之和（现行列只有固定工资与奖金两项先取全，补贴/绩效列落地后并入分母），提成不进入分母，事假单算。上限变更要求人事、财务、法务均通过，再由最高权限最终确认；已月结月份保留原上限快照。事假按已批准且覆盖实际排班的请假工时占当期总工时的比例，从当期应发薪资（固定工资+提成+奖金，个税与处罚另算）中扣减，下限为零。跨月班次按结束时点归属，月历只采用属于该班次的打卡。
- 柜机以已登记 kiosk 设备令牌（`X-Device-Token`）单端调用 `/api/v1/terminals/kiosks/{device_id}/locker-events` 上报扫码完成事件；员工身份只由本人手机会话申请的一次性短时扫码凭证（`POST /api/v1/staff/lockers/scan-token`，120 秒内有效）承载并由服务端换回，不要求同一请求带员工会话；`staff_locker_records` 留存格口和存取时刻。重复事件按幂等键返回原结果，未取空格口不能给他人使用，取空后可流动使用。员工不用另外填写登记表。此处完成的是软件事件记录；厂商扫码身份接入、物理开锁协议尚缺资料，未声称完成设备联调。


---

## 1. 数据模型（Flyway V066 草案 DDL）

```sql
-- V066__p0_staff_profiles_and_device_credentials.sql

-- 1) 员工档案
CREATE TABLE IF NOT EXISTS staff_profiles (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,                 -- 综合体归属（隔离）
    merchant_id VARCHAR(64),                         -- NULL=平台员工；非空=商户员工
    user_id VARCHAR(64),                             -- 审批通过后关联 access_users.id（生成角色）
    name TEXT NOT NULL,                         -- 姓名密文
    phone TEXT,                                     -- 手机号密文
    id_doc TEXT,                                    -- 证件密文（可选）
    staff_level_id VARCHAR(64) REFERENCES staff_levels(id),  -- 职级（V055）
    registration_status VARCHAR(32) NOT NULL DEFAULT 'submitted',
    approved_by VARCHAR(64),
    approved_at TIMESTAMPTZ,
    reject_reason VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_staff_profiles_reg CHECK (registration_status IN ('submitted','approved','rejected','suspended')),
    CONSTRAINT chk_staff_profiles_status CHECK (status IN ('active','inactive'))
);
CREATE INDEX IF NOT EXISTS idx_staff_profiles_scope ON staff_profiles(project_id, merchant_id);

-- 2) 员工私人手机设备凭证（Keystore 公钥 / 不可逆指纹）—— 区别于 kiosk 的 terminal_device_credentials
CREATE TABLE IF NOT EXISTS staff_device_credentials (
    id VARCHAR(64) PRIMARY KEY,
    staff_profile_id VARCHAR(64) NOT NULL REFERENCES staff_profiles(id) ON DELETE CASCADE,
    device_public_key TEXT NOT NULL,                 -- Android Keystore 设备公钥
    device_fingerprint_hash VARCHAR(64) NOT NULL,    -- 不可逆设备指纹哈希（不读手机本机 NFC 唯一标识，D8）
    bind_status VARCHAR(32) NOT NULL DEFAULT 'pending_review',
    supersedes_credential_id VARCHAR(64) REFERENCES staff_device_credentials(id),  -- 换机链：指向被替换旧凭证
    integrity_status VARCHAR(32) NOT NULL DEFAULT 'trusted',
    revoked_reason VARCHAR(255),
    bound_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_staff_dev_cred_bind CHECK (bind_status IN ('pending_review','active','superseded','revoked')),
    CONSTRAINT chk_staff_dev_cred_integrity CHECK (integrity_status IN ('trusted','root_detected','debug_detected','abnormal'))
);
-- 每个员工至多一条 active 凭证（换机先 superseded 旧的再 active 新的）
CREATE UNIQUE INDEX IF NOT EXISTS uq_staff_dev_cred_one_active
    ON staff_device_credentials(staff_profile_id) WHERE bind_status = 'active';
CREATE UNIQUE INDEX IF NOT EXISTS uq_staff_dev_cred_fingerprint
    ON staff_device_credentials(device_fingerprint_hash) WHERE bind_status IN ('pending_review','active');

-- 3) 员工通知 / 合规同意凭据（必须有写入方，避免"建而无写入方"）
CREATE TABLE IF NOT EXISTS employee_notices (
    id VARCHAR(64) PRIMARY KEY,
    staff_profile_id VARCHAR(64) NOT NULL REFERENCES staff_profiles(id) ON DELETE CASCADE,
    notice_type VARCHAR(48) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    consent_required BOOLEAN NOT NULL DEFAULT false,
    consent_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    consented_at TIMESTAMPTZ,
    created_by VARCHAR(64) NOT NULL,                 -- 写入方（系统/审批人/合规模块）
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_employee_notices_type CHECK (notice_type IN ('approval_result','status_change','compliance_consent','system_notice')),
    CONSTRAINT chk_employee_notices_consent CHECK (consent_status IN ('pending','acknowledged','consented','declined'))
);
CREATE INDEX IF NOT EXISTS idx_employee_notices_staff ON employee_notices(staff_profile_id);
```

---

## 2. 状态机

**注册** `staff_profiles.registration_status`
`submitted → approved`（生成 user_id + 默认角色，写 staff.approved 事件 + approval_result 通知）｜`submitted → rejected`（reject_reason 必填）｜`approved → suspended`（停用，撤销关联角色）。

**设备凭证** `staff_device_credentials.bind_status`
- 首次本人绑定：`(无) → active`，写 `bound_at`；不另加人工审批。换机及异常后重新绑定继续复核。
- 换机：新凭证 `pending_review` 且 `supersedes_credential_id=旧 active.id`；审批通过 → 新凭证 `active`、旧凭证 `superseded`（保证 `uq_staff_dev_cred_one_active`）。
- 失效：`active|pending_review → revoked`（解绑、或 integrity 检测异常），写 `revoked_at`/`revoked_reason`。
- 完整性：`integrity_status` 由 Android 端 report；`root_detected/debug_detected/abnormal` → 自动 `bind_status=revoked` + 写 staff_device.integrity_failed + 进库存域外的复核（沿用 supervisor_review 或仅通知，见下）。

> **换机口径**：先解绑旧、再绑新——通过"新凭证 pending_review + supersedes 链 + 审批后旧 superseded"实现"换机待审中间态"，符合 HTML"换手机必须先解绑旧再绑新"。

---

## 3. Service 方法（建议 `staff` 域）

`StaffProfileService`
- `submitRegistration(scope, req)` → 建 staff_profiles(submitted)；权限 `staff.profile.submit`。
- `reviewRegistration(scope, id, decision, reason)` → approve 生成 user_id 并分配"员工自助"角色 `role_staff_self`(V322) / reject 写 reason；权限 `staff.profile.review`（DENY 优先 + currentMerchantScope）。
- `suspend(scope, id)` / `listProfiles(scope)`（强制 currentMerchantScope 过滤）。

`StaffDeviceCredentialService`
- `bindCredential(scope, staffId, publicKey, fingerprintHash)` 校验本人归属；首次直接 active，已有凭证历史则 pending_review；已有 active 且指纹不同时置 supersedes，批准时先停旧再启新。
- `approveRebind(scope, credId)` → 新 active、旧 superseded；权限 `staff.device.review`。
- `reportIntegrity(staffId, credId, status)` → 非 trusted 即 revoke + 通知 + 事件；端侧上报。
- `revokeCredential(scope, credId, reason)` / `unbind(...)`；权限 `staff.device.revoke`。

`EmployeeNoticeService`
- `createNotice(staffId, type, title, body, consentRequired, createdBy)`（审批/状态变更/合规模块调用，保证有写入方）。
- `acknowledge(scope, noticeId, consent)` → consent_status 流转。

> 所有列表/查询注入 `currentMerchantScope`：平台角色见全量，商户角色仅见本 merchant_id 行。

---

## 4. API / OpenAPI 端点（P0）
| 方法 | 路径 | 权限点 | 说明 |
|---|---|---|---|
| POST | /api/v1/staff/registrations | staff.profile.submit | 商户代员工提交注册（merchant_id 必填，由商户经营主体公司审批） |
| GET | /api/v1/staff/registration-companies | public | 公司员工自助注册可选公司列表 |
| POST | /api/v1/staff/self-registrations | public（私人手机通道，须带设备公钥/指纹，频控） | 公司员工本人提交注册，只返回申请编号与待审状态 |
| POST | /api/v1/staff/profiles/{id}/employment-status | staff.profile.review | 任职五态变更——CG-12 起只发起「HR 会签＋直属领导向上三级」复核（`hr_approver_id` 必填，须持 staff.hr.review），全部通过后写入；返回复核单（幂等键必填） |
| POST | /api/v1/staff/payroll/adjustments | staff.payroll.manage | 奖惩（bonus/penalty）——CG-12 起只发起同档复核，通过后入账；返回复核单（object_id＝入账记录 id） |
| POST | /api/v1/staff/payroll/adjustments/{id}/appeals | staff.payroll.view_mine | 员工本人对已生效扣罚提出申诉（同链裁定：通过＝撤销冲回、驳回＝维持） |
| GET | /api/v1/staff/payroll/adjustments/appeals | staff.payroll.view_mine（按员工查看须 staff.payroll.manage） | 申诉清单（pending / upheld / revoked） |
| GET | /api/v1/staff/settings/shift-visible-days | staff.shift.manage | 读经营主体可见天数（缺行回落全局出厂行 7/1..31） |
| POST | /api/v1/staff/settings/shift-visible-days | staff.shift.manage | 设可见天数（值与上下限同填，幂等键必填） |
| POST | /api/v1/staff/lockers/scan-token | staff.profile.self | 本人申请寄存柜一次性扫码凭证 |
| POST | /api/v1/terminals/kiosks/{device_id}/locker-events | 柜机 X-Device-Token | 柜机单端上报格口存取事件 |
| POST | /api/v1/staff/shifts/{id}/cover | staff.shift.manage | 已开始班次的临时顶班记录（不走排班审批） |
| POST | /api/v1/staff/registrations/{id}/review | staff.profile.review | 后台审批通过/驳回 |
| GET | /api/v1/staff/profiles | staff.profile.view | 隔离列表 |
| POST | /api/v1/staff/device-credentials | staff.device.bind | 本人首次绑定直接生效；换机/异常重新绑定待复核|
| POST | /api/v1/staff/device-credentials/{id}/approve-rebind | staff.device.review | 审批换机生效 |
| POST | /api/v1/staff/device-credentials/{id}/report-integrity | （端侧鉴权）| Root/调试上报 |
| POST | /api/v1/staff/device-credentials/{id}/revoke | staff.device.revoke | 解绑/失效 |
| GET | /api/v1/staff/notices | staff.notice.view | 通知/同意列表 |
| POST | /api/v1/staff/notices/{id}/acknowledge | staff.notice.view | 确认/同意 |
| POST | /api/v1/staff/shifts | staff.shift.manage | 主管排班（单个班次，同员工同开始时刻幂等覆盖）|
| POST | /api/v1/staff/shifts/batch | staff.shift.manage | 按周批量排班（一批员工 × 勾中的星期几 × 每日同一时段，一次排完整周）|

> **排班口径（2026-08-02 用户拍板）：一次排一周，跨零点班只填时段。**
> 排班员的实际做法是一次性排出一周的班，而不是一人一天点一次，所以批量排班是主入口、单班保留作零星补改：
> 一次提交 = **一批员工 + 一个日期窗口（起始日期起 N 天，N 默认 7、上限 31）+ 勾选星期几（不勾＝窗口内每天都排）+ 每日同一时段**。
> **跨零点班（如 22:00-06:00）只填时段**：结束时刻不晚于开始时刻，系统即判定为跨零点，把班次结束自动顺延到次日，
> 排班员不用手动选日期；`work_date` 记班次**开始那天**，与单班排班同一语义。日期与时刻的折算一律按所在地（北京）时区，
> 与考勤按 `start_at`/`end_at` 区间配班用同一把尺子。重复排同一员工的同一开始时刻＝**覆盖修改**（唯一键 `(user_id, start_at)` 承接），
> 不新增第二条；返回新建/覆盖条数与逐条明细，排班员提交后能直接看到本次排出了什么。
> 一次生成上限 500 个班次（员工数 × 天数），拦「全选员工 × 31 天」的手滑。权限闸、项目隔离、审计留痕逐条复用单班排班同一条路径。

> **跨月／跨周／跨年班次的归属口径（2026-08-06，矛盾12 ＋ 矛盾11 联动；依据 BSM「请假扣薪」条、业务系统地图日结批次条、ENUM「运营营业日」条）**
> 班次起止跨越**月份、周或年度**边界的（如 1 月 31 日 22:00 至 2 月 1 日 06:00 的夜班、12 月 31 日 22:00 至次年 1 月 1 日 06:00 的夜班），
> 该班次的**工时、考勤、请假以及年假额度、全勤等年度指标，一律归属班次结束时点所在的月份与年度**；
> 该班次**是否属于法定节假日加班，同样按班次结束时点所在的自然日判定，整班按同一口径认定，不按跨零点前后拆分半程**。
> **这是营业日归口清单的唯一例外**：ENUM「运营营业日」条与 业务系统地图日结批次条的归口清单里，「考勤日期」一项**不按开台/下单时点所属营业日判定，而按班次结束时点判定**；
> 清单其余各项（日结批次、最低消费时段、票价日期类型、场次归属、巡检有效期、销售提成月份）一律**仍按营业日**，本例外不外溢。
> `work_date` 记班次**开始那天**只是排班表的排列键，**不构成考勤与薪资的归属月份**——归属月份一律由 `end_at` 推导；请假扣薪的分母取**归属月份**的当期总工时。

> OpenAPI json + yaml 同步（注意工具 `tools/apply-openapi-p0-examples.mjs` 需 `yaml` 包；若缺，手工镜像 yaml 并 pyyaml 校验）。

## 5. 事件（D4，点+下划线）
CG-12：`staff.profile.employment_status_change_requested`（发起）→ `staff.profile.employment_status_changed`（全部通过写入）；`staff.payroll.adjustment_requested` → `staff.payroll.adjustment_added`；`staff.payroll.adjustment_appeal_submitted` → `staff.payroll.adjustment_appeal_decided`（conclusion=upheld|revoked）＋ `staff.payroll.adjustment_reversed`（撤销冲回）。
`staff.registered`、`staff.approved`、`staff.rejected`、`staff.suspended`；`staff_device.bound`、`staff_device.rebind_requested`、`staff_device.rebound`、`staff_device.revoked`、`staff_device.integrity_failed`；`staff.notice_created`、`staff.notice_acknowledged`。

## 6. 权限点（D3，dot 风格，纳入 T13 全业务域最小颗粒度预置）
`staff.profile.submit/review/view`、`staff.device.bind/review/revoke`、`staff.notice.view`；CG-12 新增 `staff.hr.review`（人力会签复核：HR 岗位经角色持有，作为 `hr_review` 平行线会签人的资格码；出厂授 `role_platform_admin`）。

> **员工自助角色（V322，2026-07-19 补链）**：审批通过除建 `access_users` 外，为新员工分配角色 `role_staff_self`（data_scope=self），授予 `staff.payroll.view_mine / staff.shift.view / staff.attendance.punch / staff.leave.submit / staff.notice.view / staff.profile.self`。此前缺此分配 → 真实员工（userType=staff）无 `user_roles` 行 → BYOD 自助端点（我的薪资/班次/考勤/请假/通知/打卡）对真实员工一律 403，仅 platform_admin 可用。自助端点按 `actor.id()` 自锁本人，商户/项目隔离由 `access_users.merchant_id/organization_id` 驱动（AuthorizationService 不读 role.data_scope）。

---

## 7. 验收口径（GWT）
- **注册**：Given 员工提交注册，When 有 `staff.profile.review` 的上级审批通过，Then 生成 user_id+默认角色并发 approval_result 通知；驳回必须带 reason。
- **绑定/换机**：Given 员工已有 active 凭证，When 用新设备绑定，Then 新凭证 pending_review 且 supersedes 指向旧 active；审批通过后新 active、旧 superseded，`uq_staff_dev_cred_one_active` 始终成立。
- **完整性**：Given 端侧上报 root_detected，When reportIntegrity，Then 凭证 revoked + 发 integrity_failed + 通知。
- **隔离**：Given 商户角色，When 查 profiles/notices，Then 仅返回本 merchant_id 行（跨商户不可见）。
- **实体区分**：staff_device_credentials 与 terminal_device_credentials 无外键/无串值。

## 8. 测试计划
- 迁移应用：`MigrationVersions.latest()` 动态读最新版本，不硬钉具体版号；CHECK 约束拒绝非法枚举。
- 状态机：注册 approve/reject、换机 supersede 链、一员工仅一 active（唯一索引冲突用例）、root→revoke。
- 隔离：商户 scope 只见自身；平台 scope 见全量；越权读他户被拒。
- `verify:matrix-vs-migration`：实现后三表 absent→present 触发闸口 → 更新 claims.json。
- `verify:p0-contract`、`verify:openapi` 绿；后端测试全绿。

## 9. 依赖与下游
- 依赖：`access_users`（生成角色）、`staff_levels`（V055 职级）。
- **解锁 T9**：Android App 注册/Keystore 指纹/Root 检测/换机解绑直接对接本三表与上述端点；MDM 自有侧字段本期暂缓（仅接口+开关占位）。

## 10. 变更日志
| 日期 | 内容 | 依据 |
|---|---|---|
| 2026-06-18 | 新建 T1 员工三表可编码规格（V066 DDL+状态机+Service+API+事件+权限+验收+测试） | R5 backlog T1；裁决⑨；DDL 约定取自 V011/V055 |
| 2026-07-19 | 补 T1 缺口：审批通过分配"员工自助"角色 `role_staff_self`（V322，授自助权限点），修复真实员工（userType=staff）无 user_roles→BYOD 自助端点一律 403 | 既有缺口（非 Wave1 引入）；feature 分支 fix/byod-staff-self-service-role |
| 2026-07-19 | 修 authz 缺陷:`listNotices`（GET /notices，待办消息中心列表）实现漏 `@GuardedAction` 守卫（与 §4 API 表「staff.notice.view」承诺不符），且该注解被误扣在 `upsertShift` 上致排班多余要求 notice.view；已将 `staff.notice.view` 守卫归位到 listNotices，代码追平 §4 | 主计划会话审计发现；随 PR#180 一并修 |
| 2026-07-19 | 修 authz/PII 缺陷:`GET /staff/profiles` 实际调用的 `listProfiles(boolean)` 重载漏 `@GuardedAction`（守卫只加在无参重载、控制器未调它）→ 任何认证主体可列全员档案（含 phoneHash/idDocHash）；已给 boolean 重载补 `staff.profile.view` 守卫，代码追平 §4 | 主计划会话复核发现；随 PR#180 一并修 |
| 2026-08-02 | 新增按周批量排班 `POST /api/v1/staff/shifts/batch`（一批员工 × 勾中的星期几 × 每日同一时段，一次排完整周），跨零点班只填时段、班次结束自动顺延次日；单班 `POST /shifts` 与后台单班表单原样保留。§4 补两行接口与排班口径小节 | 2026-08-02 用户拍板「排班一般一次性排出一周的班，写清楚几月几日几点到几月几日几点」；feature 分支 feat/weekly-shift-batch-20260802 |
| 2026-10-17 | 版本升 V0.14；CG-12：员工状态任一变更与奖惩改为发起 `hr_three_levels_up` 档复核（主线＝该员工直属上级链前三级，平行线 hr_review＝HR 会签人）全部通过后写入/入账；新增员工扣罚申诉两端点、`staff.hr.review` 权限、`penalty_reversal` 冲回类型（迁移 V450，表 staff_employment_status_revisions / staff_payroll_adjustment_requests / staff_payroll_adjustment_appeals） | BSM:1478、BSM:1487、ENUM:283、PGM:658、PGM-1.7.1 |
| 2026-08-06 | 增 §4 跨月／跨周／跨年班次归属口径：工时、考勤、请假与年假额度/全勤等年度指标一律归**班次结束时点**所在月份与年度；法定节假日加班同按班次结束时点所在自然日整班认定、不拆半程；明确这是营业日归口清单里「考勤日期」一项的唯一例外，`work_date` 不构成归属月份 | 第四批草案 §F 落点 9/25/27（矛盾12 ＋ 矛盾11 联动）；用户 2026-08-06 裁决 |
