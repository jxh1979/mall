CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M038 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-21T12:56:13Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`，origin/main ebb1eb99。
- 模块 M038「功能权限点、职级三十档、权限模板与最终权限计算／网页与验证／第1段」16 个前端文件 2787 行，106 判据。B3 累计 0。16 个文件与 origin/main 逐字节相同。
- 顺序：Grok 已先派（modules/M038-b1-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何已跟踪文件；一次性 worktree 已删除；对库只做 SELECT；未调用任何会写的接口。
- 方法（网页模块）：①页面上每个会上屏的枚举取值，对后端的闭集（库内 CHECK 与服务层常量）逐键查中文字典；②每个按钮的权限码对 AccessController 的门控码；③每个写动作问「后端拒绝时用户看到什么」；④每个「读出来再写回去」的字段问「原样保存会不会改值」；⑤页面文案对 HTML 原句与后端现行行为。

## 1. 文件覆盖表

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| pages/AccessGovernancePage.tsx | 738 | 54011 | 76bf98c91ae0588ad66fd7be75b8810f3b012be1a8f6529e6a972d58eff72c08 | 全读 |
| pages/AccessGovernancePage.degraded.test.tsx | 61 | 3322 | b34d2ed33a973f52db7b18c04ffa313213b7548605394bdee86febbd7216b6f5 | 全读 |
| pages/AccessGovernancePage.guidedcreate.test.tsx | 113 | 6079 | c9978685e41352fdf40fe3846c64a3f1a250c02ac4597a4b4811771b77d2d5f3 | 全读 |
| pages/AccessGovernancePage.scopechange.test.tsx | 82 | 5060 | 84f86642cb900d037d68ad6179a8e5822cee51dd123078907d954342d33f37f6 | 全读 |
| pages/AccessGovernancePage.test.tsx | 185 | 10423 | ee160c7f86bd8a7c9087da4aea21ab82db17334295d796d7b719c070b2fe3b56 | 全读 |
| pages/AccessUsersPage.tsx | 370 | 22755 | e158cd17637d819a42468e17183e6403f8ad682bcb7bb630871364afb3a17151 | 全读 |
| pages/AccessUsersPage.metricdetail.test.tsx | 41 | 2121 | ff5caa1b72aac3f548dbd585aff4bc406cea98b5790c1ac1c63c72f20ddf04ca | 全读 |
| pages/AccessUsersPage.partnerbind.test.tsx | 113 | 5703 | 33d7f9d312be473c48638774bc2f344ab03c1022157c12d57089f3a32401d59d | 全读 |
| pages/AccessUsersPage.robustness.test.tsx | 45 | 2503 | fe77b21adf095eeff94014267cda5189c9387a1efd9faeb48308bb862b87ebd8 | 全读 |
| pages/AccessUsersPage.test.tsx | 92 | 4222 | 05f3a7f0093bb847a238e8e8160e0c919b3feb86398df43252cbcbc0f13f4836 | 全读 |
| pages/PermissionTemplatesPage.tsx | 548 | 27221 | 59ac645509458f2da463236804c0d431068fb72b82cd0765b87e233cca919a09 | 全读 |
| pages/PermissionTemplatesPage.guidedcreate.test.tsx | 116 | 6000 | e026609a0545a09f6013b258c8cc60a7f1bbd4fb53d340636f0917da4fa9b6d6 | 全读 |
| pages/PermissionTemplatesPage.test.tsx | 94 | 4393 | 3baf9ac47895584e48427fd91383d2826fe2f0117577172eecc400ec3d755d09 | 全读 |
| security/Can.tsx | 84 | 2545 | bfd3fbeb22a5fd1ee620b63c139ae0045fc36caeaa3afd9d2766edb11061d193 | 全读 |
| security/PermissionGate.tsx | 44 | 1502 | a15438dae20ba874cd4385edbd2a65cef85cda71724dd6cf10ce8e750d28a4a5 | 全读 |
| security/PermissionGate.test.ts | 61 | 2775 | 421c6e0e9de23cfab87b0d51f32535968aee73505b81d07c3f79793444d37ffe | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m038_b1_run.log）

```
START=2026-09-21T12:52:56Z HEAD=e456ba96 origin/main=ebb1eb99
== 0. files (lines/bytes/sha256 | 与 origin/main 是否相同)
AccessGovernancePage.tsx 738 54011 76bf98c91ae0588ad66fd7be75b8810f3b012be1a8f6529e6a972d58eff72c08 | same
AccessGovernancePage.degraded.test.tsx 61 3322 b34d2ed33a973f52db7b18c04ffa313213b7548605394bdee86febbd7216b6f5 | same
AccessGovernancePage.guidedcreate.test.tsx 113 6079 c9978685e41352fdf40fe3846c64a3f1a250c02ac4597a4b4811771b77d2d5f3 | same
AccessGovernancePage.scopechange.test.tsx 82 5060 84f86642cb900d037d68ad6179a8e5822cee51dd123078907d954342d33f37f6 | same
AccessGovernancePage.test.tsx 185 10423 ee160c7f86bd8a7c9087da4aea21ab82db17334295d796d7b719c070b2fe3b56 | same
AccessUsersPage.tsx 370 22755 e158cd17637d819a42468e17183e6403f8ad682bcb7bb630871364afb3a17151 | same
AccessUsersPage.metricdetail.test.tsx 41 2121 ff5caa1b72aac3f548dbd585aff4bc406cea98b5790c1ac1c63c72f20ddf04ca | same
AccessUsersPage.partnerbind.test.tsx 113 5703 33d7f9d312be473c48638774bc2f344ab03c1022157c12d57089f3a32401d59d | same
AccessUsersPage.robustness.test.tsx 45 2503 fe77b21adf095eeff94014267cda5189c9387a1efd9faeb48308bb862b87ebd8 | same
AccessUsersPage.test.tsx 92 4222 05f3a7f0093bb847a238e8e8160e0c919b3feb86398df43252cbcbc0f13f4836 | same
PermissionTemplatesPage.tsx 548 27221 59ac645509458f2da463236804c0d431068fb72b82cd0765b87e233cca919a09 | same
PermissionTemplatesPage.guidedcreate.test.tsx 116 6000 e026609a0545a09f6013b258c8cc60a7f1bbd4fb53d340636f0917da4fa9b6d6 | same
PermissionTemplatesPage.test.tsx 94 4393 3baf9ac47895584e48427fd91383d2826fe2f0117577172eecc400ec3d755d09 | same
Can.tsx 84 2545 bfd3fbeb22a5fd1ee620b63c139ae0045fc36caeaa3afd9d2766edb11061d193 | same
PermissionGate.tsx 44 1502 a15438dae20ba874cd4385edbd2a65cef85cda71724dd6cf10ce8e750d28a4a5 | same
PermissionGate.test.ts 61 2775 421c6e0e9de23cfab87b0d51f32535968aee73505b81d07c3f79793444d37ffe | same
== 1. 发布口径 vitest：origin/main 一次性 worktree
scratch at ebb1eb99
VITEST_EXIT(origin/main)=0
 ✓ src/admin/security/PermissionGate.test.ts (7 tests) 5ms
 ✓ src/admin/pages/AccessUsersPage.robustness.test.tsx (1 test) 39ms
 ✓ src/admin/pages/PermissionTemplatesPage.test.tsx (4 tests) 97ms
 ✓ src/admin/pages/AccessUsersPage.metricdetail.test.tsx (2 tests) 34ms
 ✓ src/admin/pages/AccessUsersPage.test.tsx (3 tests) 79ms
 ✓ src/admin/pages/AccessGovernancePage.scopechange.test.tsx (2 tests) 283ms
 ✓ src/admin/pages/PermissionTemplatesPage.guidedcreate.test.tsx (3 tests) 354ms
 ✓ src/admin/pages/AccessUsersPage.partnerbind.test.tsx (2 tests) 59ms
 ✓ src/admin/pages/AccessGovernancePage.degraded.test.tsx (1 test) 245ms
 ✓ src/admin/pages/AccessGovernancePage.guidedcreate.test.tsx (3 tests) 402ms
 ✓ src/admin/pages/AccessGovernancePage.test.tsx (10 tests) 628ms
 Test Files  11 passed (11)
scratch dirty: 0
scratch removed
== 2. 页面会显示的取值在 formatBusinessStatus 字典（origin/main 版）里有没有
user_role                        用户角色
user_position                    用户职位
temporary_permission             临时权限
role_permission                  （字典无此键）
user_scope_change                （字典无此键）
position_high_risk_release       （字典无此键）
pending                          待处理
applied                          （字典无此键）
rejected                         已驳回
staff                            普通员工
merchant                         商户员工
merchant_admin                   商户管理员
supervisor                       主管
sales                            销售人员
outsourced_vendor                外包账号
platform_admin                   平台管理员
manual_admin                     （字典无此键）
legacy                           （字典无此键）
invited                          待激活
disabled                         已禁用
active                           有效
inactive                         未激活
----
export function formatBusinessStatus(code: string | null | undefined): string {
  if (!code) {
    return "—";
  }
  return STATUS_LABELS[code] ?? code;
}
== 3. 库内闭集与现值（complex_test 只读；见 m038_checks.log）
access_grant_requests | chk_access_grant_requests_status | CHECK (((status)::text = ANY ((ARRAY['pending'::character varying, 'applied'::character varying, 'rejected'::character varying])::text[])))
access_grant_requests | chk_access_grant_requests_type | CHECK (((request_type)::text = ANY ((ARRAY['user_role'::character varying, 'user_position'::character varying, 'temporary_permission'::character varying, 'role_permission'::character varying, 'user_scope_change'::character varying, 'position_high_risk_release'::character varying])::text[])))
position_role_templates | chk_position_role_templates_status | CHECK (((status)::text = ANY ((ARRAY['active'::character varying, 'inactive'::character varying])::text[])))
user_permission_overrides | chk_user_permission_overrides_effect | CHECK (((effect)::text = ANY ((ARRAY['grant'::character varying, 'deny'::character varying])::text[])))
position_codes in position_role_templates: auditor_external, auditor_internal, outsourced_construction, outsourced_facility, outsourced_security_cleaning
position_codes in user_position_assignments: (none)
grant request types: (none)
psql:/home/james/audit-tmp/m038_checks.sql:8: ERROR:  column "level_code" does not exist
LINE 1: ...*) || ' rows; sample ' || string_agg(id || '=' || level_code...
                                                             ^
tables like position: position_role_templates, user_position_assignments
staff_levels columns: id, main_level, sub_level, level_rank, salary_policy_summary, status, created_at
staff_levels rows: 30
permission_templates.status distinct: active
access_users.created_via distinct: legacy
== 4. 权限模板编辑的时间往返（toDateTimeLocal 取 UTC 数字 → datetime-local 按本地时区解释）
Asia/Shanghai 库内 2026-06-14T00:00:00Z → 输入框显示 2026-06-14T00:00 → 原样保存发出 2026-06-13T16:00:00.000Z
UTC 库内 2026-06-14T00:00:00Z → 输入框显示 2026-06-14T00:00 → 原样保存发出 2026-06-14T00:00:00.000Z
== 5. AccessGovernancePage 里写动作的错误处理（函数名 | 是否有 try/catch）
   savePositionTemplate         写请求 2 处 | try/catch=有
   saveGrantPolicy              写请求 1 处 | try/catch=有
   changePositionLifecycle      写请求 1 处 | try/catch=有
   assignPosition               写请求 1 处 | try/catch=无
   changeUserScope              写请求 1 处 | try/catch=无
   roleAssignment               写请求 1 处 | try/catch=无
   userPermissionOverride       写请求 1 处 | try/catch=无
   applyGrantRequest            写请求 1 处 | try/catch=有
   reviewGrant                  写请求 1 处 | try/catch=无
== 6. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T12:53:00Z
```

## 3. 三线审计

- 第一性原理线。用途：让平台管理员在后台完成岗位模板映射、定岗、范围变更、角色装配、员工级授予/拒绝、授权申请与审批、授权政策、账号开通、权限模板维护。实际行为：主流程可用，测试 38 个全绿。未经验证的前提：「后端给的每个取值页面都有中文」「后端拒绝时用户看得到原因」「编辑后原样保存不改数据」——三条都不成立。
- 顺从式线：106 判据逐项见 §4；16 个文件全读。
- 对抗式线（grilling）：Q 审批通过后状态显示什么？A 后端闭集是 pending/applied/rejected，字典没有 applied，徽标、抽屉、结果提示都直接显示「applied」。Q 本页自己发起的范围变更，回到审批表里类型显示什么？A `user_scope_change`，字典无此键，显示英文。Q 直接给人装配一个含高危码的角色会怎样？A 后端 403「以下权限的授予需要审批」，`roleAssignment` 没有 try/catch，页面毫无反应。Q 在北京时间打开一个有生效时间的模板、只改名字就保存会怎样？A 输入框里是 UTC 的数字，保存时按本地时间解释，生效时间早了 8 小时，后端按「放宽有效期」以 409 拒绝；只有截止时间的模板则每保存一次悄悄缩短 8 小时。Q 「恢复为停用模板」点了以后真是停用吗？A 不是，M031-001 之后恢复即回到启用态、立刻可定岗。

## 4. 106 判据逐项

| 判据 | 适用｜证据｜结论 |
|---|---|
| BEQR-063 高危动作（需审批/双证据） | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「高危动作（需审批/双证据）」的对象或文字 |
| BEQR-074 隐私桌台权限 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「隐私桌台权限」的对象或文字 |
| BEQR-143 四类主体 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「四类主体」的对象或文字 |
| BEQR-144 经营主体关系 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「经营主体关系」的对象或文字 |
| BEQR-145 城市项目公司 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「城市项目公司」的对象或文字 |
| BEQR-146 运营公司职责 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「运营公司职责」的对象或文字 |
| BEQR-147 主要收入归属 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「主要收入归属」的对象或文字 |
| BEQR-148 合作模式 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「合作模式」的对象或文字 |
| BEQR-149 职级 | 适用｜职级显示为「M<级>·上/中/下」（`zhLevel`），与速查表「M1–M10，每级分上 / 中 / 下（共 30 档）」一致，有测试；库内 30 档｜通过 |
| BEQR-150 终端十一端 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「终端十一端」的对象或文字 |
| BEQR-151 最终权限计算 | 适用｜最终权限公式的五个来源在治理页与模板页各有写入口：直接装配角色、岗位模板映射与定岗、权限模板（基础/临时/单次）、员工单独授予、显式拒绝｜通过 |
| BEQR-152 项目与敏感数据权限 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「项目与敏感数据权限」的对象或文字 |
| BEQR-153 终端开关优先级 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「终端开关优先级」的对象或文字 |
| BEQR-154 综合体项目状态 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「综合体项目状态」的对象或文字 |
| BEQR-155 商户/运营主体状态 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「商户/运营主体状态」的对象或文字 |
| BEQR-156 合同状态 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「合同状态」的对象或文字 |
| BEQR-157 员工状态 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「员工状态」的对象或文字 |
| BEQR-158 设备状态 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「设备状态」的对象或文字 |
| BEQR-159 权限状态 | 适用｜速查表「权限状态：草稿、已启用、暂停、已停用、归档」。权限模板页只读显示状态，没有改状态的入口；后端也没有改权限模板状态的接口、库内现值只有 active。页面无从提供，属后端模块的事，登记 OBS-5｜通过（登记 OBS-5） |
| BEQR-166 合同审批链 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「合同审批链」的对象或文字 |
| BEQR-182 考勤日结果 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「考勤日结果」的对象或文字 |
| BEQR-183 打卡方式 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「打卡方式」的对象或文字 |
| BEQR-184 请假类型 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「请假类型」的对象或文字 |
| BEQR-185 请假单状态 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「请假单状态」的对象或文字 |
| BEQR-186 薪资单构成 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「薪资单构成」的对象或文字 |
| BEQR-187 员工提成发放 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「员工提成发放」的对象或文字 |
| BEQR-189 管理费方式 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「管理费方式」的对象或文字 |
| BSM-001 整体说明：这套系统到底管什么 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「整体说明：这套系统到底管什么」的对象或文字 |
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | 适用｜「权限分岗」：每个写按钮经 `PermissionButton` 按权限码禁用并拦截点击，权限码与 AccessController 各端点的门控码逐个核对一致（含删除审批的双权限、授权政策保存的 admission.policy.manage）；「操作有引导」：五个写动作失败时页面没有任何提示（CX-M038-003）｜不通过（CX-M038-003） |
| BSM-004 配置数据可以存档、也可以删除（删除要审批） | 适用｜存档、申请删除、批准/驳回删除、恢复的入口齐全，理由必填，最高权限本人显示「最高权限直接删除」；但恢复按钮写的是「恢复为停用模板」，而业务地图原文是「想恢复点"取消存档"即可」，M031-001 之后恢复即回到启用态（CX-M038-002）｜不通过（CX-M038-002） |
| BSM-005 后台的"新建/录入"改成分步引导 | 适用｜新增岗位角色模板、发起授权申请、新建员工账号、新建权限模板四处为「＋按钮→分步引导弹窗→确认」，当步校验、失败留在弹窗内显错，有测试｜通过 |
| BSM-006 验证用的二维码一律动态刷新（间隔可在后台设定） | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「验证用的二维码一律动态刷新（间隔可在后台设定）」的对象或文字 |
| BSM-007 平台管理员拥有全部日常功能权限 | 适用｜前端不自行给平台管理员放行：`hasPermissions` 只认后端给的 grants/denies/wildcard，显式拒绝压过 wildcard（有测试）；后端现恒给 wildcard=false，该分支不会被走到｜通过 |
| BSM-009 基础管理 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「基础管理」的对象或文字 |
| BSM-028 主要功能 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「主要功能」的对象或文字 |
| BSM-029 和谁有关 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「和谁有关」的对象或文字 |
| BSM-072 商户管理：商户在授权范围内完整使用自己的系统 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「商户管理：商户在授权范围内完整使用自己的系统」的对象或文字 |
| BSM-073 主要功能 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「主要功能」的对象或文字 |
| BSM-074 和谁有关 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「和谁有关」的对象或文字 |
| BSM-084 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「员工手机端：排班、考勤、请假、薪资，自己手机上都能办」的对象或文字 |
| BSM-085 日常工作 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「日常工作」的对象或文字 |
| BSM-086 排班、考勤与请假 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「排班、考勤与请假」的对象或文字 |
| BSM-087 我的薪资 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「我的薪资」的对象或文字 |
| BSM-088 员工提成、活动执行与人员服务边界 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「员工提成、活动执行与人员服务边界」的对象或文字 |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | 适用｜无权限的副资源不请求、各自降级（总结、权限模板、用户/角色清单），主表照常，有测试｜通过 |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「相互关系：为什么这些子系统必须连在一起」的对象或文字 |
| BSM-120 已确认规则和实施前输入项 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「已确认规则和实施前输入项」的对象或文字 |
| GEN-001 必要性与最小性 | 适用｜三个页面无重复入口；`hasPermissions` 的 wildcard 分支现不会被走到但属与后端契约字段，保留｜通过 |
| GEN-002 可执行正确性 | 适用｜发布口径（origin/main 一次性 worktree）11 个测试文件 38 个测试全绿（探针 1）｜通过 |
| GEN-003 测试有效性 | 适用｜测试用真实 AdminApi＋打桩的 fetch 渲染并操作页面，断言请求体与上屏文字，能失败；但本次四条缺陷对应的行为都没有测试：已生效/范围变更等取值的中文、恢复按钮文案、五个写动作失败的提示、编辑保存的时间往返（随各条处置补）｜不通过（随 CX-M038-001…004） |
| GEN-004 信任边界校验 | 适用｜必填与当步校验在页面侧先拦，后端再校验｜通过 |
| GEN-005 授权与最小权限 | 适用｜见 BSM-003：前端权限码与后端门控码一致；无权限按钮禁用且拦截点击｜通过 |
| GEN-006 数据完整性 | 适用｜权限模板编辑保存把 UTC 时间的数字放进按本地时区解释的输入框，原样保存即改写了时间（CX-M038-004）｜不通过（CX-M038-004） |
| GEN-007 并发与幂等 | 适用｜写请求由客户端统一带幂等键，401 刷新重放沿用同一键；引导弹窗提交期间按钮置忙｜通过 |
| GEN-008 接口与模式一致性 | 适用｜请求路径、方法、字段名（客户端统一转 snake_case）与 AccessController 映射及请求记录类逐个核对一致；模板应用 mode 用 single_use/append，与后端实际取值一致｜通过 |
| GEN-009 错误处理与恢复 | 适用｜读失败、副资源失败有降级；五个写动作无 try/catch，后端拒绝时成为未处理的 Promise 拒绝，页面无任何提示（CX-M038-003）｜不通过（CX-M038-003） |
| GEN-010 安全与敏感信息 | 适用｜初始密码输入为 password 类型；不显示手机号｜通过 |
| GEN-011 可观测与审计证据 | 适用｜每个写请求带 operatorNote；审批意见为空时页面把英文 approve/reject 当作意见与备注发给后端留痕（CX-M038-001③）｜不通过（CX-M038-001） |
| GEN-012 资源与性能边界 | 适用｜授权申请分页读取（每页 50），其余清单一次读取｜通过 |
| GEN-013 构建配置与依赖 | 适用｜依赖 React、antd Tabs 与本仓公共组件；无新增依赖｜通过 |
| GEN-014 数据库迁移正确性 | 无本面｜本模块不涉迁移 |
| GEN-015 界面可用性与可访问性 | 适用｜行可键盘聚焦与回车展开、表单控件有 aria-label；智能总结表表头「详情」重复一列，表头 7 列而每行 6 格（CX-M038-005）；界面文字见 CX-M038-001｜不通过（CX-M038-001、CX-M038-005） |
| GEN-016 文档可验证性 | 适用｜页面说明文字与实际功能逐句对照：恢复按钮文案与实际行为相反（CX-M038-002）；员工账号页标题说明提到「企业微信同步结果」「禁用」而页内无此内容（OBS-4）｜不通过（CX-M038-002） |
| GEN-017 业务来源分类 | 适用｜未见页面自造业务规则；岗位码中文名是页内 6 项小映射（OBS-1）｜通过（登记 OBS-1） |
| GEN-018 第一性原理证据 | 适用｜结论落到页面源码行、HTML 原文、后端门控与库内闭集的只读查询、vitest 与 node 实跑输出｜通过 |
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「整体定位：平台治理层不是一个业务部门，而是所有业务的底座」的对象或文字 |
| PGM-002 治理顺序：一件业务能不能做，要先经过这些判断 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「治理顺序：一件业务能不能做，要先经过这些判断」的对象或文字 |
| PGM-003 模块入口：平台治理层包含哪些管理模块 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「模块入口：平台治理层包含哪些管理模块」的对象或文字 |
| PGM-004 综合体和集团 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「综合体和集团」的对象或文字 |
| PGM-005 空间和点位 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「空间和点位」的对象或文字 |
| PGM-006 商户和主体 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「商户和主体」的对象或文字 |
| PGM-007 合同和准入 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「合同和准入」的对象或文字 |
| PGM-008 员工和手机 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「员工和手机」的对象或文字 |
| PGM-009 权限和职级 | 适用｜权限和职级模块的三个后台页：访问治理工作台、员工账号、权限模板｜通过 |
| PGM-010 终端开关 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「终端开关」的对象或文字 |
| PGM-011 设备管理 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「设备管理」的对象或文字 |
| PGM-012 商户侧 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「商户侧」的对象或文字 |
| PGM-013 开放接口 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「开放接口」的对象或文字 |
| PGM-014 留痕复核 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「留痕复核」的对象或文字 |
| PGM-015 楼宇运营 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「楼宇运营」的对象或文字 |
| PGM-016 应急与隐私 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「应急与隐私」的对象或文字 |
| PGM-017 综合体和集团：当前一个项目，未来能看集团 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「综合体和集团：当前一个项目，未来能看集团」的对象或文字 |
| PGM-018 物理项目与运营公司：多对多协作，不是固定隶属层级 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「物理项目与运营公司：多对多协作，不是固定隶属层级」的对象或文字 |
| PGM-019 各主体具体负责什么 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「各主体具体负责什么」的对象或文字 |
| PGM-020 主要功能 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「主要功能」的对象或文字 |
| PGM-021 业务意义 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「业务意义」的对象或文字 |
| PGM-022 空间和点位：所有业务都要知道“发生在哪” | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「空间和点位：所有业务都要知道“发生在哪”」的对象或文字 |
| PGM-024 影响业务 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「影响业务」的对象或文字 |
| PGM-026 主要功能 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「主要功能」的对象或文字 |
| PGM-027 业务边界 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「业务边界」的对象或文字 |
| PGM-031 外部经销商主体、合同价格与资金治理 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「外部经销商主体、合同价格与资金治理」的对象或文字 |
| PGM-032 员工和手机：人、私人手机、工作手机要能对应起来 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「员工和手机：人、私人手机、工作手机要能对应起来」的对象或文字 |
| PGM-033 主要功能 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「主要功能」的对象或文字 |
| PGM-034 工作手机和企业微信 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「工作手机和企业微信」的对象或文字 |
| PGM-035 提成发放和人员服务治理 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「提成发放和人员服务治理」的对象或文字 |
| PGM-036 设备管理：谁领了什么设备，设备能不能用 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「设备管理：谁领了什么设备，设备能不能用」的对象或文字 |
| PGM-040 权限和职级：每一个单一功能都可以单独授权 | 适用｜权限点逐个勾选（按菜单模块分组），角色级模板与员工级单独授予/显式拒绝均有入口｜通过 |
| PGM-041 权限规则 | 适用｜「临时授权可设有效期或单次使用」有入口；「高危授权审批」经授权申请与审批表；「临时权限…系统自动总结」有智能总结表与政策配置。界面文字须全中文而授权申请的状态「applied」与三种申请类型直接显示英文（CX-M038-001）；权限模板编辑保存会把生效/截止时间整体前移 8 小时（CX-M038-004）｜不通过（CX-M038-001、CX-M038-004） |
| PGM-042 职级规则 | 适用｜职级按 M1–M10 上中下显示；定岗表单可填基础薪资（「具体薪资单独填写」）｜通过 |
| PGM-043 外包人员与审计人员（2026-06-29 新增） | 适用｜员工走注册申请、后台手动建号只留外部协作机构账号，并可绑定/解绑外部协作机构；与后端 STAFF_REGISTRATION_REQUIRED 一致｜通过 |
| PGM-044 桌台、挂单与经销业务权限 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「桌台、挂单与经销业务权限」的对象或文字 |
| PGM-048 商户侧：在授权业务范围内全开放 | 适用｜商户管理员创建账号时后端按其商户收口；页面对平台账号提供「不绑定 / 平台账号」｜通过 |
| PGM-059 客人与员工的个人信息保护 | 适用｜账号表与详情抽屉不显示手机号（有测试断言）｜通过 |
| PGM-060 会员身份、营销与生物信息治理 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「会员身份、营销与生物信息治理」的对象或文字 |
| PGM-065 连接关系：平台治理层怎么连接所有业务 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「连接关系：平台治理层怎么连接所有业务」的对象或文字 |
| PGM-066 已拍板规则和待排期问题 | 适用｜「模板内既有高危权限不因在模板内而免审」在范围变更处有中文说明；直接装配高危角色被后端以须审批拒绝时，页面不提示（CX-M038-003）｜不通过（CX-M038-003） |

## 5. 本次 Findings

### CX-M038-001（冲突；工程缺陷成立）授权申请的状态与类型、账号开通方式、默认审批意见直接以英文上屏／留痕

- 位置：①`pages/AccessGovernancePage.tsx` L88 `statusBadge`、L418 与 L572 `formatBusinessStatus(row.requestType)`、L423 `formatBusinessStatus(row.status)`、L374 与 L393 结果提示里的 `formatBusinessStatus(result.status)`；②`pages/AccessUsersPage.tsx` L182 `formatBusinessStatus(row.createdVia)`；③`AccessGovernancePage.tsx` L389-390 `comment: reviewForm.comment.trim() || decision`、`operatorNote: … || decision`（decision 为 "approve"/"reject"）。
- 证据：`formatBusinessStatus` 查不到键时原样返回英文码（探针 2 末尾）。后端闭集（探针 3）：授权申请状态 pending／applied／rejected，类型六种；字典里没有 `applied`、`role_permission`、`user_scope_change`、`position_high_risk_release`，也没有开通方式的 `manual_admin`（AccessService 写入）与 `legacy`（库内现值全部是它）。现有测试只用了 pending 与 temporary_permission 两个取值。
- 正常触发：审批通过任何一张授权申请，状态即为 applied；在本页提交一次人员范围变更、或给人定一个含高危权限的岗位，审批表里就出现 user_scope_change／position_high_risk_release；点开任何一个账号的详情，「开通方式」显示 legacy；审批时不填意见，留痕里的意见就是英文 approve。
- 依据：治理地图「展示给用户的一切文字…必须全部为中文，不得出现任何英文」，可以不用中文的只有四样（单据号、设备编号、二维码内容、系统内部标识符），枚举取值不在其列；PGM-041、GEN-015、GEN-011。先例 M035-001（同一句原文，定为冲突）。
- 最小拟处置（只改本模块文件；公共字典 customer/i18n/statusLabels.ts 属别的模块，不动）：沿用本模块已有的「页内小映射」写法（PermissionTemplatesPage 的 TEMPLATE_TYPE_LABELS）——治理页为授权申请状态与类型各加一张页内映射，覆盖后端闭集的全部取值（applied→已生效、role_permission→角色权限、user_scope_change→人员范围变更、position_high_risk_release→岗位高危权限释放，其余沿用字典现有中文）；账号页为开通方式加页内映射（manual_admin→后台手动录入、legacy→历史账号）；默认审批意见改为「通过」／「驳回」。补测试：三种状态、六种类型、两种开通方式上屏均为中文；不填意见时请求体里的意见为中文。

### CX-M038-002（冲突；工程缺陷成立）恢复按钮写「恢复为停用模板」，与业务地图原句和后端现行行为相反

- 位置：`pages/AccessGovernancePage.tsx` L487：`…onClick={() => void changePositionLifecycle(row.id, "restore")}>恢复为停用模板</PermissionButton>`。
- 证据：业务地图 L566 起「存档（先下线）…想恢复点"取消存档"即可」；后端 M031-001（已发布）：restore 后映射回到 active、立刻可定岗（AccessService 在 restore 分支把状态写回 active；M037 的回归测试断言 `status == "active"`）。
- 正常触发：管理员存档一条岗位映射后想恢复，看到的按钮说会「恢复为停用」，实际一点就重新启用并可被定岗选用。
- 依据：BSM-004、GEN-016。
- 最小拟处置：按钮文案按状态分两种——已存档未删除的写「取消存档」，已删除的写「恢复」；补测试断言两种文案且不再出现「停用模板」。

### CX-M038-003（技术性；工程缺陷成立）五个写动作没有错误处理，后端拒绝时页面毫无提示

- 位置：`pages/AccessGovernancePage.tsx` `assignPosition`（L265-280）、`changeUserScope`（L284-309）、`roleAssignment`（L311-324）、`userPermissionOverride`（L328-343）、`reviewGrant`（L386-395）；调用处均为 `onClick={() => void fn()}`，`PermissionButton`（security/Can.tsx L76-79）与客户端都不兜底，仓内也没有全局的 unhandledrejection 处理。同页其余四个写动作都有 try/catch 并 `setGlobalError`（探针 5）。
- 正常触发：直接装配一个含高危码的角色 → 后端以 GRANT_REQUIRES_APPROVAL「以下权限的授予需要审批」拒绝；给别的组织的人定岗、审批自己受益的申请、范围没有变化的范围变更，后端同样会拒绝。页面既不报错也不提示成功，操作人无从知道被拒及原因。
- 依据：GEN-009；BSM-005「哪一步填得不对当步就提示」；PGM-066（高危被拒须让操作人知道要走授权申请）。
- 最小拟处置：五个函数各自 try/catch，失败时 `setGlobalError(后端中文消息，缺省为各自的中文兜底句)`；补测试：装配角色与审批两处各打桩一次 403，断言 `setGlobalError` 收到消息。

### CX-M038-004（技术性；工程缺陷成立）权限模板「选中编辑」把 UTC 时间的数字填进本地时区的输入框，原样保存即把生效/截止时间前移

- 位置：`pages/PermissionTemplatesPage.tsx` L42-47 `toDateTimeLocal`（`date.toISOString().slice(0, 16)`）与 L49-53 `toIsoOrUndefined`（`new Date(value).toISOString()`），L120-121 回填、L261-262 保存。
- 证据：探针 4——北京时间下，库内 2026-06-14T00:00:00Z → 输入框显示 2026-06-14T00:00 → 原样保存发出 2026-06-13T16:00:00.000Z。后端 `updatePermissionTemplate`：生效时间早于原值或截止时间晚于原值即 409 TEMPLATE_REAUTHORIZATION_REQUIRED。
- 正常触发：运营在北京时间选中一个带生效时间的模板，只改名称或增减权限后保存 → 409「有效期…变化须复制为新模板后重新授权」，改不了；模板只有截止时间时保存成功，但截止时间每保存一次早 8 小时，授权悄悄提前到期。
- 依据：GEN-006、PGM-041「临时授权可设有效期」。
- 最小拟处置：`toDateTimeLocal` 改为按本地时区取年月日时分拼出输入框的值，使「读出→不改→保存」得到同一时刻；补测试：在测试内把时区设为 Asia/Shanghai，选中带生效/截止时间的模板直接保存，断言请求体里的两个时间与库内原值是同一时刻。

### CX-M038-005（技术性；工程缺陷成立）智能总结表表头多一列

- 位置：`pages/AccessGovernancePage.tsx` L620：`<th>建议</th><th>详情</th><th>详情</th>`，而 L622、L623 每行只有 6 格。
- 正常触发：打开「③ 智能总结」即见表头 7 列、数据 6 列，最后一列表头悬空。
- 依据：GEN-015。
- 最小拟处置：删去重复的一个 `<th>详情</th>`；补测试断言表头列数等于首行单元格数。

## 6. 观察与登记（不立项）

- OBS-1：岗位码的中文名是页内 6 项映射（`POSITION_LABELS`），其余岗位码原样显示；库里没有岗位字典表，岗位码由管理员在「岗位编码」里自由填写（本模块测试里就填了 front_desk）。岗位码属系统内部标识符，列入治理地图的四样例外，不立项。
- OBS-2：岗位编码、职级、所属组织、目标员工、权限模板等处让操作人手填内部编号，而职级清单、模板清单、组织清单页面其实拿得到；HTML 未规定这些字段须为下拉，不为此补需求。
- OBS-3：`hasPermissions` 的 wildcard 分支在后端恒给 false 后不会被走到；属与后端的契约字段。
- OBS-4：员工账号页标题说明提到「企业微信同步结果」与「禁用」，页内没有这两样（后端 access 接口也没有停用账号的端点；员工状态在员工档案页）。
- OBS-5：速查表「权限状态」五态，后端无改状态接口、库内权限模板现值只有 active，页面只读显示；属后端模块。
- OBS-6：PermissionTemplatesPage.tsx、Can.tsx、PermissionGate.tsx 及各测试文件没有「文件版本」行，另两个页面有。
- OBS-7：岗位模板状态在下拉里叫「启用／停用」，在表格与抽屉里经公共字典显示为「有效／未激活」；用词不一，字典属别的模块。

## 7. 结论

席甲 B1 票：**FAIL**（CX-M038-001…005；OBS-1…7 登记）。16 个文件 2787 行全读、106 判据逐项、三线完成；未改任何已跟踪文件；落盘前未读 Grok 本次正文。


## M038 B2-r1 整批候选复核（席甲票，V0.02）

- 时间：2026-09-21T13:15:25Z。输入：modules/M038-b1-grok-raw.md V0.01（43552 bytes，SHA256 56eec0788741bc116816514112adfadd924790ec8b4854867deefc29e094c8bc）、modules/M038-b1-self-raw.md V0.01（上方本报告）；候选 findings.md 顶部「当前 M038 B2-r1 整批候选」段（全文件 SHA256 10153c406c987ab2becb402100a3c1e08edb68a3a9f77e6cf4a6d23ce0f1582f）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：CX-001＋GR-003＋GR-002→001；GR-004→002；GR-005→003；CX-002→004；CX-003＋GR-006＋GR-007→005；CX-004→006；CX-005＋GR-011→007；GR-001→008；GR-009＋OBS-7→009；GR-008＋GR-010→010；两席其余观察登记；无漏并、无改写。
- 自审纠正：①上方 B1 报告 OBS-1 以「岗位码属系统内部标识符」为由不立项——页面自己已把「岗位」列当给人看的文字做了中文映射，未映射时漏出英文属同一句 HTML 原文管的事，改判。②OBS-2 把职级与其他手填编号混在一起记成「HTML 未规定须为下拉」——职级是速查表写明的 30 档闭集，与岗位编码、组织编号不同，改判。③漏了模板类型 role（席甲查了授权申请与账号两处的库内现值，没去查权限模板的）、字段名里的 id／ID（只盯取值没盯字段名，而 L287 明列「字段名」）、双击重复提交、权限闸组件与发起授权申请的测试缺口。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。


## M038 B2-r2 候选复核（席甲票，V0.03）

- 时间：2026-09-21T14:15:13Z。输入：两份 B5-r1 raw 与候选 findings.md 顶部「当前 M038 B5-r1 合取 FAIL，B2-r2 候选」段（全文件 SHA256 dbb1dcb4a6d36cd6d802796c4499c565ea2deaee2405e03b22beba9288b9d1db）。本票落盘前未读 Grok B2-r2 追加段。不写独占锁行。
- GR-M038-012→M038-011，采 Grok 的技术性分类与最小拟处置；同根复查本模块其余写按钮，只剩复制与应用两处。
- 自审纠正：席甲 B5-r1 的探针 3 只扫了治理页的写请求调用点，对账号页、模板页凭 B1 通读时的印象写了「各有置忙或二次确认」，没有同样扫一遍——「复制模板」只有 try/catch。005 定稿时席甲也只按 Grok 指到的那一页收窄，没按「同模式先 grep 全单元集」把另外两页的写按钮一并过一遍。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r2 票：**PASS**。


CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458

# M038 现锁 V0.14 B1 重开 · 席甲完整报告
版本：V0.04（本段追加；上方 V0.01–V0.03 前缀 46328 字节原样保留，为旧锁 V0.11 下的 B1、B2-r1、B2-r2 报告，其票不复用）

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；本模块 8 个文件里的 001…010 修复为席甲旧锁 B3-r1 所写）。2026-09-23T17:03:38Z
- 锁 `b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458`（现行 V0.14；实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`，origin/main 53dec141。
- 为何重开（D-030）：M038 在旧锁 V0.11 下走到 B2-r2（Grok B2-r2 票 PASS、席甲票 PASS，M038-011 待修）；其后别的线程按 §5 闸 2 完成 LOCK-AMEND，锁 V0.11→V0.14，定义正文变更的 6 个 ID 里 BSM-114、BSM-120、PGM-043、PGM-059 在本模块 106 项映射内（映射行与 general 未变，探针 0b）。依 §5 闸 2 ⑤，M038 改回 PENDING 并按现锁从 B1 重开；保留 B3 累计 1、已落地的 001…010 修复与旧锁全部 raw（只作历史）。
- 审计对象：16 个文件现状（origin/main＋旧锁 B3-r1 的 001…010 修复，主工作区未提交，与 origin/main 的差异＝/home/james/audit-tmp/m038_b3r1.diff，探针 1 核对逐字节相同）合计 3210 行；M038-011 尚未修、B1 期间不修，作为既有输入如实报告。
- 顺序：本席取证脚本先跑完，随后才派 Grok；本报告落盘前未读 Grok 本次追加段（modules/M038-b1-grok-raw.md 现 83469 bytes，未超出旧前缀 83469 bytes；席甲只看大小、未读正文）。只读承诺：未改任何已跟踪文件；一次性 worktree 已删除；对库只做 SELECT；未调用任何会写的接口。
- 方法：同旧锁 B1 的五条（取值逐键对闭集、权限码对门控码、写动作问失败提示、读出再保存问保值、文案对 HTML 与后端行为），另加：现锁 V0.14 变更的 4 个 ID 逐条拿新增/改动的原句去对三个页面（探针 6）；写按钮忙态逐页清单（探针 4，旧锁 B5-r1 漏掉「复制模板」的教训）。

## 1. 文件覆盖表（现状；行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| pages/AccessGovernancePage.tsx | 777 | 57255 | d7668403ba673b8375a541f24220397d19f2cb92d345392cf63f4100dc94b101 | 全读（现状＝origin/main＋旧锁 001…010 修复） |
| pages/AccessGovernancePage.degraded.test.tsx | 61 | 3322 | b34d2ed33a973f52db7b18c04ffa313213b7548605394bdee86febbd7216b6f5 | 全读（现状＝origin/main＋旧锁 001…010 修复） |
| pages/AccessGovernancePage.guidedcreate.test.tsx | 191 | 11207 | 521dc934d768003006e329b7728985d795e3006437d905c7bb201af04da48871 | 全读（现状＝origin/main＋旧锁 001…010 修复） |
| pages/AccessGovernancePage.scopechange.test.tsx | 82 | 5060 | 84f86642cb900d037d68ad6179a8e5822cee51dd123078907d954342d33f37f6 | 全读（现状＝origin/main＋旧锁 001…010 修复） |
| pages/AccessGovernancePage.test.tsx | 328 | 19760 | bcfe27b0f09a09c0b19f6a0f92472eac841cd64559d17172196b008e9005b327 | 全读（现状＝origin/main＋旧锁 001…010 修复） |
| pages/AccessUsersPage.tsx | 374 | 23060 | b4ae66d2735e0c20b28080ea36c3ff2a472c5dcb9cd540696f0f10215f9f230d | 全读（现状＝origin/main＋旧锁 001…010 修复） |
| pages/AccessUsersPage.metricdetail.test.tsx | 41 | 2121 | ff5caa1b72aac3f548dbd585aff4bc406cea98b5790c1ac1c63c72f20ddf04ca | 全读（现状＝origin/main＋旧锁 001…010 修复） |
| pages/AccessUsersPage.partnerbind.test.tsx | 113 | 5703 | 33d7f9d312be473c48638774bc2f344ab03c1022157c12d57089f3a32401d59d | 全读（现状＝origin/main＋旧锁 001…010 修复） |
| pages/AccessUsersPage.robustness.test.tsx | 45 | 2503 | fe77b21adf095eeff94014267cda5189c9387a1efd9faeb48308bb862b87ebd8 | 全读（现状＝origin/main＋旧锁 001…010 修复） |
| pages/AccessUsersPage.test.tsx | 136 | 6905 | d5c22dfcc87a87e06a01fd28bc6c428984f716cd4e9ef675e24c4dbb39cf42b9 | 全读（现状＝origin/main＋旧锁 001…010 修复） |
| pages/PermissionTemplatesPage.tsx | 552 | 27684 | 4b31fa7e991e6c540e10930767bc29f4f31d6ec4f02a661bb42a49cade64a67d | 全读（现状＝origin/main＋旧锁 001…010 修复） |
| pages/PermissionTemplatesPage.guidedcreate.test.tsx | 116 | 6000 | e026609a0545a09f6013b258c8cc60a7f1bbd4fb53d340636f0917da4fa9b6d6 | 全读（现状＝origin/main＋旧锁 001…010 修复） |
| pages/PermissionTemplatesPage.test.tsx | 153 | 8294 | 88e1f50b434b093fd4e494c6529c6528a7d83c6d7a4a452009cf1c783af894ec | 全读（现状＝origin/main＋旧锁 001…010 修复） |
| security/Can.tsx | 84 | 2545 | bfd3fbeb22a5fd1ee620b63c139ae0045fc36caeaa3afd9d2766edb11061d193 | 全读（现状＝origin/main＋旧锁 001…010 修复） |
| security/PermissionGate.tsx | 44 | 1502 | a15438dae20ba874cd4385edbd2a65cef85cda71724dd6cf10ce8e750d28a4a5 | 全读（现状＝origin/main＋旧锁 001…010 修复） |
| security/PermissionGate.test.ts | 113 | 6276 | 14f28de8ffc8544fd26f9a5843e156ecb7dd315591375fb51af827185d4ccbfc | 全读（现状＝origin/main＋旧锁 001…010 修复） |

## 2. 可执行证据原文（/home/james/audit-tmp/m038_n1_run.log）

```
START=2026-09-23T16:59:09Z HEAD=e456ba96 origin/main=53dec141
== 0. 锁：现行 vs sidecar vs ledger 首行；LOCK-AMEND-COUNT
b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
LOCK-AMEND-COUNT: 1
版本：V0.14
== 0b. 锁 V0.11（ebb1eb99）→ 现行 逐 ID 差异（M038 映射内的标 True）
IDs old/new: 401 401 | M038 mapping: 106
changed: ['BSM-044', 'BSM-114', 'BSM-120', 'PGM-043', 'PGM-047', 'PGM-059']
added: []
removed: []
--- BSM-044 酒吧平板/工作手机：平板点酒，工作手机核销 | in M038: False | -1 +2
   - 平板、公司工作手机、员工私人手机 App 是不同设备和不同用途。平板只做酒吧点酒下单；公司工作手机用于扫客人二维码核销门票和权益，并通过企业微信承接员工工作沟通；员工私人手机装的是同一个工作 App，按设备放权：注册、看公告、查自己的工单、帮客人订位、看排班、考勤打卡、请假、查薪资这些可离场办理的事，在自己手机上就能办；必须在现场完成的操作（比如服务点单的代客下单、出示公司收款码、完成履约、扫码核
   + - 铁律4批准依据：操作员逐句批准的业务地图V0.14 L930/L1865，PR #432（本线程Claude交接）；治理地图V0.12 L741，2026-09-11“同意按此修改”、PR #433（本线程交接及findings M013-008）。仅同步已批准原文，不修改HTML。
   + 平板、公司工作手机、员工私人手机 App 是不同设备和不同用途。平板只做酒吧点酒下单；公司工作手机用于扫客人二维码核销门票和权益，并通过企业微信承接员工工作沟通；员工私人手机装的是同一个工作 App，按设备放权：注册、看公告、查自己的工单、帮客人订位、看排班、查看考勤、请假、查薪资这些可离场办理的事，在自己手机上就能办；必须在现场完成的操作（比如服务点单的代客下单、出示公司收款码、完成履约、扫码核
--- BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | in M038: True | -1 +2
   - 注册、提交资料审批、收公告通知、查自己的工单、帮客人订位、看排班、考勤打卡、请假、查薪资等可离场办理的事；必须在现场完成的操作（代客下单、出示公司收款码、完成履约、扫码核销）在公司工作手机上做；私人手机不允许带入工作现场，进办公区需寄存。
   + - 铁律4批准依据：操作员逐句批准的业务地图V0.14 L930/L1865，PR #432（本线程Claude交接）；治理地图V0.12 L741，2026-09-11“同意按此修改”、PR #433（本线程交接及findings M013-008）。仅同步已批准原文，不修改HTML。
   + 注册、提交资料审批、收公告通知、查自己的工单、帮客人订位、看排班、查看考勤、请假、查薪资等可离场办理的事；必须在现场完成的操作（代客下单、出示公司收款码、完成履约、扫码核销）在公司工作手机上做；私人手机不允许带入工作现场，进办公区需寄存。
--- BSM-120 已确认规则和实施前输入项 | in M038: True | -1 +2
   - 更新日期：2026-09-08。用途：给不懂产品和技术的业务人员理解综合体管理系统范围。解释权声明：本《业务系统地图》与《平台治理地图》《业务枚举速查表》共同组成三份最高业务解释权 HTML。发生冲突时，只以这三份 HTML 中经用户批准的内容为准；PRD、其他文档、后台配置、设计和实现均不得覆盖。某项业务内容若尚未写入三份 HTML，不因出现在 PRD、配置或实现中而取得效力，必须先逐项获得用户
   + - 批准依据：铁律4，操作员已批准PR432业务地图V0.14；docs/prd-audit/business-decision-ledger.md:2603—2618明确记录页脚更新日期09-08→09-10，本次仅同步获批HTML日期副本。
   + 更新日期：2026-09-10。用途：给不懂产品和技术的业务人员理解综合体管理系统范围。解释权声明：本《业务系统地图》与《平台治理地图》《业务枚举速查表》共同组成三份最高业务解释权 HTML。发生冲突时，只以这三份 HTML 中经用户批准的内容为准；PRD、其他文档、后台配置、设计和实现均不得覆盖。某项业务内容若尚未写入三份 HTML，不因出现在 PRD、配置或实现中而取得效力，必须先逐项获得用户
--- PGM-043 外包人员与审计人员（2026-06-29 新增） | in M038: True | -1 +2
   - 导出文件是否脱敏，不由导出人判断，也不提供关闭开关：系统按导出内容自动判定并强制执行。凡导出内容中包含以下任何一类，一律强制脱敏——一是法律法规要求保护的个人信息，只有下面七项，没有第八项：姓名、手机号、证件信息、人脸与生物特征、住址、支付账号、精确位置；其中：员工的姓名、所属部门与所属公司不脱敏，全体员工均可查看；员工的手机号只对该员工直属管理上级这条管理线上的各级岗位、以及顶层平台公司的管理层
   + - 铁律4批准依据：本线程操作员对两处完整改文及单次权限修复答“都批准修改”；本条仅同步获批HTML原文，未新增业务判据。
   + 导出文件是否脱敏，不由导出人判断，也不提供关闭开关：系统按导出内容自动判定并强制执行。凡导出内容中包含以下任何一类，一律强制脱敏——一是法律法规要求保护的个人信息，只有下面七项，没有第八项：姓名、手机号、证件信息、人脸与生物特征、住址、支付账号、精确位置；其中：员工的姓名、所属部门与所属公司不脱敏，全体员工均可查看；员工的手机号只对该员工直属管理上级这条管理线上的各级岗位、以及顶层平台公司的管理层
--- PGM-047 怎么控制 | in M038: False | -1 +2
   - 同一个员工 App，装在公司设备和员工自有手机上能用的功能不一样：能做什么由后台按设备类型统一下发，员工自有手机只开放低风险功能（公告、我的工单、帮客人订位、考勤打卡、请假、查自己薪资等）。
   + - 铁律4批准依据：操作员逐句批准的业务地图V0.14 L930/L1865，PR #432（本线程Claude交接）；治理地图V0.12 L741，2026-09-11“同意按此修改”、PR #433（本线程交接及findings M013-008）。仅同步已批准原文，不修改HTML。
   + 同一个员工 App，装在公司设备和员工自有手机上能用的功能不一样：能做什么由后台按设备类型统一下发，员工自有手机只开放低风险功能（公告、我的工单、帮客人订位、查看考勤、请假、查自己薪资等）。
--- PGM-059 客人与员工的个人信息保护 | in M038: True | -1 +2
   - 员工日常界面只显示脱敏号码，不见明文；界面脱敏不提供关闭开关。
   + - 铁律4批准依据：本线程操作员对两处完整改文及单次权限修复答“都批准修改”；本条仅同步获批HTML原文，未新增业务判据。
   + 员工日常界面只显示脱敏号码，不见明文；界面脱敏不提供关闭开关。商户档案中的第一联系人和第二联系人手机号除外：经单独授权且查看权限有效的人员，以及持授权 U 盘密钥的最高权限本人，可以在其获准访问的商户档案列表和档案操作结果中查看完整号码；其他人员仍只显示脱敏号码。平台管理员身份本身不自动取得该查看权限。该例外仅适用于商户档案联系人，不扩大到客人或员工档案的手机号，也不免除项目范围检查和查看留痕。
M038 mapping identical: True
general block identical: True
== 1. files (lines/bytes/sha256 | vs origin/main)
AccessGovernancePage.tsx 777 57255 d7668403ba673b8375a541f24220397d19f2cb92d345392cf63f4100dc94b101 | [ 1 file changed, 81 insertions(+), 42 deletions(-)]
AccessGovernancePage.degraded.test.tsx 61 3322 b34d2ed33a973f52db7b18c04ffa313213b7548605394bdee86febbd7216b6f5 | []
AccessGovernancePage.guidedcreate.test.tsx 191 11207 521dc934d768003006e329b7728985d795e3006437d905c7bb201af04da48871 | [ 1 file changed, 80 insertions(+), 2 deletions(-)]
AccessGovernancePage.scopechange.test.tsx 82 5060 84f86642cb900d037d68ad6179a8e5822cee51dd123078907d954342d33f37f6 | []
AccessGovernancePage.test.tsx 328 19760 bcfe27b0f09a09c0b19f6a0f92472eac841cd64559d17172196b008e9005b327 | [ 1 file changed, 144 insertions(+), 1 deletion(-)]
AccessUsersPage.tsx 374 23060 b4ae66d2735e0c20b28080ea36c3ff2a472c5dcb9cd540696f0f10215f9f230d | [ 1 file changed, 13 insertions(+), 9 deletions(-)]
AccessUsersPage.metricdetail.test.tsx 41 2121 ff5caa1b72aac3f548dbd585aff4bc406cea98b5790c1ac1c63c72f20ddf04ca | []
AccessUsersPage.partnerbind.test.tsx 113 5703 33d7f9d312be473c48638774bc2f344ab03c1022157c12d57089f3a32401d59d | []
AccessUsersPage.robustness.test.tsx 45 2503 fe77b21adf095eeff94014267cda5189c9387a1efd9faeb48308bb862b87ebd8 | []
AccessUsersPage.test.tsx 136 6905 d5c22dfcc87a87e06a01fd28bc6c428984f716cd4e9ef675e24c4dbb39cf42b9 | [ 1 file changed, 45 insertions(+), 1 deletion(-)]
PermissionTemplatesPage.tsx 552 27684 4b31fa7e991e6c540e10930767bc29f4f31d6ec4f02a661bb42a49cade64a67d | [ 1 file changed, 8 insertions(+), 4 deletions(-)]
PermissionTemplatesPage.guidedcreate.test.tsx 116 6000 e026609a0545a09f6013b258c8cc60a7f1bbd4fb53d340636f0917da4fa9b6d6 | []
PermissionTemplatesPage.test.tsx 153 8294 88e1f50b434b093fd4e494c6529c6528a7d83c6d7a4a452009cf1c783af894ec | [ 1 file changed, 59 insertions(+)]
Can.tsx 84 2545 bfd3fbeb22a5fd1ee620b63c139ae0045fc36caeaa3afd9d2766edb11061d193 | []
PermissionGate.tsx 44 1502 a15438dae20ba874cd4385edbd2a65cef85cda71724dd6cf10ce8e750d28a4a5 | []
PermissionGate.test.ts 113 6276 14f28de8ffc8544fd26f9a5843e156ecb7dd315591375fb51af827185d4ccbfc | [ 1 file changed, 54 insertions(+), 2 deletions(-)]
主工作区 8 个改过文件的 diff 是否仍等于第 1 轮冻结 diff（sha256）：
30bbbdc50244c0384c56bc9bcdecc326e55bf74d6c6d43250344fd438d34fc4b
30bbbdc50244c0384c56bc9bcdecc326e55bf74d6c6d43250344fd438d34fc4b
== 2. 发布口径：origin/main 一次性 worktree＋第 1 轮冻结 diff
scratch at 53dec141
r1 diff applied: 8 files
scratch 16 文件与主工作区逐字节相同（无 MISMATCH 行即是）
VITEST_EXIT(publish)=0
 ✓ src/admin/security/PermissionGate.test.ts (11 tests) 27ms
 ✓ src/admin/pages/AccessUsersPage.metricdetail.test.tsx (2 tests) 38ms
 ✓ src/admin/pages/AccessUsersPage.robustness.test.tsx (1 test) 37ms
 ✓ src/admin/pages/AccessUsersPage.partnerbind.test.tsx (2 tests) 70ms
 ✓ src/admin/pages/PermissionTemplatesPage.test.tsx (6 tests) 121ms
 ✓ src/admin/pages/AccessGovernancePage.degraded.test.tsx (1 test) 246ms
 ✓ src/admin/pages/PermissionTemplatesPage.guidedcreate.test.tsx (3 tests) 340ms
 ✓ src/admin/pages/AccessGovernancePage.scopechange.test.tsx (2 tests) 347ms
 ✓ src/admin/pages/AccessUsersPage.test.tsx (5 tests) 485ms
 ✓ src/admin/pages/AccessGovernancePage.guidedcreate.test.tsx (5 tests) 731ms
 ✓ src/admin/pages/AccessGovernancePage.test.tsx (19 tests) 986ms
 Test Files  11 passed (11)
      Tests  57 passed (57)
TSC_EXIT=0
scratch dirty after cleanup: 0
scratch removed
== 3. 页面会显示的取值在公共字典里有没有（改后页内映射另有兜底）
user_role                        用户角色
user_position                    用户职位
temporary_permission             临时权限
role_permission                  （字典无此键）
user_scope_change                （字典无此键）
position_high_risk_release       （字典无此键）
pending                          待处理
applied                          （字典无此键）
rejected                         已驳回
staff                            普通员工
merchant                         商户员工
merchant_admin                   商户管理员
supervisor                       主管
sales                            销售人员
outsourced_vendor                外包账号
platform_admin                   平台管理员
manual_admin                     （字典无此键）
legacy                           （字典无此键）
invited                          待激活
disabled                         已禁用
active                           有效
inactive                         未激活
== 4. 三个页面的写请求调用点与忙态来源（011 现状）
  AccessGovernancePage.tsx
     savePositionTemplate       写请求 2 处 | 自带 setTemplateSaving(true)
     saveGrantPolicy            写请求 1 处 | 经 runWrite
     changePositionLifecycle    写请求 1 处 | 经 runWrite
     assignPosition             写请求 1 处 | 经 runWrite
     changeUserScope            写请求 1 处 | 经 runWrite
     roleAssignment             写请求 1 处 | 经 runWrite
     userPermissionOverride     写请求 1 处 | 经 runWrite
     applyGrantRequest          写请求 1 处 | 自带 setGrantSaving(true)
     reviewGrant                写请求 1 处 | 经 runWrite
  AccessUsersPage.tsx
     createUser                 写请求 3 处 | 自带 setSubmitting(true)
     bindPartnerOrg             写请求 1 处 | 自带 setBindBusy(true)
     unbindPartnerOrg           写请求 1 处 | 自带 setBindBusy(true)
  PermissionTemplatesPage.tsx
     createTemplate             写请求 1 处 | 自带 setCreateSaving(true)
     saveTemplate               写请求 1 处 | 自带 setSubmitting(true)
     copyTemplate               写请求 1 处 | 缺口
     applyTemplate              写请求 1 处 | 缺口
  无忙态的写函数： 2
== 5. 30 档编号：页面规则生成 vs 库内 staff_levels
30 个编号逐键且同序相同
== 6. 变更判据涉及的界面文字核对：三页里「考勤打卡」「明文」「脱敏」「导出」出现处
（无输出＝三页不含这些字样）
== 7. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 5c4594a73e767c704238ad311899903dbaaf94503e4a4cc18f21a5596e67429f b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458 
END=2026-09-23T16:59:21Z
```

## 3. 三线审计

- 第一性原理线。用途：让平台管理员在后台完成岗位模板映射、定岗、范围变更、角色装配、员工级授予/拒绝、授权申请与审批、授权政策、账号开通、权限模板维护。实际行为：现状 57 个测试全绿；旧锁 001…010 的行为（取值与字段名全中文、未登记岗位、恢复文案、治理页写动作失败提示与防双击、模板时间保值、表头、职级 30 档下拉、状态用词、权限闸组件测试）均在。未经验证的前提：「三个页面的每个写按钮提交中都禁用」——对权限模板页的复制与应用不成立（CX-M038-006＝M038-011）。
- 顺从式线：106 判据逐项见 §4，其中现锁变更的 BSM-114、BSM-120、PGM-043、PGM-059 按 V0.14 正文重审：四处变更（查看考勤、页脚日期、导出脱敏整段、商户档案联系人手机号例外）在三个后台页上都无对应对象，原结论不变；16 个文件全读。
- 对抗式线（grilling）：Q 现锁新增的「导出脱敏」段会不会管到本模块？A 三个页面没有导出功能、没有导出按钮或接口调用（探针 6 无「导出」字样），不涉。Q 商户档案联系人手机号例外会不会让账号页该显示明文？A 例外原句限定「商户档案列表和档案操作结果」且「不扩大到客人或员工档案」，账号页不是商户档案，且账号表/抽屉本就不显示手机号。Q 「查看考勤」改词影响什么？A 只影响员工手机端文案，本模块无考勤内容。Q 旧锁 001…010 修复在现锁下还成立吗？A 相关判据（PGM-041、GEN-015、BSM-004、GEN-006、GEN-009、BEQR-149 等）正文未变，修复对应的测试 57 全绿。Q 还有没有无忙态的写按钮？A 探针 4 逐页清单：只剩复制模板、应用模板两处（即 011）。

## 4. 106 判据逐项（现锁 V0.14）

| 判据 | 适用｜证据｜结论 |
|---|---|
| BEQR-063 高危动作（需审批/双证据） | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「高危动作（需审批/双证据）」的对象或文字 |
| BEQR-074 隐私桌台权限 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「隐私桌台权限」的对象或文字 |
| BEQR-143 四类主体 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「四类主体」的对象或文字 |
| BEQR-144 经营主体关系 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「经营主体关系」的对象或文字 |
| BEQR-145 城市项目公司 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「城市项目公司」的对象或文字 |
| BEQR-146 运营公司职责 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「运营公司职责」的对象或文字 |
| BEQR-147 主要收入归属 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「主要收入归属」的对象或文字 |
| BEQR-148 合作模式 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「合作模式」的对象或文字 |
| BEQR-149 职级 | 适用｜职级显示「M<级>·上/中/下」；四处职级输入为 30 档下拉，选项编号与库内 30 个编号逐键一致（008，有测试）｜通过 |
| BEQR-150 终端十一端 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「终端十一端」的对象或文字 |
| BEQR-151 最终权限计算 | 适用｜最终权限公式的五个来源在治理页与模板页各有写入口：直接装配角色、岗位模板映射与定岗、权限模板（基础/临时/单次）、员工单独授予、显式拒绝｜通过 |
| BEQR-152 项目与敏感数据权限 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「项目与敏感数据权限」的对象或文字 |
| BEQR-153 终端开关优先级 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「终端开关优先级」的对象或文字 |
| BEQR-154 综合体项目状态 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「综合体项目状态」的对象或文字 |
| BEQR-155 商户/运营主体状态 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「商户/运营主体状态」的对象或文字 |
| BEQR-156 合同状态 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「合同状态」的对象或文字 |
| BEQR-157 员工状态 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「员工状态」的对象或文字 |
| BEQR-158 设备状态 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「设备状态」的对象或文字 |
| BEQR-159 权限状态 | 适用｜速查表「权限状态：草稿、已启用、暂停、已停用、归档」。权限模板页只读显示状态，没有改状态的入口；后端也没有改权限模板状态的接口、库内现值只有 active。页面无从提供，属后端模块的事，登记 OBS-5｜通过（登记 OBS-5） |
| BEQR-166 合同审批链 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「合同审批链」的对象或文字 |
| BEQR-182 考勤日结果 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「考勤日结果」的对象或文字 |
| BEQR-183 打卡方式 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「打卡方式」的对象或文字 |
| BEQR-184 请假类型 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「请假类型」的对象或文字 |
| BEQR-185 请假单状态 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「请假单状态」的对象或文字 |
| BEQR-186 薪资单构成 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「薪资单构成」的对象或文字 |
| BEQR-187 员工提成发放 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「员工提成发放」的对象或文字 |
| BEQR-189 管理费方式 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「管理费方式」的对象或文字 |
| BSM-001 整体说明：这套系统到底管什么 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「整体说明：这套系统到底管什么」的对象或文字 |
| BSM-003 系统通用能力：数据可改可删、操作有引导、权限分岗 | 适用｜「权限分岗」：每个写按钮经 `PermissionButton` 按权限码禁用并拦截点击（现有组件渲染测试，010），权限码与 AccessController 门控码逐个一致；「操作有引导」：页内写动作失败时把后端中文消息报到页面（005，有测试）｜通过 |
| BSM-004 配置数据可以存档、也可以删除（删除要审批） | 适用｜存档、申请删除、批准/驳回删除、恢复入口齐全、理由必填；恢复按钮已存档写「取消存档」、已删除写「恢复」，与业务地图原句及 M031-001 后的行为一致（004，有测试）｜通过 |
| BSM-005 后台的"新建/录入"改成分步引导 | 适用｜新增岗位角色模板、发起授权申请、新建员工账号、新建权限模板四处为「＋按钮→分步引导弹窗→确认」，当步校验、失败留在弹窗内显错，有测试｜通过 |
| BSM-006 验证用的二维码一律动态刷新（间隔可在后台设定） | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「验证用的二维码一律动态刷新（间隔可在后台设定）」的对象或文字 |
| BSM-007 平台管理员拥有全部日常功能权限 | 适用｜前端不自行给平台管理员放行：`hasPermissions` 只认后端给的 grants/denies/wildcard，显式拒绝压过 wildcard（有测试）；后端现恒给 wildcard=false，该分支不会被走到｜通过 |
| BSM-009 基础管理 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「基础管理」的对象或文字 |
| BSM-028 主要功能 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「主要功能」的对象或文字 |
| BSM-029 和谁有关 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「和谁有关」的对象或文字 |
| BSM-072 商户管理：商户在授权范围内完整使用自己的系统 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「商户管理：商户在授权范围内完整使用自己的系统」的对象或文字 |
| BSM-073 主要功能 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「主要功能」的对象或文字 |
| BSM-074 和谁有关 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「和谁有关」的对象或文字 |
| BSM-084 员工手机端：排班、考勤、请假、薪资，自己手机上都能办 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「员工手机端：排班、考勤、请假、薪资，自己手机上都能办」的对象或文字 |
| BSM-085 日常工作 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「日常工作」的对象或文字 |
| BSM-086 排班、考勤与请假 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「排班、考勤与请假」的对象或文字 |
| BSM-087 我的薪资 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「我的薪资」的对象或文字 |
| BSM-088 员工提成、活动执行与人员服务边界 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「员工提成、活动执行与人员服务边界」的对象或文字 |
| BSM-114 各类入口：不同人只看自己该看的、只做自己该做的 | 适用｜现锁 V0.14 该条正文的变更只是员工手机端可离场办理事项里「考勤打卡」改为「查看考勤」，三个后台页不含考勤功能（探针 6：三页无「考勤」字样），该句无本面；「不同人只看自己该看的」部分同前：无权限的副资源不请求、各自降级（总结、权限模板、用户/角色清单），主表照常，有测试｜通过 |
| BSM-115 相互关系：为什么这些子系统必须连在一起 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「相互关系：为什么这些子系统必须连在一起」的对象或文字 |
| BSM-120 已确认规则和实施前输入项 | 无本面｜现锁 V0.14 该条变更只是业务地图页脚更新日期 09-08→09-10；本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「已确认规则和实施前输入项」的对象或文字 |
| GEN-001 必要性与最小性 | 适用｜三个页面无重复入口；`hasPermissions` 的 wildcard 分支现不会被走到但属与后端契约字段，保留｜通过 |
| GEN-002 可执行正确性 | 适用｜发布口径（origin/main 53dec141 一次性 worktree＋旧锁 B3-r1 冻结 diff）：本模块 11 个测试文件 57 个测试全绿、web 类型检查通过（探针 2）｜通过 |
| GEN-003 测试有效性 | 适用｜测试用真实 AdminApi＋打桩的 fetch 渲染并操作页面；本轮十个 ID 的行为均有能失败的断言（RED 14 条），权限闸组件有渲染测试（010）｜通过 |
| GEN-004 信任边界校验 | 适用｜必填与当步校验在页面侧先拦，后端再校验｜通过 |
| GEN-005 授权与最小权限 | 适用｜见 BSM-003：前端权限码与后端门控码一致；无权限按钮禁用且拦截点击｜通过 |
| GEN-006 数据完整性 | 适用｜权限模板编辑「读出→不改→保存」保持同一时刻（006，在 Asia/Shanghai 下有测试）｜通过 |
| GEN-007 并发与幂等 | 适用｜写请求由客户端统一带幂等键；治理页 14 处写动作经 runWrite 置忙、账号页与两个引导弹窗各有置忙；但权限模板页「复制模板」「应用模板」仍无提交中态、连点发两笔（探针 4：无忙态的写函数 2＝copyTemplate、applyTemplate；旧锁定稿的 M038-011 尚未修）｜不通过（CX-M038-006＝M038-011） |
| GEN-008 接口与模式一致性 | 适用｜请求路径、方法、字段名（客户端统一转 snake_case）与 AccessController 映射及请求记录类逐个核对一致；模板应用 mode 用 single_use/append，与后端实际取值一致｜通过 |
| GEN-009 错误处理与恢复 | 适用｜读失败、副资源失败有降级；页内写动作统一经 runWrite，失败报后端中文消息（005）｜通过 |
| GEN-010 安全与敏感信息 | 适用｜初始密码输入为 password 类型；不显示手机号｜通过 |
| GEN-011 可观测与审计证据 | 适用｜每个写请求带 operatorNote；审批不填意见时留痕为中文「通过／驳回」（001）｜通过 |
| GEN-012 资源与性能边界 | 适用｜授权申请分页读取（每页 50），其余清单一次读取｜通过 |
| GEN-013 构建配置与依赖 | 适用｜依赖 React、antd Tabs 与本仓公共组件；无新增依赖｜通过 |
| GEN-014 数据库迁移正确性 | 无本面｜本模块不涉迁移 |
| GEN-015 界面可用性与可访问性 | 适用｜行可键盘聚焦与回车展开、表单控件有 aria-label 且不含 id/ID（002）；智能总结表表头与数据列数一致（007）；状态用词下拉与表格同一套（009）；取值全中文（001、003）｜通过 |
| GEN-016 文档可验证性 | 适用｜页面说明文字与实际功能逐句对照一致（恢复按钮文案已改，004）；员工账号页标题说明提到「企业微信同步结果」「禁用」而页内无此内容（登记）｜通过（登记） |
| GEN-017 业务来源分类 | 适用｜未见页面自造业务规则；岗位码中文名是页内 6 项小映射（OBS-1）｜通过（登记 OBS-1） |
| GEN-018 第一性原理证据 | 适用｜结论落到页面源码行、HTML 原文、后端门控与库内闭集的只读查询、vitest 与 node 实跑输出｜通过 |
| PGM-001 整体定位：平台治理层不是一个业务部门，而是所有业务的底座 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「整体定位：平台治理层不是一个业务部门，而是所有业务的底座」的对象或文字 |
| PGM-002 治理顺序：一件业务能不能做，要先经过这些判断 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「治理顺序：一件业务能不能做，要先经过这些判断」的对象或文字 |
| PGM-003 模块入口：平台治理层包含哪些管理模块 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「模块入口：平台治理层包含哪些管理模块」的对象或文字 |
| PGM-004 综合体和集团 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「综合体和集团」的对象或文字 |
| PGM-005 空间和点位 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「空间和点位」的对象或文字 |
| PGM-006 商户和主体 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「商户和主体」的对象或文字 |
| PGM-007 合同和准入 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「合同和准入」的对象或文字 |
| PGM-008 员工和手机 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「员工和手机」的对象或文字 |
| PGM-009 权限和职级 | 适用｜权限和职级模块的三个后台页：访问治理工作台、员工账号、权限模板｜通过 |
| PGM-010 终端开关 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「终端开关」的对象或文字 |
| PGM-011 设备管理 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「设备管理」的对象或文字 |
| PGM-012 商户侧 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「商户侧」的对象或文字 |
| PGM-013 开放接口 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「开放接口」的对象或文字 |
| PGM-014 留痕复核 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「留痕复核」的对象或文字 |
| PGM-015 楼宇运营 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「楼宇运营」的对象或文字 |
| PGM-016 应急与隐私 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「应急与隐私」的对象或文字 |
| PGM-017 综合体和集团：当前一个项目，未来能看集团 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「综合体和集团：当前一个项目，未来能看集团」的对象或文字 |
| PGM-018 物理项目与运营公司：多对多协作，不是固定隶属层级 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「物理项目与运营公司：多对多协作，不是固定隶属层级」的对象或文字 |
| PGM-019 各主体具体负责什么 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「各主体具体负责什么」的对象或文字 |
| PGM-020 主要功能 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「主要功能」的对象或文字 |
| PGM-021 业务意义 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「业务意义」的对象或文字 |
| PGM-022 空间和点位：所有业务都要知道“发生在哪” | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「空间和点位：所有业务都要知道“发生在哪”」的对象或文字 |
| PGM-024 影响业务 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「影响业务」的对象或文字 |
| PGM-026 主要功能 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「主要功能」的对象或文字 |
| PGM-027 业务边界 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「业务边界」的对象或文字 |
| PGM-031 外部经销商主体、合同价格与资金治理 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「外部经销商主体、合同价格与资金治理」的对象或文字 |
| PGM-032 员工和手机：人、私人手机、工作手机要能对应起来 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「员工和手机：人、私人手机、工作手机要能对应起来」的对象或文字 |
| PGM-033 主要功能 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「主要功能」的对象或文字 |
| PGM-034 工作手机和企业微信 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「工作手机和企业微信」的对象或文字 |
| PGM-035 提成发放和人员服务治理 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「提成发放和人员服务治理」的对象或文字 |
| PGM-036 设备管理：谁领了什么设备，设备能不能用 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「设备管理：谁领了什么设备，设备能不能用」的对象或文字 |
| PGM-040 权限和职级：每一个单一功能都可以单独授权 | 适用｜权限点逐个勾选（按菜单模块分组），角色级模板与员工级单独授予/显式拒绝均有入口｜通过 |
| PGM-041 权限规则 | 适用｜临时授权有效期/单次、高危授权审批、临时权限总结均有入口；授权申请状态与类型按后端闭集全中文（001）；权限模板编辑原样保存保持同一时刻（006）｜通过 |
| PGM-042 职级规则 | 适用｜职级按 M1–M10 上中下显示；定岗表单可填基础薪资（「具体薪资单独填写」）｜通过 |
| PGM-043 外包人员与审计人员（2026-06-29 新增） | 适用｜现锁 V0.14 该条变更是新增「导出文件是否脱敏由系统按内容自动判定并强制执行」整段：三个后台页没有任何导出功能，也不展示姓名之外的个人信息字段（探针 6：三页无「导出」「脱敏」「明文」字样），该段无本面；外包人员部分同前：员工走注册申请、后台手动建号只留外部协作机构账号，并可绑定/解绑外部协作机构，与后端 STAFF_REGISTRATION_REQUIRED 一致｜通过 |
| PGM-044 桌台、挂单与经销业务权限 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「桌台、挂单与经销业务权限」的对象或文字 |
| PGM-048 商户侧：在授权业务范围内全开放 | 适用｜商户管理员创建账号时后端按其商户收口；页面对平台账号提供「不绑定 / 平台账号」｜通过 |
| PGM-059 客人与员工的个人信息保护 | 适用｜现锁 V0.14 该条变更是「商户档案第一/第二联系人手机号经单独授权可看完整号码」的例外——只适用于商户档案联系人、明文不扩大到员工档案；本模块账号表与详情抽屉不显示任何手机号（有测试断言 not.toContain 手机号），不涉商户档案，例外无本面；「员工日常界面只显示脱敏号码」部分同前｜通过 |
| PGM-060 会员身份、营销与生物信息治理 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「会员身份、营销与生物信息治理」的对象或文字 |
| PGM-065 连接关系：平台治理层怎么连接所有业务 | 无本面｜本模块是权限与账号的三个后台页、两个权限闸组件及其测试，不承载、不展示「连接关系：平台治理层怎么连接所有业务」的对象或文字 |
| PGM-066 已拍板规则和待排期问题 | 适用｜范围变更处有「模板内既有高危不免审」的中文说明；直接装配高危角色被后端拒绝时页面报出原因（005）；界面文字全中文（001、002、003）｜通过 |

## 5. 本次 Findings

### CX-M038-006（技术性；工程缺陷成立）＝旧锁定稿的 M038-011：权限模板页「复制模板」「应用模板」无提交中态，连点发两笔

- 位置：`pages/PermissionTemplatesPage.tsx` `copyTemplate`（无置忙）、`applyTemplate`（无置忙）；「复制模板」按钮 `disabled={!selected?.id}` `onClick={() => void copyTemplate()}`；「应用模板」经 `HighRiskConfirm` `disabled={!selected?.id}` `onConfirm={() => void applyTemplate()}`；同页「保存修改」有 `submitting`。
- 证据：探针 4「无忙态的写函数： 2」；客户端每次请求新生成幂等键，连点两次「复制模板」后端建两份副本，两次确认「应用模板」发两笔赋权。旧锁 B5-r1 由 Grok 以 GR-M038-012 发现、B2-r2 双 PASS 定稿为 M038-011（技术性），处置：模板页加与 runWrite 同构的互斥包装、复制与应用走它、两按钮 disabled 并入 submitting；测试补两条连点用例。现锁下事实与处置不变。
- 依据：GEN-007 / GEN-002（正文未变）。
- 最小拟处置：同旧锁定稿 M038-011，不改。

## 6. 观察与登记（不立项）

- 旧锁 M038-001…010：已在 B3-r1 落地（主工作区 8 个文件未提交），现锁下相关判据正文未变、测试全绿，作为既有修复保留，不重开。
- XM-M038-A（→各自模块）：origin/main 全量前端单测另有别的模块的失败（旧锁 B3-r1 时为 TraceLinkDrilldown 1 条、TraceLinkP2c 2 条；本轮未重跑全量）。
- 其余登记同旧锁 B2-r1/B2-r2 双席只登记的观察：岗位编码、所属组织、目标员工、权限模板等处手填内部编号；`hasPermissions` 的 wildcard 分支在后端恒给 false 后不会被走到；员工账号页标题说明提到「企业微信同步结果」「禁用」而页内无此内容；速查表「权限状态」五态后端无改状态接口；PermissionTemplatesPage.tsx 等文件没有「文件版本」行；公共字典缺 applied、user_scope_change 等键（本模块以页内映射挡住上屏；字典属别的模块）；审批按钮所用公共 RowActions 无禁用槽（runWrite 的 ref 仍挡第二笔）；M080 另有一份发起授权申请的引导测试。

## 7. 结论

席甲现锁 B1 票：**FAIL**（CX-M038-006＝M038-011；其余登记）。16 个文件全读、106 判据按 V0.14 逐项、三线完成；未改任何已跟踪文件；落盘前未读 Grok 本次追加段。


## M038 现锁 B2-r3 整批候选复核（席甲票，V0.05）

- 时间：2026-09-23T17:21:31Z。输入：两席现锁 B1 追加段（席甲 V0.04＝上方本段、Grok V0.04）与候选 findings.md 顶部「当前 M038 现锁 V0.14 B2-r3 整批候选」段（全文件 SHA256 740b274c35239f9481d186fec123c824c20dfba8f3972063058e498d385ad3f0）。本票落盘前未读 Grok B2-r3 追加段。不写独占锁行。
- 差集：CX-M038-006→M038-011（既有，沿用旧锁定稿的分类与最小处置）；Grok GR-03→M038-012（席甲现锁 B1 漏：只从「写按钮忙态」「取值中文」的角度扫账号页，没有对「两步写入、后一步失败」的路径问「第一步的结果用户看得到吗」）；Grok GR-02 席甲实测不可复现（同步连点两次只发 1 笔、第二次已禁用，m038_gr02_probe.log）→ 只登记，并在候选段写明证据；两席其余观察登记；无漏并、无改写。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r3 票：**PASS**。
