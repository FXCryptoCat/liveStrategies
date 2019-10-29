/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package liveStrategies;

import com.dukascopy.api.Configurable;
import liveStrategies.Modules.AwesomeV1Module;
import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IEngine.OrderCommand;
import com.dukascopy.api.IHistory;
import com.dukascopy.api.IIndicators;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IStrategy;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import java.util.ArrayList;
import liveStrategies.common.Module;
import liveStrategies.common.Match;
import liveStrategies.common.ModuleTypeOrder;
import liveStrategies.common.Order;
import liveStrategies.common.OrdersController;
import liveStrategies.common.Util;
import liveStrategies.options.MenuStrategy;

/**
 *
 * @author rescorsim
 */
public class AwesomeV1  implements IStrategy{

    @Configurable("Period")
    public Period period = Period.FIVE_MINS;
    
    @Configurable("Instrument")
    public Instrument instrument = Instrument.EURUSD;
    
    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    
    private Match match;
    private OrdersController ordersController;
    private ArrayList<Module> modules;
    private int counterLabel;
    private IBar prevAskBar;
    private IBar prevBidBar;
    
    public static void main(String[] args) throws Exception {
        MenuStrategy menuStrategy = new MenuStrategy(new AwesomeV1());
        menuStrategy.showMenuStrategy();
    }

    public AwesomeV1() {
        this.prevAskBar = null;
        this.prevBidBar = null;
        this.counterLabel = 0;
        this.modules = new ArrayList<>();
        this.match = new AwesomeV1Match();
    }
    
    @Override
    public void onStart(IContext context) throws JFException {
        
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        this.ordersController = new OrdersController(context);
        
        console.getOut().println("AwesomeV1 -> onStart(IContext)");
        
        this.modules.add(new AwesomeV1Module(context));
        
        this.prevAskBar = history.getBar(instrument, this.period, OfferSide.ASK, 2);
        this.prevBidBar = history.getBar(instrument, this.period, OfferSide.BID, 2); 
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
//        console.getOut().println("AwesomeV1 -> onTick(Instrument, ITick)");
//        for(Module module : this.modules){
//            module.newTick(instrument, tick);
//        }
//        ArrayList<Order> orders = this.match.matchModules(modules);
        
    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        if (period != this.period || instrument != this.instrument) {
            return;
        }
        IBar tempAskBar = history.getBar(instrument, this.period, OfferSide.ASK, 1); // get previous bar
        IBar tempBidBar = history.getBar(instrument, this.period, OfferSide.BID, 1);
        if(this.prevAskBar.getClose()== tempAskBar.getClose() &&
           this.prevBidBar.getClose() == tempBidBar.getClose()){
            return;
        }
//        console.getOut().println("AwesomeV1 -> onBar(Instrument: " + period.toString() +
//                ", Period : " + period.toString() + ", IBar)");
        for(Module module : this.modules){
            module.newBar(instrument, period, tempAskBar, tempBidBar);
        }
        ArrayList<Order> orders = this.match.matchModules(modules);
        ITick lastTick = history.getLastTick(instrument);
        
        //console.getOut().println("AwesomeV1 -> onBar-> Orders.size(): " + orders.size());
        
        for(Order order : orders){
            order.setInstrument(instrument);  
            if(order.getTypeOrder() == ModuleTypeOrder.CloseBuy ||
                    order.getTypeOrder() == ModuleTypeOrder.CloseSell ||
                    order.getTypeOrder() == ModuleTypeOrder.RevertToBuy ||
                    order.getTypeOrder() == ModuleTypeOrder.RevertToSell){
                console.getOut().println("AwesomeV1 -> onBar(): Close Order!!!");     
                this.ordersController.closeOrder(order);
            }       
            
            if(order.getTypeOrder() == ModuleTypeOrder.OpenBuy ||
                    order.getTypeOrder() == ModuleTypeOrder.RevertToBuy){
                console.getOut().println("AwesomeV1 -> onBar(): Open Buy Order!!!");
                order.setOrderCmd(OrderCommand.BUY);
                order.setLabel(Util.getIntrumentLabel(instrument));
                order.setPrice(lastTick.getAsk());
                //order.setStoplossPrice(lastTick.getBid() - instrument.getPipValue() * 200);
                //order.setTakeprofitPrice(lastTick.getAsk() + instrument.getPipValue() * 200);
                this.ordersController.openOrder(order);
            }
            else if(order.getTypeOrder() == ModuleTypeOrder.OpenSell ||
                    order.getTypeOrder() == ModuleTypeOrder.RevertToSell){
                console.getOut().println("AwesomeV1 -> onBar(): Open Sell Order!!!");
                order.setOrderCmd(OrderCommand.SELL);
                order.setLabel(Util.getIntrumentLabel(instrument));
                order.setPrice(lastTick.getBid());
                //order.setStoplossPrice(lastTick.getAsk() - instrument.getPipValue() * 200);
                //order.setTakeprofitPrice(lastTick.getBid() + instrument.getPipValue() * 200);   
                this.ordersController.openOrder(order);
            }
        }
        this.prevAskBar = tempAskBar;
        this.prevBidBar = tempBidBar; 
    }

    @Override
    public void onMessage(IMessage message) throws JFException {
//        console.getOut().println("AwesomeV1 -> onMessage(IMessage)");
        
    }

    @Override
    public void onAccount(IAccount account) throws JFException {
//        console.getOut().println("AwesomeV1 -> onAccount(IAccount)");
        
    }

    @Override
    public void onStop() throws JFException {
//        console.getOut().println("AwesomeV1 -> onStop()");
        
    }
 
}
