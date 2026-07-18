import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, ProviderAdapterRow } from "../types";
import { PermissionButton } from "../security/Can";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";

export function IntegrationAdaptersPage({ api, setActionResult, setGlobalError, effectivePermissions, focusObjectId }: AdminPageProps) {
  const [rows, setRows] = React.useState<ProviderAdapterRow[]>([]);
  const [credentialRef, setCredentialRef] = React.useState("");
  const [formError, setFormError] = React.useState("");
  // 引导式流程 phase-6：新增供应商适配器收起为主按钮 + 分步引导弹窗（Mock 测试是动作不动）。
  const [adapterOpen, setAdapterOpen] = React.useState(false);
  const [adapterSubmitting, setAdapterSubmitting] = React.useState(false);
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

  // 凭证校验（步内 + 提交双保险）：只允许 cred:// 凭证引用，不允许明文密钥。
  function credentialInvalid() {
    return !credentialRef.startsWith("cred://");
  }

  async function saveAdapter() {
    if (credentialInvalid()) {
      setFormError("只能填写 credential_ref 凭证引用，不允许明文密钥。");
      return;
    }
    setFormError("");
    setAdapterSubmitting(true);
    try {
      const adapter = await api.post<ProviderAdapterRow>("/api/v1/integration/provider-adapters", { providerCode: "provider-admin", adapterType: "reservation_resource", credentialSecretRef: credentialRef });
      setRows((current) => [...current, adapter]);
      setActionResult({ action: "adapter_saved", provider: adapter.providerCode, adapter });
      setAdapterOpen(false);
    } catch (actionError) {
      const message = actionError instanceof Error ? actionError.message : "新增供应商适配器失败";
      setFormError(message);
      setGlobalError(message);
    } finally {
      setAdapterSubmitting(false);
    }
  }

  async function testMock() {
    const adapterId = rows[0]?.id ?? "adapter-1";
    const result = await api.post(`/api/v1/integration/provider-adapters/${adapterId}/test-mock`, {});
    setActionResult({ action: "adapter_mock_test", status: "mock_passed", result });
  }

  // 适配器类型（后端固定枚举，DB CHECK 约束）→ 中文业务名，展示层翻译，接口仍用原码。
  const ADAPTER_TYPE_LABELS: Record<string, string> = {
    payment: "支付",
    ticket_channel: "票务渠道",
    face_age: "人脸年龄核验",
    wecom: "企业微信",
    robot: "机器人",
    video: "视频",
    device: "设备",
    retail: "零售",
    catering: "餐饮",
    reservation_resource: "预定资源",
  };
  function adapterTypeLabel(code?: string) {
    if (!code) return "—";
    return ADAPTER_TYPE_LABELS[code] ?? code;
  }

  function readyLabel(ready?: boolean) {
    return ready ? "已就绪" : "未就绪";
  }

  function openAdapterDetail(row: ProviderAdapterRow) {
    setDetail({
      title: `适配器明细 · ${row.providerCode ?? row.id}`,
      fields: [
        { label: "供应商", value: row.providerCode ?? "—" },
        { label: "适配器类型", value: adapterTypeLabel(row.adapterType) },
        { label: "生产就绪", value: readyLabel(row.productionReady) },
        { label: "凭证引用", value: row.credentialSecretRef ?? row.credentialRef ?? "—" },
        { label: "适配器编号", value: row.id },
      ],
    });
  }

  return (
    <section data-testid="integration-adapters-page">
      <AdminPageHeader title="供应商适配器" description="双向开放配置只保存 endpoint、字段映射、签名方式和凭证引用；真实密钥不入库。" />
      <div className="admin-form-row" data-testid="integration-adapter-form">
        <PermissionButton permissionsState={effectivePermissions} permissions={["integration.provider_adapter.create"]} data-testid="integration-create-open" onClick={() => { setFormError(""); setAdapterOpen(true); }}>＋ 新增供应商适配器</PermissionButton>
        <PermissionButton permissionsState={effectivePermissions} permissions={["integration.provider_adapter.test"]} data-testid="integration-test-mock" onClick={() => void testMock()}>Mock 测试</PermissionButton>
        <span className="admin-muted-action">只填 cred:// 凭证引用，真实密钥不入库。</span>
      </div>
      {formError && !adapterOpen ? <div data-testid="integration-error" className="admin-inline-error">{formError}</div> : null}
      <GuidedFlowModal
        open={adapterOpen}
        title="新增供应商适配器"
        testId="integration"
        submitTestId="integration-save"
        submitLabel="保存适配器"
        submitting={adapterSubmitting}
        error={formError || undefined}
        onClose={() => setAdapterOpen(false)}
        onSubmit={() => void saveAdapter()}
        steps={[
          {
            title: "凭证引用",
            description: "填写供应商凭证引用（cred://...），真实密钥不入库。",
            validate: () => (credentialInvalid() ? "只能填写 credential_ref 凭证引用，不允许明文密钥。" : null),
            content: (
              <>
                <label>凭证引用<input data-testid="integration-credential-ref" value={credentialRef} onChange={(event) => setCredentialRef(event.target.value)} placeholder="cred://..." /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "供应商", value: "provider-admin" },
          { label: "适配器类型", value: "预定资源" },
          { label: "凭证引用", value: credentialRef.trim() || "未填写" },
        ]}
      />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="integration-adapters-table">
            <thead><tr><th>供应商</th><th>适配器类型</th><th>生产就绪</th><th>详情</th></tr></thead>
            <tbody>{rows.map((row) => (
              <tr key={row.id} className={`admin-row-clickable${focusObjectId === row.providerCode ? " admin-row-focus" : ""}`} tabIndex={0} data-testid={`integration-adapter-row-${row.id}`}
                onClick={() => openAdapterDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openAdapterDetail(row); } }}>
                <td>{row.providerCode}</td>
                <td title={row.adapterType ?? ""}>{adapterTypeLabel(row.adapterType)}</td>
                <td>{readyLabel(row.productionReady)}</td>
              <td className="admin-row-detail-cell"><button type="button" className="admin-row-detail-btn" aria-label="查看适配器详情" onClick={(event) => { event.stopPropagation(); openAdapterDetail(row); }}>查看详情</button></td></tr>
            ))}</tbody>
          </table>
        </div>
      </AdminDataState>
      <RowDetailDrawer open={detail !== null} title={detail?.title ?? ""} fields={detail?.fields ?? []} onClose={() => setDetail(null)} />
    </section>
  );
}
