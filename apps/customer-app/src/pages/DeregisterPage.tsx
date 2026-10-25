// 文件版本：V0.03
import { Alert, Button, Card, Checkbox, Empty, List, Spin, Typography } from "antd";
import dayjs from "dayjs";
import { useCallback, useEffect, useState } from "react";

import type { CustomerService, DeregisterPreview, DeregisterPreviewCard } from "@complex/customer-core";

const { Paragraph, Text } = Typography;

function yuan(cent: number): string {
  return `¥${(cent / 100).toFixed(2)}`;
}

function cardLabel(card: DeregisterPreviewCard): string {
  return card.cardScope === "merchant" ? `商户卡（${card.merchantId ?? "-"}）` : "综合体通用卡";
}

// 账户注销（BSM「发起注销时系统须提醒其名下尚有存酒与储值卡余额，客人可以选择不取存酒、放弃余额，径行注销；放弃的存酒按存酒到期规则转为所在酒吧的
// 自有库存资产，放弃的储值卡余额归发卡主体（通用卡归长沙项目公司，商户级卡归发卡商户）。需要退还本金的注销事项按本文件「储值」条自动生成待退事项、
// 通知发卡主体执行」）：先预览，逐卡列本金与赠送金、逐条列名下存酒（所属酒吧、到期日）；名下有存酒的须明确勾选放弃存酒才可注销。2026-10-25 操作员更正
// 「即使注销，门票是不可以退的，除非碰上特殊安全情况」（取代第十三批第 8、12 条）：名下未使用的票注销时作废、不退款，不因此拦注销，本页不设退票入口。
export function DeregisterPage({ service, onDeregistered }: { service: CustomerService; onDeregistered: () => void }): React.ReactElement {
  const [preview, setPreview] = useState<DeregisterPreview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [waive, setWaive] = useState(false);
  const [abandonBottles, setAbandonBottles] = useState(false);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const load = useCallback(() => {
    setError(null);
    service.getDeregisterPreview()
      .then((value) => setPreview(value))
      .catch((err) => setError(err instanceof Error ? err.message : "注销预览加载失败"))
      .finally(() => setLoading(false));
  }, [service]);

  useEffect(() => load(), [load]);

  const cards = preview?.storedValueCards ?? [];
  const bottles = preview?.savedBottles ?? [];
  const hasBalance = cards.some((card) => card.principalCent + card.bonusCent > 0);
  const hasPrincipal = cards.some((card) => card.principalCent > 0);
  const blocked = bottles.length > 0 && !abandonBottles;

  const submit = async () => {
    setBusy(true);
    try {
      await service.deregister({ waiveBalance: waive, abandonSavedBottles: abandonBottles });
      onDeregistered();
    } catch (err) {
      setNotice(err instanceof Error ? err.message : "注销失败");
      load();
    } finally {
      setBusy(false);
    }
  };

  if (loading) return <Spin />;
  if (error) return <Alert type="error" message={error} showIcon />;

  return (
    <Card title="注销账号" data-testid="deregister-page">
      <Paragraph>注销后账号不能再登录和使用，个人信息一律删除，积分与券清零。请先核对：</Paragraph>
      {notice ? <Alert type="error" showIcon message={notice} style={{ marginBottom: 12 }} /> : null}

      {bottles.length ? (
        <div data-testid="deregister-bottles" style={{ marginBottom: 16 }}>
          <Text strong>名下存酒</Text>
          <List dataSource={bottles} renderItem={(bottle) => (
            <List.Item key={bottle.id}>
              <Text>{bottle.barName ?? "酒吧"} · {bottle.bottleName} {bottle.remainingQuantity} {bottle.unit} · 到期 {bottle.expireAt ? dayjs(bottle.expireAt).format("YYYY-MM-DD") : "—"}</Text>
            </List.Item>
          )} />
          <Checkbox checked={abandonBottles} onChange={(event) => setAbandonBottles(event.target.checked)}>
            放弃名下全部存酒（转为所在酒吧的自有库存）
          </Checkbox>
        </div>
      ) : null}

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
        <Button type="primary" danger loading={busy} disabled={blocked} onClick={() => void submit()}>确认注销</Button>
      </div>
    </Card>
  );
}
