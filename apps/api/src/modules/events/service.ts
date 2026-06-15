import { prisma } from "../../lib/prisma.js";

type AuditEventInput = {
  eventType: string;
  eventCategory: string;
  sourceType: string;
  sourceSystem?: string;
  actorType?: string;
  actorId?: string;
  openAppId?: string;
  merchantId?: string;
  venueId?: string;
  areaId?: string;
  locationPointId?: string;
  ticketId?: string;
  entitlementId?: string;
  deviceId?: string;
  robotTaskId?: string;
  conversationId?: string;
  videoEvidenceId?: string;
  result?: string;
  riskLevel?: string;
  summary: string;
  payload?: unknown;
  traceId?: string;
};

export async function writeAuditEvent(input: AuditEventInput) {
  return prisma.auditEvent.create({
    data: {
      eventType: input.eventType,
      eventCategory: input.eventCategory,
      occurredAt: new Date(),
      receivedAt: new Date(),
      sourceType: input.sourceType,
      sourceSystem: input.sourceSystem ?? "api",
      actorType: input.actorType ?? "system",
      actorId: input.actorId,
      openAppId: input.openAppId,
      merchantId: input.merchantId,
      venueId: input.venueId,
      areaId: input.areaId,
      locationPointId: input.locationPointId,
      ticketId: input.ticketId,
      entitlementId: input.entitlementId,
      deviceId: input.deviceId,
      robotTaskId: input.robotTaskId,
      conversationId: input.conversationId,
      videoEvidenceId: input.videoEvidenceId,
      result: input.result ?? "success",
      riskLevel: input.riskLevel ?? "normal",
      summary: input.summary,
      payload: input.payload === undefined ? undefined : JSON.stringify(input.payload),
      traceId: input.traceId,
    },
  });
}
