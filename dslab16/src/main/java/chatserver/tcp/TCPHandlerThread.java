package chatserver.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.security.*;
import java.util.Arrays;

import org.bouncycastle.util.encoders.Base64;


import chatserver.Chatserver;
import chatserver.CommandHandler;
import entity.User;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;

public class TCPHandlerThread implements Runnable{

	private Socket socket;
	private User user;
	private CommandHandler commandHandler;
	private BufferedReader reader;
	private PrintWriter writer;

	private Chatserver chatserver;

	private Cipher inputCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
	private Cipher outputCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");


	public TCPHandlerThread(Socket socket, Chatserver chatserver) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
		this.socket = socket;	// socket for communicating with client
		commandHandler = new CommandHandler(chatserver);
		this.chatserver = chatserver;

		inputCipher.init(Cipher.DECRYPT_MODE, chatserver.getServerPrivateKey());
	}

	@Override
	public void run() {
		
		try {
			PublicKey clientPublicKey = null;

			byte[] serverChallenge = null;
			SecretKey secretKey = null;
			byte[] ivParameter = null;
			String _username = "";

			// prepare the input reader for the socket
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			// prepare the writer for responding to clients requests
			writer = new PrintWriter(socket.getOutputStream(), true);
		
			String requestB64;
		
			// read client requests
			while (socket.isClosed() == false && (requestB64 = reader.readLine()) != null) {
				byte[] encryptedRequest = Base64.decode(requestB64);
				String request = new String(inputCipher.doFinal(encryptedRequest));

				String[] parts = request.split("\\s");    // "\\s" is regex for single white space

				if (parts.length >= 1) {
				/* NOTE: the client checks if he is already logged in and if so he will not send a login request */

				/* login command */
//					if (request.startsWith("!login") && parts.length == 3) {
//
//						String username = parts[1];
//						String password = parts[2];
//						boolean alreadyLoggedIn = false;
//
//						user = commandHandler.getUser(username);
//
//						/* check if the user with the username sent by the client is already logged in */
//						if(user != null){
//							alreadyLoggedIn = user.isActive();
//						}
//
//						/* create and send response to server */
//						write(commandHandler.login(username,password, socket));
//
//						/* if login wasn't successful then close socket */
//						if(user == null || alreadyLoggedIn || user.isActive() == false)
//						{
//							socket.close();	// will end loop and therefore thread
//						}
//					}

					if (request.startsWith("!authenticate")) {
						_username = parts[1];
						String clientChallengeB64 = parts[2];

						clientPublicKey = chatserver.getPublicKeyForClient(_username);
						outputCipher.init(Cipher.ENCRYPT_MODE, clientPublicKey);

						SecureRandom secureRandom = new SecureRandom();
						serverChallenge = new byte[32];
						secureRandom.nextBytes(serverChallenge);

						byte[] serverChallengeB64 = Base64.encode(serverChallenge);

						KeyGenerator generator = KeyGenerator.getInstance("AES");
						generator.init(256);

						secretKey = generator.generateKey();
						ivParameter = new byte[16];

						secureRandom.nextBytes(ivParameter);

						byte[] secretKeyB64 = Base64.encode(secretKey.getEncoded());
						byte[] ivParameterB64 = Base64.encode(ivParameter);

						String message = String.format("!ok %s %s %s %s", clientChallengeB64, new String(serverChallengeB64), new String(secretKeyB64), new String(ivParameterB64));

						write(message);

						// Initialize the Ciphers for AES Encryption
						inputCipher = Cipher.getInstance("AES/CTR/NoPadding");
						outputCipher = Cipher.getInstance("AES/CTR/NoPadding");

						IvParameterSpec params = new IvParameterSpec(ivParameter);

						inputCipher.init(Cipher.DECRYPT_MODE, secretKey, params);
						outputCipher.init(Cipher.ENCRYPT_MODE, secretKey, params);
					}
					// 3. Meldung vom Client
					else if(!request.startsWith("!")){
						if(!Arrays.equals(Base64.decode(parts[0]), serverChallenge)) {
							throw new AuthExcetpion("CLIENT DIDNT REPLY RIGHT SERVER CHALLENGE");
						}
						else {
							boolean alreadyLoggedIn = false;

							user = commandHandler.getUser(_username);

							user.setHandlerThread(this);

							if(user != null){
								alreadyLoggedIn = user.isActive();
							}

							commandHandler.login(_username, socket);

							/* if login wasn't successful then close socket */
							if(user == null || alreadyLoggedIn || !user.isActive())
							{
								socket.close();	// will end loop and therefore thread
							}
						}
					}
				/* logout command */
					else if (request.startsWith("!logout")) {
						write(commandHandler.logout(user));
						socket.close();    // will end loop and therefore thread
					}

				/* send command*/
					else if (request.startsWith("!send") && parts.length >= 2) {
						String message = request.substring(request.indexOf(' ') + 1, request.length());
						commandHandler.send(message, user);
					}

				/* lookup command */
					else if (request.startsWith("!lookup") && parts.length == 2) {
						String username = parts[1];
						write(commandHandler.lookup(username));
					}

				/* register command */
					else if (request.startsWith("!register") && parts.length == 2) {
						String[] connectionParts = parts[1].split(":");
						String address = connectionParts[0];
						int port = Integer.parseInt(connectionParts[1]);    // client makes sure that port is an integer

						write(commandHandler.register(user, address, port));
					} else {
						write(commandHandler.unknownCommand());
					}

				}
				else {
					write(commandHandler.unknownCommand());
				}
			}
		}
		catch(SocketException e)
		{
			// thrown if socket is closed
		}
		catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			e.printStackTrace();
		}

		if(user!= null) {
			user.setActive(false);
		}
	}
	
	public void write(String message)
	{
		synchronized (socket) // make sure that a command response and a public message is not sent simultaneously 
		{
			try {
				byte[] encryptedMessage = outputCipher.doFinal(message.getBytes());
				byte[] base64Message = Base64.encode(encryptedMessage);

				writer.println(new String(base64Message));

			} catch (IllegalBlockSizeException e) {
				e.printStackTrace();
			} catch (BadPaddingException e) {
				e.printStackTrace();
			}
		}
	}
}
