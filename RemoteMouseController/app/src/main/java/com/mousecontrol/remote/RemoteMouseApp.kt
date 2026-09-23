package com.mousecontrol.remote

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

/** Forces dark mode for the whole app, independent of the system setting. */
class RemoteMouseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}
