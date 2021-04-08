package com.mertcaliskanyurek.binancebot;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.collection.ArraySet;

import java.util.Set;

public class LogHelper {

    private static final String NAME = "com.mertcaliskanyurek.binancebot.logs";
    private static final String KEY = "LOG";

    private static final String TAG = LogHelper.class.getSimpleName();

    private Context mContext;

    public LogHelper(Context context) {
        this.mContext = context;
    }

    public void addlog(String log)
    {
        Set<String> logs = getLogs();
        logs.add(log);
        SharedPreferences.Editor editor = mContext.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit();
        editor.putStringSet(KEY,logs);
        editor.apply();
    }

    public Set<String> getLogs()
    {
        Set<String> def = new ArraySet<>();
        return mContext.getSharedPreferences(NAME, Context.MODE_PRIVATE).getStringSet(KEY,def);
    }
}
