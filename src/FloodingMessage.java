import java.util.Map;
import java.util.Vector;

public class FloodingMessage extends Message {
    
    private int source; // direct sender
    private int id; // id of message
    private Map<Integer, Integer> adjacentNodesTable; // adjacentNodesTable from original sender
    private int originalSender;



    public FloodingMessage(int source, int id, int originalSender, Map<Integer, Integer> adjacentNodesTable){
        this.source = source;
        this.id = id;
        this.originalSender = originalSender;
        this.adjacentNodesTable = adjacentNodesTable;
    }

    public int getSource(){ return source; }
    public int getOriginalSender() {
        return originalSender;
    }
    public Map<Integer, Integer> getAdjacentNodesTable() {
        return adjacentNodesTable;
    }
    public int getFloodingId(){
        return id;
    }

    public FloodingMessage copyForSending(int source){
        return new FloodingMessage(source, id, originalSender,adjacentNodesTable);
    }

    @Override
    public types getType() {
        return types.FLOODING_MESSAGE;
    }

    @Override
    public String toString() {
        return "FloodingMessage{" +
                "source=" + source +
                ", id=" + id +
                ", adjacentNodesTable=" + adjacentNodesTable +
                '}';
    }
}
