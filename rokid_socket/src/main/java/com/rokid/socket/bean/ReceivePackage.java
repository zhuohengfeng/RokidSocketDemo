package com.rokid.socket.bean;

import android.graphics.Bitmap;

import java.util.Arrays;

public class ReceivePackage {
    public final static byte MSG_TYPE_STRING = 0x01;
    public final static byte MSG_TYPE_BITMAP = 0x02;

    public byte type;
    public byte[] content;

    public String getStr;
    public Bitmap getBitmap;

    @Override
    public String toString() {
        return "ReceivePackage{" +
                "type=" + type +
                ", content=" + Arrays.toString(content) +
                ", getStr='" + getStr + '\'' +
                ", getBitmap=" + getBitmap +
                '}';
    }
}
