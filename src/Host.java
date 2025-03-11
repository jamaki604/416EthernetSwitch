import java.net.*;
import java.util.Properties;
import java.util.Scanner;

public class Host {
    private String id;
    private String macAddress;
    private String switchIp;
    private String virtualIP;
    private int switchPort;
    private String gateway;
    private DatagramSocket socket;
    public Scanner console = new Scanner(System.in);

    public Host(String id) {
        this.id = id;
        this.macAddress = id;
        String configPath = "config.properties";
        setupConfig(configPath);
    }

    private void setupConfig(String configPath) {
        String filePath = "src/" + configPath;
        Properties properties = new ConfigLoader().loadProperties(filePath);
        String connectedSwitch = properties.getProperty("device." + id + ".connectedTo");
        this.switchIp = properties.getProperty("device." + connectedSwitch + ".ip");
        this.switchPort = Integer.parseInt(properties.getProperty("device." + connectedSwitch + ".port"));
        this.virtualIP = properties.getProperty("device." + id + ".subnet") + "." + id;
        this.gateway = properties.getProperty("device." + id + ".gateway");

        try {
            socket = new DatagramSocket(Integer.parseInt(properties.getProperty("device." + id + ".port")));
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        new Thread(this::listenForMessages).start();
        try {
            while (true) {
                System.out.println("Enter Destination IP (format: [subnet].[HostID])");
                String destIP = console.nextLine().strip();
                System.out.println("Enter Message:");
                String message = console.nextLine();
                sendFrame(destIP, message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFrame(String destIp, String message) throws Exception {
        String destMac;
        if(destIp.split("\\.")[0].equals(virtualIP.split("\\.")[0])){
            destMac = destIp.split("\\.")[1];
        }else {
            destMac = gateway.split("\\.")[1];
        }
        String frame = macAddress + "," + destMac + "," + virtualIP + "," + destIp + "," + message;
        byte[] data = frame.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(switchIp), switchPort);
        socket.send(packet);
        System.out.println("Frame sent");
    }

    private void listenForMessages() {
        try {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            System.out.println("Host " + id + " listening for messages...");
            while (true) {
                socket.receive(packet);
                String frame = new String(packet.getData(), 0, packet.getLength());
                String[] parts = frame.split(",", 5);
                String sourceMac = parts[0];
                String destMac = parts[1];
                String sourceIP = parts[2];
                String destIP = parts[3];
                String payload = parts[4];

                if (destMac.equals(macAddress)) {
                    System.out.println("Message received from " + sourceMac + ": " + payload);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Host <HostID>");
            System.exit(1);
        }
        Host host = new Host(args[0].toUpperCase() );
        host.start();
    }
}
