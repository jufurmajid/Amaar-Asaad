# متجر عمار أسعد للقرطاسية والمكتبية (Amaar Asaad Store) - المرحلة الثالثة: Telegram Integration

تطبيق متجر عمار أسعد للقرطاسية والمكتبية - تم تنفيذ المرحلة الثالثة (Telegram Integration) بالكامل وبنجاح.

---

## 🛠️ تقنيات وتطوير المرحلة الثالثة (Tech Stack & Integration)

* **تطبيق Android**: Kotlin, Jetpack Compose, Material 3, Clean Architecture, Repository Pattern, Telegram Deep-Linking.
* **الـ Backend**: Node.js + Express.js RESTful API + SQLite Database.
* **بوتات Telegram**: Node Telegram Bot API (لوحة الإدارة للتاجر + بوت استلام وتحديث الطلبات).

---

## 🤖 البوتات وطريقة إعدادها من الهاتف (Telegram Bots Setup)

### 1. إنشاء Telegram Bot من BotFather
1. افتح تطبيق Telegram وابحث عن `@BotFather`.
2. أرسل الأمر `/newbot` وأدخل اسم البوت واسم المستخدم (مثل `AmaarAsaadAdminBot`).
3. احفظ الـ **Bot Token** الذي يظهره لك BotFather.
4. كرر العملية لإنشاء بوت الطلبات (مثل `AmaarAsaadOrderBot`).

### 2. معرفة Admin Chat ID
1. ابحث في Telegram عن `@userinfobot` أو `@rawdata_bot`.
2. اضغط `Start` وسيعرض لك `id` الخاد بالـ Admin (مثال: `123456789`).

### 3. إعداد متغيرات البيئة (Environment Variables)
أنشئ ملف `.env` في مجلد `backend` (أو المجلد الرئيسي) وضع المتغيرات التالية دون مشاركة الـ Tokens على GitHub:

```env
TELEGRAM_ADMIN_BOT_TOKEN=123456789:ABCdefGHIjklMNOpqrsTUVwxyz_ADMIN
TELEGRAM_ORDER_BOT_TOKEN=987654321:ZYXwvuTSRqpoNMLkjiHGFedCBA_ORDER
ADMIN_TELEGRAM_CHAT_ID=123456789
ADMIN_TELEGRAM_USER_IDS=123456789
TELEGRAM_ORDER_BOT_USERNAME=AmaarAsaadOrderBot
PORT=3000
```

---

## 🚀 كيفية تشغيل البوتات والـ Backend

```bash
cd backend
npm install
npm start
```
سيعمل السيرفر وبوتات التلغرام تلقائياً بالتوازي مع الخادم!

### تشغيل الاختبارات
```bash
cd backend
npm test
```
ولإجراء اختبارات تطبيق Android:
```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

---

## 📱 كيفية استخدام وظائف البوتات والربط

### 🛍️ بوت الإدارة (Admin Bot)
يحظر البوت أي مستخدم غير مصرح له بالرسالة: `"❌ غير مصرح لك باستخدام لوحة الإدارة."`

الأوامر المتوفرة لصاحب المتجر:
* **➕ إضافة منتج**: معالج تفاعلي يطلب (الصورة، الاسم، السعر، المخزون، القسم، الوصف) ويقدم معاينة قبل الحفظ.
* **✏️ تعديل منتج**: تعديل أي حقل للمنتج وتحديثه فوراً.
* **🗑️ حذف منتج**: طلب تأكيد (نعم/إلغاء) وحذف المنتج من قاعدة البيانات.
* **📦 المنتجات**: عرض قائمة المنتجات مع تصفح الصفحات (Pagination).
* **📊 المخزون**: تقرير بالمنتجات المتوفرة، قليلة المخزون، والنافدة.

### 🛒 بوت الطلبات وتتبع الزبون (Order Bot & Customer Linking)
1. عند إنشاء طلب جديد في تطبيق Android، يصل إشعار فوري لصاحب المتجر مع زرين inline:
   * **✅ قبول الطلب**: يفحص المخزون، يخصم الكميات بأمان، يحول الحالة إلى ACCEPTED، ويرسل إشعاراً للزبون إذا كان مربوطاً.
   * **❌ رفض الطلب**: يحول الحالة إلى REJECTED بدون خصم أي مخزون ويشعر الزبون.
   * خيارات تحديث الحالة: ⚙️ قيد التجهيز | 📦 جاهز | 🚚 تم التسليم.
2. **ربط الزبون مع Telegram**:
   في صفحة نجاح الطلب بالتطبيق يظهر زر: **"🔔 ربط Telegram لاستلام حالة الطلب"**. عند الضغط عليه يفتح البوت برابط Deep Link (`start=ORD-XXXXXX`). وعند ضغط Start بالتلغرام يتم ربط `chat_id` الزبون بالطلب لاستلام التحديثات مباشرة!
