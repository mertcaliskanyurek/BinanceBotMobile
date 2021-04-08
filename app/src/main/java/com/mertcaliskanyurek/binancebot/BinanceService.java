package com.mertcaliskanyurek.binancebot;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.Date;

public class BinanceService extends Service implements IBinanceListener {


    public static final String PREF_NAME = "com.mertcaliskanyurek.binancebot.service";
    public static final String PREF_SYMBOL_KEY = "SYMBOL";
    public static final String PREF_TOLERANCE_KEY = "TOLERANCE";
    public static final String DEFAULT_SYMBOL = "WIN";
    public static final String DEFAULT_TOLERANCE = "0.01";
    private static final String TAG = BinanceHelper.class.getSimpleName();

    private IBinanceServiceListener mListener;


    private BinanceHelper mBinanceHelper;
    private LogHelper mLogHelper;

    //notification id
    private static final int NOTIFY_ID=1;

    //notification
    private INotification mNotification;
    private boolean started = false;
    //binder
    private final IBinder serviceBinder = new ServiceBinder();
    private String mSymbol;

    public void setListener(IBinanceServiceListener listener) {
        this.mListener = listener;
    }

    private String mTolerance;

    public void onCreate(){
        //create the service
        super.onCreate();
        SharedPreferences prefs = getSharedPreferences(PREF_NAME,MODE_PRIVATE);
        mSymbol = prefs.getString(PREF_SYMBOL_KEY,DEFAULT_SYMBOL);
        mTolerance = prefs.getString(PREF_TOLERANCE_KEY,DEFAULT_TOLERANCE);
        double tolerance = Double.parseDouble(mTolerance);
        mBinanceHelper = new BinanceHelper(mSymbol,tolerance,BinanceService.this);
        mLogHelper = new LogHelper(this);
        mNotification = new NotificationBuilder(this);
    }


    @Override
    public void onPriceChanged(String currPrice) {
        if(mListener != null) mListener.onPriceChanged(currPrice);
    }

    @Override
    public void onBuy(String orderId, String price) {
        String log = mSymbol+" Buyed! price= "+price+" t="+mTolerance;
        mLogHelper.addlog(new Date().toString() + log);
        mNotification.notify(NOTIFY_ID,mNotification.buildNotification(log));
        if(mListener != null) mListener.onTradeEvent();
    }

    @Override
    public void onSell(String orderId, String buyedPrice, String price) {
        String log = mSymbol+" Sold! bp= "+price+" sp= "+price;
        mLogHelper.addlog(new Date().toString() + log);
        mNotification.notify(NOTIFY_ID,mNotification.buildNotification(log));
        if(mListener != null) mListener.onTradeEvent();
    }

    @Override
    public void onBuyFailed(String couse) {
        String log = mSymbol+" Buy failed: "+couse;
        mLogHelper.addlog(new Date().toString() + log);
        mNotification.notify(NOTIFY_ID,mNotification.buildNotification(log));
        if(mListener != null) mListener.onTradeEvent();
    }

    @Override
    public void onSellFailed(String couse) {
        String log = mSymbol+" Sell failed: "+couse;
        mLogHelper.addlog(new Date().toString() + log);
        mNotification.notify(NOTIFY_ID,mNotification.buildNotification(log));
        if(mListener != null) mListener.onTradeEvent();
    }

    //binder
    public class ServiceBinder extends Binder {
        public BinanceService getService() {
            return BinanceService.this;
        }
    }

    //activity will bind to service
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //String action = intent.getAction();
        if(!started) mBinanceHelper.startListenSocket();
        if (!started && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            startForeground(NOTIFY_ID,buildNotification());
            started = true;
            Log.i(TAG,"Service Started with START_NOT_STICKY! Receiver registered");
            return START_NOT_STICKY;
        }
        Log.i(TAG,"Service Started with START_STICKY! Receiver registered");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        //while (!stopService());
        stopService();
    }

    public boolean stopService()
    {
        try {
            if(started) mBinanceHelper.stopListenClient();
            if (started && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                stopForeground(true);
            started = false;
            stopSelf();
            return true;
        }catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Notification buildNotification()
    {
        return mNotification.buildNotification("Binance Service");
    }
}

interface IBinanceServiceListener {
    void onPriceChanged(String price);
    void onTradeEvent();
}