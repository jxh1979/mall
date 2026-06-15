import { createHmac, createHash, randomBytes, timingSafeEqual } from "node:crypto";
import type { FastifyInstance, FastifyReply, FastifyRequest } from "fastify";
import { prisma } from "./prisma.js";

export type AuthenticatedUser = {
  id: string;
  username: string;
  accountType: string;
  organizationId?: string | null;
  merchantId?: string | null;
};

type TokenPayload = AuthenticatedUser & {
  sub: string;
  typ: "user";
  iat: number;
  exp: number;
};

declare module "fastify" {
  interface FastifyRequest {
    currentUser?: AuthenticatedUser;
  }
}

const publicRoutes = [
  { method: "GET", path: "/api/health" },
  { method: "POST", path: "/api/auth/login" },
  { method: "POST", path: "/api/open-auth/token" },
];

function base64Url(input: Buffer | string) {
  return Buffer.from(input).toString("base64url");
}

function parseBase64UrlJson<T>(input: string): T {
  return JSON.parse(Buffer.from(input, "base64url").toString("utf8")) as T;
}

function getJwtSecret() {
  const secret = process.env.JWT_SECRET ?? process.env.COMPLEX_JWT_SECRET;
  const allowDevSecret = process.env.ALLOW_INSECURE_DEV_SECRETS === "true";

  if (secret && !secret.startsWith("replace-with-") && secret.length >= 32) return secret;
  if (allowDevSecret) return "complex-dev-only-jwt-secret-change-before-production";

  throw new Error("JWT_SECRET must be configured with at least 32 characters");
}

function signRaw(header: string, payload: string) {
  return createHmac("sha256", getJwtSecret()).update(`${header}.${payload}`).digest("base64url");
}

export function hashSecret(value: string) {
  return createHash("sha256").update(value).digest("hex");
}

export function createUserAccessToken(user: AuthenticatedUser) {
  const now = Math.floor(Date.now() / 1000);
  const ttl = Number(process.env.USER_ACCESS_TOKEN_TTL_SECONDS ?? 3600);
  const header = base64Url(JSON.stringify({ alg: "HS256", typ: "JWT" }));
  const payload = base64Url(
    JSON.stringify({
      typ: "user",
      sub: user.id,
      id: user.id,
      username: user.username,
      accountType: user.accountType,
      organizationId: user.organizationId ?? null,
      merchantId: user.merchantId ?? null,
      iat: now,
      exp: now + ttl,
    } satisfies TokenPayload),
  );

  return {
    accessToken: `${header}.${payload}.${signRaw(header, payload)}`,
    expiresIn: ttl,
  };
}

async function verifyUserAccessToken(token: string): Promise<AuthenticatedUser> {
  const [header, payload, signature] = token.split(".");
  if (!header || !payload || !signature) throw new Error("Malformed token");

  const expected = Buffer.from(signRaw(header, payload));
  const actual = Buffer.from(signature);
  if (actual.length !== expected.length || !timingSafeEqual(actual, expected)) {
    throw new Error("Invalid token signature");
  }

  const parsed = parseBase64UrlJson<TokenPayload>(payload);
  if (parsed.typ !== "user" || !parsed.sub || parsed.exp <= Math.floor(Date.now() / 1000)) {
    throw new Error("Expired token");
  }

  const user = await prisma.userAccount.findUnique({ where: { id: parsed.sub } });
  if (!user || user.status !== "active" || (user.lockedUntil && user.lockedUntil > new Date())) {
    throw new Error("Inactive user");
  }

  return {
    id: user.id,
    username: user.username,
    accountType: user.accountType,
    organizationId: user.organizationId,
    merchantId: user.merchantId,
  };
}

function getBearerToken(request: FastifyRequest) {
  const header = request.headers.authorization;
  if (!header?.startsWith("Bearer ")) return null;
  return header.slice("Bearer ".length).trim();
}

function isPublicRoute(method: string, path: string) {
  if (method === "OPTIONS") return true;
  if (path.startsWith("/gateway/")) return true;
  if (process.env.ENABLE_PUBLIC_DOCS === "true" && path.startsWith("/docs")) return true;
  return publicRoutes.some((route) => route.method === method && route.path === path);
}

function isPlatformAdmin(user: AuthenticatedUser) {
  return user.accountType === "platform_admin";
}

function deny(reply: FastifyReply, status: number, error: string) {
  return reply.code(status).send({ error });
}

function requireRoleForPath(request: FastifyRequest, reply: FastifyReply) {
  const user = request.currentUser;
  if (!user) return deny(reply, 401, "AUTH_REQUIRED");

  const path = request.url.split("?")[0] ?? request.url;
  const platformOnly =
    path.startsWith("/api/admin/") ||
    path.startsWith("/api/risk/") ||
    path.startsWith("/api/analytics/") ||
    path.startsWith("/api/events") ||
    path.startsWith("/api/open-apps") ||
    path.startsWith("/api/integration/") ||
    path.startsWith("/docs") ||
    path.startsWith("/api/open-auth/open-apps/");

  if (platformOnly && !isPlatformAdmin(user)) return deny(reply, 403, "FORBIDDEN");
  return undefined;
}

export async function registerSecurity(app: FastifyInstance) {
  app.addHook("preHandler", async (request, reply) => {
    const path = request.url.split("?")[0] ?? request.url;
    if (isPublicRoute(request.method, path)) return;

    const token = getBearerToken(request);
    if (!token) return deny(reply, 401, "AUTH_REQUIRED");

    try {
      request.currentUser = await verifyUserAccessToken(token);
    } catch {
      return deny(reply, 401, "AUTH_INVALID");
    }

    return requireRoleForPath(request, reply);
  });
}

export function requireCurrentUser(request: FastifyRequest) {
  if (!request.currentUser) throw new Error("AUTH_REQUIRED");
  return request.currentUser;
}

export function requirePlatformAdminUser(request: FastifyRequest) {
  const user = requireCurrentUser(request);
  if (!isPlatformAdmin(user)) throw new Error("FORBIDDEN");
  return user;
}

export function scopedMerchantId(request: FastifyRequest, requestedMerchantId?: string | null) {
  const user = requireCurrentUser(request);
  if (isPlatformAdmin(user)) return requestedMerchantId ?? undefined;
  if (!user.merchantId) throw new Error("MERCHANT_SCOPE_REQUIRED");
  if (requestedMerchantId && requestedMerchantId !== user.merchantId) throw new Error("FORBIDDEN_MERCHANT_SCOPE");
  return user.merchantId;
}

export function assertMerchantAccess(request: FastifyRequest, merchantId?: string | null) {
  scopedMerchantId(request, merchantId);
}

export function randomSecret(bytes = 32) {
  return randomBytes(bytes).toString("hex");
}
