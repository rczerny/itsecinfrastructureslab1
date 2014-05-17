package service;

import java.util.List;

import dao.EventDAO;
import domain.ChatMessage;

public class EventService {

	private EventDAO eventDao = new EventDAO();

	public void insertEvent(ChatMessage message){
		eventDao.insertEvent(message);
	}
	
	public void insertEvents(List<ChatMessage> messages){
		eventDao.insertEvents(messages);
	}
	
	public List<ChatMessage> getAllEvents(){
		return eventDao.getAllEvents();
	}
	
	public List<ChatMessage> getEventsBetween(long from, long to){
		return eventDao.getEventsBetween(from, to);
	}
	
	public List<ChatMessage> getEventsBefore(long time){
		return eventDao.getEventsBefore(time);
	}
}
