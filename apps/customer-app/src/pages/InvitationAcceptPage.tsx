// 文件版本：V0.02
import { Alert, Button, Card, Result, Space, Typography } from "antd";
import dayjs from "dayjs";
import { useCallback, useEffect, useState } from "react";

import { parseCustomerLink } from "@complex/customer-core";
import type { BarInvitationCode, BarInvitationMember, BarInvitationPreview, CustomerService } from "@complex/customer-core";

import { CUSTOMER_LINK_HOST } from "../api";
import { DynamicCode } from "../components/DynamicCode";
import { platform } from "../platform";

const { Paragraph, Text } = Typography;

/** 深链参数（customer-core 邀请函路由已逐个校验格式）→ 预览与接受要原样回传的码载荷。 */
function codeOf(params: Record<string, string>): BarInvitationCode {
  return { invitationToken: params.token ?? "", invitationSignature: params.sig ?? "", tableId: params.table_id ?? "", capacity: Number(params.capacity) };
}

// 接受订卡邀请（BSM「朋友在 App/小程序确认接受后占一个名额，该名额自动关联到接受人本人的动态入场码，到店出示本人的动态入场码入场」；BSM「邀请函二维码
// 只用于转发与接受，本身不作为入场凭证（邀请函二维码带防伪标记，被复制或篡改卡座、可进人数即无法接受）」）：开卡人转发的深链或扫邀请函二维码进来先预览
// （不占名额），点「确认接受」才占名额；接受后（或已接受过的再打开、再确认返回原名额）出示本人动态入场码。
// 第十八批 R 段第 152 条 152-R3（第十八批裁决 34「销售可以通过微信等发送邀请函，客户点击后确定自己是订台人还是参与者」；BSM「邀请函上的人分两种身份：
// 开卡人（订下这个卡座的人）与被邀请人（接受邀请占用名额的人）」）：员工代订、一体机订台还没有订台人时（预览 holderClaimable），给「我是订台人」「我是参与者」两个选项。
export function InvitationAcceptPage({ service, initialLink }: { service: CustomerService; initialLink?: Record<string, string> }): React.ReactElement {
  const [code, setCode] = useState<BarInvitationCode | null>(initialLink ? codeOf(initialLink) : null);
  const [preview, setPreview] = useState<BarInvitationPreview | null>(null);
  const [member, setMember] = useState<BarInvitationMember | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!code) return;
    let active = true;
    setPreview(null);
    setError(null);
    service.previewBarInvitation(code)
      .then((value) => active && setPreview(value))
      .catch((err) => active && setError(err instanceof Error ? err.message : "邀请函无效"));
    return () => {
      active = false;
    };
  }, [service, code]);

  const scan = useCallback(async () => {
    setError(null);
    const raw = await platform.scanCode();
    if (raw === null) {
      setError("本机暂不能扫码，请打开朋友转发的邀请链接");
      return;
    }
    const link = parseCustomerLink(raw, CUSTOMER_LINK_HOST);
    if (link?.route !== "invitation") {
      setError("无法识别的二维码");
      return;
    }
    setCode(codeOf(link.params));
  }, []);

  const accept = async (asHolder: boolean) => {
    if (!code) return;
    setBusy(true);
    setError(null);
    try {
      setMember(await service.confirmBarInvitation(code, asHolder));
    } catch (err) {
      setError(err instanceof Error ? err.message : "接受邀请失败");
    } finally {
      setBusy(false);
    }
  };

  const reservationId = member?.reservationId ?? preview?.reservationId;

  return (
    <Space direction="vertical" style={{ width: "100%" }}>
      {error ? <Alert type="error" showIcon message={error} data-testid="invitation-error" /> : null}
      {member && reservationId ? (
        <>
          {member.isHolder && preview?.holderClaimable ? (
            <Result status="success" title="你已成为这张订台的订台人" subTitle="可在「我的订台」查看名单、转发邀请函；到店出示下方本人的动态入场码入场。" />
          ) : (
            <Result status="success" title="已接受邀请，名额已为你保留" subTitle="到店出示下方本人的动态入场码入场。" />
          )}
          <DynamicCode title="我的动态入场码" fetchCode={() => service.getBarInvitationEntryCode(reservationId)} valueOf={(entry) => entry.code} />
        </>
      ) : preview ? (
        <Card data-testid="invitation-preview" title="订卡邀请函">
          <Space direction="vertical" style={{ width: "100%" }}>
            <Text strong>{preview.barName ?? "酒吧"} · {preview.tableName ?? "卡座"} · 可进 {preview.capacity} 人</Text>
            <Text>预订时间 {preview.reservedAt ? dayjs(preview.reservedAt).format("YYYY-MM-DD HH:mm") : "—"} · 剩余 {preview.remaining} 个名额</Text>
            {preview.holderClaimable ? (
              <>
                <Paragraph type="secondary" style={{ marginBottom: 0 }}>
                  这张订台还没有订台人：订台的客人本人请选「我是订台人」，订台归到你名下，可在「我的订台」查看名单、转发邀请函；朋友请选「我是参与者」，占一个名额。
                </Paragraph>
                <Button type="primary" block loading={busy} onClick={() => void accept(true)} data-testid="invitation-accept-holder">我是订台人</Button>
                <Button block loading={busy} onClick={() => void accept(false)} data-testid="invitation-accept">我是参与者</Button>
              </>
            ) : (
              <>
                <Paragraph type="secondary" style={{ marginBottom: 0 }}>确认接受即占用一个名额，凭本人的动态入场码免通票进入这家酒吧。</Paragraph>
                <Button type="primary" block loading={busy} onClick={() => void accept(false)} data-testid="invitation-accept">确认接受</Button>
              </>
            )}
          </Space>
        </Card>
      ) : null}
      {!code ? <Paragraph type="secondary">打开朋友转发的订卡邀请链接，或扫朋友出示的邀请函二维码。</Paragraph> : null}
      {!member ? (
        <Button block onClick={() => void scan()} data-testid="invitation-scan">扫邀请函二维码</Button>
      ) : null}
    </Space>
  );
}
