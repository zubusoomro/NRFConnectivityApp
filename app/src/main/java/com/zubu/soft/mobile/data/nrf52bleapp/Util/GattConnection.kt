package com.zubu.soft.mobile.data.nrf52bleapp.Util

import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import com.zubu.soft.mobile.data.nrf52bleapp.Interface.GattConnectivityCallback
import com.zubu.soft.mobile.data.nrf52bleapp.Models.UUIDS
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList


class GattConnection {
    private var mBluetoothAdapter: BluetoothAdapter? = null
    var context: WeakReference<Context>? = null
    var mdeviceAddress: String? = null
    var mListener: GattConnectivityCallback? = null
    private val descriptorUUIDsList: ArrayList<UUID> = ArrayList()
    private val characteristicUUIDsList: ArrayList<UUID> = ArrayList()
    private val serviceUUIDsList: ArrayList<UUID> = ArrayList()
    private var dataString = ArrayList<String>()
    val TAG = "GATT CONNECTION"
    private var mGattCallback: BluetoothGattCallback? = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.i(TAG, "GATT CONNECTED")

                gatt?.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.i(TAG, "GATT disconnected")
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
                            val characteristic0x02 =
                                it.getService(UUIDS.SERVICE_UUID)?.getCharacteristic(
                                    UUIDS.CHARACTERISTIC_UUID1
                                )
                            characteristic0x02?.value = "ABCD".toByteArray(Charsets.UTF_8)
                            it.writeCharacteristic(characteristic0x02)
                        }
                    }
                }
                mListener?.onConnected(connected)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if (UUIDS.DESCRIPTOR_CONFIG_UUID == descriptor?.uuid) {
                val characteristic = gatt
                    ?.getService(UUIDS.SERVICE_UUID)
                    ?.getCharacteristic(UUIDS.CHARACTERISTIC_UUID2)
                gatt?.readCharacteristic(characteristic)
            }
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
            dataString.add(String(it))
            mListener?.onReadData(data = String(it))
        }
    }

    private var gattConn: BluetoothGatt? = null

    private fun getBleDevice(): BluetoothDevice? {
        return BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(mdeviceAddress)
    }

    fun onCreate(mContext: WeakReference<Context>, deviceAddress: String, gallCallback: GattConnectivityCallback) {
        context = mContext
        mdeviceAddress = deviceAddress
        mListener = gallCallback
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
            mListener?.onConnected(false)
            mListener?.onReadData("")
            mListener = null

            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter.isEnabled) {
                stopClient()
            }

            context?.get()?.unregisterReceiver(mBluetoothReceiver)
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

    private fun startClient() {
        val bluetoothDevice = mBluetoothAdapter?.getRemoteDevice(mdeviceAddress)
        gattConn = bluetoothDevice?.connectGatt(context?.get(), false, mGattCallback)

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

        if (!context?.get()?.packageManager?.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)!!) {
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