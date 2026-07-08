import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { CnDatePicker } from "./CnDatePicker";

describe("CnDatePicker", () => {
  it("传 value 显示中文格式且无英文/斜杠", () => {
    render(<CnDatePicker value="2026-07-05" onChange={() => {}} data-testid="d" />);
    const trigger = screen.getByTestId("d");
    expect(trigger.textContent).toContain("2026年7月5日");
    expect(trigger.textContent).not.toMatch(/\//);
    expect(trigger.textContent).not.toMatch(/[A-Za-z]/);
  });

  it("点开日历选一天回传 YYYY-MM-DD 并关闭", () => {
    const onChange = vi.fn();
    render(<CnDatePicker value="2026-07-05" onChange={onChange} data-testid="d" />);
    fireEvent.click(screen.getByTestId("d"));
    // 星期行中文
    expect(screen.getByText("日")).not.toBeNull();
    expect(screen.getByText("2026年7月")).not.toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "12" }));
    expect(onChange).toHaveBeenCalledWith("2026-07-12");
    // 选后关闭
    expect(screen.queryByText("2026年7月")).toBeNull();
  });

  it("上/下月切换改变标题", () => {
    render(<CnDatePicker value="2026-07-05" onChange={() => {}} data-testid="d" />);
    fireEvent.click(screen.getByTestId("d"));
    fireEvent.click(screen.getByRole("button", { name: "下个月" }));
    expect(screen.getByText("2026年8月")).not.toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "上个月" }));
    fireEvent.click(screen.getByRole("button", { name: "上个月" }));
    expect(screen.getByText("2026年6月")).not.toBeNull();
  });

  it("月模式显示中文年月并回传 YYYY-MM", () => {
    const onChange = vi.fn();
    render(<CnDatePicker mode="month" value="2026-07" onChange={onChange} data-testid="m" />);
    const trigger = screen.getByTestId("m");
    expect(trigger.textContent).toContain("2026年7月");
    expect(trigger.textContent).not.toMatch(/[A-Za-z]/);
    fireEvent.click(trigger);
    expect(screen.getByText("2026年")).not.toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "3月" }));
    expect(onChange).toHaveBeenCalledWith("2026-03");
  });

  it("Esc 关闭日历", () => {
    render(<CnDatePicker value="2026-07-05" onChange={() => {}} data-testid="d" />);
    fireEvent.click(screen.getByTestId("d"));
    expect(screen.getByText("2026年7月")).not.toBeNull();
    fireEvent.keyDown(document, { key: "Escape" });
    expect(screen.queryByText("2026年7月")).toBeNull();
  });

  it("空态显示中文占位", () => {
    render(<CnDatePicker value="" onChange={() => {}} data-testid="d" />);
    expect(screen.getByTestId("d").textContent).toContain("选择日期");
    render(<CnDatePicker mode="month" value="" onChange={() => {}} data-testid="m" />);
    expect(screen.getByTestId("m").textContent).toContain("选择月份");
  });

  it("日模式点标题下钻选年→选月→选日，快速跳到远期日期", () => {
    const onChange = vi.fn();
    render(<CnDatePicker value="2026-07-05" onChange={onChange} data-testid="d" />);
    fireEvent.click(screen.getByTestId("d"));
    fireEvent.click(screen.getByRole("button", { name: "选择年月" }));
    expect(screen.getByText("2026年")).not.toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "选择年份" }));
    fireEvent.click(screen.getByRole("button", { name: "2030" }));
    fireEvent.click(screen.getByRole("button", { name: "8月" }));
    fireEvent.click(screen.getByRole("button", { name: "15" }));
    expect(onChange).toHaveBeenCalledWith("2030-08-15");
  });

  it("月模式点年标题下钻选年再选月回传 YYYY-MM", () => {
    const onChange = vi.fn();
    render(<CnDatePicker mode="month" value="2026-07" onChange={onChange} data-testid="m" />);
    fireEvent.click(screen.getByTestId("m"));
    fireEvent.click(screen.getByRole("button", { name: "选择年份" }));
    fireEvent.click(screen.getByRole("button", { name: "2028" }));
    fireEvent.click(screen.getByRole("button", { name: "5月" }));
    expect(onChange).toHaveBeenCalledWith("2028-05");
  });
});
