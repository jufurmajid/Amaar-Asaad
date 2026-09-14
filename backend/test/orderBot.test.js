const {
  processOrderAcceptance,
  processOrderRejection,
  processOrderStatusChange,
  getStatusArabic
} = require('../src/telegram/orderBot');
const { initDb, closeDb, getDb, getAsync, runAsync } = require('../src/db');

describe('Telegram Order Bot Business Logic Tests', () => {
  beforeAll(async () => {
    const memoryDb = getDb(':memory:');
    await initDb(memoryDb);
  });

  afterAll(async () => {
    await closeDb();
  });

  test('processOrderAcceptance deducts stock safely and prevents double acceptance', async () => {
    // Check initial stock for prod_1
    const pBefore = await getAsync('SELECT stock FROM products WHERE id = ?', ['prod_1']);
    const initialStock = pBefore.stock;

    // Create a pending order in DB
    const orderId = 'ORD-TEST-ACCEPT';
    const now = new Date().toISOString();
    await runAsync(
      `INSERT INTO orders (id, customerName, phone, address, nearestLandmark, totalPrice, status, createdAt, updatedAt)
       VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?, ?)`,
      [orderId, 'علي', '0770', 'بغداد', 'الكرادة', 7000, now, now]
    );
    await runAsync(
      `INSERT INTO order_items (id, orderId, productId, productName, quantity, price, subtotal)
       VALUES ('ITEM-1', ?, 'prod_1', 'دفتر سلك', 2, 3500, 7000)`,
      [orderId]
    );

    // First Acceptance
    const res1 = await processOrderAcceptance(orderId, null);
    expect(res1.success).toBe(true);
    expect(res1.message).toMatch(/تم قبول الطلب/);

    // Verify DB Status and Stock
    const orderAfter1 = await getAsync('SELECT status FROM orders WHERE id = ?', [orderId]);
    expect(orderAfter1.status).toBe('ACCEPTED');

    const pAfter1 = await getAsync('SELECT stock FROM products WHERE id = ?', ['prod_1']);
    expect(pAfter1.stock).toBe(initialStock - 2);

    // Second Acceptance Attempt (Double Acceptance Guard)
    const res2 = await processOrderAcceptance(orderId, null);
    expect(res2.success).toBe(false);
    expect(res2.message).toMatch(/تم قبول هذا الطلب سابقاً/);

    // Stock should not be deducted again
    const pAfter2 = await getAsync('SELECT stock FROM products WHERE id = ?', ['prod_1']);
    expect(pAfter2.stock).toBe(initialStock - 2);
  });

  test('processOrderRejection sets status to REJECTED and does NOT deduct stock', async () => {
    const pBefore = await getAsync('SELECT stock FROM products WHERE id = ?', ['prod_1']);
    const stockBefore = pBefore.stock;

    const orderId = 'ORD-TEST-REJECT';
    const now = new Date().toISOString();
    await runAsync(
      `INSERT INTO orders (id, customerName, phone, address, nearestLandmark, totalPrice, status, createdAt, updatedAt)
       VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?, ?)`,
      [orderId, 'حسن', '0780', 'البصرة', 'الجزائر', 3500, now, now]
    );
    await runAsync(
      `INSERT INTO order_items (id, orderId, productId, productName, quantity, price, subtotal)
       VALUES ('ITEM-2', ?, 'prod_1', 'دفتر سلك', 1, 3500, 3500)`,
      [orderId]
    );

    const res = await processOrderRejection(orderId, null);
    expect(res.success).toBe(true);

    const orderAfter = await getAsync('SELECT status FROM orders WHERE id = ?', [orderId]);
    expect(orderAfter.status).toBe('REJECTED');

    // Stock should be unchanged
    const pAfter = await getAsync('SELECT stock FROM products WHERE id = ?', ['prod_1']);
    expect(pAfter.stock).toBe(stockBefore);
  });

  test('processOrderStatusChange updates status to PREPARING, READY, DELIVERED', async () => {
    const orderId = 'ORD-TEST-ACCEPT'; // Previously accepted

    let res = await processOrderStatusChange(orderId, 'PREPARING', null);
    expect(res.success).toBe(true);
    let o = await getAsync('SELECT status FROM orders WHERE id = ?', [orderId]);
    expect(o.status).toBe('PREPARING');

    res = await processOrderStatusChange(orderId, 'READY', null);
    expect(res.success).toBe(true);
    o = await getAsync('SELECT status FROM orders WHERE id = ?', [orderId]);
    expect(o.status).toBe('READY');

    res = await processOrderStatusChange(orderId, 'DELIVERED', null);
    expect(res.success).toBe(true);
    o = await getAsync('SELECT status FROM orders WHERE id = ?', [orderId]);
    expect(o.status).toBe('DELIVERED');
  });

  test('getStatusArabic returns expected translations', () => {
    expect(getStatusArabic('PENDING')).toBe('⏳ قيد الانتظار');
    expect(getStatusArabic('ACCEPTED')).toBe('✅ مقبول');
    expect(getStatusArabic('REJECTED')).toBe('❌ مرفوض');
  });
});
