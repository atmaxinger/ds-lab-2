package client.tcp;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import cli.Shell;
import util.IntegrityValidator;

public class PrivateTcpWriterThread implements Runnable{

	private Socket socket;
	private String message;
	private String username;
	private String ip;
	private int port;
	private Shell shell;
	
	public PrivateTcpWriterThread(String message, String username, String ip, int port, Shell shell)
	{
		this.message = message;
		this.username = username;
		this.ip = ip;
		this.port = port;
		this.shell = shell;
	}
	
	@Override
	public void run(){

		BufferedReader reader = null;
		PrintWriter writer = null;
		
		try { 
			socket = new Socket(ip,port);
			// create a reader to retrieve !ack send by the the other client
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			// create a writer to send private messages to the the other client
			writer= new PrintWriter(socket.getOutputStream(), true);

			message = "!msg " + message;
			try {
				writer.println(IntegrityValidator.generateHMAC(message) + " " + message);

				String response = reader.readLine();
				String[] segments = response.split(" ",2);

				if (IntegrityValidator.isMessageUntampered(segments[0], segments[1])) {
					shell.writeLine(String.format("%s replied with '%s'.%n", username, segments[1]));
				} else {
					shell.writeLine(String.format("%s replied with tampered message '%s'.%n", username, segments[1]));
				}

			} catch (NoSuchAlgorithmException | InvalidKeyException e) {
				e.printStackTrace();
			}
		}
		catch (SocketException e){
			// thrown if socket is closed
		} catch (UnknownHostException e) {
			try {
				shell.writeLine(String.format("%s is an unknown host!", ip));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			
			try {
				if(socket != null && !socket.isClosed()){
					socket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void close() {
		try {
			if(socket != null){
				socket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
