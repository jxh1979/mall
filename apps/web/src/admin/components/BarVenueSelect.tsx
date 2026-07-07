import React from "react";
import type { AdminApiClient, BarVenueRow } from "../types";

/**
 * 酒吧门店选择器（V263）：从 /api/v1/bar/venues 拉取已建门店，值=门店 id，显示 barName。
 * 替代此前各处手填「酒吧 ID」的输入框；testid 沿用原字段 testid 保持 e2e/门禁兼容。
 * autoSelectFirst：加载后若当前值为空，自动选中第一个门店（用于此前默认写死 bar-main 的场景）。
 */
export function BarVenueSelect({
  api,
  value,
  onChange,
  testId,
  autoSelectFirst = false,
  setGlobalError,
}: {
  api: AdminApiClient;
  value: string;
  onChange: (value: string) => void;
  testId: string;
  autoSelectFirst?: boolean;
  setGlobalError?: (value: string | null) => void;
}) {
  const [venues, setVenues] = React.useState<BarVenueRow[]>([]);

  React.useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const items = await api.get<BarVenueRow[]>("/api/v1/bar/venues");
        if (cancelled) return;
        const list = Array.isArray(items) ? items : [];
        setVenues(list);
        const first = list[0];
        if (autoSelectFirst && !value && first) {
          onChange(first.id);
        }
      } catch (e) {
        if (!cancelled) {
          setVenues([]);
          setGlobalError?.(e instanceof Error ? e.message : "酒吧门店读取失败");
        }
      }
    })();
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <select data-testid={testId} value={value} onChange={(e) => onChange(e.target.value)}>
      <option value="">请选择酒吧</option>
      {venues.map((v) => (
        <option key={v.id} value={v.id}>{v.barName ?? v.id}</option>
      ))}
    </select>
  );
}
