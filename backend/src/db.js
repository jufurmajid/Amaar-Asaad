const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const fs = require('fs');

const dbPath = process.env.DATABASE_URL || path.join(__dirname, '..', 'database.sqlite');

let db;

function getDb(customDbPath) {
  const targetPath = customDbPath || dbPath;
  if (targetPath === ':memory:') {
    return new sqlite3.Database(':memory:');
  }
  return new sqlite3.Database(targetPath);
}

function initDb(customDb) {
  return new Promise((resolve, reject) => {
    db = customDb || getDb();

    db.serialize(() => {
      // Categories Table
      db.run(`
        CREATE TABLE IF NOT EXISTS categories (
          id TEXT PRIMARY KEY,
          name TEXT NOT NULL,
          imageUrl TEXT,
          iconName TEXT,
          description TEXT,
          createdAt TEXT NOT NULL
        )
      `);

      // Products Table
      db.run(`
        CREATE TABLE IF NOT EXISTS products (
          id TEXT PRIMARY KEY,
          categoryId TEXT NOT NULL,
          name TEXT NOT NULL,
          description TEXT NOT NULL,
          price INTEGER NOT NULL,
          stock INTEGER NOT NULL DEFAULT 0,
          imageUrl TEXT NOT NULL,
          isAvailable INTEGER NOT NULL DEFAULT 1,
          isNewArrival INTEGER NOT NULL DEFAULT 0,
          isFeatured INTEGER NOT NULL DEFAULT 0,
          createdAt TEXT NOT NULL,
          updatedAt TEXT NOT NULL,
          FOREIGN KEY (categoryId) REFERENCES categories(id)
        )
      `);

      // Orders Table
      db.run(`
        CREATE TABLE IF NOT EXISTS orders (
          id TEXT PRIMARY KEY,
          customerName TEXT NOT NULL,
          phone TEXT NOT NULL,
          address TEXT NOT NULL,
          nearestLandmark TEXT NOT NULL,
          notes TEXT,
          totalPrice INTEGER NOT NULL,
          status TEXT NOT NULL DEFAULT 'PENDING',
          telegramChatId TEXT,
          createdAt TEXT NOT NULL,
          updatedAt TEXT NOT NULL
        )
      `);

      // Order Items Table
      db.run(`
        CREATE TABLE IF NOT EXISTS order_items (
          id TEXT PRIMARY KEY,
          orderId TEXT NOT NULL,
          productId TEXT NOT NULL,
          productName TEXT NOT NULL,
          quantity INTEGER NOT NULL,
          price INTEGER NOT NULL,
          subtotal INTEGER NOT NULL,
          FOREIGN KEY (orderId) REFERENCES orders(id),
          FOREIGN KEY (productId) REFERENCES products(id)
        )
      `, (err) => {
        if (err) return reject(err);
        seedDbIfEmpty().then(resolve).catch(reject);
      });
    });
  });
}

function runAsync(sql, params = []) {
  return new Promise((resolve, reject) => {
    db.run(sql, params, function (err) {
      if (err) reject(err);
      else resolve(this);
    });
  });
}

function getAsync(sql, params = []) {
  return new Promise((resolve, reject) => {
    db.get(sql, params, (err, row) => {
      if (err) reject(err);
      else resolve(row);
    });
  });
}

function allAsync(sql, params = []) {
  return new Promise((resolve, reject) => {
    db.all(sql, params, (err, rows) => {
      if (err) reject(err);
      else resolve(rows);
    });
  });
}

async function seedDbIfEmpty() {
  const categoryCount = await getAsync('SELECT COUNT(*) as count FROM categories');
  if (categoryCount && categoryCount.count > 0) {
    return;
  }

  const now = new Date().toISOString();

  // Categories seed data
  const seedCategories = [
    {
      id: 'cat_notebooks',
      name: 'دفاتر وسجلات',
      imageUrl: 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500',
      iconName: 'book',
      description: 'دفاتر مدرسية وجامعية وسجلات راقية',
      createdAt: now
    },
    {
      id: 'cat_pens',
      name: 'قلام ومستلزمات الكتابة',
      imageUrl: 'https://images.unsplash.com/photo-1583485088034-697b5bc54ccd?w=500',
      iconName: 'edit',
      description: 'أقلام حبر، جاف، ورصاص وماركير جودة عالية',
      createdAt: now
    },
    {
      id: 'cat_art',
      name: 'أدوات رسم وفنون',
      imageUrl: 'https://images.unsplash.com/photo-1513364776144-60967b0f800f?w=500',
      iconName: 'palette',
      description: 'ألوان، أوراق رسم، وفرش لجميع المستويات',
      createdAt: now
    },
    {
      id: 'cat_office',
      name: 'مكتبية وتنظيم',
      imageUrl: 'https://images.unsplash.com/photo-1507208773393-424d13198406?w=500',
      iconName: 'folder',
      description: 'ملفات، كابسات، ولاصق ولوازم مكتبية',
      createdAt: now
    }
  ];

  for (const cat of seedCategories) {
    await runAsync(
      `INSERT INTO categories (id, name, imageUrl, iconName, description, createdAt) VALUES (?, ?, ?, ?, ?, ?)`,
      [cat.id, cat.name, cat.imageUrl, cat.iconName, cat.description, cat.createdAt]
    );
  }

  // Products seed data
  const seedProducts = [
    {
      id: 'prod_1',
      categoryId: 'cat_notebooks',
      name: 'دفتر سلك جامعي 100 ورقة',
      description: 'دفتر جامعي غلاف مقوى بتصميم عصري وأوراق عالية الجودة لا تنفذ الحبر.',
      price: 3500,
      stock: 50,
      imageUrl: 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500',
      isAvailable: 1,
      isNewArrival: 1,
      isFeatured: 1,
      createdAt: now,
      updatedAt: now
    },
    {
      id: 'prod_2',
      categoryId: 'cat_notebooks',
      name: 'سجل مقوى 200 ورقة A4',
      description: 'سجل رسمي فاخر مناسب للملاحظات والتدوين اليومي.',
      price: 6000,
      stock: 30,
      imageUrl: 'https://images.unsplash.com/photo-1589829085413-56de8ae18c73?w=500',
      isAvailable: 1,
      isNewArrival: 0,
      isFeatured: 1,
      createdAt: now,
      updatedAt: now
    },
    {
      id: 'prod_3',
      categoryId: 'cat_pens',
      name: 'طقم أقلام جاف أزرق 10 قطع',
      description: 'أقلام جاف خط ناعم وسلس، تدوم طويلاً بدون انقطاع.',
      price: 2500,
      stock: 100,
      imageUrl: 'https://images.unsplash.com/photo-1583485088034-697b5bc54ccd?w=500',
      isAvailable: 1,
      isNewArrival: 0,
      isFeatured: 1,
      createdAt: now,
      updatedAt: now
    },
    {
      id: 'prod_4',
      categoryId: 'cat_pens',
      name: 'قلم حبر جاف فاخر هدية',
      description: 'قلم معدني أنيق بتصميم كلاسيكي مناسب للهدايا والاستخدام الشخصي.',
      price: 12000,
      stock: 15,
      imageUrl: 'https://images.unsplash.com/photo-1585336261026-8f5786372966?w=500',
      isAvailable: 1,
      isNewArrival: 1,
      isFeatured: 0,
      createdAt: now,
      updatedAt: now
    },
    {
      id: 'prod_5',
      categoryId: 'cat_art',
      name: 'علبة ألوان خشبية 24 لون',
      description: 'ألوان خشبية زاهية وسهلة الدمج ومناسبة للطلاب والموهوبين.',
      price: 8500,
      stock: 25,
      imageUrl: 'https://images.unsplash.com/photo-1513364776144-60967b0f800f?w=500',
      isAvailable: 1,
      isNewArrival: 1,
      isFeatured: 1,
      createdAt: now,
      updatedAt: now
    },
    {
      id: 'prod_6',
      categoryId: 'cat_office',
      name: 'منظم مكتب أكريليك شفاف',
      description: 'منظم متعدّد الأقسام للأقلام والأوراق والأدوات المكتبية.',
      price: 15000,
      stock: 10,
      imageUrl: 'https://images.unsplash.com/photo-1507208773393-424d13198406?w=500',
      isAvailable: 1,
      isNewArrival: 0,
      isFeatured: 0,
      createdAt: now,
      updatedAt: now
    }
  ];

  for (const prod of seedProducts) {
    await runAsync(
      `INSERT INTO products (id, categoryId, name, description, price, stock, imageUrl, isAvailable, isNewArrival, isFeatured, createdAt, updatedAt)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        prod.id,
        prod.categoryId,
        prod.name,
        prod.description,
        prod.price,
        prod.stock,
        prod.imageUrl,
        prod.isAvailable,
        prod.isNewArrival,
        prod.isFeatured,
        prod.createdAt,
        prod.updatedAt
      ]
    );
  }
}

function closeDb() {
  return new Promise((resolve, reject) => {
    if (!db) return resolve();
    db.close((err) => {
      if (err) reject(err);
      else resolve();
    });
  });
}

module.exports = {
  getDb,
  initDb,
  runAsync,
  getAsync,
  allAsync,
  closeDb
};
