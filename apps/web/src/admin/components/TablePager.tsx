import React from "react";

/**
 * Client-side pagination for the high-volume log tables. Pairs with the rowSearch
 * filter: pass the already-filtered rows in, and pass the active query as resetKey so
 * the page snaps back to 1 whenever the filter changes. The page is also clamped when
 * the (filtered) row set shrinks below the current page.
 */
export function usePagedRows<T>(rows: T[], pageSize: number, resetKey?: unknown) {
  const [page, setPage] = React.useState(1);
  React.useEffect(() => {
    setPage(1);
  }, [resetKey]);
  const pageCount = Math.max(1, Math.ceil(rows.length / pageSize));
  const safePage = Math.min(page, pageCount);
  const start = (safePage - 1) * pageSize;
  return {
    pageRows: rows.slice(start, start + pageSize),
    page: safePage,
    pageCount,
    total: rows.length,
    prev: () => setPage((value) => Math.max(1, value - 1)),
    next: () => setPage((value) => Math.min(pageCount, value + 1)),
  };
}

export function TablePager({
  testid,
  page,
  pageCount,
  total,
  onPrev,
  onNext,
}: {
  testid: string;
  page: number;
  pageCount: number;
  total: number;
  onPrev: () => void;
  onNext: () => void;
}) {
  if (total === 0) {
    return null;
  }
  return (
    <div className="admin-pager" data-testid={testid}>
      <button type="button" data-testid={`${testid}-prev`} disabled={page <= 1} onClick={onPrev}>上一页</button>
      <span data-testid={`${testid}-status`}>第 {page} / {pageCount} 页 · 共 {total} 条</span>
      <button type="button" data-testid={`${testid}-next`} disabled={page >= pageCount} onClick={onNext}>下一页</button>
    </div>
  );
}
