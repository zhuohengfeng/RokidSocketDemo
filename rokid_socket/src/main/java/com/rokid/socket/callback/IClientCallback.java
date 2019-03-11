package com.rokid.socket.callback;

import android.graphics.Bitmap;

import com.rokid.socket.SocketManager;

/**
 * @author: zhuo_hf@foxmail.com
 * @version: 1.0
 * Create Time: 2019/3/10
 */
public interface IClientCallback {

    void onStatusChange(SocketManager.SocketStatus status);

    void onReceive(String message);

    void onReceive(Bitmap bitmap);
}
