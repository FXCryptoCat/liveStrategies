
package liveStrategies.options;

import com.dukascopy.api.IStrategy;
import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.api.system.IClient;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.Instrument;

import java.util.HashSet;
import java.util.Set;
import liveStrategies.common.Account;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * This small program demonstrates how to initialize Dukascopy client and start a strategy
 */
public class StartStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartStrategy.class);

    private static IStrategy strategy;

    public StartStrategy(IStrategy st) {
        strategy = st;
    }

    public void start() throws Exception {
        //get the instance of the IClient interface
        final IClient client = Account.getTesterClient();
        //set the listener that will receive system events
        client.setSystemListener(new ISystemListener() {
            private int lightReconnects = 3;

        	@Override
        	public void onStart(long processId) {
                LOGGER.info("Strategy started: " + processId);
        	}

			@Override
			public void onStop(long processId) {
                LOGGER.info("Strategy stopped: " + processId);
                if (client.getStartedStrategies().size() == 0) {
                    System.exit(0);
                }
			}

			@Override
			public void onConnect() {
                LOGGER.info("Connected");
                lightReconnects = 3;
			}

			@Override
			public void onDisconnect() {
                LOGGER.warn("Disconnected");
                if (lightReconnects > 0) {
                    client.reconnect();
                    --lightReconnects;
                } else {
                    try {
                        //sleep for 10 seconds before attempting to reconnect
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        //ignore
                    }
                    try {
                        Account.connect();
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
			}
		});

        Account.connect();

        //subscribe to the instruments
        Set<Instrument> instruments = new HashSet<>();
        instruments.add(Instrument.EURUSD);
        LOGGER.info("Subscribing instruments...");
        client.setSubscribedInstruments(instruments);
                
        //start the strategy
        LOGGER.info("Starting strategy");
        client.startStrategy(strategy);
        //now it's running
    }
}
