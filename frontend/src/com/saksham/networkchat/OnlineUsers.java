package com.saksham.networkchat;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class OnlineUsers extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTable table;
	private DefaultTableModel model;
	private ClientWindow clientWindow;

	public OnlineUsers(ClientWindow window) {
		this.clientWindow = window;
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setSize(300, 400);
		setLocationRelativeTo(null);
		setTitle("Online Users");
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		String[] columnNames = {"Name", "ID"};
		model = new DefaultTableModel(columnNames, 0) {
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		table = new JTable(model);
		table.setRowHeight(25);
		contentPane.add(new JScrollPane(table), BorderLayout.CENTER);
		
		// Context Menu
		JPopupMenu menu = new JPopupMenu();
		JMenuItem inviteItem = new JMenuItem("Invite to Current Room");
		inviteItem.addActionListener(e -> {
			int row = table.getSelectedRow();
			if(row != -1) {
				String idStr = (String) table.getValueAt(row, 1);
				int id = Integer.parseInt(idStr);
				clientWindow.handleInviteAction(id);
			}
		});
		menu.add(inviteItem);
		
		table.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					int row = table.rowAtPoint(e.getPoint());
					table.setRowSelectionInterval(row, row);
					menu.show(table, e.getX(), e.getY());
				}
			}
		});
	}
	
	public void update(String[] users) {
		model.setRowCount(0);
		for(String u : users) {
			// Format "Name(ID)"
			String name = u.substring(0, u.indexOf("("));
			String id = u.substring(u.indexOf("(") + 1, u.indexOf(")"));
			model.addRow(new Object[]{name, id});
		}
	}

}
