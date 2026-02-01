package com.saksham.networkchat.server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Room {
	
	private String name;
	private String password;
	private int adminID;
	private String adminName; // New field
	private List<ServerClient> clients;
	private java.util.Set<Integer> invitedIDs = new java.util.HashSet<>();
	
	public Room(String name, String password, int adminID, String adminName) {
		this.name = name;
		this.password = hash(password); // Hash password
		this.adminID = adminID;
		this.adminName = adminName;
		this.clients = new ArrayList<ServerClient>();
	}
	
	public void addInvite(int id) {
		invitedIDs.add(id);
	}
	
	public boolean isInvited(int id) {
		return invitedIDs.contains(id);
	}
	
	public void removeInvite(int id) {
		invitedIDs.remove(id);
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
		if(this.password == null || this.password.isEmpty() || this.password.equals("null")) return true;
		return this.password.equals(hash(pass));
	}
	
	private String hash(String raw) {
		if(raw == null || raw.equals("null") || raw.isEmpty()) return "null";
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(raw.getBytes());
			StringBuilder hexString = new StringBuilder();
		    for (byte b : hash) {
		        String hex = Integer.toHexString(0xff & b);
		        if(hex.length() == 1) hexString.append('0');
		        hexString.append(hex);
		    }
		    return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return raw;
		}
	}
	
	public boolean hasPassword() {
		return !(this.password == null || this.password.isEmpty() || this.password.equals("null"));
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
	
	public void broadcast(String message) {
		String packet = "/m/" + name + "/" + message + "/e/";
		for(ServerClient c : clients) {
			c.send(packet);
		}
	}
	
	public int getAdminID() {
		return adminID;
	}
	
	public boolean isEmpty() {
		return clients.isEmpty();
	}
}
