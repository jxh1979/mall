import type { FormInstance } from "antd";
import { postJson } from "./apiClient";
import type { Area, LocationPoint, Merchant, Organization, Snapshot, UserAccount, Venue } from "./types";

type RunAction = <T>(label: string, action: () => Promise<T>, reset?: () => void) => Promise<T>;

type FoundationActionDependencies = {
  foundationForm: FormInstance;
  runAction: RunAction;
  snapshot: Snapshot | null;
  userForm: FormInstance;
};

export type FoundationActions = ReturnType<typeof createFoundationActions>;

export function createFoundationActions({ foundationForm, runAction, snapshot, userForm }: FoundationActionDependencies) {
  async function handleFoundationFlow(values: Record<string, string>) {
    await runAction("基础资料创建", async () => {
      const organization = await postJson<Organization>("/api/v1/platform/business-entities", {
        orgType: "platform",
        name: values.organizationName,
        shortName: values.organizationName,
        code: `org-${Date.now()}`,
      });
      const merchant = await postJson<Merchant>("/api/v1/platform/merchants", {
        organizationId: organization.id,
        merchantType: "platform",
        companyName: `${values.merchantName} Company`,
        displayName: values.merchantName,
        contactName: values.contactName || "P0 Operator",
        contactPhone: values.contactPhone || "18800000002",
        status: "active",
      });
      const venue = await postJson<Venue>("/api/v1/platform/projects", {
        merchantId: merchant.id,
        venueType: "complex",
        name: values.venueName,
        code: `venue-${Date.now()}`,
        floor: "1F",
        capacity: 300,
        status: "active",
      });
      const area = await postJson<Area>("/api/v1/platform/spaces", {
        venueId: venue.id,
        name: values.areaName,
        areaType: "operation",
        floor: "1F",
        capacity: 100,
      });
      const point = await postJson<LocationPoint>("/api/v1/platform/spaces", {
        areaId: area.id,
        venueId: venue.id,
        merchantId: merchant.id,
        pointType: "gate",
        name: values.pointName,
        code: `point-${Date.now()}`,
        capacity: 60,
      });
      return { organization, merchant, venue, area, point };
    }, () => foundationForm.resetFields());
  }

  async function handleCreateUser(values: Record<string, string>) {
    await runAction("账号创建", async () => {
      const organizationId = values.organizationId || snapshot?.organizations[0]?.id;
      const merchantId = values.merchantId || snapshot?.merchantsRaw[0]?.id;
      return postJson<UserAccount>("/api/v1/access/users", {
        username: values.username,
        password: values.password,
        phone: values.phone,
        accountType: values.accountType || "staff",
        organizationId,
        merchantId,
        status: "active",
      });
    }, () => userForm.resetFields());
  }

  return {
    handleCreateUser,
    handleFoundationFlow,
  };
}
