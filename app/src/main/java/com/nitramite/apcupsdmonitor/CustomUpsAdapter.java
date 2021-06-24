package com.nitramite.apcupsdmonitor;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class CustomUpsAdapter extends ArrayAdapter<UPS> {

    // Logging
    private final static String TAG = CustomUpsAdapter.class.getSimpleName();

    // Variables
    private final Activity context;
    private final ArrayList<UPS> upsArrayList;
    private final SharedPreferences sharedPreferences;

    // Constructor
    CustomUpsAdapter(Activity context, ArrayList<UPS> upsArrayList_) {
        super(context, R.layout.ups_item, upsArrayList_);
        // TODO Auto-generated constructor stub
        this.context = context;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.upsArrayList = upsArrayList_;
    }


    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.ups_item, null, true);

        // Find views
        TextView name = rowView.findViewById(R.id.name);
        TextView status = rowView.findViewById(R.id.status);

        LinearLayout upsModelLayout = rowView.findViewById(R.id.upsModelLayout);
        TextView model = rowView.findViewById(R.id.model);
        TextView lineVoltageOnly = rowView.findViewById(R.id.lineVoltageOnly);
        LinearLayout lineVoltageLayout = rowView.findViewById(R.id.lineVoltageLayout);
        TextView batteryVoltageOnly = rowView.findViewById(R.id.batteryVoltageOnly);
        LinearLayout batteryVoltageLayout = rowView.findViewById(R.id.batteryVoltageLayout);
        TextView internalTemperature = rowView.findViewById(R.id.internalTemperature);
        LinearLayout internalTemperatureLayout = rowView.findViewById(R.id.internalTemperatureLayout);
        ProgressBar loadPercentPB = rowView.findViewById(R.id.loadPercentPB);
        TextView loadPercent = rowView.findViewById(R.id.loadPercent);
        LinearLayout batteryLoadPercentageLayout = rowView.findViewById(R.id.batteryLoadPercentageLayout);
        TextView batteryTimeLeft = rowView.findViewById(R.id.batteryTimeLeft);
        LinearLayout batteryTimeLeftLayout = rowView.findViewById(R.id.batteryTimeLeftLayout);

        FrameLayout chargePercentageFrameLayout = rowView.findViewById(R.id.chargePercentageFrameLayout);
        CustomGauge chargePB = rowView.findViewById(R.id.chargePB);
        TextView percentageTv = rowView.findViewById(R.id.percentageTv);


        // Setting values and visibilities
        name.setText(upsArrayList.get(position).getUPS_NAME());

        upsModelLayout.setVisibility(sharedPreferences.getBoolean(Constants.SP_MS_SHOW_UPS_MODEL, true) ? View.VISIBLE : View.GONE);
        model.setText(upsArrayList.get(position).getMODEL());

        lineVoltageLayout.setVisibility(sharedPreferences.getBoolean(Constants.SP_MS_SHOW_LINE_VOLTAGE, true) ? View.VISIBLE : View.GONE);
        lineVoltageOnly.setText(upsArrayList.get(position).getLineVoltageOnlyStr(rowView.getContext()));

        batteryVoltageLayout.setVisibility(sharedPreferences.getBoolean(Constants.SP_MS_SHOW_BATTERY_VOLTAGE, true) ? View.VISIBLE : View.GONE);
        batteryVoltageOnly.setText(upsArrayList.get(position).getBatteryVoltageOnlyStr(rowView.getContext()));

        internalTemperatureLayout.setVisibility(sharedPreferences.getBoolean(Constants.SP_MS_SHOW_INTERNAL_TEMPERATURE, false) ? View.VISIBLE : View.GONE);
        internalTemperature.setText(upsArrayList.get(position).getITEMP());

        batteryLoadPercentageLayout.setVisibility(sharedPreferences.getBoolean(Constants.SP_MS_SHOW_LOAD_PERCENTAGE, false) ? View.VISIBLE : View.GONE);
        loadPercent.setText(upsArrayList.get(position).getLoadPercentStr(rowView.getContext()));
        loadPercentPB.setProgress(upsArrayList.get(position).getLoadPercentInteger());

        batteryTimeLeftLayout.setVisibility(sharedPreferences.getBoolean(Constants.SP_MS_SHOW_BATTERY_TIME_LEFT, false) ? View.VISIBLE : View.GONE);
        batteryTimeLeft.setText(upsArrayList.get(position).getBATTERY_TIME_LEFT(rowView.getContext()));

        chargePercentageFrameLayout.setVisibility(sharedPreferences.getBoolean(Constants.SP_MS_SHOW_PERCENT_BATTERY_CHARGE, true) ? View.VISIBLE : View.GONE);


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