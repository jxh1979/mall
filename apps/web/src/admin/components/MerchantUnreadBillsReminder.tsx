// 文件版本：V0.01
import React from "react";
import { Alert } from "antd";
import type { AdminPageProps, MerchantUnreadBills } from "../types";

// 第十八批 K 段第 92 条（BSM:1597「商户专区首页保留未读账单提醒，该提醒不因异议期届满而消失。」）：商户形态账号落地页顶部的未读账单提醒。
// 读不到或三组都空就不显示；「查看」进对应页（我的账单带该月），商户在那里看过即算已读。
export function MerchantUnreadBillsReminder({ api, navigateTo }: Pick<AdminPageProps, "api" | "navigateTo">) {
  const [data, setData] = React.useState<MerchantUnreadBills | null>(null);
  React.useEffect(() => {
    api.get<MerchantUnreadBills>("/api/v1/merchant-portal/unread-bills").then(setData).catch(() => undefined);
  }, [api]);
  const months = data?.billMonths ?? [];
  const tickets = data?.ticketSettlementUnreadCount ?? 0;
  const dividends = data?.dividendStatements ?? [];
  if (months.length === 0 && tickets === 0 && dividends.length === 0) return null;
  return (
    <Alert
      type="warning"
      showIcon
      data-testid="merchant-unread-bills"
      style={{ marginBottom: 12 }}
      message="未读账单提醒"
      description={(
        <ul>
          {months.map((row) => {
            const [year, month] = row.month.split("-");
            return (
              <li key={row.month}>
                {`${year} 年 ${Number(month)} 月账单 ${row.unreadCount} 笔未读 `}
                <button type="button" onClick={() => navigateTo?.("ADM-MER-003", row.month)}>查看</button>
              </li>
            );
          })}
          {tickets > 0 ? (
            <li>
              {`门票分账 ${tickets} 笔未读 `}
              <button type="button" onClick={() => navigateTo?.("ADM-MER-005")}>查看</button>
            </li>
          ) : null}
          {dividends.map((row) => (
            <li key={row.id}>
              {`分红单 ${row.periodLabel} 未读 `}
              <button type="button" onClick={() => navigateTo?.("ADM-MER-008")}>查看</button>
            </li>
          ))}
        </ul>
      )}
    />
  );
}
