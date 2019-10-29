
package liveStrategies.options;

import com.dukascopy.api.IStrategy;
import com.dukascopy.api.LoadingProgressListener;
import com.dukascopy.api.system.ITesterClient;
import java.io.BufferedWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import liveStrategies.common.Account;
import liveStrategies.common.SystemListener;

/**
 * This small program demonstrates how to initialize Dukascopy tester and start a strategy
 */
public class TestStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestStrategy.class);
    private String reportDir, reportSummary; 
    private ITesterClient client;
    private Map<String, IStrategy> strategies;
    private Map<Long, String> running;
    private Map<Double, String> sortedSummaryReport;
    private int maxThreads;
    private int activeThreads;
    private BufferedWriter buffWrite;
    
    public TestStrategy() {
        reportDir = "reports/";
        reportSummary = "ReportSummary.html";
        this.activeThreads = 0;
        this.maxThreads = 4;
        this.strategies = new HashMap<String, IStrategy>();
        this.running = new HashMap<Long, String>();
        sortedSummaryReport = new TreeMap<Double, String>();
    }
    public void setReportDir(String dir){
        reportDir = reportDir + dir + "/";
        reportSummary = dir + ".html";
    }
    public void seMaxThreads(int n){
        this.maxThreads = n;
    }
    
    public synchronized void addStrategy(String name, IStrategy strategy){
        LOGGER.info("addStrategy(): " + name);
        this.strategies.put(name, strategy);
    }
    public synchronized void delStrategy(String name){
        this.strategies.remove(name);
    }
    public synchronized void startedStrategy(Long processId, String name){
        this.running.put(processId, name);
    }
    public synchronized void finishedStrategy(Long processId){
        if(this.running.containsKey(processId)){
            String strategyName = this.running.get(processId);
            
            String report = reportDir + strategyName;
            LOGGER.info("Generating Report: " + report);
            File reportFile = new File(report);
            try {
                client.createReport(processId, reportFile);
                
                String reportLine = "Strategy: " + strategyName +
                    ", InitialDeposit:" + client.getReportData(processId).getInitialDeposit()+
                    ", Orders:" + client.getReportData(processId).getClosedOrders().size()+
                    ", Comission:" + client.getReportData(processId).getCommission()+
                    ", FinishDeposit: "+ client.getReportData(processId).getFinishDeposit();
                buffWrite.append(reportLine);
                buffWrite.newLine();
                buffWrite.flush();
                
                this.sortedSummaryReport.put(client.getReportData(processId).getFinishDeposit(), reportLine);

            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
            
            this.running.remove(processId);
            this.activeThreads--;
        }
        else{
            LOGGER.error("finishedStrategy(): ProcessId not found!");
        }
        this.strategies.remove(processId);
    }

    //public static void main(String[] args) throws Exception {
    public void runTester() throws Exception{
        //get the instance of the IClient interface
        client = Account.getTesterClient();
        
        (new File(reportDir)).mkdirs();
        buffWrite = new BufferedWriter(new FileWriter(new File(reportDir + reportSummary)));
        
        for (String strategyName : strategies.keySet()) { //Capturamos o valor a partir da chave String value = example.get(key); System.out.println(key + " = " + value); }
            while(this.activeThreads >= this.maxThreads){
                Thread.sleep(1000);
            }
            
            IStrategy strategy = strategies.get(strategyName);
            SystemListener listener = new SystemListener(this, strategyName);
            client.setSystemListener(listener);
            
            //start the strategy
            LOGGER.info("Starting strategy: " + strategyName);
            client.startStrategy(strategy, new LoadingProgressListener() {
                @Override
                public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
                    LOGGER.info( information);
                }
                @Override
                public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime) {
                }
                @Override
                public boolean stopJob() {
                    return false;
                }
            });
            this.activeThreads++;
     
        //now it's running
        }
        buffWrite.close();
        strategies.clear();
        
        // generate sorted summary report
        String sortedReportName = "sorted" + reportSummary;
        LOGGER.info("Generating sortedSummaryReport: " + sortedReportName);
        BufferedWriter buffWrite2 = new BufferedWriter(new FileWriter(reportDir + sortedReportName));
        for(Map.Entry<Double,String> entry : sortedSummaryReport.entrySet()) {
          System.out.println(entry.getKey() + " => " + entry.getValue());
          buffWrite2.append(entry.getValue());
          buffWrite2.newLine();
        }
        buffWrite2.close();
        
        sortedSummaryReport.clear();
    }
}
