import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.util.Base64;

public  abstract class Message implements Serializable{

    public static Message parseMessage(String message){
        return SerializationUtils.deserialize(Base64.getDecoder().decode(message.substring(0, message.length() - 1)));
    }

    public String sendingFormat(){
        byte[] messageSerialized = SerializationUtils.serialize(this);
        return Base64.getEncoder().encodeToString(messageSerialized) + "!";
    }

    public abstract types getType();

    enum types{
        TEXT_MESSAGE,
        FLOODING_MESSAGE,
        CONNECTION_MESSAGE,
        DISTANCE_VECTOR_ROUTING_MESSAGE,
        PING_MESSAGE
    }
}