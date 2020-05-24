public class FloodingMessage extends Message {

    public int getSource(){
        return 0;
    }

    public Message getFloodingFormatNextDestination(Integer neighbour){
        return null;
    }

    public int getFloodingId(){
        return 0;
    }


    @Override
    public types getType() {
        return types.FLOODING_MESSAGE;
    }
}
