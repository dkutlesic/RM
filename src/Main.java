import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        Node.ROUTING_TABLE_FOR_DEMO = new Map[3];
        Node.ROUTING_TABLE_FOR_DEMO[0] = new HashMap<>();
        Node.ROUTING_TABLE_FOR_DEMO[0].put(2, 1);
        Node.ROUTING_TABLE_FOR_DEMO[0].put(1, 0);
        Node.ROUTING_TABLE_FOR_DEMO[0].put(0, 0);

        Node.ROUTING_TABLE_FOR_DEMO[1] = new HashMap<>();
        Node.ROUTING_TABLE_FOR_DEMO[1].put(2, 1);
        Node.ROUTING_TABLE_FOR_DEMO[1].put(1, 0);
        Node.ROUTING_TABLE_FOR_DEMO[1].put(0, 2);

        Node.ROUTING_TABLE_FOR_DEMO[2] = new HashMap<>();
        Node.ROUTING_TABLE_FOR_DEMO[2].put(2, 1);
        Node.ROUTING_TABLE_FOR_DEMO[2].put(1, 0);
        Node.ROUTING_TABLE_FOR_DEMO[2].put(0, 0);


        JSONParser parser = new JSONParser();

        Vector<Map<Integer, Integer>> tables = null;
        Set<Integer> seenNodes = new HashSet<>();


        try(BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        new BufferedInputStream(
                                new FileInputStream(
                                        "config/network.json"
                                )
                        )
                )
        )) {
            JSONObject content = (JSONObject) parser.parse(in);
            int num_nodes = ((Long) content.get("nodes")).intValue();

            JSONArray edges = (JSONArray) content.get("edges");

            tables = new Vector<>(num_nodes);
            for (int i = 0; i < num_nodes; i++) {
                tables.add(new HashMap<>());
            };
            for(Object edge : edges){
                if(edge instanceof  JSONObject){
                    int first = ((Long) ((JSONObject) edge).get("1")).intValue();
                    int second = ((Long) ((JSONObject) edge).get("2")).intValue();
                    seenNodes.add(first);
                    seenNodes.add(second);
                    int len = ((Long) ((JSONObject) edge).get("len")).intValue();
                    tables.get(first).put(second + Node.NODE_PORT_OFFSET, len);
                    tables.get(second).put(first + Node.NODE_PORT_OFFSET, len);
                }
            }

            System.out.println(tables);


        } catch (FileNotFoundException | ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // how things should be done
        Vector<Node> nodes = new Vector<>();

        for (int i = 0; i < tables.size(); i++) {
            Node node = new Node(tables.get(i), i, seenNodes);
            nodes.add(node);
            node.start();
        }

        Scanner scanner = new Scanner(System.in);

        scanner.nextLine();

//        try {
//            while(true) {
//                Thread.sleep(60 * 1000);
//
//                //update-uj matricu
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//

    }
}
