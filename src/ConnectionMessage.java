/**
 * The connection message is used for connecting a new host to a node (router)
 */
public class ConnectionMessage extends Message {

    /**
     * Port that hosts uses for a connection
     */
    private int port;

    /**
     * Id of the host
     */
    private int id;

    public ConnectionMessage(int port, int id) {
        this.port = port;
        this.id = id;
    }

    public int getPort() {
        return port;
    }
    public int getId() {
        return id;
    }

    @Override
    public types getType() {
        return types.CONNECTION_MESSAGE;
    }

    @Override
    public String toString() {
        return "ConnectionMessage{" +
                "port=" + port +
                ", id=" + id +
                '}';
    }
}
