package com.rokid.socket.utils;

import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * @author: zhuo_hf@foxmail.com
 * @version: 1.0
 * Create Time: 2019/3/10
 */
public class Utils {

    /**
     * 获取本地IP地址
     * @return
     * @throws IOException
     */
    public static InetAddress getLocalAddress() throws IOException {
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
            NetworkInterface intf = en.nextElement();
            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                InetAddress inetAddress = enumIpAddr.nextElement();
                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                    return inetAddress;
                }
            }
        }
        return null;
    }

    public static String serialize(Object object) {
        byte[] result = null;

        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(bs);
            os.writeObject(object);
            os.close();
            result = bs.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new String();
        }

        return Base64.encodeToString(result, Base64.NO_WRAP);
    }

    public static Object unserialize(String str) {
        Object object = null;
        byte[] bytes = Base64.decode(str, Base64.NO_WRAP);

        try {
            ByteArrayInputStream bs = new ByteArrayInputStream(bytes);
            ObjectInputStream is = new ObjectInputStream(bs);
            object = (Object) is.readObject();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
            return new String();
        }
        return object;
    }

}
