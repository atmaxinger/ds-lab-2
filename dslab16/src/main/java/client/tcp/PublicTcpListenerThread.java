package client.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

import cli.Shell;

public class PublicTcpListenerThread implements Runnable{
	
	private Socket socket;
	private BufferedReader serverReader;
	private List<String> publicMessageQueue;
	private List<String> commandResponseQueue;
	private Shell shell;
	
	private final String COMMAND_RESPONSE_PREFIX = "!command_response";
	private final String PUBLIC_MESSAGE_PREFIX = "!public_message";

	
	public PublicTcpListenerThread(Socket socket, BufferedReader serverReader, List<String> publicMessageQueue, List<String> commandResponseQueue, Shell shell) {
		this.socket = socket;
		this.serverReader = serverReader;
		this.publicMessageQueue = publicMessageQueue;
		this.commandResponseQueue = commandResponseQueue;
		this.shell = shell;
	}


	@Override
	public void run() {
		
		String message;
		
		try {
			while((message = serverReader.readLine()) != null)	// will read null if server terminates
			{	
				
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
