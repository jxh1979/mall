CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M020 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（作者自审票，按控制合同 §4「合取不变、推翻权不对等」处理）。2026-09-20T10:45:17Z
- 控制合同 V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 V0.11（实算见首行，与 sidecar/ledger 顶部逐字相同；LOCK-AMEND-COUNT 1）。
- 正本主工作区 HEAD `e456ba96`（含 2026-09-11 本地提交 eaccb9c5「audit M020: settlement dispute is a note only」＝OPEN PR #443 分支 audit-m020-r1，未合并 origin/main `a29220db`）；工作区 tree `2844941461a41841bfe8deb8bbacb8c0b510e8c8`。三份业务 HTML 只读。
- 模块 M020「商户档案、商户类型与商户侧门户／后台实现／第1段」，4 个后端文件（计划有效内容行 242；§1 实算），映射 22 判据（BSM-001/003/114、GEN-001…018、PGM-001）。历史未审；eaccb9c5 不视为已审 PASS。
- 顺序：Grok 先派、本席独立取证并落盘本报告；本报告落盘前未读取 `M020-b1-grok-raw.md` 任何正文（该文件此刻存在=False，未读）。
- 只读承诺：未修改任何源码/文档/HTML/锁/台账；命令全部只读（`git write-tree` 用临时索引；psql 只 SELECT complex_test）。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| backend/src/main/java/com/complex/platform/merchantportal/MerchantContractView.java | 40 | 1348 | 3ffb0b7d30655784c7a2bf34ed72887c024b4bf4b992ba9c9dc81b9487aad1d8 | 全读 |
| backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalController.java | 55 | 2583 | 5485f91571fde266488a9b6d0acb6c827befc63022500b4ea4a5ba0094dc8bbc | 全读 |
| backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalService.java | 138 | 7155 | 54c3506234ad1545e4d9de2b1a61213506325d0a62f910eca86a4267cc2987c3 | 全读 |
| backend/src/main/java/com/complex/platform/merchantportal/dto/MerchantSettlementDisputeRequest.java | 9 | 245 | c55d2d3ca6d102b71d5e46e0a7ba48f0b7516aaac639448b8cb17d685a9d2052 | 全读 |

只读依赖（追流不审）：`access/AuthorizationService`、`contract/ContractRepository`+`JdbcContractRepository`、`contract/ContractSettlementRecord`、`contract/ContractService`（settle/resolveDispute）、`common/api/PageResponse`、`common/api/GlobalExceptionHandler`+`FieldLabels`、`common/api/ApiRequests`、`common/idempotency/IdempotencyService`、`evidence/AuditEventService`、迁移 V010/V162/V163/V171/V200/V251/V307、`docs/openapi/openapi-p0.yaml` L20380-20475、`apps/web` 商户专区两页与 `adminRoutes.ts`、测试类 `MerchantPortalServiceTests`/`MerchantPortalJdbcIntegrationTests`（属 M022）。

## 2. 可执行证据原文（/home/james/audit-tmp/m020_b1_run.log）

```
START=2026-09-20T10:42:58Z HEAD=e456ba96 origin/main=a29220db
== 0. four files (lines/bytes/sha256)
backend/src/main/java/com/complex/platform/merchantportal/MerchantContractView.java 40 1348 3ffb0b7d30655784c7a2bf34ed72887c024b4bf4b992ba9c9dc81b9487aad1d8
backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalController.java 55 2583 5485f91571fde266488a9b6d0acb6c827befc63022500b4ea4a5ba0094dc8bbc
backend/src/main/java/com/complex/platform/merchantportal/MerchantPortalService.java 138 7155 54c3506234ad1545e4d9de2b1a61213506325d0a62f910eca86a4267cc2987c3
backend/src/main/java/com/complex/platform/merchantportal/dto/MerchantSettlementDisputeRequest.java 9 245 c55d2d3ca6d102b71d5e46e0a7ba48f0b7516aaac639448b8cb17d685a9d2052
== 1. vs HEAD (expect empty) / porcelain / eaccb9c5 containment
eaccb9c5 in HEAD: yes; in origin/main: no; open PR head audit-m020-r1: 443
== 2. merchant scope: real semantics vs class doc / dead branches / test stub / openapi / V200
 public Optional<String> currentMerchantScope() {
 AccessActor actor = currentActor();
 String merchantId = actor.merchantId();
 if (merchantId != null && !merchantId.isBlank()) {
 return Optional.of(merchantId);
 }
 throw ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行");
 }

service scope call lines: 54 80 113 ; orElseGet/platform-all lines: 58 66 84 89 
class doc L23-24:  * 数据边界铁律:currentMerchantScope——商户账号只见本商户(员工无 merchant 抛 MERCHANT_SCOPE_REQUIRED  * fail-closed),平台管理员 scope 为空走全量监督口径;资金/审批的平台侧权威流程不变 
test stub: 71:        when(authz.currentMerchantScope()).thenReturn(Optional.empty());
openapi 全量 desc lines: 20386 20409 
V200 L10: -- 平台管理员默认可监督商户自助层(与其它域 role_platform_admin 全授范式一致)。
MERCHANT_SCOPE_REQUIRED since: b1fd6534 2026-06-18; portal feature: 4aa4e251 2026-07-02 feat(merchant-portal): 商户专区切片3/4——我的对�
currentMerchantIdQuietly users outside access: backend/src/main/java/com/complex/platform/floorplan/FloorPlanService.java backend/src/main/java/com/complex/platform/ticketing/TicketingService.java backend/src/main/java/com/complex/platform/gate/AdmissionGuard.java 
== 3. dispute note overwrites platform dispute trail
record fields:  String id,  String contractId,  String triggerType,   String status,   List<Map<String, Object>> settlementItems,   String disputeReason,  String resolvedBy,  Instant resolvedAt,  String projectId,  Instant createdAt,  Instant updatedAt 
portal save L123-126:  ContractSettlementRecord disputed = contractRepository.saveSettlement(new ContractSettlementRecord(  settlement.id(), settlement.contractId(), settlement.triggerType(), settlement.status(),  settlement.settlementItems(), reason, null, null,  settlement.projectId(), settlement.createdAt(), now)); 
DISPUTABLE L31-32:  private static final Set<String> DISPUTABLE_STATUSES =  Set.of("pending_settlement", "deposit_offsetting", "arrears_collecting"); 
settle() dispute open: 561:        String status = dispute != null ? "disputed" : "pending_settlement";
resolveDispute writes: 3:586-                settlement.disputeReason(), authorizationService.currentActor().id(), now, settlement.projectId(),
upsert overwrites: 250:                    dispute_reason = EXCLUDED.dispute_reason, resolved_by = EXCLUDED.resolved_by,
settle_opened metadata: 567- settlement.id(), null, actual.idempotencyKey(), Map.of("trigger", trigger, "status", status));
== 4. reason length: column / openapi / service / handler
backend/src/main/resources/db/migration/V162__contract_lifecycle.sql:121:    dispute_reason VARCHAR(500),
20432: description: 商户对未结清结算单提交异议,结算单挂起为「有争议」; 平台按既有裁决流(contract.settlement.dispute.re
20440: x-idempotency-required: true
20449: maxLength: 64
20463: description: 操作幂等键；重复提交必须返回同一处理结果，避免重复写入。
20464: maxLength: 128
20465: operator_note:
20467: description: 操作留痕备注。
20468: maxLength: 1000
20472: maxLength: 2000
20473: responses:
20474: "200":
service length() checks: 0; handler DataIntegrity: 151:     * 不拦父类 DataIntegrityViolationException——NOT NULL/CHECK/FK 违例多为服务端 bug,仍应落 500 +
== 5. idempotency: norm vs portal
services calling idempotencyService.execute: 31; portal: 0
controllers using ApiRequests.requireIdempotencyKey: 32; portal inline: 51:            throw ApiException.badRequest("IDEMPOTENCY_KEY_REQUIRED", "idempotency_key is require
== 6. operator_note: dto / openapi vs service
dto: 6: String operatorNote,; service uses operatorNote: 0; audit metadata L134:  Map.of("merchant_id", contract.merchantId(), "reason", reason)
other services consuming operatorNote(): 18
== 7. permissions: code / seeds / complex_test actual (read-only)
requirePermission lines: 53: authorizationService.requirePermission("contract.view");;79: authorizationService.requirePermission("merchant.portal.settlement.view");;100: authorizationService.requirePermission("merchant.portal.settlement.dispute");;
contract.view|role_auditor_internal,role_platform_admin
merchant.portal.settlement.dispute|role_platform_admin
merchant.portal.settlement.view|role_platform_admin
merchant roles seeded: 0; flyway top: 427
== 8. i18n: handler translates ApiException messages
61:                FieldLabels.translate(exception.getMessage())));
31: "idempotency_key", "请勿重复提交，请刷新后再试");
256: p("settlement_id", "结算单号");
461: p("reason", "原因");
== 9. frontend callers / routes
apps/web/src/admin/pages/MerchantZoneSettlementsPage.tsx:83:      setRows(await api.get<MerchantZoneSettlementRow[]>("/api/v1/merchant-porta
apps/web/src/admin/pages/MerchantZoneContractsPage.tsx:109:      setRows(await api.get<MerchantZoneContractRow[]>("/api/v1/merchant-portal/c
apps/web/src/admin/adminRoutes.ts:48:  { pageId: "ADM-MER-002", label: "商户授权工作台", module: "商户门户", group: "governance",
91:  { pageId: "ADM-MER-005", label: "我的对账", module: "商户专区", group: "merchant", route: "/admin/merchant-zone/settlements", requiredPermissions: ["merchant.portal.settlement.view"], accountForms:
92:  { pageId: "ADM-MER-006", label: "我的合同", module: "商户专区", group: "merchant", route: "/admin/merchant-zone/contracts", requiredPermissions: ["contract.view"], accountForms: ["merchant"] },
== 10. failure audit norm
services with recordFailure/withFailureAudit: 47; ContractService: 0; portal: 0; openapi x-audit-event-failure ops: 732
== 11. HTML anchor sentences (read-only)
<td>商户侧页面</td>
<td>餐饮、零售、酒吧等商户</td>
<td>在授权业务范围内全开放，处理本商户经营、供应/库存、对账、结算、员工和看板，不看平台全局。</td>
商户提出的投诉与异议，与客人投诉并入同一通道，由客户关怀部统一受理，不区分对客与对商户；商户只能就数字提出异议——包括漏算、少算、多算，不得对规则本身提出异议，规则以合同约定为准；提出异议期间商户的付款义务不暂停，先按原值付款，异议成立后多退少补。商户专区须设|我的对账（结算与分成明细，有疑问可留言提异议）|
<h2>商户侧：在授权业务范围内全开放</h2>
<p class="lead">商户侧不是平台后台，也不是只读页面。商户在自己的授权业务范围内完整使用相关功能，但不能跨商户和看
== 12. HTML + lock sha; tree
4396bb7112f1421686d2da273c6d008fedb0b9e75977371c57ac72d4dbae1d66 eebc5b417f8ce30799852b1b0315c5619cb271ce100bd52e9b9b6a4a33268d66 a7fe8c015c61564bf60d78724f9749dc0eb272371b82f97fc69bd3e2bb50e0c1 b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a 
tree=2844941461a41841bfe8deb8bbacb8c0b510e8c8
END=2026-09-20T10:43:00Z
```

## 3. 三线审计方法与覆盖

- 对象口径：4 文件 242 行逐行读完；判据锁 M020 映射 22 项；类注释/OpenAPI/测试的每条行为声称按对象追现行真实现（§2 探针 2–10）。
- 第一性原理线：业务地图 L1848-1850（商户侧页面：在授权业务范围内处理本商户对账、结算…不看平台全局）、L1322（我的对账：有疑问可留言提异议；提出异议期间付款义务不暂停）、治理地图 L749-750（不能跨商户和看平台全局）、PGM-001 L256 段（数据可溯源正反可查；同一操作重复提交只认一次）为四文件的上位；四文件的存在理由＝把「我的合同 / 我的对账 / 结算异议留言」三条商户自助端点固化为按商户 scope 硬闸的只读视图与留言写入。三问：用途（商户自助只读 + 留言）、实际行为（§4）、未经验证前提（「平台管理员 scope 为空走全量」「重复提交返回同一结果」「操作留痕备注被留痕」——三条均未被真实现支撑）。
- 顺从式线：22 判据逐族（§5）。对抗式线（grilling）：平台管理员照注释调用会怎样（§2 探针 2：403，全量分支不可达）；平台先开有争议结算并裁决后商户再留言会怎样（探针 3：裁决人/裁决时间被清空、平台理由被覆盖）；照契约写 2000 字留言会怎样（探针 4：列宽 500 → 500 内部错误）；同一幂等键重试会怎样（探针 5：再写库再落审计、不经 IdempotencyService）；operator_note 填了去哪（探针 6：丢弃）；出厂字典谁能调（探针 7：仅 role_platform_admin，而其正被 scope 闸拒绝）；英文报错是否出屏（探针 8：GlobalExceptionHandler 经 FieldLabels 中文化，通过）。
- 实现模块本轮不跑测试（B1 只读取证；B3 按 TDD 先红后绿）；本席禁止修复。

## 4. 与现行实现 / 契约的分叉对照（要害）

| 声称（位置） | 现行事实（§2 探针） | 结论 |
|---|---|---|
| Service:23-24 类注释「平台管理员 scope 为空走全量监督口径」；:56-58/:82-84 `orElseGet(平台全量)`；:66/:89 `"platform-all"`；测试 :69-79 桩 `currentMerchantScope()→Optional.empty()`；OpenAPI L20386/L20409「平台管理员为全量监督口径」；V200 L10 | `AuthorizationService.currentMerchantScope():425-432` 只返回 `Optional.of(merchantId)` 或抛 403 MERCHANT_SCOPE_REQUIRED，从不 empty（自 2026-06-18 b1fd6534，早于 2026-07-02 切片 4aa4e251）；无商户账号（含平台管理员）三端点一律 403；全量分支与 platform-all 为死代码；单元测试用真实现不可能产生的桩值得到假绿 | CX-001 |
| Service:123-126 `reason, null, null` 写 disputeReason/resolvedBy/resolvedAt；DISPUTABLE 含 pending_settlement | `ContractService.settle:558-565` 平台开账带异议 → status disputed + disputeReason；`resolveDispute:583-587` 裁决后回 pending_settlement 并写 resolvedBy=裁决人、resolvedAt=now（理由保留）；`JdbcContractRepository.saveSettlement:249-251` upsert 覆写三列；`contract.settle_opened` metadata 只有 trigger/status（:567）→ 首次正常流：开账（有争议）→裁决→商户留言 ⇒ 裁决人/时间清空、平台理由被商户留言覆盖且无处回查 | CX-002 |
| Service:104-107 只判空；OpenAPI L20472 `reason maxLength: 2000` | `V162:121 dispute_reason VARCHAR(500)`（唯一定义，无加宽）；服务 0 处 length 校验；`GlobalExceptionHandler:151` 不拦 DataIntegrityViolationException → 501–2000 字合法留言得 500 | CX-003 |
| OpenAPI L20440 `x-idempotency-required: true`、L20463「重复提交必须返回同一处理结果」；Controller:50-52 自写 IDEMPOTENCY_KEY_REQUIRED | 本仓 31 个服务经 `idempotencyService.execute(...)`、32 个控制器用 `ApiRequests.requireIdempotencyKey`（VALIDATION_FAILED）；本模块 0 处 IdempotencyService → 同键重试再次写库、再落一条 `merchant_portal.settlement.disputed`，期间状态被平台推进则得 409 而非原结果 | CX-004 |
| dto:6 `operatorNote`；OpenAPI L20465-20468「operator_note 操作留痕备注」 | Service 0 处 operatorNote；审计 metadata 只 merchant_id/reason（:134）；本仓 18 个服务落库或入痕 → 契约「留痕」不可复现 | CX-005 |
| Controller:31-43 两条 GET 权限 contract.view / merchant.portal.settlement.view；Service:53/:79/:100 requirePermission | complex_test（flyway 427）实态：三码仅授 role_platform_admin（contract.view 另 role_auditor_internal），出厂无商户角色（与全部 merchant.* 码一致，商户角色由平台运行时授出）；前端 ADM-MER-005/006 accountForms merchant 按同码门控 | 通过（登记 OBS-3） |
| Controller:51 / Service:106 英文 "… is required" | `GlobalExceptionHandler:61 FieldLabels.translate` 统一中文化（idempotency_key→整句、reason→原因、settlement_id→结算单号） | 通过（PGM-001 零英文） |
| View:9-10「最小披露」 | 11 字段无审批链/收益快照/报表口径 | 通过 |

## 5. 22 项判据逐族核对

| 判据 | 结论 | 依据 |
|---|---|---|
| BSM-001 整体说明 | 通过（登记） | 三端点属「最后管清楚结果」的对账/结算层；不发明策略参数 |
| BSM-003 通用能力 | 通过 | 权限分岗（三码）、留言可查（OBS-6 前端无入口属 M023） |
| BSM-114 各类入口 | 通过 | 实现按商户 scope fail-closed，与 L1850「不看平台全局」一致；注释/契约/测试的相反声称归 GEN-016/003（CX-001） |
| PGM-001 全局原则 | 不通过 | CX-002（裁决痕迹被清空、平台理由无处回查，违「正反可查」）、CX-004（重复提交只认一次）；零英文经 handler 通过；金额无 |
| GEN-001 必要性最小性 | 不通过（并入 CX-001） | 死分支 :56-58/:82-84 与 platform-all 占位；无参重载仅测试调用（OBS-6） |
| GEN-002 可执行正确性 | 不通过 | CX-003（契约内合法输入得 500） |
| GEN-003 测试有效性 | 不通过 | CX-001（`platformAdminSeesAllContracts` 假绿）；无 scope 拒绝、长度、幂等、留痕用例 |
| GEN-004 信任边界校验 | 不通过 | CX-003 |
| GEN-005 授权与最小权限 | 通过（登记） | 三端点均 requirePermission + 商户 scope 闸；OBS-3 字典实态 |
| GEN-006 数据完整性 | 不通过 | CX-002 |
| GEN-007 并发与幂等 | 不通过 | CX-004 |
| GEN-008 接口与模式一致性 | 不通过 | CX-001（契约/注释 vs 实现）、CX-004（错误码与共享助手不一致）、CX-005（契约字段无实现）；OpenAPI 侧 OBS-1 |
| GEN-009 错误处理与恢复 | 不通过 | CX-003（500 而非 400） |
| GEN-010 安全与敏感信息 | 通过 | 最小披露视图；审计 metadata 无敏感字段 |
| GEN-011 可观测与审计证据 | 不通过 | CX-005（留痕备注丢弃）；失败事件 OBS-4 登记 |
| GEN-012 资源与性能边界 | 通过（登记） | OBS-6：N+1 与 size 无上限均受商户自数据有界约束 |
| GEN-013 / GEN-014 | 无本面 | 无构建/迁移文件 |
| GEN-015 | 无本面 | 后端无界面 |
| GEN-016 文档可验证性 | 不通过 | CX-001（类注释）、CX-005（契约留痕声称） |
| GEN-017 业务来源分类 | 通过 | 五条均细化（HTML L1322/L1850/L256 的下位实现），无添加项 |
| GEN-018 第一性原理证据 | 通过 | §2 命令原文 + 源码/迁移/契约原文 + complex_test 实态 + HTML 逐字 |

## 6. 本次 Findings（临时 ID CX-M020-00N；正式 ID 由 B2 分配）

### CX-M020-001 · 细化 · 「平台管理员全量监督」分支不可达：类注释、契约、测试与 fail-closed 真实现相悖

- 位置：`MerchantPortalService.java:23-24`（类注释）、`:54-58`、`:80-84`（`scope.map(...).orElseGet(平台全量)`）、`:66`、`:89`（`scope.orElse("platform-all")`）；`MerchantPortalServiceTests.java:69-79`（M022，`platformAdminSeesAllContracts` 桩 `Optional.empty()`）。
- 事实：§4 第 1 行；`currentMerchantScope()` 从不返回 empty；平台管理员持 V200 三码却被 scope 闸 403；全量分支为死代码；测试桩伪造真实现不可能的返回而假绿。
- 依据类型：2（AuthorizationService 原文）+ 4（探针 2/7）。HTML 上位：业务地图 L1850「不看平台全局」、治理地图 L750、红线④（M009 读路径 fail-closed 范式）。判据：GEN-016 / GEN-003 / GEN-008 / GEN-001。四分类：细化。工程缺陷：是。入队：是。
- 最小拟处置：类注释改为「商户账号只见本商户；无商户归属的账号（含平台管理员）fail-closed 403 MERCHANT_SCOPE_REQUIRED，平台监督走各域平台端点」；删 `:56-58`/`:82-84` 的 orElseGet 平台全量分支与 `:66`/`:89` 的 platform-all 占位，商户号以 `orElseThrow(...)`（语义等同真实现）取得；测试 `platformAdminSeesAllContracts` 改为 `callerWithoutMerchantIsRejected`（桩按真实现抛 MERCHANT_SCOPE_REQUIRED，断言不触库、不落审计）；不改 AuthorizationService；OpenAPI L20386/L20409 与 V200 L10 归各自模块（OBS-1/2）。

### CX-M020-002 · 细化 · 商户留言以 `reason, null, null` 覆写平台侧争议记录，清空裁决人/裁决时间

- 位置：`MerchantPortalService.java:123-126`；`:31-32` DISPUTABLE 含 pending_settlement。
- 事实：§4 第 2 行；ContractSettlementRecord 字段序 (…, disputeReason, resolvedBy, resolvedAt, …)；saveSettlement upsert 覆写三列；首次正常流「平台开账（有争议）→ 主管裁决 → 商户留言」即丢失裁决痕迹与平台理由，且 settle_opened 审计无理由可回查。
- 依据类型：2（记录/仓储/ContractService 原文）+ 4（探针 3）。HTML：PGM-001 L256 段「正反可查…不允许无法溯源」；业务地图 L1322「有疑问可留言提异议」。判据：GEN-006 / PGM-001 / GEN-011。四分类：细化。工程缺陷：是。入队：是。
- 最小拟处置：保留 `settlement.resolvedBy()`/`resolvedAt()`；留言不覆盖既有理由——已有 disputeReason 时按「既有理由 + 换行 + 商户留言」合并写入（与 003 同一处 500 上限校验）；测试补「裁决后留言不清空裁决人、不丢平台理由」用例。B2 备选：已有平台争议记录时拒绝 SETTLEMENT_NOT_DISPUTABLE（改变可用性，次选）。

### CX-M020-003 · 细化 · 留言长度无界：契约 2000 字 > 列宽 500 → 500 内部错误

- 位置：`MerchantPortalService.java:104-107`（只判空）、`:123-126`（写库）；`V162__contract_lifecycle.sql:121`；OpenAPI L20472（别模块）。
- 事实：§4 第 3 行；服务 0 处长度校验；handler 不拦 DataIntegrityViolationException。
- 依据类型：2（迁移/handler 原文）+ 4（探针 4）。判据：GEN-004 / GEN-009 / GEN-002 / GEN-008。四分类：细化。工程缺陷：是。入队：是。
- 最小拟处置：服务内（合并后）reason 长度 >500 → 400 VALIDATION_FAILED「异议留言不能超过 500 字」；测试补超长用例；OpenAPI maxLength 2000→500 归 OpenAPI 模块（OBS-1）。

### CX-M020-004 · 细化 · 幂等键只校验存在、不经 IdempotencyService，重复提交不返回同一结果

- 位置：`MerchantPortalController.java:50-52`（自写 IDEMPOTENCY_KEY_REQUIRED）、`MerchantPortalService.java:99-136`（无 idempotencyService）。
- 事实：§4 第 4 行；本仓规范 `idempotencyService.execute(operationId, key, request, action, type)`（31 服务）与 `ApiRequests.requireIdempotencyKey`（32 控制器）；本端点同键重试再写库、再落审计，状态变化后重试得 409。
- 依据类型：2（IdempotencyService/ApiRequests 原文、OpenAPI 原文）+ 4（探针 5）。HTML：PGM-001 L256 段「同一个操作被重复提交，系统只认一次」。判据：GEN-007 / PGM-001 / GEN-008。四分类：细化。工程缺陷：是。入队：是。
- 最小拟处置：服务注入 IdempotencyService，`submitSettlementDispute` 主体包在 `idempotencyService.execute("merchantPortalSettlementDispute:" + settlementId, key, request, () -> …, ContractSettlementRecord.class)`；控制器改用 `ApiRequests.requireIdempotencyKey`；测试补同键重放用例（桩 IdempotencyService 直通并验证调用）。

### CX-M020-005 · 细化 · `operator_note` 契约/DTO 接收但服务丢弃，「操作留痕备注」不留痕

- 位置：`dto/MerchantSettlementDisputeRequest.java:6`；`MerchantPortalService.java:127-135`（审计 metadata 只 merchant_id/reason）；OpenAPI L20465-20468（别模块）。
- 事实：§4 第 5 行；服务 0 处 operatorNote；本仓 18 个服务落库或入痕。
- 依据类型：2（DTO/OpenAPI/服务原文）+ 4（探针 6）。判据：GEN-016 / GEN-011 / GEN-008。四分类：细化。工程缺陷：是。入队：是。
- 最小拟处置：审计 metadata 增加 `operator_note`（非空时，trimToNull）；测试补断言。B2 备选：删 DTO 字段并由 OpenAPI 模块删契约字段（删入口不算闭合，次选）。

## 7. 观察（登记、不入队）

- OBS-1（XM → OpenAPI 所属模块）：L20426-20475 dispute 操作仅列 200 响应（同域 resolve-dispute 列 200/400/403/404/409/500）；L20432 description「挂起为「有争议」/ 按既有裁决流闭环」与现行 note-only（eaccb9c5）不符；L20472 reason maxLength 2000 > 列宽 500；L20386/L20409「平台管理员为全量监督口径」与 fail-closed 不符。
- OBS-2（XM → M021）：V200 L10 注释「平台管理员默认可监督商户自助层」与 fail-closed 不符；迁移不可改，只登记。
- OBS-3：complex_test 实态三码仅授 role_platform_admin / role_auditor_internal，出厂无商户角色（全部 merchant.* 码同此）；商户角色由平台运行时授出，前端 ADM-MER-005/006 按同码门控——不立项。
- OBS-4：三端点无失败审计事件而 OpenAPI 列 x-audit-event-failure（732 处）；合同域 ContractService 同样 0 处 recordFailure，47 个服务有；本仓规范不统一，登记（GEN-011）。
- OBS-5（XM → M023）：apps/web 商户专区两页只 GET，无异议留言入口；HTML L1322「有疑问可留言提异议」。
- OBS-6：listMyContracts 每合同一次 listContractSpaceIds（N+1，商户量级小）；size 无上限（PageResponse.of 内存切片、商户自数据有界）；无参重载仅测试调用；Controller:50 `request == null` 在 @RequestBody required 下不可达；英文文案经 FieldLabels.translate 中文化。
- OBS-7（M022）：`MerchantPortalJdbcIntegrationTests` 类注释「商户专区平台管理员全量真分页」随 CX-001 过时（仓储行为本身有效）；B3 若改 `MerchantPortalServiceTests` 属跨单元测试变更，按 M015→M017 先例登记。
- OBS-8：eaccb9c5（PR #443）已把「挂起为有争议」改为 note-only，与 HTML L1322「付款义务不暂停」一致；未同步 OpenAPI（OBS-1）。无添加项；无 HTML 修改申请；无 questions。

## 8. 结论

本席 B1 票：**FAIL**（CX-M020-001…005 为工程缺陷且建议入队；写入集合为本模块 4 文件 + M022 测试类）。4 文件全文三线审完、22 判据逐族核对、只读探针（含 complex_test 实态）为证据；未修文、未改 HTML/锁/台账。本报告落盘前未读取 `M020-b1-grok-raw.md` 任何正文。


## M020 B2-r1 整批候选复核（席甲票）

- 时间：2026-09-20T10:59:13Z。输入：modules/M020-b1-grok-raw.md V0.01（34806 bytes，SHA256 b170414d3505366c326cd6a7381e19819f8ed4fe855cef0dfa1172b7bbd725b0）、modules/M020-b1-self-raw.md V0.01（上方本报告）；候选 findings.md 顶部「当前 M020 B2-r1 整批候选」段（全文件 SHA256 d28270b261b71347d76b587e5c11b2f43a344b07e50e95a1472f8022318034bc）。本票落盘前未读 Grok B2-r1 追加段。不写独占锁行。
- 差集：Grok GR-01…07 与席甲 CX-001…005 全部归并（GR-02+CX-001→001；CX-002→002；GR-04+CX-003→003；GR-03+CX-004→004；GR-05→005；GR-06→006；GR-01 与 GR-02/GR-04 的 OpenAPI 部分→007 XM；GR-07/CX-005 只登记）；无漏并、无改写。席甲 B1 漏判的 GR-05（失败审计，本席原记 OBS-4）与 GR-06（英文状态出网，`FieldLabels.translate:536` 含中文即透传）经核源成立，如实入队；席甲 CX-005 按同域范式（ContractService 结算写路径不消费备注）与 Grok 判定撤回入队、改登记。
- 002 补核：`ContractSettlementRecord` 字段序、`saveSettlement` upsert 覆写 dispute_reason/resolved_by/resolved_at、`resolveDispute` 写 resolvedBy/resolvedAt 后回 pending_settlement、DISPUTABLE 含 pending_settlement——首次正常流即清空裁决痕迹，成立。
- 分类：七条均细化（业务地图 L1322/L1850、治理地图 L256 段下位实现）；007 按 M015-011 先例登记 XM（M392/M021），本模块不改 yaml/迁移。
- 最小处置：只改本模块 4 文件 + M022 测试类；不改 AuthorizationService/迁移/OpenAPI/HTML；不加 isPlatformAdmin 旁路、不改回 disputed；004 取 31 服务直接 `idempotencyService.execute` 范式以保留现有成功审计 metadata；005 try/catch recordFailure。无业务发明。
- 无添加项、无 HTML 申请、无 questions；无须升为添加的项。
- 席甲 B2-r1 票：**PASS**（整批候选同意）。
