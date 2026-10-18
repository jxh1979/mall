// 文件版本：V0.01
import React from "react";
import { cnDate } from "../components/RowDetailDrawer";

export type SiteEntryHistoryRow = {
  sequence: number; action: "completion" | "acceptance"; actorId: string; occurredAt: string;
  attachmentIds: string[]; note?: string | null; result?: "pass" | "rectify" | null; completionSequence?: number | null;
};

export function SiteEntryHistory({ reportId, items }: { reportId: string; items: SiteEntryHistoryRow[] }) {
  return <section aria-label="完工材料与验收历史">
    <h3>完工材料与验收历史</h3>
    {items.length === 0 ? <p>尚无完工材料</p> : <ol>{items.map(item => <li key={item.sequence}>
      <strong>{item.action === "completion" ? "提交完工材料" : item.result === "pass" ? "验收通过" : "要求整改"}</strong>
      {" · "}{cnDate(item.occurredAt)}{" · "}{item.actorId}
      {item.completionSequence ? <span> · 对应第 {item.completionSequence} 条材料</span> : null}
      <p>{item.note || "无补充说明"}</p>
      {(item.attachmentIds ?? []).map((id, index) => <a key={id}
        href={`/api/v1/facility/site-entry-reports/${encodeURIComponent(reportId)}/attachments/${encodeURIComponent(id)}/file`}
        target="_blank" rel="noreferrer">查看材料 {index + 1} </a>)}
    </li>)}</ol>}
  </section>;
}
