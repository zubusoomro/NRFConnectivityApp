package com.zubu.soft.mobile.data.nrf52bleapp.Util

import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.util.Log
import com.zubu.soft.mobile.data.nrf52bleapp.Interface.onModelChanged
import com.zubu.soft.mobile.data.nrf52bleapp.Models.SensorModel
import com.zubu.soft.mobile.data.nrf52bleapp.Models.UUIDS
import java.lang.ref.WeakReference
import java.nio.ByteBuffer


class GattConnection(
    var mContext: WeakReference<Context>, var model: SensorModel, var listener: onModelChanged
) {
    private var mBluetoothAdapter: BluetoothAdapter? = null
    val TAG = "GATT CONNECTION"
    private var mGattCallback: BluetoothGattCallback? = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.i(TAG, "GATT CONNECTED")
                model.gattSetError = false

                gatt?.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.i(TAG, "GATT disconnected")
                model.gattSetError = true
                model.statusCode = status
                listener.modelChanged(model)
            }

        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                var connected = false
                gatt?.let {
                    if (it.getService(UUIDS.SERVICE_UUID) != null) {
                        val characteristic0x03Tx =
                            it.getService(UUIDS.SERVICE_UUID)?.getCharacteristic(
                                UUIDS.CHARACTERISTIC_UUID2
                            )

                        it.setCharacteristicNotification(characteristic0x03Tx, true)

                        characteristic0x03Tx?.getDescriptor(UUIDS.DESCRIPTOR_CONFIG_UUID)?.apply {
                            this.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            connected = it.writeDescriptor(this)
                        }
                        if (connected) {
                            model.gattSet = true
                            model.gattSetError = false
                            listener.modelChanged(model)
                        } else {
                            model.gattSet = false
                            model.gattSetError = true
                            model.statusCode = status
                            listener.modelChanged(model)
                        }
                    }
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if (UUIDS.DESCRIPTOR_CONFIG_UUID == descriptor?.uuid) {

                gatt?.let {
                    val characteristic0x02 =
                        it.getService(UUIDS.SERVICE_UUID)?.getCharacteristic(
                            UUIDS.CHARACTERISTIC_UUID1
                        )
                    characteristic0x02?.setValue("?")
                    var wrote = it.writeCharacteristic(characteristic0x02)
                    if (wrote) {
                        model.gattSet = true
                        model.gattSetError = false
                        listener.modelChanged(model)
                    }
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            val data = gatt
                ?.getService(UUIDS.SERVICE_UUID)
                ?.getCharacteristic(UUIDS.CHARACTERISTIC_UUID2)
            gatt?.readCharacteristic(data)

        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            readData(characteristic)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            readData(characteristic)
        }
    }


    private fun readData(characteristic: BluetoothGattCharacteristic?) {
        val data = characteristic?.value
        data?.let {
            var fNumber = ByteBuffer.wrap(it).float
            model.sensorData = fNumber.toString()
            listener.modelChanged(model)
            initTimer(fNumber)
        }
    }

    private fun initTimer(fNumber: Float) {
        var timer: Int = when {
            fNumber < 100 -> 10
            fNumber in 100.0..32000.0 -> 20
            fNumber > 32000 -> 30
            else -> 10
        }
        Handler().postDelayed({
            val characteristic0x02 =
                gattConn?.getService(UUIDS.SERVICE_UUID)?.getCharacteristic(
                    UUIDS.CHARACTERISTIC_UUID1
                )
            characteristic0x02?.setValue("?")
            gattConn?.writeCharacteristic(characteristic0x02)
        }, (timer * 60000).toLong())
    }

    private var gattConn: BluetoothGatt? = null


    fun onCreate() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!checkBluetoothSupport(mBluetoothAdapter)) {
            throw RuntimeException("GATT client requires Bluetooth support")
        }

        // Register for system Bluetooth events
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        mContext.get()?.registerReceiver(mBluetoothReceiver, filter)
        if (!mBluetoothAdapter?.isEnabled!!) {
            Log.w(TAG, "Bluetooth is currently disabled... enabling")
            mBluetoothAdapter?.enable()
        } else {
            Log.i(TAG, "Bluetooth enabled... starting client")
            startClient()
        }

    }

    fun onDestroy() {
        try {
            mGattCallback = null

            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter.isEnabled) {
                stopClient()
            }

            mContext.get()?.unregisterReceiver(mBluetoothReceiver)
            model.gattSet = false
            model.sensorData = ""
            model.gattSetError = false

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopClient() {
        if (gattConn != null) {
            gattConn?.disconnect()
            gattConn?.close()
            gattConn = null
        }

        if (mBluetoothAdapter != null) {
            mBluetoothAdapter = null
        }
    }

    private fun getDeviceAddress(): String {
        return model.scanResult?.device!!.address
    }

    private fun startClient() {
        val bluetoothDevice = mBluetoothAdapter?.getRemoteDevice(getDeviceAddress())
        gattConn = bluetoothDevice?.connectGatt(mContext.get(), false, mGattCallback)

        if (gattConn == null) {
            Log.w(TAG, "Unable to create GATT client")
            return
        }
    }


    private fun checkBluetoothSupport(bluetoothAdapter: BluetoothAdapter?): Boolean {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported")
            return false
        }

        if (!mContext.get()?.packageManager?.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)!!) {
            Log.w(TAG, "Bluetooth LE is not supported")
            return false
        }

        return true
    }

    private val mBluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)) {
                BluetoothAdapter.STATE_ON -> startClient()
                BluetoothAdapter.STATE_OFF -> stopClient()
                else -> {
                }
            }// Do nothing
        }
    }
}