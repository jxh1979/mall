import { ApiOutlined } from "@ant-design/icons";
import { Button, Form, Input, InputNumber, Select, Table, Tag } from "antd";
import type { FormInstance } from "antd/es/form";
import type { ColumnsType } from "antd/es/table";
import { formatMoney, formatTime, statusColor } from "../formatters";
import type { InterfaceItem, Snapshot } from "../types";
import { SectionHeading, SimpleTable } from "../ui";

const interfaceColumns: ColumnsType<InterfaceItem> = [
  { title: "编码", dataIndex: "code", key: "code", width: 190 },
  { title: "名称", dataIndex: "name", key: "name", width: 220 },
  { title: "方法", dataIndex: "method", key: "method", width: 78, render: (value) => <Tag color="blue">{value}</Tag> },
  { title: "路径", dataIndex: "path", key: "path" },
  { title: "状态", dataIndex: "status", key: "status", width: 96, render: (value) => <Tag>{value}</Tag> },
];


type InterfacesStringValues = Record<string, string>;

export type InterfacesPageProps = {
  snapshotValue: Snapshot;
  providerAdapterForm: FormInstance;
  handleWebhookReserved: () => void | Promise<void>;
  handleCreateProviderAdapter: (values: InterfacesStringValues) => void | Promise<void>;
};

export function InterfacesPage({
  snapshotValue,
  providerAdapterForm,
  handleWebhookReserved,
  handleCreateProviderAdapter,
}: InterfacesPageProps) {
  return (
    <section className="section-block">
      <SectionHeading icon={<ApiOutlined />} title="第三方预留接口与开放应用" extra={<Button onClick={() => void handleWebhookReserved()}>发送预留 Webhook</Button>} />
      <div className="action-grid two">
        <div className="action-panel">
          <h2>接口目录</h2>
          <Table className="data-table" columns={interfaceColumns} dataSource={snapshotValue.integration.interfaces} rowKey="code" pagination={false} size="small" />
        </div>
        <div className="action-panel">
          <h2>创建开放应用</h2>
          <Form form={providerAdapterForm} data-testid="provider-adapter-form" layout="vertical" onFinish={handleCreateProviderAdapter} initialValues={{ providerCode: `provider_${Date.now()}`, adapterType: "reservation_resource", mode: "sdk_api", webhookEnabled: "true", pollingIntervalMinutes: "10" }}>
            <div className="form-grid two">
              <Form.Item name="providerCode" label="\u4f9b\u5e94\u5546\u7f16\u7801" rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="adapterType" label="\u9002\u914d\u7c7b\u578b"><Select options={["reservation_resource", "payment", "face_age_check", "robot", "video", "device"].map((value) => ({ value, label: value }))} /></Form.Item>
              <Form.Item name="mode" label="\u63a5\u5165\u6a21\u5f0f"><Select options={["sdk_api", "openapi", "webhook", "manual_file"].map((value) => ({ value, label: value }))} /></Form.Item>
              <Form.Item name="webhookEnabled" label="Webhook"><Select options={[{ value: "true", label: "\u4f18\u5148" }, { value: "false", label: "\u6682\u4e0d\u542f\u7528" }]} /></Form.Item>
              <Form.Item name="pollingIntervalMinutes" label="\u8f6e\u8be2\u6821\u9a8c\u95f4\u9694\uff08\u5206\u949f\uff09"><InputNumber min={1} className="full" /></Form.Item>
              <Form.Item name="operatorNote" label="\u5907\u6ce8"><Input /></Form.Item>
            </div>
            <Button data-testid="create-provider-adapter-button" type="primary" htmlType="submit">{"\u521b\u5efa\u9002\u914d\u5668"}</Button>
          </Form>
        </div>
      </div>
      <SimpleTable title="\u4f9b\u5e94\u5546\u9002\u914d\u5668" data={snapshotValue.providerAdapters.slice(0, 12)} columns={[
        { title: "\u4f9b\u5e94\u5546", dataIndex: "providerCode" },
        { title: "\u7c7b\u578b", dataIndex: "adapterType" },
        { title: "\u6a21\u5f0f", dataIndex: "mode" },
        { title: "Webhook", dataIndex: "webhookEnabled", render: (value) => value ? <Tag color="green">on</Tag> : <Tag>off</Tag> },
        { title: "\u8f6e\u8be2", dataIndex: "pollingCheckEnabled", render: (value) => value ? <Tag color="blue">on</Tag> : <Tag>off</Tag> },
        { title: "\u72b6\u6001", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
      ]} />
      <div className="table-grid two">
        <SimpleTable title="P1 餐饮/零售订单同步" data={snapshotValue.thirdPartyOrderSyncs.slice(0, 12)} columns={[
          { title: "业务", dataIndex: "businessType" },
          { title: "类型", dataIndex: "syncType" },
          { title: "外部单号", dataIndex: "externalBusinessId" },
          { title: "金额", dataIndex: "amount", render: formatMoney },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="P1 券核销同步" data={snapshotValue.thirdPartyVoucherVerifications.slice(0, 12)} columns={[
          { title: "业务", dataIndex: "businessType" },
          { title: "外部核销号", dataIndex: "externalVerificationId" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          { title: "时间", dataIndex: "createdAt", render: formatTime },
        ]} />
        <SimpleTable title="P1 重试任务" data={snapshotValue.integrationRetryTasks.slice(0, 12)} columns={[
          { title: "业务", dataIndex: "businessType" },
          { title: "任务", dataIndex: "taskType" },
          { title: "次数", dataIndex: "attemptCount" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="P1 补偿任务" data={snapshotValue.integrationCompensationTasks.slice(0, 12)} columns={[
          { title: "业务", dataIndex: "businessType" },
          { title: "任务", dataIndex: "taskType" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          { title: "时间", dataIndex: "createdAt", render: formatTime },
        ]} />
        <SimpleTable title="P1 对账批次" data={snapshotValue.reconciliationBatches.slice(0, 12)} columns={[
          { title: "业务", dataIndex: "businessType" },
          { title: "日期", dataIndex: "reconDate" },
          { title: "差异数", dataIndex: "diffCount" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="P1 对账差异" data={snapshotValue.reconciliationDifferences.slice(0, 12)} columns={[
          { title: "业务", dataIndex: "businessType" },
          { title: "类型", dataIndex: "diffType" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          { title: "时间", dataIndex: "createdAt", render: formatTime },
        ]} />
        <SimpleTable title="P2 零售 SKU" data={snapshotValue.integrationSkus.slice(0, 12)} columns={[
          { title: "编码", dataIndex: "skuCode" },
          { title: "名称", dataIndex: "skuName" },
          { title: "分类", dataIndex: "category" },
          { title: "价格", dataIndex: "priceAmount", render: formatMoney },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="P2 机器人任务" data={snapshotValue.robotTasks.slice(0, 12)} columns={[
          { title: "任务号", dataIndex: "taskNo" },
          { title: "类型", dataIndex: "taskType" },
          { title: "机器人", dataIndex: "assignedRobotCode" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="P2 机器人事件" data={snapshotValue.robotTaskEvents.slice(0, 12)} columns={[
          { title: "事件", dataIndex: "eventType" },
          { title: "状态", dataIndex: "taskStatus", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          { title: "时间", dataIndex: "occurredAt", render: formatTime },
        ]} />
        <SimpleTable title="P2 摄像头" data={snapshotValue.cameraDevices.slice(0, 12)} columns={[
          { title: "编码", dataIndex: "cameraCode" },
          { title: "名称", dataIndex: "cameraName" },
          { title: "服务商", dataIndex: "provider" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="P2 视频证据" data={snapshotValue.videoEvidenceLinks.slice(0, 12)} columns={[
          { title: "证据号", dataIndex: "evidenceNo" },
          { title: "事件", dataIndex: "eventType" },
          { title: "对象", dataIndex: "objectType" },
          { title: "调阅", dataIndex: "viewCount" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="P2 企微绑定" data={snapshotValue.wecomBindings.slice(0, 12)} columns={[
          { title: "企业", dataIndex: "corpId" },
          { title: "员工", dataIndex: "staffUserId" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="P2 企微会话" data={snapshotValue.wecomConversations.slice(0, 12)} columns={[
          { title: "会话", dataIndex: "conversationKey" },
          { title: "员工", dataIndex: "staffUserId" },
          { title: "客户", dataIndex: "customerName" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="P2 企微消息" data={snapshotValue.wecomMessages.slice(0, 12)} columns={[
          { title: "消息", dataIndex: "msgId" },
          { title: "发送方", dataIndex: "senderType" },
          { title: "风险", dataIndex: "riskLevel", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          { title: "时间", dataIndex: "sentAt", render: formatTime },
        ]} />
        <SimpleTable title="P2 企微风险" data={snapshotValue.wecomRiskHits.slice(0, 12)} columns={[
          { title: "类型", dataIndex: "riskType" },
          { title: "关键词", dataIndex: "keyword" },
          { title: "级别", dataIndex: "riskLevel", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="P2 接入设备" data={snapshotValue.deviceIntegrationDevices.slice(0, 12)} columns={[
          { title: "编码", dataIndex: "deviceCode" },
          { title: "类型", dataIndex: "deviceType" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="P2 设备事件" data={snapshotValue.deviceIntegrationEvents.slice(0, 12)} columns={[
          { title: "设备", dataIndex: "deviceCode" },
          { title: "事件", dataIndex: "eventType" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          { title: "时间", dataIndex: "occurredAt", render: formatTime },
        ]} />
        <SimpleTable title="P2 离线同步批次" data={snapshotValue.deviceOfflineSyncBatches.slice(0, 12)} columns={[
          { title: "批次", dataIndex: "batchNo" },
          { title: "总数", dataIndex: "totalCount" },
          { title: "拒绝", dataIndex: "rejectedCount" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
      </div>
    </section>
  );
}
