/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package liveStrategies;

import liveStrategies.common.Module;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IIndicators;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import liveStrategies.common.ModuleType;
import liveStrategies.common.ModuleTypeOrder;

/**
 *
 * @author rodrigo
 */
public class AlligatorV1 extends Module{
    private IIndicators indicators;
    

    public AlligatorV1(IContext context, ModuleType type) {
        super(context, type);
    }
    public void newTick(Instrument instrument, ITick tick) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void newBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) {
        
    }
    
}
