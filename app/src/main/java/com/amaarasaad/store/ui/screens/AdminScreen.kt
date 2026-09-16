package com.amaarasaad.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amaarasaad.store.data.model.Order
import com.amaarasaad.store.data.model.OrderStatus
import com.amaarasaad.store.data.model.Product
import com.amaarasaad.store.ui.components.EmptyView
import com.amaarasaad.store.ui.components.formatPrice
import com.amaarasaad.store.ui.theme.DangerRed
import com.amaarasaad.store.ui.theme.NavyPrimary
import com.amaarasaad.store.ui.theme.TealAccent
import com.amaarasaad.store.ui.theme.TextSecondary
import com.amaarasaad.store.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminScreen(
    viewModel: AdminViewModel,
    onBackClick: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("طلبات المتجر", "إدارة المنتجات")

    val orders by viewModel.orders.collectAsState()
    val products by viewModel.products.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                text = "لوحة إدارة متجر أبو هاشم",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = NavyPrimary
            )
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.White,
            contentColor = TealAccent,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = TealAccent
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> AdminOrdersList(
                orders = orders,
                onUpdateStatus = { orderId, status -> viewModel.updateOrderStatus(orderId, status) }
            )
            1 -> AdminProductsList(
                products = products,
                onUpdateStock = { id, stock -> viewModel.updateProductStock(id, stock) },
                onDelete = { id -> viewModel.deleteProduct(id) }
            )
        }
    }
}

@Composable
fun AdminOrdersList(
    orders: List<Order>,
    onUpdateStatus: (String, String) -> Unit
) {
    if (orders.isEmpty()) {
        EmptyView(message = "لا توجد طلبات مستلمة حالياً", icon = Icons.Default.Receipt)
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(orders, key = { it.id }) { order ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "طلب #${order.id}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = NavyPrimary
                            )
                            val statusTitle = OrderStatus.entries.find { it.name == order.status }?.titleAr ?: order.status
                            Box(
                                modifier = Modifier
                                    .background(TealAccent, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = statusTitle,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("👤 الزبون: ${order.customerInfo.fullName}", fontSize = 14.sp)
                        Text("📞 الهاتف: ${order.customerInfo.phoneNumber}", fontSize = 14.sp)
                        Text("📍 العنوان: ${order.customerInfo.city} - ${order.customerInfo.address}", fontSize = 14.sp)
                        Text("📌 نقطة دالة: ${order.customerInfo.nearestLandmark}", fontSize = 14.sp)

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("🛍️ المواد المطلوب توصيلها:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        order.items.forEach { item ->
                            Text(
                                text = "• ${item.product.nameAr} × ${item.quantity} (${item.product.unit}) - ${formatPrice(item.totalPriceIqd)} د.ع",
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "المجموع: ${formatPrice(order.totalAmountIqd)} د.ع (الدفع عند الاستلام)",
                            fontWeight = FontWeight.Bold,
                            color = TealAccent,
                            fontSize = 15.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("تغيير حالة الطلب:", fontSize = 12.sp, color = TextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OrderStatus.entries.forEach { status ->
                                OutlinedButton(
                                    onClick = { onUpdateStatus(order.id, status.name) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = status.titleAr,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminProductsList(
    products: List<Product>,
    onUpdateStock: (String, Int) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(products, key = { it.id }) { product ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(product.nameAr, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("السعر: ${formatPrice(product.priceIqd)} د.ع / ${product.unit}", fontSize = 12.sp, color = TextSecondary)
                        Text("المخزون الحالي: ${product.stockQuantity}", fontSize = 12.sp, color = TealAccent)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = { onUpdateStock(product.id, product.stockQuantity + 5) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                        ) {
                            Text("+5 stock", fontSize = 10.sp)
                        }

                        IconButton(onClick = { onDelete(product.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = DangerRed)
                        }
                    }
                }
            }
        }
    }
}
