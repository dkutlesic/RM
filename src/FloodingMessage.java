import java.util.Map;
import java.util.Vector;

public abstract class FloodingMessage extends Message {
    
    protected int source; // direct sender
    protected int id; // id of message
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
