import java.net.Socket;
import java.util.Map;

public class NodeReader extends Reader {

    private NodeWriter nodeWriter;

    public NodeReader(int port, int identification, Map<Integer, Socket> socketTable, NodeWriter nodeWriter) {
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
