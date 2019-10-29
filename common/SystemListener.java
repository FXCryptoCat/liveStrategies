/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package liveStrategies.common;

import com.dukascopy.api.system.ISystemListener;
import liveStrategies.options.TestStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rodrigo
 */
public class SystemListener implements ISystemListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestStrategy.class);
    private String strategyName;
    TestStrategy testStrategy;
    
    public SystemListener(TestStrategy _testStrategy, String _strategyName){
        strategyName = _strategyName;
        testStrategy = _testStrategy;
    }
    @Override
    public void onStart(long processId) {
        LOGGER.info("Strategy started: " + strategyName + ", processId:" + processId);
        testStrategy.startedStrategy(processId, strategyName);
    }
    @Override
    public void onStop(long processId) {
        LOGGER.info("Strategy stopped: " + strategyName + ", processId:" + processId);
        testStrategy.finishedStrategy(processId);   
    }

    @Override
    public void onConnect() {
        LOGGER.info("Connected");
    }

    @Override
    public void onDisconnect() {
        //tester doesn't disconnect
    }
}
