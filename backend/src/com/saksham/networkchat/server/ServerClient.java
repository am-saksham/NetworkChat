package com.saksham.networkchat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerClient implements Runnable {
	
	public String name;
	public int ID;
	
	// TCP Components
	public Socket socket;
	public PrintWriter out;
	public BufferedReader in;
	
	// Stats
	public int attempt = 0;
	
	private Thread run;
	private boolean running = false;
	
	public ServerClient(String name, Socket socket, int ID) {
		this.name = name;
		this.socket = socket;
		this.ID = ID;
		
		try {
			// Initialize Streams
			out = new PrintWriter(socket.getOutputStream(), true); // Auto-flush
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			// Start listening thread
			run = new Thread(this, "Client-" + ID);
			running = true;
			run.start();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int getID() {
		return ID;
	}
	
	public void run() {
		while(running) {
			try {
				String msg = in.readLine();
				if(msg == null) {
					// Socket closed by client
					Server.disconnect(ID, true);
					running = false;
					break;
				}
				// Pass message to Server for processing
				Server.process(ID, msg);
				
			} catch (IOException e) {
				// Connection lost/reset
				Server.disconnect(ID, true);
				running = false;
				break;
			}
		}
	}
	
	public void send(String message) {
		if(out != null) {
			out.println(message);
		}
	}
	
	public void close() {
		running = false;
		try {
			if(socket != null) socket.close();
			if(in != null) in.close();
			if(out != null) out.close();
		} catch(Exception e) {}
	}
}
