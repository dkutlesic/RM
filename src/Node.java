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
import java.util.function.ToDoubleBiFunction;

public class Node extends Thread{

    private class Reader extends Thread{
        Selector selector;
        ServerSocketChannel serverSocketChannel;

        public Reader() {
            // selector to deal with multiple input channels
        }

        @Override
        public void run() {
            try{
                serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.bind(new InetSocketAddress(port));
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

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
                            client.configureBlocking(false);
                            client.register(selector, SelectionKey.OP_READ);
                        } else if (key.isReadable()) {
                            SocketChannel client = (SocketChannel) key.channel();

                            ByteBuffer byteBuffer = (ByteBuffer) key.attachment();
                            if (byteBuffer == null) {
                                byteBuffer = ByteBuffer.allocate(4096);
                                key.attach(byteBuffer);
                            }
                            client.read(byteBuffer);
                            String requestCompleted = new String(byteBuffer.array());
                            if (requestCompleted.contains("!")) {
                                Message message = Message.parseMessage(requestCompleted);
                                System.out.println(identification + " : " + message.toString());
                                key.cancel(); //hacky, maybe change it

                            } else
                                System.err.println("Not supported message type");
                        } else
                            System.err.println("Not supported key action!");
                    }
                }
            } catch (IOException e) {
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
                try {
                    // destination = port (for now)
                    PrintWriter out = new PrintWriter(routingTable.get(message.getDestination()).getOutputStream(), true);
                    out.write(message.toString());

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
    private Vector<Vector<Integer>> adjacentNodesTable;
    private Map<Integer, Socket> routingTable; // hack to get first thing working
                                                            // this should be done smarter

    //utils
    private int port;
    private int identification;
    private static long REPORT_TIME = 100;
    private static int BUFF_SIZE = 10;

    public Node(Vector<Vector<Integer>> adjacentNodesTable) {
        this.sentPacketsNumber = 0;
        this.receivedPacketsNumber = 0;
        this.transmissionsNumber = 0;
        this.adjacentNodesTable = new Vector<>(adjacentNodesTable);
        this.routingTable = new HashMap<>();
    }

    public void updateAdjacentNodesTable(Vector<Vector<Integer>> adjacentNodesTable){
        // TODO
    }

    private void reportToNeighbours(){
        // TODO
        // send ping to all neighbours that we are alive and well
    }

    private void chechkNeighbours(){
        // TODO
        // look what links haven't spoken with us in a while
    }

    @Override
    public void run() {
        // activate writing thread
        Writer writer = this.new Writer();
        writer.start();

        // activate reading thread
        try (Selector selector = Selector.open()){
            Reader reader = new Reader();
            reader.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        while(true){
            // periodically tell our dear neighbours we are alive and well
            // also while we are at it check for their health
            try {
                Thread.sleep(REPORT_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            reportToNeighbours();
            chechkNeighbours();
        }
    }
}
