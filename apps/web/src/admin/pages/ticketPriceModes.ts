// 文件版本：V0.01
// 第十八批 AW 段 338-R3（BSM:782「会员价、渠道价、动态价都在对应基础价上按打折/减免/加价/固定额派生」；PGM:287「界面全中文（所有终端零英文）」）：
// 派生价定价方式与「折扣/价格」的中文显示，票务策略页派生价表、行明细与规则版本表共用（键同后端 TicketCatalogService.PRICE_MODES）。
type PriceModeRow = { priceMode: string; discountRate: number | string | null; weekdayPriceCent: number | null; adjustmentAmountCent?: number | null };

const yuan = (cent: number | null | undefined) => (cent == null ? "—" : (cent / 100).toFixed(2));

export function priceModeLabel(priceMode: string): string {
  return priceMode === "fixed_price" ? "固定价" : priceMode === "amount_delta" ? "按基础价加减金额" : "按基础价折扣";
}

/** 固定价写「¥x」，按基础价加减金额写「减 ¥x」「加 ¥x」，按基础价折扣写「x%」。 */
export function priceModeValue(row: PriceModeRow): string {
  if (row.priceMode === "fixed_price") return `¥${yuan(row.weekdayPriceCent)}`;
  if (row.priceMode === "amount_delta") {
    const amount = row.adjustmentAmountCent ?? 0;
    return `${amount < 0 ? "减" : "加"} ¥${yuan(Math.abs(amount))}`;
  }
  return row.discountRate != null ? `${Number(row.discountRate)}%` : "—";
}
