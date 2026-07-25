package com.devansh.dhanwidget

import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

const val DHAN_LOGIN_REDIRECT_URL = "https://dhanholdingswidget.app/callback"

// Response schemas undocumented by Dhan — parsed defensively in DhanLoginActivity.
interface DhanAuthApi {
    @POST("app/generate-consent")
    suspend fun generateConsent(
        @Query("client_id") clientId: String,
        @Header("app_id") appId: String,
        @Header("app_secret") appSecret: String,
    ): ResponseBody

    @POST("app/consumeApp-consent")
    suspend fun consumeConsent(
        @Query("tokenId") tokenId: String,
        @Header("app_id") appId: String,
        @Header("app_secret") appSecret: String,
    ): ResponseBody
}

object DhanAuthApiFactory {
    fun create(): DhanAuthApi =
        Retrofit.Builder()
            .baseUrl("https://auth.dhan.co/")
            .build()
            .create(DhanAuthApi::class.java)
}
