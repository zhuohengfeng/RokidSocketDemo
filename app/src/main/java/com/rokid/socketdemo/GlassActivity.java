package com.rokid.socketdemo;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.rokid.socket.SocketManager;
import com.rokid.socket.callback.IClientCallback;

/**
 * @author: zhuo_hf@foxmail.com
 * @version: 1.0
 * Create Time: 2019/3/10
 */
public class GlassActivity extends AppCompatActivity implements IClientCallback {

    private String mAllChatText="";

    private TextView mStatus;
    private TextView mChat;
    private EditText mMessage;
    private Button mBtnSend;

    private ImageView mBtnSendImg;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glass);

        mStatus = findViewById(R.id.tv_status);
        mChat = findViewById(R.id.tv_chat);
        mMessage = findViewById(R.id.et_message);
        mBtnSend = findViewById(R.id.btn_send);
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = mMessage.getText().toString();
                if (SocketManager.getInstance().sendToService(msg)) {
                    setChatText(">>>"+msg);
                }
            }
        });

        mBtnSendImg = findViewById(R.id.btn_send_img);
        mBtnSendImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = Utils.createBitmapFromAssets(GlassActivity.this);
                SocketManager.getInstance().sendToService(bitmap);
            }
        });

        SocketManager.getInstance().start(this, SocketManager.SocketMode.CLIENT);
        SocketManager.getInstance().setClientCallback(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SocketManager.getInstance().stop();
    }


    @Override
    public void onStatusChange(String status) {
        mStatus.setText(status);
    }

    @Override
    public void onReceive(String message) {
        setChatText("<<<"+message);
    }

    @Override
    public void onReceive(Bitmap bitmap) {
        // 显示收到的图片
        Utils.showBitmap(GlassActivity.this, bitmap);
    }

    private void setChatText(String msg) {
        mAllChatText += msg +"\n";
        mChat.setText(mAllChatText);
    }
}
