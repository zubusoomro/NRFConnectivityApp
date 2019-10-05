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
import com.zubu.soft.mobile.data.nrf52bleapp.Interface.DeviceClickListener
import com.zubu.soft.mobile.data.nrf52bleapp.Models.SensorModel
import com.zubu.soft.mobile.data.nrf52bleapp.R
import kotlinx.android.synthetic.main.cell.view.*
import java.lang.ref.WeakReference
import java.util.*


class DevicesAdapter(
    var mContext: WeakReference<Context>,
    var list: ArrayList<SensorModel>,
    var listener: DeviceClickListener
) :
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
                list[adapterPosition].let { model ->
                    model.scanResult?.let {
                        var data = ""

                        data += "Device address: ${it.device?.address}\n"
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            data += "Device Connectible: ${it.isConnectable}\n"
                        }
                        val iterator = it.scanRecord?.serviceUuids?.iterator()
                        if (iterator != null && iterator.hasNext()) {
                            do {
                                data += "Service UUID: ${iterator.next().uuid.mostSignificantBits}\n"
                            } while (iterator.hasNext())
                        }
                        sensorName.text = it.device?.name
                        sensorData.text = data
                    }
                    if (!model.gattSet) {
                        connectBtn.text = mContext.get()?.getString(R.string.text_connect)
                        tvConnected.text = "Device Connected: false"
                    } else {
                        connectBtn.text = mContext.get()?.getString(R.string.text_disconnect)
                        tvConnected.text = "Device Connected: true"
                    }
                    if (!model.sensorData.isNullOrEmpty()) {
                        llData.visibility = View.VISIBLE
                        tvData.text = "Data: ${model.sensorData}"
                    } else {
                        llData.visibility = View.GONE
                    }
                }




                connectBtn.setOnClickListener {
                    if (connectBtn.text == mContext.get()?.getString(R.string.text_connect)) {
                        connectBtn.text = mContext.get()?.getString(R.string.text_connecting)
                        list[adapterPosition].gattSet = true
                        listener.onConnect(list[adapterPosition])
                    } else {
                        connectBtn.text = mContext.get()?.getString(R.string.text_disconnecting)
                        list[adapterPosition].getSensorAddress().let { it1 ->
                            listener.onDisconnect(it1)
                        }
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