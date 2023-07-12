package com.candor.httpserver;

import org.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpServer {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private OutputStream out;
    private BufferedReader in;
    private FileLoader fileLoader;
    private JSONObject contentTypes;

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

    public void sendText(String text) {
        try {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private String getContentType(String path) {
        String s = "text/plaintext";
        try {
            s = contentTypes.get(fileLoader.fileType(path)).toString();
        } catch (Exception e) {
            System.out.println(e);
            try {
                s = contentTypes.get("default").toString();
            } catch (Exception e2) {
                System.out.println(e2);
            }
        }
        return s;
    }

    public void start(int port) {
        fileLoader = new FileLoader("C:\\Users\\Maks\\Documents\\GitHub\\Candor\\web_server", "config.json");
        contentTypes = new JSONObject(new String(fileLoader.getFile(fileLoader.config.get("content_types_config").toString(), false, true)));
        try {
            serverSocket = new ServerSocket(port);
        } catch (Exception e) {
            System.out.println((e));
        }

        while (true) {
            String greeting = "";

            try {
                clientSocket = serverSocket.accept();
                out = clientSocket.getOutputStream();
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                greeting = in.readLine();
            } catch (Exception e) {
                System.out.println(e);
            }

            Map headers = readHeaders(greeting);
            if ("hello server".equals(greeting)) {
                sendText("<html><body>hello client</body></html>");
            } else {
                if (headers.containsKey("PATH") && headers.get("PATH").toString().startsWith("/public")) {
                    try {
                        String path = headers.get("PATH").toString();
                        if (fileLoader.isAccessible(path)) {
                            String contentType = getContentType(path);
                            sendText("HTTP/2 200 OK\nContent-Type: " + contentType + " \n\n");

                            out.write(fileLoader.getFile(path));
                        }
                        else {
                            sendText("HTTP/2 404 Not Found\nContent-Type: text/html\n\n");
                            out.write(fileLoader.getFile(fileLoader.config.get("not_found").toString(), false, true));
                        }
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
                else {

                    sendText("<!DOCTYPE html><html><head><meta charset='utf-8'><title>test</title><body><pre>");

                    for (Object i : headers.keySet()) {
                        sendText(i + "- value is " + headers.get(i));
                    }
                    sendText("</pre><h1>hi, client</h1></body></html>");
                }
                try {
                    out.close();
                } catch (Exception e) {
                    System.out.println(e);
                }
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
        HttpServer server = new HttpServer();
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