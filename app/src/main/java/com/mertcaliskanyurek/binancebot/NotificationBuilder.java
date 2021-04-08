package com.mertcaliskanyurek.binancebot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import static android.content.Context.NOTIFICATION_SERVICE;

public class NotificationBuilder implements INotification {

    public static final String CHANNEL_NAME = "NotificationChannel";

    private final Context mContext;
    private NotificationManager mNotiManager;

    public NotificationBuilder(Context context){
        this.mContext = context;
        mNotiManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            initChannel();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initChannel()
    {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_NAME,
                "Binance Service",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        mNotiManager = mContext.getSystemService(NotificationManager.class);
        mNotiManager.createNotificationChannel(serviceChannel);
    }


    @Override
    public Notification buildNotification(String notificationText) {
        Intent notificationIntent = new Intent(mContext, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext,
                0, notificationIntent, 0);

        return new NotificationCompat.Builder(mContext, CHANNEL_NAME)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Binance")
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true).build();
    }

    @Override
    public void notify(int id, Notification notification) {
        mNotiManager.notify(id,notification);
    }
}

interface INotification {
    Notification buildNotification(String notification);
    void notify(int id, Notification notificationBuilder);
}
