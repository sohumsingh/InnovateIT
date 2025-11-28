package com.example.umbilotemplefrontend

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.umbilotemplefrontend.utils.SettingsManager

class UmbiloApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Apply persisted theme mode at process start so UI matches user setting
        val settings = SettingsManager(this)
        AppCompatDelegate.setDefaultNightMode(
            if (settings.isDarkMode()) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}


