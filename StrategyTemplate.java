/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package liveStrategies;

import liveStrategies.common.Match;
import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IChart;
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
import com.dukascopy.api.Period;
import java.util.ArrayList;
import liveStrategies.common.Module;
import liveStrategies.Modules.SpeedModule;
import liveStrategies.Modules.StrengthModule;
import liveStrategies.Modules.TrendModule;

/**
 *
 * @author rescorsim
 */
public class StrategyTemplate  implements IStrategy{

    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IChart chart;
    private IOrder order;
    
    private Match match;
    private ArrayList<Module> modules;

    public StrategyTemplate() {
        this.modules = new ArrayList<>();
    }
    
    @Override
    public void onStart(IContext context) throws JFException {
        console.getOut().println("StrategyV1 -> onStart(IContext)");
        
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        //chart = context.getChart(instrument);
        
        this.modules.add(new StrengthModule(context));
        this.modules.add(new SpeedModule(context));
        this.modules.add(new TrendModule(context));
        //this.match = new Match();
    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
        console.getOut().println("StrategyV1 -> onMessage(IMessage)");
        for(Module module : this.modules){
            module.newTick(instrument, tick);
        }
        this.match.matchModules(modules);
        
    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        console.getOut().println("StrategyV1 -> onMessage(IMessage)");
        for(Module module : this.modules){
            module.newBar(instrument, period, askBar, bidBar);
        }
        this.match.matchModules(modules);
        
    }

    @Override
    public void onMessage(IMessage message) throws JFException {
        console.getOut().println("StrategyV1 -> onMessage(IMessage)");
        
    }

    @Override
    public void onAccount(IAccount account) throws JFException {
        console.getOut().println("StrategyV1 -> onAccount(IAccount)");
        
    }

    @Override
    public void onStop() throws JFException {
        console.getOut().println("StrategyV1 -> onStop()");
        
    }
    
}
