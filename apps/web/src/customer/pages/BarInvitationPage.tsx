import { Alert, Button, Card, Divider, Form, Input, List, Result, Space, Tag, Typography } from "antd";
import { QRCodeSVG } from "qrcode.react";
import { useState } from "react";

import type { CustomerService } from "../client/customerService";
import type { BarInvitation, BarInvitationMember } from "../types";

const { Paragraph, Text } = Typography;

// V155 订卡邀请函（客人端）：持函人查看/转发自己的邀请函 + 朋友确认收到的邀请（两步之第一步）。
export function BarInvitationPage({ service }: { service: CustomerService }): React.ReactElement {
  const [viewForm] = Form.useForm();
  const [confirmForm] = Form.useForm();
  const [invitation, setInvitation] = useState<BarInvitation | null>(null);
  const [viewError, setViewError] = useState<string | null>(null);
  const [viewLoading, setViewLoading] = useState(false);
  const [confirmed, setConfirmed] = useState<BarInvitationMember | null>(null);
  const [confirmError, setConfirmError] = useState<string | null>(null);
  const [confirming, setConfirming] = useState(false);

  const onView = async (values: { reservationId: string }) => {
    setViewLoading(true);
    setViewError(null);
    setInvitation(null);
    try {
      const found = await service.getMyBarInvitation(values.reservationId.trim());
      setInvitation(found);
    } catch (err) {
      setViewError(err instanceof Error ? err.message : "邀请函查询失败");
    } finally {
      setViewLoading(false);
    }
  };

  const onConfirm = async (values: { invitationToken: string }) => {
    setConfirming(true);
    setConfirmError(null);
    setConfirmed(null);
    try {
      const member = await service.confirmBarInvitation(values.invitationToken.trim());
      setConfirmed(member);
    } catch (err) {
      setConfirmError(err instanceof Error ? err.message : "确认失败");
    } finally {
      setConfirming(false);
    }
  };

  const issued = invitation && invitation.status === "active" && invitation.invitationToken;

  return (
    <div data-testid="bar-invitation-page">
      <Card title="我的订卡邀请函（查看 / 转发）">
        {viewError ? <Alert type="error" message="查询失败" description={viewError} style={{ marginBottom: 12 }} /> : null}
        <Form form={viewForm} layout="vertical" onFinish={onView}>
          <Form.Item name="reservationId" label="订台编号" rules={[{ required: true, message: "请输入订台编号" }]}>
            <Input placeholder="reservation_..." data-testid="invitation-reservation-id" />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={viewLoading} block data-testid="invitation-view-submit">
            查看邀请函
          </Button>
        </Form>
        {invitation ? (
          issued ? (
            <div style={{ marginTop: 16 }} data-testid="invitation-detail">
              <Space direction="vertical" style={{ width: "100%" }}>
                <Text>卡座可进人数：{invitation.capacity} · 已确认 {invitation.confirmedCount} · 已入场 {invitation.enteredCount} · 剩余 {invitation.remaining}</Text>
                <Text type="secondary">邀请函二维码（出示/转发给朋友，朋友扫码或在「确认收到的邀请」处输入口令确认占名额）：</Text>
                <div
                  data-testid="invitation-qr"
                  style={{ background: "#fff", padding: 16, borderRadius: 8, width: "fit-content" }}
                >
                  <QRCodeSVG value={invitation.invitationToken ?? ""} size={180} level="M" />
                </div>
                <Paragraph copyable={{ text: invitation.invitationToken ?? "" }} data-testid="invitation-token">
                  口令（可复制转发）：<Text code>{invitation.invitationToken}</Text>
                </Paragraph>
                <Divider style={{ margin: "8px 0" }}>已确认成员</Divider>
                <List
                  size="small"
                  dataSource={invitation.members ?? []}
                  locale={{ emptyText: "暂无成员" }}
                  renderItem={(member) => (
                    <List.Item data-testid={`invitation-member-${member.id}`}>
                      <Space>
                        <Text>{member.memberIdentifier}</Text>
                        {member.isHolder ? <Tag color="gold">持函人</Tag> : null}
                        <Tag color={member.status === "entered" ? "green" : "blue"}>
                          {member.status === "entered" ? "已入场" : "已确认"}
                        </Tag>
                      </Space>
                    </List.Item>
                  )}
                />
              </Space>
            </div>
          ) : (
            <Alert
              type="info"
              style={{ marginTop: 16 }}
              message="邀请函尚未签发"
              description="该订台的邀请函由前台签发后即可在此查看并转发。"
            />
          )
        ) : null}
      </Card>

      <Card title="确认收到的邀请" style={{ marginTop: 16 }}>
        <Paragraph type="secondary">朋友转发给你邀请函口令后，在此确认接受——确认即占一个名额，到店扫码入场。</Paragraph>
        {confirmError ? <Alert type="error" message="确认失败" description={confirmError} style={{ marginBottom: 12 }} /> : null}
        <Form form={confirmForm} layout="vertical" onFinish={onConfirm}>
          <Form.Item name="invitationToken" label="邀请函口令" rules={[{ required: true, message: "请输入邀请函口令" }]}>
            <Input placeholder="inv_..." data-testid="invitation-confirm-token" />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={confirming} block data-testid="invitation-confirm-submit">
            确认接受邀请
          </Button>
        </Form>
        {confirmed ? (
          <Result
            style={{ marginTop: 16 }}
            status="success"
            title="已确认接受邀请"
            subTitle={`状态 ${confirmed.status} · 到店扫码即可入场`}
            data-testid="invitation-confirm-result"
          />
        ) : null}
      </Card>
    </div>
  );
}
