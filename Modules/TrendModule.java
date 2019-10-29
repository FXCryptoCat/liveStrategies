/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package liveStrategies.Modules;

import liveStrategies.common.ModuleType;
import liveStrategies.common.ModuleTypeOrder;
import liveStrategies.common.Module;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IContext;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import liveStrategies.common.ModuleStatus;

/**
 *
 * @author rescorsim
 */
public class TrendModule extends Module {

    public TrendModule(IContext context) {
        super(context, ModuleType.Trend);
    }

    @Override
    public void newTick(Instrument instrument, ITick tick) {
    }

    @Override
    public void newBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) {
    }   
}
