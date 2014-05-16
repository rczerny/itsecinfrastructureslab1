import java.awt.Point;
import java.io.IOException;
import java.util.Enumeration;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;

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
import net.jxta.peergroup.NetPeerGroupFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.InputPipe;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.rendezvous.RendezVousService;


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

	
	public static void main(String[] args) {
		try {
			NetPeerGroupFactory npgf=new NetPeerGroupFactory();
			PeerGroup netGroup=npgf.getNetPeerGroup();
			
			RendezVousService rdv=netGroup.getRendezVousService();
			while(!rdv.getLocalRendezVousView().isEmpty())
			{
				try {Thread.sleep(1000);}
				catch(Exception ex) {}
			}
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
		PipeID pid = getChatPipeID(group.getPeerGroupID());
		adv.setPipeID(pid);
		adv.setType(PipeService.PropagateType);
		adv.setName("ChatPipe");
		DiscoveryService discovery = group.getDiscoveryService();
		try {
			discovery.publish(adv);
			discovery.remotePublish(adv);
			connectPipe(adv);
			board.append("\nWelcome to group '" + group.getPeerGroupName()
					+ "'\n");
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
	doc.appendChild(doc.createElement("Sender", nickname.getText()));

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
		appendMessage(nick, text);
	}

	private void appendMessage(String nick, String text) {
		board.append(nick+"  " + text + "\n");
		JViewport port = scrollPane.getViewport();
		int y = board.getHeight() - board.getVisibleRect().height;
		port.setViewPosition(new Point(0, y));
	}
}
