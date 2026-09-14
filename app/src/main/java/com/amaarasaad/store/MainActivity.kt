package com.amaarasaad.store

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.amaarasaad.store.data.api.ApiService
import com.amaarasaad.store.data.repository.CartRepositoryImpl
import com.amaarasaad.store.data.repository.RemoteOrderRepository
import com.amaarasaad.store.data.repository.RemoteProductRepository
import com.amaarasaad.store.ui.navigation.Screen
import com.amaarasaad.store.ui.screens.CartScreen
import com.amaarasaad.store.ui.screens.CategoriesScreen
import com.amaarasaad.store.ui.screens.CheckoutScreen
import com.amaarasaad.store.ui.screens.HomeScreen
import com.amaarasaad.store.ui.screens.ProductDetailsScreen
import com.amaarasaad.store.ui.screens.ProductsListScreen
import com.amaarasaad.store.ui.theme.AmaarAsaadStoreTheme
import com.amaarasaad.store.ui.theme.NavyPrimary
import com.amaarasaad.store.ui.theme.TealAccent
import com.amaarasaad.store.ui.viewmodel.CartViewModel
import com.amaarasaad.store.ui.viewmodel.CategoriesViewModel
import com.amaarasaad.store.ui.viewmodel.CheckoutViewModel
import com.amaarasaad.store.ui.viewmodel.HomeViewModel
import com.amaarasaad.store.ui.viewmodel.ProductDetailsViewModel
import com.amaarasaad.store.ui.viewmodel.ProductsViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize API Service & Repositories
        val apiService = ApiService.create(BuildConfig.BASE_URL)
        val cartRepository = CartRepositoryImpl()
        val productRepository = RemoteProductRepository(apiService, fallbackToMock = true)
        val orderRepository = RemoteOrderRepository(apiService, cartRepository, fallbackToMock = true)

        // Initialize ViewModels
        val homeViewModel = HomeViewModel(productRepository, cartRepository)
        val categoriesViewModel = CategoriesViewModel(productRepository)
        val productsViewModel = ProductsViewModel(productRepository, cartRepository)
        val productDetailsViewModel = ProductDetailsViewModel(productRepository, cartRepository)
        val cartViewModel = CartViewModel(cartRepository)
        val checkoutViewModel = CheckoutViewModel(cartRepository, orderRepository)

        setContent {
            AmaarAsaadStoreTheme {
                MainAppScreen(
                    homeViewModel = homeViewModel,
                    categoriesViewModel = categoriesViewModel,
                    productsViewModel = productsViewModel,
                    productDetailsViewModel = productDetailsViewModel,
                    cartViewModel = cartViewModel,
                    checkoutViewModel = checkoutViewModel
                )
            }
        }
    }
}

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)

@Composable
fun MainAppScreen(
    homeViewModel: HomeViewModel,
    categoriesViewModel: CategoriesViewModel,
    productsViewModel: ProductsViewModel,
    productDetailsViewModel: ProductDetailsViewModel,
    cartViewModel: CartViewModel,
    checkoutViewModel: CheckoutViewModel
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val cartItems by cartViewModel.cartItems.collectAsState()
    val totalCartCount = cartItems.sumOf { it.quantity }

    val bottomNavItems = listOf(
        BottomNavItem("الرئيسية", Screen.Home.route, Icons.Default.Home),
        BottomNavItem("الأقسام", Screen.Categories.route, Icons.Default.Category),
        BottomNavItem("السلة", Screen.Cart.route, Icons.Default.ShoppingCart)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom bar on Checkout & Product Details screens
    val shouldShowBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Categories.route,
        Screen.Cart.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar(containerColor = Color.White) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                if (item.route == Screen.Cart.route && totalCartCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = TealAccent, contentColor = Color.White) {
                                                Text("$totalCartCount")
                                            }
                                        }
                                    ) {
                                        Icon(item.icon, contentDescription = item.title)
                                    }
                                } else {
                                    Icon(item.icon, contentDescription = item.title)
                                }
                            },
                            label = { Text(item.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TealAccent,
                                selectedTextColor = TealAccent,
                                indicatorColor = Color(0xFFE6FFFA),
                                unselectedIconColor = NavyPrimary,
                                unselectedTextColor = NavyPrimary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onCategoryClick = { categoryId ->
                            navController.navigate(Screen.ProductsList.createRoute(categoryId = categoryId))
                        },
                        onProductClick = { productId ->
                            navController.navigate(Screen.ProductDetails.createRoute(productId))
                        },
                        onSearchSubmit = { query ->
                            navController.navigate(Screen.ProductsList.createRoute(query = query))
                        },
                        onViewAllCategories = {
                            navController.navigate(Screen.Categories.route)
                        },
                        snackbarHostState = snackbarHostState
                    )
                }

                composable(Screen.Categories.route) {
                    CategoriesScreen(
                        viewModel = categoriesViewModel,
                        onCategoryClick = { categoryId ->
                            navController.navigate(Screen.ProductsList.createRoute(categoryId = categoryId))
                        }
                    )
                }

                composable(
                    route = Screen.ProductsList.route,
                    arguments = listOf(
                        navArgument("categoryId") { type = NavType.StringType; nullable = true; defaultValue = "" },
                        navArgument("query") { type = NavType.StringType; nullable = true; defaultValue = "" }
                    )
                ) { backStackEntry ->
                    val categoryId = backStackEntry.arguments?.getString("categoryId")
                    val query = backStackEntry.arguments?.getString("query")
                    ProductsListScreen(
                        viewModel = productsViewModel,
                        categoryId = categoryId,
                        searchQuery = query,
                        onProductClick = { productId ->
                            navController.navigate(Screen.ProductDetails.createRoute(productId))
                        },
                        onBackClick = { navController.popBackStack() },
                        snackbarHostState = snackbarHostState
                    )
                }

                composable(
                    route = Screen.ProductDetails.route,
                    arguments = listOf(navArgument("productId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val productId = backStackEntry.arguments?.getString("productId") ?: ""
                    ProductDetailsScreen(
                        viewModel = productDetailsViewModel,
                        productId = productId,
                        onBackClick = { navController.popBackStack() },
                        snackbarHostState = snackbarHostState
                    )
                }

                composable(Screen.Cart.route) {
                    CartScreen(
                        viewModel = cartViewModel,
                        onCheckoutClick = {
                            navController.navigate(Screen.Checkout.route)
                        },
                        onContinueShoppingClick = {
                            navController.navigate(Screen.Home.route)
                        }
                    )
                }

                composable(Screen.Checkout.route) {
                    CheckoutScreen(
                        viewModel = checkoutViewModel,
                        onBackClick = { navController.popBackStack() },
                        onOrderSuccessClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
