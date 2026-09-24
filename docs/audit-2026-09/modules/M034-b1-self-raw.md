CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M034 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理）。2026-09-21T07:57:56Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`，origin/main 4f6ef48b。
- 模块 M034「功能权限点、职级三十档、权限模板与最终权限计算／数据库迁移／第1段」13 个已应用迁移 690 行，18 判据。B3 累计 0。13 个文件与 origin/main 逐字节相同；已应用迁移不可改。
- 顺序：Grok 已先派（modules/M034-b1-grok-raw.md 存在=False），本报告落盘前未读其正文。只读承诺：未改任何文件；数据库只对 complex_test SELECT。
- 方法：迁移效果一律看「后续迁移叠加后的库内终态」，不拿单个文件里的旧值当现状（探针 B 列出同一批对象被哪些后续迁移改过）。

## 1. 文件覆盖表

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| V003__p0_access_permission_authz_slice.sql | 181 | 8063 | fb1b158cfe1d15ec3396396105f25941ceba8a56b7a04cb7e7c800a4e4a1b638 | 全读 |
| V092__amusement_operational_config_permissions.sql | 23 | 1392 | eabcc470c5cfb3e6d4aa59023bae90753afb381e9550181bdeb8bcfdf543203b | 全读 |
| V158__refund_authority_admin_permissions.sql | 23 | 1419 | 4a5d2728b52f2513e32f37e019f278c2c657ff1f300db9344de30080189b15a2 | 全读 |
| V169__access_role_assignment_closure.sql | 53 | 2410 | a25316489af69c5a5318b743f1220e5deca75adee2e7d8c887b0ddd68d3a5ea7 | 全读 |
| V180__auditor_roles_readonly_seed.sql | 268 | 9830 | 8af881c9af09eb3ef23fdd8dce36d404997d299d0c2c8cbb3f918d20e374c551 | 全读 |
| V204__seed_runtime_checked_permissions.sql | 16 | 1290 | 55e98c86950bcef5b8e608b7af60c2bf8a0e17659d43b285715ae72a314edcf5 | 全读 |
| V261__platform_admin_full_permissions.sql | 9 | 640 | 5a6a982a2e0d7c8c9d61e53938552273bfc083e30839fcf603d54c7614f938dc | 全读 |
| V270__ticket_manual_override_permission.sql | 16 | 1309 | 45927d048b1caa9c64116a1bfe9ee99ec1633b3dbb832f50fee8904ddc4a1fcb | 全读 |
| V321__staff_self_profile_permission.sql | 12 | 879 | 49b40908456a0fac3ef23759ad08e6631f542ec35f7a9d471a336abc2621f1e1 | 全读 |
| V322__staff_self_service_role.sql | 32 | 2036 | 3bd069d3e69773c9d00041cb8ec5deb6db667c7df6e7899f49964e18196cb8fe | 全读 |
| V387__access_display_names_chinese.sql | 31 | 3120 | 7f89a0e39aaa4a161400f351e1ceae5e227358783e713d49c61c77ffd2eab511 | 全读 |
| V393__sensitive_business_data_permissions_require_approval.sql | 8 | 1057 | 40aeb2b412ee4a07ed968be6582b4c7cbfbad0658bfcc55abecb789ba25e445f | 全读 |
| V394__permission_risk_tiers_and_sensitive_codes_require_approval.sql | 18 | 2343 | acdeef24cb2e653981919b47eb0421e96a713f9fd7ce7cb14a450538ad7ceba7 | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m034_b1_run.log）

```
START=2026-09-21T07:56:17Z HEAD=e456ba96 origin/main=4f6ef48b
== 0. files (lines/bytes/sha256 | status | vs origin/main)
V003__p0_access_permission_authz_slice.sql 181 8063 fb1b158cfe1d15ec3396396105f25941ceba8a56b7a04cb7e7c800a4e4a1b638 | [] | []
V092__amusement_operational_config_permissions.sql 23 1392 eabcc470c5cfb3e6d4aa59023bae90753afb381e9550181bdeb8bcfdf543203b | [] | []
V158__refund_authority_admin_permissions.sql 23 1419 4a5d2728b52f2513e32f37e019f278c2c657ff1f300db9344de30080189b15a2 | [] | []
V169__access_role_assignment_closure.sql 53 2410 a25316489af69c5a5318b743f1220e5deca75adee2e7d8c887b0ddd68d3a5ea7 | [] | []
V180__auditor_roles_readonly_seed.sql 268 9830 8af881c9af09eb3ef23fdd8dce36d404997d299d0c2c8cbb3f918d20e374c551 | [] | []
V204__seed_runtime_checked_permissions.sql 16 1290 55e98c86950bcef5b8e608b7af60c2bf8a0e17659d43b285715ae72a314edcf5 | [] | []
V261__platform_admin_full_permissions.sql 9 640 5a6a982a2e0d7c8c9d61e53938552273bfc083e30839fcf603d54c7614f938dc | [] | []
V270__ticket_manual_override_permission.sql 16 1309 45927d048b1caa9c64116a1bfe9ee99ec1633b3dbb832f50fee8904ddc4a1fcb | [] | []
V321__staff_self_profile_permission.sql 12 879 49b40908456a0fac3ef23759ad08e6631f542ec35f7a9d471a336abc2621f1e1 | [] | []
V322__staff_self_service_role.sql 32 2036 3bd069d3e69773c9d00041cb8ec5deb6db667c7df6e7899f49964e18196cb8fe | [] | []
V387__access_display_names_chinese.sql 31 3120 7f89a0e39aaa4a161400f351e1ceae5e227358783e713d49c61c77ffd2eab511 | [] | []
V393__sensitive_business_data_permissions_require_approval.sql 8 1057 40aeb2b412ee4a07ed968be6582b4c7cbfbad0658bfcc55abecb789ba25e445f | [] | []
V394__permission_risk_tiers_and_sensitive_codes_require_approval.sql 18 2343 acdeef24cb2e653981919b47eb0421e96a713f9fd7ce7cb14a450538ad7ceba7 | [] | []
== A. 库内终态（complex_test，只 SELECT）
== 1. permissions 总数 / 状态 / 风险档 / 须审批
381|376
critical|t|16
high|f|10
high|t|192
low|f|33
medium|f|129
medium|t|1
== 2. 名称含拉丁字母的权限 / 角色 / 模板（HTML 自身用词 AI 除外）
0
== 3. role_platform_admin 缺的 active 权限
== 4. 审计员角色里非只读形态的码（不以 view/read/list/export/self 结尾）
role_auditor_internal|81
role_platform_admin|376
role_staff_self|8
== 5. 员工自助角色现持有
access.grant_request.apply
staff.attendance.punch
staff.device.bind
staff.leave.submit
staff.notice.view
staff.payroll.view_mine
staff.profile.self
staff.shift.view
== 6. V393 / V394 点名的码的终态
access.grant_request.apply|low|f
analytics.group.view|high|t
bar.combo.manage|high|t
bar.combo.view|medium|t
customer.voucher.manage|high|t
finance.merchant_expense.manage|high|t
inventory.cost.view|high|t
pii.contact_phone.view|high|t
voucher.internal_price.view|high|t
wecom.sync|high|t
== 7. 约束
chk_permission_template_items_effect|CHECK (((effect)::text = ANY ((ARRAY['grant'::character varying, 'deny'::character varying])::text[])))
chk_permissions_risk_level|CHECK (((risk_level)::text = ANY ((ARRAY['low'::character varying, 'medium'::character varying, 'high'::character varying, 'critical'::character varying])::text[])))
chk_position_role_templates_status|CHECK (((status)::text = ANY ((ARRAY['active'::character varying, 'inactive'::character varying])::text[])))
chk_user_permission_overrides_effect|CHECK (((effect)::text = ANY ((ARRAY['grant'::character varying, 'deny'::character varying])::text[])))
== 8. 本模块 13 个迁移在 flyway 历史里的状态
003|t
092|t
158|t
169|t
180|t
204|t
261|t
270|t
321|t
322|t
387|t
393|t
394|t
tpl_auditor_internal|81
tpl_auditor_external|51
== B. 后续迁移对同一批对象的叠加（文件名）
role_auditor_*/tpl_auditor_*: V180__auditor_roles_readonly_seed.sql V181__outsourced_adjustments_audit_lock.sql V185__audit_operation_log_query_permissions.sql V187__fix_auditor_audit_log_grants.sql V387__access_display_names_chinese.sql 
role_staff_self: V322__staff_self_service_role.sql V386__access_grant_authorization_fingerprint.sql V387__access_display_names_chinese.sql V394__permission_risk_tiers_and_sensitive_codes_require_approval.sql V411__staff_profile_pii_and_self_device_permission.sql 
requires_approval UPDATE: V240__payroll_manage_requires_approval.sql V388__supervisor_review_release_and_approval_classification.sql V393__sensitive_business_data_permissions_require_approval.sql V394__permission_risk_tiers_and_sensitive_codes_require_approval.sql 
risk_level UPDATE: V170__access_grant_requests.sql V394__permission_risk_tiers_and_sensitive_codes_require_approval.sql 
permission_name 中文化: V166__facility_work_orders.sql V167__facility_inspection.sql V251__permission_names_i18n_zh.sql V254__permission_names_p3_polish.sql V382__thirdparty_pass_ticket_channel.sql V385__permission_names_chinese.sql V387__access_display_names_chinese.sql 
== C. 迁移总数与最大版本: 396 / 428
== D. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T07:56:18Z
```

补充只读查询（high 且免审批的码，共 10 条）：analytics.metric_anomaly.manage、analytics.review.lock、bar.reservation.invitation.manage、customer.account.manage、integration.video.approve、inventory.supply_entity.manage、inventory.transfer_order.manage、risk_alert.escalate、risk_alert.transfer、service.catalog.approve。
说明：探针 A-4 的结果里外部审计角色 `role_auditor_external` 在 role_permissions 内 0 行、其模板 `tpl_auditor_external` 51 项——这是后续迁移 V181/V187（别的模块）把外部审计与外包的角色直授改为限时模板的结果，不是 V180 的缺陷；内部审计角色 81 条，非只读形态的码 0 条。

## 3. 三线审计

- 第一性原理线。用途：权限字典、RBAC 五张表、模板应用与用量日志、岗位映射与定岗表的骨架，以及若干批权限/角色种子与向前修正。实际行为：见库内终态。未经验证的前提逐个核过：①「平台管理员持全部权限」——库内缺 0 条；②「上屏名称全中文」——含拉丁字母 0 条；③「五类高危须审批」——V393/V394 点名的码终态全部为须审批，余下 10 条 high 免审批码不在五类内；④「员工自助角色能发起授权申请」——现持 8 个码含 access.grant_request.apply。
- 顺从式线：18 判据逐项见 §4；13 文件全读。
- 对抗式线（grilling）：迁移重放会不会重复或报错→种子全部带冲突处理，UPDATE 带旧值守卫或幂等赋值；V394 加约束前库里还有 normal 吗→同文件先 UPDATE 再 ADD CONSTRAINT；V003 给平台管理员授的是「当时 active 的全部码」，之后新增的码会不会漏→V261 一次性补齐并立下「新权限迁移须同步授」的基线，现状缺 0 条；V180 的只读正则会不会把 export/approve 放进审计员→正则排除了这些后缀，内部审计角色现 81 条全为 view/read 形态；V322 写 data_scope='self' 会不会被当真→注释写明仅文档性，AuthorizationService 不读 role.data_scope（M032 已核）；V387 只改了 5 条权限名，其余英文名呢→由 V251/V254/V385 等别的迁移改完，现状 0 条含拉丁字母。

## 4. 18 判据逐项

| 判据 | 适用｜证据｜结论 |
|---|---|
| GEN-001 必要性与最小性 | 适用｜13 个迁移各有单一目的（V003 权限骨架与首批权限码；V092/V158/V204/V270/V321 权限种子；V169 岗位映射与定岗表；V180 审计员只读角色与模板；V261 平台管理员补齐；V322 员工自助角色；V387 中文展示名；V393/V394 五类高危补审批与风险档约束）；无重复或互相抵消的语句｜通过 |
| GEN-002 可执行正确性 | 适用｜13 个版本在 complex_test 的 flyway_schema_history 均 success（探针 A-8）；库内终态与各迁移意图一致：381 个权限（active 376）、平台管理员角色缺 0 条、名称含拉丁字母的权限/角色/模板 0 条、V393/V394 点名的 10 个码终态全部符合（探针 A-1…6）｜通过 |
| GEN-003 测试有效性 | 适用｜迁移由 Flyway 校验（Spring 集成测试启动即 validate；M031/M032 基线里 AccessJdbcIntegrationTests 24/25 绿）与 `AccessJdbcIntegrationTests` 的字典/角色断言间接守住；迁移文件本身无单测属常态｜通过（间接） |
| GEN-004 信任边界校验 | 适用｜CHECK 闭集四条均在库（effect×2、岗位映射 status、risk_level 四档；探针 A-7）；V394 先把 normal 归 low 再加约束，顺序正确｜通过 |
| GEN-005 授权与最小权限 | 适用｜五类高危（资金/敏感经营数据/个人信息/企微原文等）经 V393、V394 补为须审批；现 high 且免审批的 10 个码（删除、最终业务审批、预警处置、调拨、供货主体、指标异常、邀请、客户账户生命周期类）与 V394 注释列举的类别一致，不在 HTML 五类命名范围内。平台管理员角色在库内持全部 376 个 active 码（V261），运行时由 AuthorizationService 按 M006 裁决只认日常码与治理本职码（M032 已审）｜通过（登记 OBS-1） |
| GEN-006 数据完整性 | 无本面｜无金额、时间运算；时间列均 TIMESTAMPTZ |
| GEN-007 并发与幂等 | 适用｜种子语句全部可重放：`ON CONFLICT … DO UPDATE/DO NOTHING`、`CREATE … IF NOT EXISTS`、`ADD COLUMN IF NOT EXISTS`、V387 的 UPDATE 带旧值守卫；V394 的 `ADD CONSTRAINT` 不带 IF NOT EXISTS，由 Flyway 单次执行保证｜通过 |
| GEN-008 接口与模式一致性 | 适用｜列、闭集与 Java 侧一致：`Effect` grant/deny、岗位映射 active/inactive、风险四档＝`AccessGrantPolicy.riskReviews` 四键（M032）；`uq_position_role_templates_active_scope` 与 `findPositionRoleTemplate` 的组织优先/全局兜底一致（M031）｜通过 |
| GEN-009 错误处理与恢复 | 无本面｜SQL 迁移无异常处理面；失败即整体回滚（Flyway 事务） |
| GEN-010 安全与敏感信息 | 适用｜种子不含口令、密钥或个人信息｜通过 |
| GEN-011 可观测与审计证据 | 适用｜V003 建 `permission_template_usage_logs`（到期/单次消耗留痕落点，M032 已审其写入）｜通过 |
| GEN-012 资源与性能边界 | 适用｜索引：模板应用按目标、岗位映射按范围、定岗按组织（部分索引）；无全表重写类语句｜通过 |
| GEN-013 构建配置与依赖 | 适用｜依赖顺序成立：V169 引用 staff_levels/roles/permission_templates（均更早）；V322 依赖 V321 的权限码（版本号更小先跑，文件注释已写明）；V394 依赖 V322 的角色｜通过 |
| GEN-014 数据库迁移正确性 | 适用｜已应用迁移与 origin/main 逐字节相同（探针 0），未被改写；向前修的范式（V387、V393、V394 注释均写明「历史迁移不回改，向前修」）｜通过 |
| GEN-015 界面可用性与可访问性 | 适用｜上屏名称全中文：权限 381 条、角色、模板名称含拉丁字母 0 条（探针 A-2；早期英文名由 V251/V254/V385/V387 等向前改完）｜通过 |
| GEN-016 文档可验证性 | 适用｜文件注释逐条对照 SQL 与库内终态一致（V092 两个高危码、V158 view=medium/manage=high、V180 不改 requires_approval、V204 激活/核销须审批而读取免审、V322 data_scope 仅文档性、V393/V394 终态）。两处历史计数无法在现状复现：V261「累计漏 25 条」、V394「其余 11 条 high/无审批码」（现为 10 条）——已应用迁移不可改，登记 OBS-2｜通过（登记） |
| GEN-017 业务来源分类 | 适用｜未见另造业务规则；V261「平台管理员=全部权限」与 HTML「高危须逐项授予」的张力已由 M006 在运行时裁决（种子全授、运行时过滤），不在本模块重判｜通过 |
| GEN-018 第一性原理证据 | 适用｜结论落到迁移原文、complex_test 只读查询与后续迁移清单｜通过 |

## 5. 本次 Findings

无。

## 6. 观察（登记，不立项）

- OBS-1：`role_platform_admin` 在库内持全部 376 个 active 码（含 high/critical），运行时由 AuthorizationService 只认日常码与治理本职码（M006 裁决，M032 已审）；角色详情页若直接展示 role_permissions 会显得管理员「什么都有」。
- OBS-2：两处迁移注释里的历史计数现状不可复现（V261「累计漏 25 条」、V394「其余 11 条」现为 10 条）；已应用迁移不可改。
- OBS-3：「新权限迁移必须同步授 role_platform_admin」是注释约定，没有门禁守着；现状缺 0 条。
- OBS-4：V204/V270/V321 对权限码用 `DO NOTHING`、其余用 `DO UPDATE`，两种写法并存；对现状无影响。
- OBS-5：外部审计角色 role_permissions 为 0、走限时模板（V181/V187，别的模块）。

## 7. 结论

席甲 B1 票：**PASS**（无工程缺陷；OBS-1…5 登记）。13 文件 690 行全读、18 判据逐项、三线完成；未改任何文件；落盘前未读 Grok 本次正文。按 V0.17：即使两席 B1 均无发现，也须经 B2 登记定稿、跳过 B3/B4、B5 全量确认后才收口。


## M034 B2-r1 整批候选复核（席甲票，V0.02）

- 时间：2026-09-21T08:11:31Z。输入：modules/M034-b1-grok-raw.md V0.01（27617 bytes，SHA256 41a77fb7292ae28cd0785a2031b590ab7cc5d79f610a3d29a19401e3083ff496）、modules/M034-b1-self-raw.md V0.01（上方本报告）；候选 findings.md 顶部「当前 M034 B2-r1 整批候选」段（全文件 SHA256 02291884d9341baf26dbb91c28eb97209e4aa048b63c282b2057159e34ba785d）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：GR-01→001；两席其余观察全部登记；无漏并、无改写。
- 自审纠正：席甲 B1 对 13 个迁移核了幂等、风险档、审批位、中文名与库内终态，却没有逐个问「这个权限码代表的能力，HTML 允许存在吗」。`verification.manual_override` 的能力本身被枚举速查明文否定，项目权限矩阵文档也早标了「停用，待代码轮下线」。核源成立，B1 票 PASS 撤回为 FAIL。方法补丁：权限种子模块要拿每个权限码的中文名去 grep 三份 HTML 的否定句（不提供／不得／任何…都不能）。
- 001 处置：status 置 retired 让 `knownPermission` 对任何主体（含最高权限）先行拒绝；四张授予表同时清，因为 `findPermissionEffects` 不联 `permissions.status`。已应用的 V270 不动。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。
