import { AuditOutlined } from "@ant-design/icons";
import { Tag } from "antd";
import { formatTime, statusColor } from "../formatters";
import type { Snapshot } from "../types";
import { SectionHeading, SimpleTable } from "../ui";

const TEXT = {
  pageTitle: "\u4e8b\u4ef6\u7559\u75d5",
  tableTitle: "\u6700\u8fd1\u4e8b\u4ef6",
  type: "\u7c7b\u578b",
  category: "\u5206\u7c7b",
  summary: "\u6458\u8981",
  result: "\u7ed3\u679c",
  time: "\u65f6\u95f4",
};

export type EventsPageProps = {
  snapshotValue: Snapshot;
};

export function EventsPage({ snapshotValue }: EventsPageProps) {
  return (
    <section className="section-block">
      <SectionHeading icon={<AuditOutlined />} title={TEXT.pageTitle} />
      <SimpleTable title={TEXT.tableTitle} data={snapshotValue.events} columns={[
        { title: TEXT.type, dataIndex: "eventType" },
        { title: TEXT.category, dataIndex: "eventCategory" },
        { title: TEXT.summary, dataIndex: "summary" },
        { title: TEXT.result, dataIndex: "result", render: (value) => <Tag color={statusColor(value)}>{value}</Tag> },
        { title: TEXT.time, dataIndex: "occurredAt", render: formatTime },
      ]} />
    </section>
  );
}
