public class Route {
    private Node source;
    private Node destination;
    private int distance;
    private Node nextHop;

    public Route(Node source, Node destination, int distance, Node nextHop) {
        this.source = source;
        this.destination = destination;
        this.distance = distance;
        this.nextHop = nextHop;
    }
}
