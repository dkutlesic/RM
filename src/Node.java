import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Node extends Thread{

    private class Reader extends Thread{
        ServerSocketChannel serverSocketChannel;

        public Reader() {
            // selector to deal with multiple input channels
        }

        @Override
        public void run() {
            try(Selector selector = Selector.open()){
                serverSocketChannel = ServerSocketChannel.open();
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

                            // treba zapamtiti klijenta da mozemo da mu prosledjujemo poruke kad treba
                            SocketChannel client = server.accept();
                            System.out.println(identification + ": Accepted connection");
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
                            String requestCompleted = new String(byteBuffer.array(), 0, bytes_read);
                            if (requestCompleted.contains("!")) {
                                Message message = Message.parseMessage(requestCompleted);

                                writer.getWritingBuffer().put(message);

                                System.out.println(identification + " : " + message.toString());
                                key.cancel(); //hacky, maybe change it

                            } else{
                                System.err.println("Not supported message type");
                            }
                        } else{
//                            System.err.println("Not supported key action!");
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

        void forwardMessage(Message message){
            if(routingTable.containsKey(message.getDestination())){
                // we know where to send
                try (PrintWriter out = new PrintWriter(
                        routingTable.get(message.getDestination()).getOutputStream()
                )){
                    out.print(message.sendingFormat());
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
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
    private Map<Integer, Socket> routingTable;

    //utils
    public static int NODE_PORT_OFFSET = 1234; // node i will be connected to port_offset + i port
    private int port;
    private int identification;
    private static long REPORT_TIME = 100;
    private static int BUFF_SIZE = 10;

    public static Map<Integer, Integer>[] ROUTING_TABLE_FOR_DEMO;

    private Writer writer;
    private Reader reader;

    public Node(Map<Integer, Integer> adjacentNodesTable, int id) {
        this.sentPacketsNumber = 0;
        this.receivedPacketsNumber = 0;
        this.transmissionsNumber = 0;
        this.adjacentNodesTable = new HashMap<>(adjacentNodesTable);
        this.routingTable = new HashMap<>();
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
                System.out.println("connecting to:" + p);
                Socket s = new Socket("localhost", p);
                // this socket should be put in routing table, cant find good way to do it for testing
//                routingTable.put(Node.ROUTING_TABLE_FOR_DEMO[identification].get(p - Node.NODE_PORT_OFFSET) , s);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });



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
}
