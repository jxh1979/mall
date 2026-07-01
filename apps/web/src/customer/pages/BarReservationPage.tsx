import { Alert, Button, Card, DatePicker, Empty, Form, Input, InputNumber, Result, Select, Space, Spin, Typography } from "antd";
import { useEffect, useState } from "react";

import type { CustomerService } from "../client/customerService";
import type { BarReservation, BarReservationOption, PaymentIntent } from "../types";
import { formatBusinessStatus } from "../i18n/statusLabels";

const { Paragraph, Text } = Typography;

// ③ 订台（选卡座/时段 -> 定金支付意图，V073 deposit）
export function BarReservationPage({ service }: { service: CustomerService }): React.ReactElement {
  const [form] = Form.useForm();
  const [options, setOptions] = useState<BarReservationOption[]>([]);
  const [loading, setLoading] = useState(true);
  const [reservation, setReservation] = useState<BarReservation | null>(null);
  const [intent, setIntent] = useState<PaymentIntent | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let active = true;
    service
      .listBarReservationOptions()
      .then((items) => active && setOptions(items ?? []))
      .catch((err) => active && setError(err instanceof Error ? err.message : "可订台资源加载失败"))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [service]);

  const onSubmit = async (values: {
    tableId: string;
    reservationTime: { toISOString: () => string };
    guestCount: number;
    contactPhone?: string;
  }) => {
    const option = options.find((o) => o.tableId === values.tableId);
    if (!option) {
      setError("请选择卡座/桌台");
      return;
    }
    setSubmitting(true);
    setError(null);
    setReservation(null);
    setIntent(null);
    try {
      const created = await service.createBarReservation({
        barId: option.barId,
        tableId: option.tableId,
        tableType: option.tableType,
        reservationTime: values.reservationTime.toISOString(),
        guestCount: values.guestCount,
        contactPhone: values.contactPhone,
      });
      setReservation(created);
      // 订台需支付定金：为定金金额发起聚合支付意图（V073 deposit）
      if (created.depositAmountCent > 0) {
        const payment = await service.createPaymentIntent({
          businessOrderType: "bar_reservation_deposit",
          businessOrderId: created.id,
          amountCent: created.depositAmountCent,
          currency: "CNY",
        });
        setIntent(payment);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "订台失败");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div data-testid="bar-reservation-page">
      <Card title="酒吧订台（需支付定金）">
        {error ? <Alert type="error" message="订台失败" description={error} style={{ marginBottom: 12 }} /> : null}
        {loading ? (
          <Spin />
        ) : options.length === 0 ? (
          <Empty description="暂无可订卡座" />
        ) : (
          <Form form={form} layout="vertical" onFinish={onSubmit} initialValues={{ guestCount: 2 }}>
            <Form.Item name="tableId" label="卡座 / 桌台" rules={[{ required: true, message: "请选择卡座" }]}>
              <Select
                data-testid="reservation-table"
                placeholder="请选择卡座 / 桌台"
                options={options.map((o) => ({
                  value: o.tableId,
                  label: `${o.tableName}（${o.tableType}·容纳${o.capacity}人·最低消费¥${(o.minConsumptionCent / 100).toFixed(0)}）`,
                }))}
              />
            </Form.Item>
            <Form.Item name="reservationTime" label="到店时段" rules={[{ required: true, message: "请选择时段" }]}>
              <DatePicker showTime style={{ width: "100%" }} />
            </Form.Item>
            <Form.Item name="guestCount" label="人数" rules={[{ required: true }]}>
              <InputNumber min={1} max={50} style={{ width: "100%" }} data-testid="reservation-guests" />
            </Form.Item>
            <Form.Item name="contactPhone" label="联系手机号">
              <Input placeholder="可选" />
            </Form.Item>
            <Button type="primary" htmlType="submit" loading={submitting} block data-testid="reservation-submit">
              提交订台并支付定金
            </Button>
          </Form>
        )}
      </Card>
      {reservation ? (
        <Card style={{ marginTop: 16 }} data-testid="reservation-result">
          <Result
            status="success"
            title="订台已创建"
            subTitle={`定金 ¥${(reservation.depositAmountCent / 100).toFixed(2)} · 状态 ${reservation.depositStatus}`}
          >
            <Space direction="vertical">
              <Text>订台状态：{formatBusinessStatus(reservation.status)}</Text>
              {intent ? (
                <Paragraph type="secondary">
                  已发起定金支付意图（id={String(intent.id ?? "—")}）。真实支付渠道跳转为占位。
                </Paragraph>
              ) : null}
            </Space>
          </Result>
        </Card>
      ) : null}
    </div>
  );
}
