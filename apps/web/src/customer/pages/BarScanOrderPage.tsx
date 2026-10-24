// 文件版本：V0.01
import { Alert, Button, Card, Empty, Input, InputNumber, List, Result, Space, Spin, Typography } from "antd";
import { useState } from "react";

import type { CustomerService } from "../client/customerService";
import type { BarScanMenuItem, BarScanOrder } from "../types";

const { Paragraph, Text } = Typography;

// ⑧ 扫码点单（ROOT-03-R7）：扫散台固定桌台码得 tableId -> 看菜单 -> 选数量 -> 下单即付款（付款成功才成单）。
// 年龄已在场馆入场闸核验，点单不重复核龄；当前开台会话由后端按桌台定位，客人端只提交所点商品。
export function BarScanOrderPage({ service }: { service: CustomerService }) {
  const [tableId, setTableId] = useState("");
  const [menu, setMenu] = useState<BarScanMenuItem[] | null>(null);
  const [qty, setQty] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [order, setOrder] = useState<BarScanOrder | null>(null);

  const loadMenu = async () => {
    const id = tableId.trim();
    if (!id) {
      setError("请扫描桌台二维码");
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
      const created = await service.placeBarScanOrder(tableId.trim(), items);
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
          扫描散台桌台二维码后在此点单。下单即付款，付款成功后计入本桌当前账单；年龄已在入场时核验，点单无需重复核验。
        </Paragraph>
        <Space.Compact style={{ width: "100%" }}>
          <Input
            placeholder="桌台编号（扫码自动带入）"
            value={tableId}
            onChange={(event) => setTableId(event.target.value)}
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
            title="待付款"
            subTitle={`点单 ${order.scanOrderId}，合计 ¥${(order.amountCent / 100).toFixed(2)}，付款成功后订单才成立`}
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
