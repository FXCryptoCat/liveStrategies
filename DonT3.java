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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import liveStrategies.options.MenuStrategy;

/**
 *
 * @author rescorsim
 */
public class DonT3  implements IStrategy{

    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IChart chart;
    private IOrder order;
    
    //@Configurable("defaultSlippage:")
    public int defaultSlippage = 5;
    //@Configurable("defaultTakeProfit:")
    public int defaultTakeProfit = 300;
    //@Configurable("defaultPeriod:")
    public Period defaultPeriod = Period.FIFTEEN_MINS;
    //@Configurable("defaultTradeAmount:")
    public double defaultTradeAmount = 0.1;
    //@Configurable("defaultStopLoss:")
    public int defaultStopLoss = 50;
    //@Configurable("defaultInstrument:")
    public Instrument defaultInstrument = Instrument.EURUSD;

    // private
    private List<IOrder> AllPositions =  null ;
    private int labelCounter = 1;
    private double bidPrice;
    private double askPrice;
    
    // public
    public int t3Period = 13;
    public double vfactor = 0.7;
    public int donchianPeriod = 20;
    public int maxDeltaPips = 75;
    public int deltaPips = 40; 
    public int minDeltaPips = 33;
    public boolean revertDelta = false;
    public double revertStoploss = 20;
    public double revertTakeprofit = 30;

    public DonT3() {

    }
    
    public static void main(String[] args) throws Exception {
        MenuStrategy menuStrategy = new MenuStrategy(new DonT3());
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
        
        Set subscribedInstruments = new HashSet();
        subscribedInstruments.add(defaultInstrument);
        this.context.setSubscribedInstruments(subscribedInstruments); 
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
        if (!instrument.equals(defaultInstrument)) return;   
    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if (!instrument.equals(defaultInstrument) || period != defaultPeriod) return;   
        if (askBar.getVolume() == 0) return;
        askPrice = askBar.getClose();
        bidPrice = bidBar.getClose();
        double tradeAmount = context.getAccount().getEquity() / 30000;
        
        try {
            double[] valuesT3 = context.getIndicators().t3(
                    defaultInstrument, defaultPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 
                    t3Period, vfactor, Filter.WEEKENDS, 3, bidBar.getTime(), 0);
            
            double[][] valuesDonChin = context.getIndicators().donchian(
                    defaultInstrument, defaultPeriod, OfferSide.BID, 
                    this.donchianPeriod, Filter.WEEKENDS, 2, bidBar.getTime(), 0);
            
                        
            double currentVolatilityInPips = valuesDonChin[0][1] - valuesDonChin[1][1];
            boolean isVolatility = currentVolatilityInPips > (deltaPips*defaultInstrument.getPipValue());
            
            boolean isMinVolatility = currentVolatilityInPips < (minDeltaPips*defaultInstrument.getPipValue()); 
            boolean isMaxVolatility = currentVolatilityInPips > (maxDeltaPips*defaultInstrument.getPipValue()); 
            
//            console.getOut().println("NewBar-> valuesFast[0]:" + valuesT3[0]+
//                    ", valuesFast[1]: "+valuesT3[1]+
//                    ", valuesFast[2]: "+valuesT3[2]+
//                    ", valuesDonChin[0][0]: "+valuesDonChin[0][0]+
//                    ", valuesDonChin[1][0]: "+valuesDonChin[1][0]+
//                    ", valuesDonChin[0][1]: "+valuesDonChin[0][1]+
//                    ", valuesDonChin[1][1]: "+valuesDonChin[1][1]
//                );
            
            if(valuesT3[1] <= valuesT3[0] && valuesT3[2] > valuesT3[1]){ // BEAR
                updateVariables(defaultInstrument);
                
                boolean open = true;
                for (IOrder order : AllPositions) {
                    if (order.getInstrument() == defaultInstrument) {
                        order.close();
                    }
                }
                if(isMinVolatility  || isMaxVolatility){
                    open = false;
                }
                
                if(open){
                    if(isVolatility){
                        double stoploss = bidPrice - defaultInstrument.getPipValue() * defaultStopLoss;
                        double takeprofit = bidPrice + defaultInstrument.getPipValue() * defaultTakeProfit;
                        IOrder myOrder = engine.submitOrder(
                            this.getIntrumentLabel(defaultInstrument), // String label
                            defaultInstrument, //Instrument instrument
                            IEngine.OrderCommand.BUY, // IEngine.OrderCommand orderCommand
                            tradeAmount, // double amount
                            askPrice, // double price
                            defaultSlippage, // double slippage
                            stoploss, // double stopLossPrice
                            takeprofit); // double takeProfitPrice
                    }
                    else if(this.revertDelta){
                        double stoploss = bidPrice + defaultInstrument.getPipValue() * revertStoploss;
                        double takeprofit = bidPrice - defaultInstrument.getPipValue() * revertTakeprofit;
                        IOrder myOrder = engine.submitOrder(
                            this.getIntrumentLabel(defaultInstrument), // String label
                            defaultInstrument, //Instrument instrument
                            IEngine.OrderCommand.SELL, // IEngine.OrderCommand orderCommand
                            tradeAmount, // double amount
                            askPrice, // double price
                            defaultSlippage, // double slippage
                            stoploss, // double stopLossPrice
                            takeprofit); // double takeProfitPrice
                    }
                }
 
            }
            else if(valuesT3[1] >= valuesT3[0] && valuesT3[2] < valuesT3[1]){ // BEAR
                updateVariables(defaultInstrument);
                
                boolean open = true;
                for (IOrder order : AllPositions) {
                    if (order.getInstrument() == defaultInstrument) {
                        order.close();
                    }
                }
                
                if(isMinVolatility  || isMaxVolatility){
                    open = false;
                }
                
                if(open){
                    if(isVolatility){
                        double stoploss = askPrice + defaultInstrument.getPipValue() * defaultStopLoss;
                        double takeprofit = askPrice - defaultInstrument.getPipValue() * defaultTakeProfit;
                        IOrder myOrder = engine.submitOrder(
                            this.getIntrumentLabel(defaultInstrument), // String label
                            defaultInstrument, //Instrument instrument
                            IEngine.OrderCommand.SELL, // IEngine.OrderCommand orderCommand
                            tradeAmount, // double amount
                            bidPrice, // double price
                            defaultSlippage, // double slippage
                            stoploss, // double stopLossPrice
                            takeprofit); // double takeProfitPrice
                    }
                    else if(this.revertDelta){
                        double stoploss = bidPrice - defaultInstrument.getPipValue() * revertStoploss;
                        double takeprofit = bidPrice + defaultInstrument.getPipValue() * revertTakeprofit;
                        IOrder myOrder = engine.submitOrder(
                            this.getIntrumentLabel(defaultInstrument), // String label
                            defaultInstrument, //Instrument instrument
                            IEngine.OrderCommand.BUY, // IEngine.OrderCommand orderCommand
                            tradeAmount, // double amount
                            askPrice, // double price
                            defaultSlippage, // double slippage
                            stoploss, // double stopLossPrice
                            takeprofit); // double takeProfitPrice
                    }
                }
            
            }
                    
        } 
        catch (Exception e) {
            e.printStackTrace();
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
    
    protected void updateVariables(Instrument instrument) {
        try {
            AllPositions = engine.getOrders(instrument);
        } catch(JFException e) {
            e.printStackTrace();
        }
    }
    
    public String getIntrumentLabel (Instrument instrument) throws JFException{
        String label = instrument.toString();
        label = label.replace("/", "");
        label += getFormattedTime("_ddMMyyyy_HHmmss_SSS_","GMT-03:00");
        label += labelCounter;
        labelCounter++;
        return  label;
    }
    public String getFormattedTime(String format, String gmt){
        SimpleDateFormat fmt = new SimpleDateFormat(format);
        fmt.setTimeZone(TimeZone.getTimeZone(gmt));
        return fmt.format(new Date());
    }
}
