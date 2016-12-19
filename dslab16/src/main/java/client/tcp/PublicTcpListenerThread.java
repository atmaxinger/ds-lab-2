package client.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

import cli.Shell;
import client.Client;

import javax.crypto.Cipher;

public class PublicTcpListenerThread implements Runnable{
	
	private Socket socket;
	private BufferedReader serverReader;
	private List<String> publicMessageQueue;
	private List<String> commandResponseQueue;
	private Shell shell;
	private Client client;
	
	private final String COMMAND_RESPONSE_PREFIX = "!command_response";
	private final String PUBLIC_MESSAGE_PREFIX = "!public_message";
	private final String OK_PREFIX = "!ok";

	
	public PublicTcpListenerThread(Socket socket, BufferedReader serverReader, List<String> publicMessageQueue, List<String> commandResponseQueue, Shell shell, Client client) {
		this.socket = socket;
		this.serverReader = serverReader;
		this.publicMessageQueue = publicMessageQueue;
		this.commandResponseQueue = commandResponseQueue;
		this.shell = shell;
		this.client = client;
	}


	@Override
	public void run() {
		
		String ba64msg;
		
		try {
			while((ba64msg = serverReader.readLine()) != null)	// will read null if server terminates
			{
				String message = client.decodeMessage(ba64msg);

				if(message.startsWith(COMMAND_RESPONSE_PREFIX))
				{
					synchronized (commandResponseQueue) {
						commandResponseQueue.add(message.replaceFirst(COMMAND_RESPONSE_PREFIX, ""));
						commandResponseQueue.notify();
					}
				}
				else if(message.startsWith(PUBLIC_MESSAGE_PREFIX))
				{
					message = message.replaceFirst(PUBLIC_MESSAGE_PREFIX, "");
					synchronized (publicMessageQueue) {
						publicMessageQueue.add(message);	
					}
					
					shell.writeLine(message);
				}
				else if(message.startsWith(OK_PREFIX)) {
					message = message.replaceFirst(OK_PREFIX, "").trim();
					synchronized (commandResponseQueue) {
						commandResponseQueue.add(message);
						commandResponseQueue.notify();
					}
				}
			}
		}
		catch(SocketException e)
		{
			// thrown if socket is closed
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		if(!socket.isClosed())
		{
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/* notify commandResponseQueue => command is not blocking */
		synchronized (commandResponseQueue) {
			commandResponseQueue.notify();
		}
	}
}
