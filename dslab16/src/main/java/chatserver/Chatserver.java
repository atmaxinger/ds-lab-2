package chatserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import chatserver.tcp.TCPListenerThread;
import chatserver.udp.UDPHandlerThread;
import chatserver.udp.UDPListenerThread;
import cli.Command;
import cli.Shell;
import entity.User;
import util.Config;

public class Chatserver implements IChatserverCli, Runnable {

	private String componentName;
	private Config config;
	private Shell shell;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private ServerSocket serverSocketTCP;
	private DatagramSocket serverSocketUDP;
	private Thread tcpListenerThread;
	private Thread shellThread;
	private List<User> userList;
	
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
	public Chatserver(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		
		createUserList();
		
		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
	}

	@Override
	public void run() {
		// TODO
		
		shellThread = new Thread(shell);
		shellThread.start();
		
		/* create and start a new TCP ServerSocket */
		try {
			
			serverSocketTCP = new ServerSocket(config.getInt("tcp.port"));
			
			// handle incoming connections from client in a separate thread
			tcpListenerThread = new Thread(new TCPListenerThread(serverSocketTCP,this));
			tcpListenerThread.start();
			
		} catch (IOException e) {
			try {
				shell.writeLine("Cannot listen on TCP port.");
				exit();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		/* create and start DatagramSocket */
		try {
			serverSocketUDP = new DatagramSocket(config.getInt("udp.port"));
			
			// handle incoming connections from client in a separate thread
			UDPListenerThread udpListenerThread = new UDPListenerThread(serverSocketUDP,this);
			new Thread(udpListenerThread).start();
			
		} catch (SocketException e) {
			try {
				shell.writeLine("Cannot listen on UDP port.");
				exit();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
	}

	@Override
	@Command
	public String users() throws IOException {
		
		String userStatusList = "";
		int count = 1;
		
		for(User u: userList)
		{
			String status = "offline";
			
			synchronized (u) {	
				if(u.isActive())
				{
					status = "online";
				}
				
				if(count == userList.size()){
					userStatusList += String.format("%d. %s %s", count, u.getUsername(), status); 
				}else{
					userStatusList += String.format("%d. %s %s%n", count, u.getUsername(), status);
				}
			}
			count ++;
		}
		
		return userStatusList;
	}

	@Override
	@Command
	public String exit() throws IOException {
		
		/* close threads responsible for communicating with clients */
		if(userList != null){
			for(User u: userList)
			{
				if(u.getSocket() != null)
				{
					u.getSocket().close();	// this will throw a SocketException in the threads
				}
			}
		}
		
		/* close tcp server socket => tcp listener thread will terminate */
		if(serverSocketTCP != null){
			serverSocketTCP.close();
		}
	
		/* close udp server socket => udp listener thread and udp handler thread will terminate */
		if(serverSocketUDP != null){
			serverSocketUDP.close();
		}
		
		/* terminate shell thread */
		if(shell != null){
			shell.close();
			userRequestStream.close();
			userResponseStream.close();
		}
	
		return null;
	}
	
	
	private void createUserList()
	{
		Config userConfig = new Config("user");
		
		userList = new ArrayList<User>();
		
		for(String username: userConfig.listKeys())
		{
			User u = new User();
			u.setUsername(username.substring(0, username.lastIndexOf('.')));
			u.setPassword(userConfig.getString(username));
			u.setActive(false);
			u.setRegistered(false);
			userList.add(u);
		}
		
		/* sort list alphabetically */
		Collections.sort(userList, new Comparator<User>() {
		        @Override
		        public int compare(User u1, User u2) {
		            return u1.getUsername().compareToIgnoreCase(u2.getUsername());	// compare strings
		        }
		    });
	}
	
	public List<User> getUserList()
	{
		return userList;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Chatserver}
	 *            component
	 */
	public static void main(String[] args) {
		Chatserver chatserver = new Chatserver(args[0],
				new Config("chatserver"), System.in, System.out);
		// TODO: start the chatserver
		Thread chatserverThread = new Thread(chatserver);
		chatserverThread.start();
	}
}
