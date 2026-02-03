package com.saksham.networkchat;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class Login extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField txtName;
	private JTextField txtAddress;
	private JTextField txtPort;

	public Login() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		setResizable(false); 
		setTitle("Login");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(300, 380);
		setLocationRelativeTo(null);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		txtName = new JTextField();
		txtName.setBounds(67, 50, 165, 28);
		contentPane.add(txtName);
		txtName.setColumns(10);

		JLabel lblNewLabel = new JLabel("Name:");
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel.setBounds(119, 33, 61, 16);
		contentPane.add(lblNewLabel);

		txtAddress = new JTextField("52.66.246.194");
		txtAddress.setEditable(false);
		txtAddress.setBounds(67, 110, 165, 28);
		contentPane.add(txtAddress);
		txtAddress.setColumns(10);

		JLabel lblIpAddress = new JLabel("IP Address:");
		lblIpAddress.setHorizontalAlignment(SwingConstants.CENTER);
		lblIpAddress.setBounds(104, 91, 91, 16);
		contentPane.add(lblIpAddress);

		JLabel lblPort = new JLabel("Port:");
		lblPort.setHorizontalAlignment(SwingConstants.CENTER);
		lblPort.setBounds(104, 175, 91, 16);
		contentPane.add(lblPort);

		txtPort = new JTextField("443");
		txtPort.setEditable(false);
		txtPort.setColumns(10);
		txtPort.setBounds(67, 194, 165, 28);
		contentPane.add(txtPort);

		JLabel lblAddressDesc = new JLabel("(e.g. 192.128.0.1)");
		lblAddressDesc.setHorizontalAlignment(SwingConstants.CENTER);
		lblAddressDesc.setBounds(86, 137, 128, 16);
		contentPane.add(lblAddressDesc);

		JLabel lblPortDesc = new JLabel("(e.g. 443)");
		lblPortDesc.setHorizontalAlignment(SwingConstants.CENTER);
		lblPortDesc.setBounds(86, 221, 128, 16);
		contentPane.add(lblPortDesc);

		JButton btnLogin = new JButton("Login");
		btnLogin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String name = txtName.getText();
				String address = txtAddress.getText();
				int port = Integer.parseInt(txtPort.getText());
				login(name, address, port);
			}
		});
		btnLogin.setBounds(91, 286, 117, 29);
		contentPane.add(btnLogin);
	}

	private void login(String name, String address, int port) {
		dispose();
		new ClientWindow(name, address, port);
	}

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Login frame = new Login();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
