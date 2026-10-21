// 文件版本：V0.01
import { Alert, Button, Card, DatePicker, Form, Input, InputNumber, Result, Space, Select, Typography } from "antd";
import { useEffect, useMemo, useState } from "react";

import type { CustomerService } from "../client/customerService";
import type { PaymentIntent, TicketOrder, TicketProduct } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";

const { Paragraph, Text } = Typography;

// ② 官方购票下单：创建票务订单 -> 为订单金额发起聚合支付意图（支付通道真实渠道占位）
// AUD-076 单店票挂酒吧：公共入口=通票置顶+按酒吧分组；专属入口（?bar=<barId>）锁定并显示该酒吧。
// TK-06（BSM:843「不设需要人脸照的票种；购票和转赠均不要求录入人脸照」）：购票不拍人脸。
export function BuyTicketPage({ service }: { service: CustomerService }): React.ReactElement {
  // 专属入口：酒吧二维码/分享链接带 ?bar=<barId> 预锁定该酒吧（锁定也必须显示，不能看不见就买）。
  const lockedBarId = useMemo(() => new URLSearchParams(window.location.search).get("bar"), []);
  const [form] = Form.useForm();
  const [products, setProducts] = useState<TicketProduct[]>([]);
  const [order, setOrder] = useState<TicketOrder | null>(null);
  const [intent, setIntent] = useState<PaymentIntent | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    // 3.17：票种加载失败上抛到 error 态，不再静默吞成空下拉。锁定入口只拉该酒吧单店票。
    service.listTicketProducts(lockedBarId ?? undefined)
      .then((items) => setProducts(items ?? []))
      .catch((err) => setError(err instanceof Error ? err.message : "票种加载失败"));
  }, [service, lockedBarId]);

  const onSubmit = async (values: {
    ticketTypeId: string;
    quantity: number;
    visitDate?: { format: (f: string) => string };
    contactPhone?: string;
  }) => {
    const product = products.find((p) => p.ticketTypeId === values.ticketTypeId);
    setSubmitting(true);
    setError(null);
    setOrder(null);
    setIntent(null);
    try {
      // 单店票带其所属酒吧 bar_id（服务端据票种校验）；通票不带。
      const created = await service.createTicketOrder({
        ticketTypeId: values.ticketTypeId,
        quantity: values.quantity,
        visitDate: values.visitDate ? values.visitDate.format("YYYY-MM-DD") : undefined,
        contactPhone: values.contactPhone,
        barId: product?.barId ?? undefined,
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

  // 锁定酒吧名（从商品实时解析；商品未含则回退 id，仍须显示出来）。
  const lockedBarName = lockedBarId
    ? products.find((p) => p.barId === lockedBarId)?.barName ?? lockedBarId
    : null;

  // 分组下拉：通票置顶“综合体通票”，其余按酒吧分组；label 带酒吧名，同名不同店天然可分。
  const groupedOptions = useMemo(() => {
    const passItems = products.filter((p) => !p.barId);
    const byBar = new Map<string, { name: string; items: TicketProduct[] }>();
    for (const p of products) {
      if (!p.barId) continue;
      const group = byBar.get(p.barId) ?? { name: p.barName ?? p.barId, items: [] };
      group.items.push(p);
      byBar.set(p.barId, group);
    }
    const groups: { label: string; options: { value: string; label: string }[] }[] = [];
    if (passItems.length > 0) {
      groups.push({
        label: "综合体通票",
        options: passItems.map((p) => ({ value: p.ticketTypeId, label: p.ticketName })),
      });
    }
    for (const group of byBar.values()) {
      groups.push({
        label: group.name,
        options: group.items.map((p) => ({ value: p.ticketTypeId, label: `${p.ticketName} · ${group.name}` })),
      });
    }
    return groups;
  }, [products]);

  return (
    <div data-testid="buy-ticket-page">
      <Card title="官方购票">
        {lockedBarId ? (
          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 12 }}
            message={<span data-testid="buy-ticket-locked-bar">当前酒吧：{lockedBarName}</span>}
            description="您正通过该酒吧的专属入口购票，仅展示本酒吧单店票。"
          />
        ) : null}
        {error ? <Alert type="error" message="下单失败" description={error} style={{ marginBottom: 12 }} /> : null}
        <Form form={form} layout="vertical" onFinish={onSubmit} initialValues={{ quantity: 1 }}>
          <Form.Item name="ticketTypeId" label="票种" rules={[{ required: true, message: "请选择票种" }]}>
            <Select
              data-testid="buy-ticket-type"
              placeholder="请选择票种"
              options={groupedOptions}
            />
          </Form.Item>
          <Form.Item name="quantity" label="数量" rules={[{ required: true }]}>
            <InputNumber
              min={1}
              max={20}
              style={{ width: "100%" }}
              data-testid="buy-ticket-quantity"
            />
          </Form.Item>
          <Form.Item name="visitDate" label="游玩日期">
            <DatePicker style={{ width: "100%" }} />
          </Form.Item>
          <Form.Item name="contactPhone" label="联系手机号">
            <Input placeholder="可选" />
          </Form.Item>

          <Button
            type="primary"
            htmlType="submit"
            loading={submitting}
            block
            data-testid="buy-ticket-submit"
          >
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
