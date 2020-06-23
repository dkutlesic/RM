/**
 *  Text Message is used for communication between two hosts.
 */
public class TextMessage extends Message {
    /**
     * The identification of a message source host
     */
    private Integer source;
    /**
     * The identification of a message destination host
     */
    private Integer destination;
    /**
     * Message content
     */
    private String content;
    /**
     * The host that receives the message
     */
    private Host reciever;

    public Host getReciever() { return reciever; }
    public Integer getDestination() { return destination; }
    public String getContent() { return content; }
    public Integer getSource() { return source; }

    public TextMessage(Integer source, Integer destination, String content) {
        this.source = source;
        this.destination = destination;
        this.content = content;
    }

    public TextMessage(Integer source, Host reciever, String content){
        this.source = source;
        this.reciever = reciever;
//        this.destination = reciever.getPriority();
        this.content = content;
    }

    @Override
    public String toString() {
        return "TextMessage{" +
                "source=" + source +
                ", destination=" + destination +
                ", content='" + content + '\'' +
                '}';
    }

    @Override
    public types getType() {
        return types.TEXT_MESSAGE;
    }
}
