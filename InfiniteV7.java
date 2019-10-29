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
import liveStrategies.common.Util;
import liveStrategies.common.Util.DayTime;
import liveStrategies.options.MenuStrategy;

/**
 *
 * @author rescorsim
 */
public class InfiniteV7 implements IStrategy {

    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IChart chart;
    private IOrder order;

    //@Configurable("defaultSlippage:")
    public int defaultSlippage = 5;
    public double defaultTradeAmount = 0.1;
    public int defaultTakeProfit = 50;
    public int defaultStopLoss = 15;

    public static Period defaultFastPeriod = Period.ONE_MIN;
    public static Period defaultSlowPeriod = Period.THIRTY_MINS;

    private static Double initialDeposit = 10000.0;
    private static Set<Instrument> instruments = null;
    private static Set<Period> periods = null;

    private boolean useInstrumentList = true;
    private boolean usePeriodList = false;

    private Calendar calendar;
    private List<IOrder> positions = null;
    private String name = "tematet";
    private int labelCounter = 1;
    private double bidPrice;
    private double askPrice;
    
    public TemaPositionV2 tema15m;
    public double tp_signalGap1 = 0.6*4; // 0.8
    public double tp_signalGap2 = 1.0*4;
    
    public TemaTrailingV5 tema10s;
    public double tt_trail = 0.25*5 ;//2* 4;
    public double tt_diffOpen = 0.25*4;// 1.5* 4;
    public double tt_diffTarget = 0.05*4;

    public int temaPeriodFast = 50;//80
    public int temaPeriodSlow = 30;

    public boolean trailingStopActive = false;
    public int trailingStop = 15;

    public int maxPipsLastCandle = 70;
    public int minPipsLastCandle = 0;
    public int minCandlesForNewOrder = 0;
    
    public int maxOrders = 1;

    public Map<DayTime, DayTime> allowedIntervals;
    public Map<DayTime, DayTime> deniedIntervals;
    public boolean isCheckIntervals = true;

    public InfiniteV7() throws ParseException {
        allowedIntervals = new HashMap<>();
        allowedIntervals.put(new DayTime(6, 0, 0), new DayTime(15, 00, 0));
        //allowedIntervals.put(new DayTime(22, 0, 0), new DayTime(23, 59, 59));
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
        MenuStrategy menuStrategy = new MenuStrategy(new InfiniteV5(), instruments, initialDeposit, defaultSlowPeriod);
        menuStrategy.showMenuStrategy();
    }

    @Override
    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        console.getOut().println("Strategy InfiniteV5 -> onStart()");
        this.context.setSubscribedInstruments(instruments);
        this.tema10s = new TemaTrailingV5(defaultFastPeriod, temaPeriodFast, tt_trail, tt_diffOpen, tt_diffTarget);
        this.tema15m = new TemaPositionV2(defaultSlowPeriod, temaPeriodSlow, tp_signalGap1, tp_signalGap2);
        calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT 0"));
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
    
    public void proccessBarSlowPeriod(Instrument instrument, Period period, IBar askBar, IBar bidBar) {
        try {
            this.tema15m.update(instrument);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                closeAllOrders(instrument);
                return;
            }
            
            boolean validInterval = !isCheckIntervals || (isCheckIntervals && 
                        Util.checkBetweenInterval(askBar.getTime(), allowedIntervals, false) &&
                        !Util.checkBetweenInterval(askBar.getTime(), deniedIntervals, false));
            
            if (!validInterval) {
                closeAllOrders(instrument);
                return;
            } 
            
            this.tema10s.update(instrument);
            
            checkOrdersToClose(instrument);
            
            if(!validateMaxOrders(instrument)){
                return;
            }
            checkOrdersToOpen(instrument, askBar, bidBar);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void checkOrdersToClose(Instrument instrument) throws JFException {
        positions = this.engine.getOrders(instrument); 
        for (IOrder order : positions) {
            if (order.getInstrument() == instrument) {
                if (order.isLong()) {
                    if (tema10s.signal_bullish_end) {
                        order.close();
                    }
                } else {
                    if (tema10s.signal_bearish_end) {
                        order.close();
                    }
                }
            }
        }
    }
    
    public void checkOrdersToOpen(Instrument instrument, IBar askBar, IBar bidBar) throws JFException {
        askPrice = askBar.getClose();
        bidPrice = bidBar.getClose();
        double tradeAmount = context.getAccount().getEquity() / 15000;
        boolean openBuy = false;
        boolean openSell = false;
        
        if(this.tema15m.trend == 0){
            openBuy = this.tema10s.signal_bullish_begin;
            openSell = this.tema10s.signal_bearish_begin;
        }
        else if(this.tema15m.trend > 0){
            openBuy = this.tema10s.signal_bullish_begin || this.tema10s.signal_bull_begin;
        }
        else if(this.tema15m.trend < 0){
            openSell = this.tema10s.signal_bearish_begin || this.tema10s.signal_bear_begin;
        }

        if (openBuy){
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

        if (openSell){
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
    
        
    public void closeAllOrders(Instrument instrument) throws JFException{
        for (IOrder order : this.engine.getOrders(instrument)) {
            if (order.getInstrument() == instrument) {
                order.close();
            }
        }
    }
    
    public boolean validateMaxOrders(Instrument instrument) throws JFException{
        if (this.engine.getOrders(instrument).size() >= this.maxOrders){
            return false;
        }
        return true;
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

    protected void trailingStop(Instrument instrument) throws JFException {
        for (IOrder order : engine.getOrders(instrument)) {
            if (order.getState() == IOrder.State.FILLED) {
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

    public class TemaPositionV2 {

        public TemaPositionV2(Period period, int temaPeriod, double signalGap1, double signalGap2) {
            this.period = period;
            this.temaPeriod = temaPeriod;
            this.signalGap1 = signalGap1;
            this.signalGap2 = signalGap2;
            this.trend = 0;
            this.signal_bull_begin = signal_bull_end = false;
            this.signal_bear_begin = signal_bear_end = false;
        }
        // imput
        public Period period;
        public int temaPeriod;
        public double signalGap1;
        public double signalGap2;
        
        //output
        public int trend;
        public boolean signal_bull_begin;
        public boolean signal_bull_end;
        public boolean signal_bear_begin;
        public boolean signal_bear_end;

        public void update(Instrument instrument) throws JFException {
            double[] values = context.getIndicators().tema(
                    instrument, this.period, OfferSide.BID, IIndicators.AppliedPrice.CLOSE,
                    temaPeriodSlow, Filter.WEEKENDS, 3, context.getHistory().getTimeOfLastTick(instrument), 0); 

            double signal1 = signalGap1 * instrument.getPipValue();
            double signal2 = signalGap2 * instrument.getPipValue();

            double diffLast = (values[1] - values[0]);
            double diff = (values[2] - values[1]);
            
            signal_bull_begin = signal_bull_end = false;
            signal_bear_begin = signal_bear_end = false;

            if (diffLast <= signal1 && diff > signal1) {
                signal_bull_begin = true;
                trend = 1;
            } else if (diffLast >= signal1 && diff < signal1) {
                signal_bull_end = true;
                trend = 0;
            } 
            else if (diffLast <= signal2 && diff > signal2) {
                signal_bear_begin = true;
                trend = -1;
            } else if (diffLast >= signal2 && diff < signal2) {
                signal_bear_end = true;
                trend = 0;
            } 

            else if (diffLast >= -signal1 && diff < -signal1) {
                signal_bear_begin = true;
                trend = -1;
            } else if (diffLast <= -signal1 && diff > -signal1) {
                signal_bear_end = true;
                trend = 0;
            } 
            else if (diffLast >= -signal2 && diff < -signal2) {
                signal_bull_begin = true;
                trend = 1;
            } else if (diffLast <= -signal2 && diff > -signal2) {
                signal_bull_end = true;
                trend = 0;
            } 
        }
    }

    public class TemaTrailingV5 {

        public TemaTrailingV5(Period period, int temaPeriod, double trail, double diffOpen, double diffTarget) {
            this.period = period;
            this.temaPeriod = temaPeriod;
            this.trail = trail;
            this.diffOpen = diffOpen;
            this.diffTarget = diffTarget;
            this.trend = 0;
            this.signal_bull_begin = this.signal_bullish_begin = signal_bullish_end = false;
            this.signal_bear_begin = this.signal_bearish_begin = signal_bearish_end = false;
        }
        // imput
        public Period period;
        public int temaPeriod;
        public double trail;
        public double diffOpen;
        public double diffTarget;
        
        //output
        public int trend;
        public boolean signal_bull_begin;
        public boolean signal_bear_begin;
        public boolean signal_bullish_begin;
        public boolean signal_bearish_begin;
        public boolean signal_bullish_end;
        public boolean signal_bearish_end;
        public double max_bull_diff = 0;
        public double max_bear_diff = 0;

        public void update(Instrument instrument) throws JFException {
            double[] values = context.getIndicators().tema(
                    instrument, this.period, OfferSide.BID, IIndicators.AppliedPrice.CLOSE,
                    temaPeriodFast, Filter.WEEKENDS, 3, context.getHistory().getTimeOfLastTick(instrument), 0); 

            double trailInPips = trail * instrument.getPipValue();
            double minDiffInPips = diffOpen * instrument.getPipValue();
            double targetDiffInPips = diffTarget * instrument.getPipValue();

            double diffLast = (values[1] - values[0]);
            double diff = (values[2] - values[1]);
            
            signal_bull_begin = signal_bullish_begin = signal_bullish_end = false;
            signal_bear_begin = signal_bearish_begin = signal_bearish_end = false;
            
            if(diff > max_bull_diff){
                max_bull_diff = diff;
            }
            else if(diff < max_bear_diff){
                max_bear_diff = diff;
            }
            
            if (diff < 0 && diff < (max_bull_diff - trailInPips)) {
                signal_bullish_end = true;
            }
            else if (diff > 0 && diff > (max_bear_diff + trailInPips)) {
                signal_bearish_end = true;
            }  
            
            if (diffLast >= (max_bull_diff-targetDiffInPips) && diff < (max_bull_diff-targetDiffInPips)) {
                if(max_bull_diff > minDiffInPips){
                    signal_bearish_begin = signal_bullish_end = true;
                    max_bull_diff = diff;
                    max_bear_diff = diff;
                }
                else if(diff > 0){
                    signal_bear_begin = true;
                    max_bull_diff = diff;
                    max_bear_diff = diff;
                }
            } 
            else if (diffLast <= (max_bear_diff+targetDiffInPips) && diff > (max_bear_diff + targetDiffInPips)) {
                if(max_bear_diff < -minDiffInPips){
                    signal_bullish_begin = signal_bearish_end = true;
                    max_bull_diff = diff;
                    max_bear_diff = diff;
                }
                else if(diff < 0){
                    signal_bull_begin = true;
                    max_bull_diff = diff;
                    max_bear_diff = diff;
                }
            }
        }
    }
}
