/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package liveStrategies.common;

import com.dukascopy.api.ICurrency;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.system.IClient;
import com.dukascopy.api.system.ITesterClient;
import com.dukascopy.api.system.TesterFactory;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rodrigo
 */
public class Account {
    private static final Logger LOGGER = LoggerFactory.getLogger(Account.class);
    private static IClient client = null;
    private static ITesterClient testerClient = null;
    
    public static boolean useLiveAccount = false;
    
    private static String jnlpUrl = "https://www.dukascopy.com/client/live/jclient/jforex.jnlp";
    private static String userName = "Dyke782EU";
    private static String password = "Digodigo200423";
    private static String jnlpUrlDemo = "https://www.dukascopy.com/client/demo/jclient/jforex.jnlp";
    private static String userNameDemo = "DEMOAC06NQvxs"; 
    private static String passwordDemo = "NQvxs";
    private static String PIN = "0954";
    private static String ipFixo = "127.0.0.1";

    public static void createTesterClient() throws Exception{
        if(testerClient == null){
            testerClient = TesterFactory.getDefaultInstance();
            //client = TesterFactory.getDefaultInstance();
        }
        client = testerClient;
    }
    
    public static ITesterClient getTesterClient() throws Exception{
        if(testerClient == null){
            createTesterClient();
        }
        return testerClient;
    }
    
    public static void connect() throws Exception {
        if(client == null){
            createTesterClient();
        }
        if(useLiveAccount){
            LOGGER.info("Connecting Live Account...");
            client.connect(jnlpUrl, userName, password, Account.PinDialog.showAndGetPin());   
        }
        else{
            LOGGER.info("Connecting Demo Account...");
            client.connect(jnlpUrlDemo, userNameDemo, passwordDemo);  
        }

        int i = 10; //wait max ten seconds
        while (i > 0 && !client.isConnected()) {
            Thread.sleep(1000);
            i--;
        }
        if (!client.isConnected()) {
            LOGGER.error("Failed to connect Dukascopy servers");
            System.exit(1);
        }
    }
    
    public static void setDefaultTesterParameters() throws ParseException, InterruptedException, ExecutionException{
        //set instruments that will be used in testing
        Set<Instrument> instruments = new HashSet<>();
        instruments.add(Instrument.EURUSD);
        
        LOGGER.info("Subscribing instruments...");
        testerClient.setSubscribedInstruments(instruments);
        
        //setting initial deposit
        testerClient.setInitialDeposit(Instrument.EURUSD.getSecondaryJFCurrency(), 50000);

        // define date interval
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        
        Long from = dateFormat.parse("2015/08/20 00:00:00").getTime();
        Long to = dateFormat.parse("2015/10/05 00:00:00").getTime();
        
        //Long from = Util.getCurrentTimeStamp();
        //Long to = DateUtils.addMonths(new Date(), -1).getTime();
        
        //testerClient.setDataInterval(ITesterClient.DataLoadingMethod.ALL_TICKS, from, to);
        testerClient.setDataInterval(Period.TEN_SECS,OfferSide.BID,ITesterClient.InterpolationMethod.CLOSE_TICK,from,to);
        
        //load data
        LOGGER.info("Downloading data");
        Future<?> future = testerClient.downloadData(null);
        //wait for downloading to complete
        future.get();
    }
    
    public static void subscribeInstruments(Set<Instrument> instruments){
        LOGGER.info("Subscribing instruments...");
        testerClient.setSubscribedInstruments(instruments);
    }
    public static void subscribeInstrument(Instrument instrument){
        Set<Instrument> instruments = new HashSet<>();
        instruments.add(instrument);
        LOGGER.info("Subscribing instruments...");
        testerClient.setSubscribedInstruments(instruments);
    }
    
    public static void setInitialDeposit(double deposit){
        testerClient.setInitialDeposit(Instrument.EURUSD.getSecondaryJFCurrency(), deposit);
    }
    
    public static void setDateInterval(Period period, String from, String to) throws ParseException{
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Long _from = dateFormat.parse(from).getTime();
        Long _to = dateFormat.parse(to).getTime();
        
        testerClient.setDataInterval(period, OfferSide.ASK,ITesterClient.InterpolationMethod.CUBIC_SPLINE,_from,_to);
    }
    
    public static void setDateInterval(String from, String to) throws ParseException{
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Long _from = dateFormat.parse(from).getTime();
        Long _to = dateFormat.parse(to).getTime();
        testerClient.setDataInterval(ITesterClient.DataLoadingMethod.ALL_TICKS, _from, _to);
        
    }
    
    public static void downloadData() throws InterruptedException, ExecutionException{
        //load data
        LOGGER.info("Downloading data");
        Future<?> future = testerClient.downloadData(null);
        //wait for downloading to complete
        future.get();
    }
    
    
    @SuppressWarnings("serial")
	private static class PinDialog extends JDialog {
		
		private final JTextField pinfield = new JTextField();
		private final static JFrame noParentFrame = null;
		
		static String showAndGetPin() throws Exception{
			return new PinDialog(client).pinfield.getText();
		}

		public PinDialog(final IClient client) throws Exception {			
			super(noParentFrame, "PIN Dialog", true);
			
			JPanel captchaPanel = new JPanel();
			captchaPanel.setLayout(new BoxLayout(captchaPanel, BoxLayout.Y_AXIS));
			
			final JLabel captchaImage = new JLabel();
			captchaImage.setIcon(new ImageIcon(client.getCaptchaImage(jnlpUrl)));
			captchaPanel.add(captchaImage);
			
			
			captchaPanel.add(pinfield);
			getContentPane().add(captchaPanel);
			
			JPanel buttonPane = new JPanel();
			
			JButton btnLogin = new JButton("Login");
			buttonPane.add(btnLogin);
			btnLogin.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
					dispose();
				}
			});
			
			JButton btnReload = new JButton("Reload");
			buttonPane.add(btnReload);
			btnReload.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						captchaImage.setIcon(new ImageIcon(client.getCaptchaImage(jnlpUrl)));
					} catch (Exception ex) {
						LOGGER.info(ex.getMessage(), ex);
					}
				}
			});
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			pack();
			setVisible(true);
		}		
	}
    
}
