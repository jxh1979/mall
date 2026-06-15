import cors from "@fastify/cors";
import swagger from "@fastify/swagger";
import swaggerUi from "@fastify/swagger-ui";
import Fastify from "fastify";
import { registerSecurity } from "./lib/security.js";
import { registerAmusementRoutes } from "./modules/amusement/routes.js";
import { registerAnalyticsRoutes } from "./modules/analytics/routes.js";
import { registerAuthRoutes } from "./modules/auth/routes.js";
import { registerBarRoutes } from "./modules/bar/routes.js";
import { registerEventRoutes } from "./modules/events/routes.js";
import { registerIntegrationReservedRoutes } from "./modules/integration-reserved/routes.js";
import { registerOpenAuthRoutes } from "./modules/open-auth/routes.js";
import { registerOpenAppRoutes } from "./modules/open-apps/routes.js";
import { registerPlatformRoutes } from "./modules/platform/routes.js";
import { registerSystemRoutes } from "./modules/system/routes.js";
import { registerTerminalRoutes } from "./modules/terminal/routes.js";
import { registerTicketingRoutes } from "./modules/ticketing/routes.js";

const app = Fastify({
  logger: true,
});

app.setErrorHandler((error, _request, reply) => {
  const message = error instanceof Error ? error.message : "INTERNAL_ERROR";
  if (message === "AUTH_REQUIRED" || message === "AUTH_INVALID") {
    return reply.code(401).send({ error: message });
  }
  if (message === "FORBIDDEN" || message === "FORBIDDEN_MERCHANT_SCOPE") {
    return reply.code(403).send({ error: message });
  }
  if (message === "MERCHANT_SCOPE_REQUIRED") {
    return reply.code(400).send({ error: message });
  }
  if (
    message === "ENTITLEMENT_ALREADY_USED" ||
    message === "SESSION_CAPACITY_CHANGED" ||
    message === "STOCK_NOT_ENOUGH" ||
    message === "SAVED_BOTTLE_NOT_ENOUGH" ||
    message === "RESERVATION_CAPACITY_FULL"
  ) {
    return reply.code(409).send({ error: message });
  }
  if (
    message === "SAVED_BOTTLE_NOT_FOUND" ||
    message === "SAVED_BOTTLE_NOT_AVAILABLE" ||
    message === "INVALID_DATE" ||
    message === "UNSUPPORTED_BUSINESS_TYPE"
  ) {
    return reply.code(400).send({ error: message });
  }
  return reply.send(error);
});

await app.register(cors, {
  origin: (origin, callback) => {
    const configured = (process.env.CORS_ALLOWED_ORIGINS ?? "http://192.168.88.160,http://localhost:3000,http://127.0.0.1:3000,http://localhost:5173,http://127.0.0.1:5173")
      .split(",")
      .map((item) => item.trim())
      .filter(Boolean);
    if (!origin || configured.includes(origin)) return callback(null, true);
    return callback(new Error("CORS origin is not allowed"), false);
  },
});

await app.register(swagger, {
  openapi: {
    info: {
      title: "娱乐综合体管理系统开放接口",
      version: "0.1.0",
    },
  },
});

await app.register(swaggerUi, {
  routePrefix: "/docs",
});

await registerSecurity(app);

await registerSystemRoutes(app);
await registerAuthRoutes(app);
await registerPlatformRoutes(app);
await registerEventRoutes(app);
await registerOpenAppRoutes(app);
await registerOpenAuthRoutes(app);
await registerTicketingRoutes(app);
await registerTerminalRoutes(app);
await registerBarRoutes(app);
await registerAmusementRoutes(app);
await registerAnalyticsRoutes(app);
await registerIntegrationReservedRoutes(app);

const port = Number(process.env.API_PORT ?? 4000);
const host = process.env.API_HOST ?? "127.0.0.1";

try {
  await app.listen({ port, host });
} catch (error) {
  app.log.error(error);
  process.exit(1);
}
