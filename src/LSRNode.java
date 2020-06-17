import java.io.PrintWriter;
import java.util.*;

public class LSRNode extends Node {

    public static int infinity = 100000;
    //TODO public only for testing
    public Graph topology;

    protected enum Phase{
        FLOODING,
        DIJKSTRA
    }
    protected Phase phase;

    public LSRNode(Map<Integer, Integer> adjacentNodesTable, int identification) {
        super(adjacentNodesTable, identification);
        this.identification = identification;
        this.routingTable = new HashMap<>();
        this.phase = Phase.FLOODING;
        initializeTopology();
    }

    private void initializeTopology(){
        Vertex me = new Vertex(this.identification);
        topology = new Graph(me);

        updateTopologyWithVertex(this.identification);
        adjacentNodesTable.forEach((k,v) -> {
            updateTopologyWithVertex(k);
            updateTopologyWithLink(identification, k, v);
        });
    }

    @Override
    void handleRoutingMessage(Message message) {
        if(this.phase == Phase.DIJKSTRA){
            //FIXME what if we get the routing message during the phase 2?
        }
        else{
            assert message instanceof FloodingTopologyMessage;
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
    }

    @Override
    void cleanupDeadRouts(Set<Integer> deadNeighbors) {

    }

    public Graph getTopology(){
        return topology;
    }

    //TODO public only for testing
    public void updateTopologyWithLink(int vertexId1, int vertexId2, int linkCost){
        Vertex v1, v2;

        if(!topology.containtsVertexId(vertexId1))
            updateTopologyWithVertex(vertexId1);
        v1 = topology.getVertexWithId(vertexId1);

        if(!topology.containtsVertexId(vertexId2))
            updateTopologyWithVertex(vertexId2);
        v2 = topology.getVertexWithId(vertexId2);

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

    //TODO public only for testing
    public void updateTopologyWithVertex(int vertexId){
        if(!topology.getVertices().contains(topology.getVertexWithId(vertexId))) {
            topology.addVertex(new Vertex(vertexId));
            //System.out.println("Vertex added: " + vertexId);
        }
    }

    //TODO public only for testing
    public void runDijkstra(){
        //in this function we update topology information with appropriate distance and next hop
        assert this.phase == Phase.DIJKSTRA;
        System.out.println("blaa");
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
        for(Vertex v : topology.getVertices()){
            if(v == topology.me) continue;

            //Vertex nextHop = topology.me;
            Vertex nextHop = v;
            Vertex ancestor = previous.get(v);
            while (ancestor.getId() != topology.me.getId()) {
                nextHop = ancestor;
                ancestor = previous.get(ancestor);
            }
            v.setNextHop(nextHop);
        }
    }

    private Set<Vertex> copyVertices(){
        Set<Vertex> set = new HashSet<>();
        for(Vertex v : topology.getVertices())
            set.add(v);
        return set;
    }

    //TODO public only for testing
    public class Edge {

        private Vertex to;
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

    //TODO public only for testing
    public class Vertex {

        private Integer id;
        private Set<Edge> edges;
        //distance from me to the vertex
        private int distance;
        //which node I use to get to this vertex
        private Vertex nextHop;

        public Vertex(Integer id) {
            this.id = id;
            edges = new HashSet<>();
        }

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

    private class VertexComparator implements Comparator<Vertex>{
        @Override
        public int compare(Vertex v1, Vertex v2) {
            return v1.distance - v2.distance;
        }
    }

    public class Graph {

        private Set<Vertex> vertices;
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

        public Vertex getVertexWithId(Integer id){
            for(Vertex v : vertices){
                if(v.getId() == id)
                    return v;
            }
            return null;
        }

        public boolean containtsVertexId(Integer id){
            for(Vertex v : vertices){
                if(v.getId() == id)
                    return true;
            }
            return false;
        }
    }

}
