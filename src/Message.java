import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.util.Base64;

/**
 * Message is used for communication between nodes and hosts.
 * Messages are encoded at the sender side and decoded at the receiver side.
 */
public  abstract class Message implements Serializable{

    /**
     * @param message
     * @return Decoded message
     */
    public static Message parseMessage(String message){
        return SerializationUtils.deserialize(Base64.getDecoder().decode(message.substring(0, message.length() - 1)));
    }

    /**
     * @return Encoded message
     */
    public String sendingFormat(){
        byte[] messageSerialized = SerializationUtils.serialize(this);
        return Base64.getEncoder().encodeToString(messageSerialized) + "!";
    }

    public abstract types getType();

    /**
     * There are several types of messages in the network:
     */
    enum types{
        /**
         * Plain text message: Text Message is used for communication between two hosts.
         * The text message is not used for routing. Other types of messages are used for the network utility and routing.
         *
         */
        TEXT_MESSAGE,
        /**
         * The flooding message is used to broadcast a message to all nodes in the network
         */
        FLOODING_MESSAGE,
        /**
         * The connection message is used for connecting a new host to a node
         */
        CONNECTION_MESSAGE,
        /**
         * The routing message contains information for routing
         */
        ROUTING_MESSAGE,
        /**
         * All nodes occasionally send ping messages to their neighbors reporting that they are alive and well
         */
        PING_MESSAGE
    }
}