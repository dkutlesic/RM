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
            ConnectionMessage conn_msg = new ConnectionMessage(port, id);
            out.write(conn_msg.sendingFormat());
            out.flush();
            System.out.println("Connection msg sent!");

            Scanner scanner = new Scanner(System.in);
            while(true){
                System.out.println("Enter receiver id: ");
                int receiver = scanner.nextInt();
                scanner.nextLine();
                System.out.println("Enter msg:");
                String msg = scanner.nextLine();
                if(msg.trim() == "stop")
                    break;

                out.write(new TextMessage(id, receiver, msg).sendingFormat());
                out.flush();
            }
        }
    }

    public static final int HOST_PORT_OFFSET = 4000;
    private static int host_number = 0;

    private int id;
    private int port;
    private Map<Integer, Socket> socketTable;

    public Host(int id) {
        this.id = id;
        this.port = id + Host.HOST_PORT_OFFSET;
        this.socketTable = new HashMap<>();
    }

    public void setPort(int port) { this.port = port; }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int id = scanner.nextInt();
        Host host = new Host(id);
        host.start();
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Select router: ");
        int router_id = scanner.nextInt();
        int router_port = router_id + Node.NODE_PORT_OFFSET;

        HostReader reader = new HostReader(port, id, this.socketTable);

        try(Socket socket = new Socket("localhost", router_port);
            PrintWriter out = new PrintWriter(
                    socket.getOutputStream()
                );
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
