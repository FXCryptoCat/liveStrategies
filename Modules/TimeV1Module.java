/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package liveStrategies.Modules;

import com.dukascopy.api.IBar;
import com.dukascopy.api.IContext;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import java.text.ParseException;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import liveStrategies.common.Module;
import liveStrategies.common.ModuleStatus;
import liveStrategies.common.ModuleType;
import liveStrategies.common.Util;

/**
 *
 * @author rodrigo
 */
public class TimeV1Module extends Module {
    public Period period = Period.ONE_HOUR;
    //
    boolean fixed = false;
    HashSet<Integer> daysOfWeek;
    HashSet<Integer>  hours;
    
    boolean weekInterval = false;
    int fromHour = -1; 
    int fromDay = -1;
    int toHour = -1;
    int toDay = -1;
    
    public TimeV1Module(IContext context) {
        super(context, ModuleType.TimeV1);
        daysOfWeek = new HashSet<Integer>();
        hours = new HashSet<Integer>();
    }
    
    public void setFixed(){ this.fixed = true; this.weekInterval = false; }
    public void setWeekInterval(){ this.fixed = false; this.weekInterval = true; }
    
    public void setDayOfWeek(int day){
        this.setFixed();
        this.daysOfWeek.add(day);
    }
    public void setDaysOfWeek(int from, int to){
        this.setFixed();
        for (int i = 1; i < 8; i++) { // 1-7, 1 = sunday
            if(i >= from && i <= to){
                this.daysOfWeek.add(i);
            }
        }
    }
    public void setHour(int hour){
        this.setFixed();
        this.hours.add(hour);
    }
    public void setHours(int from, int to){
        this.setFixed();
        for (int i = 0; i < 24; i++) { // 1-7, 1 = sunday
            if(i >= from && i <= to){
                this.hours.add(i);
            }
        }
    }
    
    public void setWeekInterval(int fromHour, int fromDay, int toHour, int toDay){
        this.setWeekInterval();
        this.fromHour = fromHour;
        this.fromDay = fromDay;
        this.toHour = toDay;
        this.toDay = toDay;
    }

    @Override
    public void newTick(Instrument instrument, ITick tick) {
    }

    @Override
    public void newBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) {
        this.moduleStatus = ModuleStatus.Neutral;
        if (period.equals(this.period)) {
            try {
                int day = Util.getDayFromTimestamp(askBar.getTime());
                int hour = Util.getHourFromTimestamp(askBar.getTime());
                if(this.fixed){
                    if(this.daysOfWeek.contains(day) && this.hours.contains(hour)){
                        this.moduleStatus = ModuleStatus.Valid;
                    }
                    else{
                        this.moduleStatus = ModuleStatus.Invalid;
                    }
                }
                else if(this.weekInterval){
                    if(day >= this.fromDay && day <= this.toDay &&
                        !(day == this.fromDay && hour < this.fromHour || 
                            day == this.toDay && hour > this.toHour)){
                            this.moduleStatus = ModuleStatus.Valid;
                        }
                    }
                else{
                    this.moduleStatus = ModuleStatus.Invalid;
                }
                    
            } catch (ParseException ex) {
                Logger.getLogger(TimeV1Module.class.getName()).log(Level.SEVERE, null, ex);  
            }
        }
        
    }
    
}
