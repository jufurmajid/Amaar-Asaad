const request = require('supertest');
const app = require('../src/app');
const { initDb, closeDb, getDb, getAsync } = require('../src/db');

describe('Backend API Tests', () => {
  beforeAll(async () => {
    const memoryDb = getDb(':memory:');
    await initDb(memoryDb);
  });

  afterAll(async () => {
    await closeDb();
  });

  test('GET /api/categories returns categories list', async () => {
    const res = await request(app).get('/api/categories');
    expect(res.status).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
    expect(res.body.length).toBeGreaterThan(0);
    expect(res.body[0]).toHaveProperty('id');
    expect(res.body[0]).toHaveProperty('name');
  });

  test('GET /api/products returns products list and supports filters', async () => {
    const resAll = await request(app).get('/api/products');
    expect(resAll.status).toBe(200);
    expect(Array.isArray(resAll.body)).toBe(true);
    expect(resAll.body.length).toBeGreaterThan(0);

    const resCategory = await request(app).get('/api/products?categoryId=cat_notebooks');
    expect(resCategory.status).toBe(200);
    expect(resCategory.body.every(p => p.categoryId === 'cat_notebooks')).toBe(true);

    const resSearch = await request(app).get(`/api/products?search=${encodeURIComponent('سلك')}`);
    expect(resSearch.status).toBe(200);
    expect(resSearch.body.length).toBeGreaterThan(0);
  });

  test('GET /api/products/:id returns product details or 404', async () => {
    const res = await request(app).get('/api/products/prod_1');
    expect(res.status).toBe(200);
    expect(res.body.id).toBe('prod_1');
    expect(res.body.price).toBe(3500);

    const res404 = await request(app).get('/api/products/non_existent');
    expect(res404.status).toBe(404);
  });

  test('POST /api/orders validates customer info', async () => {
    const res = await request(app)
      .post('/api/orders')
      .send({
        customerName: '',
        phone: '07701234567',
        address: 'بغداد',
        items: [{ productId: 'prod_1', quantity: 1 }]
      });

    expect(res.status).toBe(400);
    expect(res.body.error).toMatch(/اسم الزبون/);
  });

  test('POST /api/orders prevents ordering quantity greater than stock', async () => {
    const res = await request(app)
      .post('/api/orders')
      .send({
        customerName: 'علي أحمد',
        phone: '07701234567',
        address: 'بغداد - الكرادة',
        nearestLandmark: 'ساحة الواثق',
        items: [{ productId: 'prod_1', quantity: 999 }]
      });

    expect(res.status).toBe(400);
    expect(res.body.error).toMatch(/تتجاوز المخزون المتوفر/);
  });

  test('POST /api/orders calculates total on backend, sets status PENDING, and DOES NOT reduce stock', async () => {
    // Get product 1 initial stock
    const prodBefore = await getAsync('SELECT * FROM products WHERE id = ?', ['prod_1']);
    const initialStock = prodBefore.stock;

    const orderData = {
      customerName: 'محمد جاسم',
      phone: '07801234567',
      address: 'البصرة - الجزائر',
      nearestLandmark: 'قرب الجامعة',
      notes: 'يرجى الاتصال قبل التوصيل',
      items: [
        { productId: 'prod_1', quantity: 2, price: 1 } // Send fake price 1 to test price override
      ]
    };

    const res = await request(app).post('/api/orders').send(orderData);
    expect(res.status).toBe(201);
    expect(res.body).toHaveProperty('id');
    expect(res.body.id).toMatch(/^ORD-/);
    expect(res.body.status).toBe('PENDING');

    // DB price for prod_1 is 3500; quantity is 2. Total must be 7000 (not 2 from client)
    expect(res.body.totalPrice).toBe(7000);
    expect(res.body.items[0].subtotal).toBe(7000);

    // Verify stock was NOT reduced upon order creation
    const prodAfter = await getAsync('SELECT * FROM products WHERE id = ?', ['prod_1']);
    expect(prodAfter.stock).toBe(initialStock);
  });

  test('PATCH /api/orders/:id/status updates status and deducts stock when ACCEPTED', async () => {
    const prodBefore = await getAsync('SELECT * FROM products WHERE id = ?', ['prod_1']);
    const initialStock = prodBefore.stock;

    // Create a new order
    const orderRes = await request(app).post('/api/orders').send({
      customerName: 'سارة خالد',
      phone: '07712345678',
      address: 'أربيل',
      nearestLandmark: 'مول أربيل',
      items: [{ productId: 'prod_1', quantity: 3 }]
    });

    const orderId = orderRes.body.id;

    // Change status to ACCEPTED
    const patchRes = await request(app)
      .patch(`/api/orders/${orderId}/status`)
      .send({ status: 'ACCEPTED' });

    expect(patchRes.status).toBe(200);
    expect(patchRes.body.status).toBe('ACCEPTED');

    // Verify stock is reduced after ACCEPTED
    const prodAfter = await getAsync('SELECT * FROM products WHERE id = ?', ['prod_1']);
    expect(prodAfter.stock).toBe(initialStock - 3);
  });
});
