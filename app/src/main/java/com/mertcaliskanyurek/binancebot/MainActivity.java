package com.mertcaliskanyurek.binancebot;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity implements IBinanceServiceListener{

    @BindView(R.id.listView_logs) ListView lvLogs;
    @BindView(R.id.editText_symbol) EditText etSymbol;
    @BindView(R.id.editText_tolerance) EditText etTolerance;
    @BindView(R.id.tv_curr_price) TextView tvCurrPrice;

    private BinanceService mBinanceService = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        SharedPreferences pref = getSharedPreferences(BinanceService.PREF_NAME,MODE_PRIVATE);
        String symbol = pref.getString(BinanceService.PREF_SYMBOL_KEY,BinanceService.DEFAULT_SYMBOL);
        String tolerance = pref.getString(BinanceService.PREF_TOLERANCE_KEY,BinanceService.DEFAULT_TOLERANCE);
        etSymbol.setText(symbol);
        etTolerance.setText(tolerance);
    }

    @Override
    protected void onStart() {
        super.onStart();
        showLogs();
    }

    private void showLogs()
    {
        String[] logs = new LogHelper(this).getLogs().toArray(new String[0]);
        ArrayAdapter<String> adapter =new ArrayAdapter<String>
                (this, android.R.layout.simple_list_item_1, android.R.id.text1, logs);

        lvLogs.setAdapter(adapter);
    }

    private void startService()
    {
        Intent serviceIntent = new Intent(this, BinanceService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(serviceIntent);
        else
            startService(serviceIntent);
    }

    protected void stopService()
    {
        if(mBinanceService != null) {
            mBinanceService.setListener(null);
            mBinanceService.stopService();
            unbindService(serviceConnection);
            Toast.makeText(MainActivity.this,"Stoped",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //if(mBinanceService == null)
           // bindService(new Intent(this, BinanceService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void startListen(View view) {
        if(mBinanceService != null) return;

        startService();
        bindService(new Intent(this, BinanceService.class), serviceConnection, 0);
    }

    public void stopListen(View view) {
        stopService();
    }

    public final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d("Service Connection","onServiceConttected");
            BinanceService.ServiceBinder binder = (BinanceService.ServiceBinder)iBinder;
            //get service
            mBinanceService = binder.getService();
            mBinanceService.setListener(MainActivity.this);
            Toast.makeText(MainActivity.this,"Started",Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("Service Connection","onServiceDisconnected");
        }
    };

    public void setSymbol(View view) {
        SharedPreferences.Editor editor = getSharedPreferences(BinanceService.PREF_NAME,MODE_PRIVATE).edit();
        editor.putString(BinanceService.PREF_SYMBOL_KEY,etSymbol.getText().toString()).apply();
        try{
            Double.parseDouble(etTolerance.getText().toString());
            editor.putString(BinanceService.PREF_TOLERANCE_KEY,etTolerance.getText().toString()).apply();
            Toast.makeText(MainActivity.this,"Success",Toast.LENGTH_SHORT).show();
        }catch (NumberFormatException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onPriceChanged(String price) {
        tvCurrPrice.setText(price);
    }

    @Override
    public void onTradeEvent() {
        showLogs();
    }
}