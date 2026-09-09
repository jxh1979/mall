CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M012 B1 修复前全模块审计 · 席甲完整报告
版本：V0.01

## 0. 身份、范围与方法

- 席甲：Claude Fable 5.1（操作员 2026-09-09 指令：席甲全部由 Fable 接替 Codex；Grok 席乙不变）。作者自审票。2026-09-09T16:54:02Z
- 控制合同 `D:\AI\ccwork\160-audit-prompt.md` V0.17；计划 `plan/modules.md` V0.04；`plan/runbook.md` V0.02；锁 `plan/criteria-lock.md` V0.11（实算见首行；LOCK-AMEND-COUNT 1）。
- 正本 `/home/james/complex-management` main，HEAD = origin/main = `bd445728a6ec7bb36a9e7ff19fe56927410ac61e`；工作区 tree `88f5cdae10c969238ef5b519dfae8817d7c0bcfe`（含已闭环模块与本模块 B3 修复，未提交）。三份业务 HTML 只读。
- 模块 M012「综合体/集团/项目公司/业态主体拓扑与四类主体绑定／网页与验证／第1段」，6 文件（计划冻结 1485 行；当前含 M009-003 伴生改动），映射 21 判据（BSM-001、BSM-003、GEN-001–018、PGM-001）。步骤：B1（修复前全模块三线审计）。
- 顺序：Grok 真实退出后本席才写本报告；本报告落盘前未读取 `M012-b1-grok-raw.md` 任何正文（该文件此刻存在=True，未读）。
- 检查点：M001–M010 已于 2026-09-09 经 PR #428 合并（HEAD=origin/main=bd445728）；M011 已闭环（修复在工作区未提交）。本模块 6 文件相对 HEAD 无未提交改动。
- 只读依赖追流（不审）：`apps/web/src/customer/i18n/statusLabels.ts`（`formatBusinessStatus` 回退原码）、`apps/web/src/admin/components/*`（GuidedFlowModal/RowDetailDrawer/HighRiskConfirm/TraceLink）、M009 后台 `PlatformController`/`PlatformService`（商户创建请求字段与白名单）、complex_test 实态。
- 只读承诺：未修改任何源码/HTML/锁/台账；数据库探针全部 SELECT。

## 1. 文件覆盖表（行数/字节/SHA256 为本席独立实算）

| 文件 | 行 | bytes | SHA256 | 覆盖 |
|---|---:|---:|---|---|
| apps/web/src/admin/pages/GovernanceTopologyPage.tsx | 231 | 14547 | 31e530e213b51d4f0366fa1121c896f1d2c35ddd5a33c4ce476990e5aba7ae3c | 全读 |
| apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx | 188 | 9604 | 7426c19a4600d678684c06926ad3932b2e491b7b713144cc8831490dc98dc73d | 全读 |
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 480 | 29371 | 76571cb692a4bc75ccd26f46be02e8c07c4c72806c9339feb55f91fb7144abe4 | 全读 |
| apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx | 261 | 14985 | 99a3b8201d9d7a401a4013a27a5e76a325919a759c93b1ea34b0b04ac3e77f8e | 全读 |
| apps/web/src/admin/pages/SubjectBindingsPage.tsx | 220 | 12689 | 7c83e10ef83ead253e1c2daf3eb5b4e7c311f2d38478bd247e6162820ec0a2df | 全读 |
| apps/web/src/admin/pages/SubjectBindingsPage.test.tsx | 121 | 5916 | c6e9d77a20b3ff430938e6e89df545211bab16f8d8d0b8efb56443a7a0e4e428 | 全读 |

## 2. 可执行证据原文（/tmp/m012_b1_run.log）

```
START=2026-09-09T16:53:31Z
HEAD=bd445728a6ec7bb36a9e7ff19fe56927410ac61e
TREE=88f5cdae10c969238ef5b519dfae8817d7c0bcfe
=== vitest (3 files) ===
VITEST_EXIT=0
 [32m✓[39m src/admin/pages/SubjectBindingsPage.test.tsx [2m([22m[2m7 tests[22m[2m)[22m[32m 96[2mms[22m[39m
 [32m✓[39m src/admin/pages/GovernanceTopologyPage.test.tsx [2m([22m[2m9 tests[22m[2m)[22m[32m 119[2mms[22m[39m
 [32m✓[39m src/admin/pages/OrganizationMerchantPage.test.tsx [2m([22m[2m12 tests[22m[2m)[22m[33m 539[2mms[22m[39m
[2m Test Files [22m [1m[32m3 passed[39m[22m[90m (3)[39m
[2m      Tests [22m [1m[32m28 passed[39m[22m[90m (28)[39m
=== grep: raw venueType rendering (display() instead of formatBusinessStatus) ===
apps/web/src/admin/pages/OrganizationMerchantPage.tsx:292:        { label: "业态", value: display(row.venueType) },
apps/web/src/admin/pages/OrganizationMerchantPage.tsx:363:          <tbody>{projects.map((row) => <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`organization-project-row-${row.id}`} onClick={() => openProjectDetail(row)} onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openProjectDetail(row); } }}><td>{display(row.name)}</td><td>{display(row.code)}</td><td onClick={(event) => event.stopPropagation()}>{row.merchantId ? <TraceLink navigateTo={navigateTo} pageId="ADM-GOV-002" objectId={row.merchantId}>{display(row.merchantId)}</TraceLink> : display(row.merchantId)}</td><td>{display(row.venueType)}</td><td>{formatBusinessStatus(row.status)}</td><td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看项目档案详情" onClick={(event) => { event.stopPropagation(); openProjectDetail(row); }}>查看详情</button></td></tr>)}</tbody>
apps/web/src/admin/pages/GovernanceTopologyPage.tsx:125:        { label: "业态", value: display(project?.venueType) },
apps/web/src/admin/pages/GovernanceTopologyPage.tsx:168:                <td>{display(project?.venueType)}</td>
=== grep: test asserts English venue type / retired enum codes ===
apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx:12:  { id: "org-1", entity_name: "平台经营主体", entity_role: "operation", unified_social_credit_code: "91330100MA27XW8H", legal_representative: "张三", bank_account_ref: "vault://demo", status: "active", created_at: "2026-07-01T00:00:00Z" },
apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx:15:  { id: "m-1", merchant_name: "正常商户甲", merchant_type: "catering", business_entity_id: "org-1", contact_name: "联系人甲", contact_phone: "13800000001", mall_block_code: "1F-A", area_square_meter: 128.5, leased_area_square_meter: 120, cooperation_form: "fixed_rent", status: "active" },
apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx:124:    expect(entitiesText).toContain("运营主体");
apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx:132:    expect(m1).toContain("固定租金");
apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx:138:    expect(projectsText).toContain("bar");
apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx:158:    // V348 业态五值：酒吧/餐饮/零售/游乐/其他，全中文、不预选（必填）。
apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx:171:    expect(coopText).not.toContain("固定租金");
apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx:172:    expect(coopText).not.toContain("fixed_rent");
apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx:201:    expect(summary.textContent).not.toContain("fixed_rent");
apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx:227:    expect(drawer.textContent).toContain("运营主体");
apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx:237:    expect(drawer.textContent).toContain("固定租金");
apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx:238:    expect(drawer.textContent).not.toContain("fixed_rent");
apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx:9:const ENTITIES = [{ id: "org-1", entity_name: "平台经营主体", unified_social_credit_code: "ORG-1", entity_role: "operation", status: "active" }];
=== grep: hard-coded request fields in createMerchant / submitChange ===
apps/web/src/admin/pages/OrganizationMerchantPage.tsx:172:      operatorNote: "完整后台组织与商户档案验收创建",
apps/web/src/admin/pages/OrganizationMerchantPage.tsx:173:      evidenceAttachmentIds: ["att-merchant-profile-create"],
apps/web/src/admin/pages/OrganizationMerchantPage.tsx:176:      businessEntityId: entities[0]?.id ?? "org-1",
apps/web/src/admin/pages/OrganizationMerchantPage.tsx:177:      contractEntityId: "contract-subject-1",
apps/web/src/admin/pages/OrganizationMerchantPage.tsx:178:      settlementEntityId: "settlement-subject-1",
apps/web/src/admin/pages/OrganizationMerchantPage.tsx:181:      mallBlockCode: "1F-A",
apps/web/src/admin/pages/OrganizationMerchantPage.tsx:182:      areaSquareMeter: 128.5,
apps/web/src/admin/pages/OrganizationMerchantPage.tsx:184:      secondaryContactName: "备用联系人",
apps/web/src/admin/pages/OrganizationMerchantPage.tsx:185:      secondaryContactPhone: "13900000000",
apps/web/src/admin/pages/OrganizationMerchantPage.tsx:186:      rentPerSquareMeter: 12.3,
apps/web/src/admin/pages/SubjectBindingsPage.tsx:48:  const [reason, setReason] = React.useState("完整后台验收变更");
apps/web/src/admin/pages/SubjectBindingsPage.tsx:135:      evidenceAttachmentIds: ["evidence-full-admin"],
=== grep: statusLabels keys ===
125:  other: "其他",
180:  operation: "运营主体",
181:  project_company: "项目公司",
182:  management_company: "业态主体（管理公司）",
183:  spv: "项目专营主体",
185:  fixed_rent_plus_commission: "固定租金+抽成",
187:  fixed_rent: "固定租金",
191:  retail: "零售",
192:  catering: "餐饮",
193:  media: "传媒",
197:  bar: "酒吧",
198:  amusement: "游乐",
202:  platform: "平台",
259:  return STATUS_LABELS[code] ?? code;
=== grep: SubjectBindingsPage.test coverage of submitChange ===
2
=== SubjectBindingsPage change request body has no action field? ===
    setFormError("");
    setGlobalError(null);
    setSubmitting(true);
    const body = {
      reason,
      idempotencyKey: `full-admin-subject-binding-change-${selected.id}-${Date.now()}`,
      evidenceAttachmentIds: ["evidence-full-admin"],
    };
2
=== backend: StatusChangeRequest + changeBusinessEntityBinding action validation ===
package com.complex.platform.governance.dto;
public record StatusChangeRequest(
        String idempotencyKey,
        String action,
        String reason,
        List<String> evidenceAttachmentIds
) {
}
602:                    String status = requireText(actual.action(), "action");
604:                        throw ApiException.badRequest("VALIDATION_FAILED", "操作必须为启用（active）、停用（inactive）或暂停（suspended）");
=== backend: requireBusinessEntity existence check used by createMerchant ===
560:                    String contractEntityId = requireBusinessEntity(actual.contractEntityId(), "contract_entity_id");
561:                    String settlementEntityId = requireBusinessEntity(actual.settlementEntityId(), "settlement_entity_id");
711:            throw ApiException.notFound("BUSINESS_ENTITY_NOT_FOUND", "主体不存在：" + id);
=== backend: what merchant create actually requires (contract/settlement entity ids) ===
560:                    String contractEntityId = requireBusinessEntity(actual.contractEntityId(), "contract_entity_id");
561:                    String settlementEntityId = requireBusinessEntity(actual.settlementEntityId(), "settlement_entity_id");
573:                            contractEntityId,
574:                            settlementEntityId,
623:                            existing.contractEntityId(),
624:                            existing.settlementEntityId(),
719:                merchant.withEntities(binding.operationEntityId(), binding.contractEntityId(),
720:                        binding.settlementEntityId(), Instant.now())));
764:                            existing == null ? trimToNull(request.contractEntityId()) : existing.contractEntityId(),
765:                            existing == null ? trimToNull(request.settlementEntityId()) : existing.settlementEntityId(),
=== DB probes (complex_test, SELECT only) ===
-- flyway top
410|m010 business entity topology and role isolation|t
-- business_entities.entity_role distribution
management_company|4
platform|1
project_company|1
-- merchants cooperation-form column(s)
cooperation_form
-- merchants.cooperation_form distribution
<NULL>|1
-- projects venue column(s)
venue_type
-- projects.venue_type distribution
-- chk constraints on merchants / projects
chk_merchant_delete_status|CHECK (((delete_status)::text = ANY ((ARRAY['none'::character varying, 'pending'::character varying, 'approved'::character varying])::text[])))
chk_merchants_business_format|CHECK (((merchant_type)::text = ANY ((ARRAY['bar'::character varying, 'catering'::character varying, 'retail'::character varying, 'amusement'::character varying, 'media'::character varying, 'other'::character varying])::text[])))
chk_merchants_contact_phone_pii|CHECK (((contact_phone IS NULL) OR ((contact_phone)::text = ''::text) OR ((contact_phone)::text ~~ 'enc:v1:%'::text))) NOT VALID
chk_merchants_secondary_contact_phone_pii|CHECK (((secondary_contact_phone IS NULL) OR ((secondary_contact_phone)::text = ''::text) OR ((secondary_contact_phone)::text ~~ 'enc:v1:%'::text))) NOT VALID
END=2026-09-09T16:53:33Z
```

## 3. 逐判据结论（21 项：BSM-001、BSM-003、GEN-001…018、PGM-001）

| 判据 | 结论 | 依据（文件:行 / 命令输出） |
|---|---|---|
| GEN-001 必要性与最小性 | 不符 | `OrganizationMerchantPage.tsx` createMerchant 向生产接口提交 10 个用户从未填写的固定值字段（见 CX-M012-01）；`SubjectBindingsPage.tsx` submitChange 请求体只含原因/幂等键/固定证据号，不含任何要变更的主体字段（CX-M012-02） |
| GEN-002 可执行正确性 | 不符 | CX-M012-01：创建商户请求写死 `contractEntityId:"contract-subject-1"`/`settlementEntityId:"settlement-subject-1"`，而后台 `PlatformService.requireBusinessEntity`（M009-006）校验主体存在，complex_test 无此 id → 创建必然 404 BUSINESS_ENTITY_NOT_FOUND；CX-M012-02：绑定变更请求体无 `action`，后台 `changeBusinessEntityBinding` 首先 `requireText(action)`（须为 active/inactive/suspended）→ 变更必然 400 VALIDATION_FAILED。两页唯一的写动作在现行后台都不能成功 |
| GEN-003 测试有效性 | 不符 | `OrganizationMerchantPage.test.tsx` 断言英文业态 `toContain("bar")` 与已退役码 `fixed_rent`→「固定租金」、`entity_role:"operation"`→「运营主体」；`GovernanceTopologyPage.test.tsx` 夹具 `entity_role:"operation"`；`SubjectBindingsPage.test.tsx` 对 submitChange/validateChange 零覆盖（CX-M012-03/04） |
| GEN-004 信任边界校验 | 不符 | createMerchant 仅校验商户名/业态/手机号，其余提交字段（合同主体、结算主体、区块、面积、租金、备用联系人）由代码写死而非用户输入（CX-M012-01） |
| GEN-005 授权与最小权限 | 符合 | 写操作均经 `PermissionButton`/`HighRiskConfirm`/`RowActions` 按权限码门控（merchant.profile.manage/approve、entity.binding.manage）；读侧 `entity.binding.view` 缺失时不请求主体端点（两页 useAdminResource 注释与实现一致）；服务端授权属 M009/M011 范围 |
| GEN-006 数据完整性 | 符合（前端范围） | 页面不做本地状态转换；写入均走后端接口 |
| GEN-007 并发与幂等 | 符合 | 所有 POST 均带 idempotencyKey（`merchant-*-${id}-${Date.now()}`、`full-admin-subject-binding-change-*`） |
| GEN-008 接口与模式一致性 | 不符 | 测试夹具/断言使用 V410 已删除的 `entity_role="operation"` 与 M009-003 已退役的 `cooperation_form="fixed_rent"`（CX-M012-04）；`statusLabels.ts` 仍保留 operation/spv/fixed_rent* 标签 |
| GEN-009 错误处理与恢复 | 符合 | createMerchant/submitChange catch 后显错不静默；archive/delete 路径的 api.post 未 try/catch 但 AdminApi 抛错由 useAdminResource/全局错误兜底（本席未构成缺陷） |
| GEN-010 安全与敏感信息 | 符合 | 无注入/明文秘密；id 经 encodeURIComponent（SubjectBindings）；archive/delete 路径 id 来自后端行数据 |
| GEN-011 可观测与审计证据 | 不符 | 两页向后端提交固定假证据附件号 `att-merchant-profile-create` / `evidence-full-admin` 与固定操作说明「完整后台…验收…」，审计留痕不可验证（CX-M012-01/02；HTML「所有变更必须带原因、幂等键和证据附件」） |
| GEN-012 资源与性能边界 | 符合 | 列表为内存数组渲染；无无界循环/句柄 |
| GEN-013 构建配置与依赖 | 符合 | `npm run --workspace apps/web test:unit -- <3 文件>` 可复现（§2 原文） |
| GEN-014 数据库迁移正确性 | 不适用 | 本模块无迁移文件 |
| GEN-015 界面可用性与可访问性 | 不符 | 项目「业态」列/抽屉以 `display(row.venueType)` 直出英文枚举（两页四处），违反 PGM-001 零英文（CX-M012-05）；其余：行 tabIndex/Enter/Space、aria-label、Esc 关闭抽屉 符合 |
| GEN-016 文档可验证性 | 不符（次要） | `OrganizationMerchantPage.test.tsx` 注释「V348 业态五值：酒吧/餐饮/零售/游乐/其他」与代码 `MERCHANT_TYPES` 六值（含 media）不一致（CX-M012-06） |
| GEN-017 业务来源分类 | 符合 | 本席各项均按冲突→细化→技术性→添加分类（见 §4），无因 HTML 沉默直接删除 |
| GEN-018 第一性原理证据 | 符合 | 每条结论落到文件:行、vitest 输出或 psql 输出 |
| BSM-001 整体说明 | 不符 | 「数据可溯源、全链可点击」：项目表「商户」列以 TraceLink 可点（符合）；但 createMerchant 写死 `contractEntityId/settlementEntityId` 占位值，使商户→合同主体/结算主体的溯源链指向不存在的对象（CX-M012-01） |
| BSM-003 系统通用能力 | 不符 | 「操作有引导」：新建商户为引导式弹窗（符合）；「数据可改可删」：存档/申请删除/审批删除经权限门控（符合）；但绑定变更页「所有变更必须带原因、幂等键和证据附件」——证据附件为代码写死假值（CX-M012-02） |
| PGM-001 整体定位 | 不符 | 「界面全中文（零英文）」：`display(row.venueType)` 直出 `bar` 等英文枚举，测试甚至断言 `toContain("bar")`（CX-M012-05）；「见名可点」：商户名/项目商户 TraceLink 可点（符合） |

## 4. 候选 Finding（席甲候选，待 B2 与 Grok 合并定稿）

| 候选号 | 文件:行 | 现象 | 判据 | 分类 | 严重度 |
|---|---|---|---|---|---|
| CX-M012-01 | `OrganizationMerchantPage.tsx:168-190` createMerchant（`contractEntityId: "contract-subject-1"`、`settlementEntityId: "settlement-subject-1"`、`mallBlockCode: "1F-A"`、`areaSquareMeter: 128.5`、`leasedAreaSquareMeter: 120`、`secondaryContactName/Phone`、`rentPerSquareMeter: 12.3`、`evidenceAttachmentIds: ["att-merchant-profile-create"]`、`operatorNote: "完整后台组织与商户档案验收创建"`、`businessEntityId: entities[0]?.id ?? "org-1"`） | 生产页面「创建商户档案」把验收脚手架占位值写入真实商户档案；且后台 `requireBusinessEntity`（M009-006）校验 contract/settlement 主体存在，complex_test 六主体 id 为 be_* → 该请求必然 404 BUSINESS_ENTITY_NOT_FOUND，引导式创建流在现行后台不可用；`OrganizationMerchantPage.test.tsx` 只断言 POST 发出与 body 含商户名，mock 永远 200 故未暴露 | GEN-001/002/004/008/011、BSM-001 | 技术性（页面-后台契约不一致；HTML 未规定表单字段集，但规定可溯源与证据附件真实） | 重要 |
| CX-M012-02 | `SubjectBindingsPage.tsx:129-135` submitChange 请求体 `{reason, idempotencyKey, evidenceAttachmentIds:["evidence-full-admin"]}`——无 `action` 字段、页面亦无启用/停用/暂停选择控件；默认原因「完整后台验收变更」 | 后台 `StatusChangeRequest(idempotencyKey, action, reason, evidenceAttachmentIds)` 且 `changeBusinessEntityBinding` 先 `requireText(action)` 并限定 active/inactive/suspended（`PlatformService:588+`）→ 页面的「提交绑定变更」在现行后台必然 400 VALIDATION_FAILED，功能不可用；证据附件号为代码写死假值；`SubjectBindingsPage.test.tsx` 对该路径零覆盖故未暴露 | GEN-002/008/001/011、BSM-003 | 技术性（页面-后台契约不一致） | 重要 |
| CX-M012-03 | `SubjectBindingsPage.test.tsx`（§2 grep 计数 2 均为 KPI 聚焦视图对提交按钮存在/隐藏的断言，无点击提交或 `/change` 请求用例；§2 对页面源码 `action` 的 2 处命中均为 `setActionResult({ action: … })` 结果标签，请求体确无 action）；`GovernanceTopologyPage.test.tsx`（无 focusObjectId 下钻用例） | 关键写路径与下钻分支零覆盖 | GEN-003 | 技术性 | 次要 |
| CX-M012-04 | `OrganizationMerchantPage.test.tsx` 夹具 `entity_role: "operation"`、`cooperation_form: "fixed_rent"` 及断言「运营主体」「固定租金」；`GovernanceTopologyPage.test.tsx` 夹具 `entity_role: "operation"` | 测试用 V410 已删除/M009-003 已退役的枚举码通过，掩盖真实枚举下的渲染结果（complex_test 实态：entity_role 仅 platform/project_company/management_company） | GEN-003/008 | 技术性 | 次要 |
| CX-M012-05 | `OrganizationMerchantPage.tsx` 项目表 `<td>{display(row.venueType)}</td>` 与 openProjectDetail「业态」；`GovernanceTopologyPage.tsx` 拓扑表 `<td>{display(project?.venueType)}</td>` 与 openTopologyDetail「业态」；`OrganizationMerchantPage.test.tsx` `expect(projectsText).toContain("bar")` | 业态列直出英文枚举 bar/catering…，测试反向断言英文 | GEN-015、PGM-001 | 冲突（HTML「不得出现任何英文」） | 重要 |
| CX-M012-06 | `OrganizationMerchantPage.test.tsx` 注释「V348 业态五值」 | 与 `MERCHANT_TYPES` 六值（含 media）不一致 | GEN-016 | 技术性 | 次要 |

## 5. 结论

本席票：**FAIL**（重要 3：CX-M012-01/02/05；次要 3：CX-M012-03/04/06）。本模块无 2.2 添加项，不产生 BLOCKED-ASK。

## M012 B2-r1 整批候选复核（席甲票）+ B1 勘误
版本：V0.02（本文件自身；B1 正文 V0.01 原字节保留，本段追加）

- 输入：`modules/M012-b1-self-raw.md` V0.01 前缀 21559 bytes SHA256 249952156dcb1a85a8f77437f1c3e72aa6b01e104da6619535df49a377a487ea；`modules/M012-b1-grok-raw.md` V0.01 前缀 40713 bytes SHA256 93333eb5c29a14de1ba7c770051014b5659550e58c4024aa4e2e1641d649aacf（Grok B1：FAIL，GR-M012-001…008，OBS-01…18）；候选 `findings.md` V1.39 525715 bytes SHA256 a0abab33e9b8790c48f9393fbba434334adadfbb57953b69aae71ffc36ec3934「当前 M012 B2-r1 整批候选」M012-001…009 及 2.2 答复段；`questions/M012-006.md`（V0.02）/`M012-007.md`（V0.02）首行答复均「删除」。
- 本段不写独占锁行（B2 规约）；锁实算仍为 b012328f…6695a，与 B1 后四值一致。
- 席位：Claude Fable 5.1（席甲，作者自审票）。本段在 Grok B2-r1 真实退出后、读取其追加段之前落盘。

### 勘误（席甲 B1 §3 GEN-002 行、§4 CX-M012-01；同句已进入 findings V1.38/V1.39 的 006 事实栏与 questions/M012-006.md「添加后的代价」）

- 错句：「后台 `requireBusinessEntity`（M009-006）校验 contract/settlement 主体存在，complex_test 无 contract-subject-1 → 创建必然 404 BUSINESS_ENTITY_NOT_FOUND」。
- 事实（类型 4，本席复核 `PlatformService.java`）：`:560-561` 的 `requireBusinessEntity(contract_entity_id/settlement_entity_id)` 位于 `createBusinessEntityBinding`（绑定创建路径，M009-006）；商户创建走 `saveMerchant(:735+)`，对 businessEntityId/contractEntityId/settlementEntityId 仅 `trimToNull`（`:764-765`），**不校验存在、可为空**；`requireSubjectStatus` 缺省 active（`:666`）；`requireCooperationForm` 允许空（`:655`）；`evidenceAttachmentIds`/`operatorNote` 服务层未引用。因此占位主体 id 会**静默落库为悬空引用**（merchants 三主体列无 FK，Grok B1 GEN-006 观察正确），而不是被 404 拒绝。Grok B1 GR-006 的描述正确，本席 B1 的「必然 404」为误读（把 :560 归到了 createMerchant）。
- 影响：006 的「添加」分类与操作员的「删除」答复不受影响（默认值/假主体/假证据仍是三份 HTML 无据的验收脚手架，且悬空落库比 404 更有害）；但 006 的**最小处置**须改为：创建请求**不再携带**三主体、区块/面积/备用联系人/租金/status/验收备注/假证据（后台对这些均可空，status 缺省 active 为后台既定口径，三主体由四类主体绑定流程承接——与页内边界文案「不在本页替代四类主体绑定」一致），**不需要**新增主体选择控件；`findings.md` V1.39 2.2 答复段中「三主体由用户从 `/business-entities` 列表选择（三选一必填…）」一句随本勘误在定稿时改写。本席已把该勘误同步到 questions/M012-006.md 追加段（不改首行答复）。

### 差集与逐 ID 票

按 `receiving-code-review` 核实 Grok 独有项后接受：GR-001（`BusinessEntityBindingRecord` 字段 merchantId/operationEntityId/contractEntityId/settlementEntityId/status/effectiveFrom/effectiveTo/changeReason；`types.ts:1148-1158` 页面所读七字段为臆造 optional）、GR-003（`SpaceRecord` 有 projectId 无 venueId；`types.ts:1109-1110` 标「旧臆造字段」）、GR-004 空间类型/表头部分（`statusLabels.ts` 无 shop/building/zone/hall，L259 回退原码；V184 字典 type_name 为中文）、GR-006/007 的「添加」分类。席甲 B1 漏报 001/003/004（空间类型）/008（存档删除无测试）为本席覆盖不足，如实记录。差集：Grok GR-001…008 → 001…008；OBS-01…18 → 009；席甲 CX-01 → 006（技术侧事实并入，404 句作废）、CX-02 → 002、CX-03 → 008、CX-04/06 → 005、CX-05 → 004。无遗漏、无改写 Grok 原意。

| ID | 本席票 | 理由 |
|---|---|---|
| 001 | PASS（技术性／是） | 页面读臆造键与后台记录不一致，真实数据全「-」；页内改读真实键 + 夹具改真实形状最懒；`types.ts` 不在本模块，局部类型即可 |
| 002 | PASS（技术性／是） | 后台/OpenAPI 必填 action，页面无该字段与控件；加三态中文下拉最懒；与 006 同批落地时原因必填、去假证据 |
| 003 | PASS（技术性／是） | `venueId` 不存在，改 `projectId` 一行 |
| 004 | PASS（冲突／是） | PGM-001 零英文原句；项目业态走 `formatBusinessStatus`（bar→酒吧 已在表）；空间类型取页内 8 码中文表（与 V184 type_name 同词、不新增请求）为最懒；表头改「编号」；测试改中文断言 |
| 005 | PASS（细化／是） | 夹具与断言改现行闭集（三角色/六类/字典 8 码/绑定三态），注释六值 |
| 006 | PASS（添加／是，操作员答删除；处置按勘误） | 不预填；创建请求只带用户填写的商户名/业态/联系人/电话/合作方式（可空）与幂等键，不再携带三主体/区块/面积/备用联系人/租金/status/验收备注/假证据；绑定页原因空初始且必填、不带假证据 |
| 007 | PASS（添加／是，操作员答删除） | 改非空校验一行 + 文案 + 测试 |
| 008 | PASS（技术性／是） | 存档/删除申请/审批各一条可失败测试、写路径 catch 显错、绑定提交一条（随 002）、下钻一条；不为每个 verb 再扩 |
| 009 | PASS（否） | 与两席观察一致 |

本席 B2-r1 票：**PASS**（队列是＝001–008；否＝009；006 处置以本段勘误为准）。B3-r1 边界：只改本模块 6 文件（各版本递增；`OrganizationMerchantPage.tsx`/`.test.tsx` 现 V0.01→V0.02、其余四文件加 V0.01）；不改 `types.ts`/`statusLabels.ts`/后台/三份 HTML/锁；vitest 红→绿。不以本席票覆盖外审；外审 FAIL 则按其意见修订为 r2 候选。

## M012 B2-r2 修订候选复核（席甲票）
版本：V0.03（本文件自身；V0.01 B1 正文与 V0.02 B2-r1 追加段原字节保留，本段追加）

- 输入：`modules/M012-b1-grok-raw.md` V0.02 前缀 63960 bytes SHA256 875e3e8bd54f6c74710952f167b7ce4465d7ecebfe6f0603f0765636ed7a5e2e（Grok B1 FAIL + B2-r1 FAIL：唯一否决点 006 处置；004/007/008/009 修正意见）；本文件 V0.02 前缀 27474 bytes SHA256 7033fc88d7048e36ec4a3703afa6253133745d03ed111cc3a1cf4c4be413fcb8；修订候选 `findings.md` V1.40 529212 bytes SHA256 34c8270395110560181415c1b9f38ba7e6adfe3bfc0f42677303f0d9ee909779「B2-r2 修订候选」；`questions/M012-006.md` V0.03（首行「删除」不变，末尾席甲勘误）/`M012-007.md` V0.02。
- 本段不写独占锁行（B2 规约）；锁实算仍为 b012328f…6695a。
- 席位：Claude Fable 5.1（席甲，作者自审票）。本段在 Grok B2-r2 真实退出后、读取其追加段之前落盘。
- 按 `receiving-code-review` 逐条核实 Grok B2-r1 意见后的处理：(1) 006 处置——本席 B2-r1 追加段已先于 Grok 票自行勘误（`requireBusinessEntity` 属 `createBusinessEntityBinding`；`saveMerchant` 三主体 `trimToNull` 可空不校验；status 缺省 active；cooperationForm 可空），r2 候选改为「不传三主体与隐藏字段、不加选择器、不把 entity.binding.view 当创建闸」：接受。(2) 004 空间状态 active/unavailable 页内映射可用/不可用（OBS-18 并入）：接受。(3) 007 删「与后台一致」：接受（后台对电话 `trimToNull`，前端非空是本题已答的替换项）。(4) 008 去下钻用例、存档一条同条拦空理由、绑定 POST 随 002 只一条：接受。(5) 009 措辞（OBS-06→005、OBS-18→004、OBS-13→006）：接受。

| ID | 本席票 | 理由 |
|---|---|---|
| 001 | PASS（技术性／是） | 口径不变；含合同主体 KPI 改读 contractEntityId、选中展示改读 merchantId |
| 002 | PASS（技术性／是） | 口径不变；提交断言只一条（action∈三态），原因/证据归 006 |
| 003 | PASS（技术性／是） | 口径不变 |
| 004 | PASS（冲突／是，已补空间状态） | 页内 8 码表 + 可用/不可用；不改 statusLabels、不拉 /space-types |
| 005 | PASS（细化／是） | 口径不变 |
| 006 | PASS（添加／是，处置已改写） | 不传三主体与隐藏字段；不加选择器；不加权限闸；原因空初始必填；不提交假证据 |
| 007 | PASS（添加／是，措辞已改） | 前端非空，后台不改 |
| 008 | PASS（技术性／是，已收窄） | 存档一条（空理由同条）+ 删除申请一条 + catch；无下钻用例 |
| 009 | PASS（否，措辞已改） | 下钻无测试登记于此 |

本席 B2-r2 票：**PASS**（队列是＝001–008；否＝009）。B3-r1 边界：只改本模块 6 文件（各版本递增）；不改 `types.ts`/`statusLabels.ts`/后台/路由权限码/三份 HTML/锁；vitest 先改测试见红再改页面见绿。不以本席票覆盖外审。
