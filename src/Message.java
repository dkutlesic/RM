import java.io.Serializable;

public class Message implements Serializable {
    private Integer source;
    private Integer destination;
    private String content; // this can change

    public Integer getDestination() {
        return destination;
    }

    public Message(Integer source, Integer destination, String content) {
        this.source = source;
        this.destination = destination;
        this.content = content;
    }
}
