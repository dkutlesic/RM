import org.apache.commons.lang3.SerializationUtils;

import java.io.PrintWriter;
import java.util.*;

/**
 * DVRNode states for Distance Vector routing (DVR) node
 * If DVR node is used the DVR algorithm is applied for routing
 * Each node maintains a vector of distances and next hops to all destinations
 * Each node runs its own algorithm
 *
 * - Periodically send the vector to all of its neighbors
 * - Update the vector for each destination by selecting the shortest distance heard after adding cost of a neighbor cost
 */
public class DVRNode extends Node {

    //DVR
    /**
     * identifications of all nodes in the topology
     */
    private List<Integer> listOfAllNodes;
    /**
     * distance vectors
     */
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

    /**
     * Handling distance vector routing message
     * @param message
     */
    @Override
    void handleRoutingMessage(Message message) {
        assert message instanceof DistanceVectorRoutingMessage;
        int source = ((DistanceVectorRoutingMessage) message).getSource();
        if(source == identification){
            // The message is sent; the message is sent to neighbors
            adjacentNodesTable.forEach((Integer neighbour, Integer length) -> {
                if(outStreams.containsKey(neighbour - Node.NODE_PORT_OFFSET)){
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
            // The message is for this node message, this node should handle it
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

    /**
     * Set of identification of dead nodes
     * @param deadNeighbors
     */
    @Override
    void cleanupDeadRouts(Set<Integer> deadNeighbors) {
        routingTableLock.lock();
        try {
            deadNeighbors.forEach(deadRoute -> {
                log.println("removing " + (deadRoute - NODE_PORT_OFFSET) );
                routingTable.put(deadRoute - NODE_PORT_OFFSET, null);
                distancesFromNodes.put(deadRoute - NODE_PORT_OFFSET, -1);
                System.err.println(routingTable);
                routingTable.forEach((dest, next_hop) -> {
                    if(dest != this.identification
                            && dest != deadRoute - NODE_PORT_OFFSET
                            && next_hop == deadRoute - NODE_PORT_OFFSET){
                        routingTable.put(dest, null);
                        distancesFromNodes.put(dest, -1);
                    }
                });
            });
            log.println("routing table: " + routingTable);
        }
        finally {
            routingTableLock.unlock();
        }

    }

    /**
     * Reporting to neighbors that we are alive and well
     */
    @Override
    protected void reportToNeighbours() {
        //send ping to all neighbours that we are alive and well
        super.reportToNeighbours();
        DistanceVectorRoutingMessage routingMessage = new DistanceVectorRoutingMessage(distancesFromNodes, this.identification);
        try {
//            out.println(java.time.LocalDate.now() + ": " + routingMessage);
            if(routingMessage.sendingFormat().indexOf('!') != routingMessage.sendingFormat().length() - 1){
                log.write("CAN CAUSE PROBLEMS");
            }

            log.flush();
            getNodeWriter().getWritingBuffer().put(routingMessage);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
