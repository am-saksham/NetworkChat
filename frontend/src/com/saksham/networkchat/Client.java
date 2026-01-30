package com.saksham.networkchat;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

import java.awt.GridBagLayout;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import java.awt.GridBagConstraints;
import javax.swing.JButton;
import java.awt.Insets;
import javax.swing.JTextField;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Client {

	private static final long serialVersionUID = 1L;
	private String name, address;
	private int port;
	private DatagramSocket socket;
	private InetAddress ip;
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
			socket = new DatagramSocket();
			socket.setSoTimeout(2000);
			ip = InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		} catch (SocketException e) {
			e.printStackTrace();
			return false;
		}
		return true;

	}

	public String receive() {
		byte[] data = new byte[1024];
		DatagramPacket packet = new DatagramPacket(data, data.length);
		try {
			socket.receive(packet);
			return new String(packet.getData(), 0, packet.getLength());
		} catch (IOException e) {
			return null;
		}
	}

	public void send(final byte[] data) {
		send = new Thread("Send") {
			public void run() {
				DatagramPacket packet = new DatagramPacket(data, data.length, ip, port);
				try {
					socket.send(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		send.start();
	}
	
	public void close() {
		new Thread() {
			public void run() {
				synchronized(socket) {
					socket.close();
				}
			}
		}.start();
	}

	public void setID(int ID) {
		this.ID = ID;
	}
	
	public int getID() {
		return ID;
	}

}
