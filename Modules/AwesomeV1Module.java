/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package liveStrategies.Modules;

import com.dukascopy.api.Filter;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IIndicators;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import java.util.logging.Level;
import java.util.logging.Logger;
import liveStrategies.common.Module;
import liveStrategies.common.ModuleType;
import liveStrategies.common.ModuleTypeOrder;

/**
 *
 * @author rodrigo
 */
public class AwesomeV1Module extends Module {
    // parameters
    public Period period = Period.THIRTY_MINS;
    public int fasterMA = 5;
    public int slowerMA = 34;
    public int pipsGap = 0;
    
    enum Status{
        Positive, Negative, Zero
    }
    private double[] values;
    private Status lastStatus = Status.Zero;
    private double lastPositiveValue = 0;
    private double lastNegativeValue = 0;
    private double lastPositivePrice = 0;
    private double lastNegativePrice = 0;
    private double lastBeginPositiveValue = 0;
    private double lastBeginNegativeValue = 0;
    private double lastBeginPositivePrice = 0;
    private double lastBeginNegativePrice = 0;
    
    private int buyOrders = 0;
    private int sellOrders = 0;
            
   
    public AwesomeV1Module(IContext context) {
        super(context, ModuleType.AwesomeV1);
    }

    @Override
    public void newTick(Instrument instrument, ITick tick) {
    }

    @Override
    public void newBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) {
        //console.getOut().println("StrategyV1 -> newTick(Instrument, ITick)");
        this.moduleTypeOrder = ModuleTypeOrder.Neutral;
        double pipRange = 30 * instrument.getPipValue();
                
        try {
            values = this.indicators.awesome(instrument,
                    this.period, OfferSide.BID,
                    IIndicators.AppliedPrice.CLOSE,
                    this.fasterMA,
                    IIndicators.MaType.SMA,
                    this.slowerMA,
                    IIndicators.MaType.SMA,
                    1);
        
            
//        OfferSide[] offerside = new OfferSide[1];
//        IIndicators.AppliedPrice[] appliedPrice = new IIndicators.AppliedPrice[1];
//        offerside[0] = OfferSide.BID;
//        appliedPrice[0] = IIndicators.AppliedPrice.CLOSE;
// >       long time = context.getHistory().getBar(instrument, this.period, OfferSide.BID, 0).getTime();
//            
//            Object[] indicatorResult = context.getIndicators().calculateIndicator(instrument, this.period, offerside,
//                    "AWESOME", appliedPrice, params, Filter.WEEKENDS, 1, time, 0);
            
//            public double[] awesome(Instrument instrument, Period period, OfferSide side, IIndicators.AppliedPrice appliedPrice, int fasterMaTimePeriod, IIndicators.MaType fasterMaType, int slowerMaTimePeriod, IIndicators.MaType slowerMaType, int shift) throws JFException
//	/*      */   {
//	/*  331 */     Object[] ret = calculateIndicator(instrument, period, new OfferSide[] { side }, "AWESOME", new IIndicators.AppliedPrice[] { appliedPrice }, new Object[] { Integer.valueOf(fasterMaTimePeriod), Integer.valueOf(fasterMaType.ordinal()), Integer.valueOf(slowerMaTimePeriod), Integer.valueOf(slowerMaType.ordinal()) }, shift);
//	/*  332 */     return new double[] { ((Double)ret[0]).doubleValue(), ((Double)ret[1]).doubleValue(), ((Double)ret[2]).doubleValue() };
//            
            
            //console.getOut().println("AwesomeV1Module -> onBar -> values[0]: "+values[0]+
            //        ", values[1]: "+values[1]+", values[2]:"+values[2]);

            
            if (!Double.isNaN(values[1])) { // is positive
                //console.getOut().println("AwesomeV1Module -> onBar -> get positive values");
                this.lastPositiveValue = values[1];
                this.lastPositivePrice = bidBar.getClose();
                
                if (this.lastStatus == Status.Negative || this.lastStatus == Status.Zero) { // change to positive 
                    console.getOut().println("AwesomeV1Module -> onBar -> Change to positive, "
                        + "lastPositiveValue: " + lastPositiveValue + ", lastPositivePrice " + lastPositivePrice);
                     
                    if(this.sellOrders > 0){
                        this.moduleTypeOrder = ModuleTypeOrder.CloseSell;
                        this.sellOrders--;
                    }
                    // verifica preço da última inversão pra positivo
                    // se o preço é menor que o atual significa fundos ascendentes
                    // e uma possível tendência de alta; 
                    if(this.lastBeginPositivePrice != 0 && this.lastBeginNegativePrice != 0 && (
                            this.lastBeginPositivePrice + pipRange < this.lastPositivePrice
                            || lastBeginNegativePrice - pipRange > this.lastPositivePrice)){
                        if(this.moduleTypeOrder == ModuleTypeOrder.CloseSell){
                            this.moduleTypeOrder = ModuleTypeOrder.RevertToBuy;
                        }
                        else{
                            this.moduleTypeOrder = ModuleTypeOrder.OpenBuy;
                        }
                        this.buyOrders ++;
                    }
                    this.lastBeginPositiveValue = values[1];
                    this.lastBeginPositivePrice = bidBar.getClose();
                }
                this.lastStatus = Status.Positive;
            }
            if (!Double.isNaN(values[2])) { // is negative
                //console.getOut().println("AwesomeV1Module -> onBar -> get negative values");
                this.lastNegativeValue = values[2];
                this.lastNegativePrice = bidBar.getClose();
                
                if (this.lastStatus == Status.Positive || this.lastStatus == Status.Zero) { // change to negative
                    console.getOut().println("AwesomeV1Module -> onBar -> Change to negative, "
                        + "lastNegativeValue: " + lastNegativeValue + ", lastNegativePrice " + lastNegativePrice);
                    
                    if(this.buyOrders > 0){
                        this.moduleTypeOrder = ModuleTypeOrder.CloseBuy;
                        this.buyOrders--;
                    }
                    // verifica preço da última inversão pra negativo
                    // se o preço é maior que o atual significa fundos descendentes
                    // e uma possível tendência de baixa; 
                    if(this.lastBeginPositivePrice != 0 && this.lastBeginNegativePrice != 0 && (
                            this.lastBeginNegativePrice - pipRange > this.lastNegativePrice
                            || lastBeginPositivePrice + pipRange < this.lastNegativePrice)){
                        if(this.moduleTypeOrder == ModuleTypeOrder.CloseBuy){
                            this.moduleTypeOrder = ModuleTypeOrder.RevertToSell;
                        }
                        else{
                            this.moduleTypeOrder = ModuleTypeOrder.OpenSell;
                        }
                        this.sellOrders ++;
                    }
                    this.lastBeginNegativeValue = values[2];
                    this.lastBeginNegativePrice = bidBar.getClose();
                    
                }
                this.lastStatus = Status.Negative;
            }
        
        } catch (JFException ex) {
            Logger.getLogger(AwesomeV1Module.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
