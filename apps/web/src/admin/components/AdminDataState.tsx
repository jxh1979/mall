import React from "react";

export function AdminDataState({ loading, error, empty, children, onRetry }: { loading: boolean; error: string | null; empty?: boolean; children: React.ReactNode; onRetry?: () => void }) {
  if (loading) return <div className="admin-state loading-state" data-testid="admin-loading">加载中...</div>;
  if (error) {
    return <div className="admin-state admin-error" data-testid="admin-error"><strong>读取失败</strong><span>{error}</span>{onRetry ? <button type="button" data-testid="admin-retry" onClick={onRetry}>重试</button> : null}</div>;
  }
  if (empty) return <div className="admin-state admin-empty" data-testid="admin-empty">暂无数据</div>;
  return <>{children}</>;
}
