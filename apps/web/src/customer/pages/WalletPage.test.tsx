import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { WalletPage } from "./WalletPage";
import type { CustomerService } from "../client/customerService";

const projectCard = {
  id: "svacc_prj",
  customerId: "c-1",
  projectId: "prj_1",
  merchantId: null,
  cardScope: "project",
  fundOwnership: null,
  balanceCent: 5000,
  status: "active",
  createdAt: "",
  updatedAt: "",
};

const merchantCard = {
  id: "svacc_m1",
  customerId: "c-1",
  projectId: null,
  merchantId: "m1",
  cardScope: "merchant",
  fundOwnership: "platform_collect",
  balanceCent: 3000,
  status: "active",
  createdAt: "",
  updatedAt: "",
};

describe("WalletPage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("renders per-card balances for the general card and each merchant card", async () => {
    const service = {
      getStoredValueAccounts: vi.fn().mockResolvedValue([projectCard, merchantCard]),
      listMemberships: vi.fn().mockResolvedValue([]),
      topupStoredValue: vi.fn(),
      consumeStoredValue: vi.fn(),
    } as unknown as CustomerService;

    render(<WalletPage service={service} />);

    // 分卡余额展示：通用卡 ¥50.00 + 商户卡 ¥30.00，各自独立。
    await waitFor(() => expect(screen.getByText("综合体通用卡")).toBeInTheDocument());
    expect(screen.getByText(/商户卡（m1）/)).toBeInTheDocument();
    expect(screen.getByText("余额 ¥50.00")).toBeInTheDocument();
    expect(screen.getByText("余额 ¥30.00")).toBeInTheDocument();
    // 商户卡资金模式标签（平台代收）。
    expect(screen.getByText("平台代收")).toBeInTheDocument();
    // 充值/消费入口存在。
    expect(screen.getByTestId("wallet-topup")).toBeInTheDocument();
    expect(screen.getByTestId("wallet-consume")).toBeInTheDocument();
  });

  it("shows empty state when the customer has no stored-value cards", async () => {
    const service = {
      getStoredValueAccounts: vi.fn().mockResolvedValue([]),
      listMemberships: vi.fn().mockResolvedValue([]),
      topupStoredValue: vi.fn(),
      consumeStoredValue: vi.fn(),
    } as unknown as CustomerService;

    render(<WalletPage service={service} />);
    await waitFor(() => expect(screen.getByText("暂无储值卡")).toBeInTheDocument());
  });
});
