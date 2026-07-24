package com.devansh.dhanwidget

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

// Unofficial, undocumented endpoint — no auth, but no SLA either. Can change/break without notice.
interface YahooFinanceApi {
    @GET("v8/finance/chart/{symbol}")
    suspend fun getChart(
        @Path("symbol") symbol: String,
        @Header("User-Agent") userAgent: String,
    ): YahooChartResponse
}

@Serializable
data class YahooChartResponse(val chart: YahooChart)

@Serializable
data class YahooChart(val result: List<YahooChartResult>? = null)

@Serializable
data class YahooChartResult(val meta: YahooChartMeta)

@Serializable
data class YahooChartMeta(
    val regularMarketPrice: Double,
    val previousClose: Double? = null,
    val chartPreviousClose: Double? = null,
) {
    val prevClose: Double get() = previousClose ?: chartPreviousClose ?: regularMarketPrice
}

object YahooFinanceApiFactory {
    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"

    fun create(): YahooFinanceApi {
        val json = Json { ignoreUnknownKeys = true }
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://query1.finance.yahoo.com/")
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(YahooFinanceApi::class.java)
    }
}
