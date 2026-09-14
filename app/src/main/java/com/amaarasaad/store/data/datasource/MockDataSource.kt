package com.amaarasaad.store.data.datasource

import com.amaarasaad.store.data.model.Category
import com.amaarasaad.store.data.model.Product

/**
 * Isolated Sample Data Source.
 * This decouples sample mock data from the UI and ViewModel layers.
 * In the future, this class can be swapped out or wrapped with Retrofit/Ktor API client
 * or Room Local Database to communicate with the Backend server and Telegram Bots.
 */
object MockDataSource {

    val categories = listOf(
        Category(id = "cat_notebooks", nameAr = "دفاتر", description = "دفاتر مدرسية وجامعية وملاحظات بأحجام مختلفة"),
        Category(id = "cat_pens", nameAr = "أقلام", description = "أقلام جافة، حبر، رصاص، وتأشير من أرقى الماركات"),
        Category(id = "cat_stationery", nameAr = "قرطاسية", description = "مساطر، ممحاة، مبراة، ولاصق، ومعدات هندسية"),
        Category(id = "cat_school", nameAr = "مستلزمات مدرسية", description = "حقائب، مقالم، وأطقم أدوات متكاملة للطلاب"),
        Category(id = "cat_printing", nameAr = "طباعة", description = "أوراق A4، أحبار طباعة، وأغلفة تجليد عالية الجودة"),
        Category(id = "cat_office", nameAr = "مستلزمات مكتبية", description = "ملفات، خرامات، كابسات، ومنظمات مكاتب حديثة")
    )

    val products = listOf(
        Product(
            id = "prod_1",
            categoryId = "cat_notebooks",
            nameAr = "دفتر سلك جامعي 100 ورقة مقسم",
            descriptionAr = "دفتر جامعي فاخر غلاف مقوى مقسم إلى 4 أجزاء أوراق عالية الجودة لا تنفذ الحبر.",
            priceIqd = 3500,
            stockQuantity = 45,
            imageUrl = "https://picsum.photos/seed/notebook1/400/400",
            isNewArrival = true,
            isFeatured = true
        ),
        Product(
            id = "prod_2",
            categoryId = "cat_notebooks",
            nameAr = "دفتر رسم قياس A4 ورق سميك",
            descriptionAr = "دفتر رسم 50 ورقة كرافت سميكة مناسبة للألوان المائية والخشبية والفحم.",
            priceIqd = 4500,
            stockQuantity = 20,
            imageUrl = "https://picsum.photos/seed/drawingpad/400/400",
            isNewArrival = true,
            isFeatured = false
        ),
        Product(
            id = "prod_3",
            categoryId = "cat_pens",
            nameAr = "طقم أقلام جاف 10 ألوان فاخرة",
            descriptionAr = "مجموعة أقلام ملونة بسلاسة فائقة وسريعة الجفاف، مثالية للتدوين والرسم.",
            priceIqd = 2500,
            stockQuantity = 60,
            imageUrl = "https://picsum.photos/seed/penset/400/400",
            isNewArrival = false,
            isFeatured = true
        ),
        Product(
            id = "prod_4",
            categoryId = "cat_pens",
            nameAr = "قلم حبر جاف روترينج 0.5 ملم",
            descriptionAr = "قلم حبر ألماني دقيق بكتابة انسيابية وتصميم مريح لليد أثناء الكتابة الطويلة.",
            priceIqd = 1500,
            stockQuantity = 100,
            imageUrl = "https://picsum.photos/seed/rotringpen/400/400",
            isNewArrival = true,
            isFeatured = true
        ),
        Product(
            id = "prod_5",
            categoryId = "cat_stationery",
            nameAr = "طقم هندسي متكامل للمدرسة",
            descriptionAr = "يحتوي على مسطرة، مثلثين، منقلة، وفرجار معدني في علبة صفيح واقية.",
            priceIqd = 3000,
            stockQuantity = 35,
            imageUrl = "https://picsum.photos/seed/geometries/400/400",
            isNewArrival = false,
            isFeatured = true
        ),
        Product(
            id = "prod_6",
            categoryId = "cat_school",
            nameAr = "مقلمة قماشية متعددة الطبقات",
            descriptionAr = "مقلمة تتسع لأكثر من 30 قلم مع سحّاب متين وتقسيمات داخلية عملية.",
            priceIqd = 5000,
            stockQuantity = 15,
            imageUrl = "https://picsum.photos/seed/pencilcase/400/400",
            isNewArrival = true,
            isFeatured = false
        ),
        Product(
            id = "prod_7",
            categoryId = "cat_printing",
            nameAr = "باكيت ورق طباعة A4 دبل A (500 ورقة)",
            descriptionAr = "ورق طباعة أبيض 80 غرام مناسب لجميع أنواع الطابعات والمكائن آمن للنسخ السريع.",
            priceIqd = 7500,
            stockQuantity = 80,
            imageUrl = "https://picsum.photos/seed/a4paper/400/400",
            isNewArrival = false,
            isFeatured = true
        ),
        Product(
            id = "prod_8",
            categoryId = "cat_office",
            nameAr = "منظم مكتب خشب 4 رفوف",
            descriptionAr = "منظم أوراق وملفات مكتبي أنيق مصمم من الخشب المقوى لترتيب المكتب بسهولة.",
            priceIqd = 12500,
            stockQuantity = 12,
            imageUrl = "https://picsum.photos/seed/deskorganizer/400/400",
            isNewArrival = true,
            isFeatured = true
        )
    )
}
