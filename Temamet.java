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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import liveStrategies.common.Util;
import liveStrategies.common.Util.DayTime;
import liveStrategies.options.MenuStrategy;

/**
 *
 * @author rescorsim
 */
public class Temamet implements IStrategy {

    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IChart chart;
    private IOrder order;

    //@Configurable("defaultSlippage:")
    public int defaultSlippage = 5;
    //@Configurable("defaultPeriod:")
    public Period defaultPeriod = Period.FIFTEEN_MINS;
    //@Configurable("defaultTradeAmount:")
    public double defaultTradeAmount = 0.1;
    //@Configurable("defaultTakeProfit:")
    public int defaultTakeProfit = 300;
    //@Configurable("defaultStopLoss:")
    public int defaultStopLoss = 100;
    //@Configurable("defaultInstrument:")
    public Instrument defaultInstrument = Instrument.GBPUSD;

    private boolean usePeriodList = false;
    private static Set<Instrument> instruments = null;
    private List<Period> periods = null;

    // private
    private List<IOrder> AllPositions = null;
    private String name = "tematet";
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

    public boolean trailingStopActive = false;
    public Period trailingStopPeriod = Period.ONE_MIN;
    public int trailingStop = 45;


    public int maxPipsLastCandle = 70;
    public int minPipsLastCandle = 0;
    public int minCandlesForNewOrder = 0;
    public int candlesAfterTheLastOrder = 0;

    public Map<DayTime, DayTime> allowedIntervals;
    public Map<DayTime, DayTime> deniedIntervals;
    public boolean isCheckIntervals = true;

    public Temamet() throws ParseException {
        slowCrossUp_close = slowCrossDown_close = false;
        fastCrossUp_close = fastCrossDown_close = false;
        slowCrossUp_open = slowCrossDown_open = false;
        fastCrossUp_open = fastCrossDown_open = false;
        
        slowTrend = fastTrend = 0;
        allowedIntervals = new HashMap<>();
        allowedIntervals.put(new DayTime(0,0,0), new DayTime(14,00,0));
        allowedIntervals.put(new DayTime(22,0,0), new DayTime(23,59,59));
        deniedIntervals = new HashMap<>();
        //deniedIntervals.put(new DayTime(8,20,0), new DayTime(13,50,0));

        instruments = new HashSet<Instrument>();
        instruments.add(defaultInstrument);

        if(usePeriodList){
            periods = new ArrayList<Period>();
        }
    }

    public static void main(String[] args) throws Exception {
        MenuStrategy menuStrategy = new MenuStrategy(new Temamet(), instruments, 10000.0, Period.TEN_SECS);
        menuStrategy.showMenuStrategy();
    }

    @Override
    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        console.getOut().println("Strategy -> onStart()");

        this.context.setSubscribedInstruments(instruments);
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
        if (!instrument.equals(defaultInstrument)) {
            return;
        }

    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if (askBar.getVolume() == 0) {
            return;
        }
        if (instrument.equals(defaultInstrument) && period == defaultPeriod) {
            proccessBar(instrument, period, askBar, bidBar);
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
        if(this.trailingStopActive && this.trailingStopPeriod == period){
            try {
                this.trailingStop();
            } catch (JFException ex) {
                Logger.getLogger(Temamet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        candlesAfterTheLastOrder++;
        askPrice = askBar.getClose();
        bidPrice = bidBar.getClose();
        slowCrossUp_open = slowCrossDown_open = false;
        fastCrossUp_open = fastCrossDown_open = false;
        slowCrossUp_close = slowCrossDown_close = false;
        fastCrossUp_close = fastCrossDown_close = false;
        double tradeAmount = context.getAccount().getEquity() / 30000;

        try {
            double[] valuesSlow = context.getIndicators().tema(
                    defaultInstrument, defaultPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE,
                    slowTemaPeriod, Filter.WEEKENDS, 3, bidBar.getTime(), 0);

            double[] valuesFast = context.getIndicators().tema(
                    defaultInstrument, defaultPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE,
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

            if ((valuesSlow[1]- valuesSlow[0]) > valueToOpenSlow && (valuesSlow[2] - valuesSlow[1]) < valueToOpenSlow) { // BULL
                slowCrossUp_open = true;
                slowTrend = 1;
            } else if ((valuesSlow[0] - valuesSlow[1]) > valueToOpenSlow  && (valuesSlow[1] - valuesSlow[2]) < valueToOpenSlow) { // BEAR
                slowCrossDown_open = true;
                slowTrend = -1;
            }
            if ((valuesFast[1]- valuesFast[0]) > valueToOpenFast && (valuesFast[2] - valuesFast[1]) < valueToOpenFast) { // BULL
                fastCrossUp_open = true;
                fastTrend = 1;
            } else if ((valuesFast[0] - valuesFast[1]) > valueToOpenFast  && (valuesFast[1] - valuesFast[2]) < valueToOpenFast) { // BEAR
                fastCrossDown_open = true;
                fastTrend = -1;
            }
            
            if (valuesSlow[1] <= valuesSlow[0] && valuesSlow[2] > valuesSlow[1]) { // BULL
                slowCrossUp_close = true;
                slowTrend = 1;
            } else if (valuesSlow[1] >= valuesSlow[0] && valuesSlow[2] < valuesSlow[1]) { // BEAR
                slowCrossDown_close = true;
                slowTrend = -1;
            }
            if ((valuesFast[1]- valuesFast[0]) < valueToOpenFast && (valuesFast[2] - valuesFast[1]) > valueToOpenFast) { // BULL
                fastCrossUp_close = true;
                fastTrend = 1;
            } else if ((valuesFast[0] - valuesFast[1]) < valueToOpenFast  && (valuesFast[1] - valuesFast[2]) > valueToOpenFast) { // BEAR
                fastCrossDown_close = true;
                fastTrend = -1;
            }

            updateVariables(defaultInstrument);
            if (AllPositions.size() > 0 && (fastCrossUp_close || fastCrossDown_close)) {
                for (IOrder order : AllPositions) {
                    if (order.getInstrument() == defaultInstrument) {
                        order.close();
                    }
                }
            }

            if(!validateLastCandle()){
                return;
            }
            if(!validateDistanceToNewOrder()){
                return;
            }

            if(!isCheckIntervals || (
                        (isCheckIntervals && Util.checkBetweenInterval(askBar.getTime(), allowedIntervals, true)) &&
                        (isCheckIntervals && !Util.checkBetweenInterval(askBar.getTime(), deniedIntervals, true)) ))
            {
                if ((slowCrossUp_open /*&& fastTrend > 0*/) || (/*slowTrend > 0 &&*/ fastCrossUp_open)) {
                    double stoploss = bidPrice - defaultInstrument.getPipValue() * defaultStopLoss;
                    double takeprofit = bidPrice + defaultInstrument.getPipValue() * defaultTakeProfit;
                    IOrder myOrder = engine.submitOrder(
                            this.getIntrumentLabel(defaultInstrument, name), // String label
                            defaultInstrument, //Instrument instrument
                            IEngine.OrderCommand.BUY, // IEngine.OrderCommand orderCommand
                            tradeAmount, // double amount
                            askPrice, // double price
                            defaultSlippage, // double slippage
                            stoploss, // double stopLossPrice
                            takeprofit); // double takeProfitPrice
                    candlesAfterTheLastOrder = 0;
                }
                if ((slowCrossDown_open /*&& fastTrend < 0*/) || (/*slowTrend < 0 &&*/ fastCrossDown_open)) {
                    double stoploss = askPrice + defaultInstrument.getPipValue() * defaultStopLoss;
                    double takeprofit = askPrice - defaultInstrument.getPipValue() * defaultTakeProfit;
                    IOrder myOrder = engine.submitOrder(
                            this.getIntrumentLabel(defaultInstrument, name), // String label
                            defaultInstrument, //Instrument instrument
                            IEngine.OrderCommand.SELL, // IEngine.OrderCommand orderCommand
                            tradeAmount, // double amount
                            bidPrice, // double price
                            defaultSlippage, // double slippage
                            stoploss, // double stopLossPrice
                            takeprofit); // double takeProfitPrice
                    candlesAfterTheLastOrder = 0;
                }
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

    public boolean validateLastCandle() {
        IBar prevBar;
        try {
            prevBar = history.getBar(defaultInstrument, defaultPeriod, OfferSide.BID, 1);
            double candleSize = prevBar.getHigh() - prevBar.getLow();
            double maxLastCandle = maxPipsLastCandle * defaultInstrument.getPipValue();
            double minLastCandle = minPipsLastCandle * defaultInstrument.getPipValue();
            if(candleSize >= minLastCandle && candleSize <= maxLastCandle){
                return true;
            }
        } catch (JFException ex) {
            Logger.getLogger(Temamet.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    protected void trailingStop(/*ITick tick*/) throws JFException {
        for (IOrder order : engine.getOrders(defaultInstrument)) {
            if (order.getState() == IOrder.State.FILLED) {
                double profirLoss = order.getProfitLossInPips();
                console.getOut().println("trailingStop -> profirLossInPips: "+profirLoss+
                        ", trailingStopInPips: "+trailingStop);
//                if (order.getProfitLossInPips() > trailingStop) {
//                        //closePartially(order);
//                        order.close();
//                        console.getOut().println("trailingStop -> CLOSE ORDER");
//                }
//                else{
                    if (order.isLong()) {
                        double newStopLossAsk = (this.getLastTick(defaultInstrument).getAsk() - trailingStop * defaultInstrument.getPipValue());
                        double newStopLoss = roundToPippette(newStopLossAsk, defaultInstrument);
                        double currentStopLossPrice = order.getStopLossPrice();
                        console.getOut().println("trailingStopLong -> currentStopLossPrice: "+currentStopLossPrice+
                            " < newStopLoss: "+newStopLoss);
                        if (currentStopLossPrice < newStopLoss) {
                            order.setStopLossPrice(newStopLoss);
                        }
                    }
                    else{
                        double newStopLossBid = (this.getLastTick(defaultInstrument).getBid() + trailingStop * defaultInstrument.getPipValue());
                        double newStopLoss = roundToPippette(newStopLossBid, defaultInstrument);
                        double currentStopLoss = order.getStopLossPrice();
                        console.getOut().println("trailingStopShort -> currentStopLoss: "+currentStopLoss+
                            " > newStopLoss: "+newStopLoss);
                        if (currentStopLoss > newStopLoss) {
                            order.setStopLossPrice(newStopLoss);
                        }
                    }
                //}
            }
        }
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
