import { BankOutlined, PlusOutlined } from "@ant-design/icons";
import { Button, Form, Input, Select, Tag } from "antd";
import type { FormInstance } from "antd/es/form";
import { statusColor } from "../formatters";
import type { Snapshot } from "../types";
import { SectionHeading, SimpleTable } from "../ui";

type FoundationFormValues = Record<string, string>;

const TEXT = {
  organization: "\u7ec4\u7ec7",
  merchant: "\u5546\u6237",
  complex: "\u7efc\u5408\u4f53",
  area: "\u533a\u57df",
  point: "\u70b9\u4f4d",
  topologyTitle: "\u96c6\u56e2/\u7efc\u5408\u4f53\u62d3\u6251",
  emptyTopology: "\u6682\u65e0\u7ec4\u7ec7\u4e0e\u7efc\u5408\u4f53\u5173\u7cfb",
  pageTitle: "\u5e73\u53f0\u5730\u57fa\u64cd\u4f5c",
  createFoundation: "\u4e00\u952e\u521b\u5efa\u7ec4\u7ec7\u3001\u5546\u6237\u3001\u573a\u9986\u3001\u533a\u57df\u3001\u70b9\u4f4d",
  organizationName: "\u7ec4\u7ec7\u540d\u79f0",
  merchantName: "\u5546\u6237\u540d\u79f0",
  venueName: "\u573a\u9986\u540d\u79f0",
  areaName: "\u533a\u57df\u540d\u79f0",
  pointName: "\u70b9\u4f4d\u540d\u79f0",
  phone: "\u8054\u7cfb\u7535\u8bdd",
  createFoundationButton: "\u521b\u5efa\u57fa\u7840\u8d44\u6599",
  createUser: "\u521b\u5efa\u5458\u5de5\u8d26\u53f7",
  username: "\u8d26\u53f7",
  password: "\u5bc6\u7801",
  accountType: "\u8d26\u53f7\u7c7b\u578b",
  staff: "\u5458\u5de5",
  platformAdmin: "\u5e73\u53f0\u7ba1\u7406\u5458",
  createUserButton: "\u521b\u5efa\u8d26\u53f7",
  orgMerchantTable: "\u7ec4\u7ec7\u4e0e\u5546\u6237",
  venuePointTable: "\u573a\u9986\u4e0e\u70b9\u4f4d",
  company: "\u516c\u53f8",
  status: "\u72b6\u6001",
  code: "\u7f16\u7801",
  type: "\u7c7b\u578b",
};

export type FoundationPageProps = {
  snapshotValue: Snapshot;
  foundationForm: FormInstance;
  userForm: FormInstance;
  handleFoundationFlow: (values: FoundationFormValues) => void | Promise<void>;
  handleCreateUser: (values: FoundationFormValues) => void | Promise<void>;
};

function DataTables({ snapshotValue }: { snapshotValue: Snapshot }) {
  return (
    <div className="table-grid two">
      <SimpleTable title={TEXT.orgMerchantTable} data={snapshotValue.merchantsRaw.slice(0, 10)} columns={[
        { title: TEXT.merchant, dataIndex: "displayName" },
        { title: TEXT.company, dataIndex: "companyName" },
        { title: TEXT.status, dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
      ]} />
      <SimpleTable title={TEXT.venuePointTable} data={snapshotValue.locationPoints.slice(0, 10)} columns={[
        { title: TEXT.point, dataIndex: "name" },
        { title: TEXT.code, dataIndex: "code" },
        { title: TEXT.type, dataIndex: "pointType" },
        { title: TEXT.status, dataIndex: "status", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
      ]} />
    </div>
  );
}

function PlatformTopologyView({ snapshotValue }: { snapshotValue: Snapshot }) {
  const summaryItems = [
    { label: TEXT.organization, value: snapshotValue.organizations.length },
    { label: TEXT.merchant, value: snapshotValue.merchantsRaw.length },
    { label: TEXT.complex, value: snapshotValue.venues.length },
    { label: TEXT.area, value: snapshotValue.areas.length },
    { label: TEXT.point, value: snapshotValue.locationPoints.length },
  ];

  return (
    <div className="topology-panel" data-testid="platform-topology-view">
      <div className="panel-title panel-title-row topology-header">
        <span>{TEXT.topologyTitle}</span>
        <div className="topology-summary" data-testid="platform-topology-summary">
          {summaryItems.map((item) => <Tag key={item.label}>{`${item.label} ${item.value}`}</Tag>)}
        </div>
      </div>
      {snapshotValue.organizations.length === 0 ? (
        <div className="topology-empty">{TEXT.emptyTopology}</div>
      ) : (
        <div className="topology-tree">
          {snapshotValue.organizations.map((organization) => {
            const merchants = snapshotValue.merchantsRaw.filter((merchant) => merchant.organizationId === organization.id);
            const merchantVenueCount = merchants.reduce((sum, merchant) => sum + snapshotValue.venues.filter((venue) => venue.merchantId === merchant.id).length, 0);
            return (
              <div className="topology-node topology-node-root" data-testid={`platform-topology-node-${organization.id}`} key={organization.id}>
                <div className="topology-node-main">
                  <strong>{organization.name}</strong>
                  <small>{organization.code ?? organization.orgType}</small>
                </div>
                <div className="topology-node-metrics">
                  <span>{`${TEXT.merchant} ${merchants.length}`}</span>
                  <span>{`${TEXT.complex} ${merchantVenueCount}`}</span>
                </div>
                <div className="topology-children">
                  {merchants.map((merchant) => {
                    const venues = snapshotValue.venues.filter((venue) => venue.merchantId === merchant.id);
                    return (
                      <div className="topology-node topology-node-merchant" data-testid={`platform-topology-node-${merchant.id}`} key={merchant.id}>
                        <div className="topology-node-main">
                          <strong>{merchant.displayName}</strong>
                          <small>{merchant.companyName}</small>
                        </div>
                        <div className="topology-node-metrics">
                          <span>{`${TEXT.complex} ${venues.length}`}</span>
                          <span>{merchant.status}</span>
                        </div>
                        <div className="topology-children compact">
                          {venues.map((venue) => {
                            const areas = snapshotValue.areas.filter((area) => area.venueId === venue.id || area.id === venue.id);
                            const points = snapshotValue.locationPoints.filter((point) => areas.some((area) => point.areaId === area.id || point.id === area.id));
                            return (
                              <div className="topology-node topology-node-venue" data-testid={`platform-topology-node-${venue.id}`} key={venue.id}>
                                <div className="topology-node-main">
                                  <strong>{venue.name}</strong>
                                  <small>{venue.code ?? venue.venueType}</small>
                                </div>
                                <div className="topology-node-metrics">
                                  <span>{`${TEXT.area} ${areas.length}`}</span>
                                  <span>{`${TEXT.point} ${points.length}`}</span>
                                </div>
                                {points.length > 0 ? (
                                  <div className="topology-point-list">
                                    {points.slice(0, 4).map((point) => <Tag key={point.id}>{point.name}</Tag>)}
                                  </div>
                                ) : null}
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export function FoundationPage({ snapshotValue, foundationForm, userForm, handleFoundationFlow, handleCreateUser }: FoundationPageProps) {
  return (
    <section className="section-block">
      <SectionHeading icon={<BankOutlined />} title={TEXT.pageTitle} />
      <PlatformTopologyView snapshotValue={snapshotValue} />
      <div className="action-grid two">
        <div className="action-panel">
          <h2>{TEXT.createFoundation}</h2>
          <Form form={foundationForm} layout="vertical" onFinish={handleFoundationFlow} initialValues={{
            organizationName: "P0 Operation Org",
            merchantName: `P0 Merchant ${Date.now()}`,
            venueName: "P0 Venue",
            areaName: "P0 Area",
            pointName: "P0 Gate Point",
          }}>
            <div className="form-grid two">
              <Form.Item name="organizationName" label={TEXT.organizationName} rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="merchantName" label={TEXT.merchantName} rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="venueName" label={TEXT.venueName} rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="areaName" label={TEXT.areaName} rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="pointName" label={TEXT.pointName} rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="contactPhone" label={TEXT.phone}><Input /></Form.Item>
            </div>
            <Button type="primary" icon={<PlusOutlined />} htmlType="submit">{TEXT.createFoundationButton}</Button>
          </Form>
        </div>
        <div className="action-panel">
          <h2>{TEXT.createUser}</h2>
          <Form form={userForm} layout="vertical" onFinish={handleCreateUser} initialValues={{ accountType: "staff" }}>
            <div className="form-grid two">
              <Form.Item name="username" label={TEXT.username} rules={[{ required: true }]}><Input /></Form.Item>
              <Form.Item name="password" label={TEXT.password} rules={[{ required: true }]}><Input.Password /></Form.Item>
              <Form.Item name="phone" label={TEXT.phone}><Input /></Form.Item>
              <Form.Item name="accountType" label={TEXT.accountType}><Select options={[{ value: "staff", label: TEXT.staff }, { value: "platform_admin", label: TEXT.platformAdmin }]} /></Form.Item>
              <Form.Item name="organizationId" label={TEXT.organization}><Select allowClear options={snapshotValue.organizations.map((item) => ({ value: item.id, label: item.name }))} /></Form.Item>
              <Form.Item name="merchantId" label={TEXT.merchant}><Select allowClear options={snapshotValue.merchantsRaw.map((item) => ({ value: item.id, label: item.displayName }))} /></Form.Item>
            </div>
            <Button type="primary" htmlType="submit">{TEXT.createUserButton}</Button>
          </Form>
        </div>
      </div>
      <DataTables snapshotValue={snapshotValue} />
    </section>
  );
}
