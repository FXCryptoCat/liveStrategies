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
import com.dukascopy.api.Period;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import static liveStrategies.common.Util.getFormattedTime;
import liveStrategies.options.MenuStrategy;

/**
 *
 * @author rescorsim
 */
public class StrategyTemplate2  implements IStrategy{

    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IChart chart;
    private IOrder order;
    
    @Configurable("defaultSlippage:")
    public int defaultSlippage = 1;
    @Configurable("defaultTakeProfit:")
    public int defaultTakeProfit = 50;
    @Configurable("defaultPeriod:")
    public Period defaultPeriod = Period.FIVE_MINS;
    @Configurable("defaultTradeAmount:")
    public double defaultTradeAmount = 0.01;
    @Configurable("defaultStopLoss:")
    public int defaultStopLoss = 25;
    @Configurable("defaultInstrument:")
    public static Instrument defaultInstrument = Instrument.EURUSD;

    private List<IOrder> PendingPositions =  null ;
    private List<IOrder> AllPositions =  null ;
    private List<IOrder> OpenPositions =  null ;
    
    private String name = "strategyName";
    private int labelCounter = 1;
    private double bidPrice;
    private double askPrice;
    int indicatorPeriodFast = 5;
    int indicatorPeriodSlow = 83;

    public StrategyTemplate2() {

    }
    
    public static void main(String[] args) throws Exception {
        MenuStrategy menuStrategy = new MenuStrategy(new StrategyTemplate2(), defaultInstrument);
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
            
        } catch (Exception e) {
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
    
    private void updateVariables(Instrument instrument) {
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
}
