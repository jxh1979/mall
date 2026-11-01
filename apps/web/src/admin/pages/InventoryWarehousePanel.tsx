// 文件版本：V0.01
// 第十八批 L 段 105-R1（BSM:1162「管理库房和库位，包括总库、酒吧库、吧台库存、消耗品库、礼品库和存酒区。」；ENUM:160 库房分类六类；
// PGM:948「……库房……都要看商户归属……」）：库存台账「库房」区块，列出库房（显示库房编号）并新建。点开才加载；
// 列表与新建同一权限码 inventory.warehouse.manage，由库存台账页按权限挂载，区块内不再判权。
import React from "react";
import { GuidedFlowModal } from "../components/GuidedFlowModal";
import { KEY_VALUE_LABELS } from "../i18n/fieldLabels";
import { formatBusinessStatus } from "../i18n/statusLabels";
import type { AdminPageProps, GovernanceProjectRow } from "../types";

type Warehouse = { id: string; projectId: string; merchantId?: string | null; warehouseType: string; name: string; status: string };
type Merchant = { id: string; merchantName?: string };
type Props = Pick<AdminPageProps, "api" | "setActionResult">;

const WAREHOUSE_TYPES = ["main", "bar", "bar_counter", "consumable", "gift", "saved_bottle"];
const PLATFORM_OWNER = "平台（不归属商户）";
const typeLabel = (value: string) => KEY_VALUE_LABELS.warehouse_type?.[value] ?? value;

export function InventoryWarehousePanel({ api, setActionResult }: Props) {
  const [expanded, setExpanded] = React.useState(false);
  const [rows, setRows] = React.useState<Warehouse[]>([]);
  const [projects, setProjects] = React.useState<GovernanceProjectRow[]>([]);
  const [merchants, setMerchants] = React.useState<Merchant[]>([]);
  const [open, setOpen] = React.useState(false);
  const [projectId, setProjectId] = React.useState("");
  const [merchantId, setMerchantId] = React.useState("");
  const [warehouseType, setWarehouseType] = React.useState("main");
  const [name, setName] = React.useState("");
  const [error, setError] = React.useState("");
  const [saving, setSaving] = React.useState(false);

  async function load() {
    const [warehouseRows, projectRows, merchantRows] = await Promise.all([
      api.get<Warehouse[]>("/api/v1/inventory/warehouses").catch(() => [] as Warehouse[]),
      api.get<GovernanceProjectRow[]>("/api/v1/platform/projects").catch(() => [] as GovernanceProjectRow[]),
      api.get<Merchant[]>("/api/v1/platform/merchants").catch(() => [] as Merchant[]),
    ]);
    setRows(warehouseRows ?? []);
    setProjects((projectRows ?? []).filter((project) => project.status === "active"));
    setMerchants(merchantRows ?? []);
  }

  function toggle() {
    setExpanded((value) => !value);
    if (!expanded) void load();
  }

  function openCreate() {
    setProjectId(projects.length === 1 ? projects[0]!.id : "");
    setMerchantId("");
    setWarehouseType("main");
    setName("");
    setError("");
    setOpen(true);
  }

  const projectName = (id: string) => projects.find((project) => project.id === id)?.name ?? id;
  const ownerName = (id?: string | null) => (id ? merchants.find((merchant) => merchant.id === id)?.merchantName ?? id : PLATFORM_OWNER);

  async function submit() {
    const requestBody = {
      idempotency_key: `admin-warehouse-${Date.now()}`,
      project_id: projectId,
      merchant_id: merchantId || null,
      warehouse_type: warehouseType,
      name: name.trim(),
    };
    setSaving(true);
    setError("");
    try {
      const result = await api.post("/api/v1/inventory/warehouses", requestBody);
      setActionResult({ action: "inventory_warehouse_created", requestBody, result });
      setOpen(false);
      await load();
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : "库房新建失败");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="admin-panel" data-testid="inventory-warehouse-panel">
      <button type="button" onClick={toggle}>库房</button>
      {expanded ? (
        <>
          <div className="admin-form-row"><button type="button" data-testid="inventory-warehouse-create-open" onClick={openCreate}>＋ 新建库房</button></div>
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="inventory-warehouse-table">
              <thead><tr><th>库房编号</th><th>库房名称</th><th>库房分类</th><th>所属项目</th><th>归属</th><th>状态</th></tr></thead>
              <tbody>{rows.map((row) => <tr key={row.id} data-testid={`inventory-warehouse-row-${row.id}`}><td>{row.id}</td><td>{row.name}</td><td>{typeLabel(row.warehouseType)}</td><td>{projectName(row.projectId)}</td><td>{ownerName(row.merchantId)}</td><td>{formatBusinessStatus(row.status)}</td></tr>)}</tbody>
            </table>
          </div>
        </>
      ) : null}
      <GuidedFlowModal
        open={open}
        title="新建库房"
        testId="inventory-warehouse-create-guided"
        submitTestId="inventory-warehouse-create-submit"
        submitLabel="新建库房"
        submitting={saving}
        error={error || undefined}
        onClose={() => setOpen(false)}
        onSubmit={() => void submit()}
        steps={[
          {
            title: "库房信息",
            description: "库位不在这里建：采购验收时填实际库位编号，首次入库即挂到所选库房。存酒区：每个酒吧（以及平台）在一个项目里只建一个在用的，到期存酒转入这里。",
            validate: () => (!projectId ? "请选择所属项目。" : !name.trim() ? "请填写库房名称。" : null),
            content: (
              <>
                <label><span>所属项目</span><select data-testid="inventory-warehouse-create-project" value={projectId} onChange={(event) => setProjectId(event.target.value)}>
                  <option value="">请选择项目</option>
                  {projects.map((project) => <option key={project.id} value={project.id}>{project.name ?? project.id}</option>)}
                </select></label>
                <label><span>归属</span><select data-testid="inventory-warehouse-create-merchant" value={merchantId} onChange={(event) => setMerchantId(event.target.value)}>
                  <option value="">{PLATFORM_OWNER}</option>
                  {merchants.map((merchant) => <option key={merchant.id} value={merchant.id}>{merchant.merchantName ?? merchant.id}</option>)}
                </select></label>
                <label><span>库房分类</span><select data-testid="inventory-warehouse-create-type" value={warehouseType} onChange={(event) => setWarehouseType(event.target.value)}>
                  {WAREHOUSE_TYPES.map((value) => <option key={value} value={value}>{typeLabel(value)}</option>)}
                </select></label>
                <label><span>库房名称</span><input data-testid="inventory-warehouse-create-name" value={name} onChange={(event) => setName(event.target.value)} /></label>
              </>
            ),
          },
        ]}
        summary={[
          { label: "所属项目", value: projectId ? projectName(projectId) : "未选择" },
          { label: "归属", value: ownerName(merchantId || null) },
          { label: "库房分类", value: typeLabel(warehouseType) },
          { label: "库房名称", value: name.trim() || "未填写" },
        ]}
      />
    </section>
  );
}
