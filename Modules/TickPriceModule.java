package liveStrategies.Modules;

import com.dukascopy.api.IBar;
import com.dukascopy.api.IContext;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import liveStrategies.common.Module;
import liveStrategies.common.ModuleType;
import liveStrategies.common.ModuleTypeOrder;

public class TickPriceModule  extends Module {
    
    private int barSize;
    private int tickSize;
    private int pipsOpenLimit;
    private int pipsCloseLimit;
    private long totalTicks;
    private long totalBars;
    private int countTicks;
    private boolean newPriceValue;
    private double[] barArray;
    private int currentArrayPosition;
    private double currentBarAvg;
    private double currentBarSum;
    private double currentBarValue;
    private double currentPriceSum;
    private int pipsDeviation;

    public TickPriceModule(IContext context, int tickSize, int barSize, int pipsOpenLimit, int pipsCloseLimit) {
        super(context, ModuleType.Template);
        this.tickSize = tickSize;
        this.barSize = barSize;
        this.pipsOpenLimit = pipsOpenLimit;
        this.pipsCloseLimit = this.pipsCloseLimit;
        this.totalTicks = 0;
        this.totalBars = 0;
        this.countTicks = 0;
        this.newPriceValue = false;
        this.currentArrayPosition = 0;
        this.currentBarAvg = 0;
        this.currentBarSum = 0;
        this.currentBarValue = 0;
        this.currentPriceSum = 0;
        this.pipsDeviation = 0;
        this.barArray = new double[this.barSize];
    }

    @Override
    public void newTick(Instrument instrument, ITick tick) {
        this.newPriceValue = false;
        this.totalTicks++;
        countTicks++;
        this.currentPriceSum += (tick.getBid() + tick.getAsk()) / 2;     

        if (this.countTicks >= this.tickSize) {
            newPriceValue = true;
            //calculate new bar
            currentBarValue = currentPriceSum / tickSize;
            // calculate avg
            currentBarSum -= barArray[currentArrayPosition];
            currentBarSum += currentBarValue;
            //if(totalBars>=barSize)
            //  {
            currentBarAvg = currentBarSum / barSize;
            this.pipsDeviation = (int)((this.currentBarValue-this.currentBarAvg)/(instrument.getPipValue()/10));
            //  }
            // update bar array
            barArray[currentArrayPosition] = currentBarValue;

            //update variables
            currentPriceSum = 0;
            countTicks = 0;
            currentArrayPosition++;
            totalBars++;
            if (currentArrayPosition >= barSize) {
                currentArrayPosition = 0;
            }
        }

    }

    @Override
    public void newBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) {
    }
    
    public ModuleTypeOrder getOpenOrderType(){
        if(this.pipsDeviation >= this.pipsOpenLimit)
            return ModuleTypeOrder.OpenBuy;
        if(this.pipsDeviation <= (-this.pipsOpenLimit))
            return ModuleTypeOrder.OpenSell;
        return ModuleTypeOrder.Neutral;
    }
    public ModuleTypeOrder getCloseOrderType(){
        if(this.pipsDeviation >= this.pipsCloseLimit)
            return ModuleTypeOrder.CloseSell;
        if(this.pipsDeviation <= (-this.pipsCloseLimit))
            return ModuleTypeOrder.CloseBuy;
        return ModuleTypeOrder.Neutral;
    }
    
    public boolean isNewPriceValue(){
        return (this.newPriceValue && this.totalBars > this.barSize);
    }
    public String getClassInfoStr(){
        return "TickPriceModule: tickSize = "+this.tickSize+", barSize: "+this.barSize+", pipsOpenLimit: "+this.pipsOpenLimit+", pipsCloseLimit: "+this.pipsCloseLimit;
    }
    public String getBarInfoStr(){
        return "TickPriceModule:\ntotalTicks = "+this.totalTicks+"\ntotalBars = "+this.totalBars+"\ncurrentBarAvg = "+this.currentBarAvg+
                "\ncurrentBarValue: "+currentBarValue+"\ndeviation: "+this.pipsDeviation;
    }
    public void printClassInfo(){
        this.console.getOut().println(
                "TickPriceModule::printClassInfo: tickSize = "+this.tickSize+", barSize: "+this.barSize+
                ", pipsOpenLimit: "+this.pipsOpenLimit+", pipsCloseLimit: "+this.pipsCloseLimit);
    }
    public void printBarInfo(){
        this.console.getOut().println(
                "TickPriceModule::printBarInfo): totalTicks = "+this.totalTicks+
                ", totalBars = "+this.totalBars+", currentBarAvg = "+this.currentBarAvg+
                ", currentBarValue: "+currentBarValue+", deviation: "+this.pipsDeviation);
    }
    
}
