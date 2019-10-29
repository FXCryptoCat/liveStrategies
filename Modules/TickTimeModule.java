package liveStrategies.Modules;

import com.dukascopy.api.IBar;
import com.dukascopy.api.IContext;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import liveStrategies.common.Module;
import liveStrategies.common.ModuleType;
import liveStrategies.common.ModuleTypeOrder;

public class TickTimeModule  extends Module {
    
    private int countTicks;
    private long beginTime;
    private long currentTimeBar;
    private boolean newTimeValue;
    private long totalTicks;
    private long totalBars;
    private final int tickArraySize;
    private final long timeLimit;

    public TickTimeModule(IContext context, int tickArraySize, long timeLimit) {
        super(context, ModuleType.Template);
        this.tickArraySize = tickArraySize;
        this.countTicks = 0;
        this.beginTime = 0;
        this.currentTimeBar = 0;
        this.newTimeValue = false;
        this.totalTicks = 0;
        this.totalBars = 0;
        this.timeLimit = timeLimit;
    }

    @Override
    public void newTick(Instrument instrument, ITick tick) { 
        this.countTicks++;
        this.totalTicks++;
        if(this.countTicks >= this.tickArraySize){
            this.newTimeValue = true;
            this.totalBars++;
            long currentTime = tick.getTime();
            this.currentTimeBar = currentTime - this.beginTime;
            this.countTicks = 0;
            this.beginTime = currentTime;       
        }
        else{
            this.newTimeValue = false;
        }
    }

    @Override
    public void newBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) {
    }
    
    public long getCurrentTimeBar(){
        return this.currentTimeBar;
    }
    
    public boolean isNewTimeValue(){
        return this.newTimeValue;
    }
    public boolean isTimeFriendly(){
        return this.currentTimeBar <= this.timeLimit;
    }
    
    public String getClassInfoStr(){
        return "TickTimeModule: tickArraySize = "+this.tickArraySize+", TimeLimit: "+this.timeLimit;
    }
    public String getBarInfoStr(){
        return "TickTimeModule:\ntotalTicks = "+this.totalTicks+ "\ntotalBars = "+this.totalBars+"\ncurrentTimeBar = "+this.currentTimeBar+"\ntimeFriendly: "+this.isTimeFriendly();
    }
    
    public void printClassInfo(){
        this.console.getOut().println(
                "TickTimeModule::printClassInfo: tickArraySize = "+this.tickArraySize+
                        ", TimeLimit: "+this.timeLimit);
    }
    public void printBarInfo(){
        this.console.getOut().println(
                "TickTimeModule::printBarInfo): totalTicks = "+this.totalTicks+
                ", totalBars = "+this.totalBars+", currentTimeBar = "+this.currentTimeBar+
                ", timeFriendly: "+this.isTimeFriendly());
    }
}
