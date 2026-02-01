package com.saksham.networkchat;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;

public class ClientWindow extends JFrame implements Runnable {
	
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTabbedPane tabbedPane;
	private Thread run, listen;
	private Client client;
	
	private boolean running = false;
	private JMenuBar menuBar;
	private JMenu mnFile, mnRooms;
	private JMenuItem mntmOnlineUsers, mntmExit;
	private JMenuItem mntmCreateRoom, mntmJoinRoom;
	
	private OnlineUsers users;
	private Map<String, ChatPanel> openRooms = new HashMap<>(); // Track open tabs
	private String[] currentOnlineUsers = new String[0];
	
	private RoomListDialog roomListDialog;
	
	public ClientWindow(String name, String address, int port) {
		setTitle("Network Chat Client");
		client = new Client(name, address, port);
		
		createWindow();
		
		// Connection Thread
		new Thread(() -> {
			boolean connect = client.openConnection(address);
			if (!connect) {
				System.err.println("Connection Failed!");
				return;
			}
			// TCP: Connection is live instantly
			String connection = "/c/" + name + "/e/";
			client.send(connection);
			
			users = new OnlineUsers(this);
			running = true;
			run = new Thread(this, "Running");
			run.start();
		}).start();
	}
	
	private void createWindow() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800, 600);
		setLocationRelativeTo(null);
		
		
		// Shutdown Hook to handle Cmd+Q and other exit signals
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Shutdown hook running...");
	        String disconnect = "/d/" + client.getID() + "/e/";
	        client.send(disconnect);
	        try { Thread.sleep(200); } catch (Exception e) {}
	        client.close();
		}));
		
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		// FILE MENU
		mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		mntmOnlineUsers = new JMenuItem("Online Users");
		mntmOnlineUsers.addActionListener(e -> users.setVisible(true));
		mnFile.add(mntmOnlineUsers);
		
		mntmExit = new JMenuItem("Exit");
		mntmExit.addActionListener(e -> closeAndExit());
		mnFile.add(mntmExit);
		
		// ROOMS MENU
		mnRooms = new JMenu("Rooms");
		menuBar.add(mnRooms);
		
		mntmCreateRoom = new JMenuItem("Create Room");
		mntmCreateRoom.addActionListener(e -> showCreateRoomDialog());
		mnRooms.add(mntmCreateRoom);
		
		mntmJoinRoom = new JMenuItem("Join Room");
		mntmJoinRoom.addActionListener(e -> requestRoomList());
		mnRooms.add(mntmJoinRoom);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		// TABBED PANE
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		contentPane.add(tabbedPane, BorderLayout.CENTER);
		
		// Create Default Global Room
		addRoomTab("Global", false);
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				closeAndExit();
			}
		});

		setVisible(true);
	}
	
	private void addRoomTab(String roomName, boolean closable) {
		if (openRooms.containsKey(roomName)) {
		    tabbedPane.setSelectedIndex(tabbedPane.indexOfTab(roomName));
		    return;
		}
		ChatPanel panel = new ChatPanel(roomName);
		openRooms.put(roomName, panel);
		tabbedPane.addTab(roomName, panel);
		
		if(closable) {
			int index = tabbedPane.indexOfComponent(panel);
			tabbedPane.setTabComponentAt(index, new ButtonTabComponent(tabbedPane));
		}
		
		tabbedPane.setSelectedComponent(panel);
	}
	
	private void closeAndExit() {
		String disconnect = "/d/" + client.getID() + "/e/";
		client.send(disconnect); 
		running = false;
		client.close();
		System.exit(0);
	}
	
	public void handleInviteAction(int targetID) {
	    int index = tabbedPane.getSelectedIndex();
	    String roomName = tabbedPane.getTitleAt(index);
	    
	    if (roomName.equals("Global")) {
	        JOptionPane.showMessageDialog(this, "You cannot invite users to Global chat.");
	        return;
	    }
	    
	    // /r/invite/TargetID/RoomName/e/
	    client.send("/r/invite/" + targetID + "/" + roomName + "/e/");
	    JOptionPane.showMessageDialog(this, "Invitation sent!");
	}
	
	private void requestRoomList() {
		client.send("/r/req-list/" + client.getID() + "/e/");
		// Maybe show a "Loading..." dialog if we want to be fancy
	}
	
	public void joinRoom(String roomName, String password) {
		if(roomListDialog != null) roomListDialog.dispose();
		if(openRooms.containsKey(roomName)) {
			tabbedPane.setSelectedIndex(tabbedPane.indexOfTab(roomName));
			return;
		}
		if(password == null || password.isEmpty()) password = "null";
		
		// Send join request
        client.send("/r/join/" + roomName + "/" + password + "/" + client.getID() + "/e/");
        // Do NOT open tab yet. Wait for /r/joined/ confirmation.
	}
	
	// --- DIALOGS ---
	
	private void showCreateRoomDialog() {
	    JTextField nameField = new JTextField();
	    JTextField passField = new JTextField();
	    Object[] message = {
	        "Room Name:", nameField,
	        "Password (Optional):", passField
	    };

	    int option = JOptionPane.showConfirmDialog(null, message, "Create Room", JOptionPane.OK_CANCEL_OPTION);
	    if (option == JOptionPane.OK_OPTION) {
	        String name = nameField.getText().trim();
	        String pass = passField.getText().trim();
	        if(pass.isEmpty()) pass = "null";
	        if (!name.isEmpty()) {
	            client.send("/r/create/" + name + "/" + pass + "/" + client.getID() + "/e/");
	            // Auto join happens on server side which sends confirmation
	        }
	    }
	}
	
	public void run() {
		listen();
	}
	
	private void listen() {
	    listen = new Thread(() -> {
	        while(running) {
	            String message = client.receive();
	            if(message != null) {
	                final String msg = message;
	                SwingUtilities.invokeLater(() -> processMessage(msg));
	            }
	        }
	    });
	    listen.start();
	}

	private void processMessage(String message) {
	    if(message.startsWith("/c/")) {
	        client.setID(Integer.parseInt(message.split("/c/|/e/")[1]));
	        openRooms.get("Global").console("Connected! ID: " + client.getID(), Color.GRAY, false);
	    
	    } else if(message.startsWith("/i/")) {
	    	client.send("/i/" + client.getID() + "/e/");
	    	
	    } else if(message.startsWith("/m/")) {
            // /m/RoomName/Message/e/
	        String[] parts = message.split("/", 5); // Empty, m, RoomName, Content...
	        if(parts.length >= 4) {
	            String room = parts[2];
	            String content = parts[3]; 
	            
	            if (content.endsWith("/e/")) {
	                content = content.substring(0, content.length() - 3);
	            }
	            
	            if(openRooms.containsKey(room)) {
	                Color c = Color.BLACK;
	                boolean isMe = false;
	                if(content.startsWith("Server:")) {
	                	c = Color.RED;
	                } else {
	                	String name = content.split(":")[0];
	                	if(name.contains("(" + client.getID() + ")")) {
	                		isMe = true;
	                		c = Color.BLUE; 
	                	} else {
	                		c = getUserColor(name);
	                	}
	                }
	                openRooms.get(room).console(content, c, isMe);
	            }
	        }
	        
	    } else if(message.startsWith("/i/")) {
	        client.send("/i/" + client.getID() + "/e/");
	        
	    } else if(message.startsWith("/u/")) {
	    	String raw = message.substring(3);
	    	if(raw.endsWith("e/")) raw = raw.substring(0, raw.length() - 2);
	    	String[] u = raw.split("/u/");
	    	currentOnlineUsers = u; // Fix: Update the list variable!
	        users.update(u);
	        
	    } else if (message.startsWith("/r/invitation/")) {
	        // /r/invitation/RoomName/e/
	        String roomName = message.split("/")[3];
	        int choice = JOptionPane.showConfirmDialog(this, 
	                "You have been invited to join room: " + roomName, 
	                "Room Invite", JOptionPane.YES_NO_OPTION);
	        
	        if(choice == JOptionPane.YES_OPTION) {
	             client.send("/r/join/" + roomName + "/null/" + client.getID() + "/e/");
	        }
	        
	    } else if (message.startsWith("/r/members/")) {
	        // /r/members/RoomName/User1,User2.../e/
	    	String[] parts = message.split("/r/members/|/e/");
	    	if(parts.length > 1) {
	    		String content = parts[1];
	    		// content is RoomName/User1,User2...
	    		int slashIdx = content.indexOf('/');
	    		String roomName = content.substring(0, slashIdx);
	    		String usersStr = content.substring(slashIdx + 1);
	    		String[] members = usersStr.split(",");
	    		
	    		JOptionPane.showMessageDialog(this, 
	    				String.join("\n", members), 
	    				"Members of " + roomName, 
	    				JOptionPane.INFORMATION_MESSAGE);
	    	}
	    	
	    } else if (message.startsWith("/r/joined/")) {
	        // /r/joined/RoomName/e/
	        String[] parts = message.split("/r/joined/|/e/");
	        if(parts.length > 1) {
		        String room = parts[1];
		        System.out.println("Client: Received joined confirmation for: " + room);
		        addRoomTab(room, true);
	        }
	        
	    } else if (message.startsWith("/r/join_failed/")) {
	        String reason = message.split("/r/join_failed/|/e/")[1];
	        JOptionPane.showMessageDialog(this, reason, "Join Failed", JOptionPane.ERROR_MESSAGE);
	        
	    } else if (message.startsWith("/r/list/")) {
	    	// /r/list/RoomA:Creator,RoomB:Creator/e/
	    	String[] split = message.split("/r/list/|/e/");
	    	if(split.length <= 1 || split[1].isEmpty()) {
	    		JOptionPane.showMessageDialog(this, "No active rooms found.");
	    		return;
	    	}
	    	String data = split[1];
	    	String[] roomsWithCreators = data.split(",");
            if(roomListDialog == null || !roomListDialog.isVisible()) {
            	roomListDialog = new RoomListDialog(this, roomsWithCreators);
            } else {
            	// Update existing?
            	roomListDialog.dispose();
            	roomListDialog = new RoomListDialog(this, roomsWithCreators);
            }
        }
	}
	
	private Color getUserColor(String name) {
		int hash = name.hashCode();
		int r = (hash & 0xFF0000) >> 16;
		int g = (hash & 0x00FF00) >> 8;
		int b = hash & 0x0000FF;
		if (r < 50 && g < 50 && b < 50) {
			r += 100; g += 100; b += 100;
		}
		return new Color(r, g, b);
	}

	// --- INNER CLASS FOR ROOM TABS ---
	
	class ChatPanel extends JPanel {
	    String roomName;
	    JTextPane history;
	    StyledDocument doc;
	    JTextField message;
	    
	    public ChatPanel(String roomName) {
	        this.roomName = roomName;
	        setLayout(new BorderLayout(5, 5));
	        
	        history = new JTextPane();
	        history.setEditable(false);
	        history.setFont(new java.awt.Font("Verdana", java.awt.Font.PLAIN, 12));
	        doc = history.getStyledDocument();
	        
	        DefaultCaret caret = (DefaultCaret) history.getCaret();
	        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
	        
	        add(new JScrollPane(history), BorderLayout.CENTER);
	        
	        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
	        message = new JTextField();
	        JButton sendBtn = new JButton("Send");
	        
	        ActionListener sendAction = e -> sendMessage();
	        message.addActionListener(sendAction);
	        sendBtn.addActionListener(sendAction);
	        
	        bottomPanel.add(message, BorderLayout.CENTER);
	        bottomPanel.add(sendBtn, BorderLayout.EAST);
	        
	        if (!roomName.equals("Global")) {
	            JButton inviteBtn = new JButton("+ Invite");
	            inviteBtn.addActionListener(e -> showInviteDialog());
	            bottomPanel.add(inviteBtn, BorderLayout.WEST);
	            
	            // Members button (using a panel for WEST to hold both)
	            JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
	            controlPanel.add(inviteBtn);
	            JButton membersBtn = new JButton("Members");
	            membersBtn.addActionListener(e -> requestMembers());
	            controlPanel.add(membersBtn);
	            bottomPanel.add(controlPanel, BorderLayout.WEST);
	        }
	        
	        add(bottomPanel, BorderLayout.SOUTH);
	    }
	    
	    private void requestMembers() {
	    	client.send(("/r/req-members/" + roomName + "/" + client.getID() + "/e/").getBytes());
	    }
	    
	    private void sendMessage() {
	        String text = message.getText();
	        if(text.isEmpty()) return;
	        
	        String msg = client.getName() + " (" + client.getID() + "): " + text;
	        
	        String packet = "/m/" + roomName + "/" + msg + "/e/";
	        client.send(packet);
	        message.setText("");
	    }
	    
	    public void console(String msg, Color c, boolean isMe) {
	        Style style = history.addStyle("Style", null);
	        StyleConstants.setForeground(style, c);
	        int len = doc.getLength();
	        try {
				doc.insertString(len, msg + "\n", style);
				
				SimpleAttributeSet parAttrs = new SimpleAttributeSet();
				if (isMe) {
					StyleConstants.setAlignment(parAttrs, StyleConstants.ALIGN_RIGHT);
				} else {
					StyleConstants.setAlignment(parAttrs, StyleConstants.ALIGN_LEFT);
				}
				doc.setParagraphAttributes(len, 1, parAttrs, false);
				
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
	        history.setCaretPosition(doc.getLength());
	    }
	    
	    private void showInviteDialog() {
	    	new InviteUserDialog(ClientWindow.this, currentOnlineUsers, roomName);
	    }
	}
	
	// --- TAB COMPONENT FOR CLOSING ---
	
	class ButtonTabComponent extends JPanel {
	    private final JTabbedPane pane;
	    
	    public ButtonTabComponent(final JTabbedPane pane) {
	        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
	        if (pane == null) {
	            throw new NullPointerException("TabbedPane is null");
	        }
	        this.pane = pane;
	        setOpaque(false);
	        
	        //make JLabel read titles from JTabbedPane
	        JLabel label = new JLabel() {
	            public String getText() {
	                int i = pane.indexOfTabComponent(ButtonTabComponent.this);
	                if (i != -1) {
	                    return pane.getTitleAt(i);
	                }
	                return null;
	            }
	        };
	        
	        add(label);
	        //add more space between the label and the button
	        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
	        //tab button
	        JButton button = new TabButton();
	        add(button);
	        //add more space to the top of the component
	        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
	    }

	    private class TabButton extends JButton implements ActionListener {
	        public TabButton() {
	            int size = 17;
	            setPreferredSize(new Dimension(size, size));
	            setToolTipText("close this tab");
	            //Make the button looks the same for all Laf's
	            setUI(new BasicButtonUI());
	            //Make it transparent
	            setContentAreaFilled(false);
	            setFocusable(false);
	            setBorder(BorderFactory.createEtchedBorder());
	            setBorderPainted(false);
	            //Making nice rollover effect
	            //we use the same listener for all buttons
	            addMouseListener(new MouseAdapter() {
	                public void mouseEntered(MouseEvent e) {
	                    Component component = e.getComponent();
	                    if (component instanceof AbstractButton) {
	                        AbstractButton button = (AbstractButton) component;
	                        button.setBorderPainted(true);
	                    }
	                }

	                public void mouseExited(MouseEvent e) {
	                    Component component = e.getComponent();
	                    if (component instanceof AbstractButton) {
	                        AbstractButton button = (AbstractButton) component;
	                        button.setBorderPainted(false);
	                    }
	                }
	            });
	            setRolloverEnabled(true);
	            //Close the proper tab by clicking the button
	            addActionListener(this);
	        }

	        public void actionPerformed(ActionEvent e) {
	            int i = pane.indexOfTabComponent(ButtonTabComponent.this);
	            if (i != -1) {
	            	String title = pane.getTitleAt(i);
	            	client.send(("/r/leave/" + title + "/" + client.getID() + "/e/").getBytes());
	            	openRooms.remove(title);
	                pane.remove(i);
	            }
	        }

	        //we don't want to update UI for this button
	        public void updateUI() {
	        }

	        //paint the cross
	        protected void paintComponent(Graphics g) {
	            super.paintComponent(g);
	            Graphics2D g2 = (Graphics2D) g.create();
	            //shift the image for pressed buttons
	            if (getModel().isPressed()) {
	                g2.translate(1, 1);
	            }
	            g2.setStroke(new BasicStroke(2));
	            g2.setColor(Color.BLACK);
	            if (getModel().isRollover()) {
	                g2.setColor(Color.MAGENTA);
	            }
	            int delta = 6;
	            g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
	            g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
	            g2.dispose();
	        }
	    }
	}
	
	// --- ROOM LIST DIALOG ---
	
	class RoomListDialog extends JDialog {
		public RoomListDialog(ClientWindow owner, String[] roomsRawData) { // roomsRawData matches split from server
			super(owner, "Available Rooms", true);
			setSize(500, 300);
			setLocationRelativeTo(owner);
			
			String[] columnNames = {"Room Name", "Created By", "Users", "Locked", "Action"};
			// Input format: Name:Creator:HasPass:Count
			Object[][] data = new Object[roomsRawData.length][5];
			
			for(int i=0; i<roomsRawData.length; i++) {
				String[] parts = roomsRawData[i].split(":");
				data[i][0] = parts[0];
				data[i][1] = parts.length > 1 ? parts[1] : "Unknown";
				int hasPass = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
				data[i][2] = parts.length > 3 ? parts[3] : "0";
				data[i][3] = hasPass == 1 ? "Yes" : "No";
				data[i][4] = "Join";
			}
			
			DefaultTableModel model = new DefaultTableModel(data, columnNames) {
				public boolean isCellEditable(int row, int column) {
					return column == 4;
				}
			};
			
			JTable table = new JTable(model);
			
			table.getColumn("Action").setCellRenderer(new ButtonRenderer());
			table.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox(), this, table));
			
			add(new JScrollPane(table));
			
			setVisible(true);
		}
		
		class ButtonRenderer extends JButton implements TableCellRenderer {
		    public ButtonRenderer() {
		        setOpaque(true);
		    }

		    public Component getTableCellRendererComponent(JTable table, Object value,
		            boolean isSelected, boolean hasFocus, int row, int column) {
		        if (isSelected) {
		            setForeground(table.getSelectionForeground());
		            setBackground(table.getSelectionBackground());
		        } else {
		            setForeground(table.getForeground());
		            setBackground(UIManager.getColor("Button.background"));
		        }
		        setText((value == null) ? "" : value.toString());
		        return this;
		    }
		}

		class ButtonEditor extends DefaultCellEditor {
		    protected JButton button;
		    private String label;
		    private boolean isPushed;
		    private JTable table;
		    private RoomListDialog dialog;

		    public ButtonEditor(JCheckBox checkBox, RoomListDialog dialog, JTable table) {
		        super(checkBox);
		        this.dialog = dialog;
		        this.table = table;
		        button = new JButton();
		        button.setOpaque(true);
		        button.addActionListener(e -> fireEditingStopped());
		    }

		    public Component getTableCellEditorComponent(JTable table, Object value,
		            boolean isSelected, int row, int column) {
		        if (isSelected) {
		            button.setForeground(table.getSelectionForeground());
		            button.setBackground(table.getSelectionBackground());
		        } else {
		            button.setForeground(table.getForeground());
		            button.setBackground(table.getBackground());
		        }
		        label = (value == null) ? "" : value.toString();
		        button.setText(label);
		        isPushed = true;
		        return button;
		    }

		    public Object getCellEditorValue() {
		        if (isPushed) {
		        	int row = table.getSelectedRow();
		        	String roomName = (String) table.getValueAt(row, 0);
		        	String locked = (String) table.getValueAt(row, 3);
		        	
		        	String pass = "null";
		        	if("Yes".equals(locked)) {
			        	// Ask for password
			        	pass = JOptionPane.showInputDialog(dialog, "Enter Password:");
			        	if(pass == null) {
			        		isPushed = false;
			        		return label; // Cancelled
			        	}
		        	}
		        	
		        	joinRoom(roomName, pass);
		        }
		        isPushed = false;
		        return label;
		    }

		    public boolean stopCellEditing() {
		        isPushed = false;
		        return super.stopCellEditing();
		    }
		}
	}
	
	// --- INVITE USER DIALOG ---
	
	class InviteUserDialog extends JDialog {
		public InviteUserDialog(ClientWindow owner, String[] users, String roomName) {
			super(owner, "Invite Users to " + roomName, true);
			setSize(400, 300);
			setLocationRelativeTo(owner);
			
			String[] columnNames = {"Name", "ID", "Action"};
			// Users format: Name(ID)
			Object[][] data = new Object[users.length][3];
			
			for(int i=0; i<users.length; i++) {
				String u = users[i];
				try {
					String name = u.substring(0, u.indexOf("("));
					String id = u.substring(u.indexOf("(") + 1, u.indexOf(")"));
					data[i][0] = name;
					data[i][1] = id;
					data[i][2] = "Invite";
				} catch (Exception e) {
					data[i][0] = u;
					data[i][1] = "???";
					data[i][2] = "Invite";
				}
			}
			
			DefaultTableModel model = new DefaultTableModel(data, columnNames) {
				public boolean isCellEditable(int row, int column) {
					return column == 2;
				}
			};
			
			JTable table = new JTable(model);
			
			// Use our own renderer, do not instantiate RoomListDialog!
			table.getColumn("Action").setCellRenderer(new InviteButtonRenderer());
			table.getColumn("Action").setCellEditor(new InviteButtonEditor(new JCheckBox(), this, table, roomName));
			
			add(new JScrollPane(table));
			
			setVisible(true);
		}
		
		class InviteButtonRenderer extends JButton implements TableCellRenderer {
		    public InviteButtonRenderer() { setOpaque(true); }
		    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		        if (isSelected) { setForeground(table.getSelectionForeground()); setBackground(table.getSelectionBackground()); }
		        else { setForeground(table.getForeground()); setBackground(UIManager.getColor("Button.background")); }
		        setText((value == null) ? "" : value.toString());
		        return this;
		    }
		}

		class InviteButtonEditor extends DefaultCellEditor {
		    protected JButton button;
		    private String label;
		    private boolean isPushed;
		    private JTable table;
		    private String roomName;

		    public InviteButtonEditor(JCheckBox checkBox, JDialog dialog, JTable table, String roomName) {
		        super(checkBox);
		        this.table = table;
		        this.roomName = roomName;
		        button = new JButton();
		        button.setOpaque(true);
		        button.addActionListener(e -> fireEditingStopped());
		    }

		    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		        if (isSelected) { button.setForeground(table.getSelectionForeground()); button.setBackground(table.getSelectionBackground()); }
		        else { button.setForeground(table.getForeground()); button.setBackground(table.getBackground()); }
		        label = (value == null) ? "" : value.toString();
		        button.setText(label);
		        isPushed = true;
		        return button;
		    }

		    public Object getCellEditorValue() {
		        if (isPushed) {
		        	int row = table.getSelectedRow();
		        	String idStr = (String) table.getValueAt(row, 1);
		        	int targetID = Integer.parseInt(idStr);
		        	client.send(("/r/invite/" + targetID + "/" + roomName + "/e/").getBytes());
		    	    JOptionPane.showMessageDialog(button, "Invitation sent!");
		        }
		        isPushed = false;
		        return label;
		    }

		    public boolean stopCellEditing() {
		        isPushed = false;
		        return super.stopCellEditing();
		    }
		}
	}
}
