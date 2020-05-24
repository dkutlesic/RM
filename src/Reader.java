import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;

abstract public class Reader extends Thread {

    private int port;
    private int identification;
    private Map<Integer, Socket> socketTable;

    public Reader(int port, int identification, Map<Integer, Socket> socketTable){
        System.out.println("port: " + port);
        this.port = port;
        this.identification = identification;
        this.socketTable = socketTable;
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
                        if (requestCompleted.endsWith("!")) {
                            Message message = Message.parseMessage(requestCompleted);
                            processMessage(message);

                            System.out.println(identification + " (line 72): " + message.toString());


                        } else {
                            System.err.println("Not supported message type");
                        }

                        // enable us to read again
                        byteBuffer.clear(); // makes hell break loose
//                        key.cancel();  // should delete this
                    } else{
                        System.err.println("Not supported key action!");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    abstract public void processMessage(Message message);

    private void readFromKey(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();

    }
}

