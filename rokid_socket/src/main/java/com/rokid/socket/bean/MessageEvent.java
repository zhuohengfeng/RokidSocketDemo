package com.rokid.socket.bean;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: zhuo_hf@foxmail.com
 * @version: 1.0
 * Create Time: 2019/3/10
 */
public class MessageEvent{
    public final static String MSG_BROADCAST_PORT = "msg_udp_broadcast_port";

    public final static String CMD_S_TCP_SERVICE_SETUP = "cmd_tcp_s_service_setup";
    public final static String CMD_S_TCP_CLIENT_CHANGE = "cmd_tcp_s_client_change";
    public final static String CMD_S_RECV_CLIENT_MESSAGE = "cmd_tcp_s_recv_client_message";
    public final static String CMD_S_RECV_CLIENT_BITMAP = "cmd_tcp_s_recv_client_bitmap";

    public final static String CMD_C_DISCONNECT = "cmd_tcp_c_disconnect";
    public final static String CMD_C_RECV_SERVICE_MESSAGE = "cmd_tcp_c_recv_service_message";
    public final static String CMD_C_RECV_SERVICE_BITMAP = "cmd_tcp_c_recv_service_bitmap";

    private String command;
    private List<String> paramList = new ArrayList<>();

    private Bitmap bitmap;

    public MessageEvent(String command){
        this.command=command;
    }

    public MessageEvent(String command,Bitmap bitmap, String... params){
        this.command = command;
        this.bitmap = bitmap;
        for (String p : params) {
            paramList.add(p);
        }
    }

    public MessageEvent(String command, String... params){
        this.command = command;
        for (String p : params) {
            paramList.add(p);
        }
    }

    public String getCommand() {
        return command;
    }

    public String getParam(int index) {
        return paramList.get(index);
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    @Override
    public String toString() {
        String param = "";
        for (String p : paramList) {
            param += p + '\'';
        }

        return "MessageEvent{" +
                "command='" + command + '\'' +
                ", param='" + param + '\'' +
                '}';
    }
}
