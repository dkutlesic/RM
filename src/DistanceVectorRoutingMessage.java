import java.util.Map;

/**
 * Distance Vector Routing (DVR) message contains the encoded map needed for routing
 * Each node sends its own map to neighbors in a DVR message
 */
public class DistanceVectorRoutingMessage extends Message {

    /**
     * Distances represents the vector for routing
     * Map key: id of node
     * Value:   distance to that node
     */
    private Map<Integer, Integer> distances;
    /**
     * Routing table
     * Map key: id of node
     * Value:   next node in chain
     */
    private Map<Integer, Integer> routingTable;
    /**
     * The identification of a node that sent the distance vector routing message
     */
    private int source;

    public DistanceVectorRoutingMessage(Map<Integer, Integer> distances, Map<Integer, Integer> routingTable, int source) {
        this.distances = distances;
        this.routingTable = routingTable;
        this.source = source;
    }

    public boolean makesLoop(int id, int node) {
        return distances.get(node) != 0 && routingTable.get(node) == id ;
    }

    public Map<Integer, Integer> getDistances() {
        return distances;
    }
    public int getSource() {
        return source;
    }

    @Override
    public types getType() {
        return types.ROUTING_MESSAGE;
    }

    @Override
    public String toString() {
        return "DistanceVectorRoutingMessage{" +
                "distances=" + distances +
                ", source=" + source +
                '}';
    }
}
