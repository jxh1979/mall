// 文件版本：V0.01
import { describe, expect, it } from "vitest";

import { customerSwitchState } from "./switches";
import type { TerminalSwitchRow } from "./types";

const row = (scopeType: string, scopeId: string, capabilityCode = "terminal.enabled"): TerminalSwitchRow => ({
  terminalType: "app",
  capabilityCode,
  scopeType,
  scopeId,
  enabled: false,
});

const context = { version: "0.1.0", projectId: "proj-1" };

describe("customerSwitchState（开关隐藏）", () => {
  it("fetchFailureHidesAll：拉取失败按兜底关闭处理", () => {
    expect(customerSwitchState(null, context)).toEqual({ paused: true, hiddenModules: new Set() });
  });

  it("moduleRowHidesOnlyThatModule", () => {
    const state = customerSwitchState([row("module", "wallet")], context);
    expect(state.paused).toBe(false);
    expect([...state.hiddenModules]).toEqual(["wallet"]);
  });

  it("versionRowMatchingHidesAll：本端版本被关、全局被关或当前综合体被关即暂停服务", () => {
    expect(customerSwitchState([row("version", "0.1.0")], context).paused).toBe(true);
    expect(customerSwitchState([row("global", "global")], context).paused).toBe(true);
    expect(customerSwitchState([row("project", "proj-1")], context).paused).toBe(true);
  });

  it("其他版本、其他综合体与非总开关能力行不影响本端入口", () => {
    const state = customerSwitchState(
      [row("version", "0.0.9"), row("project", "proj-2"), row("module", "points", "customer.stored_value.topup")],
      context,
    );
    expect(state).toEqual({ paused: false, hiddenModules: new Set() });
    expect(customerSwitchState([row("project", "proj-1")], { version: "0.1.0" }).paused).toBe(false);
  });
});
