package com.amaarasaad.store

import com.amaarasaad.store.data.datasource.MockDataSource
import com.amaarasaad.store.data.model.OrderCustomerInfo
import com.amaarasaad.store.data.model.OrderStatus
import com.amaarasaad.store.data.repository.CartRepositoryImpl
import com.amaarasaad.store.data.repository.OrderRepositoryImpl
import com.amaarasaad.store.data.repository.ProductRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLogicTest {

    private lateinit var productRepository: ProductRepositoryImpl
    private lateinit var cartRepository: CartRepositoryImpl
    private lateinit var orderRepository: OrderRepositoryImpl

    @Before
    fun setUp() {
        productRepository = ProductRepositoryImpl()
        cartRepository = CartRepositoryImpl(productRepository)
        orderRepository = OrderRepositoryImpl(productRepository, cartRepository)
    }

    @Test
    fun testGetMeatDairyCategoriesAndProducts() = runBlocking {
        val categories = productRepository.getCategories()
        val products = productRepository.getProducts()

        assertTrue(categories.isNotEmpty())
        assertTrue(products.isNotEmpty())

        val meatCategory = categories.find { it.id == "cat_meat" }
        assertNotNull(meatCategory)
        assertEquals("اللحوم الطازجة", meatCategory?.nameAr)

        val meatProducts = productRepository.getProductsByCategory("cat_meat")
        assertTrue(meatProducts.isNotEmpty())
        assertTrue(meatProducts.all { it.categoryId == "cat_meat" })
    }

    @Test
    fun testSearchMeatProducts() = runBlocking {
        val searchResults = productRepository.searchProducts("لحم")
        assertTrue(searchResults.isNotEmpty())
        assertTrue(searchResults.any { it.nameAr.contains("لحم") })

        val cheeseResults = productRepository.searchProducts("جبن")
        assertTrue(cheeseResults.isNotEmpty())
        assertTrue(cheeseResults.any { it.nameAr.contains("جبن") })
    }

    @Test
    fun testCartOperationsAndTotalCalculation() = runBlocking {
        val meatProduct = productRepository.getProductById("prod_meat_1")!!
        val dairyProduct = productRepository.getProductById("prod_dairy_1")!!

        // Add to cart
        val addRes1 = cartRepository.addToCart(meatProduct, 2)
        val addRes2 = cartRepository.addToCart(dairyProduct, 1)

        assertTrue(addRes1.isSuccess)
        assertTrue(addRes2.isSuccess)

        var items = cartRepository.cartItems.first()
        var total = cartRepository.cartTotalIqd.first()

        assertEquals(2, items.size)
        assertEquals(meatProduct.priceIqd * 2 + dairyProduct.priceIqd * 1, total)

        // Update quantity
        val updateRes = cartRepository.updateQuantity(meatProduct.id, 3)
        assertTrue(updateRes.isSuccess)

        total = cartRepository.cartTotalIqd.first()
        assertEquals(meatProduct.priceIqd * 3 + dairyProduct.priceIqd * 1, total)

        // Remove item
        cartRepository.removeFromCart(dairyProduct.id)
        items = cartRepository.cartItems.first()
        assertEquals(1, items.size)
        assertEquals(meatProduct.id, items[0].product.id)
    }

    @Test
    fun testOrderSubmissionDeductsStockAndClearsCart() = runBlocking {
        val product = productRepository.getProductById("prod_meat_1")!!
        val initialStock = product.stockQuantity

        cartRepository.addToCart(product, 2)

        val items = cartRepository.cartItems.first()

        val info = OrderCustomerInfo(
            fullName = "جعفر ماجد",
            phoneNumber = "07701234567",
            city = "بغداد",
            address = "الكرادة - الشارع العام",
            nearestLandmark = "مقابل جامع الخلاني",
            notes = "يرجى تقطيع اللحم إلى قطع صغيرة"
        )

        val result = orderRepository.submitOrder(info, items)
        assertTrue(result.isSuccess)

        val order = result.getOrNull()
        assertNotNull(order)
        assertEquals("جعفر ماجد", order?.customerInfo?.fullName)
        assertEquals("الدفع عند الاستلام", order?.paymentMethod)
        assertEquals(OrderStatus.NEW.name, order?.status)

        // Verify stock deducted
        val updatedProduct = productRepository.getProductById(product.id)!!
        assertEquals(initialStock - 2, updatedProduct.stockQuantity)

        // Verify cart is cleared
        val remainingCart = cartRepository.cartItems.first()
        assertTrue(remainingCart.isEmpty())
    }

    @Test
    fun testAdminOrderStatusUpdate() = runBlocking {
        val product = productRepository.getProductById("prod_meat_2")!!
        cartRepository.addToCart(product, 1)

        val info = OrderCustomerInfo(
            fullName = "علي حسين",
            phoneNumber = "07800000000",
            city = "بغداد",
            address = "المنصور",
            nearestLandmark = "قرب الرواد"
        )

        val result = orderRepository.submitOrder(info, cartRepository.cartItems.first())
        val orderId = result.getOrThrow().id

        // Update status to PREPARING
        val updateRes = orderRepository.updateOrderStatus(orderId, OrderStatus.PREPARING.name)
        assertTrue(updateRes.isSuccess)
        assertEquals(OrderStatus.PREPARING.name, updateRes.getOrNull()?.status)
    }

    @Test
    fun testExceedStockCartRejection() = runBlocking {
        val product = productRepository.getProductById("prod_meat_1")!!
        val result = cartRepository.addToCart(product, product.stockQuantity + 100)
        assertTrue(result.isFailure)
    }
}
