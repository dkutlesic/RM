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






    //getters and setters

    public Node getSource() {
        return source;
    }

    public Node getDestination() {
        return destination;
    }

    public int getDistance() {
        return distance;
    }

    public Node getNextHop() {
        return nextHop;
    }

    public void setSource(Node source) {
        this.source = source;
    }

    public void setDestination(Node destination) {
        this.destination = destination;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public void setNextHop(Node nextHop) {
        this.nextHop = nextHop;
    }
}
