import { randomBytes } from "node:crypto";
import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { prisma } from "../../lib/prisma.js";
import { assertMerchantAccess, requireCurrentUser, scopedMerchantId } from "../../lib/security.js";
import { writeAuditEvent } from "../events/service.js";

const productSchema = z.object({
  productName: z.string().min(1),
  productType: z.string().default("package"),
  priceAmount: z.number().int().nonnegative().default(0),
  currency: z.string().default("CNY"),
  status: z.string().default("draft"),
  description: z.string().optional(),
});

const templateSchema = z.object({
  name: z.string().min(1),
  entitlementType: z.string().min(1),
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  projectId: z.string().optional(),
  defaultTotalTimes: z.number().int().positive().default(1),
  defaultValueAmount: z.number().int().nonnegative().default(0),
  description: z.string().optional(),
});

const bindEntitlementSchema = z.object({
  entitlementTemplateId: z.string().min(1),
  quantity: z.number().int().positive().default(1),
  totalTimes: z.number().int().positive().default(1),
  valueAmount: z.number().int().nonnegative().default(0),
  sortOrder: z.number().int().default(0),
});

const issueSchema = z.object({
  purchaserName: z.string().optional(),
  purchaserPhone: z.string().optional(),
});

const previewSchema = z.object({
  entitlementCode: z.string().min(1),
});

const verifySchema = z.object({
  entitlementCode: z.string().min(1),
  staffId: z.string().optional(),
  deviceId: z.string().optional(),
  source: z.string().default("backend"),
});

const settlementBillSchema = z.object({
  merchantId: z.string().optional(),
});

function code(prefix: string) {
  return `${prefix}_${randomBytes(6).toString("hex")}`;
}

async function previewEntitlement(entitlementCode: string) {
  const entitlement = await prisma.issuedEntitlement.findUnique({ where: { entitlementCode } });

  if (!entitlement) {
    return { allowed: false, reason: "ENTITLEMENT_NOT_FOUND" as const };
  }

  const now = new Date();
  if (entitlement.status !== "active") {
    return { allowed: false, reason: "ENTITLEMENT_NOT_ACTIVE" as const, entitlement };
  }
  if (entitlement.validFrom && entitlement.validFrom > now) {
    return { allowed: false, reason: "NOT_STARTED" as const, entitlement };
  }
  if (entitlement.validTo && entitlement.validTo < now) {
    return { allowed: false, reason: "EXPIRED" as const, entitlement };
  }
  if (entitlement.usedTimes >= entitlement.totalTimes) {
    return { allowed: false, reason: "NO_REMAINING_TIMES" as const, entitlement };
  }

  return { allowed: true, reason: "OK" as const, entitlement };
}

async function createVerificationRiskAlert(entitlementCode: string, reason: string) {
  const rule = await prisma.riskRule.findFirst({
    where: { ruleCode: "P0_VERIFICATION_FAIL", status: "active" },
  });

  return prisma.riskAlert.create({
    data: {
      ruleId: rule?.id,
      alertType: "verification_failed",
      riskLevel: "suspicious",
      objectType: "entitlement",
      objectId: entitlementCode,
      summary: `核销失败：${entitlementCode} / ${reason}`,
    },
  });
}

export async function registerTicketingRoutes(app: FastifyInstance) {
  app.get("/api/admin/ticket-products", async () =>
    prisma.ticketProduct.findMany({
      orderBy: { createdAt: "desc" },
      include: { productEntitlements: { include: { entitlementTemplate: true } } },
    }),
  );

  app.post("/api/admin/ticket-products", async (request, reply) => {
    const input = productSchema.parse(request.body);
    const product = await prisma.ticketProduct.create({
      data: { ...input, productCode: code("tp") },
    });
    await writeAuditEvent({
      eventType: "ticket.product.created",
      eventCategory: "ticketing",
      sourceType: "internal",
      summary: `创建票种：${product.productName}`,
      payload: { productId: product.id },
    });
    return reply.code(201).send(product);
  });

  app.post("/api/admin/ticket-products/:id/publish", async (request) => {
    const params = request.params as { id: string };
    return prisma.ticketProduct.update({ where: { id: params.id }, data: { status: "active" } });
  });

  app.get("/api/admin/entitlement-templates", async () =>
    prisma.entitlementTemplate.findMany({ orderBy: { createdAt: "desc" } }),
  );

  app.post("/api/admin/entitlement-templates", async (request, reply) => {
    const template = await prisma.entitlementTemplate.create({ data: templateSchema.parse(request.body) });
    await writeAuditEvent({
      eventType: "entitlement.template.created",
      eventCategory: "ticketing",
      sourceType: "internal",
      merchantId: template.merchantId ?? undefined,
      summary: `创建权益模板：${template.name}`,
    });
    return reply.code(201).send(template);
  });

  app.post("/api/admin/ticket-products/:id/entitlements", async (request, reply) => {
    const params = request.params as { id: string };
    const input = bindEntitlementSchema.parse(request.body);
    const relation = await prisma.productEntitlement.create({
      data: {
        ticketProductId: params.id,
        entitlementTemplateId: input.entitlementTemplateId,
        quantity: input.quantity,
        totalTimes: input.totalTimes,
        valueAmount: input.valueAmount,
        sortOrder: input.sortOrder,
      },
      include: { entitlementTemplate: true },
    });
    return reply.code(201).send(relation);
  });

  app.post("/api/admin/ticket-products/:id/issue", async (request, reply) => {
    const params = request.params as { id: string };
    const input = issueSchema.parse(request.body);
    const product = await prisma.ticketProduct.findUnique({
      where: { id: params.id },
      include: { productEntitlements: { include: { entitlementTemplate: true } } },
    });

    if (!product || product.productEntitlements.length === 0) {
      return reply.code(400).send({ error: "PRODUCT_NOT_READY" });
    }

    const issued = await prisma.issuedTicket.create({
      data: {
        ticketCode: code("ticket"),
        ticketProductId: product.id,
        purchaserName: input.purchaserName,
        purchaserPhone: input.purchaserPhone,
        issuedEntitlements: {
          create: product.productEntitlements.flatMap((item) =>
            Array.from({ length: item.quantity }).map(() => ({
              entitlementCode: code("ent"),
              entitlementTemplateId: item.entitlementTemplateId,
              entitlementType: item.entitlementTemplate.entitlementType,
              merchantId: item.entitlementTemplate.merchantId,
              venueId: item.entitlementTemplate.venueId,
              projectId: item.entitlementTemplate.projectId,
              totalTimes: item.totalTimes,
              valueAmount: item.valueAmount,
              status: "active",
            })),
          ),
        },
      },
      include: { issuedEntitlements: true, ticketProduct: true },
    });

    await writeAuditEvent({
      eventType: "ticket.issued",
      eventCategory: "ticketing",
      sourceType: "internal",
      ticketId: issued.id,
      summary: `出票：${issued.ticketCode}`,
      payload: { productId: product.id, entitlementCount: issued.issuedEntitlements.length },
    });

    return reply.code(201).send(issued);
  });

  app.get("/api/admin/issued-entitlements", async () =>
    prisma.issuedEntitlement.findMany({ orderBy: { createdAt: "desc" }, take: 200 }),
  );

  app.post("/api/verifications/preview", async (request) => {
    const input = previewSchema.parse(request.body);
    const preview = await previewEntitlement(input.entitlementCode);
    if (preview.entitlement) assertMerchantAccess(request, preview.entitlement.merchantId);
    return preview;
  });

  app.post("/api/verifications", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const input = verifySchema.parse(request.body);
    const preview = await previewEntitlement(input.entitlementCode);

    if (!preview.allowed || !preview.entitlement) {
      const alert = await createVerificationRiskAlert(input.entitlementCode, preview.reason);
      await writeAuditEvent({
        eventType: "verification.failed",
        eventCategory: "ticketing",
        sourceType: "internal",
        result: "failed",
        riskLevel: "suspicious",
        summary: `核销失败：${input.entitlementCode}`,
        payload: { reason: preview.reason, riskAlertId: alert.id },
      });
      return reply.code(400).send(preview);
    }
    assertMerchantAccess(request, preview.entitlement.merchantId);

    const verificationNo = code("ver");

    const verification = await prisma.$transaction(async (tx) => {
      const updated = await tx.issuedEntitlement.updateMany({
        where: {
          id: preview.entitlement.id,
          status: "active",
          usedTimes: { lt: preview.entitlement.totalTimes },
        },
        data: {
          usedTimes: { increment: 1 },
        },
      });
      if (updated.count !== 1) {
        throw new Error("ENTITLEMENT_ALREADY_USED");
      }

      const current = await tx.issuedEntitlement.findUnique({ where: { id: preview.entitlement.id } });
      if (current && current.usedTimes >= current.totalTimes) {
        await tx.issuedEntitlement.update({ where: { id: preview.entitlement.id }, data: { status: "used" } });
      }

      const record = await tx.verificationRecord.create({
        data: {
          verificationNo,
          issuedEntitlementId: preview.entitlement.id,
          merchantId: preview.entitlement.merchantId,
          venueId: preview.entitlement.venueId,
          projectId: preview.entitlement.projectId,
          staffId: actor.id,
          deviceId: input.deviceId,
          source: input.source,
          status: "success",
          resultReason: "OK",
        },
      });

      await tx.settlementItem.create({
        data: {
          verificationRecordId: record.id,
          merchantId: preview.entitlement.merchantId,
          amount: preview.entitlement.valueAmount,
          status: "pending",
        },
      });

      return record;
    });

    await writeAuditEvent({
      eventType: "verification.succeeded",
      eventCategory: "ticketing",
      sourceType: "internal",
      merchantId: preview.entitlement.merchantId ?? undefined,
      ticketId: preview.entitlement.issuedTicketId,
      entitlementId: preview.entitlement.id,
      actorType: "staff",
      actorId: actor.id,
      deviceId: input.deviceId,
      summary: `核销成功：${input.entitlementCode}`,
      payload: { verificationNo },
    });

    return reply.code(201).send(verification);
  });

  app.get("/api/admin/verifications", async () =>
    prisma.verificationRecord.findMany({ orderBy: { createdAt: "desc" }, take: 200 }),
  );

  app.get("/api/merchant/settlement-items", async (request) => {
    const merchantId = scopedMerchantId(request, (request.query as { merchantId?: string }).merchantId);
    return prisma.settlementItem.findMany({ where: { merchantId }, orderBy: { createdAt: "desc" }, take: 200 });
  });

  app.get("/api/merchant/settlement-bills", async (request) => {
    const merchantId = scopedMerchantId(request, (request.query as { merchantId?: string }).merchantId);
    return prisma.settlementBill.findMany({ where: { merchantId }, orderBy: { createdAt: "desc" }, take: 200 });
  });

  app.post("/api/merchant/settlement-bills/generate", async (request, reply) => {
    const input = settlementBillSchema.parse(request.body ?? {});
    const scopedMerchant = scopedMerchantId(request, input.merchantId);
    const pendingItems = await prisma.settlementItem.findMany({
      where: {
        merchantId: scopedMerchant,
        status: "pending",
      },
      take: 1000,
    });

    if (pendingItems.length === 0) {
      return reply.code(400).send({ error: "NO_PENDING_SETTLEMENT_ITEMS" });
    }

    const totalAmount = pendingItems.reduce((sum, item) => sum + item.amount, 0);
    const merchantId = input.merchantId ?? pendingItems[0]?.merchantId ?? undefined;
    const bill = await prisma.$transaction(async (tx) => {
      const created = await tx.settlementBill.create({
        data: {
          billNo: code("settle_bill"),
          merchantId,
          totalAmount,
          status: "generated",
        },
      });
      await tx.settlementItem.updateMany({
        where: { id: { in: pendingItems.map((item) => item.id) } },
        data: { status: "billed" },
      });
      return created;
    });

    await writeAuditEvent({
      eventType: "settlement.bill.generated",
      eventCategory: "settlement",
      sourceType: "internal",
      merchantId,
      summary: `生成结算单：${bill.billNo}`,
      payload: { itemCount: pendingItems.length, totalAmount },
    });

    return reply.code(201).send(bill);
  });
}
