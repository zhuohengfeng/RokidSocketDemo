package com.rokid.socket.callback;

import com.rokid.socket.bean.SocketDevice;

import java.util.LinkedHashMap;

public interface IServiceCallback {
    void onDevicesChange(LinkedHashMap<String, SocketDevice> devicesList);

    void onReceive(String msg, String tag);
}
