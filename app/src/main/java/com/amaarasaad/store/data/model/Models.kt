package com.amaarasaad.store.data.model

data class Category(
    val id: String,
    val nameAr: String,
    val iconName: String? = null,
    val description: String? = null,
    val sortOrder: Int = 0
)

data class Product(
    val id: String,
    val categoryId: String,
    val nameAr: String,
    val descriptionAr: String,
    val priceIqd: Long,
    val unit: String = "كيلو", // "كيلو", "نصف كيلو", "ربع كيلو", "قطعة", "عبوة"
    val stockQuantity: Int = 10,
    val imageUrl: String,
    val isAvailable: Boolean = true,
    val isVisible: Boolean = true,
    val isNewArrival: Boolean = false,
    val isFeatured: Boolean = false,
    val isBestSeller: Boolean = false
)

data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val totalPriceIqd: Long get() = product.priceIqd * quantity
}

data class OrderCustomerInfo(
    val fullName: String,
    val phoneNumber: String,
    val city: String = "بغداد",
    val address: String,
    val nearestLandmark: String,
    val notes: String = ""
)

enum class OrderStatus(val titleAr: String) {
    NEW("جديد"),
    CONFIRMED("مؤكد"),
    PREPARING("قيد التجهيز"),
    OUT_FOR_DELIVERY("خرج للتوصيل"),
    DELIVERED("تم التوصيل"),
    CANCELLED("ملغى")
}

data class Order(
    val id: String,
    val customerInfo: OrderCustomerInfo,
    val items: List<CartItem>,
    val totalAmountIqd: Long,
    val paymentMethod: String = "الدفع عند الاستلام",
    val status: String = OrderStatus.NEW.name,
    val timestamp: Long = System.currentTimeMillis()
)
