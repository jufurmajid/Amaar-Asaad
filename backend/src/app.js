const express = require('express');
const cors = require('cors');
const crypto = require('crypto');
const { runAsync, getAsync, allAsync } = require('./db');

const app = express();

app.use(cors());
app.use(express.json());

// GET /api/categories
app.get('/api/categories', async (req, res) => {
  try {
    const categories = await allAsync('SELECT * FROM categories ORDER BY name ASC');
    const formatted = categories.map(c => ({
      id: c.id,
      name: c.name,
      nameAr: c.name,
      imageUrl: c.imageUrl,
      iconName: c.iconName,
      description: c.description,
      createdAt: c.createdAt
    }));
    res.json(formatted);
  } catch (err) {
    res.status(500).json({ error: 'فشل في جلب الأقسام', details: err.message });
  }
});

// GET /api/products
app.get('/api/products', async (req, res) => {
  try {
    const { categoryId, search, featured, newArrivals } = req.query;

    let sql = 'SELECT * FROM products WHERE isAvailable = 1';
    const params = [];

    if (categoryId) {
      sql += ' AND categoryId = ?';
      params.push(categoryId);
    }

    if (featured === 'true' || featured === '1') {
      sql += ' AND isFeatured = 1';
    }

    if (newArrivals === 'true' || newArrivals === '1') {
      sql += ' AND isNewArrival = 1';
    }

    if (search && search.trim() !== '') {
      sql += ' AND (name LIKE ? OR description LIKE ?)';
      const queryParam = `%${search.trim()}%`;
      params.push(queryParam, queryParam);
    }

    sql += ' ORDER BY createdAt DESC';

    const products = await allAsync(sql, params);
    const formatted = products.map(p => ({
      id: p.id,
      categoryId: p.categoryId,
      name: p.name,
      nameAr: p.name,
      description: p.description,
      descriptionAr: p.description,
      price: p.price,
      priceIqd: p.price,
      stock: p.stock,
      stockQuantity: p.stock,
      imageUrl: p.imageUrl,
      isAvailable: Boolean(p.isAvailable),
      isNewArrival: Boolean(p.isNewArrival),
      isFeatured: Boolean(p.isFeatured),
      createdAt: p.createdAt,
      updatedAt: p.updatedAt
    }));

    res.json(formatted);
  } catch (err) {
    res.status(500).json({ error: 'فشل في جلب المنتجات', details: err.message });
  }
});

// GET /api/products/:id
app.get('/api/products/:id', async (req, res) => {
  try {
    const product = await getAsync('SELECT * FROM products WHERE id = ?', [req.params.id]);
    if (!product) {
      return res.status(404).json({ error: 'المنتج غير موجود' });
    }

    res.json({
      id: product.id,
      categoryId: product.categoryId,
      name: product.name,
      nameAr: product.name,
      description: product.description,
      descriptionAr: product.description,
      price: product.price,
      priceIqd: product.price,
      stock: product.stock,
      stockQuantity: product.stock,
      imageUrl: product.imageUrl,
      isAvailable: Boolean(product.isAvailable),
      isNewArrival: Boolean(product.isNewArrival),
      isFeatured: Boolean(product.isFeatured),
      createdAt: product.createdAt,
      updatedAt: product.updatedAt
    });
  } catch (err) {
    res.status(500).json({ error: 'فشل في جلب تفاصيل المنتج', details: err.message });
  }
});

// GET /api/orders
app.get('/api/orders', async (req, res) => {
  try {
    const orders = await allAsync('SELECT * FROM orders ORDER BY createdAt DESC');
    const result = [];
    for (const order of orders) {
      const items = await allAsync('SELECT * FROM order_items WHERE orderId = ?', [order.id]);
      result.push({
        id: order.id,
        customerName: order.customerName,
        phone: order.phone,
        address: order.address,
        nearestLandmark: order.nearestLandmark,
        notes: order.notes,
        totalPrice: order.totalPrice,
        status: order.status,
        telegramChatId: order.telegramChatId,
        createdAt: order.createdAt,
        updatedAt: order.updatedAt,
        items: items.map(i => ({
          productId: i.productId,
          productName: i.productName,
          quantity: i.quantity,
          price: i.price,
          subtotal: i.subtotal
        }))
      });
    }
    res.json(result);
  } catch (err) {
    res.status(500).json({ error: 'فشل في جلب الطلبات', details: err.message });
  }
});

// POST /api/orders
app.post('/api/orders', async (req, res) => {
  try {
    const {
      customerName,
      fullName,
      phone,
      phoneNumber,
      address,
      nearestLandmark,
      notes,
      items,
      telegramChatId
    } = req.body;

    const finalCustomerName = (customerName || fullName || '').trim();
    const finalPhone = (phone || phoneNumber || '').trim();
    const finalAddress = (address || '').trim();
    const finalLandmark = (nearestLandmark || '').trim();
    const finalNotes = (notes || '').trim();

    // Validation
    if (!finalCustomerName) {
      return res.status(400).json({ error: 'الرجاء إدخال اسم الزبون' });
    }
    if (!finalPhone) {
      return res.status(400).json({ error: 'الرجاء إدخال رقم الهاتف' });
    }
    if (!finalAddress) {
      return res.status(400).json({ error: 'الرجاء إدخال العنوان' });
    }
    if (!Array.isArray(items) || items.length === 0) {
      return res.status(400).json({ error: 'الرجاء إضافة منتج واحد على الأقل للطلب' });
    }

    let calculatedTotal = 0;
    const validatedItems = [];

    // Verify stock and calculate total using DB prices
    for (const item of items) {
      if (!item.productId || !item.quantity || item.quantity <= 0) {
        return res.status(400).json({ error: 'بيانات المنتج غير صالحة في الطلب' });
      }

      const product = await getAsync('SELECT * FROM products WHERE id = ?', [item.productId]);
      if (!product) {
        return res.status(400).json({ error: `المنتج رقم ${item.productId} غير موجود` });
      }

      if (!product.isAvailable) {
        return res.status(400).json({ error: `المنتج "${product.name}" غير متوفر حالياً` });
      }

      if (item.quantity > product.stock) {
        return res.status(400).json({
          error: `الكمية المطلوبة للمنتج "${product.name}" (${item.quantity}) تتجاوز المخزون المتوفر (${product.stock})`
        });
      }

      const dbPrice = product.price;
      const subtotal = dbPrice * item.quantity;
      calculatedTotal += subtotal;

      validatedItems.push({
        productId: product.id,
        productName: product.name,
        quantity: item.quantity,
        price: dbPrice,
        subtotal: subtotal
      });
    }

    const now = new Date().toISOString();
    const randomUuid = crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substring(2, 10);
    const orderId = `ORD-${randomUuid.substring(0, 8).toUpperCase()}`;
    const initialStatus = 'PENDING';

    // Insert Order into DB
    await runAsync(
      `INSERT INTO orders (id, customerName, phone, address, nearestLandmark, notes, totalPrice, status, telegramChatId, createdAt, updatedAt)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        orderId,
        finalCustomerName,
        finalPhone,
        finalAddress,
        finalLandmark,
        finalNotes,
        calculatedTotal,
        initialStatus,
        telegramChatId || null,
        now,
        now
      ]
    );

    // Insert Order Items into DB
    for (const item of validatedItems) {
      const itemUuid = crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substring(2, 10);
      const itemId = `ITEM-${itemUuid.substring(0, 8).toUpperCase()}`;
      await runAsync(
        `INSERT INTO order_items (id, orderId, productId, productName, quantity, price, subtotal)
         VALUES (?, ?, ?, ?, ?, ?, ?)`,
        [
          itemId,
          orderId,
          item.productId,
          item.productName,
          item.quantity,
          item.price,
          item.subtotal
        ]
      );
    }

    const createdOrder = {
      id: orderId,
      customerName: finalCustomerName,
      phone: finalPhone,
      address: finalAddress,
      nearestLandmark: finalLandmark,
      notes: finalNotes,
      totalPrice: calculatedTotal,
      totalAmountIqd: calculatedTotal,
      status: initialStatus,
      telegramChatId: telegramChatId || null,
      createdAt: now,
      updatedAt: now,
      items: validatedItems
    };

    res.status(201).json(createdOrder);
  } catch (err) {
    res.status(500).json({ error: 'فشل في إنشاء الطلب', details: err.message });
  }
});

// GET /api/orders/:id
app.get('/api/orders/:id', async (req, res) => {
  try {
    const order = await getAsync('SELECT * FROM orders WHERE id = ?', [req.params.id]);
    if (!order) {
      return res.status(404).json({ error: 'الطلب غير موجود' });
    }

    const items = await allAsync('SELECT * FROM order_items WHERE orderId = ?', [order.id]);

    res.json({
      id: order.id,
      customerName: order.customerName,
      phone: order.phone,
      address: order.address,
      nearestLandmark: order.nearestLandmark,
      notes: order.notes,
      totalPrice: order.totalPrice,
      totalAmountIqd: order.totalPrice,
      status: order.status,
      telegramChatId: order.telegramChatId,
      createdAt: order.createdAt,
      updatedAt: order.updatedAt,
      items: items.map(i => ({
        productId: i.productId,
        productName: i.productName,
        quantity: i.quantity,
        price: i.price,
        subtotal: i.subtotal
      }))
    });
  } catch (err) {
    res.status(500).json({ error: 'فشل في جلب تفاصيل الطلب', details: err.message });
  }
});

// PATCH /api/orders/:id/status
app.patch('/api/orders/:id/status', async (req, res) => {
  try {
    const { status } = req.body;
    const allowedStatuses = ['PENDING', 'ACCEPTED', 'REJECTED', 'PREPARING', 'READY', 'DELIVERED'];

    if (!status || !allowedStatuses.includes(status)) {
      return res.status(400).json({ error: 'حالة الطلب غير صالحة' });
    }

    const order = await getAsync('SELECT * FROM orders WHERE id = ?', [req.params.id]);
    if (!order) {
      return res.status(404).json({ error: 'الطلب غير موجود' });
    }

    const now = new Date().toISOString();

    // If order transitions to ACCEPTED for the first time, deduct stock (for future Phase 3 readiness)
    if (status === 'ACCEPTED' && order.status !== 'ACCEPTED') {
      const items = await allAsync('SELECT * FROM order_items WHERE orderId = ?', [order.id]);
      for (const item of items) {
        await runAsync(
          'UPDATE products SET stock = MAX(0, stock - ?), updatedAt = ? WHERE id = ?',
          [item.quantity, now, item.productId]
        );
      }
    }

    await runAsync(
      'UPDATE orders SET status = ?, updatedAt = ? WHERE id = ?',
      [status, now, req.params.id]
    );

    const updatedOrder = await getAsync('SELECT * FROM orders WHERE id = ?', [req.params.id]);
    const items = await allAsync('SELECT * FROM order_items WHERE orderId = ?', [order.id]);

    res.json({
      id: updatedOrder.id,
      customerName: updatedOrder.customerName,
      phone: updatedOrder.phone,
      address: updatedOrder.address,
      nearestLandmark: updatedOrder.nearestLandmark,
      notes: updatedOrder.notes,
      totalPrice: updatedOrder.totalPrice,
      status: updatedOrder.status,
      telegramChatId: updatedOrder.telegramChatId,
      createdAt: updatedOrder.createdAt,
      updatedAt: updatedOrder.updatedAt,
      items: items.map(i => ({
        productId: i.productId,
        productName: i.productName,
        quantity: i.quantity,
        price: i.price,
        subtotal: i.subtotal
      }))
    });
  } catch (err) {
    res.status(500).json({ error: 'فشل في تحديث حالة الطلب', details: err.message });
  }
});

module.exports = app;
