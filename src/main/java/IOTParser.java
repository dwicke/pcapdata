import io.pkts.PacketHandler;
import io.pkts.Pcap;
import io.pkts.buffer.Buffer;
import io.pkts.packet.Packet;
import io.pkts.packet.TCPPacket;
import io.pkts.packet.UDPPacket;
import io.pkts.protocol.Protocol;
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
    Map<Integer, Set<String>> numUses;

    public IOTParser() {
        lookupTable = new HashMap<>();
        dataset = new HashMap<>();
        numUses = new HashMap<>();
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
                        if (size > 0 && lookupTable.containsKey(tcpPacket.getSourceIP()) && !tcpPacket.getSourceIP().contains(".3.") && !tcpPacket.getSourceIP().contains(".1.")) {
                            //System.err.println("time = " + (tcpPacket.getArrivalTime() - timeZero) / 1000000);


                            if (dataset.containsKey(inputfilename + tcpPacket.getSourceIP())) {
                                dataset.get(inputfilename + tcpPacket.getSourceIP()).addData(new Data().setSource(tcpPacket.getSourceIP()).setDest(tcpPacket.getDestinationIP()).setDataLength(size).setArrivalTime((tcpPacket.getArrivalTime() - timeZero) / 1000000));
                            } else {
                                dataset.put(inputfilename + tcpPacket.getSourceIP(), lookupTable.get(tcpPacket.getSourceIP()).getDevice());
                            }
                            if (numUses.containsKey(dataset.get(inputfilename + tcpPacket.getSourceIP()).getClassLabel())) {

                                Set<String> hourSet = numUses.get(dataset.get(inputfilename + tcpPacket.getSourceIP()).getClassLabel());
                                hourSet.add(inputfilename + tcpPacket.getSourceIP());
                            }else {
                                Set<String> hourSet = new HashSet<>();
                                hourSet.add(inputfilename + tcpPacket.getSourceIP());
                                numUses.put(lookupTable.get(tcpPacket.getSourceIP()).getDevice().getClassLabel(), hourSet);
                            }
                        }
                    }
//                } else if (packet.hasProtocol(Protocol.UDP)) {
//
//                    UDPPacket udpPacket = (UDPPacket) packet.getPacket(Protocol.UDP);
//                    Buffer buffer = udpPacket.getPayload();
//                    if (buffer != null) {
//                        System.out.println("UDP: " + buffer);
//                    }
//                }
                    return true;
                }
            });
        } catch (IOException e) {

        }

    }


    public ArrayList<MultiVariateTimeSeries> getTimeSeries(String inputFile) {
        return null;
    }

    public static void main(String[] args) throws IOException {

        IOTParser parser = new IOTParser();
        parser.processInputFile("/home/dwicke/IOTData/dataset_refs_bro/IOTOnly_simple.csv");
        System.err.println("Loaded labels");

        Files.find(Paths.get("/home/dwicke/IOTData/2017/01/01/"),
                Integer.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().contains(".gz"))
                .parallel()
                .forEach(path -> parser.writeCSVIOTData(path.toString(), true));

        System.err.println("Finished Loading data going to write");
        Path path = Paths.get("/home/dwicke/tcpdataFourLabels.txt");


        //Use try-with-resource to get auto-closeable writer instance
//        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE))
//        {
//            boolean isDone = false;
//            writer.write("#\n");
//
//            boolean isFirstLine = true;
//            int totalDeviceCount = 0;
//                int numDone = 0;
//                int numDevices = 0;
//                for (IOTDevice iotd : parser.dataset.values()) {
//                    if (parser.numUses.get(iotd.getClassLabel()).size() > 3) {
//                        System.err.println(numDone + " / " + parser.dataset.values().size());
//                        String data = iotd.toString();
//                        if (!data.isEmpty()) {
//                            writer.write(data);
//                            writer.write("\n");
//                        }
//
//                    }
//                    numDone++;
//                }
//
//
//        }

        ArrayList<MultiVariateTimeSeries> mts = new ArrayList<>();
        for (IOTDevice iotd : parser.dataset.values()) {
            mts.add(iotd.getMTS());
        }
        System.err.println("finished creating MTSs");
        getDerivatives(mts);
        System.err.println("finished making dervitives");
        MultiVariateTimeSeries mtsa[] = mts.toArray(new MultiVariateTimeSeries[]{});

        MUSEClassifier muse = new MUSEClassifier();
        MUSEClassifier.BIGRAMS = true;
        MUSEClassifier.DEBUG = true;
        System.err.println("Going to eval using muse");
        MUSEClassifier.Score museScore = muse.eval(mtsa, mtsa);
        System.out.println(museScore.toString());

        //System.out.println("Number of devices operating = " + parser.dataset.values().size());
        //parser.lookupTable.keySet().stream().forEach(System.out::println);
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