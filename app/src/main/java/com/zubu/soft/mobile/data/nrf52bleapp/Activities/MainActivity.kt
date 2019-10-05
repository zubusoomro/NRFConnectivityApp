package com.zubu.soft.mobile.data.nrf52bleapp.Activities

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanResult
import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zubu.soft.mobile.data.nrf52bleapp.Adapter.DevicesAdapter
import com.zubu.soft.mobile.data.nrf52bleapp.Interface.DeviceClickListener
import com.zubu.soft.mobile.data.nrf52bleapp.Models.AppContextProvider
import com.zubu.soft.mobile.data.nrf52bleapp.Models.AppPreferences
import com.zubu.soft.mobile.data.nrf52bleapp.Models.SensorModel
import com.zubu.soft.mobile.data.nrf52bleapp.R
import com.zubu.soft.mobile.data.nrf52bleapp.Services.GattConnectionService
import com.zubu.soft.mobile.data.nrf52bleapp.Services.ScanningService
import com.zubu.soft.mobile.data.nrf52bleapp.Util.PermissionsHelper
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.longToast
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList

private const val REQUEST_CODE_BLUETOOTH_ENABLE = 1

class MainActivity : AppCompatActivity() {
    lateinit var adapter: DevicesAdapter
    var connectionService: GattConnectionService? = null
    var isBinded = false
    var scanningService: ScanningService? = null
    var scanningFirstTime = true
    private var localList = ArrayList<SensorModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
        AppContextProvider.appContext = this@MainActivity
        AppPreferences.createNotificationChannel(this@MainActivity)
    }

    private fun bindRunningService() {
        if (GattConnectionService.isServiceRunning) {
            bindService(
                Intent(this, GattConnectionService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    @Synchronized
    private fun notifyAdapterForChange(data: MutableLiveData<ArrayList<SensorModel>>) {
        data.observe(this, Observer { liveList ->
            try {
                if (localList.size != 0) {
                    var iterator = liveList.iterator()
                    if (iterator.hasNext()) {
                        do {
                            val liveModel = iterator.next()
                            var exists = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                localList.replaceAll {
                                    if (it == liveModel) liveModel
                                    else it
                                }
                            } else {
                                for (localModel in localList) {
                                    if (localModel == liveModel) {
                                        exists = true
                                        localModel.updateModel(liveModel)
                                    } else {
                                        exists = false
                                    }
                                }
                                if (!exists)
                                    localList.add(liveModel)
                            }
                        } while (iterator.hasNext())
                    }
                } else
                    localList.addAll(liveList)
                adapter.notifyDataSetChanged()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        })

    }

    private fun setAdapter(list: MutableLiveData<Map<String, ScanResult>>?) {
        try {
            list?.observe(this,
                Observer<Map<String, ScanResult>> { serviceList ->
                    removeExtraDevices(serviceList)
                    for ((key, value) in serviceList) {
                        val modl = SensorModel(key, value)
                        localList.add(modl)
                    }
                    localList.sortWith(Comparator { o1, o2 ->
                        if (!o1.name.isNullOrEmpty()) {
                            if (!o2.name.isNullOrEmpty()) {
                                (o1.name!!).compareTo((o2.name!!))
                            } else {
                                //o2 empty or blank
                                (o1.name!!).compareTo((""))
                            }
                        } else {
                            //o1 empty or blank
                            ("").compareTo((o2.name!!))
                        }
                    })
                    adapter.notifyDataSetChanged()
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeExtraDevices(list: Map<String, ScanResult>?) {
        try {
            for (customModels in localList) {
                var found = false
                if (!customModels.gattSet) {
                    if (list != null) {
                        for ((key) in list) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                localList.removeIf {
                                    found = false
                                    it.name.equals(key, true)
                                }
                            } else {
                                if (!customModels.name.equals(key, true)) {
                                    found = false
                                } else {
                                    found = true
                                    break
                                }
                            }

                        }
                    }
                    if (!found) {
                        localList.remove(customModels)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun init() {
        rv_devices.layoutManager =
            LinearLayoutManager(this@MainActivity, RecyclerView.VERTICAL, false)
        adapter =
            DevicesAdapter(
                WeakReference(this@MainActivity),
                localList, listener
            )
        rv_devices.adapter = adapter
        btn_Scan.setOnClickListener {

            if (ScanningService.isScanning) {
                stopScanningService()
                (it as Button).text = "Start Scanning"
                scanningFirstTime = true
            } else {
                startScanningService()
                (it as Button).text = "Stop Scanning"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mBluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        if (!isBinded)
            bindRunningService()
    }

    override fun onPause() {
        super.onPause()
        try {
            if (ScanningService.isScanning) {
                stopScanningService()

            }
            unregisterReceiver(mBluetoothReceiver)

            if (isBinded)
                unbindService(serviceConnection)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scanningService?.onDestroy()
    }

    private val listener = object : DeviceClickListener {
        override fun onDisconnect(deviceAddress: String) {
            if (BluetoothAdapter.getDefaultAdapter().isEnabled) {
                if (connectionService != null) {
                    (connectionService as DeviceClickListener).onDisconnect(deviceAddress)
                }
            }
        }

        override fun onConnect(model: SensorModel) {
            if (BluetoothAdapter.getDefaultAdapter().isEnabled) {
                if (GattConnectionService.isServiceRunning) {
                    if (connectionService != null) {
                        (connectionService as DeviceClickListener).onConnect(model)
                    }
                } else {
                    Intent(this@MainActivity, GattConnectionService::class.java).let {
                        bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
                        startService(it)
                    }
                    Handler().postDelayed({
                        onConnect(model)
                    }, 500)
                }
            }
        }

    }

    private fun stopScanningService() {
        try {
            scanningService?.stopScannig()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun startScanningService() {
        if (PermissionsHelper.checkLocationPermission(this)) {
            if (PermissionsHelper.isBLEAvailable(this)) {
                if (PermissionsHelper.isBluetoothEnabled()) {
                    scanningService = ScanningService {
                        if (scanningFirstTime) {
                            setAdapter(scanningService?.getLiveDataDeviceList())
                        }
                        scanningFirstTime = false
                    }
                    scanningService?.startScanning()
                } else {
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).also {
                        startActivityForResult(
                            it,
                            REQUEST_CODE_BLUETOOTH_ENABLE
                        )
                    }
                }
            } else {
                longToast("Bluetooth LE not supported on your device!")
            }
        }
    }

    private val mBluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)) {
                BluetoothAdapter.STATE_OFF -> {
                    stopScanningService()
                    btn_Scan.text = "Start Scan"
                }
                else -> {
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_BLUETOOTH_ENABLE && resultCode == Activity.RESULT_OK) {
            startScanningService()
        } else {
            longToast("Please turn on the bluetooth to continue")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        PermissionsHelper.onRequestPermissionsResult(
            this,
            requestCode,
            grantResults
        ) {
            startScanningService()
        }
    }

    private var serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as GattConnectionService.MyLocalBinder
            connectionService = binder.getService()
            notifyAdapterForChange(binder.getData())
            deleteDevice(binder.deleteDevice())
            isBinded = true
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isBinded = false
            connectionService = null
        }

    }

    private fun deleteDevice(deleteDevice: MutableLiveData<SensorModel>) {
        deleteDevice.observe(this, Observer {
            val iterator = localList.iterator()
            if (iterator.hasNext()) {
                do {
                    if (iterator.next() == it) {
                        iterator.remove()
                    }
                } while (iterator.hasNext())
            }
//            for (model in localList) {
//                if (model == it) {
//                    localList.remove(model)
//                }
//            }
        })
    }


}
