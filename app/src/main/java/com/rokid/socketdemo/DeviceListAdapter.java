package com.rokid.socketdemo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.rokid.socket.bean.SocketDevice;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class DeviceListAdapter extends BaseAdapter {

    private ArrayList<SocketDevice> mDeviceList;
    private Context mContext;

    public DeviceListAdapter(Context context) {
        mContext = context;
        mDeviceList = new ArrayList<>();
    }

    public void clear() {
        mDeviceList.clear();
        this.notifyDataSetChanged();
    }

    public void addAll(LinkedHashMap<String, SocketDevice> devices) {
        mDeviceList.clear();
        for(SocketDevice device : devices.values()){
            mDeviceList.add(device);
        }
        this.notifyDataSetChanged();
    }

    public void add(SocketDevice device) {
        mDeviceList.add(device);
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mDeviceList.size();
    }

    @Override
    public Object getItem(int position) {
        return mDeviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder mHolder;
        if (convertView == null) {
            mHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.device_list_item, null, true);

            mHolder.tv_name = (TextView) convertView.findViewById(R.id.tv_name);
            mHolder.tv_tag = (TextView) convertView.findViewById(R.id.tv_tag);
            convertView.setTag(mHolder);
        } else {
            mHolder = (ViewHolder) convertView.getTag();
        }

        SocketDevice device = mDeviceList.get(position);
        if (device != null) {
            mHolder.tv_name.setText(device.name);
            mHolder.tv_tag.setText(device.tag);
        }

        return convertView;
    }

    class ViewHolder {
        private TextView tv_name;
        private TextView tv_tag;
    }
}
