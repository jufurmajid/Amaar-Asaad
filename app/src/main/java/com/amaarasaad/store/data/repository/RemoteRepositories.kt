package com.amaarasaad.store.data.repository

import com.amaarasaad.store.data.api.ApiOrderItemRequest
import com.amaarasaad.store.data.api.ApiOrderRequest
import com.amaarasaad.store.data.api.ApiService
import com.amaarasaad.store.data.datasource.MockDataSource
import com.amaarasaad.store.data.model.CartItem
import com.amaarasaad.store.data.model.Category
import com.amaarasaad.store.data.model.Order
import com.amaarasaad.store.data.model.OrderCustomerInfo
import com.amaarasaad.store.data.model.Product
import com.google.gson.JsonParser

class RemoteProductRepository(
    private val apiService: ApiService,
    private val fallbackToMock: Boolean = true
) : ProductRepository {

    override suspend fun getCategories(): List<Category> {
        return try {
            val response = apiService.getCategories()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.map { it.toDomain() }
            } else if (fallbackToMock) {
                MockDataSource.categories
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            if (fallbackToMock) MockDataSource.categories else throw e
        }
    }

    override suspend fun getProducts(): List<Product> {
        return try {
            val response = apiService.getProducts()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.map { it.toDomain() }
            } else if (fallbackToMock) {
                MockDataSource.products
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            if (fallbackToMock) MockDataSource.products else throw e
        }
    }

    override suspend fun getProductById(productId: String): Product? {
        return try {
            val response = apiService.getProductById(productId)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.toDomain()
            } else if (fallbackToMock) {
                MockDataSource.products.find { it.id == productId }
            } else {
                null
            }
        } catch (e: Exception) {
            if (fallbackToMock) MockDataSource.products.find { it.id == productId } else throw e
        }
    }

    override suspend fun getNewArrivals(): List<Product> {
        return try {
            val response = apiService.getProducts(newArrivals = true)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.map { it.toDomain() }
            } else if (fallbackToMock) {
                MockDataSource.products.filter { it.isNewArrival }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            if (fallbackToMock) MockDataSource.products.filter { it.isNewArrival } else throw e
        }
    }

    override suspend fun getFeaturedProducts(): List<Product> {
        return try {
            val response = apiService.getProducts(featured = true)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.map { it.toDomain() }
            } else if (fallbackToMock) {
                MockDataSource.products.filter { it.isFeatured }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            if (fallbackToMock) MockDataSource.products.filter { it.isFeatured } else throw e
        }
    }

    override suspend fun getProductsByCategory(categoryId: String): List<Product> {
        return try {
            val response = apiService.getProducts(categoryId = categoryId)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.map { it.toDomain() }
            } else if (fallbackToMock) {
                MockDataSource.products.filter { it.categoryId == categoryId }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            if (fallbackToMock) MockDataSource.products.filter { it.categoryId == categoryId } else throw e
        }
    }

    override suspend fun searchProducts(query: String): List<Product> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return getProducts()
        return try {
            val response = apiService.getProducts(search = trimmed)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.map { it.toDomain() }
            } else if (fallbackToMock) {
                MockDataSource.products.filter {
                    it.nameAr.contains(trimmed, ignoreCase = true) || it.descriptionAr.contains(trimmed, ignoreCase = true)
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            if (fallbackToMock) {
                MockDataSource.products.filter {
                    it.nameAr.contains(trimmed, ignoreCase = true) || it.descriptionAr.contains(trimmed, ignoreCase = true)
                }
            } else throw e
        }
    }
}

class RemoteOrderRepository(
    private val apiService: ApiService,
    private val cartRepository: CartRepository,
    private val fallbackToMock: Boolean = true
) : OrderRepository {

    override suspend fun submitOrder(
        customerInfo: OrderCustomerInfo,
        items: List<CartItem>,
        totalAmount: Long
    ): Result<Order> {
        return try {
            val orderItemsRequest = items.map {
                ApiOrderItemRequest(
                    productId = it.product.id,
                    quantity = it.quantity
                )
            }

            val request = ApiOrderRequest(
                customerName = customerInfo.fullName,
                phone = customerInfo.phoneNumber,
                address = customerInfo.address,
                nearestLandmark = customerInfo.nearestLandmark,
                notes = customerInfo.notes,
                items = orderItemsRequest
            )

            val response = apiService.createOrder(request)
            if (response.isSuccessful && response.body() != null) {
                val productsMap = items.associateBy { it.product.id }.mapValues { it.value.product }
                val domainOrder = response.body()!!.toDomain(productsMap)
                cartRepository.clearCart()
                Result.success(domainOrder)
            } else {
                val errorMsg = parseErrorMessage(response.errorBody()?.string())
                if (fallbackToMock && errorMsg == null) {
                    val mockRepo = OrderRepositoryImpl(cartRepository)
                    mockRepo.submitOrder(customerInfo, items, totalAmount)
                } else {
                    Result.failure(Exception(errorMsg ?: "فشل في تسجيل الطلب"))
                }
            }
        } catch (e: Exception) {
            if (fallbackToMock) {
                val mockRepo = OrderRepositoryImpl(cartRepository)
                mockRepo.submitOrder(customerInfo, items, totalAmount)
            } else {
                Result.failure(e)
            }
        }
    }

    private fun parseErrorMessage(json: String?): String? {
        if (json.isNullOrBlank()) return null
        return try {
            @Suppress("DEPRECATION")
            val jsonObject = JsonParser().parse(json).asJsonObject
            if (jsonObject.has("error")) {
                jsonObject.get("error").asString
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
