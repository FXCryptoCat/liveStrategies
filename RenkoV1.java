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
import com.dukascopy.api.PriceRange;
import com.dukascopy.api.feed.IFeedDescriptor;
import com.dukascopy.api.feed.IFeedListener;
import com.dukascopy.api.feed.IRenkoBar;
import com.dukascopy.api.feed.util.RenkoFeedDescriptor;
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
public class RenkoV1 implements IStrategy {

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

    public static Period defaultPeriod = Period.ONE_MIN;
    public boolean useTick = false;


    private static Double initialDeposit = 10000.0;
    private static Set<Instrument> instruments = null;

    private boolean useInstrumentList = true;

    private Calendar calendar;
    private List<IOrder> positions = null;
    private String name = "renkoV1";
    private int labelCounter = 1;
    private double bidPrice;
    private double askPrice;
    
    public RenkoCtrlV1 renkoCtrl1;
    public PriceRange r1_priceRange = PriceRange.valueOf(2); 
    
    public RenkoCtrlV1 renkoCtrl2;
    public PriceRange r2_priceRange = PriceRange.valueOf(10); 

    public boolean trailingStopActive = false;
    public int trailingStop = 15;
    
    public int maxOrders = 1;

    public Map<DayTime, DayTime> allowedIntervals;
    public Map<DayTime, DayTime> deniedIntervals;
    public boolean isCheckIntervals = true;

    public RenkoV1() throws ParseException {
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
    }

    public static void main(String[] args) throws Exception {
        MenuStrategy menuStrategy = new MenuStrategy(new RenkoV1(), instruments, initialDeposit, defaultPeriod);
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
        this.renkoCtrl1 = new RenkoCtrlV1(Instrument.EURUSD, r1_priceRange);
        this.renkoCtrl2 = new RenkoCtrlV1(Instrument.EURUSD, r2_priceRange);
        calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT 0"));
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
        if(this.useTick){
            proccessRenkoBar(instrument);
        }
    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if (this.useTick || askBar.getVolume() == 0) {
            return;
        }
        for (Instrument i : instruments) {
            if (i.equals(instrument) && period == defaultPeriod) {
                proccessRenkoBar(instrument);
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

    public void proccessRenkoBar(Instrument instrument) {
        if (this.trailingStopActive) {
            try {
                this.trailingStop(instrument);
            } catch (JFException ex) {
                Logger.getLogger(Temamet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try{       
            if(isEndFriday(getCurrentTimeStamp())){
                closeAllOrders(instrument);
                return;
            }
            
            boolean validInterval = !isCheckIntervals || (isCheckIntervals && 
                        Util.checkBetweenInterval(getCurrentTimeStamp(), allowedIntervals, false) &&
                        !Util.checkBetweenInterval(getCurrentTimeStamp(), deniedIntervals, false));
            
            if (!validInterval) {
                closeAllOrders(instrument);
                return;
            } 
            
            this.renkoCtrl1.update(instrument);
            this.renkoCtrl2.update(instrument);
            
            checkOrdersToClose(instrument);
            
            if(!validateMaxOrders(instrument)){
                return;
            }
            checkOrdersToOpen(instrument);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void checkOrdersToClose(Instrument instrument) throws JFException {
        positions = this.engine.getOrders(instrument); 
        for (IOrder order : positions) {
            if (order.getInstrument() == instrument) {
                if (order.isLong()) {
                    // if (temaT2.signal_bullish_end) {
                    //     order.close();
                    // }
                } else {
                    // if (temaT2.signal_bearish_end) {
                    //     order.close();
                    // }
                }
            }
        }
    }
    
    public void checkOrdersToOpen(Instrument instrument) throws JFException {
        askPrice = this.getLastTick(instrument).getAsk();
        bidPrice = this.getLastTick(instrument).getBid();
        
        double tradeAmount = context.getAccount().getEquity() / 15000;
        boolean openBuy = false;
        boolean openSell = false;
        
//        if(this.temaT1.trend == 0){
//            openBuy = this.temaT2.signal_bullish_begin;
//            openSell = this.temaT2.signal_bearish_begin;
//        }
//        else if(this.temaT1.trend > 0){
//            openBuy = this.temaT2.signal_bullish_begin || this.temaT2.signal_bull_begin;
//        }
//        else if(this.temaT1.trend < 0){
//            openSell = this.temaT2.signal_bearish_begin || this.temaT2.signal_bear_begin;
//        }

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
    
    public Long getCurrentTimeStamp() {
        return new Date().getTime();
    }

    public class RenkoCtrlV1 {

        public RenkoCtrlV1(Instrument i, PriceRange priceRange) {
            this.barSize = barSize;
            this.trend = 0;
            this.signal_bull_begin = this.signal_bullish_begin = signal_bullish_end = false;
            this.signal_bear_begin = this.signal_bearish_begin = signal_bearish_end = false;
            this.fd =  new RenkoFeedDescriptor(
                i, 
                priceRange, 
                OfferSide.BID
            );
        }
        public RenkoFeedDescriptor fd;
        java.util.List<IRenkoBar> bars = null;

        // imput
        public double barSize;
        
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

            IRenkoBar renkoBar = (IRenkoBar)history.getFeedData(fd, 1);
            bars = history.getFeedData(fd, 3, renkoBar.getTime(), 0);


            // double[] values = context.getIndicators().tema(
            //         instrument, this.period, OfferSide.BID, IIndicators.AppliedPrice.CLOSE,
            //         temaPeriodFast, Filter.WEEKENDS, 3, context.getHistory().getTimeOfLastTick(instrument), 0); 

            // double trailInPips = trail * instrument.getPipValue();
            // double minDiffInPips = diffOpen * instrument.getPipValue();
            // double targetDiffInPips = diffTarget * instrument.getPipValue();

            // double diffLast = (values[1] - values[0]);
            // double diff = (values[2] - values[1]);
            
            // signal_bull_begin = signal_bullish_begin = signal_bullish_end = false;
            // signal_bear_begin = signal_bearish_begin = signal_bearish_end = false;
            
            // if(diff > max_bull_diff){
            //     max_bull_diff = diff;
            // }
            // else if(diff < max_bear_diff){
            //     max_bear_diff = diff;
            // }
            
            // if (diff < 0 && diff < (max_bull_diff - trailInPips)) {
            //     signal_bullish_end = true;
            // }
            // else if (diff > 0 && diff > (max_bear_diff + trailInPips)) {
            //     signal_bearish_end = true;
            // }  
            
            // if (diffLast >= (max_bull_diff-targetDiffInPips) && diff < (max_bull_diff-targetDiffInPips)) {
            //     if(max_bull_diff > minDiffInPips){
            //         signal_bearish_begin = signal_bullish_end = true;
            //         max_bull_diff = diff;
            //         max_bear_diff = diff;
            //     }
            //     else if(diff > 0){
            //         signal_bear_begin = true;
            //         max_bull_diff = diff;
            //         max_bear_diff = diff;
            //     }
            // } 
            // else if (diffLast <= (max_bear_diff+targetDiffInPips) && diff > (max_bear_diff + targetDiffInPips)) {
            //     if(max_bear_diff < -minDiffInPips){
            //         signal_bullish_begin = signal_bearish_end = true;
            //         max_bull_diff = diff;
            //         max_bear_diff = diff;
            //     }
            //     else if(diff < 0){
            //         signal_bull_begin = true;
            //         max_bull_diff = diff;
            //         max_bear_diff = diff;
            //     }
            // }
        }
    }
}
