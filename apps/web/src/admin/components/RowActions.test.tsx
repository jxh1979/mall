import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { RowActions, type RowAction } from "./RowActions";
import type { EffectivePermissionsState } from "../types";

const ALL: EffectivePermissionsState = { status: "ready", value: { grants: [], denies: [], wildcard: true } };
const NONE: EffectivePermissionsState = { status: "ready", value: { grants: ["ticket.type.approve"], denies: [], wildcard: false } };

function renderRA(actions: RowAction[], perms = ALL) {
  render(<RowActions actions={actions} permissionsState={perms} testId="ra" />);
}

describe("RowActions 行内动作", () => {
  it("单个动作 → 直接按钮(不套下拉)", () => {
    const onClick = vi.fn();
    renderRA([{ key: "archive", label: "存档", onClick, testId: "act-archive" }]);
    expect(screen.getByTestId("act-archive")).toBeTruthy();
    expect(screen.queryByTestId("ra")).toBeNull(); // 无下拉触发器
    fireEvent.click(screen.getByTestId("act-archive"));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it("2 个动作 → 「操作▾」下拉触发器", () => {
    renderRA([
      { key: "a", label: "取消存档", onClick: vi.fn(), testId: "act-a" },
      { key: "b", label: "申请删除", danger: true, onClick: vi.fn(), testId: "act-b" },
    ]);
    expect(screen.getByTestId("ra").textContent).toContain("操作");
  });

  it("无权限的动作被过滤;全无权限 → 灰显 —", () => {
    // NONE 只有 ticket.type.approve；manage 动作被过滤掉 → 仅剩 approve 动作(1个→按钮)
    renderRA([
      { key: "archive", label: "存档", permissions: ["ticket.product.manage"], onClick: vi.fn(), testId: "act-archive" },
      { key: "approve", label: "批准", permissions: ["ticket.type.approve"], onClick: vi.fn(), testId: "act-approve" },
    ], NONE);
    expect(screen.queryByTestId("act-archive")).toBeNull();
    expect(screen.getByTestId("act-approve")).toBeTruthy();
  });

  it("全部无权限 → 灰显 —", () => {
    renderRA([{ key: "archive", label: "存档", permissions: ["ticket.product.manage"], onClick: vi.fn(), testId: "act-archive" }], NONE);
    expect(screen.getByTestId("ra-none").textContent).toContain("—");
  });
});
