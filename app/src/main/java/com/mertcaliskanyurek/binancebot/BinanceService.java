package com.mertcaliskanyurek.binancebot;

import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static android.content.Context.MODE_PRIVATE;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE;

public class BinanceService extends Worker implements IBinanceListener {

    public static final String PREF_NAME = "com.mertcaliskanyurek.binancebot.service";
    public static final String PREF_SYMBOL_KEY = "SYMBOL";
    public static final String PREF_TOLERANCE_KEY = "TOLERANCE";
    public static final String DEFAULT_SYMBOL = "WIN";
    public static final String DEFAULT_TOLERANCE = "0.0001";
    private static final String TAG = BinanceHelper.class.getSimpleName();

    private IBinanceServiceListener mListener;

    //notification id
    private static final int NOTIFY_ID = 1;

    private BinanceHelper mBinanceHelper;
    private final LogHelper mLogHelper;

    //notification
    private final INotification mNotification;
    private boolean started = false;
    private String mSymbol;
    private String mTolerance;


    public BinanceService(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mLogHelper = new LogHelper(context);
        mNotification = new NotificationBuilder(context);
        initBinance();
    }

    public void setListener(IBinanceServiceListener listener) {
        this.mListener = listener;
    }

    public void initBinance() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        mSymbol = prefs.getString(PREF_SYMBOL_KEY, DEFAULT_SYMBOL);
        mTolerance = prefs.getString(PREF_TOLERANCE_KEY, DEFAULT_TOLERANCE);
        double tolerance = Double.parseDouble(mTolerance);
        mBinanceHelper = new BinanceHelper(mSymbol, tolerance, BinanceService.this);
    }

    @Override
    public void onBuy(String orderId, String price) {
        String log = mSymbol + " \nBuyed! price= " + price + " t=" + mTolerance;
        String date = SimpleDateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
        mLogHelper.addlog(date + " - " + log);
        mNotification.notify(NOTIFY_ID, mNotification.buildNotification(log));
        if (mListener != null) mListener.onTradeEvent();
    }

    @Override
    public void onSell(String orderId, String buyedPrice, String price) {
        String log = mSymbol + " \nSold! bp= " + price + " sp= " + price;
        String date = SimpleDateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
        mLogHelper.addlog(date + " - " + log);
        mNotification.notify(NOTIFY_ID, mNotification.buildNotification(log));
        if (mListener != null) mListener.onTradeEvent();
    }

    @Override
    public void onOrderGiven(String order) {
        String log = mSymbol + " \n"+order;
        String date = SimpleDateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
        mLogHelper.addlog(date + " - " + log);
        mNotification.notify(NOTIFY_ID, mNotification.buildNotification(date + " - " + log));
        if (mListener != null) mListener.onTradeEvent();
    }

    public void startBinanceClient() {
        if (!started) {
            mBinanceHelper.startTrade();
            started = true;
        }
    }

    public void stopBinanceClient() {
        if (started) {
            mBinanceHelper.stopListenClient();
            started = false;
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        String progress = "Trade Started";
        setForegroundAsync(createForegroundInfo(progress));
        startBinanceClient();
        return Result.success();
    }

    @Override
    public void onStopped() {
        super.onStopped();
        stopBinanceClient();
    }

    @NonNull
    private ForegroundInfo createForegroundInfo(@NonNull String progress) {
        // Build a notification using bytesRead and contentLength
        Notification notification = mNotification.buildNotification(progress);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            return new ForegroundInfo(NOTIFY_ID, notification, FOREGROUND_SERVICE_TYPE_NONE);
        else
            return new ForegroundInfo(NOTIFY_ID, notification);
    }
}

interface IBinanceServiceListener {
    void onPriceChanged(String price);

    void onTradeEvent();
}