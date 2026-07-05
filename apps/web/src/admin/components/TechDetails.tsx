import React from "react";

/**
 * 技术段落折叠（用户裁决）：「接口证据」「操作留痕」等技术文字默认收起进
 * <details>技术详情</details>，点开后原样展示；业务正文不再直接印接口路径。
 * label 可覆盖折叠标题（如「操作留痕」），默认「技术详情」；同页两块不同标题避免重复。
 */
export function TechDetails({ children, label = "技术详情" }: { children: React.ReactNode; label?: string }) {
  return (
    <details className="admin-tech-details">
      <summary>{label}</summary>
      {children}
    </details>
  );
}
