package com.amaarasaad.store

import com.amaarasaad.store.data.api.ApiService
import com.amaarasaad.store.data.datasource.MockDataSource
import com.amaarasaad.store.data.model.OrderCustomerInfo
import com.amaarasaad.store.data.repository.CartRepositoryImpl
import com.amaarasaad.store.data.repository.OrderRepositoryImpl
import com.amaarasaad.store.data.repository.ProductRepositoryImpl
import com.amaarasaad.store.data.repository.RemoteOrderRepository
import com.amaarasaad.store.data.repository.RemoteProductRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLogicTest {

    private lateinit var productRepository: ProductRepositoryImpl
    private lateinit var cartRepository: CartRepositoryImpl
    private lateinit var orderRepository: OrderRepositoryImpl

    private lateinit var remoteProductRepository: RemoteProductRepository
    private lateinit var remoteOrderRepository: RemoteOrderRepository

    @Before
    fun setUp() {
        productRepository = ProductRepositoryImpl()
        cartRepository = CartRepositoryImpl()
        orderRepository = OrderRepositoryImpl(cartRepository)

        val fakeApiService = ApiService.create("http://localhost:9999/api/")
        remoteProductRepository = RemoteProductRepository(fakeApiService, fallbackToMock = true)
        remoteOrderRepository = RemoteOrderRepository(fakeApiService, cartRepository, fallbackToMock = true)
    }

    @Test
    fun testGetCategoriesAndProducts() = runBlocking {
        val categories = productRepository.getCategories()
        val products = productRepository.getProducts()

        assertTrue(categories.isNotEmpty())
        assertTrue(products.isNotEmpty())
        assertEquals("دفاتر", categories.first().nameAr)
    }

    @Test
    fun testRemoteRepositoriesFallbackToMockWhenOffline() = runBlocking {
        val categories = remoteProductRepository.getCategories()
        val products = remoteProductRepository.getProducts()

        assertTrue(categories.isNotEmpty())
        assertTrue(products.isNotEmpty())

        val product = products[0]
        cartRepository.addToCart(product, 2)

        val items = cartRepository.cartItems.first()
        val total = cartRepository.cartTotalIqd.first()

        val info = OrderCustomerInfo(
            fullName = "حسين علي",
            phoneNumber = "07800000000",
            address = "النجف - حي الأمل",
            nearestLandmark = "قرب المستشفى"
        )

        val result = remoteOrderRepository.submitOrder(info, items, total)
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals("حسين علي", result.getOrNull()?.customerInfo?.fullName)

        val remainingCart = cartRepository.cartItems.first()
        assertTrue(remainingCart.isEmpty())
    }

    @Test
    fun testCartOperationsAndTotal() = runBlocking {
        val sampleProduct1 = MockDataSource.products[0]
        val sampleProduct2 = MockDataSource.products[2]

        // Add to cart
        cartRepository.addToCart(sampleProduct1, 2)
        cartRepository.addToCart(sampleProduct2, 1)

        var items = cartRepository.cartItems.first()
        var total = cartRepository.cartTotalIqd.first()

        assertEquals(2, items.size)
        assertEquals(sampleProduct1.priceIqd * 2 + sampleProduct2.priceIqd * 1, total)

        // Update quantity
        cartRepository.updateQuantity(sampleProduct1.id, 3)
        total = cartRepository.cartTotalIqd.first()
        assertEquals(sampleProduct1.priceIqd * 3 + sampleProduct2.priceIqd * 1, total)

        // Remove item
        cartRepository.removeFromCart(sampleProduct2.id)
        items = cartRepository.cartItems.first()
        assertEquals(1, items.size)
        assertEquals(sampleProduct1.id, items[0].product.id)
    }

    @Test
    fun testOrderSubmissionAndCartClearing() = runBlocking {
        val product = MockDataSource.products[0]
        cartRepository.addToCart(product, 2)

        val items = cartRepository.cartItems.first()
        val total = cartRepository.cartTotalIqd.first()

        val info = OrderCustomerInfo(
            fullName = "أحمد علي",
            phoneNumber = "07701234567",
            address = "بغداد - الكرادة",
            nearestLandmark = "قرب ساحة الواثق"
        )

        val result = orderRepository.submitOrder(info, items, total)
        assertTrue(result.isSuccess)

        val order = result.getOrNull()
        assertNotNull(order)
        assertEquals("أحمد علي", order?.customerInfo?.fullName)

        // Verify cart is cleared
        val remainingCart = cartRepository.cartItems.first()
        assertTrue(remainingCart.isEmpty())
    }
}
