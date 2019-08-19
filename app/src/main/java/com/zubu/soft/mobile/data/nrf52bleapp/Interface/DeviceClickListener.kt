package com.zubu.soft.mobile.data.nrf52bleapp.Interface

import com.zubu.soft.mobile.data.nrf52bleapp.Models.SensorModel

interface DeviceClickListener {

    fun onConnect(model: SensorModel)

    fun onDisconnect(deviceAddress: String)
}