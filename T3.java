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
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.util.Date;
import liveStrategies.T3v1;
import liveStrategies.options.MenuStrategy;

public class T3__v1  implements IStrategy{

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
    public int defaultTakeProfit = 0;
    @Configurable("defaultStopLoss:")
    public int defaultStopLoss = 50;
    @Configurable("defaultTradeAmount:")
    public double defaultTradeAmount = 0.001;
    @Configurable("defaultPeriod:")
    public Period defaultPeriod = Period.TICK;
    @Configurable("defaultInstrument:")
    public Instrument defaultInstrument = Instrument.GBPUSD;
    @Configurable("vfactor:")
    public double vfactor = 0.7; // 0.7
    @Configurable("indicatorPeriod:")
    public int indicatorPeriod = 13;
    @Configurable("pipsTrigger:")
    public int pipsTrigger = 0;
    
    @Configurable("openBuy:")
    public int openBuy = 1000;
    @Configurable("openSell:")
    public int openSell = 1000;

    private List<IOrder> PendingPositions =  null ;
    private List<IOrder> AllPositions =  null ;
    private List<IOrder> OpenPositions =  null ;
    
    private int labelCounter = 1;
    private double bidPrice;
    private double askPrice;
    
    private int countOpenBuy;
    private int countOpenSell;
    private boolean trigged;
    private double triggerUpPrice, triggerDownPrice;
    private double priceLastTick;
    private int lastDirection;

    public T3__v1() {

    }
    
    public static void main(String[] args) throws Exception {
        
        MenuStrategy menuStrategy = new MenuStrategy(new T3__v1(), Instrument.GBPUSD, 1000.00, Period.ONE_MIN);
        menuStrategy.showMenuStrategy();
    }
    
    @Override
    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        console.getOut().println("Strategy T3__ -> onStart()");
        
        Set subscribedInstruments = new HashSet();
        subscribedInstruments.add(defaultInstrument);
        this.context.setSubscribedInstruments(subscribedInstruments); 

        countOpenBuy = 0;
        countOpenSell = 0;
        priceLastTick = 0;
        lastDirection = 0;
        
        if(pipsTrigger > 0){
            trigged = false;
            ITick tick = this.history.getLastTick(defaultInstrument);
            triggerUpPrice = tick.getAsk() + (defaultInstrument.getPipValue() * pipsTrigger);
            triggerDownPrice = tick.getBid() - (defaultInstrument.getPipValue() * pipsTrigger);
            console.getOut().println("onStart -> Trigger ativado, triggerUpPrice: "+triggerUpPrice+", triggerDownPrice: "+triggerDownPrice);
        }
        else{
            trigged = true;
        }
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
        if (instrument.equals(defaultInstrument) && defaultPeriod.equals(Period.TICK)){
            askPrice = tick.getAsk();
            bidPrice = tick.getBid();
            this.executeT3(tick.getTime());
        }  
    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if (!instrument.equals(defaultInstrument) || period != defaultPeriod) return;   
        if (askBar.getVolume() == 0) return;
        askPrice = askBar.getClose();
        bidPrice = bidBar.getClose();
        this.executeT3(bidBar.getTime());
    }
    
    public void executeT3(long time){ 
        try {     
            if(pipsTrigger > 0 && !trigged){
                if(bidPrice > triggerUpPrice){
                    console.getOut().println("Gatilho de compra Ativado, triggerUpPrice: "+triggerUpPrice+", bidPrice: "+bidPrice);
                    trigged = true;
                    lastDirection = 1;
                    openOrder(IEngine.OrderCommand.BUY);
                }
                else if(askPrice < triggerDownPrice){
                    console.getOut().println("Gatilho de venda Ativado, triggerDownPrice: "+triggerDownPrice+", askPrice: "+askPrice);
                    trigged = true;
                    lastDirection = -1;
                    openOrder(IEngine.OrderCommand.SELL);
                }
                else{
                    //console.getOut().println("Aguardando gatilho, Price: "+bidPrice+", triggerDownPrice: "+triggerDownPrice+", triggerUpPrice: "+triggerUpPrice);
                }
                return;
            }
            
            double[] valuesT3;
            if(defaultPeriod.equals(Period.TICK)){
                //ITick tick = history.getTick(defaultInstrument, 2);
                valuesT3 = context.getIndicators().t3(
                        defaultInstrument, defaultPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 
                        indicatorPeriod, vfactor, time-1000, time);
                if(priceLastTick == valuesT3[0]) return;
                priceLastTick = valuesT3[0];      
            }
            else{
                valuesT3 = context.getIndicators().t3(
                        defaultInstrument, defaultPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 
                        indicatorPeriod, vfactor, Filter.WEEKENDS, 2, time, 0);
            }
                    
//                    console.getOut().println("executeT3-> valuesT3[0]:" + valuesT3[0]+
//                    ", valuesT3[1]: "+valuesT3[1]//+
//                   //", valuesT3[2]: "+valuesT3[2]
//                );
          
            if(valuesT3[0] < valuesT3[1] && lastDirection < 1){ // reversão para cima
                console.getOut().println("REVERSÃO DE ALTA");
                boolean open = true;
                lastDirection = 1;
                AllPositions = engine.getOrders(defaultInstrument);
                for (IOrder order : AllPositions) {
                    if (order.getInstrument() == defaultInstrument) {
                        if (order.getOrderCommand() == IEngine.OrderCommand.SELL) {
                            order.close();
                            console.getOut().println("Close SELL Order, askPrice: " + askPrice);
                        } 
                        else if(order.getOrderCommand() == IEngine.OrderCommand.BUY){
                            open = false;
                        }
                    }
                }
                
                if (open && countOpenBuy < openBuy) {
                    openOrder(IEngine.OrderCommand.BUY);
                }
 
            }
            else if(valuesT3[0] > valuesT3[1] && lastDirection > -1){ // reversão para baixo
                console.getOut().println("REVERSÃO DE BAIXA");
                boolean open = true;
                lastDirection = -1;
                AllPositions = engine.getOrders(defaultInstrument);
                for (IOrder order : AllPositions) {
                    if (order.getInstrument() == defaultInstrument) {
                        if (order.getOrderCommand() == IEngine.OrderCommand.BUY) {
                            order.close();
                            console.getOut().println("Close BUY Order, bidPrice: " + bidPrice);
                        }
                        else if(order.getOrderCommand() == IEngine.OrderCommand.SELL){
                            open = false;
                        }
                    }
                }
                if (open && countOpenSell < openSell) {
                    openOrder(IEngine.OrderCommand.SELL);
                }
            
            }
                    
        } 
        catch (Exception e) {
            e.printStackTrace();
            console.getOut().println("Error: "+e.toString());
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
        AllPositions = engine.getOrders(defaultInstrument);
        for (IOrder order : AllPositions) {
             if (order.getInstrument() == defaultInstrument) {
                 if (order.getOrderCommand() == IEngine.OrderCommand.BUY) {
                     order.close();
                     console.getOut().println("onStop -> Close BUY Order");
                 }
                 else if(order.getOrderCommand() == IEngine.OrderCommand.SELL){
                     order.close();
                     console.getOut().println("onStop -> Close SELL Order");
                 }
            }
        }
        
    }
    
    protected void openOrder(IEngine.OrderCommand orderCommand) throws JFException{
        double stoploss = 0, takeprofit = 0, price = 0;
        if(orderCommand == IEngine.OrderCommand.SELL){
            stoploss = defaultStopLoss != 0 ? askPrice + defaultInstrument.getPipValue() * defaultStopLoss : 0;
            takeprofit = defaultTakeProfit != 0 ? askPrice - defaultInstrument.getPipValue() * defaultTakeProfit : 0;
            price = bidPrice;
        }
        else if(orderCommand == IEngine.OrderCommand.BUY){
            stoploss = defaultStopLoss != 0 ? bidPrice - defaultInstrument.getPipValue() * defaultStopLoss : 0;
            takeprofit = defaultTakeProfit != 0 ? bidPrice + defaultInstrument.getPipValue() * defaultTakeProfit : 0;
            price = askPrice;
        }
        
        IOrder myOrder = engine.submitOrder(
             this.getIntrumentLabel(defaultInstrument), // String label
             defaultInstrument, //Instrument instrument
             orderCommand, // IEngine.OrderCommand orderCommand
             defaultTradeAmount, // double amount
             price, // double price
             defaultSlippage, // double slippage
             stoploss, // double stopLossPrice
             takeprofit); // double takeProfitPrice         
         
         if(orderCommand == IEngine.OrderCommand.SELL){
             console.getOut().println("Open SELL Order, bidPrice: "+price+", stoploss: "+stoploss+", takeprofit: "+takeprofit+
                 ", Amount: "+defaultTradeAmount+", Pair: "+defaultInstrument.toString()+", count: " + countOpenSell);
             countOpenSell++;
         }
         else if(orderCommand == IEngine.OrderCommand.BUY){
             console.getOut().println("Open BUY Order, askPrice: "+price+", stoploss: "+stoploss+", takeprofit: "+takeprofit+
                 ", Amount: "+defaultTradeAmount+", Pair: "+defaultInstrument.toString()+", count: " + countOpenBuy);
             countOpenBuy++;
         }
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
    
    public String getFormattedTime(String format, String gmt){
        SimpleDateFormat fmt = new SimpleDateFormat(format);
        fmt.setTimeZone(TimeZone.getTimeZone(gmt));
        return fmt.format(new Date());
    }
}
