package com.zubu.soft.mobile.data.nrf52bleapp.Adapter

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zubu.soft.mobile.data.nrf52bleapp.Interface.GattConnectivityCallback
import com.zubu.soft.mobile.data.nrf52bleapp.Models.CustomModel
import com.zubu.soft.mobile.data.nrf52bleapp.R
import com.zubu.soft.mobile.data.nrf52bleapp.Util.GattConnection
import kotlinx.android.synthetic.main.cell.view.*
import org.jetbrains.anko.runOnUiThread
import java.lang.ref.WeakReference
import java.util.*


class DevicesAdapter(var mContext: WeakReference<Context>, var list: ArrayList<CustomModel>) :
    RecyclerView.Adapter<DevicesAdapter.ViewHolder>() {
    private lateinit var progressBar: ProgressBar

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.cell,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return if (list.isEmpty()) 0
        else list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            holder.apply {
                var result = list[position].scanResult
                var data = ""

                data += "Device address: ${result?.device?.address}\n"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    data += "Device Connectible: ${result?.isConnectable}\n"
                }
                val iterator = result?.scanRecord?.serviceUuids?.iterator()
                if (iterator != null && iterator.hasNext()) {
                    do {
                        data += "Service UUID: ${iterator.next().uuid.mostSignificantBits}\n"
                    } while (iterator.hasNext())
                }
                sensorName.text = list[position].scanResult?.device?.name
                sensorData.text = data
                connectBtn.setOnClickListener {
                    if (connectBtn.text == "Connect") {
                        connectBtn.text = "Connecting..."
                        var connection = GattConnection()
                        list[position].gattConnection = connection
                        connection.onCreate(
                            mContext,
                            list[position].scanResult?.device?.address!!,
                            object : GattConnectivityCallback {
                                override fun onConnected(success: Boolean) {
                                    mContext.get()?.runOnUiThread {
                                        if (success) {
                                            connectBtn.text = "Disconnect"
                                        } else {
                                            connectBtn.text = "Connect"
                                        }
                                        tvConnected.text = "Device Connected: $success"
                                    }
                                }

                                override fun onReadData(data: String) {
                                    mContext.get()?.runOnUiThread {
                                        if (data.isNotEmpty()) {
                                            llData.visibility = View.VISIBLE
                                            tvData.text = "Data: $data"
                                        } else {
                                            llData.visibility = View.GONE
                                        }
                                    }
                                }

                            })
                    } else {
                        connectBtn.text = "Disconnecting..."
                        var gatt = list[position].gattConnection
                        gatt?.let {
                            it.onDestroy()
                        }
                        list.removeAt(position)
                        notifyDataSetChanged()
                    }
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var sensorName: TextView = itemView.tvSensorName
        var sensorData: TextView = itemView.tvSensorData
        var connectBtn: TextView = itemView.btn_connect
        var tvConnected: TextView = itemView.tvConnected
        var tvData: TextView = itemView.tvData
        var llData: LinearLayout = itemView.llData
    }
}