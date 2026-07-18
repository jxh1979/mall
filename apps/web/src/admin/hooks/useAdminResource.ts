import React from "react";
import { ApiError, formatApiErrorDetail } from "../../shared/apiError";

/** message=业务话术（原 err.message），detail=技术详情（path/status/request_id/原文），供折叠报障。 */
export interface AdminResourceError {
  message: string;
  detail: string | null;
}

export interface AdminResourceState {
  loading: boolean;
  error: AdminResourceError | null;
  reload: () => Promise<void>;
}

/**
 * Centralizes the loading/error boilerplate that admin pages repeat verbatim:
 * set loading, run the fetch, clear or surface the error, always stop loading,
 * and fetch once on mount. The fetcher owns its own data state (setRows/setData)
 * so this hook is invisible to the DOM and to Playwright testids.
 *
 * The fetcher and onError callbacks are read through refs so an inline closure
 * does not retrigger the mount effect on every render (avoids refetch loops),
 * while still calling the latest version.
 */
export function useAdminResource(
  fetcher: () => Promise<void>,
  options: {
    onError: (message: string) => void;
    fallbackMessage: string;
    /** Runs after loading starts, before the fetch — e.g. clear a global banner. */
    onStart?: () => void;
  },
): AdminResourceState {
  const { onError, fallbackMessage, onStart } = options;
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<AdminResourceError | null>(null);

  const fetcherRef = React.useRef(fetcher);
  fetcherRef.current = fetcher;
  const onErrorRef = React.useRef(onError);
  onErrorRef.current = onError;
  const onStartRef = React.useRef(onStart);
  onStartRef.current = onStart;

  const reload = React.useCallback(async () => {
    setLoading(true);
    onStartRef.current?.();
    try {
      await fetcherRef.current();
      setError(null);
    } catch (loadError) {
      const message = loadError instanceof Error ? loadError.message : fallbackMessage;
      setError({ message, detail: formatApiErrorDetail(loadError) });
      onErrorRef.current(message);
    } finally {
      setLoading(false);
    }
  }, [fallbackMessage]);

  React.useEffect(() => {
    void reload();
  }, [reload]);

  return { loading, error, reload };
}

/**
 * 副资源容错加载（AUD-032）：页面主资源之外的辅助数据（如外包工单页的设备台账、
 * 外包巡检页的模板）改经此包装后再进 Promise.all——副资源 403/失败不再整体 reject
 * 拖垮整页，而是降级为 fallback + 区块级提示文案；主资源仍走 useAdminResource 原有错误路径。
 */
export interface SecondaryResource<T> {
  data: T;
  /** 非 null 表示该区块已降级，UI 用它替换区块内容展示。 */
  notice: string | null;
}

export async function loadSecondary<T>(load: () => Promise<T>, fallback: T): Promise<SecondaryResource<T>> {
  try {
    return { data: (await load()) ?? fallback, notice: null };
  } catch (error) {
    const denied = error instanceof ApiError && error.detail.status === 403;
    return { data: fallback, notice: denied ? "当前账号无权查看该数据" : "该数据加载失败，可稍后重试，不影响本页其他内容" };
  }
}
