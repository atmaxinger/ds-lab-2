package client.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import chatserver.tcp.TCPHandlerThread;
import cli.Shell;

public class PrivateTcpListnerThread implements Runnable{

	private ServerSocket privateTcpServerSocket;
	private Shell shell;
	
	public PrivateTcpListnerThread(ServerSocket privateTcpServerSocket, Shell shell)
	{
		this.shell = shell;
		this.privateTcpServerSocket = privateTcpServerSocket;
	}
	
	@Override
	public void run() {
			
		while (!privateTcpServerSocket.isClosed() && !Thread.interrupted()) {
			
			try (	// try with resources (implicit finally with socket.close(), reader.close(), writer.close())
				// wait for Client to connect
				Socket socket = privateTcpServerSocket.accept();
				// prepare the input reader for the socket
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				// prepare the writer for responding to clients requests
				PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
			) {
				String privateMessage = reader.readLine();
				shell.writeLine(privateMessage);
				writer.println("!ack");
			}
			catch(SocketException e)
			{
				// thrown if socket is closed
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
