import React from "react";
import type { PagedResult } from "../client/adminClient";
import { formatApiErrorDetail } from "../../shared/apiError";

export interface ServerPagedState<T> {
  rows: T[];
  total: number;
  page: number;
  pageCount: number;
  loading: boolean;
  error: { message: string; detail: string | null } | null;
  /** 当前生效(已提交)的关键词——匹配计数/空态文案用它,而非输入框草稿。 */
  keyword: string;
  prev: () => void;
  next: () => void;
  /** 提交新关键词并跳回第 1 页(即时搜索降级为「输关键词→查询」)。 */
  search: (keyword: string) => void;
  /** 重新拉当前页(写操作后刷新)。 */
  reload: () => void;
}

/**
 * 服务端分页:页码或关键词变更即重新请求(替代客户端切片)。
 * fetchPage(page,size,keyword) 由调用方拼 query 调 api.getPaged。
 * 用户已接受即时搜索降级:关键词经 search() 提交后才生效。
 */
export function useServerPagedRows<T>(
  fetchPage: (page: number, size: number, keyword: string) => Promise<PagedResult<T>>,
  pageSize: number,
  options?: { onError?: (message: string) => void; fallbackMessage?: string },
): ServerPagedState<T> {
  const [page, setPage] = React.useState(1);
  const [keyword, setKeyword] = React.useState("");
  const [result, setResult] = React.useState<PagedResult<T>>({ items: [], total: 0, page: 1, size: pageSize });
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<{ message: string; detail: string | null } | null>(null);
  const [tick, setTick] = React.useState(0);

  const fetchRef = React.useRef(fetchPage);
  fetchRef.current = fetchPage;
  const onErrorRef = React.useRef(options?.onError);
  onErrorRef.current = options?.onError;
  const fallback = options?.fallbackMessage ?? "读取失败";

  React.useEffect(() => {
    let cancelled = false;
    setLoading(true);
    fetchRef.current(page, pageSize, keyword)
      .then((data) => {
        if (cancelled) return;
        setResult(data);
        setError(null);
      })
      .catch((loadError: unknown) => {
        if (cancelled) return;
        const message = loadError instanceof Error ? loadError.message : fallback;
        setError({ message, detail: formatApiErrorDetail(loadError) });
        onErrorRef.current?.(message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [page, pageSize, keyword, tick, fallback]);

  const pageCount = Math.max(1, Math.ceil(result.total / pageSize));

  return {
    rows: result.items,
    total: result.total,
    page,
    pageCount,
    loading,
    error,
    keyword,
    prev: () => setPage((value) => Math.max(1, value - 1)),
    next: () => setPage((value) => Math.min(pageCount, value + 1)),
    search: (next: string) => {
      setKeyword(next);
      setPage(1);
    },
    reload: () => setTick((value) => value + 1),
  };
}
