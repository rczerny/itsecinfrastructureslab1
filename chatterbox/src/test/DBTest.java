package test;

import dao.DatabaseConnector;
import dao.EventDAO;
import domain.ChatMessage;

public class DBTest {

	public static void main(String[] args){
		
		DatabaseConnector.openConnection();
		
		new EventDAO().insertEvent(new ChatMessage("nick", "hi", System.currentTimeMillis()));
		
	}
	
}
