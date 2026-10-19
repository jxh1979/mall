// 文件版本：V0.02
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { CnDatePicker } from "../components/CnDatePicker";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

// 开票清单（ADM-STL-002）—— 结算重构 PRD 附录 B.1 / 附录 D、财务底稿 23_开票清单模板。
// 系统只生成清单交财务：按开票主体分组、带税率与税额、可导出；不接税控盘、不自建开票引擎、不生成发票号。
// 金额铁律（业务 HTML 逐字）：开票金额 ≤ 已收 − 已退 − 已开；退款后先红冲再重开。
const BASE = "/api/v1/settlement/invoice-items";
const MANAGE = ["settlement.invoice.manage"];

type Item = {
  id: string; billId?: string | null; settlementItemId?: string | null; projectId?: string | null;
  periodStart: string; periodEnd: string; incomeType: string;
  subjectEntityId?: string | null; subjectName?: string | null;
  invoiceItemName: string; taxCategory?: string | null; evidenceRequired?: string | null;
  amountCent: number; vatRateBps: number; vatAmountCent: number;
  culturalFeeApplicable: boolean; culturalFeeRateBps: number; culturalFeeCent: number;
  status: string; invoiceNo?: string | null; remark?: string | null;
  // FS-022-R1：积分发行费月账单行的来源账单与购方（合同销售方）；结算单行为空。
  pointsBillId?: string | null; buyerName?: string | null;
};
type Subject = {
  subjectEntityId?: string | null; subjectName?: string | null;
  itemCount: number; amountCent: number; vatAmountCent: number; culturalFeeCent: number;
};
type Rule = {
  incomeType: string; revenueStream?: string | null; invoiceItemName: string;
  taxCategory?: string | null; evidenceRequired?: string | null; invoiceable: boolean;
};
type OverIssued = {
  billId: string; incomeType: string; settlementItemId?: string | null; subjectName?: string | null;
  dueAmountCent: number; openAmountCent: number; overIssuedCent: number;
};
type Summary = { subjects: Subject[]; rules: Rule[]; overIssued: OverIssued[] };
type Page<T> = { items: T[]; page: number; pageSize: number; total: number };

const STATUS_LABELS: Record<string, string> = {
  pending: "待开票", issuing: "开票中", issued: "已开票",
  reversed: "已红冲", voided: "已作废", failed: "开票失败",
};

// 收入类型 → 业务人员看得懂的说法（与底稿 23「业务模块」列一致）。
const INCOME_LABELS: Record<string, string> = {
  ticket: "平台通票 / 门票综合服务费",
  spv_service: "通票可分配池分账",
  nonstd_service: "非散台服务费",
  catering: "订台低消 / 二次消费 / 额外酒水",
  rent: "商业租赁 / 市集摊位",
  game: "游乐收入",
  show: "演出票",
  giveaway_transfer: "通票赠送权益兑付（内部转付）",
  marketing: "会员积分 / 门票折扣券（营销成本）",
  points_issuance: "积分发行费",
};

const yuan = (cent?: number | null) => ((cent ?? 0) / 100).toFixed(2);
const pct = (bps?: number | null) => `${((bps ?? 0) / 100).toFixed(2)}%`;
const label = (map: Record<string, string>, key: string) => map[key] ?? key;

export function SettlementInvoicePage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [month, setMonth] = React.useState("");
  const [statusFilter, setStatusFilter] = React.useState("");
  const [summary, setSummary] = React.useState<Summary | null>(null);
  const [items, setItems] = React.useState<Item[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [billId, setBillId] = React.useState("");
  const [busy, setBusy] = React.useState(false);

  // 结算月 YYYY-MM → 期间起日 YYYY-MM-01（清单按结算单期间落库，起日即结算月）。
  const periodStart = month ? `${month}-01` : "";

  const reload = React.useCallback(() => {
    setLoading(true);
    setError(null);
    const q = new URLSearchParams();
    if (periodStart) q.set("periodStart", periodStart);
    const listQuery = new URLSearchParams(q);
    if (statusFilter) listQuery.set("status", statusFilter);
    listQuery.set("size", "200");
    Promise.all([
      api.get<Summary>(`${BASE}/summary?${q.toString()}`),
      api.get<Page<Item>>(`${BASE}?${listQuery.toString()}`),
    ])
      .then(([s, p]) => {
        setSummary(s);
        setItems(p.items ?? []);
        setLoading(false);
      })
      .catch((e: unknown) => {
        setError((e as Error)?.message ?? "开票清单读取失败");
        setLoading(false);
      });
  }, [api, periodStart, statusFilter]);

  React.useEffect(() => { reload(); }, [reload]);

  const runAction = async (fn: () => Promise<unknown>, okText: string) => {
    setBusy(true);
    try {
      await fn();
      setActionResult({ ok: true, message: okText });
      reload();
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : "操作失败");
    } finally {
      setBusy(false);
    }
  };

  const generate = () => {
    if (!billId.trim()) {
      setGlobalError("请先填写要生成开票清单的结算单编号");
      return;
    }
    return runAction(
      async () => {
        const created = await api.post<number>(`${BASE}/generate?billId=${encodeURIComponent(billId.trim())}`, {});
        setActionResult({ ok: true, message: `已生成 ${created ?? 0} 条开票项（已开满的不重复生成）` });
      },
      "已生成开票清单",
    );
  };

  const changeStatus = (item: Item, status: string, prompt: string) => {
    const remark = window.prompt(prompt, "");
    if (remark === null) return;
    return runAction(
      () => api.post(`${BASE}/${encodeURIComponent(item.id)}/status`, { status, remark: remark || null }),
      `已置为「${label(STATUS_LABELS, status)}」`,
    );
  };

  const markIssued = (item: Item) => {
    const invoiceNo = window.prompt("填写税务／电子发票平台开出的发票号（本系统不生成发票号）", "");
    if (invoiceNo === null) return;
    return runAction(
      () => api.post(`${BASE}/${encodeURIComponent(item.id)}/status`, { status: "issued", invoiceNo: invoiceNo || null }),
      "已登记为「已开票」",
    );
  };

  const exportCsv = () => {
    const header = [
      "结算月", "开票主体", "开票项目", "税目口径", "收入类型",
      "开票金额(元)", "增值税率", "税额(元)", "文化事业建设费(元)", "凭证要件", "开票状态", "发票号", "来源单号", "购方",
    ].join(",");
    const lines = items.map((i) => [
      i.periodStart ?? "", i.subjectName ?? "（归属未配置）", i.invoiceItemName, i.taxCategory ?? "",
      label(INCOME_LABELS, i.incomeType), yuan(i.amountCent), pct(i.vatRateBps), yuan(i.vatAmountCent),
      i.culturalFeeApplicable ? yuan(i.culturalFeeCent) : "不计", i.evidenceRequired ?? "",
      label(STATUS_LABELS, i.status), i.invoiceNo ?? "", i.billId ?? i.pointsBillId ?? "", i.buyerName ?? "",
    ].map((cell) => `"${String(cell).replaceAll('"', '""')}"`).join(","));
    // BOM 前缀：Excel 直接双击打开不乱码。
    const blob = new Blob(["﻿" + [header, ...lines].join("\n")], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `开票清单-${month || "全部结算月"}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  };

  const grouped = React.useMemo(() => {
    const map = new Map<string, Item[]>();
    for (const item of items) {
      const key = item.subjectName ?? "（归属未配置）";
      const list = map.get(key);
      if (list) list.push(item); else map.set(key, [item]);
    }
    return [...map.entries()];
  }, [items]);

  return (
    <section data-testid="settlement-invoice-page">
      <AdminPageHeader
        title="开票清单"
        description="按结算单出到「开票主体 × 开票项目 × 税率 × 税额 × 凭证要件」粒度，财务拿来直接开票，不用再手工拆。开票动作由税务／电子发票第三方平台完成，本系统只出清单与留痕、不生成发票号。开票金额不会超过「已收 − 已退 − 已开」；退款后须先红冲、再重开。"
      />

      <div className="admin-toolbar" data-testid="settlement-invoice-toolbar">
        <label>
          结算月
          <CnDatePicker mode="month" value={month} onChange={setMonth} data-testid="settlement-invoice-month"
            aria-label="结算月" placeholder="全部结算月" />
        </label>
        <label>
          开票状态
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}
            data-testid="settlement-invoice-status-filter" aria-label="开票状态">
            <option value="">全部状态</option>
            {Object.entries(STATUS_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
          </select>
        </label>
        <label>
          结算单编号
          <input value={billId} onChange={(e) => setBillId(e.target.value)} placeholder="填结算单编号后生成清单"
            data-testid="settlement-invoice-bill-input" aria-label="结算单编号" />
        </label>
        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} onClick={generate} disabled={busy}
          data-testid="settlement-invoice-generate">按结算单生成开票清单</PermissionButton>
        <button type="button" onClick={exportCsv} disabled={items.length === 0}
          data-testid="settlement-invoice-export">导出清单（CSV）</button>
      </div>

      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        {summary && summary.overIssued.length > 0 ? (
          <div className="admin-inline-error" data-testid="settlement-invoice-over-issued">
            <strong>已超开，需先红冲再重开：</strong>
            <ul>
              {summary.overIssued.map((o) => (
                <li key={`${o.billId}-${o.incomeType}-${o.settlementItemId ?? "bill"}`}>
                  {o.subjectName ?? "（归属未配置）"} · {label(INCOME_LABELS, o.incomeType)}：
                  应开 {yuan(o.dueAmountCent)} 元、已开 {yuan(o.openAmountCent)} 元，
                  超出 {yuan(o.overIssuedCent)} 元（结算单退款后金额下调所致）。
                </li>
              ))}
            </ul>
          </div>
        ) : null}

        <h3>按开票主体汇总</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="settlement-invoice-subject-table">
            <thead>
              <tr>
                <th>开票主体</th><th>开票项数</th><th>开票金额(元)</th>
                <th>其中税额(元)</th><th>文化事业建设费(元)</th>
              </tr>
            </thead>
            <tbody>
              {(summary?.subjects ?? []).map((s) => (
                <tr key={s.subjectEntityId ?? "unassigned"}>
                  <td>{s.subjectName ?? "（归属未配置，请到结算配置中心配收入流业态归因）"}</td>
                  <td>{s.itemCount}</td>
                  <td>{yuan(s.amountCent)}</td>
                  <td>{yuan(s.vatAmountCent)}</td>
                  <td>{yuan(s.culturalFeeCent)}</td>
                </tr>
              ))}
              {(summary?.subjects ?? []).length === 0 ? (
                <tr><td colSpan={5}>该结算月还没有开票清单，填结算单编号后点「按结算单生成开票清单」。</td></tr>
              ) : null}
            </tbody>
          </table>
        </div>

        <h3>开票明细（按主体分组）</h3>
        {grouped.map(([subjectName, rows]) => (
          <div key={subjectName} className="admin-table-scroll">
            <h4>{subjectName}</h4>
            <table className="admin-table" data-testid="settlement-invoice-item-table">
              <thead>
                <tr>
                  <th>结算月</th><th>开票项目</th><th>收入类型</th><th>开票金额(元)</th>
                  <th>税率</th><th>税额(元)</th><th>文建费(元)</th><th>凭证要件</th>
                  <th>开票状态</th><th>发票号</th><th>购方</th><th>操作</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((i) => (
                  <tr key={i.id}>
                    <td>{i.periodStart}</td>
                    <td>{i.invoiceItemName}</td>
                    <td>{label(INCOME_LABELS, i.incomeType)}</td>
                    <td>{yuan(i.amountCent)}</td>
                    <td>{pct(i.vatRateBps)}</td>
                    <td>{yuan(i.vatAmountCent)}</td>
                    <td>{i.culturalFeeApplicable ? yuan(i.culturalFeeCent) : "不计"}</td>
                    <td>{i.evidenceRequired}</td>
                    <td>{label(STATUS_LABELS, i.status)}</td>
                    <td>{i.invoiceNo ?? "—"}</td>
                    <td>{i.buyerName ?? "—"}</td>
                    <td>
                      {i.status === "pending" || i.status === "issuing" ? (
                        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} onClick={() => markIssued(i)} disabled={busy}>
                          登记已开票
                        </PermissionButton>
                      ) : null}
                      {i.status === "issued" ? (
                        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} disabled={busy}
                          onClick={() => changeStatus(i, "reversed", "填写红冲理由（如：客人退款）")}>
                          红冲
                        </PermissionButton>
                      ) : null}
                      {i.status === "pending" || i.status === "failed" ? (
                        <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} disabled={busy}
                          onClick={() => changeStatus(i, "voided", "填写作废理由")}>
                          作废
                        </PermissionButton>
                      ) : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}
        {items.length === 0 ? <p>该结算月没有符合筛选条件的开票项。</p> : null}

        <h3>开票主体与税目映射规则</h3>
        <p>
          清单里每一项的开票主体、发票项目名称、凭证要件按下表规则生成；税率取自结算配置中心的税务配置（后台可改）。
          标「不开票」的两类为内部单据：通票赠送权益兑付是娱乐平台公司与提供商户之间的内部成本转付，
          会员积分／门票折扣券是营销成本，两者都不进对外开票清单。
        </p>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="settlement-invoice-rule-table">
            <thead>
              <tr><th>收入类型</th><th>发票项目名称</th><th>税目口径</th><th>开票主体来源</th><th>凭证要件</th><th>是否对外开票</th></tr>
            </thead>
            <tbody>
              {(summary?.rules ?? []).map((r) => (
                <tr key={r.incomeType}>
                  <td>{label(INCOME_LABELS, r.incomeType)}</td>
                  <td>{r.invoiceItemName}</td>
                  <td>{r.taxCategory}</td>
                  <td>{r.incomeType === "points_issuance" ? "积分月账单收款方（购方为合同销售方）"
                    : r.revenueStream ? "按收入流业态归因解析" : "按该笔分账的商户收款主体"}</td>
                  <td>{r.evidenceRequired || "—"}</td>
                  <td>{r.invoiceable ? "对外开票" : "不开票（内部单据）"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
