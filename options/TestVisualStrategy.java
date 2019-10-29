
package liveStrategies.options;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.dukascopy.api.DataType;
import com.dukascopy.api.IChart;
import com.dukascopy.api.IStrategy;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.LoadingProgressListener;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.api.system.ITesterClient;
import com.dukascopy.api.system.tester.ITesterChartController;
import com.dukascopy.api.system.tester.ITesterExecution;
import com.dukascopy.api.system.tester.ITesterExecutionControl;
import com.dukascopy.api.system.tester.ITesterGui;
import com.dukascopy.api.system.tester.ITesterIndicatorsParameters;
import com.dukascopy.api.system.tester.ITesterUserInterface;
import com.dukascopy.api.system.tester.ITesterVisualModeParameters;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import liveStrategies.common.Account;
import liveStrategies.common.Util;
import org.apache.commons.lang3.time.DateUtils;
/**
 * This small program demonstrates how to initialize Dukascopy tester and start a strategy in GUI mode
 */
@SuppressWarnings("serial")
public final class TestVisualStrategy extends JFrame implements ITesterUserInterface, ITesterExecution {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestVisualStrategy.class);

    private final int frameWidth = 1000;
    private final int frameHeight = 600;
    private final int controlPanelHeight = 100;
    private final int controlPanelMaxHeight = 150;
    
    private JPanel currentChartPanel = null;
    private ITesterExecutionControl executionControl = null;
    
    private JPanel controlPanel = null;
    private JButton startStrategyButton = null;
    private JButton pauseButton = null;
    private JButton continueButton = null;
    private JButton cancelButton = null;
    private JPeriodComboBox jPeriodComboBox = null;
    
    JTextField textFieldInicio = null;
    JTextField textFieldFim = null;
    //Instrument instrument;
    Set<Instrument> instruments;
    Double initialDeposit;
    Period period;
    
    private Map<IChart, ITesterGui> chartPanels = null;

    private static IStrategy strategy;

    public TestVisualStrategy(IStrategy st) {
        strategy = st;
        instruments = new HashSet<Instrument>();
        instruments.add(Instrument.EURUSD);
        initialDeposit = 10000.0;
        period = Period.TEN_SECS;
        this.setTesterGUIMode();
    }
    public TestVisualStrategy(IStrategy st, Instrument _instrument, double _initialDeposit, Period _period) {
        strategy = st;
        instruments.add(_instrument);
        initialDeposit = _initialDeposit;
        //period = _period;
        period = Period.TEN_SECS;
        this.setTesterGUIMode();
    }
    public TestVisualStrategy(IStrategy st, Set<Instrument> _instruments, double _initialDeposit, Period _period) {
        strategy = st;
        instruments = _instruments;
        initialDeposit = _initialDeposit;
        //period = _period;
        period = Period.TEN_SECS;
        this.setTesterGUIMode();
    }
    
    public void setTime_TextField(){
        Date from = DateUtils.addMonths(new Date(), -1);
        Date to = new Date(); 
        textFieldInicio = new JTextField(Util.getFormattedTime(from, "yyyy/MM/dd HH:mm:ss"));
        textFieldFim = new JTextField(Util.getFormattedTime(to, "yyyy/MM/dd HH:mm:ss"));
    }
    
    public void setTesterGUIMode(){
        setTime_TextField();
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
    }
    
    @Override
    public void setChartPanels(Map<IChart, ITesterGui> chartPanels) {
        this.chartPanels = chartPanels;
        this.jPeriodComboBox.setChartPanels(chartPanels);
        
        if(chartPanels != null && chartPanels.size() > 0){
            
            IChart chart = chartPanels.keySet().iterator().next();
            Instrument instrument = chart.getInstrument();
            setTitle(instrument.toString() + " " + chart.getSelectedOfferSide() + " " + chart.getSelectedPeriod());
            
            JPanel chartPanel = chartPanels.get(chart).getChartPanel();
            addChartPanel(chartPanel);
        }
    }

    @Override
    public void setExecutionControl(ITesterExecutionControl executionControl) {
        this.executionControl = executionControl;
    }
    
    public void startStrategy() throws Exception {
        //get the instance of the IClient interface
        final ITesterClient client = Account.getTesterClient();
        //set the listener that will receive system events
        client.setSystemListener(new ISystemListener() {
            @Override
            public void onStart(long processId) {
                LOGGER.info("Strategy started: " + processId);
                updateButtons();
            }

            @Override
            public void onStop(long processId) {
                LOGGER.info("Strategy stopped: " + processId);
                resetButtons();
                
                File reportFile = new File("C:\\report.html");
                try {
                    client.createReport(processId, reportFile);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
                if (client.getStartedStrategies().size() == 0) {
                    //Do nothing
                }
            }

            @Override
            public void onConnect() {
                LOGGER.info("Connected");
            }

            @Override
            public void onDisconnect() {
                //tester doesn't disconnect
            }
        });

        Account.connect();
        //Account.setDefaultTesterParameters();
        Account.subscribeInstruments(instruments);
        Account.setInitialDeposit(initialDeposit);
        // comentar period para carregar data no metodo ALL TICKS
        Account.setDateInterval(/*period,*/ textFieldInicio.getText(), textFieldFim.getText());
        Account.downloadData();
        
        //start the strategy
        LOGGER.info("Starting strategy");

        // Implementation of IndicatorParameterBean 
        final class IndicatorParameterBean implements ITesterIndicatorsParameters {
            @Override
            public boolean isEquityIndicatorEnabled() {
                return true;
            }
            @Override
            public boolean isProfitLossIndicatorEnabled() {
                return true;
            }
            @Override
            public boolean isBalanceIndicatorEnabled() {
                return true;
            }
        }
        // Implementation of TesterVisualModeParametersBean
        final class TesterVisualModeParametersBean implements ITesterVisualModeParameters {
            @Override
            public Map<Instrument, ITesterIndicatorsParameters> getTesterIndicatorsParameters() {
                Map<Instrument, ITesterIndicatorsParameters> indicatorParameters = new HashMap<Instrument, ITesterIndicatorsParameters>();
                IndicatorParameterBean indicatorParameterBean = new IndicatorParameterBean();
                for(Instrument i : instruments){
                    indicatorParameters.put(i, indicatorParameterBean);
                }
//              indicatorParameterBean indicatorParameterBean = new IndicatorParameterBean();
//              indicatorParameters.put(Instrument.EURUSD, indicatorParameterBean);
                return indicatorParameters;
            }
        }
        // Create TesterVisualModeParametersBean
        TesterVisualModeParametersBean visualModeParametersBean = new TesterVisualModeParametersBean();
   
        // Start strategy
        client.startStrategy(
            strategy,
            new LoadingProgressListener() {
                @Override
                public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
                    LOGGER.info(information);
                }

                @Override
                public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime) {
                }

                @Override
                public boolean stopJob() {
                    return false;
                }
            },
            visualModeParametersBean,
            this,
            this
        );
        //now it's running
    }
    
    /**
     * Center a frame on the screen 
     */
    private void centerFrame(){
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        int screenHeight = screenSize.height;
        int screenWidth = screenSize.width;
        setSize(screenWidth / 2, screenHeight / 2);
        setLocation(screenWidth / 4, screenHeight / 4);
    }
    
    /**
     * Add chart panel to the frame
     * @param panel
     */
    private void addChartPanel(JPanel chartPanel){
        removecurrentChartPanel();
        
        this.currentChartPanel = chartPanel;
        chartPanel.setPreferredSize(new Dimension(frameWidth, frameHeight - controlPanelHeight));
        chartPanel.setMinimumSize(new Dimension(frameWidth, 200));
        chartPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        getContentPane().add(chartPanel);
        this.validate();
        chartPanel.repaint();
    }
    
	private ITesterChartController getChartController() {
		if (chartPanels == null || chartPanels.size() == 0) {
			return null;
		}
		IChart chart = chartPanels.keySet().iterator().next();
		ITesterGui gui = chartPanels.get(chart);
		ITesterChartController chartController = gui.getTesterChartController();
		return chartController;
	}

    /**
     * Add buttons to start/pause/continue/cancel actions  and other buttons
     */
    private void addControlPanel(){
        
        controlPanel = new JPanel();
        FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
        controlPanel.setLayout(flowLayout);
        controlPanel.setPreferredSize(new Dimension(frameWidth, controlPanelHeight));
        controlPanel.setMinimumSize(new Dimension(frameWidth, controlPanelHeight));
        controlPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, controlPanelMaxHeight));

        startStrategyButton = new JButton("Start strategy");
        startStrategyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startStrategyButton.setEnabled(false);
                Runnable r = new Runnable() {
                    public void run() {
                        try {
                            startStrategy();
                        } catch (Exception e2) {
                            LOGGER.error(e2.getMessage(), e2);
                            e2.printStackTrace();
                            resetButtons();
                        }
                    }
                };
                Thread t = new Thread(r);
                t.start();
            }
        });
        
        pauseButton = new JButton("Pause");
        pauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(executionControl != null){
                    executionControl.pauseExecution();
                    updateButtons();
                }
            }
        });
        
        continueButton = new JButton("Continue");
        continueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(executionControl != null){
                    executionControl.continueExecution();
                    updateButtons();
                }
            }
        });
        
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(executionControl != null){
                    executionControl.cancelExecution();
                    updateButtons();
                }
            }
        });
        
        jPeriodComboBox = new JPeriodComboBox(this);
        
        List<JButton> chartControlButtons = new ArrayList<JButton>();
        
        chartControlButtons.add(new JButton("Add Indicators") {{
        	addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                
                	getChartController().addIndicators();
                }
            });
        }});
        
        chartControlButtons.add(new JButton("Add Price Marker") {{
        	addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                
                	getChartController().activatePriceMarker();
                }
            });
        }});
        
        chartControlButtons.add(new JButton("Add Time Marker") {{
        	addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                
                	getChartController().activateTimeMarker();
                }
            });
        }});
        
        chartControlButtons.add(new JButton("Chart Auto Shift") {{
        	addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                
                	getChartController().setChartAutoShift();
                }
            });
        }});
        
        chartControlButtons.add(new JButton("Add Percent Lines") {{
        	addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                
                	getChartController().activatePercentLines();
                }
            });
        }});
        
        chartControlButtons.add(new JButton("Add Channel Lines") {{
        	addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                
                	getChartController().activateChannelLines();
                }
            });
        }});
        
        chartControlButtons.add(new JButton("Add Poly Line") {{
        	addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                
                	getChartController().activatePolyLine();
                }
            });
        }});        
        
        chartControlButtons.add(new JButton("Add Short Line") {{
        	addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                
                	getChartController().activateShortLine();
                }
            });
        }});
                
        chartControlButtons.add(new JButton("Add Long Line") {{
        	addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                
                	getChartController().activateLongLine();
                }
            });
        }});
                
        chartControlButtons.add(new JButton("Add Ray Line") {{
        	addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                
                	getChartController().activateRayLine();
                }
            });
        }});        
        
        chartControlButtons.add(new JButton("Add Horizontal Line") {{
        	addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                
                	getChartController().activateHorizontalLine();
                }
            });
        }});
        
        chartControlButtons.add(new JButton("Add Vertical Line") {{
        	addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                
                	getChartController().activateVerticalLine();
                }
            });
        }});
        
        chartControlButtons.add(new JButton("Add Text") {{
        	addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                
                	getChartController().activateTextMode();
                }
            });
        }});
        
        chartControlButtons.add(new JButton("Zoom In") {{
        	addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                
                	getChartController().zoomIn();
                }
            });
        }});
        
        chartControlButtons.add(new JButton("Zoom Out") {{
        	addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                
                	getChartController().zoomOut();
                }
            });
        }});
        
        chartControlButtons.add(new JButton("add OHLC Index") {{
        	addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                
                	getChartController().addOHLCInformer();
                }
            });
        }});
        
        chartControlButtons.add(new JButton("Bid") {{
        	addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                
                	getChartController().switchOfferSide(OfferSide.BID);
                }
            });
        }});
        
        chartControlButtons.add(new JButton("Ask") {{
        	addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {                
                	getChartController().switchOfferSide(OfferSide.ASK);
                }
            });
        }});
        
        controlPanel.add(startStrategyButton);
        controlPanel.add(pauseButton);
        controlPanel.add(continueButton);
        controlPanel.add(cancelButton);
        
        controlPanel.add(jPeriodComboBox);
        
        for(JButton btn : chartControlButtons){
        	controlPanel.add(btn);
        }
        controlPanel.add(new JTextArea("Início"));
        controlPanel.add(textFieldInicio);
        controlPanel.add(new JTextArea("Fim"));
        controlPanel.add(textFieldFim);
        
        getContentPane().add(controlPanel);
        
        pauseButton.setEnabled(false);
        continueButton.setEnabled(false);
        cancelButton.setEnabled(false);
        
    }

    private void updateButtons(){
        if(executionControl != null){
            startStrategyButton.setEnabled(executionControl.isExecutionCanceled());
            pauseButton.setEnabled(!executionControl.isExecutionPaused() && !executionControl.isExecutionCanceled());
            cancelButton.setEnabled(!executionControl.isExecutionCanceled());
            continueButton.setEnabled(executionControl.isExecutionPaused());
            
        }
    }

    private void resetButtons(){
        startStrategyButton.setEnabled(true);
        pauseButton.setEnabled(false);
        continueButton.setEnabled(false);
        cancelButton.setEnabled(false);
    }
    
    private void removecurrentChartPanel(){
        if(this.currentChartPanel != null){
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        TestVisualStrategy.this.getContentPane().remove(TestVisualStrategy.this.currentChartPanel);
                        TestVisualStrategy.this.getContentPane().repaint();
                    }
                });             
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    public void showChartFrame(){
        setSize(frameWidth, frameHeight);
        centerFrame();
        addControlPanel();
        setVisible(true);
    }
    
//    public static void main(String[] args) throws Exception {
//        GUIModeChartControls testerMainGUI = new GUIModeChartControls();
//        testerMainGUI.showChartFrame();
//    }
}

@SuppressWarnings("serial")
class JPeriodComboBox extends JComboBox implements ItemListener {
    private JFrame mainFrame = null;
    private Map<IChart, ITesterGui> chartPanels = null;
    private Map<Period, DataType> periods = new LinkedHashMap<Period, DataType>();
    
    public void setChartPanels(Map<IChart, ITesterGui> chartPanels) {
        this.chartPanels = chartPanels;
        
        IChart chart = chartPanels.keySet().iterator().next();
        this.setSelectedItem(chart.getSelectedPeriod());
    }

    public JPeriodComboBox(JFrame mainFrame){
        this.mainFrame = mainFrame;
        this.addItemListener(this);
        //periods.put(Period.TICK, DataType.TICKS);
        //periods.put(Period.TEN_SECS, DataType.TIME_PERIOD_AGGREGATION);
        //periods.put(Period.THIRTY_SECS, DataType.TIME_PERIOD_AGGREGATION);
        periods.put(Period.ONE_MIN, DataType.TIME_PERIOD_AGGREGATION);
        periods.put(Period.FIVE_MINS, DataType.TIME_PERIOD_AGGREGATION);
        periods.put(Period.TEN_MINS, DataType.TIME_PERIOD_AGGREGATION);
        periods.put(Period.FIFTEEN_MINS, DataType.TIME_PERIOD_AGGREGATION);
        periods.put(Period.THIRTY_MINS, DataType.TIME_PERIOD_AGGREGATION);
        periods.put(Period.ONE_HOUR, DataType.TIME_PERIOD_AGGREGATION);
        periods.put(Period.FOUR_HOURS, DataType.TIME_PERIOD_AGGREGATION);
        periods.put(Period.DAILY, DataType.TIME_PERIOD_AGGREGATION);
        periods.put(Period.WEEKLY, DataType.TIME_PERIOD_AGGREGATION);
        periods.put(Period.MONTHLY, DataType.TIME_PERIOD_AGGREGATION);
        
        for(Period period: periods.keySet()){
            this.addItem(period);
        }
    }
    
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            if(chartPanels != null && chartPanels.size() > 0){
                IChart chart = chartPanels.keySet().iterator().next();
                ITesterGui gui = chartPanels.get(chart);
                ITesterChartController chartController = gui.getTesterChartController();

                Period period = (Period)e.getItem();
                DataType dataType = periods.get(period); 
                
                chartController.changePeriod(dataType, period);
                mainFrame.setTitle(chart.getInstrument().toString() + " " + chart.getSelectedOfferSide() + " " + chart.getSelectedPeriod());
            }
        }
    }
}
