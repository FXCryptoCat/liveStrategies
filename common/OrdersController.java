/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package liveStrategies.common;

import com.dukascopy.api.IConsole;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.JFException;
import java.util.ArrayList;

/**
 *
 * @author rescorsim
 */
public class OrdersController {

    private final IEngine engine;
    private final IConsole console;
    ArrayList<IOrder> openedOrders;
    
    public OrdersController(IContext context) {
        this.openedOrders = new ArrayList<>();
        this.engine = context.getEngine();
        this.console = context.getConsole();
    }

    public boolean openOrder(Order order) throws JFException {
        IOrder myOrder = engine.submitOrder(
                order.getLabel(), // String label
                order.getInstrument(), //Instrument instrument
                order.getOrderCmd(), // IEngine.OrderCommand orderCommand
                order.getAmount(), // double amount
                order.getPrice(), // double price
                order.getSlippage(), // double slippage
                order.getStoplossPrice(), // double stopLossPrice
                order.getTakeprofitPrice()); // double takeProfitPrice
        this.openedOrders.add(myOrder);
        return true;
    }
    
    public boolean closeOrder(Order order) throws JFException{
        ArrayList<IOrder> closedOrders = new ArrayList<>();
        
        for(IOrder openedOrder : this.openedOrders){
            if(openedOrder.getInstrument() == order.getInstrument()){
                this.engine.closeOrders(openedOrder);
                closedOrders.add(openedOrder);
                
            }
        }
        for(IOrder closedOrder : closedOrders){
            this.openedOrders.remove(closedOrder);
        }
        return true;
    }
    
}
