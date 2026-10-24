// 文件版本：V0.01
import React from "react";
import { withBase } from "../../basePath";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { CnDatePicker } from "../components/CnDatePicker";
import { cnDate } from "../components/RowDetailDrawer";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

// 后端 StudentCertificationService.ReviewItem（unwrap 已 camel 化）。
type ReviewItem = {
  id: string;
  customerId: string;
  status: string;
  statusLabel: string;
  chsiVerificationCode: string | null;
  attachmentCount: number;
  validUntil: string | null;
  rejectReason: string | null;
  submittedAt: string;
};

const REVIEW = ["ticket.student_cert.review"];
const key = () => `student-cert-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

/**
 * 学生认证审核页（ADM-TKT-005，ROOT-03-R28）：BSM 第 777 行「学生票要票务运营岗人工审核证件 + 学信网核验通过才可用」；第 903 行「认证通过只增加学生资格，
 * 不自动创建会员身份」。审核人查看证件照片，凭客人提供的《学籍在线验证报告》在线验证码到学信网网页核验；通过时按报告填写预计毕业日期作为资格有效期
 * （第十三批裁决第 1 条），驳回须填原因。
 */
export function StudentCertificationsPage(props: AdminPageProps) {
  const { api, setActionResult, setGlobalError, effectivePermissions } = props;
  const [rows, setRows] = React.useState<ReviewItem[]>([]);
  const [loaded, setLoaded] = React.useState(false);
  const [graduation, setGraduation] = React.useState<Record<string, string>>({});
  // 日历按今天所在月份打开（预计毕业日期只能选未来，后端拒收已过日期）。
  const now = new Date();
  const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
  const [reasons, setReasons] = React.useState<Record<string, string>>({});

  const load = React.useCallback(() => {
    api.get<{ items: ReviewItem[] } | ReviewItem[]>("/api/v1/student-certifications?status=pending")
      .then((data) => setRows(Array.isArray(data) ? data : data?.items ?? []))
      .catch((error) => setGlobalError(error instanceof Error ? error.message : "学生认证申请加载失败"))
      .finally(() => setLoaded(true));
  }, [api, setGlobalError]);
  React.useEffect(load, [load]);

  async function decide(row: ReviewItem, action: "approve" | "reject") {
    const body = action === "approve"
      ? { idempotencyKey: key(), validUntil: graduation[row.id] ?? "" }
      : { idempotencyKey: key(), reason: (reasons[row.id] ?? "").trim() };
    if (action === "approve" && !body.validUntil) {
      setGlobalError("请按学信网《学籍在线验证报告》填写预计毕业日期");
      return;
    }
    if (action === "reject" && !("reason" in body && body.reason)) {
      setGlobalError("驳回须填写原因");
      return;
    }
    try {
      await api.post(`/api/v1/student-certifications/${encodeURIComponent(row.id)}/${action}`, body);
      setActionResult({ action: `student_certification_${action}d`, certificationId: row.id });
      load();
    } catch (error) {
      setGlobalError(error instanceof Error ? error.message : "学生认证审核失败");
    }
  }

  return (
    <section data-testid="student-certifications-page">
      <AdminPageHeader
        title="学生认证审核"
        description="客人在官方 App、小程序单独申请。请核对学生证件照片，并用客人提供的《学籍在线验证报告》验证码到学信网网页核验；通过时按报告填写预计毕业日期，学生资格到该日为止，只增加学生资格、不动会员身份。"
      />
      {loaded && rows.length === 0 ? <p data-testid="student-certifications-empty">暂无待审核的学生认证申请</p> : null}
      {rows.length > 0 ? (
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="student-certifications-table">
            <thead><tr><th>申请编号</th><th>客人编号</th><th>学信网验证码</th><th>学生证件照片</th><th>提交时间</th><th>预计毕业日期</th><th>驳回原因</th><th>操作</th></tr></thead>
            <tbody>{rows.map((row) => (
              <tr key={row.id} data-testid={`student-certification-${row.id}`}>
                <td>{row.id}</td>
                <td>{row.customerId}</td>
                <td>{row.chsiVerificationCode ?? "-"}</td>
                <td>{Array.from({ length: row.attachmentCount }, (_, index) => (
                  <a key={index} href={withBase(`/api/v1/student-certifications/${encodeURIComponent(row.id)}/files/${index + 1}`)}
                    target="_blank" rel="noreferrer">照片 {index + 1}{" "}</a>
                ))}</td>
                <td>{cnDate(row.submittedAt)}</td>
                <td><CnDatePicker data-testid={`student-certification-valid-until-${row.id}`} aria-label="预计毕业日期"
                  value={graduation[row.id] ?? ""} today={today} onChange={(value) => setGraduation({ ...graduation, [row.id]: value })} /></td>
                <td><input aria-label="驳回原因" data-testid={`student-certification-reason-${row.id}`} value={reasons[row.id] ?? ""}
                  onChange={(event) => setReasons({ ...reasons, [row.id]: event.target.value })} /></td>
                <td>
                  <PermissionButton permissionsState={effectivePermissions} permissions={REVIEW}
                    data-testid={`student-certification-approve-${row.id}`} onClick={() => void decide(row, "approve")}>通过</PermissionButton>
                  <PermissionButton permissionsState={effectivePermissions} permissions={REVIEW}
                    data-testid={`student-certification-reject-${row.id}`} onClick={() => void decide(row, "reject")}>驳回</PermissionButton>
                </td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      ) : null}
    </section>
  );
}
