package com.amaarasaad.store.data.repository

import com.amaarasaad.store.data.datasource.MockDataSource
import com.amaarasaad.store.data.model.CartItem
import com.amaarasaad.store.data.model.Category
import com.amaarasaad.store.data.model.Order
import com.amaarasaad.store.data.model.OrderCustomerInfo
import com.amaarasaad.store.data.model.OrderStatus
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

    // Admin management methods
    suspend fun addProduct(product: Product): Boolean
    suspend fun updateProduct(product: Product): Boolean
    suspend fun deleteProduct(productId: String): Boolean
    suspend fun updateProductStock(productId: String, newStock: Int): Boolean
    suspend fun updateProductVisibility(productId: String, isVisible: Boolean): Boolean
    suspend fun addCategory(category: Category): Boolean
    suspend fun updateCategory(category: Category): Boolean
    suspend fun deleteCategory(categoryId: String): Boolean
}

interface CartRepository {
    val cartItems: Flow<List<CartItem>>
    val cartTotalIqd: Flow<Long>
    suspend fun addToCart(product: Product, quantity: Int = 1): Result<Unit>
    suspend fun updateQuantity(productId: String, newQuantity: Int): Result<Unit>
    suspend fun removeFromCart(productId: String)
    suspend fun clearCart()
}

interface OrderRepository {
    val orders: Flow<List<Order>>
    suspend fun getAllOrders(): List<Order>
    suspend fun getOrderById(orderId: String): Order?
    suspend fun submitOrder(customerInfo: OrderCustomerInfo, items: List<CartItem>): Result<Order>
    suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Order>
}

class ProductRepositoryImpl : ProductRepository {
    private val productsList = MockDataSource.products
    private val categoriesList = MockDataSource.categories.toMutableList()

    override suspend fun getCategories(): List<Category> = categoriesList.sortedBy { it.sortOrder }

    override suspend fun getProducts(): List<Product> {
        return productsList.filter { it.isVisible }
    }

    override suspend fun getProductById(productId: String): Product? {
        return productsList.find { it.id == productId }
    }

    override suspend fun getNewArrivals(): List<Product> {
        return productsList.filter { it.isVisible && it.isNewArrival && it.isAvailable }
    }

    override suspend fun getFeaturedProducts(): List<Product> {
        return productsList.filter { it.isVisible && it.isFeatured && it.isAvailable }
    }

    override suspend fun getProductsByCategory(categoryId: String): List<Product> {
        return productsList.filter { it.isVisible && it.categoryId == categoryId }
    }

    override suspend fun searchProducts(query: String): List<Product> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return getProducts()
        return productsList.filter {
            it.isVisible && (it.nameAr.contains(q, ignoreCase = true) || it.descriptionAr.contains(q, ignoreCase = true))
        }
    }

    override suspend fun addProduct(product: Product): Boolean {
        productsList.add(product)
        return true
    }

    override suspend fun updateProduct(product: Product): Boolean {
        val index = productsList.indexOfFirst { it.id == product.id }
        if (index >= 0) {
            productsList[index] = product
            return true
        }
        return false
    }

    override suspend fun deleteProduct(productId: String): Boolean {
        return productsList.removeIf { it.id == productId }
    }

    override suspend fun updateProductStock(productId: String, newStock: Int): Boolean {
        val index = productsList.indexOfFirst { it.id == productId }
        if (index >= 0) {
            val updated = productsList[index].copy(
                stockQuantity = newStock,
                isAvailable = newStock > 0
            )
            productsList[index] = updated
            return true
        }
        return false
    }

    override suspend fun updateProductVisibility(productId: String, isVisible: Boolean): Boolean {
        val index = productsList.indexOfFirst { it.id == productId }
        if (index >= 0) {
            productsList[index] = productsList[index].copy(isVisible = isVisible)
            return true
        }
        return false
    }

    override suspend fun addCategory(category: Category): Boolean {
        categoriesList.add(category)
        return true
    }

    override suspend fun updateCategory(category: Category): Boolean {
        val index = categoriesList.indexOfFirst { it.id == category.id }
        if (index >= 0) {
            categoriesList[index] = category
            return true
        }
        return false
    }

    override suspend fun deleteCategory(categoryId: String): Boolean {
        return categoriesList.removeIf { it.id == categoryId }
    }
}

class CartRepositoryImpl(
    private val productRepository: ProductRepository
) : CartRepository {
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    override val cartItems: Flow<List<CartItem>> = _items.asStateFlow()

    override val cartTotalIqd: Flow<Long> = _items.map { list ->
        list.sumOf { it.totalPriceIqd }
    }

    override suspend fun addToCart(product: Product, quantity: Int): Result<Unit> {
        if (quantity <= 0) {
            return Result.failure(IllegalArgumentException("يجب أن تكون الكمية أكبر من صفر"))
        }

        // Re-fetch current product from repository for security check
        val freshProduct = productRepository.getProductById(product.id)
            ?: return Result.failure(IllegalStateException("المنتج غير موجود"))

        if (!freshProduct.isAvailable || freshProduct.stockQuantity <= 0) {
            return Result.failure(IllegalStateException("المنتج غير متوفر حالياً"))
        }

        var errorMessage: String? = null

        _items.update { currentList ->
            val existingIndex = currentList.indexOfFirst { it.product.id == freshProduct.id }
            val currentQty = if (existingIndex >= 0) currentList[existingIndex].quantity else 0
            val requestedTotalQty = currentQty + quantity

            if (requestedTotalQty > freshProduct.stockQuantity) {
                errorMessage = "الكمية المطلوبة تتجاوز المخزون المتوفر (${freshProduct.stockQuantity})"
                currentList
            } else {
                if (existingIndex >= 0) {
                    currentList.toMutableList().apply {
                        this[existingIndex] = CartItem(product = freshProduct, quantity = requestedTotalQty)
                    }
                } else {
                    currentList + CartItem(product = freshProduct, quantity = quantity)
                }
            }
        }

        return errorMessage?.let { Result.failure(IllegalStateException(it)) } ?: Result.success(Unit)
    }

    override suspend fun updateQuantity(productId: String, newQuantity: Int): Result<Unit> {
        if (newQuantity <= 0) {
            removeFromCart(productId)
            return Result.success(Unit)
        }

        val freshProduct = productRepository.getProductById(productId)
            ?: run {
                removeFromCart(productId)
                return Result.failure(IllegalStateException("المنتج غير موجود"))
            }

        if (newQuantity > freshProduct.stockQuantity) {
            return Result.failure(IllegalStateException("الكمية المطلوبة تتجاوز المخزون المتوفر (${freshProduct.stockQuantity})"))
        }

        _items.update { currentList ->
            currentList.map { item ->
                if (item.product.id == productId) {
                    CartItem(product = freshProduct, quantity = newQuantity)
                } else item
            }
        }
        return Result.success(Unit)
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
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    private val onOrderCreatedListener: (suspend (Order) -> Unit)? = null
) : OrderRepository {

    private val _ordersList = MutableStateFlow<List<Order>>(emptyList())
    override val orders: Flow<List<Order>> = _ordersList.asStateFlow()

    override suspend fun getAllOrders(): List<Order> = _ordersList.value

    override suspend fun getOrderById(orderId: String): Order? {
        return _ordersList.value.find { it.id == orderId }
    }

    override suspend fun submitOrder(
        customerInfo: OrderCustomerInfo,
        items: List<CartItem>
    ): Result<Order> {
        return try {
            if (items.isEmpty()) {
                return Result.failure(IllegalArgumentException("لا يمكن إنشاء طلب بسلة فارغة"))
            }

            if (customerInfo.fullName.isBlank() || customerInfo.phoneNumber.isBlank() || customerInfo.address.isBlank()) {
                return Result.failure(IllegalArgumentException("يرجى إدخال كافة البيانات المطلوب إكمالها للتوصيل"))
            }

            // Recalculate price and validate stock for each item from server/repository source of truth
            val validatedItems = mutableListOf<CartItem>()
            var calculatedTotalIqd = 0L

            for (item in items) {
                val freshProduct = productRepository.getProductById(item.product.id)
                    ?: return Result.failure(IllegalStateException("المنتج '${item.product.nameAr}' لم يعد متوفراً"))

                if (!freshProduct.isAvailable || freshProduct.stockQuantity <= 0) {
                    return Result.failure(IllegalStateException("المنتج '${freshProduct.nameAr}' غير متوفر حالياً"))
                }

                if (item.quantity <= 0) {
                    return Result.failure(IllegalArgumentException("كمية غير صالحة للمنتج '${freshProduct.nameAr}'"))
                }

                if (item.quantity > freshProduct.stockQuantity) {
                    return Result.failure(IllegalStateException("الكمية المطلوبة للمنتج '${freshProduct.nameAr}' تتجاوز المخزون (${freshProduct.stockQuantity})"))
                }

                if (freshProduct.priceIqd <= 0) {
                    return Result.failure(IllegalStateException("سعر غير صالح للمنتج '${freshProduct.nameAr}'"))
                }

                val validatedCartItem = CartItem(product = freshProduct, quantity = item.quantity)
                validatedItems.add(validatedCartItem)
                calculatedTotalIqd += validatedCartItem.totalPriceIqd
            }

            val newOrderId = "ABU-" + UUID.randomUUID().toString().take(6).uppercase()
            val newOrder = Order(
                id = newOrderId,
                customerInfo = customerInfo,
                items = validatedItems,
                totalAmountIqd = calculatedTotalIqd,
                paymentMethod = "الدفع عند الاستلام",
                status = OrderStatus.NEW.name,
                timestamp = System.currentTimeMillis()
            )

            // Deduct stock in repository
            for (item in validatedItems) {
                val freshProduct = productRepository.getProductById(item.product.id)
                if (freshProduct != null) {
                    val remainingStock = freshProduct.stockQuantity - item.quantity
                    productRepository.updateProductStock(item.product.id, remainingStock)
                }
            }

            _ordersList.update { listOf(newOrder) + it }

            // Clear cart
            cartRepository.clearCart()

            // Notify listener (e.g., Telegram service)
            onOrderCreatedListener?.invoke(newOrder)

            Result.success(newOrder)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Order> {
        val currentOrders = _ordersList.value.toMutableList()
        val index = currentOrders.indexOfFirst { it.id == orderId }
        if (index >= 0) {
            val updatedOrder = currentOrders[index].copy(status = newStatus)
            currentOrders[index] = updatedOrder
            _ordersList.value = currentOrders
            return Result.success(updatedOrder)
        }
        return Result.failure(IllegalArgumentException("الطلب غير موجود"))
    }
}
