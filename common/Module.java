/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package liveStrategies.common;

import com.dukascopy.api.IBar;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IIndicators;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;

/**
 *
 * @author rescorsim
 */

public abstract class Module {

    protected IContext context;
    protected IIndicators indicators;
    protected IConsole console;
       
    protected ModuleType type;
    protected double threshold;
    
    protected ModuleTypeOrder moduleTypeOrder;
    protected ModuleStatus moduleStatus;
    
    
    public Module(IContext context, ModuleType type){
        this.context = context;
        this.console = context.getConsole();
        this.indicators = context.getIndicators();
        this.type = type;
        this.moduleTypeOrder = ModuleTypeOrder.Neutral;
        this.moduleStatus = ModuleStatus.Neutral;
    }
    
    public abstract void newTick(Instrument instrument, ITick tick);
    public abstract void newBar(Instrument instrument, Period period, IBar askBar, IBar bidBar);

    public ModuleType getType() {
        return this.type;
    }
    
    public void setType(ModuleType type) {
        this.type = type;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
    
    public ModuleTypeOrder getModuleTypeOrder(){
        return this.moduleTypeOrder;
    }
    public ModuleStatus getModuleStatus(){
        return this.moduleStatus;
    } 
     
}
