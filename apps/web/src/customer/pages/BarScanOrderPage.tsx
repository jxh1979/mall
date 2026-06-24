import { Alert, Button, Card, Empty, Input, InputNumber, List, Result, Space, Spin, Typography } from "antd";
import { useState } from "react";

import type { CustomerService } from "../client/customerService";
import type { BarScanMenuItem, BarScanOrder } from "../types";

const { Paragraph, Text } = Typography;

// ⑧ 扫码点单（绑桌台会话 + 继承入场闸）：扫桌台二维码得 sessionId -> 看菜单 -> 选数量 -> 自助下单。
// 年龄已在场馆入场闸核验，点单不重复核龄；bar/桌台由会话定位，客人端只提交所点商品。
export function BarScanOrderPage({ service }: { service: CustomerService }) {
  const [sessionId, setSessionId] = useState("");
  const [menu, setMenu] = useState<BarScanMenuItem[] | null>(null);
  const [qty, setQty] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [order, setOrder] = useState<BarScanOrder | null>(null);

  const loadMenu = async () => {
    const id = sessionId.trim();
    if (!id) {
      setError("请输入或扫码桌台会话编号");
      return;
    }
    setLoading(true);
    setError(null);
    setOrder(null);
    setMenu(null);
    setQty({});
    try {
      const items = await service.listBarScanMenu(id);
      setMenu(items ?? []);
    } catch (err) {
      setError(err instanceof Error ? err.message : "菜单加载失败");
    } finally {
      setLoading(false);
    }
  };

  const submitOrder = async () => {
    const items = Object.entries(qty)
      .filter(([, q]) => q > 0)
      .map(([productId, quantity]) => ({ productId, quantity }));
    if (items.length === 0) {
      setError("请至少选择一项商品");
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const created = await service.placeBarScanOrder(sessionId.trim(), items);
      setOrder(created);
    } catch (err) {
      setError(err instanceof Error ? err.message : "下单失败");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Card title="扫码点单">
      <Space direction="vertical" style={{ width: "100%" }} size="middle">
        <Paragraph type="secondary">
          扫描桌台二维码后在此点单。下单记到当前桌台；年龄已在入场时核验，点单无需重复核验。
        </Paragraph>
        <Space.Compact style={{ width: "100%" }}>
          <Input
            placeholder="桌台会话编号（扫码自动带入）"
            value={sessionId}
            onChange={(event) => setSessionId(event.target.value)}
            data-testid="scan-session-input"
          />
          <Button type="primary" onClick={() => void loadMenu()} loading={loading} data-testid="scan-load-menu">
            加载菜单
          </Button>
        </Space.Compact>

        {error ? <Alert type="error" message={error} showIcon /> : null}
        {loading ? <Spin /> : null}

        {order ? (
          <Result
            status="success"
            title="点单成功"
            subTitle={`订单 ${order.id}，合计 ¥${(order.totalAmountCent / 100).toFixed(2)}`}
          />
        ) : null}

        {menu && !order ? (
          menu.length === 0 ? (
            <Empty description="该桌台暂无可点商品" />
          ) : (
            <>
              <List
                dataSource={menu}
                rowKey={(item) => item.productId}
                renderItem={(item) => (
                  <List.Item
                    actions={[
                      <InputNumber
                        key="qty"
                        min={0}
                        disabled={item.soldOut}
                        value={qty[item.productId] ?? 0}
                        onChange={(value) =>
                          setQty((prev) => ({ ...prev, [item.productId]: Number(value) || 0 }))
                        }
                        data-testid={`scan-qty-${item.productId}`}
                      />,
                    ]}
                  >
                    <List.Item.Meta
                      title={
                        <Text delete={item.soldOut}>
                          {item.productName}
                          {item.soldOut ? "（已沽清）" : ""}
                        </Text>
                      }
                      description={`${item.productType} · ¥${(item.priceCent / 100).toFixed(2)}`}
                    />
                  </List.Item>
                )}
              />
              <Button
                type="primary"
                block
                onClick={() => void submitOrder()}
                loading={submitting}
                data-testid="scan-submit-order"
              >
                提交点单
              </Button>
            </>
          )
        ) : null}
      </Space>
    </Card>
  );
}
