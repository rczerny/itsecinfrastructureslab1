package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;

import main.ChatBoard;
import net.jxta.peergroup.PeerGroup;
import net.miginfocom.swing.MigLayout;

public class GUI extends JFrame implements ActionListener{

	private JPanel pane;
	private JTextField nameTextfield,aliasTextfield,msgTextfield;
	private JLabel  pwLabel,aliasLabel,msgLabel;
	private JPasswordField pwTextfield;
	private JButton loginButton,sendButton;
	private JTextArea textArea;
	private JScrollPane scrollPane;
	private String nickname;
	private ChatBoard cb;
	private PeerGroup peergroup;
	
	public GUI(ChatBoard cb,PeerGroup peergroup)
	{
		this.cb=cb;
		this.peergroup=peergroup;
		pane=(JPanel) this.getContentPane();
		pane.setLayout(new MigLayout("fillx"));
		
		nickname="";


		this.initWindow();
	}
	
	private void initWindow() {
			
        pwLabel=new JLabel("Gruppenkennung:");
        pwTextfield=new JPasswordField(10);
        loginButton=new JButton("Anmelden");
        loginButton.addActionListener(this);
        
        textArea=new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        scrollPane=new JScrollPane(textArea);
		scrollPane.setPreferredSize(new Dimension(600,600));      
        
        aliasLabel=new JLabel("Nickname:");
        aliasTextfield=new JTextField(10);
        msgLabel=new JLabel("Message:");
        msgTextfield=new JTextField(40);
        sendButton=new JButton("Senden");
        sendButton.addActionListener(this);
        
        pane.add(aliasLabel,"h 30!, push, aligny b");
        pane.add(aliasTextfield,"aligny b,wrap");
        pane.add(pwLabel,"push, aligny top");
        pane.add(pwTextfield,"aligny top,wrap");
        //pane.add(loginButton,"h 30!,span 2 2, align c,push, aligny top");
        pane.add(loginButton,"growx, span 2 2");
        
		 this.setSize(300, 150);
		 this.setResizable(false);
         this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         this.setVisible(true);
         this.setTitle("Chatterbox");
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		 
		if (e.getSource() == loginButton) {
				
			
			if (!aliasTextfield.getText().isEmpty()
					&& pwTextfield.getPassword().length >0) {
				if (aliasTextfield.getText().length() <= 10
						&& aliasTextfield.getText().matches("^\\w*$")
						&& pwTextfield.getPassword().length <= 15) {

						
						nickname=aliasTextfield.getText();
					
						
						cb.setSecretKey(new String(pwTextfield.getPassword()));
						cb.joinedGroup(peergroup);
						try {
							cb.sendMessage(nickname, "has joined the Chatgroup!>");
						} catch (IOException e1) {
							System.out.println(e1.getMessage());
						}
						pane.removeAll();
						
						pane.add(scrollPane, "span 2, push,wrap");
						pane.add(msgLabel);
						pane.add(msgTextfield, "wrap, growx");
						//pane.add(sendButton, "h 30!,span 2 2, align c");
						pane.add(sendButton, "pushx, growx, span 2 2");
						
						msgTextfield.addActionListener(new ActionListener() {
							
							@Override
							public void actionPerformed(ActionEvent e) {
								sendMessage();
								
							}
						});
						
						this.setSize(600, 500);
					
						this.validate();

				} else {
					JOptionPane
							.showMessageDialog(
									this,
									"Verwendung von unerlaubten(Nickname)/zu vielen Zeichen!",
									"Error", JOptionPane.ERROR_MESSAGE);
					nameTextfield.setText("");
					pwTextfield.setText("");
					aliasTextfield.setText("");
				}
			}
		}
		if (e.getSource() == sendButton) {
			sendMessage();		
		}
		
	}
	
	public void sendMessage(){
		if(!msgTextfield.getText().isEmpty())
		{
			if(msgTextfield.getText().matches("^[\\w\\s!.,?:-]*$")&&msgTextfield.getText().length()<=40)
			{

				try {
					cb.sendMessage(nickname, msgTextfield.getText());
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(this, e1.getMessage(),
							"Error", JOptionPane.ERROR_MESSAGE);

				}

				msgTextfield.setText("");
			}
			else
			{
				JOptionPane.showMessageDialog(this,"Verwendung von unerlaubten/zu vielen Zeichen!","Error",JOptionPane.ERROR_MESSAGE);
				msgTextfield.setText("");				
			}
		}
	}
	
	public void appendMessage(String nick, String text) {
		if(text.contains(">"))
		{
			textArea.append("<"+nick+" " + text + "\n");
		}
		else
		{
			textArea.append(nick+":  " + text + "\n");
		}
		JViewport port = scrollPane.getViewport();
		int y = textArea.getHeight() - textArea.getVisibleRect().height;
		port.setViewPosition(new Point(0, y));
	}
}
