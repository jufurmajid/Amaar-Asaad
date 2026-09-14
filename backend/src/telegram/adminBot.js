const TelegramBot = require('node-telegram-bot-api');
const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');
const { allAsync, getAsync, runAsync } = require('../db');

function isAuthorized(msg) {
  const adminChatId = process.env.ADMIN_TELEGRAM_CHAT_ID;
  const adminUserIds = (process.env.ADMIN_TELEGRAM_USER_IDS || '').split(',').map(s => s.trim()).filter(Boolean);

  if (!adminChatId && adminUserIds.length === 0) {
    return false;
  }

  const userId = String(msg.from ? msg.from.id : '');
  const chatId = String(msg.chat ? msg.chat.id : '');

  if (adminChatId && (chatId === String(adminChatId) || userId === String(adminChatId))) {
    return true;
  }

  if (adminUserIds.includes(userId) || adminUserIds.includes(chatId)) {
    return true;
  }

  return false;
}

// Download Telegram photo to local uploads directory to prevent exposing Telegram Bot Token
async function saveTelegramPhotoLocally(bot, fileId) {
  try {
    const uploadsDir = path.join(__dirname, '..', '..', 'uploads');
    if (!fs.existsSync(uploadsDir)) {
      fs.mkdirSync(uploadsDir, { recursive: true });
    }

    const fileStream = bot.getFileStream(fileId);
    const fileName = `img_${Date.now()}_${Math.floor(Math.random() * 10000)}.jpg`;
    const localFilePath = path.join(uploadsDir, fileName);

    await new Promise((resolve, reject) => {
      const out = fs.createWriteStream(localFilePath);
      fileStream.pipe(out);
      out.on('finish', resolve);
      out.on('error', reject);
    });

    const baseUrl = process.env.BASE_URL || 'http://localhost:3000';
    return `${baseUrl}/uploads/${fileName}`;
  } catch (err) {
    console.error('Failed to save Telegram photo locally:', err.message);
    return 'https://via.placeholder.com/500?text=Product';
  }
}

const adminSessions = {}; // chatId -> session object

function createAdminBot(token, options = {}) {
  if (!token) {
    console.log('TELEGRAM_ADMIN_BOT_TOKEN not provided. Admin Bot disabled.');
    return null;
  }

  const bot = new TelegramBot(token, { polling: true, ...options });

  // Main Menu markup
  const mainMenuKeyboard = {
    reply_markup: {
      keyboard: [
        ['➕ إضافة منتج', '✏️ تعديل منتج'],
        ['🗑️ حذف منتج', '📦 المنتجات'],
        ['📊 المخزون']
      ],
      resize_keyboard: true
    }
  };

  const showMainMenu = (chatId, text = 'مرحباً بك في لوحة إدراة متجر عمار أسعد 👋') => {
    bot.sendMessage(chatId, text, mainMenuKeyboard);
  };

  bot.onText(/\/start/, (msg) => {
    if (!isAuthorized(msg)) {
      return bot.sendMessage(msg.chat.id, '❌ غير مصرح لك باستخدام لوحة الإدارة.');
    }
    delete adminSessions[msg.chat.id];
    showMainMenu(msg.chat.id, 'مرحباً بك في لوحة إدراة المنتجات 🛒');
  });

  // Handle Text Messages / Wizard Steps
  bot.on('message', async (msg) => {
    if (!msg.text && !msg.photo) return;
    if (msg.text && msg.text.startsWith('/')) return;

    if (!isAuthorized(msg)) {
      return bot.sendMessage(msg.chat.id, '❌ غير مصرح لك باستخدام لوحة الإدارة.');
    }

    const chatId = msg.chat.id;
    const text = msg.text ? msg.text.trim() : '';

    // Cancel command
    if (text === '❌ إلغاء' || text === 'إلغاء') {
      delete adminSessions[chatId];
      return showMainMenu(chatId, 'تم إلغاء العملية.');
    }

    // Top-Level Menu Commands
    if (text === '➕ إضافة منتج') {
      adminSessions[chatId] = { step: 'ADD_WAIT_PHOTO', data: {} };
      return bot.sendMessage(chatId, '➕ إضافة منتج جديد:\n\n1️⃣ يرجى إرسال صورة المنتج:');
    }

    if (text === '✏️ تعديل منتج') {
      delete adminSessions[chatId];
      const products = await allAsync('SELECT * FROM products ORDER BY name ASC');
      if (products.length === 0) {
        return bot.sendMessage(chatId, 'لا توجد منتجات لتعديلها.');
      }
      const inline_keyboard = products.map(p => ([
        { text: `${p.name} - ${p.price} د.ع`, callback_data: `edit_select_${p.id}` }
      ]));
      return bot.sendMessage(chatId, 'اختر المنتج الذي ترغب في تعديله:', {
        reply_markup: { inline_keyboard }
      });
    }

    if (text === '🗑️ حذف منتج') {
      delete adminSessions[chatId];
      const products = await allAsync('SELECT * FROM products ORDER BY name ASC');
      if (products.length === 0) {
        return bot.sendMessage(chatId, 'لا توجد منتجات لحذفها.');
      }
      const inline_keyboard = products.map(p => ([
        { text: `${p.name} (${p.stock} متوفر)`, callback_data: `delete_select_${p.id}` }
      ]));
      return bot.sendMessage(chatId, 'اختر المنتج المراد حذفه:', {
        reply_markup: { inline_keyboard }
      });
    }

    if (text === '📦 المنتجات') {
      delete adminSessions[chatId];
      return sendPaginatedProducts(bot, chatId, 0);
    }

    if (text === '📊 المخزون') {
      delete adminSessions[chatId];
      const products = await allAsync('SELECT * FROM products');
      const inStock = products.filter(p => p.stock > 5);
      const lowStock = products.filter(p => p.stock > 0 && p.stock <= 5);
      const outOfStock = products.filter(p => p.stock === 0);

      let msgText = `📊 **تقرير المخزون والمنتجات** 📊\n\n`;
      msgText += `✅ **المنتجات المتوفرة بكثرة (>5):** ${inStock.length}\n`;
      msgText += `⚠️ **المنتجات قليلة المخزون (1-5):** ${lowStock.length}\n`;
      msgText += `❌ **المنتجات التي نفدت (0):** ${outOfStock.length}\n\n`;

      if (lowStock.length > 0) {
        msgText += `⚠️ **قائمة المنتجات قليلة المخزون:**\n`;
        lowStock.forEach(p => {
          msgText += `- ${p.name}: remaining ${p.stock}\n`;
        });
        msgText += `\n`;
      }

      if (outOfStock.length > 0) {
        msgText += `❌ **قائمة المنتجات النافدة:**\n`;
        outOfStock.forEach(p => {
          msgText += `- ${p.name}\n`;
        });
      }

      return bot.sendMessage(chatId, msgText, { parse_mode: 'Markdown' });
    }

    // Active Session Wizard Handling
    const session = adminSessions[chatId];
    if (!session) return;

    // ADD PRODUCT WIZARD
    if (session.step === 'ADD_WAIT_PHOTO') {
      if (!msg.photo) {
        return bot.sendMessage(chatId, 'الرجاء إرسال صورة للمنتج (كصورة وليس ملف).');
      }
      const photo = msg.photo[msg.photo.length - 1];
      const fileId = photo.file_id;

      const localUrl = await saveTelegramPhotoLocally(bot, fileId);
      session.data.imageUrl = localUrl;
      session.step = 'ADD_WAIT_NAME';
      return bot.sendMessage(chatId, '2️⃣ أدخل **اسم المنتج**:');
    }

    if (session.step === 'ADD_WAIT_NAME') {
      if (!text) return bot.sendMessage(chatId, 'الرجاء إدخال اسم المنتج:');
      session.data.name = text;
      session.step = 'ADD_WAIT_PRICE';
      return bot.sendMessage(chatId, '3️⃣ أدخل **سعر المنتج بالدينار العراقي** (مثال: 3500):');
    }

    if (session.step === 'ADD_WAIT_PRICE') {
      const price = parseInt(text, 10);
      if (isNaN(price) || price <= 0) {
        return bot.sendMessage(chatId, 'الرجاء إدخال رقم صحيح للمبلغ (مثال: 3500):');
      }
      session.data.price = price;
      session.step = 'ADD_WAIT_STOCK';
      return bot.sendMessage(chatId, '4️⃣ أدخل **الكمية المتوفرة (المخزون)**:');
    }

    if (session.step === 'ADD_WAIT_STOCK') {
      const stock = parseInt(text, 10);
      if (isNaN(stock) || stock < 0) {
        return bot.sendMessage(chatId, 'الرجاء إدخال عدد صحيح للكمية:');
      }
      session.data.stock = stock;
      session.step = 'ADD_WAIT_CATEGORY';

      const categories = await allAsync('SELECT * FROM categories');
      const inline_keyboard = categories.map(c => ([
        { text: c.name, callback_data: `add_cat_${c.id}` }
      ]));
      return bot.sendMessage(chatId, '5️⃣ اختر **القسم (الفئة)**:', {
        reply_markup: { inline_keyboard }
      });
    }

    if (session.step === 'ADD_WAIT_DESCRIPTION') {
      session.data.description = text || 'بدون وصف';
      session.step = 'ADD_PREVIEW';

      // Show Preview
      const previewText = `📋 **معاينة المنتج قبل الحفظ:**\n\n` +
        `اسم المنتج: ${session.data.name}\n` +
        `السعر: ${session.data.price} د.ع\n` +
        `الكمية: ${session.data.stock}\n` +
        `القسم: ${session.data.categoryName || session.data.categoryId}\n` +
        `الوصف: ${session.data.description}`;

      const inline_keyboard = [
        [
          { text: '✅ حفظ', callback_data: 'add_confirm_save' },
          { text: '❌ إلغاء', callback_data: 'add_confirm_cancel' }
        ]
      ];

      if (session.data.imageUrl && session.data.imageUrl.startsWith('http')) {
        return bot.sendMessage(chatId, `${previewText}\n\n📷 رابط الصورة: ${session.data.imageUrl}`, {
          parse_mode: 'Markdown',
          reply_markup: { inline_keyboard }
        });
      } else {
        return bot.sendMessage(chatId, previewText, {
          parse_mode: 'Markdown',
          reply_markup: { inline_keyboard }
        });
      }
    }

    // EDIT PRODUCT WIZARD INPUTS
    if (session.step === 'EDIT_WAIT_VALUE') {
      const field = session.field;
      const productId = session.productId;

      let updateData = {};
      if (field === 'name') updateData.name = text;
      else if (field === 'price') {
        const val = parseInt(text, 10);
        if (isNaN(val) || val <= 0) return bot.sendMessage(chatId, 'الرجاء إدخال رقم سعر صحيح:');
        updateData.price = val;
      } else if (field === 'stock') {
        const val = parseInt(text, 10);
        if (isNaN(val) || val < 0) return bot.sendMessage(chatId, 'الرجاء إدخال عدد كمية صحيح:');
        updateData.stock = val;
      } else if (field === 'description') {
        updateData.description = text;
      } else if (field === 'imageUrl') {
        if (msg.photo) {
          const photo = msg.photo[msg.photo.length - 1];
          updateData.imageUrl = await saveTelegramPhotoLocally(bot, photo.file_id);
        } else {
          updateData.imageUrl = text;
        }
      }

      const keys = Object.keys(updateData);
      if (keys.length > 0) {
        const key = keys[0];
        const value = updateData[key];
        const now = new Date().toISOString();
        await runAsync(`UPDATE products SET ${key} = ?, updatedAt = ? WHERE id = ?`, [value, now, productId]);
        delete adminSessions[chatId];
        showMainMenu(chatId, '✅ تم تحديث المنتج بنجاح! يظهر التحديث الآن في تطبيق Android.');
      }
    }
  });

  // Callback Query Handling (Inline Keyboards)
  bot.on('callback_query', async (query) => {
    const msg = query.message;
    const chatId = msg.chat.id;
    const data = query.data;

    if (!isAuthorized(query)) {
      return bot.answerCallbackQuery(query.id, { text: '❌ غير مصرح لك', show_alert: true });
    }

    const session = adminSessions[chatId] || {};

    // ADD PRODUCT CATEGORY SELECTION
    if (data.startsWith('add_cat_')) {
      const catId = data.replace('add_cat_', '');
      const cat = await getAsync('SELECT * FROM categories WHERE id = ?', [catId]);
      session.data.categoryId = catId;
      session.data.categoryName = cat ? cat.name : catId;
      session.step = 'ADD_WAIT_DESCRIPTION';
      bot.answerCallbackQuery(query.id);
      return bot.sendMessage(chatId, '6️⃣ أدخل **وصف المنتج**:');
    }

    // ADD PRODUCT CONFIRM SAVE
    if (data === 'add_confirm_save') {
      bot.answerCallbackQuery(query.id, { text: 'جاري الحفظ...' });
      if (!session.data || !session.data.name) {
        delete adminSessions[chatId];
        return bot.sendMessage(chatId, 'حدث خطأ أو تم إلغاء الجلسة.');
      }

      const pData = session.data;
      const id = `prod_${Date.now()}`;
      const now = new Date().toISOString();

      await runAsync(
        `INSERT INTO products (id, categoryId, name, description, price, stock, imageUrl, isAvailable, isNewArrival, isFeatured, createdAt, updatedAt)
         VALUES (?, ?, ?, ?, ?, ?, ?, 1, 1, 0, ?, ?)`,
        [
          id,
          pData.categoryId || 'cat_notebooks',
          pData.name,
          pData.description || '',
          pData.price || 0,
          pData.stock || 0,
          pData.imageUrl || 'https://via.placeholder.com/500?text=Product',
          now,
          now
        ]
      );

      delete adminSessions[chatId];
      return showMainMenu(chatId, '✅ تم حفظ المنتج بنجاح وتخزينه في قاعدة البيانات SQLite!\nسيظهر المنتج تلقائياً داخل تطبيق Android.');
    }

    // ADD PRODUCT CONFIRM CANCEL
    if (data === 'add_confirm_cancel') {
      bot.answerCallbackQuery(query.id);
      delete adminSessions[chatId];
      return showMainMenu(chatId, 'تم إلغاء إضافة المنتج.');
    }

    // EDIT PRODUCT SELECTION
    if (data.startsWith('edit_select_')) {
      const prodId = data.replace('edit_select_', '');
      const prod = await getAsync('SELECT * FROM products WHERE id = ?', [prodId]);
      if (!prod) {
        bot.answerCallbackQuery(query.id, { text: 'المنتج غير موجود' });
        return;
      }

      adminSessions[chatId] = { step: 'EDIT_WAIT_FIELD', productId: prodId };
      bot.answerCallbackQuery(query.id);

      const inline_keyboard = [
        [{ text: 'اسم المنتج', callback_data: `edit_field_name_${prodId}` }, { text: 'السعر', callback_data: `edit_field_price_${prodId}` }],
        [{ text: 'الكمية', callback_data: `edit_field_stock_${prodId}` }, { text: 'القسم', callback_data: `edit_field_category_${prodId}` }],
        [{ text: 'الوصف', callback_data: `edit_field_description_${prodId}` }, { text: 'الصورة', callback_data: `edit_field_imageUrl_${prodId}` }]
      ];

      return bot.sendMessage(chatId, `تعديل المنتج: **${prod.name}**\nاختر الحقل المراد تعديله:`, {
        parse_mode: 'Markdown',
        reply_markup: { inline_keyboard }
      });
    }

    // EDIT FIELD SELECTION
    if (data.startsWith('edit_field_')) {
      const parts = data.replace('edit_field_', '').split('_');
      const field = parts[0];
      const prodId = parts.slice(1).join('_');

      bot.answerCallbackQuery(query.id);

      if (field === 'category') {
        const categories = await allAsync('SELECT * FROM categories');
        const inline_keyboard = categories.map(c => ([
          { text: c.name, callback_data: `edit_setcat_${prodId}_${c.id}` }
        ]));
        return bot.sendMessage(chatId, 'اختر القسم الجديد:', { reply_markup: { inline_keyboard } });
      }

      adminSessions[chatId] = { step: 'EDIT_WAIT_VALUE', productId: prodId, field: field };
      const fieldNames = { name: 'الاسم', price: 'السعر', stock: 'الكمية', description: 'الوصف', imageUrl: 'رابط أو صورة المنتج' };
      return bot.sendMessage(chatId, `أدخل **${fieldNames[field]}** الجديد:`);
    }

    // EDIT SET CATEGORY
    if (data.startsWith('edit_setcat_')) {
      const [prodId, catId] = data.replace('edit_setcat_', '').split('_');
      const now = new Date().toISOString();
      await runAsync('UPDATE products SET categoryId = ?, updatedAt = ? WHERE id = ?', [catId, now, prodId]);
      bot.answerCallbackQuery(query.id, { text: 'تم التعديل بنجاح' });
      delete adminSessions[chatId];
      return showMainMenu(chatId, '✅ تم تحديث قسم المنتج بنجاح!');
    }

    // DELETE SELECTION & CONFIRMATION
    if (data.startsWith('delete_select_')) {
      const prodId = data.replace('delete_select_', '');
      const prod = await getAsync('SELECT * FROM products WHERE id = ?', [prodId]);
      if (!prod) return bot.answerCallbackQuery(query.id, { text: 'المنتج غير موجود' });

      bot.answerCallbackQuery(query.id);
      const inline_keyboard = [
        [
          { text: '✅ نعم', callback_data: `delete_confirm_${prodId}` },
          { text: '❌ إلغاء', callback_data: 'delete_cancel' }
        ]
      ];
      return bot.sendMessage(chatId, `هل أنت متأكد من حذف هذا المنتج؟\n\n📌 **${prod.name}**`, {
        parse_mode: 'Markdown',
        reply_markup: { inline_keyboard }
      });
    }

    if (data.startsWith('delete_confirm_')) {
      const prodId = data.replace('delete_confirm_', '');
      await runAsync('DELETE FROM products WHERE id = ?', [prodId]);
      bot.answerCallbackQuery(query.id, { text: 'تم الحذف' });
      delete adminSessions[chatId];
      return showMainMenu(chatId, '✅ تم حذف المنتج بنجاح من قاعدة البيانات.');
    }

    if (data === 'delete_cancel') {
      bot.answerCallbackQuery(query.id);
      delete adminSessions[chatId];
      return showMainMenu(chatId, 'تم إلغاء عملية الحذف.');
    }

    // PAGINATION FOR PRODUCTS LIST
    if (data.startsWith('prod_page_')) {
      const page = parseInt(data.replace('prod_page_', ''), 10);
      bot.answerCallbackQuery(query.id);
      return sendPaginatedProducts(bot, chatId, page, msg.message_id);
    }
  });

  return bot;
}

// Helper: Send Paginated Products
async function sendPaginatedProducts(bot, chatId, page = 0, messageId = null) {
  const pageSize = 4;
  const products = await allAsync('SELECT p.*, c.name as categoryName FROM products p LEFT JOIN categories c ON p.categoryId = c.id ORDER BY p.createdAt DESC');

  if (products.length === 0) {
    return bot.sendMessage(chatId, 'لا توجد منتجات مسجلة حالياً.');
  }

  const totalPages = Math.ceil(products.length / pageSize);
  const currentPage = Math.max(0, Math.min(page, totalPages - 1));
  const pageProducts = products.slice(currentPage * pageSize, (currentPage + 1) * pageSize);

  let text = `📦 **قائمة المنتجات (صفحة ${currentPage + 1} من ${totalPages}):**\n\n`;

  pageProducts.forEach((p, idx) => {
    const stockStatus = p.stock === 0 ? '❌ نافد' : (p.stock <= 5 ? `⚠️ قليل (${p.stock})` : `✅ ${p.stock}`);
    text += `${currentPage * pageSize + idx + 1}. **${p.name}**\n`;
    text += `   🏷️ السعر: ${p.price} د.ع | المخزون: ${stockStatus} | القسم: ${p.categoryName || p.categoryId}\n\n`;
  });

  const buttons = [];
  if (currentPage > 0) {
    buttons.push({ text: '◀️ السابقة', callback_data: `prod_page_${currentPage - 1}` });
  }
  if (currentPage < totalPages - 1) {
    buttons.push({ text: 'التالية ▶️', callback_data: `prod_page_${currentPage + 1}` });
  }

  const inline_keyboard = buttons.length > 0 ? [buttons] : [];

  if (messageId) {
    return bot.editMessageText(text, {
      chat_id: chatId,
      message_id: messageId,
      parse_mode: 'Markdown',
      reply_markup: { inline_keyboard }
    });
  } else {
    return bot.sendMessage(chatId, text, {
      parse_mode: 'Markdown',
      reply_markup: { inline_keyboard }
    });
  }
}

module.exports = { createAdminBot, isAuthorized, saveTelegramPhotoLocally };
