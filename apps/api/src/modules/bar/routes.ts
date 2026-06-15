import { randomBytes } from "node:crypto";
import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { prisma } from "../../lib/prisma.js";
import { requireCurrentUser, scopedMerchantId } from "../../lib/security.js";
import { writeAuditEvent } from "../events/service.js";

const tableSchema = z.object({
  tableName: z.string().min(1),
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  capacity: z.number().int().optional(),
  minSpendAmount: z.number().int().nonnegative().default(0),
});

const productSchema = z.object({
  productName: z.string().min(1),
  category: z.string().default("drink"),
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  priceAmount: z.number().int().nonnegative().default(0),
});

const orderSchema = z.object({
  tableId: z.string().min(1),
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
});

const orderItemSchema = z.object({
  productId: z.string().min(1),
  quantity: z.number().int().positive().default(1),
});

const updateOrderItemSchema = z.object({
  quantity: z.number().int().nonnegative(),
  reason: z.string().optional(),
});

const paySchema = z.object({
  paidAmount: z.number().int().nonnegative(),
  paymentMethod: z.string().default("offline"),
});

const inventoryFlowSchema = z.object({
  productId: z.string().optional(),
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  flowType: z.string().min(1),
  quantity: z.number().int(),
  reason: z.string().optional(),
  operatorId: z.string().optional(),
});

const inventoryItemSchema = z.object({
  itemName: z.string().min(1),
  itemCode: z.string().optional(),
  category: z.string().default("drink"),
  unit: z.string().default("bottle"),
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  minQuantity: z.number().int().nonnegative().default(0),
});

const stockChangeSchema = z.object({
  itemId: z.string().min(1),
  productId: z.string().optional(),
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  quantity: z.number().int().positive(),
  locationType: z.string().default("warehouse_stock"),
  reason: z.string().optional(),
});

const stockTransferSchema = z.object({
  itemId: z.string().min(1),
  productId: z.string().optional(),
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  quantity: z.number().int().positive(),
  fromLocationType: z.string().default("warehouse_stock"),
  toLocationType: z.string().default("bar_stock"),
  reason: z.string().optional(),
});

const stockCountSchema = z.object({
  itemId: z.string().min(1),
  productId: z.string().optional(),
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  countedQuantity: z.number().int().nonnegative(),
  locationType: z.string().default("warehouse_stock"),
  reason: z.string().optional(),
});

const savedBottleSchema = z.object({
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  productId: z.string().optional(),
  customerName: z.string().min(1),
  customerPhone: z.string().optional(),
  storageLocation: z.string().optional(),
  initialUnit: z.enum(["percent", "bottle", "ml"]).default("percent"),
  initialValue: z.number().int().positive().default(100),
  remainingValue: z.number().int().positive().optional(),
  expiresAt: z.string().optional(),
  sourceOrderId: z.string().optional(),
  sourceBillId: z.string().optional(),
});

const savedBottlePickupSchema = z.object({
  quantity: z.number().int().positive(),
  reason: z.string().optional(),
});

const savedBottleTransferSchema = z.object({
  customerName: z.string().min(1),
  customerPhone: z.string().optional(),
  reason: z.string().optional(),
});

const reasonSchema = z.object({
  reason: z.string().min(1),
});

const productionActionSchema = z.object({
  reason: z.string().optional(),
});

const reservationSchema = z.object({
  merchantId: z.string().optional(),
  venueId: z.string().optional(),
  tableId: z.string().optional(),
  customerName: z.string().min(1),
  customerPhone: z.string().optional(),
  reservedAt: z.string().datetime(),
  partySize: z.number().int().positive().default(1),
  depositAmount: z.number().int().nonnegative().default(0),
  remark: z.string().optional(),
});

function code(prefix: string) {
  return `${prefix}_${randomBytes(6).toString("hex")}`;
}

function productionDepartment(category?: string) {
  return category === "food" || category === "snack" ? "kitchen" : "bar";
}

async function getOrCreateBalance(
  tx: any,
  input: { itemId: string; merchantId?: string; venueId?: string; locationType: string },
) {
  const where = {
    itemId: input.itemId,
    merchantId: input.merchantId ?? null,
    venueId: input.venueId ?? null,
    locationType: input.locationType,
  };
  const existing = await tx.barStockBalance.findFirst({ where });
  if (existing) return existing;
  return tx.barStockBalance.create({ data: { ...where, quantity: 0 } });
}

async function adjustBalance(
  tx: any,
  input: { itemId: string; merchantId?: string; venueId?: string; locationType: string; delta: number },
) {
  const balance = await getOrCreateBalance(tx, input);
  const nextQuantity = balance.quantity + input.delta;
  if (nextQuantity < 0) {
    throw new Error("STOCK_NOT_ENOUGH");
  }
  const updated = await tx.barStockBalance.update({ where: { id: balance.id }, data: { quantity: nextQuantity } });
  return { before: balance.quantity, after: updated.quantity };
}

function parseOptionalDate(value?: string) {
  if (!value) return undefined;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) throw new Error("INVALID_DATE");
  return date;
}

export async function registerBarRoutes(app: FastifyInstance) {
  app.get("/api/admin/bar/tables", async () => prisma.barTable.findMany({ orderBy: { createdAt: "desc" } }));

  app.post("/api/admin/bar/tables", async (request, reply) => {
    const input = tableSchema.parse(request.body);
    const table = await prisma.barTable.create({
      data: { ...input, tableCode: code("bar_table") },
    });
    await writeAuditEvent({
      eventType: "bar.table.created",
      eventCategory: "bar",
      sourceType: "internal",
      merchantId: table.merchantId ?? undefined,
      summary: `创建酒吧桌台：${table.tableName}`,
    });
    return reply.code(201).send(table);
  });

  app.get("/api/admin/bar/products", async () => prisma.barProduct.findMany({ orderBy: { createdAt: "desc" } }));

  app.post("/api/admin/bar/products", async (request, reply) => {
    const product = await prisma.barProduct.create({ data: productSchema.parse(request.body) });
    return reply.code(201).send(product);
  });

  app.get("/api/admin/bar/reservations", async (request) => {
    const query = request.query as { merchantId?: string; status?: string };
    const merchantId = scopedMerchantId(request, query.merchantId);
    return prisma.barReservation.findMany({
      where: { ...(merchantId ? { merchantId } : {}), ...(query.status ? { status: query.status } : {}) },
      orderBy: { reservedAt: "desc" },
      take: 200,
    });
  });

  app.post("/api/admin/bar/reservations", async (request, reply) => {
    const user = requireCurrentUser(request);
    const input = reservationSchema.parse(request.body);
    const merchantId = scopedMerchantId(request, input.merchantId);
    const reservation = await prisma.barReservation.create({
      data: {
        reservationNo: code("bar_res"),
        merchantId,
        venueId: input.venueId,
        tableId: input.tableId,
        customerName: input.customerName,
        customerPhone: input.customerPhone,
        reservedAt: new Date(input.reservedAt),
        partySize: input.partySize,
        depositAmount: input.depositAmount,
        remark: input.remark,
      },
    });
    await writeAuditEvent({
      eventType: "bar.reservation.created",
      eventCategory: "bar",
      sourceType: "internal",
      merchantId: reservation.merchantId ?? undefined,
      actorId: user.id,
      summary: `酒吧预订：${reservation.customerName}`,
      payload: { reservationId: reservation.id, reservedAt: reservation.reservedAt.toISOString(), depositAmount: reservation.depositAmount },
    });
    return reply.code(201).send(reservation);
  });

  app.post("/api/admin/bar/reservations/:id/check-in", async (request, reply) => {
    const user = requireCurrentUser(request);
    const params = request.params as { id: string };
    const reservation = await prisma.barReservation.findUnique({ where: { id: params.id } });
    if (!reservation) return reply.code(404).send({ error: "RESERVATION_NOT_FOUND" });
    scopedMerchantId(request, reservation.merchantId);
    if (reservation.status !== "reserved") return reply.code(400).send({ error: "RESERVATION_NOT_AVAILABLE" });
    const updated = await prisma.barReservation.update({ where: { id: params.id }, data: { status: "checked_in" } });
    await writeAuditEvent({
      eventType: "bar.reservation.checked_in",
      eventCategory: "bar",
      sourceType: "internal",
      merchantId: updated.merchantId ?? undefined,
      actorId: user.id,
      summary: `酒吧预订到店：${updated.customerName}`,
      payload: { reservationId: updated.id },
    });
    return reply.code(201).send(updated);
  });

  app.post("/api/admin/bar/reservations/:id/cancel", async (request, reply) => {
    const user = requireCurrentUser(request);
    const params = request.params as { id: string };
    const input = productionActionSchema.parse(request.body ?? {});
    const reservation = await prisma.barReservation.findUnique({ where: { id: params.id } });
    if (!reservation) return reply.code(404).send({ error: "RESERVATION_NOT_FOUND" });
    scopedMerchantId(request, reservation.merchantId);
    const updated = await prisma.barReservation.update({ where: { id: params.id }, data: { status: "cancelled", remark: input.reason ?? reservation.remark } });
    await writeAuditEvent({
      eventType: "bar.reservation.cancelled",
      eventCategory: "bar",
      sourceType: "internal",
      merchantId: updated.merchantId ?? undefined,
      actorId: user.id,
      summary: `酒吧预订取消：${updated.customerName}`,
      payload: { reservationId: updated.id, reason: input.reason },
    });
    return reply.code(201).send(updated);
  });

  app.get("/api/admin/bar/inventory-flows", async () =>
    prisma.barInventoryFlow.findMany({ orderBy: { createdAt: "desc" }, take: 200, include: { product: true } }),
  );

  app.post("/api/admin/bar/inventory-flows", async (request, reply) => {
    const input = inventoryFlowSchema.parse(request.body);
    const flow = await prisma.barInventoryFlow.create({ data: input, include: { product: true } });
    await writeAuditEvent({
      eventType: "bar.inventory.flow_created",
      eventCategory: "bar",
      sourceType: "internal",
      merchantId: flow.merchantId ?? undefined,
      actorId: flow.operatorId ?? undefined,
      summary: `酒吧库存流水：${flow.flowType} ${flow.quantity}`,
      payload: { productId: flow.productId, reason: flow.reason },
    });
    return reply.code(201).send(flow);
  });

  app.get("/api/admin/bar/inventory-items", async (request) => {
    const merchantId = scopedMerchantId(request, (request.query as { merchantId?: string }).merchantId);
    return prisma.barInventoryItem.findMany({
      where: { ...(merchantId ? { merchantId } : {}) },
      orderBy: { createdAt: "desc" },
      take: 200,
    });
  });

  app.post("/api/admin/bar/inventory-items", async (request, reply) => {
    const user = requireCurrentUser(request);
    const input = inventoryItemSchema.parse(request.body);
    const merchantId = scopedMerchantId(request, input.merchantId);
    const item = await prisma.barInventoryItem.create({
      data: {
        itemCode: input.itemCode ?? code("bar_item"),
        itemName: input.itemName,
        category: input.category,
        unit: input.unit,
        merchantId,
        venueId: input.venueId,
        minQuantity: input.minQuantity,
      },
    });
    await writeAuditEvent({
      eventType: "bar.inventory.item_created",
      eventCategory: "bar",
      sourceType: "internal",
      merchantId: item.merchantId ?? undefined,
      actorId: user.id,
      summary: `创建酒吧库存物料：${item.itemName}`,
    });
    return reply.code(201).send(item);
  });

  app.get("/api/admin/bar/stock-balances", async (request) => {
    const query = request.query as { merchantId?: string; itemId?: string; locationType?: string };
    const merchantId = scopedMerchantId(request, query.merchantId);
    return prisma.barStockBalance.findMany({
      where: {
        ...(merchantId ? { merchantId } : {}),
        ...(query.itemId ? { itemId: query.itemId } : {}),
        ...(query.locationType ? { locationType: query.locationType } : {}),
      },
      include: { item: true },
      orderBy: { updatedAt: "desc" },
      take: 200,
    });
  });

  app.get("/api/admin/bar/stock-movements", async (request) => {
    const query = request.query as { merchantId?: string; itemId?: string };
    const merchantId = scopedMerchantId(request, query.merchantId);
    return prisma.barStockMovement.findMany({
      where: { ...(merchantId ? { merchantId } : {}), ...(query.itemId ? { itemId: query.itemId } : {}) },
      include: { item: true, product: true },
      orderBy: { createdAt: "desc" },
      take: 200,
    });
  });

  app.post("/api/admin/bar/stock-movements/inbound", async (request, reply) => {
    const user = requireCurrentUser(request);
    const input = stockChangeSchema.parse(request.body);
    const merchantId = scopedMerchantId(request, input.merchantId);
    const movement = await prisma.$transaction(async (tx) => {
      const to = await adjustBalance(tx, {
        itemId: input.itemId,
        merchantId,
        venueId: input.venueId,
        locationType: input.locationType,
        delta: input.quantity,
      });
      return tx.barStockMovement.create({
        data: {
          movementNo: code("bar_stock"),
          merchantId,
          venueId: input.venueId,
          itemId: input.itemId,
          productId: input.productId,
          movementType: "inbound",
          quantity: input.quantity,
          toLocationType: input.locationType,
          beforeToQuantity: to.before,
          afterToQuantity: to.after,
          reason: input.reason,
          operatorId: user.id,
        },
        include: { item: true, product: true },
      });
    });
    await writeAuditEvent({
      eventType: "bar.stock.inbound_confirmed",
      eventCategory: "bar",
      sourceType: "internal",
      merchantId: movement.merchantId ?? undefined,
      actorId: user.id,
      summary: `酒吧入库：${movement.item.itemName} x ${movement.quantity}`,
      payload: { itemId: movement.itemId, locationType: input.locationType, reason: input.reason },
    });
    return reply.code(201).send(movement);
  });

  app.post("/api/admin/bar/stock-movements/outbound", async (request, reply) => {
    const user = requireCurrentUser(request);
    const input = stockChangeSchema.parse(request.body);
    const merchantId = scopedMerchantId(request, input.merchantId);
    const movement = await prisma.$transaction(async (tx) => {
      const from = await adjustBalance(tx, {
        itemId: input.itemId,
        merchantId,
        venueId: input.venueId,
        locationType: input.locationType,
        delta: -input.quantity,
      });
      return tx.barStockMovement.create({
        data: {
          movementNo: code("bar_stock"),
          merchantId,
          venueId: input.venueId,
          itemId: input.itemId,
          productId: input.productId,
          movementType: "outbound",
          quantity: input.quantity,
          fromLocationType: input.locationType,
          beforeFromQuantity: from.before,
          afterFromQuantity: from.after,
          reason: input.reason,
          operatorId: user.id,
        },
        include: { item: true, product: true },
      });
    });
    await writeAuditEvent({
      eventType: "bar.stock.outbound_confirmed",
      eventCategory: "bar",
      sourceType: "internal",
      merchantId: movement.merchantId ?? undefined,
      actorId: user.id,
      summary: `酒吧出库：${movement.item.itemName} x ${movement.quantity}`,
      payload: { itemId: movement.itemId, locationType: input.locationType, reason: input.reason },
    });
    return reply.code(201).send(movement);
  });

  app.post("/api/admin/bar/stock-movements/damage", async (request, reply) => {
    const user = requireCurrentUser(request);
    const input = stockChangeSchema.parse(request.body);
    const merchantId = scopedMerchantId(request, input.merchantId);
    const movement = await prisma.$transaction(async (tx) => {
      const from = await adjustBalance(tx, {
        itemId: input.itemId,
        merchantId,
        venueId: input.venueId,
        locationType: input.locationType,
        delta: -input.quantity,
      });
      return tx.barStockMovement.create({
        data: {
          movementNo: code("bar_stock"),
          merchantId,
          venueId: input.venueId,
          itemId: input.itemId,
          productId: input.productId,
          movementType: "damage",
          quantity: input.quantity,
          fromLocationType: input.locationType,
          beforeFromQuantity: from.before,
          afterFromQuantity: from.after,
          reason: input.reason,
          operatorId: user.id,
        },
        include: { item: true, product: true },
      });
    });
    await writeAuditEvent({
      eventType: "bar.stock.damaged",
      eventCategory: "bar",
      sourceType: "internal",
      merchantId: movement.merchantId ?? undefined,
      actorId: user.id,
      summary: `酒吧报损：${movement.item.itemName} x ${movement.quantity}`,
      payload: { itemId: movement.itemId, locationType: input.locationType, reason: input.reason },
    });
    return reply.code(201).send(movement);
  });

  app.post("/api/admin/bar/stock-movements/transfer", async (request, reply) => {
    const user = requireCurrentUser(request);
    const input = stockTransferSchema.parse(request.body);
    const merchantId = scopedMerchantId(request, input.merchantId);
    if (input.fromLocationType === input.toLocationType) return reply.code(400).send({ error: "SAME_LOCATION" });
    const movement = await prisma.$transaction(async (tx) => {
      const from = await adjustBalance(tx, {
        itemId: input.itemId,
        merchantId,
        venueId: input.venueId,
        locationType: input.fromLocationType,
        delta: -input.quantity,
      });
      const to = await adjustBalance(tx, {
        itemId: input.itemId,
        merchantId,
        venueId: input.venueId,
        locationType: input.toLocationType,
        delta: input.quantity,
      });
      return tx.barStockMovement.create({
        data: {
          movementNo: code("bar_stock"),
          merchantId,
          venueId: input.venueId,
          itemId: input.itemId,
          productId: input.productId,
          movementType: "transfer",
          quantity: input.quantity,
          fromLocationType: input.fromLocationType,
          toLocationType: input.toLocationType,
          beforeFromQuantity: from.before,
          afterFromQuantity: from.after,
          beforeToQuantity: to.before,
          afterToQuantity: to.after,
          reason: input.reason,
          operatorId: user.id,
        },
        include: { item: true, product: true },
      });
    });
    await writeAuditEvent({
      eventType: "bar.stock.transferred",
      eventCategory: "bar",
      sourceType: "internal",
      merchantId: movement.merchantId ?? undefined,
      actorId: user.id,
      summary: `酒吧调拨：${movement.item.itemName} x ${movement.quantity}`,
      payload: { itemId: movement.itemId, from: input.fromLocationType, to: input.toLocationType, reason: input.reason },
    });
    return reply.code(201).send(movement);
  });

  app.post("/api/admin/bar/stock-counts", async (request, reply) => {
    const user = requireCurrentUser(request);
    const input = stockCountSchema.parse(request.body);
    const merchantId = scopedMerchantId(request, input.merchantId);
    const movement = await prisma.$transaction(async (tx) => {
      const balance = await getOrCreateBalance(tx, {
        itemId: input.itemId,
        merchantId,
        venueId: input.venueId,
        locationType: input.locationType,
      });
      const updated = await tx.barStockBalance.update({ where: { id: balance.id }, data: { quantity: input.countedQuantity } });
      return tx.barStockMovement.create({
        data: {
          movementNo: code("bar_stock"),
          merchantId,
          venueId: input.venueId,
          itemId: input.itemId,
          productId: input.productId,
          movementType: "count",
          quantity: input.countedQuantity - balance.quantity,
          toLocationType: input.locationType,
          beforeToQuantity: balance.quantity,
          afterToQuantity: updated.quantity,
          reason: input.reason,
          operatorId: user.id,
        },
        include: { item: true, product: true },
      });
    });
    await writeAuditEvent({
      eventType: "bar.stock.counted",
      eventCategory: "bar",
      sourceType: "internal",
      merchantId: movement.merchantId ?? undefined,
      actorId: user.id,
      summary: `酒吧盘点：${movement.item.itemName} -> ${movement.afterToQuantity}`,
      payload: { itemId: movement.itemId, locationType: input.locationType, reason: input.reason },
    });
    return reply.code(201).send(movement);
  });

  app.get("/api/admin/bar/saved-bottles", async (request) => {
    const query = request.query as { merchantId?: string; customerPhone?: string; status?: string };
    const merchantId = scopedMerchantId(request, query.merchantId);
    return prisma.savedBottle.findMany({
      where: {
        ...(merchantId ? { merchantId } : {}),
        ...(query.customerPhone ? { customerPhone: query.customerPhone } : {}),
        ...(query.status ? { status: query.status } : {}),
      },
      include: { product: true, movements: { orderBy: { createdAt: "desc" }, take: 5 } },
      orderBy: { createdAt: "desc" },
      take: 200,
    });
  });

  app.post("/api/admin/bar/saved-bottles", async (request, reply) => {
    const user = requireCurrentUser(request);
    const input = savedBottleSchema.parse(request.body);
    const merchantId = scopedMerchantId(request, input.merchantId);
    const initialValue = input.initialValue;
    const remainingValue = input.remainingValue ?? initialValue;
    if (remainingValue > initialValue) return reply.code(400).send({ error: "INVALID_REMAINING_VALUE" });
    const savedBottle = await prisma.$transaction(async (tx) => {
      const created = await tx.savedBottle.create({
        data: {
          savedNo: code("saved_bottle"),
          merchantId,
          venueId: input.venueId,
          productId: input.productId,
          customerName: input.customerName,
          customerPhone: input.customerPhone,
          storageLocation: input.storageLocation,
          initialUnit: input.initialUnit,
          initialValue,
          remainingUnit: input.initialUnit,
          remainingValue,
          expiresAt: parseOptionalDate(input.expiresAt),
          sourceOrderId: input.sourceOrderId,
          sourceBillId: input.sourceBillId,
          status: remainingValue === 0 ? "finished" : "stored",
        },
      });
      await tx.savedBottleMovement.create({
        data: {
          movementNo: code("saved_bottle_move"),
          savedBottleId: created.id,
          movementType: "created",
          quantity: remainingValue,
          beforeRemainingValue: 0,
          afterRemainingValue: remainingValue,
          operatorId: user.id,
          reason: "create saved bottle",
        },
      });
      return tx.savedBottle.findUnique({
        where: { id: created.id },
        include: { product: true, movements: { orderBy: { createdAt: "desc" }, take: 5 } },
      });
    });
    await writeAuditEvent({
      eventType: "bar.saved_bottle.created",
      eventCategory: "bar",
      sourceType: "internal",
      merchantId: savedBottle?.merchantId ?? undefined,
      actorId: user.id,
      summary: `存酒登记：${savedBottle?.customerName}`,
      payload: { savedBottleId: savedBottle?.id, productId: input.productId, remainingValue },
    });
    return reply.code(201).send(savedBottle);
  });

  app.post("/api/admin/bar/saved-bottles/:id/pickup", async (request, reply) => {
    const user = requireCurrentUser(request);
    const params = request.params as { id: string };
    const input = savedBottlePickupSchema.parse(request.body);
    const savedBottle = await prisma.$transaction(async (tx) => {
      const before = await tx.savedBottle.findUnique({ where: { id: params.id } });
      if (!before) throw new Error("SAVED_BOTTLE_NOT_FOUND");
      scopedMerchantId(request, before.merchantId);
      if (!["stored", "partially_used"].includes(before.status)) throw new Error("SAVED_BOTTLE_NOT_AVAILABLE");
      if (before.remainingValue < input.quantity) throw new Error("SAVED_BOTTLE_NOT_ENOUGH");
      const afterRemainingValue = before.remainingValue - input.quantity;
      const status = afterRemainingValue === 0 ? "finished" : "partially_used";
      await tx.savedBottle.update({
        where: { id: params.id },
        data: { remainingValue: afterRemainingValue, status },
      });
      await tx.savedBottleMovement.create({
        data: {
          movementNo: code("saved_bottle_move"),
          savedBottleId: params.id,
          movementType: "picked_up",
          quantity: input.quantity,
          beforeRemainingValue: before.remainingValue,
          afterRemainingValue,
          operatorId: user.id,
          reason: input.reason,
        },
      });
      return tx.savedBottle.findUnique({
        where: { id: params.id },
        include: { product: true, movements: { orderBy: { createdAt: "desc" }, take: 5 } },
      });
    });
    await writeAuditEvent({
      eventType: "bar.saved_bottle.picked_up",
      eventCategory: "bar",
      sourceType: "internal",
      merchantId: savedBottle?.merchantId ?? undefined,
      actorId: user.id,
      summary: `取用存酒：${savedBottle?.savedNo}`,
      payload: { savedBottleId: savedBottle?.id, quantity: input.quantity, remainingValue: savedBottle?.remainingValue },
    });
    return reply.code(201).send(savedBottle);
  });

  app.post("/api/admin/bar/saved-bottles/:id/transfer", async (request, reply) => {
    const user = requireCurrentUser(request);
    const params = request.params as { id: string };
    const input = savedBottleTransferSchema.parse(request.body);
    const savedBottle = await prisma.$transaction(async (tx) => {
      const before = await tx.savedBottle.findUnique({ where: { id: params.id } });
      if (!before) throw new Error("SAVED_BOTTLE_NOT_FOUND");
      scopedMerchantId(request, before.merchantId);
      if (!["stored", "partially_used"].includes(before.status)) throw new Error("SAVED_BOTTLE_NOT_AVAILABLE");
      const updated = await tx.savedBottle.update({
        where: { id: params.id },
        data: { customerName: input.customerName, customerPhone: input.customerPhone },
      });
      await tx.savedBottleMovement.create({
        data: {
          movementNo: code("saved_bottle_move"),
          savedBottleId: params.id,
          movementType: "transferred",
          quantity: 0,
          beforeRemainingValue: before.remainingValue,
          afterRemainingValue: before.remainingValue,
          operatorId: user.id,
          reason: input.reason,
        },
      });
      return tx.savedBottle.findUnique({
        where: { id: updated.id },
        include: { product: true, movements: { orderBy: { createdAt: "desc" }, take: 5 } },
      });
    });
    await writeAuditEvent({
      eventType: "bar.saved_bottle.transferred",
      eventCategory: "bar",
      sourceType: "internal",
      merchantId: savedBottle?.merchantId ?? undefined,
      actorId: user.id,
      summary: `存酒转赠：${savedBottle?.savedNo}`,
      payload: { savedBottleId: savedBottle?.id, customerName: input.customerName },
    });
    return reply.code(201).send(savedBottle);
  });

  app.post("/api/admin/bar/saved-bottles/:id/void", async (request, reply) => {
    const user = requireCurrentUser(request);
    const params = request.params as { id: string };
    const input = reasonSchema.parse(request.body);
    const savedBottle = await prisma.$transaction(async (tx) => {
      const before = await tx.savedBottle.findUnique({ where: { id: params.id } });
      if (!before) throw new Error("SAVED_BOTTLE_NOT_FOUND");
      scopedMerchantId(request, before.merchantId);
      if (["finished", "voided"].includes(before.status)) throw new Error("SAVED_BOTTLE_NOT_AVAILABLE");
      const updated = await tx.savedBottle.update({ where: { id: params.id }, data: { status: "voided" } });
      await tx.savedBottleMovement.create({
        data: {
          movementNo: code("saved_bottle_move"),
          savedBottleId: params.id,
          movementType: "voided",
          quantity: before.remainingValue,
          beforeRemainingValue: before.remainingValue,
          afterRemainingValue: 0,
          operatorId: user.id,
          reason: input.reason,
        },
      });
      return tx.savedBottle.findUnique({
        where: { id: updated.id },
        include: { product: true, movements: { orderBy: { createdAt: "desc" }, take: 5 } },
      });
    });
    await writeAuditEvent({
      eventType: "bar.saved_bottle.voided",
      eventCategory: "bar",
      sourceType: "internal",
      merchantId: savedBottle?.merchantId ?? undefined,
      actorId: user.id,
      summary: `存酒作废：${savedBottle?.savedNo}`,
      payload: { savedBottleId: savedBottle?.id, reason: input.reason },
    });
    return reply.code(201).send(savedBottle);
  });

  app.get("/api/admin/bar/orders", async () =>
    prisma.barOrder.findMany({ orderBy: { createdAt: "desc" }, include: { items: true, bills: true } }),
  );

  app.post("/api/admin/bar/orders", async (request, reply) => {
    const input = orderSchema.parse(request.body);
    const order = await prisma.$transaction(async (tx) => {
      const created = await tx.barOrder.create({
        data: {
          orderNo: code("bar_order"),
          tableId: input.tableId,
          merchantId: input.merchantId,
          venueId: input.venueId,
          status: "open",
        },
      });
      await tx.barTable.update({ where: { id: input.tableId }, data: { status: "occupied" } });
      return created;
    });
    await writeAuditEvent({
      eventType: "bar.order.opened",
      eventCategory: "bar",
      sourceType: "internal",
      merchantId: order.merchantId ?? undefined,
      summary: `酒吧开台：${order.orderNo}`,
    });
    return reply.code(201).send(order);
  });

  app.post("/api/admin/bar/orders/:id/items", async (request, reply) => {
    const params = request.params as { id: string };
    const input = orderItemSchema.parse(request.body);
    const product = await prisma.barProduct.findUnique({ where: { id: input.productId } });
    const order = await prisma.barOrder.findUnique({ where: { id: params.id } });

    if (!product || product.status !== "active") {
      return reply.code(400).send({ error: "PRODUCT_NOT_AVAILABLE" });
    }
    if (!order) return reply.code(404).send({ error: "ORDER_NOT_FOUND" });

    const lineAmount = product.priceAmount * input.quantity;
    const item = await prisma.$transaction(async (tx) => {
      const created = await tx.barOrderItem.create({
        data: {
          orderId: params.id,
          productId: input.productId,
          quantity: input.quantity,
          unitPrice: product.priceAmount,
          lineAmount,
        },
      });
      await tx.barOrder.update({
        where: { id: params.id },
        data: { totalAmount: { increment: lineAmount } },
      });
      await tx.barProductionTicket.create({
        data: {
          ticketNo: code("bar_prod"),
          orderId: params.id,
          orderItemId: created.id,
          productId: product.id,
          merchantId: order.merchantId,
          venueId: order.venueId,
          department: productionDepartment(product.category),
          quantity: input.quantity,
          status: "pending",
        },
      });
      return created;
    });

    await writeAuditEvent({
      eventType: "bar.order.item_added",
      eventCategory: "bar",
      sourceType: "internal",
      summary: `酒吧点单：${product.productName} x ${input.quantity}`,
      payload: { orderId: params.id, lineAmount },
    });
    return reply.code(201).send(item);
  });

  app.get("/api/admin/bar/production-tickets", async (request) => {
    const query = request.query as { merchantId?: string; status?: string; department?: string };
    const merchantId = scopedMerchantId(request, query.merchantId);
    return prisma.barProductionTicket.findMany({
      where: {
        ...(merchantId ? { merchantId } : {}),
        ...(query.status ? { status: query.status } : {}),
        ...(query.department ? { department: query.department } : {}),
      },
      orderBy: { createdAt: "desc" },
      take: 200,
    });
  });

  app.post("/api/admin/bar/production-tickets/:id/start", async (request, reply) => {
    const user = requireCurrentUser(request);
    const params = request.params as { id: string };
    const ticket = await prisma.barProductionTicket.findUnique({ where: { id: params.id } });
    if (!ticket) return reply.code(404).send({ error: "PRODUCTION_TICKET_NOT_FOUND" });
    scopedMerchantId(request, ticket.merchantId);
    if (ticket.status !== "pending") return reply.code(400).send({ error: "PRODUCTION_TICKET_NOT_PENDING" });
    const updated = await prisma.barProductionTicket.update({
      where: { id: params.id },
      data: { status: "producing", startedAt: new Date(), operatorId: user.id },
    });
    await writeAuditEvent({
      eventType: "bar.production.started",
      eventCategory: "bar",
      sourceType: "internal",
      merchantId: updated.merchantId ?? undefined,
      actorId: user.id,
      summary: `开始出品：${updated.ticketNo}`,
      payload: { productionTicketId: updated.id, department: updated.department },
    });
    return reply.code(201).send(updated);
  });

  app.post("/api/admin/bar/production-tickets/:id/serve", async (request, reply) => {
    const user = requireCurrentUser(request);
    const params = request.params as { id: string };
    const ticket = await prisma.barProductionTicket.findUnique({ where: { id: params.id } });
    if (!ticket) return reply.code(404).send({ error: "PRODUCTION_TICKET_NOT_FOUND" });
    scopedMerchantId(request, ticket.merchantId);
    if (!["pending", "producing"].includes(ticket.status)) return reply.code(400).send({ error: "PRODUCTION_TICKET_NOT_SERVABLE" });
    const updated = await prisma.$transaction(async (tx) => {
      const served = await tx.barProductionTicket.update({
        where: { id: params.id },
        data: { status: "served", servedAt: new Date(), operatorId: user.id },
      });
      await tx.barOrderItem.update({ where: { id: ticket.orderItemId }, data: { status: "served" } });
      const remaining = await tx.barProductionTicket.count({ where: { orderId: ticket.orderId, status: { not: "served" } } });
      if (remaining === 0) await tx.barOrder.update({ where: { id: ticket.orderId }, data: { status: "served" } });
      return served;
    });
    await writeAuditEvent({
      eventType: "bar.production.served",
      eventCategory: "bar",
      sourceType: "internal",
      merchantId: updated.merchantId ?? undefined,
      actorId: user.id,
      summary: `确认出品：${updated.ticketNo}`,
      payload: { productionTicketId: updated.id, orderId: updated.orderId },
    });
    return reply.code(201).send(updated);
  });

  app.post("/api/admin/bar/production-tickets/:id/abnormal", async (request, reply) => {
    const user = requireCurrentUser(request);
    const params = request.params as { id: string };
    const input = productionActionSchema.parse(request.body ?? {});
    const ticket = await prisma.barProductionTicket.findUnique({ where: { id: params.id } });
    if (!ticket) return reply.code(404).send({ error: "PRODUCTION_TICKET_NOT_FOUND" });
    scopedMerchantId(request, ticket.merchantId);
    const updated = await prisma.barProductionTicket.update({
      where: { id: params.id },
      data: { status: "abnormal", abnormalReason: input.reason, operatorId: user.id },
    });
    await writeAuditEvent({
      eventType: "bar.production.abnormal",
      eventCategory: "bar",
      sourceType: "internal",
      result: "failed",
      riskLevel: "suspicious",
      merchantId: updated.merchantId ?? undefined,
      actorId: user.id,
      summary: `出品异常：${updated.ticketNo}`,
      payload: { productionTicketId: updated.id, reason: input.reason },
    });
    return reply.code(201).send(updated);
  });

  app.patch("/api/admin/bar/order-items/:id", async (request, reply) => {
    const params = request.params as { id: string };
    const input = updateOrderItemSchema.parse(request.body);
    const existing = await prisma.barOrderItem.findUnique({ where: { id: params.id }, include: { product: true } });

    if (!existing) return reply.code(404).send({ error: "ORDER_ITEM_NOT_FOUND" });

    const nextLineAmount = existing.unitPrice * input.quantity;
    const amountDelta = nextLineAmount - existing.lineAmount;
    const item = await prisma.$transaction(async (tx) => {
      const updated = await tx.barOrderItem.update({
        where: { id: params.id },
        data: {
          quantity: input.quantity,
          lineAmount: nextLineAmount,
          status: input.quantity === 0 ? "cancelled" : "ordered",
        },
      });
      await tx.barOrder.update({
        where: { id: existing.orderId },
        data: { totalAmount: { increment: amountDelta } },
      });
      await tx.barProductionTicket.updateMany({
        where: { orderItemId: params.id, status: { in: ["pending", "producing"] } },
        data: {
          quantity: input.quantity,
          status: input.quantity === 0 ? "cancelled" : "pending",
          cancelledAt: input.quantity === 0 ? new Date() : null,
        },
      });
      return updated;
    });

    await writeAuditEvent({
      eventType: input.quantity === 0 ? "bar.order.item_cancelled" : "bar.order.item_quantity_changed",
      eventCategory: "bar",
      sourceType: "internal",
      summary: `酒吧改数量：${existing.product.productName} -> ${input.quantity}`,
      payload: { orderId: existing.orderId, itemId: item.id, reason: input.reason, amountDelta },
    });

    return item;
  });

  app.post("/api/admin/bar/orders/:id/bill", async (request, reply) => {
    const params = request.params as { id: string };
    const order = await prisma.barOrder.findUnique({ where: { id: params.id } });
    if (!order) return reply.code(404).send({ error: "ORDER_NOT_FOUND" });

    const bill = await prisma.barBill.create({
      data: {
        billNo: code("bar_bill"),
        orderId: order.id,
        tableId: order.tableId,
        totalAmount: order.totalAmount,
        status: "unpaid",
      },
    });
    await prisma.barOrder.update({ where: { id: order.id }, data: { status: "billing" } });
    return reply.code(201).send(bill);
  });

  app.post("/api/admin/bar/bills/:id/pay", async (request) => {
    const params = request.params as { id: string };
    const input = paySchema.parse(request.body);
    const bill = await prisma.barBill.update({
      where: { id: params.id },
      data: {
        paidAmount: input.paidAmount,
        paymentMethod: input.paymentMethod,
        status: "paid",
      },
    });
    await prisma.barOrder.update({ where: { id: bill.orderId }, data: { status: "paid" } });
    await prisma.barTable.update({ where: { id: bill.tableId }, data: { status: "cleaning" } });
    await writeAuditEvent({
      eventType: "bar.bill.paid",
      eventCategory: "bar",
      sourceType: "internal",
      summary: `酒吧账单已支付：${bill.billNo}`,
      payload: { billId: bill.id, paidAmount: input.paidAmount, paymentMethod: input.paymentMethod },
    });
    return bill;
  });
}
