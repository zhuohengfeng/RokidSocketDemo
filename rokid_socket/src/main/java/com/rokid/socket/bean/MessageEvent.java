package com.rokid.socket.bean;

/**
 * @author: zhuo_hf@foxmail.com
 * @version: 1.0
 * Create Time: 2019/3/10
 */
public class MessageEvent{
    public final static String MSG_BROADCAST_PORT = "msg_udp_broadcast_port";

    public final static String CMD_S_TCP_SERVICE_SETUP = "cmd_tcp_s_tcp_service_setup";
    public final static String CMD_S_ACCEPT_NEW_CLIENT = "cmd_tcp_s_accept_new_client";
    public final static String CMD_S_CLIENT_DISCONNECT = "cmd_tcp_s_client_disconnect";

    public final static String CMD_C_DISCONNECT = "cmd_tcp_c_disconnect";

    private String command;
    private String param1;
    private String param2;

    public MessageEvent(String command){
        this.command=command;
    }

    public MessageEvent(String command, String param1){
        this.command = command;
        this.param1 = param1;
    }

    public MessageEvent(String command, String param1, String param2){
        this.command = command;
        this.param1 = param1;
        this.param2 = param2;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getParam1() {
        return param1;
    }

    public void setParam1(String param1) {
        this.param1 = param1;
    }

    public String getParam2() {
        return param2;
    }

    public void setParam2(String param2) {
        this.param2 = param2;
    }

    @Override
    public String toString() {
        return "MessageEvent{" +
                "command='" + command + '\'' +
                ", param1='" + param1 + '\'' +
                ", param2='" + param2 + '\'' +
                '}';
    }
}
