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

	private static GUI gui;

	private String secretKey;

	public static final String name = "ChatBoard";
	public static final File configurationFile = new File("." + System.getProperty("file.separator") + name);
	public static final int tcpPort = 9723;

	public static void main(String[] args){
		try {

			// Creation of network manager
			NetworkManager myNetworkManager = new NetworkManager(NetworkManager.ConfigMode.EDGE, name, configurationFile.toURI());

			// Retrieving the network configurator
			NetworkConfigurator myNetworkConfigurator = myNetworkManager.getConfigurator();

			// Setting Configuration
			myNetworkConfigurator.setTcpPort(tcpPort);
			myNetworkConfigurator.setTcpEnabled(true);
			myNetworkConfigurator.setTcpIncoming(true);
			myNetworkConfigurator.setTcpOutgoing(true);

			// Starting the JXTA network
			PeerGroup netPeerGroup = myNetworkManager.startNetwork();

			ChatBoard cb = new ChatBoard();         

			RendezVousService rdv = netPeerGroup.getRendezVousService();
			while(!rdv.getLocalRendezVousView().isEmpty())
			{
				try {
					Thread.sleep(1000);
				}
				catch(Exception ex) {}
			}
			gui = new GUI(cb, netPeerGroup);

		} catch (PeerGroupException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}

	}

	public void joinedGroup(PeerGroup group)
	{
		this.group = group;
		pipes = group.getPipeService();
		PipeAdvertisement adv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
		PipeID pid=(PipeID)IDFactory.newPipeID(group.getPeerGroupID(),computeHash(secretKey).getBytes());
		adv.setPipeID(pid);
		adv.setType(PipeService.PropagateType);
		adv.setName("ChatPipe");
		DiscoveryService discovery = group.getDiscoveryService();

		try {
			discovery.publish(adv);
			discovery.remotePublish(adv);
			connectPipe(adv);
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
	}

	public void connectPipe(PipeAdvertisement adv) throws java.io.IOException
	{
		if (inPipe != null){
			inPipe.close();
		}

		inPipe = null;

		if (outPipe != null){
			outPipe.close();
		}

		outPipe = null;

		inPipe = pipes.createInputPipe(adv, this);
		outPipe = pipes.createOutputPipe(adv, -1);
	}


	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void sendMessage(String sender, String text) throws IOException 
	{
		if (outPipe == null) {
			throw new IOException("Not connected yet.\n");
		}
		
		StructuredDocument doc=StructuredDocumentFactory.newStructuredDocument(mimeType, "JXTA-Tutorial:ChatMsg");
		doc.appendChild(doc.createElement("Text", encrypt(text)));
		doc.appendChild(doc.createElement("Sender", encrypt(sender)));
		doc.appendChild(doc.createElement("Hash", encrypt(computeHash(sender+text))));

		Message msg = new Message();
		msg.addMessageElement(new TextDocumentMessageElement("ChatMsg", (TextDocument) doc,null));
		outPipe.send(msg);
	}


	@SuppressWarnings("rawtypes")
	public void pipeMsgEvent(PipeMsgEvent msg) {
		MessageElement element = msg.getMessage().getMessageElement("ChatMsg");
		StructuredDocument doc;
		try {
			doc = StructuredDocumentFactory.newStructuredDocument(mimeType,
					element.getStream());
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			return;
		}

		String nick = null;
		String text = null;
		String hash = null;
		Enumeration enums = doc.getChildren();

		while (enums.hasMoreElements()) {
			Element el = (Element) enums.nextElement();
			if (el.getKey().equals("Sender"))
				nick = decrypt((String) el.getValue());
			if (el.getKey().equals("Text"))
				text = decrypt((String) el.getValue());
			if (el.getKey().equals("Hash"))
				hash = decrypt((String) el.getValue());

		}

		if (checkHash(hash, computeHash(nick + text))) {
			gui.appendMessage(nick, text);
		} else {
			gui.appendMessage(nick, text + " Achtung! Nachricht wurde möglicherweise geändert!");
		}
	}

	public void setSecretKey(String secretKey){
		this.secretKey = secretKey;
	}

	private String encrypt(String message)  {
		String base64EncryptedString="";
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digestOfPassword = md.digest(secretKey.getBytes("utf-8"));
			byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);
			SecretKey key = new SecretKeySpec(keyBytes, "DESede");
			Cipher cipher = Cipher.getInstance("DESede");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] plainTextBytes;

			plainTextBytes = message.getBytes("utf-8");

			byte[] buf = cipher.doFinal(plainTextBytes);
			byte[] base64Bytes = Base64.encode(buf);
			base64EncryptedString = new String(base64Bytes);

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return base64EncryptedString;
	}

	private String decrypt(String encryptedText)  {
		String decMessage="";
		try {
			byte[] message = Base64.decode(encryptedText.getBytes("utf-8"));
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digestOfPassword = md.digest(secretKey.getBytes("utf-8"));
			byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);
			SecretKey key = new SecretKeySpec(keyBytes, "DESede");
			Cipher decipher = Cipher.getInstance("DESede");
			decipher.init(Cipher.DECRYPT_MODE, key);
			byte[] plainText = decipher.doFinal(message);
			decMessage=new String(plainText, "UTF-8");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return decMessage;
	}
	public static String computeHash(String text){

		String base64HashText="";
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digestOfText = md.digest(text.getBytes("utf-8"));
			byte[] base64Bytes = Base64.encode(digestOfText);
			base64HashText = new String(base64Bytes);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return base64HashText;
	}

	public static boolean checkHash(String str1, String str2){
		return ChatBoard.computeHash(str1).equals(ChatBoard.computeHash(str2));
	}

}
