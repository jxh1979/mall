// 文件版本：V0.01
// 第十八批 BP 段 300-R2（第十八批裁决 74「要审批也留版本」：域内票与跨域组合通票同一做法，审批人换成该域主管公司的人；PGM:528「发起单据时只需指定
//   该事项的最终审批人」）：新建票种向导里没挂酒吧的票发布要填最终审批人用户编号（单一酒吧门票与最高权限本人发起不填）。
import React from "react";

export function TicketFinalApproverField({ value, onChange }: { value: string; onChange: (value: string) => void }) {
  return (
    <label>最终审批人用户编号（必填：酒吧通票、游乐票等填对应域主管公司的审批人；跨域组合通票填项目之上的平台审批人）
      <input data-testid="ticket-final-approver" value={value} onChange={(event) => onChange(event.target.value)}
        placeholder="审批人用户编号" aria-label="最终审批人用户编号" />
    </label>
  );
}
