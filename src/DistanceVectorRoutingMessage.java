import java.util.Map;

/**
 * Distance Vector Routing (DVR) message contains the encoded map needed for routing
 * Each node sends its own map to neighbors in a DVR message
 */
public class DistanceVectorRoutingMessage extends Message {

    /**
     * Distances represents the vector for routing
     * Map key: //FIXME
     * Value: //FIXME
     */
    private Map<Integer, Integer> distances;
    /**
     * The identification of a node that sent the distance vector routing message
     */
    private int source;

    public DistanceVectorRoutingMessage(Map<Integer, Integer> distances, int source) {
        this.distances = distances;
        this.source = source;
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
