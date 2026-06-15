import { randomBytes } from "node:crypto";
import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { prisma } from "../../lib/prisma.js";
import { assertMerchantAccess, requireCurrentUser, scopedMerchantId } from "../../lib/security.js";
import { writeAuditEvent } from "../events/service.js";

const bindDeviceSchema = z.object({
  deviceCode: z.string().min(1),
  deviceType: z.string().default("android_handheld"),
  imei: z.string().optional(),
  serialNo: z.string().optional(),
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  areaId: z.string().optional(),
  locationPointId: z.string().optional(),
  staffId: z.string().optional(),
});

const syncTaskSchema = z.object({
  clientTaskId: z.string().min(1),
  taskType: z.enum(["offline_verification", "manual_supplement", "device_event"]),
  occurredAt: z.string().optional(),
  payload: z.record(z.unknown()).default({}),
});

const syncTasksSchema = z.object({
  deviceCode: z.string().min(1),
  staffId: z.string().optional(),
  tasks: z.array(syncTaskSchema).min(1),
});

const returnDeviceSchema = z.object({
  deviceCode: z.string().min(1),
  staffId: z.string().optional(),
  hasPendingTasks: z.boolean().default(false),
  reason: z.string().optional(),
});

const eventBatchSchema = z.object({
  deviceCode: z.string().min(1),
  staffId: z.string().optional(),
  events: z
    .array(
      z.object({
        eventType: z.string().min(1),
        eventCategory: z.string().default("android_terminal"),
        result: z.string().default("success"),
        summary: z.string().min(1),
        payload: z.record(z.unknown()).optional(),
        occurredAt: z.string().optional(),
      }),
    )
    .min(1),
});

function code(prefix: string) {
  return `${prefix}_${randomBytes(6).toString("hex")}`;
}

function stringPayloadValue(payload: Record<string, unknown>, key: string) {
  const value = payload[key];
  return typeof value === "string" && value.length > 0 ? value : undefined;
}

async function createTerminalRiskAlert(objectId: string, reason: string, deviceId?: string) {
  const rule = await prisma.riskRule.findFirst({
    where: { ruleCode: "P0_VERIFICATION_FAIL", status: "active" },
  });

  return prisma.riskAlert.create({
    data: {
      ruleId: rule?.id,
      alertType: "terminal_sync_conflict",
      riskLevel: "suspicious",
      objectType: "entitlement",
      objectId,
      summary: `终端同步异常：${objectId} / ${reason}`,
    },
  });
}

async function processOfflineVerification(input: {
  clientTaskId: string;
  payload: Record<string, unknown>;
  staffId?: string;
  deviceId?: string;
}) {
  const entitlementCode = stringPayloadValue(input.payload, "entitlementCode");
  if (!entitlementCode) {
    return { clientTaskId: input.clientTaskId, status: "rejected", reason: "MISSING_ENTITLEMENT_CODE" };
  }

  const entitlement = await prisma.issuedEntitlement.findUnique({ where: { entitlementCode } });
  const now = new Date();
  let reason = "OK";

  if (!entitlement) reason = "ENTITLEMENT_NOT_FOUND";
  else if (entitlement.status !== "active") reason = "ENTITLEMENT_NOT_ACTIVE";
  else if (entitlement.validFrom && entitlement.validFrom > now) reason = "NOT_STARTED";
  else if (entitlement.validTo && entitlement.validTo < now) reason = "EXPIRED";
  else if (entitlement.usedTimes >= entitlement.totalTimes) reason = "NO_REMAINING_TIMES";

  if (!entitlement || reason !== "OK") {
    const alert = await createTerminalRiskAlert(entitlementCode, reason, input.deviceId);
    await writeAuditEvent({
      eventType: "terminal.offline_verification.conflict",
      eventCategory: "android_terminal",
      sourceType: "android_terminal",
      actorType: input.staffId ? "staff" : "device",
      actorId: input.staffId,
      deviceId: input.deviceId,
      result: "failed",
      riskLevel: "suspicious",
      summary: `离线核销同步冲突：${entitlementCode}`,
      payload: { clientTaskId: input.clientTaskId, reason, riskAlertId: alert.id },
    });
    return { clientTaskId: input.clientTaskId, status: "conflict", reason, riskAlertId: alert.id };
  }

  const verificationNo = code("term_ver");

  const verification = await prisma.$transaction(async (tx) => {
    const updated = await tx.issuedEntitlement.updateMany({
      where: {
        id: entitlement.id,
        status: "active",
        usedTimes: { lt: entitlement.totalTimes },
      },
      data: {
        usedTimes: { increment: 1 },
      },
    });
    if (updated.count !== 1) {
      throw new Error("ENTITLEMENT_ALREADY_USED");
    }

    const current = await tx.issuedEntitlement.findUnique({ where: { id: entitlement.id } });
    if (current && current.usedTimes >= current.totalTimes) {
      await tx.issuedEntitlement.update({ where: { id: entitlement.id }, data: { status: "used" } });
    }

    const record = await tx.verificationRecord.create({
      data: {
        verificationNo,
        issuedEntitlementId: entitlement.id,
        merchantId: entitlement.merchantId,
        venueId: entitlement.venueId,
        projectId: entitlement.projectId,
        staffId: input.staffId,
        deviceId: input.deviceId,
        source: `android_offline_sync:${input.clientTaskId}`,
        status: "success",
        resultReason: "OFFLINE_SYNC_ACCEPTED",
      },
    });

    await tx.settlementItem.create({
      data: {
        verificationRecordId: record.id,
        merchantId: entitlement.merchantId,
        amount: entitlement.valueAmount,
        status: "pending",
      },
    });

    return record;
  });

  await writeAuditEvent({
    eventType: "terminal.offline_verification.synced",
    eventCategory: "android_terminal",
    sourceType: "android_terminal",
    actorType: input.staffId ? "staff" : "device",
    actorId: input.staffId,
    merchantId: entitlement.merchantId ?? undefined,
    ticketId: entitlement.issuedTicketId,
    entitlementId: entitlement.id,
    deviceId: input.deviceId,
    result: "success",
    summary: `离线核销同步成功：${entitlementCode}`,
    payload: { clientTaskId: input.clientTaskId, verificationNo },
  });

  return {
    clientTaskId: input.clientTaskId,
    status: "accepted",
    verificationId: verification.id,
    verificationNo,
  };
}

export async function registerTerminalRoutes(app: FastifyInstance) {
  app.post("/api/terminal/devices/bind", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const input = bindDeviceSchema.parse(request.body);
    const merchantId = scopedMerchantId(request, input.merchantId);
    const device = await prisma.device.upsert({
      where: { deviceCode: input.deviceCode },
      update: {
        deviceType: input.deviceType,
        imei: input.imei,
        serialNo: input.serialNo,
        merchantId,
        venueId: input.venueId,
        areaId: input.areaId,
        locationPointId: input.locationPointId,
        status: "bound",
      },
      create: {
        deviceCode: input.deviceCode,
        deviceType: input.deviceType,
        imei: input.imei,
        serialNo: input.serialNo,
        merchantId,
        venueId: input.venueId,
        areaId: input.areaId,
        locationPointId: input.locationPointId,
        status: "bound",
      },
    });

    await writeAuditEvent({
      eventType: "terminal.device.bound",
      eventCategory: "android_terminal",
      sourceType: "android_terminal",
      actorType: "staff",
      actorId: actor.id,
      merchantId,
      deviceId: device.id,
      summary: `终端绑定设备：${input.deviceCode}`,
      payload: { locationPointId: input.locationPointId, venueId: input.venueId, areaId: input.areaId },
    });

    return reply.code(201).send(device);
  });

  app.get("/api/terminal/devices/:deviceCode", async (request, reply) => {
    const params = request.params as { deviceCode: string };
    const device = await prisma.device.findUnique({ where: { deviceCode: params.deviceCode } });
    if (!device) return reply.code(404).send({ error: "DEVICE_NOT_FOUND" });
    assertMerchantAccess(request, device.merchantId);
    return device;
  });

  app.get("/api/terminal/offline-entitlements", async (request) => {
    const query = request.query as { merchantId?: string; venueId?: string; limit?: string };
    const merchantId = scopedMerchantId(request, query.merchantId);
    const limit = Math.min(Number(query.limit ?? 200), 500);
    const entitlements = await prisma.issuedEntitlement.findMany({
      where: {
        merchantId,
        venueId: query.venueId,
        status: "active",
      },
      orderBy: { createdAt: "desc" },
      take: limit,
      select: {
        id: true,
        entitlementCode: true,
        entitlementType: true,
        merchantId: true,
        venueId: true,
        projectId: true,
        totalTimes: true,
        usedTimes: true,
        valueAmount: true,
        status: true,
        validFrom: true,
        validTo: true,
        updatedAt: true,
      },
    });

    return {
      generatedAt: new Date().toISOString(),
      expiresAt: new Date(Date.now() + 4 * 60 * 60 * 1000).toISOString(),
      entitlements,
    };
  });

  app.post("/api/terminal/sync-tasks", async (request) => {
    const actor = requireCurrentUser(request);
    const input = syncTasksSchema.parse(request.body);
    const device = await prisma.device.findUnique({ where: { deviceCode: input.deviceCode } });
    if (!device) return { syncedAt: new Date().toISOString(), results: input.tasks.map((task) => ({ clientTaskId: task.clientTaskId, status: "rejected", reason: "DEVICE_NOT_FOUND" })) };
    assertMerchantAccess(request, device.merchantId);
    const results = [];

    for (const task of input.tasks) {
      if (task.taskType === "offline_verification") {
        try {
          results.push(await processOfflineVerification({
            clientTaskId: task.clientTaskId,
            payload: task.payload,
            staffId: actor.id,
            deviceId: device.id,
          }));
        } catch (error) {
          results.push({
            clientTaskId: task.clientTaskId,
            status: "conflict",
            reason: error instanceof Error ? error.message : "SYNC_REJECTED",
          });
        }
        continue;
      }

      await writeAuditEvent({
        eventType: task.taskType === "manual_supplement" ? "terminal.manual_supplement.synced" : "terminal.event.synced",
        eventCategory: "android_terminal",
        sourceType: "android_terminal",
        actorType: "staff",
        actorId: actor.id,
        merchantId: device.merchantId ?? undefined,
        deviceId: device.id,
        summary:
          task.taskType === "manual_supplement"
            ? `人工补录同步：${task.clientTaskId}`
            : `终端事件同步：${task.clientTaskId}`,
        payload: { clientTaskId: task.clientTaskId, taskType: task.taskType, payload: task.payload },
      });
      results.push({ clientTaskId: task.clientTaskId, status: "accepted" });
    }

    return { syncedAt: new Date().toISOString(), results };
  });

  app.post("/api/terminal/device-return", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const input = returnDeviceSchema.parse(request.body);
    const device = await prisma.device.findUnique({ where: { deviceCode: input.deviceCode } });
    if (!device) return reply.code(404).send({ error: "DEVICE_NOT_FOUND" });
    assertMerchantAccess(request, device.merchantId);

    if (input.hasPendingTasks) {
      await writeAuditEvent({
        eventType: "terminal.device.return_blocked",
        eventCategory: "android_terminal",
        sourceType: "android_terminal",
        actorType: "staff",
        actorId: actor.id,
        merchantId: device.merchantId ?? undefined,
        deviceId: device.id,
        result: "failed",
        riskLevel: "suspicious",
        summary: `终端归还被拦截：${input.deviceCode}`,
        payload: { reason: input.reason ?? "PENDING_TASKS" },
      });
      return reply.code(409).send({ error: "RETURN_BLOCKED_PENDING_TASKS" });
    }

    const returned = await prisma.device.update({
      where: { id: device.id },
      data: { status: "idle" },
    });
    await writeAuditEvent({
      eventType: "terminal.device.returned",
      eventCategory: "android_terminal",
      sourceType: "android_terminal",
      actorType: "staff",
      actorId: actor.id,
      merchantId: returned.merchantId ?? undefined,
      deviceId: returned.id,
      summary: `终端归还完成：${input.deviceCode}`,
      payload: { reason: input.reason },
    });
    return returned;
  });

  app.post("/api/terminal/events/batch", async (request) => {
    const actor = requireCurrentUser(request);
    const input = eventBatchSchema.parse(request.body);
    const device = await prisma.device.findUnique({ where: { deviceCode: input.deviceCode } });
    if (!device) return { accepted: 0, receivedAt: new Date().toISOString(), error: "DEVICE_NOT_FOUND" };
    assertMerchantAccess(request, device.merchantId);
    const events = [];

    for (const event of input.events) {
      events.push(
        await writeAuditEvent({
          eventType: event.eventType,
          eventCategory: event.eventCategory,
          sourceType: "android_terminal",
          actorType: "staff",
          actorId: actor.id,
          merchantId: device.merchantId ?? undefined,
          deviceId: device.id,
          result: event.result,
          summary: event.summary,
          payload: event.payload,
        }),
      );
    }

    return { accepted: events.length, receivedAt: new Date().toISOString() };
  });
}
