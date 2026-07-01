import { Alert, Button, Card, Empty, Input, List, Modal, Space, Spin, Tag, Typography } from "antd";
import { useEffect, useState } from "react";

import { TicketQrView } from "../components/TicketQrView";
import type { CustomerService } from "../client/customerService";
import type { TicketOrder } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";

const { Text } = Typography;

// ④ 我的订单 / 我的票 / 我的权益 + ⑥ 验票二维码出示入口
// 说明：P0 OpenAPI 未提供单票列表端点，"我的票"以订单视图承载；
// 二维码按票 ID 展示，票 ID 可从订单出票后获取（此处提供输入入口对接 GET /tickets/{id}/qr）。
export function MyOrdersPage({ service }: { service: CustomerService }): React.ReactElement {
  const [orders, setOrders] = useState<TicketOrder[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [qrTicketId, setQrTicketId] = useState<string | null>(null);
  const [ticketIdInput, setTicketIdInput] = useState("");

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
              <List.Item data-testid="order-item">
                <List.Item.Meta
                  title={
                    <Space>
                      <span>订单 {order.orderNo}</span>
                      <Tag color="blue">{order.salesChannel}</Tag>
                      <Tag color={order.payStatus === "paid" ? "green" : "orange"}>{formatBusinessStatus(order.payStatus)}</Tag>
                      <Tag color={order.issueStatus === "issued" ? "green" : "default"}>{formatBusinessStatus(order.issueStatus)}</Tag>
                    </Space>
                  }
                  description={
                    <Text type="secondary">
                      {order.quantity} 张 · ¥{(order.amountCent / 100).toFixed(2)} · 游玩日 {order.visitDate ?? "—"}
                    </Text>
                  }
                />
              </List.Item>
            )}
          />
        )}
      </Card>

      <Card title="验票二维码出示" style={{ marginTop: 16 }} data-testid="qr-launcher">
        <Space.Compact style={{ width: "100%" }}>
          <Input
            placeholder="输入票 ID 展示验票二维码"
            value={ticketIdInput}
            onChange={(e) => setTicketIdInput(e.target.value)}
            data-testid="qr-ticket-input"
          />
          <Button
            type="primary"
            disabled={!ticketIdInput.trim()}
            onClick={() => setQrTicketId(ticketIdInput.trim())}
            data-testid="qr-open"
          >
            出示二维码
          </Button>
        </Space.Compact>
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
