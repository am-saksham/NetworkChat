package com.saksham.networkchat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Server implements Runnable {
	
	// Master list of all connected clients
	private static List<ServerClient> allClients = new ArrayList<ServerClient>();
	// Map of Room Name -> Room Object
	private static Map<String, Room> rooms = new HashMap<String, Room>();
	
	private ServerSocket serverSocket;
	private int port;
	private boolean running = false;
	private Thread run; // Main listener thread
	
	public Server(int port) {
		this.port = port;
		try {
			// SSL Properties
			System.setProperty("javax.net.ssl.keyStore", "server.keystore");
			System.setProperty("javax.net.ssl.keyStorePassword", "password123");
			
			javax.net.ssl.SSLServerSocketFactory ssf = (javax.net.ssl.SSLServerSocketFactory) javax.net.ssl.SSLServerSocketFactory.getDefault();
			serverSocket = ssf.createServerSocket(port);
			
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		// Create the default Global room
		rooms.put("Global", new Room("Global", "", -1, "Server"));
		
		run = new Thread(this, "Server");
		run.start();
	}

	public void run() {
		running = true;
		System.out.println("Server started on SSL port " + port);
		
		// Console input handler (Admin Commands)
		// Console input handler (Admin Commands)
		new Thread(() -> {
			try (Scanner scanner = new Scanner(System.in)) {
				while(running) {
					if(!scanner.hasNextLine()) break;
					String text = scanner.nextLine();
					
					if(!text.startsWith("/")) {
						sendToAll("/m/Server: " + text + "/e/");
						continue;
					}
					text = text.substring(1);
					if(text.equals("quit")) {
						quit();
					} else if(text.equals("help")) {
						printHelp();
					} else {
						System.out.println("Unknown command.");
					}
				}
			} catch(Exception e) {
				System.out.println("Console input disabled (Headless mode).");
			}
		}).start();
		
		// Listen for new connections
		while(running) {
			try {
				Socket clientSocket = serverSocket.accept();
				// Initial connection request will be handled by process() once client sends "/c/..."
				// However, we need to wrap this socket in a ServerClient immediately OR wait for handshake?
				// Logic: Create a temp handler or assume first message is connect.
				// Better: Create ServerClient immediately with a temporary ID, or wait for handshake.
				// Let's modify ServerClient to wait for handshake in its run() method?
				// No, let's stick to the protocol. The Client sends "/c/Name/e/" immediately.
				// We can't identify them yet.
				// Let's create a "Handshake Handler" or just let ServerClient handle it.
				
				// New Approach: ServerClient starts, reads first line. If it's CONNECT, it sets ID.
				// But we need UniqueIdentifier. Let's do it here.
				
				// Wait, ServerClient constructor starts a thread.
				// Let's give it a temp ID or 0, and let it update itself?
				// Actually, we can move the ID generation inside ServerClient logic?
				// Let's keep it simple: We accept, we instantiate ServerClient, we add to list. 
				// BUT we don't know the NAME yet.
				
				// REVISED: We can't create ServerClient fully until we know the name.
				// We need a helper method to handle handshake.
				new Thread(new ClientHandshake(clientSocket)).start();
				
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// Helper class for Initial Handshake
	class ClientHandshake implements Runnable {
		Socket socket;
		public ClientHandshake(Socket s) { this.socket = s; }
		public void run() {
			try {
				// We need raw streams here just for handshake
				java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
				java.io.PrintWriter out = new java.io.PrintWriter(socket.getOutputStream(), true);
				
				// Wait for /c/Name/e/
				String line = in.readLine();
				if(line != null && line.startsWith("/c/")) {
					String name = line.split("/c/|/e/")[1];
					int id = UniqueIdentifier.getIdentifier();
					
					System.out.println(name + " (" + id + ") connected from " + socket.getInetAddress());
					
					ServerClient client = new ServerClient(name, socket, id, in, out); // Takes over the socket
					// Note: ServerClient constructor creates NEW streams on same socket. This is fine.
					
					synchronized(allClients) {
						allClients.add(client);
					}
					// Join Global
					rooms.get("Global").addClient(client);
					broadcastToGlobal(name + " joined.");
					
					// Send Welcome: /c/ID/e/
					client.send("/c/" + id + "/e/");
					sendStatus();
					
				} else {
					socket.close();
				}
			} catch(Exception e) { e.printStackTrace(); }
		}
	}
	
	public static void process(int id, String message) {
		// Handle standard messages from initialized clients
		ServerClient c = getClient(id);
		if(c == null) return;
		
		if(message.startsWith("/m/")) {
			// /m/Room/Content/e/
			String[] parts = message.split("/", 5);
			if(parts.length >= 4) {
				String room = parts[2];
				String content = parts[3];
				// Fix: Re-wrap in protocol for proper Client parsing
				String packet = "/m/" + room + "/" + content + "/e/";
				sendToRoom(room, packet);
				// Log sanitized
				System.out.println("Message in Room: " + room);
			}
		} else if(message.startsWith("/d/")) {
			disconnect(id, true);
		} else if(message.startsWith("/r/create/")) {
			// /r/create/Name/Pass...
			String[] parts = message.split("/");
			String rName = parts[3];
			String rPass = parts[4];
			if(!rooms.containsKey(rName)) {
				rooms.put(rName, new Room(rName, rPass, id, c.name));
				// Auto join
				rooms.get(rName).addClient(c);
				c.send("/r/joined/" + rName + "/e/");
				rooms.get(rName).broadcast("Server: Room created by " + c.name + ".");
				System.out.println("Room Created: " + rName);
			}
		} else if(message.startsWith("/r/join/")) {
			// /r/join/Name/Pass...
			String[] parts = message.split("/");
			String rName = parts[3];
			String rPass = parts[4];
			if(rooms.containsKey(rName)) {
				Room r = rooms.get(rName);
				if(r.checkPassword(rPass) || r.isInvited(c.getID())) {
					r.removeInvite(c.getID());
					r.addClient(c);
					r.broadcast("Server: " + c.name + " joined.");
					c.send("/r/joined/" + rName + "/e/");
				} else {
					c.send("/r/join_failed/Wrong Password/e/");
				}
			} else {
				c.send("/r/join_failed/Room does not exist/e/");
			}
		} else if(message.startsWith("/r/leave/")) {
			// /r/leave/RoomName/ID/e/
			String rName = message.split("/")[3];
			if(rooms.containsKey(rName)) {
				Room r = rooms.get(rName);
				r.removeClient(c);
				r.broadcast("Server: " + c.name + " left.");
				if(r.isEmpty() && !rName.equals("Global")) {
					rooms.remove(rName);
					System.out.println("Room removed: " + rName);
				}
			}
		} else if(message.startsWith("/r/req-list/")) {
			StringBuilder sb = new StringBuilder("/r/list/");
			for(String r : rooms.keySet()) {
				if(r.equals("Global")) continue; // Filter Global
				Room room = rooms.get(r);
				sb.append(room.getName()).append(":")
				  .append(room.getAdminName()).append(":")
				  .append(room.hasPassword() ? "1" : "0").append(":")
				  .append(room.getClients().size()).append(",");
			}
			c.send(sb.toString() + "/e/");
		} else if(message.startsWith("/r/req-members/")) {
			// .. logic ..
			String rName = message.split("/")[3];
			if(rooms.containsKey(rName)) {
				Room r = rooms.get(rName);
				StringBuilder sb = new StringBuilder("/r/members/" + rName + "/");
				for(ServerClient sc : r.getClients()) {
					sb.append(sc.name).append("(").append(sc.ID).append(")").append(",");
				}
				c.send(sb.toString() + "/e/");
			}
		} else if(message.startsWith("/r/invite/")) {
			// /r/invite/TargetID/RoomName/e/
			String[] parts = message.split("/");
			int targetID = Integer.parseInt(parts[3]);
			String roomName = parts[4];
			if(rooms.containsKey(roomName)) {
				rooms.get(roomName).addInvite(targetID);
			}
			ServerClient target = getClient(targetID);
			if(target != null) {
				target.send("/r/invitation/" + roomName + "/e/");
			}
		}
	}
	
	public static ServerClient getClient(int id) {
		for(ServerClient c : allClients) {
			if(c.getID() == id) return c;
		}
		return null;
	}
	
	public static void disconnect(int id, boolean status) {
		ServerClient c = getClient(id);
		if(c == null) return;
		
		allClients.remove(c);
		// Remove from rooms
		List<String> emptyRooms = new ArrayList<>();
		for(Room r : rooms.values()) {
			r.removeClient(c);
			if(r.isEmpty() && !r.getName().equals("Global")) {
				emptyRooms.add(r.getName());
			}
		}
		for(String rName : emptyRooms) rooms.remove(rName);
		c.close();
		if(status) {
			String msg = c.name + " has left.";
			System.out.println(msg);
			broadcastToGlobal(msg);
			sendStatus();
		}
	}
	
	public static void sendStatus() {
		if(allClients.size() <= 0) return;
		StringBuilder userList = new StringBuilder("/u/");
		for(ServerClient c : allClients) {
			userList.append(c.name).append("(").append(c.ID).append(")/u/");
		}
		userList.append("e/");
		for(ServerClient c : allClients) {
			c.send(userList.toString());
		}
	}
	
	private static void broadcastToGlobal(String message) {
		if(rooms.containsKey("Global")) {
			// Using standard message format: /m/Global/Server: Msg/e/
			// Or just raw text depending on client expectation?
			// Client expects /m/Global/Content/e/
			sendToRoom("Global", "/m/Global/Server: " + message + "/e/");
		}
	}
	
	private static void sendToRoom(String roomName, String message) {
		if(rooms.containsKey(roomName)) {
			Room r = rooms.get(roomName);
			for(ServerClient c : r.getClients()) {
				c.send(message);
			}
		}
	}
	
	private void sendToAll(String message) {
		for(ServerClient c : allClients) {
			c.send(message);
		}
	}

	private void printHelp() {
		System.out.println("Available commands: /quit");
	}
	
	private void quit() {
		running = false;
		try { serverSocket.close(); } catch(Exception e) {}
	}
}
