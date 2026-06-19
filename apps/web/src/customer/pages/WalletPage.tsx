import { Alert, Card, Descriptions, Empty, List, Spin, Statistic, Tag, Typography } from "antd";
import { useEffect, useState } from "react";

import type { CustomerService } from "../client/customerService";
import type { Membership, StoredValueAccount } from "../types";

const { Paragraph } = Typography;

// ⑦ 储值 / 会员（V090，可选展示）
export function WalletPage({ service }: { service: CustomerService }): React.ReactElement {
  const [account, setAccount] = useState<StoredValueAccount | null>(null);
  const [memberships, setMemberships] = useState<Membership[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    Promise.all([
      service.getStoredValueAccount().catch(() => null),
      service.listMemberships().catch(() => []),
    ])
      .then(([acc, plans]) => {
        if (!active) return;
        setAccount(acc);
        setMemberships(plans ?? []);
      })
      .catch((err) => active && setError(err instanceof Error ? err.message : "钱包加载失败"))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [service]);

  return (
    <div data-testid="wallet-page">
      {error ? <Alert type="error" message="钱包加载失败" description={error} style={{ marginBottom: 12 }} /> : null}
      <Card title="储值余额">
        {loading ? (
          <Spin />
        ) : account ? (
          <Descriptions column={1} size="small" bordered>
            <Descriptions.Item label="余额">
              <Statistic value={account.balanceCent / 100} precision={2} prefix="¥" />
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={account.status === "active" ? "green" : "default"}>{account.status}</Tag>
            </Descriptions.Item>
          </Descriptions>
        ) : (
          <Empty description="暂无储值账户" />
        )}
      </Card>

      <Card title="我的会员" style={{ marginTop: 16 }}>
        {loading ? (
          <Spin />
        ) : (
          <List
            dataSource={memberships}
            locale={{ emptyText: "暂无会员订阅" }}
            renderItem={(item) => (
              <List.Item data-testid="membership-item">
                <List.Item.Meta
                  title={
                    <span>
                      会员 {item.planId} <Tag color={item.status === "active" ? "green" : "default"}>{item.status}</Tag>
                    </span>
                  }
                  description={
                    <Paragraph type="secondary" style={{ marginBottom: 0 }}>
                      有效期 {item.startAt} ~ {item.expireAt} · 自动续费 {item.autoRenew ? "是" : "否"}
                    </Paragraph>
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
