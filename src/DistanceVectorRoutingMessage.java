import java.util.Map;

public class DistanceVectorRoutingMessage extends Message {

    private Map<Integer, Integer> distances;

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
