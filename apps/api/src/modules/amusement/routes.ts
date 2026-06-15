import { randomBytes } from "node:crypto";
import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { prisma } from "../../lib/prisma.js";
import { requireCurrentUser, scopedMerchantId } from "../../lib/security.js";
import { writeAuditEvent } from "../events/service.js";

const projectSchema = z.object({
  projectName: z.string().min(1),
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  capacity: z.number().int().nonnegative().default(0),
  description: z.string().optional(),
});

const ruleSchema = z.object({
  ruleType: z.string().min(1),
  ruleValue: z.string().min(1),
});

const sessionSchema = z.object({
  sessionName: z.string().min(1),
  startsAt: z.string().datetime(),
  endsAt: z.string().datetime(),
  capacity: z.number().int().positive(),
});

const verifySchema = z.object({
  entitlementCode: z.string().optional(),
  staffId: z.string().optional(),
  manualReason: z.string().optional(),
});

const reservationSchema = z.object({
  customerName: z.string().min(1),
  customerPhone: z.string().optional(),
  partySize: z.number().int().positive().default(1),
  reservedCapacity: z.number().int().positive().default(1),
});

const queueEntrySchema = z.object({
  customerName: z.string().optional(),
  customerPhone: z.string().optional(),
  partySize: z.number().int().positive().default(1),
});

const deviceStatusSchema = z.object({
  projectId: z.string().min(1),
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  deviceName: z.string().min(1),
  deviceCode: z.string().optional(),
  status: z.string().default("normal"),
  statusReason: z.string().optional(),
});

const deviceStatusChangeSchema = z.object({
  status: z.string().min(1),
  statusReason: z.string().optional(),
});

const inspectionTemplateSchema = z.object({
  projectId: z.string().optional(),
  templateName: z.string().min(1),
  checklist: z.array(z.string().min(1)).default([]),
});

const inspectionRecordSchema = z.object({
  templateId: z.string().optional(),
  projectId: z.string().min(1),
  sessionId: z.string().optional(),
  result: z.string().default("normal"),
  abnormalItems: z.array(z.string()).default([]),
  remark: z.string().optional(),
});

const faultSchema = z.object({
  projectId: z.string().min(1),
  deviceId: z.string().optional(),
  faultLevel: z.string().default("normal"),
  summary: z.string().min(1),
});

const resolveFaultSchema = z.object({
  resolution: z.string().min(1),
});

function code(prefix: string) {
  return `${prefix}_${randomBytes(6).toString("hex")}`;
}

async function createAmusementRiskAlert(objectId: string, summary: string) {
  return prisma.riskAlert.create({
    data: {
      alertType: "amusement_verification_abnormal",
      riskLevel: "suspicious",
      objectType: "amusement_session",
      objectId,
      summary,
    },
  });
}

async function assertSessionCapacity(sessionId: string, requestedCapacity: number) {
  const session = await prisma.amusementSession.findUnique({ where: { id: sessionId }, include: { project: true } });
  if (!session) throw new Error("SESSION_NOT_FOUND");
  const reserved = await prisma.amusementReservation.aggregate({
    where: { sessionId, status: { in: ["reserved", "queued", "called", "admitted"] } },
    _sum: { reservedCapacity: true },
  });
  const reservedCapacity = reserved._sum.reservedCapacity ?? 0;
  if (reservedCapacity + requestedCapacity > session.capacity) throw new Error("RESERVATION_CAPACITY_FULL");
  return session;
}

export async function registerAmusementRoutes(app: FastifyInstance) {
  app.get("/api/admin/amusement/projects", async () =>
    prisma.amusementProject.findMany({ orderBy: { createdAt: "desc" }, include: { rules: true, sessions: true } }),
  );

  app.post("/api/admin/amusement/projects", async (request, reply) => {
    const input = projectSchema.parse(request.body);
    const project = await prisma.amusementProject.create({
      data: { ...input, projectCode: code("amu_project") },
    });
    await writeAuditEvent({
      eventType: "amusement.project.created",
      eventCategory: "amusement",
      sourceType: "internal",
      merchantId: project.merchantId ?? undefined,
      summary: `创建游乐项目：${project.projectName}`,
    });
    return reply.code(201).send(project);
  });

  app.post("/api/admin/amusement/projects/:id/rules", async (request, reply) => {
    const params = request.params as { id: string };
    const input = ruleSchema.parse(request.body);
    const rule = await prisma.amusementProjectRule.create({ data: { projectId: params.id, ...input } });
    return reply.code(201).send(rule);
  });

  app.get("/api/admin/amusement/sessions", async () =>
    prisma.amusementSession.findMany({ orderBy: { startsAt: "asc" }, include: { project: true } }),
  );

  app.post("/api/admin/amusement/projects/:id/sessions", async (request, reply) => {
    const params = request.params as { id: string };
    const input = sessionSchema.parse(request.body);
    const session = await prisma.amusementSession.create({
      data: {
        projectId: params.id,
        sessionName: input.sessionName,
        startsAt: new Date(input.startsAt),
        endsAt: new Date(input.endsAt),
        capacity: input.capacity,
        status: "scheduled",
      },
    });
    await writeAuditEvent({
      eventType: "amusement.session.created",
      eventCategory: "amusement",
      sourceType: "internal",
      summary: `创建游乐场次：${session.sessionName}`,
      payload: { projectId: params.id, capacity: session.capacity },
    });
    return reply.code(201).send(session);
  });

  app.get("/api/admin/amusement/reservations", async (request) => {
    const query = request.query as { merchantId?: string; sessionId?: string; status?: string };
    const merchantId = scopedMerchantId(request, query.merchantId);
    return prisma.amusementReservation.findMany({
      where: {
        ...(merchantId ? { merchantId } : {}),
        ...(query.sessionId ? { sessionId: query.sessionId } : {}),
        ...(query.status ? { status: query.status } : {}),
      },
      orderBy: { createdAt: "desc" },
      take: 200,
    });
  });

  app.post("/api/admin/amusement/sessions/:id/reservations", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const params = request.params as { id: string };
    const input = reservationSchema.parse(request.body);
    const session = await assertSessionCapacity(params.id, input.reservedCapacity);
    scopedMerchantId(request, session.project.merchantId);
    const result = await prisma.$transaction(async (tx) => {
      const reservation = await tx.amusementReservation.create({
        data: {
          reservationNo: code("amu_res"),
          projectId: session.projectId,
          sessionId: session.id,
          merchantId: session.project.merchantId,
          venueId: session.project.venueId,
          customerName: input.customerName,
          customerPhone: input.customerPhone,
          partySize: input.partySize,
          reservedCapacity: input.reservedCapacity,
          status: "queued",
        },
      });
      const queue = await tx.amusementQueueEntry.create({
        data: {
          queueNo: code("amu_queue"),
          projectId: session.projectId,
          sessionId: session.id,
          reservationId: reservation.id,
          merchantId: session.project.merchantId,
          venueId: session.project.venueId,
          customerName: input.customerName,
          customerPhone: input.customerPhone,
          partySize: input.partySize,
          status: "waiting",
        },
      });
      const updated = await tx.amusementReservation.update({ where: { id: reservation.id }, data: { queueEntryId: queue.id } });
      return { reservation: updated, queue };
    });
    await writeAuditEvent({
      eventType: "amusement.reservation.created",
      eventCategory: "amusement",
      sourceType: "internal",
      merchantId: session.project.merchantId ?? undefined,
      actorId: actor.id,
      summary: `游乐预约：${input.customerName}`,
      payload: { reservationId: result.reservation.id, queueNo: result.queue.queueNo, sessionId: session.id },
    });
    return reply.code(201).send(result);
  });

  app.get("/api/admin/amusement/queue-entries", async (request) => {
    const query = request.query as { merchantId?: string; sessionId?: string; status?: string };
    const merchantId = scopedMerchantId(request, query.merchantId);
    return prisma.amusementQueueEntry.findMany({
      where: {
        ...(merchantId ? { merchantId } : {}),
        ...(query.sessionId ? { sessionId: query.sessionId } : {}),
        ...(query.status ? { status: query.status } : {}),
      },
      orderBy: { createdAt: "asc" },
      take: 200,
    });
  });

  app.post("/api/admin/amusement/sessions/:id/queue-entries", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const params = request.params as { id: string };
    const input = queueEntrySchema.parse(request.body);
    const session = await prisma.amusementSession.findUnique({ where: { id: params.id }, include: { project: true } });
    if (!session) return reply.code(404).send({ error: "SESSION_NOT_FOUND" });
    scopedMerchantId(request, session.project.merchantId);
    const queue = await prisma.amusementQueueEntry.create({
      data: {
        queueNo: code("amu_queue"),
        projectId: session.projectId,
        sessionId: session.id,
        merchantId: session.project.merchantId,
        venueId: session.project.venueId,
        customerName: input.customerName,
        customerPhone: input.customerPhone,
        partySize: input.partySize,
      },
    });
    await writeAuditEvent({
      eventType: "amusement.queue.created",
      eventCategory: "amusement",
      sourceType: "internal",
      merchantId: queue.merchantId ?? undefined,
      actorId: actor.id,
      summary: `游乐排队取号：${queue.queueNo}`,
      payload: { queueEntryId: queue.id, sessionId: session.id },
    });
    return reply.code(201).send(queue);
  });

  app.post("/api/admin/amusement/queue-entries/:id/call", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const params = request.params as { id: string };
    const queue = await prisma.amusementQueueEntry.findUnique({ where: { id: params.id } });
    if (!queue) return reply.code(404).send({ error: "QUEUE_ENTRY_NOT_FOUND" });
    scopedMerchantId(request, queue.merchantId);
    const updated = await prisma.amusementQueueEntry.update({ where: { id: params.id }, data: { status: "called", calledAt: new Date() } });
    if (updated.reservationId) await prisma.amusementReservation.update({ where: { id: updated.reservationId }, data: { status: "called" } });
    await writeAuditEvent({
      eventType: "amusement.queue.called",
      eventCategory: "amusement",
      sourceType: "internal",
      merchantId: updated.merchantId ?? undefined,
      actorId: actor.id,
      summary: `游乐叫号：${updated.queueNo}`,
    });
    return reply.code(201).send(updated);
  });

  app.post("/api/admin/amusement/queue-entries/:id/pass", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const params = request.params as { id: string };
    const queue = await prisma.amusementQueueEntry.findUnique({ where: { id: params.id } });
    if (!queue) return reply.code(404).send({ error: "QUEUE_ENTRY_NOT_FOUND" });
    scopedMerchantId(request, queue.merchantId);
    const updated = await prisma.amusementQueueEntry.update({ where: { id: params.id }, data: { status: "passed", passedAt: new Date() } });
    if (updated.reservationId) await prisma.amusementReservation.update({ where: { id: updated.reservationId }, data: { status: "passed" } });
    await writeAuditEvent({
      eventType: "amusement.queue.passed",
      eventCategory: "amusement",
      sourceType: "internal",
      merchantId: updated.merchantId ?? undefined,
      actorId: actor.id,
      summary: `游乐过号：${updated.queueNo}`,
    });
    return reply.code(201).send(updated);
  });

  app.post("/api/admin/amusement/queue-entries/:id/admit", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const params = request.params as { id: string };
    const queue = await prisma.amusementQueueEntry.findUnique({ where: { id: params.id } });
    if (!queue) return reply.code(404).send({ error: "QUEUE_ENTRY_NOT_FOUND" });
    scopedMerchantId(request, queue.merchantId);
    const session = await prisma.amusementSession.findUnique({ where: { id: queue.sessionId } });
    if (!session) return reply.code(404).send({ error: "SESSION_NOT_FOUND" });
    if (session.usedCapacity + queue.partySize > session.capacity) return reply.code(400).send({ error: "SESSION_FULL" });
    const updated = await prisma.$transaction(async (tx) => {
      const sessionUpdated = await tx.amusementSession.updateMany({
        where: { id: session.id, usedCapacity: { lte: session.capacity - queue.partySize } },
        data: { usedCapacity: { increment: queue.partySize } },
      });
      if (sessionUpdated.count !== 1) throw new Error("SESSION_CAPACITY_CHANGED");
      const admitted = await tx.amusementQueueEntry.update({
        where: { id: params.id },
        data: { status: "admitted", admittedAt: new Date() },
      });
      if (admitted.reservationId) await tx.amusementReservation.update({ where: { id: admitted.reservationId }, data: { status: "admitted" } });
      return admitted;
    });
    await writeAuditEvent({
      eventType: "amusement.queue.admitted",
      eventCategory: "amusement",
      sourceType: "internal",
      merchantId: updated.merchantId ?? undefined,
      actorId: actor.id,
      summary: `游乐入场：${updated.queueNo}`,
      payload: { partySize: updated.partySize, sessionId: updated.sessionId },
    });
    return reply.code(201).send(updated);
  });

  app.get("/api/admin/amusement/device-statuses", async (request) => {
    const query = request.query as { merchantId?: string; projectId?: string; status?: string };
    const merchantId = scopedMerchantId(request, query.merchantId);
    return prisma.amusementDeviceStatus.findMany({
      where: {
        ...(merchantId ? { merchantId } : {}),
        ...(query.projectId ? { projectId: query.projectId } : {}),
        ...(query.status ? { status: query.status } : {}),
      },
      orderBy: { updatedAt: "desc" },
      take: 200,
    });
  });

  app.post("/api/admin/amusement/device-statuses", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const input = deviceStatusSchema.parse(request.body);
    const project = await prisma.amusementProject.findUnique({ where: { id: input.projectId } });
    if (!project) return reply.code(404).send({ error: "PROJECT_NOT_FOUND" });
    const merchantId = scopedMerchantId(request, input.merchantId ?? project.merchantId);
    const device = await prisma.amusementDeviceStatus.create({
      data: {
        projectId: input.projectId,
        merchantId,
        venueId: input.venueId ?? project.venueId,
        deviceName: input.deviceName,
        deviceCode: input.deviceCode ?? code("amu_device"),
        status: input.status,
        statusReason: input.statusReason,
        lastChangedBy: actor.id,
      },
    });
    await writeAuditEvent({
      eventType: "amusement.device.created",
      eventCategory: "amusement",
      sourceType: "internal",
      merchantId: device.merchantId ?? undefined,
      actorId: actor.id,
      summary: `游乐设备登记：${device.deviceName}`,
      payload: { deviceId: device.id, status: device.status },
    });
    return reply.code(201).send(device);
  });

  app.post("/api/admin/amusement/device-statuses/:id/change", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const params = request.params as { id: string };
    const input = deviceStatusChangeSchema.parse(request.body);
    const existing = await prisma.amusementDeviceStatus.findUnique({ where: { id: params.id } });
    if (!existing) return reply.code(404).send({ error: "DEVICE_STATUS_NOT_FOUND" });
    scopedMerchantId(request, existing.merchantId);
    const updated = await prisma.amusementDeviceStatus.update({
      where: { id: params.id },
      data: { status: input.status, statusReason: input.statusReason, lastChangedBy: actor.id },
    });
    await writeAuditEvent({
      eventType: "amusement.device.status_changed",
      eventCategory: "amusement",
      sourceType: "internal",
      merchantId: updated.merchantId ?? undefined,
      actorId: actor.id,
      summary: `游乐设备状态变更：${updated.deviceName} -> ${updated.status}`,
      payload: { deviceId: updated.id, reason: input.statusReason },
    });
    return reply.code(201).send(updated);
  });

  app.get("/api/admin/amusement/inspection-templates", async () =>
    prisma.amusementInspectionTemplate.findMany({ orderBy: { createdAt: "desc" }, take: 200 }),
  );

  app.post("/api/admin/amusement/inspection-templates", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const input = inspectionTemplateSchema.parse(request.body);
    const template = await prisma.amusementInspectionTemplate.create({
      data: {
        projectId: input.projectId,
        templateName: input.templateName,
        checklist: JSON.stringify(input.checklist),
      },
    });
    await writeAuditEvent({
      eventType: "amusement.inspection.template_created",
      eventCategory: "amusement",
      sourceType: "internal",
      actorId: actor.id,
      summary: `创建巡检模板：${template.templateName}`,
      payload: { templateId: template.id },
    });
    return reply.code(201).send(template);
  });

  app.get("/api/admin/amusement/inspection-records", async (request) => {
    const query = request.query as { projectId?: string; result?: string };
    return prisma.amusementInspectionRecord.findMany({
      where: { ...(query.projectId ? { projectId: query.projectId } : {}), ...(query.result ? { result: query.result } : {}) },
      orderBy: { createdAt: "desc" },
      take: 200,
    });
  });

  app.post("/api/admin/amusement/inspection-records", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const input = inspectionRecordSchema.parse(request.body);
    const project = await prisma.amusementProject.findUnique({ where: { id: input.projectId } });
    if (!project) return reply.code(404).send({ error: "PROJECT_NOT_FOUND" });
    scopedMerchantId(request, project.merchantId);
    const record = await prisma.$transaction(async (tx) => {
      const created = await tx.amusementInspectionRecord.create({
        data: {
          templateId: input.templateId,
          projectId: input.projectId,
          sessionId: input.sessionId,
          inspectorId: actor.id,
          result: input.result,
          abnormalItems: input.abnormalItems.length ? JSON.stringify(input.abnormalItems) : undefined,
          remark: input.remark,
        },
      });
      if (input.result !== "normal") {
        await tx.amusementFaultRecord.create({
          data: {
            faultNo: code("amu_fault"),
            projectId: input.projectId,
            faultLevel: input.result === "critical" ? "high" : "normal",
            summary: input.remark ?? `巡检异常：${input.abnormalItems.join(",")}`,
            reportedBy: actor.id,
          },
        });
      }
      return created;
    });
    await writeAuditEvent({
      eventType: "amusement.inspection.submitted",
      eventCategory: "amusement",
      sourceType: "internal",
      merchantId: project.merchantId ?? undefined,
      actorId: actor.id,
      summary: `提交游乐巡检：${record.result}`,
      payload: { inspectionRecordId: record.id, abnormalItems: input.abnormalItems },
    });
    return reply.code(201).send(record);
  });

  app.get("/api/admin/amusement/faults", async (request) => {
    const query = request.query as { projectId?: string; status?: string };
    return prisma.amusementFaultRecord.findMany({
      where: { ...(query.projectId ? { projectId: query.projectId } : {}), ...(query.status ? { status: query.status } : {}) },
      orderBy: { createdAt: "desc" },
      take: 200,
    });
  });

  app.post("/api/admin/amusement/faults", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const input = faultSchema.parse(request.body);
    const project = await prisma.amusementProject.findUnique({ where: { id: input.projectId } });
    if (!project) return reply.code(404).send({ error: "PROJECT_NOT_FOUND" });
    scopedMerchantId(request, project.merchantId);
    const fault = await prisma.amusementFaultRecord.create({
      data: {
        faultNo: code("amu_fault"),
        projectId: input.projectId,
        deviceId: input.deviceId,
        faultLevel: input.faultLevel,
        summary: input.summary,
        reportedBy: actor.id,
      },
    });
    await writeAuditEvent({
      eventType: "amusement.fault.created",
      eventCategory: "amusement",
      sourceType: "internal",
      merchantId: project.merchantId ?? undefined,
      actorId: actor.id,
      summary: `游乐故障记录：${fault.summary}`,
      payload: { faultId: fault.id, faultLevel: fault.faultLevel },
    });
    return reply.code(201).send(fault);
  });

  app.post("/api/admin/amusement/faults/:id/resolve", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const params = request.params as { id: string };
    const input = resolveFaultSchema.parse(request.body);
    const fault = await prisma.amusementFaultRecord.findUnique({ where: { id: params.id } });
    if (!fault) return reply.code(404).send({ error: "FAULT_NOT_FOUND" });
    const project = await prisma.amusementProject.findUnique({ where: { id: fault.projectId } });
    scopedMerchantId(request, project?.merchantId);
    const updated = await prisma.amusementFaultRecord.update({
      where: { id: params.id },
      data: { status: "resolved", resolvedBy: actor.id, resolvedAt: new Date(), resolution: input.resolution },
    });
    await writeAuditEvent({
      eventType: "amusement.fault.resolved",
      eventCategory: "amusement",
      sourceType: "internal",
      merchantId: project?.merchantId ?? undefined,
      actorId: actor.id,
      summary: `游乐故障处理：${updated.faultNo}`,
      payload: { faultId: updated.id, resolution: input.resolution },
    });
    return reply.code(201).send(updated);
  });

  app.post("/api/admin/amusement/sessions/:id/verify", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const params = request.params as { id: string };
    const input = verifySchema.parse(request.body);
    const session = await prisma.amusementSession.findUnique({ where: { id: params.id }, include: { project: true } });

    if (!session) return reply.code(404).send({ error: "SESSION_NOT_FOUND" });
    if (session.usedCapacity >= session.capacity) {
      const alert = await createAmusementRiskAlert(session.id, `游乐场次容量已满：${session.sessionName}`);
      await writeAuditEvent({
        eventType: "amusement.verification.failed",
        eventCategory: "amusement",
        sourceType: "internal",
        result: "failed",
        riskLevel: "suspicious",
        merchantId: session.project.merchantId ?? undefined,
        summary: `游乐核销失败：场次已满`,
        payload: { sessionId: session.id, riskAlertId: alert.id },
      });
      return reply.code(400).send({ error: "SESSION_FULL" });
    }

    let issuedEntitlementId: string | undefined;
    let issuedEntitlementTotalTimes: number | undefined;
    if (input.entitlementCode) {
      const entitlement = await prisma.issuedEntitlement.findUnique({ where: { entitlementCode: input.entitlementCode } });
      if (!entitlement || entitlement.status !== "active" || entitlement.usedTimes >= entitlement.totalTimes) {
        const alert = await createAmusementRiskAlert(session.id, `游乐权益不可用：${input.entitlementCode}`);
        await writeAuditEvent({
          eventType: "amusement.verification.failed",
          eventCategory: "amusement",
          sourceType: "internal",
          result: "failed",
          riskLevel: "suspicious",
          merchantId: session.project.merchantId ?? undefined,
          summary: `游乐核销失败：权益不可用`,
          payload: { sessionId: session.id, entitlementCode: input.entitlementCode, riskAlertId: alert.id },
        });
        return reply.code(400).send({ error: "ENTITLEMENT_NOT_ALLOWED" });
      }
      issuedEntitlementId = entitlement.id;
      issuedEntitlementTotalTimes = entitlement.totalTimes;
    } else if (!input.manualReason) {
      return reply.code(400).send({ error: "ENTITLEMENT_OR_MANUAL_REASON_REQUIRED" });
    }

    const verification = await prisma.$transaction(async (tx) => {
      const sessionUpdated = await tx.amusementSession.updateMany({
        where: { id: session.id, usedCapacity: { lt: session.capacity } },
        data: { usedCapacity: { increment: 1 } },
      });
      if (sessionUpdated.count !== 1) throw new Error("SESSION_CAPACITY_CHANGED");

      if (issuedEntitlementId) {
        const entitlementUpdated = await tx.issuedEntitlement.updateMany({
          where: {
            id: issuedEntitlementId,
            status: "active",
            usedTimes: { lt: issuedEntitlementTotalTimes ?? 0 },
          },
          data: { usedTimes: { increment: 1 } },
        });
        if (entitlementUpdated.count !== 1) throw new Error("ENTITLEMENT_ALREADY_USED");
        const current = await tx.issuedEntitlement.findUnique({ where: { id: issuedEntitlementId } });
        if (current && current.usedTimes >= current.totalTimes) {
          await tx.issuedEntitlement.update({ where: { id: issuedEntitlementId }, data: { status: "used" } });
        }
      }

      const record = await tx.amusementVerification.create({
        data: {
          verificationNo: code("amu_ver"),
          projectId: session.projectId,
          sessionId: session.id,
          issuedEntitlementId,
          staffId: actor.id,
          status: input.manualReason ? "manual_allowed" : "success",
          resultReason: input.manualReason ?? "OK",
        },
      });
      return record;
    });

    await writeAuditEvent({
      eventType: input.manualReason ? "amusement.manual_allowed" : "amusement.verification.succeeded",
      eventCategory: "amusement",
      sourceType: "internal",
      merchantId: session.project.merchantId ?? undefined,
      actorType: "staff",
      actorId: actor.id,
      entitlementId: issuedEntitlementId,
      summary: `游乐核销：${session.project.projectName} / ${session.sessionName}`,
      payload: { verificationNo: verification.verificationNo, manualReason: input.manualReason },
    });

    return reply.code(201).send(verification);
  });

  app.get("/api/admin/amusement/verifications", async () =>
    prisma.amusementVerification.findMany({ orderBy: { createdAt: "desc" }, take: 200 }),
  );
}
