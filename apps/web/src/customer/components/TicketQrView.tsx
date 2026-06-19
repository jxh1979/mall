import { Alert, Button, Card, Descriptions, Space, Tag, Typography } from "antd";
import { useCallback, useEffect, useRef, useState } from "react";

import { deriveRefreshIntervalSeconds } from "../client/customerClient";
import type { CustomerService } from "../client/customerService";
import type { TicketCredential } from "../types";

const { Text, Paragraph } = Typography;

type TicketQrViewProps = {
  service: CustomerService;
  ticketId: string;
};

// 防伪验票二维码展示 + 30/60 秒在线刷新（T4）。
// 刷新间隔按凭证 issuedAt/expiresAt 推导（缺省 60s），每个窗口重新拉取，refreshedCount 递增。
// 截图无效：二维码原始值不持久化，只在线渲染。
export function TicketQrView({ service, ticketId }: TicketQrViewProps): React.ReactElement {
  const [credential, setCredential] = useState<TicketCredential | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [countdown, setCountdown] = useState<number>(0);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const intervalSeconds = credential
    ? deriveRefreshIntervalSeconds(credential.issuedAt, credential.expiresAt)
    : 60;

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const next = await service.getTicketQr(ticketId);
      setCredential(next);
      setCountdown(deriveRefreshIntervalSeconds(next.issuedAt, next.expiresAt));
    } catch (err) {
      setError(err instanceof Error ? err.message : "二维码加载失败");
    } finally {
      setLoading(false);
    }
  }, [service, ticketId]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  // 倒计时归零时自动重新拉取，实现 30/60 秒在线刷新。
  useEffect(() => {
    if (timerRef.current) clearInterval(timerRef.current);
    timerRef.current = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          void refresh();
          return intervalSeconds;
        }
        return prev - 1;
      });
    }, 1000);
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [refresh, intervalSeconds]);

  return (
    <Card title="验票二维码" data-testid="ticket-qr-view">
      {error ? <Alert type="error" message="二维码加载失败" description={error} style={{ marginBottom: 12 }} /> : null}
      <Paragraph type="secondary">
        二维码每 {intervalSeconds} 秒在线刷新，截图无效，现场必须在线核验。距下次刷新 {countdown} 秒。
      </Paragraph>
      {credential ? (
        <Space direction="vertical" style={{ width: "100%" }}>
          <div
            data-testid="qr-token"
            style={{
              fontFamily: "monospace",
              wordBreak: "break-all",
              background: "#0f172a",
              color: "#a7f3d0",
              padding: 16,
              borderRadius: 8,
            }}
          >
            {credential.credentialToken}
          </div>
          <Descriptions size="small" column={1} bordered>
            <Descriptions.Item label="票 ID">{credential.ticketId}</Descriptions.Item>
            <Descriptions.Item label="凭证类型">{credential.credentialType}</Descriptions.Item>
            <Descriptions.Item label="有效期至">{credential.expiresAt}</Descriptions.Item>
            <Descriptions.Item label="刷新次数">
              <Tag color="blue" data-testid="qr-refresh-count">
                {credential.refreshedCount}
              </Tag>
            </Descriptions.Item>
          </Descriptions>
        </Space>
      ) : (
        <Text type="secondary">加载中...</Text>
      )}
      <div style={{ marginTop: 12 }}>
        <Button onClick={() => void refresh()} loading={loading} data-testid="qr-manual-refresh">
          立即刷新
        </Button>
      </div>
    </Card>
  );
}
