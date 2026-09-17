package com.amaarasaad.store.data.api

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("categories")
    suspend fun getCategories(): Response<List<ApiCategory>>

    @GET("products")
    suspend fun getProducts(
        @Query("categoryId") categoryId: String? = null,
        @Query("search") search: String? = null,
        @Query("featured") featured: Boolean? = null,
        @Query("newArrivals") newArrivals: Boolean? = null
    ): Response<List<ApiProduct>>

    @GET("products/{id}")
    suspend fun getProductById(@Path("id") id: String): Response<ApiProduct>

    @POST("orders")
    suspend fun createOrder(@Body request: ApiOrderRequest): Response<ApiOrderResponse>

    @GET("orders/{id}")
    suspend fun getOrderById(@Path("id") id: String): Response<ApiOrderResponse>

    companion object {
        private const val DEFAULT_BASE_URL = "http://10.0.2.2:3000/api/"

        fun create(baseUrl: String = DEFAULT_BASE_URL): ApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(ApiService::class.java)
        }
    }
}
