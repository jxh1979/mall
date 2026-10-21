import { Alert, Button, Card, Empty, List, Space, Spin, Statistic, Tag, Typography, message } from "antd";
import { useEffect, useState } from "react";

// 文件版本：V0.02
import type { CustomerService, MerchantConsumptionReward, TicketRewardSummary, TicketRewardRule } from "../client/customerService";
import type { CheckInRecord, PointsRedemption, PointsSummary, RedemptionItem } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";

const { Paragraph, Text } = Typography;
const REWARD_STATUS: Record<string, string> = { pending: "待确认", confirmed: "已确认", cancelled: "已取消", reversed: "已冲回" };

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
  const [checkInsNotice, setCheckInsNotice] = useState<string | null>(null);
  const [itemsNotice, setItemsNotice] = useState<string | null>(null);
  const [redemptionsNotice, setRedemptionsNotice] = useState<string | null>(null);
  const [ticketRewards, setTicketRewards] = useState<TicketRewardSummary | null>(null);
  const [ticketNotice, setTicketNotice] = useState<string | null>(null);
  const [rules, setRules] = useState<TicketRewardRule[]>([]);
  const [ruleNotice, setRuleNotice] = useState<string | null>(null);
  const [merchantRewards, setMerchantRewards] = useState<MerchantConsumptionReward[]>([]);
  const [merchantNotice, setMerchantNotice] = useState<string | null>(null);

  const reload = () => {
    // 主资源=积分摘要(getPoints)决定页面级错误；签到/兑换目录/兑换历史为副资源，各自 .catch 降级为空+局部提示，不拖垮积分余额。
    setError(null);
    setCheckInsNotice(null);
    setItemsNotice(null);
    setRedemptionsNotice(null);
    setTicketNotice(null);
    setRuleNotice(null);
    setMerchantNotice(null);
    Promise.all([
      service.getPoints(),
      service.listCheckIns().catch(() => { setCheckInsNotice("签到记录暂时加载失败，不影响积分与兑换"); return [] as CheckInRecord[]; }),
      service.listRedemptionItems().catch(() => { setItemsNotice("兑换目录暂时加载失败，可稍后重试"); return [] as RedemptionItem[]; }),
      service.listRedemptions().catch(() => { setRedemptionsNotice("兑换记录暂时加载失败，可稍后重试"); return [] as PointsRedemption[]; }),
      service.listTicketRewards().catch(() => { setTicketNotice("门票奖励及待追回积分暂时加载失败，请稍后重试"); return null; }),
      service.listTicketRewardRules().catch(() => { setRuleNotice("门票奖励公告暂时加载失败，请稍后重试"); return [] as TicketRewardRule[]; }),
      service.listMerchantRewards().catch(() => { setMerchantNotice("消费积分与成长值暂时加载失败，请稍后重试"); return [] as MerchantConsumptionReward[]; }),
    ])
      .then(([points, records, catalog, history, rewards, announcements, consumption]) => {
        setSummary(points);
        setCheckIns(records ?? []);
        setItems(catalog ?? []);
        setRedemptions(history ?? []);
        setTicketRewards(rewards);
        setRules(announcements);
        setMerchantRewards(consumption);
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

      <Card title="门票积分与成长值" style={{ marginTop: 16 }} data-testid="ticket-rewards">
        <Paragraph type="secondary">奖励归实际使用会员。门票积分按核销时已公布生效的固定值计算，不随会员等级变化；等待期内有退款待处理时继续等待。</Paragraph>
        {ticketNotice ? <Alert type="warning" message={ticketNotice} /> : null}
        {(ticketRewards?.pendingRecovery ?? 0) > 0 ? <Alert type="warning" data-testid="points-recovery" message={`待追回 ${ticketRewards!.pendingRecovery} 积分；新增积分先抵扣待追回部分，结清前暂停兑换。`} /> : null}
        <List dataSource={ticketRewards?.rewards ?? []} locale={{ emptyText: "暂无门票奖励记录" }} renderItem={(reward) => (
          <List.Item key={reward.id}>
            <Space direction="vertical">
              <Text>第 {reward.slotIndex} 人份 · 第 {reward.useIndex} 次 · {reward.points} 积分 · {reward.growth} 成长值</Text>
              <Text type="secondary">{REWARD_STATUS[reward.status] ?? reward.status}{reward.status === "pending" ? ` · 原等待期至 ${new Date(reward.availableAt).toLocaleString("zh-CN")}，退款处理期间不发放` : ""}</Text>
            </Space>
          </List.Item>
        )} />
      </Card>

      {/* 第十一批 TK-04-R5（BSM L877 客人端只展示结果；ENUM L77 与消费单据双向可查）：商户消费积分与成长值，显示来源单号。 */}
      <Card title="消费积分与成长值" style={{ marginTop: 16 }} data-testid="merchant-rewards">
        <Paragraph type="secondary">商户消费在整笔消费全部完成后计算；积分等待期内有退款待处理时继续等待，已退款部分不计。</Paragraph>
        {merchantNotice ? <Alert type="warning" message={merchantNotice} /> : null}
        <List dataSource={merchantRewards} locale={{ emptyText: "暂无消费奖励记录" }} renderItem={(reward) => (
          <List.Item key={reward.id}>
            <Space direction="vertical">
              <Text>{reward.merchantName} · 单号 {reward.sourceNo} · {reward.points} 积分 · {reward.growth} 成长值</Text>
              <Text type="secondary">{REWARD_STATUS[reward.status] ?? reward.status}{reward.status === "pending" && reward.availableAt ? ` · 等待期至 ${new Date(reward.availableAt).toLocaleString("zh-CN")}，退款处理期间不发放` : ""}</Text>
            </Space>
          </List.Item>
        )} />
      </Card>

      <Card title="门票奖励规则公告" style={{ marginTop: 16 }}>
        {ruleNotice ? <Alert type="warning" message={ruleNotice} /> : null}
        <List dataSource={rules} locale={{ emptyText: "暂无已发布规则" }} renderItem={(rule) => <List.Item key={rule.id}>
          <Space direction="vertical"><Text>{rule.projectName} · {rule.ticketName} · {rule.kind === "points" ? "每人次积分" : "每人次成长值"} {rule.fixedValue}</Text>
            <Text type="secondary">{rule.version} · 公告于 {new Date(rule.publishedAt).toLocaleString("zh-CN")} · 生效于 {new Date(rule.effectiveAt).toLocaleString("zh-CN")}</Text></Space>
        </List.Item>} />
      </Card>

      <Card title="连续签到" style={{ marginTop: 16 }}>
        <Paragraph type="secondary">每日签到累积连续天数，按阶梯发放积分。</Paragraph>
        {checkInsNotice ? <Alert type="warning" showIcon message={checkInsNotice} data-testid="points-check-ins-degraded" style={{ marginBottom: 12 }} /> : null}
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
        {itemsNotice ? <Alert type="warning" showIcon message={itemsNotice} data-testid="points-items-degraded" style={{ marginBottom: 12 }} /> : null}
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
                    disabled={soldOut || !affordable || ticketRewards === null || ticketRewards.pendingRecovery > 0}
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
        {redemptionsNotice ? <Alert type="warning" showIcon message={redemptionsNotice} data-testid="points-redemptions-degraded" style={{ marginBottom: 12 }} /> : null}
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
