// 文件版本：V0.01
import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { FormModal } from "../components/FormModal";
import { cnDate } from "../components/RowDetailDrawer";
import { useAdminResource } from "../hooks/useAdminResource";
import { COMPLIANCE_ITEM_TYPE_LABELS, COMPLIANCE_REMINDER_TYPE_LABELS } from "../i18n/complianceLabels";
import { useCan } from "../security/Can";
import type { AdminPageProps } from "../types";

// 后端 /api/v1/compliance/expiry-items 与 /reminders 对商户账号只出本商户（客户端已 snake→camel）。
type License = {
  id: string;
  itemType?: string;
  itemName?: string;
  documentNo?: string | null;
  validTo?: string;
  graceStatus?: string | null;
  graceUntil?: string | null;
};
type Reminder = { id: string; sourceId?: string; reminderType?: string; dueDate?: string; reminderAt?: string };

const GRACE_REQUEST = ["compliance.grace_period.request"];

function asRows<T>(value: unknown): T[] {
  if (Array.isArray(value)) return value as T[];
  const items = (value as { items?: T[] } | null)?.items;
  return Array.isArray(items) ? items : [];
}

/** 北京日历日（与后端 LocalZone.ID 同一口径）。 */
function beijingToday(): string {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Shanghai" }).format(new Date());
}

function licenseState(item: License, reminders: Reminder[], today: string): string {
  if (item.graceStatus === "pending_review") return "宽限待审批";
  if (item.graceStatus === "approved" && item.graceUntil) {
    return today <= item.graceUntil ? `宽限中至${cnDate(item.graceUntil)}` : "宽限已过";
  }
  if (item.graceStatus === "rejected") return "宽限被驳回";
  if (item.validTo && item.validTo < today) return "已到期";
  // 只认本次有效期止的到期预警：续期登记只改有效期，旧有效期的预警行仍在，只留在「预警记录」里。
  if (reminders.some((row) => row.reminderType === "upcoming" && row.dueDate === item.validTo)) return "即将到期";
  return "正常";
}

/**
 * ADM-MER-010 我的证照（第十八批 E 段第 22 条，PGM:531「预警同时送达商户侧，商户在商户专区可查看本商户全部证照的到期情况。
 * 证照或资质到期后，可申请一个后台可配时长的宽限期……宽限期须经最高权限审批后生效」）：本商户全部证照的到期、预警与宽限状态；
 * 已到期的证照或资质（保险不适用）可在页内申请宽限期。
 */
export function MerchantZoneLicensesPage({ api, setGlobalError, setActionResult, effectivePermissions }: AdminPageProps) {
  const [items, setItems] = React.useState<License[]>([]);
  const [reminders, setReminders] = React.useState<Reminder[]>([]);
  const [applying, setApplying] = React.useState<License | null>(null);
  const [reason, setReason] = React.useState("");
  const [submitting, setSubmitting] = React.useState(false);
  const [submitError, setSubmitError] = React.useState<string | undefined>();
  const canRequest = useCan(effectivePermissions, GRACE_REQUEST);
  const today = beijingToday();

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [itemData, reminderData] = await Promise.all([
        api.get<unknown>("/api/v1/compliance/expiry-items"),
        api.get<unknown>("/api/v1/compliance/reminders"),
      ]);
      setItems(asRows<License>(itemData));
      setReminders(asRows<Reminder>(reminderData));
    },
    { onError: setGlobalError, fallbackMessage: "证照读取失败", onStart: () => setGlobalError(null) },
  );

  async function submitGrace() {
    if (!applying) return;
    setSubmitting(true);
    setSubmitError(undefined);
    try {
      await api.post(`/api/v1/compliance/expiry-items/${encodeURIComponent(applying.id)}/grace-requests`, {
        idempotency_key: `cgr-${applying.id}-${Date.now()}`,
        reason: reason.trim(),
      });
      setActionResult({ action: "compliance_grace_requested", itemName: applying.itemName });
      setApplying(null);
      setReason("");
      reload();
    } catch (e) {
      setSubmitError((e as Error).message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section data-testid="merchant-zone-licenses-page">
      <AdminPageHeader
        title="我的证照"
        description="本商户全部证照、资质与保险的到期情况、到期预警与宽限期状态。证照或资质到期后可申请宽限期，经最高权限审批后生效；宽限期内可继续经营但每日提醒，超过宽限期仍未更新的一律阻断，不得再次延长。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="merchant-zone-licenses-table">
            <thead><tr><th>证照名称</th><th>类型</th><th>证号</th><th>有效期至</th><th>状态</th><th>预警记录</th><th>操作</th></tr></thead>
            <tbody>
              {items.length === 0 ? <tr><td colSpan={7}>暂无证照</td></tr> : items.map((item) => {
                const own = reminders.filter((row) => row.sourceId === item.id);
                const expired = !!item.validTo && item.validTo < today;
                const showApply = canRequest && expired && item.itemType !== "insurance"
                  && (!item.graceStatus || item.graceStatus === "rejected");
                return (
                  <tr key={item.id} data-testid={`merchant-zone-license-row-${item.id}`}>
                    <td>{item.itemName ?? "-"}</td>
                    <td data-testid={`merchant-zone-license-type-${item.id}`}>{item.itemType ? COMPLIANCE_ITEM_TYPE_LABELS[item.itemType] ?? "其他证照" : "-"}</td>
                    <td>{item.documentNo ?? "-"}</td>
                    <td>{cnDate(item.validTo)}</td>
                    <td data-testid={`merchant-zone-license-state-${item.id}`}>{licenseState(item, own, today)}</td>
                    <td data-testid={`merchant-zone-license-reminders-${item.id}`}>
                      {own.length === 0 ? "—" : own.map((row) => `${COMPLIANCE_REMINDER_TYPE_LABELS[row.reminderType ?? ""] ?? "提醒"}（${cnDate(row.reminderAt)}）`).join("、")}
                    </td>
                    <td>
                      {showApply ? (
                        <button type="button" data-testid={`merchant-zone-license-apply-${item.id}`}
                          onClick={() => { setApplying(item); setReason(""); setSubmitError(undefined); }}>申请宽限期</button>
                      ) : "—"}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </AdminDataState>
      <FormModal
        open={applying !== null}
        title={`申请宽限期 · ${applying?.itemName ?? ""}`}
        description="宽限期时长按该证照类型的后台配置，自原有效期止的次日起算；提交后须经最高权限审批才生效，同一次到期只能获批一次。"
        testId="merchant-zone-license-grace"
        submitTestId="merchant-zone-license-grace-submit"
        submitLabel="提交最高权限审批"
        submitting={submitting}
        error={submitError}
        validate={() => (reason.trim() ? null : "请填写申请理由")}
        onClose={() => setApplying(null)}
        onSubmit={() => void submitGrace()}
      >
        <label>申请理由<textarea data-testid="merchant-zone-license-grace-reason" value={reason} onChange={(e) => setReason(e.target.value)} /></label>
      </FormModal>
    </section>
  );
}
