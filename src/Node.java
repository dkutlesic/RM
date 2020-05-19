import java.util.Vector;
import java.util.function.ToDoubleBiFunction;

public class Node {
    //statistics
    private long sentPacketsNumber;
    private long receivedPacketsNumber;
    private long transmissionsNumber;

    //tables
    private Vector<Vector<Integer>> adjacentNodesTable;
    private Vector<Route> routingTable;

    //utils
    private int port;
    private int address;

    public Node(Vector<Vector<Integer>> adjacentNodesTable) {
        this.sentPacketsNumber = 0;
        this.receivedPacketsNumber = 0;
        this.transmissionsNumber = 0;
        this.adjacentNodesTable = new Vector<Vector<Integer>>(adjacentNodesTable);
        this.routingTable = new Vector<Route>();
    }

    public void updateAdjacentNodesTable(Vector<Vector<Integer>> adjacentNodesTable){
        //TODO
    }

    public void sendMessage(String message, Node node){
        //TODO
    }






    //getters and setters

    public long getSentPacketsNumber() {
        return sentPacketsNumber;
    }

    public long getReceivedPacketsNumber() {
        return receivedPacketsNumber;
    }

    public long getTransmissionsNumber() {
        return transmissionsNumber;
    }

    public Vector<Vector<Integer>> getAdjacentNodesTable() {
        return adjacentNodesTable;
    }

    public Vector<Route> getRoutingTable() {
        return routingTable;
    }

    public int getPort() {
        return port;
    }

    public int getAddress() {
        return address;
    }

    public void setSentPacketsNumber(long sentPacketsNumber) {
        this.sentPacketsNumber = sentPacketsNumber;
    }

    public void setReceivedPacketsNumber(long receivedPacketsNumber) {
        this.receivedPacketsNumber = receivedPacketsNumber;
    }

    public void setTransmissionsNumber(long transmissionsNumber) {
        this.transmissionsNumber = transmissionsNumber;
    }

    public void setAdjacentNodesTable(Vector<Vector<Integer>> adjacentNodesTable) {
        this.adjacentNodesTable = adjacentNodesTable;
    }

    public void setRoutingTable(Vector<Route> routingTable) {
        this.routingTable = routingTable;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setAddress(int address) {
        this.address = address;
    }
}
