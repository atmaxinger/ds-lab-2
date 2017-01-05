package client.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import cli.Shell;
import util.IntegrityValidator;

public class PrivateTcpListenerThread implements Runnable{

	private ServerSocket privateTcpServerSocket;
	private Shell shell;
	
	public PrivateTcpListenerThread(ServerSocket privateTcpServerSocket, Shell shell)
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

				String[] segments = privateMessage.split(" ", 2);

				if (segments.length == 2) {

					try {
						shell.writeLine(segments[1].substring(segments[1].indexOf(' ') + 1));

						if (IntegrityValidator.isMessageUntampered(segments[0], segments[1])) {
							writer.println(IntegrityValidator.generateHMAC("!ack") + " !ack");
							System.out.println(IntegrityValidator.generateHMAC("!ack") + " !ack");
						} else {
							writer.println(IntegrityValidator.generateHMAC("!tampered " + segments[1]) + " !tampered " + segments[1]);
							System.out.println(IntegrityValidator.generateHMAC("!tampered " + segments[1]) + " !tampered " + segments[1]);
						}
					} catch (Exception e) {
						e.printStackTrace();
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
		}
	}
}
