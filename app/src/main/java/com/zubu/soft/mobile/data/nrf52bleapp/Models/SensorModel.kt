package com.zubu.soft.mobile.data.nrf52bleapp.Models

import android.bluetooth.le.ScanResult
import android.os.Parcelable
import com.zubu.soft.mobile.data.nrf52bleapp.Util.GattConnection
import org.parceler.Parcel

@Parcel
class SensorModel(
    var name: String?,
    var scanResult: ScanResult?
) {

    var sensorData: String? = null
    var gattSet = false
    var gattSetError = false
    var statusCode = 0

    var gattConnection: GattConnection? = null

    fun updateModel(model: SensorModel) {
        sensorData = model.sensorData
        gattSetError = model.gattSetError
        gattSet = model.gattSet
        gattConnection = model.gattConnection
    }

    fun getSensorName(): String {
        return scanResult?.device?.name.toString()
    }

    fun getSensorAddress(): String {
        return scanResult?.device?.address.toString()
    }

    override fun equals(other: Any?): Boolean {
        other as SensorModel
        return getSensorName() == other.getSensorName() && getSensorAddress() == other.getSensorAddress()
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (scanResult?.hashCode() ?: 0)
        result = 31 * result + (sensorData?.hashCode() ?: 0)
        result = 31 * result + gattSet.hashCode()
        result = 31 * result + gattSetError.hashCode()
        result = 31 * result + statusCode
        result = 31 * result + (gattConnection?.hashCode() ?: 0)
        return result
    }

}