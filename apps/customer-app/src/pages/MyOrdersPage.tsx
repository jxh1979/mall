// 文件版本：V0.04
import { Alert, Button, Card, Empty, List, Modal, Space, Spin, Tag, Typography } from "antd";
import dayjs from "dayjs";
import { useCallback, useEffect, useState } from "react";

import { DynamicCode } from "../components/DynamicCode";
import { OrderPayment, remainingOf } from "../components/OrderPayment";
import type {
  CustomerService,
  CustomerTicket,
  TicketChannelStatus,
  TicketOrder,
  TicketUsage,
} from "@complex/customer-core";
import { formatBusinessStatus } from "@complex/customer-core";

const { Text } = Typography;

const yuan = (cent: number) => `¥${(cent / 100).toFixed(2)}`;
const time = (iso: string | null | undefined) => (iso ? dayjs(iso).format("YYYY年M月D日 HH:mm") : "—");
// PGM「界面全中文」：日期一律中文格式，不随系统语言出 mm/dd/yyyy。
const day = (iso: string) => dayjs(iso).format("YYYY年M月D日");

// ④ 我的订单 / 我的票 / 我的权益 + ⑥ 验票二维码出示（BSM「查看我的订单、我的票、我的权益和使用记录」）。
// 我的订单：来源渠道、支付与出票状态，未付清的订单在此继续组合支付；第三方渠道（美团/大众点评/抖音）未接入时入口置灰、不展示样例。
// 我的票＝本人当前持有的票（含转赠领取来的，GET /customer/my-tickets），按父票聚合，每人份出示动态码、看使用记录；
// 转赠待领取的票不冻结、照常出码（BSM「发起转赠后不冻结该权益」）。2026-10-25 操作员更正「即使注销，门票是不可以退的，除非碰上特殊安全情况」：
// 客人端不设退票入口，安全例外与票种规则允许的退款由员工在后台办理。
export function MyOrdersPage({ service }: { service: CustomerService }): React.ReactElement {
  const [orders, setOrders] = useState<TicketOrder[]>([]);
  const [tickets, setTickets] = useState<CustomerTicket[]>([]);
  const [channels, setChannels] = useState<TicketChannelStatus[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [ticketsError, setTicketsError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [channelFilter, setChannelFilter] = useState<string | null>(null);
  const [qrTicketId, setQrTicketId] = useState<string | null>(null);
  const [usages, setUsages] = useState<{ ticket: CustomerTicket; rows: TicketUsage[] | null; error: string | null } | null>(null);

  const loadTickets = useCallback(() => {
    setTicketsError(null);
    return service.listMyTickets()
      .then((rows) => setTickets(rows ?? []))
      .catch((err) => setTicketsError(err instanceof Error ? err.message : "我的票加载失败"));
  }, [service]);

  useEffect(() => {
    let active = true;
    service
      .listOrders()
      .then((items) => active && setOrders(items ?? []))
      .catch((err) => active && setError(err instanceof Error ? err.message : "订单加载失败"))
      .finally(() => active && setLoading(false));
    void loadTickets();
    // 渠道接入状态只用于显示入口，读不到按未接入置灰。
    service.listTicketChannels().then((rows) => active && setChannels(rows ?? [])).catch(() => undefined);
    return () => {
      active = false;
    };
  }, [service, loadTickets]);

  const openUsages = async (ticket: CustomerTicket) => {
    setUsages({ ticket, rows: null, error: null });
    try {
      setUsages({ ticket, rows: await service.listTicketUsages(ticket.id), error: null });
    } catch (err) {
      setUsages({ ticket, rows: [], error: err instanceof Error ? err.message : "使用记录加载失败" });
    }
  };

  const shownOrders = channelFilter ? orders.filter((order) => order.salesChannel === channelFilter) : orders;

  const renderTickets = () => {
    if (ticketsError) return <Alert type="error" message="我的票加载失败" description={ticketsError} />;
    if (tickets.length === 0) return <Empty description="暂无持有的票" image={Empty.PRESENTED_IMAGE_SIMPLE} />;
    // AUD-066 按父票聚合：同一父票的各人份额（slot）归为一张卡片；无父票（存量）按票实例各自成组。
    const groups = new Map<string, CustomerTicket[]>();
    for (const t of tickets) {
      const key = t.parentTicketId ?? t.id;
      groups.set(key, [...(groups.get(key) ?? []), t]);
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
          const ticketButtons = (m: CustomerTicket, label: string) => (
            <Space key={m.id} size={4}>
              <Button type="primary" size="small" disabled={m.status !== "active"} onClick={() => setQrTicketId(m.id)} data-testid="ticket-qr-open">
                {label}
              </Button>
              <Button size="small" onClick={() => void openUsages(m)} data-testid="ticket-usages-open">使用记录</Button>
            </Space>
          );
          return (
            <List.Item data-testid="ticket-item" style={{ display: "block" }}>
              <List.Item.Meta
                title={
                  <Space wrap>
                    <span>{head.ticketName ?? "票"}</span>
                    <Tag color={head.status === "active" ? "green" : "default"}>{formatBusinessStatus(head.status)}</Tag>
                    {head.numPeople > 1 ? <Tag color="blue">{head.numPeople} 人票</Tag> : null}
                    {members.some((m) => m.transferState === "transferring") ? <Tag color="orange">转赠待领取</Tag> : null}
                    {head.ticketNo ? <Text type="secondary">票号 {head.ticketNo}</Text> : null}
                  </Space>
                }
                description={
                  <Space direction="vertical" size={4} style={{ width: "100%" }}>
                    <Text type="secondary">
                      剩余 {remaining}/{total} 次
                      {head.validTo ? ` · 有效期至 ${day(head.validTo)}` : ""}
                    </Text>
                    <Space wrap>
                      {members.map((m, idx) => ticketButtons(m, members.length > 1 ? `第${m.slotIndex ?? idx + 1}人 出示二维码` : "出示二维码"))}
                    </Space>
                    {Array.from(compensations.entries()).map(([key, rows]) => (
                      <Space wrap key={key} data-testid="ticket-compensation">
                        <Tag color="orange" data-testid="ticket-compensation-tag">
                          补偿（第{rows[0]?.slotIndex ?? 1}人）{rows[0]?.validTo ? ` · 补偿有效期至 ${day(rows[0].validTo)}` : ""}
                        </Tag>
                        {rows.map((c) => ticketButtons(c, "出示二维码"))}
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
  };

  return (
    <div data-testid="my-orders-page">
      <Card title="我的订单">
        {error ? <Alert type="error" message="订单加载失败" description={error} style={{ marginBottom: 12 }} /> : null}
        {channels.length > 0 ? (
          <Space wrap style={{ marginBottom: 12 }}>
            <Text type="secondary">第三方渠道购票：</Text>
            {channels.map((channel) => (
              <Button key={channel.channel} size="small" disabled={!channel.enabled} data-testid={`ticket-channel-${channel.channel}`}
                type={channelFilter === channel.channel ? "primary" : "default"}
                onClick={() => setChannelFilter(channelFilter === channel.channel ? null : channel.channel)}>
                {channel.enabled ? channel.name : `${channel.name}（渠道未接入）`}
              </Button>
            ))}
          </Space>
        ) : null}
        {loading ? (
          <Spin />
        ) : shownOrders.length === 0 ? (
          <Empty description="暂无订单" />
        ) : (
          <List
            dataSource={shownOrders}
            renderItem={(order) => (
              <List.Item data-testid="order-item" style={{ display: "block" }}>
                <List.Item.Meta
                  title={
                    <Space wrap>
                      <span>订单 {order.orderNo}</span>
                      <Tag color="blue">{formatBusinessStatus(order.salesChannel)}</Tag>
                      <Tag color={order.payStatus === "paid" ? "green" : "orange"}>{formatBusinessStatus(order.payStatus)}</Tag>
                      <Tag color={order.issueStatus === "issued" ? "green" : "default"}>{formatBusinessStatus(order.issueStatus)}</Tag>
                    </Space>
                  }
                  description={
                    <Text type="secondary">
                      {order.quantity} 张 · {yuan(order.amountCent ?? 0)} · 游玩日 {order.visitDate ?? "—"}
                    </Text>
                  }
                />
                {order.payStatus === "unpaid" && order.status === "created" && remainingOf(order) > 0 ? (
                  <OrderPayment service={service} order={order}
                    onChange={(next) => {
                      setOrders((previous) => previous.map((item) => (item.id === next.id ? { ...item, ...next } : item)));
                      if (next.payStatus === "paid") void loadTickets();
                    }} />
                ) : null}
              </List.Item>
            )}
          />
        )}
      </Card>

      <Card title="我的票" style={{ marginTop: 16 }}>
        {renderTickets()}
      </Card>

      <Modal open={Boolean(qrTicketId)} onCancel={() => setQrTicketId(null)} footer={null} destroyOnHidden title="出示验票二维码">
        {qrTicketId ? (
          <DynamicCode title="验票二维码" testId="ticket-qr-view" fetchCode={() => service.getTicketQr(qrTicketId)}
            valueOf={(credential) => credential.credentialToken} />
        ) : null}
      </Modal>

      <Modal open={usages !== null} onCancel={() => setUsages(null)} footer={null} destroyOnHidden
        title={`使用记录 · ${usages?.ticket.ticketName ?? "门票"}`}>
        {usages?.error ? <Alert type="error" message={usages.error} /> : null}
        {usages && usages.rows === null ? <Spin /> : null}
        {usages?.rows && usages.rows.length === 0 && !usages.error ? <Empty description="暂无使用记录" /> : null}
        {usages?.rows && usages.rows.length > 0 ? (
          <List size="small" dataSource={usages.rows}
            renderItem={(row) => (
              <List.Item data-testid="ticket-usage-item">
                <Space wrap>
                  <Tag color={row.result === "success" ? "green" : "default"}>{row.resultLabel}</Tag>
                  <span>{time(row.verifiedAt)}</span>
                  {row.merchantName ? <Text type="secondary">{row.merchantName}</Text> : null}
                </Space>
              </List.Item>
            )} />
        ) : null}
      </Modal>
    </div>
  );
}
