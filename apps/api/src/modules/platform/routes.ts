import argon2 from "argon2";
import type { FastifyInstance } from "fastify";
import { z } from "zod";
import { prisma } from "../../lib/prisma.js";
import { writeAuditEvent } from "../events/service.js";

const organizationSchema = z.object({
  orgType: z.string().min(1),
  name: z.string().min(1),
  shortName: z.string().optional(),
  code: z.string().optional(),
  parentId: z.string().optional(),
});

const merchantSchema = z.object({
  organizationId: z.string().min(1),
  merchantType: z.string().min(1),
  companyName: z.string().min(1),
  displayName: z.string().min(1),
  contactName: z.string().optional(),
  contactPhone: z.string().optional(),
  status: z.string().default("active"),
});

const venueSchema = z.object({
  merchantId: z.string().min(1),
  venueType: z.string().min(1),
  name: z.string().min(1),
  code: z.string().optional(),
  floor: z.string().optional(),
  capacity: z.number().int().optional(),
  status: z.string().default("active"),
});

const areaSchema = z.object({
  venueId: z.string().optional(),
  parentId: z.string().optional(),
  name: z.string().min(1),
  areaType: z.string().min(1),
  floor: z.string().optional(),
  capacity: z.number().int().optional(),
});

const pointSchema = z.object({
  areaId: z.string().min(1),
  venueId: z.string().optional(),
  merchantId: z.string().optional(),
  pointType: z.string().min(1),
  name: z.string().min(1),
  code: z.string().optional(),
  capacity: z.number().int().optional(),
});

const userSchema = z.object({
  username: z.string().min(3),
  password: z.string().min(6),
  phone: z.string().optional(),
  accountType: z.string().default("staff"),
  organizationId: z.string().optional(),
  merchantId: z.string().optional(),
  status: z.string().default("active"),
});

const roleSchema = z.object({
  roleCode: z.string().min(1),
  roleName: z.string().min(1),
  roleType: z.string().default("platform"),
  organizationId: z.string().optional(),
});

const permissionSchema = z.object({
  permissionCode: z.string().min(1),
  permissionName: z.string().min(1),
  permissionType: z.string().default("api"),
  resource: z.string().min(1),
  action: z.string().min(1),
});

export async function registerPlatformRoutes(app: FastifyInstance) {
  app.get("/api/admin/organizations", async () => prisma.organization.findMany({ orderBy: { createdAt: "desc" } }));

  app.post("/api/admin/organizations", async (request, reply) => {
    const input = organizationSchema.parse(request.body);
    const organization = await prisma.organization.create({ data: input });
    await writeAuditEvent({
      eventType: "organization.created",
      eventCategory: "platform",
      sourceType: "internal",
      summary: `创建组织：${organization.name}`,
      payload: { organizationId: organization.id },
    });
    return reply.code(201).send(organization);
  });

  app.get("/api/admin/merchants", async () =>
    prisma.merchant.findMany({ orderBy: { createdAt: "desc" }, include: { organization: true } }),
  );

  app.post("/api/admin/merchants", async (request, reply) => {
    const input = merchantSchema.parse(request.body);
    const merchant = await prisma.merchant.create({ data: input });
    await writeAuditEvent({
      eventType: "merchant.created",
      eventCategory: "platform",
      sourceType: "internal",
      merchantId: merchant.id,
      summary: `创建商户：${merchant.displayName}`,
    });
    return reply.code(201).send(merchant);
  });

  app.get("/api/admin/venues", async () => prisma.venue.findMany({ orderBy: { createdAt: "desc" } }));

  app.post("/api/admin/venues", async (request, reply) => {
    const input = venueSchema.parse(request.body);
    const venue = await prisma.venue.create({ data: input });
    await writeAuditEvent({
      eventType: "venue.created",
      eventCategory: "platform",
      sourceType: "internal",
      merchantId: venue.merchantId,
      summary: `创建门店/场地：${venue.name}`,
    });
    return reply.code(201).send(venue);
  });

  app.get("/api/admin/areas", async () => prisma.area.findMany({ orderBy: { createdAt: "desc" } }));

  app.post("/api/admin/areas", async (request, reply) => {
    const input = areaSchema.parse(request.body);
    const area = await prisma.area.create({ data: input });
    await writeAuditEvent({
      eventType: "area.created",
      eventCategory: "platform",
      sourceType: "internal",
      summary: `创建区域：${area.name}`,
      payload: { areaId: area.id, venueId: area.venueId },
    });
    return reply.code(201).send(area);
  });

  app.get("/api/admin/location-points", async () => prisma.locationPoint.findMany({ orderBy: { createdAt: "desc" } }));

  app.post("/api/admin/location-points", async (request, reply) => {
    const input = pointSchema.parse(request.body);
    const point = await prisma.locationPoint.create({ data: input });
    await writeAuditEvent({
      eventType: "location_point.created",
      eventCategory: "platform",
      sourceType: "internal",
      merchantId: point.merchantId ?? undefined,
      summary: `创建点位：${point.name}`,
      payload: { pointId: point.id, pointType: point.pointType },
    });
    return reply.code(201).send(point);
  });

  app.get("/api/admin/users", async () =>
    prisma.userAccount.findMany({
      orderBy: { createdAt: "desc" },
      select: {
        id: true,
        username: true,
        phone: true,
        accountType: true,
        organizationId: true,
        merchantId: true,
        status: true,
        createdAt: true,
      },
    }),
  );

  app.post("/api/admin/users", async (request, reply) => {
    const input = userSchema.parse(request.body);
    const { password, ...userData } = input;
    const user = await prisma.userAccount.create({
      data: {
        ...userData,
        passwordHash: await argon2.hash(password),
      },
      select: {
        id: true,
        username: true,
        accountType: true,
        organizationId: true,
        merchantId: true,
        status: true,
        createdAt: true,
      },
    });
    await writeAuditEvent({
      eventType: "user.created",
      eventCategory: "identity",
      sourceType: "internal",
      actorType: "system",
      actorId: user.id,
      merchantId: user.merchantId ?? undefined,
      summary: `创建账号：${user.username}`,
    });
    return reply.code(201).send(user);
  });

  app.get("/api/admin/roles", async () => prisma.role.findMany({ orderBy: { createdAt: "desc" } }));

  app.post("/api/admin/roles", async (request, reply) => {
    const role = await prisma.role.create({ data: roleSchema.parse(request.body) });
    return reply.code(201).send(role);
  });

  app.get("/api/admin/permissions", async () => prisma.permission.findMany({ orderBy: { createdAt: "desc" } }));

  app.post("/api/admin/permissions", async (request, reply) => {
    const permission = await prisma.permission.create({ data: permissionSchema.parse(request.body) });
    return reply.code(201).send(permission);
  });
}
