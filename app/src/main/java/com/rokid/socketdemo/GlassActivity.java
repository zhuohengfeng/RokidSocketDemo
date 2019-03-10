package com.rokid.socketdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.rokid.socket.SocketManager;
import com.rokid.socket.callback.IClientCallback;

/**
 * @author: zhuo_hf@foxmail.com
 * @version: 1.0
 * Create Time: 2019/3/10
 */
public class GlassActivity extends AppCompatActivity implements IClientCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glass);

        SocketManager.getInstance().start(this, SocketManager.SocketMode.CLIENT);
        SocketManager.getInstance().setClientCallback(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketManager.getInstance().stop();
    }



}
