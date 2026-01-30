package com.saksham.networkchat.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import java.util.HashSet;
import java.util.Set;

public class Server implements Runnable {
	
	// Master list of all connected clients for heartbeats/connection management
	private List<ServerClient> allClients = new ArrayList<ServerClient>();
	// Map of Room Name -> Room Object
	private Map<String, Room> rooms = new HashMap<String, Room>();
	// Set of pending invites: "RoomName:UserID"
	private Set<String> pendingInvites = new HashSet<String>();
	
	private List<Integer> clientResponse = new ArrayList<Integer>();
	
	private DatagramSocket socket;
	private int port;
	private boolean running = false;
	private Thread run, manage, send, receive;
	
	private final int MAX_ATTEMPTS = 5;
	private boolean raw = false;
	
	public Server(int port) {
		this.port = port;
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
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
		System.out.println("Server started on port " + port);
		manageClients();
		receive();
		
		// Console input handler
		if (System.console() != null) {
		    new Thread(() -> {
		        Scanner sc = new Scanner(System.in);
		        while(running) {
		            if (sc.hasNextLine()) {
		                String text = sc.nextLine();
		                if(!text.startsWith("/")) {
		                    broadcastToGlobal("Server: " + text);
		                    continue;
		                }
		                text = text.substring(1);
		                processCommand(text);
		            }
		        }
		        sc.close();
		    }).start();
		}
	}
	
	private void processCommand(String text) {
	    if (text.equals("raw")) {
	        raw = !raw;
	        System.out.println("Raw mode " + (raw ? "on" : "off"));
	    } else if (text.equals("clients")) {
	        System.out.println("Clients:");
	        System.out.println("===================================================");
	        for(ServerClient c : allClients) {
	            System.out.println(c.name + "(" + c.getID() + ")");
	        }
	        System.out.println("===================================================");
	    } else if (text.equals("rooms")) {
            System.out.println("Rooms:");
            System.out.println("===================================================");
            for(Room r : rooms.values()) {
                System.out.println(r.getName() + " - Users: " + r.getClients().size());
            }
            System.out.println("===================================================");
	    } else if (text.startsWith("kick")) {
	        kickClient(text);
	    } else if (text.equals("quit")) {
	        quit();
	    } else if (text.equals("help")) {
	        printHelp();
	    } else {
	        System.out.println("Unknown Command");
	    }
	}
	
	private void kickClient(String text) {
	    String nameOrId = text.split(" ")[1];
	    int id = -1;
	    try {
	        id = Integer.parseInt(nameOrId);
	    } catch(NumberFormatException e) {
	        for(ServerClient c : allClients) {
	            if(c.name.equals(nameOrId)) {
	                id = c.getID();
	                break;
	            }
	        }
	    }
	    if(id != -1) disconnect(id, true);
	    else System.out.println("Client not found.");
	}
	
	private void printHelp() {
		System.out.println("Available commands:");
		System.out.println("/raw - Toggle raw mode");
		System.out.println("/clients - List connected clients");
		System.out.println("/rooms - List active rooms");
		System.out.println("/kick [ID/Name] - Kick a user");
		System.out.println("/quit - Stop server");
	}
	
	private void manageClients() {
		manage = new Thread("Manage") {
			public void run() {
				while(running) {
					sendToAll("/i/server");
					sendStatus();
					sendToAll("/i/server");
					sendStatus();
					// sendRoomList(); // Don't auto-send, only on request to avoid client popup spam
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					// Timeout logic
					for(int i = 0; i < allClients.size(); i++) {
						ServerClient c = allClients.get(i);
						if(!clientResponse.contains(c.getID())) {
							if(c.attempt >= MAX_ATTEMPTS) {
								disconnect(c.getID(), false);
							} else {
								c.attempt++;
							}
						} else {
							clientResponse.remove(Integer.valueOf(c.getID()));
							c.attempt = 0;
						}
					}
				}
			}
		};
		manage.start();
	}
	
	// Sends Global User List (for Invites/Online status)
	private void sendStatus() {
		if(allClients.size() <= 0) return;
		String users = "/u/";
		for(int i = 0; i < allClients.size()-1; i++) {
			users += allClients.get(i).name + "(" + allClients.get(i).getID() + ")/n/";
		}
		users += allClients.get(allClients.size() - 1).name + "(" + allClients.get(allClients.size() - 1).getID() + ")/e/";
		sendToAll(users);
	}
	
	private void sendRoomList() {
	    if (rooms.isEmpty()) return;
	    // Format: /r/list/Room1:Creator,Room2:Creator/e/
	    StringBuilder sb = new StringBuilder("/r/list/");
	    for (Room r : rooms.values()) {
	        sb.append(r.getName()).append(":").append(r.getAdminName()).append(",");
	    }
	    // Remove last comma if exists
	    if (sb.length() > 8) sb.setLength(sb.length() - 1);
	    sb.append("/e/");
	    sendToAll(sb.toString());
	}
	
	private void sendRoomListTo(ServerClient c) {
	    StringBuilder sb = new StringBuilder("/r/list/");
	    for (Room r : rooms.values()) {
	    	if(r.getName().equals("Global")) continue; 
	        // Format: Name:Creator:HasPass(0/1):Count
	    	int hasPass = (r.getPassword() == null || r.getPassword().isEmpty()) ? 0 : 1;
	        sb.append(r.getName()).append(":")
	          .append(r.getAdminName()).append(":")
	          .append(hasPass).append(":")
	          .append(r.getClients().size()).append(",");
	    }
	    // Remove trailing comma if we added any rooms
	    if (sb.toString().endsWith(",")) {
	    	sb.setLength(sb.length() - 1);
	    }
	    // send() appends /e/ automatically
	    send(sb.toString(), c.address, c.port);
	}
	
	private void receive() {
		receive = new Thread("Receive") {
			public void run() {
				while(running) {
					byte[] data = new byte[1024];
					DatagramPacket packet = new DatagramPacket(data, data.length);
					try {
						socket.receive(packet);
					} catch (SocketException e) {
					} catch (IOException e) {
						e.printStackTrace();
					}
					process(packet);
				}
			}
		};
		receive.start();
	}
	
	// Broadcast to ALL connected clients (System messages)
	private void sendToAll(String message) {
		if(message.startsWith("/m/")) {
			System.out.println(message);
		}
		for(int i = 0; i < allClients.size(); i++) {
			ServerClient client = allClients.get(i);
			send(message.getBytes(), client.address, client.port);
		}
	}
	
	// Broadcast to a specific room
	private void sendToRoom(String roomName, String message) {
	    Room room = rooms.get(roomName);
	    if(room == null) return;
	    
	    if(message.startsWith("/m/")) {
            System.out.println("[" + roomName + "] " + message);
        }
	    
	    for(ServerClient client : room.getClients()) {
	        send(message.getBytes(), client.address, client.port);
	    }
	}
	
	private void broadcastToGlobal(String text) {
	    sendToRoom("Global", "/m/Global/Server: " + text + "/e/");
	}
	
	private void send(final byte[] data, final InetAddress address, final int port) {
		send = new Thread("Send") {
			public void run() {
				DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
				try {
					socket.send(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		send.start();
	}
	
	private void send(String message, InetAddress address, int port) {
		message += "/e/";
		send(message.getBytes(), address, port);
	}
	
	// --- PACKET PROCESSING ---
	
	private void process(DatagramPacket packet) {
		String string = new String(packet.getData());
		string = string.trim();
		if(raw) System.out.println(string);
		System.out.println("DEBUG PACKET: " + string);
		
		if(string.startsWith("/c/")) {
		    // CONNECT
			int id = UniqueIdentifier.getIdentifier();
			String name = string.split("/c/|/e/")[1];
			System.out.println(name + "(" + id + ") connected!");
			ServerClient newClient = new ServerClient(name, packet.getAddress(), packet.getPort(), id);
			allClients.add(newClient);
			// Auto-join Global
			rooms.get("Global").addClient(newClient);
			broadcastToGlobal(name + " joined the chat!");
			
			String ID = "/c/" + id;
			send(ID, packet.getAddress(), packet.getPort());
			
		} else if(string.startsWith("/m/")) {
		    // MESSAGE: /m/RoomName/Message...
		    String[] parts = string.split("/", 5); // /m/RoomName/Message/e/
		    if (parts.length >= 4) {
		        String roomName = parts[2];
		        String msgContent = string; // Forward the whole packet to keep format
		        sendToRoom(roomName, msgContent);
		    }
			
		} else if(string.startsWith("/d/")){
		    // DISCONNECT
			String id = string.split("/d/|/e/")[1];
			disconnect(Integer.parseInt(id), true);
			
		} else if (string.startsWith("/i/")) {
		    // PING
			clientResponse.add(Integer.parseInt(string.split("/i/|/e/")[1]));
			
		} else if (string.startsWith("/r/create/")) {
		    // CREATE ROOM: /r/create/Name/Pass/ID/e/
		    String[] parts = string.split("/");
		    String name = parts[3];
		    String pass = parts[4];
		    if (pass.equals("null")) pass = ""; // simplicity
		    int creatorID = Integer.parseInt(parts[5]);
		    
		    if (!rooms.containsKey(name)) {
		    	System.out.println("Attempting to create room: " + name);
		    	// Fetch creator name
		    	String creatorName = "Unknown";
		    	ServerClient creator = getClient(creatorID);
		    	if(creator != null) creatorName = creator.name;
		    	
		        Room newRoom = new Room(name, pass, creatorID, creatorName);
		        rooms.put(name, newRoom);
		        System.out.println("Room created: " + name);
		        
		        if(creator != null) {
		        	System.out.println("Adding creator " + creator.name + " to room " + name);
		            newRoom.addClient(creator);
		            send("/r/joined/" + name, creator.address, creator.port);
		            sendToRoom(name, "/m/" + name + "/Server: " + creator.name + " created the room./e/");
		        } else {
		        	System.out.println("Creator client instance not found for ID: " + creatorID);
		        }
		    } else {
		    	// Room exists
		    	System.out.println("Creation failed: Room " + name + " already exists.");
		    	ServerClient creator = getClient(creatorID);
		    	if(creator != null) {
		    		send("/r/join_failed/Room name '" + name + "' is already taken.", creator.address, creator.port);
		    	}
		    }
		    
		} else if (string.startsWith("/r/join/")) {
		    // JOIN ROOM: /r/join/Name/Pass/ID/e/
		    String[] parts = string.split("/");
		    String roomName = parts[3];
		    String pass = parts[4];
		    if (pass.equals("null")) pass = "";
		    int userID = Integer.parseInt(parts[5]);
		    
		    Room r = rooms.get(roomName);
		    ServerClient c = getClient(userID);
		    
		    if (r != null && c != null) {
		    	System.out.println("Processing join for room: " + roomName + " with pass: " + pass);
		    	// Check logic
		    	boolean isInvited = pendingInvites.contains(roomName + ":" + userID);
		    	if(isInvited) {
		    		System.out.println("User " + userID + " has a pending invite for " + roomName + ". Bypassing password.");
		    		pendingInvites.remove(roomName + ":" + userID);
		    	}
		    	
		        if (isInvited || r.checkPassword(pass)) {
		            if (!r.hasClient(c)) {
		            	r.addClient(c);
		            	send("/r/joined/" + roomName, c.address, c.port);
		            	sendToRoom(roomName, "/m/" + roomName + "/Server: " + c.name + " joined the room./e/");
		            } else {
		            	// Already in room, resend joined confirmation
		            	send("/r/joined/" + roomName, c.address, c.port);
		            }
		        } else {
		            // Send error
		        	System.out.println("Join failed: Wrong password. Expected: " + r.getPassword() + ", Got: " + pass);
		        	send("/r/join_failed/Wrong Password", c.address, c.port);
		        }
		    }
		    
		} else if (string.startsWith("/r/leave/")) {
		    // LEAVE ROOM: /r/leave/Name/ID/e/
	        String[] parts = string.split("/");
            String roomName = parts[3];
            int userID = Integer.parseInt(parts[4]);
            
            Room r = rooms.get(roomName);
            ServerClient c = getClient(userID);
            if(r != null && c != null) {
                r.removeClient(c);
        	        if(r.isEmpty() && !r.getName().equals("Global")) {
                    rooms.remove(r.getName());
                    System.out.println("Room removed: " + roomName);
                } else {
                	sendToRoom(roomName, "/m/" + roomName + "/Server: " + c.name + " left./e/");
                }
            }
		} else if (string.startsWith("/r/req-list/")) {
			// Request Room List: /r/req-list/ID/e/
			int id = Integer.parseInt(string.split("/r/req-list/|/e/")[1]);
			ServerClient c = getClient(id);
			if(c != null) sendRoomListTo(c);
			
		} else if (string.startsWith("/r/req-members/")) {
			// Request Members: /r/req-members/RoomName/ID/e/
			String[] parts = string.split("/");
			String roomName = parts[3];
			int requesterID = Integer.parseInt(parts[4]);
			ServerClient requester = getClient(requesterID);
			
			if(requester != null && rooms.containsKey(roomName)) {
				Room r = rooms.get(roomName);
				StringBuilder sb = new StringBuilder("/r/members/" + roomName + "/");
				for(ServerClient client : r.getClients()) {
					sb.append(client.name).append("(").append(client.getID()).append(")").append(",");
				}
				if (sb.toString().endsWith(",")) sb.setLength(sb.length() - 1);
				send(sb.toString(), requester.address, requester.port);
			}
			
		} else if (string.startsWith("/r/invite/")) {
            // INVITE: /r/invite/TargetID/RoomName/e/
		    // We forward this to the specific target user
            String[] parts = string.split("/");
            int targetID = Integer.parseInt(parts[3]);
            String roomName = parts[4];
            
            // Add to authorized list
            pendingInvites.add(roomName + ":" + targetID);
            System.out.println("Authorized invite for User " + targetID + " to Room " + roomName);
            
            ServerClient target = getClient(targetID);
            if (target != null) {
                // Packet: /r/invitation/RoomName/e/
                send("/r/invitation/" + roomName, target.address, target.port);
            }
        } else if (string.startsWith("/d/")) {
        	// DISCONNECT: /d/ID/e/
        	String idStr = string.split("/d/|/e/")[1];
        	System.out.println("Processing explicit disconnect for ID: " + idStr);
        	disconnect(Integer.parseInt(idStr), true);
        }
	}
	
	private ServerClient getClient(int id) {
	    for(ServerClient c : allClients) {
	        if(c.getID() == id) return c;
	    }
	    return null;
	}
	
	private void quit() {
		for(int i = 0; i < allClients.size(); i++) {
			disconnect(allClients.get(i).getID(), true);
		}
		running = false;
		socket.close();
	}
	
	private void disconnect(int id, boolean status) {
		ServerClient c = null;
		for(int i = 0; i < allClients.size(); i++) {
			if(allClients.get(i).getID() == id) {
				c = allClients.get(i);
				allClients.remove(i);
				break;
			}
		}
		if(c == null) return;
		
		// Remove from ALL rooms
		for(Room r : rooms.values()) {
		    if(r.hasClient(c)) {
		    	r.removeClient(c);
		    	sendToRoom(r.getName(), "/m/" + r.getName() + "/Server: " + c.name + " left./e/");
		    }
		}
		
		// Clean empty rooms
		// avoid iterator modification exception
		List<String> toRemove = new ArrayList<>();
		for(Room r : rooms.values()) {
			if(r.isEmpty() && !r.getName().equals("Global")) {
				toRemove.add(r.getName());
			}
		}
		for(String s : toRemove) rooms.remove(s);
		
		String message = "";
		if(status) {
			message = "Client " + c.name + " (" + c.getID() + ") @ " + c.address.toString() + ":" + c.port + " disconnected.";
		} else {
			message = "Client " + c.name + " (" + c.getID() + ") @ " + c.address.toString() + ":" + c.port + " timed out.";
		}
		System.out.println(message);
		
		// IMPORTANT: Broadcast updated user list to everyone!
		sendStatus(); 
	}
}
