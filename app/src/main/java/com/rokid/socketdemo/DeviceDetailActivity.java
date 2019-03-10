package com.rokid.socketdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.rokid.socket.SocketManager;
import com.rokid.socket.bean.SocketDevice;
import com.rokid.socket.callback.IServiceCallback;

import java.util.LinkedHashMap;

public class DeviceDetailActivity extends AppCompatActivity implements IServiceCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        SocketManager.getInstance().start(this, SocketManager.SocketMode.SERVER);
        SocketManager.getInstance().setServiceCallback(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketManager.getInstance().stop();
    }

    @Override
    public void onDevicesChange(LinkedHashMap<String, SocketDevice> devicesList) {

    }
}
