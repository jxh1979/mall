// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import type { CustomerService } from "@complex/customer-core";

import { platform } from "../platform";
import { StudentCertificationPage } from "./StudentCertificationPage";

// BSM「采集证件、人脸、支付账号、精确位置等敏感信息前先单独取得同意，并留同意版本、时间和范围」；BSM「学生认证必须在本人注册会员后单独申请」；
// 第十三批裁决第 1 条「有效到客人提供的学信网《学籍在线验证报告》上的预计毕业日期，审核人通过时按报告填写」。
function makeService(overrides: Record<string, unknown> = {}) {
  return {
    getStudentCertification: vi.fn().mockResolvedValue(null),
    recordConsent: vi.fn().mockResolvedValue({ id: "consent_1", consentType: "student_certificate", accepted: true }),
    applyStudentCertification: vi.fn().mockResolvedValue({ id: "sc-1", status: "pending", statusLabel: "待审核", validUntil: null,
      rejectReason: null, submittedAt: "2026-10-24T02:00:00Z", reviewedAt: null }),
    ...overrides,
  };
}

describe("StudentCertificationPage 学生认证", () => {
  it("studentCertAsksConsentBeforeUpload：先单独同意证件采集（记同意编号）才能选证件照片，申请带同意编号与学信网验证码", async () => {
    const photo = new File(["p"], "card.jpg", { type: "image/jpeg" });
    const pick = vi.spyOn(platform, "pickMedia").mockResolvedValue(photo);
    const service = makeService();
    render(<StudentCertificationPage service={service as unknown as CustomerService} />);
    await waitFor(() => expect(screen.queryByTestId("student-consent")).not.toBeNull());
    expect(screen.queryByTestId("student-add-photo")).toBeNull();
    expect(pick).not.toHaveBeenCalled();
    expect(screen.getByTestId("student-consent").textContent).toContain("学生证件照片");
    fireEvent.click(screen.getByTestId("student-consent-agree"));
    await waitFor(() => expect(service.recordConsent).toHaveBeenCalledWith(expect.objectContaining({ consentType: "student_certificate",
      accepted: true, version: expect.any(String) })));
    await waitFor(() => expect(screen.queryByTestId("student-add-photo")).not.toBeNull());
    fireEvent.click(screen.getByTestId("student-add-photo"));
    await waitFor(() => expect(screen.queryByTestId("student-files")?.textContent ?? "").toContain("card.jpg"));
    fireEvent.change(screen.getByLabelText("学信网在线验证码"), { target: { value: "ABCD1234EFGH" } });
    await waitFor(() => expect(screen.getByTestId("student-submit")).not.toBeDisabled());
    fireEvent.click(screen.getByTestId("student-submit"));
    await waitFor(() => expect(service.applyStudentCertification).toHaveBeenCalledWith({ consentId: "consent_1",
      chsiVerificationCode: "ABCD1234EFGH", files: [photo] }));
    await waitFor(() => expect(screen.queryByTestId("student-status")?.textContent ?? "").toContain("待审核"));
  });

  it("approvedShowsValidUntilGraduation：已通过显示资格有效至预计毕业日；待审核不再显示申请表", async () => {
    const service = makeService({ getStudentCertification: vi.fn().mockResolvedValue({ id: "sc-1", status: "approved", statusLabel: "已通过",
      validUntil: "2028-06-30", rejectReason: null, submittedAt: "2026-10-01T02:00:00Z", reviewedAt: "2026-10-02T02:00:00Z" }) });
    render(<StudentCertificationPage service={service as unknown as CustomerService} />);
    await waitFor(() => expect(screen.queryByTestId("student-status")?.textContent ?? "").toContain("学生资格有效至 2028-06-30"));
    expect(screen.queryByTestId("student-consent")).toBeNull();
  });

  it("rejectedShowsReasonAndAllowsReapply：被驳回显示原因，可重新申请", async () => {
    const service = makeService({ getStudentCertification: vi.fn().mockResolvedValue({ id: "sc-1", status: "rejected", statusLabel: "已驳回",
      validUntil: null, rejectReason: "照片不清晰", submittedAt: "2026-10-01T02:00:00Z", reviewedAt: "2026-10-02T02:00:00Z" }) });
    render(<StudentCertificationPage service={service as unknown as CustomerService} />);
    await waitFor(() => expect(screen.queryByTestId("student-status")?.textContent ?? "").toContain("照片不清晰"));
    expect(screen.getByTestId("student-consent")).toBeInTheDocument();
  });
});
