// 文件版本：V0.01
import { afterEach, expect, it, vi } from "vitest";
import { AdminApi } from "./adminClient";

afterEach(() => vi.unstubAllGlobals());
it("合同单价双向保留商品编号，下划线与大小写不是字段命名", async () => {
  const fetchMock = vi.fn().mockImplementation(async () => new Response(JSON.stringify({ code: "OK", data: { terms: { price_basis: "contract_unit_price", unit_prices: { item_A_01: 123, SKU_02: 456, camelItem: 789 } } } }), { headers: { "Content-Type": "application/json" } }));
  vi.stubGlobal("fetch", fetchMock);
  const api = new AdminApi();
  await expect(api.get("/api/v1/inventory/supply-contracts/sc/terms")).resolves.toEqual({ terms: { priceBasis: "contract_unit_price", unitPrices: { item_A_01: 123, SKU_02: 456, camelItem: 789 } } });
  await api.post("/api/v1/inventory/supply-contracts/sc/terms", { idempotencyKey: "dictionary-test", terms: { priceBasis: "contract_unit_price", unitPrices: { item_A_01: 123, SKU_02: 456, camelItem: 789 } } });
  expect(JSON.parse(String((fetchMock.mock.calls[1]![1] as RequestInit).body)).terms).toEqual({ price_basis: "contract_unit_price", unit_prices: { item_A_01: 123, SKU_02: 456, camelItem: 789 } });
});
