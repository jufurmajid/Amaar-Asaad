package com.amaarasaad.store.data.service

import com.amaarasaad.store.data.model.Order
import com.amaarasaad.store.data.model.OrderStatus
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Service to send Telegram notifications securely using environment variables or configuration.
 * Prevents hardcoding secrets in source code.
 */
class TelegramService(
    private val botTokenOverride: String? = null,
    private val chatIdOverride: String? = null
) {
    private val botToken: String? get() = botTokenOverride ?: System.getenv("TELEGRAM_ORDER_BOT_TOKEN")
    private val chatId: String? get() = chatIdOverride ?: System.getenv("ADMIN_TELEGRAM_CHAT_ID")

    fun isConfigured(): Boolean = !botToken.isNullOrBlank() && !chatId.isNullOrBlank()

    suspend fun sendNewOrderNotification(order: Order): Result<Boolean> = withContext(Dispatchers.IO) {
        val token = botToken
        val id = chatId

        if (token.isNullOrBlank() || id.isNullOrBlank()) {
            println("TelegramService: Token or Chat ID not configured. Skipping live Telegram dispatch.")
            return@withContext Result.success(false)
        }

        try {
            val formattedMessage = formatNewOrderMessage(order)
            val success = sendTextMessage(token, id, formattedMessage)
            if (success) Result.success(true) else Result.failure(Exception("Telegram API returned non-200 response"))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun sendOrderStatusUpdateNotification(order: Order, oldStatus: String, newStatus: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val token = botToken
        val id = chatId

        if (token.isNullOrBlank() || id.isNullOrBlank()) {
            return@withContext Result.success(false)
        }

        try {
            val statusTitle = OrderStatus.entries.find { it.name == newStatus }?.titleAr ?: newStatus
            val message = """
                🔄 تحديث حالة الطلب #${order.id}

                👤 الزبون: ${order.customerInfo.fullName}
                📌 الحالة الجديدة: $statusTitle
            """.trimIndent()
            val success = sendTextMessage(token, id, message)
            if (success) Result.success(true) else Result.failure(Exception("Telegram API returned non-200 response"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatNewOrderMessage(order: Order): String {
        val numberFormat = NumberFormat.getNumberInstance(Locale("ar"))
        val totalFormatted = numberFormat.format(order.totalAmountIqd)
        val timeFormatted = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar")).format(Date(order.timestamp))

        val itemsFormatted = order.items.joinToString("\n\n") { item ->
            "• ${item.product.nameAr}\n  الكمية: ${item.quantity} (${item.product.unit})\n  السعر: ${numberFormat.format(item.totalPriceIqd)} د.ع"
        }

        return """
🛒 طلب جديد من متجر أبو هاشم

━━━━━━━━━━━━

🔢 رقم الطلب:
#${order.id}

👤 اسم الزبون:
${order.customerInfo.fullName}

📞 رقم الهاتف:
${order.customerInfo.phoneNumber}

🏙️ المدينة:
${order.customerInfo.city}

📍 العنوان:
${order.customerInfo.address}

📌 أقرب نقطة دالة:
${order.customerInfo.nearestLandmark.ifBlank { "غير محدد" }}

${if (order.customerInfo.notes.isNotBlank()) "📝 ملاحظات:\n${order.customerInfo.notes}\n" else ""}
━━━━━━━━━━━━

🛍️ المنتجات:

$itemsFormatted

━━━━━━━━━━━━

💰 المجموع:
$totalFormatted د.ع

💵 الدفع:
الدفع عند الاستلام

🕐 وقت الطلب:
$timeFormatted

━━━━━━━━━━━━
        """.trimIndent()
    }

    private fun sendTextMessage(token: String, targetChatId: String, text: String): Boolean {
        return try {
            val url = URL("https://api.telegram.org/bot$token/sendMessage")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.doOutput = true

            val jsonParam = JSONObject().apply {
                put("chat_id", targetChatId)
                put("text", text)
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(jsonParam.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            responseCode == 200
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
