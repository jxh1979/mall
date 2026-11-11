// 文件版本：V0.05
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { AccessUsersPage } from "./AccessUsersPage";
import { StaffProfilesPage } from "./StaffProfilesPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps } from "../types";

afterEach(cleanup);
function props() {
  const api = new AdminApi("test-token");
  vi.spyOn(api, "get").mockResolvedValue([]);
  vi.spyOn(api, "post").mockResolvedValue({ id: "created", registrationStatus: "submitted" });
  return { api, user: { id: "company", username: "公司审批人", accountType: "platform_admin" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(),
    effectivePermissions: { status: "ready", value: { wildcard: true, grants: [], denies: [] } },
  } as AdminPageProps;
}
it("M006-011 员工引导表单提交注册申请，不发密码或预先赋权，也不发写死备注（198-R5）", async () => {
  const p = props();
  render(<AccessUsersPage {...p} />);
  await waitFor(() => expect(screen.getByTestId("access-user-create-open")).toBeTruthy());
  fireEvent.click(screen.getByTestId("access-user-create-open"));
  fireEvent.change(screen.getByTestId("access-user-name"), { target: { value: "新员工" } });
  await screen.findByTestId("access-user-org");
  expect(screen.queryByTestId("access-user-consent")).toBeNull();
  expect(screen.queryByTestId("access-user-evidence")).toBeNull();
  fireEvent.click(screen.getByTestId("access-user-flow-next"));
  fireEvent.click(screen.getByTestId("access-user-create"));
  await waitFor(() => expect(p.api.post).toHaveBeenCalledTimes(1));
  expect(p.api.post).toHaveBeenCalledWith("/api/v1/staff/registrations", expect.objectContaining({ name: "新员工", projectId: "org-1" }));
  expect(vi.mocked(p.api.post).mock.calls[0]?.[1]).not.toHaveProperty("password");
  expect(vi.mocked(p.api.post).mock.calls[0]?.[1]).not.toHaveProperty("operatorNote");
});
it("M006-025 新建表单不收集无用途的手机号", async () => {
  render(<AccessUsersPage {...props()} />);
  await waitFor(() => expect(screen.getByTestId("access-user-create-open")).toBeTruthy());
  fireEvent.click(screen.getByTestId("access-user-create-open"));
  expect(screen.queryByTestId("access-user-phone")).toBeNull();
});
it("M006-011 员工档案审批使用现有注册复核端点", async () => {
  const p = props();
  vi.mocked(p.api.get).mockResolvedValue([{ id: "registration", name: "新员工", registrationStatus: "submitted", status: "active" }]);
  render(<StaffProfilesPage {...p} />);
  await waitFor(() => expect(screen.getByTestId("staff-registration-approve-registration")).toBeTruthy());
  fireEvent.change(screen.getByTestId("staff-registration-password"), { target: { value: "initial-password" } });
  fireEvent.click(screen.getByTestId("staff-registration-approve-registration"));
  await waitFor(() => expect(p.api.post).toHaveBeenCalledWith("/api/v1/staff/registrations/registration/review",
    expect.objectContaining({ decision: "approve", initialPassword: "initial-password" })));
});
