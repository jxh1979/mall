// 文件版本：V0.02
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { ParkingPage } from "./ParkingPage";
import type { CustomerService } from "@complex/customer-core";
import { platform } from "../platform";

// 单测里配一个允许的 H5 支付主机（正式支付资料未到，打包产物不带此值）。
vi.mock("../api", async (importOriginal) => ({ ...(await importOriginal<typeof import("../api")>()), CUSTOMER_PAY_HOSTS: ["pay.example.com"] }));

// HP-P01（BSM L1435「出口扫码支付，或在官方 App/小程序提前缴费后在离场宽限时限内离场」；L1433 无牌车凭临时码缴费）。
describe("ParkingPage", () => {
  it("lookupPayAndShowGraceDeadline：查车 → 显示停车费与应缴 → 支付确认后显示离场宽限截止", async () => {
    const service = {
      getParkingCredit: vi.fn().mockResolvedValue({ projectId: "proj-1", businessDate: "2026-10-21", status: "not_reached",
        eligibleAmountCent: 0, thresholdCent: 20000, creditMinutes: 120 }),
      findParkingSession: vi.fn().mockResolvedValue({ parkingSessionId: "ps-1", projectId: "proj-1", plateNo: "粤A12345",
        enteredAt: "2026-10-21T02:00:00Z", feeCent: 1500, paidCent: 0, dueCent: 1500, graceUntil: null }),
      createParkingPayment: vi.fn().mockResolvedValue({ id: "ppay-1", amountCent: 1500, status: "pending", paymentUrl: null,
        launch: { kind: "h5_url", params: { url: "https://pay.example.com/cashier?no=9" } } }),
      confirmParkingPayment: vi.fn().mockResolvedValue({ id: "ppay-1", amountCent: 1500, status: "paid",
        graceUntil: "2026-10-21T05:15:00Z" }),
    } as unknown as CustomerService;

    const openPayment = vi.spyOn(platform, "openPayment").mockResolvedValue(true);
    render(<ParkingPage service={service} initialProjectId="proj-1" />);
    await waitFor(() => expect(screen.queryByLabelText("车牌号或临时码")).not.toBeNull());
    fireEvent.change(screen.getByLabelText("车牌号或临时码"), { target: { value: "粤A12345" } });
    fireEvent.click(screen.getByRole("button", { name: "查询停车费" }));
    await waitFor(() => expect(screen.queryByTestId("parking-due")?.textContent ?? "").toContain("¥15.00"));
    expect(service.findParkingSession).toHaveBeenCalledWith("proj-1", "粤A12345");

    fireEvent.click(screen.getByRole("button", { name: "去缴费" }));
    await waitFor(() => expect(service.createParkingPayment).toHaveBeenCalledWith("ps-1"));
    // 付款页交系统打开（客人包不在应用内加载远程网页）。
    await waitFor(() => expect(screen.queryByRole("button", { name: /前往支付/ })).not.toBeNull());
    await waitFor(() => expect(screen.getByRole("button", { name: /前往支付/ })).toBeEnabled());
    fireEvent.click(screen.getByRole("button", { name: /前往支付/ }));
    await waitFor(() => expect(openPayment).toHaveBeenCalledWith("https://pay.example.com/cashier?no=9"));
    fireEvent.click(screen.getByRole("button", { name: "我已完成支付" }));
    await waitFor(() => expect(screen.queryByTestId("parking-grace")?.textContent ?? "").toContain("离场"));
    expect(service.confirmParkingPayment).toHaveBeenCalledWith("ppay-1");
  });

  // HP-P01（BSM「停车场」章「连接客人端，客人在官方 App/小程序缴费并查看消费抵免」；第十二批操作员裁决补充第 4 条）：
  // 显示本日抵免状态；抵免把应缴抵到 0 时由客人确认使用抵免离场（后端记 0 元已缴），随即显示离场宽限截止。
  it("showsTodayCreditAndUsesCreditToExit：显示本日消费抵免与抵后应缴，抵到 0 元使用抵免离场", async () => {
    const service = {
      getParkingCredit: vi.fn().mockResolvedValue({ projectId: "proj-1", businessDate: "2026-10-21", status: "available",
        eligibleAmountCent: 30000, thresholdCent: 20000, creditMinutes: 120 }),
      findParkingSession: vi.fn().mockResolvedValue({ parkingSessionId: "ps-2", projectId: "proj-1", plateNo: "粤A22222",
        enteredAt: "2026-10-21T02:00:00Z", grossFeeCent: 1000, feeCent: 0, paidCent: 0, dueCent: 0, graceUntil: null,
        creditId: "pcr-1", creditStatus: "available", creditMinutes: 120, creditOffsetHours: 2 }),
      createParkingPayment: vi.fn().mockResolvedValue({ id: "ppay-2", amountCent: 0, status: "paid", graceUntil: "2026-10-21T05:15:00Z" }),
      confirmParkingPayment: vi.fn(),
    } as unknown as CustomerService;

    render(<ParkingPage service={service} initialProjectId="proj-1" initialCode="粤A22222" />);
    await waitFor(() => expect(screen.queryByTestId("parking-credit")?.textContent ?? "").toContain("可用"));
    expect(screen.getByTestId("parking-credit").textContent).toContain("120 分钟");
    expect(service.getParkingCredit).toHaveBeenCalledWith("proj-1");
    fireEvent.click(screen.getByRole("button", { name: "查询停车费" }));
    await waitFor(() => expect(screen.queryByTestId("parking-credit-offset")?.textContent ?? "").toContain("¥10.00"));
    expect(screen.getByTestId("parking-due").textContent).toContain("¥0.00");
    fireEvent.click(screen.getByRole("button", { name: "使用抵免离场" }));
    await waitFor(() => expect(screen.queryByTestId("parking-grace")?.textContent ?? "").toContain("离场"));
    expect(service.createParkingPayment).toHaveBeenCalledWith("ps-2");
    expect(service.confirmParkingPayment).not.toHaveBeenCalled();
  });

  // 第二轮复核有依据直接修（第十二批操作员裁决第 11 条「一分没少收＝没用掉抵免」）：续缴后整次停车累计少收降到 0 时，
  // 抵免已退回可用、本次未用掉，客人端不显示「消费抵免 −¥0.00」；只在实际抵掉小时数大于 0 时显示消费抵免一行。
  it("hidesCreditLineWhenNothingOffset：续缴累计少收为 0 时不显示消费抵免一行", async () => {
    const service = {
      getParkingCredit: vi.fn().mockResolvedValue({ projectId: "proj-1", businessDate: "2026-10-21", status: "used",
        eligibleAmountCent: 30000, thresholdCent: 20000, creditMinutes: 120 }),
      findParkingSession: vi.fn().mockResolvedValue({ parkingSessionId: "ps-3", projectId: "proj-1", plateNo: "粤A33333",
        enteredAt: "2026-10-21T02:00:00Z", grossFeeCent: 3000, feeCent: 3000, paidCent: 1000, dueCent: 2000, graceUntil: null,
        creditId: "pcr-3", creditStatus: "used", creditMinutes: 120, creditOffsetHours: 0 }),
      createParkingPayment: vi.fn(),
      confirmParkingPayment: vi.fn(),
    } as unknown as CustomerService;

    render(<ParkingPage service={service} initialProjectId="proj-1" initialCode="粤A33333" />);
    await waitFor(() => expect(screen.queryByTestId("parking-credit")?.textContent ?? "").toContain("已使用"));
    fireEvent.click(screen.getByRole("button", { name: "查询停车费" }));
    await waitFor(() => expect(screen.queryByTestId("parking-due")?.textContent ?? "").toContain("¥20.00"));
    expect(screen.queryByTestId("parking-credit-offset")).toBeNull();
    expect(screen.queryByText(/消费抵免（/)).toBeNull();
  });

  // 第十二批操作员裁决第 11 条「少收了就按少收金额折回的小时数」：先缴费、后达标再续缴时，实际少收＝停车费−max(抵后应收, 已缴)，
  // 与后端折回小时数同口径。停 4 小时 ¥40、已缴 ¥30、抵后 ¥20 → 实际只少收 ¥10，抵免一行显示 −¥10.00（不是 −¥20.00）。
  it("creditLineShowsActualShortfallWhenPaidExceedsFee：已缴高于抵后应收时按实际少收显示抵免", async () => {
    const service = {
      getParkingCredit: vi.fn().mockResolvedValue({ projectId: "proj-1", businessDate: "2026-10-21", status: "used",
        eligibleAmountCent: 30000, thresholdCent: 20000, creditMinutes: 120 }),
      findParkingSession: vi.fn().mockResolvedValue({ parkingSessionId: "ps-4", projectId: "proj-1", plateNo: "粤A44444",
        enteredAt: "2026-10-21T02:00:00Z", grossFeeCent: 4000, feeCent: 2000, paidCent: 3000, dueCent: 0, graceUntil: null,
        creditId: "pcr-4", creditStatus: "used", creditMinutes: 120, creditOffsetHours: 1 }),
      createParkingPayment: vi.fn(),
      confirmParkingPayment: vi.fn(),
    } as unknown as CustomerService;

    render(<ParkingPage service={service} initialProjectId="proj-1" initialCode="粤A44444" />);
    await waitFor(() => expect(screen.queryByTestId("parking-credit")?.textContent ?? "").toContain("已使用"));
    fireEvent.click(screen.getByRole("button", { name: "查询停车费" }));
    await waitFor(() => expect(screen.queryByTestId("parking-credit-offset")?.textContent ?? "").toContain("¥10.00"));
    expect(screen.getByTestId("parking-due").textContent).toContain("¥0.00");
  });

  // OpenAPI 客人停车缴费下单：支付渠道未配置时拉起参数与支付链接为空——客户端显示「支付暂不可用，未扣款」，不给「我已完成支付」。
  it("paymentUnavailableShowsNotCharged：在线支付未配置时显示「支付暂不可用，未扣款」，不给假的已付入口", async () => {
    const service = {
      getParkingCredit: vi.fn().mockResolvedValue({ projectId: "proj-1", businessDate: "2026-10-21", status: "not_reached",
        eligibleAmountCent: 0, thresholdCent: 20000, creditMinutes: 120 }),
      findParkingSession: vi.fn().mockResolvedValue({ parkingSessionId: "ps-9", projectId: "proj-1", plateNo: "粤A99999",
        enteredAt: "2026-10-21T02:00:00Z", feeCent: 1500, paidCent: 0, dueCent: 1500, graceUntil: null }),
      createParkingPayment: vi.fn().mockResolvedValue({ id: "ppay-9", amountCent: 1500, status: "pending", paymentUrl: null, launch: null }),
      confirmParkingPayment: vi.fn(),
    } as unknown as CustomerService;

    render(<ParkingPage service={service} initialProjectId="proj-1" initialCode="粤A99999" />);
    await waitFor(() => expect(screen.queryByRole("button", { name: "查询停车费" })).not.toBeNull());
    await waitFor(() => expect(screen.getByRole("button", { name: "查询停车费" })).toBeEnabled());
    fireEvent.click(screen.getByRole("button", { name: "查询停车费" }));
    await waitFor(() => expect(screen.queryByRole("button", { name: "去缴费" })).not.toBeNull());
    await waitFor(() => expect(screen.getByRole("button", { name: "去缴费" })).toBeEnabled());
    fireEvent.click(screen.getByRole("button", { name: "去缴费" }));
    await waitFor(() => expect(screen.queryByTestId("parking-payment-unavailable")?.textContent ?? "").toContain("支付暂不可用，未扣款"));
    expect(screen.queryByRole("button", { name: "我已完成支付" })).toBeNull();
    expect(service.confirmParkingPayment).not.toHaveBeenCalled();
  });

  // BSM L1435「出口扫码支付，或在官方 App/小程序提前缴费后在离场宽限时限内离场」：从首页入口进入（不带项目）也能按车牌查费——
  // 只有一个综合体就直接用它，所用综合体报给外壳（请求带 X-Project-Id）。
  const project = (projectId: string, projectName: string) => ({ projectId, projectName, address: null, intro: null, announcement: null,
    businessHours: [] });
  const lookupService = () => ({
    listProjects: vi.fn().mockResolvedValue([project("proj-1", "长沙综合体")]),
    getParkingCredit: vi.fn().mockResolvedValue({ projectId: "proj-1", businessDate: "2026-10-24", status: "not_reached",
      eligibleAmountCent: 0, thresholdCent: 20000, creditMinutes: 120 }),
    findParkingSession: vi.fn().mockResolvedValue({ parkingSessionId: "ps-5", projectId: "proj-1", plateNo: "粤A55555",
      enteredAt: "2026-10-24T02:00:00Z", feeCent: 500, paidCent: 0, dueCent: 500, graceUntil: null }),
    createParkingPayment: vi.fn(),
    confirmParkingPayment: vi.fn(),
  });

  it("homeEntryWithoutProjectUsesTheOnlyProject：首页入口进入、只有一个综合体时直接按车牌查费", async () => {
    const service = lookupService();
    const onProject = vi.fn();
    render(<ParkingPage service={service as unknown as CustomerService} onProject={onProject} />);
    await waitFor(() => expect(screen.queryByLabelText("车牌号或临时码")).not.toBeNull());
    fireEvent.change(screen.getByLabelText("车牌号或临时码"), { target: { value: "粤A55555" } });
    await waitFor(() => expect(screen.getByRole("button", { name: "查询停车费" })).toBeEnabled());
    fireEvent.click(screen.getByRole("button", { name: "查询停车费" }));
    await waitFor(() => expect(service.findParkingSession).toHaveBeenCalledWith("proj-1", "粤A55555"));
    await waitFor(() => expect(screen.queryByTestId("parking-due")?.textContent ?? "").toContain("¥5.00"));
    expect(onProject).toHaveBeenCalledWith("proj-1");
    expect(screen.queryByText(/请扫描停车场出口二维码/)).toBeNull();
  });

  // 多个综合体时由客人选停车场所在综合体再查；客人端已暂停服务的综合体不列（PGM「关闭后菜单不可见或不可点」）。
  it("homeEntryWithSeveralProjectsLetsGuestChoose：多个综合体先选再查，暂停服务的不列", async () => {
    const service = lookupService();
    service.listProjects.mockResolvedValue([project("proj-1", "长沙综合体"), project("proj-2", "株洲综合体"), project("proj-3", "湘潭综合体")]);
    const onProject = vi.fn();
    render(<ParkingPage service={service as unknown as CustomerService} onProject={onProject} projectOpen={(id: string) => id !== "proj-3"} />);
    await waitFor(() => expect(screen.queryByRole("combobox", { name: "停车场所在综合体" })).not.toBeNull());
    fireEvent.change(screen.getByLabelText("车牌号或临时码"), { target: { value: "粤A55555" } });
    expect(screen.getByRole("button", { name: "查询停车费" })).toBeDisabled();
    fireEvent.mouseDown(screen.getByRole("combobox", { name: "停车场所在综合体" }));
    await waitFor(() => expect(screen.queryByTitle("株洲综合体")).not.toBeNull());
    expect(screen.queryByTitle("湘潭综合体")).toBeNull();
    fireEvent.click(screen.getByTitle("株洲综合体"));
    await waitFor(() => expect(screen.getByRole("button", { name: "查询停车费" })).toBeEnabled());
    fireEvent.click(screen.getByRole("button", { name: "查询停车费" }));
    await waitFor(() => expect(service.findParkingSession).toHaveBeenCalledWith("proj-2", "粤A55555"));
    expect(onProject).toHaveBeenCalledWith("proj-2");
  });
});
