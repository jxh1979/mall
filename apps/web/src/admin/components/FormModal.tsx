import React from "react";
import { Button, Modal } from "antd";

// 单页表单弹窗（2026-07-07 用户拍板："引导式不用每步都引导；多数场景弹一个页面、所有字段一屏摆开、填完直接提交更清晰更快。
// 只有必须按顺序全部填齐才能继续的，才用多步 GuidedFlowModal"）。
// 与 GuidedFlowModal 同一套 testId/按钮约定，但没有步进条与"确认信息"页：字段一屏、底部只有【取消】【提交】。
// 关键：页脚用 antd Button 且不套 admin-form-row 类——原生按钮/带 admin 类的按钮在 antd 弹窗 portal（渲染于 body、
//   在 .admin-* css 变量作用域外）里 background 会塌成透明=隐形（见 GuidedFlowModal 同款修复）。

type Props = {
  open: boolean;
  /** 弹窗标题（中文）。 */
  title: string;
  /** 表单字段（调用方传 <label>字段<input/></label> 列表，沿用 admin-form-grid 样式）。 */
  children: React.ReactNode;
  /** 说明（可选，中文）。 */
  description?: string;
  /** 提交前校验：返回中文错误文案则拦截，返回 null 放行。 */
  validate?: () => string | null;
  /** 主按钮文案，默认「创建」。 */
  submitLabel?: string;
  /** 主按钮 data-testid：沿用原提交按钮的 testid，保持 e2e/门禁兼容。 */
  submitTestId: string;
  submitting?: boolean;
  /** 外部提交错误（接口失败等），显示在弹窗内。 */
  error?: string;
  onSubmit: () => void;
  onClose: () => void;
  /** 弹窗容器 testid，派生 -cancel/-error。 */
  testId: string;
};

export function FormModal({ open, title, children, description, validate, submitLabel = "创建", submitTestId, submitting = false, error, onSubmit, onClose, testId }: Props) {
  const [stepError, setStepError] = React.useState("");

  React.useEffect(() => {
    if (open) setStepError("");
  }, [open]);

  // ponytail: 不渲染关闭态（直接卸载），跳过 antd 关闭动画在 jsdom 里的残留问题（同 GuidedFlowModal）。
  if (!open) return null;

  function submit() {
    const message = validate?.() ?? null;
    if (message) {
      setStepError(message);
      return;
    }
    setStepError("");
    onSubmit();
  }

  const footer = (
    <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
      <Button data-testid={`${testId}-cancel`} onClick={onClose}>取消</Button>
      <Button type="primary" data-testid={submitTestId} disabled={submitting} loading={submitting} onClick={submit}>{submitLabel}</Button>
    </div>
  );

  return (
    <Modal open title={title} onCancel={onClose} footer={footer} width={640}>
      <div data-testid={testId}>
        {description ? <p className="admin-muted-action">{description}</p> : null}
        <div className="admin-form-grid">{children}</div>
        {stepError || error ? <div className="admin-inline-error" data-testid={`${testId}-error`}>{stepError || error}</div> : null}
      </div>
    </Modal>
  );
}
