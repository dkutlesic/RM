import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

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
                //TODO DON'T LET IT STAY LIKE THIS
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
                            // this occurs only if stream behind socket channel is closed
                            throw new IOException("Stream is kaput");
                        }

                        if(bytes_read >= byteBuffer.capacity()){
                            // this will happen only if we red more than we have reserved
                            // we can implement stacking buffers but it is not needed for this application
                            // sizes are rarely bigger then 200 bytes
                            throw new IOException("BUFFER OVERFLOW");
                        }

                        String requestCompleted = new String(byteBuffer.array(), 0, bytes_read);

                        if (requestCompleted.endsWith("!")) {
                            // this will tell us that message is done sending and that we can process it
                            String[] messages = requestCompleted.split("!");
                            Stream.of(messages).map(m -> {return m + '!';}).map(Message::parseMessage).forEach(this::processMessage);

                            // clearing buffer because we want to read into it again!
                            byteBuffer.clear();
                        }
                    } else{
                        System.err.println("Not supported key action!");
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    abstract public void processMessage(Message message);

    private void readFromKey(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();

    }
}

