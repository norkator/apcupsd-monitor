package com.nitramite.apcupsdmonitor;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class Widget extends AppWidgetProvider {

    //  Logging
    private static final String TAG = Widget.class.getSimpleName();


    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Log.i(TAG, "Widget on receive event");
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), Widget.class.getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
        onUpdate(context, appWidgetManager, appWidgetIds);
    }

    // onUpdate
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < appWidgetIds.length; i++) {
            try {
                Intent intent = new Intent(context, MainMenu.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
                DatabaseHelper databaseHelper = new DatabaseHelper(context);
                ArrayList<UPS> upsArrayList = getUpsData(databaseHelper);

                RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);
                setBitmap(rv, R.id.upsStatusImage, createUpsViewBitmap(context, upsArrayList));


                // On click refresh trigger method
                Log.i(TAG, "Widget on update event");
                Intent updateIntent = new Intent(context, Widget.class);
                updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                PendingIntent pendingUpdate = PendingIntent.getBroadcast(context, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                rv.setOnClickPendingIntent(R.id.upsStatusImage, pendingUpdate);

                // Finish
                rv.setOnClickPendingIntent(R.id.upsStatusImage, pendingIntent);
                appWidgetManager.updateAppWidget(appWidgetIds[i], rv);

                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetIds[i], rv);
            } catch (Exception e) {
                Log.i(TAG, e.toString());
            }
        }
    }


    /**
     * Get ups data
     *
     * @param databaseHelper SQLite helper class
     * @return arrayList
     */
    private ArrayList<UPS> getUpsData(DatabaseHelper databaseHelper) {
        return databaseHelper.getAllUps(null);
    }


    private Bitmap createUpsViewBitmap(Context context, ArrayList<UPS> upsArrayList) {
        boolean useDarkTheme = false;
        if ((context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            useDarkTheme = true;
        }

        LayoutInflater inflater = LayoutInflater.from(context);

        LinearLayout mainLinearLayout = new LinearLayout(context);
        mainLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                dpToPixels(context, 100), dpToPixels(context, 80)
        );
        mainLinearLayout.setLayoutParams(layoutParams);

        for (int i = 0; i < upsArrayList.size(); i++) {
            View inflatedLayout = inflater.inflate(R.layout.ups_item_widget, null, true);
            TextView upsName = inflatedLayout.findViewById(R.id.upsName);
            TextView percentageTv = inflatedLayout.findViewById(R.id.percentageTv);

            // Ups name
            upsName.setText(upsArrayList.get(i).getUPS_NAME());

            // Set status color
            if (upsArrayList.get(i).getSTATUS().contains("ONLINE")) {
                upsName.setBackgroundColor(ContextCompat.getColor(context, R.color.bootStrapSuccess));
            } else {
                upsName.setBackgroundColor(ContextCompat.getColor(context, R.color.bootStrapDanger));
            }

            CustomGauge chargePB = inflatedLayout.findViewById(R.id.chargePB);
            chargePB.setValue(upsArrayList.get(i).getBatteryChargeLevelInteger());

            if (useDarkTheme) {
                chargePB.setBackgroundColor(ContextCompat.getColor(context, R.color.widget_background));
                percentageTv.setTextColor(ContextCompat.getColor(context, R.color.whiteColor));
            } else {
                chargePB.setBackgroundColor(ContextCompat.getColor(context, R.color.whiteColor));
            }

            percentageTv.setText(upsArrayList.get(i).getBatteryChargeLevelInteger() + "%");

            mainLinearLayout.addView(inflatedLayout);
        }

        return getBitmapFromView(mainLinearLayout);
    }


    private static Bitmap getBitmapFromView(View view) {
        try {
            view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
            view.draw(canvas);
            return bitmap;
        } catch (IllegalArgumentException e) {
            Log.i(TAG, e.toString());
            return null;
        }
    }


    private int dpToPixels(Context context, int dps) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dps * scale + 0.5f);
    }

    private void setBitmap(RemoteViews views, int resId, Bitmap bitmap) throws RuntimeException {
        Bitmap proxy = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(proxy);
        c.drawBitmap(bitmap, new Matrix(), null);
        views.setImageViewBitmap(resId, proxy);
    }

} // End of class