package com.zubu.soft.mobile.data.nrf52bleapp.Interface

interface GattConnectivityCallback {

    fun onConnected(success: Boolean)
    fun onReadData(data: String)
}