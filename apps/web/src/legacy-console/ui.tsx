import React from "react";
import { Badge, Table } from "antd";
import type { ColumnsType } from "antd/es/table";

export function ResultBox({ value }: { value: unknown }) {
  if (!value) return null;
  return <pre className="result-box" data-testid="action-result">{JSON.stringify(value, null, 2)}</pre>;
}

export function SectionHeading({ icon, title, extra }: { icon: React.ReactNode; title: string; extra?: React.ReactNode }) {
  return (
    <div className="section-heading">
      {icon}
      <h1>{title}</h1>
      {extra ? <div className="section-extra">{extra}</div> : null}
    </div>
  );
}

export function SimpleTable<T extends { id?: string; key?: React.Key }>({ title, data, columns }: { title: string; data: T[]; columns: ColumnsType<T> }) {
  return (
    <div className="table-panel">
      <div className="panel-title panel-title-row"><span>{title}</span><Badge className="panel-count" count={data.length} overflowCount={999} showZero /></div>
      <Table className="data-table" columns={columns} dataSource={data} rowKey={(row, index) => String(row.id ?? row.key ?? index)} pagination={false} size="small" scroll={{ x: true }} />
    </div>
  );
}
