import argon2 from "argon2";
import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { prisma } from "../../lib/prisma.js";
import { createUserAccessToken, requireCurrentUser } from "../../lib/security.js";
import { writeAuditEvent } from "../events/service.js";

const loginSchema = z.object({
  username: z.string().min(1),
  password: z.string().min(1),
});

export async function registerAuthRoutes(app: FastifyInstance) {
  app.post("/api/auth/login", async (request, reply) => {
    const input = loginSchema.parse(request.body);
    const user = await prisma.userAccount.findUnique({ where: { username: input.username } });

    if (!user || user.status !== "active") {
      return reply.code(401).send({ error: "AUTH_FAILED" });
    }

    const passwordOk = await argon2.verify(user.passwordHash, input.password);

    if (!passwordOk) {
      await prisma.userAccount.update({
        where: { id: user.id },
        data: { failedLoginCount: user.failedLoginCount + 1 },
      });
      return reply.code(401).send({ error: "AUTH_FAILED" });
    }

    await prisma.userAccount.update({
      where: { id: user.id },
      data: { failedLoginCount: 0, lastLoginAt: new Date() },
    });

    await writeAuditEvent({
      eventType: "user.login",
      eventCategory: "identity",
      sourceType: "internal",
      actorType: "staff",
      actorId: user.id,
      merchantId: user.merchantId ?? undefined,
      summary: `账号登录：${user.username}`,
    });

    const token = createUserAccessToken({
      id: user.id,
      username: user.username,
      accountType: user.accountType,
      organizationId: user.organizationId,
      merchantId: user.merchantId,
    });

    return {
      accessToken: token.accessToken,
      tokenType: "Bearer",
      expiresIn: token.expiresIn,
      user: {
        id: user.id,
        username: user.username,
        accountType: user.accountType,
        organizationId: user.organizationId,
        merchantId: user.merchantId,
      },
    };
  });

  app.post("/api/auth/logout", async () => ({ ok: true }));

  app.post("/api/auth/refresh", async (request) => {
    const user = requireCurrentUser(request);
    const token = createUserAccessToken(user);
    return {
      accessToken: token.accessToken,
      tokenType: "Bearer",
      expiresIn: token.expiresIn,
    };
  });

  app.get("/api/me", async (request) => {
    const user = requireCurrentUser(request);
    return {
      user,
      workspaces: [
        { key: "platform", name: "平台工作台" },
        { key: "merchant", name: "商户工作台" },
      ],
    };
  });

  app.get("/api/me/menus", async () => ({
    menus: [
      "platform.dashboard",
      "platform.merchants",
      "platform.ticketing",
      "platform.bar",
      "platform.amusement",
      "platform.analytics",
      "platform.events",
    ],
  }));
}
