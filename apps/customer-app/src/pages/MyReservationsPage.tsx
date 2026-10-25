// 文件版本：V0.01
import { Alert, Button, Card, Empty, List, Space, Spin, Tag, Typography } from "antd";
import dayjs from "dayjs";
import { QRCodeSVG } from "qrcode.react";
import { useCallback, useEffect, useState } from "react";

import { buildCustomerLink, formatBusinessStatus } from "@complex/customer-core";
import type { BarInvitation, BarInvitationMember, CustomerBarReservation, CustomerService } from "@complex/customer-core";

import { CUSTOMER_LINK_HOST } from "../api";
import { DynamicCode } from "../components/DynamicCode";
import { OnlinePayment } from "../components/OnlinePayment";
import { platform } from "../platform";
import { TABLE_TYPE_LABELS } from "./BarReservationPage";

const { Paragraph, Text } = Typography;

const yuan = (cent: number) => `¥${(cent / 100).toFixed(2)}`;
const time = (value: string | null) => (value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "—");

const DEPOSIT_TEXT: Record<string, string> = { not_required: "免定金", pending: "定金待支付", paid: "定金已支付", settled: "定金已结算",
  timeout_released: "超时已释放" };
const RESERVATION_TEXT: Record<string, string> = { reserved: "已预订", cancelled: "已取消" };
// ENUM「订卡邀请函状态：未签发、已签发、（成员）已确认接受、（成员）已入场、（成员）已取消」。
const MEMBER_TEXT: Record<string, string> = { confirmed: "已确认接受", entered: "已入场", revoked: "已取消" };

function memberName(member: BarInvitationMember, index: number): string {
  return member.isHolder ? "开卡人（本人）" : `被邀请人 ${index}`;
}

// 开卡人的邀请函（BSM「查看自己的订卡邀请函二维码（注明酒吧、卡座、可进人数），转发给朋友……看到已接受邀请的名单与各自是否已入场，并可把尚未入场的
// 被邀请人移出邀请函、收回其名额」）：二维码与分享深链只带邀请函码载荷（口令、防伪标记、卡座、可进人数），不带会话令牌；开卡人本人同样凭动态入场码入场。
function InvitationPanel({ service, reservation }: { service: CustomerService; reservation: CustomerBarReservation }): React.ReactElement {
  const [invitation, setInvitation] = useState<BarInvitation | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<{ type: "success" | "error" | "warning"; text: string } | null>(null);
  const id = reservation.reservationId;

  const load = useCallback(() => {
    service.getMyBarInvitation(id)
      .then((value) => { setInvitation(value); setError(null); })
      .catch((err) => setError(err instanceof Error ? err.message : "邀请函加载失败"));
  }, [service, id]);

  useEffect(() => load(), [load]);

  if (error) return <Alert type="error" showIcon message={error} />;
  if (!invitation) return <Spin />;

  const link = buildCustomerLink(CUSTOMER_LINK_HOST, "invitation", {
    token: invitation.invitationToken ?? "",
    sig: invitation.invitationSignature ?? "",
    table_id: invitation.tableId ?? "",
    capacity: String(invitation.capacity ?? ""),
  });
  const members = invitation.members ?? [];
  const holder = members.find((member) => member.isHolder);
  let invitee = 0;

  const share = async () => {
    if (!link) return;
    const result = await platform.share({ title: `${reservation.barName ?? "酒吧"}订卡邀请函`, link });
    if (result === "copied") setNotice({ type: "success", text: "邀请链接已复制，发给朋友即可" });
    else if (result === "unavailable") setNotice({ type: "warning", text: "本机暂不能分享，请让朋友扫描邀请函二维码" });
  };

  const remove = async (memberId: string) => {
    setBusy(true);
    try {
      await service.removeBarInvitationMember(id, memberId);
      setNotice({ type: "success", text: "已移出，名额已释放" });
      load();
    } catch (err) {
      setNotice({ type: "error", text: err instanceof Error ? err.message : "移出失败" });
    } finally {
      setBusy(false);
    }
  };

  return (
    <Space direction="vertical" style={{ width: "100%" }} data-testid="invitation-detail">
      <Text strong>
        {reservation.barName ?? "酒吧"} · {reservation.tableName ?? "卡座"} · 可进 {invitation.capacity ?? 0} 人
      </Text>
      {notice ? <Alert type={notice.type} showIcon message={notice.text} /> : null}
      <Text type="secondary">已确认 {invitation.confirmedCount ?? 0} 人 · 已入场 {invitation.enteredCount ?? 0} 人 · 剩余 {invitation.remaining ?? 0} 个名额</Text>
      {link ? (
        <>
          <div data-testid="invitation-qr" style={{ display: "flex", justifyContent: "center", padding: 12, background: "#fff" }}>
            <QRCodeSVG value={link} size={180} level="M" title="订卡邀请函二维码" role="img" />
          </div>
          <Paragraph type="secondary" style={{ marginBottom: 0 }}>邀请函二维码只用于转发与接受，不能凭它入场；朋友接受后凭本人的动态入场码入场。</Paragraph>
          <Button type="primary" onClick={() => void share()} data-testid="invitation-share">转发给朋友</Button>
        </>
      ) : (
        <Text type="secondary">客人链接域名未配置，暂不能转发邀请函。</Text>
      )}
      <List
        size="small"
        header="邀请名单"
        dataSource={members}
        renderItem={(member) => {
          const name = memberName(member, member.isHolder ? 0 : ++invitee);
          const removable = !member.isHolder && member.status === "confirmed";
          return (
            <List.Item
              data-testid={`invitation-member-${member.id}`}
              actions={removable ? [
                <Button key="remove" danger size="small" disabled={busy} onClick={() => void remove(member.id)} data-testid={`invitation-remove-${member.id}`}>
                  移出
                </Button>,
              ] : []}
            >
              <Space>
                <Text>{name}</Text>
                <Tag color={member.status === "entered" ? "green" : member.status === "revoked" ? "default" : "blue"}>
                  {MEMBER_TEXT[member.status ?? ""] ?? formatBusinessStatus(member.status)}
                </Tag>
              </Space>
            </List.Item>
          );
        }}
      />
      {holder && holder.status === "confirmed" ? (
        <DynamicCode title="我的动态入场码" fetchCode={() => service.getBarInvitationEntryCode(id)} valueOf={(code) => code.code} />
      ) : null}
    </Space>
  );
}

// clients-R6 我的订台（BSM「订卡客人可在 App/小程序查看自己的订卡邀请函」）：本人订台列表（酒吧、卡座、预订时间、最晚到店时间、定金与订台状态）；
// 定金待支付的在此付定金；邀请函由酒吧员工确认订台后签发（第十三批裁决第 4 条），签发后在此查看、转发与管理名单。
export function MyReservationsPage({ service }: { service: CustomerService }): React.ReactElement {
  const [rows, setRows] = useState<CustomerBarReservation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openId, setOpenId] = useState<string | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    service.listMyBarReservations()
      .then((items) => { setRows(items ?? []); setError(null); })
      .catch((err) => setError(err instanceof Error ? err.message : "我的订台加载失败"))
      .finally(() => setLoading(false));
  }, [service]);

  useEffect(() => load(), [load]);

  if (loading && rows.length === 0) return <Spin />;
  if (error) return <Alert type="error" showIcon message={error} action={<Button onClick={load}>重试</Button>} />;
  if (rows.length === 0) return <Empty description="暂无订台" />;

  return (
    <Space direction="vertical" style={{ width: "100%" }}>
      {rows.map((row) => {
        const payable = row.depositStatus === "pending" && row.status !== "cancelled";
        return (
          <Card key={row.reservationId} size="small" data-testid={`reservation-item-${row.reservationId}`}
            title={`${row.barName ?? "酒吧"} · ${row.tableName ?? "台位"}`}
            extra={<Tag>{RESERVATION_TEXT[row.status] ?? formatBusinessStatus(row.status)}</Tag>}>
            <Space direction="vertical" style={{ width: "100%" }}>
              <Text>{TABLE_TYPE_LABELS[row.tableType ?? ""] ?? "台位"} · {row.guestCount} 人 · 到店 {time(row.reservedAt)} · 最晚 {time(row.latestArrivalAt)}</Text>
              <Text>{DEPOSIT_TEXT[row.depositStatus] ?? formatBusinessStatus(row.depositStatus)} · 定金 {yuan(row.depositAmountCent)}</Text>
              {payable ? (
                <OnlinePayment
                  testId="deposit"
                  label={`支付定金 ${yuan(row.depositAmountCent)}`}
                  start={() => service.startDepositPayment(row.reservationId)}
                  confirm={async (intentId) => (await service.confirmDepositPayment(row.reservationId, intentId)).depositStatus === "paid"}
                  onPaid={load}
                />
              ) : null}
              {row.invitationStatus === "active" ? (
                openId === row.reservationId ? (
                  <InvitationPanel service={service} reservation={row} />
                ) : (
                  <Button onClick={() => setOpenId(row.reservationId)} data-testid={`invitation-open-${row.reservationId}`}>查看邀请函</Button>
                )
              ) : row.invitationStatus === "none" && row.status !== "cancelled" ? (
                <Text type="secondary">邀请函待酒吧确认订台后签发</Text>
              ) : null}
            </Space>
          </Card>
        );
      })}
    </Space>
  );
}
