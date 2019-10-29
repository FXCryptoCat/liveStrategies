package liveStrategies;

import com.dukascopy.api.Filter;
import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IChart;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IHistory;
import com.dukascopy.api.IIndicators;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.IStrategy;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import liveStrategies.options.MenuStrategy;

/**
 *
 * @author rescorsim
 */
public class Temamet_C1 implements IStrategy {

    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IChart chart;
    private IOrder order;

    public int defaultSlippage = 5;
    public Period defaultPeriod = Period.FIVE_MINS;
    public double defaultTradeAmount = 0.1;
    public int defaultTakeProfit = 300;
    public int defaultStopLoss = 100;

    private static Set<Instrument> instruments = null;

    // private
    private List<IOrder> AllPositions = null;
    private int labelCounter = 1;
    private double bidPrice;
    private double askPrice;

    private int slowTrend, fastTrend;
    private boolean slowCrossUp_close, slowCrossDown_close;
    private boolean fastCrossUp_close, fastCrossDown_close;
    
    private boolean slowCrossUp_open, slowCrossDown_open;
    private boolean fastCrossUp_open, fastCrossDown_open;

    //public
    public int slowTemaPeriod = 100;
    public int fastTemaPeriod = 30;
    public double pipsToOpenSlow = 2;
    public double pipsToOpenFast = 3;


    public int maxPipsLastCandle = 70;
    public int minPipsLastCandle = 0;
    public int minCandlesForNewOrder = 0;
    public int candlesAfterTheLastOrder = 0;


    public Temamet_C1() {
        slowCrossUp_close = slowCrossDown_close = false;
        fastCrossUp_close = fastCrossDown_close = false;
        slowCrossUp_open = slowCrossDown_open = false;
        fastCrossUp_open = fastCrossDown_open = false;
        
        slowTrend = fastTrend = 0;
        
        instruments = new HashSet<Instrument>();
        instruments.add(Instrument.USDJPY);
        instruments.add(Instrument.GBPUSD);
        instruments.add(Instrument.EURCAD);
        instruments.add(Instrument.EURUSD);
//        
//        instruments.add(Instrument.AUDNZD);
//        instruments.add(Instrument.USDCAD);
//        instruments.add(Instrument.USDCHF);
//        instruments.add(Instrument.EURGBP);
//        instruments.add(Instrument.AUDUSD);
//        instruments.add(Instrument.NZDUSD);
//        instruments.add(Instrument.EURJPY);
    }

    public static void main(String[] args) throws Exception {
        MenuStrategy menuStrategy = new MenuStrategy(new Temamet_C1(), instruments, 10000.0, Period.TEN_SECS);
        menuStrategy.showMenuStrategy();
    }

    @Override
    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();

//        instruments.add(Instrument.EURUSD);
//        instruments.add(Instrument.EURGBP);
//        instruments.add(Instrument.EURJPY);
//        instruments.add(Instrument.USDJPY);
//        instruments.add(Instrument.USDCAD);
//        instruments.add(Instrument.AUDUSD);
//        instruments.add(Instrument.NZDUSD);
//        instruments.add(Instrument.GBPUSD);
//        instruments.add(Instrument.EURCAD);
//        instruments.add(Instrument.EURAUD);
//        instruments.add(Instrument.GBPJPY);
        this.context.setSubscribedInstruments(instruments);
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if (askBar.getVolume() == 0) {
            return;
        }
        for (Instrument i : instruments) {
            if (i.equals(instrument) && period == defaultPeriod) {
                proccessBar(i, period, askBar, bidBar);
            }
        }
    }

    @Override
    public void onMessage(IMessage message) throws JFException {

    }

    @Override
    public void onAccount(IAccount account) throws JFException {

    }

    @Override
    public void onStop() throws JFException {

    }

    public void proccessBar(Instrument instrument, Period period, IBar askBar, IBar bidBar){

        candlesAfterTheLastOrder++;
        askPrice = askBar.getClose();
        bidPrice = bidBar.getClose();
        slowCrossUp_open = slowCrossDown_open = false;
        fastCrossUp_open = fastCrossDown_open = false;
        slowCrossUp_close = slowCrossDown_close = false;
        fastCrossUp_close = fastCrossDown_close = false;
        double tradeAmount = context.getAccount().getEquity() / 20000;

        try {
            double[] valuesSlow = context.getIndicators().tema(
                    instrument, defaultPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE,
                    slowTemaPeriod, Filter.WEEKENDS, 3, bidBar.getTime(), 0);

            double[] valuesFast = context.getIndicators().tema(
                    instrument, defaultPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE,
                    fastTemaPeriod, Filter.WEEKENDS, 3, bidBar.getTime(), 0);

            console.getOut().println("NewBar-> valuesFast[0]:" + valuesFast[0]
                    + ", valuesFast[1]: " + valuesFast[1]
                    + ", valuesFast[2]: " + valuesFast[2]
                    + ", valuesSlow[0]: " + valuesSlow[0]
                    + ", valuesSlow[1]: " + valuesSlow[1]
                    + ", valuesSlow[2]: " + valuesSlow[2]
            );
            
            double valueToOpenSlow = pipsToOpenSlow * instrument.getPipValue();
            double valueToOpenFast = pipsToOpenFast * instrument.getPipValue();

            if ((valuesSlow[1]- valuesSlow[0]) > valueToOpenSlow && (valuesSlow[2] - valuesSlow[1]) < valueToOpenSlow) { 
                slowCrossUp_open = true;
                slowTrend = 1;
            } else if ((valuesSlow[0] - valuesSlow[1]) > valueToOpenSlow  && (valuesSlow[1] - valuesSlow[2]) < valueToOpenSlow) { 
                slowCrossDown_open = true;
                slowTrend = -1;
            }
            if ((valuesFast[1]- valuesFast[0]) > valueToOpenFast && (valuesFast[2] - valuesFast[1]) < valueToOpenFast) { 
                fastCrossUp_open = true;
                fastTrend = 1;
            } else if ((valuesFast[0] - valuesFast[1]) > valueToOpenFast  && (valuesFast[1] - valuesFast[2]) < valueToOpenFast) { 
                fastCrossDown_open = true;
                fastTrend = -1;
            }
            
            if (valuesSlow[1] <= valuesSlow[0] && valuesSlow[2] > valuesSlow[1]) { 
                slowCrossUp_close = true;
                slowTrend = 1;
            } else if (valuesSlow[1] >= valuesSlow[0] && valuesSlow[2] < valuesSlow[1]) { 
                slowCrossDown_close = true;
                slowTrend = -1;
            }
            if ((valuesFast[1]- valuesFast[0]) < valueToOpenFast && (valuesFast[2] - valuesFast[1]) > valueToOpenFast) { 
                fastCrossUp_close = true;
                fastTrend = 1;
            } else if ((valuesFast[0] - valuesFast[1]) < valueToOpenFast  && (valuesFast[1] - valuesFast[2]) > valueToOpenFast) { 
                fastCrossDown_close = true;
                fastTrend = -1;
            }

            updateVariables(instrument);
            if (AllPositions.size() > 0 && (fastCrossUp_close || fastCrossDown_close)) {
                for (IOrder order : AllPositions) {
                    if (order.getInstrument() == instrument) {
                        order.close();
                    }
                }
            }

            if(!validateLastCandle(instrument)){
                return;
            }
            if(!validateDistanceToNewOrder()){
                return;
            }

            //if ((slowCrossUp_open /*&& fastTrend > 0*/) || (/*slowTrend > 0 &&*/fastCrossUp_open)) {
            if ((slowCrossUp_open && fastTrend > 0) || (slowTrend > 0 && fastCrossUp_open)) {
                double stoploss = bidPrice - instrument.getPipValue() * defaultStopLoss;
                double takeprofit = bidPrice + instrument.getPipValue() * defaultTakeProfit;
                IOrder myOrder = engine.submitOrder(
                        this.getIntrumentLabel(instrument, "Temamet_C1"), // String label
                        instrument, //Instrument instrument
                        IEngine.OrderCommand.BUY, // IEngine.OrderCommand orderCommand
                        tradeAmount, // double amount
                        askPrice, // double price
                        defaultSlippage, // double slippage
                        stoploss, // double stopLossPrice
                        takeprofit); // double takeProfitPrice
                candlesAfterTheLastOrder = 0;
            }
            //if ((slowCrossDown_open /*&& fastTrend < 0*/) || (/*slowTrend < 0 &&*/fastCrossDown_open)) {
            if ((slowCrossDown_open && fastTrend < 0) || (slowTrend < 0 && fastCrossDown_open)) {
                double stoploss = askPrice + instrument.getPipValue() * defaultStopLoss;
                double takeprofit = askPrice - instrument.getPipValue() * defaultTakeProfit;
                IOrder myOrder = engine.submitOrder(
                        this.getIntrumentLabel(instrument, "Temamet_C1"), // String label
                        instrument, //Instrument instrument
                        IEngine.OrderCommand.SELL, // IEngine.OrderCommand orderCommand
                        tradeAmount, // double amount
                        bidPrice, // double price
                        defaultSlippage, // double slippage
                        stoploss, // double stopLossPrice
                        takeprofit); // double takeProfitPrice
                candlesAfterTheLastOrder = 0;
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void TakeProfitControl(){

    }

    public boolean validateDistanceToNewOrder(){
        if(candlesAfterTheLastOrder > minCandlesForNewOrder){
            return true;
        }
        return false;
    }

    public boolean validateLastCandle(Instrument instrument) {
        IBar prevBar;
        try {
            prevBar = history.getBar(instrument, defaultPeriod, OfferSide.BID, 1);
            double candleSize = prevBar.getHigh() - prevBar.getLow();
            double maxLastCandle = maxPipsLastCandle * instrument.getPipValue();
            double minLastCandle = minPipsLastCandle * instrument.getPipValue();
            if(candleSize >= minLastCandle && candleSize <= maxLastCandle){
                return true;
            }
        } catch (JFException ex) {
            Logger.getLogger(Temamet_C1.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    protected void updateVariables(Instrument instrument) {
        try {
            AllPositions = engine.getOrders(instrument);
        } catch (JFException e) {
            e.printStackTrace();
        }
    }

    private static double roundToPippette(double amount, Instrument instrument) {
        return round(amount, instrument.getPipScale() + 1);
    }

    private static double round(double amount, int decimalPlaces) {
        return (new BigDecimal(amount)).setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    public String getIntrumentLabel(Instrument instrument, String prefix) throws JFException {
        String label = instrument.toString();
        label = label.replace("/", "");
        label = prefix + "_" + label;
        label += getFormattedTime("_ddMMyyyy_HHmmss_SSS_", "GMT-03:00");
        label += labelCounter;
        labelCounter++;
        return label;
    }

    public String getFormattedTime(String format, String gmt) {
        SimpleDateFormat fmt = new SimpleDateFormat(format);
        fmt.setTimeZone(TimeZone.getTimeZone(gmt));
        return fmt.format(new Date());
    }

    public ITick getLastTick(Instrument instrument) {
        try {
            return (context.getHistory().getTick(instrument, 0));
        } catch (JFException e) {
             e.printStackTrace();
         }
         return null;
    }
}
