package com.mertcaliskanyurek.binancebot

import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.BinanceApiRestClient
import com.binance.api.client.BinanceApiWebSocketClient
import com.binance.api.client.domain.OrderSide
import com.binance.api.client.domain.OrderStatus
import com.binance.api.client.domain.TimeInForce
import com.binance.api.client.domain.account.NewOrder
import com.binance.api.client.domain.event.UserDataUpdateEvent
import com.binance.api.client.exception.BinanceApiException

class BinanceHelper(symbol: String, buySellTolerance: Double, listener: IBinanceListener?) {
    private var buyyedPrice = ""
    private var soldPrice = ""
    private val socketClient: BinanceApiWebSocketClient
    private val client: BinanceApiRestClient

    //private Closeable ws;
    private val listener: IBinanceListener?
    private val symbol: String
    private val tradeSymbol: String
    private val buySellTolerance: Double
    private var mListenKey: String? = null

    fun startTrade() {
        if (mListenKey == null) {
            mListenKey = client.startUserDataStream()
            client.keepAliveUserDataStream(mListenKey)
            socketClient.onUserDataUpdateEvent(mListenKey ) { response: UserDataUpdateEvent ->
                if (response.eventType == UserDataUpdateEvent.UserDataUpdateEventType.ORDER_TRADE_UPDATE) {
                    val orderTradeUpdateEvent = response.orderTradeUpdateEvent
                    if (orderTradeUpdateEvent.eventType == "executionReport" && orderTradeUpdateEvent.orderStatus == OrderStatus.FILLED) {
                        if (orderTradeUpdateEvent.side == OrderSide.BUY) {
                            buyyedPrice = orderTradeUpdateEvent.price
                            listener?.onBuy(orderTradeUpdateEvent.orderId.toString(),
                                    buyyedPrice)
                            while (!giveSellOrder()) {
                                try {
                                    Thread.sleep(200)
                                } catch (e: InterruptedException) {
                                    e.printStackTrace()
                                }
                            }
                            listener!!.onOrderGiven("Sell Order given!")
                        } else {
                            soldPrice = orderTradeUpdateEvent.price
                            listener?.onSell(orderTradeUpdateEvent.orderId.toString(),
                                    buyyedPrice, soldPrice)
                            while (!giveBuyOrder()) {
                                try {
                                    Thread.sleep(200)
                                } catch (e: InterruptedException) {
                                    e.printStackTrace()
                                }
                            }
                            listener!!.onOrderGiven("Buy Order given!")
                        }
                        //giveSellOrder();
                    }
                }
            }
        }
        giveBuyOrder()
    }

    /*public void startTrade() {
        ws = socketClient.onCandlestickEvent(tradeSymbol.toLowerCase(), CandlestickInterval.ONE_MINUTE, (CandlestickEvent response) -> {
                String price = response.getClose();
                listener.onPriceChanged(price);
                double currentPrice = Double.parseDouble(price);
                if (buyyedPrice == Double.MAX_VALUE || (currentPrice <= (soldPrice - buySellTolerance) && sold)) {
                    if (giveBuyOrder(currentPrice)) {
                        buyyedPrice = currentPrice;
                        sold = false;
                        listener.onBuy("id", String.valueOf(buyyedPrice));
                        Log.d(TAG, "Buyed! id= id price= " + buyyedPrice);
                    }
                }

                if (currentPrice >= (buyyedPrice + buySellTolerance) && !sold) {
                    if (giveSellOrder(currentPrice)) {
                        soldPrice = currentPrice;
                        sold = true;
                        Log.d(TAG, "Sold! id= id price= " + soldPrice);
                        listener.onSell("id", String.valueOf(buyyedPrice), String.valueOf(soldPrice));
                    }
                }

                //stop limit
                if (currentPrice <= (buyyedPrice - STOP_LIMIT_TOLERANCE) && !sold) {
                    if (giveSellOrder(currentPrice)) {
                        soldPrice = currentPrice;
                        sold = true;
                        Log.d(TAG, "Sold! id= id price= " + soldPrice);
                        listener.onSell("id", String.valueOf(buyyedPrice), String.valueOf(soldPrice));
                    }
                }
        });
    }*/
    fun stopListenClient() {
        client.closeUserDataStream(mListenKey)
    }

    /*private boolean giveBuyOrder(double price) {
        try {
            String balance = client.getAccount().getAssetBalance(SYMBOL_TO_BUY).getFree();
            double balanceDouble = Double.parseDouble(balance);
            int quantity = (int) (balanceDouble / price);
            NewOrderResponse newOrderResponse = client.newOrder(marketBuy(tradeSymbol, String.valueOf(quantity)));
            return newOrderResponse.getStatus() == OrderStatus.FILLED;
        } catch (BinanceApiException e) {
            e.printStackTrace();
            listener.onBuyFailed(e.getMessage());
            return false;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            listener.onBuyFailed(e.getMessage());
            return false;
        }
    }*/
    private fun giveBuyOrder(): Boolean {
        return try {
            val price = client.getPrice(tradeSymbol).price.toDouble()
            val balance = client.account.getAssetBalance(SYMBOL_TO_BUY).free
            val balanceDouble = balance.toDouble()
            val quantity = (balanceDouble / price).toInt()
            val newOrderResponse = client.newOrder(NewOrder.limitBuy(tradeSymbol, TimeInForce.GTC, quantity.toString(), (price - buySellTolerance).toString()))
            listener!!.onOrderGiven("Buy Order: " + (price - buySellTolerance))
            //listener.onPriceChanged("Buy Order: " + (price - buySellTolerance));
            newOrderResponse.status == OrderStatus.NEW
        } catch (e: BinanceApiException) {
            e.printStackTrace()
            //listener.onBuyFailed(e.getMessage());
            false
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            //listener.onBuyFailed(e.getMessage());
            false
        }
    }

    /*private boolean giveSellOrder(double price) {
        try {
            String balance = client.getAccount().getAssetBalance(symbol).getFree();
            double balanceDouble = Double.parseDouble(balance);
            int quantity = (int) balanceDouble;
            NewOrderResponse newOrderResponse = client.newOrder(marketSell(tradeSymbol, String.valueOf(quantity)));
            return newOrderResponse.getStatus() == OrderStatus.FILLED;
        } catch (BinanceApiException e) {
            e.printStackTrace();
            listener.onSellFailed(e.getMessage());
            return false;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            listener.onBuyFailed(e.getMessage());
            return false;
        }
    }*/
    private fun giveSellOrder(): Boolean {
        return try {
            val price = client.getPrice(tradeSymbol).price.toDouble()
            val balance = client.account.getAssetBalance(symbol).free
            val balanceDouble = balance.toDouble()
            val quantity = balanceDouble.toInt()
            val newOrderResponse = client.newOrder(NewOrder.limitSell(tradeSymbol, TimeInForce.GTC, quantity.toString(), (price + buySellTolerance).toString()))
            newOrderResponse.status == OrderStatus.NEW
        } catch (e: BinanceApiException) {
            e.printStackTrace()
            //listener.onSellFailed(e.getMessage());
            false
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            //listener.onBuyFailed(e.getMessage());
            false
        }
    }

    companion object {
        private val TAG = BinanceHelper::class.java.simpleName
        private const val API_KEY = "kX1UZzDa4na7xcS4ctDtPuSjulvRXSHDaMyP33OWWcgtrxoZ2Ma2LnzSxHV2FHTK"
        private const val API_SECRET = "FRD94mjoTdsBQ3fD61h2DGpC9y9vr4vT3eiJCtWrMahdFAQNkadLWuDC9BizmDKK"

        //private static final String TRADE_SYMBOL = "WINUSDT";
        //private static final String SYMBOL_TO_SELL = "WIN";
        private const val SYMBOL_TO_BUY = "USDT"
        private const val STOP_LIMIT_TOLERANCE = 0.5
    }

    init {
        val factory = BinanceApiClientFactory.newInstance(API_KEY, API_SECRET)
        client = factory.newRestClient()
        socketClient = factory.newWebSocketClient()
        this.symbol = symbol
        this.buySellTolerance = buySellTolerance
        tradeSymbol = symbol + SYMBOL_TO_BUY
        this.listener = listener
    }
}

interface IBinanceListener {
    fun onBuy(orderId: String?, price: String?)
    fun onSell(orderId: String?, buyedPrice: String?, soldPrice: String?)
    fun onOrderGiven(order: String?)
}