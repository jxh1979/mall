import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import React from "react";

import { GuidedFlowModal, type GuidedStep } from "./GuidedFlowModal";

// 引导式流程弹窗自身行为：步进/校验拦截/确认页汇总/提交回调/取消。
// 字段受控在调用方，这里用最小两步（一步带校验）覆盖组件编排逻辑。

function Harness({ onSubmit, onClose }: { onSubmit: () => void; onClose: () => void }) {
  const [name, setName] = React.useState("");
  const steps: GuidedStep[] = [
    {
      title: "基本信息",
      description: "填写名称",
      validate: () => (name.trim() ? null : "名称不能为空。"),
      content: <label>名称<input data-testid="flow-name" value={name} onChange={(e) => setName(e.target.value)} /></label>,
    },
    {
      title: "补充信息",
      content: <label>备注<input data-testid="flow-note" /></label>,
    },
  ];
  return (
    <GuidedFlowModal
      open
      title="新建示例"
      testId="demo-flow"
      submitTestId="demo-flow-submit"
      singlePage={false}
      steps={steps}
      summary={[{ label: "名称", value: name }]}
      onSubmit={onSubmit}
      onClose={onClose}
    />
  );
}

describe("GuidedFlowModal 分步引导", () => {
  it("校验不通过 → 显示中文错误且停在本步；通过后进入下一步", () => {
    render(<Harness onSubmit={vi.fn()} onClose={vi.fn()} />);
    expect(screen.getByTestId("flow-name")).toBeTruthy();
    fireEvent.click(screen.getByTestId("demo-flow-next"));
    expect(screen.getByTestId("demo-flow-error").textContent).toContain("名称不能为空");
    expect(screen.getByTestId("flow-name")).toBeTruthy();

    fireEvent.change(screen.getByTestId("flow-name"), { target: { value: "示例商户" } });
    fireEvent.click(screen.getByTestId("demo-flow-next"));
    expect(screen.queryByTestId("demo-flow-error")).toBeNull();
    expect(screen.getByTestId("flow-note")).toBeTruthy();
  });

  it("上一步可回退；最后一步是确认页汇总 + 主按钮提交回调", () => {
    const onSubmit = vi.fn();
    render(<Harness onSubmit={onSubmit} onClose={vi.fn()} />);
    fireEvent.change(screen.getByTestId("flow-name"), { target: { value: "示例商户" } });
    fireEvent.click(screen.getByTestId("demo-flow-next"));
    fireEvent.click(screen.getByTestId("demo-flow-prev"));
    expect(screen.getByTestId("flow-name")).toBeTruthy();

    fireEvent.click(screen.getByTestId("demo-flow-next"));
    fireEvent.click(screen.getByTestId("demo-flow-next"));
    const summary = screen.getByTestId("demo-flow-summary");
    expect(summary.textContent).toContain("名称");
    expect(summary.textContent).toContain("示例商户");
    fireEvent.click(screen.getByTestId("demo-flow-submit"));
    expect(onSubmit).toHaveBeenCalledTimes(1);
  });

  it("取消按钮触发 onClose", () => {
    const onClose = vi.fn();
    render(<Harness onSubmit={vi.fn()} onClose={onClose} />);
    fireEvent.click(screen.getByTestId("demo-flow-cancel"));
    expect(onClose).toHaveBeenCalled();
  });
});
