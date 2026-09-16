package com.amaarasaad.store.data.datasource

import com.amaarasaad.store.data.model.Category
import com.amaarasaad.store.data.model.Product

/**
 * Mock Data Source for "أبو هاشم للحوم والألبان والأجبان".
 * Decouples sample data from UI and ViewModel layers, supporting
 * local and backend API integration.
 */
object MockDataSource {

    val categories = listOf(
        Category(
            id = "cat_meat",
            nameAr = "اللحوم الطازجة",
            iconName = "meat",
            description = "لحوم غنم وبقر عراقية طازجة ومذبوحة يومياً حسب الشريعة الإسلامية",
            sortOrder = 1
        ),
        Category(
            id = "cat_dairy",
            nameAr = "الألبان الحليبية",
            iconName = "dairy",
            description = "ألبان، حليب، قيمر عرب، ولبن خاثر طازج يومياً من المزرعة مباشرة",
            sortOrder = 2
        ),
        Category(
            id = "cat_cheese",
            nameAr = "الأجبان الفاخرة",
            iconName = "cheese",
            description = "تشكيلة من الأجبان البلدية والبلدات والمبشورة عالية الجودة",
            sortOrder = 3
        )
    )

    val products = mutableListOf(
        Product(
            id = "prod_meat_1",
            categoryId = "cat_meat",
            nameAr = "لحم غنم عراقي طازج",
            descriptionAr = "لحم غنم بلدي طازج مذبوح يومياً، خالي من الدهون الزائدة مناسب للطبخ والشواء.",
            priceIqd = 22000,
            unit = "كيلو",
            stockQuantity = 25,
            imageUrl = "https://picsum.photos/seed/lambmeat/400/400",
            isAvailable = true,
            isVisible = true,
            isNewArrival = true,
            isFeatured = true,
            isBestSeller = true
        ),
        Product(
            id = "prod_meat_2",
            categoryId = "cat_meat",
            nameAr = "لحم بقر مفروم بلدي",
            descriptionAr = "لحم بقر طازج مفروم وجهين بنسبة دهن متوازنة مثالي للكبة والكباب والوجبات.",
            priceIqd = 18000,
            unit = "كيلو",
            stockQuantity = 30,
            imageUrl = "https://picsum.photos/seed/mincedmeat/400/400",
            isAvailable = true,
            isVisible = true,
            isNewArrival = true,
            isFeatured = true,
            isBestSeller = true
        ),
        Product(
            id = "prod_meat_3",
            categoryId = "cat_meat",
            nameAr = "كباب عراقي متبل جاهز للشوي",
            descriptionAr = "أسياخ كباب غنم وبقر متبل بالبهارات العراقيّة الخاصة جاهز للتدوير والشوي مباشرة.",
            priceIqd = 20000,
            unit = "كيلو",
            stockQuantity = 15,
            imageUrl = "https://picsum.photos/seed/kebab/400/400",
            isAvailable = true,
            isVisible = true,
            isNewArrival = false,
            isFeatured = true,
            isBestSeller = true
        ),
        Product(
            id = "prod_meat_4",
            categoryId = "cat_meat",
            nameAr = "ريش غنم بلدي ممتازة",
            descriptionAr = "قطع ريش غنم طرية مقطعة بعناية ومناسبة للشوي على الفحم أو الصينية.",
            priceIqd = 24000,
            unit = "كيلو",
            stockQuantity = 10,
            imageUrl = "https://picsum.photos/seed/lambchops/400/400",
            isAvailable = true,
            isVisible = true,
            isNewArrival = true,
            isFeatured = false,
            isBestSeller = false
        ),
        Product(
            id = "prod_meat_5",
            categoryId = "cat_meat",
            nameAr = "شقف لحم بقر للطبخ",
            descriptionAr = "قطع لحم بقر صافي بدون عظم مقطعة مكعبات مثالية للقص والمرق والطبخ اليومي.",
            priceIqd = 10000,
            unit = "نصف كيلو",
            stockQuantity = 20,
            imageUrl = "https://picsum.photos/seed/beefcubes/400/400",
            isAvailable = true,
            isVisible = true,
            isNewArrival = false,
            isFeatured = false,
            isBestSeller = false
        ),
        Product(
            id = "prod_dairy_1",
            categoryId = "cat_dairy",
            nameAr = "قيمر عرب عراقي طازج",
            descriptionAr = "قيمر سدة بلدي طازج مصنع يومياً بطريقة تقليدية نكهة وقوام فاخر جداً.",
            priceIqd = 6000,
            unit = "ربع كيلو",
            stockQuantity = 40,
            imageUrl = "https://picsum.photos/seed/kaymak/400/400",
            isAvailable = true,
            isVisible = true,
            isNewArrival = true,
            isFeatured = true,
            isBestSeller = true
        ),
        Product(
            id = "prod_dairy_2",
            categoryId = "cat_dairy",
            nameAr = "لبن خاثر أربيل طازج",
            descriptionAr = "سطيل لبن خاثر طبيعي 100% بدون إضافات حافظة غني بالفوائد والمذاق الأصيل.",
            priceIqd = 4500,
            unit = "عبوة",
            stockQuantity = 50,
            imageUrl = "https://picsum.photos/seed/yogurt/400/400",
            isAvailable = true,
            isVisible = true,
            isNewArrival = false,
            isFeatured = true,
            isBestSeller = true
        ),
        Product(
            id = "prod_dairy_3",
            categoryId = "cat_dairy",
            nameAr = "حليب بقر طازج كامل الدسم",
            descriptionAr = "حليب طبيعي طازج غير مضاف له أي مواد حافظة معقم ومبستر وجاهز للاستهلاك.",
            priceIqd = 2500,
            unit = "عبوة",
            stockQuantity = 35,
            imageUrl = "https://picsum.photos/seed/freshmilk/400/400",
            isAvailable = true,
            isVisible = true,
            isNewArrival = true,
            isFeatured = false,
            isBestSeller = false
        ),
        Product(
            id = "prod_dairy_4",
            categoryId = "cat_dairy",
            nameAr = "زبدة عرب بلدية طازجة",
            descriptionAr = "زبدة بلدي طبيعية ممتازة مستخرجة من ألبان الأبقار والجاموس بدون ملح.",
            priceIqd = 7000,
            unit = "نصف كيلو",
            stockQuantity = 18,
            imageUrl = "https://picsum.photos/seed/freshbutter/400/400",
            isAvailable = true,
            isVisible = true,
            isNewArrival = false,
            isFeatured = true,
            isBestSeller = false
        ),
        Product(
            id = "prod_cheese_1",
            categoryId = "cat_cheese",
            nameAr = "جبن عرب بلدي مع حبة البركة",
            descriptionAr = "جبن بلدي طازج قليـل الملح مع نكهة حبة البركة الرائعة خالي من المواد الحافظة.",
            priceIqd = 12000,
            unit = "كيلو",
            stockQuantity = 30,
            imageUrl = "https://picsum.photos/seed/arabcheese/400/400",
            isAvailable = true,
            isVisible = true,
            isNewArrival = true,
            isFeatured = true,
            isBestSeller = true
        ),
        Product(
            id = "prod_cheese_2",
            categoryId = "cat_cheese",
            nameAr = "جبن شلل مشلل فاخر",
            descriptionAr = "جبن شلل مالح قليل دسم ذو خيوط طرية ومذاق رائع للفطور والسندويشات.",
            priceIqd = 6500,
            unit = "نصف كيلو",
            stockQuantity = 22,
            imageUrl = "https://picsum.photos/seed/stringcheese/400/400",
            isAvailable = true,
            isVisible = true,
            isNewArrival = false,
            isFeatured = true,
            isBestSeller = false
        ),
        Product(
            id = "prod_cheese_3",
            categoryId = "cat_cheese",
            nameAr = "جبن موزاريلا مبشور طازج",
            descriptionAr = "كيس جبن موزاريلا غني ومطاطي جداً للمخبوزات والبيتزا والمعجنات.",
            priceIqd = 5000,
            unit = "عبوة",
            stockQuantity = 25,
            imageUrl = "https://picsum.photos/seed/mozzarella/400/400",
            isAvailable = true,
            isVisible = true,
            isNewArrival = true,
            isFeatured = false,
            isBestSeller = true
        )
    )
}
