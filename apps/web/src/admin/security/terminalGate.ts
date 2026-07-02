// 3.19 二期 · 前端菜单 gating：按终端开关矩阵隐藏被关闭的菜单模块。
// 数据源 = GET /api/v1/terminals/capability-switches/effective（当前终端已关闭的开关行）。
// 拉取失败 = 兜底关（业务 HTML「拉取失败兜底关闭」范式）：除自救组外全部隐藏，
// 保留 overview（落脚页）与 integration（含终端开关矩阵 ADM-TER-001，运营可自救重开）。

export type DisabledSwitchRow = {
  terminalType: string;
  capabilityCode: string;
  scopeType: string;
  scopeId: string;
};

/** 兜底关时仍保留的自救菜单组：总览 + 集成与终端（矩阵页所在组）。 */
export const RESCUE_GROUP_KEYS = new Set(["overview", "integration"]);

/**
 * 计算应隐藏的菜单组集合。
 * rows = 已关闭开关行（正常拉取）；null = 拉取失败 → 兜底关。
 * 只认 terminal.enabled + module scope 的行（scope_id=菜单组 key，见 V198）。
 */
export function hiddenGroupKeys(rows: DisabledSwitchRow[] | null, allGroupKeys: string[]): Set<string> {
  if (rows === null) {
    return new Set(allGroupKeys.filter((key) => !RESCUE_GROUP_KEYS.has(key)));
  }
  const hidden = new Set<string>();
  for (const row of rows) {
    if (row.capabilityCode === "terminal.enabled" && row.scopeType === "module") {
      hidden.add(row.scopeId);
    }
  }
  return hidden;
}
