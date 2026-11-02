// 文件版本：V0.01
import React from "react";
import { Button } from "antd";
import { FormModal } from "../components/FormModal";
import { cnDate } from "../components/RowDetailDrawer";
import { useAdminAction } from "../hooks/useAdminAction";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminApiClient } from "../types";

// 第十八批 M 段第 111 条 111-R3：客诉跟进（BSM:1385「客诉须经客户关怀部确认后方可视为形成闭环，未经其确认的一律不算闭环」）。
// 物业工单链（FacilityWorkbenchPage 投诉行）与商户服务工单链（MerchantWorkOrdersPage 投诉行）共用；关单之后由客户关怀部单独确认闭环。
// 第 118 条 118-R1（BSM:1385「是否属于同一类由客户关怀部认定」）：认定投诉类别，同类一个月内三个不同的人提出即由后端上升。
// 第 111 条 111-R4（第十八批裁决 28「客人投诉时自己选涉及的门店（选填），客户关怀部可以更正」）：物业工单链可更正涉及门店；商户投诉的门店就是提单商户。
type StoreOption = { merchantId: string; merchantName: string };
type ComplaintFollowup = { loopConfirmedBy?: string | null; loopConfirmedAt?: string | null; complaintClass?: string | null;
  storeMerchantId?: string | null; storeCandidates?: StoreOption[] };

export function ComplaintFollowupDialog({ api, chain, workOrderId, closed, onClose, setActionResult, setGlobalError }: {
  api: AdminApiClient;
  chain: "facility" | "merchant_service";
  workOrderId: string;
  /** 工单已关闭才能确认闭环。 */
  closed: boolean;
  onClose: () => void;
  setActionResult: (result: unknown) => void;
  setGlobalError: (message: string | null) => void;
}) {
  const url = `/api/v1/complaint-followups/${chain}/${workOrderId}`;
  const [followup, setFollowup] = React.useState<ComplaintFollowup | null>(null);
  const [complaintClass, setComplaintClass] = React.useState("");
  const [storeId, setStoreId] = React.useState("");
  const { reload } = useAdminResource(async () => {
    const loaded = await api.get<ComplaintFollowup>(url);
    setFollowup(loaded);
    setComplaintClass(loaded.complaintClass ?? "");
    setStoreId(loaded.storeMerchantId ?? "");
  }, { onError: setGlobalError, fallbackMessage: "客诉跟进读取失败" });
  const { busyAction, formError, setFormError, runAction } = useAdminAction({
    reload,
    setGlobalError,
    setActionResult,
    makeResult: (action, request, result) => ({ action, idempotencyKey: request.idempotencyKey, result }),
    fallbackMessage: "客诉跟进操作失败",
  });

  async function confirmLoop() {
    const request = { idempotencyKey: `complaint-loop-confirm-${Date.now()}` };
    await runAction("complaint_loop_confirmed", request, () => api.post(`${url}/loop-confirm`, request));
  }

  async function saveClass() {
    const value = complaintClass.trim();
    if (!value) {
      setFormError("请填写投诉类别");
      return;
    }
    const request = { idempotencyKey: `complaint-classify-${Date.now()}`, complaintClass: value };
    await runAction("complaint_classified", request, () => api.put(url, request));
  }

  /** 不选即清除涉及门店（传空）。 */
  async function saveStore() {
    const request = { idempotencyKey: `complaint-store-${Date.now()}`, storeMerchantId: storeId };
    await runAction("complaint_store_saved", request, () => api.put(url, request));
  }

  const confirmed = !!followup?.loopConfirmedAt;
  return (
    <FormModal
      open
      title="客诉跟进"
      description="客诉须经客户关怀部确认后方可视为形成闭环；工单关闭后在此确认闭环。"
      testId="complaint-followup"
      submitTestId="complaint-loop-confirm"
      submitLabel="确认闭环"
      submitting={busyAction === "complaint_loop_confirmed"}
      submitDisabled={!closed || confirmed || followup === null}
      error={formError || undefined}
      onClose={onClose}
      onSubmit={() => void confirmLoop()}
    >
      <div data-testid="complaint-loop-status">
        {followup === null ? "读取中…"
          : confirmed ? `已闭环：${followup.loopConfirmedBy ?? "-"} 于 ${cnDate(followup.loopConfirmedAt)} 确认`
          : closed ? "工单已关闭，待客户关怀部确认闭环" : "工单未关闭，关闭后才能确认闭环"}
      </div>
      <label>投诉类别（客户关怀部认定，同类一个月内三个不同的人提出即上升）
        <input data-testid="complaint-class-input" maxLength={64} value={complaintClass} onChange={(event) => setComplaintClass(event.target.value)} />
      </label>
      <Button data-testid="complaint-class-save" disabled={followup === null || busyAction !== ""}
        loading={busyAction === "complaint_classified"} onClick={() => void saveClass()}>保存认定</Button>
      {chain === "facility" ? (
        <>
          <label>涉及门店（客人投诉时自选，可更正）
            <select data-testid="complaint-store-select" value={storeId} onChange={(event) => setStoreId(event.target.value)}>
              <option value="">不标注门店</option>
              {(followup?.storeCandidates ?? []).map((store) => <option key={store.merchantId} value={store.merchantId}>{store.merchantName}</option>)}
            </select>
          </label>
          <Button data-testid="complaint-store-save" disabled={followup === null || busyAction !== ""}
            loading={busyAction === "complaint_store_saved"} onClick={() => void saveStore()}>保存门店</Button>
        </>
      ) : null}
    </FormModal>
  );
}
