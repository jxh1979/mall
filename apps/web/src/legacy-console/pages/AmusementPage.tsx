import { RocketOutlined } from "@ant-design/icons";
import { Alert, Button, Form, Input, InputNumber, Select, Tag } from "antd";
import type { FormInstance } from "antd/es/form";
import { formatTime, statusColor } from "../formatters";
import type { Snapshot } from "../types";
import { SectionHeading, SimpleTable } from "../ui";

type AmusementFormValues = Record<string, string | number>;
type AmusementStringValues = Record<string, string>;

export type AmusementPageProps = {
  snapshotValue: Snapshot;
  amusementFlowForm: FormInstance;
  amusementVerifyForm: FormInstance;
  amusementGroupBookingForm: FormInstance;
  handleAmusementFlow: (values: AmusementFormValues) => void | Promise<void>;
  handleAmusementVerify: (values: AmusementStringValues) => void | Promise<void>;
  handleAmusementGroupBooking: (values: AmusementFormValues) => void | Promise<void>;
  handleApproveGroupBooking: (bookingId: string) => Promise<void>;
  handleReleaseGroupBooking: (bookingId: string) => Promise<void>;
  confirmHighImpactAction: (title: string, content: string, action: () => Promise<void>) => void;
};

export function AmusementPage({
  snapshotValue,
  amusementFlowForm,
  amusementVerifyForm,
  amusementGroupBookingForm,
  handleAmusementFlow,
  handleAmusementVerify,
  handleAmusementGroupBooking,
  handleApproveGroupBooking,
  handleReleaseGroupBooking,
  confirmHighImpactAction,
}: AmusementPageProps) {
  return (
    <section className="section-block">
      <SectionHeading icon={<RocketOutlined />} title="游乐项目、场次、放行" />
      <div className="action-grid two">
        <div className="action-panel">
          <h2>创建项目和场次</h2>
          <Form form={amusementFlowForm} data-testid="amusement-flow-form" layout="vertical" onFinish={handleAmusementFlow} initialValues={{ projectName: `P0 Web Project ${Date.now()}`, sessionName: "P0 Web Session", capacity: 20, sessionCapacity: 10 }}>
            <div className="form-grid two">
              <Form.Item name="projectName" label="项目名称" rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="capacity" label="项目容量"><InputNumber min={1} className="full" /></Form.Item>
              <Form.Item name="sessionName" label="场次名称" rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="sessionCapacity" label="场次容量"><InputNumber min={1} className="full" /></Form.Item>
            </div>
            <Button type="primary" htmlType="submit">创建项目和场次</Button>
          </Form>
        </div>
        <div className="action-panel">
          <h2>核销或人工放行</h2>
          <Alert className="status-alert" type="warning" showIcon message="核销与人工放行会改变核销结果，须在管理后台/现场端执行。提交将跳转管理后台。" />
          <Form form={amusementVerifyForm} data-testid="amusement-verify-form" layout="vertical" onFinish={handleAmusementVerify} initialValues={{ manualReason: "P0 页面人工放行" }}>
            <Form.Item name="sessionId" label="场次" rules={[{ required: true }]}>
              <Select options={snapshotValue.amusementSessions.map((item) => ({ value: item.id, label: `${item.sessionName} · ${item.usedCapacity}/${item.capacity}` }))} />
            </Form.Item>
            <Form.Item name="entitlementCode" label="权益码"><Input placeholder="为空时按人工放行" /></Form.Item>
            <Form.Item name="manualReason" label="人工原因"><Input /></Form.Item>
            <Button type="primary" htmlType="submit">前往管理后台放行</Button>
          </Form>
        </div>
      </div>
      <div className="action-grid two">
        <div className="action-panel">
          <h2>P2 团体/包场容量申请</h2>
          <Alert className="status-alert" type="info" showIcon message="审批时由后端锁定容量；前端只展示结果，不自行裁决可锁定。" />
          <Form form={amusementGroupBookingForm} data-testid="amusement-group-booking-form" layout="vertical" onFinish={handleAmusementGroupBooking} initialValues={{ bookingType: "group", partySize: 2, reservedCapacity: 2 }}>
            <div className="form-grid two">
              <Form.Item name="sessionId" label="场次" rules={[{ required: true }]}><Select options={snapshotValue.amusementSessions.map((item) => ({ value: item.id, label: `${item.sessionName} · 已用 ${item.usedCapacity} · 锁定 ${item.lockedCapacity ?? 0} / ${item.capacity}` }))} /></Form.Item>
              <Form.Item name="bookingType" label="类型"><Select options={[{ value: "group", label: "团体" }, { value: "private", label: "包场" }]} /></Form.Item>
              <Form.Item name="customerName" label="客户" rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="contactPhone" label="联系电话"><Input /></Form.Item>
              <Form.Item name="partySize" label="人数"><InputNumber min={1} className="full" /></Form.Item>
              <Form.Item name="reservedCapacity" label="锁定容量"><InputNumber min={1} className="full" /></Form.Item>
              <Form.Item name="operatorNote" label="备注"><Input /></Form.Item>
            </div>
            <Button type="primary" htmlType="submit">创建待审批申请</Button>
          </Form>
        </div>
      </div>
      <div className="table-grid two">
        <SimpleTable title="P2 团体/包场" data={snapshotValue.amusementGroupBookings.slice(0, 12)} columns={[
          { title: "客户", dataIndex: "customerName" },
          { title: "类型", dataIndex: "bookingType" },
          { title: "人数", dataIndex: "partySize" },
          { title: "锁定容量", dataIndex: "reservedCapacity" },
          { title: "状态", dataIndex: "bookingStatus", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          { title: "操作", key: "actions", render: (_, row) => <div className="button-row compact">{row.bookingStatus === "pending_approval" ? <Button size="small" onClick={() => confirmHighImpactAction("审批并锁定容量", "审批后由后端事务锁定该场次容量。", () => handleApproveGroupBooking(row.id))}>审批锁定</Button> : null}{row.bookingStatus === "approved" ? <Button size="small" data-testid={`release-group-booking-${row.id}`} onClick={() => confirmHighImpactAction("释放已锁定容量", "释放后该申请占用的场次容量会被归还。", () => handleReleaseGroupBooking(row.id))}>释放</Button> : null}</div> },
        ]} />
        <SimpleTable title="P2 场次容量池" data={snapshotValue.amusementSessions.slice(0, 12)} columns={[
          { title: "场次", dataIndex: "sessionName" },
          { title: "已用", dataIndex: "usedCapacity" },
          { title: "锁定", dataIndex: "lockedCapacity", render: (value) => value ?? 0 },
          { title: "容量", dataIndex: "capacity" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="游乐项目" data={snapshotValue.amusementProjects.slice(0, 12)} columns={[
          { title: "项目", dataIndex: "projectName" },
          { title: "容量", dataIndex: "capacity" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="游乐放行记录" data={snapshotValue.amusementVerifications.slice(0, 12)} columns={[
          { title: "编号", dataIndex: "verificationNo" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
          { title: "时间", dataIndex: "createdAt", render: formatTime },
        ]} />
        <SimpleTable title="P1 预约" data={snapshotValue.amusementReservations.slice(0, 12)} columns={[
          { title: "预约号", dataIndex: "reservationNo" },
          { title: "客户", dataIndex: "customerName" },
          { title: "人数", dataIndex: "reservedCapacity" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="P1 排队" data={snapshotValue.amusementQueueEntries.slice(0, 12)} columns={[
          { title: "排队号", dataIndex: "queueNo" },
          { title: "客户", dataIndex: "customerName" },
          { title: "人数", dataIndex: "partySize" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="P1 设备状态" data={snapshotValue.amusementDeviceStatuses.slice(0, 12)} columns={[
          { title: "设备", dataIndex: "deviceName" },
          { title: "编码", dataIndex: "deviceCode" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
        <SimpleTable title="P1 巡检与故障" data={snapshotValue.amusementFaults.slice(0, 12)} columns={[
          { title: "故障号", dataIndex: "faultNo" },
          { title: "级别", dataIndex: "faultLevel" },
          { title: "摘要", dataIndex: "summary" },
          { title: "状态", dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        ]} />
      </div>
    </section>
  );
}
