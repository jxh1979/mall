import { Alert, Button, Card, DatePicker, Form, Input, InputNumber, Result, Select, Space, Typography } from "antd";
import { useEffect, useState } from "react";

import type { CustomerService } from "../client/customerService";
import type { PaymentIntent, TicketOrder, TicketProduct } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";

const { Paragraph, Text } = Typography;

// ② 官方购票下单：创建票务订单 -> 为订单金额发起聚合支付意图（支付通道真实渠道占位）
export function BuyTicketPage({ service }: { service: CustomerService }): React.ReactElement {
  const [form] = Form.useForm();
  const [products, setProducts] = useState<TicketProduct[]>([]);
  const [order, setOrder] = useState<TicketOrder | null>(null);
  const [intent, setIntent] = useState<PaymentIntent | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    // 3.17：票种加载失败上抛到 error 态，不再静默吞成空下拉。
    service.listTicketProducts()
      .then((items) => setProducts(items ?? []))
      .catch((err) => setError(err instanceof Error ? err.message : "票种加载失败"));
  }, [service]);

  const onSubmit = async (values: { ticketTypeId: string; quantity: number; visitDate?: { format: (f: string) => string }; contactPhone?: string }) => {
    setSubmitting(true);
    setError(null);
    setOrder(null);
    setIntent(null);
    try {
      const created = await service.createTicketOrder({
        ticketTypeId: values.ticketTypeId,
        quantity: values.quantity,
        visitDate: values.visitDate ? values.visitDate.format("YYYY-MM-DD") : undefined,
        contactPhone: values.contactPhone,
      });
      setOrder(created);
      const payment = await service.createPaymentIntent({
        businessOrderType: "ticket_order",
        businessOrderId: created.id,
        amountCent: created.amountCent,
        currency: created.currency || "CNY",
      });
      setIntent(payment);
    } catch (err) {
      setError(err instanceof Error ? err.message : "下单失败");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div data-testid="buy-ticket-page">
      <Card title="官方购票">
        {error ? <Alert type="error" message="下单失败" description={error} style={{ marginBottom: 12 }} /> : null}
        <Form form={form} layout="vertical" onFinish={onSubmit} initialValues={{ quantity: 1 }}>
          <Form.Item name="ticketTypeId" label="票种" rules={[{ required: true, message: "请选择票种" }]}>
            <Select
              data-testid="buy-ticket-type"
              placeholder="请选择票种"
              options={products.map((p) => ({ value: p.ticketTypeId, label: p.ticketName }))}
            />
          </Form.Item>
          <Form.Item name="quantity" label="数量" rules={[{ required: true }]}>
            <InputNumber min={1} max={20} style={{ width: "100%" }} data-testid="buy-ticket-quantity" />
          </Form.Item>
          <Form.Item name="visitDate" label="游玩日期">
            <DatePicker style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="contactPhone" label="联系手机号">
            <Input placeholder="可选" />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={submitting} block data-testid="buy-ticket-submit">
            提交订单并支付
          </Button>
        </Form>
      </Card>
      {order ? (
        <Card style={{ marginTop: 16 }} data-testid="buy-ticket-result">
          <Result
            status="success"
            title="订单已创建"
            subTitle={`订单号 ${order.orderNo} · 金额 ¥${(order.amountCent / 100).toFixed(2)}`}
          >
            <Space direction="vertical">
              <Text>支付状态：{formatBusinessStatus(order.payStatus)}</Text>
              {intent ? (
                <Paragraph type="secondary">
                  已发起聚合支付，当前支付状态：{formatBusinessStatus(String(intent.status ?? "pending"))}。
                  真实支付渠道跳转为占位，客人端仅展示支付结果。
                </Paragraph>
              ) : null}
            </Space>
          </Result>
        </Card>
      ) : null}
    </div>
  );
}
