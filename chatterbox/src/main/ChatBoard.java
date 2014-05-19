package main;

import gui.GUI;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.jxta.configuration.JxtaConfigurationException;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.TextDocument;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.rendezvous.RendezVousService;

import org.bouncycastle.util.encoders.Base64;


public class ChatBoard implements  PipeMsgListener
{

	//JXTA
	private PeerGroup group;
	private PipeService pipes;
	private InputPipe inPipe;
	private OutputPipe outPipe;
	private MimeMediaType mimeType=new MimeMediaType("text", "xml");

	//Swing
	private JPanel panel;
	private JScrollPane scrollPane;
	private JTextArea board;
	private JTextField input;
	private JTextField nickname;
	
	private static GUI gui;
	
	private String secretKey;
	
	public static final String Name = "ChatBoard";
	public static final File ConfigurationFile = new File("." + System.getProperty("file.separator") + Name);
    public static final int TcpPort = 9723;

	public static void main(String[] args) throws JxtaConfigurationException, IOException{
		try {

//			NetworkConfigurator config = new NetworkConfigurator();
//
//			if (!config.exists()) {
//				// Create a new configuration with a new name, principal, and pass
//				config.setName("New Name");
//				config.setPrincipal("username");
//				config.setPassword("password");
//				try {
//					//persist it
//					config.save();
//				} catch (IOException io) {
//					// deal with the io error
//				}
//			} else {
//				// Load the pre-existing configuration
//				File pc = new File(config.getHome(), "PlatformConfig");
//				try {
//					config.load(pc.toURI());
//					// make changes if so desired
//					// store the PlatformConfig under the default home
//					config.save();
//				} catch (CertificateException ce) {
//					// In case the root cert is invalid, this creates a new one
//					try {
//						//principal
//						config.setPrincipal("principal");
//						//password to encrypt private key with
//						config.setPassword("password");
//						config.save();
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//				}
//			}

            // Creation of network manager
            NetworkManager MyNetworkManager = new NetworkManager(NetworkManager.ConfigMode.EDGE,
                    Name, ConfigurationFile.toURI());
            
            // Retrieving the network configurator
            NetworkConfigurator MyNetworkConfigurator = MyNetworkManager.getConfigurator();
            
            // Setting Configuration
            MyNetworkConfigurator.setTcpPort(TcpPort);
            MyNetworkConfigurator.setTcpEnabled(true);
            MyNetworkConfigurator.setTcpIncoming(true);
            MyNetworkConfigurator.setTcpOutgoing(true);

            
            // Starting the JXTA network
            PeerGroup NetPeerGroup = MyNetworkManager.startNetwork();
            
            ChatBoard cb = new ChatBoard();         
            
			RendezVousService rdv=NetPeerGroup.getRendezVousService();
			while(!rdv.getLocalRendezVousView().isEmpty())
			{
				try {
					Thread.sleep(1000);
				}
				catch(Exception ex) {}
			}
			gui=new GUI(cb,NetPeerGroup);
			//cb.joinedGroup(NetPeerGroup);
			/*
			while(true){
				cb.sendMessage(InetAddress.getLocalHost().getHostAddress(), "Hallo");
				try {
					Thread.sleep(1000);
				}
				catch(Exception ex) {}
			}
			**/
			
		} catch (PeerGroupException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



	}
	/**
	 * @see biz.junginger.jxta.Groups.Listener#groupJoined(PeerGroup)
	 */
	public void joinedGroup(PeerGroup group)
	{
		this.group = group;
		pipes = group.getPipeService();
		PipeAdvertisement adv = (PipeAdvertisement) AdvertisementFactory
				.newAdvertisement(PipeAdvertisement.getAdvertisementType());
		//PipeID pid = getChatPipeID(group.getPeerGroupID());
		PipeID pid=(PipeID)IDFactory.newPipeID(group.getPeerGroupID(),secretKey.getBytes());
		adv.setPipeID(pid);
		adv.setType(PipeService.PropagateType);
		adv.setName("ChatPipe");
		DiscoveryService discovery = group.getDiscoveryService();
		try {
			discovery.publish(adv);
			discovery.remotePublish(adv);
			connectPipe(adv);
//			board.append("\nWelcome to group '" + group.getPeerGroupName()
//					+ "'\n");
			System.out.println("joined group");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 *  @see biz.junginger.jxta.Groups.Listener#createdGroup(net.jxta.peergroup.PeerGroup)
	 */
	public void createdGroup(PeerGroup group)
	{}

	private PipeID getChatPipeID(PeerGroupID groupID)
	{
		byte[] seed = new byte[16];
		for (int i = 0; i < seed.length; i++)
			seed[i] = (byte) 0x61;

		return(PipeID)IDFactory.newPipeID(groupID,seed);
	}

	public void connectPipe(PipeAdvertisement adv) throws java.io.IOException
	{
		if (inPipe != null)
			inPipe.close();
		inPipe = null;
		if (outPipe != null)
			outPipe.close();
		outPipe = null;

		inPipe = pipes.createInputPipe(adv, this);
		outPipe = pipes.createOutputPipe(adv, -1);
	}

	private void sendMessage() throws Exception
	{
		
		String text = input.getText();
		if (text == null || text.trim().length() == 0)
			return;
		input.setText("");
		sendMessage(nickname.getText(), text);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void sendMessage(String sender, String text) throws Exception
	{
		if (outPipe == null) {
			throw new IOException("Not connected yet.\n");
		}
		StructuredDocument doc=StructuredDocumentFactory.newStructuredDocument(mimeType, "JXTA-Tutorial:ChatMsg");
		doc.appendChild(doc.createElement("Text", encrypt(text)));
		doc.appendChild(doc.createElement("Sender", encrypt(sender)));
		doc.appendChild(doc.createElement("Hash", encrypt(computeHash(sender+text))));

		Message msg=new Message();
		msg.addMessageElement(new TextDocumentMessageElement("ChatMsg", (TextDocument) doc,null));
		outPipe.send(msg);
	}

	/**
	 * @see net.jxta.pipe.PipeMsgListener#pipeMsgEvent(PipeMsgEvent)
	 */
	@SuppressWarnings("rawtypes")
	public void pipeMsgEvent(PipeMsgEvent msg) {
		MessageElement element = msg.getMessage().getMessageElement("ChatMsg");
		StructuredDocument doc;
		try {
			doc = StructuredDocumentFactory.newStructuredDocument(mimeType,
					element.getStream());
		} catch (Exception ex) {
			ex.printStackTrace();
			return;
		}

		String nick = null;
		String text = null;
		String hash=null;
		Enumeration enums = doc.getChildren();
		while (enums.hasMoreElements()) {
			Element el = (Element) enums.nextElement();
			if (el.getKey().equals("Sender"))
			{
				try {
					nick = decrypt((String) el.getValue());
				} catch (Exception e) {
					System.out.println("Fehler beim Entschlüsseln!");
				}
			}
			if (el.getKey().equals("Text"))
			{
				try {
					text = decrypt((String) el.getValue());
				} catch (Exception e) {
					System.out.println("Fehler beim Entschlüsseln!");
				}
			}
			if (el.getKey().equals("Hash"))
			{
				try {
					hash = decrypt((String) el.getValue());
				} catch (Exception e) {
					System.out.println("Fehler beim Entschlüsseln!");
				}
			}
		}
		try {
			if(checkHash(hash, computeHash(nick+text)))
			{
				gui.appendMessage(nick, text);
			}
			else
			{
				gui.appendMessage(nick, text+" Achtung! Nachricht wurde möglicherweise geändert!");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(nick + ": " + text);
	}

	public void setSecretKey(String secretKey){
		this.secretKey = secretKey;
	}

	private String encrypt(String message) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] digestOfPassword = md.digest(secretKey.getBytes("utf-8"));
		byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);
		SecretKey key = new SecretKeySpec(keyBytes, "DESede");
		Cipher cipher = Cipher.getInstance("DESede");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		byte[] plainTextBytes = message.getBytes("utf-8");
		byte[] buf = cipher.doFinal(plainTextBytes);
		byte [] base64Bytes = Base64.encode(buf);
		String base64EncryptedString = new String(base64Bytes);
		return base64EncryptedString;
	}

	private String decrypt(String encryptedText) throws Exception {
		byte[] message = Base64.decode(encryptedText.getBytes("utf-8"));
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] digestOfPassword = md.digest(secretKey.getBytes("utf-8"));
		byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);
		SecretKey key = new SecretKeySpec(keyBytes, "DESede");
		Cipher decipher = Cipher.getInstance("DESede");
		decipher.init(Cipher.DECRYPT_MODE, key);
		byte[] plainText = decipher.doFinal(message);
		return new String(plainText, "UTF-8");
	}
	public static String computeHash(String text) throws Exception{
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] digestOfText = md.digest(text.getBytes("utf-8"));
		byte [] base64Bytes = Base64.encode(digestOfText);
		String base64HashText = new String(base64Bytes);
		return base64HashText;
	}
	
	public static boolean checkHash(String str1, String str2) throws Exception{
		return ChatBoard.computeHash(str1).equals(ChatBoard.computeHash(str2));
	}
	
}
