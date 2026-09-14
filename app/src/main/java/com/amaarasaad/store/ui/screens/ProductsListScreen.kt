package com.amaarasaad.store.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amaarasaad.store.ui.components.EmptyView
import com.amaarasaad.store.ui.components.ErrorView
import com.amaarasaad.store.ui.components.LoadingView
import com.amaarasaad.store.ui.components.ProductCard
import com.amaarasaad.store.ui.theme.NavyPrimary
import com.amaarasaad.store.ui.viewmodel.ProductsViewModel
import com.amaarasaad.store.ui.viewmodel.UiState
import kotlinx.coroutines.launch

@Composable
fun ProductsListScreen(
    viewModel: ProductsViewModel,
    categoryId: String?,
    searchQuery: String?,
    onProductClick: (String) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(categoryId, searchQuery) {
        viewModel.loadProducts(categoryId = categoryId, searchQuery = searchQuery)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "رجوع",
                    tint = NavyPrimary
                )
            }
            Text(
                text = when {
                    !searchQuery.isNullOrBlank() -> "نتائج البحث عن: \"$searchQuery\""
                    else -> "قائمة المنتجات"
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = NavyPrimary
            )
        }

        when (val state = uiState) {
            is UiState.Loading -> LoadingView()
            is UiState.Error -> ErrorView(
                message = state.message,
                onRetry = { viewModel.loadProducts(categoryId, searchQuery) }
            )
            is UiState.Success -> {
                val products = state.data
                if (products.isEmpty()) {
                    EmptyView(
                        message = "لا توجد منتجات مطابقة لطلبك",
                        icon = Icons.Default.SearchOff
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(products) { product ->
                            ProductCard(
                                product = product,
                                onProductClick = { onProductClick(product.id) },
                                onAddToCartClick = {
                                    viewModel.addToCart(product)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("تمت إضافة \"${product.nameAr}\" إلى السلة")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
