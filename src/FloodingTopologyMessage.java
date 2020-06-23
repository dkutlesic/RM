import java.util.Map;

/**
 * The special type of a flooding message that uses a concept of flooding for routing
 */
public class FloodingTopologyMessage extends FloodingMessage {

    /**
     * The table of adjacency for an original sender
     */
    private Map<Integer, Integer> adjacentNodesTable;

    public FloodingTopologyMessage(int source, int id, int originalSender, Map<Integer, Integer> adjacentNodesTable) {
        super(source, id, originalSender);
        this.adjacentNodesTable = adjacentNodesTable;
    }

    public Map<Integer, Integer> getAdjacentNodesTable() {
        return adjacentNodesTable;
    }

    @Override
    public FloodingMessage copyForSending(int source) {
        return new FloodingTopologyMessage(source, id, originalSender, adjacentNodesTable);
    }
}
