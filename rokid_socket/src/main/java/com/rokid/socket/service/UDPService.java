package com.rokid.socket.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;

import com.rokid.socket.SocketManager;
import com.rokid.socket.bean.MessageEvent;
import com.rokid.socket.utils.Logger;
import com.rokid.socket.utils.Utils;

import org.greenrobot.eventbus.EventBus;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author: zhuo_hf@foxmail.com
 * @version: 1.0
 * Create Time: 2019/3/10
 */
public class UDPService extends Service {

    private final IBinder mBinder = new LocalBinder();

    private WifiManager.MulticastLock mMulticastLock;

    private SocketManager.SocketMode mMode;

    private UDPMonitorThread mUDPMonitorThread;

    private String mMasterID = "abcdef0123456789";

    @Override
    public IBinder onBind(Intent intent) {
        mMode = (SocketManager.SocketMode)(intent.getExtras().get("mode"));
        Logger.d("UDPService: 绑定UDPService, mMode="+mMode);

        WifiManager wifiManager=(WifiManager)this.getSystemService(Context.WIFI_SERVICE);
        mMulticastLock = wifiManager.createMulticastLock("multicast.rokid");
        mMulticastLock.acquire();

        mUDPMonitorThread = new UDPMonitorThread(mMode);
        if (mUDPMonitorThread != null) {
            mUDPMonitorThread.start();
        }

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Logger.d("UDPService: 解绑UDPService");

        if (mUDPMonitorThread != null) {
            mUDPMonitorThread.close();
            mUDPMonitorThread = null;
        }

        if (mMulticastLock != null) {
            try {
                mMulticastLock.release();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        Logger.d("UDPService: onCreate");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Logger.d("UDPService: onDestroy");
        super.onDestroy();
    }


    /**
     * 服务端和客户端通过UDP进行握手操作
     */
    private class UDPMonitorThread extends Thread {
        private MulticastSocket multicastSocket;

        private Boolean keepRunning;

        private SocketManager.SocketMode mMode;

        private Timer timer = new Timer();
        private TimerTask task;

        public UDPMonitorThread(SocketManager.SocketMode mode) {
            super("UPDServer");
            this.mMode = mode;
            this.keepRunning = true;
            try {
                Logger.d("[UDPServer] start UDPServer now port:"+SocketManager.UDP_PORT);
                Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
                NetworkInterface eth0 = null;
                while (enumeration.hasMoreElements()) {
                    eth0 = enumeration.nextElement();
                    if (eth0.getName().equals("eth0")) {
                        //there is probably a better way to find ethernet interface
                        break;
                    }
                }

                // 创建组播方式的UDP socket
                multicastSocket = new MulticastSocket(SocketManager.UDP_PORT);
                //设置本MulticastSocket发送的数据报会被回送到自身
                multicastSocket.setLoopbackMode(true);
                multicastSocket.setNetworkInterface(NetworkInterface.getByName("wlan0"));

                InetAddress address = InetAddress.getByName(SocketManager.UDP_IP);
                multicastSocket.joinGroup(new InetSocketAddress(address, SocketManager.UDP_PORT), eth0);

                // 如果是客户端，定时广播发送心跳数据
                if (mMode == SocketManager.SocketMode.SERVER) {
                    /*发送广播数据*/
                    sendBroadcast(MessageEvent.MSG_BROADCAST_PORT + "|" + SocketManager.getInstance().portServer+"|"+mMasterID);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Logger.e("[UDPServer] Exception creating socket: "+e);
                try {
                    if(null!=multicastSocket && !multicastSocket.isClosed()){
                        multicastSocket.leaveGroup(InetAddress.getByName(SocketManager.UDP_IP));
                        multicastSocket.close();
                    }
                } catch (Exception e1) {
                    Logger.e("[UDPServer] Exception creating socket"+e1);
                    e.printStackTrace();
                }
            }
        }

        // 断开UDP线程， 离开组播UDP
        public void close() {
            Logger.d("[UDPServer] close udp server...bye...");
            this.keepRunning = false;

            if (timer != null) {
                timer.purge();
                timer.cancel();
                timer = null;
            }

            try {
                if(null!=multicastSocket && !multicastSocket.isClosed()){
                    multicastSocket.leaveGroup(InetAddress.getByName(SocketManager.UDP_IP));
                    multicastSocket.close();
                }
            } catch (Exception e1) {
                Logger.e("[UDPServer] Exception creating socket"+e1);
            }
        }

        /** 向组播的IP地址和端口， 发送消息， 这里由于是给组播IP发送，所以是广播的， 所有socket都能接受到 */
        public void sendBroadcast(final String data) {
            if (timer == null) {
                timer = new Timer();
            }

            if (task == null) {
                task = new TimerTask() {
                    @Override
                    public void run() {
                        Logger.d("[UDPServer] 服务器发送广播....");
                        try {
                            InetAddress local = InetAddress.getByName(SocketManager.UDP_IP);
                            DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(), local, SocketManager.UDP_PORT);
                            multicastSocket.send(packet);
                            Logger.d("[UDPServer] sendBroadcast data="+data);
                        } catch (Exception e) {
                            Logger.e("[UDPServer] Exception during sendBroadcast"+e);
                        }
                    }
                };
            }

            timer.schedule(task, 1000, 4000);
        }


        @Override
        public void run() {
            super.run();
            try {
                byte[] buf = new byte[1024];

                while (keepRunning && multicastSocket != null && !multicastSocket.isClosed()) {
                    Logger.d("[UDPServer] Inside while");

                    // 这里会阻塞住，一直等待新的报文传递过来！！！
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    multicastSocket.receive(packet);
                    Logger.d("[UDPServer] Datagram received");

                    // 是谁发过来的?
                    InetAddress remoteIP = packet.getAddress();

                    // 判断消息是不是自己发送的
                    if ((remoteIP.getHostAddress()).equals(Utils.getLocalAddress().getHostAddress())){
                        Logger.d("[UDPServer] 发给自己了....return");
                        continue;
                    }

                    // 如果不是自己发给自己的，则继续处理
                    String content = new String(packet.getData(), 0, packet.getLength());
                    Logger.d("[UDPServer] UDP Recv: Content: " + content + "， remoteIP="+remoteIP+", mode="+mMode);

                    // 如果是客户端
                    if (mMode == SocketManager.SocketMode.CLIENT) {
                        String cmd = content.split("\\|")[0];
                        Logger.d("[UDPServer] UDP 客户端收到 Recv: cmd: " + cmd);
                        if (cmd.equals(MessageEvent.MSG_BROADCAST_PORT)) {
                            if (timer != null) {
                                timer.purge();
                                timer.cancel();
                                timer = null;
                            }

                            String tcpPort = content.split("\\|")[1];
                            String masterID = content.split("\\|")[2];
                            Logger.e("[UDPServer] 客户端收到 UDP Recv: tcpIP: " + remoteIP.getHostName() + ", tcpPort="+tcpPort+", masterID="+masterID);
                            // 已经收到服务器TCP地址了，不需要再发送心跳包了

                            EventBus.getDefault().post(new MessageEvent(cmd, remoteIP.getHostAddress(), tcpPort, masterID));
                            break;
                        }
                    }
                }
                Logger.e("[UDPServer] UDPFinished work.....Bye....");
                close();
            } catch (Exception e) {
                e.printStackTrace();
                Logger.d("Exception e:"+e);
            }
        }
    }



    public class LocalBinder extends Binder {
        public UDPService getService() {
            return UDPService.this;
        }
    }

}
