import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { prisma } from "../../lib/prisma.js";
import { requireCurrentUser } from "../../lib/security.js";
import { writeAuditEvent } from "../events/service.js";

const riskRuleSchema = z.object({
  ruleCode: z.string().min(1),
  ruleName: z.string().min(1),
  ruleType: z.string().min(1),
  threshold: z.number().int().nonnegative().default(0),
});

const riskAlertSchema = z.object({
  ruleId: z.string().optional(),
  alertType: z.string().min(1),
  riskLevel: z.string().default("normal"),
  objectType: z.string().optional(),
  objectId: z.string().optional(),
  summary: z.string().min(1),
});

const metricDefinitionSchema = z.object({
  metricCode: z.string().min(1),
  metricName: z.string().min(1),
  category: z.string().min(1),
  unit: z.string().optional(),
  description: z.string().optional(),
  status: z.string().default("active"),
});

const aiQualityTaskSchema = z.object({
  taskType: z.string().default("wecom_quality"),
  objectType: z.string().optional(),
  objectId: z.string().optional(),
  processNow: z.boolean().default(true),
});

const anomalyExplainSchema = z.object({
  metricCode: z.string().min(1),
  metricName: z.string().min(1),
  dimensionType: z.string().default("platform"),
  dimensionId: z.string().optional(),
  baselineValue: z.number().int().default(0),
  currentValue: z.number().int().default(0),
});

function referenceNo(prefix: string) {
  return `${prefix}_${Date.now().toString(36)}_${Math.random().toString(16).slice(2, 10)}`;
}

function explainMetricChange(input: z.infer<typeof anomalyExplainSchema>) {
  const delta = input.currentValue - input.baselineValue;
  const changeRate = input.baselineValue === 0 ? (input.currentValue > 0 ? 10000 : 0) : Math.round((delta / input.baselineValue) * 10000);
  const absRate = Math.abs(changeRate);
  const severity = absRate >= 5000 ? "high" : absRate >= 2000 ? "medium" : "normal";
  const direction = delta >= 0 ? "上升" : "下降";
  const percent = (Math.abs(changeRate) / 100).toFixed(2);
  const explanation =
    severity === "normal"
      ? `${input.metricName}较基准${direction}${percent}%，处于可观察范围。`
      : `${input.metricName}较基准${direction}${percent}%，建议结合票务、酒吧、游乐、第三方和风控事件检查驱动因素。`;
  return { changeRate, severity, explanation };
}

async function completeAiQualityTask(taskId: string, objectType?: string, objectId?: string) {
  let riskLevel = "normal";
  let score = 95;
  let summary = "规则质检完成，未发现明显风险。";
  const tags: string[] = ["rule_quality"];

  if (objectType === "wecom_message" && objectId) {
    const message = await prisma.wecomMessage.findUnique({ where: { id: objectId } });
    if (message?.riskLevel && message.riskLevel !== "normal") {
      riskLevel = message.riskLevel;
      score = message.riskLevel === "high" ? 45 : 70;
      summary = `企微消息质检完成，风险等级：${message.riskLevel}`;
      tags.push("wecom", message.riskLevel);
    }
  }

  const result = await prisma.aiQualityResult.create({
    data: {
      taskId,
      sourceType: "internal_rule",
      objectType,
      objectId,
      riskLevel,
      score,
      summary,
      tags: JSON.stringify(tags),
    },
  });
  const task = await prisma.aiQualityTask.update({
    where: { id: taskId },
    data: { status: "completed", resultSummary: summary, startedAt: new Date(), completedAt: new Date() },
  });
  await writeAuditEvent({
    eventType: "ai.quality.completed",
    eventCategory: "ai",
    sourceType: "internal",
    result: riskLevel === "normal" ? "success" : "abnormal",
    riskLevel,
    summary,
    payload: { taskId: task.id, resultId: result.id, score },
  });
  return { task, result };
}

export async function registerAnalyticsRoutes(app: FastifyInstance) {
  app.get("/api/analytics/overview", async () => {
    const [
      merchants,
      ticketProducts,
      issuedTickets,
      verifications,
      barOrders,
      amusementSessions,
      riskAlerts,
      auditEvents,
    ] = await Promise.all([
      prisma.merchant.count(),
      prisma.ticketProduct.count(),
      prisma.issuedTicket.count(),
      prisma.verificationRecord.count({ where: { status: "success" } }),
      prisma.barOrder.count(),
      prisma.amusementSession.count(),
      prisma.riskAlert.count({ where: { status: "open" } }),
      prisma.auditEvent.count(),
    ]);

    return {
      merchants,
      ticketProducts,
      issuedTickets,
      verifications,
      barOrders,
      amusementSessions,
      openRiskAlerts: riskAlerts,
      auditEvents,
    };
  });

  app.get("/api/analytics/merchants", async () => {
    const merchants = await prisma.merchant.findMany({ orderBy: { createdAt: "desc" }, take: 100 });
    return Promise.all(
      merchants.map(async (merchant) => ({
        merchant,
        verifications: await prisma.verificationRecord.count({ where: { merchantId: merchant.id } }),
        settlementAmount: await prisma.settlementItem.aggregate({
          where: { merchantId: merchant.id },
          _sum: { amount: true },
        }),
        barOrders: await prisma.barOrder.count({ where: { merchantId: merchant.id } }),
      })),
    );
  });

  app.get("/api/risk/rules", async () => prisma.riskRule.findMany({ orderBy: { createdAt: "desc" } }));

  app.post("/api/risk/rules", async (request, reply) => {
    const rule = await prisma.riskRule.create({ data: riskRuleSchema.parse(request.body) });
    await writeAuditEvent({
      eventType: "risk.rule.created",
      eventCategory: "risk",
      sourceType: "internal",
      summary: `创建风控规则：${rule.ruleName}`,
    });
    return reply.code(201).send(rule);
  });

  app.get("/api/risk/alerts", async () =>
    prisma.riskAlert.findMany({ orderBy: { createdAt: "desc" }, include: { rule: true } }),
  );

  app.post("/api/risk/alerts", async (request, reply) => {
    const input = riskAlertSchema.parse(request.body);
    const alert = await prisma.riskAlert.create({ data: input });
    await writeAuditEvent({
      eventType: "risk.alert.created",
      eventCategory: "risk",
      sourceType: "internal",
      result: "abnormal",
      riskLevel: alert.riskLevel,
      summary: alert.summary,
      payload: { alertId: alert.id, objectType: alert.objectType, objectId: alert.objectId },
    });
    return reply.code(201).send(alert);
  });

  app.post("/api/risk/alerts/:id/resolve", async (request) => {
    const params = request.params as { id: string };
    const body = z.object({ reason: z.string().min(1) }).parse(request.body);
    const alert = await prisma.riskAlert.update({
      where: { id: params.id },
      data: { status: "resolved" },
    });
    await writeAuditEvent({
      eventType: "risk.alert.resolved",
      eventCategory: "risk",
      sourceType: "internal",
      summary: `风险预警已处理：${body.reason}`,
      payload: { alertId: alert.id },
    });
    return alert;
  });

  app.get("/api/analytics/metric-definitions", async () =>
    prisma.metricDefinition.findMany({ orderBy: { createdAt: "desc" }, take: 200 }),
  );

  app.post("/api/analytics/metric-definitions", async (request, reply) => {
    const input = metricDefinitionSchema.parse(request.body);
    const metric = await prisma.metricDefinition.upsert({
      where: { metricCode: input.metricCode },
      update: input,
      create: input,
    });
    await writeAuditEvent({
      eventType: "analytics.metric_definition.upserted",
      eventCategory: "analytics",
      sourceType: "internal",
      summary: `指标定义保存：${metric.metricName}`,
      payload: { metricCode: metric.metricCode },
    });
    return reply.code(201).send(metric);
  });

  app.get("/api/analytics/robot", async () => {
    const tasks = await prisma.robotTask.findMany({ orderBy: { createdAt: "desc" }, take: 500 });
    const total = tasks.length;
    const signed = tasks.filter((task) => task.status === "signed").length;
    const delivered = tasks.filter((task) => task.status === "delivered").length;
    const abnormal = tasks.filter((task) => ["failed", "abnormal", "exception"].includes(task.status)).length;
    const pending = tasks.filter((task) => ["pending", "accepted", "picked_up", "running"].includes(task.status)).length;
    return {
      total,
      signed,
      delivered,
      abnormal,
      pending,
      completionRate: total === 0 ? 0 : Math.round(((signed + delivered) / total) * 10000),
      abnormalRate: total === 0 ? 0 : Math.round((abnormal / total) * 10000),
      latest: tasks.slice(0, 20),
    };
  });

  app.get("/api/ai/quality/tasks", async () =>
    prisma.aiQualityTask.findMany({ orderBy: { createdAt: "desc" }, take: 200 }),
  );

  app.post("/api/ai/quality/tasks", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const input = aiQualityTaskSchema.parse(request.body ?? {});
    const task = await prisma.aiQualityTask.create({
      data: {
        taskNo: referenceNo("ai_task"),
        taskType: input.taskType,
        objectType: input.objectType,
        objectId: input.objectId,
        createdBy: actor.id,
        status: input.processNow ? "running" : "pending",
      },
    });
    await writeAuditEvent({
      eventType: "ai.task.created",
      eventCategory: "ai",
      sourceType: "internal",
      actorId: actor.id,
      summary: `AI质检任务创建：${task.taskNo}`,
      payload: { taskType: task.taskType, objectType: task.objectType, objectId: task.objectId },
    });
    if (!input.processNow) return reply.code(201).send({ task });
    return reply.code(201).send(await completeAiQualityTask(task.id, input.objectType, input.objectId));
  });

  app.post("/api/ai/quality/tasks/:id/retry", async (request, reply) => {
    const params = request.params as { id: string };
    const task = await prisma.aiQualityTask.update({
      where: { id: params.id },
      data: { status: "running", retryCount: { increment: 1 }, errorMessage: null },
    });
    return reply.code(201).send(await completeAiQualityTask(task.id, task.objectType ?? undefined, task.objectId ?? undefined));
  });

  app.get("/api/ai/quality/results", async (request) => {
    const query = request.query as { riskLevel?: string; objectType?: string };
    return prisma.aiQualityResult.findMany({
      where: { ...(query.riskLevel ? { riskLevel: query.riskLevel } : {}), ...(query.objectType ? { objectType: query.objectType } : {}) },
      orderBy: { createdAt: "desc" },
      take: 200,
    });
  });

  app.get("/api/ai/customer-intents", async () =>
    prisma.customerIntent.findMany({ orderBy: { createdAt: "desc" }, take: 200 }),
  );

  app.post("/api/ai/staff-service-score/recompute", async (request, reply) => {
    const conversations = await prisma.wecomConversation.findMany({ take: 500 });
    const riskHits = await prisma.wecomRiskHit.findMany({ take: 1000 });
    const snapshotDate = new Date().toISOString().slice(0, 10);
    const staffIds = [...new Set(conversations.map((item) => item.staffUserId).filter(Boolean))];
    const rows = [];
    for (const staffUserId of staffIds) {
      const staffConversations = conversations.filter((item) => item.staffUserId === staffUserId);
      const conversationIds = new Set(staffConversations.map((item) => item.id));
      const staffRiskHits = riskHits.filter((hit) => hit.conversationId && conversationIds.has(hit.conversationId));
      const riskPenalty = Math.min(60, staffRiskHits.length * 12);
      const score = Math.max(0, 100 - riskPenalty);
      rows.push(await prisma.staffServiceScore.create({
        data: {
          staffUserId,
          score,
          responseScore: 100,
          qualityScore: score,
          riskPenalty,
          sampleSize: staffConversations.length,
          snapshotDate,
        },
      }));
    }
    await writeAuditEvent({
      eventType: "ai.staff_service_score.recomputed",
      eventCategory: "ai",
      sourceType: "internal",
      summary: `员工服务评分重算：${rows.length} 人`,
      payload: { snapshotDate },
    });
    return reply.code(201).send({ snapshotDate, rows });
  });

  app.get("/api/ai/staff-service-score", async () =>
    prisma.staffServiceScore.findMany({ orderBy: { createdAt: "desc" }, take: 200 }),
  );

  app.post("/api/analytics/merchant-health/recompute", async (request, reply) => {
    const merchants = await prisma.merchant.findMany({ take: 200 });
    const snapshotDate = new Date().toISOString().slice(0, 10);
    const rows = [];
    for (const merchant of merchants) {
      const [barOrders, verifications, orderSyncs] = await Promise.all([
        prisma.barOrder.count({ where: { merchantId: merchant.id } }),
        prisma.verificationRecord.count({ where: { merchantId: merchant.id } }),
        prisma.thirdPartyOrderSync.count({ where: { merchantId: merchant.id } }),
      ]);
      const operationsScore = Math.min(100, 60 + barOrders + verifications + orderSyncs);
      const financeScore = merchant.settlementStatus === "normal" ? 100 : 70;
      const riskScore = merchant.riskLevel === "normal" ? 100 : merchant.riskLevel === "high" ? 45 : 70;
      const serviceScore = 90;
      const score = Math.round((operationsScore + financeScore + riskScore + serviceScore) / 4);
      rows.push(await prisma.merchantHealthScore.create({
        data: {
          merchantId: merchant.id,
          score,
          operationsScore,
          financeScore,
          riskScore,
          serviceScore,
          snapshotDate,
          summary: `${merchant.displayName} 健康评分 ${score}`,
        },
      }));
    }
    await writeAuditEvent({
      eventType: "analytics.merchant_health.recomputed",
      eventCategory: "analytics",
      sourceType: "internal",
      summary: `商户健康评分重算：${rows.length} 家`,
      payload: { snapshotDate },
    });
    return reply.code(201).send({ snapshotDate, rows });
  });

  app.get("/api/analytics/merchant-health", async () =>
    prisma.merchantHealthScore.findMany({ orderBy: { createdAt: "desc" }, take: 200 }),
  );

  app.post("/api/analytics/metric-anomalies/explain", async (request, reply) => {
    const input = anomalyExplainSchema.parse(request.body);
    const result = explainMetricChange(input);
    const explanation = await prisma.metricAnomalyExplanation.create({
      data: { ...input, ...result },
    });
    await writeAuditEvent({
      eventType: "analytics.metric_anomaly.explained",
      eventCategory: "analytics",
      sourceType: "internal",
      result: result.severity === "normal" ? "success" : "abnormal",
      riskLevel: result.severity === "high" ? "high" : "normal",
      summary: explanation.explanation,
      payload: { metricCode: input.metricCode, changeRate: result.changeRate },
    });
    return reply.code(201).send(explanation);
  });

  app.get("/api/analytics/metric-anomalies", async () =>
    prisma.metricAnomalyExplanation.findMany({ orderBy: { createdAt: "desc" }, take: 200 }),
  );
}
