import java.io.*;
import java.net.*;

class SharedBoardClient {
    static InetAddress serverIP;
    static Socket sock;


    public static void main(String[] args) throws Exception {
        String username, password, frase;
        System.out.println("====SBP client v0.1====");
        if(args.length!=1) {
            System.out.println(
                    "Server IPv4/IPv6 address or DNS name is required as argument");
            System.exit(1); }

        // Checks if host available
        try { serverIP = InetAddress.getByName(args[0]);
            System.out.println("Connected to server: "+serverIP);
        }
        catch(UnknownHostException ex) {
            System.out.println("Invalid server: " + args[0]);
            System.exit(1); }

        // Tries connection
        try { sock = new Socket(serverIP, 9999);

        }
        catch(IOException ex) {
            System.out.println("Failed to connect.");
            System.exit(1); }


        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        DataOutputStream sOut = new DataOutputStream(sock.getOutputStream());

        // Tries authentication
        try{
            // Authenticate client
            System.out.print("Username: "); username = in.readLine();
            System.out.print("password: "); password = in.readLine();
            boolean authenticated = authenticate(sock, username, password);
            while(!authenticated){
                System.out.println("Authentication failed. Please check your credentials.");
                System.out.print("Username: "); username = in.readLine();
                System.out.print("Username: "); password = in.readLine();
                authenticate(sock, username, password);
            }
            System.out.println("AUTHENTICATION Successful!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        while(true) { // read messages from the console and send them to the server
            frase=in.readLine();
            //Disconnection request to server == END CONNECTION WITH CLIENT
            if(frase.compareTo("DISCONN")==0) {
                boolean disconnected = disconnect(sock);
                if(disconnected){
                    break;
                }

            } else if (frase.compareTo("COMMTEST")==0) {

            }
        }
        sock.close();


    }
    private static boolean communicationTest(Socket socket) throws IOException{
        byte[] commtestRequest = createCommunicationTestRequest();

        // Send the DISCONN request to the server
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(commtestRequest);
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
                // ACK recived
                System.out.println("Connection live");
                return true;
            } else if (code == 3) {
                // Disconnection failed (ERR received)
                System.out.println("Disconnection error: " + new String(response, 4, bytesRead - 4));
            } else {
                System.out.println("Unexpected response from server: " + code);
            }
        }
        return false;
    }
    private static byte[] createCommunicationTestRequest(){
            byte[] request = new byte[4];
            request[0] = 1; // Version
            request[1] = 0; // COMMTEST code
            request[2] = 0;
            request[3] = 0;

            return request;
        }
    private static boolean disconnect(Socket socket) throws IOException{
        // Prepare the DISCONN request
        byte[] disconnRequest = createDisconnectRequest();

        // Send the DISCONN request to the server
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(disconnRequest);
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
                // ACK recived
                System.out.println("Closing application! Bye bye...");
                return true;
            } else if (code == 3) {
                // Disconnection failed (ERR received)
                System.out.println("Disconnection error: " + new String(response, 4, bytesRead - 4));
            } else {
                System.out.println("Unexpected response from server: " + code);
            }
        }
        return false;
    }
    private static byte[] createDisconnectRequest() {

        byte[] request = new byte[4];
        request[0] = 1; // Version
        request[1] = 1; // DISCONN code
        request[2] = 0;
        request[3] = 0;

        return request;
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
                // ACK recived
                System.out.println("Authentication Successful!");
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
