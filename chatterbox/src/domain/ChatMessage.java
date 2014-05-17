package domain;

public class ChatMessage {

	private String nickname;
	private String message;
	private long timestamp;
	
	public ChatMessage(String nickname, String message, long timestamp) {
		super();
		this.nickname = nickname;
		this.message = message;
		this.timestamp = timestamp;
	}
	
	
	public String getNickname() {
		return nickname;
	}
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
}
