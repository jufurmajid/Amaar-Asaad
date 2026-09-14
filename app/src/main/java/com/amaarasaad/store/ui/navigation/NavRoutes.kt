package com.amaarasaad.store.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Categories : Screen("categories")
    object ProductsList : Screen("products?categoryId={categoryId}&query={query}") {
        fun createRoute(categoryId: String? = null, query: String? = null): String {
            val catParam = categoryId ?: ""
            val queryParam = query ?: ""
            return "products?categoryId=$catParam&query=$queryParam"
        }
    }
    object ProductDetails : Screen("product/{productId}") {
        fun createRoute(productId: String) = "product/$productId"
    }
    object Cart : Screen("cart")
    object Checkout : Screen("checkout")
}
