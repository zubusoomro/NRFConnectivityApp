package com.zubu.soft.mobile.data.nrf52bleapp.Activities

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanResult
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zubu.soft.mobile.data.nrf52bleapp.Adapter.DevicesAdapter
import com.zubu.soft.mobile.data.nrf52bleapp.Models.AppContextProvider
import com.zubu.soft.mobile.data.nrf52bleapp.Models.AppPreferences
import com.zubu.soft.mobile.data.nrf52bleapp.Models.CustomModel
import com.zubu.soft.mobile.data.nrf52bleapp.R
import com.zubu.soft.mobile.data.nrf52bleapp.Services.BeaconStickyService
import com.zubu.soft.mobile.data.nrf52bleapp.Services.STOP_ACTION
import com.zubu.soft.mobile.data.nrf52bleapp.Util.PermissionsHelper
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.longToast
import java.lang.ref.WeakReference
import java.util.*

private const val REQUEST_CODE_BLUETOOTH_ENABLE = 1

class MainActivity : AppCompatActivity() {
    lateinit var adapter: DevicesAdapter
    var scanService: BeaconStickyService? = null
    var isBinded = false
    private var localList = ArrayList<CustomModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    private fun bindRunningService() {
        if (BeaconStickyService.isServiceRunning) {
            bindService(Intent(this, BeaconStickyService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
            btn_Scan.text = "Stop Scanning"
        } else {
            btn_Scan.text = "Start Scanning"

        }
    }

    private fun setAdapter(list: MutableLiveData<Map<String, ScanResult>>?) {
        try {
            list?.observe(this,
                Observer<Map<String, ScanResult>> { serviceList ->
                    removeExtraDevices(serviceList)
                    for ((key, value) in serviceList) {
                        val modl = CustomModel(key, value)
                        localList.add(modl)
                    }
                    localList.sortWith(Comparator { o1, o2 ->
                        (o1.scanResult?.rssi!!).compareTo((o2.scanResult?.rssi!!))
                    })
                    adapter.notifyDataSetChanged()
                })
//            if (list?.value == null || list.value?.isEmpty()!!) {
//                removeExtraDevices(null)
//            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeExtraDevices(list: Map<String, ScanResult>?) {
        try {
            for ((index, customModels) in localList.withIndex()) {
                if (customModels.gattConnection != null) continue
                var found = false
                if (list != null) {
                    for ((key) in list) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            localList.removeIf {
                                it.data.equals(key, true)
                            }
                        } else {
                            if (!customModels.data.equals(key, true)) {
                                found = false
                            } else {
                                found = true
                                break
                            }
                        }

                    }
                }
                if (customModels.gattConnection == null && localList.size - 1 >= index && !found) {
                    localList.removeAt(index)
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun init() {
        rv_devices.layoutManager = LinearLayoutManager(this@MainActivity, RecyclerView.VERTICAL, false)
        adapter =
            DevicesAdapter(
                WeakReference<Context>(this@MainActivity),
                localList
            )
        rv_devices.adapter = adapter
        btn_Scan.setOnClickListener {

            if (BeaconStickyService.isServiceRunning) {
                stopBeaconService()
                (it as Button).text = "Start Scanning"

            } else {
                AppContextProvider.appContext = this@MainActivity
                AppPreferences.createNotificationChannel(this@MainActivity)
                startBeaconService()
                (it as Button).text = "Stop Scanning"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isBinded)
            bindRunningService()
    }

    override fun onPause() {
        super.onPause()
        try {
            if (isBinded)
                unbindService(serviceConnection)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopBeaconService() {
        try {
            unbindService(serviceConnection)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        Intent(this, BeaconStickyService::class.java).let {
            it.action = STOP_ACTION
            startService(it)
        }

    }

    private fun startBeaconService() {
        if (PermissionsHelper.checkLocationPermission(this)) {
            if (PermissionsHelper.isBLEAvailable(this)) {
                if (PermissionsHelper.isBluetoothEnabled()) {
                    Intent(this, BeaconStickyService::class.java).let {
                        bindService(it, serviceConnection, Context.BIND_AUTO_CREATE)
                        startService(it)
                    }

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_BLUETOOTH_ENABLE && resultCode == Activity.RESULT_OK) {
            startBeaconService()
        } else {
            longToast("Please turn on the bluetooth to continue")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        PermissionsHelper.onRequestPermissionsResult(
            this,
            requestCode,
            permissions,
            grantResults
        ) {
            startBeaconService()
        }
    }

    private var serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as BeaconStickyService.MyLocalBinder
            setAdapter(p1.getScanList())
            scanService = binder.getService()
            isBinded = true
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isBinded = false
        }

    }
}
