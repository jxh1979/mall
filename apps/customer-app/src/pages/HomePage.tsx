// 文件版本：V0.01
import { Alert, Card, Empty, List, Spin, Tag, Typography } from "antd";
import dayjs from "dayjs";
import { useEffect, useState } from "react";

import type { AmusementProject, AmusementSession, CustomerBusinessHours, CustomerProject, CustomerService } from "@complex/customer-core";
import type { TicketProduct } from "@complex/customer-core";
import { formatBusinessStatus } from "@complex/customer-core";

const { Title, Paragraph, Text } = Typography;

const WEEKDAYS = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"];

function formatYuan(cent: number | null): string {
  if (cent == null) return "—";
  return `¥${(cent / 100).toFixed(2)}`;
}

// 闭店时刻不晚于开门时刻＝跨夜营业，落次日。
function formatHours(row: CustomerBusinessHours): string {
  const overnight = row.closeTime <= row.openTime;
  return `${WEEKDAYS[row.dayOfWeek] ?? ""} ${row.openTime}–${overnight ? "次日 " : ""}${row.closeTime}`;
}

function hoursByVenue(rows: CustomerBusinessHours[]): Array<{ venueName: string; days: string[] }> {
  const venues = new Map<string, { venueName: string; days: string[] }>();
  for (const row of rows) {
    const venue = venues.get(row.venueId) ?? { venueName: row.venueName, days: [] };
    venue.days.push(formatHours(row));
    venues.set(row.venueId, venue);
  }
  return [...venues.values()];
}

function sessionText(session: AmusementSession): string {
  const left = Math.max(0, session.capacity - session.usedCapacity);
  return `${dayjs(session.startTime).format("M月D日 HH:mm")}–${dayjs(session.endTime).format("HH:mm")} · ${formatBusinessStatus(session.status)}`
    + ` · 余 ${left} / ${session.capacity}`;
}

// ① 首页（BSM「查看综合体介绍、营业时间、活动公告、票务入口」）：综合体介绍与活动公告只展示经审批发布的（第十三批裁决第 5 条
// 「运营编辑后走审批，批准才对客人展示」，服务端未发布即为空、客人端不展示）；各场地营业时间；可售票种（购票入口在本页下方）。
// 每个综合体开放中的游乐项目与尚未结束的场次（BSM「连接游乐系统，展示可预定项目和场次」）：只展示，不在此预约；读不到不显示。
export function HomePage({ service }: { service: CustomerService }): React.ReactElement {
  const [projects, setProjects] = useState<CustomerProject[]>([]);
  const [amusements, setAmusements] = useState<Record<string, AmusementProject[]>>({});
  const [products, setProducts] = useState<TicketProduct[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [projectError, setProjectError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    service
      .listProjects()
      .then((items) => {
        if (!active) return;
        setProjects(items ?? []);
        for (const project of items ?? []) {
          service.listAmusementProjects(project.projectId)
            .then((rows) => active && setAmusements((prev) => ({ ...prev, [project.projectId]: rows ?? [] })))
            .catch(() => undefined);
        }
      })
      .catch((err) => active && setProjectError(err instanceof Error ? err.message : "综合体信息加载失败"));
    service
      .listTicketProducts()
      .then((items) => {
        if (active) setProducts(items ?? []);
      })
      .catch((err) => active && setError(err instanceof Error ? err.message : "票种加载失败"))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [service]);

  return (
    <div data-testid="home-page">
      {projectError ? <Alert type="error" message="综合体信息加载失败" description={projectError} style={{ marginBottom: 12 }} /> : null}
      {projects.map((project) => (
        <Card key={project.projectId} style={{ marginBottom: 16 }} data-testid="home-project">
          <Title level={4}>{project.projectName}</Title>
          {project.address ? <Paragraph type="secondary">{project.address}</Paragraph> : null}
          {project.intro ? <Paragraph style={{ whiteSpace: "pre-wrap" }}>{project.intro}</Paragraph> : null}
          {project.announcement ? (
            <Alert type="info" showIcon message="活动公告" data-testid="home-announcement" style={{ marginBottom: 12 }}
              description={<span style={{ whiteSpace: "pre-wrap" }}>{project.announcement}</span>} />
          ) : null}
          {project.businessHours.length > 0 ? (
            <div data-testid="home-business-hours">
              <Text strong>营业时间</Text>
              {hoursByVenue(project.businessHours).map((venue) => (
                <Paragraph key={venue.venueName} style={{ marginBottom: 4 }}>
                  {venue.venueName}：{venue.days.join("；")}
                </Paragraph>
              ))}
            </div>
          ) : null}
          {amusements[project.projectId]?.length ? (
            <div data-testid="home-amusement" style={{ marginTop: 12 }}>
              <Text strong>游乐项目</Text>
              {amusements[project.projectId]!.map((item) => (
                <div key={item.amusementProjectId} style={{ marginTop: 4 }}>
                  <Text>{item.projectName}</Text>
                  {item.sessions.length === 0 ? (
                    <Paragraph type="secondary" style={{ marginBottom: 4 }}>暂无可预定场次</Paragraph>
                  ) : (
                    item.sessions.map((session) => (
                      <Paragraph key={session.sessionId} type="secondary" style={{ marginBottom: 2 }}>{sessionText(session)}</Paragraph>
                    ))
                  )}
                </div>
              ))}
            </div>
          ) : null}
        </Card>
      ))}
      <Card title="可售票种">
        {error ? <Alert type="error" message="票种加载失败" description={error} style={{ marginBottom: 12 }} /> : null}
        {loading ? (
          <Spin />
        ) : products.length === 0 ? (
          <Empty description="暂无可售票种" />
        ) : (
          <List
            dataSource={products}
            renderItem={(item) => (
              <List.Item data-testid="ticket-product-item">
                <List.Item.Meta
                  title={
                    <span>
                      {item.ticketName} <Tag color={item.status === "active" || item.status === "published" ? "green" : "default"}>{formatBusinessStatus(item.status)}</Tag>
                    </span>
                  }
                  description={
                    <Text type="secondary">
                      平日 {formatYuan(item.weekdayPriceCent)} · 周末 {formatYuan(item.weekendPriceCent)} · 节假日{" "}
                      {formatYuan(item.holidayPriceCent)}
                    </Text>
                  }
                />
              </List.Item>
            )}
          />
        )}
      </Card>
    </div>
  );
}
