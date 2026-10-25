// 文件版本：V0.01
import { Alert, Button, Card, Empty, Input, Modal, Space, Spin, Typography } from "antd";
import dayjs from "dayjs";
import { useCallback, useEffect, useState } from "react";

import type { CustomerSavedBottle, CustomerService, SavedBottleRenewal } from "@complex/customer-core";

import { DynamicCode } from "../components/DynamicCode";

const { Text } = Typography;

const day = (value: string | null | undefined) => (value ? dayjs(value).format("YYYY-MM-DD") : "—");

// 转存（ENUM「转存须客人本人短信验证码确认」）：先给本人手机发码，填码确认后从当前到期日顺延（次数上限与只能在到期日前办理由服务端判定）。
function RenewDialog({ service, bottle, onClose, onRenewed }: {
  service: CustomerService;
  bottle: CustomerSavedBottle;
  onClose: () => void;
  onRenewed: () => void;
}): React.ReactElement {
  const [smsCode, setSmsCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<SavedBottleRenewal | null>(null);
  const [sent, setSent] = useState(false);

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setError(null);
    try {
      await action();
    } catch (err) {
      setError(err instanceof Error ? err.message : "操作失败");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal open title={`转存：${bottle.bottleName}`} onCancel={onClose} footer={<Button onClick={onClose}>关闭</Button>} destroyOnHidden>
      {result ? (
        <Alert type="success" showIcon data-testid="saved-bottle-renew-result" message={`已转存，新到期日 ${day(result.newExpireAt)}`} />
      ) : (
        <Space direction="vertical" style={{ width: "100%" }}>
          {error ? <Alert type="error" showIcon message={error} /> : null}
          <Text>当前到期日 {day(bottle.expireAt)}。验证码发到你的注册手机号。</Text>
          <Button disabled={busy} onClick={() => void run(async () => {
            await service.requestSavedBottleRenewalCode(bottle.id);
            setSent(true);
          })} data-testid="saved-bottle-renew-send">{sent ? "重新发送验证码" : "发送验证码"}</Button>
          {sent ? <Text type="success">验证码已发送</Text> : null}
          <label htmlFor="saved-bottle-sms">短信验证码</label>
          <Input id="saved-bottle-sms" inputMode="numeric" autoComplete="one-time-code" value={smsCode} onChange={(e) => setSmsCode(e.target.value)} />
          <Button type="primary" loading={busy} disabled={!smsCode.trim()} onClick={() => void run(async () => {
            setResult(await service.renewSavedBottle(bottle.id, smsCode.trim()));
            onRenewed();
          })} data-testid="saved-bottle-renew-confirm">确认转存</Button>
        </Space>
      )}
    </Modal>
  );
}

// 我的存酒（BSM「查看本人名下全部存酒、到期日与动态取酒码。存酒只能在存入的那家酒吧取用……「我的存酒」为跨店汇总展示，每条记录标明所属酒吧，取酒须到
// 该酒吧办理。门店退出后仍在保管有效期内的存酒……由所属娱乐平台公司接收，并在指定地点领取」）：取酒码为按全场统一间隔刷新的动态码，只画进二维码。
export function SavedBottlesPage({ service }: { service: CustomerService }): React.ReactElement {
  const [bottles, setBottles] = useState<CustomerSavedBottle[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showing, setShowing] = useState<string | null>(null);
  const [renewing, setRenewing] = useState<CustomerSavedBottle | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    service.listMySavedBottles()
      .then((rows) => { setBottles(rows ?? []); setError(null); })
      .catch((err) => setError(err instanceof Error ? err.message : "我的存酒加载失败"))
      .finally(() => setLoading(false));
  }, [service]);

  useEffect(() => load(), [load]);

  if (loading && bottles.length === 0) return <Spin />;
  if (error) return <Alert type="error" showIcon message={error} action={<Button onClick={load}>重试</Button>} />;
  if (bottles.length === 0) return <Empty description="名下暂无存酒" />;

  return (
    <Space direction="vertical" style={{ width: "100%" }}>
      {bottles.map((bottle) => (
        <Card key={bottle.id} size="small" title={bottle.barName ?? "酒吧"} data-testid={`saved-bottle-${bottle.id}`}>
          <Space direction="vertical" style={{ width: "100%" }}>
            <Text strong>{bottle.bottleName} · 剩余 {bottle.remainingQuantity} {bottle.unit}</Text>
            <Text>到期 {day(bottle.expireAt)}</Text>
            <Text type="secondary">
              {bottle.handedOverToPlatform
                ? `门店已退出，请到${bottle.pickupLocation ?? "平台公司指定地点"}领取`
                : `取酒须到${bottle.barName ?? "存入的酒吧"}办理`}
            </Text>
            <Space wrap>
              <Button type="primary" onClick={() => setShowing(showing === bottle.id ? null : bottle.id)} data-testid={`saved-bottle-code-${bottle.id}`}>
                {showing === bottle.id ? "收起取酒码" : "出示取酒码"}
              </Button>
              <Button onClick={() => setRenewing(bottle)} data-testid={`saved-bottle-renew-${bottle.id}`}>转存</Button>
            </Space>
            {showing === bottle.id ? (
              <DynamicCode title="动态取酒码" fetchCode={() => service.getSavedBottlePickupCode(bottle.id)} valueOf={(code) => code.code} />
            ) : null}
          </Space>
        </Card>
      ))}
      {renewing ? <RenewDialog service={service} bottle={renewing} onClose={() => setRenewing(null)} onRenewed={load} /> : null}
    </Space>
  );
}
