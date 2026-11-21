import React from "react";
import type { AdminApiClient, MerchantAdmissionRow } from "../types";

/**
 * 酒吧门店选择器（V267 纠正）：酒吧门店＝业态为「酒吧」的商户，通过招商/商户准入正路引进，不再是独立门店主数据。
 * 数据源改为 /api/v1/platform/merchants，客户端过滤 merchantType==="bar"；值＝商户 id（即 bar_id），显示 merchantName。
 * testid 沿用原字段 testid 保持 e2e/门禁兼容。
 * autoSelectFirst：加载后若当前值为空，自动选中第一个酒吧商户（用于此前默认写死 bar-main 的场景）。
 * 加载态修复（2026-07-07）：拉取期显「加载中…」而非「请先设为酒吧」——否则约 2.5s 拉取期误显“没有酒吧”，
 *   用户以为需先去建，实为字典正在加载。不做模块缓存：新建酒吧需即时出现（onboarding 链依赖）。
 * merchantId（第十八批 BL 段控制席并入第 415 条，BSM:1017「各处填写「所属酒吧」时从已引进的酒吧商户里下拉选择」）：当前账号挂商户时传本人商户号——
 *   不调要平台范围的商户列表（挂商户账号 403），下拉只列「本店」一项、默认选中、不可改；平台管理员不传，照旧可选全部酒吧。
 */
export function BarVenueSelect({
  api,
  value,
  onChange,
  testId,
  autoSelectFirst = false,
  setGlobalError,
  merchantId,
}: {
  api: AdminApiClient;
  value: string;
  onChange: (value: string) => void;
  testId: string;
  autoSelectFirst?: boolean;
  setGlobalError?: (value: string | null) => void;
  merchantId?: string | null;
}) {
  const own = merchantId?.trim() ?? "";
  const [bars, setBars] = React.useState<MerchantAdmissionRow[]>([]);
  const [loading, setLoading] = React.useState(!own);

  React.useEffect(() => {
    if (own) return undefined;
    let cancelled = false;
    void (async () => {
      try {
        const items = await api.get<MerchantAdmissionRow[]>("/api/v1/platform/merchants");
        if (cancelled) return;
        const list = (Array.isArray(items) ? items : []).filter((m) => m.merchantType === "bar");
        setBars(list);
        setLoading(false);
        const first = list[0];
        if (autoSelectFirst && !value && first) {
          onChange(first.id);
        }
      } catch (e) {
        if (!cancelled) {
          setBars([]);
          setLoading(false);
          setGlobalError?.(e instanceof Error ? e.message : "酒吧门店读取失败");
        }
      }
    })();
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 挂商户的账号：所属酒吧恒为本店（表单清空后也补回）。
  React.useEffect(() => {
    if (own && value !== own) onChange(own);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [own, value]);

  if (own) {
    return (
      <select data-testid={testId} value={own} disabled onChange={() => undefined}>
        <option value={own}>本店</option>
      </select>
    );
  }

  return (
    <select data-testid={testId} value={value} onChange={(e) => onChange(e.target.value)}>
      <option value="">请选择酒吧</option>
      {loading && (
        <option value="" disabled>加载中…</option>
      )}
      {!loading && bars.length === 0 && (
        <option value="" disabled>请先在招商/商户准入把某商户业态设为酒吧</option>
      )}
      {bars.map((m) => (
        <option key={m.id} value={m.id}>{m.merchantName ?? m.id}</option>
      ))}
    </select>
  );
}
