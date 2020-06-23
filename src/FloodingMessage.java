import java.util.Map;
import java.util.Vector;

/**
 * The flooding message is used to broadcast a message to all nodes in the network
 */
public abstract class FloodingMessage extends Message {

    /**
     * The identification of a direct sender (neighbor)
     */
    protected int source;
    /**
     * Id of the message
     * It is essential to know the id of the message because the node needs to know has the message been seen so far
     * If the message is new (not seen before), the node flood received message to its neighbors.
     * Otherwise, the node do not flood the message.
     */
    protected int id;
    /**
     * The identification of an original sender (a node that started the flood)
     */
    protected int originalSender;


    public FloodingMessage(int source, int id, int originalSender){
        this.source = source;
        this.id = id;
        this.originalSender = originalSender;
    }

    public int getSource(){ return source; }
    public int getOriginalSender() {
        return originalSender;
    }
    public int getFloodingId(){
        return id;
    }


    /**
     * @param source
     * @return The special type of a flooding message
     */
    public  abstract FloodingMessage copyForSending(int source);

    @Override
    public types getType() {
        return types.FLOODING_MESSAGE;
    }

    @Override
    public String toString() {
        return "FloodingMessage{" +
                "source=" + source +
                ", id=" + id +
                '}';
    }
}
