// 文件版本：V0.01
import { RowActions, type RowAction } from "../components/RowActions";
import type { AdminPageProps } from "../types";

type LifecycleRow = { id: string; archivedAt?: string | null; deleteStatus?: string | null };

/** 状态列：删除审批中、已存档时显示存档状态，否则返回 null 由页面显示业务状态。 */
export function archiveStateLabel(row: LifecycleRow): string | null {
  if (row.deleteStatus === "pending") return "待审批删除";
  if (row.archivedAt) return "已存档";
  return null;
}

type Props = Pick<AdminPageProps, "api" | "user" | "effectivePermissions" | "setActionResult" | "setGlobalError"> & {
  row: LifecycleRow;
  /** 该行接口前缀，如 /api/v1/facility/equipment/{id}。 */
  basePath: string;
  manage: string[];
  approve: string[];
  /** 结果键前缀，如 facility_equipment。 */
  resultPrefix: string;
  /** testId 前缀，如 facility-equipment。 */
  testIdPrefix: string;
  onChanged: () => unknown;
};

/**
 * 第十八批 BY 段第 383 条 383-R3、383-R4（BSM:577「设施设备、商户服务工单……现在也支持存档、以及审批后删除」「存档及删除都需要填写理由」
 * 「删除一律须由具备最高权限的人审批通过后执行……持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」；BSM:569「想恢复点「取消存档」即可」）：
 * 楼宇运营工作台设备台账与商户服务工单页的行内存档、取消存档、申请删除（最高权限「直接删除」）、批准与驳回，逐字照 BarReservationLifecycleActions；
 * 按权限码显示按钮，审批只认最高权限由服务端兜底。
 */
export function ArchiveLifecycleActions({ api, user, effectivePermissions, setActionResult, setGlobalError, row, basePath, manage, approve,
  resultPrefix, testIdPrefix, onChanged }: Props) {
  // 同 BarReservationLifecycleActions 判最高权限本人。
  const isMaster = user.id === "usr_master_authority" && user.username === "master-authority";

  /** 收理由：点「取消」返回 null，空白提示必填后返回 null。 */
  function askReason(message: string, emptyError: string): string | null {
    const reason = window.prompt(message);
    if (reason === null) return null;
    if (!reason.trim()) { setGlobalError(emptyError); return null; }
    return reason.trim();
  }

  async function archive(on: boolean) {
    const reason = on ? askReason("存档理由：", "存档理由必填") : undefined;
    if (on && reason === null) return;
    try {
      await api.post(`${basePath}/${on ? "archive" : "unarchive"}`, reason ? { reason } : {});
      setActionResult({ action: `${resultPrefix}_${on ? "archived" : "unarchived"}`, id: row.id });
      await onChanged();
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : "存档操作失败");
    }
  }

  async function requestDelete() {
    const reason = askReason(isMaster ? "删除理由（最高权限本人发起即删除）：" : "删除理由（提交后需最高权限审批）：", "删除理由必填");
    if (reason === null) return;
    try {
      await api.post(`${basePath}/delete-request`, { reason });
      if (isMaster) setActionResult({ action: `${resultPrefix}_delete_approved`, id: row.id, deleted: true });
      else setActionResult({ action: `${resultPrefix}_delete_requested`, id: row.id });
      await onChanged();
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : "删除申请失败");
    }
  }

  async function decide(decision: "approved" | "rejected") {
    try {
      const decided = await api.post<{ deleted?: boolean }>(`${basePath}/delete-approve`, { decision });
      setActionResult({ action: decision === "approved" ? `${resultPrefix}_delete_approved` : `${resultPrefix}_delete_rejected`,
        id: row.id, deleted: decided?.deleted ?? false });
      await onChanged();
    } catch (e) {
      setGlobalError(e instanceof Error ? e.message : "删除审批失败");
    }
  }

  const prefix = testIdPrefix;
  const actions: RowAction[] = row.deleteStatus === "pending"
    ? [
        { key: "approve", label: "批准删除", permissions: approve, onClick: () => void decide("approved"), testId: `${prefix}-delete-approve-${row.id}` },
        { key: "reject", label: "驳回", permissions: approve, danger: true, onClick: () => void decide("rejected"), testId: `${prefix}-delete-reject-${row.id}` },
      ]
    : row.archivedAt
    ? [
        { key: "unarchive", label: "取消存档", permissions: manage, onClick: () => void archive(false), testId: `${prefix}-unarchive-${row.id}` },
        { key: "delete-request", label: isMaster ? "直接删除" : "申请删除", permissions: manage, danger: true, onClick: () => void requestDelete(), testId: `${prefix}-delete-request-${row.id}` },
      ]
    : [{ key: "archive", label: "存档", permissions: manage, onClick: () => void archive(true), testId: `${prefix}-archive-${row.id}` }];
  return <RowActions actions={actions} permissionsState={effectivePermissions} testId={`${prefix}-actions-${row.id}`} />;
}
