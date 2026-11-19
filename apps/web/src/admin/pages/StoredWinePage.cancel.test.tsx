// 文件版本：V0.04
// FS-017（BSM L1072「存酒作废需照片+视频双证据+主管复核」）：作废照片、视频拍照录像上传后 POST …/cancel 含双证据；
// 第十八批 AY 段 269-R2：高低价值交服务端在作废当刻判定，前端不再预检照片视频。
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { StoredWinePage } from "./StoredWinePage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState, StoredWineRow } from "../types";

const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };

const ROWS: StoredWineRow[] = [
  { id: "w-1", savedNo: "SW-0001", customerName: "客户甲", itemName: "威士忌", remainingQuantity: 2, status: "stored", createdAt: "2026-07-01" },
];
const ROWS_AB: StoredWineRow[] = [
  ...ROWS,
  { id: "w-2", savedNo: "SW-0002", customerName: "客户乙", itemName: "白兰地", remainingQuantity: 1, status: "stored", createdAt: "2026-07-02" },
];

function makeApi(rows: StoredWineRow[] = ROWS): AdminApiClient {
  return {
    get: vi.fn(async (path: string) => (path.endsWith("/remedies") ? [] : rows)),
    getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }),
    post: vi.fn(async () => ({ id: "w-1", status: "disputed_frozen" })),
    // 第十八批 AY 段 269-R1：证据改为真实上传，按附件角色回服务端编号（视频 att_v1，其余 att_p1）。
    postForm: vi.fn(async (_path: string, form: FormData) => ({ id: form.get("attachment_role") === "video" ? "att_v1" : "att_p1" })),
    put: vi.fn().mockResolvedValue({}),
    patch: vi.fn().mockResolvedValue({}),
    delete: vi.fn().mockResolvedValue({}),
  } as unknown as AdminApiClient;
}

function renderPage(api: AdminApiClient) {
  const props: AdminPageProps = {
    api,
    user: { id: "u1", username: "admin", accountType: "platform_admin" },
    setActionResult: vi.fn(),
    setGlobalError: vi.fn(),
    effectivePermissions: ADMIN_PERMS,
  };
  render(<StoredWinePage {...props} />);
}

describe("StoredWinePage 作废（FS-017）", () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, "", "/admin/bar/stored-wine");
  });

  // 第十八批 AY 段 269-R2（第十八批裁决 14「高价值的……存酒作废，必须照片＋视频＋主管复核才生效；低价值的维持现状，即时生效。」）：
  // 前端不按登记快照预判高低价值、不强制照片视频，交服务端在作废当刻判定；高价值缺证据时显示服务端的中文报错。
  it("269-R2：只填作废原因、不上传就能提交 → POST …/cancel 照片视频编号为空数组，不被前端预检拦下", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-cancel-form")).not.toBeNull());
    fireEvent.click(screen.getByTestId("stored-wine-select-w-1"));
    fireEvent.change(screen.getByTestId("stored-wine-cancel-reason"), { target: { value: "酒瓶破损" } });
    fireEvent.click(screen.getByTestId("stored-wine-cancel"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/inventory/saved-bottles/w-1/cancel",
      expect.objectContaining({ reason: "酒瓶破损", photo_attachment_ids: [], video_attachment_ids: [] }),
    ));
    expect(screen.getByTestId("stored-wine-page").textContent).not.toContain("作废须填写照片与视频");
    expect(screen.getByTestId("stored-wine-cancel-boundary").textContent).toBe(
      "高价值存酒作废须上传照片和视频，先冻结、待主管复核通过后才作废；其余存酒提交即作废。作废后在下方登记补存或赔付。");
  });

  it("269-R2：服务端拒绝高价值作废缺证据时，作废区显示服务端中文报错", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = {
      ...makeApi(),
      post: vi.fn(async (path: string) => {
        if (path.endsWith("/cancel")) throw new Error("照片附件不能为空：高价值动作需照片+视频双证据");
        return {};
      }),
    } as unknown as AdminApiClient;
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-cancel-form")).not.toBeNull());
    fireEvent.click(screen.getByTestId("stored-wine-select-w-1"));
    fireEvent.change(screen.getByTestId("stored-wine-cancel-reason"), { target: { value: "酒瓶破损" } });
    fireEvent.click(screen.getByTestId("stored-wine-cancel"));
    await waitFor(() => expect(screen.getByTestId("stored-wine-cancel-error").textContent).toBe("照片附件不能为空：高价值动作需照片+视频双证据"));
  });

  // 第十八批 AY 段 269-R1（BSM:1072、BSM:1940；PGM:287）：作废照片、视频改为拍照、录像上传，标签不再要员工填编号。
  it("269-R1：作废照片、视频拍照录像上传（business_type=saved_bottle_cancel）；上传后 POST …/cancel 含上传回的编号，不含 operator_note", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.getByTestId("stored-wine-cancel-photos").getAttribute("type")).toBe("file"));
    expect(screen.getByTestId("stored-wine-cancel-photos").closest("label")!.textContent).not.toMatch(/id|逗号分隔/);
    expect(screen.getByTestId("stored-wine-cancel-videos").closest("label")!.textContent).not.toMatch(/id|逗号分隔/);
    fireEvent.click(screen.getByTestId("stored-wine-select-w-1"));
    fireEvent.change(screen.getByTestId("stored-wine-cancel-reason"), { target: { value: "酒瓶破损" } });
    fireEvent.change(screen.getByTestId("stored-wine-cancel-photos"), { target: { files: [new File(["x"], "a.jpg", { type: "image/jpeg" })] } });
    await waitFor(() => expect(screen.getByTestId("stored-wine-cancel-photos-files").textContent).toBe("a.jpg"));
    const photoForm = vi.mocked(api.postForm!).mock.calls[0]!;
    expect(photoForm[0]).toBe("/api/v1/evidence/attachments/upload");
    expect(photoForm[1].get("business_type")).toBe("saved_bottle_cancel");
    expect(photoForm[1].get("attachment_role")).toBe("photo");
    fireEvent.change(screen.getByTestId("stored-wine-cancel-videos"), { target: { files: [new File(["x"], "a.mp4", { type: "video/mp4" })] } });
    await waitFor(() => expect(screen.getByTestId("stored-wine-cancel-videos-files").textContent).toBe("a.mp4"));
    const videoForm = vi.mocked(api.postForm!).mock.calls[1]!;
    expect(videoForm[1].get("business_type")).toBe("saved_bottle_cancel");
    expect(videoForm[1].get("attachment_role")).toBe("video");
    fireEvent.click(screen.getByTestId("stored-wine-cancel"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/inventory/saved-bottles/w-1/cancel",
      expect.objectContaining({ reason: "酒瓶破损", photo_attachment_ids: ["att_p1"], video_attachment_ids: ["att_v1"] }),
    ));
    // 第十八批 AF 段 198-R5：服务端不读 operator_note，请求体不再带（本页备注框只随补救登记提交）。
    expect(vi.mocked(api.post).mock.calls.find(([path]) => path === "/api/v1/inventory/saved-bottles/w-1/cancel")![1]).not.toHaveProperty("operator_note");
  });

  // 第十八批 AP 段第 252 条 252-R1（BSM:1940「……存酒作废……必须记录原因、员工、时间、点位、照片、视频和主管复核结果。」）：作废原因必填。
  it("252-R1：作废原因为空 → 不发请求并提示「作废须填写作废原因」；填写后请求体 reason 等于所填", async () => {
    const confirmSpy = vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi();
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-cancel-form")).not.toBeNull());
    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/inventory/saved-bottles/w-1/remedies"), { timeout: 5000 });
    fireEvent.change(screen.getByTestId("stored-wine-cancel-photos"), { target: { files: [new File(["x"], "a.jpg", { type: "image/jpeg" })] } });
    await waitFor(() => expect(screen.queryByTestId("stored-wine-cancel-photos-files")).not.toBeNull());
    fireEvent.change(screen.getByTestId("stored-wine-cancel-videos"), { target: { files: [new File(["x"], "a.mp4", { type: "video/mp4" })] } });
    await waitFor(() => expect(screen.queryByTestId("stored-wine-cancel-videos-files")).not.toBeNull());
    fireEvent.click(screen.getByTestId("stored-wine-cancel"));
    await waitFor(() => expect(screen.getByTestId("stored-wine-cancel-error").textContent).toContain("作废须填写作废原因"));
    expect(confirmSpy).not.toHaveBeenCalled();
    expect(api.post).not.toHaveBeenCalledWith(expect.stringContaining("/cancel"), expect.anything());

    fireEvent.change(screen.getByTestId("stored-wine-cancel-reason"), { target: { value: " 酒瓶破损 " } });
    fireEvent.click(screen.getByTestId("stored-wine-cancel"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/inventory/saved-bottles/w-1/cancel",
      expect.objectContaining({ reason: "酒瓶破损" }),
    ));
  });

  // 第十八批 AY 段收尾复核（BSM:1940「……存酒作废……必须记录原因、员工、时间、点位、照片、视频和主管复核结果。」；第十八批裁决 14）：
  // 作废提交成功后清空本次照片视频，换选另一瓶也清空，下一瓶不带上前一瓶的证据。
  it("收尾复核：作废 A 成功后照片视频列表清空；再选 B 只填原因作废，请求体照片与视频编号都是空数组", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    const api = makeApi(ROWS_AB);
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-select-w-2")).not.toBeNull());
    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/inventory/saved-bottles/w-1/remedies"), { timeout: 5000 });
    fireEvent.change(screen.getByTestId("stored-wine-cancel-reason"), { target: { value: "酒瓶破损" } });
    fireEvent.change(screen.getByTestId("stored-wine-cancel-photos"), { target: { files: [new File(["x"], "a.jpg", { type: "image/jpeg" })] } });
    await waitFor(() => expect(screen.getByTestId("stored-wine-cancel-photos-files").textContent).toBe("a.jpg"));
    fireEvent.change(screen.getByTestId("stored-wine-cancel-videos"), { target: { files: [new File(["x"], "a.mp4", { type: "video/mp4" })] } });
    await waitFor(() => expect(screen.getByTestId("stored-wine-cancel-videos-files").textContent).toBe("a.mp4"));
    fireEvent.click(screen.getByTestId("stored-wine-cancel"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/inventory/saved-bottles/w-1/cancel",
      expect.objectContaining({ photo_attachment_ids: ["att_p1"], video_attachment_ids: ["att_v1"] }),
    ));
    await waitFor(() => expect(screen.queryByTestId("stored-wine-cancel-photos-files")).toBeNull());
    expect(screen.queryByTestId("stored-wine-cancel-videos-files")).toBeNull();
    await waitFor(() => expect((screen.getByTestId("stored-wine-cancel") as HTMLButtonElement).disabled).toBe(false));

    fireEvent.click(screen.getByTestId("stored-wine-select-w-2"));
    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/inventory/saved-bottles/w-2/remedies"));
    fireEvent.change(screen.getByTestId("stored-wine-cancel-reason"), { target: { value: "瓶身破裂" } });
    fireEvent.click(screen.getByTestId("stored-wine-cancel"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith(
      "/api/v1/inventory/saved-bottles/w-2/cancel",
      expect.objectContaining({ reason: "瓶身破裂", photo_attachment_ids: [], video_attachment_ids: [] }),
    ));
  });

  it("收尾复核：为 A 上传作废照片未提交，换选 B 后作废照片列表清空", async () => {
    const api = makeApi(ROWS_AB);
    renderPage(api);
    await waitFor(() => expect(screen.queryByTestId("stored-wine-select-w-2")).not.toBeNull());
    await waitFor(() => expect(api.get).toHaveBeenCalledWith("/api/v1/inventory/saved-bottles/w-1/remedies"), { timeout: 5000 });
    fireEvent.change(screen.getByTestId("stored-wine-cancel-photos"), { target: { files: [new File(["x"], "a.jpg", { type: "image/jpeg" })] } });
    await waitFor(() => expect(screen.getByTestId("stored-wine-cancel-photos-files").textContent).toBe("a.jpg"));
    fireEvent.click(screen.getByTestId("stored-wine-select-w-2"));
    await waitFor(() => expect(screen.queryByTestId("stored-wine-cancel-photos-files")).toBeNull());
  });
});
