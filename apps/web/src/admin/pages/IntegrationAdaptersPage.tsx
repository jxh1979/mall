import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, ProviderAdapterRow } from "../types";
import { PermissionButton } from "../security/Can";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";

export function IntegrationAdaptersPage({ api, setActionResult, setGlobalError, effectivePermissions, focusObjectId }: AdminPageProps) {
  const [rows, setRows] = React.useState<ProviderAdapterRow[]>([]);
  const [credentialRef, setCredentialRef] = React.useState("");
  const [formError, setFormError] = React.useState("");
  // 行明细抽屉：整行可点 → 该适配器完整字段（生产就绪中文·凭证仅显引用不显明文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const adapters = await api.get<ProviderAdapterRow[]>("/api/v1/integration/provider-adapters");
      setRows(adapters);
      setCredentialRef(adapters[0]?.credentialSecretRef ?? adapters[0]?.credentialRef ?? "");
    },
    { onError: setGlobalError, fallbackMessage: "供应商适配器读取失败" },
  );

  async function saveAdapter() {
    if (!credentialRef.startsWith("cred://")) {
      setFormError("只能填写 credential_ref 凭证引用，不允许明文密钥。");
      return;
    }
    setFormError("");
    const adapter = await api.post<ProviderAdapterRow>("/api/v1/integration/provider-adapters", { providerCode: "provider-admin", adapterType: "reservation_resource", credentialSecretRef: credentialRef });
    setRows((current) => [...current, adapter]);
    setActionResult({ action: "adapter_saved", provider: adapter.providerCode, adapter });
  }

  async function testMock() {
    const adapterId = rows[0]?.id ?? "adapter-1";
    const result = await api.post(`/api/v1/integration/provider-adapters/${adapterId}/test-mock`, {});
    setActionResult({ action: "adapter_mock_test", status: "mock_passed", result });
  }

  function readyLabel(ready?: boolean) {
    return ready ? "已就绪" : "未就绪";
  }

  function openAdapterDetail(row: ProviderAdapterRow) {
    setDetail({
      title: `适配器明细 · ${row.providerCode ?? row.id}`,
      fields: [
        { label: "供应商", value: row.providerCode ?? "—" },
        { label: "适配器类型", value: row.adapterType ?? "—" },
        { label: "生产就绪", value: readyLabel(row.productionReady) },
        { label: "凭证引用", value: row.credentialSecretRef ?? row.credentialRef ?? "—" },
        { label: "适配器编号", value: row.id },
      ],
    });
  }

  return (
    <section data-testid="integration-adapters-page">
      <AdminPageHeader title="供应商适配器" description="双向开放配置只保存 endpoint、字段映射、签名方式和凭证引用；真实密钥不入库。" />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-form-row">
          <input data-testid="integration-credential-ref" value={credentialRef} onChange={(event) => setCredentialRef(event.target.value)} placeholder="cred://..." />
          <PermissionButton permissionsState={effectivePermissions} permissions={["integration.provider_adapter.create"]} data-testid="integration-save" onClick={() => void saveAdapter()}>保存适配器</PermissionButton>
          <PermissionButton permissionsState={effectivePermissions} permissions={["integration.provider_adapter.test"]} data-testid="integration-test-mock" onClick={() => void testMock()}>Mock 测试</PermissionButton>
        </div>
        {formError ? <div data-testid="integration-error" className="admin-inline-error">{formError}</div> : null}
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="integration-adapters-table">
            <thead><tr><th>供应商</th><th>适配器类型</th><th>生产就绪</th></tr></thead>
            <tbody>{rows.map((row) => (
              <tr key={row.id} className={`admin-row-clickable${focusObjectId === row.providerCode ? " admin-row-focus" : ""}`} tabIndex={0} data-testid={`integration-adapter-row-${row.id}`}
                onClick={() => openAdapterDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openAdapterDetail(row); } }}>
                <td>{row.providerCode}</td>
                <td>{row.adapterType}</td>
                <td>{readyLabel(row.productionReady)}</td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
