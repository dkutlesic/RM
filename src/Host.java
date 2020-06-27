import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Host in a network
 */
public class Host extends Thread{

    /**
     * Specialized Writer for host
     */
    private class Writer extends Thread{
        private PrintWriter out;
        public Writer(PrintWriter out) {
            this.out = out;
        }

        /**
         * Scan the message and receiver id from the standard input and write it to output stream
         */
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

                out.write(new TextMessage(id, receiver, 0, msg).sendingFormat());
                out.flush();
            }
        }
    }

    public static final int HOST_PORT_OFFSET = 4000;
    private static int host_number = 0;

    /**
     * Host identificaiton
     */
    private int id;

    /**
     * ID of router we are connected to
     */
    private int router_id;
    /**
     * Port = host identification + HOST_PORT_OFFSET
     */
    private int port;
    private Map<Integer, Socket> socketTable;

    public Host(int id) {
        this.id = id;
        this.port = id + Host.HOST_PORT_OFFSET;
        this.socketTable = new HashMap<>();
    }

    public void setPort(int port) { this.port = port; }


    /**
     *
     * @return
     * id of router we are connected to
     */
    public Integer getRouterID() {
        return this.router_id;
    }

    public static void main(String[] args) {
        System.out.print("Enter your id: ");
        Scanner scanner = new Scanner(System.in);
        int id = scanner.nextInt();
        Host host = new Host(id);
        host.start();
    }

    /**
     * Start streams for reading and writing
     */
    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Select router: ");

        this.router_id = scanner.nextInt();
        int router_port = this.router_id + Node.NODE_PORT_OFFSET;

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
