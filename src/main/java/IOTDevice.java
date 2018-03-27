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

    public MultiVariateTimeSeries getMTS() {
        TimeSeries tss[] = new TimeSeries[2];
        tss[0] = getDataTimeSeries();
        tss[1] = getIATTS();
        return new MultiVariateTimeSeries(tss, (double) classLabel);
    }

    public TimeSeries getDataTimeSeries() {
        double ts[] = new double[3601];

        timeseries.stream().forEach(dp -> ts[(int) dp.arrivalTime] += dp.dataLength);
        return new TimeSeries(ts, (double) classLabel);
    }

    public TimeSeries getIATTS() {
        ArrayList<Double> iat = new ArrayList<>();
        timeseries.stream().forEach(dp -> iat.add( dp.arrivalTime - (iat.size() == 0 ? 0 : iat.get(iat.size() - 1))));
        double ts[] = new double[iat.size()];
        for (int i = 0; i < iat.size(); i++) {
            ts[i] = iat.get(i);
        }

        return new TimeSeries(ts , (double) classLabel);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(7200);
//        sb.append(classLabel);
//        sb.append(" ");
//        sb.append(type);
//        sb.append(" ");

        double ts[] = new double[3601];
        ArrayList<Long> iat = new ArrayList<>();
        timeseries.stream().forEach(dp -> ts[(int) dp.arrivalTime] += dp.dataLength);
        timeseries.stream().forEach(dp -> iat.add( dp.arrivalTime - (iat.size() == 0 ? 0 : iat.get(iat.size() - 1))));

//        int count = 0;
//        for (int i =0; i < ts.length; i++){
//            if (ts[i] > 0) {
//                count++;
//            }
//        }
//        // so heavy traffic is 1 and light traffic is type 2
//        if (count > ts.length / 4) {
//            sb.append("1 ");
//        } else {
//            sb.append("2 ");
//        }
//        Arrays.stream(ts).forEach(s -> sb.append(s + " "));


        //System.out.println(sb.toString());
        return sb.toString();
        //return "IP = " + IP + " Device name = " + type + " Number of packets sent = " + timeseries.size() + " total size of data sent = " + timeseries.stream().mapToLong(Data::getDataLength).sum();
    }

    public String OLDtoString() {
        StringBuilder sb = new StringBuilder(3600);
        sb.append(classLabel);
        sb.append(" ");
//        sb.append(type);
//        sb.append(" ");
        long lastTime = -1;
        for (Data dp : timeseries) {
            if(lastTime == -1 && dp.dataLength > 0) {
                lastTime = dp.getArrivalTime();
                sb.append(dp.dataLength);
                sb.append(" ");
            } else if (lastTime != -1) {
                int secondsElapsed = (int) ((dp.arrivalTime - lastTime) / 1000000000);
                sb.append(String.join("", Collections.nCopies(secondsElapsed, "0 ")));
                sb.append(dp.dataLength);
                sb.append(" ");
            }
        }
        //System.out.println(sb.toString());
        return sb.toString();
        //return "IP = " + IP + " Device name = " + type + " Number of packets sent = " + timeseries.size() + " total size of data sent = " + timeseries.stream().mapToLong(Data::getDataLength).sum();
    }


    String tsEntry[];
    int currentIndex = -1;
    public void reset() {
        tsEntry = toString().split(" ");
        currentIndex = 0;
    }

    public String nextValue() {
        if (currentIndex == -1) {
            reset();
        }
        return tsEntry[currentIndex++];
    }

    public boolean hasNextValue() {
        if (currentIndex == -1) {
            reset();
        }
        return tsEntry.length > 1 && currentIndex < tsEntry.length;
    }

    public boolean isEmpty() {
        return timeseries.isEmpty();
    }

    public IOTDevice getDevice() {
        return new IOTDevice(IP, type, label);
    }
}
