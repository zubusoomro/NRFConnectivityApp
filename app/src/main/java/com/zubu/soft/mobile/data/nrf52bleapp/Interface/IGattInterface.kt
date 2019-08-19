package com.zubu.soft.mobile.data.nrf52bleapp.Interface

import com.zubu.soft.mobile.data.nrf52bleapp.Models.SensorModel

interface IGattInterface {
    fun onGotDevice(model: SensorModel)
}