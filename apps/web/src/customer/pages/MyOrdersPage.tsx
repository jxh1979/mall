import { Alert, Button, Card, Empty, List, Modal, Space, Spin, Tag, Typography } from "antd";
import { useEffect, useState } from "react";

import { TicketQrView } from "../components/TicketQrView";
import type { CustomerService } from "../client/customerService";
import type { CustomerTicket, TicketOrder } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";

const { Text } = Typography;

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
    return (
      <List
        size="small"
        dataSource={tickets}
        renderItem={(ticket) => (
          <List.Item
            data-testid="ticket-item"
            actions={[
              <Button
                key="qr"
                type="primary"
                size="small"
                disabled={ticket.status !== "active"}
                onClick={() => setQrTicketId(ticket.id)}
                data-testid="ticket-qr-open"
              >
                出示二维码
              </Button>,
            ]}
          >
            <List.Item.Meta
              title={
                <Space>
                  <span>{ticket.ticketName ?? "票"}</span>
                  <Tag color={ticket.status === "active" ? "green" : "default"}>{formatBusinessStatus(ticket.status)}</Tag>
                  {ticket.numPeople > 1 ? <Tag color="blue">{ticket.numPeople} 人票</Tag> : null}
                </Space>
              }
              description={
                <Text type="secondary">
                  剩余 {ticket.remainingTimes}/{ticket.totalTimes} 次
                  {ticket.validTo ? ` · 有效期至 ${new Date(ticket.validTo).toLocaleDateString()}` : ""}
                </Text>
              }
            />
          </List.Item>
        )}
      />
    );
  }

  return (
    <div data-testid="my-orders-page">
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
