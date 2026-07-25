package com.devansh.dhanwidget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.tabs.TabLayout

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val tokenStore = TokenStore(this)
        val clientIdField = findViewById<EditText>(R.id.clientIdField)
        val appIdField = findViewById<EditText>(R.id.appIdField)
        val appSecretField = findViewById<EditText>(R.id.appSecretField)
        val tokenField = findViewById<EditText>(R.id.tokenField)
        val amoledSwitch = findViewById<Switch>(R.id.amoledSwitch)
        clientIdField.setText(tokenStore.clientId)
        appIdField.setText(tokenStore.appId)
        appSecretField.setText(tokenStore.appSecret)
        tokenField.setText(tokenStore.accessToken)
        amoledSwitch.isChecked = tokenStore.amoledTheme

        amoledSwitch.setOnCheckedChangeListener { _, isChecked ->
            tokenStore.amoledTheme = isChecked
            WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<RefreshWorker>().build())
        }

        val loginTabContent = findViewById<View>(R.id.loginTabContent)
        val manualTabContent = findViewById<View>(R.id.manualTabContent)
        findViewById<TabLayout>(R.id.authTabs).addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                loginTabContent.visibility = if (tab.position == 0) View.VISIBLE else View.GONE
                manualTabContent.visibility = if (tab.position == 0) View.GONE else View.VISIBLE
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        findViewById<Button>(R.id.getTokenButton).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://web.dhan.co/index/profile")))
        }

        findViewById<Button>(R.id.loginButton).setOnClickListener {
            tokenStore.clientId = clientIdField.text.toString().trim()
            tokenStore.appId = appIdField.text.toString().trim()
            tokenStore.appSecret = appSecretField.text.toString().trim()
            startActivity(Intent(this, DhanLoginActivity::class.java))
        }

        findViewById<Button>(R.id.saveTokenButton).setOnClickListener {
            tokenStore.accessToken = tokenField.text.toString().trim()
            WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<RefreshWorker>().build())
            Toast.makeText(this, "Saved. Widget refreshing…", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
