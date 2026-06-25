import React from "react";
import { AdminDataState } from "../components/AdminDataState";
import { AdminPageHeader } from "../components/AdminPageHeader";
import { useAdminResource } from "../hooks/useAdminResource";
import type { AdminPageProps } from "../types";
import { PermissionButton } from "../security/Can";

// 后端 LeasingLeadRecord / LeasingOfferRecord（客户端 unwrap 已 camel 化）。
type LeadRow = { id: string; contactName: string; intendedBusinessType?: string; intendedSpaceId?: string; source?: string; status: string };
type OfferRow = { id: string; leadId: string; spaceId?: string; merchantId?: string; effectiveDate?: string; expiryDate?: string; status: string; convertedContractId?: string };

const LEAD_MANAGE = ["leasing.lead.manage"];
const OFFER_MANAGE = ["leasing.offer.manage"];

/**
 * 招商租赁工作台（ADM-LEASE-001，② 履约闭环 S4）：招商线索漏斗 + 租赁报价 + 报价转合同。
 * accepted 报价「转合同」喂入既有合同环（生成合同 draft，进 draft→approve→生效）。
 */
export function LeasingWorkbenchPage({ api, setActionResult, setGlobalError, effectivePermissions }: AdminPageProps) {
  const [leads, setLeads] = React.useState<LeadRow[]>([]);
  const [offers, setOffers] = React.useState<OfferRow[]>([]);
  const [leadForm, setLeadForm] = React.useState({ contactName: "", intendedBusinessType: "bar", source: "walk_in" });
  const [offerForm, setOfferForm] = React.useState({ leadId: "", spaceId: "", merchantId: "", baseRentYuan: "" });
  const [convertSubjects, setConvertSubjects] = React.useState({ contractSubjectId: "", settlementSubjectId: "" });

  const { loading, error, reload } = useAdminResource(
    async () => {
      const [leadRows, offerRows] = await Promise.all([
        api.get<LeadRow[]>("/api/v1/leasing/leads"),
        api.get<OfferRow[]>("/api/v1/leasing/offers"),
      ]);
      setLeads(leadRows ?? []);
      setOffers(offerRows ?? []);
    },
    { onError: setGlobalError, fallbackMessage: "招商/租赁数据读取失败", onStart: () => setGlobalError(null) },
  );

  async function createLead() {
    if (!leadForm.contactName.trim()) {
      setGlobalError("请填写线索联系人");
      return;
    }
    await api.post("/api/v1/leasing/leads", {
      contactName: leadForm.contactName.trim(),
      intendedBusinessType: leadForm.intendedBusinessType,
      source: leadForm.source,
    });
    setActionResult({ action: "leasing_lead_created", contactName: leadForm.contactName.trim() });
    setLeadForm({ ...leadForm, contactName: "" });
    reload();
  }

  async function setLeadStatus(lead: LeadRow, status: string) {
    await api.post(`/api/v1/leasing/leads/${lead.id}/status`, { status });
    setActionResult({ action: "leasing_lead_status", leadId: lead.id, status });
    reload();
  }

  async function createOffer() {
    const cent = Math.round(Number(offerForm.baseRentYuan) * 100);
    if (!offerForm.leadId.trim()) {
      setGlobalError("请选择线索 id");
      return;
    }
    await api.post("/api/v1/leasing/offers", {
      leadId: offerForm.leadId.trim(),
      spaceId: offerForm.spaceId.trim() || null,
      merchantId: offerForm.merchantId.trim() || null,
      rentTerms: Number.isFinite(cent) && cent > 0 ? { baseRentCent: cent } : {},
    });
    setActionResult({ action: "leasing_offer_created", leadId: offerForm.leadId.trim() });
    setOfferForm({ ...offerForm, baseRentYuan: "" });
    reload();
  }

  async function setOfferStatus(offer: OfferRow, status: string) {
    await api.post(`/api/v1/leasing/offers/${offer.id}/status`, { status });
    setActionResult({ action: "leasing_offer_status", offerId: offer.id, status });
    reload();
  }

  async function convertOffer(offer: OfferRow) {
    if (!convertSubjects.contractSubjectId.trim() || !convertSubjects.settlementSubjectId.trim()) {
      setGlobalError("转合同需填写合同主体与结算主体 id");
      return;
    }
    const contract = await api.post<{ id: string }>(`/api/v1/leasing/offers/${offer.id}/convert-to-contract`, {
      contractSubjectId: convertSubjects.contractSubjectId.trim(),
      settlementSubjectId: convertSubjects.settlementSubjectId.trim(),
    });
    setActionResult({ action: "leasing_offer_converted", offerId: offer.id, contractId: contract?.id });
    reload();
  }

  return (
    <section data-testid="leasing-workbench-page">
      <AdminPageHeader
        title="招商租赁工作台"
        description="招商线索漏斗 + 租赁报价 + 报价转合同。accepted 报价「转合同」生成合同 draft，进入既有合同审批→生效→绑定空间履约链。"
      />
      <AdminDataState loading={loading} error={error} empty={false} onRetry={reload}>
        <h3>招商线索</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="leasing-leads-table">
            <thead><tr><th>id</th><th>联系人</th><th>意向业态</th><th>来源</th><th>状态</th><th>操作</th></tr></thead>
            <tbody>
              {leads.map((row) => (
                <tr key={row.id} data-testid={`leasing-lead-${row.id}`}>
                  <td>{row.id}</td><td>{row.contactName}</td><td>{row.intendedBusinessType ?? "-"}</td>
                  <td>{row.source ?? "-"}</td><td>{row.status}</td>
                  <td>
                    <PermissionButton permissionsState={effectivePermissions} permissions={LEAD_MANAGE}
                      data-testid={`leasing-lead-qualify-${row.id}`} onClick={() => void setLeadStatus(row, "qualified")}>标记合格</PermissionButton>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="leasing-lead-form">
          <input aria-label="联系人" placeholder="联系人" value={leadForm.contactName} onChange={(e) => setLeadForm({ ...leadForm, contactName: e.target.value })} />
          <input aria-label="意向业态" placeholder="意向业态" value={leadForm.intendedBusinessType} onChange={(e) => setLeadForm({ ...leadForm, intendedBusinessType: e.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={LEAD_MANAGE}
            data-testid="leasing-lead-create" onClick={() => void createLead()}>新建线索</PermissionButton>
        </div>

        <h3>租赁报价</h3>
        <div className="admin-table-scroll">
          <table className="admin-table" data-testid="leasing-offers-table">
            <thead><tr><th>id</th><th>线索</th><th>空间</th><th>拟商户</th><th>状态</th><th>已转合同</th><th>操作</th></tr></thead>
            <tbody>
              {offers.map((row) => (
                <tr key={row.id} data-testid={`leasing-offer-${row.id}`}>
                  <td>{row.id}</td><td>{row.leadId}</td><td>{row.spaceId ?? "-"}</td><td>{row.merchantId ?? "-"}</td>
                  <td>{row.status}</td><td>{row.convertedContractId ?? "-"}</td>
                  <td>
                    <PermissionButton permissionsState={effectivePermissions} permissions={OFFER_MANAGE}
                      data-testid={`leasing-offer-send-${row.id}`} onClick={() => void setOfferStatus(row, "sent")}>发送</PermissionButton>
                    <PermissionButton permissionsState={effectivePermissions} permissions={OFFER_MANAGE}
                      data-testid={`leasing-offer-accept-${row.id}`} onClick={() => void setOfferStatus(row, "accepted")}>接受</PermissionButton>
                    <PermissionButton permissionsState={effectivePermissions} permissions={OFFER_MANAGE}
                      data-testid={`leasing-offer-convert-${row.id}`} onClick={() => void convertOffer(row)}>转合同</PermissionButton>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="admin-inline-form" data-testid="leasing-offer-form">
          <input aria-label="线索 id" placeholder="线索 id" value={offerForm.leadId} onChange={(e) => setOfferForm({ ...offerForm, leadId: e.target.value })} />
          <input aria-label="空间 id" placeholder="空间 id" value={offerForm.spaceId} onChange={(e) => setOfferForm({ ...offerForm, spaceId: e.target.value })} />
          <input aria-label="拟商户 id" placeholder="拟商户 id" value={offerForm.merchantId} onChange={(e) => setOfferForm({ ...offerForm, merchantId: e.target.value })} />
          <input aria-label="月租金（元）" type="number" min="0" placeholder="月租金（元）" value={offerForm.baseRentYuan} onChange={(e) => setOfferForm({ ...offerForm, baseRentYuan: e.target.value })} />
          <PermissionButton permissionsState={effectivePermissions} permissions={OFFER_MANAGE}
            data-testid="leasing-offer-create" onClick={() => void createOffer()}>新建报价</PermissionButton>
        </div>
        <div className="admin-inline-form" data-testid="leasing-convert-form">
          <input aria-label="合同主体 id" placeholder="合同主体 id（转合同用）" value={convertSubjects.contractSubjectId} onChange={(e) => setConvertSubjects({ ...convertSubjects, contractSubjectId: e.target.value })} />
          <input aria-label="结算主体 id" placeholder="结算主体 id（转合同用）" value={convertSubjects.settlementSubjectId} onChange={(e) => setConvertSubjects({ ...convertSubjects, settlementSubjectId: e.target.value })} />
        </div>
      </AdminDataState>
    </section>
  );
}
