import React from "react";

/** 错误既可是纯文案，也可带技术详情（useAdminResource 传入 {message, detail}）。 */
export type AdminDataError = string | { message: string; detail?: string | null } | null;

export function AdminDataState({ loading, error, empty, children, onRetry }: { loading: boolean; error: AdminDataError; empty?: boolean; children: React.ReactNode; onRetry?: () => void }) {
  if (loading) return <div className="admin-state loading-state" data-testid="admin-loading">加载中...</div>;
  if (error) {
    const message = typeof error === "string" ? error : error.message;
    const detail = typeof error === "string" ? null : error.detail ?? null;
    return (
      <div className="admin-state admin-error" data-testid="admin-error">
        <strong>读取失败</strong>
        <span>{message}</span>
        {detail ? (
          <details className="admin-tech-details" data-testid="admin-error-detail">
            <summary>技术详情</summary>
            <pre>{detail}</pre>
          </details>
        ) : null}
        {onRetry ? <button type="button" data-testid="admin-retry" onClick={onRetry}>重试</button> : null}
      </div>
    );
  }
  if (empty) return <div className="admin-state admin-empty" data-testid="admin-empty">暂无数据</div>;
  return <>{children}</>;
}
