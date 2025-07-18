package com.nitramite.apcupsdmonitor;

import android.annotation.SuppressLint;
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

    @SuppressLint("UnspecifiedImmutableFlag")
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < appWidgetIds.length; i++) {
            try {
                Log.i(TAG, "Widget on update event");
                DatabaseHelper databaseHelper = new DatabaseHelper(context);
                ArrayList<UPS> upsArrayList = getUpsData(databaseHelper);
                RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);
                setBitmap(rv, R.id.upsStatusImage, createUpsViewBitmap(context, upsArrayList));


                // intent to open app on widget click
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    Intent intentSync = new Intent(context, MainMenu.class);
                    intentSync.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    PendingIntent pendingSync = PendingIntent.getActivity(
                            context, 0, intentSync, PendingIntent.FLAG_IMMUTABLE
                    );
                    rv.setOnClickPendingIntent(R.id.upsStatusImage, pendingSync);
                }


                appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
                AppWidgetManager.getInstance(context).updateAppWidget(appWidgetIds[i], rv);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
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
        return databaseHelper.getAllUps("UPS_ID", null, false);
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

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
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


}