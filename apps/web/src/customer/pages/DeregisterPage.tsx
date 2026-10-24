// 文件版本：V0.02
import { Alert, Button, Card, Checkbox, Empty, List, message, Spin, Typography } from "antd";
import { useEffect, useState } from "react";

import type { CustomerService } from "../client/customerService";
import type { DeregisterPreviewCard } from "../types";

const { Paragraph, Text } = Typography;

function yuan(cent: number): string {
  return `¥${(cent / 100).toFixed(2)}`;
}

function cardLabel(card: DeregisterPreviewCard): string {
  return card.cardScope === "merchant" ? `商户卡（${card.merchantId ?? "-"}）` : "综合体通用卡";
}

// TK-21-R3（BSM「账户注销」：「发起注销时系统须提醒其名下尚有……储值卡余额，客人可以选择……放弃余额，径行注销；
// 放弃的储值卡余额归发卡主体（通用卡归长沙项目公司，商户级卡归发卡商户）。需要退还本金的注销事项按本文件「储值」条自动生成待退事项、
// 通知发卡主体执行」）：先预览各卡本金/赠送金，再选择是否放弃余额后确认注销。
export function DeregisterPage({ service, onDeregistered }: { service: CustomerService; onDeregistered: () => void }): React.ReactElement {
  const [cards, setCards] = useState<DeregisterPreviewCard[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [waive, setWaive] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    service.getDeregisterPreview()
      .then((preview) => { if (active) setCards(preview?.storedValueCards ?? []); })
      .catch((err) => { if (active) setError(err instanceof Error ? err.message : "注销预览加载失败"); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [service]);

  const hasBalance = cards.some((card) => card.principalCent + card.bonusCent > 0);
  const hasPrincipal = cards.some((card) => card.principalCent > 0);

  const submit = async () => {
    setBusy(true);
    try {
      await service.deregister(waive);
      void message.success("账号已注销");
      onDeregistered();
    } catch (err) {
      void message.error(err instanceof Error ? err.message : "注销失败");
    } finally {
      setBusy(false);
    }
  };

  if (loading) return <Spin />;
  if (error) return <Alert type="error" message={error} showIcon />;

  return (
    <Card title="注销账号" data-testid="deregister-page">
      <Paragraph>注销后账号不能再登录和使用。请先核对名下储值卡余额：</Paragraph>
      {cards.length === 0 || !hasBalance ? (
        <Empty description="名下没有储值卡余额" />
      ) : (
        <List
          dataSource={cards}
          renderItem={(card) => (
            <List.Item key={card.accountId}>
              <List.Item.Meta
                title={cardLabel(card)}
                description={`本金 ${yuan(card.principalCent)} · 赠送金 ${yuan(card.bonusCent)}`}
              />
            </List.Item>
          )}
        />
      )}
      {hasBalance ? (
        <>
          <Paragraph>
            <Text>不放弃余额：{hasPrincipal ? "需退还的本金会自动生成待退事项并通知发卡主体，按原退款路径退还；" : ""}赠送金不退现、作废。</Text>
          </Paragraph>
          <Checkbox checked={waive} onChange={(event) => setWaive(event.target.checked)} data-testid="deregister-waive">
            放弃全部余额（归发卡主体）
          </Checkbox>
        </>
      ) : null}
      <div style={{ marginTop: 16 }}>
        <Button type="primary" danger loading={busy} onClick={() => void submit()}>确认注销</Button>
      </div>
    </Card>
  );
}
