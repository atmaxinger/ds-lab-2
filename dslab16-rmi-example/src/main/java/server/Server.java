package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import util.Config;

/**
 * Server application that creates a RemoteObject and provides it via the
 * Registry.
 */
public class Server implements Runnable, IServer {

	private Config config;
	private Registry registry;

	public Server(Config config) {
		this.config = config;
	}

	@Override
	public void run() {

		try {
			// create and export the registry instance on localhost at the
			// specified port
			registry = LocateRegistry.createRegistry(config
					.getInt("registry.port"));

			// create a remote object of this server object
			IServer remote = (IServer) UnicastRemoteObject
					.exportObject(this, 0);
			// bind the obtained remote object on specified binding name in the
			// registry
			registry.bind(config.getString("binding.name"), remote);
		} catch (RemoteException e) {
			throw new RuntimeException("Error while starting server.", e);
		} catch (AlreadyBoundException e) {
			throw new RuntimeException(
					"Error while binding remote object to registry.", e);
		}

		System.out.println("Server is up! Hit <ENTER> to exit!");
		// create reader to read commands from console
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		try {
			// read commands from console
			reader.readLine();
		} catch (IOException e) {
			// IOException from System.in is very very unlikely (or impossible)
			// and cannot be handled
		}

		// shut down server and clean resources
		close();
	}

	public void close() {
		try {
			// unexport the previously exported remote object
			UnicastRemoteObject.unexportObject(this, true);
		} catch (NoSuchObjectException e) {
			System.err.println("Error while unexporting object: "
					+ e.getMessage());
		}

		try {
			// unbind the remote object so that a client can't find it anymore
			registry.unbind(config.getString("binding.name"));
		} catch (Exception e) {
			System.err.println("Error while unbinding object: "
					+ e.getMessage());
		}
	}

	/*
	 * Implements the interface server.IServer#ping(java.lang.String).
	 */
	@Override
	public String ping(String clientName) throws RemoteException {
		System.out.println("Received request from Client: " + clientName);
		return "!pong " + clientName;
	}

	public static void main(String[] args) {
		new Server(new Config("server")).run();
	}

}
