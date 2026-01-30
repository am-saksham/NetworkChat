package com.saksham.networkchat.server;

import java.util.ArrayList;
import java.util.List;

public class Room {
	
	private String name;
	private String password;
	private int adminID;
	private String adminName; // New field
	private List<ServerClient> clients;
	
	public Room(String name, String password, int adminID, String adminName) {
		this.name = name;
		this.password = password;
		this.adminID = adminID;
		this.adminName = adminName;
		this.clients = new ArrayList<ServerClient>();
	}
	
	public String getName() {
		return name;
	}
	
	public String getAdminName() {
		return adminName;
	}

	public String getPassword() {
		return password;
	}

	// Returns true if password matches or if room has no password
	public boolean checkPassword(String pass) {
		if(this.password == null || this.password.isEmpty()) return true;
		return this.password.equals(pass);
	}
	
	public void addClient(ServerClient client) {
		clients.add(client);
	}
	
	public void removeClient(ServerClient client) {
		clients.remove(client);
	}
	
	public boolean hasClient(ServerClient client) {
		return clients.contains(client);
	}
	
	public List<ServerClient> getClients() {
		return clients;
	}
	
	public int getAdminID() {
		return adminID;
	}
	
	public boolean isEmpty() {
		return clients.isEmpty();
	}
}
