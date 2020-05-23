import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class Node extends Thread{
    //statistics
    private long sentPacketsNumber;
    private long receivedPacketsNumber;
    private long transmissionsNumber;

    //tables
    private Map<Integer, Integer> adjacentNodesTable;
    private Map<Integer, Socket> socketTable;
    private Map<Integer, Integer> routingTable;
    private Map<Integer, Vector<Integer>> FloodingTable;

    //utils
    public static final int NODE_PORT_OFFSET = 1234; // node i will be connected to port_offset + i port
    private int port;
    private int identification;
    private static final long REPORT_TIME = 100;

    public static Map<Integer, Integer>[] ROUTING_TABLE_FOR_DEMO;

    private NodeWriter nodeWriter;
    private NodeReader reader;

    public Node(Map<Integer, Integer> adjacentNodesTable, int id) {
        this.sentPacketsNumber = 0;
        this.receivedPacketsNumber = 0;
        this.transmissionsNumber = 0;
        this.adjacentNodesTable = new HashMap<>(adjacentNodesTable);
        this.socketTable = new HashMap<>();
        this.routingTable = new HashMap<>(Node.ROUTING_TABLE_FOR_DEMO[identification]);
        this.FloodingTable = new HashMap<>();
        this.port = id + NODE_PORT_OFFSET;
        this.identification = id;
    }

    public void updateAdjacentNodesTable(Vector<Vector<Integer>> adjacentNodesTable){
        // TODO
    }

    private void reportToNeighbours(){
        // TODO
        // send ping to all neighbours that we are alive and well
    }

    private void checkNeighbours(){
        // TODO
        // look what links haven't spoken with us in a while
    }

    @Override
    public void run() {
        System.out.println(port);

        // activate writing thread
        nodeWriter = new NodeWriter(this.identification, this.socketTable, this.routingTable);
        nodeWriter.start();

        // activate reading thread
        reader = new NodeReader(this.port, this.identification, this.socketTable, this.nodeWriter);
        reader.start();

        // this part is terrible hacking because i am not sure how can we
        // guarantee that we can connect to socket in adjacentNodesTable
        // we wait for 1 second and all the sockets SHOULD be active

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        adjacentNodesTable.forEach((p, l) -> {
            try {
                System.out.println("Node" + identification + " connecting to:" + p);
                Socket s = new Socket("localhost", p);
                socketTable.put(p - Node.NODE_PORT_OFFSET, s);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        System.out.println("routing table for " + identification + " " + routingTable);

        System.out.println("adjacency table for " + identification + " " + adjacentNodesTable);



//        while(true){
//            // periodically tell our dear neighbours we are alive and well
//            // also while we are at it check for their health
//            try {
//                Thread.sleep(REPORT_TIME);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            reportToNeighbours();
//            checkNeighbours();
//        }
    }


    public void floodNeighbors(Message message){
        Integer source = message.getSource();
        Integer messageId = message.getFloodingId();

        //Do we need to flood the message
        if (!FloodingTable.containsKey(messageId) || !FloodingTable.get(messageId).contains(source)){
            //flooding the message
            for (Map.Entry<Integer, Integer> entry : adjacentNodesTable.entrySet()) {
                Integer neighbor = entry.getKey();
                Integer distance = entry.getValue();
                try {
                    nodeWriter.getWritingBuffer().put(message);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //remembering the message
            if(!FloodingTable.containsKey(messageId))
                FloodingTable.put(messageId, new Vector<Integer>(source));
            else
                FloodingTable.get(messageId).add(source);
        }
        //else
        // we've seen the message so we don't have to flood it again
    }



}
