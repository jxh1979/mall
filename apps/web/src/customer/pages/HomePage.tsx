import { Alert, Card, Empty, List, Spin, Tag, Typography } from "antd";
import { useEffect, useState } from "react";

import type { CustomerService } from "../client/customerService";
import type { TicketProduct } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";

const { Title, Paragraph, Text } = Typography;

function formatYuan(cent: number | null): string {
  if (cent == null) return "—";
  return `¥${(cent / 100).toFixed(2)}`;
}

// ① 首页 / 票种浏览：综合体介绍 + 可售票种、平日/周末/节假日价
export function HomePage({ service }: { service: CustomerService }): React.ReactElement {
  const [products, setProducts] = useState<TicketProduct[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    service
      .listTicketProducts()
      .then((items) => {
        if (active) setProducts(items ?? []);
      })
      .catch((err) => active && setError(err instanceof Error ? err.message : "票种加载失败"))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [service]);

  return (
    <div data-testid="home-page">
      <Card style={{ marginBottom: 16 }}>
        <Title level={4}>欢迎来到综合体</Title>
        <Paragraph type="secondary">
          浏览票种、套餐与通票，在线官方购票、订台，查看我的订单、票、权益与会员积分。
        </Paragraph>
      </Card>
      <Card title="可售票种">
        {error ? <Alert type="error" message="票种加载失败" description={error} style={{ marginBottom: 12 }} /> : null}
        {loading ? (
          <Spin />
        ) : products.length === 0 ? (
          <Empty description="暂无可售票种" />
        ) : (
          <List
            dataSource={products}
            renderItem={(item) => (
              <List.Item data-testid="ticket-product-item">
                <List.Item.Meta
                  title={
                    <span>
                      {item.ticketName} <Tag color={item.status === "active" ? "green" : "default"}>{formatBusinessStatus(item.status)}</Tag>
                    </span>
                  }
                  description={
                    <Text type="secondary">
                      平日 {formatYuan(item.weekdayPriceCent)} · 周末 {formatYuan(item.weekendPriceCent)} · 节假日{" "}
                      {formatYuan(item.holidayPriceCent)}
                    </Text>
                  }
                />
              </List.Item>
            )}
          />
        )}
      </Card>
    </div>
  );
}
