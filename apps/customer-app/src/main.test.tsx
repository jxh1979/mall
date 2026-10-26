// 文件版本：V0.01
import { waitFor } from "@testing-library/react";
import { message } from "antd";
import { describe, expect, it, vi } from "vitest";

// 第 10 条（技术缺陷）：antd v5 的静态提示（message／Modal／notification）在 React 19 下要入口先接 antd 官方兼容补丁才渲染得出来；
// 本例走真实入口 main.tsx（只把客人端外壳换成空组件，免得拉接口），断言「验证码已发送」这类静态提示能弹出。
vi.mock("./CustomerApp", () => ({ default: () => null }));

describe("客人 App 入口：React 19 下 antd 静态提示", () => {
  it("经入口挂载后 message.success 能弹出", async () => {
    document.body.innerHTML = '<div id="root"></div>';
    await import("./main");
    void message.success("验证码已发送");
    await waitFor(() => expect(document.body.textContent).toContain("验证码已发送"));
  });
});
