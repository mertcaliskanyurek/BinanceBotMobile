package com.mertcaliskanyurek.binancebot;

import android.util.Log;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.event.AggTradeEvent;
import com.binance.api.client.exception.BinanceApiException;

import java.io.Closeable;
import java.io.IOException;

import static com.binance.api.client.domain.account.NewOrder.marketBuy;
import static com.binance.api.client.domain.account.NewOrder.marketSell;

public class BinanceHelper {

    private static final String TAG = BinanceHelper.class.getSimpleName();

    private static final String API_KEY = "kX1UZzDa4na7xcS4ctDtPuSjulvRXSHDaMyP33OWWcgtrxoZ2Ma2LnzSxHV2FHTK";
    private static final String API_SECRET = "FRD94mjoTdsBQ3fD61h2DGpC9y9vr4vT3eiJCtWrMahdFAQNkadLWuDC9BizmDKK";
    //private static final String TRADE_SYMBOL = "WINUSDT";
    //private static final String SYMBOL_TO_SELL = "WIN";
    private static final String SYMBOL_TO_BUY = "USDT";

    //private static final double BUY_SELL_TOLERANCE = 0.01;
    private static final double STOP_LIMIT_TOLERANCE = 0.5;

    private double buyyedPrice = -1;
    private double soldPrice = -1;
    private boolean sold = false;

    private final BinanceApiWebSocketClient socketClient;
    private final BinanceApiRestClient client;
    private Closeable ws;

    private final IBinanceListener listener;
    private final String symbol;
    private final String tradeSymbol;

    private final double buySellTolerance;

    public BinanceHelper(String symbol, double buySellTolerance, IBinanceListener listener)
    {
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(API_KEY, API_SECRET);
        client = factory.newRestClient();
        socketClient = factory.newWebSocketClient();

        this.symbol = symbol;
        this.buySellTolerance = buySellTolerance;
        this.tradeSymbol = symbol + SYMBOL_TO_BUY;
        this.listener = listener;
    }

    public void startListenSocket()
    {
        ws = socketClient.onAggTradeEvent(tradeSymbol.toLowerCase(), new BinanceApiCallback<AggTradeEvent>() {
            @Override
            public void onResponse(final AggTradeEvent response) {
                String price = response.getPrice();
                listener.onPriceChanged(price);
                try {
                    double currentPrice = Double.parseDouble(price);
                    if(buyyedPrice == -1 || (currentPrice <= (soldPrice - (soldPrice * buySellTolerance)) && sold))
                    {
                        if(giveBuyOrder(currentPrice)) {
                            buyyedPrice = currentPrice;
                            sold = false;
                            listener.onBuy("id",String.valueOf(buyyedPrice));
                            Log.d(TAG,"Buyed! id= id price= "+buyyedPrice);
                        }
                    }

                    if (currentPrice >= (buyyedPrice + (buyyedPrice * buySellTolerance)) && !sold)
                    {
                        if(giveSellOrder(currentPrice)) {
                            soldPrice = currentPrice;
                            sold = true;
                            Log.d(TAG,"Sold! id= id price= "+soldPrice);
                            listener.onSell("id",String.valueOf(buyyedPrice),String.valueOf(soldPrice));
                        }
                    }

                    //stop limit
                    if (currentPrice <= (buyyedPrice - (buyyedPrice * STOP_LIMIT_TOLERANCE)) && !sold)
                    {
                        if(giveSellOrder(currentPrice)) {
                            soldPrice = currentPrice;
                            sold = true;
                            Log.d(TAG,"Sold! id= id price= "+soldPrice);
                            listener.onSell("id",String.valueOf(buyyedPrice),String.valueOf(soldPrice));
                        }
                    }

                }catch (NumberFormatException e)
                {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                Log.w(TAG,"Web socket failed");
                cause.printStackTrace(System.err);
            }
        });
    }

    public void stopListenClient() throws IOException {
        ws.close();
    }

    private boolean giveBuyOrder(double price)
    {
        try {
            String balance = client.getAccount().getAssetBalance(SYMBOL_TO_BUY).getFree();
            double balanceDouble = Double.parseDouble(balance);
            int quantity = (int) (balanceDouble / price);
            NewOrderResponse newOrderResponse = client.newOrder(marketBuy(tradeSymbol, String.valueOf(quantity)));
            return true;
        }catch (BinanceApiException e)
        {
            e.printStackTrace();
            listener.onBuyFailed(e.getMessage());
            return false;
        }
        catch (NumberFormatException e)
        {
            e.printStackTrace();
            listener.onBuyFailed(e.getMessage());
            return false;
        }
    }

    private boolean giveSellOrder(double price)
    {
        try {
            String balance = client.getAccount().getAssetBalance(symbol).getFree();
            double balanceDouble = Double.parseDouble(balance);
            int quantity = (int) (balanceDouble / price);
            NewOrderResponse newOrderResponse = client.newOrder(marketSell(tradeSymbol, String.valueOf(quantity)));
            return true;
        }
        catch (BinanceApiException e)
        {
            e.printStackTrace();
            listener.onSellFailed(e.getMessage());
            return false;
        }
        catch (NumberFormatException e)
        {
            e.printStackTrace();
            listener.onBuyFailed(e.getMessage());
            return false;
        }
    }

}

interface IBinanceListener {
    void onPriceChanged(String currPrice);

    void onBuy(String orderId, String price);
    void onSell(String orderId, String buyedPrice, String soldPrice);

    void onBuyFailed(String couse);
    void onSellFailed(String couse);
}
