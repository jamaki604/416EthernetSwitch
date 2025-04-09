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
    private final Map<String, String> virtualPorts = new HashMap<>();
    private final DistanceVector distanceVector; // Replaced routingTable with DistanceVector
    private DatagramSocket socket;
    private Properties properties;

    public Router(String id) {
        this.id = id;
        this.distanceVector = new DistanceVector(); // Initialize DistanceVector
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

        // Initialize Distance Vector with directly connected subnets
        Map<String, String> directlyConnectedSubnets = new HashMap<>();
        String routing = properties.getProperty("device." + id + ".routing");
        for (String route : routing.split(",")) {
            String[] parts = route.split(":");
            String subnet = parts[0];
            String nextHop = parts[1];
            if (nextHop.equals("direct")) {
                // For directly connected subnets, the next-hop is the neighbor device
                for (String neighbor : connectedTo.split(",")) {
                    String neighborSubnet = properties.getProperty("device." + neighbor + ".subnet");
                    if (neighborSubnet != null && neighborSubnet.equals(subnet)) {
                        directlyConnectedSubnets.put(subnet, neighbor);
                        break;
                    }
                }
            } else {
                directlyConnectedSubnets.put(subnet, nextHop);
            }
        }
        // Add subnets of directly connected switches that aren't in routing
        for (String neighbor : connectedTo.split(",")) {
            String neighborSubnet = properties.getProperty("device." + neighbor + ".subnet");
            if (neighborSubnet != null && !directlyConnectedSubnets.containsKey(neighborSubnet)) {
                directlyConnectedSubnets.put(neighborSubnet, neighbor);
            }
        }
        distanceVector.initialize(directlyConnectedSubnets);

        // Table Prints for testing (kept as-is for virtualPorts, added for DistanceVector)
        System.out.println("Virtual Ports Table for Router " + id + ":");
        virtualPorts.forEach((key, value) -> System.out.println("  " + key + " -> " + value));
        System.out.println("--------------------------------------------");

        // Print initial Distance Vector
        distanceVector.printDistanceVector(id);

        // Send initial Distance Vector to all neighbors
        sendDistanceVectorToNeighbors();
    }

    public void start() {
        System.out.println("Router " + id + " listening on port " + port); // Fixed typo: "Switch" to "Router"
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

        // Split the frame to check the flag
        String[] parts = frame.split(",", 2);
        String flag = parts[0];

        if (flag.equals("0")) {
            // This is a Distance Vector update
            handleDistanceVectorUpdate(parts[1]);
        } else if (flag.equals("1")) {
            // This is a user packet
            String[] userParts = parts[1].split(",", 5);
            String srcMac = userParts[0];
            String srcIp = userParts[2];
            String destIp = userParts[3];
            String payload = userParts[4];

            // Don't do anything if everything is in the same subnet (switches forwarding table will get messed up)
            if (srcIp.split("\\.")[0].equals(destIp.split("\\.")[0])) {
                System.out.println("Same Subnet.... I don't care");
                return;
            }

            String destNetwork = destIp.split("\\.")[0];
            String nextHop = distanceVector.getNextHop(destNetwork);

            if (nextHop == null) {
                System.out.println("Router " + id + ": No route to destination subnet " + destNetwork);
                return;
            }

            if (properties.getProperty("device." + nextHop + ".subnet") != null &&
                    properties.getProperty("device." + nextHop + ".subnet").equals(destNetwork)) {
                System.out.println("Destination is directly connected: " + destIp);
                String destSwitch = properties.getProperty("device." + destIp.split("\\.")[1] + ".connectedTo");
                sendFrame("1", srcMac, destIp.split("\\.")[1], srcIp, destIp, payload, destSwitch);
            } else {
                System.out.println("Forwarding to Next Router: " + nextHop);
                sendFrame("1", srcMac, nextHop, srcIp, destIp, payload, nextHop);
            }
        } else {
            System.out.println("Router " + id + ": Unknown packet type with flag " + flag);
        }
    }

    private void handleDistanceVectorUpdate(String dvData) {
        // Parse the sender and the DV
        String[] dvParts = dvData.split(",", 2);
        String sender = dvParts[0];
        String dvString = dvParts.length > 1 ? dvParts[1] : "";
        DistanceVector receivedDV = DistanceVector.deserialize(dvString);

        System.out.println("Router " + id + " received DV from " + sender);

        // Update the Distance Vector using Bellman-Ford
        boolean updated = distanceVector.updateWithBellmanFord(sender, receivedDV);
        if (updated) {
            System.out.println("Router " + id + " updated its Distance Vector");
            distanceVector.printDistanceVector(id);
            sendDistanceVectorToNeighbors();
        }
    }

    private void sendFrame(String flag, String srcMac, String destMac, String srcIp, String destIp, String payload, String nextHop) {
        try {
            String frame = flag + "," + srcMac + "," + destMac + "," + srcIp + "," + destIp + "," + payload;

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

    private void sendDistanceVectorToNeighbors() {
        String dvString = distanceVector.serialize();
        for (String neighbor : virtualPorts.keySet()) {
            sendFrame("0", id, neighbor, "", "", dvString, neighbor);
        }
        System.out.println("Router " + id + " sent Distance Vector to neighbors: " + dvString);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Router <RouterID>"); // Fixed typo: "java Host" to "java Router"
            System.exit(1);
        }
        Router router = new Router(args[0]);
        router.start();
    }
}