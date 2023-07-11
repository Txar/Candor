package com.candor;

import java.io.*;
import java.net.Socket;

public class GreetClient {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public void startConnection(String ip, int port) {
        try {
            clientSocket = new Socket(ip, port);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }

    public String sendMessage(String msg) {
        out.println(msg);
        String resp = "";
        try {
            resp = in.readLine();
        }
        catch (Exception e) {
            System.out.println(e);
        }

        return resp;
    }

    public void stopConnection() {
        try {
            in.close();
            out.close();
            clientSocket.close();
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
}