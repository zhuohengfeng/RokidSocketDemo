package com.rokid.socketdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.rokid.socket.SocketManager;

/**
 * @author: zhuo_hf@foxmail.com
 * @version: 1.0
 * Create Time: 2019/3/10
 */
public class PhoneActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);
    }

    @Override
    protected void onStart() {
        super.onStart();
        SocketManager.getInstance().start(this, SocketManager.SocketMode.SERVER);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SocketManager.getInstance().stop();
    }


}
