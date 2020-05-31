import org.apache.commons.lang3.SerializationUtils;

import java.io.PrintWriter;
import java.util.*;

public class DVRNode extends Node {

    //DVR
    private List<Integer> listOfAllNodes;
    private Map<Integer, Integer> distancesFromNodes;

    public DVRNode(Map<Integer, Integer> adjacentNodesTable, int id, Collection<Integer> listOfAllNodes) {
        super(adjacentNodesTable, id);
        this.listOfAllNodes = new Vector<>(listOfAllNodes);
        this.distancesFromNodes = new HashMap<>();
        this.routingTable = new HashMap<>();
        this.listOfAllNodes.forEach(node -> {
            this.routingTable.put(node, null);
            this.distancesFromNodes.put(node, -1);
        });
        this.distancesFromNodes.put(this.identification, 0);
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
                        if ( distancesFromNodes.get(node) < 0 || distance + distance_from_source < distancesFromNodes.get(node)) {
                            distancesFromNodes.put(node, distance + distance_from_source);
                            routingTable.put(node, source);
                        }
                    }

                });
            } else {
                System.err.println("got packet from unknown node!");
            }
        }
    }

    @Override
    protected void reportToNeighbours(PrintWriter out) {
        super.reportToNeighbours(out);
        DistanceVectorRoutingMessage routingMessage = new DistanceVectorRoutingMessage(distancesFromNodes, this.identification);
        try {
            out.println("===========================\nBefore:");
            out.println(routingMessage);
            out.println("After:");
            out.println(Message.parseMessage(routingMessage.sendingFormat()));
            out.println("Message length: " + routingMessage.toString().length());
            if(routingMessage.sendingFormat().indexOf('!') != routingMessage.sendingFormat().length() - 1){
                out.write("CAN CAUSE PROBLEMS");
            }

            out.flush();
            getNodeWriter().getWritingBuffer().put(routingMessage);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
