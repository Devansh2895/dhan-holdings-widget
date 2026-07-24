package com.devansh.dhanwidget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val tokenStore = TokenStore(this)
        val tokenField = findViewById<EditText>(R.id.tokenField)
        val amoledSwitch = findViewById<Switch>(R.id.amoledSwitch)
        tokenField.setText(tokenStore.accessToken)
        amoledSwitch.isChecked = tokenStore.amoledTheme

        findViewById<Button>(R.id.getTokenButton).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://web.dhan.co/index/profile")))
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            tokenStore.accessToken = tokenField.text.toString().trim()
            tokenStore.amoledTheme = amoledSwitch.isChecked
            WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<RefreshWorker>().build())
            Toast.makeText(this, "Saved. Widget refreshing…", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
