package de.learnlib.ralib.tools.sockanalyzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

// Wrapper around the java Socket so we have clear segmentation of inputs and outputs
public class SocketWrapper {
	private static Map<Integer, Socket> socketMap = new HashMap<Integer, Socket>();
	protected Socket sock;
	protected PrintWriter sockout;
	protected BufferedReader sockin;
	

	public SocketWrapper(String sutIP, int sutPort) {
		try {
			if(socketMap.containsKey(sutPort)) {
				sock = socketMap.get(sutPort);
			} else {
				sock = new Socket(InetAddress.getByName(sutIP), sutPort);
				socketMap.put(sutPort, sock);
				Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
					public void run() {
						SocketWrapper.this.close();
					}
				}));
			}
			sockout = new PrintWriter(sock.getOutputStream(), true);
			sockin = new BufferedReader(new InputStreamReader(
					sock.getInputStream()));
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public SocketWrapper(int sutPort) {
		this("localhost", sutPort);
	}

	public void writeInput(String input) {
	    if (sockout != null) {
		sockout.println(input);
		sockout.flush();
	    }
	}

	public String readOutput() {
		String output = null;
		try {
			output = sockin.readLine();
			if (output == null) {
				throw new RuntimeException("socket closed!");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output;
	}

	public void close() {
	    if (sockout != null) {
		//sockout.write("exit");
		try {
			if (!sock.isClosed())
				sock.close();
		} catch (IOException ex) {

		}
	    }
	}
}
