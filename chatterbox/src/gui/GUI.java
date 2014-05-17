package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;

import net.miginfocom.swing.MigLayout;

public class GUI extends JFrame implements ActionListener{

	private JPanel pane;
	private JTextField nameTextfield,pwTextfield,aliasTextfield,msgTextfield;
	private JLabel nameLabel, pwLabel,aliasLabel,msgLabel;
	private JButton loginButton,sendButton;
	private JTextArea textArea;
	private JScrollPane scrollPane;
	private String nickname;
	
	public GUI()
	{
		pane=(JPanel) this.getContentPane();
		pane.setLayout(new MigLayout("fillx"));
		
		nickname="";


		this.initWindow();
	}
	
	private void initWindow() {
			
		nameLabel=new JLabel("Benutzername:");
        nameTextfield=new JTextField(10);
        pwLabel=new JLabel("Passwort:");
        pwTextfield=new JTextField(10);
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
        pane.add(nameLabel);
	    pane.add(nameTextfield,"wrap");
        pane.add(pwLabel,"push, aligny top");
        pane.add(pwTextfield,"aligny top,wrap");
        pane.add(loginButton,"h 30!,span 2 2, align c,push, aligny top");
        
		 this.setSize(350, 330);
         this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         this.setVisible(true);
         this.setTitle("Chatterbox");
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		 
		if (e.getSource() == loginButton) {
				
			
			if (!aliasTextfield.getText().isEmpty()
					&& !nameTextfield.getText().isEmpty()
					&& !pwTextfield.getText().isEmpty()) {
				if (aliasTextfield.getText().length() <= 10
						&& aliasTextfield.getText().matches("^\\w*$")
						&& nameTextfield.getText().length() <= 10
						&& pwTextfield.getText().length() <= 15) {
					
					//Passwort�berpr�fung
					if (true) {
						
						nickname=aliasTextfield.getText();
						pane.removeAll();

						pane.add(scrollPane, "span 2, push,wrap");
						pane.add(msgLabel);
						pane.add(msgTextfield, "wrap");
						pane.add(sendButton, "h 30!,span 2 2, align c");
						this.setSize(600, 500);
						this.validate();

					} else {
						JOptionPane.showMessageDialog(this,
								"Benutzername/Passwort falsch!", "Error",
								JOptionPane.ERROR_MESSAGE);
						nameTextfield.setText("");
						pwTextfield.setText("");
						aliasTextfield.setText("");

					}
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
			
			if(!msgTextfield.getText().isEmpty())
			{
				if(msgTextfield.getText().matches("^\\w*$")&&msgTextfield.getText().length()<=40)
				{
				textArea.append(nickname+":  " + msgTextfield.getText() + "\n");
				JViewport port = scrollPane.getViewport();
				int y = textArea.getHeight() - textArea.getVisibleRect().height;
				port.setViewPosition(new Point(0, y));
				
				aliasTextfield.setText("");
				msgTextfield.setText("");
				}
				else
				{
					JOptionPane.showMessageDialog(this,"Verwendung von unerlaubten/zu vielen Zeichen!","Error",JOptionPane.ERROR_MESSAGE);
					msgTextfield.setText("");
					
				}
			}
			
			
		}
		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new GUI();

	}
	

}
