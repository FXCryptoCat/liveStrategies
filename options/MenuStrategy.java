/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package liveStrategies.options;

import com.dukascopy.api.IStrategy;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.Period;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import liveStrategies.common.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author rodrigo
 */
public final class MenuStrategy extends JFrame{
    private static final Logger LOGGER = LoggerFactory.getLogger(MenuStrategy.class);
    private static IStrategy strategy;
    
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
    
    public Double initialDeposit;
    public Set<Instrument> instruments;
    public Period period;
    
    
    public MenuStrategy(IStrategy st, Set<Instrument> _instruments , Double _initialDeposit, Period _period) {
        strategy = st;
        instruments = _instruments;
        initialDeposit = _initialDeposit;
        period = _period;
    }
    public MenuStrategy(IStrategy st, Instrument _instrument , Double _initialDeposit, Period _period) {
        strategy = st;
        instruments = new HashSet<Instrument>();
        instruments.add(_instrument);
        initialDeposit = _initialDeposit;
        period = _period;
    }
    
    public MenuStrategy(IStrategy st) {
        strategy = st;
        instruments = new HashSet<Instrument>();
        instruments.add(Instrument.EURUSD);
        initialDeposit = 100.0;
        period = Period.ONE_MIN;
    }
    
    public MenuStrategy(IStrategy st, Instrument instrument) {
        strategy = st;
        instruments = new HashSet<Instrument>();
        instruments.add(instrument);
        initialDeposit = 100.0;
        period = Period.ONE_MIN;
    }
    
    public void showMenuStrategy() {
//        MenuStrategy menuStrategy = new MenuStrategy(strategy);
//        menuStrategy.pack();
//        menuStrategy.setDefaultCloseOperation( EXIT_ON_CLOSE );
//        menuStrategy.showFrame();
        this.pack();
        this.setDefaultCloseOperation( EXIT_ON_CLOSE );
        this.showFrame();
    }
    
    public void showFrame(){
        setSize(frameWidth, frameHeight);
        addControlPanel();
        setVisible(true);
    }
    
    public JPanel getMenuPanel(){        
        return this.controlPanel;
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

        testVisualStrategyButton = new JButton("Test visual strategy");
        testVisualStrategyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Runnable r = new Runnable() {
                    public void run() {
                        try {
                            TestVisualStrategy testerGuiCtrl = new TestVisualStrategy(strategy, instruments, initialDeposit, period);
                            testerGuiCtrl.showChartFrame();
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
        
        testStrategyButton = new JButton("Test strategy");
        testStrategyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Runnable r = new Runnable() {
                    public void run() {
                        try {
                            TestStrategy testStrategy = new TestStrategy();
                            testStrategy.addStrategy("report_" + Util.getFormattedTime() + ".html", strategy);
                            testStrategy.runTester();
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
                            new StartStrategy(strategy).start();
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
        controlPanel.add(testVisualStrategyButton);
        controlPanel.add(testStrategyButton);
        controlPanel.add(startStrategyButton);
        controlPanel.add(stopStrategyButton);
        controlPanel.add(startRemoteStrategyButton);
        controlPanel.add(stopRemoteStrategyButton);
        
        getContentPane().add(controlPanel);

    }
    
}
