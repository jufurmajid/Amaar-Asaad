package com.amaarasaad.store.data.model

data class Category(
    val id: String,
    val nameAr: String,
    val iconName: String? = null,
    val description: String? = null
)

data class Product(
    val id: String,
    val categoryId: String,
    val nameAr: String,
    val descriptionAr: String,
    val priceIqd: Long,
    val stockQuantity: Int,
    val imageUrl: String,
    val isNewArrival: Boolean = false,
    val isFeatured: Boolean = false
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
    val address: String,
    val nearestLandmark: String,
    val notes: String = ""
)

data class Order(
    val id: String,
    val customerInfo: OrderCustomerInfo,
    val items: List<CartItem>,
    val totalAmountIqd: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING"
)
