import React from "react";
import type { AdminApiClient, MerchantAdmissionRow } from "../types";

/**
 * 酒吧门店选择器（V267 纠正）：酒吧门店＝业态为「酒吧」的商户，通过招商/商户准入正路引进，不再是独立门店主数据。
 * 数据源改为 /api/v1/platform/merchants，客户端过滤 merchantType==="bar"；值＝商户 id（即 bar_id），显示 merchantName。
 * testid 沿用原字段 testid 保持 e2e/门禁兼容。
 * autoSelectFirst：加载后若当前值为空，自动选中第一个酒吧商户（用于此前默认写死 bar-main 的场景）。
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
  const [bars, setBars] = React.useState<MerchantAdmissionRow[]>([]);

  React.useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const items = await api.get<MerchantAdmissionRow[]>("/api/v1/platform/merchants");
        if (cancelled) return;
        const list = (Array.isArray(items) ? items : []).filter((m) => m.merchantType === "bar");
        setBars(list);
        const first = list[0];
        if (autoSelectFirst && !value && first) {
          onChange(first.id);
        }
      } catch (e) {
        if (!cancelled) {
          setBars([]);
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
      {bars.length === 0 && (
        <option value="" disabled>请先在招商/商户准入把某商户业态设为酒吧</option>
      )}
      {bars.map((m) => (
        <option key={m.id} value={m.id}>{m.merchantName ?? m.id}</option>
      ))}
    </select>
  );
}
