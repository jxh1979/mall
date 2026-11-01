// 文件版本：V0.01
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { InventoryWarehousePanel } from "./InventoryWarehousePanel";
import type { AdminApiClient } from "../types";

// 第十八批 L 段 105-R1（BSM:1162「管理库房和库位，包括总库、酒吧库、吧台库存、消耗品库、礼品库和存酒区。」；ENUM:160 库房分类六类；
// PGM:948「……库房……都要看商户归属……」）：库存台账「库房」区块列出库房（显示库房编号）并新建。
const WAREHOUSE = { id: "wh_1", projectId: "p1", merchantId: "m1", warehouseType: "bar_counter", name: "一号吧台", status: "active" };

function setup() {
  const api = {
    get: vi.fn(async (path: string) => {
      if (path === "/api/v1/inventory/warehouses") return [WAREHOUSE];
      if (path === "/api/v1/platform/projects") return [{ id: "p1", name: "长沙综合体", status: "active" }, { id: "p0", name: "停用项目", status: "inactive" }];
      if (path === "/api/v1/platform/merchants") return [{ id: "m1", merchantName: "一号酒吧" }];
      return [];
    }),
    post: vi.fn().mockResolvedValue({ id: "warehouse_new" }),
  } as unknown as AdminApiClient;
  const setActionResult = vi.fn();
  render(<InventoryWarehousePanel api={api} setActionResult={setActionResult} />);
  return { api, setActionResult };
}

async function expand() {
  fireEvent.click(screen.getByRole("button", { name: "库房" }));
  await waitFor(() => expect(screen.getByTestId("inventory-warehouse-row-wh_1").textContent).toContain("一号吧台"));
}

describe("库存台账「库房」区块（第十八批 L 段 105-R1）", () => {
  it("展开后列出库房编号、名称、中文分类、所属项目与归属商户名", async () => {
    setup();
    expect(screen.queryByTestId("inventory-warehouse-row-wh_1")).toBeNull();
    await expand();
    const row = screen.getByTestId("inventory-warehouse-row-wh_1").textContent ?? "";
    expect(row).toContain("wh_1");
    expect(row).toContain("吧台库存");
    expect(row).toContain("长沙综合体");
    expect(row).toContain("一号酒吧");
    expect(row).not.toContain("bar_counter");
  });

  it("新建：唯一在用项目默认选中，归属选商户、分类选吧台库存，提交 POST /api/v1/inventory/warehouses 蛇形键", async () => {
    const { api, setActionResult } = setup();
    await expand();
    fireEvent.click(screen.getByTestId("inventory-warehouse-create-open"));
    await waitFor(() => expect((screen.getByTestId("inventory-warehouse-create-project") as HTMLSelectElement).value).toBe("p1"));
    fireEvent.change(screen.getByTestId("inventory-warehouse-create-merchant"), { target: { value: "m1" } });
    fireEvent.change(screen.getByTestId("inventory-warehouse-create-type"), { target: { value: "bar_counter" } });
    fireEvent.change(screen.getByTestId("inventory-warehouse-create-name"), { target: { value: " 二号吧台 " } });
    fireEvent.click(screen.getByTestId("inventory-warehouse-create-guided-next"));
    fireEvent.click(screen.getByTestId("inventory-warehouse-create-submit"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/inventory/warehouses", expect.objectContaining({
      project_id: "p1", merchant_id: "m1", warehouse_type: "bar_counter", name: "二号吧台", idempotency_key: expect.any(String),
    })));
    expect(setActionResult).toHaveBeenCalledWith(expect.objectContaining({ action: "inventory_warehouse_created" }));
  });

  it("归属选「平台（不归属商户）」时 merchant_id 为 null", async () => {
    const { api } = setup();
    await expand();
    fireEvent.click(screen.getByTestId("inventory-warehouse-create-open"));
    fireEvent.change(screen.getByTestId("inventory-warehouse-create-merchant"), { target: { value: "" } });
    fireEvent.change(screen.getByTestId("inventory-warehouse-create-type"), { target: { value: "main" } });
    fireEvent.change(screen.getByTestId("inventory-warehouse-create-name"), { target: { value: "平台总库" } });
    fireEvent.click(screen.getByTestId("inventory-warehouse-create-guided-next"));
    fireEvent.click(screen.getByTestId("inventory-warehouse-create-submit"));
    await waitFor(() => expect(api.post).toHaveBeenCalledWith("/api/v1/inventory/warehouses", expect.objectContaining({
      project_id: "p1", merchant_id: null, warehouse_type: "main", name: "平台总库",
    })));
  });

  it("名称为空当步提示，不调用新建", async () => {
    const { api } = setup();
    await expand();
    fireEvent.click(screen.getByTestId("inventory-warehouse-create-open"));
    fireEvent.click(screen.getByTestId("inventory-warehouse-create-guided-next"));
    await waitFor(() => expect(screen.getByTestId("inventory-warehouse-create-guided-error").textContent).toContain("库房名称"));
    expect(screen.queryByTestId("inventory-warehouse-create-submit")).toBeNull();
    expect(api.post).not.toHaveBeenCalled();
  });
});
