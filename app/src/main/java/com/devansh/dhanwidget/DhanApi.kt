package com.devansh.dhanwidget

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header

interface DhanApi {

    @GET("holdings")
    suspend fun getHoldings(
        @Header("access-token") accessToken: String,
    ): List<HoldingDto>
}

@Serializable
data class HoldingDto(
    val tradingSymbol: String,
    val isin: String,
    val totalQty: Int,
    val avgCostPrice: Double,
)

object DhanApiFactory {
    fun create(): DhanApi {
        val json = Json { ignoreUnknownKeys = true }
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.dhan.co/v2/")
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(DhanApi::class.java)
    }
}
