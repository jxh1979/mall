// 文件版本：V0.03
import { Alert, Button, Card, Empty, Input, List, Modal, Select, Space, Spin, Tag, Typography, message } from "antd";
import { useEffect, useState } from "react";

import { TicketQrView } from "../components/TicketQrView";
import type { CustomerService } from "../client/customerService";
import type { CustomerTicket, StoredValueAccount, TicketOrder } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";

const { Text } = Typography;

function cardAppliesToOrder(card: StoredValueAccount, order: TicketOrder): boolean {
  return card.status === "active"
    && (card.projectId === null || card.projectId === order.projectId)
    && (card.merchantId === null || card.merchantId === order.barId);
}

// ④ 我的订单 / 我的票 / 我的权益 + ⑥ 验票二维码出示（修复 1.8）
// 订单直接展开票实例/权益，点票出码，无需手输票 ID（GET /customer/orders/{id}/tickets）。
export function MyOrdersPage({ service }: { service: CustomerService }): React.ReactElement {
  const [orders, setOrders] = useState<TicketOrder[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [expandedOrderId, setExpandedOrderId] = useState<string | null>(null);
  const [ticketsByOrder, setTicketsByOrder] = useState<Record<string, CustomerTicket[]>>({});
  const [ticketsLoading, setTicketsLoading] = useState(false);
  const [ticketsError, setTicketsError] = useState<string | null>(null);
  const [qrTicketId, setQrTicketId] = useState<string | null>(null);
  const [payOrder, setPayOrder] = useState<TicketOrder | null>(null);
  const [cards, setCards] = useState<StoredValueAccount[]>([]);
  const [cardId, setCardId] = useState<string>();
  const [phone, setPhone] = useState("");
  const [smsCode, setSmsCode] = useState("");
  const [paymentBusy, setPaymentBusy] = useState(false);
  const [paymentError, setPaymentError] = useState<string | null>(null);

  async function openStoredValuePayment(order: TicketOrder) {
    setPayOrder(order);
    setCards([]);
    setCardId(undefined);
    setPhone("");
    setSmsCode("");
    setPaymentError(null);
    setPaymentBusy(true);
    try {
      const ownCards = await service.getStoredValueAccounts();
      setCards(ownCards);
      setCardId(ownCards.find((card) => cardAppliesToOrder(card, order))?.id);
    } catch (err) {
      setPaymentError(err instanceof Error ? err.message : "储值卡加载失败");
    } finally {
      setPaymentBusy(false);
    }
  }

  async function submitStoredValuePayment() {
    if (!payOrder || !cardId || !phone.trim() || !smsCode.trim()) return;
    setPaymentBusy(true);
    setPaymentError(null);
    try {
      const paid = await service.payTicketOrderWithStoredValue(payOrder.id, {
        accountId: cardId, phone: phone.trim(), smsCode: smsCode.trim(),
      });
      setOrders((previous) => previous.map((order) => order.id === paid.id ? paid : order));
      setPayOrder(null);
      setSmsCode("");
    } catch (err) {
      setPaymentError(err instanceof Error ? err.message : "余额支付失败");
    } finally {
      setPaymentBusy(false);
    }
  }

  async function requestPaymentCode() {
    if (!phone.trim()) { setPaymentError("请填写本人注册手机号"); return; }
    setPaymentBusy(true);
    setPaymentError(null);
    try {
      await service.requestStoredValueCode(phone.trim());
      void message.success("验证码已发送");
    } catch (err) {
      setPaymentError(err instanceof Error ? err.message : "验证码发送失败");
    } finally {
      setPaymentBusy(false);
    }
  }

  useEffect(() => {
    let active = true;
    service
      .listOrders()
      .then((items) => active && setOrders(items ?? []))
      .catch((err) => active && setError(err instanceof Error ? err.message : "订单加载失败"))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [service]);

  async function toggleTickets(orderId: string) {
    if (expandedOrderId === orderId) {
      setExpandedOrderId(null);
      return;
    }
    setExpandedOrderId(orderId);
    setTicketsError(null);
    if (ticketsByOrder[orderId]) return;
    setTicketsLoading(true);
    try {
      const tickets = await service.listOrderTickets(orderId);
      setTicketsByOrder((prev) => ({ ...prev, [orderId]: tickets ?? [] }));
    } catch (err) {
      setTicketsError(err instanceof Error ? err.message : "票据加载失败");
    } finally {
      setTicketsLoading(false);
    }
  }

  function renderTickets(orderId: string) {
    if (ticketsLoading && !ticketsByOrder[orderId]) return <Spin size="small" />;
    if (ticketsError) return <Alert type="error" message="票据加载失败" description={ticketsError} />;
    const tickets = ticketsByOrder[orderId] ?? [];
    if (tickets.length === 0) return <Empty description="该订单暂无已出票据（未出票或已退）" image={Empty.PRESENTED_IMAGE_SIMPLE} />;
    // AUD-066 按父票聚合：同一父票的各人份额（slot）归为一张卡片；无父票（存量）按票实例各自成组。
    const groups = new Map<string, CustomerTicket[]>();
    for (const t of tickets) {
      const key = t.parentTicketId ?? t.id;
      const arr = groups.get(key) ?? [];
      arr.push(t);
      groups.set(key, arr);
    }
    return (
      <List
        size="small"
        dataSource={Array.from(groups.values())}
        renderItem={(all) => {
          // TK-13（ENUM L148「“我的票”按对应人份或次份显示补偿标注及其实际有效期」）：完整补发的补偿行按人份单列标注与补偿有效期，
          // 不并入原票「剩余 x/y 次」。
          const members = all.filter((m) => m.compensationKind !== "full_reissue");
          const compensations = new Map<string, CustomerTicket[]>();
          for (const c of all.filter((m) => m.compensationKind === "full_reissue")) {
            const key = `${c.slotIndex ?? 1}|${c.validTo ?? ""}`;
            compensations.set(key, [...(compensations.get(key) ?? []), c]);
          }
          const head = members[0] ?? all[0];
          if (!head) return null;
          const remaining = members.reduce((sum, m) => sum + m.remainingTimes, 0);
          const total = members.reduce((sum, m) => sum + m.totalTimes, 0);
          return (
            <List.Item data-testid="ticket-item" style={{ display: "block" }}>
              <List.Item.Meta
                title={
                  <Space wrap>
                    <span>{head.ticketName ?? "票"}</span>
                    <Tag color={head.status === "active" ? "green" : "default"}>{formatBusinessStatus(head.status)}</Tag>
                    {head.numPeople > 1 ? <Tag color="blue">{head.numPeople} 人票</Tag> : null}
                    {head.ticketNo ? <Text type="secondary">票号 {head.ticketNo}</Text> : null}
                  </Space>
                }
                description={
                  <Space direction="vertical" size={4} style={{ width: "100%" }}>
                    <Text type="secondary">
                      剩余 {remaining}/{total} 次
                      {head.validTo ? ` · 有效期至 ${new Date(head.validTo).toLocaleDateString()}` : ""}
                    </Text>
                    <Space wrap>
                      {members.map((m, idx) => (
                        <Button
                          key={m.id}
                          type="primary"
                          size="small"
                          disabled={m.status !== "active"}
                          onClick={() => setQrTicketId(m.id)}
                          data-testid="ticket-qr-open"
                        >
                          {members.length > 1 ? `第${m.slotIndex ?? idx + 1}人 出示二维码` : "出示二维码"}
                        </Button>
                      ))}
                    </Space>
                    {Array.from(compensations.entries()).map(([key, rows]) => (
                      <Space wrap key={key} data-testid="ticket-compensation">
                        <Tag color="orange" data-testid="ticket-compensation-tag">
                          补偿（第{rows[0]?.slotIndex ?? 1}人）{rows[0]?.validTo ? ` · 补偿有效期至 ${new Date(rows[0].validTo).toLocaleDateString()}` : ""}
                        </Tag>
                        {rows.map((c) => (
                          <Button key={c.id} size="small" disabled={c.status !== "active"} onClick={() => setQrTicketId(c.id)}
                            data-testid="ticket-qr-open">
                            出示二维码
                          </Button>
                        ))}
                      </Space>
                    ))}
                  </Space>
                }
              />
            </List.Item>
          );
        }}
      />
    );
  }

  return (
    <div data-testid="my-orders-page">
      <Modal title="订单储值支付" open={payOrder !== null} onCancel={() => setPayOrder(null)}
        closable={!paymentBusy} maskClosable={false} keyboard={!paymentBusy}
        okText="确认余额支付" cancelText="取消" confirmLoading={paymentBusy}
        okButtonProps={{ disabled: paymentBusy || !cardId || !phone.trim() || !smsCode.trim() }}
        cancelButtonProps={{ disabled: paymentBusy }} onOk={() => void submitStoredValuePayment()}>
        {paymentError ? <Alert type="error" message={paymentError} /> : null}
        <Space direction="vertical" style={{ width: "100%" }}>
          <Text>本订单应付 ¥{((payOrder?.amountCent ?? 0) / 100).toFixed(2)}</Text>
          <Select aria-label="付款储值卡" value={cardId} onChange={setCardId} style={{ width: "100%" }}
            options={cards.map((card) => ({ value: card.id,
              disabled: !payOrder || !cardAppliesToOrder(card, payOrder),
              label: `${card.merchantId ? "商户卡（" + card.merchantId + "）" : "综合体通用卡"} · 余额 ¥${(card.balanceCent / 100).toFixed(2)}${payOrder && !cardAppliesToOrder(card, payOrder) ? " · 不适用于该订单" : ""}` }))} />
          <label htmlFor="order-payment-phone">付款手机号</label>
          <Input id="order-payment-phone" type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} />
          <Button disabled={paymentBusy} onClick={() => void requestPaymentCode()}>发送扣款验证码</Button>
          <label htmlFor="order-payment-code">短信验证码</label>
          <Input id="order-payment-code" autoComplete="one-time-code" inputMode="numeric" value={smsCode} onChange={(e) => setSmsCode(e.target.value)} />
        </Space>
      </Modal>
      <Card title="我的订单 / 我的票 / 我的权益">
        {error ? <Alert type="error" message="订单加载失败" description={error} style={{ marginBottom: 12 }} /> : null}
        {loading ? (
          <Spin />
        ) : orders.length === 0 ? (
          <Empty description="暂无订单" />
        ) : (
          <List
            dataSource={orders}
            renderItem={(order) => (
              <List.Item data-testid="order-item" style={{ display: "block" }}>
                <List.Item.Meta
                  title={
                    <Space>
                      <span>订单 {order.orderNo}</span>
                      <Tag color="blue">{formatBusinessStatus(order.salesChannel)}</Tag>
                      <Tag color={order.payStatus === "paid" ? "green" : "orange"}>{formatBusinessStatus(order.payStatus)}</Tag>
                      <Tag color={order.issueStatus === "issued" ? "green" : "default"}>{formatBusinessStatus(order.issueStatus)}</Tag>
                    </Space>
                  }
                  description={
                    <Space>
                      <Text type="secondary">
                        {order.quantity} 张 · ¥{(order.amountCent / 100).toFixed(2)} · 游玩日 {order.visitDate ?? "—"}
                      </Text>
                      {order.payStatus === "unpaid" && order.status === "created" ? (
                        <Button onClick={() => void openStoredValuePayment(order)}>储值余额支付</Button>
                      ) : null}
                      <Button size="small" onClick={() => void toggleTickets(order.id)} data-testid="order-tickets-toggle">
                        {expandedOrderId === order.id ? "收起票据" : "查看票 / 出码"}
                      </Button>
                    </Space>
                  }
                />
                {expandedOrderId === order.id ? (
                  <div style={{ marginTop: 8 }} data-testid="order-tickets">
                    {renderTickets(order.id)}
                  </div>
                ) : null}
              </List.Item>
            )}
          />
        )}
      </Card>

      <Modal
        open={Boolean(qrTicketId)}
        onCancel={() => setQrTicketId(null)}
        footer={null}
        destroyOnClose
        title="出示验票二维码"
      >
        {qrTicketId ? <TicketQrView service={service} ticketId={qrTicketId} /> : null}
      </Modal>
    </div>
  );
}
