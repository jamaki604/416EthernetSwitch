import java.net.*;
import java.util.Properties;
import java.util.Scanner;

public class Host {
    private String id;
    private String macAddress;
    private String switchIp;
    private int switchPort;
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
                System.out.println("Enter Destination MAC (A, B, C, or D)");
                String destMac = console.nextLine().strip().toUpperCase();
                if (destMac.length() > 1 || !destMac.matches("[ABCD]")) {
                    System.out.println("Invalid input. Try again.");
                    continue;
                }
                System.out.println("Enter Message:");
                String message = console.nextLine();
                sendFrame(destMac, message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendFrame(String destMac, String message) throws Exception {
        String frame = macAddress + "," + destMac + "," + message;
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
                String[] parts = frame.split(",", 3);
                String sourceMac = parts[0];
                String destMac = parts[1];
                String payload = parts[2];

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
        Host host = new Host(args[0].toUpperCase());
        host.start();
    }
}
