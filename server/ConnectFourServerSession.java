import java.util.ArrayList;
import java.util.Arrays;
import java.io.*;
import java.net.*;

/**
 * Server session which takes care of all game logic between client (user)
 * and server (AI).
 * 
 * @author Joey Campanelli, Yiming Yan
 *
 */
public class ConnectFourServerSession implements Runnable {
	private Game game;
	private Socket clntSock;
	private static final int BUFSIZE = 3; // Size of receive buffer
	public static final byte[] START = new byte[] {0, 0, 0};
	public static final byte[] GAMEON = new byte[] {0, 0, 1};
	public static final byte[] REDWON = new byte[] {1, 0, 0};
	public static final byte[] QUIT = new byte[] {4, 4, 4};

	/**
	 * One parameter constructor for the ConnectFourServerSession class.
	 * 
	 * @param clntSock socket to the client
	 * 
	 * @throws IOException
	 */
	public ConnectFourServerSession(Socket clntSock) throws IOException {
		this.clntSock = clntSock;
		game = new Game();
	}
	
	public void run () {
		try {
			game.run();
		}
		catch(IOException ioe) {}
	}

	/**
	 * Game class of the ConnectFour server side.
	 * 
	 * @author Joey, Yimming, Daniel
	 *
	 */
	class Game {
		public static final int EMPTY = 0;
		public static final int RED = 1;
		public static final int BLACK = 2;
		public static final int ROW = 12;
		public static final int COLUMN = 13;
		private int[][] board;
		private int emptyCell = 0;

		/**
		 * Default constructor for the Game class.
		 */
		public Game() {
			init();
		}

		/**
		 * Creates the game board.
		 */
		public void init() {
			emptyCell = 42;
			this.board = new int[ROW][COLUMN];

			for (int i = 0; i < ROW; i++) {
				for (int j = 0; j < COLUMN; j++) {
					board[i][j] = EMPTY;
				}
			}
		}

		/**
		 * Checks if a certain spot is equal to a value.
		 * 
		 * @param firstDimension outer array index
		 * @param secondDimension inner array index
		 * @param value	value checked
		 * @param board	board to check
		 * 
		 * @return if it is equal or not
		 */
		public boolean isCellEquals(int firstDemension, int secondDimension, int value, int[][] board) {
			return board[firstDemension][secondDimension] == value;
		}

		/**
		 * Set chip and check for a win.
		 * 
		 * @param firstDimension inner array position
		 * @param secondDimension	outer array position
		 * @param player	player being checked
		 * @param actualMove	move status
		 * 
		 * @return win statu
		 */
		public boolean setChipAndCheck(int firstDimension, int secondDimension, int player, boolean actualMove) {
			int[][] boardToCheck;

			if (actualMove) {
				boardToCheck = board;
				board[firstDimension][secondDimension] = player;
				emptyCell--;
			} else {
				boardToCheck = deepCopy(board); 
				boardToCheck[firstDimension][secondDimension] = player;
			}

			return checkWin(firstDimension, secondDimension, 0, 1, player, boardToCheck)
					|| checkWin(firstDimension, secondDimension, -1, 1, player, boardToCheck)
					|| checkWin(firstDimension, secondDimension, -1, 0, player, boardToCheck)
					|| checkWin(firstDimension, secondDimension, 1, 1, player, boardToCheck);
		}

		/**
		 * Check for a win for specific player.
		 * 
		 * @param x	outer array position
		 * @param y	inner array position
		 * @param xDir	x direction
		 * @param yDir	y direction
		 * @param player	player to check
		 * @param board	board to check on
		 * 
		 * @return win status
		 */
		private boolean checkWin(int x, int y, int xDir, int yDir, int player, int[][] board) {
			int count = 0;
			int tempx = x;
			int tempy = y;

			while (count < 4) {
				if (!isCellEquals(tempx, tempy, player, board)) {
					break;
				}
				tempx += xDir;
				tempy += yDir;
				count++;
			}

			tempx = x - xDir;
			tempy = y - yDir;

			while (count < 4) {
				if (!isCellEquals(tempx, tempy, player, board)) {
					break;
				}
				tempx -= xDir;
				tempy -= yDir;
				count++;
			}
			
			return count == 4;
		}

		/**
		 * Gets the computer's move.
		 * 
		 * @return computer's move spot
		 */
		public byte[] getComputerMove() {
			byte[] move = lookForWin(BLACK);// look for win

			if (move == null) {
				move = lookForWin(RED);// look for block

				if (move == null) {
					move = openSpaceMove();
				}
			}
			return move;
		}

		/**
		 * Looks for an open space
		 * 
		 * @return open space spot
		 */
		private byte[] openSpaceMove() {
			byte row;
			ArrayList<byte[]> possibleMove = new ArrayList<byte[]>();

			for (byte i = 3; i < COLUMN - 3; i++) {
				if ((row = findEmptyCellByColumn(i)) != -1) {
					possibleMove.add(new byte[] { row, i });
				}
			}
			return possibleMove.get(getRandomInteger(0, possibleMove.size() - 1));
		}

		/**
		 * Return a random integer between to values/
		 * 
		 * @param maximum	upper boundary
		 * @param minimum	lower boundary
		 * 
		 * @return a random integer
		 */
		public int getRandomInteger(int maximum, int minimum) {
			return ((int) (Math.random() * (maximum - minimum))) + minimum;
		}

		/**
		 * Create a deep copy of the game board.
		 * 
		 * @param original	game board to be copied
		 * 
		 * @return copied game board
		 */
		private int[][] deepCopy(int[][] original) {
			if (original == null) {
				return null;
			}

			final int[][] result = new int[original.length][];

			for (int i = 0; i < original.length; i++) {
				result[i] = Arrays.copyOf(original[i], original[i].length);
			}

			return result;
		}

		/**
		 * Looks for a win.
		 * 
		 * @param player	player to look for win for
		 * 
		 * @return best move
		 */
		private byte[] lookForWin(int player) {
			byte row;
			ArrayList<byte[]> possibleMove = new ArrayList<byte[]>();
			
			for (byte i = 3; i < COLUMN - 3; i++) {
				if ((row = findEmptyCellByColumn(i)) != -1) {
					System.out.println(row + "*" + i + "*" + player);
					possibleMove.add(new byte[] { row, i });
				}
			}
			
			for (int j = 0; j < possibleMove.size(); j++) {
				if (setChipAndCheck(possibleMove.get(j)[0], possibleMove.get(j)[1], player, false)) {
					System.out.println(possibleMove.get(j)[0] + "*" + possibleMove.get(j)[0] + "*" + player);
					return possibleMove.get(j);
				}
			}

			return null;
		}

		/**
		 * Find an empty cell in a column.
		 * 
		 * @param x column to look in
		 * 
		 * @return column cell
		 */
		public byte findEmptyCellByColumn(byte x) {
			byte row = -1;

			for (byte i = 3; i < ROW - 3; i++) {
				if (board[i][x] == EMPTY) {
					row = i;
				}
			}

			return row;
		}

		/**
		 * Displays the board.
		 * 
		 * @throws IOException
		 */
		public void display() throws IOException {
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

		/**
		 * Runs the game on the server.
		 * 
		 * @throws IOException
		 */
		public void run() throws IOException {
			System.out.println("Handling client at " + clntSock.getInetAddress().getHostAddress() + " on port "
					+ clntSock.getPort());
			OutputStream out = clntSock.getOutputStream();

			for (;;) {
				System.out.println("in run ");
				byte[] byteBuffer = receiveBytes();

				System.out.println("Bytes received: " + byteBuffer[0] + byteBuffer[1] + byteBuffer[2]);

				if (Arrays.equals(byteBuffer, QUIT)){
					clntSock.close();
					break;
				}
				
				if (byteBuffer[0] == 0 && byteBuffer[1] == 0 && byteBuffer[2] == 0) {
					out.write(GAMEON);
				}
				else {
					if (byteBuffer[0] == 9) {
						if (setChipAndCheck(byteBuffer[1] + 3, byteBuffer[2] + 3, RED, true)) {
							display();
							init();
							out.write(REDWON);
						} else {
							byte[] move = getComputerMove();
							if (setChipAndCheck(move[0], move[1], BLACK, true)) {
								display();
								System.out.println("Bytes sent if: " + "*" + move[0] + "*" + move[1] + "*");
								init();
								out.write(new byte[] { 2, move[0], move[1] });
							} else {
								if (emptyCell != 0) {
									display();
									System.out.println("Bytes sent else: " + "*" + move[0] + "*" + move[1] + "*");
									out.write(new byte[] { 9, move[0], move[1] });
								} else {
									display();
									init();
									out.write(new byte[] { 3, move[0], move[1] });
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Receives bytes from the client.
	 * 
	 * @return message to send to the client
	 * 
	 * @throws IOException
	 */
	private byte[] receiveBytes() throws IOException {
		int totalBytesRcvd = 0; // Total bytes received so far
		int bytesRcvd; // Bytes received in last read
		byte[] byteBuffer = new byte[BUFSIZE]; // Receive buffer
		InputStream in = clntSock.getInputStream();

		try {
			while (totalBytesRcvd < BUFSIZE) {
				if ((bytesRcvd = in.read(byteBuffer, totalBytesRcvd, BUFSIZE - totalBytesRcvd)) == -1) {
					clntSock.close();
				}
				totalBytesRcvd += bytesRcvd;
			}
		} catch (SocketException e) {
			clntSock.close();
			throw new SocketException("Connection closed");
		} catch (IOException e) {
			System.out.println("IOException in receiveBytes()");
		}

		return byteBuffer;
	}
}