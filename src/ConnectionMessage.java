public class ConnectionMessage extends Message {
    private int port;
    private int id;
    @Override
    public types getType() {
        return types.CONNECTION_MESSAGE;
    }

    public int getPort() {
        return port;
    }

    public int getId() {
        return id;
    }

    public ConnectionMessage(int port, int id) {
        this.port = port;
        this.id = id;
    }

    @Override
    public String toString() {
        return "ConnectionMessage{" +
                "port=" + port +
                ", id=" + id +
                '}';
    }
}
