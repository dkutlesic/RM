/**
 * New Connection Flooding message is a special type of a flooding message that is used when a new host is connected
 * to a network
 */
public class FloodingNewConnectionMessage extends FloodingMessage{
    /**
     * The identification of the host
     */
    private int newHostId;

    public FloodingNewConnectionMessage(int source, int id, int originalSender, int newHostId) {
        super(source, id, originalSender);
        this.newHostId = newHostId;
    }

    public int getNewHostId() {
        return newHostId;
    }

    @Override
    public FloodingMessage copyForSending(int source) {
        return new FloodingNewConnectionMessage(source, id, originalSender, newHostId);
    }
}
