import java.io.*;
import java.net.*;

class SBCliV2 {
    static InetAddress serverIP;
    static Socket sock;
    static int version = 1;

    public static void main(String[] args) throws Exception{
        String username, password, frase;
        // Checks if ip argument given
        if(args.length!=1) {
            System.out.println(
                    "Server IPv4/IPv6 address or DNS name is required as argument");
            System.exit(1); }
        // Checks if host available
        try { serverIP = InetAddress.getByName(args[0]);
            System.out.println("Connected to server: "+serverIP);}
        catch(UnknownHostException ex) {
            System.out.println("Invalid server: " + args[0]);
            System.exit(1); }
        // Tries connection
        try { sock = new Socket(serverIP, 9999);}
        catch(IOException ex) {
            System.out.println("Failed to connect.");
            System.exit(1); }
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        // Tries authentication
        try{
            // Authenticate client
            System.out.print("Username: "); username = in.readLine();
            System.out.print("password: "); password = in.readLine();
            boolean authenticated = authenticateUser(sock, username, password);
            while(!authenticated){
                System.out.println("Authentication failed. Please check your credentials.");
                System.out.print("Username: "); username = in.readLine();
                System.out.print("Username: "); password = in.readLine();
                authenticateUser(sock, username, password);
            }
        } catch (IOException e) {
            System.out.println("Error after authentication...");
            e.printStackTrace();}
        while(true) { // read messages from the console and send them to the server
            System.out.println("DISCONN to disconnect, COMMTEST to test connection");
            frase=in.readLine();
            //Disconnection request to server == END CONNECTION WITH CLIENT
            if(frase.compareTo("DISCONN")==0) {
                // Send DISCONN message
                sendRequest(sock, 1, null);
                // Receive and process server responses
                receiveResponse(sock);
                break;
            } else if (frase.compareTo("COMMTEST")==0) {
                // Send DISCONN message
                sendRequest(sock, 0, null);
                // Receive and process server responses
                receiveResponse(sock);}}
        // Close the TCP connection
        System.out.println("Bye bye...");
        sock.close();
    }

    private static boolean authenticateUser(Socket socket, String username, String password) throws IOException {
        // Concatenate username and password with null terminators
        byte[] userData = (username + '\0' + password + '\0').getBytes();

        // Send AUTH request with username and password
        sendRequest(socket, 4, userData);

        // Receive and process server response
        byte[] response = receiveResponse(socket);
        return (response[1] == 2); // Check if the response is ACK
    }

    private static void sendRequest(Socket socket, int code, byte[] data) throws IOException {
        OutputStream outputStream = socket.getOutputStream();

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

    private static byte[] receiveResponse(Socket socket) throws IOException {
        InputStream inputStream = socket.getInputStream();

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
        byte[] message = new byte[4+dataLength];
        message[0] = (byte) version;
        message[1] = (byte) code;
        message[2] = (byte) dLength1;
        message[3] = (byte) dLength2;
        if (dataLength > 0) {
            System.arraycopy(data, 0, message, 4, dataLength);
        }

        // Process the received response
        switch (code) {
            case 2:
                System.out.println("Received ACK message");
                break;
            case 3:
                System.out.println("Received ERR message: " + new String(data));
                break;
            default:
                System.out.println("Received unknown message with code: " + code);
                break;
        }
        return message;
    }
}
