package com.rokid.socket.callback;

public interface IClientCallback {

    void onStatusChange(String status);

    void onReceive(String message);

}
