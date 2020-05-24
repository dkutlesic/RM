import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class NodeWriter extends Thread{

    private static final int BUFF_SIZE = 10;

    public int identification;
    private Map<Integer, Socket> socketTable;
    private Map<Integer, Integer> routingTable;
    BlockingQueue<Message> writingBuffer;


    public NodeWriter(int identification, Map<Integer, Socket> socketTable, Map<Integer, Integer> routingTable) {
        this.identification = identification;
        this.socketTable = socketTable;
        this.routingTable = routingTable;
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
                        break;
                    case CONNECTION_MESSAGE:
                        makeConnection(((ConnectionMessage) nextMessage).getPort(),
                                ((ConnectionMessage) nextMessage).getId());
                        break;
                    default:
                        System.err.println("UNSUPPORTED MESSAGE TYPE");
                }
            }
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
                // host is directly connected to us
                System.out.println(message.getDestination());
                System.out.println(socketTable.get(message.getDestination()));
                try (PrintWriter out = new PrintWriter(
                        socketTable.get(message.getDestination()).getOutputStream()
                )){

                    System.out.println(identification + "|" + message + "| forwarded to: " + routingTable.get(message.getDestination()) );
                    System.out.println("sending to socket " + socketTable.get(routingTable.get(message.getDestination())));
                    out.print(message.sendingFormat());
                    out.flush();
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
