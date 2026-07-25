package com.devansh.dhanwidget

import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class DhanLoginActivity : AppCompatActivity() {

    private lateinit var tokenStore: TokenStore
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dhan_login)
        tokenStore = TokenStore(this)
        progressBar = findViewById(R.id.progressBar)

        val clientId = tokenStore.clientId
        val appId = tokenStore.appId
        val appSecret = tokenStore.appSecret
        if (clientId.isNullOrBlank() || appId.isNullOrBlank() || appSecret.isNullOrBlank()) {
            Toast.makeText(this, "Set Client ID, App ID and App Secret first", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val webView = findViewById<WebView>(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                if (url.toString().startsWith(DHAN_LOGIN_REDIRECT_URL)) {
                    val tokenId = url.getQueryParameter("tokenId")
                    if (tokenId != null) consumeConsent(appId, appSecret, tokenId) else fail("Redirect missing tokenId")
                    return true
                }
                return false
            }

            override fun onPageFinished(view: WebView, url: String) {
                progressBar.visibility = ProgressBar.GONE
            }
        }

        lifecycleScope.launch {
            try {
                val body = DhanAuthApiFactory.create().generateConsent(clientId, appId, appSecret).string()
                val consentAppId = extractJsonField(body, "consentAppId", "consentId", "consent_id")
                    ?: return@launch fail("generate-consent response missing consent id, body=$body")
                webView.loadUrl("https://auth.dhan.co/login/consentApp-login?consentAppId=$consentAppId")
            } catch (e: Exception) {
                fail("generate-consent failed: ${e.message}")
            }
        }
    }

    private fun consumeConsent(appId: String, appSecret: String, tokenId: String) {
        lifecycleScope.launch {
            try {
                val body = DhanAuthApiFactory.create().consumeConsent(tokenId, appId, appSecret).string()
                val accessToken = extractJsonField(body, "accessToken", "access_token")
                    ?: return@launch fail("consumeApp-consent response missing token, body=$body")
                val dhanClientId = extractJsonField(body, "dhanClientId", "dhanClientld", "client_id")

                tokenStore.accessToken = accessToken
                if (dhanClientId != null) tokenStore.clientId = dhanClientId

                WorkManager.getInstance(this@DhanLoginActivity)
                    .enqueue(OneTimeWorkRequestBuilder<RefreshWorker>().build())
                Toast.makeText(this@DhanLoginActivity, "Logged in to Dhan", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                fail("consumeApp-consent failed: ${e.message}")
            }
        }
    }

    private fun fail(message: String) {
        android.util.Log.e("DhanLoginActivity", message)
        Toast.makeText(this, "Dhan login failed — check logs", Toast.LENGTH_LONG).show()
        finish()
    }
}

private fun extractJsonField(body: String, vararg candidates: String): String? =
    candidates.firstNotNullOfOrNull { field ->
        runCatching { Json.parseToJsonElement(body).jsonObject[field]?.jsonPrimitive?.content }.getOrNull()
    }
