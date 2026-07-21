import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { PermissionButton } from "../security/Can";
import type { AdminPageProps } from "../types";

// 结算配置中心(新立项批0)总控页 —— PRD settlement-rebuild §9(ADM-STL-001)。
// 批0 骨架:①「看全局」当前生效配置一览(读 /settlement/config/overview,已强制只读正式口径);
// ②草案待复核队列 + 复核转正(POST activate,须 settlement.config.manage)。
// 各组具体配置维护页随批1-5 接入本页(以标签/区块扩展)。
const MANAGE = ["settlement.config.manage"];

type ConfigItem = { group: string; key: string; label: string; value: string };
type DraftItem = { id: string; configGroup: string; summary: string };
type Overview = { platformDefaults: ConfigItem[]; pendingDraftCount: number; pendingDrafts: DraftItem[] };

export function SettlementConfigPage({ api, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [data, setData] = React.useState<Overview | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [activating, setActivating] = React.useState<string | null>(null);

  const reload = React.useCallback(() => {
    setLoading(true);
    setError(null);
    api
      .get<Overview>("/api/v1/settlement/config/overview")
      .then((d) => {
        setData(d);
        setLoading(false);
      })
      .catch((e: unknown) => {
        setError((e as Error)?.message ?? "结算配置总览读取失败");
        setLoading(false);
      });
  }, [api]);

  React.useEffect(() => {
    reload();
  }, [reload]);

  async function activate(id: string) {
    setActivating(id);
    try {
      await api.post(`/api/v1/settlement/config/attribution-rules/${id}/activate`);
      reload();
    } catch (e: unknown) {
      setGlobalError((e as Error)?.message ?? "草案转正失败");
    } finally {
      setActivating(null);
    }
  }

  const defaults = data?.platformDefaults ?? [];
  const drafts = data?.pendingDrafts ?? [];

  return (
    <section data-testid="settlement-config-page">
      <AdminPageHeader
        title="结算配置中心"
        description="统一查看结算相关配置的当前生效口径,并复核转正配置草案。配置改动一律先落草案、经复核转正后才生效(草案不即时生效)。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <div className="admin-muted-action" data-testid="settlement-config-guide">
          本页为结算配置中心骨架:当前展示「通票分账/赠酒/门票综合服务费」等的平台默认生效口径与待复核草案队列;
          税费、地产费用、提成分红、内部结算价等各组的可视化维护将随后续批次接入本页。
        </div>

        <h3>当前生效配置(平台默认口径)</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="settlement-config-defaults-table">
            <thead>
              <tr>
                <th>分组</th>
                <th>配置项</th>
                <th>当前生效值</th>
              </tr>
            </thead>
            <tbody>
              {defaults.map((it) => (
                <tr key={it.key} data-testid={`settlement-config-default-${it.key}`}>
                  <td>{it.group}</td>
                  <td>{it.label}</td>
                  <td className="admin-num" style={{ textAlign: "right" }}>{it.value}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <h3>待复核草案({data?.pendingDraftCount ?? 0})</h3>
        {drafts.length === 0 ? (
          <p className="admin-muted-action" data-testid="settlement-config-drafts-empty">
            当前无待复核草案。配置改动会先落草案、在此复核转正后方生效。
          </p>
        ) : (
          <div className="admin-table-scroll">
            <table className="admin-table" data-testid="settlement-config-drafts-table">
              <thead>
                <tr>
                  <th>配置组</th>
                  <th>草案内容</th>
                  <th>复核</th>
                </tr>
              </thead>
              <tbody>
                {drafts.map((d) => (
                  <tr key={d.id} data-testid={`settlement-config-draft-${d.id}`}>
                    <td>{d.configGroup}</td>
                    <td>{d.summary}</td>
                    <td>
                      <PermissionButton
                        permissionsState={effectivePermissions}
                        permissions={MANAGE}
                        data-testid={`settlement-config-activate-${d.id}`}
                        disabled={activating === d.id}
                        onClick={() => void activate(d.id)}
                      >
                        {activating === d.id ? "转正中…" : "复核转正"}
                      </PermissionButton>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </AdminDataState>
    </section>
  );
}
