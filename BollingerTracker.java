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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import  liveStrategies.common.BinaryStrategyTester;
import static liveStrategies.common.Util.getFormattedTime;
import liveStrategies.options.MenuStrategy;

/**
 *
 * @author rescorsim
 */


        
public class BollingerTracker  implements IStrategy{

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
    
    @Configurable("Bands Period:")
    public static int bandsPeriod = 20;
    @Configurable("Bands Deviation:")
    public static double bandsDeviation = 2.0;
    @Configurable("Moving Average Type:")
    public static IIndicators.MaType maType = IIndicators.MaType.SMA;
    
    private List<IOrder> PendingPositions =  null ;
    private List<IOrder> AllPositions =  null ;
    private List<IOrder> OpenPositions =  null ;
    
    private String name = "strategyName";
    private int labelCounter = 1;
    private double bidPrice;
    private double askPrice;
    int indicatorPeriodFast = 5;
    int indicatorPeriodSlow = 83;
    
    BinaryStrategyTester bst_bb_upper_lower;


    public BollingerTracker() {

    }
    
    public static void main(String[] args) throws Exception {
        MenuStrategy menuStrategy = new MenuStrategy(new BollingerTracker(), defaultInstrument, 1000.00, Period.TEN_SECS);
        menuStrategy.showMenuStrategy();
    }
    
    @Override
    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        console.getOut().println("Strategy BollingerTracker -> onStart()");
        
        Set subscribedInstruments = new HashSet();
        
        subscribedInstruments.add(defaultInstrument);
//        console.getOut().println("subscribedInstruments: ");
//        for(Instrument instrument : Instrument.values()){
//            subscribedInstruments.add(instrument);
//            console.getOut().println(instrument.toString() + ", ");
//        }
        this.context.setSubscribedInstruments(subscribedInstruments); 
        bst_bb_upper_lower = new BinaryStrategyTester("bst_bb_upper_lower");
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
        if (!instrument.equals(defaultInstrument)) return;   
        
    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if (!instrument.equals(defaultInstrument) || period != defaultPeriod) return;   
        //if (askBar.getVolume() == 0) return;
        askPrice = askBar.getClose();
        bidPrice = bidBar.getClose();
        try {
            
            double[] valuesBBs0 = context.getIndicators().bbands(
                    defaultInstrument, defaultPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 
                    bandsPeriod, bandsDeviation, bandsDeviation, maType, 0);
            double[] valuesBBs1 = context.getIndicators().bbands(
                    defaultInstrument, defaultPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 
                    bandsPeriod, bandsDeviation, bandsDeviation, maType, 1);
            double[] valuesBBs2 = context.getIndicators().bbands(
                    defaultInstrument, defaultPeriod, OfferSide.BID, IIndicators.AppliedPrice.CLOSE, 
                    bandsPeriod, bandsDeviation, bandsDeviation, maType, 2);
            
//            console.getOut().println("NewBar-> valuesBBs0[0] upper:" + valuesBBs0[0]+ ", valuesBBs0[1] middle: "+valuesBBs0[1]+", valuesBBs0[2] lower: "+valuesBBs0[2]);
//            console.getOut().println("NewBar-> valuesBBs1[0] upper:" + valuesBBs1[0]+", valuesBBs1[1] middle: "+valuesBBs1[1]+", valuesBBs1[2] lower: "+valuesBBs1[2]);
//            console.getOut().println("NewBar-> valuesBBs2[0] upper:" + valuesBBs2[0]+", valuesBBs2[1] middle: "+valuesBBs2[1]+", valuesBBs2[2] lower: "+valuesBBs2[2]);

            IBar barShift1 = this.history.getBar(defaultInstrument, defaultPeriod, OfferSide.BID, 1);
            IBar barShift2 = this.history.getBar(defaultInstrument, defaultPeriod, OfferSide.BID, 2);   
            //console.getOut().println("NewBar-> bidbar:" + bidBar.getClose()+ ", bar0: " + bar0.getClose()+ ", bar1: "+bar1.getClose()+", bar2: "+bar2.getClose());
            
            if(bst_bb_upper_lower.isOpenBuy()){
                if(bst_bb_upper_lower.closeBuyOrder(bidPrice)){
                    console.getOut().println("GAIN buy order!!! :)");
                }
                else{
                    console.getOut().println("LOSS buy order!!! :(");
                }
            }
            if(bst_bb_upper_lower.isOpenSell()){
                if(bst_bb_upper_lower.closeSellOrder(askPrice)){
                    console.getOut().println("GAIN sell order!!! :)");
                }
                else{
                    console.getOut().println("LOSS sell order!!! :(");
                }
            }
            
            if(barShift2.getClose() > valuesBBs2[0] && barShift1.getClose() < valuesBBs1[0] && valuesBBs1[2] < valuesBBs2[2]){
                console.getOut().println("SELL - New upper bbands signal -> barShift2.getClose(): "+barShift2.getClose()+", valuesBBs2[0]: " + valuesBBs2[0]+
                        ", barShift1.getClose(): "+barShift1.getClose()+", valuesBBs1[0]: "+valuesBBs1[0]);
                bst_bb_upper_lower.openSellOrder(bidPrice);
            }
            if(barShift2.getClose() < valuesBBs2[2] && barShift1.getClose() > valuesBBs1[2] && valuesBBs1[0] > valuesBBs2[0]){
                console.getOut().println("BUY - New upper bbands signal -> barShift2.getClose(): "+barShift2.getClose()+", valuesBBs2[2]: " + valuesBBs2[2]+
                        ", barShift1.getClose(): "+barShift1.getClose()+", valuesBBs1[2]: "+valuesBBs1[2]);
                bst_bb_upper_lower.openBuyOrder(askPrice);
            }
            
            
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
        console.getOut().println(bst_bb_upper_lower.ToString());
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
