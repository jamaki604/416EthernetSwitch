import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Router {
    String id;
    int port;
    private Map<String, String> virtualPorts = new HashMap<>();

    private Map<String, String> routingTable = new HashMap<>();
    private DatagramSocket socket;
    private Properties properties;

    public Router(String id){
        this.id = id;
        String configPath = "config.properties";
        setupConfig(configPath);
    }

    private void setupConfig(String configPath) {
        String filePath = "src/" + configPath;
        properties = new ConfigLoader().loadProperties(filePath);
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
            virtualPorts.put(neighbor, neighborIp + ":" + neighborPort);
        }
        String routing = properties.getProperty("device." + id + ".routing");
        for(String route : routing.split(",")){
            String[] parts = route.split(":");
            routingTable.put(parts[0], parts[1]);
        }

        // Table Prints for testing
        System.out.println("Routing Table for Router " + id + ":");
        routingTable.forEach((key, value) -> System.out.println("  " + key + " -> " + value));
        System.out.println("--------------------------------------------");


        System.out.println("Virtual Ports Table for Router " + id + ":");
        virtualPorts.forEach((key, value) -> System.out.println("  " + key + " -> " + value));
        System.out.println("--------------------------------------------");
    }

    public void start() {
        System.out.println("Switch " + id + " listening on port " + port);
        try {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (true) {
                socket.receive(packet);
                String frame = new String(packet.getData(), 0, packet.getLength());
                handleFrame(frame);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleFrame(String frame) {
        System.out.println("Router " + id + " received frame: " + frame);

        String[] parts = frame.split(",", 5);

        String srcMac = parts[0];
        String destMac = parts[1];
        String srcIp = parts[2];
        String destIp = parts[3];
        String payload = parts[4];

        //don't do anything if everything is in the same subnet (switches forwarding table will get messed up)
        if (srcIp.split("\\.")[0].equals(destIp.split("\\.")[0])){
            System.out.println("Same Subnet.... I don't care");
            return;
        }

        String destNetwork = destIp.split("\\.")[0];

        String nextHop = routingTable.get(destNetwork);

        if (nextHop.equals("direct")) {
            System.out.println("Destination is directly connected: " + destIp);
            String destSwitch = properties.getProperty("device." + destIp.split("\\.")[1] + ".connectedTo");
            sendFrame(srcMac, destIp.split("\\.")[1], srcIp, destIp, payload, destSwitch);
        } else {
            System.out.println("Forwarding to Next Router: " + nextHop);
            sendFrame(srcMac, nextHop, srcIp, destIp, payload, nextHop);
        }
    }
    private void sendFrame(String srcMac, String destMac, String srcIp, String destIp, String payload, String nextHop) {
        try {
            String frame = srcMac + "," + destMac + "," + srcIp + "," + destIp + "," + payload;

            String nextHopAddress = virtualPorts.get(nextHop);

            String[] parts = nextHopAddress.split(":");
            InetAddress ip = InetAddress.getByName(parts[0]);
            int port = Integer.parseInt(parts[1]);

            byte[] data = frame.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, ip, port);
            socket.send(packet);

            System.out.println("Router " + id + " forwarded frame to " + nextHop + " (" + nextHopAddress + ")");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Host <HostID>");
            System.exit(1);
        }
        Router router = new Router(args[0]);
        router.start();
    }
}

