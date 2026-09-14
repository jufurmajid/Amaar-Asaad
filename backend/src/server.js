require('dotenv').config();
const app = require('./app');
const { initDb } = require('./db');

const PORT = process.env.PORT || 3000;

const { createAdminBot } = require('./telegram/adminBot');
const { createOrderBot } = require('./telegram/orderBot');

initDb()
  .then(() => {
    const adminBotToken = process.env.TELEGRAM_ADMIN_BOT_TOKEN;
    const orderBotToken = process.env.TELEGRAM_ORDER_BOT_TOKEN;

    const adminBot = createAdminBot(adminBotToken);
    const orderBot = createOrderBot(orderBotToken);

    app.set('adminBot', adminBot);
    app.set('orderBot', orderBot);

    app.listen(PORT, () => {
      console.log(`Backend server running on http://localhost:${PORT}`);
    });
  })
  .catch((err) => {
    console.error('Failed to initialize database:', err);
    process.exit(1);
  });
