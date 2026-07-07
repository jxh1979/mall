// 引导式流程范式约定（2026-07-06 用户拍板，铺全站时照做）：
// 1. 页面保留醒目「＋ 新建」入口按钮（PermissionButton，权限门在入口，不在弹窗内提交按钮）。
// 2. 字段状态由页面持有，本组件只做步进/校验/确认/提交编排。
// 3. 提交失败 = 弹窗留开 + 通过 error prop 在弹窗内显错（try/catch → setXxxError），绝不静默关闭。
// 4. 原有 data-testid 原样保留在弹窗内输入与提交按钮上（e2e/门禁依赖）。
import React from "react";
import { Button, Modal, Steps } from "antd";

// 引导式流程弹窗（一期范式，2026-07-06 用户拍板）：
// 页面上只留一个醒目的功能按钮，点击后弹出分步引导：每步一组相关字段（带标题/说明），
// 校验通过才能下一步；最后一步是「确认信息」汇总页，核对无误后点主按钮提交。
// 字段仍由调用方受控（state 留在页面），本组件只负责步进/校验/确认/提交编排，
// 原有 data-testid 原样保留在各输入上（e2e/门禁依赖）。后续页面逐批迁入此范式。

export type GuidedStep = {
  /** 步骤标题（中文）。 */
  title: string;
  /** 本步说明（可选，中文）。 */
  description?: string;
  /** 本步字段（调用方传 <label>字段<input/></label> 列表，沿用 admin-form-grid 样式）。 */
  content: React.ReactNode;
  /** 本步校验：返回中文错误文案则阻止下一步，返回 null 放行。 */
  validate?: () => string | null;
};

export type GuidedSummaryItem = { label: string; value: React.ReactNode };

type Props = {
  open: boolean;
  /** 弹窗标题（中文）。 */
  title: string;
  steps: GuidedStep[];
  /** 确认页汇总（已填项），提交前最后核对。 */
  summary: GuidedSummaryItem[];
  /** 主按钮文案，默认「创建」。 */
  submitLabel?: string;
  /** 主按钮 data-testid：沿用原提交按钮的 testid，保持 e2e/门禁兼容。 */
  submitTestId: string;
  /** 提交中禁用主按钮。 */
  submitting?: boolean;
  /** 外部提交错误（接口失败等），显示在弹窗内。 */
  error?: string;
  onSubmit: () => void;
  onClose: () => void;
  /** 弹窗容器 testid，派生 -next/-prev/-cancel/-summary/-error。 */
  testId: string;
};

export function GuidedFlowModal({ open, title, steps, summary, submitLabel = "创建", submitTestId, submitting = false, error, onSubmit, onClose, testId }: Props) {
  const [current, setCurrent] = React.useState(0);
  const [stepError, setStepError] = React.useState("");

  // 每次打开都从第一步开始，清掉上次的步内错误。
  React.useEffect(() => {
    if (open) {
      setCurrent(0);
      setStepError("");
    }
  }, [open]);

  // ponytail: 不渲染关闭态（直接卸载），跳过 antd 关闭动画在 jsdom 里的残留问题。
  if (!open) return null;

  const confirmIndex = steps.length;
  const items = [...steps.map((step) => ({ title: step.title })), { title: "确认信息" }];
  const activeStep = current < confirmIndex ? steps[current] : undefined;

  function next() {
    const message = steps[current]?.validate?.() ?? null;
    if (message) {
      setStepError(message);
      return;
    }
    setStepError("");
    setCurrent((value) => value + 1);
  }

  function prev() {
    setStepError("");
    setCurrent((value) => value - 1);
  }

  // 页脚用 antd Button：原生 <button> 在 antd 弹窗内会被 antd 样式覆盖成"白字透明底"=隐形（关键缺陷修复）。
  const footer = (
    <div className="admin-form-row">
      <Button data-testid={`${testId}-cancel`} onClick={onClose}>取消</Button>
      {current > 0 ? <Button data-testid={`${testId}-prev`} onClick={prev}>上一步</Button> : null}
      {current < confirmIndex ? <Button type="primary" data-testid={`${testId}-next`} onClick={next}>下一步</Button> : null}
      {current === confirmIndex ? (
        <Button type="primary" data-testid={submitTestId} disabled={submitting} loading={submitting} onClick={onSubmit}>{submitLabel}</Button>
      ) : null}
    </div>
  );

  return (
    <Modal open title={title} onCancel={onClose} footer={footer} width={640}>
      <div data-testid={testId}>
        <Steps size="small" current={current} items={items} style={{ marginBottom: 16 }} />
        {activeStep ? (
          <>
            {activeStep.description ? <p className="admin-muted-action">{activeStep.description}</p> : null}
            <div className="admin-form-grid">{activeStep.content}</div>
          </>
        ) : (
          <div data-testid={`${testId}-summary`}>
            <p className="admin-muted-action">请核对以下信息，确认无误后点击「{submitLabel}」。</p>
            <dl className="admin-drawer-fields">
              {summary.map((item, index) => (
                <div className="admin-drawer-field" key={`${item.label}-${index}`}>
                  <dt className="admin-drawer-label">{item.label}</dt>
                  <dd className="admin-drawer-value">{item.value}</dd>
                </div>
              ))}
            </dl>
          </div>
        )}
        {stepError || error ? <div className="admin-inline-error" data-testid={`${testId}-error`}>{stepError || error}</div> : null}
      </div>
    </Modal>
  );
}
