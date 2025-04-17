package com.saradabar.cpadcustomizetool.data.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.RemoteViews;

import com.saradabar.cpadcustomizetool.R;

public class AlwaysNotiService extends Service {

    private static final int NOTIFICATION_ID = 1000;

    /** @noinspection SameParameterValue*/
    private void startForeground(int id) {
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification);
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setContent(views);
        builder.setContentText("");
        builder.setContentTitle("");
        builder.setTicker("");
        builder.setShowWhen(false);
        builder.setSmallIcon(R.drawable.ic_stat_transparent);
        builder.setPriority(Notification.PRIORITY_MIN);
        builder.setAutoCancel(false);
        Notification noti = builder.build();
        noti.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(id, noti);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
