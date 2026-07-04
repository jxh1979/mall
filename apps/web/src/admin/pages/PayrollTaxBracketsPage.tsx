import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";

// 个税税表档位(payroll_tax_brackets, V217);客户端 unwrap 已 camel 化。
type TaxBracketRow = {
  id: string;
  category: string;
  seq: number;
  incomeFloorCent: number;
  incomeCeilingCent: number | null;
  incomeBaseBps: number;
  flatDeductionCent: number;
  rateBps: number;
  quickDeductionCent: number;
};

const MANAGE = ["staff.tax_bracket.manage"];
const yuan = (cent: number | null) => (cent == null ? "—" : (cent / 100).toFixed(2));
const pct = (bps: number) => (bps / 100).toFixed(2) + "%";

/**
 * 个税税表配置页(ADM-STF-003, V217):劳务报酬预扣个税税表分档维护——
 * 税额 = GREATEST(0,(毛收入*应税基数比 - 定额减除)*税率 - 速算扣除);税法参数变更改配置不改代码。
 * 高敏:仅 staff.tax_bracket.manage 可改(决定代扣个税口径)。
 */
export function PayrollTaxBracketsPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<TaxBracketRow[]>([]);
  const [form, setForm] = React.useState({
    id: "", seq: "", floor: "", ceiling: "", baseBps: "", flat: "", rateBps: "", quick: "",
  });

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<TaxBracketRow[]>("/api/v1/staff/payroll/tax-brackets");
      setRows((items ?? []).slice().sort((a, b) => a.seq - b.seq));
    },
    { onError: setGlobalError, fallbackMessage: "个税税表读取失败" },
  );

  async function saveBracket() {
    const seq = Number(form.seq);
    const floor = Number(form.floor);
    const baseBps = Number(form.baseBps);
    const rateBps = Number(form.rateBps);
    if (!form.id.trim() || !Number.isInteger(seq) || !Number.isFinite(floor) || floor < 0
      || !Number.isInteger(baseBps) || baseBps <= 0 || baseBps > 10000
      || !Number.isInteger(rateBps) || rateBps < 0 || rateBps > 10000) {
      setGlobalError("请填写档位 id/序号/毛收入下界(分)/应税基数万分比(1-10000)/税率万分比(0-10000)");
      return;
    }
    await api.post("/api/v1/staff/payroll/tax-brackets", {
      id: form.id.trim(),
      category: "labor_remuneration",
      seq,
      incomeFloorCent: floor,
      incomeCeilingCent: form.ceiling.trim() === "" ? null : Number(form.ceiling),
      incomeBaseBps: baseBps,
      flatDeductionCent: form.flat.trim() === "" ? 0 : Number(form.flat),
      rateBps,
      quickDeductionCent: form.quick.trim() === "" ? 0 : Number(form.quick),
    });
    setActionResult({ action: "payroll_tax_bracket_saved", id: form.id.trim(), seq });
    setForm({ id: "", seq: "", floor: "", ceiling: "", baseBps: "", flat: "", rateBps: "", quick: "" });
    reload();
  }

  return (
    <section data-testid="payroll-tax-brackets-page">
      <AdminPageHeader
        title="个税税表"
        description="维护劳务报酬预扣个税税表分档(按毛收入分段)。税额=(毛收入*应税基数比-定额减除)*税率-速算扣除,不为负。税法调整改配置不改代码;仅高敏权限可改(决定代扣个税口径)。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="payroll-tax-brackets-table">
            <thead>
              <tr>
                <th>档</th><th>毛收入下界(元)</th><th>上界(元)</th><th>应税基数比</th>
                <th>定额减除(元)</th><th>税率</th><th>速算扣除(元)</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.id} data-testid={`tax-bracket-${row.id}`}>
                  <td>{row.seq}</td>
                  <td>{yuan(row.incomeFloorCent)}</td>
                  <td>{yuan(row.incomeCeilingCent)}</td>
                  <td>{pct(row.incomeBaseBps)}</td>
                  <td>{yuan(row.flatDeductionCent)}</td>
                  <td>{pct(row.rateBps)}</td>
                  <td>{yuan(row.quickDeductionCent)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="payroll-tax-brackets-form">
          <input aria-label="档位 id" placeholder="档位 id" value={form.id} onChange={(e) => setForm({ ...form, id: e.target.value })} />
          <input aria-label="序号" type="number" placeholder="序号" value={form.seq} onChange={(e) => setForm({ ...form, seq: e.target.value })} />
          <input aria-label="毛收入下界(分)" type="number" placeholder="毛收入下界(分)" value={form.floor} onChange={(e) => setForm({ ...form, floor: e.target.value })} />
          <input aria-label="毛收入上界(分,空=无上界)" type="number" placeholder="上界(分,空=无上界)" value={form.ceiling} onChange={(e) => setForm({ ...form, ceiling: e.target.value })} />
          <input aria-label="应税基数万分比" type="number" placeholder="应税基数万分比(8000=80%)" value={form.baseBps} onChange={(e) => setForm({ ...form, baseBps: e.target.value })} />
          <input aria-label="定额减除(分)" type="number" placeholder="定额减除(分)" value={form.flat} onChange={(e) => setForm({ ...form, flat: e.target.value })} />
          <input aria-label="税率万分比" type="number" placeholder="税率万分比(2000=20%)" value={form.rateBps} onChange={(e) => setForm({ ...form, rateBps: e.target.value })} />
          <input aria-label="速算扣除(分)" type="number" placeholder="速算扣除(分)" value={form.quick} onChange={(e) => setForm({ ...form, quick: e.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid="payroll-tax-brackets-save" onClick={() => void saveBracket()}>
            保存档位
          </PermissionButton>
        </div>
      </AdminDataState>
    </section>
  );
}
