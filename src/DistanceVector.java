import java.util.HashMap;
import java.util.Map;

public class DistanceVector {
    private static class Entry {
        int distance;
        String nextHop;

        Entry(int distance, String nextHop) {
            this.distance = distance;
            this.nextHop = nextHop;
        }

        @Override
        public String toString() {
            return distance + ":" + nextHop;
        }
    }

    private final Map<String, Entry> distanceVector;

    public DistanceVector() {
        this.distanceVector = new HashMap<>();
    }

    public void initialize(Map<String, String> directlyConnectedSubnets) {
        for (Map.Entry<String, String> entry : directlyConnectedSubnets.entrySet()) {
            String subnet = entry.getKey();
            String nextHop = entry.getValue();
            distanceVector.put(subnet, new Entry(1, nextHop));
        }
    }

    public boolean updateWithBellmanFord(String neighbor, DistanceVector receivedDV) {
        boolean updated = false;
        for (Map.Entry<String, Entry> entry : receivedDV.distanceVector.entrySet()) {
            String destination = entry.getKey();
            int distanceToDest = entry.getValue().distance;
            if (destination.equals(neighbor)) {
                continue;
            }
            int newDistance = 1 + distanceToDest;
            Entry currentEntry = distanceVector.get(destination);
            if (currentEntry == null || newDistance < currentEntry.distance) {
                distanceVector.put(destination, new Entry(newDistance, neighbor));
                updated = true;
            }
        }
        return updated;
    }

    public String getNextHop(String destination) {
        Entry entry = distanceVector.get(destination);
        return entry != null ? entry.nextHop : null;
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Entry> entry : distanceVector.entrySet()) {
            if (!sb.isEmpty()) {
                sb.append(";");
            }
            sb.append(entry.getKey()).append(":").append(entry.getValue().toString());
        }
        return sb.toString();
    }

    public static DistanceVector deserialize(String dvString) {
        DistanceVector dv = new DistanceVector();
        if (dvString == null || dvString.isEmpty()) {
            return dv;
        }
        String[] entries = dvString.split(";");
        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length == 3 && !parts[0].contains(",")) { // Skip malformed destinations
                String destination = parts[0];
                int distance = Integer.parseInt(parts[1]);
                String nextHop = parts[2];
                dv.distanceVector.put(destination, new Entry(distance, nextHop));
            }
        }
        return dv;
    }

    public void printDistanceVector(String routerId) {
        System.out.println("Distance Vector for Router " + routerId + ":");
        distanceVector.forEach((dest, entry) ->
                System.out.println("  Destination: " + dest + ", Distance: " + entry.distance + ", Next-Hop: " + entry.nextHop));
        System.out.println("--------------------------------------------");
    }
}