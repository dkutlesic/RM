import java.util.HashMap;
import java.util.Map;

/**
 * Class used for testing
 */
public class TestLSRNodeDijkstra {

    public static void main(String[] args){

        Map<Integer, Integer> adjacent = new HashMap<>();
        adjacent.put(2,3);
        adjacent.put(3,2);
        LSRNode node = new LSRNode(adjacent, 1);

        node.updateTopologyWithVertex(3);
        node.updateTopologyWithVertex(4);
        node.updateTopologyWithVertex(5);

        node.updateTopologyWithLink(2, 4, 8);
        node.updateTopologyWithLink(2, 5, 1);
        node.updateTopologyWithLink(3, 4, 4);
        node.updateTopologyWithLink(4, 5, 3);

        for(LSRNode.Vertex v : node.topology.getVertices()){
            System.out.println("vertex: " + v.getId());
            for(LSRNode.Edge e : v.getEdges()){
                System.out.println("to: " + e.getTo().getId());
            }
        }

        node.runDijkstra();

        for(LSRNode.Vertex v : node.topology.getVertices()){
            System.out.println("target: " + v.getId() + ", distance: " + v.getDistance() + " , next hop: " + v.getNextHop().getId());
        }
    }
}
