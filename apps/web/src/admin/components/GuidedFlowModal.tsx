// 引导式流程范式约定（2026-07-06 用户拍板，铺全站时照做）：
// 1. 页面保留醒目「＋ 新建」入口按钮（PermissionButton，权限门在入口，不在弹窗内提交按钮）。
// 2. 字段状态由页面持有，本组件只做步进/校验/确认/提交编排。
// 3. 提交失败 = 弹窗留开 + 通过 error prop 在弹窗内显错（try/catch → setXxxError），绝不静默关闭。
// 4. 原有 data-testid 原样保留在弹窗内输入与提交按钮上（e2e/门禁依赖）。
import React from "react";
import { Button, Modal, Steps } from "antd";

// 引导式流程弹窗（一期范式，2026-07-06 用户拍板；2026-07-07 用户细化）：
// 页面上只留一个醒目的功能按钮，点击后弹出引导弹窗。
// 默认 singlePage：所有字段并到一屏（各步 content 作为分节堆叠）→「下一步」一次跑完各步校验 → 确认页核对 → 提交。
// 仅当"后一步要用前一步结果做判断"时才传 singlePage={false} 走逐步分步。
// 字段仍由调用方受控（state 留在页面），本组件只负责校验/确认/提交编排，
// 原有 data-testid 原样保留在各输入上（e2e/门禁依赖）。

export type GuidedStep = {
  /** 步骤标题（中文）。singlePage 下作为分节小标题。 */
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
  /** 单屏模式（默认 true）：所有步字段并到一屏。仅当后步依赖前步结果时传 false 走逐步。 */
  singlePage?: boolean;
};

export function GuidedFlowModal({ open, title, steps, summary, submitLabel = "创建", submitTestId, submitting = false, error, onSubmit, onClose, testId, singlePage = true }: Props) {
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

  // singlePage：填写(0) → 确认(1)；逐步：每步一页 → 确认(steps.length)。
  const confirmIndex = singlePage ? 1 : steps.length;
  const items = singlePage
    ? [{ title: "填写信息" }, { title: "确认信息" }]
    : [...steps.map((step) => ({ title: step.title })), { title: "确认信息" }];
  const onForm = current < confirmIndex;
  const activeStep = !singlePage && current < steps.length ? steps[current] : undefined;

  function next() {
    // singlePage 一次跑完所有步校验取第一个失败；逐步只校验当前步。
    const message = singlePage
      ? (steps.map((step) => step.validate?.() ?? null).find((m) => m) ?? null)
      : (steps[current]?.validate?.() ?? null);
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

  // 页脚用 antd Button + 不带 admin-form-row 类：原生<button>/带admin类的按钮在 antd 弹窗(portal 渲染于 body、
  // 在 .admin-* 作用域外)里,css 变量 --adm-accent 取不到→background 塌成透明=按钮隐形(关键缺陷修复)。
  // 去掉 admin-form-row 类,让 antd Button 自带样式生效;内联 flex 只管布局不碰按钮外观。
  const footer = (
    <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
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
        {onForm ? (
          singlePage ? (
            // 一屏堆叠所有步：每步一个带小标题的分节，各自套 admin-form-grid，避免网格跨列 CSS。
            <>
              {steps.map((step, index) => (
                <div key={`${step.title}-${index}`} style={{ marginBottom: index < steps.length - 1 ? 12 : 0 }}>
                  {steps.length > 1 ? <div style={{ fontWeight: 600, margin: "4px 0" }}>{step.title}</div> : null}
                  {step.description ? <p className="admin-muted-action">{step.description}</p> : null}
                  <div className="admin-form-grid">{step.content}</div>
                </div>
              ))}
            </>
          ) : (
            <>
              {activeStep?.description ? <p className="admin-muted-action">{activeStep.description}</p> : null}
              <div className="admin-form-grid">{activeStep?.content}</div>
            </>
          )
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
