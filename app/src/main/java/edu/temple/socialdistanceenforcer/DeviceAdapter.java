package edu.temple.socialdistanceenforcer;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeviceAdapter extends BaseAdapter {
    // our current list of devices
    List<Device> deviceList;
    Context context;

    // set our device list
    public DeviceAdapter(Context context, ArrayList<Device> deviceList){
        this.deviceList = deviceList;
        this.context = context;
    }

    // return the number of devices for the adapter
    @Override
    public int getCount() {
        return deviceList.size();
    }

    // get the device at position
    @Override
    public Object getItem(int position) {
        return deviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    // generate a textview for the current device
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // recycle the textview if possible
        TextView textView;
        if(convertView instanceof TextView){
            textView = (TextView)convertView;
        }else{
            textView = new TextView(context);
        }

        // convert the item to device
        Device temp = (Device)this.getItem(position);

        // display nearby devices with distance
        textView.setText(temp.name + " : " + Float.toString(temp.distance) + " ft");
        textView.setTextSize(24);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setGravity(Gravity.CENTER);

        // display red if closer than 6 feet
        if(temp.distance < 6){
            textView.setBackgroundColor(Color.RED);
            textView.setTextColor(Color.WHITE);
        }else{
            // otherwise white
            textView.setBackgroundColor(Color.WHITE);
            textView.setTextColor(Color.BLACK);
        }

        // return our textview
        return textView;
    }
}
