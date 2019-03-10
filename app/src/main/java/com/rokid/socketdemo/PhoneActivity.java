package com.rokid.socketdemo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.rokid.socket.SocketManager;
import com.rokid.socket.bean.SocketDevice;
import com.rokid.socket.callback.IServiceCallback;
import com.rokid.socket.utils.Logger;

import java.util.LinkedHashMap;

/**
 * @author: zhuo_hf@foxmail.com
 * @version: 1.0
 * Create Time: 2019/3/10
 */
public class PhoneActivity extends AppCompatActivity implements IServiceCallback {

    private TextView mStatus;

    private String mAllChatText="";

    private TextView mChat;
    private EditText mMessage;
    private Button mBtnSend;

    private Spinner mDeviceSpinner;
    private DeviceListAdapter mDeviceListAdapter;

    private ImageView mBtnSendImg;

    private String mCurrentTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);

        mCurrentTag = null;
        mStatus = findViewById(R.id.tv_status);
        mStatus.setText("发现0台设备");
        mChat = findViewById(R.id.tv_chat);
        mMessage = findViewById(R.id.et_message);
        mBtnSend = findViewById(R.id.btn_send);
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mCurrentTag)) {
                    Toast.makeText(PhoneActivity.this, "请选择要发送的设备", Toast.LENGTH_SHORT).show();
                    return;
                }

                String msg = mMessage.getText().toString();
                if (SocketManager.getInstance().sendToclient(msg, mCurrentTag)) {
                    setChatText(">>>"+msg);
                }
            }
        });

        mDeviceSpinner = findViewById(R.id.list_devices);
        mDeviceListAdapter = new DeviceListAdapter(this);
        mDeviceSpinner.setAdapter(mDeviceListAdapter);
        mDeviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SocketDevice device = (SocketDevice) mDeviceListAdapter.getItem(position);
                mCurrentTag = device.tag;
                Toast.makeText(PhoneActivity.this, "当前选中了"+mCurrentTag, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mCurrentTag = null;
                Toast.makeText(PhoneActivity.this, "当前选中了onNothingSelected", Toast.LENGTH_SHORT).show();
            }
        });

        mBtnSendImg = findViewById(R.id.btn_send_img);
        mBtnSendImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = Utils.createBitmapFromAssets(PhoneActivity.this);
                SocketManager.getInstance().sendToclient(bitmap, mCurrentTag);
            }
        });


        SocketManager.getInstance().start(this, SocketManager.SocketMode.SERVER);
        SocketManager.getInstance().setServiceCallback(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketManager.getInstance().stop();
    }

    @Override
    public void onDevicesChange(LinkedHashMap<String, SocketDevice> devicesList) {
        for(SocketDevice device : devicesList.values()){
            Logger.d("onDevicesChange: 当前已经注册的设备: "+device+"\n");
        }

        mDeviceListAdapter.addAll(devicesList);
        mDeviceListAdapter.notifyDataSetChanged();

        mStatus.setText("发现"+devicesList.size()+"台设备");
    }

    @Override
    public void onReceive(String message, String tag) {
        setChatText("["+tag+"]<<<"+message);
    }

    @Override
    public void onReceive(Bitmap bitmap, String tag) {
        // 显示收到的图片
        Utils.showBitmap(PhoneActivity.this, bitmap);
    }

    private void setChatText(String msg) {
        mAllChatText += msg +"\n";
        mChat.setText(mAllChatText);
    }
}
