type JsonObject = Record<string, unknown>;

type IdResponse = { id: string };
type LoginResponse = { accessToken: string };
type InventoryItemResponse = { id: string; itemName: string; itemCode: string };
type StockMovementResponse = {
  id: string;
  movementType: string;
  quantity: number;
  afterFromQuantity?: number | null;
  afterToQuantity?: number | null;
};
type StockBalanceResponse = { id: string; itemId: string; locationType: string; quantity: number };
type SavedBottleResponse = {
  id: string;
  savedNo: string;
  customerName: string;
  remainingValue: number;
  status: string;
};
type BarReservationResponse = { id: string; reservationNo: string; status: string; depositAmount: number };
type BarOrderItemResponse = { id: string; quantity: number; status: string };
type BarProductionTicketResponse = { id: string; ticketNo: string; department: string; status: string };

const baseUrl = process.env.P1_API_BASE_URL ?? process.env.P0_API_BASE_URL ?? 'http://127.0.0.1:8080';
let accessToken = '';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  if (accessToken) headers.set('authorization', `Bearer ${accessToken}`);
  const response = await fetch(`${baseUrl}${path}`, { ...init, headers });
  const text = await response.text();
  const body = text ? JSON.parse(text) : null;
  if (!response.ok) throw new Error(`${path} ${response.status}: ${text}`);
  return body as T;
}

async function post<T>(path: string, body: JsonObject): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(body),
  });
}

async function postFailure(path: string, body: JsonObject) {
  const headers = new Headers({ 'content-type': 'application/json' });
  if (accessToken) headers.set('authorization', `Bearer ${accessToken}`);
  const response = await fetch(`${baseUrl}${path}`, {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
  });
  const text = await response.text();
  return { status: response.status, body: text ? JSON.parse(text) : null };
}

function assertOk(condition: unknown, message: string) {
  if (!condition) throw new Error(message);
}

async function main() {
  const stamp = Date.now();
  const adminUsername = process.env.P0_ADMIN_USERNAME;
  const adminPassword = process.env.P0_ADMIN_PASSWORD;
  if (!adminUsername || !adminPassword) {
    throw new Error('P0_ADMIN_USERNAME and P0_ADMIN_PASSWORD are required');
  }

  const health = await request<{ ok: boolean }>('/api/health');
  assertOk(health.ok, 'health check failed');
  const login = await post<LoginResponse>('/api/auth/login', { username: adminUsername, password: adminPassword });
  assertOk(login.accessToken, 'admin login failed');
  accessToken = login.accessToken;

  const organization = await post<IdResponse>('/api/admin/organizations', {
    orgType: 'platform',
    name: `P1 Bar Org ${stamp}`,
    shortName: 'P1 Bar',
    code: `p1-bar-org-${stamp}`,
  });
  const merchant = await post<IdResponse>('/api/admin/merchants', {
    organizationId: organization.id,
    merchantType: 'bar',
    companyName: `P1 Bar Merchant Co ${stamp}`,
    displayName: `P1 Bar Merchant ${stamp}`,
    contactName: 'P1 Bar',
    contactPhone: '18800000101',
    status: 'active',
  });
  const venue = await post<IdResponse>('/api/admin/venues', {
    merchantId: merchant.id,
    venueType: 'bar',
    name: `P1 Bar Venue ${stamp}`,
    code: `p1-bar-venue-${stamp}`,
    floor: '1F',
    capacity: 120,
    status: 'active',
  });

  const product = await post<IdResponse>('/api/admin/bar/products', {
    merchantId: merchant.id,
    venueId: venue.id,
    productName: `P1 Whisky ${stamp}`,
    category: 'drink',
    priceAmount: 168800,
  });
  const foodProduct = await post<IdResponse>('/api/admin/bar/products', {
    merchantId: merchant.id,
    venueId: venue.id,
    productName: `P1 Snack ${stamp}`,
    category: 'food',
    priceAmount: 5800,
  });
  const table = await post<IdResponse>('/api/admin/bar/tables', {
    merchantId: merchant.id,
    venueId: venue.id,
    tableName: `P1 Table ${stamp}`,
    capacity: 4,
    minSpendAmount: 10000,
  });
  const reservation = await post<BarReservationResponse>('/api/admin/bar/reservations', {
    merchantId: merchant.id,
    venueId: venue.id,
    tableId: table.id,
    customerName: 'P1 Reserved Guest',
    customerPhone: '18800000104',
    reservedAt: new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString(),
    partySize: 3,
    depositAmount: 20000,
  });
  const checkedInReservation = await post<BarReservationResponse>(`/api/admin/bar/reservations/${reservation.id}/check-in`, {});
  assertOk(checkedInReservation.status === 'checked_in', 'bar reservation check-in failed');
  const order = await post<IdResponse>('/api/admin/bar/orders', {
    merchantId: merchant.id,
    venueId: venue.id,
    tableId: table.id,
  });
  const foodItem = await post<BarOrderItemResponse>(`/api/admin/bar/orders/${order.id}/items`, {
    productId: foodProduct.id,
    quantity: 2,
  });
  assertOk(foodItem.quantity === 2, 'bar food order item failed');
  const productionTickets = await request<BarProductionTicketResponse[]>(`/api/admin/bar/production-tickets?merchantId=${merchant.id}`);
  const kitchenTicket = productionTickets.find((ticket) => ticket.department === 'kitchen' && ticket.status === 'pending');
  assertOk(kitchenTicket, 'kitchen production ticket missing');
  const startedTicket = await post<BarProductionTicketResponse>(`/api/admin/bar/production-tickets/${kitchenTicket!.id}/start`, {});
  const servedTicket = await post<BarProductionTicketResponse>(`/api/admin/bar/production-tickets/${kitchenTicket!.id}/serve`, {});
  assertOk(startedTicket.status === 'producing' && servedTicket.status === 'served', 'bar production flow failed');
  const item = await post<InventoryItemResponse>('/api/admin/bar/inventory-items', {
    merchantId: merchant.id,
    venueId: venue.id,
    itemName: `P1 Whisky Bottle ${stamp}`,
    category: 'drink',
    unit: 'bottle',
    minQuantity: 3,
  });

  const inbound = await post<StockMovementResponse>('/api/admin/bar/stock-movements/inbound', {
    merchantId: merchant.id,
    venueId: venue.id,
    itemId: item.id,
    productId: product.id,
    quantity: 20,
    locationType: 'warehouse_stock',
    reason: 'P1 smoke inbound',
  });
  assertOk(inbound.movementType === 'inbound' && inbound.afterToQuantity === 20, 'inbound failed');

  const transfer = await post<StockMovementResponse>('/api/admin/bar/stock-movements/transfer', {
    merchantId: merchant.id,
    venueId: venue.id,
    itemId: item.id,
    productId: product.id,
    quantity: 8,
    fromLocationType: 'warehouse_stock',
    toLocationType: 'bar_stock',
    reason: 'P1 smoke transfer',
  });
  assertOk(transfer.afterFromQuantity === 12 && transfer.afterToQuantity === 8, 'transfer failed');

  const damage = await post<StockMovementResponse>('/api/admin/bar/stock-movements/damage', {
    merchantId: merchant.id,
    venueId: venue.id,
    itemId: item.id,
    productId: product.id,
    quantity: 1,
    locationType: 'bar_stock',
    reason: 'P1 smoke damage',
  });
  assertOk(damage.afterFromQuantity === 7, 'damage failed');

  const count = await post<StockMovementResponse>('/api/admin/bar/stock-counts', {
    merchantId: merchant.id,
    venueId: venue.id,
    itemId: item.id,
    productId: product.id,
    countedQuantity: 6,
    locationType: 'bar_stock',
    reason: 'P1 smoke count',
  });
  assertOk(count.afterToQuantity === 6, 'stock count failed');

  const stockNotEnough = await postFailure('/api/admin/bar/stock-movements/outbound', {
    merchantId: merchant.id,
    venueId: venue.id,
    itemId: item.id,
    productId: product.id,
    quantity: 999,
    locationType: 'bar_stock',
    reason: 'P1 smoke should fail',
  });
  assertOk(stockNotEnough.status === 409, 'stock shortage protection failed');

  const balances = await request<StockBalanceResponse[]>(`/api/admin/bar/stock-balances?merchantId=${merchant.id}&itemId=${item.id}`);
  const barBalance = balances.find((balance) => balance.locationType === 'bar_stock');
  const warehouseBalance = balances.find((balance) => balance.locationType === 'warehouse_stock');
  assertOk(barBalance?.quantity === 6 && warehouseBalance?.quantity === 12, 'stock balances mismatch');

  const savedBottle = await post<SavedBottleResponse>('/api/admin/bar/saved-bottles', {
    merchantId: merchant.id,
    venueId: venue.id,
    productId: product.id,
    customerName: 'P1 Saved Customer',
    customerPhone: '18800000102',
    storageLocation: 'A-01',
    initialUnit: 'percent',
    initialValue: 100,
    expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
  });
  assertOk(savedBottle.status === 'stored' && savedBottle.remainingValue === 100, 'saved bottle create failed');

  const pickup = await post<SavedBottleResponse>(`/api/admin/bar/saved-bottles/${savedBottle.id}/pickup`, {
    quantity: 35,
    reason: 'P1 smoke pickup',
  });
  assertOk(pickup.status === 'partially_used' && pickup.remainingValue === 65, 'saved bottle pickup failed');

  const transferBottle = await post<SavedBottleResponse>(`/api/admin/bar/saved-bottles/${savedBottle.id}/transfer`, {
    customerName: 'P1 Transfer Customer',
    customerPhone: '18800000103',
    reason: 'P1 smoke transfer owner',
  });
  assertOk(transferBottle.customerName === 'P1 Transfer Customer', 'saved bottle transfer failed');

  const pickupTooMuch = await postFailure(`/api/admin/bar/saved-bottles/${savedBottle.id}/pickup`, {
    quantity: 999,
    reason: 'P1 smoke should fail',
  });
  assertOk(pickupTooMuch.status === 409, 'saved bottle shortage protection failed');

  const voidedBottle = await post<SavedBottleResponse>(`/api/admin/bar/saved-bottles/${savedBottle.id}/void`, {
    reason: 'P1 smoke void',
  });
  assertOk(voidedBottle.status === 'voided', 'saved bottle void failed');

  const savedBottleRows = await request<SavedBottleResponse[]>(`/api/admin/bar/saved-bottles?merchantId=${merchant.id}`);
  const movements = await request<StockMovementResponse[]>(`/api/admin/bar/stock-movements?merchantId=${merchant.id}&itemId=${item.id}`);

  console.log(
    JSON.stringify(
      {
        health: health.ok,
        login: Boolean(login.accessToken),
        foundation: { organization: organization.id, merchant: merchant.id, venue: venue.id },
        production: {
          reservation: checkedInReservation.status,
          foodOrderItem: foodItem.id,
          kitchenTicket: servedTicket.status,
        },
        inventory: {
          item: item.itemCode,
          inbound: inbound.afterToQuantity,
          transfer: { warehouse: transfer.afterFromQuantity, bar: transfer.afterToQuantity },
          damage: damage.afterFromQuantity,
          count: count.afterToQuantity,
          shortageRejected: stockNotEnough.status,
          balances: { warehouse: warehouseBalance?.quantity, bar: barBalance?.quantity },
          movements: movements.length,
        },
        savedBottle: {
          savedNo: savedBottle.savedNo,
          afterPickup: pickup.remainingValue,
          transferredTo: transferBottle.customerName,
          pickupRejected: pickupTooMuch.status,
          finalStatus: voidedBottle.status,
          rows: savedBottleRows.length,
        },
      },
      null,
      2,
    ),
  );
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
