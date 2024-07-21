package com.nitramite.apcupsdmonitor;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class CustomEventsAdapter extends ArrayAdapter<String> {

    // Variables
    private final Activity context;
    private final ArrayList<String> events;
    private final Boolean eventsColoring;

    // Constructor
    CustomEventsAdapter(Activity context, ArrayList<String> events, Boolean eventsColoring) {
        super(context, R.layout.events_adapter, events);
        // TODO Auto-generated constructor stub
        this.context = context;
        this.events = events;
        this.eventsColoring = eventsColoring;
    }


    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.events_adapter, null, true);
        TextView event = rowView.findViewById(R.id.event);
        final String positionStr = events.get(position);
        event.setText(positionStr);
        if (
                positionStr.contains("Power failure") || positionStr.contains("powered by the UPS battery")
                        || positionStr.contains("UPS fault")
                        || positionStr.contains("The UPS output is off")
        ) {
            if (this.eventsColoring) {
                event.setBackgroundColor(ContextCompat.getColor(context, R.color.bootStrapDanger));
            } else {
                event.setTextColor(ContextCompat.getColor(context, R.color.bootStrapDanger));
            }
        } else if (
                positionStr.contains("Power is back") || positionStr.contains("is restored")
                        || positionStr.contains("returns to normal load") || positionStr.contains("powered by the utility")
        ) {
            if (this.eventsColoring) {
                event.setBackgroundColor(ContextCompat.getColor(context, R.color.bootStrapSuccess));
            } else {
                event.setTextColor(ContextCompat.getColor(context, R.color.bootStrapSuccess));
            }
        }
        return rowView;
    }


}