package com.amaarasaad.store.data.repository

import com.amaarasaad.store.data.datasource.MockDataSource
import com.amaarasaad.store.data.model.CartItem
import com.amaarasaad.store.data.model.Category
import com.amaarasaad.store.data.model.Order
import com.amaarasaad.store.data.model.OrderCustomerInfo
import com.amaarasaad.store.data.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

interface ProductRepository {
    suspend fun getCategories(): List<Category>
    suspend fun getProducts(): List<Product>
    suspend fun getProductById(productId: String): Product?
    suspend fun getNewArrivals(): List<Product>
    suspend fun getFeaturedProducts(): List<Product>
    suspend fun getProductsByCategory(categoryId: String): List<Product>
    suspend fun searchProducts(query: String): List<Product>
}

interface CartRepository {
    val cartItems: Flow<List<CartItem>>
    val cartTotalIqd: Flow<Long>
    suspend fun addToCart(product: Product, quantity: Int = 1)
    suspend fun updateQuantity(productId: String, newQuantity: Int)
    suspend fun removeFromCart(productId: String)
    suspend fun clearCart()
}

interface OrderRepository {
    suspend fun submitOrder(customerInfo: OrderCustomerInfo, items: List<CartItem>, totalAmount: Long): Result<Order>
}

class ProductRepositoryImpl : ProductRepository {
    override suspend fun getCategories(): List<Category> = MockDataSource.categories

    override suspend fun getProducts(): List<Product> = MockDataSource.products

    override suspend fun getProductById(productId: String): Product? {
        return MockDataSource.products.find { it.id == productId }
    }

    override suspend fun getNewArrivals(): List<Product> {
        return MockDataSource.products.filter { it.isNewArrival }
    }

    override suspend fun getFeaturedProducts(): List<Product> {
        return MockDataSource.products.filter { it.isFeatured }
    }

    override suspend fun getProductsByCategory(categoryId: String): List<Product> {
        return MockDataSource.products.filter { it.categoryId == categoryId }
    }

    override suspend fun searchProducts(query: String): List<Product> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return MockDataSource.products
        return MockDataSource.products.filter {
            it.nameAr.contains(q, ignoreCase = true) || it.descriptionAr.contains(q, ignoreCase = true)
        }
    }
}

class CartRepositoryImpl : CartRepository {
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    override val cartItems: Flow<List<CartItem>> = _items.asStateFlow()

    override val cartTotalIqd: Flow<Long> = _items.map { list ->
        list.sumOf { it.totalPriceIqd }
    }

    override suspend fun addToCart(product: Product, quantity: Int) {
        _items.update { currentList ->
            val existingIndex = currentList.indexOfFirst { it.product.id == product.id }
            if (existingIndex >= 0) {
                val existing = currentList[existingIndex]
                val updatedQuantity = (existing.quantity + quantity).coerceAtMost(product.stockQuantity)
                currentList.toMutableList().apply {
                    this[existingIndex] = existing.copy(quantity = updatedQuantity)
                }
            } else {
                val initialQty = quantity.coerceAtMost(product.stockQuantity)
                currentList + CartItem(product = product, quantity = initialQty)
            }
        }
    }

    override suspend fun updateQuantity(productId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(productId)
            return
        }
        _items.update { currentList ->
            currentList.map { item ->
                if (item.product.id == productId) {
                    val validQty = newQuantity.coerceAtMost(item.product.stockQuantity)
                    item.copy(quantity = validQty)
                } else item
            }
        }
    }

    override suspend fun removeFromCart(productId: String) {
        _items.update { currentList ->
            currentList.filter { it.product.id != productId }
        }
    }

    override suspend fun clearCart() {
        _items.value = emptyList()
    }
}

class OrderRepositoryImpl(
    private val cartRepository: CartRepository
) : OrderRepository {
    override suspend fun submitOrder(
        customerInfo: OrderCustomerInfo,
        items: List<CartItem>,
        totalAmount: Long
    ): Result<Order> {
        return try {
            val newOrder = Order(
                id = "ORD-" + UUID.randomUUID().toString().take(8).uppercase(),
                customerInfo = customerInfo,
                items = items,
                totalAmountIqd = totalAmount
            )
            // Clear cart after submitting
            cartRepository.clearCart()
            Result.success(newOrder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
