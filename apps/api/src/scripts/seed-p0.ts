import argon2 from 'argon2';
import { prisma } from '../lib/prisma.js';

function requireSeedAdminUsername() {
  const username = process.env.P0_SEED_ADMIN_USERNAME ?? process.env.P0_ADMIN_USERNAME;
  if (!username) {
    throw new Error('P0_SEED_ADMIN_USERNAME or P0_ADMIN_USERNAME is required');
  }
  return username;
}

function requireSeedAdminPassword() {
  const password = process.env.P0_SEED_ADMIN_PASSWORD ?? process.env.P0_ADMIN_PASSWORD;
  if (!password) {
    throw new Error('P0_SEED_ADMIN_PASSWORD or P0_ADMIN_PASSWORD is required');
  }
  if (
    password.length < 12 ||
    !/[a-z]/.test(password) ||
    !/[A-Z]/.test(password) ||
    !/[0-9]/.test(password) ||
    !/[^A-Za-z0-9]/.test(password)
  ) {
    throw new Error('Seed admin password must be at least 12 chars and include upper, lower, number, and symbol');
  }
  return password;
}

async function main() {
  const adminUsername = requireSeedAdminUsername();
  const adminPassword = requireSeedAdminPassword();
  const organization = await prisma.organization.upsert({
    where: { code: 'platform-root' },
    update: {
      orgType: 'platform',
      name: 'Complex Platform Organization',
      shortName: 'Platform',
      status: 'active',
    },
    create: {
      code: 'platform-root',
      orgType: 'platform',
      name: 'Complex Platform Organization',
      shortName: 'Platform',
      status: 'active',
    },
  });

  const merchantData = {
    organizationId: organization.id,
    merchantType: 'platform',
    companyName: 'P0 Self Operated Complex Ltd',
    displayName: 'P0 Self Operated Complex',
    contactName: 'Platform Admin',
    contactPhone: '18800000000',
    settlementStatus: 'normal',
    riskLevel: 'normal',
    status: 'active',
  };
  const existingMerchant = await prisma.merchant.findFirst({
    where: {
      OR: [
        { contactPhone: merchantData.contactPhone },
        { displayName: merchantData.displayName },
      ],
    },
  });
  const merchant = existingMerchant
    ? await prisma.merchant.update({ where: { id: existingMerchant.id }, data: merchantData })
    : await prisma.merchant.create({ data: merchantData });

  const venue = await prisma.venue.upsert({
    where: { code: 'p0-main-venue' },
    update: {
      merchantId: merchant.id,
      venueType: 'complex',
      name: 'P0 Main Venue',
      floor: '1F',
      capacity: 500,
      status: 'active',
    },
    create: {
      merchantId: merchant.id,
      venueType: 'complex',
      name: 'P0 Main Venue',
      code: 'p0-main-venue',
      floor: '1F',
      capacity: 500,
      status: 'active',
    },
  });

  const existingArea = await prisma.area.findFirst({ where: { venueId: venue.id, areaType: 'entrance' } });
  const area = existingArea
    ? await prisma.area.update({
        where: { id: existingArea.id },
        data: { venueId: venue.id, name: 'P0 Entrance Area', areaType: 'entrance', floor: '1F', capacity: 120, status: 'active' },
      })
    : await prisma.area.create({
        data: { venueId: venue.id, name: 'P0 Entrance Area', areaType: 'entrance', floor: '1F', capacity: 120, status: 'active' },
      });

  const locationPoint = await prisma.locationPoint.upsert({
    where: { code: 'p0-gate-01' },
    update: {
      areaId: area.id,
      venueId: venue.id,
      merchantId: merchant.id,
      pointType: 'gate',
      name: 'P0 Gate Verification Point',
      capacity: 60,
      status: 'active',
    },
    create: {
      areaId: area.id,
      venueId: venue.id,
      merchantId: merchant.id,
      pointType: 'gate',
      name: 'P0 Gate Verification Point',
      code: 'p0-gate-01',
      capacity: 60,
      status: 'active',
    },
  });

  const adminPasswordHash = await argon2.hash(adminPassword);
  const admin = await prisma.userAccount.upsert({
    where: { username: adminUsername },
    update: { status: 'active', passwordHash: adminPasswordHash, organizationId: organization.id, merchantId: merchant.id },
    create: {
      username: adminUsername,
      phone: '18800000001',
      passwordHash: adminPasswordHash,
      accountType: 'platform_admin',
      organizationId: organization.id,
      merchantId: merchant.id,
      status: 'active',
    },
  });

  await prisma.role.upsert({
    where: { roleCode: 'platform_admin' },
    update: { roleName: 'Platform Admin', roleType: 'platform', organizationId: organization.id, status: 'active' },
    create: {
      roleCode: 'platform_admin',
      roleName: 'Platform Admin',
      roleType: 'platform',
      organizationId: organization.id,
      status: 'active',
    },
  });

  for (const permission of [
    ['platform.dashboard.view', 'View Platform Dashboard', 'dashboard', 'view'],
    ['ticket.verify', 'Verify Tickets', 'ticketing', 'verify'],
    ['bar.order.manage', 'Manage Bar Orders', 'bar', 'manage'],
    ['amusement.verify', 'Verify Amusement Access', 'amusement', 'verify'],
  ] as const) {
    await prisma.permission.upsert({
      where: { permissionCode: permission[0] },
      update: { permissionName: permission[1], permissionType: 'api', resource: permission[2], action: permission[3], status: 'active' },
      create: {
        permissionCode: permission[0],
        permissionName: permission[1],
        permissionType: 'api',
        resource: permission[2],
        action: permission[3],
        status: 'active',
      },
    });
  }

  const ticketProduct = await prisma.ticketProduct.upsert({
    where: { productCode: 'p0-pass' },
    update: {
      productName: 'P0 Experience Pass',
      productType: 'package',
      priceAmount: 9900,
      currency: 'CNY',
      status: 'active',
      description: 'P0 baseline pass',
    },
    create: {
      productCode: 'p0-pass',
      productName: 'P0 Experience Pass',
      productType: 'package',
      priceAmount: 9900,
      currency: 'CNY',
      status: 'active',
      description: 'P0 baseline pass',
    },
  });

  const existingTemplate = await prisma.entitlementTemplate.findFirst({
    where: { merchantId: merchant.id, venueId: venue.id, entitlementType: 'entry' },
  });
  const entryTemplate = existingTemplate
    ? await prisma.entitlementTemplate.update({
        where: { id: existingTemplate.id },
        data: { name: 'P0 Entry Entitlement', defaultTotalTimes: 1, defaultValueAmount: 9900, status: 'active' },
      })
    : await prisma.entitlementTemplate.create({
        data: {
          name: 'P0 Entry Entitlement',
          entitlementType: 'entry',
          merchantId: merchant.id,
          venueId: venue.id,
          defaultTotalTimes: 1,
          defaultValueAmount: 9900,
          status: 'active',
        },
      });

  const relation = await prisma.productEntitlement.findFirst({
    where: { ticketProductId: ticketProduct.id, entitlementTemplateId: entryTemplate.id },
  });
  if (!relation) {
    await prisma.productEntitlement.create({
      data: { ticketProductId: ticketProduct.id, entitlementTemplateId: entryTemplate.id, quantity: 1, totalTimes: 1, valueAmount: 9900 },
    });
  }

  const barTable = await prisma.barTable.upsert({
    where: { tableCode: 'p0-table-01' },
    update: { merchantId: merchant.id, venueId: venue.id, tableName: 'P0 Table 01', capacity: 4, minSpendAmount: 10000, status: 'idle' },
    create: {
      merchantId: merchant.id,
      venueId: venue.id,
      tableCode: 'p0-table-01',
      tableName: 'P0 Table 01',
      capacity: 4,
      minSpendAmount: 10000,
      status: 'idle',
    },
  });

  const existingBarProduct = await prisma.barProduct.findFirst({
    where: { merchantId: merchant.id, category: 'drink', priceAmount: 3000 },
  });
  const barProduct = existingBarProduct
    ? await prisma.barProduct.update({ where: { id: existingBarProduct.id }, data: { venueId: venue.id, productName: 'P0 Test Drink', status: 'active' } })
    : await prisma.barProduct.create({
        data: { merchantId: merchant.id, venueId: venue.id, category: 'drink', productName: 'P0 Test Drink', priceAmount: 3000, status: 'active' },
      });

  const project = await prisma.amusementProject.upsert({
    where: { projectCode: 'p0-amusement-01' },
    update: { merchantId: merchant.id, venueId: venue.id, projectName: 'P0 Amusement Project', capacity: 20, status: 'active' },
    create: {
      merchantId: merchant.id,
      venueId: venue.id,
      projectCode: 'p0-amusement-01',
      projectName: 'P0 Amusement Project',
      capacity: 20,
      status: 'active',
    },
  });

  const startsAt = new Date('2026-06-05T08:00:00.000Z');
  const endsAt = new Date('2026-06-05T09:00:00.000Z');
  const existingSession = await prisma.amusementSession.findFirst({ where: { projectId: project.id, startsAt, endsAt } });
  const session = existingSession
    ? await prisma.amusementSession.update({
        where: { id: existingSession.id },
        data: { sessionName: 'P0 Default Session', capacity: 20, status: 'scheduled' },
      })
    : await prisma.amusementSession.create({
        data: { projectId: project.id, sessionName: 'P0 Default Session', startsAt, endsAt, capacity: 20, usedCapacity: 0, status: 'scheduled' },
      });

  await prisma.riskRule.upsert({
    where: { ruleCode: 'P0_VERIFICATION_FAIL' },
    update: { ruleName: 'P0 Verification Failure Alert', ruleType: 'verification', threshold: 3, status: 'active' },
    create: {
      ruleCode: 'P0_VERIFICATION_FAIL',
      ruleName: 'P0 Verification Failure Alert',
      ruleType: 'verification',
      threshold: 3,
      status: 'active',
    },
  });

  console.log(JSON.stringify({
    organization: organization.id,
    merchant: merchant.id,
    venue: venue.id,
    locationPoint: locationPoint.id,
    admin: admin.username,
    ticketProduct: ticketProduct.productCode,
    barTable: barTable.tableCode,
    barProduct: barProduct.productName,
    amusementProject: project.projectCode,
    amusementSession: session.sessionName,
  }, null, 2));
}

main()
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
