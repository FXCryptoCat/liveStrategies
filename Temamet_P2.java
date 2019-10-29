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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import liveStrategies.common.Account;
import liveStrategies.common.Util;
import liveStrategies.common.Util.DayTime;
import liveStrategies.options.MenuStrategy;

/**
 *
 * @author rescorsim
 */
public class Temamet_2period_V2 implements IStrategy {

    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IChart chart;
    private IOrder order;

    //@Configurable("defaultSlippage:")
    public int defaultSlippage = 5;
    //@Configurable("defaultTradeAmount:")
    public double defaultTradeAmount = 0.1;
    //@Configurable("defaultTakeProfit:")
    public int defaultTakeProfit = 40;
    //@Configurable("defaultStopLoss:")
    public int defaultStopLoss = 20;
    //@Configurable("defaultInstrument:")
    //public Instrument defaultInstrument = Instrument.EURUSD;

    //@Configurable("defaultPeriod:")
    public static Period defaultFastPeriod = Period.TEN_SECS;
    public static Period defaultSlowPeriod = Period.FIFTEEN_MINS;

    private static Double initialDeposit = 10000.0;
    private static Set<Instrument> instruments = null;
    private static Set<Period> periods = null;

    private boolean useInstrumentList = true;
    private boolean usePeriodList = false;

    // private
    private Calendar calendar;
    private List<IOrder> positions = null;
    private String name = "tematet";
    private int labelCounter = 1;
    private double bidPrice;
    private double askPrice;
    
    public TemaPosition tema15mFast;
    public TemaPosition tema15mSlow;
    public TemaPosition tema10s1, tema10s2, tema10s3, tema10s4, tema10s5;

    //public
    public int slowTemaPeriod = 100;
    public int fastTemaPeriod = 120;

    public boolean trailingStopActive = false;
    public int trailingStop = 45;

    public int maxPipsLastCandle = 70;
    public int minPipsLastCandle = 0;
    public int minCandlesForNewOrder = 0;

    public Map<DayTime, DayTime> allowedIntervals;
    public Map<DayTime, DayTime> deniedIntervals;
    public boolean isCheckIntervals = false;

    public Temamet_2period_V2() throws ParseException {
        allowedIntervals = new HashMap<>();
        allowedIntervals.put(new DayTime(0, 0, 0), new DayTime(13, 00, 0));
        allowedIntervals.put(new DayTime(22, 0, 0), new DayTime(23, 59, 59));
        deniedIntervals = new HashMap<>();
        //deniedIntervals.put(new DayTime(8,20,0), new DayTime(13,50,0));

        if (useInstrumentList) {
            instruments = new HashSet<Instrument>();
            instruments.add(Instrument.EURUSD);
            //instruments.add(Instrument.GBPUSD);
            //instruments.add(Instrument.USDJPY);
        }
        if (usePeriodList) {
            periods = new HashSet<Period>();
        }
    }

    public static void main(String[] args) throws Exception {
        MenuStrategy menuStrategy = new MenuStrategy(new Temamet_2period_V2(), instruments, initialDeposit, defaultSlowPeriod);
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
        this.tema10s1 = new TemaPosition(defaultFastPeriod, fastTemaPeriod, 0.1);
        this.tema10s2 = new TemaPosition(defaultFastPeriod, slowTemaPeriod, 0.2);
        this.tema10s3 = new TemaPosition(defaultFastPeriod, slowTemaPeriod, 0.3);
        this.tema10s4 = new TemaPosition(defaultFastPeriod, slowTemaPeriod, 0.4);
        this.tema10s5 = new TemaPosition(defaultFastPeriod, slowTemaPeriod, 0.5);
        this.tema15mFast = new TemaPosition(defaultSlowPeriod, fastTemaPeriod, 2.0);
        this.tema15mSlow = new TemaPosition(defaultSlowPeriod, slowTemaPeriod, 1.0);
        calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT 0"));
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
        //if (!instrument.equals(defaultInstrument)) {
            return;
        //}

    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if (askBar.getVolume() == 0) {
            return;
        }
        for (Instrument i : instruments) {
            if (i.equals(instrument) && period == defaultFastPeriod) {
                proccessBarFastPeriod(i, period, askBar, bidBar);
            }
            else if (i.equals(instrument) && period == defaultSlowPeriod) {
                proccessBarSlowPeriod(i, period, askBar, bidBar);
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

    public void proccessBarFastPeriod(Instrument instrument, Period period, IBar askBar, IBar bidBar) {
        if (this.trailingStopActive) {
            try {
                this.trailingStop(instrument);
            } catch (JFException ex) {
                Logger.getLogger(Temamet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try{
            
            if(isEndFriday(bidBar.getTime())){
                closeAll(instrument);
                return;
            }

            boolean continueToOpen = false;
            boolean continueToClose = false;  

            if(/*(this.tema15mSlow.trend > 1) && */(this.tema15mFast.trend > 1) ||
                /*(this.tema15mSlow.trend < -1) && */(this.tema15mFast.trend < -1))
                continueToOpen = true;

            positions = this.engine.getOrders(instrument);
            if(positions.size() > 0)
                continueToClose = true;

            if(!continueToOpen && !continueToClose)
                return;
            
            //this.tema10s1.update(instrument);
            this.tema10s2.update(instrument);
            this.tema10s3.update(instrument);
            this.tema10s4.update(instrument);
            this.tema10s5.update(instrument);
            
            for (IOrder order : positions) {
                if (order.getInstrument() == instrument) {
                    if(order.isLong()){
                        if(//tema10s1.signal_bullish_end || 
                           tema10s2.signal_bullish_end || 
                           tema10s3.signal_bullish_end || 
                           tema10s4.signal_bullish_end || 
                           tema10s5.signal_bullish_end)
                            order.close();
                    }
                    else{
                        if(//tema10s1.signal_bearish_end ||
                           tema10s2.signal_bearish_end ||
                           tema10s3.signal_bearish_end ||
                           tema10s4.signal_bearish_end ||
                           tema10s5.signal_bearish_end)
                            order.close();
                    }
                }
            }

            // if (!validateLastCandle(instrument)) {
            //     return;
            // }
            
//            positions = this.engine.getOrders(instrument);
//            if(positions.size() > 0)
//                return;

            if (!isCheckIntervals || ((isCheckIntervals && Util.checkBetweenInterval(askBar.getTime(), allowedIntervals, true))
                    && (isCheckIntervals && !Util.checkBetweenInterval(askBar.getTime(), deniedIntervals, true)))) {

                askPrice = askBar.getClose();
                bidPrice = bidBar.getClose();
                double tradeAmount = context.getAccount().getEquity() / 20000;

                if(this.tema15mFast.trend > 1 && (//this.tema10s1.signal_bearish_end ||
                        this.tema10s2.signal_bearish_end ||
                        this.tema10s3.signal_bearish_end ||
                        this.tema10s4.signal_bearish_end ||
                        this.tema10s5.signal_bearish_end)){
                    double stoploss = bidPrice - instrument.getPipValue() * defaultStopLoss;
                    double takeprofit = bidPrice + instrument.getPipValue() * defaultTakeProfit;
                    IOrder myOrder = engine.submitOrder(
                            this.getIntrumentLabel(instrument, name), // String label
                            instrument, //Instrument instrument
                            IEngine.OrderCommand.BUY, // IEngine.OrderCommand orderCommand
                            tradeAmount, // double amount
                            askPrice, // double price
                            defaultSlippage, // double slippage
                            stoploss, // double stopLossPrice
                            takeprofit); // double takeProfitPrice
                }

                if(this.tema15mFast.trend < -1 && (//this.tema10s1.signal_bullish_end ||
                        this.tema10s2.signal_bullish_end ||
                        this.tema10s3.signal_bullish_end ||
                        this.tema10s4.signal_bullish_end ||
                        this.tema10s5.signal_bullish_end)){
                    double stoploss = askPrice + instrument.getPipValue() * defaultStopLoss;
                    double takeprofit = askPrice - instrument.getPipValue() * defaultTakeProfit;
                    IOrder myOrder = engine.submitOrder(
                            this.getIntrumentLabel(instrument, name), // String label
                            instrument, //Instrument instrument
                            IEngine.OrderCommand.SELL, // IEngine.OrderCommand orderCommand
                            tradeAmount, // double amount
                            bidPrice, // double price
                            defaultSlippage, // double slippage
                            stoploss, // double stopLossPrice
                            takeprofit); // double takeProfitPrice
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void proccessBarSlowPeriod(Instrument instrument, Period period, IBar askBar, IBar bidBar) {
        try {
            this.tema15mFast.update(instrument);
            this.tema15mSlow.update(instrument);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void TakeProfitControl() {

    }

    public boolean validateLastCandle(Instrument instrument) {
        IBar prevBar;
        try {
            prevBar = history.getBar(instrument, defaultFastPeriod, OfferSide.BID, 1);
            double candleSize = prevBar.getHigh() - prevBar.getLow();
            double maxLastCandle = maxPipsLastCandle * instrument.getPipValue();
            double minLastCandle = minPipsLastCandle * instrument.getPipValue();
            if (candleSize >= minLastCandle && candleSize <= maxLastCandle) {
                return true;
            }
        } catch (JFException ex) {
            Logger.getLogger(Temamet.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public boolean isEndFriday(long barTime){
        calendar.setTimeInMillis(barTime);
        int day_of_week = calendar.get(Calendar.DAY_OF_WEEK);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (day_of_week == Calendar.FRIDAY && hour >= 16)
            return true;
        else
            return false;
    }

    public void closeAll(Instrument instrument) throws JFException{
        positions = this.engine.getOrders(instrument);
        for (IOrder order : positions) {
            if (order.getInstrument() == instrument) {
                order.close();
            }
        }
    }

    protected void trailingStop(Instrument instrument) throws JFException {
        for (IOrder order : engine.getOrders(instrument)) {
            if (order.getState() == IOrder.State.FILLED) {
                double profirLoss = order.getProfitLossInPips();
                console.getOut().println("trailingStop -> profirLossInPips: " + profirLoss
                        + ", trailingStopInPips: " + trailingStop);
//                if (order.getProfitLossInPips() > trailingStop) {
//                        //closePartially(order);
//                        order.close();
//                        console.getOut().println("trailingStop -> CLOSE ORDER");
//                }
//                else{
                if (order.isLong()) {
                    double newStopLossAsk = (this.getLastTick(instrument).getAsk() - trailingStop * instrument.getPipValue());
                    double newStopLoss = roundToPippette(newStopLossAsk, instrument);
                    double currentStopLossPrice = order.getStopLossPrice();
                    console.getOut().println("trailingStopLong -> currentStopLossPrice: " + currentStopLossPrice
                            + " < newStopLoss: " + newStopLoss);
                    if (currentStopLossPrice < newStopLoss) {
                        order.setStopLossPrice(newStopLoss);
                    }
                } else {
                    double newStopLossBid = (this.getLastTick(instrument).getBid() + trailingStop * instrument.getPipValue());
                    double newStopLoss = roundToPippette(newStopLossBid, instrument);
                    double currentStopLoss = order.getStopLossPrice();
                    console.getOut().println("trailingStopShort -> currentStopLoss: " + currentStopLoss
                            + " > newStopLoss: " + newStopLoss);
                    if (currentStopLoss > newStopLoss) {
                        order.setStopLossPrice(newStopLoss);
                    }
                }
                //}
            }
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

    public class TemaPosition {

        public TemaPosition(Period period, int temaPeriod, double signalGap) {
            this.period = period;
            this.temaPeriod = temaPeriod;
            this.signalGap = signalGap;
            this.trend = 0;
            this.signal_bull_begin = this.signal_bullish_begin = this.signal_bullish_end = false;
            this.signal_bear_begin = this.signal_bearish_begin = this.signal_bearish_end = false;
        }
        // imput
        public Period period;
        public int temaPeriod;
        public double signalGap;
        //output
        public int trend;
        public boolean signal_bull_begin;
        public boolean signal_bear_begin;
        public boolean signal_bullish_begin;
        public boolean signal_bullish_end;
        public boolean signal_bearish_begin;
        public boolean signal_bearish_end;

        public void update(Instrument instrument) throws JFException {
            double[] values = context.getIndicators().tema(
                    instrument, this.period, OfferSide.BID, IIndicators.AppliedPrice.CLOSE,
                    slowTemaPeriod, Filter.WEEKENDS, 3, context.getHistory().getTimeOfLastTick(instrument), 0); 

            double signal = signalGap * instrument.getPipValue();

            double diffLast = (values[1] - values[0]);
            double diff = (values[2] - values[1]);
            
            signal_bull_begin = signal_bullish_begin = signal_bullish_end = false;
            signal_bear_begin = signal_bearish_begin = signal_bearish_end = false;

            if (diffLast <= signal && diff > signal) {
                signal_bullish_begin = true;
                trend = 2;
            } else if (diffLast >= signal && diff < signal) {
                signal_bullish_end = true;
                trend = 1;
            } else if (diffLast >= -signal && diff < -signal) {
                signal_bearish_begin = true;
                trend = -2;
            } else if (diffLast <= -signal && diff > -signal) {
                signal_bearish_end = true;
                trend = -1;
            } else if (diffLast <= 0 && diff > 0) { 
                signal_bull_begin = true;
                trend = 1;
            } else if (values[1] >= values[0] && values[2] < values[1]) { 
                signal_bear_begin = true;
                trend = -1;
            }
        }
    }
}
