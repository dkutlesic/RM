import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Node class is the crucial class for routing
 * Node represents a router in the network
 */
public abstract class Node extends Thread{
    //statistics
    private long sentPacketsNumber;
    private long receivedPacketsNumber;
    private long transmissionsNumber;

    //tables
    /**
     * (identification of a neighbor, distance to the neighbor)
     */
    protected Map<Integer, Integer> adjacentNodesTable;
    /**
     * (identification of a neighbor, socket for the neighbor)
     */
    protected Map<Integer, Socket> socketTable;
    /**
     * (identification of a targeted receiver, identification of the next hop)
     */
    protected Map<Integer, Integer> routingTable;

    //utils
    /**
     * The node will be connected to NODE_PORT_OFFSET + port
     */
    public static final int NODE_PORT_OFFSET = 1234;
    private int port;
    protected int identification;
    private static final long REPORT_TIME = 100;
    /**
     * The set of a identification of neighbors that sent ping message in the last cycle
     */
    private Set<Integer> liveNeighbors;
    public static Map<Integer, Integer>[] ROUTING_TABLE_FOR_DEMO;  // hopefully not needed

    //locksTrue
    protected ReentrantLock adjacentNodesTableLock;
    protected ReentrantLock socketTableLock;
    protected ReentrantLock routingTableLock;

    //logging
    protected PrintWriter log;

    //IO
    private NodeWriter nodeWriter;
    private NodeReader reader;
    /**
     * (identification of the neighbor, the output stream of a neighbor)
     */
    protected Map<Integer, PrintWriter> outStreams = new HashMap<>();

    /**
     * The stream for logs/messages_forgoten.txt
     */
    private static PrintWriter placeholderStream;

    static {
        try {
            placeholderStream = new PrintWriter(
                        new FileOutputStream("logs/messages_forgoten.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public NodeWriter getNodeWriter() {
        return nodeWriter;
    }

    public Node(Map<Integer, Integer> adjacentNodesTable, int id) {
        this.sentPacketsNumber = 0;
        this.receivedPacketsNumber = 0;
        this.transmissionsNumber = 0;
        this.adjacentNodesTable = new HashMap<>(adjacentNodesTable);
        this.socketTable = new HashMap<>();
        //TODO commenting this out for testing LSR node
        //this.routingTable = new HashMap<>(Node.ROUTING_TABLE_FOR_DEMO[identification]);
        this.liveNeighbors = new HashSet<>();
        this.port = id + NODE_PORT_OFFSET;
        this.identification = id;
        this.adjacentNodesTableLock = new ReentrantLock();
        this.socketTableLock = new ReentrantLock();
        this.routingTableLock = new ReentrantLock();
        try {
            this.log = new PrintWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(
                                    "logs/" + identification + ".txt"
                            )
                    )
            );
        } catch (FileNotFoundException e) {
            System.err.println("NOT GONNA HAPPEN");
        }
    }

    /**
     * Handling routing message is implemented differently for different types of routing algorithms (types of nodes)
     * @param message
     */
    abstract void handleRoutingMessage(Message message);

    /**
     * Updating routing table (cleaning up dead routes)
     * @param deadNeighbors
     */
    abstract void cleanupDeadRouts(Set<Integer> deadNeighbors);

    /**
     * Select dead neighbors and clean up dead routes
     * Update adjacency table (removing dead neighbors)
     */
    public void checkNeighbours(){
        // look what links haven't spoken with us in a while
        Set<Integer> deadNeighbors = new HashSet<>();
        adjacentNodesTableLock.lock();
        try {
            deadNeighbors.addAll(adjacentNodesTable.keySet());
        }
        finally {
            adjacentNodesTableLock.unlock();
        }
        deadNeighbors.removeAll(liveNeighbors);
        if(!deadNeighbors.isEmpty()){
            System.err.println(this.identification);
            log.println("========================================");
            log.println("Dead nodes: " + deadNeighbors);
            log.println("========================================");
        }

        cleanupDeadRouts(deadNeighbors);

        adjacentNodesTableLock.lock();
        try {
            adjacentNodesTable.keySet().removeIf(k -> deadNeighbors.contains(k));
        }
        finally {
            adjacentNodesTableLock.unlock();
        }

        socketTableLock.lock();
        try{
            socketTable.keySet().removeIf(k -> deadNeighbors.contains(k + NODE_PORT_OFFSET));
        }
        finally {
            socketTableLock.unlock();
        }
    }

    /**
     * send ping to all neighbours that we are alive and well
     */
    protected void reportToNeighbours(){
        PingMessage pingMessage = new PingMessage(this.identification);
        try {
//            out.println(java.time.LocalDate.now() + ": " + pingMessage);
//            out.flush();
            nodeWriter.getWritingBuffer().put(pingMessage);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.flush();
    }

    /**
     *  deletes edge towards node with ID nodeID
     * @param nodeID
     */
    public void deleteEdge(int nodeID){

        socketTableLock.lock();
        try {
            socketTable.get(nodeID).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        outStreams.put(nodeID, placeholderStream);
        socketTableLock.unlock();
    }

    /**
     * Node thread
     */
    @Override
    public void run() {
        log.println(port);

        // activate writing thread
        nodeWriter = new NodeWriter(this. identification);
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
        adjacentNodesTableLock.lock();
        try {
            adjacentNodesTable.forEach((p, l) -> {
                try {
                    log.println("Node" + identification + " connecting to:" + p);
                    Socket s = new Socket("localhost", p);
                    socketTableLock.lock();
                    try {
                        socketTable.put(p - Node.NODE_PORT_OFFSET, s);
                    } finally {
                        socketTableLock.unlock();
                    }
                    outStreams.put(p - Node.NODE_PORT_OFFSET, new PrintWriter(s.getOutputStream()));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        finally {
            adjacentNodesTableLock.unlock();
        }

        routingTableLock.lock();
        try {
            log.println("routing table for " + identification + " " + routingTable);
        }
        finally {
            routingTableLock.unlock();
        }

        adjacentNodesTableLock.lock();
        try {
            log.println("adjacency table for " + identification + " " + adjacentNodesTable);
        }
        finally {
            adjacentNodesTableLock.unlock();
        }

        log.flush();

        int cycle = 1;
        while(true){
            // periodically tell our dear neighbours we are alive and well
            // also while we are at it check for their health
            log.println(java.time.LocalDate.now() + ": " + "routing table = " + routingTable);
            log.println(java.time.LocalDate.now() + ": " + "adjacentNodes table = " + adjacentNodesTable);
            log.flush();
            try {
                Thread.sleep(REPORT_TIME);
                cycle++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            reportToNeighbours();
            if (cycle % 10 == 0) {
                checkNeighbours();
                cycle = 0;
                liveNeighbors.removeAll(liveNeighbors);
            }

            //for LSRnode: change phase if needed
            if(this instanceof LSRNode) {
                if (cycle  == 3) {
                    ((LSRNode) this).phase = LSRNode.Phase.FLOODING;
                }
                else{
                    ((LSRNode) this).runDijkstra();
                    ((LSRNode) this).phase = LSRNode.Phase.DIJKSTRA;
                }
            }
        }

    }

    /**
     * Reader thread specialized for a node
     */
    private class NodeReader extends Reader {

        private NodeWriter nodeWriter;

        public NodeReader(int port, int identification,  NodeWriter nodeWriter) {
            super(port, identification, socketTable);
            this.nodeWriter = nodeWriter;
        }

        /**
         * Put message to Writing Buffer queue
         * @param message
         */
        @Override
        public void processMessage(Message message) {

            try {
                nodeWriter.getWritingBuffer().put(message); //FIXME this could block should be fixed, we dont want it
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Writer thread specialized for node
     */
    protected class NodeWriter extends Thread{

        /**
         * The size of the buffer
         */
        private static final int BUFF_SIZE = 10;

        public int identification;
        /**
         * Messages that need to be forwarded
         */
        private BlockingQueue<Message> writingBuffer;
        private Set<Integer> floodingMessages = new HashSet<>();

        public NodeWriter(int identification) {
            this.identification = identification;
            writingBuffer = new ArrayBlockingQueue<>(BUFF_SIZE);
        }

        public BlockingQueue<Message> getWritingBuffer() {
            return writingBuffer;
        }

        /**
         * forever: take next message from the queue and forward it
         */
        @Override
        public void run() {
            while(true){
                Message nextMessage = null;
                try {
                    // take next message to the buffer and write it to the stream
                    // It will block if the buffer is empty
                    nextMessage = writingBuffer.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(nextMessage == null)
                    System.err.println("Message is null");
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
                        case ROUTING_MESSAGE:
                            handleRoutingMessage(nextMessage);
                            break;
                        case PING_MESSAGE:
                            pingMessage((PingMessage) nextMessage);
                            break;
                        default:
                            System.err.println("UNSUPPORTED MESSAGE TYPE");
                    }
                }
            }
        }

        /**
         * Flood the message to neighbors
         * @param message
         */
        private void floodMessage(FloodingMessage message){
            if(! floodingMessages.contains(message.getFloodingId())){
                // this message has not been seen (or seen for flooding)

                floodingMessages.add(message.getFloodingId());

                FloodingMessage forwardingMessage = message.copyForSending(this.identification);
                adjacentNodesTableLock.lock();
                try {
                    adjacentNodesTable.forEach((router_id, length) -> {
                        if (router_id != message.getSource()) {
                            socketTableLock.lock();
                            try {
                                if (socketTable.containsKey(router_id - Node.NODE_PORT_OFFSET)) {
                                    PrintWriter outWriter = outStreams.get(router_id - NODE_PORT_OFFSET);
                                    outWriter.write(forwardingMessage.sendingFormat());
                                    outWriter.flush();
                                } else {
                                    System.err.println("UNKNOWN SOCKET FOR NODE " + router_id);
                                }
                            } finally {
                                socketTableLock.unlock();
                            }
                        }
                    });
                }
                finally {
                    adjacentNodesTableLock.unlock();
                }

                if(message instanceof FloodingNewConnectionMessage){
                    int source = message.getOriginalSender();
                    int newHost = ((FloodingNewConnectionMessage) message).getNewHostId();
                    //FIXME update routing table
                }
                else if(message instanceof FloodingTopologyMessage){
                    Map<Integer, Integer> senderAdjacentNodesTable = ((FloodingTopologyMessage) message).getAdjacentNodesTable();
                    int sender = message.getOriginalSender();
                    //FIXME update topology
                }
            }
        }

        /**
         * adding new connection
         * updating socket table, output streams table
         * @param port
         * @param id identification
         */
        private void makeConnection(int port, int id) {
            try {
                Socket socket = new Socket("localhost", port);
                socketTableLock.lock();
                try {
                    socketTable.put(id, socket);
                }
                finally {
                    socketTableLock.unlock();
                }
                outStreams.put(id, new PrintWriter(socket.getOutputStream()));

                socketTableLock.lock();
                try {
                    System.out.println("connected to socket" + socketTable.get(id) + " for id " + id);
                }
                finally {
                    socketTableLock.unlock();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * forwarding the message depending on the routing table
         * @param message
         */
        private void forwardMessage(TextMessage message){
            routingTableLock.lock();
            try{
                if(routingTable.containsKey(message.getDestination())){
                    // it is known where the message should be sent
                    if(routingTable.get(message.getDestination()) == identification){
                        // host is directly connected to this node if routing table point to this node
                        // in outStreams table - output stream of a destination
                        PrintWriter outWriter = outStreams.get(message.getDestination());
                        outWriter.print(message.sendingFormat());
                        outWriter.flush();
                    }
                    else
                    {
                        if(routingTable.get(message.getDestination()) == -1){
                            // it means that destination got changed up and the routing need to be finished
                            try {
                                writingBuffer.put(message);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        else{
                            // Routing table entry is pointing us toward next router
                            // The socket for next router is located in socketTable
                            PrintWriter outWriter = outStreams.get(routingTable.get(message.getDestination()));
                            outWriter.print(message.sendingFormat());
                            outWriter.flush();
                        }
                    }
                }
                else{
                    // cry for help
                    System.err.println("Not supported destination");
                    System.err.println("Putting message back ");
                }
            }
            finally {
                routingTableLock.unlock();
            }
        }

        /**
         * Sending the ping message
         * @param pingMessage
         */
        public void pingMessage(PingMessage pingMessage) {
            if(pingMessage.getSource() == this.identification) {
                adjacentNodesTableLock.lock();
                try {
                    adjacentNodesTable.forEach((neighbour, length) -> {
                        if (outStreams.containsKey(neighbour - Node.NODE_PORT_OFFSET)) {
                            PrintWriter outWriter = outStreams.get(neighbour - NODE_PORT_OFFSET);
                            outWriter.write(pingMessage.sendingFormat());
                            outWriter.flush();
                        } else {
                            System.err.println("UNKNOWN SOCKET FOR NODE " + neighbour);
                        }
                    });
                }
                finally {
                    adjacentNodesTableLock.unlock();
                }
            }
            else {
                liveNeighbors.add(pingMessage.getSource() + NODE_PORT_OFFSET);
            }

        }


    }
}