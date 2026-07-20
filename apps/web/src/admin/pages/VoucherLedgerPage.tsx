import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { AuditTrailHint } from "../components/AuditTrailHint";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { RowDetailDrawer, cnDate, type RowDetailField } from "../components/RowDetailDrawer";
import { TablePager } from "../components/TablePager";
import { PermissionButton } from "../security/Can";
import { useAdminAction } from "../hooks/useAdminAction";
import { useAdminResource } from "../hooks/useAdminResource";
import { useServerPagedRows } from "../hooks/useServerPagedRows";
import type { PagedResult } from "../client/adminClient";
import type {
  AdminPageProps,
  VoucherDefinitionRow,
  VoucherInstanceRow,
  VoucherOverviewRow,
} from "../types";

// 券台账管理（V301 统一台账·方案A）：一张台账收编积分兑换券＋营销赠券；
// 通票第三方合作券不搬表，"全部券"聚合视图读层并入。核销分三类：商户折扣（结账抵扣）/
// 店内实物（核销扣库存＋内部结算价挂应付）/外部伙伴（对账回流，仍在通票权益侧）。
const VIEW_PERMS = ["customer.voucher.view"];
const MANAGE_PERMS = ["customer.voucher.manage"];
const REDEEM_PERMS = ["customer.voucher.redeem"];
const PAGE_SIZE = 20;

const KIND_LABELS: Record<string, string> = {
  merchant_discount: "商户折扣券",
  in_store_goods: "店内实物券",
  external_partner: "外部伙伴券",
};

const MODE_LABELS: Record<string, string> = {
  none: "不结算",
  internal_price: "内部结算价挂应付",
  pass_settlement: "通票结算口径",
};

const METHOD_LABELS: Record<string, string> = {
  in_store_staff: "店员核销",
  onsite_code: "场内扫码",
  external_reflow: "外部回流",
};

const STATUS_LABELS: Record<string, string> = {
  issued: "已发放",
  redeemed: "已核销",
  expired: "已过期",
  revoked: "已作废",
  active: "生效中",
  used: "已用完",
};

const SOURCE_LABELS: Record<string, string> = {
  points_redemption: "积分兑换",
  marketing_grant: "营销赠发",
  unified: "统一台账",
  pass_partner: "通票第三方券",
};

const TEXT = {
  title: "券台账管理",
  description:
    "积分兑换券与营销赠券统一收进一张台账：发放、作废、核销、结算挂钩全程留痕。核销分三类口径——商户折扣券结账抵扣、店内实物券核销时扣库存并按内部结算价挂应付、外部伙伴券仍走通票权益对账回流；「全部券」视图聚合展示统一台账与通票第三方券两个来源。",
  loadFailed: "券台账读取失败",
  createDefFailed: "新建券定义失败",
  issueFailed: "发放券失败",
  revokeFailed: "作废券失败",
  redeemFailed: "核销失败",
  nameRequired: "请填写券名称。",
  faceValueRequired: "商户折扣券必须填写面额（元，大于 0）。",
  merchantRequired: "该券类型/结算口径必须选择归属商户。",
  goodsRequired: "店内实物券必须填写货品、库位与每张券件数。",
  internalPriceRequired: "内部结算必须填写内部结算价（元，大于 0）。",
  customerRequired: "请填写客人 ID。",
  definitionRequired: "请选择券定义。",
  quantityInvalid: "发放张数必须是 1-100 的整数。",
  reasonRequired: "作废理由必填（进入审计留痕）。",
};

function centToYuan(cent: number | undefined | null): string {
  if (cent === undefined || cent === null || Number.isNaN(Number(cent))) return "—";
  return new Intl.NumberFormat("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(cent) / 100);
}

function nextKey(prefix: string) {
  return `${prefix}-${Date.now()}`;
}

function makeResult(action: string, request: Record<string, unknown>, result: unknown) {
  return { action, idempotencyKey: request.idempotencyKey, result };
}

export function VoucherLedgerPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [definitions, setDefinitions] = React.useState<VoucherDefinitionRow[]>([]);
  // 视图切换：统一台账实例 / 全部券聚合（含通票第三方券，只读）。
  const [viewMode, setViewMode] = React.useState<"unified" | "all">("unified");
  const [statusFilter, setStatusFilter] = React.useState("");
  const [sourceFilter, setSourceFilter] = React.useState("");
  const [customerFilter, setCustomerFilter] = React.useState("");

  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  const [createDefOpen, setCreateDefOpen] = React.useState(false);
  const [issueOpen, setIssueOpen] = React.useState(false);
  const [revokeTarget, setRevokeTarget] = React.useState<VoucherInstanceRow | null>(null);
  const [redeemTarget, setRedeemTarget] = React.useState<VoucherInstanceRow | null>(null);

  // 新建券定义表单
  const [defName, setDefName] = React.useState("");
  const [defKind, setDefKind] = React.useState("merchant_discount");
  const [defMethod, setDefMethod] = React.useState("in_store_staff");
  const [defMode, setDefMode] = React.useState("none");
  const [defMerchant, setDefMerchant] = React.useState("");
  const [defFaceYuan, setDefFaceYuan] = React.useState("");
  const [defItemId, setDefItemId] = React.useState("");
  const [defLocationId, setDefLocationId] = React.useState("");
  const [defStockQty, setDefStockQty] = React.useState("1");
  const [defInternalYuan, setDefInternalYuan] = React.useState("");
  const [defValidDays, setDefValidDays] = React.useState("");
  // V313 券种级"可转让"开关，默认开（可转让）；关闭则该券种发出的券不可会员间转赠。
  const [defTransferable, setDefTransferable] = React.useState(true);
  // 发放表单
  const [issueDefinition, setIssueDefinition] = React.useState("");
  const [issueCustomer, setIssueCustomer] = React.useState("");
  const [issueQuantity, setIssueQuantity] = React.useState("1");
  // 作废理由 / 核销备注
  const [revokeReason, setRevokeReason] = React.useState("");
  const [redeemNote, setRedeemNote] = React.useState("");

  const definitionsResource = useAdminResource(
    async () => {
      const page = await api.getPaged<VoucherDefinitionRow>("/api/v1/vouchers/definitions?page=1&page_size=200");
      setDefinitions(page.items);
    },
    { onError: setGlobalError, fallbackMessage: TEXT.loadFailed, onStart: () => setGlobalError(null) },
  );

  const fetchPage = React.useCallback(
    (page: number, size: number): Promise<PagedResult<VoucherInstanceRow | VoucherOverviewRow>> => {
      const params = new URLSearchParams({ page: String(page), page_size: String(size) });
      if (customerFilter.trim()) params.set("customer_id", customerFilter.trim());
      if (viewMode === "all") {
        return api.getPaged<VoucherOverviewRow>(`/api/v1/vouchers/overview?${params.toString()}`);
      }
      if (statusFilter) params.set("status", statusFilter);
      if (sourceFilter) params.set("source_type", sourceFilter);
      return api.getPaged<VoucherInstanceRow>(`/api/v1/vouchers/instances?${params.toString()}`);
    },
    [api, viewMode, statusFilter, sourceFilter, customerFilter],
  );

  const rows = useServerPagedRows<VoucherInstanceRow | VoucherOverviewRow>(fetchPage, PAGE_SIZE, {
    onError: setGlobalError,
    fallbackMessage: TEXT.loadFailed,
  });

  React.useEffect(() => {
    rows.reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [viewMode, statusFilter, sourceFilter, customerFilter]);

  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload: async () => {
      rows.reload();
      await definitionsResource.reload();
    },
    setGlobalError,
    setActionResult,
    makeResult,
    fallbackMessage: TEXT.loadFailed,
  });

  // ── 新建券定义 ─────────────────────────────────────────────

  function openCreateDef() {
    setFormError("");
    setDefName("");
    setDefKind("merchant_discount");
    setDefMethod("in_store_staff");
    setDefMode("none");
    setDefMerchant("");
    setDefFaceYuan("");
    setDefItemId("");
    setDefLocationId("");
    setDefStockQty("1");
    setDefInternalYuan("");
    setDefValidDays("");
    setCreateDefOpen(true);
  }

  function validateDefBasics(): string | null {
    if (!defName.trim()) return TEXT.nameRequired;
    if (defKind === "merchant_discount") {
      const face = Number(defFaceYuan);
      if (!Number.isFinite(face) || face <= 0) return TEXT.faceValueRequired;
      if (!defMerchant.trim()) return TEXT.merchantRequired;
    }
    if (defKind === "in_store_goods") {
      if (!defItemId.trim() || !defLocationId.trim()) return TEXT.goodsRequired;
      const qty = Number(defStockQty);
      if (!Number.isInteger(qty) || qty <= 0) return TEXT.goodsRequired;
    }
    if (defMode === "internal_price") {
      const price = Number(defInternalYuan);
      if (!Number.isFinite(price) || price <= 0) return TEXT.internalPriceRequired;
      if (!defMerchant.trim()) return TEXT.merchantRequired;
    }
    return null;
  }

  async function submitCreateDef() {
    const invalid = validateDefBasics();
    if (invalid) {
      setFormError(invalid);
      return;
    }
    const request: Record<string, unknown> = {
      idempotencyKey: nextKey("voucher-def"),
      name: defName.trim(),
      voucherKind: defKind,
      redeemMethod: defMethod,
      settlementMode: defMode,
      merchantId: defMerchant.trim() || null,
      faceValueCent: defKind === "merchant_discount" ? Math.round(Number(defFaceYuan) * 100) : null,
      inventoryItemId: defKind === "in_store_goods" ? defItemId.trim() : null,
      stockLocationId: defKind === "in_store_goods" ? defLocationId.trim() : null,
      stockQuantity: defKind === "in_store_goods" ? Number(defStockQty) : null,
      internalPriceCent: defMode === "internal_price" ? Math.round(Number(defInternalYuan) * 100) : null,
      validDays: defValidDays.trim() ? Number(defValidDays) : null,
      validUntil: null,
      transferable: defTransferable,
    };
    const ok = await runAction("voucher_definition_created", request,
      () => api.post("/api/v1/vouchers/definitions", request));
    if (ok) {
      setCreateDefOpen(false);
    }
  }

  // ── 发放（营销赠券）────────────────────────────────────────

  function openIssue() {
    setFormError("");
    setIssueDefinition(definitions[0]?.id ?? "");
    setIssueCustomer(customerFilter.trim());
    setIssueQuantity("1");
    setIssueOpen(true);
  }

  function validateIssue(): string | null {
    if (!issueDefinition) return TEXT.definitionRequired;
    if (!issueCustomer.trim()) return TEXT.customerRequired;
    const qty = Number(issueQuantity);
    if (!Number.isInteger(qty) || qty <= 0 || qty > 100) return TEXT.quantityInvalid;
    return null;
  }

  async function submitIssue() {
    const invalid = validateIssue();
    if (invalid) {
      setFormError(invalid);
      return;
    }
    const request = {
      idempotencyKey: nextKey("voucher-issue"),
      definitionId: issueDefinition,
      customerId: issueCustomer.trim(),
      quantity: Number(issueQuantity),
    };
    const ok = await runAction("voucher_issued", request,
      () => api.post("/api/v1/vouchers/instances", request));
    if (ok) {
      setIssueOpen(false);
    }
  }

  // ── 作废 / 核销 ────────────────────────────────────────────

  function openRevoke(row: VoucherInstanceRow) {
    setFormError("");
    setRevokeReason("");
    setRevokeTarget(row);
  }

  async function submitRevoke() {
    if (!revokeTarget) return;
    if (!revokeReason.trim()) {
      setFormError(TEXT.reasonRequired);
      return;
    }
    const request = { idempotencyKey: nextKey("voucher-revoke"), reason: revokeReason.trim() };
    const ok = await runAction("voucher_revoked", request,
      () => api.post(`/api/v1/vouchers/instances/${encodeURIComponent(revokeTarget.id)}/revoke`, request));
    if (ok) {
      setRevokeTarget(null);
    }
  }

  function openRedeem(row: VoucherInstanceRow) {
    setFormError("");
    setRedeemNote("");
    setRedeemTarget(row);
  }

  async function submitRedeem() {
    if (!redeemTarget) return;
    const request: Record<string, unknown> = {
      idempotencyKey: nextKey("voucher-redeem"),
      voucherInstanceId: redeemTarget.id,
      note: redeemNote.trim() || null,
    };
    const ok = await runAction("voucher_redeemed", request,
      () => api.post("/api/v1/vouchers/redemptions", request));
    if (ok) {
      setRedeemTarget(null);
    }
  }

  // ── 详情 ───────────────────────────────────────────────────

  function definitionName(definitionId: string | undefined): string {
    return definitions.find((d) => d.id === definitionId)?.name ?? definitionId ?? "—";
  }

  function openInstanceDetail(row: VoucherInstanceRow) {
    setDetail({
      title: `券实例明细 · ${row.id}`,
      fields: [
        { label: "核销码", value: row.code ?? "—" },
        { label: "券定义", value: definitionName(row.definitionId) },
        { label: "持有客人", value: row.customerId ?? "—" },
        { label: "来源", value: SOURCE_LABELS[row.sourceType ?? ""] ?? row.sourceType ?? "—" },
        { label: "来源引用", value: row.sourceRef ?? "—" },
        { label: "状态", value: STATUS_LABELS[row.status ?? ""] ?? row.status ?? "—" },
        { label: "发放时间", value: cnDate(row.issuedAt) },
        { label: "有效期至", value: row.expiresAt ? cnDate(row.expiresAt) : "不过期" },
        { label: "核销时间", value: row.redeemedAt ? cnDate(row.redeemedAt) : "—" },
        { label: "作废理由", value: row.revokeReason ?? "—" },
        { label: "实例号", value: row.id },
      ],
    });
  }

  function openOverviewDetail(row: VoucherOverviewRow) {
    setDetail({
      title: `券明细 · ${row.id}`,
      fields: [
        { label: "来源", value: SOURCE_LABELS[row.source ?? ""] ?? row.source ?? "—" },
        { label: "券名称", value: row.name ?? "—" },
        { label: "券类型", value: KIND_LABELS[row.voucherKind ?? ""] ?? row.voucherKind ?? "—" },
        { label: "持有客人", value: row.customerId ?? "—" },
        { label: "状态", value: STATUS_LABELS[row.status ?? ""] ?? row.status ?? "—" },
        { label: "剩余次数", value: String(row.remainingTimes ?? "—") },
        { label: "有效期至", value: row.expiresAt ? cnDate(row.expiresAt) : "不过期" },
      ],
    });
  }

  const activeDefinitions = definitions.filter((d) => d.status === "active" || d.status === undefined);
  const empty = !rows.loading && rows.total === 0 && !statusFilter && !sourceFilter && !customerFilter;
  const isUnified = viewMode === "unified";

  return (
    <section data-testid="voucher-ledger-page">
      <AdminPageHeader title={TEXT.title} description={TEXT.description} />
      <AuditTrailHint event="voucher.definition_created / voucher.issued / voucher.revoked / voucher.redeemed" />

      {/* ── 筛选 + 动作 ─────────────────────────────────────── */}
      <div className="admin-form-row" data-testid="voucher-filter">
        <label>视图<select data-testid="voucher-filter-view" value={viewMode} onChange={(event) => setViewMode(event.target.value === "all" ? "all" : "unified")}>
          <option value="unified">统一台账券实例</option>
          <option value="all">全部券（含通票第三方券）</option>
        </select></label>
        {isUnified ? (
          <>
            <label>状态<select data-testid="voucher-filter-status" value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
              <option value="">全部状态</option>
              {["issued", "redeemed", "expired", "revoked"].map((value) => <option key={value} value={value}>{STATUS_LABELS[value]}</option>)}
            </select></label>
            <label>来源<select data-testid="voucher-filter-source" value={sourceFilter} onChange={(event) => setSourceFilter(event.target.value)}>
              <option value="">全部来源</option>
              <option value="points_redemption">积分兑换</option>
              <option value="marketing_grant">营销赠发</option>
            </select></label>
          </>
        ) : null}
        <label>客人 ID<input data-testid="voucher-filter-customer" value={customerFilter} onChange={(event) => setCustomerFilter(event.target.value)} placeholder="按持有客人筛选" /></label>
        <PermissionButton
          permissionsState={effectivePermissions}
          permissions={MANAGE_PERMS}
          data-testid="voucher-create-def-open"
          onClick={openCreateDef}
        >＋新建券定义</PermissionButton>
        <PermissionButton
          permissionsState={effectivePermissions}
          permissions={MANAGE_PERMS}
          data-testid="voucher-issue-open"
          onClick={openIssue}
        >＋发放券</PermissionButton>
      </div>

      {/* ── 券定义 ───────────────────────────────────────────── */}
      <h3>券定义（模板）</h3>
      <div className="admin-table-scroll">
        <table className="admin-table" data-testid="voucher-definition-table">
          <thead><tr><th>名称</th><th>类型</th><th>核销方式</th><th>结算口径</th><th>归属商户</th><th>面额/内部价（元）</th><th>有效期</th><th>状态</th></tr></thead>
          <tbody>
            {definitions.length === 0 ? (
              <tr data-testid="voucher-definition-table-empty"><td colSpan={8} className="admin-muted-action">暂无券定义。积分兑换目录未挂接时会自动派生实物券模板。</td></tr>
            ) : definitions.map((definition) => (
              <tr key={definition.id} data-testid={`voucher-definition-row-${definition.id}`}>
                <td>{definition.name ?? "—"}</td>
                <td>{KIND_LABELS[definition.voucherKind ?? ""] ?? definition.voucherKind ?? "—"}</td>
                <td>{METHOD_LABELS[definition.redeemMethod ?? ""] ?? definition.redeemMethod ?? "—"}</td>
                <td>{MODE_LABELS[definition.settlementMode ?? ""] ?? definition.settlementMode ?? "—"}</td>
                <td>{definition.merchantId ?? "平台通用"}</td>
                <td className="admin-num">{definition.faceValueCent != null ? `¥${centToYuan(definition.faceValueCent)}` : definition.internalPriceCent != null ? `内部价 ¥${centToYuan(definition.internalPriceCent)}` : "—"}</td>
                <td>{definition.validDays != null ? `发放起 ${definition.validDays} 天` : definition.validUntil ? `至 ${cnDate(definition.validUntil)}` : "不过期"}</td>
                <td>{definition.status === "active" ? "生效中" : "已停用"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* ── 券实例台账 / 全部券聚合 ──────────────────────────── */}
      <h3>{isUnified ? "券实例台账" : "全部券（统一台账 ∪ 通票第三方券，只读聚合）"}</h3>
      <AdminDataState loading={rows.loading || definitionsResource.loading} error={rows.error ?? definitionsResource.error} empty={empty} onRetry={rows.reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="voucher-instance-table">
            <thead>
              {isUnified ? (
                <tr><th>核销码</th><th>券定义</th><th>持有客人</th><th>来源</th><th>状态</th><th>有效期至</th><th>操作</th></tr>
              ) : (
                <tr><th>券号</th><th>券名称</th><th>类型</th><th>来源</th><th>持有客人</th><th>状态</th><th>剩余次数</th><th>有效期至</th></tr>
              )}
            </thead>
            <tbody>
              {rows.rows.length === 0 ? (
                <tr data-testid="voucher-instance-table-empty"><td colSpan={8} className="admin-muted-action">当前筛选下暂无券。</td></tr>
              ) : isUnified ? (rows.rows as VoucherInstanceRow[]).map((row) => (
                <tr key={row.id} className="admin-row-clickable" tabIndex={0} data-testid={`voucher-instance-row-${row.id}`}
                  onClick={() => openInstanceDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openInstanceDetail(row); } }}>
                  <td>{row.code ?? "—"}</td>
                  <td>{definitionName(row.definitionId)}</td>
                  <td>{row.customerId ?? "—"}</td>
                  <td>{SOURCE_LABELS[row.sourceType ?? ""] ?? row.sourceType ?? "—"}</td>
                  <td>{STATUS_LABELS[row.status ?? ""] ?? row.status ?? "—"}</td>
                  <td>{row.expiresAt ? cnDate(row.expiresAt) : "不过期"}</td>
                  <td className="admin-row-detail-cell" onClick={(event) => event.stopPropagation()}>
                    <button type="button" className="admin-row-detail-btn" aria-label="查看券实例明细" onClick={(event) => { event.stopPropagation(); openInstanceDetail(row); }}>查看详情</button>
                    {row.status === "issued" ? (
                      <>
                        <PermissionButton
                          permissionsState={effectivePermissions}
                          permissions={REDEEM_PERMS}
                          data-testid={`voucher-redeem-open-${row.id}`}
                          onClick={() => openRedeem(row)}
                        >核销</PermissionButton>
                        <PermissionButton
                          permissionsState={effectivePermissions}
                          permissions={MANAGE_PERMS}
                          data-testid={`voucher-revoke-open-${row.id}`}
                          onClick={() => openRevoke(row)}
                        >作废</PermissionButton>
                      </>
                    ) : null}
                  </td>
                </tr>
              )) : (rows.rows as VoucherOverviewRow[]).map((row) => (
                <tr key={`${row.source}-${row.id}`} className="admin-row-clickable" tabIndex={0} data-testid={`voucher-overview-row-${row.id}`}
                  onClick={() => openOverviewDetail(row)}
                  onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openOverviewDetail(row); } }}>
                  <td>{row.id}</td>
                  <td>{row.name ?? "—"}</td>
                  <td>{KIND_LABELS[row.voucherKind ?? ""] ?? row.voucherKind ?? "—"}</td>
                  <td>{SOURCE_LABELS[row.source ?? ""] ?? row.source ?? "—"}</td>
                  <td>{row.customerId ?? "—"}</td>
                  <td>{STATUS_LABELS[row.status ?? ""] ?? row.status ?? "—"}</td>
                  <td className="admin-num">{row.remainingTimes ?? "—"}</td>
                  <td>{row.expiresAt ? cnDate(row.expiresAt) : "不过期"}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <TablePager testid="voucher-pager" page={rows.page} pageCount={rows.pageCount} total={rows.total} onPrev={rows.prev} onNext={rows.next} />
        </div>
      </AdminDataState>

      {formError && !createDefOpen && !issueOpen && !revokeTarget && !redeemTarget ? (
        <div className="admin-form-error" data-testid="voucher-form-error">{formError}</div>
      ) : null}
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />

      {/* ── 新建券定义（引导式）───────────────────────────────── */}
      <GuidedFlowModal
        open={createDefOpen}
        title="新建券定义"
        testId="voucher-create-def-guided"
        submitTestId="voucher-create-def-save"
        submitLabel="确认创建"
        submitting={busyAction === "voucher_definition_created"}
        error={formError || undefined}
        onClose={() => setCreateDefOpen(false)}
        onSubmit={submitCreateDef}
        steps={[
          {
            title: "券类型与参数",
            description: "商户折扣券＝结账抵扣（须填面额与归属商户）；店内实物券＝核销时扣库存（须填货品/库位/件数），选内部结算时按内部结算价给商户挂应付。外部伙伴券请在通票权益模板上配置（第三方券），此处不建。",
            validate: validateDefBasics,
            content: (
              <>
                <label>券名称<input data-testid="voucher-create-def-name" value={defName} onChange={(event) => setDefName(event.target.value)} placeholder="如：满 100 减 50 券" /></label>
                <label>券类型<select data-testid="voucher-create-def-kind" value={defKind} onChange={(event) => setDefKind(event.target.value)}>
                  <option value="merchant_discount">商户折扣券（结账抵扣）</option>
                  <option value="in_store_goods">店内实物券（核销扣库存）</option>
                </select></label>
                <label>核销方式<select data-testid="voucher-create-def-method" value={defMethod} onChange={(event) => setDefMethod(event.target.value)}>
                  <option value="in_store_staff">店员核销</option>
                  <option value="onsite_code">场内扫码</option>
                </select></label>
                <label>结算口径<select data-testid="voucher-create-def-mode" value={defMode} onChange={(event) => setDefMode(event.target.value)}>
                  <option value="none">不结算（免费营销）</option>
                  <option value="internal_price">内部结算价挂应付</option>
                </select></label>
                <label>归属商户 ID<input data-testid="voucher-create-def-merchant" value={defMerchant} onChange={(event) => setDefMerchant(event.target.value)} placeholder="留空＝平台通用（折扣券/内部结算必填）" /></label>
                {defKind === "merchant_discount" ? (
                  <label>面额（元）<input data-testid="voucher-create-def-face" type="number" step="0.01" value={defFaceYuan} onChange={(event) => setDefFaceYuan(event.target.value)} placeholder="如 50.00" /></label>
                ) : (
                  <>
                    <label>货品 ID<input data-testid="voucher-create-def-item" value={defItemId} onChange={(event) => setDefItemId(event.target.value)} placeholder="库存货品 ID" /></label>
                    <label>库位 ID<input data-testid="voucher-create-def-location" value={defLocationId} onChange={(event) => setDefLocationId(event.target.value)} placeholder="出库库位 ID" /></label>
                    <label>每张券件数<input data-testid="voucher-create-def-qty" type="number" step="1" value={defStockQty} onChange={(event) => setDefStockQty(event.target.value)} /></label>
                  </>
                )}
                {defMode === "internal_price" ? (
                  <label>内部结算价（元）<input data-testid="voucher-create-def-internal" type="number" step="0.01" value={defInternalYuan} onChange={(event) => setDefInternalYuan(event.target.value)} placeholder="核销时按此价给商户挂应付" /></label>
                ) : null}
              </>
            ),
          },
          {
            title: "有效期",
            description: "自发放日起按天数计有效期；留空＝不过期。发出的券过期后不可核销（核销时惰性判定）。",
            validate: () => {
              if (defValidDays.trim() && (!Number.isInteger(Number(defValidDays)) || Number(defValidDays) <= 0)) {
                return "有效天数必须是正整数（或留空表示不过期）。";
              }
              return null;
            },
            content: (
              <label>有效天数<input data-testid="voucher-create-def-days" type="number" step="1" value={defValidDays} onChange={(event) => setDefValidDays(event.target.value)} placeholder="如 30；留空＝不过期" /></label>
            ),
          },
          {
            title: "可转让",
            description: "该券种下发的所有券是否可由持券客人转赠他人；默认允许，关闭后该券种发出的券不可转赠。",
            content: (
              <label className="admin-check-inline">
                <input
                  data-testid="voucher-create-def-transferable"
                  type="checkbox"
                  checked={defTransferable}
                  onChange={(event) => setDefTransferable(event.target.checked)}
                /> 允许持券客人转赠他人
              </label>
            ),
          },
        ]}
        summary={[
          { label: "券名称", value: defName.trim() || "未填写" },
          { label: "类型", value: KIND_LABELS[defKind] ?? defKind },
          { label: "核销方式", value: METHOD_LABELS[defMethod] ?? defMethod },
          { label: "结算口径", value: MODE_LABELS[defMode] ?? defMode },
          { label: "归属商户", value: defMerchant.trim() || "平台通用" },
          { label: defKind === "merchant_discount" ? "面额" : "货品/库位/件数", value: defKind === "merchant_discount" ? (defFaceYuan ? `¥${defFaceYuan}` : "未填写") : `${defItemId || "?"} / ${defLocationId || "?"} / ${defStockQty || "?"} 件` },
          { label: "内部结算价", value: defMode === "internal_price" ? (defInternalYuan ? `¥${defInternalYuan}` : "未填写") : "不适用" },
          { label: "有效期", value: defValidDays.trim() ? `发放起 ${defValidDays} 天` : "不过期" },
          { label: "可转让", value: defTransferable ? "允许转赠" : "不可转赠" },
        ]}
      />

      {/* ── 发放券（引导式）──────────────────────────────────── */}
      <GuidedFlowModal
        open={issueOpen}
        title="发放券（营销赠发）"
        testId="voucher-issue-guided"
        submitTestId="voucher-issue-save"
        submitLabel="确认发放"
        submitting={busyAction === "voucher_issued"}
        error={formError || undefined}
        onClose={() => setIssueOpen(false)}
        onSubmit={submitIssue}
        steps={[
          {
            title: "发放对象",
            description: "按券定义给指定客人发放营销赠券（来源记营销赠发）；积分兑换券由客人兑换自动发放，不在此操作。",
            validate: validateIssue,
            content: (
              <>
                <label>券定义<select data-testid="voucher-issue-definition" value={issueDefinition} onChange={(event) => setIssueDefinition(event.target.value)}>
                  {activeDefinitions.length === 0 ? <option value="">（暂无生效中的券定义）</option> : null}
                  {activeDefinitions.map((definition) => <option key={definition.id} value={definition.id}>{definition.name}</option>)}
                </select></label>
                <label>客人 ID<input data-testid="voucher-issue-customer" value={issueCustomer} onChange={(event) => setIssueCustomer(event.target.value)} placeholder="持有客人 ID" /></label>
                <label>发放张数<input data-testid="voucher-issue-quantity" type="number" step="1" value={issueQuantity} onChange={(event) => setIssueQuantity(event.target.value)} /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "券定义", value: activeDefinitions.find((d) => d.id === issueDefinition)?.name ?? (issueDefinition || "未选择") },
          { label: "客人", value: issueCustomer.trim() || "未填写" },
          { label: "张数", value: issueQuantity || "未填写" },
        ]}
      />

      {/* ── 作废（引导式）────────────────────────────────────── */}
      <GuidedFlowModal
        open={revokeTarget !== null}
        title={`作废券 · ${revokeTarget?.code ?? ""}`}
        testId="voucher-revoke-guided"
        submitTestId="voucher-revoke-save"
        submitLabel="确认作废"
        submitting={busyAction === "voucher_revoked"}
        error={formError || undefined}
        onClose={() => setRevokeTarget(null)}
        onSubmit={submitRevoke}
        steps={[
          {
            title: "作废理由（必填）",
            description: "仅未核销（已发放）的券可作废；作废后不可恢复、不可核销，理由与操作人进入审计留痕。",
            validate: () => (revokeReason.trim() ? null : TEXT.reasonRequired),
            content: (
              <label>理由<textarea data-testid="voucher-revoke-reason" value={revokeReason} onChange={(event) => setRevokeReason(event.target.value)} placeholder="如：活动提前下线，未核销券统一作废" /></label>
            ),
          },
        ]}
        summary={[
          { label: "券实例", value: revokeTarget?.id ?? "—" },
          { label: "核销码", value: revokeTarget?.code ?? "—" },
          { label: "持有客人", value: revokeTarget?.customerId ?? "—" },
          { label: "理由", value: revokeReason.trim() || "未填写" },
        ]}
      />

      {/* ── 核销（引导式）────────────────────────────────────── */}
      <GuidedFlowModal
        open={redeemTarget !== null}
        title={`核销券 · ${redeemTarget?.code ?? ""}`}
        testId="voucher-redeem-guided"
        submitTestId="voucher-redeem-save"
        submitLabel="确认核销"
        submitting={busyAction === "voucher_redeemed"}
        error={formError || undefined}
        onClose={() => setRedeemTarget(null)}
        onSubmit={submitRedeem}
        steps={[
          {
            title: "核销确认",
            description: "商户折扣券＝结账抵扣不结算；店内实物券＝核销即出库、按内部结算价挂应付（如该券定义启用内部结算）。每张券只能核销一次，商户账号只能核自家可核的券。",
            validate: () => null,
            content: (
              <label>备注（选填）<textarea data-testid="voucher-redeem-note" value={redeemNote} onChange={(event) => setRedeemNote(event.target.value)} placeholder="如：客人到店领取" /></label>
            ),
          },
        ]}
        summary={[
          { label: "券实例", value: redeemTarget?.id ?? "—" },
          { label: "核销码", value: redeemTarget?.code ?? "—" },
          { label: "券定义", value: definitionName(redeemTarget?.definitionId) },
          { label: "持有客人", value: redeemTarget?.customerId ?? "—" },
          { label: "备注", value: redeemNote.trim() || "无" },
        ]}
      />
    </section>
  );
}
