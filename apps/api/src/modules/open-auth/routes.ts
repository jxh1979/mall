import { createHash, randomBytes } from "node:crypto";
import argon2 from "argon2";
import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { prisma } from "../../lib/prisma.js";
import { requireCurrentUser } from "../../lib/security.js";
import { writeAuditEvent } from "../events/service.js";

const authSchema = z.object({
  loginAccount: z.string().min(1),
  password: z.string().min(1),
});

function hashToken(token: string) {
  return createHash("sha256").update(token).digest("hex");
}

function secondsFromNow(seconds: number) {
  return new Date(Date.now() + seconds * 1000);
}

function bearerToken(header: string | undefined) {
  if (!header?.startsWith("Bearer ")) return null;
  return header.slice("Bearer ".length).trim();
}

function ipAllowed(ip: string, whitelist?: string | null) {
  if (!whitelist?.trim()) return true;
  return whitelist
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean)
    .includes(ip);
}

export async function registerOpenAuthRoutes(app: FastifyInstance) {
  app.post("/api/open-auth/token", async (request, reply) => {
    const input = authSchema.parse(request.body);
    const credential = await prisma.openAppCredential.findUnique({
      where: { loginAccount: input.loginAccount },
      include: { openApp: true },
    });

    if (!credential) {
      await prisma.authFailureRecord.create({
        data: {
          loginAccount: input.loginAccount,
          failureType: "password_error",
          ip: request.ip,
          occurredAt: new Date(),
          failureReason: "开放应用账号不存在",
        },
      });
      return reply.code(401).send({ error: "AUTH_FAILED" });
    }

    if (credential.status === "locked" || credential.openApp.status === "locked") {
      return reply.code(423).send({ error: "APP_LOCKED" });
    }

    const passwordOk = await argon2.verify(credential.passwordHash, input.password);

    if (!passwordOk) {
      const failedCount = credential.failedAuthCount + 1;

      await prisma.authFailureRecord.create({
        data: {
          openAppId: credential.openAppId,
          loginAccount: input.loginAccount,
          failureType: "password_error",
          ip: request.ip,
          userAgent: request.headers["user-agent"],
          occurredAt: new Date(),
          failureReason: "密码错误",
        },
      });

      const threshold = Number(process.env.AUTH_FAILURE_LOCK_THRESHOLD ?? 3);

      if (failedCount >= threshold) {
        await prisma.$transaction([
          prisma.openAppCredential.update({
            where: { id: credential.id },
            data: {
              failedAuthCount: failedCount,
              lastFailedAt: new Date(),
              lockedAt: new Date(),
              status: "locked",
            },
          }),
          prisma.openApp.update({
            where: { id: credential.openAppId },
            data: { status: "locked" },
          }),
          prisma.appLockRecord.create({
            data: {
              openAppId: credential.openAppId,
              credentialId: credential.id,
              lockedReason: "连续鉴权失败达到阈值",
              failedCount,
              lockedAt: new Date(),
              lockedBy: "system",
              status: "locked",
            },
          }),
        ]);

        await writeAuditEvent({
          eventType: "open_app.locked",
          eventCategory: "third_party",
          sourceType: "internal",
          actorType: "system",
          openAppId: credential.openAppId,
          merchantId: credential.openApp.merchantId ?? undefined,
          result: "abnormal",
          riskLevel: "high",
          summary: `开放应用连续${failedCount}次鉴权失败，已锁定`,
        });

        return reply.code(423).send({ error: "APP_LOCKED" });
      }

      await prisma.openAppCredential.update({
        where: { id: credential.id },
        data: {
          failedAuthCount: failedCount,
          lastFailedAt: new Date(),
        },
      });

      return reply.code(401).send({ error: "AUTH_FAILED", failedCount });
    }

    const accessTokenTtl = Number(process.env.ACCESS_TOKEN_TTL_SECONDS ?? 900);
    const endpointTtl = Number(process.env.DYNAMIC_ENDPOINT_TTL_SECONDS ?? 600);
    const rawToken = randomBytes(32).toString("hex");
    const endpointKey = randomBytes(16).toString("hex");

    const authSession = await prisma.authSession.create({
      data: {
        openAppId: credential.openAppId,
        credentialId: credential.id,
        authIp: request.ip,
        userAgent: request.headers["user-agent"],
        authenticatedAt: new Date(),
        expiresAt: secondsFromNow(accessTokenTtl),
        status: "active",
      },
    });

    await prisma.$transaction([
      prisma.openAppCredential.update({
        where: { id: credential.id },
        data: {
          failedAuthCount: 0,
          lastFailedAt: null,
        },
      }),
      prisma.accessToken.create({
        data: {
          openAppId: credential.openAppId,
          authSessionId: authSession.id,
          tokenHash: hashToken(rawToken),
          issuedAt: new Date(),
          expiresAt: secondsFromNow(accessTokenTtl),
          status: "active",
        },
      }),
      prisma.dynamicEndpoint.create({
        data: {
          openAppId: credential.openAppId,
          authSessionId: authSession.id,
          endpointKey,
          endpointPath: `/gateway/${endpointKey}`,
          issuedAt: new Date(),
          expiresAt: secondsFromNow(endpointTtl),
          status: "active",
        },
      }),
    ]);

    await writeAuditEvent({
      eventType: "open_app.authenticated",
      eventCategory: "third_party",
      sourceType: "third_party",
      sourceSystem: "open-auth",
      actorType: "open_app",
      actorId: credential.openAppId,
      openAppId: credential.openAppId,
      merchantId: credential.openApp.merchantId ?? undefined,
      summary: `开放应用鉴权成功：${credential.openApp.appName}`,
    });

    return {
      accessToken: rawToken,
      tokenType: "Bearer",
      expiresIn: accessTokenTtl,
      dynamicEndpoint: `/gateway/${endpointKey}`,
      dynamicEndpointExpiresIn: endpointTtl,
    };
  });

  app.post("/api/open-auth/open-apps/:openAppId/unlock", async (request, reply) => {
    const actor = requireCurrentUser(request);
    const params = request.params as { openAppId: string };
    const body = z.object({ reason: z.string().min(1) }).parse(request.body);

    const lock = await prisma.appLockRecord.findFirst({
      where: { openAppId: params.openAppId, status: "locked" },
      orderBy: { lockedAt: "desc" },
    });

    if (!lock) {
      return reply.code(404).send({ error: "LOCK_RECORD_NOT_FOUND" });
    }

    await prisma.$transaction([
      prisma.openApp.update({
        where: { id: params.openAppId },
        data: { status: "active" },
      }),
      prisma.openAppCredential.updateMany({
        where: { openAppId: params.openAppId },
        data: {
          failedAuthCount: 0,
          lockedAt: null,
          status: "active",
        },
      }),
      prisma.appLockRecord.update({
        where: { id: lock.id },
        data: { status: "unlocked" },
      }),
      prisma.manualUnlockRecord.create({
        data: {
          lockRecordId: lock.id,
          openAppId: params.openAppId,
          unlockedBy: actor.id,
          unlockedAt: new Date(),
          reason: body.reason,
          reviewResult: "approved",
        },
      }),
    ]);

    await writeAuditEvent({
      eventType: "open_app.unlocked",
      eventCategory: "third_party",
      sourceType: "internal",
      actorType: "staff",
      actorId: actor.id,
      openAppId: params.openAppId,
      summary: `人工解锁开放应用：${body.reason}`,
    });

    return { ok: true };
  });

  app.all("/gateway/:endpointKey", async (request, reply) => {
    const params = request.params as { endpointKey: string };
    const rawToken = bearerToken(request.headers.authorization);
    if (!rawToken) return reply.code(401).send({ error: "OPEN_TOKEN_REQUIRED" });

    const [accessToken, endpoint] = await Promise.all([
      prisma.accessToken.findUnique({
        where: { tokenHash: hashToken(rawToken) },
        include: { openApp: true },
      }),
      prisma.dynamicEndpoint.findUnique({ where: { endpointKey: params.endpointKey } }),
    ]);

    const now = new Date();
    if (
      !accessToken ||
      !endpoint ||
      accessToken.status !== "active" ||
      endpoint.status !== "active" ||
      accessToken.expiresAt <= now ||
      endpoint.expiresAt <= now ||
      accessToken.openAppId !== endpoint.openAppId ||
      accessToken.authSessionId !== endpoint.authSessionId ||
      accessToken.openApp.status !== "active"
    ) {
      return reply.code(401).send({ error: "OPEN_AUTH_INVALID" });
    }

    if (!ipAllowed(request.ip, accessToken.openApp.ipWhitelist)) {
      return reply.code(403).send({ error: "OPEN_IP_NOT_ALLOWED" });
    }

    await writeAuditEvent({
      eventType: "open_app.gateway.accepted",
      eventCategory: "third_party",
      sourceType: "third_party",
      sourceSystem: "dynamic-gateway",
      actorType: "open_app",
      actorId: accessToken.openAppId,
      openAppId: accessToken.openAppId,
      merchantId: accessToken.openApp.merchantId ?? undefined,
      summary: `Dynamic gateway accepted: ${endpoint.endpointPath}`,
      payload: { method: request.method, path: request.url.split("?")[0] },
    });

    return reply.code(202).send({
      accepted: true,
      gateway: true,
      openAppId: accessToken.openAppId,
      businessProcessed: false,
      message: "Dynamic gateway authentication accepted; concrete third-party handlers are reserved.",
    });
  });
}
