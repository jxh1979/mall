// 文件版本：V0.01
// 第十八批 AW 段 299-R2（ENUM:30「票务策略状态为：草稿（规则未发布）、待启用（规则已配置但未到生效时间）、售卖中（当前可售卖）、
//   暂停售卖（临时停止售卖）、已结束（售卖周期结束）、归档（历史策略）。」；R14 裁决 20「「待启用」「已结束」按生效时间派生、不存储」）：
// 票种行状态按后端派生的 sale_status 显示六态；「售卖设置」弹窗改暂停售卖与开售、停售时间（PUT …/sale-settings，整体替换）。
import React from "react";
import { FormModal } from "../components/FormModal";
import { cnDate } from "../components/RowDetailDrawer";
import { PermissionButton } from "../security/Can";
import type { AdminApiClient, EffectivePermissionsState } from "../types";

const MANAGE = ["ticket.product.manage"];

// 六态用语逐字取 ENUM:30；全局码表的 paused「已暂停」、archived「已存档」与此不同，故放页内（先例 DynamicPricingPage 的 ADJUSTMENT_TYPE_LABELS）。
export const TICKET_SALE_STATUS_LABELS: Record<string, string> = {
  draft: "草稿",
  pending_activation: "待启用",
  on_sale: "售卖中",
  paused: "暂停售卖",
  ended: "已结束",
  archived: "归档",
};

// 服务端时刻 → datetime-local 本地串（同 BarServiceResponsibilityPanel 的 inputTime）。
const inputTime = (iso?: string | null) => {
  if (!iso) return "";
  const d = new Date(iso);
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
};

/** 开售、停售时间的中文显示（本地时刻）；空＝不限。 */
export const saleTimeLabel = (iso?: string | null) => (iso ? cnDate(inputTime(iso)) : "不限");

type SaleRow = {
  id: string;
  ticketName: string;
  archivedAt?: string | null;
  deleteStatus?: string;
  saleStartsAt?: string | null;
  saleEndsAt?: string | null;
  salePausedAt?: string | null;
};

type Props = {
  api: AdminApiClient;
  row: SaleRow;
  permissionsState: EffectivePermissionsState;
  setActionResult: (value: unknown) => void;
  onDone: () => void;
};

export function TicketTypeSaleSettings({ api, row, permissionsState, setActionResult, onDone }: Props) {
  const [open, setOpen] = React.useState(false);
  const [paused, setPaused] = React.useState(false);
  const [startsAt, setStartsAt] = React.useState("");
  const [endsAt, setEndsAt] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");

  // 已存档或待删除的票种不可改售卖设置（后端同样 409 TICKET_TYPE_NOT_CONFIGURABLE）。
  if (row.archivedAt || (row.deleteStatus ?? "none") !== "none") return null;

  async function submit() {
    setBusy(true);
    setError("");
    try {
      // 幂等键与蛇形转换由 AdminApi 统一加；空串＝不限，传 null。
      await api.put(`/api/v1/ticketing/ticket-types/${row.id}/sale-settings`, {
        salePaused: paused,
        saleStartsAt: startsAt ? new Date(startsAt).toISOString() : null,
        saleEndsAt: endsAt ? new Date(endsAt).toISOString() : null,
      });
      setActionResult({ action: "ticket_type_sale_settings_changed", id: row.id });
      setOpen(false);
      onDone();
    } catch (err) {
      setError(err instanceof Error ? err.message : "保存售卖设置失败");
    } finally {
      setBusy(false);
    }
  }

  // 弹层经 portal 渲染，React 事件仍沿组件树冒泡到票种行（点击开详情、回车与空格被拦），这里截住。
  const stop = (event: React.SyntheticEvent) => event.stopPropagation();
  return (
    <span onClick={stop} onKeyDown={stop}>
      <PermissionButton permissionsState={permissionsState} permissions={MANAGE} data-testid={`ticket-type-sale-settings-${row.id}`}
        onClick={() => {
          setPaused(Boolean(row.salePausedAt));
          setStartsAt(inputTime(row.saleStartsAt));
          setEndsAt(inputTime(row.saleEndsAt));
          setError("");
          setOpen(true);
        }}>
        售卖设置
      </PermissionButton>
      <FormModal
        open={open}
        title={`售卖设置 · ${row.ticketName}`}
        description="开售、停售时间留空＝不限"
        testId="ticket-type-sale-settings-modal"
        submitTestId="ticket-type-sale-settings-submit"
        submitLabel="保存"
        submitting={busy}
        error={error}
        validate={() => (startsAt && endsAt && new Date(endsAt) <= new Date(startsAt) ? "停售时间须晚于开售时间。" : null)}
        onSubmit={() => void submit()}
        onClose={() => setOpen(false)}
      >
        <label><input type="checkbox" data-testid="ticket-type-sale-paused" checked={paused}
          onChange={(event) => setPaused(event.target.checked)} />暂停售卖</label>
        <label>开售时间<input type="datetime-local" data-testid="ticket-type-sale-starts-at" aria-label="开售时间" value={startsAt}
          onChange={(event) => setStartsAt(event.target.value)} /></label>
        <label>停售时间<input type="datetime-local" data-testid="ticket-type-sale-ends-at" aria-label="停售时间" value={endsAt}
          onChange={(event) => setEndsAt(event.target.value)} /></label>
      </FormModal>
    </span>
  );
}
