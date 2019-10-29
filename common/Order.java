/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package liveStrategies.common;

import com.dukascopy.api.IEngine;
import com.dukascopy.api.IEngine.OrderCommand;
import com.dukascopy.api.Instrument;

/**
 *
 * @author rescorsim
 */
public class Order {
    
    private Instrument instrument;
    //private OrderCommand orderCmd;
    private double amount;
    private double price;
    private double slippage;
    private double stoplossPrice;
    private double takeprofitPrice;
    private String label;
    private IEngine.OrderCommand orderCmd;

    private ModuleTypeOrder typeOrder;

    public Order(Instrument instrument, ModuleTypeOrder typeOrder, double amount, double price, double slippage, double stoplossPrice, double takeprofitPrice) {
        this.instrument = instrument;
        this.typeOrder = typeOrder;
        this.amount = amount;
        this.price = price;
        this.slippage = slippage;
        this.stoplossPrice = stoplossPrice;
        this.takeprofitPrice = takeprofitPrice;
    }
    
    public Order() {
        this.instrument = Instrument.EURUSD;
        this.typeOrder = ModuleTypeOrder.Neutral;
        this.amount = 0.1;
        this.price = 0;
        this.slippage = 5;
        this.stoplossPrice = 0;
        this.takeprofitPrice = 0;
    }
    
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }

    public ModuleTypeOrder getTypeOrder() {
        return typeOrder;
    }

    public void setTypeOrder(ModuleTypeOrder typeOrder) {
        this.typeOrder = typeOrder;
    }  
    
    public OrderCommand getOrderCmd() {
        return orderCmd;
    }

    public void setOrderCmd(OrderCommand orderCmd) {
        this.orderCmd = orderCmd;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getSlippage() {
        return slippage;
    }

    public void setSlippage(double slippage) {
        this.slippage = slippage;
    }

    public double getStoplossPrice() {
        return stoplossPrice;
    }

    public void setStoplossPrice(double stoplossPrice) {
        this.stoplossPrice = stoplossPrice;
    }

    public double getTakeprofitPrice() {
        return takeprofitPrice;
    }

    public void setTakeprofitPrice(double takeprofitPrice) {
        this.takeprofitPrice = takeprofitPrice;
    }
}