package liveStrategies;

import com.dukascopy.api.Configurable;
import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IChart;
import com.dukascopy.api.IChartObject;
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
import com.dukascopy.api.drawings.ICustomWidgetChartObject;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import liveStrategies.Modules.TickPriceModule;
import liveStrategies.Modules.TickTimeModule;
import liveStrategies.common.ModuleTypeOrder;
import static liveStrategies.common.Util.getFormattedTime;
import liveStrategies.options.MenuStrategy;

/**
 *
 * @author rescorsim
 */
public class BlackHoleScalper implements IStrategy {

    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IAccount account;
    private IChart chart;
    private IOrder order;

    @Configurable("defaultPeriod:")
    public Period defaultPeriod = Period.ONE_MIN;
    @Configurable("defaultInstrument:")
    public static Instrument defaultInstrument = Instrument.EURUSD;

    @Configurable("defaultSlippage:")
    public int defaultSlippage = 3;
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
    public double Risk = 20.0;
    @Configurable("Max_Spread:")
    public double Max_Spread = 10.0;
    @Configurable("accountBalance:")
    public double accountBalance = 10.0;
    @Configurable("useGapToOpenOrders:")
    private boolean useGapToOpenOrders = false;
    @Configurable("TakeProfitGap:")
    public double TakeProfitGap = 20.0;
    @Configurable("StopLossGap:")
    public double StopLossGap = 60.0;
    @Configurable("useTickToOpenOrder:")
    private boolean useTickToOpenOrder = true;
    @Configurable("TakeProfitTick:")
    public double TakeProfitTick = 60.0;
    @Configurable("StopLossTick:")
    public double StopLossTick = 60.0;
    @Configurable("spreadFactor:")
    public double spreadFactor = 0.001;
    @Configurable("useTrailingStop:")
    private boolean useTrailingStop = true;
    @Configurable("trailingDist:")
    public double trailingDist = 20.0;
    @Configurable("trailingLimit:")
    public double trailingLimit = 0.0;
    @Configurable("trailingGap:")
    public double trailingGap = 0.0;
    @Configurable("orderExpiration:")
    private boolean orderExpiration = true;
    @Configurable("DEBUG:")
    private boolean debug = true;

    private List<IOrder> PendingPositions = null;
    private List<IOrder> AllPositions = null;
    private List<IOrder> OpenPositions = null;

    private String name = "BlackHoleScalper";
    private int labelCounter = 1;
    private double bidPrice;
    private double askPrice;
    private double tradeAmount;

    private String orderComment = "GodBase";
    private String orderLabel = name;
    private int i, ticket, brokerDigits, tickCount, spreadSumCount, openedOrders;
    private int timeExpiration, maTrend;
    private double point, spread, spreadAverage, HLspread, slAverage;
    private double scalperPriceDiff, pipsGapNewOrder, freeMargin;
    private double newOrderPrice, newOrderTP, newOrderSL;
    private double[] arrayBid, arrayAsk, spreadArray;
    private long[] arrayTickCount;
    ICustomWidgetChartObject widget;
    private JTextArea chartComment;

    public BlackHoleScalper() {

    }

    public static void main(String[] args) throws Exception {
        MenuStrategy menuStrategy = new MenuStrategy(new BlackHoleScalper(), defaultInstrument);
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

        arrayBid = new double[30];
        arrayAsk = new double[30];
        arrayTickCount = new long[30];
        spreadArray = new double[30];
        Arrays.fill(arrayBid, 0.0);
        Arrays.fill(arrayAsk, 0.0);
        Arrays.fill(arrayTickCount, 0);
        Arrays.fill(spreadArray, 0.0);

        if (defaultPeriod == Period.ONE_MIN) {
            pipsGapNewOrder = 30.0;
        } else if (defaultPeriod == Period.FIVE_MINS) {
            pipsGapNewOrder = 30.0;
        } else if (defaultPeriod == Period.FIFTEEN_MINS) {
            pipsGapNewOrder = 60.0;
        } else if (defaultPeriod == Period.THIRTY_MINS) {
            pipsGapNewOrder = 90.0;
        } else if (defaultPeriod == Period.ONE_HOUR) {
            pipsGapNewOrder = 120.0;
        } else if (defaultPeriod == Period.FOUR_HOURS) {
            pipsGapNewOrder = 150.0;
        } else if (defaultPeriod == Period.DAILY) {
            pipsGapNewOrder = 180.0;
        } else if (defaultPeriod == Period.WEEKLY) {
            pipsGapNewOrder = 210.0;
        }

        tickCount = spreadSumCount = timeExpiration = 0;

        brokerDigits = defaultInstrument.getPipScale();
        point = defaultInstrument.getPipValue()/10;

        slAverage = ((StopLossGap + StopLossTick) / 2) * point;
        trailingDist *= point;
        trailingLimit *= point;
        trailingGap *= point;

        console.getOut().println(name + " -> onStart() - Digits: " + brokerDigits + ", Point: " + point);
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
        if (!instrument.equals(this.defaultInstrument)) {
            return;
        }
        try {

            calculateScalperPrice(tick);

            openedOrders = maTrend = 0;
            int lotsDigit;
            double currentStopLoss, currentTakeProfit;
            double distanceTP;
            String commentStr;
            tickCount++;

            IBar bar = this.history.getBar(defaultInstrument, defaultPeriod, OfferSide.BID, 0);

            double Bid = tick.getBid();
            double Ask = tick.getAsk();
            double ihigh = bar.getHigh();
            double ilow = bar.getLow();

            double imaLow = this.indicators.lwma(defaultInstrument, defaultPeriod, OfferSide.BID, IIndicators.AppliedPrice.LOW, 3, 0);
            double imaHigh = this.indicators.lwma(defaultInstrument, defaultPeriod, OfferSide.BID, IIndicators.AppliedPrice.HIGH, 3, 0);
            double imaDiffHL = imaLow - imaHigh;

            boolean isBidHighImaDiff = Bid >= imaHigh + imaDiffHL / 2.0;
            double currentSpread = Ask - Bid;

            for (i = 0; i < 29; i++) {
                spreadArray[i] = spreadArray[i + 1];
            }
            //ArrayCopy(spreadArray, spreadArray, 0, 1, 29);
            spreadArray[29] = currentSpread;

            if (spreadSumCount < 30) {
                spreadSumCount++;
            }
            double spreadSum = 0;
            int aux = 29;
            for (i = 0; i < spreadSumCount; i++) {
                spreadSum += spreadArray[aux];
                aux--;
            }
            spreadAverage = Normalize(spreadSum / spreadSumCount);
            HLspread = ihigh - ilow;

            if (HLspread > spreadFactor) {
                if (Bid < imaHigh) {
                    maTrend = -1;
                } else {
                    if (Bid > imaLow) {
                        maTrend = 1;
                    }
                }
            }

            /**
             * ********** OrderExpiration *************
             */
            //if (orderExpiration) 
            //   timeExpiration = TimeCurrent() + 60.0 * MathMax(10 * Period(), 60);
            lotsDigit = LotsLog(0.1, minLots);
            if (useMM) {
                if (Risk < 0.01 || Risk > 100.0) {
                    console.getOut().println("ERROR -- Invalid Risk Value.");
                    return;
                }
                if (account.getBalance() <= 0.0) {
                    console.getOut().println("ERROR -- Account Balance is " + account.getBalance());
                    return;
                }
                freeMargin = account.getEquity() - account.getUsedMargin();
                accountBalance = Math.max(account.getBalance(), accountBalance);
                freeMargin = Math.min(freeMargin * account.getLeverage() / 2.0, accountBalance * Risk / 100.0 * Bid / (slAverage + spreadAverage));
                lots = freeMargin / lotSize;
                lots = Normalize(lots, lotsDigit);
                lots = Math.max(minLots, lots);
                lots = Math.min(maxLots, lots);
                
                
                
                //chartText.move(tick.getTime()-(6000000), tick.getBid() );
                String validSpread = spreadAverage <= Normalize(Max_Spread * point) ? "OK" : "HIGH";
                String curSpreadStr = new DecimalFormat("#.#####").format(currentSpread);
                String avgSpreadStr = new DecimalFormat("#.#####").format(spreadAverage);
                //chartText.setText("Equity: "+account.getEquity()+"\nLots: "+lots+"\nSpread: "+curSpreadStr+"\nSpreadAvg = "+avgSpreadStr+"\n"+validSpread );
                //this.context.getChart(defaultInstrument).add(chartText);
                
                chartComment.setText("Equity: "+account.getEquity()+"\nLots: "+lots+"\nSpread: "+curSpreadStr+"\nSpreadAvg = "+avgSpreadStr+"\n"+validSpread  +"\n1\n2\n3");
                
            }
            /**
             * ************** OPENED ORDERS *****************
             */
            this.updatePositions();
            for (IOrder order : this.OpenPositions) {
                if (order.getInstrument() != defaultInstrument) {
                    continue;
                }
                if (order.getOrderCommand() == IEngine.OrderCommand.BUY) {
                    while (useTrailingStop) {
                        currentStopLoss = order.getStopLossPrice();
                        currentTakeProfit = order.getTakeProfitPrice();

                        if (!((currentTakeProfit < Normalize(Ask + trailingDist) && Ask + trailingDist - currentTakeProfit > trailingGap)))
                            break;
                        try{
                            currentStopLoss = Normalize(Bid - trailingDist);
                            currentTakeProfit = Normalize(Ask + trailingDist);
                            order.setStopLossPrice(currentStopLoss);
                            order.setTakeProfitPrice(currentTakeProfit);
                        }
                        catch(JFException e){
                            console.getOut().println("ERROR when changing SL/TP from BUY order : " + e.getMessage());
                        }
                        break;
                    }
                    openedOrders++;
                }
                else if (order.getOrderCommand() == IEngine.OrderCommand.SELL) {
                    while (useTrailingStop) {
                        currentStopLoss = order.getStopLossPrice();
                        currentTakeProfit = order.getTakeProfitPrice();

                        if (!(currentTakeProfit > Normalize(Bid - trailingDist) && currentTakeProfit - Bid + trailingDist > trailingGap)) 
                            break;
                        try{
                            currentStopLoss = Normalize(Ask + trailingDist);
                            currentTakeProfit = Normalize(Bid - trailingDist);
                            order.setStopLossPrice(currentStopLoss);
                            order.setTakeProfitPrice(currentTakeProfit);
                        }
                        catch(JFException e){
                            console.getOut().println("ERROR when changing SL/TP from SELL order : " + e.getMessage());
                        }
                        break;
                    }
                    openedOrders++;
                }
                else if (order.getOrderCommand() == IEngine.OrderCommand.BUYSTOP || order.getOrderCommand() == IEngine.OrderCommand.BUYSTOP_BYBID) {
                    if (!isBidHighImaDiff) {
                        distanceTP = order.getTakeProfitPrice() - order.getOpenPrice();
                        if (!(Normalize(Ask + trailingLimit) < order.getOpenPrice() && order.getOpenPrice() - Ask - trailingLimit > trailingGap)) {
                            break;
                        }
                        try{
                            order.setOpenPrice(Normalize(Ask + trailingLimit));
                            order.setStopLossPrice(Normalize(Bid + trailingLimit - distanceTP));
                            order.setTakeProfitPrice(Normalize(Ask + trailingLimit + distanceTP));
                        }
                        catch(JFException e){
                            console.getOut().println("ERROR when changing SL/TP from BUYSTOP order : " + e.getMessage());
                        }    
                        openedOrders++;
                    }
                    else{
                        try {
                            order.close();
                        } catch (JFException e) {
                            console.getOut().println("ERROR when closing BUYSTOP order : " + e.getMessage());
                        }
                    }
                }
                else if (order.getOrderCommand() == IEngine.OrderCommand.SELLSTOP || order.getOrderCommand() == IEngine.OrderCommand.SELLSTOP_BYASK) {
                    if (isBidHighImaDiff) {
                        distanceTP = order.getOpenPrice() - order.getTakeProfitPrice();
                        
                        if (!(Normalize(Bid - trailingLimit) > order.getOpenPrice() && Bid - trailingLimit - order.getOpenPrice() > trailingGap)) 
                            break;
                        try{
                            order.setOpenPrice(Normalize(Bid - trailingLimit));
                            order.setStopLossPrice(Normalize(Ask - trailingLimit + distanceTP));
                            order.setTakeProfitPrice(Normalize(Bid - trailingLimit - distanceTP));
                        }
                        catch(JFException e){
                            console.getOut().println("ERROR when changing SL/TP from SELLSTOP order : " + e.getMessage());
                        }    
                        openedOrders++;
                    }
                    else{
                        try {
                            order.close();
                        } catch (JFException e) {
                            console.getOut().println("ERROR when closing SELLSTOP order : " + e.getMessage());
                        }
                    }
                }
            }

            /**
             * ************** OPENING ORDERS *****************
             */
            if (openedOrders == 0 && maTrend != 0 && spreadAverage <= Normalize(Max_Spread * point)) {
                if (maTrend < 0) {
                    if (useGapToOpenOrders) {
                        newOrderPrice = Ask + pipsGapNewOrder * point;
                        newOrderTP = newOrderPrice + TakeProfitGap * point;
                        newOrderSL = newOrderPrice - StopLossGap * point;

                        try{
                            IOrder order = engine.submitOrder(orderLabel, defaultInstrument, IEngine.OrderCommand.BUYSTOP, lots, newOrderPrice, defaultSlippage, newOrderSL, newOrderTP, timeExpiration, orderComment); 
                            console.getOut().println("BUYSTOP : " + newOrderPrice + " SL:" + newOrderSL + " TP:" + newOrderTP);
                        } catch (JFException e) {
                            console.getOut().println("ERROR when opening new BUYSTOP order : " + e.getMessage());
                        }
                    }
                    if (useTickToOpenOrder && Bid - ilow > 0 && scalperPriceDiff > 0.0) {
                        newOrderPrice = Ask;
                        newOrderTP = Ask + TakeProfitTick * point;
                        newOrderSL = Bid - StopLossTick * point;
                        
                        try{
                            IOrder order = engine.submitOrder(orderLabel, defaultInstrument, IEngine.OrderCommand.BUY, lots, newOrderPrice, defaultSlippage, newOrderSL, newOrderTP, timeExpiration, orderComment); 
                            console.getOut().println("BUY Ask: " + newOrderPrice + " SL:" + newOrderSL + " TP:" + newOrderTP);
                        } catch (JFException e) {
                            console.getOut().println("ERROR when opening new BUY order : " + e.getMessage());
                        }
                    }

                } else if (maTrend > 0) {
                    if (useGapToOpenOrders) {
                        newOrderPrice = Bid - pipsGapNewOrder * point;
                        newOrderTP = newOrderPrice - TakeProfitGap * point;
                        newOrderSL = newOrderPrice + StopLossGap * point;

                        try{
                            IOrder order = engine.submitOrder(orderLabel, defaultInstrument, IEngine.OrderCommand.SELLSTOP, lots, newOrderPrice, defaultSlippage, newOrderSL, newOrderTP, timeExpiration, orderComment); 
                            console.getOut().println("SELLSTOP : " + newOrderPrice + " SL:" + newOrderSL + " TP:" + newOrderTP);
                        } catch (JFException e) {
                            console.getOut().println("ERROR when opening new SELLSTOP order : " + e.getMessage());
                        }
                    }
                    if (useTickToOpenOrder && ihigh - Bid > 0 && scalperPriceDiff < 0.0) {
                        newOrderPrice = Bid;
                        newOrderTP = Bid - TakeProfitTick * point;
                        newOrderSL = Ask + StopLossTick * point;

                        try{
                            IOrder order = engine.submitOrder(orderLabel, defaultInstrument, IEngine.OrderCommand.SELL, lots, newOrderPrice, defaultSlippage, newOrderSL, newOrderTP, timeExpiration, orderComment); 
                            console.getOut().println("SELL Ask: " + newOrderPrice + " SL:" + newOrderSL + " TP:" + newOrderTP);
                        } catch (JFException e) {
                            console.getOut().println("ERROR when opening new SELL order : " + e.getMessage());
                        }
                    }
                }

            }
        } catch (Exception e) {
            console.getOut().println("ERROR in TickRace: " + e.getMessage());
        }
    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if (!instrument.equals(defaultInstrument) || period != defaultPeriod) {
            return;
        }
        tickCount = 0;

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

    private double Normalize(double value) {
        return (new BigDecimal(value)).setScale(brokerDigits, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    private double Normalize(double value, int places) {
        return (new BigDecimal(value)).setScale(places, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    int LotsLog(double _double, double _lotStep) {
        return (int) (Math.log(_lotStep) / Math.log(_double));
    }

    private void calculateScalperPrice(ITick tick) {
        double bidDiffAux;
        if (arrayTickCount[0] == 0 || tick.getBid() != arrayBid[0]) {
            for (i = 29; i > 0; i--) {
                arrayBid[i] = arrayBid[i - 1];
                arrayAsk[i] = arrayAsk[i - 1];
                arrayTickCount[i] = arrayTickCount[i - 1];
            }
            arrayBid[0] = tick.getBid();
            arrayAsk[0] = tick.getAsk();
            arrayTickCount[0] = tick.getTime();
        }
        scalperPriceDiff = 0;
        double minBidDiff = 0;
        double maxBidDiff = 0;

        for (i = 1; i < 30; i++) {
            if (arrayTickCount[i] == 0) {
                break;
            }

            bidDiffAux = arrayBid[0] - arrayBid[i];
            if (bidDiffAux < minBidDiff) {
                minBidDiff = bidDiffAux;
            }
            if (bidDiffAux > maxBidDiff) {
                maxBidDiff = bidDiffAux;
            }
            if (minBidDiff < 0.0 && maxBidDiff > 0.0 && minBidDiff < 3.0 * point || maxBidDiff > 3.0 * point) {
                if ((-minBidDiff) / maxBidDiff < 0.5) {
                    scalperPriceDiff = maxBidDiff;
                    break;
                }
                if ((-maxBidDiff) / minBidDiff < 0.5) {
                    scalperPriceDiff = minBidDiff;
                }
            } else if (maxBidDiff > 5.0 * point) {
                scalperPriceDiff = maxBidDiff;
            } else if (minBidDiff < 5.0 * point) {
                scalperPriceDiff = minBidDiff;
                break;
            }
        }
    }

    private void calculateTradeAmount() {
        //double equity = this.context.getAccount().getEquity();
        //this.tradeAmount = (equity * this.useLeverage) / 100000;
    }

    private void updatePositions() {
        try {
            this.AllPositions = engine.getOrders(this.defaultInstrument);
            List<IOrder> listOpen = new ArrayList<IOrder>();
            for (IOrder order : AllPositions) {
                if (order.getState().equals(IOrder.State.FILLED) || order.getState().equals(IOrder.State.OPENED)) {
                    listOpen.add(order);
                }
            }
            OpenPositions = listOpen;
        } catch (JFException e) {
            e.printStackTrace();
        }
    }

    public String getIntrumentLabel(Instrument instrument) throws JFException {
        String label = instrument.toString();
        label = label.replace("/", "");
        label += getFormattedTime("_ddMMyyyy_HHmmss_SSS_", "GMT-03:00");
        label += labelCounter;
        labelCounter++;
        return label;
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

    public void printTick(ITick tick) {
        console.getOut().println("TickRace::PrintTick - Ask:" + tick.getAsk() + ", Bid:" + tick.getBid()
                + ", askVolume:" + tick.getAskVolume() + ", bidVolume:" + tick.getBidVolume() + ", time:" + tick.getTime());
    }
    
    public void createWidget(){

        this.widget = chart.getChartObjectFactory().createChartWidget();
        //this.widget.setText("Price marker adder");

        this.widget.setFillOpacity(0.0f); //use 0f for transparent chart widget
        //this.widget.setColor(Color.GREEN.darker());

        JPanel panel = this.widget.getContentPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        chartComment = new JTextArea("Iniciando widget...");
        chartComment.setBackground(Color.LIGHT_GRAY);
        chartComment.setAlignmentX(Component.CENTER_ALIGNMENT);
        /*JButton button = new JButton("Add price marker on last Ask");
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    chart.add(chart.getChartObjectFactory().createPriceMarker(
                            "PriceMarker" + System.currentTimeMillis(), 
                            context.getHistory().getLastTick(instrument).getAsk())
                        );
                    label.setText(chart.getAll().size() + " chart objects on chart");
                } catch (JFException e1) {
                    e1.printStackTrace();
                }
            }});

        JButton buttonBuy = new JButton("Buy 0.1M");
        buttonBuy.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonBuy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                context.executeTask(new Callable<IOrder>() {
                    @Override
                    public IOrder call() throws Exception {
                        return context.getEngine().submitOrder("order" + System.currentTimeMillis(), instrument, OrderCommand.BUY, 0.1);
                    }
                });
            }
        });*/

        //panel.add(Box.createRigidArea(new Dimension(0,10)));
        panel.add(chartComment);
        //panel.add(Box.createRigidArea(new Dimension(0,5)));
        //panel.add(button);
        //panel.add(Box.createRigidArea(new Dimension(0,5)));
        //panel.add(buttonBuy);
        panel.setSize(new Dimension(250, 500));
        //panel.setMinimumSize(new Dimension(550, 100));
        //panel.setMaximumSize(new Dimension(750, 120));
        chart.add(this.widget);
        
    }
}
