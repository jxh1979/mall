// 文件版本：V0.01
import { List, Modal } from "antd";
import dayjs from "dayjs";
import { useEffect, useRef, useState } from "react";

import type { CustomerService, TicketExpiryReminder as Reminder } from "@complex/customer-core";

import { platform } from "../platform";

// 未核销票到期提醒（BSM「未核销的票在到期前通过官方 App 或小程序弹出的到期提醒（业务参数，按项目可配，出厂默认到期前二十四小时，
// 须同时配置上下限）」）：登录后（含恢复会话）与回到前台时拉取本人即将到期的未核销票并弹出；提前量由服务端按项目参数判定，客户端不设关闭此类提醒的开关。
// 已点「知道了」的票本次运行内不再重复弹（只记在内存）。应用关闭时的系统推送需推送凭证，未接入。
export function TicketExpiryReminder({ service }: { service: CustomerService }): React.ReactElement | null {
  const [pending, setPending] = useState<Reminder[]>([]);
  const seen = useRef(new Set<string>());

  useEffect(() => {
    const load = () => {
      service
        .listTicketExpiryReminders()
        .then((rows) => setPending((rows ?? []).filter((row) => !seen.current.has(row.ticketId))))
        .catch(() => undefined); // 提醒拉取失败不打断客人当前操作，下次回前台再拉。
    };
    load();
    return platform.onResume(load);
  }, [service]);

  if (pending.length === 0) return null;
  const dismiss = () => {
    for (const row of pending) seen.current.add(row.ticketId);
    setPending([]);
  };
  return (
    <Modal open title="门票即将到期" onOk={dismiss} onCancel={dismiss} okText="知道了" cancelButtonProps={{ style: { display: "none" } }}>
      <div data-testid="ticket-expiry-reminder">
        <List
          size="small"
          dataSource={pending}
          renderItem={(row) => (
            <List.Item key={row.ticketId}>
              {row.ticketName ?? "门票"} 将于 {dayjs(row.validTo).format("YYYY年M月D日 HH:mm")} 到期，请在到期前使用
            </List.Item>
          )}
        />
      </div>
    </Modal>
  );
}
