// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
// 条款正文收在可展开的条目里（<details>），点标题展开。
import { describe, expect, it, vi } from "vitest";

import type { CustomerService } from "@complex/customer-core";

import { AccountPage } from "./AccountPage";

// ENUM「会员注册……同意当前会员条款和隐私条款后，系统自动注册成功」：客人在「我的」里可随时查看当前生效的两份条款及其版本。
describe("AccountPage 账户与条款", () => {
  it("showsAccountAndCurrentTerms：显示登录手机号（掩码）与当前会员条款、隐私条款的版本和正文", async () => {
    const service = { getTerms: vi.fn().mockResolvedValue({ memberTerms: { version: "M-2026.10", content: "会员条款正文" },
      privacyPolicy: { version: "P-2026.10", content: "隐私条款正文" } }) } as unknown as CustomerService;
    render(<AccountPage service={service} maskedPhone="138****8888" />);
    expect(screen.getByTestId("account-phone").textContent).toContain("138****8888");
    await waitFor(() => expect(screen.queryByTestId("account-terms")?.textContent ?? "").toContain("M-2026.10"));
    expect(screen.getByTestId("account-terms").textContent).toContain("P-2026.10");
    fireEvent.click(screen.getByTestId("account-terms-member"));
    await waitFor(() => expect(screen.queryByText("会员条款正文")).not.toBeNull());
    expect(screen.queryByText("隐私条款正文")).not.toBeNull();
  });
});
