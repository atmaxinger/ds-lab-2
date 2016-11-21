package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import server.IServer;
import util.Config;

/**
 * Client application that invokes the RemoteObject provided by the Server.
 */
public class Client implements Runnable {

	private Config config;
	private String name;
	private IServer server;

	public Client(Config config) {
		this.config = config;
		this.name = config.getString("name");
	}

	@Override
	public void run() {

		try {
			// obtain registry that was created by the server
			Registry registry = LocateRegistry.getRegistry(
					config.getString("registry.host"),
					config.getInt("registry.port"));
			// look for the bound server remote-object implementing the IServer
			// interface
			server = (IServer) registry.lookup(config
					.getString("server.binding.name"));
		} catch (RemoteException e) {
			throw new RuntimeException(
					"Error while obtaining registry/server-remote-object.", e);
		} catch (NotBoundException e) {
			throw new RuntimeException(
					"Error while looking for server-remote-object.", e);
		}

		BufferedReader userInputReader = null;
		try {
			// create reader to read from console
			userInputReader = new BufferedReader(new InputStreamReader(
					System.in));

			System.out.println("Client: " + name + " is up! Enter command.");

			while (true) {
				// read input from console
				String input = userInputReader.readLine();

				// in case "!stop" was entered (or the end of the stream has
				// been reached) shut down client
				if (input == null || input.startsWith("!stop")) {
					break;
				} else if (input.startsWith("!ping")) {
					// use server's exposed method for ping, sending the client
					// name and receiving the server response
					String response = server.ping(name);

					System.out.println("Received response from Server: "
							+ response);
				} else {
					System.out.println("Command not known!");
				}

			}
		} catch (RemoteException e) {
			System.out
					.println("An error occurred while communicating with the server: "
							+ e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getClass() + ": " + e.getMessage());
		} finally {

			if (userInputReader != null)
				try {
					userInputReader.close();
				} catch (IOException e) {
					// Ignored because we cannot handle it
				}
		}
	}

	public static void main(String[] args) {
		new Client(new Config(args[0])).run();
	}

}
