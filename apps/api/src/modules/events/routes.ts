import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { prisma } from "../../lib/prisma.js";
import { writeAuditEvent } from "./service.js";

const createEventSchema = z.object({
  eventType: z.string().min(1),
  eventCategory: z.string().min(1),
  sourceType: z.string().default("internal"),
  sourceSystem: z.string().optional(),
  actorType: z.string().optional(),
  actorId: z.string().optional(),
  merchantId: z.string().optional(),
  openAppId: z.string().optional(),
  result: z.string().optional(),
  riskLevel: z.string().optional(),
  summary: z.string().min(1),
  payload: z.unknown().optional(),
  traceId: z.string().optional(),
});

export async function registerEventRoutes(app: FastifyInstance) {
  app.get("/api/events", async (request) => {
    const query = request.query as {
      merchantId?: string;
      openAppId?: string;
      eventCategory?: string;
      limit?: string;
    };

    return prisma.auditEvent.findMany({
      where: {
        merchantId: query.merchantId,
        openAppId: query.openAppId,
        eventCategory: query.eventCategory,
      },
      orderBy: { occurredAt: "desc" },
      take: Math.min(Number(query.limit ?? 50), 200),
    });
  });

  app.post("/api/events", async (request, reply) => {
    const input = createEventSchema.parse(request.body);
    const event = await writeAuditEvent(input);
    return reply.code(201).send(event);
  });
}
