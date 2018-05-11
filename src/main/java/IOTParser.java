import com.google.gson.Gson;
import io.pkts.PacketHandler;
import io.pkts.Pcap;
import io.pkts.buffer.Buffer;
import io.pkts.packet.Packet;
import io.pkts.packet.TCPPacket;
import io.pkts.packet.UDPPacket;
import io.pkts.protocol.Protocol;
import sfa.classification.Classifier;
import sfa.classification.MUSEClassifier;
import sfa.timeseries.MultiVariateTimeSeries;
import sfa.timeseries.TimeSeries;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;


public class IOTParser {

    Map<String, IOTDevice> lookupTable;
    Map<String, IOTDevice> dataset;
    public static int SEND = 1;
    public static int RECV = 2;

    public static int TCP = 1;
    public static int UDP = 2;


    public IOTParser() {
        lookupTable = new HashMap<>();
        dataset = new HashMap<>();
    }


    public void writeCSVIOTData(String inputfilename, boolean isGZ){
        System.err.println("Loading file " + inputfilename);
        try {
        InputStream fileStream = new FileInputStream(inputfilename);
        if (isGZ) {
            fileStream = new GZIPInputStream(fileStream);
        }
        final Pcap pcap = Pcap.openStream(fileStream);
        final ArrayList<Data> tcpData = new ArrayList<Data>();

            pcap.loop(new PacketHandler() {

                double count = 0;
                long timeZero = -1;
                public boolean nextPacket(Packet packet) throws IOException {
                    if (timeZero == -1) {
                        timeZero = packet.getArrivalTime();
                    }

                    if (packet.hasProtocol(Protocol.TCP)) {

                        TCPPacket tcpPacket = (TCPPacket) packet.getPacket(Protocol.TCP);
                        Buffer buffer = tcpPacket.getPayload();
                        count++;
                        int size = 0;
                        //System.out.println(count);
                        if (buffer != null) {
                            //System.out.println("payload length: " + buffer.getRawArray().length);
                            size = buffer.getRawArray().length;
                        }
                        //(tcpPacket.getArrivalTime() - timeZero) / 1000000
                        // String key, String lookupTableKey, String sourceIP, String destIP, int size, int protocol, int arrivalTime
                        updateDataset(tcpPacket.getSourceIP(), tcpPacket.getSourceIP(), tcpPacket.getSourceIP(), tcpPacket.getDestinationIP(), size, TCP, tcpPacket.getArrivalTime(), SEND);
                        updateDataset(tcpPacket.getDestinationIP(), tcpPacket.getDestinationIP(), tcpPacket.getSourceIP(), tcpPacket.getDestinationIP(), size, TCP,  tcpPacket.getArrivalTime(), RECV);

                    } else if (packet.hasProtocol(Protocol.UDP)) {

                        UDPPacket udpPacket = (UDPPacket) packet.getPacket(Protocol.UDP);
                        Buffer buffer = udpPacket.getPayload();
                        int size = 0;
                        if (buffer != null) {
                            //System.out.println("payload length: " + buffer.getRawArray().length);
                            size = buffer.getRawArray().length;
                        }
                        // going to have both sending and receiving data.  The data that the IoT device receives
                        // is included along with the sending.
                        //(udpPacket.getArrivalTime() - timeZero) / 1000000
                        updateDataset(udpPacket.getSourceIP(), udpPacket.getSourceIP(), udpPacket.getSourceIP(), udpPacket.getDestinationIP(), size, UDP, udpPacket.getArrivalTime(), SEND);
                        updateDataset(udpPacket.getDestinationIP(), udpPacket.getDestinationIP(), udpPacket.getSourceIP(), udpPacket.getDestinationIP(), size, UDP, udpPacket.getArrivalTime(), RECV);

//



                    }
                    return true;
                }
            });
        } catch (IOException e) {

        }

    }


    public void  updateDataset(String key, String lookupTableKey, String sourceIP, String destIP, int size, int protocol, long arrivalTime, int sendOrReceive) {
        if (size > 0 && lookupTable.containsKey(lookupTableKey) && lookupTableKey.contains("172.16.2.") || lookupTableKey.contains("172.16.10.")) {
            //System.err.println("time = " + (tcpPacket.getArrivalTime() - timeZero) / 1000000);
            // check if i already have looked at data with this ip in this file
            if (dataset.containsKey(key)) {
                dataset.get(key).addData(new Data().setSendorRecv(sendOrReceive).setProtocol(protocol).setSource(sourceIP).setDest(destIP).setDataLength(size).setArrivalTime(arrivalTime));
            } else {
                dataset.put(key, lookupTable.get(lookupTableKey).getDevice());
                dataset.get(key).addData(new Data().setSendorRecv(sendOrReceive).setProtocol(protocol).setSource(sourceIP).setDest(destIP).setDataLength(size).setArrivalTime(arrivalTime));
            }

        }
    }


    public ArrayList<MultiVariateTimeSeries> getTimeSeries(String inputFile) throws IOException{
        processInputFile("/home/dwicke/IOTData/dataset_refs_bro/IOTOnly_simple.csv");
        System.err.println("Loaded labels");

        Files.find(Paths.get(inputFile),
                Integer.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().contains(".gz"))
                .parallel()
                .forEach(path -> writeCSVIOTData(path.toString(), true));

        System.err.println("Finished Loading data going to write");
        ArrayList<MultiVariateTimeSeries> mts = new ArrayList<>();
        Map<Integer, Integer> labels = new HashMap<>();
        for (IOTDevice iotd : dataset.values()) {
            if (iotd.timeseries.size() > 0) {
                MultiVariateTimeSeries mtsa = iotd.getMTS();
                if (mtsa.timeSeries[0].getLength() > 0 && mtsa.timeSeries[1].getLength() > 0) {
                    mts.add(iotd.getMTS());
                    int lab = iotd.getClassLabel();
                    if (labels.containsKey(lab)) {
                        labels.put(lab, labels.get(lab) + 1);
                    } else {
                        labels.put(lab, 1);
                    }
                }
            }

        }

        System.err.println(labels.size());
        for (Map.Entry<Integer, Integer> en : labels.entrySet()) {
            System.err.println("Class " + en.getKey() + " has " + en.getValue());
        }
        return mts;
    }

    public static void main(String[] args) throws IOException {



        IOTParser parserTrain = new IOTParser();
        ArrayList<MultiVariateTimeSeries> mts = parserTrain.getTimeSeries("/home/dwicke/IOTData/2017/01/");
        writeForTSAT(mts, "IoTDataTrainUDP.json");

        IOTParser parserTest = new IOTParser();
        ArrayList<MultiVariateTimeSeries> mtsTest = parserTest.getTimeSeries("/home/dwicke/IOTData/2017/02/");
        writeForTSAT(mtsTest, "IoTDataTestUDP.json");

//        doMUSE(mts, mtsTest);

    }


    public static void doMUSE(ArrayList<MultiVariateTimeSeries> mts, ArrayList<MultiVariateTimeSeries> mtsTest) throws FileNotFoundException {

//        MUSEClassifier muse = MUSEClassifier.load(new File("/home/dwicke/IOTData/museModel0101"));

        MUSEClassifier muse = new MUSEClassifier();
        getDerivatives(mts);
        System.err.println("finished making dervitives");
        MultiVariateTimeSeries mtsa[] = mts.toArray(new MultiVariateTimeSeries[]{});

        getDerivatives(mtsTest);
        System.err.println("finished making dervitives");
        MultiVariateTimeSeries mtsaTest[] = mtsTest.toArray(new MultiVariateTimeSeries[]{});

//        Classifier.Predictions p = muse.score(mtsaTest);
//        HashMap<Double, Integer> res = new HashMap<>();
//        for(int ind = 0; ind < p.labels.length; ind++) {
//            double lb = p.labels[ind];
//            if (res.containsKey(lb)) {
//
//                boolean aresame = (lb == Double.valueOf(mtsaTest[ind].getLabel()));
//                res.put(lb, res.get(lb) +(aresame ?1:0));
//            }
//        }
//        for (Map.Entry<Double, Integer> en : res.entrySet()) {
//            System.err.println("Class " + en.getKey() + " success " + en.getValue());
//        }

        MUSEClassifier.BIGRAMS = true;
        MUSEClassifier.DEBUG = true;
        System.err.println("Going to eval using muse");
        MUSEClassifier.Score museScore = muse.eval(mtsa, mtsaTest);

        System.out.println(museScore.toString());
//        muse.save(new File("/home/dwicke/IOTData/museModel0101"));
//        System.out.println("Saved model to file");
    }

    protected static ArrayList<MultiVariateTimeSeries> getDerivatives(ArrayList<MultiVariateTimeSeries> mtsSamples) {
        for (MultiVariateTimeSeries mts : mtsSamples) {
            TimeSeries[] deltas = new TimeSeries[2 * mts.timeSeries.length];
            TimeSeries[] samples = mts.timeSeries;
            for (int a = 0; a < samples.length; a++) {
                TimeSeries s = samples[a];
                double[] d = new double[s.getLength() - 1];
                for (int i = 1; i < s.getLength(); i++) {
                    d[i - 1] = s.getData()[i] - s.getData()[i - 1];
                }
                deltas[2 * a] = samples[a];
                deltas[2 * a + 1] = new TimeSeries(d, mts.getLabel());
            }
            mts.timeSeries = deltas;
        }
        return mtsSamples;
    }

    public static void writeForTSAT(ArrayList<MultiVariateTimeSeries> mts, String fileName) throws IOException {
        Gson gson = new Gson();
        try(BufferedWriter writer = Files.newBufferedWriter(Paths.get("/home/dwicke/" + fileName), StandardOpenOption.CREATE)) {
            writer.write(gson.toJson(mts));
        }
    }

    public void processInputFile(String inputFilePath) {



        List<IOTDevice> inputList = new ArrayList<>();
        try{
            File inputF = new File(inputFilePath);
            InputStream inputFS = new FileInputStream(inputF);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputFS));
            // skip the header of the csv
            inputList = br.lines().skip(1).map((line)-> new IOTDevice(line.split(",")[0], line.split(",")[2], line.split(",")[3])).collect(Collectors.toList());
            br.close();
        } catch (IOException e) {
        }

        lookupTable = inputList.stream().collect(Collectors.toMap(IOTDevice::getIP, p -> p));

    }

}