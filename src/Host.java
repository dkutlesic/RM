import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Host extends Thread{

    private class Reader extends Thread{
        private BufferedReader in;

        private static final int BUFFER_SIZE = 4096;

        public Reader(BufferedReader in) {
            this.in = in;
        }

        @Override
        public void run() {

            char[] buffer = new char[BUFFER_SIZE];

            while(true){
                int bytes_read = 0;
                try {
                    bytes_read = in.read(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(bytes_read != 0){
                    Message msg = Message.parseMessage(buffer.toString().substring(0, bytes_read));
                    System.out.println(msg);
                }
            }
        }
    }

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

    public void connect(int port){
        //TODO
    }

    public Host() {
        // can be done smarter to avoid concurrency problems here
        id = host_number++;
    }

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

        try(Socket socket = new Socket("localhost", router_id + Node.NODE_PORT_OFFSET);
            PrintWriter out = new PrintWriter(
                    socket.getOutputStream()
                );
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()
                    )
            )
        ) {
            Reader reader = new Reader(in);
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
