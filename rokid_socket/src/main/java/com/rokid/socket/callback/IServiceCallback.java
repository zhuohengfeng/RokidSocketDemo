package com.rokid.socket.callback;

import android.graphics.Bitmap;

import com.rokid.socket.bean.SocketDevice;

import java.util.LinkedHashMap;

/**
 * @author: zhuo_hf@foxmail.com
 * @version: 1.0
 * Create Time: 2019/3/10
 */
public interface IServiceCallback {
    void onDevicesChange(LinkedHashMap<String, SocketDevice> devicesList);

    void onReceive(String msg, String tag);

    void onReceive(Bitmap bitmap, String tag);
}
