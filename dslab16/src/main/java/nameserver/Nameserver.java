package nameserver;

import java.io.*;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
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

    /**
     * @param componentName      the name of the component - represented in the prompt
     * @param config             the configuration to use
     * @param userRequestStream  the input stream to read user input from
     * @param userResponseStream the output stream to write the console output to
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

        if (!config.listKeys().contains("domain")) {
            /* root nameserver */

            try {
                registry = LocateRegistry.createRegistry(config
                        .getInt("registry.port"));

                INameserver rootNameserver = (INameserver) UnicastRemoteObject
                        .exportObject(nameserverRemote, 0);

                // bind rootNameserver object on specified binding name in the registry
                registry.bind(config.getString("root_id"), rootNameserver);

            } catch (RemoteException e) {
                throw new RuntimeException("Error while starting server.", e);
            } catch (AlreadyBoundException e) {
                throw new RuntimeException(
                        "Error while binding remote object to registry.", e);
            }

        } else {
            /* non-root nameserver */

            try {

                Registry registry = LocateRegistry.getRegistry(
                        config.getString("registry.host"),
                        config.getInt("registry.port"));

                INameserver rootNameserver = (INameserver) registry.lookup(config
                        .getString("root_id"));


                INameserver stub = ((INameserver) UnicastRemoteObject.exportObject(nameserverRemote, 0));

                rootNameserver.registerNameserver(config.getString("domain"), stub, stub);

            } catch (RemoteException e) {
                throw new RuntimeException(
                        "Error while obtaining registry/server-remote-object.", e);
            } catch (NotBoundException e) {
                throw new RuntimeException(
                        "Error while looking for server-remote-object.", e);
            } catch (InvalidDomainException | AlreadyRegisteredException e) {
                try {
                    shell.writeLine(e.getMessage());
                    exit();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    @Override
    @Command
    public String nameservers() throws IOException {

        String result = "";

        String[] domains = nameserverRemote.getDomains().toArray(new String[nameserverRemote.getDomains().size()]);

        Arrays.sort(domains);    // sort domains in alphabetical order

        int i = 1;

        for (String domain : domains) {
            result += String.format("%d. %s%n", i, domain);
            i++;
        }

        return result;
    }

    @Override
    @Command
    public String addresses() throws IOException {

        String result = "";

        List<User> userList = nameserverRemote.getUserList();

		/* sort user list alphabetically */
        Collections.sort(userList, new Comparator<User>() {
            @Override
            public int compare(User u1, User u2) {
                return u1.getUsername().compareToIgnoreCase(u2.getUsername());    // compare strings
            }
        });

        int i = 1;

        for (User user : userList) {
            result += String.format("%d. %s %s:%d%n", i, user.getUsername(), user.getIp(),user.getPort());
            i++;
        }

        return result;
    }

    @Override
    @Command
    public String exit() throws IOException {

        /* unexport the previously exported remote object */
        try {
            UnicastRemoteObject.unexportObject(nameserverRemote, true);
        } catch (NoSuchObjectException e) {
            e.printStackTrace();
        }

        /* unbind the remote object so that a client can't find it anymore */
        try {
            if(registry != null) {
                registry.unbind(config.getString("root_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* terminate shell thread */
        if(shell != null){
            shell.close();
            userRequestStream.close();
            userResponseStream.close();
        }

        return null;
    }


    /**
     * @param args the first argument is the name of the {@link Nameserver}
     *             component
     */
    public static void main(String[] args) {
        Nameserver nameserver = new Nameserver(args[0], new Config(args[0]),
                System.in, System.out);

        new Thread(nameserver).start();
    }
}
