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
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import static liveStrategies.BlackHoleScalper.defaultInstrument;
import liveStrategies.common.ModuleTypeOrder;
import static liveStrategies.common.Util.getFormattedTime;
import liveStrategies.options.MenuStrategy;

/**
 *
 * @author rescorsim
 */
public class BetelgeuseProfit implements IStrategy{
    
    /*******************************************/
    @Configurable("defaultInstrument:")
    public static Instrument defaultInstrument = Instrument.EURUSD;
    @Configurable("defaultPeriod:")
    public Period defaultPeriod = Period.ONE_MIN;
    @Configurable("defaultTakeProfit:")
    public int takeprofit= 200;
    @Configurable("defaultStopLoss:")
    public int stoploss = 100;
    @Configurable("defaultSlippage:")
    public int slippage = 1;
    @Configurable("lots:")
    public double lots = 0.01;
    @Configurable("useMM:")
    private boolean useMM = true;
    @Configurable("minLots:")
    public double minLots = 0.001;
    @Configurable("maxLots:")
    public double maxLots = 100.0;
    @Configurable("lotSize:")
    public int lotSize = 1000000;
    @Configurable("Risk:")
    public double risk = 50.0;
    /*************** PRICE SETUP ***************/
    @Configurable("priceArraySize:")
    public int priceArraySize1 = 100;
    @Configurable("priceArraySize2(0=OFF):")
    public int priceArraySize2 = 50;
    @Configurable("priceArraySize3(0=OFF):")
    public int priceArraySize3 = 25;
    /*************** SPREAD SETUP ***************/
    @Configurable("spreadArraySize:")
    public int spreadArraySize = 30;
    @Configurable("spreadFactorTrade:")
    public double spreadFactorTrade = 1.2;
    @Configurable("spreadFactorExpansion:")
    public double spreadFactorExpansion = 1.8;
    @Configurable("timeSpreadTrade(min):")
    public long timeSpreadTrade = 30;
    /*************** ACTIVITY SETUP ***************/
    @Configurable("activityArraySize:")
    public int activityArraySize = 100;
    @Configurable("activityFactorTrade:")
    public double activityFactorTrade = 1.5;
    @Configurable("activityFactorExpansion:")
    public double activityFactorExpansion = 2.0;
    @Configurable("timeActivityTrade(min):")
    public long timeActivityTrade = 10;
    /*************** OTHERS ***************/
    @Configurable("spreadSlowSize:")
    public long spreadSlowSize = 10000;
    @Configurable("activitySlowSize:")
    public long activitySlowSize = 200000;
    @Configurable("DEBUG:")
    private boolean debug = true;
    /**************************************/
    
    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IChart chart;
    private IOrder order;
    private IAccount account;
    
    private List<IOrder> PendingPositions;
    private List<IOrder> AllPositions;
    private List<IOrder> OpenPositions;
    
    private String name;
    private String comment;
    
    private long labelCounter, ticksLoad, ticksCount;
    private int digits, spread;
    private double bidPrice, askPrice, freeMargin, point, Bid, Ask;
    
    private Deque<Double> priceArray1, priceArray2, priceArray3;
    private Deque<Integer> spreadArray;
    private Deque<Long> activityArray;
    double priceAvg1, priceAvg2, priceAvg3, priceSum1, priceSum2, priceSum3;
    double spreadCurrentFactor, activityCurrentFactor;
    long spreadAvg, activityAvg, spreadSum, activitySum;
    long spreadSlowCount, spreadSlowSum, spreadSlowAvg;
    long activitySlowCount, activitySlowSum, activitySlowAvg;
    long lastTickTime, tickTime;
    
    ICustomWidgetChartObject widget;
    private JTextArea textAreaHeader;
    private JTextArea textAreaPrice;
    private JTextArea textAreaSpread;
    private JTextArea textAreaActivity;
    private String headerStr, priceStr, spreadStr, activityStr;

    public BetelgeuseProfit() {

    }
    
    public static void main(String[] args) throws Exception {
        MenuStrategy menuStrategy = new MenuStrategy(new BetelgeuseProfit(), defaultInstrument);
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
        this.name = "BetelgeuseProfit";
        this.comment = "BetelgeuseProfit";
        createWidget();
        
        Set subscribedInstruments = new HashSet();
        subscribedInstruments.add(defaultInstrument);
        this.context.setSubscribedInstruments(subscribedInstruments); 
        
        this.priceArray1 = new ArrayDeque<Double>(this.priceArraySize1);
        if(priceArraySize2>0) this.priceArray2 = new ArrayDeque<Double>(this.priceArraySize2);
        if(priceArraySize3>0) this.priceArray3 = new ArrayDeque<Double>(this.priceArraySize3);
        this.spreadArray = new ArrayDeque<Integer>(this.spreadArraySize);
        this.activityArray = new ArrayDeque<Long>(this.activityArraySize);
        
        priceAvg1=priceAvg2=priceAvg3=priceSum1=priceSum2=priceSum3=0;
        spreadAvg=activityAvg=spreadSum=activitySum=0;
        spreadSlowCount=spreadSlowSum=spreadSlowAvg=0;
        activitySlowCount=activitySlowSum=activitySlowAvg=0;
        spreadCurrentFactor=activityCurrentFactor=-1;
        
        digits = defaultInstrument.getPipScale();
        point = defaultInstrument.getPipValue()/10;
        takeprofit *= point;
        stoploss *= point;
        labelCounter = 1;
        ticksCount = 0;
        lastTickTime = tickTime = 0;
        ticksLoad = Math.max(Math.max(priceArraySize1, priceArraySize2),priceArraySize3);
        ticksLoad = Math.max(Math.max(ticksLoad, spreadArraySize),(activityArraySize+1));
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
        if (!instrument.equals(this.defaultInstrument)) return;   
        try {
            if(lastTickTime == 0){
                lastTickTime = tick.getTime();
                return;
            }
            ticksCount++;
            Bid = tick.getBid();
            Ask = tick.getAsk();
            spread = (int)((Ask - Bid)/point);
            this.updateArrays(tick);
            this.updatePanel();
            
            
            freeMargin = account.getEquity() - account.getUsedMargin();
            freeMargin = Math.min(freeMargin * account.getLeverage() / 2.0, account.getBalance() * risk / 100.0 * Bid / (stoploss + spread));
            lots = freeMargin / lotSize;
            lots = Normalize(lots, digits);
            lots = Math.max(minLots, lots);
            lots = Math.min(maxLots, lots);
                
            freeMargin = Normalize(account.getEquity() - account.getUsedMargin(),2);
            headerStr = " **$$$** "+this.name+" - "+defaultInstrument.toString()+ " **$$$** \n";
            headerStr += "Balance: "+account.getBalance()+" - Equity: "+account.getEquity()+"\n";
            headerStr += "FreeMargin: "+freeMargin+" - Leverage: "+account.getLeverage()+"\n";
            headerStr += "MM: "+useMM+" - Risk: "+risk+" - Lots: "+lots+"\n";
            headerStr += "ticksCount: "+ticksCount+((ticksCount<ticksLoad)?"LOADING...":"")+" - Spread: "+spread+"\n";
            textAreaHeader.setText(headerStr);
            
            if(ticksCount>ticksLoad){
                
                
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
        //double equity = this.context.getAccount().getEquity();
        //this.tradeAmount = (equity * this.useLeverage) / 100000;
    }
    
    private void updateArrays(ITick _tick) throws JFException{
        /**************** PRICE ARRAY ****************/  
                
        this.priceArray1.addFirst(Bid);
        
        
        /**************** SPREAD ARRAY ****************/  
        
        this.spreadArray.addFirst(spread);
        
        /**************** ACTIVITY ARRAY ****************/  
        this.tickTime = _tick.getTime() - lastTickTime;
        this.activityArray.addFirst(tickTime);
        this.activitySum += tickTime;
        if(ticksCount > this.activityArraySize){
            long removeActivity = this.activityArray.removeLast();
            this.activitySum -= removeActivity;
            this.activityAvg = activitySum / activityArraySize;
        }
        else{
            activityAvg = -1;
        }
        lastTickTime = _tick.getTime();
        
        this.activitySlowCount++;
        if(this.activitySlowCount < this.activitySlowSize){
            this.activitySlowSum += tickTime;
            this.activitySlowAvg = this.activitySlowSum / this.activitySlowCount;
        }
        else{
            this.activitySlowSum -= this.activitySlowAvg;
            this.activitySlowSum += tickTime;
            this.activitySlowAvg = this.activitySlowSum / this.activitySlowSize;
        }
        if(ticksCount > this.activityArraySize){
            this.activityCurrentFactor = (double)this.activityAvg / (double)this.activitySlowAvg;
            this.activityCurrentFactor = Normalize(this.activityCurrentFactor, 2);
        }
        
    }
    private void updatePanel(){
        this.activityStr = "TradeFactor: "+this.activityFactorTrade+" - ExpansionFactor: "+this.activityFactorExpansion+"\n";
        this.activityStr += "ActivitySum: "+this.activitySum+" - ActivitySlowSum: "+this.activitySlowSum+"\n";
        this.activityStr += "ActivityAvg: "+this.activityAvg+" - ActivitySlowAvg: "+this.activitySlowAvg+"\n";
        this.activityStr += "TickTime: "+this.tickTime+" - CurrentFactor: "+this.activityCurrentFactor+"\n";
        this.textAreaActivity.setText(activityStr);
        if(this.activityCurrentFactor >= this.activityFactorExpansion){
            this.textAreaActivity.setBackground(this.getRedColor());
        }
        else if(this.activityCurrentFactor <= this.activityFactorTrade){
            this.textAreaActivity.setBackground(this.getGreenColor());
        }
        else{
            this.textAreaActivity.setBackground(this.getNeutralColor());
        } 
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
    private double Normalize(double value) {
        return (new BigDecimal(value)).setScale(digits, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
    private double Normalize(double value, int places) {
        return (new BigDecimal(value)).setScale(places, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
    
    public void createWidget(){
        this.widget = chart.getChartObjectFactory().createChartWidget();
        this.widget.setFillOpacity(0.0f); //use 0f for transparent chart widget
        JPanel panel = this.widget.getContentPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        
        this.textAreaHeader = new JTextArea("Iniciando widget...");
        this.textAreaHeader.setBackground(this.getNeutralColor());
        this.textAreaHeader.setAlignmentX(Component.CENTER_ALIGNMENT);        
        this.textAreaPrice = new JTextArea("Area1");
        this.textAreaPrice.setBackground(this.getNeutralColor());
        this.textAreaSpread = new JTextArea("Area2");
        this.textAreaSpread.setBackground(this.getNeutralColor());
        this.textAreaActivity = new JTextArea("Area2");
        this.textAreaActivity.setBackground(this.getNeutralColor());
        
        panel.add(this.textAreaHeader);
        panel.add(this.textAreaPrice);
        panel.add(this.textAreaSpread);
        panel.add(this.textAreaActivity);
        panel.setSize(new Dimension(350, 500));
        panel.setMaximumSize(new Dimension(350, 500));
        panel.setMinimumSize(new Dimension(350, 100));
        chart.add(this.widget);
    }
    
    private Color getNeutralColor() { return new Color(210, 221, 215); }
    private Color getRedColor() { return new Color(244, 88, 88); }
    private Color getGreenColor() { return new Color(88, 244, 88); }
    private Color getYellowColor() { return new Color(231, 231, 77); }
    private Color getBlueColor() { return new Color(66, 66, 255); }
    private Color getOrangeColor() { return new Color(226, 138, 40); }
    
}
