import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { AiThresholdsPage } from "./AiThresholdsPage";
import type { AdminApiClient, AdminPageProps, EffectivePermissionsState } from "../types";

const CFG = { localLlmMinConfidence: 0.6, benchmarkDeviation: 0.2 };
function makeApi(get = vi.fn().mockResolvedValue(CFG), post = vi.fn().mockResolvedValue({})): AdminApiClient {
  return { get, getPaged: vi.fn().mockResolvedValue({ items: [], total: 0, page: 1, size: 10 }), post, put: vi.fn().mockResolvedValue({}), patch: vi.fn().mockResolvedValue({}), delete: vi.fn().mockResolvedValue({}) };
}
const ADMIN_PERMS: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
function renderPage(api: AdminApiClient) {
  const props: AdminPageProps = { api, user: { id: "u1", username: "admin", accountType: "platform_admin" }, setActionResult: vi.fn(), setGlobalError: vi.fn(), effectivePermissions: ADMIN_PERMS };
  render(<AiThresholdsPage {...props} />);
}

describe("AiThresholdsPage AI/对标阈值(V218)", () => {
  it("读回当前阈值并按百分比展示", async () => {
    const get = vi.fn().mockResolvedValue(CFG);
    renderPage(makeApi(get));
    await waitFor(() => expect(get).toHaveBeenCalledWith("/api/v1/analytics/ai-thresholds"));
    await waitFor(() => expect(screen.getByTestId("ai-threshold-confidence").textContent).toContain("60%"));
  });

  it("保存调 POST(百分比转小数)", async () => {
    const post = vi.fn().mockResolvedValue({});
    renderPage(makeApi(vi.fn().mockResolvedValue(CFG), post));
    await waitFor(() => screen.getByTestId("ai-thresholds-save"));
    fireEvent.click(screen.getByTestId("ai-thresholds-save"));
    await waitFor(() => expect(post).toHaveBeenCalledWith("/api/v1/analytics/ai-thresholds", { localLlmMinConfidence: 0.6, benchmarkDeviation: 0.2 }));
  });
});
