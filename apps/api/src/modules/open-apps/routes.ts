import { randomBytes } from "node:crypto";
import argon2 from "argon2";
import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { prisma } from "../../lib/prisma.js";
import { hashSecret, randomSecret, requireCurrentUser } from "../../lib/security.js";
import { writeAuditEvent } from "../events/service.js";

const createOpenAppSchema = z.object({
  appName: z.string().min(1),
  appType: z.string().min(1),
  merchantId: z.string().optional(),
  serviceProviderId: z.string().optional(),
  venueId: z.string().optional(),
  contactName: z.string().optional(),
  contactPhone: z.string().optional(),
  callbackUrl: z.string().url().optional(),
  loginAccount: z.string().min(3),
  password: z.string().min(8),
});

export async function registerOpenAppRoutes(app: FastifyInstance) {
  app.get("/api/open-apps", async () => {
    return prisma.openApp.findMany({
      orderBy: { createdAt: "desc" },
      include: {
        credentials: {
          select: {
            id: true,
            loginAccount: true,
            secretMasked: true,
            failedAuthCount: true,
            lockedAt: true,
            status: true,
            createdAt: true,
            updatedAt: true,
          },
        },
      },
    });
  });

  app.post("/api/open-apps", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const input = createOpenAppSchema.parse(request.body);
    const secret = randomSecret(32);
    const passwordHash = await argon2.hash(input.password);

    const openApp = await prisma.openApp.create({
      data: {
        appCode: `app_${randomBytes(6).toString("hex")}`,
        appName: input.appName,
        appType: input.appType,
        merchantId: input.merchantId,
        serviceProviderId: input.serviceProviderId,
        venueId: input.venueId,
        contactName: input.contactName,
        contactPhone: input.contactPhone,
        callbackUrl: input.callbackUrl,
        status: "active",
        credentials: {
          create: {
            loginAccount: input.loginAccount,
            passwordHash,
            secretEncrypted: hashSecret(secret),
            secretMasked: `${secret.slice(0, 6)}****${secret.slice(-4)}`,
            status: "active",
          },
        },
      },
      include: {
        credentials: {
          select: {
            id: true,
            loginAccount: true,
            secretMasked: true,
            failedAuthCount: true,
            lockedAt: true,
            status: true,
            createdAt: true,
            updatedAt: true,
          },
        },
      },
    });

    await writeAuditEvent({
      eventType: "open_app.created",
      eventCategory: "third_party",
      sourceType: "internal",
      actorType: "staff",
      actorId: actor.id,
      openAppId: openApp.id,
      merchantId: openApp.merchantId ?? undefined,
      summary: `创建开放应用：${openApp.appName}`,
      payload: { appCode: openApp.appCode, appType: openApp.appType },
    });

    return reply.code(201).send({
      ...openApp,
      initialSecret: secret,
    });
  });
}
