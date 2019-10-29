/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package liveStrategies.common;

import com.dukascopy.api.IOrder;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import liveStrategies.Temamet;

/**
 *
 * @author rescorsim
 */
public class Util {

    /****************************** Label Functions ******************************/
    private static int labelCounter = 1;
    public static String getIntrumentLabel (Instrument instrument) throws JFException{
        String label = instrument.toString();
        label = label.replace("/", "");
        label += getFormattedTime("_ddMMyyyy_HHmmss_SSS_","GMT-03:00");
        label += labelCounter;
        labelCounter++;
        return  label;
    }
    public static String getIntrumentLabel(Instrument instrument, String prefix) throws JFException {
        String label = instrument.toString();
        label = label.replace("/", "");
        label = prefix + "_" + label;
        label += getFormattedTime("_ddMMyyyy_HHmmss_SSS_", "GMT-03:00");
        label += labelCounter;
        labelCounter++;
        return label;
    }

    /****************************** Date Functions ******************************/
    public static Long getCurrentTimeStamp() {
        return new Date().getTime();
    }
    public static String getFormattedTime(){
        return new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS").format(new Date());
    }
    public static String getFormattedTime(String format){
        return new SimpleDateFormat(format).format(new Date());
    }
    public static String getFormattedTime(Date date, String format){
        return new SimpleDateFormat(format).format(date);
    }
    public static String getFormattedTime(String format, String gmt){
        SimpleDateFormat fmt = new SimpleDateFormat(format);
        fmt.setTimeZone(TimeZone.getTimeZone(gmt));
        return fmt.format(new Date());
    }
    public static String getFormattedTime_US(){
        SimpleDateFormat fmt = new SimpleDateFormat("MM-dd-yyyy_HH:mm:ss.SSS");
        fmt.setTimeZone(TimeZone.getTimeZone("GMT-04:00"));
        return fmt.format(new Date());
    }
    public static String getFormattedTime_BR(){
        SimpleDateFormat fmt = new SimpleDateFormat("dd-MM-yyyy_HH:mm:ss.SSS"); //
        fmt.setTimeZone(TimeZone.getTimeZone("GMT-03:00"));
        return fmt.format(new Date());
    }
    public static String getFormattedTime_GMT(){
        SimpleDateFormat fmt = new SimpleDateFormat("dd-MM-yyyy_HH:mm:ss.SSS"); //
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        return fmt.format(new Date());
    }
    public static Date getDateFromString(String str, String format, String timezone) throws ParseException{
	final SimpleDateFormat dateFormat = new SimpleDateFormat(format);
	dateFormat.setTimeZone(TimeZone.getTimeZone(timezone));
	return dateFormat.parse(str);
    }
    public static long getTimeFromString_GMT(String str) throws ParseException{
        return getDateFromString(str, "dd/MM/yyyy HH:mm:ss", "GMT").getTime();
    }
    public static long getTimeFromString_BR(String str) throws ParseException{
        return getDateFromString(str, "dd/MM/yyyy HH:mm:ss", "GMT-03:00").getTime();
    }
    public static Date getDateFromTimestamp(long timestamp) throws ParseException{
        return new Date(timestamp);
    }
    public static Calendar getCalendarFromDate(Date date){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }
    public static Calendar getCalendarFromTimeStamp(long timestamp) throws ParseException{
        return getCalendarFromDate(getDateFromTimestamp(timestamp));
    }
    public static int getDayFromTimestamp (long timestamp) throws ParseException{
        return getCalendarFromTimeStamp(timestamp).get(Calendar.DAY_OF_WEEK); // 1-7 for Sunday to Saturday. Calendar.TUESDAY
    }
    public static int getHourFromTimestamp (long timestamp) throws ParseException{
        return getCalendarFromTimeStamp(timestamp).get(Calendar.HOUR_OF_DAY); //0-23
    }
    public static int getMinuteFromTimestamp (long timestamp) throws ParseException{ // 0-59
        return getCalendarFromTimeStamp(timestamp).get(Calendar.MINUTE);
    }

    /****************************** Price Functions ******************************/
    public static double roundToPippette(double amount, Instrument instrument) {
        return round(amount, instrument.getPipScale() + 1);
    }
    public static double round(double amount, int decimalPlaces) {
        return (new BigDecimal(amount)).setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

//    protected void trailingStop(/*ITick tick*/) throws JFException {
//        for (IOrder order : engine.getOrders(defaultInstrument)) {
//            if (order.getState() == IOrder.State.FILLED) {
//                double profirLoss = order.getProfitLossInPips();
//                console.getOut().println("trailingStop -> profirLossInPips: "+profirLoss+
//                        ", trailingStopInPips: "+trailingStop);
//
//                    if (order.isLong()) {
//                        double newStopLossAsk = (this.getLastTick(defaultInstrument).getAsk() - trailingStop * defaultInstrument.getPipValue());
//                        double newStopLoss = roundToPippette(newStopLossAsk, defaultInstrument);
//                        double currentStopLossPrice = order.getStopLossPrice();
//                        console.getOut().println("trailingStopLong -> currentStopLossPrice: "+currentStopLossPrice+
//                            " < newStopLoss: "+newStopLoss);
//                        if (currentStopLossPrice < newStopLoss) {
//                            order.setStopLossPrice(newStopLoss);
//                        }
//                    }
//                    else{
//                        double newStopLossBid = (this.getLastTick(defaultInstrument).getBid() + trailingStop * defaultInstrument.getPipValue());
//                        double newStopLoss = roundToPippette(newStopLossBid, defaultInstrument);
//                        double currentStopLoss = order.getStopLossPrice();
//                        console.getOut().println("trailingStopShort -> currentStopLoss: "+currentStopLoss+
//                            " > newStopLoss: "+newStopLoss);
//                        if (currentStopLoss > newStopLoss) {
//                            order.setStopLossPrice(newStopLoss);
//                        }
//                    }
//            }
//        }
//    }

    /****************************** Intervals Functions ******************************/
    public static class DayTime{
        public int hour;
        public int min;
        public int sec;
        public DayTime(){hour=0;min=0;sec=0;}
        public DayTime(int h, int m, int s){hour=h;min=m;sec=s;}
        public String str(){return hour+":"+min+":"+sec;}
    }
    public static boolean checkBetweenInterval(long time, Map<DayTime, DayTime> intervals, boolean verbose){
        for (Map.Entry<DayTime, DayTime> entry : intervals.entrySet())
        {
            if(verbose){
                System.out.println("checkBetweenInterval -> Clock1: "+entry.getKey().str() + ", Clock2: " + entry.getValue().str());
            }
            Calendar currentTime = Calendar.getInstance(); // current date/time
            currentTime.setTimeInMillis(time);
            Calendar startTime = (Calendar)currentTime.clone();
            Calendar endTime = (Calendar)currentTime.clone();
            startTime.set(Calendar.HOUR_OF_DAY, entry.getKey().hour);
            startTime.set(Calendar.MINUTE, entry.getKey().min);
            startTime.set(Calendar.SECOND, entry.getKey().sec);
            endTime.set(Calendar.HOUR_OF_DAY, entry.getValue().hour);
            endTime.set(Calendar.MINUTE, entry.getValue().min);
            endTime.set(Calendar.SECOND, entry.getValue().sec);
            if (verbose){
                System.out.println("checkBetweenInterval -> currentTime: "+currentTime.getTime().toString()
                            + ", startTime: " + startTime.getTime().toString()
                            + ", endTime: " + endTime.getTime().toString()
                );
            }
            if(  (currentTime.after(startTime) && currentTime.before(endTime)) ){
                return true;
            }
        }
        return false;
    }
}
/*
Obs:
2. Converter timestamp em data legivel:
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS"); //
    fmt.setTimeZone(TimeZone.getTimeZone("GMT-03:00"));
    System.out.println(fmt.format(timestamp));
*/