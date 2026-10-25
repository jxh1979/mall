// 文件版本：V0.01
import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import { HomePage } from "./HomePage";
import type { CustomerService } from "@complex/customer-core";

describe("HomePage", () => {
  afterEach(() => vi.restoreAllMocks());

  it("renders ticket products from the service", async () => {
    const service = {
      listProjects: vi.fn().mockResolvedValue([]),
      listTicketProducts: vi.fn().mockResolvedValue([
        {
          ticketTypeId: "t-1",
          ticketName: "平日票",
          weekdayPriceCent: 9900,
          weekendPriceCent: 12900,
          holidayPriceCent: 15900,
          currency: "CNY",
          status: "active",
        },
      ]),
    } as unknown as CustomerService;

    render(<HomePage service={service} />);

    await waitFor(() => expect(screen.queryByText("平日票")).not.toBeNull());
    await waitFor(() => expect(screen.getByText("平日票")).toBeInTheDocument());
    expect(screen.getByText(/平日 ¥99.00/)).toBeInTheDocument();
  });

  it("shows an error alert when loading fails", async () => {
    const service = {
      listProjects: vi.fn().mockResolvedValue([]),
      listTicketProducts: vi.fn().mockRejectedValue(new Error("network down")),
    } as unknown as CustomerService;

    render(<HomePage service={service} />);

    await waitFor(() => expect(screen.queryByText("network down")).not.toBeNull());
    await waitFor(() => expect(screen.getByText("network down")).toBeInTheDocument());
  });

  // BSM「查看综合体介绍、营业时间、活动公告、票务入口」；操作员裁决第十三批第 5 条「上级审批后发布：运营编辑后走审批，批准才对客人展示」
  // ——服务端 GET /api/v1/customer/projects 只回已批准发布的介绍与公告（未发布为空），客人端不展示空的公告。
  it("showsApprovedIntroAnnouncementAndBusinessHours：首页展示审批通过的综合体介绍、活动公告与营业时间", async () => {
    const service = {
      listTicketProducts: vi.fn().mockResolvedValue([]),
      listAmusementProjects: vi.fn().mockResolvedValue([]),
      listProjects: vi.fn().mockResolvedValue([
        { projectId: "p-1", projectName: "长沙综合体", address: "长沙市某路 1 号", intro: "城市夜间娱乐综合体", announcement: "国庆夜场加开",
          businessHours: [{ venueId: "v-1", venueName: "酒吧街", dayOfWeek: 5, openTime: "20:00", closeTime: "02:00" },
            { venueId: "v-1", venueName: "酒吧街", dayOfWeek: 6, openTime: "10:00", closeTime: "22:00" }] },
        { projectId: "p-2", projectName: "株洲综合体", address: null, intro: null, announcement: null, businessHours: [] },
      ]),
    } as unknown as CustomerService;

    render(<HomePage service={service} />);

    await waitFor(() => expect(screen.queryByText("长沙综合体")).not.toBeNull());
    await waitFor(() => expect(screen.getByText("长沙综合体")).toBeInTheDocument());
    expect(screen.getByText("城市夜间娱乐综合体")).toBeInTheDocument();
    expect(screen.getByText("国庆夜场加开")).toBeInTheDocument();
    expect(screen.getAllByTestId("home-announcement")).toHaveLength(1);
    const hours = screen.getAllByTestId("home-business-hours").map((node) => node.textContent ?? "").join("\n");
    expect(hours).toContain("酒吧街");
    expect(hours).toContain("周五 20:00–次日 02:00");
    expect(hours).toContain("周六 10:00–22:00");
    expect(screen.getByText("株洲综合体")).toBeInTheDocument();
  });

  // BSM L858「查看票种、套餐、通票、游乐项目和酒吧规则」、L878「连接游乐系统，展示可预定项目和场次」：每个综合体只读列出开放中的游乐项目与
  // 尚未结束的场次（起止时间、状态、余量），只展示、不在此预约。
  it("showsAmusementProjectsAndSessionsPerProject：首页按综合体列出游乐项目与可预定场次", async () => {
    const service = {
      listTicketProducts: vi.fn().mockResolvedValue([]),
      listProjects: vi.fn().mockResolvedValue([
        { projectId: "p-1", projectName: "长沙综合体", address: null, intro: null, announcement: null, businessHours: [] },
      ]),
      listAmusementProjects: vi.fn().mockResolvedValue([
        { amusementProjectId: "ap-1", projectName: "旋转木马", openStatus: "open", sessions: [
          { sessionId: "s-1", startTime: "2026-10-25T12:00:00Z", endTime: "2026-10-25T13:00:00Z", capacity: 20, usedCapacity: 5, status: "scheduled" },
          { sessionId: "s-2", startTime: "2026-10-25T14:00:00Z", endTime: "2026-10-25T15:00:00Z", capacity: 10, usedCapacity: 10, status: "open" }] },
        { amusementProjectId: "ap-2", projectName: "碰碰车", openStatus: "open", sessions: [] },
      ]),
    } as unknown as CustomerService;

    render(<HomePage service={service} />);

    await waitFor(() => expect(screen.queryByTestId("home-amusement")?.textContent ?? "").toContain("旋转木马"));
    expect(service.listAmusementProjects).toHaveBeenCalledWith("p-1");
    const text = screen.getByTestId("home-amusement").textContent ?? "";
    expect(text).toContain("游乐项目");
    expect(text).toContain("已排期");
    expect(text).toContain("余 15 / 20");
    expect(text).toContain("进行中");
    expect(text).toContain("余 0 / 10");
    expect(text).toContain("碰碰车");
    expect(text).toContain("暂无可预定场次");
  });
});
