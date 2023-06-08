import java.io.*;
import java.net.*;
import java.util.*;

class SBSrvV2  {
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

    static int version = 1;
    private static ServerSocket sock;

    public static void main(String[] args) throws Exception{
        try {
            // Create a server socket and bind it to a port
            sock = new ServerSocket(9999);
            System.out.println("Server started. Listening on port "+sock.getLocalPort()+"...");
        }catch(IOException ex) {
            System.out.println("Local port number not available.");
            System.exit(1); }

        try {
            // Waits for clients to connect
            while (true) {
                Socket clientSocket = sock.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                // Handles client connection in a separate thread
                Thread clientThread = new Thread(() -> {
                    try {processRequests(clientSocket);
                    } catch (IOException e) {throw new RuntimeException(e);}});
                clientThread.start();
                addCli(clientSocket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processRequests(Socket clientSocket) throws IOException {
        InputStream inputStream = clientSocket.getInputStream();
        OutputStream outputStream = clientSocket.getOutputStream();

        while (true) {
            // Read the SBP message version
            int version = inputStream.read();

            // Read the SBP message code
            int code = inputStream.read();

            // Read the data length fields
            int dLength1 = inputStream.read();
            int dLength2 = inputStream.read();
            int dataLength = dLength1 + 256 * dLength2;

            // Read the data field
            byte[] data = new byte[dataLength];
            if (dataLength > 0) {
                int bytesRead = inputStream.read(data);
                if (bytesRead != dataLength) {
                    throw new IOException("Failed to read complete data field");
                }
            }

            // Process the received request
            switch (code) {
                case 0:
                    System.out.println("Received COMMTEST request");
                    sendResponse(outputStream, 2, null);
                    break;
                case 1:
                    System.out.println("Received DISCONN request");
                    sendResponse(outputStream, 2, null);
                    try {
                        remCli(clientSocket);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("Client: " + clientSocket.getInetAddress() + " disconnected");
                    return; // Exit the method to close the connection
                case 4:
                    System.out.println("Received AUTH request");
                    boolean isAuthenticated = authenticateUser(data);
                    if (isAuthenticated) {
                        sendResponse(outputStream, 2, null);
                    } else {
                        sendResponse(outputStream, 3, "Authentication failed".getBytes());
                    }
                    break;
                default:
                    System.out.println("Received unknown request with code: " + code);
                    sendResponse(outputStream, 3, "Unknown request".getBytes());
                    break;
            }
        }
    }

    private static void sendResponse(OutputStream outputStream, int code, byte[] data) throws IOException {
        // Calculate the length of the data field
        int dataLength = (data != null) ? data.length : 0;
        int dLength1 = dataLength % 256;
        int dLength2 = dataLength / 256;

        // Build the SBP message
        byte[] message = new byte[4 + dataLength];
        message[0] = (byte) version;
        message[1] = (byte) code;
        message[2] = (byte) dLength1;
        message[3] = (byte) dLength2;
        if (dataLength > 0) {
            System.arraycopy(data, 0, message, 4, dataLength);
        }

        // Send the SBP message
        outputStream.write(message);
        outputStream.flush();
    }

    private static boolean authenticateUser(byte[] data) {
        String[] credentials = new String(data).split("\0");
        String username = credentials[0];
        String password = credentials[1];

        // Perform authentication logic here
        // Replace this with your actual authentication mechanism
        return (username.equals("username") && password.equals("password"));
    }
}
