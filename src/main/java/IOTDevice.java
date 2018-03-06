import java.util.ArrayList;
import java.util.List;

public class IOTDevice {

    String IP;
    String type;
    List<Data> timeseries;

    public IOTDevice(String IP, String type) {
        this.IP = IP;
        this.type = type;
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
}
