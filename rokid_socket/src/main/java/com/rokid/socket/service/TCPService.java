package com.rokid.socket.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;

import com.rokid.socket.SocketManager;
import com.rokid.socket.bean.MessageEvent;
import com.rokid.socket.bean.ReceivePackage;
import com.rokid.socket.bean.SocketDevice;
import com.rokid.socket.utils.Logger;
import com.rokid.socket.utils.Utils;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author: zhuo_hf@foxmail.com
 * @version: 1.0
 * Create Time: 2019/3/10
 */
public class TCPService extends Service {

    private final IBinder mBinder = new LocalBinder();

    private SocketManager.SocketMode mMode;

    private String mMasterID = "aaabbccdddee1112233";

    private ThreadPoolExecutor mThreadPoolExecutor;

    private LinkedHashMap<String, SocketDevice> mSocketDevices = new LinkedHashMap<>();

    private int mCurrentSockets = 0;

    private TCPAcceptThread mTCPAcceptThread;

    private TCPConnectThread mTCPConnectThread;

    @Override
    public IBinder onBind(Intent intent) {
        mMode = (SocketManager.SocketMode)(intent.getExtras().get("mode"));
        Logger.d("TCPService: 绑定TCPService, mMode="+mMode);

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Logger.d("TCPService: 解绑TCPService");

        // For service
        if (mTCPAcceptThread != null) {
            mTCPAcceptThread.stopRunning();
            mTCPAcceptThread = null;
        }
        for (SocketDevice device : mSocketDevices.values()) {
            ((TCPRecvThread)device.connectThread).stopRunning();
        }

        // For client
        if (mTCPConnectThread != null) {
            mTCPConnectThread.stopRunning();
            mTCPConnectThread = null;
        }

        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        Logger.d("TCPService: onCreate");
        this.mThreadPoolExecutor = new ThreadPoolExecutor(3,5,1, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(100));
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Logger.d("TCPService: onDestroy");
        super.onDestroy();
    }

    public LinkedHashMap<String, SocketDevice> getAllDevices() {
        return mSocketDevices;
    }

    public LinkedHashMap<String, SocketDevice> getAllRegistedDevices() {
        LinkedHashMap<String, SocketDevice> registedDevices = new LinkedHashMap<>();
        for (SocketDevice device : mSocketDevices.values()) {
            if (device.isRegisted) {
                registedDevices.put(device.tag, device);
            }
        }
        return registedDevices;
    }

    /**
     * TCP服务端开启监听模式，等到客户端连接
     */
    public void startAccept() {
        if (mMode == SocketManager.SocketMode.SERVER) {
            // 启动监听TCP线程
            mTCPAcceptThread = new TCPAcceptThread();
            if (mTCPAcceptThread != null) {
                mTCPAcceptThread.start();
            }
        }
    }

    /**
     * 客户端连接服务端
     */
    public void startConnect(String ip, int port, String masterID) {
        Logger.d("TCPService: startConnect ip="+ip+"， port="+port+", masterID="+masterID);
        if (mMode == SocketManager.SocketMode.CLIENT) {
            this.mMasterID = masterID;
            // 启动监听TCP线程
            mTCPConnectThread = new TCPConnectThread(ip, port);
            if (mTCPConnectThread != null) {
                mTCPConnectThread.start();
            }
        }
    }

    /**
     * 服务端监听线程
     */
    private class TCPAcceptThread extends Thread {
        private ServerSocket mServerSocket;
        private Boolean mKeepRunning;

        public TCPAcceptThread() {
            super("TCPAcceptRunnable");
            this.mKeepRunning = true;
        }

        public void stopRunning() {
            this.mKeepRunning = false;
        }

        @Override
        public void run() {
            try {
                SocketManager.getInstance().portServer = SocketManager.TCP_PORT;
                do {
                    try {
                        // 首先创建一个ServerSocket, 这里是循环端口创建
                        mServerSocket = new ServerSocket(SocketManager.getInstance().portServer);
                    } catch (Exception e) {
                        SocketManager.getInstance().portServer++;
                        Logger.d("CPService: Trying with " + SocketManager.getInstance().portServer);
                    }
                } while (mServerSocket == null || !mServerSocket.isBound());

                EventBus.getDefault().post(new MessageEvent(MessageEvent.CMD_S_TCP_SERVICE_SETUP));
                Logger.d("TCPService: 服务端启动，进入监听状态 port="+SocketManager.getInstance().portServer+", Address="+ Utils.getLocalAddress());

                // 服务器等待客户端的tcp连接 ---- 终于等到TCP连接上了，这里就在服务端创建一个设备名称在APP端连接
                while (mKeepRunning && mServerSocket!=null & !mServerSocket.isClosed()) {
                    Socket clientSocket = mServerSocket.accept();

                    SocketDevice device = new SocketDevice();
                    device.tag = String.valueOf(++mCurrentSockets);
                    device.socket = clientSocket;
                    device.isRegisted = false;

                    device.connectThread = new TCPRecvThread(device.socket, device.tag);
                    device.connectThread.start();

                    mSocketDevices.put(device.tag, device);

                }
            } catch (Exception e) {
                Logger.e("TCPService: Exception in running TCPAcceptThread: "+e);
            }
            finally {
                mSocketDevices.clear();
                try {
                    mServerSocket.close();
                    mServerSocket = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 服务端接收消息线程
     */
    private class TCPRecvThread extends Thread {

        private final Socket mServiceSocket;
        private Boolean mKeepRunning;
        private final String mTag;

        public TCPRecvThread(Socket socket, String tag) {
            this.mServiceSocket = socket;
            this.mKeepRunning = true;
            this.mTag = tag;
        }

        public void stopRunning() {
            this.mKeepRunning = false;
        }

        public void close() {
            Logger.d("TCPConnectThread 关闭服务端连接");
            this.mKeepRunning = false;
            try {
                this.mServiceSocket.shutdownInput();
            } catch (IOException e) {
                Logger.e("TCPConnectThread Closing shutdownInput TCPConnection" + e);
            }

            try {
                if (!this.mServiceSocket.isClosed()) {
                    this.mServiceSocket.close();
                }
            } catch (IOException e) {
                Logger.e("TCPConnectThread Closing close TCPConnection" + e);
            }
        }

        @Override
        public void run() {
            try {
                send(mServiceSocket, "hello");
                Logger.d("TCPRecvThread：服务端收到消息进入消息等待....");
                while (mKeepRunning && mServiceSocket != null && !mServiceSocket.isClosed()) {
                    try{
                        ReceivePackage recvPackage = read(mServiceSocket);
                        if (recvPackage == null) {
                            continue;
                        }

                        Logger.d("TCPRecvThread：服务端收到消息 recvPackage ="+recvPackage);
                        if (recvPackage.type == ReceivePackage.MSG_TYPE_STRING && !TextUtils.isEmpty(recvPackage.getStr)) {
                            SocketDevice device = mSocketDevices.get(mTag);
                            if (recvPackage.getStr.startsWith("register")) {
                                String name = recvPackage.getStr.split("\\|")[1];
                                Logger.d("TCPService: 客户端登录成功：name="+name);
                                device.name = name;
                                device.isRegisted = true;
                                Logger.e("TCPService: 有一个新的设备加入： device="+device);
                                EventBus.getDefault().post(new MessageEvent(MessageEvent.CMD_S_TCP_CLIENT_CHANGE));
                            }
                            else if(recvPackage.getStr.equals("PING")) {
                                // TODO 接收到心跳包

                            }
                            else {
                                // 服务端收到了消息
                                EventBus.getDefault().post(new MessageEvent(MessageEvent.CMD_S_RECV_CLIENT_MESSAGE, recvPackage.getStr, mTag));
                            }
                        }
                        else if(recvPackage.type == ReceivePackage.MSG_TYPE_BITMAP && recvPackage.getBitmap != null) {
                            MessageEvent message = new MessageEvent(MessageEvent.CMD_S_RECV_CLIENT_BITMAP, recvPackage.getBitmap, mTag);
                            EventBus.getDefault().post(message);
                        }
                    }
                    catch(Exception e) {
                        Logger.e("TCPService: Exception in running TCPRecvThread"+e);
                        break;
                    }
                }
                mSocketDevices.remove(mTag);
                // 状态发生变化
                EventBus.getDefault().post(new MessageEvent(MessageEvent.CMD_S_TCP_CLIENT_CHANGE));
            } catch (Exception e) {
                e.printStackTrace();
                Logger.e("TCPService: Exception "+e);
            }
            finally {
                try {
                    if (!mServiceSocket.isClosed()) {
                        mServiceSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 客户端连接线程
     */
    private class TCPConnectThread extends Thread {
        private Socket mClientSocket;
        private Boolean mKeepRunning;
        private String mIP;
        private int mPort;

        public TCPConnectThread(final String ip, final int port) {
            super("TCPConnectThread");
            this.mKeepRunning = true;
            this.mIP = ip;
            this.mPort = port;
        }

        public void stopRunning() {
            this.mKeepRunning = false;
        }

        public Socket getSocket() {
            return mClientSocket;
        }

        public void close() {
            Logger.d("TCPConnectThread 关闭客户端连接");
            this.mKeepRunning = false;
            try {
                this.mClientSocket.shutdownInput();
            } catch (IOException e) {
                Logger.e("TCPConnectThread Closing shutdownInput TCPConnection" + e);
            }

            try {
                if (!this.mClientSocket.isClosed()) {
                    this.mClientSocket.close();
                }
            } catch (IOException e) {
                Logger.e("TCPConnectThread Closing close TCPConnection" + e);
            }
        }

        @Override
        public void run() {
            try {
                this.mClientSocket = new Socket(mIP, mPort);
                Logger.d("TCPConnectThread 客户端进入等到消息 socket="+mClientSocket);
                while (mKeepRunning && mClientSocket != null && !mClientSocket.isClosed()) {

                    ReceivePackage recvPackage = read(mClientSocket);
                    if (recvPackage == null) {
                        continue;
                    }

                    Logger.d("TCPConnectThread：客户端收到消息 recvPackage ="+recvPackage);
                    if (recvPackage.type == ReceivePackage.MSG_TYPE_STRING && !TextUtils.isEmpty(recvPackage.getStr)) {
                        if (recvPackage.getStr.equals("hello")) {
                            // 注册的时候带上设备信息,比如SN等
                            send(mClientSocket, "register|"+"33300011122");
                        }
                        else {
                            EventBus.getDefault().post(new MessageEvent(MessageEvent.CMD_C_RECV_SERVICE_MESSAGE, recvPackage.getStr));
                        }
                    }
                    else if(recvPackage.type == ReceivePackage.MSG_TYPE_BITMAP && recvPackage.getBitmap != null) {
                        MessageEvent message = new MessageEvent(MessageEvent.CMD_C_RECV_SERVICE_BITMAP, recvPackage.getBitmap);
                        EventBus.getDefault().post(message);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Logger.e("TCPConnectThread: CONNECTION READ EXCEPTION"+e);
            }
            finally {
                this.mKeepRunning = false;
                EventBus.getDefault().post(new MessageEvent(MessageEvent.CMD_C_DISCONNECT));
                try {
                    if (mClientSocket != null) {
                        mClientSocket.close();
                        mClientSocket = null;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public ReceivePackage read(final Socket socket) throws IOException {
        DataInputStream input = new DataInputStream(socket.getInputStream());
        ReceivePackage msg = new ReceivePackage();
        //读取长度，也即是消息头，
        Logger.d("read(): 开始等待接收数据");
        long len = input.readLong();
        Logger.d("read(): 读到数据长度 len="+len);
        if (len < 1) {
            return null;
        }

        byte[] bytes = new byte[(int)len];
        long total = 0;
        do {
            byte[] temp = new byte[1024];
            int size = input.read(temp);
            Logger.d("read(): size="+size+", total="+total+", len="+len);
            System.arraycopy(temp, 0, bytes, (int)total, size);
            total += size;
        }while(total < len);


        byte type = bytes[0];
        Logger.d("read(): len="+len+", type="+type);

        byte[] content = new byte[(int)len -1 ];
        System.arraycopy(bytes, 1, content, 0, content.length);

        msg.content = content;
        msg.type = type;
        if (type == ReceivePackage.MSG_TYPE_STRING) {
            msg.getStr = new String(content);
        }
        else if (type == ReceivePackage.MSG_TYPE_BITMAP) {
            msg.getBitmap = Utils.getBitmapFromBytes(content, null);
        }

        return msg;
    }


    // 客户端给服务端发送消息
    public boolean send(final Socket socket, final String content) {
        if (TextUtils.isEmpty(content) || socket == null) {
            return false;
        }
        Logger.d("TCPService send the message content=" + content);
        mThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                DataOutputStream out = null;
                try {
                    byte[] strArray = content.getBytes();
                    byte[] sendMessage = new byte[1 + strArray.length];
                    sendMessage[0] = ReceivePackage.MSG_TYPE_STRING;
                    System.arraycopy(strArray, 0, sendMessage, 1, strArray.length);
                    out = new DataOutputStream(socket.getOutputStream());
                    out.writeLong(strArray.length + 1);
                    out.write(sendMessage);
                    out.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return true;
    }

    public boolean send(final Socket socket, final Bitmap bitmap) {
        if (socket == null || socket.isClosed() || bitmap == null || bitmap.isRecycled()) {
            return false;
        }
        Logger.d("TCPService 发送图片 bitmap=" + bitmap);
        mThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                DataOutputStream out = null;
                try {
                    byte[] bitmapArray = Utils.Bitmap2Bytes(bitmap);
                    byte[] sendMessage = new byte[1 + bitmapArray.length];
                    sendMessage[0] = ReceivePackage.MSG_TYPE_BITMAP;
                    System.arraycopy(bitmapArray, 0, sendMessage, 1, bitmapArray.length);
                    out = new DataOutputStream(socket.getOutputStream());
                    out.writeLong(bitmapArray.length + 1);
                    out.write(sendMessage);
                    out.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return true;
    }

    /**
     * 客户端给服务端发送消息
     * @param message
     * @return
     */
    public boolean sendToService(String message) {
        // 只允许客户端给服务端发送消息
        if (mMode != SocketManager.SocketMode.CLIENT) {
            return false;
        }

        if (mTCPConnectThread == null) {
            return false;
        }

        return this.send(mTCPConnectThread.getSocket(), message);
    }

    /**
     * 服务端给客户端发送消息
     * @param message
     * @param tag
     * @return
     */
    public boolean sendToclient(String message, String tag){
        // 只允许客户端给服务端发送消息
        if (mMode != SocketManager.SocketMode.SERVER) {
            return false;
        }

        if (mSocketDevices == null) {
            return false;
        }

        return this.send(mSocketDevices.get(tag).socket, message);
    }



    /**
     * 客户端给服务端发送消息
     * @return
     */
    public boolean sendToService(Bitmap bitmap) {
        // 只允许客户端给服务端发送消息
        if (mMode != SocketManager.SocketMode.CLIENT) {
            return false;
        }

        if (mTCPConnectThread == null) {
            return false;
        }

        return this.send(mTCPConnectThread.getSocket(), bitmap);
    }

    /**
     * 服务端给客户端发送消息
     * @param tag
     * @return
     */
    public boolean sendToclient(Bitmap bitmap, String tag){
        // 只允许客户端给服务端发送消息
        if (mMode != SocketManager.SocketMode.SERVER) {
            return false;
        }

        if (mSocketDevices == null) {
            return false;
        }

        return this.send(mSocketDevices.get(tag).socket, bitmap);
    }






    public boolean disconnect() {
        Logger.d("Disconnecting the TCP connect");
        if (mMode == SocketManager.SocketMode.CLIENT) {
            if (this.mTCPConnectThread != null && this.mTCPConnectThread.isAlive()) {
                // 调用客户端的close函数
                this.mTCPConnectThread.close();
                while (this.mTCPConnectThread.isAlive()) {
                    ;
                }
            }
            this.mTCPConnectThread = null;
            return true;
        }
        else if (mMode == SocketManager.SocketMode.SERVER) {
            if (mSocketDevices != null) {
                for (SocketDevice device : mSocketDevices.values()) {
                    if (device.connectThread != null && device.connectThread.isAlive()) {
                        // 调用客户端的close函数
                        ((TCPRecvThread)device.connectThread).close();
                        while (device.connectThread.isAlive()) {
                            ;
                        }
                    }
                }
                mSocketDevices.clear();
                mSocketDevices = null;
                return true;
            }
        }
        return false;
    }

    public class LocalBinder extends Binder {
        public TCPService getService() {
            return TCPService.this;
        }
    }
}
