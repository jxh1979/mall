import { BankOutlined, PlusOutlined } from "@ant-design/icons";
import { Alert, Button, Form, Input, InputNumber, Select, Tag } from "antd";
import type { FormInstance } from "antd/es/form";
import { statusColor } from "../formatters";
import type { Snapshot } from "../types";
import type { NumericFormValues } from "../stockMovementRequest";
import { SectionHeading, SimpleTable } from "../ui";

type StringFormValues = Record<string, string>;

const TEXT = {
  dot: "\u00b7",
  title: "P2 \u5e73\u9762\u56fe\u5bfc\u5165",
  notice: "P2 \u5148\u505a\u56fe\u7247\u5e95\u56fe + \u591a\u8fb9\u5f62\u5750\u6807\uff0cSVG/\u6807\u51c6 JSON \u5bfc\u5165\u53ea\u751f\u6210\u5019\u9009\uff0c\u5fc5\u987b\u4eba\u5de5\u786e\u8ba4\u540e\u624d\u5199\u5165\u6b63\u5f0f\u533a\u57df\u3002",
  createPlan: "\u521b\u5efa\u5e73\u9762\u56fe",
  importCandidates: "\u5bfc\u5165 SVG/\u6807\u51c6 JSON \u5019\u9009",
  taskAndCandidates: "\u5bfc\u5165\u4efb\u52a1\u4e0e\u5019\u9009",
  publishCandidates: "\u5019\u9009\u53d1\u5e03",
  projectId: "\u9879\u76ee ID",
  buildingCode: "\u697c\u680b\u7f16\u7801",
  floorNo: "\u697c\u5c42",
  planName: "\u5e73\u9762\u56fe\u540d\u79f0",
  imageUrl: "\u5e95\u56fe\u5730\u5740",
  imageWidth: "\u5e95\u56fe\u5bbd\u5ea6",
  imageHeight: "\u5e95\u56fe\u9ad8\u5ea6",
  version: "\u7248\u672c",
  status: "\u72b6\u6001",
  note: "\u5907\u6ce8",
  sourceFormat: "\u6765\u6e90\u683c\u5f0f",
  standardJson: "\u6807\u51c6 JSON",
  sourceName: "\u6765\u6e90\u540d\u79f0",
  importNote: "\u5bfc\u5165\u5907\u6ce8",
  sourcePayload: "\u6e90\u6587\u4ef6\u5185\u5bb9",
  createImportTask: "\u521b\u5efa\u5bfc\u5165\u4efb\u52a1",
  floorPlan: "\u5e73\u9762\u56fe",
  importTask: "\u5bfc\u5165\u4efb\u52a1",
  loadCandidates: "\u52a0\u8f7d\u5019\u9009",
  publishTitle: "\u53d1\u5e03\u5e73\u9762\u56fe\u5bfc\u5165\u5019\u9009",
  publishConfirm: "\u53d1\u5e03\u540e\u5019\u9009\u4f1a\u5199\u5165\u6b63\u5f0f\u5e73\u9762\u56fe\u533a\u57df\uff0c\u8bf7\u786e\u8ba4\u5df2\u5b8c\u6210\u4e1a\u52a1\u590d\u6838\u3002",
  candidateIds: "\u5019\u9009 ID",
  publishNote: "\u53d1\u5e03\u5907\u6ce8",
  publishButton: "\u53d1\u5e03\u5019\u9009",
  officialShapes: "\u6b63\u5f0f\u533a\u57df",
  name: "\u540d\u79f0",
  size: "\u5c3a\u5bf8",
  type: "\u7c7b\u578b",
  binding: "\u7ed1\u5b9a",
  pointCount: "\u70b9\u6570",
  file: "\u6587\u4ef6",
  format: "\u683c\u5f0f",
  candidateCount: "\u5019\u9009",
  errorCount: "\u9519\u8bef",
  action: "\u64cd\u4f5c",
  detail: "\u660e\u7ec6",
  candidateDetail: "\u5019\u9009\u660e\u7ec6",
  importErrors: "\u5bfc\u5165\u9519\u8bef",
  lineNo: "\u884c\u53f7",
  field: "\u5b57\u6bb5",
  errorCode: "\u9519\u8bef\u7801",
  description: "\u8bf4\u660e",
  noTask: "\u672a\u9009\u62e9\u5bfc\u5165\u4efb\u52a1",
  noPlan: "\u672a\u9009\u62e9\u5e73\u9762\u56fe",
};

export type FloorPlanPageProps = {
  snapshotValue: Snapshot;
  selectedFloorPlanId: string;
  selectedFloorPlanImportId: string;
  setSelectedFloorPlanId: (value: string) => void;
  setSelectedFloorPlanImportId: (value: string) => void;
  floorPlanForm: FormInstance;
  floorPlanImportForm: FormInstance;
  floorPlanPublishForm: FormInstance;
  handleFloorPlanCreate: (values: NumericFormValues) => void | Promise<void>;
  handleFloorPlanImport: (values: StringFormValues) => void | Promise<void>;
  handleLoadFloorPlanImportDetails: (floorPlanId: string, importId: string) => void | Promise<void>;
  handlePublishFloorPlanImport: (values: StringFormValues) => void | Promise<void>;
  confirmHighImpactAction: (title: string, content: string, action: () => Promise<void>) => void;
};

function floorPlanOptions(snapshotValue: Snapshot) {
  return snapshotValue.floorPlans.map((item) => ({
    value: item.id,
    label: `${item.planName} ${TEXT.dot} ${item.floorNo} ${TEXT.dot} ${item.versionNo}`,
  }));
}

export function FloorPlanPage({
  snapshotValue,
  selectedFloorPlanId,
  selectedFloorPlanImportId,
  setSelectedFloorPlanId,
  setSelectedFloorPlanImportId,
  floorPlanForm,
  floorPlanImportForm,
  floorPlanPublishForm,
  handleFloorPlanCreate,
  handleFloorPlanImport,
  handleLoadFloorPlanImportDetails,
  handlePublishFloorPlanImport,
  confirmHighImpactAction,
}: FloorPlanPageProps) {
  const selectedPlan = snapshotValue.floorPlans.find((item) => item.id === selectedFloorPlanId) ?? snapshotValue.floorPlans[0];
  const selectedImport = snapshotValue.floorPlanImports.find((item) => item.id === selectedFloorPlanImportId) ?? snapshotValue.floorPlanImports[0];
  const pendingCandidates = snapshotValue.floorPlanImportDetails?.candidates.filter((item) => item.status === "pending") ?? [];
  const defaultCandidateIds = pendingCandidates.map((item) => item.id).join(",");

  return (
    <section className="section-block">
      <SectionHeading icon={<BankOutlined />} title={TEXT.title} extra={<Tag color="blue">floor_plan.import.manage</Tag>} />
      <Alert className="status-alert" type="info" showIcon message={TEXT.notice} />
      <div className="action-grid two">
        <div className="action-panel">
          <h2>{TEXT.createPlan}</h2>
          <Form form={floorPlanForm} layout="vertical" onFinish={handleFloorPlanCreate} initialValues={{ planName: "1F Demo", floorNo: "1F", imageUrl: "/assets/floor-plan-1f.png", imageWidth: 1200, imageHeight: 800, versionNo: "v1", status: "active" }}>
            <div className="form-grid two">
              <Form.Item name="projectId" label={TEXT.projectId}><Input /></Form.Item>
              <Form.Item name="buildingCode" label={TEXT.buildingCode}><Input /></Form.Item>
              <Form.Item name="floorNo" label={TEXT.floorNo} rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="planName" label={TEXT.planName} rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="imageUrl" label={TEXT.imageUrl} rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="imageWidth" label={TEXT.imageWidth} rules={[{ required: true }]}><InputNumber min={1} className="full" /></Form.Item>
              <Form.Item name="imageHeight" label={TEXT.imageHeight} rules={[{ required: true }]}><InputNumber min={1} className="full" /></Form.Item>
              <Form.Item name="versionNo" label={TEXT.version}><Input /></Form.Item>
              <Form.Item name="status" label={TEXT.status}><Select options={["active", "inactive", "archived"].map((value) => ({ value, label: value }))} /></Form.Item>
              <Form.Item name="operatorNote" label={TEXT.note}><Input /></Form.Item>
            </div>
            <Button type="primary" icon={<PlusOutlined />} htmlType="submit">{TEXT.createPlan}</Button>
          </Form>
        </div>
        <div className="action-panel">
          <h2>{TEXT.importCandidates}</h2>
          <Form form={floorPlanImportForm} layout="vertical" onFinish={handleFloorPlanImport} initialValues={{ sourceFormat: "standard_json", sourceName: "floor-plan-standard.json", sourcePayload: JSON.stringify({ shapes: [{ label: "A101", shape_type: "shop", ref_type: "custom", polygon_points: [{ x: 0.1, y: 0.1 }, { x: 0.3, y: 0.1 }, { x: 0.3, y: 0.3 }] }] }) }}>
            <div className="form-grid two">
              <Form.Item name="floorPlanId" label={TEXT.floorPlan} rules={[{ required: !selectedPlan }]}><Select allowClear value={selectedFloorPlanId || undefined} onChange={(value) => { setSelectedFloorPlanId(value || ""); setSelectedFloorPlanImportId(""); }} options={floorPlanOptions(snapshotValue)} /></Form.Item>
              <Form.Item name="sourceFormat" label={TEXT.sourceFormat}><Select options={[{ value: "standard_json", label: TEXT.standardJson }, { value: "svg", label: "SVG" }]} /></Form.Item>
              <Form.Item name="sourceName" label={TEXT.sourceName} rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="operatorNote" label={TEXT.importNote}><Input /></Form.Item>
            </div>
            <Form.Item name="sourcePayload" label={TEXT.sourcePayload} rules={[{ required: true }]}><Input.TextArea rows={8} /></Form.Item>
            <Button type="primary" htmlType="submit">{TEXT.createImportTask}</Button>
          </Form>
        </div>
        <div className="action-panel">
          <h2>{TEXT.taskAndCandidates}</h2>
          <Form layout="vertical" initialValues={{ floorPlanId: selectedPlan?.id, importId: selectedImport?.id }}>
            <div className="form-grid two">
              <Form.Item label={TEXT.floorPlan}><Select value={selectedPlan?.id} onChange={(value) => { setSelectedFloorPlanId(value); setSelectedFloorPlanImportId(""); }} options={floorPlanOptions(snapshotValue)} /></Form.Item>
              <Form.Item label={TEXT.importTask}><Select value={selectedImport?.id} onChange={(value) => setSelectedFloorPlanImportId(value || "")} options={snapshotValue.floorPlanImports.map((item) => ({ value: item.id, label: `${item.sourceName} ${TEXT.dot} ${item.status} ${TEXT.dot} ${item.candidateCount}/${item.errorCount}` }))} /></Form.Item>
            </div>
            <Button disabled={!selectedPlan || !selectedImport} onClick={() => selectedPlan && selectedImport ? void handleLoadFloorPlanImportDetails(selectedPlan.id, selectedImport.id) : undefined}>{TEXT.loadCandidates}</Button>
          </Form>
        </div>
        <div className="action-panel">
          <h2>{TEXT.publishCandidates}</h2>
          <Form key={selectedImport?.id || "empty-import"} form={floorPlanPublishForm} layout="vertical" onFinish={(values) => confirmHighImpactAction(TEXT.publishTitle, TEXT.publishConfirm, () => Promise.resolve(handlePublishFloorPlanImport(values as StringFormValues)))} initialValues={{ floorPlanId: selectedPlan?.id, importId: selectedImport?.id, candidateIds: defaultCandidateIds }}>
            <Form.Item name="floorPlanId" label={TEXT.floorPlan} rules={[{ required: true }]}><Select options={floorPlanOptions(snapshotValue)} /></Form.Item>
            <Form.Item name="importId" label={TEXT.importTask} rules={[{ required: true }]}><Select options={snapshotValue.floorPlanImports.map((item) => ({ value: item.id, label: `${item.sourceName} ${TEXT.dot} ${item.status}` }))} /></Form.Item>
            <Form.Item name="candidateIds" label={TEXT.candidateIds} rules={[{ required: true }]}><Input placeholder="fpic_xxx,fpic_yyy" /></Form.Item>
            <Form.Item name="operatorNote" label={TEXT.publishNote}><Input /></Form.Item>
            <Button type="primary" danger disabled={pendingCandidates.length === 0} htmlType="submit">{TEXT.publishButton}</Button>
          </Form>
        </div>
      </div>
      <div className="table-grid two">
        <SimpleTable title={TEXT.floorPlan} data={snapshotValue.floorPlans.slice(0, 12)} columns={[
          { title: TEXT.name, dataIndex: "planName" },
          { title: TEXT.floorNo, dataIndex: "floorNo" },
          { title: TEXT.version, dataIndex: "versionNo" },
          { title: TEXT.size, key: "size", render: (_, row) => row.imageWidth + " x " + row.imageHeight },
          { title: TEXT.status, dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title={TEXT.officialShapes} data={snapshotValue.floorPlanShapes.slice(0, 12)} columns={[
          { title: TEXT.name, dataIndex: "label" },
          { title: TEXT.type, dataIndex: "shapeType" },
          { title: TEXT.binding, key: "ref", render: (_, row) => `${row.refType} ${TEXT.dot} ${row.refId || "-"}` },
          { title: TEXT.pointCount, key: "points", render: (_, row) => row.polygonPoints.length },
          { title: TEXT.status, dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title={TEXT.importTask} data={snapshotValue.floorPlanImports.slice(0, 12)} columns={[
          { title: TEXT.file, dataIndex: "sourceName" },
          { title: TEXT.format, dataIndex: "sourceFormat" },
          { title: TEXT.candidateCount, dataIndex: "candidateCount" },
          { title: TEXT.errorCount, dataIndex: "errorCount" },
          { title: TEXT.status, dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          { title: TEXT.action, key: "actions", render: (_, row) => <Button size="small" onClick={() => void handleLoadFloorPlanImportDetails(row.floorPlanId, row.id)}>{TEXT.detail}</Button> },
        ]} />
        <SimpleTable title={TEXT.candidateDetail} data={snapshotValue.floorPlanImportDetails?.candidates.slice(0, 12) ?? []} columns={[
          { title: TEXT.candidateIds, dataIndex: "id" },
          { title: TEXT.name, dataIndex: "label" },
          { title: TEXT.type, dataIndex: "shapeType" },
          { title: TEXT.pointCount, key: "points", render: (_, row) => row.polygonPoints.length },
          { title: TEXT.status, dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title={TEXT.importErrors} data={snapshotValue.floorPlanImportDetails?.errors.slice(0, 12) ?? []} columns={[
          { title: TEXT.lineNo, dataIndex: "lineNo" },
          { title: TEXT.field, dataIndex: "fieldName" },
          { title: TEXT.errorCode, dataIndex: "errorCode" },
          { title: TEXT.description, dataIndex: "errorMessage" },
        ]} />
      </div>
    </section>
  );
}
