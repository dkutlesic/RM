import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Node extends Thread{

    private class Reader extends Thread{

        public Reader() {
            // selector to deal with multiple input channels
        }

        @Override
        public void run() {
            try(Selector selector = Selector.open();
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()){

                if(selector == null || serverSocketChannel == null){
                    System.err.println("well what can you do :/");
                }

                serverSocketChannel.bind(new InetSocketAddress(port));
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT );

                while(true) {
                    selector.select();
                    Iterator<SelectionKey> selectionKeyIterator = selector.selectedKeys().iterator();
                    while (selectionKeyIterator.hasNext()) {
                        SelectionKey key = selectionKeyIterator.next();
                        selectionKeyIterator.remove();

                        if (key.isAcceptable()) {

                            ServerSocketChannel server = (ServerSocketChannel) key.channel();

                            SocketChannel client = server.accept();

                            System.out.println(identification + ": Accepted connection");

                            socketTable.put(identification, client.socket());

                            client.configureBlocking(false);
                            client.register(selector, SelectionKey.OP_READ);

                        } else if (key.isReadable()) {
                            SocketChannel client = (SocketChannel) key.channel();

                            ByteBuffer byteBuffer = (ByteBuffer) key.attachment();
                            if (byteBuffer == null) {
                                byteBuffer = ByteBuffer.allocate(4096);
                                key.attach(byteBuffer);
                            }

                            int bytes_read = client.read(byteBuffer);
                            if(bytes_read == -1){
                                byteBuffer.clear();
                                continue;
                            }
                            String requestCompleted = new String(byteBuffer.array(), 0, bytes_read);
                            if (requestCompleted.contains("!")) {
                                Message message = Message.parseMessage(requestCompleted);

                                writer.getWritingBuffer().put(message); // this could block should be fixed, we dont want it

                                System.out.println(identification + " (line 72): " + message.toString());


                            } else {
                                System.err.println("Not supported message type");
                            }

                            // enable us to read again
//                            byteBuffer.clear(); // makes hell break loose
                            key.cancel();  // should delete this
                        } else{
                            System.err.println("Not supported key action!");
                        }
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void readFromKey(SelectionKey key) {
            SocketChannel socketChannel = (SocketChannel) key.channel();

        }
    }

    private class Writer extends Thread{


        BlockingQueue<Message> writingBuffer;

        public Writer() {
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
                else
                    forwardMessage(nextMessage);
            }
        }

        private void forwardMessage(Message message){
            if(routingTable.containsKey(message.getDestination())){
                // we know where to send
                if(routingTable.get(message.getDestination()) == identification){
                    // host is directly connected to us
                    try (PrintWriter out = new PrintWriter(
                            socketTable.get(
                                    routingTable.get(message.getDestination())
                            ).getOutputStream()
                    )){

                        System.out.println(identification + "|" + message + "| forwarded to: " + routingTable.get(message.getDestination()) );
                        System.out.println("sending to socket " + socketTable.get(routingTable.get(message.getDestination())));
                        out.print(message.sendingFormat());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else
                {
                    try (PrintWriter out = new PrintWriter(
                            socketTable.get(
                                    routingTable.get(message.getDestination())
                            ).getOutputStream()
                    )){

                        System.out.println(identification + "|" + message + "| forwarded to: " + routingTable.get(message.getDestination()) );
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
    private static final int BUFF_SIZE = 10;

    public static Map<Integer, Integer>[] ROUTING_TABLE_FOR_DEMO;

    private Writer writer;
    private Reader reader;

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
        writer = this.new Writer();
        writer.start();

        // activate reading thread
        reader = new Reader();
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
                    writer.getWritingBuffer().put(message);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //remembering the message
            if(!FloodingTable.containsKey(messageId))
                FloodingTable.put(messageId, new Vector<>(source));
            else
                FloodingTable.get(messageId).add(source);
        }
        //else
        // we've seen the message so we don't have to flood it again
    }



}
