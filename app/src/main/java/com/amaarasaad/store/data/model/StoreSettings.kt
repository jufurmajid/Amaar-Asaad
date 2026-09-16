package com.amaarasaad.store.data.model

data class DeliveryZone(
    val id: String,
    val nameAr: String,
    val deliveryFeeIqd: Long
)

data class StoreSettings(
    val storeName: String = "أبو هاشم للحوم والألبان والأجبان",
    val subtitle: String = "أفضل أنواع اللحوم البلدي والألبان والأجبان الطازجة",
    val phoneNumber: String = "+9647700000000",
    val address: String = "العراق - بغداد",
    val telegramUsername: String = "@AbuHashemStoreBot",
    val minimumOrderAmountIqd: Long = 5000,
    val defaultDeliveryFeeIqd: Long = 3000,
    val deliveryZones: List<DeliveryZone> = listOf(
        DeliveryZone("zone_1", "بغداد - الكرخ", 3000),
        DeliveryZone("zone_2", "بغداد - الرصافة", 3000),
        DeliveryZone("zone_3", "المحافظات", 6000)
    )
)
