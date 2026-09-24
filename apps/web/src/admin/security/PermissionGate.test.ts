import { fireEvent, render, screen } from "@testing-library/react";
import { createElement } from "react";
import { describe, expect, it, vi } from "vitest";

import type { EffectivePermissions, EffectivePermissionsState } from "../types";
import { Can, PermissionButton } from "./Can";
import { hasPermissions, PermissionGate } from "./PermissionGate";

function ready(value: Partial<EffectivePermissions>): EffectivePermissionsState {
  return {
    status: "ready",
    value: { grants: [], denies: [], wildcard: false, ...value },
  };
}

describe("hasPermissions", () => {
  it("grant-only: 全部 required 在 grants 内才放行", () => {
    const state = ready({ grants: ["finance.ledger.read", "finance.ledger.write"] });
    expect(hasPermissions(state, ["finance.ledger.read"])).toBe(true);
    expect(hasPermissions(state, ["finance.ledger.read", "finance.ledger.write"])).toBe(true);
    expect(hasPermissions(state, ["finance.ledger.read", "finance.adjust.approve"])).toBe(false);
  });

  it("deny-override: 任一 required 命中 denies 即拒（优先于 grants）", () => {
    const state = ready({
      grants: ["finance.ar_ap.view", "finance.ledger.read"],
      denies: ["finance.ar_ap.view"],
    });
    expect(hasPermissions(state, ["finance.ar_ap.view"])).toBe(false);
    expect(hasPermissions(state, ["finance.ledger.read", "finance.ar_ap.view"])).toBe(false);
    expect(hasPermissions(state, ["finance.ledger.read"])).toBe(true);
  });

  it("wildcard: 无 DENY 命中时放行任意 required", () => {
    const state = ready({ wildcard: true });
    expect(hasPermissions(state, ["finance.ledger.read"])).toBe(true);
    expect(hasPermissions(state, ["integration.adapter.read", "secret.ref.bind"])).toBe(true);
  });

  it("wildcard+deny: wildcard 仍受 DENY 约束（DENY 优先于 wildcard）", () => {
    const state = ready({ wildcard: true, denies: ["finance.refund.create"] });
    expect(hasPermissions(state, ["finance.refund.create"])).toBe(false);
    expect(hasPermissions(state, ["finance.ledger.read"])).toBe(true);
    expect(hasPermissions(state, ["finance.ledger.read", "finance.refund.create"])).toBe(false);
  });

  it("loading: fail-safe 一律拒绝", () => {
    const state: EffectivePermissionsState = { status: "loading" };
    expect(hasPermissions(state, ["finance.ledger.read"])).toBe(false);
    expect(hasPermissions(state, [])).toBe(false);
  });

  it("error: fail-safe 一律拒绝", () => {
    const state: EffectivePermissionsState = { status: "error" };
    expect(hasPermissions(state, ["finance.ledger.read"])).toBe(false);
    expect(hasPermissions(state, [])).toBe(false);
  });

  it("空 required: ready 态放行（无需任何权限）", () => {
    expect(hasPermissions(ready({}), [])).toBe(true);
    expect(hasPermissions(ready({ grants: ["finance.ledger.read"] }), [])).toBe(true);
    expect(hasPermissions(ready({ wildcard: true }), [])).toBe(true);
  });
});

// M038-010：权限闸组件的渲染分支（此前只测了纯函数 hasPermissions）。本文件是 .ts，用 createElement 渲染。
describe("PermissionButton / Can / PermissionGate 组件", () => {
  const denied = ready({ grants: ["access.role.view"] });
  const allowed = ready({ grants: ["access.role.assign"] });

  it("无权限：按钮禁用、带提示，点击不触发；有权限：点击触发", () => {
    const onClick = vi.fn();
    const view = render(createElement(PermissionButton, { permissionsState: denied, permissions: ["access.role.assign"], onClick, "data-testid": "gate-btn" , children: "装配角色" }));
    const button = screen.getByTestId("gate-btn") as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    expect(button.title).toBe("当前账号无此操作权限");
    fireEvent.click(button);
    expect(onClick).not.toHaveBeenCalled();
    view.rerender(createElement(PermissionButton, { permissionsState: allowed, permissions: ["access.role.assign"], onClick, "data-testid": "gate-btn" , children: "装配角色" }));
    expect((screen.getByTestId("gate-btn") as HTMLButtonElement).disabled).toBe(false);
    fireEvent.click(screen.getByTestId("gate-btn"));
    expect(onClick).toHaveBeenCalledTimes(1);
    view.unmount();
  });

  it("显式拒绝压过授予：按钮仍禁用", () => {
    const onClick = vi.fn();
    const view = render(createElement(PermissionButton, { permissionsState: ready({ grants: ["access.role.assign"], denies: ["access.role.assign"] }), permissions: ["access.role.assign"], onClick, "data-testid": "gate-btn-deny" , children: "装配角色" }));
    fireEvent.click(screen.getByTestId("gate-btn-deny"));
    expect((screen.getByTestId("gate-btn-deny") as HTMLButtonElement).disabled).toBe(true);
    expect(onClick).not.toHaveBeenCalled();
    view.unmount();
  });

  it("Can：无权限渲染 fallback，有权限渲染子内容", () => {
    const view = render(createElement(Can, { permissionsState: denied, permissions: ["access.role.assign"], fallback: "无权查看" , children: "敏感内容" }));
    expect(view.container.textContent).toBe("无权查看");
    view.rerender(createElement(Can, { permissionsState: allowed, permissions: ["access.role.assign"], fallback: "无权查看" , children: "敏感内容" }));
    expect(view.container.textContent).toBe("敏感内容");
    view.unmount();
  });

  it("PermissionGate：加载中、权限不足、放行三种渲染", () => {
    const view = render(createElement(PermissionGate, { permissionsState: { status: "loading" } as EffectivePermissionsState, permissions: ["access.role.assign"] , children: "页面内容" }));
    expect(screen.getByTestId("admin-permission-loading").textContent).toContain("权限加载中");
    view.rerender(createElement(PermissionGate, { permissionsState: denied, permissions: ["access.role.assign"] , children: "页面内容" }));
    expect(screen.getByTestId("admin-permission-denied").textContent).toContain("权限不足");
    expect(view.container.textContent).not.toContain("页面内容");
    view.rerender(createElement(PermissionGate, { permissionsState: allowed, permissions: ["access.role.assign"] , children: "页面内容" }));
    expect(view.container.textContent).toBe("页面内容");
    view.unmount();
  });
});
