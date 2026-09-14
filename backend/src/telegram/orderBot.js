const TelegramBot = require('node-telegram-bot-api');
const { getAsync, allAsync, runAsync } = require('../db');
const { isAuthorized } = require('./adminBot');

function createOrderBot(token, options = {}) {
  if (!token) {
    console.log('TELEGRAM_ORDER_BOT_TOKEN not provided. Order Bot disabled.');
    return null;
  }

  const bot = new TelegramBot(token, { polling: true, ...options });

  // Handle /start <token> Deep Linking for Customer Registration
  bot.onText(/\/start(?:\s+(.+))?/, async (msg, match) => {
    const chatId = String(msg.chat.id);
    const startParam = match && match[1] ? match[1].trim() : null;

    if (!startParam) {
      return bot.sendMessage(
        chatId,
        'مرحباً بك في بوت طلبات متجر عمار أسعد 🛒\n\nعند إكمال طلبك من تطبيق Android، اضغط على زر "ربط Telegram" لمتابعة حالة طلبك مباشرة من هنا!'
      );
    }

    // startParam can be an Order ID (e.g. ORD-12345678)
    const order = await getAsync('SELECT * FROM orders WHERE id = ?', [startParam]);
    if (order) {
      const now = new Date().toISOString();
      await runAsync('UPDATE orders SET telegramChatId = ?, updatedAt = ? WHERE id = ?', [chatId, now, order.id]);
      return bot.sendMessage(
        chatId,
        `✅ **تم ربط حسابك بطلبك نجاح!**\n\nرقم الطلب: #${order.id}\nحالة الطلب الحالية: ${getStatusArabic(order.status)}\n\nسيصلك إشعار تلقائي بأي تغيير في حالة الطلب! 🔔`,
        { parse_mode: 'Markdown' }
      );
    } else {
      return bot.sendMessage(chatId, `⚠️ لم نتمكن من العثور على طلب برمز: ${startParam}`);
    }
  });

  // Inline Button Callback Handler (Accept, Reject, Change Status)
  bot.on('callback_query', async (query) => {
    const msg = query.message;
    const adminChatId = msg.chat.id;
    const data = query.data;

    // Security Check: Only authorized admins can manage orders via Order Bot inline buttons
    if (!isAuthorized(query)) {
      return bot.answerCallbackQuery(query.id, { text: '❌ غير مصرح لك باستخدام هذه الوظيفة.', show_alert: true });
    }

    // ACCEPT ORDER
    if (data.startsWith('order_accept_')) {
      const orderId = data.replace('order_accept_', '');
      const result = await processOrderAcceptance(orderId, bot);
      bot.answerCallbackQuery(query.id, { text: result.message, show_alert: true });
      if (result.success) {
        updateAdminOrderMessage(bot, adminChatId, msg.message_id, orderId);
      }
      return;
    }

    // REJECT ORDER
    if (data.startsWith('order_reject_')) {
      const orderId = data.replace('order_reject_', '');
      const result = await processOrderRejection(orderId, bot);
      bot.answerCallbackQuery(query.id, { text: result.message, show_alert: true });
      if (result.success) {
        updateAdminOrderMessage(bot, adminChatId, msg.message_id, orderId);
      }
      return;
    }

    // STATUS CHANGE (PREPARING, READY, DELIVERED)
    if (data.startsWith('order_status_')) {
      const parts = data.replace('order_status_', '').split('_');
      const newStatus = parts[0];
      const orderId = parts.slice(1).join('_');

      const result = await processOrderStatusChange(orderId, newStatus, bot);
      bot.answerCallbackQuery(query.id, { text: result.message, show_alert: true });
      if (result.success) {
        updateAdminOrderMessage(bot, adminChatId, msg.message_id, orderId);
      }
      return;
    }
  });

  return bot;
}

// Function to notify admin on new order creation
async function notifyAdminNewOrder(orderBot, order) {
  const adminChatId = process.env.ADMIN_TELEGRAM_CHAT_ID;
  if (!orderBot || !adminChatId) return;

  const items = await allAsync('SELECT * FROM order_items WHERE orderId = ?', [order.id]);

  let text = `🛒 **طلب جديد**\n\n`;
  text += `رقم الطلب: #${order.id}\n\n`;
  text += `👤 **الاسم:**\n${order.customerName}\n\n`;
  text += `📞 **الهاتف:**\n${order.phone}\n\n`;
  text += `🏠 **العنوان:**\n${order.address}\n\n`;
  if (order.nearestLandmark) {
    text += `📍 **أقرب نقطة دالة:**\n${order.nearestLandmark}\n\n`;
  }
  if (order.notes) {
    text += `📝 **ملاحظات:**\n${order.notes}\n\n`;
  }

  text += `🛍️ **المنتجات:**\n`;
  items.forEach(item => {
    text += `- ${item.productName} × ${item.quantity} — ${item.subtotal} د.ع\n`;
  });

  text += `\n💰 **المجموع:**\n${order.totalPrice} د.ع\n\n`;
  text += `📅 **وقت الطلب:**\n${new Date(order.createdAt).toLocaleString('ar-IQ')}\n`;

  const inline_keyboard = [
    [
      { text: '✅ قبول الطلب', callback_data: `order_accept_${order.id}` },
      { text: '❌ رفض الطلب', callback_data: `order_reject_${order.id}` }
    ]
  ];

  try {
    await orderBot.sendMessage(adminChatId, text, {
      parse_mode: 'Markdown',
      reply_markup: { inline_keyboard }
    });
  } catch (err) {
    console.error('Failed to send new order notification to Telegram admin:', err.message);
  }
}

// Process Order Acceptance Safely
async function processOrderAcceptance(orderId, orderBot) {
  const order = await getAsync('SELECT * FROM orders WHERE id = ?', [orderId]);
  if (!order) return { success: false, message: 'الطلب غير موجود' };

  if (order.status === 'ACCEPTED') {
    return { success: false, message: '⚠️ تم قبول هذا الطلب سابقاً!' };
  }
  if (order.status !== 'PENDING') {
    return { success: false, message: `⚠️ لا يمكن قبول طلب بحالة: ${getStatusArabic(order.status)}` };
  }

  const items = await allAsync('SELECT * FROM order_items WHERE orderId = ?', [orderId]);

  // Stock re-check
  for (const item of items) {
    const prod = await getAsync('SELECT * FROM products WHERE id = ?', [item.productId]);
    if (!prod) {
      return { success: false, message: `❌ المنتج ${item.productName} غير موجود في قاعدة البيانات` };
    }
    if (item.quantity > prod.stock) {
      return {
        success: false,
        message: `❌ الكمية المطلوبة لـ "${prod.name}" (${item.quantity}) تتجاوز المخزون الحالي (${prod.stock})`
      };
    }
  }

  // Deduct stock atomically and update order status
  const now = new Date().toISOString();
  for (const item of items) {
    await runAsync('UPDATE products SET stock = MAX(0, stock - ?), updatedAt = ? WHERE id = ?', [
      item.quantity,
      now,
      item.productId
    ]);
  }

  await runAsync('UPDATE orders SET status = "ACCEPTED", updatedAt = ? WHERE id = ?', [now, orderId]);

  // Notify customer if customer Telegram Chat ID exists
  if (order.telegramChatId && orderBot) {
    try {
      await orderBot.sendMessage(
        order.telegramChatId,
        `✅ **تحديث حالة الطلب**\n\nتم **قبول طلبك** (#${order.id}) بنجاح! جاري تحضير وتجهيز المنتجات للتوجه إليك. 🎉`,
        { parse_mode: 'Markdown' }
      );
    } catch (e) {
      console.log('Customer telegram notification error:', e.message);
    }
  }

  return { success: true, message: '✅ تم قبول الطلب وخصم الكمية من المخزون بنجاح!' };
}

// Process Order Rejection
async function processOrderRejection(orderId, orderBot) {
  const order = await getAsync('SELECT * FROM orders WHERE id = ?', [orderId]);
  if (!order) return { success: false, message: 'الطلب غير موجود' };

  if (order.status === 'REJECTED') {
    return { success: false, message: '⚠️ هذا الطلب مرفوض بالفعل!' };
  }

  const now = new Date().toISOString();
  await runAsync('UPDATE orders SET status = "REJECTED", updatedAt = ? WHERE id = ?', [now, orderId]);

  if (order.telegramChatId && orderBot) {
    try {
      await orderBot.sendMessage(
        order.telegramChatId,
        `❌ **تحديث حالة الطلب**\n\nللأسف، تعذر قبول طلبك (#${order.id}). نعتذر منك!`,
        { parse_mode: 'Markdown' }
      );
    } catch (e) {
      console.log('Customer telegram notification error:', e.message);
    }
  }

  return { success: true, message: '❌ تم رفض الطلب ولم يتم خصم أي مخزون.' };
}

// Process Status Change (PREPARING, READY, DELIVERED)
async function processOrderStatusChange(orderId, newStatus, orderBot) {
  const order = await getAsync('SELECT * FROM orders WHERE id = ?', [orderId]);
  if (!order) return { success: false, message: 'الطلب غير موجود' };

  const now = new Date().toISOString();
  await runAsync('UPDATE orders SET status = ?, updatedAt = ? WHERE id = ?', [newStatus, now, orderId]);

  if (order.telegramChatId && orderBot) {
    let msgText = '';
    if (newStatus === 'PREPARING') msgText = `⚙️ **تحديث حالة الطلب** (#${order.id}): طلبك الآن **قيد التجهيز**!`;
    else if (newStatus === 'READY') msgText = `📦 **تحديث حالة الطلب** (#${order.id}): طلبك **جاهز للتوصيل**!`;
    else if (newStatus === 'DELIVERED') msgText = `🚚 **تحديث حالة الطلب** (#${order.id}): **تم تسليم الطلب** بنجاح. شكراً لتسوقكم من عمار أسعد! ❤️`;

    if (msgText) {
      try {
        await orderBot.sendMessage(order.telegramChatId, msgText, { parse_mode: 'Markdown' });
      } catch (e) {
        console.log('Customer telegram notification error:', e.message);
      }
    }
  }

  return { success: true, message: `تم تغيير الحالة إلى ${getStatusArabic(newStatus)}` };
}

// Helper: Edit Admin Message displaying current order status and controls
async function updateAdminOrderMessage(orderBot, chatId, messageId, orderId) {
  const order = await getAsync('SELECT * FROM orders WHERE id = ?', [orderId]);
  if (!order || !orderBot) return;

  const items = await allAsync('SELECT * FROM order_items WHERE orderId = ?', [orderId]);

  let text = `🛒 **تفاصيل الطلب** (#${order.id})\n\n`;
  text += `📌 **الحالة الحالية:** ${getStatusArabic(order.status)}\n\n`;
  text += `👤 **الاسم:** ${order.customerName}\n`;
  text += `📞 **الهاتف:** ${order.phone}\n`;
  text += `🏠 **العنوان:** ${order.address}\n\n`;

  text += `🛍️ **المنتجات:**\n`;
  items.forEach(item => {
    text += `- ${item.productName} × ${item.quantity} — ${item.subtotal} د.ع\n`;
  });

  text += `\n💰 **المجموع:** ${order.totalPrice} د.ع\n`;

  let inline_keyboard = [];
  if (order.status === 'PENDING') {
    inline_keyboard = [
      [
        { text: '✅ قبول الطلب', callback_data: `order_accept_${order.id}` },
        { text: '❌ رفض الطلب', callback_data: `order_reject_${order.id}` }
      ]
    ];
  } else if (order.status === 'ACCEPTED' || order.status === 'PREPARING' || order.status === 'READY') {
    inline_keyboard = [
      [
        { text: '⚙️ قيد التجهيز', callback_data: `order_status_PREPARING_${order.id}` },
        { text: '📦 جاهز', callback_data: `order_status_READY_${order.id}` },
        { text: '🚚 تم التسليم', callback_data: `order_status_DELIVERED_${order.id}` }
      ]
    ];
  }

  try {
    await orderBot.editMessageText(text, {
      chat_id: chatId,
      message_id: messageId,
      parse_mode: 'Markdown',
      reply_markup: { inline_keyboard }
    });
  } catch (err) {
    // Ignore edit error if unchanged
  }
}

function getStatusArabic(status) {
  switch (status) {
    case 'PENDING': return '⏳ قيد الانتظار';
    case 'ACCEPTED': return '✅ مقبول';
    case 'REJECTED': return '❌ مرفوض';
    case 'PREPARING': return '⚙️ قيد التجهيز';
    case 'READY': return '📦 جاهز للتوصيل';
    case 'DELIVERED': return '🚚 تم التسليم';
    default: return status;
  }
}

module.exports = {
  createOrderBot,
  notifyAdminNewOrder,
  processOrderAcceptance,
  processOrderRejection,
  processOrderStatusChange,
  getStatusArabic
};
