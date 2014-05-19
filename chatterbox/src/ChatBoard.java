import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.cert.CertificateException;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;

import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.mortbay.servlet.SendRedirect;

import net.jxse.configuration.JxseConfigurationTool;
import net.jxse.configuration.JxsePeerConfiguration;
import net.jxta.configuration.JxtaConfigurationException;
import net.jxta.credential.AuthenticationCredential;
import net.jxta.credential.Credential;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.Element;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.TextDocument;
import net.jxta.document.XMLElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.TextDocumentMessageElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.exception.ProtocolNotSupportedException;
import net.jxta.id.IDFactory;
import net.jxta.impl.access.pse.PSEAccessService;
import net.jxta.impl.content.ContentServiceImpl;
import net.jxta.impl.membership.pse.DialogAuthenticator;
import net.jxta.impl.membership.pse.FileKeyStoreManager;
import net.jxta.impl.membership.pse.PSEMembershipService;
import net.jxta.impl.membership.pse.PSEUtils;
import net.jxta.impl.membership.pse.StringAuthenticator;
import net.jxta.impl.peergroup.CompatibilityUtils;
import net.jxta.impl.peergroup.StdPeerGroup;
import net.jxta.impl.peergroup.StdPeerGroupParamAdv;
import net.jxta.membership.MembershipService;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.NetPeerGroupFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.peergroup.WorldPeerGroupFactory;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.platform.Module;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.rendezvous.RendezVousService;


public class ChatBoard implements PipeMsgListener
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
	
	private String secretKey;

	public static final String name = "ChatBoard";
	public static final File configurationFile = new File("." + System.getProperty("file.separator") + name);
	public static final int tcpPort = 9723;

	public static final String pipeType = PipeService.UnicastType;

	public static final PeerID pID = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, name.getBytes());

	public static final String myPrincipalName = "Principal - " + name;
	public static final String myPrivateKeyPassword = "PrivateKey Password - " + name;

	public static final String myKeyStoreFileName = "MyKeyStoreFile";
	public static final String myKeyStoreLocation = "." + System.getProperty("file.separator") + name + File.separator + "MyKeyStoreLocation";
	public static final String myKeyStorePassword = "KeyStore Password - " + name;
	public static final String myKeyStoreProvider = "KeyStore Provider - " + name;

	public static final File myKeyStoreDirectory = new File(myKeyStoreLocation);
	public static final File myKeyStoreFile = new File(myKeyStoreLocation + File.separator + myKeyStoreFileName);

	public static X509Certificate theX509Certificate;
	public static PrivateKey thePrivateKey;

	public static final String psePeerGroupName = "PSE ChatBoard";
	public static final PeerGroupID psePeerGroupID = IDFactory.newPeerGroupID(PeerGroupID.defaultNetPeerGroupID, psePeerGroupName.getBytes());

	static {

		try {
			if(!new File(configurationFile.getCanonicalPath()).exists()){
				// Static initialization of certificates
				PSEUtils.IssuerInfo ForPSE = PSEUtils.genCert(name, null);

				theX509Certificate = ForPSE.cert;
				thePrivateKey = ForPSE.issuerPkey;
			}
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args) throws Exception{
		try {

			KeyStore myKeyStore;
			FileKeyStoreManager myFileKeyStoreManager = null;

			if(!new File(configurationFile.getCanonicalPath()).exists()){

				// Preparing data
				myKeyStoreDirectory.mkdirs();

				// Creating the key store
				myFileKeyStoreManager = new FileKeyStoreManager((String)null, myKeyStoreProvider, myKeyStoreFile);

				myFileKeyStoreManager.createKeyStore(myKeyStorePassword.toCharArray());

				if (!myFileKeyStoreManager.isInitialized()) {
					System.out.println("Keystore is NOT initialized");
				} else {
					System.out.println("Keystore is initialized");
				}

				// Loading the (empty) keystore 
				myKeyStore = myFileKeyStoreManager.loadKeyStore(myKeyStorePassword.toCharArray());

				// Setting data
				X509Certificate[] Temp = { theX509Certificate };
				myKeyStore.setKeyEntry(pID.toString(), thePrivateKey, myPrivateKeyPassword.toCharArray(), Temp);

				// Saving the data
				myFileKeyStoreManager.saveKeyStore(myKeyStore, myKeyStorePassword.toCharArray());

			}

			myFileKeyStoreManager = new FileKeyStoreManager((String)null, myKeyStoreProvider, myKeyStoreFile);

			// Reloading the KeyStore
			myKeyStore = myFileKeyStoreManager.loadKeyStore(myKeyStorePassword.toCharArray());

			// Retrieving Certificate
			X509Certificate myCertificate = (X509Certificate) myKeyStore.getCertificate(pID.toString());

			if (myCertificate==null) {
				System.out.println("X509 Certificate CANNOT be retrieved");
			} else {
				System.out.println("X509 Certificate can be retrieved");
			}

			// Retrieving private key 
			PrivateKey myPrivateKey = (PrivateKey) myKeyStore.getKey(pID.toString(), myPrivateKeyPassword.toCharArray());

			if (myPrivateKey==null) {
				System.out.println("Private key CANNOT be retrieved");
			} else {
				System.out.println("Private key can be retrieved");
				System.out.println(myPrivateKey.toString());
			}




			// Creation of network manager
			NetworkManager myNetworkManager = new NetworkManager(NetworkManager.ConfigMode.EDGE, name, configurationFile.toURI());

			// Retrieving the network configurator
			NetworkConfigurator myNetworkConfigurator = myNetworkManager.getConfigurator();

			// Setting Configuration
			myNetworkConfigurator.setTcpPort(tcpPort);
			myNetworkConfigurator.setTcpEnabled(true);
			myNetworkConfigurator.setTcpIncoming(true);
			myNetworkConfigurator.setTcpOutgoing(true);
			myNetworkConfigurator.setTcpInterfaceAddress("10.0.0.13");


			// Setting the keystore
			myNetworkConfigurator.setKeyStoreLocation(myKeyStoreFile.toURI());
			myNetworkConfigurator.setPassword(myKeyStorePassword);


			// Starting the JXTA network
			PeerGroup netPeerGroup = myNetworkManager.startNetwork();



			// Checking membership implementation
			MembershipService NPGMembership = netPeerGroup.getMembershipService();

			// Creating a child group with PSE
			PeerGroup childPeerGroup = netPeerGroup.newGroup(psePeerGroupID, createAllPurposePeerGroupWithPSEModuleImplAdv(), psePeerGroupName, "Checking PSE...");

			if (Module.START_OK != childPeerGroup.startApp(new String[0]))
				System.out.println("Cannot start PSE peergroup");

			// Checking membership implementation
			MembershipService childGroupMembership = childPeerGroup.getMembershipService();

			// Joining the peer group
			AuthenticationCredential myAuthenticationCredit = new AuthenticationCredential(netPeerGroup, "DialogAuthentication", null);

			//DialogAuthenticator dialogAuthenticator = (DialogAuthenticator) childGroupMembership.apply(myAuthenticationCredit);

			//dialogAuthenticator.interact();

			StringAuthenticator myStringAuthenticator = (StringAuthenticator) childGroupMembership.apply(myAuthenticationCredit);

			myStringAuthenticator.setAuth1_KeyStorePassword(myKeyStorePassword);
			myStringAuthenticator.setAuth2Identity(pID);
			myStringAuthenticator.setAuth3_IdentityPassword(myPrivateKeyPassword);

			Credential myCredential = null;

			if (myStringAuthenticator.isReadyForJoin()) {
				myCredential = childGroupMembership.join(myStringAuthenticator);
			}

			if (myCredential!=null) {
				System.out.println("Credentials created successfully");
			} else {
				System.out.println("Credentials NOT created successfully");
			}



			ChatBoard cb = new ChatBoard();         

			RendezVousService rdv = childPeerGroup.getRendezVousService();
			while(!rdv.getLocalRendezVousView().isEmpty())
			{
				try {
					Thread.sleep(1000);
				}
				catch(Exception ex) {}
			}

			cb.joinedGroup(childPeerGroup);

			System.out.println("Peer Group ID: " + childPeerGroup.getPeerGroupID());

			cb.setSecretKey("gdfg435dsdgfgdfg3");
			
			while(true){
				String text = "Hallo Text!";
				cb.sendMessage(InetAddress.getLocalHost().getHostAddress(), text);
				
				String encrypted = cb.encrypt(text);
				cb.sendMessage(InetAddress.getLocalHost().getHostAddress(), "Encrypted: " + encrypted);
				
				String decrypted = cb.decrypt(encrypted);
				cb.sendMessage(InetAddress.getLocalHost().getHostAddress(), "Decrypted: " + decrypted);
				
				try {
					Thread.sleep(1000);
				}
				catch(Exception ex) {}
			}

		} catch (PeerGroupException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setSecretKey(String secretKey) throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException{
		this.secretKey = secretKey;
	}

	public String encrypt(String message) throws Exception {
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

	public String decrypt(String encryptedText) throws Exception {
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
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] digestOfText = md.digest(text.getBytes("utf-8"));
		byte [] base64Bytes = Base64.encode(digestOfText);
		String base64HashText = new String(base64Bytes);
		return base64HashText;
	}
	
	public static boolean checkHash(String str1, String str2) throws Exception{
		return ChatBoard.computeHash(str1).equals(ChatBoard.computeHash(str2));
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
		PipeID pid = getChatPipeID(group.getPeerGroupID());
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
			System.out.println(pid);
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

	private void sendMessage() throws IOException
	{
		if (outPipe == null) {
			board.append("Not connected yet.\n");
			return;
		}
		String text = input.getText();
		if (text == null || text.trim().length() == 0)
			return;
		input.setText("");
		sendMessage(nickname.getText(), text);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void sendMessage(String sender, String text) throws IOException
	{
		StructuredDocument doc=StructuredDocumentFactory.newStructuredDocument(mimeType, "JXTA-Tutorial:ChatMsg");
		doc.appendChild(doc.createElement("Text", text));
		doc.appendChild(doc.createElement("Sender", sender));

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
		Enumeration enums = doc.getChildren();
		while (enums.hasMoreElements()) {
			Element el = (Element) enums.nextElement();
			if (el.getKey().equals("Sender"))
				nick = (String) el.getValue();
			if (el.getKey().equals("Text"))
				text = (String) el.getValue();
		}
		//appendMessage(nick, text);
		System.out.println(nick + ": " + text);
	}

	private void appendMessage(String nick, String text) {
		board.append(nick+"  " + text + "\n");
		JViewport port = scrollPane.getViewport();
		int y = board.getHeight() - board.getVisibleRect().height;
		port.setViewPosition(new Point(0, y));
	}


	public static ModuleImplAdvertisement createAllPurposePeerGroupWithPSEModuleImplAdv() {

		ModuleImplAdvertisement implAdv = CompatibilityUtils.createModuleImplAdvertisement(
				PeerGroup.allPurposePeerGroupSpecID, StdPeerGroup.class.getName(),
				"General Purpose Peer Group with PSE Implementation");

		// Create the service list for the group.
		StdPeerGroupParamAdv paramAdv = new StdPeerGroupParamAdv();

		// set the services
		paramAdv.addService(PeerGroup.endpointClassID, PeerGroup.refEndpointSpecID);
		paramAdv.addService(PeerGroup.resolverClassID, PeerGroup.refResolverSpecID);
		paramAdv.addService(PeerGroup.membershipClassID, PSEMembershipService.pseMembershipSpecID);
		paramAdv.addService(PeerGroup.accessClassID, PSEAccessService.PSE_ACCESS_SPEC_ID);

		// standard services
		paramAdv.addService(PeerGroup.discoveryClassID, PeerGroup.refDiscoverySpecID);
		paramAdv.addService(PeerGroup.rendezvousClassID, PeerGroup.refRendezvousSpecID);
		paramAdv.addService(PeerGroup.pipeClassID, PeerGroup.refPipeSpecID);
		paramAdv.addService(PeerGroup.peerinfoClassID, PeerGroup.refPeerinfoSpecID);

		paramAdv.addService(PeerGroup.contentClassID, ContentServiceImpl.MODULE_SPEC_ID);

		// Insert the newParamAdv in implAdv
		XMLElement paramElement = (XMLElement) paramAdv.getDocument(MimeMediaType.XMLUTF8);
		implAdv.setParam(paramElement);

		return implAdv;

	}
}
