package com.zubu.soft.mobile.data.nrf52bleapp.Services

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.zubu.soft.mobile.data.nrf52bleapp.Interface.DeviceClickListener
import com.zubu.soft.mobile.data.nrf52bleapp.Interface.onModelChanged
import com.zubu.soft.mobile.data.nrf52bleapp.Models.SensorModel
import com.zubu.soft.mobile.data.nrf52bleapp.R
import com.zubu.soft.mobile.data.nrf52bleapp.Util.GattConnection
import org.parceler.Parcel
import org.parceler.Parcels
import java.lang.ref.WeakReference

@Parcel
class GattConnectionService : Service(), DeviceClickListener, onModelChanged {
    companion object {
        var isServiceRunning = false
        const val CHANNEl_ID = "beaconServiceChannel"
    }

    private val liveDataModel: MutableLiveData<SensorModel> = MutableLiveData()
    private val list = ArrayList<SensorModel>()

    private var liveDataDeviceList: MutableLiveData<ArrayList<SensorModel>> = MutableLiveData()
    private val binder: MyLocalBinder = MyLocalBinder()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP_ACTION) {
            isServiceRunning = false
            stopSelf()
            stopForeground(true)
        } else {
            isServiceRunning = true
            getPermanentBLENotification()
            if (intent?.hasExtra("sensor_model")!!) {
                startDataListening(Parcels.unwrap(intent.getParcelableExtra("sensor_model")))
            }
        }
        return START_STICKY
    }

    private fun startDataListening(parcelableExtra: SensorModel) {
        try {
            var connection = GattConnection(WeakReference(this), parcelableExtra, this)
            connection.onCreate()
            parcelableExtra.gattConnection = connection
            list.add(parcelableExtra)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onConnect(model: SensorModel) {
        startDataListening(model)
    }

    override fun onDisconnect(deviceAddress: String) {
        for (model in list)
            if (model.getSensorAddress() == deviceAddress) {
                model.gattConnection?.onDestroy()
                liveDataModel.postValue(model)
                list.remove(model)
            }
        liveDataDeviceList.postValue(list)
    }

    override fun modelChanged(model: SensorModel) {
        for ((index, localModel) in list.withIndex()) {
            if (localModel == model && model.gattSet) {
                list.removeAt(index)
                list.add(model)
                break
            }
        }
        liveDataDeviceList.postValue(list)
    }

    private fun getPermanentBLENotification() {
        val stopIntent =
            Intent(this@GattConnectionService, GattConnectionService::class.java).apply {
                action = STOP_ACTION
            }
        val stopSelfIntent =
            PendingIntent.getService(
                this@GattConnectionService, 0,
                stopIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )
        val notification = NotificationCompat.Builder(
            this,
            CHANNEl_ID
        ).apply {
            setContentTitle("Searching...")
            setContentText("Searching for BLE Devices")
            setSmallIcon(R.drawable.ic_bluetooth_searching)
            addAction(R.drawable.ic_stop_black_24dp, "Stop Searching", stopSelfIntent)
        }.build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        for (model in list)
            model.gattConnection?.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return binder
    }

    inner class MyLocalBinder : Binder() {

        fun getService(): GattConnectionService {
            return this@GattConnectionService
        }

        fun getData(): MutableLiveData<ArrayList<SensorModel>> {
            return liveDataDeviceList
        }

        fun deleteDevice(): MutableLiveData<SensorModel> {
            return liveDataModel
        }
    }
}