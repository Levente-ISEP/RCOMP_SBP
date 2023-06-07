package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

class SharedBoardServer {

    private static HashMap<Socket,DataOutputStream> cliList = new HashMap<>();
    public static synchronized void sendToAll(int len, byte[] data) throws Exception {
        for(DataOutputStream cOut: cliList.values()) {
            cOut.write(len);
            cOut.write(data,0,len);
        }
    }
    public static synchronized void addCli(Socket s) throws Exception {
        cliList.put(s,new DataOutputStream(s.getOutputStream()));
    }
    public static synchronized void remCli(Socket s) throws Exception {
        cliList.get(s).write(0);
        cliList.remove(s);
        s.close();
    }
    private static ServerSocket sock;

    public static void main(String[] args) throws Exception{
        try { sock = new ServerSocket(9999);
            System.out.println("Server running on port " + SharedBoardServer.sock);
        }
        catch(IOException ex) {
            System.out.println("Local port number not available.");
            System.exit(1); }




        try {
            // Waits for clients to connect
            while (true) {
                Socket clientSocket = sock.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                // Handles client connection in a separate thread
                addCli(clientSocket);
                Thread clientThread = new Thread(() -> handleClient(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try {
            // Read the request from the client
            InputStream inputStream = clientSocket.getInputStream();
            byte[] request = new byte[1024];
            int bytesRead = inputStream.read(request);
            //System.out.println("Reading request...");

            // Process the request
            //System.out.println("Processing request...");
            if (bytesRead != -1) {
                byte version = request[0];
                byte code = request[1];
                //System.out.println("Message code: "+code);
                if(code == 0){
                    OutputStream outputStream = clientSocket.getOutputStream();
                    byte[] response = acknowledgeResponse();
                    outputStream.write(response);
                }
                if(code == 1){
                    OutputStream outputStream = clientSocket.getOutputStream();
                    byte[] response = acknowledgeResponse();
                    outputStream.write(response);
                    clientSocket.close();
                    System.out.println("Connection closed with client: "+clientSocket.getInetAddress());
                }
                if (code == 4) {
                    //System.out.println("Processing AUTH request: "+code);
                    // AUTH request
                    byte[] usernameBytes = new byte[request[2]];
                    byte[] passwordBytes = new byte[request[3]];

                    System.arraycopy(request, 4, usernameBytes, 0, usernameBytes.length);
                    System.arraycopy(request, 4 + usernameBytes.length, passwordBytes, 0, passwordBytes.length);

                    String username = new String(usernameBytes);
                    String password = new String(passwordBytes);

                    // Perform authentication
                    //System.out.println("Authenticating client...");
                    boolean authenticated = authenticate(username, password);

                    // Send the response to the client
                    //System.out.println("Responding to client...");
                    OutputStream outputStream = clientSocket.getOutputStream();
                    if (authenticated) {
                        byte[] response = acknowledgeResponse();
                        outputStream.write(response);
                    } else {
                        byte[] response = errorResponse("Authentication failed");
                        outputStream.write(response);
                    }
                    outputStream.flush();
                } else {
                    System.out.println("Unexpected request code received: " + code);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean authenticate(String username, String password) {
        // Authentication logic to be extended...
        return (username.equals("admin") && password.equals("password"));
    }

    private static byte[] errorResponse(String message) {
        byte[] messageBytes = message.getBytes();
        byte[] response = new byte[4 + messageBytes.length];
        response[0] = 1; // Version
        response[1] = 3; // Response code
        response[2] = 0; // D_LENGTH_1
        response[3] = (byte) messageBytes.length; // D_LENGTH_2
        System.arraycopy(messageBytes, 0, response, 4, messageBytes.length);
        return response;
    }

    private static byte[] acknowledgeResponse(){
        byte[] response = new byte[4];
        response[0] = 1; // Version
        response[1] = 2; // Response code
        response[2] = 0; // D_LENGTH_1
        response[3] = 0; // D_LENGTH_2
        return response;
    }

}