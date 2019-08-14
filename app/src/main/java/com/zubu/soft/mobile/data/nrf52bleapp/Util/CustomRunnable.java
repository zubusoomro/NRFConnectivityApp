package com.zubu.soft.mobile.data.nrf52bleapp.Util;

import android.os.Handler;
import com.zubu.soft.mobile.data.nrf52bleapp.Interface.DeviceDeleteCallBack;
import com.zubu.soft.mobile.data.nrf52bleapp.Services.BeaconStickyService;


public class CustomRunnable implements Runnable {
    private static final long mInterval = 5000;
    private Handler handler;
    private String key;
    private BeaconStickyService advertiser;
    private DeviceDeleteCallBack callBack;

    public CustomRunnable(String key, BeaconStickyService advertiser) {
        this.key = key;
        this.advertiser = advertiser;
        callBack = advertiser;
    }

    @Override
    public void run() {
        callBack.onDeviceDelete(key);
    }

    public void setPostDelayedHandler() {
        if (handler != null) {
            handler.removeCallbacks(this);
        }
        handler = new Handler();
        handler.postDelayed(this, mInterval);
    }
}
