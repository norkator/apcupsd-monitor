package com.nitramite.apcupsdmonitor;

import android.app.Activity;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
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
        ViewHolder holder;
        if (view == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            view = inflater.inflate(R.layout.ups_item, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        UPS ups = upsArrayList.get(position);

        // Setting values and visibilities
        holder.name.setText(ups.getUPS_NAME());

        holder.upsModelLayout.setVisibility(sharedPreferences.getBoolean(Constants.SP_MS_SHOW_UPS_MODEL, true) ? View.VISIBLE : View.GONE);
        holder.model.setText(ups.getMODEL());

        holder.lineVoltageLayout.setVisibility(sharedPreferences.getBoolean(Constants.SP_MS_SHOW_LINE_VOLTAGE, true) ? View.VISIBLE : View.GONE);
        holder.lineVoltageOnly.setText(ups.getLineVoltageOnlyStr(view.getContext()));

        holder.batteryVoltageLayout.setVisibility(sharedPreferences.getBoolean(Constants.SP_MS_SHOW_BATTERY_VOLTAGE, true) ? View.VISIBLE : View.GONE);
        holder.batteryVoltageOnly.setText(ups.getBatteryVoltageOnlyStr(view.getContext()));

        holder.internalTemperatureLayout.setVisibility(sharedPreferences.getBoolean(Constants.SP_MS_SHOW_INTERNAL_TEMPERATURE, false) ? View.VISIBLE : View.GONE);
        holder.internalTemperature.setText(ups.getITEMP());

        holder.batteryLoadPercentageLayout.setVisibility(sharedPreferences.getBoolean(Constants.SP_MS_SHOW_LOAD_PERCENTAGE, false) ? View.VISIBLE : View.GONE);
        holder.loadPercent.setText(ups.getLoadPercentStr(view.getContext()));
        try {
            holder.loadPercentPB.setProgress(ups.getLoadPercentInteger());
        } catch (Exception ignored) {
            holder.batteryLoadPercentageLayout.setVisibility(View.INVISIBLE);
        }

        holder.batteryTimeLeftLayout.setVisibility(sharedPreferences.getBoolean(Constants.SP_MS_SHOW_BATTERY_TIME_LEFT, false) ? View.VISIBLE : View.GONE);
        holder.batteryTimeLeft.setText(ups.getBATTERY_TIME_LEFT(view.getContext()));

        holder.chargePercentageFrameLayout.setVisibility(sharedPreferences.getBoolean(Constants.SP_MS_SHOW_PERCENT_BATTERY_CHARGE, true) ? View.VISIBLE : View.GONE);


        // Set status (Always shown)
        if (!ups.UPS_ENABLED) {
            holder.status.setText(R.string.ups_disabled);
            holder.status.setBackgroundColor(ContextCompat.getColor(context, R.color.materialGray));
        } else if (ups.upsIsReachable()) {
            holder.status.setText(ups.getSTATUS());
            if (ups.isOnline()) {
                holder.status.setBackgroundColor(ContextCompat.getColor(context, R.color.bootStrapSuccess));
            } else {
                holder.status.setBackgroundColor(ContextCompat.getColor(context, R.color.bootStrapDanger));
            }
        } else {
            holder.status.setText(context.getString(R.string.ups_unreachable));
            holder.status.setBackgroundColor(ContextCompat.getColor(context, R.color.bootStrapWarning));
        }


        // Set battery charge level
        holder.chargePB.setValue(ups.getBatteryChargeLevelInteger());
        String getBatteryChargeLevelInteger = ups.getBatteryChargeLevelInteger() + "%";
        holder.percentageTv.setText(getBatteryChargeLevelInteger);

        return view;
    }

    private static class ViewHolder {
        final TextView name;
        final TextView status;
        final LinearLayout upsModelLayout;
        final TextView model;
        final TextView lineVoltageOnly;
        final LinearLayout lineVoltageLayout;
        final TextView batteryVoltageOnly;
        final LinearLayout batteryVoltageLayout;
        final TextView internalTemperature;
        final LinearLayout internalTemperatureLayout;
        final ProgressBar loadPercentPB;
        final TextView loadPercent;
        final LinearLayout batteryLoadPercentageLayout;
        final TextView batteryTimeLeft;
        final LinearLayout batteryTimeLeftLayout;
        final FrameLayout chargePercentageFrameLayout;
        final CustomGauge chargePB;
        final TextView percentageTv;

        ViewHolder(View view) {
            name = view.findViewById(R.id.name);
            status = view.findViewById(R.id.status);
            upsModelLayout = view.findViewById(R.id.upsModelLayout);
            model = view.findViewById(R.id.model);
            lineVoltageOnly = view.findViewById(R.id.lineVoltageOnly);
            lineVoltageLayout = view.findViewById(R.id.lineVoltageLayout);
            batteryVoltageOnly = view.findViewById(R.id.batteryVoltageOnly);
            batteryVoltageLayout = view.findViewById(R.id.batteryVoltageLayout);
            internalTemperature = view.findViewById(R.id.internalTemperature);
            internalTemperatureLayout = view.findViewById(R.id.internalTemperatureLayout);
            loadPercentPB = view.findViewById(R.id.loadPercentPB);
            loadPercent = view.findViewById(R.id.loadPercent);
            batteryLoadPercentageLayout = view.findViewById(R.id.batteryLoadPercentageLayout);
            batteryTimeLeft = view.findViewById(R.id.batteryTimeLeft);
            batteryTimeLeftLayout = view.findViewById(R.id.batteryTimeLeftLayout);
            chargePercentageFrameLayout = view.findViewById(R.id.chargePercentageFrameLayout);
            chargePB = view.findViewById(R.id.chargePB);
            percentageTv = view.findViewById(R.id.percentageTv);
        }
    }

}
