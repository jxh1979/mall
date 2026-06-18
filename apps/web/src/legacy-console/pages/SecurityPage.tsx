import { SafetyCertificateOutlined } from "@ant-design/icons";
import { Button, Form, Input, Select, Tag } from "antd";
import type { FormInstance } from "antd/es/form";
import { formatTime, statusColor } from "../formatters";
import type { Snapshot } from "../types";
import { SectionHeading } from "../ui";

type SecurityFormValues = Record<string, string>;

const TEXT = {
  pageTitle: "\u98ce\u63a7\u9884\u8b66",
  createTitle: "\u521b\u5efa\u98ce\u9669\u9884\u8b66",
  defaultSummary: "P0 \u9875\u9762\u4eba\u5de5\u98ce\u9669\u9884\u8b66",
  rule: "\u89c4\u5219",
  level: "\u7b49\u7ea7",
  type: "\u7c7b\u578b",
  objectType: "\u5bf9\u8c61\u7c7b\u578b",
  summary: "\u6458\u8981",
  createButton: "\u521b\u5efa\u9884\u8b66",
  openTitle: "\u672a\u5904\u7406\u9884\u8b66",
  resolve: "\u5904\u7406",
};

export type SecurityPageProps = {
  snapshotValue: Snapshot;
  riskForm: FormInstance;
  handleRiskAlert: (values: SecurityFormValues) => void | Promise<void>;
  handleResolveRisk: (alertId: string) => void | Promise<void>;
};

export function SecurityPage({ snapshotValue, riskForm, handleRiskAlert, handleResolveRisk }: SecurityPageProps) {
  return (
    <section className="section-block">
      <SectionHeading icon={<SafetyCertificateOutlined />} title={TEXT.pageTitle} />
      <div className="action-grid two">
        <div className="action-panel">
          <h2>{TEXT.createTitle}</h2>
          <Form form={riskForm} layout="vertical" onFinish={handleRiskAlert} initialValues={{ riskLevel: "normal", alertType: "manual", summary: TEXT.defaultSummary }}>
            <div className="form-grid two">
              <Form.Item name="ruleId" label={TEXT.rule}><Select allowClear options={snapshotValue.riskRules.map((item) => ({ value: item.id, label: item.ruleName }))} /></Form.Item>
              <Form.Item name="riskLevel" label={TEXT.level}><Select options={["normal", "suspicious", "high"].map((value) => ({ value, label: value }))} /></Form.Item>
              <Form.Item name="alertType" label={TEXT.type}><Input /></Form.Item>
              <Form.Item name="objectType" label={TEXT.objectType}><Input /></Form.Item>
              <Form.Item name="summary" label={TEXT.summary} rules={[{ required: true }]}><Input /></Form.Item>
            </div>
            <Button type="primary" htmlType="submit">{TEXT.createButton}</Button>
          </Form>
        </div>
        <div className="action-panel">
          <h2>{TEXT.openTitle}</h2>
          <div className="record-list">
            {snapshotValue.riskAlerts.slice(0, 10).map((alert) => (
              <div className="record-row" key={alert.id}>
                <div><strong>{alert.summary}</strong><small>{alert.alertType} ? {formatTime(alert.createdAt)}</small></div>
                <div className="row-actions"><Tag color={statusColor(alert.riskLevel)}>{alert.riskLevel}</Tag>{alert.status === "open" ? <Button size="small" onClick={() => void handleResolveRisk(alert.id)}>{TEXT.resolve}</Button> : <Tag>{alert.status}</Tag>}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
