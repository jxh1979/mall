CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M028 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按 §4「合取不变、推翻权不对等」处理；两文件非席甲所写，自动区块/CSV 曾由席甲在 M021 检查点里用生成器重生成）。2026-09-21T01:03:20Z
- 锁 `b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a`（实算＝sidecar＝ledger 首行；LOCK-AMEND-COUNT 1）。正本主工作区 HEAD `e456ba96`。
- 模块 M028「租户范围与数据隔离（技术不变量）／文档与接口定义／第1段」2 文件，33 判据（BEQR 3、BSM 5、PGM 7、GEN 18）。B3 累计 0。
- 环境前提（ledger「D-028 环境决定」）：两文件为生成物/含生成区块；主工作区旧副本落后于 origin/main，B1 前已用 `git show origin/main:<路径>` 覆盖为已发布版本（只写工作区、不动索引），现与 origin/main 逐字节一致。主工作区含未发布的 V427，其 `--check` 必红，不作证据；一致性证据取自基于 origin/main 的一次性 worktree（已删除）。
- 顺序：本席取证脚本先跑完（避免与 Grok 同时建一次性 worktree），随后派 Grok，再落本报告；本报告落盘前未读 Grok 正文（modules/M028-b1-grok-raw.md 存在=False）。只读承诺：除 D-028 记载的对齐动作外未改任何文件；数据库只对 complex_test SELECT。

## 1. 文件覆盖表

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| docs/data-isolation-and-tenant-scope-security-spec-v0.1.md | 371 | 41083 | c5aa076ed11adc8d351cd16b9354bdc3ed888f4b8fb0f1bdb6fc7268f1e30dad | 全读 |
| docs/domain-ownership-scope-audit-v0.1.csv | 336 | 60192 | d935779dd184190cc7e59567cdde61467bdf9513d2c192eb821c305b61f22faa | 全读 |

## 2. 可执行证据原文（/home/james/audit-tmp/m028_b1_run.log）

```
START=2026-09-21T00:58:55Z HEAD=e456ba96 origin/main=b99a9e5d
== 0. files (lines/bytes/sha256) + three states
docs/data-isolation-and-tenant-scope-security-spec-v0.1.md 371 41083 c5aa076ed11adc8d351cd16b9354bdc3ed888f4b8fb0f1bdb6fc7268f1e30dad
  vs HEAD:  1 file changed, 1 insertion(+), 1 deletion(-) | vs origin/main: [empty=same]
docs/domain-ownership-scope-audit-v0.1.csv 336 60192 d935779dd184190cc7e59567cdde61467bdf9513d2c192eb821c305b61f22faa
  vs HEAD:  1 file changed, 1 insertion(+) | vs origin/main: [empty=same]
== 1. generator consistency, checked in a disposable worktree at origin/main (main worktree carries staged-unpublished V427, so it cannot be in sync by construction)
SPEC_CHECK_EXIT=0
CSV_CHECK_EXIT=0
worktree files identical to main-worktree copies: spec=yes csv=yes
scratch clean
scratch removed
data isolation spec is in sync with Flyway migration scope baseline.
DOMAIN_OWNERSHIP_SCOPE_PASS: 335 tables classified
main-worktree check (expected red because of V427): spec=1 csv=1
== 2. gate wiring claims (spec 6A: verify:tenant-isolation in verify:delivery-gates; 3A: docs/verify scripts exist)
script verify:tenant-isolation: 1
script docs:data-isolation-spec: 1
script verify:data-isolation-spec: 1
script docs:domain-ownership-scope: 1
script verify:domain-ownership-scope: 1
script docs:security-baseline: 1
verify:delivery-gates includes tenant-isolation: 1 ; verify:predeploy includes data-isolation-spec: 1 domain-ownership-scope: 1
== 3. spec sec.2 reference implementation still exists?
FinanceService.listArApItems(: 1 listArApItemsByMerchant:  listArApItemsPaged: 2
currentMerchantScope impl (main worktree vs origin/main):
425:    public Optional<String> currentMerchantScope() {
426-        AccessActor actor = currentActor();
427-        String merchantId = actor.merchantId();
428-        if (merchantId != null && !merchantId.isBlank()) {
429-            return Optional.of(merchantId);
430-        }
431-        throw ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行");
432-    }
433-
origin/main:
422:    public Optional<String> currentMerchantScope() {
423-        AccessActor actor = currentActor();
424-        if (isPlatformAdminBridge(actor)) {
425-            return Optional.empty();
426-        }
427-        String merchantId = actor.merchantId();
428-        if (merchantId != null && !merchantId.isBlank()) {
429-            return Optional.of(merchantId);
430-        }
== 4. spec sec.3 historical table facts vs immutable migrations
V051 width: merchant_id VARCHAR(80) | V049 width: merchant_id VARCHAR(64)  | V046 inventory_* tables: 9 + saved_bottles: 1
28:迁移 V044/V046 的 .sql 注释已确立、本规格正式提升为强制条款：
41:| V046 | inventory 10 表 + `saved_bottles` | merchant_id | VARCHAR(64) | 部分索引；`chk_stock_adjustment_type`（**NOT VALID，待 VALIDATE**）|
44:| V049 | amusement 4 表 | merchant_id | VARCHAR(?) | 部分索引已补（V065，ISO-GAP-03 已关闭）|
45:| V051 | `compliance_reminders` | merchant_id | VARCHAR(64) | 部分索引；`uq_compliance_reminder_dedup` 去重约束 |
53:| V061 | `saved_bottles` | merchant_id（V046 已加） | VARCHAR(64) | `direct`；V061 另加 `customer_phone` 等字段（**敏感字段 at-rest 加密为后续项**，见 §6 ISO-GAP） |
69:| `amusement_incidents` | `merchant_id` + `project_id` | direct | V024, V049 | `V024__p1_amusement_safety_waiver_incident.sql`<br>`V049__amusement_security_fixes.sql` |
76:| `amusement_projects` | `merchant_id` + `space_id` | direct | V049, V084 | `V049__amusement_security_fixes.sql`<br>`V084__object_space_mapping.sql` |
80:| `amusement_safety_notices` | `merchant_id` + `project_id` | direct | V024, V049 | `V024__p1_amusement_safety_waiver_incident.sql`<br>`V049__amusement_security_fixes.sql` |
== 5. line-number references inside the spec
238:每个**查询和写入** operation 必测两条（与 `p0-openapi-operation-acceptance-matrix-v0.1.md:272-273` 绑定）：
342:**隔离缺口**：该表自身**无 scope 列**（`domain-ownership-scope-audit-v0.1.csv:131` 标注 "no direct scope column detected"）——需决策复核记录是否需按 organization/merchant 隔离（登记为 ISO-GAP，待治理负责人裁决）。
csv line 131: facility_inspection_templates,direct,project_id | supervisor_review_decisions actually at line: 281
matrix 272-273:
| ticketing | `POST` | `/api/v1/ticketing/refunds/{refund_id}/approve` | `approveTicketRefund` | 退款审批 | `StatusChangeRequest` | `ticket.refund.approve` | `staff-token` | `project-order` | web | 是 | `ticket_refund.approved` | `ticket_refund.approve_failed` |
| ticketing | `GET` | `/api/v1/ticketing/qr-refresh-config` | `getTicketQrRefreshConfig` | 读全场统一动态码刷新间隔 | `-` | `ticket.product.view` | `staff-token` | `platform` | web | 否 | `ticket.qr_refresh_config.viewed` | `ticket.qr_refresh_config.view_failed` |
797:| 终端越权 | 员工端、平板、一体机、客人端调用不属于自己的接口 | 按 `x-terminal-allowed`、设备能力、登录主体和数据范围同时校验 |
804:| 附件归档 | 合同、审批、事故、整改附件缺失或 hash 不一致 | 附件上传、下载、病毒扫描、hash 校验、权限隔离和归档留痕必须可测 |
== 6. spec sec.9 V140 lists vs migration
V140__append_only_enforcement_and_runtime_role.sql
spec immutable count claim 15 parsed 15
spec permanent count claim 15 parsed 15 ['ar_ap_item_sources', 'bar_production_ticket_events', 'customer_points_ledger', 'evidence_attachments', 'evidence_reviews', 'finance_payment_intents', 'finance_reconciliation_batches', 'finance_reconciliation_differences', 'inventory_stock_movements', 'inventory_trace_exceptions', 'inventory_trace_handovers', 'inventory_trace_steps', 'open_api_events', 'ticket_payment_intents', 'ticket_refund_lines']
immutable names absent from V140 text: []
permanent names absent from V140 text: ['inventory_trace_steps', 'inventory_trace_exceptions']
triggers in complex_test calling complex_block_mutation: 41
complex_app role referenced in V140: 15 ; application-prod.yml: 2
== 7. spec sec.10 claims: V178 / V180 / account type / roles / ADM-VND-004 / completion service
V178__outsourced_vendor_facility_assignee_permissions.sql V180__auditor_roles_readonly_seed.sql 
outsourced_vendor: migrations=2 java=3
OUTSOURCED_FACILITY: migrations=2 java=0
OUTSOURCED_SECURITY_CLEANING: migrations=2 java=0
OUTSOURCED_CONSTRUCTION: migrations=2 java=0
PartnerSubordinateCompletionService: 1 ; subordinates-completion endpoint: 0 ; ADM-VND-004 route: 1
== 8. spec sec.6 gap table facts
saved_bottles.customer_phone now encrypted? column checks: CHECK (((customer_phone IS NULL) OR ((customer_phone)::text = ''::text) OR ((customer_phone)::text ~~ 'enc:v1:%'::text))) CHECK (((transfer_to_phone IS NULL) OR ((transfer_to_phone)::text = ''::text) OR ((transfer_to_phone)::text ~~ 'enc:v1:%'::text))) 
backend/src/main/resources/db/migration/V396__pii_phone_envelope_check_widen.sql:10
backend/src/main/resources/db/migration/V396__pii_phone_envelope_check_widen.sql:12
backend/src/main/resources/db/migration/V396__pii_phone_envelope_check_widen.sql:13
security-scans files: security-scans/findings-register-v1.md security-scans/formal-security-baseline-v1.md 
SEC-P0-TENANT-ISOLATION-001 in register: 1
superpowers plan file: docs/superpowers/plans/2026-06-15-security-audit-fixes.md
== 9. CSV shape
rows 335 cols ['table_name', 'scope_type', 'scope_source', 'scope_path', 'multi_complex_note', 'file_version', 'status']
scope_type {'direct': 173, 'inherited': 79, 'global_reference': 7, 'single_complex_default': 76}
status {'covered': 335}
file_version {'V0.01': 335}
sorted True dups []
tables in complex_test not in CSV: business_entity_projects
CSV tables not in complex_test: 
== 10. three HTML + lock sha
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
END=2026-09-21T00:59:08Z
```

如实说明：探针 3 里统计 `listArApItemsByMerchant` 的那条管道把 `文件名:计数` 喂给了 bc，产生一串 `(standard_in)` 报错行（已从上方引用中滤去，原日志保留）；该项实数另行 grep：`listArApItemsByMerchant` 在 backend/src/main 共 5 处（FinanceRepository 接口与两个默认方法、Jdbc 实现、FinanceService:1695），但 `FinanceService.listArApItems()` 已不含文中所述三元式（见 CX-M028-001）。探针 6 的「permanent names absent from V140 text」两项是本席展开简写时拼出的名字，不是文档缺陷，见 OBS-1。

补充只读核对：
- `saved_bottles.customer_phone`：写入 `JdbcInventoryRepository.java:1306` `piiCipher.encrypt(…)`、读出 `JdbcInventoryRowMapper.java:285` `piiCipher.decrypt(…)`；V396 头注「下列五列自 ISO-GAP-06 起已按 PiiCipher 加密落库」并加 `chk_saved_bottles_customer_phone_pii`（库内存在，探针 8）。
- 读谓词两种写法的分布：行空放行式 `(col IS NULL OR col = :p)` 12 处，全部是「NULL＝平台默认槽」的配置类表（comprehensive_service_fee_configs、pass_gift_cost_configs、bar_commission_rate_configs、points_accrual_rate_configs、high_value_thresholds、access_grant_requests 等）；参数空放行式 `(:p IS NULL OR col = :p)` 23 处，为业务数据表（库存、财务等，旧行 NULL 对商户/项目级调用方不可见）。
- V140 中相关表实名：`inventory_trace_handovers`、`inventory_trace_handover_steps`、`inventory_trace_handover_exceptions`；端点 `PartnerController.java:75` `@GetMapping("/subordinates/completion")`。

## 3. 三线审计

- 第一性原理线。用途：给开发/测试/安全/验收一份隔离模型的单一权威文档，加一张逐表 scope 清单，并作为 predeploy 门禁的比对对象。实际行为：自动区块与 CSV 由生成器守住（同步）；手写段落没人守。未经验证的前提：「手写段落仍反映现状」——5 处不成立。
- 顺从式线：33 判据逐项见 §4；规格 371 行、CSV 336 行逐段读过（CSV 以脚本核形状与表集合，人工读表头与首、中、尾共约 20 行）。
- 对抗式线（grilling）：新同事照 §2 去 `FinanceService.listArApItems()` 找参考实现→找不到所述三元式；照 §4 的行号去验收矩阵找「必测两条」→落在票务退款行；照 §7 去 CSV 第 131 行→是别的表；照 §6A.1「一律」写法给库存表写读谓词→旧行 NULL 对所有商户可见，与本文 §4 AC-ISO-4「默认不可见」和现行库存仓储相反；照 §6 ISO-GAP-06 排期补加密→早已实现；照 §3 以为 V051 列宽 64→实为 80（无害但事实错）；自动区块会不会被手改→`--check` 即红；CSV 会不会漏表→与测试库只差未发布的 V427 一张表。

## 4. 33 判据逐项

| 判据 | 适用｜证据｜结论 |
|---|---|
| BEQR-074 隐私桌台权限 | 无本面｜两文件只描述租户/商户/项目范围隔离、append-only 与特殊人员边界，不承载、不转述「隐私桌台权限」的业务规则 |
| BEQR-115 经销价格与税务快照 | 无本面｜两文件只描述租户/商户/项目范围隔离、append-only 与特殊人员边界，不承载、不转述「经销价格与税务快照」的业务规则 |
| BEQR-192 分红税务 | 无本面｜两文件只描述租户/商户/项目范围隔离、append-only 与特殊人员边界，不承载、不转述「分红税务」的业务规则 |
| BSM-116 系统交付与上线：正式上线要做的安全加固和安装交付 | 无本面｜两文件只描述租户/商户/项目范围隔离、append-only 与特殊人员边界，不承载、不转述「系统交付与上线：正式上线要做的安全加固和安装交付」的业务规则 |
| BSM-117 正式上线时的安全加固 | 适用（引用面）｜§9 受限运行时角色 `complex_app` 与 append-only 触发器属上线安全加固的工程承接；V140 存在、`application-prod.yml` 引用 `complex_app`、complex_test 内 41 个触发器调用 `complex_block_mutation`（探针 6）。文档未改写 HTML 的弱口令/U 盘密钥条款｜通过 |
| BSM-118 能自动识别环境的安装包 + 内附教程 | 无本面｜两文件只描述租户/商户/项目范围隔离、append-only 与特殊人员边界，不承载、不转述「能自动识别环境的安装包 + 内附教程」的业务规则 |
| BSM-119 和谁有关 | 无本面｜两文件只描述租户/商户/项目范围隔离、append-only 与特殊人员边界，不承载、不转述「和谁有关」的业务规则 |
| BSM-120 已确认规则和实施前输入项 | 无本面｜两文件只描述租户/商户/项目范围隔离、append-only 与特殊人员边界，不承载、不转述「已确认规则和实施前输入项」的业务规则 |
| GEN-001 必要性与最小性 | 适用｜规格文档是隔离模型的单一权威文档、CSV 是逐表清单，二者被 `verify:predeploy` 门禁引用，有存在理由｜通过 |
| GEN-002 可执行正确性 | 适用｜生成器一致性：在基于 origin/main 的一次性 worktree 里 `--check` 两个均通过（探针 1：SPEC_CHECK_EXIT=0「in sync」、CSV_CHECK_EXIT=0「335 tables classified」），且与主工作区副本逐字节相同；主工作区自身 `--check` 为红系 V427 未发布所致（D-028）。手写部分有 5 处与现状不符（CX-001…005）｜不通过 |
| GEN-003 测试有效性 | 适用（间接）｜两文件本身无单测；一致性由两个 `--check` 门禁守住（新增迁移不重生成即红），手写段落无任何机审——5 条发现都出在手写段｜登记 |
| GEN-004 信任边界校验 | 适用｜§2「scope 只来自安全 token、绝不取请求参数」与 M027 抽核的三域写入路径一致｜通过 |
| GEN-005 授权与最小权限 | 适用｜§4 AC-ISO-3「平台管理员（scope 空）→ 可见全量」与 origin/main `AuthorizationService.currentMerchantScope()`（平台管理员桥返回 Optional.empty）一致；§6A.1 的读路径谓词「一律」写法与 §4 AC-ISO-4、与 23 处业务表仓储谓词相矛盾（CX-M028-005）｜不通过 |
| GEN-006 数据完整性 | 适用｜CSV 335 行、表名有序无重复、status 全 covered；与 complex_test 表集合只差 `business_entity_projects`（来自未发布的 V427，测试库已应用）——与 D-028 事实一致，不是 CSV 缺陷｜通过 |
| GEN-007 并发与幂等 | 无本面 |
| GEN-008 接口与模式一致性 | 适用｜§2 参考实现所述三元式已不在 `FinanceService.listArApItems()`，现行为两维范围下推 `listArApItemsPaged(merchantId, projectId, …)`（CX-M028-001）；§3/§6 对不可变迁移的列宽与表数写错（CX-M028-003）｜不通过 |
| GEN-009 错误处理与恢复 | 无本面 |
| GEN-010 安全与敏感信息 | 适用｜见 PGM-059 行（CX-M028-004）｜不通过（文档侧） |
| GEN-011 可观测与审计证据 | 适用｜§6A.2 越权台账落点 `security-scans/findings-register-v1.md`、`formal-security-baseline-v1.md` 存在且含 `SEC-P0-TENANT-ISOLATION-001`（探针 8）｜通过 |
| GEN-012 资源与性能边界 | 无本面 |
| GEN-013 构建配置与依赖 | 适用｜文中 6 个 npm 脚本全部存在；`verify:tenant-isolation` 在 `verify:delivery-gates` 内，两个文档门禁在 `verify:predeploy` 内（探针 2）｜通过 |
| GEN-014 数据库迁移正确性 | 适用（引用面）｜§3A 自动区块与迁移目录一致（见 GEN-002）；§3 手写快照对 V046/V049/V051 的描述有误（CX-M028-003）｜不通过 |
| GEN-015 界面可用性与可访问性 | 无本面 |
| GEN-016 文档可验证性 | 适用｜核心判据。5 条不可复现/自相矛盾的声称：参考实现过期（001）、两处行号引用失效（002）、不可变迁移的事实写错（003）、已实现的加密仍标未实现（004）、读谓词「一律」规则与本文 AC-ISO-4 及现行实践矛盾（005）。其余可复现：V140 两份 15 张表清单与迁移文本一致（§9 的 handovers/steps/exceptions 为简写，实名 inventory_trace_handover_steps/_exceptions，登记 OBS-1）、V178/V180 存在、`outsourced_vendor` 与三个外包角色在迁移中存在、`PartnerSubordinateCompletionService` 与 `ADM-VND-004` 存在（端点实为 `/subordinates/completion`，文中写作 subordinates-completion，登记 OBS-2）、安全基线与 plan 文件存在｜不通过 |
| GEN-017 业务来源分类 | 适用｜五条发现均为技术性（安全设计文档对实现/迁移/门禁的描述），不涉业务规则发明；§10 对 HTML 的转述标明「口径向业务 HTML 对齐」，未见发明｜通过 |
| GEN-018 第一性原理证据 | 适用｜结论落到只读命令输出（m028_b1_run.log 与补充 grep/SELECT）、迁移与源码原文｜通过 |
| PGM-016 应急与隐私 | 无本面｜两文件只描述租户/商户/项目范围隔离、append-only 与特殊人员边界，不承载、不转述「应急与隐私」的业务规则 |
| PGM-026 主要功能 | 无本面｜两文件只描述租户/商户/项目范围隔离、append-only 与特殊人员边界，不承载、不转述「主要功能」的业务规则 |
| PGM-057 应急恢复和客人隐私：最坏情况有后路，个人信息看不到明文 | 适用（引用面）｜§9 append-only 与 §10 审计人员敏感查看留痕的转述与 HTML「个人信息看不到明文、敏感查看须留痕」一致，未缩小｜通过 |
| PGM-058 紧急恢复（破窗机制） | 无本面｜两文件只描述租户/商户/项目范围隔离、append-only 与特殊人员边界，不承载、不转述「紧急恢复（破窗机制）」的业务规则 |
| PGM-059 客人与员工的个人信息保护 | 适用｜HTML 要求个人信息一律加密存放。文 §3 V061 行与 §6 ISO-GAP-06 仍写「saved_bottles.customer_phone…at-rest 加密未实现（明文存储）」，而现行 `JdbcInventoryRepository:1306` 写入即 `piiCipher.encrypt`、V396 已加 `enc:v1:` 信封 CHECK——文档把已满足 HTML 的现状写成未满足，属过期缺口（CX-M028-004）｜不通过（文档侧） |
| PGM-060 会员身份、营销与生物信息治理 | 无本面｜两文件只描述租户/商户/项目范围隔离、append-only 与特殊人员边界，不承载、不转述「会员身份、营销与生物信息治理」的业务规则 |
| PGM-066 已拍板规则和待排期问题 | 无本面｜两文件只描述租户/商户/项目范围隔离、append-only 与特殊人员边界，不承载、不转述「已拍板规则和待排期问题」的业务规则 |

## 5. 本次 Findings（临时编号；均技术性、工程缺陷成立；依据类型 4）

### CX-M028-001 §2 参考实现已不存在
- `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md:33`「参考实现：`FinanceService.listArApItems()`（`currentMerchantScope().isPresent() ? listArApItemsByMerchant : listArApItems`）」。现行 `FinanceService.listArApItems(keyword, page, size)`（`FinanceService.java:162-172`）不是该三元式，而是取 `currentMerchantScope()` 与 `currentProjectScopeQuietly()` 两维后下推 `repository.listArApItemsPaged(merchantId, projectId, keyword, page, size)`；文中的三元式如今只剩在 `FinanceRepository.java:160/183` 两个默认方法里，`listArApItemsByMerchant` 本身仍存在（接口 :151、Jdbc 实现 :122、`FinanceService.java:1695` 一处调用）。读者按文去 `FinanceService.listArApItems()` 找不到所述写法。最小处置：把括号内换成现行写法（两维范围下推 `listArApItemsPaged`），句式不变。判据 GEN-016 / GEN-008。

### CX-M028-002 两处行号引用失效
- `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md:238`「`p0-openapi-operation-acceptance-matrix-v0.1.md:272-273`」现为两条票务接口行；`docs/data-isolation-and-tenant-scope-security-spec-v0.1.md:342`「`domain-ownership-scope-audit-v0.1.csv:131`」现为 facility_inspection_templates，`supervisor_review_decisions` 在第 281 行（CSV 随迁移增行，行号必然漂移）。最小处置：去掉行号，改为不会漂移的指称（矩阵按章节/关键词，CSV 按表名行）。判据 GEN-016。

### CX-M028-003 对不可变迁移的事实写错（§3 与 §6 ISO-GAP-04 清单）
- `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md:45` V051 `compliance_reminders.merchant_id` 写 VARCHAR(64)，迁移为 VARCHAR(80)；`:44` V049 写 VARCHAR(?)，迁移为 VARCHAR(64)；`:41` 与 ISO-GAP-04 清单「V046：inventory 10 表 + saved_bottles」，迁移为 inventory 9 表 + saved_bottles。最小处置：改成迁移原值（80／64／9 表）。§3 虽标「历史快照仅作背景」，但描述的是不可变迁移，事实应当对。判据 GEN-016 / GEN-014 / GEN-008。

### CX-M028-004 已实现的手机号加密仍标「未实现（明文存储）」
- `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md:53`（§3 V061 行「敏感字段 at-rest 加密为后续项」）与 §6 ISO-GAP-06（P1「at-rest 加密未实现（明文存储）」）。现状见 §2 补充核对：写入即加密、库上有信封 CHECK（V396 头注亦点名 ISO-GAP-06 已落库加密）。最小处置：ISO-GAP-06 改为已关闭并注明承接（PiiCipher 写入加密 + V396 信封 CHECK），§3 V061 行同步。判据 GEN-016 / GEN-010 / PGM-059（文档侧）。

### CX-M028-005 §6A.1 读谓词「一律」写法与本文 AC-ISO-4 及现行实践矛盾
- `docs/data-isolation-and-tenant-scope-security-spec-v0.1.md:304`「带 scope 列的表，读路径一律写 `(<scope_col> IS NULL OR <scope_col> = :<scope_col>)`」。该式让 scope 列为 NULL 的行对所有调用方可见——这正是配置类表「NULL＝平台默认槽」要的语义（现行 12 处全是这类表），但对业务数据表与本文 §4 AC-ISO-4「旧行默认不可见」相反，现行 23 处业务表仓储用的是 `(:p IS NULL OR col = :p)`。照「一律」去写业务表读路径，会把历史 NULL 行暴露给所有商户。最小处置：把「带 scope 列的表…一律写」限定为「以 NULL 表示平台默认槽的配置类表」，并补一句业务数据表按 AC-ISO-4 用参数空放行式；禁止 COALESCE 哨兵的结论与理由不动。判据 GEN-016 / GEN-005 / GEN-008。

## 6. 观察（登记，不立项）

- OBS-1 §9「`inventory_trace_handovers`/`steps`/`exceptions`」为简写，实名 inventory_trace_handover_steps、inventory_trace_handover_exceptions；15+15 张表与 V140 文本一致。
- OBS-2 §10「subordinates-completion 端点」实为 `/subordinates/completion`。
- OBS-3 自动区块称「170 张含 scope 字段的表」，CSV direct 为 173——两个生成器的口径不同（前者按迁移文本出现 scope 列，后者逐表分类），各自 `--check` 通过。
- OBS-4 CSV `file_version` 列 335 行恒为 V0.01、`status` 恒为 covered，由生成器写死。
- OBS-5 §5「旧行回填策略（待执行）」含 V046 `chk_stock_adjustment_type` 待 VALIDATE，与 M027 登记一致。
- OBS-6 §6 末「缺口登记状态（截至 2026-06-18，迁移基线 V061）」为历史快照说明，文首已声明以 §3A 为准。
- OBS-7 主工作区 `AuthorizationService.currentMerchantScope()` 的暂存改动（无商户即抛）与 origin/main（平台管理员桥返回空）不同；本文 AC-ISO-3 与 origin/main 一致。属未发布改动联动项（同 M025 登记）。

## 7. 结论

席甲 B1 票：**FAIL**（CX-M028-001…005，均技术性、工程缺陷成立）。2 文件全读、33 判据逐项、三线完成；除 D-028 记载的对齐外未改任何文件；落盘前未读 Grok 本次正文。


## M028 B2-r1 整批候选复核（席甲票，V0.02）

- 时间：2026-09-21T01:23:15Z。输入：modules/M028-b1-grok-raw.md V0.01（42611 bytes，SHA256 9b5bc5636987e448bfd4ed51e9193815874a6102aa84dcd0324ef9fa98829e6c）、modules/M028-b1-self-raw.md V0.01（上方本报告）；候选 findings.md 顶部「当前 M028 B2-r1 整批候选」段（全文件 SHA256 65e6cdf27591a8eb106d06ec808cf3d90fd1eafbfaec2388cf682da498b5362c）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：Grok 11 条与席甲 5 条归并为 10 个 ID + 1 条拟登记（见候选「来源归并」列）；两席观察全部登记；无漏并、无改写。
- 自审纠正：①分类——席甲 B1 全判技术性；本规格整体是治理地图隔离要求的下位设计，按四分类顺序细化在先，采 Grok；010 为与治理地图明文的冲突。②席甲 B1 漏五条，逐条核源成立：GR-05（§3 14 行／§3A 170 行／CSV 335 行并不逐行对应）、GR-07（该证据 ID 除本句外全仓 0 命中）、GR-09（cam 两表已被 V274 删除、现库 41 个触发器——席甲 B1 只核了表名在 V140 文本里出现，没去对现库，这一步漏了）、GR-10（两份治理文档的 §4 不是一回事）、GR-11（治理地图 L676 明文内审可看本综合体全部数据）。③席甲 B1 的 CX-001 小标题写「参考实现已不存在」不准确——`listArApItemsByMerchant` 仍在仓储里，过期的是「`FinanceService.listArApItems()` 用该三元式」这一指称；正文与候选以后者为准。
- GR-06：席甲按本文档自身的路径约定核为可解析，拟只登记；Grok 有否决权，若其说明应入队的理由成立，以其意见为准再议。
- 处置只改规格文档手写段与文首版本行；自动区块、CSV、生成器、代码、三份 HTML 不动；010 是改文档向 HTML 对齐，不改 HTML。
- 无添加项、无 HTML 申请、无 questions。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。
