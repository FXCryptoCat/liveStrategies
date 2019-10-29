package liveStrategies;

import com.dukascopy.api.Configurable;
import com.dukascopy.api.Filter;
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import liveStrategies.options.MenuStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rescorsim
 */
public class MovingAverageControl extends JFrame implements IStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(MenuStrategy.class);
    private IEngine engine;
    private IConsole console;
    private IHistory history;
    private IContext context;
    private IIndicators indicators;
    private IChart chart;
    private IOrder order;
    
    private final int frameWidth = 500;
    private final int frameHeight = 300;
    private final int controlPanelHeight = 100;
    private final int controlPanelMaxHeight = 150;
    
    private JPanel controlPanel = null;
    private JButton connectButton = null;
    private JButton testVisualStrategyButton = null;
    private JButton testStrategyButton = null;
    private JButton startStrategyButton = null;
    private JButton startRemoteStrategyButton = null;
    private JButton stopStrategyButton = null;
    private JButton stopRemoteStrategyButton = null;

    private List<IOrder> PendingPositions =  null ;
    private List<IOrder> AllPositions =  null ;
    private List<IOrder> OpenPositions =  null ;
    
    private String name = "strategyName";
    private int labelCounter = 1;
    private double bidPrice;
    private double askPrice;
    int indicatorPeriodFast = 5;
    int indicatorPeriodSlow = 83;
    
    private static MovingAverageControl movingAverageControl;
    public static void main(String[] args) throws Exception {
        movingAverageControl = new MovingAverageControl();
    }

    public MovingAverageControl() {
        this.showFrame();
    }
    
    @Override
    public void onStart(IContext context) throws JFException {
        this.engine = context.getEngine();
        this.console = context.getConsole();
        this.history = context.getHistory();
        this.context = context;
        this.indicators = context.getIndicators();
        console.getOut().println("Strategy -> onStart()");
        
//        Set subscribedInstruments = new HashSet();
//        subscribedInstruments.add(defaultInstrument);
//        this.context.setSubscribedInstruments(subscribedInstruments); 

    }

    @Override
    public void onTick(Instrument instrument, ITick tick) throws JFException {
        //if (!instrument.equals(defaultInstrument)) return;   
        
    }

    @Override
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
        //if (!instrument.equals(defaultInstrument) || period != defaultPeriod) return;   
        if (askBar.getVolume() == 0) return;
        askPrice = askBar.getClose();
        bidPrice = bidBar.getClose();
        try {
            
        } catch (Exception e) {
        }
        
    }

    @Override
    public void onMessage(IMessage message) throws JFException {
        
    }

    @Override
    public void onAccount(IAccount account) throws JFException {
        
    }

    @Override
    public void onStop() throws JFException {
        
    }
    
    private void updateVariables(Instrument instrument) {
        try {
            AllPositions = engine.getOrders(instrument);
            List<IOrder> listMarket = new ArrayList<IOrder>();
            for (IOrder order: AllPositions) {
                if (order.getState().equals(IOrder.State.FILLED)){
                    listMarket.add(order);
                }
            }
            List<IOrder> listPending = new ArrayList<IOrder>();
            for (IOrder order: AllPositions) {
                if (order.getState().equals(IOrder.State.OPENED)){
                    listPending.add(order);
                }
            }
            OpenPositions = listMarket;
            PendingPositions = listPending;
        } catch(JFException e) {
            e.printStackTrace();
        }
    }
    
    public String getIntrumentLabel (Instrument instrument) throws JFException{
        String label = instrument.toString();
        label = label.replace("/", "");
        label += getFormattedTime("_ddMMyyyy_HHmmss_SSS_","GMT-03:00");
        label += labelCounter;
        labelCounter++;
        return  label;
    }
    public String getIntrumentLabel(Instrument instrument, String prefix) throws JFException {
        String label = instrument.toString();
        label = label.replace("/", "");
        label = prefix + "_" + label;
        label += getFormattedTime("_ddMMyyyy_HHmmss_SSS_", "GMT-03:00");
        label += labelCounter;
        labelCounter++;
        return label;
    }
    public String getFormattedTime(String format, String gmt){
        SimpleDateFormat fmt = new SimpleDateFormat(format);
        fmt.setTimeZone(TimeZone.getTimeZone(gmt));
        return fmt.format(new Date());
    }
    
    public void showFrame(){
        this.pack();
        this.setDefaultCloseOperation( EXIT_ON_CLOSE );
        setSize(frameWidth, frameHeight);
        addControlPanel();
        setVisible(true);
    }
    
    private void addControlPanel(){
         
        controlPanel = new JPanel();
        FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
        controlPanel.setLayout(flowLayout);
        controlPanel.setPreferredSize(new Dimension(frameWidth, controlPanelHeight));
        controlPanel.setMinimumSize(new Dimension(frameWidth, controlPanelHeight));
        controlPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, controlPanelMaxHeight));
        
        connectButton = new JButton("Connect");
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Runnable r = new Runnable() {
                    public void run() {
                        try {

                        } catch (Exception e2) {
                            LOGGER.error(e2.getMessage(), e2);
                            e2.printStackTrace();
                        }
                    }
                };
                Thread t = new Thread(r);
                t.start();
            }
        });

        startStrategyButton = new JButton("Start strategy");
        startStrategyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Runnable r = new Runnable() {
                    public void run() {
                        try {
                            //new StartStrategy(this).start();
                        } catch (Exception e2) {
                            LOGGER.error(e2.getMessage(), e2);
                            e2.printStackTrace();
                        }
                    }
                };
                Thread t = new Thread(r);
                t.start();
            }
        });
        
        stopStrategyButton = new JButton("Stop strategy");
        stopStrategyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Runnable r = new Runnable() {
                    public void run() {
                        try {

                        } catch (Exception e2) {
                            LOGGER.error(e2.getMessage(), e2);
                            e2.printStackTrace();
                        }
                    }
                };
                Thread t = new Thread(r);
                t.start();
            }
        });
        
        startRemoteStrategyButton = new JButton("Start Remote strategy");
        startRemoteStrategyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Runnable r = new Runnable() {
                    public void run() {
                        try {

                        } catch (Exception e2) {
                            LOGGER.error(e2.getMessage(), e2);
                            e2.printStackTrace();
                        }
                    }
                };
                Thread t = new Thread(r);
                t.start();
            }
        });
        
        stopRemoteStrategyButton = new JButton("Stop Remote strategy");
        stopRemoteStrategyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Runnable r = new Runnable() {
                    public void run() {
                        try {

                        } catch (Exception e2) {
                            LOGGER.error(e2.getMessage(), e2);
                            e2.printStackTrace();
                        }
                    }
                };
                Thread t = new Thread(r);
                t.start();
            }
        });
        
        controlPanel.add(connectButton);        
        controlPanel.add(startStrategyButton);
        controlPanel.add(stopStrategyButton);
        controlPanel.add(startRemoteStrategyButton);
        controlPanel.add(stopRemoteStrategyButton);
        
        getContentPane().add(controlPanel);

    }
}
