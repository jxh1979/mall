import React from "react";

type Props = {
  navigateTo?: (pageId: string, objectId?: string) => void;
  pageId: string;
  objectId?: string | null;
  children: React.ReactNode;
};

/**
 * 全链可点击下钻链接（条目5 原则「数据可溯源、全链可点击」）：把一个对象引用渲染成可点链接，
 * 点击跳到承载该对象的后台页并按 focusObjectId 定位。无 navigateTo 或无 objectId 时降级为纯文本，不破坏既有展示。
 */
export function TraceLink({ navigateTo, pageId, objectId, children }: Props) {
  const value = objectId ?? undefined;
  if (!navigateTo || !value) {
    return <>{children}</>;
  }
  return (
    <button
      type="button"
      className="admin-trace-link"
      data-testid={`trace-link-${pageId}-${value}`}
      title={`下钻到来源：${value}`}
      onClick={() => navigateTo(pageId, value)}
    >
      {children}
    </button>
  );
}
