import java.io.*; // for IOException and Input/OutputStream
import java.net.*; // for Socket, ServerSocket, and InetAddress

/**
 * Server side of the connect four game which takes care of connecting
 * to a client.
 * 
 * @author Joey Campanelli, Yiming Yan
 *
 */
public class ConnectFourServer {
	
	public static void main(String[] args) throws IOException {
		try {
			InetAddress address = InetAddress.getLocalHost();
			//Display serevr ip to allow user to get ip to connect to
			System.out.println("Server IP Address: " + address.getHostAddress());
		} catch (UnknownHostException e) {
			System.out.println("Unable to determine this host's address");
		}

		ServerSocket servSock = new ServerSocket(50000);
		
		for (;;) {
			try {
				Socket clntSock = servSock.accept(); // Get client connection
				ConnectFourServerSession protocol=new ConnectFourServerSession(clntSock);
				Thread thread = new Thread(protocol);
				thread.start();
				
			} catch (SocketException e) {
				System.out.println("Client closed prematurely: " + e.getMessage());
			} catch (Exception e) {
				System.out.println("Program crashed " + e.getMessage());
			}
		}
	}
}
