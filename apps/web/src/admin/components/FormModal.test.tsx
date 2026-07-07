import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { FormModal } from "./FormModal";

// 单页表单弹窗：字段一屏、底部【取消】【提交】；提交前跑 validate 拦截；关闭态直接卸载。
function renderModal(props: Partial<React.ComponentProps<typeof FormModal>> = {}) {
  const onSubmit = vi.fn();
  const onClose = vi.fn();
  render(
    <FormModal
      open
      title="新建示例"
      testId="demo-form"
      submitTestId="demo-submit"
      submitLabel="创建"
      onSubmit={onSubmit}
      onClose={onClose}
      {...props}
    >
      <label>名称<input aria-label="名称" data-testid="demo-name" /></label>
    </FormModal>,
  );
  return { onSubmit, onClose };
}

describe("FormModal 单页表单弹窗", () => {
  it("关闭态不渲染", () => {
    render(<FormModal open={false} title="x" testId="demo-form" submitTestId="demo-submit" onSubmit={vi.fn()} onClose={vi.fn()}><span /></FormModal>);
    expect(screen.queryByTestId("demo-form")).toBeNull();
  });

  it("打开：一屏展示字段 + 取消/提交按钮，无步进 next", () => {
    renderModal();
    expect(screen.getByTestId("demo-form")).toBeTruthy();
    expect(screen.getByTestId("demo-name")).toBeTruthy();
    expect(screen.getByTestId("demo-submit")).toBeTruthy();
    expect(screen.getByTestId("demo-form-cancel")).toBeTruthy();
    expect(screen.queryByTestId("demo-form-next")).toBeNull();
  });

  it("validate 返回错误 → 显示错误、不提交", () => {
    const { onSubmit } = renderModal({ validate: () => "名称必填" });
    fireEvent.click(screen.getByTestId("demo-submit"));
    expect(screen.getByTestId("demo-form-error").textContent).toContain("名称必填");
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it("validate 放行 → 提交", () => {
    const { onSubmit } = renderModal({ validate: () => null });
    fireEvent.click(screen.getByTestId("demo-submit"));
    expect(onSubmit).toHaveBeenCalledTimes(1);
  });

  it("取消 → onClose", () => {
    const { onClose } = renderModal();
    fireEvent.click(screen.getByTestId("demo-form-cancel"));
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
