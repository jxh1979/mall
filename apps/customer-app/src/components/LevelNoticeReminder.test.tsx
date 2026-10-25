// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import type { CustomerService, LevelNotice } from "@complex/customer-core";

import { LevelNoticeReminder } from "./LevelNoticeReminder";

// OBS-13-01（BSM L820）：降级预警告知按当日成长值测算将降至哪一等级并说明只是提醒；「已保级」通知；降级生效后补发通知。
// 等级通知只在官方 App／小程序站内弹出（BSM L1596 客人短信清单不含等级通知），点「知道了」回写已读。
const notices: LevelNotice[] = [
  { id: "lvn-1", noticeType: "downgrade_warning", currentLevel: "gold", currentLevelName: "金卡", targetLevel: "silver", targetLevelName: "银卡",
    cycleEnd: "2031-06-15T04:00:00Z", createdAt: "2031-05-17T04:00:00Z" },
  { id: "lvn-2", noticeType: "downgraded", currentLevel: "silver", currentLevelName: "银卡", targetLevel: "base", targetLevelName: "新乐会员",
    cycleEnd: null, createdAt: "2031-06-16T04:00:00Z" },
];

describe("LevelNoticeReminder（会员等级通知）", () => {
  it("弹出降级预警与降级通知，点知道了回写已读", async () => {
    const markLevelNoticeRead = vi.fn().mockResolvedValue(null);
    const service = { listLevelNotices: vi.fn().mockResolvedValue(notices), markLevelNoticeRead } as unknown as CustomerService;
    render(<LevelNoticeReminder service={service} />);
    await waitFor(() => expect(screen.queryByTestId("level-notice-reminder")?.textContent ?? "").toContain("保护期结束"));
    const text = screen.getByTestId("level-notice-reminder").textContent ?? "";
    expect(text).toContain("将降至银卡");
    expect(text).toContain("这只是提醒");
    expect(text).toContain("调整为新乐会员");
    expect(text).not.toContain("silver");
    fireEvent.click(screen.getByText("知道了"));
    await waitFor(() => expect(markLevelNoticeRead).toHaveBeenCalledTimes(2));
    expect(markLevelNoticeRead).toHaveBeenCalledWith("lvn-1");
    expect(markLevelNoticeRead).toHaveBeenCalledWith("lvn-2");
    await waitFor(() => expect(screen.queryByTestId("level-notice-reminder")).toBeNull());
  });

  it("已保级通知", async () => {
    const service = {
      listLevelNotices: vi.fn().mockResolvedValue([{ id: "lvn-3", noticeType: "level_kept", currentLevel: "gold", currentLevelName: "金卡",
        targetLevel: "gold", targetLevelName: "金卡", cycleEnd: "2031-06-15T04:00:00Z", createdAt: "2031-05-20T04:00:00Z" }]),
      markLevelNoticeRead: vi.fn().mockResolvedValue(null),
    } as unknown as CustomerService;
    render(<LevelNoticeReminder service={service} />);
    await waitFor(() => expect(screen.queryByTestId("level-notice-reminder")?.textContent ?? "").toContain("已重新达到金卡门槛"));
  });
});
