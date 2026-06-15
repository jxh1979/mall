import type { FastifyInstance } from "fastify";

export async function registerSystemRoutes(app: FastifyInstance) {
  app.get("/api/health", async () => ({
    ok: true,
    service: "entertainment-complex-api",
    time: new Date().toISOString(),
  }));
}
