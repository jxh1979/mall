import { describe, expect, it } from "vitest";
import {
  averageNumber,
  formatConfidence,
  formatMetricValue,
  formatMoney,
  formatRatio,
  latestBusinessDate,
  metricDisplayName,
  statusColor,
  summarizeRobotMetrics,
  toCents,
} from "./formatters";

describe("legacy console formatters", () => {
  it("formats money, cents and defensive ratios", () => {
    expect(formatMoney(12345)).toBe("\u00a5123.45");
    expect(toCents(12.34)).toBe(1234);
    expect(formatRatio(1, 4)).toBe("25.00%");
    expect(formatRatio(1, 0)).toBe("0.00%");
  });

  it("summarizes metrics without producing NaN for empty data", () => {
    expect(averageNumber([1, null, undefined, 3])).toBe(2);
    expect(formatConfidence(0.91)).toBe("91%");
    expect(summarizeRobotMetrics([])).toEqual({
      taskCount: 0,
      successCount: 0,
      failureCount: 0,
      completionRate: 0,
      abnormalRate: 0,
    });
  });

  it("keeps domain display helpers stable", () => {
    expect(formatMetricValue("revenue_amount_cents", 8800)).toBe("\u00a588.00");
    expect(latestBusinessDate([{ businessDate: "2026-06-15" }, { businessDate: "2026-06-16" }])).toBe("2026-06-16");
    expect(metricDisplayName("ticket_count", [{ metricKey: "ticket_count", name: "ticket volume", domain: "ticketing", unit: "count", calculationNote: "sum", ownerRole: "ops", status: "active", createdAt: "", updatedAt: "" }])).toBe("ticket volume");
    expect(statusColor("failed")).toBe("red");
  });
});
