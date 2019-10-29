/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package liveStrategies.common;

/**
 *
 * @author rodrigo_desktop
 */
public class BinaryStrategyTester {
    private boolean openBuy, openSell;
    private int ordersBuy, ordersSell;
    private int ordersBuyGain, ordersSellGain, ordersBuyLoss, ordersSellLoss;
    private double openPrice ;  
    private String name; 
    public BinaryStrategyTester(String name) {
        this.name = name;
        openBuy = openSell = false;
        ordersBuy = ordersSell = 0;
         ordersBuyGain = ordersSellGain = ordersBuyLoss = ordersSellLoss = 0;
    }
    public void openBuyOrder(double price){
        openBuy = true;
        openPrice = price;
    }
    public void openSellOrder(double price){
        openSell = true;
        openPrice = price;
    }
    public boolean isOpenBuy(){
        return openBuy;
    }
    public boolean isOpenSell(){
        return openSell;
    }
    public boolean closeBuyOrder(double price) {
        openBuy = false;
        ordersBuy++;
        if (price > openPrice) {
            ordersBuyGain++;
            return true;
        } else {
            ordersBuyLoss++;
        }
        return false;
    }
    public boolean closeSellOrder(double price) {
        openSell = false;
        ordersSell++;
        if (price < openPrice) {
            ordersSellGain++;
            return true;
        } else {
            ordersSellLoss++;
        }
        return false;
    }
    public String ToString(){
        String result = name + " result -> ordersBuy: "+ordersBuy+", ordersSell: "+ordersSell+
                ", ordersBuyGain: "+ordersBuyGain+" ("+(ordersBuyGain/ordersBuy)+"%)"+
                ", ordersSellGain: "+ordersSellGain+" ("+(ordersSellGain/ordersSell)+"%)";
        return result;        
    }
    
}
