import java.net.Socket;
import java.util.Map;

public class HostReader extends Reader{
    public HostReader(int port, int identification, Map<Integer, Socket> socketTable) {
        super(port, identification, socketTable);
    }

    @Override
    public void processMessage(Message message) {
        System.out.println(message);
    }
}
