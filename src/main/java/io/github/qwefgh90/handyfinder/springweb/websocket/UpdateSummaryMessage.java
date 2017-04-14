package io.github.qwefgh90.handyfinder.springweb.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * singleton pattern
 * @author cheochangwon
 *
 */
@JsonIgnoreProperties({ "receiver" })
public class UpdateSummaryMessage implements IMessage {

	public enum STATE {
		START, TERMINATE
	}
	
	private int countOfDeleted;
	private int countOfExcluded;
	private int countOfModified;
	private STATE state;
	private IMessageSender receiver;

	private UpdateSummaryMessage() {
		// TODO Auto-generated constructor stub
	}
	
	public static UpdateSummaryMessage createCommand(IMessageSender receiver, STATE state, int countOfDeleted, int countOfExcluded, int countOfModified){
		UpdateSummaryMessage command = new UpdateSummaryMessage();
		command.receiver = receiver;
		command.countOfDeleted = countOfDeleted;
		command.countOfExcluded = countOfExcluded;
		command.countOfModified = countOfModified;
		command.state = state;
		return command;
	}
	
	@Override
	public void send() {
		receiver.sendToUpdateSummary(this);
	}
	
	public STATE getState() {
		return state;
	}

	public void setState(STATE state) {
		this.state = state;
	}

	public int getCountOfDeleted() {
		return countOfDeleted;
	}

	public void setCountOfDeleted(int countOfDeleted) {
		this.countOfDeleted = countOfDeleted;
	}

	public int getCountOfExcluded() {
		return countOfExcluded;
	}

	public void setCountOfExcluded(int countOfExcluded) {
		this.countOfExcluded = countOfExcluded;
	}

	public int getCountOfModified() {
		return countOfModified;
	}

	public void setCountOfModified(int countOfModified) {
		this.countOfModified = countOfModified;
	}

	
}
