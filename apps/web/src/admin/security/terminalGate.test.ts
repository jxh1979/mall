import { describe, expect, it } from "vitest";

import { hiddenGroupKeys } from "./terminalGate";

const ALL = ["overview", "access", "governance", "finance", "integration", "bar"];

describe("terminalGate 3.19 二期菜单 gating", () => {
  it("模块级关闭行 → 隐藏对应菜单组", () => {
    const hidden = hiddenGroupKeys(
      [
        { terminalType: "web", capabilityCode: "terminal.enabled", scopeType: "module", scopeId: "finance" },
        { terminalType: "web", capabilityCode: "terminal.enabled", scopeType: "module", scopeId: "bar" },
      ],
      ALL,
    );
    expect(hidden).toEqual(new Set(["finance", "bar"]));
  });

  it("非 terminal.enabled / 非 module scope 的关闭行不影响菜单", () => {
    const hidden = hiddenGroupKeys(
      [
        { terminalType: "web", capabilityCode: "camera.scan", scopeType: "module", scopeId: "bar" },
        { terminalType: "kiosk", capabilityCode: "terminal.enabled", scopeType: "global", scopeId: "global" },
      ],
      ALL,
    );
    expect(hidden.size).toBe(0);
  });

  it("拉取失败兜底关：除自救组(overview/integration)外全部隐藏", () => {
    const hidden = hiddenGroupKeys(null, ALL);
    expect(hidden).toEqual(new Set(["access", "governance", "finance", "bar"]));
  });

  it("无关闭行 → 不隐藏", () => {
    expect(hiddenGroupKeys([], ALL).size).toBe(0);
  });
});
