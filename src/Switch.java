import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Switch {
    private String id;
    private int port;
    private Map<String, String> neighbors = new HashMap<>();
    private Map<String, String> forwardingTable = new ConcurrentHashMap<>();
    private DatagramSocket socket;

    public Switch(String id, String configPath) {
        this.id = id;
        setupConfig(configPath);
    }

    private void setupConfig(String configPath) {
        String filePath = "src/" + configPath;
        Properties properties = new ConfigLoader().loadProperties(filePath);
        this.port = Integer.parseInt(properties.getProperty("device." + id + ".port"));
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        String connectedTo = properties.getProperty("device." + id + ".connectedTo");
        for (String neighbor : connectedTo.split(",")) {
            String neighborIp = properties.getProperty("device." + neighbor + ".ip");
            String neighborPort = properties.getProperty("device." + neighbor + ".port");
            neighbors.put(neighbor, neighborIp + ":" + neighborPort);
        }
    }

    public void start() {
        System.out.println("Switch " + id + " listening on port " + port);
        try {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (true) {
                socket.receive(packet);
                String frame = new String(packet.getData(), 0, packet.getLength());
                handleFrame(frame, packet.getAddress(), packet.getPort());

                printForwardingTable();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleFrame(String frame, InetAddress senderAddress, int senderPort) {
        System.out.println("Switch " + id + " received frame: " + frame);

        String[] parts = frame.split(",", 3);
        String sourceMac = parts[0];
        String destMac = parts[1];

        String sender = senderAddress.getHostAddress() + ":" + senderPort;
        forwardingTable.put(sourceMac, sender);

        if (forwardingTable.containsKey(destMac)) {
            String forwardTo = forwardingTable.get(destMac);
            sendFrame(frame, forwardTo);
        } else {
            floodFrame(frame, sender);
        }
    }

    private void sendFrame(String frame, String neighbor) {
        try {
            String[] parts = neighbor.split(":");
            InetAddress ip = InetAddress.getByName(parts[0]);
            int port = Integer.parseInt(parts[1]);

            byte[] data = frame.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, ip, port);
            socket.send(packet);
            System.out.println("Switch " + id + " forwarded frame to " + neighbor);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void floodFrame(String frame, String sender) {
        neighbors.values().forEach(neighbor -> {
            if (!neighbor.equals(sender)) {
                sendFrame(frame, neighbor);
            }
        });
    }

    private void printForwardingTable() {
        System.out.println("Forwarding Table for Switch " + id + ":");
        forwardingTable.forEach((key, value) -> System.out.println("  " + key + " -> " + value));
        System.out.println("-----------------------------");
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java Switch <SwitchID> <ConfigFilePath>");
            System.exit(1);
        }
        Switch sw = new Switch(args[0], args[1]);
        sw.start();
    }
}
