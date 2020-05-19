import java.io.*;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.ToDoubleBiFunction;

public class Node extends Thread{

    private class Reader extends Thread{
        Selector selector;

        public Reader(Selector selector) {
            // selector to deal with multiple input channels
            this.selector = selector;
        }

        @Override
        public void run() {
            while(true){
                try {
                    selector.select();

                    Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                    while(keyIterator.hasNext()){
                        SelectionKey key = keyIterator.next();
                        if(key.isReadable()){
                            readFromKey(key);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        private void readFromKey(SelectionKey key) {
            SocketChannel socketChannel = (SocketChannel) key.channel();

        }
    }

    private class Writer extends Thread{


        BlockingQueue<Message> writing_buffer;

        public Writer() {
            writing_buffer = new ArrayBlockingQueue<>(BUFF_SIZE);
        }

        @Override
        public void run() {
            while(true){
                Message next_message = null;
                try {
                    // take next msg in buffer and write it
                    //this call will block if buffer is empty
                    next_message = writing_buffer.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(next_message == null)
                    System.err.println("msg is null");
                else
                    forwardMessage(next_message);
            }
        }

        void forwardMessage(Message msg){
            if(routingTable.containsKey(msg.getDestination())){
                // we know where to send
                try {
                    routingTable.get(msg.getDestination()).writeObject(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else{
                // cry for help
            }
        }
    }

    //statistics
    private long sentPacketsNumber;
    private long receivedPacketsNumber;
    private long transmissionsNumber;

    //tables
    private Vector<Vector<Integer>> adjacentNodesTable;
    private Map<Integer, ObjectOutputStream> routingTable; // hack to get first thing working
                                                            // this should be done smarter

    //utils
    private int port;
    private int address;
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
            Reader reader = new Reader(selector);
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
