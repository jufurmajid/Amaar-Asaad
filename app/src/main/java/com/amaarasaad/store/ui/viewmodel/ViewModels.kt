package com.amaarasaad.store.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amaarasaad.store.data.model.CartItem
import com.amaarasaad.store.data.model.Category
import com.amaarasaad.store.data.model.Order
import com.amaarasaad.store.data.model.OrderCustomerInfo
import com.amaarasaad.store.data.model.Product
import com.amaarasaad.store.data.repository.CartRepository
import com.amaarasaad.store.data.repository.OrderRepository
import com.amaarasaad.store.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class HomeData(
    val categories: List<Category>,
    val newArrivals: List<Product>,
    val featuredProducts: List<Product>
)

class HomeViewModel(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val uiState: StateFlow<UiState<HomeData>> = _uiState

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val categories = productRepository.getCategories()
                val newArrivals = productRepository.getNewArrivals()
                val featured = productRepository.getFeaturedProducts()
                _uiState.value = UiState.Success(
                    HomeData(
                        categories = categories,
                        newArrivals = newArrivals,
                        featuredProducts = featured
                    )
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error("حدث خطأ أثناء تحميل البيانات: ${e.message}")
            }
        }
    }

    fun addToCart(product: Product) {
        viewModelScope.launch {
            cartRepository.addToCart(product, 1)
        }
    }
}

class CategoriesViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Category>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Category>>> = _uiState

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val categories = productRepository.getCategories()
                _uiState.value = UiState.Success(categories)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("فشل في تحميل الأقسام")
            }
        }
    }
}

class ProductsViewModel(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Product>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Product>>> = _uiState

    fun loadProducts(categoryId: String? = null, searchQuery: String? = null) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val products = when {
                    !searchQuery.isNullOrBlank() -> productRepository.searchProducts(searchQuery)
                    !categoryId.isNullOrEmpty() -> productRepository.getProductsByCategory(categoryId)
                    else -> productRepository.getProducts()
                }
                _uiState.value = UiState.Success(products)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("فشل في تحميل المنتجات")
            }
        }
    }

    fun addToCart(product: Product) {
        viewModelScope.launch {
            cartRepository.addToCart(product, 1)
        }
    }
}

class ProductDetailsViewModel(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Product>>(UiState.Loading)
    val uiState: StateFlow<UiState<Product>> = _uiState

    private val _selectedQuantity = MutableStateFlow(1)
    val selectedQuantity: StateFlow<Int> = _selectedQuantity

    fun loadProductDetails(productId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val product = productRepository.getProductById(productId)
            if (product != null) {
                _uiState.value = UiState.Success(product)
            } else {
                _uiState.value = UiState.Error("المنتج غير موجود")
            }
        }
    }

    fun updateQuantity(newQty: Int) {
        _selectedQuantity.value = newQty
    }

    fun addToCart(product: Product) {
        viewModelScope.launch {
            cartRepository.addToCart(product, _selectedQuantity.value)
        }
    }
}

class CartViewModel(
    private val cartRepository: CartRepository
) : ViewModel() {

    val cartItems: StateFlow<List<CartItem>> = cartRepository.cartItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalAmount: StateFlow<Long> = cartRepository.cartTotalIqd
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun increaseQuantity(item: CartItem) {
        viewModelScope.launch {
            cartRepository.updateQuantity(item.product.id, item.quantity + 1)
        }
    }

    fun decreaseQuantity(item: CartItem) {
        viewModelScope.launch {
            cartRepository.updateQuantity(item.product.id, item.quantity - 1)
        }
    }

    fun removeItem(productId: String) {
        viewModelScope.launch {
            cartRepository.removeFromCart(productId)
        }
    }
}

sealed interface CheckoutUiState {
    object Idle : CheckoutUiState
    object Loading : CheckoutUiState
    data class Success(val order: Order) : CheckoutUiState
    data class Error(val message: String) : CheckoutUiState
}

class CheckoutViewModel(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Idle)
    val uiState: StateFlow<CheckoutUiState> = _uiState

    val cartItems: StateFlow<List<CartItem>> = cartRepository.cartItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalAmount: StateFlow<Long> = cartRepository.cartTotalIqd
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun submitOrder(customerInfo: OrderCustomerInfo) {
        val items = cartItems.value
        val total = totalAmount.value

        if (items.isEmpty()) {
            _uiState.value = CheckoutUiState.Error("السلة فارغة")
            return
        }

        if (customerInfo.fullName.isBlank() || customerInfo.phoneNumber.isBlank() || customerInfo.address.isBlank() || customerInfo.nearestLandmark.isBlank()) {
            _uiState.value = CheckoutUiState.Error("يرجى ملء جميع الحقول المطلوبة")
            return
        }

        viewModelScope.launch {
            _uiState.value = CheckoutUiState.Loading
            val result = orderRepository.submitOrder(customerInfo, items, total)
            result.onSuccess { order ->
                _uiState.value = CheckoutUiState.Success(order)
            }.onFailure { ex ->
                _uiState.value = CheckoutUiState.Error("فشل في تقديم الطلب: ${ex.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = CheckoutUiState.Idle
    }
}
