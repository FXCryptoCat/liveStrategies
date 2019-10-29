
package liveStrategies.common;

import java.util.ArrayList;

/**
 *
 * @author rescorsim
 */
public abstract class Match {
    
    public void matchModules(){ 
    }
    
    public abstract ArrayList<Order> matchModules(ArrayList<Module> modules);
    
}
