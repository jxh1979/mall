// 文件版本：V0.02
import { Alert, Button, Card, Empty, List, Result, Space, Spin, Tag, Typography } from "antd";
import { useCallback, useEffect, useRef, useState } from "react";

import { parseCustomerLink } from "@complex/customer-core";
import type { BarScanMenuItem, BarTableBillItem, CustomerService } from "@complex/customer-core";

import { CUSTOMER_LINK_HOST } from "../api";
import { OnlinePayment } from "../components/OnlinePayment";
import { platform } from "../platform";

const { Paragraph, Text } = Typography;

const yuan = (cent: number) => `¥${(cent / 100).toFixed(2)}`;

// ⑧ 扫码点单（BSM「扫码点单只适用于散台……开台后客人扫描桌台二维码即可自助点单，下单时即时付款、付款成功订单才成立，不挂账；酒水必须先完成付款才能
// 出品上台。付款成功后所点酒水计入该桌位当前账单，账单上逐项列出物品名称与价格，并分别标注已上菜与未上菜」；ENUM「沽清商品在客人扫码点单菜单上置灰
// 展示为已售完，不隐藏」；ENUM「非会员可以浏览、点单和付款」）：扫散台固定桌台码（或桌台码深链）定位该桌，当前开台会话由服务端按桌台找；
// 不手填桌台或会话编号。下单即在线付款，渠道确认收款才成单；本桌当前账单只列该桌本次开台已成单的酒水。
export function BarScanOrderPage({ service, initialTableId }: { service: CustomerService; initialTableId?: string }): React.ReactElement {
  const [tableId, setTableId] = useState<string | null>(initialTableId ?? null);
  const [menu, setMenu] = useState<BarScanMenuItem[] | null>(null);
  const [bill, setBill] = useState<BarTableBillItem[]>([]);
  const [qty, setQty] = useState<Record<string, number>>({});
  const [error, setError] = useState<string | null>(null);
  const [paid, setPaid] = useState(false);
  const scanOrderRef = useRef<string | null>(null);

  const loadBill = useCallback((id: string) => {
    service.getBarTableCurrentBill(id).then((items) => setBill(items ?? [])).catch(() => setBill([]));
  }, [service]);

  useEffect(() => {
    if (!tableId) return;
    let active = true;
    setMenu(null);
    setError(null);
    setQty({});
    service.listBarScanMenu(tableId)
      .then((items) => active && setMenu(items ?? []))
      .catch((err) => active && setError(err instanceof Error ? err.message : "菜单加载失败"));
    loadBill(tableId);
    return () => {
      active = false;
    };
  }, [service, tableId, loadBill]);

  const scan = async () => {
    setError(null);
    const raw = await platform.scanCode();
    if (raw === null) {
      setError("本机暂不能扫码，请用手机相机扫桌台二维码");
      return;
    }
    const link = parseCustomerLink(raw, CUSTOMER_LINK_HOST);
    if (link?.route !== "table" || !link.params.table_id) {
      setError("无法识别的二维码");
      return;
    }
    setPaid(false);
    setTableId(link.params.table_id);
  };

  const add = (productId: string, delta: number) =>
    setQty((prev) => ({ ...prev, [productId]: Math.max(0, (prev[productId] ?? 0) + delta) }));

  const lines = (menu ?? []).filter((item) => (qty[item.productId] ?? 0) > 0);
  const totalCent = lines.reduce((sum, item) => sum + item.priceCent * (qty[item.productId] ?? 0), 0);

  return (
    <Space direction="vertical" style={{ width: "100%" }} size="middle">
      <Paragraph type="secondary" style={{ marginBottom: 0 }}>
        扫散台桌台二维码点单（卡座不开放扫码点单，须服务员先开台）。下单即付款，付款成功后计入本桌当前账单。
      </Paragraph>
      <Button block onClick={() => void scan()} data-testid="scan-start">{tableId ? "重新扫桌台码" : "扫桌台二维码"}</Button>
      {error ? <Alert type="error" showIcon message={error} data-testid="scan-error" /> : null}
      {tableId && !menu && !error ? <Spin /> : null}

      {paid ? <div data-testid="scan-paid"><Result status="success" title="付款成功，已计入本桌账单" /></div> : null}

      {menu ? (
        menu.length === 0 ? (
          <Empty description="该桌台暂无可点商品" />
        ) : (
          <Card title="菜单" size="small">
            <List
              dataSource={menu}
              rowKey={(item) => item.productId}
              renderItem={(item) => (
                <List.Item
                  data-testid={`scan-item-${item.productId}`}
                  actions={[
                    <Button key="minus" size="small" disabled={!qty[item.productId]} onClick={() => add(item.productId, -1)}>－</Button>,
                    <Text key="count">{qty[item.productId] ?? 0}</Text>,
                    <Button key="plus" size="small" disabled={item.soldOut} onClick={() => add(item.productId, 1)} data-testid={`scan-add-${item.productId}`}>＋</Button>,
                  ]}
                >
                  <List.Item.Meta
                    title={<Text type={item.soldOut ? "secondary" : undefined}>{item.productName}{item.soldOut ? <Tag style={{ marginLeft: 8 }}>已售完</Tag> : null}</Text>}
                    description={yuan(item.priceCent)}
                  />
                </List.Item>
              )}
            />
            {lines.length > 0 && tableId ? (
              <OnlinePayment
                testId="scan"
                label={`下单并支付 ${yuan(totalCent)}`}
                start={async () => {
                  const order = await service.placeBarScanOrder(tableId, lines.map((item) => ({ productId: item.productId, quantity: qty[item.productId]! })));
                  scanOrderRef.current = order.scanOrderId;
                  return { paymentIntentId: order.paymentIntentId, businessOrderId: order.scanOrderId, amountCent: order.amountCent,
                    channel: order.channel ?? null, gatewayStatus: order.gatewayStatus ?? null, paymentUrl: order.paymentUrl ?? null, launch: order.launch ?? null };
                }}
                confirm={async () => (await service.confirmBarScanOrder(tableId, scanOrderRef.current ?? "")).status === "paid"}
                onPaid={() => {
                  setPaid(true);
                  setQty({});
                  loadBill(tableId);
                }}
              />
            ) : null}
          </Card>
        )
      ) : null}

      {bill.length > 0 ? (
        <Card title="本桌当前账单" size="small" data-testid="scan-bill">
          <List
            size="small"
            dataSource={bill}
            renderItem={(item, index) => (
              <List.Item key={index}>
                <Text>{item.productName} {yuan(item.unitPriceCent)} × {item.quantity}</Text>
                <Tag color={item.served ? "green" : "orange"}>{item.served ? "已上菜" : "未上菜"}</Tag>
              </List.Item>
            )}
          />
          <Text strong>合计 {yuan(bill.reduce((sum, item) => sum + item.amountCent, 0))}</Text>
        </Card>
      ) : null}
    </Space>
  );
}
