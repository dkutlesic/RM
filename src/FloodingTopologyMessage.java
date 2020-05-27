import java.util.Map;

public class FloodingTopologyMessage extends FloodingMessage {

    private Map<Integer, Integer> adjacentNodesTable; // adjacentNodesTable from original sender

    public Map<Integer, Integer> getAdjacentNodesTable() {
        return adjacentNodesTable;
    }

    public FloodingTopologyMessage(int source, int id, int originalSender, Map<Integer, Integer> adjacentNodesTable) {
        super(source, id, originalSender);
        this.adjacentNodesTable = adjacentNodesTable;
    }

    @Override
    public FloodingMessage copyForSending(int source) {
        return new FloodingTopologyMessage(source, id, originalSender, adjacentNodesTable);
    }
}
