package com.rokid.socket.bean;

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
    //public final static String CMD_S_ACCEPT_NEW_CLIENT = "cmd_tcp_s_accept_new_client";
    //public final static String CMD_S_CLIENT_DISCONNECT = "cmd_tcp_s_client_disconnect";

    public final static String CMD_C_DISCONNECT = "cmd_tcp_c_disconnect";
    public final static String CMD_C_RECV_SERVICE_MESSAGE = "cmd_tcp_c_recv_service_message";

    private String command;
    private List<String> paramList = new ArrayList<>();

    public MessageEvent(String command){
        this.command=command;
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
