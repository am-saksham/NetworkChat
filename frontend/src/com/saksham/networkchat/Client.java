package com.saksham.networkchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.security.SecureRandom;

public class Client {

	private static final long serialVersionUID = 1L;
	private String name, address;
	private int port;
	
	// TCP Components
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	
	private Thread send;
	private int ID = -1;
	
	public Client(String name, String address, int port) {
		this.name = name;
		this.address = address;
		this.port = port;
	}
	
	public String getName() {
		return name;
	}
	
	public String getAddress() {
		return address;
	}
	
	public int getPort() {
		return port;
	}


	public boolean openConnection(String address) {
		try {
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[]{
				new X509TrustManager() {
					public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
					public void checkClientTrusted(X509Certificate[] certs, String authType) {}
					public void checkServerTrusted(X509Certificate[] certs, String authType) {}
				}
			};

			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new SecureRandom());
			SSLSocketFactory ssf = sc.getSocketFactory();
			
			socket = ssf.createSocket(address, port);
			// Setup streams
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public String receive() {
		try {
			// This blocks until a line is received
			String line = in.readLine();
			return line;
		} catch (IOException e) {
			return null;
		}
	}

	public void send(final String message) {
		// Just write to stream
		if(out != null) {
			out.println(message);
		}
	}
	
	// Overload for bytes (legacy support, converts to string)
	public void send(final byte[] data) {
		String message = new String(data);
		send(message);
	}
	
	public void close() {
		try {
			if(socket != null) socket.close();
			if(in != null) in.close();
			if(out != null) out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setID(int ID) {
		this.ID = ID;
	}
	
	public int getID() {
		return ID;
	}

}
