package com.zubu.soft.mobile.data.nrf52bleapp.Services

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.os.ParcelUuid
import androidx.lifecycle.MutableLiveData
import com.zubu.soft.mobile.data.nrf52bleapp.Interface.DeviceDeleteCallBack
import com.zubu.soft.mobile.data.nrf52bleapp.Models.UUIDS
import com.zubu.soft.mobile.data.nrf52bleapp.Util.CustomRunnable
import java.util.*
import kotlin.collections.HashMap

class ScanningService(var function: () -> Unit) : DeviceDeleteCallBack {


    companion object {
        var isServiceRunning = false
        var isScanning = false
    }

    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private var settings: ScanSettings? = null
    private var liveDataDeviceList: MutableLiveData<Map<String, ScanResult>>? = null
    private var list: HashMap<String, ScanResult>? = HashMap()
    private var pendingRunnables = HashMap<String, CustomRunnable?>()

    fun onDestroy() {
        stopScannig()
        bluetoothLeScanner = null
        scanCallback = null
        settings = null
        liveDataDeviceList = null
        list = null
    }

    fun stopScannig() {
        if (bluetoothLeScanner != null)
            bluetoothLeScanner?.stopScan(scanCallback)

        isScanning = false
    }

    fun startScanning() {
        if (bluetoothLeScanner == null)
            bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner

        bluetoothLeScanner?.startScan(getScanFilter(), getScanSettings(), getScanCallBack())
        isScanning = true
    }

    private fun getScanFilter(): List<ScanFilter> {
        var filter = ArrayList<ScanFilter>()
        filter.add(ScanFilter.Builder().setServiceUuid(ParcelUuid(UUIDS.SERVICE_UUID)).build())
        return filter
    }

    fun getLiveDataDeviceList(): MutableLiveData<Map<String, ScanResult>>? {
        if (liveDataDeviceList == null)
            liveDataDeviceList = MutableLiveData()

        return liveDataDeviceList
    }

    override fun onDeviceDelete(key: String) {
        list?.remove(key)
        getLiveDataDeviceList()?.postValue(list)
    }

    private fun getScanCallBack(): ScanCallback? {
        if (scanCallback == null)
            scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    super.onScanResult(callbackType, result)
                    if (result == null)
                        return
                    try {
                        val s = when {
                            result.device.name != null -> result.device.name
                            result.device.address != null -> result.device.address
                            else -> null
                        }
                        s?.let {
                            if (!list?.containsKey(it)!!) {
                                list!![s] = result
                                getLiveDataDeviceList()?.postValue(list)
                            }
                            setDeviceRomalLogic(it)
                            function.invoke()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onBatchScanResults(results: List<ScanResult>) {
                    super.onBatchScanResults(results)
                    for (result in results) {
                        try {
                            val s = when {
                                result.device.name != null -> result.device.name
                                result.device.address != null -> result.device.address
                                else -> null
                            }
                            s?.let {
                                if (!list?.containsKey(it)!!) {
                                    list!![s] = result
                                    getLiveDataDeviceList()?.postValue(list)
                                }
                                setDeviceRomalLogic(it)
                                function.invoke()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    super.onScanFailed(errorCode)
                }
            }
        return scanCallback
    }

    private fun getScanSettings(): ScanSettings? {
        if (settings == null)
            settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//                .setReportDelay(500)
                .build()

        return settings
    }

    private fun setDeviceRomalLogic(s: String) {
        val runnable: CustomRunnable?
        if (pendingRunnables[s] != null) {
            runnable = pendingRunnables[s]
            runnable?.setPostDelayedHandler()
        } else {
            runnable = CustomRunnable(s, this@ScanningService)
            runnable.setPostDelayedHandler()
        }
        pendingRunnables[s] = runnable
    }
}