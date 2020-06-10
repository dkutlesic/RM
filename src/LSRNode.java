import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class LSRNode extends Node {

    public static int infinity = 100000;
    Graph topology;
    Set<Route> routes;

    public LSRNode(Map<Integer, Integer> adjacentNodesTable, int id) {
        super(adjacentNodesTable, id);
    }

    @Override
    void handleRoutingMessage(Message message) {

    }

    @Override
    void cleanupDeadRouts(Set<Integer> deadNeighbors) {

    }

    protected void runDijkstra(){
        //in this function we update topology information with appropriate distance and next hop
        for(Vertex v : topology.getVertices()){
            v.setDistance(infinity);
        }
        topology.me.setDistance(0);
        topology.me.setNextHop(topology.me);

        Set<Vertex> unprocessed = topology.getVertices();
        Map<Vertex, Vertex> previous = new HashMap<>();

        while (!unprocessed.isEmpty()){
            Vertex current = Collections.min(unprocessed, new VertexComparator());
            for(Edge adjacentEdge : current.getEdges()){
                if(current.getDistance() + adjacentEdge.getWeight() < adjacentEdge.getTo().getDistance()) {
                    adjacentEdge.getTo().setDistance(current.getDistance() + adjacentEdge.getWeight());
                    previous.put(adjacentEdge.getTo(), current);
                }
            }
            unprocessed.remove(current);
        }

        //calculating next hop
        for(Vertex v : topology.getVertices()){
            Vertex nextHop = topology.me;
            Vertex ancestor = previous.get(v);
            while (ancestor != topology.me) {
                nextHop = ancestor;
                ancestor = previous.get(ancestor);
            }
            v.setNextHop(nextHop);
        }

    }

    class Edge {

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


    private class Vertex {

        private String label;
        private Set<Edge> edges;
        //distance from me to the vertex
        private int distance;
        //which node I use to get to this vertex
        private Vertex nextHop;

        public Vertex(String label) {
            this.label = label;
            edges = new HashSet<>();
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
    }

    private class VertexComparator implements Comparator<Vertex>{
        @Override
        public int compare(Vertex v1, Vertex v2) {
            return v1.distance - v2.distance;
        }
    }

    private class Graph {

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
    }

}
