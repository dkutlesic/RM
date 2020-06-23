/**
 * One node sends a ping message to a neighbor node.
 * All nodes occasionally send ping messages to their neighbors reporting that they are alive and well.
 */
public class PingMessage extends Message{
    /**
     * Identification of a node that sends the ping message
     */
    private int source;

    public PingMessage(int source) {
        this.source = source;
    }

    public int getSource() {
        return source;
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
