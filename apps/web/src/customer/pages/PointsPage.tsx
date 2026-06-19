import { Alert, Button, Card, Empty, List, Space, Spin, Statistic, Tag, Typography, message } from "antd";
import { useEffect, useState } from "react";

import type { CustomerService } from "../client/customerService";
import type { CheckInRecord, PointsSummary } from "../types";

const { Title, Paragraph, Text } = Typography;

// ⑤ 积分摘要 + 等级（GET /customer/points）+ 连续签到（POST /customer/check-ins, V089）
export function PointsPage({ service }: { service: CustomerService }): React.ReactElement {
  const [summary, setSummary] = useState<PointsSummary | null>(null);
  const [checkIns, setCheckIns] = useState<CheckInRecord[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [checkingIn, setCheckingIn] = useState(false);

  const reload = () => {
    Promise.all([service.getPoints(), service.listCheckIns()])
      .then(([points, records]) => {
        setSummary(points);
        setCheckIns(records ?? []);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "积分加载失败"))
      .finally(() => setLoading(false));
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
            <Statistic title="会员等级" valueRender={() => <Tag color="gold">{summary.level || "普通"}</Tag>} />
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

      <Card title="积分明细 / 礼物兑换" style={{ marginTop: 16 }} data-testid="points-p1-placeholder">
        <Title level={5} type="secondary">
          P1 能力（占位）
        </Title>
        <Paragraph type="secondary">
          积分明细、礼物列表、兑换发起与兑换记录为 P1 客人端能力（底层扣库存规则保留），未进入 P0 OpenAPI，本页仅占位标注。
        </Paragraph>
      </Card>
    </div>
  );
}
