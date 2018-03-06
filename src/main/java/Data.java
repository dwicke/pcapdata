import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Data {

    String source;
    String dest;
    int dataLength;
    long arrivalTime;

    public String getSource() {
        return source;
    }

    public Data setSource(String source) {
        this.source = source;
        return this;
    }

    public String getDest() {
        return dest;
    }

    public Data setDest(String dest) {
        this.dest = dest;
        return this;
    }

    public int getDataLength() {
        return dataLength;
    }

    public Data setDataLength(int dataLength) {
        this.dataLength = dataLength;
        return this;
    }

    public long getArrivalTime() {
        return arrivalTime;
    }

    public Data setArrivalTime(long arrivalTime) {
        this.arrivalTime = arrivalTime;
        return this;
    }

    public String toCsvRow() {
        return Stream.of(source, dest, String.valueOf(dataLength), String.valueOf(arrivalTime))
                .collect(Collectors.joining(","));
    }

}