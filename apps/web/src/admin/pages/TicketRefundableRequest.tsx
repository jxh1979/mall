// 文件版本：V0.01
// 第十八批 AW 段 295-R2（BSM:785「要让某一个票种可以退款，必须逐个票种单独开启，每次开启都须经高敏权限审批并留痕，不得整批打开」）：
// 票种列表「可退」格里，不可退的票种逐个申请开启可退：填理由与最终审批人提交审批，批准后该票种才可退；最高权限本人提交即生效（BSM:576）。
import React from "react";
import { FormModal } from "../components/FormModal";
import { PermissionButton } from "../security/Can";
import type { AdminApiClient, EffectivePermissionsState } from "../types";

const MANAGE = ["ticket.product.manage"];

type Props = {
  api: AdminApiClient;
  ticketTypeId: string;
  ticketName: string;
  isMaster: boolean;
  effectivePermissions: EffectivePermissionsState;
  setActionResult: (value: unknown) => void;
  onDone: () => void;
};

export function TicketRefundableRequest({ api, ticketTypeId, ticketName, isMaster, effectivePermissions, setActionResult, onDone }: Props) {
  const [open, setOpen] = React.useState(false);
  const [reason, setReason] = React.useState("");
  const [approver, setApprover] = React.useState("");
  const [busy, setBusy] = React.useState(false);
  const [error, setError] = React.useState("");

  async function submit() {
    setBusy(true);
    setError("");
    try {
      const result = await api.post<{ refundable?: boolean }>(`/api/v1/ticketing/ticket-types/${ticketTypeId}/refundable-enable-requests`, {
        idempotency_key: crypto.randomUUID(),
        reason: reason.trim(),
        final_approver_id: approver.trim() || null,
      });
      setActionResult(result?.refundable
        ? { action: "ticket_type_refundable_enabled", ticketTypeId, note: "最高权限本人提交，该票种已可退" }
        : { action: "ticket_type_refundable_enable_submitted", ticketTypeId, note: "已提交审批，批准后该票种才可退" });
      setOpen(false);
      onDone();
    } catch (err) {
      setError(err instanceof Error ? err.message : "申请开启可退失败");
    } finally {
      setBusy(false);
    }
  }

  // 弹层经 portal 渲染，React 事件仍沿组件树冒泡到票种行（点击开详情、回车与空格被拦），这里截住。
  const stop = (event: React.SyntheticEvent) => event.stopPropagation();
  return (
    <span onClick={stop} onKeyDown={stop}>
      不可退
      <PermissionButton permissionsState={effectivePermissions} permissions={MANAGE} data-testid={`ticket-refundable-request-${ticketTypeId}`}
        onClick={() => { setReason(""); setApprover(""); setError(""); setOpen(true); }}>
        申请开启可退
      </PermissionButton>
      <FormModal
        open={open}
        title={`申请开启可退 · ${ticketName}`}
        description="每个票种单独申请，经持票种开启可退审批权限的人批准后才可退；批准前客人不能按「票种规则明确允许」退款。"
        testId="ticket-refundable-request"
        submitTestId="ticket-refundable-request-submit"
        submitLabel="提交审批"
        submitting={busy}
        error={error}
        validate={() => (!reason.trim() ? "请填写开启可退的理由。" : !isMaster && !approver.trim() ? "请填写最终审批人。" : null)}
        onSubmit={() => void submit()}
        onClose={() => setOpen(false)}
      >
        <label>理由（必填）<input data-testid="ticket-refundable-request-reason" aria-label="开启可退理由" value={reason}
          onChange={(event) => setReason(event.target.value)} /></label>
        <label>最终审批人（用户编号）<input data-testid="ticket-refundable-request-approver" aria-label="最终审批人" value={approver}
          placeholder={isMaster ? "最高权限本人发起可不填，提交即生效" : "持票种开启可退审批权限的人，不是登录名"}
          onChange={(event) => setApprover(event.target.value)} /></label>
      </FormModal>
    </span>
  );
}
