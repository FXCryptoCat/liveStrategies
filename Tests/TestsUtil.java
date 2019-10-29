
package liveStrategies.Tests;

import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import liveStrategies.common.Util;

/**
 *
 * @author rescorsim
 */
public class TestsUtil {
    
    
    
    public static void main(String[] args){
        
        try {
            System.out.println("TestsUtil -> Util.getIntrumentLabel(Instrument): " + Util.getIntrumentLabel(Instrument.AUDJPY));
            System.out.println("TestsUtil -> Util.getFormattedTime(): " + Util.getFormattedTime());
            System.out.println("TestsUtil -> Util.getFormattedTime(\"yyyy-MM-dd_HH:mm:ss.SSS_\"): " + Util.getFormattedTime("yyyy-MM-dd_HH:mm:ss.SSS_"));
            System.out.println("TestsUtil -> Util.getFormattedTime(\"MM-dd-yyyy_HH:mm:ss.SSS\", \"GMT-03:00\"): " + Util.getFormattedTime("MM-dd-yyyy_HH:mm:ss.SSS", "GMT-03:00"));
            System.out.println("TestsUtil -> Util.getFormattedTime_BR(): " + Util.getFormattedTime_BR());
            System.out.println("TestsUtil -> Util.getFormattedTime_US(): " + Util.getFormattedTime_US());
            System.out.println("TestsUtil -> Util.getFormattedTime_GMT(): " + Util.getFormattedTime_GMT());
            
            
            TimeZone tz = TimeZone.getTimeZone("<local-time-zone>");
            System.out.println("TestsUtil -> Timezone -> DisplayName: " + tz.getDisplayName());
            System.out.println("TestsUtil -> Timezone -> ID: " + tz.getID());
            System.out.println("TestsUtil -> Timezone -> Description: " + tz.toString());
            
            
        } catch (JFException ex) {
            Logger.getLogger(TestsUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
    } 
}
