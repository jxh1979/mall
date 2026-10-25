// 文件版本：V0.02
import { Alert, Button, Card, Empty, Input, List, Result, Space, Spin, Tag, Typography } from "antd";
import { useEffect, useState } from "react";

import type { BarReservation, BarReservationOption, CustomerService } from "@complex/customer-core";

import { OnlinePayment } from "../components/OnlinePayment";

const { Text } = Typography;

const yuan = (cent: number) => `¥${(cent / 100).toFixed(2)}`;

/** 台位类型（后端 bar_tables.table_type）。 */
export const TABLE_TYPE_LABELS: Record<string, string> = { card_seat: "卡座", vip: "包厢", bar_counter: "吧台位", loose_table: "散台" };

function serviceFeeText(bps: number | null | undefined): string {
  return typeof bps === "number" ? `服务费 ${bps / 100}%` : "服务费比例未设置";
}

/** 不可自助订的原因：已被有效预留占着（ENUM 排他预留）；本酒吧未配定金规则（应付定金为 0，服务端 409 BAR_SELF_RESERVATION_UNAVAILABLE）。 */
function unavailableReason(option: BarReservationOption): string | null {
  if (option.reserved) return "已预留，暂不可订";
  if (!option.depositAmountCent) return "该酒吧暂未开放自助订台";
  return null;
}

/** 订卡规则里的定金条款（BSM L858「订卡时在订卡规则中写明」；L889 定金不抵扣消费……；L1062 超过最晚到店时间未到店的自动释放台位）。 */
const DEPOSIT_RULES = "定金不抵扣消费、不计入最低消费累计；在该台位的消费达到该台位适用的最低消费后，定金原路退回；"
  + "到店未达最低消费、爽约或超时取消的，定金按规则没收。超过最晚到店时间未到店的，台位自动释放、定金按爽约规则没收。";

type Draft = { reservationTime: string; latestArrivalAt: string; guestCount: string; contactPhone: string };
const EMPTY_DRAFT: Draft = { reservationTime: "", latestArrivalAt: "", guestCount: "2", contactPhone: "" };

// ③ 订台（BSM「官方 App 和小程序都可以订台，订台需要支付定金；订台页面须写明该卡座适用的最低消费金额与服务费比例」；BSM「客人自助订台必须支付定金，
// 定金直接进该卡座所属的酒吧商户……每一笔订台都必须填写最晚到店时间」）：选台位（写明最低消费、服务费比例、应付定金与可进人数；已预留与未配定金规则的
// 不可选）→ 填到店时间、最晚到店时间与人数 → 下单 → 付定金（本人订台定金在线付款，渠道确认收款才算付成、才占台）。邀请函由酒吧员工确认订台后签发
// （第十三批裁决第 4 条），在「我的订台」查看。PGM「可以按全局、综合体……开关」：所选台位所属的综合体报给外壳（onProject），订台请求带 X-Project-Id。
export function BarReservationPage({ service, onProject }: { service: CustomerService;
  onProject?: (projectId: string) => void }): React.ReactElement {
  const [options, setOptions] = useState<BarReservationOption[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [picked, setPicked] = useState<BarReservationOption | null>(null);
  const [draft, setDraft] = useState<Draft>(EMPTY_DRAFT);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reservation, setReservation] = useState<BarReservation | null>(null);
  const [paid, setPaid] = useState(false);

  useEffect(() => {
    let active = true;
    service
      .listBarReservationOptions()
      .then((items) => active && setOptions(items ?? []))
      .catch((err) => active && setLoadError(err instanceof Error ? err.message : "可订台位加载失败"))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [service]);

  const set = (key: keyof Draft) => (event: React.ChangeEvent<HTMLInputElement>) => setDraft({ ...draft, [key]: event.target.value });
  const guests = Number(draft.guestCount);
  const ready = Boolean(picked && draft.reservationTime && draft.latestArrivalAt && Number.isInteger(guests) && guests > 0);

  const submit = async () => {
    if (!picked || !ready) return;
    setSubmitting(true);
    setError(null);
    try {
      setReservation(await service.createBarReservation({
        barId: picked.barId,
        tableId: picked.tableId,
        tableType: picked.tableType,
        reservationTime: new Date(draft.reservationTime).toISOString(),
        latestArrivalAt: new Date(draft.latestArrivalAt).toISOString(),
        guestCount: guests,
        contactPhone: draft.contactPhone.trim() || undefined,
      }));
    } catch (err) {
      setError(err instanceof Error ? err.message : "订台失败");
    } finally {
      setSubmitting(false);
    }
  };

  if (reservation) {
    return (
      <Card data-testid="reservation-result">
        {paid ? (
          <div data-testid="reservation-paid">
            <Result status="success" title="定金已支付，台位已为你预留" subTitle="邀请函由酒吧确认后签发，可在「我的订台」查看与转发。" />
          </div>
        ) : (
          <Space direction="vertical" style={{ width: "100%" }}>
            <Text strong>订台已提交，待支付定金 {yuan(reservation.depositAmountCent)}</Text>
            <Text type="secondary">定金付款成功后台位才为你预留；台位被他人先付定金订走时，已付定金原路退回。</Text>
            <OnlinePayment
              testId="deposit"
              label={`支付定金 ${yuan(reservation.depositAmountCent)}`}
              start={() => service.startDepositPayment(reservation.id)}
              confirm={async (intentId) => (await service.confirmDepositPayment(reservation.id, intentId)).depositStatus === "paid"}
              onPaid={() => setPaid(true)}
            />
          </Space>
        )}
      </Card>
    );
  }

  return (
    <div data-testid="bar-reservation-page">
      {loadError ? <Alert type="error" showIcon message="可订台位加载失败" description={loadError} /> : null}
      {loading ? (
        <Spin />
      ) : options.length === 0 ? (
        <Empty description="暂无可订台位" />
      ) : (
        <List
          dataSource={options}
          renderItem={(option) => {
            const reason = unavailableReason(option);
            return (
              <List.Item
                data-testid={`reservation-option-${option.tableId}`}
                actions={[
                  <Button key="pick" type={picked?.tableId === option.tableId ? "primary" : "default"} disabled={Boolean(reason)}
                    onClick={() => { setPicked(option); onProject?.(option.projectId ?? ""); }} data-testid={`reservation-pick-${option.tableId}`}>
                    {picked?.tableId === option.tableId ? "已选" : "选这个"}
                  </Button>,
                ]}
              >
                <List.Item.Meta
                  title={<Space>{option.tableName}<Tag>{TABLE_TYPE_LABELS[option.tableType] ?? "台位"}</Tag></Space>}
                  description={
                    <Space direction="vertical" size={0}>
                      <Text>最低消费 {yuan(option.minConsumptionCent)} · {serviceFeeText(option.serviceFeeRateBps)} · 可进 {option.capacity} 人</Text>
                      {reason ? <Text type="warning">{reason}</Text> : <Text>定金 {yuan(option.depositAmountCent ?? 0)}</Text>}
                    </Space>
                  }
                />
              </List.Item>
            );
          }}
        />
      )}
      {picked ? (
        <Card title={`订 ${picked.tableName}`} style={{ marginTop: 12 }}>
          {error ? <Alert type="error" showIcon message={error} data-testid="reservation-error" style={{ marginBottom: 12 }} /> : null}
          <Space direction="vertical" style={{ width: "100%" }}>
            <label htmlFor="reservation-time">到店时间</label>
            <Input id="reservation-time" type="datetime-local" value={draft.reservationTime} onChange={set("reservationTime")} />
            <label htmlFor="reservation-latest">最晚到店时间</label>
            <Input id="reservation-latest" type="datetime-local" value={draft.latestArrivalAt} onChange={set("latestArrivalAt")} />
            <Text type="secondary" data-testid="reservation-deposit-rules">{DEPOSIT_RULES}</Text>
            <label htmlFor="reservation-guests">人数</label>
            <Input id="reservation-guests" type="number" min={1} value={draft.guestCount} onChange={set("guestCount")} />
            <label htmlFor="reservation-phone">联系手机号（选填）</label>
            <Input id="reservation-phone" type="tel" value={draft.contactPhone} onChange={set("contactPhone")} />
            <Button type="primary" block loading={submitting} disabled={!ready} onClick={() => void submit()} data-testid="reservation-submit">
              提交订台（下一步付定金）
            </Button>
          </Space>
        </Card>
      ) : null}
    </div>
  );
}
