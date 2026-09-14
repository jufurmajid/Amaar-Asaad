package com.amaarasaad.store.data.api

import com.amaarasaad.store.data.model.CartItem
import com.amaarasaad.store.data.model.Category
import com.amaarasaad.store.data.model.Order
import com.amaarasaad.store.data.model.OrderCustomerInfo
import com.amaarasaad.store.data.model.Product
import com.google.gson.annotations.SerializedName

data class ApiCategory(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("nameAr") val nameAr: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("iconName") val iconName: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
) {
    fun toDomain(): Category {
        return Category(
            id = id,
            nameAr = nameAr ?: name,
            iconName = iconName,
            description = description
        )
    }
}

data class ApiProduct(
    @SerializedName("id") val id: String,
    @SerializedName("categoryId") val categoryId: String,
    @SerializedName("name") val name: String,
    @SerializedName("nameAr") val nameAr: String? = null,
    @SerializedName("description") val description: String,
    @SerializedName("descriptionAr") val descriptionAr: String? = null,
    @SerializedName("price") val price: Long,
    @SerializedName("priceIqd") val priceIqd: Long? = null,
    @SerializedName("stock") val stock: Int,
    @SerializedName("stockQuantity") val stockQuantity: Int? = null,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("isAvailable") val isAvailable: Boolean = true,
    @SerializedName("isNewArrival") val isNewArrival: Boolean = false,
    @SerializedName("isFeatured") val isFeatured: Boolean = false,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
) {
    fun toDomain(): Product {
        return Product(
            id = id,
            categoryId = categoryId,
            nameAr = nameAr ?: name,
            descriptionAr = descriptionAr ?: description,
            priceIqd = priceIqd ?: price,
            stockQuantity = stockQuantity ?: stock,
            imageUrl = imageUrl,
            isNewArrival = isNewArrival,
            isFeatured = isFeatured
        )
    }
}

data class ApiOrderItemRequest(
    @SerializedName("productId") val productId: String,
    @SerializedName("quantity") val quantity: Int
)

data class ApiOrderRequest(
    @SerializedName("customerName") val customerName: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("address") val address: String,
    @SerializedName("nearestLandmark") val nearestLandmark: String,
    @SerializedName("notes") val notes: String = "",
    @SerializedName("items") val items: List<ApiOrderItemRequest>,
    @SerializedName("telegramChatId") val telegramChatId: String? = null
)

data class ApiOrderItemResponse(
    @SerializedName("productId") val productId: String,
    @SerializedName("productName") val productName: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("price") val price: Long,
    @SerializedName("subtotal") val subtotal: Long
)

data class ApiOrderResponse(
    @SerializedName("id") val id: String,
    @SerializedName("customerName") val customerName: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("address") val address: String,
    @SerializedName("nearestLandmark") val nearestLandmark: String,
    @SerializedName("notes") val notes: String? = "",
    @SerializedName("totalPrice") val totalPrice: Long,
    @SerializedName("status") val status: String = "PENDING",
    @SerializedName("telegramChatId") val telegramChatId: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("items") val items: List<ApiOrderItemResponse> = emptyList()
) {
    fun toDomain(domainProducts: Map<String, Product> = emptyMap()): Order {
        val cartItems = items.map { item ->
            val domainProd = domainProducts[item.productId] ?: Product(
                id = item.productId,
                categoryId = "",
                nameAr = item.productName,
                descriptionAr = "",
                priceIqd = item.price,
                stockQuantity = 100,
                imageUrl = ""
            )
            CartItem(product = domainProd, quantity = item.quantity)
        }

        return Order(
            id = id,
            customerInfo = OrderCustomerInfo(
                fullName = customerName,
                phoneNumber = phone,
                address = address,
                nearestLandmark = nearestLandmark,
                notes = notes ?: ""
            ),
            items = cartItems,
            totalAmountIqd = totalPrice,
            timestamp = System.currentTimeMillis(),
            status = status
        )
    }
}
