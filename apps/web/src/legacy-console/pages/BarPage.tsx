import { CoffeeOutlined } from "@ant-design/icons";
import { Alert, Button, Form, Input, InputNumber, Select, Tag } from "antd";
import type { FormInstance } from "antd/es/form";
import { formatMoney, formatTime, statusColor } from "../formatters";
import type { Snapshot } from "../types";
import { SectionHeading, SimpleTable } from "../ui";

type BarFormValues = Record<string, string | number>;

export type BarPageProps = {
  snapshotValue: Snapshot;
  barFlowForm: FormInstance;
  inventoryForm: FormInstance;
  inventoryItemForm: FormInstance;
  stockMovementForm: FormInstance;
  savedBottleForm: FormInstance;
  savedBottlePickupForm: FormInstance;
  handleBarFlow: (values: BarFormValues) => void | Promise<void>;
  handleInventoryFlow: (values: BarFormValues) => void | Promise<void>;
  handleInventoryItemCreate: (values: BarFormValues) => void | Promise<void>;
  handleStockMovement: (values: BarFormValues) => void | Promise<void>;
  handleSavedBottleCreate: (values: BarFormValues) => void | Promise<void>;
  handleSavedBottlePickup: (values: BarFormValues) => void | Promise<void>;
};

export function BarPage({
  snapshotValue,
  barFlowForm,
  inventoryForm,
  inventoryItemForm,
  stockMovementForm,
  savedBottleForm,
  savedBottlePickupForm,
  handleBarFlow,
  handleInventoryFlow,
  handleInventoryItemCreate,
  handleStockMovement,
  handleSavedBottleCreate,
  handleSavedBottlePickup,
}: BarPageProps) {
  return (
    <section className="section-block">
      <SectionHeading icon={<CoffeeOutlined />} title="酒吧开台、点单、结账" />
      <div className="action-grid two">
        <div className="action-panel">
          <h2>一键完成酒吧闭环</h2>
          <Alert className="status-alert" type="warning" showIcon message="结账支付会改变资金，须在管理后台/现场端执行。提交将跳转管理后台。" />
          <Form form={barFlowForm} data-testid="bar-flow-form" layout="vertical" onFinish={handleBarFlow} initialValues={{ quantity: 2, priceYuan: 30, minSpendYuan: 100, capacity: 4 }}>
            <div className="form-grid two">
              <Form.Item name="tableId" label="已有桌台"><Select allowClear options={snapshotValue.barTables.map((item) => ({ value: item.id, label: `${item.tableName} · ${item.status}` }))} /></Form.Item>
              <Form.Item name="tableName" label="新桌台名"><Input placeholder="不选已有桌台时创建" /></Form.Item>
              <Form.Item name="capacity" label="人数"><InputNumber min={1} className="full" /></Form.Item>
              <Form.Item name="minSpendYuan" label="低消（元）"><InputNumber min={0} className="full" /></Form.Item>
              <Form.Item name="productId" label="已有商品"><Select allowClear options={snapshotValue.barProducts.map((item) => ({ value: item.id, label: `${item.productName} · ${formatMoney(item.priceAmount)}` }))} /></Form.Item>
              <Form.Item name="productName" label="新商品名"><Input placeholder="不选已有商品时创建" /></Form.Item>
              <Form.Item name="priceYuan" label="单价（元）"><InputNumber min={0} className="full" /></Form.Item>
              <Form.Item name="quantity" label="数量"><InputNumber min={1} className="full" /></Form.Item>
            </div>
            <Button type="primary" htmlType="submit">前往管理后台结账</Button>
          </Form>
        </div>
        <div className="action-panel">
          <h2>登记库存流水</h2>
          <Form form={inventoryForm} layout="vertical" onFinish={handleInventoryFlow} initialValues={{ flowType: "inbound", quantity: 10, reason: "P0 页面入库" }}>
            <div className="form-grid two">
              <Form.Item name="productId" label="商品"><Select allowClear options={snapshotValue.barProducts.map((item) => ({ value: item.id, label: item.productName }))} /></Form.Item>
              <Form.Item name="flowType" label="类型"><Select options={["inbound", "consume", "loss", "gift"].map((value) => ({ value, label: value }))} /></Form.Item>
              <Form.Item name="quantity" label="数量"><InputNumber className="full" /></Form.Item>
              <Form.Item name="reason" label="原因"><Input /></Form.Item>
            </div>
            <Button type="primary" htmlType="submit">登记库存</Button>
          </Form>
        </div>
      </div>
      <div className="action-grid two">
        <div className="action-panel">
          <h2>P1 库存物料</h2>
          <Form form={inventoryItemForm} layout="vertical" onFinish={handleInventoryItemCreate} initialValues={{ category: "drink", unit: "bottle", minQuantity: 0 }}>
            <div className="form-grid two">
              <Form.Item name="merchantId" label="商户"><Select allowClear options={snapshotValue.merchantsRaw.map((item) => ({ value: item.id, label: item.displayName }))} /></Form.Item>
              <Form.Item name="venueId" label="场馆"><Select allowClear options={snapshotValue.venues.map((item) => ({ value: item.id, label: item.name }))} /></Form.Item>
              <Form.Item name="itemName" label="物料名称" rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="category" label="分类"><Select options={["drink", "food", "consumable"].map((value) => ({ value, label: value }))} /></Form.Item>
              <Form.Item name="unit" label="单位"><Select options={["bottle", "box", "piece", "ml", "kg"].map((value) => ({ value, label: value }))} /></Form.Item>
              <Form.Item name="minQuantity" label="最低库存"><InputNumber min={0} className="full" /></Form.Item>
            </div>
            <Button type="primary" htmlType="submit">创建物料</Button>
          </Form>
        </div>
        <div className="action-panel">
          <h2>P1 出入库、调拨、盘点</h2>
          <Form form={stockMovementForm} layout="vertical" onFinish={handleStockMovement} initialValues={{ movementType: "inbound", quantity: 10, locationType: "warehouse_stock", fromLocationType: "warehouse_stock", toLocationType: "bar_stock" }}>
            <div className="form-grid two">
              <Form.Item name="merchantId" label="商户"><Select allowClear options={snapshotValue.merchantsRaw.map((item) => ({ value: item.id, label: item.displayName }))} /></Form.Item>
              <Form.Item name="venueId" label="场馆"><Select allowClear options={snapshotValue.venues.map((item) => ({ value: item.id, label: item.name }))} /></Form.Item>
              <Form.Item name="itemId" label="物料" rules={[{ required: true }]}><Select options={snapshotValue.barInventoryItems.map((item) => ({ value: item.id, label: `${item.itemName} · ${item.unit}` }))} /></Form.Item>
              <Form.Item name="productId" label="关联商品"><Select allowClear options={snapshotValue.barProducts.map((item) => ({ value: item.id, label: item.productName }))} /></Form.Item>
              <Form.Item name="movementType" label="动作"><Select options={["inbound", "outbound", "transfer", "damage", "count"].map((value) => ({ value, label: value }))} /></Form.Item>
              <Form.Item name="quantity" label="数量/盘点数"><InputNumber min={0} className="full" /></Form.Item>
              <Form.Item name="locationType" label="库位"><Select options={["warehouse_stock", "bar_stock"].map((value) => ({ value, label: value }))} /></Form.Item>
              <Form.Item name="fromLocationType" label="调出"><Select options={["warehouse_stock", "bar_stock"].map((value) => ({ value, label: value }))} /></Form.Item>
              <Form.Item name="toLocationType" label="调入"><Select options={["bar_stock", "warehouse_stock"].map((value) => ({ value, label: value }))} /></Form.Item>
              <Form.Item name="reason" label="原因"><Input /></Form.Item>
            </div>
            <Button type="primary" htmlType="submit">提交库存动作</Button>
          </Form>
        </div>
        <div className="action-panel">
          <h2>P1 存酒登记</h2>
          <Alert className="status-alert" type="warning" showIcon message="存酒登记会改变库存，须在管理后台/现场端执行。提交将跳转管理后台。" />
          <Form form={savedBottleForm} layout="vertical" onFinish={handleSavedBottleCreate} initialValues={{ initialUnit: "percent", initialValue: 100 }}>
            <div className="form-grid two">
              <Form.Item name="merchantId" label="商户"><Select allowClear options={snapshotValue.merchantsRaw.map((item) => ({ value: item.id, label: item.displayName }))} /></Form.Item>
              <Form.Item name="venueId" label="场馆"><Select allowClear options={snapshotValue.venues.map((item) => ({ value: item.id, label: item.name }))} /></Form.Item>
              <Form.Item name="productId" label="酒水商品"><Select allowClear options={snapshotValue.barProducts.map((item) => ({ value: item.id, label: item.productName }))} /></Form.Item>
              <Form.Item name="customerName" label="客户" rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="customerPhone" label="手机号"><Input /></Form.Item>
              <Form.Item name="storageLocation" label="存放位置"><Input /></Form.Item>
              <Form.Item name="initialUnit" label="计量"><Select options={["percent", "bottle", "ml"].map((value) => ({ value, label: value }))} /></Form.Item>
              <Form.Item name="initialValue" label="初始余量"><InputNumber min={1} className="full" /></Form.Item>
              <Form.Item name="expiresAt" label="到期时间"><Input placeholder="2026-12-31T23:59:59.000Z" /></Form.Item>
            </div>
            <Button type="primary" htmlType="submit">前往管理后台登记</Button>
          </Form>
        </div>
        <div className="action-panel">
          <h2>P1 存酒取用</h2>
          <Alert className="status-alert" type="warning" showIcon message="取酒会改变库存，须在管理后台/现场端执行。提交将跳转管理后台。" />
          <Form form={savedBottlePickupForm} layout="vertical" onFinish={handleSavedBottlePickup} initialValues={{ quantity: 10 }}>
            <div className="form-grid two">
              <Form.Item name="savedBottleId" label="存酒" rules={[{ required: true }]}><Select options={snapshotValue.savedBottles.map((item) => ({ value: item.id, label: `${item.savedNo} · ${item.customerName} · ${item.remainingValue}${item.remainingUnit}` }))} /></Form.Item>
              <Form.Item name="quantity" label="取用量"><InputNumber min={1} className="full" /></Form.Item>
              <Form.Item name="reason" label="原因"><Input /></Form.Item>
            </div>
            <Button type="primary" htmlType="submit">前往管理后台取酒</Button>
          </Form>
        </div>
      </div>
      <div className="table-grid two">
        <SimpleTable title="桌台" data={snapshotValue.barTables.slice(0, 12)} columns={[
          { title: "桌台", dataIndex: "tableName" },
          { title: "低消", dataIndex: "minSpendAmount", render: formatMoney },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="订单" data={snapshotValue.barOrders.slice(0, 12)} columns={[
          { title: "订单号", dataIndex: "orderNo" },
          { title: "金额", dataIndex: "totalAmount", render: formatMoney },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="库存流水" data={snapshotValue.barInventoryFlows.slice(0, 12)} columns={[
          { title: "商品", dataIndex: ["product", "productName"] },
          { title: "类型", dataIndex: "flowType" },
          { title: "数量", dataIndex: "quantity" },
          { title: "时间", dataIndex: "createdAt", render: formatTime },
        ]} />
        <SimpleTable title="P1 物料" data={snapshotValue.barInventoryItems.slice(0, 12)} columns={[
          { title: "物料", dataIndex: "itemName" },
          { title: "分类", dataIndex: "category" },
          { title: "单位", dataIndex: "unit" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="P1 库存余额" data={snapshotValue.barStockBalances.slice(0, 12)} columns={[
          { title: "物料", dataIndex: ["item", "itemName"] },
          { title: "库位", dataIndex: "locationType" },
          { title: "数量", dataIndex: "quantity" },
        ]} />
        <SimpleTable title="P1 库存动作" data={snapshotValue.barStockMovements.slice(0, 12)} columns={[
          { title: "物料", dataIndex: ["item", "itemName"] },
          { title: "动作", dataIndex: "movementType" },
          { title: "数量", dataIndex: "quantity" },
          { title: "时间", dataIndex: "createdAt", render: formatTime },
        ]} />
        <SimpleTable title="P1 存酒" data={snapshotValue.savedBottles.slice(0, 12)} columns={[
          { title: "编号", dataIndex: "savedNo" },
          { title: "客户", dataIndex: "customerName" },
          { title: "余量", key: "remaining", render: (_, row) => `${row.remainingValue}${row.remainingUnit}` },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="P1 出品票" data={snapshotValue.barProductionTickets.slice(0, 12)} columns={[
          { title: "出品号", dataIndex: "ticketNo" },
          { title: "部门", dataIndex: "department" },
          { title: "数量", dataIndex: "quantity" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="P1 酒吧预订" data={snapshotValue.barReservations.slice(0, 12)} columns={[
          { title: "预订号", dataIndex: "reservationNo" },
          { title: "客户", dataIndex: "customerName" },
          { title: "人数", dataIndex: "partySize" },
          { title: "订金", dataIndex: "depositAmount", render: formatMoney },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
      </div>
    </section>
  );
}
