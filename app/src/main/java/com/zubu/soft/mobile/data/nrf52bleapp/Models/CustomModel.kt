package com.zubu.soft.mobile.data.nrf52bleapp.Models

import android.bluetooth.le.ScanResult
import android.content.Context
import com.zubu.soft.mobile.data.nrf52bleapp.Util.GattConnection
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.math.RoundingMode

class CustomModel(var data: String?, var scanResult: ScanResult?) {

    var gattConnection: GattConnection? = null

    override fun equals(other: Any?): Boolean {
        return data == other
    }

    override fun hashCode(): Int {
        var result = data?.hashCode() ?: 0
        result = 31 * result + (scanResult?.hashCode() ?: 0)
        return result
    }
}