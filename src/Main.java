import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;

public class Main {
    public static void killEdge(Node n1, Node n2){
        n1.deleteEdge(n2.identification);
        n2.deleteEdge(n1.identification);
    }

    public static void main(String[] args) throws InterruptedException {
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

        Vector<Node> nodes = new Vector<>();

//        for (int i = 0; i < tables.size(); i++) {
//            DVRNode node = new DVRNode(tables.get(i), i, seenNodes);
//            nodes.add(node);
//            node.start();
//        }

        for (int i = 0; i < tables.size(); i++) {
            LSRNode node = new LSRNode(tables.get(i), i);
            nodes.add(node);
            node.start();
        }

        Thread.sleep(5000);

        killEdge(nodes.elementAt(0), nodes.elementAt(2));
        System.out.println("EDGE 2-0 KILLED");

        Scanner scanner = new Scanner(System.in);

        scanner.nextLine();

    }
}
