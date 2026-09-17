# متجر عمار أسعد للقرطاسية والمكتبية (Amaar Asaad Store) - المرحلة الثانية: Backend & Database

تطبيق متجر عمار أسعد للقرطاسية والمكتبية - تم بناء وتجهيز المرحلة الثانية (Backend + Database) بنجاح لربط تطبيق Android Native بنظام إدارة خادمي وقاعدة بيانات حقيقية.

---

## 🛠️ تقنيات المباشرة والتطبيق (Tech Stack)

* **تطبيق Android**: Kotlin, Jetpack Compose, Material 3, Clean Architecture, Repository Pattern, Retrofit 2, Gson.
* **الـ Backend**: Node.js + Express.js RESTful API.
* **قاعدة البيانات**: SQLite (مع إمكانية الربط المباشر بـ PostgreSQL / Supabase).
* **الاختبارات والتأكد**: Jest + Supertest للـ Backend، و JUnit لكود Android.

---

## 🚀 تشغيل خادم الـ Backend والتحقق

### 1. تشغيل الخادم محلياً
```bash
cd backend
npm install
npm start
```
سيعمل الخادم على العنوان: `http://localhost:3000` (أو `http://10.0.2.2:3000` للـ Android Emulator).

### 2. تشغيل اختبارات الخادم
```bash
cd backend
npm test
```

### 3. بناء وتجربة تطبيق Android
```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

---

## 📖 توثيق الـ API Endpoints

### 1. الأقسام (Categories)
* **`GET /api/categories`**
  * **الوصف**: جلب جميع الأقسام المتوفرة في المتجر.
  * **الاستجابة**: قائمة JSON بالأقسام.

### 2. المنتجات (Products)
* **`GET /api/products`**
  * **الوصف**: جلب المنتجات المتاحة.
  * **Query Parameters**:
    * `categoryId` (اختياري): تصفية حسب القسم.
    * `search` (اختياري): البحث في الاسم والوصف.
    * `featured` (`true`/`false`): تصفية المنتجات المميزة.
    * `newArrivals` (`true`/`false`): تصفية المنتجات الجديدة.
* **`GET /api/products/:id`**
  * **الوصف**: جلب تفاصيل منتج معين بواسطة الـ ID.

### 3. الطلبات (Orders)
* **`GET /api/orders`**
  * **الوصف**: جلب قائمة جميع الطلبات المسجلة.
* **`POST /api/orders`**
  * **الوصف**: إنشاؤ طلب جديد وتوثيقه في قاعدة البيانات.
  * **جسم الطلب (Request Body)**:
    ```json
    {
      "customerName": "أحمد علي",
      "phone": "07701234567",
      "address": "بغداد - الكرادة",
      "nearestLandmark": "قرب ساحة الواثق",
      "notes": "يرجى التوصيل بعد العصر",
      "items": [
        { "productId": "prod_1", "quantity": 2 }
      ]
    }
    ```
  * **قواعد السيرفر (Backend Logic & Security)**:
    * التحقق الإجباري من وجود: الاسم، رقم الهاتف، العنوان، والمنتجات.
    * التأكد من وجود المنتجات في DB وتوفر كميةStock كافية.
    * حظر طلب كمية أكبر من المخزون المتوفر.
    * حساب الأسعار والمجموع الكلي من قاعدة البيانات حصراً وإهمال أي سعر مرسل من التطبيق.
    * تعيين حالة الطلب الافتراضية إلى `PENDING`.
    * عدم خصم المخزون عند مجرد إنشاء الطلب.
* **`GET /api/orders/:id`**
  * **الوصف**: جلب تفاصيل طلب محدد برقم الطلب الفريد `ORD-XXXXXX`.
* **`PATCH /api/orders/:id/status`**
  * **الوصف**: تحديث حالة الطلب (`PENDING`, `ACCEPTED`, `REJECTED`, `PREPARING`, `READY`, `DELIVERED`). عند تغيير الحالة إلى `ACCEPTED` يتم خصم الكميات من مخزون السيرفر.

---

## 🔑 متغيرات البيئة (Environment Variables)

يتم ضبطها في ملف `backend/.env` أو في لوحة التحكم بالاستضافة السحابية المجانية:
* `PORT`: منفذ تشغيل السيرفر (الافتراضي `3000`).
* `DATABASE_URL`: مسار قاعدة البيانات SQLite أو السلسلة لـ PostgreSQL/Supabase.
* `NODE_ENV`: بيئة التشغيل (`development` أو `production`).
* `TELEGRAM_BOT_TOKEN`: (مستقبلي للمرحلة الثالثة).
* `TELEGRAM_CHAT_ID`: (مستقبلي للمرحلة الثالثة).
