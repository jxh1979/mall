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


CRITERIA-LOCK-SHA256: b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a

# M012 本次B1全模块三线审计 · Codex完整原始报告
版本：V0.04

本次结论：FAIL。5项临时finding全部列出，尚未归并/修复。生命周期IN_PROGRESS；B3历史累计1，本次未执行。日期2026-09-14，正本160:/home/james/complex-management main HEAD e456ba96c84185c8d39f18f548011daac48a6466。
按用户“Codex从M009再审；无问题自己审，有问题按原提示词完整执行”，M010/M011补充复查PASS后进入M012。发现问题故恢复原完整B1流程；先派Grok，Codex独立全读并封存本票，本票落盘前未读本次Grok原始报告/输出/发现。Grok适配器仅缓冲后丢弃输出，固定grok-4.6/xhigh/no-subagents，直接SSH，无MCP/第三席。
冻结控制V0.17、计划V0.04、判据V0.11；M012六文件计划1485行、当前1755行；21映射判据不扩展。Grok本次指定modules/M012-b1-grok-raw.md，目前调度会话80754待实际退出；命令完整记录于本次ledger后续回执，不能预填exit0。
旧self原30522byte前缀SHA256 78d345911e7fc11f41f98d0c97fbde654d687459454df232527388b3d64a2261逐字保留。旧审计/修复结论不改写，当前新票不继承旧PASS。

## 1. 范围完整覆盖
| 文件 | 当前行 | SHA256 | 完成 |
|---|---:|---|---|
| apps/web/src/admin/pages/GovernanceTopologyPage.tsx | 244 | 403f714dd58b96f33d4da39a6870a6287054d61296fd036b0deada9cca32b342 | 全文 |
| apps/web/src/admin/pages/GovernanceTopologyPage.test.tsx | 220 | 146d449fa98a19beec0f61c6a602c3d1049a31c9344b5eea57e85f080637868d | 全文 |
| apps/web/src/admin/pages/OrganizationMerchantPage.tsx | 492 | 95328007702f02c6c80f9c846eea72f80a94a5813f6b97349e8759964a4c1d95 | 全文 |
| apps/web/src/admin/pages/OrganizationMerchantPage.test.tsx | 331 | a32dc1eb11bef528b532b64888b1752d8a3a63e943b96bdf0e33ea77b816110b | 全文 |
| apps/web/src/admin/pages/SubjectBindingsPage.tsx | 251 | 028c97a810da1fce27768dfe3ca8983dbfca8af2d60d85eb3994aa81e5ea3378 | 全文 |
| apps/web/src/admin/pages/SubjectBindingsPage.test.tsx | 217 | 81d89f5b917d0ca96d9eefd51e830405c0d2c2960431a40b3d55021bf2b2f1bd | 全文 |
六文件完整逐行读取，含每个effect、请求、列表/统计、action handler、表格键盘事件、表单、全部37既有测试及夹具。
依赖只读追流：adminClient的unwrap/get/getPaged/request；useAdminResource；useSelectedRow；HighRiskConfirm；statusLabels页面reexport及customer实表；types MerchantAdmissionRow；当前PlatformController/Service列表/绑定状态处理；M009刚完成的MerchantRecord/项目存储；M011同会话已经实跑的自定义空间类型成功用例。未审扩其他模块。

## 2. 第一性原理 / 顺从式 / 对抗式
第一性原理：需求正本“所有基础对象都要带综合体项目归属”“所有数据默认按项目隔离”；项目与运营公司多对多；商户/运营主体状态“待准入、正常、暂停、终止、归档、黑名单”，暂停不等于黑名单；治理系统业务界面中文。出处三HTML的项目归属/治理/状态条目，沿BSM-003、PGM-001及冻结GEN工程判据审查。代码/迁移只用于验证事实，不据其新增业务。
顺从式：检查6文件与21项，已有权限闸、操作理由/幂等键、无假附件、错误展示、抽屉和筛选都逐行核对；既有37测试11+15+11全部执行通过，但覆盖不足不能直接判PASS。
对抗式：不改实际源码，通过Vitest现有programmatic API和内存transform向两份现有测试追加独立反例，读取同一未改页面。用真实AdminApi+snake_case分页响应证明projectId、分页、状态、键盘、动态字典五问题。所有业务六文件SHA前后相同。无HTTP生产写/数据库改动。反例中的虚拟行号超过源文件末尾是内存追加，并非磁盘测试内容变更。
首轮50/51分页探针验证丢total；读到生产Controller默认size500后修正为500/501再实跑，仍失败。本次报告以500/501为正式触发阈值，不把初始测试信封50说成生产默认。
证据类型：5项均为HTML原文+可执行产物/可复现命令输出；不以惯例或旧审计意见作为依据。

## 3. 本次全部Findings
### CX-01 — 拓扑页按旧反向字段找项目，忽略商户真实project_id
- 位置：apps/web/src/admin/pages/GovernanceTopologyPage.tsx:164–182；GovernanceTopologyPage.test.tsx:13–16、144–155
- 分类：细化；工程缺陷：是；拟修复队列：是（正式ID待B2），不是添加项。
- 冻结判据：BSM-003、GEN-002、GEN-003、GEN-008。
- 证据：当前MerchantRecord由M009补齐projectId；一条merchant.project_id=p-1，projects[p-1].merchant_id=null的正常记录，页面查project.merchantId===merchant.id得到空数组，表格/抽屉项目显示“-”。内存渲染反例真实得到“商户甲--—有效”，不含项目一。现有夹具只造旧反向关系，因此37绿未发现。
- 最小拟修边界：按merchant.projectId匹配project.id；用当前字段补夹具/反例。旧project.merchantId若仍展示只能按其真实从属用途，不能当商户所属综合体。无HTML修改。

### CX-02 — 三页丢弃分页元数据，超过默认500条后总数失真且后续记录不可达
- 位置：apps/web/src/admin/pages/GovernanceTopologyPage.tsx:95–104、109–111、207、227–231；OrganizationMerchantPage.tsx:134–141、265、381、406–425；SubjectBindingsPage.tsx:124–134、198–201
- 分类：细化；工程缺陷：是；拟修复队列：是（正式ID待B2），不是添加项。
- 冻结判据：BSM-003、GEN-002、GEN-003、GEN-008、GEN-012。
- 证据：AdminApi.get→unwrap只返回data.items，无自动翻页；现有getPaged可保留total/page/size。当前Controller各列表默认size=500。三页不传page且无下一页；把数组length/局部去重及过滤数标成总数。内存真实响应500条,total501，KPI渲染500。最初使用测试信封50/51证明丢total；核到生产默认500后已用500/501修正再实跑，仍失败，不声称生产阈值50。
- 最小拟修边界：复用现有getPaged/分页能力，确保剩余行可达且总数按真实语义展示；跨表关联不能因独立首批截断误报未挂接。避免无限量抓全数据或提高固定上限掩盖。

### CX-03 — 暂停状态被译成已封禁，测试反而锁定错误业务含义
- 位置：apps/web/src/admin/pages/GovernanceTopologyPage.tsx:139、182；GovernanceTopologyPage.test.tsx:101–107；OrganizationMerchantPage.tsx:275、291、305、363；SubjectBindingsPage.tsx:89、202、238
- 分类：冲突；工程缺陷：是；拟修复队列：是（正式ID待B2），不是添加项。
- 冻结判据：GEN-002、GEN-003、GEN-008、GEN-015、GEN-017、PGM-001。
- 证据：共享customer/statusLabels.ts:27 suspended=已封禁，但当前业务HTML商户/运营主体六态把暂停与黑名单分列、项目六态也独列暂停；Subject动作字面也写暂停。内存商户suspended渲染“已封禁”而非暂停。不能仅因全中文就PASS。
- 最小拟修边界：在这三个页面按对象上下文映射暂停、主体/项目/绑定状态，删除错误显示与测试期望。共享通用码表用于其他业务的封禁语义不得顺手全改。

### CX-04 — 行键盘监听取消子控件默认动作，单选框空格无法选择
- 位置：apps/web/src/admin/pages/SubjectBindingsPage.tsx:230–233；OrganizationMerchantPage.tsx:316、363、375；GovernanceTopologyPage.tsx:173–178、200
- 分类：细化；工程缺陷：是；拟修复队列：是（正式ID待B2），不是添加项。
- 冻结判据：GEN-002、GEN-003、GEN-015。
- 证据：tr对任何冒泡Enter/空格一律preventDefault并开详情，只阻止click冒泡的td/input仍会触发row keydown。内存测试对绑定第二行radio发Space，fireEvent.keyDown返回false（默认动作被取消）；不是理论按键组合。三个页面相同写法还影响行内按钮/链接。现有测试只模拟鼠标click。
- 最小拟修边界：仅在事件来自行自身时执行行快捷键，保留原生input/button/link键盘动作；补Space/Enter负例与行自身开详情正例。

### CX-05 — 空间类型只内置八码，新建字典类型显示英文编码
- 位置：apps/web/src/admin/pages/GovernanceTopologyPage.tsx:14–17、95–100、152、200；GovernanceTopologyPage.test.tsx:10、18、117–128
- 分类：冲突；工程缺陷：是；拟修复队列：是（正式ID待B2），不是添加项。
- 冻结判据：GEN-002、GEN-003、GEN-008、GEN-015、PGM-001。
- 证据：后端已有空间类型字典维护且允许新增type_code=kiosk、type_name=一体机位，M011已有成功创建/使用测试。页面只查四端点、不读取空间类型字典；未知码传formatBusinessStatus，实际fallback原码。内存以正常字典和空间响应渲染结果“空间甲项目一L1120kiosk可用查看详情”，未显示一体机位。八固定码测试不能覆盖动态字典。
- 最小拟修边界：读取现有字典接口并用真实typeName显示，默认码可作已有中文兜底；未解析的编码不直出给业务用户。复用既有权限，不新增业务空间类型规则。


## 4. 21冻结判据逐项核对
| 判据 | 本次结果 |
|---|---|
| BSM-001 | FAIL：治理拓扑/档案/绑定三页面用途有来源；CX01/02影响关系与记录可达，不能把页面存在当完成。 |
| BSM-003 | FAIL：真实关系/完整数据应一致，CX01错关系、CX02只展示首批却以总数标记。 |
| GEN-001 | PASS：复用现有加载/错误/权限/抽屉/确认组件，页面用途明确；修复应优先现有getPaged，不另造全量抓取框架。 |
| GEN-002 | FAIL：37既有单测与typecheck均通过，但5个实际反例显示当前用户路径不正确。 |
| GEN-003 | FAIL：夹具只造旧关系/不分页/固定类型；错误暂停显示被测试要求；缺子控件键盘反例。 |
| GEN-004 | PASS：表单名称/业态/电话非空、绑定action/reason及selected校验；后端仍承担对象授权和真实状态校验，前端不能替代。 |
| GEN-005 | PASS：控制器真实授权仍在；缺副资源entity.binding.view不请求；写按钮已有功能点，未发现绕开后台的新通道。 |
| GEN-006 | FAIL：CX01关联关系不对、CX02局部集合衍生“全部”统计不完整；写端后端留痕与软删保持。 |
| GEN-007 | PASS：AdminApi同次401重放沿用键；绑定提交有submitting；本次未证实并发重复业务写缺陷，未为理论组合添加要求。 |
| GEN-008 | FAIL：CX01真实projectId未消费；CX02total被吞；CX03跨业务悬置状态误译；CX05字典数据未消费。 |
| GEN-009 | PASS：创建/存档/删除/绑定写错误捕获；load错误可重试，无吞成业务成功的新证据。 |
| GEN-010 | PASS：文本经React输出，后台手机号脱敏职责保留；未导出真实私人数据/凭据；无新敏感展示越权证据。 |
| GEN-011 | PASS：使用AuditTrailHint和setActionResult不代替后台真实审计；本次37绿、探针失败与真实exit均封存，不伪报。 |
| GEN-012 | FAIL：CX02首500截断且无后续页；未建议无界一次拉全，需复用分页。 |
| GEN-013 | PASS：本机160现有npm依赖，unit exit0，tsc -b --noEmit exit0；内存反例真实exit1按证据失败，不视为环境失败。 |
| GEN-014 | 不适用：六文件不含迁移；审计与拟修不应改变数据库模式或已冻结历史迁移。 |
| GEN-015 | FAIL：CX03/05业务显示错误；CX04行内子控件键盘不可用。普通鼠标抽屉与Esc已有测试通过。 |
| GEN-016 | FAIL：CX02总数声称失真；字段真实声称与新projectId偏离。既有证据附件上传缺口继续登记，不伪称已完成。 |
| GEN-017 | FAIL：5项逐分类已列，状态/英文显示冲突删除错误展示；其余已有功能细化有工程缺陷；无新增业务发明。 |
| GEN-018 | PASS：来源HTML当前原句、全部6文件/直接依赖原文、真实unit/typecheck/内存探针输出；旧双PASS不作当前正确证据。 |
| PGM-001 | FAIL：CX03含义错误、CX05自定义码英文直出；默认八码全中文不足以覆盖已有可维护字典。 |

## 5. 实际命令与完整结果
原单测：cd /home/james/complex-management/apps/web；npm run test:unit -- src/admin/pages/GovernanceTopologyPage.test.tsx src/admin/pages/OrganizationMerchantPage.test.tsx src/admin/pages/SubjectBindingsPage.test.tsx。
```text

> apps-web@0.1.0 test:unit
> vitest run src/admin/pages/GovernanceTopologyPage.test.tsx src/admin/pages/OrganizationMerchantPage.test.tsx src/admin/pages/SubjectBindingsPage.test.tsx


 RUN  v4.1.9 /home/james/complex-management/apps/web

Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
 ✓ src/admin/pages/GovernanceTopologyPage.test.tsx (11 tests) 134ms
 ✓ src/admin/pages/SubjectBindingsPage.test.tsx (11 tests) 139ms
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's getComputedStyle() method: with pseudo-elements
Not implemented: Window's getComputedStyle() method: with pseudo-elements
 ✓ src/admin/pages/OrganizationMerchantPage.test.tsx (15 tests) 729ms

 Test Files  3 passed (3)
      Tests  37 passed (37)
   Start at  12:59:33
   Duration  1.75s (transform 294ms, setup 150ms, import 844ms, tests 1.00s, environment 820ms)


M012_UNIT_EXIT=0

```
Typecheck：npm run typecheck，实际输出末尾：
```text
0.1.0 typecheck
> tsc -b --noEmit


M012_TYPECHECK_EXIT=0

```
内存探针命令为node --input-type=module，在apps/web中import {startVitest} from 'vitest/node'；第四参数viteOverrides.plugins使用enforce:'pre'的transform，将下节源追加到指定测试模块的内存code；不写模块文件。await ctx.close()后保留真实进程exit1。首轮执行拓扑与绑定两测试：22原用例绿+4反例红。后续testNamePattern仅选“默认首500|自定义空间类型”，2反例红、13未选跳过（不计通过）。
```text

 RUN  v4.1.9 /home/james/complex-management/apps/web

Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
Not implemented: Window's scrollTo() method
 ❯ src/admin/pages/SubjectBindingsPage.test.tsx (12 tests | 1 failed) 143ms
     ✓ 点“生效绑定”数字 → ?kpi=bindings&status=active，横幅(1)+表只剩生效子集，KPI 卡/变更表单隐藏 41ms
     ✓ 点“唯一绑定对象”数字 → 全量表 11ms
     ✓ 「返回全部」→ 恢复 KPI 卡+变更表单+全量表 13ms
     ✓ 带参直链进入 → 直接呈现聚焦视图 4ms
     ✓ KPI 子标签、表头与状态列为中文，不含英文 active/ID 7ms
     ✓ 表格按后台真实字段渲染商户/三主体编号与变更原因（读臆造键时整行是“-”，本用例必红） 6ms
     ✓ 点绑定行 → 抽屉含该行三主体编号（状态中文），Esc 关闭 11ms
     ✓ 点行内单选按钮不触发抽屉（stopPropagation） 5ms
     ✓ 未选变更后状态 → 表单报错、不发请求（后台 action 为必填三态） 9ms
     ✓ 原因为空 → 表单报错、不发请求（原因不再预填验收句） 9ms
     ✓ 选行+选状态+填原因 → 确认后 POST …/change，请求体含 action 与 reason、不含假证据附件 14ms
     × 单选框空格键不能被行处理器取消并打开详情 12ms
 ❯ src/admin/pages/GovernanceTopologyPage.test.tsx (14 tests | 3 failed) 159ms
     ✓ 点“空间点位”数字 → URL 带 ?kpi=spaces，只剩空间表+横幅 38ms
     ✓ 点“商户”数字 → 只剩主体-商户-项目表+横幅 15ms
     ✓ 「返回全部」→ 参数清除，恢复完整页面（两表+KPI 卡都在） 13ms
     ✓ 带参直链进入 → 直接呈现聚焦视图 5ms
     ✓ 渲染输出状态列为中文，不含原英文枚举 active/suspended 6ms
     ✓ 项目业态与空间类型渲染为中文，不直出 bar/shop 英文码（PGM-001） 6ms
     ✓ 「已挂接」按后台 project_id 计数（读臆造 venue_id 时恒为 0，本用例必红） 6ms
     ✓ 点拓扑关系行 → 抽屉含主体/商户/项目与中文状态、中文业态 9ms
     ✓ 点空间点位行 → 抽屉含空间名/所属项目/面积/中文空间类型 8ms
     ✓ Esc 关闭抽屉 + 点遮罩关闭 15ms
     ✓ 缺主体查看权限：business-entities 端点根本不请求，整页不白屏，商户/项目/空间照常渲染 6ms
     × 按商户project_id显示项目，不把project.merchant_id当归属 8ms
     × 暂停商户不能展示为已封禁 6ms
     × 后端51条商户不能把首50条称作商户总数 18ms

⎯⎯⎯⎯⎯⎯⎯ Failed Tests 4 ⎯⎯⎯⎯⎯⎯⎯

 FAIL  src/admin/pages/GovernanceTopologyPage.test.tsx > M012 Codex内存审计反例，不写源码 > 按商户project_id显示项目，不把project.merchant_id当归属
AssertionError: expected '经营主体统一社会信用代码商户项目项目编码业态状态详情平台经营主体ORG-1…' to contain '项目一'

Expected: "项目一"
Received: "经营主体统一社会信用代码商户项目项目编码业态状态详情平台经营主体ORG-1商户甲--—有效查看详情"

 ❯ src/admin/pages/GovernanceTopologyPage.test.tsx:236:71
    218|     expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String)…
    219|   });
    220| });
       |              ^
    221|

⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯[1/4]⎯

 FAIL  src/admin/pages/GovernanceTopologyPage.test.tsx > M012 Codex内存审计反例，不写源码 > 暂停商户不能展示为已封禁
AssertionError: expected '平台经营主体ORG-1商户乙--—已封禁查看详情' to contain '暂停'

Expected: "暂停"
Received: "平台经营主体ORG-1商户乙--—已封禁查看详情"

 ❯ src/admin/pages/GovernanceTopologyPage.test.tsx:240:78
    218|     expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String)…
    219|   });
    220| });
       |              ^
    221|

⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯[2/4]⎯

 FAIL  src/admin/pages/GovernanceTopologyPage.test.tsx > M012 Codex内存审计反例，不写源码 > 后端51条商户不能把首50条称作商户总数
AssertionError: expected '50' to be '51' // Object.is equality

Expected: "51"
Received: "50"

 ❯ src/admin/pages/GovernanceTopologyPage.test.tsx:252:79
    218|     expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String)…
    219|   });
    220| });
       |              ^
    221|

⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯[3/4]⎯

 FAIL  src/admin/pages/SubjectBindingsPage.test.tsx > M012 Codex内存键盘反例，不写源码 > 单选框空格键不能被行处理器取消并打开详情
AssertionError: expected false to be true // Object.is equality

- Expected
+ Received

- true
+ false

 ❯ src/admin/pages/SubjectBindingsPage.test.tsx:226:19
    215|     expect(body).not.toContain("完整后台验收变更");
    216|   });
    217| });
       |              ^
    218|

⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯[4/4]⎯


 Test Files  2 failed (2)
      Tests  4 failed | 22 passed (26)
   Start at  13:00:56
   Duration  1.20s (transform 1.21s, setup 1.20s, import 168ms, tests 302ms, environment 528ms)


M012_PROBE_EXIT=1
BUSINESS_SOURCE_UNCHANGED=True

```
```text

 RUN  v4.1.9 /home/james/complex-management/apps/web

 ❯ src/admin/pages/GovernanceTopologyPage.test.tsx (15 tests | 2 failed | 13 skipped) 160ms
     ↓ 点“空间点位”数字 → URL 带 ?kpi=spaces，只剩空间表+横幅
     ↓ 点“商户”数字 → 只剩主体-商户-项目表+横幅
     ↓ 「返回全部」→ 参数清除，恢复完整页面（两表+KPI 卡都在）
     ↓ 带参直链进入 → 直接呈现聚焦视图
     ↓ 渲染输出状态列为中文，不含原英文枚举 active/suspended
     ↓ 项目业态与空间类型渲染为中文，不直出 bar/shop 英文码（PGM-001）
     ↓ 「已挂接」按后台 project_id 计数（读臆造 venue_id 时恒为 0，本用例必红）
     ↓ 点拓扑关系行 → 抽屉含主体/商户/项目与中文状态、中文业态
     ↓ 点空间点位行 → 抽屉含空间名/所属项目/面积/中文空间类型
     ↓ Esc 关闭抽屉 + 点遮罩关闭
     ↓ 缺主体查看权限：business-entities 端点根本不请求，整页不白屏，商户/项目/空间照常渲染
     ↓ 按商户project_id显示项目，不把project.merchant_id当归属
     ↓ 暂停商户不能展示为已封禁
     × 后端501条商户不能把默认首500条称作商户总数 152ms
     × 后台已配置的自定义空间类型应显示字典中文名称 7ms

⎯⎯⎯⎯⎯⎯⎯ Failed Tests 2 ⎯⎯⎯⎯⎯⎯⎯

 FAIL  src/admin/pages/GovernanceTopologyPage.test.tsx > M012 Codex内存审计反例，不写源码 > 后端501条商户不能把默认首500条称作商户总数
AssertionError: expected '500' to be '501' // Object.is equality

Expected: "501"
Received: "500"

 ❯ src/admin/pages/GovernanceTopologyPage.test.tsx:251:79
    218|     expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String)…
    219|   });
    220| });
       |              ^
    221|

⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯[1/2]⎯

 FAIL  src/admin/pages/GovernanceTopologyPage.test.tsx > M012 Codex内存自定义字典反例 > 后台已配置的自定义空间类型应显示字典中文名称
AssertionError: expected '空间甲项目一L1120kiosk可用查看详情' to contain '一体机位'

Expected: "一体机位"
Received: "空间甲项目一L1120kiosk可用查看详情"

 ❯ src/admin/pages/GovernanceTopologyPage.test.tsx:267:70
    218|     expect(setGlobalError).not.toHaveBeenCalledWith(expect.any(String)…
    219|   });
    220| });
       |              ^
    221|

⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯[2/2]⎯


 Test Files  1 failed (1)
      Tests  2 failed | 13 skipped (15)
   Start at  13:01:41
   Duration  614ms (transform 83ms, setup 53ms, import 83ms, tests 160ms, environment 253ms)


M012_CORRECTED_PROBE_EXIT=1
BUSINESS_SOURCE_UNCHANGED=True

```

## 6. 可复现探针源（内存，不是修复）
初次拓扑探针：
```tsx

describe("M012 Codex内存审计反例，不写源码", () => {
 afterEach(() => { vi.unstubAllGlobals(); window.history.replaceState({}, "", "/admin/governance/topology"); });
 it("按商户project_id显示项目，不把project.merchant_id当归属", async () => {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input) => {
   const url=String(input);
   if(url.includes("business-entities"))return Promise.resolve(envelope(ENTITIES));
   if(url.includes("merchants"))return Promise.resolve(envelope([{...MERCHANTS[0],project_id:"p-1"}]));
   if(url.includes("projects"))return Promise.resolve(envelope([{...PROJECTS[0],merchant_id:null}]));
   if(url.includes("spaces"))return Promise.resolve(envelope([]));
   return Promise.resolve(envelope([]));
  }));
  render(<GovernanceTopologyPage api={new AdminApi("test")} user={{id:"u1",username:"admin",accountType:"platform_admin"}} setGlobalError={vi.fn()} setActionResult={vi.fn()} effectivePermissions={ADMIN_PERMS}/>);
  await waitFor(()=>expect(screen.getByTestId("governance-topology-table")).toBeTruthy());
  expect(screen.getByTestId("governance-topology-table").textContent).toContain("项目一");
 });
 it("暂停商户不能展示为已封禁",async()=>{
  renderPage(); await waitFor(()=>expect(screen.getByTestId("governance-topology-row-m-2-none")).toBeTruthy());
  expect(screen.getByTestId("governance-topology-row-m-2-none").textContent).toContain("暂停");
  expect(screen.getByTestId("governance-topology-row-m-2-none").textContent).not.toContain("封禁");
 });
 it("后端51条商户不能把首50条称作商户总数",async()=>{
  const rows=Array.from({length:50},(_,i)=>({id:"many-"+i,merchant_name:"商户"+i,status:"active"}));
  vi.stubGlobal("fetch",vi.fn().mockImplementation((input)=>{
   const url=String(input);
   if(url.includes("merchants"))return Promise.resolve(new Response(JSON.stringify({code:"OK",data:{items:rows,page:1,page_size:50,total:51}}),{status:200,headers:{"content-type":"application/json"}}));
   return Promise.resolve(envelope([]));
  }));
  render(<GovernanceTopologyPage api={new AdminApi("test")} user={{id:"u1",username:"admin",accountType:"platform_admin"}} setGlobalError={vi.fn()} setActionResult={vi.fn()} effectivePermissions={ADMIN_PERMS}/>);
  await waitFor(()=>expect(screen.getByTestId("governance-topology-kpi-merchants")).toBeTruthy());
  expect(screen.getByTestId("governance-topology-kpi-merchants").textContent).toBe("51");
 });
});

```
绑定键盘探针：
```tsx

describe("M012 Codex内存键盘反例，不写源码",()=>{
 afterEach(()=>{vi.unstubAllGlobals();window.history.replaceState({},"","/admin/governance/subject-bindings");});
 it("单选框空格键不能被行处理器取消并打开详情",async()=>{
  renderPage();await waitFor(()=>expect(screen.getByTestId("subject-bindings-select-b-2")).toBeTruthy());
  const radio=screen.getByTestId("subject-bindings-select-b-2");radio.focus();
  const allowed=fireEvent.keyDown(radio,{key:" ",code:"Space"});
  expect(allowed).toBe(true);
  expect(screen.queryByTestId("row-detail-drawer")).toBeNull();
 });
});

```
修正分页阈值并加自定义类型后的拓扑探针：
```tsx
describe("M012 Codex内存审计反例，不写源码", () => {
 afterEach(() => { vi.unstubAllGlobals(); window.history.replaceState({}, "", "/admin/governance/topology"); });
 it("按商户project_id显示项目，不把project.merchant_id当归属", async () => {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input) => {
   const url=String(input);
   if(url.includes("business-entities"))return Promise.resolve(envelope(ENTITIES));
   if(url.includes("merchants"))return Promise.resolve(envelope([{...MERCHANTS[0],project_id:"p-1"}]));
   if(url.includes("projects"))return Promise.resolve(envelope([{...PROJECTS[0],merchant_id:null}]));
   if(url.includes("spaces"))return Promise.resolve(envelope([]));
   return Promise.resolve(envelope([]));
  }));
  render(<GovernanceTopologyPage api={new AdminApi("test")} user={{id:"u1",username:"admin",accountType:"platform_admin"}} setGlobalError={vi.fn()} setActionResult={vi.fn()} effectivePermissions={ADMIN_PERMS}/>);
  await waitFor(()=>expect(screen.getByTestId("governance-topology-table")).toBeTruthy());
  expect(screen.getByTestId("governance-topology-table").textContent).toContain("项目一");
 });
 it("暂停商户不能展示为已封禁",async()=>{
  renderPage(); await waitFor(()=>expect(screen.getByTestId("governance-topology-row-m-2-none")).toBeTruthy());
  expect(screen.getByTestId("governance-topology-row-m-2-none").textContent).toContain("暂停");
  expect(screen.getByTestId("governance-topology-row-m-2-none").textContent).not.toContain("封禁");
 });
 it("后端501条商户不能把默认首500条称作商户总数",async()=>{
  const rows=Array.from({length:500},(_,i)=>({id:"many-"+i,merchant_name:"商户"+i,status:"active"}));
  vi.stubGlobal("fetch",vi.fn().mockImplementation((input)=>{
   const url=String(input);
   if(url.includes("merchants"))return Promise.resolve(new Response(JSON.stringify({code:"OK",data:{items:rows,page:1,page_size:500,total:501}}),{status:200,headers:{"content-type":"application/json"}}));
   return Promise.resolve(envelope([]));
  }));
  render(<GovernanceTopologyPage api={new AdminApi("test")} user={{id:"u1",username:"admin",accountType:"platform_admin"}} setGlobalError={vi.fn()} setActionResult={vi.fn()} effectivePermissions={ADMIN_PERMS}/>);
  await waitFor(()=>expect(screen.getByTestId("governance-topology-kpi-merchants")).toBeTruthy());
  expect(screen.getByTestId("governance-topology-kpi-merchants").textContent).toBe("501");
 });
});

describe("M012 Codex内存自定义字典反例",()=>{
 afterEach(()=>{vi.unstubAllGlobals();window.history.replaceState({},"","/admin/governance/topology");});
 it("后台已配置的自定义空间类型应显示字典中文名称",async()=>{
  vi.stubGlobal("fetch",vi.fn().mockImplementation((input)=>{
   const u=String(input);
   if(u.includes("space-types"))return Promise.resolve(envelope([{id:"type-kiosk",type_code:"kiosk",type_name:"一体机位",status:"active"}]));
   if(u.includes("spaces"))return Promise.resolve(envelope([{...SPACES[0],space_type:"kiosk"}]));
   if(u.includes("projects"))return Promise.resolve(envelope(PROJECTS));
   return Promise.resolve(envelope([]));
  }));
  render(<GovernanceTopologyPage api={new AdminApi("test")} user={{id:"u1",username:"admin",accountType:"platform_admin"}} setGlobalError={vi.fn()} setActionResult={vi.fn()} effectivePermissions={ADMIN_PERMS}/>);
  await waitFor(()=>expect(screen.getByTestId("governance-space-row-s-1")).toBeTruthy());
  expect(screen.getByTestId("governance-space-row-s-1").textContent).toContain("一体机位");
 });
});

```

## 7. 观察与边界
旧M012-001–008修复和009附件能力登记全部保留，不更改为新完成。Subject页active→inactive的旧mock只验证请求体，不是后端变更允许性的证明；真实后端现会对需要财税审核的停用请求报错，页面catch可见。本次不因此发明财税审核新功能。项目新增三个档案字段此前批准范围仅后端/数据库，网页没有输入框不再次作为未批准新增任务提出。
本次未运行真实浏览器/全仓测试，不夸大jsdom覆盖。原jsdom scrollTo/getComputedStyle提示不影响37真实通过；新反例失败不是Grok运行失败或测试环境故障。无HTML申请、添加业务问题、审批弹窗；B1/B2/B4/B5不修。
Codex本次B1审计FAIL。全6文件/21项审完，5项全部封存，等待Grok独立票真实交付后整批归并B2；B3仍1。业务HTML/源文件/测试均未改；未提交推送合并部署。


# M012 本次重开 B2 整批归并复核 · Codex
报告自身版本：V0.05。本段追加在 V0.04 完整 B1 后，旧61399bytes逐字保留。阶段B2、模块IN_PROGRESS、历史B3累计1、本次新增0。只复核归并，不实施修复。
## 输入与输出身份
输入：modules/M012-b1-self-raw.md V0.04（61399bytes，SHA256 5a776f60330237348994adf36f765352a8f1e0d083b2ed74b6f2d28a500ee4f6）；modules/M012-b1-grok-raw.md V0.04（117487bytes，SHA256 d89fa6cd0a6110c36156c825d323d6d7f0bc9406b91e8494a52ebbfa03b630f1）。
候选：docs/audit-2026-09/findings.md V2.16，877060bytes，SHA256 e802cf09b96b397334b933ccad13d6b2fc7a7bbc6fc499270e363eab1e20df54。顶端M012当前候选为本席复核对象，不把历史表当当前值。
Grok本次B1确已exit0；本席先封存B1才读其正文。B2派Grok后本席独立复核，下方结论不读取本次Grok B2。两席B2都PASS前候选不定稿、不执行B3。
## 全部差集
|原条目|正式ID/处置|本席复核|
|---|---|---|
|CX-01 与 GR-M012-010|010是，关联测试共同011|同一根因去重；保留Grok组织页面分支。完整HTML要求看清所属综合体，现行Merchant.projectId才是关系，Project.merchantId不得反当上级。NULL历史不可见不当作要恢复越权显示。|
|GR-M012-011|011是|测试夹具反连为真实工程缺陷。两页真实merchant.project_id；绑定失败与组织缺权限负例保留。BindingRecord无projectId所以不造字段，此为事实澄清不隐去有效缺陷。|
|GR-M012-012|012是|接受完整导语问题，仅删幂等键/附件过声称；改后保留原因及状态选择。无HTML修改、新附件控件。|
|CX-02 / GR OBS-01|013是，本席仍认缺陷，待Grok复核分歧|生产默认500，正常返回500items/total501即触发，无并发/故障组合。原get丢total无后页是代码路径事实；实跑500/501失败。既有getPaged与TablePager可修、统计收窄本页，不发明汇总后台。已明确争议，不写成双方已同意。|
|CX-03 / GR OBS-04|014是|暂停≠黑名单，全局用户状态词表不能用于此业务。读已有pending/归档状态也是首次正常路径，创建默认active不能豁免。保留已有合法态、删除误义/英文直出，不增转换。|
|CX-04|015是|真正radio Space返回false与开抽屉，键盘被父行取消；target=currentTarget收窄既有监听足够。不增键盘框架。|
|CX-05|016是|动态字典可新建kiosk/一体机位且后端接受空间，页面固定八码不足。使用既有字典读口，不加新业务类型，缺词时中文未识别，不把英文字典编码冒充允许内部ID。|
|GR OBS-02/03/05/06，与本席其余观察|009否|幂等双击/内部ID/无创建或归属UI/URL内部键及已有财税守卫均登记；旧附件缺口保持，不以012改文案当作已补上传。批准项目档案三资料后端范围不扩成前端必做。|
|历史M012-001..008|仍否|旧修复已存在，未因旧表是而重复修。历史009保留，未重复分配ID。|

原始双方5+3条全部有映射；同根因关系合并，7项拟修；观察登记一处，不删任何原报告或意见。无添加、无HTML问题、无questions，用户已经允许普通代码修复，本批不再制造审批。
## 分类复核（依冲突→细化→技术性→添加顺序）
010/011：具体上位是business-system-map:509–510“哪个综合体、谁在经营、在哪经营、签了什么合同、谁能操作、谁负责。”和platform-governance-map:260–261“先管归属：哪个综合体、哪个楼层、哪个区域、哪个点位、哪个商户”。归属实现和其对应夹具先归细化；011虽是测试而Grok原报技术性，候选按具体业务上位先细化，原票未改写，必须让Grok独立复核而非单方定稿。权限/失败验证不产生新权限或新审批。
012/013/015：既有页面说明、现有列表与键盘操作的下位实现，归细化且有实际工程缺陷。013复用现有getPaged与分页组件、纠正统计语义；不是为未来新接口增加机制。
014/016：PGM:287要求菜单/状态/枚举取值全中文；BEQR:237项目状态草稿、待启用、已启用、暂停、终止、归档；:238商户/运营主体待准入、正常、暂停、终止、归档、黑名单。已于本B2只读复核原HTML。错误封禁词义与kiosk直出违反明文，冲突，处置是删除错误词义/英文直出路径并使用现有语义，不能删除整个业务功能。
009：旧登记项无本批工程修复，不把HTML沉默当冲突，不新增判据、业务规则、角色或流程。
## 修复边界与验证可执行性
只允许六个已分配页面/test文件；局部类型补projectId，现有全局AdminApi、TablePager/useServerPagedRows、权限函数、原生事件可直接复用。组织项目表及抽屉旧“商户”反连应同时纠正，同010，不只改一处表格。绑定DTO不扩字段。字典端点现存PlatformController:169–173，非新建API。
分页影响所有列表；后页必须可达、总计与本页区分，分页后的关系不可因子资源首批遗漏就报无关联；如实现必须超候选六文件或新机制应回B2而非偷做。无无限fetchall、无抬固定上限、无新增共享分页器。三页的过滤/刷新/选中对象和写错误仍保持，回归须覆盖。
当前三测试37/37及typecheck已真实执行PASS；B1内存探针分别复现关系、状态、分页、键盘、字典缺陷且业务六文件SHA不变。上述是已执行证据，未重跑充数；未跑浏览器/真实HTTP，不声称这些通过。
B3仍未开始。本次只读补核HTML状态原句、字典端点、Organization抽屉/表格共同调用，未改业务文件。全批修复后需适当回归/typecheck、完整before/after diff逐块ID归因，B4双PASS后才转否，B5全量双PASS才关模块。
## 本席结论
**PASS**
这是对候选V2.16归并完整性、分类、队列与最小边界的本席B2票；不是模块PASS，也不是已获Grok同意。尚待Grok B2完整结论；任一FAIL整批不可执行。


## M012 获批追加一次修复的 B2 整批归并复核（Codex）
文件版本：V0.06
本批B2结论：PASS（仅本席；Grok本次尚未读取，不代表双PASS）。
本席按receiving-code-review先核真实调用链再判断候选，不以他席意见或旧绿票代替事实；不派第三席。先派Grok，本席完整B2封存后才读其本次追加段。操作员“批准”指向M012再执行1次修复，B3累计3保持，源/测试/HTML未动。
输入完整报告：modules/M012-b5-r3-self-raw.md V0.01/15529bytes/SHA f09e7ce3c550c4d56f6947bcf100b8f3f087c8f8d72d0391a28db2ae5d551b6c；modules/M012-b5-r3-grok-raw.md V0.01/18848bytes/SHA f02108cfa1a711ce8bf47f334b536fc41b26991c246ae6de219f051ba1c813a5。上轮两票FAIL未覆盖、未重写。
候选findings V2.21 SHA256 516a6478dda793c8ed27ec661379f9daae7248cc111c26ff7746fcd2f9df518a。完整并集只有CX-B5-01与GR-M012-B5-01，分别新017与旧013；无漏并/新增锁外主张。M012映射21未变，B2不写新独占锁行。

|本批依据|正式ID|分类与队列票|核实结果|
|---|---|---|---|
|CX-B5-01|M012-017|细化/有缺陷/是；PASS|实际拓扑import6和节点245唯一使用AuditTrailHint；组织import6含AuditTrailHint及auditTraceField，节点419唯一Hint，319仍使用auditTraceField。删除两个纯提示节点不删除该真实追溯入口。后台实际事件不同，组件自身无日志行为，旧文字错误成立。|
|GR-M012-B5-01|M012-013|细化/本批反例不成立/否；PASS|Controller144–150要求platformscope；Service444–445传currentProject；Repository1287–1300 COUNT和SELECT按projects.id=:project_id，V005__p0_platform_master_data.sql:36–47定义id VARCHAR(64) PRIMARY KEY；所以当前projects接口最多1条。商户同1267–1284按project_id，后台501个项目不等于该响应有501项。仅手造首页500项目无当前契约证据，不能支持增项目分页。|

017先检查冲突不成立，再指向BSM-001/PGM-001的溯源上位归细化，引用冻结GEN-011/016；没有新业务默认值、实体、状态、权限或收费。PGM内部标识符可不翻译，只解决语言例外，不允许虚假事件说明。AuditTrailHint的“用户裁决折叠、原样保留”注释不是这些事件真实存在的证据；用户本次已批准删除两个提示的具体范围。
真实事件读取Service94 entity_binding.viewed、152 merchant.viewed、447 project.viewed、514 space.viewed、528 space_type.viewed；商户POST的saveMerchant成功事件993 merchant.updated。两页Hint不带navigateTo/businessType/businessId，本身无反查链接，故删节点只删不准文案。保留TechDetails接口证据和组织auditTraceField。
017最小处置：Topology删import AuditTrailHint与JSX行，V0.03→V0.04；Organization从现有组合import只移除AuditTrailHint、保留auditTraceField，删JSX，V0.04→V0.05。只改这两源。无共享组件、后台、types、测试、HTML、PRD等范围变化。不增加审计事件补旧承诺。
纯提示内容删除不新增同构单测：现有56用例/tsc跑完整，附源码检查及全部diff确认；B4以两tree完整diff核每块017，B5全6文件/21判据重审。未把前次56绿写成本次修后已跑。
013原已验收分页/重试保持；本批只建议否，不把Grok原FAIL抹掉。Grok必须自行复核并在本次B2明确改判/撤回；若不认可候选则本批不能双PASS，本席不能先修017。以前001–016仍否，009观察仍是原边界。
无添加项、无HTML问题、无未答questions、无新判据。当前本席PASS不授权直接B3；等待Grok完整B2票并合取，用户批准的一次修复尚未消耗。



CRITERIA-LOCK-SHA256: b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458
# M012 当前锁重开 B1／Codex 独立完整报告
版本：V0.07

## 范围与独立性
正本 /home/james/complex-management，main主工作区；HEAD e456ba96c84185c8d39f18f548011daac48a6466。按160-audit-prompt.md V0.17、runbook V0.02，固定Codex/Grok两席。当前锁独立实算为首行值，sidecar及ledger一致；LOCK-AMEND-COUNT=1。本报告写入前未阅读本次Grok B1正文/JSON，不以旧PASS替代当前票。Grok此前已交付，本席接续独立全审，不重派。M012 IN_PROGRESS，B3累计4，方案修复2，失败重跑0；未执行第5次修复。仅本报告及ledger可写，业务代码/测试/HTML未修改。

## 六文件覆盖
路径前缀 apps/web/src/admin/pages/，全部逐文件阅读全文，共2085行：
| 文件 | 行数 | SHA256 | 审计内容 |
|---|---:|---|---|
| GovernanceTopologyPage.tsx | 274 | 6338225cb0367502fefb3db2348673fe602ee9e6bed3cdae321feeea9a9b7036 | 三域分页、字典、正向项目连接、权限、KPI、键盘与下钻 |
| GovernanceTopologyPage.test.tsx | 308 | 985196ad40864145f1be814cad1f5db6667883a114aed27e61453188219a28cb | 19例真实snake_case信封、501边界、权限与错误恢复 |
| OrganizationMerchantPage.tsx | 520 | d3ddb2cc423cc901190e8a5d9c5c6f2e4095919d956a19a6c866124947fa6722 | 创建、生命周期、三表、筛选分页、抽屉、焦点提示、审批反馈 |
| OrganizationMerchantPage.test.tsx | 435 | d40c83d948e0b07f83f6eb5bcbb2a18af12b186ddd93fdcc651557d5870408a7 | 21例闭集创建/无假数据、归档、申请、失败、分页与键盘 |
| SubjectBindingsPage.tsx | 257 | 94f4d4dcfcb24c85701450794544574efe26f209a4aed242ff5c428af98d247c | 真实绑定DTO、三态、原因、选择、确认、权限、失败与本页KPI |
| SubjectBindingsPage.test.tsx | 291 | ec22a9250ed24f61031f1997119b5537d77d1af537e388eff2018fa947d8591b | 16例DTO/请求/缺值/错误/501分页/单选键盘 |

六文件与既有B3-r4 after tree f2366e35ef257fbc349cbbc5e00e0052cc2b389c逐字相同，实算oldSame全True；这仅证明未漂移，不证明现锁PASS。

## 三线
第一性原理：归属展示是BSM-001/PGM-001基础盘子；删除页面会失去既有商户建档/绑定状态操作与关系查看。分页、DTO转键、错误传播、测试夹具是运行和验证手段，无业务发明。检查“有链接=已找到来源”“mock200=财税变更可执行”“有approve权限=密钥最高权限”等未经验证前提，均不接受。
顺从式：沿页面->AdminApi->PlatformController->PlatformService/Jdbc字段核对。getPaged保留total，客户端转camelCase；主体读受entity.binding.view门，服务端项目/商户范围与动作授权仍执行；projects按当前projectId主键查询，不能用虚构501项目断言拓扑缺项目pager。真实merchant.projectId、space.projectId归属正确；Subject后台三态一致，财税守卫拒绝路径由页面显错。
对抗式：检查501后页、末页删除、未授权主体、父行键盘吞按钮/单选、加载失败再试、缺原因、未选状态、假主体/附件/默认数据、无目标下钻、密钥本人删除提示。前述17项已修行为现测试有效；最后两项仍有首次正常路径可见错误，见C-A1/C-A2。没有因为理论极端场景加功能。

## 全21映射逐项
| 判据 | 当前核验 |
|---|---|
| GEN-001 | 复用分页/选择/按钮/引导组件，无需另造定位器/权限机制。旧伪审计提示已删。 |
| GEN-002 | 三页面56测试+typecheck成功；定位成功声明与实际匹配不同，C-A1；审批文案与服务不同，C-A2。 |
| GEN-003 | 真实AdminApi/fetch信封与断言执行目标；56例通过不覆盖全部focus/审批文案，新增缺陷后B3才补最小回归。 |
| GEN-004 | 用户必填/三态前端校验；服务端校验动作、理由、真实对象；未发现本模块新增信任绕过。 |
| GEN-005 | 页面Permission/RowActions仅呈现，Controller与Service仍强制动作/范围/最高权及禁自审。无权主体不请求。 |
| GEN-006 | 当前模块无事务/数据库写实现，写入走既有服务；mock不证明持久化。项目归属不反连。 |
| GEN-007 | Idempotency key随请求/401重放保留；Date.now逐次提交是既有009观察，不凭它新增并发协议。 |
| GEN-008 | DTO字段与snake/camel一致、闭集三态/六业态/六合作匹配；C-A2反馈语义不一致。 |
| GEN-009 | 写失败catch留输入/全局显错，重试清错、末页回首；focus不命中误称成功为C-A1。 |
| GEN-010 | React转义，联系人展示后端授权结果，不另建明文开关；现HTML授权例外不扩展到导出。 |
| GEN-011 | organization商户抽屉真实auditTrace链接；Subject entity_binding.viewed/changed在Service739/817真实存在；旧topology伪事件已删。 |
| GEN-012 | 分页500有边界/501可达；去重指标明示本页，关联资料未加载不冒称不存在。 |
| GEN-013 | npm现装Vitest4.1.9与tsc现命令成功；无新依赖/配置。 |
| GEN-014 | 本模块无SQL/迁移，不适用；未操作数据库。 |
| GEN-015 | 中文状态、字典、错误、键盘与抽屉基本可用；错误定位/审批提示为两候选。 |
| GEN-016 | 页面技术接口声明与当前controller相符；不把旧票作现票；不声称浏览器/HTTP/数据库已实测。 |
| GEN-017 | 逐类按冲突->细化->技术性->添加；新候选均既有展示/操作细化错误，不发明业务；006/007已答删除仍有效。 |
| GEN-018 | 本报告证据为当前源码/HTML及下面真实执行命令；技能/历史票不作业务来源。 |
| BSM-001 | 基础归属正连；KPI与来源点入既有能力；无目标误称已定位违背可执行声称，C-A1。无金额计算/策略配置变更。 |
| BSM-003 | 按其冻结通用导语及GEN检查页面引导/既有操作；不把未映射子节变成新增判据。 |
| PGM-001 | 归属/资格/责任展示与中文/追溯检查；C-A1损伤下钻反馈真实性；无新税额分摊/并发实现。 |

## 本轮候选（尚未定稿，不修）
### C-A1：未匹配对象仍声称“已定位”
- 文件 OrganizationMerchantPage.tsx:168-177、415-416；工程缺陷是，分类细化，依据类型2/4，冻结GEN-002/009/015、BSM-001、PGM-001。
- 原句：`{focusObjectId && (... 已从下钻定位到商户：{focusObjectId}（在下表中查看其档案与归属） ...)}`。
- 效果反例：从既有TraceLink进入目标为第501个商户；初次getPaged取前500，无该目标；滚动效应171行明确未命中return，415行只检查focusObjectId却仍声明已定位。即使目标被当前过滤隐藏也一样。只需正常分页，无多重意外。
- 证据命令：Python读取该页断言存在上述两不同guard；exit0，输出“focus banner guard=focusObjectId only; scroll guard=loaded merchants contain id. Target on second page => banner true, scroll guard false.”。此为源码可复现证据，不冒充新增DOM测试。
- 最小拟法：只在目标确在当前可见表格时声称已定位；不命中/加载时删除或收窄成功文案，明确尚未在当前显示列表定位。保留现有翻页/过滤能力。不新增跨页扫描、接口、定位器或项目分配功能。既有test文件补当前页命中/后页未命中反馈断言（B3执行）。

### C-A2：删除理由框无条件承诺“提交后需上级审批”
- 文件 OrganizationMerchantPage.tsx:256；工程缺陷是，分类细化（业务提示与真实已有流程不一致），依据类型1/2/4；判据GEN-002/008/015/017。HTML原文只解释现有流程，不新增映射。
- 原句：“删除理由（提交后需上级审批）：”。
- 业务原文 business-system-map-for-operations-v0.1.html:570：“要删除，得先存档，再‘申请删除’、填写删除理由，由具备最高权限的人审批通过后执行（申请人不能审批自己的申请）。”576-577：“持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。”
- 实际PlatformService.java:283-295在最高权限本人分支直接软删；328-335其他审批须最高权且禁自审。因此普通上级并非均可审批，密钥本人并不等待后续审批。安全操作提示会使操作者误解即时后果。
- 证据：read/rg当前页面256及Service254-350；Python存在性断言exit0，输出“UI promises superior review; service requires master and master request directly soft-deletes.”。
- 最小拟法：将该句收窄到真实规则“删除理由（由最高权限审批；持授权 U 盘密钥的最高权限本人发起时直接执行）：”。仅提示文案与现有test校验；不改服务端/权限/删除/恢复/HTML，不新增流程。

## 既有17项与观察
001/002真实绑定字段/三态；003真实空间归属；004中文；005真实夹具；006已答删除假预填/隐式主体/附件；007已答删除11位电话限制，仅非空；008写失败；010merchant.projectId正连；011权限/DTO/错误测试；012删除导语过度声称；013分页/本页KPI/错误恢复；014状态中文；015父行键盘；016字典；017伪审计提示删除：当前确认已执行，不重开为待修。
009保留登记：无新增绑定创建/上传/项目分配/三资料前端，主后端能力不等于前端全可用；NULL归属不得放宽，财税拒绝mock不得冒称放行；Date.now键、共享statusLabels、内部ID、其它模块OpenAPI观察不扩修复。单纯无approve/reject测试覆盖不在缺乏具体错误时独立新设覆盖率要求。C-A1/C-A2是本次实证提示错误，提交B2决定独立ID或合并，不自行覆盖历史。

## 实际验证与限制
2026-09-22，正本apps/web：
`npm run test:unit -- src/admin/pages/GovernanceTopologyPage.test.tsx src/admin/pages/OrganizationMerchantPage.test.tsx src/admin/pages/SubjectBindingsPage.test.tsx`
exit0，3files/56tests PASS（19+21+16），4.13s；jsdom原生scrollTo/getComputedStyle提示保留，不当作业务FAIL。
`npm run typecheck` exit0。
六源SHA及与aftertree逐字比较已执行，全相同。
未跑真实浏览器、真实HTTP、全量后端、DB迁移；未写业务文件，不伪报。

技能：当前会话规则启用ADHD/ponytail；实际读取using-superpowers、grilling、diagnosing-bugs、codebase-design、receiving-code-review，按V0.17取消第三席/问人/无ID修改，仅用三线追问、现有seam与证据定位。B1不用TDD落盘、无B3；其它未实际读取不称已调用。

## 完整结论
B1：FAIL。全范围已审，候选C-A1/C-A2待与Grok整批B2；无新添加项/HTML修改要求。当前非B5，不宣称模块PASS。先保存本完整票，再读取本次Grok正文。B3累计4不清零；如B2后仍有修复队列，必须处理追加轮次授权后才能B3。

# M012 当前锁 B2／Codex 完整归并票
版本：V0.08

输入：modules/M012-b1-self-raw.md V0.07完整B1 FAIL、modules/M012-b1-grok-raw.md V0.07完整B1 PASS；不得混用旧b012锁票。本次仅复核findings顶部候选，不读取本次Grok B2后抄票。锁实算/sidecar/ledger/B1Grokr四值均b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458，LOCK-AMEND-COUNT1。B3累计4，IN_PROGRESS。

差集：根C-A1→M012-018，C-A2→M012-019；Grok无GR工程缺陷，OBS-R-01—10全部续入009；旧001—008、010—017已修状态保持，006/007已答删除不问。009的TraceLink观察不吞掉当前页错误成功声明：018限“已定位”的真假及匹配可见性，不以此前登记推翻实际缺陷。019是已有删除入口安全提示与当前Service分支不符，既非新增审批又不要求后台变更。GEN-002/008/009/015足以支持，无需扩HTML映射。

分类均细化：既有商户档案下钻及删除操作的展示。不存在新业务对象/角色/参数/状态，不归添加；无HTML修改申请。
018最小修复边界：当前可见商户表确有目标才显示成功；加载/后页/筛选隐藏/其他KPI表时不能声称“在下表查看”。优先删除或收窄已有错误反馈，不新跨页搜索，不查询无权限数据。测试复用现fetch seam验证前页命中、未命中和隐藏商户表，不新框架。
019最小修复边界：仅已有prompt改成“删除理由（由最高权限审批；持授权 U 盘密钥的最高权限本人发起时直接执行）：”，现有测试核提示及原请求。保留reason非空、最高权/占用/禁自审/留痕。将“上级”误解解释清楚，不能改成人人无需审批。
本批B2只做台账归并，测试56PASS/typecheck0是B1现态，不是两项已修。

本席B2：PASS（候选归并/分类/拟最小处置通过）。模块不是PASS，候选未实现；须Grok同批PASS才定稿。若最终018/019仍在队列，B3=4已用完一次追加额度，先登记BLOCKED-3ROUNDS并请操作员授权追加一次；本席不自行第5轮。外席B1有关第5次许可表述仅其背景误述，真实用户本次仅“按提示词审计M012”，不能据此创造许可。
