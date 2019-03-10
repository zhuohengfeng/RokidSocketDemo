package com.rokid.socket;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.IBinder;

import com.rokid.socket.service.TCPService;
import com.rokid.socket.service.UDPService;
import com.rokid.socket.bean.MessageEvent;
import com.rokid.socket.utils.Logger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author: zhuo_hf@foxmail.com
 * @version: 1.0
 * Create Time: 2019/3/10
 */
public class SocketManager {

    // 保存当前UDP线程是服务端，还是客户端
    public enum SocketMode {
        SERVER, CLIENT
    }
    private SocketMode mMode;

    private Context mContext;

    private WifiManager.MulticastLock mMulticastLock;

    // 单实例
    private static SocketManager mInstance = null;
    // 组播的IP地址
    public final static String UDP_IP = "228.5.6.7";//"239.9.9.1";
    // 组播的Port
    public final static Integer UDP_PORT = 6789; //5761;//17375;
    // tcp的Port
    public final static Integer TCP_PORT = 6761;//17375;

    // 为了避免端口被占用，这里定义一个变量可以循环搜索可以试用的端口
    public Integer portServer = TCP_PORT;

    private TCPService mTCPService;

    private UDPService mUDPService;

    private SocketManager() {
        /*this.mDevices = new Vector<>();*/
    }

    public static SocketManager getInstance() {
        if (mInstance == null) {
            synchronized (SocketManager.class){
                mInstance = new SocketManager();
            }
        }
        return mInstance;
    }

    /**
     * 启动连接
     * @param context
     * @param mode 设置是手机端，还是设备端
     */
    public void start(Context context, SocketMode mode) {
        Logger.d("启动连接, mode="+mode);
        EventBus.getDefault().register(this);

        this.mContext = context;
        this.mMode = mode;

        // 启动TCP 服务端
        Intent tcpIntent = new Intent(mContext, TCPService.class);
        tcpIntent.putExtra("mode", mMode);
        mContext.bindService(tcpIntent, mTCPServiceConnection, Context.BIND_AUTO_CREATE);

        // 如果是客户端，则立即启动UDP服务器开始监听
        if(mode == SocketMode.CLIENT) {
            Intent udpIntent = new Intent(mContext, UDPService.class);
            udpIntent.putExtra("mode", mMode);
            mContext.bindService(udpIntent, mUDPServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * 断开连接
     */
    public void stop() {
        Logger.d("关闭连接, mode="+mMode);
        // 解绑TCP服务器
        mContext.unbindService(mTCPServiceConnection);
        mTCPService = null;

        mContext.unbindService(mUDPServiceConnection);
        mUDPService = null;

        if(EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void handleEvent(MessageEvent messageEvent) {
        Logger.d("收到事件, messageEvent="+messageEvent);
        String cmd = messageEvent.getCommand();
        String param1 = messageEvent.getParam1();
        String param2 = messageEvent.getParam2();
        if (mMode == SocketMode.SERVER) {
            // TCP服务端启动成功
            if (cmd.equals(MessageEvent.CMD_S_TCP_SERVICE_SETUP)) {
                Intent tcpIntent = new Intent(mContext, UDPService.class);
                tcpIntent.putExtra("mode", mMode);
                mContext.bindService(tcpIntent, mUDPServiceConnection, Context.BIND_AUTO_CREATE);
            }
            // 有新的TCP客户端连接上了
            else if (cmd.equals(MessageEvent.CMD_S_ACCEPT_NEW_CLIENT)) {
                //
            }
        }
        else if(mMode == SocketMode.CLIENT) {
            // 客户端收到服务端广播的端口信息
            if (cmd.equals(MessageEvent.MSG_BROADCAST_PORT)) {
                if (mTCPService != null) {
                    String tcpIp = param1;
                    int tcpPort = Integer.valueOf(param2);
                    mTCPService.startConnect(tcpIp, tcpPort);
                }
            }

        }
    }


    /**
     * 客户端给服务端发送消息
     * @param message
     * @return
     */
    public boolean sendToService(String message) {
        if (mTCPService != null) {
            mTCPService.sendToService(message);
            return true;
        }
        return false;
    }

    /**
     * 服务端给客户端发送消息
     * @param message
     * @param index
     * @return
     */
    public boolean sendToclient(String message, int index){
        if (mTCPService != null) {
            mTCPService.sendToclient(message, index);
            return true;
        }
        return false;
    }

    /**
     * 断开连接，如果是服务端，则断开所有连接
     */
    public boolean disconnect(){
        if (mTCPService != null) {
            mTCPService.disconnect();
            return true;
        }
        return false;
    }


    private ServiceConnection mTCPServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mTCPService = ((TCPService.LocalBinder) service).getService();
            if (mTCPService != null) {
                mTCPService.startAccept();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mTCPService = null;
        }
    };

    private ServiceConnection mUDPServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mUDPService = ((UDPService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mUDPService = null;
        }
    };


}
