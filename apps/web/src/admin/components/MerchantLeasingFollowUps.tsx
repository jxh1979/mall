// 文件版本：V0.01
import React from "react";
import type { AdminPageProps } from "../types";
import { KEY_VALUE_LABELS } from "../i18n/fieldLabels";
import { cnDate } from "./RowDetailDrawer";

type LeasingFollowUp = { id: string; method?: string | null; content?: string | null; createdAt?: string | null };

// 第十八批 BW 段第 385 条 385-R2（PGM:502「签约转商户自动带入联系人与联系方式、意向业态/面积/位置、报价租金条款和跟进历史，……」）：
// 商户档案详情里的招商跟进历史——后端按来源线索读出（线索本身加该线索各意向的跟进），逐条「时间 · 方式 · 内容」，方式取共用字典中文名。
export function MerchantLeasingFollowUps({ api, merchantId }: Pick<AdminPageProps, "api"> & { merchantId: string }) {
  const [rows, setRows] = React.useState<LeasingFollowUp[] | null>(null);
  const [failed, setFailed] = React.useState(false);
  React.useEffect(() => {
    let alive = true;
    api.get<LeasingFollowUp[]>(`/api/v1/leasing/merchants/${encodeURIComponent(merchantId)}/follow-ups`)
      .then((items) => { if (alive) setRows(items ?? []); })
      .catch(() => { if (alive) setFailed(true); });
    return () => { alive = false; };
  }, [api, merchantId]);
  if (failed) return <span data-testid="merchant-leasing-follow-ups">招商跟进历史读取失败</span>;
  if (!rows) return null;
  if (rows.length === 0) return <span data-testid="merchant-leasing-follow-ups">没有招商跟进记录</span>;
  return (
    <ul data-testid="merchant-leasing-follow-ups">
      {rows.map((row) => <li key={row.id}>{cnDate(row.createdAt)} · {KEY_VALUE_LABELS.method?.[row.method ?? ""] ?? "—"} · {row.content ?? "—"}</li>)}
    </ul>
  );
}
