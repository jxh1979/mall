import React from "react";

export interface AdminResourceState {
  loading: boolean;
  error: string | null;
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
  const [error, setError] = React.useState<string | null>(null);

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
      setError(message);
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
