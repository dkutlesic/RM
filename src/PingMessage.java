public class PingMessage extends Message{
    public int getSource() {
        return source;
    }

    private int source;

    public PingMessage(int source) {
        this.source = source;
    }

    @Override
    public types getType() {
        return types.PING_MESSAGE;
    }

    @Override
    public String toString() {
        return "PingMessage{" +
                "source=" + source +
                '}';
    }
}
