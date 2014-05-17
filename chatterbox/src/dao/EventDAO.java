package dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import domain.ChatMessage;

public class EventDAO {

	public void insertEvent(ChatMessage message){
		
		String sql = "insert into events values(NULL,?,?,?)";
		
		try {
			PreparedStatement ps = DatabaseConnector.getConnection().prepareStatement(sql);
			ps.setLong(1, message.getTimestamp());
			ps.setString(2, message.getNickname());
			ps.setString(3, message.getMessage());

			ps.executeUpdate();

			ps.close();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		
	}
	
	public void insertEvents(List<ChatMessage> messages){
		
		for(ChatMessage msg : messages){
			insertEvent(msg);
		}
		
	}
	
	public List<ChatMessage> getAllEvents(){
		
		String sql = "SELECT eid, ts, nickname, txt FROM events";
		
		List<ChatMessage> messages = new ArrayList<ChatMessage>();
		
		try{
			PreparedStatement ps = DatabaseConnector.getConnection().prepareStatement(sql);		

			ResultSet rs = ps.executeQuery();	

			ChatMessage m;
			while (rs.next()) {
				m = new ChatMessage(rs.getString(3), rs.getString(4), rs.getLong(2));
				messages.add(m);
			}

			ps.close();

		}catch(SQLException e){
			System.out.println(e.getMessage());
		}

		return messages;
	}
	
	public List<ChatMessage> getEventsBetween(long from, long to){
		
		String sql = "SELECT eid, ts, nickname, txt FROM events WHERE ts > ? AND ts < ?";
		
		List<ChatMessage> messages = new ArrayList<ChatMessage>();
		
		try{
			PreparedStatement ps = DatabaseConnector.getConnection().prepareStatement(sql);		

			ps.setLong(1, from);
			ps.setLong(2, to);
			
			ResultSet rs = ps.executeQuery();	

			ChatMessage m;
			while (rs.next()) {
				m = new ChatMessage(rs.getString(3), rs.getString(4), rs.getLong(2));
				messages.add(m);
			}

			ps.close();

		}catch(SQLException e){
			System.out.println(e.getMessage());
		}
		
		return messages;
	}
	
	public List<ChatMessage> getEventsBefore(long time){
		return getEventsBetween(0, time);
	}
	
	public List<ChatMessage> getEventsAfter(long time){
		return getEventsBetween(time, Long.MAX_VALUE);
	}	
	
}
