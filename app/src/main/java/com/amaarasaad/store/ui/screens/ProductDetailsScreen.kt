package com.amaarasaad.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amaarasaad.store.ui.components.ErrorView
import com.amaarasaad.store.ui.components.LoadingView
import com.amaarasaad.store.ui.components.QuantitySelector
import com.amaarasaad.store.ui.components.formatPrice
import com.amaarasaad.store.ui.components.getCategoryIcon
import com.amaarasaad.store.ui.theme.NavyPrimary
import com.amaarasaad.store.ui.theme.TealAccent
import com.amaarasaad.store.ui.theme.TextSecondary
import com.amaarasaad.store.ui.viewmodel.ProductDetailsViewModel
import com.amaarasaad.store.ui.viewmodel.UiState
import kotlinx.coroutines.launch

@Composable
fun ProductDetailsScreen(
    viewModel: ProductDetailsViewModel,
    productId: String,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val quantity by viewModel.selectedQuantity.collectAsState()

    LaunchedEffect(productId) {
        viewModel.loadProductDetails(productId)
    }

    when (val state = uiState) {
        is UiState.Loading -> LoadingView()
        is UiState.Error -> ErrorView(message = state.message, onRetry = { viewModel.loadProductDetails(productId) })
        is UiState.Success -> {
            val product = state.data
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Header with back button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                        text = "تفاصيل المنتج",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = NavyPrimary
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    // Big Image Mock
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(product.categoryId),
                                contentDescription = null,
                                modifier = Modifier.size(96.dp),
                                tint = TealAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = product.nameAr,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = NavyPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = TealAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "الكمية المتوفرة: ${product.stockQuantity} قطعة",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "${formatPrice(product.priceIqd)} دينار عراقي",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealAccent
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "وصف المنتج",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NavyPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = product.descriptionAr,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Quantity Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "اختر الكمية المطلوبة:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = NavyPrimary
                        )

                        QuantitySelector(
                            quantity = quantity,
                            maxStock = product.stockQuantity,
                            onQuantityChange = { viewModel.updateQuantity(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Add to Cart Bottom Action Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.addToCart(product)
                            scope.launch {
                                snackbarHostState.showSnackbar("تمت إضافة $quantity من \"${product.nameAr}\" إلى السلة")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إضافة إلى السلة (${formatPrice(product.priceIqd * quantity)} د.ع)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
