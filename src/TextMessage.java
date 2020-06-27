/**
 *  Text Message is used for communication between two hosts.
 */
public class TextMessage extends Message {
    /**
     * The identification of a message source host
     */
    private Integer source;
    /**
     * The identification of a message reciever host
     */
    private Integer receiver;
    /**
     * Message content
     */
    private String content;
    /**
     * The host that receives the message
     */
    private int receiverRouterId;

    public int getReceiverRouterId() {
        return receiverRouterId;
    }

    public Integer getReceiver() { return receiver; }
    public String getContent() { return content; }
    public Integer getSource() { return source; }


    public TextMessage(Integer source, Integer receiver, int receiverRouterId, String content) {
        this.source = source;
        this.receiver = receiver;
        this.content = content;
        this.receiverRouterId = receiverRouterId;
    }


    @Override
    public String toString() {
        return "TextMessage{" +
                "source=" + source +
                ", receiver=" + receiver +
                ", content='" + content + '\'' +
                '}';
    }

    @Override
    public types getType() {
        return types.TEXT_MESSAGE;
    }
}
