import sfa.timeseries.MultiVariateTimeSeries;
import sfa.timeseries.TimeSeries;

import javax.sdp.Time;
import java.util.*;

public class IOTDevice implements Cloneable{

    String IP;
    String type;
    String label;
    int classLabel;
    List<Data> timeseries;
    static Map<String, Integer> labels = new HashMap<>();
    static int numLabels = 1;
    long TS_INTERVAL = 3600000000L; // one hour in us (micro seconds)
    final int tsLength = 3601;

    int MAX = 0;
    int TOTAL = 1;
    int AVG = 2;
    int ACTUAL = 3;

    public IOTDevice(String IP, String type, String label) {
        this.IP = IP;
        this.type = type;
        this.label = label;
//        String[] splitType = type.split("-");
//        String deviceType = "";
//        if (splitType.length == 4 || splitType.length == 5) {
//            deviceType = splitType[2];
//        } else if (splitType.length == 3) {
//            deviceType = splitType[1];
//        } else if (splitType.length == 2) {
//            deviceType = splitType[0];
//        }
//        if (!labels.containsKey(deviceType)) {
//            labels.put(deviceType, numLabels);
//            numLabels++;
//        }

        //System.out.println("numLabels= " + numLabels);

        switch(this.label) {
            case "cam":
                classLabel = 1;
                break;
            case "burst":
                classLabel = 2;
                break;
            case "multi":
                classLabel = 3;
                break;
            case "laptop":
                classLabel  = 4;
                break;
        }

//        classLabel = labels.get(deviceType);
        //tv, audio, tv_dongle, sensor, doorbell, cam, weather, hub, light, switch, other, tablet, router, roomba,game, fitbit
/*
        switch (this.label) {
            case "tv":
                classLabel = 1;
                break;
            case "audio":
                classLabel = 2;
                break;
            case "tv_dongle":
                classLabel = 3;
                break;
            case "sensor":
                classLabel = 4;
                break;
            case "doorbell":
                classLabel = 5;
                break;
            case "cam":
                classLabel = 6;
                break;
            case "weather":
                classLabel = 7;
                break;
            case "hub":
                classLabel = 8;
                break;
            case "light":
                classLabel = 9;
                break;
            case "switch":
                classLabel = 10;
                break;
            case "other":
                classLabel = 11;
                break;
            case "tablet":
                classLabel = 12;
                break;
            case "router":
                classLabel = 13;
                break;
            case "roomba":
                classLabel = 14;
                break;
            case "game":
                classLabel = 15;
                break;
            case "fitbit":
                classLabel = 16;
                break;
        }
*/
        timeseries = new ArrayList<>();
    }


    public void addData(Data dp) {

        timeseries.add(dp);
    }


    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public String getType() {
        return type;
    }

    public int getClassLabel() {
        return classLabel;
    }

    public void setType(String type) {
        this.type = type;
    }

    public MultiVariateTimeSeries getMTS(boolean isHour) {


        TimeSeries tss[] = null;
        if (!isHour) {
            tss = new TimeSeries[7];
            tss[0] = getDataTimeSeries(MAX, IOTParser.TCP, IOTParser.SEND);
            tss[1] = getIATTS(AVG);
            tss[2] = getDataTimeSeries(TOTAL, IOTParser.TCP, IOTParser.SEND);
            tss[3] = getDataTimeSeries(AVG, IOTParser.TCP, IOTParser.SEND);
            // new ones
            tss[4] = getDataTimeSeries(MAX, IOTParser.UDP, IOTParser.SEND);
            tss[5] = getDataTimeSeries(MAX, IOTParser.TCP, IOTParser.RECV);
            tss[6] = getDataTimeSeries(MAX, IOTParser.UDP, IOTParser.RECV);
        } else {
            // it is hour data so we have different time series
            tss = new TimeSeries[5];
            tss[0] = getDataTimeSeries(ACTUAL, IOTParser.TCP, IOTParser.SEND);
            tss[1] = getDataTimeSeries(ACTUAL, IOTParser.TCP, IOTParser.RECV);
            tss[2] = getDataTimeSeries(ACTUAL, IOTParser.UDP, IOTParser.SEND);
            tss[3] = getDataTimeSeries(ACTUAL, IOTParser.UDP, IOTParser.RECV);
            tss[4] = getIATTS(ACTUAL);
        }
        return new MultiVariateTimeSeries(tss, (double) classLabel);
    }

    public TimeSeries getDataTimeSeries(int type, int protocol, int sendOrRecv) {
        double ts[] = new double[tsLength];

        //timeseries.stream().forEach(dp -> ts[(int) dp.arrivalTime] += dp.dataLength);
        int index = 0;
        long start = -1;

        Collections.sort(timeseries, new Comparator<Data>() {
            @Override
            public int compare(Data o1, Data o2) {
                return ((Long)o1.arrivalTime).compareTo(o2.arrivalTime);
            }
        });

        int count = 0;
        for (int i = 0; i < timeseries.size(); i++) {
            if (timeseries.get(i).getProtocol() == protocol && timeseries.get(i).getIsSendOrRecv() == sendOrRecv) {
                if (start == -1) {
                    start = timeseries.get(i).getArrivalTime();
                }
                if (timeseries.get(i).getArrivalTime() - start <= TS_INTERVAL) {
                    if (type == MAX) {
                        ts[index] = Math.max(ts[index], timeseries.get(i).getDataLength());
                    } else if (type != ACTUAL){
                        ts[index] += timeseries.get(i).getDataLength();
                    } else if (type == ACTUAL) {
                        ts[(int) ((timeseries.get(i).getArrivalTime() - start) / 1000000)] += timeseries.get(i).getDataLength();
                    }
                    count++;
                    if (timeseries.get(i).getArrivalTime() - start < 0)
                        System.err.println("diff = " + (timeseries.get(i).getArrivalTime() - start) + "Arrival time = " + timeseries.get(i).getArrivalTime() + " start time = " + start);
                } else {
                    if (type == AVG && count > 0) {
                        // it might be that there was only a single value within a whole hour so need to ensure count > 0
                        ts[index] = ts[index] / (double) count;
                    }
                    count = 0;
                    index++;
                    start = timeseries.get(i).getArrivalTime();
                    if (type != ACTUAL) {
                        ts[index] += timeseries.get(i).getDataLength();
                    }
                }
            }

            if (type == AVG && count > 0 && ts[index] > 0) {
                ts[index] = ts[index] / (double) count;
            }

        }


        return new TimeSeries(ts, (double) classLabel);
    }

    public TimeSeries getIATTS(int type) {


        double iat[] = new double[tsLength];

        //double arrivalTimes[] = new double[timeseries.size()];

        Collections.sort(timeseries, new Comparator<Data>() {
            @Override
            public int compare(Data o1, Data o2) {
                return ((Long)o1.arrivalTime).compareTo(o2.arrivalTime);
            }
        });
        int startIndex = 0;
        //while (timeseries.get(startIndex).getIsSendOrRecv() != IOTParser.SEND || timeseries.get(i).getProtocol() != IOTParser.TCP) { startIndex++; }
        long start = timeseries.get(startIndex).getArrivalTime();
        int index = 0;
        long prev = timeseries.get(startIndex).getArrivalTime();
        int count = 0;
        startIndex++;
        for (int i = startIndex; i < timeseries.size(); i++) {
            if (timeseries.get(i).getIsSendOrRecv() == IOTParser.SEND && timeseries.get(i).getProtocol() == IOTParser.TCP) {

                if (timeseries.get(i).getArrivalTime() - start <= TS_INTERVAL) {
                    //iat[index] += (((double)(timeseries.get(i).getArrivalTime() - start)) / 1000000.0) - (((double)(prev - start)) / 1000000.0);

                    if (type == ACTUAL) {
                        iat[ (int)((timeseries.get(i).getArrivalTime() - start) / 1000000)] += (((double)(timeseries.get(i).getArrivalTime() - start))) - (((double)(prev - start)));
                        count++;
                        //System.err.println("count = " + count + " Start = " + (((double)(timeseries.get(i).getArrivalTime() - start)) / 1000000.0) + " prev-start = " + (((double)(prev - start)) / 1000000.0) + " total = " + iat[index]);

                        prev = timeseries.get(i).getArrivalTime();
                    } else {
                        iat[index] += (((double) (timeseries.get(i).getArrivalTime() - start))) - (((double) (prev - start)));
                        count++;
                        //System.err.println("count = " + count + " Start = " + (((double)(timeseries.get(i).getArrivalTime() - start)) / 1000000.0) + " prev-start = " + (((double)(prev - start)) / 1000000.0) + " total = " + iat[index]);

                        prev = timeseries.get(i).getArrivalTime();
                    }
                    if (timeseries.get(i).getArrivalTime() - start < 0)
                        System.err.println("diff = " + (timeseries.get(i).getArrivalTime() - start) + "Arrival time = " + timeseries.get(i).getArrivalTime() + " start time = " + start);
                } else {
                    if (count > 0 && iat[index] > 0 && type == AVG) {
                        iat[index] = iat[index] / (double) count;
                    }
                    count = 0;
                    index++;
                    start = timeseries.get(i).getArrivalTime();
                    prev = start;
                }
            }
        }

        return new TimeSeries(iat , (double) classLabel);
    }

    public IOTDevice getDevice() {
        return new IOTDevice(IP, type, label);
    }
}
