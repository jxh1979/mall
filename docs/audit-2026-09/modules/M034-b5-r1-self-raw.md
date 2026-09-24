CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M034 B5-r1 修复后全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；V429 为席甲 B3 所写，且席甲 B1 的 PASS 已被 Grok 推翻过一次，以 Grok 票为准）。2026-09-21T08:41:14Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`，origin/main `4f6ef48b`。
- 模块 M034 13 个已应用迁移（未改）＋本轮新增 V429（§1 全读），18 判据；B3 累计 1。complex_test 现 Flyway 最高 429。
- 顺序：本席取证脚本先跑完，随后落本报告；落盘前未读 Grok 正文（modules/M034-b5-r1-grok-raw.md 存在=False）。只读承诺：未改任何已跟踪文件；数据库只 SELECT。
- 本轮补的方法（B1 漏了 GR-01）：权限种子不只核风险档与审批位，还要问「这个码代表的能力，HTML 允许存在吗」——把全部 active 权限名拿去对三份 HTML 的否定句（§2b）。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

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
| V429__retire_verification_manual_override.sql | 17 | 1360 | 903331b27bd536cdce11c3abb3547f4ec77b1c4a0f07408cf2a706c6b3d02078 | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m034_b5_run.log）

```
START=2026-09-21T08:41:04Z HEAD=e456ba96 origin/main=4f6ef48b
== 0. files (lines/bytes/sha256 | vs origin/main)
V003__p0_access_permission_authz_slice.sql 181 8063 fb1b158cfe1d15ec3396396105f25941ceba8a56b7a04cb7e7c800a4e4a1b638 | []
V092__amusement_operational_config_permissions.sql 23 1392 eabcc470c5cfb3e6d4aa59023bae90753afb381e9550181bdeb8bcfdf543203b | []
V158__refund_authority_admin_permissions.sql 23 1419 4a5d2728b52f2513e32f37e019f278c2c657ff1f300db9344de30080189b15a2 | []
V169__access_role_assignment_closure.sql 53 2410 a25316489af69c5a5318b743f1220e5deca75adee2e7d8c887b0ddd68d3a5ea7 | []
V180__auditor_roles_readonly_seed.sql 268 9830 8af881c9af09eb3ef23fdd8dce36d404997d299d0c2c8cbb3f918d20e374c551 | []
V204__seed_runtime_checked_permissions.sql 16 1290 55e98c86950bcef5b8e608b7af60c2bf8a0e17659d43b285715ae72a314edcf5 | []
V261__platform_admin_full_permissions.sql 9 640 5a6a982a2e0d7c8c9d61e53938552273bfc083e30839fcf603d54c7614f938dc | []
V270__ticket_manual_override_permission.sql 16 1309 45927d048b1caa9c64116a1bfe9ee99ec1633b3dbb832f50fee8904ddc4a1fcb | []
V321__staff_self_profile_permission.sql 12 879 49b40908456a0fac3ef23759ad08e6631f542ec35f7a9d471a336abc2621f1e1 | []
V322__staff_self_service_role.sql 32 2036 3bd069d3e69773c9d00041cb8ec5deb6db667c7df6e7899f49964e18196cb8fe | []
V387__access_display_names_chinese.sql 31 3120 7f89a0e39aaa4a161400f351e1ceae5e227358783e713d49c61c77ffd2eab511 | []
V393__sensitive_business_data_permissions_require_approval.sql 8 1057 40aeb2b412ee4a07ed968be6582b4c7cbfbad0658bfcc55abecb789ba25e445f | []
V394__permission_risk_tiers_and_sensitive_codes_require_approval.sql 18 2343 acdeef24cb2e653981919b47eb0421e96a713f9fd7ce7cb14a450538ad7ceba7 | []
V429__retire_verification_manual_override.sql 17 1360 903331b27bd536cdce11c3abb3547f4ec77b1c4a0f07408cf2a706c6b3d02078 | []
== A. 库内终态（complex_test，只 SELECT）
== 1. permissions 总数 / 状态 / 风险档 / 须审批
381|375
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
role_platform_admin|375
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
-- 001 状态
flyway_top=429 status=retired role_permissions=0 template_items=0 overrides=0 active_temp_grants=0 platform_admin_missing_active=0 
== B. AccessJdbcIntegrationTests（主工作区，complex_test）
b5 MVN_EXIT=0
   AccessJdbcIntegrationTests tests=26 errors=0 skipped=0 failures=0
   AuthorizationServiceTests tests=24 errors=0 skipped=0 failures=0
   TicketingControllerTests tests=20 errors=0 skipped=0 failures=0
== C. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T08:41:10Z
```

## 2b. 权限名对 HTML 否定句的反查（/home/james/audit-tmp/m034_b5_negation.log）

```
active 权限 375 个；三份 HTML 带否定标记的句子 528 句
access.permission_template.apply	申请权限模板	≈「权限模板」@governance: …职级影响默认权限模板和审批层级，但不能替代功能权限点…
access.permission_template.manage	管理权限模板	≈「权限模板」@governance: …职级影响默认权限模板和审批层级，但不能替代功能权限点…
access.permission_template.revoke	撤销权限模板申请	≈「权限模板」@governance: …职级影响默认权限模板和审批层级，但不能替代功能权限点…
access.permission_template.view	查看权限模板	≈「权限模板」@governance: …职级影响默认权限模板和审批层级，但不能替代功能权限点…
amusement.entitlement_session.initiate	发起权益核销会话(工作手机扫描赠送权益码)	≈「工作手机」@system: …客下单、出示公司收款码、完成履约、扫码核销放行），一律在公司工作手机上做——私人手机不允许带入工作现场，进办公区需寄存…
amusement.over_capacity_policy.configure	配置游乐项目超容处置策略(经营软上限+处置模式，高危)	≈「游乐项目」@system: …因为通票里可能包含游乐项目，游乐入口要判断客人能不能进、项目容量（实时在玩人数）够不够…
amusement.over_capacity_policy.view	查看游乐项目超容处置策略	≈「游乐项目」@system: …因为通票里可能包含游乐项目，游乐入口要判断客人能不能进、项目容量（实时在玩人数）够不够…
amusement.project.manage	管理游乐项目规则	≈「游乐项目」@system: …因为通票里可能包含游乐项目，游乐入口要判断客人能不能进、项目容量（实时在玩人数）够不够…
amusement.project_entitlement.approve	审批游乐项目权益绑定删除	≈「游乐项目」@system: …因为通票里可能包含游乐项目，游乐入口要判断客人能不能进、项目容量（实时在玩人数）够不够…
amusement.project_entitlement.manage	管理游乐项目权益绑定	≈「游乐项目」@system: …因为通票里可能包含游乐项目，游乐入口要判断客人能不能进、项目容量（实时在玩人数）够不够…
amusement.session.manage	管理游乐项目会话	≈「游乐项目」@system: …因为通票里可能包含游乐项目，游乐入口要判断客人能不能进、项目容量（实时在玩人数）够不够…
amusement.session.view	查看游乐项目会话	≈「游乐项目」@system: …因为通票里可能包含游乐项目，游乐入口要判断客人能不能进、项目容量（实时在玩人数）够不够…
amusement.verification.override	强制改判游乐核验结果	≈「核验结果」@enum: …第三方只返回年龄核验结果，不得将人脸用于其他目的…
bar.combo.manage	维护酒吧套餐目录（组合商品、售价与内部结算价，属敏感成本数据）	≈「部结算价」@enum: …酒吧入场资格是目前唯一不设置内部结算价的特殊权益，进入酒吧通票可分配池…
bar.combo.view	查看酒吧套餐目录（含内部结算价）	≈「部结算价」@enum: …酒吧入场资格是目前唯一不设置内部结算价的特殊权益，进入酒吧通票可分配池…
bar.entitlement.read	读取酒吧通票权益	≈「酒吧通票」@enum: …一张酒吧通票或跨域组合通票覆盖的每一家酒吧，赠酒杯数不得低于后台配置的最低赠酒杯数（业务参数，按票种可配…
bar.entitlement.redeem	核销酒吧通票权益	≈「酒吧通票」@enum: …一张酒吧通票或跨域组合通票覆盖的每一家酒吧，赠酒杯数不得低于后台配置的最低赠酒杯数（业务参数，按票种可配…
bar.internal_settlement_price.view	查看酒吧内部结算价（成本价）	≈「内部结算」@enum: …酒吧入场资格是目前唯一不设置内部结算价的特殊权益，进入酒吧通票可分配池…
bar.product.manage	创建更新酒吧商品(含敏感内部结算价)	≈「内部结算」@enum: …酒吧入场资格是目前唯一不设置内部结算价的特殊权益，进入酒吧通票可分配池…
bar.product.view	查看酒吧商品目录(含内部结算价)	≈「内部结算」@enum: …酒吧入场资格是目前唯一不设置内部结算价的特殊权益，进入酒吧通票可分配池…
contract.approve.finance	审批合同(财务审核)	≈「财务审核」@system: …所有税费规则和税费调整均须经过财务审核、税务审核、法务审核，再由顶层平台公司的老板最终确认后生效，提交人不得审批自己提交的调整…
contract.approve.legal	审批合同(法务审核)	≈「法务审核」@system: …所有税费规则和税费调整均须经过财务审核、税务审核、法务审核，再由顶层平台公司的老板最终确认后生效，提交人不得审批自己提交的调整…
customer.stored_value.consume	扣减储值余额	≈「储值余额」@system: …公共经营大屏不得展示可识别个人的消费金额、储值余额、充值情况或个人排行榜，只能展示匿名、聚合或非个人经营信息…
finance.ar_ap.export	导出应收应付台账快照(敏感资金流)	≈「应收应付」@enum: …一律按各自总额分别收付，不得轧差——与平台内部各主体之间的应收应付同例，因涉及税务处理…
finance.ar_ap.manage	管理应收应付台账项	≈「应收应付」@enum: …一律按各自总额分别收付，不得轧差——与平台内部各主体之间的应收应付同例，因涉及税务处理…
finance.ar_ap.view	查看应收应付台账项	≈「应收应付」@enum: …一律按各自总额分别收付，不得轧差——与平台内部各主体之间的应收应付同例，因涉及税务处理…
finance.dividend.manage	管理持股分红（方案配置/算单/审批/发放）	≈「持股分红」@governance: …持股分红的方案配置、算单、审批和发放属于资金类高危动作，由最高权限审批，提交人不能审批自己的单据…
finance.dividend.view	查看持股分红（方案与分红单）	≈「持股分红」@governance: …持股分红的方案配置、算单、审批和发放属于资金类高危动作，由最高权限审批，提交人不能审批自己的单据…
finance.inventory_variance.post	过账已审核库存差异至应收应付	≈「应收应付」@enum: …一律按各自总额分别收付，不得轧差——与平台内部各主体之间的应收应付同例，因涉及税务处理…
finance.locked_backfill.approve	审批锁定应收应付补录调整	≈「应收应付」@enum: …一律按各自总额分别收付，不得轧差——与平台内部各主体之间的应收应付同例，因涉及税务处理…
finance.locked_backfill.request	申请锁定应收应付补录调整	≈「应收应付」@enum: …一律按各自总额分别收付，不得轧差——与平台内部各主体之间的应收应付同例，因涉及税务处理…
finance.merchant_expense.manage	管理商户费用台账（手工登记、冲正、自动挂账）	≈「手工登记」@system: …动记下是谁、用了哪一格、什么时间存进去、什么时间取出来，不设手工登记本，也不要员工另外填单子…
finance.turnover_rent.bill	生成商户营业额租金账单	≈「商户营业」@system: …长款当期计入该商户营业外收入并单独留痕可查，不得留存钱箱、不得用于冲抵其他班次的短款…
governance.project.view	查看平台项目	≈「查看平台」@governance: …商户不能查看平台全局和其他商户数据…
governance.space.view	查看平台空间	≈「查看平台」@governance: …商户不能查看平台全局和其他商户数据…
governance.supervisor_review.approve	审批或驳回主管复核决定(高危)	≈「主管复核」@system: …码或动态取酒码时适用，须同时满足四项：①由店长发起并经另一名主管复核，普通员工不得单独放酒…
governance.supervisor_review.manage	按主管复核策略评估操作	≈「主管复核」@system: …码或动态取酒码时适用，须同时满足四项：①由店长发起并经另一名主管复核，普通员工不得单独放酒…
governance.supervisor_review.view	查看主管复核策略	≈「主管复核」@system: …码或动态取酒码时适用，须同时满足四项：①由店长发起并经另一名主管复核，普通员工不得单独放酒…
integration.reconciliation.manage	创建第三方对账批次和差异	≈「三方对账」@system: …未完成规则映射的券不得现场核销，只能登记差异，按三方对账流程处理…
integration.reconciliation.resolve	解决第三方对账差异	≈「三方对账」@system: …未完成规则映射的券不得现场核销，只能登记差异，按三方对账流程处理…
integration.reconciliation.view	查看第三方对账批次和差异	≈「三方对账」@system: …未完成规则映射的券不得现场核销，只能登记差异，按三方对账流程处理…
inventory.stocktake.variance_review	审核库存盘点差异(审批/驳回，解锁财务过账至应收应付，高危)	≈「应收应付」@enum: …一律按各自总额分别收付，不得轧差——与平台内部各主体之间的应收应付同例，因涉及税务处理…
inventory.supply_entity.view	查看平台供货主体	≈「查看平台」@governance: …商户不能查看平台全局和其他商户数据…
merchant.profile.manage	管理商户档案	≈「商户档案」@enum: …选“其他”的商户，必须在商户档案里指定一家主管公司并说明理由，未指定的不得开通任何结算与分账…
merchant.profile.view	查看商户档案	≈「商户档案」@enum: …选“其他”的商户，必须在商户档案里指定一家主管公司并说明理由，未指定的不得开通任何结算与分账…
ticket.entitlement.approve	审批删除票务权益模板	≈「票务权益」@enum: …票务权益、通票、卡座免通票入场名额一律不抵最低消费，服务费与小费也不计入最低消费…
ticket.entitlement.manage	管理票务权益模板	≈「票务权益」@enum: …票务权益、通票、卡座免通票入场名额一律不抵最低消费，服务费与小费也不计入最低消费…
ticket.entitlement.view	查看票务权益模板	≈「票务权益」@enum: …票务权益、通票、卡座免通票入场名额一律不抵最低消费，服务费与小费也不计入最低消费…
ticket.order.create	发放票务权益	≈「票务权益」@enum: …票务权益、通票、卡座免通票入场名额一律不抵最低消费，服务费与小费也不计入最低消费…
ticket.verify.view	查看已发放票务权益	≈「票务权益」@enum: …票务权益、通票、卡座免通票入场名额一律不抵最低消费，服务费与小费也不计入最低消费…
voucher.internal_price.view	查看券内部结算价（成本价）	≈「内部结算」@enum: …酒吧入场资格是目前唯一不设置内部结算价的特殊权益，进入酒吧通票可分配池…
命中 51 个权限（候选，逐条人工判读见报告）
```

## 3. 三线审计

- 第一性原理线：用途/实际行为同 B1。修后：字典里不再有 HTML 明文否定的「人工放行」能力；其余终态未变（381 个权限、active 由 376 变 375、平台管理员缺 0）。
- 顺从式线：18 判据逐项见 §4。
- 对抗式线（grilling）：V429 在没有该码的库上跑→五条语句命中 0 行也成功；该码若日后被别的迁移用 `ON CONFLICT DO UPDATE … status = EXCLUDED.status` 重新种回 active→只有显式重写才会，V270 用的是 `DO NOTHING` 且已应用不再跑；最高权限还能不能放行→不能，`knownPermission` 在最高权限判断之前；角色页/模板页还会不会列出→`listPermissions` 只列 active；端点还在→在，但对任何主体都 403，下线登记 XM-M034-A；否定句反查有没有第二个相反的码→见 §2b：51 个候选逐条判读，50 个只是与否定句共用名词（权限模板、内部结算价、应收应付、主管复核等），句意并不否定该能力；1 个成立——`amusement.verification.override`（强制改判游乐核验结果）与同一句「系统不提供人工放行」同根，项目权限矩阵文档 L78/L269 同样标「停用，待代码轮下线」，库内仍 active。它由 V014 种入（另有 V170/V251/V254 改过其风险档与名称），不在本模块 13 个文件内，按模块边界登记 XM-M034-B 转游乐迁移/后台模块，由该模块自己的前向迁移停用；本模块不代改。

## 4. 18 判据逐项

| 判据 | 适用｜证据｜结论 |
|---|---|
| GEN-001 必要性与最小性 | 适用｜13 个迁移各有单一目的（V003 权限骨架与首批权限码；V092/V158/V204/V270/V321 权限种子；V169 岗位映射与定岗表；V180 审计员只读角色与模板；V261 平台管理员补齐；V322 员工自助角色；V387 中文展示名；V393/V394 五类高危补审批与风险档约束）；无重复或互相抵消的语句｜通过 |
| GEN-002 可执行正确性 | 适用｜14 个版本（13＋V429）在 complex_test 的 flyway_schema_history 均 success；库内终态与各迁移意图一致（探针 A）；AccessJdbcIntegrationTests 26/26 绿｜通过 |
| GEN-003 测试有效性 | 适用｜迁移由 Flyway 校验（Spring 集成测试启动即 validate；M031/M032 基线里 AccessJdbcIntegrationTests 24/25 绿）与 `AccessJdbcIntegrationTests` 的字典/角色断言间接守住；迁移文件本身无单测属常态｜通过（间接） |
| GEN-004 信任边界校验 | 适用｜CHECK 闭集四条均在库（effect×2、岗位映射 status、risk_level 四档；探针 A-7）；V394 先把 normal 归 low 再加约束，顺序正确｜通过 |
| GEN-005 授权与最小权限 | 适用｜五类高危须审批的终态同 B1；人工放行权限码（与枚举速查「系统不提供人工放行…任何岗位、任何权限都不能」相反）已由 V429 停用并清掉全部授予来源，`permissionExists` 为假、对任何主体一律拒绝（001，探针 A 终态）；本轮另把全部 active 权限名拿去对三份 HTML 的否定句（§2b）：本模块 13 个迁移种入的码里未见第二个相反的能力码；同根的 `amusement.verification.override`（强制改判游乐核验结果）由 V014 种入、属游乐迁移模块，登记 XM-M034-B｜通过 |
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
| GEN-017 业务来源分类 | 适用｜001 已修：字典不再提供 HTML 明文否定的能力；V261「平台管理员=全部权限」与 HTML 的张力仍由 M006 运行时裁决，不在本模块重判｜通过 |
| GEN-018 第一性原理证据 | 适用｜结论落到迁移原文、complex_test 只读查询与后续迁移清单｜通过 |

## 5. 已定稿 001 现态

已修（B3-r1），B4-r1 双 PASS；修复队列「是」= 0。XM-M034-A 登记未变。

## 6. 本次 Findings

本模块文件无新增。新登记跨模块观察 XM-M034-B（→游乐迁移/后台模块）：`amusement.verification.override` 与「系统不提供人工放行」同根，库内仍 active，端点 `AmusementController` L41/L49 与 `AmusementService` L114-129 仍在。

## 7. 结论

席甲 B5-r1 票：**PASS**。14 文件全读、18 判据逐项、取证脚本实跑复核；未改任何已跟踪文件；落盘前未读 Grok 本次正文。
