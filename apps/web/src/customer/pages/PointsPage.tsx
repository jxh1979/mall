import { Alert, Button, Card, Empty, List, Space, Spin, Statistic, Tag, Typography, message } from "antd";
import { useEffect, useState } from "react";

import type { CustomerService } from "../client/customerService";
import type { CheckInRecord, PointsRedemption, PointsSummary, RedemptionItem } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";

const { Paragraph, Text } = Typography;

// ⑤ 积分摘要 + 等级（GET /customer/points）+ 连续签到（POST /customer/check-ins, V089）
export function PointsPage({ service }: { service: CustomerService }): React.ReactElement {
  const [summary, setSummary] = useState<PointsSummary | null>(null);
  const [checkIns, setCheckIns] = useState<CheckInRecord[]>([]);
  const [items, setItems] = useState<RedemptionItem[]>([]);
  const [redemptions, setRedemptions] = useState<PointsRedemption[]>([]);
  const [redeemingId, setRedeemingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [checkingIn, setCheckingIn] = useState(false);

  const reload = () => {
    Promise.all([
      service.getPoints(),
      service.listCheckIns(),
      service.listRedemptionItems(),
      service.listRedemptions(),
    ])
      .then(([points, records, catalog, history]) => {
        setSummary(points);
        setCheckIns(records ?? []);
        setItems(catalog ?? []);
        setRedemptions(history ?? []);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "积分加载失败"))
      .finally(() => setLoading(false));
  };

  const onRedeem = async (item: RedemptionItem) => {
    setRedeemingId(item.id);
    try {
      const record = await service.redeemPoints(item.id, 1);
      void message.success(`兑换成功：${record.itemName} ×${record.quantity}，扣 ${record.totalPoints} 积分`);
      reload();
    } catch (err) {
      void message.error(err instanceof Error ? err.message : "兑换失败");
    } finally {
      setRedeemingId(null);
    }
  };

  useEffect(() => {
    reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [service]);

  const onCheckIn = async () => {
    setCheckingIn(true);
    try {
      const record = await service.checkIn();
      void message.success(`签到成功，连续 ${record.streak} 天，获 ${record.awardedPoints} 积分`);
      reload();
    } catch (err) {
      void message.error(err instanceof Error ? err.message : "签到失败");
    } finally {
      setCheckingIn(false);
    }
  };

  return (
    <div data-testid="points-page">
      <Card title="会员积分">
        {error ? <Alert type="error" message="积分加载失败" description={error} style={{ marginBottom: 12 }} /> : null}
        {loading ? (
          <Spin />
        ) : summary ? (
          <Space size="large" wrap>
            <Statistic title="积分余额" value={summary.balance} />
            <Statistic title="会员等级" valueRender={() => <Tag color="gold">{summary.level ? formatBusinessStatus(summary.level) : "普通"}</Tag>} />
          </Space>
        ) : (
          <Empty description="暂无积分数据" />
        )}
      </Card>

      <Card title="连续签到" style={{ marginTop: 16 }}>
        <Paragraph type="secondary">每日签到累积连续天数，按阶梯发放积分。</Paragraph>
        <Button type="primary" onClick={onCheckIn} loading={checkingIn} data-testid="check-in-button">
          今日签到
        </Button>
        <List
          style={{ marginTop: 12 }}
          dataSource={checkIns}
          locale={{ emptyText: "暂无签到记录" }}
          renderItem={(item) => (
            <List.Item data-testid="check-in-record">
              <Text>
                {item.checkInDate} · 连续 {item.streak} 天 · +{item.awardedPoints} 积分
              </Text>
            </List.Item>
          )}
        />
      </Card>

      <Card title="积分商城" style={{ marginTop: 16 }} data-testid="points-redemption-catalog">
        <Paragraph type="secondary">用积分兑换礼品；兑换即按目录扣减积分并扣库存。</Paragraph>
        <List
          dataSource={items}
          locale={{ emptyText: "暂无可兑换礼品" }}
          renderItem={(item) => {
            const soldOut = item.stockQuantity <= 0 || item.status !== "active";
            const affordable = (summary?.balance ?? 0) >= item.pointsCost;
            return (
              <List.Item
                data-testid={`redemption-item-${item.id}`}
                actions={[
                  <Button
                    key="redeem"
                    type="primary"
                    size="small"
                    disabled={soldOut || !affordable}
                    loading={redeemingId === item.id}
                    onClick={() => void onRedeem(item)}
                    data-testid={`redeem-${item.id}`}
                  >
                    {soldOut ? "已兑罄" : affordable ? "兑换" : "积分不足"}
                  </Button>,
                ]}
              >
                <List.Item.Meta
                  title={
                    <Text>
                      {item.itemName} <Tag>{item.itemType}</Tag>
                    </Text>
                  }
                  description={`${item.pointsCost} 积分 · 库存 ${item.stockQuantity}`}
                />
              </List.Item>
            );
          }}
        />
      </Card>

      <Card title="兑换记录" style={{ marginTop: 16 }} data-testid="points-redemption-history">
        <List
          dataSource={redemptions}
          locale={{ emptyText: "暂无兑换记录" }}
          renderItem={(record) => (
            <List.Item data-testid={`redemption-record-${record.id}`}>
              <Text>
                {record.itemName} ×{record.quantity} · 扣 {record.totalPoints} 积分 · {formatBusinessStatus(record.status)}
              </Text>
            </List.Item>
          )}
        />
      </Card>
    </div>
  );
}
