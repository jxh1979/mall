// 文件版本：V0.02
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { TicketOrdersPage } from "./TicketOrdersPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

// TK-06-R5（BSM:780 一人多次票首次核销留存人脸、后续以该人脸核对；2026-10-21 裁决：供应商不可用时不留不核、不做目视核对）：
// 核销预检按后端 face_binding 提示本次入场的人脸处置；单次票、多人票（face_binding 为空）不出现任何人脸区块，也不去取人脸照。
const ORDERS = [{ id: "o1", orderNo: "TO-1", payStatus: "paid", issueStatus: "issued" }];
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

function renderWithPreview(previewResult: Record<string, unknown>) {
  const get = vi.fn().mockImplementation((url: string) =>
    Promise.resolve(url.includes("/ticketing/orders") ? ORDERS : []));
  const post = vi.fn().mockResolvedValue(previewResult);
  const api = {
    get,
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post,
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult,
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<TicketOrdersPage {...props} />);
  return { get, post };
}

const setActionResult = vi.fn();

// 点核销预检并等这次动作整个跑完（预检结果已落到操作结果里）。
async function runPreview() {
  setActionResult.mockClear();
  await waitFor(() => expect(screen.getByTestId("ticket-verification-preview")).toBeInTheDocument());
  await waitFor(() => expect(screen.getByTestId("ticket-verification-preview")).not.toBeDisabled());
  await userEvent.click(screen.getByTestId("ticket-verification-preview"));
  await waitFor(() => expect(setActionResult).toHaveBeenCalledWith(
    expect.objectContaining({ action: "ticket_verification_previewed" })));
}

describe("TicketOrdersPage 一人多次票留脸提示（TK-06）", () => {
  it("预检返回 capture：提示首次入场须客人单独同意后在检票终端拍照留存", async () => {
    const { get, post } = renderWithPreview({ allowed: true, entitlement: { id: "ent-1" }, faceBinding: "capture" });
    await runPreview();
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/ticketing/verifications/preview", expect.anything()));
    await waitFor(() => expect(screen.queryByTestId("ticket-face-binding")?.textContent ?? "").toContain("首次"));
    expect(screen.queryByTestId("ticket-face-binding")?.textContent ?? "").toContain("同意");
    expect(get).not.toHaveBeenCalledWith(expect.stringContaining("/face-photo"));
  });

  it("预检返回 match：提示由系统比对留存人脸，不一致拒绝", async () => {
    const { get } = renderWithPreview({ allowed: true, entitlement: { id: "ent-1" }, faceBinding: "match" });
    await runPreview();
    await waitFor(() => expect(screen.queryByTestId("ticket-face-binding")?.textContent ?? "").toContain("比对"));
    expect(get).not.toHaveBeenCalledWith(expect.stringContaining("/face-photo"));
  });

  it("预检未返回 face_binding（单次票、多人票或供应商不可用）：不出现任何人脸区块，不取人脸照", async () => {
    const { get, post } = renderWithPreview({ allowed: true, entitlement: { id: "ent-1" } });
    await runPreview();
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/ticketing/verifications/preview", expect.anything()));
    await waitFor(() => expect(screen.queryByTestId("ticket-face-photo-panel")).toBeNull());
    expect(screen.queryByTestId("ticket-face-binding")).toBeNull();
    expect(get).not.toHaveBeenCalledWith(expect.stringContaining("/face-photo"));
  });

  // 第十八批 AF 段第 198 条 198-R5（核销预检 VerificationRequest 契约与 DTO 都没有 operator_note）：预检请求体不再带写死的 operatorNote。
  it("核销预检请求体不含 operatorNote", async () => {
    const { post } = renderWithPreview({ allowed: true, entitlement: { id: "ent-1" } });
    await runPreview();
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/ticketing/verifications/preview", expect.anything()));
    const body = post.mock.calls.find(([path]) => path === "/api/v1/ticketing/verifications/preview")![1];
    expect(Object.keys(body as object)).not.toContain("operatorNote");
  });
});
