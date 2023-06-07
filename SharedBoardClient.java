package org.example;

import java.io.*;
import java.net.*;

public class SharedBoardClient {
    static InetAddress serverIP;
    static Socket sock;

    public static void main(String[] args) {
        if(args.length!=1) {
            System.out.println(
                    "Server IPv4/IPv6 address or DNS name is required as argument");
            System.exit(1); }

        // Checks if host available
        try { serverIP = InetAddress.getByName(args[0]); }
        catch(UnknownHostException ex) {
            System.out.println("Invalid server: " + args[0]);
            System.exit(1); }

        // Tries connection
        try { sock = new Socket(serverIP, 9999);

            // Authenticate client
            boolean authenticated = authenticate(sock, "username", "password");
            if (authenticated) {
                System.out.println("User authenticated successfully!");
            } else {
                System.out.println("Authentication failed. Please check your credentials.");
            }
        }
        catch(IOException ex) {
            System.out.println("Failed to connect.");
            System.exit(1); }

    }

    private static boolean authenticate(Socket socket, String username, String password) throws IOException {
        // Prepare the AUTH request
        byte[] authRequest = createAuthRequest(username, password);

        // Send the AUTH request to the server
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(authRequest);
        outputStream.flush();

        // Read the response from the server
        InputStream inputStream = socket.getInputStream();
        byte[] response = new byte[1024];
        int bytesRead = inputStream.read(response);

        // Process the response
        if (bytesRead != -1) {
            byte version = response[0];
            byte code = response[1];

            if (code == 2) {
                // Authentication successful (ACK received)
                return true;
            } else if (code == 3) {
                // Authentication failed (ERR received)
                System.out.println("Authentication error: " + new String(response, 4, bytesRead - 4));
            } else {
                System.out.println("Unexpected response from server: " + code);
            }
        }

        return false;
    }

    private static byte[] createAuthRequest(String username, String password) {
        byte[] usernameBytes = username.getBytes();
        byte[] passwordBytes = password.getBytes();

        byte[] request = new byte[4 + usernameBytes.length + passwordBytes.length];
        request[0] = 1; // Version
        request[1] = 4; // AUTH code
        request[2] = (byte) usernameBytes.length;
        request[3] = (byte) passwordBytes.length;

        System.arraycopy(usernameBytes, 0, request, 4, usernameBytes.length);
        System.arraycopy(passwordBytes, 0, request, 4 + usernameBytes.length, passwordBytes.length);

        return request;
    }
}