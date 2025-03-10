import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Router {
    String id;
    String nextHop;
    boolean isDirectlyConnected;
    private Map<String, Router> routingTable = new HashMap<>();

    public Router(String id){
        this.id = id;
        String configPath = "config.properties";
        setupConfig(configPath);
    }

    private void setupConfig(String configPath) {
        String filePath = "src/" + configPath;
        Properties properties = new ConfigLoader().loadProperties(filePath);
        routingTable.put(properties.getProperty("device." + id + ".left"), "left port", isDirectlyConnected);
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
