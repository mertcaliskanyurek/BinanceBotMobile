package com.mertcaliskanyurek.binancebot

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import butterknife.BindView
import butterknife.ButterKnife
import java.util.*

class MainActivity : AppCompatActivity(), IBinanceServiceListener {
    @kotlin.jvm.JvmField
    @BindView(R.id.listView_logs)
    var lvLogs: ListView? = null

    @kotlin.jvm.JvmField
    @BindView(R.id.editText_symbol)
    var etSymbol: EditText? = null

    @kotlin.jvm.JvmField
    @BindView(R.id.editText_tolerance)
    var etTolerance: EditText? = null

    @kotlin.jvm.JvmField
    @BindView(R.id.tv_curr_price)
    var tvCurrPrice: TextView? = null

    //private BinanceService mBinanceService = null;
    private val mServiceBind = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)
        //mBinanceService = new BinanceService(this, WorkerParameters.a);
        val pref = getSharedPreferences(BinanceService.PREF_NAME, Context.MODE_PRIVATE)
        val symbol = pref.getString(BinanceService.PREF_SYMBOL_KEY, BinanceService.DEFAULT_SYMBOL)
        val tolerance = pref.getString(BinanceService.PREF_TOLERANCE_KEY, BinanceService.DEFAULT_TOLERANCE)
        etSymbol!!.setText(symbol)
        etTolerance!!.setText(tolerance)
    }

    override fun onStart() {
        super.onStart()
        showLogs()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    private fun showLogs() {
        val list = LogHelper(this).getLogs(null)
        val logs: List<String> = ArrayList(list)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, android.R.id.text1, logs)
        lvLogs!!.adapter = adapter
    }

    private fun startService() {
        val manager = WorkManager.getInstance(this)
        manager.cancelAllWork()
        val uploadWorkRequest: WorkRequest = OneTimeWorkRequest.Builder(BinanceService::class.java)
                .build()
        manager.enqueue(uploadWorkRequest)
        Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show()
    }

    protected fun stopService() {
        WorkManager.getInstance(this).cancelAllWork()
        Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show()
    }

    fun startListen(view: View?) {
        //if(mBinanceService != null) mBinanceService.startBinanceClient();
        startService()
    }

    fun stopListen(view: View?) {
        //if(mBinanceService.stopBinanceClient()) stopService();
        stopService()
    }

    fun setSymbol(view: View?) {
        val editor = getSharedPreferences(BinanceService.PREF_NAME, Context.MODE_PRIVATE).edit()
        editor.putString(BinanceService.PREF_SYMBOL_KEY, etSymbol!!.text.toString()).apply()
        try {
            stopService()
            etTolerance!!.text.toString().toDouble()
            editor.putString(BinanceService.PREF_TOLERANCE_KEY, etTolerance!!.text.toString()).apply()
            Toast.makeText(this@MainActivity, "Success", Toast.LENGTH_SHORT).show()
            //if(mBinanceService != null) mBinanceService.initBinance();
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
    }

    override fun onPriceChanged(price: String) {
        runOnUiThread { tvCurrPrice!!.text = price }
    }

    override fun onTradeEvent() {
        runOnUiThread { showLogs() }
    }
}