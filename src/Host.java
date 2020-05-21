import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Host {

    private Node router;

    public Host(Node node) {
        this.router = node;
    }

    public void connect(int port){
        //TODO
    }

    public void forwardMessage(Message message){

    }

    public static void main(String[] args) {
        // pitanje je kako zelimo host da nam izgleda, vrv i on mora da ima 2 threada
        // dodacu ih sutra
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter your id:");
        int sender = sc.nextInt();

        System.out.println("Select router: ");
        int router_id = sc.nextInt();

        try(Socket socket = new Socket("localhost", router_id + Node.NODE_PORT_OFFSET);
            PrintWriter out = new PrintWriter(
                    socket.getOutputStream()
            )) {
            System.out.println("Enter message: ");
//            String msg = sc.nextLine();
            System.out.println("Enter receiver id: ");
            int receiver = sc.nextInt();
            out.print(new Message(sender, receiver, "dummy").sendingFormat());
            out.flush();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        sc.close();
    }


}
