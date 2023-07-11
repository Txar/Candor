package com.candor;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class GreetServer {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private FileLoader fileLoader;

    private Map<String, String> readHeaders(String s) {
        Map<String, String> headers = new HashMap<>();

        if (s == null) {
            return headers;
        }
        for (String i : s.split("\n")) {
            if (s.startsWith("GET")) {
                String[] lst = i.split(" ");
                if (lst.length == 2) {
                    headers.put("HTTP_VERSION", lst[1]);
                }
                else if (lst.length > 2) {
                    headers.put("HTTP_VERSION", lst[2]);
                    headers.put("PATH", lst[1]);
                }
            }
            else {
                String[] lst = i.split(": ", 2);
                if (lst.length == 2) {
                    headers.put(lst[0], lst[1]);
                }
            }
        }

        return headers;
    }

    public void start(int port) {
        fileLoader = new FileLoader("C:\\Users\\Maks\\Documents\\GitHub\\Candor\\web_page");
        try {
            serverSocket = new ServerSocket(port);
        } catch (Exception e) {
            System.out.println((e));
        }

        while (true) {
            String greeting = "";

            try {
                clientSocket = serverSocket.accept();
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                greeting = in.readLine();
            } catch (Exception e) {
                System.out.println(e);
            }

            Map headers = readHeaders(greeting);
            if ("hello server".equals(greeting)) {
                out.println("<html><body>hello client</body></html>");
            } else {
                out.println("HTTP/3 200 OK\nContent-Type: text/html\n\n");
                if (headers.containsKey("PATH") && headers.get("PATH").toString().startsWith("/public")) {
                    try {
                        out.println(new String(fileLoader.getFile(headers.get("PATH").toString())));
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
                else {

                    out.println("<!DOCTYPE html><html><head><meta charset='utf-8'><title>test</title><body><pre>");

                    for (Object i : headers.keySet()) {
                        out.println(i + "- value is " + headers.get(i));
                    }
                    out.println("</pre><h1>hi, client</h1></body></html>");
                }
                out.close();
            }
        }
    }

    public void stop() {
        try {
            in.close();
            out.close();
            clientSocket.close();
            serverSocket.close();
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        //Scanner s = new Scanner(System.in);
        //String choice = s.next();

        //if (choice.equals("server")) {
        System.out.println("Starting server.");
        GreetServer server = new GreetServer();
        server.start(2100);
        /*}
        else if (choice.equals("client")) {
            System.out.println("Starting client.");
            GreetClient client = new GreetClient();
            client.startConnection("127.0.0.1", 2100);
            String response = client.sendMessage("hello server");
            System.out.println("hello client".equals(response));
        }*/
    }

    /*public void givenGreetingClient_whenServerRespondsWhenStarted_thenCorrect() {
        GreetClient client = new GreetClient();
        client.startConnection("127.0.0.1", 2100);
        String response = client.sendMessage("hello server");
        System.out.println("hello client".equals(response));
    }*/
}