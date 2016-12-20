package nameserver;

import java.io.*;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import cli.Command;
import cli.Shell;
import entity.User;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;
import util.Config;

/**
 * Please note that this class is not needed for Lab 1, but will later be used
 * in Lab 2. Hence, you do not have to implement it for the first submission.
 */
public class Nameserver implements INameserverCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private Shell shell;
	private Registry registry;
	private NameserverRemote nameserverRemote;
	private INameserverForChatserver nameserverForChatserver;

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
	public Nameserver(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
	}

	@Override
	public void run() {

		new Thread(shell).start();

		nameserverRemote = new NameserverRemote(shell);
		nameserverForChatserver = new NameserverForChatserverImpl();

		if(!config.listKeys().contains("domain")) {

			System.out.println("start root server");

			try {
				// create and export the registry instance on localhost at the
				// specified port
				registry = LocateRegistry.createRegistry(config
						.getInt("registry.port"));

				// create a remote object of this server object
				INameserver remote = (INameserver) UnicastRemoteObject
						.exportObject(nameserverRemote, 0);

				// bind the obtained remote object on specified binding name in the registry
				registry.bind(config.getString("root_id"), remote);
			} catch (RemoteException e) {
				throw new RuntimeException("Error while starting server.", e);
			} catch (AlreadyBoundException e) {
				throw new RuntimeException(
						"Error while binding remote object to registry.", e);
			}

			System.out.println("root server started");
		}
		else {
			System.out.println("start name server");

			try {
				// obtain registry that was created by the server
				Registry registry = LocateRegistry.getRegistry(
						config.getString("registry.host"),
						config.getInt("registry.port"));

				// look for the bound server remote-object implementing the INameserver interface
				INameserver remoteNameserver = (INameserver) registry.lookup(config
						.getString("root_id"));


				INameserver myremote = ((INameserver) UnicastRemoteObject.exportObject(nameserverRemote, 0));
				// register the new nameserver
				remoteNameserver.registerNameserver(config.getString("domain"), myremote,myremote);

			} catch (RemoteException e) {
				throw new RuntimeException(
						"Error while obtaining registry/server-remote-object.", e);
			} catch (NotBoundException e) {
				throw new RuntimeException(
						"Error while looking for server-remote-object.", e);
			} catch (InvalidDomainException e) {
				e.printStackTrace();
			} catch (AlreadyRegisteredException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	@Command
	public String nameservers() throws IOException {

		String result="";

		String[] domains = nameserverRemote.getDomains().toArray(new String[nameserverRemote.getDomains().size()]);

		Arrays.sort(domains);

		int i = 0;

		for (String domain: domains) {
			result += String.format("%d. %s%n",i,domain);
		}

		return result;
	}

	@Override
	@Command
	public String addresses() throws IOException {
		// TODO Auto-generated method stub
		return "called addresses";
	}

	@Override
	@Command
	public String exit() throws IOException {
		// TODO Auto-generated method stub
		return "called exit";
	}


	/**
	 * @param args
	 *            the first argument is the name of the {@link Nameserver}
	 *            component
	 */
	public static void main(String[] args) {
		Nameserver nameserver = new Nameserver(args[0], new Config(args[0]),
				System.in, System.out);
		// TODO: start the nameserver
		new Thread(nameserver).start();
	}
}
