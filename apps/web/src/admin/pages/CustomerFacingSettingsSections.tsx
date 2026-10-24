// 文件版本：V0.01
import React from "react";
import { ApiError } from "../../shared/apiError";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

// ROOT-03 B 段券与展示组：客人端相关的四块后台配置，分别嵌在既有页面里（不新增后台页）。
type SectionProps = Pick<AdminPageProps, "api" | "setActionResult" | "setGlobalError" | "effectivePermissions">;

function errorText(err: unknown, fallback: string): string {
  return err instanceof Error ? err.message : fallback;
}

function wholeNumber(value: string): number | null {
  const n = Number(value);
  return value.trim() !== "" && Number.isInteger(n) && n >= 1 ? n : null;
}

type ExitNoticePolicy = { publishDays?: number; minDays?: number; maxDays?: number };
const EXIT_POLICY_PATH = "/api/v1/loyalty/stored-value/merchant-card-exit-notice-policy";

/** ROOT-03-R31（ENUM 第 86 行「公示期不少于后台配置的天数（业务参数，全平台统一可配，须同时配置上下限）」）：储值规则配置页内。 */
export function ExitNoticePolicySection({ api, setActionResult, setGlobalError, effectivePermissions }: SectionProps) {
  const [current, setCurrent] = React.useState<ExitNoticePolicy | null | undefined>(undefined);
  const [days, setDays] = React.useState("");
  const [minDays, setMinDays] = React.useState("");
  const [maxDays, setMaxDays] = React.useState("");
  const [reason, setReason] = React.useState("");

  async function load() {
    try {
      const policy = await api.get<ExitNoticePolicy | null>(EXIT_POLICY_PATH);
      setCurrent(policy ?? null);
      if (policy) {
        setDays(String(policy.publishDays ?? ""));
        setMinDays(String(policy.minDays ?? ""));
        setMaxDays(String(policy.maxDays ?? ""));
      }
    } catch (err) {
      setGlobalError(errorText(err, "退出公示天数读取失败"));
    }
  }

  async function save() {
    const value = wholeNumber(days);
    const min = wholeNumber(minDays);
    const max = wholeNumber(maxDays);
    if (value === null || min === null || max === null || min > value || value > max) {
      setGlobalError("公示天数与上下限须同时填写（整数天，下限不小于 1），天数须在上下限之间");
      return;
    }
    try {
      await api.post(EXIT_POLICY_PATH, { publishDays: value, minDays: min, maxDays: max, reason: reason.trim() || null });
      setActionResult({ action: "stored_value_exit_notice_policy_saved", publishDays: value, minDays: min, maxDays: max });
      void load();
    } catch (err) {
      setGlobalError(errorText(err, "退出公示天数保存失败"));
    }
  }

  return (
    <>
      <h3>商户级储值卡退出公示天数（全平台统一；须同时配置上下限；未配置前商户不能发布退出公告）</h3>
      <div className="admin-form-grid" data-testid="stored-value-exit-notice-policy-form">
        <button type="button" data-testid="stored-value-exit-notice-policy-query" onClick={() => void load()}>查询公示天数</button>
        <span className="admin-muted-action" data-testid="stored-value-exit-notice-policy-current">
          {current === undefined ? "未查询" : current === null ? "尚未配置" : `当前：不少于 ${current.publishDays} 天，下限 ${current.minDays} 天，上限 ${current.maxDays} 天`}
        </span>
        <label>公示天数<input aria-label="退出公示天数" type="number" min="1" value={days} onChange={(e) => setDays(e.target.value)} /></label>
        <label>下限（天）<input aria-label="退出公示天数下限" type="number" min="1" value={minDays} onChange={(e) => setMinDays(e.target.value)} /></label>
        <label>上限（天）<input aria-label="退出公示天数上限" type="number" min="1" value={maxDays} onChange={(e) => setMaxDays(e.target.value)} /></label>
        <label>配置缘由<input aria-label="退出公示天数配置缘由" value={reason} onChange={(e) => setReason(e.target.value)} /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={["customer.stored_value.refund"]}
          data-testid="stored-value-exit-notice-policy-save" onClick={() => void save()}>保存公示天数</PermissionButton>
      </div>
    </>
  );
}

type ReminderPolicy = { projectId?: string; remindHours?: number; minHours?: number | null; maxHours?: number | null };

/** ROOT-03 clients-R10（BSM 第 1596 行「出厂默认到期前二十四小时，须同时配置上下限」，按项目）：票务策略页内。 */
export function TicketExpiryReminderSection({ api, setActionResult, setGlobalError, effectivePermissions }: SectionProps) {
  const [projectId, setProjectId] = React.useState("");
  const [current, setCurrent] = React.useState<ReminderPolicy | null>(null);
  const [hours, setHours] = React.useState("");
  const [minHours, setMinHours] = React.useState("");
  const [maxHours, setMaxHours] = React.useState("");

  function path(): string {
    return `/api/v1/ticketing/expiry-reminder-policies/${encodeURIComponent(projectId.trim())}`;
  }

  async function load() {
    if (!projectId.trim()) { setGlobalError("请先填写综合体项目 id"); return; }
    try {
      const policy = await api.get<ReminderPolicy>(path());
      setCurrent(policy ?? null);
      setHours(String(policy?.remindHours ?? ""));
      setMinHours(policy?.minHours == null ? "" : String(policy.minHours));
      setMaxHours(policy?.maxHours == null ? "" : String(policy.maxHours));
    } catch (err) {
      setGlobalError(errorText(err, "到期提醒参数读取失败"));
    }
  }

  async function save() {
    const value = wholeNumber(hours);
    const min = wholeNumber(minHours);
    const max = wholeNumber(maxHours);
    if (!projectId.trim()) { setGlobalError("请先填写综合体项目 id"); return; }
    if (value === null || min === null || max === null || min > value || value > max) {
      setGlobalError("提前小时数与上下限须同时填写（整数小时，下限不小于 1），小时数须在上下限之间");
      return;
    }
    try {
      await api.put(path(), { remindHours: value, minHours: min, maxHours: max });
      setActionResult({ action: "ticket_expiry_reminder_policy_saved", projectId: projectId.trim(), remindHours: value, minHours: min, maxHours: max });
      void load();
    } catch (err) {
      setGlobalError(errorText(err, "到期提醒参数保存失败"));
    }
  }

  return (
    <div className="admin-preview" data-testid="ticket-expiry-reminder-policy">
      <strong>未核销票到期提醒（按项目；出厂到期前 24 小时；须同时配置上下限）：</strong>
      客人官方 App / 小程序在票到期前按此提前量弹出提醒，客人不能关闭此类提醒。
      <div className="admin-form-grid" style={{ marginTop: 8 }}>
        <label>项目 id<input aria-label="到期提醒项目 id" value={projectId} onChange={(e) => setProjectId(e.target.value)} /></label>
        <button type="button" data-testid="ticket-expiry-reminder-query" onClick={() => void load()}>查询</button>
        <span className="admin-muted-action" data-testid="ticket-expiry-reminder-current">
          {current ? `当前：到期前 ${current.remindHours} 小时${current.minHours == null ? "（出厂值，上下限未配置）" : `，下限 ${current.minHours}、上限 ${current.maxHours} 小时`}` : "未查询"}
        </span>
        <label>提前小时数<input aria-label="到期提醒提前小时数" type="number" min="1" value={hours} onChange={(e) => setHours(e.target.value)} /></label>
        <label>下限（小时）<input aria-label="到期提醒下限" type="number" min="1" value={minHours} onChange={(e) => setMinHours(e.target.value)} /></label>
        <label>上限（小时）<input aria-label="到期提醒上限" type="number" min="1" value={maxHours} onChange={(e) => setMaxHours(e.target.value)} /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={["ticket.product.manage"]}
          data-testid="ticket-expiry-reminder-save" onClick={() => void save()}>保存到期提醒</PermissionButton>
      </div>
    </div>
  );
}

type ExitNotice = {
  id: string; merchantName?: string; useUntil?: string; refundable?: boolean; refundRatioBps?: number;
  refundMethod?: string; refundContact?: string; publishUntil?: string;
};
const MY_NOTICES_PATH = "/api/v1/merchant-portal/card-exit-notices";

/** ROOT-03-R31＋第十三批裁决第 2 条「商户在商户门户自行发公告，平台只校验公示天数，不影响退租流程」：商户专区「我的合同」页内。 */
export function MerchantCardExitNoticeSection({ api, setActionResult, setGlobalError, effectivePermissions }: SectionProps) {
  const [notices, setNotices] = React.useState<ExitNotice[] | null>(null);
  const [useUntil, setUseUntil] = React.useState("");
  const [refundable, setRefundable] = React.useState(true);
  const [refundPct, setRefundPct] = React.useState("100");
  const [refundMethod, setRefundMethod] = React.useState("");
  const [refundContact, setRefundContact] = React.useState("");
  const [publishUntil, setPublishUntil] = React.useState("");

  async function load() {
    try {
      setNotices(await api.get<ExitNotice[]>(MY_NOTICES_PATH) ?? []);
    } catch (err) {
      setGlobalError(errorText(err, "退出公告读取失败"));
    }
  }

  async function publish() {
    const pct = Number(refundPct);
    if (!useUntil || !publishUntil || !refundMethod.trim() || !refundContact.trim() || !Number.isFinite(pct) || pct < 0 || pct > 100) {
      setGlobalError("请写明可用到哪天、能否退、退款比例（0-100%）、怎么退、找谁退，以及公示截止日");
      return;
    }
    try {
      await api.post(MY_NOTICES_PATH, {
        useUntil, refundable, refundRatioBps: Math.round(pct * 100), refundMethod: refundMethod.trim(),
        refundContact: refundContact.trim(), publishUntil,
      });
      setActionResult({ action: "merchant_card_exit_notice_published", useUntil, publishUntil });
      void load();
    } catch (err) {
      setGlobalError(errorText(err, "退出公告发布失败"));
    }
  }

  return (
    <>
      <h3>储值卡退出公告（本商户退租、合同终止或被清退前提前公告持卡客人；客人 App / 小程序同步展示，店面也须张贴）</h3>
      <div className="admin-form-grid" data-testid="merchant-card-exit-notice-form">
        <label>可用至<input aria-label="储值卡可用至" type="date" value={useUntil} onChange={(e) => setUseUntil(e.target.value)} /></label>
        <label>能否退
          <select aria-label="储值卡能否退" value={refundable ? "yes" : "no"} onChange={(e) => setRefundable(e.target.value === "yes")}>
            <option value="yes">可退</option>
            <option value="no">不可退</option>
          </select>
        </label>
        <label>退款比例(%)<input aria-label="储值卡退款比例" type="number" min="0" max="100" value={refundPct} onChange={(e) => setRefundPct(e.target.value)} /></label>
        <label>怎么退<input aria-label="储值卡怎么退" value={refundMethod} onChange={(e) => setRefundMethod(e.target.value)} /></label>
        <label>找谁退<input aria-label="储值卡找谁退" value={refundContact} onChange={(e) => setRefundContact(e.target.value)} /></label>
        <label>公示截止日<input aria-label="退出公告公示截止日" type="date" value={publishUntil} onChange={(e) => setPublishUntil(e.target.value)} /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={["merchant.portal.card_exit_notice.publish"]}
          data-testid="merchant-card-exit-notice-publish" onClick={() => void publish()}>发布退出公告</PermissionButton>
        <button type="button" data-testid="merchant-card-exit-notice-list" onClick={() => void load()}>查看已发布公告</button>
      </div>
      {notices && (
        <ul data-testid="merchant-card-exit-notices">
          {notices.length === 0 ? <li>暂无已发布的退出公告</li> : notices.map((n) => (
            <li key={n.id}>可用至 {n.useUntil}；{n.refundable ? `可退 ${(Number(n.refundRatioBps ?? 0) / 100).toFixed(0)}%` : "不可退"}；
              怎么退：{n.refundMethod}；找谁退：{n.refundContact}；公示至 {n.publishUntil}</li>
          ))}
        </ul>
      )}
    </>
  );
}

type HomeContent = { intro?: string; announcement?: string; publishedAt?: string };

/**
 * ROOT-03 clients-R9（BSM 第 857 行）＋第十三批裁决第 5 条「运营编辑后走审批，批准才对客人展示」：场地营业时间页内。
 * 首次提交即建复核单；上级在「治理 → 主管复核与违规阻断」批准后，原样再提交一次才对客人展示。
 */
export function CustomerHomeContentSection({ api, setActionResult, setGlobalError, effectivePermissions }: SectionProps) {
  const [projectId, setProjectId] = React.useState("");
  const [published, setPublished] = React.useState<HomeContent | null | undefined>(undefined);
  const [intro, setIntro] = React.useState("");
  const [announcement, setAnnouncement] = React.useState("");
  const [note, setNote] = React.useState("");

  function base(): string {
    return `/api/v1/home-contents/${encodeURIComponent(projectId.trim())}`;
  }

  async function load() {
    if (!projectId.trim()) { setGlobalError("请先填写综合体项目 id"); return; }
    try {
      const content = await api.get<HomeContent | null>(base());
      setPublished(content ?? null);
      setIntro(content?.intro ?? "");
      setAnnouncement(content?.announcement ?? "");
    } catch (err) {
      setGlobalError(errorText(err, "客人端首页内容读取失败"));
    }
  }

  async function publish() {
    if (!projectId.trim() || !intro.trim()) { setGlobalError("请填写综合体项目 id 与综合体介绍"); return; }
    setNote("");
    try {
      await api.post(`${base()}/publish`, { intro: intro.trim(), announcement: announcement.trim() });
      setActionResult({ action: "customer_home_content_published", projectId: projectId.trim() });
      void load();
    } catch (err) {
      if (err instanceof ApiError && err.detailCode === "ADMISSION_BLOCKED") {
        setNote("已提交上级审批：审批通过后，请原样再点一次「提交发布」，客人端随即展示。");
      }
      setGlobalError(errorText(err, "客人端首页内容提交失败"));
    }
  }

  return (
    <div className="admin-preview" data-testid="customer-home-content">
      <strong>客人端首页：综合体介绍与活动公告（运营编辑后须上级审批，批准才对客人展示）</strong>
      <div className="admin-form-grid" style={{ marginTop: 8 }}>
        <label>项目 id<input aria-label="首页内容项目 id" value={projectId} onChange={(e) => setProjectId(e.target.value)} /></label>
        <button type="button" data-testid="customer-home-content-query" onClick={() => void load()}>查询已发布内容</button>
        <span className="admin-muted-action" data-testid="customer-home-content-current">
          {published === undefined ? "未查询" : published === null ? "尚未发布" : `已发布于 ${published.publishedAt ?? ""}`}
        </span>
        <label>综合体介绍<textarea aria-label="综合体介绍" value={intro} onChange={(e) => setIntro(e.target.value)} /></label>
        <label>活动公告<textarea aria-label="活动公告" value={announcement} onChange={(e) => setAnnouncement(e.target.value)} /></label>
        <PermissionButton permissionsState={effectivePermissions} permissions={["customer.home_content.manage"]}
          data-testid="customer-home-content-publish" onClick={() => void publish()}>提交发布（须上级审批）</PermissionButton>
      </div>
      {note && <p className="admin-muted-action" data-testid="customer-home-content-note">{note}</p>}
    </div>
  );
}
