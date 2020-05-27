public class FloodingNewConnection extends FloodingMessage{
    private int newHostId;

    public FloodingNewConnection(int source, int id, int originalSender, int newHostId) {
        super(source, id, originalSender);
        this.newHostId = newHostId;
    }

    @Override
    public FloodingMessage copyForSending(int source) {
        return new FloodingNewConnection(source, id, originalSender, newHostId);
    }

    public int getNewHostId() {
        return newHostId;
    }
}
