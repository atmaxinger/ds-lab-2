package chatserver;

import java.net.Socket;

import entity.User;

public class CommandHandler {

	private Chatserver chatserver;
	
	/* response messages */
	private final String COMMAND_RESPONSE_PREFIX = "!command_response";
	private final String PUBLIC_MESSAGE_PREFIX = "!public_message";
	private final String WRONG_USERNAME_OR_PASSWORD = "Wrong username or password.";
	private final String ALREADY_LOGGED_IN = "Already logged in.";
	private final String SUCESSFULLY_LOGGED_IN = "Successfully logged in.";
	private final String SUCESSFULLY_LOGGED_OUT = "Successfully logged out.";
	private final String WRONG_USER_OR_NOT_REGISTERED = "Wrong username or user not registered.";
	private final String SUCCESSFULLY_REGISTERED_ADDRESS = "Successfully registered address for";
	private final String UNKNONWN_COMMAND = "Unknown Command";
	
	public CommandHandler(Chatserver chatserver) {
		this.chatserver = chatserver;
	}

	public boolean login(String username, Socket socket){
		for(User u:chatserver.getUserList())
		{
			synchronized(u){	// for example necessary if two users log in simultaneously with the same username
				if(u.getUsername().equals(username))
				{
					u.setActive(true);
					u.setSocket(socket);

					return true;
				}
			}
		}

		return false;
	}
	
	public User getUser(String username)
	{
		for(User u:chatserver.getUserList())
		{
			if(u.getUsername().equals(username))
			{
				return u;
			}
		}
		
		return null;
	}
	
	public String logout(User user){
		
		synchronized (user) {
			user.setActive(false);
			user.setRegistered(false);
		}
		
		return addCommandResponsePrefix(SUCESSFULLY_LOGGED_OUT);
	}
	
	public void send(String message, User user){
		for(User u:chatserver.getUserList())
		{
			synchronized (u) {
				
				/* check if user is active and user is not sending user */
				if(u.isActive() && !user.equals(u))
				{
					u.getHandlerThread().write(String.format("%s%s: %s%n",PUBLIC_MESSAGE_PREFIX, user.getUsername(), message));
				}
			}
		}
	}
	
	public String lookup(String username){

		for(User u:chatserver.getUserList())
		{
			synchronized (u) {
				if(u.isRegistered() && u.getUsername().equals(username))
				{
					return addCommandResponsePrefix(String.format("%s:%d%n",u.getIp(), u.getPort()));
				}
			}
		}
		
		return addCommandResponsePrefix(WRONG_USER_OR_NOT_REGISTERED);
	}
	
	public String register(User user, String address, int port){
		
		synchronized (user) {
			user.setRegistered(true);
			user.setIp(address);
			user.setPort(port);
		}
		
		return addCommandResponsePrefix(String.format("%s %s.%n",SUCCESSFULLY_REGISTERED_ADDRESS, user.getUsername()));
	}

	/* list is the only service that is offered over UDP */
	public String list() {

		String onlineList = "Online users:";
		
		for(User u: chatserver.getUserList()){
			synchronized (u) {
				if(u.isActive()){
					onlineList += String.format("%n* %s", u.getUsername());
				}	
			}
		}
		
		return addCommandResponsePrefix(onlineList);
	}
	
	public String unknownCommand(){
		return addCommandResponsePrefix(UNKNONWN_COMMAND);
	}
	
	private String addCommandResponsePrefix(String message){
		return String.format("%s%s",COMMAND_RESPONSE_PREFIX,message);
	}
	
}
