// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { WecomComplianceReviewPage } from "./WecomComplianceReviewPage";
import { AdminApi } from "../client/adminClient";
import type { AdminPageProps, EffectivePermissionsState } from "../types";

const PERMISSIONS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const preview = { id: "wcp-1", candidates: [{ message_id: "message-1", conversation_id: "conversation-1",
  project_id: "project-1", reason: "核实后的清理理由", risk_hits: [{ hit_id: "hit-1", rule_code: "ABUSE", status: "open" }] }],
  scope_digest: "a".repeat(64), message_count: 1, risk_hit_count: 1, created_at: "2026-10-17T12:00:00Z" };
const posts: { url: string; body: Record<string, unknown> }[] = [];

function raw(data: unknown): Response {
  return new Response(JSON.stringify({ success: true, code: "OK", message: "success", data }),
    { status: 200, headers: { "content-type": "application/json" } });
}

function renderPage(master = false) {
  vi.stubGlobal("fetch", vi.fn().mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    if ((init?.method ?? "GET").toUpperCase() === "POST") {
      posts.push({ url, body: JSON.parse(String(init?.body ?? "{}")) });
      if (url.endsWith("/challenge")) return Promise.resolve(raw({ challenge: "this-action-proof", expires_at: "2026-10-17T12:02:00Z" }));
      if (url.endsWith("/execute")) return Promise.resolve(raw({ ...preview, executed_at: "2026-10-17T12:01:00Z", executed_by: "usr_master_authority", key_fingerprint: "fingerprint" }));
      return Promise.resolve(raw(preview));
    }
    return Promise.resolve(raw({ items: [] }));
  }));
  const props: AdminPageProps = {
    api: new AdminApi("test-token"),
    user: master ? { id: "usr_master_authority", username: "master-authority", accountType: "master_authority" }
      : { id: "u1", username: "合规专员", accountType: "platform_admin" },
    setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: PERMISSIONS,
  };
  render(<WecomComplianceReviewPage {...props} />);
  return props;
}

async function createPreview() {
  fireEvent.change(screen.getByLabelText("消息编号 1"), { target: { value: "message-1" } });
  fireEvent.change(screen.getByLabelText("清理理由 1"), { target: { value: "核实后的清理理由" } });
  fireEvent.click(screen.getByRole("button", { name: "预览清理范围" }));
  await screen.findByTestId("wecom-cleanup-preview");
}

describe("企微永久保存与逐次授权清理", () => {
  afterEach(() => { vi.unstubAllGlobals(); posts.length = 0; });

  it("普通管理员只能看永久保存规则，没有期限编辑或清理入口", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText("永久保存，不自动到期归档、删除或匿名化；此规则不可修改。")).toBeTruthy());
    expect(screen.queryByText("新增留存策略")).toBeNull();
    expect(screen.queryByLabelText("消息编号 1")).toBeNull();
    expect(posts).toHaveLength(0);
  });

  it("最高权限逐条理由必填，空白不产生预览", async () => {
    renderPage(true);
    await screen.findByLabelText("消息编号 1");
    fireEvent.change(screen.getByLabelText("消息编号 1"), { target: { value: "message-1" } });
    fireEvent.click(screen.getByRole("button", { name: "预览清理范围" }));
    expect(screen.getByRole("alert").textContent).toContain("每条消息");
    expect(posts).toHaveLength(0);
  });

  it("先预览真实关联范围，再获取挑战和提交本次签名", async () => {
    const props = renderPage(true);
    await screen.findByLabelText("消息编号 1");
    await createPreview();
    expect(screen.getByText("预览：1 条消息、1 条关联命中")).toBeTruthy();
    expect(screen.getByText(/hit-1/)).toBeTruthy();
    expect(posts[0]?.body).toMatchObject({ items: [{ message_id: "message-1", reason: "核实后的清理理由" }] });
    expect(posts[0]?.body.idempotency_key).toBeTruthy();
    expect(screen.queryByLabelText("本次签名")).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "已核对范围，获取本次授权码" }));
    await screen.findByLabelText("本次签名");
    expect((screen.getByLabelText("本次授权码") as HTMLTextAreaElement).value).toBe("this-action-proof");
    fireEvent.change(screen.getByLabelText("本次签名"), { target: { value: "signed-proof" } });
    fireEvent.click(screen.getByRole("button", { name: "使用本次签名执行清理" }));
    await waitFor(() => expect(props.setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "wecom_cleanup_executed" })));
    expect(posts[2]?.body).toMatchObject({ challenge: "this-action-proof", signature: "signed-proof" });
    expect(posts[2]?.body.idempotency_key).toBeTruthy();
    expect(screen.getByRole("status").textContent).toContain("已清理");
    expect(posts.every((post) => !post.url.endsWith("/retention-policies"))).toBe(true);
  });

  it("修改范围或理由后必须重新预览，不能沿用原授权", async () => {
    renderPage(true);
    await screen.findByLabelText("消息编号 1");
    await createPreview();
    fireEvent.click(screen.getByRole("button", { name: "已核对范围，获取本次授权码" }));
    await screen.findByLabelText("本次签名");
    fireEvent.change(screen.getByLabelText("清理理由 1"), { target: { value: "新的理由" } });
    expect(screen.queryByTestId("wecom-cleanup-preview")).toBeNull();
    expect(screen.queryByLabelText("本次签名")).toBeNull();
    expect(posts).toHaveLength(2);
  });
});
