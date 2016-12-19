package chatserver.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import chatserver.Chatserver;
import chatserver.CommandHandler;

public class UDPHandlerThread implements Runnable {

	private DatagramPacket packet;
	private DatagramSocket socket;
	private Chatserver chatserver;
	private CommandHandler commandHelper;
	

	public UDPHandlerThread(DatagramPacket packet, DatagramSocket socket, Chatserver chatserver) {
		this.packet = packet;
		this.socket = socket;
		this.chatserver = chatserver;
		
		commandHelper = new CommandHandler(chatserver);
	}

	@Override
	public void run() {
	
		InetAddress address = packet.getAddress();	// address of the client
		int port = packet.getPort();	// port of the client
		String request = new String(packet.getData());
		
		String response;
		
		if(request.startsWith("!list")){
			
			response = commandHelper.list();
		}
		else
		{
			response = commandHelper.unknownCommand();
		}
		
		byte[] buf = response.getBytes();
		packet = new DatagramPacket(buf,buf.length,address,port);

		try {
			socket.send(packet);
		} catch(SocketException e){
			// thrown if socket closed
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}

}
