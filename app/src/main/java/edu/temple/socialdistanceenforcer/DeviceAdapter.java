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
    List<Device> deviceList;
    Context context;

    public DeviceAdapter(Context context, ArrayList<Device> deviceList){
        this.deviceList = deviceList;
        this.context = context;
    }

    @Override
    public int getCount() {
        return deviceList.size();
    }

    @Override
    public Object getItem(int position) {
        return deviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        if(convertView instanceof TextView){
            textView = (TextView)convertView;
        }else{
            textView = new TextView(context);
        }

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
            textView.setBackgroundColor(Color.WHITE);
            textView.setTextColor(Color.BLACK);
        }
        return textView;
    }
}
