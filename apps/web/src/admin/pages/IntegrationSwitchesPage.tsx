import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps, CapabilitySwitchRow } from "../types";
import { PermissionSwitch } from "../components/PermissionSwitch";

type SwitchCatalogEntry = {
  label: string;
  category: string;
  disabledBehavior: string;
  enabledRequirements: string;
};

/**
 * 第三方/集成能力开关「接入状态」目录：按 capability_code 标注（同步自
 * docs/integration-capability-switches-v0.1.md 矩阵 v0.1）。disabledBehavior=当前未接入时的业务降级口径；
 * enabledRequirements=接入真实第三方 API/SDK 所需。新增第三方开关时在此补一条即可获得完整标注。
 */
const THIRD_PARTY_SWITCH_CATALOG: Record<string, SwitchCatalogEntry> = {
  "face.verification.enabled": {
    label: "人脸入场闸（实时年龄/人脸核验）",
    category: "人脸/年龄合规",
    disabledBehavior: "关闭：入场/售酒走人工证件核验 + 制度兜底，不展示「自动人脸已通过」；酒吧售酒继承入场闸豁免（非阻塞放行）。",
    enabledRequirements: "开启需：第三方人脸/年龄 SDK/API + 成功/失败样例 + 错误码 + 合规材料 + 测试账号 + credential_ref；开启后入场闸 fail-closed 判龄。",
  },
  "integration.face_age.sync": {
    label: "人脸/年龄第三方对账同步",
    category: "人脸/年龄合规",
    disabledBehavior: "关闭：不与第三方人脸/年龄供应商做数据对账同步。",
    enabledRequirements: "开启需：供应商对账接口 + 字段映射 + credential_ref。",
  },
  "integration.payment.sync": {
    label: "聚合支付回流/对账",
    category: "支付",
    disabledBehavior: "关闭：不读取聚合支付接口；支付仅允许现金 / 线下 POS / 人工收款确认并写审计留痕。",
    enabledRequirements: "开启需：商户号 + 回调验签 + 退款权限 + 对账文件 + credential_ref + 失败补偿 + 生产证据。",
  },
  "integration.ticket_channel.sync": {
    label: "团购/渠道售票（美团/大众点评/抖音）",
    category: "票务渠道",
    disabledBehavior: "关闭：不接渠道订单/票码/退款/对账；仅平台自出票 + 后台手动补录渠道信息。",
    enabledRequirements: "开启需：渠道 API/SDK + 券码核销 + 订单回流 + 退款同步 + 对账口径 + 生产证据。",
  },
  "integration.wecom.sync": {
    label: "企业微信",
    category: "企业微信",
    disabledBehavior: "关闭：不读通讯录/会话存档/客户联系/回调；员工后台手动录入，质检仅用平台内数据。",
    enabledRequirements: "开启需：企微应用 + 通讯录 + 会话存档 + 回调 + 同步任务 + 告警 + credential_ref。",
  },
  "integration.catering_pos.sync": {
    label: "餐饮 POS",
    category: "餐饮/零售",
    disabledBehavior: "关闭：不接餐饮 POS 点单/收银/券核销/退款/回流/对账；仅后台手工登记或不启用。",
    enabledRequirements: "开启需：餐饮 POS API/SDK + 订单/退款/券核销字段 + Webhook/轮询 + 对账文件 + credential_ref。",
  },
  "integration.retail_pos.sync": {
    label: "零售 POS",
    category: "餐饮/零售",
    disabledBehavior: "关闭：不接零售 POS SKU/收银/券核销/退款/回流/对账；仅后台手工登记或不启用。",
    enabledRequirements: "开启需：零售 POS API/SDK + SKU/订单/退款/券核销字段 + Webhook/轮询 + 对账文件 + credential_ref。",
  },
  "integration.robot.sync": {
    label: "机器人平台",
    category: "机器人",
    disabledBehavior: "关闭：不下发机器人任务/不接机器人状态；配送与交接按人工流程留痕。",
    enabledRequirements: "开启需：机器人任务 API + 状态回调 + 签名 + 异常补偿 + 测试账号 + 生产证据。",
  },
  "integration.video.sync": {
    label: "视频监控平台",
    category: "视频监控",
    disabledBehavior: "关闭：不读摄像头/录像/片段链接；仅记录人工 evidence_link，不做内嵌播放/自动关联。",
    enabledRequirements: "开启需：摄像头点位 + 播放鉴权 + 截图/片段接口 + 事件关联 + 审计回调 + credential_ref。",
  },
};

function accessStatusOf(row: CapabilitySwitchRow, entry: SwitchCatalogEntry | undefined): string {
  if (row.enabled) {
    return "已开启（须已接入真实 API/SDK + 生产证据）";
  }
  return entry ? "未接入（开关关闭，待厂商 API/SDK + 后台开启）" : "已关闭";
}

export function IntegrationSwitchesPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [rows, setRows] = React.useState<CapabilitySwitchRow[]>([]);

  const { loading, error, reload } = useAdminResource(
    async () => {
      const items = await api.get<CapabilitySwitchRow[]>("/api/v1/terminals/capability-switches");
      // 仅展示第三方/集成能力开关（catalog 已登记的 capability_code），过滤纯终端模式开关。
      setRows((items ?? []).filter((row) => THIRD_PARTY_SWITCH_CATALOG[row.capabilityCode] !== undefined));
    },
    { onError: setGlobalError, fallbackMessage: "第三方开关读取失败" },
  );

  async function toggle(row: CapabilitySwitchRow) {
    const next = !row.enabled;
    const updated = await api.put<CapabilitySwitchRow>(`/api/v1/terminals/capability-switches/${row.id}`, {
      enabled: next,
      terminalType: row.terminalType,
      capabilityCode: row.capabilityCode,
      scopeId: row.scopeId,
    });
    setRows((current) => current.map((item) => (item.id === row.id ? { ...item, enabled: updated.enabled } : item)));
    setActionResult({ action: "capability_switch_toggled", capabilityCode: row.capabilityCode, enabled: updated.enabled });
  }

  return (
    <section data-testid="integration-switches-page">
      <AdminPageHeader
        title="第三方开关"
        description="所有第三方接口 / 集成能力开关，默认关闭。仅平台管理员可切换；每个开关标注关闭口径、开启要求与当前 API/SDK 接入状态。"
      />
      <AdminDataState loading={loading} error={error} empty={rows.length === 0} onRetry={reload}>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="integration-switches-table">
            <thead>
              <tr>
                <th>能力</th>
                <th>类别</th>
                <th>状态</th>
                <th>接入状态</th>
                <th>关闭口径 / 开启要求</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => {
                const entry = THIRD_PARTY_SWITCH_CATALOG[row.capabilityCode];
                return (
                  <tr key={row.id} data-testid={`switch-row-${row.capabilityCode}`}>
                    <td>
                      {entry ? entry.label : row.capabilityCode}
                      <br />
                      <small>{row.capabilityCode}</small>
                    </td>
                    <td>{entry ? entry.category : "—"}</td>
                    <td data-testid={`switch-state-${row.capabilityCode}`}>{row.enabled ? "开启" : "关闭"}</td>
                    <td data-testid={`switch-access-${row.capabilityCode}`}>{accessStatusOf(row, entry)}</td>
                    <td>
                      <div>关闭：{entry ? entry.disabledBehavior : "—"}</div>
                      <div>开启：{entry ? entry.enabledRequirements : "—"}</div>
                    </td>
                    <td>
                      <PermissionSwitch
                        permissionsState={effectivePermissions}
                        permissions={["terminal.switch.manage"]}
                        checked={row.enabled}
                        onChange={() => void toggle(row)}
                        testId={`switch-toggle-${row.capabilityCode}`}
                      />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </AdminDataState>
    </section>
  );
}
