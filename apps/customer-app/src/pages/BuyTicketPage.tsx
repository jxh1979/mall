// 文件版本：V0.02
import { Alert, Button, Card, DatePicker, Descriptions, Form, Input, InputNumber, Result, Select, Typography } from "antd";
import { useEffect, useMemo, useState } from "react";

import type { CustomerService } from "@complex/customer-core";
import type { TicketOrder, TicketProduct } from "@complex/customer-core";

import { OrderPayment } from "../components/OrderPayment";

const { Paragraph } = Typography;

const yuan = (cent: number) => `¥${(cent / 100).toFixed(2)}`;

// ② 官方购票：选票种 → 看我的价格与门票守则 → 下单 → 组合支付（储值＋在线补足）→ 付清才显示支付成功。
// ROOT-03-R27（BSM「多种资格价不叠加，选择最终价格最低的一项」）：价格由系统按本人资格自动取最低，客人不选资格、不填金额。
// clients-R9（BSM「购票前须以门票守则的形式向客人告知该票的规则，包括能否退款、有效期、年龄限制与可使用范围」）。
// BSM「客人付款成功后必须出票」：付清即服务端同一事务出票，本页按服务端订单视图显示结果。
// AUD-076 单店票挂酒吧：公共入口=通票置顶+按酒吧分组；专属入口（深链 /c/bar?bar_id=<barId>）锁定并显示该酒吧。
// TK-06（BSM:843「不设需要人脸照的票种；购票和转赠均不要求录入人脸照」）：购票不拍人脸。
// PGM「可以按全局、综合体……开关」：所选票种所属的综合体报给外壳（onProject），购票请求带 X-Project-Id，后端按综合体的终端开关拒绝。
export function BuyTicketPage({ service, lockedBarId = null, onProject }: { service: CustomerService; lockedBarId?: string | null;
  onProject?: (projectId: string) => void }): React.ReactElement {
  const [form] = Form.useForm();
  const [products, setProducts] = useState<TicketProduct[]>([]);
  const [order, setOrder] = useState<TicketOrder | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const selectedId = Form.useWatch("ticketTypeId", form) as string | undefined;
  const selected = products.find((p) => p.ticketTypeId === selectedId);

  useEffect(() => {
    // 3.17：票种加载失败上抛到 error 态，不再静默吞成空下拉。锁定入口只拉该酒吧单店票。
    service.listTicketProducts(lockedBarId ?? undefined)
      .then((items) => setProducts(items ?? []))
      .catch((err) => setError(err instanceof Error ? err.message : "票种加载失败"));
  }, [service, lockedBarId]);

  useEffect(() => {
    if (selected) onProject?.(selected.projectId ?? "");
  }, [selected, onProject]);

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
    try {
      // 单店票带其所属酒吧 bar_id（服务端据票种校验）；通票不带。
      setOrder(await service.createTicketOrder({
        ticketTypeId: values.ticketTypeId,
        quantity: values.quantity,
        visitDate: values.visitDate ? values.visitDate.format("YYYY-MM-DD") : undefined,
        contactPhone: values.contactPhone,
        barId: product?.barId ?? undefined,
      }));
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
          {selected ? (
            <>
              {selected.myPriceCent != null ? (
                <Paragraph data-testid="buy-ticket-my-price">
                  我的价格 {yuan(selected.myPriceCent)}{selected.myPriceBasis ? `（${selected.myPriceBasis}）` : ""}，系统已按您的资格自动取最低价
                </Paragraph>
              ) : null}
              <Descriptions title="门票守则" size="small" column={1} bordered data-testid="ticket-rules" style={{ marginBottom: 16 }}>
                <Descriptions.Item label="能否退款">{selected.refundable ? "按票种规则可申请退款，开始核销后不可退" : "售出后不退款"}</Descriptions.Item>
                <Descriptions.Item label="有效期">{selected.validityDays != null ? `${selected.validityDays} 天` : "以出票后票面为准"}</Descriptions.Item>
                <Descriptions.Item label="年龄限制">{selected.ageLimitText ?? "—"}</Descriptions.Item>
                <Descriptions.Item label="可使用范围">{selected.usageScope ?? "—"}</Descriptions.Item>
                {selected.entitlements && selected.entitlements.length > 0 ? (
                  <Descriptions.Item label="所含权益">{selected.entitlements.join("、")}</Descriptions.Item>
                ) : null}
              </Descriptions>
            </>
          ) : null}
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
            已阅读门票守则，提交订单
          </Button>
        </Form>
      </Card>
      {order ? (
        <Card style={{ marginTop: 16 }} data-testid="buy-ticket-result" title={`订单 ${order.orderNo ?? ""}`}>
          {order.payStatus === "paid" ? (
            <div data-testid="buy-ticket-paid">
              <Result status="success" title="支付成功"
                subTitle={order.issueStatus === "issued" ? "已出票，可在「我的订单 / 票 / 二维码」出示验票码" : "正在出票，请稍后在「我的订单 / 票 / 二维码」查看"} />
            </div>
          ) : order.status === "created" ? (
            <OrderPayment service={service} order={order} onChange={setOrder} />
          ) : (
            <Alert type="warning" showIcon message="订单已不可支付，请到「我的订单」查看状态" />
          )}
        </Card>
      ) : null}
    </div>
  );
}
