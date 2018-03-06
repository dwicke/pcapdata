import io.pkts.PacketHandler;
import io.pkts.Pcap;
import io.pkts.buffer.Buffer;
import io.pkts.packet.Packet;
import io.pkts.packet.TCPPacket;
import io.pkts.packet.UDPPacket;
import io.pkts.protocol.Protocol;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;


public class IOTParser {

    Map<String, IOTDevice> lookupTable;
    Map<String, IOTDevice> dataset;

    public IOTParser() {
        lookupTable = new HashMap<>();
        dataset = new HashMap<>();
    }


    public void writeCSVIOTData(String inputfilename, String outputfilename) throws IOException{
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
                    if (lookupTable.containsKey(tcpPacket.getSourceIP()) && !tcpPacket.getSourceIP().contains(".3.") && !tcpPacket.getSourceIP().contains(".1.")){
                        dataset.putIfAbsent(tcpPacket.getSourceIP(), lookupTable.get(tcpPacket.getSourceIP()));
                        lookupTable.get(tcpPacket.getSourceIP()).addData(new Data().setSource(tcpPacket.getSourceIP()).setDest(tcpPacket.getDestinationIP()).setDataLength(size).setArrivalTime(tcpPacket.getArrivalTime()));
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

//        String recordAsCsv = tcpData.stream()
//                .map(Data::toCsvRow)
//                .collect(Collectors.joining(System.getProperty("line.separator")));

        //lookupTable.values().stream()


//        Path path = Paths.get(outputfilename);

        //Use try-with-resource to get auto-closeable writer instance
//        try (BufferedWriter writer = Files.newBufferedWriter(path))
//        {
//            writer.write(recordAsCsv);
//        }
    }

    public static void main(String[] args) throws IOException {

        IOTParser parser = new IOTParser();
        parser.processInputFile("/home/dwicke/IOTData/dataset_refs_bro/IOTOnly_labeled.csv");



        parser.writeCSVIOTData("/home/dwicke/IOTData/2017/01/01/truncated-iot-gateway_eth1_20170101_000102.pcap.gz","/home/dwicke/tcpdata.txt");

        //parser.dataset.values().stream().forEach(System.out::println);

        String recordAsCsv = parser.dataset.values().stream()
                .map(IOTDevice::toString)
                .collect(Collectors.joining(System.getProperty("line.separator")));

        Path path = Paths.get("/home/dwicke/tcpdata.txt");

        //Use try-with-resource to get auto-closeable writer instance
        try (BufferedWriter writer = Files.newBufferedWriter(path))
        {
            writer.write(recordAsCsv);

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