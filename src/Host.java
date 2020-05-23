import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Host extends Thread{

    private class Writer extends Thread{
        private PrintWriter out;
        public Writer(PrintWriter out) {
            this.out = out;
        }

        @Override
        public void run() {
            Scanner scanner = new Scanner(System.in);
            while(true){
                System.out.println("Enter receiver id: ");
                int receiver = scanner.nextInt();
                scanner.nextLine();
                System.out.println("reciver = " + receiver);
                System.out.println("Enter msg:");
                String msg = scanner.nextLine();

                if(msg.trim() == "stop")
                    break;

                out.write(new Message(id, receiver, msg).sendingFormat());
                out.flush();
            }
        }
    }

    public static int host_number = 0;

    private int id;
    private int port;
    private Map<Integer, Socket> socketTable;

    public Host() {
        // can be done smarter to avoid concurrency problems here
        id = host_number++;
        this.socketTable = new HashMap<>();
    }

    public void setPort(int port) { this.port = port; }

    public void forwardMessage(Message message){

    }

    public static void main(String[] args) {
        Host host = new Host();
        host.start();
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Select router: ");
        int router_id = scanner.nextInt();
        int port = router_id + Node.NODE_PORT_OFFSET;
        setPort(port);

        HostReader reader = new HostReader(port, id, this.socketTable);

        try(Socket socket = new Socket("localhost", port);
            PrintWriter out = new PrintWriter(
                    socket.getOutputStream()
                );
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()
                    )
            )
        ) {
            reader.start();

            Writer writer = new Writer(out);
            writer.start();

            reader.join();
            writer.join();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        scanner.close();
    }
}
