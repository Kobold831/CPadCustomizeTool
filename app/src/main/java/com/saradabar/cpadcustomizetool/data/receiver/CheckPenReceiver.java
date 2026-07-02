package com.saradabar.cpadcustomizetool.data.receiver;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.BenesseExtension;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.saradabar.cpadcustomizetool.R;

public class CheckPenReceiver extends AppWidgetProvider {

    private static final String BC_FTS_PEN_BATTERY = "bc:pen:battery";
    private static final String ACTION_CHECK_PEN = "com.saradabar.cpadcustomizetool.ACTION_CHECK_PEN";

    private static final int BATTERY_UNACQUIRED = 0;
    private static final int BATTERY_EXTREMELY_LOW = 1;
    private static final int BATTERY_LOW = 2;
    private static final int BATTERY_MEDIUM = 3;
    private static final int BATTERY_FULL = 4;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_CHECK_PEN.equals(intent.getAction())) {
            notifyBatteryStatus(context);
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_check_pen);
        Intent intent = new Intent(context, CheckPenReceiver.class);
        intent.setAction(ACTION_CHECK_PEN);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_pen_btn, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private void notifyBatteryStatus(Context context) {
        int batteryStatus = BenesseExtension.getInt(BC_FTS_PEN_BATTERY);

        switch (batteryStatus) {
            case BATTERY_UNACQUIRED:
                showToast(context, R.string.check_pen_battery_annotation_text_unacquired);
                break;
            case BATTERY_EXTREMELY_LOW:
                showToast(context, R.string.check_pen_battery_annotation_text_extremely_low);
                break;
            case BATTERY_LOW:
                showToast(context, R.string.check_pen_battery_annotation_text_low);
                break;
            case BATTERY_MEDIUM:
                showToast(context, R.string.check_pen_battery_annotation_text_medium);
                break;
            case BATTERY_FULL:
                showToast(context, R.string.check_pen_battery_annotation_text_full);
                break;
            default:
                break;
        }
    }

    private void showToast(Context context, int resId) {
        Toast.makeText(context, context.getString(resId), Toast.LENGTH_LONG).show();
    }
}