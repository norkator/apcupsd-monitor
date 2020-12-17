package com.nitramite.apcupsdmonitor;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class CustomUpsAdapter extends ArrayAdapter<UPS> {

    // Logging
    private final static String TAG = "CustomUpsAdapter";

    // Variables
    private final Activity context;
    private final ArrayList<UPS> upsArrayList;

    // Constructor
    CustomUpsAdapter(Activity context, ArrayList<UPS> upsArrayList_) {
        super(context, R.layout.ups_item, upsArrayList_);
        // TODO Auto-generated constructor stub
        this.context = context;
        this.upsArrayList = upsArrayList_;
    }


    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.ups_item, null, true);

        // Find views
        TextView name = rowView.findViewById(R.id.name);
        TextView status = rowView.findViewById(R.id.status);

        TextView model = rowView.findViewById(R.id.model);
        TextView lineVoltageOnly = rowView.findViewById(R.id.lineVoltageOnly);
        TextView batteryVoltageOnly = rowView.findViewById(R.id.batteryVoltageOnly);

        CustomGauge chargePB = rowView.findViewById(R.id.chargePB);
        TextView percentageTv = rowView.findViewById(R.id.percentageTv);


        // Name
        name.setText(upsArrayList.get(position).getUPS_NAME());

        model.setText(upsArrayList.get(position).getMODEL());
        lineVoltageOnly.setText(upsArrayList.get(position).getLineVoltageOnlyStr(rowView.getContext()));
        batteryVoltageOnly.setText(upsArrayList.get(position).getBatteryVoltageOnlyStr(rowView.getContext()));

        // Set status (Always shown)
        if (upsArrayList.get(position).upsIsReachable()) {
            status.setText(upsArrayList.get(position).getSTATUS());
            if (upsArrayList.get(position).isOnline()) {
                status.setBackgroundColor(ContextCompat.getColor(context, R.color.bootStrapSuccess));
            } else {
                status.setBackgroundColor(ContextCompat.getColor(context, R.color.bootStrapDanger));
            }
        } else {
            status.setText(context.getString(R.string.ups_unreachable));
            status.setBackgroundColor(ContextCompat.getColor(context, R.color.bootStrapWarning));
        }


        // Set battery charge level
        chargePB.setValue(upsArrayList.get(position).getBatteryChargeLevelInteger());
        String getBatteryChargeLevelInteger = upsArrayList.get(position).getBatteryChargeLevelInteger() + "%";
        percentageTv.setText(getBatteryChargeLevelInteger);

        return rowView;
    }


} // End of class