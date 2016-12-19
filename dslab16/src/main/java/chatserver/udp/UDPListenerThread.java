package chatserver.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import chatserver.Chatserver;

public class UDPListenerThread implements Runnable{

	private DatagramSocket socket;
	private Chatserver server;

	public UDPListenerThread(DatagramSocket socket, Chatserver server) {
		this.socket = socket;
		this.server = server;
	}


	@Override
	public void run() {
		
		/* pool will cache finished threads for 60s and reuse them if a new task comes in
		 * Use CachedThreadPool because at this point we have many short-lived tasks 
		 */
		ExecutorService pool = Executors.newCachedThreadPool();
		
		while(!socket.isClosed()){
			try {
				byte[] buf = new byte[1024];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				
				socket.receive(packet);	// wait for packet from client
				
				/* handle packet form client in separate thread */
				UDPHandlerThread handlerThread = new UDPHandlerThread(packet, socket, server);
				pool.execute(handlerThread);
				
			}catch(SocketException e){
				// thrown if socket is closed
				pool.shutdown();
			} 
			catch (IOException e) {
				pool.shutdown();
				e.printStackTrace();
			}
		}
		
		
	}
	
	

}
