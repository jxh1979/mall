// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { StudentCertificationsPage } from "./StudentCertificationsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// ROOT-03-R28（BSM 第 777 行「学生票要票务运营岗人工审核证件 + 学信网核验通过才可用」；第十三批裁决第 1 条「有效到客人提供的学信网
// 《学籍在线验证报告》上的预计毕业日期，审核人通过时按报告填写」）。
const PENDING = {
  id: "scert_1", customerId: "customer_1", status: "pending", statusLabel: "待审核", chsiVerificationCode: "CHSI1234567890AB",
  attachmentCount: 2, validUntil: null, rejectReason: null, submittedAt: "2026-10-22T00:00:00Z",
};
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderPage(post = vi.fn().mockResolvedValue({}), setGlobalError = vi.fn()) {
  const api = {
    get: vi.fn().mockResolvedValue({ items: [PENDING] }),
    getPaged: vi.fn(),
    post,
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  } as unknown as AdminApiClient;
  const props = {
    api,
    user: { id: "usr_ticket_ops", username: "ops", accountType: "staff" },
    setActionResult: vi.fn(),
    setGlobalError,
    effectivePermissions: ADMIN_PERMS,
  } as unknown as AdminPageProps;
  render(<StudentCertificationsPage {...props} />);
  return { api, post, setGlobalError };
}

describe("StudentCertificationsPage", () => {
  it("列出待审申请（学信网验证码与证件照片），通过须按报告填预计毕业日期", async () => {
    const { api, post, setGlobalError } = renderPage();
    await waitFor(() => expect(screen.getByTestId("student-certification-scert_1")).toBeInTheDocument());
    expect(api.get).toHaveBeenCalledWith("/api/v1/student-certifications?status=pending");
    expect(screen.getByText("CHSI1234567890AB")).toBeInTheDocument();
    expect(screen.getByText(/照片 2/).closest("a")?.getAttribute("href")).toContain("/api/v1/student-certifications/scert_1/files/2");

    fireEvent.click(screen.getByTestId("student-certification-approve-scert_1"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("请按学信网《学籍在线验证报告》填写预计毕业日期"));
    expect(post).not.toHaveBeenCalled();

    // 日历按今天所在月份打开：翻到下个月选 15 日＝一个未来的预计毕业日期（真实后端拒收已过的日期）。
    fireEvent.click(screen.getByTestId("student-certification-valid-until-scert_1"));
    fireEvent.click(screen.getByRole("button", { name: "下个月" }));
    fireEvent.click(screen.getByRole("button", { name: "15" }));
    fireEvent.click(screen.getByTestId("student-certification-approve-scert_1"));
    const now = new Date();
    const next = new Date(now.getFullYear(), now.getMonth() + 1, 15);
    const expected = `${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, "0")}-15`;
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/student-certifications/scert_1/approve",
      expect.objectContaining({ validUntil: expected })));
  });

  it("驳回须填原因", async () => {
    const { post, setGlobalError } = renderPage();
    await waitFor(() => expect(screen.getByTestId("student-certification-reject-scert_1")).toBeInTheDocument());
    fireEvent.click(screen.getByTestId("student-certification-reject-scert_1"));
    await waitFor(() => expect(setGlobalError).toHaveBeenCalledWith("驳回须填写原因"));
    fireEvent.change(screen.getByTestId("student-certification-reason-scert_1"), { target: { value: "证件照片模糊" } });
    fireEvent.click(screen.getByTestId("student-certification-reject-scert_1"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/student-certifications/scert_1/reject",
      expect.objectContaining({ reason: "证件照片模糊" })));
  });
});
