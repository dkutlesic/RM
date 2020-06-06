import java.util.Map;
import java.util.Set;

public class LSRNode extends Node {
    public LSRNode(Map<Integer, Integer> adjacentNodesTable, int id) {
        super(adjacentNodesTable, id);
    }

    @Override
    void handleRoutingMessage(Message message) {

    }

    @Override
    void cleanupDeadRouts(Set<Integer> deadNeighbors) {

    }
}
