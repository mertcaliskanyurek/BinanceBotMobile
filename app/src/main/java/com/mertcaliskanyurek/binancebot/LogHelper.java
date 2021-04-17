package com.mertcaliskanyurek.binancebot;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.collection.ArraySet;

import java.util.HashSet;
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
        SharedPreferences prefs = mContext.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        Set<String> logs = getLogs(prefs);
        logs.add(log);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(KEY,logs);
        editor.commit();
    }

    public Set<String> getLogs(SharedPreferences prefs)
    {
        if(prefs == null) prefs = mContext.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        return prefs.getStringSet(KEY,new HashSet<>());
    }
}
