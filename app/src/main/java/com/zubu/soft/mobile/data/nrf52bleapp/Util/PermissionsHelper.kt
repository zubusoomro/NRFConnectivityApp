package com.zubu.soft.mobile.data.nrf52bleapp.Util

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.zubu.soft.mobile.data.nrf52bleapp.Models.AppPreferences
import org.jetbrains.anko.toast

object PermissionsHelper {

    fun checkLocationPermission(activity: Activity): Boolean {
        if (!hasLocationPermission(
                activity,
                getHighestRequiredPermission()
            ) && !AppPreferences.locationPermissionSettled
        ) {
            requestLocationPermission(activity)
            return false
        }
        return true
    }


    private fun hasLocationPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission(activity: Activity) {
        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
        ActivityCompat.requestPermissions(
            activity, permissions, REQUEST_CODE_LOCATION_PERMISSION
        )
    }

    private fun showLocationRationaleDialog(activity: Activity) {
        AlertDialog.Builder(activity).apply {
            setTitle("Caution!")
            setMessage("ALL TIME location permission is required for BLE advertisement discovering.\nNot choosing ALL TIME permission may cause unexpected behaviour!")
            setPositiveButton(android.R.string.ok) { _, _ ->
                requestLocationPermission(activity)
            }
        }.also {
            it.show()
        }
    }

    fun onRequestPermissionsResult(
        activity: Activity,
        requestCode: Int,
        grantResults: IntArray,
        onPermissionSettled: () -> Unit
    ) {
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                        || grantResults[1] == PackageManager.PERMISSION_GRANTED)
            ) {
                activity.toast("Location permission granted!")
                AppPreferences.locationPermissionSettled = true
                onPermissionSettled.invoke()
            } else {
                activity.toast("Full location permission denied!")
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        getHighestRequiredPermission()
                    )
                ) {
                    showLocationRationaleDialog(activity)
                } else {
                    AppPreferences.locationPermissionSettled = true
                    onPermissionSettled.invoke()
                }
            }
        }
    }

    private fun getHighestRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        } else {
            Manifest.permission.ACCESS_COARSE_LOCATION
        }
    }

    fun isBLEAvailable(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    fun isBluetoothEnabled(): Boolean = BluetoothAdapter.getDefaultAdapter().isEnabled
}

private const val REQUEST_CODE_LOCATION_PERMISSION = 121
