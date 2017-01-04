package nameserver;

import cli.Shell;
import entity.User;
import nameserver.exceptions.AlreadyRegisteredException;
import nameserver.exceptions.InvalidDomainException;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.*;

public class NameserverRemote implements INameserver, Serializable {

    private Shell shell;
    private HashMap<String, INameserver> nameserverHashMap;
    private HashMap<String, INameserverForChatserver> nameserverForChatserverHashMap;
    private List<User> userList;

    private final String WRONG_USER_OR_NOT_REGISTERED = "Wrong username or user not registered.";


    public NameserverRemote(Shell shell) {
        this.shell = shell;
        nameserverHashMap = new HashMap<>();
        nameserverForChatserverHashMap = new HashMap<>();
        userList = new ArrayList<>();
    }

    @Override
    public void registerNameserver(String domain, INameserver nameserver, INameserverForChatserver nameserverForChatserver) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {

        String domainParts[] = domain.split("\\.");

        if (domainParts.length == 1) {

            synchronized (nameserverHashMap) {

                if (nameserverHashMap.containsKey(domain)) {
                    throw new AlreadyRegisteredException(String.format("'%s' has already been registered", domain));
                }

                nameserverHashMap.put(domain, nameserver);
            }

            synchronized (nameserverForChatserverHashMap){
                nameserverForChatserverHashMap.put(domain, nameserverForChatserver);
            }

            try {
                shell.writeLine(String.format("Registering nameserver for zone ’%s’%n", domain));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (domainParts.length > 1) {

            INameserver topNameserver = getTopNameserver(domainParts);

            String cutDomain = cutTopDomain(domainParts);

            topNameserver.registerNameserver(cutDomain, nameserver, nameserverForChatserver);

        }
    }

    @Override
    public void registerUser(String username, String address) throws RemoteException, AlreadyRegisteredException, InvalidDomainException {

        String usernameParts[] = username.split("\\.");

        if (usernameParts.length == 1) {

            synchronized (userList) {
                for (User u : userList) {
                    if (u.getUsername().equals(username)) {
                        throw new AlreadyRegisteredException(String.format("User %s is already registered.", username));
                    }
                }
            }

            User user = new User();
            user.setUsername(username);

            String addressParts[] = address.split(":");
            user.setIp(addressParts[0]);
            user.setPort(Integer.parseInt(addressParts[1]));

            synchronized (userList) {
                userList.add(user);
            }

            try {
                shell.writeLine(String.format("Registering user ’%s’%n", username));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (usernameParts.length > 1) {
            INameserver topNameserver = getTopNameserver(usernameParts);

            String cutUsername = cutTopDomain(usernameParts);

            topNameserver.registerUser(cutUsername, address);
        }
    }

    @Override
    public INameserverForChatserver getNameserver(String zone) throws RemoteException {
        try {
            shell.writeLine(String.format("Nameserver for ’%s’ requested by chatserver%n", zone));
        } catch (IOException e) {
            e.printStackTrace();
        }

        synchronized (nameserverForChatserverHashMap){
            return nameserverForChatserverHashMap.get(zone);
        }
    }

    @Override
    public String lookup(String username) throws RemoteException {

        try {
            shell.writeLine(String.format("Address for user ’%s’ requested by chatserver%n", username));
        } catch (IOException e) {
            e.printStackTrace();
        }

        synchronized (userList) {
            for (User user : userList) {
                if (user.getUsername().equals(username)) {
                    return String.format("%s:%s", user.getIp(), user.getPort());
                }
            }
        }

        return WRONG_USER_OR_NOT_REGISTERED;
    }

    private String cutTopDomain(String[] domainParts) {
        String subdomain = "";

        for (int i = 0; i < domainParts.length - 1; i++) {
            subdomain += domainParts[i];

            if (i + 1 != domainParts.length - 1) {
                subdomain += ".";
            }
        }
        return subdomain;
    }

    private INameserver getTopNameserver(String[] domainParts) throws InvalidDomainException {

        String parentDomain = domainParts[domainParts.length - 1];

        INameserver parentNameserver;

        synchronized (nameserverHashMap) {
            parentNameserver = nameserverHashMap.get(parentDomain);
        }

        if (parentNameserver == null) {
            throw new InvalidDomainException(String.format("no nameserver for %s", parentDomain));
        }
        return parentNameserver;
    }

    public String getDomainPrintList() {
        String result = "";

        List<String> domains = new ArrayList<>();

        synchronized (nameserverHashMap) {

            domains.addAll(nameserverHashMap.keySet());

            /* sort list alphabetically */
            Collections.sort(domains, new Comparator<String>() {
                @Override
                public int compare(String d1, String d2) {
                    return d1.compareToIgnoreCase(d2);	// compare strings
                }
            });

            int i = 1;

            for (String domain : domains) {
                result += String.format("%d. %s%n", i, domain);
                i++;
            }
        }

        return result;
    }

    public String getUserPrintList() {

        String result = "";

        synchronized (userList) {

		    /* sort user list alphabetically */
            Collections.sort(userList, new Comparator<User>() {
                @Override
                public int compare(User u1, User u2) {
                    return u1.getUsername().compareToIgnoreCase(u2.getUsername());    // compare strings
                }
            });

            int i = 1;

            for (User user : userList) {
                result += String.format("%d. %s %s:%d%n", i, user.getUsername(), user.getIp(), user.getPort());
                i++;
            }
        }

        return result;
    }
}
