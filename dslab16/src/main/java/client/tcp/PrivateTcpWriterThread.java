package client.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import cli.Shell;
import util.Config;

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
		
			writer.println(message);
			shell.writeLine(String.format("%s replied with %s.%n", username, reader.readLine()));
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
