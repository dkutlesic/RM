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

    public static Message parseMessage(String message){

        //message format for testing
        String[] tokens = message.split(":");
        Integer source = Integer.parseInt(tokens[0]);
        Integer destination = Integer.parseInt(tokens[1]);
        String content = tokens[2];

        return new Message(source, destination, content);
    }

    @Override
    public String toString() {
        return "Source: " + source + "\tDestination: " + destination + "\tContent: " + content;
    }

    public static boolean isStringRoutingMessage(String message){
        //TODO
        return true;
    }
}
