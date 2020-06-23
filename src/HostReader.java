import java.net.Socket;
import java.util.Map;

/**
 * Reader specializes for hosts
 */
public class HostReader extends Reader{
    public HostReader(int port, int identification, Map<Integer, Socket> socketTable) {
        super(port, identification, socketTable);
    }

    /**
     * Outputs the message to the output stream
     * @param message Message to be processed
     */
    @Override
    public void processMessage(Message message) {
        System.out.println(message);
    }
}
