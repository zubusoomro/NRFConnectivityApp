package com.zubu.soft.mobile.data.nrf52bleapp.Services

import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.ParcelUuid
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.zubu.soft.mobile.data.nrf52bleapp.Util.CustomRunnable
import com.zubu.soft.mobile.data.nrf52bleapp.Interface.DeviceDeleteCallBack
import com.zubu.soft.mobile.data.nrf52bleapp.R
import com.zubu.soft.mobile.data.nrf52bleapp.Models.UUIDS
import java.util.*
import kotlin.collections.ArrayList

class BeaconStickyService : Service(), DeviceDeleteCallBack {

    companion object {
        var isServiceRunning = false
        val CHANNEl_ID = "beaconServiceChannel"

    }

    override fun onDeviceDelete(key: String) {
        list.remove(key)
        getLiveDataDeviceList()?.postValue(list)
    }


    private var scanCallback: ScanCallback? = null
    private var settings: ScanSettings? = null
    private val binder: MyLocalBinder = MyLocalBinder()
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var liveDataDeviceList: MutableLiveData<Map<String, ScanResult>>? = null
    private val list = HashMap<String, ScanResult>()
    private var pendingRunnables = HashMap<String, CustomRunnable?>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP_ACTION) {
            isServiceRunning = false
            stopSelf()
            stopForeground(true)
            stopScannig()
        } else {
            isServiceRunning = true
            getPermanentBLENotification()
            startScanning()
        }
        return START_STICKY
    }

    private fun stopScannig() {
        if (bluetoothLeScanner != null)
            bluetoothLeScanner?.stopScan(scanCallback)
    }

    private fun startScanning() {
        if (bluetoothLeScanner == null)
            bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner

        bluetoothLeScanner?.startScan(getScanFilter(), getScanSettings(), getScanCallBack())
    }

    private fun getScanFilter(): List<ScanFilter> {
        var filter = ArrayList<ScanFilter>()
//        filter.add(ScanFilter.Builder().setServiceUuid(ParcelUuid(UUIDS.SERVICE_UUID)).build())
        return filter
    }

    fun getLiveDataDeviceList(): MutableLiveData<Map<String, ScanResult>>? {
        if (liveDataDeviceList == null)
            liveDataDeviceList = MutableLiveData()

        return liveDataDeviceList
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
                            result.scanRecord!!.deviceName != null -> result.scanRecord!!.deviceName
                            result.device.name != null -> result.device.name
                            result.scanRecord?.serviceUuids != null && result.scanRecord?.serviceUuids!!.size > 0 -> result.scanRecord?.serviceUuids!![0].uuid.toString()
                            else -> null
                        }
                        s?.let {
                            if (!list.containsKey(it)) {
                                list[s] = result
                                getLiveDataDeviceList()?.postValue(list)
                            }
                            setDeviceRomalLogic(it)
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
                                result.scanRecord!!.deviceName != null -> result.scanRecord!!.deviceName
                                result.device.name != null -> result.device.name
                                result.scanRecord?.serviceUuids != null && result.scanRecord?.serviceUuids!!.size > 0 -> result.scanRecord?.serviceUuids!![0].uuid.toString()
                                else -> null
                            }
                            s?.let {
                                if (!list.containsKey(it)) {
                                    list[s] = result
                                    getLiveDataDeviceList()?.postValue(list)
                                }
                                setDeviceRomalLogic(it)
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
            runnable = CustomRunnable(s, this)
            runnable.setPostDelayedHandler()
        }
        pendingRunnables[s] = runnable
    }

    private fun getPermanentBLENotification() {
        val stopIntent =
            Intent(this@BeaconStickyService, BeaconStickyService::class.java).apply {
                action = STOP_ACTION
            }
        val stopSelfIntent =
            PendingIntent.getService(
                this@BeaconStickyService, 0,
                stopIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )
        val notification = NotificationCompat.Builder(this,
            CHANNEl_ID
        ).apply {
            setContentTitle("Searching...")
            setContentText("Searching for BLE Devices")
            setSmallIcon(R.drawable.ic_bluetooth_searching)
            addAction(R.drawable.ic_stop_black_24dp, "Stop Searching", stopSelfIntent)
        }.build()
        startForeground(1, notification)
    }

    override fun onBind(p0: Intent?): IBinder? {
        return binder
    }

    inner class MyLocalBinder : Binder() {
        fun getService(): BeaconStickyService {
            return this@BeaconStickyService
        }

        fun getScanList(): MutableLiveData<Map<String, ScanResult>>? {
            return getLiveDataDeviceList()
        }
    }
}

const val STOP_ACTION = "stop_service"