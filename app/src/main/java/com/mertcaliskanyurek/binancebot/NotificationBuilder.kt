package com.mertcaliskanyurek.binancebot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

class NotificationBuilder(private val mContext: Context) : INotification {
    private var mNotiManager: NotificationManager

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun initChannel() {
        val serviceChannel = NotificationChannel(
                CHANNEL_NAME,
                NOTIFICATION_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
        )
        mNotiManager = mContext.getSystemService<NotificationManager>(NotificationManager::class.java)
        mNotiManager.createNotificationChannel(serviceChannel)
    }

    override fun buildNotification(notificationText: String?): Notification {
        val notificationIntent = Intent(mContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(mContext,
                0, notificationIntent, 0)
        return NotificationCompat.Builder(mContext, CHANNEL_NAME)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(CONTENT_TITLE)
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent).build()
    }

    override fun notify(id: Int, notification: Notification?) {
        mNotiManager.notify(id, notification)
    }

    companion object {
        const val CHANNEL_NAME = "NotificationChannel"
        const val NOTIFICATION_NAME = "Binance Service"
        const val CONTENT_TITLE = "Binance Bot"
    }

    init {
        mNotiManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) initChannel()
    }
}

public interface INotification {
    fun buildNotification(notification: String?): Notification
    fun notify(id: Int, notificationBuilder: Notification?)
}