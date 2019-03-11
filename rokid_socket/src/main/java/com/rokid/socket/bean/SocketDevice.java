package com.rokid.socket.bean;

import java.net.Socket;

/**
 * @author: zhuo_hf@foxmail.com
 * @version: 1.0
 * Create Time: 2019/3/11
 */
public class SocketDevice {



    public String tag;

    public String name;

    public Socket socket;

    public Thread connectThread;

    public boolean isRegisted;

    @Override
    public String toString() {
        return "SocketDevice{" +
                "tag='" + tag + '\'' +
                ", name='" + name + '\'' +
                ", isRegisted=" + isRegisted +
                '}';
    }
}
