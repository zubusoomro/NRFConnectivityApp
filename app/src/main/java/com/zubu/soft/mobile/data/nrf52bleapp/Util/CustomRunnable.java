package com.zubu.soft.mobile.data.nrf52bleapp.Util;

import android.os.Handler;
import com.zubu.soft.mobile.data.nrf52bleapp.Interface.DeviceDeleteCallBack;


public class CustomRunnable implements Runnable {
    private static final long mInterval = 10000;
    private Handler handler;
    private String key;
    private DeviceDeleteCallBack callBack;

    public CustomRunnable(String key, Object advertiser) {
        this.key = key;
        callBack = (DeviceDeleteCallBack) advertiser;
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
