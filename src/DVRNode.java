import java.io.PrintWriter;
import java.util.*;

public class DVRNode extends Node {

    //DVR
    private List<Integer> DVRlistOfAllNodes;
    private Map<Integer, Integer> DVRdistancesFromNodes;
    private Map<Integer, Integer> DVRnextNode;

    public DVRNode(Map<Integer, Integer> adjacentNodesTable, int id, Collection<Integer> listOfAllNodes) {
        super(adjacentNodesTable, id);
        this.DVRlistOfAllNodes = new Vector<>(listOfAllNodes);
        this.DVRdistancesFromNodes = new HashMap<>();
        this.DVRnextNode = new HashMap<>();
        this.DVRlistOfAllNodes.forEach(node -> {
            this.DVRnextNode.put(node, null);
            this.DVRdistancesFromNodes.put(node, -1);
        });
        this.DVRdistancesFromNodes.put(this.identification, 0);
    }


    private void handleDistanceVector(DistanceVectorRoutingMessage message){
        int source = message.getSource();
        if(source == identification){
            // we sent message we should send it to our neighbours
            adjacentNodesTable.forEach((Integer neighbour, Integer length) -> {
                if(socketTable.containsKey(neighbour - Node.NODE_PORT_OFFSET)){
                    PrintWriter outWriter = outStreams.get(neighbour - Node.NODE_PORT_OFFSET);
                    outWriter.write(message.sendingFormat());
                    outWriter.flush();
                }
                else{
                    System.err.println("UNKNOWN SOCKET FOR NEIGHBOUR");
                }
            });
        }
        else {
            // we got message, we should handle it
            if (adjacentNodesTable.containsKey(source + Node.NODE_PORT_OFFSET)) {
                int distance_from_source = adjacentNodesTable.get(source + Node.NODE_PORT_OFFSET);
                message.getDistances().forEach((Integer node, Integer distance) -> {
                    if (distance >= 0) {
                        if ( DVRdistancesFromNodes.get(node) < 0 || distance + distance_from_source < DVRdistancesFromNodes.get(node)) {
                            DVRdistancesFromNodes.put(node, distance + distance_from_source);
                            DVRnextNode.put(node, source);
                        }
                    }

                });
            } else {
                System.err.println("got packet from unknown node!");
            }
        }
    }

    @Override
    void handleRoutingMessage(Message message) {
        assert message instanceof DistanceVectorRoutingMessage;
        int source = ((DistanceVectorRoutingMessage) message).getSource();
        if(source == identification){
            // we sent message we should send it to our neighbours
            adjacentNodesTable.forEach((Integer neighbour, Integer length) -> {
                if(socketTable.containsKey(neighbour - Node.NODE_PORT_OFFSET)){
                    PrintWriter outWriter = outStreams.get(neighbour - Node.NODE_PORT_OFFSET);
                    outWriter.write(message.sendingFormat());
                    outWriter.flush();
                }
                else{
                    System.err.println("UNKNOWN SOCKET FOR NEIGHBOUR");
                }
            });
        }
        else {
            // we got message, we should handle it
            if (adjacentNodesTable.containsKey(source + Node.NODE_PORT_OFFSET)) {
                int distance_from_source = adjacentNodesTable.get(source + Node.NODE_PORT_OFFSET);
                ((DistanceVectorRoutingMessage) message).getDistances().forEach((Integer node, Integer distance) -> {
                    if (distance >= 0) {
                        if ( DVRdistancesFromNodes.get(node) < 0 || distance + distance_from_source < DVRdistancesFromNodes.get(node)) {
                            DVRdistancesFromNodes.put(node, distance + distance_from_source);
                            DVRnextNode.put(node, source);
                        }
                    }

                });
            } else {
                System.err.println("got packet from unknown node!");
            }
        }
    }
}
