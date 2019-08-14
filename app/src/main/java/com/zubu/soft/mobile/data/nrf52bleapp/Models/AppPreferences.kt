package com.zubu.soft.mobile.data.nrf52bleapp.Models

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.gb.prefsutil.PrefsUtil

object AppPreferences {
    val LOCATION_REQUEST = 121
    val REQUEST_BLUETOOTH = 1
    val CHANNEl_ID = "beaconServiceChannel"


    private val prefsUtil = PrefsUtil(AppContextProvider.appContext, "app_prefs")

    var locationPermissionSettled: Boolean by prefsUtil.delegate(
        PREF_LOCATION_PERMISSION_SETTLED, false
    )

    public fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var notificationChannel =
                NotificationChannel(CHANNEl_ID, "Beacon Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
            var notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}

private const val PREF_LOCATION_PERMISSION_SETTLED = "PREF_LOCATION_PERMISSION_SETTLED"