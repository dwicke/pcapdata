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
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;


public class IOTParser {

    Map<String, IOTDevice> lookupTable;

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
                    System.out.println(count);
                    if (buffer != null) {
                        System.out.println("payload length: " + buffer.getRawArray().length);
                        size = buffer.getRawArray().length;
                    }

                    lookupTable.get(tcpPacket.getSourceIP()).addData(new Data().setSource(tcpPacket.getSourceIP()).setDest(tcpPacket.getDestinationIP()).setDataLength(size).setArrivalTime(tcpPacket.getArrivalTime()));
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
        parser.processInputFile("/home/drew/devices.txt");
        parser.writeCSVIOTData("/home/drew/mypcaps/mypcapgz.pcap.gz","/home/drew/tcpdata.txt");

    }



    public void processInputFile(String inputFilePath) {



        List<IOTDevice> inputList = new ArrayList<>();
        try{
            File inputF = new File(inputFilePath);
            InputStream inputFS = new FileInputStream(inputF);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputFS));
            // skip the header of the csv
            inputList = br.lines().skip(1).map((line)-> new IOTDevice(line.split(",")[0], line.split(",")[1])).collect(Collectors.toList());
            br.close();
        } catch (IOException e) {
        }

        lookupTable = inputList.stream().collect(Collectors.toMap(IOTDevice::getIP, p -> p));

    }

}