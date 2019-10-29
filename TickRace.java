package liveStrategies;

import com.dukascopy.api.Configurable;
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
import com.dukascopy.api.Period;
import com.dukascopy.api.drawings.ICustomWidgetChartObject;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import static liveStrategies.BlackHoleScalper.defaultInstrument;
import liveStrategies.Modules.TickPriceModule;
import liveStrategies.Modules.TickTimeModule;
import liveStrategies.common.ModuleTypeOrder;
import static liveStrategies.common.Util.getFormattedTime;
import liveStrategies.options.MenuStrategy;

/**
 *
 * @author rescorsim
 */
public class TickRace implements IStrategy{

    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IChart chart;
    private IOrder order;
    private IAccount account;
    
    @Configurable("defaultSlippage:")
    public int defaultSlippage = 1;
    @Configurable("defaultTakeProfit:")
    public int defaultTakeProfit = 0;
    @Configurable("defaultPeriod:")
    public Period defaultPeriod = Period.ONE_MIN;
    @Configurable("useLeverage:")
    public double useLeverage = 50;
    @Configurable("defaultStopLoss:")
    public int defaultStopLoss = 0;
    @Configurable("defaultInstrument:")
    public static Instrument defaultInstrument = Instrument.EURUSD;
    @Configurable("timeArraySize:")
    public int timeArraySize = 30;
    @Configurable("timeLimit:")
    public long timeLimit = 15000;
    @Configurable("priceBarSize:")
    private int priceBarSize = 10;
    @Configurable("priceArraySize:")
    private int priceArraySize = 30;
    @Configurable("pipsLimit:")
    private int pipsOpenLimit = 50;
    @Configurable("pipsLimit:")
    private int pipsCloseLimit = 20;
    @Configurable("DEBUG:")
    private boolean debug = true;

    private List<IOrder> PendingPositions =  null ;
    private List<IOrder> AllPositions =  null ;
    private List<IOrder> OpenPositions =  null ;
    
    private String name = "TickRace";
    private int labelCounter = 1;
    private ModuleTypeOrder orderType;
    private TickTimeModule tickTimeModule;
    private TickPriceModule tickPriceModule;
    private double bidPrice;
    private double askPrice;
    private double tradeAmount;
    
    ICustomWidgetChartObject widget;
    private JTextArea chartComment1;
    private JTextArea chartComment2;
    private JTextArea chartComment3;

    public TickRace() {

    }
    
    public static void main(String[] args) throws Exception {
        MenuStrategy menuStrategy = new MenuStrategy(new TickRace(), defaultInstrument);
        menuStrategy.showMenuStrategy();
    }
    
    @Override
    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        this.account = context.getAccount();
        this.chart = this.context.getChart(defaultInstrument);
        createWidget();
        
        Set subscribedInstruments = new HashSet();
        subscribedInstruments.add(defaultInstrument);
        this.context.setSubscribedInstruments(subscribedInstruments); 
        
        tickTimeModule = new TickTimeModule(this.context, this.timeArraySize, this.timeLimit);
        tickPriceModule = new TickPriceModule(this.context, this.priceArraySize, this.priceBarSize, this.pipsOpenLimit, this.pipsCloseLimit);
        this.tickTimeModule.printClassInfo();
        this.tickPriceModule.printClassInfo();
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
        if (!instrument.equals(this.defaultInstrument)) return;   
        try {
            this.tickTimeModule.newTick(instrument, tick);
            this.tickPriceModule.newTick(instrument, tick);
            //this.printTick(tick);
            if(this.tickTimeModule.isNewTimeValue() && this.tickPriceModule.isNewPriceValue()){
                String comment =this.tickTimeModule.getClassInfoStr()+"\n"+this.tickPriceModule.getClassInfoStr()+"\n***************************************\n";
                comment += this.tickTimeModule.getBarInfoStr()+"\n"+this.tickPriceModule.getBarInfoStr();
                this.chartComment1.setText(comment);
                //this.tickTimeModule.printBarInfo();
                //this.tickPriceModule.printBarInfo(); 
                askPrice = tick.getAsk();
                bidPrice = tick.getBid();
                
                this.updatePositions();
                if(!this.OpenPositions.isEmpty()){
                    orderType = this.tickPriceModule.getCloseOrderType();
                    for (IOrder order: this.OpenPositions) {
                          
                        if(order.isLong()){
                            if(orderType == ModuleTypeOrder.CloseBuy){
                                order.close();
                                if (debug){
                                    console.getOut().println("$$$ CLOSE ORDER BUY in BID PRICE " + tick.getBid()
                                            + ", ASK is " + tick.getAsk() + " and spread is " + (tick.getAsk() - tick.getBid()));
                                }
                            }
                        }
                        else{
                            if(orderType == ModuleTypeOrder.CloseSell){
                                order.close();
                                if (debug){
                                    console.getOut().println("$$$ CLOSE ORDER SELL in ASK PRICE " + tick.getAsk()
                                            + ", BID is " + tick.getBid() + " and spread is " + (tick.getAsk() - tick.getBid()));
                                }
                            }
                        }
                    }
                }
                else{
                    if(this.tickTimeModule.isTimeFriendly()){
                        orderType = this.tickPriceModule.getOpenOrderType();
                        if(orderType == ModuleTypeOrder.OpenBuy){
                            this.calculateTradeAmount();
                            IOrder myOrder = engine.submitOrder(
                                    this.getIntrumentLabel(this.defaultInstrument, name), // String label
                                    this.defaultInstrument, //Instrument instrument
                                    IEngine.OrderCommand.BUY, // IEngine.OrderCommand orderCommand
                                    this.tradeAmount, // double amount
                                    bidPrice, // double price
                                    this.defaultSlippage, // double slippage
                                    this.defaultStopLoss, // double stopLossPrice
                                    this.defaultTakeProfit); // double takeProfitPrice
                            if (debug) {
                                console.getOut().println("### NEW ORDER BUY in ASK PRICE " + tick.getAsk()
                                        + ", BID is " + tick.getBid() + " and spread is " + (tick.getAsk() - tick.getBid()));
                            }
                        }
                        else if(orderType == ModuleTypeOrder.OpenSell){
                            this.calculateTradeAmount();    
                            IOrder myOrder = engine.submitOrder(
                                    this.getIntrumentLabel(this.defaultInstrument, name), // String label
                                    this.defaultInstrument, //Instrument instrument
                                    IEngine.OrderCommand.SELL, // IEngine.OrderCommand orderCommand
                                    this.tradeAmount, // double amount
                                    bidPrice, // double price
                                    this.defaultSlippage, // double slippage
                                    this.defaultStopLoss, // double stopLossPrice
                                    this.defaultTakeProfit); // double takeProfitPrice
                            if (debug) {
                                console.getOut().println("### NEW ORDER SELL in BID PRICE " + tick.getBid()
                                        + ", ASK is " + tick.getAsk() + " and spread is " + (tick.getAsk() - tick.getBid()));
                            }
                        }
                    }
                }
                
            }
            
        } catch (Exception e) {
            console.getOut().println("ERROR in TickRace: "+e.getMessage());
        }
    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
//        if (!instrument.equals(defaultInstrument) || period != defaultPeriod) return;   
//        if (askBar.getVolume() == 0) return;
//        askPrice = askBar.getClose();
//        bidPrice = bidBar.getClose();
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
    
    private void calculateTradeAmount(){
        double equity = this.context.getAccount().getEquity();
        this.tradeAmount = (equity * this.useLeverage) / 100000;
    }
    
    private void updatePositions(){
        try {
            AllPositions = engine.getOrders(this.defaultInstrument);  
            List<IOrder> listOpen = new ArrayList<IOrder>();
            for (IOrder order: AllPositions) {
                if (order.getState().equals(IOrder.State.FILLED) || order.getState().equals(IOrder.State.OPENED)){
                    listOpen.add(order);
                }
            }
            OpenPositions = listOpen;
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
    public String getIntrumentLabel(Instrument instrument, String prefix) throws JFException {
        String label = instrument.toString();
        label = label.replace("/", "");
        label = prefix + "_" + label;
        label += getFormattedTime("_ddMMyyyy_HHmmss_SSS_", "GMT-03:00");
        label += labelCounter;
        labelCounter++;
        return label;
    }
    public String getFormattedTime(String format, String gmt){
        SimpleDateFormat fmt = new SimpleDateFormat(format);
        fmt.setTimeZone(TimeZone.getTimeZone(gmt));
        return fmt.format(new Date());
    }
    public void printTick(ITick tick){
        console.getOut().println("TickRace::PrintTick - Ask:"+tick.getAsk()+", Bid:"+tick.getBid()+
        ", askVolume:"+tick.getAskVolume()+", bidVolume:"+tick.getBidVolume()+", time:"+tick.getTime());
    }
    
    public void createWidget(){
        this.widget = chart.getChartObjectFactory().createChartWidget();
        this.widget.setFillOpacity(0.0f); //use 0f for transparent chart widget
        JPanel panel = this.widget.getContentPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        
        chartComment1 = new JTextArea("Iniciando widget...");
        chartComment1.setBackground(new Color(210, 221, 215));
        chartComment1.setBorder(BorderFactory.createCompoundBorder());
        chartComment1.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        chartComment2 = new JTextArea("Area1");
        chartComment2.setBackground(new Color(244, 88, 88));
        chartComment2.setBorder(BorderFactory.createCompoundBorder());
        
        chartComment3 = new JTextArea("Area2");
        chartComment3.setBackground(new Color(88, 244, 88));
        chartComment3.setBorder(BorderFactory.createCompoundBorder());
        
        
        panel.add(chartComment1);
        panel.add(chartComment2);
        panel.add(chartComment3);
        panel.setSize(new Dimension(250, 500));
        panel.setMaximumSize(new Dimension(250, 500));
        panel.setMinimumSize(new Dimension(250, 100));
        chart.add(this.widget);
    }
}
