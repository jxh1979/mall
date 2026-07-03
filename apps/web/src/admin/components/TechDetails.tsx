import React from "react";

/**
 * 技术段落折叠（用户裁决）：「接口证据」「操作留痕」等技术文字默认收起进
 * <details>技术详情</details>，点开后原样展示；业务正文不再直接印接口路径。
 */
export function TechDetails({ children }: { children: React.ReactNode }) {
  return (
    <details className="admin-tech-details">
      <summary>技术详情</summary>
      {children}
    </details>
  );
}
