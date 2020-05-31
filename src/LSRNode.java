import java.util.Map;

public class LSRNode extends Node {
    public LSRNode(Map<Integer, Integer> adjacentNodesTable, int id) {
        super(adjacentNodesTable, id);
    }

    @Override
    void handleRoutingMessage(Message message) {

    }
}
