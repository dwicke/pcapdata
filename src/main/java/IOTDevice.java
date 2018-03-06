import java.util.*;

public class IOTDevice {

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
        String[] splitType = type.split("-");
        String deviceType = "";
        if (splitType.length == 4 || splitType.length == 5) {
            deviceType = splitType[2];
        } else if (splitType.length == 3) {
            deviceType = splitType[1];
        } else if (splitType.length == 2) {
            deviceType = splitType[0];
        }
        labels.putIfAbsent(deviceType, numLabels);
        numLabels += 1;
        System.out.println("numLabels= " + numLabels);
        classLabel = labels.get(deviceType);
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

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(3600);
        sb.append(classLabel);
        sb.append(" ");
        sb.append(type);
        sb.append(" ");
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
        System.out.println(sb.toString());
        return sb.toString();
        //return "IP = " + IP + " Device name = " + type + " Number of packets sent = " + timeseries.size() + " total size of data sent = " + timeseries.stream().mapToLong(Data::getDataLength).sum();
    }
}
