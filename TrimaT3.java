package liveStrategies;

import com.dukascopy.api.Configurable;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static liveStrategies.common.Util.getFormattedTime;
import liveStrategies.options.MenuStrategy;

/**
 *
 * @author rescorsim
 */
public class TrimaT3  implements IStrategy{

    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IChart chart;
    private IOrder order;
    
    @Configurable("defaultSlippage:")
    public int defaultSlippage = 5;
    @Configurable("defaultTakeProfit:")
    public int defaultTakeProfit = 350;
    @Configurable("defaultPeriod:")
    public Period defaultPeriod = Period.ONE_HOUR;
    @Configurable("defaultTradeAmount:")
    public double defaultTradeAmount = 0.01;
    @Configurable("defaultStopLoss:")
    public int defaultStopLoss = 250;
    @Configurable("defaultInstrument:")
    public Instrument defaultInstrument = Instrument.EURUSD;

    private List<IOrder> PendingPositions =  null ;
    private List<IOrder> AllPositions =  null ;
    private List<IOrder> OpenPositions =  null ;
    
    private int labelCounter = 1;
    private double bidPrice;
    private double askPrice;
    int indicatorPeriodFast = 5;
    int indicatorPeriodSlow = 83;
    double vfactor = 0.7;

    public TrimaT3() {

    }
    
    public static void main(String[] args) throws Exception {
        MenuStrategy menuStrategy = new MenuStrategy(new TrimaT3());
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
        try {
            
            //long time = context.getHistory().getBar(defaultInstrument, defaultPeriod, OfferSide.BID, 1).getTime(); 
            
            double[] valuesSlow = context.getIndicators().trima(
                    defaultInstrument, defaultPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 
                    indicatorPeriodSlow, Filter.WEEKENDS, 2, bidBar.getTime(), 0);
            
            double[] valuesFast = context.getIndicators().t3(
                    defaultInstrument, defaultPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 
                    indicatorPeriodFast, vfactor, Filter.WEEKENDS, 3, bidBar.getTime(), 0);
            
            console.getOut().println("NewBar-> valuesFast[0]:" + valuesFast[0]+
                    ", valuesFast[1]: "+valuesFast[1]+
                    ", valuesFast[2]: "+valuesFast[2]+
                    ", valuesSlow[0]: "+valuesSlow[0]+
                    ", valuesSlow[1]: "+valuesSlow[1]
                );
            
            if(valuesFast[0] <= valuesSlow[0] && valuesFast[1] > valuesSlow[1]){ // fast up
                updateVariables(defaultInstrument);
                
                boolean open = true;
                for (IOrder order : AllPositions) {
                    if (order.getInstrument() == defaultInstrument) {
                        if (order.getOrderCommand() == IEngine.OrderCommand.SELL) {
                            order.close();
                        } else if (order.getOrderCommand() == IEngine.OrderCommand.BUY) {
                            open = false;
                        }

                    }
                }
                
                if(open){
                    double stoploss = bidPrice - defaultInstrument.getPipValue() * defaultStopLoss;
                    double takeprofit = bidPrice + defaultInstrument.getPipValue() * defaultTakeProfit;
                    IOrder myOrder = engine.submitOrder(
                        this.getIntrumentLabel(defaultInstrument), // String label
                        defaultInstrument, //Instrument instrument
                        IEngine.OrderCommand.BUY, // IEngine.OrderCommand orderCommand
                        defaultTradeAmount, // double amount
                        askPrice, // double price
                        defaultSlippage, // double slippage
                        stoploss, // double stopLossPrice
                        takeprofit); // double takeProfitPrice
                }
 
            }
            else if(valuesFast[1] >= valuesSlow[0] && valuesFast[2] < valuesSlow[1]){ // fast down
                updateVariables(defaultInstrument);
                
                boolean open = true;
                for (IOrder order : AllPositions) {
                    if (order.getInstrument() == defaultInstrument) {
                        if (order.getOrderCommand() == IEngine.OrderCommand.BUY) {
                            order.close();
                        } else if (order.getOrderCommand() == IEngine.OrderCommand.SELL) {
                            open = false;
                        }

                    }
                }
                
                if(open){
                    double stoploss = askPrice + defaultInstrument.getPipValue() * defaultStopLoss;
                    double takeprofit = askPrice - defaultInstrument.getPipValue() * defaultTakeProfit;
                    IOrder myOrder = engine.submitOrder(
                        this.getIntrumentLabel(defaultInstrument), // String label
                        defaultInstrument, //Instrument instrument
                        IEngine.OrderCommand.SELL, // IEngine.OrderCommand orderCommand
                        defaultTradeAmount, // double amount
                        bidPrice, // double price
                        defaultSlippage, // double slippage
                        stoploss, // double stopLossPrice
                        takeprofit); // double takeProfitPrice
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
            List<IOrder> listMarket = new ArrayList<IOrder>();
            for (IOrder order: AllPositions) {
                if (order.getState().equals(IOrder.State.FILLED)){
                    listMarket.add(order);
                }
            }
            List<IOrder> listPending = new ArrayList<IOrder>();
            for (IOrder order: AllPositions) {
                if (order.getState().equals(IOrder.State.OPENED)){
                    listPending.add(order);
                }
            }
            OpenPositions = listMarket;
            PendingPositions = listPending;
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
}
