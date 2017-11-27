import java.net.*;
import java.util.Arrays;
import java.util.Scanner;
import java.io.*; // for IOException and Input/OutputStream

/**
 * Connect Four game played client side.
 * 
 * @author Joey Campanelli, Yiming Yan
 */
public class ConnectFourClient {
	static String servIP = "";
	static int servPort = 50000;
	
	public static final byte[] START = new byte[] {0, 0, 0};
	public static final byte[] GAMEON = new byte[] {0, 0, 1};
	public static final byte[] REDWON = new byte[] {1, 0, 0};
	public static final byte[] QUIT = new byte[] {4, 4, 4};

	public static final byte EMPTY = 0;
	public static final byte RED = 1;
	public static final byte BLACK = 2;

	public static final byte COLUMN = 7;
	public static final byte ROW = 6;
	
	private byte[][] board;
	private InputStream in;
	private OutputStream out;
	private Socket socket;

	private static final int BUFSIZE = 3; // Size of receive buffer
	int recvMsgSize; // Size of received message
	byte[] byteBuffer = new byte[BUFSIZE]; // Receive buffer

	/**
	 * Default constructor for the ConnectFourClient.
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public ConnectFourClient() throws UnknownHostException, IOException {
		// Setup networking
		socket = new Socket(servIP, servPort);
		out = socket.getOutputStream();
		in = socket.getInputStream();
	}

	/**
	 * Receives bytes from the server.
	 * 
	 * @return game update message
	 * 
	 * @throws IOException
	 */
	private byte[] receiveBytes() throws IOException {
		int totalBytesRcvd = 0; // Total bytes received so far
		int bytesRcvd; // Bytes received in last read
		InputStream in = socket.getInputStream();

		try {
			while (totalBytesRcvd < BUFSIZE) {
				if ((bytesRcvd = in.read(byteBuffer, totalBytesRcvd, BUFSIZE - totalBytesRcvd)) == -1) {
					socket.close();
				}
				totalBytesRcvd += bytesRcvd;
			}
		} catch (SocketException e) {
			socket.close();
			throw new SocketException("Connection closed");
		} catch (IOException e) {
			System.out.println("IOException in receiveBytes()");
		}

		return byteBuffer;
	}

	/**
	 * Plays the game.
	 * 
	 * @throws Exception
	 */
	public void play() throws Exception {
		byte[] response = new byte[BUFSIZE];

		try {
			while (true) {
				response = receiveBytes();
				
				if (Arrays.equals(response, GAMEON)) {
					display();
					getInputAndSend();
				} else if (response[0] == 9) {
					board[response[1]-3][response[2]-3] = BLACK;
					try {
						Thread.sleep(1000);
					} catch(InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
					display();
					getInputAndSend();
				} else if (response[0] == 1) {
					System.out.println("You win!");
					if (!wantsToPlay()) {
						break;
					}
				} else if (response[0] == 2) {
					board[response[1]-3][response[2]-3] = BLACK;
					display();
					System.out.println("The computer wins!");
					if (!wantsToPlay()) {
						break;
					}
				} else if (response[0] == 3) {
					board[response[1]-3][response[2]-3] = BLACK;
					display();
					System.out.println("It's a tie!");
					if (!wantsToPlay()) {
						break;
					}
				}
			}
		}
		catch (Exception e) {
			System.out.println("Game interrupted.");
			out.write(QUIT);
		} finally {
			socket.close();
		}
	}
	
	/**
	 * Get the input from the user and send it to the server.
	 * 
	 * @throws IOException
	 */
	private void getInputAndSend() throws IOException{
		while (true) {
			byte inputColumn = getPlayerMove();
			if (inputColumn != -1) {
				byte inputRow = findEmptyCellByColumn(inputColumn);
				
				if (inputRow != -1) {
					board[inputRow][inputColumn] = RED;
					display();
					out.write(new byte[] { 9, inputRow, inputColumn });
					break;
				} else {
					display();
					System.out.println("No space left in column to place chip.");
				}
			}
		}
	}

	/**
	 * Find an empty cell in a column.
	 * 
	 * @param x column to look in
	 * 
	 * @return column cell
	 */
	public byte findEmptyCellByColumn(byte x) { // checks for room in column and returns free spot.
		byte row = -1;
		for (byte i = 0; i < ROW; i++) {
			if (board[i][x] == EMPTY) {
				row = i;
			}
		}
		return row;
	}

	/**
	 * Get the user's move.
	 * 
	 * @return column user chose
	 */
	private byte getPlayerMove() {
		byte column = -1;
		Scanner input = new Scanner(System.in);
		System.out.println("Place a chip between 0 to 6.");
		try {
			column = input.nextByte();
			if (column >= 0 && column < COLUMN) {
			} 
			else {
				System.out.println("Not a number between 0 and " + (COLUMN - 1) + ".");
				column = -1;
			}
		} catch (Exception exc) {
			System.out.println("Not a number between 0 and " + (COLUMN - 1) + ".");
			input.nextLine();
		}
		return column;
	}

	/**
	 * Ask the user to play game or quit.
	 * 
	 * @return user's decision
	 */
	private boolean wantsToPlay() {
		System.out.println("Press S to start a new game.");
		System.out.println("Press Q to quit.");
		// input scanner to get user input
		Scanner input = new Scanner(System.in);
		String inputString = "";
		
		while (true) {
			try {
				inputString = input.next();

				if (inputString.charAt(0) == 'S' || inputString.charAt(0) == 's') {
					out.write(START);
					// Setup new board
					board = new byte[ROW][COLUMN];
					for (byte i = 0; i < ROW; i++) {
						for (byte j = 0; j < COLUMN; j++) {
							board[i][j] = EMPTY ;
						}
					}
					return true;
				} else {
					if (inputString.charAt(0) == 'Q' || inputString.charAt(0) == 'q') {
						out.write(QUIT);
						socket.close();
						System.exit(0);
						return false;
					}
				}
			} catch (Exception exc) {
				System.out.println("Invalid move.");
				input.nextLine();
			}
		}
	}

	/**
	 * Displays the board.
	 * 
	 * @throws IOException
	 */
	public void display() throws IOException {
		for (int k = 0; k < 100; k++) System.out.println();
		for (int i = -1; i < ROW; i++) {
			for (int j = 0; j < COLUMN; j++) {
				if (i != -1) {
					System.out.print("[");
					
					if (board[i][j] == 0) {
						System.out.print(" ]");
					} else {
						System.out.print(board[i][j] + "]");
					}

				} else {
					System.out.print(" " + j + " ");
				}
			}
			System.out.println();
		}
	}

	public static void main(String[] args) throws Exception {
		try {
			while (true) {
				// Test for correct # of args
				if (args.length != 1)
					throw new IllegalArgumentException("Parameter(s): <Server>");
				// Server name or IP address
				servIP = args[0];

				ConnectFourClient c4 = new ConnectFourClient();
				
				if (c4.wantsToPlay())
				c4.play();
			}
		} catch (ConnectException e) {
			System.out.println("Connection closed by server.");
		}
	}
}