import { QrcodeOutlined } from "@ant-design/icons";
import { Alert, Button, Form, Input, InputNumber, Select, Tag } from "antd";
import type { FormInstance } from "antd/es/form";
import { formatMoney, formatPercentPoint, formatTime, statusColor } from "../formatters";
import type { Snapshot } from "../types";
import { SectionHeading, SimpleTable } from "../ui";

type TicketingFormValues = Record<string, string | number>;
type TicketingStringValues = Record<string, string>;

export type TicketingPageProps = {
  snapshotValue: Snapshot;
  ticketFlowForm: FormInstance;
  verifyForm: FormInstance;
  settlementForm: FormInstance;
  dynamicPricingForm: FormInstance;
  dynamicPricingSimulationForm: FormInstance;
  dynamicPricingOrderForm: FormInstance;
  handleTicketFlow: (values: TicketingFormValues) => void | Promise<void>;
  handlePreview: () => void | Promise<void>;
  handleVerify: () => void | Promise<void>;
  handleSettlementGenerate: (values: TicketingStringValues) => void | Promise<void>;
  handleDynamicPricingRule: (values: TicketingFormValues) => void | Promise<void>;
  handlePublishDynamicPricingRule: (ruleId: string) => Promise<void>;
  handleDynamicPricingSimulation: (values: TicketingFormValues) => void | Promise<void>;
  handleDynamicPricingOrder: (values: TicketingFormValues) => void | Promise<void>;
  confirmHighImpactAction: (title: string, content: string, action: () => Promise<void>) => void;
};

export function TicketingPage({
  snapshotValue,
  ticketFlowForm,
  verifyForm,
  settlementForm,
  dynamicPricingForm,
  dynamicPricingSimulationForm,
  dynamicPricingOrderForm,
  handleTicketFlow,
  handlePreview,
  handleVerify,
  handleSettlementGenerate,
  handleDynamicPricingRule,
  handlePublishDynamicPricingRule,
  handleDynamicPricingSimulation,
  handleDynamicPricingOrder,
  confirmHighImpactAction,
}: TicketingPageProps) {
  return (
    <section className="section-block">
      <SectionHeading icon={<QrcodeOutlined />} title="票务、权益、出票、核销" />
      <div className="action-grid two">
        <div className="action-panel">
          <h2>创建票种并出票</h2>
          <Form form={ticketFlowForm} data-testid="ticket-flow-form" layout="vertical" onFinish={handleTicketFlow} initialValues={{
            productName: `P0 Web Pass ${Date.now()}`,
            entitlementName: "P0 Web Entry Entitlement",
            entitlementType: "entry",
            priceYuan: 99,
            totalTimes: 1,
            purchaserName: "P0 Visitor",
          }}>
            <div className="form-grid two">
              <Form.Item name="productName" label="票种名称" rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="priceYuan" label="售价（元）" rules={[{ required: true }]}><InputNumber min={0} className="full" /></Form.Item>
              <Form.Item name="entitlementName" label="权益名称" rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="entitlementType" label="权益类型"><Select options={["entry", "drink", "amusement", "coupon"].map((value) => ({ value, label: value }))} /></Form.Item>
              <Form.Item name="totalTimes" label="可用次数"><InputNumber min={1} className="full" /></Form.Item>
              <Form.Item name="purchaserName" label="购买人"><Input /></Form.Item>
              <Form.Item name="purchaserPhone" label="手机号"><Input /></Form.Item>
            </div>
            <Button type="primary" htmlType="submit">创建并出票</Button>
          </Form>
        </div>
        <div className="action-panel">
          <h2>核销预检与执行</h2>
          <Alert className="status-alert" type="warning" showIcon message="核销会改变核销结果，须在管理后台/现场端执行。此处仅提供只读预检，确认核销将跳转管理后台。" />
          <Form form={verifyForm} layout="vertical">
            <Form.Item name="entitlementCode" label="权益码" rules={[{ required: true }]}>
              <Select
                showSearch
                optionFilterProp="label"
                options={snapshotValue.issuedEntitlements.slice(0, 80).map((item) => ({
                  value: item.entitlementCode,
                  label: `${item.entitlementCode} · ${item.status} · ${item.usedTimes}/${item.totalTimes}`,
                }))}
              />
            </Form.Item>
            <div className="button-row">
              <Button onClick={() => void handlePreview()}>核销预检</Button>
              <Button type="primary" onClick={() => void handleVerify()}>前往管理后台核销</Button>
            </div>
          </Form>
        </div>
        <div className="action-panel">
          <h2>生成结算单</h2>
          <Form form={settlementForm} layout="vertical" onFinish={handleSettlementGenerate}>
            <Form.Item name="merchantId" label="商户">
              <Select allowClear options={snapshotValue.merchantsRaw.map((item) => ({ value: item.id, label: item.displayName }))} />
            </Form.Item>
            <Button type="primary" htmlType="submit">生成待结算账单</Button>
          </Form>
        </div>
      </div>
      <div className="action-grid two">
        <div className="action-panel">
          <h2>P2 Dynamic Pricing Rules</h2>
          <Alert className="status-alert" type="info" showIcon message="Draft rules are for simulation. Published matching rules affect formal ticket order amount and keep price trace fields." />
          <Form form={dynamicPricingForm} data-testid="dynamic-pricing-form" layout="vertical" onFinish={handleDynamicPricingRule} initialValues={{ ticketTypeId: snapshotValue.tickets[0]?.id, ruleName: "Weekend Boost", effectiveFrom: "2026-06-01", effectiveTo: "2026-06-30", priority: 10, salesChannel: "web", adjustmentType: "percent_delta", adjustmentRate: 10, minPriceYuan: 0 }}>
            <div className="form-grid two">
              <Form.Item name="ticketTypeId" label="Ticket type" rules={[{ required: true }]}><Select options={snapshotValue.tickets.map((item) => ({ value: item.id, label: `${item.productName} - ${formatMoney(item.priceAmount)}` }))} /></Form.Item>
              <Form.Item name="ruleName" label="Rule name" rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="effectiveFrom" label="Effective from" rules={[{ required: true }]}><Input placeholder="2026-06-01" /></Form.Item>
              <Form.Item name="effectiveTo" label="Effective to" rules={[{ required: true }]}><Input placeholder="2026-06-30" /></Form.Item>
              <Form.Item name="salesChannel" label="Channel"><Select options={["admin", "kiosk", "miniapp", "app", "web", "channel"].map((value) => ({ value, label: value }))} /></Form.Item>
              <Form.Item name="priority" label="Priority"><InputNumber className="full" /></Form.Item>
              <Form.Item name="adjustmentType" label="Adjustment"><Select options={[{ value: "percent_delta", label: "Percent" }, { value: "fixed_delta", label: "Fixed amount" }]} /></Form.Item>
              <Form.Item name="adjustmentRate" label="Percent"><InputNumber className="full" /></Form.Item>
              <Form.Item name="adjustmentAmountYuan" label="Fixed amount"><InputNumber className="full" /></Form.Item>
              <Form.Item name="minPriceYuan" label="Min price"><InputNumber min={0} className="full" /></Form.Item>
              <Form.Item name="maxPriceYuan" label="Max price"><InputNumber min={0} className="full" /></Form.Item>
              <Form.Item name="conditionKey" label="Condition key"><Input placeholder="inventory_level" /></Form.Item>
              <Form.Item name="conditionValue" label="Condition value"><Input placeholder="low" /></Form.Item>
              <Form.Item name="operatorNote" label="Note"><Input /></Form.Item>
            </div>
            <Button type="primary" htmlType="submit">Create draft</Button>
          </Form>
        </div>
        <div className="action-panel">
          <h2>P2 Dynamic Pricing Simulation</h2>
          <Form form={dynamicPricingSimulationForm} data-testid="dynamic-pricing-simulation-form" layout="vertical" onFinish={handleDynamicPricingSimulation} initialValues={{ ticketTypeId: snapshotValue.tickets[0]?.id, visitDate: "2026-06-20", salesChannel: "web" }}>
            <div className="form-grid two">
              <Form.Item name="ticketTypeId" label="Ticket type" rules={[{ required: true }]}><Select options={snapshotValue.tickets.map((item) => ({ value: item.id, label: item.productName }))} /></Form.Item>
              <Form.Item name="visitDate" label="Visit date" rules={[{ required: true }]}><Input placeholder="2026-06-20" /></Form.Item>
              <Form.Item name="salesChannel" label="Channel"><Select options={["admin", "kiosk", "miniapp", "app", "web", "channel"].map((value) => ({ value, label: value }))} /></Form.Item>
              <Form.Item name="conditionKey" label="Condition key"><Input placeholder="inventory_level" /></Form.Item>
              <Form.Item name="conditionValue" label="Condition value"><Input placeholder="low" /></Form.Item>
            </div>
            <Button htmlType="submit">Simulate price</Button>
          </Form>
        </div>
        <div className="action-panel">
          <h2>Formal Order Pricing Check</h2>
          <Form form={dynamicPricingOrderForm} data-testid="dynamic-pricing-order-form" layout="vertical" onFinish={handleDynamicPricingOrder} initialValues={{ ticketTypeId: snapshotValue.tickets[0]?.id, visitDate: "2026-06-20", salesChannel: "web", quantity: 1 }}>
            <div className="form-grid two">
              <Form.Item name="ticketTypeId" label="Ticket type" rules={[{ required: true }]}><Select options={snapshotValue.tickets.map((item) => ({ value: item.id, label: item.productName }))} /></Form.Item>
              <Form.Item name="quantity" label="Quantity" rules={[{ required: true }]}><InputNumber min={1} className="full" /></Form.Item>
              <Form.Item name="visitDate" label="Visit date" rules={[{ required: true }]}><Input placeholder="2026-06-20" /></Form.Item>
              <Form.Item name="salesChannel" label="Channel"><Select options={["admin", "kiosk", "miniapp", "app", "web", "channel"].map((value) => ({ value, label: value }))} /></Form.Item>
              <Form.Item name="buyerPhone" label="Phone"><Input placeholder="13800138000" /></Form.Item>
              <Form.Item name="customerId" label="Customer ID"><Input placeholder="customer_1" /></Form.Item>
              <Form.Item name="conditionKey" label="Condition key"><Input placeholder="inventory_level" /></Form.Item>
              <Form.Item name="conditionValue" label="Condition value"><Input placeholder="low" /></Form.Item>
            </div>
            <Button type="primary" htmlType="submit">Create order</Button>
          </Form>
        </div>
      </div>
      <div className="table-grid two">
        <SimpleTable title="P2 动态定价规则" data={snapshotValue.ticketDynamicPricingRules.slice(0, 12)} columns={[
          { title: "规则", dataIndex: "ruleName" },
          { title: "渠道", dataIndex: "salesChannel", render: (value) => value || "全部" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          { title: "有效期", key: "period", render: (_, row) => `${row.effectiveFrom} 至 ${row.effectiveTo}` },
          { title: "调整", key: "adjustment", render: (_, row) => row.adjustmentType === "percent_delta" ? formatPercentPoint(row.adjustmentRate) : formatMoney(row.adjustmentAmountCent) },
          { title: "操作", key: "actions", render: (_, row) => row.status === "draft" ? <Button size="small" data-testid={`publish-dynamic-pricing-${row.id}`} onClick={() => confirmHighImpactAction("Publish dynamic pricing rule", "Published matching rules affect formal ticket order amount and keep price trace fields.", () => handlePublishDynamicPricingRule(row.id))}>发布</Button> : <Tag>{row.status}</Tag> },
        ]} />
        <SimpleTable title="票种" data={snapshotValue.tickets.slice(0, 12)} columns={[
          { title: "名称", dataIndex: "productName" },
          { title: "价格", dataIndex: "priceAmount", render: formatMoney },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="核销记录" data={snapshotValue.verifications.slice(0, 12)} columns={[
          { title: "核销号", dataIndex: "verificationNo" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          { title: "时间", dataIndex: "createdAt", render: formatTime },
        ]} />
        <SimpleTable title="结算单" data={snapshotValue.settlementBills.slice(0, 12)} columns={[
          { title: "单号", dataIndex: "billNo" },
          { title: "金额", dataIndex: "totalAmount", render: formatMoney },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
      </div>
    </section>
  );
}
