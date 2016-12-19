package client;

import java.io.*;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import cli.Command;
import cli.Shell;
import client.tcp.PrivateTcpListnerThread;
import client.tcp.PrivateTcpWriterThread;
import client.tcp.PublicTcpListenerThread;
import client.udp.PublicUdpListenerThread;
import util.Config;
import util.Keys;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;

public class Client implements IClientCli, Runnable {

	private Socket tcpSocket;
	private DatagramSocket udpSocket;
	private String componentName;
	private Config config;
	private Shell shell;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private BufferedReader serverReader;
	private PrintWriter unencryptedServerWriter;
	private ServerSocket privateTcpServerSocket;
	private PrivateTcpWriterThread privateTcpWriter;
	private List<String> publicMessageQueue;
	private List<String> commandResponseQueue;
	
	private final String COULD_NOT_ESTABLISH_CONNECTION = "Could not establish connection.";
	private final String PRIVATE_ADDRESS_INCORRECT = "PrivateAddress is not correct!";
	private final String PORT_NOT_A_NUMBER = "Port is not a number!";
	private final String PORT_OUT_OF_RANGE = "Port value out of range!";
	private final String NO_MESSAGE_RECEIVED = "No message received!";
	private final String WORONG_USER_OR_USER_NOT_REACHABLE = "Wrong username or user not reachable.";
	private final String NOT_LOGGED_IN = "Not logged in.";
	private final String SUCESSFULLY_LOGGED_IN = "Successfully logged in.";
	private final String ALREADY_LOGGED_IN = "Already logged in.";
	private final String COULD_NOT_OPEN_SOCKET = "Could not open socket!";

	private Cipher inputCipher;
	private Cipher outputCipher;


	public String decodeMessage(String message) {
		byte[] encrypted = Base64.decode(message);
		String plainText = null;

		try {
			plainText = new String(inputCipher.doFinal(encrypted));
			return plainText;

		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void write(String msg) {
		write(msg.getBytes());
	}
	public void write(byte[] msg) {
		try {
			byte[] encryptedMessage = outputCipher.doFinal(msg);
			byte[] base64Message = Base64.encode(encryptedMessage);

			unencryptedServerWriter.println(new String(base64Message));
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}

	}


	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Client(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		
		publicMessageQueue = new LinkedList<String>();
		commandResponseQueue = new LinkedList<String>();
		
		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
	}

	@Override
	public void run() {
		
		new Thread(shell).start();
	}

	@Override
	@Command
	public String login(String username, String password) throws IOException {
//
//		/* check if user is already logged in and if so do not send login command to server (save resources) */
//		if(isLoggedIn())
//		{
//			return ALREADY_LOGGED_IN;
//		}
//
//		createTcpServerSocket();
//
//		// if createTcpServerSocket was not successful
//		if(tcpSocket == null)
//		{
//			return null;
//		}
//
//		// write login command to server
//		write(String.format("!login %s %s%n",username,password));
//
//		String response = waitForResponse(commandResponseQueue);
//
//		if(response == null || !response.equals(SUCESSFULLY_LOGGED_IN))
//		{
//			tcpSocket.close();
//		}
//
//		return response;

		return null;
	}

	@Override
	@Command
	public String logout() throws IOException {
		
		if(!isLoggedIn())
		{
			return NOT_LOGGED_IN;
		}
		
		// write login command to server
		write("!logout");
		
		String response  = waitForResponse(commandResponseQueue);
		
		tcpSocket.close();	// will also close in and output stream
		
		return response;
	}

	@Override
	@Command
	public String send(String message) throws IOException {
		
		if(!isLoggedIn())
		{
			return NOT_LOGGED_IN;
		}
		
		write(String.format("!send %s%n", message));
		
		return null;
	}

	@Override
	@Command
	public String list() throws IOException {
		
		String request = "!list";
		byte[] buffer = request.getBytes();
			
		try{
			if(udpSocket == null)
			{
				udpSocket = new DatagramSocket();
			}
			
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, 
					InetAddress.getByName(config.getString("chatserver.host")), config.getInt("chatserver.udp.port"));
			
			udpSocket.send(packet);	// send udp packet to server (connectionless => it is not ensured that server is available)
			
			// start thread to listen to server response
			PublicUdpListenerThread udpListnerThread = new PublicUdpListenerThread(shell, udpSocket);
			new Thread(udpListnerThread).start();
		}
		catch(IOException e){
			
		}
	
		return null;
	}

	@Override
	@Command
	public String msg(String username, String message) throws IOException {
		
		if(!isLoggedIn())
		{
			return NOT_LOGGED_IN;
		}

		String lookupResponse = lookup(username);
		
		if(!lookupResponse.matches("(.*):(.*)"))
		{
			return WORONG_USER_OR_USER_NOT_REACHABLE;
		}
		
		String parts[] = lookupResponse.split(":");
		
		/* open thread to send private message to other user */
		privateTcpWriter = new PrivateTcpWriterThread(message, username, parts[0], Integer.parseInt(parts[1]), shell);
		Thread privateWriterThread = new Thread(privateTcpWriter);
		privateWriterThread.start();
		
		return null;
	}

	@Override
	@Command
	public String lookup(String username) throws IOException {
		
		if(!isLoggedIn())
		{
			return NOT_LOGGED_IN;
		}
		
		// write lookup command to server
		write("!lookup "+ username);
		
		return waitForResponse(commandResponseQueue);
	}

	@Override
	@Command
	public String register(String privateAddress) throws IOException {
		
		if(!isLoggedIn())
		{
			return NOT_LOGGED_IN;
		}
		
		int port;
		
		String parts[] = privateAddress.split(":");
		if(parts.length != 2)
		{
			return PRIVATE_ADDRESS_INCORRECT;
		}
		
		/* check if port is a number and in range */
		try{
			port = Integer.parseInt(parts[1]);
			if(port <= 0 || port > 65535)	// port number of 0 means that the port number is automatically allocated this does not make sense at this point
			{
				return PORT_OUT_OF_RANGE;
			}
		}
		catch(NumberFormatException e)
		{
			return PORT_NOT_A_NUMBER;
		}
		
		try{
			ServerSocket newPrivateTcpServerSocket = new ServerSocket(port); // try to open new server socket
			
			// if successful then
			
			/* if user has already registered a address then close old public listener */
			if(privateTcpServerSocket != null)
			{
				privateTcpServerSocket.close();
			}
			privateTcpServerSocket = newPrivateTcpServerSocket;
		}
		catch(SocketException e){
			return COULD_NOT_OPEN_SOCKET;
		}
		
		write(String.format("!register %s%n",privateAddress));
		
		String response = waitForResponse(commandResponseQueue);
	
		/* start listener for private messages */
		PrivateTcpListnerThread privateTcpListner = new PrivateTcpListnerThread(privateTcpServerSocket,shell);
		Thread privateListenerThread = new Thread(privateTcpListner);
		privateListenerThread.start();
		
		return response;
	}
	
	@Override
	@Command
	public String lastMsg() throws IOException {
		
		if(!isLoggedIn())
		{
			return NOT_LOGGED_IN;
		}
		
		if(publicMessageQueue.size() == 0)
		{
			return NO_MESSAGE_RECEIVED;
		}
		
		String lastMessage = null;
		
		synchronized (publicMessageQueue) {
			lastMessage = publicMessageQueue.get(publicMessageQueue.size()-1);
		}
		
		return lastMessage;
	}

	@Override
	@Command
	public String exit() throws IOException {
		
		logout();	// logout also close tcpSocket
		
		if(privateTcpServerSocket != null)
		{
			privateTcpServerSocket.close();
		}
		
		if(privateTcpWriter != null)
		{
			privateTcpWriter.close();
		}
		
		if(udpSocket != null)
		{
			udpSocket.close();
		}
		
		/* terminate shell thread */
		if(shell != null){
			shell.close();
			userRequestStream.close();
			userResponseStream.close();
		}
		
		return null;
	}
	
	
	private void createTcpServerSocket(){
		try {
			/* create tcp socket with server hostname and server port */
			tcpSocket = new Socket(config.getString("chatserver.host"),config.getInt("chatserver.tcp.port"));
			
			// create a reader to retrieve messages send by the server
			serverReader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
			
			// create a writer to send messages to the server
			unencryptedServerWriter = new PrintWriter(tcpSocket.getOutputStream(), true);
			
			/* start thread for listening to messages from the server */
			PublicTcpListenerThread publicListener = new PublicTcpListenerThread(tcpSocket, serverReader, publicMessageQueue, commandResponseQueue, shell, this);
			Thread publicListenerThread = new Thread(publicListener);
			publicListenerThread.start();
		}
		catch (ConnectException e) {
			try {
				shell.writeLine(COULD_NOT_ESTABLISH_CONNECTION);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isLoggedIn()
	{
		if(tcpSocket == null)
			return false;
		
		return !tcpSocket.isClosed();
	}
	
	private String waitForResponse(List<String> queue)
	{
		int messageQueueSize = queue.size();
		
		synchronized(queue){
			while(messageQueueSize +1 != queue.size() && !tcpSocket.isClosed()){	// recognize spurious wakeup
				try {
					queue.wait();	// note that wait relinquishes any and all synchronization claims on this object
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		if(messageQueueSize +1 != queue.size()){
			return null;
		}
		
		return queue.get(messageQueueSize);
	}
	
	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		//Client client = new Client("test", new Config("client"), System.in, System.out);	// for debugging

		Client client = new Client(args[0], new Config("client"), System.in,
				System.out);
		// TODO: start the client
		new Thread(client).start();
		
		/* as the application only uses user-threads, the jvm will wait till all threads are finished =>
		 * it is not neccessary to wait (thread.join();) for them
		 */
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	@Command
	public String authenticate(String username) throws IOException {
		createTcpServerSocket();

		// TODO Auto-generated method stub
		SecureRandom secureRandom = new SecureRandom();
		final byte[] clientChallenge = new byte[32];
		secureRandom.nextBytes(clientChallenge);

		byte[] clientChallengeB64 = Base64.encode(clientChallenge);

		String plainTextMessage = String.format("!authenticate %s %s", username, new String(clientChallengeB64));

		File fpubkey = new File("keys/client/chatserver.pub.pem");
		PublicKey serverPublicKey = Keys.readPublicPEM(fpubkey);
		PrivateKey clientPrivateKey = Keys.readPrivatePEM(new File(config.getString("keys.dir") + "/" + username + ".pem"));

		try {
			// Initialize the ciphers for the first 2 messages (!authenticate and !ok)
			outputCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
			outputCipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);

			inputCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
			inputCipher.init(Cipher.DECRYPT_MODE, clientPrivateKey);

			write(plainTextMessage);

			// Wait for the server to respond
			String serverResponse = waitForResponse(commandResponseQueue);

			System.out.println("Got Response From Server: [" + serverResponse + "]");

			String[] response = serverResponse.split(" ");
			if(response.length != 4) {
				throw new RuntimeException("WRONG COUNT OF !ok PARAMS");
			}

			byte[] retClientChallenge = Base64.decode(response[0].getBytes());
			byte[] serverChallenge = Base64.decode(response[1].getBytes());
			byte[] secretKeyBytes = Base64.decode(response[2].getBytes());
			byte[] ivParameter = Base64.decode(response[3].getBytes());

			if(!Arrays.equals(retClientChallenge, clientChallenge)) {
				throw new RuntimeException("RETURNED CLIENT CHALLENGE DOES NOT MATCH");
			}


			// Intialize the ciphers for all other messages
			IvParameterSpec params = new IvParameterSpec(ivParameter);
			SecretKey secretKey = new SecretKeySpec(secretKeyBytes, 0, secretKeyBytes.length, "AES");

			outputCipher = Cipher.getInstance("AES/CTR/NoPadding");
			outputCipher.init(Cipher.ENCRYPT_MODE, secretKey,params);

			inputCipher = Cipher.getInstance("AES/CTR/NoPadding");
			inputCipher.init(Cipher.DECRYPT_MODE, secretKey,params);

			System.out.println("Sending the server the challenge");
			write(Base64.encode(serverChallenge));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}

		return null;
	}
}
