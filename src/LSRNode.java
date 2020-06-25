import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * LSRNode states for Link State routing (LSR) node
 * If LSR node is used the LSR algorithm is applied for routing
 * Each node runs its own algorithm
 * First, each node flood neighbors with its adjacency tables. When all nodes have full topology
 * of a network, Dijkstra's algorithm is applied to calculate optimal paths and next hops
 */
public class LSRNode extends Node {

    /**
     * In the link state algorithm the node is in different phases
     */
    protected enum Phase{
        /**
         * Flooding is an initial phase in the algorithm
         * All nodes flood information about their neighbors until all nodes learn full topology of a network
         */
        FLOODING,
        /**
         * In this phase node calculates distances and next hops using Dijkstra's algorithm
         */
        DIJKSTRA,
        /**
         * The topology is updated after cleaning up dead routes
         */
        UPDATED,
        /**
         * The node has full topology and the routing is finished
         */
        FINISHED
    }

    /**
     * Maximal distance between nodes
     * Used in Dijkstra's algorithm for a distance initialization
     */
    private static int infinity = 100000;
    /**
     * Topology is a graph that represents the network topology
     */
    protected Graph topology;

    /**
     * Current phase
     */
    protected Phase phase;
    protected ReentrantLock phaseLock;

    public LSRNode(Map<Integer, Integer> adjacentNodesTable, int identification) {
        super(adjacentNodesTable, identification);   // here we know that routing table entry is pointing us toward next router

        this.identification = identification;
        this.routingTable = new HashMap<>();
        this.phase = Phase.FLOODING;
        initializeTopology();
        phaseLock = new ReentrantLock();
    }

    public Graph getTopology(){ return topology; }

    /**
     * initialize topology
     * add this node and its neighbors to a network topology
     */
    private void initializeTopology(){
        Vertex me = new Vertex(this.identification);
        topology = new Graph(me);

        updateTopologyWithVertex(this.identification);
        adjacentNodesTable.forEach((k,v) -> {
            updateTopologyWithVertex(k);

            updateTopologyWithLink(identification, k, v);
        });
    }

    /**
     * @param message
     * Handling flooding topology message
     */
    @Override
    void handleRoutingMessage(Message message) {

        assert message instanceof FloodingTopologyMessage;
        phaseLock.lock();
        if(this.phase == Phase.FINISHED){
            // if we calculated routes we need
            // check if something is changed
            if(topology.containsVertexId(((FloodingTopologyMessage) message).originalSender)) {
                // we are checking if there is news about topology
                ((FloodingTopologyMessage) message).getAdjacentNodesTable().forEach(
                        (id, length) -> {
                            if (!topology.containsEdge(((FloodingTopologyMessage) message).originalSender, id, length)){
                                // the topology need to be updated
                                updateTopologyWithLink(((FloodingTopologyMessage) message).originalSender, id, length);
                                phase = Phase.FLOODING;
                            }
                        });
            }
        }
        else if(this.phase == Phase.DIJKSTRA){
            // this shouldn't happen
            System.err.println("PHASE IS DIJKSTRA");
        }
        else{
            FloodingTopologyMessage floodingTopologyMessage = (FloodingTopologyMessage) message;
            int source = floodingTopologyMessage.getSource();
            if(source == identification){
                // we sent message we should send it to our neighbours
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
                // we got message, we should handle it
                if (adjacentNodesTable.containsKey(source + Node.NODE_PORT_OFFSET)) {
                    updateTopologyWithVertex(floodingTopologyMessage.originalSender);
                    floodingTopologyMessage.getAdjacentNodesTable().forEach((k, v) -> {
                        updateTopologyWithVertex(k);
                        updateTopologyWithLink(floodingTopologyMessage.originalSender, k, v);
                    });
                } else {
                    System.err.println("got packet from unknown node!");
                }
            }
        }
        phaseLock.unlock();
    }

    /**
     * @param deadNeighbors Set of identification of dead nodes
     */
    @Override
    public void cleanupDeadRouts(Set<Integer> deadNeighbors) {
        phaseLock.lock();
        if (!deadNeighbors.isEmpty()) {
            deadNeighbors.forEach(neighborId -> removeVertex(neighborId));

            phase = Phase.UPDATED;
        }

        phaseLock.unlock();
    }

    /**
     * Removing a link between two given vertices (their identification)
     * @param vertexId1
     * @param vertexId2
     */
    public void removeLink(Integer vertexId1, Integer vertexId2){
        Vertex v1, v2;

        if(!topology.containsVertexId(vertexId1))
            return;
        v1 = topology.getVertexWithId(vertexId1);

        if(!topology.containsVertexId(vertexId2))
            return;
        v2 = topology.getVertexWithId(vertexId2);

        v1.edges.removeIf(e -> e.getTo().getId() == vertexId2);
        v2.edges.removeIf(e -> e.getTo().getId() == vertexId1);
    }


    /**
     * Removing vertex with a given id
     * Removing links to-from this vertex
     * @param vertexId identification of the vertex
     */
    public void removeVertex(Integer vertexId){
        Vertex v;

        if(!topology.containsVertexId(vertexId))
            return;
        v = topology.getVertexWithId(vertexId);

        //removing links to-from this vertex
        for(Vertex ver: topology.vertices){
            removeLink(ver.id, v.id);
        }

        //removing vertex
        topology.vertices.removeIf(ver -> ver.id  == vertexId);
    }

    /**
     * @param vertexId1 Identification of the first vertex
     * @param vertexId2 Identification of the second vector
     * @param linkCost The cost of the link between these two vertices
     * Update the topology with a link with a given cost between two given vertices
     */
    protected void updateTopologyWithLink(int vertexId1, int vertexId2, int linkCost){
        Vertex v1, v2;

        if(!topology.containsVertexId(vertexId1))
            updateTopologyWithVertex(vertexId1);
        v1 = topology.getVertexWithId(vertexId1);

        if(!topology.containsVertexId(vertexId2))
            updateTopologyWithVertex(vertexId2);
        v2 = topology.getVertexWithId(vertexId2);

        System.out.println(vertexId1);
        System.out.println(vertexId2);

        if(!v1.hasEdgeTo(vertexId2)){
            Edge from1to2 = new Edge(v2, linkCost);
            v1.addEdge(from1to2);
            //System.out.println("Link added: from " + vertexId1 + " to " + vertexId2);
        }

        if(!v2.hasEdgeTo(vertexId1)){
            Edge from2to1 = new Edge(v1, linkCost);
            v2.addEdge(from2to1);
            //System.out.println("Link added: from " + vertexId2 + " to " + vertexId1);
        }

    }

    /**
     * @param vertexId Identification of the vertex
     * Adds the vertex with a given identification to the topology
     * No links are added to the topology
     */
    protected void updateTopologyWithVertex(int vertexId){
        if(!topology.getVertices().contains(topology.getVertexWithId(vertexId))) {
            topology.addVertex(new Vertex(vertexId));
            //System.out.println("Vertex added: " + vertexId);
        }
    }

    /**
     *  Update the topology information with appropriate distances between
     *  this node and other nodes and next hops for routing
     */
    protected void runDijkstra(){
        assert this.phase == Phase.DIJKSTRA;

        for(Vertex v : topology.getVertices()){
            v.setDistance(infinity);
        }
        topology.me.setDistance(0);
        topology.getVertexWithId(this.identification).setDistance(0);
        topology.me.setNextHop(topology.me);
        topology.getVertexWithId(this.identification).setNextHop(topology.me);

        Set<Vertex> unprocessed = copyVertices();

        //(node, parent)
        Map<Vertex, Vertex> previous = new HashMap<>();
        previous.put(topology.getVertexWithId(this.identification), topology.getVertexWithId(this.identification));

        while (!unprocessed.isEmpty()){
            Vertex current = Collections.min(unprocessed, new VertexComparator());
            //System.out.println("Currently processing: " + current.getId());
            for(Edge adjacentEdge : current.getEdges()){
                if(current.getDistance() + adjacentEdge.getWeight() < adjacentEdge.getTo().getDistance()) {
                    adjacentEdge.getTo().setDistance(current.getDistance() + adjacentEdge.getWeight());
                    previous.put(adjacentEdge.getTo(), current);
                }
            }
            unprocessed.remove(current);
            /*System.out.println("\tcurrent situation:");
            for(Vertex v: topology.getVertices()){
                System.out.println(v.getId() + " " + v.getDistance());
            }*/
        }

        //previous.forEach((k,v) -> System.out.println("ja: " + k.getId() + " roditelj: " + v.getId()));

        //calculating next hop
        routingTableLock.lock();
        for(Vertex v : topology.getVertices()){
            if(v == topology.me) continue;
            Vertex nextHop = v;
            Vertex ancestor = previous.get(v);
            while (ancestor.getId() != topology.me.getId()) {
                nextHop = ancestor;
                ancestor = previous.get(ancestor);
            }
            v.setNextHop(nextHop);
            routingTable.put(v.id , nextHop.id);
        }
        routingTableLock.unlock();
        this.phase = Phase.FINISHED;
    }

    /**
     * @return all vertices from the graph topology
     */
    private Set<Vertex> copyVertices(){
        Set<Vertex> set = new HashSet<>();
        for(Vertex v : topology.getVertices())
            set.add(v);
        return set;
    }

    /**
     * Utility class for graph class
     * Represents edge between two vertex in a topology graph
     * Edges are directional
     */
    protected class Edge {

        /**
         * The vertex that is at the end of the link
         */
        private Vertex to;
        /**
         * link cost
         */
        private int weight;

        public Edge(Vertex to, int weight) {
            this.to = to;
            this.weight = weight;
        }

        public Vertex getTo(){
            return to;
        }
        public int getWeight() {
            return weight;
        }
    }

    /**
     * Utility class for graph class
     * Represents vertex in a topology class
     * Vertex in topology == node in network
     */
    protected class Vertex {

        /**
         * The identification of the vertex
         */
        private Integer id;
        /**
         * All edges from this vertex to other vertices
         */
        private Set<Edge> edges;
        /**
         * distance from me (this node) to the vertex (node)
         */
        private int distance;
        /**
         * which node is used to get to this vertex (node)
         */
        private Vertex nextHop;

        public Vertex(Integer id) {
            this.id = id;
            edges = new HashSet<>();
        }

        /**
         * @param identification The identification of the vertex
         * @return true if there is a link from this Vertex to a vertex with a given identification
         */
        public boolean hasEdgeTo(int identification){
            for(Edge e : edges){
                if(e.getTo().getId() == identification)
                    return true;
            }
            return false;
        }

        public boolean addEdge(Edge edge){
            return edges.add(edge);
        }

        public void setDistance(int distance){
            this.distance = distance;
        }

        public Set<Edge> getEdges(){
            return edges;
        }

        public void setNextHop(Vertex nextHop){
            this.nextHop = nextHop;
        }

        public int getDistance() {
            return distance;
        }

        public Integer getId() { return id; }

        public Vertex getNextHop() { return nextHop; }
    }

    /**
     * Reporting to neighbors that we are alive and well
     * If the topology has changed, send the message to neighbors about changing
     */
    @Override
    protected void reportToNeighbours(){
        super.reportToNeighbours();

        phaseLock.lock();
        if(this.phase == Phase.UPDATED){
            // this will mean we updated something in our adjacency neighbour list
            FloodingTopologyMessage messaqe = new FloodingTopologyMessage(identification, 0, identification,adjacentNodesTable);
            try {
                getNodeWriter().getWritingBuffer().put(messaqe);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            phase = Phase.FLOODING;
        }
        else if(this.phase == Phase.FLOODING){
            // if we are in flooding state check how much time has passed since last change
        }
        phaseLock.unlock();
    }

    /**
     * The vertex is "less" if it is closer to this node
     */
    private class VertexComparator implements Comparator<Vertex>{
        @Override
        public int compare(Vertex v1, Vertex v2) {
            return v1.distance - v2.distance;
        }
    }

    protected class Graph  {

        /**
         * Nodes in the network
         */
        private Set<Vertex> vertices;
        /**
         * This node
         */
        private Vertex me;

        public Graph(Vertex me) {
            vertices = new HashSet<>();
            this.me = me;
        }

        boolean addVertex(Vertex vertex) {
            return vertices.add(vertex);
        }

        public Set<Vertex> getVertices() {
            return vertices;
        }

        /**
         * @param id
         * @return Vertex for a given id. If it does not exist null is returned
         */
        public Vertex getVertexWithId(Integer id){
            for(Vertex v : vertices){
                if(v.getId().equals(id))
                    return v;
            }
            return null;
        }

        /**
         * @param id
         * @return true if the graph contains the vertex with given identification
         */
        public boolean containsVertexId(Integer id){
            for(Vertex v : vertices){
                if(v.getId() == id)
                    return true;
            }
            return false;
        }

        /**
         * @param v1 vertex1 identification
         * @param v2 vertex2 identification
         * @param length //FIXME think we do not need it?
         * @return
         */
        public boolean containsEdge(Integer v1, Integer v2, int length){
            if(vertices.stream()
                    .map(v -> v.id)
                    .filter(id -> {return id == v1 || id == v2;})
                    .count() == 2)
            {
                // FIXME: topology needs changing in this case!
                return true;
            }
            else
                return false;
        }
    }

}
