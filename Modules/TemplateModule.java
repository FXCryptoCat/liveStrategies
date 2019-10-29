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
import liveStrategies.common.Module;
import liveStrategies.common.ModuleType;
import liveStrategies.common.ModuleTypeOrder;

/**
 *
 * @author rodrigo
 */
public class TemplateModule  extends Module {

    public TemplateModule(IContext context) {
        super(context, ModuleType.Template);
    }

    @Override
    public void newTick(Instrument instrument, ITick tick) {
    }

    @Override
    public void newBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) {
    }
    
}
