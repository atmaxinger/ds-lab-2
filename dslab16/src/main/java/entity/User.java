package entity;

import java.net.Socket;

public class User {
	
	private String username;
	private String password;
	private boolean active;
	private Socket socket;
	private boolean registered;
	private String ip;
	private int port;
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
	public void setSocket(Socket socket){
		this.socket = socket;
	}
	
	public Socket getSocket(){
		return socket;
	}
	
	public boolean isRegistered() {
		return registered;
	}

	public void setRegistered(boolean registered) {
		this.registered = registered;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
