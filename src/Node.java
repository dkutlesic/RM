import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public abstract class Node extends Thread{
    //statistics
    private long sentPacketsNumber;
    private long receivedPacketsNumber;
    private long transmissionsNumber;

    public NodeWriter getNodeWriter() {
        return nodeWriter;
    }

    //tables
    protected Map<Integer, Integer> adjacentNodesTable;
    protected Map<Integer, Socket> socketTable;
    protected Map<Integer, Integer> routingTable;

    //utils
    public static final int NODE_PORT_OFFSET = 1234; // node i will be connected to port_offset + i port
    private int port;
    protected int identification;
    private static final long REPORT_TIME = 1000;
    private Set<Integer> liveNeighbors; // FIXME every time we receive ping message, add message source to the liveNeighbors set
    public static Map<Integer, Integer>[] ROUTING_TABLE_FOR_DEMO;  // hopefully not needed

    //IO
    private NodeWriter nodeWriter;
    private NodeReader reader;
    protected Map<Integer, PrintWriter> outStreams = new HashMap<>();

    // handler for routing messages


    public Node(Map<Integer, Integer> adjacentNodesTable, int id) {
        this.sentPacketsNumber = 0;
        this.receivedPacketsNumber = 0;
        this.transmissionsNumber = 0;
        this.adjacentNodesTable = new HashMap<>(adjacentNodesTable);
        this.socketTable = new HashMap<>();
        this.routingTable = new HashMap<>(Node.ROUTING_TABLE_FOR_DEMO[identification]);
        this.liveNeighbors = new HashSet<>();
        this.port = id + NODE_PORT_OFFSET;
        this.identification = id;
    }

    abstract void handleRoutingMessage(Message message);


    public void checkNeighbours(PrintWriter out){
        // look what links haven't spoken with us in a while
        Set<Integer> deadNeighbors = new HashSet<>();
        deadNeighbors.addAll(adjacentNodesTable.keySet());
        deadNeighbors.removeAll(liveNeighbors);
        if(!deadNeighbors.isEmpty()){
            System.err.println(this.identification);
           out.println("========================================");
           out.println("Dead nodes: " + deadNeighbors);
           out.println("========================================\nBefore:");
        }
        for(Integer deadRoute : deadNeighbors){
            routingTable.remove(deadRoute);
           for(Map.Entry<Integer, Integer> e : routingTable.entrySet()){
               if(e.getValue() == deadRoute){
                   e.setValue(-1);
               }
           }
        }
        adjacentNodesTable.keySet().removeIf(k -> deadNeighbors.contains(k));
        socketTable.keySet().removeIf(k -> deadNeighbors.contains(k + NODE_PORT_OFFSET));
    }

    protected void reportToNeighbours(PrintWriter out){
        // send ping to all neighbours that we are alive and well
        PingMessage pingMessage = new PingMessage(this.identification);
        try {
            out.println("========================================\nBefore:");
            out.println(pingMessage);
            out.println("After:");
            out.println(Message.parseMessage(pingMessage.sendingFormat()));
            out.flush();
            nodeWriter.getWritingBuffer().put(pingMessage);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        out.flush();
    }


    @Override
    public void run() {
        try(PrintWriter out = new PrintWriter(
                new OutputStreamWriter(
                        new FileOutputStream(
                            "logs/" + identification + ".txt"
                        )
                )
        )) {

            out.println(port);

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

            adjacentNodesTable.forEach((p, l) -> {
                try {
                    out.println("Node" + identification + " connecting to:" + p);
                    Socket s = new Socket("localhost", p);
                    socketTable.put(p - Node.NODE_PORT_OFFSET, s);
                    outStreams.put(p - Node.NODE_PORT_OFFSET, new PrintWriter(s.getOutputStream()));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            out.println("routing table for " + identification + " " + routingTable);

            out.println("adjacency table for " + identification + " " + adjacentNodesTable);

            out.flush();

            int cycle = 1;
            while(true){
                // periodically tell our dear neighbours we are alive and well
                // also while we are at it check for their health
                try {
                    Thread.sleep(REPORT_TIME);
                    cycle++;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                reportToNeighbours(out);
                if (cycle % 10 == 0) {
                    checkNeighbours(out);
                    cycle = 0;
                    liveNeighbors.removeAll(liveNeighbors);
                }

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


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
                nodeWriter.getWritingBuffer().put(message); //FIXME this could block should be fixed, we dont want it
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected class NodeWriter extends Thread{

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

        private void floodMessage(FloodingMessage message){
            //FIXME
            // think that we need two ids to identify message uniqueness
            // one for node id of the message source
            // and the other for messageId
            // when the node wants to flood new message, it doesn't know which messageIds are not used so far
            // second option: to statically increment messageId for all nodes, but that is bljaksi
            if(! floodingMessages.contains(message.getFloodingId())){
                // we never sent (or seen for that matter) this message

                floodingMessages.add(message.getFloodingId());

                FloodingMessage forwardingMessage = message.copyForSending(this.identification);

                adjacentNodesTable.forEach((router_id, length) -> {
                    if(router_id != message.getSource()){
                        if(socketTable.containsKey(router_id - Node.NODE_PORT_OFFSET)){
                            PrintWriter outWriter = outStreams.get(router_id - NODE_PORT_OFFSET);
                            outWriter.write(forwardingMessage.sendingFormat());
                            outWriter.flush();
                        }
                        else{
                            System.err.println("UNKNOWN SOCKET FOR NODE " + router_id);
                        }
                    }
                });

                if(message instanceof FloodingNewConnection){
                    int source = message.getOriginalSender();
                    int newHost = ((FloodingNewConnection) message).getNewHostId();
                    // update routing table
                }
                else if(message instanceof FloodingTopologyMessage){
                    Map<Integer, Integer> senderAdjacentNodesTable = ((FloodingTopologyMessage) message).getAdjacentNodesTable();
                    int sender = message.getOriginalSender();
                    // update topology
                }
            }
        }

        private void makeConnection(int port, int id) {
            try {
                Socket socket = new Socket("localhost", port);
                socketTable.put(id, socket);
                outStreams.put(id, new PrintWriter(socket.getOutputStream()));

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
                    // in outStreams table we can get output-stream for destination
                    PrintWriter outWriter = outStreams.get(message.getDestination());
                    outWriter.print(message.sendingFormat());
                    outWriter.flush();
                }
                else
                {
                    // here we know that routing table entry is pointing us toward next router
                    // socket for next router is located in socketTable
                    PrintWriter outWriter = outStreams.get(routingTable.get(message.getDestination()));
                    outWriter.print(message.sendingFormat());
                    outWriter.flush();
                }
            }
            else{
                // cry for help
                System.err.println("Not supported destination");
                System.err.println("Putting message back ");
            }
        }

        public void pingMessage(PingMessage pingMessage) {
            if(pingMessage.getSource() == this.identification) {
                // we are sending ping message
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
            else {
                liveNeighbors.add(pingMessage.getSource() + NODE_PORT_OFFSET);
            }

        }


    }
}