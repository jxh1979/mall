// 文件版本：V0.02
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, ProviderAdapterRow } from "../types";
import { PermissionButton } from "../security/Can";
import { RowDetailDrawer, type RowDetailField } from "../components/RowDetailDrawer";
import { GuidedFlowModal } from "../components/GuidedFlowModal";

// 模拟测试场景（后端 IntegrationValidation.TEST_SCENARIOS 闭集）→ 中文名，接口仍用原码。
const SCENARIO_LABELS: Record<string, string> = {
  auth: "鉴权",
  webhook: "回调推送",
  polling: "轮询",
  reconcile: "对账",
  field_mapping: "字段映射",
  failure_retry: "失败重试",
};

// 适配器类型：后端 IntegrationValidation.ADAPTER_TYPES 闭集（= OpenAPI ProviderAdapterRequest.adapter_type 枚举、DB CHECK）→ 中文业务名，接口仍用原码。
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
  parking: "停车系统",
};

// 接入模式：后端 IntegrationValidation.MODES 闭集（新增必填）→ 中文名，接口仍用原码。
const MODE_LABELS: Record<string, string> = {
  mock: "模拟",
  sandbox: "沙箱联调",
  production: "生产",
};

function adapterTypeLabel(code?: string) {
  if (!code) return "—";
  return ADAPTER_TYPE_LABELS[code] ?? code;
}

export function IntegrationAdaptersPage({ api, setActionResult, setGlobalError, effectivePermissions, focusObjectId }: AdminPageProps) {
  const [rows, setRows] = React.useState<ProviderAdapterRow[]>([]);
  // OBS-14-01：新增按后端 ProviderAdapterRequest 真实契约填写（供应商编码、类型、接入模式均必填；凭证引用不在新增请求里）。
  const [providerCode, setProviderCode] = React.useState("");
  const [adapterType, setAdapterType] = React.useState("");
  const [mode, setMode] = React.useState("");
  const [formError, setFormError] = React.useState("");
  // 引导式流程 phase-6：新增供应商适配器收起为主按钮 + 分步引导弹窗（模拟测试是动作不动）。
  const [adapterOpen, setAdapterOpen] = React.useState(false);
  const [adapterSubmitting, setAdapterSubmitting] = React.useState(false);
  // 行明细抽屉：整行可点 → 该适配器完整字段（生产就绪中文·凭证仅显引用不显明文）。
  const [detail, setDetail] = React.useState<{ title: string; fields: RowDetailField[] } | null>(null);
  // 模拟测试：所选适配器（未选默认第一行）与场景；结果按后端 test_status 显示，不在前端写死。
  const [testAdapterId, setTestAdapterId] = React.useState("");
  const [scenario, setScenario] = React.useState("auth");
  const testRow = rows.find((row) => row.id === testAdapterId) ?? rows[0];

  const { loading, error, reload } = useAdminResource(
    async () => {
      const adapters = await api.get<ProviderAdapterRow[]>("/api/v1/integration/provider-adapters");
      setRows(adapters);
    },
    { onError: setGlobalError, fallbackMessage: "供应商适配器读取失败" },
  );

  // 步内校验（中文原因）：三项均为后端必填。
  function adapterFormError() {
    if (!providerCode.trim()) return "请填写供应商编码";
    if (!adapterType) return "请选择适配器类型";
    if (!mode) return "请选择接入模式";
    return null;
  }

  async function saveAdapter() {
    setFormError("");
    setAdapterSubmitting(true);
    try {
      // 只发后端 ProviderAdapterRequest 接受的字段（幂等键由 AdminApi 自动补）。
      const adapter = await api.post<ProviderAdapterRow>("/api/v1/integration/provider-adapters", { providerCode: providerCode.trim(), adapterType, mode });
      setRows((current) => [...current, adapter]);
      setActionResult(`已保存供应商适配器：${adapter.providerCode}`);
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
    if (!testRow) return;
    try {
      const result = await api.post<{ testStatus?: string }>(`/api/v1/integration/provider-adapters/${encodeURIComponent(testRow.id)}/test-mock`, { providerCode: testRow.providerCode, scenario });
      setActionResult(`模拟测试${result.testStatus === "passed" ? "通过" : "未通过"}：${testRow.providerCode} · ${SCENARIO_LABELS[scenario]}`);
    } catch (testError) {
      setGlobalError(testError instanceof Error ? testError.message : "模拟测试失败");
    }
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
      <AdminPageHeader title="供应商适配器" description="双向开放配置只保存接口地址、字段映射、签名方式和凭证引用；真实密钥不入库。" />
      <div className="admin-form-row" data-testid="integration-adapter-form">
        <PermissionButton permissionsState={effectivePermissions} permissions={["integration.provider_adapter.create"]} data-testid="integration-create-open" onClick={() => { setFormError(""); setProviderCode(""); setAdapterType(""); setMode(""); setAdapterOpen(true); }}>＋ 新增供应商适配器</PermissionButton>
        <select data-testid="integration-test-adapter" aria-label="测试适配器" value={testRow?.id ?? ""} onChange={(event) => setTestAdapterId(event.target.value)}>
          {rows.map((row) => <option key={row.id} value={row.id}>{`${row.providerCode} · ${adapterTypeLabel(row.adapterType)}`}</option>)}
        </select>
        <select data-testid="integration-test-scenario" aria-label="测试场景" value={scenario} onChange={(event) => setScenario(event.target.value)}>
          {Object.entries(SCENARIO_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
        <PermissionButton permissionsState={effectivePermissions} permissions={["integration.provider_adapter.test"]} data-testid="integration-test-mock" disabled={!testRow} onClick={() => void testMock()}>模拟测试</PermissionButton>
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
            title: "适配器信息",
            validate: adapterFormError,
            content: (
              <>
                <label>供应商编码<input data-testid="integration-provider-code" value={providerCode} onChange={(event) => setProviderCode(event.target.value)} /></label>
                <label>适配器类型<select data-testid="integration-adapter-type" value={adapterType} onChange={(event) => setAdapterType(event.target.value)}>
                  <option value="">请选择适配器类型</option>
                  {Object.entries(ADAPTER_TYPE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select></label>
                <label>接入模式<select data-testid="integration-mode" value={mode} onChange={(event) => setMode(event.target.value)}>
                  <option value="">请选择接入模式</option>
                  {Object.entries(MODE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "供应商编码", value: providerCode.trim() },
          { label: "适配器类型", value: adapterTypeLabel(adapterType) },
          { label: "接入模式", value: MODE_LABELS[mode] ?? "—" },
        ]}
      />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="integration-adapters-table">
            <thead><tr><th>供应商</th><th>适配器类型</th><th>生产就绪</th><th>详情</th></tr></thead>
            <tbody>{rows.map((row) => (
              <tr key={row.id} className={`admin-row-clickable${focusObjectId === row.providerCode ? " admin-row-selected" : ""}`} tabIndex={0} data-testid={`integration-adapter-row-${row.id}`}
                onClick={() => openAdapterDetail(row)}
                onKeyDown={(event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); openAdapterDetail(row); } }}>
                <td>{row.providerCode}</td>
                <td>{adapterTypeLabel(row.adapterType)}</td>
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
