import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Node extends Thread{
    //statistics
    private long sentPacketsNumber;
    private long receivedPacketsNumber;
    private long transmissionsNumber;

    public NodeWriter getNodeWriter() {
        return nodeWriter;
    }

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
        nodeWriter = new NodeWriter(this.identification);
        nodeWriter.start();

        // activate reading thread
        reader = new NodeReader(this.port, this.identification, this.nodeWriter);
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



    private class NodeReader extends Reader {

        private NodeWriter nodeWriter;

        public NodeReader(int port, int identification,  NodeWriter nodeWriter) {
            super(port, identification, socketTable);
            this.nodeWriter = nodeWriter;
        }

        @Override
        public void processMessage(Message message) {

            try {
                nodeWriter.getWritingBuffer().put(message); // this could block should be fixed, we dont want it
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public class NodeWriter extends Thread{

        private static final int BUFF_SIZE = 10;

        public int identification;
        private BlockingQueue<Message> writingBuffer;
        private Set<Integer> floodingMessages = new HashSet<>();


        public NodeWriter(int identification) {
            this.identification = identification;
            writingBuffer = new ArrayBlockingQueue<>(BUFF_SIZE);
        }

        public BlockingQueue<Message> getWritingBuffer() {
            return writingBuffer;
        }

        @Override
        public void run() {
            while(true){
                Message nextMessage = null;
                try {
                    // take next msg in buffer and write it
                    //this call will block if buffer is empty
                    nextMessage = writingBuffer.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(nextMessage == null)
                    System.err.println("msg is null");
                else{
                    switch (nextMessage.getType()){
                        case TEXT_MESSAGE:
                            forwardMessage((TextMessage) nextMessage);
                            break;
                        case FLOODING_MESSAGE:
                            floodMessage((FloodingMessage) nextMessage);
                            break;
                        case CONNECTION_MESSAGE:
                            makeConnection(((ConnectionMessage) nextMessage).getPort(),
                                    ((ConnectionMessage) nextMessage).getId());
                            break;
                        case DISTANCE_VECTOR_ROUTING_MESSAGE:
    //                        handle_distance_vector((DistanceVectorRoutingMessage) message);
                            break;
                        default:
                            System.err.println("UNSUPPORTED MESSAGE TYPE");
                    }
                }
            }
        }

        private void floodMessage(FloodingMessage message){
            if(! floodingMessages.contains(message.getFloodingId())){
                // we never sent (or seen for that matter) this message

                floodingMessages.add(message.getFloodingId());

                FloodingMessage forwardingMessage = message.copyForSending(this.identification);

                adjacentNodesTable.forEach((router_id, length) -> {
                    if(router_id != message.getSource()){
                        if(socketTable.containsKey(router_id - Node.NODE_PORT_OFFSET)){
                            try {
                                PrintWriter out = new PrintWriter(
                                        socketTable.get(router_id - NODE_PORT_OFFSET).getOutputStream()
                                );
                                out.write(forwardingMessage.sendingFormat());
                                out.flush();

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        else{
                            System.err.println("UNKNOWN SOCKET FOR NODE " + router_id);
                        }
                    }
                });

                Map<Integer, Integer> senderAdjacentNodesTable = message.getAdjacentNodesTable();
                int sender = message.getOriginalSender();
                // update topology
            }
        }

        private void makeConnection(int port, int id) {
            try {
                socketTable.put(id, new Socket("localhost", port));

                System.out.println("connected to socket" + socketTable.get(id) + " for id " + id);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void forwardMessage(TextMessage message){
            if(routingTable.containsKey(message.getDestination())){
                // we know where to send
                if(routingTable.get(message.getDestination()) == identification){
                    // host is directly connected to us if routing table point to us
                    try {
                        // in socketTable we can get socket connected to destination
                        PrintWriter out = new PrintWriter(socketTable.get(message.getDestination()).getOutputStream());
                        out.print(message.sendingFormat());
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else
                {
                    // here we know that routing table entry is pointing us toward next router
                    // socket for next router is located in socketTable
                    try {
                         PrintWriter out = new PrintWriter(
                                 socketTable.get(routingTable.get(message.getDestination())).getOutputStream()
                            );
                        out.print(message.sendingFormat());
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else{
                // cry for help
                System.err.println("Not supported destination");
            }
        }
    }
}