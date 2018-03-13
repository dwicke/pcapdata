import io.pkts.PacketHandler;
import io.pkts.Pcap;
import io.pkts.buffer.Buffer;
import io.pkts.packet.Packet;
import io.pkts.packet.TCPPacket;
import io.pkts.packet.UDPPacket;
import io.pkts.protocol.Protocol;

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


    public void writeCSVIOTData(String inputfilename){
        try {
        InputStream fileStream = new FileInputStream(inputfilename);
        InputStream gin = new GZIPInputStream(fileStream);
        final Pcap pcap = Pcap.openStream(gin);
        final ArrayList<Data> tcpData = new ArrayList<Data>();

            pcap.loop(new PacketHandler() {

                double count = 0;

                public boolean nextPacket(Packet packet) throws IOException {

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
                        if (lookupTable.containsKey(tcpPacket.getSourceIP()) && !tcpPacket.getSourceIP().contains(".3.") && !tcpPacket.getSourceIP().contains(".1.")) {
                            if (dataset.containsKey(inputfilename + tcpPacket.getSourceIP())) {
                                dataset.get(inputfilename + tcpPacket.getSourceIP()).addData(new Data().setSource(tcpPacket.getSourceIP()).setDest(tcpPacket.getDestinationIP()).setDataLength(size).setArrivalTime(tcpPacket.getArrivalTime()));
                            } else {
                                dataset.put(inputfilename + tcpPacket.getSourceIP(), lookupTable.get(tcpPacket.getSourceIP()).getDevice());
                            }
                            if (numUses.containsKey(dataset.get(inputfilename + tcpPacket.getSourceIP()).getClassLabel())) {

                                Set<String> hourSet = numUses.get(tcpPacket.getSourceIP());
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




    public static void main(String[] args) throws IOException {

        IOTParser parser = new IOTParser();
        parser.processInputFile("/home/dwicke/IOTData/dataset_refs_bro/IOTOnly_labeled.csv");

        Files.find(Paths.get("/home/dwicke/IOTData/2017/01/01"),
                Integer.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().contains(".gz"))
                .forEach(path -> parser.writeCSVIOTData(path.toString()));


        Path path = Paths.get("/home/dwicke/tcpdata.txt");

        //Use try-with-resource to get auto-closeable writer instance
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE))
        {
            boolean isDone = false;
            writer.write("# ");
            boolean isFirstLine = true;
            while (!isDone) {
                int numDone = 0;
                int numDevices = 0;
                for (IOTDevice iotd : parser.dataset.values()) {
                    if (parser.numUses.get(iotd.getClassLabel()).size() > 1) {
                        numDevices++;
                        if (!iotd.isEmpty()) {
                            if (iotd.hasNextValue()) {
                                writer.write(iotd.nextValue() + " ");
                            } else if (!isFirstLine) {
                                writer.write("0 ");
                                numDone++;
                            }
                        }
                    }
                }
                isFirstLine = false;
                writer.write("\n");
                isDone = numDone == numDevices;
            }
            //writer.write(recordAsCsv);

        }
        //System.out.println("Number of devices operating = " + parser.dataset.values().size());
        //parser.lookupTable.keySet().stream().forEach(System.out::println);
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