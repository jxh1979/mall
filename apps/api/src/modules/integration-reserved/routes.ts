import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { prisma } from "../../lib/prisma.js";
import { requireCurrentUser, scopedMerchantId } from "../../lib/security.js";
import { writeAuditEvent } from "../events/service.js";

const reservedInterfaces = [
  {
    code: "catering.order.sync",
    name: "餐饮订单同步接口",
    method: "POST",
    path: "/api/integration/catering/orders/sync",
    status: "active_p1",
  },
  {
    code: "catering.refund.sync",
    name: "餐饮退款同步接口",
    method: "POST",
    path: "/api/integration/catering/refunds/sync",
    status: "active_p1",
  },
  {
    code: "catering.voucher.verify",
    name: "餐饮券核销同步接口",
    method: "POST",
    path: "/api/integration/catering/vouchers/verify",
    status: "active_p1",
  },
  {
    code: "retail.order.sync",
    name: "零售订单同步接口",
    method: "POST",
    path: "/api/integration/retail/orders/sync",
    status: "active_p1",
  },
  {
    code: "retail.refund.sync",
    name: "零售退款同步接口",
    method: "POST",
    path: "/api/integration/retail/refunds/sync",
    status: "active_p1",
  },
  {
    code: "retail.voucher.verify",
    name: "零售券核销同步接口",
    method: "POST",
    path: "/api/integration/retail/vouchers/verify",
    status: "active_p1",
  },
  {
    code: "integration.reconciliation.batch",
    name: "第三方基础对账批次接口",
    method: "POST",
    path: "/api/integration/reconciliation/batches",
    status: "active_p1",
  },
  {
    code: "robot.task.callback",
    name: "机器人状态回调接口",
    method: "POST",
    path: "/api/integration/robot/tasks/callback",
    status: "active_p2",
  },
  {
    code: "robot.task.create",
    name: "机器人任务创建接口",
    method: "POST",
    path: "/api/integration/robot/tasks",
    status: "active_p2",
  },
  {
    code: "video.evidence.bind",
    name: "视频证据绑定接口",
    method: "POST",
    path: "/api/integration/video/evidences/bind",
    status: "active_p2",
  },
  {
    code: "video.camera.register",
    name: "视频摄像头登记接口",
    method: "POST",
    path: "/api/integration/video/cameras",
    status: "active_p2",
  },
  {
    code: "wecom.binding.create",
    name: "企业微信员工绑定接口",
    method: "POST",
    path: "/api/integration/wecom/bindings",
    status: "active_p2",
  },
  {
    code: "wecom.conversation.sync",
    name: "企业微信会话同步接口",
    method: "POST",
    path: "/api/integration/wecom/conversations/sync",
    status: "active_p2",
  },
  {
    code: "wecom.message.sync",
    name: "企业微信消息同步接口",
    method: "POST",
    path: "/api/integration/wecom/messages/sync",
    status: "active_p2",
  },
  {
    code: "device.register",
    name: "设备注册接口",
    method: "POST",
    path: "/api/integration/devices/register",
    status: "active_p2",
  },
  {
    code: "device.heartbeat",
    name: "设备心跳接口",
    method: "POST",
    path: "/api/integration/devices/heartbeat",
    status: "active_p2",
  },
  {
    code: "device.event.report",
    name: "设备事件上报接口",
    method: "POST",
    path: "/api/integration/devices/events",
    status: "active_p2",
  },
  {
    code: "device.offline.sync",
    name: "设备离线同步接口",
    method: "POST",
    path: "/api/integration/devices/offline-sync",
    status: "active_p2",
  },
];

const webhookSchema = z.object({
  requestId: z.string().optional(),
  payload: z.unknown().optional(),
});

const orderSyncSchema = z.object({
  externalOrderId: z.string().min(1),
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  amount: z.number().int().nonnegative().default(0),
  currency: z.string().default("CNY"),
  status: z.string().default("paid"),
  occurredAt: z.string().datetime().optional(),
  payload: z.unknown().optional(),
  createRetryTask: z.boolean().optional(),
  createCompensationTask: z.boolean().optional(),
});

const refundSyncSchema = z.object({
  externalRefundId: z.string().min(1),
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  amount: z.number().int().nonnegative().default(0),
  currency: z.string().default("CNY"),
  status: z.string().default("refunded"),
  occurredAt: z.string().datetime().optional(),
  payload: z.unknown().optional(),
  createRetryTask: z.boolean().optional(),
  createCompensationTask: z.boolean().optional(),
});

const voucherVerifySchema = z.object({
  externalVerificationId: z.string().min(1),
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  entitlementCode: z.string().optional(),
  amount: z.number().int().nonnegative().default(0),
  status: z.string().default("verified"),
  resultReason: z.string().optional(),
  payload: z.unknown().optional(),
});

const reconciliationBatchSchema = z.object({
  businessType: z.enum(["catering", "retail"]),
  reconDate: z.string().min(1),
  sourceType: z.string().default("manual"),
  thirdPartyCount: z.number().int().nonnegative().optional(),
  thirdPartyAmount: z.number().int().nonnegative().optional(),
});

const retailSkuSyncSchema = z.object({
  externalSkuId: z.string().min(1),
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  skuCode: z.string().min(1),
  skuName: z.string().min(1),
  category: z.string().optional(),
  priceAmount: z.number().int().nonnegative().default(0),
  currency: z.string().default("CNY"),
  status: z.string().default("active"),
  payload: z.unknown().optional(),
});

const robotTaskSchema = z.object({
  externalTaskId: z.string().optional(),
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  taskType: z.string().default("delivery"),
  pickupLocation: z.string().optional(),
  dropoffLocation: z.string().optional(),
  assignedRobotCode: z.string().optional(),
  priority: z.number().int().default(0),
  scheduledAt: z.string().datetime().optional(),
  payload: z.unknown().optional(),
});

const robotTaskEventSchema = z.object({
  taskId: z.string().optional(),
  externalTaskId: z.string().optional(),
  externalEventId: z.string().optional(),
  eventType: z.string().min(1),
  status: z.string().default("running"),
  occurredAt: z.string().datetime().optional(),
  assignedRobotCode: z.string().optional(),
  abnormalReason: z.string().optional(),
  payload: z.unknown().optional(),
});

const cameraSchema = z.object({
  cameraCode: z.string().min(1),
  externalCameraId: z.string().optional(),
  deviceId: z.string().optional(),
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  areaId: z.string().optional(),
  locationPointId: z.string().optional(),
  cameraName: z.string().min(1),
  provider: z.string().optional(),
  streamUrl: z.string().optional(),
  status: z.string().default("active"),
});

const videoEvidenceSchema = z.object({
  cameraId: z.string().optional(),
  eventType: z.string().min(1),
  eventId: z.string().optional(),
  objectType: z.string().optional(),
  objectId: z.string().optional(),
  snapshotUrl: z.string().optional(),
  clipUrl: z.string().optional(),
  capturedAt: z.string().datetime().optional(),
  payload: z.unknown().optional(),
});

const wecomBindingSchema = z.object({
  corpId: z.string().min(1),
  staffUserId: z.string().min(1),
  userId: z.string().optional(),
  staffId: z.string().optional(),
  merchantId: z.string().optional(),
  externalUserId: z.string().optional(),
  status: z.string().default("active"),
});

const wecomConversationSchema = z.object({
  conversationKey: z.string().min(1),
  corpId: z.string().min(1),
  merchantId: z.string().optional(),
  staffUserId: z.string().min(1),
  externalCustomerId: z.string().optional(),
  customerName: z.string().optional(),
  status: z.string().default("active"),
  startedAt: z.string().datetime().optional(),
  lastMessageAt: z.string().datetime().optional(),
  payload: z.unknown().optional(),
});

const wecomMessageSchema = z.object({
  conversationKey: z.string().min(1),
  corpId: z.string().min(1),
  merchantId: z.string().optional(),
  staffUserId: z.string().min(1),
  externalCustomerId: z.string().optional(),
  customerName: z.string().optional(),
  msgId: z.string().min(1),
  senderType: z.string().default("customer"),
  senderId: z.string().optional(),
  contentType: z.string().default("text"),
  content: z.string().optional(),
  sentAt: z.string().datetime().optional(),
  payload: z.unknown().optional(),
});

const reviewRiskSchema = z.object({
  reviewResult: z.string().min(1),
});

const robotTaskActionSchema = z.object({
  reason: z.string().optional(),
  signedBy: z.string().optional(),
});

const deviceRegisterSchema = z.object({
  deviceCode: z.string().min(1),
  deviceType: z.string().default("integration_device"),
  externalDeviceId: z.string().optional(),
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  areaId: z.string().optional(),
  locationPointId: z.string().optional(),
  providerId: z.string().optional(),
  serialNo: z.string().optional(),
  imei: z.string().optional(),
  status: z.string().default("active"),
});

const deviceEventSchema = z.object({
  deviceId: z.string().optional(),
  deviceCode: z.string().optional(),
  externalDeviceId: z.string().optional(),
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  locationPointId: z.string().optional(),
  eventType: z.string().min(1),
  eventSeq: z.string().min(1),
  status: z.string().default("received"),
  occurredAt: z.string().datetime().optional(),
  payload: z.unknown().optional(),
});

const offlineSyncSchema = z.object({
  batchNo: z.string().optional(),
  deviceId: z.string().optional(),
  deviceCode: z.string().optional(),
  events: z.array(deviceEventSchema).default([]),
  payload: z.unknown().optional(),
});

const resolveSchema = z.object({
  resolution: z.string().min(1),
});

const actualIntegrationPaths = new Set([
  "/api/integration/catering/orders/sync",
  "/api/integration/catering/refunds/sync",
  "/api/integration/catering/vouchers/verify",
  "/api/integration/retail/orders/sync",
  "/api/integration/retail/refunds/sync",
  "/api/integration/retail/vouchers/verify",
  "/api/integration/reconciliation/batches",
  "/api/integration/robot/tasks",
  "/api/integration/robot/tasks/callback",
  "/api/integration/video/cameras",
  "/api/integration/video/evidences/bind",
  "/api/integration/wecom/bindings",
  "/api/integration/wecom/conversations/sync",
  "/api/integration/wecom/messages/sync",
  "/api/integration/devices/register",
  "/api/integration/devices/heartbeat",
  "/api/integration/devices/events",
  "/api/integration/devices/offline-sync",
]);

function serializePayload(payload: unknown) {
  return payload === undefined ? undefined : JSON.stringify(payload);
}

function parseBusinessType(value: string) {
  if (value !== "catering" && value !== "retail") throw new Error("UNSUPPORTED_BUSINESS_TYPE");
  return value;
}

function referenceNo(prefix: string) {
  return `${prefix}_${Date.now().toString(36)}_${Math.random().toString(16).slice(2, 10)}`;
}

function toDate(value?: string) {
  return value ? new Date(value) : new Date();
}

function maskSensitiveText(value?: string) {
  if (!value) return value;
  return value
    .replace(/1[3-9]\d{9}/g, (match) => `${match.slice(0, 3)}****${match.slice(7)}`)
    .replace(/[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/g, "***@***");
}

function classifyWecomContent(content?: string) {
  const text = content ?? "";
  const rules = [
    { keyword: "投诉", riskType: "complaint", riskLevel: "high", ruleCode: "WECOM_COMPLAINT" },
    { keyword: "私下收款", riskType: "private_payment", riskLevel: "high", ruleCode: "WECOM_PRIVATE_PAYMENT" },
    { keyword: "违规承诺", riskType: "illegal_commitment", riskLevel: "high", ruleCode: "WECOM_ILLEGAL_COMMITMENT" },
    { keyword: "退款", riskType: "refund_request", riskLevel: "medium", ruleCode: "WECOM_REFUND_REQUEST" },
    { keyword: "差评", riskType: "negative_feedback", riskLevel: "medium", ruleCode: "WECOM_NEGATIVE_FEEDBACK" },
  ];
  return rules.find((rule) => text.includes(rule.keyword));
}

function classifyCustomerIntent(content?: string) {
  const text = content ?? "";
  if (text.includes("生日") || text.includes("团建") || text.includes("包场")) return { intentType: "event_booking", label: "活动预订" };
  if (text.includes("酒") || text.includes("餐") || text.includes("套餐")) return { intentType: "food_beverage", label: "餐酒需求" };
  if (text.includes("排队") || text.includes("预约") || text.includes("项目")) return { intentType: "amusement", label: "游乐咨询" };
  return null;
}

function isHighRiskDeviceEvent(eventType: string, status?: string) {
  return (
    ["forced_open", "tamper", "root_detected", "offline_conflict", "abnormal_time", "gate_open_failed"].includes(eventType) ||
    ["failed", "abnormal", "conflict"].includes(status ?? "")
  );
}

async function createRiskAlertFromIntegration(input: {
  alertType: string;
  riskLevel: string;
  objectType?: string;
  objectId?: string;
  summary: string;
}) {
  return prisma.riskAlert.create({
    data: {
      alertType: input.alertType,
      riskLevel: input.riskLevel,
      objectType: input.objectType,
      objectId: input.objectId,
      summary: input.summary,
    },
  });
}

async function recordDeviceIntegrationEvent(input: z.infer<typeof deviceEventSchema>) {
  const device = input.deviceId
    ? await prisma.device.findUnique({ where: { id: input.deviceId } })
    : input.deviceCode
      ? await prisma.device.findUnique({ where: { deviceCode: input.deviceCode } })
      : null;
  const deviceId = input.deviceId ?? device?.id;
  const deviceCode = input.deviceCode ?? device?.deviceCode;
  const eventKey = `${deviceId ?? deviceCode ?? input.externalDeviceId ?? "unknown"}:${input.eventSeq}`;
  const existing = await prisma.deviceIntegrationEvent.findUnique({ where: { eventKey } });
  const event = await prisma.deviceIntegrationEvent.upsert({
    where: { eventKey },
    update: {
      deviceId,
      deviceCode,
      externalDeviceId: input.externalDeviceId,
      merchantId: input.merchantId ?? device?.merchantId,
      venueId: input.venueId ?? device?.venueId,
      locationPointId: input.locationPointId ?? device?.locationPointId,
      eventType: input.eventType,
      status: input.status,
      occurredAt: toDate(input.occurredAt),
      payload: serializePayload(input.payload),
    },
    create: {
      eventKey,
      deviceId,
      deviceCode,
      externalDeviceId: input.externalDeviceId,
      merchantId: input.merchantId ?? device?.merchantId,
      venueId: input.venueId ?? device?.venueId,
      locationPointId: input.locationPointId ?? device?.locationPointId,
      eventType: input.eventType,
      eventSeq: input.eventSeq,
      status: input.status,
      occurredAt: toDate(input.occurredAt),
      payload: serializePayload(input.payload),
    },
  });

  if (!existing && isHighRiskDeviceEvent(input.eventType, input.status)) {
    await createRiskAlertFromIntegration({
      alertType: "device_integration_event",
      riskLevel: "high",
      objectType: "device_integration_event",
      objectId: event.id,
      summary: `设备异常事件：${deviceCode ?? input.externalDeviceId ?? "unknown"} / ${input.eventType}`,
    });
  }

  return { event, created: !existing };
}

async function createRetryOrCompensationTasks(input: {
  businessType: string;
  taskType: string;
  referenceId?: string;
  createRetryTask?: boolean;
  createCompensationTask?: boolean;
  reason?: string;
}) {
  if (input.createRetryTask) {
    await prisma.integrationRetryTask.create({
      data: {
        businessType: input.businessType,
        taskType: input.taskType,
        referenceId: input.referenceId,
        lastError: input.reason ?? "manual retry requested",
      },
    });
  }
  if (input.createCompensationTask) {
    await prisma.integrationCompensationTask.create({
      data: {
        businessType: input.businessType,
        taskType: input.taskType,
        referenceId: input.referenceId,
        reason: input.reason ?? "manual compensation requested",
      },
    });
  }
}

export async function registerIntegrationReservedRoutes(app: FastifyInstance) {
  app.get("/api/integration/interfaces", async () => ({
    policy: "P0 仅预留第三方接口契约，不处理真实供应商业务。",
    interfaces: reservedInterfaces,
  }));

  app.post("/api/integration/webhooks/:provider/:eventType", async (request, reply) => {
    const params = request.params as { provider: string; eventType: string };
    const body = webhookSchema.parse(request.body ?? {});

    await writeAuditEvent({
      eventType: "integration.webhook.reserved",
      eventCategory: "integration",
      sourceType: "third_party",
      sourceSystem: params.provider,
      result: "reserved",
      summary: `第三方预留 Webhook 收到请求：${params.provider}/${params.eventType}`,
      payload: {
        requestId: body.requestId,
        eventType: params.eventType,
        p0Policy: "reserved_only",
      },
    });

    return reply.code(202).send({
      accepted: true,
      reserved: true,
      businessProcessed: false,
      message: "P0 仅确认接口契约和留痕，不执行真实第三方业务处理。",
    });
  });

  app.post("/api/integration/:businessType/orders/sync", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const params = request.params as { businessType: string };
    const businessType = parseBusinessType(params.businessType);
    const input = orderSyncSchema.parse(request.body);
    const merchantId = scopedMerchantId(request, input.merchantId);
    const record = await prisma.thirdPartyOrderSync.upsert({
      where: {
        businessType_syncType_externalBusinessId: {
          businessType,
          syncType: "order",
          externalBusinessId: input.externalOrderId,
        },
      },
      update: {
        merchantId,
        venueId: input.venueId,
        amount: input.amount,
        currency: input.currency,
        status: input.status,
        occurredAt: input.occurredAt ? new Date(input.occurredAt) : new Date(),
        payload: serializePayload(input.payload),
      },
      create: {
        businessType,
        syncType: "order",
        externalBusinessId: input.externalOrderId,
        merchantId,
        venueId: input.venueId,
        amount: input.amount,
        currency: input.currency,
        status: input.status,
        occurredAt: input.occurredAt ? new Date(input.occurredAt) : new Date(),
        payload: serializePayload(input.payload),
      },
    });
    await createRetryOrCompensationTasks({
      businessType,
      taskType: "order_sync",
      referenceId: record.id,
      createRetryTask: input.createRetryTask,
      createCompensationTask: input.createCompensationTask,
      reason: `order sync ${input.status}`,
    });
    await writeAuditEvent({
      eventType: `third_party.${businessType}.order_synced`,
      eventCategory: "third_party",
      sourceType: "third_party",
      merchantId: record.merchantId ?? undefined,
      actorId: actor.id,
      summary: `${businessType} 订单同步：${record.externalBusinessId}`,
      payload: { recordId: record.id, amount: record.amount, status: record.status },
    });
    return reply.code(202).send({ accepted: true, businessProcessed: true, record });
  });

  app.post("/api/integration/:businessType/refunds/sync", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const params = request.params as { businessType: string };
    const businessType = parseBusinessType(params.businessType);
    const input = refundSyncSchema.parse(request.body);
    const merchantId = scopedMerchantId(request, input.merchantId);
    const record = await prisma.thirdPartyOrderSync.upsert({
      where: {
        businessType_syncType_externalBusinessId: {
          businessType,
          syncType: "refund",
          externalBusinessId: input.externalRefundId,
        },
      },
      update: {
        merchantId,
        venueId: input.venueId,
        amount: input.amount,
        currency: input.currency,
        status: input.status,
        occurredAt: input.occurredAt ? new Date(input.occurredAt) : new Date(),
        payload: serializePayload(input.payload),
      },
      create: {
        businessType,
        syncType: "refund",
        externalBusinessId: input.externalRefundId,
        merchantId,
        venueId: input.venueId,
        amount: input.amount,
        currency: input.currency,
        status: input.status,
        occurredAt: input.occurredAt ? new Date(input.occurredAt) : new Date(),
        payload: serializePayload(input.payload),
      },
    });
    await createRetryOrCompensationTasks({
      businessType,
      taskType: "refund_sync",
      referenceId: record.id,
      createRetryTask: input.createRetryTask,
      createCompensationTask: input.createCompensationTask,
      reason: `refund sync ${input.status}`,
    });
    await writeAuditEvent({
      eventType: `third_party.${businessType}.refund_synced`,
      eventCategory: "third_party",
      sourceType: "third_party",
      merchantId: record.merchantId ?? undefined,
      actorId: actor.id,
      summary: `${businessType} 退款同步：${record.externalBusinessId}`,
      payload: { recordId: record.id, amount: record.amount, status: record.status },
    });
    return reply.code(202).send({ accepted: true, businessProcessed: true, record });
  });

  app.post("/api/integration/:businessType/vouchers/verify", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const params = request.params as { businessType: string };
    const businessType = parseBusinessType(params.businessType);
    const input = voucherVerifySchema.parse(request.body);
    const merchantId = scopedMerchantId(request, input.merchantId);
    const record = await prisma.thirdPartyVoucherVerification.upsert({
      where: {
        businessType_externalVerificationId: {
          businessType,
          externalVerificationId: input.externalVerificationId,
        },
      },
      update: {
        merchantId,
        venueId: input.venueId,
        entitlementCode: input.entitlementCode,
        amount: input.amount,
        status: input.status,
        resultReason: input.resultReason,
        payload: serializePayload(input.payload),
      },
      create: {
        businessType,
        externalVerificationId: input.externalVerificationId,
        merchantId,
        venueId: input.venueId,
        entitlementCode: input.entitlementCode,
        amount: input.amount,
        status: input.status,
        resultReason: input.resultReason,
        payload: serializePayload(input.payload),
      },
    });
    await writeAuditEvent({
      eventType: `third_party.${businessType}.voucher_verified`,
      eventCategory: "third_party",
      sourceType: "third_party",
      merchantId: record.merchantId ?? undefined,
      actorId: actor.id,
      summary: `${businessType} 券核销同步：${record.externalVerificationId}`,
      payload: { recordId: record.id, entitlementCode: record.entitlementCode, status: record.status },
    });
    return reply.code(202).send({ accepted: true, businessProcessed: true, record });
  });

  app.post("/api/integration/retail/skus/sync", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const input = retailSkuSyncSchema.parse(request.body);
    const merchantId = scopedMerchantId(request, input.merchantId);
    const sku = await prisma.integrationSku.upsert({
      where: { businessType_externalSkuId: { businessType: "retail", externalSkuId: input.externalSkuId } },
      update: {
        merchantId,
        venueId: input.venueId,
        skuCode: input.skuCode,
        skuName: input.skuName,
        category: input.category,
        priceAmount: input.priceAmount,
        currency: input.currency,
        status: input.status,
        payload: serializePayload(input.payload),
      },
      create: {
        businessType: "retail",
        externalSkuId: input.externalSkuId,
        merchantId,
        venueId: input.venueId,
        skuCode: input.skuCode,
        skuName: input.skuName,
        category: input.category,
        priceAmount: input.priceAmount,
        currency: input.currency,
        status: input.status,
        payload: serializePayload(input.payload),
      },
    });
    await writeAuditEvent({
      eventType: "third_party.retail.sku_synced",
      eventCategory: "third_party",
      sourceType: "third_party",
      actorId: actor.id,
      merchantId,
      summary: `零售 SKU 同步：${sku.skuCode}`,
      payload: { skuId: sku.id, externalSkuId: sku.externalSkuId },
    });
    return reply.code(202).send({ accepted: true, businessProcessed: true, sku });
  });

  app.get("/api/integration/retail/skus", async (request) => {
    const query = request.query as { merchantId?: string; status?: string };
    const merchantId = scopedMerchantId(request, query.merchantId);
    return prisma.integrationSku.findMany({
      where: { businessType: "retail", ...(merchantId ? { merchantId } : {}), ...(query.status ? { status: query.status } : {}) },
      orderBy: { createdAt: "desc" },
      take: 200,
    });
  });

  app.post("/api/integration/robot/tasks", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const input = robotTaskSchema.parse(request.body);
    const merchantId = scopedMerchantId(request, input.merchantId);
    const task = await prisma.robotTask.create({
      data: {
        taskNo: referenceNo("robot_task"),
        externalTaskId: input.externalTaskId,
        merchantId,
        venueId: input.venueId,
        taskType: input.taskType,
        pickupLocation: input.pickupLocation,
        dropoffLocation: input.dropoffLocation,
        assignedRobotCode: input.assignedRobotCode,
        priority: input.priority,
        scheduledAt: input.scheduledAt ? new Date(input.scheduledAt) : undefined,
        status: "pending",
        payload: serializePayload(input.payload),
      },
    });
    await prisma.robotTaskEvent.create({
      data: {
        eventKey: `${task.id}:created`,
        robotTaskId: task.id,
        eventType: "created",
        taskStatus: task.status,
        occurredAt: new Date(),
        payload: serializePayload(input.payload),
      },
    });
    await writeAuditEvent({
      eventType: "robot.task.created",
      eventCategory: "robot",
      sourceType: "internal",
      actorId: actor.id,
      merchantId,
      robotTaskId: task.id,
      summary: `机器人任务创建：${task.taskNo}`,
      payload: { taskType: task.taskType, pickupLocation: task.pickupLocation, dropoffLocation: task.dropoffLocation },
    });
    return reply.code(201).send(task);
  });

  app.get("/api/integration/robot/tasks", async (request) => {
    const query = request.query as { merchantId?: string; status?: string };
    const merchantId = scopedMerchantId(request, query.merchantId);
    return prisma.robotTask.findMany({
      where: { ...(merchantId ? { merchantId } : {}), ...(query.status ? { status: query.status } : {}) },
      orderBy: { createdAt: "desc" },
      take: 200,
    });
  });

  app.get("/api/integration/robot/tasks/:id", async (request, reply) => {
    const params = request.params as { id: string };
    const task = await prisma.robotTask.findUnique({ where: { id: params.id } });
    if (!task) return reply.code(404).send({ error: "ROBOT_TASK_NOT_FOUND" });
    const events = await prisma.robotTaskEvent.findMany({ where: { robotTaskId: task.id }, orderBy: { occurredAt: "desc" } });
    return { task, events };
  });

  app.post("/api/integration/robot/tasks/callback", async (request, reply) => {
    const input = robotTaskEventSchema.parse(request.body);
    let task = input.taskId ? await prisma.robotTask.findUnique({ where: { id: input.taskId } }) : null;
    if (!task && input.externalTaskId) task = await prisma.robotTask.findUnique({ where: { externalTaskId: input.externalTaskId } });
    if (!task) {
      task = await prisma.robotTask.create({
        data: {
          taskNo: referenceNo("robot_task"),
          externalTaskId: input.externalTaskId,
          assignedRobotCode: input.assignedRobotCode,
          status: input.status,
          abnormalReason: input.abnormalReason,
          payload: serializePayload(input.payload),
        },
      });
    }
    const occurredAt = toDate(input.occurredAt);
    const eventKey = input.externalEventId ?? `${task.id}:${input.eventType}:${occurredAt.toISOString()}`;
    const taskUpdate = {
      status: input.status,
      assignedRobotCode: input.assignedRobotCode ?? task.assignedRobotCode,
      abnormalReason: input.abnormalReason ?? task.abnormalReason,
      acceptedAt: input.status === "accepted" ? occurredAt : task.acceptedAt,
      pickedUpAt: input.status === "picked_up" ? occurredAt : task.pickedUpAt,
      deliveredAt: input.status === "delivered" ? occurredAt : task.deliveredAt,
      signedAt: input.status === "signed" ? occurredAt : task.signedAt,
      cancelledAt: input.status === "cancelled" ? occurredAt : task.cancelledAt,
    };
    const [updatedTask, event] = await prisma.$transaction([
      prisma.robotTask.update({ where: { id: task.id }, data: taskUpdate }),
      prisma.robotTaskEvent.upsert({
        where: { eventKey },
        update: { taskStatus: input.status, payload: serializePayload(input.payload) },
        create: {
          eventKey,
          robotTaskId: task.id,
          externalEventId: input.externalEventId,
          eventType: input.eventType,
          taskStatus: input.status,
          occurredAt,
          payload: serializePayload(input.payload),
        },
      }),
    ]);
    if (["failed", "abnormal", "exception"].includes(input.status)) {
      await prisma.integrationCompensationTask.create({
        data: { businessType: "robot", taskType: "robot_task_exception", referenceId: task.id, reason: input.abnormalReason ?? input.status },
      });
      await createRiskAlertFromIntegration({
        alertType: "robot_task_exception",
        riskLevel: "high",
        objectType: "robot_task",
        objectId: task.id,
        summary: `机器人任务异常：${task.taskNo} / ${input.status}`,
      });
    }
    await writeAuditEvent({
      eventType: `robot.task.${input.eventType}`,
      eventCategory: "robot",
      sourceType: "third_party",
      robotTaskId: task.id,
      result: ["failed", "abnormal", "exception"].includes(input.status) ? "abnormal" : "success",
      riskLevel: ["failed", "abnormal", "exception"].includes(input.status) ? "high" : "normal",
      summary: `机器人任务状态回传：${updatedTask.taskNo}/${input.status}`,
      payload: { eventId: event.id, externalEventId: input.externalEventId },
    });
    return reply.code(202).send({ accepted: true, businessProcessed: true, task: updatedTask, event });
  });

  app.post("/api/integration/robot/tasks/:id/cancel", async (request, reply) => {
    const params = request.params as { id: string };
    const input = robotTaskActionSchema.parse(request.body ?? {});
    const task = await prisma.robotTask.update({ where: { id: params.id }, data: { status: "cancelled", cancelledAt: new Date(), abnormalReason: input.reason } });
    await prisma.robotTaskEvent.create({
      data: { eventKey: `${task.id}:cancelled:${Date.now()}`, robotTaskId: task.id, eventType: "cancelled", taskStatus: task.status, occurredAt: new Date(), payload: serializePayload(input) },
    });
    await writeAuditEvent({
      eventType: "robot.task.cancelled",
      eventCategory: "robot",
      sourceType: "internal",
      robotTaskId: task.id,
      summary: `机器人任务取消：${task.taskNo}`,
      payload: { reason: input.reason },
    });
    return reply.code(201).send(task);
  });

  app.post("/api/integration/robot/tasks/:id/manual-sign", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const params = request.params as { id: string };
    const input = robotTaskActionSchema.parse(request.body ?? {});
    const task = await prisma.robotTask.update({ where: { id: params.id }, data: { status: "signed", signedAt: new Date() } });
    await prisma.robotTaskEvent.create({
      data: { eventKey: `${task.id}:manual_signed:${Date.now()}`, robotTaskId: task.id, eventType: "manual_signed", taskStatus: task.status, occurredAt: new Date(), payload: serializePayload(input) },
    });
    await writeAuditEvent({
      eventType: "robot.task.manual_signed",
      eventCategory: "robot",
      sourceType: "internal",
      actorId: actor.id,
      robotTaskId: task.id,
      summary: `机器人任务人工签收：${task.taskNo}`,
      payload: { signedBy: input.signedBy, reason: input.reason },
    });
    return reply.code(201).send(task);
  });

  app.get("/api/integration/robot/task-events", async () =>
    prisma.robotTaskEvent.findMany({ orderBy: { occurredAt: "desc" }, take: 200 }),
  );

  app.post("/api/integration/video/cameras", async (request, reply) => {
    const input = cameraSchema.parse(request.body);
    const merchantId = scopedMerchantId(request, input.merchantId);
    const camera = await prisma.cameraDevice.upsert({
      where: { cameraCode: input.cameraCode },
      update: { ...input, merchantId },
      create: { ...input, merchantId },
    });
    await writeAuditEvent({
      eventType: "video.camera.registered",
      eventCategory: "video",
      sourceType: "internal",
      merchantId,
      summary: `摄像头登记：${camera.cameraName}`,
      payload: { cameraId: camera.id, cameraCode: camera.cameraCode },
    });
    return reply.code(201).send(camera);
  });

  app.get("/api/integration/video/cameras", async (request) => {
    const query = request.query as { merchantId?: string; status?: string };
    const merchantId = scopedMerchantId(request, query.merchantId);
    return prisma.cameraDevice.findMany({
      where: { ...(merchantId ? { merchantId } : {}), ...(query.status ? { status: query.status } : {}) },
      orderBy: { createdAt: "desc" },
      take: 200,
    });
  });

  app.post("/api/integration/video/evidences/bind", async (request, reply) => {
    const input = videoEvidenceSchema.parse(request.body);
    const evidence = await prisma.videoEvidenceLink.create({
      data: {
        evidenceNo: referenceNo("video_ev"),
        cameraId: input.cameraId,
        eventType: input.eventType,
        eventId: input.eventId,
        objectType: input.objectType,
        objectId: input.objectId,
        snapshotUrl: input.snapshotUrl,
        clipUrl: input.clipUrl,
        capturedAt: toDate(input.capturedAt),
        payload: serializePayload(input.payload),
      },
    });
    await writeAuditEvent({
      eventType: "video.evidence.bound",
      eventCategory: "video",
      sourceType: "third_party",
      videoEvidenceId: evidence.id,
      summary: `视频证据绑定：${evidence.evidenceNo}`,
      payload: { eventType: evidence.eventType, objectType: evidence.objectType, objectId: evidence.objectId },
    });
    return reply.code(201).send(evidence);
  });

  app.get("/api/integration/video/evidences", async (request) => {
    const query = request.query as { objectType?: string; objectId?: string; status?: string };
    return prisma.videoEvidenceLink.findMany({
      where: { ...(query.objectType ? { objectType: query.objectType } : {}), ...(query.objectId ? { objectId: query.objectId } : {}), ...(query.status ? { status: query.status } : {}) },
      orderBy: { createdAt: "desc" },
      take: 200,
    });
  });

  app.post("/api/integration/video/evidences/:id/view", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const params = request.params as { id: string };
    const evidence = await prisma.videoEvidenceLink.update({
      where: { id: params.id },
      data: { viewCount: { increment: 1 }, lastViewedAt: new Date() },
    });
    await writeAuditEvent({
      eventType: "video.evidence.viewed",
      eventCategory: "video",
      sourceType: "internal",
      actorId: actor.id,
      videoEvidenceId: evidence.id,
      summary: `视频证据调阅：${evidence.evidenceNo}`,
      payload: { viewCount: evidence.viewCount, objectType: evidence.objectType, objectId: evidence.objectId },
    });
    return reply.code(201).send(evidence);
  });

  app.post("/api/integration/wecom/bindings", async (request, reply) => {
    const input = wecomBindingSchema.parse(request.body);
    const merchantId = scopedMerchantId(request, input.merchantId);
    const binding = await prisma.wecomBinding.upsert({
      where: { corpId_staffUserId: { corpId: input.corpId, staffUserId: input.staffUserId } },
      update: { ...input, merchantId },
      create: { ...input, merchantId },
    });
    await writeAuditEvent({
      eventType: "wecom.binding.upserted",
      eventCategory: "conversation",
      sourceType: "internal",
      merchantId,
      summary: `企业微信员工绑定：${binding.staffUserId}`,
      payload: { bindingId: binding.id, corpId: binding.corpId },
    });
    return reply.code(201).send(binding);
  });

  app.get("/api/integration/wecom/bindings", async (request) => {
    const query = request.query as { merchantId?: string; corpId?: string };
    const merchantId = scopedMerchantId(request, query.merchantId);
    return prisma.wecomBinding.findMany({
      where: { ...(merchantId ? { merchantId } : {}), ...(query.corpId ? { corpId: query.corpId } : {}) },
      orderBy: { createdAt: "desc" },
      take: 200,
    });
  });

  app.post("/api/integration/wecom/conversations/sync", async (request, reply) => {
    const input = wecomConversationSchema.parse(request.body);
    const merchantId = scopedMerchantId(request, input.merchantId);
    const conversation = await prisma.wecomConversation.upsert({
      where: { conversationKey: input.conversationKey },
      update: {
        corpId: input.corpId,
        merchantId,
        staffUserId: input.staffUserId,
        externalCustomerId: input.externalCustomerId,
        customerName: input.customerName,
        status: input.status,
        startedAt: input.startedAt ? new Date(input.startedAt) : undefined,
        lastMessageAt: input.lastMessageAt ? new Date(input.lastMessageAt) : undefined,
        payload: serializePayload(input.payload),
      },
      create: {
        conversationKey: input.conversationKey,
        corpId: input.corpId,
        merchantId,
        staffUserId: input.staffUserId,
        externalCustomerId: input.externalCustomerId,
        customerName: input.customerName,
        status: input.status,
        startedAt: input.startedAt ? new Date(input.startedAt) : undefined,
        lastMessageAt: input.lastMessageAt ? new Date(input.lastMessageAt) : undefined,
        payload: serializePayload(input.payload),
      },
    });
    await writeAuditEvent({
      eventType: "wecom.conversation.synced",
      eventCategory: "conversation",
      sourceType: "third_party",
      merchantId,
      conversationId: conversation.id,
      summary: `企业微信会话同步：${conversation.conversationKey}`,
      payload: { customerName: conversation.customerName, staffUserId: conversation.staffUserId },
    });
    return reply.code(202).send({ accepted: true, businessProcessed: true, conversation });
  });

  app.get("/api/integration/wecom/conversations", async (request) => {
    const query = request.query as { merchantId?: string; corpId?: string; staffUserId?: string };
    const merchantId = scopedMerchantId(request, query.merchantId);
    return prisma.wecomConversation.findMany({
      where: {
        ...(merchantId ? { merchantId } : {}),
        ...(query.corpId ? { corpId: query.corpId } : {}),
        ...(query.staffUserId ? { staffUserId: query.staffUserId } : {}),
      },
      orderBy: { updatedAt: "desc" },
      take: 200,
    });
  });

  app.post("/api/integration/wecom/messages/sync", async (request, reply) => {
    const input = wecomMessageSchema.parse(request.body);
    const merchantId = scopedMerchantId(request, input.merchantId);
    const sentAt = toDate(input.sentAt);
    const conversation = await prisma.wecomConversation.upsert({
      where: { conversationKey: input.conversationKey },
      update: {
        corpId: input.corpId,
        merchantId,
        staffUserId: input.staffUserId,
        externalCustomerId: input.externalCustomerId,
        customerName: input.customerName,
        lastMessageAt: sentAt,
      },
      create: {
        conversationKey: input.conversationKey,
        corpId: input.corpId,
        merchantId,
        staffUserId: input.staffUserId,
        externalCustomerId: input.externalCustomerId,
        customerName: input.customerName,
        startedAt: sentAt,
        lastMessageAt: sentAt,
      },
    });
    const riskRule = classifyWecomContent(input.content);
    const sanitizedContent = maskSensitiveText(input.content);
    const messageRecord = await prisma.wecomMessage.upsert({
      where: { msgId: input.msgId },
      update: {
        conversationId: conversation.id,
        senderType: input.senderType,
        senderId: input.senderId,
        contentType: input.contentType,
        content: input.content,
        sanitizedContent,
        riskLevel: riskRule?.riskLevel ?? "normal",
        sentAt,
        payload: serializePayload(input.payload),
      },
      create: {
        msgId: input.msgId,
        conversationId: conversation.id,
        senderType: input.senderType,
        senderId: input.senderId,
        contentType: input.contentType,
        content: input.content,
        sanitizedContent,
        riskLevel: riskRule?.riskLevel ?? "normal",
        sentAt,
        payload: serializePayload(input.payload),
      },
    });
    const intent = classifyCustomerIntent(input.content);
    if (intent) {
      await prisma.customerIntent.create({
        data: {
          conversationId: conversation.id,
          messageId: messageRecord.id,
          customerKey: input.externalCustomerId,
          intentType: intent.intentType,
          label: intent.label,
          confidence: 80,
        },
      });
    }
    let riskHit = null;
    if (riskRule) {
      riskHit = await prisma.wecomRiskHit.findFirst({ where: { messageId: messageRecord.id, ruleCode: riskRule.ruleCode } });
      if (!riskHit) {
        riskHit = await prisma.wecomRiskHit.create({
          data: {
            conversationId: conversation.id,
            messageId: messageRecord.id,
            ruleCode: riskRule.ruleCode,
            riskType: riskRule.riskType,
            riskLevel: riskRule.riskLevel,
            keyword: riskRule.keyword,
            summary: `企业微信质检命中：${riskRule.keyword}`,
          },
        });
        await prisma.aiQualityResult.create({
          data: {
            sourceType: "wecom",
            objectType: "wecom_message",
            objectId: messageRecord.id,
            riskLevel: riskRule.riskLevel,
            score: riskRule.riskLevel === "high" ? 45 : 70,
            summary: `规则质检命中：${riskRule.keyword}`,
            tags: JSON.stringify([riskRule.riskType, riskRule.keyword]),
          },
        });
        await createRiskAlertFromIntegration({
          alertType: "wecom_quality_risk",
          riskLevel: riskRule.riskLevel,
          objectType: "wecom_risk_hit",
          objectId: riskHit.id,
          summary: `企业微信风险命中：${riskRule.keyword}`,
        });
      }
    }
    await writeAuditEvent({
      eventType: riskRule ? "wecom.risk.hit" : "wecom.message.received",
      eventCategory: "conversation",
      sourceType: "third_party",
      merchantId,
      conversationId: conversation.id,
      result: riskRule ? "abnormal" : "success",
      riskLevel: riskRule?.riskLevel ?? "normal",
      summary: riskRule ? `企业微信风险命中：${riskRule.keyword}` : `企业微信消息同步：${messageRecord.msgId}`,
      payload: { messageId: messageRecord.id, riskHitId: riskHit?.id },
    });
    return reply.code(202).send({ accepted: true, businessProcessed: true, conversation, message: messageRecord, riskHit });
  });

  app.get("/api/integration/wecom/messages", async (request) => {
    const query = request.query as { conversationId?: string; riskLevel?: string };
    return prisma.wecomMessage.findMany({
      where: { ...(query.conversationId ? { conversationId: query.conversationId } : {}), ...(query.riskLevel ? { riskLevel: query.riskLevel } : {}) },
      orderBy: { sentAt: "desc" },
      take: 200,
    });
  });

  app.get("/api/integration/wecom/risk-hits", async (request) => {
    const query = request.query as { status?: string; riskLevel?: string };
    return prisma.wecomRiskHit.findMany({
      where: { ...(query.status ? { status: query.status } : {}), ...(query.riskLevel ? { riskLevel: query.riskLevel } : {}) },
      orderBy: { createdAt: "desc" },
      take: 200,
    });
  });

  app.post("/api/integration/wecom/risk-hits/:id/review", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const params = request.params as { id: string };
    const input = reviewRiskSchema.parse(request.body);
    const riskHit = await prisma.wecomRiskHit.update({
      where: { id: params.id },
      data: { status: "reviewed", reviewedBy: actor.id, reviewedAt: new Date(), reviewResult: input.reviewResult },
    });
    await writeAuditEvent({
      eventType: "wecom.risk.reviewed",
      eventCategory: "conversation",
      sourceType: "internal",
      actorId: actor.id,
      conversationId: riskHit.conversationId ?? undefined,
      summary: `企业微信风险复核：${riskHit.summary}`,
      payload: { riskHitId: riskHit.id, reviewResult: input.reviewResult },
    });
    return reply.code(201).send(riskHit);
  });

  app.post("/api/integration/devices/register", async (request, reply) => {
    const input = deviceRegisterSchema.parse(request.body);
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
        providerId: input.providerId,
        status: input.status,
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
        providerId: input.providerId,
        status: input.status,
      },
    });
    await writeAuditEvent({
      eventType: "device.integration.registered",
      eventCategory: "device",
      sourceType: "third_party",
      merchantId,
      deviceId: device.id,
      summary: `设备接入注册：${device.deviceCode}`,
      payload: { externalDeviceId: input.externalDeviceId, deviceType: device.deviceType },
    });
    return reply.code(201).send(device);
  });

  app.get("/api/integration/devices", async (request) => {
    const query = request.query as { merchantId?: string; status?: string; deviceType?: string };
    const merchantId = scopedMerchantId(request, query.merchantId);
    return prisma.device.findMany({
      where: {
        ...(merchantId ? { merchantId } : {}),
        ...(query.status ? { status: query.status } : {}),
        ...(query.deviceType ? { deviceType: query.deviceType } : {}),
      },
      orderBy: { createdAt: "desc" },
      take: 200,
    });
  });

  app.post("/api/integration/devices/heartbeat", async (request, reply) => {
    const input = deviceEventSchema.extend({ eventType: z.string().default("heartbeat"), eventSeq: z.string().optional() }).parse(request.body);
    const eventInput = {
      ...input,
      eventType: input.eventType,
      eventSeq: input.eventSeq ?? referenceNo("heartbeat"),
      status: input.status ?? "online",
    };
    if (input.deviceCode) {
      await prisma.device.updateMany({ where: { deviceCode: input.deviceCode }, data: { status: eventInput.status } });
    }
    const recorded = await recordDeviceIntegrationEvent(eventInput);
    await writeAuditEvent({
      eventType: "device.heartbeat",
      eventCategory: "device",
      sourceType: "third_party",
      deviceId: recorded.event.deviceId ?? undefined,
      summary: `设备心跳：${recorded.event.deviceCode ?? recorded.event.externalDeviceId ?? "unknown"}`,
      payload: { eventId: recorded.event.id, status: recorded.event.status },
    });
    return reply.code(202).send({ accepted: true, businessProcessed: true, ...recorded });
  });

  app.post("/api/integration/devices/events", async (request, reply) => {
    const input = deviceEventSchema.parse(request.body);
    const recorded = await recordDeviceIntegrationEvent(input);
    await writeAuditEvent({
      eventType: `device.${input.eventType}`,
      eventCategory: "device",
      sourceType: "third_party",
      merchantId: recorded.event.merchantId ?? undefined,
      venueId: recorded.event.venueId ?? undefined,
      locationPointId: recorded.event.locationPointId ?? undefined,
      deviceId: recorded.event.deviceId ?? undefined,
      result: isHighRiskDeviceEvent(input.eventType, input.status) ? "abnormal" : "success",
      riskLevel: isHighRiskDeviceEvent(input.eventType, input.status) ? "high" : "normal",
      summary: `设备事件上报：${recorded.event.deviceCode ?? recorded.event.externalDeviceId ?? "unknown"} / ${input.eventType}`,
      payload: { eventId: recorded.event.id, eventSeq: input.eventSeq },
    });
    return reply.code(202).send({ accepted: true, businessProcessed: true, ...recorded });
  });

  app.post("/api/integration/devices/offline-sync", async (request, reply) => {
    const input = offlineSyncSchema.parse(request.body ?? {});
    const batchNo = input.batchNo ?? referenceNo("offline_batch");
    const recorded = [];
    for (const item of input.events) {
      recorded.push(await recordDeviceIntegrationEvent({ ...item, deviceId: item.deviceId ?? input.deviceId, deviceCode: item.deviceCode ?? input.deviceCode }));
    }
    const rejectedCount = recorded.filter((item) => isHighRiskDeviceEvent(item.event.eventType, item.event.status)).length;
    const batch = await prisma.deviceOfflineSyncBatch.create({
      data: {
        batchNo,
        deviceId: input.deviceId,
        deviceCode: input.deviceCode,
        totalCount: input.events.length,
        acceptedCount: input.events.length - rejectedCount,
        rejectedCount,
        status: rejectedCount > 0 ? "partial" : "accepted",
        payload: serializePayload(input.payload),
      },
    });
    await writeAuditEvent({
      eventType: "device.offline_sync.uploaded",
      eventCategory: "device",
      sourceType: "third_party",
      deviceId: input.deviceId,
      result: rejectedCount > 0 ? "abnormal" : "success",
      riskLevel: rejectedCount > 0 ? "high" : "normal",
      summary: `设备离线同步：${batch.batchNo}`,
      payload: { batchId: batch.id, totalCount: batch.totalCount, rejectedCount: batch.rejectedCount },
    });
    return reply.code(202).send({ accepted: true, businessProcessed: true, batch, events: recorded.map((item) => item.event) });
  });

  app.get("/api/integration/devices/events", async (request) => {
    const query = request.query as { deviceId?: string; deviceCode?: string; eventType?: string };
    return prisma.deviceIntegrationEvent.findMany({
      where: {
        ...(query.deviceId ? { deviceId: query.deviceId } : {}),
        ...(query.deviceCode ? { deviceCode: query.deviceCode } : {}),
        ...(query.eventType ? { eventType: query.eventType } : {}),
      },
      orderBy: { occurredAt: "desc" },
      take: 200,
    });
  });

  app.get("/api/integration/devices/offline-sync-batches", async () =>
    prisma.deviceOfflineSyncBatch.findMany({ orderBy: { createdAt: "desc" }, take: 200 }),
  );

  app.get("/api/integration/order-syncs", async (request) => {
    const query = request.query as { businessType?: string; syncType?: string; merchantId?: string };
    const merchantId = scopedMerchantId(request, query.merchantId);
    return prisma.thirdPartyOrderSync.findMany({
      where: {
        ...(merchantId ? { merchantId } : {}),
        ...(query.businessType ? { businessType: query.businessType } : {}),
        ...(query.syncType ? { syncType: query.syncType } : {}),
      },
      orderBy: { createdAt: "desc" },
      take: 200,
    });
  });

  app.get("/api/integration/voucher-verifications", async (request) => {
    const query = request.query as { businessType?: string; merchantId?: string };
    const merchantId = scopedMerchantId(request, query.merchantId);
    return prisma.thirdPartyVoucherVerification.findMany({
      where: { ...(merchantId ? { merchantId } : {}), ...(query.businessType ? { businessType: query.businessType } : {}) },
      orderBy: { createdAt: "desc" },
      take: 200,
    });
  });

  app.get("/api/integration/retry-tasks", async () =>
    prisma.integrationRetryTask.findMany({ orderBy: { createdAt: "desc" }, take: 200 }),
  );

  app.post("/api/integration/retry-tasks/:id/run-now", async (request, reply) => {
    const params = request.params as { id: string };
    const task = await prisma.integrationRetryTask.update({
      where: { id: params.id },
      data: { status: "succeeded", attemptCount: { increment: 1 }, lastError: null },
    });
    await writeAuditEvent({
      eventType: "integration.retry.run_now",
      eventCategory: "integration",
      sourceType: "internal",
      summary: `第三方重试任务执行：${task.id}`,
      payload: { taskType: task.taskType, referenceId: task.referenceId },
    });
    return reply.code(201).send(task);
  });

  app.get("/api/integration/compensation-tasks", async () =>
    prisma.integrationCompensationTask.findMany({ orderBy: { createdAt: "desc" }, take: 200 }),
  );

  app.post("/api/integration/compensation-tasks/:id/resolve", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const params = request.params as { id: string };
    const input = resolveSchema.parse(request.body);
    const task = await prisma.integrationCompensationTask.update({
      where: { id: params.id },
      data: { status: "resolved", resolvedBy: actor.id, resolvedAt: new Date(), resolution: input.resolution },
    });
    await writeAuditEvent({
      eventType: "integration.compensation.resolved",
      eventCategory: "integration",
      sourceType: "internal",
      actorId: actor.id,
      summary: `第三方补偿任务处理：${task.id}`,
      payload: { taskType: task.taskType, resolution: input.resolution },
    });
    return reply.code(201).send(task);
  });

  app.post("/api/integration/reconciliation/batches", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const input = reconciliationBatchSchema.parse(request.body);
    const records = await prisma.thirdPartyOrderSync.findMany({
      where: { businessType: input.businessType, syncType: "order" },
    });
    const platformCount = records.length;
    const platformAmount = records.reduce((sum, record) => sum + record.amount, 0);
    const thirdPartyCount = input.thirdPartyCount ?? platformCount;
    const thirdPartyAmount = input.thirdPartyAmount ?? platformAmount;
    const diffCount = (thirdPartyCount === platformCount ? 0 : 1) + (thirdPartyAmount === platformAmount ? 0 : 1);
    const batch = await prisma.$transaction(async (tx) => {
      const created = await tx.reconciliationBatch.create({
        data: {
          businessType: input.businessType,
          reconDate: input.reconDate,
          sourceType: input.sourceType,
          platformCount,
          thirdPartyCount,
          platformAmount,
          thirdPartyAmount,
          diffCount,
          status: diffCount === 0 ? "matched" : "diff",
        },
      });
      if (thirdPartyCount !== platformCount) {
        await tx.reconciliationDifference.create({
          data: {
            batchId: created.id,
            businessType: input.businessType,
            diffType: "count_mismatch",
            platformValue: JSON.stringify({ count: platformCount }),
            thirdPartyValue: JSON.stringify({ count: thirdPartyCount }),
          },
        });
      }
      if (thirdPartyAmount !== platformAmount) {
        await tx.reconciliationDifference.create({
          data: {
            batchId: created.id,
            businessType: input.businessType,
            diffType: "amount_mismatch",
            platformValue: JSON.stringify({ amount: platformAmount }),
            thirdPartyValue: JSON.stringify({ amount: thirdPartyAmount }),
          },
        });
      }
      return created;
    });
    await writeAuditEvent({
      eventType: "integration.reconciliation.batch_created",
      eventCategory: "integration",
      sourceType: "internal",
      actorId: actor.id,
      summary: `第三方对账批次：${batch.businessType}/${batch.reconDate}`,
      payload: { batchId: batch.id, diffCount: batch.diffCount },
    });
    return reply.code(201).send(batch);
  });

  app.get("/api/integration/reconciliation/batches", async () =>
    prisma.reconciliationBatch.findMany({ orderBy: { createdAt: "desc" }, take: 200 }),
  );

  app.get("/api/integration/reconciliation/differences", async (request) => {
    const query = request.query as { batchId?: string; status?: string };
    return prisma.reconciliationDifference.findMany({
      where: { ...(query.batchId ? { batchId: query.batchId } : {}), ...(query.status ? { status: query.status } : {}) },
      orderBy: { createdAt: "desc" },
      take: 200,
    });
  });

  app.post("/api/integration/reconciliation/differences/:id/resolve", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const params = request.params as { id: string };
    const input = resolveSchema.parse(request.body);
    const diff = await prisma.reconciliationDifference.update({
      where: { id: params.id },
      data: { status: "resolved", resolvedBy: actor.id, resolvedAt: new Date(), resolution: input.resolution },
    });
    await writeAuditEvent({
      eventType: "integration.reconciliation.difference_resolved",
      eventCategory: "integration",
      sourceType: "internal",
      actorId: actor.id,
      summary: `第三方对账差异处理：${diff.diffType}`,
      payload: { differenceId: diff.id, resolution: input.resolution },
    });
    return reply.code(201).send(diff);
  });

  for (const item of reservedInterfaces) {
    if (actualIntegrationPaths.has(item.path)) continue;
    app.route({
      method: item.method as "POST",
      url: item.path,
      handler: async (_request, reply) =>
        reply.code(202).send({
          accepted: true,
          reserved: true,
          interfaceCode: item.code,
          businessProcessed: false,
          message: "P0 第三方接口预留，真实供应商联调进入后续阶段。",
        }),
    });
  }
}
