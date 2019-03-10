package com.rokid.socketdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.rokid.socket.SocketManager;
import com.rokid.socket.bean.SocketDevice;
import com.rokid.socket.callback.IServiceCallback;
import com.rokid.socket.utils.Logger;

import java.util.LinkedHashMap;

/**
 * @author: zhuo_hf@foxmail.com
 * @version: 1.0
 * Create Time: 2019/3/10
 */
public class PhoneActivity extends AppCompatActivity implements IServiceCallback {

    private TextView mStatus;

    private ListView mDeviceListView;
    private DeviceListAdapter mDeviceListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);

        mStatus = findViewById(R.id.tv_status);
        mStatus.setText("当前已经连接 0 台设备");

        mDeviceListView = findViewById(R.id.list_devices);
        mDeviceListAdapter = new DeviceListAdapter(this);
        mDeviceListView.setAdapter(mDeviceListAdapter);
        mDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });

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
        for(SocketDevice device : devicesList.values()){
            Logger.d("onDevicesChange: 当前已经注册的设备: "+device+"\n");
        }

        mDeviceListAdapter.addAll(devicesList);
        mDeviceListAdapter.notifyDataSetChanged();

        mStatus.setText("当前已经连接 "+devicesList.size()+" 台设备");
    }
}
